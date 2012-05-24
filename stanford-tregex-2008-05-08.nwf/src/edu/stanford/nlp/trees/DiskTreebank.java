package edu.stanford.nlp.trees;

import edu.stanford.nlp.io.NumberRangesFileFilter;
import edu.stanford.nlp.util.FilePathProcessor;
import edu.stanford.nlp.util.FileProcessor;
import edu.stanford.nlp.util.Timing;

import edu.stanford.nlp.parser.lexparser.TreebankLangParserParams;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A <code>DiskTreebank</code> is a <code>Collection</code> of
 * <code>Tree</code>s.
 * A <code>DiskTreebank</code> object stores merely the information to
 * get at a corpus of trees that is stored on disk.  Access is usually
 * via apply()'ing a TreeVisitor to each Tree in the Treebank or by using
 * an iterator() to get an iteration over the Trees.
 * <p/>
 * If the root Label of the Tree objects built by the TreeReader
 * implements HasIndex, then the filename and index of the tree in
 * a corpus will be inserted as they are read in.
 *
 * @author Christopher Manning
 */
public final class DiskTreebank extends Treebank {

  private static final boolean PRINT_FILENAMES = false;
  private ArrayList<File> filePaths = new ArrayList<File>();
  private ArrayList<FileFilter> fileFilters = new ArrayList<FileFilter>();

  /**
   * Maintains as a class variable the <code>File</code> from which
   * trees are currently being read.
   */
  private File currentFile = null;

  /**
   * If this is true, the system will retry opening files a few times
   * before concluding that there is really a problem. This seems to
   * be necessary with NFS on Linux boxes -- at least the DB ones.
   */
  private static final boolean BROKEN_NFS = true;


  /**
   * Create a new DiskTreebank.
   * The trees are made with a <code>LabeledScoredTreeReaderFactory</code>.
   * <p/>
   * <i>Compatibility note: Until Sep 2004, this used to create a Treebank
   * with a SimpleTreeReaderFactory, but this was changed as the old
   * default wasn't very useful, especially to naive users.</i>
   */
  public DiskTreebank() {
    this(new LabeledScoredTreeReaderFactory());
  }

  /**
   * Create a new tree bank, set the encoding for file access.
   *
   * @param encoding The charset encoding to use for treebank file decoding
   */
  public DiskTreebank(String encoding) {
    this(new LabeledScoredTreeReaderFactory(), encoding);
  }

  /**
   * Create a new DiskTreebank.
   *
   * @param trf the factory class to be called to create a new
   *            <code>TreeReader</code>
   */
  public DiskTreebank(TreeReaderFactory trf) {
    super(trf);
  }

  /**
   * Create a new DiskTreebank.
   *
   * @param trf      the factory class to be called to create a new
   *                 <code>TreeReader</code>
   * @param encoding The charset encoding to use for treebank file decoding
   */
  public DiskTreebank(TreeReaderFactory trf, String encoding) {
    super(trf, encoding);
  }

  /**
   * Create a new Treebank.
   * The trees are made with a <code>LabeledScoredTreeReaderFactory</code>.
   * <p/>
   * <i>Compatibility note: Until Sep 2004, this used to create a Treebank
   * with a SimpleTreeReaderFactory, but this was changed as the old
   * default wasn't very useful, especially to naive users.</i>
   *
   * @param initialCapacity The initial size of the underlying Collection.
   *                        For a <code>DiskTreebank</code>, this parameter is ignored.
   */
  public DiskTreebank(int initialCapacity) {
    this(initialCapacity, new LabeledScoredTreeReaderFactory());
  }

  /**
   * Create a new Treebank.
   *
   * @param initialCapacity The initial size of the underlying Collection,
   *                        For a <code>DiskTreebank</code>, this parameter is ignored.
   * @param trf             the factory class to be called to create a new
   *                        <code>TreeReader</code>
   */
  public DiskTreebank(int initialCapacity, TreeReaderFactory trf) {
    this(trf);
  }


  /**
   * Empty a <code>Treebank</code>.
   */
  public void clear() {
    filePaths.clear();
    fileFilters.clear();
  }

  /**
   * Load trees from given directory.  This version just records
   * the paths to be processed, and actually processes them at apply time.
   *
   * @param path file or directory to load from
   * @param filt a FilenameFilter of files to load
   */
  public void loadPath(File path, FileFilter filt) {
    filePaths.add(path);
    fileFilters.add(filt);
  }

