package edu.stanford.nlp.util;

import java.util.*;

/**
 * A Map which wraps an original Map, and only stores the changes (deltas) from
 * the original Map. This increases Map access time (roughly doubles it) but eliminates
 * Map creation time and decreases memory usage (if you're keeping the original Map in memory
 * anyway).
 * <p/>
 * @author Teg Grenager (grenager@cs.stanford.edu)
 * Date: Jan 9, 2004
 * Time: 9:19:06 AM
 */
public class DeltaMap extends AbstractMap implements Map {

  private Map originalMap;
  private Map deltaMap;
  private static Object nullValue = new Object();
  private static Object removedValue = new Object();

  static class SimpleEntry implements Map.Entry {
    Object key;
    Object value;

    public SimpleEntry(Object key, Object value) {
      this.key = key;
      this.value = value;
    }

    public SimpleEntry(Map.Entry e) {
      this.key = e.getKey();
      this.value = e.getValue();
    }

    public Object getKey() {
      return key;
    }

    public Object getValue() {
      return value;
    }

    public Object setValue(Object value) {
      Object oldValue = this.value;
      this.value = value;
      return oldValue;
    }

    public boolean equals(Object o) {
      if (!(o instanceof Map.Entry)) {
        return false;
      }
      Map.Entry e = (Map.Entry) o;
      return eq(key, e.getKey()) && eq(value, e.getValue());
    }

    public int hashCode() {
      return ((key == null) ? 0 : key.hashCode()) ^ ((value == null) ? 0 : value.hashCode());
    }

    public String toString() {
      return key + "=" + value;
    }

    private static boolean eq(Object o1, Object o2) {
      return (o1 == null ? o2 == null : o1.equals(o2));
    }
  }

  /**
   * This is expensive.
   *
   * @param key key whose presence in this map is to be tested.
   * @return <tt>true</tt> if this map contains a mapping for the specified
   *         key.
   */
  public boolean containsKey(Object key) {
    // key could be not in original or in deltaMap
    // key could be not in original but in deltaMap
    // key could be in original but removed from deltaMap
    // key could be in original but mapped to something else in deltaMap
    Object value = deltaMap.get(key);
    if (value == null) {
      return originalMap.containsKey(key);
    }
    if (value == removedValue) {
      return false;
    }
    return true;
  }

  /**
   * This may cost twice what it would in the original Map.
   *
   * @param key key whose associated value is to be returned.
   * @return the value to which this map maps the specified key, or
   *         <tt>null</tt> if the map contains no mapping for this key.
   */
  public Object get(Object key) {
    // key could be not in original or in deltaMap
    // key could be not in original but in deltaMap
    // key could be in original but removed from deltaMap
    // key could be in original but mapped to something else in deltaMap
    Object deltaResult = deltaMap.get(key);
    if (deltaResult == null) {
      return originalMap.get(key);
    }
    if (deltaResult == nullValue) {
      return null;
    }
    if (deltaResult == removedValue) {
      return null;
    }
    return deltaResult;
  }

  // Modification Operations

  /**
   * This may cost twice what it would in the original Map because we have to find
   * the original value for this key.
   *
   * @param key   key with which the specified value is to be associated.
   * @param value value to be associated with the specified key.
   * @return previous value associated with specified key, or <tt>null</tt>
   *         if there was no mapping for key.  A <tt>null</tt> return can
   *         also indicate that the map previously associated <tt>null</tt>
   *         with the specified key, if the implementation supports
   *         <tt>null</tt> values.
   */
  public Object put(Object key, Object value) {
    if (value == null) {
      return put(key, nullValue);
    }
    // key could be not in original or in deltaMap
    // key could be not in original but in deltaMap
    // key could be in original but removed from deltaMap
    // key could be in original but mapped to something else in deltaMap
    Object result = deltaMap.put(key, value);
    if (result == null) {
      return originalMap.get(key);
    }
    if (result == nullValue) {
      return null;
    }
    if (result == removedValue) {
      return null;
    }
    return result;
  }

