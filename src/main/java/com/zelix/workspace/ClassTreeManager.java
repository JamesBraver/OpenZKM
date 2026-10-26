package com.zelix.workspace;

import com.zelix.event.ChangeTrackingNotifier;
import com.zelix.exceptions.ProcessingException;
import com.zelix.framework.AbstractClassNode;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.klassmaster.config.Options;
import com.zelix.klassmaster.constant.CopyrightConstants;
import com.zelix.repository.ClassProvider;
import com.zelix.tools.trim.TrimExcludes;
import com.zelix.utils.DebugUtils;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.references.AtomicReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

public class ClassTreeManager extends ChangeTrackingNotifier {
	private boolean hasNonWorkspaceClasses = false;
	private List<ClassHierarchyNode> rootClasses;
	private List<ClassHierarchyNode> allClasses;
	private List<ClassHierarchyNode> rootInterfaces;
	private MultiMap<ClassHierarchyNode, ClassHierarchyNode> nestMembersByHost;
	private MultiMap<ClassHierarchyNode, ClassHierarchyNode> subclassesBySuperclass;
	private Set<ClassHierarchyNode> validInnerClassNodes;
	private Set<ClassHierarchyNode> sealedClassNodes;

	public ClassTreeManager(ClassNode[] classNodes) {
		this.rebuildClassTree(classNodes);
	}

	private boolean validateClassHierarchy(AbstractClassNode openedClass, AbstractClassNode currentClass, AtomicReference<String> errorMessage, ClassProvider classProvider, boolean isCurrentClassOpened) {
		if (currentClass.getInternalName().equals("java/lang/Object")) {
			return true;
		} else {
			String superclassName = currentClass.getSuperclassName();
			if (superclassName != null) {
				AbstractClassNode superclassNode = ClassHierarchyNode.getClassNodeByInternalName(superclassName);
				if (superclassNode != null) {
					if (superclassNode.isInterface()) {
						errorMessage.set("Class '" + currentClass.getSourcePath() + "' extends '" + superclassNode.getSourcePath() + "' but '" + superclassNode.getSimpleNameInternal() + "' is now an interface.");
						return false;
					}

					if (isCurrentClassOpened) {
						if (!this.validateClassHierarchy(superclassNode, superclassNode, errorMessage, classProvider, true)) {
							return false;
						}
					} else if (Options.ZKM_TEST_HIERARCHY) {
						errorMessage.set(this.buildNotOpenedMessage(superclassNode, "extended", currentClass, openedClass));
						return false;
					}
				} else {
					String superclassLookupContext = "looking for the superclass of '" + currentClass.getSourcePath() + "'";
					AbstractClassNode resolvedSuperclassNode = classProvider.getClassNode(superclassName, superclassLookupContext);
					if (!this.validateClassHierarchy(openedClass, resolvedSuperclassNode, errorMessage, classProvider, false)) {
						return false;
					}
				}
			}

			String[] interfaceNames = currentClass.getInterfaceNames();

			for (int i = 0; i < interfaceNames.length; i++) {
				String interfaceName = interfaceNames[i];
				AbstractClassNode interfaceNode = ClassHierarchyNode.getClassNodeByInternalName(interfaceName);
				if (interfaceNode != null) {
					if (!interfaceNode.isInterface()) {
						errorMessage.set(
							"Class '" + currentClass.getSourcePath() + "' implements '" + interfaceNode.getSourcePath() + "' but '" + interfaceNode.getSimpleNameInternal() + "' is no longer an interface."
						);
						return false;
					}

					if (isCurrentClassOpened) {
						if (!this.validateClassHierarchy(interfaceNode, interfaceNode, errorMessage, classProvider, true)) {
							return false;
						}
					} else if (Options.ZKM_TEST_HIERARCHY) {
						errorMessage.set(this.buildNotOpenedMessage(interfaceNode, "implemented", currentClass, openedClass));
						return false;
					}
				} else {
					String interfaceLookupContext = "looking for an implemented interface of '" + currentClass.getRawSourcePath() + "'";
					AbstractClassNode resolvedInterfaceNode = classProvider.getClassNode(interfaceName, interfaceLookupContext);
					if (!this.validateClassHierarchy(openedClass, resolvedInterfaceNode, errorMessage, classProvider, false)) {
						return false;
					}
				}
			}

			return true;
		}
	}

