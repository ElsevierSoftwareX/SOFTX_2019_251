// Copyright (C) 2019 by Sten Ternström & Dennis J. Johansson, KTH Stockholm
// Released under European Union Public License v1.2, at https://eupl.eu
// *** EUPL *** //
VRPDataVRP {
	// Data
	var <density; // Density as # of values that went into each slot, or nil if no data exists
	var <clarity; // Clarity at each slot, or nil if no data exists
	var <>maxDensity; // Largest value in the density matrix, or nil if no data exists
	var <crest; // Crest factor at each slot, or nil if no data exists
	var <entropy; // SampEn entropy at each slot, or nil if no data exists
	var <dEGGmax;  // normalized dEGGmax at from the conditioned cycle waveform
	var <qContact;  // contact quotient as if integrated over the cycle
	var <iContact;  // index of contacting as described in JASA-EL paper
	var <mostCommonCluster; // The most common cluster in each slot, or nil if no data exists
	var <clusters; // A density matrix for each cluster

	// The most recent frequency, amplitude, clarity, entropy and cluster measurements
	var <>currentFrequency;
	var <>currentAmplitude;
	var <>currentClarity;
	var <>currentEntropy;
	var <>currentCluster;

	// Settings
	classvar <nMinMIDI = 30;
	classvar <nMaxMIDI = 96;
	classvar <nMinSPL = 40;
	classvar <nMaxSPL = 120;
	classvar <>clarityThreshold = 0.96;
	classvar <mLastPath;
	classvar <colorZeroEntropy;

//	classvar <>waitSeconds = 1.0;

	classvar <vrpWidth = 66;  // nMaxMIDI - nMinMIDI; // 1 cell per semitone
	classvar <vrpHeight = 80; // nMaxSPL  - nMinSPL;  // 1 cell per dB

	*new { | settings |
		^super.new.init(settings);
	}

	init { | settings |
		var w = this.class.vrpWidth + 1;    // +1 because we want to include the upper limit
		var h = this.class.vrpHeight + 1;
		var clusterPalette;
		var nC;

		colorZeroEntropy = Color.hsv(0.33, 0.1, 1);
		nC = if(settings.isNil,
			{2}, 	// invoked by VRPViewVRP.loadVRP, will be changed soon
			{settings.cluster.nClusters}
		);
		clusterPalette = VRPDataCluster.palette(nC);

		density = DrawableSparseMatrix(h, w, VRPDataVRP.paletteDensity);
		maxDensity = nil;
		clarity = DrawableSparseMatrix(h, w, VRPDataVRP.paletteClarity);
		crest = DrawableSparseMatrix(h, w, VRPDataVRP.paletteCrest);
		entropy = DrawableSparseMatrix(h, w, VRPDataVRP.paletteEntropy);
		dEGGmax = DrawableSparseMatrix(h, w, VRPDataVRP.paletteDEGGmax);
		qContact = DrawableSparseMatrix(h, w, VRPDataVRP.paletteQcontact);
		iContact = DrawableSparseMatrix(h, w, VRPDataVRP.paletteIcontact);
		mostCommonCluster = DrawableSparseMatrix(h, w, clusterPalette);
		clusters = Array.fill(nC,
			{ | idx |
				var color = clusterPalette.(idx);
				DrawableSparseMatrix(h, w,
					VRPDataVRP.paletteCluster(color)
				)
			}
		);
		mLastPath = thisProcess.platform.recordingsDir;
	}

	reset { | old |
		density = old.density;
		clarity = old.clarity;
		maxDensity = old.maxDensity;
		crest = old.crest;
		entropy = old.entropy;
		dEGGmax = old.dEGGmax;
		qContact = old.qContact;
		iContact = old.iContact;
		mostCommonCluster = old.mostCommonCluster;
		clusters = old.clusters;
	}

	*frequencyToIndex { arg freq, width = VRPDataVRP.vrpWidth;
		^freq
		.linlin(nMinMIDI, nMaxMIDI, 0, width)
		.round
		.asInteger;
	}

	*amplitudeToIndex { arg amp, height = VRPDataVRP.vrpHeight;
		^(amp + nMaxSPL)
		.linlin(nMinSPL, nMaxSPL, 0, height)
		.round
		.asInteger;
	}

	reorder { arg newOrder;
		var tmp;
		var color;
		// To recolor, we need only change to the new order and reset the palettes
		if ((newOrder.class==Array) and: (newOrder.size==clusters.size),
			{
			var bOK = true;
			var clusterPalette = VRPDataCluster.palette( newOrder.size );
				newOrder.do { arg elem, i; if (elem.class != Integer, { bOK = false }) };
				if (bOK, {
					tmp = clusters[newOrder];
					clusters = tmp;
					clusters.do { arg c, i;
						if (c.notNil, {
							color = clusterPalette.(i);
							c.recolor(VRPDataVRP.paletteCluster(color));
							}, {
								post(c);
						})
					};
					mostCommonCluster.renumber(newOrder);
				})
			}
		);
	}

	saveVRPdata {
		var cDelim = VRPMain.cListSeparator;

		Dialog.savePanel({
			| path |
			mLastPath = PathName.new(path).pathOnly;
			if (path.endsWith(".csv").not) {
				path = path ++ "_VRP.csv"; // "VRP Save File"
			};

			// Write every non-nil VRP cell as a line
			// First cols are x,y in (MIDI, dB),
			// then density, clarity, crest factor, maxEntropy, topCluster, cluster 1..n
			File.use(path, "w", { | file |
				var cv;

				// Build and output the title row
				cv = List.newUsing(["MIDI", "dB", "Total", "Clarity", "Crest", "Entropy", "dEGGmax", "Qcontact", "Icontact", "maxCluster"]);
				clusters.size.do ({ |i| cv = cv.add("Cluster"+(i+1).asString)});
				cv.do ({|v, i|
					file << v;
					if (i < (cv.size-1), { file.put(cDelim)})
				});
				file.put($\r); file.nl;
				cv.clear;

				// Build and output the data rows
				// Cells below the clarity threshold are not stored
				density.rows.do({ |r|
					density.columns.do({arg c; var dValue, mc;
						dValue = density.at(r, c);
						if (dValue.notNil, {
							cv.add(c+nMinMIDI);
							cv.add(r+nMinSPL);
							cv.add(dValue);
							cv.add(clarity.at(r, c));
							cv.add(crest.at(r, c));
							cv.add(entropy.at(r, c) ? 0);
							cv.add(dEGGmax.at(r, c));
							cv.add(qContact.at(r, c));
							cv.add(iContact.at(r, c));
							mc = mostCommonCluster.at(r, c);
							if (mc.isNil, { cv.add(-1) }, { cv.add(1+mc[0]) });
							clusters.size.do ({|k| cv = cv.add(clusters[k].at(r, c) ? 0)});
							cv.do ({|v, i|
								file << v;
								if (i < (cv.size-1), { file.put(cDelim)});
							});
							file.put($\r); file.nl;
							cv.clear;
						})}
			)})});
		}, path: mLastPath);
	} /* saveVRPdata{} */

	/* Invoke this method on a newly created instance of VRPDataVRP.
	   It returns the number of clusters found in the CSV file via funcDone.
	   The number of existing clusters must match.
	*/
	loadVRPdata { | funcDone |
		var cDelim = VRPMain.cListSeparator;
		var nClusters = 0;
		var nCols = 0;

		Dialog.openPanel({ | path |
			if (path.endsWith("_VRP.csv"), {
				var cArray, testItem, ct, ix1;
				mLastPath = PathName.new(path).pathOnly;

				cArray = FileReader.read(path, skipEmptyLines: true, skipBlanks: true, delimiter: cDelim);
				// First cols are x,y in (MIDI, dB),
				// then density, clarity, crest factor, maxEntropy, topCluster, cluster 1..n

				ct = cArray.at(0); 					// extracts the row of column headings
				ix1 = ct.indexOfEqual("Cluster 1");
				nCols = cArray[1].size;
				testItem = cArray[0][nCols-1];
				if (testItem.notNil and: (testItem.size > 0),
					{ nClusters = nCols-ix1 },		// the cluster data must be in the last columns
					{ nClusters = nCols-(ix1+1) }   // Trailing cDelim on lines (bug in old files)
				);
				cArray.removeAt(0); // deletes the row of column headings

				// In case nClusters has changed, we must reallocate "clusters" and change palettes
				clusters = Array.fill(nClusters,
					{ | idx |
						var color = VRPDataCluster.palette(nClusters).(idx);
						DrawableSparseMatrix(
							this.class.vrpHeight + 1,
							this.class.vrpWidth + 1,
							VRPDataVRP.paletteCluster(color)
						)
					}
				);
				mostCommonCluster.setPalette(VRPDataCluster.palette(nClusters));

				/* Parse cArray row by row */
				cArray.do( { | rowData, rowNo |
					var x, y, value, percent, totalCycles, mostCycles, ix;
					x = rowData[ct.indexOfEqual("MIDI")].asInteger - nMinMIDI;
					y = rowData[ct.indexOfEqual("dB")].asInteger - nMinSPL;
					totalCycles = rowData[ct.indexOfEqual("Total")].asInteger;
					if ((maxDensity ? 0) < totalCycles, { maxDensity = totalCycles });
					density.put(y, x, totalCycles);

					// "Clarity" column is optional
					if ((ix = ct.indexOfEqual("Clarity")).notNil, {
						clarity.put(y, x, rowData[ix].asFloat)
					});

					// "Crest" column is optional
					if ((ix = ct.indexOfEqual("Crest")).notNil, {
						crest.put(y, x, rowData[ix].asFloat)
					});

					// "Entropy" column is optional
					if ((ix = ct.indexOfEqual("Entropy")).notNil, {
						value = rowData[ix].asFloat;
						entropy.put(y, x, value)}
					);

					// "dEGGmax" column is optional
					if ((ix = ct.indexOfEqual("dEGGmax")).notNil, {
						dEGGmax.put(y, x, rowData[ix].asFloat)
					});

					// "Qcontact" column is optional
					if ((ix = ct.indexOfEqual("Qcontact")).notNil, {
						qContact.put(y, x, rowData[ix].asFloat)
					});

					// "Icontact" column is optional
					if ((ix = ct.indexOfEqual("Icontact")).notNil, {
						iContact.put(y, x, rowData[ix].asFloat)
					});

					// "maxCluster" column is mandatory
					mostCycles = 0;
					(0..nClusters-1).do ({ |i|
						value = rowData[ix1+i].asInteger;
						if (value > 0, { clusters[i].put(y, x, value) } );
						if (value > mostCycles, { mostCycles = value });
					});
					value = rowData[ct.indexOfEqual("maxCluster")].asInteger - 1;
					percent = 100 * mostCycles * (totalCycles.reciprocal);
					mostCommonCluster.put(y, x, [value, percent]);
				});
				path.postln;
			});
		funcDone.value(nClusters);
		}, path: mLastPath);
	} /* loadVRPdata{} */
}
