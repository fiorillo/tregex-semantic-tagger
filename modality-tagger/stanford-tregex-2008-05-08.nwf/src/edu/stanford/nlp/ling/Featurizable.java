package edu.stanford.nlp.ling;

import java.util.Collection;

/**
 * Interface for Objects that can be described by their features.
 *
 * @author Sepandar Kamvar (sdkamvar@stanford.edu)
 */

public interface Featurizable {

  /**
   * returns Object as a Collection of its features
   */
  public Collection asFeatures();

}
