package com.zelix.framework;

import com.zelix.exceptions.FileException;
import com.zelix.framework.attributes.base.AbstractParameterAnnotationsAttributeNode;
import com.zelix.framework.attributes.code.CodeAttribute;
import com.zelix.framework.attributes.declaration.ExceptionsAttribute;
import com.zelix.framework.attributes.declaration.MethodParametersAttribute;
import com.zelix.framework.code.InstructionListNode;
import com.zelix.framework.listener.SimpleReferenceListener;
import com.zelix.framework.signature.MethodID;
import com.zelix.framework.signature.MethodParameterKey;
import com.zelix.framework.signature.MethodSignature;
import com.zelix.framework.writer.UTFStringWriter;
import com.zelix.framework.writer.base.AbstractConstantPoolWriter;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.repository.Hierarchy;
import com.zelix.tools.parameterChanger.ParameterPattern;
import com.zelix.utils.ClassMetrics;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.dataset.AtomicInt;
import com.zelix.utils.dataset.ByteDataBuffer;
import com.zelix.utils.enumerations.EmptyEnumeration;
import com.zelix.workspace.AbstractWorkspace;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.Map.Entry;

public abstract class AbstractMethodNode extends MemberNode {
	public int codeAttrIndex = -1;
	public int exceptionsAttrIndex = -1;
	private int lambdaSamParamCount = -1;
	public String descriptor;
	public String nameLower;
	public String methodName;
	private boolean lambdaMetaFactoryMarked;
	public List<String> parameterDescriptors;
	public boolean isSyntheticLibraryStub;

	public AbstractMethodNode(AbstractClassNode owner, UTFStringWriter nameWriter, UTFStringWriter descriptorWriter, AttributesNode[] attributes, int generationTypeFlag) {
		super(owner, nameWriter, descriptorWriter, attributes, generationTypeFlag);
		this.initMethodCache();
	}

	public AbstractMethodNode(StructureNode parent, ByteDataBuffer buf, MultiMap<UTFStringWriter, SimpleReferenceListener> utf8References, PrintWriter printWriter) throws IOException {
		super(parent, buf, utf8References, printWriter);
		this.initMethodCache();
	}

	public AbstractMethodNode(StructureNode parent, ByteDataBuffer buf, MultiMap<UTFStringWriter, SimpleReferenceListener> utf8References) throws IOException {
		super(parent, buf, utf8References);
		this.initMethodCache();
	}

	public static int getLocalSlotSize(String descriptor) {
		if (descriptor.equals("D")) {
			return 2;
		} else {
			return descriptor.equals("J") ? 2 : 1;
		}
	}

	public static String formatNameWithParams(String name, String descriptor) {
		StringBuilder builder = new StringBuilder();
		builder.append(name);
		builder.append('(');
		List<String> paramDescriptors = TypeWriter.parseParameterDescriptors(descriptor, true);
		int parameterCount = paramDescriptors.size();

		for (int i = 0; i < parameterCount; i++) {
			String parameterDescriptor = paramDescriptors.get(i);
			builder.append(TypeWriter.descriptorToHumanReadable(parameterDescriptor));
			if (i < parameterCount - 1) {
				builder.append(", ");
			}
		}

		builder.append(')');
		return builder.toString();
	}

	public static AbstractMethodNode resolveByCovariantReturn(AbstractMethodNode[] methods, AbstractWorkspace workspace) {
		if (methods != null && methods.length != 0) {
			if (methods.length == 1) {
				return methods[0];
			} else {
				HashMap<String, AbstractMethodNode> methodsByTypeName = GlobalUtils.newHashMap();
				int expectedDimensionCount = 0;
				String[] returnTypeNames = new String[methods.length];

				for (int methodIndex = 0; methodIndex < methods.length; methodIndex++) {
					String returnTypeName = methods[methodIndex].getReturnDescriptor();
					int dimensionCount = 0;

					while (returnTypeName.charAt(dimensionCount) == '[') {
						dimensionCount++;
					}

					if (methodIndex == 0) {
						expectedDimensionCount = dimensionCount;
					} else if (dimensionCount != expectedDimensionCount) {
						return null;
					}

					if (dimensionCount > 0) {
						returnTypeName = returnTypeName.substring(dimensionCount);
					}

					if (returnTypeName.endsWith(";")) {
						returnTypeName = returnTypeName.substring(1, returnTypeName.length() - 1);
					}

					returnTypeNames[methodIndex] = returnTypeName;
				}

				try {
					sortTypesMostSpecificFirst(returnTypeNames, workspace);

					for (int typeIndex = 0; typeIndex < returnTypeNames.length - 1; typeIndex++) {
						if (!workspace.isClassInstanceOf(returnTypeNames[typeIndex], returnTypeNames[typeIndex + 1]) && !workspace.isSubclassOf(returnTypeNames[typeIndex], returnTypeNames[typeIndex + 1])) {
							return null;
						}
					}
				} catch (FileException ignored) {
					return null;
				}

				return methodsByTypeName.get(returnTypeNames[0]);
			}
		} else {
			return null;
		}
	}

