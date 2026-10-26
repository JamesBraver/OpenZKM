package com.zelix.framework;

import com.zelix.framework.constant.ConstantPoolVisitor;
import com.zelix.utils.dataset.AtomicInt;
import com.zelix.utils.dataset.ByteDataBuffer;
import java.io.IOException;

public class AccessNode extends StructureNode {
	public static final AtomicInt unusedAtomicInt1 = new AtomicInt(1);
	public static final AtomicInt unusedAtomicInt2 = new AtomicInt(2);
	public static final AtomicInt unusedAtomicInt3 = new AtomicInt(3);
	public static final AtomicInt unusedAtomicInt4 = new AtomicInt(4);
	private int accessFlags;

	public AccessNode(StructureNode parent) {
		super(parent);
		this.accessFlags = 0;
	}

	public AccessNode(StructureNode parent, ByteDataBuffer buf) throws IOException {
		super(parent);
		this.accessFlags = buf.readUnsignedShort();
	}

	public AccessNode(StructureNode parent, int accessFlags) {
		super(parent);
		this.accessFlags = accessFlags;
	}

	public static boolean isPrivate(int accessFlags) {
		return (accessFlags & 2) != 0;
	}

	public static boolean isVarArgs(int accessFlags) {
		return (accessFlags & 128) != 0;
	}

	public static int setAnnotation(int accessFlags, boolean value) {
		if (value) {
			accessFlags |= 8192;
		} else {
			accessFlags &= -8193;
		}

		return accessFlags;
	}

	public static int setDefaultAccess(int accessFlags) {
		return accessFlags & -8;
	}

	public static int markSynchronized(int accessFlags) {
		return setSynchronized(accessFlags, true);
	}

	public static int setBridge(int accessFlags, boolean value) {
		if (value) {
			accessFlags |= 64;
		} else {
			accessFlags &= -65;
		}

		return accessFlags;
	}

	public static int setSynthetic(int accessFlags, boolean value) {
		if (value) {
			accessFlags |= 4096;
		} else {
			accessFlags &= -4097;
		}

		return accessFlags;
	}

	public static int setFinal(int accessFlags, boolean value) {
		if (value) {
			accessFlags |= 16;
		} else {
			accessFlags &= -17;
		}

		return accessFlags;
	}

	public static int markStatic(int accessFlags) {
		return setStatic(accessFlags, true);
	}

	public static boolean isStatic(int accessFlags) {
		return (accessFlags & 8) != 0;
	}

	public static boolean isAnnotation(int accessFlags) {
		return (accessFlags & 8192) != 0;
	}

	public static int setPublic(int accessFlags) {
		return accessFlags & -8 | 1;
	}

	public static boolean isVolatile(int accessFlags) {
		return (accessFlags & 64) != 0;
	}

	public static int markEnum(int accessFlags) {
		return setEnum(accessFlags, true);
	}

	public static boolean isModule(int accessFlags) {
		return (accessFlags & 32768) != 0;
	}

	public static int markFinal(int accessFlags) {
		return setFinal(accessFlags, true);
	}

	public static boolean isSynchronized(int accessFlags) {
		return (accessFlags & 32) != 0;
	}

	public static int setVarargs(int accessFlags, boolean value) {
		if (value) {
			accessFlags |= 128;
		} else {
			accessFlags &= -129;
		}

		return accessFlags;
	}

	public static boolean isDefaultAccess(int accessFlags) {
		return (accessFlags & 7) == 0;
	}

	public static int markPublic(int accessFlags) {
		return accessFlags | 1;
	}

	public static int setNative(int accessFlags, boolean value) {
		if (value) {
			accessFlags |= 256;
		} else {
			accessFlags &= -257;
		}

		return accessFlags;
	}

	public static int markVolatile(int accessFlags) {
		return setVolatile(accessFlags, true);
	}

	public static int markPrivate(int accessFlags) {
		return accessFlags | 2;
	}

	public static int setEnum(int accessFlags, boolean value) {
		if (value) {
			accessFlags |= 16384;
		} else {
			accessFlags &= -16385;
		}

		return accessFlags;
	}

	public static boolean isStrict(int accessFlags) {
		return (accessFlags & 2048) != 0;
	}

	public static int setInterface(int accessFlags, boolean value) {
		if (value) {
			accessFlags |= 512;
		} else {
			accessFlags &= -513;
		}

		return accessFlags;
	}

	public static boolean isEnum(int accessFlags) {
		return (accessFlags & 16384) != 0;
	}

	public static int markBridge(int accessFlags) {
		return setBridge(accessFlags, true);
	}

	public static int setTransient(int accessFlags, boolean value) {
		if (value) {
			accessFlags |= 128;
		} else {
			accessFlags &= -129;
		}

		return accessFlags;
	}

	public static boolean isAbstract(int accessFlags) {
		return (accessFlags & 1024) != 0;
	}

	public static boolean isBridge(int accessFlags) {
		return (accessFlags & 64) != 0;
	}

