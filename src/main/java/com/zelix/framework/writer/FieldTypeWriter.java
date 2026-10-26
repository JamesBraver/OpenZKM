package com.zelix.framework.writer;

import com.zelix.enums.TypeTags;
import com.zelix.framework.AbstractClassNode;
import com.zelix.framework.AbstractFieldNode;
import com.zelix.framework.listener.ReferenceUpdateListener;
import com.zelix.framework.signature.FieldSignature;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.writer.base.AbstractConstantPoolWriter;
import com.zelix.framework.writer.base.AbstractMemberRefInfoWriter;
import com.zelix.framework.writer.base.MemberTypeWriter;
import com.zelix.repository.ClassProvider;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.references.AtomicReference;
import com.zelix.workspace.AbstractWorkspace;
import com.zelix.workspace.ClassHierarchyNode;
import com.zelix.workspace.PhantomClassManager;
import java.util.HashSet;
import java.util.Set;

public class FieldTypeWriter extends MemberTypeWriter {
	public static final TypeTags TYPE_TAG = TypeTags.FIELDREF;

	public FieldTypeWriter(
		int index, AbstractConstantPoolWriter constantPool, ClassInfoWriter owner, NameAndTypeWriter target, AbstractWorkspace workspace, ClassProvider classProvider, boolean resolveTarget
	) {
		super(index, constantPool, owner, target, workspace, classProvider, resolveTarget);
	}

	public FieldTypeWriter(
		AbstractMemberRefInfoWriter memberRefInfoWriter, ClassInfoWriter owner, NameAndTypeWriter target, MultiMap<ClassInfoWriter, ReferenceUpdateListener> classReferences
	) {
		super(memberRefInfoWriter, owner, target, classReferences);
	}

	public FieldTypeWriter(int index, AbstractConstantPoolWriter constantPool, ClassInfoWriter owner, NameAndTypeWriter target, AbstractFieldNode targetMember) {
		super(index, constantPool, owner, target, targetMember);
	}

	public String getStackType() {
		String descriptor = this.target.getDescriptor();
		return !descriptor.equals("B") && !descriptor.equals("C") && !descriptor.equals("S") && !descriptor.equals("Z") ? descriptor : "I";
	}

	public ClassNode getFieldTypeClassNode() {
		String elementInternalName = AbstractClassNode.getArrayElementInternalName(this.getDescriptor());
		return elementInternalName != null ? ClassHierarchyNode.getWorkspaceClassNode(elementInternalName) : null;
	}

	public AbstractFieldNode findFieldInHierarchy(
		AbstractWorkspace workspace,
		ClassProvider classProvider,
		AtomicReference<String> searchedClassName,
		AtomicReference<AbstractClassNode> ownerClass,
		AtomicReference<Set<AbstractClassNode>> searchedClasses,
		PhantomClassManager phantomClassManager
	) {
		AbstractFieldNode fieldNode = null;
		FieldSignature fieldSignature = new FieldSignature(this.getName(), this.getDescriptor());
		String ownerName = this.getOwnerName();
		String currentClassName = ownerName;
		AbstractClassNode ownerClassNode = null;
		HashSet<AbstractClassNode> searchedClassNodes = null;
		if (ownerName.startsWith("[")) {
			currentClassName = "java/lang/Object";
		}

		String lookupMessage = "looking for field '"
			+ fieldSignature.toRemappedString(null)
			+ "' in class '"
			+ GlobalUtils.internalToBinaryName(ownerName)
			+ "' which is referenced in class '"
			+ this.getOwnerRawSourcePath()
			+ "'";
		AbstractClassNode constantPoolOwnerClassNode = this.constantPool.getOwnerClassNode();
		Integer innerClassOrdinal = constantPoolOwnerClassNode.hasInnerClassOrdinal() ? constantPoolOwnerClassNode.getInnerClassOrdinal() : null;
		AbstractClassNode searchedClassNode = classProvider.getClassNode(currentClassName, innerClassOrdinal, lookupMessage, phantomClassManager);

		while (fieldNode == null && searchedClassNode != null) {
			fieldNode = this.findFieldInClass(searchedClassNode, fieldSignature, workspace);
			if (ownerClassNode == null && fieldNode == null) {
				ownerClassNode = searchedClassNode;
				searchedClassNodes = GlobalUtils.newHashSet();
			}

			if (fieldNode != null) {
				break;
			}

			searchedClassNodes.add(searchedClassNode);
			fieldNode = this.findFieldInInterfaces(fieldSignature, searchedClassNode, innerClassOrdinal, searchedClassNodes, workspace, classProvider, lookupMessage, phantomClassManager);
			if (fieldNode != null) {
				break;
			}

			currentClassName = searchedClassNode.getSuperclassName();
			if (currentClassName == null) {
				break;
			}

			Integer nextInnerClassOrdinal = searchedClassNode.hasInnerClassOrdinal() ? searchedClassNode.getInnerClassOrdinal() : innerClassOrdinal;
			searchedClassNode = classProvider.getClassNode(currentClassName, nextInnerClassOrdinal, lookupMessage, phantomClassManager);
		}

		searchedClassName.set(currentClassName);
		ownerClass.set(ownerClassNode);
		searchedClasses.set(searchedClassNodes);
		return fieldNode;
	}

