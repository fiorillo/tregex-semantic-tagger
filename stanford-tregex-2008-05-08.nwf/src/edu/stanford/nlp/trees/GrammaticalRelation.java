package edu.stanford.nlp.trees;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.trees.tregex.TregexMatcher;
import edu.stanford.nlp.trees.tregex.TregexPattern;
import edu.stanford.nlp.util.Index;
import edu.stanford.nlp.util.Pair;
import edu.stanford.nlp.util.StringUtils;


/**
 * <code>GrammaticalRelation</code> is used to define a
 * standardized, hierarchical set of grammatical relations,
 * together with patterns for identifying them in
 * parse trees.<p>
 *
 * Each <code>GrammaticalRelation</code> has:
 * <ul>
 *   <li>A <code>String</code> short name, which should be a lowercase
 *       abbreviation of some kind.</li>
 *   <li>A <code>String</code> long name, which should be descriptive.</li>
 *   <li>A parent in the <code>GrammaticalRelation</code> hierarchy.</li>
 *   <li>A {@link Pattern <code>Pattern</code>} called
 *   <code>sourcePattern</code> which matches (parent) nodes from which
 *   this <code>GrammaticalRelation</code> could hold.  (Note: this is done
 *   with the Java regex Pattern <code>matches()</code> predicate: the pattern
 *   must match the
 *   whole node name, and <code>^</code> or <code>$</code> aren't needed.)</li>
 *   <li>A list of zero or more {@link TregexPattern
 *   <code>TregexPattern</code>s} called <code>targetPatterns</code>,
 *   which describe the local tree structure which must hold between
 *   the source node and a target node for the
 *   <code>GrammaticalRelation</code> to apply. (Note <code>tregex</code>
 *   regular expressions match with the <code>find()</code> method - though
 *   literal string label descriptions that are not regular expressions must
 *   be <code>equals()</code>.)</li>
 * </ul>
 *
 * The <code>targetPatterns</code> associated
 * with a <code>GrammaticalRelation</code> are designed as follows.
 * In order to recognize a grammatical relation X holding between
 * nodes A and B in a parse tree, we want to associate with
 * <code>GrammaticalRelation</code> X a {@link TregexPattern
 * <code>TregexPattern</code>} such that:
 * <ul>
 *   <li>the root of the pattern matches A, and</li>
 *   <li>the pattern includes a special node label, "target", which matches B.</li>
 * </ul>
 * For example, for the grammatical relation <code>PREDICATE</code>
 * which holds between a clause and its primary verb phrase, we might
 * want to use the pattern <code>"S < VP=target"</code>, in which the
 * root will match a clause and the node labeled <code>"target"</code>
 * will match the verb phrase.<p>
 *
 * For a given grammatical relation, the method {@link
 * GrammaticalRelation#getRelatedNodes <code>getRelatedNodes()</code>}
 * takes a <code>Tree</code> node as an argument and attempts to
 * return other nodes which have this grammatical relation to the
 * argument node.  By default, this method operates as follows: it
 * steps through the patterns in the pattern list, trying to match
 * each pattern against the argument node, until it finds some
 * matches.  If a pattern matches, all matching nodes (that is, each
 * node which corresponds to node label "target" in some match) are
 * returned as a list; otherwise the next pattern is tried.<p>
 *
 * For some grammatical relations, we need more sophisticated logic to
 * identify related nodes.  In such cases, {@link
 * GrammaticalRelation#getRelatedNodes <code>getRelatedNodes()</code>}
 * can be overridden on a per-relation basis using anonymous subclassing.<p>
 *
 * @see GrammaticalStructure
 * @see EnglishGrammaticalRelations
 * @see edu.stanford.nlp.trees.international.pennchinese.ChineseGrammaticalRelations
 * @see EnglishGrammaticalStructure
 *
 * @author Bill MacCartney
 * @author Galen Andrew (refactoring English-specific stuff)
 */
public class GrammaticalRelation implements Comparable<GrammaticalRelation> {
  
