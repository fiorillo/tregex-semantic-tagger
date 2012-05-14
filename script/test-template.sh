#!/bin/sh

# tests a specific pattern on a given test sentence, and prints the parse tree to stdout
# usage: ./test-template.sh [name of pattern i.e. able-JJ-infinitive] [test sentence to use]

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
# for whatever reason, i can't get the sed-pattern to work without typing it literally and escaping everything
# so if you want to change the directory, do that.
order=`cat $prep_dir/command.txt | sed -e 's/ / \/Users\/matthewfiorillo\/Documents\/school\/tsurgeon\/modality-tagger\/modality-patterns\/idiosyncratic\//g'`

#echo $order

# run those patterns, in order, and then the user-selected pattern
cd $root/stanford-tregex-2012-03-09
./tsurgeon.sh -treeFile $test_dir/$pattern.txt.parsed $order $pattern_dir/$pattern.txt > $output_dir/$pattern.txt

cat $output_dir/$pattern.txt

exit 0
