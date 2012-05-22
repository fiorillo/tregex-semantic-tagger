#!/bin/sh

# usage: ./test-pattern.sh [name-of-pattern-w/o-.txt] "[test-sentence]"

pattern=$1
sentence=$2

root=/Users/matthewfiorillo/Documents/work/tsurgeon
prep_dir=$root/modality-tagger/modality-patterns/idiosyncratic
pattern_dir=$root/modality-tagger/modality-patterns/instantiated-templates
tmp_dir=$root/tmp
parser_dir=$root/tools/stanford-parser
tregex_dir=$root/tools/stanford-tregex

# parse the sentence
echo $sentence > $tmp_dir/$pattern.txt
$parser_dir/lexparser.sh $tmp_dir/$pattern.txt > $tmp_dir/$pattern.parsed

# tag the sentence
cd $tregex_dir
./tsurgeon.sh -treeFile $tmp_dir/$pattern.parsed \
  $prep_dir/relabel-vb-zero.txt $prep_dir/remove-g.txt $prep_dir/remove-n.txt $prep_dir/remove-d.txt $prep_dir/remove-z.txt $prep_dir/remove-p.txt $prep_dir/mark-have-aux.txt $prep_dir/mark-be-aux.txt $prep_dir/mark-get-passive.txt $prep_dir/mark-passive-verbs.txt $prep_dir/md-must-have.txt $prep_dir/negative.txt $prep_dir/negative-never.txt $prep_dir/negative-finite-be.txt $prep_dir/have-to.txt $prep_dir/let-vp.txt $prep_dir/let-s.txt $prep_dir/have-need-of.txt $prep_dir/in-need-of.txt $prep_dir/fall-short.txt $prep_dir/fall-short-jj.txt \
  $pattern_dir/$pattern.txt

# cleanup
rm $tmp_dir/$pattern*

exit 0
