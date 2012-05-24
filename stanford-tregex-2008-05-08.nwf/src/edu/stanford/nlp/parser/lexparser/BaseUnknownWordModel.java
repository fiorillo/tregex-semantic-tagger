package edu.stanford.nlp.parser.lexparser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.ling.WordTag;
import edu.stanford.nlp.stats.ClassicCounter;
import edu.stanford.nlp.trees.Tree;


/**
 *  An unknown word model for a generic language.  This was originally designed for
 *  German, changing only to remove German-specific numeric features.  Models unknown
 *  words based on their prefix and suffixes, as well as capital letters.
 *
 * @author Roger Levy
 * @author Greg Donaker (corrections and modeling improvements)
 * @author Christopher Manning (generalized and improved what Greg did)
 * @author Anna Rafferty
 *
 */
public class BaseUnknownWordModel implements UnknownWordModel, Serializable {
  private static final long serialVersionUID = 6355171148751673822L;
  private static final boolean useFirst = false; //= true;
  private static boolean useEnd = true;
  private static final boolean useGT = false;
  private static boolean useFirstCap = true; // Only care if cap

  private static int endLength = 2; // only used if useEnd==true

  private static final String unknown = "UNK";

  private static final String numberMatch = "[0-9]+(?:\\.[0-9]*)";

  private Map<String,ClassicCounter<String>> tagHash = new HashMap<String,ClassicCounter<String>>();
  private Set<String> seenEnd = new HashSet<String>();

  private Map<String,Double> unknownGT = new HashMap<String,Double>();
  Lexicon l; 

  double[] smooth = { 1.0, 1.0 };

  public BaseUnknownWordModel() {
    this(new Options.LexOptions());
  }
  public BaseUnknownWordModel(Options.LexOptions op) {
    endLength = op.unknownSuffixSize;
    useEnd = op.unknownSuffixSize > 0 && op.useUnknownWordSignatures > 0;
    useFirstCap = op.useUnknownWordSignatures > 0;
  }

  
  /**
   * Currently we don't consider loc in determining score.
   */
  public double score(IntTaggedWord itw, int loc) {
    return (float) score(itw);
  }
  
  public double score(IntTaggedWord itw) {
    // treat an IntTaggedWord by changing it into a TaggedWord
    return score(itw.toTaggedWord());
  }


  /** Calculate the log-prob score of a particular TaggedWord in the
   *  unknown word model.
   *  @param tw the tag->word production in TaggedWord form
   *  @return The log-prob score of a particular TaggedWord.
   */
  public double score(TaggedWord tw) {
    double logProb;

    String word = tw.word();
    String tag = tw.tag();

    // testing
    //EncodingPrintWriter.out.println("Scoring unknown word " + word + " with tag " + tag,encoding);
    // end testing


    if (word.matches(numberMatch)) {
      //EncodingPrintWriter.out.println("Number match for " + word,encoding);
      if (tag.equals("CARD")) {
        logProb = 0.0;
      } else {
        logProb = Double.NEGATIVE_INFINITY;
      }
    } else {
      end:
      if (useEnd || useFirst || useFirstCap) {
          String end = getSignature(word, -1);
        if (!seenEnd.contains(end)) {
          if (useGT) {
            logProb = scoreGT(tag);
            break end;
          } else {
            end = unknown;
          }
        }
        //System.out.println("using end-character model for for unknown word "+  word + " for tag " + tag);

        /* get the Counter of terminal rewrites for the relevant tag */
        ClassicCounter<String> wordProbs = tagHash.get(tag);

        /* if the proposed tag has never been seen before, issue a
         * warning and return probability 0 */
        if (wordProbs == null) {
          //System.err.println("Warning: proposed tag is unseen in training data!");
          logProb = Double.NEGATIVE_INFINITY;
        } else if (wordProbs.keySet().contains(end)) {
          logProb = wordProbs.getCount(end);
        } else {
          logProb = wordProbs.getCount(unknown);
        }
      } else if (useGT) {
        logProb = scoreGT(tag);
      } else {
        System.err.println("Warning: no unknown word model in place!\nGiving the combination " + word + " " + tag + " zero probability.");
        logProb = Double.NEGATIVE_INFINITY; // should never get this!
      }
    }

    //EncodingPrintWriter.out.println("Unknown word estimate for " + word + " as " + tag + ": " + logProb,encoding); //debugging
    return logProb;
  }

