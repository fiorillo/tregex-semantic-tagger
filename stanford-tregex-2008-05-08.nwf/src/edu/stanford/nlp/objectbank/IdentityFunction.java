package edu.stanford.nlp.objectbank;

import edu.stanford.nlp.util.Function;


/**
 * @author Jenny Finkel
 */

public class IdentityFunction<X> implements Function<X, X> {

  /**
   * @param o The Object to be returned
   * @return o
   */
  public X apply(X o) {
    return o;
  }

}
