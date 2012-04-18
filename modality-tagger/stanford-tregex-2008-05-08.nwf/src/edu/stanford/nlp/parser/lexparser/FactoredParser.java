// StanfordLexicalizedParser -- a probabilistic lexicalized NL CFG parser
// Copyright (c) 2002, 2003, 2004, 2005 The Board of Trustees of
// The Leland Stanford Junior University. All Rights Reserved.
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
// For more information, bug reports, fixes, contact:
//    Christopher Manning
//    Dept of Computer Science, Gates 1A
//    Stanford CA 94305-9010
//    USA
//    parser-support@lists.stanford.edu
//    http://nlp.stanford.edu/downloads/lex-parser.shtml

package edu.stanford.nlp.parser.lexparser;

import edu.stanford.nlp.io.NumberRangeFileFilter;
import edu.stanford.nlp.ling.*;
import edu.stanford.nlp.math.SloppyMath;
import edu.stanford.nlp.trees.*;
import edu.stanford.nlp.util.*;
import edu.stanford.nlp.stats.ClassicCounter;

import java.io.PrintWriter;
import java.io.Serializable;
import java.util.*;

/**
 * @author Dan Klein (original version)
 * @author Christopher Manning (better features, ParserParams, serialization)
 * @author Roger Levy (internationalization)
 * @author Teg Grenager (grammar compaction, etc., tokenization, etc.)
 * @author Galen Andrew (lattice parsing)
 * @author Philip Resnik and Dan Zeman (n good parses)
 */



class FA {
  protected boolean[] inStatePrev;
  protected boolean[] inStateNext;
  protected boolean[] loopState;
  protected int acceptingState;
  protected int initialState;
  protected int numStates;
  protected int numSymbols;
  protected int[][] transition; // state x tag

  public void init() {
    Arrays.fill(inStatePrev, false);
    Arrays.fill(inStateNext, false);
    inStatePrev[initialState] = true;
  }

  public void input(int symbol) {
    for (int prevState = 0; prevState < numStates; prevState++) {
      if (inStatePrev[prevState]) {
        inStateNext[transition[prevState][symbol]] = true;
      }
    }
  }

  public void advance() {
    boolean[] temp = inStatePrev;
    inStatePrev = inStateNext;
    inStateNext = temp;
    Arrays.fill(inStateNext, false);
    for (int state = 0; state < numStates; state++) {
      if (inStatePrev[state] && loopState[state]) {
        inStateNext[state] = true;
      }
    }
  }

  public boolean isAccepting() {
    return inStatePrev[acceptingState];
  }

  public void setTransition(int state, int symbol, int result) {
    transition[state][symbol] = result;
  }

  public void setLoopState(int state, boolean loops) {
    loopState[state] = loops;
  }

  public FA(int numStates, int numSymbols) {
    this.numStates = numStates;
    this.numSymbols = numSymbols;
    acceptingState = numStates - 1;
    inStatePrev = new boolean[numStates];
    inStateNext = new boolean[numStates];
    loopState = new boolean[numStates];
    transition = new int[numStates][numSymbols];
  }

} // end class FA


class SmoothedBinaryGrammarExtractor extends AbstractTreeExtractor {

  protected int HISTORY_DEPTH() {
    return (Train.gPA ? 3 : (Train.PA ? 2 : 1));
  }

  protected Numberer stateNumberer = Numberer.getGlobalNumberer("states");
  protected Numberer tagNumberer = Numberer.getGlobalNumberer("tags");

  protected Map ruleToLabel = new HashMap();
  protected ClassicCounter rulePairs = new ClassicCounter();
  protected ClassicCounter labelPairs = new ClassicCounter();

  public void tallyTree(Tree t) {
    LinkedList<String> ll = new LinkedList<String>();
    //ll.add("ROOT");
    //ll.add("ROOT");
    tallyTree(t, ll);
  }

  protected void tallyTree(Tree t, LinkedList<String> parents) {
    // traverse tree, building parent list
    String str = t.label().value();
    boolean strIsPassive = (str.indexOf('@') == -1);
    if (strIsPassive) {
      parents.addFirst(str);
    }
    if (!t.isLeaf()) {
      if (!t.children()[0].isLeaf()) {
        tallyInternalNode(t, parents);
        for (int c = 0; c < t.children().length; c++) {
          Tree child = t.children()[c];
          tallyTree(child, parents);
        }
      } else {
        tagNumberer.number(t.label().value());
      }
    }
    if (strIsPassive) {
      parents.removeFirst();
    }
  }

  protected Rule ltToRule(Tree lt) {
    if (lt.children().length == 1) {
      UnaryRule ur = new UnaryRule();
      ur.parent = stateNumberer.number(lt.label().value());
      ur.child = stateNumberer.number(lt.children()[0].label().value());
      return ur;
    } else {
      BinaryRule br = new BinaryRule();
      br.parent = stateNumberer.number(lt.label().value());
      br.leftChild = stateNumberer.number(lt.children()[0].label().value());
      br.rightChild = stateNumberer.number(lt.children()[1].label().value());
      return br;
    }
  }

  protected boolean isSynthetic(int state) {
    return ((String) stateNumberer.object(state)).indexOf('@') > -1;
  }

  protected boolean isTag(int state) {
    return tagNumberer.hasSeen(stateNumberer.object(state));
  }

