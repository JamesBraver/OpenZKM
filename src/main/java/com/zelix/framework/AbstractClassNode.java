package com.zelix.framework;

import com.zelix.exceptions.ClassParseFailedException;
import com.zelix.exceptions.FileException;
import com.zelix.exceptions.LookupException;
import com.zelix.framework.annotation.AnnotationNode;
import com.zelix.framework.attributes.base.AbstractAnnotationsAttributeNode;
import com.zelix.framework.attributes.debug.SourceFileAttribute;
import com.zelix.framework.attributes.declaration.EnclosingMethodAttribute;
import com.zelix.framework.attributes.declaration.InnerClassNode;
import com.zelix.framework.attributes.declaration.InnerClassesAttribute;
import com.zelix.framework.attributes.declaration.NestHostAttribute;
import com.zelix.framework.constant.StringNode;
import com.zelix.framework.hierarchy.ClassHierarchy;
import com.zelix.framework.listener.ReferenceUpdateListener;
import com.zelix.framework.signature.FieldSignature;
import com.zelix.framework.signature.MethodParameterKey;
import com.zelix.framework.signature.MethodSignature;
import com.zelix.framework.tree.ClasspathResource;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.writer.ClassInfoWriter;
import com.zelix.framework.writer.base.AbstractClassInfoWriter;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.klassmaster.config.Options;
import com.zelix.klassmaster.constant.AntiStringerConstants;
import com.zelix.klassmaster.constant.CopyrightConstants;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.logging.AbstractLogger;
import com.zelix.repository.ClassProvider;
import com.zelix.repository.Hierarchy;
import com.zelix.tools.parameterChanger.ParameterManager;
import com.zelix.tools.renamer.MethodRenamer;
import com.zelix.utils.*;
import com.zelix.utils.collections.DualKeyMap;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.collections.OrderedSet;
import com.zelix.utils.dataset.AtomicInt;
import com.zelix.utils.dataset.ByteDataBuffer;
import com.zelix.utils.enumerations.EmptyEnumeration;
import com.zelix.utils.references.AtomicReference;
import com.zelix.workspace.AbstractWorkspace;
import com.zelix.workspace.ClassHierarchyNode;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;

public abstract class AbstractClassNode extends StructureNode implements Types, Comparable<AbstractClassNode> {
	private List<ClasspathResource> classSources = Options.ZKM_USE_PARALLEL ? new Vector<>(1) : new ArrayList<>(1);
	public boolean hasValidInnerClasses = false;
	public boolean hasInvalidInnerClasses = false;
	public boolean isSealed = false;
	private boolean isLocalOrAnonymous = false;
	public boolean fromFilesystem;
	private final byte[] classHash;
	public final int generationTypeFlag;
	public String className;
	public int constantPoolSize;
	public boolean usedInFxml;
	public AttributesNode[] attributes;
	public int minorVersion;
	public int methodCount;
	public AbstractClassInfoWriter[] interfaceWriters;
	public int attributesCount;
	public AbstractClassNode multiReleaseBaseNode;
	public String internalName;
	private InnerClassNode innerClassNode;
	public int interfacesCount;
	public List<AbstractClassNode> multiReleaseVariants;
	public boolean isFxmlFqnTag;
	public AccessNode access;
	public AbstractClassInfoWriter thisClassWriter;
	public int majorVersion;
	public int fieldsCount;
	public AbstractClassInfoWriter superClassWriter;
	public Integer multiReleaseVersion;
	public String lowerCaseNameKey;

	public AbstractClassNode(ClasspathResource classpathResource, byte[] classHash, int classType) {
		super(null);
		this.classSources.add(classpathResource);
		this.fromFilesystem = !classpathResource.isFileSystemResource();
		this.classHash = classHash;
		this.generationTypeFlag = classType;
	}

	public static String internalToBinaryName(String internalName) {
		return internalName.replace('/', '.');
	}

	public static boolean isJavaLangInvokeHandle(String internalName) {
		return internalName.equals("java/lang/invoke/MethodHandle") || internalName.equals("java/lang/invoke/VarHandle");
	}

	public static String getPackageName(String internalName) {
		return getPrefixBeforeLast(internalName, '/');
	}

	public static String getArrayElementInternalName(String descriptor, AtomicInt arrayDepth) {
		int elementTypeStart = descriptor.lastIndexOf("[") + 1;
		if (arrayDepth != null) {
			arrayDepth.set(elementTypeStart);
		}

		String elementDescriptor = descriptor.substring(elementTypeStart);
		if (elementTypeStart > 0 && elementDescriptor.length() == 1) {
			return null;
		} else if (elementDescriptor.startsWith("L") && elementDescriptor.endsWith(";")) {
			return binaryToInternalName(elementDescriptor.substring(1, elementDescriptor.length() - 1));
		} else {
			return elementDescriptor.length() > 1 ? binaryToInternalName(elementDescriptor) : null;
		}
	}

	public static String getSimpleNameFromInnerClass(String innerClassInternalName) {
		int dollarIndex = innerClassInternalName.lastIndexOf("$");
		String simpleName;
		if (dollarIndex == innerClassInternalName.length() - 1) {
			simpleName = innerClassInternalName;
		} else if (dollarIndex > -1) {
			simpleName = innerClassInternalName.substring(dollarIndex + 1);
		} else {
			simpleName = innerClassInternalName;
		}

		return simpleName;
	}

	public static String getSimpleNameFromInternalName(String internalName) {
		int slashIndex = internalName.lastIndexOf("/");
		String simpleName;
		if (slashIndex > -1) {
			simpleName = internalName.substring(slashIndex + 1);
		} else {
			simpleName = internalName;
		}

		return simpleName;
	}

	public static String getTypeName(String descriptor) {
		int elementTypeStart = descriptor.lastIndexOf("[") + 1;
		String elementDescriptor = descriptor.substring(elementTypeStart);
		if (elementTypeStart > 0 && elementDescriptor.length() == 1) {
			return null;
		} else {
			String elementInternalName;
			if (elementDescriptor.startsWith("L") && elementDescriptor.endsWith(";")) {
				elementInternalName = elementDescriptor.substring(1, elementDescriptor.length() - 1);
			} else {
				elementInternalName = elementDescriptor;
			}

			return binaryToInternalName(elementInternalName);
		}
	}

	public static String createClassDescriptor(String internalName, int arrayDepth) {
		String classDescriptor = binaryToInternalName(internalName);
		classDescriptor = "L" + classDescriptor + ";";
		if (arrayDepth > 0) {
			classDescriptor = StringUtils.justificateString(classDescriptor, 82, classDescriptor.length() + arrayDepth, 91);
		}

		return classDescriptor;
	}

