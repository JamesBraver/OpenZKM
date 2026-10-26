package com.zelix.framework.analyzer;

import com.zelix.framework.AbstractClassNode;
import com.zelix.framework.AbstractFieldNode;
import com.zelix.framework.AbstractMethodNode;
import com.zelix.framework.reference.ReferenceGraph;
import com.zelix.framework.signature.MethodSignature;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.wrappers.LibraryClassNode;
import com.zelix.framework.wrappers.LibraryMethodNode;
import com.zelix.framework.wrappers.MethodNode;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.repository.ClassProvider;
import com.zelix.tools.flow.FlowObfuscateExclude;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.collections.ArraySet;
import com.zelix.utils.collections.MultiMap;
import com.zelix.workspace.ClassHierarchyNode;
import com.zelix.workspace.ClassTreeManager;
import com.zelix.workspace.PhantomClassManager;
import com.zelix.workspace.Workspace;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

public class StaticBlockAnalyzer {
	private Set<AbstractMethodNode> initializerMethods = GlobalUtils.newHashSet();
	private final Workspace workspace;
	private final MultiMap<AbstractMethodNode, AbstractMethodNode> initializerCallers;
	private final MultiMap<AbstractMethodNode, AbstractMethodNode> calledMethods;
	private final MultiMap<AbstractClassNode, AbstractClassNode> referencedClasses;

	public StaticBlockAnalyzer(
		AbstractClassNode[] classes,
		Workspace workspace,
		ClassTreeManager classTreeManager,
		ScriptContext scriptContext,
		FlowObfuscateExclude flowObfuscateExclude,
		ClassProvider classProvider,
		PhantomClassManager phantomClassManager,
		boolean flowObfuscationEnabled
	) {
		this.workspace = workspace;

		for (AbstractClassNode classNode : classes) {
			if (classNode.isWorkspaceNode()) {
				ClassNode workspaceClass = (ClassNode)classNode;
				MethodNode workspaceInitializerMethod = workspace.getMethodBySignature(workspaceClass, MethodSignature.STATIC_INITIALIZER);
				if (workspaceInitializerMethod != null) {
					this.initializerMethods.add(workspaceInitializerMethod);
				}
			} else {
				LibraryClassNode libraryClass = (LibraryClassNode)classNode;
				libraryClass.resolveConstantPoolMembers(workspace, classProvider, phantomClassManager);
				LibraryMethodNode libraryInitializerMethod = (LibraryMethodNode)libraryClass.findMethod(MethodSignature.STATIC_INITIALIZER);
				if (libraryInitializerMethod != null) {
					this.initializerMethods.add(libraryInitializerMethod);
				}
			}

			if (classNode.hasInnerClasses()) {
				Iterator<AbstractClassNode> innerClassIterator = classNode.getInnerClasses().iterator();

				while (innerClassIterator.hasNext()) {
					AbstractMethodNode innerInitializerMethod = innerClassIterator.next().findMethod(MethodSignature.STATIC_INITIALIZER);
					if (innerInitializerMethod != null) {
						this.initializerMethods.add(innerInitializerMethod);
					}
				}
			}
		}

		ReferenceGraph<AbstractClassNode, AbstractClassNode> classReferenceGraph = new ReferenceGraph<>(this.initializerMethods.size());
		HashSet<AbstractClassNode> allClasses = GlobalUtils.newHashSet(new ArraySet<>(classes));
		ReferenceGraph<AbstractMethodNode, AbstractMethodNode> methodReferenceGraph = new ReferenceGraph<>(allClasses.size());

		for (AbstractMethodNode initializerMethod : this.initializerMethods) {
			List<MethodNode> referencingMethods = null;
			List<ClassNode> stringReferencingClasses = null;
			if (initializerMethod.isWorkspaceNode()) {
				referencingMethods = workspace.getReferencingWorkspaceMethods((MethodNode)initializerMethod);
				stringReferencingClasses = workspace.getClassesWithReferencingStrings((MethodNode)initializerMethod);
			}

			this.collectMethodDependencies(initializerMethod, stringReferencingClasses, referencingMethods, allClasses, methodReferenceGraph, classReferenceGraph);
		}

		this.initializerCallers = methodReferenceGraph.toMultiMap();
		methodReferenceGraph.clear();
		this.calledMethods = new MultiMap<>(this.initializerMethods.size());

		for (Entry<AbstractMethodNode, List<AbstractMethodNode>> initializerCallerEntry : this.initializerCallers.entrySet()) {
			for (AbstractMethodNode callerMethod : initializerCallerEntry.getValue()) {
				AbstractMethodNode calledInitializerMethod = initializerCallerEntry.getKey();
				this.calledMethods.put(callerMethod, calledInitializerMethod);
			}
		}

		ReferenceGraph<AbstractClassNode, AbstractClassNode> inheritedReferenceGraph = new ReferenceGraph<>(this.initializerMethods.size());

		for (ClassHierarchyNode classHierarchyRoot : classTreeManager.getAvailableClassRoots()) {
			this.collectInheritedReferences(classHierarchyRoot, GlobalUtils.newHashSet(), classReferenceGraph, inheritedReferenceGraph);
		}

		this.referencedClasses = inheritedReferenceGraph.toMultiMap();
		classReferenceGraph.clear();
		inheritedReferenceGraph.clear();

		for (Entry<AbstractClassNode, List<AbstractClassNode>> referencedClassesEntry : this.referencedClasses.entrySet()) {
			AbstractClassNode referencingClassNode = referencedClassesEntry.getKey();
			List<AbstractClassNode> referencedClassNodes = referencedClassesEntry.getValue();
			if (referencingClassNode.hasInnerClassInfo()) {
				for (AbstractClassNode innerClassNode : referencingClassNode.getSelfAndAllInnerClasses()) {
					referencedClassNodes.remove(innerClassNode);
				}
			} else {
				referencedClassNodes.remove(referencingClassNode);
			}
		}

		if (flowObfuscationEnabled && this.initializerMethods.size() > 0 && scriptContext.isVerbose()) {
			PrintWriter printWriter = scriptContext.getWriter();
			if (printWriter != null) {
				this.printFlowExclusions(printWriter, flowObfuscateExclude);
			}
		}
	}

