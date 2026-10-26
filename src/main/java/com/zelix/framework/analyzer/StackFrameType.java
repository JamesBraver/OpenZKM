package com.zelix.framework.analyzer;

import com.zelix.framework.instructions.NewNode;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.collections.SoftReferenceMap;
import java.util.Map;

public class StackFrameType {
	private static Map<String, StackFrameType> PRIMITIVE_TYPE_CACHE = GlobalUtils.newHashMap();
	private static SoftReferenceMap<String, StackFrameType> FLYWEIGHT_TYPE_CACHE = new SoftReferenceMap<>("TO");
	private static StackFrameType[] EMPTY_TYPE_ARRAY = new StackFrameType[0];
	public static final StackFrameType BYTE;
	public static final StackFrameType BOOLEAN;
	public static final StackFrameType CHAR;
	public static final StackFrameType SHORT;
	public static final StackFrameType INT;
	public static final StackFrameType LONG;
	public static final StackFrameType FLOAT;
	public static final StackFrameType DOUBLE;
	public static final StackFrameType UNKNOWN;
	public static final StackFrameType NULL;
	public static final StackFrameType UNINITIALIZED;
	public static final StackFrameType RETURN_ADDRESS;
	public static final StackFrameType THROWABLE;
	public static final StackFrameType OBJECT;
	public static final StackFrameType STRING;
	public static final StackFrameType CLONEABLE;
	public static final StackFrameType CLASS;
	public static final StackFrameType BYTE_ARRAY;
	public static final StackFrameType BOOLEAN_ARRAY;
	public static final StackFrameType CHAR_ARRAY;
	public static final StackFrameType SHORT_ARRAY;
	public static final StackFrameType INT_ARRAY;
	public static final StackFrameType LONG_ARRAY;
	public static final StackFrameType FLOAT_ARRAY;
	public static final StackFrameType DOUBLE_ARRAY;
	private final String descriptor;

	protected StackFrameType(String descriptor) {
		this.descriptor = descriptor;
	}

	static {
		PRIMITIVE_TYPE_CACHE.put("B", new StackFrameType("B"));
		PRIMITIVE_TYPE_CACHE.put("Z", new StackFrameType("Z"));
		PRIMITIVE_TYPE_CACHE.put("C", new StackFrameType("C"));
		PRIMITIVE_TYPE_CACHE.put("S", new StackFrameType("S"));
		PRIMITIVE_TYPE_CACHE.put("I", new StackFrameType("I"));
		PRIMITIVE_TYPE_CACHE.put("F", new StackFrameType("F"));
		PRIMITIVE_TYPE_CACHE.put("J", new StackFrameType("J"));
		PRIMITIVE_TYPE_CACHE.put("D", new StackFrameType("D"));
		PRIMITIVE_TYPE_CACHE.put("?", new StackFrameType("?"));
		PRIMITIVE_TYPE_CACHE.put("n", new StackFrameType("n"));
		PRIMITIVE_TYPE_CACHE.put("~", new StackFrameType("~"));
		PRIMITIVE_TYPE_CACHE.put("[B", new StackFrameType("[B"));
		PRIMITIVE_TYPE_CACHE.put("[Z", new StackFrameType("[Z"));
		PRIMITIVE_TYPE_CACHE.put("[C", new StackFrameType("[C"));
		PRIMITIVE_TYPE_CACHE.put("[S", new StackFrameType("[S"));
		PRIMITIVE_TYPE_CACHE.put("[I", new StackFrameType("[I"));
		PRIMITIVE_TYPE_CACHE.put("[F", new StackFrameType("[F"));
		PRIMITIVE_TYPE_CACHE.put("[J", new StackFrameType("[J"));
		PRIMITIVE_TYPE_CACHE.put("[D", new StackFrameType("[D"));
		PRIMITIVE_TYPE_CACHE.put("RETURN_ADDRESS", new StackFrameType("RETURN_ADDRESS"));
		BYTE = getResolved("B");
		BOOLEAN = getResolved("Z");
		CHAR = getResolved("C");
		SHORT = getResolved("S");
		INT = getResolved("I");
		LONG = getResolved("J");
		FLOAT = getResolved("F");
		DOUBLE = getResolved("D");
		UNKNOWN = getResolved("?");
		NULL = getResolved("n");
		UNINITIALIZED = getResolved("~");
		RETURN_ADDRESS = getResolved("RETURN_ADDRESS");
		THROWABLE = getResolved("Ljava/lang/Throwable;");
		OBJECT = getResolved("Ljava/lang/Object;");
		STRING = getResolved("Ljava/lang/String;");
		CLONEABLE = getResolved("Ljava/lang/Cloneable;");
		CLASS = getResolved("Ljava/lang/Class;");
		BYTE_ARRAY = getResolved("[B");
		BOOLEAN_ARRAY = getResolved("[Z");
		CHAR_ARRAY = getResolved("[C");
		SHORT_ARRAY = getResolved("[S");
		INT_ARRAY = getResolved("[I");
		LONG_ARRAY = getResolved("[J");
		FLOAT_ARRAY = getResolved("[F");
		DOUBLE_ARRAY = getResolved("[D");
	}

