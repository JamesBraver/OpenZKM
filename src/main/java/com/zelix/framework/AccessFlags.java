package com.zelix.framework;

public class AccessFlags {
	private int memberType;
	private boolean isPackagePrivate;
	private boolean isExcludedPackagePrivate;
	private int positiveFlags;
	private int negativeFlags;

	public AccessFlags(int memberType) {
		this.memberType = memberType;
	}

	private AccessFlags(AccessFlags other) {
		this.memberType = other.memberType;
		this.isPackagePrivate = other.isPackagePrivate;
		this.isExcludedPackagePrivate = other.isExcludedPackagePrivate;
		this.positiveFlags = other.positiveFlags;
		this.negativeFlags = other.negativeFlags;
	}

	public final void markNegativeFinal() {
		this.negativeFlags = AccessNode.markFinal(this.negativeFlags);
	}

	public final boolean isNegativeSynchronized() {
		return AccessNode.isSynchronized(this.negativeFlags);
	}

	public final boolean isPositiveAbstract() {
		return AccessNode.isAbstract(this.positiveFlags);
	}

	public final void markPositiveAbstract() {
		this.positiveFlags = AccessNode.markAbstract(this.positiveFlags);
	}

	public final void markPositiveFinal() {
		this.positiveFlags = AccessNode.markFinal(this.positiveFlags);
	}

	public final void markNegativeNative() {
		this.negativeFlags = AccessNode.markNative(this.negativeFlags);
	}

	public final boolean isNegativeNative() {
		return AccessNode.isNative(this.negativeFlags);
	}

	public final void markNegativeSynchronized() {
		this.negativeFlags = AccessNode.markSynchronized(this.negativeFlags);
	}

	public final void markNegativePublic() {
		this.negativeFlags = AccessNode.markPublic(this.negativeFlags);
	}

	public final void setPositiveAbstract(boolean value) {
		this.positiveFlags = AccessNode.setAbstract(this.positiveFlags, value);
	}

	public final void markPositiveNative() {
		this.positiveFlags = AccessNode.markNative(this.positiveFlags);
	}

	public final boolean isPositiveProtected() {
		return AccessNode.isProtected(this.positiveFlags);
	}

	public final void markPositivePackagePrivate() {
		this.positiveFlags = AccessNode.setDefaultAccess(this.positiveFlags);
		this.isPackagePrivate = true;
	}

	public final boolean isPositiveStatic() {
		return AccessNode.isStatic(this.positiveFlags);
	}

	public final void setNegativeTransient(boolean value) {
		this.negativeFlags = AccessNode.setTransient(this.negativeFlags, value);
	}

	public boolean hasConflictingModifiers() {
		return (this.positiveFlags & this.negativeFlags) != 0 || this.isPackagePrivate && this.isExcludedPackagePrivate;
	}

	public final boolean isPositiveInterface() {
		return AccessNode.isInterface(this.positiveFlags);
	}

	public final void markNegativeEnum() {
		this.negativeFlags = AccessNode.markEnum(this.negativeFlags);
	}

	public final void setNegativeAbstract(boolean value) {
		this.negativeFlags = AccessNode.setAbstract(this.negativeFlags, value);
	}

	public final void markNegativePrivate() {
		this.negativeFlags = AccessNode.markPrivate(this.negativeFlags);
	}

	public final void setNegativeSynchronized(boolean value) {
		this.negativeFlags = AccessNode.setSynchronized(this.negativeFlags, value);
	}

	public final void markNegativeVolatile() {
		this.negativeFlags = AccessNode.markVolatile(this.negativeFlags);
	}

	public final boolean isPositiveFinal() {
		return AccessNode.isFinal(this.positiveFlags);
	}

	public final void setNegativeNative(boolean value) {
		this.negativeFlags = AccessNode.setNative(this.negativeFlags, value);
	}

	public final void setPositiveVolatile(boolean value) {
		this.positiveFlags = AccessNode.setVolatile(this.positiveFlags, value);
	}

	public final boolean isNegativeStatic() {
		return AccessNode.isStatic(this.negativeFlags);
	}

	public final void setPositiveFinal(boolean value) {
		this.positiveFlags = AccessNode.setFinal(this.positiveFlags, value);
	}

	public final boolean isDefaultAccess() {
		return this.isPackagePrivate;
	}

	public final void markNegativePackagePrivate() {
		this.isExcludedPackagePrivate = true;
	}

	public final void markPositiveVolatile() {
		this.positiveFlags = AccessNode.markVolatile(this.positiveFlags);
	}

	public final boolean isPositiveVolatile() {
		return AccessNode.isVolatile(this.positiveFlags);
	}

	public final void setPositiveInterface(boolean value) {
		this.positiveFlags = AccessNode.setInterface(this.positiveFlags, value);
	}

	public final boolean isPositiveNative() {
		return AccessNode.isNative(this.positiveFlags);
	}

	public final void markNegativeProtected() {
		this.negativeFlags = AccessNode.markProtected(this.negativeFlags);
	}

	public final void setPositiveTransient(boolean value) {
		this.positiveFlags = AccessNode.setTransient(this.positiveFlags, value);
	}

	public final void markPositiveTransient() {
		this.positiveFlags = AccessNode.markTransient(this.positiveFlags);
	}

	public final boolean isNegativeInterface() {
		return AccessNode.isInterface(this.negativeFlags);
	}

