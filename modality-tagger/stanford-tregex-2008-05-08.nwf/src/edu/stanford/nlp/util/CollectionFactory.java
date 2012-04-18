package edu.stanford.nlp.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

/**
 * Factory for vending Collections.  It's a class instead of an interface because I guessed that it'd primarily be used for its inner classes.
 *
 * @author Dan Klein (klein@cs.stanford.edu)
 */
abstract public class CollectionFactory<T> implements Serializable {
  public static final CollectionFactory ARRAY_LIST_FACTORY = new ArrayListFactory();
  public static final CollectionFactory LINKED_LIST_FACTORY = new LinkedListFactory();
  public static final CollectionFactory HASH_SET_FACTORY = new HashSetFactory();

  /** This method allows type safety in calling code. */
  public static <E> CollectionFactory<E> hashSetFactory() {
    return HASH_SET_FACTORY;
  }

  public static <E> CollectionFactory<E> arrayListFactory() {
      return ARRAY_LIST_FACTORY;
    }

    public static class ArrayListFactory<T> extends CollectionFactory<T> {
    public Collection<T> newCollection() {
      return new ArrayList<T>();
    }

    public Collection<T> newEmptyCollection() {
      return Collections.emptyList();
    }
  }

  public static <E> CollectionFactory<E> linkedListFactory() {
      return LINKED_LIST_FACTORY;
    }

    public static class LinkedListFactory<T> extends CollectionFactory<T> {
    public Collection<T> newCollection() {
      return new LinkedList<T>();
    }

    public Collection<T> newEmptyCollection() {
      return Collections.emptyList();
    }
  }

  
  public static class HashSetFactory<T> extends CollectionFactory<T> {
    public Collection<T> newCollection() {
      return new HashSet<T>();
    }

    public Collection<T> newEmptyCollection() {
      return Collections.emptySet();
    }
  }

  public abstract Collection<T> newCollection();

  abstract public Collection<T> newEmptyCollection();
}
