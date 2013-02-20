#!/bin/sh

# runs all the patterns on the given parsed file, and prints the modified parse tree to stdout
# usage: tag.sh [pattern-to-test] [parsed-text-file] > [output file]

root=`pwd`
pattern=$root/$1
treefile=$root/$2
prep_dir=$root/modality-patterns/preparatory
cleanup_dir=$root/modality-patterns/cleanup
tregex_dir=$root/tools/stanford-tregex

# run the patterns, in order
cd $tregex_dir
./tsurgeon.sh -treeFile $root/$treefile $prep_dir/*.txt $pattern $cleanup_dir/*.txt

exit 0
