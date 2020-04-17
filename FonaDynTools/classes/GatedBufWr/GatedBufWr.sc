// Copyright (C) 2016-2019 by Dennis J. Johansson, 
// Released under European Union Public License v1.2, at https://eupl.eu
// *** EUPL *** //
GatedBufWr : UGen {
	*kr { | inputArray, bufnum, gate, phase, loop = 1 |
		^this.multiNewList( ['control', bufnum, gate, phase, loop] ++ inputArray );
	}
}