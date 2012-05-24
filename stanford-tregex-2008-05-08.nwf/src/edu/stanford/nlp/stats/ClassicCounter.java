package edu.stanford.nlp.stats;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.text.NumberFormat;
import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import edu.stanford.nlp.math.SloppyMath;
import edu.stanford.nlp.util.BinaryHeapPriorityQueue;
import edu.stanford.nlp.util.EntryValueComparator;
import edu.stanford.nlp.util.Factory;
import edu.stanford.nlp.util.Filter;
import edu.stanford.nlp.util.MapFactory;
import edu.stanford.nlp.util.MutableDouble;
import edu.stanford.nlp.util.PriorityQueue;

/**
 * A specialized kind of hash table (or map) for storing numeric counts for
 * objects. It works like a Map,
 * but with different methods for easily getting/setting/incrementing counts
 * for objects and computing various functions with the counts.
 * The Counter constructor
 * and <tt>addAll</tt> method can be used to copy another Counter's contents
 * over. This class also provides access
 * to Comparators that can be used to sort the keys or entries of this Counter
 * by the counts, in either ascending or descending order.
 * <p/>
 * <i>Implementation note:</i> Note that this class stores a
 * <code>totalCount</code> field as well as the map.  This makes certain
 * operations much more efficient, but means that any methods that change the
 * map must also update <code>totalCount</code> appropriately. If you use the
 * <code>setCount</code> method, then you cannot go wrong.
 *
 * @author Dan Klein (klein@cs.stanford.edu)
 * @author Joseph Smarr (jsmarr@stanford.edu)
 * @author Teg Grenager
 * @author Galen Andrew
 * @author Christopher Manning
 * @author Kayur Patel (kdpatel@cs)
 */
public class ClassicCounter<E> implements Serializable, Counter<E>,Iterable<E> {

  Map<E, MutableDouble> map;  // accessed by DeltaCounter
  MapFactory<E, MutableDouble> mapFactory;      // accessed by DeltaCounter
  private double totalCount;
  private double defaultValue = 0.0;

  /**
   * Default comparator for breaking ties in argmin and argmax.
   */
  private static final Comparator hashCodeComparator = new Comparator<Object>() {
      public int compare(Object o1, Object o2) {
        return o1.hashCode() - o2.hashCode();
      }

      public boolean equals(Comparator comporator) {
        return (comporator == this);
      }
  };


  private static final long serialVersionUID = 4;

  // for more efficient memory usage
  private transient MutableDouble tempMDouble = null;

  // CONSTRUCTORS

  /**
   * Constructs a new (empty) Counter.
   */
  public ClassicCounter() {
    this(MapFactory.HASH_MAP_FACTORY);
  }

  /**
   * Pass in a MapFactory and the map it vends will back your counter.
   */
  public ClassicCounter(MapFactory<E,MutableDouble> mapFactory) {
    this.mapFactory = mapFactory;
    this.map = mapFactory.newMap();

    totalCount = 0.0;
  }

  /**
   * Constructs a new Counter with the contents of the given Counter.
   */
  public ClassicCounter(Counter<E> c) {
    this();
    Counters.addInPlace(this, c);
    defaultReturnValue(c.defaultReturnValue());
  }

  /**
   * Constructs a new Counter by counting the elements in the given Collection.
   */
  public ClassicCounter(Collection<E> collection) {
    this();
    for (E key : collection) {
      incrementCount(key);
    }
  }


  // STANDARD ACCESS MODIFICATION METHODS

  private MapFactory<E,MutableDouble> getMapFactory() {
    return mapFactory;
  }


  //
  // Methods needed by Counter interface
  //

  public void defaultReturnValue(double rv) { defaultValue = rv; }

  public double defaultReturnValue() { return defaultValue; }

  public Factory<Counter<E>> getFactory() {
    return new Factory<Counter<E>>() {
      public Counter<E> create() {
        return new ClassicCounter<E>(getMapFactory());
      }
    };
  }

  /**
   * Returns the current count for the given key, which is 0 if it hasn't
   * been
   * seen before. This is a convenient version of <code>get</code> that casts
   * and extracts the primitive value.
   */

  public double getCount(E key) {
    Number count = map.get(key);
    if (count == null) {
      return defaultValue; // haven't seen this object before -> 0 count
    }
    return count.doubleValue();
  }

