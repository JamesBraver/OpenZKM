package com.zelix.klassmaster.config.base;

import com.zelix.utils.DebugUtils;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class ConfigBase implements Serializable {
	protected Map<String, String> parameterMap = new LinkedHashMap<>();
	protected transient boolean persisted;

	public final int getParameterCount() {
		return this.parameterMap.size();
	}

	public final Enumeration<String> getParameterEnumeration() {
		DebugUtils.assertNotNull(this.parameterMap, "Unexpected null value (A) : " + this.persisted);
		Set<String> parameterNames = this.parameterMap.keySet();
		DebugUtils.assertNotNull(parameterNames, "Unexpected null value (B) : " + this.persisted);
		return Collections.enumeration(parameterNames);
	}

	public final void saveToFile(String directory, String fileName) {
		File outputFile = new File(directory, fileName);
		if (!outputFile.exists() || !outputFile.isDirectory() && outputFile.canWrite()) {
			ObjectOutputStream objectOutputStream = null;

			try {
				FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
				objectOutputStream = new ObjectOutputStream(fileOutputStream);
				objectOutputStream.writeObject(this);
				this.persisted = true;
			} catch (IOException ignoredWriteFailure) {
			} finally {
				if (objectOutputStream != null) {
					try {
						objectOutputStream.close();
					} catch (IOException ignoredCloseFailure) {
					}
				}
			}
		}
	}

	public final List<String> getParameters() {
		return new ArrayList<>(this.parameterMap.keySet());
	}

	public void setParameters(List<String> ruleStrings) {
		this.parameterMap.clear();

		for (int parameterIndex = 0; parameterIndex < ruleStrings.size(); parameterIndex++) {
			String parameterName = ruleStrings.get(parameterIndex);
			this.parameterMap.put(parameterName, parameterName);
		}
	}

	public final boolean isPersisted() {
		return this.persisted;
	}
}