	public static String getArrayElementInternalName(String descriptor) {
		return getArrayElementInternalName(descriptor, null);
	}

	public static String binaryToInternalName(String binaryName) {
		return binaryName.replace('.', '/');
	}

	public static String getPrefixBeforeLast(String name, char separator) {
		int separatorIndex = name.lastIndexOf(separator);
		return separatorIndex == -1 ? "" : name.substring(0, separatorIndex);
	}

	public void addInnerClass(AbstractClassNode innerClass, Integer innerClassOrdinal) {
		if (this.multiReleaseVariants == null) {
			this.multiReleaseVariants = new ArrayList<>();
		}

		innerClass.multiReleaseBaseNode = this;
		innerClass.setInnerClassOrdinal(innerClassOrdinal);
		this.multiReleaseVariants.add(innerClass);
		if (this.multiReleaseVariants.size() > 1) {
			Collections.sort(this.multiReleaseVariants, (first, second) -> first.multiReleaseVersion - second.multiReleaseVersion);
		}
	}

	public boolean isInnerClass() {
		return this.multiReleaseBaseNode != null;
	}

	public abstract AbstractMethodNode[] getMethods();

	@Override
	public final String getBinaryName() {
		return this.internalName.replace('/', '.');
	}

	public void deriveInnerClassSimpleNames() {
		if (this.hasValidInnerClasses) {
			for (int i = 0; i < this.attributesCount; i++) {
				if (this.attributes[i] instanceof InnerClassesAttribute) {
					((InnerClassesAttribute)this.attributes[i]).deriveSimpleNames();
				}
			}
		}
	}

	public abstract void refreshFieldRefsAndConstants(ScriptContext scriptContext, AbstractWorkspace workspace);

	public Enumeration<ClasspathResource> getClassSources() {
		return Collections.enumeration(this.classSources);
	}

	public final boolean isFinal() {
		return this.access.isFinal();
	}

	public void setHasInvalidInnerClasses(boolean hasInvalid) {
		this.hasValidInnerClasses = !hasInvalid;
		this.hasInvalidInnerClasses = hasInvalid;
	}

	public final void validateOverrideParamChanges(ClassHierarchyNode classHierarchyNode, ParameterManager parameterManager) {
		AbstractMethodNode[] methods = this.getMethods();

		for (int i = 0; i < this.methodCount; i++) {
			parameterManager.validateOverrideMapping(classHierarchyNode, this, methods[i]);
		}
	}

	public String getNestHostClassName() {
		for (int i = 0; i < this.attributesCount; i++) {
			if (this.attributes[i] instanceof NestHostAttribute) {
				NestHostAttribute nestHostAttribute = (NestHostAttribute)this.attributes[i];
				if (nestHostAttribute.isParsedSuccessfully) {
					return nestHostAttribute.getHostClassInternalName();
				}
			}
		}

		return null;
	}

	public final boolean isAnnotation() {
		return this.access.isAnnotation();
	}

	public AbstractClassNode findInnerClassByOrdinal(Integer innerClassOrdinal) {
		AbstractClassNode closestInnerClass = this;
		if (this.multiReleaseVariants != null) {
			for (AbstractClassNode innerClass : this.multiReleaseVariants) {
				if (innerClass.multiReleaseVersion <= innerClassOrdinal) {
					closestInnerClass = innerClass;
				}
			}
		}

		return closestInnerClass;
	}

	public int getMajorVersion() {
		return this.majorVersion;
	}

	public void setHasValidInnerClasses(boolean hasValid) {
		this.hasValidInnerClasses = hasValid;
		this.hasInvalidInnerClasses = !hasValid;
	}

	public boolean isPublic() {
		return this.access.isPublic();
	}

	public boolean isFxmlFqnTag() {
		return this.isFxmlFqnTag;
	}

	public final String getDescriptor() {
		return 'L' + this.internalName + ';';
	}

	public boolean hasFxmlUsageFlag() {
		return this.usedInFxml;
	}

	public int getInnerClassAccessFlags() {
		return this.innerClassNode != null ? this.innerClassNode.getAccessFlags() : -1;
	}

	@Override
	public String getClassNameBinary() {
		return internalToBinaryName(this.className);
	}

	public boolean hasInnerClassOrdinal() {
		return this.multiReleaseVersion != null;
	}

	public ArrayList<String> getSuperclassHierarchy(ClassProvider classProvider, Integer innerClassOrdinal) {
		ArrayList<String> superclassNames = new ArrayList<>();
		HashSet<String> seenClassNames = null;
		String superclassName = this.getSuperclassName();
		int iterationCount = 0;
		AbstractClassNode currentClass = this;
		AbstractClassNode previousClass = null;
		Integer currentInnerClassOrdinal = innerClassOrdinal;

		while (superclassName != null) {
			superclassNames.add(superclassName);
			String contextMarker = "looking for the superclass of '" + currentClass.getRawSourcePath() + "'";
			currentClass = classProvider.getClassNode(superclassName, currentInnerClassOrdinal, contextMarker);
			String inheritanceError;
			if (Options.ZKM_TEST_HIERARCHY && previousClass != null && (inheritanceError = ClassHierarchyNode.validateInheritanceLink(previousClass, currentClass, 1)) != null) {
				throw new LookupException(inheritanceError);
			}

			previousClass = currentClass;
			superclassName = currentClass.getSuperclassName();
			currentInnerClassOrdinal = currentClass.hasInnerClassOrdinal() ? currentClass.getInnerClassOrdinal() : innerClassOrdinal;
			if (iterationCount++ > 1000) {
				if (seenClassNames == null) {
					seenClassNames = GlobalUtils.newHashSet();
				}

				if (!seenClassNames.add(superclassName)) {
					throw new LookupException(
						"Class '" + currentClass.getRawSourcePath() + "' appears more than once in inheritance hierarchy of class '" + this.getRawSourcePath() + "'"
					);
				}
			}
		}

		return superclassNames;
	}

	public List<AbstractMethodNode> findMethodsByName(String methodName) {
		ArrayList<AbstractMethodNode> matchingMethods = new ArrayList<>();
		AbstractMethodNode[] methods = this.getMethods();

		for (int i = 0; i < methods.length; i++) {
			if (methods[i].getName().equals(methodName)) {
				matchingMethods.add(methods[i]);
			}
		}

		return matchingMethods;
	}

