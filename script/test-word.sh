#!/bin/sh

# usage: script/test-word.sh [word-to-test] "[test-sentence]"

if [ $# -eq 2 ]; then
  word=$1
  sentence=$2
#elif [ $# -eq 1 ]; then
#  sentence=$1
else
  echo "usage: $0 [word-to-test] \"[sentence]\""
  exit 1
fi

PROJECT_ROOT=`cat script/project-root.txt`
prep_dir=$PROJECT_ROOT/modality-patterns/preparatory
idio_dir=$PROJECT_ROOT/modality-patterns/idiosyncratic
pattern_dir=$PROJECT_ROOT/modality-patterns/instantiated-templates
cleanup_dir=$PROJECT_ROOT/modality-patterns/cleanup
tmp_dir=$PROJECT_ROOT/tmp
parser_dir=$PROJECT_ROOT/tools/stanford-parser
tregex_dir=$PROJECT_ROOT/tools/stanford-tregex

# parse the sentence
echo $sentence > $tmp_dir/$word.txt
$parser_dir/lexparser.sh $tmp_dir/$word.txt > $tmp_dir/$word.parsed

# tag the sentence
cd $tregex_dir
./tsurgeon.sh -treeFile $tmp_dir/$word.parsed $prep_dir/*.txt $pattern_dir/$word* $cleanup_dir/*.txt
# don't include idiosyncratic patterns here - should I?

# cleanup
rm $tmp_dir/$word*

exit 0
