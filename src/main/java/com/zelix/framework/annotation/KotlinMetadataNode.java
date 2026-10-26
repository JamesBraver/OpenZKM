package com.zelix.framework.annotation;

import com.zelix.framework.AbstractClassNode;
import com.zelix.framework.AbstractFieldNode;
import com.zelix.framework.AbstractMethodNode;
import com.zelix.framework.StructureNode;
import com.zelix.framework.constant.ConstantPoolVisitor;
import com.zelix.framework.listener.SimpleReferenceListener;
import com.zelix.framework.signature.MethodSignature;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.wrappers.FieldNode;
import com.zelix.framework.wrappers.MethodNode;
import com.zelix.framework.writer.UTFStringWriter;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.klassmaster.config.Options;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.repository.ClassProvider;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.StringUtils;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.dataset.AtomicBool;
import com.zelix.utils.dataset.AtomicInt;
import com.zelix.utils.dataset.ByteDataBuffer;
import com.zelix.workspace.PhantomClassManager;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class KotlinMetadataNode extends AnnotationNode {
	private int kind;
	private String[] data1;
	private String[] data2;
	private String extraString;
	private String packageName;
	private int[] metadataVersion;
	private int[] bytecodeVersion;
	private Integer extraInt;

	public KotlinMetadataNode(StructureNode parent, int typeIndex, TypeWriter typeWriter, ByteDataBuffer dataBuffer, MultiMap<UTFStringWriter, SimpleReferenceListener> utf8References) throws IOException {
		super(parent, typeIndex, typeWriter, dataBuffer, utf8References);

		for (int paramIndex = 0; paramIndex < super.paramCount; paramIndex++) {
			AnnotationParameterNode param = super.params[paramIndex];
			AnnotationElementNode element = param.getElement();
			String paramName = param.getName();
			switch (paramName) {
				case "k":
					this.kind = ((PrimitiveAnnotationElement)element).getValue();
					break;
				case "d1":
					if (!element.isArrayElement()) {
						break;
					}

					AnnotationElementNode[] data1Elements = ((ArrayAnnotationElement)element).getElements();
					this.data1 = new String[data1Elements.length];

					for (int data1Index = 0; data1Index < data1Elements.length; data1Index++) {
						AnnotationElementNode data1Element = data1Elements[data1Index];
						if (data1Element instanceof StringAnnotationElement) {
							StringAnnotationElement data1StringElement = (StringAnnotationElement)data1Element;
							this.data1[data1Index] = data1StringElement.getValue();
						}
					}
					break;
				case "d2":
					if (!element.isArrayElement()) {
						break;
					}

					AnnotationElementNode[] data2Elements = ((ArrayAnnotationElement)element).getElements();
					this.data2 = new String[data2Elements.length];

					for (int data2Index = 0; data2Index < data2Elements.length; data2Index++) {
						AnnotationElementNode data2Element = data2Elements[data2Index];
						if (data2Element instanceof StringAnnotationElement) {
							StringAnnotationElement data2StringElement = (StringAnnotationElement)data2Element;
							this.data2[data2Index] = data2StringElement.getValue();
						}
					}
					break;
				case "xs":
					if (element instanceof StringAnnotationElement) {
						StringAnnotationElement extraStringElement = (StringAnnotationElement)element;
						this.extraString = extraStringElement.getValue();
					}
					break;
				case "pn":
					if (element instanceof StringAnnotationElement) {
						StringAnnotationElement packageNameElement = (StringAnnotationElement)element;
						this.packageName = packageNameElement.getValue();
					}
					break;
				case "mv":
					if (!element.isArrayElement()) {
						break;
					}

					AnnotationElementNode[] metadataVersionElements = ((ArrayAnnotationElement)element).getElements();
					this.metadataVersion = new int[metadataVersionElements.length];

					for (int metadataVersionIndex = 0; metadataVersionIndex < metadataVersionElements.length; metadataVersionIndex++) {
						AnnotationElementNode metadataVersionElement = metadataVersionElements[metadataVersionIndex];
						if (metadataVersionElement instanceof PrimitiveAnnotationElement) {
							PrimitiveAnnotationElement metadataVersionPrimitiveElement = (PrimitiveAnnotationElement)metadataVersionElement;
							this.metadataVersion[metadataVersionIndex] = metadataVersionPrimitiveElement.getValue();
						}
					}
					break;
				case "bv":
					if (!element.isArrayElement()) {
						break;
					}

					AnnotationElementNode[] bytecodeVersionElements = ((ArrayAnnotationElement)element).getElements();
					this.bytecodeVersion = new int[bytecodeVersionElements.length];

					for (int bytecodeVersionIndex = 0; bytecodeVersionIndex < bytecodeVersionElements.length; bytecodeVersionIndex++) {
						AnnotationElementNode bytecodeVersionElement = bytecodeVersionElements[bytecodeVersionIndex];
						if (bytecodeVersionElement instanceof PrimitiveAnnotationElement) {
							PrimitiveAnnotationElement bytecodeVersionPrimitiveElement = (PrimitiveAnnotationElement)bytecodeVersionElement;
							this.bytecodeVersion[bytecodeVersionIndex] = bytecodeVersionPrimitiveElement.getValue();
						}
					}
					break;
				case "xi":
					if (element instanceof PrimitiveAnnotationElement) {
						PrimitiveAnnotationElement extraIntElement = (PrimitiveAnnotationElement)element;
						this.extraInt = extraIntElement.getValue();
					}
			}
		}
	}

	@Override
	public boolean isValid() {
		return super.isValid();
	}

	@Override
	public void syncName() {
		if ((Options.ZKM_TRANSLATE_KOTLIN || Options.translateKotlinMetadata) && super.typeClass != null && super.typeClass.getClassName().equals("kotlin/Metadata")) {
			AbstractClassNode classNode = this.getClassNode();
			AbstractFieldNode[] fieldNodes = classNode.getFields();
			HashSet<String> fieldNames = GlobalUtils.newHashSet(GlobalUtils.getInitialCapacity(fieldNodes.length));

			for (AbstractFieldNode fieldNode : fieldNodes) {
				fieldNames.add(fieldNode.getOriginalName());
			}

			AbstractMethodNode[] methodNodes = classNode.getMethods();
			MultiMap<String, AbstractMethodNode> methodsByOriginalName = new MultiMap<>(methodNodes.length);

			for (AbstractMethodNode methodNode : methodNodes) {
				methodsByOriginalName.put(methodNode.getOriginalName(), methodNode);
			}

			for (int paramIndex = 0; paramIndex < super.paramCount; paramIndex++) {
				AnnotationParameterNode param = super.params[paramIndex];
				if (param.getName().equals("d2")) {
					AnnotationElementNode data2ArrayElement = param.getElement();
					if (data2ArrayElement.isArrayElement()) {
						for (AnnotationElementNode data2Element : ((ArrayAnnotationElement)data2ArrayElement).getElements()) {
							if (data2Element instanceof StringAnnotationElement) {
								StringAnnotationElement data2StringElement = (StringAnnotationElement)data2Element;
								String data2Name = data2StringElement.getValue();
								List<AbstractMethodNode> matchingMethods = methodsByOriginalName.get(data2Name);
								if (matchingMethods != null && !fieldNames.contains(data2Name)) {
									boolean displayNamesMatch = true;
									String displayName = null;

									for (AbstractMethodNode candidateMethod : matchingMethods) {
										if (displayName == null) {
											displayName = candidateMethod.getNameDisplay();
										} else if (!displayName.equals(candidateMethod.getNameDisplay())) {
											displayNamesMatch = false;
											break;
										}
									}

									if (displayNamesMatch && !data2Name.equals(displayName)) {
										data2StringElement.setValue(displayName);
									}
								}
							}
						}
					}
				}
			}
		} else {
			for (int i = 0; i < super.paramCount; i++) {
				super.params[i].syncName();
			}
		}
	}

	@Override
	public String getOriginalTypeName() {
		if (super.valid) {
			return super.typeClass != null ? super.typeClass.getClassName() : this.getTypeInternalName();
		} else {
			return null;
		}
	}

	@Override
	public void acceptVisitor(ConstantPoolVisitor visitor) {
		if (super.valid) {
			super.typeDescUtf.accept(visitor, this, this.getParent());

			for (int i = 0; i < super.paramCount; i++) {
				super.params[i].acceptVisitor(visitor);
			}
		}
	}

	@Override
	public void write(DataOutputStream dataOutputStream) throws IOException {
		dataOutputStream.writeShort(super.typeDescUtf.getTag());
		dataOutputStream.writeShort(super.paramCount);

		for (int i = 0; i < super.paramCount; i++) {
			super.params[i].write(dataOutputStream);
		}
	}

	@Override
	public void writeRemapped(DataOutputStream dataOutputStream, Map<TypeWriter, TypeWriter> remappedConstants, ScriptContext scriptContext) throws IOException {
		UTFStringWriter remappedTypeDescriptorUtf = (UTFStringWriter)remappedConstants.get(super.typeDescUtf);
		if (remappedTypeDescriptorUtf != null) {
			dataOutputStream.writeShort(remappedTypeDescriptorUtf.getTag());
		} else {
			dataOutputStream.writeShort(super.typeDescUtf.getTag());
		}

		dataOutputStream.writeShort(super.paramCount);

		for (int i = 0; i < super.paramCount; i++) {
			super.params[i].writeRemapped(dataOutputStream, remappedConstants, scriptContext);
		}
	}

	@Override
	public void updateSimpleNameReference(UTFStringWriter oldReference, UTFStringWriter newReference) {
		if (super.typeDescUtf == oldReference) {
			super.typeDescUtf = newReference;
		}
	}

	@Override
	public String getTypeDescriptor() {
		return super.typeDescUtf.getValue();
	}

	@Override
	public void collectWorkspaceRefs(Set<ClassNode> classReferences, Set<ClassNode> signatureClassReferences, Set<FieldNode> fieldReferences, Set<MethodNode> methodReferences) {
		if (super.typeClass != null && super.typeClass.isWorkspaceNode()) {
			classReferences.add((ClassNode)super.typeClass);
		}

		for (int i = 0; i < super.paramCount; i++) {
			super.params[i].collectWorkspaceRefs(super.typeClass, classReferences, signatureClassReferences, fieldReferences, methodReferences);
		}
	}

	@Override
	public String findParamLiteral(String paramName, boolean matchOnOriginalNames) {
		for (int i = 0; i < super.paramCount; i++) {
			String candidateName;
			if (matchOnOriginalNames) {
				candidateName = super.params[i].getOriginalName();
			} else {
				candidateName = super.params[i].getName();
			}

			if (paramName.equals(candidateName)) {
				return super.params[i].getReferencedClassName();
			}
		}

		return null;
	}

	@Override
	public void remapDescriptors(HashMap<String, String> packageMappings, HashMap<String, String> classNameMappings) {
		if (super.typeClass != null) {
			String arrayElementName = AbstractClassNode.getArrayElementInternalName(super.typeDescUtf.getValue());
			String classInternalName = super.typeClass.getInternalName();
			if (!classInternalName.equals(arrayElementName)) {
				String newTypeDescriptor = AbstractClassNode.createClassDescriptor(super.typeClass.getInternalName(), super.arrayDepth);
				super.typeDescUtf.setValue(newTypeDescriptor);
			}

			this.getClassNode();
			int metadataKind = 0;

			for (int paramIndex = 0; paramIndex < super.paramCount; paramIndex++) {
				AnnotationParameterNode param = super.params[paramIndex];
				AnnotationElementNode paramElement = param.getElement();
				String paramName = param.getName();
				switch (paramName) {
					case "k":
						metadataKind = ((PrimitiveAnnotationElement)paramElement).getValue();
						break;
					case "d1":
						if (metadataKind != 4 || !paramElement.isArrayElement()) {
							break;
						}

						AnnotationElementNode[] data1Elements = ((ArrayAnnotationElement)paramElement).getElements();

						for (int data1Index = 0; data1Index < data1Elements.length; data1Index++) {
							AnnotationElementNode data1Element = data1Elements[data1Index];
							if (data1Element instanceof StringAnnotationElement) {
								StringAnnotationElement data1StringElement = (StringAnnotationElement)data1Element;
								String data1Descriptor = data1StringElement.getValue();
								if (StringUtils.countOccurrences(data1Descriptor, '/') >= 1) {
									AtomicBool data1Remapped = new AtomicBool();
									String data1RemappedDescriptor = TypeWriter.tryRemapDescriptor(data1Descriptor, classNameMappings, data1Remapped);
									if (data1Remapped.get() && !data1RemappedDescriptor.equals(data1Descriptor)) {
										data1StringElement.setValue(data1RemappedDescriptor);
									}
								}
							}
						}
						break;
					case "d2":
						if (!paramElement.isArrayElement()) {
							break;
						}

						AnnotationElementNode[] data2Elements = ((ArrayAnnotationElement)paramElement).getElements();
						int data2Index = 0;

						for (; data2Index < data2Elements.length; data2Index++) {
							AnnotationElementNode data2Element = data2Elements[data2Index];
							if (data2Element instanceof StringAnnotationElement) {
								StringAnnotationElement data2StringElement = (StringAnnotationElement)data2Element;
								String data2Descriptor = data2StringElement.getValue();
								if (data2Descriptor.startsWith("(") && data2Descriptor.indexOf(41) < data2Descriptor.length() && StringUtils.countOccurrences(data2Descriptor, ')') == 1) {
									String parameterDescriptor = MethodSignature.extractParameterDescriptor(data2Descriptor);
									String returnDescriptor = MethodSignature.extractReturnDescriptor(data2Descriptor);
									String remappedParams = '(' + TypeWriter.remapDescriptor(parameterDescriptor.substring(1, parameterDescriptor.length() - 1), classNameMappings) + ')';
									String remappedReturnDescriptor = TypeWriter.tryRemapDescriptor(returnDescriptor, classNameMappings, new AtomicBool());
									if (!remappedParams.equals(parameterDescriptor) || !remappedReturnDescriptor.equals(returnDescriptor)) {
										data2StringElement.setValue(remappedParams + remappedReturnDescriptor);
									}
								} else if (data2Descriptor.startsWith("L") && data2Descriptor.endsWith(";") || StringUtils.countOccurrences(data2Descriptor, '/') >= 1) {
									AtomicBool data2Remapped = new AtomicBool();
									String data2RemappedDescriptor = TypeWriter.tryRemapDescriptor(data2Descriptor, classNameMappings, data2Remapped);
									if (data2Remapped.get() && !data2RemappedDescriptor.equals(data2Descriptor)) {
										data2StringElement.setValue(data2RemappedDescriptor);
									}
								}
							}
						}
						break;
					case "xs":
						if (paramElement instanceof StringAnnotationElement) {
							StringAnnotationElement extraStringElement = (StringAnnotationElement)paramElement;
							String extraStringValue = extraStringElement.getValue();
							if (StringUtils.countOccurrences(extraStringValue, '/') >= 1) {
								AtomicBool extraStringRemapped = new AtomicBool();
								String remappedExtraString = TypeWriter.tryRemapDescriptor(extraStringValue, classNameMappings, extraStringRemapped);
								if (extraStringRemapped.get() && !remappedExtraString.equals(extraStringValue)) {
									extraStringElement.setValue(remappedExtraString);
								}
							}
						}
					case "mv":
					case "bv":
					case "pn":
					case "xi":
				}
			}
		}
	}

	@Override
	public void collectClassReferences(Set<AbstractClassNode> classReferences) {
		if (super.valid) {
			if (super.typeClass != null) {
				classReferences.add(super.typeClass);
			}

			for (int i = 0; i < super.paramCount; i++) {
				super.params[i].collectClassReferences(classReferences);
			}
		}
	}

	@Override
	public void resolveTypeAndParams(ClassProvider classProvider, PhantomClassManager phantomClassManager, ScriptContext scriptContext) {
		AtomicInt arrayDepthHolder = new AtomicInt(0);
		String elementInternalName = AbstractClassNode.getArrayElementInternalName(super.typeDescUtf.getValue(), arrayDepthHolder);
		super.arrayDepth = arrayDepthHolder.get();
		if (elementInternalName != null) {
			super.typeClass = classProvider.getClassNode(elementInternalName, "analyzing annotations in class '" + this.getSourcePath() + "' (C)", phantomClassManager);
		}

		for (int i = 0; i < super.paramCount; i++) {
			super.params[i].resolveTypeAndParams(super.typeClass, classProvider, phantomClassManager, scriptContext);
		}
	}

	@Override
	public void updateConstants() {
		if (super.typeClass != null && super.typeClass.getClassName().equals("kotlin/Metadata")) {
			AbstractClassNode classNode = this.getClassNode();
			AbstractMethodNode[] methodNodes = classNode.getMethods();
			AbstractFieldNode[] fieldNodes = classNode.getFields();
			HashSet<String> methodNames = GlobalUtils.newHashSet(GlobalUtils.getInitialCapacity(fieldNodes.length));

			for (AbstractMethodNode methodNode : methodNodes) {
				methodNames.add(methodNode.getOriginalName());
			}

			MultiMap<String, AbstractFieldNode> fieldsByOriginalName = new MultiMap<>(fieldNodes.length);

			for (AbstractFieldNode fieldNode : fieldNodes) {
				fieldsByOriginalName.put(fieldNode.getOriginalName(), fieldNode);
			}

			for (int paramIndex = 0; paramIndex < super.paramCount; paramIndex++) {
				AnnotationParameterNode param = super.params[paramIndex];
				if (param.getName().equals("d2")) {
					for (AnnotationElementNode data2Element : ((ArrayAnnotationElement)param.getElement()).getElements()) {
						if (data2Element instanceof StringAnnotationElement) {
							StringAnnotationElement data2StringElement = (StringAnnotationElement)data2Element;
							String data2Name = data2StringElement.getValue();
							List<AbstractFieldNode> matchingFields = fieldsByOriginalName.get(data2Name);
							if (matchingFields != null && !methodNames.contains(data2Name)) {
								boolean displayNamesMatch = true;
								String displayName = null;

								for (AbstractFieldNode candidateField : matchingFields) {
									if (displayName == null) {
										displayName = candidateField.getNameDisplay();
									} else if (!displayName.equals(candidateField.getNameDisplay())) {
										displayNamesMatch = false;
										break;
									}
								}

								if (displayNamesMatch && !data2Name.equals(displayName)) {
									data2StringElement.setValue(displayName);
								}
							}
						}
					}
				}
			}
		} else {
			for (int i = 0; i < super.paramCount; i++) {
				super.params[i].updateConstants();
			}
		}
	}

	@Override
	public String getTypeInternalName() {
		if (super.valid) {
			String internalName = super.typeDescUtf.getValue();
			return internalName.substring(1, internalName.length() - 1);
		} else {
			return null;
		}
	}

	@Override
	public AbstractClassNode getTypeClass() {
		return super.typeClass;
	}

	@Override
	public String getErrorMsg() {
		return super.getErrorMsg();
	}

	@Override
	public int getSize() {
		return super.getSize();
	}
}