	private void printFlowExclusions(PrintWriter writer, FlowObfuscateExclude flowObfuscateExclude) {
		List<AbstractMethodNode> methodsCalledByInitializers = GlobalUtils.toList(this.initializerCallers.keys());
		StaticBlockAnalyzer.AbstractMethodNodeComparator methodComparator = new StaticBlockAnalyzer.AbstractMethodNodeComparator(this);
		Collections.sort(methodsCalledByInitializers, methodComparator);

		for (int i = 0; i < methodsCalledByInitializers.size(); i++) {
			AbstractMethodNode calledMethod = methodsCalledByInitializers.get(i);
			if (calledMethod.isWorkspaceNode()) {
				MethodNode calledWorkspaceMethod = (MethodNode)calledMethod;
				if (flowObfuscateExclude == null || !flowObfuscateExclude.isClassExcluded(calledWorkspaceMethod.getMethodOwner()) && !flowObfuscateExclude.isMethodExcluded(calledWorkspaceMethod)) {
					StringBuilder callerClassNamesBuilder = new StringBuilder();
					List<AbstractMethodNode> callers = this.initializerCallers.get(calledWorkspaceMethod);
					Iterator<AbstractMethodNode> callerIterator = callers.iterator();

					while (callerIterator.hasNext()) {
						AbstractClassNode callerClass = callerIterator.next().getClassNode();
						callerClassNamesBuilder.append("\"" + callerClass.getBinaryName() + "\"");
						if (callerClass.hasInnerClassOrdinal()) {
							callerClassNamesBuilder.append(" v" + callerClass.getInnerClassOrdinal());
						}

						if (callerIterator.hasNext()) {
							callerClassNamesBuilder.append(", ");
						}
					}

					writer.println(
						"\tExcluding from flow obfuscation method \""
							+ calledWorkspaceMethod.toDeclarationStringSimple(false).trim()
							+ "\" in class \""
							+ calledWorkspaceMethod.getBinaryName()
							+ "\" "
							+ (callers.size() == 1 ? "because it is called by the class initializer in " : "because it is called by class initializers in ")
							+ callerClassNamesBuilder
					);
				}
			}
		}
	}

