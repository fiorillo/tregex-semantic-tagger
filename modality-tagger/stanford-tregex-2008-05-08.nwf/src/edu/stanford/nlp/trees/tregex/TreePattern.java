package edu.stanford.nlp.trees.tregex;

import edu.stanford.nlp.ling.StringLabelFactory;
import edu.stanford.nlp.process.AbstractTokenizer;
import edu.stanford.nlp.process.Tokenizer;
import edu.stanford.nlp.trees.*;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * A TreePattern is the obsolete class for specifying
 * a <code>tgrep</code>-type pattern.
 * <i>This class is still used by quite a bit of existing code, but the
 * code has various known problems.  New
 * code should use TregexPattern instead.</i>
 * <p/>
 * <p/>
 * Instances of a TreePattern
 * can be matched against instances of the {@link Tree} class.
 * TreeMatcher contains a main method for matching TreePattern objects.
 * Currently supported node/node relations and their symbols:
 * <p/>
 * <table border = "1">
 * <tr><th>Symbol<th>Meaning
 * <tr><td>A &#60;&#60; B <td>A dominates B
 * <tr><td>A &#62;&#62; B <td>A is dominated by B
 * <tr><td>A &#60; B <td>A immediately dominates B
 * <tr><td>A &#62; B <td>A is immediately dominated by B
 * <tr><td>A &#36; B <td>A is a sister of B (and not equal to B)
 * <tr><td>A .. B <td>A precedes B
 * <tr><td>A . B <td>A immediately precedes B
 * <tr><td>A &#60;&#60;, B <td>B is a leftmost descendent of A
 * <tr><td>A &#60;&#60;- B <td>B is a rightmost descendent of A
 * <tr><td>A &#62;&#62;, B <td>A is a leftmost descendent of B
 * <tr><td>A &#62;&#62;- B <td>A is a rightmost descendent of B
 * <tr><td>A &#60;, B <td>B is the first child of A
 * <tr><td>A &#62;, B <td>A is the first child of B
 * <tr><td>A &#60;- B <td>B is the last child of A
 * <tr><td>A &#62;- B <td>A is the last child of B
 * <tr><td>A &#60;i B <td>B is the ith child of A (i > 0)
 * <tr><td>A &#62;i B <td>A is the ith child of B (i > 0)
 * <tr><td>A &#60;-i B <td>B is the ith-to-last child of A (i > 0)
 * <tr><td>A &#62;-i B <td>A is the ith-to-last child of B (i > 0)
 * <tr><td>A &#60;: B <td>B is the only child of A
 * <tr><td>A &#36;++ B <td>A is a left sister of B
 * <tr><td>A $-- B <td>A is a right sister of B
 * <tr><td>A &#36;+ B <td>A is the immediate left sister of B
 * <tr><td>A $- B <td>A is the immediate right sister of B
 * <tr><td>A <+C B <td>A dominates B via an unbroken chain of (zero or more) nodes matching description C
 * <tr><td>A >+C B <td>A is dominated by B via an unbroken chain of (zero or more) nodes matching description C
 * <tr><td>A &#60;&#60;&#35; B <td>B is a head of phrase A
 * <tr><td>A &#62;&#62;&#35; B <td>A is a head of phrase B
 * <tr><td>A &#60;&#35; B <td>B is the immediate head of phrase A
 * <tr><td>A &#62;&#35; B <td>B is the immediate head of phrase A
 * </table>
 * <p/>
 * <p>Note that the operators are implicitly left-associative, so that "S
 * < VP < NP" is equivalent to "(S < VP) < NP", which matches
 * something where <i>both</i> a VP and an NP are directly below the S (in
 * no particular order), like
 * <pre>
 *     (S (NP (NNP Juan))
 *        (VP (VBD tried)))
 * </pre>
 * If instead what you want is an NP below a VP below an S, you should
 * write "S < (VP < NP)".</p>
 * <p/>
 * Current known bugs/shortcomings:
 * <p/>
 * <ul>
 * <p/>
 * <li> Node search currently takes no advantage of limitations
 * imposed by the queried relations.  This reduces the efficiency of
 * the search, quite a bit in some cases.
 * <p/>
 * </ul>
 *
 * @author Roger Levy
 */
public class TreePattern {

  static HeadFinder defaultHeadFinder = new CollinsHeadFinder();

  static final TreePattern[] zeroChildren = new TreePattern[0];

  // the name given to the node in the pattern, for external reference
  Object name;

