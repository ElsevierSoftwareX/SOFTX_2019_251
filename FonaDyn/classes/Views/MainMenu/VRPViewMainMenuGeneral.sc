// Copyright (C) 2019 by Sten Ternström & Dennis J. Johansson, KTH Stockholm
// Released under European Union Public License v1.2, at https://eupl.eu
// *** EUPL *** //
VRPViewMainMenuGeneral {
	var mView;

	// Controls
	var mStaticTextShowAs;
	var mListShowAs;
	var mListShow;

	var mStaticTextOutputDirectory;
	var mButtonBrowseOutputDirectory;
	var mStaticTextOutputDirectoryPath;

	// Holders for the non-modal Settings dialog
	var mButtonSettingsDialog;
	var <newSettings, <oldSettings, >bSettingsChanged;

	*new { | view |
		^super.new.init(view);
	}

	init { | view |
		var b = view.bounds;
		var static_font = Font(\Arial, 12);
		mView = view;
		mView.background_(VRPMain.panelColor);
		bSettingsChanged = false;
		this addDependant: ~gVRPMain;


		////////////////////////////////////////////////////////////
		////////////////////////////////////////////////////////////

		mStaticTextShowAs = StaticText(mView, Rect(0, 0, 70, 0))
		.string_("  Show:")
		.font_(static_font);
		mStaticTextShowAs
		.fixedWidth_(mStaticTextShowAs.sizeHint.width)
		.fixedHeight_(35)
		.stringColor_(Color.white);

		mListShowAs = ListView(mView, Rect(0, 0, 70, 0))
		.items_([
			"All graphs",
			"One graph"
		]);
		mListShowAs
		.fixedHeight_(mListShowAs.minSizeHint.height/2)
		.fixedWidth_(70)
		.selectionMode_(\single)
		.font_(static_font)
		.action_{ | list |
			mListShow.visible_(list.value == 1);
		};

		mListShow = ListView(mView, Rect(0, 0, 100, 0))
		.visible_(false)
		.font_(static_font)
		.items_([
			"Voice Field",
			"Cluster Data",
			"Time Plots",
			"Moving EGG"
		]);
		mListShow
		.fixedHeight_(mListShow.minSizeHint.height)
		.fixedWidth_(mListShow.sizeHint.width)
		.selectionMode_(\single);

		////////////////////////////////////////////////////////////
		////////////////////////////////////////////////////////////

		mStaticTextOutputDirectory = StaticText(mView, Rect(0, 0, 100, 0))
		.string_("Output Directory:")
		.font_(static_font);
		mStaticTextOutputDirectory
		.fixedWidth_(mStaticTextOutputDirectory.sizeHint.width)
		.fixedHeight_(35)
		.stringColor_(Color.white);

		mButtonBrowseOutputDirectory = Button(mView, Rect(0, 0, 100, 0))
		.resize_(4)
		.states_([["Browse"]])
		.action_{ |b|
			FileDialog(
				{ | path |
					mStaticTextOutputDirectoryPath.string = path.first;
				},
				nil, 2, 0, // Select a single existing directory
				path: mStaticTextOutputDirectoryPath.string;
			);
		};

		mStaticTextOutputDirectoryPath = TextField(mView, Rect(0, 0, 100, b.height))
		.enabled_(false)
		.visible_(true)
		.string_( thisProcess.platform.recordingsDir )
		.background_(Color.white);

		////////////////////////////////////////////////////////////
		////////////////////////////////////////////////////////////

		mButtonSettingsDialog = Button(mView, Rect(0, 0, 100, 0))
		.resize_(4)
		.states_([["Settings..."]])
		.action_({
			newSettings = oldSettings.deepCopy;
			VRPSettingsDialog.new(this)
		});

		////////////////////////////////////////////////////////////
		////////////////////////////////////////////////////////////

		mView.layout = HLayout(
			[mStaticTextShowAs, stretch: 1],
			[mListShowAs, stretch: 1],
			[mListShow, stretch: 1],
			[mStaticTextOutputDirectory, stretch: 1],
			[mButtonBrowseOutputDirectory, stretch: 1],
			[mStaticTextOutputDirectoryPath, stretch: 8],
			[nil, stretch: 2],
			[mButtonSettingsDialog, stretch: 1]
		);
	}

	stash { | settings |
		var gs = settings.general;
		mStaticTextOutputDirectoryPath.string_(gs.output_directory);
	}

	fetch { | settings |
		var gs = settings.general;
		var cs = settings.csdft;

		gs.layout = switch( mListShowAs.value,
			0, VRPViewMain.layoutGrid,
			1, VRPViewMain.layoutStack
		);

		gs.stackType = switch( mListShow.value,
			0, VRPViewMain.stackTypeVRP,
			1, VRPViewMain.stackTypeCluster,
			2, VRPViewMain.stackTypeSampEn,
			3, VRPViewMain.stackTypeMovingEGG
		);

		if (bSettingsChanged, {
			cs.method_(newSettings.csdft.method);
			settings.io.enabledCalibrationTone = newSettings.io.enabledCalibrationTone;
			settings.io.writeLogFrameRate = newSettings.io.writeLogFrameRate;  // ????
			settings.io.keepInputName = newSettings.io.keepInputName;
			settings.io.enabledWriteGates = newSettings.io.enabledWriteGates;
			settings.io.enabledRecordExtraChannels = newSettings.io.enabledRecordExtraChannels;
			settings.io.arrayRecordExtraInputs = newSettings.io.arrayRecordExtraInputs;
			settings.general.enabledDiagnostics = newSettings.general.enabledDiagnostics;
			settings.general.colorThemeKey = newSettings.general.colorThemeKey;
			settings.general.saveSettingsOnExit = newSettings.general.saveSettingsOnExit;
			this.changed(this, \dialogSettings, nil);
			bSettingsChanged = false;
		});

		gs.output_directory = mStaticTextOutputDirectoryPath.string;
	}

	updateData { | data |
		var dsg = data.settings.general;

		if (bSettingsChanged.not, { oldSettings = data.settings });
		if (data.general.starting, { mButtonSettingsDialog.enabled = false; }); // Disable when starting
		if (data.general.stopping, { mButtonSettingsDialog.enabled = true;  }); // Enable when starting

		if (dsg.guiChanged, {
			mView.background_(dsg.getThemeColor(\backPanel));
			[mStaticTextShowAs, mStaticTextOutputDirectory].do ({ arg c;
				c.stringColor_(dsg.getThemeColor(\panelText))}
			);
		});
	}

	close {
		this removeDependant: ~gVRPMain;
	}
}