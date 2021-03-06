/**************
 Listing 1. Launching chucklib-livecode.
 **************/

\loadAllCl.eval;

// optional
\cllGui.eval;


/**************
 Listing 2. A quick techno-ish drumset.
 **************/

\loadAllCl.eval;
TempoClock.tempo = 124/60;

/hh.(\hardhh);
/hhh = ".-.-.-.-";
/hhh+

/drum.(\tightsnr);
/tsn = " - -";
/tsn+

/drum.(\deepkick);
/dk = "o| o| _o |";
/dk+

// mixing board
/makeEmptyMixer8.();
/hhh => MCG(0);
/tsn => MCG(1);
/dk => MCG(2);

/hhh/tsn/dk-


/**************
 Listing 3. Generators for drums.
 **************/

/hhh/tsn/dk+

// A
/tsn = "[ - -]::\ins(".", 2, 0.25)";

// B
/tsn = "[ - -]::\ins(".", 2, 0.5)";

// C
/tsn = "[ - -]::\ins(".", 2, 0.5)::\shift(".", 2, 0.25)";

// D (empty source, so, omitted)
/hhh = "\ins("-", 1, 0.5)::\ins(".", 7, 0.5)";

// E (one closed HH to fill start of bar)
/hhh = "[.]::\ins("-", 1, 0.5)::\ins(".", 6, 0.5)";

// F
/hhh = "[.]::\ins("-", 1, 0.5)::\ins(".", 6, 0.5)::\ins(".", 2, 0.25)";

// G
/hhh = "\fork("|\ins("-", 1, 0.5)||x")::\ins(".", 7, 0.5)::\ins(".", 2, 0.25)";

/hhh/tsn/dk-


/**************
 Listing 4. Adding sound effects to a simple beat.
 **************/

\loadAllCl.eval;  // If you haven't already done this
TempoClock.tempo = 124/60;

