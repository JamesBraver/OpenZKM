package com.zelix.tools.excludes;

import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.wrappers.FieldNode;
import com.zelix.framework.wrappers.MethodNode;
import com.zelix.klassmaster.config.Options;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.statements.exclusion.FixedClassesStatement;
import com.zelix.klassmaster.script.statements.specifier.RenameFilterParameter;
import com.zelix.tools.exceptions.ObfuscateExceptionExclude;
import com.zelix.tools.flow.FlowObfuscateExclude;
import com.zelix.tools.integerEncryption.IntegerEncryptionExcludes;
import com.zelix.tools.longEncryption.LongEncryptionExcludes;
import com.zelix.tools.string.StringEncryptionExcludes;
import com.zelix.tools.trim.TrimExcludes;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.collections.DualKeyMap;
import com.zelix.utils.enumerations.ArrayEnumeration;
import com.zelix.workspace.ClassHierarchyNode;
import com.zelix.workspace.PackageManager;
import com.zelix.workspace.Workspace;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Map;

public class FixedClassManager extends AbstractFixedClassManager {
	private final HashMap<ClassNode, String> changeReasons = GlobalUtils.newHashMap();
	private final DualKeyMap<String, ClassNode, ClassNode> changedClassesByPackage;
	private final DualKeyMap<ClassNode, ClassNode, ClassNode> changedClassesByClass;
	private final DualKeyMap<FieldNode, ClassNode, ClassNode> changedClassesByField;
	private final DualKeyMap<MethodNode, ClassNode, ClassNode> changedClassesByMethod;

	public FixedClassManager(Workspace workspace, List<FixedClassesStatement> fixedClassesStatements, ScriptContext scriptContext) {
		super(workspace, fixedClassesStatements, scriptContext);
		int classCount = workspace.getWorkspaceClassesSize();
		this.changedClassesByPackage = new DualKeyMap<>();
		this.changedClassesByClass = new DualKeyMap<>(classCount, 13);
		this.changedClassesByField = new DualKeyMap<>(classCount * 5, 13);
		this.changedClassesByMethod = new DualKeyMap<>(classCount * 5, 13);
		if (Options.ZKM_FIXED_TOTALLY_UNCHANGED) {
			scriptContext.logMessage("Fixed classes will be saved completely unchanged because 'ZKM_FIXED_TOTALLY_UNCHANGED' set.");
		}

		if (workspace.getWorkspaceClassesSize() > 0) {
			this.initExclusionMaps(workspace.getClasses(), workspace.getWorkspaceClassesSize());
			this.applyFixedClassParameters();
			this.registerClassDependencies();
		}
	}

	public final void excludeMethod(MethodNode method, ClassNode excludedClass) {
		if (this.processableMethods.remove(method) != null) {
			this.excludedMethods.put(method, excludedClass);
			ArrayList<ClassNode> descriptorClasses = method.getDescriptorWorkspaceClasses();
			if (descriptorClasses != null) {
				for (int i = 0; i < descriptorClasses.size(); i++) {
					ClassNode descriptorClass = descriptorClasses.get(i);
					if (!this.isClassExcluded(descriptorClass)) {
						this.removeProcessableClass(descriptorClass, excludedClass);
					}
				}
			}
		}

		this.changedClassesByMethod.put(method, excludedClass, excludedClass);
	}

