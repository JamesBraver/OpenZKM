package com.zelix.framework.writer;

import com.zelix.enums.TypeTags;
import com.zelix.framework.constant.ConstantPoolVisitor;
import com.zelix.framework.listener.SimpleReferenceListener;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.collections.MultiMap;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;

public class ModuleNameWriter extends TypeWriter implements SimpleReferenceListener, IVisitable {
	public static final TypeTags TAG = TypeTags.MODULE;
	public UTFStringWriter utf8StringWriter;

	public ModuleNameWriter(ModuleConstantWriter moduleConstantWriter, UTFStringWriter utf8StringWriter, MultiMap<UTFStringWriter, SimpleReferenceListener> utf8References) {
		super(moduleConstantWriter.tag, moduleConstantWriter.constantPool);
		this.utf8StringWriter = utf8StringWriter;
		utf8References.put(utf8StringWriter, this);
	}

	@Override
	public String toDisplayString() {
		return "\"" + this.getValueAsString() + "\"";
	}

	@Override
	public String getValueAsString() {
		return this.utf8StringWriter.getValue();
	}

	@Override
	public void writeToStream(DataOutputStream dos, Map<TypeWriter, TypeWriter> twMap) throws IOException {
		dos.writeByte(TAG.getTagID());
		UTFStringWriter remappedNameEntry = (UTFStringWriter)twMap.get(this.utf8StringWriter);
		if (remappedNameEntry != null) {
			dos.writeShort(remappedNameEntry.getTag());
		} else {
			dos.writeShort(this.utf8StringWriter.getTag());
		}
	}

	@Override
	public boolean accept(ConstantPoolVisitor visitor, Object instance, Object parent) {
		return visitor.visit(this, instance, parent);
	}

	public UTFStringWriter getUtf8StringWriter() {
		return this.utf8StringWriter;
	}

	@Override
	public void updateSimpleNameReference(UTFStringWriter oldReference, UTFStringWriter newReference) {
		if (this.utf8StringWriter == oldReference) {
			this.utf8StringWriter = newReference;
		}
	}

	@Override
	public String toLoggableString() {
		String escapedValue = GlobalUtils.escapeString(this.getValueAsString());
		if (escapedValue.length() > 200) {
			escapedValue = escapedValue.substring(0, 50) + "...<" + (escapedValue.length() - 100) + " CHARS>..." + escapedValue.substring(escapedValue.length() - 50);
		}

		return escapedValue;
	}

	public String getModuleName() {
		return this.utf8StringWriter.getValue();
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
