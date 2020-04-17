// Copyright (C) 2019 by Sten Ternström & Dennis J. Johansson, KTH Stockholm
// Released under European Union Public License v1.2, at https://eupl.eu
// *** EUPL *** //
/*
// FonaDyn (C) Sten Ternström, Dennis Johansson 2015-2019
// KTH Royal Institute of Technology, Stockholm, Sweden.
// For full details of using this software, please see the FonaDyn Handbook, and the class help files.
// The main entry point to the online help files is that of the class VRPMain.
*/

// REVISION NOTES
// v2.0.2	= implemented saving of all settings (FonaDyn.rerun, Settings... dialog box)
// v2.0.1	= bug in Log files: wrong order of channels 7,8,9 - corrected
//			= rewrote & moved double-peak-picking to Dolansky.ar, used in 3 places
//			= added nil-check in BusListenerScopeHandler.dispatch, prevents rare hanging
// v2.0.0   = updated copyright banners to 2019 in all .sc files
//			= finally found and fixed the "Reset Count" bug, in KMeansRTv2.scx.
//			= implemented variable plot duration 1..10 s: drag the mouse sideways
//			= submitted to SoftwareX
// v1.7.2   = introduced Ic as a new map-layer and log channel (replacing diplo)
//			= removed the Diplophonia computation and plot from 1.6.5 (not reliable)
//			= completed the rebuild of plugins for MacOS, including PitchDetection.scx
//			= SampEn controls are now hidden unless "Sample Entropy" is checked
//			= The wall clock is now redrawn every second, not just during phonation
//			= Renamed the colour schemes, for fun
// v1.7.1   = fixed a problem with the Reset Counts buttonAction
//          = changed the peak-follower's tau from .99 to .95 => improved cycle picking at high fo
//			= enclosed some Dialog calls in a .forkIfNeeded;
//				hopefully this will prevent "scheduler queue is full"
//			= small points now show the exact centroid coordinates (single centroid view only)
// v1.7.0	= Rewrote the crest factor calculation as a pseudo-UGen (VRPSDVRP.sc),
//			  since the built-in Crest UGen is both clunky and wrong.
//			= Restored the EGG CLIPPING warning that had stopped working at some point
// v1.6.7   = found a problem in AverageOut: Integrator.ar does not work properly,
//			  so the H levels have been wrong. Rewrote it with Phasor.ar
//			= LoadVRP now reloads SampEn fields properly, with pale green zero cells.
// v1.6.6   = changed the line plots to horizontal flecks, one per cycle, which is clearer.
//			= in VRPSDCSDFT.sc:SynthDef(\sdNDFT), prevented residual from becoming too small
//          = fixed a bug that was causing dEGGmax not to be Logged
// v1.6.5   = added a Diplophonia plot (included in _Log.aiff files, but not in the VRP).
//			  It shows the relative level of the halfth partial to the first partial (+4 Bels),
//			  median-filtered over 5 cycles.
//		 	  If fo actually drops an octave, it won't count as diplophonia.
//			= Reliable updating of VRP background finally found and implemented.
// v1.6.4b  = small layout tweaks
// v1.6.4   = added optional plotting of multiple curves in what was the SampEn graph
//			  This included adding a .colors method to ScopeViewer
// v1.6.3b  = tiny bug in VRPViewMainMenuInput.sc fixed
// v1.6.3   = added .sync to the Pause action, improved stability on Pause.
//			= file batching was skipping a file if interrupted
//			= file batch now starts from the selected file (default: the first)
//			= added mLastPath to the file list browser for batches
// v1.6.2   = Press Alt + { c | m | v | p } to toggle the visibility of any graph
//          = A resolution bug in UGen TimeStamp.scx made FonaDyn stick and misbehave
//			  after 380.4 seconds; fixed. Now seems to run OK with long files.
//			= Merged the dEGGmax and Qci computations into one SynthDef 'sdQciDEGG' .
// v1.6.1   = replaced dEGGnt from the FDs with dEGGmax (1..20) from EGGcond
// v1.6.0   = changed the call bm.control(\GateReset).set(1)
//			to bm.control(\GateReset).setSynchronous(1). It seems to be more stable now.
// v1.5.9 = Fixed the color scale bar updating and its dEGGnt grid
//		  = recordingsDir, as specified in the file "startup.scd" or browsed to during a session,
//			is now the initial default folder for all Save/Open file operations.
// v1.5.8 = Added to the VRP display a cycle count threshold which can be changed at any time.
//			- this threshold is for display only, it is not stored.
//		  = added left-clicking on the VRP colour bar to switch layers
// v1.5.7 = Added the qContact metric: display, logging and _VRP.csv save/load (many changes)
//        = keyboard focus now moves to Pause after Start
// v1.5.6 = Removed the SampEn Arrows display, which was never used for anything.
//		    - will keep the code, but commented out, for a few more releases.
//		  = Added the dEGGnorm@trig metric: display, logging and _VRP.csv save/load (many changes)
//		  = Added a color scale bar in the VRP
//		  = Open File now initializes to the last directory on Loads and Saves (_clusters.csv, ...)
//		  = Internal: at SC 3.10.0, had to undo Float.asString, with overrides in GridLinesExp.sc
//		  = Internal: Changed nHarmonics to actually represent the # of harmonics, not nHarm+1
//		  	One extra harmonic was DFT'd, to waste - fixed.
//		  	Not sure how csdft.nHarmonics is initialized; it is used in VRPSDIO.sc.
//		  = Internal: moved the average Crest display computation
//			to the filtered handler (VRPControllerScope.sc)
// v1.5.5 Bug fix: clear averaged cycles when renumbering clusters
//        Fixed PATH bug in FonaDyn.run
//		  Tweaked the "Studio" colour scheme a bit
// v1.5.4 UGen AverageOutput.ar found to be broken;
//        worked around it with a pseudo-UGen AverageOut.ar (in file VRPSDCSDFT.sc)
// v1.5.3 Turned off smoothing in graphs, which became more consistent.
//        Reworked the redraw mechanism, still not bulletproof.
// v1.5.2 Added the Load VRP button for inspecting saved VRPs,
//		  and for recording more into an existing VRP (clusterings must match!).
//        Fixed a bug in Save _VRP.csv: it no longer appends a delimiter char to the last item,
//            old csv files are still parsed OK.
// v1.5.1 Added the "Keep data" option for concatenated runs,
//		     thus making "Batch multiple files" meaningful.
//		  Method .update was overriding Object.update; renamed it to .updateData (in all Views)
//		  Corrected the coding for dependants
//        On loadClusterData, set "Pre-learned" and a new palette
//		  Reordered the fetch/update sequence of color schemes
//        Found PATH bug in FonaDyn.run; needs fixing
// v1.5.0 License info added to all VRP*.sc files; release to SoftwareX
// v1.4.4 in FonaDyn.sc, added a check of PATH and prepending <resourceDir>; to it,
//        if necessary - not sure if this is Windows-specific or not
// v1.4.3 simplified the file format _clusters.csv to a rectangular array
// v1.4.2 added a Pause button, and moved the Start button to its left
//        Pause will also mute any audio feedback.
// v1.4.1 optimized "fetch"; tweaked the color schemes a little; optimized VRP drawing
// 		  so as not to have to redraw everything on every update.
// v1.4.0 added interface for reordering the clusters manually: On cluster columns,
//          ctrl-click left to swap with cluster to the left,
//          ctrl-click right to swap with with cluster to the right.
//          Wraps around at first and last cluster.
// v1.3.6.3 added Settings... GUI for recording of extra channels
// v1.3.6.2 added recording of extra channels;
//		with a hardcoded framerate 100 Hz, enable and input array are in VRPSettingsIO.sc.
// 		Also ganged the left cluster slider to the VRP display but not vice versa
// v1.3.6 added "colour schemes"; more trimming of the GUI
// v1.3.5 Took out the weighting of phase (cos, sin), much better!
// v1.3.4 GUI: repaired the tab order, enabled/disabled buttons on stop/start,
//        tweaked text field sizes, and the SampEn grid
// v1.3.3 Disk-in now records conditioned signals and colors output buttons
// v1.301 fixed the Cluster bug in isochronous log files
// v1.30 added Hz log scale to VRP (toggle with right-click)
//       (the source code: GridLinesExp.sc must be included in .\Extensions\SystemOverwrites)
//       Also realigned the VRP grid to the cell boundaries