	private <T> void visitClassTree(ClassHierarchyVisitor<T> visitor, Enumeration<ClassHierarchyNode> nodes, int depth, T visitorArg) {
		if (nodes != null) {
			depth++;

			while (nodes.hasMoreElements()) {
				ClassHierarchyNode classNode = nodes.nextElement();
				visitor.visitNode(classNode, depth, visitorArg);
				this.visitClassTree(visitor, this.subclassesBySuperclass.getValuesEnumeration(classNode), depth, visitorArg);
			}
		}
	}

	private void collectAvailableClassRoots(ClassHierarchyNode classNode, List<ClassHierarchyNode> result) {
		if (classNode.isMissing()) {
			Enumeration<ClassHierarchyNode> subclassesEnumeration = classNode.getSubclassesEnumeration();
			if (subclassesEnumeration != null) {
				while (subclassesEnumeration.hasMoreElements()) {
					ClassHierarchyNode subclassNode = subclassesEnumeration.nextElement();
					this.collectAvailableClassRoots(subclassNode, result);
				}
			}
		} else {
			result.add(classNode);
		}
	}

	public Enumeration<ClassHierarchyNode> getAllClassesEnumeration() {
		return Collections.enumeration(this.allClasses);
	}

	public void trimNestMembers(TrimExcludes trimExcludes) {
		for (Entry<ClassHierarchyNode, List<ClassHierarchyNode>> nestMembersByHostEntry : this.nestMembersByHost.entrySet()) {
			ClassNode hostClass = nestMembersByHostEntry.getKey().getClassNodeIfWorkspace();
			if (hostClass != null) {
				hostClass.trimNestMembersAttribute(trimExcludes);
				if (hostClass.hasInnerClasses()) {
					Iterator<AbstractClassNode> hostInnerClassIterator = hostClass.getInnerClasses().iterator();

					while (hostInnerClassIterator.hasNext()) {
						((ClassNode)hostInnerClassIterator.next()).trimNestMembersAttribute(trimExcludes);
					}
				}
			}

			Iterator<ClassHierarchyNode> nestMemberIterator = nestMembersByHostEntry.getValue().iterator();

			while (nestMemberIterator.hasNext()) {
				ClassNode nestMemberClass = nestMemberIterator.next().getClassNodeIfWorkspace();
				if (nestMemberClass != null) {
					nestMemberClass.pruneNestHostAttribute(trimExcludes);
					if (nestMemberClass.hasInnerClasses()) {
						Iterator<AbstractClassNode> nestMemberInnerClassIterator = nestMemberClass.getInnerClasses().iterator();

						while (nestMemberInnerClassIterator.hasNext()) {
							((ClassNode)nestMemberInnerClassIterator.next()).pruneNestHostAttribute(trimExcludes);
						}
					}
				}
			}
		}
	}

	public void pruneExcludedInnerClasses(TrimExcludes trimExcludes) {
		Iterator<ClassHierarchyNode> validInnerClassIterator = this.validInnerClassNodes.iterator();

		while (validInnerClassIterator.hasNext()) {
			ClassNode classNode = validInnerClassIterator.next().getClassNodeIfWorkspace();
			if (classNode != null) {
				classNode.pruneExcludedInnerClasses(trimExcludes);
				if (classNode.hasInnerClasses()) {
					Iterator<AbstractClassNode> innerClassIterator = classNode.getInnerClasses().iterator();

					while (innerClassIterator.hasNext()) {
						((ClassNode)innerClassIterator.next()).pruneExcludedInnerClasses(trimExcludes);
					}
				}
			}
		}
	}

	public ClassHierarchyNode getClassByIndex(int index) {
		return this.allClasses.get(index);
	}

	public void clearValidInnerClassNodes() {
		this.validInnerClassNodes.clear();
	}

