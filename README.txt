README: MURI Modality Tagging System

Included in this package are the following directories:

  modality-patterns: all the patterns and templates that the system uses
  data: a sample corpus for testing
  script: some useful scripts
  tools: stanford parser and tregex
  doc: documentation
  tmp: any temp files created by the scripts (also a good place to stick test files)
  attic: old, unused files, or files that I don't know what to do with
  output: various outputs from the system at different times for comparison.

I. MODALITY-PATTERNS

There are five sub-directories of modality-patterns:

-preparatory: these are run before everything else, to get the input into a state 
that the other patterns can use.

-idiosyncratic: these are run next. Anything that can't be generalized, basically.

-generalized-templates: these are all the patterns with wildcards (** or ***) in 
them. They are used to generate the patterns in instantiated-templates.

-instantiated-templates: the vast majority of patterns are here. They are 
templates from generalized-templates with the wildcards filled in with specific 
words from final-modality-morph-variants.txt.

-cleanup: a couple patterns that need to be run after everything else.

Templates can be re-instantiated with script/rebuild_instantiated_templates.sh.

Information about the generalized templates and examples can be found in 
doc/attic-misc.


II. SCRIPTS

There are a few ways to use the system. All of the scripts read the location of the 
project root from the file script/project-root.txt.  Make sure you change this file 
before running any of these scripts. This also means that all the scripts have to be run 
from the main directory (the one that has this readme in it). I will change this as soon 
as I come up with a better solution and have time (probably just by writing a setup 
script).

1. tag.sh: to tag a file that has already been parsed, use:

$ script/tag.sh parsed-input.txt > tagged-output.txt

2. parse-and-tag.sh: to parse and then tag an unparsed text file, do:

$ script/parse-and-tag.sh input.txt > tagged-output.txt

3. test-template.sh: for quick testing of a single pattern (from the 
instantiated-templates dir), use:

$ script/test-pattern.sh [name-of-pattern-w/o-extension] "example sentence"

For example, to test the pattern able-JJ-infinitive.txt, run:

$ script/test-pattern.sh able-JJ-infinitive "they were able to go"

This will still run all the preprocessing patterns, but not any of the other 
patterns in the instantiated-templates directory, so it goes a bit faster.

4. test-word.sh: this is similar to test-pattern, but runs all the patterns that 
start with a specified word:

$ script/test-word.sh able "they were able to go"

5. test-prep.sh: essentially the same thing, but only runs the preprocessing scripts:

$ script/test-prep.sh "they were able to go"

6. parse.sh: just a shortcut for using the stanford parser to parse a text file.

$ script/parse.sh unparsed-file.txt

III. TODO

Matt Fiorillo 2012-05-20
