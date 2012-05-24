package edu.stanford.nlp.trees.tregex;

import edu.stanford.nlp.util.Function;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.util.Pair;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class DescriptionPattern extends TregexPattern {

  private Relation rel;
  private boolean negDesc;
  private Pattern descPattern;
  private String stringDesc;
  private Object name;
  private boolean isLink;
  private TregexPattern child;
  private List<Pair<Integer,String>> variableGroups; // specifies the groups in a regex that are captured as matcher-global string variables

  private Function<String, String> basicCatFunction;

  public DescriptionPattern(Relation rel, boolean negDesc, String desc, Object name, boolean useBasicCat) {
    this(rel,negDesc,desc,name,useBasicCat, new ArrayList<Pair<Integer,String>>(0));
  }

  public DescriptionPattern(Relation rel, boolean negDesc, String desc, Object name, boolean useBasicCat, List<Pair<Integer,String>> varGroups) {
    this(rel,negDesc,desc,name,useBasicCat, false,varGroups);
  }

  public DescriptionPattern(Relation rel, boolean negDesc, String desc, Object name, boolean useBasicCat, boolean ignoreCase, List<Pair<Integer,String>> variableGroups) {
    this.rel = rel;
    this.negDesc = negDesc;
    if (desc != null) {
      stringDesc = desc;
      if (desc.equals("__")) {
        descPattern = Pattern.compile(".*");
      } else if (desc.matches("/.*/")) {
        descPattern = Pattern.compile(desc.substring(1, desc.length() - 1));
      } else { // raw description
        descPattern = Pattern.compile("^(" + desc + ")$");
      }
    } else {
      assert name != null;
      stringDesc = " ";
      descPattern = null;
    }
    this.name = name;
    this.child = null;
    this.basicCatFunction = (useBasicCat ? currentBasicCatFunction : null);
    //    System.out.println("Made " + (negDesc ? "negated " : "") + "DescNode with " + desc);
    this.variableGroups = variableGroups;
  }

  public void makeLink() {
    isLink = true;
  }

  public String localString() {
    return rel.toString() + ' ' + (negDesc ? "!" : "") + (basicCatFunction != null ? "@" : "") + stringDesc + (name == null ? "" : '=' + name.toString());
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    if (isNegated()) {
      sb.append('!');
    }
    if (isOptional()) {
      sb.append('?');
    }
    sb.append(rel.toString());
    sb.append(' ');
    if (child != null) {
      sb.append('(');
    }
    if (negDesc) {
      sb.append('!');
    }
    if (basicCatFunction != null) {
      sb.append("@");
    }
    sb.append(stringDesc);
    if (name != null) {
      sb.append("=").append(name.toString());
    }
    sb.append(' ');
    if (child != null) {
      sb.append(child.toString());
      sb.append(')');
    }
    return sb.toString();
  }

  public void setChild(TregexPattern n) {
    child = n;
  }

  public List<TregexPattern> getChildren() {
    if (child == null) {
      return Collections.emptyList();
    } else {
      return Collections.singletonList(child);
    }
  }

  public TregexMatcher matcher(Tree root, Tree tree, Map<Object, Tree> namesToNodes, VariableStrings variableStrings) {
    return new DescriptionMatcher(this, root, tree, namesToNodes,variableStrings);
  }

  private static class DescriptionMatcher extends TregexMatcher {
    private Iterator treeNodeMatchCandidateIterator;
    private final DescriptionPattern myNode;
    private TregexMatcher childMatcher; // a DescriptionMatcher only has a single child; if it is the left side of multiple relations, a CoordinationMatcher is used.
    private Tree nextTreeNodeMatchCandidate; // the Tree node that this DescriptionMatcher node is trying to match on.
    private boolean finished = false; // when finished = true, it means I have exhausted my potential tree node match candidates.
    private boolean matchedOnce = false;
    private boolean committedVariables = false;

    // universal: childMatcher is null if and only if
    // myNode.child == null OR resetChild has never been called

    public DescriptionMatcher(DescriptionPattern n, Tree root, Tree tree, Map<Object, Tree> namesToNodes, VariableStrings variableStrings) {
      super(root, tree, namesToNodes,variableStrings);
      myNode = n;
      resetChildIter();
    }

    void resetChildIter() {
      treeNodeMatchCandidateIterator = myNode.rel.searchNodeIterator(tree, root);
      finished = false;
      nextTreeNodeMatchCandidate = null;
    }

    private void resetChild() {
      if (childMatcher == null) {
        if (myNode.child == null) {
          matchedOnce = false;
        } else {
          childMatcher = myNode.child.matcher(root, nextTreeNodeMatchCandidate, namesToNodes,variableStrings);
        }
      } else {
        childMatcher.resetChildIter(nextTreeNodeMatchCandidate);
      }
    }

    /* goes to the next node in the tree that is a successful match to my description pattern */
    // when finished = false; break; is called, it means I successfully matched.
    private void goToNextTreeNodeMatch() {
      decommitVariableGroups(); // make sure variable groups are free.
      finished = true;
      Matcher m = null;
      while (treeNodeMatchCandidateIterator.hasNext()) {
        nextTreeNodeMatchCandidate = (Tree) treeNodeMatchCandidateIterator.next();
        if (myNode.descPattern == null) {
          // this is a backreference or link
          if (myNode.isLink) {
            Tree otherTree = namesToNodes.get(myNode.name);
            if (otherTree != null) {
              String otherValue = myNode.basicCatFunction == null ? otherTree.value() : myNode.basicCatFunction.apply(otherTree.value());
              String myValue = myNode.basicCatFunction == null ? nextTreeNodeMatchCandidate.value() : myNode.basicCatFunction.apply(nextTreeNodeMatchCandidate.value());
              if (otherValue.equals(myValue)) {
                finished = false;
                break;
              }
            }
          } else if (namesToNodes.get(myNode.name) == nextTreeNodeMatchCandidate) {
            finished = false;
            break;
          }
        } else { // try to match the description pattern.
          // cdm: Nov 2006: Check for null label, just make found false
          // String value = (myNode.basicCatFunction == null ? nextTreeNodeMatchCandidate.value() : myNode.basicCatFunction.apply(nextTreeNodeMatchCandidate.value()));
          // m = myNode.descPattern.matcher(value);
          // boolean found = m.find();
          boolean found;
          String value = nextTreeNodeMatchCandidate.value();
          if (value == null) {
            found = false;
          } else {
            if (myNode.basicCatFunction != null) {
              value = myNode.basicCatFunction.apply(value);
            }
            m = myNode.descPattern.matcher(value);
            found = m.find();
          }
          if (found) {
            for (Pair<Integer,String> varGroup : myNode.variableGroups) { // if variables have been captured from a regex, they must match any previous matchings
              String thisVariable = varGroup.second();
              String thisVarString = variableStrings.getString(thisVariable);
              if (thisVarString != null && ! thisVarString.equals(m.group(varGroup.first()))) {  // failed to match a variable
                found = false;
                break;
              }
            }
          }
          if (found != myNode.negDesc) {
            finished = false;
            break;
          }
        }
      }
      if (!finished) { // I successfully matched.
        resetChild(); // reset my unique TregexMatcher child based on the Tree node I successfully matched at.
        if (myNode.name != null) {
          // note: have to fill in the map as we go for backreferencing
          namesToNodes.put(myNode.name, nextTreeNodeMatchCandidate);
        }
        commitVariableGroups(m); // commit my variable groups.
      }
      // finished is false exiting this if and only if nextChild exists
      // and has a label or backreference that matches
      // (also it will just have been reset)
    }

    private void commitVariableGroups(Matcher m) {
      committedVariables = true; // commit all my variable groups.
      for(Pair<Integer,String> varGroup : myNode.variableGroups) {
        String thisVarString = m.group(varGroup.first());
        variableStrings.setVar(varGroup.second(),thisVarString);
      }
    }

    private void decommitVariableGroups() {
      if(committedVariables)
        for(Pair<Integer,String> varGroup : myNode.variableGroups) {
          variableStrings.unsetVar(varGroup.second());
        }
      committedVariables = false;
    }


    /* tries to match the unique child of the DescriptionPattern node to a Tree node.  Returns "true" if succeeds.*/
    private boolean matchChild() {
      // entering here (given that it's called only once in matches())
      // we know finished is false, and either nextChild == null
      // (meaning goToNextChild has not been called) or nextChild exists
      // and has a label or backreference that matches
      if (nextTreeNodeMatchCandidate == null) {  // I haven't been initialized yet, so my child certainly can't be matched yet.
        return false;
      }
      if (childMatcher == null) {
        if (!matchedOnce) {
          matchedOnce = true;
          return true;
        }
        return false;
      }
      return childMatcher.matches();
    }

    // find the next local match
    public boolean matches() {
      // this is necessary so that a negated/optional node matches only once
      if (finished) {
        return false;
      }
      while (!finished) {
        if (matchChild()) {
          if (myNode.isNegated()) {
            // negated node only has to fail once
            finished = true;
            return false; // cannot be optional and negated
          } else {
            if (myNode.isOptional()) {
              finished = true;
            }
            return true;
          }
        } else {
          goToNextTreeNodeMatch();
        }
      }
      if (myNode.isNegated()) { // couldn't match my relation/pattern, so succeeded!
        return true;
      } else { // couldn't match my relation/pattern, so failed!
        nextTreeNodeMatchCandidate = null;
        if (myNode.name != null) {
          namesToNodes.remove(myNode.name);
        }
        // didn't match, but return true anyway if optional
        return myNode.isOptional();
      }
    }

    public Tree getMatch() {
      return nextTreeNodeMatchCandidate;
    }

  } // end class DescriptionMatcher

  private static final long serialVersionUID = 1179819056757295757L;

}
