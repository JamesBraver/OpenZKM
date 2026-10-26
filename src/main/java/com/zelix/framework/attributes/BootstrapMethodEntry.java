package com.zelix.framework.attributes;

import com.zelix.enums.TypeTags;
import com.zelix.framework.AbstractClassNode;
import com.zelix.framework.AbstractFieldNode;
import com.zelix.framework.AbstractMethodNode;
import com.zelix.framework.MemberNode;
import com.zelix.framework.StructureNode;
import com.zelix.framework.constant.ConstantNode;
import com.zelix.framework.constant.ConstantPoolVisitor;
import com.zelix.framework.constant.IntegerNode;
import com.zelix.framework.constant.LongNode;
import com.zelix.framework.constant.StringNode;
import com.zelix.framework.listener.IntegerConstants;
import com.zelix.framework.listener.LongConstants;
import com.zelix.framework.listener.ReferenceUpdateListener;
import com.zelix.framework.listener.StringConstantUpdateListener;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.wrappers.FieldNode;
import com.zelix.framework.wrappers.MethodNode;
import com.zelix.framework.writer.ClassInfoWriter;
import com.zelix.framework.writer.DynamicConstantWriter;
import com.zelix.framework.writer.MethodDescriptorWriter;
import com.zelix.framework.writer.ResolvedMethodHandleWriter;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.dataset.ByteDataBuffer;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

public class BootstrapMethodEntry extends StructureNode implements LongConstants, IntegerConstants, StringConstantUpdateListener, ReferenceUpdateListener {
	public boolean isValid = true;
	public ResolvedMethodHandleWriter bootstrapMethodHandle;
	public String errorMessage;
	public TypeWriter[] bootstrapArguments;
	private int entryIndex;

	public BootstrapMethodEntry(
		StructureNode parent,
		ByteDataBuffer buf,
		int entryIndex,
		MultiMap<StringNode, StringConstantUpdateListener> stringReferences,
		MultiMap<IntegerNode, IntegerConstants> integerReferences,
		MultiMap<LongNode, LongConstants> longReferences,
		MultiMap<ClassInfoWriter, ReferenceUpdateListener> classReferences,
		PrintWriter printWriter
	) throws IOException {
		super(parent);
		this.setEntryIndex(entryIndex);
		int methodHandleIndex = buf.readUnsignedShort();
		int argumentCount = buf.readUnsignedShort();
		TypeWriter methodHandle = this.getConstantPoolEntry(methodHandleIndex);
		if (methodHandle instanceof ResolvedMethodHandleWriter) {
			this.bootstrapMethodHandle = (ResolvedMethodHandleWriter)methodHandle;
		} else {
			this.isValid = false;
			this.errorMessage = "Invalid bootstrap method handle entry '" + TypeWriter.getConstantPoolTagName(methodHandle.getTypeTag()) + "'";
		}

		if (this.isValid) {
			this.bootstrapArguments = new TypeWriter[argumentCount];

			for (int i = 0; i < argumentCount; i++) {
				int argumentConstantIndex = buf.readUnsignedShort();
				TypeWriter argument = this.getConstantPoolEntry(argumentConstantIndex);
				if (!(argument instanceof ClassInfoWriter)
					&& !(argument instanceof ConstantNode)
					&& !(argument instanceof ResolvedMethodHandleWriter)
					&& !(argument instanceof MethodDescriptorWriter)
					&& !(argument instanceof DynamicConstantWriter)) {
					this.isValid = false;
					this.errorMessage = "Invalid bootstrap method argument entry '" + TypeWriter.getConstantPoolTagName(argument.getTypeTag()) + "'";
					break;
				}

				if (argument instanceof StringNode) {
					stringReferences.put((StringNode)argument, this);
				} else if (argument instanceof IntegerNode) {
					integerReferences.put((IntegerNode)argument, this);
				} else if (argument instanceof LongNode) {
					longReferences.put((LongNode)argument, this);
				} else if (argument instanceof ClassInfoWriter) {
					classReferences.put((ClassInfoWriter)argument, this);
				}

				this.bootstrapArguments[i] = argument;
			}
		}
	}

