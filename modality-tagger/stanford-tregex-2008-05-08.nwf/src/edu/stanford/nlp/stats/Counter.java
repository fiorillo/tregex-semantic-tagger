package edu.stanford.nlp.stats;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import edu.stanford.nlp.util.Factory;

/**
 * Basic Object to double map.  Utility functions are contained in
 * {@link Counters}.  Class previously known as Counter has been
 * renamed ot {@link ClassicCounter} and many of its functions deprecated
 * in preference to those in {@link Counters}.  The preferred Counter
 * implementation is OpenAddressCounter.
 *
 * @author dramage
 * @author cer
 */
public interface Counter<E> {

  /**
   * Returns a factory that can create new instances of this kind of Counter.
   *
   * @return A factory that can create new instances of this kind of Counter.
   */
  public Factory<Counter<E>> getFactory();

  /**
   * Sets the default return value.
   *
   * @param rv The default value
   */
  public void defaultReturnValue(double rv) ;

  /**
   * Returns the default return value.
   *
   * @return The default return value.
   */
  public double defaultReturnValue() ;

  /**
   * Returns the count for this key as a double.
   *
   * @param key The key
   * @return The count
   */
  public double getCount(E key);

  /**
   * Sets the count for this key to be the given value.
   *
   * @param key The key
   * @param value The count
   */
  public void setCount(E key, double value);

  /**
   * Increments the count for this key by the given value.
   *
   * @param key The key to increment
   * @param value The amount to increment it by
   * @return The value associated with they key, post-increment.
   */
  public double incrementCount(E key, double value);

  /**
   * Increments the count for this key by 1.0.
   *
   * @param key The key to increment by 1.0
   * @return The value associated with they key, post-increment.
   */
  public double incrementCount(E key);

  /**
   * Decrements the count for this key by the given value.
   *
   * @param key The key to decrement
   * @param value The amount to decrement it by
   * @return The value associated with they key, post-decrement.
   */
  public double decrementCount(E key, double value);

  /**
   * Decrements the count for this key by 1.0.
   *
   * @param key The key to decrement by 1.0
   * @return The value of associated with they key, post-decrement.
   */
  public double decrementCount(E key);

  /**
   * log space increments the count for this key by the given value.
   *
   * @param key The key to increment
   * @param value The amount to increment it by, in log space
   * @return The value associated with they key, post-increment, in log space
   */
  public double logIncrementCount(E key, double value);

  /**
   * Removes the value associated with the given key.
   *
   * @return The value removed from the map or the default value if no
   *   count was associated with that key.
   */
  public double remove(E key);

  /**
   * @return true iff key is a key in this GenericCounter.
   */
  public boolean containsKey(E key);

  /**
   * Returns the Set of keys in this counter.
   *
   * @return The  Set of keys in this counter.
   */
  public Set<E> keySet();

  /**
   * Returns a copy of the values currently in this counter.
   */
  public Collection<Double> values();

  /**
   * Returns a view of the entries in this counter
   */
  public Set<Map.Entry<E,Double>> entrySet();

  /**
   * Removes all entries from the counter
   */
  public void clear();

  /**
   * Returns the number of entries in this counter.
   */
  public int size();

  /**
   * Computes the total of all counts in this counter, and returns it
   * as a double.
   *
   * @return The total of all counts in this counter
   */
  public double totalCount();

}
