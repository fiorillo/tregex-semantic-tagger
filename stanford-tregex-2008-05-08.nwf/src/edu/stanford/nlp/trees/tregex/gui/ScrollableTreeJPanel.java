package edu.stanford.nlp.trees.tregex.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.*;
import javax.swing.SwingConstants;

import edu.stanford.nlp.parser.ui.TreeJPanel;
import edu.stanford.nlp.trees.Tree;

/**
 * Component for displaying a tree in a JPanel that works correctly with
 * scrolling.
 * @author Anna Rafferty
 *
 */
@SuppressWarnings("serial")
public class ScrollableTreeJPanel extends TreeJPanel   {
  double sisterSkip = 2.5;
  double parentSkip = 0.8;
  double belowLineSkip = 0.2;
  double aboveLineSkip = 0.2;
  private int fontSize = 12;
  private Color defaultColor = Color.BLACK;
  private Color matchedColor = Color.RED;
  private String fontName = "";
  private Font font = null;
  private int style = 0;
  private Dimension preferredSize = null;
  private List<Tree> matchedParts = new ArrayList<Tree>();
  private FontMetrics fM = null;

  public ScrollableTreeJPanel() {
    super();
  }
  
  public ScrollableTreeJPanel(int i, int j) {
   super(i,j);
  }

  
  public void paintComponent(Graphics g) {
    superPaint(g);
    Graphics2D g2 = (Graphics2D) g;
    if(fontName == "") {
      font = g2.getFont();
      fontName = font.getName();
      style = font.getStyle();
    } 
    font = new Font(fontName, style, this.fontSize);
    g2.setFont(font);
    fM = g2.getFontMetrics();
    Dimension space = getSize();
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
    if(matchedParts != null && matchedParts.contains(tree))
      paintTree(tree, new Point2D.Double(startX, startY), g2, fM, matchedColor);
    else
      paintTree(tree, new Point2D.Double(startX, startY), g2, fM, defaultColor);

  }
  
  protected double paintTree(Tree t, Point2D start, Graphics2D g2, FontMetrics fM, Color paintColor) {
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
    Color curColor = g2.getColor();
    g2.setColor(paintColor);
    g2.drawString(nodeStr, (float) (nodeTab + start.getX()), (float) (start.getY() + nodeAscent));
    g2.setColor(curColor);
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
      double cWidth;
      if(matchedParts != null && matchedParts.contains(child))
        cWidth = paintTree(child, new Point2D.Double(childStartX, childStartY), g2, fM, matchedColor);
      else
        cWidth = paintTree(child, new Point2D.Double(childStartX, childStartY), g2, fM, defaultColor);
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

  public Dimension getPreferredSize() {
    if(preferredSize != null) {
      return preferredSize;
    }
    if(fM == null || tree == null) {
      return super.getSize();
    }
    preferredSize = new Dimension((int)width(tree,fM),(int)height(tree,fM));    
    return preferredSize;
  }

  public List<Tree> getMatchedParts() {
    return matchedParts;
  }

  public void setMatchedParts(List<Tree> matchedParts) {
    this.matchedParts = matchedParts;
  }

  public int getFontSize() {
    return fontSize;
  }

  public void setFontSize(int fontSize) {
    this.fontSize = fontSize;
  }

  public Color getDefaultColor() {
    return defaultColor;
  }

  public void setDefaultColor(Color defaultColor) {
    this.defaultColor = defaultColor;
  }

  public Color getMatchedColor() {
    return matchedColor;
  }

  public void setMatchedColor(Color matchedColor) {
    this.matchedColor = matchedColor;
  }

  public String getFontName() {
    return fontName;
  }

  public void setFontName(String fontName) {
    this.fontName = fontName;
  }
  
  
 
  
  
}
