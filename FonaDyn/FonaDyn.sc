/* This installation assumes that files have been unpacked as follows:

	...\Extensions\FonaDyn: including FonaDyn.sc and GridLinesExp.sc.txt
	...\Extensions\FonaDynTools
	...\Extensions\FonaDynTools\win32
	...\Extensions\FonaDynTools\win64
	...\Extensions\FonaDynTools\macos, including libfftw3f.3.dylib and a recompiled PitchDetection.scx
	...\Extensions\FonaDynTools\linux

Three of the four subfolders will be deleted during the installation.
*/

FonaDyn {

	*run { arg bReRun = false;
		var setPathStr;
		var resDir = Platform.resourceDir;
		var currentPath = "PATH".getenv;
		if (currentPath.notNil and: { currentPath.find(resDir, true).isNil }, {
			setPathStr = resDir ++ ";" ++ currentPath;
			"PATH".setenv(setPathStr);
		});
		VRPMain.new(bReRun);
	}

	*rerun {
		FonaDyn.run(true);
	}

	*setPaths {
		var fdAllUsers, fdExtPath, plugsAllUsers, plugsPath;

		// Find out if user has copied FonaDyn "per-user", or "system-wide"
		fdExtPath = PathName(VRPMain.class.filenameSymbol.asString.standardizePath).parentPath; // classes
		fdExtPath = PathName(fdExtPath).parentPath; // FonaDyn
		fdExtPath = PathName(fdExtPath).parentPath.asString.withoutTrailingSlash; // Extensions
		fdAllUsers = (fdExtPath == thisProcess.platform.systemExtensionDir);

		if (fdAllUsers,
			{ ~fdExtensions = thisProcess.platform.systemExtensionDir; },
			{ ~fdExtensions = thisProcess.platform.userExtensionDir; }
		);
		("Found FonaDyn in" + ~fdExtensions).postln;

		~fdProgram = ~fdExtensions +/+ "FonaDyn";
		~fdProgramTools = ~fdExtensions +/+ "FonaDynTools";

		// Find out if user has installed SC3Plugins "per-user", or "system-wide"
		plugsPath = PathName(Tartini.class.filenameSymbol.asString.standardizePath).parentPath; // classes
		plugsPath = PathName(plugsPath).parentPath; // PitchDetection
		plugsPath = PathName(plugsPath).parentPath; // SC3plugins
		plugsPath = PathName(plugsPath).parentPath.asString.withoutTrailingSlash; // Extensions
		plugsAllUsers = (plugsPath == thisProcess.platform.systemExtensionDir);

		if (plugsAllUsers,
			{ ~plugsExtensions = thisProcess.platform.systemExtensionDir; },
			{ ~plugsExtensions = thisProcess.platform.userExtensionDir; }
		);
		("Found SC3-plugins in" + ~plugsExtensions).postln;
	}

	*removeFolder { arg folder;
		var rmCmd;

		rmCmd = Platform.case(
			\windows, { "rmdir /s /q" },
			\osx,     { "rm -R" },
			\linux,   { "rm -R" }
		);

		rmCmd = rmCmd +  "\"" ++ folder ++ "\"";
		rmCmd.postln;
		rmCmd.unixCmd;
	}

	*install {
		var success;
		var dirName, fName;

		// Check that the SC3 plugins are installed
		// and post instructions if they are not.
		if (thisProcess.platform.hasFeature(\Tartini).not,
			{ FonaDyn.promptInstallSC3plugins
			},{
				FonaDyn.setPaths;

				if (Main.versionAtMost(3,9),
					{
						postln ("FonaDyn will run best on SuperCollider 3.10.2 or higher.");
						postln ("This SuperCollider is at version" + Main.version + ".");
						postln ("Some FonaDyn functionality may be limited or incorrect.");
				});

				// Move the log-grid option to the proper location
				// The .txt file is ignored, it can stay.
				// fName = PathName(~fdExtensions +/+ "SystemOverwrites" +/+ "GridLinesExp.sc");

				dirName = ~fdExtensions +/+ "SystemOverwrites" ;
				dirName.mkdir;
				fName = dirName +/+ "GridLinesExp.sc";
				if (File.exists(fName),
					{ (fName + "exists - ok,").postln },
					{ File.copy(~fdProgram +/+ "GridLinesExp.sc.txt", fName)}
				);

				success = Platform.case(
					\windows, { FonaDyn.install_win },
					\osx,     { FonaDyn.install_osx },
					\linux,   { FonaDyn.install_linux }
				);

				if (success == true,
					{ postln ("FonaDyn was installed successfully.") },
					{ postln ("There was a problem with the installation.")}
				);
		});
	}

	*install_win {
		var retval = false;
		var cpu = "PROCESSOR_ARCHITECTURE".getenv.toUpper;

		if (cpu == "X86", {
			FonaDyn.removeFolder(~fdProgramTools +/+ "win64");
			postln ("Installing Win32 plugins");
		},{
			FonaDyn.removeFolder(~fdProgramTools +/+ "win32");
			postln ("Installing Win64 plugins");
		});
		FonaDyn.removeFolder(~fdProgramTools +/+ "macos");
		FonaDyn.removeFolder(~fdProgramTools +/+ "linux");
		^retval = true
	}

	*install_osx {
		var retval = false;
		// Rename the original PitchDetection.scx so that ours becomes the active one
		var scxName = "PitchDetection/PitchDetection.scx";
		var fftwLibPath = "/usr/local/lib";
		var fftwLibName = "libfftw3f.3.dylib";
		var destPath;
		var cmdLine;
		destPath = ~plugsExtensions +/+ "SC3plugins" +/+ scxName;
		if (File.exists(destPath), {
			cmdLine = "mv \"" ++ destPath ++ "\" \"" ++ destPath ++".original\"";
			cmdLine.postln;
			cmdLine.unixCmd;
			postln (scxName + "overridden.");
		},{
			("Did not find "+ scxName).postln;
		});

		// Install the FFTW library where our recompiled PitchDetection.scx expects to find it
		// destPath = fftwLibPath +/+ fftwLibName;
		if (File.exists(destPath),
			{ (destPath + "exists - ok,").postln },
			{
				var srcPath = ~fdProgramTools +/+ "macos" +/+ fftwLibName;
				cmdLine ="install -CSpv \"" ++ srcPath ++ "\" " ++ fftwLibPath;
				cmdLine.postln;
				cmdLine.unixCmd;
			};
		);

		FonaDyn.removeFolder(~fdProgramTools +/+ "win32");
		FonaDyn.removeFolder(~fdProgramTools +/+ "win64");
		FonaDyn.removeFolder(~fdProgramTools +/+ "linux");
		^true
	}

	*uninstall_osx {
		var retval = false;
		// Restore the name of the original PitchDetection.scx
		var scxName = "PitchDetection/PitchDetection.scx";
		var srcPath, destPath;
		var cmdLine;
		destPath = ~plugsExtensions +/+ "SC3plugins" +/+ scxName;
		srcPath = destPath ++ ".original";
		if (File.exists(srcPath), {
			cmdLine = "mv \"" ++ srcPath ++ "\" \"" ++ destPath ++ "\"";
			cmdLine.postln;
			cmdLine.unixCmd;
			postln (scxName + "restored.");
		},{
			("Did not find "+ scxName).postln;
		});
		^true
	}

	*install_linux {
		FonaDyn.removeFolder(~fdProgramTools +/+ "win32");
		FonaDyn.removeFolder(~fdProgramTools +/+ "win64");
		FonaDyn.removeFolder(~fdProgramTools +/+ "macos");
		^true
	}

	*uninstall {
		var dirName;
		var fName;

		Server.killAll;  // otherwise the plugin binaries won't be deleted
		FonaDyn.setPaths;
		dirName = ~fdExtensions +/+ "SystemOverwrites" ;
		fName = dirName +/+ "GridLinesExp.sc";
		warn("This removes all FonaDyn code, including any changes you have made.");
		if (File.exists(fName),	{
			File.delete(fName);
			(fName + "removed.").postln;
		});
		Platform.case(
			\osx,     { FonaDyn.uninstall_osx }
		);

		FonaDyn.removeFolder(~fdProgram);
		FonaDyn.removeFolder(~fdProgramTools);
	}

	*promptInstallSC3plugins {
		postln ("The \"SC3 plugins\" are not yet installed.");
		postln ("Download the version for your system,");
		postln ("and follow the instructions in its file README.txt.");
		postln ("Then re-run this installation.");
	}

} /* class FonaDyn */


