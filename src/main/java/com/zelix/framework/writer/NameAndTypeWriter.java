package com.zelix.framework.writer;

import com.zelix.enums.TypeTags;
import com.zelix.framework.AbstractClassNode;
import com.zelix.framework.constant.ConstantPoolVisitor;
import com.zelix.framework.listener.SimpleReferenceListener;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.writer.base.AbstractConstantPoolWriter;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.utils.collections.MultiMap;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NameAndTypeWriter extends TypeWriter implements SimpleReferenceListener, IVisitable, StringableConstant {
	public static final TypeTags TAG = TypeTags.NAME_AND_TYPE;
	public UTFStringWriter nameUtf8Entry;
	public UTFStringWriter descriptorUtf8Entry;

	public NameAndTypeWriter(UnresolvedNameAndTypeEntry unresolvedEntry, UTFStringWriter nameUtf8Entry, UTFStringWriter descriptorUtf8Entry) {
		super(unresolvedEntry.tag, unresolvedEntry.constantPool);
		this.nameUtf8Entry = nameUtf8Entry;
		this.descriptorUtf8Entry = descriptorUtf8Entry;
	}

	public NameAndTypeWriter(
		int tag, AbstractConstantPoolWriter constantPool, UTFStringWriter nameUtf8Entry, UTFStringWriter descriptorUtf8Entry, MultiMap<UTFStringWriter, SimpleReferenceListener> utf8References
	) {
		super(tag, constantPool);
		this.nameUtf8Entry = nameUtf8Entry;
		this.descriptorUtf8Entry = descriptorUtf8Entry;
		if (utf8References != null) {
			utf8References.put(nameUtf8Entry, this);
			utf8References.put(descriptorUtf8Entry, this);
		}
	}

	@Override
	public String toDisplayString() {
		String name = this.nameUtf8Entry.getValueAsString();
		String descriptor = this.descriptorUtf8Entry.getValueAsString();
		String displayString = name;
		if (descriptor.indexOf(40) == 0) {
			displayString = name + "(";
			List<String> parameterDescriptors = TypeWriter.parseParameterDescriptors(descriptor);

			for (int parameterIndex = 0; parameterIndex < parameterDescriptors.size(); parameterIndex++) {
				displayString = displayString + TypeWriter.descriptorToHumanReadable(parameterDescriptors.get(parameterIndex)) + (parameterIndex < parameterDescriptors.size() - 1 ? ", " : "");
			}

			displayString = displayString + ")";
		}

		return displayString;
	}

	@Override
	public String getValueAsString() {
		String descriptor = this.descriptorUtf8Entry.getValueAsString();
		String name = this.nameUtf8Entry.getValueAsString();
		String displayString = TypeWriter.descriptorToHumanReadable(descriptor) + " " + name;
		if (descriptor.indexOf(40) == 0) {
			displayString = displayString + "(";
			List<String> parameterDescriptors = TypeWriter.parseParameterDescriptors(descriptor);

			for (int parameterIndex = 0; parameterIndex < parameterDescriptors.size(); parameterIndex++) {
				displayString = displayString + TypeWriter.descriptorToHumanReadable(parameterDescriptors.get(parameterIndex)) + (parameterIndex < parameterDescriptors.size() - 1 ? ", " : "");
			}

			displayString = displayString + ")";
		}

		return displayString;
	}

	@Override
	public void writeToStream(DataOutputStream dos, Map<TypeWriter, TypeWriter> twMap) throws IOException {
		dos.writeByte(TAG.getTagID());
		UTFStringWriter remappedNameEntry = (UTFStringWriter)twMap.get(this.nameUtf8Entry);
		if (remappedNameEntry != null) {
			dos.writeShort(remappedNameEntry.getTag());
		} else {
			dos.writeShort(this.nameUtf8Entry.getTag());
		}

		UTFStringWriter remappedDescriptorEntry = (UTFStringWriter)twMap.get(this.descriptorUtf8Entry);
		if (remappedDescriptorEntry != null) {
			dos.writeShort(remappedDescriptorEntry.getTag());
		} else {
			dos.writeShort(this.descriptorUtf8Entry.getTag());
		}
	}

	public List<AbstractClassNode> getReferencedClasses() {
		return TypeWriter.getReferencedClasses(this.getDescriptor());
	}

	public String getName() {
		return this.nameUtf8Entry.getValue();
	}

	@Override
	public String getAsString() {
		return this.nameUtf8Entry.getValueAsString() + '~' + this.descriptorUtf8Entry.getValueAsString();
	}

	@Override
	public boolean accept(ConstantPoolVisitor visitor, Object instance, Object parent) {
		return visitor.visit(this, instance, parent);
	}

	public UTFStringWriter getNameEntry() {
		return this.nameUtf8Entry;
	}

	@Override
	public void updateSimpleNameReference(UTFStringWriter oldReference, UTFStringWriter newReference) {
		if (this.nameUtf8Entry == oldReference) {
			this.nameUtf8Entry = newReference;
		} else if (this.descriptorUtf8Entry == oldReference) {
			this.descriptorUtf8Entry = newReference;
		}
	}

	public String getDescriptor() {
		return this.descriptorUtf8Entry.getValue();
	}

	public void remapDescriptor(HashMap<String, String> classNameMap) {
		String descriptor = this.descriptorUtf8Entry.getValue();
		String remappedDescriptor = TypeWriter.remapDescriptor(descriptor, classNameMap);
		if (remappedDescriptor != descriptor) {
			this.descriptorUtf8Entry.setValue(remappedDescriptor);
		}
	}

	public final void setName(String newName) {
		this.nameUtf8Entry.setValue(newName);
	}

	public UTFStringWriter getDescriptorEntry() {
		return this.descriptorUtf8Entry;
	}

	public List<ClassNode> getWorkspaceClassRefs() {
		return TypeWriter.getReferencedWorkspaceClasses(this.getDescriptor());
	}

	public final void setDescriptor(String newDescriptor) {
		this.descriptorUtf8Entry.setValue(newDescriptor);
	}

	@Override
	public void writeToStream(DataOutputStream dos) throws IOException {
		dos.writeByte(TAG.getTagID());
		dos.writeShort(this.nameUtf8Entry.getTag());
		dos.writeShort(this.descriptorUtf8Entry.getTag());
	}

	@Override
	public TypeTags getTypeTag() {
		return TAG;
	}
}
