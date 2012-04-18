package edu.stanford.nlp.trees;

import edu.stanford.nlp.ling.CategoryWordTagFactory;
import edu.stanford.nlp.ling.LabelFactory;

import java.io.Reader;

/**
 * This class implements a <code>TreeReaderFactory</code> that produces
 * labeled, scored array-based Trees, which have been cleaned up to
 * delete empties, etc.   This seems to be a common case.
 * By default, the labels are of type CategoryWordTag,
 * but a different Label type can be specified by the user.
 *
 * @author Christopher Manning
 */
public class LabeledScoredTreeReaderFactory implements TreeReaderFactory {

  private final LabelFactory lf;

  /**
   * Create a new TreeReaderFactory with CategoryWordTag labels.
   */
  public LabeledScoredTreeReaderFactory() {
    lf = new CategoryWordTagFactory();
  }

  public LabeledScoredTreeReaderFactory(LabelFactory lf) {
    this.lf = lf;
  }

  /**
   * An implementation of the <code>TreeReaderFactory</code> interface.
   * It creates a <code>TreeReader</code> which normalizes trees using
   * the <code>BobChrisTreeNormalizer</code>, and makes
   * <code>LabeledScoredTree</code> objects with
   * <code>CategoryWordTag</code> labels (unless otherwise specified on
   * construction).
   */
  public TreeReader newTreeReader(Reader in) {
    return new PennTreeReader(in, new LabeledScoredTreeFactory(lf), new BobChrisTreeNormalizer());
  }

}
