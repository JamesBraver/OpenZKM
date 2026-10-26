package com.zelix.tools.excludes;

import com.zelix.exceptions.ClasspathException;
import com.zelix.exceptions.FileException;
import com.zelix.exceptions.ProcessingException;
import com.zelix.framework.hierarchy.Inheritance;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.wrappers.FieldNode;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.statements.exclusion.ExistingSerializedClassesStatement;
import com.zelix.klassmaster.script.statements.specifier.RenameFilterParameter;
import com.zelix.tools.trim.TrimExcludes;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.SerialFieldBuilder;
import com.zelix.utils.enumerations.ArrayEnumeration;
import com.zelix.workspace.ClassHierarchyNode;
import com.zelix.workspace.Workspace;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

public class SerializedClassManager extends AbstractFixedClassManager {
	private final FixedClassManager fixedClassManager;

	public SerializedClassManager(Workspace workspace, List<ExistingSerializedClassesStatement> serializedClassesStatements, ScriptContext scriptContext, FixedClassManager fixedClassManager, Inheritance inheritance) {
		super(workspace, serializedClassesStatements, scriptContext);
		this.fixedClassManager = fixedClassManager;
		if (workspace.hasClasses()) {
			this.initExclusionMaps(workspace.getClasses(), workspace.getWorkspaceClassesSize());
			this.applySerializedClassParams();
			this.propagateClassExclusions();
			this.addSerialVersionUidFields(inheritance);
		}
	}

	private void propagateClassExclusions() {
		Enumeration<ClassNode> excludedClassEnumeration = this.getExcludedClasses();
		HashSet<ClassNode> visitedClasses = GlobalUtils.newHashSet();

		while (excludedClassEnumeration.hasMoreElements()) {
			ClassNode excludedClass = excludedClassEnumeration.nextElement();

			try {
				this.excludeClassRecursively(excludedClass, visitedClasses, excludedClass, true);
			} catch (ClasspathException classpathException) {
				super.scriptContext
					.logWarning(
						"Error while setting existing serialized classes exclusions : class '" + GlobalUtils.internalToBinaryName(classpathException.getClassName()) + "' not found."
					);
			} catch (FileException fileException) {
				super.scriptContext.logWarning("Error while setting existing serialized classes exclusions : " + fileException.getMessage());
			}
		}
	}

	public final void excludeSerializedClassFields(String internalName, ClassNode classNode, Set<ClassNode> visitedClasses, ClassNode excludedClass) {
		if (this.isSubclassOf(internalName, "java/io/Externalizable")) {
			this.excludeInstanceFields(classNode, visitedClasses, excludedClass);

			for (ClassHierarchyNode outerClassNode = ClassHierarchyNode.getNodeByInternalName(internalName).getOuterClass();
				outerClassNode != null && outerClassNode.isWorkspaceClass();
				outerClassNode = outerClassNode.getOuterClass()
			) {
				ClassNode outerClass = outerClassNode.getClassNodeIfWorkspace();
				this.excludeInstanceFields(outerClass, visitedClasses, excludedClass);
			}
		} else {
			FieldNode serialPersistentFieldsField;
			if ((serialPersistentFieldsField = this.workspace.getFieldByNameAndDescriptor(internalName, "serialPersistentFields", "[Ljava/io/ObjectStreamField;")) != null && serialPersistentFieldsField.isStatic()) {
				if (super.processableFields.remove(serialPersistentFieldsField) != null) {
					super.excludedFields.put(serialPersistentFieldsField, classNode);
				}

				try {
					List<?> serialPersistentFields = classNode.getSerialPersistentFields(this.workspace);
					int fieldCount = serialPersistentFields.size();

					for (int i = 0; i < fieldCount; i++) {
						FieldNode field = (FieldNode)serialPersistentFields.get(i);
						this.excludeField(field, visitedClasses, excludedClass);
					}
				} catch (ProcessingException processingException) {
					super.scriptContext.logWarning("Error while analyzing 'serialPersistentFields' in class '" + internalName + "' : " + processingException.getMessage());
				}
			} else {
				this.excludeInstanceFields(classNode, visitedClasses, excludedClass);
			}
		}
	}