	private static void sortTypesMostSpecificFirst(String[] typeNames, AbstractWorkspace workspace) {
		for (int i = 0; i < typeNames.length - 1; i++) {
			int bestIndex = i;
			String mostSpecificType = typeNames[i];

			for (int candidateIndex = i + 1; candidateIndex < typeNames.length; candidateIndex++) {
				String candidateType = typeNames[candidateIndex];
				if (workspace.isClassInstanceOf(candidateType, mostSpecificType) || workspace.isSubclassOf(candidateType, mostSpecificType)) {
					bestIndex = candidateIndex;
					mostSpecificType = candidateType;
				}
			}

			if (bestIndex > i) {
				typeNames[bestIndex] = typeNames[i];
				typeNames[i] = mostSpecificType;
			}
		}
	}

	@Override
	public String getNameLowercase() {
		return this.nameLower;
	}

	public void updateParameterAnnotations(ParameterPattern parameterPattern) {
		for (int i = 0; i < this.attributeCount; i++) {
			if (this.attributes[i] instanceof AbstractParameterAnnotationsAttributeNode) {
				((AbstractParameterAnnotationsAttributeNode)this.attributes[i]).updateParameterAnnotations(parameterPattern);
			}
		}
	}

	public void updateMethodParameters(ParameterPattern parameterPattern, AbstractConstantPoolWriter constantPoolWriter, List<TypeWriter> newConstantPoolEntries) {
		for (int i = 0; i < this.attributeCount; i++) {
			if (this.attributes[i] instanceof MethodParametersAttribute) {
				MethodParametersAttribute parametersAttribute = (MethodParametersAttribute)this.attributes[i];
				List<String> normalizedParameterDescriptors = this.getNormalizedParameterDescriptors();
				parametersAttribute.insertParameters(parameterPattern, this.isStatic(), normalizedParameterDescriptors, constantPoolWriter, newConstantPoolEntries);
				break;
			}
		}
	}

	public final void setBridge(boolean flag) {
		this.accessFlagsNode.setBridge(flag);
	}

	public final void setParameterObfuscationMarked(boolean flag) {
		if (flag) {
			this.setInternalStateFlag(4);
		} else {
			this.clearInternalStateFlag(4);
		}
	}

	public String getOriginalParameterDescriptor() {
		int closeParenIndex = this.originalDescriptor.lastIndexOf(41);
		return this.originalDescriptor.substring(0, closeParenIndex + 1);
	}

	public abstract void collectClassDependencies(
		Set<AbstractClassNode> referencedClasses, Set<AbstractClassNode> signatureClasses, Set<AbstractFieldNode> referencedFields, Set<AbstractMethodNode> referencedMethods
	);

	public MethodSignature getOriginalMethodSignature() {
		return new MethodSignature(this.getOriginalNameForSignature(), this.getOriginalDescriptor());
	}

	public boolean isSameMethodKey(AbstractMethodNode other) {
		return this.methodName.equals(other.methodName) && this.descriptor.equals(other.descriptor) && this.getInternalName().equals(other.getInternalName());
	}

	public MethodID toMethodId() {
		return new MethodID(this.getOwnerInternalName(), this.getName(), this.getDescriptor());
	}

	public boolean hasLambdaSamParamCount() {
		return this.lambdaSamParamCount > -1;
	}

	public abstract InstructionListNode getInstructionList();

	public final void markVarargs() {
		this.accessFlagsNode.markVarargs();
	}

	public final String toDeclarationWithParamNames() {
		return this.toDeclarationStringSimple(true);
	}

	private void initMethodCache() {
		if (this.isValid()) {
			this.descriptor = this.descriptorWriter.getValue();
			this.methodName = this.nameWriter.getValue();
			this.nameLower = this.getNameDisplay().toLowerCase();
			this.parameterDescriptors = TypeWriter.parseParameterDescriptors(this.descriptor, true);
		}
	}

	public final boolean isClinit() {
		return this.methodName.equals("<clinit>");
	}

