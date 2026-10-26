package com.zelix.framework;

import com.zelix.framework.attributes.declaration.ConstantValueAttribute;
import com.zelix.framework.listener.SimpleReferenceListener;
import com.zelix.framework.signature.FieldID;
import com.zelix.framework.signature.FieldSignature;
import com.zelix.framework.writer.UTFStringWriter;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.klassmaster.config.Options;
import com.zelix.klassmaster.config.TrimConfig;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.tools.trim.TrimExcludes;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.dataset.ByteDataBuffer;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;

public abstract class AbstractFieldNode extends MemberNode {
	public AbstractFieldNode(StructureNode parent, ByteDataBuffer buf, MultiMap<UTFStringWriter, SimpleReferenceListener> utf8References) throws IOException {
		super(parent, buf, utf8References);
	}

	public AbstractFieldNode(AbstractClassNode owner, UTFStringWriter nameWriter, UTFStringWriter descriptorWriter, AttributesNode[] attributes, int generationTypeFlag) {
		super(owner, nameWriter, descriptorWriter, attributes, generationTypeFlag);

		for (int i = 0; i < attributes.length; i++) {
			attributes[i].setParent(this);
		}
	}

	public AbstractFieldNode(StructureNode parent, ByteDataBuffer buf, MultiMap<UTFStringWriter, SimpleReferenceListener> utf8References, PrintWriter printWriter) throws IOException {
		super(parent, buf, utf8References, printWriter);
	}

	public String getStaticDescriptorKey() {
		return (this.isStatic() ? "static:" : ":") + this.getDescriptor();
	}

	public ConstantValueAttribute getConstantValueAttribute() {
		for (int i = 0; i < this.attributes.length; i++) {
			if (this.attributes[i] instanceof ConstantValueAttribute) {
				return (ConstantValueAttribute)this.attributes[i];
			}
		}

		return null;
	}

	public boolean isString() {
		return this.descriptorWriter.getValue().equals("Ljava/lang/String;");
	}

	public final FieldSignature getFieldSignature() {
		return new FieldSignature(this.getNameDisplay(), this.getDescriptor());
	}

	public final void setEnum(boolean isEnum) {
		this.accessFlagsNode.setEnum(isEnum);
	}

	@Override
	public final String toCode() {
		StringBuffer buffer = new StringBuffer();
		Enumeration<String> annotationTypeNameEnumeration = this.getAnnotationTypeNames();

		while (annotationTypeNameEnumeration.hasMoreElements()) {
			buffer.append("   ");
			buffer.append('@');
			buffer.append(GlobalUtils.internalToBinaryName(annotationTypeNameEnumeration.nextElement()));
			buffer.append(Options.lineSeparator);
		}

		buffer.append("   ");
		buffer.append(this.getAccessFlagsString());
		buffer.append(TypeWriter.descriptorToHumanReadable(this.getDescriptor()));
		buffer.append(" ");
		buffer.append(this.getNameDisplay());
		buffer.append(Options.lineSeparator);
		return buffer.toString();
	}

	public final FieldID toFieldId() {
		return new FieldID(this.getOwnerInternalName(), this.getNameDisplay(), this.getDescriptor());
	}

	@Override
	public boolean isField() {
		return true;
	}

	public final boolean isEnum() {
		return this.accessFlagsNode.isEnum();
	}

	@Override
	public void filterCodeAttribute(TrimExcludes trimExcludes, TrimConfig trimConfig, AttributesNode attribute, ScriptContext scriptContext, PrintWriter printWriter) {
	}

	public final FieldSignature getOriginalFieldSignature() {
		return new FieldSignature(this.getOriginalName(), this.getOriginalDescriptor());
	}

	public final String getReadableType() {
		return TypeWriter.descriptorToHumanReadable(this.getDescriptor());
	}

	@Override
	public String getSignature() {
		return TypeWriter.descriptorToHumanReadable(this.getOriginalDescriptor()) + " " + this.getOriginalName();
	}

	@Override
	public String getDisplayName() {
		return TypeWriter.descriptorToHumanReadable(this.getDescriptor()) + " " + this.getNameDisplay();
	}
}
