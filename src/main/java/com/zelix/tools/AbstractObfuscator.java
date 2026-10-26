package com.zelix.tools;

import com.zelix.exceptions.ChangeLogsParserException;
import com.zelix.exceptions.ClasspathException;
import com.zelix.exceptions.FileException;
import com.zelix.exceptions.ProcessingException;
import com.zelix.framework.AbstractClassNode;
import com.zelix.framework.signature.FieldSignature;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.wrappers.FieldNode;
import com.zelix.framework.wrappers.LibraryClassNode;
import com.zelix.klassmaster.changelogs.parser.ChangeLogsParser;
import com.zelix.klassmaster.changelogs.parser.base.ChangeLogsNodeBase;
import com.zelix.klassmaster.changelogs.parser.context.ChangeLogContext;
import com.zelix.klassmaster.constant.LicenseCheckConstant;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.TokenMgrError;
import com.zelix.logging.AbstractLogger;
import com.zelix.repository.ClassProvider;
import com.zelix.repository.ClassRepository;
import com.zelix.tools.excludes.DefaultExcludes;
import com.zelix.tools.excludes.ExcludeElement;
import com.zelix.tools.excludes.FixedClassManager;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.IntegerCache;
import com.zelix.utils.collections.DualKeyMap;
import com.zelix.utils.collections.KeyValuePair;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.collections.RingQueue;
import com.zelix.utils.dataset.ChangeLogFileData;
import com.zelix.utils.enumerations.ArrayEnumeration;
import com.zelix.utils.references.AtomicReference;
import com.zelix.workspace.ClassHierarchyNode;
import com.zelix.workspace.ClassTreeManager;
import com.zelix.workspace.PackageManager;
import com.zelix.workspace.Workspace;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;

public abstract class AbstractObfuscator extends LicenseCheckConstant {
	public final Workspace workspace;
	public ClassNode[] classes;
	public final PackageManager packageManager;
	public final ClassTreeManager classTreeManager;
	public final ClassRepository classRepository;
	public final ClassProvider classProvider;
	public final boolean enableVerbose;
	public final IntegerCache integerCache;

	public AbstractObfuscator(Workspace workspace, ClassNode[] classes, PackageManager packageManager) {
		this.workspace = workspace;
		this.classes = classes;
		this.packageManager = packageManager;
		this.classTreeManager = workspace.getClassTreeManager();
		this.classRepository = workspace.getClassRepository();
		this.classProvider = workspace.getClassProvider();
		this.enableVerbose = workspace.enableVerbose();
		this.integerCache = IntegerCache.getInstance();
	}

	public static MultiMap<String, String> invertClassNameMap(HashMap<String, String> classNameMap) {
		MultiMap<String, String> invertedClassNameMap = new MultiMap<>();

		for (Entry<String, String> classNameEntry : classNameMap.entrySet()) {
			invertedClassNameMap.put(classNameEntry.getValue(), classNameEntry.getKey());
		}

		return invertedClassNameMap;
	}

	public static boolean mergeClassMappings(
		HashMap<String, String> oldToNewClassNameMap, HashMap<String, String> newToOldClassNameMap, HashMap<String, String> classNameToSourceFileMap, ClassRepository classRepository, ChangeLogContext changeLogContext
	) {
		boolean classNameChanged = false;
		if (changeLogContext != null) {
			ArrayList<String> oldClassNames = changeLogContext.getOldClassNames();
			ArrayList<ExcludeElement> classRenameElements = new ArrayList<>();

			for (int i = 0; i < oldClassNames.size(); i++) {
				String oldClassName = oldClassNames.get(i);
				if (!oldToNewClassNameMap.containsKey(oldClassName)) {
					String newClassName = changeLogContext.getNewClassName(oldClassName);
					if (newClassName == null) {
						newClassName = oldClassName;
					}

					if (!oldClassName.equals(newClassName)) {
						classNameChanged = true;
					}

					oldToNewClassNameMap.put(oldClassName, newClassName);
					newToOldClassNameMap.put(newClassName, oldClassName);
					String sourceName = changeLogContext.getSourceName(oldClassName);
					if (sourceName != null) {
						classNameToSourceFileMap.put(newClassName, sourceName);
					}

					LibraryClassNode libraryClassNode = (LibraryClassNode)ClassHierarchyNode.getNodeByInternalName(oldClassName).getClassNode();
					if (!oldClassName.equals(newClassName)) {
						classRepository.removeFromCaches(oldClassName, libraryClassNode);
						ExcludeElement excludeElement = new ExcludeElement(oldClassName, newClassName, libraryClassNode);
						classRenameElements.add(excludeElement);
					}
				}
			}

			for (ExcludeElement renameElement : classRenameElements) {
				((LibraryClassNode)renameElement.getSuffix()).renameClassInternal((String)renameElement.getPrefix(), null);
				classRepository.addToCaches((String)renameElement.getPrefix(), (LibraryClassNode)renameElement.getSuffix());
			}
		}

		return classNameChanged;
	}