  public static class GrammaticalRelationAnnotation implements CoreAnnotation<Set<TreeGraphNode>> {
    @SuppressWarnings("unchecked")
    public Class<Set<TreeGraphNode>> getType() {  return (Class) Set.class; }
  }
  static Index<Class<? extends GrammaticalRelationAnnotation>> annotationIndex = new Index<Class<? extends GrammaticalRelationAnnotation>>();
  static Index<GrammaticalRelation> relationIndex = new Index<GrammaticalRelation>();

  /**
   * The "governor" grammatical relation, which is the inverse of "dependent".<p>
   * <p/>
   * Example: "the red car" &rarr; <code>gov</code>(red, car)
   */
  public static final GrammaticalRelation GOVERNOR = new GrammaticalRelation("gov", "governor", null, null, StringUtils.EMPTY_STRING_ARRAY);
  public static class GovernorGRAnnotation extends GrammaticalRelationAnnotation{ };
  /**
   * The "dependent" grammatical relation, which is the inverse of "governor".<p>
   * <p/>
   * Example: "the red car" &rarr; <code>dep</code>(car, red)
   */
  public static final GrammaticalRelation DEPENDENT = new GrammaticalRelation("dep", "dependent", null, null, StringUtils.EMPTY_STRING_ARRAY);
  public static class DependendentGRAnnotation extends GrammaticalRelationAnnotation{ };
  /**
   * Dummy relation, used while collapsing relations, in English & Chinese GrammaticalStructure
   *
   */
  public static final GrammaticalRelation KILL = new GrammaticalRelation("KILL", "dummy relation kill", null, null, StringUtils.EMPTY_STRING_ARRAY);
  public static class KillGRAnnotation extends GrammaticalRelationAnnotation{ };
  static {
    annotationIndex.add(GovernorGRAnnotation.class);
    relationIndex.add(GOVERNOR);
    annotationIndex.add(DependendentGRAnnotation.class);
    relationIndex.add(DEPENDENT);
    annotationIndex.add(KillGRAnnotation.class);
    relationIndex.add(KILL);
  }
  
  
  /** Non-static stuff */
  private final String shortName;
  private final String longName;
  private GrammaticalRelation parent;
  private List<GrammaticalRelation> children = new ArrayList<GrammaticalRelation>();
  // a regexp for node values at which this relation can hold
  private Pattern sourcePattern = null;
  private List<TregexPattern> targetPatterns = new ArrayList<TregexPattern>();
  private String specific = null; // to hold the specific prep or conjunction associated with the grammatical relation

  public GrammaticalRelation(String shortName,
                      String longName,
                      GrammaticalRelation parent,
                      String sourcePattern,
                      String[] targetPatterns) {
    this.shortName = shortName;
    this.longName = longName;
    this.parent = parent;
    if (parent != null) {
      parent.addChild(this);
    }
    if (sourcePattern != null) {
      try {
        this.sourcePattern = Pattern.compile(sourcePattern);
      } catch (java.util.regex.PatternSyntaxException e) {
        throw new RuntimeException("Bad pattern: " + sourcePattern);
      }
    }
    for (String pattern : targetPatterns) {
      TregexPattern p = null;
      try {
        p = TregexPattern.compile(pattern);
      } catch (edu.stanford.nlp.trees.tregex.ParseException pe) {
        throw new RuntimeException("Bad pattern: " + pattern);
      }
      this.targetPatterns.add(p);
    }
  }

  private void addChild(GrammaticalRelation child) {
    children.add(child);
  }

  public GrammaticalRelation(String shortName, String longName,
                      GrammaticalRelation parent,
                      String sourcePattern,
                      String[] targetPatterns, String specificString) {
    this(shortName, longName, parent, sourcePattern, targetPatterns);
    this.specific = specificString;
  }

  public String getSpecific() {
    return specific;
  }

