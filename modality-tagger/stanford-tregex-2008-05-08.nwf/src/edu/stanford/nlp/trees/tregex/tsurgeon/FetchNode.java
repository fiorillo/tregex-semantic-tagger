package edu.stanford.nlp.trees.tregex.tsurgeon;

import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.tregex.TregexMatcher;

/**
 * @author Roger Levy (rog@stanford.edu)
 */
class FetchNode extends TsurgeonPattern {

  public FetchNode(String nodeName) {
    super(nodeName, new TsurgeonPattern[] {});
  }

  public Tree evaluate(Tree t, TregexMatcher m) {
    Tree result = root.newNodeNames.get(label);
    if (result == null) {
      result = m.getNode(label);
    }
    if (result == null) {
      System.err.println("Warning -- null node fetched by tsurgeon operation for node: " + this);
    }
    return result;
  }
}
