package com.zelix.framework.writer;

import com.zelix.enums.TypeTags;
import com.zelix.framework.writer.base.AbstractConstantPoolWriter;
import com.zelix.framework.writer.base.TypeWriter;
import java.io.DataOutputStream;

public class NullTypeWriter extends TypeWriter {
	private int slotSize;

	private NullTypeWriter(int tag, AbstractConstantPoolWriter constantPool, int slotSize) {
		super(tag, constantPool);
		this.slotSize = slotSize;
	}

	public static NullTypeWriter getNullSlotPlaceholder(AbstractConstantPoolWriter constantPool, int slotSize) {
		if (slotSize == 1) {
			if (constantPool.nullSlotPlaceholders[0] == null) {
				constantPool.nullSlotPlaceholders[0] = new NullTypeWriter(0, constantPool, 1);
			}
		} else if (slotSize == 2 && constantPool.nullSlotPlaceholders[1] == null) {
			constantPool.nullSlotPlaceholders[1] = new NullTypeWriter(0, constantPool, 2);
		}

		return constantPool.nullSlotPlaceholders[slotSize - 1];
	}

	@Override
	public String getValueAsString() {
		return this.getClass().getName();
	}

	@Override
	public int getSlotSize() {
		return this.slotSize;
	}

	@Override
	public void writeToStream(DataOutputStream dos) {
	}

	@Override
	public TypeTags getTypeTag() {
		return TypeTags.NULL;
	}
}
