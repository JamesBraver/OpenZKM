package com.zelix.framework.encryption;

import com.zelix.framework.instructions.LabelNode;
import com.zelix.framework.writer.FieldTypeWriter;
import com.zelix.framework.writer.InvokeDynamicCallWriter;
import com.zelix.framework.writer.MethodCallWriter;
import com.zelix.framework.writer.base.AbstractMethodCallWriter;

public class StringEntry {
	private int localArrayIndex = -1;
	private final StringLookup lookupType;
	private InvokeDynamicCallWriter indyCallWriter;
	private AbstractMethodCallWriter methodCallWriter;
	private FieldTypeWriter arrayFieldWriter;
	private int index;
	private boolean useJsrJump;
	private MethodCallWriter encoderMethod;
	private LabelNode jumpTargetLabel;
	private MethodCallWriter decoderMethod;
	private int lookupKey;
	private long encryptionKey;

	public StringEntry(FieldTypeWriter arrayFieldWriter, int index) {
		this.lookupType = StringLookup.FIELD_ARRAY_AND_INDEX;
		this.arrayFieldWriter = arrayFieldWriter;
		this.index = index;
	}

	public StringEntry(int index, AbstractMethodCallWriter methodCallWriter) {
		this.lookupType = StringLookup.LOOKUP_METHOD_AND_INDEX_DES;
		this.methodCallWriter = methodCallWriter;
		this.index = index;
	}

	public StringEntry(int localArrayIndex, int index) {
		this.lookupType = StringLookup.LOCAL_ARRAY_AND_INDEX;
		this.localArrayIndex = localArrayIndex;
		this.index = index;
	}

	public StringEntry(AbstractMethodCallWriter methodCallWriter, int index) {
		this.lookupType = StringLookup.LOOKUP_METHOD_AND_INDEX;
		this.methodCallWriter = methodCallWriter;
		this.index = index;
	}

	public StringEntry(InvokeDynamicCallWriter indyCallWriter, int index) {
		this.lookupType = StringLookup.INDY_ENTRY_AND_INDEX;
		this.indyCallWriter = indyCallWriter;
		this.index = index;
	}

	public int getLookupKey() {
		return this.lookupKey;
	}

	public void setEncryptionKey(long encryptionKey) {
		this.encryptionKey = encryptionKey;
	}

	public boolean hasIndex() {
		return this.index > -1;
	}

	public MethodCallWriter getDecoderMethod() {
		return this.decoderMethod;
	}

	public boolean hasLocalArrayIndex() {
		return this.localArrayIndex > -1;
	}

	public int getIndex() {
		return this.index;
	}

	public FieldTypeWriter getArrayFieldWriter() {
		return this.arrayFieldWriter;
	}

	public void setLookupKey(int lookupKey) {
		this.lookupKey = lookupKey;
	}

	public MethodCallWriter getEncoderMethod() {
		return this.encoderMethod;
	}

	public LabelNode getJumpTargetLabel() {
		return this.jumpTargetLabel;
	}

	public InvokeDynamicCallWriter getIndyCallWriter() {
		return this.indyCallWriter;
	}

	public StringLookup getLookupType() {
		return this.lookupType;
	}

	public int getLocalArrayIndex() {
		return this.localArrayIndex;
	}

	public AbstractMethodCallWriter getMethodCallWriter() {
		return this.methodCallWriter;
	}

	public long getEncryptionKey() {
		return this.encryptionKey;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public boolean useJsrJump() {
		return this.useJsrJump;
	}

	public boolean hasArrayField() {
		return this.arrayFieldWriter != null;
	}
}
