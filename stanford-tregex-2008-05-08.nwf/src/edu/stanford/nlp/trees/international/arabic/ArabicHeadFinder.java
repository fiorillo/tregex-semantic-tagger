package edu.stanford.nlp.trees.international.arabic;

import java.util.HashMap;
import java.util.regex.Pattern;

import edu.stanford.nlp.trees.AbstractCollinsHeadFinder;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreebankLanguagePack;

/**
 * Find the head of an Arabic tree, using the usual kind of heuristic
 * head finding rules.
 * <p>
 * <i>Implementation notes.</i> 
 * TO DO: make sure that -PRD marked elements are always chosen as heads.
 * (Has this now been successfully done or not??)
 * <p>
 * Mona: I added the 8 new Nonterm for the merged DT with its following
 * category as a rule the DT nonterm is right headed, the 8 new nonterm DTs
 * are: DTCD, DTRB, DTRP, DTJJ, DTNN, DTNNS, DTNNP, DTNNPS.
 * This was added Dec 7th, 2004.
 *
 * @author Roger Levy, Mona Diab
 */
public class ArabicHeadFinder extends AbstractCollinsHeadFinder {

  protected TagSet tagSet;
  
  /* A work in progress. There may well be a better way to parameterize the HeadFinders via tagset. */  
  public enum TagSet {
    BIES_COLLAPSED {
      String prep()  { return "IN"; }
      String noun()  { return "NN"; } // really there should be several here.
      String det()  { return "DT"; }
      String adj()  { return "JJ"; }
      String detPlusNoun()  { return "NN"; }  // really there should be several here; major point is that the det part is ignored completely
      TreebankLanguagePack langPack()  { return new ArabicTreebankLanguagePack(); }
    },
    ORIGINAL {
      String prep()  { return "PREP"; }
      String noun()  { return "NOUN"; }
      String det()  { return "DET"; }
      String adj()  { return "ADJ"; }
      String detPlusNoun()  { return ArabicTreebankLanguagePack.detPlusNoun; } 
      TreebankLanguagePack langPack()  { return new ArabicTreebankLanguagePack(true); }
    };

    abstract String prep();
    abstract String noun();
    abstract String adj();
    abstract String det();
    abstract String detPlusNoun();
    abstract TreebankLanguagePack langPack();
    
    static TagSet tagSet(String str) {
      if(str.equals("BIES_COLLAPSED"))
        return BIES_COLLAPSED;
      else if(str.equals("ORIGINAL"))
        return ORIGINAL;
      else throw new IllegalArgumentException("Don't know anything about tagset " + str);
    }
  }
  
  public ArabicHeadFinder() {
    this(new ArabicTreebankLanguagePack());
  }

  /**
   * Construct an ArabicHeadFinder with a String parameter corresponding to the tagset in use.
   * @param tagSet Either "ORIGINAL" or "BIES_COLLAPSED"
   */
  public ArabicHeadFinder(String tagSet) {
    this(TagSet.tagSet(tagSet));
  }
  
  public ArabicHeadFinder(TagSet tagSet) {
    this(tagSet.langPack(), tagSet);
    //this(new ArabicTreebankLanguagePack(), tagSet);
  }
  
  protected ArabicHeadFinder(TreebankLanguagePack tlp) {
    this(tlp,TagSet.BIES_COLLAPSED);
  }
  
