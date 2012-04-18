package edu.stanford.nlp.trees.international.arabic;

import edu.stanford.nlp.trees.PennTreebankTokenizer;
import edu.stanford.nlp.process.Tokenizer;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;


/**
 * Builds a tokenizer for English PennTreebank (release 2) trees.
 * This is currently internally implemented via a java.io.StreamTokenizer.
 *
 * @author Christopher Manning
 */
public class ArabicTreebankTokenizer extends PennTreebankTokenizer {
  private ArabicTreebankLexer lexer = null;

  public ArabicTreebankTokenizer(Reader r) {
    super(r);
    lexer = new ArabicTreebankLexer(r);
  }

  /**
   * Internally fetches the next token.
   *
   * @return the next token in the token stream, or null if none exists.
   */
  public Object getNext() {
    try {
      while (true) {
        int nextToken = st.nextToken();
        switch (nextToken) {
          case java.io.StreamTokenizer.TT_EOL:
            System.out.println("end of line");
            return eolString;
          case java.io.StreamTokenizer.TT_EOF:
            return null;
          case java.io.StreamTokenizer.TT_WORD:
            if (st.sval.equals(":::")) {
              nextToken = st.nextToken();
              nextToken = st.nextToken();
              if ( ! st.sval.equals(":::")) {
                System.err.println("ArabicTreebankTokenizer assumptions broken!");
              }
            } else if(st.sval.startsWith(";;")) {
              String last = st.sval;
              while(st.sval != null && !st.sval.equals("\n")) {
                last = st.sval;
                nextToken = st.nextToken();

              }
              if(st.sval == null) System.out.println("!!!!!!!!!!! LAST: " + last);
//              System.out.println("READ COMMENT");
              return st.sval;
            } else {
              return st.sval;
            }
            break;
          case java.io.StreamTokenizer.TT_NUMBER:
            return Double.toString(st.nval);
          default:
            char[] t = {(char) nextToken};    // (array initialization)
            return new String(t);
        }
      }
    } catch (IOException ioe) {
      // do nothing, return null
    }
    return null;
  }

  /**
   * Internally fetches the next token.
   *
   * @return the next token in the token stream, or null if none exists.
   */
//  public Object getNext() {
//    try {
//      int a = 0;
//      while ((a = lexer.yylex()) == lexer.IGNORE) {
//        //System.err.println("#ignored: " + lexer.match());
//
//      }
//      if (a == lexer.YYEOF) {
//        return null;
//      } else {
//       //System.err.println("#matched: " + lexer.match());
////        System.out.println("a="+a+"\tTT_WORD="+java.io.StreamTokenizer.TT_WORD);
////        switch (a) {
////        case java.io.StreamTokenizer.TT_EOL:
////          //return eolString;
////        case java.io.StreamTokenizer.TT_EOF:
////          //return null;
////        case java.io.StreamTokenizer.TT_WORD:
////          if (st.sval.equals(":::")) {
////            nextToken = st.nextToken();
////            nextToken = st.nextToken();
////            if ( ! st.sval.equals(":::")) {
////              System.err.println("ArabicTreebankTokenizer assumptions broken!");
////            }
////          } else {
//            return lexer.match();//st.sval;
//          //}
//          //break;
////        case java.io.StreamTokenizer.TT_NUMBER:
////          return Double.toString(st.nval);
////        default:
////          char[] t = {(char) a};    // (array initialization)
////          return new String(t);
////      }      }
//      }
//    } catch (IOException ioe) {
//      // do nothing, return null
//      //ioe.printStackTrace();
//    }
//    return null;
//  }

  public static void main(String[] args) throws IOException {
    Tokenizer att = new ArabicTreebankTokenizer(new FileReader(args[0]));
    while (att.hasNext()) {
      System.out.println(att.next());
    }
  }

}
