package edu.stanford.nlp.trees;

import edu.stanford.nlp.ling.*;
import edu.stanford.nlp.ling.CoreAnnotations.CopyAnnotation;
import edu.stanford.nlp.parser.lexparser.TreebankLangParserParams;
import edu.stanford.nlp.util.Function;
import edu.stanford.nlp.trees.international.pennchinese.ChineseEnglishWordMap;
import edu.stanford.nlp.util.*;

import java.io.*;
import java.util.*;


/**
 * A class for customizing the print method(s) for a
 * <code>edu.stanford.nlp.trees.Tree</code> as the output of the
 * parser.  This class supports printing in multiple ways and altering
 * behavior via properties.
 *
 * @author Roger Levy
 * @author Christopher Manning
 * @author Galen Andrew
 */
public class TreePrint {

  public static final String rootLabelOnlyFormat = "rootSymbolOnly";

  /** The legal output tree formats. */
  public static final String[] outputTreeFormats = {
    "penn",
    "oneline",
    rootLabelOnlyFormat,
    "words",
    "wordsAndTags",
    "dependencies",
    "typedDependencies",
    "typedDependenciesCollapsed",
    "latexTree",
    "collocations",
    "semanticGraph"
  };

  public static final String headMark = "=H";

  private Properties formats;
  private Properties options;

  private boolean markHeadNodes; // = false;
  private boolean lexicalize; // = false;
  private boolean stem; // = false;
  private boolean transChinese; // = false;

  private HeadFinder hf;
  private TreebankLanguagePack tlp;
  private WordStemmer stemmer;
  private Filter<Dependency> dependencyFilter;
  private GrammaticalStructureFactory gsf;

  /** Pool use of one WordNetConnection.  I don't really know if
   *  Dan Bikel's WordNet code is thread safe, but it definitely doesn't
   *  close its files, and too much of our code makes TreePrint objects and
   *  then drop them on the floor, and so we run out of file handles.
   *  CDM July 2006.
   */
  private static WordNetConnection wnc;


  /** This PrintWriter is used iff the user doesn't pass one in to a
   *  call to printTree().
   */
  private PrintWriter pw = new PrintWriter(System.out, true);

  /** The anglocentric constructor. Should work for English only.
   *  @param formats The formats to print the tree in.
   */
  public TreePrint(String formats) {
    this(formats, "", new PennTreebankLanguagePack());
  }

  public TreePrint(String formats, TreebankLanguagePack tlp) {
    this(formats, "", tlp);
  }

  public TreePrint(String formats, String options, TreebankLanguagePack tlp) {
    this(formats, options, tlp, new CollinsHeadFinder());
  }

  /**
   * Make a TreePrint instance.
   *
   * @param formatString A comma separated list of ways to print each Tree.
   *                For instance, "penn" or "words,typedDependencies".
   *                Known formats are: oneline, penn, latexTree, words,
   *                wordsAndTags, rootSymbolOnly, dependencies,
   *                typedDependencies, typedDependenciesCollapsed,
   *                collocations, semanticGraph.  All of them print a blank line after
   *                the output except for oneline.  oneline is also not
   *                meaningul in XML output (it is ignored: use penn instead).
   * @param optionsString Options that additionally specify how trees are to
   *                be printed (for instance, whether stemming should be done).
   *                Known options are: <code>stem, lexicalize, markHeadNodes,
   *                xml, removeTopBracket, transChinese,
   *                includePunctuationDependencies
   *                </code>.
   * @param tlp     The TreebankLanguagePack used to do things like delete
   *                or ignore punctuation in output
   * @param hf      The HeadFinder used in printing output
   */
  public TreePrint(String formatString, String optionsString, TreebankLanguagePack tlp, HeadFinder hf) {
    formats = StringUtils.stringToProperties(formatString);
    options = StringUtils.stringToProperties(optionsString);
    List<String> okOutputs = Arrays.asList(outputTreeFormats);
    for (Object formObj : formats.keySet()) {
      String format = (String) formObj;
      if ( ! okOutputs.contains(format)) {
        throw new RuntimeException("Error: output tree format " + format + " not supported");
      }
    }

    setHeadFinder(hf);
    this.tlp = tlp;
    boolean includePunctuationDependencies;
    includePunctuationDependencies = propertyToBoolean(this.options,
                                                       "includePunctuationDependencies");
    Filter<String> puncWordFilter;
    if (includePunctuationDependencies) {
      dependencyFilter = Filters.acceptFilter();
      puncWordFilter = Filters.acceptFilter();
    } else {
      dependencyFilter = new Dependencies.DependentPuncTagRejectFilter(tlp.punctuationTagRejectFilter());
      puncWordFilter = tlp.punctuationWordRejectFilter();
    }
    stem = propertyToBoolean(this.options, "stem");
    if (stem) {
      stemmer = new WordStemmer();
    }
    if (formats.containsKey("typedDependenciesCollapsed") ||
        formats.containsKey("typedDependencies")) {
      gsf = tlp.grammaticalStructureFactory(puncWordFilter);
    }

    lexicalize = propertyToBoolean(this.options, "lexicalize");
    markHeadNodes = propertyToBoolean(this.options, "markHeadNodes");
    transChinese = propertyToBoolean(this.options, "transChinese");
  }


