package edu.stanford.nlp.trees;

import edu.stanford.nlp.trees.tregex.TregexPattern;
import edu.stanford.nlp.trees.tregex.tsurgeon.Tsurgeon;
import edu.stanford.nlp.trees.tregex.tsurgeon.TsurgeonPattern;
import edu.stanford.nlp.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Transforms an English structure parse tree in order to get the dependencies right:
 *  -- put a ROOT node
 *  -- remove NONE nodes
 *  -- retain only NP-TMP and NP-ADV tags
 *
 * @author mcdm
 */
public class DependencyTreeTransformer implements TreeTransformer {


  private Pattern NPTmpPattern = Pattern.compile("NP.*-TMP.*");
  private static final Pattern NPAdvPattern = Pattern.compile("NP.*-ADV.*");
  protected final TreebankLanguagePack tlp;

  public DependencyTreeTransformer() {
    tlp = new PennTreebankLanguagePack();
  }

  public Tree transformTree(Tree t) {
    //deal with empty root
    t.setValue(cleanUpRoot(t.value()));

    //strips tags
    stripTag(t, t);

    // strip empty nodes
    return stripEmptyNode(t);
  }

  protected String cleanUpRoot(String label) {
    if (label == null || label.equals("TOP")) {
      return "ROOT";
      // String constants are always interned
    } else {
      return label;
    }
  }

  // only leaves NP-TMP and NP-ADV
  protected String cleanUpLabel(String label) {
    boolean nptemp = NPTmpPattern.matcher(label).matches();
    boolean npadv = NPAdvPattern.matcher(label).matches();
    label = tlp.basicCategory(label);
    if (nptemp) {
      label = label + "-TMP";
    } else if (npadv) {
      label = label + "-ADV";
    }
    return label;
  }

  protected void stripTag(Tree t, Tree root) {
    if ( ! t.isLeaf()) {
      String label = cleanUpLabel(t.value());
      t.setValue(label);
      for (Tree child : t.getChildrenAsList()) {
        stripTag(child, root);
      }
    }
  }

  protected Tree stripEmptyNode(Tree t) {
    List<Pair<TregexPattern, TsurgeonPattern>> ops = new ArrayList<Pair<TregexPattern, TsurgeonPattern>>();

    String patternMatch = "-NONE-=none";
    String operation = "prune none";
    TregexPattern matchPattern = null;
    try {
      matchPattern = TregexPattern.compile(patternMatch);
    } catch (Exception e) {
      System.err.println("Error compiling match pattern");
    }
    TsurgeonPattern p = Tsurgeon.parseOperation(operation);
    ops.add(new Pair<TregexPattern, TsurgeonPattern>(matchPattern, p));

    return Tsurgeon.processPatternsOnTree(ops, t);
  }
  
}
