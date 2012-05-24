package edu.stanford.nlp.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.zip.GZIPInputStream;

import edu.stanford.nlp.objectbank.IteratorFromReaderFactory;
import edu.stanford.nlp.util.Function;

/**
 * This class takes a filename and allows you
 * to iterate through it line by line.  It
 * does _not_ read in the whole file immediately
 * and instead reads in a new line every time the
 * iterator is called.
 * @author Anna Rafferty
 * @author Michel Galley (gzip support)
 *
 */
public class FileLines implements Collection<String>, IteratorFromReaderFactory<String> {

  public static final String DEFAULT_ENCODING = "utf-8";

  /** Parameters */
  private File file;
  private String charEncoding = null;
  private boolean trim = false;
  private Function<String, String> function = null;

  /**
   * Constructs a new FileLines object for reading
   * lines from the given filename; checks that
   * the file exists upon creation, but does not read
   * until an iterator is created.
   * @param filename The file to be opened
   */
  public FileLines(String filename) {
    this(filename, null);
  }

  /**
   * Constructs a new FileLines object for reading
   * lines from the given filename which is in the
   * given encoding; if encoding is not recognized,
   * uses the default encoding for your platform
   * @param filename The file to be opened
   * @param encoding The char encoding to be used
   */
  public FileLines(String filename, String encoding) {
    this(filename, encoding, false);

  }

  /**
   * Constructs a new FileLines object for reading
   * lines from the given filename which is in the
   * given encoding; if encoding is not recognized,
   * uses a default encoding.  If trim is true,
   * trims excess whitespace from the lines.
   * @param filename
   */
  public FileLines(String filename, String encoding, boolean trim) {
    this(filename, encoding, trim, null);
  }

  /**
   * Constructs a new FileLines object for reading
   * lines from the given filename which is in the
   * given encoding; if encoding is not recognized,
   * uses a default encoding.  If trim is true,
   * trims excess whitespace from the lines. If a
   * function is given, the function will be applied
   * to each line and the result will be return; function
   * is applied after trimming if trimming is true.
   * @param filename
   */
  public FileLines(String filename, String encoding, boolean trim, Function<String, String> function) {
    this.file = new File(filename);
    this.charEncoding = encoding;
    this.trim = trim;
    this.function = function;
  }

  public Iterator<String> iterator() {
    try {
      BufferedReader r;
      InputStream s;
      if (file.getAbsolutePath().endsWith(".gz")) {        
        try {
          s = new GZIPInputStream(new FileInputStream(file));
        } catch(IOException e) {
          System.err.println("FileLines warning: " + file.getName() + " is a bad gzip file.");
          System.err.println("FileLines warning: now re-opening it as a plain text file.");
          //Assume file is plain text:
          s = new FileInputStream(file);
        }
      } else {
        s = new FileInputStream(file);
      }
      if (charEncoding != null) {
        try {
          r = new BufferedReader(new InputStreamReader(s, charEncoding));
        } catch(Exception e) {
          //Assume that the charEncoding was bad and try again
          r = new BufferedReader(new InputStreamReader(s));
        }
      } else {
        r = new BufferedReader(new InputStreamReader(s));
      }
      return new FileIterator(r);
    } catch(FileNotFoundException e) {
      System.err.println("FileLines exception: " + file.getName() + " not found.");
      e.printStackTrace();
    }
    return null;
  }

  private class FileIterator implements Iterator<String> {
    BufferedReader reader;
    String nextLine;

    public FileIterator(BufferedReader r) {
      this.reader = r;
      readNextLine();
    }
    public boolean hasNext() {
      return (nextLine != null);
    }

    public String next() {
      String curLine = nextLine;
      readNextLine();
      if(trim) {
        curLine = curLine.trim();
      }
      if(function != null) {
        function.apply(curLine);
      }
      return curLine;
    }

    private void readNextLine() {
      try {
        nextLine = reader.readLine();
      } catch(Exception e) {
        System.err.println("FileLines: IO exception");
        e.printStackTrace();
        nextLine = null;
      }
    }

    /**
     * Unsupported Operation.  File-backed iterator
     */
    public void remove() {
      throw new UnsupportedOperationException();
    }
  }


  /**
   * Unsupported Operation.  File-backed class
   */
  public boolean add(String arg0) {
    throw new UnsupportedOperationException();
  }

  /**
   * Unsupported Operation.  File-backed class
   */
  public boolean addAll(Collection<? extends String> c) {
    throw new UnsupportedOperationException();
  }

  /**
   * Unsupported Operation.  File-backed class
   */
  public void clear() {
    throw new UnsupportedOperationException();
  }

  /**
   * Can be slow.  Usage not recommended.
   */
  public boolean contains(Object o) {
    for(String cur : this) {
      if(cur.equals(o))
        return true;
    }
    return false;
  }

  /**
   * Can be slow.  Usage not recommended.
   */
  public boolean containsAll(Collection<?> c) {
    for (Object obj : c) {
      if (!contains(obj)) {
        return false;
      }
    }
    return true;
  }

  public boolean isEmpty() {
    return !iterator().hasNext();
  }

  /**
   * Unsupported Operation.  File-backed class
   */
  public boolean remove(Object o) {
    throw new UnsupportedOperationException();
  }

  /**
   * Unsupported Operation.  File-backed class
   */
  public boolean removeAll(Collection<?> c) {
    throw new UnsupportedOperationException();
  }

  /**
   * Unsupported Operation.  File-backed class
   */
  public boolean retainAll(Collection<?> c) {
    throw new UnsupportedOperationException();
  }

  /**
   * Can be slow.  Usage not recommended.
   */
  @SuppressWarnings("unused")
  public int size() {
    int i = 0;
    for(String cur : this) {
      i++;
    }
    return i;
  }

  /**
   * Can be slow.  Usage not recommended.
   */
  public Object[] toArray() {
    Iterator<String> iter = iterator();
    ArrayList<Object> al = new ArrayList<Object>();
    while (iter.hasNext()) {
      al.add(iter.next());
    }
    return al.toArray();
  }

  /**
   * Can be slow.  Usage not recommended.
   * String must extend T
   */
  public <T> T[] toArray(T[] a) {
    List<String> lines = new ArrayList<String>();
    Iterator<String> iter = iterator();
    while (iter.hasNext()) {
      lines.add(iter.next());
    }
    T[] array = (T[]) lines.toArray(a);
    return array;
  }

  public Iterator<String> getIterator(Reader r) {
    return new FileIterator(new BufferedReader(r));
  }

  //For testing only
  public static void main(String[] args) {
    FileLines f = new FileLines(args[0]);
    for(String cur : f) {
      System.out.println(cur);

    }
  }

}
