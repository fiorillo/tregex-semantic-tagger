package edu.stanford.nlp.parser.lexparser;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import edu.stanford.nlp.stats.ClassicCounter;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.util.Numberer;
import edu.stanford.nlp.util.Pair;

public class BinaryGrammarExtractor extends AbstractTreeExtractor {

  protected Numberer stateNumberer = Numberer.getGlobalNumberer("states");
  private ClassicCounter<Rule> ruleCounter = new ClassicCounter<Rule>();
  private ClassicCounter symbolCounter = new ClassicCounter();
  private Set<Rule> binaryRules = new HashSet<Rule>();
  private Set<Rule> unaryRules = new HashSet<Rule>();

  //  protected void tallyTree(Tree t) {
  //    super.tallyTree(t);
  //    System.out.println("Tree:");
  //    t.pennPrint();
  //  }

  protected void tallyRule(Rule r, double score) {
    symbolCounter.incrementCount(stateNumberer.object(r.parent), score);
    ruleCounter.incrementCount(r, score);
    if (r.isUnary()) {
      unaryRules.add(r);
    } else {
      binaryRules.add(r);
    }
  }

  protected void tallyInternalNode(Tree lt) {
    Rule r;
    if (lt.children().length == 1) {
      r = new UnaryRule(stateNumberer.number(lt.label().value()),
                        stateNumberer.number(lt.children()[0].label().value()));
    } else {
      r = new BinaryRule(stateNumberer.number(lt.label().value()),
                         stateNumberer.number(lt.children()[0].label().value()),
                         stateNumberer.number(lt.children()[1].label().value()));
    }
    tallyRule(r, weight);
  }

  public Object formResult() {
    stateNumberer.number(Lexicon.BOUNDARY_TAG);
    BinaryGrammar bg = new BinaryGrammar(stateNumberer.total());
    UnaryGrammar ug = new UnaryGrammar(stateNumberer.total());
    // add unaries
    for (Iterator uI = unaryRules.iterator(); uI.hasNext();) {
      UnaryRule ur = (UnaryRule) uI.next();
      ur.score = (float) Math.log(ruleCounter.getCount(ur) / symbolCounter.getCount(stateNumberer.object(ur.parent)));
      if (Train.compactGrammar() >= 4) {
        ur.score = (float) ruleCounter.getCount(ur);
      }
      ug.addRule(ur);
    }
    // add binaries
    for (Iterator bI = binaryRules.iterator(); bI.hasNext();) {
      BinaryRule br = (BinaryRule) bI.next();
      br.score = (float) Math.log((ruleCounter.getCount(br) - Train.ruleDiscount) / symbolCounter.getCount(stateNumberer.object(br.parent)));
      if (Train.compactGrammar() >= 4) {
        br.score = (float) ruleCounter.getCount(br);
      }
      bg.addRule(br);
    }
    return new Pair<UnaryGrammar,BinaryGrammar>(ug, bg);
  }


} // end class BinaryGrammarExtractor