  protected ArabicHeadFinder(TreebankLanguagePack tlp, TagSet tagSet) {
    super(tlp);
    this.tagSet = tagSet;
    //System.err.println("##testing: noun tag is " + tagSet.noun());
    
    nonTerminalInfo = new HashMap<String,String[][]>();
    nonTerminalInfo.put("SUBROOT", new String[][]{{"right", "S"}});
    nonTerminalInfo.put("NX", new String[][]{{"left", "DT","DTNN","DTNNS","DTNNP", "DTNNPS", "DTJJ"}});
    nonTerminalInfo.put("ADJP", new String[][]{{"right", tagSet.adj(), "ADJP", tagSet.noun(),"NNP","NOFUNC", "NNPS", "NNS", "DTNN", "DTNNS","DTNNP","DTNNPS","DTJJ"}, {"right", "RB", "CD","DTRB","DTCD"}, {"right", "DT"}});
    nonTerminalInfo.put("ADVP", new String[][]{{"left", "WRB", "RB", "ADVP", "WHADVP","DTRB"}, {"left", "CD", "RP", tagSet.noun(), "CC", tagSet.adj(), "IN", "NP", "NNP", "NOFUNC","DTRP","DTNN","DTNNP","DTNNPS","DTNNS","DTJJ"}}); // NNP is a gerund that they called an unknown (=NNP, believe it or not...)
    nonTerminalInfo.put("CONJP", new String[][]{{"right", "IN", "RB", tagSet.noun(),"NNS","NNP", "NNPS", "DTRB", "DTNN", "DTNNS", "DTNNP", "DTNNPS"}});
    nonTerminalInfo.put("FRAG", new String[][]{{"left", tagSet.noun(), "NNPS", "NNP","NNS", "DTNN", "DTNNS", "DTNNP", "DTNNPS"}, {"left", "VBP"}});
    nonTerminalInfo.put("INTJ", new String[][]{{"left", "RP", "UH", "DTRP"}});
    nonTerminalInfo.put("NAC", new String[][]{{"left", "NP", "SBAR", "PP", "ADJP", "S", "PRT", "UCP"}, {"left", "ADVP"}}); // note: maybe CC, RB should be the heads?
    nonTerminalInfo.put("WHADVP", new String[][]{{"left", "WRB", "WP"}, {"right", "CC"}, {"left", "IN"}});
    
    nonTerminalInfo.put("UCP", new String[][]{{"left"}});
    nonTerminalInfo.put("X", new String[][]{{"left"}});
    nonTerminalInfo.put("LST", new String[][]{{"left"}});
    //Added by Mona 12/7/04 for the newly created DT nonterm cat
    nonTerminalInfo.put("DTNN", new String[][]{{"right"}});
    nonTerminalInfo.put("DTNNS", new String[][]{{"right"}});
    nonTerminalInfo.put("DTNNP", new String[][]{{"right"}});
    nonTerminalInfo.put("DTNNPS", new String[][]{{"right"}});
    nonTerminalInfo.put("DTJJ", new String[][]{{"right"}});
    nonTerminalInfo.put("DTRP", new String[][]{{"right"}});
    nonTerminalInfo.put("DTRB", new String[][]{{"right"}});
    nonTerminalInfo.put("DTCD", new String[][]{{"right"}});
    nonTerminalInfo.put("DTIN", new String[][]{{"right"}});
    nonTerminalInfo.put("PP", new String[][]{{"left", tagSet.prep(), "PP", "PRT", "X"}, {"left", "NNP", "RP", tagSet.noun()}, {"left", "NP"}}); // NN is for a mistaken "fy", and many wsT
    nonTerminalInfo.put("PRN", new String[][]{{"left", "NP"}}); // don't get PUNC
    nonTerminalInfo.put("PRT", new String[][]{{"left", "RP", "PRT", "IN", "DTRP"}});
    nonTerminalInfo.put("QP", new String[][]{{"right", "CD", tagSet.noun(), tagSet.adj(), "NNS", "NNP", "NNPS", "DTNN", "DTNNS", "DTNNP", "DTNNPS", "DTJJ"}});
    nonTerminalInfo.put("S", new String[][]{{"left", "VP", "S"}, {"right", "PP", "ADVP", "SBAR", "UCP", "ADJP"}}); // really important to put in -PRD sensitivity here!
    nonTerminalInfo.put("SQ", new String[][]{{"left", "VP", "PP"}}); // to be principled, we need -PRD sensitivity here too.
    nonTerminalInfo.put("SBAR", new String[][]{{"left", "WHNP", "WHADVP", "RP", "IN", "SBAR", "CC", "WP", "WHPP", "ADVP", "PRT", "RB", "X", "DTRB", "DTRP"}, {"left", tagSet.noun(), "NNP", "NNS", "NNPS", "DTNN", "DTNNS", "DTNNP", "DTNNPS"}, {"left", "S"}});
    nonTerminalInfo.put("SBARQ", new String[][]{{"left", "WHNP", "WHADVP", "RP", "IN", "SBAR", "CC", "WP", "WHPP", "ADVP", "PRT", "RB", "X"}, {"left", tagSet.noun(), "NNP", "NNS", "NNPS","DTNN", "DTNNS", "DTNNP", "DTNNPS"}, {"left", "S"}}); // copied from SBAR rule -- look more closely when there's time
    
    nonTerminalInfo.put("WHNP", new String[][]{{"right", "WP"}});
    nonTerminalInfo.put("WHPP", new String[][]{{"right"}});
    nonTerminalInfo.put("VP", new String[][]{{"left", "VBD", "VBN", "VBP", "VP", "RB", "X","VB"}, {"left", "IN"}, {"left", "NNP","NOFUNC", tagSet.noun(), "DTNN", "DTNNP", "DTNNPS", "DTNNS"}}); // exclude RP because we don't want negation markers as heads -- no useful information?
    //also, RB is used as gerunds
    
    nonTerminalInfo.put("NP", new String[][]{{"left", tagSet.noun(), tagSet.detPlusNoun(), "NNS", "NNP", "NNPS", "NP", "PRP", "WHNP", "QP", "WP","DTNN", "DTNNS", "DTNNPS", "DTNNP", "NOFUNC"}, {"left", tagSet.adj(), "DTJJ"}, {"right", "CD", "DTCD"}, {"left", "PRP$"}, {"right", "DT"}}); // should the JJ rule be left or right?
    
    // stand-in dependency:
    nonTerminalInfo.put("EDITED", new String[][]{{"left"}});
    nonTerminalInfo.put("ROOT", new String[][]{{"left"}});
    
    // one stray SINV in the training set...garbage head rule here.
    nonTerminalInfo.put("SINV", new String[][]{{"left","ADJP","VP"}});
  }


  private Pattern predPattern = Pattern.compile(".*-PRD$");

  /**
   * Predicatively marked elements in a sentence should be noted as heads
   */
  protected Tree findMarkedHead(Tree t) {
    String cat = t.value();
    if (cat.equals("S")) {
      Tree[] kids = t.children();
      for (Tree kid : kids) {
        if (predPattern.matcher(kid.value()).matches()) {
          return kid;
        }
      }
    }
    return null;
  }
  
} // end class ArabicHeadFinder
