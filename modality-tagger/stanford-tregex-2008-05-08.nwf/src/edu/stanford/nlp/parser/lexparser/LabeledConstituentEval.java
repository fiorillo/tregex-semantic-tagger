package edu.stanford.nlp.parser.lexparser;

import edu.stanford.nlp.trees.*;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.io.PrintWriter;

/**
 * Provides for Precision/Recall/F1 evaluation of labeled constituents.
 *
 * @author Dan Klein
 */
public class LabeledConstituentEval extends AbstractEval {

  private static final boolean DEBUG = false;

  private ConstituentFactory cf = new LabeledScoredConstituentFactory();
  private TreeFactory tf = new LabeledScoredTreeFactory();
  private TreebankLanguagePack tlp;

  protected Tree stripLeaves(Tree tree) {
    if (tree.isLeaf()) {
      return null;
    }
    if (tree.isPreTerminal()) {
      return tf.newLeaf(tree.label());
    }
    int numKids = tree.numChildren();
    List<Tree> children = new ArrayList<Tree>(numKids);
    for (int cNum = 0; cNum < numKids; cNum++) {
      children.add(stripLeaves(tree.getChild(cNum)));
    }
    return tf.newTreeNode(tree.label(), children);
  }

  Set makeObjects(Tree tree) {
    // Strip off a root node. Allow it to have multiple daughters.
    // Assumes boundary symbol is already removed.
    Tree noLeafTree = stripLeaves(tree);
    Set set = new HashSet();
    if (noLeafTree == null) {
      return set;
    }
    // TODO: note that this code is written to ignore last kid which is the
    // boundary symbol, but these days the tree is collinized before
    // sending in and so the boundary is gone....
    if (tlp.isStartSymbol(noLeafTree.label().value())) {
      for (int i = 0; i < noLeafTree.children().length - 1; i++) {
        set.addAll(noLeafTree.children()[i].constituents(cf));
      }
    } else {
      set.addAll(noLeafTree.constituents(cf));
    }
    return set;
  }

  public void evaluate(Tree guess, Tree gold, PrintWriter pw) {
    if (guess.yield().size() != gold.yield().size()) {
      pw.println("Warning: yield differs:");
      pw.println("Guess: " + guess.yield());
      pw.println("Gold: " + gold.yield());
    }
    if (DEBUG) {
      System.err.println("Guess is: " + guess.toString());
      System.err.println("Gold is: " + gold.toString());
      Set sguess = makeObjects(guess);
      Set sgold = makeObjects(gold);
      System.err.println("LCEval Guess " + sguess.size() + " constits: " + makeObjects(guess));
      System.err.println("LCEval Gold  " + sgold.size() + " constits: " + makeObjects(gold));
    }
    super.evaluate(guess, gold, pw);
  }

  public LabeledConstituentEval(String str, boolean runningAverages, TreebankLanguagePack tlp) {
    super(str, runningAverages);
    this.tlp = tlp;
  }

  public static void main(String[] args) {
    TreebankLangParserParams tlpp = new EnglishTreebankParserParams();

    if (args.length < 2 || args.length > 3) {
      System.err.println("usage: LabeledConstituentEval gold gues [TLPP]");
      return;
    }
    if (args.length > 2) {
      try {
        final Object o = Class.forName(args[2]).newInstance();
        tlpp = (TreebankLangParserParams) o;
      } catch (Exception e) {
        System.err.println("Couldn't instantiate: " + args[2]);
      }
    }
    Treebank tb1 = tlpp.diskTreebank();
    Treebank tb2 = tlpp.diskTreebank();
    PrintWriter pw = tlpp.pw();

    LabeledConstituentEval lce = new LabeledConstituentEval("LP/LR", true,
                                              tlpp.treebankLanguagePack());
    TreeTransformer tc = tlpp.collinizer();

    tb1.loadPath(args[0]);
    tb2.loadPath(args[1]);
    System.err.println(tb1.textualSummary());
    System.err.println(tb2.textualSummary());

    Iterator<Tree> tb2it = tb2.iterator();
    for (Tree t : tb1) {
      if (tb2it.hasNext()) {
        Tree tGuess = tb2it.next();
        lce.evaluate(tc.transformTree(t), tc.transformTree(tGuess), pw);
      }
    }
    pw.println();
    lce.display(true, pw);
  }


  public static class CBEval extends LabeledConstituentEval {

    private double cb = 0.0;
    private double num = 0.0;
    private double zeroCB = 0.0;

    protected void checkCrossing(Set<Constituent> s1, Set<Constituent> s2) {
      double c = 0.0;
      for (Constituent constit : s1) {
        if (constit.crosses(s2)) {
          c += 1.0;
        }
      }
      if (c == 0.0) {
        zeroCB += 1.0;
      }
      cb += c;
      num += 1.0;
    }

    public void evaluate(Tree t1, Tree t2, PrintWriter pw) {
      Set b1 = makeObjects(t1);
      Set b2 = makeObjects(t2);
      checkCrossing(b1, b2);
      if (pw != null && runningAverages) {
        pw.println("AvgCB: " + ((int) (10000.0 * cb / num)) / 100.0 +
                   " ZeroCB: " + ((int) (10000.0 * zeroCB / num)) / 100.0 + " N: " + getNum());
      }
    }

    public void display(boolean verbose, PrintWriter pw) {
      pw.println(str + " AvgCB: " + ((int) (10000.0 * cb / num)) / 100.0 +
                 " ZeroCB: " + ((int) (10000.0 * zeroCB / num)) / 100.0);
    }

    public CBEval(String str, boolean runningAverages, TreebankLanguagePack tlp) {
      super(str, runningAverages, tlp);
    }

  } // end class CBEval

} // end class LabeledConstituentEval
