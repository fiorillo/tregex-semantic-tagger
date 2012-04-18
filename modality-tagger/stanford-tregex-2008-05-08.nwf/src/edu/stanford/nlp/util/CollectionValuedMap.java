package edu.stanford.nlp.util;

import java.io.Serializable;
import java.util.*;

/**
 * A class which can store mappings from Object keys to {@link Collection}s of Object values.
 * Important methods are the {@link #add(Object key, Object value)} and
 * {@link #removeMapping(Object key, Object value)} methods for adding and removing a value
 * to/from the Collection associated with the key, and the {@link #get(Object key)} method for
 * getting the Collection associated with a key.
 * The class is quite general, because on construction, it is possible to pass a {@link MapFactory}
 * which will be used to create the underlying map and a {@link CollectionFactory} which will
 * be used to create the Collections. Thus this class can be configured to act like a "HashSetValuedMap"
 * or a "ListValuedMap", or even a "HashSetValuedIdentityHashMap". The possibilities are endless!
 * @author Teg Grenager (grenager@cs.stanford.edu)
 */
public class CollectionValuedMap<K, V> implements Map<K, Collection<V>>, Serializable {

  private Map<K, Collection<V>> map;
  protected CollectionFactory cf;
  private boolean treatCollectionsAsImmutable;
  protected MapFactory mf;

  /**
   * Replaces current Collection mapped to key with the specified Collection. Use carefully!
   *
   * @param key
   * @param collection
   * @return
   */
  public Collection<V> put(K key, Collection<V> collection) {
    return map.put(key, collection);
  }

  /**
   * Unsupported. Use {@link #addAll(Map)} instead.
   * @param m
   */
  public void putAll(Map<? extends K, ? extends Collection<V>> m) {
    throw new UnsupportedOperationException();
  }

  private Set<V> emptySet = Collections.<V>emptySet();
  
  /**
   * @param key
   * @return the Collection mapped to by key, never null, but may be empty.
   */
  public Collection<V> get(Object key) {
    Collection<V> c = (Collection<V>) map.get(key);
    if (c == null) {
//      c = cf.newEmptyCollection();
      c = emptySet;
    }
    return c;
  }

  /**
   * Adds the value to the Collection mapped to by the key.
   *
   * @param key
   * @param value
   */
  public void add(K key, V value) {
    if (treatCollectionsAsImmutable) {
      Collection<V> newC = cf.newCollection();
      Collection<V> c = map.get(key);
      if (c != null) {
        newC.addAll(c);
      }
      newC.add(value);
      map.put(key, newC); // replacing the old collection
    } else {
      Collection<V> c = map.get(key);
      if (c == null) {
        c = cf.newCollection();
        map.put(key, c);
      }
      c.add(value); // modifying to old collection
    }
  }

  /**
   * Adds all of the mappings in m to this CollectionValuedMap.
   * If m is a CollectionValuedMap, it will behave strangely. Use the constructor instead.
   *
   * @param m
   */
  public void addAll(Map<K, V> m) {
    if (m instanceof CollectionValuedMap) {
      throw new UnsupportedOperationException();
    }
    for ( Map.Entry<K, V> e : m.entrySet()) {
      add(e.getKey(), e.getValue());
    }
  }

  /**
   * Removes the mapping associated with this key from this Map.
   * @param key
   * @return the Collection mapped to by this key.
   */
  public Collection remove(Object key) {
    return map.remove(key);
  }

  /**
   * Removes the value from the Collection mapped to by this key, leaving the rest of
   * the collection intact.
   * @param key the key to the Collection to remove the value from
   * @param value the value to remove
   */
  public void removeMapping(K key, V value) {
    if (treatCollectionsAsImmutable) {
      Collection<V> c = map.get(key);
      if (c != null) {
        Collection<V> newC = cf.newCollection();
        newC.addAll(c);
        newC.remove(value);
        map.put(key, newC);
      }

    } else {
      Collection<V> c = get(key);
      c.remove(value);
    }
  }