	public void rebuildClassTree(AbstractClassNode[] classNodes) {
		this.subclassesBySuperclass = new MultiMap<>();
		MultiMap<ClassHierarchyNode, ClassHierarchyNode> interfacesByClass = new MultiMap<>();
		this.rootClasses = new ArrayList<>();
		this.rootInterfaces = new ArrayList<>();
		this.validInnerClassNodes = GlobalUtils.newHashSet();
		this.nestMembersByHost = new MultiMap<>();
		this.sealedClassNodes = GlobalUtils.newHashSet();
		this.allClasses = new ArrayList<>();
		HashMap<String, AbstractClassNode> classesByInternalName = GlobalUtils.newHashMap();

		for (int classIndex = 0; classIndex < classNodes.length; classIndex++) {
			AbstractClassNode classNode = classNodes[classIndex];
			classesByInternalName.put(classNode.getInternalName(), classNode);
			if (!classNode.isWorkspaceNode()) {
				this.hasNonWorkspaceClasses = true;
			}
		}

		HashMap<AbstractClassNode, String> outerClassNameByInnerClass = GlobalUtils.newHashMap();
		ClassHierarchyNode.resetAll();
		HashMap<ClassHierarchyNode, ClassHierarchyNode> superclassByClassNode = GlobalUtils.newHashMap();

		for (int hierarchyIndex = 0; hierarchyIndex < classNodes.length; hierarchyIndex++) {
			ClassHierarchyNode classHierarchyNode = ClassHierarchyNode.getOrCreateNode(classNodes[hierarchyIndex].getInternalName(), classNodes[hierarchyIndex]);
			ClassHierarchyNode directSuperclassNode;
			if (!classNodes[hierarchyIndex].isModule()) {
				directSuperclassNode = ClassHierarchyNode.getOrCreateNode(classNodes[hierarchyIndex].getSuperclassName(), classesByInternalName.get(classNodes[hierarchyIndex].getSuperclassName()));
			} else {
				directSuperclassNode = ClassHierarchyNode.getOrCreateNode("java/lang/Object", classesByInternalName.get("java/lang/Object"));
			}

			classHierarchyNode.setOuterClass(directSuperclassNode);
			directSuperclassNode.addDirectSubclass(classHierarchyNode);
			this.subclassesBySuperclass.put(directSuperclassNode, classHierarchyNode);
			superclassByClassNode.put(classHierarchyNode, directSuperclassNode);
			if (classNodes[hierarchyIndex].getInterfacesCount() > 0) {
				for (int interfaceIndex = 0; interfaceIndex < classNodes[hierarchyIndex].getInterfacesCount(); interfaceIndex++) {
					String interfaceName = classNodes[hierarchyIndex].getInterfaceName(interfaceIndex);
					ClassHierarchyNode interfaceNode = ClassHierarchyNode.getOrCreateNode(interfaceName, classesByInternalName.get(interfaceName));
					interfacesByClass.put(classHierarchyNode, interfaceNode);
					classHierarchyNode.addDirectInterface(interfaceNode);
					interfaceNode.addDirectImplementer(classHierarchyNode);
				}
			}

			if (classNodes[hierarchyIndex].hasValidInnerClasses()) {
				this.validInnerClassNodes.add(classHierarchyNode);
			}

			if (classNodes[hierarchyIndex].isSealed()) {
				this.sealedClassNodes.add(classHierarchyNode);
			}

			if (classNodes[hierarchyIndex].hasInnerClassNode()) {
				String declaredOuterClassName = classNodes[hierarchyIndex].getOuterClassName();
				if (declaredOuterClassName != null) {
					outerClassNameByInnerClass.put(classNodes[hierarchyIndex], declaredOuterClassName);
				}
			}
		}

		for (AbstractClassNode nestMemberClassNode : classNodes) {
			String nestHostClassName = nestMemberClassNode.getNestHostClassName();
			if (nestHostClassName != null) {
				ClassHierarchyNode nestHostNode = ClassHierarchyNode.getNodeByInternalName(nestHostClassName);
				if (nestHostNode != null) {
					ClassHierarchyNode nestMemberNode = ClassHierarchyNode.getNodeByInternalName(nestMemberClassNode.getInternalName());
					this.nestMembersByHost.put(nestHostNode, nestMemberNode);
				}
			}
		}

		this.checkHierarchyLoops(classNodes);
		Enumeration<ClassHierarchyNode> superclassEnumeration = this.subclassesBySuperclass.keys();

		while (superclassEnumeration.hasMoreElements()) {
			ClassHierarchyNode superclassNode = superclassEnumeration.nextElement();
			if (!superclassByClassNode.containsKey(superclassNode)) {
				this.rootClasses.add(superclassNode);
			}

			Collections.sort(this.subclassesBySuperclass.get(superclassNode));
		}

		Collections.sort(this.rootClasses);

		for (int rootInterfaceIndex = 0; rootInterfaceIndex < classNodes.length; rootInterfaceIndex++) {
			if (classNodes[rootInterfaceIndex].isWorkspaceNode() && classNodes[rootInterfaceIndex].isInterface()) {
				ClassHierarchyNode candidateInterfaceNode = ClassHierarchyNode.getNodeByInternalName(classNodes[rootInterfaceIndex].getInternalName());
				List<ClassHierarchyNode> implementerNodes = interfacesByClass.get(candidateInterfaceNode);
				if (implementerNodes == null) {
					this.rootInterfaces.add(candidateInterfaceNode);
				} else {
					boolean workspaceImplementerFound = false;

					for (int implementerIndex = 0; implementerIndex < implementerNodes.size(); implementerIndex++) {
						if (implementerNodes.get(implementerIndex).isWorkspaceClass()) {
							workspaceImplementerFound = true;
							break;
						}
					}

					if (!workspaceImplementerFound) {
						this.rootInterfaces.add(candidateInterfaceNode);
					}
				}
			}
		}

		this.visitClassTree(new ClassHierarchyCollector(), Collections.enumeration(this.rootClasses), -1, this.allClasses);

		for (Entry<AbstractClassNode, String> outerClassNameEntry : outerClassNameByInnerClass.entrySet()) {
			AbstractClassNode innerClass = outerClassNameEntry.getKey();
			String outerClassName = outerClassNameEntry.getValue();
			ClassHierarchyNode innerClassNode = ClassHierarchyNode.getNodeByInternalName(innerClass.getInternalName());
			ClassHierarchyNode outerClassNode = ClassHierarchyNode.getNodeByInternalName(outerClassName);
			if (outerClassNode != null) {
				outerClassNode.addDirectInnerClass(innerClassNode);
				innerClassNode.setSuperClassNode(outerClassNode);
				String enclosingClassName = innerClassNode.getClassNode().getEnclosingClassName();
				if (enclosingClassName != null) {
					ClassHierarchyNode enclosingClassNode = ClassHierarchyNode.getNodeByInternalName(enclosingClassName);
					if (enclosingClassNode != null) {
						innerClassNode.setSuperClassLink(enclosingClassNode);
					} else {
						DebugUtils.noop(DebugUtils.getSimpleName(this) + " AZZERT DISABLED! " + innerClass.getSourceName());
					}
				}
			} else {
				innerClassNode.getClassNode().setHasInvalidInnerClasses(true);
				this.validInnerClassNodes.remove(innerClassNode);
			}
		}

		for (int workspaceIndex = 0; workspaceIndex < classNodes.length; workspaceIndex++) {
			if (classNodes[workspaceIndex].isWorkspaceNode()) {
				ClassNode workspaceClassNode = (ClassNode)classNodes[workspaceIndex];
				if (workspaceClassNode.hasInvalidInnerClasses()) {
					workspaceClassNode.removeAllInnerClassMetadata();
				}
			}
		}
	}

