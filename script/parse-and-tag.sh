#!/bin/sh

# parses the textfile, then runs the modality tagger on it and prints the modified parse tree to stdout
# relative paths only, please
# usage: script/parse-and-tag.sh [text file] > [output file]

textfile=$1

# root directory of project file; change this if need be
root=`cat script/project-root.txt`
prep_dir=$root/modality-patterns/idiosyncratic
pattern_dir=$root/modality-patterns/instantiated-templates
tmp_dir=$root/tmp
parser_dir=$root/tools/stanford-parser
tregex_dir=$root/tools/stanford-tregex

# parse the textfile. outputs to a tempfile which will be deleted at the end.
$parser_dir/lexparser.sh $textfile > $tmp_dir/parse-and-tag.parsed

# run the patterns, in order
cd $tregex_dir
./tsurgeon.sh -treeFile $tmp_dir/parse-and-tag.parsed $prep_dir/*.txt $pattern_dir/*.txt

# clean up tempfile
rm $tmp_dir/parse-and-tag.parsed

exit 0