  private static boolean propertyToBoolean(Properties prop, String key) {
    return Boolean.parseBoolean(prop.getProperty(key));
  }

  /**
   * Prints the tree to the default PrintWriter.
   * @param t The tree to display
   */
  public void printTree(Tree t) {
    printTree(t, pw);
  }

  /**
   * Prints the tree, with an empty ID.
   * @param t The tree to display
   * @param pw The PrintWriter to print it to
   */
  public void printTree(final Tree t, PrintWriter pw) {
    printTree(t, "", pw);
  }


  /**
   * Prints the tree according to the options specified for this instance.
   * If the tree <code>t</code> is <code>null</code>, then the code prints
   * a line indicating a skipped tree.  Under the XML option this is
   * an <code>s</code> element with the <code>skipped</code> attribute having
   * value <code>true</code>, and, otherwise, it is the token
   * <code>SENTENCE_SKIPPED_OR_UNPARSABLE</code>.
   *
   * @param t The tree to display
   * @param id A name for this sentence
   * @param pw Where to dislay the tree
   */
  public void printTree(final Tree t, final String id, final PrintWriter pw) {
    final boolean inXml = propertyToBoolean(options, "xml");
    if (t == null) {
      // Parsing didn't succeed.
      if (inXml) {
        pw.print("<s");
        if (id != null && ! "".equals(id)) {
          pw.print(" id=\"" + XMLUtils.escapeXML(id) + "\"");
        }
        pw.println(" skipped=\"true\"/>");
        pw.println();
      } else {
        pw.println("SENTENCE_SKIPPED_OR_UNPARSABLE");
      }
    } else {
      if (inXml) {
        pw.print("<s");
        if (id != null && ! "".equals(id)) {
          pw.print(" id=\"" + XMLUtils.escapeXML(id) + "\"");
        }
        pw.println(">");
      }
      printTreeInternal(t, pw, inXml);
      if (inXml) {
        pw.println("</s>");
        pw.println();
      }
    }
  }


  /**
   * Prints the trees according to the options specified for this instance.
   * If the tree <code>t</code> is <code>null</code>, then the code prints
   * a line indicating a skipped tree.  Under the XML option this is
   * an <code>s</code> element with the <code>skipped</code> attribute having
   * value <code>true</code>, and, otherwise, it is the token
   * <code>SENTENCE_SKIPPED_OR_UNPARSABLE</code>.
   *
   * @param trees The list of trees to display
   * @param id A name for this sentence
   * @param pw Where to dislay the tree
   */
  public void printTrees(final List<ScoredObject<Tree>> trees, final String id, final PrintWriter pw) {
    final boolean inXml = propertyToBoolean(options, "xml");
    int ii = 0;  // incremented before used, so first tree is numbered 1
    for (ScoredObject<Tree> tp : trees) {
      ii++;
      Tree t = tp.object();
      double score = tp.score();

      if (t == null) {
        // Parsing didn't succeed.
        if (inXml) {
          pw.print("<s");
          if (id != null && ! "".equals(id)) {
            pw.print(" id=\"" + XMLUtils.escapeXML(id) + "\"");
          }
          pw.print(" n=\"" + ii + "\"");
          pw.print(" score=\"" + score + "\"");
          pw.println(" skipped=\"true\"/>");
          pw.println();
        } else {
          pw.println("SENTENCE_SKIPPED_OR_UNPARSABLE Parse #" + ii + " with score " + score);
        }
      } else {
        if (inXml) {
          pw.print("<s");
          if (id != null && ! "".equals(id)) {
            pw.print(" id=\"" + XMLUtils.escapeXML(id) + "\"");
          }
          pw.print(" n=\"" + ii + "\"");
          pw.print(" score=\"" + score + "\"");
          pw.println(">");
        } else {
          pw.println("# Parse " + ii + " with score " + score);
        }
        printTreeInternal(t, pw, inXml);
        if (inXml) {
          pw.println("</s>");
          pw.println();
        }
      }
    }
  }