  /**
   * Sets the current count for the given key. This will wipe out any existing
   * count for that key.
   * <p/>
   * To add to a count instead of replacing it, use
   * {@link #incrementCount(Object,double)}.
   */
  public void setCount(E key, double count) {
    if (tempMDouble == null) {
      //System.out.println("creating mdouble");
      tempMDouble = new MutableDouble();
    }
    //System.out.println("setting mdouble");
    tempMDouble.set(count);
    //System.out.println("putting mdouble in map");
    tempMDouble = map.put(key, tempMDouble);
    //System.out.println("placed mDouble in map");

    totalCount += count;
    if (tempMDouble != null) {
      totalCount -= tempMDouble.doubleValue();
    }
  }

  /**
   * Removes the given key from this Counter. Its count will now be 0 and it
   * will no longer be considered previously seen. If a key not contained in
   * the Counter is given, no action is performed on the Counter and
   * Double.NaN.  This behavior echoes that of HashMap, but differs since
   * a HashMap returns a Double (rather than double) and thus returns null
   * if a key is not present.  Any future revisions of Counter should preserve
   * the ability to "remove" a key that is not present in the Counter.
   */
  public double remove(E key) {
    MutableDouble d = mutableRemove(key);
    if(d != null)
      return d.doubleValue();
    else
      return Double.NaN;
  }

  public boolean containsKey(E key) {
    return map.containsKey(key);
  }

  public Set<E> keySet() {
    return map.keySet();
  }

  public Collection<Double> values() {
    return new AbstractCollection<Double>() {
      @Override
      public Iterator<Double> iterator() {
        return new Iterator<Double>() {
          Iterator<MutableDouble> inner = map.values().iterator();

          public boolean hasNext() {
            return inner.hasNext();
          }

          public Double next() {
            return new Double(inner.next().doubleValue());
          }

          public void remove() {
            throw new UnsupportedOperationException();
          }
        };
      }

      @Override
      public int size() {
        return map.size();
      }

      @Override
      public boolean contains(Object v) {
        return v instanceof Double && map.values().contains(new MutableDouble(((Double) v).doubleValue()));
      }

    };
  }

  /**
   * Removes all counts from this Counter.
   */
  public void clear() {
    map.clear();
    totalCount = 0.0;
  }

  /**
   * Returns the number of keys stored in the counter.
   *
   * @return The number of keys
   */
  public int size() {
    return map.size();
  }

  /**
   * Returns the current total count for all objects in this Counter.
   */
  public double totalCount() {
    return totalCount;
  }

  public Iterator<E> iterator() {
    return keySet().iterator();
  }

  // MAP LIKE OPERATIONS

  private MutableDouble mutableRemove(E key) {
    MutableDouble md = map.remove(key);
    if (md != null) {
      totalCount -= md.doubleValue();
    }
    return md;
  }

  /**
   * Removes all the given keys from this Counter.
   * Keys may be included that are not actually in the
   * Counter - no action is taken in response to those
   * keys.  This behavior should be retained in future
   * revisions of Counter (matches HashMap).
   */
  public void removeAll(Collection<E> keys) {
    for (E key : keys) {
      mutableRemove(key);
    }
  }

  public boolean isEmpty() {
    return (size() == 0);
  }

  /**
   * Returns a view of the doubles in this map.  Can be safely modified.
   */
  public Set<Map.Entry<E,Double>> entrySet() {
    return new AbstractSet<Map.Entry<E,Double>>() {
      @Override
      public Iterator<Entry<E, Double>> iterator() {
        return new Iterator<Entry<E,Double>>() {
          final Iterator<Entry<E,MutableDouble>> inner = map.entrySet().iterator();

          public boolean hasNext() {
            return inner.hasNext();
          }

          public Entry<E, Double> next() {
            return new Entry<E,Double>() {
              final Entry<E,MutableDouble> e = inner.next();

              public double getDoubleValue() {
                return e.getValue().doubleValue();
              }

              public double setValue(double value) {
                final double old = e.getValue().doubleValue();
                e.getValue().set(value);
                totalCount = totalCount - old + value;
                return old;
              }

              public E getKey() {
                return e.getKey();
              }

              public Double getValue() {
                return getDoubleValue();
              }

              public Double setValue(Double value) {
                return setValue(value.doubleValue());
              }
            };
          }

          public void remove() {
            throw new UnsupportedOperationException();
          }
        };
      }

      @Override
      public int size() {
        return map.size();
      }
    };
  }

