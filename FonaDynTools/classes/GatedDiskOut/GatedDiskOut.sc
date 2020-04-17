// Copyright (C) 2016-2019 by Dennis J. Johansson, 
// Released under European Union Public License v1.2, at https://eupl.eu
// *** EUPL *** //
GatedDiskOut : UGen {
	*ar { | bufnum, gate, channelsArray |
		^this.multiNewList(['audio', bufnum, gate] ++ channelsArray.asArray)
	}
}
