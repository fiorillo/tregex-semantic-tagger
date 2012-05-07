#!/bin/sh

pattern_name=$1

root=/Users/matthewfiorillo/Documents/school/tsurgeon
cd $root/stanford-tregex-2012-03-09
./tsurgeon.sh -treeFile $root/example-sents/$pattern_name.txt.head.parsed $root/modality-tagger/modality-patterns/fixed-patterns/$pattern_name.txt > $root/output/$pattern_name.tree

exit 0
