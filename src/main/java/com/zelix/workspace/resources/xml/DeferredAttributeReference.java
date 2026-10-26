package com.zelix.workspace.resources.xml;

import com.zelix.utils.DebugUtils;
import com.zelix.utils.references.AtomicReference;

public class DeferredAttributeReference {
	private final String beanReference;
	private final AtomicReference<String> attributeValueRef;
	private final String referenceReason;

	public DeferredAttributeReference(String beanReference, String referenceReason) {
		this(beanReference, null, referenceReason);
	}

	public DeferredAttributeReference(String beanReference, AtomicReference<String> attributeValueRef, String referenceReason) {
		this.beanReference = beanReference;
		this.attributeValueRef = attributeValueRef;
		this.referenceReason = referenceReason;
	}

	public String getBeanReference() {
		return this.beanReference;
	}

	public AtomicReference<String> getAttributeValueRef() {
		return this.attributeValueRef;
	}

	public String getReferenceReason() {
		return this.referenceReason;
	}

	@Override
	public String toString() {
		return "<" + DebugUtils.getSimpleName(this) + " " + this.beanReference + ", " + this.attributeValueRef + ", " + this.referenceReason + ", >";
	}
}
