package com.zelix.klassmaster.config;

import com.zelix.exceptions.DebugAssertFailedException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;

public class FilterConfig extends FilterBaseConfig implements Serializable {
	public static final String ALL_PACKAGE_QUALIFIER = "*.";
	private boolean excludePackageQualifier = false;
	private String packageQualifier = null;

	public static FilterConfig loadFromFile(String directory, String fileName) {
		File configFile = new File(directory, fileName);
		if (configFile.exists() && !configFile.isDirectory() && configFile.canRead()) {
			ObjectInputStream objectInputStream = null;

			FilterConfig loadedConfig;
			try {
				FileInputStream fileInputStream = new FileInputStream(configFile);
				objectInputStream = new ObjectInputStream(fileInputStream);
				loadedConfig = (FilterConfig)objectInputStream.readObject();
				if (loadedConfig.parameterMap == null) {
					loadedConfig.parameterMap = new LinkedHashMap<>();
				}

				loadedConfig.persisted = true;
				return loadedConfig;
			} catch (IOException ignoredDeserialize) {
				return new FilterConfig();
			} catch (ClassNotFoundException ignoredClassNotFound) {
				return new FilterConfig();
			} catch (ClassCastException ignoredClassCast) {
				return new FilterConfig();
			} catch (DebugAssertFailedException debugAssertFailedException) {
				throw debugAssertFailedException;
			} catch (Throwable ignoredThrowable) {
				loadedConfig = new FilterConfig();
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
			return new FilterConfig();
		}
	}

	private boolean isValidPackageQualifier(String packageQualifier) {
		return packageQualifier != null && packageQualifier.length() > 0 && packageQualifier.indexOf("*") == -1 && packageQualifier.indexOf("^") == -1 && packageQualifier.charAt(packageQualifier.length() - 1) == '.';
	}

	@Override
	public void setParameters(List<String> ruleStrings) {
		super.parameterMap.clear();
		int matchedQualifierCount = 0;
		String firstPackageQualifier = null;

		for (int i = 0; i < ruleStrings.size(); i++) {
			String ruleString = ruleStrings.get(i);
			super.parameterMap.put(ruleString, ruleString);
			if (this.isValidPackageQualifier(ruleString)) {
				if (++matchedQualifierCount == 1) {
					firstPackageQualifier = ruleString;
				}
			}
		}

		if (super.mainClassName != null && super.mainClassName.length() > 0) {
			if (super.excludeMainClass) {
				if (!super.parameterMap.containsKey(super.mainClassName)) {
					super.excludeMainClass = false;
				}
			} else if (super.parameterMap.containsKey(super.mainClassName)) {
				super.excludeMainClass = true;
			}
		}

		if (this.excludePackageQualifier) {
			if (!super.parameterMap.containsKey(this.packageQualifier)) {
				this.excludePackageQualifier = false;
			}
		} else if (super.parameterMap.containsKey(this.packageQualifier)) {
			this.excludePackageQualifier = true;
		}

		if (!this.excludePackageQualifier && matchedQualifierCount > 0) {
			this.excludePackageQualifier = true;
			this.packageQualifier = firstPackageQualifier;
		}
	}

	public void setPackageQualifier(boolean excludePackageQualifier, String packageQualifier) {
		this.excludePackageQualifier = excludePackageQualifier;
		this.packageQualifier = packageQualifier;
	}

	public String getPackageQualifier() {
		return this.packageQualifier;
	}

	public boolean isExcludePackageQualifier() {
		return this.excludePackageQualifier;
	}
}