  // OBJECT STUFF

  public boolean equals(Object o) {
    if (this == o) {
      return true;
    } else if (!(o instanceof Counter)) {
      return false;
    } else if (!(o instanceof ClassicCounter)) {
      return Counters.equals(this, (Counter<E>)o);
    }

    final ClassicCounter<E> counter = (ClassicCounter<E>) o;
    return totalCount == counter.totalCount && map.equals(counter.map);
  }

  public int hashCode() {
    return map.hashCode();
  }

  public String toString() {
    return map.toString();
  }

  /**
   * Returns a string representation which includes no more than the
   * maxKeysToPrint elements with largest counts.
   *
   * @param maxKeysToPrint
   * @return partial string representation
   */
  public String toString(int maxKeysToPrint) {
    return asBinaryHeapPriorityQueue().toString(maxKeysToPrint);
  }

  /** Pretty print a Counter. This one has more flexibility in formatting,
   *  and doesn't sort the keys.
   */
  public String toString(NumberFormat nf, String preAppend, String postAppend,
                         String keyValSeparator, String itemSeparator) {
    StringBuilder sb = new StringBuilder();
    sb.append(preAppend);
    // List<E> list = new ArrayList<E>(map.keySet());
    //     try {
    //       Collections.sort(list); // see if it can be sorted
    //     } catch (Exception e) {
    //     }
    for (Iterator<E> iter = map.keySet().iterator(); iter.hasNext(); ) {
      E key = iter.next();
      MutableDouble d = map.get(key);
      sb.append(key);
      sb.append(keyValSeparator);
      sb.append(nf.format(d));
      if (iter.hasNext()) {
        sb.append(itemSeparator);
      }
    }
    sb.append(postAppend);
    return sb.toString();
  }


  /**
   * Pretty print a Counter.  This version tries to sort the keys.
   * @deprecated Use {@link Counters#toString(Counter, NumberFormat)}
   */
  @Deprecated public String toString(NumberFormat nf) {
    return Counters.toString(this,nf);
  }

  public Object clone() {
    return new ClassicCounter<E>(this);
  }



  //
  // Deprecated utility functions - use the methods in Counters instead.
  //

  /**
   * Returns the current total count for all objects in this Counter.
   *
   * @deprecated Use {@link ClassicCounter#totalCount()}.
   */
  @Deprecated public double totalDoubleCount() {
    return totalCount();
  }

  /**
   * Returns the total count for all objects in this Counter that pass the
   * given Filter. Passing in a filter that always returns true is equivalent
   * to calling {@link #totalCount()}.
   *
   * @deprecated Use own loop?
   */
  @Deprecated public double totalCount(Filter<E> filter) {
    double total = 0.0;
    for (E key : map.keySet()) {
      if (filter.accept(key)) {
        total += getCount(key);
      }
    }
    return (total);
  }

  /**
   * @deprecated Use {@link Counters#logSum(Counter)}.
   */
  @Deprecated public double logSum() {
    return Counters.logSum(this);
  }

  /**
   * @deprecated Use {@link Counters#logNormalize(Counter)}.
   */
  @Deprecated public void logNormalize() {
    Counters.logNormalize(this);
  }

  /**
   * Returns the mean of all the counts (totalCount/size).
   *
   * @deprecated Use {@link Counters#mean(Counter)}.
   */
  @Deprecated public double averageCount() {
    return Counters.mean(this);
  }

  /**
   * @deprecated Use Double.toString
   */
  @Deprecated public String getCountAsString(E key) {
    return Double.toString(getCount(key));
  }

  /**
   * @deprecated Use setCount after Double.parseDouble
   */
  @Deprecated public void setCount(E key, String s) {
    setCount(key, Double.parseDouble(s));
  }

  /** Return the proportion of the Counter mass under this key.
   *
   *  This has been de-deprecated in order to reduce compilation warnings, but
   *  really you should create a {@link Distribution} instead of using this
   *  method.
   *
   *  @deprecated Use a Distribution or divide getCount by totalCount
   */
  @Deprecated public double getNormalizedCount(E key) {
    return getCount(key) / totalCount();
  }

