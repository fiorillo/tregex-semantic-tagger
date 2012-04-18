#!/usr/bin/perl

use Data::Dumper;
use File::Basename;
use File::Spec::Functions qw(rel2abs);
use Getopt::Long;

use strict;

BEGIN {
  my $SELFNAME = rel2abs($0);
  my $SELFPATH = dirname($SELFNAME);
  push @INC, $SELFPATH;
}
use hive_terp_utils;

my $wantinter = 0;  # Set this to 1 if you want intermediate
                    # nodes to show up in the parsed output.

while($_ = hive_terp_sentence_from_tsurgeon_file(*STDIN, $wantinter)) {
 last if $#{$_} < 0;
 map { my ($w, $d, $tsr) = @$_;
      print (" " x $d, $w ? "$w\@$d" : "__", " -- ", (join " ", @$tsr), "\n");
    } @$_;
}

