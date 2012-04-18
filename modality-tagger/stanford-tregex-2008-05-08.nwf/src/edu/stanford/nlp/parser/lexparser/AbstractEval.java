package edu.stanford.nlp.parser.lexparser;

import edu.stanford.nlp.ling.*;
import edu.stanford.nlp.stats.ClassicCounter;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.UnnamedDependency;
import edu.stanford.nlp.util.Filter;
import edu.stanford.nlp.util.Pair;
import edu.stanford.nlp.parser.KBestViterbiParser;

import java.util.*;
import java.io.PrintWriter;
import java.text.NumberFormat;
import java.text.DecimalFormat;


/**
 * A framework for Set-based precision/recall/F1 evaluation.
 *
 * @author Dan Klein
 */
public abstract class AbstractEval {

  private static final boolean DEBUG = false;

  protected final String str;
  protected final boolean runningAverages;

  private double precision = 0.0;
  private double recall = 0.0;
  private double f1 = 0.0;
  protected double num = 0.0;
  private double exact = 0.0;

  private double precision2 = 0.0;
  private double recall2 = 0.0;
  private double pnum2 = 0.0;
  private double rnum2 = 0.0;

  public AbstractEval() {
    this(true);
  }

  public AbstractEval(boolean runningAverages) {
    this("", runningAverages);
  }

  public AbstractEval(String str) {
    this(str, true);
  }

  public AbstractEval(String str, boolean runningAverages) {
    this.str = str;
    this.runningAverages = runningAverages;
  }

  public double getSentAveF1() {
    return f1 / num;
  }

  public double getEvalbF1() {
    return 2.0 / (rnum2 / recall2 + pnum2 / precision2);
  }

  /** @return The evalb (micro-averaged) F1 times 100 to make it
   *  a number between 0 and 100.
   */
  public double getEvalbF1Percent() {
    return getEvalbF1() * 100.0;
  }

  public double getExact() {
    return exact / num;
  }

  public double getExactPercent() {
    return getExact() * 100.0;
  }

  public int getNum() {
    return (int) num;
  }

  // should be able to pass in a comparator!
  protected double precision(Set s1, Set s2) {
    double n = 0.0;
    double p = 0.0;
    for (Object o1 : s1) {
      if (s2.contains(o1)) {
        p += 1.0;
      }
      if (DEBUG) {
        if (s2.contains(o1)) {
          System.err.println("Eval Found: "+o1);
        } else {
          System.err.println("Eval Failed to find: "+o1);
        }
      }
      n += 1.0;
    }
    if (DEBUG) System.err.println("Matched " + p + " of " + n);
    return (n > 0.0 ? p / n : 0.0);
  }

  abstract Set makeObjects(Tree tree);

  public void evaluate(Tree guess, Tree gold) {
    evaluate(guess, gold, new PrintWriter(System.out, true));
  }

  /* Evaluates precision and recall by calling makeObjects() to make a
   * set of structures for guess Tree and gold Tree, and compares them
   * with each other.
   */
  public void evaluate(Tree guess, Tree gold, PrintWriter pw) {
    if (DEBUG) {
      System.err.println("Evaluating gold tree:");
      gold.pennPrint(System.err);
      System.err.println("and guess tree");
      guess.pennPrint(System.err);
    }
    Set dep1 = makeObjects(guess);
    Set dep2 = makeObjects(gold);
    double curPrecision = precision(dep1, dep2);
    double curRecall = precision(dep2, dep1);
    double curF1 = (curPrecision > 0.0 && curRecall > 0.0 ? 2.0 / (1.0 / curPrecision + 1.0 / curRecall) : 0.0);
    precision += curPrecision;
    recall += curRecall;
    f1 += curF1;
    num += 1.0;

    precision2 += dep1.size() * curPrecision;
    pnum2 += dep1.size();

    recall2 += dep2.size() * curRecall;
    rnum2 += dep2.size();

    if (curF1 > 0.9999) {
      exact += 1.0;
    }
    if (pw != null) {
      pw.print(" P: " + ((int) (curPrecision * 10000)) / 100.0);
      if (runningAverages) {
        pw.println(" (sent ave " + ((int) (precision * 10000 / num)) / 100.0 + ") (evalb " + ((int) (precision2 * 10000 / pnum2)) / 100.0 + ")");
      }
      pw.print(" R: " + ((int) (curRecall * 10000)) / 100.0);
      if (runningAverages) {
        pw.print(" (sent ave " + ((int) (recall * 10000 / num)) / 100.0 + ") (evalb " + ((int) (recall2 * 10000 / rnum2)) / 100.0 + ")");
      }
      pw.println();
      double cF1 = 2.0 / (rnum2 / recall2 + pnum2 / precision2);
      pw.print(str + " F1: " + ((int) (curF1 * 10000)) / 100.0);
      if (runningAverages) {
        pw.print(" (sent ave " + ((int) (10000 * f1 / num)) / 100.0 + ", evalb " + ((int) (10000 * cF1)) / 100.0 + ")   Exact: " + ((int) (10000 * exact / num)) / 100.0);
      }
      pw.println(" N: " + getNum());
    }
    /*
      Sentence s = guess.yield();
      for (Object obj : s) {
        if (curF1 < 0.7) {
          badwords.incrementCount(obj);
        } else {
          goodwords.incrementCount(obj);
        }
      }
    */
  }