	public void propagateInnerExclusion(int keepInnerClassInfo, DefaultExcludes excludes, ChangeLogContext changeLogContext) {
		Enumeration<ClassHierarchyNode> validInnerClassNodes = this.classTreeManager.getValidInnerClassNodes();
		RingQueue<ClassHierarchyNode> classQueue = new RingQueue<>();
		classQueue.addAll(validInnerClassNodes);

		while (!classQueue.isEmpty()) {
			ClassHierarchyNode innerClassNode = classQueue.pop();
			if (innerClassNode.hasSuperClass() && !this.isProcessableWithChangeLog(innerClassNode.getClassNode(), excludes, changeLogContext) && keepInnerClassInfo != 1) {
				ClassHierarchyNode superClassNode = innerClassNode.getSuperClassNode();
				if (superClassNode != null
					&& superClassNode.isWorkspaceClass()
					&& !excludes.isClassExcluded(superClassNode.getClassNodeIfWorkspace())
					&& (changeLogContext == null || !changeLogContext.hasOldClass(superClassNode.getInternalName()))) {
					String superClassExclusionReason = "Inner class format retained and inner class '" + GlobalUtils.internalToBinaryName(innerClassNode.getInternalName()) + "' is excluded. (1)";
					excludes.excludeClassWithReason(superClassNode.getClassNodeIfWorkspace(), superClassExclusionReason, true);
					classQueue.push(superClassNode);
				}

				ClassHierarchyNode superClassLink = innerClassNode.getSuperClassLink();
				if (superClassLink != null
					&& superClassLink.isWorkspaceClass()
					&& superClassLink != superClassNode
					&& !excludes.isClassExcluded(superClassLink.getClassNodeIfWorkspace())
					&& (changeLogContext == null || !changeLogContext.hasOldClass(superClassLink.getInternalName()))) {
					String superClassLinkExclusionReason = "Inner class format retained and inner class '" + GlobalUtils.internalToBinaryName(innerClassNode.getInternalName()) + "' is excluded. (2)";
					excludes.excludeClassWithReason(superClassLink.getClassNodeIfWorkspace(), superClassLinkExclusionReason, true);
					classQueue.push(superClassLink);
				}
			}
		}
	}

	public boolean mergeFieldMappings(
		DualKeyMap<String, FieldSignature, FieldSignature> oldToNewFieldMappings, DualKeyMap<String, FieldSignature, FieldSignature> newToOldFieldMappings, ChangeLogContext changeLogContext
	) {
		for (int i = 0; i < this.classes.length; i++) {
			String classInternalName = this.classes[i].getInternalName();
			ArrayEnumeration<FieldNode> fieldEnumeration = this.classes[i].getFieldEnumeration();

			while (fieldEnumeration.hasMoreElements()) {
				FieldSignature fieldSignature = fieldEnumeration.nextElement().getFieldSignature();
				oldToNewFieldMappings.put(classInternalName, fieldSignature, fieldSignature);
				newToOldFieldMappings.put(classInternalName, fieldSignature, fieldSignature);
			}
		}

		boolean fieldNameChanged = false;
		if (changeLogContext != null) {
			Enumeration<String> fieldMappingClasses = changeLogContext.getFieldMappingClasses();

			while (fieldMappingClasses.hasMoreElements()) {
				String fieldMappingClassName = fieldMappingClasses.nextElement();
				if (!oldToNewFieldMappings.containsGroup(fieldMappingClassName)) {
					ArrayList<KeyValuePair<FieldSignature, FieldSignature>> fieldSignatureMappings = changeLogContext.getFieldSignatureMappings(fieldMappingClassName);

					for (int mappingIndex = 0; mappingIndex < fieldSignatureMappings.size(); mappingIndex++) {
						KeyValuePair<FieldSignature, FieldSignature> fieldSignaturePair = fieldSignatureMappings.get(mappingIndex);
						FieldSignature oldFieldSignature = fieldSignaturePair.getKey();
						FieldSignature newFieldSignature = fieldSignaturePair.getValue();
						oldToNewFieldMappings.put(fieldMappingClassName, oldFieldSignature, newFieldSignature);
						newToOldFieldMappings.put(fieldMappingClassName, newFieldSignature, oldFieldSignature);
						if (!fieldNameChanged && !oldFieldSignature.getName().equals(newFieldSignature.getName())) {
							fieldNameChanged = true;
						}
					}

					((LibraryClassNode)ClassHierarchyNode.getClassNodeByInternalName(fieldMappingClassName)).applyMappedFieldNames(oldToNewFieldMappings);
				}
			}
		}

		return fieldNameChanged;
	}