	public BootstrapMethodEntry(BootstrapMethodsAttribute bootstrapMethodsAttribute, ResolvedMethodHandleWriter bootstrapMethodHandle, TypeWriter[] bootstrapArguments, int entryIndex) {
		super(bootstrapMethodsAttribute);
		this.bootstrapMethodHandle = bootstrapMethodHandle;
		this.bootstrapArguments = bootstrapArguments;
		this.entryIndex = entryIndex;
	}

	public void updateRecordComponentNames(AbstractClassNode recordClass) {
		if (this.isValid
			&& this.getBootstrapMethodClassName().equals("java/lang/runtime/ObjectMethods")
			&& this.bootstrapArguments != null
			&& this.bootstrapArguments.length >= 2) {
			int nextArgumentIndex = 1;
			nextArgumentIndex++;
			TypeWriter namesArgument = this.bootstrapArguments[1];
			if (namesArgument instanceof StringNode) {
				StringNode namesNode = (StringNode)namesArgument;
				String originalNames = namesNode.getDisplayValue();
				StringBuilder namesBuilder = new StringBuilder();
				StringTokenizer namesTokenizer = new StringTokenizer(originalNames, ";");
				int componentCount = namesTokenizer.countTokens();

				for (int componentIndex = 0; componentIndex < componentCount; componentIndex++) {
					if (componentIndex > 0) {
						namesBuilder.append(";");
					}

					String componentName = namesTokenizer.nextToken();
					if (nextArgumentIndex < this.bootstrapArguments.length) {
						TypeWriter argument = this.bootstrapArguments[nextArgumentIndex++];
						if (argument instanceof ResolvedMethodHandleWriter) {
							MemberNode getterMember = ((ResolvedMethodHandleWriter)argument).getTargetMember();
							if (getterMember != null) {
								namesBuilder.append(getterMember.getNameDisplay());
							} else {
								namesBuilder.append(componentName);
							}
						} else {
							namesBuilder.append(componentName);
						}
					} else {
						namesBuilder.append(componentName);
					}
				}

				String updatedNames = namesBuilder.toString();
				if (!updatedNames.equals(originalNames)) {
					namesNode.setValueFromString(updatedNames);
				}
			}
		}
	}

	@Override
	public void updateIntegerConstant(IntegerNode oldReference, IntegerNode newReference) {
		for (int i = 0; i < this.bootstrapArguments.length; i++) {
			if (this.bootstrapArguments[i] == oldReference) {
				this.bootstrapArguments[i] = newReference;
				break;
			}
		}
	}

	public void collectMethodHandleRefs(Set<MethodNode> methodReferences) {
		for (TypeWriter argument : this.bootstrapArguments) {
			if (argument.getTypeTag() == TypeTags.METHOD_HANDLE) {
				((ResolvedMethodHandleWriter)argument).collectWorkspaceTargets(methodReferences);
			}
		}
	}

