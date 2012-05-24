#!/usr/bin/perl

# With no options, Takes N files : the references and hypothesis (in that
# order) lists of sentences.  Invokes an army of scripts to SGMLize these files
# and then derive BLEU, METEOR, and TERpa scores.  Other metrics are available,
# namely NIST, TER, and TERp.  If you want anything else, please just ask.
#
# Currently "BLEU" and "NIST" refer by default to MTEVAL V13.  The script is
# also capable of running earlier versions of MTEVAL, and currently has support
# for versions 10, 11a, 11b, and 12 as well.
#
# Options:
#  --workdir    Specify the work directory.  If unspecified, will use the
#               unix timestamp and hostname and pid to derive a directory name.
#
#  --[no]keepwork   Turn on more (human-) useful output formats and keep
#               and keep the work directory around after we're done.
#               Note that specifying --workdir by default implies --keepwork;
#               explicitly specify --nokeepwork if you don't want that.
#               Note as well that --keepwork causes some metric evaluations to
#               produce more output on disk (e.g. the TER family produces
#               text and HTML formats) under the assumption that humans wanted
#               to investigate the scoring; this may slow down scoring.
#
#  --bloodgood  Produce scores in individual files, named ${METRIC}.txt,
#               containing "${METRIC} = ${SCORE}\n", in the directory given.
#
#  --haterp     Report HATERp scoring using the default SCALE configuration.
#
#  --haterpcfg  Report additional HATERp scoring.
#               Takes the path to the HATERp parameter file (as produced by,
#               e.g., hive_terp_make_config.sh)
#
#               Requires that the reference files be named according to SCALE
#               naming conventions.  That is, references are named $STEM.$PART
#               and must have $STEM.tagged.$PART and
#               $STEM.norm.phxout.tags_simple.$PART beside them.  e.g.:
#                      ref: devtest.en.tok.0
#                      pos: devtest.en.tok.tagged.0
#                      phx: devtest.en.tok.norm.phxout.tags_simple.0
#               See hive_terp_markup_maker.pl for details.
#
#               Note that more than one --haterp can be given; the script will
#               call HATERp with each as the configuration file.  This might be
#               useful for scoring with multiple sets of weights.  The outputs
#               will be named HATERp_$bn_$c, where $bn is the base name of the
#               configuration file given, and $c is a disambiguation counter.
#
#  --haterpsgml Used to give HATERp reference already processed into SGML.
#
#  --[no]{{mtevalv{10,11a,11b,12,13},}{nist,bleu},meteor,ter,terp,terpa}
#               Specify which of the metrics to run, overriding the defaults.
#
#  --terpacfg   Like --haterpcfg, take a TERPa configuration file and use it
#               as an additional metric.  The output naming is similar,
#               prefixed with "TERpa_".
#
#  --sgml       Do not do the SGMLization munging pass.  Rather than the
#               usual reference and hypothesis files, ARGV after all options
#               are removed must be (in order) the reference, hypothesis,
#               and source (if needed) files.
#
#  --refsgml    Do not do the SGMLization munging pass for reference material.
#               ARGV after all options are removed must be (in order) the
#               SGML reference and the hypothesis file (one line per file).
#
#  --[no]onlyascii Strip non-ASCII characters from the hypothesis files.  This
#               option defaults on for plaintext inputs and does not make sense
#               with --sgml.
#
#  --pretoken   Assume that hypothesis input is pretokenized and limit the
#               amount of tokenization that the metrics will do.  This is
#               global and affects all metrics under consideration.  If you
#               want both scores (unlikely), run this script twice.
#               (You can use --sgml (or --sgmlref) modes the second time
#               to avoid reprocessing (some) inputs).
#
#               This option implies --noonlyascii by default.
#
#               Note that METEOR by default assumes that the inputs are
#               tokenized.  This option has no effect on METEOR scores.
#
#               The MTEVAL NIST/BLEU score script insists on doing _some_
#               tokenization internally (so-called "international"
#               tokenization) so this option simply switches to that
#               minimal tokenization rather than "disabling" tokenization
#               altogether.
#
#  --debug      Provide additional debugging information
#
#
# Example usages:
#
#   # Basic usage
#   scale_eval.pl ref1.txt ref2.txt hyp.txt
#
#   # Using pre-concatenated SGML references
#   scale_eval.pl --sgmlref ref.sgm hyp.txt
#
#   # Specifying the work directory and looking at TERpa alignment files:
#   scale_eval.pl ref1.txt ref2.txt hyp.txt --workdir eval
#   firefox eval/TERpa.out.html
#
#   # Using HATERp
#   scale_eval.pl sample.ref.txt sample.hyp.txt \
#                 --haterp terp.config/terp.param 
#
#   # Using pre-generated SGML for all files with HATERp
#   scale_eval.pl --sgml ref.sgml tst.sgml \
#                 --haterpsgml haterpref.sgml --haterp terp.config/terp.param
#

