package com.zelix.workspace;

import com.zelix.enums.MixCaseType;
import com.zelix.event.ChangeListener;
import com.zelix.event.ChangeTrackingNotifier;
import com.zelix.event.EventRegistry;
import com.zelix.event.INotifier;
import com.zelix.exceptions.*;
import com.zelix.framework.AbstractClassNode;
import com.zelix.framework.AbstractFieldNode;
import com.zelix.framework.AbstractMethodNode;
import com.zelix.framework.StructureNode;
import com.zelix.framework.analyzer.MethodAnalyzer;
import com.zelix.framework.analyzer.dataset.MethodAnalysisResult;
import com.zelix.framework.analyzer.reference.Reference;
import com.zelix.framework.analyzer.reference.StringReference;
import com.zelix.framework.attributes.UnknownAttribute;
import com.zelix.framework.code.InstructionListNode;
import com.zelix.framework.code.MethodInvocation;
import com.zelix.framework.constant.StringNode;
import com.zelix.framework.hierarchy.TypeResolver;
import com.zelix.framework.manifest.ManifestFile;
import com.zelix.framework.modules.ModuleInfoNode;
import com.zelix.framework.reference.BitSetReferenceGraph;
import com.zelix.framework.reference.IReferenceGraph;
import com.zelix.framework.reference.ReferenceGraph;
import com.zelix.framework.signature.*;
import com.zelix.framework.tree.ArchiveNode;
import com.zelix.framework.tree.ClasspathResource;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.wrappers.FieldNode;
import com.zelix.framework.wrappers.LibraryClassNode;
import com.zelix.framework.wrappers.MethodNode;
import com.zelix.framework.writer.ClassInfoWriter;
import com.zelix.framework.writer.base.AbstractMethodCallWriter;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.framework.writer.reflect.ReflectionMethodPattern;
import com.zelix.klassmaster.KlassMaster;
import com.zelix.klassmaster.config.Options;
import com.zelix.klassmaster.config.TrimConfig;
import com.zelix.klassmaster.constant.CopyrightConstants;
import com.zelix.klassmaster.constant.SystemConstants;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.statements.annotation.ComplexAnnotationSpecifier;
import com.zelix.klassmaster.script.statements.base.ExclusionInclusionStatement;
import com.zelix.klassmaster.script.statements.exclusion.*;
import com.zelix.logging.AbstractLogger;
import com.zelix.repository.ClassLoadingStatus;
import com.zelix.repository.ClassProvider;
import com.zelix.repository.ClassRepository;
import com.zelix.repository.ClasspathManager;
import com.zelix.tools.Obfuscator;
import com.zelix.tools.excludes.ExcludeElement;
import com.zelix.tools.excludes.FixedClassManager;
import com.zelix.tools.excludes.SerializedClassManager;
import com.zelix.tools.removeCall.RemoveMethodCallExcludes;
import com.zelix.tools.renamer.packages.PackageRenameMap;
import com.zelix.tools.trim.TrimExcludes;
import com.zelix.utils.*;
import com.zelix.utils.collections.*;
import com.zelix.utils.collections.readOnly.ReadOnlyMap;
import com.zelix.utils.collections.readOnly.ReadOnlyRegistryView;
import com.zelix.utils.dataset.AtomicInt;
import com.zelix.utils.dataset.ByteDataBuffer;
import com.zelix.utils.dataset.ChangeLogFileData;
import com.zelix.utils.dataset.Triplet;
import com.zelix.utils.enumerations.ArrayEnumeration;
import com.zelix.utils.enumerations.SnapshotEnumerator;
import com.zelix.utils.function.ZKMConsumer;
import com.zelix.utils.references.AtomicReference;
import com.zelix.workspace.resources.ResourceFileAnalyzer;
import com.zelix.workspace.resources.ResourceIndex;
import com.zelix.workspace.writer.JadFileProcessor;
import com.zelix.workspace.writer.WorkspaceWriter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;

public class Workspace extends ChangeTrackingNotifier implements AbstractWorkspace, ChangeListener {
	private final KlassMaster klassMaster;
	private static Integer ARCHIVE_TYPE_CLDC10 = 1;
	private static Integer ARCHIVE_TYPE_MICRO_EDITION = 2;
	public static final Boolean BOOLEAN_TRUE = Boolean.TRUE;
	public static final Boolean BOOLEAN_FALSE = Boolean.FALSE;
	private static IntegerCache INTEGER_CACHE = IntegerCache.getInstance();
	private final DualKeyMap<String, String, Boolean> superClassHierarchyCache = Options.ZKM_USE_PARALLEL ? new DualKeyMap<>(true) : new DualKeyMap<>();
	private final DualKeyMap<String, String, Boolean> interfaceHierarchyCache = Options.ZKM_USE_PARALLEL ? new DualKeyMap<>(true) : new DualKeyMap<>();
	private final ReferenceGraph<String, String> annotationReferenceGraph = new ReferenceGraph<>(Options.ZKM_USE_PARALLEL);
	private final PackageRenameMap packageRenameMap = new PackageRenameMap();
	private Map<String, String> classNameToInternalNameMap = GlobalUtils.newHashMap();
	private Map<String, String> internalToBinaryNameMap = GlobalUtils.newHashMap();
	private DualKeyMap<AbstractClassNode, FieldSignature, FieldSignature> fieldRegistry = new DualKeyMap<>();
	private DualKeyMap<AbstractClassNode, MethodSignature, MethodSignature> methodRegistry = new DualKeyMap<>();
	private DualKeyMap<AbstractClassNode, Reference, Integer> stringClassReferenceMap = new DualKeyMap<>(101, 7);
	private MultiMap<String, Reference> stringResourcePathReferenceMap = new MultiMap<>();
	private DualKeyMap<String, Reference, Reference> stringInternalClassNameReferenceMap = new DualKeyMap<>();
	private DualKeyMap<AbstractFieldNode, Reference, Reference> stringFieldNameReferenceMap = new DualKeyMap<>();
	private DualKeyMap<AbstractMethodNode, Reference, Reference> stringMethodNameReferenceMap = new DualKeyMap<>();
	private HashSet<StringNode> processedStringNodes = GlobalUtils.newHashSet();
	private MultiMap<MethodNode, StructureNode> usageReferenceMap = new MultiMap<>();
	private MultiMap<MethodNode, MethodInvocation> methodInvocationMap = new MultiMap<>();
	private AtomicReference<ClassNode> referenceObfuscationDataClassNode = new AtomicReference<>();
	private Map<ArchiveNode, ClassNode> archiveToAutoReflectionMap = GlobalUtils.newHashMap();
	private Map<ArchiveNode, ClassNode> archiveToReferenceObfuscationMap = GlobalUtils.newHashMap();
	private AtomicReference<ClassNode> autoReflectionDataClassNode = new AtomicReference<>();
	private AtomicReference<Triplet> defaultMpcClassTripletRef = new AtomicReference<>();
	private Map<ArchiveNode, Triplet> archiveToMpcClassTripletMap = GlobalUtils.newHashMap();
	private HashSet<String> resourceFilePaths = GlobalUtils.newHashSet();
	private final Set<MethodNode> indyReferencedMethods = Options.ZKM_USE_PARALLEL
		? Collections.newSetFromMap(new ConcurrentHashMap<>())
		: GlobalUtils.newHashSet();
	private final Set<MethodNode> bootstrapReferencedMethods = Options.ZKM_USE_PARALLEL
		? Collections.newSetFromMap(new ConcurrentHashMap<>())
		: GlobalUtils.newHashSet();
	private final Set<String> integrityCheckClassHashes = new LinkedHashSet<>();
	private HashMap<JadFileProcessor, ArchiveNode> jadFileToArchiveNodeMap = GlobalUtils.newHashMap(13);
	private ResourceIndex fileAdaptor = new ResourceIndex();
	private Map<MethodSignature, AbstractMethodNode> objectMethods = GlobalUtils.newHashMap();
	private AtomicReference<String> status;
	private ClasspathManager classpathManager;
	private final ClassRepository classRepository;
	private final ClassProvider classProvider;
	private boolean verbose;
	private final ClassRepository runtimeClassRepository;
	private MultiMap<String, ArchiveNode> moduleNameToArchiveMultiMap;
	private IReferenceGraph<ClassNode, ClassNode> autoReflectionReferenceGraph;
	private DualKeyMap<ArchiveNode, String, ClasspathResource> deletedResources;
	private IReferenceGraph<ClassNode, ClassNode> mpcReferenceGraph;
	private Map<ArchiveNode, String> archiveToModuleNameMap;
	private DualKeyMap<ArchiveNode, String, ClasspathResource> skippedPropertiesResources;
	private boolean classpathChanged;
	private DualKeyMap<ClassNode, MethodSignature, MethodNode> classToMethodSignatureRegistry;
	private DualKeyMap<String, String, Boolean> superClassHierarchySnapshot;
	private boolean longConstantsEncrypted;
	private boolean integerConstantsEncrypted;
	private HashMap<JadFileProcessor, File> customJadOutputPaths;
	private IReferenceGraph<ClassNode, ClassNode> obfuscateReferencesGraph;
	private boolean methodNameObfFinish;
	private ClassTreeManager classTreeManager;
	private DualKeyMap<MethodNode, String, Object> methodAccessorReferences;
	private Map<ClassNode, String> resourceReferencedClasses;
	private HierarchicalMultiMap<ClassNode, MethodParameterKey, MethodNode> classMethodParameterKeyRegistry;
	private DualKeyMap<String, String, Boolean> interfaceHierarchySnapshot;
	private HashMap<ClasspathResource, File> customYamlOutputPaths;
	private boolean hasNonModularSources;
	private ArchiveNode[] allArchiveNodes;
	private HashMap<ArchiveNode, File> archiveToOutputFileMap;
	private ModuleInfoNode[] moduleInfoNodes;
	private TripleKeyMap<String, ClassNode, FieldNode, FieldNode> fieldRegistryByName;
	private DualKeyMap<FieldSignature, ClassNode, FieldNode> fieldSignatureRegistry;
	private boolean hasHierarchySnapshot;
	private Map<FieldNode, String> resourceReferencedFields;
	private Map<MethodNode, String> resourceProtectedStaticMethods;
	private boolean fieldNameObfFinish;
	private DualKeyMap<ArchiveNode, String, ClasspathResource> skippedYamlResources;
	private PackageManager packageManager;
	private HierarchicalMultiMap<String, MethodSignature, ClassNode> classLookupByMethodSignature;
	private DualKeyMap<ArchiveNode, String, ClasspathResource> skippedClassResources;
	private DualKeyMap<ArchiveNode, String, ClasspathResource> skippedXmlResources;
	private boolean methodParameterObfuscated;
	private List<String> overrideFieldNames;
	private List<String> overrideMethodNames;
	private ReferenceGraph<String, String> annotationReferenceSnapshot;
	private String errorMessage;
	private ClassNode[] workspaceClasses;
	private HashMap<ClasspathResource, File> customXmlOutputPaths;
	private Set<ArchiveNode> nestedArchives;
	private Map<MethodNode, String> resourceReferencedMethods;
	private ClasspathResource[] xmlResources;
	private MultiMap<ArchiveNode, ClassNode> archiveToClassesMultiMap;
	private HashMap<ClasspathResource, File> customPropertiesOutputPaths;
	private TripleKeyMap<String, String, String, FieldNode> classToFieldRegistry;
	private ClasspathResource[] propertiesResources;
	private ClasspathResource[] yamlResources;

	public Workspace(
		AtomicReference<String> statusReference,
		ClasspathManager classpathManager,
		boolean verbose,
		boolean allowSaving,
		boolean unix,
		boolean enableImplementsClauseMatching,
		KlassMaster klassMaster
	) {
		this.status = statusReference;
		this.classpathManager = classpathManager;
		this.classRepository = new ClassRepository(this.classpathManager, unix);
		this.classProvider = new ClassProvider(this.classRepository);
		this.classpathManager.subscribe(this);
		this.verbose = verbose;
		this.klassMaster = klassMaster;
		ClasspathManager bootClasspath = new ClasspathManager(ChangeTrackingNotifier.DEFAULT_CLASS_PATH);
		this.runtimeClassRepository = new ClassRepository(bootClasspath, unix);
		bootClasspath.initializeBootClasspath(new AtomicReference<>(), new AtomicReference<>());
		TempFileManager.cleanupOldTempFiles();
	}

	public static void buildClassTree(String className, int maxClasses, int resourceCount, Set<String> resourceSet, ClassRepository classRepository, List<String> sourcePaths, boolean includeBootClasses) {
		if (resourceSet.size() == resourceCount || className == null) {
			int classCount = 0;

			for (AbstractClassNode classNode = loadClassNode(classRepository, className.replace('.', '/'), includeBootClasses); classNode != null && classCount < maxClasses; classCount++) {
				if (sourcePaths != null) {
					sourcePaths.add(classNode.getRawSourcePath());
				}

				byte[] classHash = classNode.getClassHash();
				resourceSet.add(GlobalUtils.toHex(classHash));
				String superclassName = classNode.getSuperclassName();
				classNode = loadClassNode(classRepository, superclassName, includeBootClasses);
			}
		}
	}

	public static boolean isSameSignatures(AbstractMethodNode method1, AbstractMethodNode method2, TypeResolver typeResolver) {
		List<String> parameterDescriptors1 = TypeWriter.parseParameterDescriptors(method1.getDescriptor());
		List<String> parameterDescriptors2 = TypeWriter.parseParameterDescriptors(method2.getDescriptor());
		if (!method1.getName().equals(method2.getName())) {
			return false;
		} else if (parameterDescriptors1.size() != parameterDescriptors2.size()) {
			return false;
		} else {
			try {
				for (int i = 0; i < parameterDescriptors1.size(); i++) {
					String parameterDescriptor1 = parameterDescriptors1.get(i);
					String parameterDescriptor2 = parameterDescriptors2.get(i);
					if (!typeResolver.isAssignable(parameterDescriptor1, parameterDescriptor2)) {
						return false;
					}
				}

				return typeResolver.isAssignable(method1.getReturnDescriptor(), method2.getReturnDescriptor());
			} catch (FileException ignored) {
				return false;
			}
		}
	}

	private static AbstractClassNode loadClassNode(ClassRepository classRepository, String internalName, boolean includeBootClasses) {
		LibraryClassNode classNode = null;
		if (!includeBootClasses) {
			classNode = classRepository.loadClassFromArchive(internalName);
		}

		if (classNode == null) {
			classNode = classRepository.findClass(internalName, !includeBootClasses);
		}

		return classNode;
	}

	public Map<FieldNode, String> getClassPackageMap() {
		return this.resourceReferencedFields;
	}

	@Override
	public final synchronized Enumeration<ClassNode> getClassesByMethodSignature(MethodSignature methodSignature) {
		List<ClassNode> classNodes = this.classLookupByMethodSignature.getValues(methodSignature.getName(), methodSignature);
		return classNodes != null ? Collections.enumeration(classNodes) : null;
	}

	public boolean isClasspathChanged() {
		return this.classpathChanged;
	}

	public void refreshFieldRefsAndConstants(AbstractClassNode[] classNodes, ScriptContext scriptContext) {
		for (int i = 0; i < classNodes.length; i++) {
			AbstractClassNode classNode = classNodes[i];
			classNode.refreshFieldRefsAndConstants(scriptContext, this);
			if (classNode.hasInnerClasses()) {
				Iterator<AbstractClassNode> innerClassIterator = classNode.getInnerClasses().iterator();

				while (innerClassIterator.hasNext()) {
					innerClassIterator.next().refreshFieldRefsAndConstants(scriptContext, this);
				}
			}
		}
	}

	@Override
	public final boolean isInstanceOf(String className, String target, ComplexAnnotationSpecifier annotationSpecifier) {
		try {
			boolean targetHasWildcard = target.indexOf(42) != -1;
			return this.checkInheritanceHierarchy(className, target, targetHasWildcard, annotationSpecifier);
		} catch (StackOverflowError stackOverflowError) {
			AbstractClassNode classNode = this.classProvider.getClassNode(className, "reporting corrupt inheritance hierarchy");
			throw new ZKMUnexpectedException(stackOverflowError.getMessage() + " : Class " + classNode.getSourcePath() + " has corrupt inheritance hierarchy (B)", stackOverflowError);
		}
	}

	private void collectMemberAccesses(
		HierarchicalMultiMap<FieldID, InstructionListNode, Integer> fieldReadInstructions,
		HierarchicalMultiMap<FieldID, InstructionListNode, Integer> fieldWriteInstructions,
		HierarchicalMultiMap<MethodID, InstructionListNode, Integer> methodCallInstructions
	) {
		int classCount = this.workspaceClasses.length;

		for (int i = 0; i < classCount; i++) {
			this.workspaceClasses[i].collectMemberAccesses(fieldReadInstructions, fieldWriteInstructions, methodCallInstructions, INTEGER_CACHE);
		}
	}

	public void methodNameObfFinish() {
		this.methodNameObfFinish = true;
	}

	private void scanArchiveResources() throws IOException {
		for (int archiveIndex = 0; archiveIndex < this.allArchiveNodes.length; archiveIndex++) {
			ArchiveNode archiveNode = this.allArchiveNodes[archiveIndex];
			if (this.nestedArchives == null || !this.nestedArchives.contains(archiveNode)) {
				ZipFileWrapper zipFile = null;

				try {
					zipFile = new ZipFileWrapper(archiveNode.getFilePath());
					Enumeration<? extends ZipEntry> entryEnumeration = zipFile.entries();

					while (entryEnumeration.hasMoreElements()) {
						ZipEntry zipEntry = entryEnumeration.nextElement();
						if (!zipEntry.isDirectory()) {
							String resourceName = zipEntry.getName();
							if (!resourceName.endsWith(".class")) {
								this.resourceFilePaths.add(resourceName);
								if (FileUtils.isXmlFile(resourceName)) {
									this.fileAdaptor.addResourceArchive(resourceName, archiveNode);
								}
							}
						}
					}
				} finally {
					if (zipFile != null) {
						zipFile.close();
					}
				}
			}
		}

		List<String> extraXmlFileExtensions = FileUtils.getExtraXmlFileExtensions();
		if (extraXmlFileExtensions != null) {
			for (String extraXmlFileExtension : extraXmlFileExtensions) {
				if (extraXmlFileExtension.indexOf("*") != 0) {
					throw new ProcessingException(
						"All elements in 'ZKM_EXTRA_XML_FILE_TYPES' System property must start with the '*' wildcard. : '"
							+ extraXmlFileExtension
							+ "' : '"
							+ Options.ZKM_EXTRA_XML_FILE_TYPES
							+ "'"
					);
				}
			}
		}
	}

	public void buildNameMappings(AbstractClassNode[] classNodes) {
		this.classNameToInternalNameMap = GlobalUtils.newHashMap();
		this.fieldRegistry = new DualKeyMap<>();
		this.methodRegistry = new DualKeyMap<>();

		for (AbstractClassNode classNode : classNodes) {
			String className = classNode.getClassName();
			String internalName = classNode.getInternalName();
			this.classNameToInternalNameMap.put(className, internalName);

			for (AbstractFieldNode field : classNode.getFields()) {
				this.fieldRegistry.put(classNode, field.getOriginalFieldSignature(), field.getFieldSignature());
			}

			for (AbstractMethodNode method : classNode.getMethods()) {
				this.methodRegistry.put(classNode, method.getOriginalMethodSignature(), method.getMethodSignature());
			}
		}

		this.internalToBinaryNameMap = GlobalUtils.getInverseMap(this.classNameToInternalNameMap);
	}

	public final boolean isEmpty() {
		return this.workspaceClasses == null || this.workspaceClasses.length == 0 && (this.moduleInfoNodes == null || this.moduleInfoNodes.length == 0);
	}

	public ClasspathResource[] getLogFiles() {
		ClasspathResource[] logFilesCopy = new ClasspathResource[this.propertiesResources.length];
		System.arraycopy(this.propertiesResources, 0, logFilesCopy, 0, this.propertiesResources.length);
		return logFilesCopy;
	}

	public HashMap<ClassNode, Boolean> detectMicroEditionClasses() {
		HashMap<ClassNode, Boolean> microEditionByClass = GlobalUtils.newHashMap();
		int classCount = this.workspaceClasses.length;
		HashMap<ArchiveNode, Integer> archiveTypeByArchive = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(this.allArchiveNodes.length));

		for (int archiveIndex = 0; archiveIndex < this.allArchiveNodes.length; archiveIndex++) {
			ArchiveNode archiveNode = this.allArchiveNodes[archiveIndex];
			if (this.nestedArchives == null || !this.nestedArchives.contains(archiveNode)) {
				try {
					if (archiveNode.isCldc10()) {
						archiveTypeByArchive.put(archiveNode, ARCHIVE_TYPE_CLDC10);
					} else if (archiveNode.isMicroEdition()) {
						archiveTypeByArchive.put(archiveNode, ARCHIVE_TYPE_MICRO_EDITION);
					}
				} catch (ZKMException zkmException) {
					throw new ProcessingException(zkmException.getMessage());
				}
			}
		}

		boolean anySourceUnarchived = false;

		for (int midletIndex = 0; midletIndex < classCount; midletIndex++) {
			try {
				if (this.isClassInstanceOf(this.workspaceClasses[midletIndex].getInternalName(), "javax/microedition/midlet/MIDlet")) {
					Enumeration<ClasspathResource> midletClassSources = this.workspaceClasses[midletIndex].getClassSources();

					while (midletClassSources.hasMoreElements()) {
						ArchiveNode midletSourceArchive = midletClassSources.nextElement().getArchiveNode();
						if (midletSourceArchive != null) {
							if (!archiveTypeByArchive.containsKey(midletSourceArchive)) {
								archiveTypeByArchive.put(midletSourceArchive, ARCHIVE_TYPE_CLDC10);
							}
						} else {
							anySourceUnarchived = true;
						}
					}
				}
			} catch (ClasspathException ignoredClasspathException) {
			} catch (FileException ignoredFileException) {
			}
		}

		for (int classIndex = 0; classIndex < classCount; classIndex++) {
			boolean archiveTypeKnown = false;
			boolean microEditionDetected = true;
			boolean sourceUnarchived = false;
			Enumeration<ClasspathResource> classSources = this.workspaceClasses[classIndex].getClassSources();

			while (classSources.hasMoreElements()) {
				ArchiveNode sourceArchive = classSources.nextElement().getArchiveNode();
				if (sourceArchive != null) {
					Integer archiveType = archiveTypeByArchive.get(sourceArchive);
					if (archiveType != null) {
						archiveTypeKnown = true;
						if (archiveType == ARCHIVE_TYPE_CLDC10) {
							microEditionDetected = false;
						}
					}
				} else {
					sourceUnarchived = true;
				}
			}

			if (sourceUnarchived && anySourceUnarchived && !archiveTypeKnown) {
				archiveTypeKnown = true;
				microEditionDetected = false;
			}

			if (archiveTypeKnown) {
				microEditionByClass.put(this.workspaceClasses[classIndex], microEditionDetected ? Boolean.TRUE : Boolean.FALSE);
			}
		}

