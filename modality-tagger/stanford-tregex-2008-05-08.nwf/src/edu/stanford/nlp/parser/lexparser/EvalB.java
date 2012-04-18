package edu.stanford.nlp.parser.lexparser;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import edu.stanford.nlp.trees.Tree;

public class EvalB {

  private static PrintWriter goldWriter, testWriter;

  public static void initEVALBfiles(TreebankLangParserParams tlpParams) {
    try {
      goldWriter = tlpParams.pw(new FileOutputStream("parses.gld"));
      testWriter = tlpParams.pw(new FileOutputStream("parses.tst"));
    } catch (IOException e) {
      System.exit(0);
    }
  }

  public static void closeEVALBfiles() {
    goldWriter.close();
    testWriter.close();
  }

  static void writeEVALBline(Tree gold, Tree test) {
    if (false) {
      if (gold.yield().length() <= 1) {
        goldWriter.println("(ROOT (X X))");
        testWriter.println("(ROOT (X X))");
        return;
      }
    }
    goldWriter.println(gold.toString());
    testWriter.println(test.toString());
    System.err.println("Wrote EVALB lines.");
  }

}