	public static int markProtected(int accessFlags) {
		return accessFlags | 4;
	}

	public static int setAbstract(int accessFlags, boolean value) {
		if (value) {
			accessFlags |= 1024;
		} else {
			accessFlags &= -1025;
		}

		return accessFlags;
	}

	public static boolean isFinal(int accessFlags) {
		return (accessFlags & 16) != 0;
	}

	public static int setSynchronized(int accessFlags, boolean value) {
		if (value) {
			accessFlags |= 32;
		} else {
			accessFlags &= -33;
		}

		return accessFlags;
	}

	public static boolean isInterface(int accessFlags) {
		return (accessFlags & 512) != 0;
	}

	public static boolean isSuper(int accessFlags) {
		return (accessFlags & 32) != 0;
	}

	public static int makeProtected(int accessFlags) {
		return accessFlags & -8 | 4;
	}

	public static boolean isProtected(int accessFlags) {
		return (accessFlags & 4) != 0;
	}

	public static int markNative(int accessFlags) {
		return setNative(accessFlags, true);
	}

	public static int setStatic(int accessFlags, boolean value) {
		if (value) {
			accessFlags |= 8;
		} else {
			accessFlags &= -9;
		}

		return accessFlags;
	}

	public static int markInterface(int accessFlags) {
		return setInterface(accessFlags, true);
	}

	public static boolean isPublic(int accessFlags) {
		return (accessFlags & 1) != 0;
	}

	public static boolean isSynthetic(int accessFlags) {
		return (accessFlags & 4096) != 0;
	}

	public static int markSynthetic(int accessFlags) {
		return setSynthetic(accessFlags, true);
	}

	public static boolean isTransient(int accessFlags) {
		return (accessFlags & 128) != 0;
	}

	public static int setVolatile(int accessFlags, boolean value) {
		if (value) {
			accessFlags |= 64;
		} else {
			accessFlags &= -65;
		}

		return accessFlags;
	}

	public static boolean isNative(int accessFlags) {
		return (accessFlags & 256) != 0;
	}

	public static int markAnnotation(int accessFlags) {
		return setAnnotation(accessFlags, true);
	}

	public static String accessFlagsToString(int accessFlags, int memberType, boolean isClassMember) {
		return accessFlagsToString(accessFlags, memberType, isClassMember, false);
	}

	public static int markTransient(int accessFlags) {
		return setTransient(accessFlags, true);
	}

	public static String accessFlagsToString(int accessFlags, int memberType, boolean isClassMember, boolean addMarker) {
		StringBuilder sb = new StringBuilder();
		if (isPublic(accessFlags)) {
			if (addMarker) {
				sb.append("!");
			}

			sb.append("public");
			sb.append(" ");
		}

		if (isProtected(accessFlags)) {
			if (addMarker) {
				sb.append("!");
			}

			sb.append("protected");
			sb.append(" ");
		}

		if (isPrivate(accessFlags)) {
			if (addMarker) {
				sb.append("!");
			}

			sb.append("private");
			sb.append(" ");
		}

		if ((memberType == 1 || memberType == 3) && isAbstract(accessFlags)) {
			if (addMarker) {
				sb.append("!");
			}

			sb.append("abstract");
			sb.append(" ");
		}

		if ((memberType == 2 || memberType == 3) && isStatic(accessFlags)) {
			if (addMarker) {
				sb.append("!");
			}

			sb.append("static");
			sb.append(" ");
		}

		if (isFinal(accessFlags)) {
			if (addMarker) {
				sb.append("!");
			}

			sb.append("final");
			sb.append(" ");
		}

		if (memberType == 3) {
			if (isSynchronized(accessFlags)) {
				if (addMarker) {
					sb.append("!");
				}

				sb.append("synchronized");
				sb.append(" ");
			}
		} else if (memberType == 1 && isClassMember && isSuper(accessFlags)) {
			if (addMarker) {
				sb.append("!");
			}

			sb.append("super");
			sb.append(" ");
		}

		if (memberType == 3) {
			if (isNative(accessFlags)) {
				if (addMarker) {
					sb.append("!");
				}

				sb.append("native");
				sb.append(" ");
			}
		} else if ((memberType == 1 || memberType == 2) && isEnum(accessFlags)) {
			if (addMarker) {
				sb.append("!");
			}

			sb.append("enum");
			sb.append(" ");
		}

		if (memberType == 1 && isModule(accessFlags)) {
			if (addMarker) {
				sb.append("!");
			}

			sb.append("module");
			sb.append(" ");
		}

		if (memberType == 2) {
			if (isVolatile(accessFlags)) {
				if (addMarker) {
					sb.append("!");
				}

				sb.append("volatile");
				sb.append(" ");
			}
		} else if (memberType == 3 && isBridge(accessFlags)) {
			if (addMarker) {
				sb.append("!");
			}

			sb.append("bridge");
			sb.append(" ");
		}

		if (memberType == 2) {
			if (isTransient(accessFlags)) {
				if (addMarker) {
					sb.append("!");
				}

				sb.append("transient");
				sb.append(" ");
			}
		} else if (memberType == 3 && isClassMember && isVarArgs(accessFlags)) {
			if (addMarker) {
				sb.append("!");
			}

			sb.append("varargs");
			sb.append(" ");
		}

		if (isClassMember && isInterface(accessFlags)) {
			if (addMarker) {
				sb.append("!");
			}

			sb.append("interface");
			sb.append(" ");
		}

		if (memberType == 1) {
			if (isAnnotation(accessFlags)) {
				if (addMarker) {
					sb.append("!");
				}

				sb.append("annotation");
				sb.append(" ");
			}
		} else if (memberType == 3 && isClassMember && isStrict(accessFlags)) {
			if (addMarker) {
				sb.append("!");
			}

			sb.append("strict");
			sb.append(" ");
		}

		if (isSynthetic(accessFlags)) {
			if (addMarker) {
				sb.append("!");
			}

			sb.append("synthetic");
			sb.append(" ");
		}

		return sb.toString();
	}

