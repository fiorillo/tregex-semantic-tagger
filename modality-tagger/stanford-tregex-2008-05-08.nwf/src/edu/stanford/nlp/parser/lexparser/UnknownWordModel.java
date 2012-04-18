package edu.stanford.nlp.parser.lexparser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;

import edu.stanford.nlp.trees.Tree;

public interface UnknownWordModel extends Serializable {
  /** One unknown word model may allow different options to be set; for example,
   *  several models of unknown words for a given language could be included in one
   *  class.  The unknown level can be used to set the model one would like.  Effects
   *  of the level will vary based on the implementing class.  If a given class only
   *  includes one model, setting the unknown level should have no effect.  */
  void setUnknownLevel(int unknownLevel);
  
  /**
   * Get the level of equivalence classing for the model.
   * @return
   */
  int getUnknownLevel();
  
  /**
   * Returns the lexicon used by this unknown word model;
   * lexicon is used to check information about words being seen/unseen
   * @return
   */
  Lexicon getLexicon();
  
  /**
   * Connect the unknown word model to a specific lexicon; often required to
   * set a lexicon prior to using the model.
   * @param l
   */
  void setLexicon(Lexicon l);
  
  /**
   * Trains this unknown word model on the Collection of trees.
   */
  void train(Collection<Tree> trees);

  /**
   * Get the score of this word with this tag (as an IntTaggedWord) at this 
   * loc.
   * (Presumably an estimate of P(word | tag).)
   * Assumes the word is unknown.
   * @param iTW An IntTaggedWord pairing a word and POS tag
   * @param loc The position in the sentence.  <i>In the default implementation
   *               this is used only for unknown words to change their
   *               probability distribution when sentence initial.</i>
   * @return A double valued score, usually - log P(word|tag)
   */
  double score(IntTaggedWord iTW, int loc);
  
  
  /**
   * This routine returns a String that is the "signature" of the class of a
   * word. For, example, it might represent whether it is a number of ends in
   * -s. The strings returned by convention match the pattern UNK or UNK-.* ,
   * which is just assumed to not match any real word. Behavior depends on the
   * unknownLevel (-uwm flag) passed in to the class. 
   *
   * @param word The word to make a signature for
   * @param loc Its position in the sentence (mainly so sentence-initial
   *          capitalized words can be treated differently)
   * @return A String that is its signature (equivalence class)
   */
  public String getSignature(String word, int loc);
  public int getSignatureIndex(int wordIndex, int sentencePosition);

  
  public void readData(BufferedReader in) throws IOException;
}
