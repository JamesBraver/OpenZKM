package com.zelix.tools.renamer;

import com.zelix.exceptions.ProcessingException;
import com.zelix.exceptions.ZKMUnexpectedException;
import com.zelix.framework.AbstractClassNode;
import com.zelix.framework.AbstractFieldNode;
import com.zelix.framework.AbstractMethodNode;
import com.zelix.framework.reference.ReferenceGraph;
import com.zelix.framework.signature.FieldSignature;
import com.zelix.framework.signature.MethodSignature;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.wrappers.FieldNode;
import com.zelix.framework.wrappers.LibraryClassNode;
import com.zelix.klassmaster.changelogs.parser.context.ChangeLogContext;
import com.zelix.klassmaster.config.Options;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.repository.ClassProvider;
import com.zelix.tools.excludes.DefaultExcludes;
import com.zelix.tools.renamer.field.AbstractFieldNameGenerator;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.collections.*;
import com.zelix.utils.dataset.AtomicBool;
import com.zelix.workspace.ClassHierarchyNode;
import com.zelix.workspace.ClassTreeManager;
import com.zelix.workspace.Workspace;

import java.util.*;

public class FieldRenamer {
	private final ClassTreeManager classTreeManager;
	private final ClassNode[] classes;
	private final AbstractClassNode[] abstractClassNodes;
	private final Workspace workspace;
	private final DefaultExcludes defaultExcludes;
	private final ChangeLogContext changeLogContext;
	private final HashMap<String, String> newToOldClassNameMap;
	private final HashMap<AbstractFieldNode, String> excludedFieldNames;
	private final DualKeyMap<String, FieldSignature, FieldSignature> oldToNewSignatures;
	private final DualKeyMap<String, FieldSignature, FieldSignature> newToOldSignatures;
	private final boolean randomizeObfuscation;

