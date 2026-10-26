package com.zelix.framework.wrappers;

import com.zelix.exceptions.ProcessingException;
import com.zelix.framework.AbstractClassNode;
import com.zelix.framework.AbstractFieldNode;
import com.zelix.framework.AbstractMethodNode;
import com.zelix.framework.AttributesNode;
import com.zelix.framework.StructureNode;
import com.zelix.framework.attributes.UnknownAttribute;
import com.zelix.framework.attributes.code.CodeAttribute;
import com.zelix.framework.attributes.code.RawCodeAttribute;
import com.zelix.framework.attributes.declaration.ExceptionsAttribute;
import com.zelix.framework.code.InstructionListNode;
import com.zelix.framework.code.MethodMapping;
import com.zelix.framework.constant.ConstantPoolVisitor;
import com.zelix.framework.listener.ReferenceUpdateListener;
import com.zelix.framework.listener.SimpleReferenceListener;
import com.zelix.framework.signature.MethodSignature;
import com.zelix.framework.writer.AbstractNamedWriter;
import com.zelix.framework.writer.ClassInfoWriter;
import com.zelix.framework.writer.NameAndTypeWriter;
import com.zelix.framework.writer.UTFStringWriter;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.klassmaster.config.TrimConfig;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.tools.trim.TrimExcludes;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.collections.ThreeLevelMultiMap;
import com.zelix.utils.dataset.ByteDataBuffer;
import com.zelix.workspace.ClassHierarchyNode;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;

public class LibraryMethodNode extends AbstractMethodNode {
	private static int NO_ATTRIBUTE_INDEX = -1;
	private int libraryCodeAttrIndex = NO_ATTRIBUTE_INDEX;

	public LibraryMethodNode(
		StructureNode parent,
		ByteDataBuffer buf,
		MultiMap<UTFStringWriter, SimpleReferenceListener> utf8References,
		MultiMap<NameAndTypeWriter, AbstractNamedWriter> nameAndTypeReferences,
		MultiMap<ClassInfoWriter, ReferenceUpdateListener> classReferences,
		PrintWriter printWriter,
		ThreeLevelMultiMap<String, String, UnknownAttribute, Object> unknownAttributes
	) throws IOException {
		super(parent, buf, utf8References);
		this.parameterDescriptors = TypeWriter.parseParameterDescriptors(this.descriptor, true);
		this.attributes = new AttributesNode[this.attributeCount];

		for (int i = 0; i < this.attributes.length; i++) {
			this.attributes[i] = AttributesNode.readLibraryAttribute(this, buf, utf8References, nameAndTypeReferences, classReferences, printWriter, unknownAttributes);
			if (this.attributes[i] instanceof RawCodeAttribute) {
				this.libraryCodeAttrIndex = i;
			} else if (this.attributes[i] instanceof ExceptionsAttribute) {
				this.exceptionsAttrIndex = i;
			}
		}

		if (this.parameterDescriptors != null && TypeWriter.descriptorToHumanReadable(this.descriptor, true) != null) {
			this.setValid(true);
		} else {
			this.setValid(false);
		}
	}

	public LibraryMethodNode(AbstractClassNode owner, UTFStringWriter nameWriter, UTFStringWriter descriptorWriter, AttributesNode[] attributes, boolean isSyntheticLibraryStub, int generationTypeFlag) {
		super(owner, nameWriter, descriptorWriter, attributes, generationTypeFlag);

		for (int i = 0; i < attributes.length; i++) {
			if (attributes[i] instanceof CodeAttribute) {
				this.libraryCodeAttrIndex = i;
			} else if (attributes[i] instanceof ExceptionsAttribute) {
				this.exceptionsAttrIndex = i;
			}

			attributes[i].setParent(this);
		}

		this.parameterDescriptors = TypeWriter.parseParameterDescriptors(this.descriptor);
		super.isSyntheticLibraryStub = isSyntheticLibraryStub;
	}

	public void applyMappedMethodName(Map<MethodSignature, MethodSignature> mappedSignatures, MethodMapping methodMapping) {
		MethodSignature methodSignature = this.getMethodSignature();
		MethodSignature mappedSignature = mappedSignatures.get(methodSignature);
		if (mappedSignature != null && !mappedSignature.getName().equals(this.getName())) {
			this.setName(mappedSignature.getName());
			methodMapping.putNewPair(this, methodSignature.getName(), mappedSignature.getName());
		} else {
			methodMapping.putNewPair(this, this.getNameDisplay(), this.getNameDisplay());
		}
	}

