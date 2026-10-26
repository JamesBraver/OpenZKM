package com.zelix.tools.trim;

import com.zelix.exceptions.ClasspathException;
import com.zelix.exceptions.FileException;
import com.zelix.exceptions.TrimClassNotFoundException;
import com.zelix.framework.AbstractClassNode;
import com.zelix.framework.signature.MethodSignature;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.wrappers.LibraryClassNode;
import com.zelix.framework.wrappers.LibraryMethodNode;
import com.zelix.framework.wrappers.MethodNode;
import com.zelix.repository.ClassRepository;
import com.zelix.utils.DebugUtils;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.collections.DualKeyMap;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.enumerations.EmptyEnumeration;
import com.zelix.workspace.ClassHierarchyNode;
import com.zelix.workspace.ClassTreeManager;
import com.zelix.workspace.Workspace;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

public class CommonSuperClassAnalyzer {
	private HashSet<MethodNode> libraryOverridingMethods = GlobalUtils.newHashSet();
	private DualKeyMap<MethodNode, MethodNode, MethodNode> overriddenMethods = new DualKeyMap<>();
	private DualKeyMap<MethodNode, MethodNode, MethodNode> overridingMethods = new DualKeyMap<>();
	private Workspace workspace;
	private ClassRepository classRepository;

	public CommonSuperClassAnalyzer(Workspace workspace, ClassTreeManager classTreeManager, ClassRepository classRepository) {
		this.workspace = workspace;
		this.classRepository = classRepository;
		MultiMap<LibraryClassNode, MethodSignature> signaturesByClass = new MultiMap<>();
		List<ClassHierarchyNode> workspaceClassRoots = classTreeManager.getWorkspaceClassRoots();

		for (int i = 0; i < workspaceClassRoots.size(); i++) {
			ClassHierarchyNode classRoot = workspaceClassRoots.get(i);
			HashSet<String> processedClassNames = GlobalUtils.newHashSet();
			HashSet<MethodSignature> librarySignatures = GlobalUtils.newHashSet();
			DualKeyMap<MethodSignature, MethodNode, MethodNode> methodsBySignature = new DualKeyMap<>();
			this.analyzeHierarchyNode(classRoot, librarySignatures, methodsBySignature, processedClassNames, signaturesByClass, false);
		}

		Enumeration<MethodNode> overridingMethodEnumeration = this.overriddenMethods.getGroupKeys();

		while (overridingMethodEnumeration.hasMoreElements()) {
			MethodNode overridingMethod = overridingMethodEnumeration.nextElement();

			for (MethodNode overriddenMethod : this.overriddenMethods.getGroup(overridingMethod).keySet()) {
				this.overridingMethods.put(overriddenMethod, overridingMethod, overridingMethod);
			}
		}
	}

	private void addOuterImplementations(ClassNode classNode, Iterator<MethodSignature> signatures) {
		while (signatures.hasNext()) {
			MethodSignature signature = signatures.next();
			if (!this.workspace.containsMethodSignature(signature, classNode)) {
				for (ClassHierarchyNode outerClassNode = ClassHierarchyNode.getNodeByInternalName(classNode.getInternalName()).getOuterClass();
					outerClassNode.isWorkspaceClass();
					outerClassNode = outerClassNode.getOuterClass()
				) {
					ClassNode outerClass = outerClassNode.getClassNodeIfWorkspace();
					if (this.workspace.containsMethodSignature(signature, outerClass)) {
						MethodNode method = this.workspace.getMethodBySignature(outerClass, signature);
						if (!method.isAbstract()) {
							this.libraryOverridingMethods.add(method);
						}
						break;
					}
				}
			}
		}
	}

	public Enumeration<MethodNode> getOverridingMethods(MethodNode method) {
		Map<MethodNode, MethodNode> overridingMethodMap = this.overridingMethods.getGroup(method);
		return (Enumeration<MethodNode>)(overridingMethodMap != null ? Collections.enumeration(overridingMethodMap.keySet()) : new EmptyEnumeration<>());
	}

