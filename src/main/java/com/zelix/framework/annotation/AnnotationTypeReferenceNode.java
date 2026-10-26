package com.zelix.framework.annotation;

import com.zelix.enums.TypeReference;
import com.zelix.framework.AbstractClassNode;
import com.zelix.framework.ClearingFlag;
import com.zelix.framework.SettingFlag;
import com.zelix.framework.StructureNode;
import com.zelix.framework.constant.ConstantPoolVisitor;
import com.zelix.framework.instructions.LabelNode;
import com.zelix.framework.listener.SimpleReferenceListener;
import com.zelix.framework.reference.IGraphElement;
import com.zelix.framework.reference.ReferenceGraph;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.wrappers.FieldNode;
import com.zelix.framework.wrappers.MethodNode;
import com.zelix.framework.writer.UTFStringWriter;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.dataset.ByteDataBuffer;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class AnnotationTypeReferenceNode extends StructureNode implements AnnotationElement, IGraphElement {
	private boolean valid = true;
	private int targetInfo;
	private int boundIndex;
	private LocalVarTargetNode[] varTarget;
	private int offset;
	private int targetPathLength;
	private TypePathEntryNode[] targetPath;
	private LabelNode offsetLabel;
	private TypeReference targetType;
	private String errorMessage;

	public AnnotationTypeReferenceNode(
		StructureNode parent, ByteDataBuffer dataBuffer, MultiMap<UTFStringWriter, SimpleReferenceListener> utf8References, MultiMap<Integer, IGraphElement> offsetReferences
	) throws IOException {
		super(parent);
		this.setTargetType(dataBuffer.readUnsignedByte());
		switch (this.targetType) {
			case CLASS_TYPE_PARAMETER:
			case METHOD_TYPE_PARAMETER:
				this.targetInfo = dataBuffer.readUnsignedByte();
				break;
			case CLASS_EXTENDS:
				this.targetInfo = dataBuffer.readUnsignedShort();
				break;
			case CLASS_TYPE_PARAMETER_BOUND:
			case METHOD_TYPE_PARAMETER_BOUND:
				this.targetInfo = dataBuffer.readUnsignedByte();
				this.boundIndex = dataBuffer.readUnsignedByte();
			case FIELD:
			case METHOD_RETURN:
			case METHOD_RECEIVER:
			default:
				break;
			case METHOD_FORMAL_PARAMETER:
				this.targetInfo = dataBuffer.readUnsignedByte();
				break;
			case THROWS:
				this.targetInfo = dataBuffer.readUnsignedShort();
				break;
			case LOCAL_VARIABLE:
			case RESOURCE_VARIABLE:
				int targetCount = dataBuffer.readUnsignedShort();
				this.varTarget = new LocalVarTargetNode[targetCount];

				for (int targetIndex = 0; targetIndex < targetCount; targetIndex++) {
					LocalVarTargetNode target = new LocalVarTargetNode(this);
					target.setStartPC(dataBuffer.readUnsignedShort());
					target.setEndPC(target.getStartPC() + dataBuffer.readUnsignedShort());
					target.setIndex(dataBuffer.readUnsignedShort());
					offsetReferences.put(target.getStartPC(), target);
					offsetReferences.put(target.getEndPC(), target);
					this.varTarget[targetIndex] = target;
				}
				break;
			case EXCEPTION_PARAMETER:
				this.targetInfo = dataBuffer.readUnsignedShort();
				break;
			case INSTANCEOF:
			case NEW:
			case CONSTRUCTOR_REFERENCE:
			case METHOD_REFERENCE:
				this.offset = dataBuffer.readUnsignedShort();
				offsetReferences.put(this.offset, this);
				break;
			case CAST:
			case CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT:
			case METHOD_INVOCATION_TYPE_ARGUMENT:
			case CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT:
			case METHOD_REFERENCE_TYPE_ARGUMENT:
				this.offset = dataBuffer.readUnsignedShort();
				offsetReferences.put(this.offset, this);
				this.targetInfo = dataBuffer.readUnsignedByte();
		}

		this.targetPathLength = dataBuffer.readUnsignedByte();
		if (this.targetPathLength > 0) {
			this.targetPath = new TypePathEntryNode[this.targetPathLength];

			for (int pathIndex = 0; pathIndex < this.targetPathLength; pathIndex++) {
				this.targetPath[pathIndex] = new TypePathEntryNode(this, dataBuffer);
				if (!this.targetPath[pathIndex].isValid()) {
					this.valid = false;
				}
			}
		}
	}

	private static boolean hasTypeEntry(TypePathEntryNode[] targetPath) {
		if (targetPath != null && targetPath.length != 0) {
			TypePathEntryNode[] entries = targetPath;
			int pathLength = targetPath.length;

			for (int i = 0; i < pathLength; i++) {
				if (entries[i].isType()) {
					return true;
				}
			}

			return false;
		} else {
			return false;
		}
	}

	@Override
	public SettingFlag getSettingFlag() {
		return SettingFlag.SET_USAGE_ANNOTATION;
	}

	@Override
	public void setLabel(Integer pos, LabelNode lbl) {
		switch (this.targetType) {
			case INSTANCEOF:
			case NEW:
			case CONSTRUCTOR_REFERENCE:
			case METHOD_REFERENCE:
			case CAST:
			case CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT:
			case METHOD_INVOCATION_TYPE_ARGUMENT:
			case CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT:
			case METHOD_REFERENCE_TYPE_ARGUMENT:
				this.offsetLabel = lbl;
			case CLASS_TYPE_PARAMETER:
			case METHOD_TYPE_PARAMETER:
			case CLASS_EXTENDS:
			case CLASS_TYPE_PARAMETER_BOUND:
			case METHOD_TYPE_PARAMETER_BOUND:
			case FIELD:
			case METHOD_RETURN:
			case METHOD_RECEIVER:
			case METHOD_FORMAL_PARAMETER:
			case THROWS:
			case LOCAL_VARIABLE:
			case RESOURCE_VARIABLE:
			case EXCEPTION_PARAMETER:
		}
	}

	@Override
	public boolean isValid() {
		return this.valid;
	}

	@Override
	public void syncName() {
	}

	@Override
	public void acceptVisitor(ConstantPoolVisitor visitor) {
	}

	@Override
	public String getMnemonics() {
		return this.getAttributesNode().getAttributeName();
	}

	@Override
	public void write(DataOutputStream dataOutputStream) throws IOException {
		dataOutputStream.writeByte(this.targetType.getTypeRef());
		switch (this.targetType) {
			case CLASS_TYPE_PARAMETER:
			case METHOD_TYPE_PARAMETER:
				dataOutputStream.writeByte(this.targetInfo);
				break;
			case CLASS_EXTENDS:
				dataOutputStream.writeShort(this.targetInfo);
				break;
			case CLASS_TYPE_PARAMETER_BOUND:
			case METHOD_TYPE_PARAMETER_BOUND:
				dataOutputStream.writeByte(this.targetInfo);
				dataOutputStream.writeByte(this.boundIndex);
			case FIELD:
			case METHOD_RETURN:
			case METHOD_RECEIVER:
			default:
				break;
			case METHOD_FORMAL_PARAMETER:
				dataOutputStream.writeByte(this.targetInfo);
				break;
			case THROWS:
				dataOutputStream.writeShort(this.targetInfo);
				break;
			case LOCAL_VARIABLE:
			case RESOURCE_VARIABLE:
				this.writeVarTargets(dataOutputStream);
				break;
			case EXCEPTION_PARAMETER:
				dataOutputStream.writeShort(this.targetInfo);
				break;
			case INSTANCEOF:
			case NEW:
			case CONSTRUCTOR_REFERENCE:
			case METHOD_REFERENCE:
				if (this.offsetLabel != null) {
					dataOutputStream.writeShort(this.offsetLabel.getOffset());
				} else {
					dataOutputStream.writeShort(this.offset);
				}
				break;
			case CAST:
			case CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT:
			case METHOD_INVOCATION_TYPE_ARGUMENT:
			case CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT:
			case METHOD_REFERENCE_TYPE_ARGUMENT:
				if (this.offsetLabel != null) {
					dataOutputStream.writeShort(this.offsetLabel.getOffset());
				} else {
					dataOutputStream.writeShort(this.offset);
				}

				dataOutputStream.writeByte(this.targetInfo);
		}

		dataOutputStream.writeByte(this.targetPathLength);
		if (this.targetPathLength > 0) {
			for (int i = 0; i < this.targetPathLength; i++) {
				this.targetPath[i].write(dataOutputStream);
			}
		}
	}

	@Override
	public void writeRemapped(DataOutputStream dataOutputStream, Map<TypeWriter, TypeWriter> remappedConstants, ScriptContext scriptContext) throws IOException {
		dataOutputStream.writeByte(this.targetType.getTypeRef());
		switch (this.targetType) {
			case CLASS_TYPE_PARAMETER:
			case METHOD_TYPE_PARAMETER:
				dataOutputStream.writeByte(this.targetInfo);
				break;
			case CLASS_EXTENDS:
				dataOutputStream.writeShort(this.targetInfo);
				break;
			case CLASS_TYPE_PARAMETER_BOUND:
			case METHOD_TYPE_PARAMETER_BOUND:
				dataOutputStream.writeByte(this.targetInfo);
				dataOutputStream.writeByte(this.boundIndex);
			case FIELD:
			case METHOD_RETURN:
			case METHOD_RECEIVER:
			default:
				break;
			case METHOD_FORMAL_PARAMETER:
				dataOutputStream.writeByte(this.targetInfo);
				break;
			case THROWS:
				dataOutputStream.writeShort(this.targetInfo);
				break;
			case LOCAL_VARIABLE:
			case RESOURCE_VARIABLE:
				this.writeVarTargets(dataOutputStream);
				break;
			case EXCEPTION_PARAMETER:
				dataOutputStream.writeShort(this.targetInfo);
				break;
			case INSTANCEOF:
			case NEW:
			case CONSTRUCTOR_REFERENCE:
			case METHOD_REFERENCE:
				if (this.offsetLabel != null) {
					dataOutputStream.writeShort(this.offsetLabel.getOffset());
				} else {
					dataOutputStream.writeShort(this.offset);
				}
				break;
			case CAST:
			case CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT:
			case METHOD_INVOCATION_TYPE_ARGUMENT:
			case CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT:
			case METHOD_REFERENCE_TYPE_ARGUMENT:
				if (this.offsetLabel != null) {
					dataOutputStream.writeShort(this.offsetLabel.getOffset());
				} else {
					dataOutputStream.writeShort(this.offset);
				}

				dataOutputStream.writeByte(this.targetInfo);
		}

		dataOutputStream.writeByte(this.targetPathLength);
		if (this.targetPathLength > 0) {
			for (int i = 0; i < this.targetPathLength; i++) {
				this.targetPath[i].write(dataOutputStream);
			}
		}
	}

	private boolean clearUsageAnnotationIfUnused(LabelNode label, ReferenceGraph<LabelNode, IGraphElement> referenceGraph) {
		Set<IGraphElement> references = referenceGraph.getReferences(label);
		if (references != null) {
			Iterator<IGraphElement> referenceIterator = references.iterator();

			while (referenceIterator.hasNext()) {
				if (referenceIterator.next().getSettingFlag() == SettingFlag.SET_USAGE_ANNOTATION) {
					return false;
				}
			}
		}

		label.removeFlag(ClearingFlag.CLEAR_USAGE_ANNOTATION);
		return true;
	}

	public boolean isThrowsTarget() {
		return this.targetType == TypeReference.THROWS;
	}

	public boolean removeInGraph(HashSet<LabelNode> removedLabels, ReferenceGraph<LabelNode, IGraphElement> referenceGraph) {
		boolean removed = false;
		if (this.valid) {
			switch (this.targetType) {
				case CLASS_TYPE_PARAMETER:
				case METHOD_TYPE_PARAMETER:
				case CLASS_EXTENDS:
				case CLASS_TYPE_PARAMETER_BOUND:
				case METHOD_TYPE_PARAMETER_BOUND:
				case FIELD:
				case METHOD_RETURN:
				case METHOD_RECEIVER:
				case METHOD_FORMAL_PARAMETER:
				case THROWS:
				case EXCEPTION_PARAMETER:
				default:
					break;
				case LOCAL_VARIABLE:
				case RESOURCE_VARIABLE:
					LocalVarTargetNode[] targets = this.varTarget;
					int targetCount = targets.length;

					for (int i = 0; i < targetCount; i++) {
						if (targets[i].removeInGraph(removedLabels, referenceGraph)) {
							removed = true;
						}
					}
					break;
				case INSTANCEOF:
				case NEW:
				case CONSTRUCTOR_REFERENCE:
				case METHOD_REFERENCE:
				case CAST:
				case CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT:
				case METHOD_INVOCATION_TYPE_ARGUMENT:
				case CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT:
				case METHOD_REFERENCE_TYPE_ARGUMENT:
					if (removedLabels.contains(this.offsetLabel)) {
						referenceGraph.removeReference(this.offsetLabel, this);
						this.clearUsageAnnotationIfUnused(this.offsetLabel, referenceGraph);
						removed = true;
					}
			}
		}

		return removed;
	}

	@Override
	public void remapDescriptors(HashMap<String, String> packageMappings, HashMap<String, String> classNameMappings) {
	}

	@Override
	public void collectClassReferences(Set<AbstractClassNode> classReferences) {
	}

	public void collectAllReferences(Set<ClassNode> referencedClasses, Set<ClassNode> signatureClasses, Set<FieldNode> referencedFields, Set<MethodNode> referencedMethods) {
	}

	public String getErrorMessage() {
		return this.errorMessage;
	}

	private void setTargetType(int typeRefTag) {
		switch (typeRefTag) {
			case 0:
				this.targetType = TypeReference.CLASS_TYPE_PARAMETER;
				break;
			case 1:
				this.targetType = TypeReference.METHOD_TYPE_PARAMETER;
			case 2:
			case 3:
			case 4:
			case 5:
			case 6:
			case 7:
			case 8:
			case 9:
			case 10:
			case 11:
			case 12:
			case 13:
			case 14:
			case 15:
			case 24:
			case 25:
			case 26:
			case 27:
			case 28:
			case 29:
			case 30:
			case 31:
			case 32:
			case 33:
			case 34:
			case 35:
			case 36:
			case 37:
			case 38:
			case 39:
			case 40:
			case 41:
			case 42:
			case 43:
			case 44:
			case 45:
			case 46:
			case 47:
			case 48:
			case 49:
			case 50:
			case 51:
			case 52:
			case 53:
			case 54:
			case 55:
			case 56:
			case 57:
			case 58:
			case 59:
			case 60:
			case 61:
			case 62:
			case 63:
			default:
				break;
			case 16:
				this.targetType = TypeReference.CLASS_EXTENDS;
				break;
			case 17:
				this.targetType = TypeReference.CLASS_TYPE_PARAMETER_BOUND;
				break;
			case 18:
				this.targetType = TypeReference.METHOD_TYPE_PARAMETER_BOUND;
				break;
			case 19:
				this.targetType = TypeReference.FIELD;
				break;
			case 20:
				this.targetType = TypeReference.METHOD_RETURN;
				break;
			case 21:
				this.targetType = TypeReference.METHOD_RECEIVER;
				break;
			case 22:
				this.targetType = TypeReference.METHOD_FORMAL_PARAMETER;
				break;
			case 23:
				this.targetType = TypeReference.THROWS;
				break;
			case 64:
				this.targetType = TypeReference.LOCAL_VARIABLE;
				break;
			case 65:
				this.targetType = TypeReference.RESOURCE_VARIABLE;
				break;
			case 66:
				this.targetType = TypeReference.EXCEPTION_PARAMETER;
				break;
			case 67:
				this.targetType = TypeReference.INSTANCEOF;
				break;
			case 68:
				this.targetType = TypeReference.NEW;
				break;
			case 69:
				this.targetType = TypeReference.CONSTRUCTOR_REFERENCE;
				break;
			case 70:
				this.targetType = TypeReference.METHOD_REFERENCE;
				break;
			case 71:
				this.targetType = TypeReference.CAST;
				break;
			case 72:
				this.targetType = TypeReference.CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT;
				break;
			case 73:
				this.targetType = TypeReference.METHOD_INVOCATION_TYPE_ARGUMENT;
				break;
			case 74:
				this.targetType = TypeReference.CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT;
				break;
			case 75:
				this.targetType = TypeReference.METHOD_REFERENCE_TYPE_ARGUMENT;
		}
	}

	@Override
	public void updateConstants() {
	}

	public boolean isTypeDependent() {
		switch (this.targetType) {
			case CLASS_TYPE_PARAMETER:
			case METHOD_TYPE_PARAMETER:
				return true;
			case CLASS_EXTENDS:
				return hasTypeEntry(this.targetPath);
			case CLASS_TYPE_PARAMETER_BOUND:
			case METHOD_TYPE_PARAMETER_BOUND:
				return true;
			case FIELD:
			case METHOD_RETURN:
			case METHOD_RECEIVER:
				return hasTypeEntry(this.targetPath);
			case METHOD_FORMAL_PARAMETER:
				return hasTypeEntry(this.targetPath);
			case THROWS:
				return false;
			case LOCAL_VARIABLE:
			case RESOURCE_VARIABLE:
				return hasTypeEntry(this.targetPath);
			case EXCEPTION_PARAMETER:
				return false;
			case INSTANCEOF:
			case NEW:
			case CONSTRUCTOR_REFERENCE:
			case METHOD_REFERENCE:
				return hasTypeEntry(this.targetPath);
			case CAST:
			case CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT:
			case METHOD_INVOCATION_TYPE_ARGUMENT:
			case CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT:
			case METHOD_REFERENCE_TYPE_ARGUMENT:
				return hasTypeEntry(this.targetPath);
			default:
				return false;
		}
	}

	private void writeVarTargets(DataOutputStream dataOutputStream) throws IOException {
		dataOutputStream.writeShort(this.varTarget.length);

		for (LocalVarTargetNode target : this.varTarget) {
			if (target.isResolved()) {
				dataOutputStream.writeShort(target.getStartPos().getOffset());
				dataOutputStream.writeShort(target.getLength());
			} else {
				dataOutputStream.writeShort(target.getStartPC());
				dataOutputStream.writeShort(target.getEndPC() - target.getStartPC());
			}

			dataOutputStream.writeShort(target.getIndex());
		}
	}

	@Override
	public void registerToGraph(ReferenceGraph<LabelNode, IGraphElement> graph) {
		if (this.valid) {
			switch (this.targetType) {
				case CLASS_TYPE_PARAMETER:
				case METHOD_TYPE_PARAMETER:
				case CLASS_EXTENDS:
				case CLASS_TYPE_PARAMETER_BOUND:
				case METHOD_TYPE_PARAMETER_BOUND:
				case FIELD:
				case METHOD_RETURN:
				case METHOD_RECEIVER:
				case METHOD_FORMAL_PARAMETER:
				case THROWS:
				case EXCEPTION_PARAMETER:
				default:
					break;
				case LOCAL_VARIABLE:
				case RESOURCE_VARIABLE:
					LocalVarTargetNode[] targets = this.varTarget;
					int targetCount = targets.length;

					for (int i = 0; i < targetCount; i++) {
						targets[i].registerToGraph(graph);
					}
					break;
				case INSTANCEOF:
				case NEW:
				case CONSTRUCTOR_REFERENCE:
				case METHOD_REFERENCE:
				case CAST:
				case CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT:
				case METHOD_INVOCATION_TYPE_ARGUMENT:
				case CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT:
				case METHOD_REFERENCE_TYPE_ARGUMENT:
					graph.addReference(this.offsetLabel, this);
			}
		}
	}

	@Override
	public int getSize() {
		int bytelen = 1;
		switch (this.targetType) {
			case CLASS_TYPE_PARAMETER:
			case METHOD_TYPE_PARAMETER:
				bytelen++;
				break;
			case CLASS_EXTENDS:
				bytelen += 2;
				break;
			case CLASS_TYPE_PARAMETER_BOUND:
			case METHOD_TYPE_PARAMETER_BOUND:
				bytelen++;
				bytelen++;
			case FIELD:
			case METHOD_RETURN:
			case METHOD_RECEIVER:
			default:
				break;
			case METHOD_FORMAL_PARAMETER:
				bytelen++;
				break;
			case THROWS:
				bytelen += 2;
				break;
			case LOCAL_VARIABLE:
			case RESOURCE_VARIABLE:
				bytelen += 2;
				bytelen += this.varTarget.length * 6;
				break;
			case EXCEPTION_PARAMETER:
				bytelen += 2;
				break;
			case INSTANCEOF:
			case NEW:
			case CONSTRUCTOR_REFERENCE:
			case METHOD_REFERENCE:
				bytelen += 2;
				break;
			case CAST:
			case CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT:
			case METHOD_INVOCATION_TYPE_ARGUMENT:
			case CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT:
			case METHOD_REFERENCE_TYPE_ARGUMENT:
				bytelen += 3;
		}

		bytelen++;
		if (this.targetPathLength > 0) {
			for (int i = 0; i < this.targetPathLength; i++) {
				bytelen += this.targetPath[i].getSize();
			}
		}

		return bytelen;
	}

	public static boolean clearUsageAnnotation(AnnotationTypeReferenceNode typeReferenceNode, LabelNode labelNode, ReferenceGraph<LabelNode, IGraphElement> referenceGraph) {
		return typeReferenceNode.clearUsageAnnotationIfUnused(labelNode, referenceGraph);
	}
}