	private void addSerialVersionUidFields(Inheritance inheritance) {
		Enumeration<ClassNode> excludedClassEnumeration = this.getExcludedClasses();

		try {
			while (excludedClassEnumeration.hasMoreElements()) {
				ClassNode excludedClass = excludedClassEnumeration.nextElement();
				String internalName = excludedClass.getInternalName();
				if ((this.fixedClassManager == null || !this.fixedClassManager.isInFixedClasses(excludedClass))
					&& !this.workspace.isClassInstanceOf(internalName, "java/lang/Enum")
					&& !this.workspace.isClassInstanceOf(internalName, "java/lang/reflect/Proxy")) {
					FieldNode serialVersionUidField = this.workspace.getFieldByNameAndDescriptor(internalName, "serialVersionUID", "J");
					if (serialVersionUidField == null) {
						long serialVersionUid = SerialFieldBuilder.computeSerialVersionUid(excludedClass, this.workspace);
						serialVersionUidField = excludedClass.addSerialVersionUidField(serialVersionUid, inheritance, this.workspace, 7);
					}

					if (super.processableFields.remove(serialVersionUidField) != null) {
						super.excludedFields.put(serialVersionUidField, excludedClass);
					}

					super.excludedFields.put(serialVersionUidField, excludedClass);
				}
			}
		} catch (ProcessingException processingException) {
			super.scriptContext.logFatalError("Could not process existing serialized classes : '" + processingException.getMessage() + "'");
		}
	}

	public final List<FieldNode> excludeInstanceFields(ClassNode classNode, Set<ClassNode> visitedClasses, ClassNode excludedClass) {
		ArrayList<FieldNode> serialVersionUidFields = new ArrayList<>();
		ArrayEnumeration<FieldNode> fieldEnumeration = classNode.getFieldEnumeration();

		while (fieldEnumeration.hasMoreElements()) {
			FieldNode field = fieldEnumeration.nextElement();
			if (!field.isStatic() && !field.isTransient()) {
				this.excludeField(field, visitedClasses, excludedClass);
			}

			if (!field.isPrivate() || !field.isStatic() && !field.isTransient()) {
				serialVersionUidFields.add(field);
			}
		}

		return serialVersionUidFields;
	}

