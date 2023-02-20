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

package io.hermes.util.gnu.trove;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;

//////////////////////////////////////////////////
// THIS IS A GENERATED CLASS. DO NOT HAND EDIT! //
//////////////////////////////////////////////////


/**
 * An open addressed Map implementation for float keys and short values.
 * <p/>
 * Created: Sun Nov 4 08:52:45 2001
 *
 * @author Eric D. Friedman
 */
public class TFloatShortHashMap extends TFloatHash implements Externalizable {

  static final long serialVersionUID = 1L;
  /**
   * the values of the map
   */
  protected transient short[] _values;
  private final TFloatShortProcedure PUT_ALL_PROC = new TFloatShortProcedure() {
    public boolean execute(float key, short value) {
      put(key, value);
      return true;
    }
  };

  /**
   * Creates a new <code>TFloatShortHashMap</code> instance with the default capacity and load
   * factor.
   */
  public TFloatShortHashMap() {
    super();
  }

  /**
   * Creates a new <code>TFloatShortHashMap</code> instance with a prime capacity equal to or
   * greater than <tt>initialCapacity</tt> and with the default load factor.
   *
   * @param initialCapacity an <code>int</code> value
   */
  public TFloatShortHashMap(int initialCapacity) {
    super(initialCapacity);
  }

  /**
   * Creates a new <code>TFloatShortHashMap</code> instance with a prime capacity equal to or
   * greater than <tt>initialCapacity</tt> and with the specified load factor.
   *
   * @param initialCapacity an <code>int</code> value
   * @param loadFactor      a <code>float</code> value
   */
  public TFloatShortHashMap(int initialCapacity, float loadFactor) {
    super(initialCapacity, loadFactor);
  }

  /**
   * Creates a new <code>TFloatShortHashMap</code> instance with the default capacity and load
   * factor.
   *
   * @param strategy used to compute hash codes and to compare keys.
   */
  public TFloatShortHashMap(TFloatHashingStrategy strategy) {
    super(strategy);
  }

  /**
   * Creates a new <code>TFloatShortHashMap</code> instance whose capacity is the next highest prime
   * above <tt>initialCapacity + 1</tt> unless that value is already prime.
   *
   * @param initialCapacity an <code>int</code> value
   * @param strategy        used to compute hash codes and to compare keys.
   */
  public TFloatShortHashMap(int initialCapacity, TFloatHashingStrategy strategy) {
    super(initialCapacity, strategy);
  }

  /**
   * Creates a new <code>TFloatShortHashMap</code> instance with a prime value at or near the
   * specified capacity and load factor.
   *
   * @param initialCapacity used to find a prime capacity for the table.
   * @param loadFactor      used to calculate the threshold over which rehashing takes place.
   * @param strategy        used to compute hash codes and to compare keys.
   */
  public TFloatShortHashMap(int initialCapacity, float loadFactor, TFloatHashingStrategy strategy) {
    super(initialCapacity, loadFactor, strategy);
  }

  /**
   * @return a deep clone of this collection
   */
  public Object clone() {
    TFloatShortHashMap m = (TFloatShortHashMap) super.clone();
    m._values = this._values.clone();
    return m;
  }

  /**
   * @return a TFloatShortIterator with access to this map's keys and values
   */
  public TFloatShortIterator iterator() {
    return new TFloatShortIterator(this);
  }

  /**
   * initializes the hashtable to a prime capacity which is at least <tt>initialCapacity + 1</tt>.
   *
   * @param initialCapacity an <code>int</code> value
   * @return the actual capacity chosen
   */
  protected int setUp(int initialCapacity) {
    int capacity;

    capacity = super.setUp(initialCapacity);
    _values = new short[capacity];
    return capacity;
  }

  /**
   * Inserts a key/value pair into the map.
   *
   * @param key   an <code>float</code> value
   * @param value an <code>short</code> value
   * @return the previous value associated with <tt>key</tt>, or (float)0 if none was found.
   */
  public short put(float key, short value) {
    int index = insertionIndex(key);
    return doPut(key, value, index);
  }