  /** Print the internal part of a tree having already identified it.
   *  The ID and outer XML element is printed wrapping this method, but none
   *  of the internal content.
   *
   * @param t The tree to print. Now known to be non-null
   * @param pw Where to print it to
   * @param inXml Whether to use XML style printing
   */
  private void printTreeInternal(final Tree t, final PrintWriter pw, final boolean inXml) {
    Tree outputTree = t;
    if (formats.containsKey("words")) {
      if (inXml) {
        Sentence<HasWord> sentUnstemmed = outputTree.yield();
        pw.println("  <words>");
        int i = 1;
        for (HasWord w : sentUnstemmed) {
          pw.println("    <word ind=\"" + i + "\">" + XMLUtils.escapeXML(w.word()) + "</word>");
          i++;
        }
        pw.println("  </words>");
      } else {
        pw.println(outputTree.yield().toString(false));
        pw.println();
      }
    }

    if (propertyToBoolean(options, "removeTopBracket")) {
      String s = outputTree.label().value();
      if (tlp.isStartSymbol(s)) {
        if (outputTree.isUnaryRewrite()) {
          outputTree = outputTree.firstChild();
        } else {
          // It's not quite clear what to do if the tree isn't unary at the top
          // but we then don't strip the ROOT symbol, since that seems closer
          // than losing part of the tree altogether....
          System.err.println("TreePrint: can't remove top bracket: not unary");
        }
      }
      // Note that TreePrint is also called on dependency trees that have
      // a word as the root node, and so we don't error if there isn't
      // the root symbol at the top; rather we silently assume that this
      // is a dependency tree!!
    }
    if (stem) {
      stemmer.visitTree(outputTree);
    }
    if (lexicalize) {
      Function<Tree, Tree> a =
        TreeFunctions.getLabeledTreeToCategoryWordTagTreeFunction();
      outputTree = a.apply(outputTree);
      outputTree.percolateHeads(hf);
    }

    if (formats.containsKey("collocations")) {
      /* Reflection to check that a Wordnet connection exists.  Otherwise
       * we print an error and exit.
       */
      if (wnc == null) {
        try {
          Class cl = Class.forName("edu.stanford.nlp.trees.WordNetInstance");
          wnc = (WordNetConnection) cl.newInstance();
        } catch (Exception e) {
          e.printStackTrace();
          System.err.println("Couldn't open WordNet Connection.  Aborting collocation detection.");
          wnc = null;
        }
      }
      if (wnc != null) {
        CollocationFinder cf = new CollocationFinder(outputTree, wnc, hf);
        outputTree = cf.getMangledTree();
      } else {
        System.err.println("ERROR: WordNetConnection unavailable for collocations.");
      }
    }

    if (!lexicalize) { // delexicalize the output tree
      Function<Tree, Tree> a =
        TreeFunctions.getLabeledTreeToStringLabeledTreeFunction();
      outputTree = a.apply(outputTree);
    }

    Tree outputPSTree = outputTree;  // variant with head-marking, translations

    if (markHeadNodes) {
      outputPSTree = markHeadNodes(outputPSTree);
    }

    if (transChinese) {
      TreeTransformer tt = new TreeTransformer() {
        public Tree transformTree(Tree t) {
          t = t.deepCopy();
          for (Tree subtree : t) {
            if (subtree.isLeaf()) {
              Label oldLabel = subtree.label();
              String translation = ChineseEnglishWordMap.getInstance().getFirstTranslation(oldLabel.value());
              if (translation == null) translation = "[UNK]";
              Label newLabel = new StringLabel(oldLabel.value()+":"+translation);
              subtree.setLabel(newLabel);
            }
          }
          return t;
        }
      };
      outputPSTree = tt.transformTree(outputPSTree);
    }

    if (propertyToBoolean(options, "xml")) {
      if (formats.containsKey("wordsAndTags")) {
        Sentence<TaggedWord> sent = outputTree.taggedYield();
        pw.println("  <words pos=\"true\">");
        int i = 1;
        for (TaggedWord tw : sent) {
          pw.println("    <word ind=\"" + i + "\" pos=\"" + XMLUtils.escapeXML(tw.tag()) + "\">" + XMLUtils.escapeXML(tw.word()) + "</word>");
          i++;
        }
        pw.println("  </words>");
      }
      if (formats.containsKey("penn")) {
        pw.println("  <tree style=\"penn\">");
        StringWriter sw = new StringWriter();
        PrintWriter psw = new PrintWriter(sw);
        outputPSTree.pennPrint(psw);
        pw.print(XMLUtils.escapeXML(sw.toString()));
        pw.println("  </tree>");
      }
      if (formats.containsKey("latexTree")) {
        pw.println("    <tree style=\"latexTrees\">");
        pw.println(".[");
        StringWriter sw = new StringWriter();
        PrintWriter psw = new PrintWriter(sw);
        outputTree.indentedListPrint(psw,false);
        pw.print(XMLUtils.escapeXML(sw.toString()));
        pw.println(".]");
        pw.println("  </tree>");
      }
      if (formats.containsKey("dependencies")) {
        Tree indexedTree = outputTree.deeperCopy(outputTree.treeFactory(),
                                                 CyclicCoreLabel.factory());
        indexedTree.indexLeaves();
        Set<Dependency> depsSet = indexedTree.mapDependencies(dependencyFilter, hf);
        List<Dependency> sortedDeps = new ArrayList<Dependency>(depsSet);
        Collections.sort(sortedDeps, Dependencies.dependencyIndexComparator());
        pw.println("<dependencies style=\"untyped\">");
        for (Dependency d : sortedDeps) {
          pw.println(d.toString("xml"));
        }
        pw.println("</dependencies>");
      }
      if (formats.containsKey("typedDependencies")) {
        GrammaticalStructure gs = gsf.newGrammaticalStructure(outputTree);
        print(gs.typedDependencies(), "xml", pw);
      }
      if (formats.containsKey("typedDependenciesCollapsed")) {
        GrammaticalStructure gs = gsf.newGrammaticalStructure(outputTree);
        print(gs.typedDependenciesCollapsed(), "xml", pw);
      }
      // This makes parser require jgrapht.  Bad
      // if (formats.containsKey("semanticGraph")) {
      //  SemanticGraph sg = SemanticGraph.makeFromTree(outputTree, true, false, false, null);
      //  pw.println(sg.toFormattedString());
      // }
    } else {
      // non-XML printing
      if (formats.containsKey("wordsAndTags")) {
        pw.println(outputTree.taggedYield().toString(false));
        pw.println();
      }
      if (formats.containsKey("oneline")) {
        pw.println(outputTree.toString());
      }
      if (formats.containsKey("penn")) {
        outputPSTree.pennPrint(pw);
        pw.println();
      }
      if (formats.containsKey(rootLabelOnlyFormat)) {
        pw.println(outputTree.label());
      }
      if (formats.containsKey("latexTree")) {
        pw.println(".[");
        outputTree.indentedListPrint(pw,false);
        pw.println(".]");
      }
      if (formats.containsKey("dependencies")) {
        Tree indexedTree = outputTree.deeperCopy(outputTree.treeFactory(),
                                                 CyclicCoreLabel.factory());
        indexedTree.indexLeaves();
        Set<Dependency> depsSet = indexedTree.mapDependencies(dependencyFilter, hf);
        List<Dependency> sortedDeps = new ArrayList<Dependency>(depsSet);
        Collections.sort(sortedDeps, Dependencies.dependencyIndexComparator());
        for (Dependency d : sortedDeps) {
          pw.println(d.toString("predicate"));
        }
        pw.println();
      }
      if (formats.containsKey("typedDependencies")) {
        // pw.println(new GrammaticalStructure(outputTree).typedDependencies());
        GrammaticalStructure gs = gsf.newGrammaticalStructure(outputTree);
        // pw.print(gs);
        print(gs.typedDependencies(), pw);
        pw.println();
      }
      if (formats.containsKey("typedDependenciesCollapsed")) {
        GrammaticalStructure gs = gsf.newGrammaticalStructure(outputTree);
        // pw.print(gs);
        print(gs.typedDependenciesCollapsed(), pw);
      }
      // This makes parser require jgrapht.  Bad
      // if (formats.containsKey("semanticGraph")) {
      //  SemanticGraph sg = SemanticGraph.makeFromTree(outputTree, true, false, false, null);
      //  pw.println(sg.toFormattedString());
      // }
    }
  }