	private String findOuterClassFromSynthetic(String internalName) {
		int dollarIndex = internalName.lastIndexOf("$");
		if (dollarIndex > -1 && dollarIndex < internalName.length() - 1 && IntegerUtils.isInteger(internalName.substring(dollarIndex + 1))) {
			internalName = internalName.substring(0, dollarIndex);
			if (ClassHierarchyNode.getNodeByInternalName(internalName) != null) {
				return internalName;
			}
		}

		return null;
	}

	public void clearInnerClassInfo() {
		this.innerClassNode = null;
		this.isLocalOrAnonymous = false;
	}

	public final boolean isAbstract() {
		return this.access.isAbstract();
	}

	public final void validateRenameOverrides(ClassHierarchyNode classHierarchyNode, MethodRenamer methodRenamer) {
		AbstractMethodNode[] methods = this.getMethods();

		for (int i = 0; i < this.methodCount; i++) {
			methodRenamer.validateRenameOverride(classHierarchyNode, this, methods[i]);
		}
	}

	public boolean isStaticInnerClass() {
		return this.innerClassNode != null && this.innerClassNode.isStatic();
	}

	public int getClassType() {
		return this.generationTypeFlag;
	}

	@Override
	public abstract TypeWriter getConstantPoolEntry(int index);

	public String getEnclosingClassName() {
		if (this.innerClassNode != null) {
			String enclosingClassName = this.innerClassNode.getEnclosingClassName();
			if (enclosingClassName != null) {
				if (enclosingClassName.equals(this.getInternalName())) {
					return null;
				}

				return enclosingClassName;
			}

			String outerClassName = this.getOuterClassName();
			if (outerClassName != null) {
				String ownInternalName = this.getInternalName();
				if (ownInternalName.startsWith(outerClassName)) {
					enclosingClassName = outerClassName;
				} else {
					enclosingClassName = ownInternalName;

					while (enclosingClassName != null && !outerClassName.startsWith(enclosingClassName)) {
						int dollarIndex = enclosingClassName.lastIndexOf("$");
						if (dollarIndex > 0) {
							enclosingClassName = enclosingClassName.substring(0, dollarIndex);
						} else {
							enclosingClassName = null;
						}
					}
				}

				return enclosingClassName;
			}
		}

		return null;
	}

	public void setSealed(boolean isSealed) {
		this.isSealed = isSealed;
	}

	public boolean isSealed() {
		return this.isSealed;
	}

	public String getSuperclassName() {
		return this.superClassWriter != null ? this.superClassWriter.getInternalName() : null;
	}

	public boolean isJava8OrNewer() {
		return EnvironmentUtils.isJava8OrNewer(this.majorVersion);
	}

	public final boolean isSuper() {
		return this.access.isSuper();
	}

	@Override
	public final boolean isPrimitive() {
		return false;
	}

	public boolean hasValidInnerClasses() {
		return this.hasValidInnerClasses;
	}

	public final String[] getInterfaceNames() {
		String[] internalNames;
		if (this.interfaceWriters == null) {
			internalNames = new String[0];
		} else {
			internalNames = new String[this.interfaceWriters.length];

			for (int i = 0; i < this.interfaceWriters.length; i++) {
				internalNames[i] = this.interfaceWriters[i].getInternalName();
			}
		}

		return internalNames;
	}

	public int getFieldsCount() {
		return this.fieldsCount;
	}

	@Override
	public String getClassName() {
		return this.className;
	}

	public Integer getInnerClassOrdinal() {
		return this.multiReleaseVersion;
	}

	public final void collectClassMetrics(ClassMetrics classMetrics) {
		classMetrics.incrementClassCount();
		classMetrics.addPackageName(this.getPackageName());
		AbstractMethodNode[] methods = this.getMethods();
		int methodsCount = methods.length;

		for (int i = 0; i < methodsCount; i++) {
			methods[i].updateCodeMetrics(classMetrics);
		}
	}

	public void setMajorVersion(int majorVersion) {
		this.majorVersion = majorVersion;
	}

	public int getInterfacesCount() {
		return this.interfacesCount;
	}

	public boolean isJava9OrNewer() {
		return EnvironmentUtils.isJava9OrNewer(this.majorVersion);
	}

	public final String getSimpleName() {
		int slashIndex = this.className.lastIndexOf("/");
		return slashIndex > 0 ? this.className.substring(slashIndex + 1) : this.className;
	}

	public boolean isFromFilesystem() {
		return this.fromFilesystem;
	}

	@Override
	public AbstractClassNode getClassNode() {
		return this;
	}

	public boolean isJava7OrNewer() {
		return EnvironmentUtils.isJava7OrNewer(this.majorVersion);
	}

	public void setPublic() {
		this.access.setPublic();
	}

	public AbstractClassInfoWriter getThisClassWriter() {
		return this.thisClassWriter;
	}

	public final AbstractFieldNode[] findFieldsByName(String fieldName) {
		ArrayList<AbstractFieldNode> matchingFields = new ArrayList<>();
		AbstractFieldNode[] fields = this.getFields();

		for (int i = 0; i < fields.length; i++) {
			if (fields[i].getNameDisplay().equals(fieldName)) {
				matchingFields.add(fields[i]);
			}
		}

		AbstractFieldNode[] fieldsArray = new AbstractFieldNode[matchingFields.size()];
		return matchingFields.toArray(fieldsArray);
	}

	public final void setFinal(boolean isFinal) {
		this.access.setFinal(isFinal);
	}

	public boolean isJava5OrNewer() {
		return EnvironmentUtils.isJava5OrNewer(this.majorVersion);
	}

	public int getMinorVersion() {
		return this.minorVersion;
	}

	public boolean isJava19OrNewer() {
		return EnvironmentUtils.isJava19OrNewer(this.majorVersion);
	}

	public AbstractFieldNode findField(FieldSignature fieldSignature) {
		AbstractFieldNode[] fields = this.getFields();

		for (int i = 0; i < fields.length; i++) {
			if (fields[i].getFieldSignature().equals(fieldSignature)) {
				return fields[i];
			}
		}

		return null;
	}

	public void markAsEnclosedByMethod() {
		this.isLocalOrAnonymous = true;
	}

	public final void validateMethodRenames(ClassHierarchyNode classHierarchyNode, MethodRenamer methodRenamer) {
		AbstractMethodNode[] methods = this.getMethods();

		for (int i = 0; i < this.methodCount; i++) {
			AbstractMethodNode method = methods[i];
			if (!method.isClinit() && !method.isConstructor()) {
				methodRenamer.validateMethodRename(classHierarchyNode, this, method);
			}
		}
	}

