package edu.stanford.nlp.ling;

import java.io.Serializable;


/**
 * Interface for Objects which can be described by their features.
 * These objects can also be Serialized (for insertion into a file database).
 *
 * @author Sepandar Kamvar (sdkamvar@stanford.edu)
 */
public abstract interface Datum extends Serializable, Featurizable, Labeled {
}




