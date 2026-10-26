package com.zelix.framework.writer;

import com.zelix.enums.TypeTags;
import com.zelix.framework.AbstractClassNode;
import com.zelix.framework.AbstractFieldNode;
import com.zelix.framework.AbstractMethodNode;
import com.zelix.framework.MemberNode;
import com.zelix.framework.attributes.BootstrapMethodEntry;
import com.zelix.framework.signature.MethodSignature;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.wrappers.FieldNode;
import com.zelix.framework.wrappers.LibraryClassNode;
import com.zelix.framework.wrappers.LibraryMethodNode;
import com.zelix.framework.wrappers.MethodNode;
import com.zelix.framework.writer.base.AbstractConstantPoolWriter;
import com.zelix.framework.writer.base.AbstractMethodCallWriter;
import com.zelix.framework.writer.base.BaseDynamicCallWriter;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.klassmaster.config.Options;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.repository.ClassProvider;
import com.zelix.utils.GlobalUtils;
import com.zelix.workspace.AbstractWorkspace;
import com.zelix.workspace.ClassHierarchyNode;
import com.zelix.workspace.PhantomClassManager;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class InvokeDynamicCallWriter extends BaseDynamicCallWriter implements StringableConstant {
	public static final TypeTags TYPE_TAG = TypeTags.INVOKE_DYNAMIC;
	private AbstractMethodNode resolvedBootstrapTarget;
	private AbstractMethodNode lambdaParameterChangeTarget;

	public InvokeDynamicCallWriter(int index, NameAndTypeWriter target, InvokeDynamicCallWriter source) {
		super(index, target, source);
		this.resolvedBootstrapTarget = source.resolvedBootstrapTarget;
		this.lambdaParameterChangeTarget = source.lambdaParameterChangeTarget;
	}

	public InvokeDynamicCallWriter(int index, AbstractConstantPoolWriter constantPool, int bootstrapMethodIndex, NameAndTypeWriter target) {
		super(index, constantPool, bootstrapMethodIndex, target);
	}

	public InvokeDynamicCallWriter(int index, AbstractConstantPoolWriter constantPool, NameAndTypeWriter target, BootstrapMethodEntry bootstrapMethodEntry) {
		super(index, constantPool, target, bootstrapMethodEntry);
	}

	public void prependArgumentDescriptor(String argumentDescriptor) {
		String oldDescriptor = super.target.getDescriptor();
		String newDescriptor = '(' + argumentDescriptor + oldDescriptor.substring(oldDescriptor.indexOf(41));
		super.target.setDescriptor(newDescriptor);
	}

	public AbstractMethodNode getLambdaParamChangeTarget() {
		return this.lambdaParameterChangeTarget;
	}

	public AbstractMethodNode getResolvedBootstrapTarget() {
		return this.resolvedBootstrapTarget;
	}

	public void resolveBootstrapTargets(Set<MethodNode> bootstrapTargets, AbstractWorkspace workspace, ClassProvider classProvider, PhantomClassManager phantomClassManager) {
		String callSiteName = super.target.getName();
		String returnDescriptor = MethodSignature.extractReturnDescriptor(super.target.getDescriptor());
		if (this.bootstrapMethodEntry.getBootstrapMethodClassName().equals("java/lang/runtime/ObjectMethods")) {
			List<String> callSiteParameterDescriptors = MethodSignature.parseParameterDescriptors(super.target.getDescriptor());
			if (callSiteParameterDescriptors.size() >= 1) {
				String recordClassDescriptor = callSiteParameterDescriptors.get(0);
				if (recordClassDescriptor.startsWith("L") && recordClassDescriptor.endsWith(";")) {
					ClassNode recordClassNode = ClassHierarchyNode.getWorkspaceClassNode(recordClassDescriptor.substring(1, recordClassDescriptor.length() - 1));
					if (recordClassNode != null) {
						MethodSignature recordMethodSignature;
						if (callSiteName.equals("toString")) {
							recordMethodSignature = new MethodSignature(callSiteName, "()Ljava/lang/String;");
						} else if (callSiteName.equals("equals")) {
							recordMethodSignature = new MethodSignature(callSiteName, "(Ljava/lang/Object;)Z");
						} else if (callSiteName.equals("hashCode")) {
							recordMethodSignature = new MethodSignature(callSiteName, "()I");
						} else {
							recordMethodSignature = null;
						}

						this.resolvedBootstrapTarget = recordClassNode.findMethodNode(recordMethodSignature);
					}
				}
			}
		} else if (this.bootstrapMethodEntry.getBootstrapMethodClassName().equals("java/lang/runtime/SwitchBootstraps")) {
			MemberNode switchBootstrapMember = this.bootstrapMethodEntry.getBootstrapMethodHandle().getTargetMemberWriter().getTargetMember();
			if (switchBootstrapMember != null && switchBootstrapMember.isMethod()) {
				this.resolvedBootstrapTarget = (AbstractMethodNode)switchBootstrapMember;
			}
		} else if (this.bootstrapMethodEntry.getBootstrapMethodClassName().equals("java/lang/runtime/TemplateRuntime")) {
			MemberNode templateRuntimeBootstrapMember = this.bootstrapMethodEntry.getBootstrapMethodHandle().getTargetMemberWriter().getTargetMember();
			if (templateRuntimeBootstrapMember != null && templateRuntimeBootstrapMember.isMethod()) {
				this.resolvedBootstrapTarget = (AbstractMethodNode)templateRuntimeBootstrapMember;
			}
		} else if (this.bootstrapMethodEntry.getBootstrapMethodClassName().equals("java/lang/invoke/StringConcatFactory")) {
			MemberNode stringConcatBootstrapMember = this.bootstrapMethodEntry.getBootstrapMethodHandle().getTargetMemberWriter().getTargetMember();
			if (stringConcatBootstrapMember != null && stringConcatBootstrapMember.isMethod()) {
				this.resolvedBootstrapTarget = (AbstractMethodNode)stringConcatBootstrapMember;
			}
		} else {
			TypeWriter[] metafactoryBootstrapArguments = this.bootstrapMethodEntry.getBootstrapArguments();
			if (metafactoryBootstrapArguments.length == 3
				&& metafactoryBootstrapArguments[0] instanceof MethodDescriptorWriter
				&& metafactoryBootstrapArguments[1] instanceof ResolvedMethodHandleWriter
				&& metafactoryBootstrapArguments[2] instanceof MethodDescriptorWriter
				&& Options.ZKM_INDY_PARAM_CHANGING) {
				MemberNode implementationMethodMember = ((ResolvedMethodHandleWriter)metafactoryBootstrapArguments[1]).getTargetMemberWriter().getTargetMember();
				if (implementationMethodMember != null && implementationMethodMember.isMethod()) {
					AbstractMethodNode implementationMethod = (AbstractMethodNode)implementationMethodMember;
					if (implementationMethod.isWorkspaceNode()) {
						bootstrapTargets.add((MethodNode)implementationMethod);
						List<String> implementationParameterDescriptors = MethodSignature.parseParameterDescriptors(implementationMethod.getDescriptor());
						MemberNode bootstrapMethodMember = this.bootstrapMethodEntry.getBootstrapMethodHandle().getTargetMemberWriter().getTargetMember();
						if (bootstrapMethodMember != null) {
							AbstractClassNode bootstrapMethodClass = bootstrapMethodMember.getClassNode();
							if (bootstrapMethodClass != null && bootstrapMethodClass.getInternalName().equals("java/lang/invoke/LambdaMetafactory") && bootstrapMethodMember.getNameDisplay().equals("metafactory")) {
								implementationMethod.markLambdaMetaFactory();
								List<String> invokedTypeParameterDescriptors = MethodSignature.parseParameterDescriptors(super.target.getDescriptor());
								List<String> instantiatedTypeParameterDescriptors = MethodSignature.parseParameterDescriptors(((MethodDescriptorWriter)metafactoryBootstrapArguments[2]).getDescriptor());
								if (!implementationMethod.isInstanceMethod() || !invokedTypeParameterDescriptors.isEmpty()) {
									if (implementationMethod.isInstanceMethod()) {
										String removedReceiverDescriptor = invokedTypeParameterDescriptors.remove(0);
									}

									if (implementationParameterDescriptors.size() > invokedTypeParameterDescriptors.size()) {
									}

									int capturedParameterCount = implementationParameterDescriptors.size() - instantiatedTypeParameterDescriptors.size();
									int samParameterCount = implementationParameterDescriptors.size() - capturedParameterCount;
									implementationMethod.setLambdaSamParamCount(samParameterCount);
									this.lambdaParameterChangeTarget = implementationMethod;
								}
							}
						}
					}
				}
			}

			if (returnDescriptor.charAt(0) == '[' || TypeWriter.isPrimitiveDescriptor(returnDescriptor)) {
				return;
			}

			String returnTypeInternalName = returnDescriptor.substring(1, returnDescriptor.length() - 1);
			TypeWriter[] bootstrapArguments = this.bootstrapMethodEntry.getBootstrapArguments();
			HashSet<String> candidateMethodDescriptors = GlobalUtils.newHashSet();

			for (int i = 0; i < bootstrapArguments.length; i++) {
				if (bootstrapArguments[i] instanceof MethodDescriptorWriter) {
					candidateMethodDescriptors.add(((MethodDescriptorWriter)bootstrapArguments[i]).getDescriptor());
				} else if (bootstrapArguments[i] instanceof ResolvedMethodHandleWriter) {
					ResolvedMethodHandleWriter methodHandleWriter = (ResolvedMethodHandleWriter)bootstrapArguments[i];
					if (methodHandleWriter != null && methodHandleWriter.isMethodHandle()) {
						candidateMethodDescriptors.add(methodHandleWriter.getTargetMemberWriter().getDescriptor());
					}
				}
			}

			AbstractClassNode ownerClassNode = this.getOwnerClassNode();
			Integer innerClassOrdinal = ownerClassNode.hasInnerClassOrdinal() ? ownerClassNode.getInnerClassOrdinal() : null;
			AbstractClassNode returnTypeClassNode = classProvider.getClassNode(
				returnTypeInternalName, innerClassOrdinal, "looking for class '" + GlobalUtils.internalToBinaryName(returnTypeInternalName) + "' which is referenced in class '" + this.getOwnerRawSourcePath() + "'"
			);
			if (!candidateMethodDescriptors.isEmpty()) {
				for (String candidateDescriptor : candidateMethodDescriptors) {
					MethodSignature candidateMethodSignature = new MethodSignature(callSiteName, candidateDescriptor);
					if (MethodSignature.isHandleMethod(returnTypeInternalName, callSiteName, workspace) && this.getOwnerClassNode().isJava8OrNewer()) {
						LibraryClassNode methodHandleClassNode = (LibraryClassNode)classProvider.getClassNode(
							"java/lang/invoke/MethodHandle", innerClassOrdinal, "looking for class 'java/lang/invoke/MethodHandle' which is referenced in class '" + this.getOwnerRawSourcePath() + "'"
						);
						LibraryMethodNode stubMethod = methodHandleClassNode.getOrCreateStubMethod(candidateMethodSignature);
						this.resolvedBootstrapTarget = stubMethod;
						return;
					}

					String lookupMessage = "looking for method '"
						+ candidateMethodSignature.getSignature(null)
						+ "' in class '"
						+ GlobalUtils.internalToBinaryName(returnTypeInternalName)
						+ "' which is referenced in class '"
						+ this.getOwnerRawSourcePath()
						+ "'";
					this.resolvedBootstrapTarget = this.findMethodInHierarchy(returnTypeClassNode, candidateMethodSignature, innerClassOrdinal, lookupMessage, workspace, classProvider, phantomClassManager);
					if (this.resolvedBootstrapTarget != null) {
						if (this.resolvedBootstrapTarget.isWorkspaceNode()) {
							bootstrapTargets.add((MethodNode)this.resolvedBootstrapTarget);
						}
						break;
					}
				}
			}
		}
	}

	private void addLambdaReturnTypeSubclass(AbstractClassNode targetClass, Set<? super ClassNode> referencedClasses) {
		String returnDescriptor = MethodSignature.extractReturnDescriptor(super.target.getDescriptor());
		if (returnDescriptor.length() > 2 && returnDescriptor.charAt(returnDescriptor.length() - 1) == ';') {
			String returnTypeInternalName = returnDescriptor.substring(1, returnDescriptor.length() - 1);
			if (!returnTypeInternalName.equals(targetClass.getInternalName())) {
				ClassHierarchyNode returnTypeHierarchyNode = ClassHierarchyNode.getNodeByInternalName(returnTypeInternalName);
				if (returnTypeHierarchyNode != null && returnTypeHierarchyNode.isWorkspaceClass()) {
					ClassHierarchyNode targetHierarchyNode = ClassHierarchyNode.getNodeByInternalName(targetClass.getInternalName());
					if (targetHierarchyNode != null) {
						HashMap<ClassHierarchyNode, ClassHierarchyNode> descendantsMap = GlobalUtils.newHashMap(13);
						targetHierarchyNode.collectAllDescendants(descendantsMap);
						if (descendantsMap.containsKey(returnTypeHierarchyNode)) {
							referencedClasses.add(returnTypeHierarchyNode.getClassNodeIfWorkspace());
						}
					}
				}
			}
		}
	}

	public final void remapCallSiteName(ScriptContext scriptContext) {
		if (this.resolvedBootstrapTarget != null && !ClassHierarchyNode.isMissingClass(this.resolvedBootstrapTarget.getInternalName())) {
			String callSiteName = super.target.getName();
			String bootstrapTargetName = this.resolvedBootstrapTarget.getName();
			if (!callSiteName.equals(bootstrapTargetName)) {
				super.target.setName(bootstrapTargetName);
			}
		}
	}

	@Override
	public String getAsString() {
		return super.target.getAsString() + '~' + this.bootstrapMethodEntry.getUniqueID();
	}

	@Override
	public void collectWorkspaceDependencies(Set<ClassNode> referencedClasses, Set<ClassNode> signatureClasses, Set<FieldNode> referencedFields, Set<MethodNode> referencedMethods) {
		super.collectWorkspaceDependencies(referencedClasses, signatureClasses, referencedFields, referencedMethods);
		if (this.resolvedBootstrapTarget != null && this.resolvedBootstrapTarget.isWorkspaceNode()) {
			referencedMethods.add((MethodNode)this.resolvedBootstrapTarget);
			AbstractClassNode bootstrapTargetClassNode = this.resolvedBootstrapTarget.getClassNode();
			if (this.resolvedBootstrapTarget.isStatic()) {
				signatureClasses.add((ClassNode)bootstrapTargetClassNode);
			} else {
				referencedClasses.add((ClassNode)bootstrapTargetClassNode);
				this.addLambdaReturnTypeSubclass(bootstrapTargetClassNode, referencedClasses);
			}
		}
	}

	private AbstractMethodNode findMethodInHierarchy(
		AbstractClassNode classNode, MethodSignature methodSignature, Integer innerClassOrdinal, String lookupMessage, AbstractWorkspace workspace, ClassProvider classProvider, PhantomClassManager phantomClassManager
	) {
		HashSet<AbstractClassNode> seenClasses = GlobalUtils.newHashSet();
		classNode.getInternalName();
		AbstractClassNode currentClass = classNode;
		AbstractMethodNode foundMethod = null;

		while (foundMethod == null && currentClass != null) {
			Integer currentInnerClassOrdinal = currentClass.hasInnerClassOrdinal() ? currentClass.getInnerClassOrdinal() : innerClassOrdinal;
			foundMethod = AbstractMethodCallWriter.findMethodInClass(currentClass, methodSignature, workspace);
			seenClasses.add(currentClass);
			String superclassName = currentClass.getSuperclassName();
			if (superclassName == null) {
				break;
			}

			currentClass = classProvider.getClassNode(superclassName, currentInnerClassOrdinal, lookupMessage, phantomClassManager);
		}

		if (foundMethod == null) {
			currentClass = classNode;

			while (foundMethod == null && currentClass != null) {
				foundMethod = AbstractMethodCallWriter.findMethodInInterfaces(currentClass, methodSignature, innerClassOrdinal, workspace, classProvider, seenClasses, lookupMessage, phantomClassManager);
				if (foundMethod == null) {
					String nextSuperclassName = currentClass.getSuperclassName();
					if (nextSuperclassName == null) {
						break;
					}

					Integer nextInnerClassOrdinal = currentClass.hasInnerClassOrdinal() ? currentClass.getInnerClassOrdinal() : innerClassOrdinal;
					currentClass = classProvider.getClassNode(nextSuperclassName, nextInnerClassOrdinal, lookupMessage, phantomClassManager);
				}
			}
		}

		return foundMethod;
	}

	@Override
	public void collectDependencies(Set<AbstractClassNode> referencedClasses, Set<AbstractClassNode> signatureClasses, Set<AbstractFieldNode> referencedFields, Set<AbstractMethodNode> referencedMethods) {
		super.collectDependencies(referencedClasses, signatureClasses, referencedFields, referencedMethods);
		if (this.resolvedBootstrapTarget != null) {
			referencedMethods.add(this.resolvedBootstrapTarget);
			AbstractClassNode bootstrapTargetClassNode = this.resolvedBootstrapTarget.getClassNode();
			if (!ClassHierarchyNode.isMissingClass(bootstrapTargetClassNode.getInternalName())) {
				if (this.resolvedBootstrapTarget.isStatic()) {
					signatureClasses.add((ClassNode)bootstrapTargetClassNode);
				} else {
					referencedClasses.add(bootstrapTargetClassNode);
					this.addLambdaReturnTypeSubclass(bootstrapTargetClassNode, referencedClasses);
				}
			}
		}
	}

	@Override
	public TypeTags getTypeTag() {
		return TYPE_TAG;
	}
}