	public void applyInnerClassFieldRenames(DualKeyMap<String, FieldSignature, FieldSignature> fieldRenames, ScriptContext scriptContext) {
		Map<FieldSignature, FieldSignature> classFieldRenames = fieldRenames.getGroup(this.getInternalName());
		if (classFieldRenames != null) {
			for (Entry<FieldSignature, FieldSignature> entry : classFieldRenames.entrySet()) {
				FieldSignature oldFieldSignature = entry.getKey();
				FieldSignature newFieldSignature = entry.getValue();
				if (!oldFieldSignature.equals(newFieldSignature)) {
					Iterator<AbstractClassNode> innerClassIterator = this.getInnerClasses().iterator();

					while (innerClassIterator.hasNext()) {
						AbstractFieldNode matchingField = innerClassIterator.next().findField(oldFieldSignature);
						if (matchingField != null) {
							matchingField.setName(newFieldSignature.getName());
						}
					}
				}
			}
		}
	}

	public boolean isJava6OrNewer() {
		return EnvironmentUtils.isJava6OrNewer(this.majorVersion);
	}

	@Override
	public String getSourceName() {
		return this.classSources.get(0).getResourceName();
	}

	public void setInnerClassOrdinal(Integer innerClassOrdinal) {
		this.multiReleaseVersion = innerClassOrdinal;
	}

	@Override
	public boolean isClassNode() {
		return true;
	}

	private OrderedSet<String> getAllInterfacesRecursive(HashSet<AbstractClassNode> visitedClasses, ClassProvider classProvider, Integer innerClassOrdinal) {
		OrderedSet<String> interfaceNames = new OrderedSet<>();

		for (int i = 0; i < this.interfacesCount; i++) {
			String interfaceName = this.interfaceWriters[i].getInternalName();
			interfaceNames.add(interfaceName);
		}

		int processedCount = 0;
		int frontierCount = interfaceNames.size();

		Integer currentInnerClassOrdinal;
		for (currentInnerClassOrdinal = innerClassOrdinal; processedCount < frontierCount; frontierCount = interfaceNames.size()) {
			for (int interfaceIndex = processedCount; interfaceIndex < frontierCount; interfaceIndex++) {
				String currentInterfaceName = interfaceNames.get(interfaceIndex);
				String interfaceLookupContext = "looking for an interface implemented by '" + this.getRawSourcePath() + "'";
				AbstractClassNode interfaceNode = classProvider.getClassNode(currentInterfaceName, currentInnerClassOrdinal, interfaceLookupContext);
				currentInnerClassOrdinal = interfaceNode.hasInnerClassOrdinal() ? interfaceNode.getInnerClassOrdinal() : innerClassOrdinal;
				String inheritanceError;
				if (Options.ZKM_TEST_HIERARCHY && (inheritanceError = ClassHierarchyNode.validateInheritanceLink(this, interfaceNode, 2)) != null) {
					throw new LookupException(inheritanceError);
				}

				if (visitedClasses.add(interfaceNode)) {
					String[] directInterfaceNames = interfaceNode.getInterfaceNames();

					for (int directInterfaceIndex = 0; directInterfaceIndex < directInterfaceNames.length; directInterfaceIndex++) {
						interfaceNames.add(directInterfaceNames[directInterfaceIndex]);
					}
				}
			}

			processedCount = frontierCount;
		}

		String superclassName = this.getSuperclassName();
		if (superclassName != null) {
			String superclassLookupContext = "looking for the superclass of '" + this.getRawSourcePath() + "'";
			AbstractClassNode superclassNode = classProvider.getClassNode(superclassName, currentInnerClassOrdinal, superclassLookupContext);
			if (superclassNode.hasInnerClassOrdinal()) {
				superclassNode.getInnerClassOrdinal();
			}

			String superclassInheritanceError;
			if (Options.ZKM_TEST_HIERARCHY && (superclassInheritanceError = ClassHierarchyNode.validateInheritanceLink(this, superclassNode, 1)) != null) {
				throw new LookupException(superclassInheritanceError);
			}

			if (visitedClasses.add(superclassNode)) {
				OrderedSet<String> inheritedInterfaceNames = superclassNode.getAllInterfacesRecursive(visitedClasses, classProvider, innerClassOrdinal);

				for (int inheritedInterfaceIndex = 0; inheritedInterfaceIndex < inheritedInterfaceNames.size(); inheritedInterfaceIndex++) {
					String inheritedInterfaceName = inheritedInterfaceNames.get(inheritedInterfaceIndex);
					interfaceNames.add(inheritedInterfaceName);
				}
			}
		}

		return interfaceNames;
	}

	public void resolveClassHierarchyTypes(int thisClassIndex, int superClassIndex, int[] interfaceIndexes, MultiMap<ClassInfoWriter, ReferenceUpdateListener> classReferences) {
		TypeWriter thisClassEntry = this.getConstantPoolEntry(thisClassIndex);
		if (!(thisClassEntry instanceof AbstractClassInfoWriter)) {
			throw new ClassParseFailedException(this.getRawSourcePath() + " : Invalid Constant Pool index (A)");
		} else {
			this.thisClassWriter = (AbstractClassInfoWriter)thisClassEntry;
			this.internalName = this.thisClassWriter.getInternalName();
			this.lowerCaseNameKey = this.internalName.toLowerCase();
			this.className = this.internalName;
			if (classReferences != null) {
				classReferences.put((ClassInfoWriter)this.thisClassWriter, (ClassNode)this);
			}

			if (!this.access.isModule()) {
				TypeWriter superClassEntry = this.getConstantPoolEntry(superClassIndex);
				if (!(superClassEntry instanceof AbstractClassInfoWriter)) {
					if (!this.getInternalName().equals("java/lang/Object")) {
						throw new ClassParseFailedException(this.getRawSourcePath() + " : Invalid Constant Pool index (B)");
					}
				} else {
					this.superClassWriter = (AbstractClassInfoWriter)superClassEntry;
					if (classReferences != null && this.superClassWriter instanceof ClassInfoWriter) {
						classReferences.put((ClassInfoWriter)this.superClassWriter, (ClassNode)this);
					}
				}
			}

			this.interfaceWriters = new AbstractClassInfoWriter[interfaceIndexes.length];

			for (int i = 0; i < this.interfacesCount; i++) {
				TypeWriter interfaceEntry = this.getConstantPoolEntry(interfaceIndexes[i]);
				if (!(interfaceEntry instanceof AbstractClassInfoWriter)) {
					throw new ClassParseFailedException(this.getRawSourcePath() + " : Invalid Constant Pool index (C)");
				}

				this.interfaceWriters[i] = (AbstractClassInfoWriter)interfaceEntry;
				if (classReferences != null && this.interfaceWriters[i] instanceof ClassInfoWriter) {
					classReferences.put((ClassInfoWriter)this.interfaceWriters[i], (ClassNode)this);
				}
			}
		}
	}