  /**
   * Applies the TreeVisitor to to all trees in the Treebank.
   *
   * @param tp A class that can process trees.
   */
  public void apply(final TreeVisitor tp) {
    for (Tree t : this) {
      tp.visitTree(t);
    }
  }

  /**
   * Return the <code>File</code> from which trees are currently being
   * read by <code>apply()</code> and passed to a
   * <code>TreePprocessor</code>.
   * <p/>
   * This is useful if one wants to map the original file and
   * directory structure over to a set of modified trees.  New code
   * might prefer to build trees with labels that implement
   * HasIndex.
   *
   * @return the file that trees are currently being read from, or
   *         <code>null</code> if no file is currently open
   */
  public File getCurrentFile() {
    return currentFile;
  }

  private class DiskTreebankIterator implements Iterator<Tree> {

    private int fileUpto = -1; // before starting on index array 0
    private int treeUpto; // = 0
    private List<String> files;
    private MemoryTreebank currentFileTrees;
    private boolean hasNext;

    private DiskTreebankIterator() {
      files = new ArrayList<String>();
      // get the list of files in the Treebank via a new
      // FilePathProcessor()
      FileProcessor dtifp = new FileProcessor() {
        public void processFile(File file) {
          files.add(file.toString());
        }
      };
      int numPaths = filePaths.size();
      for (int i = 0; i < numPaths; i++) {
        FilePathProcessor.processPath(filePaths.get(i), fileFilters.get(i), dtifp);
      }
      currentFileTrees = new MemoryTreebank(treeReaderFactory(), encoding());
      // we're now all setup to read a new file on the priming call
      // first time treeUpto = currentFileTrees.size() = 0
      hasNext = primeNextFile();
    }

    private boolean primeNextFile() {
      while (fileUpto < files.size()) {
        if (treeUpto < currentFileTrees.size()) {
          return true;
        } else {
          // load the next file
          currentFileTrees.clear();
          fileUpto++;
          treeUpto = 0;
          if (fileUpto < files.size()) {
            String fname = files.get(fileUpto);
            // maybe print file name to stdout to get some feedback
            if (PRINT_FILENAMES) {
              System.err.println(fname);
            }
            currentFile = new File(fname);
            currentFileTrees.loadPath(fname);
          }
        }
      }
      // there's nothing left;
      return false;
    }

    /**
     * Returns true if the iteration has more elements.
     */
    public boolean hasNext() {
      return hasNext;
    }

    /**
     * Returns the next element in the iteration.
     */
    public Tree next() {
      Tree ret = currentFileTrees.get(treeUpto++);
      hasNext = primeNextFile();
      return ret;
    }

    /**
     * Not supported
     */
    public void remove() {
      throw new UnsupportedOperationException();
    }

  }

  /**
   * Return an Iterator over Trees in the Treebank.  This is implemented
   * by building per-file MemoryTreebanks for the files in the
   * DiskTreebank.  As such, it isn't as efficient as using
   * <code>apply()</code>.
   */
  public Iterator<Tree> iterator() {
    return new DiskTreebankIterator();
  }

