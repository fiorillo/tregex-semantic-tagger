#!/bin/sh

# usage: script/parse.sh [file.txt]

$file=$1

root=`script/project-root.txt`
parser_dir=$root/tools/stanford-parser

$parser_dir/lexparser.sh $file

exit 0
