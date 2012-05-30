#!/bin/sh

# usage: script/test-pattern.sh [name-of-pattern-w/o-.txt] "[test-sentence]"

pattern=$1
sentence=$2

root=`cat script/project-root.txt`
prep_dir=$root/modality-patterns/idiosyncratic
pattern_dir=$root/modality-patterns/instantiated-templates
tmp_dir=$root/tmp
parser_dir=$root/tools/stanford-parser
tregex_dir=$root/tools/stanford-tregex

# parse the sentence
echo $sentence > $tmp_dir/$pattern.txt
$parser_dir/lexparser.sh $tmp_dir/$pattern.txt > $tmp_dir/$pattern.parsed

# tag the sentence
cd $tregex_dir
./tsurgeon.sh -treeFile $tmp_dir/$pattern.parsed $prep_dir/*.txt $pattern_dir/$pattern.txt

# cleanup
rm $tmp_dir/$pattern*

exit 0
