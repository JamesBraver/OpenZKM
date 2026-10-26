package com.zelix.framework.attributes.debug;

import com.zelix.framework.AbstractClassNode;
import com.zelix.framework.SettingFlag;
import com.zelix.framework.StructureNode;
import com.zelix.framework.constant.ConstantPoolVisitor;
import com.zelix.framework.instructions.LabelNode;
import com.zelix.framework.listener.SimpleReferenceListener;
import com.zelix.framework.reference.IGraphElement;
import com.zelix.framework.reference.ReferenceGraph;
import com.zelix.framework.variable.LocalVariableOperand;
import com.zelix.framework.variable.LocalVariableTable;
import com.zelix.framework.writer.UTFStringWriter;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.klassmaster.config.Options;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.dataset.AtomicInt;
import com.zelix.utils.dataset.ByteDataBuffer;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class LocalVariableEntry extends StructureNode implements SimpleReferenceListener, IGraphElement, Comparable<LocalVariableEntry> {
	private boolean isValid = true;
	private int startPc;
	private int endPc;
	public final LocalVariableOperand variable;
	private LabelNode endLabel;
	private LabelNode startLabel;
	private UTFStringWriter nameUtf8;
	private byte[] rawBytes;
	private UTFStringWriter descriptorUtf8;

	public LocalVariableEntry(
		StructureNode parent,
		ByteDataBuffer buf,
		LocalVariableTable localVariableTable,
		MultiMap<UTFStringWriter, SimpleReferenceListener> utf8References,
		MultiMap<Integer, IGraphElement> elementsByOffset
	) throws IOException {
		super(parent);
		this.startPc = buf.readUnsignedShort();
		int scopeLength = buf.readUnsignedShort();
		this.endPc = this.startPc + scopeLength;
		elementsByOffset.put(INTEGER_CACHE.valueOf(this.startPc), this);
		elementsByOffset.put(INTEGER_CACHE.valueOf(this.endPc), this);
		int nameIndex = buf.readUnsignedShort();
		int descriptorIndex = buf.readUnsignedShort();
		int slotIndex = buf.readUnsignedShort();
		this.variable = localVariableTable.getVariable(slotIndex);
		TypeWriter nameWriter = this.getConstantPoolEntry(nameIndex);
		TypeWriter descriptorWriter = this.getConstantPoolEntry(descriptorIndex);
		if (nameWriter instanceof UTFStringWriter && descriptorWriter instanceof UTFStringWriter && this.variable != null) {
			this.setNameUtf8((UTFStringWriter)nameWriter);
			this.setDescriptorUtf8((UTFStringWriter)descriptorWriter);
			utf8References.put(this.getNameUtf8(), this);
			utf8References.put(this.getDescriptorUtf8(), this);
		} else {
			this.isValid = false;
			this.setRawBytes(nameIndex, descriptorIndex, slotIndex);
		}
	}

	public LocalVariableEntry(
		StructureNode parent, int startPc, int endPc, UTFStringWriter nameUtf8, UTFStringWriter descriptorUtf8, LocalVariableOperand variable, MultiMap<Integer, IGraphElement> elementsByOffset
	) {
		super(parent);
		this.isValid = true;
		this.startPc = startPc;
		elementsByOffset.put(INTEGER_CACHE.valueOf(startPc), this);
		this.endPc = endPc;
		elementsByOffset.put(INTEGER_CACHE.valueOf(endPc), this);
		this.variable = variable;
		this.setNameUtf8(nameUtf8);
		this.setDescriptorUtf8(descriptorUtf8);
	}

	public void writeEntry(DataOutputStream dataOutputStream) throws IOException {
		if (this.isValid) {
			dataOutputStream.writeShort(this.startLabel.getOffset());
			dataOutputStream.writeShort(this.endLabel.getOffset() - this.startLabel.getOffset());
			dataOutputStream.writeShort(this.getNameUtf8().getTag());
			dataOutputStream.writeShort(this.getDescriptorUtf8().getTag());
			dataOutputStream.writeShort(this.variable.getIndex());
		} else {
			dataOutputStream.write(this.rawBytes);
		}
	}

	public UTFStringWriter getDescriptorUtf8() {
		return this.descriptorUtf8;
	}

	@Override
	public SettingFlag getSettingFlag() {
		return SettingFlag.SET_USAGE_LOCAL_VARIABLE;
	}

	private void setRawBytes(int nameIndex, int descriptorIndex, int slotIndex) {
		this.rawBytes = new byte[10];
		this.rawBytes[0] = (byte)(this.startPc >>> 8 & 0xFF);
		this.rawBytes[1] = (byte)(this.startPc >>> 0 & 0xFF);
		this.rawBytes[2] = (byte)(this.endPc >>> 8 & 0xFF);
		this.rawBytes[3] = (byte)(this.endPc >>> 0 & 0xFF);
		this.rawBytes[4] = (byte)(nameIndex >>> 8 & 0xFF);
		this.rawBytes[5] = (byte)(nameIndex >>> 0 & 0xFF);
		this.rawBytes[6] = (byte)(descriptorIndex >>> 8 & 0xFF);
		this.rawBytes[7] = (byte)(descriptorIndex >>> 0 & 0xFF);
		this.rawBytes[8] = (byte)(slotIndex >>> 8 & 0xFF);
		this.rawBytes[9] = (byte)(slotIndex >>> 0 & 0xFF);
	}

	public void setNameUtf8(UTFStringWriter nameUtf8) {
		this.nameUtf8 = nameUtf8;
	}

	@Override
	public void setLabel(Integer pos, LabelNode lbl) {
		if (this.startLabel == null && pos == this.startPc) {
			this.startLabel = lbl;
		} else if (pos == this.endPc) {
			this.endLabel = lbl;
		}
	}

	public void setDescriptorUtf8(UTFStringWriter descriptorUtf8) {
		this.descriptorUtf8 = descriptorUtf8;
	}

	public void obfuscateName() {
		if (Options.ZKM_DEBUG_SHOW_OLD_NAMES) {
			String obfuscatedName = this.getNameUtf8().getValue() + '_' + "a";
			this.getNameUtf8().setValue(obfuscatedName);
		} else {
			this.getNameUtf8().setValue("a");
		}
	}

	@Override
	public void acceptVisitor(ConstantPoolVisitor visitor) {
		if (this.isValid) {
			this.nameUtf8.accept(visitor, this, this.getParent());
			this.descriptorUtf8.accept(visitor, this, this.getParent());
		}
	}

	@Override
	public String getMnemonics() {
		return "LocalVariableTable";
	}

	public int compareByIndex(LocalVariableEntry other) {
		if (this.variable.getIndex() < other.variable.getIndex()) {
			return -1;
		} else {
			return this.variable.getIndex() == other.variable.getIndex() ? 0 : 1;
		}
	}

	public void remapDescriptor(int keepInnerClassInfo, int keepGenericsInformation, HashMap<String, String> classNameMap) {
		AtomicInt arrayDimensions = new AtomicInt(0);
		String elementInternalName = AbstractClassNode.getArrayElementInternalName(this.getDescriptorUtf8().getValue(), arrayDimensions);
		if (elementInternalName != null) {
			String mappedInternalName = GlobalUtils.getMappedOrDefault(elementInternalName, classNameMap);
			if (!mappedInternalName.equals(elementInternalName)) {
				String remappedDescriptor = AbstractClassNode.createClassDescriptor(mappedInternalName, arrayDimensions.get());
				this.getDescriptorUtf8().setValue(remappedDescriptor);
			}
		}
	}

	public UTFStringWriter getNameUtf8() {
		return this.nameUtf8;
	}

	@Override
	public int compareTo(LocalVariableEntry o) {
		return this.compareByIndex(o);
	}

	public LabelNode getStartLabel() {
		return this.startLabel;
	}

	public int getIndex() {
		return this.variable.getIndex();
	}

	@Override
	public void updateSimpleNameReference(UTFStringWriter oldReference, UTFStringWriter newReference) {
		if (this.getNameUtf8() == oldReference) {
			this.setNameUtf8(newReference);
		} else if (this.getDescriptorUtf8() == oldReference) {
			this.setDescriptorUtf8(newReference);
		}
	}

	public String getVariableName() {
		return this.isValid ? this.getNameUtf8().getValue() : "";
	}

	public void writeRemappedEntry(DataOutputStream dataOutputStream, Map<TypeWriter, TypeWriter> remappedConstants) throws IOException {
		dataOutputStream.writeShort(this.startLabel.getOffset());
		dataOutputStream.writeShort(this.endLabel.getOffset() - this.startLabel.getOffset());
		UTFStringWriter remappedName = (UTFStringWriter)remappedConstants.get(this.getNameUtf8());
		if (remappedName != null) {
			dataOutputStream.writeShort(remappedName.getTag());
		} else {
			dataOutputStream.writeShort(this.getNameUtf8().getTag());
		}

		UTFStringWriter remappedDescriptor = (UTFStringWriter)remappedConstants.get(this.getDescriptorUtf8());
		if (remappedDescriptor != null) {
			dataOutputStream.writeShort(remappedDescriptor.getTag());
		} else {
			dataOutputStream.writeShort(this.getDescriptorUtf8().getTag());
		}

		dataOutputStream.writeShort(this.variable.getIndex());
	}

	public boolean isValid() {
		return this.isValid;
	}

	public LabelNode getEndLabel() {
		return this.endLabel;
	}

	@Override
	public void registerToGraph(ReferenceGraph<LabelNode, IGraphElement> graph) {
		graph.addReference(this.startLabel, this);
		graph.addReference(this.endLabel, this);
	}
}
