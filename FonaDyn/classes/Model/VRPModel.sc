// Copyright (C) 2019 by Sten Ternström & Dennis J. Johansson, KTH Stockholm
// Released under European Union Public License v1.2, at https://eupl.eu
// *** EUPL *** //
VRPModel {
	// Settings & Data
	var <settings; // An instance of VRPSettings, that in turn contains the individual settings for the controllers.
	var <data; // An instance of VRPData, that in turn contains the individual data from the controllers.

	// Nodes on the server
	var <busManager; // Manager for the buses
	var <groups; // Dictionary of groups

	var <server; // The server
	var <libname; // The library name where all the synthdefs are placed

	*new { | libname, server |
		^super.new.init(libname, server);
	}

	init { | l, s |
		libname = l;
		server = s;

		busManager = BusManager(s);
		groups = Dictionary();

		settings = VRPSettings();
		data = VRPData(settings);
	}

	loadSettings {
		if (File.exists(VRPMain.settingsArchiveName), {
			settings = Object.readArchive(VRPMain.settingsArchiveName);
			^true;
		},{
			(VRPMain.settingsArchiveName + "not found - using defaults.").warn;
			^false;
		}
		);
		// Settings are saved in VRPMain: mWindow.onClose
	}

	resetData {
		var old = data;
		data = VRPData.new(settings);
		data.general.reset(old.general); // Keep some old states of general
		if (data.settings.io.keepData, {
			data.vrp.reset(old.vrp);
			data.cluster.reset(old.cluster);
		});
	}
}