package com.zelix.framework.attributes.stackmap;

import com.zelix.framework.SettingFlag;
import com.zelix.framework.StructureNode;
import com.zelix.framework.instructions.LabelNode;
import com.zelix.framework.instructions.NewNode;
import com.zelix.framework.listener.ReferenceUpdateListener;
import com.zelix.framework.reference.IGraphElement;
import com.zelix.framework.reference.ReferenceGraph;
import com.zelix.framework.writer.ClassInfoWriter;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.utils.DebugUtils;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.dataset.ByteDataBuffer;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

public class UninitializedVariableInfo extends VerificationTypeInfo implements IGraphElement {
	private NewNode newNode;
	private LabelNode offsetLabel;

	public UninitializedVariableInfo(
		StructureNode stackMapAttribute,
		int tag,
		ByteDataBuffer byteDataBuffer,
		MultiMap<Integer, IGraphElement> elementsByOffset,
		MultiMap<ClassInfoWriter, ReferenceUpdateListener> classReferences,
		PrintWriter printWriter
	) throws IOException {
		super(stackMapAttribute, tag);
		int bytecodeOffset = byteDataBuffer.readUnsignedShort();
		elementsByOffset.put(INTEGER_CACHE.valueOf(bytecodeOffset), this);
	}

	public UninitializedVariableInfo(StructureNode stackMapAttribute, int tag, NewNode newNode) {
		super(stackMapAttribute, tag);
		this.newNode = newNode;
	}

	@Override
	public SettingFlag getSettingFlag() {
		return SettingFlag.SET_USAGE_STACK_MAP;
	}

	@Override
	public void setLabel(Integer pos, LabelNode lbl) {
		this.offsetLabel = lbl;
	}

	@Override
	public boolean isSameType(VerificationTypeInfo other) {
		if (this.tag == other.tag) {
			UninitializedVariableInfo otherVariableInfo = (UninitializedVariableInfo)other;
			return this.newNode == otherVariableInfo.newNode;
		} else {
			return false;
		}
	}

	@Override
	protected void writeToStream(DataOutputStream dataOutputStream, Map<TypeWriter, TypeWriter> writerMap) throws IOException {
		if (!this.isValid) {
			DebugUtils.assertTrue(false, new String[]{"Invalid StackMap Entry in " + this.getBinaryName()});
		}

		dataOutputStream.writeByte(this.tag);
		if (this.offsetLabel != null) {
			dataOutputStream.writeShort(this.offsetLabel.getOffset());
		} else {
			dataOutputStream.writeShort(this.newNode.getBytecodeOffset());
		}
	}

	@Override
	public void registerToGraph(ReferenceGraph<LabelNode, IGraphElement> graph) {
		if (this.offsetLabel != null) {
			graph.addReference(this.offsetLabel, this);
		}
	}
}