  protected Rule specifyRule(Rule rule, List history, int childDepth) {
    Rule r;
    String topHistoryStr = historyToString(history.subList(1, history.size()));
    String bottomHistoryStr = historyToString(history.subList(0, childDepth));
    if (rule instanceof UnaryRule) {
      UnaryRule ur = new UnaryRule();
      UnaryRule urule = (UnaryRule) rule;
      ur.parent = stateNumberer.number(stateNumberer.object(urule.parent) + topHistoryStr);
      if (isSynthetic(urule.child)) {
        ur.child = stateNumberer.number(stateNumberer.object(urule.child) + topHistoryStr);
      } else if (isTag(urule.child)) {
        ur.child = urule.child;
      } else {
        ur.child = stateNumberer.number(stateNumberer.object(urule.child) + bottomHistoryStr);
      }
      r = ur;
    } else {
      BinaryRule br = new BinaryRule();
      BinaryRule brule = (BinaryRule) rule;
      br.parent = stateNumberer.number(stateNumberer.object(brule.parent) + topHistoryStr);
      if (isSynthetic(brule.leftChild)) {
        br.leftChild = stateNumberer.number(stateNumberer.object(brule.leftChild) + topHistoryStr);
      } else if (isTag(brule.leftChild)) {
        br.leftChild = brule.leftChild;
      } else {
        br.leftChild = stateNumberer.number(stateNumberer.object(brule.leftChild) + bottomHistoryStr);
      }
      if (isSynthetic(brule.rightChild)) {
        br.rightChild = stateNumberer.number(stateNumberer.object(brule.rightChild) + topHistoryStr);
      } else if (isTag(brule.rightChild)) {
        br.rightChild = brule.rightChild;
      } else {
        br.rightChild = stateNumberer.number(stateNumberer.object(brule.rightChild) + bottomHistoryStr);
      }
      r = br;
    }
    return r;
  }

  protected void tallyInternalNode(Tree lt, List parents) {
    // form base rule
    String label = lt.label().value();
    Rule baseR = ltToRule(lt);
    ruleToLabel.put(baseR, label);
    // act on each history depth
    for (int depth = 0, maxDepth = Math.min(HISTORY_DEPTH(), parents.size()); depth <= maxDepth; depth++) {
      List history = new ArrayList(parents.subList(0, depth));
      // tally each history level / rewrite pair
      rulePairs.incrementCount(new Pair(baseR, history), 1);
      labelPairs.incrementCount(new Pair(label, history), 1);
    }
  }

  protected Map historyToString = new HashMap();

  protected String historyToString(List history) {
    String str = (String) historyToString.get(history);
    if (str == null) {
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < history.size(); i++) {
        sb.append('^');
        sb.append(history.get(i));
      }
      str = sb.toString();
      historyToString.put(history, str);
    }
    return str;
  }

  public Object formResult() {
    Set brs = new HashSet();
    Set urs = new HashSet();
    // scan each rule / history pair
    int ruleCount = 0;
    for (Iterator pairI = rulePairs.keySet().iterator(); pairI.hasNext();) {
      if (ruleCount % 100 == 0) {
        System.err.println("Rules multiplied: " + ruleCount);
      }
      ruleCount++;
      Pair rulePair = (Pair) pairI.next();
      Rule baseRule = (Rule) rulePair.first;
      String baseLabel = (String) ruleToLabel.get(baseRule);
      List history = (List) rulePair.second;
      double totalProb = 0;
      for (int depth = 1; depth <= HISTORY_DEPTH() && depth <= history.size(); depth++) {
        List subHistory = history.subList(0, depth);
        double c_label = labelPairs.getCount(new Pair(baseLabel, subHistory));
        double c_rule = rulePairs.getCount(new Pair(baseRule, subHistory));
        //System.out.println("Multiplying out "+baseRule+" with history "+subHistory);
        //System.out.println("Count of "+baseLabel+" with "+subHistory+" is "+c_label);
        //System.out.println("Count of "+baseRule+" with "+subHistory+" is "+c_rule );

        double prob = (1.0 / HISTORY_DEPTH()) * (c_rule) / (c_label);
        totalProb += prob;
        for (int childDepth = 0; childDepth <= Math.min(HISTORY_DEPTH() - 1, depth); childDepth++) {
          Rule rule = specifyRule(baseRule, subHistory, childDepth);
          rule.score = (float) Math.log(totalProb);
          //System.out.println("Created  "+rule+" with score "+rule.score);
          if (rule instanceof UnaryRule) {
            urs.add(rule);
          } else {
            brs.add(rule);
          }
        }
      }
    }
    System.out.println("Total states: " + stateNumberer.total());
    BinaryGrammar bg = new BinaryGrammar(stateNumberer.total());
    UnaryGrammar ug = new UnaryGrammar(stateNumberer.total());
    for (Iterator brI = brs.iterator(); brI.hasNext();) {
      BinaryRule br = (BinaryRule) brI.next();
      bg.addRule(br);
    }
    for (Iterator urI = urs.iterator(); urI.hasNext();) {
      UnaryRule ur = (UnaryRule) urI.next();
      ug.addRule(ur);
    }
    return new Pair(ug, bg);
  }

} // end class SmoothedBinaryGrammarExtractor


class XBarGrammarProjection implements GrammarProjection {
  UnaryGrammar sourceUG;
  BinaryGrammar sourceBG;
  Numberer sourceNumberer;

  UnaryGrammar targetUG;
  BinaryGrammar targetBG;
  Numberer targetNumberer;

  int[] projection;

  public int project(int state) {
    return projection[state];
  }

  public UnaryGrammar sourceUG() {
    return sourceUG;
  }

  public BinaryGrammar sourceBG() {
    return sourceBG;
  }

  public UnaryGrammar targetUG() {
    return targetUG;
  }

  public BinaryGrammar targetBG() {
    return targetBG;
  }


  protected String projectString(String str) {
    if (str.indexOf('@') == -1) {
      if (str.indexOf('^') == -1) {
        return str;
      }
      return str.substring(0, str.indexOf('^'));
    }
    StringBuilder sb = new StringBuilder();
    sb.append(str.substring(0, str.indexOf(' ')));
    if (str.indexOf('^') > -1) {
      //sb.append(str.substring(str.indexOf('^'),str.length()));
    }
    int num = -2;
    for (int i = 0; i < str.length(); i++) {
      if (str.charAt(i) == ' ') {
        num++;
      }
    }
    sb.append(" w " + num);
    return sb.toString();
  }

