/*******************************************************************************
 * Copyright 2021 spancer
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/

package io.hermes.discovery.zen.ping.multicast;

import static io.hermes.cluster.node.DiscoveryNode.readNode;
import static io.hermes.util.concurrent.ConcurrentMaps.newConcurrentMap;
import static io.hermes.util.concurrent.DynamicExecutors.daemonThreadFactory;
import static io.hermes.util.io.NetworkUtils.resolvePublishHostAddress;

import io.hermes.HermesException;
import io.hermes.HermesIllegalStateException;
import io.hermes.cluster.ClusterName;
import io.hermes.cluster.node.DiscoveryNode;
import io.hermes.cluster.node.DiscoveryNodes;
import io.hermes.discovery.DiscoveryException;
import io.hermes.discovery.zen.DiscoveryNodesProvider;
import io.hermes.discovery.zen.ping.ZenPing;
import io.hermes.discovery.zen.ping.ZenPingException;
import io.hermes.threadpool.ThreadPool;
import io.hermes.transport.BaseTransportRequestHandler;
import io.hermes.transport.RemoteTransportException;
import io.hermes.transport.TransportChannel;
import io.hermes.transport.TransportService;
import io.hermes.transport.VoidTransportResponseHandler;
import io.hermes.util.TimeValue;
import io.hermes.util.component.AbstractLifecycleComponent;
import io.hermes.util.io.stream.BytesStreamInput;
import io.hermes.util.io.stream.BytesStreamOutput;
import io.hermes.util.io.stream.HandlesStreamInput;
import io.hermes.util.io.stream.HandlesStreamOutput;
import io.hermes.util.io.stream.StreamInput;
import io.hermes.util.io.stream.StreamOutput;
import io.hermes.util.io.stream.Streamable;
import io.hermes.util.io.stream.VoidStreamable;
import io.hermes.util.settings.ImmutableSettings;
import io.hermes.util.settings.Settings;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author spancer.ray
 */
public class MulticastZenPing extends AbstractLifecycleComponent<ZenPing> implements ZenPing {

  private final String address;

  private final int port;

  private final String group;

  private final int bufferSize;

  private final int ttl;

  private final ThreadPool threadPool;

  private final TransportService transportService;

  private final ClusterName clusterName;
  private final AtomicInteger pingIdGenerator = new AtomicInteger();
  private final Map<Integer, ConcurrentMap<DiscoveryNode, PingResponse>> receivedResponses =
      newConcurrentMap();
  private final Object sendMutex = new Object();
  private final Object receiveMutex = new Object();
  private volatile DiscoveryNodesProvider nodesProvider;
  private volatile Receiver receiver;
  private volatile Thread receiverThread;
  private MulticastSocket multicastSocket;
  private DatagramPacket datagramPacketSend;
  private DatagramPacket datagramPacketReceive;

  public MulticastZenPing(ThreadPool threadPool, TransportService transportService,
      ClusterName clusterName) {
    this(ImmutableSettings.Builder.EMPTY_SETTINGS, threadPool, transportService, clusterName);
  }

  public MulticastZenPing(Settings settings, ThreadPool threadPool,
      TransportService transportService, ClusterName clusterName) {
    super(settings);
    this.threadPool = threadPool;
    this.transportService = transportService;
    this.clusterName = clusterName;

    this.address = componentSettings.get("address");
    this.port = componentSettings.getAsInt("port", 54328);
    this.group = componentSettings.get("group", "224.2.2.4");
    this.bufferSize = componentSettings.getAsInt("buffer_size", 2048);
    this.ttl = componentSettings.getAsInt("ttl", 3);

    this.transportService.registerHandler(MulticastPingResponseRequestHandler.ACTION,
        new MulticastPingResponseRequestHandler());
  }

  @Override
  public void setNodesProvider(DiscoveryNodesProvider nodesProvider) {
    if (lifecycle.started()) {
      throw new HermesIllegalStateException("Can't set nodes provider when started");
    }
    this.nodesProvider = nodesProvider;
  }