	public AbstractClassNode getOuterClass() {
		return this.multiReleaseBaseNode;
	}

	public final Enumeration<String> getAnnotationTypeNames() {
		Set<String> annotationTypeNames = this.getAnnotationTypeNameSet();
		return (Enumeration<String>)(annotationTypeNames == null ? new EmptyEnumeration<>() : Collections.enumeration(annotationTypeNames));
	}

	@Override
	public String getConstructorDisplayName() {
		String ownInternalName = this.getInternalName();
		int slashIndex = ownInternalName.lastIndexOf('/');
		return slashIndex == -1 ? ownInternalName : ownInternalName.substring(slashIndex + 1);
	}

	public void applyInnerClassMethodRenames(DualKeyMap<String, MethodSignature, MethodSignature> methodRenames, ScriptContext scriptContext) {
		Map<MethodSignature, MethodSignature> classMethodRenames = methodRenames.getGroup(this.getInternalName());
		if (classMethodRenames != null) {
			for (Entry<MethodSignature, MethodSignature> entry : classMethodRenames.entrySet()) {
				MethodSignature oldMethodSignature = entry.getKey();
				MethodSignature newMethodSignature = entry.getValue();
				if (!oldMethodSignature.equals(newMethodSignature)) {
					Iterator<AbstractClassNode> innerClassIterator = this.getInnerClasses().iterator();

					while (innerClassIterator.hasNext()) {
						AbstractMethodNode matchingMethod = innerClassIterator.next().findMethod(oldMethodSignature);
						if (matchingMethod != null) {
							matchingMethod.setName(newMethodSignature.getName());
						}
					}
				}
			}
		}
	}

	@Override
	public int compareTo(AbstractClassNode other) {
		return this.getInternalName().compareTo(other.getInternalName());
	}

	public boolean hasInnerClassInfo() {
		return this.hasInnerClasses() || this.hasInnerClassOrdinal();
	}

	public AbstractMethodNode findMethod(MethodSignature methodSignature) {
		AbstractMethodNode[] methods = this.getMethods();

		for (int i = 0; i < methods.length; i++) {
			if (methods[i].getMethodSignature().equals(methodSignature)) {
				return methods[i];
			}
		}

		return null;
	}

	public String getOuterClassName() {
		if (this.innerClassNode != null) {
			for (int i = 0; i < this.attributesCount; i++) {
				if (this.attributes[i] instanceof EnclosingMethodAttribute) {
					EnclosingMethodAttribute enclosingMethodAttribute = (EnclosingMethodAttribute)this.attributes[i];
					if (enclosingMethodAttribute.isParsedSuccessfully()) {
						return enclosingMethodAttribute.getEnclosingClassName();
					}
					break;
				}
			}

			String outerClassName = this.innerClassNode.getOuterClassName();
			if (outerClassName != null) {
				return outerClassName;
			}

			String ownInternalName = this.getInternalName();
			String innerClassSimpleName = this.innerClassNode.getSimpleName();
			if (innerClassSimpleName != null && innerClassSimpleName.trim().length() > 0 && ownInternalName.endsWith(innerClassSimpleName)) {
				String candidateOuterName = ownInternalName.substring(0, ownInternalName.length() - innerClassSimpleName.length());
				if (candidateOuterName.endsWith("$")) {
					candidateOuterName = candidateOuterName.substring(0, candidateOuterName.length() - 1);
				}

				if (ClassHierarchyNode.getNodeByInternalName(candidateOuterName) != null) {
					return candidateOuterName;
				}

				String syntheticOuterFromSimpleName = this.findOuterClassFromSynthetic(candidateOuterName);
				if (syntheticOuterFromSimpleName != null) {
					return syntheticOuterFromSimpleName;
				}
			}

			int dollarIndex = ownInternalName.lastIndexOf("$");
			if (dollarIndex > -1 && dollarIndex < ownInternalName.length() - 1) {
				outerClassName = ownInternalName.substring(0, dollarIndex);
				if (ClassHierarchyNode.getNodeByInternalName(outerClassName) != null) {
					return outerClassName;
				}

				String syntheticOuterFromPrefix = this.findOuterClassFromSynthetic(outerClassName);
				if (syntheticOuterFromPrefix != null) {
					return syntheticOuterFromPrefix;
				}
			}
		}

		return null;
	}

	public void flagAsUsedInFxml() {
		this.usedInFxml = true;
	}

	public String getPackageNameBinary() {
		String ownInternalName = this.getInternalName();
		int slashIndex = ownInternalName.lastIndexOf('/');
		return slashIndex == -1 ? "" : ownInternalName.substring(0, slashIndex).replace('/', '.');
	}

	public final String getSimpleNameInternal() {
		int slashIndex = this.internalName.lastIndexOf("/");
		return slashIndex > 0 ? this.internalName.substring(slashIndex + 1) : this.internalName;
	}

	private void resolveMethodOverride(Hierarchy hierarchy, AbstractClassNode implementingClass, MethodSignature methodSignature, AbstractClassNode declaringClass, String contextMarker) {
		if (implementingClass != declaringClass) {
			String implementingClassName = implementingClass.getInternalName();
			hierarchy.putMethodOwner(implementingClassName, methodSignature, declaringClass);
			AbstractClassNode currentInterfaceImpl = hierarchy.putInterfaceMethodImpl(implementingClassName, methodSignature, declaringClass);
			if (!ClassHierarchyNode.isMissingClass(declaringClass.getInternalName()) && declaringClass.isInterface()) {
				while (currentInterfaceImpl != null && currentInterfaceImpl != declaringClass && !ClassHierarchyNode.isMissingClass(currentInterfaceImpl.getInternalName()) && currentInterfaceImpl.isInterface()) {
					String currentInterfaceImplName = currentInterfaceImpl.getInternalName();
					hierarchy.putMethodOwner(currentInterfaceImplName, methodSignature, declaringClass);
					AbstractClassNode previousInterfaceImpl = currentInterfaceImpl;
					currentInterfaceImpl = hierarchy.putInterfaceMethodImpl(currentInterfaceImplName, methodSignature, declaringClass);
					DebugUtils.assertTrue(
						currentInterfaceImpl == null || currentInterfaceImpl != previousInterfaceImpl, new String[]{"Loop: " + implementingClass.getBinaryName() + " " + (currentInterfaceImpl != null ? currentInterfaceImpl.getBinaryName() : "null")}
					);
				}
			}
		}
	}

