README: MURI Modality Tagging System

Included in this package are the following directories:

  modality-tagger: the actual system, including all the patterns
  newscomm-en: a sample corpus for testing
  script: some useful scripts
  stanford-parser-X-Y-Z: an up-to-date version of the stanford parser
  stanford-tregex-X-Y-Z: an up-to-date version of tregex/tsurgeon

I. MODALITY-TAGGER AND PATTERNS

In the modality-tagger directory, there is an older version of tregex. The 
patterns will not work with that version. I will reorganize this soon. 
All of the patterns are in the modality-patterns directory. The important 
directories in there are idiosyncratic, generalized-templates, and 
instantiated-templates. 

The files in idiosyncratic are all preprocessing patterns that run before the 
other patterns. A description of them can be found in 
modality-tagger/modality-patterns/attic-misc/README.txt. Some of the 
information there is a little out of date, and I will update it in the future. 

The files in generalized-templates generate the patterns in the 
instantiated-templates directory. The script 
modality-tagger/modality-patterns/rebuild_instantiated_templates.sh should be 
run after any changes are made to the templates. This will populate 
instantiated-templates with new versions of the patterns, based on the 
lexicon. I should probably move the rebuild script to the script directory.

Information about the generalized templates and examples can be found in 
modality-tagger/modality-patterns/attic-misc.


II. SCRIPTS

I've included two useful scripts: tag.sh and parse-and-tag.sh. To tag a file 
that has already been parsed, use tag.sh as follows:

$ script/tag.sh parsed-input.txt > tagged-output.txt

To parse and then tag an unparsed text file, do:

$ script/parse-and-tag.sh input.txt > tagged-output.txt


III. TODO

Expect a reorganized version, with more useful scripts, soon.

Matt Fiorillo 2012-05-20