README: MURI Modality Tagging System

Included in this package are the following directories:

  modality-patterns: all the patterns and templates that the system uses
  data: a sample corpus for testing
  script: some useful scripts
  tools: stanford parser and tregex
  doc: documentation
  tmp: any temp files created by the scripts (also a good place to stick test files)
  attic: old, unused files, or files that I don't know what to do with

I. MODALITY-PATTERNS

The files in idiosyncratic are all preprocessing patterns that run before the 
other patterns. A description of them can be found in doc/attic-misc/README.txt. 
Some of the information there is a little out of date, and I will update it in 
the future. 

The files in generalized-templates generate the patterns in the 
instantiated-templates directory. The script 
script/rebuild_instantiated_templates.sh should be run after any changes are made 
to the templates. This will populate instantiated-templates with new versions of 
the patterns, based on the lexicon.

The lexicon is in final-modality-morph-variants.txt (which is in the root directory 
now, but should probably be moved elsewhere).

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

5. parse.sh: just a shortcut for using the stanford parser to parse a text file.

$ script/parse.sh unparsed-file.txt

III. TODO

Matt Fiorillo 2012-05-20