  protected void scanStates(Numberer source, Numberer target) {
    for (int i = 0; i < source.total(); i++) {
      String stateStr = (String) source.object(i);
      String projStr = projectString(stateStr);
      projection[i] = target.number(projStr);
    }
  }

  protected BinaryRule projectBinaryRule(BinaryRule br) {
    BinaryRule br2 = new BinaryRule();
    br2.parent = projection[br.parent];
    br2.leftChild = projection[br.leftChild];
    br2.rightChild = projection[br.rightChild];
    br2.score = br.score;
    return br2;
  }

  protected UnaryRule projectUnaryRule(UnaryRule ur) {
    UnaryRule ur2 = new UnaryRule();
    ur2.parent = projection[ur.parent];
    ur2.child = projection[ur.child];
    ur2.score = ur.score;
    return ur2;
  }

  public XBarGrammarProjection(BinaryGrammar bg, UnaryGrammar ug) {
    Map<BinaryRule,BinaryRule> binaryRules = new HashMap<BinaryRule,BinaryRule>();
    Map<UnaryRule,UnaryRule> unaryRules = new HashMap<UnaryRule,UnaryRule>();
    sourceUG = ug;
    sourceBG = bg;
    sourceNumberer = Numberer.getGlobalNumberer(bg.stateSpace());
    targetNumberer = Numberer.getGlobalNumberer(bg.stateSpace() + "-xbar");
    projection = new int[sourceNumberer.total()];
    scanStates(sourceNumberer, targetNumberer);
    targetBG = new BinaryGrammar(targetNumberer.total(), bg.stateSpace() + "-xbar");
    targetUG = new UnaryGrammar(targetNumberer.total());
    for (Iterator<BinaryRule> brI = bg.iterator(); brI.hasNext();) {
      BinaryRule rule = projectBinaryRule(brI.next());
      Rule old = binaryRules.get(rule);
      if (old == null || rule.score > old.score) {
        binaryRules.put(rule, rule);
      }
    }
    for (BinaryRule br : binaryRules.keySet()) {
      targetBG.addRule(br);
      //System.out.println("BR: "+targetNumberer.object(br.parent)+" -> "+targetNumberer.object(br.leftChild)+" "+targetNumberer.object(br.rightChild)+" %% "+br.score);
    }
    targetBG.splitRules();
    for (int parent = 0; parent < sourceNumberer.total(); parent++) {
      for (Iterator<UnaryRule> urI = ug.ruleIteratorByParent(parent); urI.hasNext();) {
        UnaryRule sourceRule = urI.next();
        UnaryRule rule = projectUnaryRule(sourceRule);
        Rule old = unaryRules.get(rule);
        if (old == null || rule.score > old.score) {
          unaryRules.put(rule, rule);
        }
        /*
          if (((UnaryRule)rule).child == targetNumberer.number("PRP") &&
            ((String)sourceNumberer.object(rule.parent)).charAt(0) == 'N') {
          System.out.println("Source UR: "+sourceRule+" %% "+sourceRule.score);
          System.out.println("Score of "+rule+"is now: "+((UnaryRule)unaryRules.get(rule)).score);
        }
        */
      }
    }
    for (UnaryRule ur : unaryRules.keySet()) {
      targetUG.addRule(ur);
      //System.out.println("UR: "+targetNumberer.object(ur.parent)+" -> "+targetNumberer.object(ur.child)+" %% "+ur.score);
    }
    targetUG.purgeRules();
    System.out.println("Projected " + sourceNumberer.total() + " states to " + targetNumberer.total() + " states.");
  }
}


interface GrammarProjection {
  int project(int state);

  UnaryGrammar sourceUG();

  BinaryGrammar sourceBG();

  UnaryGrammar targetUG();

  BinaryGrammar targetBG();
}

class NullGrammarProjection implements GrammarProjection {
  UnaryGrammar ug;
  BinaryGrammar bg;

  public int project(int state) {
    return state;
  }

  public UnaryGrammar sourceUG() {
    return ug;
  }

  public BinaryGrammar sourceBG() {
    return bg;
  }

  public UnaryGrammar targetUG() {
    return ug;
  }

  public BinaryGrammar targetBG() {
    return bg;
  }

  NullGrammarProjection(BinaryGrammar bg, UnaryGrammar ug) {
    this.ug = ug;
    this.bg = bg;
  }
}


class ProjectionScorer implements Scorer {
  protected GrammarProjection gp;
  protected Scorer scorer;

  protected Edge tempEdge = new Edge();

  protected Edge project(Edge edge) {
    tempEdge.start = edge.start;
    tempEdge.end = edge.end;
    tempEdge.state = gp.project(edge.state);
    tempEdge.head = edge.head;
    tempEdge.tag = edge.tag;
    return tempEdge;
  }

  protected Hook tempHook = new Hook();

  protected Hook project(Hook hook) {
    tempHook.start = hook.start;
    tempHook.end = hook.end;
    tempHook.state = gp.project(hook.state);
    tempHook.head = hook.head;
    tempHook.tag = hook.tag;
    tempHook.subState = gp.project(hook.subState);
    return tempHook;
  }

  public double oScore(Edge edge) {
    return scorer.oScore(project(edge));
  }

  public double iScore(Edge edge) {
    return scorer.iScore(project(edge));
  }

  public boolean oPossible(Hook hook) {
    return scorer.oPossible(project(hook));
  }

  public boolean iPossible(Hook hook) {
    return scorer.iPossible(project(hook));
  }

  public boolean parse(List<? extends HasWord> words) {
    return scorer.parse(words);
  }

  public ProjectionScorer(Scorer scorer, GrammarProjection gp) {
    this.scorer = scorer;
    this.gp = gp;
  }
} // end ProjectionScorer


class BasicCategoryTagProjection implements TagProjection, Serializable {
  TreebankLanguagePack tlp;

