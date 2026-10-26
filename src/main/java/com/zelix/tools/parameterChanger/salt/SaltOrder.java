package com.zelix.tools.parameterChanger.salt;

public class SaltOrder {
	private final SaltEntry keyEntry;
	private final SaltEntry valueEntry;
	private final Long methodValue;
	private final Long methodKey;
	private final Long rotationKey;

	public SaltOrder(SaltEntry keyEntry, SaltEntry valueEntry, Long methodValue, Long methodKey, Long rotationKey) {
		this.keyEntry = keyEntry;
		this.valueEntry = valueEntry;
		this.methodValue = methodValue;
		this.methodKey = methodKey;
		this.rotationKey = rotationKey;
	}

	public Long getRotationKey() {
		return this.rotationKey;
	}

	public SaltEntry getKeyEntry() {
		return this.keyEntry;
	}

	public Long getMethodKey() {
		return this.methodKey;
	}

	public SaltEntry getValueEntry() {
		return this.valueEntry;
	}

	public Long getMethodValue() {
		return this.methodValue;
	}
}
