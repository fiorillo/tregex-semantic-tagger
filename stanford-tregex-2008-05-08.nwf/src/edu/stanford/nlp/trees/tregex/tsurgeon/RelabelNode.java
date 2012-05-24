package edu.stanford.nlp.trees.tregex.tsurgeon;

import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.tregex.TregexMatcher;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * @author Roger Levy (rog@stanford.edu)
 */
class RelabelNode extends TsurgeonPattern {

  private static final Pattern regexPattern = Pattern.compile("^/(.*)/$");
  private static final Pattern quotexPattern = Pattern.compile("^\\|(.*)\\|$");

  private final boolean fixedNewLabel; // = false;
  private String newLabel;

  private Pattern labelRegex;
  private int groupNumber;

  public RelabelNode(TsurgeonPattern child, String newLabel) {
    super("relabel", new TsurgeonPattern[] { child });
    Matcher m = quotexPattern.matcher(newLabel);
    if (m.matches()) {
      this.newLabel = m.group(1);
    } else {
      this.newLabel = newLabel;
    }
    fixedNewLabel = true;
  }

  public RelabelNode(TsurgeonPattern child, String labelRegex, int groupNumber) {
    super("relabel", new TsurgeonPattern[] { child });
    fixedNewLabel = false;
    Matcher m = regexPattern.matcher(labelRegex);
    if (m.matches()) {
      this.labelRegex = Pattern.compile(m.group(1));
      this.groupNumber = groupNumber;
    } else {
      throw new RuntimeException("Illegal label regex: " + labelRegex);
    }
  }

  public Tree evaluate(Tree t, TregexMatcher tm) {
    Tree nodeToRelabel = children[0].evaluate(t,tm);
    if (fixedNewLabel) {
      nodeToRelabel.label().setValue(newLabel);
    } else {
      Matcher m = labelRegex.matcher(nodeToRelabel.label().value());
      if(m.find())
        nodeToRelabel.label().setValue(m.group(groupNumber));
    }
    return t;
  }

  public String toString() {
    String result;
    if (fixedNewLabel) {
      result =  label + "(" + children[0].toString() + "," + newLabel + ")";
    } else {
      result = label + "(" + children[0].toString() + "," + labelRegex.toString() + "," + groupNumber + ")";
    }
    return result;
  }

}