  String description;

  // determines whether the node label description is to be negated
  boolean negatedDescription = false;

  // the relation that the node stands in with its parent.
  Relation relation;

  // determines whether the relation of the node to its parent is to be negated
  boolean negatedRelation = false;


  TreePattern parent; // null for the root node in the TreePattern
  TreePattern[] children;

  Map namesToNodes;

  Pattern descriptionPattern;

  private TreePattern(TreePattern[] children) {
    this.children = children;
    namesToNodes = new HashMap();
    for (int i = 0; i < children.length; i++) {
      children[i].parent = this;
    }
    unifyNamesToNodesMaps();
  }

  TreePattern(String description, Relation relation, TreePattern[] children) {
    this(description, nameNode(), relation, children);
  }

  TreePattern(String description, Object name, Relation relation, TreePattern[] children) {
    this(description, name, relation, children, true);
  }

  TreePattern(String description, Relation relation, TreePattern[] children, boolean negatedDescription) {
    this(description, nameNode(), relation, children, negatedDescription);
  }

  TreePattern(String description, Object name, Relation relation, TreePattern[] children, boolean negatedDescription) {
    this(children);
    this.description = description;
    this.relation = relation;
    this.name = name;
    descriptionPattern = Pattern.compile(this.description);
    this.negatedDescription = negatedDescription;
  }

  private void unifyNamesToNodesMaps() {
    for (int i = 0; i < children.length; i++) {
      children[i].namesToNodes = namesToNodes;
      children[i].unifyNamesToNodesMaps();
    }
  }

  /* returns the Tree node that has been matched to this node of the TreePattern */
  Tree node() {
    return (Tree) namesToNodes.get(name);
  }

  void setNode(Tree t) {
    namesToNodes.put(name, t);
  }

  private ListIterator nodesInOrder() {
    List l = new ArrayList();
    nodesInOrder(l);
    return l.listIterator();
  }

  private void nodesInOrder(List l) {
    l.add(this);
    for (int i = 0, n = children.length; i < n; i++) {
      children[i].nodesInOrder(l);
    }
  }

  TreePatternIterator iterator() {
    return new MyIterator(nodesInOrder());
  }

  private static class MyIterator implements TreePatternIterator {

    public MyIterator(ListIterator i) {
      this.i = i;
    }

    private final ListIterator i;

    public TreePattern next() {
      return (TreePattern) i.next();
    }

    public TreePattern previous() {
      return (TreePattern) i.previous();
    }

    public boolean hasNext() {
      return i.hasNext();
    }

    public boolean hasPrevious() {
      return i.hasPrevious();
    }

  }

  /**
   * Creates a <code>TreeMatcher</code> that can match
   * <code>this</code> pattern to the specified input tree.
   */
  public TreeMatcher matcher(Tree t) {
    return new TreeMatcher(t, this);
  }

  private static Pattern separateNamesPattern = Pattern.compile("^(\\S+)(=[^=/]+)$");
  private static Pattern separateLeftParensPattern = Pattern.compile("^\\((.+)$");
  private static Pattern separateRightParensPattern = Pattern.compile("^(.+)\\)$");


  /**
   * Compiles the given tree expression into a
   * <code>TreePattern</code> instance.
   */
  public static TreePattern compile(String str) {
    //str = str.replaceAll("\\("," \\( ");
    //str = str.replaceAll("\\)"," \\) ");

    List<String> tokens = Arrays.asList(str.split("\\s+"));
    tokens = separateParens(tokens);
    tokens = separateNames(tokens);

    if (TreeMatcher.Verbose.verbose) {
      System.out.println("Compiling " + str);
      System.out.println("Here are the tokens:\n" + tokens);
    }
    Tokenizer tokenizer = new ListTokenizer(tokens);

    TreePattern pattern = compile(tokenizer);
    if (tokenizer.hasNext()) {
      throw new RuntimeException("Error -- extra tokens at end of input TreePattern string!");
    }

    return pattern;
  }

  private static List<String> separateNames(List<String> tokens) {
    List<String> newTokens = new ArrayList<String>(tokens.size());
    for (String token : tokens) {
      Matcher m = separateNamesPattern.matcher(token);
      if (m.matches()) {
        newTokens.add(m.group(1));
        newTokens.add(m.group(2));
      } else {
        newTokens.add(token);
      }
    }
    return newTokens;
  }

