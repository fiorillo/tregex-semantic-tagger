#!/bin/sh

# usage: script/test.sh "[test-sentence]"

sentence=$1

PROJECT_ROOT=`cat script/project-root.txt`
prep_dir=$PROJECT_ROOT/modality-patterns/preparatory
idio_dir=$PROJECT_ROOT/modality-patterns/idiosyncratic
pattern_dir=$PROJECT_ROOT/modality-patterns/instantiated-templates
cleanup_dir=$PROJECT_ROOT/modality-patterns/cleanup
tmp_dir=$PROJECT_ROOT/tmp
parser_dir=$PROJECT_ROOT/tools/stanford-parser
tregex_dir=$PROJECT_ROOT/tools/stanford-tregex

# parse the sentence
echo $sentence > $tmp_dir/test.txt
$parser_dir/lexparser.sh $tmp_dir/test.txt > $tmp_dir/test.parsed

# tag the sentence
cd $tregex_dir
./tsurgeon.sh -treeFile $tmp_dir/test.parsed $prep_dir/*.txt $idio_dir/*.txt $pattern_dir/*.txt $cleanup_dir/*.txt

# cleanup
rm $tmp_dir/test*

exit 0
