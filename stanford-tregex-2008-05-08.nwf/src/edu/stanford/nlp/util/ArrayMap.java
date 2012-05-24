package edu.stanford.nlp.util;

import java.util.*;

/**
 * ArrayMap: A map that is backed by an Array
 *
 * @author Dan Klein
 * @author Roger Levy
 */
public final class ArrayMap<K,V> extends AbstractMap<K,V> {
  Map.Entry[] entryArray;
  int capacity;
  int size;

  final class Entry<K,V> implements Map.Entry<K,V> {
    K key;
    V value;

    public K getKey() {
      return key;
    }

    public V getValue() {
      return value;
    }

    public V setValue(V o) {
      V old = value;
      value = o;
      return old;
    }

    public int hashCode() {
      return (getKey() == null ? 0 : getKey().hashCode()) ^ (getValue() == null ? 0 : getValue().hashCode());
    }

    public boolean equals(Object o) {
      if(! (o instanceof Entry))
        return false;
      return (getKey() == null ? ((Entry) o).getKey() == null : getKey().equals(((Entry) o).getKey())) && (getValue() == null ? ((Entry) o).getValue() == null : getValue().equals(((Entry) o).getValue()));
    }


    Entry(K key, V value) {
      this.key = key;
      this.value = value;
    }
  }


  public ArrayMap() {
    size = 0;
    capacity = 2;
    entryArray = new Map.Entry[2];
  }

  public ArrayMap(int capacity) {
    size = 0;
    this.capacity = capacity;
    entryArray = new Map.Entry[capacity];
  }
  
  public ArrayMap(Map<? extends K, ? extends V> m) {
	  size = 0;
	  capacity = m.size();
	  entryArray = new Map.Entry[m.size()];
	  this.putAll(m);
  }

  public ArrayMap(K[] keys, V[] values) {
    if (keys.length!=values.length) throw new IllegalArgumentException("different number of keys and values.");
    size = keys.length;
    capacity = size;
    entryArray = new Map.Entry[size];
    for (int i=0; i<keys.length; i++) {
      entryArray[i] = new Entry<K,V>(keys[i], values[i]);
    }
  }

  public Set entrySet() {
    //throw new java.lang.UnsupportedOperationException();
    return new HashSet(Arrays.asList(entryArray).subList(0, size));
  }

  public int size() {
    return size;
  }

  public boolean isEmpty() {
    return size == 0;
  }

  private void resize() {
    Object[] oldEntryArray = entryArray;
    int newCapacity = 2*size;
    if (newCapacity==0) newCapacity=1;
    entryArray = new Map.Entry[newCapacity];
    System.arraycopy(oldEntryArray, 0, entryArray, 0, size);
    capacity = newCapacity;
  }

  public V put(K key, V val) {
    for (int i = 0; i < size; i++) {
      if (key.equals(entryArray[i].getKey())) {
        return (V) entryArray[i].setValue(val);
      }
    }
    if (capacity <= size) {
      resize();
    }
    entryArray[size] = new Entry<K,V>(key, val);
    size++;
    return null;
  }

  public V get(Object key) {
    for (int i = 0; i < size; i++) {
      if (key == null ? entryArray[i].getKey() == null : key.equals(entryArray[i].getKey())) {
        return (V) entryArray[i].getValue();
      }
    }
    return null;
  }

  public V remove(Object key) {
    for (int i = 0; i < size; i++) {
      if (key == null ? entryArray[i].getKey() == null : key.equals(entryArray[i].getKey())) {
        V value = (V) entryArray[i].getValue();
        if (size > 1) {
          entryArray[i] = entryArray[size - 1];
        }
        size--;
        return value;
      }
    }
    return null;
  }

  protected int hashCodeCache = 0;

  public int hashCode() {
    if (hashCodeCache == 0) {
      int hashCode = 0;
      for (int i = 0; i < size; i++) {
        hashCode += entryArray[i].hashCode();
      }
      hashCodeCache = hashCode;
    }
    return hashCodeCache;
  }

  public boolean equals(Object o) {
    Map m = (Map) o;
    for (int i = 0; i < size; i++) {
      Object mVal = m.get(entryArray[i].getKey());
      if (mVal == null) {
        if (entryArray[i] != null) {
          return false;
        } else {
          continue;
        }
      }
      if (!m.get(entryArray[i].getKey()).equals(entryArray[i].getValue())) {
        return false;
      }
    }
    return true;
  }

}
