#!/bin/bash

SCALEROOT=/export/projects/SCALE/2009_SIMT
EVALSCRIPTS=${SCALEROOT}/source_code/eval_scripts/
TSURGEON=${SCALEROOT}/external_tools/stanford-tregex-2008-05-08.nwf
MODALPATTERNS=${SCALEROOT}/source_code/modality/modality-patterns-version-controlled/

sed -e 's/^()$/(TOP parserfailure)/' training.en.tok.flattened | \
	 perl ${EVALSCRIPTS}/pipeline.pl \
		--prefix "java -cp ${TSURGEON}/stanford-tregex.jar edu.stanford.nlp.trees.tregex.tsurgeon.Tsurgeon -treeFile /dev/fd/0 -s " \
		--maxsize 60000 --maxcount 1000 \
		--inlist <(sed -e "s:^:${MODALPATTERNS}/idiosyncratic/:" < ${MODALPATTERNS}/idiosyncratic/ORDER.txt ; \
			   find ${MODALPATTERNS}/instantiated-templates/ -type f -name \*.txt ) \
| sed -e 's/^(TOP parserfailure)$/()/' > training.en.tok.tsurgeon