  /**
   *
   */
  public Object remove(Object key) {
    // always put it locally
    return put(key, removedValue);
  }


  // Bulk Operations

  /**
   * This is more expensive than normal.
   */
  public void clear() {
    // iterate over all keys in originalMap and set them to null in deltaMap
    for (Iterator i = originalMap.keySet().iterator(); i.hasNext();) {
      Object key = i.next();
      deltaMap.put(key, removedValue);
    }
  }


  // Views

  /**
   * This is cheap.
   *
   * @return a set view of the mappings contained in this map.
   */
  public Set entrySet() {
    return new AbstractSet() {
      public Iterator iterator() {
        Filter filter1 = new Filter() {
          // only accepts stuff not overwritten by deltaMap
          public boolean accept(Object o) {
            Map.Entry e = (Map.Entry) o;
            Object key = e.getKey();
            if (deltaMap.containsKey(key)) {
              return false;
            }
            return true;
          }
        };

        Iterator iter1 = new FilteredIterator(originalMap.entrySet().iterator(), filter1);

        Filter filter2 = new Filter() {
          // only accepts stuff not overwritten by deltaMap
          public boolean accept(Object o) {
            Map.Entry e = (Map.Entry) o;
            Object value = e.getValue();
            if (value == removedValue) {
              return false;
            }
            return true;
          }
        };

        class NullingIterator implements Iterator {
          private Iterator i;

          public NullingIterator(Iterator i) {
            this.i = i;
          }

          public boolean hasNext() {
            return i.hasNext();
          }

          public Object next() {
            Map.Entry e = (Map.Entry) i.next();
            Object o = e.getValue();
            if (o == nullValue) {
              return new SimpleEntry(e.getKey(), null);
            }
            return e;
          }

          public void remove() {
            throw new UnsupportedOperationException();
          }
        }

        Iterator iter2 = new FilteredIterator(new NullingIterator(deltaMap.entrySet().iterator()), filter2);

        return new ConcatenationIterator(iter1, iter2);
      }

      public int size() {
        int size = 0;
        for (Iterator iterator = this.iterator(); iterator.hasNext();) {
          iterator.next();
          size++;
        }
        return size;
      }
    };
  }


  /**
   * This is very cheap.
   *
   * @param originalMap will serve as the basis for this DeltaMap
   */
  public DeltaMap(Map originalMap, MapFactory mf) {
    this.originalMap = Collections.unmodifiableMap(originalMap); // unmodifiable for debugging only
    this.deltaMap = mf.newMap();
  }

  public DeltaMap(Map originalMap) {
    this(originalMap, MapFactory.HASH_MAP_FACTORY);
  }

  /**
   * For testing only.
   *
   * @param args from command line
   */
  public static void main(String[] args) {
    Map originalMap = new HashMap();
    Random r = new Random();
    for (int i = 0; i < 1000; i++) {
      originalMap.put(new Integer(i), new Integer(r.nextInt(1000)));
    }
    Map originalCopyMap = new HashMap(originalMap);
    Map deltaCopyMap = new HashMap(originalMap);
    Map deltaMap = new DeltaMap(originalMap);
    // now make a lot of changes to deltaMap;
    // add and change some stuff
    for (int i = 900; i < 1100; i++) {
      Integer rInt = new Integer(r.nextInt(1000));
      deltaMap.put(new Integer(i), rInt);
      deltaCopyMap.put(new Integer(i), rInt);
    }
    // remove some stuff
    for (int i = 0; i < 100; i++) {
      Integer rInt = new Integer(r.nextInt(1100));
      deltaMap.remove(rInt);
      deltaCopyMap.remove(rInt);
    }
    // set some stuff to null
    for (int i = 0; i < 100; i++) {
      Integer rInt = new Integer(r.nextInt(1100));
      deltaMap.put(rInt, null);
      deltaCopyMap.put(rInt, null);
    }

    System.out.println("Original preserved? " + originalCopyMap.equals(originalMap));
    System.out.println("Delta accurate? " + deltaMap.equals(deltaCopyMap));
  }
}
