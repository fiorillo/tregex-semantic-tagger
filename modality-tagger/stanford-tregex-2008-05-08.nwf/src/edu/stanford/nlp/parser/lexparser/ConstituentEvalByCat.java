package edu.stanford.nlp.parser.lexparser;

import edu.stanford.nlp.ling.Label;
import edu.stanford.nlp.trees.LabeledConstituent;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreebankLanguagePack;
import edu.stanford.nlp.stats.ClassicCounter;

import java.io.PrintWriter;
import java.util.*;
import java.text.NumberFormat;
import java.text.DecimalFormat;


public class ConstituentEvalByCat extends AbstractEval {

  public ConstituentEvalByCat(String str, TreebankLanguagePack tlp, boolean runningAverages) {
    super(str, runningAverages);
    lce = new LabeledConstituentEval(str, false, tlp);
  }

  private LabeledConstituentEval lce;

  private static final Set emptySet = new HashSet();

  Map<Label,Set<LabeledConstituent>> guessDeps = new HashMap<Label,Set<LabeledConstituent>>();
  Map<Label,Set<LabeledConstituent>> goldDeps = new HashMap<Label,Set<LabeledConstituent>>();

  ClassicCounter<Label> precisions = new ClassicCounter<Label>();
  ClassicCounter<Label> recalls = new ClassicCounter<Label>();
  ClassicCounter<Label> f1s = new ClassicCounter<Label>();

  ClassicCounter<Label> precisions2 = new ClassicCounter<Label>();
  ClassicCounter<Label> recalls2 = new ClassicCounter<Label>();
  ClassicCounter<Label> pnums2 = new ClassicCounter<Label>();
  ClassicCounter<Label> rnums2 = new ClassicCounter<Label>();

  /* We don't use this directly, it's here just to make it an AbstractEval. */
  Set makeObjects(Tree tree) {
    return lce.makeObjects(tree);
  }

  /* returns a map of categories to sets of constituents*/
  private Map<Label,Set<LabeledConstituent>> makeObjectsByCat(Tree t) {
    Map<Label,Set<LabeledConstituent>> objMap = new HashMap<Label,Set<LabeledConstituent>>();
    Set<LabeledConstituent> objSet = makeObjects(t);
    for (LabeledConstituent lc : objSet) {
      Label l = lc.label();
      if (!objMap.keySet().contains(l)) {
        objMap.put(l, new HashSet<LabeledConstituent>());
      }
      objMap.get(l).add(lc);
    }
    return objMap;
  }


  public void evaluate(Tree guess, Tree gold, PrintWriter pw) {
    guessDeps = makeObjectsByCat(guess);
    goldDeps = makeObjectsByCat(gold);

    Set<Label> cats = new HashSet<Label>();
    cats.addAll(guessDeps.keySet());
    cats.addAll(goldDeps.keySet());

    if (pw != null && runningAverages) {
      pw.println("========================================");
      pw.println("Labeled Bracketed Evaluation by Category");
      pw.println("========================================");
    }

    num += 1.0;

    for (Label cat : cats) {
      Set thisGuessDeps = guessDeps.get(cat);
      Set thisGoldDeps = goldDeps.get(cat);

      if (thisGuessDeps == null) {
        thisGuessDeps = emptySet;
      }
      if (thisGoldDeps == null) {
        thisGoldDeps = emptySet;
      }

      double currentPrecision = precision(thisGuessDeps, thisGoldDeps);
      double currentRecall = precision(thisGoldDeps, thisGuessDeps);

      double currentF1 = (currentPrecision > 0.0 && currentRecall > 0.0 ? 2.0 / (1.0 / currentPrecision + 1.0 / currentRecall) : 0.0);

      precisions.incrementCount(cat, currentPrecision);
      recalls.incrementCount(cat, currentRecall);
      f1s.incrementCount(cat, currentF1);

      precisions2.incrementCount(cat, thisGuessDeps.size() * currentPrecision);
      pnums2.incrementCount(cat, thisGuessDeps.size());

      recalls2.incrementCount(cat, thisGoldDeps.size() * currentRecall);
      rnums2.incrementCount(cat, thisGoldDeps.size());

      if (pw != null && runningAverages) {
        pw.println(cat + "\tP: " + ((int) (currentPrecision * 10000)) / 100.0 + " (sent ave " + ((int) (precisions.getCount(cat) * 10000 / num)) / 100.0 + ") (evalb " + ((int) (precisions2.getCount(cat) * 10000 / pnums2.getCount(cat))) / 100.0 + ")");
        pw.println("\tR: " + ((int) (currentRecall * 10000)) / 100.0 + " (sent ave " + ((int) (recalls.getCount(cat) * 10000 / num)) / 100.0 + ") (evalb " + ((int) (recalls2.getCount(cat) * 10000 / rnums2.getCount(cat))) / 100.0 + ")");
        double cF1 = 2.0 / (rnums2.getCount(cat) / recalls2.getCount(cat) + pnums2.getCount(cat) / precisions2.getCount(cat));
        String emit = str + " F1: " + ((int) (currentF1 * 10000)) / 100.0 + " (sent ave " + ((int) (10000 * f1s.getCount(cat) / num)) / 100.0 + ", evalb " + ((int) (10000 * cF1)) / 100.0 + ")";
        pw.println(emit);
      }
    }
    if (pw != null && runningAverages) {
      pw.println("========================================");
    }
    // lce.evaluate(guess, gold, pw); // evaluate regular LabeledConsituentEval; don't print it
  }

  public void display(boolean verbose, PrintWriter pw) {
    NumberFormat nf = new DecimalFormat("0.00");
    Set<Label> cats = new HashSet<Label>();
    cats.addAll(precisions.keySet());
    cats.addAll(recalls.keySet());

    pw.println("========================================");
    pw.println("Labeled Bracketed Evaluation by Category -- final statistics");
    pw.println("========================================");

    for (Label cat : cats) {
      double pnum2 = pnums2.getCount(cat);
      double rnum2 = rnums2.getCount(cat);
      double prec = precisions2.getCount(cat) / pnum2;//(num > 0.0 ? precision/num : 0.0);
      double rec = recalls2.getCount(cat) / rnum2;//(num > 0.0 ? recall/num : 0.0);
      double f = 2.0 / (1.0 / prec + 1.0 / rec);//(num > 0.0 ? f1/num : 0.0);

      pw.println(cat + "\tLP: " + ((pnum2 == 0.0) ? " N/A": nf.format(prec)) + "\tguessed: " + (int) pnum2 +
              "\tLR: " + ((rnum2 == 0.0) ? " N/A": nf.format(rec)) + "\tgold:  " + (int) rnum2 +
              "\tF1: " + ((pnum2 == 0.0 || rnum2 == 0.0) ? " N/A": nf.format(f)));
    }
    pw.println("========================================");
    // lce.display(verbose, pw);
  }

}
