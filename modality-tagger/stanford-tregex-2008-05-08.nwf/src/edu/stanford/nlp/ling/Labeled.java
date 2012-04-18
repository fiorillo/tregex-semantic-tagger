package edu.stanford.nlp.ling;

import java.util.Collection;

/**
 * Interface for Objects that have a label, whose label is an Object.
 * There are only two methods: Object label() and Collection labels().
 * If there is only one label, labels() will return a collection of one label.
 * If there are multiple labels, label() will return the primary label,
 * or a consistent arbitrary label if there is not primary label.
 *
 * @author Sepandar Kamvar (sdkamvar@stanford.edu)
 */
public interface Labeled {

  /**
   * Returns the primary label for this Object, or null if none have been set.
   */
  public Object label();

  /**
   * Returns the complete list of labels for this Object, which may be empty.
   */
  public Collection labels();

}