// The beat
BP(#[tk, clp, hhh, tsn]).free;  // Clean up first
/drum.(\tightkick); /drum.(\clap); /hh.(\hardhh);
/tk = "o|| o|";
/clp = " -  ";
/hhh = "........";

/tk/clp/hhh+

/drum.(\tightsnr);
/tsn = "|||.";

/tsn+

/clp = "|-|| .  ";
/tsn = "|||.  .";

// make the effects
/make(bufBP:mch(set:\machine));
/mch = "| -||";
/mch+

/mch = "| -| , ,|";

/tk/clp/hhh/tsn/mch-;


/**************
 Listing 5. Bassline template.
 **************/

/changeKey.(\dmixo);
/make(pbsVC:pbs/melBP:bs(octave:3));

/bs = "1_| 1.|  7~4|x";

/bs+
/bs-


/**************
 Listing 6. Chord-playing template.
 **************/

/make(anapadVC:pad/chordBP:ch(chords:\one));
/ch = "87~05";
/ch+

VC(\pad).gui

MBM(0)[\two] => BP(\ch);

MBM(0)[\smallch] => BP(\ch);

/ch-


/**************
 Listing 7. Example of arpeggiator usage.
 **************/

/make(fmclavVC:fmc/arpegBP:arp(chords:\bigch));

// These are indices, from the top down, into the current chord.
/arp = "1234";

/arp+

// Add some lower notes as a second layer.
// Accent articulates the start of the bar.
/arp = "[1>234]::\ins("456", 6, 0.25)";

// Extend the second layer higher.
/arp = "[1>234]::\ins("23456", 7, 0.25)";

// Use wildcards to substitute a sequential pattern.
/arp = "[1>234]::\ins("*", 7, 0.25)::\seq("65432")";

// Change the harmony's top note every bar.
/arp..top = "[*]::\seq("5'6'3'2'")";

// Skip: Play dyads instead of single notes.
/arp..skip = "2";

// Skip can also accent specific notes.
/arp..skip = "20  |20  |20  |20  ";

// same, but algorithmic
/arp..skip = "[2222]::\choke(0.25, "0")";

// Add a second process to change the chord root.
// After this, you should hear tonic, dominant
// and subdominant functions.
// No instrument -- this is for data only.
/make(melBP:root(bassID:\bass));
/root = "[*]::\seq("154")";
/root+

/arp/root-


/**************
 Listing 8. Phrase selection for drum fills.
 **************/

TempoClock.tempo = 124/60;

/drum.(\tightkick); /drum.(\tightsnr); /hh.(\thinhh);

/tk = "oooo";
/tsn = " - -";
/thh = "[.]::\ins("-", 1, 0.5)::\ins(".", 6, 0.5)";

/tk/tsn/thh+

/tk.fill = "o||   _|o __";

// mid-bar source string:
// in this position, it fills 3 eighth-notes
/tsn.fill = "|-| [ - ]::\ins(".", 4, 0.25)|";

/tk = (main*3.fill); /tsn = (main*3.fill);

/tk/tsn/thh-


/**************
 Listing 9. Multi-bar bassline.
 **************/

// If the bass doesn't exist, first do this:
/make(pbsVC:pbs/melBP:bs(octave:3));

/bars.(\bs, 2, \a);

/bs.a0 = "1>|4~5~7 | 4~|3'~";
/bs.a1 = " 5>~|6| 4~| 3";

/setupbars.(\bs, 2, \b);

/bs.b0 = "9>.9.9 | 4'~|   3'|8~7~8~ ";
/bs.b1 = "  33.| 4.5~ | 431.|6.6. 6.";

// short form of /setm.(\bs, 2, \b)
/bs = (b**2);

/bs+
/bs-


/**************
 Listing 10. Defining a simple cll process as a factory.
 **************/

(
(
defaultName: \beep,
make: { |name|
   PR(\abstractLiveCode).chuck(BP(name), nil, (
      userprep: {
         ~buf = Buffer.read(
            s, Platform.resourceDir +/+ "sounds/a11wlk01.wav",
            4982, 10320
         );
         ~defaults[\bufnum] = ~buf;
         SynthDef(\buf1, { |out, bufnum, pan, amp, time = 1|
            var sig = PlayBuf.ar(1, bufnum),
            eg = EnvGen.kr(
               Env.linen(0.02,
                  min(time, BufDur.ir(bufnum) - 0.04), 0.02),
               doneAction: 2
            );
            Out.ar(out, Pan2.ar(sig, pan, amp * eg));
         }).add;
      },
      userfree: {
         ~buf.free;
      },
      defaultParm: \amp,
      parmMap: (
         amp: ($.: 0.1, $-: 0.4, $^: 0.8),
         pan: (
            $<: -0.9, $>: 0.9,
            $(: -0.4, $): 0.4,
            $-: 0
         )
      ),
      defaults: (instrument: \buf1),
      postDefaults: Pbind(
         \time, (Pkey(\dur) * 0.6 / Pfunc { ~clock.tempo }).clip(0.04, 0.2)
      )
   ));
}, type: \bp) => Fact(\beepBP);
)


/**************
 Listing 11. Using the cll process factory in a performance.
 **************/

\loadCl.eval;  // or \loadAllCl.eval;
TempoClock.tempo = 2;

/make(beepBP);  // defaultName is 'beep' so you get BP(\beep)
/beep = "^|.. .| .- | .  ";  // "Set pattern"
/beep+;  // start it

/beep..pan = "<><><><>";  // change something

/make(beepBP:beep2);  // ':' overrides defaultName
/beep2 = " ..-| .^ |. ..| .";
/beep2+

/beep..pan = "<";
/beep2..pan = ">";

/beep/beep2-;

/beep(free); /beep2(free);


/**************
 Listing 12. Template for the parameter map.
 **************/

parmMap: (
   parmName: (
      char: value,
      char: value,
      char: value...
   ),
   parmName: (...)
)


/**************
 Listing 13. How to write arrays in the parameter map.
 **************/

parmMap: (
   freqs: (
      $2: [200, 300, 400],
   ),
   parmName: (...)
)


/**************
 Listing 14. Arrays for multiple-parameter setting using one cll parameter.
 **************/

parmMap: (
   filt: (
      alias: [\ffreq, \rq],
      $x: [2000, 0.05]
   )
)


/**************
 Listing 15. Cll statements, one by one or as a batch.
 **************/

// run one at a time
/kick.fotf = "----";
/snare.bt24 = " - -";

// or as a batch
/kick.fotf = "----"; /snare.bt24 = " - -";


/**************
 Listing 16. Syntax template for the Set pattern statement.
 **************/

/proc.phrase.parm = quant"string";


/**************
 Listing 17. Multiple parameters with different timing.
 **************/

/x = "--";
/x.filt = "ab c";  // "c" is not heard

/x = "-|-  -";  // now "c" is heard on beat 4.5


/**************
 Listing 18. A retro acid-house bassline, demonstrating pitch notation.
 **************/

// Initialization code
(
SynthDef(\sqrbass, { |out, freq = 110, gate = 1,
   freqMul = 1.006, amp = 0.1,
   filtMul = 3, filtDecay = 0.12, ffreq = 2000, rq = 0.1,
   lagTime = 0.1|
   var sig = Mix(
      Pulse.ar(Lag.kr(freq, lagTime) * [1, freqMul], 0.5)
   ) * amp,
   filtEg = EnvGen.kr(
      Env([filtMul, filtMul, 1], [0.005, filtDecay], \exp),
      gate
   ),
   ampEg = EnvGen.kr(Env.adsr(0.01, 0.08, 0.5, 0.1),
      gate, doneAction: 2);
   sig = RLPF.ar(sig, (ffreq * filtEg).clip(20, 20000), rq);
   Out.ar(out, (sig * ampEg).dup);
}).add;

(
keys: #[master],  // ~master comes from \loadAllCl.eval
defaultName: \sqrbs,
initLevel: -12.dbamp,
argPairs: #[amp, 0.5],
make: { |name|
   var out;
   ~target = MixerChannel(name, s, 2, 2, ~initLevel, outbus: ~master);
   out = Voicer(5, \sqrbass, target: ~target);
   out.mapGlobal(\ffreq, nil, 300, \freq);
   out.mapGlobal(\rq, nil, 0.2, \myrq);
   out.mapGlobal(\filtMul, nil, 8, [1, 12]);
   out
},
free: { ~target.free },
type: \vc) => Fact(\sqrbsVC);
)

// Performance code:
\loadAllCl.eval;
TempoClock.tempo = 132/60;
Mode(\fsloc) => Mode(\default);

/make(sqrbsVC/melBP:bs(octave:3));
/bs = "1_  1.|5~3_9.4.|7.2~4_5'.|5_8~2_4.";
/bs+;

/bs-;


/**************
 Listing 19. Pitch notation in PR(\abstractLiveCode); not generally recommended.
 **************/

// Use the same SynthDef as in the previous example

BP(\acid).free;
PR(\abstractLiveCode).chuck(BP(\acid), nil, (
   event: (eventKey: \default),
   alwaysReset: true,
   defaultParm: \degree,
   parmMap: (
      degree: (isPitch: true),
   ),
   defaults: (
      ffreq: 300, filtMul: 8, rq: 0.2,
      octave: 3, root: 6, scale: Scale.locrian.semitones
   ),
   postDefaults: PmonoArtic(\sqrbass,
      \dummy, 1
   )
));

TempoClock.tempo = 132/60;
)

