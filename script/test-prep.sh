#!/bin/sh

# tests all the preprocessing patterns, in the order defined in idiosyncratic/command.txt
# if you want to change the order or add a new pattern, put it there (or come up with a better solution)
# uses the sentences in modality-tagger/test/prep.txt.parsed to test.
# any file parsed with the stanford parser will do though.

# root directory of project file; change this if need be
root=/Users/matthewfiorillo/Documents/school/tsurgeon
cd $root

# load up order of preprocessing and other idiosyncratic patterns
order=`cat modality-tagger/modality-patterns/idiosyncratic/command.txt | sed -e 's/ / \/Users\/matthewfiorillo\/Documents\/school\/tsurgeon\/modality-tagger\/modality-patterns\/idiosyncratic\//g'`

echo $order

# run those patterns, in order, and then the user-selected pattern
cd $root/stanford-tregex-2012-03-09
./tsurgeon.sh -treeFile $root/modality-tagger/test/prep.txt.parsed $order > $root/modality-tagger/modality-patterns/output/prep-test.txt

exit 0