  public void printHeader(PrintWriter pw, String charset) {
    if (propertyToBoolean(options, "xml")) {
      pw.println("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>");
      pw.println("<corpus>");
    }
  }


  public void printFooter(PrintWriter pw) {
    if (propertyToBoolean(options, "xml")) {
      pw.println("</corpus>");
    }
  }


  /**
   * Sets whether or not to stem the Trees using Morphology.
   * This option is currently only for English.
   * @param stem Whether to stem each word
   */
  public void setStem(boolean stem) {
    this.stem = stem;
    if (stem) {
      stemmer = new WordStemmer();
    } else {
      stemmer = null;
    }
  }

  /**
   * Set the headfinder to be used for lexicalizing trees.  Relevant
   * if the output is to be lexicalized.  By default,
   * <code>CollinsHeadFinder</code> is used.
   * @param hf The HeadFinder to use
   */
  public void setHeadFinder(HeadFinder hf) {
    this.hf = hf;
  }

  public HeadFinder getHeadFinder() {
    return hf;
  }


  /**
   * Sets the default print writer for printing trees with the instance.
   * Unless this is set, the default is <code>System.out</code>.
   * @param pw The PrintWriter to use for output by default
   */
  public void setPrintWriter(PrintWriter pw) {
    this.pw = pw;
  }

