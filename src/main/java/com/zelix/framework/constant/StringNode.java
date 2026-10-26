package com.zelix.framework.constant;

import com.zelix.enums.TypeTags;
import com.zelix.framework.listener.SimpleReferenceListener;
import com.zelix.framework.writer.StringTypeWriter;
import com.zelix.framework.writer.StringableConstant;
import com.zelix.framework.writer.UTFStringWriter;
import com.zelix.framework.writer.base.AbstractConstantPoolWriter;
import com.zelix.framework.writer.base.ConstantWriter;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.collections.MultiMap;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;

public class StringNode extends ConstantWriter implements SimpleReferenceListener, StringableConstant {
	public static final TypeTags TAG = TypeTags.STRING;
	public UTFStringWriter utf8StringWriter;
	public StringNode remappedNode;
	private boolean needStringObf;

	public StringNode(int tag, AbstractConstantPoolWriter constantPool, UTFStringWriter utf8StringWriter, boolean needStringObf) {
		this(tag, constantPool, utf8StringWriter, null, null, needStringObf);
	}

	public StringNode(
		int tag, AbstractConstantPoolWriter constantPool, UTFStringWriter utf8StringWriter, StringNode remappedNode, MultiMap<UTFStringWriter, SimpleReferenceListener> utf8References, boolean needStringObf
	) {
		super(tag, constantPool);
		this.utf8StringWriter = utf8StringWriter;
		if (utf8References != null) {
			utf8References.put(utf8StringWriter, this);
		}

		this.remappedNode = remappedNode;
		this.needStringObf = needStringObf;
	}

	public StringNode(StringTypeWriter stringTypeWriter, UTFStringWriter utf8StringWriter, MultiMap<UTFStringWriter, SimpleReferenceListener> utf8References) {
		super(stringTypeWriter.tag, stringTypeWriter.constantPool);
		this.utf8StringWriter = utf8StringWriter;
		utf8References.put(utf8StringWriter, this);
		this.needStringObf = true;
	}

	@Override
	public String toDisplayString() {
		return "\"" + this.getValueAsString() + "\"";
	}

	@Override
	public String getTypeName() {
		return "string";
	}

	@Override
	public String getValueAsString() {
		return this.utf8StringWriter.getValue();
	}

	@Override
	public void writeToStream(DataOutputStream dos, Map<TypeWriter, TypeWriter> twMap) throws IOException {
		dos.writeByte(TAG.getTagID());
		UTFStringWriter mappedUtfStringWriter = (UTFStringWriter)twMap.get(this.utf8StringWriter);
		if (mappedUtfStringWriter != null) {
			dos.writeShort(mappedUtfStringWriter.getTag());
		} else {
			dos.writeShort(this.utf8StringWriter.getTag());
		}
	}

	@Override
	public String getAsString() {
		return this.utf8StringWriter.getValue();
	}

	@Override
	public boolean accept(ConstantPoolVisitor visitor, Object instance, Object parent) {
		return visitor.visit(this, instance, parent);
	}

	public void setNeedStringObf(boolean needStringObf) {
		this.needStringObf = needStringObf;
	}

	@Override
	public void updateSimpleNameReference(UTFStringWriter oldReference, UTFStringWriter newReference) {
		if (this.utf8StringWriter == oldReference) {
			this.utf8StringWriter = newReference;
		}
	}

	@Override
	public final String getDisplayValue() {
		return this.getValueAsString();
	}

	@Override
	public String toLoggableString() {
		String escapedValue = GlobalUtils.escapeString(this.getValueAsString());
		if (escapedValue.length() > 200) {
			escapedValue = escapedValue.substring(0, 50) + "...<" + (escapedValue.length() - 100) + " CHARS>..." + escapedValue.substring(escapedValue.length() - 50);
		}

		return escapedValue;
	}

	public UTFStringWriter getUtf8StringWriter() {
		return this.utf8StringWriter;
	}

	public boolean isNeedStringObf() {
		return this.needStringObf;
	}

	@Override
	public void setValueFromString(String valueAsString) {
		this.utf8StringWriter.setValue(valueAsString);
		this.notifySimpleChange();
	}

	public StringNode getRemappedNode() {
		return this.remappedNode;
	}

	@Override
	public void writeToStream(DataOutputStream dos) throws IOException {
		dos.writeByte(TAG.getTagID());
		dos.writeShort(this.utf8StringWriter.getTag());
	}

	@Override
	public TypeTags getTypeTag() {
		return TAG;
	}
}
