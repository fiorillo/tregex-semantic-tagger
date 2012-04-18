package edu.stanford.nlp.util;

import java.io.Serializable;
import java.util.*;

/**
 * A factory class for vending different sorts of Maps.
 *
 * @author Dan Klein (klein@cs.stanford.edu)
 * @author Kayur Patel (kdpatel@cs)
 */
abstract public class MapFactory<K,V> implements Serializable {

  private MapFactory() {
  }

  private static final long serialVersionUID = 4529666940763477360L;

  public static final MapFactory HASH_MAP_FACTORY = new HashMapFactory();

  public static final MapFactory IDENTITY_HASH_MAP_FACTORY = new IdentityHashMapFactory();

  public static final MapFactory WEAK_HASH_MAP_FACTORY = new WeakHashMapFactory();

  public static final MapFactory TREE_MAP_FACTORY = new TreeMapFactory();

  public static final MapFactory ARRAY_MAP_FACTORY = new ArrayMapFactory();


  private static class HashMapFactory<K,V> extends MapFactory<K,V> {

    private static final long serialVersionUID = -9222344631596580863L;

    public Map<K,V> newMap() {
      return new HashMap<K,V>();
    }

    public Map<K,V> newMap(int initCapacity) {
      return new HashMap<K,V>(initCapacity);
    }

    public <K1, V1> Map<K1, V1> setMap(Map<K1,V1> map) {
      map = new HashMap<K1,V1>();
      return map;
    }

    public <K1, V1> Map<K1, V1> setMap(Map<K1,V1> map, int initCapacity) {
      map = new HashMap<K1,V1>(initCapacity);
      return map;
    }

  } // end class HashMapFactory


  private static class IdentityHashMapFactory<K,V> extends MapFactory<K,V> {

    private static final long serialVersionUID = -9222344631596580863L;

    public Map<K,V> newMap() {
      return new IdentityHashMap<K,V>();
    }

    public Map<K,V> newMap(int initCapacity) {
      return new IdentityHashMap<K,V>(initCapacity);
    }

    public <K1, V1> Map<K1, V1> setMap(Map<K1,V1> map) {
      map = new IdentityHashMap<K1,V1>();
      return map;
    }

    public <K1, V1> Map<K1, V1> setMap(Map<K1,V1> map, int initCapacity) {
      map = new IdentityHashMap<K1,V1>(initCapacity);
      return map;
    }

  } // end class IdentityHashMapFactory


  private static class WeakHashMapFactory<K,V> extends MapFactory<K,V> {

    private static final long serialVersionUID = 4790014244304941000L;

    public Map<K,V> newMap() {
      return new WeakHashMap<K,V>();
    }

    public Map<K,V> newMap(int initCapacity) {
      return new WeakHashMap<K,V>(initCapacity);
    }


    public <K1, V1> Map<K1, V1> setMap(Map<K1,V1> map) {
      map = new WeakHashMap<K1,V1>();
      return map;
    }

    public <K1, V1> Map<K1, V1> setMap(Map<K1,V1> map, int initCapacity) {
      map = new WeakHashMap<K1,V1>(initCapacity);
      return map;
    }

  } // end class WeakHashMapFactory


  private static class TreeMapFactory<K,V> extends MapFactory<K,V> {

    private static final long serialVersionUID = -9138736068025818670L;

    public Map<K,V> newMap() {
      return new TreeMap<K,V>();
    }

    public Map<K,V> newMap(int initCapacity) {
      return newMap();
    }


    public <K1, V1> Map<K1, V1> setMap(Map<K1,V1> map) {
      map = new TreeMap<K1,V1>();
      return map;
    }

    public <K1, V1> Map<K1, V1> setMap(Map<K1,V1> map, int initCapacity) {
      map = new TreeMap<K1,V1>();
      return map;
    }

  } // end class TreeMapFactory


  private static class ArrayMapFactory<K,V> extends MapFactory<K,V> {

    private static final long serialVersionUID = -5855812734715185523L;

    public Map<K,V> newMap() {
      return new ArrayMap<K,V>();
    }

    public Map<K,V> newMap(int initCapacity) {
      return new ArrayMap<K,V>(initCapacity);
    }

    public <K1, V1> Map<K1, V1> setMap(Map<K1, V1> map) {
      return new ArrayMap<K1,V1>();
    }

    public <K1, V1> Map<K1, V1> setMap(Map<K1,V1> map, int initCapacity) {
      map = new ArrayMap<K1,V1>(initCapacity);
      return map;
    }

  } // end class ArrayMapFactory


  /**
   * Returns a new non-parameterized map of a particular sort.
   *
   * @return A new non-parameterized map of a particular sort
   */
  abstract public Map<K,V> newMap();

  /**
   * Returns a new non-parameterized map of a particular sort with an initial capacity.
   *
   * @param initCapacity initial capacity of the map
   * @return A new non-parameterized map of a particular sort with an initial capacity
   */
  abstract public Map<K,V> newMap(int initCapacity);

  /**
   * A method to get a parameterized (genericized) map out.
   *
   * @param map A type-parameterized {@link Map} argument
   * @return A {@link Map} with type-parameterization identical to that of
   *         the argument.
   */
  abstract public <K1, V1> Map<K1, V1> setMap(Map<K1,V1> map);

  abstract public <K1, V1> Map<K1, V1> setMap(Map<K1,V1> map, int initCapacity);

}