	public List<String> getReadableParameterTypes() {
		return TypeWriter.getReadableParameterTypes(this.descriptor);
	}

	public String getParameterDescriptor() {
		int closeParenIndex = this.descriptor.lastIndexOf(41);
		return this.descriptor.substring(0, closeParenIndex + 1);
	}

	public List<String> getParameterDescriptors() {
		return this.parameterDescriptors != null ? Collections.unmodifiableList(this.parameterDescriptors) : Collections.emptyList();
	}

	public final void setParameterLocalsShuffled(boolean flag) {
		if (flag) {
			this.setInternalStateFlag(8);
		} else {
			this.clearInternalStateFlag(8);
		}
	}

	public final void setPreserveUnusedParameterSlots(boolean flag) {
		if (flag) {
			this.setInternalStateFlag(16);
		} else {
			this.clearInternalStateFlag(16);
		}
	}

	public final boolean preservesUnusedParameterSlots() {
		return this.hasInternalStateFlag(16);
	}

	public final void setMethodParametersAttributePresent(boolean flag) {
		if (flag) {
			this.setInternalStateFlag(32);
		} else {
			this.clearInternalStateFlag(32);
		}
	}

	public final boolean isMethodParametersAttributePresent() {
		return this.hasInternalStateFlag(32);
	}

	public final String formatNameAndParams(boolean useCurrentName) {
		StringBuilder builder = new StringBuilder();
		if (useCurrentName) {
			builder.append(this.getName());
		} else {
			builder.append(this.getNameDisplay());
		}

		builder.append('(');
		int parameterCount = this.parameterDescriptors.size();

		for (int i = 0; i < parameterCount; i++) {
			String parameterDescriptor = this.parameterDescriptors.get(i);
			builder.append(TypeWriter.descriptorToHumanReadable(parameterDescriptor));
			if (i < parameterCount - 1) {
				builder.append(", ");
			}
		}

		builder.append(')');
		return builder.toString();
	}

	public int getGenerationType() {
		return this.generationTypeFlag;
	}

	public MethodParameterKey getMethodParameterKey() {
		return new MethodParameterKey(this.getName(), this.getDescriptor());
	}

	public final boolean isVarArgs() {
		return this.accessFlagsNode.isVarArgs();
	}

	public void setLambdaSamParamCount(int count) {
		this.lambdaSamParamCount = count;
	}

	public abstract void collectInnerClassMethodCalls(Set<AbstractMethodNode> collectedMethods, Set<AbstractClassNode> innerClasses);

	public void markLambdaMetaFactory() {
		this.lambdaMetaFactoryMarked = true;
	}

	@Override
	public String getName() {
		return this.methodName;
	}

	@Override
	public final String getNameDisplay() {
		if (this.methodName.equals("<init>")) {
			return this.getConstructorDisplayName();
		} else {
			return this.methodName.equals("<clinit>") ? "static" : this.methodName;
		}
	}

	public final String getDisplayNameWithDescriptor() {
		return this.getNameDisplay() + this.getDescriptor();
	}

	@Override
	public final void setDescriptor(String descriptor) {
		this.descriptor = descriptor;
		this.parameterDescriptors = TypeWriter.parseParameterDescriptors(this.descriptor, true);
		super.setDescriptor(descriptor);
	}

	public final String toDeclarationString(boolean includeParamNames, Map<Integer, String> localNames) {
		String[] localNamesBySlot = null;
		if (localNames != null) {
			int maxLocalSlot = 0;

			for (Integer localSlot : localNames.keySet()) {
				if (localSlot > maxLocalSlot) {
					maxLocalSlot = localSlot;
				}
			}

			localNamesBySlot = new String[maxLocalSlot + 1];

			for (Entry<Integer, String> localNameEntry : localNames.entrySet()) {
				localNamesBySlot[localNameEntry.getKey()] = localNameEntry.getValue();
			}
		}

		StringBuilder builder = new StringBuilder();
		int currentSlot = this.accessFlagsNode.isStatic() ? 0 : 1;

		for (int i = 0; i < this.parameterDescriptors.size(); i++) {
			if (includeParamNames) {
				builder.append(TypeWriter.descriptorToHumanReadable(this.parameterDescriptors.get(i)));
				builder.append(" ");
				if (localNamesBySlot != null && localNamesBySlot.length > currentSlot && localNamesBySlot[currentSlot] != null) {
					builder.append(localNamesBySlot[currentSlot]);
				} else {
					builder.append("local" + currentSlot);
				}

				if (i < this.parameterDescriptors.size() - 1) {
					builder.append(", ");
				}

				currentSlot += getLocalSlotSize(this.parameterDescriptors.get(i));
			} else {
				builder.append(TypeWriter.descriptorToHumanReadable(this.parameterDescriptors.get(i)));
				builder.append(i < this.parameterDescriptors.size() - 1 ? ", " : "");
			}
		}

		String declaration;
		if (this.methodName.equals("<init>")) {
			declaration = this.accessFlagsNode.getAccessFlagsString() + this.getConstructorDisplayName() + "(" + builder + ")";
		} else if (this.methodName.equals("<clinit>")) {
			declaration = this.accessFlagsNode.getAccessFlagsString();
		} else {
			declaration = this.accessFlagsNode.getAccessFlagsString() + TypeWriter.descriptorToHumanReadable(this.descriptor) + " " + this.methodName + "(" + builder + ")";
		}

		return declaration;
	}

