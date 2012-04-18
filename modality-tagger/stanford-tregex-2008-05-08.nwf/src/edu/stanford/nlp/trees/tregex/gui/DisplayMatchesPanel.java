//Tregex/Tsurgeon, DisplayMatchesPanel - a GUI for tree search and modification
//Copyright (c) 2007-2008 The Board of Trustees of
//The Leland Stanford Junior University. All Rights Reserved.

//This program is free software; you can redistribute it and/or
//modify it under the terms of the GNU General Public License
//as published by the Free Software Foundation; either version 2
//of the License, or (at your option) any later version.

//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU General Public License for more details.

//You should have received a copy of the GNU General Public License
//along with this program; if not, write to the Free Software
//Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.

//This code is a GUI interface to Tregex and Tsurgeon (which were
//written by Rogey Levy and Galen Andrew).

//For more information, bug reports, fixes, contact:
//Christopher Manning
//Dept of Computer Science, Gates 1A
//Stanford CA 94305-9010
//USA
//java-nlp-support@lists.stanford.edu
//http://www-nlp.stanford.edu/software/tregex.shtml

package edu.stanford.nlp.trees.tregex.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Point;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.TransferHandler;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.MouseInputAdapter;

import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.util.Pair;

/**
 * Class for creating the panel which shows a graphical version of the tree (as in TreeJPanel) as well
 * as the file name of the file from which the tree is from.
 * @author Anna Rafferty
 *
 */
@SuppressWarnings("serial")
public class DisplayMatchesPanel extends JPanel implements ListSelectionListener {
  private JScrollPane scroller;
  private MouseEvent firstMouseEvent = null;

  private String fontName = "";
  private int fontSize = 12;
  private Color defaultColor = Color.BLACK;
  private Color matchedColor = Color.RED;

  private static DisplayMatchesPanel instance = null;

  public static DisplayMatchesPanel getInstance() {
    if(instance == null)
      instance = new DisplayMatchesPanel();
    return instance;
  }