  /**
   * Inserts a key/value pair into the map if the specified key is not already associated with a
   * value.
   *
   * @param key   an <code>float</code> value
   * @param value an <code>short</code> value
   * @return the previous value associated with <tt>key</tt>, or (float)0 if none was found.
   */
  public short putIfAbsent(float key, short value) {
    int index = insertionIndex(key);
    if (index < 0) {
      return _values[-index - 1];
    }
    return doPut(key, value, index);
  }

  private short doPut(float key, short value, int index) {
    byte previousState;
    short previous = (short) 0;
    boolean isNewMapping = true;
    if (index < 0) {
      index = -index - 1;
      previous = _values[index];
      isNewMapping = false;
    }
    previousState = _states[index];
    _set[index] = key;
    _states[index] = FULL;
    _values[index] = value;
    if (isNewMapping) {
      postInsertHook(previousState == FREE);
    }

    return previous;
  }


  /**
   * Put all the entries from the given map into this map.
   *
   * @param map The map from which entries will be obtained to put into this map.
   */
  public void putAll(TFloatShortHashMap map) {
    map.forEachEntry(PUT_ALL_PROC);
  }


  /**
   * rehashes the map to the new capacity.
   *
   * @param newCapacity an <code>int</code> value
   */
  protected void rehash(int newCapacity) {
    int oldCapacity = _set.length;
    float[] oldKeys = _set;
    short[] oldVals = _values;
    byte[] oldStates = _states;

    _set = new float[newCapacity];
    _values = new short[newCapacity];
    _states = new byte[newCapacity];

    for (int i = oldCapacity; i-- > 0; ) {
      if (oldStates[i] == FULL) {
        float o = oldKeys[i];
        int index = insertionIndex(o);
        _set[index] = o;
        _values[index] = oldVals[i];
        _states[index] = FULL;
      }
    }
  }

  /**
   * retrieves the value for <tt>key</tt>
   *
   * @param key an <code>float</code> value
   * @return the value of <tt>key</tt> or (float)0 if no such mapping exists.
   */
  public short get(float key) {
    int index = index(key);
    return index < 0 ? (short) 0 : _values[index];
  }

  /**
   * Empties the map.
   */
  public void clear() {
    super.clear();
    float[] keys = _set;
    short[] vals = _values;
    byte[] states = _states;

    Arrays.fill(_set, 0, _set.length, (float) 0);
    Arrays.fill(_values, 0, _values.length, (short) 0);
    Arrays.fill(_states, 0, _states.length, FREE);
  }

  /**
   * Deletes a key/value pair from the map.
   *
   * @param key an <code>float</code> value
   * @return an <code>short</code> value, or (float)0 if no mapping for key exists
   */
  public short remove(float key) {
    short prev = (short) 0;
    int index = index(key);
    if (index >= 0) {
      prev = _values[index];
      removeAt(index); // clear key,state; adjust size
    }
    return prev;
  }

  /**
   * Compares this map with another map for equality of their stored entries.
   *
   * @param other an <code>Object</code> value
   * @return a <code>boolean</code> value
   */
  public boolean equals(Object other) {
    if (!(other instanceof TFloatShortHashMap)) {
      return false;
    }
    TFloatShortHashMap that = (TFloatShortHashMap) other;
    if (that.size() != this.size()) {
      return false;
    }
    return forEachEntry(new EqProcedure(that));
  }

  public int hashCode() {
    HashProcedure p = new HashProcedure();
    forEachEntry(p);
    return p.getHashCode();
  }

  /**
   * removes the mapping at <tt>index</tt> from the map.
   *
   * @param index an <code>int</code> value
   */
  protected void removeAt(int index) {
    _values[index] = (short) 0;
    super.removeAt(index); // clear key, state; adjust size
  }