	public final boolean isInterface() {
		return this.access.isInterface();
	}

	public boolean hasInnerClassNode() {
		return this.innerClassNode != null;
	}

	public final AbstractMethodNode[] findMethods(MethodParameterKey methodParameterKey) {
		AbstractMethodNode[] methods = this.getMethods();
		ArrayList<AbstractMethodNode> matchingMethods = new ArrayList<>();

		for (int i = 0; i < methods.length; i++) {
			if (methods[i].getName().equals(methodParameterKey.getName()) && methods[i].getParameterDescriptor().equals(methodParameterKey.getParameterDescriptor())) {
				matchingMethods.add(methods[i]);
			}
		}

		if (matchingMethods.size() == 0) {
			return null;
		} else {
			AbstractMethodNode[] methodsArray = new AbstractMethodNode[matchingMethods.size()];
			return matchingMethods.toArray(methodsArray);
		}
	}

	public final void setAnnotation(boolean isAnnotation) {
		this.access.setAnnotation(isAnnotation);
	}

	public boolean hasInvalidInnerClasses() {
		return this.hasInvalidInnerClasses;
	}

	@Override
	public String getSourcePath() {
		String fullPath = this.classSources.get(0).getFullPath();
		String tempPath = TempFileManager.resolveTempPathInString(fullPath);
		if (!fullPath.equals(tempPath)) {
			StringBuilder pathBuffer = new StringBuilder();
			pathBuffer.append(tempPath);
			pathBuffer.append(" (");
			pathBuffer.append(fullPath);
			pathBuffer.append(")");
			return pathBuffer.toString();
		} else {
			return fullPath;
		}
	}

	@Override
	public String getRawSourcePath() {
		return this.classSources.get(0).getFullPath();
	}

	public String getInnerClassSimpleName() {
		return this.innerClassNode != null ? this.innerClassNode.getSimpleName() : null;
	}

	public void initializeInnerClassNode() {
		if (!this.isModule()) {
			for (int i = 0; i < this.attributesCount; i++) {
				if (this.attributes[i] instanceof InnerClassesAttribute) {
					InnerClassesAttribute innerClassesAttribute = (InnerClassesAttribute)this.attributes[i];
					if (innerClassesAttribute.isValid) {
						this.innerClassNode = innerClassesAttribute.findInnerClassEntry(this.thisClassWriter);
					}
					break;
				}
			}
		}
	}

	public final AbstractFieldNode findField(String fieldName, String fieldDescriptor) {
		AbstractFieldNode[] fields = this.getFields();

		for (int i = 0; i < fields.length; i++) {
			if (fields[i].getNameDisplay().equals(fieldName) && fields[i].getDescriptor().equals(fieldDescriptor)) {
				return fields[i];
			}
		}

		return null;
	}

	public String getLowerCaseNameKey() {
		return this.lowerCaseNameKey;
	}

	public final String getAccessFlagsString() {
		return this.access.getAccessFlagsString();
	}

	public abstract AbstractFieldNode[] getFields();

	public String getPackageNameInternal() {
		return getPackageName(this.className);
	}

	public final Map<MethodSignature, AbstractClassNode> buildMethodOverrideMap(Hierarchy hierarchy, AtomicReference<Boolean> integrityResult, ScriptContext scriptContext, Random random) {
		String ownInternalName = this.getInternalName();
		boolean integrityVerified = true;
		if (random.nextInt(23) == 0) {
			if (integrityResult.isNull()) {
				String tempLogFilePath = TempFileManager.findTempFileByOriginalName(scriptContext.getLogFilePath());
				BufferedReader logReader = null;

				try {
					if (tempLogFilePath != null) {
						File logFile = new File(tempLogFilePath);
						if (logFile.exists()) {
							logReader = new BufferedReader(new InputStreamReader(new FileInputStream(logFile), "UTF-8"));
							ArrayList<String> logLines = new ArrayList<>();
							boolean contactInfoFound = false;

							String logLine;
							while ((logLine = logReader.readLine()) != null && !contactInfoFound) {
								logLines.add(logLine);
								if (logLine.indexOf(CopyrightConstants.CONTACT_INFO) > -1) {
									contactInfoFound = true;
									if (GlobalUtils.longHashCodeAbs(logLines.get(logLines.size() - 3).trim()) != AntiStringerConstants.LICENSE_NAME_HASHCODE) {
										integrityResult.set(Boolean.FALSE);
									} else {
										integrityResult.set(Boolean.TRUE);
									}

									if (false) {
										String licenseDateLine = logLines.get(logLines.size() - 5).trim();
										String licenseDate = licenseDateLine.substring(3, 10);
										StringTokenizer licenseDateTokenizer = new StringTokenizer(licenseDate, ".");
										double elapsedMinutes = Integer.parseInt(licenseDateTokenizer.nextToken()) * 365.25;
										elapsedMinutes += Integer.parseInt(licenseDateTokenizer.nextToken()) * 30.4;
										elapsedMinutes += Integer.parseInt(licenseDateTokenizer.nextToken());
										elapsedMinutes *= 1440.0;
										if ((long)elapsedMinutes > 14117040L) {
											integrityResult.set(Boolean.FALSE);
										}
									}
								}
							}
						}
					}
				} catch (Exception ignored) {
				} finally {
					if (logReader != null) {
						try {
							logReader.close();
						} catch (IOException ignoredOnClose) {
						}
					}
				}
			}

			integrityVerified = !integrityResult.isNull() ? integrityResult.get() : true;
			if (!integrityVerified && scriptContext.getIndentationLevel() == 0) {
				scriptContext.incrementIndent();
			}
		}

		HashMap<MethodSignature, AbstractClassNode> methodOwners;
		if (!ownInternalName.equals("java/lang/Object") && !this.isModule() && integrityVerified) {
			String superclassName = this.getSuperclassName();
			String superclassLookupContext = "looking for superclass of '" + this.getRawSourcePath() + "'";
			methodOwners = GlobalUtils.newHashMap(hierarchy.getOrLoadClassMethodMap(superclassName, integrityResult, scriptContext, random, superclassLookupContext));
		} else {
			methodOwners = GlobalUtils.newHashMap();
		}

		Vector<Map<MethodSignature, AbstractClassNode>> interfaceMethodMaps = new Vector<>();

		for (int interfaceIndex = 0; interfaceIndex < this.interfacesCount; interfaceIndex++) {
			String interfaceName = this.getInterfaceName(interfaceIndex);
			String interfaceLookupContext = "looking implemented interface of '" + this.getRawSourcePath() + "'";
			Map<MethodSignature, AbstractClassNode> interfaceMethodMap = hierarchy.getOrLoadClassMethodMap(interfaceName, integrityResult, scriptContext, random, interfaceLookupContext);
			interfaceMethodMaps.add(interfaceMethodMap);
		}

		for (int interfaceMapIndex = 0; interfaceMapIndex < interfaceMethodMaps.size(); interfaceMapIndex++) {
			for (Entry<MethodSignature, AbstractClassNode> entry : interfaceMethodMaps.elementAt(interfaceMapIndex).entrySet()) {
				MethodSignature methodSignature = entry.getKey();
				AbstractClassNode ownerClass = entry.getValue();
				String ownerClassName = ownerClass.getInternalName();
				if (!ownerClassName.equals("java/lang/Object")) {
					AbstractClassNode resolvedImpl = hierarchy.resolveInterfaceMethodImpl(ownerClass, methodSignature);
					if (resolvedImpl != null && resolvedImpl != ownerClass) {
						ownerClass = resolvedImpl;
						ownerClassName = resolvedImpl.getInternalName();
					}

					if (ownerClass != this) {
						AbstractClassNode previousOwner = methodOwners.put(methodSignature, ownerClass);
						if (previousOwner != null && ownerClass != previousOwner) {
							ClassHierarchyNode ownerNode = ClassHierarchyNode.getNodeByInternalName(ownerClassName);
							ClassHierarchyNode previousOwnerNode = ClassHierarchyNode.getNodeByInternalName(previousOwner.getInternalName());
							if (previousOwnerNode != null && !previousOwnerNode.isMissing()) {
								if (ownerNode != null && !ownerNode.isMissing()) {
									this.resolveMethodOverride(hierarchy, previousOwner, methodSignature, ownerClass, "(4c)");
								} else {
									this.resolveMethodOverride(hierarchy, previousOwner, methodSignature, ownerClass, "(3)");
								}
							} else if (ownerNode != null && !ownerNode.isMissing()) {
								this.resolveMethodOverride(hierarchy, ownerClass, methodSignature, previousOwner, "(2)");
								methodOwners.put(methodSignature, previousOwner);
							}
						}
					}
				}
			}
		}

		HashMap<MethodSignature, AbstractClassNode> methodOwnersSnapshot = GlobalUtils.newHashMap(methodOwners);
		AbstractMethodNode[] methods = this.getMethods();

		for (int methodIndex = 0; methodIndex < this.methodCount; methodIndex++) {
			methods[methodIndex].registerVirtualMethodOwner(hierarchy, methodOwnersSnapshot, ClassHierarchyNode.isMissingClass(ownInternalName), this);
		}

		hierarchy.putAllInterfaceMethodImpls(ownInternalName, methodOwners);
		hierarchy.putAllMethodOwners(ownInternalName, methodOwnersSnapshot);
		return methodOwnersSnapshot;
	}

