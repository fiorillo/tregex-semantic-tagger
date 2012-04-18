package edu.stanford.nlp.trees.international.arabic;

import edu.stanford.nlp.trees.BobChrisTreeNormalizer;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeFactory;
import edu.stanford.nlp.trees.tregex.TregexPattern;
import edu.stanford.nlp.trees.tregex.ParseException;
import edu.stanford.nlp.trees.tregex.TregexMatcher;

import java.util.*;
import java.util.regex.Pattern;

/**
 * A first-version tree normalizer for the Arabic Penn Treebank.  
 * Just like BobChrisTreeNormalizer but:
 * <ul>
 * <li> Adds a ROOT node to the top of every tree
 * <li> Strips all the interesting stuff off of the POS tags.
 * <li> Can keep NP-TMP annotations (retainNPTmp parameter)
 * <li> Can keep whatever annotations there are on verbs that are sisters
 *           to predicatively marked (-PRD) elements (markPRDverb parameter)
 *           [Chris Nov 2006: I'm a bit unsure on that one!]
 * <li> Can keep categories unchanged, i.e., not mapped to basic categories
 *           (changeNoLabels parameter)
 * </ul>
 *
 * @author Roger Levy
 */
public class ArabicTreeNormalizer extends BobChrisTreeNormalizer {

  private boolean retainNPTmp;
  private boolean markPRDverb;
  private boolean normalizeConj = false;
  private boolean changeNoLabels = false;
  private Pattern prdPattern = Pattern.compile("^[A-Z]+-PRD");
  private TregexPattern prdVerbPattern;

  public ArabicTreeNormalizer(boolean retainNPTmp, boolean markPRDverb, boolean changeNoLabels) {
    super(new ArabicTreebankLanguagePack());
    this.retainNPTmp = retainNPTmp;
    this.markPRDverb = markPRDverb;
    this.changeNoLabels = changeNoLabels;
    try {
      prdVerbPattern  = TregexPattern.compile("/^V[^P]/ > VP $ /-PRD$/=prd");
    } catch(ParseException e) {
      System.out.println(e);
      throw new RuntimeException();
    }
  }

  public ArabicTreeNormalizer(boolean retainNPTmp, boolean markPRDverb) {
    this(retainNPTmp,markPRDverb,false);
  }

  public ArabicTreeNormalizer(boolean retainNPTmp) {
    this(retainNPTmp,false);
  }

  public ArabicTreeNormalizer() {
    this(false);
  }


  public String normalizeNonterminal(String category) {
    if (changeNoLabels) {
      return category;
    } else if (retainNPTmp && category != null && category.startsWith("NP-TMP")) {
      return "NP-TMP";
    } else if  (markPRDverb && category != null && prdPattern.matcher(category).matches()) {
      return category;
    } else {
      return super.normalizeNonterminal(category);
    }
  }

  /** Miscellany:
   * <ul>
   * <li> Escapes out "/" and "*" tokens (this is ugly, should be fixed!)
   * </ul>
   */
  public String normalizeTerminal(String leaf) {
    if(changeNoLabels)
      return leaf;
    if(escape && escapeCharacters.contains(leaf))
      return "\\" + leaf;
    return super.normalizeTerminal(leaf);
  }

  private static final boolean escape = false;
  private static final Collection escapeCharacters = Arrays.asList(
          new String[] { "/","*" }
    );

  public Tree normalizeWholeTree(Tree tree, TreeFactory tf) {
    tree = super.normalizeWholeTree(tree, tf);

    // there are some nodes "/" missing preterminals.  We'll splice in a tag for these.
    for (Tree t : tree) {
      if (t.isPreTerminal()) {
        // CDM Nov 2006: It's not clear to me that this is actually an error
        // since ElY can change to Ely with pronoun objects (Ryding p.29)
        // but it seems a reasonable normalization to do
        if ((t.value().equals("PREP") || t.value().equals("IN")) && 
            t.firstChild().value().equals("Ely")) {
          System.err.println("ATBNormalizer FIX: correcting Ely to ElY: " + t);
          t.children()[0].label().setValue("ElY");  // preposition meaning "on" should consistently be ElY
        } else if ((t.value().equals("PREP") || t.value().equals("IN")) && 
                   t.firstChild().value().equals("<ly")) {
          System.err.println("ATBNormalizer FIX: correcting <ly to <lY: " + t);
          t.children()[0].label().setValue("<lY");  
        } else if ((t.value().equals("PREP") || t.value().equals("IN")) && 
                   t.firstChild().value().equals("Aly")) {
          System.err.println("ATBNormalizer FIX: correcting Aly to AlY: " + t);
          t.children()[0].label().setValue("AlY");  
        } else if ((t.value().equals("PREP") || t.value().equals("IN")) && 
                   t.firstChild().value().equals("ldy")) {
          System.err.println("ATBNormalizer FIX: correcting ldy to ldY: " + t);
          t.children()[0].label().setValue("ldY");  
        } else if (t.label().value() == null || t.label().value().equals("")) {
          System.err.println("ATBNormalizer ERROR: missing tag: " + t);
        }
      }
      if (t.isPreTerminal() || t.isLeaf()) {
        continue;
      }
      int nk = t.numChildren();
      List<Tree> newKids = new ArrayList<Tree>(nk);
      for (int j = 0; j < nk; j++) {
        Tree child = t.getChild(j);
        if (child.isLeaf()) {
          newKids.add(tf.newTreeNode("DUMMYTAG", Collections.singletonList(child)));
        } else {
          newKids.add(child);
        }
      }
      t.setChildren(newKids);
    }
    // special global coding for moving PRD annotation from constituent to verb tag.
    if (markPRDverb) {
      TregexMatcher m = prdVerbPattern.matcher(tree);
      Tree match = null;
      while (m.find()) {
        if (m.getMatch()==match)
          continue;
        else {
          match = m.getMatch();
          match.label().setValue(match.label().value() + "-PRDverb");
          Tree prd = m.getNode("prd");
          prd.label().setValue(super.normalizeNonterminal(prd.label().value()));
        }
      }
    }
    if (normalizeConj && tree.isPreTerminal() && tree.children()[0].label().value().equals("w") && wrongConjPattern.matcher(tree.label().value()).matches()) {
      System.err.print("ATBNormalizer ERROR: bad CC remapped tree " + tree + " to ");
      tree.label().setValue("CC");
      System.err.println(tree);
    }
    if (tree.isPreTerminal()) {
      String val = tree.label().value();
      if (val.equals("CC") || val.equals("PUNC") || val.equals("CONJ")) {
        System.err.println("ATBNormalizer ERROR: bare tagged word: " + tree + 
                           " being wrapped in FRAG");
        tree = tf.newTreeNode("FRAG", Collections.singletonList(tree));
      } else {
        System.err.println("ATBNormalizer ERROR: bare tagged word: " + tree +
                           ": fix it!!");
      }
    }
    if (! tree.label().value().equals("ROOT")) {
      tree = tf.newTreeNode("ROOT", Collections.singletonList(tree));
    } 
    return tree;
  }

  private static final Pattern wrongConjPattern = Pattern.compile("NNP|NO_FUNC|NOFUNC|IN");

}