	public void validateOpenedClassHierarchy(AbstractLogger logger) {
		try {
			AtomicReference<String> errorDetails = new AtomicReference<>();
			if (!this.classTreeManager.validateOpenedHierarchy(errorDetails, this.classProvider)) {
				logger.errorWithDetails("SERIOUS ERROR:", "Gap in inheritance or implementation hierarchy.", errorDetails.get());
			}
		} catch (ClasspathException classpathException) {
			logger.info(
				"WARNING:",
				"'"
					+ classpathException.getClassName()
					+ "' not found. Class validation incomplete because classpath is incomplete or it has not been set BEFORE opening classes. You should correct the classpath and reopen classes."
			);
		} catch (FileException fileException) {
			logger.fatal("FATAL ERROR:", fileException.getMessage());
		}
	}

	private ChangeLogContext parseChangeLog(String changeLogFileName, Reader reader, AbstractLogger logger, ScriptContext scriptContext, boolean isLooseChangeLogFile) {
		ChangeLogsNodeBase changeLogRootNode = null;

		ChangeLogContext changeLogContext;
		try {
			changeLogContext = new ChangeLogContext(changeLogFileName, scriptContext, isLooseChangeLogFile);
			if (ChangeLogsParser.instance == null) {
				new ChangeLogsParser(reader);
			} else {
				ChangeLogsParser.reInit(reader);
			}

			changeLogRootNode = ChangeLogsParser.parseInput();
			changeLogRootNode.interpret(null, changeLogContext);
			if (changeLogRootNode.getChildCount() == 0) {
				logger.error("INPUT CHANGE LOG ERROR:", "\"" + changeLogFileName + "\" appears to contain no change data");
				changeLogContext = null;
			} else {
				changeLogContext.checkNameClashes();
				changeLogContext.checkPackageMappings();
				changeLogContext.initKeyTypes();
				changeLogContext.markParsed();
			}
		} catch (TokenMgrError tokenManagerError) {
			String lexicalErrorLocationHint = ChangeLogContext.buildFileLocationHint(changeLogFileName, scriptContext);
			logger.errorWithDetails("INPUT CHANGE LOG ERROR:", "Lexical error while reading \"" + changeLogFileName + "\"" + lexicalErrorLocationHint, tokenManagerError.getMessage());
			changeLogContext = null;
		} catch (ChangeLogsParserException parserException) {
			String parseErrorLocationHint = ChangeLogContext.buildFileLocationHint(changeLogFileName, scriptContext);
			logger.errorWithDetails("INPUT CHANGE LOG ERROR:", "Parse error while reading \"" + changeLogFileName + "\"" + parseErrorLocationHint, parserException.getMessage());
			changeLogContext = null;
		} finally {
			if (changeLogRootNode != null) {
				changeLogRootNode.dispose();
			}

			if (reader != null) {
				try {
					reader.close();
				} catch (IOException ignored) {
				}
			}
		}

		return changeLogContext;
	}

	public AbstractClassNode[] buildChangeLogClassSet(ChangeLogContext changeLogContext) {
		ArrayList<String> oldClassNames = changeLogContext.getOldClassNames();
		ArrayList<LibraryClassNode> libraryClassNodes = new ArrayList<>();

		for (int i = 0; i < oldClassNames.size(); i++) {
			String oldClassName = oldClassNames.get(i);
			AbstractClassNode classNode = this.classProvider.getClassNode(oldClassName, "looking for input change log classes");
			if (!classNode.isWorkspaceNode()) {
				if (this.enableVerbose) {
					changeLogContext.logMessage(
						"Class '"
							+ classNode.getBinaryName()
							+ "' no longer appears in the opened classes but its name mappings will be honored since the class was found in the classpath at '"
							+ classNode.getRawSourcePath()
							+ "'."
					);
				}

				libraryClassNodes.add((LibraryClassNode)classNode);
			}
		}

		AbstractClassNode[] allClassNodes = new AbstractClassNode[this.classes.length + libraryClassNodes.size()];
		libraryClassNodes.toArray(allClassNodes);
		System.arraycopy(this.classes, 0, allClassNodes, libraryClassNodes.size(), this.classes.length);
		this.classTreeManager.rebuildClassTree(allClassNodes);
		return allClassNodes;
	}

