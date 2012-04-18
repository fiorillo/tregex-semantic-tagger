package edu.stanford.nlp.trees.tregex;

import edu.stanford.nlp.ling.StringLabelFactory;
import edu.stanford.nlp.trees.*;
import edu.stanford.nlp.util.Timing;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * An obsolete class for matching a {@link TreePattern} against a {@link Tree}.
 * <i>This class is still used by quite a bit of existing code, but has
 * various known problems. New
 * code should use TregexMatcher instead.</i>
 * <p/>
 * For programmatic usage, the important methods are:
 * <p/>
 * <ul>
 * <li> {@link #reset} resets the TreeMatcher
 * <li> {@link #find} attempts to find the next match of the
 * TreePattern to the Tree.
 * <li> {@link #matches} determines whether the TreePattern matches
 * the Tree at the root of the Tree.
 * <li> {@link #getMatch} once a match has been found, returns the
 * Tree node corresponding to the root of the TreePattern.
 * <li> {@link #getNode} once a match has been found, allows you to
 * use the name given to a node description in the TreePattern to
 * retrieve the node matched to that node description
 * </ul>
 * <p>For running it from the command line, see the <code>main</code> method
 * for a description of command line arguments and the <code>TreePattern</code>
 * class for a description of supported tree patterns.
 *
 * @author Roger Levy
 */
public class TreeMatcher {

  private Tree root;
  private TreePattern rootPattern;

  private TreePatternIterator i; /* iterator over TreePattern nodes */
  private TreePattern currentPatternNode; /* most recent node from i */

  private List stack; /* stack of iterators over Tree nodes */
  private Iterator currentTreeIterator; /* not on the stack */

  /**
   * Initializes the TreeMatcher.
   */
  TreeMatcher(Tree t, TreePattern p) {
    root = t;
    rootPattern = p;
    reset();
  }

  /**
   * Attempts to match the input {@link TreePattern} against the
   * specified Tree, <i>at the supplied {@link Tree} node</i> <code>t</code>.
   * Stops at the first full match, if any.
   * <p/>
   * <p> At the moment, matches() is fairly inefficiently implemented:
   * for cases where the root tree-pattern node description matches
   * <code>t</code> but the rest of the pattern does not match given
   * that assignment, it actually traverses the entire tree and the
   * entire pattern to look for a deeper complete match before
   * rejecting any that it may find. Hopefully this will be fixed in
   * the future.
   *
   * @param t the supplied tree node for a match
   */
  public boolean matches(Tree t) {
    reset();
    if (!rootPattern.descriptionPattern.matcher(t.label().value()).matches()) {
      return false;
    } else {
      while (find()) {
        if (t == getMatch()) {
          return true;
        }
      }
      return false;
    }
  }

  /**
   * Resets the TreeMatcher.
   */
  public TreeMatcher reset() {
    i = rootPattern.iterator();
    currentPatternNode = i.next();
    stack = new ArrayList(rootPattern.size());
    currentTreeIterator = root.iterator();
    return this;
  }

  /**
   * Attempts to find the next match, if any.
   */
  public boolean find() {
    while (true) {
      if (!currentTreeIterator.hasNext()) {
        i.previous(); // needed due to semantics of ListIterator
        while (!currentTreeIterator.hasNext()) {
          if (!i.hasPrevious()) {
            return false;
          } else {
            currentPatternNode = i.previous();
            currentTreeIterator = (Iterator) stack.remove(stack.size() - 1);
            if (Verbose.verbose) {
              System.out.println("###Stack: " + stack);
            }
          }
        }
        i.next();
      }
      Tree t = (Tree) currentTreeIterator.next();
      //       if(t==null) {
      //      throw new RuntimeException("null tree iterated to");
      //       }
      if (Verbose.verbose) {
        System.out.println("###" + t + "\t" + currentPatternNode.relation + "(" + (currentPatternNode.parent == null ? "--" : currentPatternNode.parent.description) + "," + currentPatternNode.description + ")");
      }

      boolean descriptionMatchesNode = currentPatternNode.descriptionPattern.matcher(t.label().value()).matches();
      if (((descriptionMatchesNode && !currentPatternNode.negatedDescription) || currentPatternNode.negatedDescription && !descriptionMatchesNode) && (currentPatternNode.parent == null || currentPatternNode.relation.satisfies(currentPatternNode.parent.node(), t, root))) {
        if (Verbose.verbose) {
          System.out.println("###matched node description " + currentPatternNode.descriptionPattern.pattern() + " at node " + t);
        }
        currentPatternNode.namesToNodes.put(currentPatternNode.name, t);
        if (!i.hasNext()) {
          return true;
        } else {
          currentPatternNode = i.next();
          stack.add(currentTreeIterator);
          if (Verbose.verbose) {
            System.out.println("###Stack: " + stack);
            System.out.println("Last iterator at: " + t);
          }
          currentTreeIterator = currentPatternNode.relation.searchNodeIterator(currentPatternNode.parent.node(), root);
        }
      }
    }
  }

  /**
   * Returns the root node currently matched by the TreePattern
   */
  public Tree getMatch() {
    return rootPattern.node();
  }

  /**
   * Returns the relevant node in the pattern by the name it was
   * given in the TreePattern
   */
  public Tree getNode(Object name) {
    if (Verbose.verbose) {
      System.out.println("###Here's the names to nodes map:\n" + rootPattern.namesToNodes);
    }
    return (Tree) rootPattern.namesToNodes.get(name);
  }


  private static Treebank treebank; // used by main method, must be accessible

  /**
   * Use to match a tree pattern to the trees in files.
   * Usage: <p><code>
   * java edu.stanford.nlp.trees.tregex.TreeMatcher
   * [-T] [-C] [-w] [-f] [-tn TN]
   * pattern [handle] filepath   </code><p>
   * It prints out all the matches of the tree pattern to every tree in the
   * treebank rooted at the filepath.
   *
   * @param args Command line arguments: Argument 1 is the tree pattern which
   *             might name a node with =name (for some arbitrary string "name"),
   *             argument 2 is an optional name =name, and argument 3 is a filepath
   *             to files with trees.  A -T flag causes all trees to be printed as
   *             processed.  Otherwise just matches are printed.  The -C flag
   *             suppresses printing of matches, so only a number of matches is
   *             printed.  The -w flag causes the whole of a tree that matches to
   *             be printed.  The -f flag prints the filename of the file containing
   *             each match.  The -tn flag is followed by the name of a
   *             TreeNormalizer class, which can be used to normalize trees before
   *             they are matched (e.g., to strip functional tags)
   */
  public static void main(String[] args) {
    Timing.startTime();
    if (args.length < 2) {
      System.err.println("Usage: java edu.stanford.nlp.trees.tregex.TreeMatcher [-T] [-C] [-w] [-f] pattern [handle] filepath");
      System.exit(0);
    }

    String tnClass = null;
    int i = 0;
    while (i < args.length && args[i].charAt(0) == '-') {
      if (args[i].equals("-T")) {
        Verbose.printTree = true;
      } else if (args[i].equals("-C")) {
        Verbose.printMatches = false;
      } else if (args[i].equals("-w")) {
        Verbose.printWholeTree = true;
      } else if (args[i].equals("-f")) {
        Verbose.printFilename = true;
      } else if (args[i].equals("-tn") && (i + 1) < args.length) {
        tnClass = args[++i];
      } else {
        System.err.println("Unknown flag: " + args[i]);
      }
      i++;
    }
    try {
      //TreePattern p = TreePattern.compile("/^S/ > S=dt $++ '' $-- ``");
      TreePattern p = TreePattern.compile(args[i++]);
      System.err.println("Parsed pattern: " + p.pattern());

      int j = i + 1;
      if (j >= args.length) {
        j--;
      }
      if (j < args.length) {
        System.err.println("Reading trees from file(s) " + args[j]);
        TreeReaderFactory trf;
        TreeNormalizer tn = null;
        if (tnClass != null) {
          try {
            tn = (TreeNormalizer) Class.forName(tnClass).newInstance();
          } catch (Exception e) {
          }
        }
        if (tn == null) {
          trf = new TRegexTreeReaderFactory();
        } else {
          trf = new TRegexTreeReaderFactory(tn);
        }
        treebank = new DiskTreebank(trf);
        treebank.loadPath(args[j], null, true);
      } else {
        System.err.println("Using default tree");
        Tree t = Tree.valueOf("(VP (VP (VBZ Try) (NP (NP (DT this) (NN wine)) (CC and) (NP (DT these) (NNS snails)))) (PUNCT .))");
        treebank = new MemoryTreebank();
        treebank.add(t);
      }
      String label = null;
      if (j != i) {
        label = args[i];
      }
      TRegexTreeVisitor vis = new TRegexTreeVisitor(p, label);

      treebank.apply(vis);
      Timing.endTime();
      System.err.println("There were " + vis.numMatches() + " matches in total.");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }


  static class TRegexTreeVisitor implements TreeVisitor {

    TreePattern p;
    String node;
    int numMatches;

    TRegexTreeVisitor(TreePattern p, String node) {
      this.p = p;
      this.node = node;
    }

    public void visitTree(Tree t) {
      if (Verbose.printTree) {
        System.out.println("Next tree read:");
        t.pennPrint();
      }
      TreeMatcher match = p.matcher(t);
      while (match.find()) {
        numMatches++;
        if (Verbose.printFilename && TreeMatcher.treebank instanceof DiskTreebank) {
          DiskTreebank dtb = (DiskTreebank) TreeMatcher.treebank;
          System.out.print("# ");
          System.out.println(dtb.getCurrentFile());
        }
        if (Verbose.printMatches) {
          if (Verbose.printTree) {
            System.out.println("Found a full match:");
          }
          if (Verbose.printWholeTree) {
            t.pennPrint();
          } else if (node != null) {
            if (Verbose.printTree) {
              System.out.println("Here's the node you were interested in:");
            }
            match.getNode(node).pennPrint();
          } else {
            match.getMatch().pennPrint();
          }
          System.out.println();
        }
      }
    }

    public int numMatches() {
      return numMatches;
    }

  } // end class TRegexTreeVisitor


  public static class TRegexTreeReaderFactory implements TreeReaderFactory {

    private TreeNormalizer tn;

    public TRegexTreeReaderFactory() {
      this(new TreeNormalizer() {
        public String normalizeNonterminal(String str) {
          if (str == null) {
            return "";
          } else {
            return str;
          }
        }
      });
    }

    public TRegexTreeReaderFactory(TreeNormalizer tn) {
      this.tn = tn;
    }

    public TreeReader newTreeReader(Reader in) {
      return new PennTreeReader(new BufferedReader(in), new LabeledScoredTreeFactory(new StringLabelFactory()), tn);
    }

  } // end class TRegexTreeReaderFactory


  static class Verbose {
    static boolean verbose = false;
    static boolean printTree = false;
    static boolean printWholeTree = false;
    static boolean printMatches = true;
    static boolean printFilename = false;
  }

}