use Data::Dumper;
use File::Spec::Functions qw(rel2abs);
use File::Basename;
use Getopt::Long;

use English;
use strict;
use warnings;

my $SELFNAME = rel2abs($0);
my $SELFPATH = dirname($SELFNAME);
my $ORIGARGS = join " ", @ARGV;

# Paths to files out here in the world.
my $HATERPDIR="$SELFPATH/haterp.config/";

# Paths to programs out there in the world.
my %MTEVAL = (
        10 => "/export/projects/SCALE/2009_SIMT/external_tools/mteval/mteval-v10.pl",
        "11a" => "/export/projects/SCALE/2009_SIMT/external_tools/mteval/mteval-v11a.pl",
        "11b" => "/export/projects/SCALE/2009_SIMT/external_tools/mteval/mteval-v11b.pl",
        12 => "/export/projects/SCALE/2009_SIMT/external_tools/mteval/mteval-v12.pl",
        13 => "/export/projects/SCALE/2009_SIMT/external_tools/mteval/mteval-v13.pl",
          # Bugfixed version of NIST MTEVAL.
        "13S" => "$SELFPATH/mteval-v13s.pl",
);
my $METEOR="/export/projects/SCALE/2009_SIMT/external_tools/meteor/meteor-0.8.2/bin/meteor";
my $QUICKLY_SGMLIZE="$SELFPATH/quickly_sgmlize.pl";
my $HATERP_MARKUP_MAKER="$SELFPATH/hive_terp_markup_maker.pl";
my $TERP_BASENAME="/export/projects/SCALE/2009_SIMT/external_tools/terp_source/bin/";

die "Unable to find quickly_sgmlize.pl" if not -x $QUICKLY_SGMLIZE;
die "Unable to find hive_terp_markup_maker.pl" if not -x $HATERP_MARKUP_MAKER;
die "TERp basename looks suspicious" if not -x "$TERP_BASENAME/terpa";

my $BLOODGOOD=undef;
my $DIRNAME=undef;
my $HATERPSGML=undef;
my $KEEPWORK=undef;
my $ONLYASCII=undef;
my $DEBUG=0;
my $PRETOKEN=0;
my $REFSGMLMODE=0;
my $SGMLMODE=0;
my @TERPACFGS=();
my @HATERPCFGS=();
my %METRICS = (
  "mtevalv13Sbleu" => 1,
  "meteor" => 1,
  "ter" => 0,
  "terp" => 0,
  "terpa" => 0,
  "haterp" => 1,
);
foreach my $MTEVER (keys %MTEVAL) {
  $METRICS{"mtevalv${MTEVER}bleu"} = 0
        unless exists $METRICS{"mtevalv${MTEVER}bleu"};

  $METRICS{"mtevalv${MTEVER}nist"} = 0
        unless exists $METRICS{"mtevalv${MTEVER}nist"};
}
my %OPTIONS = (
  "workdir=s" => \$DIRNAME,
  "keepwork!" => \$KEEPWORK,
  "pretoken!" => \$PRETOKEN,
  "bloodgood=s" => \$BLOODGOOD,
  "haterpcfg=s" => \@HATERPCFGS,
  "terpacfg=s" => \@TERPACFGS,
  "haterpsgml=s" => \$HATERPSGML,
  "onlyascii" => \$ONLYASCII,
  "sgml" => \$SGMLMODE,
  "refsgml" => \$REFSGMLMODE,
  "debug" => \$DEBUG,
);
foreach my $m (keys %METRICS) {
        $OPTIONS{"${m}!"} = \$METRICS{$m};
}
$OPTIONS{"bleu!"} = \$METRICS{"mtevalv13Sbleu"};
$OPTIONS{"nist!"} = \$METRICS{"mtevalv13Snist"};

