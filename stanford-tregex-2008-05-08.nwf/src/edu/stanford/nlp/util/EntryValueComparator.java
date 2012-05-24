package edu.stanford.nlp.util;

import java.util.Comparator;
import java.util.Map;

/**
 * Comparator designed for the values of Map entries.  This is a somewhat
 * hacked ("overloaded") version of a comparator.  But the idea is that
 * you want to be comparing two "values".  There are three cases: what you
 * have might either be a Map.Entry, which is compared based on its value,
 * or it might be a key in the Map optionally associated with the comparator,
 * and then comparison is done based on the associated value, or if neither
 * of the above is true, the object is compared as being itself a value.
 * Values must implement Comparable to be used.
 * Options to the comparator allow sorting in normal or reversed order,
 * and for sorting by unsigned magnitude (which works only if the values
 * extend Number.
 * <p/>
 * Example use (sorts Map of counts or Counter with highest first):<pre>
 *  Map counts = ... // Object key -> Integer/Double count
 *  List entries=new ArrayList(counts.entrySet());
 *  Collections.sort(entries, new EntryValueComparator(false));
 *  </pre>
 *
 * @author Joseph Smarr (jsmarr@stanford.edu)
 * @author Dan Klein
 * @author Christopher Manning (make it do compare properly)
 */
public class EntryValueComparator implements Comparator {

  private boolean ascending; // whether to sort in normal order or reversed
  private boolean useMagnitude;  // whether to sort on abs. value
  private Map m; // map from which to get the values for the keys to sort

  /**
   * Constructs a new EntryValueComparator using ascending (normal) order
   * that works on Map.Entry objects.
   */
  public EntryValueComparator() {
    this(null, true);
  }

  /**
   * Constructs a new EntryValueComparator that will sort in the given order
   * and works on Map.Entry objects.
   */
  public EntryValueComparator(boolean ascending) {
    this(null, ascending);
  }

  /**
   * Constructs a new EntryValueComparator that will sort keys for the given
   * Map in ascending (normal) order. It will also sort Map.Entry objects.
   */
  public EntryValueComparator(Map m) {
    this(m, true);
  }


  /**
   * Constructs a new EmptyValueComparator to sort keys or entries of the
   * given map
   * in the given order.  If <tt>m</tt> is non-null, this Comparator can be
   * used to sort its <tt>keySet()</tt> as well as its <tt>entrySet()</tt>.
   * Otherwise it can only be used on the entries, since there's no way to
   * get the value for a given key.
   *
   * @param m         Map whose keys are to be sorted, or <tt>null</tt> if
   *                  <tt>Map.Entry</tt> objects will be sorted.
   * @param ascending whether to sort in ascending (normal) order or
   *                  descending (reverse) order. Ascending order is alphabetical, descending
   *                  order puts higher numbers first.
   */
  public EntryValueComparator(Map m, boolean ascending) {
    this(m, ascending, false);
  }

  /**
   * Constructs a new EmptyValueComparator to sort keys or entries of the
   * given map
   * in the given order.  If <tt>m</tt> is non-null, this Comparator can be
   * used to sort its <tt>keySet()</tt> as well as its <tt>entrySet()</tt>.
   * Otherwise it can only be used on the entries, since there's no way to
   * get the value for a given key.
   *
   * @param m            Map whose keys are to be sorted, or <tt>null</tt> if
   *                     <tt>Map.Entry</tt> objects will be sorted.
   * @param ascending    whether to sort in ascending (normal) order or
   *                     descending (reverse) order. Ascending order is alphabetical, descending
   *                     order puts higher numbers first.
   * @param useMagnitude Sort values for magnitude (absolute value).
   *                     This only works if the values stored implement Number, and
   *                     their magnitudes are compared according to their double value.
   */
  public EntryValueComparator(Map m, boolean ascending, boolean useMagnitude) {
    this.m = m;
    this.ascending = ascending;
    this.useMagnitude = useMagnitude;
  }


  /**
   * Compares the values of the two given Map.Entry objects in the given
   * order.
   *
   * @return 0 if either object is not a Map.Entry or if their values cannot
   *         be compared.
   */
  public int compare(Object o1, Object o2) {
    Object v1;
    Object v2;
    // System.err.print("Comparing " + o1 + " to " + o2 + ": " );

    if (o1 instanceof Map.Entry) {
      v1 = ((Map.Entry) o1).getValue();
    } else if (m != null) {
      v1 = m.get(o1);
      if (v1 == null) {
        throw new RuntimeException("Key not found in map.");
      }
    } else {
      v1 = o1;
    }
    if (o2 instanceof Map.Entry) {
      v2 = ((Map.Entry) o2).getValue();
    } else if (m != null) {
      v2 = m.get(o2);
      if (v2 == null) {
        throw new RuntimeException("Key not found in map.");
      }
    } else {
      v2 = o2;
    }

    if (useMagnitude) {
      v1 = new Double(Math.abs(((Number) v1).doubleValue()));
      v2 = new Double(Math.abs(((Number) v2).doubleValue()));
    }
    // System.err.println(ascending? compareOr0(v1,v2): compareOr0(v2,v1));
    int result;
    if (ascending) {
      result = ((Comparable) v1).compareTo(v2);
    } else {
      result = ((Comparable) v2).compareTo(v1);
    }
    return result;
  }

}
