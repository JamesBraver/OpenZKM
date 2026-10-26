package com.zelix.framework.encryption;

import com.zelix.framework.writer.FieldTypeWriter;
import com.zelix.framework.writer.InvokeDynamicCallWriter;
import com.zelix.framework.writer.base.AbstractMethodCallWriter;

public class IntegerEntry {
	private int localArrayIndex = -1;
	private int initialIndex = -1;
	private final IntegerLookup lookupType;
	private InvokeDynamicCallWriter indyCallWriter;
	private FieldTypeWriter arrayFieldWriter;
	private AbstractMethodCallWriter methodCallWriter;
	private int index;
	private long encryptionKey;

	public IntegerEntry(int localArrayIndex, int index) {
		this.lookupType = IntegerLookup.LOCAL_ARRAY_AND_INDEX;
		this.localArrayIndex = localArrayIndex;
		this.index = index;
		this.initialIndex = index;
	}

	public IntegerEntry(int index, AbstractMethodCallWriter methodCallWriter) {
		this.lookupType = IntegerLookup.LOOKUP_METHOD_AND_INDEX_DES;
		this.methodCallWriter = methodCallWriter;
		this.index = index;
		this.initialIndex = index;
	}

	public IntegerEntry(AbstractMethodCallWriter methodCallWriter, int index) {
		this.lookupType = IntegerLookup.LOOKUP_METHOD_AND_INDEX;
		this.methodCallWriter = methodCallWriter;
		this.index = index;
		this.initialIndex = index;
	}

	public IntegerEntry(FieldTypeWriter arrayFieldWriter, int index) {
		this.lookupType = IntegerLookup.FIELD_ARRAY_AND_INDEX;
		this.arrayFieldWriter = arrayFieldWriter;
		this.index = index;
		this.initialIndex = index;
	}

	public IntegerEntry(InvokeDynamicCallWriter indyCallWriter, int index) {
		this.lookupType = IntegerLookup.INDY_ENTRY_AND_INDEX;
		this.indyCallWriter = indyCallWriter;
		this.index = index;
		this.initialIndex = index;
	}

	public long getEncryptionKey() {
		return this.encryptionKey;
	}

	public int getLocalArrayIndex() {
		return this.localArrayIndex;
	}

	public void setEncryptionKey(long encryptionKey) {
		this.encryptionKey = encryptionKey;
	}

	public IntegerLookup getLookupType() {
		return this.lookupType;
	}

	public int getIndex() {
		return this.index;
	}

	public boolean hasLocalArrayIndex() {
		return this.localArrayIndex > -1;
	}

	public boolean hasIndex() {
		return this.index > -1;
	}

	public FieldTypeWriter getArrayFieldWriter() {
		return this.arrayFieldWriter;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public AbstractMethodCallWriter getMethodCallWriter() {
		return this.methodCallWriter;
	}

	public boolean hasArrayField() {
		return this.arrayFieldWriter != null;
	}

	public InvokeDynamicCallWriter getIndyCallWriter() {
		return this.indyCallWriter;
	}
}
