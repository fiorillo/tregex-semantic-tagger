#!/bin/sh

pattern_name=$1

# root directory of project file; change this if need be
root=/Users/matthewfiorillo/Documents/school/tsurgeon

# load up order of preprocessing and other idiosyncratic patterns
order=""
cat $root/modality-tagger/modality-patterns/idiosyncratic/ORDER.txt | while read file; do
  order="$order $root/modality-tagger/modality-patterns/idiosyncratic/$file"
done

# run those patterns, in order, and then the user-selected pattern
cd $root/stanford-tregex-2012-03-09
./tsurgeon.sh -treeFile $root/example-sents/$pattern_name.txt.head.parsed $order $root/modality-tagger/modality-patterns/fixed-patterns/$pattern_name.txt > $root/output/$pattern_name.tree

exit 0