	public void collectDependencies(Set<AbstractClassNode> classReferences, Set<AbstractClassNode> signatureClassReferences, Set<AbstractFieldNode> fieldReferences, Set<AbstractMethodNode> methodReferences) {
		this.bootstrapMethodHandle.collectDependencies(classReferences, signatureClassReferences, fieldReferences, methodReferences);

		for (TypeWriter argument : this.bootstrapArguments) {
			switch (argument.getTypeTag()) {
				case INTEGER:
				case FLOAT:
				case LONG:
				case DOUBLE:
				case STRING:
				default:
					break;
				case CLASS:
					String internalName = ((ClassInfoWriter)argument).getInternalName();
					AbstractClassNode elementClassNode = TypeWriter.getArrayElementClassNode(internalName);
					if (elementClassNode != null) {
						Set<AbstractClassNode> targetClassReferences;
						if (!internalName.startsWith("[")) {
							targetClassReferences = signatureClassReferences;
						} else {
							targetClassReferences = classReferences;
						}

						if (elementClassNode.hasInnerClassInfo()) {
							targetClassReferences.addAll(elementClassNode.getSelfAndAllInnerClasses());
						} else {
							targetClassReferences.add(elementClassNode);
						}
					}
					break;
				case METHOD_HANDLE:
					((ResolvedMethodHandleWriter)argument).collectDependencies(classReferences, signatureClassReferences, fieldReferences, methodReferences);
					break;
				case METHOD_TYPE:
					((MethodDescriptorWriter)argument).collectDependencies(classReferences, signatureClassReferences, fieldReferences, methodReferences);
			}
		}
	}

	public int getEntryIndex() {
		return this.entryIndex;
	}

	public void collectWorkspaceDependencies(Set<ClassNode> classReferences, Set<ClassNode> signatureClassReferences, Set<FieldNode> fieldReferences, Set<MethodNode> methodReferences) {
		this.bootstrapMethodHandle.collectWorkspaceDependencies(classReferences, signatureClassReferences, fieldReferences, methodReferences);

		for (TypeWriter argument : this.bootstrapArguments) {
			switch (argument.getTypeTag()) {
				case INTEGER:
				case FLOAT:
				case LONG:
				case DOUBLE:
				case STRING:
				default:
					break;
				case CLASS:
					String internalName = ((ClassInfoWriter)argument).getInternalName();
					ClassNode classNode = TypeWriter.getArrayElementWorkspaceNode(internalName);
					classNode = this.getEquivalentClassNode(classNode);
					if (classNode != null) {
						if (!internalName.startsWith("[")) {
							signatureClassReferences.add(classNode);
						} else {
							classReferences.add(classNode);
						}
					}
					break;
				case METHOD_HANDLE:
					((ResolvedMethodHandleWriter)argument).collectWorkspaceDependencies(classReferences, signatureClassReferences, fieldReferences, methodReferences);
					break;
				case METHOD_TYPE:
					((MethodDescriptorWriter)argument).collectWorkspaceDependencies(classReferences, signatureClassReferences, fieldReferences, methodReferences);
					break;
				case CONSTANT_DYNAMIC:
					((DynamicConstantWriter)argument).collectWorkspaceDependencies(classReferences, signatureClassReferences, fieldReferences, methodReferences);
			}
		}
	}

	public boolean isValid() {
		return this.isValid;
	}

	@Override
	public void updateLongConstant(LongNode oldReference, LongNode newReference) {
		for (int i = 0; i < this.bootstrapArguments.length; i++) {
			if (this.bootstrapArguments[i] == oldReference) {
				this.bootstrapArguments[i] = newReference;
				break;
			}
		}
	}

	@Override
	public void acceptVisitor(ConstantPoolVisitor visitor) {
		this.bootstrapMethodHandle.accept(visitor, this, this.getParent());

		for (TypeWriter argument : this.bootstrapArguments) {
			visitor.visit(argument, this, this.getParent());
		}
	}

	@Override
	public void updateClassReference(ClassInfoWriter oldReference, ClassInfoWriter newReference) {
		for (int i = 0; i < this.bootstrapArguments.length; i++) {
			if (this.bootstrapArguments[i] == oldReference) {
				this.bootstrapArguments[i] = newReference;
				break;
			}
		}
	}

	public String getDebugString() {
		StringBuilder builder = new StringBuilder();
		builder.append('[');
		builder.append(this.bootstrapMethodHandle.getValueAsString());
		builder.append(" : ");
		builder.append('[');

		for (int i = 0; i < this.bootstrapArguments.length; i++) {
			builder.append(this.bootstrapArguments[i].getValueAsString());
			if (i < this.bootstrapArguments.length - 1) {
				builder.append(',');
			}
		}

		builder.append(']');
		builder.append(']');
		return builder.toString();
	}