	public final void validateParamChangeRequests(ClassHierarchyNode classHierarchyNode, ParameterManager parameterManager) {
		AbstractMethodNode[] methods = this.getMethods();

		for (int i = 0; i < this.methodCount; i++) {
			AbstractMethodNode method = methods[i];
			if (!method.isClinit()) {
				parameterManager.validateSignatureChange(classHierarchyNode, this, method);
			}
		}
	}

	public boolean hasAnnotation(String annotationTypeName) {
		for (int i = 0; i < this.attributesCount; i++) {
			if (this.attributes[i] instanceof AbstractAnnotationsAttributeNode) {
				String[] annotationTypeNames = ((AbstractAnnotationsAttributeNode)this.attributes[i]).getAnnotationTypeNames();

				for (int annotationNameIndex = 0; annotationNameIndex < annotationTypeNames.length; annotationNameIndex++) {
					if (annotationTypeNames[annotationNameIndex].equals(annotationTypeName)) {
						return true;
					}
				}
			}
		}

		return false;
	}

	public String getAnnotationsAsString(ClassHierarchy classHierarchy) {
		StringBuilder textBuffer = new StringBuilder();

		Iterator<String> annotationTypeIterator;
		try {
			annotationTypeIterator = classHierarchy.getAnnotationTypes(this.getInternalName(), this.hasInnerClassOrdinal() ? this.getInnerClassOrdinal() : null, true).iterator();
		} catch (FileException ignored) {
			return this.getAnnotationsAsString();
		}

		while (annotationTypeIterator.hasNext()) {
			textBuffer.append('@');
			String annotationTypeName = annotationTypeIterator.next();
			textBuffer.append(internalToBinaryName(annotationTypeName));
			textBuffer.append(" ");
		}

		return textBuffer.toString();
	}

	public boolean isGeneratedClass() {
		return this.generationTypeFlag != 0;
	}

	public String getInterfaceName(int interfaceIndex) {
		return this.interfaceWriters[interfaceIndex].getInternalName();
	}

	public void readClassHeader(ByteDataBuffer dataBuffer) throws IOException {
		int magicNumber = dataBuffer.readInt();
		if (magicNumber != 0xCAFEBABE) {
			throw new ClassParseFailedException("'" + this.getRawSourcePath() + "' is not a valid class file : '" + Integer.toHexString(magicNumber) + "'");
		} else {
			this.minorVersion = dataBuffer.readUnsignedShort();
			this.majorVersion = dataBuffer.readUnsignedShort();
			if (this.majorVersion < 45 || this.majorVersion == 45 && this.minorVersion < 3) {
				throw new ClassParseFailedException("'" + this.getRawSourcePath() + "' is an incompatible version. : " + this.majorVersion + "." + this.minorVersion);
			}
		}
	}

	public int getClassSourceCount() {
		return this.classSources.size();
	}

	public void flagAsFxmlFqnTag() {
		this.isFxmlFqnTag = true;
	}

	public boolean isLocalOrAnonymous() {
		return this.isLocalOrAnonymous;
	}

	public abstract void remapReferences(
		int keepInnerClassInfo,
		int keepGenericsInformation,
		HashMap<String, String> classNameMappings,
		HashSet<StringNode> processedStringNodes,
		Set<String> resourceFilePaths,
		HashMap<String, String> packageMappings,
		AbstractLogger logger
	);

	public final int getAccessFlags() {
		return this.access.getAccessFlags();
	}

	@Override
	public String getPackageName() {
		return getPrefixBeforeLast(this.getInternalName(), '/');
	}