  public PrintWriter getPrintWriter() {
    return pw;
  }

  public Tree markHeadNodes(Tree t) {
    return markHeadNodes(t, null);
  }

  private Tree markHeadNodes(Tree t, Tree head) {
    if (t.isLeaf()) {
      return t; // don't worry about head-marking leaves
    }
    Label newLabel;
    if (t == head) {
      newLabel = headMark(t.label());
    } else {
      newLabel = t.label();
    }
    Tree newHead = hf.determineHead(t);
    return t.treeFactory().newTreeNode(newLabel, Arrays.asList(headMarkChildren(t, newHead)));
  }

  private static Label headMark(Label l) {
    Label l1 = l.labelFactory().newLabel(l);
    l1.setValue(l1.value() + headMark);
    return l1;
  }

  private Tree[] headMarkChildren(Tree t, Tree head) {
    Tree[] kids = t.children();
    Tree[] newKids = new Tree[kids.length];
    for (int i = 0, n = kids.length; i < n; i++) {
      newKids[i] = markHeadNodes(kids[i], head);
    }
    return newKids;
  }

  /** This provides a simple main method for calling TreePrint.
   *  Flags supported are:
   *  <ol>
   *  <li> -format format (like -outputFormat of parser, default "penn")
   *  <li> -options options (like -outputFormatOptions of parser, default "")
   *  <li> -tLPP class (the TreebankLanguageParserParams, default "edu.stanford.nlp.parser.lexparser.EnglishTreebankParserParams")
   *  <li> -hf class (the HeadFinder, default, the one in the class specified by -tLPP)
   *  <li> -useTLPPTreeReader (use the diskTreebank() and hence the TreeReaderFactory inside
   *       the -tLPP class; otherwise a PennTreeReader with no normalization is used)
   *  </ol>
   *  The single argument should be a file containing Trees in the format that is either
   *  Penn Treebank s-expressions or as specified by -useTLPPTreeReader and the -tLPP class,
   *  or if there is no such argument, trees are read from stdin and the program runs as a
   *  filter.
   *  @param args Command line arguments, as above.
   */
  public static void main(String[] args) {
    String format = "penn";
    String options = "";
    String tlppName = "edu.stanford.nlp.parser.lexparser.EnglishTreebankParserParams";
    String hfName = null;
    Map<String,Integer> flagMap = new HashMap<String,Integer>();
    flagMap.put("-format", 1);
    flagMap.put("-options", 1);
    flagMap.put("-tLPP", 1);
    flagMap.put("-hf", 1);
    Map<String,String[]> argsMap = StringUtils.argsToMap(args,flagMap);
    args = argsMap.get(null);
    if(argsMap.keySet().contains("-format")) {
      format = argsMap.get("-format")[0];
    }
    if(argsMap.keySet().contains("-options")) {
      options = argsMap.get("-options")[0];
    }
    if (argsMap.keySet().contains("-tLPP")) {
      tlppName = argsMap.get("-tLPP")[0];
    }
    if (argsMap.keySet().contains("-hf")) {
      hfName = argsMap.get("-hf")[0];
    }
    TreebankLangParserParams tlpp;
    try {
      tlpp = (TreebankLangParserParams) Class.forName(tlppName).newInstance();
    } catch (Exception e) {
      e.printStackTrace();
      return;
    }
    HeadFinder hf;
    if (hfName != null) {
      try {
        hf = (HeadFinder) Class.forName(hfName).newInstance();
      } catch (Exception e) {
        e.printStackTrace();
        return;
      }
    } else {
      hf = tlpp.headFinder();
    }
    TreePrint print = new TreePrint(format, options, tlpp.treebankLanguagePack(), (hf == null) ? tlpp.headFinder(): hf);
    Iterator<Tree> i; // initialized below
    if (args.length > 0) {
      Treebank trees; // initialized below
      if (argsMap.keySet().contains("-useTLPPTreeReader")) {
        trees = tlpp.diskTreebank();
      } else {
        trees = new DiskTreebank(new TreeReaderFactory() {
            public TreeReader newTreeReader(Reader in) {
              return new PennTreeReader(in, new LabeledScoredTreeFactory(new StringLabelFactory()), new TreeNormalizer());
            }
          });
      }
      trees.loadPath(args[0]);
      i = trees.iterator();
    } else {
      i = tlpp.treeTokenizerFactory().getIterator(new BufferedReader(new InputStreamReader(System.in)));
    }
    while(i.hasNext()) {
      print.printTree(i.next());
    }
  }