  private DisplayMatchesPanel() {
    //data
    JPanel spaceholder = new JPanel();
    spaceholder.setBackground(Color.white);
    scroller = new JScrollPane(spaceholder);
    this.setFocusable(true);
    this.setTransferHandler(new DisplayTransferHandler());
    MatchesPanel.getInstance().addListener(this);

    //layout
    this.setLayout(new BorderLayout());
    this.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(),""));
    this.add(scroller, BorderLayout.CENTER);
  }

  private static class DisplayTransferHandler extends TransferHandler {
    public DisplayTransferHandler() {
      super();
    }
    protected String exportString(JComponent c) {
      if(c instanceof ScrollableTreeJPanel) {
        ScrollableTreeJPanel tjp = (ScrollableTreeJPanel) c;
        return tjp.getTree().pennString();
      }

      return "";
    }

    protected Transferable createTransferable(JComponent c) {
      return new StringSelection(exportString(c));
    }

    public int getSourceActions(JComponent c) {
      return COPY_OR_MOVE;
    }
  }


  /**
   * Used to set the single tree to be displayed in this panel (which should match
   * the tregex expression)
   * @param match tree that matches the expression
   */
  public void setMatch(TreeFromFile match, List<Tree> matchedParts) {
    clearMatches();
    if(match != null)
      addMatch(match, matchedParts);
  }

  /**
   * Remove all trees from the display
   */
  public void clearMatches() {
    JPanel spaceholder = new JPanel();
    spaceholder.setBackground(Color.white);
    scroller.setViewportView(spaceholder);
    scroller.validate();
    scroller.repaint();
  }

  public class FilenameMouseInputAdapter extends MouseInputAdapter {
    JTextField textField;

    public FilenameMouseInputAdapter(JTextField textField) {
      this.textField = textField;
    }

    private boolean dragNDrop = false;
    public void mousePressed(MouseEvent e) {
      if (MatchesPanel.getInstance().isEmpty()) return;
      if(firstMouseEvent == null) {
        firstMouseEvent = e;
      }
      e.consume();
      if(((e.getModifiersEx()) & InputEvent.SHIFT_DOWN_MASK) == InputEvent.SHIFT_DOWN_MASK) {
        //shift is being held
        addHighlight(textField, firstMouseEvent, e);
      } else if(!HighlightUtils.isInHighlight(e, textField, textField.getHighlighter())) {
        textField.getHighlighter().removeAllHighlights();
        firstMouseEvent = e;
        dragNDrop = false;
        textField.repaint();
      } else {
        //in a highlight, if we drag after this, we'll be DnDing
        dragNDrop = true;
      }
    }

    private boolean addHighlight(JTextField label, MouseEvent mouseEvent1, MouseEvent mouseEvent2) {
      return HighlightUtils.addHighlight(label, mouseEvent1, mouseEvent2);
    }

    public void mouseDragged(MouseEvent e) {
      if (MatchesPanel.getInstance().isEmpty()) return;

      if (firstMouseEvent != null) {
        e.consume();
        if(dragNDrop) {
          if(textField == null)
            return;
          if(Point.distanceSq(e.getX(), e.getY(), firstMouseEvent.getX(), firstMouseEvent.getY()) > 25) {
            //do DnD
            textField.getTransferHandler().exportAsDrag((JComponent) e.getSource(), firstMouseEvent, TransferHandler.COPY);
          }
        } else {
          addHighlight(textField, firstMouseEvent, e);
        }
      }
    }
  }

  /**
   * Adds the given tree to the display without removing already
   * displayed trees
   * @param match tree to be added
   */
  private void addMatch(TreeFromFile match, List<Tree> matchedParts) {

    JPanel treeDisplay = new JPanel(new BorderLayout());
    JTextField filename = new JTextField("From file: " + match.getFilename());
    filename.setEditable(false);
    MouseInputAdapter mouseListener = new FilenameMouseInputAdapter(filename);  
    filename.addMouseListener(mouseListener);
    filename.addMouseMotionListener(mouseListener);
    treeDisplay.add(filename, BorderLayout.NORTH);
    ScrollableTreeJPanel tjp = getTreeJPanel(match.getTree(), matchedParts);
    treeDisplay.add(tjp, BorderLayout.CENTER);

    filename.setOpaque(true);
    filename.setBackground(tjp.getBackground());
    filename.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));


    scroller.setViewportView(treeDisplay);
    this.revalidate();
    this.repaint();
  }

  private ScrollableTreeJPanel getTreeJPanel(Tree t, List<Tree> matchedParts) {
    final ScrollableTreeJPanel tjp = new ScrollableTreeJPanel(SwingConstants.CENTER,SwingConstants.TOP);
    tjp.setFontName(fontName);
    tjp.setFontSize(fontSize);
    tjp.setDefaultColor(defaultColor);
    tjp.setMatchedColor(matchedColor);
    tjp.setTree(t);
    tjp.setMatchedParts(matchedParts);
    tjp.setBackground(Color.WHITE);
    tjp.setFocusable(true);
    tjp.addMouseListener(new MouseAdapter() {
      public void mouseClicked(MouseEvent e) {
        tjp.requestFocusInWindow();
      }
    });
    DisplayMouseMotionAdapter d = new DisplayMouseMotionAdapter();
    tjp.addMouseMotionListener(d);
    tjp.addMouseListener(d);
    tjp.setTransferHandler(new DisplayTransferHandler());
    InputMap imap = tjp.getInputMap();
    imap.put(KeyStroke.getKeyStroke("ctrl C"),
        TransferHandler.getCopyAction().getValue(Action.NAME));
    ActionMap map = tjp.getActionMap();
    map.put(TransferHandler.getCopyAction().getValue(Action.NAME),
        TransferHandler.getCopyAction());
    return tjp;
  }

  private class DisplayMouseMotionAdapter extends MouseInputAdapter {
    /*
     * Motion listener is based off the Java sun tutorial for DnD transfer
     */
    MouseEvent firstMouseEvent = null;

    public void mousePressed(MouseEvent e) {
      if (MatchesPanel.getInstance().isEmpty()) return;
      firstMouseEvent = e;
      e.consume();
    }

    public void mouseDragged(MouseEvent e) {
      if (MatchesPanel.getInstance().isEmpty()) return;

      if (firstMouseEvent != null) {
        e.consume();

        int dx = Math.abs(e.getX() - firstMouseEvent.getX());
        int dy = Math.abs(e.getY() - firstMouseEvent.getY());
        //Arbitrarily define a 5-pixel shift as the
        //official beginning of a drag.
        if (dx > 5 || dy > 5) {
          //This is a drag, not a click.
          JComponent c = (JComponent)e.getSource();
          //Tell the transfer handler to initiate the drag.
          TransferHandler handler = c.getTransferHandler();
          handler.exportAsDrag(c, firstMouseEvent, TransferHandler.COPY);
          firstMouseEvent = null;
        }
      }
    }
    public void mouseReleased(MouseEvent e) {
      firstMouseEvent = null;
    }
  }

  public String getFontName() {
    return fontName;
  }

  public void setFontName(String fontName) {
    this.fontName = fontName;
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

  public void valueChanged(ListSelectionEvent e) {
    Pair<TreeFromFile, List<Tree>> newMatch = MatchesPanel.getInstance().getSelectedMatch();
    if(newMatch != null)
      this.setMatch(newMatch.first(), newMatch.second());
    else {
      this.clearMatches();
    }
  }



}
