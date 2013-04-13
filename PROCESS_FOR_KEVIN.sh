#!/bin/bash

awk 'BEGIN {RS = ""} {gsub("\n", "\t", $0); print $0}' | sed 's/\t/ /g; s/  */ /g; s/ \([A-Z]\)/\1/g; s/\([A-Z][^ ]*\)/\U\1/g'