	public final Set<String> getAnnotationTypeNameSet() {
		HashSet<String> annotationTypeNames = null;

		for (int i = 0; i < this.attributesCount; i++) {
			if (this.attributes[i] instanceof AbstractAnnotationsAttributeNode) {
				if (annotationTypeNames == null) {
					annotationTypeNames = GlobalUtils.newHashSet(13);
				}

				String[] attributeAnnotationTypeNames = ((AbstractAnnotationsAttributeNode)this.attributes[i]).getAnnotationTypeNames();

				for (int nameIndex = 0; nameIndex < attributeAnnotationTypeNames.length; nameIndex++) {
					annotationTypeNames.add(attributeAnnotationTypeNames[nameIndex]);
				}
			}
		}

		return annotationTypeNames;
	}

	public boolean isNameChanged() {
		return !this.className.equals(this.internalName);
	}

	public void setMinorVersion(int minorVersion) {
		this.minorVersion = minorVersion;
	}

	public final boolean isModule() {
		return this.access.isModule();
	}

	public final void renameClassInternal(String newInternalName, HashMap<String, String> sourceFileNames) {
		this.getConstantPoolWriter().remapTargetClassName(newInternalName);
		this.internalName = newInternalName;
		this.lowerCaseNameKey = this.internalName.toLowerCase();
		if (!Options.keepOriginalSourceFileName) {
			for (int i = 0; i < this.attributesCount; i++) {
				if (this.attributes[i] instanceof SourceFileAttribute) {
					sourceFileNames.put(this.internalName, ((SourceFileAttribute)this.attributes[i]).sourceFileName.getValue());
					((SourceFileAttribute)this.attributes[i]).sourceFileName.setValue(this.getSimpleNameInternal() + ".java");
					break;
				}
			}
		}

		this.markChanged();
		this.notify(new AtomicInt(0), this, null);
		if (this.hasInnerClasses()) {
			Iterator<AbstractClassNode> innerClassIterator = this.getInnerClasses().iterator();

			while (innerClassIterator.hasNext()) {
				innerClassIterator.next().renameClassInternal(newInternalName, sourceFileNames);
			}
		}
	}

	public int getMethodCount() {
		return this.methodCount;
	}

	public abstract void resolveRefsAndSyncMetadata(ScriptContext scriptContext, AbstractWorkspace workspace, boolean pruneSignatureIfChanged);

	public boolean isJava1_4OrNewer() {
		return EnvironmentUtils.isJava1_4OrNewer(this.majorVersion);
	}

	public OrderedSet<String> getAllInterfaces(ClassProvider classProvider, Integer innerClassOrdinal) {
		HashSet<AbstractClassNode> visitedClasses = GlobalUtils.newHashSet();
		visitedClasses.add(this);
		return this.getAllInterfacesRecursive(visitedClasses, classProvider, innerClassOrdinal);
	}

	public void addClassSource(ClasspathResource classpathResource) {
		this.classSources.add(classpathResource);
		if (classpathResource.isFileSystemResource()) {
			this.fromFilesystem = false;
		}
	}

	public String getAnnotationsAsString() {
		StringBuffer textBuffer = new StringBuffer();
		Enumeration<String> annotationTypes = this.getAnnotationTypeNames();

		while (annotationTypes.hasMoreElements()) {
			textBuffer.append('@');
			String annotationTypeName = annotationTypes.nextElement();
			textBuffer.append(internalToBinaryName(annotationTypeName));
			textBuffer.append(" ");
		}

		return textBuffer.toString();
	}

	public abstract boolean isWorkspaceNode();

	public boolean hasInnerClasses() {
		return this.multiReleaseVariants != null && this.multiReleaseVariants.size() > 0;
	}

	@Override
	public final String getInternalName() {
		return this.internalName;
	}

	public byte[] getClassHash() {
		return (byte[])this.classHash.clone();
	}

	public List<AbstractClassNode> getSelfAndAllInnerClasses() {
		ArrayList<AbstractClassNode> selfAndInnerClasses;
		if (this.multiReleaseVariants != null) {
			selfAndInnerClasses = new ArrayList<>(this.multiReleaseVariants.size() + 1);
			selfAndInnerClasses.add(this);
			selfAndInnerClasses.addAll(this.multiReleaseVariants);
		} else if (this.multiReleaseBaseNode != null) {
			selfAndInnerClasses = new ArrayList<>(this.multiReleaseBaseNode.multiReleaseVariants.size() + 1);
			selfAndInnerClasses.add(this.multiReleaseBaseNode);
			selfAndInnerClasses.addAll(this.multiReleaseBaseNode.multiReleaseVariants);
		} else {
			selfAndInnerClasses = new ArrayList<>(1);
			selfAndInnerClasses.add(this);
		}

		return selfAndInnerClasses;
	}

	public Set<String> findAnnotationValue(String annotationTypeName, String annotationPropertyName, boolean useOriginalNames) {
		HashSet<String> literalValues = GlobalUtils.newHashSet(13);

		for (int i = 0; i < this.attributesCount; i++) {
			if (this.attributes[i] instanceof AbstractAnnotationsAttributeNode) {
				AnnotationNode annotation = ((AbstractAnnotationsAttributeNode)this.attributes[i]).getAnnotationByType(annotationTypeName, useOriginalNames);
				if (annotation != null) {
					String literalValue = annotation.findParamLiteral(annotationPropertyName, useOriginalNames);
					if (literalValue != null && !literalValue.startsWith("[")) {
						literalValues.add(literalValue);
					}
				}
			}
		}

		AbstractFieldNode[] fields = this.getFields();

		for (int fieldIndex = 0; fieldIndex < fields.length; fieldIndex++) {
			Set<String> fieldValues = fields[fieldIndex].findAnnotationValue(annotationTypeName, annotationPropertyName, useOriginalNames);
			literalValues.addAll(fieldValues);
		}

		AbstractMethodNode[] methods = this.getMethods();

		for (int methodIndex = 0; methodIndex < methods.length; methodIndex++) {
			Set<String> methodValues = methods[methodIndex].findAnnotationValue(annotationTypeName, annotationPropertyName, useOriginalNames);
			literalValues.addAll(methodValues);
		}

		return literalValues;
	}

	public ClasspathResource getPrimaryClassSource() {
		return this.classSources != null && this.classSources.size() != 0 ? this.classSources.get(0) : null;
	}

	public List<AbstractClassNode> getInnerClasses() {
		return this.multiReleaseVariants != null ? new ArrayList<>(this.multiReleaseVariants) : new ArrayList<>(0);
	}
}