  /**
   * Clears this Map.
   */
  public void clear() {
    map.clear();
  }

  /**
   * @param key
   * @return true iff this key is in this map
   */
  public boolean containsKey(Object key) {
    return map.containsKey(key);
  }

  /**
   * Unsupported.
   * @param value
   * @return
   */
  public boolean containsValue(Object value) {
    throw new UnsupportedOperationException();
  }

  /**
   * @return true iff this Map has no mappings in it.
   */
  public boolean isEmpty() {
    return map.isEmpty();
  }

  /**
   * Each element of the Set is a Map.Entry object, where getKey() returns the key of the mapping,
   * and getValue() returns the Collection mapped to by the key.
   * @return a Set view of the mappings contained in this map.
   */
  public Set entrySet() {
    return map.entrySet();
  }

  /**
   * @return a Set view of the keys in this Map.
   */
  public Set<K> keySet() {
    return map.keySet();
  }

  /**
   * The number of keys in this map.
   * @return
   */
  public int size() {
    return map.size();
  }

  /**
   * Unsupported.
   * @return
   */
  public Collection values() {
    throw new UnsupportedOperationException();
  }

  public Collection<V> allValues() {
    Collection<V> c = cf.newCollection();
    for (Collection<V> c1 : map.values()) {
      c.addAll(c1);
    }
    return c;
  }

