package com.zelix.framework.writer;

import com.zelix.enums.TypeTags;
import com.zelix.framework.AbstractClassNode;
import com.zelix.framework.AbstractFieldNode;
import com.zelix.framework.AbstractMethodNode;
import com.zelix.framework.signature.MethodSignature;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.wrappers.FieldNode;
import com.zelix.framework.wrappers.MethodNode;
import com.zelix.framework.writer.base.AbstractConstantPoolWriter;
import com.zelix.framework.writer.base.BaseDynamicCallWriter;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.repository.ClassProvider;
import com.zelix.utils.GlobalUtils;
import com.zelix.workspace.AbstractWorkspace;
import com.zelix.workspace.ClassHierarchyNode;
import com.zelix.workspace.PhantomClassManager;
import java.util.HashSet;
import java.util.Set;

public class DynamicConstantWriter extends BaseDynamicCallWriter {
	public static final TypeTags TYPE_TAG = TypeTags.CONSTANT_DYNAMIC;
	private AbstractFieldNode referencedField;

	public DynamicConstantWriter(int index, AbstractConstantPoolWriter constantPool, int bootstrapMethodIndex, NameAndTypeWriter target) {
		super(index, constantPool, bootstrapMethodIndex, target);
	}

	public void resolveReferencedClass(AbstractWorkspace workspace, ClassProvider classProvider, PhantomClassManager phantomClassManager) {
		super.target.getName();
		String typeDescriptor = MethodSignature.extractReturnDescriptor(super.target.getDescriptor());
		if (typeDescriptor.charAt(0) != '[' && !TypeWriter.isPrimitiveDescriptor(typeDescriptor)) {
			String internalName = typeDescriptor.substring(1, typeDescriptor.length() - 1);
			TypeWriter[] bootstrapArguments = this.bootstrapMethodEntry.getBootstrapArguments();
			HashSet<String> bootstrapArgumentDescriptors = GlobalUtils.newHashSet();

			for (int i = 0; i < bootstrapArguments.length; i++) {
				if (bootstrapArguments[i] instanceof MethodDescriptorWriter) {
					bootstrapArgumentDescriptors.add(((MethodDescriptorWriter)bootstrapArguments[i]).getDescriptor());
				} else if (bootstrapArguments[i] instanceof ResolvedMethodHandleWriter) {
					bootstrapArgumentDescriptors.add(((ResolvedMethodHandleWriter)bootstrapArguments[i]).getTargetMemberWriter().getDescriptor());
				}
			}

			AbstractClassNode constantPoolOwnerClassNode = this.getOwnerClassNode();
			Integer innerClassOrdinal = constantPoolOwnerClassNode.hasInnerClassOrdinal() ? constantPoolOwnerClassNode.getInnerClassOrdinal() : null;
			classProvider.getClassNode(internalName, innerClassOrdinal, "looking for class '" + GlobalUtils.internalToBinaryName(internalName) + "' which is referenced in class '" + this.getOwnerRawSourcePath() + "'");
		}
	}

	@Override
	public void collectWorkspaceDependencies(Set<ClassNode> referencedClasses, Set<ClassNode> signatureClasses, Set<FieldNode> referencedFields, Set<MethodNode> referencedMethods) {
		super.collectWorkspaceDependencies(referencedClasses, signatureClasses, referencedFields, referencedMethods);
		if (this.referencedField != null && this.referencedField.isWorkspaceNode()) {
			referencedFields.add((FieldNode)this.referencedField);
			AbstractClassNode fieldOwnerClassNode = this.referencedField.getClassNode();
			if (this.referencedField.isStatic()) {
				signatureClasses.add((ClassNode)fieldOwnerClassNode);
			} else {
				referencedClasses.add((ClassNode)fieldOwnerClassNode);
			}
		}
	}

	@Override
	public void collectDependencies(Set<AbstractClassNode> referencedClasses, Set<AbstractClassNode> signatureClasses, Set<AbstractFieldNode> referencedFields, Set<AbstractMethodNode> referencedMethods) {
		super.collectDependencies(referencedClasses, signatureClasses, referencedFields, referencedMethods);
		if (this.referencedField != null) {
			referencedFields.add(this.referencedField);
			AbstractClassNode fieldOwnerClassNode = this.referencedField.getClassNode();
			if (!ClassHierarchyNode.isMissingClass(fieldOwnerClassNode.getInternalName())) {
				if (this.referencedField.isStatic()) {
					signatureClasses.add((ClassNode)fieldOwnerClassNode);
				} else {
					referencedClasses.add(fieldOwnerClassNode);
				}
			}
		}
	}

	@Override
	public TypeTags getTypeTag() {
		return TYPE_TAG;
	}
}
