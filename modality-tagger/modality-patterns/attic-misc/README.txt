Known problems: (1) None of the patterns work when the target is a
coordinate structure.  (2) When the target is a noun phrase, the Targ
label might not land on the head noun. (3) Ambiguity: some phrases are
ambiguous between being targets and holders.  Not much we can do about
it.  

This directory contains T-Surgeon files.  Each file contains one
pattern and one or more actions.  The purpose of the patterns/actions
is to relabel parse trees.


This command line takes about four minutes to run:

java edu.stanford.nlp.trees.tregex.tsurgeon.Tsurgeon -treeFile /export/projects/SCALE/2009_SIMT/data/processed/modality/tsurgeon-input/flat-test-corpus.txt relabel-vb-zero.txt remove-g.txt remove-n.txt remove-d.txt remove-z.txt remove-p.txt mark-have-aux.txt mark-be-aux.txt  mark-get-passive.txt mark-passive-verbs.txt md-could.txt md-must.txt md-must-have.txt md-may.txt md-can.txt md-might.txt negative.txt negative-never.txt negative-finite-be.txt  have-to.txt md-should.txt require-transitive.txt  require-passive.txt require-NP-VP.txt require-adjective.txt require-SBAR.txt need-transitive.txt need-passive.txt need-NP-VP.txt need-adjective.txt want-transitive.txt want-NP-VP.txt  want-SBAR.txt  allow-transitive.txt allow-passive.txt  allow-passive-vp.txt allow-ditransitive.txt allow-NP-VP.txt able-to.txt permit-transitive.txt permit-passive.txt  permit-passive-vp.txt permit-NP-VP.txt order-transitive.txt order-passive.txt order-passive-vp.txt order-NP-VP.txt authorize-transitive.txt authorize-passive.txt authorize-passive-vp.txt authorize-NP-VP.txt let-vp.txt let-s.txt obligate-NP-VP.txt obligate-passive-vp.txt need-noun-vp.txt need-noun-for.txt have-need-of.txt in-need-of.txt  fail-vp.txt fail-in.txt fail-intrans.txt fail-in-vbg.txt abort-transitive.txt  abort-passive.txt fall-short.txt short-of.txt short-of-vbg.txt  fizzle-intransitive.txt flop-intransitive.txt unsuccessful-adjective.txt  unsuccessful-intransitive.txt  unsuccessful-in.txt unsuccessful-in-vbg.txt capable-of.txt capable-of-vbg.txt ready.txt powerless.txt unable.txt useless-in-vbg.txt useless-as.txt requirement-compound-noun.txt requirement-for.txt deny-transitive.txt deny-SBAR.txt deny-vbg.txt make-small-clause.txt make-NP-VP.txt appear-adj-complement.txt > output/test.txt


Some input files are in: 
/export/projects/SCALE/2009_SIMT/data/processed/modality/tsurgeon-input/short-flat-data.txt   
/export/projects/SCALE/2009_SIMT/data/processed/modality/tsurgeon-input/flat-test-corpus.txt   (very big)


Sample output:
/export/projects/SCALE/2009_SIMT/data/processed/modality/tsurgeon-output/tsurgeon-output-7-8-09.txt.

Several of these patterns are for pre-processing to make the remaining
patterns simpler.  The remaining patterns won't work without these
pre-processing patterns.

I don't know if the pre-processing steps need to be called in order,
or if tsurgeon keeps applying all the patterns until it can't apply
any more patterns.  I suspect it is the latter.  Maybe you can
understand the t-surgeon documentation better than I do.

relabel-vb-zero.txt  changes (VB <terminal>) to (VB B <terminal>).  B stands for "bare" or "base".

remove-g.txt changes (VBG <terminal>) into (VB G <terminal>)
remove-n.txt changes (VBN <terminal>) into (VB N <terminal>)
remove-d.txt changes (VBD <terminal>) into (VB D <terminal>)
remove-z.txt changes (VBZ <terminal>) into (VB Z <terminal>)
remove-p.txt changes (VBP <terminal>) into (VB P <terminal>)

The next three pre-processing steps identify Auxiliary verbs so that the 
modality marking patterns won't mark them as modality targets. 


mark-have-aux.txt changes (VB <letter> <form-of-have>) to (VB <letter>
AUX <form-of-have>) when it is a sister to a VBN (past particple).
The purpose is to distinguish the auxiliary verb "have" from the have
in "have a book" or "have to go" or "have him arrested", etc. 


mark-be-aux.txt changes (VB <letter> <form-of-be>) to (VB <letter> AUX
<form-of-be>) when it is a sister to a VBN (past particple) or VBG
(present particple).  The purpose isto distinguish the aspectual
auxiliary verb "be" and the passive auxiliary verb "be" from "be a
teacher", "be happy", etc.

