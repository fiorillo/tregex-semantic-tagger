package edu.stanford.nlp.process;


import edu.stanford.nlp.util.Function;


import edu.stanford.nlp.util.Function;

import edu.stanford.nlp.ling.Document;

import java.util.ArrayList;
import java.util.List;

/**
 * Class AbstractListProcessor
 *
 * @author Teg Grenager
 */
public abstract class AbstractListProcessor<IN,OUT> implements ListProcessor<IN,OUT>, Processor<IN,OUT> {

  public AbstractListProcessor() {
  }

  public Document<OUT> processDocument(Document<IN> in) {
    Document<OUT> doc = in.blankDocument();
    doc.addAll(process(in));
    return doc;
  }

  /** Process a list of lists of tokens.  For example this might be a
   *  list of lists of words.
   *
   * @param lists a List of objects of type List
   * @return a List of objects of type List, each of which has been processed.
   */
  public List<List<OUT>> processLists(List<List<IN>> lists) {
    List<List<OUT>> result = new ArrayList<List<OUT>>(lists.size());
    for (List<IN> list : lists) {
      List<OUT> outList = process(list);
      result.add(outList);
    }
    return result;
  }

}
