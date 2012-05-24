package edu.stanford.nlp.parser.lexparser;

import edu.stanford.nlp.util.Function;
import edu.stanford.nlp.trees.LeftHeadFinder;
import edu.stanford.nlp.trees.Tree;

/** The purpose of this class is to do the necessary transformations to
 *  parse trees read off the treebank, so that they can be passed to a
 *  <code>MLEDependencyGrammarExtractor</code>.
 * 
 * @author Kristina Toutanova (kristina@cs.stanford.edu)
 */
public class TransformTreeDependency implements Function<Tree,Tree> {

  TreeAnnotatorAndBinarizer binarizer;
  CollinsPuncTransformer collinsPuncTransformer;

  public TransformTreeDependency(TreebankLangParserParams tlpParams, boolean forceCNF) {
    if (!Train.leftToRight) {
      binarizer = new TreeAnnotatorAndBinarizer(tlpParams, forceCNF, !Train.outsideFactor(), true);
    } else {
      binarizer = new TreeAnnotatorAndBinarizer(tlpParams.headFinder(), new LeftHeadFinder(), tlpParams, forceCNF, !Train.outsideFactor(), true);
    }
    if (Train.collinsPunc) {
      collinsPuncTransformer = new CollinsPuncTransformer(tlpParams.treebankLanguagePack());
    }
  }


  public Tree apply(Tree tree) {

    if (Train.hSelSplit) {
      binarizer.setDoSelectiveSplit(false);
      if (Train.collinsPunc) {
        tree = collinsPuncTransformer.transformTree(tree);
      }
      binarizer.transformTree(tree);
      binarizer.setDoSelectiveSplit(true);
    }

    if (Train.collinsPunc) {
      tree = collinsPuncTransformer.transformTree(tree);
    }
    tree = binarizer.transformTree(tree);
    return tree;
  }

}
