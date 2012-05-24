package edu.stanford.nlp.util;


/**
 * An interface for classes that act as a function transforming one object
 * to another.
 *
 * @author Dan Klein
 */
public interface Function <T1,T2> {

  /**
   * Converts a T1 to a different T2.  For example, a Parser
   * will convert a Sentence to a Tree.  A Tagger will convert a Sentence
   * to a TaggedSentence.
   *
   * @param in The function's argument
   * @return The function's evaluated value
   */
  public T2 apply(T1 in);

}