  @Override
  protected void doStart() throws HermesException {
    try {
      this.datagramPacketReceive = new DatagramPacket(new byte[bufferSize], bufferSize);
      this.datagramPacketSend =
          new DatagramPacket(new byte[bufferSize], bufferSize, InetAddress.getByName(group), port);
    } catch (Exception e) {
      throw new DiscoveryException("Failed to set datagram packets", e);
    }

    try {
      MulticastSocket multicastSocket = new MulticastSocket(null);
      multicastSocket.setReuseAddress(true);
      // bind to receive interface
      multicastSocket.bind(new InetSocketAddress(port));
      multicastSocket.setTimeToLive(ttl);
      // set the send interface
      InetAddress multicastInterface = resolvePublishHostAddress(address, settings);
      multicastSocket.setInterface(multicastInterface);
      multicastSocket.setReceiveBufferSize(bufferSize);
      multicastSocket.setSendBufferSize(bufferSize);
      multicastSocket.joinGroup(InetAddress.getByName(group));
      multicastSocket.setSoTimeout(60000);

      this.multicastSocket = multicastSocket;
    } catch (Exception e) {
      throw new DiscoveryException("Failed to setup multicast socket", e);
    }

    this.receiver = new Receiver();
    this.receiverThread =
        daemonThreadFactory(settings, "discovery#multicast#received").newThread(receiver);
    this.receiverThread.start();
  }

  @Override
  protected void doStop() throws HermesException {
    receiver.stop();
    receiverThread.interrupt();
    multicastSocket.close();
  }

  @Override
  protected void doClose() throws HermesException {
  }

  public PingResponse[] pingAndWait(TimeValue timeout) {
    final AtomicReference<PingResponse[]> response = new AtomicReference<PingResponse[]>();
    final CountDownLatch latch = new CountDownLatch(1);
    ping(new PingListener() {
      @Override
      public void onPing(PingResponse[] pings) {
        response.set(pings);
        latch.countDown();
      }
    }, timeout);
    try {
      latch.await();
      return response.get();
    } catch (InterruptedException e) {
      return null;
    }
  }

  @Override
  public void ping(final PingListener listener, final TimeValue timeout) {
    final int id = pingIdGenerator.incrementAndGet();
    receivedResponses.put(id, new ConcurrentHashMap<DiscoveryNode, PingResponse>());
    sendPingRequest(id);
    // try and send another ping request halfway through (just in case someone woke up during it...)
    // this can be a good trade-off to nailing the initial lookup or un-delivered messages
    threadPool.schedule(new Runnable() {
      @Override
      public void run() {
        try {
          sendPingRequest(id);
        } catch (Exception e) {
          logger.debug("[{}] Failed to send second ping request", e, id);
        }
      }
    }, timeout.millis() / 2, TimeUnit.MILLISECONDS);
    threadPool.schedule(new Runnable() {
      @Override
      public void run() {
        ConcurrentMap<DiscoveryNode, PingResponse> responses = receivedResponses.remove(id);
        listener.onPing(responses.values().toArray(new PingResponse[responses.size()]));
      }
    }, timeout);
  }

  private void sendPingRequest(int id) {
    synchronized (sendMutex) {
      try {
        HandlesStreamOutput out = BytesStreamOutput.Cached.cachedHandles();
        out.writeInt(id);
        clusterName.writeTo(out);
        nodesProvider.nodes().localNode().writeTo(out);
        datagramPacketSend.setData(((BytesStreamOutput) out.wrappedOut()).copiedByteArray());
      } catch (IOException e) {
        receivedResponses.remove(id);
        throw new ZenPingException("Failed to serialize ping request", e);
      }
      try {
        multicastSocket.send(datagramPacketSend);
        if (logger.isTraceEnabled()) {
          logger.trace("[{}] Sending ping request", id);
        }
      } catch (IOException e) {
        receivedResponses.remove(id);
        throw new ZenPingException("Failed to send ping request over multicast", e);
      }
    }
  }

  static class MulticastPingResponse implements Streamable {

    int id;

    PingResponse pingResponse;

    MulticastPingResponse() {
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
      id = in.readInt();
      pingResponse = PingResponse.readPingResponse(in);
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
      out.writeInt(id);
      pingResponse.writeTo(out);
    }
  }

