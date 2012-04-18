#!/usr/bin/perl

# reads from STDIN lines of the form "word$class" and generates a series of
# wds.$CLASS file and an enumeration of all classes (on STDOUT).  Tags in the
# word field must use the TERp SCALECost format of "word''tag".
#
# The file format is thus:
#   W1 W2''T2$CLASS &ADDITIONAL
# where
#   CLASS is not permitted to have any contained $ or & literal
#         (but W* and T* and ADDITIONAL are)
#   There must be at least one W1 or W1''T1 on the line.
#   ''Tx are optional in all cases.
#   The entire string " &ADDITIONAL" is optional.
#   The leading space in " &ADDITIONAL" is optional if the field is present,
#         and any amount of whitespace will be accepted.

use strict;

use File::Basename;
use File::Spec::Functions qw(rel2abs);

BEGIN {
  my $SELFNAME = rel2abs($0);
  my $SELFPATH = dirname($SELFNAME);
  push @INC, $SELFPATH;
}
use hive_terp_utils;

my %fhs = ( );

while(<STDIN>) {
	chomp;
	$_ = hive_terp_classdef_line_retag($_);

	next if not defined $_;
	my ($n, $w) = @$_[0,1];

	if(not exists $fhs{$n}) {
		local *FILE;
		open FILE, ">>", "wds.$n";
		$fhs{$n} = *FILE;
	}
	print {$fhs{$n}} $w, "\n";
}

# For each class, add backoff token.
map {	# e.g. !Able --> ''TCZEAble
	print {$fhs{$_}}
		$hive_terp_separator
                , $hive_terp_classforce_prefix
		, hive_terp_tag_encoder($_)
		, "\n";
    } (keys %fhs);

# Emit the list of classes to STDOUT
map { print "$_\n" ; } (keys %fhs);

# Close all of our files
map { close $_ ; } (keys %fhs);

print "m.name\n";
open NAME, ">>", "wds.m.name";
print NAME "${hive_terp_separator}name\n";
close NAME;

sub print_punct ($$) {
	my ($fh, $char) = @_;
	print $fh "$char\n";

	my $enc = hive_terp_tag_encoder($char);
	if ($enc ne $char) {
		# Wildcards of the punctuation tag family.
		print $fh $hive_terp_separator, $enc, "\n";
	}
}

print "m.punct\n";
open PUNCT, ">>", "wds.m.punct";
# TODO: Others?
print_punct (*PUNCT, ":");
print_punct (*PUNCT, ";");
print_punct (*PUNCT, ",");
print_punct (*PUNCT, ".");
print_punct (*PUNCT, "'");
print_punct (*PUNCT, '"');
print_punct (*PUNCT, "-");
print_punct (*PUNCT, "?");
print_punct (*PUNCT, "!");
close PUNCT;

# Note that because of a quirk in how SCALECost reads its input,
# the tagless variants must come first.
sub print_with_tag ($$$) {
	my ($fh, $wd, $t) = @_;
	print $fh $wd, "\n";
	print $fh $wd, $hive_terp_separator, hive_terp_tag_encoder($t), "\n";
}

print "m.det\n";
open DET, ">>", "wds.m.det";
map { print_with_tag(*DET, $_, "DT") } (
	"a", "all", "an", "another", "any", "both", "each", "either",
	"every", "half", "neither", "no", "some", "that", "the", "these",
	"this", "those"
);
print DET $hive_terp_separator, hive_terp_tag_encoder("DT"), "\n";
close DET;

print "m.conj\n";
open CONJ, ">>", "wds.m.conj";
map { print_with_tag(*CONJ, $_, "IN") } (
	"and", "both", "but", "either", "neither", "nor", "or", "so"
);
print CONJ $hive_terp_separator, hive_terp_tag_encoder("CC"), "\n";
close CONJ;

print "m.prep\n";
open PREP, ">>", "wds.m.prep";
map { print_with_tag(*PREP, $_, "IN") } (
	"about","across","after","against","along","among","around",
	"as","at","before","behind","beside","between",
	"beyond","by","despite","during","except","for","from","if","in",
	"inside","into","like","near","of","off","on","out","outside","over",
	"since","so","than","that","though","through","till","towards",
	"under","until","up","upon","via","while","with","within","without"
);
print PREP $hive_terp_separator, hive_terp_tag_encoder("IN"), "\n";
close PREP;

print "m.be\n";
open BE, ">>", "wds.m.be";
print_with_tag(*BE, "am", "VBP");
print_with_tag(*BE, "are", "VBP");
print_with_tag(*BE, "be", "VB");
print_with_tag(*BE, "is", "VBZ");
print_with_tag(*BE, "was", "VBD");
close BE;
