package edu.stanford.nlp.util;

import java.util.Comparator;
import java.io.Serializable;

/**
 * ScoredComparator allows one to compare Scored things.
 * There are two ScoredComparators, one which sorts in ascending order and
 * the other in descending order. They are implemented as singletons.
 *
 * @author Dan Klein
 * @author Christopher Manning
 * @version 2006/08/20
 */
final public class ScoredComparator implements Comparator, Serializable {

  private static final long serialVersionUID = 1L;

  private static final boolean ASCENDING = true;
  private static final boolean DESCENDING = false;

  public static final ScoredComparator ASCENDING_COMPARATOR = new ScoredComparator(ASCENDING);

  public static final ScoredComparator DESCENDING_COMPARATOR = new ScoredComparator(DESCENDING);

  private final boolean ascending;

  private ScoredComparator(boolean ascending) {
    this.ascending = ascending;
  }

  public int compare(Object o1, Object o2) {
    if (o1 == o2) {
      return 0;
    }
    double d1 = ((Scored) o1).score();
    double d2 = ((Scored) o2).score();
    if (ascending) {
      if (d1 < d2) {
        return -1;
      }
      if (d1 > d2) {
        return 1;
      }
    } else {
      if (d1 < d2) {
        return 1;
      }
      if (d1 > d2) {
        return -1;
      }
    }
    return 0;
  }

  public boolean equals(Object o) {
    if (o instanceof ScoredComparator) {
      ScoredComparator sc = (ScoredComparator) o;
      if (ascending == sc.ascending) {
        return true;
      }
    }
    return false;
  }

  /**
   * Return the hashCode: there are only two distinct comparators by
   * equals().
   */
  public int hashCode() {
    if (ascending) {
      return (1 << 23);
    } else {
      return (1 << 23) + 1;
    }
  }

  public String toString() {
    return "ScoredComparator(" + (ascending ? "ascending": "descending") + ")";
  }
}
