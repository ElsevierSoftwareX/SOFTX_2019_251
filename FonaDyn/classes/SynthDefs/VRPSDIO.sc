// Copyright (C) 2019 by Sten Ternström & Dennis J. Johansson, KTH Stockholm
// Released under European Union Public License v1.2, at https://eupl.eu
// *** EUPL *** //
VRPSDIO {
	classvar nameLiveInput = \sdLiveInput;
	classvar nameDiskInput = \sdDiskInput;
	classvar nameEchoMicrophone = \sdEchoMicrophone;
	classvar nameWriteAudio = \sdWriteAudio;
	classvar nameWriteCycleDetectionLog = \sdWriteCycleDetectionLog;
	classvar nameWritePoints = \sdWritePoints;
	classvar nameWriteSampEn = \sdWriteSampEn;
	classvar nameWriteFreqAmp = \sdWriteFrequencyAmplitude;
	classvar nameWriteLog = \sdWriteLog;
	classvar nameWriteGates = \sdWriteGates;
	classvar nameRecordExtraChannels = \sdRecordExtraChannels;

	/* FIR filter additions	*/

	classvar lpBuffer = nil;
	classvar hpBuffer = nil;

	*loadCoeffs {
		var lpCoeffs, hpCoeffs;

		/* FIR coeffs generated in Matlab for lowpass @ 10 kHz */
		lpCoeffs = VRPSDIO.getCoeffs(2);
		lpBuffer !? { lpBuffer.free; lpBuffer = nil };
		lpBuffer = Buffer.sendCollection(Server.default, lpCoeffs, 1, -1,
			{ /* |b| (b.numFrames.asInteger.asString + 'lpCoeffs loaded.').postln */ });

		/* FIR coeffs generated in Matlab for highpass @ 100 Hz */
		hpCoeffs = VRPSDIO.getCoeffs(1);
		hpBuffer !? { hpBuffer.free; hpBuffer = nil };
		hpBuffer = Buffer.sendCollection(Server.default, hpCoeffs, 1, -1,
			{ /* |b| (b.numFrames.asInteger.asString + 'hpCoeffs loaded.').postln */ } );
	}


	*compile { | libname, triggerIDEOF, triggerIDClip, mainGroupId, nHarmonics, calTone=false, logRate=0, arrayRecordExtraInputs |
		this.loadCoeffs;

		///////////////////////////////////////////////////////////////////////////////////////////////////////
		// Live Input SynthDef
		///////////////////////////////////////////////////////////////////////////////////////////////////////

		SynthDef(nameLiveInput,
			{ | aiBusMic, aiBusEGG, aoBusMic, aoBusEGG, aoBusConditionedMic, aoBusConditionedEGG |
				var inMic, inEGG, micCond, eggCond, bClip;

				inMic = SoundIn.ar(aiBusMic);       // Get input from a live source
				inEGG = SoundIn.ar(aiBusEGG);

				// Detect when EGG signal is too large
				bClip = PeakFollower.ar(inEGG.abs.madd(1, 0.005), 0.9999) - 1;
				SendTrig.ar(bClip, triggerIDClip, bClip);			    // Tell main about clipping

				micCond = HPF.ar(inMic, 30);				            // HPF +12 db/oct to remove rumble
				eggCond   = Convolution2.ar(inEGG, hpBuffer.bufnum, 0, 1024);	// HP @100 Hz
				eggCond	= Median.ar(9, eggCond);											// suppress EG2 "crackle"
				eggCond = Convolution2.ar(eggCond, lpBuffer.bufnum, 0, lpBuffer.numFrames);	// LP @10 kHz

				Out.ar(aoBusMic, [inMic]);                              // Feed the raw input to aoBusMic
				Out.ar(aoBusEGG, [inEGG]);                              // Feed the raw input to aoBusEGG
				Out.ar(aoBusConditionedMic, [micCond]);
				Out.ar(aoBusConditionedEGG, [eggCond]);
			}
		).add(libname);

		///////////////////////////////////////////////////////////////////////////////////////////////////////
		// Disk Input SynthDef
		///////////////////////////////////////////////////////////////////////////////////////////////////////

		SynthDef(nameDiskInput,
			{ | iBufferDisk, aoBusMic, aoBusEGG, aoBusConditionedMic, aoBusConditionedEGG |
				var inMic, inEGG, micCond, eggCond, eofGate;

				#inMic, inEGG = DiskIn.ar(2, iBufferDisk);		// Add a Done.kr to stop on end-of-file

				eofGate = Done.kr([inMic, inEGG]);
				SendTrig.kr(eofGate, triggerIDEOF, -1);  // Notify about end-of-file
				Pause.kr( 1 - eofGate, mainGroupId ); // Pause the main group (all synths)

				micCond = HPF.ar(inMic, 30);				    // HPF +12 db/oct to remove rumble
				eggCond   = Convolution2.ar(inEGG, hpBuffer.bufnum, 0, 1024);	// HP @100 Hz
				eggCond	= Median.ar(9, eggCond);												// suppress EG2 "crackle"
				eggCond = Convolution2.ar(eggCond, lpBuffer.bufnum, 0, lpBuffer.numFrames);	// LP @10 kHz

				Out.ar(aoBusMic, [inMic]);                      // Feed the raw input to aoBusMic
				Out.ar(aoBusEGG, [inEGG]);                      // Feed the raw input to aoBusEGG
				Out.ar(aoBusConditionedMic, [micCond]);
				Out.ar(aoBusConditionedEGG, [eggCond]);
			}
		).add(libname);

		///////////////////////////////////////////////////////////////////////////////////////////////////////
		// Echo Microphone SynthDef
		///////////////////////////////////////////////////////////////////////////////////////////////////////

		if (calTone==true, {
			SynthDef(nameEchoMicrophone,
				{ | aiBusMic, aoBusSpeaker |
					Out.ar(aoBusSpeaker, [In.ar(aiBusMic), SinOsc.ar(250, 0, 0.3)]);
				}
			).add(libname);
		},{
			SynthDef(nameEchoMicrophone,
				{ | aiBusMic, aoBusSpeaker |
					Out.ar(aoBusSpeaker, In.ar(aiBusMic) ! 2);
				}
			).add(libname);
		});

		///////////////////////////////////////////////////////////////////////////////////////////////////////
		// Write Audio SynthDef
		///////////////////////////////////////////////////////////////////////////////////////////////////////

		SynthDef(nameWriteAudio,
			{ | aiBusMic, aiBusEGG, oBufferAudio |
				GatedDiskOut.ar(oBufferAudio, 1, [ In.ar(aiBusMic), In.ar(aiBusEGG) ]);
			}
		).add(libname);

		///////////////////////////////////////////////////////////////////////////////////////////////////////
		// Write the EGG signal and the new cycle markers
		///////////////////////////////////////////////////////////////////////////////////////////////////////

		SynthDef(nameWriteCycleDetectionLog,
			{ | aiBusEGG, aiBusGateCycle, oBufferLog |
				GatedDiskOut.ar(oBufferLog, 1, [In.ar(aiBusEGG), In.ar(aiBusGateCycle)]);
			}
		).add(libname);

		///////////////////////////////////////////////////////////////////////////////////////////////////////
		// Write points
		///////////////////////////////////////////////////////////////////////////////////////////////////////

		SynthDef(nameWritePoints,
			{ | aiBusGate,
				aiBusDeltaAmplitudeFirst,
				aiBusDeltaPhaseFirst,
				oBuffer |

				var gate = In.ar(aiBusGate);
				var damps = In.ar(aiBusDeltaAmplitudeFirst, nHarmonics);
				var dphases = In.ar(aiBusDeltaPhaseFirst, nHarmonics);

				GatedDiskOut.ar(oBuffer, gate, damps ++ dphases);
			}
		).add(libname);

		///////////////////////////////////////////////////////////////////////////////////////////////////////
		// Write SampEn
		///////////////////////////////////////////////////////////////////////////////////////////////////////

		SynthDef(nameWriteSampEn,
			{ | aiBusGate,
				aiBusSampEn,
				oBuffer |

				var gate = In.ar(aiBusGate);
				var sampen = In.ar(aiBusSampEn);

				GatedDiskOut.ar(oBuffer, gate, [sampen]);
			}
		).add(libname);

		///////////////////////////////////////////////////////////////////////////////////////////////////////
		// Write Frequency Amplitude
		///////////////////////////////////////////////////////////////////////////////////////////////////////

		SynthDef(nameWriteFreqAmp,
			{ | aiBusGate,
				aiBusFrequency,
				aiBusAmplitude,
				oBuffer |

				var gate = In.ar(aiBusGate);
				var freq = In.ar(aiBusFrequency);
				var amp = In.ar(aiBusAmplitude);

				GatedDiskOut.ar(oBuffer, gate, [freq, amp]);
			}
		).add(libname);

		///////////////////////////////////////////////////////////////////////////////////////////////////////
		// Write Log
		///////////////////////////////////////////////////////////////////////////////////////////////////////

		SynthDef(nameWriteLog,
			{ | aiBusGate,
				aiBusTimestamp,
				aiBusFrequency,
				aiBusAmplitude,
				aiBusClarity,
				aiBusCrest,
				aiBusClusterNumber,
				aiBusSampEn,
				aiBusIcontact,
				aiBusDEGGmax,
				aiBusQcontact,
				aiBusAmplitudeFirst,
				aiBusPhaseFirst,
				oBuffer |

				var gate = Select.ar((logRate > 0).asInteger, [In.ar(aiBusGate), LFPulse.ar(logRate, 0, 0)]);
				var time = In.ar(aiBusTimestamp);
				var freq = In.ar(aiBusFrequency);
				var amp = In.ar(aiBusAmplitude);
				var clarity = In.ar(aiBusClarity);
				var crest = In.ar(aiBusCrest);
				var cluster_number = In.ar(aiBusClusterNumber);
				var sampen = In.ar(aiBusSampEn);
				var icontact = In.ar(aiBusIcontact);
				var dEGGmax = In.ar(aiBusDEGGmax);
				var qContact = In.ar(aiBusQcontact);
				var amps = In.ar(aiBusAmplitudeFirst, nHarmonics+1);
				var phases = In.ar(aiBusPhaseFirst, nHarmonics+1);
				GatedDiskOut.ar(oBuffer, gate, [time, freq, amp, clarity, crest, cluster_number, sampen, icontact, dEGGmax, qContact] ++ amps ++ phases);
			},
		).add(libname);

		///////////////////////////////////////////////////////////////////////////////////////////////////////
		// Write Gates
		///////////////////////////////////////////////////////////////////////////////////////////////////////

		SynthDef(nameWriteGates,
			{ | aiBusEGG,
				aiBusConditionedEGG,
				aiBusGateCycle,
				aiBusGateDelayedCycle,
				aiBusGateFilteredDFT,
				oBuffer |

				var egg = In.ar(aiBusEGG);
				var cegg = In.ar(aiBusConditionedEGG);
				var gcycle = In.ar(aiBusGateCycle);
				var gdcycle = In.ar(aiBusGateDelayedCycle);
				var gfdft = In.ar(aiBusGateFilteredDFT);

				GatedDiskOut.ar(oBuffer, 1, [egg, cegg, gcycle, gdcycle, gfdft]);
			}
		).add(libname);

		///////////////////////////////////////////////////////////////////////////////////////////////////////
		// Record extra channels if requested
		///////////////////////////////////////////////////////////////////////////////////////////////////////

		if (arrayRecordExtraInputs.notNil , {
			SynthDef(nameRecordExtraChannels,
				{ | oBuffer |
					var physios = SoundIn.ar(arrayRecordExtraInputs);
					var gate = LFPulse.ar(100, 0, 0);  //hard-coded frame rate 100 Hz
					GatedDiskOut.ar(oBuffer, gate, physios);
				}
			).add(libname);
		});

	}

	*liveInput { | aiBusMic, aiBusEGG, aoBusMic, aoBusEGG, aoBusConditionedMic, aoBusConditionedEGG ...args |
		^Array.with(nameLiveInput,
			[
				\aiBusMic, aiBusMic,
				\aiBusEGG, aiBusEGG,
				\aoBusMic, aoBusMic,
				\aoBusEGG, aoBusEGG,
				\aoBusConditionedMic, aoBusConditionedMic,
				\aoBusConditionedEGG, aoBusConditionedEGG
			],
			*args
		);
	}

	*diskInput { | iBufferDisk, aoBusMic, aoBusEGG, aoBusConditionedMic, aoBusConditionedEGG ...args |
		^Array.with(nameDiskInput,
			[
				\iBufferDisk, iBufferDisk,
				\aoBusMic, aoBusMic,
				\aoBusEGG, aoBusEGG,
				\aoBusConditionedMic, aoBusConditionedMic,
				\aoBusConditionedEGG, aoBusConditionedEGG
			],
			*args
		);
	}

	*echoMicrophone { | aiBusMic, aoBusSpeaker ...args |
		^Array.with(nameEchoMicrophone,
			[
				\aiBusMic, aiBusMic,
				\aoBusSpeaker, aoBusSpeaker
			],
			*args
		);
	}

	*writeAudio { | aiBusMic, aiBusEGG, oBufferAudio ...args |
		^Array.with(nameWriteAudio,
			[
				\aiBusMic, aiBusMic,
				\aiBusEGG, aiBusEGG,
				\oBufferAudio, oBufferAudio
			],
			*args
		);
	}

	*writeCycleDetectionLog { | aiBusEGG, aiBusGateCycle, oBufferLog ...args |
		^Array.with(nameWriteCycleDetectionLog,
			[
				\aiBusEGG, aiBusEGG,
				\aiBusGateCycle, aiBusGateCycle,
				\oBufferLog, oBufferLog
			],
			*args
		);
	}

	*writePoints { |
		aiBusGate,
		aiBusDeltaAmplitudeFirst,
		aiBusDeltaPhaseFirst,
		oBuffer
		...args |

		^Array.with(nameWritePoints,
			[
				\aiBusGate, aiBusGate,
				\aiBusDeltaAmplitudeFirst, aiBusDeltaAmplitudeFirst,
				\aiBusDeltaPhaseFirst, aiBusDeltaPhaseFirst,
				\oBuffer, oBuffer
			],
			*args
		);
	}

	*writeSampEn { |
		aiBusGate,
		aiBusSampEn,
		oBuffer
		...args |

		^Array.with(nameWriteSampEn,
			[
				\aiBusGate, aiBusGate,
				\aiBusSampEn, aiBusSampEn,
				\oBuffer, oBuffer
			],
			*args
		);
	}

	*writeFrequencyAmplitude { |
		aiBusGate,
		aiBusFrequency,
		aiBusAmplitude,
		oBuffer
		...args |

		^Array.with(nameWriteFreqAmp,
			[
				\aiBusGate, aiBusGate,
				\aiBusFrequency, aiBusFrequency,
				\aiBusAmplitude, aiBusAmplitude,
				\oBuffer, oBuffer
			],
			*args
		);
	}

	*writeLog { |
		aiBusGate,
		aiBusTimestamp,
		aiBusFrequency,
		aiBusAmplitude,
		aiBusClarity,
		aiBusCrest,
		aiBusClusterNumber,
		aiBusSampEn,
		aiBusIcontact,
		aiBusDEGGmax,
		aiBusQcontact,
		aiBusAmplitudeFirst,
		aiBusPhaseFirst,
		oBuffer
		...args |

		^Array.with(nameWriteLog,
			[
				\aiBusGate, aiBusGate,
				\aiBusTimestamp, aiBusTimestamp,
				\aiBusFrequency, aiBusFrequency,
				\aiBusAmplitude, aiBusAmplitude,
				\aiBusClarity, aiBusClarity,
				\aiBusCrest, aiBusCrest,
				\aiBusClusterNumber, aiBusClusterNumber,
				\aiBusSampEn, aiBusSampEn,
				\aiBusIcontact, aiBusIcontact,
				\aiBusDEGGmax, aiBusDEGGmax,
				\aiBusQcontact, aiBusQcontact,
				\aiBusAmplitudeFirst, aiBusAmplitudeFirst,
				\aiBusPhaseFirst, aiBusPhaseFirst,
				\oBuffer, oBuffer
			],
			*args
		);
	}

	*writeGates { |
		aiBusEGG,
		aiBusConditionedEGG,
		aiBusGateCycle,
		aiBusGateDelayedCycle,
		aiBusGateFilteredDFT,
		oBuffer
		...args |

		^Array.with(nameWriteGates,
			[
				\aiBusEGG, aiBusEGG,
				\aiBusConditionedEGG, aiBusConditionedEGG,
				\aiBusGateCycle, aiBusGateCycle,
				\aiBusGateDelayedCycle, aiBusGateDelayedCycle,
				\aiBusGateFilteredDFT, aiBusGateFilteredDFT,
				\oBuffer, oBuffer
			],
			*args
		);
	}

	*recordExtraChannels { |
		oBuffer
		...args |

		^Array.with(nameRecordExtraChannels,
			[
				\oBuffer, oBuffer
			],
			*args
		);
	}

}