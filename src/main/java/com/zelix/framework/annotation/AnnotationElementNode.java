package com.zelix.framework.annotation;

import com.zelix.framework.StructureNode;
import com.zelix.framework.listener.SimpleReferenceListener;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.wrappers.FieldNode;
import com.zelix.framework.wrappers.MethodNode;
import com.zelix.framework.writer.UTFStringWriter;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.repository.ClassProvider;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.dataset.ByteDataBuffer;
import com.zelix.workspace.PhantomClassManager;
import java.io.IOException;
import java.util.Set;

public abstract class AnnotationElementNode extends StructureNode implements SimpleReferenceListener, AnnotationElement {
	private boolean valid = true;
	private int typeTag;
	private String errorMessage;

	public AnnotationElementNode(StructureNode parent, int typeTag) {
		super(parent);
		this.typeTag = typeTag;
	}

	public static AnnotationElementNode createAnnotationElement(StructureNode parent, ByteDataBuffer dataBuffer, MultiMap<UTFStringWriter, SimpleReferenceListener> utf8References) throws IOException {
		int elementTag = dataBuffer.readUnsignedByte();
		switch (elementTag) {
			case 64:
				return new TypeAnnotationElement(parent, elementTag, dataBuffer, utf8References);
			case 65:
			case 69:
			case 71:
			case 72:
			case 75:
			case 76:
			case 77:
			case 78:
			case 79:
			case 80:
			case 81:
			case 82:
			case 84:
			case 85:
			case 86:
			case 87:
			case 88:
			case 89:
			case 92:
			case 93:
			case 94:
			case 95:
			case 96:
			case 97:
			case 98:
			case 100:
			case 102:
			case 103:
			case 104:
			case 105:
			case 106:
			case 107:
			case 108:
			case 109:
			case 110:
			case 111:
			case 112:
			case 113:
			case 114:
			default:
				return null;
			case 66:
			case 67:
			case 73:
			case 83:
			case 90:
				return new PrimitiveAnnotationElement(parent, elementTag, dataBuffer);
			case 68:
				return new DoubleAnnotationElement(parent, elementTag, dataBuffer);
			case 70:
				return new FloatAnnotationElement(parent, elementTag, dataBuffer);
			case 74:
				return new LongAnnotationElement(parent, elementTag, dataBuffer);
			case 91:
				return new ArrayAnnotationElement(parent, elementTag, dataBuffer, utf8References);
			case 99:
				return new ClassAnnotationElement(parent, elementTag, dataBuffer, utf8References);
			case 101:
				return new EnumAnnotationElement(parent, elementTag, dataBuffer, utf8References);
			case 115:
				return new StringAnnotationElement(parent, elementTag, dataBuffer, utf8References);
		}
	}

	public abstract void resolveTypeAndParams(ClassProvider classProvider, PhantomClassManager phantomClassManager, ScriptContext scriptContext);

	public final void setValid(boolean valid) {
		this.valid = valid;
	}

	public abstract boolean isArrayElement();

	@Override
	public final boolean isValid() {
		return this.valid;
	}

	public abstract boolean hasReferencedClass();

	public final void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public final int getTypeTag() {
		return this.typeTag;
	}

	public final String getErrorMessage() {
		return this.errorMessage;
	}

	public final String getElementName() {
		StructureNode ancestor = this.getParent();

		while (ancestor != null && !(ancestor instanceof AnnotationParameterNode) && !(ancestor instanceof MethodNode)) {
			ancestor = ancestor.getParent();
		}

		String elementName = null;
		if (ancestor == null) {
		}

		if (ancestor instanceof AnnotationParameterNode) {
			elementName = ((AnnotationParameterNode)ancestor).getName();
		} else if (ancestor instanceof MethodNode) {
			elementName = ((MethodNode)ancestor).getNameDisplay();
		}

		return elementName;
	}

	public abstract void collectWorkspaceRefs(Set<ClassNode> classReferences, Set<ClassNode> signatureClassReferences, Set<FieldNode> fieldReferences, Set<MethodNode> methodReferences);

	public abstract String getTypeDescriptor();

	public abstract String getReferencedClassName();

	public final String getAnnotationTypeName() {
		StructureNode ancestor = this.getParent();

		while (ancestor != null && !(ancestor instanceof AnnotationNode) && !(ancestor instanceof ClassNode)) {
			ancestor = ancestor.getParent();
		}

		String annotationTypeName = null;
		if (ancestor == null) {
		}

		if (ancestor instanceof AnnotationNode) {
			annotationTypeName = ((AnnotationNode)ancestor).getTypeInternalName();
		} else if (ancestor instanceof ClassNode) {
			annotationTypeName = ((ClassNode)ancestor).getClassName();
		}

		return annotationTypeName;
	}
}