  /*
  private Counter goodwords = new Counter();
  private Counter badwords = new Counter();

  public void printGoodBad() {
    System.out.println("Printing bad categories");
    for (Object key : badwords.keysAbove(5.0)) {
      System.out.println("In badwords 5 times: " + key);
      double numb = badwords.getCount(key);
      double numg = goodwords.getCount(key);
      if (numb / (numb + numg) > 0.1) {
        System.out.println("Bad word!  " + key + " (" +
                           (numb / (numb + numg)) + " bad)");
        // EncodingPrintWriter.out.println("Bad word!  " + key + " (" +
        //                 (numb / (numb + numg)) + " bad)",
        //                              "GB18030");
      }
    }
  }
  */

  public void display(boolean verbose) {
    display(verbose, new PrintWriter(System.out, true));
  }

  public void display(boolean verbose, PrintWriter pw) {
    double prec = precision2 / pnum2;//(num > 0.0 ? precision/num : 0.0);
    double rec = recall2 / rnum2;//(num > 0.0 ? recall/num : 0.0);
    double f = 2.0 / (1.0 / prec + 1.0 / rec);//(num > 0.0 ? f1/num : 0.0);
    //System.out.println(" Precision: "+((int)(10000.0*prec))/100.0);
    //System.out.println(" Recall:    "+((int)(10000.0*rec))/100.0);
    //System.out.println(" F1:        "+((int)(10000.0*f))/100.0);
    pw.println(str + " summary evalb: LP: " + ((int) (10000.0 * prec)) / 100.0 + " LR: " + ((int) (10000.0 * rec)) / 100.0 + " F1: " + ((int) (10000.0 * f)) / 100.0 + " Exact: " + ((int) (10000.0 * exact / num)) / 100.0 + " N: " + getNum());
    /*
    double prec = (num > 0.0 ? precision/num : 0.0);
    double rec = (num > 0.0 ? recall/num : 0.0);
    double f = (num > 0.0 ? f1/num : 0.0);
    System.out.println(" Precision: "+prec);
    System.out.println(" Recall:    "+rec);
    System.out.println(" F1:        "+f);
    */
  }


  /** Evaluates the dependency accuracy of a tree (based on HeadFinder
   *  dependency judgements).
   *  CDM Mar 2004: This should be rewritten so as to root a word at an
   *  index position; otherwise it doesn't work correctly when you get two
   *  identical dependents (like with "I went to Greece to see the ruins").
   */
  public static class DependencyEval extends AbstractEval {

    private static final boolean DEBUG = false;
    Filter<String> punctFilter;

    /**
     * Build the set of dependencies for evaluation.  This set excludes
     * all dependencies for which the argument is a punctuation tag.
     */
    Set makeObjects(Tree tree) {
      Set deps = new HashSet();
      for (Tree node : tree.subTreeList()) {
        // every child with a different head is an argument, as are ones with
        // the same head after the first one found
        if (node.isLeaf() || node.children().length < 2) {
          continue;
        }
        // System.err.println("XXX node is " + node + "; label type is " +
        //                         node.label().getClass().getName());
        String head = ((HasWord) node.label()).word();
        boolean seenHead = false;
        for (int cNum = 0; cNum < node.children().length; cNum++) {
          Tree child = node.children()[cNum];
          String arg = ((HasWord) child.label()).word();
          if (head.equals(arg) && !seenHead) {
            seenHead = true;
          } else if (!punctFilter.accept(arg)) {
            deps.add(new UnnamedDependency(head, arg));
          }
        }
      }
      if (DEBUG) {
        System.err.println("Deps: " + deps);
      }
      return deps;
    }