	public final void setPositiveNative(boolean value) {
		this.positiveFlags = AccessNode.setNative(this.positiveFlags, value);
	}

	public boolean matchesAccessFlags(int accessFlags) {
		return (this.positiveFlags & accessFlags) == this.positiveFlags
			&& (this.negativeFlags & accessFlags) == 0
			&& (!this.isPackagePrivate || AccessNode.isDefaultAccess(accessFlags))
			&& (!this.isExcludedPackagePrivate || !AccessNode.isDefaultAccess(accessFlags));
	}

	public final void markNegativeAbstract() {
		this.negativeFlags = AccessNode.markAbstract(this.negativeFlags);
	}

	public final boolean isNegativeVolatile() {
		return AccessNode.isVolatile(this.negativeFlags);
	}

	public final void markNegativeInterface() {
		this.negativeFlags = AccessNode.markInterface(this.negativeFlags);
	}

	public final boolean isExcludedPackagePrivate() {
		return this.isExcludedPackagePrivate;
	}

	public final void setPositiveStatic(boolean value) {
		this.positiveFlags = AccessNode.setStatic(this.positiveFlags, value);
	}

	@Override
	public Object clone() {
		return new AccessFlags(this);
	}

	public final void markPositiveInterface() {
		this.positiveFlags = AccessNode.markInterface(this.positiveFlags);
	}

	public final void markPositiveEnum() {
		this.positiveFlags = AccessNode.markEnum(this.positiveFlags);
	}

	public final void markPositiveAnnotation() {
		this.positiveFlags = AccessNode.markAnnotation(this.positiveFlags);
	}

	public final void markNegativeStatic() {
		this.negativeFlags = AccessNode.markStatic(this.negativeFlags);
	}

	public final void markPositiveSynthetic() {
		this.positiveFlags = AccessNode.markSynthetic(this.positiveFlags);
	}

	public final void markPositiveBridge() {
		this.positiveFlags = AccessNode.markBridge(this.positiveFlags);
	}

	public final void setPositiveSynchronized(boolean value) {
		this.positiveFlags = AccessNode.setSynchronized(this.positiveFlags, value);
	}

	public final void markNegativeBridge() {
		this.negativeFlags = AccessNode.markBridge(this.negativeFlags);
	}

	public final boolean isPositiveTransient() {
		return AccessNode.isTransient(this.positiveFlags);
	}

	public final boolean isNegativeAbstract() {
		return AccessNode.isAbstract(this.negativeFlags);
	}

	public final void clearPositivePackagePrivate() {
		this.positiveFlags = AccessNode.setDefaultAccess(this.positiveFlags);
		this.isPackagePrivate = false;
	}

	public final void setNegativeVolatile(boolean value) {
		this.negativeFlags = AccessNode.setVolatile(this.negativeFlags, value);
	}

	public final String toAccessFlagsString() {
		String positiveFlagsString = AccessNode.accessFlagsToString(this.positiveFlags, this.memberType, true, false);
		String negativeFlagsString = AccessNode.accessFlagsToString(this.negativeFlags, this.memberType, true, true);
		if (positiveFlagsString.length() > 0 && negativeFlagsString.length() > 0) {
			return positiveFlagsString + negativeFlagsString;
		} else if (positiveFlagsString.length() > 0) {
			return positiveFlagsString;
		} else {
			return negativeFlagsString.length() > 0 ? negativeFlagsString : "";
		}
	}

	public final void markPositiveSynchronized() {
		this.positiveFlags = AccessNode.markSynchronized(this.positiveFlags);
	}

	public final void markNegativeSynthetic() {
		this.negativeFlags = AccessNode.markSynthetic(this.negativeFlags);
	}

	public final boolean isPositivePrivate() {
		return AccessNode.isPrivate(this.positiveFlags);
	}

	public final void setNegativeStatic(boolean value) {
		this.negativeFlags = AccessNode.setStatic(this.negativeFlags, value);
	}

	public final void setNegativeInterface(boolean value) {
		this.negativeFlags = AccessNode.setInterface(this.negativeFlags, value);
	}

	public final void markNegativeAnnotation() {
		this.negativeFlags = AccessNode.markAnnotation(this.negativeFlags);
	}

	public final boolean isNegativeFinal() {
		return AccessNode.isFinal(this.negativeFlags);
	}

	public final void markPositivePrivate() {
		this.positiveFlags = AccessNode.makePrivate(this.positiveFlags);
		this.isPackagePrivate = false;
	}

	public final void markPositiveStatic() {
		this.positiveFlags = AccessNode.markStatic(this.positiveFlags);
	}

	public final boolean isPositiveSynchronized() {
		return AccessNode.isSynchronized(this.positiveFlags);
	}

	public final void markPositiveProtected() {
		this.positiveFlags = AccessNode.makeProtected(this.positiveFlags);
		this.isPackagePrivate = false;
	}

	public final void markNegativeTransient() {
		this.negativeFlags = AccessNode.markTransient(this.negativeFlags);
	}

	public final boolean isNegativeTransient() {
		return AccessNode.isTransient(this.negativeFlags);
	}

	public final boolean isPositivePublic() {
		return AccessNode.isPublic(this.positiveFlags);
	}

	public final void setNegativeFinal(boolean value) {
		this.negativeFlags = AccessNode.setFinal(this.negativeFlags, value);
	}

	public final void markPositivePublic() {
		this.positiveFlags = AccessNode.setPublic(this.positiveFlags);
		this.isPackagePrivate = false;
	}
}