  /** Given a <code>Tree</code> node <code>t</code>, attempts to
   *  return a list of nodes to which node <code>t</code> has this
   *  grammatical relation.
   */
  public Collection<Tree> getRelatedNodes(Tree t, Tree root) {
    Set<Tree> nodeList = new LinkedHashSet<Tree>();
    for (TregexPattern p : targetPatterns) {    // cdm: I deleted: && nodeList.isEmpty()
      if (root.value() == null) {
	      root.setValue("ROOT");
      }
      TregexMatcher m = p.matcher(root);
      while (m.find()) {
        if (m.getMatch() == t) {
          nodeList.add(m.getNode("target"));
          //System.out.println("found " + this + "(" + t + ", " + m.getNode("target") + ")");
        }
      }
    }
    return nodeList;
  }

  /** Returns <code>true</code> iff the value of <code>Tree</code>
   *  node <code>t</code> matches the <code>sourcePattern</code> for
   *  this <code>GrammaticalRelation</code>, indicating that this
   *  <code>GrammaticalRelation</code> is one that could hold between
   *  <code>Tree</code> node <code>t</code> and some other node.
   */
  public boolean isApplicable(Tree t) {
      if(t.value() != null) {
	return (sourcePattern != null) && sourcePattern.matcher(t.value()).matches();}
      else return false;
  }

  public boolean isAncestor(GrammaticalRelation gr) {
    while (gr != null) {
      if (this == gr) { return true; }
      gr = gr.parent;
    }
    return false;
  }

  /**
   * Returns short name (abbreviation) for this
   * <code>GrammaticalRelation</code>.
   */
  public String toString() {
    if(specific == null){
      return shortName;
    } else {
      return shortName + "_" + specific;
    }
  }

  /**
   * Returns the parent of this <code>GrammaticalRelation</code>.
   */
  public GrammaticalRelation parent() {
    return parent;
  }

  public boolean equals(GrammaticalRelation gr) {
    if(this.shortName.equals(gr.shortName) && this.specific == gr.specific) {
      return true;
    }
    else return false;
  }

  /**
   * Returns a <code>String</code> representation of this
   * <code>GrammaticalRelation</code> and the hierarchy below
   * it, with one node per line, indented according to level.
   *
   * @return <code>String</code> representation of this
   *         <code>GrammaticalRelation</code>
   */
  public String toPrettyString() {
    StringBuilder buf = new StringBuilder("\n");
    toPrettyString(0, buf);
    return buf.toString();
  }


  /**
   * Returns the GrammaticalRelation having the given string
   * representation (e.g. "nsubj"), or null if no such is found.
   *
   * @param s The short name of the GrammaticalRelation
   * @return The GrammaticalRelation with that name
   */
  public static GrammaticalRelation valueOf(String s, List<GrammaticalRelation> values) {
    for (GrammaticalRelation reln : values) {
      if (reln.toString().equals(s)) return reln;
    }

    return null;
  }

  /**
   * Returns a <code>String</code> representation of this
   * <code>GrammaticalRelation</code> and the hierarchy below
   * it, with one node per line, indented according to
   * <code>indentLevel</code>.
   *
   * @param indentLevel how many levels to indent (0 for root node)
   *
   */
  private void toPrettyString(int indentLevel, StringBuilder buf) {
    for (int i = 0; i < indentLevel; i++) {
      buf.append("  ");
    }
    buf.append(shortName).append(": ").append(targetPatterns);
    for (GrammaticalRelation child : children) {
      buf.append("\n");
      child.toPrettyString(indentLevel + 1, buf);
    }
  }

  public int compareTo(GrammaticalRelation o) {
    StringBuilder thisName = new StringBuilder(this.shortName);
    StringBuilder oName = new StringBuilder(o.shortName);
    thisName.append(this.specific);
    oName.append(o.specific);
    String thisN = thisName.toString();
    String oN = oName.toString();
    return thisN.compareTo(oN);
  }

  public String getLongName() {
    return longName;
  }

  public String getShortName() {
    return shortName;
  }
}
