package edu.stanford.nlp.parser.ui;

import edu.stanford.nlp.ling.StringLabelFactory;
import edu.stanford.nlp.trees.LabeledScoredTreeFactory;
import edu.stanford.nlp.trees.PennTreeReader;
import edu.stanford.nlp.trees.Tree;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.io.StringReader;

/**
 * Class for displaying a Tree.
 *
 * @author Dan Klein
 */

@SuppressWarnings("serial")
public class TreeJPanel extends JPanel {

  protected int VERTICAL_ALIGN = SwingConstants.CENTER;
  protected int HORIZONTAL_ALIGN = SwingConstants.CENTER;

  int maxFontSize = 128;
  int minFontSize = 2;

  int preferredX = 400;
  int preferredY = 300;

  double sisterSkip = 2.5;
  double parentSkip = 0.8;
  double belowLineSkip = 0.2;
  double aboveLineSkip = 0.2;

  protected Tree tree;

  public Tree getTree() {
    return tree;
  }

  public void setTree(Tree tree) {
    this.tree = tree;
    repaint();
  }

  protected String nodeToString(Tree t) {
    if (t == null) {
      return " ";
    }
    edu.stanford.nlp.ling.Label l = t.label();
    if (l == null) {
      return " ";
    }
    String str = l.value();
    if (str == null) {
      return " ";
    }
    return str;
  }

  public static class WidthResult {
    public double width = 0.0;
    public double nodeTab = 0.0;
    public double nodeCenter = 0.0;
    public double childTab = 0.0;
  }

  protected double width(Tree tree, FontMetrics fM) {
    return widthResult(tree, fM).width;
  }

  WidthResult wr = new WidthResult();

  protected WidthResult widthResult(Tree tree, FontMetrics fM) {
    if (tree == null) {
      wr.width = 0.0;
      wr.nodeTab = 0.0;
      wr.nodeCenter = 0.0;
      wr.childTab = 0.0;
      return wr;
    }
    double local = fM.stringWidth(nodeToString(tree));
    if (tree.isLeaf()) {
      wr.width = local;
      wr.nodeTab = 0.0;
      wr.nodeCenter = local / 2.0;
      wr.childTab = 0.0;
      return wr;
    }
    double sub = 0.0;
    double nodeCenter = 0.0;
    //double childTab = 0.0;
    for (int i = 0; i < tree.children().length; i++) {
      WidthResult subWR = widthResult(tree.children()[i], fM);
      if (i == 0) {
        nodeCenter += (sub + subWR.nodeCenter) / 2.0;
      }
      if (i == tree.children().length - 1) {
        nodeCenter += (sub + subWR.nodeCenter) / 2.0;
      }
      sub += subWR.width;
      if (i < tree.children().length - 1) {
        sub += sisterSkip * fM.stringWidth(" ");
      }
    }
    double localLeft = local / 2.0;
    double subLeft = nodeCenter;
    double totalLeft = Math.max(localLeft, subLeft);
    double localRight = local / 2.0;
    double subRight = sub - nodeCenter;
    double totalRight = Math.max(localRight, subRight);
    wr.width = totalLeft + totalRight;
    wr.childTab = totalLeft - subLeft;
    wr.nodeTab = totalLeft - localLeft;
    wr.nodeCenter = nodeCenter + wr.childTab;
    return wr;
  }

  protected double height(Tree tree, FontMetrics fM) {
    if (tree == null) {
      return 0.0;
    }
    double depth = tree.depth();
    return fM.getHeight() * (1.0 + depth * (1.0 + parentSkip + aboveLineSkip + belowLineSkip));
  }
  
  protected void superPaint(Graphics g) {
    super.paintComponent(g);
  }

  protected FontMetrics pickFont(Graphics2D g2, Tree tree, Dimension space) {
    Font font = g2.getFont();
    String name = font.getName();
    int style = font.getStyle();

    for (int size = maxFontSize; size > minFontSize; size--) {
      font = new Font(name, style, size);
      g2.setFont(font);
      FontMetrics fontMetrics = g2.getFontMetrics();
      if (height(tree, fontMetrics) > space.getHeight()) {
        continue;
      }
      if (width(tree, fontMetrics) > space.getWidth()) {
        continue;
      }
      //System.out.println("Chose: "+size+" for space: "+space.getWidth());
      return fontMetrics;
    }
    font = new Font(name, style, minFontSize);
    g2.setFont(font);
    return g2.getFontMetrics();
  }

