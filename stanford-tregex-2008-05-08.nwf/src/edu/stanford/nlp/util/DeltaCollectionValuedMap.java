package edu.stanford.nlp.util;

import java.util.*;

/**
 * @author Teg Grenager (grenager@cs.stanford.edu)
 * Date: Jan 14, 2004
 * Time: 10:40:57 AM
 */
public class DeltaCollectionValuedMap extends CollectionValuedMap {
  private CollectionValuedMap originalMap;
  private Map deltaMap;
  private static Object removedValue = new Object();
  private CollectionFactory cf;

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

  public Collection get(Object key) {
    // key could be not in original or in deltaMap
    // key could be not in original but in deltaMap
    // key could be in original but removed from deltaMap
    // key could be in original but mapped to something else in deltaMap
    Collection deltaResult = (Collection) deltaMap.get(key);
    if (deltaResult == null) {
      return originalMap.get(key);
    }
    if (deltaResult == removedValue) {
      return cf.newEmptyCollection();
    }
    return deltaResult;
  }

  // Modification Operations

  public Collection put(Object key, Collection value) {
    throw new UnsupportedOperationException();
  }

  public void putAll(Map m) {
    throw new UnsupportedOperationException();
  }

  public void add(Object key, Object value) {
    Collection deltaC = (Collection) deltaMap.get(key);
    if (deltaC == null) {
      deltaC = cf.newCollection();
      Collection originalC = (Collection) originalMap.get(key);
      if (originalC != null) {
        deltaC.addAll(originalC);
      }
      deltaMap.put(key, deltaC);
    }
    deltaC.add(value);
  }

  /**
   * Adds all of the mappings in m to this CollectionValuedMap.
   * If m is a CollectionValuedMap, it will behave strangely. Use the constructor instead.
   *
   * @param m
   */
  public void addAll(Map m) {
    Iterator i = m.entrySet().iterator();
    while (i.hasNext()) {
      Map.Entry e = (Map.Entry) i.next();
      add(e.getKey(), e.getValue());
    }
  }

  public Collection remove(Object key) {
    Collection result = get(key);
    deltaMap.put(key, removedValue);
    return result;
  }

  public void removeMapping(Object key, Object value) {
    Collection deltaC = (Collection) deltaMap.get(key);
    if (deltaC == null) {
      Collection originalC = (Collection) originalMap.get(key);
      if (originalC != null && originalC.contains(value)) {
        deltaC = cf.newCollection();
        deltaC.addAll(originalC);
        deltaMap.put(key, deltaC);
      }
    }
    if (deltaC != null) {
      deltaC.remove(value);
    }
  }

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

  public boolean containsValue(Object value) {
    throw new UnsupportedOperationException();
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

  public boolean isEmpty() {
    return size() == 0;
  }

  public int size() {
    return entrySet().size();
  }

  public Collection values() {
    throw new UnsupportedOperationException();
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


        Iterator iter2 = new FilteredIterator(deltaMap.entrySet().iterator(), filter2);

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

  public DeltaCollectionValuedMap(CollectionValuedMap originalMap) {
    this.originalMap = originalMap;
    this.cf = originalMap.cf;
    this.mf = originalMap.mf;
    this.deltaMap = mf.newMap();
  }
}