	public void truncateArguments(int lastArgumentIndex) {
		TypeWriter[] truncatedArguments = new TypeWriter[lastArgumentIndex + 1];
		System.arraycopy(this.bootstrapArguments, 0, truncatedArguments, 0, truncatedArguments.length);
		this.bootstrapArguments = truncatedArguments;
	}

	public boolean matchesMethodHandle(String className, String methodName, String descriptor) {
		return this.bootstrapMethodHandle.getTargetClassName().equals(className)
			&& this.bootstrapMethodHandle.getTargetName().equals(methodName)
			&& this.bootstrapMethodHandle.getTargetDescriptor().equals(descriptor);
	}

	public Set<BootstrapMethodEntry> getDynamicConstantRefs() {
		HashSet<BootstrapMethodEntry> dynamicConstantEntries = new HashSet<>(13);

		for (TypeWriter argument : this.bootstrapArguments) {
			if (argument instanceof DynamicConstantWriter) {
				DynamicConstantWriter dynamicConstant = (DynamicConstantWriter)argument;
				dynamicConstantEntries.add(dynamicConstant.getBootstrapMethodEntry());
			}
		}

		return dynamicConstantEntries;
	}

	protected void writeToStream(DataOutputStream dataOutputStream) throws IOException {
		dataOutputStream.writeShort(this.bootstrapMethodHandle.getTag());
		dataOutputStream.writeShort(this.bootstrapArguments.length);

		for (TypeWriter argument : this.bootstrapArguments) {
			dataOutputStream.writeShort(argument.getTag());
		}
	}

	public String getUniqueID() {
		return String.valueOf(this.getEntryIndex()) + '~' + System.identityHashCode(this);
	}

	protected void writeToStreamRemapped(DataOutputStream dataOutputStream, Map<TypeWriter, TypeWriter> writerMap, ScriptContext scriptContext) throws IOException {
		dataOutputStream.writeShort(this.bootstrapMethodHandle.getTag());
		dataOutputStream.writeShort(this.bootstrapArguments.length);

		for (TypeWriter argument : this.bootstrapArguments) {
			TypeWriter remappedArgument = writerMap.get(argument);
			if (remappedArgument != null) {
				dataOutputStream.writeShort(remappedArgument.getTag());
			} else {
				dataOutputStream.writeShort(argument.getTag());
			}
		}
	}

	public ResolvedMethodHandleWriter getBootstrapMethodHandle() {
		return this.bootstrapMethodHandle;
	}

	public TypeWriter[] getBootstrapArguments() {
		TypeWriter[] arguments = new TypeWriter[this.bootstrapArguments.length];
		System.arraycopy(this.bootstrapArguments, 0, arguments, 0, this.bootstrapArguments.length);
		return arguments;
	}

	@Override
	public void updateStringConstant(StringNode oldReference, StringNode newReference) {
		for (int i = 0; i < this.bootstrapArguments.length; i++) {
			if (this.bootstrapArguments[i] == oldReference) {
				this.bootstrapArguments[i] = newReference;
				break;
			}
		}
	}

	public String getBootstrapMethodClassName() {
		return this.bootstrapMethodHandle != null ? this.bootstrapMethodHandle.getTargetClassName() : null;
	}

	public void notifyParentToReindex() {
		((BootstrapMethodsAttribute)this.getParent()).reindexEntries();
	}

	public String getErrorMessage() {
		return this.errorMessage;
	}

	public boolean isBootstrapMethodStatic() {
		return this.bootstrapMethodHandle.isInvokeStatic();
	}

	public int getEntrySizeInBytes() {
		return 4 + this.bootstrapArguments.length * 2;
	}

	public void setEntryIndex(int entryIndex) {
		this.entryIndex = entryIndex;
	}
}
