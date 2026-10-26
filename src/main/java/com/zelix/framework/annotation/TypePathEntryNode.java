package com.zelix.framework.annotation;

import com.zelix.framework.StructureNode;
import com.zelix.framework.constant.ConstantPoolVisitor;
import com.zelix.utils.dataset.ByteDataBuffer;
import java.io.DataOutputStream;
import java.io.IOException;

public class TypePathEntryNode extends StructureNode {
	private boolean valid = true;
	private TypePathKind kind;
	private int index;

	public TypePathEntryNode(StructureNode parent, ByteDataBuffer dataBuffer) throws IOException {
		super(parent);
		switch (dataBuffer.readUnsignedByte()) {
			case 0:
				this.kind = TypePathKind.ARRAY;
				break;
			case 1:
				this.kind = TypePathKind.NESTED;
				break;
			case 2:
				this.kind = TypePathKind.WILDCARD;
				break;
			case 3:
				this.kind = TypePathKind.TYPE;
				break;
			default:
				this.valid = false;
		}

		this.index = dataBuffer.readUnsignedByte();
		this.valid = this.kind == TypePathKind.TYPE || this.index == 0;
	}

	@Override
	public void acceptVisitor(ConstantPoolVisitor visitor) {
	}

	public int getSize() {
		return 2;
	}

	public boolean isValid() {
		return this.valid;
	}

	public void write(DataOutputStream dataOutputStream) throws IOException {
		dataOutputStream.writeByte(this.kind.getIndex());
		dataOutputStream.writeByte(this.index);
	}

	public boolean isType() {
		switch (this.kind) {
			case ARRAY:
			case NESTED:
				return false;
			case WILDCARD:
			case TYPE:
				return true;
			default:
				return false;
		}
	}
}
