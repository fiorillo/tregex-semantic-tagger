package edu.stanford.nlp.trees.tregex.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.MouseInputAdapter;
import javax.swing.text.Highlighter;
import javax.swing.text.Highlighter.Highlight;

import edu.stanford.nlp.swing.TooltipJList;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.util.Pair;

/**
 * Component for displaying the list of trees that match
 * the query. 
 * @author Anna Rafferty
 *
 */
@SuppressWarnings("serial")
public class MatchesPanel extends JPanel implements ListSelectionListener {
  private static MatchesPanel instance = null;
  private JList list;
  private HashMap<TreeFromFile,List<Tree>> matchedParts;
  private List<MatchesPanelListener> listeners;
  private Color highlightColor = Color.CYAN;
  private boolean showOnlyMatchedPortion = false;
  private JTextField lastSelected = null;
  private MouseEvent firstMouseEvent = null;
  private int maxMatches =1000;


  /**
   * Returns the singleton instance of the MatchesPanel
   * @return
   */
  public static MatchesPanel getInstance() {
    if(instance == null)
      instance = new MatchesPanel();
    return instance;
  }

  private MatchesPanel() {
    //data
    DefaultListModel model = new DefaultListModel();
    list = new TooltipJList(model);
    list.setCellRenderer(new MatchCellRenderer());
    list.setTransferHandler(new TreeTransferHandler());
    matchedParts = new HashMap<TreeFromFile,List<Tree>>();
    list.addListSelectionListener(this);
    MouseInputAdapter mouseListener = new MouseInputAdapter() {
      private boolean dragNDrop = false;
      public void mousePressed(MouseEvent e) {
        if (MatchesPanel.getInstance().isEmpty()) return;
        if(firstMouseEvent == null) {
          firstMouseEvent = e;
        }
        e.consume();
        TreeFromFile selectedValue = (TreeFromFile) list.getSelectedValue();
        if(selectedValue == null) return;
        JTextField label = selectedValue.getLabel();
        if(((e.getModifiersEx()) & InputEvent.SHIFT_DOWN_MASK) == InputEvent.SHIFT_DOWN_MASK) {
          //shift is being held
          addHighlight(label, firstMouseEvent, e);
        } else if(!HighlightUtils.isInHighlight(e, label, label.getHighlighter())) {
          label.getHighlighter().removeAllHighlights();
          firstMouseEvent = e;
          dragNDrop = false;
          list.repaint();
        } else {
          //in a highlight, if we drag after this, we'll be DnDing
          dragNDrop = true;
        }
      }

      private boolean addHighlight(JTextField label, MouseEvent mouseEvent1, MouseEvent mouseEvent2) {
        //Two parts: adding the highlight on the label, and scrolling the list appropriately
        //HighlightUtils handles the first part, we handle the second part here
        boolean highlightSuccessful = HighlightUtils.addHighlight(label, mouseEvent1, mouseEvent2);
        FontMetrics fm = label.getFontMetrics(label.getFont());
        int firstXpos = mouseEvent1.getX();
        int lastXpos = mouseEvent2.getX();
        int firstOffset = getCharOffset(fm, label.getText(), firstXpos);
        int lastOffset = getCharOffset(fm, label.getText(), lastXpos);
        if(lastOffset != firstOffset) {
          if(firstOffset > lastOffset) {
            int tmp = firstOffset;
            firstOffset = lastOffset;
            lastOffset = tmp;
          }
          Rectangle curVisible = list.getVisibleRect();
          if(lastXpos > curVisible.x+curVisible.width) {
            list.scrollRectToVisible(new Rectangle(new Point(lastXpos-curVisible.width, curVisible.y), curVisible.getSize()));
          } else if(lastXpos < curVisible.x) {
            list.scrollRectToVisible(new Rectangle(new Point(lastXpos, curVisible.y), curVisible.getSize()));
          }
          list.repaint();
          return highlightSuccessful;
        } else
          return false;
      }

      public void mouseDragged(MouseEvent e) {
        if (MatchesPanel.getInstance().isEmpty()) return;

        if (firstMouseEvent != null) {
          e.consume();
          JTextField label = ((TreeFromFile) list.getSelectedValue()).getLabel();
          if(dragNDrop) {
            if(label == null)
              return;
            if(Point.distanceSq(e.getX(), e.getY(), firstMouseEvent.getX(), firstMouseEvent.getY()) > 25) {
              //do DnD
              list.getTransferHandler().exportAsDrag((JComponent) e.getSource(), firstMouseEvent, TransferHandler.COPY);
            }
          } else {
            addHighlight(label, firstMouseEvent, e);
          }
        }
      }

      private int getCharOffset(FontMetrics fm, String characters, int xPos) {
        StringBuffer s = new StringBuffer();
        char[] sArray = characters.toCharArray();
        int i;
        for(i = 0; i < characters.length() && fm.stringWidth(s.toString()) < xPos; i++) {
          s.append(sArray[i]);
        }
        return i;

      }
    };



    list.addMouseMotionListener(mouseListener);
    list.addMouseListener(mouseListener);
    listeners = new ArrayList<MatchesPanelListener>();
    //layout
    this.setLayout(new BorderLayout());
    this.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(),"Matches: "));
    JScrollPane scroller = new JScrollPane(list);
    this.add(scroller, BorderLayout.CENTER);
  }

  public void removeAllMatches() {
    setMatchedParts(new HashMap<TreeFromFile, List<Tree>>());
    ((DefaultListModel) list.getModel()).removeAllElements();
    list.setSelectedIndex(-1);
    this.sendToListeners();
  }

  /**
   * Used to set the trees to be displayed in this panel (which should match
   * the tregex expression)
   * @param matches trees that match the expression
   */
  public void setMatches(List<TreeFromFile> matches, HashMap<TreeFromFile, List<Tree>> matchedParts) {
    this.removeAllMatches();
    setMatchedParts(matchedParts);
    this.setPreferredSize(this.getSize());
    if(!showOnlyMatchedPortion || matchedParts == null) {
      for(int i = 0; i < maxMatches && i < matches.size(); i++) {
        final TreeFromFile t = matches.get(i);
        ((DefaultListModel) list.getModel()).addElement(t);
      }
    } else {
      Iterator<TreeFromFile> iter = matchedParts.keySet().iterator();
      for(int i = 0; i < maxMatches && iter.hasNext(); i++) {
        final TreeFromFile t = iter.next();
        List<Tree> curMatches = matchedParts.get(t);
        for(final Tree match : curMatches) {
          ((DefaultListModel) list.getModel()).addElement(new TreeFromFile(match, t.getFilename()));
          i++;
          if(i >= maxMatches) break;
        }
      }
    }
    if(!((DefaultListModel) list.getModel()).isEmpty())
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          list.setSelectedIndex(0);
        }
      });

    this.sendToListeners();
  }



  /**
   * Get the selected tree and its corresponding matched parts
   * @return a tree that matches the tregex expression
   */
  public Pair<TreeFromFile, List<Tree>> getSelectedMatch() {
    if(!isEmpty()) {
      TreeFromFile selectedTree = (TreeFromFile) list.getSelectedValue();
      return new Pair<TreeFromFile,List<Tree>>(selectedTree, matchedParts.get(selectedTree));
    }
    else
      return null;
  }

  /**
   * Returns all currently displayed matches in string buffer, penn treebank form
   * (suitable for writing out, for instance)
   * @return StringBuffer filled with the penn treebank forms of all trees in the matches panel
   */
  public StringBuffer getMatches() {
    StringBuffer sb = new StringBuffer();
    for(int i = 0; i < list.getModel().getSize(); i++) {
      Tree t = ((TreeFromFile) list.getModel().getElementAt(i)).getTree();
      sb.append(t.pennString());
      sb.append("\n\n");
    }
    return sb;
  }

  /**
   * Returns all currently displayed sentences in string buffer, plain text form
   * @return StringBuffer filled with the plain text form of all sentences in the matches panel
   */
  public StringBuffer getMatchedSentences() {
    StringBuffer sb = new StringBuffer();
    for(int i = 0; i < list.getModel().getSize(); i++) {
      String t = ((TreeFromFile) list.getModel().getElementAt(i)).getLabel().getText();
      sb.append(t);
      sb.append("\n");
    }
    return sb;
  }

  /**
   * Determine whether any trees are in the matches panel at this time
   * @return true if trees are present
   */
  public boolean isEmpty() {
    return ((DefaultListModel) list.getModel()).isEmpty();
  }

  /**
   * Allows other panels to be updated about changes to the matches panel
   * (better abstraction)
   * @author rafferty
   *
   */
  public interface MatchesPanelListener {
    public void matchesChanged();

  }

  /**
   * Become a listener to changes in the trees the matches panel is showing
   * @param l
   */
  public void addListener(MatchesPanelListener l) {
    listeners.add(l);
  }

  /**
   * Become a listener to changes in which tree is selected
   * @param l
   */
  public void addListener(ListSelectionListener l) {
    list.addListSelectionListener(l);
  }

  private void sendToListeners() {
    for(MatchesPanelListener l : listeners)
      l.matchesChanged();
  }

  @SuppressWarnings("serial")
  private class MatchCellRenderer extends JLabel implements ListCellRenderer {

    public MatchCellRenderer() {
      setOpaque(true);
    }

    public Component getListCellRendererComponent(JList list, Object value,
        int index, boolean isSelected, boolean cellHasFocus) {
      JTextField l = ((TreeFromFile) value).getLabel();
      l.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
      l.setOpaque(true);
      if(cellHasFocus || isSelected) {
        l.setBackground(highlightColor);
      } else {
        l.setBackground(Color.WHITE);
      }
      return l;
    }

  }

  private static class TreeTransferHandler extends TransferHandler {
    public TreeTransferHandler() {
      super();
    }
    protected String exportString(JComponent c) {
      JList list = (JList)c;
      Object[] values = list.getSelectedValues();   
      StringBuffer sb = new StringBuffer();
      for (int i = 0; i < values.length; i++) {
        TreeFromFile val = (TreeFromFile) values[i];
        Highlighter h = val.getLabel().getHighlighter();
        Highlight[] highlights = h.getHighlights();
        if(highlights == null || highlights.length == 0) {
          sb.append(val.getLabel().getText());
        } else {
          //we have a highlight
          for(int j = 0; i < highlights.length; i++) {
            sb.append(val.getLabel().getText().substring(highlights[j].getStartOffset(), highlights[j].getEndOffset()));
          }
        }
      }
      return sb.toString();
    }

    protected Transferable createTransferable(JComponent c) {
      return new StringSelection(exportString(c));
    }

    public int getSourceActions(JComponent c) {
      return COPY_OR_MOVE;
    }
  }

  public HashMap<TreeFromFile, List<Tree>> getMatchedParts() {
    return matchedParts;
  }

  /**
   * Set the matched parts to the given hash/list - if null is passed in,
   * resets matchedParts to an empty hash.
   * @param matchedParts
   */
  private void setMatchedParts(HashMap<TreeFromFile, List<Tree>> matchedParts) {
    if(matchedParts == null)
      this.matchedParts = new HashMap<TreeFromFile, List<Tree>>();
    else
      this.matchedParts = matchedParts;
  }

  public Color getHighlightColor() {
    return highlightColor;
  }

  public void setHighlightColor(Color highlightColor) {
    this.highlightColor = highlightColor;
  }

  public boolean isShowOnlyMatchedPortion() {
    return showOnlyMatchedPortion;
  }

  public void setShowOnlyMatchedPortion(boolean showOnlyMatchedPortion) {
    this.showOnlyMatchedPortion = showOnlyMatchedPortion;
  }

  public void setFontName(String fontName) {
    Font curFont = this.getFont();
    Font newFont = new Font(fontName, curFont.getStyle(), curFont.getSize());
    list.setFont(newFont);
  }

  public void valueChanged(ListSelectionEvent arg0) {
    TreeFromFile t = (TreeFromFile) list.getSelectedValue();
    if(t == null) {
      lastSelected = null;
      return;
    }
    JTextField curSelected = t.getLabel();
    if(lastSelected != null) {
      if(lastSelected != curSelected) {//get rid of old highlights
        lastSelected.getHighlighter().removeAllHighlights();
        lastSelected = curSelected;
        firstMouseEvent = null;
        lastSelected.repaint();
      }

    } else
      lastSelected = curSelected;

  }

  public int getMaxMatches() {
    return maxMatches;
  }

  public void setMaxMatches(int maxMatches) {
    this.maxMatches = maxMatches;
  }
}