  /**
   * Sets the current count for each of the given keys. This will wipe out
   * any existing counts for these keys.
   *
   * @deprecated use own loop
   */
  @Deprecated private void setCounts(Collection<E> keys, double count) {
    for (E key : keys) {
      setCount(key, count);
    }
  }

  /**
   * Adds the given count to the current count for the given key. If the key
   * hasn't been seen before, it is assumed to have count 0, and thus this
   * method will set its count to the given amount. Negative increments are
   * equivalent to calling <tt>decrementCount</tt>.
   * <p/>
   * To more conveniently increment the count by 1.0, use
   * {@link #incrementCount(Object)}.
   * <p/>
   * To set a count to a specifc value instead of incrementing it, use
   * {@link #setCount(Object,double)}.
   *
   * @return Value of incremented key (post-increment)
   */
  public double incrementCount(E key, double count) {
    if (tempMDouble == null) {
      tempMDouble = new MutableDouble();
    }
    MutableDouble oldMDouble = map.put(key, tempMDouble);
    totalCount += count;
    if (oldMDouble != null) {
      count += oldMDouble.doubleValue();
    }
    tempMDouble.set(count);
    tempMDouble = oldMDouble;

    return count;
  }

  /**
   * If the current count for the object is c1, and you call
   * logIncrementCount with a value of c2, then the new value will
   * be log(e^c1 + e^c2). If the key
   * hasn't been seen before, it is assumed to have count Double.NEGATIVE_INFINITY,
   * and thus this
   * method will set its count to the given amount.
   * To set a count to a specifc value instead of incrementing it, use
   * {@link #setCount(Object,double)}.
   *
   * @return Value of incremented key (post-increment)
   */
  public double logIncrementCount(E key, double count) {
    if (tempMDouble == null) {
      tempMDouble = new MutableDouble();
    }
    MutableDouble oldMDouble = map.put(key, tempMDouble);
    if (oldMDouble != null) {
      count = SloppyMath.logAdd(count, oldMDouble.doubleValue());
      totalCount += count - oldMDouble.doubleValue();
    } else {
      totalCount += count;
    }
    tempMDouble.set(count);
    tempMDouble = oldMDouble;

    return count;
  }


  /**
   * Adds 1.0 to the count for the given key. If the key hasn't been seen
   * before, it is assumed to have count 0, and thus this method will set
   * its count to 1.0.
   * <p/>
   * To increment the count by a value other than 1.0, use
   * {@link #incrementCount(Object,double)}.
   * <p/>
   * To set a count to a specifc value instead of incrementing it, use
   * {@link #setCount(Object,double)}.
   */
  public double incrementCount(E key) {
    return incrementCount(key, 1);
  }

    /**
   * Adds the given count to the current counts for each of the given keys.
   * If any of the keys haven't been seen before, they are assumed to have
   * count 0, and thus this method will set their counts to the given
   * amount. Negative increments are equivalent to calling
   * <tt>decrementCounts</tt>.
   *
   * @deprecated Use your own loop
   */
  @Deprecated public void incrementCounts(Collection<E> keys, double count) {
    for (E key : keys) {
      incrementCount(key, count);
    }
  }

  /**
   * Adds 1.0 to the counts for each of the given keys. If any of the keys
   * haven't been seen before, they are assumed to have count 0, and thus
   * this method will set their counts to 1.0.
   *
   * @deprecated Use your own loop
   */
  @Deprecated public void incrementCounts(Collection<E> keys) {
    for (E key : keys) {
      incrementCount(key, 1.0);
    }
  }

  /**
   * Adds the same amount to every count, that is to every key currently
   * stored in the counter (with no lookups).
   *
   * @param count The amount to be added
   */
  public void incrementAll(double count) {
    for (MutableDouble md : map.values()) {
      md.set(md.doubleValue() + count);
      totalCount += count;
    }
  }

  /**
   * Subtracts the given count from the current count for the given key.
   * If the key hasn't been seen before, it is assumed to have count 0, and
   * thus this  method will set its count to the negative of the given amount.
   * Negative increments are equivalent to calling <tt>incrementCount</tt>.
   * <p/>
   * To more conviently decrement the count by 1.0, use
   * {@link #decrementCount(Object)}.
   * <p/>
   * To set a count to a specifc value instead of decrementing it, use
   * {@link #setCount(Object,double)}.
   */
  public double decrementCount(E key, double count) {
    return incrementCount(key, -count);
  }

