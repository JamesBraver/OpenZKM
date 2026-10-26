package com.zelix.klassmaster.config;

import com.zelix.exceptions.DebugAssertFailedException;
import com.zelix.klassmaster.config.base.ConfigBase;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.LinkedHashMap;

public class ReferenceObfuscateConfig extends ConfigBase {
	private ReferenceObfuscateConfig() {
	}

	public static ReferenceObfuscateConfig loadFromFile(String directory, String fileName) {
		File configFile = new File(directory, fileName);
		if (configFile.exists() && !configFile.isDirectory() && configFile.canRead()) {
			ObjectInputStream objectInputStream = null;

			ReferenceObfuscateConfig loadedConfig;
			try {
				FileInputStream fileInputStream = new FileInputStream(configFile);
				objectInputStream = new ObjectInputStream(fileInputStream);
				loadedConfig = (ReferenceObfuscateConfig)objectInputStream.readObject();
				if (loadedConfig.parameterMap == null) {
					loadedConfig.parameterMap = new LinkedHashMap<>();
				}

				loadedConfig.persisted = true;
				return loadedConfig;
			} catch (IOException ignoredDeserialize) {
				return new ReferenceObfuscateConfig();
			} catch (ClassNotFoundException ignoredClassNotFound) {
				return new ReferenceObfuscateConfig();
			} catch (ClassCastException ignoredClassCast) {
				return new ReferenceObfuscateConfig();
			} catch (DebugAssertFailedException debugAssertFailedException) {
				throw debugAssertFailedException;
			} catch (Throwable ignoredThrowable) {
				loadedConfig = new ReferenceObfuscateConfig();
			} finally {
				if (objectInputStream != null) {
					try {
						objectInputStream.close();
					} catch (IOException ignoredClose) {
					}
				}
			}

			return loadedConfig;
		} else {
			return new ReferenceObfuscateConfig();
		}
	}
}