  public BasicCategoryTagProjection(TreebankLanguagePack tlp) {
    this.tlp = tlp;
  }

  public String project(String tagStr) {
    // return tagStr;
    String ret = tlp.basicCategory(tagStr);
    // System.err.println("BCTP mapped " + tagStr + " to " + ret);
    return ret;
  }
}


// Looks like the intended behavior of TestTagProjection is:
// 1) Include the basic category (everything before a '-' or '^' annotation)
// 2) Include any annotation introduced with '-'
// 3) Exclude any annotation introduced with '^'
// 4) Annotations introduced with other characters will be included or excluded
//    as determined by the previous annotation or basic category.
//
// This seems awfully haphazard :(
//
//  Roger

class TestTagProjection implements TagProjection, Serializable {
  public String project(String tagStr) {
    StringBuilder sb = new StringBuilder();
    boolean good = true;
    for (int pos = 0, len = tagStr.length(); pos < len; pos++) {
      char c = tagStr.charAt(pos);
      if (c == '-') {
        good = true;
      } else if (c == '^') {
        good = false;
      }
      if (good) {
        sb.append(c);
      }
    }
    String ret = sb.toString();
    // System.err.println("TTP mapped " + tagStr + " to " + ret);
    return ret;
  }
}

/** This tag projection just returns the same tag space. */
class IdentityTagProjection implements TagProjection, Serializable {

  public String project(String tagStr) {
    return tagStr;
  }

}


class TwinScorer implements Scorer {
  private Scorer scorer1;
  private Scorer scorer2;

  public double oScore(Edge edge) {
    return scorer1.oScore(edge) + scorer2.oScore(edge);
  }

  public double iScore(Edge edge) {
    return scorer1.iScore(edge) + scorer2.iScore(edge);
  }

  public boolean oPossible(Hook hook) {
    return scorer1.oPossible(hook) && scorer2.oPossible(hook);
  }

  public boolean iPossible(Hook hook) {
    return scorer1.iPossible(hook) && scorer2.iPossible(hook);
  }

  public boolean parse(List<? extends HasWord> words) {
    boolean b1 = scorer1.parse(words);
    boolean b2 = scorer2.parse(words);
    return (b1 && b2);
  }

  public TwinScorer(Scorer scorer1, Scorer scorer2) {
    this.scorer1 = scorer1;
    this.scorer2 = scorer2;
  }
}

class MaxScorer implements Scorer {
  private Scorer scorer1;
  private Scorer scorer2;

  public double oScore(Edge edge) {
    return SloppyMath.min(scorer1.oScore(edge), scorer2.oScore(edge));
  }

  public double iScore(Edge edge) {
    return SloppyMath.min(scorer1.iScore(edge), scorer2.iScore(edge));
  }

  public boolean oPossible(Hook hook) {
    return scorer1.oPossible(hook) && scorer2.oPossible(hook);
  }

  public boolean iPossible(Hook hook) {
    return scorer1.iPossible(hook) && scorer2.iPossible(hook);
  }

  public boolean parse(List<? extends HasWord> words) {
    boolean b1 = scorer1.parse(words);
    boolean b2 = scorer2.parse(words);
    return (b1 && b2);
  }

  public MaxScorer(Scorer scorer1, Scorer scorer2) {
    this.scorer1 = scorer1;
    this.scorer2 = scorer2;
  }
}


// A* Guts


class Interner {
  private Map oToO = new HashMap();

  public Object intern(Object o) {
    Object i = oToO.get(o);
    if (i == null) {
      i = o;
      oToO.put(o, o);
    }
    return i;
  }
}



class NodePruner {

  private ExhaustivePCFGParser parser;
  private TreeTransformer debinarizer;

  List<Tree> prune(List<Tree> treeList, Label label, int start, int end) {
    // get reference tree
    if (treeList.size() == 1) {
      return treeList;
    }
    Tree testTree = treeList.get(0).treeFactory().newTreeNode(label, treeList);
    int goal = Numberer.getGlobalNumberer("states").number(label.value());
    Tree tempTree = parser.extractBestParse(goal, start, end);
    // parser.restoreUnaries(tempTree);
    Tree pcfgTree = debinarizer.transformTree(tempTree);
    Set<Constituent> pcfgConstituents = pcfgTree.constituents(new LabeledScoredConstituentFactory());
    // delete child labels that are not in reference but do not cross reference
    List<Tree> prunedChildren = new ArrayList<Tree>();
    int childStart = 0;
    for (int c = 0, numCh = testTree.numChildren(); c < numCh; c++) {
      Tree child = testTree.getChild(c);
      boolean isExtra = true;
      int childEnd = childStart + child.yield().size();
      Constituent childConstituent = new LabeledScoredConstituent(childStart, childEnd, child.label(), 0);
      if (pcfgConstituents.contains(childConstituent)) {
        isExtra = false;
      }
      if (childConstituent.crosses(pcfgConstituents)) {
        isExtra = false;
      }
      if (child.isLeaf() || child.isPreTerminal()) {
        isExtra = false;
      }
      if (pcfgTree.yield().size() != testTree.yield().size()) {
        isExtra = false;
      }
      if (!label.value().startsWith("NP^NP")) {
        isExtra = false;
      }
      if (isExtra) {
        System.err.println("Pruning: " + child.label() + " from " + (childStart + start) + " to " + (childEnd + start));
        System.err.println("Was: " + testTree + " vs " + pcfgTree);
        prunedChildren.addAll(child.getChildrenAsList());
      } else {
        prunedChildren.add(child);
      }
      childStart = childEnd;
    }
    return prunedChildren;
  }

  private List<Tree> helper(List<Tree> treeList, int start) {
    List<Tree> newTreeList = new ArrayList<Tree>(treeList.size());
    for (Tree tree : treeList) {
      int end = start + tree.yield().size();
      newTreeList.add(prune(tree, start));
      start = end;
    }
    return newTreeList;
  }