	public boolean isCalledByInitializer(MethodNode method) {
		ClassNode methodOwner = method.getMethodOwner();
		if (methodOwner.hasInnerClassInfo()) {
			MethodSignature methodSignature = method.getMethodSignature();
			Iterator<AbstractClassNode> innerClassIterator = methodOwner.getSelfAndAllInnerClasses().iterator();

			while (innerClassIterator.hasNext()) {
				AbstractMethodNode innerClassMethod = innerClassIterator.next().findMethod(methodSignature);
				if (this.initializerCallers.containsKey(innerClassMethod)) {
					return true;
				}
			}

			return false;
		} else {
			return this.initializerCallers.containsKey(method);
		}
	}

	private void collectMethodDependencies(
		AbstractMethodNode initializerMethod,
		List<ClassNode> stringReferencingClasses,
		List<MethodNode> referencingMethods,
		Set<AbstractClassNode> allClasses,
		ReferenceGraph<AbstractMethodNode, AbstractMethodNode> methodReferenceGraph,
		ReferenceGraph<AbstractClassNode, AbstractClassNode> classReferenceGraph
	) {
		HashSet<AbstractMethodNode> calledInnerClassMethods = GlobalUtils.newHashSet(13);
		HashSet<AbstractClassNode> directClassDependencies = GlobalUtils.newHashSet();
		HashSet<AbstractClassNode> indirectClassDependencies = GlobalUtils.newHashSet(13);
		HashSet<AbstractFieldNode> referencedFields = GlobalUtils.newHashSet(13);
		HashSet<AbstractMethodNode> referencedMethods = GlobalUtils.newHashSet(13);
		initializerMethod.collectInnerClassMethodCalls(calledInnerClassMethods, allClasses);
		if (referencingMethods != null) {
			for (MethodNode referencingMethod : referencingMethods) {
				referencingMethod.collectInnerClassMethodCalls(calledInnerClassMethods, allClasses);
			}
		}

		calledInnerClassMethods.remove(initializerMethod);
		initializerMethod.collectClassDependencies(directClassDependencies, indirectClassDependencies, referencedFields, referencedMethods);
		if (stringReferencingClasses != null) {
			for (ClassNode stringReferencingClass : stringReferencingClasses) {
				indirectClassDependencies.add(stringReferencingClass);
			}
		}

		for (AbstractMethodNode calledMethod : calledInnerClassMethods) {
			methodReferenceGraph.addReference(calledMethod, initializerMethod);
			calledMethod.collectClassDependencies(directClassDependencies, indirectClassDependencies, referencedFields, referencedMethods);
		}

		if (!referencedFields.isEmpty()) {
			Iterator<AbstractFieldNode> fieldIterator = referencedFields.iterator();

			while (fieldIterator.hasNext()) {
				AbstractClassNode arrayElementClass = TypeWriter.getArrayElementClassNode(fieldIterator.next().getDescriptor());
				if (arrayElementClass != null) {
					if (arrayElementClass.hasInnerClassInfo()) {
						indirectClassDependencies.addAll(arrayElementClass.getSelfAndAllInnerClasses());
					} else {
						indirectClassDependencies.add(arrayElementClass);
					}
				}
			}
		}

		if (!referencedMethods.isEmpty()) {
			Iterator<AbstractMethodNode> methodIterator = referencedMethods.iterator();

			while (methodIterator.hasNext()) {
				List<AbstractClassNode> descriptorReferencedClasses = TypeWriter.getReferencedClasses(methodIterator.next().getDescriptor());
				if (descriptorReferencedClasses != null) {
					indirectClassDependencies.addAll(descriptorReferencedClasses);
				}
			}
		}

		if (!indirectClassDependencies.isEmpty()) {
			classReferenceGraph.addReferences(initializerMethod.getClassNode(), indirectClassDependencies);
		}

		if (!directClassDependencies.isEmpty()) {
			classReferenceGraph.addReferences(initializerMethod.getClassNode(), directClassDependencies);
		}
	}

	public boolean hasOnlyLeafDependencies(AbstractClassNode classNode) {
		List<AbstractClassNode> directDependencies = this.referencedClasses.get(classNode);
		boolean allLeafDependencies = true;
		if (directDependencies != null && !directDependencies.isEmpty()) {
			for (AbstractClassNode referencedClass : directDependencies) {
				List<AbstractClassNode> transitiveDependencies = this.referencedClasses.get(referencedClass);
				if (transitiveDependencies != null && !transitiveDependencies.isEmpty()) {
					allLeafDependencies = false;
					break;
				}
			}
		}

		return allLeafDependencies;
	}

