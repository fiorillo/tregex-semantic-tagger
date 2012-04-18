package edu.stanford.nlp.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Utilities for Maps, including inverting, composing, and support for list/set values.
 * <p/>
 * @author Dan Klein (klein@cs.stanford.edu)
 * Date: Oct 22, 2003
 * Time: 8:56:16 PM
 */
public class Maps {
  /**
   * Adds the value to the HashSet given by map.get(key), creating a new HashMap if needed.
   *
   * @param map
   * @param key
   * @param value
   */
  public static void putIntoValueHashSet(Map map, Object key, Object value) {
    putIntoValueCollection(map, key, value, CollectionFactory.HASH_SET_FACTORY);
  }

  /**
   * Adds the value to the ArrayList given by map.get(key), creating a new ArrayList if needed.
   *
   * @param map
   * @param key
   * @param value
   */
  public static void putIntoValueArrayList(Map map, Object key, Object value) {
    putIntoValueCollection(map, key, value, CollectionFactory.ARRAY_LIST_FACTORY);
  }

  /**
   * Adds the value to the collection given by map.get(key).  A new collection is created using the supplied CollectionFactory.
   *
   * @param map
   * @param key
   * @param value
   * @param cf
   */
  public static void putIntoValueCollection(Map map, Object key, Object value, CollectionFactory cf) {
    Collection c = (Collection) map.get(key);
    if (c == null) {
      c = cf.newCollection();
      map.put(key, c);
    }
    c.add(value);
  }

  /**
   * Compose two maps map1:x->y and map2:y->z to get a map x->z
   *
   * @param map1
   * @param map2
   * @return The composed map
   */
  public static Map compose(Map map1, Map map2) {
    Map composedMap = new HashMap();
    for (Iterator keyI = map1.keySet().iterator(); keyI.hasNext();) {
      Object key = keyI.next();
      composedMap.put(key, map2.get(map1.get(key)));
    }
    return composedMap;
  }

  /**
   * Inverts a map x->y to a map y->x assuming unique preimages.  If they are not unique, you get an arbitrary ones as the values in the inverted map.
   *
   * @param map
   * @return The inverted map
   */
  public static Map invert(Map map) {
    Map invertedMap = new HashMap();
    for (Iterator entryI = map.entrySet().iterator(); entryI.hasNext();) {
      Map.Entry entry = (Map.Entry) entryI.next();
      Object key = entry.getKey();
      Object value = entry.getValue();
      invertedMap.put(value, key);
    }
    return invertedMap;
  }

  /**
   * Inverts a map x->y to a map y->pow(x) not assuming unique preimages.
   *
   * @param map
   * @return The inverted set
   */
  public static Map invertSet(Map map) {
    Map invertedMap = new HashMap();
    for (Iterator entryI = map.entrySet().iterator(); entryI.hasNext();) {
      Map.Entry entry = (Map.Entry) entryI.next();
      Object key = entry.getKey();
      Object value = entry.getValue();
      putIntoValueHashSet(invertedMap, value, key);
    }
    return invertedMap;
  }

  public static void main(String[] args) {
    Map map1 = new HashMap();
    map1.put("a", "1");
    map1.put("b", "2");
    map1.put("c", "2");
    map1.put("d", "4");
    Map map2 = new HashMap();
    map2.put("1", "x");
    map2.put("2", "y");
    map2.put("3", "z");
    System.out.println("map1: " + map1);
    System.out.println("invert(map1): " + Maps.invert(map1));
    System.out.println("invertSet(map1): " + Maps.invertSet(map1));
    System.out.println("map2: " + map2);
    System.out.println("compose(map1,map2): " + Maps.compose(map1, map2));
    Map setValues = new HashMap();
    Map listValues = new HashMap();
    Maps.putIntoValueArrayList(listValues, "a", "1");
    Maps.putIntoValueArrayList(listValues, "a", "1");
    Maps.putIntoValueArrayList(listValues, "a", "2");
    Maps.putIntoValueHashSet(setValues, "a", "1");
    Maps.putIntoValueHashSet(setValues, "a", "1");
    Maps.putIntoValueHashSet(setValues, "a", "2");
    System.out.println("listValues: " + listValues);
    System.out.println("setValues: " + setValues);
  }
}