/acid = "1_  1.|5~3_9.4.|7.2~4_5'.|5_8~2_4.";

/acid+;
/acid-;


/**************
 Listing 20. Syntax template for "Set pattern" phrase selection.
 **************/

/proc = (group...);


/**************
 Listing 21. Nested phrase-selection groups.
 **************/

((a%4|b)*4.(a|b%4)*2)


/**************
 Listing 22. Syntax template for make statements.
 **************/

/make(factory0:targetName0(parameters0)/factory1:targetName1(parameters1)/...);

// Or, with autoGui
/make*(factory0:targetName0(parameters0)/factory1:targetName1(parameters1)/...);


/**************
 Listing 23. Example of the make statement.
 **************/

(
// THIS PART IN THE INIT FILE
(
defaultName: \demo,
octave: 5,  // a default octave
make: { |name|
	PR(\abstractLiveCode).chuck(BP(name), nil, (
		event: (eventKey: \default),
		defaultParm: \degree,
		parmMap: (degree: (isPitch: true)),
		// Here, the Factory transmits ~octave
		defaults: (octave: ~octave)
	));
}, type: \bp) => Fact(\demoBP);
)

// DO THIS IN PERFORMANCE
/make(demoBP:dm(octave:6));  // :dm overrides defaultName

/dm = "1353427,5,";
/dm+;
/dm-;