  public Tree prune(Tree tree) {
    return prune(tree, 0);
  }

  Tree prune(Tree tree, int start) {
    if (tree.isLeaf() || tree.isPreTerminal()) {
      return tree;
    }
    // check each node's children for deletion
    List<Tree> children = helper(tree.getChildrenAsList(), start);
    children = prune(children, tree.label(), start, start + tree.yield().size());
    return tree.treeFactory().newTreeNode(tree.label(), children);
  }

  public NodePruner(ExhaustivePCFGParser parser, TreeTransformer debinarizer) {
    this.parser = parser;
    this.debinarizer = debinarizer;
  }

} // end class NodePruner

/* some documentation for Roger's convenience
 * {pcfg,dep,combo}{PE,DE,TE} are precision/dep/tagging evals for the models

 * parser is the PCFG parser
 * dparser is the dependency parser
 * bparser is the combining parser

 * during testing:
 * tree is the test tree (gold tree)
 * binaryTree is the gold tree binarized
 * tree2b is the best PCFG paser, binarized
 * tree2 is the best PCFG parse (debinarized)
 * tree3 is the dependency parse, binarized
 * tree3db is the dependency parser, debinarized
 * tree4 is the best combo parse, binarized and then debinarized
 * tree4b is the best combo parse, binarized
 */

public class FactoredParser {

