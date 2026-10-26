package com.zelix.framework.attributes.declaration;

import com.zelix.exceptions.ClassParseFailedException;
import com.zelix.framework.StructureNode;
import com.zelix.framework.attributes.base.AbstractAttributeNode;
import com.zelix.framework.constant.ConstantPoolVisitor;
import com.zelix.framework.listener.ReferenceUpdateListener;
import com.zelix.framework.listener.SimpleReferenceListener;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.writer.ClassInfoWriter;
import com.zelix.framework.writer.UTFStringWriter;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.tools.trim.TrimExcludes;
import com.zelix.utils.DebugUtils;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.dataset.ByteDataBuffer;
import com.zelix.workspace.ClassHierarchyNode;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class NestMembersAttribute extends AbstractAttributeNode implements SimpleReferenceListener, ReferenceUpdateListener {
	public ClassInfoWriter[] nestMembers;

	public NestMembersAttribute(
		StructureNode parent,
		int nameIndex,
		String attributeName,
		ByteDataBuffer buf,
		MultiMap<UTFStringWriter, SimpleReferenceListener> utf8References,
		MultiMap<ClassInfoWriter, ReferenceUpdateListener> classReferences
	) throws IOException {
		super(parent, nameIndex, attributeName, buf, utf8References);
		super.rawData = new byte[this.length];
		buf.read(super.rawData);
		try (ByteDataBuffer bodyBuffer = ByteDataBuffer.fromBytes(super.rawData, false)) {
			int nestMemberCount = bodyBuffer.readUnsignedShort();
			this.nestMembers = new ClassInfoWriter[nestMemberCount];

			for (int i = 0; i < nestMemberCount; i++) {
				int classIndex = bodyBuffer.readUnsignedShort();
				TypeWriter classEntry = parent.getConstantPoolEntry(classIndex);
				if (classEntry == null) {
					super.isParsedSuccessfully = false;
					throw new ClassParseFailedException(
						parent.getClassNode().getRawSourcePath()
							+ " : Illegal constant pool index in 'NestMembers' attribute : "
							+ classIndex
							+ " : '"
							+ this.getRawSourcePath()
							+ "' : File is probably corrupt (AX)"
					);
				}

				if (!(classEntry instanceof ClassInfoWriter)) {
					super.isParsedSuccessfully = false;
					throw new ClassParseFailedException(
						parent.getClassNode().getRawSourcePath()
							+ " : Invalid 'NestMembers' attribute : "
							+ classIndex
							+ " : '"
							+ this.getRawSourcePath()
							+ "' : File is probably corrupt (AY)"
					);
				}

				this.nestMembers[i] = (ClassInfoWriter)classEntry;
			}
		}

		if (!GlobalUtils.isUnique(this.nestMembers)) {
			StringBuilder errorMessageBuffer = new StringBuilder();
			errorMessageBuffer.append("Invalid 'NestMembers' attribute in class '");
			errorMessageBuffer.append(this.getRawSourcePath());
			errorMessageBuffer.append("' : Duplicate class names : ");

			for (int memberIndex = 0; memberIndex < this.nestMembers.length; memberIndex++) {
				errorMessageBuffer.append(this.nestMembers[memberIndex].getValueAsString());
				if (memberIndex < this.nestMembers.length - 1) {
					errorMessageBuffer.append(", ");
				}
			}

			DebugUtils.assertTrue(false, new String[]{errorMessageBuffer.toString()});
		}
	}

	public int trimExcludedNestMembers(TrimExcludes trimExcludes) {
		if (super.isParsedSuccessfully) {
			ArrayList<ClassInfoWriter> remainingNestMembers = new ArrayList<>(this.nestMembers.length);

			for (ClassInfoWriter nestMember : this.nestMembers) {
				ClassNode classNode = ClassHierarchyNode.getWorkspaceClassNode(nestMember.getInternalName());
				if (classNode == null || trimExcludes.isClassOrInnerClassExcluded(classNode)) {
					remainingNestMembers.add(nestMember);
				}
			}

			if (remainingNestMembers.size() < this.nestMembers.length) {
				this.nestMembers = remainingNestMembers.toArray(new ClassInfoWriter[remainingNestMembers.size()]);
			}
		}

		return this.nestMembers.length;
	}

	@Override
	public void acceptVisitor(ConstantPoolVisitor visitor) {
		this.attributeNameUtf8.accept(visitor, this, this.getParent());
		if (super.isParsedSuccessfully) {
			for (int i = 0; i < this.nestMembers.length; i++) {
				this.nestMembers[i].accept(visitor, this, this.getParent());
			}
		}
	}

	@Override
	public void updateClassReference(ClassInfoWriter oldReference, ClassInfoWriter newReference) {
		for (int i = 0; i < this.nestMembers.length; i++) {
			if (this.nestMembers[i] == oldReference) {
				this.nestMembers[i] = newReference;
			}
		}
	}

	@Override
	public void writeAttributeBody(DataOutputStream dataOutputStream) throws IOException {
		super.writeAttributeBody(dataOutputStream);
		if (super.isParsedSuccessfully) {
			dataOutputStream.writeShort(this.nestMembers.length);

			for (ClassInfoWriter nestMember : this.nestMembers) {
				dataOutputStream.writeShort(nestMember.getTag());
			}
		} else {
			dataOutputStream.write(super.rawData);
		}
	}

	@Override
	public void writeAttributeBodyRemapped(DataOutputStream dataOutputStream, Map<TypeWriter, TypeWriter> writerMap, ScriptContext scriptContext) throws IOException {
		super.writeAttributeBody(dataOutputStream);
		if (super.isParsedSuccessfully) {
			dataOutputStream.writeShort(this.nestMembers.length);

			for (ClassInfoWriter nestMember : this.nestMembers) {
				TypeWriter remappedNestMember = writerMap.get(nestMember);
				if (remappedNestMember != null) {
					dataOutputStream.writeShort(remappedNestMember.getTag());
				} else {
					dataOutputStream.writeShort(nestMember.getTag());
				}
			}
		} else {
			dataOutputStream.write(super.rawData);
		}
	}

	@Override
	public int getAttributeLength() {
		return 2 + this.nestMembers.length * 2;
	}

	@Override
	public void remapDescriptors(int keepInnerClassInfo, int keepGenericsInformation, HashMap<String, String> packageRenameMap, HashMap<String, String> classNameMap) {
	}
}