		return microEditionByClass;
	}

	public Set<MethodNode> getProcessedPackageResources() {
		return this.indyReferencedMethods;
	}

	@Override
	public boolean hasHierarchySnapshot() {
		return this.hasHierarchySnapshot;
	}

	public void setCLDCVersionMap(HashMap<ArchiveNode, File> cldcVersionMap) {
		this.archiveToOutputFileMap = cldcVersionMap;
	}

	public final void rebuildFieldRegistries() {
		this.fieldRegistryByName = new TripleKeyMap<>(Math.max(15, this.workspaceClasses.length * 5));
		this.fieldSignatureRegistry = new DualKeyMap<>(Math.max(15, this.workspaceClasses.length * 5));
		this.classToFieldRegistry = new TripleKeyMap<>(this.workspaceClasses.length, 5);

		for (int i = 0; i < this.workspaceClasses.length; i++) {
			this.workspaceClasses[i].registerFieldsInWorkspace(this);
		}
	}

	public final void processReferences(HashMap<String, String> originalToNewNameMap, HashMap<String, String> packageNameMappings, AbstractLogger logger) {
		this.remapAllReferences(this.workspaceClasses, this.moduleInfoNodes, 0, 0, originalToNewNameMap, packageNameMappings, logger);
	}

	private String getSuperclassName(String internalName) {
		ClassHierarchyNode classHierarchyNode = ClassHierarchyNode.getNodeByInternalName(GlobalUtils.getMappedOrDefault(internalName, this.classNameToInternalNameMap));
		String superclassName;
		AbstractClassNode classNode;
		if (classHierarchyNode != null && !classHierarchyNode.isMissing()) {
			classNode = classHierarchyNode.getClassNode();
			superclassName = classHierarchyNode.getOuterClass().getClassNameOrInternalName();
		} else {
			classNode = this.classRepository.loadClassOrThrow(internalName);
			superclassName = classNode.getSuperclassName();
		}

		String validationError;
		if (Options.ZKM_TEST_HIERARCHY
			&& !classNode.isGeneratedClass()
			&& (
					validationError = ClassHierarchyNode.validateInheritanceLink(
						classNode, this.classProvider.getClassNode(GlobalUtils.getMappedOrDefault(superclassName, this.classNameToInternalNameMap)), 1
					)
				)
				!= null) {
			throw new LookupException(validationError);
		} else {
			this.superClassHierarchySnapshot.put(superclassName, internalName, BOOLEAN_TRUE);
			return superclassName;
		}
	}

	public void createHierarchySnapshot() {
		if (!this.hasHierarchySnapshot) {
			this.superClassHierarchySnapshot = this.superClassHierarchyCache.clone();
			this.interfaceHierarchySnapshot = this.interfaceHierarchyCache.clone();
			this.annotationReferenceSnapshot = this.annotationReferenceGraph.deepClone();
			this.hasHierarchySnapshot = true;
		}

		this.clearHierarchyCaches();
	}

	public String getInternalToBinaryName(String internalName) {
		return this.internalToBinaryNameMap.get(internalName);
	}

	@Override
	public final boolean isResourceRegistered(String internalName, String fieldName) {
		return this.classToFieldRegistry.containsGroup(internalName, fieldName);
	}

	public void updateStringReferences(HashMap<String, String> newToOriginalNameMap, HashMap<String, String> originalToNewNameMap, HashMap<String, String> packageNameMappings, ScriptContext scriptContext) {
		HashMap<StringNode, ExcludeElement> excludeElements = GlobalUtils.newHashMap();
		DualKeyMap<AbstractClassNode, StringNode, String> classAccessStrings = new DualKeyMap<>();

		for (AbstractClassNode classNode : this.stringClassReferenceMap.getGroupKeySet()) {
			if (newToOriginalNameMap.containsKey(classNode.getInternalName())) {
				Map<Reference, Integer> arrayDimensionsByReference = this.stringClassReferenceMap.getGroup(classNode);

				for (Reference reference : arrayDimensionsByReference.keySet()) {
					if (reference instanceof StringReference) {
						StringNode classStringNode = ((StringReference)reference).getStringNode();
						classAccessStrings.put(classNode, classStringNode, classStringNode.getDisplayValue());
						Integer arrayDimension = arrayDimensionsByReference.get(reference);
						String newStringValue;
						if (arrayDimension == 0) {
							newStringValue = classNode.getBinaryName();
						} else {
							StringBuffer valueBuffer = new StringBuffer();
							valueBuffer.append(StringUtils.justificateString("", 76, arrayDimension, 91));
							valueBuffer.append("L");
							valueBuffer.append(classNode.getBinaryName());
							valueBuffer.append(";");
							newStringValue = valueBuffer.toString();
						}

						this.updateStringReferencesOnRename(excludeElements, classStringNode, classStringNode.getValueAsString(), newStringValue, classNode, newToOriginalNameMap, scriptContext);
						classStringNode.setValueFromString(newStringValue);
					}
				}
			}
		}

		ReadOnlyMap<String, String> readOnlyPackageNameMappings = new ReadOnlyMap<>(packageNameMappings);
		Enumeration<String> resourcePathEnumeration = this.stringResourcePathReferenceMap.keys();

		while (resourcePathEnumeration.hasMoreElements()) {
			String resourcePath = resourcePathEnumeration.nextElement();
			boolean simpleResourceName = resourcePath.indexOf(47) == -1;
			String resourceClassName = GlobalUtils.binaryToInternalName(resourcePath);
			String mappedClassName = originalToNewNameMap.get(resourceClassName);
			AbstractClassNode mappedClassNode = null;
			if (mappedClassName != null) {
				mappedClassNode = ClassHierarchyNode.getClassNodeByInternalName(mappedClassName);
			}

			if (AbstractClassNode.getPackageName(resourceClassName).length() > 0) {
				List<Reference> references = this.stringResourcePathReferenceMap.get(resourcePath);

				for (int i = 0; i < references.size(); i++) {
					Reference resourceReference = references.get(i);
					if (resourceReference instanceof StringReference) {
						StringNode resourceStringNode = ((StringReference)resourceReference).getStringNode();
						String newResourcePath = PathRemapper.remapDirectoryOfPath(resourceClassName, readOnlyPackageNameMappings);
						if (mappedClassNode != null
							&& mappedClassNode.isWorkspaceNode()
							&& classAccessStrings.contains((ClassNode)mappedClassNode, resourceStringNode)
							&& !mappedClassNode.getInternalName().equals(GlobalUtils.binaryToInternalName(newResourcePath))) {
							scriptContext.logSpecialError(
								"The one String '"
									+ classAccessStrings.get((ClassNode)mappedClassNode, resourceStringNode)
									+ "' is used to access both a resource and the class '"
									+ mappedClassNode.getBinaryName()
									+ "'.  The class is renamed but the resource cannot be renamed. The class name must be excluded from obfuscation."
							);
						}

						if (!newResourcePath.equals(resourceClassName)) {
							if (simpleResourceName) {
								newResourcePath = GlobalUtils.internalToBinaryName(newResourcePath);
							}

							resourceStringNode.setValueFromString(newResourcePath);
						}
					}
				}
			}
		}

		Enumeration<String> internalClassNameEnumeration = this.stringInternalClassNameReferenceMap.getGroupKeys();

		while (internalClassNameEnumeration.hasMoreElements()) {
			String internalClassName = internalClassNameEnumeration.nextElement();
			if (packageNameMappings.containsKey(internalClassName)) {
				for (Reference internalClassReference : this.stringInternalClassNameReferenceMap.getGroup(internalClassName).keySet()) {
					if (internalClassReference instanceof StringReference) {
						StringNode internalClassStringNode = ((StringReference)internalClassReference).getStringNode();
						String newBinaryName = GlobalUtils.internalToBinaryName(packageNameMappings.get(internalClassName));
						internalClassStringNode.setValueFromString(newBinaryName);
					}
				}
			}
		}

		Enumeration<AbstractFieldNode> fieldEnumeration = this.stringFieldNameReferenceMap.getGroupKeys();

		while (fieldEnumeration.hasMoreElements()) {
			AbstractFieldNode field = fieldEnumeration.nextElement();

			for (Reference fieldReference : this.stringFieldNameReferenceMap.getGroup(field).keySet()) {
				if (fieldReference instanceof StringReference) {
					StringNode fieldStringNode = ((StringReference)fieldReference).getStringNode();
					if (!field.getNameDisplay().equals(fieldStringNode.getDisplayValue())) {
						this.updateStringReferencesOnRename(excludeElements, fieldStringNode, fieldStringNode.getValueAsString(), field.getNameDisplay(), field, newToOriginalNameMap, scriptContext);
						fieldStringNode.setValueFromString(field.getNameDisplay());
					} else {
						this.updateStringReferencesOnRename(excludeElements, fieldStringNode, fieldStringNode.getValueAsString(), field.getNameDisplay(), field, newToOriginalNameMap, scriptContext);
					}
				}
			}
		}

		Enumeration<AbstractMethodNode> methodEnumeration = this.stringMethodNameReferenceMap.getGroupKeys();

		while (methodEnumeration.hasMoreElements()) {
			AbstractMethodNode method = methodEnumeration.nextElement();

			for (Reference methodReference : this.stringMethodNameReferenceMap.getGroup(method).keySet()) {
				if (methodReference instanceof StringReference) {
					StringReference methodStringReference = (StringReference)methodReference;
					StringNode methodStringNode = methodStringReference.getStringNode();
					if (methodStringReference.isNameAndDescriptor()) {
						if (!method.getNameAndDescriptor().equals(methodStringNode.getDisplayValue())) {
							methodStringNode.setValueFromString(method.getNameAndDescriptor());
						}
					} else if (methodStringReference.hasAccessorPrefix()) {
						String accessorPrefix = methodStringReference.getAccessorPrefix();
						String methodName = method.getName();
						if (methodName.startsWith(accessorPrefix) && methodName.length() > accessorPrefix.length()) {
							String accessorFieldName = MethodSignature.getFieldNameFromAccessor(method.getName(), accessorPrefix);
							methodStringNode.setValueFromString(accessorFieldName);
						} else {
							scriptContext.logWarning(
								"Could not update property '"
									+ methodStringNode.getValueAsString()
									+ "' to reflect method '"
									+ method.getOriginalNameWithParams()
									+ "' in class '"
									+ method.getBinaryName()
									+ "' which has been renamed to '"
									+ method.getNameDisplay()
									+ "'"
							);
						}
					} else if (!method.getName().equals(methodStringNode.getDisplayValue())) {
						this.updateStringReferencesOnRename(excludeElements, methodStringNode, methodStringNode.getValueAsString(), method.getName(), method, newToOriginalNameMap, scriptContext);
						methodStringNode.setValueFromString(method.getName());
					} else {
						this.updateStringReferencesOnRename(excludeElements, methodStringNode, methodStringNode.getValueAsString(), method.getName(), method, newToOriginalNameMap, scriptContext);
					}
				}
			}
		}
	}

	@Override
	public boolean isObjectMethodSignature(MethodSignature methodSignature) {
		return this.objectMethods.containsKey(methodSignature);
	}

	public void setCustomYamlOutputPaths(HashMap<ClasspathResource, File> customArchiveOutputPaths) {
		this.customYamlOutputPaths = customArchiveOutputPaths;
	}

	public boolean markMethodParameterObfuscated() {
		return this.methodParameterObfuscated = true;
	}

	private boolean checkInheritanceHierarchy(String className, String target, boolean targetIsWildcard, ComplexAnnotationSpecifier annotationSpecifier) {
		if (className.equals("java/lang/Object")) {
			return false;
		} else if (className.equals(target)) {
			return false;
		} else {
			String superclassName = this.getSuperclassInternalName(className);
			if (!targetIsWildcard && superclassName.equals(target)) {
				return annotationSpecifier.matchesAnnotationTypes(this.getAnnotationTypes(superclassName, null, false));
			} else if (targetIsWildcard && StringUtils.matchesWildcard(superclassName, target) && annotationSpecifier.matchesAnnotationTypes(this.getAnnotationTypes(superclassName, null, false))) {
				return true;
			} else {
				return superclassName.equals("java/lang/Object") ? false : this.checkInheritanceHierarchy(superclassName, target, targetIsWildcard, annotationSpecifier);
			}
		}
	}

	private Enumeration<String> getSnapshotInterfaceNames(String internalName, AtomicReference<AbstractClassNode> outClassNode) {
		ClassHierarchyNode classHierarchyNode = ClassHierarchyNode.getNodeByInternalName(GlobalUtils.getMappedOrDefault(internalName, this.classNameToInternalNameMap));
		AbstractClassNode classNode;
		String[] interfaceNames;
		if (classHierarchyNode != null && !classHierarchyNode.isMissing()) {
			classNode = classHierarchyNode.getClassNode();
			interfaceNames = classHierarchyNode.getDirectInterfaceNames();
		} else {
			classNode = this.classRepository.loadClassOrThrow(internalName);
			interfaceNames = classNode.getInterfaceNames();
		}

		int interfaceCount = interfaceNames.length;

		for (int i = 0; i < interfaceCount; i++) {
			String interfaceName = interfaceNames[i];
			String mappedInterfaceName = GlobalUtils.getMappedOrDefault(interfaceName, this.internalToBinaryNameMap);
			if (!interfaceName.equals(mappedInterfaceName)) {
				interfaceNames[i] = mappedInterfaceName;
			}

			this.interfaceHierarchySnapshot.put(mappedInterfaceName, internalName, BOOLEAN_TRUE);
		}

		outClassNode.set(classNode);
		return new ArrayEnumeration<>(interfaceNames);
	}

	public void setCustomPropertiesOutputPaths(HashMap<ClasspathResource, File> externalLibraryDependencyMap) {
		this.customPropertiesOutputPaths = externalLibraryDependencyMap;
	}

	@Override
	public final MethodNode getMethodBySignature(ClassNode classNode, MethodSignature methodSignature) {
		return this.classToMethodSignatureRegistry.get(classNode, methodSignature);
	}

	@Override
	public final Enumeration<ClassNode> getClasses() {
		return new ArrayEnumeration<>(this.workspaceClasses);
	}

	public TypeResolver getTypeResolver() {
		return new TypeResolver(this, this.classProvider);
	}

	public final synchronized void writeWorkspace(
		int compressionFlag, boolean deleteEmptyDirectories, boolean useUtf8Bom, String globalTimestamp, File outputTarget, AbstractLogger logger, ScriptContext scriptContext, ZKMConsumer consumer
	) {
		scriptContext.getMessageCount();
		scriptContext.getWarningCount();
		scriptContext.getErrorCount();
		scriptContext.getSpecialErrorCount();
		scriptContext.clearMessageCache();
		TypeResolver resolver = new TypeResolver(this, this.classProvider);
		int referenceCount = this.obfuscateReferencesGraph.size() + this.autoReflectionReferenceGraph.size() + this.mpcReferenceGraph.size();
		BitSetReferenceGraph<ClassNode, ClassNode> mergedReferenceGraph = new BitSetReferenceGraph<>(
			(BitSetReferenceGraph<ClassNode, ClassNode>)this.obfuscateReferencesGraph, referenceCount
		);
		mergedReferenceGraph.mergeWith((BitSetReferenceGraph<ClassNode, ClassNode>)this.autoReflectionReferenceGraph);
		mergedReferenceGraph.mergeWith((BitSetReferenceGraph<ClassNode, ClassNode>)this.mpcReferenceGraph);
		new WorkspaceWriter(
			this.workspaceClasses,
			this.moduleInfoNodes,
			this.allArchiveNodes,
			this.nestedArchives,
			this.skippedClassResources,
			this.skippedXmlResources,
			this.skippedYamlResources,
			this.skippedPropertiesResources,
			this.deletedResources,
			this.jadFileToArchiveNodeMap,
			this.xmlResources,
			this.yamlResources,
			this.propertiesResources,
			this.archiveToOutputFileMap,
			this.customJadOutputPaths,
			this.customXmlOutputPaths,
			this.customYamlOutputPaths,
			this.customPropertiesOutputPaths,
			this.getPackageRenameMap(),
			this.getClassNameToInternalNameMap(),
			this.getInternalToBinaryNameMap(),
			this.getFieldRegistryReadOnlyView(),
			this.getMethodRegistryReadOnlyView(),
			this,
			outputTarget,
			compressionFlag,
			deleteEmptyDirectories,
			useUtf8Bom,
			globalTimestamp,
			logger,
			scriptContext,
			resolver,
			scriptContext.getFixedClassManager(),
			mergedReferenceGraph,
			this.fileAdaptor,
			this.status
		);
		consumer.accept();
	}

	public Set<File> getClassSourceDirectories() {
		HashSet<File> sourceDirectories = GlobalUtils.newHashSet();
		if (this.allArchiveNodes != null) {
			for (int archiveIndex = 0; archiveIndex < this.allArchiveNodes.length; archiveIndex++) {
				if (this.nestedArchives == null || !this.nestedArchives.contains(this.allArchiveNodes[archiveIndex])) {
					String archiveRelativePath = this.allArchiveNodes[archiveIndex].getRelativePath();
					File archiveDirectory = new File(archiveRelativePath).getParentFile();
					if (archiveDirectory != null) {
						sourceDirectories.add(archiveDirectory);
					}
				}
			}
		}

		for (int classIndex = 0; classIndex < this.workspaceClasses.length; classIndex++) {
			ClassNode classNode = this.workspaceClasses[classIndex];
			if (!classNode.isGeneratedClass()) {
				Enumeration<ClasspathResource> classSources = this.workspaceClasses[classIndex].getClassSources();

				while (classSources.hasMoreElements()) {
					ClasspathResource classSource = classSources.nextElement();
					if (classSource.isFileSystemResource()) {
						File classSourceDirectory = classSource.getAsFile().getParentFile();
						if (classSourceDirectory != null) {
							String classSourcePath = classSourceDirectory.getAbsolutePath();
							if (classSourcePath.endsWith(File.separator) && classSourcePath.length() > 1) {
								classSourcePath = classSourcePath.substring(0, classSourcePath.length() - 1);
							}

							String packageName = classNode.getPackageName();
							if (packageName.length() == 0) {
								sourceDirectories.add(classSourceDirectory);
							} else {
								packageName = packageName.replace('/', File.separatorChar);
								int packagePathIndex = classSourcePath.indexOf(packageName);
								if (packagePathIndex > -1) {
									String sourceRootPath = classSourcePath.substring(0, packagePathIndex);
									sourceDirectories.add(new File(sourceRootPath));
								}
							}
						}
					}
				}
			}
		}

		return sourceDirectories;
	}

	public void clearHierarchyCaches() {
		this.superClassHierarchyCache.clearAll();
		this.interfaceHierarchyCache.clearAll();
		this.annotationReferenceGraph.clear();
	}

	@Override
	public final synchronized Enumeration<ClassNode> getClassesByMethodName(String methodName) {
		MultiMap<MethodSignature, ClassNode> classesBySignature = this.classLookupByMethodSignature.get(methodName);
		return classesBySignature != null ? classesBySignature.elements() : null;
	}

	public void setCustomJadOutputPaths(HashMap<JadFileProcessor, File> customJadOutputPaths) {
		this.customJadOutputPaths = customJadOutputPaths;
	}

	public boolean isIntegerConstantsEncrypted() {
		return this.integerConstantsEncrypted;
	}

	public final ClassRepository getClassRepository() {
		return this.classRepository;
	}

	private boolean isModuleInfoClassResource(ClasspathResource resource) {
		String resourceName = resource.getResourceName();
		String fileName;
		if (resource.isFileSystemResource()) {
			fileName = resourceName.substring(resourceName.lastIndexOf(FileUtils.FILE_SEPARATOR) + 1);
			if (!FileUtils.IS_UNIX) {
				fileName = fileName.toLowerCase();
			}
		} else {
			fileName = resourceName.substring(resourceName.lastIndexOf("/") + 1);
		}

		return fileName.equals("module-info.class");
	}

	@Override
	public final synchronized Enumeration<ClassNode> getClassesByFieldName(String fieldName) {
		DualKeyMap<ClassNode, FieldNode, FieldNode> fieldsByClass = this.fieldRegistryByName.getDualKeyMap(fieldName);
		return fieldsByClass != null ? fieldsByClass.getGroupKeys() : null;
	}

	public void refreshClassChangeData(Object rootObject, int maxClasses, int resourceCount) {
		buildClassTree(rootObject.getClass().getName(), maxClasses, resourceCount, this.integrityCheckClassHashes, this.runtimeClassRepository, null, false);
	}

	public ReadOnlyMap<String, String> getClassNameToInternalNameMap() {
		return new ReadOnlyMap<>(this.classNameToInternalNameMap);
	}

	public int updatePackageRenameMap(Map<String, String> packageNameMappings) {
		return this.packageRenameMap.updateMap(packageNameMappings);
	}

	@Override
	public final FieldNode[] getFieldsByClassAndName(String internalName, String fieldName) {
		Map<String, FieldNode> fieldsByDescriptor = this.classToFieldRegistry.getGroup(internalName, fieldName);
		if (fieldsByDescriptor == null) {
			return null;
		} else {
			FieldNode[] fieldNodes = new FieldNode[fieldsByDescriptor.size()];
			int i = 0;
			Iterator<FieldNode> fieldIterator = fieldsByDescriptor.values().iterator();

			while (fieldIterator.hasNext()) {
				fieldNodes[i++] = fieldIterator.next();
			}

			return fieldNodes;
		}
	}

	public final synchronized void applyClassNameMappings(HashMap<String, String> originalToNewNameMap) {
		for (Entry<String, String> entry : originalToNewNameMap.entrySet()) {
			String originalClassName = entry.getKey();
			String newClassName = entry.getValue();
			this.renameFieldsWithClassNamePrefix(originalClassName, newClassName);
		}
	}

	@Override
	public final boolean isSubclassOf(String className, String target) {
		try {
			boolean targetHasWildcard = target.indexOf(42) != -1;
			return this.isInterfaceImplementing(className, target, targetHasWildcard);
		} catch (StackOverflowError stackOverflowError) {
			AbstractClassNode classNode = this.classProvider.getClassNode(className, "reporting corrupt inheritance heirarchy");
			throw new ZKMUnexpectedException(stackOverflowError.getMessage() + " : Class " + classNode.getSourcePath() + " has corrupt hierarchy (B)", stackOverflowError);
		}
	}

	@Override
	public final boolean isAssignableFrom(String className, String target) {
		try {
			boolean targetHasWildcard = target.indexOf(42) != -1;
			return this.checkInterfaceHierarchyWithSnapshot(className, target, targetHasWildcard);
		} catch (StackOverflowError stackOverflowError) {
			AbstractClassNode classNode = this.classProvider
				.getClassNode(GlobalUtils.getMappedOrDefault(target, this.classNameToInternalNameMap), "reporting corrupt inheritance heirarchy");
			throw new ZKMUnexpectedException(stackOverflowError.getMessage() + " : Class " + classNode.getSourcePath() + " has corrupt hierarchy (D)", stackOverflowError);
		}
	}

	public final void remapAllReferences(
		AbstractClassNode[] classNodes, AbstractClassNode[] moduleInfoNodes, int keepInnerClassInfo, int keepGenericsInformation, HashMap<String, String> originalToNewNameMap, HashMap<String, String> packageNameMappings, AbstractLogger logger
	) {
		if (classNodes != null) {
			for (int classIndex = 0; classIndex < classNodes.length; classIndex++) {
				AbstractClassNode classNode = classNodes[classIndex];
				classNode.remapReferences(keepInnerClassInfo, keepGenericsInformation, originalToNewNameMap, this.processedStringNodes, this.resourceFilePaths, packageNameMappings, logger);
				if (classNode.hasInnerClasses()) {
					Iterator<AbstractClassNode> innerClassIterator = classNode.getInnerClasses().iterator();

					while (innerClassIterator.hasNext()) {
						innerClassIterator.next().remapReferences(keepInnerClassInfo, keepGenericsInformation, originalToNewNameMap, this.processedStringNodes, this.resourceFilePaths, packageNameMappings, logger);
					}
				}
			}
		}

		if (moduleInfoNodes != null) {
			for (int moduleIndex = 0; moduleIndex < moduleInfoNodes.length; moduleIndex++) {
				moduleInfoNodes[moduleIndex].remapReferences(keepInnerClassInfo, keepGenericsInformation, originalToNewNameMap, this.processedStringNodes, this.resourceFilePaths, packageNameMappings, logger);
			}
		}
	}

	@Override
	public Set<String> getAnnotationTypes(String className, Integer innerClassOrdinal, boolean matchOnOriginalNames) {
		Set<String> annotationTypes;
		if (matchOnOriginalNames && Options.ZKM_MATCH_ON_ORIGINAL && this.hasHierarchySnapshot()) {
			String binaryName = GlobalUtils.getMappedOrDefault(className, this.internalToBinaryNameMap);
			annotationTypes = this.collectOriginalAnnotationTypes(className, binaryName, innerClassOrdinal);
		} else {
			annotationTypes = this.collectAnnotationTypes(className, innerClassOrdinal);
		}

		return annotationTypes;
	}

	@Override
	public final ClassProvider getClassProvider() {
		return this.classProvider;
	}

	public ClassTreeManager getClassTreeManager() {
		return this.classTreeManager;
	}

	public final boolean hasClasses() {
		return this.workspaceClasses != null && this.workspaceClasses.length > 0;
	}

	public void analyzeReflectionCalls(
		HierarchicalMultiMap<FieldID, InstructionListNode, Integer> fieldReadInstructions,
		HierarchicalMultiMap<FieldID, InstructionListNode, Integer> fieldWriteInstructions,
		HierarchicalMultiMap<MethodID, InstructionListNode, Integer> methodCallInstructions,
		PrintWriter printWriter,
		AtomicInt warningCounter,
		boolean verbose,
		AbstractLogger logger
	) {
		HashSet<ClassNode> classesWithMethodPatterns = GlobalUtils.newHashSet();
		HashMap<AbstractMethodCallWriter, ReflectionMethodPattern> methodPatternsByWriter = GlobalUtils.newHashMap();

		for (int i = 0; i < this.workspaceClasses.length; i++) {
			this.workspaceClasses[i].resolveMethodTargets(classesWithMethodPatterns, methodPatternsByWriter, this, logger);
			if (this.workspaceClasses[i].hasInnerClasses()) {
				Iterator<AbstractClassNode> resolveInnerClassIterator = this.workspaceClasses[i].getInnerClasses().iterator();

				while (resolveInnerClassIterator.hasNext()) {
					((ClassNode)resolveInnerClassIterator.next()).resolveMethodTargets(classesWithMethodPatterns, methodPatternsByWriter, this, logger);
				}
			}
		}

		Map<AbstractMethodCallWriter, ReflectionMethodPattern> readOnlyMethodPatternsByWriter = Collections.unmodifiableMap(methodPatternsByWriter);
		HashMap<MethodID, MethodAnalysisResult> analysisResultsByMethodId = GlobalUtils.newHashMap();
		HashMap<FieldID, HashSet<Reference>> referencesByFieldId = GlobalUtils.newHashMap();
		HashMap<ExcludeElement, HashSet<Reference>> stackSourceReferences = GlobalUtils.newHashMap();
		HashMap<ExcludeElement, HashSet<Reference>> localVariableSourceReferences = GlobalUtils.newHashMap();
		SoftReferenceMap<InstructionListNode, MethodAnalyzer> methodAnalyzerCache = new SoftReferenceMap<>("GC");
		String indentation = StringUtils.justificateString("", 76, 4, 32);
		TypeResolver typeResolver = new TypeResolver(this, this.classProvider);
		ArrayList<MethodInvocation> methodInvocations = new ArrayList<>();

		for (int classIndex = 0; classIndex < this.workspaceClasses.length; classIndex++) {
			ClassNode classNode = this.workspaceClasses[classIndex];
			classNode.analyzeReflectionCalls(
				classesWithMethodPatterns,
				readOnlyMethodPatternsByWriter,
				fieldReadInstructions,
				this.classRepository,
				fieldWriteInstructions,
				methodCallInstructions,
				printWriter,
				warningCounter,
				this,
				typeResolver,
				indentation,
				analysisResultsByMethodId,
				referencesByFieldId,
				stackSourceReferences,
				localVariableSourceReferences,
				methodAnalyzerCache,
				this.stringClassReferenceMap,
				this.stringResourcePathReferenceMap,
				this.stringFieldNameReferenceMap,
				this.stringMethodNameReferenceMap,
				this.stringInternalClassNameReferenceMap,
				this.processedStringNodes,
				this.usageReferenceMap,
				logger,
				methodInvocations,
				verbose
			);
			if ((Options.ZKM_TRANSLATE_KOTLIN || Options.translateKotlinMetadata) && classNode.isFinal() && this.isClassInstanceOf(classNode.getInternalName(), "kotlin/jvm/internal/CallableReference")) {
				this.processKotlinCallableReference(classNode);
				if (classNode.hasInnerClasses()) {
					for (AbstractClassNode innerClassNode : classNode.getInnerClasses()) {
						this.processKotlinCallableReference((ClassNode)innerClassNode);
					}
				}
			}

			if (classNode.hasInnerClasses()) {
				Iterator<AbstractClassNode> analyzeInnerClassIterator = classNode.getInnerClasses().iterator();

				while (analyzeInnerClassIterator.hasNext()) {
					((ClassNode)analyzeInnerClassIterator.next())
						.analyzeReflectionCalls(
							classesWithMethodPatterns,
							readOnlyMethodPatternsByWriter,
							fieldReadInstructions,
							this.classRepository,
							fieldWriteInstructions,
							methodCallInstructions,
							printWriter,
							warningCounter,
							this,
							typeResolver,
							indentation,
							analysisResultsByMethodId,
							referencesByFieldId,
							stackSourceReferences,
							localVariableSourceReferences,
							methodAnalyzerCache,
							this.stringClassReferenceMap,
							this.stringResourcePathReferenceMap,
							this.stringFieldNameReferenceMap,
							this.stringMethodNameReferenceMap,
							this.stringInternalClassNameReferenceMap,
							this.processedStringNodes,
							this.usageReferenceMap,
							logger,
							methodInvocations,
							verbose
						);
				}
			}
		}

		for (MethodInvocation methodInvocation : methodInvocations) {
			this.methodInvocationMap.put((MethodNode)methodInvocation.getInstructionListNode().getMethodNode(), methodInvocation);
		}
	}

	private void processKotlinCallableReference(ClassNode classNode) {
		MethodSignature getOwnerSignature = new MethodSignature("getOwner", "()Lkotlin/reflect/KDeclarationContainer;");
		MethodNode getOwnerMethod = classNode.findMethodNode(getOwnerSignature);
		if (getOwnerMethod != null) {
			TypeWriter ldcConstant = getOwnerMethod.getFirstLdcConstant();
			if (ldcConstant instanceof ClassInfoWriter) {
				String ownerInternalName = ((ClassInfoWriter)ldcConstant).getInternalName();
				MethodSignature getNameSignature = new MethodSignature("getName", "()Ljava/lang/String;");
				MethodNode getNameMethod = classNode.findMethodNode(getNameSignature);
				if (getNameMethod != null) {
					ldcConstant = getNameMethod.getFirstLdcConstant();
					if (ldcConstant instanceof StringNode) {
						StringNode nameNode = (StringNode)ldcConstant;
						MethodSignature getSignatureSignature = new MethodSignature("getSignature", "()Ljava/lang/String;");
						MethodNode getSignatureMethod = classNode.findMethodNode(getSignatureSignature);
						if (getSignatureMethod != null) {
							ldcConstant = getSignatureMethod.getFirstLdcConstant();
							if (ldcConstant instanceof StringNode) {
								StringNode signatureNode = (StringNode)ldcConstant;
								ClassNode ownerClass = ClassHierarchyNode.getWorkspaceClassNode(ownerInternalName);
								if (ownerClass != null) {
									String signatureString = signatureNode.getValueAsString();
									if (signatureString.indexOf(40) > 0) {
										MethodSignature referencedSignature = new MethodSignature(signatureString);
										MethodNode referencedMethod = ownerClass.findMethodNode(referencedSignature);
										if (referencedMethod != null) {
											if (signatureString.startsWith(nameNode.getValueAsString())) {
												StringReference nameReference = new StringReference(nameNode);
												this.stringMethodNameReferenceMap.put(referencedMethod, nameReference, nameReference);
											} else {
												String accessorName = referencedSignature.getName();
												String accessorPrefix = null;
												if (accessorName.startsWith("get")) {
													accessorPrefix = "get";
												} else if (accessorName.startsWith("set")) {
													accessorPrefix = "set";
												} else if (accessorName.startsWith("is")) {
													accessorPrefix = "is";
												}

												if (accessorPrefix != null && MethodSignature.getFieldNameFromAccessor(accessorName, accessorPrefix).equals(nameNode.getValueAsString())) {
													StringReference fieldNameReference = new StringReference(nameNode, accessorPrefix);
													this.stringMethodNameReferenceMap.put(referencedMethod, fieldNameReference, fieldNameReference);
												}
											}

											StringReference signatureReference = new StringReference(signatureNode, true);
											this.stringMethodNameReferenceMap.put(referencedMethod, signatureReference, signatureReference);
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}

	public final synchronized List<ClassNode> findClassesExtending(String target) {
		ArrayList<ClassNode> classes = null;

		for (int i = 0; i < this.workspaceClasses.length; i++) {
			if (this.isClassInstanceOf(this.workspaceClasses[i].getInternalName(), target)) {
				if (classes == null) {
					classes = new ArrayList<>();
				}

				classes.add(this.workspaceClasses[i]);
			}
		}

		return classes;
	}

	public void applyInnerClassFieldRenames(AbstractClassNode[] classNodes, DualKeyMap<String, FieldSignature, FieldSignature> fieldSignatureMappings, ScriptContext scriptContext) {
		for (int i = 0; i < classNodes.length; i++) {
			AbstractClassNode classNode = classNodes[i];
			if (classNode.hasInnerClasses()) {
				classNode.applyInnerClassFieldRenames(fieldSignatureMappings, scriptContext);
			}
		}
	}

	public ReadOnlyMap<String, String> getInternalToBinaryNameMap() {
		return new ReadOnlyMap<>(this.internalToBinaryNameMap);
	}

	private String getStructureNodeType(StructureNode structureNode) {
		if (structureNode instanceof ClassNode) {
			return "class";
		} else if (structureNode instanceof FieldNode) {
			return "field";
		} else {
			return structureNode instanceof MethodNode ? "method" : "unknown";
		}
	}

	public Set<MethodNode> getMethodsReferencingStrings() {
		HashSet<MethodNode> methodNodes = GlobalUtils.newHashSet();
		Enumeration<StructureNode> structureNodeEnumeration = this.usageReferenceMap.elements();

		while (structureNodeEnumeration.hasMoreElements()) {
			StructureNode structureNode = structureNodeEnumeration.nextElement();
			if (structureNode instanceof MethodNode && ((MethodNode)structureNode).isWorkspaceNode()) {
				methodNodes.add((MethodNode)structureNode);
			}
		}

		return methodNodes;
	}

	@Override
	public final boolean containsFieldSignature(FieldSignature fieldSignature, ClassNode classNode) {
		return this.fieldSignatureRegistry.contains(fieldSignature, classNode);
	}

	public ReadOnlyRegistryView<AbstractClassNode, MethodSignature, MethodSignature> getMethodRegistryReadOnlyView() {
		return new ReadOnlyRegistryView<>(this.methodRegistry);
	}

	public final ClassHierarchyNode getClassHierarchyNode(int classIndex) {
		return this.classTreeManager.getClassByIndex(classIndex);
	}

	public final boolean isErrorEmpty() {
		return this.errorMessage == null;
	}

	public ClassNode[] removeClassFromWorkspace(ClassNode classNode) {
		int classIndex = Arrays.binarySearch(this.workspaceClasses, classNode);
		if (classIndex >= 0) {
			ClassNode[] remainingClasses = new ClassNode[this.workspaceClasses.length - 1];
			if (classIndex > 0) {
				System.arraycopy(this.workspaceClasses, 0, remainingClasses, 0, classIndex);
			}

			if (classIndex < this.workspaceClasses.length - 1) {
				System.arraycopy(this.workspaceClasses, classIndex + 1, remainingClasses, classIndex, remainingClasses.length - classIndex);
			}

			this.workspaceClasses = remainingClasses;
			Arrays.sort(this.workspaceClasses);
			ClassHierarchyNode.resetAll();
			this.classTreeManager.rebuildClassTree(this.workspaceClasses);
			this.packageManager.rebuildPackageTree(this.workspaceClasses);
			this.rebuildMethodRegistries();
			this.rebuildFieldRegistries();
		}

		return this.workspaceClasses;
	}

	public void setMethodRegistryFromClasses(AbstractClassNode[] classNodes) {
		this.methodRegistry = new DualKeyMap<>();

		for (AbstractClassNode classNode : classNodes) {
			for (AbstractMethodNode methodNode : classNode.getMethods()) {
				this.methodRegistry.put(classNode, methodNode.getOriginalMethodSignature(), methodNode.getMethodSignature());
			}
		}
	}

	public final void renameFieldInRegistries(ClassNode classNode, FieldNode fieldNode, String newName) {
		String newDisplayName = fieldNode.getNameDisplay();
		String fieldDescriptor = fieldNode.getDescriptor();
		String classInternalName = classNode.getInternalName();
		FieldNode previousField = this.fieldRegistryByName.remove(newName, classNode, fieldNode);
		previousField = this.fieldRegistryByName.put(newDisplayName, classNode, fieldNode, fieldNode);
		previousField = this.classToFieldRegistry.remove(classInternalName, newName, fieldDescriptor);
		previousField = this.classToFieldRegistry.put(classInternalName, newDisplayName, fieldDescriptor, fieldNode);
		previousField = this.fieldSignatureRegistry.remove(new FieldSignature(newName, fieldNode.getDescriptor()), classNode);
		previousField = this.fieldSignatureRegistry.put(fieldNode.getFieldSignature(), classNode, fieldNode);
	}

	public boolean hasTrackedMethodInvocations() {
		return this.methodInvocationMap.size() > 0;
	}

	public void removeStringNodeReferences(Set<StringNode> stringNodes) {
		if (stringNodes != null && stringNodes.size() > 0) {
			this.removeStringReferences(this.stringClassReferenceMap, stringNodes);
			SnapshotEnumerationAdapter<String> resourcePathEnumeration = new SnapshotEnumerationAdapter<>(this.stringResourcePathReferenceMap.keys());

			while (resourcePathEnumeration.hasMoreElements()) {
				String resourcePath = resourcePathEnumeration.nextElement();
				List<Reference> references = this.stringResourcePathReferenceMap.get(resourcePath);

				for (int i = 0; i < references.size(); i++) {
					Reference reference = references.get(i);
					if (reference instanceof StringReference) {
						StringNode stringNode = ((StringReference)reference).getStringNode();
						if (stringNodes.contains(stringNode)) {
							this.stringResourcePathReferenceMap.remove(resourcePath, reference);
						}
					}
				}
			}

			this.removeStringReferences(this.stringInternalClassNameReferenceMap, stringNodes);
			this.removeStringReferences(this.stringFieldNameReferenceMap, stringNodes);
			this.removeStringReferences(this.stringMethodNameReferenceMap, stringNodes);
		}
	}

	private void resolveUnknownAttributes(ThreeLevelMultiMap<String, String, UnknownAttribute, Object> unknownAttributes) {
		Enumeration<String> ownerClassNameEnumeration = unknownAttributes.keys();

		while (ownerClassNameEnumeration.hasMoreElements()) {
			String ownerClassName = ownerClassNameEnumeration.nextElement();
			HierarchicalMultiMap<String, UnknownAttribute, Object> attributesByName = unknownAttributes.get(ownerClassName);
			Enumeration<String> attributeNameEnumeration = attributesByName.keys();

			while (attributeNameEnumeration.hasMoreElements()) {
				String attributeName = attributeNameEnumeration.nextElement();
				MultiMap<UnknownAttribute, Object> attributes = attributesByName.get(attributeName);
				boolean argumentCountsMatch = true;
				boolean argumentTypesDiffer = false;
				ArrayList<String> argumentTypeNames = null;
				HashSet<AbstractClassNode> owningClasses = GlobalUtils.newHashSet();
				Enumeration<UnknownAttribute> attributeEnumeration = attributes.keys();

				while (attributeEnumeration.hasMoreElements()) {
					UnknownAttribute attribute = attributeEnumeration.nextElement();
					owningClasses.add(attribute.getClassNode());
					List<Object> attributeArguments = attributes.get(attribute);
					if (argumentTypeNames == null) {
						argumentTypeNames = new ArrayList<>(attributeArguments.size());

						for (int i = 0; i < attributeArguments.size(); i++) {
							Object argument = attributeArguments.get(i);
							argumentTypeNames.add(argument.getClass().getName());
						}
					} else {
						if (attributeArguments.size() != argumentTypeNames.size()) {
							argumentCountsMatch = false;
							break;
						}

						for (int argumentIndex = 0; argumentIndex < attributeArguments.size(); argumentIndex++) {
							String recordedTypeName = argumentTypeNames.get(argumentIndex);
							if (!attributeArguments.get(argumentIndex).getClass().getName().equals(recordedTypeName)) {
								argumentTypesDiffer = true;
								if (!recordedTypeName.equals("java.lang.Object")) {
									argumentTypeNames.set(argumentIndex, "java.lang.Object");
								}
							}
						}
					}
				}

				if (!argumentCountsMatch) {
					attributeEnumeration = attributes.keys();

					while (attributeEnumeration.hasMoreElements()) {
						UnknownAttribute resetAttribute = attributeEnumeration.nextElement();
						resetAttribute.setParsedSuccessfully(false);
						resetAttribute.setSharedByManyClasses(false);
					}
				} else {
					boolean manyOwningClasses = owningClasses.size() > 7;
					Enumeration<UnknownAttribute> applyAttributeEnumeration = attributes.keys();

					while (applyAttributeEnumeration.hasMoreElements()) {
						UnknownAttribute updatedAttribute = applyAttributeEnumeration.nextElement();
						updatedAttribute.setSharedByManyClasses(manyOwningClasses);
						if (argumentTypesDiffer) {
							for (int objectTypeIndex = 0; objectTypeIndex < argumentTypeNames.size(); objectTypeIndex++) {
								if (argumentTypeNames.get(objectTypeIndex).equals("java.lang.Object")) {
									updatedAttribute.clearResolvedConstant(objectTypeIndex);
								}
							}
						}
					}
				}
			}
		}
	}

	public void validateMethodAnalyzers(TypeResolver typeResolver, ScriptContext scriptContext) {
		ClassNode[] classes = this.workspaceClasses;
		int classCount = classes.length;

		for (int i = 0; i < classCount; i++) {
			classes[i].validateMethodAnalyzers(typeResolver, this, scriptContext);
		}
	}

	public String getError() {
		return this.errorMessage;
	}

	public final void rebuildMethodRegistries() {
		this.classLookupByMethodSignature = new HierarchicalMultiMap<>(Math.max(15, this.workspaceClasses.length * 5), 5, Options.ZKM_USE_PARALLEL);
		this.classToMethodSignatureRegistry = new DualKeyMap<>(Math.max(15, this.workspaceClasses.length), 5, Options.ZKM_USE_PARALLEL);
		this.classMethodParameterKeyRegistry = new HierarchicalMultiMap<>(Math.max(15, this.workspaceClasses.length), 5, 2, Options.ZKM_USE_PARALLEL);

		for (int i = 0; i < this.workspaceClasses.length; i++) {
			this.workspaceClasses[i].registerMethodsInWorkspace(this);
		}
	}

	public final Enumeration<ClassHierarchyNode> getAllClassesEnumeration() {
		return this.classTreeManager.getAllClassesEnumeration();
	}

	public MultiMap<MethodNode, MethodInvocation> getTrackedMethodInvocations() {
		return this.methodInvocationMap;
	}

	public ArchiveNode[] getAllArchiveNodes() {
		ArchiveNode[] archiveNodes = new ArchiveNode[this.allArchiveNodes.length];
		System.arraycopy(this.allArchiveNodes, 0, archiveNodes, 0, this.allArchiveNodes.length);
		return archiveNodes;
	}

	private boolean checkInterfaceHierarchyWildcardWithSnapshot(String className, String target, boolean targetIsWildcard, ComplexAnnotationSpecifier annotationSpecifier) {
		if (className.equals("java/lang/Object")) {
			return false;
		} else {
			AtomicReference<AbstractClassNode> classNodeReference = new AtomicReference<>();
			Enumeration<String> interfaceNameEnumeration = this.getSnapshotInterfaceNames(className, classNodeReference);

			while (interfaceNameEnumeration.hasMoreElements()) {
				String interfaceName = interfaceNameEnumeration.nextElement();
				String hierarchyErrorMessage;
				if (Options.ZKM_TEST_HIERARCHY
					&& (
							hierarchyErrorMessage = ClassHierarchyNode.validateInheritanceLink(
								classNodeReference.get(), this.classProvider.getClassNode(GlobalUtils.getMappedOrDefault(interfaceName, this.classNameToInternalNameMap)), 2
							)
						)
						!= null) {
					throw new LookupException(hierarchyErrorMessage);
				}

				if (!targetIsWildcard && interfaceName.equals(target)) {
					return annotationSpecifier.matchesAnnotationTypes(this.getAnnotationTypes(interfaceName, null, true));
				}

				if (targetIsWildcard && StringUtils.matchesWildcard(interfaceName, target) && annotationSpecifier.matchesAnnotationTypes(this.getAnnotationTypes(interfaceName, null, true))) {
					return true;
				}

				if (this.checkInterfaceHierarchyWildcardWithSnapshot(interfaceName, target, targetIsWildcard, annotationSpecifier)) {
					return true;
				}
			}

			String superclassInternalName = this.getSuperclassName(className);
			return this.checkInterfaceHierarchyWildcardWithSnapshot(superclassInternalName, target, targetIsWildcard, annotationSpecifier);
		}
	}

	@Override
	public final void registerField(ClassNode classNode, FieldNode fieldNode) {
		String fieldName = fieldNode.getNameDisplay();
		FieldNode previousField = this.fieldSignatureRegistry.put(fieldNode.getFieldSignature(), classNode, fieldNode);
		DebugUtils.assertTrue(
			previousField == null,
			new String[]{
				previousField == null ? "" : "Duplicate field (1) '" + classNode.getRawSourcePath() + "' '" + fieldNode.getDisplayName() + "' : '" + previousField.getDisplayName() + "'"
			}
		);
		FieldNode previousFieldByName = this.fieldRegistryByName.put(fieldName, classNode, fieldNode, fieldNode);
		DebugUtils.assertTrue(
			previousFieldByName == null,
			new String[]{
				previousFieldByName == null ? "" : "Duplicate field (2) '" + classNode.getRawSourcePath() + "' '" + fieldNode.getDisplayName() + "' : '" + previousFieldByName.getDisplayName() + "'"
			}
		);
		FieldNode previousFieldByPath = this.classToFieldRegistry.put(classNode.getInternalName(), fieldName, fieldNode.getDescriptor(), fieldNode);
		DebugUtils.assertTrue(
			previousFieldByPath == null,
			new String[]{
				previousFieldByPath == null ? "" : "Duplicate field (3) '" + classNode.getRawSourcePath() + "' '" + fieldNode.getDisplayName() + "' : '" + previousFieldByPath.getDisplayName() + "'"
			}
		);
	}

	private boolean checkInterfaceHierarchyWildcard(String className, String target, boolean targetIsWildcard, ComplexAnnotationSpecifier annotationSpecifier) {
		if (className.equals("java/lang/Object")) {
			return false;
		} else {
			AtomicReference<AbstractClassNode> classNodeReference = new AtomicReference<>();
			Enumeration<String> interfaceNameEnumeration = this.getDirectInterfaceNames(className, classNodeReference);

			while (interfaceNameEnumeration.hasMoreElements()) {
				String interfaceName = interfaceNameEnumeration.nextElement();
				String hierarchyErrorMessage;
				if (Options.ZKM_TEST_HIERARCHY && (hierarchyErrorMessage = ClassHierarchyNode.validateInheritanceLink(classNodeReference.get(), this.classProvider.getClassNode(interfaceName), 2)) != null) {
					throw new LookupException(hierarchyErrorMessage);
				}

				if (!targetIsWildcard && interfaceName.equals(target)) {
					return annotationSpecifier.matchesAnnotationTypes(this.getAnnotationTypes(interfaceName, null, false));
				}

				if (targetIsWildcard && StringUtils.matchesWildcard(interfaceName, target) && annotationSpecifier.matchesAnnotationTypes(this.getAnnotationTypes(interfaceName, null, false))) {
					return true;
				}

				if (this.checkInterfaceHierarchyWildcard(interfaceName, target, targetIsWildcard, annotationSpecifier)) {
					return true;
				}
			}

			String superclassInternalName = this.getSuperclassInternalName(className);
			return this.checkInterfaceHierarchyWildcard(superclassInternalName, target, targetIsWildcard, annotationSpecifier);
		}
	}

	private boolean isInterfaceImplementing(String className, String target, boolean targetIsWildcard) {
		if (className.equals("java/lang/Object")) {
			return false;
		} else {
			Boolean cachedResult = this.interfaceHierarchyCache.get(target, className);
			if (cachedResult != null) {
				return cachedResult;
			} else {
				AtomicReference<AbstractClassNode> classNodeReference = new AtomicReference<>();
				Enumeration<String> interfaceNameEnumeration = this.getDirectInterfaceNames(className, classNodeReference);

				while (interfaceNameEnumeration.hasMoreElements()) {
					String interfaceName = interfaceNameEnumeration.nextElement();
					String hierarchyErrorMessage;
					if (Options.ZKM_TEST_HIERARCHY && (hierarchyErrorMessage = ClassHierarchyNode.validateInheritanceLink(classNodeReference.get(), this.classProvider.getClassNode(interfaceName), 2)) != null) {
						throw new LookupException(hierarchyErrorMessage);
					}

					if (!targetIsWildcard ? interfaceName.equals(target) : StringUtils.matchesWildcard(interfaceName, target)) {
						this.interfaceHierarchyCache.put(target, className, BOOLEAN_TRUE);
						return true;
					}

					if (this.isInterfaceImplementing(interfaceName, target, targetIsWildcard)) {
						this.interfaceHierarchyCache.put(target, className, BOOLEAN_TRUE);
						return true;
					}
				}

				String superclassInternalName = this.getSuperclassInternalName(className);
				boolean implementedBySuperclass = this.isInterfaceImplementing(superclassInternalName, target, targetIsWildcard);
				this.interfaceHierarchyCache.put(target, className, implementedBySuperclass ? BOOLEAN_TRUE : BOOLEAN_FALSE);
				return implementedBySuperclass;
			}
		}
	}

	public void markLongConstantsEncrypted() {
		this.longConstantsEncrypted = true;
	}

	public final PackageManager getPackageManager() {
		return this.packageManager;
	}

	@Override
	public final boolean containsMethodSignature(MethodSignature methodSignature, ClassNode classNode) {
		return this.classToMethodSignatureRegistry.contains(classNode, methodSignature);
	}

	public final synchronized void renameField(FieldNode fieldNode, String newName) {
		ClassNode ownerClass = fieldNode.getOwnerClass();
		if (this.classToFieldRegistry.containsEntry(ownerClass.getInternalName(), newName, fieldNode.getDescriptor())) {
			throw new DuplicateFieldException("The class already has a field with this name and type");
		} else {
			fieldNode.setName(newName);

			for (int i = 0; i < this.workspaceClasses.length; i++) {
				this.workspaceClasses[i].refreshFieldRefsAndConstants(null, this);
			}

			this.rebuildFieldRegistries();
		}
	}

	@Override
	public boolean isMethodNameObfFinish() {
		return this.methodNameObfFinish;
	}

	@Override
	public final boolean isSuperclassOf(String className, String target) {
		try {
			boolean targetIsWildcard = target.indexOf(42) != -1;
			return this.checkSuperclassHierarchyWithSnapshot(className, target, targetIsWildcard);
		} catch (StackOverflowError stackOverflowError) {
			AbstractClassNode classNode = this.classProvider
				.getClassNode(GlobalUtils.getMappedOrDefault(className, this.classNameToInternalNameMap), "reporting corrupt inheritance hierarchy");
			throw new ZKMUnexpectedException(stackOverflowError.getMessage() + " : Class " + classNode.getSourcePath() + " has corrupt inheritance hierarchy (C) '" + className + "'", stackOverflowError);
		}
	}

	public void setupObjectMethods() {
		try {
			for (AbstractMethodNode method : this.classProvider.getClassNode("java/lang/Object").getMethods()) {
				this.objectMethods.put(method.getMethodSignature(), method);
			}
		} catch (FileException ignored) {
			DebugUtils.assertTrue(
				false,
				new String[]{"Could not find java/lang/Object" + (this.classpathManager != null ? " in '" + this.classpathManager.getRawClasspath() + "'" : "") + "."}
			);
		}
	}

	public Map<MethodNode, String> getEntryPointSignatures() {
		return this.resourceReferencedMethods;
	}

	public DualKeyMap<MethodNode, String, Object> getResourceToClassReferences() {
		return this.methodAccessorReferences;
	}

	public List<MethodNode> getReferencingWorkspaceMethods(MethodNode methodNode) {
		ArrayList<MethodNode> workspaceMethods = new ArrayList<>();
		List<StructureNode> structureNodes = this.usageReferenceMap.get(methodNode);
		if (structureNodes != null) {
			for (StructureNode structureNode : structureNodes) {
				if (structureNode instanceof MethodNode && ((MethodNode)structureNode).isWorkspaceNode()) {
					workspaceMethods.add((MethodNode)structureNode);
				}
			}
		}

		return workspaceMethods;
	}

	public final synchronized void rebuildAllRegistries() {
		ClassHierarchyNode.rebuildRegistry();
		this.rebuildMethodRegistries();
		this.rebuildFieldRegistries();
		this.classTreeManager.rebuildRegistries();
		this.markChanged();
		this.notifySimpleChange();
//      LogUtils.getMaxLocals();
	}

	public void performRemoveMethodCallAnalysis(
		List<RemoveMethodCallsIncludeStatement> includeStatements,
		List<RemoveMethodCallsExcludeStatement> excludeStatements,
		AbstractLogger logger,
		ZKMConsumer consumer,
		ClassLoadingStatus classLoadingStatus,
		ScriptContext scriptContext
	) {
		this.status.set("Performing remove method call analysis");
		MultiMap<AbstractMethodNode, InstructionListNode> methodCalls = new MultiMap<>();

		for (ClassNode classNode : this.workspaceClasses) {
			classNode.collectMethodCalls(methodCalls);
		}

		new TypeResolver(this, this.classProvider);
		RemoveMethodCallExcludes excludes = new RemoveMethodCallExcludes(this, methodCalls, includeStatements, excludeStatements, scriptContext);
		ReferenceGraph<InstructionListNode, AbstractMethodNode> referenceGraph = new ReferenceGraph<>();

		for (Entry<AbstractMethodNode, List<InstructionListNode>> methodCallEntry : methodCalls.entrySet()) {
			AbstractMethodNode methodNode = methodCallEntry.getKey();

			for (InstructionListNode instructionNode : methodCallEntry.getValue()) {
				if (excludes.isCallSiteMatched(methodNode, instructionNode)) {
					referenceGraph.addReference(instructionNode, methodNode);
				}
			}
		}

		if (referenceGraph.isEmpty()) {
			scriptContext.logWarningInternal("Remove Method Calls : No methods matched for removal.", true);
		} else {
			for (Entry<InstructionListNode, Set<AbstractMethodNode>> adjacencyEntry : referenceGraph.getAdjacencyEntries()) {
				InstructionListNode referencedInstruction = adjacencyEntry.getKey();
				referencedInstruction.removeUnusedMethodCalls(adjacencyEntry.getValue(), scriptContext);
			}
		}
	}

	public void trim(
		TrimConfig trimConfig,
		List<? extends ExclusionInclusionStatement> excludeStatements,
		List<TrimUnexcludeStatement> unexcludeStatements,
		List<ExistingSerializedClassesStatement> existingSerializedClassesStatements,
		List<FixedClassesStatement> fixedClassesStatements,
		PrintWriter printWriter,
		AbstractLogger logger,
		ZKMConsumer consumer,
		ClassLoadingStatus classLoadingStatus,
		ScriptContext scriptContext
	) throws IOException {
		if (this.classpathChanged) {
			logger.fatal(
				"FATAL ERROR:", "Classpath has been changed but the classes have not been reopened. You must reopen the classes. (B)"
			);
		} else if (!this.hasClasses()) {
			logger.message("TRIM MESSAGE:", "No classes opened other than module-info.class so no classes, fields or methods were trimmed.");
		} else {
			this.status.set("Performing trim analysis");

			try {
				FixedClassManager fixedClassManager = null;
				if (fixedClassesStatements != null && fixedClassesStatements.size() > 0) {
					fixedClassManager = new FixedClassManager(this, fixedClassesStatements, scriptContext);
					scriptContext.setFixedClassManager(fixedClassManager);
				}

				SerializedClassManager serializedClassManager = null;
				if (existingSerializedClassesStatements != null && existingSerializedClassesStatements.size() > 0) {
					serializedClassManager = new SerializedClassManager(this, existingSerializedClassesStatements, scriptContext, fixedClassManager, null);
				}

				TrimExcludes trimExcludes = new TrimExcludes(this, this.classTreeManager, excludeStatements, unexcludeStatements, trimConfig, scriptContext, this.usageReferenceMap, serializedClassManager, fixedClassManager);

				for (int i = 0; i < this.workspaceClasses.length; i++) {
					ClassNode classNode = this.workspaceClasses[i];
					if (fixedClassManager == null || !fixedClassManager.isClassExcluded(classNode)) {
						classNode.trimAttributes(trimExcludes, trimConfig, scriptContext, printWriter);
					}
				}

				trimExcludes.computeTrimResults();
				Thread.yield();
				if (trimExcludes.hasNoExcludedClasses()) {
					if (classLoadingStatus != null) {
						classLoadingStatus.setStatus(false, null);
					}

					logger.specialError("TRIM ERROR:", "All classes would be trimmed. You must change the trim exclusions to specify all application entry points.");
					return;
				}

				if (trimExcludes.hasNoProcessableClasses() && trimExcludes.hasNoProcessableFields() && trimExcludes.hasNoProcessableMethods()) {
					logger.message("TRIM MESSAGE:", "No classes, fields or methods were trimmed.");
					this.status.set("Trimming unused attributes");
				} else {
					this.status.set("Trimming unused classes, fields and methods");
				}

				List<ClassNode> trimmedFieldOwnerClasses = trimExcludes.getTrimmedFieldOwners();

				for (int fieldOwnerIndex = 0; fieldOwnerIndex < trimmedFieldOwnerClasses.size(); fieldOwnerIndex++) {
					ClassNode fieldOwnerClass = trimmedFieldOwnerClasses.get(fieldOwnerIndex);
					String fieldOwnerInternalName = fieldOwnerClass.getInternalName();
					ReadOnlyMap<FieldNode, FieldNode> classTrimmedFields = trimExcludes.getTrimmedFieldsForClass(fieldOwnerClass);
					fieldOwnerClass.removeFieldsMarkedForRemoval(classTrimmedFields);
					if (!fieldOwnerClass.isInnerClass()) {
						Enumeration<FieldNode> classTrimmedFieldEnumeration = classTrimmedFields.getKeyEnumeration();

						while (classTrimmedFieldEnumeration.hasMoreElements()) {
							FieldNode classTrimmedField = classTrimmedFieldEnumeration.nextElement();
							FieldNode removedField = this.classToFieldRegistry.remove(fieldOwnerInternalName, classTrimmedField.getNameDisplay(), classTrimmedField.getDescriptor());
							this.fieldRegistry.remove(fieldOwnerClass, classTrimmedField.getOriginalFieldSignature());
						}
					}
				}

				List<ClassNode> trimmedMethodOwnerClasses = trimExcludes.getTrimmedMethodOwners();

				for (int methodOwnerIndex = 0; methodOwnerIndex < trimmedMethodOwnerClasses.size(); methodOwnerIndex++) {
					ClassNode trimmedMethodOwnerClass = trimmedMethodOwnerClasses.get(methodOwnerIndex);
					ReadOnlyMap<MethodNode, MethodNode> classTrimmedMethods = trimExcludes.getTrimmedMethodsForClass(trimmedMethodOwnerClass);
					trimmedMethodOwnerClass.removeMethodsMarkedForRemoval(classTrimmedMethods);
					if (!trimmedMethodOwnerClass.isInnerClass()) {
						Enumeration<MethodNode> classTrimmedMethodEnumeration = classTrimmedMethods.getKeyEnumeration();

						while (classTrimmedMethodEnumeration.hasMoreElements()) {
							MethodNode classTrimmedMethod = classTrimmedMethodEnumeration.nextElement();
							MethodNode removedMethod = this.classToMethodSignatureRegistry.remove(trimmedMethodOwnerClass, classTrimmedMethod.getMethodSignature());
							this.classMethodParameterKeyRegistry.remove(trimmedMethodOwnerClass, classTrimmedMethod.getMethodParameterKey(), classTrimmedMethod);
							this.methodRegistry.remove(trimmedMethodOwnerClass, classTrimmedMethod.getOriginalMethodSignature());
						}
					}

					Iterator<MethodNode> bootstrapReferencedMethodIterator = this.bootstrapReferencedMethods.iterator();

					while (bootstrapReferencedMethodIterator.hasNext()) {
						MethodNode bootstrapReferencedMethod = bootstrapReferencedMethodIterator.next();
						if (classTrimmedMethods.containsKey(bootstrapReferencedMethod)) {
							bootstrapReferencedMethodIterator.remove();
						}
					}

					Iterator<MethodNode> indyReferencedMethodIterator = this.indyReferencedMethods.iterator();

					while (indyReferencedMethodIterator.hasNext()) {
						MethodNode indyReferencedMethod = indyReferencedMethodIterator.next();
						if (classTrimmedMethods.containsKey(indyReferencedMethod)) {
							indyReferencedMethodIterator.remove();
						}
					}
				}

				Enumeration<ClassNode> processableClassEnumeration = trimExcludes.getProcessableClasses();

				while (processableClassEnumeration.hasMoreElements()) {
					ClassNode processableClass = processableClassEnumeration.nextElement();
					this.classToFieldRegistry.removeDualKeyMap(processableClass.getInternalName());
					this.classToMethodSignatureRegistry.removeGroup(processableClass);
					this.classMethodParameterKeyRegistry.remove(processableClass);
					Map<Reference, Integer> stringReferenceCounts = this.stringClassReferenceMap.removeGroup(processableClass);
					if (stringReferenceCounts != null) {
						for (Reference reference : stringReferenceCounts.keySet()) {
							if (reference instanceof StringReference) {
								StringReference stringReference = (StringReference)reference;
								this.processedStringNodes.remove(stringReference.getStringNode());
							}
						}
					}
				}

				Thread.yield();
				Enumeration<FieldNode> trimmedFieldEnumeration = trimExcludes.getTrimmedFields();

				while (trimmedFieldEnumeration.hasMoreElements()) {
					FieldNode trimmedField = trimmedFieldEnumeration.nextElement();
					if (!trimmedField.getOwnerClass().isInnerClass()) {
						FieldNode removedFieldFromRegistries = this.fieldRegistryByName.remove(trimmedField.getNameDisplay(), trimmedField.getOwnerClass(), trimmedField);
						removedFieldFromRegistries = this.fieldSignatureRegistry.remove(trimmedField.getFieldSignature(), trimmedField.getOwnerClass());
						this.stringFieldNameReferenceMap.removeGroup(trimmedField);
					}
				}

				Enumeration<MethodNode> trimmedMethodEnumeration = trimExcludes.getTrimmedMethods();

				while (trimmedMethodEnumeration.hasMoreElements()) {
					MethodNode trimmedMethod = trimmedMethodEnumeration.nextElement();
					ClassNode methodOwnerClass = trimmedMethod.getMethodOwner();
					if (!methodOwnerClass.isInnerClass()) {
						this.classLookupByMethodSignature.remove(trimmedMethod.getName(), trimmedMethod.getMethodSignature(), methodOwnerClass);
						this.stringMethodNameReferenceMap.removeGroup(trimmedMethod);
						this.usageReferenceMap.removeAll(trimmedMethod);
						this.methodInvocationMap.removeAll(trimmedMethod);
					}
				}

				this.classTreeManager.pruneExcludedInnerClasses(trimExcludes);
				this.classTreeManager.trimNestMembers(trimExcludes);
				this.classTreeManager.prunePermittedSubclasses(trimExcludes);
				ArrayList<ClassNode> keptClasses = new ArrayList<>(this.workspaceClasses.length);

				for (int classIndex = 0; classIndex < this.workspaceClasses.length; classIndex++) {
					ClassNode checkedClass = this.workspaceClasses[classIndex];
					if (trimExcludes.isClassOrInnerClassExcluded(checkedClass)) {
						keptClasses.add(checkedClass);
					} else {
						String classInternalName = checkedClass.getInternalName();
						String binaryClassName = this.internalToBinaryNameMap.remove(classInternalName);
						if (binaryClassName != null) {
							String classCount = this.classNameToInternalNameMap.remove(binaryClassName);
						}

						if (this.fieldRegistry != null) {
							this.fieldRegistry.removeGroup(classInternalName);
						}

						if (this.methodRegistry != null) {
							this.methodRegistry.removeGroup(classInternalName);
						}
					}
				}

				Iterator<MethodNode> processableBootstrapMethodIterator = this.bootstrapReferencedMethods.iterator();

				while (processableBootstrapMethodIterator.hasNext()) {
					MethodNode processableBootstrapMethod = processableBootstrapMethodIterator.next();
					if (trimExcludes.isClassProcessable(processableBootstrapMethod.getMethodOwner())) {
						processableBootstrapMethodIterator.remove();
					}
				}

				Iterator<MethodNode> processableIndyMethodIterator = this.indyReferencedMethods.iterator();

				while (processableIndyMethodIterator.hasNext()) {
					MethodNode processableIndyMethod = processableIndyMethodIterator.next();
					if (trimExcludes.isClassProcessable(processableIndyMethod.getMethodOwner())) {
						processableIndyMethodIterator.remove();
					}
				}

				this.workspaceClasses = keptClasses.toArray(new ClassNode[keptClasses.size()]);
				Arrays.sort(this.workspaceClasses);
				ClassHierarchyNode.resetAll();
				this.classTreeManager.rebuildClassTree(this.workspaceClasses);
				this.packageManager.rebuildPackageTree(this.workspaceClasses);
				ArrayList<ClassNode> retainedClasses = new ArrayList<>(this.workspaceClasses.length);

				for (ClassNode workspaceClass : this.workspaceClasses) {
					if (!workspaceClass.isGeneratedClass()) {
						retainedClasses.add(workspaceClass);
						if (workspaceClass.hasInnerClasses()) {
							for (AbstractClassNode innerClass : workspaceClass.getInnerClasses()) {
								retainedClasses.add((ClassNode)innerClass);
							}
						}
					}
				}

				ClassNode[] retainedClassArray = retainedClasses.toArray(new ClassNode[retainedClasses.size()]);
				this.obfuscateReferencesGraph.retainReferences(retainedClassArray);
				this.autoReflectionReferenceGraph.retainReferences(retainedClassArray);
				this.mpcReferenceGraph.retainReferences(retainedClassArray);

				for (int pruneClassIndex = 0; pruneClassIndex < this.workspaceClasses.length; pruneClassIndex++) {
					ClassNode prunedClass = this.workspaceClasses[pruneClassIndex];
					prunedClass.pruneUnusedReferences(trimExcludes);
					if (prunedClass.hasInnerClasses()) {
						Iterator<AbstractClassNode> innerClassIterator = prunedClass.getInnerClasses().iterator();

						while (innerClassIterator.hasNext()) {
							((ClassNode)innerClassIterator.next()).pruneUnusedReferences(trimExcludes);
						}
					}
				}

				trimExcludes.printTrimReport(printWriter, this, false);
				scriptContext.getTrimExcludedMemberSet().retain(trimExcludes);
				this.markChanged();
				this.notifySimpleChange();
				return;
			} catch (TrimClassNotFoundException trimClassNotFoundError) {
				logger.error("FILE ERROR:", trimClassNotFoundError.getMessage());
				return;
			} catch (ProcessingException trimProcessingError) {
				logger.error("SERIOUS ERROR:", trimProcessingError.getMessage());
			} finally {
				consumer.accept();
			}

			return;
		}
	}

	private boolean checkSuperclassHierarchyWithSnapshot(String className, String target, boolean targetIsWildcard) {
		if (className.equals("java/lang/Object")) {
			return false;
		} else if (target.equals("java/lang/Object")) {
			return true;
		} else if (className.equals(target)) {
			return false;
		} else {
			Boolean cachedMatch = this.superClassHierarchySnapshot.get(target, className);
			if (cachedMatch != null) {
				return cachedMatch;
			} else {
				String superclassName = this.getSuperclassName(className);
				if (!targetIsWildcard ? !superclassName.equals(target) : !StringUtils.matchesWildcard(superclassName, target)) {
					if (superclassName.equals("java/lang/Object")) {
						this.superClassHierarchySnapshot.put(target, className, BOOLEAN_FALSE);
						return false;
					} else {
						boolean superclassMatches = this.checkSuperclassHierarchyWithSnapshot(superclassName, target, targetIsWildcard);
						this.superClassHierarchySnapshot.put(target, className, superclassMatches ? BOOLEAN_TRUE : BOOLEAN_FALSE);
						return superclassMatches;
					}
				} else {
					this.superClassHierarchySnapshot.put(target, className, BOOLEAN_TRUE);
					return true;
				}
			}
		}
	}

	@Override
	public final MethodNode[] getMethodNodesByParameterKey(ClassNode classNode, MethodParameterKey parameterKey) {
		MethodNode[] methodNodes = null;
		if (parameterKey.hasReturnType()) {
			MethodSignature methodSignature = new MethodSignature(parameterKey);
			MethodNode methodNode = this.classToMethodSignatureRegistry.get(classNode, methodSignature);
			if (methodNode != null) {
				methodNodes = new MethodNode[]{methodNode};
			}
		}

		List<MethodNode> methodNodeList = this.classMethodParameterKeyRegistry.getValues(classNode, parameterKey);
		if (methodNodeList != null) {
			methodNodes = new MethodNode[methodNodeList.size()];
			methodNodes = methodNodeList.toArray(methodNodes);
		}

		return methodNodes;
	}

	public ReadOnlyMap<String, String> getPackageRenameMap() {
		return this.packageRenameMap.getReadOnlyMap();
	}

	public boolean isMethodParameterObfuscated() {
		return this.methodParameterObfuscated;
	}

	public String getCopyright() {
		return CopyrightConstants.COPYRIGHT;
	}

	public Set<MethodNode> getProcessingErrors() {
		return this.bootstrapReferencedMethods;
	}

	public void resolveExternalMembers(Set<MethodNode> processedPackageResources, Set<MethodNode> processingErrors, PhantomClassManager phantomClassManager, ScriptContext scriptContext) {
		for (int i = 0; i < this.workspaceClasses.length; i++) {
			ClassNode classNode = this.workspaceClasses[i];
			classNode.resolveExternalMembers(processedPackageResources, this, this.classProvider, phantomClassManager, scriptContext);
			if (classNode.hasInnerClasses()) {
				Iterator<AbstractClassNode> innerClassIterator = classNode.getInnerClasses().iterator();

				while (innerClassIterator.hasNext()) {
					((ClassNode)innerClassIterator.next()).resolveExternalMembers(processedPackageResources, this, this.classProvider, phantomClassManager, scriptContext);
				}
			}
		}

		for (int classIndex = 0; classIndex < this.workspaceClasses.length; classIndex++) {
			ClassNode bootstrapClassNode = this.workspaceClasses[classIndex];
			bootstrapClassNode.collectBootstrapMethodRefs(processingErrors);
			if (bootstrapClassNode.hasInnerClasses()) {
				Iterator<AbstractClassNode> bootstrapInnerClassIterator = bootstrapClassNode.getInnerClasses().iterator();

				while (bootstrapInnerClassIterator.hasNext()) {
					((ClassNode)bootstrapInnerClassIterator.next()).collectBootstrapMethodRefs(processingErrors);
				}
			}
		}
	}

	public final ModuleInfoNode[] getModuleInfoNodes() {
		ModuleInfoNode[] moduleInfoNodesCopy = new ModuleInfoNode[this.moduleInfoNodes.length];
		System.arraycopy(this.moduleInfoNodes, 0, moduleInfoNodesCopy, 0, this.moduleInfoNodes.length);
		return moduleInfoNodesCopy;
	}

	public void markIntegerConstantsEncrypted() {
		this.integerConstantsEncrypted = true;
	}

	public KlassMaster getKlassMaster() {
		return this.klassMaster;
	}

	static String checkEvaluationExpiry(String licenseStr, String scrambledGracePeriod, String scrambledInterval, Workspace workspace) {
		return null;
	}

	public ClassNode[] addClassToWorkspace(ClassNode classNode) {
		ClassHierarchyNode existingHierarchyNode = ClassHierarchyNode.getNodeByInternalName(classNode.getInternalName());
		if (existingHierarchyNode != null) {
			AbstractClassNode existingClassNode = existingHierarchyNode.getClassNode();
			DebugUtils.assertTrue(
				false,
				new String[]{
					"Class '"
						+ classNode.getInternalName()
						+ "' already exists. : "
						+ classNode.getClassType()
						+ " : "
						+ (existingClassNode == null ? "null" : existingClassNode.getClassType() + " : " + existingClassNode.getSourceName())
				}
			);
		}

		ClassNode[] newWorkspaceClasses = new ClassNode[this.workspaceClasses.length + 1];
		System.arraycopy(this.workspaceClasses, 0, newWorkspaceClasses, 0, this.workspaceClasses.length);
		newWorkspaceClasses[this.workspaceClasses.length] = classNode;
		this.workspaceClasses = newWorkspaceClasses;
		Arrays.sort(this.workspaceClasses);
		ClassHierarchyNode.resetAll();
		this.classTreeManager.rebuildClassTree(this.workspaceClasses);
		this.packageManager.rebuildPackageTree(this.workspaceClasses);
		this.rebuildMethodRegistries();
		this.rebuildFieldRegistries();
		classNode.resolveExternalMembers(null, this, this.classProvider, null, null);
		return this.workspaceClasses;
	}

	@Override
	public final boolean isInterfaceOf(String className, String target, ComplexAnnotationSpecifier annotationSpecifier) {
		try {
			boolean wildcardPattern = target.indexOf(42) != -1;
			return this.checkInterfaceHierarchyWildcardWithSnapshot(className, target, wildcardPattern, annotationSpecifier);
		} catch (StackOverflowError stackOverflowError) {
			AbstractClassNode classNode = this.classProvider.getClassNode(className, "reporting corrupt inheritance heirarchy");
			throw new ZKMUnexpectedException(stackOverflowError.getMessage() + " : Class " + classNode.getSourcePath() + " has corrupt hierarchy (D)", stackOverflowError);
		}
	}

	public Set<String> getChangedResourceSet() {
		return this.integrityCheckClassHashes;
	}

	@Override
	public final FieldNode getFieldByNameAndDescriptor(String internalName, String fieldName, String descriptor) {
		if (!TypeWriter.isValidFieldDescriptor(descriptor)) {
			descriptor = TypeWriter.sourceNameToDescriptor(descriptor);
		}

		return this.classToFieldRegistry.get(internalName, fieldName, descriptor);
	}

	public void cleanup() {
		if (this.classRepository != null) {
			this.classRepository.close();
		}

		if (this.classProvider != null) {
			this.classProvider.close();
		}

		if (this.runtimeClassRepository != null) {
			this.runtimeClassRepository.close();
		}

		this.workspaceClasses = null;
		this.classTreeManager = null;
		this.classLookupByMethodSignature = null;
		this.classToMethodSignatureRegistry = null;
		this.classMethodParameterKeyRegistry = null;
		this.fieldRegistryByName = null;
		this.fieldSignatureRegistry = null;
		this.classToFieldRegistry = null;
		this.fileAdaptor.clear();
	}

	public synchronized int loadClasses(
		ClasspathResource[] classpathResources,
		ArchiveNode[] archiveNodes,
		Set<ArchiveNode> nestedArchives,
		Set<ClasspathResource> skippedClassResources,
		Set<ClasspathResource> skippedXmlResources,
		Set<ClasspathResource> skippedYamlResources,
		Set<ClasspathResource> skippedPropertiesResources,
		Set<ClasspathResource> filteredPathResources,
		String skippedClassesTempFilePath,
		ClasspathResource[] jadFileResources,
		ClasspathResource[] xmlFileResources,
		ClasspathResource[] yamlFileResources,
		ClasspathResource[] propertiesFileResources,
		boolean processNestedArchives,
		AbstractLogger logger,
		ZKMConsumer consumer,
		ClassLoadingStatus classLoadingStatus,
		ScriptContext scriptContext,
		PrintWriter loadLogPrintWriter,
		PrintWriter warningLogPrintWriter,
		AtomicInt warningCounter,
		PrintWriter outputPrintWriter
	) throws IOException {
		if (!Options.ZKM_TEST_HIERARCHY) {
			logger.warn(
				"WARNING:",
				"'ZKM_TEST_HIERARCHY' command line option set to 'false'. The inheritance hierarchy will not be tested for gaps. Gaps in the inheritance hierarchy can cause problems in the Trim and Name Obfuscation functions."
			);
		}

		Runtime runtime = Runtime.getRuntime();
		long currentTimeMillis = System.currentTimeMillis();
		this.allArchiveNodes = archiveNodes;
		this.nestedArchives = nestedArchives;
		if (skippedClassResources != null && skippedClassResources.size() > 0) {
			this.skippedClassResources = new DualKeyMap<>();

			for (ClasspathResource annotationResource : skippedClassResources) {
				this.skippedClassResources.put(annotationResource.getArchiveNode(), annotationResource.getResourceName(), annotationResource);
			}
		}

		if (skippedXmlResources != null && skippedXmlResources.size() > 0) {
			this.skippedXmlResources = new DualKeyMap<>();

			for (ClasspathResource nonClassArchiveResource : skippedXmlResources) {
				this.skippedXmlResources.put(nonClassArchiveResource.getArchiveNode(), nonClassArchiveResource.getResourceName(), nonClassArchiveResource);
			}
		}

		if (skippedYamlResources != null && skippedYamlResources.size() > 0) {
			this.skippedYamlResources = new DualKeyMap<>();

			for (ClasspathResource declaredFieldResource : skippedYamlResources) {
				this.skippedYamlResources.put(declaredFieldResource.getArchiveNode(), declaredFieldResource.getResourceName(), declaredFieldResource);
			}
		}

		if (skippedPropertiesResources != null && skippedPropertiesResources.size() > 0) {
			this.skippedPropertiesResources = new DualKeyMap<>();

			for (ClasspathResource unobfuscatableResource : skippedPropertiesResources) {
				this.skippedPropertiesResources.put(unobfuscatableResource.getArchiveNode(), unobfuscatableResource.getResourceName(), unobfuscatableResource);
			}
		}

		if (filteredPathResources != null && filteredPathResources.size() > 0) {
			this.deletedResources = new DualKeyMap<>();

			for (ClasspathResource deletedResource : filteredPathResources) {
				this.deletedResources.put(deletedResource.getArchiveNode(), deletedResource.getResourceName(), deletedResource);
			}
		}

		this.clearHierarchyCaches();
		this.workspaceClasses = null;
		this.moduleInfoNodes = null;
		this.moduleNameToArchiveMultiMap = null;
		this.archiveToModuleNameMap = null;
		this.archiveToClassesMultiMap = null;
		this.hasNonModularSources = false;
		this.classTreeManager = null;
		this.classLookupByMethodSignature = null;
		this.classToMethodSignatureRegistry = null;
		this.classMethodParameterKeyRegistry = null;
		this.fieldRegistryByName = null;
		this.fieldSignatureRegistry = null;
		this.classToFieldRegistry = null;
		this.packageManager = new PackageManager();
		this.packageRenameMap.clear();
		this.classNameToInternalNameMap.clear();
		this.internalToBinaryNameMap.clear();
		this.fieldRegistry.clearAll();
		this.methodRegistry.clearAll();
		this.stringClassReferenceMap.clearAll();
		this.stringResourcePathReferenceMap.clear();
		this.stringInternalClassNameReferenceMap.clearAll();
		this.stringFieldNameReferenceMap.clearAll();
		this.stringMethodNameReferenceMap.clearAll();
		this.processedStringNodes.clear();
		this.usageReferenceMap.clear();
		this.methodInvocationMap.clear();
		this.autoReflectionDataClassNode.setNull();
		this.archiveToReferenceObfuscationMap.clear();
		this.referenceObfuscationDataClassNode.setNull();
		this.archiveToAutoReflectionMap.clear();
		this.defaultMpcClassTripletRef.setNull();
		this.archiveToMpcClassTripletMap.clear();
		this.fileAdaptor.clear();
		this.resourceFilePaths.clear();
		this.jadFileToArchiveNodeMap.clear();
		this.xmlResources = null;
		this.resourceReferencedClasses = null;
		this.resourceReferencedFields = null;
		this.resourceReferencedMethods = null;
		this.methodAccessorReferences = null;
		this.resourceProtectedStaticMethods = null;
		this.classRepository.reinitializeCaches();
		this.indyReferencedMethods.clear();
		this.bootstrapReferencedMethods.clear();
		ClassHierarchyNode.resetAll();
		Thread currentThread = Thread.currentThread();
		if (this.nestedArchives != null && this.nestedArchives.size() > 0) {
			StringBuilder tempFilePathBuffer = new StringBuilder();
			StringBuilder fullPathBuffer = new StringBuilder();
			Iterator<ArchiveNode> warnedArchiveIterator = this.nestedArchives.iterator();

			while (warnedArchiveIterator.hasNext()) {
				ArchiveNode archiveNode = warnedArchiveIterator.next();
				tempFilePathBuffer.append(archiveNode.getFilePath());
				fullPathBuffer.append(archiveNode.getFullPath());
				if (warnedArchiveIterator.hasNext()) {
					tempFilePathBuffer.append(SystemConstants.pathSep);
					fullPathBuffer.append(", ");
				}
			}

			if (this.verbose && fullPathBuffer.length() > 0) {
				String prependMessage;
				if (processNestedArchives) {
					prependMessage = "Prepending unopened nested archive";
				} else {
					prependMessage = "Prepending skipped archive";
				}

				String pluralSuffix = fullPathBuffer.indexOf(", ") == -1 ? "" : "s";
				outputPrintWriter.println(prependMessage + pluralSuffix + " '" + fullPathBuffer + "' to classpath as temporary file" + pluralSuffix + " : '" + tempFilePathBuffer + "'");
			}

			this.classpathManager.prependClasspath(tempFilePathBuffer.toString());
		}

		if (skippedClassesTempFilePath != null && skippedClassesTempFilePath.length() > 0) {
			this.classpathManager.prependClasspath(skippedClassesTempFilePath);
			if (this.verbose) {
				StringBuilder pathBuffer = new StringBuilder();
				HashSet<String> seenResourceNames = GlobalUtils.newHashSet(GlobalUtils.getInitialCapacity(skippedClassResources.size()));

				for (ClasspathResource skippedClassResource : skippedClassResources) {
					if (seenResourceNames.add(skippedClassResource.getResourceName())) {
						if (pathBuffer.length() > 0) {
							pathBuffer.append(", ");
						}

						pathBuffer.append(skippedClassResource.getFullPath());
					}
				}

				outputPrintWriter.println(
					"Prepending skipped class" + (pathBuffer.indexOf(", ") == -1 ? "" : "es") + " '" + pathBuffer + "' to classpath within temporary file '" + skippedClassesTempFilePath + "'"
				);
			}
		}

		this.classpathChanged = false;
		this.fieldNameObfFinish = false;
		this.methodNameObfFinish = false;
		this.hasHierarchySnapshot = false;
		this.methodParameterObfuscated = false;
		this.integerConstantsEncrypted = false;
		this.longConstantsEncrypted = false;
		this.errorMessage = null;
		List<ClassNode> loadedClasses = new Vector<>();
		Vector<ModuleInfoNode> loadedModuleInfos = new Vector<>();

		try {
			loadedClasses = this.loadClassResources(classpathResources, loadedModuleInfos, currentThread, runtime, loadLogPrintWriter, warningLogPrintWriter, outputPrintWriter);
		} catch (ProcessingException classLoadError) {
			logger.fatal("FATAL ERROR:", classLoadError.getMessage());
		}

		if (skippedClassResources != null && skippedClassResources.size() > 0) {
			Iterator<ClasspathResource> skippedClassResourceIterator = skippedClassResources.iterator();

			while (skippedClassResourceIterator.hasNext()) {
				skippedClassResourceIterator.next().close();
			}
		}

		this.refreshClassChangeData(this.klassMaster, 2, 0);
		this.status.set("Analyzing classes");
		int classCount = loadedClasses.size();
		Collections.sort(loadedClasses);
		this.workspaceClasses = new ClassNode[classCount];

		for (int i = 0; i < classCount; i++) {
			this.workspaceClasses[i] = loadedClasses.get(i);
			String classInternalName = this.workspaceClasses[i].getInternalName();
			if (classInternalName.equals("java/lang/Object")
				|| classInternalName.equals("java/lang/invoke/MethodHandle")
				|| this.workspaceClasses[i].getSuperclassName().equals("java/lang/invoke/MethodHandle")) {
				if (classLoadingStatus != null) {
					classLoadingStatus.setStatus(false, null);
				}

				logger.fatal(
					"FATAL ERROR:",
					"Cannot open "
						+ (classInternalName.equals("java/lang/Object") ? "java/lang/Object" : "java/lang/invoke/MethodHandle or its subclasses")
						+ " in this version : '"
						+ this.workspaceClasses[i].getRawSourcePath()
						+ "'"
				);
			}
		}

		for (ClassNode classNode : this.workspaceClasses) {
			if (classNode.hasClassAnnotation() || classNode.hasMethodAnnotationConfig()) {
				classNode.trimAnnotationConfig(scriptContext);
			}
		}

		this.moduleInfoNodes = new ModuleInfoNode[loadedModuleInfos.size()];
		if (this.moduleInfoNodes.length > 0) {
			this.moduleNameToArchiveMultiMap = new MultiMap<>(this.moduleInfoNodes.length, Options.ZKM_REPRODUCIBLE_RANDOM);
			this.archiveToModuleNameMap = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(this.allArchiveNodes.length));

			for (int moduleIndex = 0; moduleIndex < this.moduleInfoNodes.length; moduleIndex++) {
				ModuleInfoNode moduleInfoNode = loadedModuleInfos.get(moduleIndex);
				this.moduleInfoNodes[moduleIndex] = moduleInfoNode;
				String moduleName = moduleInfoNode.getModuleName();
				Enumeration<ClasspathResource> moduleClassSourceEnumeration = moduleInfoNode.getClassSources();

				while (moduleClassSourceEnumeration.hasMoreElements()) {
					ClasspathResource moduleClassSourceResource = moduleClassSourceEnumeration.nextElement();
					if (moduleClassSourceResource.isZipEntry()) {
						ArchiveNode sourceArchive = moduleClassSourceResource.getArchiveNode();
						this.moduleNameToArchiveMultiMap.put(moduleName, sourceArchive);
						this.archiveToModuleNameMap.put(sourceArchive, moduleName);
					}
				}
			}

			for (ArchiveNode moduleArchive : this.allArchiveNodes) {
				if (this.archiveToModuleNameMap.containsKey(moduleArchive)) {
					moduleArchive.setModuleName(this.archiveToModuleNameMap.get(moduleArchive));
				}
			}
		}

		ArrayList<ClassNode> graphClasses = new ArrayList<>(this.workspaceClasses.length + 10);

		for (ClassNode graphClass : this.workspaceClasses) {
			graphClasses.add(graphClass);
			if (graphClass.hasInnerClasses()) {
				Iterator<AbstractClassNode> innerClassIterator = graphClass.getInnerClasses().iterator();

				while (innerClassIterator.hasNext()) {
					graphClasses.add((ClassNode)innerClassIterator.next());
				}
			}
		}

		ClassNode[] graphClassArray = graphClasses.toArray(new ClassNode[graphClasses.size()]);
		this.obfuscateReferencesGraph = new BitSetReferenceGraph<>(graphClassArray, 4, false);
		this.autoReflectionReferenceGraph = new BitSetReferenceGraph<>(graphClassArray, 4, false);
		this.mpcReferenceGraph = new BitSetReferenceGraph<>(graphClassArray, 4, false);
		this.archiveToClassesMultiMap = new MultiMap<>(this.allArchiveNodes.length);

		for (ClassNode dependencyClass : this.workspaceClasses) {
			Enumeration<ClasspathResource> classSourceEnumeration = dependencyClass.getClassSources();

			while (classSourceEnumeration.hasMoreElements()) {
				ClasspathResource classSourceResource = classSourceEnumeration.nextElement();
				if (classSourceResource.isZipEntry()) {
					ArchiveNode dependencyArchive = classSourceResource.getArchiveNode();
					this.archiveToClassesMultiMap.put(dependencyArchive, dependencyClass);
					if (!dependencyArchive.hasModuleName()) {
						this.hasNonModularSources = true;
					}
				} else {
					this.hasNonModularSources = true;
				}
			}
		}

		this.archiveToClassesMultiMap.trimToSize();
		this.packageManager.rebuildPackageTree(this.workspaceClasses);

		try {
			this.classTreeManager = new ClassTreeManager(this.workspaceClasses);
		} catch (ProcessingException classTreeError) {
			logger.fatal("FATAL ERROR:", classTreeError.getMessage());
		}

		this.rebuildMethodRegistries();
		this.rebuildFieldRegistries();

		try {
			this.resolveExternalMembers(
				this.indyReferencedMethods, this.bootstrapReferencedMethods, scriptContext != null ? scriptContext.getPhantomClassManager() : null, scriptContext
			);
		} catch (ClasspathException classpathError) {
			logger.info("ERROR:", classpathError.getMessage());
			this.errorMessage = classpathError.getMessage();
			if (classLoadingStatus != null) {
				classLoadingStatus.setStatus(false, "'" + classpathError.getClassName() + "' not found");
			}
		} catch (MissingMemberException missingComponentError) {
			logger.info("ERROR:", missingComponentError.getMessage());
			this.errorMessage = missingComponentError.getMessage();
			if (classLoadingStatus != null) {
				classLoadingStatus.setStatus(false, "'" + missingComponentError.getMessage());
			}
		} catch (ProcessingException memberResolutionError) {
			logger.fatal("FATAL ERROR:", memberResolutionError.getMessage());
		}

		HierarchicalMultiMap<FieldID, InstructionListNode, Integer> fieldReads = new HierarchicalMultiMap<>(this.workspaceClasses.length * 5, 7, 2);
		HierarchicalMultiMap<FieldID, InstructionListNode, Integer> fieldWrites = new HierarchicalMultiMap<>(this.workspaceClasses.length * 5, 7, 2);
		HierarchicalMultiMap<MethodID, InstructionListNode, Integer> methodInvocations = new HierarchicalMultiMap<>(this.workspaceClasses.length * 5, 7, 2);
		this.collectMemberAccesses(fieldReads, fieldWrites, methodInvocations);

		try {
			this.analyzeReflectionCalls(fieldReads, fieldWrites, methodInvocations, warningLogPrintWriter, warningCounter, this.verbose, logger);
		} catch (ProcessingException reflectionAnalysisError) {
			if (classLoadingStatus != null) {
				classLoadingStatus.setStatus(false, null);
			}

			logger.fatal("FATAL ERROR:", reflectionAnalysisError.getMessage());
		}

		try {
			this.scanArchiveResources();
		} catch (ProcessingException archiveScanError) {
			if (classLoadingStatus != null) {
				classLoadingStatus.setStatus(false, null);
			}

			logger.fatal("FATAL ERROR:", archiveScanError.getMessage());
		}

		MultiMap<String, ArchiveNode> archivesByFileName = new MultiMap<>();

		for (int archiveIndex = 0; archiveIndex < this.allArchiveNodes.length; archiveIndex++) {
			if (this.nestedArchives == null || !this.nestedArchives.contains(this.allArchiveNodes[archiveIndex])) {
				String fileName = this.allArchiveNodes[archiveIndex].getFileName();
				if (!FileUtils.IS_UNIX) {
					fileName = fileName.toLowerCase();
				}

				archivesByFileName.put(fileName, this.allArchiveNodes[archiveIndex]);
			}
		}

		for (int jadFileIndex = 0; jadFileIndex < jadFileResources.length; jadFileIndex++) {
			try {
				JadFileProcessor jadFileProcessor = new JadFileProcessor(jadFileResources[jadFileIndex]);
				long jarSize = -1L;

				try {
					jarSize = jadFileProcessor.getJarSize();
				} catch (InvalidJadAttributeException invalidJadAttributeError) {
					logger.info("WARNING:", "Jad file '" + jadFileResources[jadFileIndex].getFullPath() + "' : " + invalidJadAttributeError.getMessage());
				}

				String jarUrl = jadFileProcessor.getJarUrl();
				if (!FileUtils.IS_UNIX) {
					jarUrl = jarUrl.toLowerCase();
				}

				List<ArchiveNode> archivesForJarUrl = archivesByFileName.get(jarUrl);
				ArchiveNode matchedArchive = null;
				if (archivesForJarUrl != null) {
					if (archivesForJarUrl.size() == 1) {
						matchedArchive = archivesForJarUrl.get(0);
					} else {
						for (int candidateArchiveIndex = 0; candidateArchiveIndex < archivesForJarUrl.size(); candidateArchiveIndex++) {
							ArchiveNode candidateArchive = archivesForJarUrl.get(candidateArchiveIndex);
							if (candidateArchive.getSizeInBytes() == jarSize) {
								matchedArchive = candidateArchive;
								break;
							}
						}
					}
				}

				if (matchedArchive == null) {
					logger.info(
						"WARNING:",
						"JAD file '"
							+ jadFileProcessor.getFullPath()
							+ "' has no associated JAR file '"
							+ jarUrl
							+ "'"
							+ (jarSize > -1L && archivesForJarUrl != null && archivesForJarUrl.size() > 0 ? " with length " + jarSize : "")
							+ ". Cannot automatically update JAR size in JAD file."
					);
				}

				this.jadFileToArchiveNodeMap.put(jadFileProcessor, matchedArchive);
			} catch (ZKMException jadError) {
				logger.error("ERROR:", "Jad file '" + jadFileResources[jadFileIndex].getFullPath() + "' : " + jadError.getMessage());
			}
		}

		this.xmlResources = xmlFileResources;
		ClasspathResource[] allResources = this.xmlResources;
		int resourceCount = allResources.length;

		for (int resourceIndex = 0; resourceIndex < resourceCount; resourceIndex++) {
			this.fileAdaptor.addClasspathFile(allResources[resourceIndex].getAsFile());
		}

		this.yamlResources = yamlFileResources;
		this.propertiesResources = propertiesFileResources;
		this.resourceReferencedClasses = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(this.workspaceClasses.length));
		this.resourceReferencedFields = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(this.workspaceClasses.length * 5));
		this.resourceReferencedMethods = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(this.workspaceClasses.length * 5));
		this.methodAccessorReferences = new DualKeyMap<>(GlobalUtils.getInitialCapacity(this.workspaceClasses.length * 5));
		this.resourceProtectedStaticMethods = GlobalUtils.newHashMap();
		new ResourceFileAnalyzer(
			this.allArchiveNodes,
			this.nestedArchives,
			this.skippedXmlResources,
			this.skippedYamlResources,
			this.deletedResources,
			Collections.enumeration(this.jadFileToArchiveNodeMap.keySet()),
			new ArrayEnumeration<>(this.xmlResources),
			this.fileAdaptor,
			new ArrayEnumeration<>(this.yamlResources),
			new ArrayEnumeration<>(this.propertiesResources),
			this,
			this.classProvider,
			logger,
			this.resourceReferencedClasses,
			this.resourceReferencedFields,
			this.resourceReferencedMethods,
			this.methodAccessorReferences,
			this.resourceProtectedStaticMethods
		);
		this.markChanged();
		this.notifySimpleChange();
		consumer.accept();
		long endTimeMillis = System.currentTimeMillis();
		StringBuilder statusBuffer = new StringBuilder();
		if (this.workspaceClasses.length > 0) {
			statusBuffer.append(this.workspaceClasses.length + " class" + (this.workspaceClasses.length > 1 ? "es" : ""));
		}

		if (this.moduleInfoNodes.length > 0) {
			if (statusBuffer.length() > 0) {
				statusBuffer.append(" and ");
			}

			statusBuffer.append(this.moduleInfoNodes.length + " module-info.class class" + (this.moduleInfoNodes.length > 1 ? "es" : ""));
		}

		if (statusBuffer.length() > 0) {
			statusBuffer.append(" opened in " + String.format("%d seconds. ", (endTimeMillis - currentTimeMillis) / 1000L));
		} else {
			statusBuffer.append("No classes opened. ");
		}

		statusBuffer.append((runtime.totalMemory() - runtime.freeMemory()) / 1024L + "K of memory used.");
		this.status.set(statusBuffer.toString());
		statusBuffer.append(" " + runtime.freeMemory() / 1024L + "K of memory free.");
		outputPrintWriter.println(statusBuffer.toString());
		return this.getClassesCount();
	}

	public void setCustomXmlOutputPaths(HashMap<ClasspathResource, File> classNameToObfuscatedNameMap) {
		this.customXmlOutputPaths = classNameToObfuscatedNameMap;
	}

	private String getSuperclassInternalName(String internalName) {
		ClassHierarchyNode hierarchyNode = ClassHierarchyNode.getNodeByInternalName(internalName);
		String superclassName;
		Object classNode;
		if (hierarchyNode != null && !hierarchyNode.isMissing()) {
			classNode = hierarchyNode.getClassNode();
			superclassName = hierarchyNode.getOuterClass().getInternalName();
		} else {
			classNode = this.classRepository.loadClassOrThrow(internalName);
			superclassName = ((AbstractClassNode)classNode).getSuperclassName();
		}

		String linkError;
		if (Options.ZKM_TEST_HIERARCHY
			&& !((AbstractClassNode)classNode).isGeneratedClass()
			&& (linkError = ClassHierarchyNode.validateInheritanceLink((AbstractClassNode)classNode, this.classProvider.getClassNode(superclassName), 1)) != null) {
			throw new LookupException(linkError);
		} else {
			this.superClassHierarchyCache.put(superclassName, internalName, BOOLEAN_TRUE);
			return superclassName;
		}
	}

	private Enumeration<String> getDirectInterfaceNames(String internalName, AtomicReference<AbstractClassNode> outClassNode) {
		ClassHierarchyNode hierarchyNode = ClassHierarchyNode.getNodeByInternalName(internalName);
		AbstractClassNode classNode;
		String[] interfaceNames;
		if (hierarchyNode != null && !hierarchyNode.isMissing()) {
			classNode = hierarchyNode.getClassNode();
			interfaceNames = hierarchyNode.getDirectInterfaceNames();
		} else {
			classNode = this.classRepository.loadClassOrThrow(internalName);
			interfaceNames = classNode.getInterfaceNames();
		}

		for (String interfaceName : interfaceNames) {
			this.interfaceHierarchyCache.put(interfaceName, internalName, BOOLEAN_TRUE);
		}

		outClassNode.set(classNode);
		return new ArrayEnumeration<>(interfaceNames);
	}

	private Set<String> collectOriginalAnnotationTypes(String internalName, String binaryName, Integer innerClassOrdinal) {
		try {
			if (this.annotationReferenceSnapshot.hasMember(binaryName)) {
				return new LinkedHashSet<>(this.annotationReferenceSnapshot.getReferences(binaryName));
			} else {
				LinkedHashSet<String> annotationTypes = new LinkedHashSet<>(13);
				String errorContext = "analyzing the annotations of class '" + GlobalUtils.internalToBinaryName(binaryName) + "'";
				this.collectMappedAnnotationTypes(internalName, binaryName, innerClassOrdinal, annotationTypes, false, errorContext);
				this.annotationReferenceSnapshot.setReferences(binaryName, annotationTypes);
				return annotationTypes;
			}
		} catch (StackOverflowError stackOverflowError) {
			AbstractClassNode classNode = this.classProvider.getClassNode(internalName, "reporting corrupt inheritance hierarchy");
			throw new ZKMUnexpectedException(stackOverflowError.getMessage() + " : Class " + classNode.getSourcePath() + " has corrupt hierarchy (C)", stackOverflowError);
		}
	}

	public ClasspathResource[] getYamlResources() {
		ClasspathResource[] resourceArray = new ClasspathResource[this.yamlResources.length];
		System.arraycopy(this.yamlResources, 0, resourceArray, 0, this.yamlResources.length);
		return resourceArray;
	}

	public final boolean hasWorkspaceClasses() {
		return this.workspaceClasses != null;
	}

	private <I> void removeStringReferences(DualKeyMap<I, Reference, ?> referenceMap, Set<StringNode> stringNodes) {
		SnapshotEnumerationAdapter<I> groupKeyEnumeration = new SnapshotEnumerationAdapter<>(referenceMap.getGroupKeys());

		while (groupKeyEnumeration.hasMoreElements()) {
			I groupKey = groupKeyEnumeration.nextElement();
			Map<Reference, ?> referenceGroup = referenceMap.getGroup(groupKey);
			SnapshotEnumerator<Reference> referenceEnumerator = new SnapshotEnumerator<>(referenceGroup.keySet());

			while (referenceEnumerator.hasMoreElements()) {
				Reference reference = referenceEnumerator.nextElement();
				if (reference instanceof StringReference) {
					StringNode stringNode = ((StringReference)reference).getStringNode();
					if (stringNodes.contains(stringNode)) {
						referenceMap.remove(groupKey, reference);
					}
				}
			}
		}
	}

	public List<ClassNode> getClassesWithReferencingStrings(MethodNode methodNode) {
		ArrayList<ClassNode> classes = new ArrayList<>();
		List<StructureNode> structureNodes = this.usageReferenceMap.get(methodNode);
		if (structureNodes != null) {
			for (StructureNode structureNode : structureNodes) {
				if (structureNode instanceof ClassNode && ((ClassNode)structureNode).isWorkspaceNode()) {
					classes.add((ClassNode)structureNode);
				}
			}
		}

		return classes;
	}

	@Override
	public final void registerMethod(MethodNode methodNode, ClassNode classNode) {
		MethodSignature methodSignature = methodNode.getMethodSignature();
		this.classLookupByMethodSignature.put(methodNode.getName(), methodSignature, classNode);
		MethodNode previousMethod = this.classToMethodSignatureRegistry.put(classNode, methodSignature, methodNode);
		if (previousMethod != null) {
			DebugUtils.assertTrue(
				false,
				new String[]{
					"Duplicate method in class '"
						+ classNode.getBinaryName()
						+ "' opened from "
						+ classNode.getRawSourcePath()
						+ " - "
						+ methodSignature
						+ " : "
						+ previousMethod.getMethodSignature().toFullString()
						+ ""
				}
			);
		}

		this.classMethodParameterKeyRegistry.put(classNode, methodSignature.getParameterKey(), methodNode);
	}

	public boolean isLongConstantsEncrypted() {
		return this.longConstantsEncrypted;
	}

	@Override
	public final boolean isClassInstanceOf(String className, String target) {
		try {
			boolean targetIsWildcard = target.indexOf(42) != -1;
			return this.checkClassInstanceOfWithSnapshot(className, target, targetIsWildcard);
		} catch (StackOverflowError stackOverflowError) {
			AbstractClassNode classNode = this.classProvider.getClassNode(className, "reporting corrupt inheritance hierarchy");
			throw new ZKMUnexpectedException(stackOverflowError.getMessage() + " : Class " + classNode.getSourcePath() + " has corrupt inheritance hierarchy (A)", stackOverflowError);
		}
	}

	private Set<String> collectMappedAnnotationTypes(String internalName, String binaryName, Integer innerClassOrdinal, Set<String> annotationTypes, boolean onlyInheritedAnnotations, String errorContext) {
		AbstractClassNode classNode = this.classProvider.getClassNode(internalName, innerClassOrdinal, errorContext);
		Enumeration<String> annotationTypeNameEnumeration = classNode.getAnnotationTypeNames();

		while (annotationTypeNameEnumeration.hasMoreElements()) {
			String annotationTypeName = annotationTypeNameEnumeration.nextElement();
			if (onlyInheritedAnnotations) {
				AbstractClassNode annotationClassNode = this.classProvider.getClassNode(annotationTypeName, errorContext);
				if (annotationClassNode.hasAnnotation("java/lang/annotation/Inherited")) {
					annotationTypes.add(annotationClassNode.getClassName());
				}
			} else {
				annotationTypes.add(GlobalUtils.getMappedOrDefault(annotationTypeName, this.internalToBinaryNameMap));
			}
		}

		String superclassInternalName = classNode.getSuperclassName();
		String superclassBinaryName = GlobalUtils.getMappedOrDefault(superclassInternalName, this.internalToBinaryNameMap);
		if (superclassInternalName != null) {
			this.collectMappedAnnotationTypes(superclassInternalName, superclassBinaryName, innerClassOrdinal, annotationTypes, true, errorContext);
		}

		return annotationTypes;
	}

	public void fieldNameObfFinish() {
		this.fieldNameObfFinish = true;
	}

	public final Enumeration<ModuleInfoNode> getModuleInfoEnumeration() {
		return new ArrayEnumeration<>(this.moduleInfoNodes);
	}

	@Override
	public boolean isFieldNameObfFinish() {
		return this.fieldNameObfFinish;
	}

	public void applyInnerClassMethodRenames(AbstractClassNode[] classNodes, DualKeyMap<String, MethodSignature, MethodSignature> methodSignatureMappings, ScriptContext scriptContext) {
		for (int i = 0; i < classNodes.length; i++) {
			AbstractClassNode classNode = classNodes[i];
			if (classNode.hasInnerClasses()) {
				classNode.applyInnerClassMethodRenames(methodSignatureMappings, scriptContext);
			}
		}
	}

	public Iterator<String> getResourceFilePathsIterator() {
		return Collections.unmodifiableSet(this.resourceFilePaths).iterator();
	}

	private void processMultiReleaseModules(
		List<ModuleInfoNode> modules, Map<String, ModuleInfoNode> modulesByName, KeyValuePairMultiMap<String, ModuleInfoNode, Integer> multiReleaseModules, PrintWriter errorPrintWriter, PrintWriter verbosePrintWriter
	) {
		KeyValuePairMultiMap<ModuleInfoNode, ModuleInfoNode, Integer> acceptedVersionsByModule = null;
		DualKeyMap<String, Integer, ModuleInfoNode> versionedModulesByModuleName = null;

		for (Entry<String, List<KeyValuePair<ModuleInfoNode, Integer>>> multiReleaseEntry : multiReleaseModules.entrySet()) {
			String moduleName = multiReleaseEntry.getKey();
			List<KeyValuePair<ModuleInfoNode, Integer>> versionedModules = multiReleaseEntry.getValue();
			ModuleInfoNode baseModule = modulesByName.get(moduleName);
			ModuleInfoNode firstVersionedModule = versionedModules.get(0).getKey();
			if (baseModule == null) {
				if (versionedModulesByModuleName == null) {
					versionedModulesByModuleName = new DualKeyMap<>();
				}

				for (KeyValuePair<ModuleInfoNode, Integer> moduleVersionPair : versionedModules) {
					ModuleInfoNode checkedModule = moduleVersionPair.getKey();
					ModuleInfoNode previousModule = versionedModulesByModuleName.put(checkedModule.getModuleName(), moduleVersionPair.getValue(), checkedModule);
					if (previousModule != null) {
						errorPrintWriter.println(
							"ERROR: Modules '"
								+ previousModule.getSourcePath()
								+ "' and '"
								+ checkedModule.getSourcePath()
								+ "' both have the same multi-release version '"
								+ moduleVersionPair.getValue()
								+ "'. Class '"
								+ previousModule.getSourcePath()
								+ "'. The muti-release version will be ignored. (D)"
						);
					}
				}
			} else {
				int classSourceCount = baseModule.getClassSourceCount();
				if (classSourceCount <= 1) {
					ClasspathResource baseModuleSource = baseModule.getPrimaryClassSource();
					if (!baseModuleSource.isZipEntry()) {
						errorPrintWriter.println(
							"ERROR: Module '"
								+ baseModule.getBinaryName()
								+ "' has a multi-release version '"
								+ firstVersionedModule.getSourcePath()
								+ "' but the base class is not in an archive : '"
								+ baseModule.getSourcePath()
								+ "'. The muti-release version will be ignored. (F)"
						);
					} else {
						ArchiveNode baseModuleArchive = baseModuleSource.getArchiveNode();

						for (KeyValuePair<ModuleInfoNode, Integer> assignedModulePair : versionedModules) {
							ModuleInfoNode assignedModule = assignedModulePair.getKey();
							ArchiveNode assignedModuleArchive = assignedModule.getPrimaryClassSource().getArchiveNode();
							if (assignedModuleArchive.equals(baseModuleArchive)) {
								int assignedVersion = assignedModulePair.getValue();
								baseModule.addInnerClass(assignedModule, assignedVersion);
								if (this.verbose) {
									if (acceptedVersionsByModule == null) {
										acceptedVersionsByModule = new KeyValuePairMultiMap<>();
									}

									acceptedVersionsByModule.put(baseModule, assignedModule, assignedVersion);
								}
							} else {
								errorPrintWriter.println(
									"ERROR: Module '"
										+ baseModule.getSourcePath()
										+ "' has a multi-release version '"
										+ assignedModule.getSourcePath()
										+ "' but they are not inside the same JAR file. The multi-release version will be ignored. (A)"
								);
							}
						}
					}
				} else {
					ArrayList<String> sourcePaths = new ArrayList<>(classSourceCount);
					Enumeration<ClasspathResource> classSourceEnumeration = baseModule.getClassSources();

					while (classSourceEnumeration.hasMoreElements()) {
						sourcePaths.add(classSourceEnumeration.nextElement().getFullPath());
					}

					errorPrintWriter.println(
						"ERROR: Module '"
							+ baseModule.getBinaryName()
							+ "' has a multi-release version '"
							+ firstVersionedModule.getSourcePath()
							+ "' but it has been opened from more than one place : "
							+ GlobalUtils.toQuotedString(sourcePaths)
							+ ". The muti-release version will be ignored. (E)"
					);
				}
			}
		}

		if (versionedModulesByModuleName != null) {
			for (Entry<String, Map<Integer, ModuleInfoNode>> versionEntry : versionedModulesByModuleName.getEntrySet()) {
				String unusedEntryKey = versionEntry.getKey();
				Map<Integer, ModuleInfoNode> modulesByVersionNumber = versionEntry.getValue();
				TreeMap<Integer, ModuleInfoNode> sortedModulesByVersion = new TreeMap<>(modulesByVersionNumber);
				ModuleInfoNode lowestVersionModule = null;
				ArchiveNode lowestVersionArchive = null;

				for (Entry<Integer, ModuleInfoNode> sortedVersionEntry : sortedModulesByVersion.entrySet()) {
					int versionNumber = sortedVersionEntry.getKey();
					ModuleInfoNode candidateModule = sortedVersionEntry.getValue();
					if (lowestVersionModule == null) {
						lowestVersionModule = candidateModule;
						lowestVersionArchive = candidateModule.getPrimaryClassSource().getArchiveNode();
						candidateModule.setInnerClassOrdinal(sortedVersionEntry.getKey());
						modules.add(candidateModule);
					} else if (candidateModule.getPrimaryClassSource().getArchiveNode() == lowestVersionArchive) {
						lowestVersionModule.addInnerClass(candidateModule, versionNumber);
						if (this.verbose) {
							if (acceptedVersionsByModule == null) {
								acceptedVersionsByModule = new KeyValuePairMultiMap<>();
							}

							acceptedVersionsByModule.put(lowestVersionModule, candidateModule, versionNumber);
						}
					} else {
						errorPrintWriter.println(
							"ERROR: Class '"
								+ lowestVersionModule.getSourcePath()
								+ "' has a multi-release version '"
								+ candidateModule.getSourcePath()
								+ "' but they are not inside the same JAR file. The multi-release version will be ignored. (B)"
						);
					}
				}
			}
		}

		if (this.verbose && acceptedVersionsByModule != null) {
			verbosePrintWriter.println("Multi-release modules found...");

			for (ModuleInfoNode moduleWithVersions : new TreeSet<>(acceptedVersionsByModule.keySet())) {
				List<KeyValuePair<ModuleInfoNode, Integer>> versionsForModule = acceptedVersionsByModule.get(moduleWithVersions);
				verbosePrintWriter.println("\tModule '" + moduleWithVersions.getModuleName() + "' (" + moduleWithVersions.getSourcePath() + ") has version" + (versionsForModule.size() > 0 ? "s" : ""));
				Workspace.ModuleVersionComparator versionComparator = new Workspace.ModuleVersionComparator(this);
				Collections.sort(versionsForModule, versionComparator);

				for (KeyValuePair<ModuleInfoNode, Integer> versionedModulePair : versionsForModule) {
					verbosePrintWriter.println("\t\tVersion " + versionedModulePair.getValue() + " '" + versionedModulePair.getKey().getPrimaryClassSource().getResourceName() + "'");
				}
			}
		}
	}

	public ReadOnlyRegistryView<AbstractClassNode, FieldSignature, FieldSignature> getFieldRegistryReadOnlyView() {
		return new ReadOnlyRegistryView<>(this.fieldRegistry);
	}

	private void processMultiReleaseClasses(
		List<ClassNode> classes, Map<String, ClassNode> classesByName, KeyValuePairMultiMap<String, ClassNode, Integer> multiReleaseClasses, PrintWriter errorPrintWriter, PrintWriter verbosePrintWriter
	) {
		KeyValuePairMultiMap<ClassNode, ClassNode, Integer> acceptedVersionsByClass = null;
		DualKeyMap<String, Integer, ClassNode> versionedClassesByClassName = null;

		for (Entry<String, List<KeyValuePair<ClassNode, Integer>>> multiReleaseEntry : multiReleaseClasses.entrySet()) {
			String className = multiReleaseEntry.getKey();
			List<KeyValuePair<ClassNode, Integer>> versionedClasses = multiReleaseEntry.getValue();
			ClassNode baseClass = classesByName.get(className);
			ClassNode firstVersionedClass = versionedClasses.get(0).getKey();
			if (baseClass == null) {
				if (versionedClassesByClassName == null) {
					versionedClassesByClassName = new DualKeyMap<>();
				}

				for (KeyValuePair<ClassNode, Integer> classVersionPair : versionedClasses) {
					ClassNode checkedClass = classVersionPair.getKey();
					ClassNode previousClass = versionedClassesByClassName.put(checkedClass.getInternalName(), classVersionPair.getValue(), checkedClass);
					if (previousClass != null) {
						errorPrintWriter.println(
							"ERROR: Classes '"
								+ previousClass.getSourcePath()
								+ "' and '"
								+ checkedClass.getSourcePath()
								+ "' both have the same multi-release version '"
								+ classVersionPair.getValue()
								+ "'. Class '"
								+ previousClass.getSourcePath()
								+ "'. The muti-release version will be ignored. (A)"
						);
					}
				}
			} else {
				int classSourceCount = baseClass.getClassSourceCount();
				if (classSourceCount <= 1) {
					ClasspathResource baseClassSource = baseClass.getPrimaryClassSource();
					if (!baseClassSource.isZipEntry()) {
						errorPrintWriter.println(
							"ERROR: Class '"
								+ baseClass.getBinaryName()
								+ "' has a multi-release version '"
								+ firstVersionedClass.getSourcePath()
								+ "' but the base class is not in an archive : '"
								+ baseClass.getSourcePath()
								+ "'. The muti-release version will be ignored. (C)"
						);
					} else {
						ArchiveNode baseClassArchive = baseClassSource.getArchiveNode();

						for (KeyValuePair<ClassNode, Integer> assignedClassPair : versionedClasses) {
							ClassNode assignedClass = assignedClassPair.getKey();
							ArchiveNode assignedClassArchive = assignedClass.getPrimaryClassSource().getArchiveNode();
							if (assignedClassArchive.equals(baseClassArchive)) {
								int assignedVersion = assignedClassPair.getValue();
								baseClass.addInnerClass(assignedClass, assignedVersion);
								if (this.verbose) {
									if (acceptedVersionsByClass == null) {
										acceptedVersionsByClass = new KeyValuePairMultiMap<>();
									}

									acceptedVersionsByClass.put(baseClass, assignedClass, assignedVersion);
								}
							} else {
								errorPrintWriter.println(
									"ERROR: Class '"
										+ baseClass.getSourcePath()
										+ "' has a multi-release version '"
										+ assignedClass.getSourcePath()
										+ "' but they are not inside the same JAR file. The multi-release version will be ignored. (A)"
								);
							}
						}
					}
				} else {
					ArrayList<String> sourcePaths = new ArrayList<>(classSourceCount);
					Enumeration<ClasspathResource> classSourceEnumeration = baseClass.getClassSources();

					while (classSourceEnumeration.hasMoreElements()) {
						sourcePaths.add(classSourceEnumeration.nextElement().getFullPath());
					}

					errorPrintWriter.println(
						"ERROR: Class '"
							+ baseClass.getBinaryName()
							+ "' has a multi-release version '"
							+ firstVersionedClass.getSourcePath()
							+ "' but it has been opened from more than one place : "
							+ GlobalUtils.toQuotedString(sourcePaths)
							+ ". The muti-release version will be ignored. (B)"
					);
				}
			}
		}

		if (versionedClassesByClassName != null) {
			for (Entry<String, Map<Integer, ClassNode>> versionEntry : versionedClassesByClassName.getEntrySet()) {
				String unusedEntryKey = versionEntry.getKey();
				Map<Integer, ClassNode> classesByVersionNumber = versionEntry.getValue();
				TreeMap<Integer, ClassNode> sortedClassesByVersion = new TreeMap<>(classesByVersionNumber);
				ClassNode lowestVersionClass = null;
				ArchiveNode lowestVersionArchive = null;

				for (Entry<Integer, ClassNode> sortedVersionEntry : sortedClassesByVersion.entrySet()) {
					int versionNumber = sortedVersionEntry.getKey();
					ClassNode candidateClass = sortedVersionEntry.getValue();
					if (lowestVersionClass == null) {
						lowestVersionClass = candidateClass;
						lowestVersionArchive = candidateClass.getPrimaryClassSource().getArchiveNode();
						candidateClass.setInnerClassOrdinal(sortedVersionEntry.getKey());
						classes.add(candidateClass);
					} else if (candidateClass.getPrimaryClassSource().getArchiveNode() == lowestVersionArchive) {
						lowestVersionClass.addInnerClass(candidateClass, versionNumber);
						if (this.verbose) {
							if (acceptedVersionsByClass == null) {
								acceptedVersionsByClass = new KeyValuePairMultiMap<>();
							}

							acceptedVersionsByClass.put(lowestVersionClass, candidateClass, versionNumber);
						}
					} else {
						errorPrintWriter.println(
							"ERROR: Class '"
								+ lowestVersionClass.getSourcePath()
								+ "' has a multi-release version '"
								+ candidateClass.getSourcePath()
								+ "' but they are not inside the same JAR file. The multi-release version will be ignored. (B)"
						);
					}
				}
			}
		}

		if (this.verbose && acceptedVersionsByClass != null) {
			verbosePrintWriter.println("Multi-release classes found...");

			for (ClassNode classWithVersions : new TreeSet<>(acceptedVersionsByClass.keySet())) {
				List<KeyValuePair<ClassNode, Integer>> versionsForClass = acceptedVersionsByClass.get(classWithVersions);
				verbosePrintWriter.println("\tClass '" + classWithVersions.getSourcePath() + "' has version" + (versionsForClass.size() > 0 ? "s" : ""));
				ClassVersionComparator versionComparator = new ClassVersionComparator(this);
				Collections.sort(versionsForClass, versionComparator);

				for (KeyValuePair<ClassNode, Integer> versionedClassPair : versionsForClass) {
					verbosePrintWriter.println("\t\tVersion " + versionedClassPair.getValue() + " '" + versionedClassPair.getKey().getPrimaryClassSource().getResourceName() + "'");
				}
			}
		}
	}

	public final synchronized void performObfuscation(
		ChangeLogFileData[] changelogIn,
		boolean isLooseChangeLogFile,
		PrintWriter changeLogPrintWriter,
		String changeLogFileOut,
		String useFileEncoding,
		List<? extends ExclusionInclusionStatement> excludeStatements,
		List<UnexcludeStatement> unexcludeStatements,
		List<ObfuscateFlowExcludeStatement> obfuscateFlowExcludeStatements,
		List<ObfuscateFlowUnexcludeStatement> obfuscateFlowUnexcludeStatements,
		List<ObfuscateExceptionsExcludeStatement> obfuscateExceptionsExcludeStatements,
		List<ObfuscateExceptionsUnexcludeStatement> obfuscateExceptionsUnexcludeStatements,
		List<StringEncryptionExcludeStatement> encryptStringLiteralsExcludeStatements,
		List<StringEncryptionUnexcludeStatement> encryptStringLiteralsUnexcludeStatements,
		List<IntegerEncryptionExcludeStatement> integerEncryptionExcludeStatements,
		List<IntegerEncryptionUnexcludeStatement> integerEncryptionUnexcludeStatements,
		List<LongEncryptionExcludeStatement> longEncryptionExcludeStatements,
		List<LongEncryptionUnexcludeStatement> longEncryptionUnexcludeStatements,
		List<ExistingSerializedClassesStatement> existingSerializedClassesStatements,
		List<FixedClassesStatement> fixedClassesStatements,
		List<GroupingsStatement> groupingsStatements,
		List<AccessedByReflectionStatement> accessedByReflectionStatements,
		List<AccessedByReflectionExcludeStatement> accessedByReflectionExcludeStatements,
		List<ObfuscateReferencesIncludeStatement> obfuscateReferencesIncludeStatements,
		List<ObfuscateReferencesExcludeStatement> obfuscateReferencesExcludeStatements,
		List<MethodParameterChangesIncludeStatement> methodParameterChangesIncludeStatements,
		List<MethodParameterChangesExcludeStatement> methodParameterChangesExcludeStatements,
		List<MethodParameterObfuscationIncludeStatement> methodParameterObfuscationIncludeStatements,
		List<MethodParameterObfuscationExcludeStatement> methodParameterObfuscationExcludeStatements,
		boolean legalIdentifiers,
		String newNamesPrefix,
		boolean thideFieldNames,
		boolean hideStaticMethodNames,
		boolean aggressiveMethodRenaming,
		boolean randomizeObfuscation,
		boolean allClassesOpened,
		boolean deriveGroupingsFromInputChangeLog,
		int keepInnerClassInfo,
		int keepGenericsInformation,
		int obfuscateControlFlow,
		int obfuscateParameters,
		int encryptStringLiterals,
		int encryptIntegerConstants,
		int encryptLongConstants,
		int lineNumbers,
		int localVariableTables,
		int methodParameterOption,
		boolean collapsePackagesWithDefault,
		String collapsePackagesWithDefaultValue,
		int exceptionObfuscation,
		int autoReflectionHandling,
		String autoReflectionPackage,
		String autoReflectionHash,
		int obfuscateReferences,
		boolean obfuscateReferenceInSpecialClass,
		String obfuscateReferencesPackage,
		boolean preverify,
		boolean makeClassesPublic,
		boolean keepBalancedLocks,
		MixCaseType mixedCaseClassNames,
		boolean uniqueClassNames,
		boolean uniqueMethodNames,
		int methodParameterChanges,
		String methodParameterChangesPackage,
		Integer assumeRuntimeVersionStringInteger,
		String newPackageNameFile,
		String newClassNameFile,
		String newFieldNameFile,
		String newMethodNameFile,
		boolean useAsciiCharactersOnly,
		AbstractLogger logger,
		ZKMConsumer unknownConsumer,
		ScriptContext context
	) throws IOException {
		if (false) {
			checkEvaluationExpiry(this.klassMaster.getLicenseStr(), null, null, this);
		}

		if (newFieldNameFile != null && newFieldNameFile.length() > 0) {
			this.overrideFieldNames = Obfuscator.loadOverrideNamesFromFile(newFieldNameFile, "newFieldNameFile", context);
		}

		if (newMethodNameFile != null && newMethodNameFile.length() > 0) {
			this.overrideMethodNames = Obfuscator.loadOverrideNamesFromFile(newMethodNameFile, "newMethodNameFile", context);
		}

		new Obfuscator(
			this,
			this.workspaceClasses,
			this.moduleInfoNodes,
			this.packageManager,
			this.status,
			changelogIn,
			isLooseChangeLogFile,
			changeLogPrintWriter,
			changeLogFileOut,
			useFileEncoding,
			excludeStatements,
			unexcludeStatements,
			obfuscateFlowExcludeStatements,
			obfuscateFlowUnexcludeStatements,
			obfuscateExceptionsExcludeStatements,
			obfuscateExceptionsUnexcludeStatements,
			encryptStringLiteralsExcludeStatements,
			encryptStringLiteralsUnexcludeStatements,
			integerEncryptionExcludeStatements,
			integerEncryptionUnexcludeStatements,
			longEncryptionExcludeStatements,
			longEncryptionUnexcludeStatements,
			existingSerializedClassesStatements,
			fixedClassesStatements,
			groupingsStatements,
			accessedByReflectionStatements,
			accessedByReflectionExcludeStatements,
			obfuscateReferencesIncludeStatements,
			obfuscateReferencesExcludeStatements,
			methodParameterChangesIncludeStatements,
			methodParameterChangesExcludeStatements,
			methodParameterObfuscationIncludeStatements,
			methodParameterObfuscationExcludeStatements,
			legalIdentifiers,
			newNamesPrefix,
			thideFieldNames,
			hideStaticMethodNames,
			aggressiveMethodRenaming,
			randomizeObfuscation,
			allClassesOpened,
			deriveGroupingsFromInputChangeLog,
			keepInnerClassInfo,
			keepGenericsInformation,
			obfuscateControlFlow,
			obfuscateParameters,
			encryptStringLiterals,
			encryptIntegerConstants,
			encryptLongConstants,
			lineNumbers,
			localVariableTables,
			methodParameterOption,
			collapsePackagesWithDefault,
			collapsePackagesWithDefaultValue,
			exceptionObfuscation,
			autoReflectionHandling,
			autoReflectionPackage,
			autoReflectionHash,
			this.referenceObfuscationDataClassNode,
			this.archiveToAutoReflectionMap,
			this.obfuscateReferencesGraph,
			obfuscateReferences,
			obfuscateReferenceInSpecialClass,
			obfuscateReferencesPackage,
			this.autoReflectionDataClassNode,
			this.archiveToReferenceObfuscationMap,
			this.autoReflectionReferenceGraph,
			preverify,
			makeClassesPublic,
			keepBalancedLocks,
			mixedCaseClassNames,
			uniqueClassNames,
			uniqueMethodNames,
			methodParameterChanges,
			methodParameterChangesPackage,
			assumeRuntimeVersionStringInteger,
			newPackageNameFile,
			newClassNameFile,
			this.overrideFieldNames,
			this.overrideMethodNames,
			useAsciiCharactersOnly,
			new UnmodifiableMultiMap<>(this.archiveToClassesMultiMap),
			this.defaultMpcClassTripletRef,
			this.archiveToMpcClassTripletMap,
			this.mpcReferenceGraph,
			this.resourceProtectedStaticMethods,
			logger,
			unknownConsumer,
			context
		);
	}

	private boolean checkSuperclassHierarchyOriginals(String className, String target, boolean targetIsWildcard, ComplexAnnotationSpecifier annotationSpecifier) {
		if (className.equals("java/lang/Object")) {
			return false;
		} else if (className.equals(target)) {
			return false;
		} else {
			String superclassName = this.getSuperclassName(className);
			if (!targetIsWildcard && superclassName.equals(target)) {
				return annotationSpecifier.matchesAnnotationTypes(this.getAnnotationTypes(superclassName, null, true));
			} else if (targetIsWildcard && StringUtils.matchesWildcard(superclassName, target) && annotationSpecifier.matchesAnnotationTypes(this.getAnnotationTypes(superclassName, null, true))) {
				return true;
			} else {
				return superclassName.equals("java/lang/Object") ? false : this.checkSuperclassHierarchyOriginals(superclassName, target, targetIsWildcard, annotationSpecifier);
			}
		}
	}

	public void resolveRefsAndSyncMetadata(AbstractClassNode[] classNodes, ScriptContext scriptContext, boolean pruneParameterSignatures) {
		for (int i = 0; i < classNodes.length; i++) {
			AbstractClassNode classNode = classNodes[i];
			classNode.resolveRefsAndSyncMetadata(scriptContext, this, pruneParameterSignatures);
			if (classNode.hasInnerClasses()) {
				Iterator<AbstractClassNode> innerClassIterator = classNode.getInnerClasses().iterator();

				while (innerClassIterator.hasNext()) {
					innerClassIterator.next().resolveRefsAndSyncMetadata(scriptContext, this, pruneParameterSignatures);
				}
			}
		}
	}

	@Override
	public final boolean isInterfaceImplementedBy(String className, String target, ComplexAnnotationSpecifier annotationSpecifier) {
		try {
			boolean targetIsWildcard = target.indexOf(42) != -1;
			return this.checkInterfaceHierarchyWildcard(className, target, targetIsWildcard, annotationSpecifier);
		} catch (StackOverflowError stackOverflowError) {
			AbstractClassNode corruptHierarchyClassNode = this.classProvider.getClassNode(className, "reporting corrupt inheritance heirarchy");
			throw new ZKMUnexpectedException(stackOverflowError.getMessage() + " : Class " + corruptHierarchyClassNode.getSourcePath() + " has corrupt hierarchy (B)", stackOverflowError);
		}
	}

	@Override
	public void onChange(INotifier notifier, Object changeKind, Object changedNode, Object changeArg) {
		this.clearHierarchyCaches();
		this.classpathChanged = true;
	}

	public void remapResourceFilePaths(HashMap<String, String> packageNameMappings, AbstractLogger logger) {
		ReadOnlyMap<String, String> readOnlyMappings = new ReadOnlyMap<>(packageNameMappings);
		HashMap<String, String> originalPathByRemappedPath = GlobalUtils.newHashMap();
		Iterator<String> pathIterator = this.resourceFilePaths.iterator();

		while (pathIterator.hasNext()) {
			String originalPath = pathIterator.next();
			String remappedPath = PathRemapper.remapDirectoryOfPath(originalPath, readOnlyMappings);
			String previousOriginalPath = null;
			if (!remappedPath.equals(originalPath)) {
				pathIterator.remove();
				previousOriginalPath = originalPathByRemappedPath.put(remappedPath, originalPath);
			}

			if (previousOriginalPath != null || remappedPath.equals(originalPath) && originalPathByRemappedPath.containsKey(remappedPath)) {
				logger.fatal(
					"File path clash",
					"Files '"
						+ (previousOriginalPath == null ? originalPathByRemappedPath.get(remappedPath) : previousOriginalPath)
						+ "' and '"
						+ originalPath
						+ "' both mapped to '"
						+ remappedPath
						+ "' after package name changes. Did you specify inconsistent input change logs?"
				);
			}
		}

		for (String remappedPathToAdd : originalPathByRemappedPath.keySet()) {
			this.resourceFilePaths.add(remappedPathToAdd);
		}
	}

	@Override
	public final boolean isOriginalSuperclassOf(String className, String target, ComplexAnnotationSpecifier annotationSpecifier) {
		try {
			boolean targetIsWildcard = target.indexOf(42) != -1;
			return this.checkSuperclassHierarchyOriginals(className, target, targetIsWildcard, annotationSpecifier);
		} catch (StackOverflowError stackOverflowError) {
			AbstractClassNode corruptHierarchyClassNode = this.classProvider.getClassNode(className, "reporting corrupt inheritance hierarchy");
			throw new ZKMUnexpectedException(stackOverflowError.getMessage() + " : Class " + corruptHierarchyClassNode.getSourcePath() + " has corrupt inheritance hierarchy (C)", stackOverflowError);
		}
	}

	private Set<String> collectUnmappedAnnotationTypes(String internalName, Integer innerClassOrdinal, Set<String> annotationTypes, boolean onlyInheritedAnnotations, String errorContext) {
		AbstractClassNode classNode = this.classProvider.getClassNode(internalName, innerClassOrdinal, errorContext);
		Enumeration<String> annotationTypeNameEnumeration = classNode.getAnnotationTypeNames();

		while (annotationTypeNameEnumeration.hasMoreElements()) {
			String annotationTypeName = annotationTypeNameEnumeration.nextElement();
			if (onlyInheritedAnnotations) {
				if (this.classProvider.getClassNode(annotationTypeName, errorContext).hasAnnotation("java/lang/annotation/Inherited")) {
					annotationTypes.add(annotationTypeName);
				}
			} else {
				annotationTypes.add(annotationTypeName);
			}
		}

		String superclassName = classNode.getSuperclassName();
		if (superclassName != null) {
			this.collectUnmappedAnnotationTypes(superclassName, innerClassOrdinal, annotationTypes, true, errorContext);
		}

		return annotationTypes;
	}

	@Override
	public String getBinaryName(String internalName) {
		return GlobalUtils.getMappedOrDefault(internalName, this.internalToBinaryNameMap);
	}

	private synchronized void renameFieldsWithClassNamePrefix(String originalClassName, String newClassName) {
		String oldArticleTypeName = FieldNode.toArticleTypeName(originalClassName);
		String newArticleTypeName = FieldNode.toArticleTypeName(newClassName);
		Enumeration<String> fieldNameEnumeration = this.fieldRegistryByName.keys();
		HashMap<ClassNode, ClassNode> seenClasses = GlobalUtils.newHashMap();
		DualKeyMap<String, String, String> renameMap = new DualKeyMap<>();

		while (fieldNameEnumeration.hasMoreElements()) {
			String fieldName = fieldNameEnumeration.nextElement();
			if (fieldName.startsWith(oldArticleTypeName)) {
				Enumeration<ClassNode> classNodeEnumeration = this.fieldRegistryByName.getDualKeyMap(fieldName).getGroupKeys();

				while (classNodeEnumeration.hasMoreElements()) {
					ClassNode classNode = classNodeEnumeration.nextElement();
					if (!seenClasses.containsKey(classNode)) {
						classNode.renameFieldsWithPrefix(oldArticleTypeName, newArticleTypeName, renameMap, this);
						seenClasses.put(classNode, classNode);
					}
				}
			}
		}

		for (int i = 0; i < this.workspaceClasses.length; i++) {
			this.workspaceClasses[i].refreshFieldRefsAndConstants(null, this);
		}
	}

	public List<ClassNode> loadClassResources(
		ClasspathResource[] classpathResources, List<ModuleInfoNode> modules, Thread thread, Runtime runtime, PrintWriter loadLogPrintWriter, PrintWriter warningLogPrintWriter, PrintWriter outputPrintWriter
	) throws IOException {
		ThreeLevelMultiMap<String, String, UnknownAttribute, Object> unknownAttributes = new ThreeLevelMultiMap<>(4, 3, 100, 5, Options.ZKM_USE_PARALLEL);
		Vector<ClassNode> classes = new Vector<>(classpathResources.length);
		Map<String, ClassNode> classesByName = Options.ZKM_USE_PARALLEL
			? new ConcurrentHashMap<>(GlobalUtils.getInitialCapacity(classpathResources.length))
			: GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(classpathResources.length));
		KeyValuePairMultiMap<String, ClassNode, Integer> multiReleaseClasses = new KeyValuePairMultiMap<>(Options.ZKM_USE_PARALLEL);
		KeyValuePairMultiMap<String, ModuleInfoNode, Integer> multiReleaseModules = new KeyValuePairMultiMap<>(Options.ZKM_USE_PARALLEL);
		Map<String, ModuleInfoNode> modulesByName = Options.ZKM_USE_PARALLEL ? new ConcurrentHashMap<>() : GlobalUtils.newHashMap();
		EventRegistry eventRegistry = new EventRegistry(Options.ZKM_USE_PARALLEL);
		List<ClasspathResource> resourceList = Arrays.asList(classpathResources);
		if (Options.ZKM_USE_PARALLEL && Options.threadCount >= 2) {
			Vector<Exception> exceptions = new Vector<>();
			resourceList.parallelStream().forEach(resource -> {
				try {
					this.loadSingleClassResource(resource, classes, modules, eventRegistry, classesByName, modulesByName, multiReleaseClasses, multiReleaseModules, unknownAttributes, loadLogPrintWriter, warningLogPrintWriter, outputPrintWriter, runtime);
				} catch (IOException | ProcessingException loadException) {
					exceptions.add(loadException);
				}
			});
			if (!exceptions.isEmpty()) {
				throw (ProcessingException)exceptions.get(0);
			}
		} else {
			for (ClasspathResource resource : resourceList) {
				this.loadSingleClassResource(resource, classes, modules, eventRegistry, classesByName, modulesByName, multiReleaseClasses, multiReleaseModules, unknownAttributes, loadLogPrintWriter, warningLogPrintWriter, outputPrintWriter, runtime);
			}
		}

		if (!multiReleaseClasses.isEmpty()) {
			this.processMultiReleaseClasses(classes, classesByName, multiReleaseClasses, loadLogPrintWriter, outputPrintWriter);
		}

		if (!multiReleaseModules.isEmpty()) {
			this.processMultiReleaseModules(modules, modulesByName, multiReleaseModules, loadLogPrintWriter, outputPrintWriter);
		}

		for (int i = 0; i < classpathResources.length; i++) {
			classpathResources[i].close();
		}

		this.resolveUnknownAttributes(unknownAttributes);
		return classes;
	}

	private boolean checkClassInstanceOfWithSnapshot(String className, String target, boolean targetIsWildcard) {
		if (className.equals("java/lang/Object")) {
			return false;
		} else if (target.equals("java/lang/Object")) {
			return true;
		} else if (className.equals(target)) {
			return false;
		} else {
			Boolean cachedInstanceOf = this.superClassHierarchyCache.get(target, className);
			if (cachedInstanceOf != null) {
				return cachedInstanceOf;
			} else {
				String superclassInternalName = this.getSuperclassInternalName(className);
				if (!targetIsWildcard ? !superclassInternalName.equals(target) : !StringUtils.matchesWildcard(superclassInternalName, target)) {
					if (superclassInternalName.equals("java/lang/Object")) {
						this.superClassHierarchyCache.put(target, className, BOOLEAN_FALSE);
						return false;
					} else {
						boolean instanceOfTarget = this.checkClassInstanceOfWithSnapshot(superclassInternalName, target, targetIsWildcard);
						this.superClassHierarchyCache.put(target, className, instanceOfTarget ? BOOLEAN_TRUE : BOOLEAN_FALSE);
						return instanceOfTarget;
					}
				} else {
					this.superClassHierarchyCache.put(target, className, BOOLEAN_TRUE);
					return true;
				}
			}
		}
	}

	public HashMap<JadFileProcessor, ArchiveNode> getJadFileToArchiveNodeMap() {
		return GlobalUtils.newHashMap(this.jadFileToArchiveNodeMap);
	}

	private Set<String> collectAnnotationTypes(String internalName, Integer innerClassOrdinal) {
		try {
			if (this.annotationReferenceGraph.hasMember(internalName)) {
				return new LinkedHashSet<>(this.annotationReferenceGraph.getReferences(internalName));
			} else {
				LinkedHashSet<String> annotationTypeNames = new LinkedHashSet<>(13);
				String errorContext = "analyzing the annotations of class '" + GlobalUtils.internalToBinaryName(internalName) + "'";
				this.collectUnmappedAnnotationTypes(internalName, innerClassOrdinal, annotationTypeNames, false, errorContext);
				this.annotationReferenceGraph.setReferences(internalName, annotationTypeNames);
				return annotationTypeNames;
			}
		} catch (StackOverflowError stackOverflowError) {
			AbstractClassNode corruptHierarchyClassNode = this.classProvider.getClassNode(internalName, "reporting corrupt inheritance hierarchy");
			throw new ZKMUnexpectedException(stackOverflowError.getMessage() + " : Class " + corruptHierarchyClassNode.getSourcePath() + " has corrupt hierarchy (C)", stackOverflowError);
		}
	}

	public void previewTrimAnalysis(
		Vector<ExclusionInclusionStatement> excludeStatements,
		Vector<ExclusionInclusionStatement> unexcludeStatements,
		PrintWriter printWriter,
		AbstractLogger logger,
		ZKMConsumer consumer,
		TrimConfig trimConfig,
		ScriptContext scriptContext
	) throws IOException {
		try {
			TrimExcludes trimExcludes = new TrimExcludes(
				this, this.classTreeManager, excludeStatements, unexcludeStatements, trimConfig, scriptContext, this.usageReferenceMap, (SerializedClassManager)null, (FixedClassManager)null
			);
			trimExcludes.computeTrimResults();
			if (trimExcludes.hasNoExcludedClasses()) {
				printWriter.println("All classes would be trimmed. You must change the trim exclusions to specify all application entry points.");
			} else if (trimExcludes.hasNoProcessableClasses() && trimExcludes.hasNoProcessableFields() && trimExcludes.hasNoProcessableMethods()) {
				printWriter.println("No classes, fields or methods would be trimmed.");
			} else {
				trimExcludes.printTrimReport(printWriter, this, true);
			}
		} finally {
			consumer.accept();
		}
	}

	private void loadSingleClassResource(
		ClasspathResource resource,
		List<ClassNode> classes,
		List<ModuleInfoNode> modules,
		EventRegistry eventRegistry,
		Map<String, ClassNode> classesByName,
		Map<String, ModuleInfoNode> modulesByName,
		KeyValuePairMultiMap<String, ClassNode, Integer> multiReleaseClasses,
		KeyValuePairMultiMap<String, ModuleInfoNode, Integer> multiReleaseModules,
		ThreeLevelMultiMap<String, String, UnknownAttribute, Object> unknownAttributes,
		PrintWriter loadLogPrintWriter,
		PrintWriter warningLogPrintWriter,
		PrintWriter outputPrintWriter,
		Runtime runtime
	) throws IOException {
		try {
			AtomicInt streamSize = new AtomicInt(0);
			InputStream inputStream = resource.getInputStream(streamSize);
			ByteDataBuffer byteDataBuffer = ByteDataBuffer.fromStream(inputStream, streamSize.get());
			int retryCount = 0;
			boolean loaded = false;
			long retryDelayMillis = 2000L;
			AtomicInt multiReleaseVersion = new AtomicInt();

			do {
				try {
					if (this.isModuleInfoClassResource(resource)) {
						boolean multiReleaseModule = false;
						if (resource.isZipEntry()) {
							multiReleaseModule = ManifestFile.isMultiReleaseVersionEntry(resource.getResourceName(), multiReleaseVersion);
						}

						ModuleInfoNode moduleNode = new ModuleInfoNode(byteDataBuffer, resource, this.status, eventRegistry, loadLogPrintWriter, warningLogPrintWriter, unknownAttributes);
						String moduleName = moduleNode.getModuleName();
						if (multiReleaseModule) {
							multiReleaseModules.put(moduleName, moduleNode, INTEGER_CACHE.valueOf(multiReleaseVersion.get()));
						} else {
							ModuleInfoNode duplicateModule = modulesByName.put(moduleName, moduleNode);
							if (duplicateModule != null) {
								modulesByName.put(moduleName, duplicateModule);
								ClasspathResource existingModuleSource = duplicateModule.getPrimaryClassSource();
								if (resource.isFileSystemResource() && existingModuleSource.isFileSystemResource()) {
									throw new ClassParseFailedException(
										"Module '" + moduleNode.getModuleName() + "' found more than once. File " + existingModuleSource.getResourceName() + " used. File " + resource.getResourceName() + " ignored."
									);
								}

								duplicateModule.addClassSource(resource);
								if (!Arrays.equals(moduleNode.getClassHash(), duplicateModule.getClassHash())) {
									throw new ClassParseFailedException(
										"Multiple versions of module '"
											+ moduleNode.getModuleName()
											+ "' detected. File "
											+ existingModuleSource.getFullPath()
											+ " used. File "
											+ resource.getFullPath()
											+ " ignored."
									);
								}
							} else {
								modules.add(moduleNode);
							}
						}

						if (!resource.isZipEntry()) {
							throw new ProcessingException("Module '" + moduleNode.getModuleName() + "' not in an archive. : '" + resource.getResourceName() + "'");
						}
					} else {
						boolean multiReleaseClass = false;
						if (resource.isZipEntry()) {
							multiReleaseClass = ManifestFile.isMultiReleaseVersionEntry(resource.getResourceName(), multiReleaseVersion);
						}

						ClassNode classNode = new ClassNode(byteDataBuffer, resource, this.status, eventRegistry, loadLogPrintWriter, warningLogPrintWriter, unknownAttributes);
						String internalName = classNode.getInternalName();
						if (multiReleaseClass) {
							multiReleaseClasses.put(internalName, classNode, INTEGER_CACHE.valueOf(multiReleaseVersion.get()));
						} else {
							ClassNode duplicateClass = classesByName.put(internalName, classNode);
							if (duplicateClass != null) {
								classesByName.put(internalName, duplicateClass);
								ClasspathResource existingClassSource = duplicateClass.getPrimaryClassSource();
								if (resource.isFileSystemResource() && existingClassSource.isFileSystemResource()) {
									throw new ClassParseFailedException(
										"Class "
											+ GlobalUtils.internalToBinaryName(internalName)
											+ " found more than once. File "
											+ existingClassSource.getResourceName()
											+ " used. File "
											+ resource.getResourceName()
											+ " ignored."
									);
								}

								duplicateClass.addClassSource(resource);
								if (!Arrays.equals(classNode.getClassHash(), duplicateClass.getClassHash())) {
									throw new ClassParseFailedException(
										"Multiple versions of class "
											+ GlobalUtils.internalToBinaryName(internalName)
											+ " detected. File "
											+ existingClassSource.getFullPath()
											+ " used. File "
											+ resource.getFullPath()
											+ " ignored."
									);
								}
							} else {
								classes.add(classNode);
							}
						}
					}

					loaded = true;
				} catch (OutOfMemoryError ignoredOutOfMemoryRetry) {
					retryCount++;
					runtime.gc();
					this.status.set("Waiting for garbage collection. Retry " + retryCount + ". " + (runtime.totalMemory() - runtime.freeMemory()) + "bytes used.");
					retryDelayMillis *= retryCount;

					try {
						Thread.sleep(retryDelayMillis);
					} catch (InterruptedException ignoredRetryInterrupt) {
					}

					byteDataBuffer.reset();
				}
			} while (!loaded && retryCount < 2);

			Thread.yield();
			if (!loaded) {
				this.status.set("Insufficient Memory");
				throw new ProcessingException("Insufficient Memory to open classes");
			}
		} catch (ClassParseFailedException classParseFailedException) {
			loadLogPrintWriter.println("ERROR: " + classParseFailedException.getMessage());
		} catch (IOException ioException) {
			throw new ProcessingException("IOException: " + ioException.getMessage());
		} catch (OutOfMemoryError ignoredOutOfMemoryFatal) {
			runtime.gc();
			this.status.set("Insufficient Memory (2)");

			try {
				Thread.sleep(2000L);
			} catch (InterruptedException ignoredFinalInterrupt) {
			}

			throw new ProcessingException("Insufficient Memory to open classes");
		}
	}

	@Override
	public final FieldNode getFieldBySignatureAndClass(ClassNode classNode, FieldSignature fieldSignature) {
		return this.fieldSignatureRegistry.get(fieldSignature, classNode);
	}

	public final int getWorkspaceClassesSize() {
		return this.workspaceClasses != null ? this.workspaceClasses.length : 0;
	}

	public ClasspathResource[] getAllClasspathResources() {
		ClasspathResource[] classpathResources = new ClasspathResource[this.xmlResources.length];
		System.arraycopy(this.xmlResources, 0, classpathResources, 0, this.xmlResources.length);
		return classpathResources;
	}

	private boolean checkInterfaceHierarchyWithSnapshot(String className, String target, boolean targetIsWildcard) {
		if (className.equals("java/lang/Object")) {
			return false;
		} else {
			Boolean cachedInstanceOf = this.interfaceHierarchySnapshot.get(target, className);
			if (cachedInstanceOf != null) {
				return cachedInstanceOf;
			} else {
				AtomicReference<AbstractClassNode> classNodeReference = new AtomicReference<>();
				Enumeration<String> interfaceNameEnumeration = this.getSnapshotInterfaceNames(className, classNodeReference);

				while (interfaceNameEnumeration.hasMoreElements()) {
					String interfaceName = interfaceNameEnumeration.nextElement();
					String hierarchyValidationError;
					if (Options.ZKM_TEST_HIERARCHY
						&& (
								hierarchyValidationError = ClassHierarchyNode.validateInheritanceLink(
									classNodeReference.get(), this.classProvider.getClassNode(GlobalUtils.getMappedOrDefault(interfaceName, this.classNameToInternalNameMap)), 2
								)
							)
							!= null) {
						throw new LookupException(hierarchyValidationError);
					}

					if (!targetIsWildcard ? interfaceName.equals(target) : StringUtils.matchesWildcard(interfaceName, target)) {
						this.interfaceHierarchySnapshot.put(target, className, BOOLEAN_TRUE);
						return true;
					}

					if (this.checkInterfaceHierarchyWithSnapshot(interfaceName, target, targetIsWildcard)) {
						this.interfaceHierarchySnapshot.put(target, className, BOOLEAN_TRUE);
						return true;
					}
				}

				String superclassName = this.getSuperclassName(className);
				boolean implementsTarget = this.checkInterfaceHierarchyWithSnapshot(superclassName, target, targetIsWildcard);
				this.interfaceHierarchySnapshot.put(target, className, implementsTarget ? BOOLEAN_TRUE : BOOLEAN_FALSE);
				return implementsTarget;
			}
		}
	}

	public Map<ClassNode, String> getClassNameToArchivePathMap() {
		return this.resourceReferencedClasses;
	}

	public boolean enableVerbose() {
		return this.verbose;
	}

	public final int getClassesCount() {
		int classCount = 0;
		if (this.workspaceClasses != null) {
			classCount = this.workspaceClasses.length;
		}

		if (this.moduleInfoNodes != null) {
			classCount += this.moduleInfoNodes.length;
		}

		return classCount;
	}

	@Override
	public final FieldNode[] getFieldsBySignature(FieldSignature fieldSignature) {
		Map<ClassNode, FieldNode> fieldNodesByClassNode = this.fieldSignatureRegistry.getGroup(fieldSignature);
		if (fieldNodesByClassNode == null) {
			return null;
		} else {
			FieldNode[] fields = new FieldNode[fieldNodesByClassNode.size()];
			int fieldIndex = 0;

			for (FieldNode fieldNode : fieldNodesByClassNode.values()) {
				fields[fieldIndex++] = fieldNode;
			}

			return fields;
		}
	}

	@Override
	public List<String> getOverrideFieldNames() {
		return this.overrideFieldNames;
	}

	@Override
	public List<String> getOverrideMethodNames() {
		return this.overrideMethodNames;
	}

	private void updateStringReferencesOnRename(
		Map<StringNode, ExcludeElement> excludeElements, StringNode stringNode, String currentValue, String newValue, StructureNode structureNode, HashMap<String, String> newToOriginalNameMap, ScriptContext scriptContext
	) {
		ExcludeElement existingExcludeElement = excludeElements.get(stringNode);
		if (existingExcludeElement != null) {
			String linkedClassName = (String)existingExcludeElement.getLinkedClass();
			String existingPrefix = (String)existingExcludeElement.getPrefix();
			StructureNode existingSuffixNode = (StructureNode)existingExcludeElement.getSuffix();
			if (!existingPrefix.equals(newValue)) {
				String existingSuffixType = this.getStructureNodeType(existingSuffixNode);
				String newSuffixType = this.getStructureNodeType(structureNode);
				String ownerClassName = GlobalUtils.internalToBinaryName(GlobalUtils.getMappedOrDefault(stringNode.getOwnerInternalName(), newToOriginalNameMap));
				String existingSuffixInternalName = GlobalUtils.getMappedOrDefault(existingSuffixNode.getInternalName(), newToOriginalNameMap);
				String newSuffixInternalName = GlobalUtils.getMappedOrDefault(structureNode.getInternalName(), newToOriginalNameMap);
				String existingSuffixClassName = GlobalUtils.internalToBinaryName(existingSuffixInternalName);
				String newSuffixClassName = GlobalUtils.internalToBinaryName(newSuffixInternalName);
				String existingSuffixSourcePath = existingSuffixNode.getRawSourcePath();
				String newSuffixSourcePath = structureNode.getRawSourcePath();
				StringBuilder errorMessageBuilder = new StringBuilder();
				if (existingSuffixClassName.equals(newSuffixClassName)) {
					if (existingSuffixType.equals(newSuffixType)) {
						errorMessageBuilder.append(
							"The one String '"
								+ linkedClassName
								+ "' in class '"
								+ ownerClassName
								+ "' is used to access two different "
								+ existingSuffixType
								+ "s which will be given different names. The "
								+ existingSuffixType
								+ " name '"
								+ linkedClassName
								+ "' must be excluded from obfuscation in the class '"
								+ existingSuffixClassName
								+ "' in file '"
								+ existingSuffixSourcePath
								+ "'. (a)"
						);
					} else {
						errorMessageBuilder.append(
							"The one String '"
								+ linkedClassName
								+ "' in class '"
								+ ownerClassName
								+ "' is used to access a "
								+ existingSuffixType
								+ " and a "
								+ newSuffixType
								+ " which will be given different names. The "
								+ existingSuffixType
								+ " and "
								+ newSuffixType
								+ " names '"
								+ linkedClassName
								+ "' must be excluded from obfuscation in class '"
								+ existingSuffixClassName
								+ "' in file '"
								+ existingSuffixSourcePath
								+ "'. (b)"
						);
					}
				} else if (existingSuffixType.equals(newSuffixType)) {
					errorMessageBuilder.append(
						"The one String '"
							+ linkedClassName
							+ "' in class '"
							+ ownerClassName
							+ "' is used to access two different "
							+ existingSuffixType
							+ "s which will be given different names. The "
							+ existingSuffixType
							+ " name '"
							+ linkedClassName
							+ "' must be excluded from obfuscation in the classes '"
							+ existingSuffixClassName
							+ "' and '"
							+ newSuffixClassName
							+ "' in files '"
							+ existingSuffixSourcePath
							+ "' and '"
							+ newSuffixSourcePath
							+ "'. (c)"
					);
				} else {
					errorMessageBuilder.append(
						"The one String '"
							+ linkedClassName
							+ "' in class '"
							+ ownerClassName
							+ "' is used to access a "
							+ existingSuffixType
							+ " and a "
							+ newSuffixType
							+ " which will be given different names. The "
							+ existingSuffixType
							+ " name in class '"
							+ existingSuffixClassName
							+ "' and "
							+ newSuffixType
							+ " name in class '"
							+ newSuffixClassName
							+ "' must be excluded from obfuscation.  Files names are '"
							+ existingSuffixSourcePath
							+ "' and '"
							+ newSuffixSourcePath
							+ "'. (d)"
					);
				}

				scriptContext.logSpecialError(errorMessageBuilder.toString());
			}

			excludeElements.put(stringNode, new ExcludeElement(linkedClassName, newValue, structureNode));
		} else {
			excludeElements.put(stringNode, new ExcludeElement(currentValue, newValue, structureNode));
		}
	}

	public static class ClassVersionComparator implements Comparator<KeyValuePair<ClassNode, Integer>> {
		public final Workspace workspace;

		public ClassVersionComparator(Workspace workspace) {
			this.workspace = workspace;
		}

		public int compareByValue(KeyValuePair<ClassNode, Integer> pair1, KeyValuePair<ClassNode, Integer> pair2) {
			return pair1.getValue() - pair2.getValue();
		}

		@Override
		public int compare(KeyValuePair<ClassNode, Integer> o1, KeyValuePair<ClassNode, Integer> o2) {
			return this.compareByValue(o1, o2);
		}
	}

	public static class SnapshotEnumerationAdapter<T> implements Enumeration<T> {
		private Enumeration<T> snapshot;

		public SnapshotEnumerationAdapter(Enumeration<T> source) {
			ArrayList<T> snapshotElements = new ArrayList<>();

			while (source.hasMoreElements()) {
				snapshotElements.add(source.nextElement());
			}

			this.snapshot = Collections.enumeration(snapshotElements);
		}

		@Override
		public final boolean hasMoreElements() {
			return this.snapshot.hasMoreElements();
		}

		@Override
		public final T nextElement() {
			return this.snapshot.nextElement();
		}
	}

	public static class ModuleVersionComparator implements Comparator<KeyValuePair<ModuleInfoNode, Integer>> {
		public final Workspace workspace;

		public ModuleVersionComparator(Workspace workspace) {
			this.workspace = workspace;
		}

		public int compare(KeyValuePair<ModuleInfoNode, Integer> o1, KeyValuePair<ModuleInfoNode, Integer> o2) {
			return o1.getValue() - o2.getValue();
		}
	}
}
