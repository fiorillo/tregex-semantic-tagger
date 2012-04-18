package edu.stanford.nlp.trees.tregex;

/**
 * @author Roger Levy
 */
class TreePatternReturnValue {
  public TreePatternReturnValue(TreePattern finalNode) {
    this.finalNode = finalNode;
  }

  TreePattern finalNode;
  boolean success;
}