	public AbstractFieldNode findFieldInClass(AbstractClassNode classNode, FieldSignature fieldSignature, AbstractWorkspace workspace) {
		return (AbstractFieldNode)(classNode.isWorkspaceNode() && !classNode.isInnerClass() ? workspace.getFieldBySignatureAndClass((ClassNode)classNode, fieldSignature) : classNode.findField(fieldSignature));
	}

	public final AbstractFieldNode findFieldInInterfaces(
		FieldSignature fieldSignature,
		AbstractClassNode classNode,
		Integer innerClassOrdinal,
		HashSet<AbstractClassNode> searchedClasses,
		AbstractWorkspace workspace,
		ClassProvider classProvider,
		String lookupMessage,
		PhantomClassManager phantomClassManager
	) {
		String[] interfaceNames = classNode.getInterfaceNames();
		Integer interfaceInnerClassOrdinal = classNode.hasInnerClassOrdinal() ? classNode.getInnerClassOrdinal() : innerClassOrdinal;

		for (int i = 0; i < interfaceNames.length; i++) {
			AbstractClassNode interfaceClassNode = classProvider.getClassNode(interfaceNames[i], interfaceInnerClassOrdinal, lookupMessage, phantomClassManager);
			if (interfaceClassNode != null) {
				AbstractFieldNode fieldNode = this.findFieldInClass(interfaceClassNode, fieldSignature, workspace);
				if (fieldNode != null) {
					return fieldNode;
				}

				if (searchedClasses.add(interfaceClassNode)) {
					fieldNode = this.findFieldInInterfaces(fieldSignature, interfaceClassNode, innerClassOrdinal, searchedClasses, workspace, classProvider, lookupMessage, phantomClassManager);
					if (fieldNode != null) {
						return fieldNode;
					}
				}
			}
		}

		return null;
	}

	@Override
	public void resolveTargetMember(AbstractWorkspace workspace, ClassProvider classProvider, PhantomClassManager phantomClassManager) {
		AtomicReference<String> searchedClassNameRef = new AtomicReference<>();
		AtomicReference<AbstractClassNode> ownerClassRef = new AtomicReference<>();
		AtomicReference<Set<AbstractClassNode>> searchedClassesRef = new AtomicReference<>();
		this.targetMember = this.findFieldInHierarchy(workspace, classProvider, searchedClassNameRef, ownerClassRef, searchedClassesRef, phantomClassManager);
		AbstractClassNode ownerClassNode = ownerClassRef.get();
		String searchedClassName = searchedClassNameRef.get();
		Set<AbstractClassNode> searchedClassNodes = searchedClassesRef.get();
		if (searchedClassNodes != null && this.targetMember != null) {
			for (AbstractClassNode classNode : searchedClassNodes) {
				if (classNode.isWorkspaceNode()) {
					((ClassNode)classNode).addFieldClassReference((AbstractFieldNode)this.targetMember, ownerClassNode);
				}
			}
		}
	}

	@Override
	public TypeTags getTypeTag() {
		return TYPE_TAG;
	}
}
