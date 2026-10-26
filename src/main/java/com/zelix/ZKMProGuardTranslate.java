package com.zelix;

import com.zelix.tools.proguardConfigurationTranslate.ProguardConfigUtils;
import com.zelix.utils.FileUtils;
import com.zelix.utils.TempFileManager;
import com.zelix.utils.references.AtomicReference;
import java.io.File;
import java.io.IOException;
import java.util.Properties;

public class ZKMProGuardTranslate {
	public static void main(String[] args) throws IOException {
		if (args.length != 2) {
			System.out.println("Usage: java com.zelix.ZKMProGuardTranslate <proguardConfigFileName> <outputFileName>");
			System.exit(1);
		}

		File configFile = new File(args[0]);
		File outputFile = new File(args[1]);
		if (!configFile.exists() || configFile.isDirectory()) {
			System.out.println("File '" + configFile.getAbsolutePath() + "' doesn't exist or is a directory");
		} else if (outputFile.isDirectory()) {
			System.out.println("File '" + outputFile.getAbsolutePath() + "' is a directory");
		} else {
			String translatedConfig = ProguardConfigUtils.translateProGuardConfig(FileUtils.readToString(configFile), new Properties());
			AtomicReference<String> errorMessage = new AtomicReference<>();
			FileUtils.writeStringToFile(translatedConfig, outputFile, errorMessage);
			if (errorMessage.isNull()) {
				System.out.println("Translated contents of '" + configFile.getAbsolutePath() + "' written to '" + outputFile.getAbsolutePath() + "'");
			} else {
				System.out.println("ERROR: '" + errorMessage.get() + "'");
			}
		}
	}

	public void close() {
		TempFileManager.cleanupOldTempFiles();
	}

	public String getTranslatedStackTrace(String configText) {
		return ProguardConfigUtils.translateProGuardConfig(configText, new Properties());
	}
}
