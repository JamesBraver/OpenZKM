package com.zelix;

import com.sun.kvem.environment.Obfuscator;
import com.zelix.klassmaster.AbstractBootstrap;
import java.io.File;

public class ZKMWtkPlugin implements Obfuscator {
	private File jadFile;

	public void createScriptFile(File jadFile, File projectDir) {
		this.jadFile = jadFile;
		if (this.jadFile == null) {
			throw new IllegalArgumentException("ZKM: Null JAD file.");
		} else if (!this.jadFile.exists()) {
			throw new IllegalArgumentException("ZKM: JAD file '" + this.jadFile.getAbsolutePath() + "' does not exist.");
		}
	}

	public void run(File outputJarFile, String wtkBinDir, String wtkLibDir, String inputJarFileName, String projectDirName, String classpath, String emptyAPI) {
		AbstractBootstrap.processMIDlet(this.jadFile, inputJarFileName, outputJarFile, classpath, projectDirName);
	}
}
