package edu.stanford.nlp.trees.tregex.gui;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import edu.stanford.nlp.io.NumberRangesFileFilter;
import edu.stanford.nlp.trees.DiskTreebank;
import edu.stanford.nlp.trees.TreeReaderFactory;
import edu.stanford.nlp.trees.Treebank;
import edu.stanford.nlp.trees.tregex.TreeMatcher;
import edu.stanford.nlp.trees.tregex.gui.FileTreeNode.FileTreeNodeListener;
import edu.stanford.nlp.trees.tregex.gui.TregexGUI.FilterType;

/**
 * Component for managing the data for files containing trees.
 * @author Anna Rafferty
 *
 */
@SuppressWarnings("serial")
public class FileTreeModel extends DefaultTreeModel implements FileTreeNodeListener {

  List<TreeModelListener> listeners;
  FileTreeNode root = null;
  HashMap<FileTreeNode, List<FileTreeNode>> treeStructure;
  public static final String DEFAULT_ENCODING = "UTF-8";
  public static final String DEFAULT_CHINESE_ENCODING = "GB18030";
  public static final String DEFAULT_NEGRA_ENCODING = " ISO-8859-1";
  private static String curEncoding = DEFAULT_ENCODING;
  private TreeReaderFactory trf;

  public FileTreeModel(FileTreeNode root) {
   super(root);
   this.root = root;
   root.addListener(this);
   listeners = new ArrayList<TreeModelListener>();
   treeStructure = new HashMap<FileTreeNode, List<FileTreeNode>>();
   treeStructure.put(root, new ArrayList<FileTreeNode>());

   //other data
   trf = new TreeMatcher.TRegexTreeReaderFactory();
  }

  public void addTreeModelListener(TreeModelListener l) {
    listeners.add(l);
  }

  protected void fireTreeStructureChanged(TreePath parentPath) {
    TreeModelEvent e = null;
    for (TreeModelListener l : listeners) {
      if (e == null)
        e = new TreeModelEvent(this, parentPath, null, null);
      l.treeStructureChanged(e);
    }
  }

  public FileTreeNode getChild(Object parent, int childNum) {
    List<FileTreeNode> children = treeStructure.get(parent);
    if(children == null || childNum < 0 || children.size() <= childNum)
      return null;
    else {
      return children.get(childNum);
    }
  }

  public int getChildCount(Object parent) {
    List<FileTreeNode> children = treeStructure.get(parent);
    if(children == null)
      return 0;
    else
      return children.size();
  }

  public int getIndexOfChild(Object parent, Object child) {
    if(parent == null || child == null) {
      return -1;
    }
    List<FileTreeNode> children = treeStructure.get(parent);
    if(children == null)
      return -1;
    else
      return children.indexOf(child);

  }

  public boolean isLeaf(Object node) {
    List<FileTreeNode> children = treeStructure.get(node);
    if(children == null)
      return true;
    else
      return false;
  }

  public void removeTreeModelListener(TreeModelListener l) {
    listeners.remove(l);
  }

  public void treeNodeChanged(FileTreeNode n) {
    TreePath t = new TreePath(makeTreePathArray(n));
    //System.out.println("Tree path is: " + t);
    this.fireTreeStructureChanged(t);

  }

  /**
   * Returns true if the root has no children; false otherwise
   */
  public boolean isEmpty() {
    return this.getChildCount(root) == 0;
  }

  private Object[] makeTreePathArray(FileTreeNode node) {
    List<TreeNode> path = new ArrayList<TreeNode>();
    path.add(node);
    TreeNode child = node;
    while(child != this.getRoot()) {
      child = child.getParent();
      path.add(0, child);
    }
    return path.toArray();
  }


  public FileTreeNode getRoot() {
    return root;
  }
  /**
   * Converts a list of files containing trees into a list of treebanks,
   * with one treebank for each file; this method is thread safe (may be invoked
   * from swing thread and will fork off a thread so as not to stall the gui)
   * @param files files to read in tree form
   */
  private void readTrees(List<FileTreeNode> files) {
    final List<FileTreeNode> rFiles = files;
    //System.err.println("Reading trees from file(s) " + );
    Thread t = new Thread() {
      public void run() {
        for(FileTreeNode f : rFiles) {
          readTrees(f);
        }
      }
    };
    t.start();
  }