  /**
   * Returns the values of the map.
   *
   * @return a <code>Collection</code> value
   */
  public short[] getValues() {
    short[] vals = new short[size()];
    short[] v = _values;
    byte[] states = _states;

    for (int i = v.length, j = 0; i-- > 0; ) {
      if (states[i] == FULL) {
        vals[j++] = v[i];
      }
    }
    return vals;
  }

  /**
   * returns the keys of the map.
   *
   * @return a <code>Set</code> value
   */
  public float[] keys() {
    float[] keys = new float[size()];
    float[] k = _set;
    byte[] states = _states;

    for (int i = k.length, j = 0; i-- > 0; ) {
      if (states[i] == FULL) {
        keys[j++] = k[i];
      }
    }
    return keys;
  }

  /**
   * returns the keys of the map.
   *
   * @param a the array into which the elements of the list are to be stored, if it is big enough;
   *          otherwise, a new array of the same type is allocated for this purpose.
   * @return a <code>Set</code> value
   */
  public float[] keys(float[] a) {
    int size = size();
    if (a.length < size) {
      a = (float[]) java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), size);
    }

    float[] k = _set;
    byte[] states = _states;

    for (int i = k.length, j = 0; i-- > 0; ) {
      if (states[i] == FULL) {
        a[j++] = k[i];
      }
    }
    return a;
  }

  /**
   * checks for the presence of <tt>val</tt> in the values of the map.
   *
   * @param val an <code>short</code> value
   * @return a <code>boolean</code> value
   */
  public boolean containsValue(short val) {
    byte[] states = _states;
    short[] vals = _values;

    for (int i = vals.length; i-- > 0; ) {
      if (states[i] == FULL && val == vals[i]) {
        return true;
      }
    }
    return false;
  }

  /**
   * checks for the present of <tt>key</tt> in the keys of the map.
   *
   * @param key an <code>float</code> value
   * @return a <code>boolean</code> value
   */
  public boolean containsKey(float key) {
    return contains(key);
  }

  /**
   * Executes <tt>procedure</tt> for each key in the map.
   *
   * @param procedure a <code>TFloatProcedure</code> value
   * @return false if the loop over the keys terminated because the procedure returned false for
   * some key.
   */
  public boolean forEachKey(TFloatProcedure procedure) {
    return forEach(procedure);
  }

  /**
   * Executes <tt>procedure</tt> for each value in the map.
   *
   * @param procedure a <code>TShortProcedure</code> value
   * @return false if the loop over the values terminated because the procedure returned false for
   * some value.
   */
  public boolean forEachValue(TShortProcedure procedure) {
    byte[] states = _states;
    short[] values = _values;
    for (int i = values.length; i-- > 0; ) {
      if (states[i] == FULL && !procedure.execute(values[i])) {
        return false;
      }
    }
    return true;
  }

  /**
   * Executes <tt>procedure</tt> for each key/value entry in the map.
   *
   * @param procedure a <code>TOFloatShortProcedure</code> value
   * @return false if the loop over the entries terminated because the procedure returned false for
   * some entry.
   */
  public boolean forEachEntry(TFloatShortProcedure procedure) {
    byte[] states = _states;
    float[] keys = _set;
    short[] values = _values;
    for (int i = keys.length; i-- > 0; ) {
      if (states[i] == FULL && !procedure.execute(keys[i], values[i])) {
        return false;
      }
    }
    return true;
  }

  /**
   * Retains only those entries in the map for which the procedure returns a true value.
   *
   * @param procedure determines which entries to keep
   * @return true if the map was modified.
   */
  public boolean retainEntries(TFloatShortProcedure procedure) {
    boolean modified = false;
    byte[] states = _states;
    float[] keys = _set;
    short[] values = _values;

    // Temporarily disable compaction. This is a fix for bug #1738760
    tempDisableAutoCompaction();
    try {
      for (int i = keys.length; i-- > 0; ) {
        if (states[i] == FULL && !procedure.execute(keys[i], values[i])) {
          removeAt(i);
          modified = true;
        }
      }
    } finally {
      reenableAutoCompaction(true);
    }

    return modified;
  }

  /**
   * Transform the values in this map using <tt>function</tt>.
   *
   * @param function a <code>TShortFunction</code> value
   */
  public void transformValues(TShortFunction function) {
    byte[] states = _states;
    short[] values = _values;
    for (int i = values.length; i-- > 0; ) {
      if (states[i] == FULL) {
        values[i] = function.execute(values[i]);
      }
    }
  }

  /**
   * Increments the primitive value mapped to key by 1
   *
   * @param key the key of the value to increment
   * @return true if a mapping was found and modified.
   */
  public boolean increment(float key) {
    return adjustValue(key, (short) 1);
  }

  /**
   * Adjusts the primitive value mapped to key.
   *
   * @param key    the key of the value to increment
   * @param amount the amount to adjust the value by.
   * @return true if a mapping was found and modified.
   */
  public boolean adjustValue(float key, short amount) {
    int index = index(key);
    if (index < 0) {
      return false;
    } else {
      _values[index] += amount;
      return true;
    }
  }

  /**
   * Adjusts the primitive value mapped to the key if the key is present in the map. Otherwise, the
   * <tt>initial_value</tt> is put in the map.
   *
   * @param key           the key of the value to increment
   * @param adjust_amount the amount to adjust the value by
   * @param put_amount    the value put into the map if the key is not initial present
   * @return the value present in the map after the adjustment or put operation
   * @since 2.0b1
   */
  public short adjustOrPutValue(final float key, final short adjust_amount,
      final short put_amount) {
    int index = insertionIndex(key);
    final boolean isNewMapping;
    final short newValue;
    if (index < 0) {
      index = -index - 1;
      newValue = (_values[index] += adjust_amount);
      isNewMapping = false;
    } else {
      newValue = (_values[index] = put_amount);
      isNewMapping = true;
    }

    byte previousState = _states[index];
    _set[index] = key;
    _states[index] = FULL;

    if (isNewMapping) {
      postInsertHook(previousState == FREE);
    }

    return newValue;
  }

  public void writeExternal(ObjectOutput out) throws IOException {
    // VERSION
    out.writeByte(0);

    // NUMBER OF ENTRIES
    out.writeInt(_size);

    // ENTRIES
    SerializationProcedure writeProcedure = new SerializationProcedure(out);
    if (!forEachEntry(writeProcedure)) {
      throw writeProcedure.exception;
    }
  }

  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {

    // VERSION
    in.readByte();

    // NUMBER OF ENTRIES
    int size = in.readInt();
    setUp(size);

    // ENTRIES
    while (size-- > 0) {
      float key = in.readFloat();
      short val = in.readShort();
      put(key, val);
    }
  }

  public String toString() {
    final StringBuilder buf = new StringBuilder("{");
    forEachEntry(new TFloatShortProcedure() {
      private boolean first = true;

      public boolean execute(float key, short value) {
        if (first) {
          first = false;
        } else {
          buf.append(",");
        }

        buf.append(key);
        buf.append("=");
        buf.append(value);
        return true;
      }
    });
    buf.append("}");
    return buf.toString();
  }

  private static final class EqProcedure implements TFloatShortProcedure {

    private final TFloatShortHashMap _otherMap;

    EqProcedure(TFloatShortHashMap otherMap) {
      _otherMap = otherMap;
    }

    public final boolean execute(float key, short value) {
      int index = _otherMap.index(key);
      return index >= 0 && eq(value, _otherMap.get(key));
    }

    /**
     * Compare two shorts for equality.
     */
    private final boolean eq(short v1, short v2) {
      return v1 == v2;
    }

  }

  private final class HashProcedure implements TFloatShortProcedure {

    private int h = 0;

    public int getHashCode() {
      return h;
    }

    public final boolean execute(float key, short value) {
      h += (_hashingStrategy.computeHashCode(key) ^ HashFunctions.hash(value));
      return true;
    }
  }
} // TFloatShortHashMap