  private static List<String> separateParens(List<String> tokens) {
    List<String> newTokens = new ArrayList<String>(tokens.size());
    for (String token : tokens) {
      Matcher m = separateLeftParensPattern.matcher(token);
      while (m.matches()) {
        newTokens.add("(");
        token = m.group(1);
        m = separateLeftParensPattern.matcher(token);
      }
      List<String> rightParensList = new ArrayList<String>();
      Matcher m1 = separateRightParensPattern.matcher(token);
      while (m1.matches()) {
        rightParensList.add(")");
        token = m1.group(1);
        m1 = separateRightParensPattern.matcher(token);
      }
      rightParensList.add(0, token);
      newTokens.addAll(rightParensList);
    }
    return newTokens;
  }

  private static TreePattern compile(Tokenizer tokenizer) {
    //    if(TreeMatcher.Verbose.verbose)
    //      System.out.println("###" + relationMap.toString());
    return parseNonTerminalTreePattern(tokenizer, Relation.ROOT);
  }

  private static TreePattern parseTerminalTreePattern(Tokenizer tokenizer, Relation r) {
    String str = (String) tokenizer.peek();
    Object name;
    if (namePattern.matcher(str).matches() || relationPattern.matcher(str).matches() || leftParPattern.matcher(str).matches() || rightParPattern.matcher(str).matches()) {
      throw new RuntimeException("Error -- terminal tree pattern parse method called for non-terminal tree pattern");
    }
    tokenizer.next();
    String next = (String) tokenizer.peek();
    if (next == null) {
      name = new Object();
    } else {
      Matcher m = namePattern.matcher(next);
      if (!m.matches()) {
        name = new Object();
      } else {
        tokenizer.next();
        name = m.group(1);
      }
    }
    NegationDescriptionPair p = formatDescriptionString(str);
    return new TreePattern(p.description, name, r, zeroChildren, p.negation);
  }

  private static TreePattern parseNonTerminalTreePattern(Tokenizer tokenizer, Relation r) {
    Object name = null;
    String str = (String) tokenizer.peek();
    if (rightParPattern.matcher(str).matches() || namePattern.matcher(str).matches()) {
      throw new RuntimeException("Error -- non-terminal tree pattern parse method called for name or ) token");
    }
    if (leftParPattern.matcher(str).matches()) {
      tokenizer.next();
      TreePattern pattern = parseNonTerminalTreePattern(tokenizer, r);
      String next = (String) tokenizer.peek();
      if (next == null || !rightParPattern.matcher(next).matches()) {
        throw new RuntimeException("Error -- unbalanced parenthesis!");
      } else {
        tokenizer.next();
        return pattern;
      }
    } else { // token was a node description
      tokenizer.next();
      String next = (String) tokenizer.peek();
      if (next != null) {
        Matcher m = namePattern.matcher(next);
        if (m.matches()) {
          tokenizer.next();
          if (TreeMatcher.Verbose.verbose) {
            System.out.println("###Matched as name: " + next);
          }
          name = m.group(1);
        }
      }
      TreePattern[] children = parseTreePatternChildren(tokenizer);
      NegationDescriptionPair p = formatDescriptionString(str);
      if (name != null) {
        return new TreePattern(p.description, name, r, children, p.negation);
      } else {
        return new TreePattern(p.description, r, children, p.negation);
      }
    }
  }

  private static TreePattern[] parseTreePatternChildren(Tokenizer tokenizer) {
    List kids = new ArrayList(0);
    return (TreePattern[]) parseTreePatternChildren(tokenizer, kids).toArray(zeroChildren);
  }