	@Override
	public void collectClassDependencies(Set<AbstractClassNode> referencedClasses, Set<AbstractClassNode> signatureClasses, Set<AbstractFieldNode> referencedFields, Set<AbstractMethodNode> referencedMethods) {
		if (referencedMethods.add(this)) {
			if (this.libraryCodeAttrIndex != NO_ATTRIBUTE_INDEX) {
				CodeAttribute codeAttribute;
				if (this.attributes[this.libraryCodeAttrIndex] instanceof RawCodeAttribute) {
					try {
						codeAttribute = new CodeAttribute((RawCodeAttribute)this.attributes[this.libraryCodeAttrIndex]);
						this.attributes[this.libraryCodeAttrIndex] = codeAttribute;
					} catch (IOException ioException) {
						throw new ProcessingException(
							"Error reading method '" + this.getDisplayName() + "' in file '" + this.getRawSourcePath() + "' : " + ioException.getMessage(), ioException
						);
					}
				} else {
					codeAttribute = (CodeAttribute)this.attributes[this.libraryCodeAttrIndex];
				}

				codeAttribute.collectClassDependencies(referencedClasses, signatureClasses, referencedFields, referencedMethods);
			}

			if (this.exceptionsAttrIndex != NO_ATTRIBUTE_INDEX) {
				Enumeration<String> exceptionNames = ((ExceptionsAttribute)this.attributes[this.exceptionsAttrIndex]).getExceptionInternalNames();

				while (exceptionNames.hasMoreElements()) {
					AbstractClassNode exceptionClassNode = ClassHierarchyNode.getClassNodeByInternalName(exceptionNames.nextElement());
					if (exceptionClassNode != null) {
						signatureClasses.add(exceptionClassNode);
					}
				}
			}
		}
	}

	@Override
	public InstructionListNode getInstructionList() {
		if (this.libraryCodeAttrIndex != NO_ATTRIBUTE_INDEX) {
			CodeAttribute codeAttribute;
			if (this.attributes[this.libraryCodeAttrIndex] instanceof RawCodeAttribute) {
				try {
					codeAttribute = new CodeAttribute((RawCodeAttribute)this.attributes[this.libraryCodeAttrIndex]);
				} catch (IOException ioException) {
					throw new ProcessingException(
						"Error reading method '" + this.getDisplayName() + "' in file '" + this.getRawSourcePath() + "' : " + ioException.getMessage(), ioException
					);
				}

				this.attributes[this.libraryCodeAttrIndex] = codeAttribute;
			} else {
				codeAttribute = (CodeAttribute)this.attributes[this.libraryCodeAttrIndex];
			}

			return codeAttribute.getInstructionList();
		} else {
			return null;
		}
	}

	@Override
	public void acceptVisitor(ConstantPoolVisitor visitor) {
	}

	@Override
	public void collectInnerClassMethodCalls(Set<AbstractMethodNode> collectedMethods, Set<AbstractClassNode> innerClasses) {
		if (collectedMethods.add(this) && this.libraryCodeAttrIndex != NO_ATTRIBUTE_INDEX) {
			CodeAttribute codeAttribute;
			if (this.attributes[this.libraryCodeAttrIndex] instanceof RawCodeAttribute) {
				try {
					codeAttribute = new CodeAttribute((RawCodeAttribute)this.attributes[this.libraryCodeAttrIndex]);
					this.attributes[this.libraryCodeAttrIndex] = codeAttribute;
				} catch (IOException ioException) {
					throw new ProcessingException(
						"Error reading method '" + this.getDisplayName() + "' in file '" + this.getRawSourcePath() + "' : " + ioException.getMessage(), ioException
					);
				}
			} else {
				codeAttribute = (CodeAttribute)this.attributes[this.libraryCodeAttrIndex];
			}

			codeAttribute.collectInnerClassMethodCalls(collectedMethods, innerClasses);
		}
	}

	@Override
	public void filterCodeAttribute(TrimExcludes trimExcludes, TrimConfig trimConfig, AttributesNode attribute, ScriptContext scriptContext, PrintWriter printWriter) {
	}

	@Override
	public final boolean isWorkspaceNode() {
		return false;
	}
}
