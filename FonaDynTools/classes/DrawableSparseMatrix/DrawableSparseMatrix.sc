// Copyright (C) 2016-2019 by Dennis J. Johansson, 
// Released under European Union Public License v1.2, at https://eupl.eu
// *** EUPL *** //
DrawableSparseMatrix {
	var mPalette;  // function from VRPColorMap.paletteCluster
	var mValues; // Triplets of x, y, color
	var mNewest; // Triplets of x, y, color

	var mIdx; // Index of the triplet at pos x, y, or nil
	var mVal; // The actual matrix - not used for drawing
	var mCols;
	var mRows;
	var mbFullRedrawNeeded;
	var <>mbActive; // True if "this" is being displayed
	var <>refDensity;
	var <>thresholdCount;

	*new { | rows, cols, palette |
		^super.new.init(rows, cols, palette);
	}

	init { | rows, cols, palette |
		mCols = cols;
		mRows = rows;
		mPalette = palette;
		mbFullRedrawNeeded = true;
		mbActive = false;
		refDensity = nil;
		thresholdCount = 0;

		mValues = List(cols*rows*0.25);    // avoid re-allocations until the List becomes really big
		mNewest = List.newClear();
		mIdx = Array.fill(mRows * mCols);
		mVal = Array.fill(mRows * mCols);
	}

	put { | row, col, value |
		var idx = row * mCols + col;
		var pos = mIdx[ idx ];
		if ( pos.isNil,
			{
				mIdx[ idx ] = mValues.size;
				mValues.add( [row, col, mPalette.(value)] );
			}, {
				mValues[pos][2] = mPalette.(value);
			});
		mVal[ idx ] = value;
		mbActive.if { mNewest.add( mValues[mIdx[ idx ]] ) } ;
	}

	at { | row, col |
		^mVal[ row * mCols + col ]
	}

	rows { ^mRows }
	columns { ^mCols }
	size { ^mValues.size }

	recolor { | newPalette |
		var index, value;
		mPalette = newPalette;
		mValues.do ( { | t, pos |
			index = t[0]*mCols + t[1];
			value = mVal[index];
			if (value.notNil, {
				mValues[pos][2] = mPalette.(value)
			} )
		});
	}

	renumber { arg newOrder;
		var index, value, nc;
		mValues.do ( { | t, pos |
			index = t[0]*mCols + t[1];
			value = mVal[index];
			if (value.class == Array, {
				nc = newOrder[value[0]];
				mVal[index][0] = nc;
				mValues[pos][2] = mPalette.([nc, value[1]]);
			} )
		});
	}

	draw { | userView |
		var b = userView.bounds;
		var r = Rect(-0.5, -0.5, 1, 1);
		var drawList;

		if (mbFullRedrawNeeded, {
			drawList = mValues;
			mbFullRedrawNeeded = false;
		},{
			drawList = mNewest;
		});
		Pen.use {
			Pen.scale(b.width / (mCols-1), b.height / (mRows-1));
			Pen.width_(0);
			Pen.smoothing_(false);
			// If thresholding, we need first to check the total of cycles in each cell
			if (thresholdCount>1 and: refDensity.notNil, {
				drawList do: { | triplet |
					var x, y, color;
					#y, x, color = triplet;
					if (refDensity.at(y, x) >= thresholdCount, {
						Pen.translate(x, y);
						Pen.fillColor = color;
						Pen.fillRect(r);
						Pen.translate(x.neg, y.neg);
					})
			}},{
			// If not thresholding, just draw all cells
				drawList do: { | triplet |
					var x, y, color;
					#y, x, color = triplet;
					Pen.translate(x, y);
					Pen.fillColor = color;
					Pen.fillRect(r);
					Pen.translate(x.neg, y.neg);
			}});
		};
		mNewest.clear;
	}

	setPalette { arg palette;
		mPalette = palette;
	}

	invalidate {
		mbFullRedrawNeeded = true;
	}
}