  private static List parseTreePatternChildren(Tokenizer tokenizer, List kids) {
    if (!tokenizer.hasNext()) {
      return kids;
    }
    String relnStr = (String) tokenizer.peek();
    if (rightParPattern.matcher(relnStr).matches()) {
      if (TreeMatcher.Verbose.verbose) {
        System.out.println("###Finished composing children list.");
      }
      return kids;
    }
    tokenizer.next();
    if (!relationPattern.matcher(relnStr).matches()) {
      throw new RuntimeException("Error -- invalid relation string: " + relnStr);
    }

    // look for arguments to relations
    String arg = null;
    Matcher m1 = numericArgRelationPattern.matcher(relnStr);
    if (m1.matches()) {
      //System.out.println("### found numeric argument " + m1.group(2) + " for " + m1.group(1));
      relnStr = m1.group(1);
      arg = m1.group(2);
    }
    m1 = stringArgRelationPattern.matcher(relnStr);
    if (m1.matches()) {
      //System.out.println("### Found string argument " + m1.group(2) + " for " + m1.group(1));
      relnStr = m1.group(1);
      arg = m1.group(2);
    }

    //RelationFactory rf = (RelationFactory) relationMap.get(relnStr);
    //System.err.println("### class of rf: " + rf.getClass().toString());
    Relation r;
    try {
      r = Relation.getRelation(relnStr, arg);
    } catch (ParseException e) {
      throw new RuntimeException(e);
    }
    //rf.relation( (String[]) args.toArray(new String[] {}) );

    if (TreeMatcher.Verbose.verbose) {
      System.out.println("### Found relation: " + r + " from string " + relnStr);
    }

    String str = (String) tokenizer.peek();
    if (namePattern.matcher(str).matches()) {
      throw new RuntimeException("Error -- node name token in wrong place.");
    }
    if (leftParPattern.matcher(str).matches()) {
      kids.add(parseNonTerminalTreePattern(tokenizer, r));
    } else {
      kids.add(parseTerminalTreePattern(tokenizer, r));
    }
    return parseTreePatternChildren(tokenizer, kids);
  }

  //throw new RuntimeException("Error -- bad token sequence for compilation into TreePattern\n + tokens");

  static final Pattern leftParPattern = Pattern.compile("\\(");
  static final Pattern rightParPattern = Pattern.compile("\\)");
  static final Pattern namePattern = Pattern.compile("\\A(=.*)\\z");
  static final Pattern relationPattern = Pattern.compile("[<>][-,`:+H]?|[<>]-?[0-9]|(?:<<|>>)[,`:-H]?|[.,]|\\.\\.|,,|\\$[.,+-]?|\\$\\.\\.|\\$,,|\\$\\+\\+|\\$--|=|<\\+.+|>\\+.+");

  static final Pattern numericArgRelationPattern = Pattern.compile("([<>])(-?[0-9]+)");
  static final Pattern stringArgRelationPattern = Pattern.compile("(<\\+|>\\+)(.+)");

  //
  //  static Map relationMap = new HashMap();
  //  //initialize relationMap
  //  static {
  //    relationMap.put("<",new ImmediatelyDominatesFactory());
  //    relationMap.put(">",new IsImmediatelyDominatedByFactory());
  //    relationMap.put("<<",new DominatesFactory());
  //    relationMap.put(">>",new IsDominatedByFactory());
  //    relationMap.put("<<-",new RightmostDescendentFactory());
  //    relationMap.put("<<,",new LeftmostDescendentFactory());
  //    relationMap.put(">>-",new InvertedRightmostDescendentFactory());
  //    relationMap.put(">>,",new InvertedLeftmostDescendentFactory());
  //    relationMap.put("<:",new OnlyChildOfFactory());
  //    relationMap.put("$",new SisterOfFactory());
  //    relationMap.put("$++",new LeftSisterOfFactory());
  //    relationMap.put("$--",new RightSisterOfFactory());
  //    relationMap.put("$+",new ImmediateLeftSisterOfFactory());
  //    relationMap.put("$-",new ImmediateRightSisterOfFactory());
  //    relationMap.put("..",new PrecedesFactory());
  //    relationMap.put(".",new ImmediatelyPrecedesFactory());
  //    relationMap.put("<+strArg",new UnbrokenCategoryDominatesFactory());
  //    relationMap.put(">+strArg",new UnbrokenCategoryIsDominatedByFactory());
  //    relationMap.put("<numArg", new HasIthChildFactory());
  //    relationMap.put(">-", new IsLastChildFactory());
  //    relationMap.put("<-", new HasLastChildFactory());
  //    relationMap.put(">,", new IsFirstChildFactory());
  //    relationMap.put("<,", new HasFirstChildFactory());
  //    relationMap.put(">H", new ImmediatelyHeadsFactory());
  //    relationMap.put("<H", new ImmediatelyHeadedByFactory());
  //    relationMap.put(">>H", new HeadsFactory());
  //    relationMap.put("<<H", new HeadedByFactory());
  //
  //
  //  }



  private static Object nameNode() {
    return new Object();
  }

  /**
   * Returns a representation of the TreePattern
   */
  public String toString() {
    String str = "";
    String end = "";

    if (relation != null) {
      str += relation.toString() + "( ";
      end = ") ";
    }

    if (name instanceof String) {
      str += description + name + " ";
    } else {
      str += description + " ";
    }

    for (int i = 0; i < children.length; i++) {
      str += children[i].toString();
    }
    str += end;
    return str;
  }