  /**
   * NO OUTSIDE USE
   * Returns a String representation of the result of this set of
   * typed dependencies in a user-specified format.
   * Currently, three formats are supported:
   * <dl>
   * <dt>"plain"</dt>
   * <dd>(Default.)  Formats the dependencies as logical relations,
   * as exemplified by the following:
   * <pre>
   *  nsubj(died-1, Sam-0)
   *  tmod(died-1, today-2)
   *  </pre>
   * </dd>
   * <dt>"readable"</dt>
   * <dd>Formats the dependencies as a table with columns
   * <code>dependent</code>, <code>relation</code>, and
   * <code>governor</code>, as exemplified by the following:
   * <pre>
   *  Sam-0               nsubj               died-1
   *  today-2             tmod                died-1
   *  </pre>
   * </dd>
   * <dt>"xml"</dt>
   * <dd>Formats the dependencies as XML, as exemplified by the following:
   * <pre>
   *  &lt;dependencies&gt;
   *    &lt;dep type="nsubj"&gt;
   *      &lt;governor idx="1"&gt;died&lt;/governor&gt;
   *      &lt;dependent idx="0"&gt;Sam&lt;/dependent&gt;
   *    &lt;/dep&gt;
   *    &lt;dep type="tmod"&gt;
   *      &lt;governor idx="1"&gt;died&lt;/governor&gt;
   *      &lt;dependent idx="2"&gt;today&lt;/dependent&gt;
   *    &lt;/dep&gt;
   *  &lt;/dependencies&gt;
   *  </pre>
   * </dd>
   * </dl>
   *
   * @param dependencies The TypedDependencies to print
   * @param format a <code>String</code> specifying the desired format
   * @return a <code>String</code> representation of the typed
   *         dependencies in this <code>GrammaticalStructure</code>
   */
  private static String toString(Collection<TypedDependency> dependencies, String format) {
    if (format != null && format.equals("xml")) {
      return toXMLString(dependencies);
    } else if (format != null && format.equals("readable")) {
      return toReadableString(dependencies);
    } else {
      return toString(dependencies);
    }
  }

