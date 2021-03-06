// Copyright (C) 2019 by Sten Ternström & Dennis J. Johansson, KTH Stockholm
// Released under European Union Public License v1.2, at https://eupl.eu
// *** EUPL *** //

VRPViewMain {
	// Views
	var mView;

	var mViewMenu;
	var mVRPViewMenu;

	var mViewSampEn;
	var mVRPViewSampEn;

	var mViewCluster;
	var mVRPViewCluster;

	var mViewVRP;
	var mVRPViewVRP;

	var mViewMovingEGG;
	var mVRPViewMovingEGG;

	// States
	var mStackLayout;
	var mCurrentLayout;
	var mCurrentStackType;

	// Layout types
	classvar <layoutGrid = 1;
	classvar <layoutStack = 2;

	classvar <stackTypeSampEn = 1;
	classvar <stackTypeCluster = 2;
	classvar <stackTypeVRP = 3;
	classvar <stackTypeMovingEGG = 4;

	*new { | view |
		^super.new.init(view);
	}

	init { | view |
		mView = view;

		// Create the views and menu
		mViewMenu = CompositeView(mView, mView.bounds);
		mViewSampEn = CompositeView(mView, mView.bounds);
		mViewMovingEGG = CompositeView(mView, mView.bounds);
		mViewVRP = CompositeView(mView, mView.bounds);
		mViewCluster = CompositeView(mView, mView.bounds);

		// Fix the layout - so the subobjects know their sizes
		mStackLayout = nil;
		mCurrentLayout = layoutGrid;
		mCurrentStackType = stackTypeVRP;
		this.setLayout(layoutGrid);

		// Init the subobjects
		mVRPViewMenu = VRPViewMainMenu( mViewMenu );
		mVRPViewSampEn = VRPViewSampEn( mViewSampEn );
		mVRPViewMovingEGG = VRPViewMovingEGG( mViewMovingEGG );
		mVRPViewVRP = VRPViewVRP( mViewVRP );
		mVRPViewCluster = VRPViewCluster( mViewCluster );

		// press Alt + one of c|v|m|p to toggle the visibility of any graph
		mView.keyDownAction_({ arg v, c, mods, u, kCode, k;
			var bHandled = false;
			if ((c.toLower > $a) and: (mods.isAlt), {
				switch (c.toLower,
					$c, {
						mViewCluster.visible_(mViewCluster.visible.not);
						bHandled = true;
					},
					$v, {
						mViewVRP.visible_(mViewVRP.visible.not);
						bHandled = true;
					},
					$m, {
						mViewMovingEGG.visible_(mViewMovingEGG.visible.not);
						bHandled = true;
					},
					$p, {
						mViewSampEn.visible_(mViewSampEn.visible.not);
						bHandled = true;
				});
			});
			bHandled
		});
	}

	vrpViewsArray {
		^[
			mVRPViewMenu,
			mVRPViewSampEn,
			mVRPViewMovingEGG,
			mVRPViewCluster,
			mVRPViewVRP
		];
	}

	setLayout { | l |
		// Ugly hack to fix a bug (Swapping from a StackLayout to another layout without looking at the correct item (top left from experimentation) => Bugs)
		if (mCurrentLayout == layoutStack, {
			mStackLayout.index_(2);
		});

		mView.layout_(
			this.layout(
				l,
				mViewMenu,
				mViewVRP,
				mViewCluster,
				mViewSampEn,
				mViewMovingEGG
			)
		);

		mCurrentLayout = l;
		mView.refresh;
	}

	layout { | type, menu, vrp, cluster, sampen, movingEGG |

		^VLayout(
			[menu, stretch: 1],
			[
				switch ( type,

					// Layout like a grid
					layoutGrid, {
						HLayout(
							[VLayout(
								[
									HLayout(
										[sampen, stretch: 1],
										[movingEGG, stretch: 1]
									),
									stretch: 1
								],
								[cluster, stretch: 1]
							), stretch: 1],
							[vrp, stretch: 1]
						)
					},

					// Layout using a stack
					layoutStack, {
						mStackLayout = StackLayout(
							vrp,
							cluster,
							sampen,
							movingEGG
						)
					}
				)
			, stretch: 10] // Force the menu to take up as little space as possible!
		);
	}

	stash { | settings |
		this.vrpViewsArray
		do: { | x |
			x.stash(settings);
		};
	}

	fetch { | settings |
		var lo, st, bNewLayout;

		this.vrpViewsArray
		do: { | x |
			x.fetch(settings);
		};

		// Change layout?
		lo = settings.general.layout;
		st = settings.general.stackType;
		bNewLayout = (lo != mCurrentLayout);
		if (mStackLayout.notNil, { if (mCurrentStackType != st, { bNewLayout = true; }) });

		if ( bNewLayout, {
			this.setLayout(lo);
			if (layoutStack == lo, {
				mStackLayout.index = switch( st,
					VRPViewMain.stackTypeVRP, 0,
					VRPViewMain.stackTypeCluster, 1,
					VRPViewMain.stackTypeSampEn, 2,
					VRPViewMain.stackTypeMovingEGG, 3
				);
				mCurrentStackType = st;
			});
		});
	}

	updateData { | data |
		var dsg = data.settings.general;

		this.vrpViewsArray
		do: { | x |
			x.updateData(data);
		};
		if (dsg.guiChanged, {
			mView.background_(dsg.getThemeColor(\backDrop));
			dsg.guiChanged = false; // Now all windows have done it
		});
	}

	close {
		this.vrpViewsArray
		do: { | x |
			x.close;
		};
	}
}