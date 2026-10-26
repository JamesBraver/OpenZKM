package com.zelix.framework.attributes.declaration;

import com.zelix.framework.StructureNode;
import com.zelix.framework.attributes.UnknownAttribute;
import com.zelix.framework.attributes.base.AbstractAttributeNode;
import com.zelix.framework.constant.ConstantPoolVisitor;
import com.zelix.framework.listener.ReferenceUpdateListener;
import com.zelix.framework.listener.SimpleReferenceListener;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.wrappers.FieldNode;
import com.zelix.framework.wrappers.MethodNode;
import com.zelix.framework.writer.ClassInfoWriter;
import com.zelix.framework.writer.UTFStringWriter;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.repository.ClassProvider;
import com.zelix.tools.trim.TrimExcludes;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.collections.ThreeLevelMultiMap;
import com.zelix.utils.dataset.ByteDataBuffer;
import com.zelix.workspace.AbstractWorkspace;
import com.zelix.workspace.PhantomClassManager;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class RecordAttribute extends AbstractAttributeNode implements SimpleReferenceListener {
	private RecordComponentNode[] components;

	public RecordAttribute(
		StructureNode parent,
		int nameIndex,
		String attributeName,
		ByteDataBuffer buf,
		MultiMap<UTFStringWriter, SimpleReferenceListener> utf8References,
		MultiMap<ClassInfoWriter, ReferenceUpdateListener> classReferences,
		ThreeLevelMultiMap<String, String, UnknownAttribute, Object> unknownAttributes,
		PrintWriter printWriter
	) throws IOException {
		super(parent, nameIndex, attributeName, buf, utf8References);
		byte[] attributeBytes = new byte[this.length];
		buf.read(attributeBytes);
		try (ByteDataBuffer bodyBuffer = ByteDataBuffer.fromBytes(attributeBytes, false)) {
			int componentCount = bodyBuffer.readUnsignedShort();
			this.components = new RecordComponentNode[componentCount];

			for (int i = 0; i < componentCount; i++) {
				this.components[i] = new RecordComponentNode(this, bodyBuffer, utf8References, classReferences, unknownAttributes, printWriter);
				if (!this.components[i].isValid()) {
					super.isParsedSuccessfully = false;
					super.rawData = attributeBytes;
					printWriter.println("ERROR: " + this.getSourcePath() + " : " + this.getAttributeName() + " (A) " + this.components[i].getErrorMessage());
				}
			}
		}
	}

	public void syncAnnotationNames() {
		if (super.isParsedSuccessfully) {
			RecordComponentNode[] componentNodes = this.components;
			int componentCount = componentNodes.length;

			for (int i = 0; i < componentCount; i++) {
				componentNodes[i].syncAnnotationNames();
			}
		}
	}

	public void removeSignatureAttributes() {
		if (super.isParsedSuccessfully) {
			RecordComponentNode[] componentNodes = this.components;
			int componentCount = componentNodes.length;

			for (int i = 0; i < componentCount; i++) {
				componentNodes[i].removeSignatureAttributes();
			}
		}
	}

	@Override
	public void acceptVisitor(ConstantPoolVisitor visitor) {
		visitor.visit(this.attributeNameUtf8, this, this.getParent());
		RecordComponentNode[] componentNodes = this.components;
		int componentCount = componentNodes.length;

		for (int i = 0; i < componentCount; i++) {
			componentNodes[i].acceptVisitor(visitor);
		}
	}

	public void collectAllReferences(HashSet<ClassNode> classReferences, HashSet<ClassNode> signatureClassReferences, HashSet<FieldNode> fieldReferences, HashSet<MethodNode> methodReferences) {
		if (super.isParsedSuccessfully) {
			RecordComponentNode[] componentNodes = this.components;
			int componentCount = componentNodes.length;

			for (int i = 0; i < componentCount; i++) {
				componentNodes[i].collectAllReferences(classReferences, signatureClassReferences, fieldReferences, methodReferences);
			}
		}
	}

	public void resolveComponents(AbstractWorkspace workspace, ClassProvider classProvider, PhantomClassManager phantomClassManager, ScriptContext scriptContext) {
		if (super.isParsedSuccessfully) {
			RecordComponentNode[] componentNodes = this.components;
			int componentCount = componentNodes.length;

			for (int i = 0; i < componentCount; i++) {
				componentNodes[i].resolveComponentTargets(workspace, classProvider, phantomClassManager, scriptContext);
			}
		}
	}

	@Override
	public void writeAttributeBody(DataOutputStream dataOutputStream) throws IOException {
		super.writeAttributeBody(dataOutputStream);
		if (super.isParsedSuccessfully) {
			dataOutputStream.writeShort(this.components.length);
			RecordComponentNode[] componentNodes = this.components;
			int componentCount = componentNodes.length;

			for (int i = 0; i < componentCount; i++) {
				componentNodes[i].write(dataOutputStream);
			}
		} else {
			dataOutputStream.write(super.rawData);
		}
	}

	@Override
	public void updateSimpleNameReference(UTFStringWriter oldReference, UTFStringWriter newReference) {
		super.updateSimpleNameReference(oldReference, newReference);
		RecordComponentNode[] componentNodes = this.components;
		int componentCount = componentNodes.length;

		for (int i = 0; i < componentCount; i++) {
			componentNodes[i].updateSimpleNameReference(oldReference, newReference);
		}
	}

	public void trimExcludedAnnotations(TrimExcludes trimExcludes, ScriptContext scriptContext, PrintWriter printWriter) {
		if (super.isParsedSuccessfully) {
			RecordComponentNode[] componentNodes = this.components;
			int componentCount = componentNodes.length;

			for (int i = 0; i < componentCount; i++) {
				componentNodes[i].trimExcludedAnnotations(trimExcludes, scriptContext, printWriter);
			}
		}
	}

	public void updateConstants() {
		if (super.isParsedSuccessfully) {
			RecordComponentNode[] componentNodes = this.components;
			int componentCount = componentNodes.length;

			for (int i = 0; i < componentCount; i++) {
				componentNodes[i].syncNameAndConstants();
			}
		}
	}

	@Override
	public void writeAttributeBodyRemapped(DataOutputStream dataOutputStream, Map<TypeWriter, TypeWriter> writerMap, ScriptContext scriptContext) throws IOException {
		super.writeAttributeBody(dataOutputStream);
		if (super.isParsedSuccessfully) {
			dataOutputStream.writeShort(this.components.length);
			RecordComponentNode[] componentNodes = this.components;
			int componentCount = componentNodes.length;

			for (int i = 0; i < componentCount; i++) {
				componentNodes[i].writeRemapped(dataOutputStream, writerMap, scriptContext);
			}
		} else {
			dataOutputStream.write(super.rawData);
		}
	}

	@Override
	public void remapDescriptors(int keepInnerClassInfo, int keepGenericsInformation, HashMap<String, String> packageRenameMap, HashMap<String, String> classNameMap) {
		if (super.isParsedSuccessfully) {
			RecordComponentNode[] componentNodes = this.components;
			int componentCount = componentNodes.length;

			for (int i = 0; i < componentCount; i++) {
				componentNodes[i].remapDescriptors(keepInnerClassInfo, keepGenericsInformation, packageRenameMap, classNameMap);
			}
		}
	}
}
