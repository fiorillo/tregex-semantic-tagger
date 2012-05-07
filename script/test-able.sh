#!/bin/sh

root=/Users/matthewfiorillo/Documents/school/tsurgeon
cd $root/stanford-tregex-2012-03-09
./tsurgeon.sh -treeFile $root/example-sents/able-JJ-infinitive.txt.head.parsed $root/modality-tagger/modality-patterns/fixed-instantiated-templates/able-JJ-infinitive.txt > $root/output/able-JJ-infinitive.tree

exit 0
