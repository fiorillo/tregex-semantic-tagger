package edu.stanford.nlp.trees;

import edu.stanford.nlp.util.Function;
import edu.stanford.nlp.util.MutableInteger;
import edu.stanford.nlp.util.StringUtils;
import edu.stanford.nlp.ling.*;
import edu.stanford.nlp.ling.CoreAnnotations.TagLabelAnnotation;

import java.util.*;
import java.io.*;


/**
 * Various utilities for the <code>Tree</code> class.
 *
 * @author Roger Levy
 * @author Dan Klein
 */
public class Trees {

  /**
   * Returns the positional index of the left edge of a tree <i>t</i>
   * within a given root, as defined by the size of the yield of all
   * material preceding <i>t</i>.
   */
  public static int leftEdge(Tree t, Tree root) {
    MutableInteger i = new MutableInteger(0);
    if (leftEdge(t, root, i)) {
      return i.intValue();
    } else {
      return -1;
    }
  }

  static boolean leftEdge(Tree t, Tree t1, MutableInteger i) {
    if (t == t1) {
      return true;
    } else if (t1.isLeaf()) {
      int j = t1.yield().size(); // so that empties don't add size
      i.set(i.intValue() + j);
      return false;
    } else {
      Tree[] kids = t1.children();
      for (int j = 0, n = kids.length; j < n; j++) {
        if (leftEdge(t, kids[j], i)) {
          return true;
        }
      }
      return false;
    }
  }

  /**
   * Returns the positional index of the right edge of a tree
   * <i>t</i> within a given root, as defined by the size of the yield
   * of all material preceding <i>t</i> plus all the material
   * contained in <i>t</i>.
   */
  public static int rightEdge(Tree t, Tree root) {
    MutableInteger i = new MutableInteger(root.yield().size());
    if (rightEdge(t, root, i)) {
      return i.intValue();
    } else {
      return root.yield().size() + 1;
    }
  }

  static boolean rightEdge(Tree t, Tree t1, MutableInteger i) {
    if (t == t1) {
      return true;
    } else if (t1.isLeaf()) {
      int j = t1.yield().size(); // so that empties don't add size
      i.set(i.intValue() - j);
      return false;
    } else {
      Tree[] kids = t1.children();
      for (int j = kids.length - 1; j >= 0; j--) {
        if (rightEdge(t, kids[j], i)) {
          return true;
        }
      }
      return false;
    }
  }


  /**
   * Returns a lexicalized Tree whose Labels are CategoryWordTag
   * instances, all corresponds to the input tree.
   */
  public static Tree lexicalize(Tree t, HeadFinder hf) {
    Function<Tree,Tree> a = TreeFunctions.getLabeledTreeToCategoryWordTagTreeFunction();
    Tree t1 = a.apply(t);
    t1.percolateHeads(hf);
    return t1;
  }

  /**
   * returns the leaves in a Tree in the order that they're found.
   */
  public static List<Tree> leaves(Tree t) {
    List<Tree> l = new ArrayList<Tree>();
    leaves(t, l);
    return l;
  }

  private static void leaves(Tree t, List<Tree> l) {
    if (t.isLeaf()) {
      l.add(t);
    } else {
      Tree[] kids = t.children();
      for (int j = 0, n = kids.length; j < n; j++) {
        leaves(kids[j], l);
      }
    }
  }

  /**
   * returns the labels of the leaves in a Tree in the order that they're found.
   */
  public static List<Label> leafLabels(Tree t) {
    List<Label> l = new ArrayList<Label>();
    leafLabels(t, l);
    return l;
  }

  private static void leafLabels(Tree t, List<Label> l) {
    if (t.isLeaf()) {
      l.add(t.label());
    } else {
      Tree[] kids = t.children();
      for (int j = 0, n = kids.length; j < n; j++) {
        leafLabels(kids[j], l);
      }
    }
  }