  /**
   * Subtracts 1.0 from the count for the given key. If the key hasn't been
   * seen  before, it is assumed to have count 0, and thus this method will
   * set its count to -1.0.
   * <p/>
   * To decrement the count by a value other than 1.0, use
   * {@link #decrementCount(Object,double)}.
   * <p/>
   * To set a count to a specifc value instead of decrementing it, use
   * {@link #setCount(Object,double)}.
   */
  public double decrementCount(E key) {
    return incrementCount(key, -1);
  }

  /**
   * Subtracts the given count from the current counts for each of the given keys.
   * If any of the keys haven't been seen before, they are assumed to have
   * count 0, and thus this method will set their counts to the negative of the given
   * amount. Negative increments are equivalent to calling <tt>incrementCount</tt>.
   *
   * @deprecated Use your own loop
   */
  @Deprecated public void decrementCounts(Collection<E> keys, double count) {
    for (E key : keys) {
      incrementCount(key, -count);
    }
  }

  /**
   * Subtracts 1.0 from the counts of each of the given keys. If any of the
   * keys haven't been seen before, they are assumed to have count 0, and thus
   * this method will set their counts to -1.0.
   *
   * @deprecated Use your own loop
   */
  @Deprecated public void decrementCounts(Collection<E> keys) {
    decrementCounts(keys, 1.0);
  }

  /**
   * Adds the counts in the given Counter to the counts in this Counter.
   * <p/>
   * To copy the values from another Counter rather than adding them, use
   *
   * @deprecated Use {@link Counters#addInPlace(Counter, Counter)}.
   */
  @Deprecated public void addAll(Counter<E> counter) {
    Counters.addInPlace(this, counter);
  }

  /**
   * Adds the counts in the given Counter to the counts in this Counter.
   *
   * @deprecated Use {@link Counters#addInPlace(Counter, Counter, double)}
   */
  @Deprecated public void addMultiple(Counter<E> counter, double d) {
    Counters.addInPlace(this,counter,d);
  }

  /**
   * Subtracts the counts in the given Counter to the counts in this Counter.
   * <p/>
   * To copy the values from another Counter rather than adding them, use
   *
   * @deprecated Use {@link Counters#subtractInPlace(Counter, Counter)}
   */
  @Deprecated public void subtractAll(Counter<E> counter) {
    Counters.subtractInPlace(this, counter);
  }

  /**
   * Subtracts the counts in the given Counter to the counts in this Counter.
   *
   * @deprecated Use {@link Counters#addInPlace(Counter, Counter, double)}
   */
  @Deprecated public void subtractMultiple(Counter<E> counter, double d) {
    Counters.addInPlace(this,counter,-d);
  }

  /**
   * Calls incrementCount(key) on each key in the given collection.
   *
   * @deprecated Use own loop.
   */
  @Deprecated public void addAll(Collection<E> collection) {
    for (E key : collection) {
      incrementCount(key);
    }
  }


  /**
   * Multiplies every count by the given multiplier.
   *
   * @deprecated Use {@link Counters#scaleInPlace(Counter, double)}
   */
  @Deprecated public void multiplyBy(double multiplier) {
    Counters.scaleInPlace(this,multiplier);
  }

  /**
   * Divides every count by the given divisor.
   *
   * @deprecated Use {@link Counters#scaleInPlace(Counter, double)}
   */
  @Deprecated public void divideBy(double divisor) {
    Counters.scaleInPlace(this, 1.0 / divisor);
  }

  /**
   * Divides every non-zero count by the count for the corresponding key in
   * the argument Counter.
   * Beware that this can give NaN values for zero counts in the argument
   * counter!
   *
   * @param counter Entries in argument scale individual keys in this counter
   *
   * @deprecated Use {@link Counters#divideInPlace(Counter, Counter)}
   */
  @Deprecated public void divideBy(ClassicCounter<E> counter) {
    Counters.divideInPlace(this,counter);
  }

