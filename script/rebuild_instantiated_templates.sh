#!/bin/sh

SCALEROOT=/Users/matthewfiorillo/Documents/work/tsurgeon
EVALSCRIPTS=${SCALEROOT}/script/eval_scripts/
MODALCLASSES=${SCALEROOT}
PATTERN_DIR=${SCALEROOT}/modality-patterns

cd $PATTERN_DIR
rm -rf instantiated-templates
mkdir instantiated-templates

${EVALSCRIPTS}/instantiate_tsurgeon_templates.pl generalized-templates instantiated-templates \
	--keepgoing \
	< ${MODALCLASSES}/final-modality-morph-variants.txt \
	2> instantiated-templates/__instantiator_errors

if [ -s instantiated-templates/__instantiator_errors ]; then
	echo "Warning: Errors encountered while processing templates.";
	echo "  Please see instantiated-templates/__instantiator_errors .";
fi;