  /**
   * returns the labels of the leaves in a Tree, augmented with POS tags.  assumes that
   * the labels are CoreLabels.
   */
  public static List<CoreLabel> taggedLeafLabels(Tree t) {
    List<CoreLabel> l = new ArrayList<CoreLabel>();
    taggedLeafLabels(t, l);
    return l;
  }

  private static void taggedLeafLabels(Tree t, List<CoreLabel> l) {
    if (t.isPreTerminal()) {
      CoreLabel fl = (CoreLabel)t.getChild(0).label();
      fl.set(TagLabelAnnotation.class, t.label());
      l.add(fl);
    } else {
      Tree[] kids = t.children();
      for (int j = 0, n = kids.length; j < n; j++) {
        taggedLeafLabels(kids[j], l);
      }
    }
  }
  
  /**
   * returns true iff <code>head</code> (transitively) heads <code>node</code>
   */
  public static boolean heads(Tree head, Tree node, HeadFinder hf) {
    if (node.isLeaf()) {
      return false;
    } else {
      return heads(head, hf.determineHead(node), hf);
    }
  }


  /**
   * returns the maximal projection of <code>head</code> in
   * <code>root</code> given a {@link HeadFinder}
   */
  public static Tree maximalProjection(Tree head, Tree root, HeadFinder hf) {
    Tree projection = head;
    if (projection == root) {
      return root;
    }
    Tree parent = projection.parent(root);
    while (hf.determineHead(parent) == projection) {
      projection = parent;
      if (projection == root) {
        return root;
      }
      parent = projection.parent(root);
    }
    return projection;
  }

  /* applies a TreeVisitor to all projections (including the node itself) of a node in a Tree.
  *  Does nothing if head is not in root.
  * @return the maximal projection of head in root.
  */
  public static Tree applyToProjections(TreeVisitor v, Tree head, Tree root, HeadFinder hf) {
    Tree projection = head;
    Tree parent = projection.parent(root);
    if (parent == null && projection != root) {
      return null;
    }
    v.visitTree(projection);
    if (projection == root) {
      return root;
    }
    while (hf.determineHead(parent) == projection) {
      projection = parent;
      v.visitTree(projection);
      if (projection == root) {
        return root;
      }
      parent = projection.parent(root);
    }
    return projection;
  }

  /**
   * gets the <code>n</code>th terminal in <code>tree</code>.  The first terminal is number zero.
   */
  public static Tree getTerminal(Tree tree, int n) {
    return getTerminal(tree, new MutableInteger(0), n);
  }

  static Tree getTerminal(Tree tree, MutableInteger i, int n) {
    if (i.intValue() == n) {
      if (tree.isLeaf()) {
        return tree;
      } else {
        return getTerminal(tree.children()[0], i, n);
      }
    } else {
      if (tree.isLeaf()) {
        i.set(i.intValue() + tree.yield().size());
        return null;
      } else {
        Tree[] kids = tree.children();
        for (int j = 0; j < kids.length; j++) {
          Tree result = getTerminal(kids[j], i, n);
          if (result != null) {
            return result;
          }
        }
        return null;
      }
    }
  }

  /**
   * gets the <code>n</code>th preterminal in <code>tree</code>.  The first terminal is number zero.
   */
  public static Tree getPreTerminal(Tree tree, int n) {
    return getPreTerminal(tree, new MutableInteger(0), n);
  }

  static Tree getPreTerminal(Tree tree, MutableInteger i, int n) {
    if (i.intValue() == n) {
      if (tree.isPreTerminal()) {
        return tree;
      } else {
        return getPreTerminal(tree.children()[0], i, n);
      }
    } else {
      if (tree.isPreTerminal()) {
        i.set(i.intValue() + tree.yield().size());
        return null;
      } else {
        Tree[] kids = tree.children();
        for (int j = 0; j < kids.length; j++) {
          Tree result = getPreTerminal(kids[j], i, n);
          if (result != null) {
            return result;
          }
        }
        return null;
      }
    }
  }