    /** @param punctFilter A filter that accepts punctuation words.
     */
    public DependencyEval(String str, boolean runningAverages, Filter<String> punctFilter) {
      super(str, runningAverages);
      this.punctFilter = punctFilter;
    }

  } // end class DependencyEval


  public static class TaggingEval extends AbstractEval {

    private static final boolean DEBUG = false;
    private static final boolean DEBUG_MORE = false;
    private final Lexicon lex;
    private final boolean useTag;

    Set makeObjects(Tree tree) {
      List twList;
      if (useTag) {
        twList = myExtractor(tree);
      } else {
        twList = tree.taggedYield();
      }
      Set set = new HashSet();
      for (int i = 0, sz = twList.size(); i < sz; i++) {
        TaggedWord tw = (TaggedWord) twList.get(i);
        //IntTaggedWord iTW = new IntTaggedWord(Numberer.number("words",tw.word()), Numberer.number("tags",tw.tag()));
        Pair positionWT = new Pair(new Integer(i), new WordTag(tw.value(), tw.tag()));
        //WordTag positionWT = new WordTag(tw.value(),tw.tag());
        //System.out.println(iTW);
        //if (! tw.tag.equals("*"))
        set.add(positionWT);
      }
      if (DEBUG_MORE) System.err.println("Tags: " + set);
      return set;
    }

    public TaggingEval(String str) {
      this(str, true, null);
    }

    public TaggingEval(String str, boolean runningAverages, Lexicon lex) {
      this(str, runningAverages, lex, false);
    }

    /** @param useTag If true, use a special way of getting
     *    the tags out of a dependency tree, when the usual
     *    Tree code doesn't work.
     */
    public TaggingEval(String str, boolean runningAverages, Lexicon lex, boolean useTag) {
      super(str, runningAverages);
      this.lex = lex;
      this.useTag = useTag;
    }

    private static Sentence myExtractor(Tree t) {
      return myExtractor(t, new Sentence());
    }

    private static Sentence myExtractor(Tree t, Sentence ty) {
      Tree[] kids = t.children();
      // this inlines the content of t.isPreTerminal()
      if (kids.length == 1 && kids[0].isLeaf()) {
        if (t.label() instanceof HasTag) {
          //   System.err.println("Object is: " + ((CategoryWordTag) t.label()).toString("full"));
          ty.add(new TaggedWord(kids[0].label().value(), ((HasTag) t.label()).tag()));
        } else {
        //   System.err.println("Object is: " + StringUtils.getShortClassName(t.label()) + " " + t.label());
          ty.add(new TaggedWord(kids[0].label().value(), t.label().value()));
        }
      } else {
        for (int i = 0; i < kids.length; i++) {
          myExtractor(kids[i], ty);
        }
      }
      return ty;
    }

    public void evaluate(Tree guess, Tree gold, PrintWriter pw) {
      Sentence sGold = gold.taggedYield();
      Sentence sGuess;
      if (useTag) {
        sGuess = myExtractor(guess);
      } else {
        sGuess = guess.taggedYield();
      }
      if (sGuess.size() != sGold.size()) {
        pw.println("Warning: yield length differs:");
        pw.println("Guess: " + sGuess);
        pw.println("Gold: " + sGold);
      } else {
        if (DEBUG) {
          for (Iterator goldIt = sGold.iterator(), guessIt = sGuess.iterator(); goldIt.hasNext();) {
            TaggedWord goldNext = (TaggedWord) goldIt.next();
            TaggedWord guessNext = (TaggedWord) guessIt.next();
            if (!goldNext.tag().equals(guessNext.tag())) {
              pw.print("TaggingError ");
              if (lex != null && lex.isKnown(goldNext.word())) {
                pw.print("seen ");
              } else {
                pw.print("unseen ");
              }
              pw.println(goldNext.word() + " correct " + goldNext.tag() + " chose " + guessNext.tag());
            }
          }
        }
      }
      super.evaluate(guess, gold, pw);
    }

  } // end class TaggingEval


  public static class RuleErrorEval extends AbstractEval {

    private boolean verbose = false;

    private ClassicCounter over = new ClassicCounter();
    private ClassicCounter under = new ClassicCounter();