  private double scoreGT(String tag) {
    //System.out.println("using GT for unknown word and tag " + tag);
    double logProb;
    if (unknownGT.containsKey(tag)) {
      logProb = unknownGT.get(tag).doubleValue();
    } else {
      logProb = Double.NEGATIVE_INFINITY;
    }
    return logProb;
  }

  /**
   * Signature for a specific German word; loc parameter is ignored.
   * @param word
   * @param loc
   * @return
   */
  public String getSignature(String word, int loc) {
    String subStr = "";
    int n = word.length() - 1;
    if (useFirstCap) {
      String first = word.substring(0, 1);
      if (first.equals(first.toUpperCase())) {
        subStr += "C";
      } else {
        subStr += "c";
      }
    }
    if (useFirst) {
      subStr += word.substring(0, 1);
    }
    if (useEnd) {
      subStr += word.substring(n - endLength > 0 ? n - endLength : 0, n);
    }
    return subStr;
  }
  
  public int getSignatureIndex(int wordIndex, int sentencePosition) {
    return 0;
  }


  /**
   * trains the end-character based unknown word model.
   *
   * @param trees the collection of trees to be trained over
   */
  public void train(Collection<Tree> trees) {
    if (useFirst) {
      System.err.println("Including first letter for unknown words.");
    }
    if (useFirstCap) {
      System.err.println("Including whether first letter is capitalized for unknown words");
    }
    if (useEnd) {
      System.err.println("Classsing unknown word as the average of their equivalents by identity of last " + endLength + " letters.");
    }
    if (useGT) {
      System.err.println("Using Good-Turing smoothing for unknown words.");
    }

    trainUnknownGT(trees);

    HashMap<String,ClassicCounter<String>> c = new HashMap<String,ClassicCounter<String>>(); // counts

    ClassicCounter<String> tc = new ClassicCounter<String>();

    for (Tree t : trees) {
      List<TaggedWord> words = t.taggedYield();
      for (TaggedWord tw : words) {
        String word = tw.word();
        String subString = getSignature(word, -1);

        String tag = tw.tag();
        if ( ! c.containsKey(tag)) {
          c.put(tag, new ClassicCounter<String>());
        }
        c.get(tag).incrementCount(subString);

        tc.incrementCount(tag);

        seenEnd.add(subString);

      }
    }

    for (Iterator<String> i = c.keySet().iterator(); i.hasNext();) {
      String tag = i.next();
      ClassicCounter<String> wc = c.get(tag); // counts for words given a tag

      /* outer iteration is over tags */
      if (!tagHash.containsKey(tag)) {
        tagHash.put(tag, new ClassicCounter<String>());
      }

      /* the UNKNOWN sequence is assumed to be seen once in each tag */
      // this is really sort of broken!
      tc.incrementCount(tag);
      wc.setCount(unknown, 1.0);

      /* inner iteration is over words */
      for (Iterator<String> j = wc.keySet().iterator(); j.hasNext();) {
        String end = j.next();
        double prob = Math.log(((double) wc.getCount(end)) / ((double) tc.getCount(tag)));
        tagHash.get(tag).setCount(end, prob);
        //if (Test.verbose)
        //EncodingPrintWriter.out.println(tag + " rewrites as " + end + " endchar with probability " + prob,encoding);
      }
    }
  }

