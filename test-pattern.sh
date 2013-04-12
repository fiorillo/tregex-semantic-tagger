#!/bin/sh

# runs all the patterns on the given parsed file, and prints the modified parse tree to stdout
# usage: tag.sh [pattern-to-test] [parsed-text-file] > [output file]

pattern=$1
treefile=$2

root=`pwd`
prep_dir=$root/patterns/preparatory
cleanup_dir=$root/patterns/cleanup
tregex_dir=$root/tools/stanford-tregex

# run the patterns, in order
cd $tregex_dir
./tsurgeon.sh -treeFile $root/$treefile $prep_dir/*.txt $root/$pattern $cleanup_dir/*.txt

exit 0
