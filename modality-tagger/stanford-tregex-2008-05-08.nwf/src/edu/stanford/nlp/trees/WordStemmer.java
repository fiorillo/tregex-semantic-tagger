package edu.stanford.nlp.trees;

import edu.stanford.nlp.ling.CategoryWordTagFactory;
import edu.stanford.nlp.ling.WordTag;
import edu.stanford.nlp.process.Morphology;

import java.io.Reader;

/**
 * Stems the Words in a Tree using Morphology.
 *
 * @author Huy Nguyen (htnguyen@cs.stanford.edu)
 */
public class WordStemmer implements TreeVisitor, TreeReaderFactory {

  private Morphology morpha;

  public WordStemmer() {
    morpha = new Morphology();
  }

  public void visitTree(Tree t) {
    processTree(t, null);
  }

  private void processTree(Tree t, String tag) {
    if (t.isPreTerminal()) {
      tag = t.label().value();
    }
    if (t.isLeaf()) {
      WordTag wt = morpha.stem(t.label().value(), tag);
      t.label().setValue(wt.word());
    } else {
      for (Tree kid : t.children()) {
        processTree(kid, tag);
      }
    }
  }

  public TreeReader newTreeReader(Reader in) {
    return new PennTreeReader(in, new LabeledScoredTreeFactory(new CategoryWordTagFactory()), new BobChrisTreeNormalizer());
  }

  /**
   * Reads, stems, and prints the trees in the file.
   * Usage: WordStemmer file
   */
  public static void main(String[] args) {
    WordStemmer ls = new WordStemmer();
    Treebank treebank = new DiskTreebank(ls);
    treebank.loadPath(args[0]);
    for (Tree tree : treebank) {
      ls.visitTree(tree);
      System.out.println(tree);
    }
  }

}