  class MulticastPingResponseRequestHandler
      extends BaseTransportRequestHandler<MulticastPingResponse> {

    static final String ACTION = "discovery/zen/multicast";

    @Override
    public MulticastPingResponse newInstance() {
      return new MulticastPingResponse();
    }

    @Override
    public void messageReceived(MulticastPingResponse request, TransportChannel channel)
        throws Exception {
      if (logger.isTraceEnabled()) {
        logger.trace("[{}] Received {}", request.id, request.pingResponse);
      }
      ConcurrentMap<DiscoveryNode, PingResponse> responses = receivedResponses.get(request.id);
      if (responses == null) {
        logger.warn("Received ping response with no matching id [{}]", request.id);
      } else {
        responses.put(request.pingResponse.target(), request.pingResponse);
      }
      channel.sendResponse(VoidStreamable.INSTANCE);
    }
  }

  private class Receiver implements Runnable {

    private volatile boolean running = true;

    public void stop() {
      running = false;
    }

    @Override
    public void run() {
      while (running) {
        try {
          int id;
          DiscoveryNode requestingNodeX;
          ClusterName clusterName;
          synchronized (receiveMutex) {
            try {
              multicastSocket.receive(datagramPacketReceive);
            } catch (SocketTimeoutException ignore) {
              continue;
            } catch (Exception e) {
              if (running) {
                logger.warn("Failed to receive packet", e);
              }
              continue;
            }
            try {
              StreamInput input = HandlesStreamInput.Cached
                  .cached(new BytesStreamInput(datagramPacketReceive.getData(),
                      datagramPacketReceive.getOffset(), datagramPacketReceive.getLength()));
              id = input.readInt();
              clusterName = ClusterName.readClusterName(input);
              requestingNodeX = readNode(input);
            } catch (Exception e) {
              logger.warn("Failed to read requesting node from {}", e,
                  datagramPacketReceive.getSocketAddress());
              continue;
            }
          }
          DiscoveryNodes discoveryNodes = nodesProvider.nodes();
          final DiscoveryNode requestingNode = requestingNodeX;
          if (requestingNode.id().equals(discoveryNodes.localNodeId())) {
            // that's me, ignore
            continue;
          }
          if (!clusterName.equals(MulticastZenPing.this.clusterName)) {
            // not our cluster, ignore it...
            continue;
          }
          final MulticastPingResponse multicastPingResponse = new MulticastPingResponse();
          multicastPingResponse.id = id;
          multicastPingResponse.pingResponse = new PingResponse(discoveryNodes.localNode(),
              discoveryNodes.masterNode(), clusterName);

          if (logger.isTraceEnabled()) {
            logger.trace("[{}] Received ping_request from [{}], sending {}", id, requestingNode,
                multicastPingResponse.pingResponse);
          }

          if (!transportService.nodeConnected(requestingNode)) {
            // do the connect and send on a thread pool
            threadPool.execute(new Runnable() {
              @Override
              public void run() {
                // connect to the node if possible
                try {
                  transportService.connectToNode(requestingNode);
                } catch (Exception e) {
                  logger.warn("Failed to connect to requesting node {}", e, requestingNode);
                }
                transportService.sendRequest(requestingNode,
                    MulticastPingResponseRequestHandler.ACTION, multicastPingResponse,
                    new VoidTransportResponseHandler(false) {
                      @Override
                      public void handleException(RemoteTransportException exp) {
                        logger.warn("Failed to receive confirmation on sent ping response to [{}]",
                            exp, requestingNode);
                      }
                    });
              }
            });
          } else {
            transportService.sendRequest(requestingNode, MulticastPingResponseRequestHandler.ACTION,
                multicastPingResponse, new VoidTransportResponseHandler(false) {
                  @Override
                  public void handleException(RemoteTransportException exp) {
                    logger.warn("Failed to receive confirmation on sent ping response to [{}]", exp,
                        requestingNode);
                  }
                });
          }
        } catch (Exception e) {
          logger.warn("Unexpected exception in multicast receiver", e);
        }
      }
    }
  }
}