  /**
   * Subtracts the counts in the given Counter from the counts in this Counter.
   * <p/>
   * To copy the values from another Counter rather than subtracting them, use
   *
   * @deprecated Use {@link Counters#subtractInPlace(Counter, Counter)}
   *  and then {@link Counters#retainNonZeros(Counter)}
   */
  public void subtractAll(Counter<E> counter, boolean removeZeroKeys) {
    Counters.subtractInPlace(this,counter);
    if(removeZeroKeys)
      Counters.retainNonZeros(this);
  }

  // EXTRA CALCULATION METHODS

  /**
   * This has been de-deprecated in order to reduce compilation warnings, but
   * really you should create a {@link Distribution}
   * instead.
   *
   * @deprecated Use Counters#normalize(Counter)
   */
  @Deprecated public void normalize() {
    Counters.normalize(this);
  }

  /**
   * Removes all keys whose count is 0. After incrementing and decrementing
   * counts or adding and subtracting Counters, there may be keys left whose
   * count is 0, though normally this is undesirable. This method cleans up
   * the map.
   * <p/>
   * Maybe in the future we should try to do this more on-the-fly, though it's
   * not clear whether a distinction should be made between "never seen" (i.e.
   * null count) and "seen with 0 count". Certainly there's no distinction in
   * getCount() but there is in containsKey().
   *
   * @deprecated Use {@link Counters#retainNonZeros(Counter)}
   */
  @Deprecated public void removeZeroCounts() {
    Counters.retainNonZeros(this);
  }

  /**
   * Builds a priority queue whose elements are the counter's elements, and
   * whose priorities are those elements' counts in the counter.
   *
   * @deprecated Use {@link Counters#toPriorityQueue(Counter)}
   */
  @Deprecated public PriorityQueue<E> asPriorityQueue() {
    return Counters.toPriorityQueue(this);
  }

  /**
   * Builds a priority queue whose elements are the counter's elements, and
   * whose priorities are those elements' counts in the counter.
   */
  private BinaryHeapPriorityQueue<E> asBinaryHeapPriorityQueue() {
    BinaryHeapPriorityQueue<E> pq = new BinaryHeapPriorityQueue<E>();
    for (Map.Entry<E, MutableDouble> entry : map.entrySet()) {
      pq.add(entry.getKey(), entry.getValue().doubleValue());
    }
    return pq;
  }

  /**
   * @deprecated Use {@link Counters#retainTop}
   */
  @Deprecated public void retainTop(int num) {
    Counters.retainTop(this,num);
  }

  /**
   * Returns the set of keys whose counts are at or above the given threshold.
   * This set may have 0 elements but will not be null.
   * @deprecated Use {@link Counters#keysAbove(Counter, double)}
   */
  @Deprecated public Set<E> keysAbove(double countThreshold) {
    return Counters.keysAbove(this,countThreshold);
  }

  /**
   * Returns the set of keys whose counts are at or below the given threshold.
   * This set may have 0 elements but will not be null.
   * @deprecated Use {@link Counters#keysBelow(Counter, double)}
   */
  @Deprecated public Set<E> keysBelow(double countThreshold) {
    return Counters.keysBelow(this,countThreshold);
  }

  /**
   * Returns the set of keys that have exactly the given count.
   * This set may have 0 elements but will not be null.
   * @deprecated Use {@link Counters#keysAt(Counter, double)}
   */
  @Deprecated public Set<E> keysAt(double count) {
    return Counters.keysAt(this,count);
  }

  /**
   * Returns a comparator suitable for sorting this Counter's keys or entries
   * by their respective counts. If <tt>ascending</tt> is true, lower counts
   * will be returned first, otherwise higher counts will be returned first.
   * <p/>
   * Sample usage:
   * <pre>
   * Counter c = new Counter();
   * // add to the counter...
   * List biggestKeys = new ArrayList(c.keySet());
   * Collections.sort(biggestKeys, c.comparator(false));
   * List smallestEntries = new ArrayList(c.entrySet());
   * Collections.sort(smallestEntries, c.comparator(true))
   * </pre>
   * @deprecated Use {@link Counters#toComparator} and {@link Collections#reverseOrder(Comparator)} if necessary.
   */
  @Deprecated public Comparator<E> comparator(boolean ascending) {
    return (new EntryValueComparator(map, ascending));
  }