/dm(free);


/**************
 Listing 24. Syntax template for passthrough statements.
 **************/

// This...
/snr(clock = ~myTempoClock);

// ... is the same as running:
BP(\snr).clock = ~myTempoClock;

// Or...
/VC.bass(releaseAll);  // VC(\bass).releaseAll;


/**************
 Listing 25. Syntax template for Chuck statements.
 **************/

// This...
/snr => MCG(0);

// ... is the same as running:
BP(\snr) => MCG(0);

// Or...
/VC.keys => MCG(0);  // VC(\keys) => MCG(0);


/**************
 Listing 26. Syntax template for func-call statements.
 **************/

/func.(arguments);

// e.g.:
/bars.(\proc, 2, \a);


/**************
 Listing 27. Syntax template for copy/transfer statements.
 **************/

/proc.phrase*n -> newPhrase;  // copy

/proc.phrase*n ->> newPhrase;  // transfer


/**************
 Listing 28. Demonstration of "Show pattern" statements.
 **************/

/snr.a = " - -";

/snr.a -> b;

/snr.b   // now hit ctrl-return at the end of this line

// the line magically changes to
/snr.b = " - -";


/**************
 Listing 29. Common initialization sequence, using helper functions.
 **************/

/make(kick);
/bars.(\kick, 2, \a);

// the following lines are automatically inserted
/kick.a0 = "";
/kick.a1 = "";


/**************
 Listing 30. Isorhythmic cycles with generators.
 **************/

(
BP(\y).free;
PR(\abstractLiveCode).chuck(BP(\y), nil, (
	event: (eventKey: \default),
	defaultParm: \degree,
	parmMap: (degree: (isPitch: true))
));
)

TempoClock.tempo = 140/60;

/y = "12 4| 5 6| 12 |45";  // A

/y+;

/y = "[** *| * *| ** |**]::\seq("12456", "*")";  // B

/y = "[** *| * *| ** |**]::\seq("12456", "*")::\ins("*", 7, 0.25)";  // C

/y = "[** *| * *| ** |**]::\seq("12456", "*")::\ins("*", 7, 0.25)::\seq("6,214", "*")";  // D

/y-;


/**************
 Listing 31. Interaction between generator syntax and "set pattern" rhythmic notation.
 **************/

// 1. Chain starts on the downbeat and occupies the whole bar.
/y = "[1,]::\ins("*", 3, 0.5)::\rand("13467", "*")";

/y+;

// 2. Chain starts on beat 2
// Note that a generator source can appear
// anywhere within the bar!
/y = "1,|[6,]::\ins("*", 3, 0.5)::\rand("13467", "*")||";

// 3. Chain starts on the 2nd 16th-note of beat 2
// Here, '6,' occupies time and is not a generator source.
// So it is not bracketed.
/y = "1,|6,\ins("*", 3, 0.5)::\rand("13467", "*")  ||";

// 4. Chain starts on the 2nd 16th-note of beat 2
// and stop on the 'and' of 4
/y = "1,|6,\ins("*", 3, 0.5)::\rand("13467", "*")  || x";

/y-;


/**************
 Listing 32. Usage of \rot() generator.
 **************/

// Reich, "Piano Phase"-ish

