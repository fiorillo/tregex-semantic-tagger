package edu.stanford.nlp.trees.international.arabic;

import java.util.regex.Pattern;

import edu.stanford.nlp.objectbank.TokenizerFactory;
import edu.stanford.nlp.trees.AbstractTreebankLanguagePack;
import edu.stanford.nlp.trees.PennTreebankLanguagePack;
import edu.stanford.nlp.trees.TreebankLanguagePack;
import edu.stanford.nlp.ling.HasWord;


/**
 * Specifies the treebank/language specific components needed for
 * parsing the English Penn Treebank.
 * <p/>
 * The encoding for Arabic is the default UTF-8 specified in
 * AbstractTreebankLanguagePack.
 *
 * @author Christopher Manning
 * @author Mona Diab
 * @author Roger Levy
 * @version 1.1
 */
public class ArabicTreebankLanguagePack extends AbstractTreebankLanguagePack {

  /**
   * Initialize an Arabic Treebank.
   */
  public ArabicTreebankLanguagePack() {
    this(false);
  }

  /** Initialize an Arabic Treebank.
   *
   *  @param detPlusNounIsBasicCategory If invoked with argument <code>true</code>,
   *     the category <code>DET+NOUN</code> is considered a basic category
   *     for purposes of {@link #basicCategory(String)}.  (Note: this is maybe
   *     obsolete. In more recent practice we've used tags like DTNN.)
   */
  public ArabicTreebankLanguagePack(boolean detPlusNounIsBasicCategory) {
   this.detPlusNounIsBasicCategory = detPlusNounIsBasicCategory;
  }

  private boolean detPlusNounIsBasicCategory;


  private static String[] pennPunctTags = {"PUNC",","}; // "," is used in Bikel's parser.

  private static String[] pennSFPunctTags = {"."};

  private static String[] collinsPunctTags = {"PUNC"};

  private static String[] pennPunctWords = {"''", "'", "``", "`", "-LRB-", "-RRB-", "-LCB-", "-RCB-", ".", "?", "!", ",", ":", "-", "--", "...", ";", "%", "&", "\"", "\"__", "%", "&", "*", "+", ",", "-", "-RRB-_", "-RRB-__", "-_", "-__", ".", "..", "......", "/", "05,", "115,", "1985,", "1998,", "30,", "910,192,2", "910,492,2", ":", ":_", ":__", ";", "?\"", "?\".", "?", "?.", "En", "A", "="};

  private static String[] pennSFPunctWords = {".", "!", "?", "?\"", "?\".", "?", "?.", };


  /**
   * The first 3 are used by the Penn Treebank; # is used by the
   * BLLIP corpus, and ^ and ~ are used by Klein's lexparser.
   * Chris deleted '_' for Arabic as it appears in tags (NO_FUNC).
   * June 2006: CDM tested _ again with true (new) Treebank tags to see if it
   * was useful for densening up the tag space, but the results were negative.
   * Roger added + for Arabic (?)
   */
  private static char[] annotationIntroducingChars = {'-', '=', '|', '#', '^', '~', '+','_'};

  /**
   * This is valid for "BobChrisTreeNormalizer" conventions only.
   */
  private static String[] pennStartSymbols = {"ROOT"};


  /**
   * Returns a String array of punctuation tags for this treebank/language.
   *
   * @return The punctuation tags
   */
  public String[] punctuationTags() {
    return pennPunctTags;
  }


  /**
   * Returns a String array of punctuation words for this treebank/language.
   *
   * @return The punctuation words
   */
  public String[] punctuationWords() {
    return pennPunctWords;
  }


  /**
   * Returns a String array of sentence final punctuation tags for this
   * treebank/language.
   *
   * @return The sentence final punctuation tags
   */
  public String[] sentenceFinalPunctuationTags() {
    return pennSFPunctTags;
  }

  /**
   * Returns a String array of sentence final punctuation words for this
   * treebank/language.
   *
   * @return The sentence final punctuation tags
   */
  public String[] sentenceFinalPunctuationWords() {
    return pennSFPunctWords;
  }

  /**
   * Returns a String array of punctuation tags that EVALB-style evaluation
   * should ignore for this treebank/language.
   * Traditionally, EVALB has ignored a subset of the total set of
   * punctuation tags in the English Penn Treebank (quotes and
   * period, comma, colon, etc., but not brackets)
   *
   * @return Whether this is a EVALB-ignored punctuation tag
   */
  public String[] evalBIgnoredPunctuationTags() {
    return collinsPunctTags;
  }


  /**
   * Return an array of characters at which a String should be
   * truncated to give the basic syntactic category of a label.
   * The idea here is that Penn treebank style labels follow a syntactic
   * category with various functional and crossreferencing information
   * introduced by special characters (such as "NP-SBJ=1").  This would
   * be truncated to "NP" by the array containing '-' and "=".
   *
   * @return An array of characters that set off label name suffixes
   */
  public char[] labelAnnotationIntroducingCharacters() {
    return annotationIntroducingChars;
  }


  /**
   * Returns a String array of treebank start symbols.
   *
   * @return The start symbols
   */
  public String[] startSymbols() {
    return pennStartSymbols;
  }

  /**
   * NB: Apr 2008 - this probably shouldn't be static, but we want to avoid it getting
   * written over when the serialized parser is loaded.
   * TODO: change to be non-static and work out how the serialization works (Anna)
   */
  private static transient TokenizerFactory<? extends HasWord>  tf;

  public void setTokenizerFactory(TokenizerFactory<? extends HasWord>  tf) {
    this.tf = tf;
  }



  /**
   * Return a tokenizer which might be suitable for tokenizing text that
   * will be used with this Treebank/Language pair.  We assume at the moment that
   * someone else has tokenized our Arabic, and so use the Whitespace tokenizer
   * of superclass.
   *
   * @return A tokenizer
   */
  public TokenizerFactory<? extends HasWord> getTokenizerFactory() {
    if (tf == null) {
      tf = ArabicTokenizer.factory();
    }
    return tf;
  }

  /**
   * Returns the extension of treebank files for this treebank.
   * This is "tree".
   */
  public String treebankFileExtension() {
    return "tree";
  }

  public static void main(String[] args) {
    TreebankLanguagePack tlp = new PennTreebankLanguagePack();
    System.out.println("Start symbol: " + tlp.startSymbol());
    String start = tlp.startSymbol();
    System.out.println("Should be true: " + (tlp.isStartSymbol(start)));
    String[] strs = new String[]{"-", "-LLB-", "NP-2", "NP=3", "NP-LGS", "NP-TMP=3"};
    for (String str : strs) {
      System.out.println("String: " + str + " basic: " + tlp.basicCategory(str) + " basicAndFunc: " + tlp.categoryAndFunction(str));
    }
  }

  private static final Pattern detPlusNounPattern = Pattern.compile("^DET\\+NOUN");
  static final String detPlusNoun = "DET+NOUN";

  @Override
  public String basicCategory(String category) {
    if(detPlusNounIsBasicCategory && detPlusNounPattern.matcher(category).find()) {
      //System.err.println("## using DET+NOUN basic category.");
      return detPlusNoun;
    } else {
      return super.basicCategory(category);
    }
  }

  public String toString() {
    return "ArabicTreebankLanguagePack";
  }

  private static final long serialVersionUID = 9081305982861675328L;

}