	private String buildNotOpenedMessage(AbstractClassNode openedSuperclass, String relation, AbstractClassNode notOpenedClass, AbstractClassNode openedSubclass) {
		return "Class '"
			+ openedSuperclass.getSourcePath()
			+ "' is "
			+ relation
			+ " by '"
			+ notOpenedClass.getSourcePath()
			+ "' which has not been opened inside "
			+ CopyrightConstants.NAME
			+ ". '"
			+ notOpenedClass.getBinaryName()
			+ "' is then extended or implemented by '"
			+ openedSubclass.getSourcePath()
			+ "'. There must be no gaps in the opened inheritance and implementation hierarchies. Either '"
			+ openedSuperclass.getBinaryName()
			+ "' should not be opened or '"
			+ notOpenedClass.getSourcePath()
			+ "' must also be opened for obfuscation. (A)";
	}

	public boolean validateOpenedHierarchy(AtomicReference<String> errorMessage, ClassProvider classProvider) {
		List<ClassHierarchyNode> availableClassRoots = this.getAvailableClassRoots();

		for (int i = 0; i < availableClassRoots.size(); i++) {
			AbstractClassNode classNode = availableClassRoots.get(i).getClassNode();
			if (!this.validateClassHierarchy(classNode, classNode, errorMessage, classProvider, true)) {
				return false;
			}
		}

		return true;
	}

