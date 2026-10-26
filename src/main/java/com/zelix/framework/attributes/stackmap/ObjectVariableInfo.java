package com.zelix.framework.attributes.stackmap;

import com.zelix.framework.StructureNode;
import com.zelix.framework.attributes.base.AbstractStackMapAttribute;
import com.zelix.framework.constant.ConstantPoolManager;
import com.zelix.framework.constant.ConstantPoolVisitor;
import com.zelix.framework.listener.ReferenceUpdateListener;
import com.zelix.framework.writer.ClassInfoWriter;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.utils.DebugUtils;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.dataset.ByteDataBuffer;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ObjectVariableInfo extends VerificationTypeInfo implements ReferenceUpdateListener {
	private ClassInfoWriter classInfo;

	protected ObjectVariableInfo(
		StructureNode parent, int tag, int classIndex, ByteDataBuffer buf, MultiMap<ClassInfoWriter, ReferenceUpdateListener> classReferences, PrintWriter printWriter
	) {
		super(parent, tag);
		TypeWriter constantPoolEntry = this.getConstantPoolEntry(classIndex);
		if (constantPoolEntry instanceof ClassInfoWriter) {
			this.classInfo = (ClassInfoWriter)constantPoolEntry;
			//         if (F.J.D.equals("J")) {
			//            System.out.println();
			//         }
			classReferences.put(this.classInfo, this);
		} else {
			this.isValid = false;
			printWriter.println("ERROR: " + this.getRawSourcePath() + " : Invalid StackMap attribute (C)");
		}
	}

	protected ObjectVariableInfo(StructureNode parent, int tag, String descriptor, ConstantPoolManager constantPoolManager, Set<ClassInfoWriter> classInfoWriters, List<TypeWriter> utf8Strings) {
		super(parent, tag);
		String internalName;
		if (descriptor.startsWith("L") && descriptor.endsWith(";")) {
			internalName = descriptor.substring(1, descriptor.length() - 1);
		} else {
			internalName = descriptor;
		}

		ClassInfoWriter existingClassInfo = ClassInfoWriter.findByName(internalName, classInfoWriters);
		if (existingClassInfo == null) {
			this.classInfo = constantPoolManager.getOrCreateClassInfo(internalName, utf8Strings);
		} else {
			this.classInfo = existingClassInfo;
			classInfoWriters.remove(existingClassInfo);
		}
		//      if (F.J.D.equals("J")) {
		//         System.out.println();
		//      }
	}

	public static VerificationTypeInfo getOrCreateObjectType(
		AbstractStackMapAttribute stackMapAttribute,
		int tag,
		String descriptor,
		ConstantPoolManager constantPoolManager,
		Set<ClassInfoWriter> classInfoWriters,
		List<TypeWriter> utf8Strings,
		Map<String, VerificationTypeInfo> objectTypeCache
	) {
		ObjectVariableInfo objectTypeInfo;
		if (objectTypeCache.containsKey(descriptor)) {
			objectTypeInfo = (ObjectVariableInfo)objectTypeCache.get(descriptor);
		} else {
			objectTypeInfo = new ObjectVariableInfo(stackMapAttribute, tag, descriptor, constantPoolManager, classInfoWriters, utf8Strings);
			objectTypeCache.put(descriptor, objectTypeInfo);
		}

		return objectTypeInfo;
	}

	@Override
	public boolean isSameType(VerificationTypeInfo other) {
		if (this.tag == other.tag) {
			ObjectVariableInfo otherObjectInfo = (ObjectVariableInfo)other;
			return this.classInfo.getInternalName().equals(otherObjectInfo.classInfo.getInternalName());
		} else {
			return false;
		}
	}

	@Override
	public void acceptVisitor(ConstantPoolVisitor visitor) {
		this.classInfo.accept(visitor, this, this.getParent());
	}

	@Override
	public void updateClassReference(ClassInfoWriter oldReference, ClassInfoWriter newReference) {
		if (this.classInfo == oldReference) {
			this.classInfo = newReference;
		}
	}

	@Override
	protected void writeToStream(DataOutputStream dataOutputStream, Map<TypeWriter, TypeWriter> writerMap) throws IOException {
		if (!this.isValid) {
			DebugUtils.assertTrue(false, new String[]{"Invalid StackMap Entry in " + this.getBinaryName()});
		}

		dataOutputStream.writeByte(this.tag);
		if (writerMap != null) {
			TypeWriter mappedClassWriter = writerMap.get(this.classInfo);
			if (mappedClassWriter != null) {
				dataOutputStream.writeShort(mappedClassWriter.getTag());
			} else {
				dataOutputStream.writeShort(this.classInfo.getTag());
			}
		} else {
			dataOutputStream.writeShort(this.classInfo.getTag());
		}
	}
}