  /**
   * Returns a comparator suitable for sorting this Counter's keys or entries
   * by their respective value or magnitude (unsigned value).
   * If <tt>ascending</tt> is true, smaller magnitudes will
   * be returned first, otherwise higher magnitudes will be returned first.
   * <p/>
   * Sample usage:
   * <pre>
   * Counter c = new Counter();
   * // add to the counter...
   * List biggestKeys = new ArrayList(c.keySet());
   * Collections.sort(biggestKeys, c.comparator(false, true));
   * List smallestEntries = new ArrayList(c.entrySet());
   * Collections.sort(smallestEntries, c.comparator(true))
   * </pre>
   * @deprecated Use {@link Counters#toComparator}
   */
  @Deprecated public Comparator<E> comparator(boolean ascending, boolean useMagnitude) {
    return (new EntryValueComparator(map, ascending, useMagnitude));
  }

  /**
   * Comparator that sorts objects by (increasing) count. Shortcut for calling
   * {@link #comparator(boolean) comparator(true)}.
   * @deprecated Use {@link Counters#toComparator}
   */
  @Deprecated public Comparator<E> comparator() {
    return (comparator(true));
  }

  /**
   * Comparator that uses natural ordering.
   * Returns 0 if o1 is not Comparable.
   */
  private static class NaturalComparator implements Comparator {
    public NaturalComparator() {}
    public String toString() { return "NaturalComparator"; }
    public int compare(Object o1, Object o2) {
      if (o1 instanceof Comparable) {
        return (((Comparable) o1).compareTo(o2));
      }
      return 0; // soft-fail
    }
  }

  /**
   * Returns the Counter over Strings specified by this String.
   * The String is normally the whole contents of a file.
   * Format is one entry per line, which is "String key\tdouble value".
   * @param s A String representation of a Counter, where the entries are
   *     one-per-line ('\n') and each line is key \t value
   * @return The Counter with String keys
   */
  public static ClassicCounter<String> valueOf(String s) {
    ClassicCounter<String> result = new ClassicCounter<String>();
    String[] lines = s.split("\n");
    for (String line : lines) {
      String[] fields = line.split("\t");
      if (fields.length!=2) throw new RuntimeException("Got unsplittable line: \"" + line + "\"");
      result.setCount(fields[0], Double.parseDouble(fields[1]));
    }
    return result;
  }

  /**
   * Similar to valueOf in that it returns the Counter over Strings specified by this String.
   * String is again normally the whole contents of a file, but in this case
   * the file can include comments if each line of comment starts with a hash (#) symbol.
   * Otherwise, format is one entry per line, "String key\tdouble value".
   * @param s String representation of a coounter, where entries are one per line such that each
   *  line is either a comment (begins with #) or key \t value
   * @return The Counter with String keys
   */
  public static ClassicCounter<String> valueOfIgnoreComments(String s) {
      ClassicCounter<String> result = new ClassicCounter<String>();
      String[] lines = s.split("\n");
      for (String line : lines) {
        if(line.startsWith("#")) continue;
        String[] fields = line.split("\t");
        if (fields.length!=2) throw new RuntimeException("Got unsplittable line: \"" + line + "\"");
        result.setCount(fields[0], Double.parseDouble(fields[1]));
      }
      return result;
    }


  /**
   * converts from format printed by toString method back into
   * a Counter<String>.
   */
  public static ClassicCounter<String> fromString(String s) {
    ClassicCounter<String> result = new ClassicCounter<String>();
    if (!s.startsWith("{") || !s.endsWith("}")) {
      throw new RuntimeException("invalid format: ||"+s+"||");
    }
    s = s.substring(1, s.length()-1);
    String[] lines = s.split(", ");
    for (String line : lines) {
      String[] fields = line.split("=");
      if (fields.length!=2) throw new RuntimeException("Got unsplittable line: \"" + line + "\"");
      result.setCount(fields[0], Double.parseDouble(fields[1]));
    }
    return result;
  }