mark-get-passive chages (VB <letter> <form-of-get>) to (VB <letter>
AUX <form-of-get>) when it is followed by a past participle (VBN).
The purpose is to identify the passive auxiliary verb "get" as in "get
arrested".


The next pre-processing step is mark-passive-verbs.  It changes (VB
<letter> <terminal>) to (VB <letter> VoicePassive <terminal>) when it
is preceded by a passive auxiliary "be" or "get".  I just did this so that 
later patterns can just look for VoicePassive instead of looking for all the
forms of "be" and "get" before the verb.


Negatives


There are three rules for detecting negatives: negative.txt,
negative-never.txt, and negative-finite-be.  See below for to-do list.

(I don't know if it is worth making these into templates because they
are just special cases of closed class items.  Maybe
negative-never.txt could be changed into negative-adverb.txt in case
there are other negative adverbs.)

negative.txt looks for (RB not) or (RB n't) and changes them to (RB
TrigNeg not) and (RB TrigNeg n't).  A non-AUX VB following the RB is
tagged as TargNeg.  For example (VB G running) would become (VB G
TargNeg running).  I think I lucked out on how this works.  Suppose
there is more than one VB following the RB.  T-Surgeon seems to apply
the pattern to the the last one, which happens to be the correct thing
to do.  After labeling the last one, it doesn't apply again because it
has a condition to stop recursion: the RB can't already dominate
TrigNeg.


negative-never.txt looks for (ADVP (RB never)) and labels it as
TrigNeg.  A verb that follows it is labeled as TargNeg.


negative-finite-be is a special case.  Finite forms of "be" that are
not marked as AUX are the only negative targets that precede (RB not).


Negative to-do list:

Not only:  This should not be counted as negative.

How about "almost never"?

no longer: need to add this one

Coordinate structures: not currently finding targets inside of
coordinate structures

Negative determiners and pronouns: a quick corpus check indicated that
these may account for up to 20% of negation in English.  We have to
check how often they are a source of divergence between English and
the other language. 


Modals


The verbs that are labeled MD in treebank are a closed class that has
specific syntactic behavior in English: they only occur in finite
positions (no participles or infinitives) and they don't inflect for
person, number, tense.  Some of them express semantic modality and
some don't.  So that is confusing.


I'm pretty sure these are all of the English modal auxiliaries: may,
might, must, shall, should, can, could, will, would.  


There are patterns for may, might, must, must have (must have gone),
should, and can.


"must" meaning "require" is distinguished from "must have" with
epistemic (believe) meaning.  I did a quick corpus study.  "Must have"
as in "must have gone" is always epistemic.  "must" not followed by
"have" is almost, but not always, requirement.  Some cases are
ambiguous, but we will miss them: the pattern always labels "must" not
followed by "have" as requirement.


In the corpus (English side of the language pack data), "can" and
"may" are almost always ability/belief and not permission.  The
patterns just label them as ability/belief.


Even though this is a closed class, we might want to make a template
in case someone wants to play with "would" and "could".  I didn't
include them because their meaning is difficult and ambiguous.
"Could" can be conditional or can be "can" (ability) in the past.
"Would" can be conditional or can be "will" in the past.

Require:

We require aid. (transitive)
Aid is requried. (passive)
We require them to help.  (NP VP)
The required aid (adjective)
They required that he go. (SBAR)

Took out require-passive-vp.txt (He was required to go).  (1) It seems
to be rare.  (2) We can't distinguish it from "$5B is required to
house the refugees" Anyway, the passive pattern kicks in first and
marks the $5B as the target, which happens to be right most of the
time in the English side of the language pack corpus.



Need:

We need aid. (transitive)
Aid is needed. (passive)
We need them to help.  (NP VP)
We need to help. (NP VP) (*so note that this pattern is different from require-NP-VP*)
The needed aid (adjective)


Want

We want aid. (transitive)
We want him to go. (NP VP)
We want to go.  (NP VP)
We want that he goes.  (SBAR -- not very common)

I didn't implement passive "Aid is wanted" because in a quick corpus
survey the most common use of passive "wanted" was for people being
wanted for crimes.   


It might be worth implementing "unwanted" as an adjective.

**I had to add somemthing special to want-transitive to be sure that
it doesn't apply when an S follows the direct object.  The reason is
that "want NP to VB" is parsed two ways, with the NP inside the S with
the VB and with the NP in direct object position.  For "require" and
"need", the NP was always inside the S.  So when there is an NP and an
S after the VB, we want to ignore the NP and find the target in the S.
I didn't implement this restriction in require-transitive or
need-transitive because it will filter out cases where some kind of
adjunct S is a sister to the verb.

Allow

Allow someone something (ditransitive)
allow something (transitive)
something is allowed (passive)
someone is allowed to VP (passive-vp)
allow someone to VP (allow-NP-VP)

allow-transitive has a special restriction on it.  The NP can't
precede another NP.  This is to prevent it from applying to
ditransitives.  Other words aren't ditransitive, so their transitive
rules don't need the restriction.  Adding the restriction to other
verbs would take a small bite out of recall because sometimes there
might be an adjunct or adverbial NP.  


allow-passive has a restriction to prevent it from applying where
allow-passive-vp should apply.  Don't remember right now why I didn't
do that for "require".  I think I didn't find "he is required to go"
in the corpus.


Able to:  This was very straightforward.  No problems.   


Permit:

permit something (transitive)
something is permitted (passive)
someone is permitted to VB (passive-vp)
permit someone to VB (NP-VP)

Order:

order something (transitive)
something was ordered (passive)
someone is ordered to VB (passive-VB)
permit someone to VB (NP-VP)

Skipped "order for something".  Sounded non-native.  

Added the extra stipluation in order-transitive that the NP not be
followed by an S.  (See want-transitive.)  But I only found one
example of "order NP S".

Authorize

authorize something (transitive)
something was authorized (passive)
someone is authorized to VB (passive-vp)
authorize someone to VB (NP-VP)

Let

let someone VB (let-vp, let-s)

There are two patterns for "let" because sentences with "let" are
parsed in two different ways: let NP VP or let S(NP VP).   

"Let" also participates in a lot of idioms (let someone down) and is
also lexically ambiguous.  One sense of "let" means "permit".  Another
sense is hortative (which means "encouraging".  Sorry.  Lingusists use
strange words.) as in "Let's go" or "Let it rain".  "Let me VB"
sometimes means "give me permission to VB" but sometimes it is a
polite way to say "I'm going to VB".  So....

1.  To avoid hortatives, "let" will not be tagged when it starts with
    a capital L.

2.  To avoid hortatives, "let" will not be tagged when followed
    by "us", "me", or "'s".  We will lose some recall in cases where
    "let me/us" really means "give me/us permission".  We will also
    lose some precision when a hortative has a noun phrase other than
    me, us, or 's ("let it rain").   

3.  I also wrote the pattern so that it doesn't include "lets" as in
    "He lets the dog go out every morning".  In the English side of
    the language pack corpus, "lets" is almost always a typo for
    "let's".  That might be overfitting to this corpus, so I can
    change that if necessary.

Obligate

Didn't find examples in the corpus.  Guessing that these are the most
common subcat frames.


Something obligated someone to VB  (NP VP)
Someone is obligated to VB (passive-vp)


Need (noun)

the need to study (need-noun-vp)
a need for help (need-noun-for)
have need of help (have-need-of)
in need of help (in-need-of)

I didn't make a general pattern for "need of" because the "of" phrase
is not always the target, e.g., "the need(s) of the government".  So
"need of" is only picked up in certain phrases like "have need of" and
"in need of".


Fail (!Succeed)

the plan failed (intransitive)
the government failed in providing aid (fail-in-vbg)
the government failed in their plan (fail-in)
the government failed to provide aid (fail-vp)

fail-intransitive will have false positives (lower precision) where
the subject is the holder, not the target, e.g., he failed.


Abort (!Succeed)

the insurgents aborted the plan (transitive)
the plan was aborted (intransitive)


Short (!Succeed)

Any attempt would fall short (fall short)
He would fall short of winning (short-of-vbg, not fall-short)
They were short of winning (short-of-vbg)
short of physical violence (short-of)


False positives (low precision): "He fell short": "He" will be tagged
as the target, but it is actually the holder.  "Short of time": "Time"
will be tagged as a target of !Succeed.  This is not clearly a case of
!Succeed.


Fizzle

The plan fizzled (out) (intransitive)

Not tested: no examples.

Flop

The plan flopped (intransitive)

Not tested: no examples.


Unsuccessful

unsuccessful in attacking (unsuccessful-in-vbg)
unsuccessful in their plan (unsuccessful-in)
The attempt was unsuccessful (intransitive)
unsuccessful attepmt (adjective)


False positives (low recall): "the players were unsuccessful":
"Playsers" is a holder, but it will be tagged as a target.  Same for
"the unsuccessful players".


Requirement

energy (target) requirements (trigger)
requirements (trigger) for energy (target)


The compound noun rule only works when "requirements" is the last
daughter of NP.  I did this because the NPs are too flat and it is
sometimes hard to find the head.   