  /**
   * returns the syntactic category of the tree as a list of the syntactic categories of the mother and the daughters
   */
  public static List<String> localTreeAsCatList(Tree t) {
    List<String> l = new ArrayList<String>(t.children().length + 1);
    l.add(t.label().value());
    for (int i = 0; i < t.children().length; i++) {
      l.add(t.children()[i].label().value());
    }
    return l;
  }

  /**
   * Returns the index of <code>daughter</code> in <code>parent</code> by ==.
   * Returns -1 if <code>daughter</code> not found.
   */
  public static int objectEqualityIndexOf(Tree parent, Tree daughter) {
    for (int i = 0; i < parent.children().length; i++) {
      if (daughter == parent.children()[i]) {
        return i;
      }
    }
    return -1;
  }

  /** Return information about the objects in this Tree.
   *  @param t The tree to examine.
   *  @return A human-readable String
   */
  public static String toDebugStructureString(Tree t) {
    StringBuilder sb = new StringBuilder();
    String tCl = StringUtils.getShortClassName(t);
    String tfCl = StringUtils.getShortClassName(t.treeFactory());
    String lCl = StringUtils.getShortClassName(t.label());
    String lfCl = StringUtils.getShortClassName(t.label().labelFactory());
    Set<String> otherClasses = new HashSet<String>();
    for (Tree st : t) {
      String stCl = StringUtils.getShortClassName(st);
      String stfCl = StringUtils.getShortClassName(st.treeFactory());
      String slCl = StringUtils.getShortClassName(st.label());
      String slfCl = StringUtils.getShortClassName(st.label().labelFactory());

      if ( ! tCl.equals(stCl)) {
        otherClasses.add(stCl);
      }
      if ( ! tfCl.equals(stfCl)) {
        otherClasses.add(stfCl);
      }
      if ( ! lCl.equals(slCl)) {
        otherClasses.add(slCl);
      }
      if ( ! lfCl.equals(slfCl)) {
        otherClasses.add(slfCl);
      }
    }
    sb.append("Tree with root of class ").append(tCl).append(" and factory ").append(tfCl);
    sb.append(" with label class ").append(lCl).append(" and factory ").append(lfCl);
    if ( ! otherClasses.isEmpty()) {
      sb.append(" with the following classes also found within the tree: ").append(otherClasses);
    }
    return sb.toString();
  }


  /** Turns a sentence into a flat phrasal tree.
   *  The structure is S -> tag*.  And then each tag goes to a word.
   *  The tag is either found from the label or made "WD".
   *  The tag and phrasal node have a StringLabel.
   *
   *  @param s The Sentence to make the Tree from
   *  @return The one phrasal level Tree
   */
  public static Tree toFlatTree(Sentence<?> s) {
    return toFlatTree(s, new StringLabelFactory());
  }

  /** Turns a sentence into a flat phrasal tree.
   *  The structure is S -> tag*.  And then each tag goes to a word.
   *  The tag is either found from the label or made "WD".
   *  The tag and phrasal node have a StringLabel.
   *
   *  @param s The Sentence to make the Tree from
   *  @param lf The LabelFactory with which to create the new Tree labels
   *  @return The one phrasal level Tree
   */
  public static Tree toFlatTree(Sentence<?> s, LabelFactory lf) {
    List<Tree> daughters = new ArrayList<Tree>(s.length());
    for (HasWord word : s) {
      Tree wordNode = new LabeledScoredTreeLeaf(lf.newLabel(word.word()));
      if (word instanceof TaggedWord) {
        TaggedWord taggedWord = (TaggedWord) word;
        wordNode = new LabeledScoredTreeNode(new StringLabel(taggedWord.tag()), Collections.singletonList(wordNode));
      } else {
        wordNode = new LabeledScoredTreeNode(lf.newLabel("WD"), Collections.singletonList(wordNode));
      }
      daughters.add(wordNode);
    }
    return new LabeledScoredTreeNode(new StringLabel("S"), daughters);
  }


   public static String treeToLatex(Tree t) {
     StringBuilder connections = new StringBuilder();
     StringBuilder hierarchy = new StringBuilder();
     treeToLatexHelper(t,connections,hierarchy,0,1,0);
     return "\\tree"+hierarchy+"\n"+connections+"\n";
   }