(
BP(\y).free;
PR(\abstractLiveCode).chuck(BP(\y), nil, (
   event: (eventKey: \default, pan: -0.6),
   defaultParm: \degree,
   parmMap: (degree: (isPitch: true))
));

BP(\z).free;
PR(\abstractLiveCode).chuck(BP(\z), nil, (
   event: (eventKey: \default, pan: 0.6),
   defaultParm: \degree,
   parmMap: (degree: (isPitch: true))
));
)

TempoClock.setMeterAtBeat(3, TempoClock.nextBar);
TempoClock.tempo = 112/60;

/y = "[*^*^*^*^*^*^]::\seq("268", "*")::\seq("37", "^")";

/z = "[*^*^*^*^*^*^]::\seq("268", "*")::\seq("37", "^")";

/y/z+;

/z = "[*^*^*^*^*^*^]::\seq("268", "*")::\seq("37", "^")::\rot(-0.25)";

/z = "[*^*^*^*^*^*^]::\seq("268", "*")::\seq("37", "^")::\rot(-0.5)";

/z = "[*^*^*^*^*^*^]::\seq("268", "*")::\seq("37", "^")::\rot(-0.75)";

/y/z-;


/**************
 Listing 33. Usage of \rDelta() generator.
 **************/

// equal distribution
// but more total time spent on longer notes
/y = "\rDelta("*", 1, 8, , , `lin)::\wrand("\xrand("12345")\xrand("3'4'5'6'")", 2, 1)";

/y+

// equal total time spent on longer notes vs shorter
/y = "\rDelta("*", 1, 8, , , `exp)::\wrand("\xrand("12345")\xrand("3'4'5'6'")", 2, 1)";

// Phprand
/y = "\rDelta("*", 1, 8, , , `hp)::\wrand("\xrand("12345")\xrand("3'4'5'6'")", 2, 1)";

// Plprand
/y = "\rDelta("*", 1, 8, , , `lp)::\wrand("\xrand("12345")\xrand("3'4'5'6'")", 2, 1)";

// beta distribution: similar problem as `lin
/y = "\rDelta("*", 1, 8, , , `beta, 0.2, 0.2)::\wrand("\xrand("12345")\xrand("3'4'5'6'")", 2, 1)";

// expb: "exponentialized" beta distribution
/y = "\rDelta("*", 1, 8, , , `expb, 0.2)::\wrand("\xrand("12345")\xrand("3'4'5'6'")", 2, 1)";

/y-


/**************
 Listing 34. Usage of \ramp() generator.
 **************/

/y = "\ramp("*", 1, 0.2, 1, `exp)::\pitch("*", "2", 0, 0)";
/y+

// randomly choose accel or decel
/y = "\ramp("*", 1, 0.2, {2.rand}, `exp)::\pitch("*", "2", 0, 0)";

// alternate between accel and decel
/y = "\ramp("*", 1, 0.2, {Pseq([0, 1], inf)}, `exp)::\pitch("*", "2", 0, 0)";

/y = "\ramp("*", 1, 0.2, 0.5, `exp)::\pitch("*", "2", 0, 0)";

// pulls 1 -> 0.2 curve to the right, biasing long durations
/y = "\ramp("*", 1, 0.2, 0.5, 2)::\pitch("*", "2", 0, 0)";

// pulls 1 -> 0.2 curve to the left, biasing short durations
/y = "\ramp("*", 1, 0.2, 0.5, -2)::\pitch("*", "2", 0, 0)";

/y = "\ramp("*", 1, 0.2, 0.5, -4)::\pitch("*", "2", 0, 0)";

/y-


/**************
 Listing 35. Using the \unis() generator to coordinate two harmony processes.
 **************/

TempoClock.tempo = 124/60;
/make(anapadVC:pad/chordBP:ch(chords:\bigch));
/ch(leadTime = 0.01);
/ch = "\delta("*", 0.5, 0, 1, 2, 2)::\shuf("0976")";
/ch+

