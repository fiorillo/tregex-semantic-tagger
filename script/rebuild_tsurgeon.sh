#!/bin/bash

# this absolutely WILL NOT WORK right now.  I'll get to it though.

PROJECT_ROOT=`cat script/project-root.txt`
EVALSCRIPTS=${PROJECT_ROOT}/source_code/eval_scripts/
TSURGEON=${PROJECT_ROOT}/external_tools/stanford-tregex-2008-05-08.nwf
MODALPATTERNS=${PROJECT_ROOT}/source_code/modality/modality-patterns-version-controlled/

sed -e 's/^()$/(TOP parserfailure)/' training.en.tok.flattened | \
	 perl ${EVALSCRIPTS}/pipeline.pl \
		--prefix "java -cp ${TSURGEON}/stanford-tregex.jar edu.stanford.nlp.trees.tregex.tsurgeon.Tsurgeon -treeFile /dev/fd/0 -s " \
		--maxsize 60000 --maxcount 1000 \
		--inlist <(sed -e "s:^:${MODALPATTERNS}/idiosyncratic/:" < ${MODALPATTERNS}/idiosyncratic/ORDER.txt ; \
			   find ${MODALPATTERNS}/instantiated-templates/ -type f -name \*.txt ) \
| sed -e 's/^(TOP parserfailure)$/()/' > training.en.tok.tsurgeon