    protected static String localize(Tree tree) {
      if (tree.isLeaf()) {
        return "";
      }
      StringBuffer sb = new StringBuffer();
      sb.append(tree.label());
      sb.append(" ->");
      for (int i = 0; i < tree.children().length; i++) {
        sb.append(" ");
        sb.append(tree.children()[i].label());
      }
      return sb.toString();
    }

    Set makeObjects(Tree tree) {
      Set localTrees = new HashSet();
      for (Tree st : tree.subTreeList()) {
        localTrees.add(localize(st));
      }
      return localTrees;
    }

    public void evaluate(Tree t1, Tree t2, PrintWriter pw) {
      Set s1 = makeObjects(t1);
      Set s2 = makeObjects(t2);
      for (Object o1 : s1) {
        if (!s2.contains(o1)) {
          over.incrementCount(o1);
        }
      }
      for (Object o2 : s2) {
        if (!s1.contains(o2)) {
          under.incrementCount(o2);
        }
      }
    }

    protected void display(ClassicCounter c, int num, PrintWriter pw) {
      List rules = new ArrayList(c.keySet());
      Collections.sort(rules, c.comparator(false));
      int rSize = rules.size();
      if (num > rSize) {
        num = rSize;
      }
      for (int i = 0; i < num; i++) {
        pw.println(rules.get(i) + " " + c.getCount(rules.get(i)));
      }
    }

    public void display(boolean verbose, PrintWriter pw) {
      this.verbose = verbose;
      pw.println("Most frequently underproposed rules:");
      display(under, (verbose ? 100 : 10), pw);
      pw.println("Most frequently overproposed rules:");
      display(over, (verbose ? 100 : 10), pw);
    }

    public RuleErrorEval(String str) {
      super(str);
    }

  } // end class RuleErrorEval


  /** This class counts which categories are over and underproposed in trees.
   */
  public static class CatErrorEval extends AbstractEval {

    private ClassicCounter over = new ClassicCounter();
    private ClassicCounter under = new ClassicCounter();

    /** Unused. Fake satisfying the abstract class. */
    Set makeObjects(Tree tree) {
      return null;
    }

    List myMakeObjects(Tree tree) {
      List cats = new LinkedList();
      for (Tree st : tree.subTreeList()) {
        cats.add(st.value());
      }
      return cats;
    }

    public void evaluate(Tree t1, Tree t2, PrintWriter pw) {
      List s1 = myMakeObjects(t1);
      List s2 = myMakeObjects(t2);
      List del2 = new LinkedList(s2);
      // we delete out as we find them so we can score correctly a cat with
      // a certain cardinality in a tree.
      for (Object o1 : s1) {
        if ( ! del2.remove(o1)) {
          over.incrementCount(o1);
        }
      }
      for (Object o2 : s2) {
        if (! s1.remove(o2)) {
          under.incrementCount(o2);
        }
      }
    }

    protected void display(ClassicCounter c, PrintWriter pw) {
      List cats = new ArrayList(c.keySet());
      Collections.sort(cats, c.comparator(false));
      for (Object ob : cats) {
        pw.println(ob + " " + c.getCount(ob));
      }
    }

    public void display(boolean verbose, PrintWriter pw) {
      pw.println("Most frequently underproposed categories:");
      display(under, pw);
      pw.println("Most frequently overproposed categories:");
      display(over, pw);
    }

    public CatErrorEval(String str) {
      super(str);
    }

  } // end class CatErrorEval


  /** This isn't really a kind of AbstractEval: we're sort of cheating here. */
  public static class ScoreEval extends AbstractEval {

    double totScore = 0.0;
    double n = 0.0;
    NumberFormat nf = new DecimalFormat("0.000");

    Set makeObjects(Tree tree) {
      return null;
    }

    public void recordScore(KBestViterbiParser parser, PrintWriter pw) {
      double score = parser.getBestScore();
      totScore += score;
      n++;
      if (pw != null) {
        pw.print(str + " score: " + nf.format(score));
        if (runningAverages) {
          pw.print(" average score: " + nf.format(totScore / n));
        }
        pw.println();
      }
    }

    public void display(boolean verbose, PrintWriter pw) {
      if (pw != null) {
        pw.println(str + " total score: " + nf.format(totScore) +
                " average score: " + ((n == 0.0) ? "N/A": nf.format(totScore / n)));
      }
    }

    public ScoreEval(String str, boolean runningAverages) {
      super(str, runningAverages);
    }

  } // end class DependencyEval




} // end class AbstractEval