VRPMain {
	// Graphics
	var mWindow;
	var mViewMain;

	// Data
	var <mContext;
	var mGUIRunning;
	var mMutexGUI;
	classvar <mVersion = "2.0.2";

	// Edit the character after $ to set the column delimiter in CSV files
	classvar <cListSeparator = $; ;

	// Clocks
	var mClockGuiUpdates; 	// Send updates to the GUI via this tempoclock
	var mClockControllers; 	// Clock used by the controllers to schedule fetches etc
//	var mClockDebug;  		// Clock used to dump debug info at intervals

	// Sundry
	classvar <guiUpdateRate = 24;  // Must be an integer # frames/sec
	classvar <panelColor;
	classvar <settingsArchiveName;

	*new { arg bRerun = false;
		^super.new.start(bRerun);
	}

	postLicence {
		var texts = [
			"=========== FonaDyn Version % ============",
			"© 2017-2019 Sten Ternström, Dennis Johansson, KTH Royal Institute of Technology",
			"Distributed under European Union Public License v1.2, see ",
			~gLicenceLink ];

		format(texts[0], mVersion).postln;
		texts[1].postln;
		texts[2].post;
		texts[3].postln;
	}

	update { arg theChanged, theChanger, whatHappened;
		if (whatHappened == \dialogSettings, {
			mContext.model.resetData;
		});
	}

	start { arg bRerun;
		~gLicenceLink = "https://eupl.eu";
		~gVRPMain = this;
		settingsArchiveName = Platform.userAppSupportDir +/+ "FonaDynSettings.SCarchive";

		// Set the important members
		mContext = VRPContext(\global, Server.default);
		mClockGuiUpdates = TempoClock(this.class.guiUpdateRate);	 // Maybe increase the queuesize here?
		mClockControllers = TempoClock(60, queueSize: 1024); // Enough space for 512 entries
//		mClockDebug = TempoClock(0.2); /**/

		// Start the server
		mContext.model.server.boot;
		mContext.model.server.doWhenBooted( { this.postLicence } );


		// Create the main window
		mWindow = Window("FonaDyn", Window.availableBounds().insetAll(20, 250, 600, 0), true, true, mContext.model.server);
		mWindow.view.background_( Color.grey(0.85) );   // no effect?

		// Create the Main View
		mViewMain = VRPViewMain( mWindow.view );
		if (bRerun and: { mContext.model.loadSettings },
			{
				mViewMain.stash(mContext.model.settings)
			}
		);

		panelColor = mContext.model.settings.general.getThemeColor(\backPanel);

		mViewMain.fetch(mContext.model.settings);
		mContext.model.resetData;
		// mContext.inspect;

		mWindow.onClose_ {
			var gd = mContext.model.data.general;
			var gs = mContext.model.settings.general;
			mGUIRunning = false;
			if (gd.started, {
				gs.stop = true;
			});
			gs.start = false;
			if (gs.saveSettingsOnExit, { mContext.model.settings.writeArchive(settingsArchiveName) });
		};

		mWindow.front;

		// Initiate GUI updates
		mMutexGUI = Semaphore();
		mGUIRunning = true;
		mClockGuiUpdates.sched(1, {
			var ret = if (mGUIRunning, 1, nil);
			Routine.new({this.updateData}).next;
			ret
		});

		/*
		mClockDebug.sched(1, {
			var ret = if (mGUIRunning, 1, nil);
			Routine.new({
				Main.gcInfo;
				// "Free: " + Main.totalFree.postln;
			}.defer ).next;
			ret
		});
		*/
	}

	guiUpdate {
		// Propagates the update to the views if the GUI is running
		var m = mContext.model;
		if ( mGUIRunning, {
			defer {
				if (mWindow.isClosed.not, {
					mViewMain.updateData(m.data);
					mViewMain.fetch(m.settings);
				});
				mMutexGUI.signal;
			};
			mMutexGUI.wait;
		});
	}

	updateData {
		var cond = Condition();
		var c = mContext;
		var cs = c.controller;
		var m = c.model;
		var s = m.server;
		var d = m.data;
		var se = m.settings;
		var bm = m.busManager;

		block { | break |
			if ( se.general.start, {
				se.general.start = false;
				Date.localtime.format("START %H:%M:%S, %Y-%m-%d").postln;

				// We should start the server!
				if (d.general.started or: d.general.starting, {
					d.general.error = "Unable to start the server as it is already started!";
					break.value; // Bail out
				});

				d.general.starting = true;
				this.guiUpdate(); // Let the views know that we're starting the server

				if ( se.sanityCheck.not, {
					// Some check failed - bail out
					d.general.starting = false;
					d.general.started = false;
					break.value;
				});

				// Reset the data - grabbing the new settings
				m.resetData;
				d = m.data;

				// Wait for the server to fully boot
				s.bootSync(cond);

				// Allocate the groups
				value {
					var c = Condition();
					var sub_groups = { Group.basicNew(s) } ! 8;
					var main_group = Group.basicNew(s);
					var msgs = [main_group.newMsg(s), main_group.runMsg(false)]; // Ensure that the main group is paused immediately!
					msgs = msgs ++ ( sub_groups collect: { | g | g.newMsg(main_group, \addToTail) } ); // Create the rest normally

					m.groups.putPairs([
						\Main, main_group,
						\Input, sub_groups[0],
						\AnalyzeAudio, sub_groups[1],
						\CSDFT, sub_groups[2],
						\Cluster, sub_groups[3],
						\SampEn, sub_groups[4],
						\PostProcessing, sub_groups[5],
						\Scope, sub_groups[6],
						\Output, sub_groups[7],
					]);

					// Send the bundle and sync
					s.sync(c, msgs);
				};

				// Create the controllers
				cs.io = VRPControllerIO(m.groups[\Input], m.groups[\Output], m.groups[\Main], d);
				cs.cluster = VRPControllerCluster(m.groups[\Cluster], d);
				cs.csdft = VRPControllerCSDFT(m.groups[\CSDFT], d);
				cs.sampen = VRPControllerSampEn(m.groups[\SampEn], d);
				cs.scope = VRPControllerScope(m.groups[\Scope], d);
				cs.vrp = VRPControllerVRP(m.groups[\AnalyzeAudio], d);
				cs.postp = VRPControllerPostProcessing(m.groups[\PostProcessing], d);

				// Find out what buses are required and allocate them
				cs.asArray do: { | c | c.requires(bm); };
				bm.allocate();
				s.sync;
				if (m.settings.general.enabledDiagnostics, {
				    bm.debug;  // Post the bus numbers for inspection
				});

				// Prepare all controllers and sync
				cs.asArray do: { | x | x.prepare(m.libname, s, bm, mClockControllers); };
				s.sync;

				// Start all controllers and sync
				cs.asArray do: { | x | x.start(s, bm, mClockControllers); };
				s.sync;

				// Resume the main group so all synths can start!
				m.groups[\Main].run;
				d.general.started = true;
				d.general.starting = false;
				d.general.pause = 0;
			}); // End start

			if (d.general.pause == 1, {
				"Pausing - ".post;
				m.groups[\Main].run(false);
				s.sync;
				d.general.pause = 2;
			});

			if (d.general.pause == 3, {
				m.groups[\Main].run(true);
				d.general.pause = 0;
				Date.localtime.format("resumed %H:%M:%S").postln;
			});

			// Either user wants to stop, or we reached EOF - make sure we're not already stopping or have stopped the server.
			if (se.general.stop or: (d.io.eof and: d.general.stopping.not and: d.general.started), {
				se.general.stop = false;
				Date.localtime.format("STOP  %H:%M:%S").postln;

				// Perform sanity checks, if they fail -> bail out
				if (d.general.started.not, {
					d.general.error = "Unable to stop: the server has not yet been started.";
					break.value; // Bail out
				});

				d.general.stopping = true;

				// Pause the main group
				m.groups[\Main].run(false);

				// Stop the controllers and sync
				cs.asArray do: { | x | x.stop; };

				this.guiUpdate(); // Let the views know that we're stopping the server

				cs.asArray do: { | x | x.sync; };

				// Free the buses & groups
				m.busManager.free;
				m.groups[\Main].free;
				m.groups.clear;

				// Done
				s.sync;
				d.general.started = false;
				d.general.stopping = false;
			}); // End stop
		}; // End block

		this.guiUpdate();
	}
}
