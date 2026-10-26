package com.zelix.framework.attributes.declaration;

import com.zelix.exceptions.ClassParseFailedException;
import com.zelix.framework.AttributesNode;
import com.zelix.framework.StructureNode;
import com.zelix.framework.constant.ConstantNode;
import com.zelix.framework.constant.ConstantPoolVisitor;
import com.zelix.framework.constant.IntegerNode;
import com.zelix.framework.constant.LongNode;
import com.zelix.framework.constant.StringNode;
import com.zelix.framework.listener.IntegerConstants;
import com.zelix.framework.listener.LongConstants;
import com.zelix.framework.listener.SimpleReferenceListener;
import com.zelix.framework.listener.StringConstantUpdateListener;
import com.zelix.framework.writer.UTFStringWriter;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.utils.DebugUtils;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.dataset.ByteDataBuffer;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;

public class ConstantValueAttribute extends AttributesNode implements LongConstants, IntegerConstants, StringConstantUpdateListener {
	public TypeWriter constantValue;

	public ConstantValueAttribute(StructureNode parent, UTFStringWriter nameUtf8, ConstantNode constantValue) {
		super(parent, nameUtf8, 2);
		this.constantValue = (TypeWriter)constantValue;
	}

	public ConstantValueAttribute(
		StructureNode parent,
		int nameIndex,
		String attributeName,
		ByteDataBuffer buf,
		MultiMap<UTFStringWriter, SimpleReferenceListener> utf8References,
		MultiMap<StringNode, StringConstantUpdateListener> stringReferences,
		MultiMap<IntegerNode, IntegerConstants> integerReferences,
		MultiMap<LongNode, LongConstants> longReferences
	) throws IOException {
		super(parent, nameIndex, attributeName, buf, utf8References);
		int constantIndex = buf.readUnsignedShort();
		this.constantValue = this.getConstantPoolEntry(constantIndex);
		if (this.constantValue instanceof StringNode) {
			stringReferences.put((StringNode)this.constantValue, this);
		} else if (this.constantValue instanceof IntegerNode) {
			integerReferences.put((IntegerNode)this.constantValue, this);
		} else if (this.constantValue instanceof LongNode) {
			longReferences.put((LongNode)this.constantValue, this);
		}

		if (!(this.constantValue instanceof ConstantNode)) {
			throw new ClassParseFailedException(this.getRawSourcePath() + " : Invalid ConstantValue Attribute");
		}
	}

	@Override
	public void updateIntegerConstant(IntegerNode oldReference, IntegerNode newReference) {
		if (this.constantValue == oldReference) {
			this.constantValue = newReference;
		}
	}

	public StringNode getStringConstant() {
		return this.constantValue instanceof StringNode ? (StringNode)this.constantValue : null;
	}

	@Override
	public void updateLongConstant(LongNode oldReference, LongNode newReference) {
		if (this.constantValue == oldReference) {
			this.constantValue = newReference;
		}
	}

	public IntegerNode getIntegerConstant() {
		return this.constantValue instanceof IntegerNode ? (IntegerNode)this.constantValue : null;
	}

	@Override
	public void acceptVisitor(ConstantPoolVisitor visitor) {
		visitor.visit(this.attributeNameUtf8, this, this.getParent());
		((ConstantNode)this.constantValue).accept(visitor, this, this.getParent());
	}

	public long getLongValue() {
		if (this.constantValue instanceof LongNode) {
			return ((LongNode)this.constantValue).getValue();
		} else {
			DebugUtils.assertTrue(false, new String[]{this.getInternalName() + " " + this.constantValue.getClass().getName()});
			return 0L;
		}
	}

	@Override
	public void writeAttributeBody(DataOutputStream dataOutputStream) throws IOException {
		super.writeAttributeBody(dataOutputStream);
		dataOutputStream.writeShort(this.constantValue.getTag());
	}

	public LongNode getLongConstant() {
		return this.constantValue instanceof LongNode ? (LongNode)this.constantValue : null;
	}

	@Override
	public void writeAttributeBodyRemapped(DataOutputStream dataOutputStream, Map<TypeWriter, TypeWriter> writerMap, ScriptContext scriptContext) throws IOException {
		super.writeAttributeBodyRemapped(dataOutputStream, writerMap, scriptContext);
		TypeWriter remappedConstantValue = writerMap.get(this.constantValue);
		if (remappedConstantValue != null) {
			dataOutputStream.writeShort(remappedConstantValue.getTag());
		} else {
			dataOutputStream.writeShort(this.constantValue.getTag());
		}
	}

	@Override
	public void updateStringConstant(StringNode oldReference, StringNode newReference) {
		if (this.constantValue == oldReference) {
			this.constantValue = newReference;
		}
	}
}
