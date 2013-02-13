#!/bin/sh

# runs all the patterns on the given parsed file, and prints the modified parse tree to stdout
# usage: tag.sh [parsed-text-file] > [output file]

treefile=$1

root=`pwd`
prep_dir=$root/modality-patterns/preparatory
idio_dir=$root/modality-patterns/idiosyncratic
pattern_dir=$root/modality-patterns/instantiated-templates
cleanup_dir=$root/modality-patterns/cleanup
tregex_dir=$root/tools/stanford-tregex

# run the patterns, in order
cd $tregex_dir
./tsurgeon.sh -treeFile $root/$treefile $prep_dir/*.txt $idio_dir/*.txt $pattern_dir/*.txt $cleanup_dir/*.txt

exit 0