  protected double paintTree(Tree t, Point2D start, Graphics2D g2, FontMetrics fM) {
    if (t == null) {
      return 0.0;
    }
    String nodeStr = nodeToString(t);
    double nodeWidth = fM.stringWidth(nodeStr);
    double nodeHeight = fM.getHeight();
    double nodeAscent = fM.getAscent();
    WidthResult wr = widthResult(t, fM);
    double treeWidth = wr.width;
    double nodeTab = wr.nodeTab;
    double childTab = wr.childTab;
    double nodeCenter = wr.nodeCenter;
    //double treeHeight = height(t, fM);
    // draw root
    g2.drawString(nodeStr, (float) (nodeTab + start.getX()), (float) (start.getY() + nodeAscent));
    if (t.isLeaf()) {
      return nodeWidth;
    }
    double layerMultiplier = (1.0 + belowLineSkip + aboveLineSkip + parentSkip);
    double layerHeight = nodeHeight * layerMultiplier;
    double childStartX = start.getX() + childTab;
    double childStartY = start.getY() + layerHeight;
    double lineStartX = start.getX() + nodeCenter;
    double lineStartY = start.getY() + nodeHeight * (1.0 + belowLineSkip);
    double lineEndY = lineStartY + nodeHeight * parentSkip;
    // recursively draw children
    for (int i = 0; i < t.children().length; i++) {
      Tree child = t.children()[i];
      double cWidth = paintTree(child, new Point2D.Double(childStartX, childStartY), g2, fM);
      // draw connectors
      wr = widthResult(child, fM);
      double lineEndX = childStartX + wr.nodeCenter;
      g2.draw(new Line2D.Double(lineStartX, lineStartY, lineEndX, lineEndY));
      childStartX += cWidth;
      if (i < t.children().length - 1) {
        childStartX += sisterSkip * fM.stringWidth(" ");
      }
    }
    return treeWidth;
  }

  public void paintComponent(Graphics g) {
    super.paintComponent(g);
    Graphics2D g2 = (Graphics2D) g;
    Dimension space = getSize();
    FontMetrics fM = pickFont(g2, tree, space);
    double width = width(tree, fM);
    double height = height(tree, fM);
    double startX = 0.0;
    double startY = 0.0;
    if (HORIZONTAL_ALIGN == SwingConstants.CENTER) {
      startX = (space.getWidth() - width) / 2.0;
    }
    if (HORIZONTAL_ALIGN == SwingConstants.RIGHT) {
      startX = space.getWidth() - width;
    }
    if (VERTICAL_ALIGN == SwingConstants.CENTER) {
      startY = (space.getHeight() - height) / 2.0;
    }
    if (VERTICAL_ALIGN == SwingConstants.BOTTOM) {
      startY = space.getHeight() - height;
    }
    paintTree(tree, new Point2D.Double(startX, startY), g2, fM);
  }

  public TreeJPanel() {
    this(SwingConstants.CENTER, SwingConstants.CENTER);
  }

  public TreeJPanel(int hAlign, int vAlign) {
    HORIZONTAL_ALIGN = hAlign;
    VERTICAL_ALIGN = vAlign;
    setPreferredSize(new Dimension(400, 300));
  }

  public void setMinFontSize(int size) {
    minFontSize = size;
  }

  public void setMaxFontSize(int size) {
    maxFontSize = size;
  }

  public static void main(String[] args) throws IOException {
    TreeJPanel tjp = new TreeJPanel();
    String ptbTreeString1 = "(ROOT (S (NP (DT This)) (VP (VBZ is) (NP (DT a) (NN test))) (. .)))";
    String ptbTreeString = "(ROOT (S (NP (NNP Interactive_Tregex)) (VP (VBZ works)) (PP (IN for) (PRP me)) (. !))))";
    if (args.length > 0) {
      ptbTreeString = args[0];
    }
    Tree tree = (new PennTreeReader(new StringReader(ptbTreeString), new LabeledScoredTreeFactory(new StringLabelFactory()))).readTree();
    tjp.setTree(tree);
    tjp.setBackground(Color.white);
    JFrame frame = new JFrame();
    frame.getContentPane().add(tjp, BorderLayout.CENTER);
    frame.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        System.exit(0);
      }
    });
    frame.pack();
    frame.setVisible(true);
    frame.setVisible(true);
  }

}
