package edu.stanford.nlp.parser.lexparser;

import edu.stanford.nlp.io.NumberRangeFileFilter;
import edu.stanford.nlp.ling.CategoryWordTag;
import edu.stanford.nlp.ling.WordFactory;
import edu.stanford.nlp.trees.*;
import edu.stanford.nlp.util.Pair;

import java.io.Reader;
import java.util.*;

/**
 * Class for getting an annotated treebank.
 *
 * @author Dan Klein
 */
public class TreebankAnnotator {

  TreeTransformer treeTransformer;
  TreeTransformer treeUnTransformer;
  TreeTransformer collinizer;

  public List annotateTrees(List trees) {
    List annotatedTrees = new ArrayList();
    for (Iterator treeI = trees.iterator(); treeI.hasNext();) {
      Tree tree = (Tree) treeI.next();
      annotatedTrees.add(treeTransformer.transformTree(tree));
    }
    return annotatedTrees;
  }

  public List deannotateTrees(List trees) {
    List deannotatedTrees = new ArrayList();
    for (Iterator treeI = trees.iterator(); treeI.hasNext();) {
      Tree tree = (Tree) treeI.next();
      deannotatedTrees.add(treeUnTransformer.transformTree(tree));
    }
    return deannotatedTrees;
  }


  public static Pair extractGrammars(List trees) {
    BinaryGrammarExtractor binaryGrammarExtractor = new BinaryGrammarExtractor();
    Pair pair = (Pair) binaryGrammarExtractor.extract(trees);
    return pair;
  }

  public static Lexicon extractLexicon(List trees, Options op) {
    Lexicon lexicon = op.tlpParams.lex(op.lexOptions);
    lexicon.train(trees);
    return lexicon;
  }

  public static List getTrees(String path, int low, int high, int minLength, int maxLength) {
    Treebank treebank = new DiskTreebank(new TreeReaderFactory() {
      public TreeReader newTreeReader(Reader in) {
        return new PennTreeReader(in, new LabeledScoredTreeFactory(new WordFactory()), new BobChrisTreeNormalizer());
      }
    });
    treebank.loadPath(path, new NumberRangeFileFilter(low, high, true));
    List trees = new ArrayList();
    for (Iterator treeI = treebank.iterator(); treeI.hasNext();) {
      Tree tree = (Tree) treeI.next();
      if (tree.yield().size() <= maxLength && tree.yield().size() >= minLength) {
        trees.add(tree);
      }
    }
    return trees;
  }

  public static List removeDependencyRoots(List trees) {
    List prunedTrees = new ArrayList();
    for (Iterator treeI = trees.iterator(); treeI.hasNext();) {
      Tree tree = (Tree) treeI.next();
      prunedTrees.add(removeDependencyRoot(tree));
    }
    return prunedTrees;
  }

  static Tree removeDependencyRoot(Tree tree) {
    List childList = tree.getChildrenAsList();
    Tree last = (Tree) childList.get(childList.size() - 1);
    if (!last.label().value().equals(Lexicon.BOUNDARY_TAG)) {
      return tree;
    }
    List lastGoneList = childList.subList(0, childList.size() - 1);
    tree.setChildren(lastGoneList);
    return tree;
  }

  public Tree collinize(Tree tree) {
    return collinizer.transformTree(tree);
  }

  public TreebankAnnotator(Options op, String treebankRoot) {
    //    op.tlpParams = new EnglishTreebankParserParams();
    // CDM: Aug 2004: With new implementation of treebank split categories,
    // I've hardwired this to load English ones.  Otherwise need training data.
    // Train.splitters = new HashSet(Arrays.asList(op.tlpParams.splitters()));
    Train.splitters = ParentAnnotationStats.getEnglishSplitCategories(treebankRoot);
    Train.sisterSplitters = new HashSet(Arrays.asList(op.tlpParams.sisterSplitters()));
    op.setOptions("-acl03pcfg", "-cnf");
    treeTransformer = new TreeAnnotatorAndBinarizer(op.tlpParams, op.forceCNF, !Train.outsideFactor(), true);
    //    BinarizerFactory.TreeAnnotator.setTreebankLang(op.tlpParams);
    treeUnTransformer = new Debinarizer(op.forceCNF);
    collinizer = op.tlpParams.collinizer();
  }


  public static void main(String[] args) {
    CategoryWordTag.printWordTag = false;
    String path = args[0];
    List trees = getTrees(path, 200, 219, 0, 10);
    ((Tree) trees.iterator().next()).pennPrint();
    Options op = new Options();
    List annotatedTrees = TreebankAnnotator.removeDependencyRoots(new TreebankAnnotator(op, path).annotateTrees(trees));
    ((Tree) annotatedTrees.iterator().next()).pennPrint();
  }

}
