package edu.stanford.nlp.trees;

/**
 * Implements a variant on the HeadFinder found in Michael Collins' 1999
 * thesis. This starts with
 * Collins' head finder. As in <code>CollinsHeadFinder.java</code>, we've
 * added a head rule for NX.
 *
 * Changes:
 * <ol>
 * <li>The PRN rule used to just take the leftmost thing, we now have it
 * choose the leftmost lexical category (not the common punctuation etc.)
 * <li>Delete IN as a possible head of S, and add FRAG (low priority)
 * <li>Place NN before QP in ADJP head rules (more to do for ADJP!)
 * <li>Place PDT before RB and after CD in QP rules.  Also prefer CD to
 * DT or RB.  And DT to RB.
 * <li>Add DT, WDT as low priority choice for head of NP. Add PRP before PRN
 * Add RBR as low priority choice of head for NP.
 * <li>Prefer NP or NX as head of NX, and otherwise default to rightmost not
 * leftmost (NP-like headedness)
 * <li>VP: add JJ and NNP as low priority heads (many tagging errors)
 *   Place JJ above NP in priority, as it is to be preferred to NP object.
 * <li>PP: add PP as a possible head (rare conjunctions)
 * <li>Added rule for POSSP (can be introduced by parser)
 * <li>Added a sensible-ish rule for X.
 * </ol>
 * These rules are suitable for the Penn Treebank.
 * <p/>
 * A case that you apparently just can't handle well in this framework is
 * (NP (NP ... NP)).  If this is a conjunction, apposition or similar, then
 * the leftmost NP is the head, but if the first is a measure phrase like
 * (NP $ 38) (NP a share) then the second should probably be the head.
 *
 * @author Christopher Manning
 */
public class ModCollinsHeadFinder extends CollinsHeadFinder {

  public ModCollinsHeadFinder() {
    this(new PennTreebankLanguagePack());
  }

