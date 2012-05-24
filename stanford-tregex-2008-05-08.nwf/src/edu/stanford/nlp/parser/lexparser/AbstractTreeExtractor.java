package edu.stanford.nlp.parser.lexparser;

import java.util.Collection;
import java.util.Iterator;

import edu.stanford.nlp.util.Function;
import edu.stanford.nlp.trees.Tree;

public abstract class AbstractTreeExtractor implements Extractor {

  protected void tallyLeaf(Tree lt) {
  }

  protected void tallyPreTerminal(Tree lt) {
  }

  protected void tallyInternalNode(Tree lt) {
  }

  protected void tallyRoot(Tree lt) {
  }

  public Object formResult() {
    return null;
  }

  protected void tallyLocalTree(Tree lt) {
    // printTrainTree(null, "Tallying local tree:", lt);

    if (lt.isLeaf()) {
      //      System.out.println("it's a leaf");
      tallyLeaf(lt);
    } else if (lt.isPreTerminal()) {
      //      System.out.println("it's a preterminal");
      tallyPreTerminal(lt);
    } else {
      //      System.out.println("it's a internal node");
      tallyInternalNode(lt);
    }
  }

  public void tallyTree(Tree t) {
    tallyRoot(t);
    for (Tree localTree : t.subTreeList()) {
      tallyLocalTree(localTree);
    }
  }

  protected void tallyTrees(Collection<Tree> trees) {
    for (Tree tree : trees) {
      tallyTree(tree);
    }
  }

  protected void tallyTreeIterator(Iterator<Tree> treeIterator, Function<Tree, Tree> f) {
    while (treeIterator.hasNext()) {
      Tree tree = treeIterator.next();
      try {
        tree = f.apply(tree);
      } catch (Exception e) {
        if (Test.verbose) {
          e.printStackTrace();
        }
        continue;  // UGH!
      }
      tallyTree(tree);
    }
  }

  public Object extract() {
    return formResult();
  }

  public Object extract(Collection<Tree> treeList) {
    tallyTrees(treeList);
    return formResult();
  }

  protected double weight = 1.0;

  public Object extract(Collection<Tree> trees1, Collection<Tree> trees2, double weight) {
    tallyTrees(trees1);
    this.weight = weight;
    tallyTrees(trees2);
    return formResult();
  }

  public Object extract(Iterator<Tree> treeIterator, Function<Tree, Tree> f) {
    tallyTreeIterator(treeIterator, f);
    return formResult();
  }
}
