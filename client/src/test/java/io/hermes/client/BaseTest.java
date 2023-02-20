package io.hermes.client;

import org.junit.After;
import org.junit.Before;
import io.hermes.client.transport.TransportClient;
import io.hermes.util.transport.InetSocketTransportAddress;

/**
 * @Author spancer.ray
 * @Description:
 * @Date 2021年11月5日
 */
public abstract class BaseTest {

  protected TransportClient transportClient;

  @Before
  public void initClient() {
    transportClient = new TransportClient();
    transportClient.addTransportAddress(new InetSocketTransportAddress("localhost", 9400));
  }

  @After
  public void closeClient() {
    transportClient.close();
  }
}
