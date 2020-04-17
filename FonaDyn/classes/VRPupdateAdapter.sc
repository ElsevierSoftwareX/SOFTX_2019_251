// Copyright (C) 2019 by Sten Ternström & Dennis J. Johansson, KTH Stockholm
// Released under European Union Public License v1.2, at https://eupl.eu
// *** EUPL *** //
VRPupdateAdapter {
	var mFunction;

	*new { | fcn |
		^super.new.init(fcn);
	}

	init { |fcn|
		mFunction = fcn ;
	}

	update { arg theChanged, theChanger, whatHappened;
		whatHappened.postln;
		^mFunction.value(whatHappened);
	}
}