	public void prunePermittedSubclasses(TrimExcludes trimExcludes) {
		Iterator<ClassHierarchyNode> sealedClassIterator = this.sealedClassNodes.iterator();

		while (sealedClassIterator.hasNext()) {
			ClassNode classNode = sealedClassIterator.next().getClassNodeIfWorkspace();
			if (classNode != null) {
				classNode.prunePermittedSubclasses(trimExcludes);
				if (classNode.hasInnerClasses()) {
					Iterator<AbstractClassNode> innerClassIterator = classNode.getInnerClasses().iterator();

					while (innerClassIterator.hasNext()) {
						((ClassNode)innerClassIterator.next()).prunePermittedSubclasses(trimExcludes);
					}
				}
			}
		}
	}

	public void removeInvalidInnerClassNodes() {
		Iterator<ClassHierarchyNode> validInnerClassIterator = this.validInnerClassNodes.iterator();

		while (validInnerClassIterator.hasNext()) {
			ClassNode classNode = validInnerClassIterator.next().getClassNodeIfWorkspace();
			if (classNode != null && !classNode.hasValidInnerClasses()) {
				validInnerClassIterator.remove();
			}
		}
	}

	public void rebuildRegistries() {
		if (this.validInnerClassNodes != null && this.validInnerClassNodes.size() > 0) {
			HashSet<ClassHierarchyNode> validInnerClassNodeCopy = GlobalUtils.newHashSet(this.validInnerClassNodes.size());

			for (ClassHierarchyNode validInnerClassNode : this.validInnerClassNodes) {
				validInnerClassNodeCopy.add(validInnerClassNode);
			}

			this.validInnerClassNodes = validInnerClassNodeCopy;
		}

		if (this.sealedClassNodes != null && this.sealedClassNodes.size() > 0) {
			HashSet<ClassHierarchyNode> sealedClassNodeCopy = GlobalUtils.newHashSet(this.sealedClassNodes.size());

			for (ClassHierarchyNode sealedClassNode : this.sealedClassNodes) {
				sealedClassNodeCopy.add(sealedClassNode);
			}

			this.sealedClassNodes = sealedClassNodeCopy;
		}

		if (this.nestMembersByHost != null && !this.nestMembersByHost.isEmpty()) {
			MultiMap<ClassHierarchyNode, ClassHierarchyNode> nestMembersByHostCopy = new MultiMap<>();

			for (Entry<ClassHierarchyNode, List<ClassHierarchyNode>> nestMembersByHostEntry : this.nestMembersByHost.entrySet()) {
				nestMembersByHostCopy.putAll(nestMembersByHostEntry.getKey(), nestMembersByHostEntry.getValue());
			}

			this.nestMembersByHost = nestMembersByHostCopy;
		}

		if (this.subclassesBySuperclass != null) {
			MultiMap<ClassHierarchyNode, ClassHierarchyNode> subclassesBySuperclassCopy = new MultiMap<>(this.subclassesBySuperclass.size());
			Enumeration<ClassHierarchyNode> allNodesEnumeration = ClassHierarchyNode.getAllNodesEnumeration();

			while (allNodesEnumeration.hasMoreElements()) {
				ClassHierarchyNode classHierarchyNode = allNodesEnumeration.nextElement();
				ClassHierarchyNode superclassNode = classHierarchyNode.getOuterClass();
				if (superclassNode != null) {
					subclassesBySuperclassCopy.put(superclassNode, classHierarchyNode);
				}
			}

			this.subclassesBySuperclass = subclassesBySuperclassCopy;
		}
	}

