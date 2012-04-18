#!/usr/bin/perl

# A script which constructs a pipeline of streaming processes
# This is somewhat funny.  Like xargs, your processes must not
# really care about which one gets which arguments.
#
# We hook our stdin to the stdin of the first process
# We then stream the output of the last stage to our own STDOUT.
#
# --inlist  A file containing the additional arguments to run
# --prefix  The prefix of the pipeline command
#
# --dryrun  Dhows what would happen but doesn't ctually
# --maxsize Set the maximum size of commands run
#
# Example: 
#   perl ./pipeline.pl --prefix echo --maxsize 12 --inlist <(echo foo bar baz)
#
#

use strict;

use Data::Dumper;
use FileHandle;
use Getopt::Long;
use IPC::Open2;

my $INLIST=undef;
my $PREFIX=undef;
my $DEBUG=0;
my $DRYRUN=0;
my $MAXCMDSIZE=60000;
my $MAXCOUNT=1000;

my %OPTIONS = (
  "debug" => \$DEBUG,
  "dryrun" => \$DRYRUN,
  "inlist=s" => \$INLIST,
  "maxcount=i" => \$MAXCOUNT,
  "maxsize=i" => \$MAXCMDSIZE,
  "prefix=s" => \$PREFIX,
); 

GetOptions(%OPTIONS) or die;
die "Need input list (--inlist)" unless defined $INLIST;
die "Need prefix (--prefix)" unless defined $PREFIX;

my $kickoffs = 0;
sub kickoff($$) {
  my ($cmd, $pipein) = @_;
  print STDERR "Pipeline kickoff stage $kickoffs\n" if $DEBUG;
  $kickoffs++;
  if ($DRYRUN) {
    print STDERR "PIPELINE STAGE $pipein : '$cmd'\n";
    return $pipein+1;
  } else {
    my $pipeout;
    $pipein = fileno $pipein;
    open ARGH, "<&$pipein";
    open2($pipeout, "<&ARGH", $cmd);
    return $pipeout;
  }
}

open INLIST, "<", $INLIST or die;

my $nextarg;
my $prevout = $DRYRUN ? 0 : *STDIN;

my $size = length $PREFIX;
my @args = ( $PREFIX );
while(my $nextarg = <INLIST>) {
  chomp $nextarg;

  my $incr = 1 + length $nextarg;
  die "Overlong argument: $nextarg" if $incr + length $PREFIX > $MAXCMDSIZE;
  
  if ($size + $incr > $MAXCMDSIZE or $#args >= $MAXCOUNT) {
    $prevout = kickoff( (join " ", @args), $prevout );
    @args = ( $PREFIX, $nextarg );
    $size = $incr + length $PREFIX;
  } else {
    $size += $incr;
    push @args, $nextarg;
  }
}
if ($#args > 0 ){
  $prevout = kickoff( (join " ", @args), $prevout );
}

while (<$prevout>) { print; }