   private static int treeToLatexHelper(Tree t, StringBuilder c, StringBuilder h, int 
n, int
nextN, int indent) {
     StringBuilder sb = new StringBuilder();
     for (int i=0; i<indent; i++)
       sb.append("  ");
     h.append("\n"+sb);
     h.append("{\\"+(t.isLeaf() ? "" : "n")+"tnode{z"+n+"}{"+t.label()+"}");
     if (!t.isLeaf()) {
       for (int k=0; k<t.children().length; k++) {
         h.append(", ");
         c.append("\\nodeconnect{z"+n+"}{z"+nextN+"}\n");
         nextN = treeToLatexHelper(t.children()[k],c,h,nextN,nextN+1,indent+1);
       }
     }
     h.append("}");
     return nextN;
   }

  public static String treeToLatexEven(Tree t) {
     StringBuilder connections = new StringBuilder();
     StringBuilder hierarchy = new StringBuilder();
     int maxDepth = t.depth();
     treeToLatexEvenHelper(t,connections,hierarchy,0,1,0,0,maxDepth);
     return "\\tree"+hierarchy+"\n"+connections+"\n";
   }

  private static int treeToLatexEvenHelper(Tree t, StringBuilder c, StringBuilder h, 
int n,
int nextN, int indent, int curDepth, int maxDepth) {
     StringBuilder sb = new StringBuilder();
     for (int i=0; i<indent; i++)
       sb.append("  ");
     h.append("\n"+sb);
     int tDepth = t.depth();
     if (tDepth == 0 && tDepth+curDepth < maxDepth) {
       for (int pad=0; pad < maxDepth-tDepth-curDepth; pad++) {
         h.append("{\\ntnode{pad}{}, ");
       }
     }
     h.append("{\\ntnode{z"+n+"}{"+t.label()+"}");
     if (!t.isLeaf()) {
       for (int k=0; k<t.children().length; k++) {
         h.append(", ");
         c.append("\\nodeconnect{z"+n+"}{z"+nextN+"}\n");
         nextN = 
treeToLatexEvenHelper(t.children()[k],c,h,nextN,nextN+1,indent+1,curDepth+1,maxDepth);
       }
     }
     if (tDepth == 0 && tDepth+curDepth < maxDepth) {
       for (int pad=0; pad < maxDepth-tDepth-curDepth; pad++) {
         h.append("}");
       }
     }
     h.append("}");
     return nextN;
   }

   static String texTree(Tree t) {
     return treeToLatex(t);
   }

   static Tree readTree(String s) throws IOException {
     return (new PennTreeReader(new StringReader(s), new 
LabeledScoredTreeFactory(new StringLabelFactory()))).readTree();
   }

   static String escape(String s) {
     StringBuilder sb = new StringBuilder();
     for (int i=0; i<s.length(); i++) {
       char c = s.charAt(i);
       if (c == '^')
         sb.append('\\');
       sb.append(c);
       if (c == '^')
         sb.append("{}");
     }
     return sb.toString();
   }


   public static void main(String[] args) throws IOException {
     int i = 0;
     while (i < args.length) {
       Tree tree = readTree(args[i]);
       System.out.println(escape(texTree(tree)));
       i++;
     }
     if (i == 0) {
       Tree tree = (new PennTreeReader(new BufferedReader(new 
InputStreamReader(System.in)), new LabeledScoredTreeFactory(new 
StringLabelFactory()))).readTree();
       System.out.println(escape(texTree(tree)));
     }
   }

  public static Tree normalizeTree(Tree tree, TreeNormalizer tn, TreeFactory tf) {
    for (Tree node : tree) {
      if (node.isLeaf()) {
        node.label().setValue(tn.normalizeTerminal(node.label().value()));
      } else {
        node.label().setValue(tn.normalizeNonterminal(node.label().value()));
      }
    }
    return tn.normalizeWholeTree(tree, tf);
  }
}
