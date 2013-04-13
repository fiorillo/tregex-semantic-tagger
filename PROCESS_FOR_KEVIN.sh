#!/bin/bash

nltot | sed 's/\t/ /g; s/  */ /g; s/ \([A-Z]\)/\1/g; s/\([A-Z][^ ]*\)/\U\1/g'
