package edu.stanford.nlp.trees.tregex.tsurgeon;

import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.tregex.TregexMatcher;

/**
 * @author Roger Levy (rog@stanford.edu)
 */
class HoldTreeNode extends TsurgeonPattern {

  AuxiliaryTree subTree;

  public HoldTreeNode(AuxiliaryTree t) {
    super("hold", new TsurgeonPattern[] {});
    this.subTree = t;
  }

  public Tree evaluate(Tree t, TregexMatcher m) {
    return subTree.copy(this).tree;
  }

  public String toString() {
    return subTree.toString();
  }
}