  /**
   * NO OUTSIDE USE
   * Returns a String representation of this set of typed dependencies
   * as exemplified by the following:
   * <pre>
   *  tmod(died-6, today-9)
   *  nsubj(died-6, Sam-3)
   *  </pre>
   *
   * @param dependencies The TypedDependencies to print
   * @return a <code>String</code> representation of this set of
   *         typed dependencies
   */
  private static String toString(Collection<TypedDependency> dependencies) {
    StringBuilder buf = new StringBuilder();
    for (TypedDependency td : dependencies) {
      buf.append(td.toString()).append("\n");
    }
    return buf.toString();
  }

  // NO OUTSIDE USE
  private static String toReadableString(Collection<TypedDependency> dependencies) {
    StringBuilder buf = new StringBuilder();
    buf.append(String.format("%-20s%-20s%-20s%n", "dep", "reln", "gov"));
    buf.append(String.format("%-20s%-20s%-20s%n", "---", "----", "---"));
    for (TypedDependency td : dependencies) {
      buf.append(String.format("%-20s%-20s%-20s%n", td.dep(), td.reln(), td.gov()));
    }
    return buf.toString();
  }

  // NO OUTSIDE USE
  private static String toXMLString(Collection<TypedDependency> dependencies) {
    StringBuilder buf = new StringBuilder("<dependencies style=\"typed\">\n");
    for (TypedDependency td : dependencies) {
      String reln = td.reln().toString();
      String gov = td.gov().value();
      int govIdx = td.gov().index();
      String dep = td.dep().value();
      int depIdx = td.dep().index();
      // add an attribute if the node is a copy
      // (this happens in collapsing when different prepositions are conjuncts)
      String govCopy = "";
      if (td.gov().label.get(CopyAnnotation.class) != null && td.gov().label.get(CopyAnnotation.class)) {
        govCopy = " copy=\"yes\"";
      }
      String depCopy = "";
      if (td.dep().label.get(CopyAnnotation.class) != null && td.dep().label.get(CopyAnnotation.class)) {
        depCopy = " copy=\"yes\"";
      }
      buf.append("  <dep type=\"").append(XMLUtils.escapeXML(reln)).append("\">\n");
      buf.append("    <governor idx=\"").append(govIdx).append("\"").append(govCopy).append(">").append(XMLUtils.escapeXML(gov)).append("</governor>\n");
      buf.append("    <dependent idx=\"").append(depIdx).append("\"").append(depCopy).append(">").append(XMLUtils.escapeXML(dep)).append("</dependent>\n");
      buf.append("  </dep>\n");
    }
    buf.append("</dependencies>");
    return buf.toString();
  }

  /**
   * USED BY TREEPRINT AND WSD.SUPWSD.PREPROCESS
   * Prints this set of typed dependencies to the specified
   * <code>PrintWriter</code>.
   * @param dependencies The collection of TypedDependency to print
   * @param pw Where to print them
   */
  public static void print(Collection<TypedDependency> dependencies, PrintWriter pw) {
    pw.println(toString(dependencies));
  }

  /**
   * USED BY TREEPRINT
   * Prints this set of typed dependencies to the specified
   * <code>PrintWriter</code> in the specified format.
   * @param dependencies The collection of TypedDependency to print
   * @param format Where "xml" or "readable" or other
   * @param pw Where to print them
   */
  public static void print(Collection<TypedDependency> dependencies, String format, PrintWriter pw) {
    pw.println(toString(dependencies, format));
  }

}