	public FieldRenamer(
		DefaultExcludes defaultExcludes,
		ChangeLogContext changeLogContext,
		Workspace workspace,
		ClassNode[] classes,
		AbstractClassNode[] abstractClassNodes,
		ClassTreeManager classTreeManager,
		HashMap<String, String> newToOldClassNameMap,
		DualKeyMap<String, FieldSignature, FieldSignature> oldToNewSignatures,
		DualKeyMap<String, FieldSignature, FieldSignature> newToOldSignatures,
		boolean randomizeObfuscation
	) {
		this.classTreeManager = classTreeManager;
		this.classes = classes;
		this.abstractClassNodes = abstractClassNodes;
		this.workspace = workspace;
		this.defaultExcludes = defaultExcludes;
		this.changeLogContext = changeLogContext;
		this.newToOldClassNameMap = newToOldClassNameMap;
		this.excludedFieldNames = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(classes.length * 5));
		this.oldToNewSignatures = oldToNewSignatures;
		this.newToOldSignatures = newToOldSignatures;
		this.randomizeObfuscation = randomizeObfuscation;
	}

	private void addHierarchyReferences(ClassHierarchyNode classHierarchyNode, FieldSignature fieldSignature, ReferenceGraph<AbstractClassNode, FieldSignature> referenceGraph) {
		Enumeration<ClassHierarchyNode> interfaceEnumeration = classHierarchyNode.getInterfacesEnumeration();
		if (interfaceEnumeration != null) {
			while (interfaceEnumeration.hasMoreElements()) {
				ClassHierarchyNode interfaceNode = interfaceEnumeration.nextElement();
				if (interfaceNode.isWorkspaceClass()) {
					this.addReferenceRecursively(interfaceNode, fieldSignature, referenceGraph);
				}
			}
		}

		ClassHierarchyNode outerClassNode = classHierarchyNode.getOuterClass();
		if (outerClassNode.isWorkspaceClass()) {
			this.addReferenceRecursively(outerClassNode, fieldSignature, referenceGraph);
		}
	}

	private Map<AbstractFieldNode, String> getChangeLogFieldNames(ChangeLogContext changeLogContext) {
		if (changeLogContext == null) {
			return null;
		} else {
			Enumeration<String> mappingClassEnumeration = changeLogContext.getFieldMappingClasses();
			if (mappingClassEnumeration == null) {
				return null;
			} else {
				HashMap<AbstractFieldNode, String> changeLogFieldNames = GlobalUtils.newHashMap();

				while (mappingClassEnumeration.hasMoreElements()) {
					String classInternalName = mappingClassEnumeration.nextElement();
					AbstractClassNode classNode = ClassHierarchyNode.getClassNodeByInternalName(classInternalName);
					if (classNode != null) {
						for (KeyValuePair<FieldSignature, FieldSignature> fieldSignatureMapping : changeLogContext.getFieldSignatureMappings(classInternalName)) {
							FieldSignature oldFieldSignature = fieldSignatureMapping.getKey();
							FieldSignature newFieldSignature = fieldSignatureMapping.getValue();
							AbstractFieldNode fieldNode = classNode.findField(oldFieldSignature);
							changeLogFieldNames.put(fieldNode, newFieldSignature.getName());
						}
					}
				}

				return changeLogFieldNames;
			}
		}
	}

	private void addReferenceRecursively(ClassHierarchyNode classHierarchyNode, FieldSignature fieldSignature, ReferenceGraph<AbstractClassNode, FieldSignature> referenceGraph) {
		if (referenceGraph.addReference(classHierarchyNode.getClassNode(), fieldSignature)) {
			this.addHierarchyReferences(classHierarchyNode, fieldSignature, referenceGraph);
		}
	}

	private void addExcludedFieldNames(HashMap<AbstractFieldNode, String> excludedFieldNames, DefaultExcludes defaultExcludes) {
		Enumeration<FieldNode> excludedFieldEnumeration = defaultExcludes.getExcludedFields();

		while (excludedFieldEnumeration.hasMoreElements()) {
			FieldNode excludedField = excludedFieldEnumeration.nextElement();
			excludedFieldNames.put(excludedField, excludedField.getNameDisplay());
		}
	}

	private void addReferrerReferences(ClassHierarchyNode classHierarchyNode, FieldSignature fieldSignature, ReferenceGraph<AbstractClassNode, FieldSignature> referenceGraph) {
		if (referenceGraph.addReference(classHierarchyNode.getClassNode(), fieldSignature)) {
			this.addHierarchyReferences(classHierarchyNode, fieldSignature, referenceGraph);
		}
	}

	private void addSubclassReferences(ClassHierarchyNode classHierarchyNode, FieldSignature fieldSignature, ReferenceGraph<AbstractClassNode, FieldSignature> referenceGraph) {
		if (referenceGraph.addReference(classHierarchyNode.getClassNode(), fieldSignature)) {
			List<ClassHierarchyNode> directSubclasses = classHierarchyNode.getDirectSubclassesList();
			if (directSubclasses != null) {
				for (ClassHierarchyNode directSubclass : directSubclasses) {
					this.addSubclassReferences(directSubclass, fieldSignature, referenceGraph);
				}
			}
		}
	}

	private List<AbstractClassNode> getSuperclassChain(String internalName) {
		List<AbstractClassNode> superclasses = new ArrayList<>();
		ClassProvider classProvider = this.workspace.getClassProvider();
		String lookupContext = "Searching for superclasses of '" + internalName + "'";
		AbstractClassNode classNode = classProvider.getClassNode(internalName, lookupContext);

		try {
			String superclassName = classNode.getSuperclassName();

			while (superclassName != null) {
				AbstractClassNode superclassNode = classProvider.getClassNode(superclassName, lookupContext);
				superclasses.add(superclassNode);
				superclassName = superclassNode.getSuperclassName();
			}
		} catch (StackOverflowError stackOverflowError) {
			throw new ZKMUnexpectedException(stackOverflowError.getMessage() + " : Class " + classNode.getSourcePath() + " has corrupt inheritance hierarchy (D)", stackOverflowError);
		}

		return superclasses;
	}

	private ReferenceGraph<AbstractClassNode, FieldSignature> buildReferenceGraph(DefaultExcludes defaultExcludes, Map<AbstractFieldNode, String> changeLogFieldNames, Map<FieldNode, String> accessorFieldNames) {
		ReferenceGraph<AbstractClassNode, FieldSignature> referenceGraph = new ReferenceGraph<>(this.classes.length, 5);
		if (defaultExcludes != null) {
			Enumeration<FieldNode> excludedFieldEnumeration = defaultExcludes.getExcludedFields();

			while (excludedFieldEnumeration.hasMoreElements()) {
				FieldNode excludedField = excludedFieldEnumeration.nextElement();
				FieldSignature excludedFieldSignature = excludedField.getFieldSignature();
				ClassHierarchyNode excludedFieldHierarchyNode = ClassHierarchyNode.getNodeByInternalName(excludedField.getInternalName());
				this.addHierarchyReferences(excludedFieldHierarchyNode, excludedFieldSignature, referenceGraph);
				AbstractClassNode excludedFieldClassNode = excludedFieldHierarchyNode.getClassNode();
				if (excludedFieldClassNode != null && excludedFieldClassNode.isWorkspaceNode()) {
					Enumeration<AbstractFieldNode> excludedClassReferencedFieldEnumeration = ((ClassNode)excludedFieldClassNode).getReferencedFieldNodes();
					if (excludedClassReferencedFieldEnumeration != null) {
						while (excludedClassReferencedFieldEnumeration.hasMoreElements()) {
							AbstractFieldNode excludedClassReferencedField = excludedClassReferencedFieldEnumeration.nextElement();
							Enumeration<AbstractClassNode> excludedReferencingClassEnumeration = ((ClassNode)excludedFieldClassNode).getClassesReferencingField(excludedClassReferencedField);

							while (excludedReferencingClassEnumeration.hasMoreElements()) {
								ClassHierarchyNode excludedReferencingClassNode = ClassHierarchyNode.getNodeByInternalName(excludedReferencingClassEnumeration.nextElement().getInternalName());
								if (excludedReferencingClassNode != null) {
									this.addReferrerReferences(excludedReferencingClassNode, excludedFieldSignature, referenceGraph);
								}
							}
						}
					}
				}
			}
		}

		if (changeLogFieldNames != null) {
			for (int classIndex = 0; classIndex < this.classes.length; classIndex++) {
				ClassNode classNode = this.classes[classIndex];
				FieldNode[] declaredFields = classNode.getFieldNodes();

				for (int fieldIndex = 0; fieldIndex < declaredFields.length; fieldIndex++) {
					FieldNode declaredField = declaredFields[fieldIndex];
					String newFieldName = changeLogFieldNames.get(declaredField);
					if (newFieldName != null) {
						FieldSignature newFieldSignature = new FieldSignature(newFieldName, declaredField.getDescriptor());
						ClassHierarchyNode renamedFieldHierarchyNode = ClassHierarchyNode.getNodeByInternalName(classNode.getInternalName());
						this.addHierarchyReferences(renamedFieldHierarchyNode, newFieldSignature, referenceGraph);
					}
				}

				Enumeration<AbstractFieldNode> classReferencedFieldEnumeration = classNode.getReferencedFieldNodes();
				if (classReferencedFieldEnumeration != null) {
					while (classReferencedFieldEnumeration.hasMoreElements()) {
						AbstractFieldNode classReferencedField = classReferencedFieldEnumeration.nextElement();
						String newClassReferencedFieldName = changeLogFieldNames.get(classReferencedField);
						if (newClassReferencedFieldName != null) {
							FieldSignature newClassReferencedFieldSignature = new FieldSignature(newClassReferencedFieldName, classReferencedField.getDescriptor());
							Enumeration<AbstractClassNode> classReferencingClassEnumeration = classNode.getClassesReferencingField(classReferencedField);

							while (classReferencingClassEnumeration.hasMoreElements()) {
								ClassHierarchyNode classReferencingClassNode = ClassHierarchyNode.getNodeByInternalName(classReferencingClassEnumeration.nextElement().getInternalName());
								this.addReferrerReferences(classReferencingClassNode, newClassReferencedFieldSignature, referenceGraph);
							}
						}
					}
				}
			}
		}

		for (FieldNode referencedField : defaultExcludes.getReferencedFields()) {
			if (!defaultExcludes.isFieldExcluded(referencedField) && (changeLogFieldNames == null || !changeLogFieldNames.containsKey(referencedField))) {
				Set<AbstractMethodNode> accessorMethods = defaultExcludes.getFieldAccessorMethods(referencedField);
				AbstractMethodNode accessorMethod = accessorMethods != null && accessorMethods.size() > 0 ? accessorMethods.iterator().next() : null;
				if (accessorMethod != null) {
					String methodPrefix = defaultExcludes.getMethodPrefix(accessorMethod);
					String accessorFieldName = MethodSignature.getFieldNameFromAccessor(accessorMethod.getNameDisplay(), methodPrefix);
					FieldSignature accessorFieldSignature = new FieldSignature(accessorFieldName, referencedField.getDescriptor());
					referenceGraph.addReference(referencedField.getOwnerClass(), accessorFieldSignature);
					ClassHierarchyNode referencedFieldClassNode = ClassHierarchyNode.getNodeByInternalName(referencedField.getInternalName());
					this.addHierarchyReferences(referencedFieldClassNode, accessorFieldSignature, referenceGraph);
					accessorFieldNames.put(referencedField, accessorFieldName);
				}
			}
		}

		if (!Options.registerMissingClassFieldRefs) {
			for (int classIndex = 0; classIndex < this.classes.length; classIndex++) {
				ClassNode classNode = this.classes[classIndex];
				Enumeration<AbstractFieldNode> referencedFieldEnumeration = classNode.getReferencedFieldNodes();
				if (referencedFieldEnumeration != null) {
					while (referencedFieldEnumeration.hasMoreElements()) {
						AbstractFieldNode referencedField = referencedFieldEnumeration.nextElement();
						if (ClassHierarchyNode.isMissingClass(referencedField.getInternalName())) {
							FieldSignature referencedFieldSignature = referencedField.getFieldSignature();
							Enumeration<AbstractClassNode> referencingClassEnumeration = classNode.getClassesReferencingField(referencedField);

							while (referencingClassEnumeration.hasMoreElements()) {
								ClassHierarchyNode referencingClassNode = ClassHierarchyNode.getNodeByInternalName(referencingClassEnumeration.nextElement().getInternalName());
								if (referencingClassNode != null) {
									this.addReferrerReferences(referencingClassNode, referencedFieldSignature, referenceGraph);
								}
							}
						}
					}
				}
			}
		}

		return referenceGraph;
	}

	private boolean hasNoFieldNameConflicts() {
		boolean noConflictDetected = true;
		if (this.changeLogContext == null) {
			return true;
		} else {
			Enumeration<String> mappingClassEnumeration = this.changeLogContext.getFieldMappingClasses();
			if (mappingClassEnumeration == null) {
				return true;
			} else {
				while (mappingClassEnumeration.hasMoreElements()) {
					String classInternalName = mappingClassEnumeration.nextElement();
					ClassNode classNode = ClassHierarchyNode.getWorkspaceClassNode(classInternalName);
					if (classNode != null) {
						Enumeration<FieldSignature> newFieldSignatureEnumeration = this.changeLogContext.getRenamedFieldSignatures(classInternalName);
						if (newFieldSignatureEnumeration != null) {
							while (newFieldSignatureEnumeration.hasMoreElements()) {
								FieldSignature newFieldSignature = newFieldSignatureEnumeration.nextElement();
								if (this.workspace.containsFieldSignature(newFieldSignature, classNode)) {
									FieldNode conflictingField = classNode.findFieldNode(newFieldSignature);
									if (this.defaultExcludes.isFieldExcluded(conflictingField)) {
										String oldClassName = GlobalUtils.internalToBinaryName(this.changeLogContext.getOldClassName(classInternalName));
										this.changeLogContext
											.logSpecialError(
												"\""
													+ newFieldSignature.toRemappedString(this.newToOldClassNameMap)
													+ "\" is specified as a new field name in class \""
													+ oldClassName
													+ "\" but there is an existing field of that name which will not be renamed."
											);
										noConflictDetected = false;
									}
								}
							}
						}
					}
				}

				return noConflictDetected;
			}
		}
	}

	public void renameFields(AbstractFieldNameGenerator dictionaryGenerator, ScriptContext scriptContext) {
		if (this.hasNoFieldNameConflicts()) {
			Map<AbstractFieldNode, String> changeLogFieldNames = this.getChangeLogFieldNames(this.changeLogContext);
			HashMap<FieldNode, String> accessorFieldNames = GlobalUtils.newHashMap();
			ReferenceGraph<AbstractClassNode, FieldSignature> referenceGraph = this.buildReferenceGraph(this.defaultExcludes, changeLogFieldNames, accessorFieldNames);
			this.addExcludedFieldNames(this.excludedFieldNames, this.defaultExcludes);
			List<ClassHierarchyNode> rootInterfaceNodes = this.classTreeManager.getRootInterfaces();
			List<ClassHierarchyNode> workspaceRootNodes = this.classTreeManager.getWorkspaceClassRoots();

			if (!Options.registerRootClassFieldRefs) {
				for (ClassHierarchyNode workspaceRootNode : workspaceRootNodes) {
					if (!workspaceRootNode.isInterface()) {
						ClassNode workspaceRootClassNode = workspaceRootNode.getClassNodeIfWorkspace();
						if (workspaceRootClassNode == null) {
							continue;
						}

						for (AbstractClassNode superclassNode : this.getSuperclassChain(workspaceRootClassNode.getInternalName())) {
							AbstractFieldNode[] superclassFields = superclassNode.getFields();

							for (int fieldIndex = 0; fieldIndex < superclassFields.length; fieldIndex++) {
								AbstractFieldNode superclassField = superclassFields[fieldIndex];
								if ((!superclassField.isFinal() || !superclassField.isStatic())
									&& (superclassField.isPublic() || superclassField.isProtected() || !superclassField.isPrivate() && workspaceRootClassNode.getPackageName().equals(superclassNode.getPackageName()))) {
									this.addSubclassReferences(workspaceRootNode, superclassField.getFieldSignature(), referenceGraph);
								}
							}
						}
					}
				}
			}

			RingQueue<ClassHierarchyNode> classQueue = new RingQueue<>(Math.max(this.classes.length, 5));

			for (int rootInterfaceIndex = 0; rootInterfaceIndex < rootInterfaceNodes.size(); rootInterfaceIndex++) {
				classQueue.push(rootInterfaceNodes.get(rootInterfaceIndex));
			}

			label222:
			for (int rootClassIndex = 0; rootClassIndex < workspaceRootNodes.size(); rootClassIndex++) {
				ClassHierarchyNode rootClassNode = workspaceRootNodes.get(rootClassIndex);
				if (!rootClassNode.isInterface()) {
					Enumeration<ClassHierarchyNode> rootInterfaceEnumeration = rootClassNode.getInterfacesEnumeration();
					if (rootInterfaceEnumeration != null) {
						while (rootInterfaceEnumeration.hasMoreElements()) {
							if (rootInterfaceEnumeration.nextElement().isWorkspaceClass()) {
								continue label222;
							}
						}
					}

					classQueue.push(rootClassNode);
				}
			}

			DualKeyMap<AbstractClassNode, FieldNode, AbstractClassNode> referencingClassByClassAndField = new DualKeyMap<>();
			DualKeyMap<ClassHierarchyNode, String, FieldNode> fieldNodesByClassGroup = new DualKeyMap<>(this.classes.length, 20);
			HierarchicalMultiMap<ClassHierarchyNode, String, String> fieldNamesByDescriptorKeyByClass = new HierarchicalMultiMap<>(this.classes.length);
			int progressMade = 1;
			int interfaceCheckEnabled = 1;

			while (progressMade != 0 && !classQueue.isEmpty()) {
				progressMade = 0;
				int passQueueSize = classQueue.size();

				label201:
				for (int queueIndex = 0; queueIndex < passQueueSize; queueIndex++) {
					ClassHierarchyNode queuedClassNode = classQueue.pop();
					ClassNode workspaceClassNode = queuedClassNode.getClassNodeIfWorkspace();
					if (interfaceCheckEnabled != 0 && workspaceClassNode.isInterface() && workspaceClassNode.getFieldsCount() > 0) {
						Enumeration<AbstractFieldNode> referencedFieldEnumeration = workspaceClassNode.getReferencedFieldNodes();
						if (referencedFieldEnumeration != null) {
							while (referencedFieldEnumeration.hasMoreElements()) {
								AbstractFieldNode referencedField = referencedFieldEnumeration.nextElement();
								if (!this.excludedFieldNames.containsKey(referencedField) && !ClassHierarchyNode.isMissingClass(referencedField.getInternalName())) {
									AbstractClassNode referencedFieldClassNode = referencedField.getClassNode();
									if (!referencedFieldClassNode.isInterface() || !this.workspace.isSubclassOf(referencedFieldClassNode.getInternalName(), workspaceClassNode.getInternalName())) {
										classQueue.push(queuedClassNode);
										continue label201;
									}
								}
							}
						}
					}

					ArrayList<ClassHierarchyNode> parentClassNodes = new ArrayList<>();
					boolean parentGroupsReady = true;
					ClassHierarchyNode outerClassNode = queuedClassNode.getOuterClass();
					if (outerClassNode.isWorkspaceClass() && !fieldNodesByClassGroup.containsGroup(outerClassNode)) {
						parentGroupsReady = false;
					} else {
						if (outerClassNode.isWorkspaceClass()) {
							parentClassNodes.add(outerClassNode);
						}

						Enumeration<ClassHierarchyNode> classInterfaceEnumeration = queuedClassNode.getInterfacesEnumeration();
						if (classInterfaceEnumeration != null) {
							while (classInterfaceEnumeration.hasMoreElements()) {
								ClassHierarchyNode interfaceNode = classInterfaceEnumeration.nextElement();
								if (interfaceNode.isWorkspaceClass()) {
									parentClassNodes.add(interfaceNode);
									if (!fieldNodesByClassGroup.containsGroup(interfaceNode)) {
										parentGroupsReady = false;
									}
								}
							}
						}
					}

					if (!parentGroupsReady) {
						classQueue.push(queuedClassNode);
					} else {
						progressMade = 1;
						HashMap<String, FieldNode> fieldNodesByName = GlobalUtils.newHashMap();
						MultiMap<String, String> fieldNamesByDescriptorKey = new MultiMap<>();

						for (ClassHierarchyNode parentClassNode : parentClassNodes) {
							Map<String, FieldNode> parentFieldNodesByName = fieldNodesByClassGroup.getGroup(parentClassNode);
							if (parentFieldNodesByName != null) {
								fieldNodesByName.putAll(parentFieldNodesByName);
							}

							MultiMap<String, String> parentFieldNamesByDescriptorKey = fieldNamesByDescriptorKeyByClass.get(parentClassNode);
							if (parentFieldNamesByDescriptorKey != null) {
								fieldNamesByDescriptorKey.putAll(parentFieldNamesByDescriptorKey);
							}
						}

						Enumeration<ClassHierarchyNode> subclassEnumeration = queuedClassNode.getSubclassesEnumeration();
						if (subclassEnumeration != null) {
							while (subclassEnumeration.hasMoreElements()) {
								ClassHierarchyNode subclassNode = subclassEnumeration.nextElement();
								if (subclassNode.isWorkspaceClass()) {
									classQueue.push(subclassNode);
								} else {
									ArrayList<ClassHierarchyNode> workspaceSubclasses = new ArrayList<>();
									subclassNode.collectWorkspaceSubclasses(workspaceSubclasses);
									classQueue.addAll(workspaceSubclasses);
								}
							}
						}

						Enumeration<ClassHierarchyNode> implementerEnumeration = queuedClassNode.getImplementersEnumeration();
						if (implementerEnumeration != null) {
							while (implementerEnumeration.hasMoreElements()) {
								ClassHierarchyNode implementerNode = implementerEnumeration.nextElement();
								if (implementerNode.isWorkspaceClass()) {
									classQueue.push(implementerNode);
								} else {
									ArrayList<ClassHierarchyNode> workspaceImplementers = new ArrayList<>();
									implementerNode.collectWorkspaceImplementers(workspaceImplementers);
									classQueue.addAll(workspaceImplementers);
								}
							}
						}

						AtomicBool recheckNeeded = new AtomicBool(false);
						workspaceClassNode.renameFieldsWithHierarchy(
							dictionaryGenerator,
							this.oldToNewSignatures,
							this.newToOldSignatures,
							fieldNodesByName,
							new MultiMap<>(fieldNamesByDescriptorKey),
							fieldNamesByDescriptorKey,
							this.defaultExcludes,
							this.changeLogContext,
							this.newToOldClassNameMap,
							changeLogFieldNames,
							this.excludedFieldNames,
							referenceGraph,
							referencingClassByClassAndField,
							accessorFieldNames,
							recheckNeeded,
							this.workspace,
							this.randomizeObfuscation
						);
						if (interfaceCheckEnabled == 0 && recheckNeeded.get()) {
							interfaceCheckEnabled = 1;
						}

						fieldNodesByClassGroup.putGroup(queuedClassNode, fieldNodesByName);
						fieldNamesByDescriptorKeyByClass.put(queuedClassNode, fieldNamesByDescriptorKey);
					}
				}

				if (progressMade == 0 && !classQueue.isEmpty() && interfaceCheckEnabled != 0) {
					interfaceCheckEnabled = 0;
					progressMade = 1;
				}
			}

			if (!classQueue.isEmpty()) {
				StringBuffer cyclicClassNames = new StringBuffer();

				while (!classQueue.isEmpty()) {
					cyclicClassNames.append(classQueue.pop().getInternalName());
					if (!classQueue.isEmpty()) {
						cyclicClassNames.append(", ");
					}
				}

				throw new ProcessingException("Cyclic dependency within : " + cyclicClassNames);
			}

			if (this.changeLogContext != null) {
				Enumeration<String> mappingClassEnumeration = this.changeLogContext.getFieldMappingClasses();

				while (mappingClassEnumeration.hasMoreElements()) {
					String classInternalName = mappingClassEnumeration.nextElement();
					if (!ClassHierarchyNode.isWorkspaceClassByInternalName(classInternalName)) {
						ArrayList<KeyValuePair<FieldSignature, FieldSignature>> fieldSignatureMappings = this.changeLogContext.getFieldSignatureMappings(classInternalName);
						if (fieldSignatureMappings != null) {
							for (int mappingIndex = 0; mappingIndex < fieldSignatureMappings.size(); mappingIndex++) {
								KeyValuePair<FieldSignature, FieldSignature> fieldSignatureMapping = fieldSignatureMappings.get(mappingIndex);
								FieldSignature oldFieldSignature = fieldSignatureMapping.getKey();
								FieldSignature newFieldSignature = fieldSignatureMapping.getValue();
								this.oldToNewSignatures.put(classInternalName, oldFieldSignature, newFieldSignature);
								this.newToOldSignatures.put(classInternalName, newFieldSignature, oldFieldSignature);
							}

							((LibraryClassNode)ClassHierarchyNode.getClassNodeByInternalName(classInternalName)).applyMappedFieldNames(this.oldToNewSignatures);
						}
					}
				}
			}

			this.workspace.applyInnerClassFieldRenames(this.abstractClassNodes, this.oldToNewSignatures, scriptContext);
			this.workspace.refreshFieldRefsAndConstants(this.abstractClassNodes, scriptContext);
			this.workspace.rebuildFieldRegistries();
		}
	}
}
