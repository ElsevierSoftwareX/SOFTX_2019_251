// Copyright (C) 2019 by Sten Ternström & Dennis J. Johansson, KTH Stockholm
// Released under European Union Public License v1.2, at https://eupl.eu
// *** EUPL *** //

Dolansky {
	*ar { arg in, decay, coeff;
		var peakPlus  = FOS.ar(PeakFollower.ar(in.max(0), decay), coeff, coeff.neg, coeff);
		var peakMinus = FOS.ar(PeakFollower.ar(in.neg.max(0), decay), coeff, coeff.neg, coeff);
		^Trig1.ar(SetResetFF.ar(peakPlus, peakMinus), 0);
	}
}

// This pseudo-UGen for the crest factor is cycle-synchronous
// and more correct than the built-in Crest UGen.
CrestCycles {
	*ar { arg in;
		var rmsInv, trig, out;

		rmsInv = RMS.ar(in, 10).reciprocal;
		trig = Dolansky.ar(in, 0.999, 0.99);
		out = RunningMax.ar(in.abs, Delay1.ar(trig))*rmsInv;
		^Latch.ar(out, trig)
	}
}


VRPSDVRP {
	classvar nameAnalyzeAudio = \sdAnalyzeAudio;

	*compile { | libname |

		///////////////////////////////////////////////////////////////////////////////////////////////////////
		// Analyze Audio SynthDef
		///////////////////////////////////////////////////////////////////////////////////////////////////////  ;
		SynthDef(nameAnalyzeAudio,
			{ | aiBusConditionedMic,
				coBusFrequency,
				coBusAmplitude,
				coBusClarity,
				coBusCrest |

				var in, inpow, amp, freq, crest, gate, hasFreq, out;

				in = In.ar(aiBusConditionedMic);
//				crest = Crest.kr(in, 500); // Crest may contain a library bug (abs rather than square)
				crest = CrestCycles.ar(in);

				// Mean-square over 30Hz, remove dips; - should ideally be cycle-synchronous
				// The median filter delay approximates the freq and condEGG delays
				inpow = Median.kr(17, WAmp.kr(in.squared, 0.033));

				// The following line serves only to guard agains true-zero audio in test files
				amp = Select.kr(InRange.kr(inpow, -1.0, 0.0), [0.5 * inpow.ampdb, DC.kr(-100)]);

				// Integrator brings down the HF
				// # freq, hasFreq = Pitch.kr(Integrator.ar(in, 0.995), execFreq: 20);
				// # freq, hasFreq = Tartini.kr(Integrator.ar(in, 0.995) /*, n: 512, overlap: 256*/);
				# freq, hasFreq = Tartini.kr(Integrator.ar(in, 0.995), n: 2048, k: 1024, overlap: 1024);
				freq = freq.cpsmidi;

				Out.kr(coBusFrequency, [freq]);
				Out.kr(coBusAmplitude, [amp]);
				Out.kr(coBusClarity, [hasFreq]);
				Out.kr(coBusCrest, [crest]);
			}
		).add(libname);
	}

	*analyzeAudio { | aiBusConditionedMic, coBusFrequency, coBusAmplitude, coBusClarity, coBusCrest ...args |
		^Array.with(nameAnalyzeAudio,
			[
				\aiBusConditionedMic, aiBusConditionedMic,
				\coBusFrequency, coBusFrequency,
				\coBusAmplitude, coBusAmplitude,
				\coBusClarity, coBusClarity,
				\coBusCrest, coBusCrest
			],
			*args
		);
	}
}