  /**
   * For internal debugging purposes only.
   */
  public static void main(String[] args) throws Exception {
    ClassicCounter<String> c = new ClassicCounter<String>();
    c.setCount("p", 0);
    c.setCount("q", 2);
    ClassicCounter<String> small_c = new ClassicCounter<String>(c);
    System.out.println(c + " -> " + c.totalCount() + " should be {p=0.0, q=2.0} -> 2.0");
    c.incrementCount("p");
    System.out.println(c + " -> " + c.totalCount() + " should be {p=1.0, q=2.0} -> 3.0");
    c.incrementCount("p", 2.0);
    System.out.println(Counters.min(c) + " " + Counters.min(c) + " should be 2.0 q");
    c.setCount("w", -5);
    c.setCount("x", -2.5);
    List<String> biggestKeys = new ArrayList<String>(c.keySet());
    Collections.sort(biggestKeys, c.comparator(false, true));
    System.out.println(biggestKeys + " should be [w, p, x, q]");
    System.out.println(c + " (c) should be {p=3.0, q=2.0, w=-5.0, x=-2.5}");
    System.out.println(Counters.min(c) + " " + Counters.argmin(c) + " should be -5 w");
    System.out.println(Counters.max(c) + " " + Counters.argmax(c) + " should be 3 p");
    System.out.println(Counters.mean(c) + " should be -0.625");

    ClassicCounter<String> c2 = new ClassicCounter<String>(c);
    System.out.println(c2 + " (c2) should be {p=3.0, q=2.0, w=-5.0, x=-2.5}");

    ClassicCounter<String> c3 = new ClassicCounter<String>(c2.keySet());
    System.out.println(c3 + " (c3) should be {p=1.0, q=1.0, w=1.0, x=1.0}");

    c2.addMultiple(c3,10);
    System.out.println(c2 + " (c2 = c2+c3*10) should be {p=13.0, q=12.0, w=5.0, x=7.5}");

    c3.addAll(c);
    System.out.println(c3 + " (c3 += c) should be {p=4.0, q=3.0, w=-4.0, x=-1.5}");

    c3.subtractAll(c);
    System.out.println(c3 + " (c3 -= c) should be {p=1.0, q=1.0, w=1.0, x=1.0}");

    c3.addAll(c.keySet());
    System.out.println(c3 + " (c3 += keys(c)) should be {p=2.0, q=2.0, w=2.0, x=2.0}");

    c2.divideBy(c3);
    System.out.println(c2 + " (c2 = c2/c3) should be {p=6.5, q=6.0, w=2.5, x=3.75}");

    c2.divideBy(0.5);
    System.out.println(c2 + " (c2 = c2/.5) should be {p=13.0, q=12.0, w=5.0, x=7.5}");
    System.out.println(c2.getNormalizedCount("w") + " should be 0.13333");

    c2.multiplyBy(2);
    System.out.println(c2 + " (c2 *= 2) should be {p=26.0, q=24.0, w=10.0, x=15}");
    c2.divideBy(2);
    System.out.println(c2 + " (c2 /= 2) should be {p=13.0, q=12.0, w=5.0, x=7.5}");

    c2.incrementAll(1.0);
    System.out.println(c2 + " (c2 = +1) should be {p=14.0, q=13.0, w=6.0, x=8.5}");

    c2.incrementCounts(small_c.keySet());
    System.out.println(c2 + " (c2 += small_c) should be {p=15.0, q=14.0, w=6.0, x=8.5}");

    System.out.println(c2.keysAbove(14) + " should be p q");
    System.out.println(c2.keysAt(14) + " should be q");
    System.out.println(c2.keysBelow(8.5) + " should be x w");

    c2.subtractMultiple(small_c,6);
    System.out.println(c2 + " (c2 -= c_small*10) should be {w=6.0, p=15.0, q=2.0, x=8.5}");

    c2.subtractAll(small_c,true);
    System.out.println(c2 + " (c2 -= 1) should be {w=6.0, p=15.0, x=8.5}");

    if (args.length > 0) {
      // serialize to  file
      ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(args[0])));
      out.writeObject(c);
      out.close();

      // reconstitute
      ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(args[0])));
      c = (ClassicCounter<String>) in.readObject();
      in.close();
      System.out.println(c + " -> " + c.totalCount() +
                         " should be same -> -2.5");
      //c.put("p",new Integer(3));
      System.out.println(Counters.min(c) + " " + Counters.argmin(c) + " should be -5 w");
      c.clear();
      System.out.println(c+" -> "+c.totalCount() + " should be {} -> 0");
    }
  }

}