  public ModCollinsHeadFinder(TreebankLanguagePack tlp) {
    super(tlp);

    // just throw away the nonTerminalInfo created by the superclass
    // ModCollins extends Collins just so it can use the same postOperationFix.
    nonTerminalInfo.clear();

    // This version from Collins' diss (1999: 236-238)
    // NNS, NN is actually sensible (money, etc.)!
    // QP early isn't; should prefer JJR NN RB
    // remove ADVP; it just shouldn't be there.
    // NOT DONE: if two JJ, should take right one (e.g. South Korean)

    nonTerminalInfo.put("ADJP", new String[][]{{"left", "NNS", "NN", "$", "QP", "JJ", "VBN", "VBG", "ADJP", "JJR", "NP", "JJS", "DT", "FW", "RBR", "RBS", "SBAR", "RB"}});
    nonTerminalInfo.put("ADVP", new String[][]{{"right", "RB", "RBR", "RBS", "FW", "ADVP", "TO", "CD", "JJR", "JJ", "IN", "NP", "JJS", "NN"}});
    nonTerminalInfo.put("CONJP", new String[][]{{"right", "CC", "RB", "IN"}});
    nonTerminalInfo.put("FRAG", new String[][]{{"right"}}); // crap
    nonTerminalInfo.put("INTJ", new String[][]{{"left"}});
    nonTerminalInfo.put("LST", new String[][]{{"right", "LS", ":"}});
    nonTerminalInfo.put("NAC", new String[][]{{"left", "NN", "NNS", "NNP", "NNPS", "NP", "NAC", "EX", "$", "CD", "QP", "PRP", "VBG", "JJ", "JJS", "JJR", "ADJP", "FW"}});
    nonTerminalInfo.put("NX", new String[][]{{"right", "NP", "NX"}});
    nonTerminalInfo.put("PP", new String[][]{{"right", "IN", "TO", "VBG", "VBN", "RP", "FW"}, {"right", "PP"}});

    // should prefer JJ? (PP (JJ such) (IN as) (NP (NN crocidolite)))

    nonTerminalInfo.put("PRN", new String[][]{{"left", "VP", "NP", "PP", "S", "SINV", "SBAR", "ADJP", "ADVP", "INTJ", "WHNP", "NAC", "VBP", "JJ", "NN", "NNP"}});
    nonTerminalInfo.put("PRT", new String[][]{{"right", "RP"}});
    // add '#' for pounds!!
    nonTerminalInfo.put("QP", new String[][]{{"left", "$", "IN", "NNS", "NN", "JJ", "CD", "PDT", "DT", "RB", "NCD", "QP", "JJR", "JJS"}});
    nonTerminalInfo.put("RRC", new String[][]{{"right", "VP", "NP", "ADVP", "ADJP", "PP"}});

    // delete IN -- go for main part of sentence; add FRAG

    nonTerminalInfo.put("S", new String[][]{{"left", "TO", "VP", "S", "FRAG", "SBAR", "ADJP", "UCP", "NP"}});
    nonTerminalInfo.put("SBAR", new String[][]{{"left", "WHNP", "WHPP", "WHADVP", "WHADJP", "IN", "DT", "S", "SQ", "SINV", "SBAR", "FRAG"}});
    nonTerminalInfo.put("SBARQ", new String[][]{{"left", "SQ", "S", "SINV", "SBARQ", "FRAG"}});
    nonTerminalInfo.put("SINV", new String[][]{{"left", "VBZ", "VBD", "VBP", "VB", "MD", "VP", "S", "SINV", "ADJP", "NP"}});
    nonTerminalInfo.put("SQ", new String[][]{{"left", "VBZ", "VBD", "VBP", "VB", "MD", "VP", "SQ"}});
    nonTerminalInfo.put("UCP", new String[][]{{"right"}});
    // below is weird!! Make 2 lists, one for good and one for bad heads??
    nonTerminalInfo.put("VP", new String[][]{{"left", "TO", "VBD", "VBN", "MD", "VBZ", "VB", "VBG", "VBP", "VP", "ADJP", "NN", "NNS", "JJ", "NP", "NNP"}});
    nonTerminalInfo.put("WHADJP", new String[][]{{"left", "CC", "WRB", "JJ", "ADJP"}});
    nonTerminalInfo.put("WHADVP", new String[][]{{"right", "CC", "WRB"}});
    nonTerminalInfo.put("WHNP", new String[][]{{"left", "WDT", "WP", "WP$", "WHADJP", "WHPP", "WHNP"}});
    nonTerminalInfo.put("WHPP", new String[][]{{"right", "IN", "TO", "FW"}});
    nonTerminalInfo.put("X", new String[][]{{"right", "S", "VP", "ADJP", "NP", "SBAR", "PP", "X"}});
    nonTerminalInfo.put("NP", new String[][]{{"rightdis", "NN", "NNP", "NNPS", "NNS", "NX", "POS", "JJR"}, {"left", "NP", "PRP"}, {"rightdis", "$", "ADJP", "PRN"}, {"right", "CD"}, {"rightdis", "JJ", "JJS", "RB", "QP", "DT", "WDT", "RBR", "ADVP"}});
    nonTerminalInfo.put("POSSP", new String[][]{{"right", "POS"}});

	/* HJT: Adding the following to deal with oddly formed data in
	 (for example) the Brown corpus */
    nonTerminalInfo.put("ROOT", new String[][]{{"left", "S", "SQ", "SINV", "SBAR", "FRAG"}});
    nonTerminalInfo.put("TYPO", new String[][]{{"left", "NN", "NP", "NNP", "NNPS", "TO",
      "VBD", "VBN", "MD", "VBZ", "VB", "VBG", "VBP", "VP", "ADJP", "FRAG"}}); // for Brown (Roger)
    nonTerminalInfo.put("ADV", new String[][]{{"right", "RB", "RBR", "RBS", "FW",
      "ADVP", "TO", "CD", "JJR", "JJ", "IN", "NP", "JJS", "NN"}});
    nonTerminalInfo.put("EDITED", new String[][] {{"left"}});  // crap rule for Switchboard (if don't delete EDITED nodes)
    nonTerminalInfo.put("XS", new String[][] {{"right", "IN"}}); // rule for new structure in QP

  }

  private static final long serialVersionUID = -5870387458902637256L;

}
