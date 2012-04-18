package edu.stanford.nlp.stats;

/**
 * A strategy-type interface for specifying a function from {@link Object}s
 * to their equivalence classes.
 *
 * @author Roger Levy
 * @see EquivalenceClassEval
 */

public interface EquivalenceClasser <T> {
  public Object equivalenceClass(T o);
}