	private void checkHierarchyLoops(AbstractClassNode[] classNodes) {
		for (int classIndex = 0; classIndex < classNodes.length; classIndex++) {
			LinkedHashSet<AbstractClassNode> ancestorClasses = new LinkedHashSet<>();
			AbstractClassNode classNode = classNodes[classIndex];
			ancestorClasses.add(classNode);

			for (ClassHierarchyNode outerClassNode = ClassHierarchyNode.getNodeByInternalName(classNode.getInternalName()).getOuterClass();
				outerClassNode != null;
				outerClassNode = outerClassNode.getOuterClass()
			) {
				AbstractClassNode outerClass = outerClassNode.getClassNode();
				if (!ancestorClasses.add(outerClass)) {
					StringBuffer messageBuffer = new StringBuffer();
					messageBuffer.append("Class '" + classNode.getBinaryName() + "' has a loop in its inheritance hierarchy : ");
					Iterator<AbstractClassNode> ancestorIterator = ancestorClasses.iterator();

					while (ancestorIterator.hasNext()) {
						messageBuffer.append(ancestorIterator.next().getSourcePath());
						messageBuffer.append(", ");
					}

					messageBuffer.append(outerClass.getSourcePath());
					throw new ProcessingException(messageBuffer.toString());
				}
			}
		}

		for (int interfaceLoopIndex = 0; interfaceLoopIndex < classNodes.length; interfaceLoopIndex++) {
			LinkedHashSet<ClassHierarchyNode> visitedPath = new LinkedHashSet<>();
			AtomicReference<String> errorMessage = new AtomicReference<>();
			ClassHierarchyNode classHierarchyNode = ClassHierarchyNode.getNodeByInternalName(classNodes[interfaceLoopIndex].getInternalName());
			if (!this.checkInterfaceLoop(classHierarchyNode, visitedPath, errorMessage)) {
				throw new ProcessingException(errorMessage.get());
			}
		}
	}

	private boolean checkInterfaceLoop(ClassHierarchyNode classNode, LinkedHashSet<ClassHierarchyNode> visitedPath, AtomicReference<String> errorMessage) {
		if (visitedPath.add(classNode)) {
			Enumeration<ClassHierarchyNode> interfacesEnumeration = classNode.getInterfacesEnumeration();
			if (interfacesEnumeration != null) {
				while (interfacesEnumeration.hasMoreElements()) {
					ClassHierarchyNode interfaceNode = interfacesEnumeration.nextElement();
					if (!this.checkInterfaceLoop(interfaceNode, GlobalUtils.cloneLinkedHashSet(visitedPath), errorMessage)) {
						return false;
					}
				}
			}

			return true;
		} else {
			StringBuffer messageBuffer = new StringBuffer();
			messageBuffer.append("Loop in it implementation hierarchy : ");
			Iterator<ClassHierarchyNode> pathIterator = visitedPath.iterator();

			while (pathIterator.hasNext()) {
				AbstractClassNode visitedClassNode = pathIterator.next().getClassNode();
				messageBuffer.append(visitedClassNode.getSourcePath());
				messageBuffer.append(", ");
			}

			messageBuffer.append(classNode.getClassNode().getSourcePath());
			errorMessage.set(messageBuffer.toString());
			return false;
		}
	}

	public List<ClassHierarchyNode> getRootInterfaces() {
		return this.rootInterfaces;
	}

	public List<ClassHierarchyNode> getWorkspaceClassRoots() {
		ArrayList<ClassHierarchyNode> workspaceRoots = new ArrayList<>();

		for (int i = 0; i < this.rootClasses.size(); i++) {
			this.collectWorkspaceClassRoots(this.rootClasses.get(i), workspaceRoots);
		}

		return workspaceRoots;
	}

	public List<ClassHierarchyNode> getAvailableClassRoots() {
		ArrayList<ClassHierarchyNode> availableRoots = new ArrayList<>();

		for (int i = 0; i < this.rootClasses.size(); i++) {
			this.collectAvailableClassRoots(this.rootClasses.get(i), availableRoots);
		}

		return availableRoots;
	}

	private void collectWorkspaceClassRoots(ClassHierarchyNode classNode, List<ClassHierarchyNode> result) {
		if (!classNode.isWorkspaceClass()) {
			Enumeration<ClassHierarchyNode> subclassesEnumeration = classNode.getSubclassesEnumeration();
			if (subclassesEnumeration != null) {
				while (subclassesEnumeration.hasMoreElements()) {
					ClassHierarchyNode subclassNode = subclassesEnumeration.nextElement();
					this.collectWorkspaceClassRoots(subclassNode, result);
				}
			}
		} else {
			result.add(classNode);
		}
	}

	public Enumeration<ClassHierarchyNode> getValidInnerClassNodes() {
		return Collections.enumeration(this.validInnerClassNodes);
	}
}
