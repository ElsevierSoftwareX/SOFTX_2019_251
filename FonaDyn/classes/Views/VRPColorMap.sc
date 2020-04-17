// Copyright (C) 2019 by Sten Ternström & Dennis J. Johansson, KTH Stockholm
// Released under European Union Public License v1.2, at https://eupl.eu
// *** EUPL *** //
+ VRPDataVRP {

	*paletteDensity {
		^{ | v |
			// map 1..<10000 to light...darker grey
			var cSat = v.explin(1, 10000, 0.95, 0.25);
			Color.grey(cSat, 1);
		};
	}

	*paletteClarity {
		^{ | v |
			// Map values above the threshold to a green shade (brighter green the better the clarity)
			// Map values below the threshold to gray
			if (v > VRPDataVRP.clarityThreshold,
				Color.green(v.linlin(VRPDataVRP.clarityThreshold, 1.0, 0.5, 1.0)),
				Color.gray)
		};
	}

	*paletteCrest {
		^{ | v |
			var cHue;

			// map crest factor 1.414 (+3 dB) ... <4 (+12 dB) to green...red
			cHue = v.linlin(1.414, 4, 0.333, 0);
			Color.hsv(cHue, 1, 1);
		};
	}

	*paletteEntropy {
		^{ | v |
			var sat;
			if (v==0, { colorZeroEntropy },{
					// Brown, saturated at 20. Should be scaled for nHarmonics in SampEn
					sat = v.linlin(0, 20, 0.1, 0.95);
					Color.white.blend(Color.new255(165, 42, 42), sat);
			})
		}
	}

	*paletteDEGGmax {
		^{ | v |
			var cHue;

			// map log(dEGGnt) 1.0 ... <10 to green...red
			// map dEGGnt 0.1 ... 1.0 to purple...green
			//	(dEGGnt)		cHue = v.biexp(1, 0.3, 10, 0.333, 0.9, 0);
			cHue = v.explin(1, 20, 0.333, 0);
			Color.hsv(cHue, 1, 1)
		};
	}

	*paletteQcontact {
		^{ | v |
			var cHue;

			// map large Qc=0.6 to red, small Qc=0.1 to purple
			cHue = v.linlin(0.1, 0.6, 0.83, 0.0);
			Color.hsv(cHue, 1, 1)
		};
	}

	*paletteIcontact {
		^{ | v |
			var cHue;

			// map large Ic=1.0 to red, small Ic=0 to blue
			cHue = v.linlin(0.0, 0.7, 0.67, 0.0);
			Color.hsv(cHue, 1, 1)
		};
	}

	*paletteCluster { | typeColor |
		^{ | v |
			// Blend with white depending on the count. Counts >= 200 aren't blended at all.
			var sat, cSat;
			sat = v.explin(1, 200, 0.75, 0);
			cSat = typeColor.blend(Color.white, sat);
			cSat
		};
	}

	*paletteCursor {
		^{ | v |
			// Map values above the threshold to a green shade
			// Map values below the threshold to gray
			if (v > VRPDataVRP.clarityThreshold,
				Color.green(v**3),
				Color.gray
			)
		};
	}
}

+ VRPDataCluster {
	*palette { | nClusters |
		^{ | v |
			var color, cHue, sat;
			(v.class == Array).if(
				{	// invoked with [index, count]
					sat = v[1].clip(1, 100).linlin(1, 100, 0, 0.7);
					color = Color.hsv(v[0].linlin(0.0, nClusters, 0.0, 0.999), sat, 1);
				},{ // invoked with index only
					cHue = v.linlin(0.0, nClusters, 0.0, 0.999);
					color = Color.hsv(cHue, 0.7, 1);
			});
			color
		};
	}
}