  /**
   * @param o
   * @return true iff o is a CollectionValuedMap, and each key maps to the a Collection of the
   * same objects in o as it does in this CollectionValuedMap.
   */
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof CollectionValuedMap)) {
      return false;
    }

    CollectionValuedMap other = (CollectionValuedMap) o;

    if (other.size() != size()) {
      return false;
    }

    try {
      Iterator i = entrySet().iterator();
      while (i.hasNext()) {
        Map.Entry e = (Map.Entry) i.next();
        Object key = e.getKey();
        Object value = e.getValue();
        if (value == null) {
          if (!(other.get(key) == null && other.containsKey(key))) {
            return false;
          }
        } else {
          if (!value.equals(other.get(key))) {
            return false;
          }
        }
      }
    } catch (ClassCastException unused) {
      return false;
    } catch (NullPointerException unused) {
      return false;
    }

    return true;
  }

  /**
   * @return the hashcode of the underlying Map
   */
  public int hashCode() {
    return map.hashCode();
  }

  /**
   * Creates a "delta clone" of this Map, where only the differences are represented.
   * @return
   */
  public Object deltaClone() {
    CollectionValuedMap result = new CollectionValuedMap(null, cf, true);
    result.map = new DeltaMap(this.map);
    return result;
  }

  /**
   * @return a clone of this Map
   */
  public Object clone() {
    CollectionValuedMap result = new CollectionValuedMap(this);
    return result;
  }

  /**
   * @return a String representation of this CollectionValuedMap, with special machinery to avoid
   * recursion problems
   */
  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("{");

    Iterator i = entrySet().iterator();
    boolean hasNext = i.hasNext();
    while (hasNext) {
      Map.Entry e = (Map.Entry) (i.next());
      Object key = e.getKey();
      Object value = e.getValue();
      buf.append((key == this ? "(this Map)" : key) + "=" + (value == this ? "(this Map)" : value));

      hasNext = i.hasNext();
      if (hasNext) {
        buf.append(", ");
      }
    }

    buf.append("}");
    return buf.toString();
  }

  /**
   * Creates a new empty CollectionValuedMap.
   * @param mf a MapFactory which will be used to generate the underlying Map
   * @param cf a CollectionFactory which will be used to generate the Collections in each mapping
   * @param treatCollectionsAsImmutable if true, forces this Map to create new a Collection everytime
   * a new value is added to or deleted from the Collection a mapping.
   */
  public CollectionValuedMap(MapFactory mf, CollectionFactory cf, boolean treatCollectionsAsImmutable) {
    this.mf = mf;
    this.cf = cf;
    this.treatCollectionsAsImmutable = treatCollectionsAsImmutable;
    if (mf != null) {
      map = mf.newMap();
    }
  }


  /**
   * Creates a new CollectionValuedMap with all of the mappings from cvm. Same as {@link #clone()}.
   * @param cvm
   */
  public CollectionValuedMap(CollectionValuedMap cvm) {
    this.mf = cvm.mf;
    this.cf = cvm.cf;
    this.treatCollectionsAsImmutable = cvm.treatCollectionsAsImmutable;
    map = mf.newMap();
    for (Object o :  cvm.map.entrySet()) {
      Map.Entry<K, Collection<V>> entry = (Map.Entry<K, Collection<V>>) o;
      K key = entry.getKey();
      Collection<V> c = entry.getValue();
      for (V value : c) {
        add(key, value);
      }
    }
  }


  /**
   * Creates a new empty CollectionValuedMap which uses a HashMap as the
   * underlying Map, and HashSets as the Collections in each mapping. Does not
   * treat Collections as immutable.
   */
  public CollectionValuedMap() {
    this(MapFactory.HASH_MAP_FACTORY, CollectionFactory.HASH_SET_FACTORY, false);
  }


  /**
   * Creates a new empty CollectionValuedMap which uses a HashMap as the
   * underlying Map.  Does not treat Collections as immutable.
   *
   * @param cf a CollectionFactory which will be used to generate the
   * Collections in each mapping
   */
  public CollectionValuedMap(CollectionFactory cf) {
    this(MapFactory.HASH_MAP_FACTORY, cf, false);
  }


  /**
   * For testing only.
   *
   * @param args from command line
   */
  public static void main(String[] args) {
    CollectionValuedMap originalMap = new CollectionValuedMap();
    /*
        for (int i=0; i<4; i++) {
          for (int j=0; j<4; j++) {
            originalMap.add(new Integer(i), new Integer(j));
          }
        }
        originalMap.remove(new Integer(2));
        System.out.println("Map: ");
        System.out.println(originalMap);
        System.exit(0);
    */
    Random r = new Random();
    for (int i = 0; i < 800; i++) {
      Integer rInt1 = new Integer(r.nextInt(400));
      Integer rInt2 = new Integer(r.nextInt(400));
      originalMap.add(rInt1, rInt2);
      System.out.println("Adding " + rInt1 + " " + rInt2);
    }
    CollectionValuedMap originalCopyMap = new CollectionValuedMap(originalMap);
    CollectionValuedMap deltaCopyMap = new CollectionValuedMap(originalMap);
    CollectionValuedMap deltaMap = new DeltaCollectionValuedMap(originalMap);
    // now make a lot of changes to deltaMap;
    // add and change some stuff
    for (int i = 0; i < 400; i++) {
      Integer rInt1 = new Integer(r.nextInt(400));
      Integer rInt2 = new Integer(r.nextInt(400) + 1000);
      deltaMap.add(rInt1, rInt2);
      deltaCopyMap.add(rInt1, rInt2);
      System.out.println("Adding " + rInt1 + " " + rInt2);
    }
    // remove some stuff
    for (int i = 0; i < 400; i++) {
      Integer rInt1 = new Integer(r.nextInt(1400));
      Integer rInt2 = new Integer(r.nextInt(1400));
      deltaMap.removeMapping(rInt1, rInt2);
      deltaCopyMap.removeMapping(rInt1, rInt2);
      System.out.println("Removing " + rInt1 + " " + rInt2);
    }
    System.out.println("original: " + originalMap);
    System.out.println("copy: " + deltaCopyMap);
    System.out.println("delta: " + deltaMap);

    System.out.println("Original preserved? " + originalCopyMap.equals(originalMap));
    System.out.println("Delta accurate? " + deltaMap.equals(deltaCopyMap));
  }

}
