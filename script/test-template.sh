#!/bin/sh

# usage: first arg name of pattern file, second arg sentence to test it on.

pattern=$1
sentence=$2

# root directory of project file; change this if need be
root=/Users/matthewfiorillo/Documents/school/tsurgeon
test_dir=$root/modality-tagger/test
prep_dir=$root/modality-tagger/modality-patterns/idiosyncratic
pattern_dir=$root/modality-tagger/modality-patterns/instantiated-templates
output_dir=$root/output
cd $root

# create test file using $sentence
echo $sentence > $test_dir/$pattern.txt

# parse the file
$root/stanford-parser-2012-01-06/lexparser.sh $test_dir/$pattern.txt > $test_dir/$pattern.txt.parsed

# load up order of preprocessing and other idiosyncratic patterns
# shell is being FUCKED right now, so i just put everything on one line...
order=`cat $prep_dir/command.txt | sed -e 's/ / \/Users\/matthewfiorillo\/Documents\/school\/tsurgeon\/modality-tagger\/modality-patterns\/idiosyncratic\//g'`

#echo $order

# run those patterns, in order, and then the user-selected pattern
cd $root/stanford-tregex-2012-03-09
./tsurgeon.sh -treeFile $test_dir/$pattern.txt.parsed $order $pattern_dir/$pattern.txt > $output_dir/$pattern.txt

cat $output_dir/$pattern.txt

exit 0
