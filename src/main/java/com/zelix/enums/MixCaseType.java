package com.zelix.enums;

import java.io.Serializable;

public class MixCaseType implements Serializable {
	public static final MixCaseType ALWAYS = new MixCaseType(0);
	public static final MixCaseType NEVER = new MixCaseType(1);
	public static final MixCaseType IF_IN_ARCHIVE = new MixCaseType(2);
	private static final MixCaseType[] VALUES = new MixCaseType[]{ALWAYS, NEVER, IF_IN_ARCHIVE};
	public static final int ALWAYS_ID = 0;
	public static final int NEVER_ID = 1;
	public static final int IF_IN_ARCHIVE_ID = 2;
	private final int type;

	private MixCaseType(int type) {
		this.type = type;
	}

	public static MixCaseType fromTypeID(int typeID) {
		if (typeID >= 0 && typeID < VALUES.length) {
			return VALUES[typeID];
		} else {
			throw new IllegalArgumentException();
		}
	}

	public int getType() {
		return this.type;
	}

	@Override
	public String toString() {
		switch (this.type) {
			case ALWAYS_ID:
				return "ALWAYS";
			case NEVER_ID:
				return "NEVER";
			case IF_IN_ARCHIVE_ID:
				return "IF_IN_ARCHIVE";
			default:
				return "ERROR";
		}
	}
}