/make(pulseLeadVC:pl/arpegBP:arp(chords:\bigch));
/arp = "\unis(`note, "1", `ch)::\ins("*", 16, 0.25)::\seq("23456")::\artic(".")";
/arp..top = "\unis(`note, , `ch)";
/arp..acc = "\unis(`top, ">", `arp)::\choke(0.25, "-")";
/arp+

VC(\pad).v.gui; VC(\pl).v.gui;  // adjust filters


/**************
 Listing 36. Usage of \ser() generator.
 **************/

/y = "\ins("*", 8, 0.5)::\seq("\seq*2..4("1234")\seq("875")")";
/y+

/y = "\ins("*", 8, 0.5)::\seq("\ser*2..4("1234")\seq("875")")";

/y-


/**************
 Listing 37. Usage of \pdefn() generator.
 **************/

Pdefn(\y, Pn(Pseries(0, 1, 8), inf).collect { |d| SequenceNote(d, nil, 0.9) });
   
/y.a0 = "[*]::\ins("*", 2, 0.5)::\pdefn(`y, "*")";
/y.a1 = "\ins("*", 3, 0.5)::\pdefn(`y, "*")";
/y = (a**2);


/**************
 Listing 38. Usage of \gdefn() generator.
 **************/

(
BP(\x).free;
PR(\abstractLiveCode).chuck(BP(\x), nil, (
   event: (play: { ~x.debug(~collIndex) }),
   defaultParm: \x,
   parmMap: (
      x: ($0: 0, $1: 1, $2: 2, $3: 3, $4: 4)
   ),
));

BP(\y).free;
PR(\abstractLiveCode).chuck(BP(\y), nil, (
   event: (play: { ~x.debug(~collIndex) }),
   defaultParm: \x,
   parmMap: (
      x: ($0: 0, $1: 1, $2: 2, $3: 3, $4: 4)
   ),
));

BP(\x).leadTime = 0.01;
Pdefn(\x, Pseq("01234", inf).trace(prefix: "pdefn: "));
)

/x = "[****]::\gdefn(`x, "*", 1)";
/y = "[****]::\gdefn(`x, "*", 1)";
/x/y+
/x/y-


/**************
 Listing 39. Usage of \pitch() and \pitch2 generators.
 **************/

TempoClock.tempo = 140/60;
Mode(\cphr) => Mode(\default);

/make(pulseLeadVC:pl/melBP:pl);

// Every "6,7," descending pattern starts at the previous given note
/pl = "[5'>| 3'>|4'>  2'>|]::\ins("*", 5..10, 0.25)::\pitch("*", "6,7,", 8, 10, ".")";
/pl+

// The first descent starts from "5'" and keeps going down through the bar
/pl = "[5'>| 3'>|4'>  2'>|]::\ins("*", 5..10, 0.25)::\pitch2("*", "6,7,", 8, 10, ".")";

// One \pitch() stream;
// another interlocking \pitch2() stream in a higher register
/pl = "[5'>| 3'>|4'>  2'>|]::\ins("*", 3..7, 0.25)::\ins("@", 3..8, 0.25)::\pitch("*", "6,7,", 8, 10, ".")::\pitch2("@", "6,7,23", 14, 18, ".", 0)";

/pl-


/**************
 Listing 40. Complex sequencing with sub-generators.
 **************/

(
BP(\y).free;
PR(\abstractLiveCode).chuck(BP(\y), nil, (
	event: (eventKey: \default),
	defaultParm: \degree,
	parmMap: (degree: (isPitch: true))
));
)

TempoClock.tempo = 140/60;

/y = "\ins("*", 8, 0.5)::\seq("123")";

/y+

// can repeat (like Prand)
/y = "\ins("*", 8, 0.5)::\rand("\seq("123")\seq("7854")\seq("26,")")";

// no repeats (like Pxrand)
/y = "\ins("*", 8, 0.5)::\xrand("\seq("123")\seq("7854")\seq("26,")")";

