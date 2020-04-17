// Copyright (C) 2019 by Sten Ternström & Dennis J. Johansson, KTH Stockholm
// Released under European Union Public License v1.2, at https://eupl.eu
// *** EUPL *** //

VRPViewVRP{
	// Views
	var mView;
	var mUserViewMatrix;
	var mUserViewMatrixBack;
	var mUserViewCursor;
//	var mUserViewSampEn;
	var mDrawableSparseMatrix;
	var mDrawableSparseMatrixBack;
	var mCycleCountThreshold;
//	var mDrawableSparseArrowMatrix;
	var mGrid;
	var mGridLinesMIDI, mGridLinesHz;
	var mGridHorzGridSelectHz;
	var mColorRect;
	var mColorGrid;
	var mColorSpec;
	var mD;

	// Controls
	var mDropDownType;
	var mSliderCluster;
	var mStaticTextCluster;

/**	var mButtonShowSampEnArrows; // Show/Hide SampEn arrows
	var mStaticTextSampEnLimit;
	var mNumberBoxSampEnLimit; // Limit for when SampEn arrows are generated
**/	var mStaticTextCycleThreshold;
	var mNumberBoxCycleThreshold; // min cycle count for display
	var mButtonLoadVRPdata;
	var mButtonSaveVRPdata;
	var mButtonSaveVRPimage;
	var mStaticTextInfo;

	// Entire VRP data
	var mVRPdata;
	var mVRPdataLoaded;

	// States
	var mnClusters;
	var mSelected;
	var mCursorColor;
	var mCursorRect;
	var mCellCount;
	var mbSized;
	var mbValidate;
	var mLastPath;  // last path to image files
	var mLargerFont;

	var mNewClusterOrder;
	classvar <mAdapterUpdate;


	// Constants
	classvar iDensity = 0,
	         iClarity = 1,
	         iCrestFactor = 2,
			 iEntropy = 3,
			 idEGGmax = 4,
			 iQcontact = 5,
			 iIcontact = 6,
	         iClusters = 7;

	classvar mInfoTexts = #[
		"Darkest: >10k cycles",
		"Threshold: ",
		"Red: >= 4 (12 dB)",
		"Most brown: >20",
		"Red: fastest contact",
		"Red: longest contact",
		"Blue: no contact",
		"Whiter: more overlap",
		"Most color: >200 cycles" ];

	classvar iCursorWidth = 9;
	classvar iCursorHeight = 9;

	*new { | view |
		^super.new.init(view);
	}

	invalidate { | all |
		if (all and: mUserViewMatrixBack.notNil, { mUserViewMatrixBack.clearOnRefresh_(true) });
		if (mUserViewMatrix.notNil, { mUserViewMatrix.clearOnRefresh_(true) });
	}

	// The following two methods handle the colors in the color bar
	// The values here must match those in VRPColorMap.sc
	initColorScale {
		mD = [
			// [minVal, maxVal, warp, palette, infoText, unit]
			[1, 10000.1, \exp, VRPDataVRP.paletteDensity, "Density (cycles)", ""],
			[0.96, 1, \lin, VRPDataVRP.paletteClarity, "Clarity", ""],
			[1.414, 4, \lin, VRPDataVRP.paletteCrest, "Mean crest factor", ""],
			[0, 20, \lin, VRPDataVRP.paletteEntropy, "Max sample entropy", ""],
			[1, 20.01, \exp, VRPDataVRP.paletteDEGGmax, "Mean QΔ", "slope"],
			[0.1, 0.6, \lin, VRPDataVRP.paletteQcontact, "Mean Qcontact (area)", ""],
			[0.0, 0.7, \lin, VRPDataVRP.paletteIcontact, "Mean Icontact", ""],
			[0, 5, \lin, VRPDataCluster.palette, "Cluster #", ""],
		];
	}

	setColorScale { | i, selCluster |
		var fnPalette, cColor, warp;
		var gridX;
		mD[iClarity][0] = VRPDataVRP.clarityThreshold;

		// The color mapping for clusters is a bit messy:
		if (i == iClusters,
			{
				fnPalette = VRPDataCluster.palette(mnClusters) ;
				if (selCluster > 0, {
					cColor = fnPalette.(selCluster-1);
					fnPalette = VRPDataVRP.paletteCluster(cColor);
					mD[iClusters][0] = 1;
					mD[iClusters][1] = 200;
					warp = \exp;
				},{
					mD[iClusters][0] = 1;
					mD[iClusters][1] = mnClusters + 0.97;
					warp = \lin;
				});
			    mD[i][2] = warp;
				mD[i][3] = fnPalette;
			}
		);
		mColorSpec = ControlSpec(mD[i][0], mD[i][1], mD[i][2], units: mD[i][5]);
		gridX = GridLines.new(mColorSpec);
		mColorGrid = DrawGrid(mColorRect, gridX, nil);
		mColorGrid
		.smoothing_(false)
		.font_("Arial", 10)
		.x.labelOffset_(4@(-15)); // mid-align the x labels
	} /* setColorScale */

	init { | view |
		var minFreq, maxFreq;

		mView = view;
		mView.background_(VRPMain.panelColor);
		mView.onResize_( { mbSized = true }  );
		mLargerFont = Font(\Arial, 12);
		mLastPath = thisProcess.platform.recordingsDir;

		mCellCount = 0;
		mNewClusterOrder = nil;
		mVRPdata = nil;
		mVRPdataLoaded = nil;
		mCycleCountThreshold = 5;
		mbSized = true;
		mbValidate = false;
		mSelected = iDensity;

		mGridHorzGridSelectHz = false;
		minFreq = VRPDataVRP.nMinMIDI;
		maxFreq = VRPDataVRP.nMaxMIDI;
		mGridLinesMIDI = GridLines(ControlSpec(minFreq, maxFreq, warp: \lin, units: "MIDI"));
		mGridLinesHz   = GridLines(ControlSpec(minFreq.midicps, maxFreq.midicps, warp: \exp, units: "Hz"));

		mGrid = DrawGrid(
			Rect(),
			mGridLinesMIDI,
			GridLines(ControlSpec(VRPDataVRP.nMinSPL,  VRPDataVRP.nMaxSPL, units: "dB"))
		);

		mGrid
		.smoothing_(false)
		.fontColor_(Color.gray);

		this.initColorScale;
		this.setColorScale(iDensity, mSelected);
		mUserViewMatrixBack = UserView(mView, mView.bounds);
		mUserViewMatrixBack
		.background_(Color.white)
		.acceptsMouse_(true)
		.addUniqueMethod(\getColor, { |v, ix, pos|	mD[ix][3].value(mColorSpec.map(pos)) } )
		.drawFunc_{ | uv |
			var b = uv.bounds.moveTo(0, 0);
			if (uv.clearOnRefresh, {
				// Draw the grid behind the "back" matrix
				mGrid.bounds_(b);
				mGrid.draw;

				// Draw the color scale bar over grid
				mColorRect = uv.bounds.moveTo(10, 20).setExtent(200, 32);
				Pen.use {
					var index = mDropDownType.value;
					var nColors = mColorRect.width.half;
					var pos, rc, str;
					nColors.do ({ |k|
						pos = k / nColors.asFloat;
						if ((index == iClusters) and: (mSelected == 0),
							{ pos = ((pos*mnClusters-1).floor.mod(mnClusters)) * (mnClusters.reciprocal)}
						);
						rc = Rect(mColorRect.left+(k*2), mColorRect.top, 2, mColorRect.height);
						Pen.color = uv.getColor(index, pos);
						Pen.addRect(rc);
						Pen.fill;
					});
					Pen.strokeColor = Color.gray;
					Pen.strokeRect(mColorRect);
					mColorGrid.fontColor = Color.black;
					mColorGrid.gridColors = [Color.gray, Color.gray];
					mColorGrid.bounds_(mColorRect);
					mColorGrid.draw;
					str = mD[index][4];
					if (mSelected > 0, { str = str.replace("#", mSelected) });
					Pen.stringInRect(str, mColorRect.insetBy(4,2), mLargerFont, Color.gray,  \topLeft);
					Pen.stringInRect(str, mColorRect.insetBy(3,1), mLargerFont, Color.gray(0.75), \topLeft);
				};
			mbValidate = true;
			});

			// Draw the cell density data in gray in the back plane
			if (mDrawableSparseMatrixBack.notNil, {
				Pen.use {
					// Flip the drawing of the matrix vertically, since the y-axis is flipped in the grid
					Pen.translate(0, b.height);
					Pen.scale(1, -1);
					if (uv.clearOnRefresh, {
						mDrawableSparseMatrixBack.invalidate;
					});
					mDrawableSparseMatrixBack.draw(uv);
				};
			});

			if (mbValidate, {
				uv.clearOnRefresh_(false);
				mbValidate = false;
			});

			// This is to ensure that the background is refreshed first
			if (mUserViewMatrix.notNil, { mUserViewMatrix.clearOnRefresh_(true) });
		} ;

		mUserViewMatrixBack.mouseDownAction_({
			| uv, x, y, m, bn |
			// Change the display layer, on left-click in the colour bar
			if (bn == 0 and: mColorRect.contains(x@y), {
				var dir = (x - mColorRect.center.x).sign;
				mDropDownType.valueAction = (mDropDownType.value + dir).mod(mDropDownType.items.size);
			});
			// Toggle lin/log grid, on right-click
			if (bn == 1,    {
				mGridHorzGridSelectHz = mGridHorzGridSelectHz.not;
				mGrid.horzGrid_(if (mGridHorzGridSelectHz, { mGridLinesHz }, { mGridLinesMIDI } ));
			});
			this.invalidate(true);
		});

		mUserViewMatrix = UserView(mView, mView.bounds);
		mUserViewMatrix
		.acceptsMouse_(false)
		.drawFunc_{ | uv |
			var b = uv.bounds.moveTo(0, 0);

			Pen.use {
				// Flip the drawing of the matrix vertically, since the y-axis is flipped in the grid
				Pen.translate(0, b.height);
				Pen.scale(1, -1);
				if (mDrawableSparseMatrix.notNil, {
					if (uv.clearOnRefresh, {
						mDrawableSparseMatrix.invalidate;
					});
					mDrawableSparseMatrix.draw(uv);
				});
			};
			uv.clearOnRefresh_(false);
		};

		this.invalidate(true);

/**		mUserViewSampEn = UserView(mView, mView.bounds);
		mUserViewSampEn
		.acceptsMouse_(false)
		.drawFunc_{ | uv |
			if ( mDrawableSparseArrowMatrix.notNil, {
				var b = uv.bounds;
				Pen.use {
					// Flip the drawing of the matrix vertically, since the y-axis is flipped in the grid
					Pen.translate(0, b.height);
					Pen.scale(1, -1);
					mDrawableSparseArrowMatrix.draw(uv, Color.black);
				};
			});
		};
**/
		mUserViewCursor = UserView(mView, mView.bounds);
		mUserViewCursor
		.acceptsMouse_(false)
		.drawFunc_{ | uv |
			if ( mCursorRect.notNil, {
				Pen.use {
					Pen.fillColor = mCursorColor;
					Pen.strokeColor = Color.black;
					Pen.fillRect(mCursorRect);
					Pen.strokeRect(mCursorRect);
				};
			});
		};

		this.initMenu();
		mView.layout_(
			VLayout(
				[
					HLayout(
//						[mButtonShowSampEnArrows, stretch: 1],
//						[mStaticTextSampEnLimit, stretch: 1],
//						[mNumberBoxSampEnLimit, stretch: 1],
//						[10, stretch: 5],
						[mStaticTextCycleThreshold, stretch: 1],
						[mNumberBoxCycleThreshold, stretch: 1],
						[10, stretch: 5],
						[mButtonLoadVRPdata, stretch: 1],
						[mButtonSaveVRPdata, stretch: 1],
						[mButtonSaveVRPimage, stretch: 1],
						[nil, stretch: 50]  // Force the controls to take up as little space as possible
					), stretch: 1
				],

				[
					HLayout(
						[mDropDownType, stretch: 1],
						[mStaticTextCluster, stretch: 1],
						[mSliderCluster, stretch: 5],
						[mStaticTextInfo, stretch: 1]
					), stretch: 1
				],

				[
					StackLayout(
						mUserViewCursor,
//						mUserViewSampEn,
						mUserViewMatrix,
						mUserViewMatrixBack
					).mode_(\stackAll) // Draw mUserViewCursor on top of mMatrixViewer.view

					, stretch: 50 // Force the menu to take up as little space as possible!
				]

			)
		);

		mAdapterUpdate =
		{ | menu, who, what, newValue |
			switch (what,
				\selectCluster,
				{ mSliderCluster.valueAction = newValue },
				\reorderClusters,
				{
					mNewClusterOrder = newValue;
					this.invalidate(false)
				},
				\dialogSettings,
				{
					if (newValue.isInteger, { mnClusters = newValue });
					this.setColorScale(mDropDownType.value, mSelected);
					this.invalidate(true)
				},
				// else
				{ postln("Unknown change notification") }
			);
		};

		this.updateView;
	} /* init */

	initMenu {
		var static_font = Font(\Arial, 12);

/*		mButtonShowSampEnArrows = Button(mView, Rect());
		mButtonShowSampEnArrows
		.states_([
			["SampEn Arrows: Off"],
			["SampEn Arrows: On "]
		]);

		mStaticTextSampEnLimit = StaticText(mView, Rect())
		.string_("Threshold:")
		.font_(static_font);
		mStaticTextSampEnLimit
		.fixedWidth_(mStaticTextSampEnLimit.sizeHint.width)
		.stringColor_(Color.white);

		mNumberBoxSampEnLimit = NumberBox(mView, Rect())
		.clipLo_(0.1)
		.step_(0.1)
		.scroll_step_(0.1)
		.value_(1);

		mButtonShowSampEnArrows
		.action_({ | btn |
			var show = btn.value == 1;
			mUserViewSampEn.visible_(show);
			mStaticTextSampEnLimit.visible_(show);
			mNumberBoxSampEnLimit.visible_(show);
		})
		.valueAction_(0)
		.fixedHeight_(mNumberBoxSampEnLimit.sizeHint.height);
*/

		mStaticTextCycleThreshold = StaticText(mView, Rect())
		.string_("Cycle threshold:")
		.font_(static_font);
		mStaticTextCycleThreshold
		.fixedWidth_(mStaticTextCycleThreshold.sizeHint.width)
		.stringColor_(Color.white);

		mNumberBoxCycleThreshold = NumberBox(mView, Rect())
		.clipLo_(1)
		.step_(1)
		.scroll_step_(5)
		.value_(1);

		mButtonLoadVRPdata = Button(mView, Rect());
		mButtonLoadVRPdata
		.states_([["Load Map"]])
		.action_( { |btn|
			this.loadVRP;
		})
		.enabled_(true);

		mButtonSaveVRPdata = Button(mView, Rect());
		mButtonSaveVRPdata
		.states_([["Save Map"]])
		.action_( { |btn|
			if (mVRPdata.notNil, { mVRPdata.saveVRPdata; } );
		})
		.enabled_(false);

		mButtonSaveVRPimage = Button(mView, Rect());
		mButtonSaveVRPimage
		.states_([["Save Image"]])
		.action_( { |btn|  this.writeImage()} );

		mDropDownType = PopUpMenu(mView, [0, 0, 100, 30]);
		mDropDownType
		.items_([
			"Density",
			"Clarity",
			"Crest Factor",
			"Max Entropy",
			"QΔ",
			"Qcontact",
			"Icontact",
			"Clusters"
		])
		.action_{ | v |
			this.setColorScale(v.value, mSelected);
			this.invalidate(true);
			this.updateView();
		}
		.resize_(4);

		mStaticTextCluster = TextField(mView, [0, 0, 100, 30]);
		mStaticTextCluster
		.fixedWidth_(100)
		.enabled_(false);

		mnClusters = 5;
		mSelected = 0;
		mSliderCluster = Slider(mView, [0, 0, mView.bounds.width, 30]);
		mSliderCluster
		.maxHeight_(24)
		.resize_(4);
		mSliderCluster.action_{ |s|
		    s.step_(1 / mnClusters);
			mSelected = (s.value * mnClusters).round(0.01).asInteger;
			this.setColorScale(mDropDownType.value, mSelected);
			this.invalidate(true);
			this.updateView;
		};

		mStaticTextInfo = StaticText(mView, Rect())
		.string_("Info")
		.align_(\right)
		.font_(mLargerFont.boldVariant);
		mStaticTextInfo
		.fixedWidth_(150)
		.fixedHeight_(35);
	} /* initMenu */

	loadVRP {
		var nClusters;
		var tempVRPdata;
		tempVRPdata = VRPDataVRP.new(nil);
		tempVRPdata.loadVRPdata( { | nClusters |
		if (nClusters < 2, {
			mVRPdataLoaded = nil;
			"Load of _VRP.csv failed.".postln;
			},{
			mVRPdataLoaded = tempVRPdata;
			format("Loaded a map with % clusters.", nClusters).postln;
			});
		});
	}

	updateView {
		var is_clusters = iClusters == mDropDownType.value;
		var infoStr;

		// Show or hide the cluster controls
		mStaticTextCluster.visible_(is_clusters);
		mSliderCluster.visible_(is_clusters);

		// Update the cluster text
		mStaticTextCluster.string_(
			"Cluster: " ++
			if ( mSelected == 0, "All", mSelected.asString )
		);

		// Update the info text for the current display
		switch (mDropDownType.value,
			iDensity, {
				mStaticTextInfo.stringColor_(Color.grey);
				infoStr = mInfoTexts[iDensity];
			},

			iCrestFactor, {
				mStaticTextInfo
				.stringColor_(Color.red);
				infoStr = mInfoTexts[iCrestFactor];
			},

			iClarity, {
				mStaticTextInfo
				.stringColor_(Color.green(0.5));
				infoStr = mInfoTexts[iClarity] + VRPDataVRP.clarityThreshold.asString;
			},

			iEntropy, {
				mStaticTextInfo
				.stringColor_(Color.new255(165, 42, 42));
				infoStr = mInfoTexts[iEntropy];
			},

			idEGGmax, {
				mStaticTextInfo
				.stringColor_(Color.red);
				infoStr = mInfoTexts[idEGGmax];
			},

			iQcontact, {
				mStaticTextInfo
				.stringColor_(Color.red);
				infoStr = mInfoTexts[iQcontact];
			},

			iIcontact, {
				mStaticTextInfo
				.stringColor_(Color.blue);
				infoStr = mInfoTexts[iIcontact];
			},

			iClusters, {
				if (mSelected == 0,
					{ mStaticTextInfo
					.stringColor_(Color.white);
					infoStr = mInfoTexts[iClusters];
					},
					{ mStaticTextInfo
					.stringColor_(VRPDataCluster.palette(mnClusters).value(mSelected-1));
					infoStr = mInfoTexts[iClusters+1];
					}
				);
			},
			{ infoStr = "" }
		);
		mStaticTextInfo.string_(infoStr);
	} /* updateView{} */

	stash { | settings |
	}

	fetch { | settings |
/**		settings.sampen.limit = mNumberBoxSampEnLimit.value;  **/
	}

	updateData { | data |
		var vrpd;
		var sd, dsg;
		var palette;

		if (mNewClusterOrder.notNil, {
			data.vrp.reorder(mNewClusterOrder);
			mNewClusterOrder = nil;
		} );

		if (mVRPdataLoaded.notNil,
			{
				data.vrp.reset(mVRPdataLoaded);
				mVRPdataLoaded.free;
				mVRPdataLoaded = nil;
				this.invalidate(true);
			}
		);

		vrpd = data.vrp;
//		sd = data.sampen;
		palette = data.cluster.palette;
		mnClusters = data.settings.cluster.nClusters;
		mCycleCountThreshold = mNumberBoxCycleThreshold.value;
		this.updateView;

		dsg = data.settings.general;
		if (dsg.guiChanged, {
			mView.background_(dsg.getThemeColor(\backPanel));
			mStaticTextCycleThreshold.stringColor_(dsg.getThemeColor(\panelText));
		});

		if (data.general.stopping, {
			mButtonLoadVRPdata.enabled = true;
			mButtonSaveVRPdata.enabled = true;
			mVRPdata = vrpd; // Remember for saving
		}); // Enable if stopping

		if (data.general.starting, {
			mButtonLoadVRPdata.enabled = false; // Disable when starting
			mButtonSaveVRPdata.enabled = false;
			mCellCount = 0;
			this.invalidate(true);					// Clear the old graph
		});

		// Update the graph depending on the type selected in the dropdown menu
		// The refDensity matrix is used by some for calculating the per-cell means
		mDrawableSparseMatrixBack = nil;
		mDrawableSparseMatrix.notNil.if { mDrawableSparseMatrix.mbActive_(false) };
		switch (mDropDownType.value,
			iDensity, {
				mDrawableSparseMatrix = vrpd.density;
				mDrawableSparseMatrix.refDensity_(nil);
			},

			iClarity, {
				mDrawableSparseMatrix = vrpd.clarity;
				mDrawableSparseMatrix.refDensity_(nil);
			},

			iCrestFactor, {
				mDrawableSparseMatrix = vrpd.crest;
				mDrawableSparseMatrix.refDensity_(vrpd.density);
			},

			iEntropy, {
				mDrawableSparseMatrix = vrpd.entropy;
				mDrawableSparseMatrix.refDensity_(vrpd.density);
			},

			idEGGmax, {
				mDrawableSparseMatrix = vrpd.dEGGmax;
				mDrawableSparseMatrix.refDensity_(vrpd.density);
			},

			iQcontact, {
				mDrawableSparseMatrix = vrpd.qContact;
				mDrawableSparseMatrix.refDensity_(vrpd.density);
			},

			iIcontact, {
				mDrawableSparseMatrix = vrpd.iContact;
				mDrawableSparseMatrix.refDensity_(vrpd.density);
			},

			iClusters, {
				if ( mSliderCluster.value == 0,
					{
						// View all clusters
						mDrawableSparseMatrix = vrpd.mostCommonCluster;
						mDrawableSparseMatrix.refDensity_(vrpd.density);
					}, {
						// View specific cluster
						var idx = ( (mSliderCluster.value * mnClusters) - 1 ).round.asInteger;
						mDrawableSparseMatrix = vrpd.clusters[idx];
						mDrawableSparseMatrix.refDensity_(nil);
						mDrawableSparseMatrixBack = vrpd.density;
						mDrawableSparseMatrixBack.notNil.if {  mDrawableSparseMatrixBack.mbActive_(true) };
					}
				);
			}
		); // End switch

		mDrawableSparseMatrix.notNil.if {
			mDrawableSparseMatrix.thresholdCount_(mCycleCountThreshold);
			mDrawableSparseMatrix.mbActive_(true);
		};

		if (mbSized, {
			this.invalidate(true);
			mbSized = false;
		});

		mUserViewMatrixBack.refresh;
		mUserViewMatrix.refresh;

		// Update the cursor
		if (vrpd.currentAmplitude.notNil and: vrpd.currentFrequency.notNil and: vrpd.currentClarity.notNil, {
			var idx_midi = VRPDataVRP.frequencyToIndex( vrpd.currentFrequency );
			var idx_spl = VRPDataVRP.amplitudeToIndex( vrpd.currentAmplitude );

			var px = VRPDataVRP.frequencyToIndex( vrpd.currentFrequency, mUserViewCursor.bounds.width );
			var py = mUserViewCursor.bounds.height - 1 - // Flip vertically
			VRPDataVRP.amplitudeToIndex( vrpd.currentAmplitude, mUserViewCursor.bounds.height );

			mCursorRect = Rect.aboutPoint(
				px@py,
				iCursorWidth.half.asInteger,
				iCursorHeight.half.asInteger
			);

			// Update the cursor depending on the type selected in the dropdown menu
			if (mDropDownType.value == iClusters,
				{mCursorColor = palette.(vrpd.currentCluster ? 0)},  // testing for nil just in case
				{mCursorColor = Color.clear }
			);

			mUserViewCursor.refresh;
		});

		if ( data.general.started.not,
			{
			mCursorRect = nil;
			}
		);

		// Update the arrow pointers (deprecated)
/**		mDrawableSparseArrowMatrix = sd.sampenPoints;
		mUserViewSampEn.refresh; **/
	} /* updateData{} */

	writeImage {
		var rect = (mDropDownType.bounds union: mUserViewMatrix.bounds).insetBy(-5);
		var iTotal = Image.fromWindow(mView, rect);
		var tmpWnd = iTotal.plot(bounds: rect.moveTo(200,200), freeOnClose:false, showInfo: false);
		var str = format("Supported image file formats:\n % ", Image.formats);
		str.postln;
		tmpWnd
		.setInnerExtent(rect.width, rect.height)
		.onClose_({
			Dialog.savePanel({ arg path;
				mLastPath = PathName.new(path).pathOnly;
				iTotal.write(path, format: nil);
				("Image saved to" + path).postln;
				iTotal.free;
			}, path: mLastPath);
		})
		.front;
	}

	close {
	}
}