	private LibraryClassNode loadLibraryClass(String className, String reason) {
		try {
			return this.classRepository.loadClass(className, true, reason);
		} catch (ClasspathException classpathException) {
			throw new TrimClassNotFoundException("Class '" + GlobalUtils.internalToBinaryName(classpathException.getClassName()) + "' not found while " + reason + ". (2)");
		} catch (FileException fileException) {
			throw new TrimClassNotFoundException(fileException.getMessage());
		}
	}

	private void analyzeHierarchyNode(
		ClassHierarchyNode hierarchyNode,
		HashSet<MethodSignature> librarySignatures,
		DualKeyMap<MethodSignature, MethodNode, MethodNode> methodsBySignature,
		HashSet<String> processedClassNames,
		MultiMap<LibraryClassNode, MethodSignature> signaturesByClass,
		boolean isImplementer
	) {
		ClassNode classNode = hierarchyNode.getClassNodeIfWorkspace();
		this.addLibraryAncestorSignatures(classNode, librarySignatures, processedClassNames, signaturesByClass);
		if (isImplementer) {
			Enumeration<MethodSignature> signatureEnumeration = methodsBySignature.getGroupKeys();

			while (signatureEnumeration.hasMoreElements()) {
				MethodSignature signature = signatureEnumeration.nextElement();
				if (!this.workspace.containsMethodSignature(signature, classNode)) {
					for (ClassHierarchyNode outerClassNode = hierarchyNode.getOuterClass(); outerClassNode.isWorkspaceClass(); outerClassNode = outerClassNode.getOuterClass()) {
						ClassNode outerClass = outerClassNode.getClassNodeIfWorkspace();
						if (this.workspace.containsMethodSignature(signature, outerClass)) {
							MethodNode outerClassMethod = this.workspace.getMethodBySignature(outerClass, signature);
							if (!outerClassMethod.isAbstract()) {
								for (MethodNode interfaceMethod : methodsBySignature.getGroup(signature).keySet()) {
									this.overriddenMethods.put(outerClassMethod, interfaceMethod, interfaceMethod);
								}
							}
							break;
						}
					}
				}
			}

			this.addOuterImplementations(classNode, librarySignatures.iterator());
		}

		MethodNode[] methodNodes = classNode.getMethodNodes();

		for (int i = 0; i < methodNodes.length; i++) {
			MethodNode method = methodNodes[i];
			if (!method.isStatic() && !method.isPrivate() && !method.isConstructor()) {
				MethodSignature methodSignature = method.getMethodSignature();
				if (librarySignatures.contains(methodSignature)) {
					this.libraryOverridingMethods.add(method);
				}

				if (methodsBySignature.containsGroup(methodSignature)) {
					for (MethodNode overriddenMethod : methodsBySignature.getGroup(methodSignature).keySet()) {
						this.overriddenMethods.put(method, overriddenMethod, overriddenMethod);
					}
				}

				methodsBySignature.put(methodSignature, method, method);
			}
		}

		Enumeration<ClassHierarchyNode> subclassEnumeration = hierarchyNode.getSubclassesEnumeration();
		if (subclassEnumeration != null) {
			while (subclassEnumeration.hasMoreElements()) {
				ClassHierarchyNode subclassNode = subclassEnumeration.nextElement();
				this.analyzeHierarchyNode(subclassNode, GlobalUtils.clone(librarySignatures), GlobalUtils.clone(methodsBySignature), GlobalUtils.clone(processedClassNames), signaturesByClass, false);
			}
		}

		Enumeration<ClassHierarchyNode> implementerEnumeration = hierarchyNode.getImplementersEnumeration();
		if (implementerEnumeration != null) {
			while (implementerEnumeration.hasMoreElements()) {
				ClassHierarchyNode implementerNode = implementerEnumeration.nextElement();
				DebugUtils.assertTrue(hierarchyNode.isWorkspaceClass(), new String[]{"(1) " + hierarchyNode.getInternalName() + " " + implementerNode.getInternalName()});
				DebugUtils.assertTrue(implementerNode.isWorkspaceClass(), new String[]{"(2) " + hierarchyNode.getInternalName() + " " + implementerNode.getInternalName()});
				boolean concreteImplementer = hierarchyNode.isInterface() && !implementerNode.isInterface();
				this.analyzeHierarchyNode(implementerNode, GlobalUtils.clone(librarySignatures), GlobalUtils.clone(methodsBySignature), GlobalUtils.clone(processedClassNames), signaturesByClass, concreteImplementer);
			}
		}
	}