  static final Pattern quotedString = Pattern.compile("\"(.*)\"");
  static final Pattern regularExpressionString = Pattern.compile("/(.*)/");
  static final Pattern wildCardString = Pattern.compile("__|\\*");

  /* formats the description string -- checks whether it's a regex,
   * etc. */
  static NegationDescriptionPair formatDescriptionString(String str) {
    NegationDescriptionPair p = new NegationDescriptionPair();

    // check if string is negatedDescription
    if (str.charAt(0) == '!') {
      p.negation = true;
      str = str.substring(1);
    }

    // check if description is wildcard
    if (wildCardString.matcher(str).matches()) {
      p.description = ".*";
      return p;
    }

    Matcher m = regularExpressionString.matcher(str);
    if (m.matches()) { // interpret description as regex
      String regex = m.group(1);
      String regexStart;
      String regexEnd;
      if (regex.startsWith("^")) {
        regexStart = "^";
        regex = regex.substring(1);
      } else {
        regexStart = ".*";
      }
      if (regex.endsWith("$")) {
        regexEnd = "$";
        regex = regex.substring(0, regex.length() - 1);
      } else {
        regexEnd = ".*";
      }
      p.description = regexStart + regex + regexEnd;
      return p;
    }

    // else non-regex
    String[] alternates = str.split("\\|"); // split alternates by "|"

    for (int i = 0, n = alternates.length; i < n; i++) {
      Matcher q = quotedString.matcher(alternates[i]);
      if (q.matches()) {
        String quoted = q.group(1);
        alternates[i] = deescapeQuotes(quoted);
      } else {
        if (hasUnquotedSpecials(alternates[i])) {
          throw new RuntimeException("error -- unquoted specials in description subsequence " + alternates[i]);
        }
      }

    }

    StringBuilder result = new StringBuilder();
    for (int i = 0, n = alternates.length - 1; i < n; i++) {
      result.append(alternates[i]).append("|");
    }
    result.append(alternates[alternates.length - 1]);

    p.description = result.toString();
    return p;

  }

  static class NegationDescriptionPair {
    boolean negation = false;
    String description;
  }


  private static Pattern escapedQuote = Pattern.compile("\\\"");
  private static Pattern unEscapedQuote = Pattern.compile(".*[^\\\\]\".*");

  private static String deescapeQuotes(String str) {
    if (unEscapedQuote.matcher(str).matches()) {
      throw new RuntimeException("error -- unescaped quotation mark \" contained in description string!");
    } else {
      Matcher m = escapedQuote.matcher(str);
      return m.replaceAll("\"");
    }
  }

  private static Pattern specials = Pattern.compile("[*$#&%!]");

  private static boolean hasUnquotedSpecials(String str) {
    return specials.matcher(str).find();
  }


  /* returns number of nodes in TreePattern */
  int size() {
    if (children.length == 0) {
      return 1;
    }
    int numKids = 0;
    for (int i = 0, n = children.length; i < n; i++) {
      numKids += children[i].size();
    }
    return numKids;
  }


  /**
   * returns the tree-pattern expression from which the pattern was
   * compiled.
   */
  public String pattern() {
    StringBuilder sb = new StringBuilder();
    sb.append(description);
    if (name instanceof String) {
      sb.append(name);
    }
    sb.append(" ");
    for (int i = 0, n = children.length; i < n; i++) {
      sb.append(children[i].childPattern());
    }
    return sb.toString();
  }

  private String childPattern() {
    if (relation == null) {
      throw new RuntimeException("Error -- null relation at node " + pattern());
    }
    StringBuilder sb = new StringBuilder();
    sb.append(relation.toString());
    sb.append(" ");
    if (children.length > 0) {
      sb.append("( ");
    }
    sb.append(description);
    if (name instanceof String) {
      sb.append(" ").append(name);
    }
    sb.append(" ");
    for (int i = 0, n = children.length; i < n; i++) {
      sb.append(children[i].childPattern());
    }
    if (children.length > 0) {
      sb.append(") ");
    }
    return sb.toString();
  }

  // below here is new stuff for doing matching along the structure of the TreePattern more directly

  TreePattern lastNode; // the last TreePattern node to be involved in the match
  Tree root;            // the root of the Tree to be matched against
  Iterator it;          // Iterator over candidate nodes of the Tree to match this.nodeDescription