  public static void main(String[] args) {
    Options op = new Options(new EnglishTreebankParserParams());
    // op.tlpParams may be changed to something else later, so don't use it till
    // after options are parsed.

    System.out.println("Currently " + new Date());
    System.out.print("Invoked with arguments:");
    for (String arg : args) {
      System.out.print(" " + arg);
    }
    System.out.println();

    String path = "/u/nlp/stuff/corpora/Treebank3/parsed/mrg/wsj";
    int trainLow = 200, trainHigh = 2199, testLow = 2200, testHigh = 2219;
    String serializeFile = null;

    int i = 0;
    while (i < args.length && args[i].startsWith("-")) {
      if (args[i].equalsIgnoreCase("-path") && (i + 1 < args.length)) {
        path = args[i + 1];
        i += 2;
      } else if (args[i].equalsIgnoreCase("-train") && (i + 2 < args.length)) {
        trainLow = Integer.parseInt(args[i + 1]);
        trainHigh = Integer.parseInt(args[i + 2]);
        i += 3;
      } else if (args[i].equalsIgnoreCase("-test") && (i + 2 < args.length)) {
        testLow = Integer.parseInt(args[i + 1]);
        testHigh = Integer.parseInt(args[i + 2]);
        i += 3;
      } else if (args[i].equalsIgnoreCase("-serialize") && (i + 1 < args.length)) {
        serializeFile = args[i + 1];
        i += 2;
      } else if (args[i].equalsIgnoreCase("-tLPP") && (i + 1 < args.length)) {
        try {
          op.tlpParams = (TreebankLangParserParams) Class.forName(args[i + 1]).newInstance();
        } catch (ClassNotFoundException e) {
          System.err.println("Class not found: " + args[i + 1]);
        } catch (InstantiationException e) {
          System.err.println("Couldn't instantiate: " + args[i + 1] + ": " + e.toString());
        } catch (IllegalAccessException e) {
          System.err.println("illegal access" + e);
        }
        i += 2;
      } else if (args[i].equals("-encoding")) {
        // sets encoding for TreebankLangParserParams
        op.tlpParams.setInputEncoding(args[i + 1]);
        op.tlpParams.setOutputEncoding(args[i + 1]);
        i += 2;
      } else {
        i = op.setOptionOrWarn(args, i);
      }
    }
    // System.out.println(tlpParams.getClass());
    TreebankLanguagePack tlp = op.tlpParams.treebankLanguagePack();

    Train.sisterSplitters = new HashSet(Arrays.asList(op.tlpParams.sisterSplitters()));
    //    BinarizerFactory.TreeAnnotator.setTreebankLang(tlpParams);
    PrintWriter pw = op.tlpParams.pw();

    Test.display();
    Train.display();
    op.display();
    op.tlpParams.display();

    // setup tree transforms
    Treebank trainTreebank = op.tlpParams.memoryTreebank();
    MemoryTreebank testTreebank = op.tlpParams.testMemoryTreebank();
    // Treebank blippTreebank = ((EnglishTreebankParserParams) tlpParams).diskTreebank();
    // String blippPath = "/afs/ir.stanford.edu/data/linguistic-data/BLLIP-WSJ/";
    // blippTreebank.loadPath(blippPath, "", true);

    Timing.startTime();
    System.err.print("Reading trees...");
    testTreebank.loadPath(path, new NumberRangeFileFilter(testLow, testHigh, true));
    if (Test.increasingLength) {
      Collections.sort(testTreebank, new TreeLengthComparator());
    }

    trainTreebank.loadPath(path, new NumberRangeFileFilter(trainLow, trainHigh, true));
    Timing.tick("done.");
    System.err.print("Binarizing trees...");
    TreeAnnotatorAndBinarizer binarizer = null;
    if (!Train.leftToRight) {
      binarizer = new TreeAnnotatorAndBinarizer(op.tlpParams, op.forceCNF, !Train.outsideFactor(), true);
    } else {
      binarizer = new TreeAnnotatorAndBinarizer(op.tlpParams.headFinder(), new LeftHeadFinder(), op.tlpParams, op.forceCNF, !Train.outsideFactor(), true);
    }
    CollinsPuncTransformer collinsPuncTransformer = null;
    if (Train.collinsPunc) {
      collinsPuncTransformer = new CollinsPuncTransformer(tlp);
    }
    TreeTransformer debinarizer = new Debinarizer(op.forceCNF);
    List<Tree> binaryTrainTrees = new ArrayList<Tree>();

    if (Train.selectiveSplit) {
      Train.splitters = ParentAnnotationStats.getSplitCategories(trainTreebank, Train.tagSelectiveSplit, 0, Train.selectiveSplitCutOff, Train.tagSelectiveSplitCutOff, op.tlpParams.treebankLanguagePack());
      if (Train.deleteSplitters != null) {
        List<String> deleted = new ArrayList<String>();
        for (String del : Train.deleteSplitters) {
          String baseDel = tlp.basicCategory(del);
          boolean checkBasic = del.equals(baseDel);
          for (Iterator<String> it = Train.splitters.iterator(); it.hasNext(); ) {
            String elem = it.next();
            String baseElem = tlp.basicCategory(elem);
            boolean delStr = checkBasic && baseElem.equals(baseDel) ||
              elem.equals(del);
            if (delStr) {
              it.remove();
              deleted.add(elem);
            }
          }
        }
        System.err.println("Removed from vertical splitters: " + deleted);
      }
    }
    if (Train.selectivePostSplit) {
      TreeTransformer myTransformer = new TreeAnnotator(op.tlpParams.headFinder(), op.tlpParams);
      Treebank annotatedTB = trainTreebank.transform(myTransformer);
      Train.postSplitters = ParentAnnotationStats.getSplitCategories(annotatedTB, true, 0, Train.selectivePostSplitCutOff, Train.tagSelectivePostSplitCutOff, op.tlpParams.treebankLanguagePack());
    }

    if (Train.hSelSplit) {
      binarizer.setDoSelectiveSplit(false);
      for (Tree tree : trainTreebank) {
        if (Train.collinsPunc) {
          tree = collinsPuncTransformer.transformTree(tree);
        }
        //tree.pennPrint(tlpParams.pw());
        tree = binarizer.transformTree(tree);
        //binaryTrainTrees.add(tree);
      }
      binarizer.setDoSelectiveSplit(true);
    }
    for (Tree tree : trainTreebank) {
      if (Train.collinsPunc) {
        tree = collinsPuncTransformer.transformTree(tree);
      }
      tree = binarizer.transformTree(tree);
      binaryTrainTrees.add(tree);
    }
    if (Test.verbose) {
      binarizer.dumpStats();
    }

    List<Tree> binaryTestTrees = new ArrayList<Tree>();
    for (Tree tree : testTreebank) {
      if (Train.collinsPunc) {
        tree = collinsPuncTransformer.transformTree(tree);
      }
      tree = binarizer.transformTree(tree);
      binaryTestTrees.add(tree);
    }
    Timing.tick("done.");  // binarization
    BinaryGrammar bg = null;
    UnaryGrammar ug = null;
    DependencyGrammar dg = null;
    // DependencyGrammar dgBLIPP = null;
    Lexicon lex = null;
    // extract grammars
    Extractor bgExtractor = new BinaryGrammarExtractor();
    //Extractor bgExtractor = new SmoothedBinaryGrammarExtractor();//new BinaryGrammarExtractor();
    // Extractor lexExtractor = new LexiconExtractor();

    //Extractor dgExtractor = new DependencyMemGrammarExtractor();

    Extractor dgExtractor = new MLEDependencyGrammarExtractor(op);
    if (op.doPCFG) {
      System.err.print("Extracting PCFG...");
      Pair bgug = null;
      if (Train.cheatPCFG) {
        List allTrees = new ArrayList(binaryTrainTrees);
        allTrees.addAll(binaryTestTrees);
        bgug = (Pair) bgExtractor.extract(allTrees);
      } else {
        bgug = (Pair) bgExtractor.extract(binaryTrainTrees);
      }
      bg = (BinaryGrammar) bgug.second;
      bg.splitRules();
      ug = (UnaryGrammar) bgug.first;
      ug.purgeRules();
      Timing.tick("done.");
    }
    System.err.print("Extracting Lexicon...");
    lex = op.tlpParams.lex(op.lexOptions);
    lex.train(binaryTrainTrees);
    Timing.tick("done.");

    if (op.doDep) {
      System.err.print("Extracting Dependencies...");
      binaryTrainTrees.clear();
      // dgBLIPP = (DependencyGrammar) dgExtractor.extract(new ConcatenationIterator(trainTreebank.iterator(),blippTreebank.iterator()),new TransformTreeDependency(tlpParams,true));

      DependencyGrammar dg1 = (DependencyGrammar) dgExtractor.extract(trainTreebank.iterator(), new TransformTreeDependency(op.tlpParams, true));
      //dgBLIPP=(DependencyGrammar)dgExtractor.extract(blippTreebank.iterator(),new TransformTreeDependency(tlpParams));

      //dg = (DependencyGrammar) dgExtractor.extract(new ConcatenationIterator(trainTreebank.iterator(),blippTreebank.iterator()),new TransformTreeDependency(tlpParams));
      // dg=new DependencyGrammarCombination(dg1,dgBLIPP,2);
      //dg = (DependencyGrammar) dgExtractor.extract(binaryTrainTrees); //uses information whether the words are known or not, discards unknown words
      Timing.tick("done.");
      //System.out.print("Extracting Unknown Word Model...");
      //UnknownWordModel uwm = (UnknownWordModel)uwmExtractor.extract(binaryTrainTrees);
      //Timing.tick("done.");
      System.out.print("Tuning Dependency Model...");
      dg.tune(binaryTestTrees);
      //System.out.println("TUNE DEPS: "+tuneDeps);
      Timing.tick("done.");
    }

    BinaryGrammar boundBG = bg;
    UnaryGrammar boundUG = ug;

    GrammarProjection gp = new NullGrammarProjection(bg, ug);

    // serialization
    if (serializeFile != null) {
      System.err.print("Serializing parser...");
      LexicalizedParser.saveParserDataToSerialized(new ParserData(lex, bg, ug, dg, Numberer.getNumberers(), op), serializeFile);
      Timing.tick("done.");
    }

    // test: pcfg-parse and output

    ExhaustivePCFGParser parser = null;
    if (op.doPCFG) {
      parser = new ExhaustivePCFGParser(boundBG, boundUG, lex, op);
    }


    ExhaustiveDependencyParser dparser = ((op.doDep && ! Test.useFastFactored) ? new ExhaustiveDependencyParser(dg, lex, op) : null);

    Scorer scorer = (op.doPCFG ? new TwinScorer(new ProjectionScorer(parser, gp), dparser) : null);
    //Scorer scorer = parser;
    BiLexPCFGParser bparser = null;
    if (op.doPCFG && op.doDep) {
      bparser = (Test.useN5) ? new BiLexPCFGParser.N5BiLexPCFGParser(scorer, parser, dparser, bg, ug, dg, lex, op, gp) : new BiLexPCFGParser(scorer, parser, dparser, bg, ug, dg, lex, op, gp);
    }

    LabeledConstituentEval pcfgPE = new LabeledConstituentEval("pcfg  PE", true, tlp);
    LabeledConstituentEval comboPE = new LabeledConstituentEval("combo PE", true, tlp);
    AbstractEval pcfgCB = new LabeledConstituentEval.CBEval("pcfg  CB", true, tlp);

    AbstractEval pcfgTE = new AbstractEval.TaggingEval("pcfg  TE");
    AbstractEval comboTE = new AbstractEval.TaggingEval("combo TE");
    AbstractEval pcfgTEnoPunct = new AbstractEval.TaggingEval("pcfg nopunct TE");
    AbstractEval comboTEnoPunct = new AbstractEval.TaggingEval("combo nopunct TE");
    AbstractEval depTE = new AbstractEval.TaggingEval("depnd TE");

    AbstractEval depDE = new AbstractEval.DependencyEval("depnd DE", true, tlp.punctuationWordAcceptFilter());
    AbstractEval comboDE = new AbstractEval.DependencyEval("combo DE", true, tlp.punctuationWordAcceptFilter());

    if (Test.evalb) {
      EvalB.initEVALBfiles(op.tlpParams);
    }

    // int[] countByLength = new int[Test.maxLength+1];

    // use a reflection ruse, so one can run this without needing the tagger
    //edu.stanford.nlp.process.SentenceTagger tagger = (Test.preTag ? new edu.stanford.nlp.process.SentenceTagger("/u/nlp/data/tagger.params/wsj0-21.holder") : null);
    SentenceProcessor tagger = null;
    if (Test.preTag) {
      try {
        Class[] argsClass = new Class[]{String.class};
        Object[] arguments = new Object[]{"/u/nlp/data/pos-tagger/wsj3t0-18-bidirectional/train-wsj-0-18.holder"};
        tagger = (SentenceProcessor) Class.forName("edu.stanford.nlp.tagger.maxent.MaxentTagger").getConstructor(argsClass).newInstance(arguments);
      } catch (Exception e) {
        System.err.println(e);
        System.err.println("Warning: No pretagging of sentences will be done.");
      }
    }

    for (int tNum = 0, ttSize = testTreebank.size(); tNum < ttSize; tNum++) {
      Tree tree = testTreebank.get(tNum);
      int testTreeLen = tree.yield().size();
      if (testTreeLen > Test.maxLength) {
        continue;
      }
      Tree binaryTree = binaryTestTrees.get(tNum);
      // countByLength[testTreeLen]++;
      System.out.println("-------------------------------------");
      System.out.println("Number: " + (tNum + 1));
      System.out.println("Length: " + testTreeLen);

      //tree.pennPrint(pw);
      // System.out.println("XXXX The binary tree is");
      // binaryTree.pennPrint(pw);
      //System.out.println("Here are the tags in the lexicon:");
      //System.out.println(lex.showTags());
      //System.out.println("Here's the tagnumberer:");
      //System.out.println(Numberer.getGlobalNumberer("tags").toString());

      long timeMil1 = System.currentTimeMillis();
      Timing.tick("Starting parse.");
      if (op.doPCFG) {
        //System.err.println(Test.forceTags);
        if (Test.forceTags) {
          if (tagger != null) {
            //System.out.println("Using a tagger to set tags");
            //System.out.println("Tagged sentence as: " + tagger.processSentence(cutLast(wordify(binaryTree.yield()))).toString(false));
            parser.parse(addLast(tagger.processSentence(cutLast(wordify(binaryTree.yield())))));
          } else {
            //System.out.println("Forcing tags to match input.");
            parser.parse(cleanTags(binaryTree.taggedYield(), tlp));
          }
        } else {
          // System.out.println("XXXX Parsing " + binaryTree.yield());
          parser.parse(binaryTree.yield());
        }
        //Timing.tick("Done with pcfg phase.");
      }
      if (op.doDep) {
        dparser.parse(binaryTree.yield());
        //Timing.tick("Done with dependency phase.");
      }
      boolean bothPassed = false;
      if (op.doPCFG && op.doDep) {
        bothPassed = bparser.parse(binaryTree.yield());
        //Timing.tick("Done with combination phase.");
      }
      long timeMil2 = System.currentTimeMillis();
      long elapsed = timeMil2 - timeMil1;
      System.err.println("Time: " + ((int) (elapsed / 100)) / 10.00 + " sec.");
      //System.out.println("PCFG Best Parse:");
      Tree tree2b = null;
      Tree tree2 = null;
      //System.out.println("Got full best parse...");
      if (op.doPCFG) {
        tree2b = parser.getBestParse();
        tree2 = debinarizer.transformTree(tree2b);
      }
      //System.out.println("Debinarized parse...");
      //tree2.pennPrint();
      //System.out.println("DepG Best Parse:");
      Tree tree3 = null;
      Tree tree3db = null;
      if (op.doDep) {
        tree3 = dparser.getBestParse();
        // was: but wrong Tree tree3db = debinarizer.transformTree(tree2);
        tree3db = debinarizer.transformTree(tree3);
        tree3.pennPrint(pw);
      }
      //tree.pennPrint();
      //((Tree)binaryTrainTrees.get(tNum)).pennPrint();
      //System.out.println("Combo Best Parse:");
      Tree tree4 = null;
      if (op.doPCFG && op.doDep) {
        try {
          tree4 = bparser.getBestParse();
          if (tree4 == null) {
            tree4 = tree2b;
          }
        } catch (NullPointerException e) {
          System.err.println("Blocked, using PCFG parse!");
          tree4 = tree2b;
        }
      }
      if (op.doPCFG && !bothPassed) {
        tree4 = tree2b;
      }
      //tree4.pennPrint();
      if (op.doDep) {
        depDE.evaluate(tree3, binaryTree, pw);
        depTE.evaluate(tree3db, tree, pw);
      }
      TreeTransformer tc = op.tlpParams.collinizer();
      TreeTransformer tcEvalb = op.tlpParams.collinizerEvalb();
      Tree tree4b = null;
      if (op.doPCFG) {
        // System.out.println("XXXX Best PCFG was: ");
        // tree2.pennPrint();
        // System.out.println("XXXX Transformed best PCFG is: ");
        // tc.transformTree(tree2).pennPrint();
        //System.out.println("True Best Parse:");
        //tree.pennPrint();
        //tc.transformTree(tree).pennPrint();
        pcfgPE.evaluate(tc.transformTree(tree2), tc.transformTree(tree), pw);
        pcfgCB.evaluate(tc.transformTree(tree2), tc.transformTree(tree), pw);
        if (op.doDep) {
          comboDE.evaluate((bothPassed ? tree4 : tree3), binaryTree, pw);
          tree4b = tree4;
          tree4 = debinarizer.transformTree(tree4);
          if (op.nodePrune) {
            NodePruner np = new NodePruner(parser, debinarizer);
            tree4 = np.prune(tree4);
          }
          //tree4.pennPrint();
          comboPE.evaluate(tc.transformTree(tree4), tc.transformTree(tree), pw);
        }
        //pcfgTE.evaluate(tree2, tree);
        pcfgTE.evaluate(tcEvalb.transformTree(tree2), tcEvalb.transformTree(tree), pw);
        pcfgTEnoPunct.evaluate(tc.transformTree(tree2), tc.transformTree(tree), pw);

        if (op.doDep) {
          comboTE.evaluate(tcEvalb.transformTree(tree4), tcEvalb.transformTree(tree), pw);
          comboTEnoPunct.evaluate(tc.transformTree(tree4), tc.transformTree(tree), pw);
        }
        System.out.println("PCFG only: " + parser.scoreBinarizedTree(tree2b, 0));

        //tc.transformTree(tree2).pennPrint();
        tree2.pennPrint(pw);

        if (op.doDep) {
          System.out.println("Combo: " + parser.scoreBinarizedTree(tree4b, 0));
          // tc.transformTree(tree4).pennPrint(pw);
          tree4.pennPrint(pw);
        }
        System.out.println("Correct:" + parser.scoreBinarizedTree(binaryTree, 0));
        /*
        if (parser.scoreBinarizedTree(tree2b,true) < parser.scoreBinarizedTree(binaryTree,true)) {
          System.out.println("SCORE INVERSION");
          parser.validateBinarizedTree(binaryTree,0);
        }
        */
        tree.pennPrint(pw);
      } // end if doPCFG

      if (Test.evalb) {
        if (op.doPCFG && op.doDep) {
          EvalB.writeEVALBline(tcEvalb.transformTree(tree), tcEvalb.transformTree(tree4));
        } else if (op.doPCFG) {
          EvalB.writeEVALBline(tcEvalb.transformTree(tree), tcEvalb.transformTree(tree2));
        } else if (op.doDep) {
          EvalB.writeEVALBline(tcEvalb.transformTree(tree), tcEvalb.transformTree(tree3db));
        }
      }
    } // end for each tree in test treebank

    if (Test.evalb) {
      EvalB.closeEVALBfiles();
    }

    // Test.display();
    if (op.doPCFG) {
      pcfgPE.display(false, pw);
      System.out.println("Grammar size: " + Numberer.getGlobalNumberer("states").total());
      pcfgCB.display(false, pw);
      if (op.doDep) {
        comboPE.display(false, pw);
      }
      pcfgTE.display(false, pw);
      pcfgTEnoPunct.display(false, pw);
      if (op.doDep) {
        comboTE.display(false, pw);
        comboTEnoPunct.display(false, pw);
      }
    }
    if (op.doDep) {
      depTE.display(false, pw);
      depDE.display(false, pw);
    }
    if (op.doPCFG && op.doDep) {
      comboDE.display(false, pw);
    }
    // pcfgPE.printGoodBad();
  }


  private static List<TaggedWord> cleanTags(List twList, TreebankLanguagePack tlp) {
    int sz = twList.size();
    List<TaggedWord> l = new ArrayList<TaggedWord>(sz);
    for (int i = 0; i < sz; i++) {
      TaggedWord tw = (TaggedWord) twList.get(i);
      TaggedWord tw2 = new TaggedWord(tw.word(), tlp.basicCategory(tw.tag()));
      l.add(tw2);
    }
    return l;
  }

  private static Sentence wordify(List wList) {
    Sentence s = new Sentence();
    for (Object obj : wList) {
      s.add(new Word(obj.toString()));
    }
    return s;
  }

  private static Sentence cutLast(Sentence s) {
    return new Sentence(s.subList(0, s.size() - 1));
  }

  private static Sentence addLast(Sentence s) {
    Sentence s2 = new Sentence(s);
    //s2.add(new StringLabel(Lexicon.BOUNDARY));
    s2.add(new Word(Lexicon.BOUNDARY));
    return s2;
  }

  /**
   * Not an instantiable class
   */
  private FactoredParser() {
  }

}
