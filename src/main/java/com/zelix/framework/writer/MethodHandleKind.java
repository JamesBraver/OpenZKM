package com.zelix.framework.writer;

public enum MethodHandleKind {
	REF_GET_FIELD(1),
	REF_GET_STATIC(2),
	REF_PUT_FIELD(3),
	REF_PUT_STATIC(4),
	REF_INVOKE_VIRTUAL(5),
	REF_INVOKE_STATIC(6),
	REF_INVOKE_SPECIAL(7),
	REF_NEW_INVOKE_SPECIAL(8),
	REF_INVOKE_INTERFACE(9);

	public final int referenceKind;

	private MethodHandleKind(int referenceKind) {
		this.referenceKind = referenceKind;
	}


	public static boolean isValidMethodName(MethodHandleKind referenceKind, String methodName) {
		switch (referenceKind) {
			case REF_INVOKE_VIRTUAL:
			case REF_INVOKE_STATIC:
			case REF_INVOKE_SPECIAL:
			case REF_INVOKE_INTERFACE:
				return !methodName.equals("<init>") && !methodName.equals("<clinit>");
			case REF_NEW_INVOKE_SPECIAL:
				return methodName.equals("<init>");
			default:
				return false;
		}
	}

	public static MethodHandleKind fromReferenceKind(int referenceKind) {
		switch (referenceKind) {
			case 1:
				return REF_GET_FIELD;
			case 2:
				return REF_GET_STATIC;
			case 3:
				return REF_PUT_FIELD;
			case 4:
				return REF_PUT_STATIC;
			case 5:
				return REF_INVOKE_VIRTUAL;
			case 6:
				return REF_INVOKE_STATIC;
			case 7:
				return REF_INVOKE_SPECIAL;
			case 8:
				return REF_NEW_INVOKE_SPECIAL;
			case 9:
				return REF_INVOKE_INTERFACE;
			default:
				return null;
		}
	}

	public int getReferenceKind() {
		return this.referenceKind;
	}
}