	public static int markAbstract(int accessFlags) {
		return setAbstract(accessFlags, true);
	}

	public static int makePrivate(int accessFlags) {
		return accessFlags & -8 | 2;
	}

	public final void setAnnotation(boolean value) {
		this.accessFlags = setAnnotation(this.accessFlags, value);
	}

	public final void makeProtected() {
		this.accessFlags = makeProtected(this.accessFlags);
	}

	public final boolean isSynchronized() {
		return isSynchronized(this.accessFlags);
	}

	public final boolean isPublic() {
		return isPublic(this.accessFlags);
	}

	public final boolean isStatic() {
		return isStatic(this.accessFlags);
	}

	public final void setVarargs(boolean value) {
		this.accessFlags = setVarargs(this.accessFlags, value);
	}

	@Override
	public void acceptVisitor(ConstantPoolVisitor visitor) {
	}

	public final boolean isFinal() {
		return isFinal(this.accessFlags);
	}

	public final boolean isEnum() {
		return isEnum(this.accessFlags);
	}

	public final void setStatic(boolean value) {
		this.accessFlags = setStatic(this.accessFlags, value);
	}

	public final void markVarargs() {
		this.accessFlags = setVarargs(this.accessFlags, true);
	}

	public final void setSynthetic(boolean value) {
		this.accessFlags = setSynthetic(this.accessFlags, value);
	}

	public final boolean isBridge() {
		return isBridge(this.accessFlags);
	}

	public final boolean isSynthetic() {
		return isSynthetic(this.accessFlags);
	}

	public final boolean isModule() {
		return isModule(this.accessFlags);
	}

	public void setAccessFlags(int accessFlags) {
		this.accessFlags = accessFlags;
	}

	public final boolean isProtected() {
		return isProtected(this.accessFlags);
	}

	public final void makePrivate() {
		this.accessFlags = makePrivate(this.accessFlags);
	}

	public final void setFinal(boolean value) {
		this.accessFlags = setFinal(this.accessFlags, value);
	}

	public final boolean isNative() {
		return isNative(this.accessFlags);
	}

	@Override
	public Object clone() {
		return new AccessNode(this.getParent(), this.accessFlags);
	}

	public String getAccessFlagsString() {
		return this.getAccessFlagsString(false);
	}

	public int getAccessRank() {
		if (this.isPublic()) {
			return 4;
		} else if (this.isProtected()) {
			return 3;
		} else {
			return this.isPrivate() ? 1 : 2;
		}
	}

	public final boolean isAnnotation() {
		return isAnnotation(this.accessFlags);
	}

	public final void setBridge(boolean value) {
		this.accessFlags = setBridge(this.accessFlags, value);
	}

	public final String getAccessFlagsString(boolean isClassMember) {
		return accessFlagsToString(this.accessFlags, this.getMemberType(), isClassMember);
	}

	public final boolean isInterface() {
		return isInterface(this.accessFlags);
	}

	public final void setPublic() {
		this.accessFlags = setPublic(this.accessFlags);
	}

	public final boolean isPrivate() {
		return isPrivate(this.accessFlags);
	}

	public final void setEnum(boolean value) {
		this.accessFlags = setEnum(this.accessFlags, value);
	}

	public final boolean isAbstract() {
		return isAbstract(this.accessFlags);
	}

	public final boolean isVarArgs() {
		return isVarArgs(this.accessFlags);
	}

	public final void setDefaultAccess() {
		this.accessFlags = setDefaultAccess(this.accessFlags);
	}

	public final boolean isSuper() {
		return isSuper(this.accessFlags);
	}

	public final int getAccessFlags() {
		return this.accessFlags;
	}

	public final boolean isTransient() {
		return isTransient(this.accessFlags);
	}

	public final boolean isDefaultAccess() {
		return isDefaultAccess(this.accessFlags);
	}

	public final boolean isVolatile() {
		return isVolatile(this.accessFlags);
	}
}
