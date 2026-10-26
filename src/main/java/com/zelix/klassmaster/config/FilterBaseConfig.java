package com.zelix.klassmaster.config;

import com.zelix.klassmaster.config.base.ConfigBase;

public abstract class FilterBaseConfig extends ConfigBase {
	protected boolean excludeMainClass = false;
	public boolean excludeMainMethod;
	protected transient int applicationType;
	protected String mainClassName;

	public int getApplicationType() {
		return this.applicationType;
	}

	public void setApplicationType(int applicationType) {
		this.applicationType = applicationType;
	}

	public void setExcludeMainClass(boolean excludeMainClass, String mainClassName, boolean excludeMainMethod) {
		this.excludeMainClass = excludeMainClass;
		if (this.excludeMainClass) {
			this.mainClassName = mainClassName;
			this.excludeMainMethod = excludeMainMethod;
		} else {
			this.mainClassName = null;
		}
	}

	public String getMainClassName() {
		return this.mainClassName;
	}

	public boolean isExcludeMainClass() {
		return this.excludeMainClass;
	}

	public boolean isExcludeMainMethod() {
		return this.excludeMainMethod;
	}
}
