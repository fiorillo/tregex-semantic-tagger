package edu.stanford.nlp.trees;


/**
 * HeadFinder that always returns the leftmost daughter as head.  For
 * testing purposes.
 *
 * @author Roger Levy
 */
public class LeftHeadFinder implements HeadFinder {

  public Tree determineHead(Tree t) {
    if (t.isLeaf()) {
      return null;
    } else {
      return t.children()[0];
    }
  }

  public Tree determineHead(Tree t, Tree parent) {
    return determineHead(t);
  }

}
