#!/bin/sh

# runs all the patterns on the given parsed file, and prints the modified parse tree to stdout
# usage: tag.sh [parsed-text-file] > [output file]

treefile=$1

root=`pwd`
prep_dir=$root/patterns/preparatory
idio_dir=$root/patterns/idiosyncratic
pattern_dir=$root/patterns/instantiated-templates
cleanup_dir=$root/patterns/cleanup
tregex_dir=$root/tools/stanford-tregex

# run the patterns, in order
cd $tregex_dir
./tsurgeon.sh -treeFile $root/$treefile $prep_dir/*.txt $idio_dir/*.txt $pattern_dir/*.txt $cleanup_dir/*.txt
###find "$root/$treefile" "$prep_dir/*.txt" "$idio_dir/*.txt" "$pattern_dir/*.txt" "$cleanup_dir/*.txt" -print0 | xargs -0 ./tsurgeon.sh -treeFile

exit 0
