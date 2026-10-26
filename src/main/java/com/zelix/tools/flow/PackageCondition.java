package com.zelix.tools.flow;

public class PackageCondition {
	private final Condition groupCondition;
	private final Condition packageCondition;
	private final String packageName;

	public PackageCondition(Condition groupCondition, Condition packageCondition, String packageName) {
		this.groupCondition = groupCondition;
		this.packageCondition = packageCondition;
		this.packageName = packageName;
	}

	public Condition getGroupCondition() {
		return this.groupCondition;
	}

	public String getPackageName() {
		return this.packageName;
	}

	public Condition getPackageCondition() {
		return this.packageCondition;
	}
}
