package edu.stanford.nlp.trees.tregex;

interface TreePatternIterator {

  public TreePattern next();

  public TreePattern previous();

  public boolean hasNext();

  public boolean hasPrevious();

}