	public void applyAttributeStripMode(int keepGenericsInformation) {
		for (ClassNode classNode : this.classes) {
			classNode.pruneSignatureMetadata(keepGenericsInformation);
			if (classNode.hasInnerClasses()) {
				Iterator<AbstractClassNode> innerClassIterator = classNode.getInnerClasses().iterator();

				while (innerClassIterator.hasNext()) {
					((ClassNode)innerClassIterator.next()).pruneSignatureMetadata(keepGenericsInformation);
				}
			}
		}
	}

	public boolean initializeClassMappings(
		HashMap<String, String> oldToNewClassNameMap, HashMap<String, String> newToOldClassNameMap, HashMap<String, String> classNameToSourceFileMap, ChangeLogContext changeLogContext
	) {
		for (int i = 0; i < this.classes.length; i++) {
			String classInternalName = this.classes[i].getInternalName();
			oldToNewClassNameMap.put(classInternalName, classInternalName);
			newToOldClassNameMap.put(classInternalName, classInternalName);
		}

		return mergeClassMappings(oldToNewClassNameMap, newToOldClassNameMap, classNameToSourceFileMap, this.classRepository, changeLogContext);
	}

	public final ChangeLogContext loadAndMergeChangeLogs(ChangeLogFileData[] changeLogFiles, AbstractLogger logger, ScriptContext scriptContext, boolean isLooseChangeLogFile, boolean aggressiveMethodRenaming) {
		ChangeLogContext mergedChangeLogContext = null;
		int specialErrorCount = scriptContext.getSpecialErrorCount();
		boolean changeLogsMerged = false;

		for (int i = 0; i < changeLogFiles.length && specialErrorCount == scriptContext.getSpecialErrorCount(); i++) {
			ChangeLogFileData changeLogFileData = changeLogFiles[i];
			String changeLogPath = changeLogFileData.getPath();
			File changeLogFile = new File(changeLogPath);
			boolean aggressiveOverloading = ChangeLogContext.hasAggressiveOverloading(changeLogFile);
			if (aggressiveOverloading != aggressiveMethodRenaming) {
				String warningMessage = "Aggressive method overloading "
					+ (aggressiveMethodRenaming ? "is" : "is not")
					+ " set but change log '"
					+ changeLogFile.getAbsolutePath()
					+ "' was created "
					+ (aggressiveOverloading ? "with" : "without")
					+ " aggressive method overloading. This can result in name clashes.";
				scriptContext.logWarning(warningMessage);
			}

			ChangeLogContext parsedChangeLogContext = this.parseChangeLog(changeLogPath, changeLogFileData.getReader(), logger, scriptContext, isLooseChangeLogFile);
			if (mergedChangeLogContext == null) {
				mergedChangeLogContext = parsedChangeLogContext;
			} else {
				mergedChangeLogContext.mergeWith(parsedChangeLogContext, logger, scriptContext);
				changeLogsMerged = true;
			}
		}

		if (changeLogsMerged) {
			mergedChangeLogContext.checkNameClashes();
		}

		return mergedChangeLogContext;
	}

	public void pruneChangeLogByAvailability(ChangeLogContext changeLogContext, boolean allClassesOpened) {
		if (changeLogContext != null) {
			HashSet<String> packageNames = GlobalUtils.newHashSet();
			ArrayList<String> oldClassNames = changeLogContext.getOldClassNames();

			for (int i = 0; i < oldClassNames.size(); i++) {
				String oldClassName = oldClassNames.get(i);
				if (!ClassHierarchyNode.isWorkspaceClassByInternalName(oldClassName)) {
					try {
						if (!allClassesOpened && this.classRepository.loadClass(oldClassName, false) != null) {
							String libraryPackageName = AbstractClassNode.getPackageName(oldClassName);
							if (libraryPackageName.length() > 0) {
								packageNames.add(libraryPackageName);
							}
						} else {
							changeLogContext.pruneClassMapping(oldClassName);
						}
					} catch (FileException fileException) {
						throw new ProcessingException(fileException.getMessage());
					}
				} else {
					String workspacePackageName = AbstractClassNode.getPackageName(oldClassName);
					if (workspacePackageName.length() > 0) {
						packageNames.add(workspacePackageName);
					}
				}
			}

			this.packageManager.pruneMissingPackages(changeLogContext, packageNames);
		}
	}

