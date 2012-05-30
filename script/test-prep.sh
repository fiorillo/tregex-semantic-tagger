#!/bin/sh

# usage: script/test-prep.sh "[test-sentence]"

sentence=$1

PROJECT_ROOT=`cat script/project-root.txt`
prep_dir=$PROJECT_ROOT/modality-patterns/idiosyncratic
pattern_dir=$PROJECT_ROOT/modality-patterns/instantiated-templates
tmp_dir=$PROJECT_ROOT/tmp
parser_dir=$PROJECT_ROOT/tools/stanford-parser
tregex_dir=$PROJECT_ROOT/tools/stanford-tregex

# parse the sentence
echo $sentence > $tmp_dir/prep.txt
$parser_dir/lexparser.sh $tmp_dir/prep.txt > $tmp_dir/prep.parsed

# tag the sentence
cd $tregex_dir
./tsurgeon.sh -treeFile $tmp_dir/prep.parsed $prep_dir/*.txt

rm $tmp_dir/prep*

exit 0
