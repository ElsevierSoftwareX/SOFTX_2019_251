// Copyright (C) 2016-2019 by Dennis J. Johansson, 
// Released under European Union Public License v1.2, at https://eupl.eu
// *** EUPL *** //
ScopeViewer {
	var mUV;
	var mHSpec;
	var mVSpec;
	var mfMakeGrid;

	var mGrid;
	var mGridFontColor;
	var mPoints;
	var mAmplitudesArray;
	var mColors;
	var <>colors;
	var mPoints;

	// Timestamp bases for the client and server
	var mServerBase;
	var mClientBase;
	var mShift;

	*new { | parent, hspec, vspec |
		^super.new.init(parent, hspec, vspec);
	}

	init { | parent, hspec, vspec |
		mUV = UserView(parent, parent.bounds.moveTo(0, 0));

		mHSpec = hspec;
		mVSpec = vspec;

		mGridFontColor = Color.white;
		colors = nil;

		mfMakeGrid = { arg color = mGridFontColor;
			mGrid = DrawGrid(mUV.bounds.moveTo(0, 0), GridLines(mHSpec), GridLines(mVSpec))
			.gridColors_( [Color.gray(0.3), Color.gray(0.3)] )
			.smoothing_(false)
			.fontColor_(color);
		};

		this.reset;

		mfMakeGrid.(mGridFontColor);

		mUV.drawFunc_ {
			var b = mUV.bounds;
			mGrid.bounds_(b.moveTo(0, 0));
			mGrid.x.labelOffset_(4 @ b.height.neg); // top-align the x labels
			mGrid.draw;

			if (mPoints.notNil, {
				if (mPoints.notEmpty, {
					// The shift is generated due to not necessarily grabbing data at a constant and fast enough rate
					// Hence we check how long ago since we got our first timestamp both on the server and client, this
					// difference is the necessary shift. It is essentially an estimate of where the server's timestamp should be.
					var shift = mShift;
					if ( mServerBase != inf, {
						shift = (mPoints.last.x - mServerBase) - (Process.elapsedTime - mClientBase);
						mShift = shift;
					});

					Pen.use {
						var xscale = b.width / mHSpec.range;
						var yscale = b.height / mVSpec.range;
						Pen.width = 0.15;
						//Pen.smoothing_(false);
						Pen.translate(0, b.height / 2); // The UserViews coordinate system doesn't start in the middle
						Pen.scale(xscale, yscale.neg); // Scale the points to match the window
						Pen.translate(
							mPoints.first.x.neg + mHSpec.range - (mPoints.last.x - mPoints.first.x) + shift, // Timestamps don't start at 0 - so move them to end at mHSpec.clipHi
							( mVSpec.clipLo + mVSpec.range.half ).neg // The vertical axis is not necessarily centered around 0 - so move the center down to 0
						);

						mAmplitudesArray do: { | amps, idx |
							var t_prev = 0.0;
							var t_diff = 0.0;
							amps do: { | amp, i | mPoints[i].y = amp; };

							Pen.moveTo( mPoints.first );
							mPoints do: { | p |
								t_diff = p.x - t_prev;
								if (t_diff < 0.05,
									{
										Pen.moveTo(t_prev@(p.y));
										Pen.lineTo(p);
									},{
										Pen.moveTo(p)
									}
								);
								t_prev = p.x;
							};
							Pen.strokeColor = mColors[idx];
							Pen.stroke;
						};
					}; // Pen.use
				}); // Not empty
			}); // Not nil
		}; // drawFunc_
	}

	//
	// Will superpose all scoped buses if amplitudesArray contains amplitudes from more than one bus.
	//
	update { | timestamps, amplitudesArray, paused=false |
		if ( timestamps.last < mServerBase, {
			mClientBase = Process.elapsedTime;
			mServerBase = timestamps.last;
		});

		if (paused, {
			mServerBase = inf;
		});

		mPoints = timestamps collect: { | time | Point(time) };
		mAmplitudesArray = amplitudesArray;
		if (colors.notNil, {
			mColors = colors;
		}, {
			mColors = Array.iota(mAmplitudesArray.size) collect: { | val |
				var hue = val.linlin(0, mAmplitudesArray.size, 0, 1);
				Color.hsv( hue, 0.5, 1 ) };
		});

		this.refresh;
	}

	reset {
		mServerBase = inf;
		mPoints = nil;
		mShift = 0;
	}

	stop {
		mServerBase = inf;
	}

	gridFontColor_ { |c|
		mGridFontColor = c;
		this.refresh;
    }

	gridFontColor {
		^mGridFontColor
	}

	hspec { ^mHSpec; }
	hspec_ { | spec |
		mHSpec = spec;
		mfMakeGrid.(mGridFontColor);
	}

	vspec { ^mVSpec; }
	vspec_ { | spec |
		mVSpec = spec;
		mfMakeGrid.(mGridFontColor);
	}

	view { ^mUV; }

	refresh { { mUV.refresh }.defer }
}
