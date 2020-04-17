// Copyright (C) 2019 by Sten Ternström & Dennis J. Johansson, KTH Stockholm
// Released under European Union Public License v1.2, at https://eupl.eu
// *** EUPL *** //
VRPDataScope {
	// Data
	var <>sampen; // Plot data for SampEn
	var <>qci; // Plot data for contact quotient
	var <>degg; // Plot data for dEGGmax
	var <>movingEGGData;

	*new { | settings |
		^super.new.init(settings);
	}

	init { | settings |
	}
}