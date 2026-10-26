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

public class PackageNameWriter extends TypeWriter implements SimpleReferenceListener, IVisitable {
	public static final TypeTags TYPE_TAG = TypeTags.PACKAGE;
	public UTFStringWriter nameWriter;

	public PackageNameWriter(PackageConstantWriter moduleTypeWriter, UTFStringWriter nameWriter, MultiMap<UTFStringWriter, SimpleReferenceListener> utf8References) {
		super(moduleTypeWriter.tag, moduleTypeWriter.constantPool);
		this.nameWriter = nameWriter;
		utf8References.put(nameWriter, this);
	}

	@Override
	public String toDisplayString() {
		return "\"" + this.getValueAsString() + "\"";
	}

	public UTFStringWriter getNameWriter() {
		return this.nameWriter;
	}

	@Override
	public String getValueAsString() {
		return this.nameWriter.getValue();
	}

	@Override
	public void writeToStream(DataOutputStream dos, Map<TypeWriter, TypeWriter> twMap) throws IOException {
		dos.writeByte(TYPE_TAG.getTagID());
		UTFStringWriter remappedNameEntry = (UTFStringWriter)twMap.get(this.nameWriter);
		if (remappedNameEntry != null) {
			dos.writeShort(remappedNameEntry.getTag());
		} else {
			dos.writeShort(this.nameWriter.getTag());
		}
	}

	@Override
	public boolean accept(ConstantPoolVisitor visitor, Object instance, Object parent) {
		return visitor.visit(this, instance, parent);
	}

	@Override
	public void updateSimpleNameReference(UTFStringWriter oldReference, UTFStringWriter newReference) {
		if (this.nameWriter == oldReference) {
			this.nameWriter = newReference;
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

	public void remapName(Map<String, String> packageMappings) {
		String packageName = this.nameWriter.getValue();
		String mappedName = GlobalUtils.getMappedOrDefault(packageName, packageMappings);
		if (!packageName.equals(mappedName)) {
			this.nameWriter.setValue(mappedName);
		}
	}

	@Override
	public void writeToStream(DataOutputStream dos) throws IOException {
		dos.writeByte(TYPE_TAG.getTagID());
		dos.writeShort(this.nameWriter.getTag());
	}

	@Override
	public TypeTags getTypeTag() {
		return TYPE_TAG;
	}
}