	public final void initExclusionMaps(Enumeration<ClassNode> classes, int classCount) {
		super.processableClasses = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(classCount));
		super.excludedClasses = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(classCount));
		super.processableFields = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(classCount * 5));
		super.excludedFields = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(classCount * 5));

		while (classes.hasMoreElements()) {
			ClassNode classNode = classes.nextElement();
			super.processableClasses.put(classNode, classNode);
			ArrayEnumeration<FieldNode> fieldEnumeration = classNode.getFieldEnumeration();

			while (fieldEnumeration.hasMoreElements()) {
				FieldNode field = fieldEnumeration.nextElement();
				super.processableFields.put(field, field.getOwnerClass());
			}
		}
	}

	private final void applySerializedClassParams() {
		int statementCount = super.exclusionStatements.size();
		HashMap<RenameFilterParameter, RenameFilterParameter> seenParameters = GlobalUtils.newHashMap();
		Vector<RenameFilterParameter> distinctParameters = new Vector<>();

		for (int statementIndex = 0; statementIndex < statementCount; statementIndex++) {
			Enumeration<RenameFilterParameter> filterParameterEnumeration = super.exclusionStatements.get(statementIndex).getRenameFilterParameters();

			while (filterParameterEnumeration.hasMoreElements()) {
				RenameFilterParameter filterParameter = filterParameterEnumeration.nextElement();
				if (!seenParameters.containsKey(filterParameter)) {
					seenParameters.put(filterParameter, filterParameter);
					distinctParameters.addElement(filterParameter);
				}
			}
		}

		if (super.scriptContext.isVerbose() && distinctParameters.size() > 0) {
			super.logWriter.println("\tExisting Serialized classes parameters:");

			for (int logIndex = distinctParameters.size() - 1; logIndex >= 0; logIndex--) {
				super.logWriter.println("\t\t" + distinctParameters.elementAt(logIndex));
			}
		}

		for (int parameterIndex = distinctParameters.size() - 1; parameterIndex >= 0; parameterIndex--) {
			RenameFilterParameter parameter = distinctParameters.elementAt(parameterIndex);
			if (!parameter.isClassTarget()) {
				super.scriptContext
					.logWarningInternal("Parameter '" + parameter + "' in 'existingSerializedClasses' statement is not a class exclusion parameter. It will be ignored.", true);
			} else if (parameter.isLinkClass()) {
				super.scriptContext
					.logWarningInternal("Parameter '" + parameter + "' in 'existingSerializedClasses' statement cannot use the '<link>' syntax. It will be ignored.", true);
			} else {
				if (parameter.isClassWildcard()) {
					super.scriptContext.logWarningInternal("Parameter '" + parameter + "' in 'existingSerializedClasses' cannot use the '+' tag. The tag will be ignored.", true);
				}

				parameter.excludeSerializedClass(this);
			}
		}
	}

	public final void applyTrimExclusions(TrimExcludes trimExcludes, PrintWriter logWriter) {
		logWriter.println("\tApplying trim exclusions specified by existingSerializedClasses statement");
		Enumeration<ClassNode> excludedClassEnumeration = this.getExcludedClasses();

		while (excludedClassEnumeration.hasMoreElements()) {
			ClassNode excludedClass = excludedClassEnumeration.nextElement();
			ClassNode serializedClass = super.excludedClasses.get(excludedClass);
			trimExcludes.excludeClass(excludedClass, "Existing serialized class '" + this.formatClassForDisplay(serializedClass) + "'");
		}

		Enumeration<FieldNode> excludedFieldEnumeration = this.getExcludedFields();

		while (excludedFieldEnumeration.hasMoreElements()) {
			FieldNode excludedField = excludedFieldEnumeration.nextElement();
			ClassNode serializedFieldClass = super.excludedFields.get(excludedField);
			trimExcludes.excludeField(excludedField, "Existing serialized class '" + this.formatClassForDisplay(serializedFieldClass) + "'");
		}
	}

	public void applyDefaultExclusions(DefaultExcludes defaultExcludes, PrintWriter logWriter) {
		logWriter.println("\tApplying exclusions specified by existingSerializedClasses statement");
		Enumeration<ClassNode> excludedClassEnumeration = this.getExcludedClasses();

		while (excludedClassEnumeration.hasMoreElements()) {
			ClassNode excludedClass = excludedClassEnumeration.nextElement();
			ClassNode serializedClass = super.excludedClasses.get(excludedClass);
			String packageName = excludedClass.getPackageName();
			if (packageName != null && packageName.length() > 0) {
				defaultExcludes.excludePackage(packageName, "Existing serialized class '" + this.formatClassForDisplay(serializedClass) + "'");
			}

			defaultExcludes.excludeClass(excludedClass, "Existing serialized class '" + this.formatClassForDisplay(serializedClass) + "'");
		}

		Enumeration<FieldNode> excludedFieldEnumeration = this.getExcludedFields();

		while (excludedFieldEnumeration.hasMoreElements()) {
			FieldNode excludedField = excludedFieldEnumeration.nextElement();
			ClassNode serializedFieldClass = super.excludedFields.get(excludedField);
			defaultExcludes.excludeField(excludedField, "Existing serialized class '" + this.formatClassForDisplay(serializedFieldClass) + "'");
		}
	}

	public final void excludeField(FieldNode field, Set<ClassNode> visitedClasses, ClassNode excludedClass) {
		if (super.processableFields.remove(field) != null) {
			super.excludedFields.put(field, excludedClass);
			ClassNode fieldTypeClass = field.getFieldTypeClass();
			if (fieldTypeClass != null) {
				this.excludeClassRecursively(fieldTypeClass, visitedClasses, excludedClass, true);
			}
		}
	}

	public final void excludeClassRecursively(ClassNode classNode, Set<ClassNode> visitedClasses, ClassNode excludedClass, boolean includeSubclasses) {
		if (classNode != null && !visitedClasses.contains(classNode)) {
			visitedClasses.add(classNode);
			String internalName = classNode.getInternalName();
			if (!classNode.isInterface()
				&& (this.isSubclassOf(classNode.getInternalName(), "java/io/Serializable") || this.isSubclassOf(classNode.getInternalName(), "java/io/Externalizable"))) {
				this.removeProcessableClass(classNode, excludedClass);
				this.excludeSerializedClassFields(internalName, classNode, visitedClasses, excludedClass);
			}

			ClassHierarchyNode classHierarchyNode = ClassHierarchyNode.getNodeByInternalName(internalName);
			ClassHierarchyNode outerClassNode = classHierarchyNode.getOuterClass();
			if (outerClassNode != null && outerClassNode.isWorkspaceClass()) {
				ClassNode outerClass = outerClassNode.getClassNodeIfWorkspace();
				this.excludeClassRecursively(outerClass, visitedClasses, excludedClass, false);
			}

			if (includeSubclasses) {
				Enumeration<ClassHierarchyNode> subclassesEnumeration = classHierarchyNode.getSubclassesEnumeration();
				if (subclassesEnumeration != null) {
					while (subclassesEnumeration.hasMoreElements()) {
						ClassNode subclassNode = subclassesEnumeration.nextElement().getClassNodeIfWorkspace();
						this.excludeClassRecursively(subclassNode, visitedClasses, excludedClass, true);
					}
				}

				Enumeration<ClassHierarchyNode> implementersEnumeration = classHierarchyNode.getImplementersEnumeration();
				if (implementersEnumeration != null) {
					while (implementersEnumeration.hasMoreElements()) {
						ClassNode implementerNode = implementersEnumeration.nextElement().getClassNodeIfWorkspace();
						this.excludeClassRecursively(implementerNode, visitedClasses, excludedClass, true);
					}
				}
			}
		}
	}
}
