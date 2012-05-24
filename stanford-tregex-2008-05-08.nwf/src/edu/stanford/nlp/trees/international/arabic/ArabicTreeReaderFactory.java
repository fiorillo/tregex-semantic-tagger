package edu.stanford.nlp.trees.international.arabic;

import java.io.Reader;
import java.io.Serializable;

import edu.stanford.nlp.trees.*;
import edu.stanford.nlp.util.Filter;


/** Reads ArabicTreebank trees.  See {@link ArabicTreeNormalizer} for the 
 *  meaning of the constructor parameters.
 *
 *  @author Roger Levy
 *  @author Christopher Manning
 */
public class ArabicTreeReaderFactory implements TreeReaderFactory,
                                                Serializable {

  private boolean retainNPTmp;
  private boolean retainPRD;
  private boolean changeNoLabels;
  private boolean filterX;
  
  public ArabicTreeReaderFactory() {
    this(false, false, false, false);
  }

  public ArabicTreeReaderFactory(boolean retainNPTmp, boolean retainPRD, 
                                 boolean changeNoLabels, boolean filterX) {
    this.retainNPTmp = retainNPTmp;
    this.retainPRD = retainPRD;
    this.changeNoLabels = changeNoLabels;
    this.filterX = filterX;
  }

  public TreeReader newTreeReader(Reader in) {
    TreeReader tr = new PennTreeReader(in, new LabeledScoredTreeFactory(), new ArabicTreeNormalizer(retainNPTmp,retainPRD,changeNoLabels), new ArabicTreebankTokenizer(in));
    if (filterX) {
      tr = new FilteringTreeReader(tr, new XFilter());
    }
    return tr;
  }


  static class XFilter implements Filter<Tree> {

    public XFilter() {}

    public boolean accept(Tree t) {
      return ! (t.numChildren() == 1 && "X".equals(t.firstChild().value()));
    }

  } // end class XFilter


  public static class ArabicXFilteringTreeReaderFactory extends ArabicTreeReaderFactory {

    public ArabicXFilteringTreeReaderFactory() {
      super(false, false, false, true);
    }

  }


  public static class ArabicRawTreeReaderFactory extends ArabicTreeReaderFactory {

    public ArabicRawTreeReaderFactory() {
      super(false, false, true, false);
    }

  }

} // end class ArabicTreeReaderFactory

