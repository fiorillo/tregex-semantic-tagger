package edu.stanford.nlp.process;


import edu.stanford.nlp.util.Function;


import edu.stanford.nlp.util.Function;

import edu.stanford.nlp.ling.BasicDocument;
import edu.stanford.nlp.ling.Document;
import edu.stanford.nlp.ling.HasWord;

import java.io.File;
import java.net.URL;
import java.util.*;


/**
 * Produces a new Document of Words in which special characters of the PTB
 * have been properly escaped.
 *
 * @author Teg Grenager (grenager@stanford.edu)
 */
public class PTBEscapingProcessor extends AbstractListProcessor
                           implements Function<List<HasWord>, List<HasWord>> {

  protected Map<String,String> stringSubs;
  protected char[] oldChars;

  // these should all appear as tokens themselves
  protected static final String[] oldStrings = {"(", ")", "[", "]", "{", "}", "*", "/","_"};
  protected static final String[] newStrings = {"-LRB-", "-RRB-", "-LCB-", "-RCB-", "-LCB-", "-RCB-", "\\*", "\\/","-"};

  // these are chars that might appear inside tokens
  protected static final char[] defaultOldChars = {'*', '/'};

  protected boolean fixQuotes = true;

  public PTBEscapingProcessor() {
    this(makeStringMap(), defaultOldChars, true);
  }

  public PTBEscapingProcessor(Map<String,String> stringSubs, char[] oldChars, boolean fixQuotes) {
    this.stringSubs = stringSubs;
    this.oldChars = oldChars;
    this.fixQuotes = fixQuotes;
  }

  protected static Map<String,String> makeStringMap() {
    Map<String,String> map = new HashMap<String,String>();
    for (int i = 0; i < oldStrings.length; i++) {
      map.put(oldStrings[i], newStrings[i]);
    }
    return map;
  }

  /*
  public Document processDocument(Document input) {
    Document result = input.blankDocument();
    result.addAll(process((List)input));
    return result;
  }
  */


  /** Unescape a List of HasWords.  Implements the 
   *  Function&lt;List&lt;HasWord&gt;, List&lt;HasWord&gt;&gt; interface.
   */
  public List<HasWord> apply(List<HasWord> hasWordsList) {
    return process(hasWordsList);
  }


  /**
   * @param input must be a List of objects of type HasWord
   */
  public List process(List input) {
    List output = new ArrayList();
    HasWord h;
    String s, newS;
    for (Iterator i = input.iterator(); i.hasNext();) {
      h = (HasWord) i.next();
      s = h.word();
      newS = stringSubs.get(s);
      if (newS != null) {
        h.setWord(newS);
      } else {
        h.setWord(escapeString(s));
      }
      output.add(h);
    }
    if (fixQuotes) {
      return fixQuotes(output);
    }
    return output;
  }

  private List fixQuotes(List input) {
    int inputSize = input.size();
    LinkedList result = new LinkedList();
    if (inputSize == 0) {
      return result;
    }
    boolean begin;
    // see if there is a quote at the end
    if (((HasWord) input.get(inputSize - 1)).word().equals("\"")) {
      // alternate from the end
      begin = false;
      for (int i = inputSize - 1; i >= 0; i--) {
        HasWord hw = (HasWord) input.get(i);
        String tok = hw.word();
        if (tok.equals("\"")) {
          if (begin) {
            hw.setWord("``");
            begin = false;
          } else {
            hw.setWord("\'\'");
            begin = true;
          }
        } // otherwise leave it alone
        result.addFirst(hw);
      } // end loop
    } else {
      // alternate from the beginning
      begin = true;
      for (int i = 0; i < inputSize; i++) {
        HasWord hw = (HasWord) input.get(i);
        String tok = hw.word();
        if (tok.equals("\"")) {
          if (begin) {
            hw.setWord("``");
            begin = false;
          } else {
            hw.setWord("\'\'");
            begin = true;
          }
        } // otherwise leave it alone
        result.addLast(hw);
      } // end loop
    }
    return result;
  }

  private String escapeString(String s) {
    StringBuilder buff = new StringBuilder();
    for (int i = 0; i < s.length(); i++) {
      char curChar = s.charAt(i);
      if (curChar == '\\') {
        // add this and the next one
        buff.append(curChar);
        i++;
        if (i < s.length()) {
          curChar = s.charAt(i);
          buff.append(curChar);
        }
      } else {
        // run through all the chars we need to escape
        for (int j = 0; j < oldChars.length; j++) {
          if (curChar == oldChars[j]) {
            buff.append('\\');
            break;
          }
        }
        // append the old char no matter what
        buff.append(curChar);
      }
    }
    return buff.toString();
  }

  /**
   * This will do the escaping on an input file. Input file must already be tokenized,
   * with tokens separated by whitespace. <br>
   * Usage: java edu.stanford.nlp.process.PTBEscapingProcessor fileOrUrl
   *
   * @param args Command line argument: a file or URL
   */
  public static void main(String[] args) {
    if (args.length != 1) {
      System.out.println("usage: java edu.stanford.nlp.process.PTBEscapingProcessor fileOrUrl");
      System.exit(0);
    }
    String filename = args[0];
    try {
      Document d = null;
      if (filename.startsWith("http://")) {
        Document dpre = new BasicDocument(WhitespaceTokenizer.factory()).init(new URL(filename));
        Processor notags = new StripTagsProcessor();
        d = notags.processDocument(dpre);
      } else {
        d = new BasicDocument(WhitespaceTokenizer.factory()).init(new File(filename));
      }
      Processor proc = new PTBEscapingProcessor();
      Document newD = proc.processDocument(d);
      for (Iterator it = newD.iterator(); it.hasNext();) {
        HasWord word = (HasWord) it.next();
        System.out.println(word);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

  }

}