  TreePatternIterator childrenIterator;  // two-way iterator over chidren of this TreePattern node
  TreePattern currentChild;              // current child being matched in the TreePattern node

  TreePatternReturnValue currentResult;  // a pair of the last TreePattern node to be involved and the success of the last match

  public void reset(Tree root) {
    initializeSearchOnTree(root, root);
    this.root = root;
    lastNode = this;
  }

  public boolean findNext() {
    TreePatternReturnValue result = processForward(root);
    lastNode = result.finalNode;
    return result.success;
  }

  public Tree returnMatch() {
    return node();
  }

  TreePatternReturnValue processForward(Tree root) {
    boolean matchesNode = matchOnTree(root);
    if (matchesNode) {
      initializeChildrenIterator();
      return processChildrenForward(root);
    } else {
      if (parent == null) {
        return new TreePatternReturnValue(this);
      }
      return parent.processBackward(root);

    }
  }

  void initializeSearchOnTree(Tree t, Tree root) {
    it = relation.searchNodeIterator(t, root);
  }

  boolean matchOnTree(Tree root) {
    while (it.hasNext()) {
      Tree t = (Tree) it.next();
      boolean descriptionMatchesNode = descriptionPattern.matcher(t.label().value()).matches();
      if ((descriptionMatchesNode ^ negatedDescription) && (parent == null || relation.satisfies(parent.node(), t, root))) {
        setNode(t);
        return true;
      }
    }
    return false;
  }

  public void setCurrentResult(TreePatternReturnValue currentResult) {
    this.currentResult = currentResult;
  }

  private void initializeChildrenIterator() {
    currentResult = new TreePatternReturnValue(this);
    childrenIterator = new MyIterator(Arrays.asList(children).listIterator());
  }

  TreePatternReturnValue processChildrenForward(Tree root) {
    if (!childrenIterator.hasNext()) {
      currentResult.success = true;
      if (parent == null) {
        return currentResult;
      } else {
        parent.setCurrentResult(currentResult);
        return parent.processChildrenForward(root);
      }
    } else {
      currentChild = childrenIterator.next();
      currentChild.initializeSearchOnTree(node(), root);
      return currentChild.processForward(root);
    }
  }

  TreePatternReturnValue processBackward(Tree root) {
    if (!childrenIterator.hasPrevious()) {
      throw new RuntimeException("error -- processBackward() should only be called by a child on a parent");
    } else {
      childrenIterator.previous();
    }
    if (childrenIterator.hasPrevious()) {
      currentChild = childrenIterator.previous();
      childrenIterator.next();
      return currentChild.processForward(root);
    } else {
      boolean matchNewTreeNode = matchOnTree(root);
      if (matchNewTreeNode) {
        return processChildrenForward(root);
      } else {
        if (parent == null) {
          return currentResult;
        }
        return parent.processBackward(root);
      }
    }
  }


  /**
   * Used to check how tree patterns are compiled, and how node
   * descriptions are compiled into regular expressions.
   */
  public static void main(String[] args) {
    TreePattern p = TreePattern.compile(args[0]);

    System.out.println(p.pattern());
    //System.out.println(formatDescriptionString(args[1]));

    Tree t = null;
    try {
      t = (new PennTreeReader(new StringReader("(VP (VP (VBZ Try) (NP (NP (DT this) (NN wine)) (CC and) (NP (DT these) (NNS snails)))) (PUNCT .))"), new LabeledScoredTreeFactory(new StringLabelFactory()))).readTree();
    } catch (IOException e) {
    }

    p.reset(t);
    if (p.findNext()) {
      p.node().pennPrint();
    }
  }


  static class ListTokenizer extends AbstractTokenizer {
    ListIterator li;

    /**
     * Not supported, since this Tokenizer overrides next() and hasNext() and peek() and remove().
     */
    protected Object getNext() {
      throw new UnsupportedOperationException();
    }

    public ListTokenizer(List l) {
      li = l.listIterator();
    }

    public boolean hasNext() {
      return li.hasNext();
    }

    public Object next() {
      return li.next();
    }

    public Object peek() {
      if (!li.hasNext()) {
        return null;
      }
      Object result = li.next();
      li.previous();
      return result;
    }

    public void remove() {
      li.remove();
    }

    public void setSource(Reader r) {
      throw new UnsupportedOperationException();
    }

  } // end class ListTokenizer


} // end class TreePattern

