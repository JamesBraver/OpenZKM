package com.zelix.klassmaster.config;

import com.zelix.exceptions.DebugAssertFailedException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.LinkedHashMap;

public class RuleFilterConfig extends FilterBaseConfig implements Serializable {
	public static RuleFilterConfig loadFromFile(String directory, String fileName) {
		File configFile = new File(directory, fileName);
		if (configFile.exists() && !configFile.isDirectory() && configFile.canRead()) {
			ObjectInputStream objectInputStream = null;

			RuleFilterConfig loadedConfig;
			try {
				FileInputStream fileInputStream = new FileInputStream(configFile);
				objectInputStream = new ObjectInputStream(fileInputStream);
				loadedConfig = (RuleFilterConfig)objectInputStream.readObject();
				if (loadedConfig.parameterMap == null) {
					loadedConfig.parameterMap = new LinkedHashMap<>();
				}

				loadedConfig.persisted = true;
				return loadedConfig;
			} catch (IOException ignoredDeserialize) {
				return new RuleFilterConfig();
			} catch (ClassNotFoundException ignoredClassNotFound) {
				return new RuleFilterConfig();
			} catch (ClassCastException ignoredClassCast) {
				return new RuleFilterConfig();
			} catch (DebugAssertFailedException debugAssertFailedException) {
				throw debugAssertFailedException;
			} catch (Throwable ignoredThrowable) {
				loadedConfig = new RuleFilterConfig();
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
			return new RuleFilterConfig();
		}
	}
}
