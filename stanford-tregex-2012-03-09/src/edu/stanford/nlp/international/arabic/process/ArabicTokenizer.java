package edu.stanford.nlp.international.arabic.process;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.Properties;

import edu.stanford.nlp.io.RuntimeIOException;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.objectbank.TokenizerFactory;
import edu.stanford.nlp.process.AbstractTokenizer;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.LexedTokenFactory;
import edu.stanford.nlp.process.Tokenizer;
import edu.stanford.nlp.util.PropertiesUtils;
import edu.stanford.nlp.util.StringUtils;

/**
 * Tokenizer for UTF-8 Arabic. Buckwalter encoding is *not* supported.
 * <p>
 * TODO(spenceg): Merge in rules from ibm tokenizer (v5).
 * TODO(spenceg): Add XML escaping
 * TODO(spenceg): When running from the command line, the tokenizer does not
 *   produce the correct number of newline-delimited lines for the ATB data
 *   sets.
 * 
 * @author Spence Green
 */
public class ArabicTokenizer<T extends HasWord> extends AbstractTokenizer<T> {

  // The underlying JFlex lexer
  private final ArabicLexer lexer;

  // Produces the normalization for parsing used in Green and Manning (2010)
  private static final Properties atbOptions = new Properties();
  static {
    String optionsStr = "normArDigits,normArPunc,normAlif,removeDiacritics,removeTatweel,removeQuranChars";
    String[] optionToks = optionsStr.split(",");
    for (String option : optionToks) {
      atbOptions.put(option, "true");
    }
  }

  public static ArabicTokenizer<CoreLabel> newArabicTokenizer(Reader r, Properties lexerProperties) {
    return new ArabicTokenizer<CoreLabel>(r, new CoreLabelTokenFactory(), lexerProperties);
  }

  public ArabicTokenizer(Reader r, LexedTokenFactory<T> tf, Properties lexerProperties) {
    lexer = new ArabicLexer(r, tf, lexerProperties);
  }

  @Override
  @SuppressWarnings("unchecked")
  protected T getNext() {
    try {
      T nextToken = null;
      // Depending on the orthographic normalization options,
      // some tokens can be obliterated. In this case, keep iterating
      // until we see a non-zero length token.
      do {
        nextToken = (T) lexer.next();
      } while (nextToken != null && nextToken.word().length() == 0);

      return nextToken;

    } catch (IOException e) {
      throw new RuntimeIOException(e);
    }
  }

  public static class ArabicTokenizerFactory<T extends HasWord> implements TokenizerFactory<T>, Serializable  {

    protected final LexedTokenFactory<T> factory;
    protected Properties lexerProperties = new Properties();

    public static TokenizerFactory<CoreLabel> newTokenizerFactory() {
      return new ArabicTokenizerFactory<CoreLabel>(new CoreLabelTokenFactory());
    }

    private ArabicTokenizerFactory(LexedTokenFactory<T> factory) {
      this.factory = factory;
    }

    public Iterator<T> getIterator(Reader r) {
      return getTokenizer(r);
    }

    public Tokenizer<T> getTokenizer(Reader r) {
      return new ArabicTokenizer<T>(r, factory, lexerProperties);
    }

    /**
     * Set a single tokenizer normalization option. By default, they are all disabled.
     */
    public void setOptions(String option) {
      lexerProperties.put(option, "true");
    }

    public Tokenizer<T> getTokenizer(Reader r, String extraOptions) {
      setOptions(extraOptions);
      return getTokenizer(r);
    }

    private static final long serialVersionUID = 4L;
  }

  public static TokenizerFactory<CoreLabel> factory() {
    return ArabicTokenizerFactory.newTokenizerFactory();
  }

  public static TokenizerFactory<CoreLabel> atbFactory() {
    TokenizerFactory<CoreLabel> tf = ArabicTokenizerFactory.newTokenizerFactory();
    for (String option : atbOptions.stringPropertyNames()) {
      tf.setOptions(option);
    }
    return tf;
  }

  /**
   * A fast, rule-based tokenizer for Modern Standard Arabic (UTF-8 encoding).
   * Performs punctuation splitting and light tokenization by default.
   * Orthographic normalization options are available, and can be enabled with
   * command line options.
   * 
   * The following normalization options are provided:
   * <ul>
   * <li><code>useUTF8Ellipsis</code> : Replaces sequences of three or more full stops with \u2026</li>
   * <li><code>normArDigits</code> : Convert Arabic digits to ASCII equivalents</li>
   * <li><code>normArPunc</code> : Convert Arabic punctuation to ASCII equivalents</li>
   * <li><code>normAlif</code> : Change all alif forms to bare alif</li>
   * <li><code>normYa</code> : Map ya to alif maqsura</li>
   * <li><code>removeDiacritics</code> : Strip all diacritics</li>
   * <li><code>removeTatweel</code> : Strip tatweel elongation character</li>
   * <li><code>removeQuranChars</code> : Remove diacritics that appear in the Quran</li>
   * <li><code>removeProMarker</code> : Remove the ATB null pronoun marker</li>
   * <li><code>removeSegMarker</code> : Remove the ATB clitic segmentation marker</li>
   * <li><code>removeMorphMarker</code> : Remove the ATB morpheme boundary markers</li>
   * <li><code>atbEscaping</code> : Replace left/right parentheses with ATB escape characters</li>
   * </ul>
   * 
   * @param args
   */
  public static void main(String[] args) {
    if (args.length > 0 && args[0].contains("help")) {
      System.err.printf("Usage: java %s [OPTIONS] < file%n", ArabicTokenizer.class.getName());
      System.err.printf("%nOptions:%n");
      System.err.println("   -help : Print this message. See javadocs for all normalization options.");
      System.err.println("   -atb  : Tokenization for the parsing experiments in Green and Manning (2010)");
      System.exit(-1);
    }

    // Process normalization options
    final Properties tokenizerOptions = StringUtils.argsToProperties(args);
    final TokenizerFactory<CoreLabel> tf = tokenizerOptions.containsKey("atb") ?
        ArabicTokenizer.atbFactory() : ArabicTokenizer.factory();
    for (String option : tokenizerOptions.stringPropertyNames()) {
      tf.setOptions(option);        
    }
    
    // Read the file
    int nLines = 0;
    int nTokens = 0;
    final String encoding = "UTF-8";
    final String nl = System.getProperty("line.separator");
    try {
      Tokenizer<CoreLabel> tokenizer = tf.getTokenizer(new InputStreamReader(System.in, encoding));
      while (tokenizer.hasNext()) {
        ++nTokens;
        String word = tokenizer.next().word();
        System.out.print(word);
        if (word.equals(nl)) {
          ++nLines;
        } else {
          System.out.print(" ");
        }
      }
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }
    System.err.printf("Done! Tokenized %d lines (%d tokens)%n", nLines, nTokens);
  }
}