// 2-5random notes after 6,
// note here that articulation/transposition applies to sub-choices
/y = "\ins("*", 8, 0.5)::\xrand("\seq("123")\seq("7854")\seq("26,\xrand*2..5("34567")::\xpose("1'")::\artic(".")")")";

// also, articulations and transposition can be streamed this way
/y = "\ins("*", 8, 0.5)::\seq("123")::\artic(\seq("\seq*3("_")\seq*7(".")"))";

/y-


/**************
 Listing 41. Usage of \artic() generator.
 **************/

TempoClock.tempo = 140/60;
Mode(\cphr) => Mode(\default);
/make(pulseLeadVC:pl/melBP:pl);

// No wildcards, all notes match: All are staccato
/pl = "[1574]::\artic(".")";
/pl+

// Wildcard, but none of the notes came from a
// wildcard operation, so none of them match.
/pl = "[1574]::\artic(".", "*")";

// Notes were inserted by \seq operating on "*";
// all notes match.
/pl = "[****]::\seq("1574")::\artic(".", "*")";

// Two layers of notes with different articulations
/pl = "[****]::\seq("1574")::\ins("@", 4..8, 0.25)::\shuf("1'2'3'4'5'6'", "@")::\artic(">.", "*")::\artic("~", "@")";

/pl-


/**************
 Listing 42. Usage of \xpose() generator.
 **************/

TempoClock.tempo = 140/60;
Mode(\cphr) => Mode(\default);
/make(pulseLeadVC:pl/melBP:pl);

/pl = "\ins("*", 16, 0.25)::\seq("12345432")";
/pl+

// Easy octave displacement.
/pl = "\ins("*", 16, 0.25)::\seq("12345432")::\xpose("1111,1'1''")";
/pl-


/**************
 Listing 43. Usage of \fork() generator.
 **************/

/y = "\ins("*", 10, 0.25)::\fork("\seq("13", "*")\seq("14", "*")")";

/y = "\ins("1,", 10, 0.25)::\fork(" \seq("13", "1,")x\seq("14", "1,")")";


/**************
 Listing 44. Cll statement regular expression templates.
 **************/

   ~statements = [
      \clMake -> "^ *make\\(.*\\)",
      \clFuncCall -> "^ *`id\\.\\(.*\\)",
      \clPassThru -> "^ *([A-Z][A-Za-z0-9_]*\\.)?`id\\(.*\\)",
      \clChuck -> "^ *([A-Z][A-Za-z0-9_]*\\.)?`id *=>.*",
      \clPatternSet -> "^ *`id(\\.|`id|`id\\*[0-9]+)* = .*",
      \clGenerator -> "^ *`id(\\.|`id)* \\*.*",
      // harder match should come first
      \clXferPattern -> "^ *`id(\\.`id)?(\\*`int)? ->>",
      \clCopyPattern -> "^ *`id(\\.`id)?(\\*`int)? ->",
      \clStartStop -> "^([/`spc]*`id)+[`spc]*[+-]",
      \clPatternToDoc -> "^ *`id(\\.|`id)*[`spc]*$"
   ];


/**************
 Listing 45. Regular expression macros for SC language tokens.
 **************/

   ~tokens = (
      al: "A-Za-z",
      dig: "0-9",
      id: "[A-Za-z][A-Za-z0-9_]*",
      int: "(-[0-9]+|[0-9]+)",
      // http://www.regular-expressions.info/floatingpoint.html
      float: "[\\-+]?[0-9]*\\.?[0-9]+([eE][\\-+]?[0-9]+)?",
      spc: "    "  // space, tab, return
   );


/**************
 Listing 46. Template for cll statement handlers.
 **************/

Proto {
   ~process = { |code|
      // parse 'code' and build the SC language statement(s)...
      translatedStatement  // return value
   };
} => PR(\clMyNewStatement);


/**************
 Listing 47. Adding a function into PR(\chucklibLiveCode).
 **************/

PR(\chucklibLiveCode).clMyNewStatement = { |code|
   // parse 'code' and build the SC language statement(s)...
   translatedStatement  // return value
};