	public MethodNode[] getLibraryOverridingMethods() {
		MethodNode[] methodArray = new MethodNode[this.libraryOverridingMethods.size()];
		return this.libraryOverridingMethods.toArray(methodArray);
	}

	private void addLibraryMethodSignatures(LibraryClassNode libraryClass, HashSet<MethodSignature> signatures, MultiMap<LibraryClassNode, MethodSignature> signaturesByClass) {
		Vector<MethodSignature> methodSignatures = (Vector<MethodSignature>)signaturesByClass.get(libraryClass);
		if (methodSignatures == null) {
			methodSignatures = new Vector<>();
			LibraryMethodNode[] methodNodes = libraryClass.getMethodNodes();

			for (int i = 0; i < methodNodes.length; i++) {
				LibraryMethodNode method = methodNodes[i];
				if (!method.isStatic() && !method.isPrivate() && !method.isConstructor()) {
					methodSignatures.add(method.getMethodSignature());
				}
			}

			signaturesByClass.put(libraryClass, methodSignatures);
		}

		signatures.addAll(methodSignatures);
	}

	public Set<MethodNode> getOverriddenMethods(MethodNode method) {
		Map<MethodNode, MethodNode> overriddenMethodMap = this.overriddenMethods.getGroup(method);
		return overriddenMethodMap != null ? GlobalUtils.newHashSet(overriddenMethodMap.keySet()) : GlobalUtils.newHashSet();
	}

	private void addLibraryAncestorSignatures(AbstractClassNode classNode, HashSet<MethodSignature> librarySignatures, HashSet<String> processedClassNames, MultiMap<LibraryClassNode, MethodSignature> signaturesByClass) {
		if (!classNode.getInternalName().equals("java/lang/Object")) {
			String superclassName = classNode.getSuperclassName();
			ClassHierarchyNode superclassHierarchyNode = ClassHierarchyNode.getNodeByInternalName(superclassName);
			if ((superclassHierarchyNode == null || !superclassHierarchyNode.isWorkspaceClass()) && !processedClassNames.contains(superclassName)) {
				processedClassNames.add(superclassName);
				String superclassReason = "looking for superclass of '" + classNode.getBinaryName() + "' : '" + classNode.getRawSourcePath() + "'";
				LibraryClassNode superclassLibraryClass = this.loadLibraryClass(superclassName, superclassReason);
				this.addLibraryMethodSignatures(superclassLibraryClass, librarySignatures, signaturesByClass);
				this.addLibraryAncestorSignatures(superclassLibraryClass, librarySignatures, processedClassNames, signaturesByClass);
			}

			String[] interfaceNames = classNode.getInterfaceNames();

			for (int i = 0; i < interfaceNames.length; i++) {
				String interfaceName = interfaceNames[i];
				ClassHierarchyNode interfaceHierarchyNode = ClassHierarchyNode.getNodeByInternalName(interfaceName);
				if ((interfaceHierarchyNode == null || !interfaceHierarchyNode.isWorkspaceClass()) && !processedClassNames.contains(interfaceName)) {
					processedClassNames.add(interfaceName);
					String interfaceReason = "looking for interfaces implemented by '" + classNode.getBinaryName() + "' : '" + classNode.getRawSourcePath() + "'";
					LibraryClassNode interfaceLibraryClass = this.loadLibraryClass(interfaceName, interfaceReason);
					HashSet<MethodSignature> interfaceSignatures;
					if (classNode.isWorkspaceNode()) {
						interfaceSignatures = GlobalUtils.newHashSet();
					} else {
						interfaceSignatures = librarySignatures;
					}

					this.addLibraryMethodSignatures(interfaceLibraryClass, interfaceSignatures, signaturesByClass);
					this.addLibraryAncestorSignatures(interfaceLibraryClass, interfaceSignatures, processedClassNames, signaturesByClass);
					if (classNode.isWorkspaceNode()) {
						librarySignatures.addAll(interfaceSignatures);
						this.addOuterImplementations((ClassNode)classNode, interfaceSignatures.iterator());
					}
				}
			}
		}
	}
}