	public boolean isInstanceMethod() {
		return !this.isStatic() && !this.isConstructor();
	}

	@Override
	public final boolean isBridge() {
		return this.accessFlagsNode.isBridge();
	}

	public final MethodSignature getMethodSignature() {
		return new MethodSignature(this.getName(), this.getDescriptor());
	}

	public final boolean isParameterLocalsShuffled() {
		return this.hasInternalStateFlag(8);
	}

	public final void setVarargs(boolean flag) {
		this.accessFlagsNode.setVarargs(flag);
	}

	public final String toDeclarationStringSimple(boolean includeParamNames) {
		return this.toDeclarationString(includeParamNames, null);
	}

	@Override
	public boolean isField() {
		return false;
	}

	@Override
	public final String getDescriptor() {
		return this.descriptor;
	}

	public String getOriginalNameWithParams() {
		return this.getOriginalName() + MethodSignature.formatRemappedParameters(this.getOriginalParameterDescriptor(), null);
	}

	public Enumeration<String> getParameterAnnotationTypes(int paramIndex) {
		HashSet<String> annotationTypes = null;

		for (int i = 0; i < this.attributeCount; i++) {
			if (annotationTypes == null) {
				annotationTypes = GlobalUtils.newHashSet(13);
			}

			if (this.attributes[i] instanceof AbstractParameterAnnotationsAttributeNode) {
				String[] attributeAnnotationTypes = ((AbstractParameterAnnotationsAttributeNode)this.attributes[i]).getAnnotationTypes(paramIndex);

				for (int annotationIndex = 0; annotationIndex < attributeAnnotationTypes.length; annotationIndex++) {
					annotationTypes.add(attributeAnnotationTypes[annotationIndex]);
				}
			}
		}

		return (Enumeration<String>)(annotationTypes == null ? new EmptyEnumeration<>() : Collections.enumeration(annotationTypes));
	}

	public int getParameterCount() {
		return this.parameterDescriptors != null ? this.parameterDescriptors.size() : 0;
	}

	public final boolean isConstructor() {
		return this.methodName.equals("<init>");
	}

	public final String getNameAndDescriptor() {
		return this.methodName + this.descriptor;
	}

	public final int getFirstNonParamLocalSlot() {
		int nextLocalSlot = this.accessFlagsNode.isStatic() ? 0 : 1;

		for (int i = 0; i < this.parameterDescriptors.size(); i++) {
			if (this.parameterDescriptors.get(i).equals("D")) {
				nextLocalSlot += 2;
			} else if (this.parameterDescriptors.get(i).equals("J")) {
				nextLocalSlot += 2;
			} else {
				nextLocalSlot++;
			}
		}

		return nextLocalSlot;
	}

	public int getLambdaSamParamCount() {
		return this.lambdaSamParamCount;
	}

	public final void updateCodeMetrics(ClassMetrics classMetrics) {
		if (this.codeAttrIndex != -1) {
			((CodeAttribute)this.attributes[this.codeAttrIndex]).updateClassMetrics(classMetrics);
		}
	}

	public String getReturnDescriptor() {
		int closeParenIndex = this.descriptor.lastIndexOf(41);
		return this.descriptor.substring(closeParenIndex + 1);
	}

	public final List<String> getNormalizedParameterDescriptors() {
		List<String> normalizedDescriptors = TypeWriter.parseParameterDescriptors(this.descriptor);
		int parameterCount = normalizedDescriptors.size();

		for (int i = 0; i < parameterCount; i++) {
			String parameterDescriptor = normalizedDescriptors.get(i);
			if (parameterDescriptor.equals("B") || parameterDescriptor.equals("C") || parameterDescriptor.equals("S") || parameterDescriptor.equals("Z")) {
				normalizedDescriptors.set(i, "I");
			}
		}

		return normalizedDescriptors;
	}