	private void collectInheritedReferences(
		ClassHierarchyNode hierarchyNode,
		Set<AbstractClassNode> accumulatedReferences,
		ReferenceGraph<AbstractClassNode, AbstractClassNode> classReferenceGraph,
		ReferenceGraph<AbstractClassNode, AbstractClassNode> inheritedReferenceGraph
	) {
		AbstractClassNode hierarchyClassNode = hierarchyNode.getClassNode();
		Set<AbstractClassNode> directReferences = classReferenceGraph.getReferences(hierarchyClassNode);
		if (directReferences != null) {
			accumulatedReferences.addAll(directReferences);
		}

		if (hierarchyClassNode.hasInnerClasses()) {
			for (AbstractClassNode innerClass : hierarchyClassNode.getInnerClasses()) {
				Set<AbstractClassNode> innerClassReferences = classReferenceGraph.getReferences(innerClass);
				if (innerClassReferences != null) {
					accumulatedReferences.addAll(innerClassReferences);
				}
			}
		}

		inheritedReferenceGraph.addReferences(hierarchyClassNode, accumulatedReferences);
		if (hierarchyClassNode.hasInnerClasses()) {
			for (AbstractClassNode innerClassNode : hierarchyClassNode.getInnerClasses()) {
				inheritedReferenceGraph.addReferences(innerClassNode, accumulatedReferences);
			}
		}

		Enumeration<ClassHierarchyNode> subclassEnumeration = hierarchyNode.getSubclassesEnumeration();
		if (subclassEnumeration != null) {
			while (subclassEnumeration.hasMoreElements()) {
				ClassHierarchyNode subclassHierarchyNode = subclassEnumeration.nextElement();
				if (subclassHierarchyNode.getClassNode() != null) {
					this.collectInheritedReferences(subclassHierarchyNode, GlobalUtils.newHashSet(accumulatedReferences), classReferenceGraph, inheritedReferenceGraph);
				}
			}
		}
	}

	public boolean isCalledBySuperInitializer(MethodNode method) {
		String methodInternalName = method.getInternalName();
		List<AbstractMethodNode> callers = this.initializerCallers.get(method);
		if (callers != null) {
			Iterator<AbstractMethodNode> callerIterator = callers.iterator();

			while (callerIterator.hasNext()) {
				String callerInternalName = callerIterator.next().getInternalName();
				if (this.workspace.isClassInstanceOf(methodInternalName, callerInternalName)) {
					return true;
				}
			}
		}

		return false;
	}

	public boolean isInitializerOrCallee(MethodNode method) {
		return this.initializerMethods.contains(method) ? true : this.isCalledByInitializer(method);
	}

	public Set<ClassNode> getReferencedSubclasses(MethodNode method) {
		String methodInternalName = method.getInternalName();
		HashSet<ClassNode> referencedSubclasses = GlobalUtils.newHashSet();
		List<AbstractClassNode> referencedClassNodes = this.referencedClasses.get(method.getMethodOwner());
		if (referencedClassNodes != null) {
			for (AbstractClassNode referencedClassNode : referencedClassNodes) {
				if (referencedClassNode.isWorkspaceNode() && this.workspace.isClassInstanceOf(referencedClassNode.getInternalName(), methodInternalName)) {
					referencedSubclasses.add((ClassNode)referencedClassNode);
				}
			}
		}

		return referencedSubclasses;
	}

	public static class AbstractMethodNodeComparator implements Comparator<AbstractMethodNode> {
		public final StaticBlockAnalyzer staticBlockAnalyzer;

		public AbstractMethodNodeComparator(StaticBlockAnalyzer staticBlockAnalyzer) {
			this.staticBlockAnalyzer = staticBlockAnalyzer;
		}

		public int compareByOwnerAndName(AbstractMethodNode method1, AbstractMethodNode method2) {
			int nameComparison = method1.getInternalName().compareTo(method2.getInternalName());
			return nameComparison != 0 ? nameComparison : method1.getName().compareTo(method2.getName());
		}

		@Override
		public int compare(AbstractMethodNode o1, AbstractMethodNode o2) {
			return this.compareByOwnerAndName(o1, o2);
		}
	}
}