  private void readTrees(FileTreeNode fileNode) {
    Treebank treebank = new DiskTreebank(trf, curEncoding);
    treebank.loadPath(fileNode.getFile(), null, true);
    fileNode.setTreebank(treebank);
  }

  /**
   * Forks off a new thread to load your files based on the filters you set in the interface
   * @param filters
   * @param files
   */
  public void addFileFolder(final HashMap<FilterType, String> filters, final File[] files) {
    Thread t = new Thread() {
      public void run() {
        List<FileTreeNode> newFiles = new ArrayList<FileTreeNode>();
        findLoadableFiles(filters, files, newFiles, FileTreeModel.this.getRoot());//findLoadableFiles updates newFiles
        readTrees(newFiles);
       // System.out.println("Loadable files are: " + newFiles);
        FileTreeModel.this.fireTreeStructureChanged(new TreePath(getRoot()));
      }
    };
    t.start();
  }


  private void findLoadableFiles(HashMap<FilterType, String> filters, File[] files,
       List<FileTreeNode> newFiles, FileTreeNode parent) {
    for(File f : files) {
      if(f.isDirectory()) {
        if(isLikelyInvisible(f.getName()))
          continue;
        FileTreeNode newParent = createNode(f, parent);
        treeStructure.put(newParent, new ArrayList<FileTreeNode>());
        //recursively call on all the files inside
        findLoadableFiles(filters, f.listFiles(), newFiles, newParent);
        if(!treeStructure.get(newParent).isEmpty()) {//only add non-empty directories
          List<FileTreeNode> value = treeStructure.get(parent);
          value.add(newParent);
        }
      } else {
        boolean loadFile = checkFile(filters,f);
        if(loadFile) {
          FileTreeNode newFile = addToMap(f, parent);
          newFiles.add(newFile);
          //System.out.println("Loading: " + loadFile);
        }
      }
    }
  }

  private FileTreeNode createNode(File f, FileTreeNode parent) {
    FileTreeNode newNode = new FileTreeNode(f,parent);
    newNode.addListener(this);
    return newNode;
  }

  private FileTreeNode addToMap(File f, FileTreeNode parent) {
    List<FileTreeNode> value = treeStructure.get(parent);
    if(value == null) {
      System.err.println("Something very very bad has happened; a parent was not in the tree for the given child; parent: " + parent);
    }
    FileTreeNode newNode = createNode(f, parent);
    value.add(newNode);
    return newNode;
  }

  private boolean checkFile(HashMap<FilterType, String> filters, File file) {
    String fileName = file.getName();
    if(isLikelyInvisible(fileName))
      return false;
    if(filters.containsKey(FilterType.hasExtension)) {
      String ext = filters.get(FilterType.hasExtension);
      if(!fileName.endsWith(ext)) {
        return false;
      }
    }
    if(filters.containsKey(FilterType.hasPrefix)) {
      String pre = filters.get(FilterType.hasPrefix);
      if(!fileName.startsWith(pre))
        return false;
    }
    if(filters.containsKey(FilterType.isInRange)) {
      NumberRangesFileFilter f = new NumberRangesFileFilter(filters.get(FilterType.isInRange), false);
      if(!f.accept(fileName))
        return false;
    }
    return true;
  }

  //filter files and directories that start with .
  private static boolean isLikelyInvisible(String filename) {
    return filename.startsWith(".");
  }

  public TreeReaderFactory getTRF() {
    return trf;
  }

  public void setTRF(TreeReaderFactory trf) {
    this.trf = trf;
  }

  public static String getCurEncoding() {
    return curEncoding;
  }

  public static void setCurEncoding(String curEncoding) {
    FileTreeModel.curEncoding = curEncoding;
  }


}
