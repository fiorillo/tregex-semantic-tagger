package edu.stanford.nlp.trees.tregex.gui;

import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JTextField;

import edu.stanford.nlp.trees.Tree;

/**
 * Simple utility class for storing a tree as well as the sentence the tree represents and
 * a label with the filename of the file that the tree was stored in.
 * @author Anna Rafferty
 *
 */
public class TreeFromFile {
  private String treeString;
  //private Tree t;
  private String filename;
  private String sentence = "";
  private JTextField label = null;

  public TreeFromFile(Tree t) {
    //this.t = t;
    this.treeString = t.pennString();
  }

  public TreeFromFile(Tree t, String filename) {
    this(t);
    this.filename = filename;
  }

  public String getFilename() {
    return filename;
  }

  public void setFilename(String filename) {
    this.filename = filename;
  }

  public Tree getTree() {
    try {
      return Tree.valueOf(treeString);
    } catch(Exception e) {
      return null;
    }
    
  }

  public JTextField getLabel() {
    if(label == null) {
      label = new JTextField(this.toString());
      label.setBorder(BorderFactory.createEmptyBorder());
    }
    return label;
  }

  public String toString() {
    if(sentence == "") {
      if(this.getTree() != null) {
        List<Tree> leaves = this.getTree().getLeaves();
        sentence = "";
        for(Tree leaf : leaves) {
          sentence += leaf.toString() + " ";
        }
      } else
        sentence = "* deleted *";
    } 
    return sentence;      
  }
}
