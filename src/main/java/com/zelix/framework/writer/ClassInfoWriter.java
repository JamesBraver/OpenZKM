package com.zelix.framework.writer;

import com.zelix.enums.TypeTags;
import com.zelix.framework.AbstractClassNode;
import com.zelix.framework.constant.ConstantPoolVisitor;
import com.zelix.framework.listener.SimpleReferenceListener;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.writer.base.AbstractClassInfoWriter;
import com.zelix.framework.writer.base.AbstractConstantPoolWriter;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.utils.collections.MultiMap;
import com.zelix.workspace.ClassHierarchyNode;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ClassInfoWriter extends AbstractClassInfoWriter implements SimpleReferenceListener, IVisitable {
	public static final TypeTags TYPE_TAG = TypeTags.CLASS;
	public UTFStringWriter nameWriter;

	public ClassInfoWriter(RawClassInfoWriter rawClassInfoWriter, UTFStringWriter nameWriter, MultiMap<UTFStringWriter, SimpleReferenceListener> utf8References) {
		super(rawClassInfoWriter.tag, rawClassInfoWriter.constantPool);
		this.nameWriter = nameWriter;
		utf8References.put(nameWriter, this);
	}

	public ClassInfoWriter(int index, AbstractConstantPoolWriter constantPool, UTFStringWriter nameWriter, MultiMap<UTFStringWriter, SimpleReferenceListener> utf8References) {
		super(index, constantPool);
		this.nameWriter = nameWriter;
		if (utf8References != null) {
			utf8References.put(nameWriter, this);
		}
	}

	public ClassInfoWriter(int index, AbstractConstantPoolWriter constantPool, UTFStringWriter nameWriter) {
		this(index, constantPool, nameWriter, null);
	}

	private static String formatAsDescriptor(String typeName) {
		if (typeName.endsWith(";")) {
			return typeName;
		} else {
			int arrayDepth = 0;

			while (typeName.charAt(arrayDepth) == '[') {
				arrayDepth++;
			}

			String arrayPrefix = typeName.substring(0, arrayDepth);
			if (arrayDepth == typeName.length() - 1) {
				if (arrayDepth > 0) {
					switch (typeName.charAt(arrayDepth)) {
						case 'B':
						case 'C':
						case 'D':
						case 'F':
						case 'I':
						case 'J':
						case 'S':
						case 'Z':
							return typeName;
						case 'E':
						case 'G':
						case 'H':
						case 'K':
						case 'L':
						case 'M':
						case 'N':
						case 'O':
						case 'P':
						case 'Q':
						case 'R':
						case 'T':
						case 'U':
						case 'V':
						case 'W':
						case 'X':
						case 'Y':
						default:
							return arrayPrefix + "L" + typeName.charAt(arrayDepth) + ";";
					}
				} else {
					return arrayPrefix + "L" + typeName + ";";
				}
			} else {
				return arrayPrefix + "L" + typeName.substring(arrayDepth) + ";";
			}
		}
	}

	public static ClassInfoWriter findByName(String internalName, Collection<ClassInfoWriter> classInfoWriters) {
		for (ClassInfoWriter classInfoWriter : classInfoWriters) {
			if (classInfoWriter.getInternalName().equals(internalName)) {
				return classInfoWriter;
			}
		}

		return null;
	}

	@Override
	public void setName(String newName) {
		this.nameWriter.setValue(newName);
	}

	public boolean isArray() {
		return this.getInternalName().startsWith("[");
	}

	public void remapName(HashMap<String, String> classNameMappings) {
		if (this.constantPool.getOwnerThisClassWriter() != this) {
			String internalName = this.nameWriter.getValue();
			String mappedName = classNameMappings.get(internalName);
			if (mappedName != null && !mappedName.equals(internalName)) {
				this.nameWriter.setValue(mappedName);
			} else {
				mappedName = TypeWriter.remapDescriptor(internalName, classNameMappings);
				if (!internalName.equals(mappedName)) {
					this.nameWriter.setValue(mappedName);
				}
			}
		}
	}

	@Override
	public String getValueAsString() {
		return this.getExternalName();
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

	public UTFStringWriter getNameWriter() {
		return this.nameWriter;
	}

	public boolean hasInternalName(String internalName) {
		return this.getInternalName().equals(internalName);
	}

	@Override
	public boolean accept(ConstantPoolVisitor visitor, Object instance, Object parent) {
		return visitor.visit(this, instance, parent);
	}

	public ClassNode getWorkspaceClassNode() {
		String typeName = AbstractClassNode.getTypeName(this.getInternalName());
		return typeName != null ? ClassHierarchyNode.getWorkspaceClassNode(typeName) : null;
	}

	@Override
	public void updateSimpleNameReference(UTFStringWriter oldReference, UTFStringWriter newReference) {
		if (this.nameWriter == oldReference) {
			this.nameWriter = newReference;
		}
	}

	public String getDescriptor() {
		return formatAsDescriptor(this.nameWriter.getValue());
	}

	public AbstractClassNode getClassNode() {
		String typeName = AbstractClassNode.getTypeName(this.getInternalName());
		return typeName != null ? ClassHierarchyNode.getClassNodeByInternalName(typeName) : null;
	}

	@Override
	public void writeToStream(DataOutputStream dos) throws IOException {
		dos.writeByte(TYPE_TAG.getTagID());
		dos.writeShort(this.nameWriter.getTag());
	}

	@Override
	public String getInternalName() {
		String name = this.nameWriter.getValue();
		if (name.indexOf(46) != -1) {
			name = name.replace('.', '/');
		}

		return name;
	}

	@Override
	public String getExternalName() {
		return this.nameWriter.getValue().replace('/', '.');
	}

	@Override
	public TypeTags getTypeTag() {
		return TYPE_TAG;
	}
}
