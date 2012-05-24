package edu.stanford.nlp.parser.lexparser;

import java.util.Collection;
import java.util.Iterator;

import edu.stanford.nlp.util.Function;
import edu.stanford.nlp.trees.Tree;

public interface Extractor {
  public Object extract(Collection<Tree> trees);

  public Object extract(Iterator<Tree> iterator, Function<Tree, Tree> f);
}

