package com.zelix.framework.attributes.annotations;

import com.zelix.framework.AbstractClassNode;
import com.zelix.framework.StructureNode;
import com.zelix.framework.annotation.AnnotationElementNode;
import com.zelix.framework.attributes.base.AbstractOwnedAttributeNode;
import com.zelix.framework.constant.ConstantPoolVisitor;
import com.zelix.framework.listener.SimpleReferenceListener;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.wrappers.FieldNode;
import com.zelix.framework.wrappers.MethodNode;
import com.zelix.framework.writer.UTFStringWriter;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.repository.ClassProvider;
import com.zelix.tools.trim.TrimExcludes;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.dataset.ByteDataBuffer;
import com.zelix.workspace.AbstractWorkspace;
import com.zelix.workspace.PhantomClassManager;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class AnnotationDefaultAttribute extends AbstractOwnedAttributeNode {
	private AnnotationElementNode defaultValue;
	private String errorMessage;

	public AnnotationDefaultAttribute(
		StructureNode parent, int nameIndex, String attributeName, ByteDataBuffer buf, MultiMap<UTFStringWriter, SimpleReferenceListener> utf8References, PrintWriter printWriter
	) throws IOException {
		super(parent, nameIndex, attributeName, buf, utf8References);
		byte[] attributeBytes = new byte[this.length];
		buf.read(attributeBytes);
		ByteDataBuffer attributeBuffer = ByteDataBuffer.fromBytes(attributeBytes, false);

		try {
			this.defaultValue = AnnotationElementNode.createAnnotationElement(this, attributeBuffer, utf8References);
			if (!this.defaultValue.isValid()) {
				super.isParsedSuccessfully = false;
				this.errorMessage = "Invalid default value : " + this.defaultValue.getErrorMessage();
			}
		} finally {
			attributeBuffer.close();
		}
	}

	@Override
	public void resolveAnnotations(AbstractWorkspace workspace, ClassProvider classProvider, PhantomClassManager phantomClassManager, ScriptContext scriptContext) {
		if (super.isParsedSuccessfully) {
			this.defaultValue.resolveTypeAndParams(classProvider, phantomClassManager, scriptContext);
		}
	}

	@Override
	public void updateConstants() {
		if (super.isParsedSuccessfully) {
			this.defaultValue.updateConstants();
		}
	}

	@Override
	public void acceptVisitor(ConstantPoolVisitor visitor) {
		visitor.visit(this.attributeNameUtf8, this, this.getParent());
		if (super.isParsedSuccessfully) {
			this.defaultValue.acceptVisitor(visitor);
		}
	}

	@Override
	public boolean trimExcludedAnnotations(TrimExcludes trimExcludes, ScriptContext scriptContext, PrintWriter printWriter) {
		return false;
	}

	@Override
	public void syncNames() {
		if (super.isParsedSuccessfully) {
			this.defaultValue.syncName();
		}
	}

	@Override
	public void writeAttributeBody(DataOutputStream dataOutputStream) throws IOException {
		super.writeAttributeBody(dataOutputStream);
		if (super.isParsedSuccessfully) {
			this.defaultValue.write(dataOutputStream);
		} else {
			dataOutputStream.write(super.rawData);
		}
	}

	@Override
	public void collectClassReferences(Set<AbstractClassNode> classNodes) {
		if (super.isParsedSuccessfully) {
			this.defaultValue.collectClassReferences(classNodes);
		}
	}

	@Override
	public void writeAttributeBodyRemapped(DataOutputStream dataOutputStream, Map<TypeWriter, TypeWriter> writerMap, ScriptContext scriptContext) throws IOException {
		super.writeAttributeBodyRemapped(dataOutputStream, writerMap, scriptContext);
		if (super.isParsedSuccessfully) {
			this.defaultValue.writeRemapped(dataOutputStream, writerMap, scriptContext);
		} else {
			dataOutputStream.write(super.rawData);
		}
	}

	@Override
	public void remapDescriptors(int keepInnerClassInfo, int keepGenericsInformation, HashMap<String, String> packageRenameMap, HashMap<String, String> classNameMap) {
		if (super.isParsedSuccessfully) {
			this.defaultValue.remapDescriptors(packageRenameMap, classNameMap);
		}
	}

	@Override
	public void collectAllReferences(Set<ClassNode> classReferences, Set<ClassNode> signatureClassReferences, Set<FieldNode> fieldReferences, Set<MethodNode> methodReferences) {
		if (super.isParsedSuccessfully) {
			this.defaultValue.collectWorkspaceRefs(classReferences, signatureClassReferences, fieldReferences, methodReferences);
		}
	}
}
