package com.zelix.framework.encryption;

import com.zelix.framework.writer.FieldTypeWriter;
import com.zelix.framework.writer.InvokeDynamicCallWriter;
import com.zelix.framework.writer.base.AbstractMethodCallWriter;

public class LongEntry {
	private int localArrayIndex = -1;
	private int initialIndex = -1;
	private final LongLookup lookupType;
	private FieldTypeWriter arrayFieldWriter;
	private InvokeDynamicCallWriter indyCallWriter;
	private AbstractMethodCallWriter methodCallWriter;
	private int index;
	private long encryptionKey;

	public LongEntry(int index, AbstractMethodCallWriter methodCallWriter) {
		this.lookupType = LongLookup.LOOKUP_METHOD_AND_INDEX_DES;
		this.methodCallWriter = methodCallWriter;
		this.index = index;
		this.initialIndex = index;
	}

	public LongEntry(InvokeDynamicCallWriter indyCallWriter, int index) {
		this.lookupType = LongLookup.INDY_ENTRY_AND_INDEX;
		this.indyCallWriter = indyCallWriter;
		this.index = index;
		this.initialIndex = index;
	}

	public LongEntry(AbstractMethodCallWriter methodCallWriter, int index) {
		this.lookupType = LongLookup.LOOKUP_METHOD_AND_INDEX;
		this.methodCallWriter = methodCallWriter;
		this.index = index;
		this.initialIndex = index;
	}

	public LongEntry(FieldTypeWriter arrayFieldWriter, int index) {
		this.lookupType = LongLookup.FIELD_ARRAY_AND_INDEX;
		this.arrayFieldWriter = arrayFieldWriter;
		this.index = index;
		this.initialIndex = index;
	}

	public LongEntry(int localArrayIndex, int index) {
		this.lookupType = LongLookup.LOCAL_ARRAY_AND_INDEX;
		this.localArrayIndex = localArrayIndex;
		this.index = index;
		this.initialIndex = index;
	}

	public long getEncryptionKey() {
		return this.encryptionKey;
	}

	public InvokeDynamicCallWriter getIndyCallWriter() {
		return this.indyCallWriter;
	}

	public boolean hasArrayField() {
		return this.arrayFieldWriter != null;
	}

	public AbstractMethodCallWriter getMethodCallWriter() {
		return this.methodCallWriter;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public int getIndex() {
		return this.index;
	}

	public int getLocalArrayIndex() {
		return this.localArrayIndex;
	}

	public FieldTypeWriter getArrayFieldWriter() {
		return this.arrayFieldWriter;
	}

	public void setEncryptionKey(long encryptionKey) {
		this.encryptionKey = encryptionKey;
	}

	public boolean hasLocalArrayIndex() {
		return this.localArrayIndex > -1;
	}

	public boolean hasIndex() {
		return this.index > -1;
	}

	public LongLookup getLookupType() {
		return this.lookupType;
	}
}
