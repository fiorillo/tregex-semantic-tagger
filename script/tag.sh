#!/bin/sh

# runs all the patterns on the given parsed file, and prints the modified parse tree to stdout
# usage: script/tag.sh [parsed-text-file] > [output file]

treefile=$1

root=`cat script/project-root.txt`
prep_dir=$root/modality-patterns/idiosyncratic
pattern_dir=$root/modality-patterns/instantiated-templates
tregex_dir=$root/tools/stanford-tregex

# run the patterns, in order
cd $tregex_dir
./tsurgeon.sh -treeFile $root/$treefile $prep_dir/*.txt $pattern_dir/*.txt

exit 0
