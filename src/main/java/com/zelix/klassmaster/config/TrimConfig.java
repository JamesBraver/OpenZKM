package com.zelix.klassmaster.config;

import com.zelix.exceptions.DebugAssertFailedException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class TrimConfig implements Serializable {
	public boolean deleteSourceFileAttributes = false;
	public boolean deleteDeprecatedAttributes = true;
	public boolean deleteAnnotationAttributes = false;
	public boolean deleteUnknownAttributes = false;
	public boolean deleteExceptionAttributes = false;
	public boolean deleteDebugExtensionAttributes = true;

	public TrimConfig() {
	}

	public TrimConfig(String directory, String fileName) {
		File configFile = new File(directory, fileName);
		if (configFile.exists() && !configFile.isDirectory() && configFile.canRead()) {
			ObjectInputStream objectInputStream = null;

			try {
				FileInputStream fileInputStream = new FileInputStream(configFile);
				objectInputStream = new ObjectInputStream(fileInputStream);
				TrimConfig loadedConfig = (TrimConfig)objectInputStream.readObject();
				this.deleteSourceFileAttributes = loadedConfig.deleteSourceFileAttributes;
				this.deleteDeprecatedAttributes = loadedConfig.deleteDeprecatedAttributes;
				this.deleteUnknownAttributes = loadedConfig.deleteUnknownAttributes;
				this.deleteAnnotationAttributes = loadedConfig.deleteAnnotationAttributes;
				this.deleteExceptionAttributes = loadedConfig.deleteExceptionAttributes;
				this.deleteDebugExtensionAttributes = loadedConfig.deleteDebugExtensionAttributes;
			} catch (IOException ignoredDeserialize) {
			} catch (ClassNotFoundException ignoredClassNotFound) {
			} catch (ClassCastException ignoredClassCast) {
			} catch (DebugAssertFailedException debugAssertFailedException) {
				throw debugAssertFailedException;
			} catch (Throwable ignoredThrowable) {
			} finally {
				if (objectInputStream != null) {
					try {
						objectInputStream.close();
					} catch (IOException ignoredClose) {
					}
				}
			}
		}
	}

	public void saveToFile(String directory, String fileName) {
		File configFile = new File(directory, fileName);
		if (!configFile.exists() || !configFile.isDirectory() && configFile.canWrite()) {
			ObjectOutputStream objectOutputStream = null;

			try {
				FileOutputStream fileOutputStream = new FileOutputStream(configFile);
				objectOutputStream = new ObjectOutputStream(fileOutputStream);
				objectOutputStream.writeObject(this);
			} catch (IOException ignoredSerialization) {
			} finally {
				if (objectOutputStream != null) {
					try {
						objectOutputStream.close();
					} catch (IOException ignoredClose) {
					}
				}
			}
		}
	}
}