GetOptions(%OPTIONS) or die;

if ($#HATERPCFGS >= 0 or $METRICS{'haterp'}) {
  foreach my $hac (@HATERPCFGS) {
    die "Cannot read HATERp configuration file $hac" if not -r $hac;
  }
  if (($SGMLMODE or $REFSGMLMODE) and not defined $HATERPSGML) {
    die "Need --haterpsgml if --haterp and (--sgml or --refsgml) is given";
  }
} else {
  if (defined $HATERPSGML) {
    die "--haterpsgml doesn't make sense without --haterp";
  }
}

my $needsrc=0;
foreach my $MTEVER (keys %MTEVAL) {
  $needsrc |= $METRICS{"mtevalv${MTEVER}bleu"};
  $needsrc |= $METRICS{"mtevalv${MTEVER}nist"};
}

if ($SGMLMODE) {
  if ($ONLYASCII) {
    die "--onlyascii doesn't make sense with --sgml";
  }
  if ($REFSGMLMODE) {
    die "Cannot have both --sgml and --refsgml";
  } else {
    if ($needsrc) {
      die "--sgml for given metrics needs exactly three arguments" if ($#ARGV != 2);
    } else {
      if ($#ARGV == 2) {
        my $fn = pop @ARGV;
        warn "Discarding file $fn as unneeded source.";
      }
      die "--sgml for given metrics needs exactly two arguments" if ($#ARGV != 1);
    }
  }
} else {
  if ($REFSGMLMODE) {
    die "--refsgml means I need exactly two arguments" if $#ARGV != 1;
  } else {
    die "Need at least two arguments" if $#ARGV < 1;
  }

  # Unless the user explicitly said --noonlyascii or --pretoken, turn it on.
  $ONLYASCII = 1 if (not defined $ONLYASCII and not $PRETOKEN);
}


# If the user did not explicitly provide --nokeepwork but did specify a
# workdir, set act as if they'd provided a --keepwork.
$KEEPWORK = defined $DIRNAME if (not defined $KEEPWORK);

{
  my $doing_something = 0;
  foreach my $sys (keys %METRICS) {
    $doing_something = 1 if $METRICS{$sys};
  }
  $doing_something |= $#HATERPCFGS >= 0;
  $doing_something |= $#TERPACFGS >= 0;

  if (not $doing_something) {
    warn "Running with no metrics...";
    die "And that doesn't seem useful" if not $KEEPWORK;
  }
}

# If the user did not specify a directory name, make one up.  We use
# the time, hostname, and our PID to try to be as unique as possible
# and avoid NFS problems.  Note that we are not race-free.
if (not defined $DIRNAME) {
  my $TIMESTAMP=time;
  my $HOSTNAME=`hostname`;
  chomp $HOSTNAME;
  $DIRNAME=rel2abs("eval.$TIMESTAMP.$HOSTNAME.$PID");
}

die "Stubbornly refusing to work with extant directory: ${DIRNAME}"
  if (-e $DIRNAME);

mkdir $DIRNAME or die "Can't make directory $DIRNAME";
print STDERR "Running evaluation; work in $DIRNAME\n" if $KEEPWORK;

if ($KEEPWORK) {
open CMDFILE, ">", "$DIRNAME/eval.cmdlines";
print CMDFILE "## ", $SELFNAME, " ", $ORIGARGS, "\n";

open OUTFILE, ">", "$DIRNAME/eval.out";
}
$ORIGARGS=undef;

sub say { 
  print STDERR @_;
  print OUTFILE @_ if $KEEPWORK;
}

sub logsystem {
  print CMDFILE (@_, "\n") if $KEEPWORK;
  system @_;
}

if (defined $BLOODGOOD) {
  mkdir $BLOODGOOD if not -e $BLOODGOOD;
  die "Unable to mkdir $BLOODGOOD" if not -e $BLOODGOOD;
  say "Running in Bloodgood mode; output in $BLOODGOOD\n";
}

if ($DEBUG) {
  say "After all command line processing...\n";
  say "ARGV is : ", (join " ", @ARGV), "\n";
  say "Metrics :\n" , Dumper(\%METRICS);
}

sub rename_collisions ($$) {
  my ($ctrs, $name) = @_;
  if (not exists $$ctrs{$name}) {
    $$ctrs{$name} = 0;
  } else {
    $name .= "_" ;  
    $name .= $$ctrs{$name}++ ;  
  }
  $name;
}

my $PROCSRC;
my $PROCREF;
my $PROCTST;
my $PROCHATERPREF;

sub adjust_concats ($$) {
    my ($in, $out) = @_;

    say "Concatenating...\n";

    open REALREF, ">", $out;
    open PREREF, "<", $in;
    print REALREF (scalar <PREREF>); # <refset ... >
    while (<PREREF>) {
      next if( /^<refset.*>$/ or /^<\/refset>$/ );
      print REALREF;
    }
    print REALREF "</refset>";
    close PREREF;
    close REALREF;
}

if (not $SGMLMODE) {
  # Not being in SGML mode means that we've got the
  # hypothesis in line-per-sentence format.
  my $HYPFILE=rel2abs(pop @ARGV);

  say "Hypothesis is $HYPFILE\n";

  $PROCSRC="$DIRNAME/src.sgml";
  $PROCTST="$DIRNAME/tst.sgml";

  if ($REFSGMLMODE) {
    # If we're in refsgml mode, the reference was already
    # provided, so we can skip any processing we need to do.
    $PROCREF = rel2abs(shift @ARGV);
    say "Using SGML reference file : $PROCREF\n";
  } else {
    my %cols = ( );
    $PROCREF="$DIRNAME/ref.sgml";
    say "SGMLizing reference files ...\n";

    foreach my $ref (@ARGV) {
      my $reffile=rel2abs($ref);
      my $refname=rename_collisions(\%cols, basename($ref));

      say "    $refname : $ref \n";

      logsystem "$QUICKLY_SGMLIZE ref $refname < $reffile 2>&1 >> $PROCREF.pre "
           . ($KEEPWORK ? " | tee -a $PROCREF.warnings >&2 " : "" );
    }

    adjust_concats ("$PROCREF.pre", $PROCREF);
  }

  if ($needsrc) {
    say "SGMLizing hypothesis as source surrogate (for NIST)...\n";
    logsystem "$QUICKLY_SGMLIZE src evalsys < $HYPFILE 2>&1 > $PROCSRC"
         . ($KEEPWORK ? " | tee -a $PROCSRC.warnings >&2 " : "") ;
  }

  if ($ONLYASCII) {
    say "Stripping hypothesis of nonascii characters...\n";
    open HYP, "<", $HYPFILE;
    $HYPFILE = "$DIRNAME/hyp.strip";
    open HYPPRE, ">", $HYPFILE;
      # This regex works because the \S* on either side will be greedy
    while(<HYP>) { s/\S*[^[:ascii:]]+\S*/UNKWORD/g; print HYPPRE; }
    close HYPPRE;
    close HYP;
  }

  say "SGMLizing hypothesis...\n";
  logsystem "$QUICKLY_SGMLIZE tst evalsys < $HYPFILE 2>&1 > $PROCTST"
       . ($KEEPWORK ? " | tee -a $PROCTST.warnings >&2 " : "") ;
} else {
  if ($needsrc) {
    ($PROCREF, $PROCTST, $PROCSRC) = @ARGV;
  } else {
    ($PROCREF, $PROCTST) = @ARGV;
  }
}

# Deal with HATERp
if ($#HATERPCFGS >= 0 or $METRICS{'haterp'}) {
  if ( defined $HATERPSGML ) {
    $PROCHATERPREF = rel2abs($HATERPSGML);
  } else {

    $PROCHATERPREF="$DIRNAME/haterpref.sgml";

    say "Making reference file for HATERp ...\n";

    my %cols = ( );
    foreach my $reff (@ARGV) {
      my $refname=rename_collisions(\%cols, basename($reff));

      say "    $refname : $reff \n";

      my ($refbase, $refpart) = ($reff, "");
      ($refbase, $refpart) = ($1,$2) if ($reff =~ /^(.*)(\.\d+)$/);

      my $posf = $refbase.".tagged".$refpart;
      my $phxf = $refbase.".norm.phxout.tags_simple".$refpart;

      if (not -r $posf) {
        say "! Unable to read POS file $posf for reference $refname; this will impact"
           . " HATERp scores!";
      } elsif (not -r $phxf) {
        say "! Unable to read PHX file $phxf for reference $refname; this will impact"
           . " HATERp scores!";
      } else {
        my $haterpcmd = "$HATERP_MARKUP_MAKER --refbase=$refname $HATERPDIR/terp.classes.list $reff";
        if( $KEEPWORK ) {
          $haterpcmd .= " 2>>$PROCHATERPREF.errors"
        } else {
          $haterpcmd .= " 2>/dev/null";
        }
        $haterpcmd .= " >> $PROCHATERPREF";
        logsystem $haterpcmd;
      }
    }

    if (-s "$PROCHATERPREF.errors") {
      say "  !!! HATERp reference preprocessing raised errors.\n";
      say "  !!! Please investigate $PROCHATERPREF.errors .\n";
    }
  }
}

if ($DEBUG) {
  say "USING FILES:\n";
  say "  src : $PROCSRC\n";
  say "  ref : $PROCREF\n";
  say "  tst : $PROCTST\n";
  say "  hat : $PROCHATERPREF\n" if defined $PROCHATERPREF;
  say "  hac : $HATERPDIR/terp.param\n" if $METRICS{'haterp'};
  foreach my $hac (@HATERPCFGS) {
    say "  hac : $hac\n" ;
  }
}

sub _write_output_core ($$$$) {
    my ($fname, $mode, $system, $score) = @_;
    open WOCFILE, $mode, $fname or die "Unable to open $fname";
    print WOCFILE "${system} = ${score}\n";
    close WOCFILE;
}

sub write_output ($$$) {
  my ($human_sys, $system, $score) = @_;

  print "${human_sys} : ${score}\n";
  _write_output_core("${BLOODGOOD}/${system}.txt", ">", $system, $score)
    if(defined $BLOODGOOD);
  _write_output_core("${DIRNAME}/all.scores", ">>", $system, $score)
    if($KEEPWORK);
}


foreach my $MTEVER (keys %MTEVAL) {
  if ($METRICS{"mtevalv${MTEVER}bleu"} or $METRICS{"mtevalv${MTEVER}nist"}) {
    # BLEU and NIST evaluation
    say "Running MTEVAL V${MTEVER}...\n";

    my $MTECMD = $MTEVAL{$MTEVER} . " -r $PROCREF -s $PROCSRC -t $PROCTST";
    $MTECMD .= " -b " unless $METRICS{"mtevalv${MTEVER}nist"};
    $MTECMD .= " -n " unless $METRICS{"mtevalv${MTEVER}bleu"};
    $MTECMD .= " --international-tokenization " if $PRETOKEN;
    print CMDFILE $MTECMD, "\n\n" if $KEEPWORK;
    $MTECMD .= "| tee $DIRNAME/mteval${MTEVER}" if $KEEPWORK;
    my $MTEOUT = `$MTECMD | grep 'score .* for system'`;
    $MTEOUT =~ /NIST score = ([^ ]+)/ and write_output("NIST (MTEVAL V${MTEVER})","mtevalv${MTEVER}NIST",$1);
    $MTEOUT =~ /BLEU score = ([^ ]+)/ and write_output("BLEU (MTEVAL V${MTEVER})","mtevalv${MTEVER}BLEU",$1);
  }
}

if ($METRICS{'meteor'}) {
  # METEOR
  say "Running METEOR...\n";

  my $METEORCMD = "$METEOR $PROCTST $PROCREF -sgml";
  print CMDFILE $METEORCMD, "\n\n" if $KEEPWORK;
  logsystem "(cd $DIRNAME; $METEORCMD > meteor.out)";
  my $METEORSCORE=`cat $DIRNAME/evalsys-sys.score | awk '{ print \$3 }'`;
  chomp $METEORSCORE;
  write_output("METEOR", "METEOR", $METEORSCORE) if $METEORSCORE !~ /^\s*$/;
}

# Generate TER and related config files
sub ter_config_common_lines ($$) {
  my ($FH, $ishater) = @_;

  my $ref = $ishater ? $PROCHATERPREF : $PROCREF;
  print $FH "Reference File (filename) : $ref\n";
  print $FH "Hypothesis File (filename): $PROCTST\n";
  print $FH "Normalize (boolean)       : false\n" if $PRETOKEN;
  if($KEEPWORK) {
    print $FH "Output Formats (list)     : param nist pra html\n";
  } else {
    # If this is empty, TERp defaults.  Oof.
    print $FH "Output Formats (list)     : param\n";
  }
}

# Run something that looks like TER.
sub ter_run ($$$$) {
  my ($mode, $exec, $ishater, $conf) = @_;

  say "Running TER ${mode}...\n";

  my $fname = "$DIRNAME/${mode}.param";
  open CONF, ">", $fname;
  ter_config_common_lines(*CONF, $ishater);
  print CONF "Output Prefix (filename)  : $DIRNAME/${mode}.out.\n";
  close CONF;

  my $TERCMD = "$TERP_BASENAME/${exec} $conf $fname";
  if( $KEEPWORK ) {
    print CMDFILE $TERCMD, " # ", $mode, "\n";
    $TERCMD .= " 2>&1 | tee $DIRNAME/${mode}.out ";
  } else { 
    $TERCMD .= " 2>/dev/null";
  } 
  my $TEROUT = `$TERCMD | grep Total`;
  $TEROUT =~ /^Total TER:\s+([^\s]*)\s+.*$/ and write_output($mode,$mode,$1);
}

ter_run("TER", "terp_ter", 0, "") if $METRICS{'ter'};
ter_run("TERp", "terp", 0, "") if $METRICS{'terp'};
ter_run("TERpa", "terpa", 0, "") if $METRICS{'terpa'};
ter_run("HATERp", "terpa", 1, "$HATERPDIR/terp.param") if $METRICS{'haterp'};

# Run additional TERPa configurations
{
  my %aterpcol = ( );
  foreach my $tac (@TERPACFGS) {
    my $sfx = "_" . rename_collisions(\%aterpcol, basename($tac, ".param"));
    ter_run("TERpa$sfx", "terpa", 0, $tac);
  }
}

# Run HATERp configurations
{
  my %haccol = ( );
  foreach my $hac (@HATERPCFGS) {
    my $sfx = "_" . rename_collisions(\%haccol, basename($hac, ".param"));
    ter_run("HATERp$sfx", "terpa", 1, $hac);
  }
}

system "rm -r ${DIRNAME}" if not $KEEPWORK;

if ($KEEPWORK) {
  close CMDFILE;
  close OUTFILE;
}