	@Override
	public final void setName(String name) {
		if (this.isConstructor()) {
			this.markChanged();
			this.notify(new AtomicInt(0), this, null);
		} else {
			this.methodName = name;
			super.setName(name);
		}

		this.nameLower = this.getNameDisplay().toLowerCase();
	}

	public final boolean isParameterObfuscationMarked() {
		return this.hasInternalStateFlag(4);
	}

	public String getReadableReturnType() {
		return TypeWriter.descriptorToHumanReadable(this.descriptor, false, true);
	}

	public boolean isLambdaMetaFactoryMarked() {
		return this.lambdaMetaFactoryMarked;
	}

	@Override
	public String getSignature() {
		StringBuilder builder = new StringBuilder();
		builder.append(TypeWriter.descriptorToHumanReadable(this.getOriginalDescriptor()));
		builder.append(" ");
		builder.append(this.getOriginalName());
		builder.append('(');
		List<String> originalParameterDescriptors = TypeWriter.parseParameterDescriptors(this.getOriginalDescriptor(), false);

		for (int i = 0; i < originalParameterDescriptors.size(); i++) {
			builder.append(TypeWriter.descriptorToHumanReadable(originalParameterDescriptors.get(i)));
			builder.append(i < originalParameterDescriptors.size() - 1 ? ", " : "");
		}

		builder.append(')');
		return builder.toString();
	}

	@Override
	public String getDisplayName() {
		return this.getAccessFlagsString() + this.getMethodSignature().toFullHumanReadableString();
	}

	public final void setParameterDescriptorChanged(boolean flag) {
		if (flag) {
			this.setInternalStateFlag(2);
		} else {
			this.clearInternalStateFlag(2);
		}
	}

	public final String toDeclarationWithMappedLocals(Map<Integer, String> localNames) {
		return this.toDeclarationString(true, localNames);
	}

	public void registerVirtualMethodOwner(Hierarchy hierarchy, Map<MethodSignature, AbstractClassNode> methodOwners, boolean classIsMissing, AbstractClassNode ownerClass) {
		if (!this.isConstructor() && !this.isClinit() && !this.isStatic() && (this.isAbstract() || !this.isPrivate())) {
			MethodSignature methodSignature = this.getMethodSignature();
			AbstractClassNode previousOwner = methodOwners.put(methodSignature, ownerClass);
			if (previousOwner != null) {
				methodOwners.put(methodSignature, previousOwner);
			}

			if (classIsMissing) {
				hierarchy.addConflictingSignature(methodSignature);
			}
		}
	}

	public String getDisplayNameWithParams(HashMap<String, String> classNameMap) {
		return this.getNameDisplay() + MethodSignature.formatRemappedParameters(this.getParameterDescriptor(), classNameMap);
	}

	public ArrayList<String> getDeclaredExceptionInternalNames() {
		ArrayList<String> exceptionInternalNames = new ArrayList<>();
		ExceptionsAttribute exceptionsAttribute = null;
		if (this.exceptionsAttrIndex != -1) {
			exceptionsAttribute = (ExceptionsAttribute)this.attributes[this.exceptionsAttrIndex];
		}

		if (exceptionsAttribute != null) {
			for (int i = 0; i < exceptionsAttribute.getExceptionCount(); i++) {
				exceptionInternalNames.add(exceptionsAttribute.getExceptionClass(i).getInternalName());
			}
		}

		return exceptionInternalNames;
	}

	public final boolean isParameterDescriptorChanged() {
		return this.hasInternalStateFlag(2);
	}

	public Enumeration<String> getOriginalParameterAnnotationTypes(int paramIndex) {
		HashSet<String> annotationTypes = null;

		for (int i = 0; i < this.attributeCount; i++) {
			if (annotationTypes == null) {
				annotationTypes = GlobalUtils.newHashSet(13);
			}

			if (this.attributes[i] instanceof AbstractParameterAnnotationsAttributeNode) {
				String[] attributeAnnotationTypes = ((AbstractParameterAnnotationsAttributeNode)this.attributes[i]).getOriginalAnnotationTypes(paramIndex);

				for (int annotationIndex = 0; annotationIndex < attributeAnnotationTypes.length; annotationIndex++) {
					annotationTypes.add(attributeAnnotationTypes[annotationIndex]);
				}
			}
		}

		return (Enumeration<String>)(annotationTypes == null ? new EmptyEnumeration<>() : Collections.enumeration(annotationTypes));
	}
}