	public final void initExclusionMaps(Enumeration<ClassNode> classes, int classCount) {
		super.processableClasses = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(classCount));
		super.excludedClasses = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(classCount));
		super.processableFields = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(classCount * 5));
		super.excludedFields = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(classCount * 5));
		this.processableMethods = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(classCount * 5));
		this.excludedMethods = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(classCount * 5));

		while (classes.hasMoreElements()) {
			ClassNode classNode = classes.nextElement();
			super.processableClasses.put(classNode, classNode);
			ArrayEnumeration<FieldNode> fieldEnumeration = classNode.getFieldEnumeration();

			while (fieldEnumeration.hasMoreElements()) {
				FieldNode field = fieldEnumeration.nextElement();
				super.processableFields.put(field, field.getOwnerClass());
			}

			ArrayEnumeration<MethodNode> methodEnumeration = classNode.getMethodEnumeration();

			while (methodEnumeration.hasMoreElements()) {
				MethodNode method = methodEnumeration.nextElement();
				if (!method.isConstructor() && !method.isClinit()) {
					this.processableMethods.put(method, method.getMethodOwner());
				}
			}
		}
	}

	public final void applyTrimExclusions(TrimExcludes trimExcludes, PrintWriter logWriter) {
		logWriter.println("\tApplying trim exclusions specified by fixedClasses statement");
		Enumeration<ClassNode> fixedClassEnumeration = this.enumerateFixedClasses();

		while (fixedClassEnumeration.hasMoreElements()) {
			ClassNode fixedClass = fixedClassEnumeration.nextElement();
			String reason = "Fixed class '" + this.formatClassForDisplay(fixedClass) + "'";
			trimExcludes.excludeClass(fixedClass, reason);
			HashSet<ClassNode> classReferences = GlobalUtils.newHashSet();
			HashSet<ClassNode> signatureClassReferences = GlobalUtils.newHashSet();
			HashSet<FieldNode> fieldReferences = GlobalUtils.newHashSet();
			HashSet<MethodNode> methodReferences = GlobalUtils.newHashSet();
			fixedClass.collectAttributeReferences(classReferences, signatureClassReferences, fieldReferences, methodReferences);
			if (!classReferences.isEmpty()) {
				for (ClassNode classReference : classReferences) {
					trimExcludes.excludeClass(classReference, reason);
				}
			}

			if (!signatureClassReferences.isEmpty()) {
				for (ClassNode signatureClassReference : signatureClassReferences) {
					trimExcludes.excludeClass(signatureClassReference, reason);
				}
			}

			if (!fieldReferences.isEmpty()) {
				for (FieldNode fieldReference : fieldReferences) {
					trimExcludes.excludeField(fieldReference, reason);
				}
			}

			if (!methodReferences.isEmpty()) {
				for (MethodNode methodReference : methodReferences) {
					trimExcludes.excludeMethod(methodReference, reason);
				}
			}

			ArrayEnumeration<FieldNode> fieldEnumeration = fixedClass.getFieldEnumeration();

			while (fieldEnumeration.hasMoreElements()) {
				FieldNode field = fieldEnumeration.nextElement();
				trimExcludes.excludeField(field, reason);
			}

			ArrayEnumeration<MethodNode> methodEnumeration = fixedClass.getMethodEnumeration();

			while (methodEnumeration.hasMoreElements()) {
				MethodNode method = methodEnumeration.nextElement();
				trimExcludes.excludeMethod(method, reason);
			}
		}
	}

	private void registerClassDependency(ClassNode dependency, ClassNode excludedClass) {
		String packageName = dependency.getPackageName();
		if (packageName.length() > 0) {
			this.changedClassesByPackage.put(packageName, excludedClass, dependency);
			String[] packageAncestors = PackageManager.getPackageAncestors(packageName);

			for (int i = 0; i < packageAncestors.length; i++) {
				this.changedClassesByPackage.put(packageAncestors[i], excludedClass, dependency);
			}
		}

		this.changedClassesByClass.put(dependency, excludedClass, excludedClass);
	}

	public String getChangeReason(ClassNode classNode) {
		return this.changeReasons.get(classNode);
	}

	public void recordMethodChange(MethodNode method, String changeLogName, boolean renamed) {
		Map<ClassNode, ClassNode> changedClasses = this.changedClassesByMethod.getGroup(method);
		if (changedClasses != null) {
			for (ClassNode changedClass : changedClasses.keySet()) {
				if (!this.changeReasons.containsKey(changedClass)) {
					String changeDescription = renamed ? "given a new name" : "given changed parameters";
					this.changeReasons.put(changedClass, "Method '" + method.getDisplayName() + "' in class '" + method.getBinaryName() + "' " + changeDescription + " by the input change log '" + changeLogName + "'");
					super.scriptContext
						.logWarning(
							"Class '"
								+ changedClass.getBinaryName()
								+ "' was specified as 'fixed' but it has had to be changed because method '"
								+ method.getDisplayName()
								+ "' in class '"
								+ method.getBinaryName()
								+ "' "
								+ changeDescription
								+ " by the input change log '"
								+ changeLogName
								+ "'"
						);
				}
			}
		}
	}

	public boolean isChangedClass(ClassNode classNode) {
		return this.changeReasons.containsKey(classNode);
	}

	private void registerClassDependencies() {
		Enumeration<ClassNode> excludedClassEnumeration = this.getExcludedClasses();

		while (excludedClassEnumeration.hasMoreElements()) {
			ClassNode excludedClass = excludedClassEnumeration.nextElement();
			String superclassName = excludedClass.getSuperclassName();
			if (superclassName != null) {
				ClassNode superclassNode = ClassHierarchyNode.getWorkspaceClassNode(superclassName);
				if (superclassNode != null) {
					this.registerClassDependency(superclassNode, excludedClass);
					if (!this.isClassExcluded(superclassNode)) {
						this.removeProcessableClass(superclassNode, excludedClass);
					}
				}
			}

			String[] interfaceNames = excludedClass.getInterfaceNames();

			for (int interfaceIndex = 0; interfaceIndex < interfaceNames.length; interfaceIndex++) {
				ClassNode interfaceNode = ClassHierarchyNode.getWorkspaceClassNode(interfaceNames[interfaceIndex]);
				if (interfaceNode != null) {
					this.registerClassDependency(interfaceNode, excludedClass);
					if (!this.isClassExcluded(interfaceNode)) {
						this.removeProcessableClass(interfaceNode, excludedClass);
					}
				}
			}

			FieldNode[] fieldNodes = excludedClass.getFieldNodes();

			for (int fieldIndex = 0; fieldIndex < fieldNodes.length; fieldIndex++) {
				this.excludeField(fieldNodes[fieldIndex], excludedClass);
			}

			MethodNode[] methodNodes = excludedClass.getMethodNodes();

			for (int methodIndex = 0; methodIndex < methodNodes.length; methodIndex++) {
				this.excludeMethod(methodNodes[methodIndex], excludedClass);
			}

			HashSet<ClassNode> classReferences = GlobalUtils.newHashSet();
			HashSet<FieldNode> fieldReferences = GlobalUtils.newHashSet();
			HashSet<MethodNode> methodReferences = GlobalUtils.newHashSet();
			excludedClass.collectAllReferences(classReferences, fieldReferences, methodReferences);

			for (ClassNode classReference : classReferences) {
				if (classReference.isWorkspaceNode()) {
					this.registerClassDependency(classReference, excludedClass);
					if (!this.isClassExcluded(classReference)) {
						this.removeProcessableClass(classReference, excludedClass);
					}
				}
			}

			for (FieldNode fieldReference : fieldReferences) {
				if (!this.isFieldExcluded(fieldReference)) {
					this.excludeField(fieldReference, excludedClass);
				}
			}

			for (MethodNode methodReference : methodReferences) {
				if (!this.isMethodExcluded(methodReference)) {
					this.excludeMethod(methodReference, excludedClass);
				}
			}
		}
	}

	public void recordPackageRename(String packageName, String changeLogName) {
		if (this.hasAffectedClasses(packageName)) {
			Map<ClassNode, ClassNode> changedClasses = this.changedClassesByPackage.getGroup(packageName);
			if (changedClasses != null) {
				for (Entry<ClassNode, ClassNode> entry : changedClasses.entrySet()) {
					ClassNode changedClass = entry.getKey();
					ClassNode renamedClass = entry.getValue();
					if (!this.changeReasons.containsKey(changedClass)) {
						this.changeReasons.put(changedClass, "Class '" + renamedClass.getBinaryName() + "' has been given a new fully qualified name by the input change log '" + changeLogName + "'");
						super.scriptContext
							.logWarning(
								"Class '"
									+ changedClass.getBinaryName()
									+ "' was specified as 'fixed' but it has had to be changed because class '"
									+ renamedClass.getBinaryName()
									+ "' has been given a new fully qualified name by the input change log '"
									+ changeLogName
									+ "'"
							);
					}
				}
			}
		}
	}

	public void recordFieldRename(FieldNode field, String changeLogName) {
		Map<ClassNode, ClassNode> changedClasses = this.changedClassesByField.getGroup(field);
		if (changedClasses != null) {
			for (ClassNode changedClass : changedClasses.keySet()) {
				if (!this.changeReasons.containsKey(changedClass)) {
					this.changeReasons.put(changedClass, "Field '" + field.getDisplayName() + "' in class '" + field.getBinaryName() + "' given a new name by the input change log '" + changeLogName + "'");
					super.scriptContext
						.logWarning(
							"Class '"
								+ changedClass.getBinaryName()
								+ "' was specified as 'fixed' but it has had to be changed because field '"
								+ field.getDisplayName()
								+ "' in class '"
								+ field.getBinaryName()
								+ "' given a new name by the input change log '"
								+ changeLogName
								+ "'"
						);
				}
			}
		}
	}

	public boolean hasAffectedClasses(ClassNode classNode) {
		Map<ClassNode, ClassNode> changedClasses = this.changedClassesByClass.getGroup(classNode);
		return changedClasses != null && changedClasses.size() > 0;
	}

	private final void applyFixedClassParameters() {
		int exclusionStatementCount = super.exclusionStatements.size();
		HashMap<RenameFilterParameter, RenameFilterParameter> seenExclusionParameters = GlobalUtils.newHashMap();
		ArrayList<RenameFilterParameter> distinctExclusionParameters = new ArrayList<>();

		for (int exclusionStatementIndex = 0; exclusionStatementIndex < exclusionStatementCount; exclusionStatementIndex++) {
			Enumeration<RenameFilterParameter> exclusionParameterEnumeration = super.exclusionStatements.get(exclusionStatementIndex).getRenameFilterParameters();

			while (exclusionParameterEnumeration.hasMoreElements()) {
				RenameFilterParameter exclusionParameter = exclusionParameterEnumeration.nextElement();
				if (!seenExclusionParameters.containsKey(exclusionParameter)) {
					seenExclusionParameters.put(exclusionParameter, exclusionParameter);
					distinctExclusionParameters.add(exclusionParameter);
				}
			}
		}

		if (super.scriptContext.isVerbose() && distinctExclusionParameters.size() > 0) {
			super.logWriter.println("\tFixed classes parameters:");

			for (int exclusionDisplayIndex = distinctExclusionParameters.size() - 1; exclusionDisplayIndex >= 0; exclusionDisplayIndex--) {
				super.logWriter.println("\t\t" + distinctExclusionParameters.get(exclusionDisplayIndex));
			}
		}

		for (int exclusionApplyIndex = distinctExclusionParameters.size() - 1; exclusionApplyIndex >= 0; exclusionApplyIndex--) {
			RenameFilterParameter exclusionParameterToApply = distinctExclusionParameters.get(exclusionApplyIndex);
			if (!exclusionParameterToApply.isClassTarget()) {
				super.scriptContext
					.logWarningInternal("Parameter '" + exclusionParameterToApply + "' in 'fixedClasses' statement is not a class exclusion parameter. It will be ignored.", true);
			} else if (exclusionParameterToApply.isLinkClass()) {
				super.scriptContext.logWarningInternal("Parameter '" + exclusionParameterToApply + "' in 'fixedClasses' statement cannot use the '<link>' syntax. It will be ignored.", true);
			} else {
				if (exclusionParameterToApply.isClassWildcard()) {
					super.scriptContext.logWarningInternal("Parameter '" + exclusionParameterToApply + "' in 'fixedClasses' statement cannot use the '+' tag. The tag will be ignored.", true);
				}

				exclusionParameterToApply.applyFixedClassExclusion(this);
			}
		}
	}

	public final void excludeField(FieldNode field, ClassNode excludedClass) {
		if (super.processableFields.remove(field) != null) {
			super.excludedFields.put(field, excludedClass);
			ClassNode fieldTypeClass = field.getFieldTypeClass();
			if (fieldTypeClass != null && !this.isClassExcluded(fieldTypeClass)) {
				this.removeProcessableClass(fieldTypeClass, excludedClass);
			}
		}

		this.changedClassesByField.put(field, excludedClass, excludedClass);
	}

	public boolean hasAffectedClasses(String packageName) {
		Map<ClassNode, ClassNode> changedClasses = this.changedClassesByPackage.getGroup(packageName);
		return changedClasses != null && changedClasses.size() > 0;
	}

	public void recordClassRename(ClassNode classNode, String changeLogName) {
		String binaryName = classNode.getBinaryName();
		if (this.isInFixedClasses(classNode) && !this.changeReasons.containsKey(classNode)) {
			this.changeReasons.put(classNode, "'" + binaryName + "' given a new name by the input change log '" + changeLogName + "'");
			super.scriptContext.logWarning("Class '" + binaryName + "' was specified as 'fixed' but it has been given a new name by the input change log '" + changeLogName + "'");
		}

		if (this.hasAffectedClasses(classNode)) {
			Map<ClassNode, ClassNode> changedClasses = this.changedClassesByClass.getGroup(classNode);
			if (changedClasses != null) {
				for (ClassNode changedClass : changedClasses.keySet()) {
					if (!this.changeReasons.containsKey(changedClass)) {
						this.changeReasons.put(changedClass, "Class '" + binaryName + "' given a new name by the input change log '" + changeLogName + "'");
						super.scriptContext
							.logWarning(
								"Class '"
									+ changedClass.getBinaryName()
									+ "' was specified as 'fixed' but it has had to be changed because class '"
									+ classNode.getBinaryName()
									+ "' given a new name by the input change log '"
									+ changeLogName
									+ "'"
							);
					}
				}
			}
		}
	}

	public void applyFixedClassExclusions(
		DefaultExcludes defaultExcludes,
		FlowObfuscateExclude flowObfuscateExclude,
		ObfuscateExceptionExclude obfuscateExceptionExclude,
		StringEncryptionExcludes stringEncryptionTargets,
		IntegerEncryptionExcludes integerEncryptionExcludes,
		LongEncryptionExcludes longEncryptionExclude,
		PrintWriter logWriter
	) {
		logWriter.println("\tApplying exclusions specified by fixedClasses statement");
		Enumeration<ClassNode> excludedClassEnumeration = this.getExcludedClasses();

		while (excludedClassEnumeration.hasMoreElements()) {
			ClassNode excludedClass = excludedClassEnumeration.nextElement();
			ClassNode fixedClass = super.excludedClasses.get(excludedClass);
			String classReason = "Fixed class '" + this.formatClassForDisplay(fixedClass) + "'";
			String packageName = excludedClass.getPackageName();
			if (packageName != null && packageName.length() > 0) {
				defaultExcludes.excludePackage(packageName, classReason);
			}

			defaultExcludes.excludeClass(excludedClass, classReason);
		}

		Enumeration<FieldNode> excludedFieldEnumeration = this.getExcludedFields();

		while (excludedFieldEnumeration.hasMoreElements()) {
			FieldNode excludedField = excludedFieldEnumeration.nextElement();
			ClassNode fieldFixedClass = super.excludedFields.get(excludedField);
			String fieldReason = "Fixed class '" + this.formatClassForDisplay(fieldFixedClass) + "'";
			defaultExcludes.excludeField(excludedField, fieldReason);
		}

		Enumeration<MethodNode> excludedMethodEnumeration = this.getExcludedMethods();

		while (excludedMethodEnumeration.hasMoreElements()) {
			MethodNode excludedMethod = excludedMethodEnumeration.nextElement();
			ClassNode methodFixedClass = this.excludedMethods.get(excludedMethod);
			String methodReason = "Fixed class '" + this.formatClassForDisplay(methodFixedClass) + "'";
			defaultExcludes.excludeMethod(excludedMethod, methodReason);
		}

		Enumeration<ClassNode> fixedClassEnumeration = this.enumerateFixedClasses();

		while (fixedClassEnumeration.hasMoreElements()) {
			ClassNode fixedClassToExclude = fixedClassEnumeration.nextElement();
			String fixedClassReason = "Fixed class '" + this.formatClassForDisplay(fixedClassToExclude) + "'";
			if (flowObfuscateExclude != null) {
				flowObfuscateExclude.excludeClassAndMethods(fixedClassToExclude, fixedClassReason, true);
			}

			if (obfuscateExceptionExclude != null) {
				obfuscateExceptionExclude.excludeClassAndMethods(fixedClassToExclude, fixedClassReason, true);
			}

			if (stringEncryptionTargets != null) {
				stringEncryptionTargets.excludeClass(fixedClassToExclude, fixedClassReason);
			}

			if (integerEncryptionExcludes != null) {
				integerEncryptionExcludes.excludeClass(fixedClassToExclude, fixedClassReason);
			}

			if (longEncryptionExclude != null) {
				longEncryptionExclude.excludeClass(fixedClassToExclude, fixedClassReason);
			}
		}
	}
}