  /**
   * Loads treebank and prints it.
   * All files below the designated <code>filePath</code> within the given
   * number range if any are loaded.  You can normalize the trees or not
   * (English-specific) and print trees one per line up to a certain length
   * (for EVALB).
   * <p>
   * Usage: <code>
   * java edu.stanford.nlp.trees.DiskTreebank [-maxLength n|-normalize|-treeReaderFactory class] filePath [numberRanges]
   * </code>
   *
   * @param args Array of command-line arguments
   * @throws IOException If there is a treebank file access problem
   */
  public static void main(String[] args) throws IOException {
    if (args.length == 0) {
      System.err.println("This main method will let you variously manipulate and view a treebank.");
      System.err.println("Usage: java DiskTreebank [-flags]* treebankPath fileRanges");
      System.err.println("Useful flags include:");
      System.err.println("\t-maxLength n\t-suffix ext\t-treeReaderFactory class");
      System.err.println("\t-pennPrint\t-encoding enc\t -tlpp class");
      System.err.println("\t-summary\t-decimate\t-yield\t-correct");
      return;
    }
    int i = 0;
    int maxLength = -1;
    boolean normalized = false;
    boolean decimate = false;
    boolean pennPrintTrees = false;
    boolean correct = false;
    boolean summary = false;
    boolean timing = false;
    boolean yield = false;
    String decimatePrefix = null;
    String encoding = TreebankLanguagePack.DEFAULT_ENCODING;
    String suffix = Treebank.DEFAULT_TREE_FILE_SUFFIX;
    TreeReaderFactory trf = null;

    while (i < args.length && args[i].startsWith("-")) {
      if (args[i].equals("-maxLength") && i + 1 < args.length) {
        maxLength = Integer.parseInt(args[i+1]);
        i += 2;
      } else if (args[i].equals("-normalized")) {
        normalized = true;
        i += 1;
      } else if (args[i].equalsIgnoreCase("-tlpp")) {
        try {
          final Object o = Class.forName(args[i+1]).newInstance();
          TreebankLangParserParams tlpp = (TreebankLangParserParams) o;
          trf = tlpp.treeReaderFactory();
        } catch (Exception e) {
          System.err.println("Couldn't instantiate as TreebankLangParserParams: " + args[i+1]);
          return;
        }
        i += 2;
      } else if (args[i].equals("-treeReaderFactory") || args[i].equals("-trf")) {
        try {
          final Object o = Class.forName(args[i+1]).newInstance();
          trf = (TreeReaderFactory) o;
        } catch (Exception e) {
          System.err.println("Couldn't instantiate as TreeReaderFactory: " + args[i+1]);
          return;
        }
        i += 2;
      } else if (args[i].equals("-suffix")) {
        suffix = args[i+1];
        i += 2;
      } else if (args[i].equals("-decimate")) {
        decimate = true;
        decimatePrefix = args[i+1];
        i += 2;
      } else if (args[i].equals("-encoding")) {
        encoding = args[i+1];
        i += 2;
      } else if (args[i].equals("-correct")) {
        correct = true;
        i += 1;
      } else if (args[i].equals("-summary")) {
        summary = true;
        i += 1;
      } else if (args[i].equals("-yield")) {
        yield = true;
        i += 1;
      } else if (args[i].equals("-pennPrint")) {
        pennPrintTrees = true;
        i++;
      } else if (args[i].equals("-timing")) {
        timing = true;
        i++;
      } else {
        System.err.println("Unknown option: " + args[i]);
        i++;
      }
    }
    Treebank treebank;
    if (trf == null) {
      trf = new TreeReaderFactory() {
          public TreeReader newTreeReader(Reader in) {
            return new PennTreeReader(in, new LabeledScoredTreeFactory());
          }
        };
    }
    if (normalized) {
      treebank = new DiskTreebank();
    } else {
      treebank = new DiskTreebank(trf, encoding);
    }

    final PrintWriter pw = new PrintWriter(new OutputStreamWriter(System.out, encoding), true);

    if (i + 1 < args.length ) {
      treebank.loadPath(args[i], new NumberRangesFileFilter(args[i+1], true));
    } else {
      treebank.loadPath(args[i], suffix, true);
    }
    // System.err.println("Loaded " + treebank.size() + " trees from " + args[i]);

    if (summary) {
      System.out.println(treebank.textualSummary());
    }

    if (correct) {
      treebank = new EnglishPTBTreebankCorrector().transformTrees(treebank);
    }

    if (pennPrintTrees) {
      treebank.apply(new TreeVisitor() {
          public void visitTree(Tree tree) {
            tree.pennPrint(pw);
            pw.println();
          }
        });
    }

    if (yield) {
      treebank.apply(new TreeVisitor() {
          public void visitTree(Tree tree) {
            pw.println(tree.yield().toString());
          }
        });
    }

    if (decimate) {
      Writer w1 = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(decimatePrefix + "-train.txt"), encoding));
      Writer w2 = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(decimatePrefix + "-dev.txt"), encoding));
      Writer w3 = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(decimatePrefix + "-test.txt"), encoding));
      treebank.decimate(w1, w2, w3);
    } else if (maxLength >= 0) {
      for (Tree t : treebank) {
        if (t.yield().length() <= maxLength) {
          System.out.println(t);
        }
      }
    } else if (timing) {
      System.out.println();
      Timing.startTime();
      int num = 0;
      for (Tree t : treebank) {
        num += t.yield().length();
      }
      Timing.endTime("traversing corpus, counting words with iterator");
      System.err.println("There were " + num + " words in the treebank.");

      treebank.apply(new TreeVisitor() {
          int num = 0;
          public void visitTree(final Tree t) {
            num += t.yield().length();
          }
        });
      System.err.println();
      Timing.endTime("traversing corpus, counting words with TreeVisitor");
      System.err.println("There were " + num + " words in the treebank.");

      System.err.println();
      Timing.startTime();
      System.err.println("This treebank contains " + treebank.size() + " trees.");
      Timing.endTime("size of corpus");
    }
  } // end main()

}
