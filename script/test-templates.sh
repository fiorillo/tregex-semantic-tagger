#!/bin/sh

template=$1

# root directory of project file; change this if need be
root=/Users/matthewfiorillo/Documents/school/tsurgeon
cd $root

# load up order of preprocessing and other idiosyncratic patterns
# shell is FUCKED right now, so i just put everything on one line...
order=`cat modality-tagger/modality-patterns/idiosyncratic/command.txt | sed -e 's/ / \/Users\/matthewfiorillo\/Documents\/school\/tsurgeon\/modality-tagger\/modality-patterns\/idiosyncratic\//g'`

echo $order

# run those patterns, in order, and then the user-selected pattern
cd $root/stanford-tregex-2012-03-09
./tsurgeon.sh -treeFile $root/modality-tagger/test/templates.txt.parsed $order > $root/modality-tagger/test/templates.txt.prepped

# run the desired template here...but not sure how so i'm just going to do it manually for now

exit 0