  /** Trains Good-Turing estimation of unknown words. */
  private void trainUnknownGT(Collection<Tree> trees) {

    ClassicCounter<TaggedWord> twCount = new ClassicCounter<TaggedWord>();
    ClassicCounter<WordTag> wtCount = new ClassicCounter<WordTag>();
    ClassicCounter<String> tagCount = new ClassicCounter<String>();
    ClassicCounter<String> r1 = new ClassicCounter<String>(); // for each tag, # of words seen once
    ClassicCounter<String> r0 = new ClassicCounter<String>(); // for each tag, # of words not seen
    Set<String> seenWords = new HashSet<String>();

    int tokens = 0;

    /* get TaggedWord and total tag counts, and get set of all
     * words attested in training */
    for (Tree t : trees) {
      List<TaggedWord> words = t.taggedYield();
      for (TaggedWord tw : words) {
        tokens++;
        WordTag wt = toWordTag(tw);
        //String word = wt.word();
        String tag = wt.tag();
        //if (Test.verbose) EncodingPrintWriter.out.println("recording instance of " + wt.toString(),encoding); // testing

        wtCount.incrementCount(wt);// TaggedWord has crummy equality conditions
        twCount.incrementCount(tw);//testing
        //if (Test.verbose) EncodingPrintWriter.out.println("This is the " + wtCount.getCount(wt) + "th occurrence of" + wt.toString(),encoding); // testing
        tagCount.incrementCount(tag);
        //boolean alreadySeen = seenWords.add(word);

        // if (Test.verbose) if(! alreadySeen) EncodingPrintWriter.out.println("already seen " + wt.toString(),encoding); // testing

      }
    }

    // testing: get some stats here
    System.out.println("Total tokens: " + tokens);
    System.out.println("Total WordTag types: " + wtCount.keySet().size());
    System.out.println("Total TaggedWord types: " + twCount.keySet().size());
    System.out.println("Total tag types: " + tagCount.keySet().size());
    System.out.println("Total word types: " + seenWords.size());


    /* find # of once-seen words for each tag */
    for (Iterator<WordTag> i = wtCount.keySet().iterator(); i.hasNext();) {
      WordTag wt = i.next();
      if (wtCount.getCount(wt) == 1) {
        r1.incrementCount(wt.tag());
      }
    }

    /* find # of unseen words for each tag */
    for (Iterator<String> i = tagCount.keySet().iterator(); i.hasNext();) {
      String tag = i.next();
      for (Iterator<String> j = seenWords.iterator(); j.hasNext();) {
        String word = j.next();
        WordTag wt = new WordTag(word, tag);
        //EncodingPrintWriter.out.println("seeking " + wt.toString(),encoding); // testing
        if (!(wtCount.keySet().contains(wt))) {
          r0.incrementCount(tag);
          //EncodingPrintWriter.out.println("unseen " + wt.toString(),encoding); // testing
        } else {
          //EncodingPrintWriter.out.println("count for " + wt.toString() + " is " + wtCount.getCount(wt),encoding);
        }
      }
    }

    /* set unseen word probability for each tag */
    for (Iterator<String> i = tagCount.keySet().iterator(); i.hasNext();) {
      String tag = i.next();
      //System.out.println("Tag " + tag + ".  Word types for which seen once: " + r1.getCount(tag) + ".  Word types for which unseen: " + r0.getCount(tag) + ".  Total count token for tag: " + tagCount.getCount(tag)); // testing

      double logprob = Math.log(r1.getCount(tag) / (tagCount.getCount(tag) * r0.getCount(tag)));

      unknownGT.put(tag, new Double(logprob));
    }

    /* testing only: print the GT-smoothed model */
    //System.out.println("The GT-smoothing model:");
    //System.out.println(unknownGT.toString());
    //EncodingPrintWriter.out.println(wtCount.toString(),encoding);


  }

  private static WordTag toWordTag(TaggedWord tw) {
    return new WordTag(tw.word(), tw.tag());
  }


  /**
   * Get the lexicon associated with this unknown word model; usually not used, but
   * might be useful to tell you if a related word is known or unknown, for example.
   */
  public Lexicon getLexicon() {
    return l;
  }


  /**
   * This operation not supported by this model.
   */
  public void readData(BufferedReader in) throws IOException {
    throw new UnsupportedOperationException();
  }


  public void setLexicon(Lexicon l) {
    this.l = l;
  }
  public int getUnknownLevel() {
    // TODO Auto-generated method stub
    return 0;
  }
  public void setUnknownLevel(int unknownLevel) {
    // TODO Auto-generated method stub
    
  }
  
  

 
  
}