	public static boolean isDescriptorCategory2(String descriptor) {
		if (descriptor.length() == 1) {
			switch (descriptor.charAt(0)) {
				case 'B':
				case 'C':
				case 'F':
				case 'I':
				case 'S':
				case 'Z':
					return false;
				case 'D':
				case 'J':
					return true;
				case 'E':
				case 'G':
				case 'H':
				case 'K':
				case 'L':
				case 'M':
				case 'N':
				case 'O':
				case 'P':
				case 'Q':
				case 'R':
				case 'T':
				case 'U':
				case 'V':
				case 'W':
				case 'X':
				case 'Y':
				default:
					return false;
			}
		} else {
			return false;
		}
	}

	public static StackFrameType getInstance(String descriptor, boolean resolved) {
		return getInstance(descriptor, resolved, null);
	}

	public static StackFrameType[] createEmptyFrame(int length) {
		return length == 0 ? EMPTY_TYPE_ARRAY : new StackFrameType[length];
	}

	public static StackFrameType getInstance(String descriptor, boolean resolved, NewNode newNode) {
		StackFrameType frameType = null;
		if (descriptor.length() <= 2 || descriptor.equals("RETURN_ADDRESS")) {
			frameType = PRIMITIVE_TYPE_CACHE.get(descriptor);
		}

		if (frameType == null) {
			if (newNode == null) {
				if (resolved) {
					frameType = FLYWEIGHT_TYPE_CACHE.get(descriptor);
					if (frameType == null) {
						frameType = new StackFrameType(descriptor);
						FLYWEIGHT_TYPE_CACHE.put(descriptor, frameType);
					}
				} else {
					frameType = new UnresolvedType(descriptor);
				}
			} else {
				frameType = new UnresolvedNewType(descriptor, newNode);
			}
		}

		return frameType;
	}

	public static StackFrameType getResolved(String descriptor) {
		return getInstance(descriptor, true);
	}

	public boolean isResolved() {
		return true;
	}

	public String getFrameKey() {
		StringBuffer keyBuffer = new StringBuffer();
		keyBuffer.append(this.descriptor);
		if (!this.isResolved()) {
			if (this.isUninitialized()) {
				keyBuffer.append("**");
			} else {
				keyBuffer.append("*");
			}
		}

		return keyBuffer.toString();
	}

	public StackFrameType toResolved() {
		return new StackFrameType(this.descriptor);
	}

	public boolean endsWith(String suffix) {
		return this.descriptor.endsWith(suffix);
	}

	public boolean isArray() {
		return this.descriptor.charAt(0) == '[';
	}

	public int getStackSize() {
		return !this.descriptor.equals("J") && !this.descriptor.equals("D") ? 1 : 2;
	}

	public boolean isUnknown() {
		return this.descriptor.equals("?");
	}

	public boolean isComputationalInt() {
		return this == BYTE || this == BOOLEAN || this == CHAR || this == SHORT || this == INT;
	}

	public boolean isUninitialized() {
		return !this.isResolved();
	}

	public StackFrameType getElementType() {
		if (this.descriptor.charAt(0) == '[') {
			return getResolved(this.descriptor.substring(1));
		} else {
			return this.descriptor.startsWith("n") ? NULL : null;
		}
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof StackFrameType && ((StackFrameType)o).descriptor.equals(this.descriptor) && ((StackFrameType)o).isResolved() == this.isResolved();
	}

	@Override
	public int hashCode() {
		return this.descriptor.hashCode();
	}

	public boolean isNullType() {
		return this.descriptor.equals("n");
	}

	public boolean isUninitializedMarker() {
		return this == UNINITIALIZED;
	}

	public boolean isThatType(String descriptor) {
		return this.descriptor.equals(descriptor);
	}

	public String getDescriptor() {
		return this.descriptor;
	}

	public boolean isCategory2() {
		return this == LONG || this == DOUBLE;
	}

	public boolean isObjectReference() {
		return this.descriptor.startsWith("L") && this.descriptor.endsWith(";");
	}

	public boolean startsWith(String prefix) {
		return this.descriptor.startsWith(prefix);
	}
}