	private boolean isProcessableWithChangeLog(AbstractClassNode classNode, DefaultExcludes excludes, ChangeLogContext changeLogContext) {
		String classInternalName = classNode.getInternalName();
		if (changeLogContext != null && changeLogContext.hasOldClass(classInternalName)) {
			return changeLogContext.isSimpleNameChanged(classInternalName);
		} else {
			return classNode.isWorkspaceNode() ? excludes.isClassProcessable((ClassNode)classNode) : false;
		}
	}

	public void applyInnerClassPolicy(int keepInnerClassInfo, HashMap<String, String> newToOldClassNameMap, FixedClassManager fixedClassManager, ScriptContext scriptContext) {
		if (keepInnerClassInfo != 0) {
			if (fixedClassManager == null && keepInnerClassInfo == 1) {
				for (ClassNode strippedClass : this.classes) {
					strippedClass.removeAllInnerClassMetadata();
					if (strippedClass.hasInnerClasses()) {
						Iterator<AbstractClassNode> strippedInnerClassIterator = strippedClass.getInnerClasses().iterator();

						while (strippedInnerClassIterator.hasNext()) {
							((ClassNode)strippedInnerClassIterator.next()).removeAllInnerClassMetadata();
						}
					}
				}

				ClassHierarchyNode.resetAllLinks();
				this.classTreeManager.clearValidInnerClassNodes();
			} else {
				HashSet<ClassNode> classesToPrune = GlobalUtils.newHashSet();
				Enumeration<ClassHierarchyNode> validInnerClassNodes = this.classTreeManager.getValidInnerClassNodes();

				while (validInnerClassNodes.hasMoreElements()) {
					ClassHierarchyNode classHierarchyNode = validInnerClassNodes.nextElement();
					ClassNode workspaceClassNode = classHierarchyNode.getClassNodeIfWorkspace();
					String classInternalName = classHierarchyNode.getInternalName();
					if (workspaceClassNode != null && classHierarchyNode.hasSuperClass()) {
						if (keepInnerClassInfo == 1) {
							classesToPrune.add(workspaceClassNode);
						} else if (keepInnerClassInfo == 2
							&& newToOldClassNameMap.containsKey(classInternalName)
							&& !AbstractClassNode.getSimpleNameFromInternalName(newToOldClassNameMap.get(classInternalName)).equals(AbstractClassNode.getSimpleNameFromInternalName(classInternalName))) {
							classesToPrune.add(workspaceClassNode);
						}

						if (fixedClassManager != null && fixedClassManager.isInFixedClasses(workspaceClassNode) && classesToPrune.contains(workspaceClassNode)) {
							classesToPrune.remove(workspaceClassNode);
							String binaryClassName = GlobalUtils.internalToBinaryName(GlobalUtils.getMappedOrDefault(classInternalName, newToOldClassNameMap));
							scriptContext.logWarning("Inner class information will not be removed from class '" + binaryClassName + "' because it is a fixed class.");
						}
					}
				}

				for (ClassNode classToPrune : new ArrayList<>(classesToPrune)) {
					if (classToPrune.hasInnerClasses()) {
						for (AbstractClassNode innerClassNode : classToPrune.getInnerClasses()) {
							classesToPrune.add((ClassNode)innerClassNode);
						}
					}
				}

				for (ClassNode prunedClass : this.classes) {
					prunedClass.pruneInnerClassAttributes(classesToPrune);
					if (prunedClass.hasInnerClasses()) {
						Iterator<AbstractClassNode> prunedInnerClassIterator = prunedClass.getInnerClasses().iterator();

						while (prunedInnerClassIterator.hasNext()) {
							((ClassNode)prunedInnerClassIterator.next()).pruneInnerClassAttributes(classesToPrune);
						}
					}
				}

				ClassHierarchyNode.pruneRemovedNodes(classesToPrune);
				this.classTreeManager.removeInvalidInnerClassNodes();
			}
		}
	}
}
