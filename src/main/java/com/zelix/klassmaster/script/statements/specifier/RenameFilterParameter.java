package com.zelix.klassmaster.script.statements.specifier;

import com.zelix.exceptions.ClasspathException;
import com.zelix.exceptions.FileException;
import com.zelix.framework.AbstractClassNode;
import com.zelix.framework.AbstractFieldNode;
import com.zelix.framework.AbstractMethodNode;
import com.zelix.framework.AccessFlags;
import com.zelix.framework.code.InstructionListNode;
import com.zelix.framework.hierarchy.ClassHierarchy;
import com.zelix.framework.hierarchy.GroupingsHierarchy;
import com.zelix.framework.modules.ModuleInfoNode;
import com.zelix.framework.tree.ClasspathResource;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.wrappers.FieldNode;
import com.zelix.framework.wrappers.MethodNode;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.klassmaster.config.Options;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.base.AbstractScriptNode;
import com.zelix.klassmaster.script.base.ScriptNode;
import com.zelix.klassmaster.script.statements.annotation.ComplexAnnotationSpecifier;
import com.zelix.klassmaster.script.statements.clause.ContainedInClause;
import com.zelix.klassmaster.script.statements.clause.ContainingClause;
import com.zelix.klassmaster.script.statements.javaccInterface.*;
import com.zelix.klassmaster.script.statements.signature.MethodArgumentSpecifier;
import com.zelix.tools.autoReflection.AutoReflectionExcludes;
import com.zelix.tools.exceptions.ObfuscateExceptionExclude;
import com.zelix.tools.excludes.*;
import com.zelix.tools.excludes.base.AbstractClassExcludeManager;
import com.zelix.tools.flow.FlowObfuscateExclude;
import com.zelix.tools.integerEncryption.IntegerEncryptionExcludes;
import com.zelix.tools.longEncryption.LongEncryptionExcludes;
import com.zelix.tools.parameterChanger.ParameterChangerExcludes;
import com.zelix.tools.references.ObfuscateReferenceExcludes;
import com.zelix.tools.removeCall.RemoveMethodCallExcludes;
import com.zelix.tools.string.StringEncryptionExcludes;
import com.zelix.tools.trim.TrimExcludes;
import com.zelix.utils.DebugUtils;
import com.zelix.utils.FileUtils;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.StringUtils;
import com.zelix.utils.enumerations.ArrayEnumeration;
import com.zelix.utils.references.AtomicReference;
import com.zelix.workspace.ClassHierarchyNode;

import java.io.IOException;
import java.util.*;

public class RenameFilterParameter extends AbstractScriptNode implements FieldSpecifierAcceptor, ThrowsExceptionNameAcceptor, Comparable<RenameFilterParameter> {
	public static final char DOT_CHAR = ".".charAt(0);
	public static final char CARET_CHAR = "^".charAt(0);
	public Map<String, String> classAccessFlagStrings = GlobalUtils.newHashMap(19);
	public Map<String, String> memberAccessFlagStrings = GlobalUtils.newHashMap(19);
	public ArrayList<String> searchPaths = new ArrayList<>();
	public List<ComplexAnnotationSpecifier> implementsAnnotationSpecifiers = new ArrayList<>();
	public List<String> implementsInterfaceNames = new ArrayList<>();
	public List<String> throwsExceptionNames = new ArrayList<>();
	private boolean isEnabled = true;
	public ComplexAnnotationSpecifier[] parameterAnnotationSpecifiers;
	private int priority;
	public ComplexMethodSpecifier methodSpecifier;
	public String searchByAnnotationName;
	public ContainingClause containingClause;
	public String linkClassNamePrefix;
	public ComplexAnnotationSpecifier memberAnnotationSpecifier;
	private String primaryVisibilityModifier;
	private ScriptLocation scriptCommandNode;
	public ComplexModuleSpecifier moduleSpecifier;
	public Specifier packageSpecifier;
	public String searchByAnnotationProperty;
	public String sourceFilePattern;
	public PatternMatcher classSpecifier;
	public ContainedInClause containedInClause;
	public String linkClassNamePattern;
	public String extendsClassName;
	public String fieldTypeDescriptor;
	public int targetType;
	public String linkMethodNamePattern;
	public AccessFlags classAccessFlags;
	public boolean isAnnotationType;
	private String ruleString;
	public MethodArgumentSpecifier methodArgumentSpecifier;
	public AccessFlags memberAccessFlags;
	public ComplexAnnotationSpecifier classAnnotationSpecifier;
	public ComplexAnnotationSpecifier extendsAnnotationSpecifier;
	public String linkMethodNamePrefix;
	public boolean includeSignatureClasses;
	public String linkClassNameSuffix;
	public ComplexFieldSpecifier fieldSpecifier;
	public ScriptContext scriptContext;

	public RenameFilterParameter(int nodeType) {
		super(nodeType);
	}

	public static boolean matchesAccessFlags(int accessFlags, AccessFlags filterFlags) {
		return filterFlags == null ? true : filterFlags.matchesAccessFlags(accessFlags);
	}

	public static String descriptorToHumanReadable(String descriptor) {
		return !descriptor.equals("*") && !descriptor.equals("?") ? TypeWriter.descriptorToHumanReadable(descriptor).replace('/', '.') : descriptor;
	}

	public static String buildFieldAnnotationString(ComplexAnnotationSpecifier annotationSpecifier, AccessFlags accessFlags, String descriptor, NamePattern fieldNameSpecifier) {
		return buildSignatureString(annotationSpecifier, accessFlags, descriptor, fieldNameSpecifier, null, null, null, null, null, false);
	}

	public static boolean isWildcardStar(String name) {
		return name.equals("*");
	}

	public static String buildMethodAnnotationString(
		ComplexAnnotationSpecifier annotationSpecifier, AccessFlags accessFlags, NamePattern methodNameSpecifier, MethodArgumentSpecifier methodArgumentSpecifier, ComplexAnnotationSpecifier[] parameterAnnotations, List<String> throwsExceptionNames
	) {
		return buildSignatureString(annotationSpecifier, accessFlags, null, null, null, methodNameSpecifier, methodArgumentSpecifier, parameterAnnotations, throwsExceptionNames, false);
	}

	public static AccessFlags parseAccessFlagsFromMap(Map<String, String> accessFlagStrings, int targetType) {
		AccessFlags accessFlags = null;
		Iterator<String> accessFlagIterator = accessFlagStrings.keySet().iterator();

		while (accessFlagIterator.hasNext()) {
			if (accessFlags == null) {
				byte accessFlagsTargetType;
				if (targetType == 3) {
					accessFlagsTargetType = 2;
				} else if (targetType == 4) {
					accessFlagsTargetType = 3;
				} else {
					accessFlagsTargetType = 0;
				}

				accessFlags = new AccessFlags(accessFlagsTargetType);
			}

			boolean negated = false;
			String modifierName = accessFlagIterator.next();
			if (modifierName.startsWith(AbstractScriptNode.NEGATED_MODIFIER_MARK)) {
				modifierName = modifierName.substring(1);
				negated = true;
			}

			if (negated) {
				if (modifierName.equals("public")) {
					accessFlags.markNegativePublic();
				} else if (modifierName.equals("protected")) {
					accessFlags.markNegativeProtected();
				} else if (modifierName.equals("private")) {
					accessFlags.markNegativePrivate();
				} else if (modifierName.equals("package")) {
					accessFlags.markNegativePackagePrivate();
				} else if (modifierName.equals("native")) {
					accessFlags.markNegativeNative();
				} else if (modifierName.equals("static")) {
					accessFlags.markNegativeStatic();
				} else if (modifierName.equals("final")) {
					accessFlags.markNegativeFinal();
				} else if (modifierName.equals("synchronized")) {
					accessFlags.markNegativeSynchronized();
				} else if (modifierName.equals("volatile")) {
					accessFlags.markNegativeVolatile();
				} else if (modifierName.equals("transient")) {
					accessFlags.markNegativeTransient();
				} else if (modifierName.equals("abstract")) {
					accessFlags.markNegativeAbstract();
				} else if (modifierName.equals("synthetic")) {
					accessFlags.markNegativeSynthetic();
				} else if (modifierName.equals("enum")) {
					accessFlags.markNegativeEnum();
				} else if (modifierName.equals("bridge")) {
					accessFlags.markNegativeBridge();
				}
			} else if (modifierName.equals("public")) {
				accessFlags.markPositivePublic();
			} else if (modifierName.equals("protected")) {
				accessFlags.markPositiveProtected();
			} else if (modifierName.equals("private")) {
				accessFlags.markPositivePrivate();
			} else if (modifierName.equals("package")) {
				accessFlags.markPositivePackagePrivate();
			} else if (modifierName.equals("native")) {
				accessFlags.markPositiveNative();
			} else if (modifierName.equals("static")) {
				accessFlags.markPositiveStatic();
			} else if (modifierName.equals("final")) {
				accessFlags.markPositiveFinal();
			} else if (modifierName.equals("synchronized")) {
				accessFlags.markPositiveSynchronized();
			} else if (modifierName.equals("volatile")) {
				accessFlags.markPositiveVolatile();
			} else if (modifierName.equals("transient")) {
				accessFlags.markPositiveTransient();
			} else if (modifierName.equals("abstract")) {
				accessFlags.markPositiveAbstract();
			} else if (modifierName.equals("synthetic")) {
				accessFlags.markPositiveSynthetic();
			} else if (modifierName.equals("enum")) {
				accessFlags.markPositiveEnum();
			} else if (modifierName.equals("bridge")) {
				accessFlags.markPositiveBridge();
			}
		}

		return accessFlags;
	}

	public static boolean fieldHasAnnotation(AbstractFieldNode fieldNode, ComplexAnnotationSpecifier annotationSpecifier, ClassHierarchy classHierarchy) {
		return annotationsMatch(fieldNode.getAnnotationTypeNames(), annotationSpecifier, classHierarchy);
	}

	public static String buildSignatureString(
		ComplexAnnotationSpecifier annotationSpecifier,
		AccessFlags accessFlags,
		String descriptor,
		NamePattern nameSpecifier,
		String memberName,
		NamePattern methodNameSpecifier,
		MethodArgumentSpecifier methodArgumentSpecifier,
		ComplexAnnotationSpecifier[] parameterAnnotations,
		List<String> throwsExceptionNames,
		boolean includeSignatureClasses
	) {
		StringBuilder signatureBuffer = new StringBuilder();
		if (annotationSpecifier != null) {
			signatureBuffer.append(annotationSpecifier.toRuleString());
		}

		if (accessFlags != null) {
			if (signatureBuffer.length() > 0 && signatureBuffer.charAt(signatureBuffer.length() - 1) != ' ') {
				signatureBuffer.append(' ');
			}

			if (accessFlags.isDefaultAccess()) {
				signatureBuffer.append("package ");
			}

			if (accessFlags.isExcludedPackagePrivate()) {
				signatureBuffer.append(AbstractScriptNode.NEGATED_MODIFIER_MARK + "package" + ' ');
			}

			signatureBuffer.append(accessFlags.toAccessFlagsString());
		}

		if (descriptor != null) {
			if (signatureBuffer.length() > 0 && signatureBuffer.charAt(signatureBuffer.length() - 1) != ' ') {
				signatureBuffer.append(' ');
			}

			signatureBuffer.append(descriptorToHumanReadable(descriptor));
		}

		if (nameSpecifier != null) {
			if (signatureBuffer.length() > 0 && signatureBuffer.charAt(signatureBuffer.length() - 1) != ' ') {
				signatureBuffer.append(' ');
			}

			signatureBuffer.append(nameSpecifier.toRuleString());
		}

		if (memberName != null || methodNameSpecifier != null) {
			if (signatureBuffer.length() > 0 && signatureBuffer.charAt(signatureBuffer.length() - 1) != ' ') {
				signatureBuffer.append(' ');
			}

			if (memberName != null) {
				signatureBuffer.append(memberName);
			} else {
				signatureBuffer.append(methodNameSpecifier.toRuleString());
			}

			signatureBuffer.append('(');
			if (methodArgumentSpecifier != null) {
				signatureBuffer.append(formatMethodArguments(methodArgumentSpecifier, parameterAnnotations));
			}

			signatureBuffer.append(')');
			if (throwsExceptionNames.size() > 0) {
				signatureBuffer.append(" throws ");

				for (int i = 0; i < throwsExceptionNames.size(); i++) {
					signatureBuffer.append(internalToBinaryName(throwsExceptionNames.get(i)));
					if (i < throwsExceptionNames.size() - 1) {
						signatureBuffer.append(", ");
					}
				}
			}

			if (includeSignatureClasses) {
				signatureBuffer.append(" +signatureClasses");
			}
		}

		return signatureBuffer.toString();
	}

	public static String internalToBinaryName(String internalName) {
		return internalName.replace('/', '.');
	}

	public static boolean methodHasAnnotation(AbstractMethodNode methodNode, ComplexAnnotationSpecifier annotationSpecifier, ClassHierarchy classHierarchy) {
		return annotationsMatch(methodNode.getAnnotationTypeNames(), annotationSpecifier, classHierarchy);
	}

	public static boolean matchesMethodDescriptor(String descriptor, MethodArgumentSpecifier methodArgumentSpecifier) {
		String parameterDescriptor = descriptor.substring(0, descriptor.indexOf(41) + 1);
		if (methodArgumentSpecifier == null) {
			return parameterDescriptor.length() == 2;
		} else {
			return methodArgumentSpecifier.isMatchAll() ? true : methodArgumentSpecifier.matchingPattern.matcher(parameterDescriptor).matches();
		}
	}

	public static String formatMethodArguments(MethodArgumentSpecifier methodArgumentSpecifier, ComplexAnnotationSpecifier[] parameterAnnotations) {
		DebugUtils.formatArrayMultiLine(parameterAnnotations);
		StringBuilder argumentsBuffer = new StringBuilder();
		List<String> parameterDescriptors = TypeWriter.parseParameterDescriptors(methodArgumentSpecifier.rawDescriptor);
		int parameterCount = parameterDescriptors.size();

		for (int i = 0; i < parameterCount; i++) {
			String parameterDescriptor = parameterDescriptors.get(i);
			if (parameterAnnotations != null && parameterAnnotations[i] != null) {
				argumentsBuffer.append(parameterAnnotations[i].toRuleString());
				argumentsBuffer.append(' ');
			}

			argumentsBuffer.append(descriptorToHumanReadable(parameterDescriptor));
			if (i < parameterDescriptors.size() - 1) {
				argumentsBuffer.append(", ");
			}
		}

		return argumentsBuffer.toString();
	}

	public static boolean validateThrowsClause(ClassHierarchy classHierarchy, AbstractMethodNode methodNode, List<String> throwsExceptionNames, AtomicReference<String> errorMessage) {
		if (throwsExceptionNames.size() == 0) {
			return true;
		} else {
			ArrayList<String> declaredExceptionNames = methodNode.getDeclaredExceptionInternalNames();
			if (declaredExceptionNames.size() == 0) {
				return false;
			} else {
				for (int throwsExceptionIndex = 0; throwsExceptionIndex < throwsExceptionNames.size(); throwsExceptionIndex++) {
					boolean matched = false;
					String throwsExceptionName = throwsExceptionNames.get(throwsExceptionIndex);
					if (!containsWildcard(throwsExceptionName)) {
						try {
							if (!throwsExceptionName.equals("java/lang/Throwable")
								&& (
									Options.ZKM_MATCH_ON_ORIGINAL && classHierarchy.hasHierarchySnapshot()
										? !classHierarchy.isSuperclassOf(throwsExceptionName, "java/lang/Throwable")
										: !classHierarchy.isClassInstanceOf(throwsExceptionName, "java/lang/Throwable")
								)) {
								errorMessage.set("Method exclude parameter '<0>' in \"<1>\" statement at line <2> has a non-Throwable class in it's throws clause");
							}
						} catch (ClasspathException throwsClasspathException) {
							String className = throwsClasspathException.getClassName();
							if (!className.startsWith("java.rmi")) {
								errorMessage.set(
									"Error while executing \"<1>\" statement at line <2> while analysing '<0>' : class '" + GlobalUtils.internalToBinaryName(className) + "' not found. (A)"
								);
							}
						} catch (FileException throwsFileException) {
							errorMessage.set(
								"Error while executing \"<1>\" statement at line <2> while analysing '<0>' : \""
									+ GlobalUtils.internalToBinaryName(throwsExceptionName)
									+ "\" not found. : \""
									+ throwsFileException.getMessage()
									+ "\" (A)"
							);
						}
					}

					for (int declaredExceptionIndex = 0; declaredExceptionIndex < declaredExceptionNames.size(); declaredExceptionIndex++) {
						String declaredExceptionName = declaredExceptionNames.get(declaredExceptionIndex);

						try {
							if (Options.ZKM_MATCH_ON_ORIGINAL && classHierarchy.hasHierarchySnapshot()) {
								String binaryName = classHierarchy.getBinaryName(declaredExceptionName);
								if (throwsExceptionName.equals(binaryName) || containsWildcard(throwsExceptionName) && StringUtils.matchesWildcard(binaryName, throwsExceptionName) || classHierarchy.isSuperclassOf(binaryName, throwsExceptionName)) {
									matched = true;
									break;
								}
							} else if (throwsExceptionName.equals(declaredExceptionName) || containsWildcard(throwsExceptionName) && StringUtils.matchesWildcard(declaredExceptionName, throwsExceptionName) || classHierarchy.isClassInstanceOf(declaredExceptionName, throwsExceptionName)) {
								matched = true;
								break;
							}
						} catch (ClasspathException declaredClasspathException) {
							errorMessage.set(
								"Error while executing \"<1>\" statement at line <2> while analysing '<1>' : class '"
									+ GlobalUtils.internalToBinaryName(declaredClasspathException.getClassName())
									+ "' not found. (B)"
							);
						} catch (FileException declaredFileException) {
							errorMessage.set("Error while executing \"<1>\" statement at line <2> while analysing '<0>' : \"" + declaredFileException.getMessage() + "\" (B)");
						}
					}

					if (!matched) {
						return false;
					}
				}

				return true;
			}
		}
	}

	public static boolean containsWildcard(String name) {
		return name.indexOf("*") != -1;
	}

	public static boolean annotationsMatch(Enumeration<String> annotationTypeNames, ComplexAnnotationSpecifier annotationSpecifier, ClassHierarchy classHierarchy) {
		if (annotationSpecifier == null) {
			return true;
		} else {
			HashSet<String> memberAnnotationTypeNames = GlobalUtils.newHashSet(13);

			while (annotationTypeNames.hasMoreElements()) {
				String annotationTypeName = annotationTypeNames.nextElement();
				if (Options.ZKM_MATCH_ON_ORIGINAL && classHierarchy.hasHierarchySnapshot()) {
					memberAnnotationTypeNames.add(classHierarchy.getBinaryName(annotationTypeName));
				} else {
					memberAnnotationTypeNames.add(annotationTypeName);
				}
			}

			return annotationSpecifier.matchesAnnotationTypes(memberAnnotationTypeNames);
		}
	}

	public static boolean matchesParameterAnnotations(
		AbstractMethodNode methodNode, ComplexAnnotationSpecifier[] parameterAnnotations, MethodArgumentSpecifier methodArgumentSpecifier, ClassHierarchy classHierarchy
	) {
		if (parameterAnnotations != null) {
			boolean matchOnOriginal = false;
			int parameterCount;
			if (Options.ZKM_MATCH_ON_ORIGINAL && classHierarchy.hasHierarchySnapshot()) {
				parameterCount = TypeWriter.parseParameterDescriptors(methodNode.getOriginalDescriptor()).size();
				matchOnOriginal = true;
			} else {
				parameterCount = methodNode.getParameterCount();
			}

			if (methodArgumentSpecifier.isMatchAll()) {
				if (parameterAnnotations[0] == null) {
					return true;
				}

				HashSet<String> allAnnotationTypeNames = GlobalUtils.newHashSet(23);

				for (int allParameterIndex = 0; allParameterIndex < parameterCount; allParameterIndex++) {
					Enumeration<String> allAnnotationTypeEnumeration;
					if (matchOnOriginal) {
						allAnnotationTypeEnumeration = methodNode.getOriginalParameterAnnotationTypes(allParameterIndex);
					} else {
						allAnnotationTypeEnumeration = methodNode.getParameterAnnotationTypes(allParameterIndex);
					}

					while (allAnnotationTypeEnumeration.hasMoreElements()) {
						String annotationTypeName = allAnnotationTypeEnumeration.nextElement();
						if (matchOnOriginal) {
							allAnnotationTypeNames.add(classHierarchy.getBinaryName(annotationTypeName));
						} else {
							allAnnotationTypeNames.add(annotationTypeName);
						}
					}
				}

				return parameterAnnotations[0].matchesAnnotationTypes(allAnnotationTypeNames);
			}

			if (parameterAnnotations.length == parameterCount) {
				for (int parameterIndex = 0; parameterIndex < parameterAnnotations.length; parameterIndex++) {
					if (parameterAnnotations[parameterIndex] != null) {
						HashSet<String> parameterAnnotationTypeNames = GlobalUtils.newHashSet(13);
						Enumeration<String> parameterAnnotationTypeEnumeration;
						if (matchOnOriginal) {
							parameterAnnotationTypeEnumeration = methodNode.getOriginalParameterAnnotationTypes(parameterIndex);
						} else {
							parameterAnnotationTypeEnumeration = methodNode.getParameterAnnotationTypes(parameterIndex);
						}

						while (parameterAnnotationTypeEnumeration.hasMoreElements()) {
							String parameterAnnotationTypeName = parameterAnnotationTypeEnumeration.nextElement();
							if (matchOnOriginal) {
								parameterAnnotationTypeNames.add(classHierarchy.getBinaryName(parameterAnnotationTypeName));
							} else {
								parameterAnnotationTypeNames.add(parameterAnnotationTypeName);
							}
						}

						if (!parameterAnnotations[parameterIndex].matchesAnnotationTypes(parameterAnnotationTypeNames)) {
							return false;
						}
					}
				}
			}
		}

		return true;
	}

	private double calculateClassMatchScore() {
		return this.isLinkClass() ? 0.05 : this.classSpecifier.calculateClassMatchScore(this.classAccessFlags, this.classAnnotationSpecifier);
	}

	public void excludeParameterChanger(ParameterChangerExcludes excludes) {
		if (this.isEnabled) {
			Set<ClassNode> annotationClasses = null;
			if (this.isSearchByAnnotation()) {
				boolean matchOnOriginal = Options.ZKM_MATCH_ON_ORIGINAL && (excludes.hasHierarchySnapshot() || excludes.isFieldNameObfFinish() || excludes.isMethodNameObfFinish());
				annotationClasses = this.findClassesByAnnotation(excludes.getWorkspaceClasses(), matchOnOriginal);
			}

			if (this.methodSpecifier.isExactName() && excludes.shouldMatchCurrentMethodName()) {
				Enumeration<ClassNode> classEnumeration = excludes.getClassesByMethodName(this.methodSpecifier.toRuleString());
				if (classEnumeration != null) {
					while (classEnumeration.hasMoreElements()) {
						ClassNode classNode = classEnumeration.nextElement();
						String packageName = ExcludeStatementManager.getPackageNameForMatching(classNode);
						String simpleName = ExcludeStatementManager.getSimpleNameForMatching(classNode);
						if (this.matchesClass(classNode, annotationClasses, packageName, simpleName, excludes)) {
							ArrayEnumeration<MethodNode> methodEnumeration = classNode.getMethodEnumeration();

							while (methodEnumeration.hasMoreElements()) {
								MethodNode methodNode = methodEnumeration.nextElement();
								if (this.matchesMethod(excludes, methodNode)) {
									excludes.includeMethod(methodNode, "exclusion parameter '" + this.ruleString + "'");
									this.includeClassIfWildcard(excludes, classNode);
								}
							}
						}
					}
				}
			} else {
				this.excludeParameterChangerMethods(excludes, excludes.getExcludedMethods(), annotationClasses);
			}
		}
	}

	public final boolean hasContainedInClause() {
		return this.containedInClause != null && this.containedInClause.getFilterParameter() != null && this.containedInClause.getFilterParameter().countExplicitSpecifiers() > 0;
	}

	public void includeParameterChanger(ParameterChangerExcludes excludes) {
		if (this.isEnabled) {
			Set<ClassNode> annotationClasses = null;
			if (this.isSearchByAnnotation()) {
				boolean matchOnOriginal = Options.ZKM_MATCH_ON_ORIGINAL && (excludes.hasHierarchySnapshot() || excludes.isFieldNameObfFinish() || excludes.isMethodNameObfFinish());
				annotationClasses = this.findClassesByAnnotation(excludes.getWorkspaceClasses(), matchOnOriginal);
			}

			if (this.methodSpecifier.isExactName() && excludes.shouldMatchCurrentMethodName()) {
				Enumeration<ClassNode> classEnumeration = excludes.getClassesByMethodName(this.methodSpecifier.toRuleString());
				if (classEnumeration != null) {
					while (classEnumeration.hasMoreElements()) {
						ClassNode classNode = classEnumeration.nextElement();
						String packageName = ExcludeStatementManager.getPackageNameForMatching(classNode);
						String simpleName = ExcludeStatementManager.getSimpleNameForMatching(classNode);
						if (this.matchesClass(classNode, annotationClasses, packageName, simpleName, excludes)) {
							ArrayEnumeration<MethodNode> methodEnumeration = classNode.getMethodEnumeration();

							while (methodEnumeration.hasMoreElements()) {
								MethodNode methodNode = methodEnumeration.nextElement();
								if (ParameterChangerExcludes.isProcessableMethod(methodNode) && this.matchesMethod(excludes, methodNode)) {
									excludes.excludeMethod(methodNode, "exclusion parameter '" + this.ruleString + "'");
									this.excludeClass(excludes, classNode);
								}
							}
						}
					}
				}
			} else {
				this.includeParameterChangerMethods(excludes, excludes.getProcessableMethods(), annotationClasses);
			}
		}
	}

	public boolean matchesMethod(ClassHierarchy classHierarchy, AbstractMethodNode methodNode) {
		if (!matchesAccessFlags(methodNode.getAccessFlags(), this.memberAccessFlags)) {
			return false;
		} else if (!this.matchesMethodName(methodNode)) {
			return false;
		} else if (!matchesMethodDescriptor(ExcludeStatementManager.getDescriptorForMatching(methodNode), this.methodArgumentSpecifier)) {
			return false;
		} else if (!methodHasAnnotation(methodNode, this.memberAnnotationSpecifier, classHierarchy)) {
			return false;
		} else if (!matchesParameterAnnotations(methodNode, this.parameterAnnotationSpecifiers, this.methodArgumentSpecifier, classHierarchy)) {
			return false;
		} else {
			AtomicReference<String> errorMessage = new AtomicReference<>();
			boolean throwsClauseValid = validateThrowsClause(classHierarchy, methodNode, this.throwsExceptionNames, errorMessage);
			if (!errorMessage.isNull()) {
				String warningMessage = StringUtils.replace(
					StringUtils.replace(StringUtils.replace(errorMessage.get(), "<0>", this.ruleString), "<1>", this.getCommandName()), "<2>", String.valueOf(this.getLineNumber())
				);
				this.scriptContext.logWarning(warningMessage);
			}

			return throwsClauseValid;
		}
	}

	private String joinWithAnd(Vector<String> names) {
		StringBuffer namesBuffer = new StringBuffer();

		for (int i = 0; i < names.size(); i++) {
			if (i == names.size() - 1) {
				namesBuffer.append(names.elementAt(i));
			} else if (i == names.size() - 2) {
				namesBuffer.append(names.elementAt(i) + " and ");
			} else {
				namesBuffer.append(names.elementAt(i) + ", ");
			}
		}

		return namesBuffer.toString();
	}

	private void applyTrimExclusionToFields(TrimExcludes excludes, Enumeration<FieldNode> fields, Set<ClassNode> annotationClasses) {
		while (fields.hasMoreElements()) {
			FieldNode fieldNode = fields.nextElement();
			if (this.isFieldMatch(fieldNode, excludes)) {
				ClassNode ownerClass = fieldNode.getOwnerClass();
				String packageName = ExcludeStatementManager.getPackageNameForMatching(ownerClass);
				String simpleName = ExcludeStatementManager.getSimpleNameForMatching(ownerClass);
				if (this.matchesClass(ownerClass, annotationClasses, packageName, simpleName, excludes)) {
					excludes.excludeField(fieldNode, "exclusion parameter '" + this.ruleString + "'");
					this.warnPackageExclusionIgnored(excludes, packageName);
					this.excludeClass(excludes, ownerClass);
				}
			}
		}
	}

	public final String toRuleString() {
		StringBuilder ruleStringBuffer = new StringBuilder();
		if (this.containedInClause != null) {
			ruleStringBuffer.append(this.containedInClause.toRuleString());
			ruleStringBuffer.append(' ');
		}

		if (this.classAnnotationSpecifier != null) {
			ruleStringBuffer.append(this.classAnnotationSpecifier.toRuleString());
			ruleStringBuffer.append(' ');
		}

		if (this.classAccessFlags != null && this.classAccessFlags.isDefaultAccess()) {
			ruleStringBuffer.append("package ");
		}

		if (this.classAccessFlags != null && this.classAccessFlags.isExcludedPackagePrivate()) {
			ruleStringBuffer.append(AbstractScriptNode.NEGATED_MODIFIER_MARK + "package ");
		}

		if (!this.isAnnotationType) {
			ruleStringBuffer.append(this.classAccessFlags != null ? this.classAccessFlags.toAccessFlagsString() : "");
			if (this.searchByAnnotationName != null) {
				ruleStringBuffer.append('@');
				ruleStringBuffer.append(internalToBinaryName(this.searchByAnnotationName));
				ruleStringBuffer.append('(');
				ruleStringBuffer.append(this.searchByAnnotationProperty);
				ruleStringBuffer.append('=');
			}

			if (this.sourceFilePattern != null) {
				ruleStringBuffer.append("\"");
				ruleStringBuffer.append(this.sourceFilePattern);
				ruleStringBuffer.append("\"");
				ruleStringBuffer.append("!");
			}

			if (this.packageSpecifier != null) {
				ruleStringBuffer.append(this.packageSpecifier.toRuleString());
			}

			if (this.linkClassNamePattern != null) {
				ruleStringBuffer.append(this.linkClassNamePattern);
			} else if (this.classSpecifier != null) {
				ruleStringBuffer.append(this.classSpecifier.toRuleString());
			}

			if (this.searchByAnnotationName != null) {
				ruleStringBuffer.append(')');
			}

			if (this.containingClause != null) {
				ruleStringBuffer.append(' ' + this.containingClause.toRuleString());
			}

			if (this.extendsClassName != null) {
				ruleStringBuffer.append(" extends ");
				if (this.extendsAnnotationSpecifier != null) {
					ruleStringBuffer.append(this.extendsAnnotationSpecifier.toRuleString());
					ruleStringBuffer.append(" ");
				}

				ruleStringBuffer.append(internalToBinaryName(this.extendsClassName));
			}

			if (this.implementsInterfaceNames.size() > 0) {
				ruleStringBuffer.append(" implements ");

				for (int interfaceIndex = 0; interfaceIndex < this.implementsInterfaceNames.size(); interfaceIndex++) {
					if (this.implementsAnnotationSpecifiers.get(interfaceIndex) != null) {
						ruleStringBuffer.append(this.implementsAnnotationSpecifiers.get(interfaceIndex).toRuleString());
						ruleStringBuffer.append(" ");
					}

					ruleStringBuffer.append(internalToBinaryName(this.implementsInterfaceNames.get(interfaceIndex)));
					if (interfaceIndex < this.implementsInterfaceNames.size() - 1) {
						ruleStringBuffer.append(", ");
					}
				}
			}
		} else {
			ruleStringBuffer.append('@');
			if (this.packageSpecifier != null) {
				ruleStringBuffer.append(this.packageSpecifier.toRuleString());
			}

			ruleStringBuffer.append(this.classSpecifier.toRuleString());
		}

		if (this.searchPaths.size() > 0) {
			ruleStringBuffer.append(" search ");

			for (int searchPathIndex = 0; searchPathIndex < this.searchPaths.size(); searchPathIndex++) {
				ruleStringBuffer.append(this.searchPaths.get(searchPathIndex).replace('/', '.'));
				if (searchPathIndex < this.searchPaths.size() - 1) {
					ruleStringBuffer.append(", ");
				}
			}
		}

		String signatureString = buildSignatureString(
			this.memberAnnotationSpecifier,
			this.memberAccessFlags,
			this.fieldTypeDescriptor,
			this.fieldSpecifier,
			this.linkMethodNamePattern,
			this.methodSpecifier,
			this.methodArgumentSpecifier,
			this.parameterAnnotationSpecifiers,
			this.throwsExceptionNames,
			this.includeSignatureClasses
		);
		if (signatureString.length() > 0) {
			ruleStringBuffer.append(' ');
			ruleStringBuffer.append(signatureString);
		}

		if (this.moduleSpecifier != null) {
			ruleStringBuffer.append(this.moduleSpecifier.toRuleString());
		}

		return ruleStringBuffer.toString();
	}

	@Override
	public void setFieldSpecifier(ComplexFieldSpecifier fieldSpecifier) {
		this.fieldSpecifier = fieldSpecifier;
	}

	public boolean isClassTarget() {
		return this.targetType == 2;
	}

	public void excludeAutoReflection(AutoReflectionExcludes excludes) {
		if (this.isEnabled) {
			Set<ClassNode> annotationClasses = null;
			if (this.isSearchByAnnotation()) {
				boolean matchOnOriginal = Options.ZKM_MATCH_ON_ORIGINAL && (excludes.hasHierarchySnapshot() || excludes.isFieldNameObfFinish() || excludes.isMethodNameObfFinish());
				annotationClasses = this.findClassesByAnnotation(excludes.getWorkspaceClasses(), matchOnOriginal);
			}

			switch (this.targetType) {
				case 2:
					if (!this.hasExplicitPackageOrClass() && !this.isClassWildcard()) {
						this.excludeAutoReflectionClasses(excludes, excludes.getProcessableClasses(), annotationClasses);
					} else {
						this.excludeAutoReflectionClasses(excludes, excludes.getWorkspaceClasses(), annotationClasses);
					}
					break;
				case 3:
					if (this.fieldSpecifier.isExactName() && excludes.shouldMatchCurrentFieldName()) {
						Enumeration<ClassNode> fieldClassEnumeration = excludes.getClassesByFieldName(this.fieldSpecifier.toRuleString());
						if (fieldClassEnumeration != null) {
							while (fieldClassEnumeration.hasMoreElements()) {
								ClassNode fieldClassNode = fieldClassEnumeration.nextElement();
								String fieldPackageName = ExcludeStatementManager.getPackageNameForMatching(fieldClassNode);
								String fieldSimpleName = ExcludeStatementManager.getSimpleNameForMatching(fieldClassNode);
								if (this.matchesClass(fieldClassNode, annotationClasses, fieldPackageName, fieldSimpleName, excludes)) {
									ArrayEnumeration<FieldNode> fieldEnumeration = fieldClassNode.getFieldEnumeration();

									while (fieldEnumeration.hasMoreElements()) {
										FieldNode fieldNode = fieldEnumeration.nextElement();
										if (this.isFieldMatch(fieldNode, excludes)) {
											excludes.excludeFieldBecause(fieldNode, "exclusion parameter '" + this.ruleString + "'");
											this.excludeClass(excludes, fieldClassNode);
										}
									}
								}
							}
						}
					} else if (this.hasExplicitPackageOrClass()) {
						this.excludeAutoReflectionFields(excludes, excludes.getAllFields(), annotationClasses);
					} else {
						this.excludeAutoReflectionFields(excludes, excludes.getProcessableFields(), annotationClasses);
					}
					break;
				case 4:
					if (this.methodSpecifier.isExactName() && excludes.shouldMatchCurrentMethodName()) {
						Enumeration<ClassNode> methodClassEnumeration = excludes.getClassesByMethodName(this.methodSpecifier.toRuleString());
						if (methodClassEnumeration != null) {
							while (methodClassEnumeration.hasMoreElements()) {
								ClassNode methodClassNode = methodClassEnumeration.nextElement();
								String methodPackageName = ExcludeStatementManager.getPackageNameForMatching(methodClassNode);
								String methodSimpleName = ExcludeStatementManager.getSimpleNameForMatching(methodClassNode);
								if (this.matchesClass(methodClassNode, annotationClasses, methodPackageName, methodSimpleName, excludes)) {
									ArrayEnumeration<MethodNode> methodEnumeration = methodClassNode.getMethodEnumeration();

									while (methodEnumeration.hasMoreElements()) {
										MethodNode methodNode = methodEnumeration.nextElement();
										if (this.matchesMethod(excludes, methodNode)) {
											excludes.excludeMethodBecause(methodNode, "exclusion parameter '" + this.ruleString + "'");
											this.excludeClass(excludes, methodClassNode);
										}
									}
								}
							}
						}
					} else if (this.hasExplicitPackageOrClass()) {
						this.excludeAutoReflectionMethods(excludes, excludes.getAllMethods(), annotationClasses);
					} else {
						this.excludeAutoReflectionMethods(excludes, excludes.getProcessableMethods(), annotationClasses);
					}
			}
		}
	}

	public boolean isPackageTarget() {
		return this.targetType == 1;
	}

	public final String getCommandName() {
		return this.scriptCommandNode.getCommandName();
	}

	private void unexcludeSignatureClasses(DefaultExcludes excludes, MethodNode methodNode) {
		ArrayList<ClassNode> descriptorClasses = methodNode.getDescriptorWorkspaceClasses();

		for (int i = 0; i < descriptorClasses.size(); i++) {
			ClassNode classNode = descriptorClasses.get(i);
			String packageName = classNode.getPackageName();
			excludes.unexcludePackage(packageName, "unexclusion parameter '" + this.ruleString + "'");
			excludes.includeClassWithReason(classNode, "unexclusion parameter '" + this.ruleString + "'", false);
		}
	}

	private void excludeSignatureClasses(DefaultExcludes excludes, MethodNode methodNode) {
		ArrayList<ClassNode> descriptorClasses = methodNode.getDescriptorWorkspaceClasses();

		for (int i = 0; i < descriptorClasses.size(); i++) {
			ClassNode classNode = descriptorClasses.get(i);
			String packageName = classNode.getPackageName();
			excludes.excludePackage(packageName, "exclusion parameter '" + this.ruleString + "'");
			excludes.excludeClass(classNode, "exclusion parameter '" + this.ruleString + "'");
		}
	}

	public void includeIntegerEncryption(IntegerEncryptionExcludes excludes) {
		Set<ClassNode> annotationClasses = null;
		if (this.isSearchByAnnotation()) {
			boolean matchOnOriginal = Options.ZKM_MATCH_ON_ORIGINAL && (excludes.hasHierarchySnapshot() || excludes.isFieldNameObfFinish() || excludes.isMethodNameObfFinish());
			annotationClasses = this.findClassesByAnnotation(excludes.getWorkspaceClasses(), matchOnOriginal);
		}

		switch (this.targetType) {
			case 2:
				this.includeIntegerEncryptionClasses(excludes, excludes.getWorkspaceClasses(), annotationClasses);
				break;
			case 3:
				if (this.fieldSpecifier.isExactName() && excludes.shouldMatchCurrentFieldName()) {
					Enumeration<ClassNode> fieldClassEnumeration = excludes.getClassesByFieldName(this.fieldSpecifier.toRuleString());
					if (fieldClassEnumeration != null) {
						while (fieldClassEnumeration.hasMoreElements()) {
							ClassNode fieldClassNode = fieldClassEnumeration.nextElement();
							String fieldPackageName = ExcludeStatementManager.getPackageNameForMatching(fieldClassNode);
							String fieldSimpleName = ExcludeStatementManager.getSimpleNameForMatching(fieldClassNode);
							if (this.matchesClass(fieldClassNode, annotationClasses, fieldPackageName, fieldSimpleName, excludes)) {
								ArrayEnumeration<FieldNode> fieldEnumeration = fieldClassNode.getFieldEnumeration();

								while (fieldEnumeration.hasMoreElements()) {
									FieldNode fieldNode = fieldEnumeration.nextElement();
									if (this.isFieldMatch(fieldNode, excludes)) {
										excludes.includeFieldConstants(fieldNode, "unexclusion parameter '" + this.ruleString + "'");
									}
								}
							}
						}
					}
				} else {
					this.includeIntegerEncryptionFields(excludes, excludes.getAllFields(), annotationClasses);
				}
				break;
			case 4:
				if (this.methodSpecifier.isExactName() && excludes.shouldMatchCurrentMethodName()) {
					Enumeration<ClassNode> methodClassEnumeration = excludes.getClassesByMethodName(this.methodSpecifier.toRuleString());
					if (methodClassEnumeration != null) {
						while (methodClassEnumeration.hasMoreElements()) {
							ClassNode methodClassNode = methodClassEnumeration.nextElement();
							String methodPackageName = ExcludeStatementManager.getPackageNameForMatching(methodClassNode);
							String methodSimpleName = ExcludeStatementManager.getSimpleNameForMatching(methodClassNode);
							if (this.matchesClass(methodClassNode, annotationClasses, methodPackageName, methodSimpleName, excludes)) {
								ArrayEnumeration<MethodNode> methodEnumeration = methodClassNode.getMethodEnumeration();

								while (methodEnumeration.hasMoreElements()) {
									MethodNode methodNode = methodEnumeration.nextElement();
									if (this.matchesMethod(excludes, methodNode)) {
										excludes.includeMethodConstants(methodNode, "unexclusion parameter '" + this.ruleString + "'");
									}
								}
							}
						}
					}
				} else {
					this.includeIntegerEncryptionMethods(excludes, excludes.getAllMethods(), annotationClasses);
				}
		}
	}

	public void excludeIntegerEncryption(IntegerEncryptionExcludes excludes) {
		Set<ClassNode> annotationClasses = null;
		if (this.isSearchByAnnotation()) {
			boolean matchOnOriginal = Options.ZKM_MATCH_ON_ORIGINAL && (excludes.hasHierarchySnapshot() || excludes.isFieldNameObfFinish() || excludes.isMethodNameObfFinish());
			annotationClasses = this.findClassesByAnnotation(excludes.getWorkspaceClasses(), matchOnOriginal);
		}

		switch (this.targetType) {
			case 2:
				this.excludeIntegerEncryptionClasses(excludes, excludes.getProcessableClasses(), annotationClasses);
				break;
			case 3:
				if (this.fieldSpecifier.isExactName() && excludes.shouldMatchCurrentFieldName()) {
					Enumeration<ClassNode> fieldClassEnumeration = excludes.getClassesByFieldName(this.fieldSpecifier.toRuleString());
					if (fieldClassEnumeration != null) {
						while (fieldClassEnumeration.hasMoreElements()) {
							ClassNode fieldClassNode = fieldClassEnumeration.nextElement();
							String fieldPackageName = ExcludeStatementManager.getPackageNameForMatching(fieldClassNode);
							String fieldSimpleName = ExcludeStatementManager.getSimpleNameForMatching(fieldClassNode);
							if (this.matchesClass(fieldClassNode, annotationClasses, fieldPackageName, fieldSimpleName, excludes)) {
								ArrayEnumeration<FieldNode> fieldEnumeration = fieldClassNode.getFieldEnumeration();

								while (fieldEnumeration.hasMoreElements()) {
									FieldNode fieldNode = fieldEnumeration.nextElement();
									if (this.isFieldMatch(fieldNode, excludes)) {
										excludes.excludeFieldConstants(fieldNode, "exclusion parameter '" + this.ruleString + "'");
									}
								}
							}
						}
					}
				} else {
					this.excludeIntegerEncryptionFields(excludes, excludes.getProcessableFields(), annotationClasses);
				}
				break;
			case 4:
				if (this.methodSpecifier.isExactName() && excludes.shouldMatchCurrentMethodName()) {
					Enumeration<ClassNode> methodClassEnumeration = excludes.getClassesByMethodName(this.methodSpecifier.toRuleString());
					if (methodClassEnumeration != null) {
						while (methodClassEnumeration.hasMoreElements()) {
							ClassNode methodClassNode = methodClassEnumeration.nextElement();
							String methodPackageName = ExcludeStatementManager.getPackageNameForMatching(methodClassNode);
							String methodSimpleName = ExcludeStatementManager.getSimpleNameForMatching(methodClassNode);
							if (this.matchesClass(methodClassNode, annotationClasses, methodPackageName, methodSimpleName, excludes)) {
								ArrayEnumeration<MethodNode> methodEnumeration = methodClassNode.getMethodEnumeration();

								while (methodEnumeration.hasMoreElements()) {
									MethodNode methodNode = methodEnumeration.nextElement();
									if (this.matchesMethod(excludes, methodNode)) {
										excludes.excludeMethodConstants(methodNode, "exclusion parameter '" + this.ruleString + "'");
									}
								}
							}
						}
					}
				} else {
					this.excludeIntegerEncryptionMethods(excludes, excludes.getProcessableMethods(), annotationClasses);
				}
		}
	}

	private void applyTrimInclusionToMethods(TrimExcludes excludes, Enumeration<MethodNode> methods, Set<ClassNode> annotationClasses) {
		while (methods.hasMoreElements()) {
			MethodNode methodNode = methods.nextElement();
			if (this.matchesMethod(excludes, methodNode)) {
				ClassNode methodOwner = methodNode.getMethodOwner();
				String packageName = ExcludeStatementManager.getPackageNameForMatching(methodOwner);
				String simpleName = ExcludeStatementManager.getSimpleNameForMatching(methodOwner);
				if (this.matchesClass(methodOwner, annotationClasses, packageName, simpleName, excludes)) {
					excludes.unexcludeMethod(methodNode, "unexclusion parameter '" + this.ruleString + "'");
					this.warnPackageExclusionIgnored(excludes, packageName);
					this.includeClassIfWildcard(excludes, methodOwner);
				}
			}
		}
	}

	private double calculateFieldMatchScore() {
		return this.fieldSpecifier.calculateMatchScore(this.memberAccessFlags, this.fieldTypeDescriptor, this.memberAnnotationSpecifier);
	}

	public boolean isWildcardField() {
		return this.fieldSpecifier == null ? false : this.fieldTypeDescriptor == null && isWildcardStar(this.fieldSpecifier.getNamePattern());
	}

	public void setModuleSpecifier(ComplexModuleSpecifier moduleSpecifier) {
		this.moduleSpecifier = moduleSpecifier;
	}

	public void warnPackageIgnoredForTrim(TrimExcludes excludes, String packageName) {
		this.warnPackageExclusionIgnored(excludes, packageName);
	}

	public void warnPackageExclusionIgnored(TrimExcludes excludes, String packageName) {
		if (this.packageSpecifier != null && this.packageSpecifier.isPackageRecursive()) {
			this.scriptContext
				.logWarningInternal(
					"\""
						+ this.getCommandName()
						+ "\" parameters cannot specify a package exclusion. Package exclusion in \""
						+ this.ruleString
						+ "\" in \""
						+ this.getCommandName()
						+ "\" statement at line "
						+ this.getLineNumber()
						+ " will be ignored.",
					true
				);
		}
	}

	private void includeRemoveMethodCallMethods(RemoveMethodCallExcludes excludes, Enumeration<AbstractMethodNode> methods, Set<ClassNode> annotationClasses) {
		while (methods.hasMoreElements()) {
			AbstractMethodNode methodNode = methods.nextElement();
			if (this.matchesMethod(excludes, methodNode)) {
				AbstractClassNode classNode = methodNode.getClassNode();
				String packageName = ExcludeStatementManager.getPackageNameForMatching(classNode);
				String simpleName = ExcludeStatementManager.getSimpleNameForMatching(classNode);
				if (this.matchesClass(classNode, annotationClasses, packageName, simpleName, excludes)) {
					if (this.containedInClause == null) {
						excludes.includeMethod(methodNode, null, "inclusion parameter '" + this.ruleString + "'");
					} else {
						for (InstructionListNode callSite : excludes.getCallSites(methodNode)) {
							if (this.matchesContainedInMethod(excludes, callSite)) {
								excludes.includeMethod(methodNode, callSite, "inclusion parameter '" + this.ruleString + "'");
							}
						}
					}
				}
			}
		}
	}

	public boolean isClassSpecifierRecursive() {
		return this.classSpecifier != null && this.classSpecifier.isRecursive();
	}

	public void excludeObfuscateReference(ObfuscateReferenceExcludes excludes) {
		if (this.isEnabled) {
			Set<ClassNode> annotationClasses = null;
			if (this.isSearchByAnnotation()) {
				boolean matchOnOriginal = Options.ZKM_MATCH_ON_ORIGINAL && (excludes.hasHierarchySnapshot() || excludes.isFieldNameObfFinish() || excludes.isMethodNameObfFinish());
				annotationClasses = this.findClassesByAnnotation(excludes.getAnnotationSearchClasses(), matchOnOriginal);
			}

			switch (this.targetType) {
				case 3:
					this.excludeObfuscateReferenceFields(excludes, excludes.getIncludedFields(), annotationClasses);
					break;
				case 4:
					this.excludeObfuscateReferenceMethods(excludes, excludes.getIncludedMethods(), annotationClasses);
			}
		}
	}

	public void includeObfuscateReference(ObfuscateReferenceExcludes excludes) {
		if (this.isEnabled) {
			Set<ClassNode> annotationClasses = null;
			if (this.isSearchByAnnotation()) {
				boolean matchOnOriginal = Options.ZKM_MATCH_ON_ORIGINAL && (excludes.hasHierarchySnapshot() || excludes.isFieldNameObfFinish() || excludes.isMethodNameObfFinish());
				annotationClasses = this.findClassesByAnnotation(excludes.getAnnotationSearchClasses(), matchOnOriginal);
			}

			switch (this.targetType) {
				case 3:
					this.includeObfuscateReferenceFields(excludes, excludes.getExcludedFields(), annotationClasses);
					break;
				case 4:
					this.includeObfuscateReferenceMethods(excludes, excludes.getExcludedMethods(), annotationClasses);
			}
		}
	}

	public void unexcludePackage(DefaultExcludes excludes, String packageName) {
		if (this.packageSpecifier != null && this.packageSpecifier.isPackageRecursive()) {
			excludes.unexcludePackage(packageName, "unexclusion parameter '" + this.ruleString + "'");
		}
	}

	private boolean isClassAbstract() {
		return this.classAccessFlags.isPositiveAbstract();
	}

	public void excludePackage(DefaultExcludes excludes, String packageName) {
		if (this.packageSpecifier != null && this.packageSpecifier.isPackageRecursive()) {
			excludes.excludePackage(packageName, "exclusion parameter '" + this.ruleString + "'");
		}
	}

	public void setContainedInClause(ContainedInClause containedInClause) {
		this.containedInClause = containedInClause;
	}

	@Override
	public void setFieldTypeDescriptor(String fieldTypeDescriptor) {
		this.fieldTypeDescriptor = fieldTypeDescriptor;
	}

	private boolean isMemberAbstract() {
		return this.memberAccessFlags.isPositiveAbstract();
	}

	private void excludeRenameMethods(DefaultExcludes excludes, Enumeration<MethodNode> methods, Set<ClassNode> annotationClasses) {
		while (methods.hasMoreElements()) {
			MethodNode methodNode = methods.nextElement();
			if (this.matchesMethod(excludes, methodNode)) {
				ClassNode methodOwner = methodNode.getMethodOwner();
				String excludedPackageName = methodOwner.getPackageName();
				String packageName = ExcludeStatementManager.getPackageNameForMatching(methodOwner);
				String simpleName = ExcludeStatementManager.getSimpleNameForMatching(methodOwner);
				if (this.matchesClass(methodOwner, annotationClasses, packageName, simpleName, excludes)) {
					if (this.hasLinkMethodNamePattern()) {
						excludes.matchMethodToField(methodNode, this.linkMethodNamePrefix, null, "exclusion parameter '" + this.ruleString + "'");
					} else {
						excludes.excludeMethod(methodNode, "exclusion parameter '" + this.ruleString + "'");
						this.excludePackage(excludes, excludedPackageName);
						this.excludeClass(excludes, methodOwner);
						if (this.includeSignatureClasses) {
							this.excludeSignatureClasses(excludes, methodNode);
						}
					}
				}
			}
		}
	}

	private void includeRenameMethods(DefaultExcludes excludes, Enumeration<MethodNode> methods, Set<ClassNode> annotationClasses) {
		while (methods.hasMoreElements()) {
			MethodNode methodNode = methods.nextElement();
			if (this.matchesMethod(excludes, methodNode)) {
				ClassNode methodOwner = methodNode.getMethodOwner();
				String packageName = ExcludeStatementManager.getPackageNameForMatching(methodOwner);
				String simpleName = ExcludeStatementManager.getSimpleNameForMatching(methodOwner);
				if (this.matchesClass(methodOwner, annotationClasses, packageName, simpleName, excludes)) {
					excludes.unexcludeMethod(methodNode, "unexclusion parameter '" + this.ruleString + "'");
					this.unexcludePackage(excludes, packageName);
					this.includeClassIfWildcard(excludes, methodOwner);
					if (this.includeSignatureClasses) {
						this.unexcludeSignatureClasses(excludes, methodNode);
					}
				}
			}
		}
	}

	private void applyRenameInclusionToFields(DefaultExcludes excludes, Enumeration<FieldNode> fields, Set<ClassNode> annotationClasses) {
		while (fields.hasMoreElements()) {
			FieldNode fieldNode = fields.nextElement();
			if (this.isFieldMatch(fieldNode, excludes)) {
				ClassNode ownerClass = fieldNode.getOwnerClass();
				String packageName = ExcludeStatementManager.getPackageNameForMatching(ownerClass);
				String simpleName = ExcludeStatementManager.getSimpleNameForMatching(ownerClass);
				if (this.matchesClass(ownerClass, annotationClasses, packageName, simpleName, excludes)) {
					excludes.unexcludeField(fieldNode, "unexclusion parameter '" + this.ruleString + "'");
					this.unexcludePackage(excludes, packageName);
					this.includeClassIfWildcard(excludes, ownerClass);
				}
			}
		}
	}

	private void applyRenameExclusionToFields(DefaultExcludes excludes, Enumeration<FieldNode> fields, Set<ClassNode> annotationClasses) {
		while (fields.hasMoreElements()) {
			FieldNode fieldNode = fields.nextElement();
//			System.out.println("[" + fieldNode.getOwnerClass().getClassName() + "] " + "Checking Field: " + fieldNode.getName() + " " + fieldNode.getDescriptor() + " " );
			if (this.isFieldMatch(fieldNode, excludes)) {
				ClassNode ownerClass = fieldNode.getOwnerClass();
				String excludedPackageName = ownerClass.getPackageName();
				String packageName = ExcludeStatementManager.getPackageNameForMatching(ownerClass);
				String simpleName = ExcludeStatementManager.getSimpleNameForMatching(ownerClass);
				if (this.matchesClass(ownerClass, annotationClasses, packageName, simpleName, excludes)) {
					excludes.excludeField(fieldNode, "exclusion parameter '" + this.ruleString + "'");
					this.excludePackage(excludes, excludedPackageName);
					this.excludeClass(excludes, ownerClass);
				}
			}
		}
	}

	public void setContainingClause(ContainingClause containingClause) {
		this.containingClause = containingClause;
	}

	private void excludeRenameClasses(DefaultExcludes excludes, Enumeration<ClassNode> classes, Set<ClassNode> annotationClasses) {
		while (classes.hasMoreElements()) {
			ClassNode classNode = classes.nextElement();
			this.applyRenameExclusionToClass(excludes, classNode, annotationClasses);
		}
	}

	@Override
	public void parseArguments(ScriptNode parent, ScriptContext scriptContext) throws IOException {
		this.scriptContext = scriptContext;
		this.scriptCommandNode = (ScriptLocation)parent;
		int childCount = this.getChildrenCount();

		for (int i = 0; i < childCount; i++) {
			this.getChild(i).parseArguments(this, this.scriptContext);
		}

		this.targetType = this.determineTargetType();
		this.parseAccessFlags();
		this.noOpPackageSpecifierCheck();
		this.noOpClassSpecifierCheck();
		this.priority = this.calculatePriority();
		this.ruleString = this.toRuleString();
		this.logClassAndMembersWarning();
		this.validateAccessFlags();
	}

	public void includeAutoReflection(AutoReflectionExcludes excludes) {
		if (this.isEnabled) {
			Set<ClassNode> annotationClasses = null;
			if (this.isSearchByAnnotation()) {
				boolean matchOnOriginal = Options.ZKM_MATCH_ON_ORIGINAL && (excludes.hasHierarchySnapshot() || excludes.isFieldNameObfFinish() || excludes.isMethodNameObfFinish());
				annotationClasses = this.findClassesByAnnotation(excludes.getWorkspaceClasses(), matchOnOriginal);
			}

			switch (this.targetType) {
				case 2:
					if (!this.hasExplicitPackageOrClass() && !this.isClassWildcard()) {
						this.includeAutoReflectionClasses(excludes, excludes.getExcludedClasses(), annotationClasses);
					} else {
						this.includeAutoReflectionClasses(excludes, excludes.getWorkspaceClasses(), annotationClasses);
					}
					break;
				case 3:
					if (this.fieldSpecifier.isExactName() && excludes.shouldMatchCurrentFieldName()) {
						Enumeration<ClassNode> classesByFieldName = excludes.getClassesByFieldName(this.fieldSpecifier.toRuleString());
						if (classesByFieldName != null) {
							while (classesByFieldName.hasMoreElements()) {
								ClassNode fieldOwnerClass = classesByFieldName.nextElement();
								String fieldOwnerPackageName = ExcludeStatementManager.getPackageNameForMatching(fieldOwnerClass);
								String fieldOwnerSimpleName = ExcludeStatementManager.getSimpleNameForMatching(fieldOwnerClass);
								if (this.matchesClass(fieldOwnerClass, annotationClasses, fieldOwnerPackageName, fieldOwnerSimpleName, excludes)) {
									ArrayEnumeration<FieldNode> fieldEnumeration = fieldOwnerClass.getFieldEnumeration();

									while (fieldEnumeration.hasMoreElements()) {
										FieldNode fieldNode = fieldEnumeration.nextElement();
										if (this.isFieldMatch(fieldNode, excludes)) {
											excludes.includeFieldBecause(fieldNode, "unexclusion parameter '" + this.ruleString + "'");
											this.includeClassIfWildcard(excludes, fieldOwnerClass);
										}
									}
								}
							}
						}
					} else if (this.hasExplicitPackageOrClass()) {
						this.includeAutoReflectionFields(excludes, excludes.getAllFields(), annotationClasses);
					} else {
						this.includeAutoReflectionFields(excludes, excludes.getExcludedFields(), annotationClasses);
					}
					break;
				case 4:
					if (this.methodSpecifier.isExactName() && excludes.shouldMatchCurrentMethodName()) {
						Enumeration<ClassNode> classesByMethodName = excludes.getClassesByMethodName(this.methodSpecifier.toRuleString());
						if (classesByMethodName != null) {
							while (classesByMethodName.hasMoreElements()) {
								ClassNode methodOwnerClass = classesByMethodName.nextElement();
								String methodOwnerPackageName = ExcludeStatementManager.getPackageNameForMatching(methodOwnerClass);
								String methodOwnerSimpleName = ExcludeStatementManager.getSimpleNameForMatching(methodOwnerClass);
								if (this.matchesClass(methodOwnerClass, annotationClasses, methodOwnerPackageName, methodOwnerSimpleName, excludes)) {
									ArrayEnumeration<MethodNode> methodEnumeration = methodOwnerClass.getMethodEnumeration();

									while (methodEnumeration.hasMoreElements()) {
										MethodNode methodNode = methodEnumeration.nextElement();
										if (this.matchesMethod(excludes, methodNode)) {
											excludes.includeMethodBecause(methodNode, "unexclusion parameter '" + this.ruleString + "'");
											this.includeClassIfWildcard(excludes, methodOwnerClass);
										}
									}
								}
							}
						}
					} else if (this.hasExplicitPackageOrClass()) {
						this.includeAutoReflectionMethods(excludes, excludes.getAllMethods(), annotationClasses);
					} else {
						this.includeAutoReflectionMethods(excludes, excludes.getExcludedMethods(), annotationClasses);
					}
			}
		}
	}

	private boolean isMemberSynchronized() {
		return this.memberAccessFlags.isPositiveSynchronized();
	}

	private void applyTrimExclusionToClass(TrimExcludes excludes, ClassNode classNode, Set<ClassNode> annotationClasses) {
		String packageName = ExcludeStatementManager.getPackageNameForMatching(classNode);
		String simpleName = ExcludeStatementManager.getSimpleNameForMatching(classNode);
		if (this.matchesClass(classNode, annotationClasses, packageName, simpleName, excludes)) {
			excludes.excludeClass(classNode, "exclusion parameter '" + this.ruleString + "'");
			if (this.classSpecifier.includesMembers()) {
				this.excludeAllMembersFromTrim(excludes, classNode);
			}
		}
	}

	private void applyTrimInclusionToClass(TrimExcludes excludes, ClassNode classNode, Set<ClassNode> annotationClasses) {
		String packageName = ExcludeStatementManager.getPackageNameForMatching(classNode);
		String simpleName = ExcludeStatementManager.getSimpleNameForMatching(classNode);
		if (this.matchesClass(classNode, annotationClasses, packageName, simpleName, excludes)) {
			excludes.includeClass(classNode, "unexclusion parameter '" + this.ruleString + "'");
			if (this.classSpecifier.includesMembers()) {
				this.includeAllMembersInTrim(excludes, classNode);
			}
		}
	}

	public final int getLineNumber() {
		return this.scriptCommandNode.getLineNumber();
	}

	public final boolean hasExplicitPackageOrClass() {
		return this.countExplicitSpecifiers() > 0;
	}

	public boolean matchesFieldName(AbstractFieldNode fieldNode) {
		return this.fieldSpecifier.matchesWildcard(ExcludeStatementManager.getNameForMatching(fieldNode));
	}

	public void collectMatchingClasses(GroupingsHierarchy groupingsHierarchy, HashSet<ClassNode> matchingClasses) {
		Set<ClassNode> annotationClasses = null;
		if (this.isSearchByAnnotation()) {
			boolean matchOnOriginal = Options.ZKM_MATCH_ON_ORIGINAL && (groupingsHierarchy.hasHierarchySnapshot() || groupingsHierarchy.isFieldNameObfFinish() || groupingsHierarchy.isMethodNameObfFinish());
			annotationClasses = this.findClassesByAnnotation(groupingsHierarchy.getClasses(), matchOnOriginal);
		}

		Enumeration<ClassNode> classEnumeration = groupingsHierarchy.getClasses();

		while (classEnumeration.hasMoreElements()) {
			ClassNode classNode = classEnumeration.nextElement();
			String packageName = ExcludeStatementManager.getPackageNameForMatching(classNode);
			String simpleName = ExcludeStatementManager.getSimpleNameForMatching(classNode);
			if (this.matchesClass(classNode, annotationClasses, packageName, simpleName, groupingsHierarchy)) {
				matchingClasses.add(classNode);
			}
		}
	}

	public void excludeAllMembersFromAutoReflection(AutoReflectionExcludes excludes, ClassNode classNode) {
		FieldNode[] fieldNodes = classNode.getFieldNodes();

		for (int fieldIndex = 0; fieldIndex < fieldNodes.length; fieldIndex++) {
			excludes.excludeFieldBecause(fieldNodes[fieldIndex], "exclusion parameter '" + this.ruleString + "'");
		}

		MethodNode[] methodNodes = classNode.getMethodNodes();

		for (int methodIndex = 0; methodIndex < methodNodes.length; methodIndex++) {
			MethodNode methodNode = methodNodes[methodIndex];
			if (!methodNode.isConstructor() && !methodNode.isClinit()) {
				excludes.excludeMethodBecause(methodNode, "exclusion parameter '" + this.ruleString + "'");
			}
		}
	}

	private void excludeParameterChangerMethods(ParameterChangerExcludes excludes, Enumeration<MethodNode> methods, Set<ClassNode> annotationClasses) {
		while (methods.hasMoreElements()) {
			MethodNode methodNode = methods.nextElement();
			if (this.matchesMethod(excludes, methodNode)) {
				ClassNode methodOwnerClass = methodNode.getMethodOwner();
				String packageName = ExcludeStatementManager.getPackageNameForMatching(methodOwnerClass);
				String simpleName = ExcludeStatementManager.getSimpleNameForMatching(methodOwnerClass);
				if (this.matchesClass(methodOwnerClass, annotationClasses, packageName, simpleName, excludes)) {
					excludes.includeMethod(methodNode, "exclusion parameter '" + this.ruleString + "'");
				}
			}
		}
	}

	public void includeAllMembersInAutoReflection(AutoReflectionExcludes excludes, ClassNode classNode) {
		FieldNode[] fieldNodes = classNode.getFieldNodes();

		for (int fieldIndex = 0; fieldIndex < fieldNodes.length; fieldIndex++) {
			excludes.includeFieldBecause(fieldNodes[fieldIndex], "unexclusion parameter '" + this.ruleString + "'");
		}

		MethodNode[] methodNodes = classNode.getMethodNodes();

		for (int methodIndex = 0; methodIndex < methodNodes.length; methodIndex++) {
			MethodNode methodNode = methodNodes[methodIndex];
			if (!methodNode.isConstructor() && !methodNode.isClinit()) {
				excludes.includeMethodBecause(methodNode, "unexclusion parameter '" + this.ruleString + "'");
			}
		}
	}

	public boolean matchesClass(AbstractClassNode classNode, Set<ClassNode> annotationClasses, String packageName, String simpleName, ClassHierarchy classHierarchy) {
		if (annotationClasses != null && !annotationClasses.contains(classNode)) {
			return false;
		} else if (!this.matchesClassNameLink(simpleName)) {
			return false;
		} else if (!this.matchesPackage(packageName)) {
			return false;
		} else if (!matchesAccessFlags(classNode.getAccessFlags(), this.classAccessFlags)) {
			return false;
		} else if (!this.checkSourceFileMatch(classNode)) {
			return false;
		} else if (!this.checkContainingClauseMatch(classNode, classHierarchy)) {
			return false;
		} else if (!this.matchesExtendsClause(classNode, classHierarchy)) {
			return false;
		} else {
			return !this.checkImplementsClauseMatch(classNode, classHierarchy) ? false : this.checkClassAnnotationMatch(classNode, classHierarchy);
		}
	}

	@Override
	public void addThrowsExceptionName(String exceptionName) {
		this.throwsExceptionNames.add(exceptionName);
	}

	private ClassNode findLinkedClass() {
		String fullyQualifiedClassName;
		if (this.packageSpecifier != null) {
			StringBuilder classNameBuilder = new StringBuilder();
			classNameBuilder.append(this.packageSpecifier.getPackageNamePrefix());
			classNameBuilder.append(this.classSpecifier.getClassName());
			fullyQualifiedClassName = classNameBuilder.toString();
		} else {
			fullyQualifiedClassName = this.classSpecifier.getClassName();
		}

		return ClassHierarchyNode.getWorkspaceClassNode(fullyQualifiedClassName.replace('.', '/'));
	}

	public boolean matchesPackageAndSimpleName(String className) {
		return !this.matchesClassNameLink(AbstractClassNode.getSimpleNameFromInternalName(className))
			? false
			: this.matchesPackage(AbstractClassNode.getPackageName(className));
	}

	public void setSearchByAnnotationName(String searchByAnnotationName) {
		this.searchByAnnotationName = searchByAnnotationName;
	}

	public boolean matchesPackage(String packageName) {
		return this.packageSpecifier != null ? this.packageSpecifier.matchesWildcard(packageName) : packageName.length() == 0;
	}

	public void setClassSpecifier(PatternMatcher classSpecifier) {
		this.classSpecifier = classSpecifier;
		this.primaryVisibilityModifier = null;
	}

	public boolean hasExtendsClause() {
		return this.extendsClassName != null;
	}

	private void applyFixedClassExclusion(FixedClassManager fixedClassManager, Enumeration<ClassNode> classes) {
		Set<ClassNode> annotationClasses = null;
		if (this.isSearchByAnnotation()) {
			boolean matchOnOriginal = Options.ZKM_MATCH_ON_ORIGINAL && (fixedClassManager.hasHierarchySnapshot() || fixedClassManager.isFieldNameObfFinish() || fixedClassManager.isMethodNameObfFinish());
			annotationClasses = this.findClassesByAnnotation(fixedClassManager.getWorkspaceClasses(), matchOnOriginal);
		}

		while (classes.hasMoreElements()) {
			ClassNode classNode = classes.nextElement();
			String packageName = ExcludeStatementManager.getPackageNameForMatching(classNode);
			String simpleName = ExcludeStatementManager.getSimpleNameForMatching(classNode);
			if (this.matchesClass(classNode, annotationClasses, packageName, simpleName, fixedClassManager)) {
				fixedClassManager.excludeClass(classNode, "exclusion parameter '" + this.ruleString + "'");
			}
		}
	}

	public boolean matchesMethodName(AbstractMethodNode methodNode) {
		if (!this.hasLinkMethodNamePattern()) {
			return this.methodSpecifier.matchesWildcard(ExcludeStatementManager.getNameForMatching(methodNode));
		} else {
			String nameForMatching = ExcludeStatementManager.getNameForMatching(methodNode);
			return nameForMatching.startsWith(this.linkMethodNamePrefix) && nameForMatching.length() > this.linkMethodNamePrefix.length();
		}
	}

	private void includeLongEncryptionMethods(LongEncryptionExcludes excludes, Enumeration<MethodNode> methods, Set<ClassNode> annotationClasses) {
		while (methods.hasMoreElements()) {
			MethodNode methodNode = methods.nextElement();
			if (this.matchesMethod(excludes, methodNode)) {
				ClassNode methodOwnerClass = methodNode.getMethodOwner();
				String packageName = ExcludeStatementManager.getPackageNameForMatching(methodOwnerClass);
				String simpleName = ExcludeStatementManager.getSimpleNameForMatching(methodOwnerClass);
				if (this.matchesClass(methodOwnerClass, annotationClasses, packageName, simpleName, excludes)) {
					excludes.includeMethodConstants(methodNode, "unexclusion parameter '" + this.ruleString + "'");
				}
			}
		}
	}

	public boolean matchesFieldByNameAndType(ClassHierarchy classHierarchy, AbstractClassNode classNode, String fieldName, String fieldTypeDescriptor) {
		String packageName = classNode.getPackageName();
		String simpleName = classNode.getSimpleNameInternal();
		if (!this.matchesClass(classNode, null, packageName, simpleName, classHierarchy)) {
			return false;
		} else {
			return this.isClassWildcard() ? true : this.fieldSpecifier.matchesWildcard(fieldName) && (this.fieldTypeDescriptor == null || this.fieldTypeDescriptor.equals(fieldTypeDescriptor));
		}
	}

	public final int countExplicitSpecifiers() {
		int specifierCount = 0;
		if (this.packageSpecifier != null && this.packageSpecifier.isPackageRecursive()) {
			specifierCount++;
		}

		if (this.classSpecifier != null && this.classSpecifier.isRecursive()) {
			specifierCount++;
		}

		return specifierCount;
	}

	public List<String> getPackagePatterns() {
		return (List<String>)(this.packageSpecifier != null ? this.packageSpecifier.getPackagePatterns() : new ArrayList<>());
	}

	public final boolean matchesExtendsClause(AbstractClassNode classNode, ClassHierarchy classHierarchy) {
		boolean extendsClauseMatched;
		if (this.extendsClassName != null) {
			try {
				if (classNode.isGeneratedClass()) {
					return this.extendsClassName.equals("java/lang/Object") && this.extendsAnnotationSpecifier == null;
				}

				if (this.extendsAnnotationSpecifier != null) {
					if (Options.ZKM_MATCH_ON_ORIGINAL && classHierarchy.hasHierarchySnapshot()) {
						extendsClauseMatched = classHierarchy.isOriginalSuperclassOf(classNode.getClassName(), this.extendsClassName, this.extendsAnnotationSpecifier);
					} else {
						extendsClauseMatched = classHierarchy.isInstanceOf(classNode.getInternalName(), this.extendsClassName, this.extendsAnnotationSpecifier);
					}
				} else if (Options.ZKM_MATCH_ON_ORIGINAL && classHierarchy.hasHierarchySnapshot()) {
					extendsClauseMatched = classHierarchy.isSuperclassOf(classNode.getClassName(), this.extendsClassName);
				} else {
					extendsClauseMatched = classHierarchy.isClassInstanceOf(classNode.getInternalName(), this.extendsClassName);
				}
			} catch (ClasspathException classpathException) {
				this.scriptContext
					.logWarning(
						"Error while executing \""
							+ this.getCommandName()
							+ "\" statement at line "
							+ this.getLineNumber()
							+ " while analysing '"
							+ this.ruleString
							+ "' : class '"
							+ GlobalUtils.internalToBinaryName(classpathException.getClassName())
							+ "' not found. (C)"
					);
				extendsClauseMatched = false;
			} catch (FileException fileException) {
				this.scriptContext
					.logWarning(
						"Error while executing \""
							+ this.getCommandName()
							+ "\" statement at line "
							+ this.getLineNumber()
							+ " while analysing '"
							+ this.ruleString
							+ "' : "
							+ fileException.getMessage()
							+ " (C)"
					);
				extendsClauseMatched = false;
			}
		} else {
			extendsClauseMatched = true;
		}

		return !extendsClauseMatched && this.scriptContext.isImplementsClauseMatching() && !classNode.isGeneratedClass() && !Options.dontCheckImplementsClause || !extendsClauseMatched && Options.checkImplementsClause
			? this.checkExtendsClauseFallback(classNode, classHierarchy)
			: extendsClauseMatched;
	}

	private void excludeIntegerEncryptionMethods(IntegerEncryptionExcludes excludes, Enumeration<MethodNode> methods, Set<ClassNode> annotationClasses) {
		while (methods.hasMoreElements()) {
			MethodNode methodNode = methods.nextElement();
			if (this.matchesMethod(excludes, methodNode)) {
				ClassNode methodOwnerClass = methodNode.getMethodOwner();
				String packageName = ExcludeStatementManager.getPackageNameForMatching(methodOwnerClass);
				String simpleName = ExcludeStatementManager.getSimpleNameForMatching(methodOwnerClass);
				if (this.matchesClass(methodOwnerClass, annotationClasses, packageName, simpleName, excludes)) {
					excludes.excludeMethodConstants(methodNode, "exclusion parameter '" + this.ruleString + "'");
				}
			}
		}
	}

	private void recordModifier(String modifier, Map<String, String> accessFlagStrings) {
		if (modifier.equals("public") || modifier.equals("protected") || modifier.equals("private") || modifier.equals("package")) {
			if (this.primaryVisibilityModifier == null) {
				this.primaryVisibilityModifier = modifier;
			} else if (!this.primaryVisibilityModifier.equals(modifier)) {
				this.scriptContext
					.logWarningInternal(
						"\""
							+ modifier
							+ "\" appears after \""
							+ this.primaryVisibilityModifier
							+ "\" in \""
							+ this.getCommandName()
							+ "\" statement at line "
							+ this.getLineNumber()
							+ ". \""
							+ modifier
							+ "\" ignored.",
						true
					);
				return;
			}
		}

		if (accessFlagStrings.put(modifier, modifier) != null) {
			this.scriptContext
				.logWarningInternal("\"" + modifier + "\" appears more than once in \"" + this.getCommandName() + "\" statement at line " + this.getLineNumber() + ".", true);
		}
	}

	private void includeIntegerEncryptionClasses(IntegerEncryptionExcludes excludes, Enumeration<ClassNode> classes, Set<ClassNode> annotationClasses) {
		while (classes.hasMoreElements()) {
			ClassNode classNode = classes.nextElement();
			String packageName = ExcludeStatementManager.getPackageNameForMatching(classNode);
			String simpleName = ExcludeStatementManager.getSimpleNameForMatching(classNode);
			if (this.matchesClass(classNode, annotationClasses, packageName, simpleName, excludes)) {
				excludes.includeClass(classNode, "unexclusion parameter '" + this.ruleString + "'");
			}
		}
	}

	private void includeIntegerEncryptionFields(IntegerEncryptionExcludes excludes, Enumeration<FieldNode> fields, Set<ClassNode> annotationClasses) {
		while (fields.hasMoreElements()) {
			FieldNode fieldNode = fields.nextElement();
			if (this.isFieldMatch(fieldNode, excludes)) {
				ClassNode ownerClass = fieldNode.getOwnerClass();
				String packageName = ExcludeStatementManager.getPackageNameForMatching(ownerClass);
				String simpleName = ExcludeStatementManager.getSimpleNameForMatching(ownerClass);
				if (this.matchesClass(ownerClass, annotationClasses, packageName, simpleName, excludes)) {
					excludes.includeFieldConstants(fieldNode, "unexclusion parameter '" + this.ruleString + "'");
				}
			}
		}
	}

	private void includeIntegerEncryptionMethods(IntegerEncryptionExcludes excludes, Enumeration<MethodNode> methods, Set<ClassNode> annotationClasses) {
		while (methods.hasMoreElements()) {
			MethodNode methodNode = methods.nextElement();
			if (this.matchesMethod(excludes, methodNode)) {
				ClassNode methodOwnerClass = methodNode.getMethodOwner();
				String packageName = ExcludeStatementManager.getPackageNameForMatching(methodOwnerClass);
				String simpleName = ExcludeStatementManager.getSimpleNameForMatching(methodOwnerClass);
				if (this.matchesClass(methodOwnerClass, annotationClasses, packageName, simpleName, excludes)) {
					excludes.includeMethodConstants(methodNode, "unexclusion parameter '" + this.ruleString + "'");
				}
			}
		}
	}

	public boolean isClassWildcard() {
		return this.isClassTarget() && this.classSpecifier != null && this.classSpecifier.includesMembers();
	}

	private void excludeStringEncryptionClasses(StringEncryptionExcludes stringEncryptionManager, Enumeration<ClassNode> classes, Set<ClassNode> annotationClasses) {
		while (classes.hasMoreElements()) {
			ClassNode classNode = classes.nextElement();
			String packageName = ExcludeStatementManager.getPackageNameForMatching(classNode);
			String simpleName = ExcludeStatementManager.getSimpleNameForMatching(classNode);
			if (this.matchesClass(classNode, annotationClasses, packageName, simpleName, stringEncryptionManager)) {
				stringEncryptionManager.excludeClass(classNode, "exclusion parameter '" + this.ruleString + "'");
			}
		}
	}

	public void excludeRemoveMethodCall(RemoveMethodCallExcludes excludes) {
		if (this.isEnabled) {
			Set<ClassNode> annotationClasses = null;
			if (this.isSearchByAnnotation()) {
				boolean matchOnOriginal = Options.ZKM_MATCH_ON_ORIGINAL && (excludes.hasHierarchySnapshot() || excludes.isFieldNameObfFinish() || excludes.isMethodNameObfFinish());
				annotationClasses = this.findClassesByAnnotation(excludes.getAnnotationSearchClasses(), matchOnOriginal);
			}

			switch (this.targetType) {
				case 4:
					this.excludeRemoveMethodCallMethods(excludes, excludes.getIncludedMethods(), annotationClasses);
			}
		}
	}

	private Set<ClassNode> findClassesByAnnotation(Enumeration<? extends AbstractClassNode> classes, boolean matchOnOriginal) {
		HashSet<ClassNode> annotationClasses = GlobalUtils.newHashSet();

		while (classes.hasMoreElements()) {
			Iterator<String> annotationValueIterator = classes.nextElement().findAnnotationValue(this.searchByAnnotationName, this.searchByAnnotationProperty, matchOnOriginal).iterator();

			while (annotationValueIterator.hasNext()) {
				ClassNode annotationClassNode = ClassHierarchyNode.getWorkspaceClassNode(annotationValueIterator.next());
				if (annotationClassNode != null) {
					annotationClasses.add(annotationClassNode);
				}
			}
		}

		return annotationClasses;
	}

	public boolean matchesMethodInClass(ClassHierarchy classHierarchy, AbstractClassNode classNode, String methodName, String methodDescriptor) {
		String packageName = classNode.getPackageName();
		String simpleName = classNode.getSimpleNameInternal();
		if (!this.matchesClass(classNode, null, packageName, simpleName, classHierarchy)) {
			return false;
		} else {
			return this.isClassWildcard() ? true : this.methodSpecifier.matchesWildcard(methodName) && matchesMethodDescriptor(methodDescriptor, this.methodArgumentSpecifier);
		}
	}

	public void includeRemoveMethodCall(RemoveMethodCallExcludes excludes) {
		if (this.isEnabled) {
			Set<ClassNode> annotationClasses = null;
			if (this.isSearchByAnnotation()) {
				boolean matchOnOriginal = Options.ZKM_MATCH_ON_ORIGINAL && (excludes.hasHierarchySnapshot() || excludes.isFieldNameObfFinish() || excludes.isMethodNameObfFinish());
				annotationClasses = this.findClassesByAnnotation(excludes.getAnnotationSearchClasses(), matchOnOriginal);
			}

			switch (this.targetType) {
				case 4:
					this.includeRemoveMethodCallMethods(excludes, excludes.getExcludedMethods(), annotationClasses);
			}
		}
	}

	private double calculatePackageMatchScore() {
		return this.packageSpecifier != null ? this.packageSpecifier.getMatchScore() : 1.0;
	}

	public boolean matchesClassSimpleName(String simpleName) {
		return this.classSpecifier != null && this.classSpecifier.isRecursive() ? this.classSpecifier.matchesWildcard(simpleName) : false;
	}

	private void excludeAutoReflectionFields(AutoReflectionExcludes excludes, Enumeration<FieldNode> fields, Set<ClassNode> annotationClasses) {
		while (fields.hasMoreElements()) {
			FieldNode fieldNode = fields.nextElement();
			if (this.isFieldMatch(fieldNode, excludes)) {
				ClassNode ownerClass = fieldNode.getOwnerClass();
				String packageName = ExcludeStatementManager.getPackageNameForMatching(ownerClass);
				String simpleName = ExcludeStatementManager.getSimpleNameForMatching(ownerClass);
				if (this.matchesClass(ownerClass, annotationClasses, packageName, simpleName, excludes)) {
					excludes.excludeFieldBecause(fieldNode, "exclusion parameter '" + this.ruleString + "'");
					this.excludeClass(excludes, ownerClass);
				}
			}
		}
	}

	public void setPackageSpecifier(Specifier packageSpecifier) {
		this.packageSpecifier = packageSpecifier;
	}

	public final boolean hasLinkMethodNamePattern() {
		return this.linkMethodNamePattern != null;
	}

	private void excludeRemoveMethodCallMethods(RemoveMethodCallExcludes excludes, Enumeration<AbstractMethodNode> methods, Set<ClassNode> annotationClasses) {
		while (methods.hasMoreElements()) {
			AbstractMethodNode methodNode = methods.nextElement();
			if (this.matchesMethod(excludes, methodNode)) {
				AbstractClassNode methodOwnerClass = methodNode.getClassNode();
				String packageName = ExcludeStatementManager.getPackageNameForMatching(methodOwnerClass);
				String simpleName = ExcludeStatementManager.getSimpleNameForMatching(methodOwnerClass);
				if (this.matchesClass(methodOwnerClass, annotationClasses, packageName, simpleName, excludes)) {
					if (this.containedInClause == null) {
						excludes.excludeMethod(methodNode, null, "exclusion parameter '" + this.ruleString + "'");
					} else {
						for (InstructionListNode callSite : excludes.getCallSites(methodNode)) {
							if (this.matchesContainedInMethod(excludes, callSite)) {
								excludes.excludeMethod(methodNode, callSite, "exclusion parameter '" + this.ruleString + "'");
							}
						}
					}
				}
			}
		}
	}

	public void setMethodSpecifier(ComplexMethodSpecifier methodSpecifier) {
		this.methodSpecifier = methodSpecifier;
	}

	public void setLinkMethodNamePattern(String linkMethodNamePattern) {
		this.linkMethodNamePattern = linkMethodNamePattern;
		int linkMarkerIndex = this.linkMethodNamePattern.indexOf("<link>");
		this.linkMethodNamePrefix = this.linkMethodNamePattern.substring(0, linkMarkerIndex);
	}

	private double calculateMethodMatchScore() {
		return this.hasLinkMethodNamePattern()
			? 0.05
			: this.methodSpecifier.calculateMatchScore(this.memberAccessFlags, this.methodArgumentSpecifier, this.memberAnnotationSpecifier);
	}

	private boolean isMemberFinal() {
		return this.memberAccessFlags.isPositiveFinal();
	}

	public void setIncludeSignatureClasses() {
		this.includeSignatureClasses = true;
	}

	public int determineTargetType() {
		if (this.moduleSpecifier != null) {
			return 0;
		} else if (this.linkClassNamePattern == null && this.classSpecifier == null) {
			return 1;
		} else if (this.fieldSpecifier != null) {
			return 3;
		} else if (this.linkMethodNamePattern == null && this.methodSpecifier == null) {
			return this.linkClassNamePattern == null && this.classSpecifier == null ? 99 : 2;
		} else {
			return 4;
		}
	}

	public void setEnabled(boolean enabled) {
		this.isEnabled = enabled;
	}

	private void includeAutoReflectionMethods(AutoReflectionExcludes excludes, Enumeration<MethodNode> methods, Set<ClassNode> annotationClasses) {
		while (methods.hasMoreElements()) {
			MethodNode methodNode = methods.nextElement();
			if (this.matchesMethod(excludes, methodNode)) {
				ClassNode methodOwnerClass = methodNode.getMethodOwner();
				String packageName = ExcludeStatementManager.getPackageNameForMatching(methodOwnerClass);
				String simpleName = ExcludeStatementManager.getSimpleNameForMatching(methodOwnerClass);
				if (this.matchesClass(methodOwnerClass, annotationClasses, packageName, simpleName, excludes)) {
					excludes.includeMethodBecause(methodNode, "unexclusion parameter '" + this.ruleString + "'");
					this.includeClassIfWildcard(excludes, methodOwnerClass);
				}
			}
		}
	}

	public void includeClassIfWildcard(InclusionExclusionManager inclusionManager, ClassNode classNode) {
		if (this.classSpecifier.isRecursive()) {
			inclusionManager.includeClass(classNode, "unexclusion parameter '" + this.ruleString + "'");
		}
	}

	private void excludeAutoReflectionMethods(AutoReflectionExcludes excludes, Enumeration<MethodNode> methods, Set<ClassNode> annotationClasses) {
		while (methods.hasMoreElements()) {
			MethodNode methodNode = methods.nextElement();
			if (this.matchesMethod(excludes, methodNode)) {
				ClassNode methodOwnerClass = methodNode.getMethodOwner();
				String packageName = ExcludeStatementManager.getPackageNameForMatching(methodOwnerClass);
				String simpleName = ExcludeStatementManager.getSimpleNameForMatching(methodOwnerClass);
				if (this.matchesClass(methodOwnerClass, annotationClasses, packageName, simpleName, excludes)) {
					excludes.excludeMethodBecause(methodNode, "exclusion parameter '" + this.ruleString + "'");
					this.excludeClass(excludes, methodOwnerClass);
				}
			}
		}
	}

	private void includeAutoReflectionClasses(AutoReflectionExcludes excludes, Enumeration<ClassNode> classes, Set<ClassNode> annotationClasses) {
		while (classes.hasMoreElements()) {
			ClassNode classNode = classes.nextElement();
			String packageName = ExcludeStatementManager.getPackageNameForMatching(classNode);
			String simpleName = ExcludeStatementManager.getSimpleNameForMatching(classNode);
			if (this.matchesClass(classNode, annotationClasses, packageName, simpleName, excludes)) {
				excludes.includeClass(classNode, "unexclusion parameter '" + this.ruleString + "'");
				if (this.classSpecifier != null && this.classSpecifier.includesMembers()) {
					this.includeAllMembersInAutoReflection(excludes, classNode);
				}
			}
		}
	}

	private void includeAutoReflectionFields(AutoReflectionExcludes excludes, Enumeration<FieldNode> fields, Set<ClassNode> annotationClasses) {
		while (fields.hasMoreElements()) {
			FieldNode fieldNode = fields.nextElement();
			if (this.isFieldMatch(fieldNode, excludes)) {
				ClassNode ownerClass = fieldNode.getOwnerClass();
				String packageName = ExcludeStatementManager.getPackageNameForMatching(ownerClass);
				String simpleName = ExcludeStatementManager.getSimpleNameForMatching(ownerClass);
				if (this.matchesClass(ownerClass, annotationClasses, packageName, simpleName, excludes)) {
					excludes.includeFieldBecause(fieldNode, "unexclusion parameter '" + this.ruleString + "'");
					this.includeClassIfWildcard(excludes, ownerClass);
				}
			}
		}
	}

	private void excludeAutoReflectionClasses(AutoReflectionExcludes excludes, Enumeration<ClassNode> classes, Set<ClassNode> annotationClasses) {
		while (classes.hasMoreElements()) {
			ClassNode classNode = classes.nextElement();
			String packageName = ExcludeStatementManager.getPackageNameForMatching(classNode);
			String simpleName = ExcludeStatementManager.getSimpleNameForMatching(classNode);
			if (this.matchesClass(classNode, annotationClasses, packageName, simpleName, excludes)) {
				excludes.excludeClass(classNode, "exclusion parameter '" + this.ruleString + "'");
				if (this.classSpecifier != null && this.classSpecifier.includesMembers()) {
					this.excludeAllMembersFromAutoReflection(excludes, classNode);
				}
			}
		}
	}

	public void includeStringEncryption(StringEncryptionExcludes stringEncryptionManager) {
		Set<ClassNode> annotationClasses = null;
		if (this.isSearchByAnnotation()) {
			boolean matchOnOriginal = Options.ZKM_MATCH_ON_ORIGINAL && (stringEncryptionManager.hasHierarchySnapshot() || stringEncryptionManager.isFieldNameObfFinish() || stringEncryptionManager.isMethodNameObfFinish());
			annotationClasses = this.findClassesByAnnotation(stringEncryptionManager.getWorkspaceClasses(), matchOnOriginal);
		}

		switch (this.targetType) {
			case 2:
				this.includeStringEncryptionClasses(stringEncryptionManager, stringEncryptionManager.getWorkspaceClasses(), annotationClasses);
				break;
			case 3:
				if (this.fieldSpecifier.isExactName() && stringEncryptionManager.shouldMatchCurrentFieldName()) {
					Enumeration<ClassNode> classesByFieldName = stringEncryptionManager.getClassesByFieldName(this.fieldSpecifier.toRuleString());
					if (classesByFieldName != null) {
						while (classesByFieldName.hasMoreElements()) {
							ClassNode fieldOwnerClass = classesByFieldName.nextElement();
							String fieldOwnerPackageName = ExcludeStatementManager.getPackageNameForMatching(fieldOwnerClass);
							String fieldOwnerSimpleName = ExcludeStatementManager.getSimpleNameForMatching(fieldOwnerClass);
							if (this.matchesClass(fieldOwnerClass, annotationClasses, fieldOwnerPackageName, fieldOwnerSimpleName, stringEncryptionManager)) {
								ArrayEnumeration<FieldNode> fieldEnumeration = fieldOwnerClass.getFieldEnumeration();

								while (fieldEnumeration.hasMoreElements()) {
									FieldNode fieldNode = fieldEnumeration.nextElement();
									if (this.isFieldMatch(fieldNode, stringEncryptionManager)) {
										stringEncryptionManager.includeFieldStrings(fieldNode, "unexclusion parameter '" + this.ruleString + "'");
									}
								}
							}
						}
					}
				} else {
					this.includeStringEncryptionFields(stringEncryptionManager, stringEncryptionManager.getAllFields(), annotationClasses);
				}
				break;
			case 4:
				if (this.methodSpecifier.isExactName() && stringEncryptionManager.shouldMatchCurrentMethodName()) {
					Enumeration<ClassNode> classesByMethodName = stringEncryptionManager.getClassesByMethodName(this.methodSpecifier.toRuleString());
					if (classesByMethodName != null) {
						while (classesByMethodName.hasMoreElements()) {
							ClassNode methodOwnerClass = classesByMethodName.nextElement();
							String methodOwnerPackageName = ExcludeStatementManager.getPackageNameForMatching(methodOwnerClass);
							String methodOwnerSimpleName = ExcludeStatementManager.getSimpleNameForMatching(methodOwnerClass);
							if (this.matchesClass(methodOwnerClass, annotationClasses, methodOwnerPackageName, methodOwnerSimpleName, stringEncryptionManager)) {
								ArrayEnumeration<MethodNode> methodEnumeration = methodOwnerClass.getMethodEnumeration();

								while (methodEnumeration.hasMoreElements()) {
									MethodNode methodNode = methodEnumeration.nextElement();
									if (this.matchesMethod(stringEncryptionManager, methodNode)) {
										stringEncryptionManager.includeMethodStrings(methodNode, "unexclusion parameter '" + this.ruleString + "'");
									}
								}
							}
						}
					}
				} else {
					this.includeStringEncryptionMethods(stringEncryptionManager, stringEncryptionManager.getAllMethods(), annotationClasses);
				}
		}
	}

	private boolean isMemberTransient() {
		return this.memberAccessFlags.isPositiveTransient();
	}

	public void applyRenameInclusion(DefaultExcludes excludes) {
		if (this.isEnabled) {
			Set<ClassNode> annotationClasses = null;
			if (this.isSearchByAnnotation()) {
				boolean matchOnOriginal = Options.ZKM_MATCH_ON_ORIGINAL && (excludes.hasHierarchySnapshot() || excludes.isFieldNameObfFinish() || excludes.isMethodNameObfFinish());
				annotationClasses = this.findClassesByAnnotation(excludes.getChangeLogClasses(), matchOnOriginal);
			}

			switch (this.targetType) {
				case 1:
					Enumeration<String> excludedPackageNames = excludes.getExcludedPackageNames();

					while (excludedPackageNames.hasMoreElements()) {
						String excludedPackageName = excludedPackageNames.nextElement();
						if (this.matchesPackage(excludedPackageName)) {
							excludes.unexcludePackage(excludedPackageName, "unexclusion parameter '" + this.ruleString + "'");
						}
					}
					break;
				case 2:
					if (this.classSpecifier == null || !this.classSpecifier.isExactName() || this.packageSpecifier != null && !this.packageSpecifier.isExactName()) {
						if (!this.hasExplicitPackageOrClass() && !this.isClassWildcard()) {
							this.includeRenameClasses(excludes, excludes.getExcludedClasses(), annotationClasses);
						} else {
							this.includeRenameClasses(excludes, excludes.getWorkspaceClasses(), annotationClasses);
						}
					} else {
						ClassNode linkedClass = this.findLinkedClass();
						if (linkedClass != null) {
							this.applyRenameInclusionToClass(excludes, linkedClass, annotationClasses);
						}
					}
					break;
				case 3:
					if (this.classSpecifier == null || !this.classSpecifier.isExactName() || this.packageSpecifier != null && !this.packageSpecifier.isExactName()) {
						if (this.fieldSpecifier.isExactName() && excludes.shouldMatchCurrentFieldName()) {
							Enumeration<ClassNode> classesByFieldName = excludes.getClassesByFieldName(this.fieldSpecifier.toRuleString());
							if (classesByFieldName != null) {
								while (classesByFieldName.hasMoreElements()) {
									ClassNode fieldOwnerClass = classesByFieldName.nextElement();
									String fieldOwnerPackageName = ExcludeStatementManager.getPackageNameForMatching(fieldOwnerClass);
									String fieldOwnerSimpleName = ExcludeStatementManager.getSimpleNameForMatching(fieldOwnerClass);
									if (this.matchesClass(fieldOwnerClass, annotationClasses, fieldOwnerPackageName, fieldOwnerSimpleName, excludes)) {
										ArrayEnumeration<FieldNode> fieldEnumeration = fieldOwnerClass.getFieldEnumeration();

										while (fieldEnumeration.hasMoreElements()) {
											FieldNode fieldNode = fieldEnumeration.nextElement();
											if (this.isFieldMatch(fieldNode, excludes)) {
												excludes.unexcludeField(fieldNode, "unexclusion parameter '" + this.ruleString + "'");
												this.unexcludePackage(excludes, fieldOwnerPackageName);
												this.includeClassIfWildcard(excludes, fieldOwnerClass);
											}
										}
									}
								}
							}
						} else if (this.hasExplicitPackageOrClass()) {
							this.applyRenameInclusionToFields(excludes, excludes.getAllFields(), annotationClasses);
						} else {
							this.applyRenameInclusionToFields(excludes, excludes.getExcludedFields(), annotationClasses);
						}
					} else {
						ClassNode linkedClassForFields = this.findLinkedClass();
						if (linkedClassForFields != null) {
							this.applyRenameInclusionToFields(excludes, linkedClassForFields.getFieldEnumeration(), annotationClasses);
						}
					}
					break;
				case 4:
					if (this.classSpecifier == null || !this.classSpecifier.isExactName() || this.packageSpecifier != null && !this.packageSpecifier.isExactName()) {
						if (this.methodSpecifier.isExactName() && excludes.shouldMatchCurrentMethodName()) {
							Enumeration<ClassNode> classesByMethodName = excludes.getClassesByMethodName(this.methodSpecifier.toRuleString());
							if (classesByMethodName != null) {
								while (classesByMethodName.hasMoreElements()) {
									ClassNode methodOwnerClass = classesByMethodName.nextElement();
									String methodOwnerPackageName = ExcludeStatementManager.getPackageNameForMatching(methodOwnerClass);
									String methodOwnerSimpleName = ExcludeStatementManager.getSimpleNameForMatching(methodOwnerClass);
									if (this.matchesClass(methodOwnerClass, annotationClasses, methodOwnerPackageName, methodOwnerSimpleName, excludes)) {
										ArrayEnumeration<MethodNode> methodEnumeration = methodOwnerClass.getMethodEnumeration();

										while (methodEnumeration.hasMoreElements()) {
											MethodNode methodNode = methodEnumeration.nextElement();
											if (this.matchesMethod(excludes, methodNode)) {
												excludes.unexcludeMethod(methodNode, "unexclusion parameter '" + this.ruleString + "'");
												this.unexcludePackage(excludes, methodOwnerPackageName);
												this.includeClassIfWildcard(excludes, methodOwnerClass);
												if (this.includeSignatureClasses) {
													this.unexcludeSignatureClasses(excludes, methodNode);
												}
											}
										}
									}
								}
							}
						} else if (!this.hasExplicitPackageOrClass() && !this.hasIncludeSignatureClasses()) {
							this.includeRenameMethods(excludes, excludes.getExcludedMethods(), annotationClasses);
						} else {
							this.includeRenameMethods(excludes, excludes.getAllMethods(), annotationClasses);
						}
					} else {
						ClassNode linkedClassForMethods = this.findLinkedClass();
						if (linkedClassForMethods != null) {
							this.includeRenameMethods(excludes, linkedClassForMethods.getMethodEnumeration(), annotationClasses);
						}
					}
			}
		}
	}

	public void applyRenameExclusion(DefaultExcludes excludes) {
		if (this.isEnabled) {
			Set<ClassNode> annotationClasses = null;
			if (this.isSearchByAnnotation()) {
				boolean matchOnOriginal = Options.ZKM_MATCH_ON_ORIGINAL && (excludes.hasHierarchySnapshot() || excludes.isFieldNameObfFinish() || excludes.isMethodNameObfFinish());
				annotationClasses = this.findClassesByAnnotation(excludes.getChangeLogClasses(), matchOnOriginal);
			}

			switch (this.targetType) {
				case 0:
					Enumeration<ModuleInfoNode> moduleInfoEnumeration = excludes.getProcessableModules();

					while (moduleInfoEnumeration.hasMoreElements()) {
						ModuleInfoNode moduleInfoNode = moduleInfoEnumeration.nextElement();
						if (this.isModuleMatch(moduleInfoNode)) {
							excludes.excludeModule(moduleInfoNode, "exclusion parameter '" + this.ruleString + "'");
						}
					}
					break;
				case 1:
					Enumeration<String> packageNameEnumeration = excludes.getProcessablePackageNames();

					while (packageNameEnumeration.hasMoreElements()) {
						String packageName = packageNameEnumeration.nextElement();
						if (this.matchesPackage(packageName)) {
							excludes.excludePackage(packageName, "exclusion parameter '" + this.ruleString + "'");
						}
					}
					break;
				case 2:
					if (this.classSpecifier == null || !this.classSpecifier.isExactName() || this.packageSpecifier != null && !this.packageSpecifier.isExactName()) {
						if (!this.hasExplicitPackageOrClass() && !this.isClassWildcard() && !this.isLinkClass()) {
							this.excludeRenameClasses(excludes, excludes.getProcessableClasses(), annotationClasses);
						} else {
							this.excludeRenameClasses(excludes, excludes.getWorkspaceClasses(), annotationClasses);
						}
					} else {
						ClassNode linkedClassForClasses = this.findLinkedClass();
						if (linkedClassForClasses != null) {
							this.applyRenameExclusionToClass(excludes, linkedClassForClasses, annotationClasses);
						}
					}
					break;
				case 3:
					if (this.classSpecifier == null || !this.classSpecifier.isExactName() || this.packageSpecifier != null && !this.packageSpecifier.isExactName()) {
						if (this.fieldSpecifier.isExactName() && excludes.shouldMatchCurrentFieldName()) {
							Enumeration<ClassNode> classesByFieldName = excludes.getClassesByFieldName(this.fieldSpecifier.toRuleString());
							if (classesByFieldName != null) {
								while (classesByFieldName.hasMoreElements()) {
									ClassNode fieldOwnerClass = classesByFieldName.nextElement();
									String fieldExcludedPackageName = fieldOwnerClass.getPackageName();
									String fieldOwnerPackageName = ExcludeStatementManager.getPackageNameForMatching(fieldOwnerClass);
									String fieldOwnerSimpleName = ExcludeStatementManager.getSimpleNameForMatching(fieldOwnerClass);
									if (this.matchesClass(fieldOwnerClass, annotationClasses, fieldOwnerPackageName, fieldOwnerSimpleName, excludes)) {
										ArrayEnumeration<FieldNode> fieldEnumeration = fieldOwnerClass.getFieldEnumeration();

										while (fieldEnumeration.hasMoreElements()) {
											FieldNode fieldNode = fieldEnumeration.nextElement();
											if (this.isFieldMatch(fieldNode, excludes)) {
												excludes.excludeField(fieldNode, "exclusion parameter '" + this.ruleString + "'");
												this.excludePackage(excludes, fieldExcludedPackageName);
												this.excludeClass(excludes, fieldOwnerClass);
											}
										}
									}
								}
							}
						} else if (this.hasExplicitPackageOrClass()) {
							this.applyRenameExclusionToFields(excludes, excludes.getAllFields(), annotationClasses);
						} else {
							this.applyRenameExclusionToFields(excludes, excludes.getProcessableFields(), annotationClasses);
						}
					} else {
						ClassNode linkedClassForFields = this.findLinkedClass();
						if (linkedClassForFields != null) {
							this.applyRenameExclusionToFields(excludes, linkedClassForFields.getFieldEnumeration(), annotationClasses);
						}
					}
					break;
				case 4:
					if (this.classSpecifier == null || !this.classSpecifier.isExactName() || this.packageSpecifier != null && !this.packageSpecifier.isExactName()) {
						if (this.hasLinkMethodNamePattern()) {
							this.excludeRenameMethods(excludes, excludes.getAllMethods(), annotationClasses);
						} else if (this.methodSpecifier.isExactName() && excludes.shouldMatchCurrentMethodName()) {
							Enumeration<ClassNode> classesByMethodName = excludes.getClassesByMethodName(this.methodSpecifier.toRuleString());
							if (classesByMethodName != null) {
								while (classesByMethodName.hasMoreElements()) {
									ClassNode methodOwnerClass = classesByMethodName.nextElement();
									String methodExcludedPackageName = methodOwnerClass.getPackageName();
									String methodOwnerPackageName = ExcludeStatementManager.getPackageNameForMatching(methodOwnerClass);
									String methodOwnerSimpleName = ExcludeStatementManager.getSimpleNameForMatching(methodOwnerClass);
									if (this.matchesClass(methodOwnerClass, annotationClasses, methodOwnerPackageName, methodOwnerSimpleName, excludes)) {
										ArrayEnumeration<MethodNode> methodEnumeration = methodOwnerClass.getMethodEnumeration();

										while (methodEnumeration.hasMoreElements()) {
											MethodNode methodNode = methodEnumeration.nextElement();
											if (this.matchesMethod(excludes, methodNode)) {
												excludes.excludeMethod(methodNode, "exclusion parameter '" + this.ruleString + "'");
												this.excludePackage(excludes, methodExcludedPackageName);
												this.excludeClass(excludes, methodOwnerClass);
												if (this.includeSignatureClasses) {
													this.excludeSignatureClasses(excludes, methodNode);
												}
											}
										}
									}
								}
							}
						} else if (!this.hasExplicitPackageOrClass() && !this.hasIncludeSignatureClasses()) {
							this.excludeRenameMethods(excludes, excludes.getProcessableMethods(), annotationClasses);
						} else {
							this.excludeRenameMethods(excludes, excludes.getAllMethods(), annotationClasses);
						}
					} else {
						ClassNode linkedClassForMethods = this.findLinkedClass();
						if (linkedClassForMethods != null) {
							this.excludeRenameMethods(excludes, linkedClassForMethods.getMethodEnumeration(), annotationClasses);
						}
					}
			}
		}
	}

	@Override
	public int compareTo(RenameFilterParameter other) {
		if (this.priority < other.priority) {
			return -1;
		} else {
			return this.priority == other.priority ? 0 : 1;
		}
	}

	public boolean hasContainedInSignatureClasses() {
		return this.containedInClause != null && this.containedInClause.getFilterParameter() != null && this.containedInClause.getFilterParameter().includeSignatureClasses;
	}

	private void excludeIntegerEncryptionClasses(IntegerEncryptionExcludes excludes, Enumeration<ClassNode> classes, Set<ClassNode> annotationClasses) {
		while (classes.hasMoreElements()) {
			ClassNode classNode = classes.nextElement();
			String packageName = ExcludeStatementManager.getPackageNameForMatching(classNode);
			String simpleName = ExcludeStatementManager.getSimpleNameForMatching(classNode);
			if (this.matchesClass(classNode, annotationClasses, packageName, simpleName, excludes)) {
				excludes.excludeClass(classNode, "exclusion parameter '" + this.ruleString + "'");
			}
		}
	}

	public boolean hasIncludeSignatureClasses() {
		return this.includeSignatureClasses;
	}

	public boolean matchesClassNameLink(String simpleName) {
		return !this.isLinkClass()
			? this.classSpecifier.matchesWildcard(simpleName)
			: simpleName.endsWith(this.linkClassNameSuffix)
				&& (this.linkClassNamePrefix.length() == 0 || simpleName.startsWith(this.linkClassNamePrefix))
				&& simpleName.length() > this.linkClassNamePrefix.length() + this.linkClassNameSuffix.length();
	}

	private void excludeFlowObfuscationMethods(FlowObfuscateExclude excludes, Enumeration<MethodNode> methods, Set<ClassNode> annotationClasses) {
		while (methods.hasMoreElements()) {
			MethodNode methodNode = methods.nextElement();
			if (this.matchesMethod(excludes, methodNode)) {
				ClassNode ownerClass = methodNode.getMethodOwner();
				String packageName = ExcludeStatementManager.getPackageNameForMatching(ownerClass);
				String simpleName = ExcludeStatementManager.getSimpleNameForMatching(ownerClass);
				if (this.matchesClass(ownerClass, annotationClasses, packageName, simpleName, excludes)) {
					excludes.excludeMethod(methodNode, "exclusion parameter '" + this.ruleString + "'");
				}
			}
		}
	}

	private void includeFlowObfuscationClasses(FlowObfuscateExclude excludes, Enumeration<ClassNode> classes, Set<ClassNode> annotationClasses) {
		while (classes.hasMoreElements()) {
			ClassNode classNode = classes.nextElement();
			String packageName = ExcludeStatementManager.getPackageNameForMatching(classNode);
			String simpleName = ExcludeStatementManager.getSimpleNameForMatching(classNode);
			if (this.matchesClass(classNode, annotationClasses, packageName, simpleName, excludes)) {
				excludes.includeClass(classNode, "unexclusion parameter '" + this.ruleString + "'");
			}
		}
	}

	private void excludeFlowObfuscationClasses(FlowObfuscateExclude excludes, Enumeration<ClassNode> classes, Set<ClassNode> annotationClasses) {
		while (classes.hasMoreElements()) {
			ClassNode classNode = classes.nextElement();
			String packageName = ExcludeStatementManager.getPackageNameForMatching(classNode);
			String simpleName = ExcludeStatementManager.getSimpleNameForMatching(classNode);
			if (this.matchesClass(classNode, annotationClasses, packageName, simpleName, excludes)) {
				excludes.excludeClass(classNode, "exclusion parameter '" + this.ruleString + "'");
			}
		}
	}

	private void includeFlowObfuscationMethods(FlowObfuscateExclude excludes, Enumeration<MethodNode> methods, Set<ClassNode> annotationClasses) {
		while (methods.hasMoreElements()) {
			MethodNode methodNode = methods.nextElement();
			if (this.matchesMethod(excludes, methodNode)) {
				ClassNode ownerClass = methodNode.getMethodOwner();
				String packageName = ExcludeStatementManager.getPackageNameForMatching(ownerClass);
				String simpleName = ExcludeStatementManager.getSimpleNameForMatching(ownerClass);
				if (this.matchesClass(ownerClass, annotationClasses, packageName, simpleName, excludes)) {
					excludes.includeMethod(methodNode, "unexclusion parameter '" + this.ruleString + "'");
				}
			}
		}
	}

	private void includeFlowObfuscationFields(FlowObfuscateExclude excludes, Enumeration<ClassNode> classes, Set<ClassNode> annotationClasses) {
		while (classes.hasMoreElements()) {
			ClassNode classNode = classes.nextElement();
			String packageName = ExcludeStatementManager.getPackageNameForMatching(classNode);
			String simpleName = ExcludeStatementManager.getSimpleNameForMatching(classNode);
			if (this.matchesClass(classNode, annotationClasses, packageName, simpleName, excludes)) {
				excludes.includeClassForFlowFields(classNode, "unexclusion parameter '" + this.ruleString + "'");
			}
		}
	}

	private void excludeFlowObfuscationFields(FlowObfuscateExclude excludes, Enumeration<ClassNode> classes, Set<ClassNode> annotationClasses) {
		while (classes.hasMoreElements()) {
			ClassNode classNode = classes.nextElement();
			String packageName = ExcludeStatementManager.getPackageNameForMatching(classNode);
			String simpleName = ExcludeStatementManager.getSimpleNameForMatching(classNode);
			if (this.matchesClass(classNode, annotationClasses, packageName, simpleName, excludes)) {
				excludes.excludeClassForFlowFields(classNode, "exclusion parameter '" + this.ruleString + "'");
			}
		}
	}

	private void excludeStringEncryptionFields(StringEncryptionExcludes stringEncryptionManager, Enumeration<FieldNode> fields, Set<ClassNode> annotationClasses) {
		while (fields.hasMoreElements()) {
			FieldNode fieldNode = fields.nextElement();
			if (this.isFieldMatch(fieldNode, stringEncryptionManager)) {
				ClassNode ownerClass = fieldNode.getOwnerClass();
				String packageName = ExcludeStatementManager.getPackageNameForMatching(ownerClass);
				String simpleName = ExcludeStatementManager.getSimpleNameForMatching(ownerClass);
				if (this.matchesClass(ownerClass, annotationClasses, packageName, simpleName, stringEncryptionManager)) {
					stringEncryptionManager.excludeFieldStrings(fieldNode, "exclusion parameter '" + this.ruleString + "'");
				}
			}
		}
	}

	public boolean isMethodTarget() {
		return this.targetType == 4;
	}

	protected int calculatePriority() {
		int priorityScore = 1000;
		switch (this.targetType) {
			case 0:
				priorityScore = (int)(1000.0 * this.moduleSpecifier.calculateModuleMatchScore());
				break;
			case 1:
				priorityScore = (int)(1000.0 * this.calculatePackageMatchScore());
				break;
			case 2:
				priorityScore = (int)((int)(1000.0 * this.calculatePackageMatchScore()) * this.calculateClassMatchScore());
				break;
			case 3:
				priorityScore = (int)((int)((int)(1000.0 * this.calculatePackageMatchScore()) * this.calculateClassMatchScore()) * this.calculateFieldMatchScore());
				break;
			case 4:
				priorityScore = (int)((int)((int)(1000.0 * this.calculatePackageMatchScore()) * this.calculateClassMatchScore()) * this.calculateMethodMatchScore());
		}

		return priorityScore + this.countExplicitSpecifiers() * 1001;
	}

	public void includeAllMembersInRename(DefaultExcludes excludes, ClassNode classNode) {
		FieldNode[] fieldNodes = classNode.getFieldNodes();

		for (int fieldIndex = 0; fieldIndex < fieldNodes.length; fieldIndex++) {
			excludes.unexcludeField(fieldNodes[fieldIndex], "unexclusion parameter '" + this.ruleString + "'");
		}

		MethodNode[] methodNodes = classNode.getMethodNodes();

		for (int methodIndex = 0; methodIndex < methodNodes.length; methodIndex++) {
			MethodNode methodNode = methodNodes[methodIndex];
			if (!methodNode.isConstructor() && !methodNode.isClinit()) {
				excludes.unexcludeMethod(methodNode, "unexclusion parameter '" + this.ruleString + "'");
			}
		}
	}

	@Override
	public final boolean equals(Object obj) {
		if (obj instanceof RenameFilterParameter) {
			RenameFilterParameter otherParameter = (RenameFilterParameter)obj;
			return this.ruleString.equals(otherParameter.ruleString);
		} else {
			return false;
		}
	}

	public void excludeAllMembersFromRename(DefaultExcludes excludes, ClassNode classNode) {
		FieldNode[] fieldNodes = classNode.getFieldNodes();

		for (int fieldIndex = 0; fieldIndex < fieldNodes.length; fieldIndex++) {
			excludes.excludeField(fieldNodes[fieldIndex], "exclusion parameter '" + this.ruleString + "'");
		}

		MethodNode[] methodNodes = classNode.getMethodNodes();

		for (int methodIndex = 0; methodIndex < methodNodes.length; methodIndex++) {
			MethodNode methodNode = methodNodes[methodIndex];
			if (!methodNode.isConstructor() && !methodNode.isClinit()) {
				excludes.excludeMethod(methodNode, "exclusion parameter '" + this.ruleString + "'");
			}
		}
	}

	private void includeTrimAnnotationTypes(TrimExcludes excludes, Enumeration<AbstractClassNode> annotationTypes, Set<ClassNode> annotationClasses) {
		while (annotationTypes.hasMoreElements()) {
			AbstractClassNode annotationType = annotationTypes.nextElement();
			String packageName = annotationType.getPackageName();
			String simpleName = annotationType.getSimpleNameInternal();
			if (this.matchesClass(annotationType, annotationClasses, packageName, simpleName, excludes)) {
				excludes.includeAnnotationType(annotationType, "unexclusion parameter '" + this.ruleString + "'");
			}
		}
	}

	private void excludeTrimLinkClasses(TrimExcludes excludes, Enumeration<ClassNode> classes, Set<ClassNode> annotationClasses) {
		HashMap<String, ClassNode> linkedClassesByBaseName = GlobalUtils.newHashMap();

		while (classes.hasMoreElements()) {
			ClassNode classNode = classes.nextElement();
			String packageName = ExcludeStatementManager.getPackageNameForMatching(classNode);
			String simpleName = ExcludeStatementManager.getSimpleNameForMatching(classNode);
			if (this.matchesClass(classNode, annotationClasses, packageName, simpleName, excludes)) {
				excludes.excludeClass(classNode, "exclusion parameter '" + this.ruleString + "'");
				String internalName = classNode.getInternalName();
				if (simpleName.length() > this.linkClassNameSuffix.length()) {
					String baseName = internalName.substring(0, internalName.length() - this.linkClassNameSuffix.length());
					linkedClassesByBaseName.put(baseName, classNode);
				}
			}
		}

		classes = excludes.getWorkspaceClasses();

		while (classes.hasMoreElements()) {
			ClassNode workspaceClassNode = classes.nextElement();
			String workspaceClassInternalName = workspaceClassNode.getInternalName();
			if (linkedClassesByBaseName.containsKey(workspaceClassInternalName)) {
				ClassNode linkedClassNode = linkedClassesByBaseName.get(workspaceClassInternalName);
				excludes.excludeClass(workspaceClassNode, "exclusion parameter '" + this.ruleString + "' matching class '" + linkedClassNode.getBinaryName() + "'");
			}
		}
	}

	private void includeExceptionObfuscationClasses(ObfuscateExceptionExclude excludes, Enumeration<ClassNode> classes, Set<ClassNode> annotationClasses) {
		while (classes.hasMoreElements()) {
			ClassNode classNode = classes.nextElement();
			String packageName = ExcludeStatementManager.getPackageNameForMatching(classNode);
			String simpleName = ExcludeStatementManager.getSimpleNameForMatching(classNode);
			if (this.matchesClass(classNode, annotationClasses, packageName, simpleName, excludes)) {
				excludes.includeClass(classNode, "unexclusion parameter '" + this.ruleString + "'");
			}
		}
	}

	private void applyTrimInclusionToFields(TrimExcludes excludes, Enumeration<FieldNode> fields, Set<ClassNode> annotationClasses) {
		while (fields.hasMoreElements()) {
			FieldNode fieldNode = fields.nextElement();
			if (this.isFieldMatch(fieldNode, excludes)) {
				ClassNode ownerClass = fieldNode.getOwnerClass();
				String packageName = ExcludeStatementManager.getPackageNameForMatching(ownerClass);
				String simpleName = ExcludeStatementManager.getSimpleNameForMatching(ownerClass);
				if (this.matchesClass(ownerClass, annotationClasses, packageName, simpleName, excludes)) {
					excludes.unexcludeField(fieldNode, "unexclusion parameter '" + this.ruleString + "'");
					this.warnPackageExclusionIgnored(excludes, packageName);
					this.includeClassIfWildcard(excludes, ownerClass);
				}
			}
		}
	}

	private void excludeTrimAnnotationTypes(TrimExcludes excludes, Enumeration<AbstractClassNode> annotationTypes, Set<ClassNode> annotationClasses) {
		while (annotationTypes.hasMoreElements()) {
			AbstractClassNode annotationType = annotationTypes.nextElement();
			String packageName = annotationType.getPackageName();
			String simpleName = annotationType.getSimpleNameInternal();
			if (this.matchesClass(annotationType, annotationClasses, packageName, simpleName, excludes)) {
				excludes.excludeAnnotationType(annotationType, "exclusion parameter '" + this.ruleString + "'");
			}
		}
	}

	private void includeExceptionObfuscationMethods(ObfuscateExceptionExclude excludes, Enumeration<MethodNode> methods, Set<ClassNode> annotationClasses) {
		while (methods.hasMoreElements()) {
			MethodNode methodNode = methods.nextElement();
			if (this.matchesMethod(excludes, methodNode)) {
				ClassNode ownerClass = methodNode.getMethodOwner();
				String packageName = ExcludeStatementManager.getPackageNameForMatching(ownerClass);
				String simpleName = ExcludeStatementManager.getSimpleNameForMatching(ownerClass);
				if (this.matchesClass(ownerClass, annotationClasses, packageName, simpleName, excludes)) {
					excludes.includeMethod(methodNode, "unexclusion parameter '" + this.ruleString + "'");
				}
			}
		}
	}

	private void applyTrimExclusionToClasses(TrimExcludes excludes, Enumeration<ClassNode> classes, Set<ClassNode> annotationClasses) {
		while (classes.hasMoreElements()) {
			ClassNode classNode = classes.nextElement();
			this.applyTrimExclusionToClass(excludes, classNode, annotationClasses);
		}
	}

	private void applyTrimInclusionToClasses(TrimExcludes excludes, Enumeration<ClassNode> classes, Set<ClassNode> annotationClasses) {
		while (classes.hasMoreElements()) {
			ClassNode classNode = classes.nextElement();
			String packageName = ExcludeStatementManager.getPackageNameForMatching(classNode);
			String simpleName = ExcludeStatementManager.getSimpleNameForMatching(classNode);
			if (this.matchesClass(classNode, annotationClasses, packageName, simpleName, excludes)) {
				excludes.includeClass(classNode, "unexclusion parameter '" + this.ruleString + "'");
				if (this.classSpecifier.includesMembers()) {
					this.includeAllMembersInTrim(excludes, classNode);
				}
			}
		}
	}

	private void excludeExceptionObfuscationMethods(ObfuscateExceptionExclude excludes, Enumeration<MethodNode> methods, Set<ClassNode> annotationClasses) {
		while (methods.hasMoreElements()) {
			MethodNode methodNode = methods.nextElement();
			if (this.matchesMethod(excludes, methodNode)) {
				ClassNode ownerClass = methodNode.getMethodOwner();
				String packageName = ExcludeStatementManager.getPackageNameForMatching(ownerClass);
				String simpleName = ExcludeStatementManager.getSimpleNameForMatching(ownerClass);
				if (this.matchesClass(ownerClass, annotationClasses, packageName, simpleName, excludes)) {
					excludes.excludeMethod(methodNode, "exclusion parameter '" + this.ruleString + "'");
				}
			}
		}
	}

	public boolean isFieldTarget() {
		return this.targetType == 3;
	}

	public final boolean checkContainingClauseMatch(AbstractClassNode classNode, ClassHierarchy classHierarchy) {
		return this.containingClause != null ? this.containingClause.matchesClass(classNode, classHierarchy) : true;
	}

	private void includeStringEncryptionClasses(StringEncryptionExcludes stringEncryptionManager, Enumeration<ClassNode> classes, Set<ClassNode> annotationClasses) {
		while (classes.hasMoreElements()) {
			ClassNode classNode = classes.nextElement();
			String packageName = ExcludeStatementManager.getPackageNameForMatching(classNode);
			String simpleName = ExcludeStatementManager.getSimpleNameForMatching(classNode);
			if (this.matchesClass(classNode, annotationClasses, packageName, simpleName, stringEncryptionManager)) {
				stringEncryptionManager.includeClass(classNode, "unexclusion parameter '" + this.ruleString + "'");
			}
		}
	}

	private void excludeStringEncryptionMethods(StringEncryptionExcludes stringEncryptionManager, Enumeration<MethodNode> methods, Set<ClassNode> annotationClasses) {
		while (methods.hasMoreElements()) {
			MethodNode methodNode = methods.nextElement();
			if (this.matchesMethod(stringEncryptionManager, methodNode)) {
				ClassNode ownerClass = methodNode.getMethodOwner();
				String packageName = ExcludeStatementManager.getPackageNameForMatching(ownerClass);
				String simpleName = ExcludeStatementManager.getSimpleNameForMatching(ownerClass);
				if (this.matchesClass(ownerClass, annotationClasses, packageName, simpleName, stringEncryptionManager)) {
					stringEncryptionManager.excludeMethodStrings(methodNode, "exclusion parameter '" + this.ruleString + "'");
				}
			}
		}
	}

	private void includeStringEncryptionFields(StringEncryptionExcludes stringEncryptionManager, Enumeration<FieldNode> fields, Set<ClassNode> annotationClasses) {
		while (fields.hasMoreElements()) {
			FieldNode fieldNode = fields.nextElement();
			if (this.isFieldMatch(fieldNode, stringEncryptionManager)) {
				ClassNode ownerClass = fieldNode.getOwnerClass();
				String packageName = ExcludeStatementManager.getPackageNameForMatching(ownerClass);
				String simpleName = ExcludeStatementManager.getSimpleNameForMatching(ownerClass);
				if (this.matchesClass(ownerClass, annotationClasses, packageName, simpleName, stringEncryptionManager)) {
					stringEncryptionManager.includeFieldStrings(fieldNode, "unexclusion parameter '" + this.ruleString + "'");
				}
			}
		}
	}

	private void includeStringEncryptionMethods(StringEncryptionExcludes stringEncryptionManager, Enumeration<MethodNode> methods, Set<ClassNode> annotationClasses) {
		while (methods.hasMoreElements()) {
			MethodNode methodNode = methods.nextElement();
			if (this.matchesMethod(stringEncryptionManager, methodNode)) {
				ClassNode ownerClass = methodNode.getMethodOwner();
				String packageName = ExcludeStatementManager.getPackageNameForMatching(ownerClass);
				String simpleName = ExcludeStatementManager.getSimpleNameForMatching(ownerClass);
				if (this.matchesClass(ownerClass, annotationClasses, packageName, simpleName, stringEncryptionManager)) {
					stringEncryptionManager.includeMethodStrings(methodNode, "unexclusion parameter '" + this.ruleString + "'");
				}
			}
		}
	}

	private void logClassAndMembersWarning() {
		if (this.classSpecifier != null && this.classSpecifier.includesMembers()) {
			if (this.targetType == 3) {
				this.logClassAndMembersWarning("field");
			} else if (this.targetType == 4) {
				this.logClassAndMembersWarning("method");
			}
		}
	}

	@Override
	public final int hashCode() {
		return this.ruleString.hashCode();
	}

	public void applyLongEncryptionExclusion(LongEncryptionExcludes excludes) {
		Set<ClassNode> annotationClasses = null;
		if (this.isSearchByAnnotation()) {
			boolean matchOnOriginal = Options.ZKM_MATCH_ON_ORIGINAL && (excludes.hasHierarchySnapshot() || excludes.isFieldNameObfFinish() || excludes.isMethodNameObfFinish());
			annotationClasses = this.findClassesByAnnotation(excludes.getWorkspaceClasses(), matchOnOriginal);
		}

		switch (this.targetType) {
			case 2:
				this.excludeLongEncryptionClasses(excludes, excludes.getProcessableClasses(), annotationClasses);
				break;
			case 3:
				if (this.fieldSpecifier.isExactName() && excludes.shouldMatchCurrentFieldName()) {
					Enumeration<ClassNode> classesByFieldName = excludes.getClassesByFieldName(this.fieldSpecifier.toRuleString());
					if (classesByFieldName != null) {
						while (classesByFieldName.hasMoreElements()) {
							ClassNode fieldOwnerClass = classesByFieldName.nextElement();
							String fieldOwnerPackageName = ExcludeStatementManager.getPackageNameForMatching(fieldOwnerClass);
							String fieldOwnerSimpleName = ExcludeStatementManager.getSimpleNameForMatching(fieldOwnerClass);
							if (this.matchesClass(fieldOwnerClass, annotationClasses, fieldOwnerPackageName, fieldOwnerSimpleName, excludes)) {
								ArrayEnumeration<FieldNode> fieldEnumeration = fieldOwnerClass.getFieldEnumeration();

								while (fieldEnumeration.hasMoreElements()) {
									FieldNode fieldNode = fieldEnumeration.nextElement();
									if (this.isFieldMatch(fieldNode, excludes)) {
										excludes.excludeFieldConstants(fieldNode, "exclusion parameter '" + this.ruleString + "'");
									}
								}
							}
						}
					}
				} else {
					this.excludeLongEncryptionFields(excludes, excludes.getProcessableFields(), annotationClasses);
				}
				break;
			case 4:
				if (this.methodSpecifier.isExactName() && excludes.shouldMatchCurrentMethodName()) {
					Enumeration<ClassNode> classesByMethodName = excludes.getClassesByMethodName(this.methodSpecifier.toRuleString());
					if (classesByMethodName != null) {
						while (classesByMethodName.hasMoreElements()) {
							ClassNode methodOwnerClass = classesByMethodName.nextElement();
							String methodOwnerPackageName = ExcludeStatementManager.getPackageNameForMatching(methodOwnerClass);
							String methodOwnerSimpleName = ExcludeStatementManager.getSimpleNameForMatching(methodOwnerClass);
							if (this.matchesClass(methodOwnerClass, annotationClasses, methodOwnerPackageName, methodOwnerSimpleName, excludes)) {
								ArrayEnumeration<MethodNode> methodEnumeration = methodOwnerClass.getMethodEnumeration();

								while (methodEnumeration.hasMoreElements()) {
									MethodNode methodNode = methodEnumeration.nextElement();
									if (this.matchesMethod(excludes, methodNode)) {
										excludes.excludeMethodConstants(methodNode, "exclusion parameter '" + this.ruleString + "'");
									}
								}
							}
						}
					}
				} else {
					this.excludeLongEncryptionMethods(excludes, excludes.getProcessableMethods(), annotationClasses);
				}
		}
	}

	public final boolean checkExtendsClauseFallback(AbstractClassNode classNode, ClassHierarchy classHierarchy) {
		boolean implementsClauseMatched = false;

		try {
			if (this.extendsAnnotationSpecifier != null) {
				implementsClauseMatched = classHierarchy.isInterfaceImplementedBy(classNode.getInternalName(), this.extendsClassName, this.extendsAnnotationSpecifier);
			} else if (Options.ZKM_MATCH_ON_ORIGINAL && classHierarchy.hasHierarchySnapshot()) {
				implementsClauseMatched = classHierarchy.isAssignableFrom(classNode.getClassName(), this.extendsClassName);
			} else {
				implementsClauseMatched = classHierarchy.isSubclassOf(classNode.getInternalName(), this.extendsClassName);
			}
		} catch (ClasspathException classpathException) {
			this.scriptContext
				.logWarning(
					"Error while executing \""
						+ this.getCommandName()
						+ "\" statement at line "
						+ this.getLineNumber()
						+ " while analysing '"
						+ this.ruleString
						+ "' : class '"
						+ GlobalUtils.internalToBinaryName(classpathException.getClassName())
						+ "' not found. (G)"
				);
		} catch (FileException fileException) {
			this.scriptContext
				.logWarning(
					"Error while executing \""
						+ this.getCommandName()
						+ "\" statement at line "
						+ this.getLineNumber()
						+ " while analysing '"
						+ this.ruleString
						+ "' : '"
						+ fileException.getMessage()
						+ "' (G)"
				);
		}

		return implementsClauseMatched;
	}

	public void applyLongEncryptionInclusion(LongEncryptionExcludes excludes) {
		Set<ClassNode> annotationClasses = null;
		if (this.isSearchByAnnotation()) {
			boolean matchOnOriginal = Options.ZKM_MATCH_ON_ORIGINAL && (excludes.hasHierarchySnapshot() || excludes.isFieldNameObfFinish() || excludes.isMethodNameObfFinish());
			annotationClasses = this.findClassesByAnnotation(excludes.getWorkspaceClasses(), matchOnOriginal);
		}

		switch (this.targetType) {
			case 2:
				this.includeLongEncryptionClasses(excludes, excludes.getWorkspaceClasses(), annotationClasses);
				break;
			case 3:
				if (this.fieldSpecifier.isExactName() && excludes.shouldMatchCurrentFieldName()) {
					Enumeration<ClassNode> classesByFieldName = excludes.getClassesByFieldName(this.fieldSpecifier.toRuleString());
					if (classesByFieldName != null) {
						while (classesByFieldName.hasMoreElements()) {
							ClassNode fieldOwnerClass = classesByFieldName.nextElement();
							String fieldOwnerPackageName = ExcludeStatementManager.getPackageNameForMatching(fieldOwnerClass);
							String fieldOwnerSimpleName = ExcludeStatementManager.getSimpleNameForMatching(fieldOwnerClass);
							if (this.matchesClass(fieldOwnerClass, annotationClasses, fieldOwnerPackageName, fieldOwnerSimpleName, excludes)) {
								ArrayEnumeration<FieldNode> fieldEnumeration = fieldOwnerClass.getFieldEnumeration();

								while (fieldEnumeration.hasMoreElements()) {
									FieldNode fieldNode = fieldEnumeration.nextElement();
									if (this.isFieldMatch(fieldNode, excludes)) {
										excludes.includeFieldConstants(fieldNode, "unexclusion parameter '" + this.ruleString + "'");
									}
								}
							}
						}
					}
				} else {
					this.includeLongEncryptionFields(excludes, excludes.getAllFields(), annotationClasses);
				}
				break;
			case 4:
				if (this.methodSpecifier.isExactName() && excludes.shouldMatchCurrentMethodName()) {
					Enumeration<ClassNode> classesByMethodName = excludes.getClassesByMethodName(this.methodSpecifier.toRuleString());
					if (classesByMethodName != null) {
						while (classesByMethodName.hasMoreElements()) {
							ClassNode methodOwnerClass = classesByMethodName.nextElement();
							String methodOwnerPackageName = ExcludeStatementManager.getPackageNameForMatching(methodOwnerClass);
							String methodOwnerSimpleName = ExcludeStatementManager.getSimpleNameForMatching(methodOwnerClass);
							if (this.matchesClass(methodOwnerClass, annotationClasses, methodOwnerPackageName, methodOwnerSimpleName, excludes)) {
								ArrayEnumeration<MethodNode> methodEnumeration = methodOwnerClass.getMethodEnumeration();

								while (methodEnumeration.hasMoreElements()) {
									MethodNode methodNode = methodEnumeration.nextElement();
									if (this.matchesMethod(excludes, methodNode)) {
										excludes.includeMethodConstants(methodNode, "unexclusion parameter '" + this.ruleString + "'");
									}
								}
							}
						}
					}
				} else {
					this.includeLongEncryptionMethods(excludes, excludes.getAllMethods(), annotationClasses);
				}
		}
	}

	private boolean isMemberNative() {
		return this.memberAccessFlags.isPositiveNative();
	}

	private void excludeIntegerEncryptionFields(IntegerEncryptionExcludes excludes, Enumeration<FieldNode> fields, Set<ClassNode> annotationClasses) {
		while (fields.hasMoreElements()) {
			FieldNode fieldNode = fields.nextElement();
			if (this.isFieldMatch(fieldNode, excludes)) {
				ClassNode ownerClass = fieldNode.getOwnerClass();
				String packageName = ExcludeStatementManager.getPackageNameForMatching(ownerClass);
				String simpleName = ExcludeStatementManager.getSimpleNameForMatching(ownerClass);
				if (this.matchesClass(ownerClass, annotationClasses, packageName, simpleName, excludes)) {
					excludes.excludeFieldConstants(fieldNode, "exclusion parameter '" + this.ruleString + "'");
				}
			}
		}
	}

	private boolean matchesContainedInMethod(ClassHierarchy classHierarchy, InstructionListNode instructionNode) {
		RenameFilterParameter containedFilter;
		if (this.containedInClause != null && (containedFilter = this.containedInClause.getFilterParameter()) != null) {
			AbstractMethodNode methodNode = instructionNode.getMethodNode();
			if (containedFilter.matchesMethod(classHierarchy, methodNode)) {
				AbstractClassNode classNode = methodNode.getClassNode();
				String packageName = ExcludeStatementManager.getPackageNameForMatching(classNode);
				String simpleName = ExcludeStatementManager.getSimpleNameForMatching(classNode);
				if (containedFilter.matchesClass(classNode, null, packageName, simpleName, classHierarchy)) {
					return true;
				}
			}

			return false;
		} else {
			return true;
		}
	}

	private void excludeObfuscateReferenceFields(ObfuscateReferenceExcludes excludes, Enumeration<AbstractFieldNode> fields, Set<ClassNode> annotationClasses) {
		while (fields.hasMoreElements()) {
			AbstractFieldNode fieldNode = fields.nextElement();
			if (this.isFieldMatch(fieldNode, excludes)) {
				AbstractClassNode classNode = fieldNode.getClassNode();
				String packageName = ExcludeStatementManager.getPackageNameForMatching(classNode);
				String simpleName = ExcludeStatementManager.getSimpleNameForMatching(classNode);
				if (this.matchesClass(classNode, annotationClasses, packageName, simpleName, excludes)) {
					if (this.containedInClause == null) {
						excludes.excludeFieldReference(fieldNode, null, "exclusion parameter '" + this.ruleString + "'");
					} else {
						for (InstructionListNode referencingInstruction : excludes.getReferencingInstructions(fieldNode)) {
							if (this.matchesContainedInMethod(excludes, referencingInstruction)) {
								excludes.excludeFieldReference(fieldNode, referencingInstruction, "exclusion parameter '" + this.ruleString + "'");
							}
						}
					}
				}
			}
		}
	}

	public boolean isFieldTypeMatch(AbstractFieldNode fieldNode) {
		if (this.fieldTypeDescriptor == null) {
			return true;
		} else {
			String typeDescriptor = ExcludeStatementManager.getDescriptorForMatching(fieldNode);
			return this.fieldTypeDescriptor.equals(typeDescriptor);
		}
	}

	private void includeObfuscateReferenceFields(ObfuscateReferenceExcludes excludes, Enumeration<AbstractFieldNode> fields, Set<ClassNode> annotationClasses) {
		while (fields.hasMoreElements()) {
			AbstractFieldNode fieldNode = fields.nextElement();
			if (this.isFieldMatch(fieldNode, excludes)) {
				AbstractClassNode classNode = fieldNode.getClassNode();
				String packageName = ExcludeStatementManager.getPackageNameForMatching(classNode);
				String simpleName = ExcludeStatementManager.getSimpleNameForMatching(classNode);
				if (this.matchesClass(classNode, annotationClasses, packageName, simpleName, excludes)) {
					if (this.containedInClause == null) {
						excludes.includeFieldReference(fieldNode, null, "inclusion parameter '" + this.ruleString + "'");
					} else {
						for (InstructionListNode referencingInstruction : excludes.getReferencingInstructions(fieldNode)) {
							if (this.matchesContainedInMethod(excludes, referencingInstruction)) {
								excludes.includeFieldReference(fieldNode, referencingInstruction, "inclusion parameter '" + this.ruleString + "'");
							}
						}
					}
				}
			}
		}
	}

	public void excludeClass(AbstractClassExcludeManager excludeManager, ClassNode classNode) {
		if (this.classSpecifier.isRecursive()) {
			excludeManager.excludeClass(classNode, "exclusion parameter '" + this.ruleString + "'");
		}
	}

	private void includeObfuscateReferenceMethods(ObfuscateReferenceExcludes excludes, Enumeration<AbstractMethodNode> methods, Set<ClassNode> annotationClasses) {
		while (methods.hasMoreElements()) {
			AbstractMethodNode methodNode = methods.nextElement();
			if (this.matchesMethod(excludes, methodNode)) {
				AbstractClassNode classNode = methodNode.getClassNode();
				String packageName = ExcludeStatementManager.getPackageNameForMatching(classNode);
				String simpleName = ExcludeStatementManager.getSimpleNameForMatching(classNode);
				if (this.matchesClass(classNode, annotationClasses, packageName, simpleName, excludes)) {
					if (this.containedInClause == null) {
						excludes.includeMethodReference(methodNode, null, "inclusion parameter '" + this.ruleString + "'");
					} else {
						for (InstructionListNode referencingInstruction : excludes.getReferencingInstructions(methodNode)) {
							if (this.matchesContainedInMethod(excludes, referencingInstruction)) {
								excludes.includeMethodReference(methodNode, referencingInstruction, "inclusion parameter '" + this.ruleString + "'");
							}
						}
					}
				}
			}
		}
	}

	private void excludeObfuscateReferenceMethods(ObfuscateReferenceExcludes excludes, Enumeration<AbstractMethodNode> methods, Set<ClassNode> annotationClasses) {
		while (methods.hasMoreElements()) {
			AbstractMethodNode methodNode = methods.nextElement();
			if (this.matchesMethod(excludes, methodNode)) {
				AbstractClassNode classNode = methodNode.getClassNode();
				String packageName = ExcludeStatementManager.getPackageNameForMatching(classNode);
				String simpleName = ExcludeStatementManager.getSimpleNameForMatching(classNode);
				if (this.matchesClass(classNode, annotationClasses, packageName, simpleName, excludes)) {
					if (this.containedInClause == null) {
						excludes.excludeMethodReference(methodNode, null, "exclusion parameter '" + this.ruleString + "'");
					} else {
						for (InstructionListNode referencingInstruction : excludes.getReferencingInstructions(methodNode)) {
							if (this.matchesContainedInMethod(excludes, referencingInstruction)) {
								excludes.excludeMethodReference(methodNode, referencingInstruction, "exclusion parameter '" + this.ruleString + "'");
							}
						}
					}
				}
			}
		}
	}

	public boolean isModuleMatch(ModuleInfoNode moduleNode) {
		return this.moduleSpecifier != null ? this.moduleSpecifier.matchesWildcard(moduleNode.getModuleName()) : false;
	}

	public final boolean checkSourceFileMatch(AbstractClassNode classNode) {
		if (this.sourceFilePattern != null) {
			Enumeration<ClasspathResource> classSources = classNode.getClassSources();

			while (classSources.hasMoreElements()) {
				String containerPath = classSources.nextElement().getContainerPath();
				if (containerPath != null) {
					if (this.sourceFilePattern.length() > 0) {
						containerPath = containerPath.replace('\\', '/');
						if (!FileUtils.IS_UNIX) {
							containerPath = containerPath.toLowerCase();
						}

						String wildcardPattern = "*" + this.sourceFilePattern;
						if (StringUtils.matchesWildcard(containerPath, wildcardPattern)) {
							return true;
						}
					}
				} else if (this.sourceFilePattern.length() == 0) {
					return true;
				}
			}

			return false;
		} else {
			return true;
		}
	}

	public void excludeFlowObfuscation(FlowObfuscateExclude excludes) {
		if (this.isEnabled) {
			Set<ClassNode> annotationClasses = null;
			if (this.isSearchByAnnotation()) {
				boolean matchOnOriginal = Options.ZKM_MATCH_ON_ORIGINAL && (excludes.hasHierarchySnapshot() || excludes.isFieldNameObfFinish() || excludes.isMethodNameObfFinish());
				annotationClasses = this.findClassesByAnnotation(excludes.getWorkspaceClasses(), matchOnOriginal);
			}

			switch (this.targetType) {
				case 2:
					this.excludeFlowObfuscationClasses(excludes, excludes.getProcessableClasses(), annotationClasses);
					break;
				case 3:
					this.excludeFlowObfuscationFields(excludes, excludes.getProcessableClasses(), annotationClasses);
					break;
				case 4:
					if (this.methodSpecifier.isExactName() && excludes.shouldMatchCurrentMethodName()) {
						Enumeration<ClassNode> classesByMethodName = excludes.getClassesByMethodName(this.methodSpecifier.toRuleString());
						if (classesByMethodName != null) {
							while (classesByMethodName.hasMoreElements()) {
								ClassNode classNode = classesByMethodName.nextElement();
								String packageName = ExcludeStatementManager.getPackageNameForMatching(classNode);
								String simpleName = ExcludeStatementManager.getSimpleNameForMatching(classNode);
								if (this.matchesClass(classNode, annotationClasses, packageName, simpleName, excludes)) {
									ArrayEnumeration<MethodNode> methodEnumeration = classNode.getMethodEnumeration();

									while (methodEnumeration.hasMoreElements()) {
										MethodNode methodNode = methodEnumeration.nextElement();
										if (this.matchesMethod(excludes, methodNode)) {
											excludes.excludeMethod(methodNode, "exclusion parameter '" + this.ruleString + "'");
										}
									}
								}
							}
						}
					} else {
						this.excludeFlowObfuscationMethods(excludes, excludes.getProcessableMethods(), annotationClasses);
					}
			}
		}
	}

	public void applyTrimExclusion(TrimExcludes excludes) {
		if (this.isEnabled) {
			Set<ClassNode> annotationClasses = null;
			if (this.isSearchByAnnotation()) {
				boolean matchOnOriginal = Options.ZKM_MATCH_ON_ORIGINAL && (excludes.hasHierarchySnapshot() || excludes.isFieldNameObfFinish() || excludes.isMethodNameObfFinish());
				annotationClasses = this.findClassesByAnnotation(excludes.getWorkspaceClasses(), matchOnOriginal);
			}

			switch (this.targetType) {
				case 2:
					if (this.classSpecifier != null && this.classSpecifier.isExactName() && (this.packageSpecifier == null || this.packageSpecifier.isExactName())) {
						ClassNode linkedClass = this.findLinkedClass();
						if (linkedClass != null) {
							this.applyTrimExclusionToClass(excludes, linkedClass, annotationClasses);
						}
					} else if (this.isClassWildcard()) {
						this.applyTrimExclusionToClasses(excludes, excludes.getWorkspaceClasses(), annotationClasses);
					} else if (this.isLinkClass()) {
						this.excludeTrimLinkClasses(excludes, excludes.getWorkspaceClasses(), annotationClasses);
					} else {
						this.applyTrimExclusionToClasses(excludes, excludes.getProcessableClasses(), annotationClasses);
					}

					if (excludes.isDeleteAnnotationAttributes() && this.isAnnotationType) {
						this.excludeTrimAnnotationTypes(excludes, excludes.getAnnotationCandidates(), annotationClasses);
					}
					break;
				case 3:
					if (this.fieldSpecifier.isExactName() && excludes.shouldMatchCurrentFieldName()) {
						Enumeration<ClassNode> classesByFieldName = excludes.getClassesByFieldName(this.fieldSpecifier.toRuleString());
						if (classesByFieldName != null) {
							while (classesByFieldName.hasMoreElements()) {
								ClassNode fieldOwnerClass = classesByFieldName.nextElement();
								String fieldOwnerPackageName = ExcludeStatementManager.getPackageNameForMatching(fieldOwnerClass);
								String fieldOwnerSimpleName = ExcludeStatementManager.getSimpleNameForMatching(fieldOwnerClass);
								if (this.matchesClass(fieldOwnerClass, annotationClasses, fieldOwnerPackageName, fieldOwnerSimpleName, excludes)) {
									ArrayEnumeration<FieldNode> fieldEnumeration = fieldOwnerClass.getFieldEnumeration();

									while (fieldEnumeration.hasMoreElements()) {
										FieldNode fieldNode = fieldEnumeration.nextElement();
										if (this.isFieldMatch(fieldNode, excludes)) {
											excludes.excludeField(fieldNode, "exclusion parameter '" + this.ruleString + "'");
											this.warnPackageExclusionIgnored(excludes, fieldOwnerPackageName);
											this.excludeClass(excludes, fieldOwnerClass);
										}
									}
								}
							}
						}
					} else {
						this.applyTrimExclusionToFields(excludes, excludes.getProcessableFields(), annotationClasses);
					}
					break;
				case 4:
					if (this.methodSpecifier.isExactName() && excludes.shouldMatchCurrentMethodName()) {
						Enumeration<ClassNode> classesByMethodName = excludes.getClassesByMethodName(this.methodSpecifier.toRuleString());
						if (classesByMethodName != null) {
							while (classesByMethodName.hasMoreElements()) {
								ClassNode methodOwnerClass = classesByMethodName.nextElement();
								String methodOwnerPackageName = ExcludeStatementManager.getPackageNameForMatching(methodOwnerClass);
								String methodOwnerSimpleName = ExcludeStatementManager.getSimpleNameForMatching(methodOwnerClass);
								if (this.matchesClass(methodOwnerClass, annotationClasses, methodOwnerPackageName, methodOwnerSimpleName, excludes)) {
									ArrayEnumeration<MethodNode> methodEnumeration = methodOwnerClass.getMethodEnumeration();

									while (methodEnumeration.hasMoreElements()) {
										MethodNode methodNode = methodEnumeration.nextElement();
										if (this.matchesMethod(excludes, methodNode)) {
											excludes.excludeMethod(methodNode, "exclusion parameter '" + this.ruleString + "'");
											this.warnPackageExclusionIgnored(excludes, methodOwnerPackageName);
											this.excludeClass(excludes, methodOwnerClass);
										}
									}
								}
							}
						}
					} else {
						this.applyTrimExclusionToMethods(excludes, excludes.getProcessableMethods(), annotationClasses);
					}
			}
		}
	}

	public void setClassAnnotationSpecifier(ComplexAnnotationSpecifier classAnnotationSpecifier) {
		this.classAnnotationSpecifier = classAnnotationSpecifier;
	}

	public void includeFlowObfuscation(FlowObfuscateExclude excludes) {
		if (this.isEnabled) {
			Set<ClassNode> annotationClasses = null;
			if (this.isSearchByAnnotation()) {
				boolean matchOnOriginal = Options.ZKM_MATCH_ON_ORIGINAL && (excludes.hasHierarchySnapshot() || excludes.isFieldNameObfFinish() || excludes.isMethodNameObfFinish());
				annotationClasses = this.findClassesByAnnotation(excludes.getWorkspaceClasses(), matchOnOriginal);
			}

			switch (this.targetType) {
				case 2:
					this.includeFlowObfuscationClasses(excludes, excludes.getWorkspaceClasses(), annotationClasses);
					break;
				case 3:
					this.includeFlowObfuscationFields(excludes, excludes.getWorkspaceClasses(), annotationClasses);
					break;
				case 4:
					if (this.methodSpecifier.isExactName() && excludes.shouldMatchCurrentMethodName()) {
						Enumeration<ClassNode> classesByMethodName = excludes.getClassesByMethodName(this.methodSpecifier.toRuleString());
						if (classesByMethodName != null) {
							while (classesByMethodName.hasMoreElements()) {
								ClassNode classNode = classesByMethodName.nextElement();
								String packageName = ExcludeStatementManager.getPackageNameForMatching(classNode);
								String simpleName = ExcludeStatementManager.getSimpleNameForMatching(classNode);
								if (this.matchesClass(classNode, annotationClasses, packageName, simpleName, excludes)) {
									ArrayEnumeration<MethodNode> methodEnumeration = classNode.getMethodEnumeration();

									while (methodEnumeration.hasMoreElements()) {
										MethodNode methodNode = methodEnumeration.nextElement();
										if (this.matchesMethod(excludes, methodNode)) {
											excludes.includeMethod(methodNode, "unexclusion parameter '" + this.ruleString + "'");
										}
									}
								}
							}
						}
					} else {
						this.includeFlowObfuscationMethods(excludes, excludes.getAllMethods(), annotationClasses);
					}
			}
		}
	}

	public void applyTrimInclusion(TrimExcludes excludes) {
		if (this.isEnabled) {
			Set<ClassNode> annotationClasses = null;
			if (this.isSearchByAnnotation()) {
				boolean matchOnOriginal = Options.ZKM_MATCH_ON_ORIGINAL && (excludes.hasHierarchySnapshot() || excludes.isFieldNameObfFinish() || excludes.isMethodNameObfFinish());
				annotationClasses = this.findClassesByAnnotation(excludes.getWorkspaceClasses(), matchOnOriginal);
			}

			switch (this.targetType) {
				case 2:
					if (this.classSpecifier != null && this.classSpecifier.isExactName() && (this.packageSpecifier == null || this.packageSpecifier.isExactName())) {
						ClassNode linkedClass = this.findLinkedClass();
						if (linkedClass != null) {
							this.applyTrimInclusionToClass(excludes, linkedClass, annotationClasses);
						}
					} else if (this.isClassWildcard()) {
						this.applyTrimInclusionToClasses(excludes, excludes.getWorkspaceClasses(), annotationClasses);
					} else {
						this.applyTrimInclusionToClasses(excludes, excludes.getExcludedClasses(), annotationClasses);
					}

					if (excludes.isDeleteAnnotationAttributes() && this.isAnnotationType) {
						this.includeTrimAnnotationTypes(excludes, excludes.getAnnotationCandidates(), annotationClasses);
					}
					break;
				case 3:
					if (this.fieldSpecifier.isExactName() && excludes.shouldMatchCurrentFieldName()) {
						Enumeration<ClassNode> classesByFieldName = excludes.getClassesByFieldName(this.fieldSpecifier.toRuleString());
						if (classesByFieldName != null) {
							while (classesByFieldName.hasMoreElements()) {
								ClassNode fieldOwnerClass = classesByFieldName.nextElement();
								String fieldOwnerPackageName = ExcludeStatementManager.getPackageNameForMatching(fieldOwnerClass);
								String fieldOwnerSimpleName = ExcludeStatementManager.getSimpleNameForMatching(fieldOwnerClass);
								if (this.matchesClass(fieldOwnerClass, annotationClasses, fieldOwnerPackageName, fieldOwnerSimpleName, excludes)) {
									ArrayEnumeration<FieldNode> fieldEnumeration = fieldOwnerClass.getFieldEnumeration();

									while (fieldEnumeration.hasMoreElements()) {
										FieldNode fieldNode = fieldEnumeration.nextElement();
										if (this.isFieldMatch(fieldNode, excludes)) {
											excludes.unexcludeField(fieldNode, "unexclusion parameter '" + this.ruleString + "'");
											this.warnPackageIgnoredForTrim(excludes, fieldOwnerPackageName);
											this.includeClassIfWildcard(excludes, fieldOwnerClass);
										}
									}
								}
							}
						}
					} else {
						this.applyTrimInclusionToFields(excludes, excludes.getExcludedFields(), annotationClasses);
					}
					break;
				case 4:
					if (this.methodSpecifier.isExactName() && excludes.shouldMatchCurrentMethodName()) {
						Enumeration<ClassNode> classesByMethodName = excludes.getClassesByMethodName(this.methodSpecifier.toRuleString());
						if (classesByMethodName != null) {
							while (classesByMethodName.hasMoreElements()) {
								ClassNode methodOwnerClass = classesByMethodName.nextElement();
								String methodOwnerPackageName = ExcludeStatementManager.getPackageNameForMatching(methodOwnerClass);
								String methodOwnerSimpleName = ExcludeStatementManager.getSimpleNameForMatching(methodOwnerClass);
								if (this.matchesClass(methodOwnerClass, annotationClasses, methodOwnerPackageName, methodOwnerSimpleName, excludes)) {
									ArrayEnumeration<MethodNode> methodEnumeration = methodOwnerClass.getMethodEnumeration();

									while (methodEnumeration.hasMoreElements()) {
										MethodNode methodNode = methodEnumeration.nextElement();
										if (this.matchesMethod(excludes, methodNode)) {
											excludes.unexcludeMethod(methodNode, "unexclusion parameter '" + this.ruleString + "'");
											this.warnPackageIgnoredForTrim(excludes, methodOwnerPackageName);
											this.includeClassIfWildcard(excludes, methodOwnerClass);
										}
									}
								}
							}
						}
					} else {
						this.applyTrimInclusionToMethods(excludes, excludes.getExcludedMethods(), annotationClasses);
					}
			}
		}
	}

	public void setMemberAnnotationSpecifier(ComplexAnnotationSpecifier memberAnnotationSpecifier) {
		this.memberAnnotationSpecifier = memberAnnotationSpecifier;
	}

	public void addImplementsAnnotationSpecifier(ComplexAnnotationSpecifier annotationSpecifier) {
		this.implementsAnnotationSpecifiers.add(annotationSpecifier);
	}

	public void setExtendsAnnotationSpecifier(ComplexAnnotationSpecifier extendsAnnotationSpecifier) {
		this.extendsAnnotationSpecifier = extendsAnnotationSpecifier;
	}

	private boolean isMemberStatic() {
		return this.memberAccessFlags.isPositiveStatic();
	}

	public void setMethodArgumentSpecifier(MethodArgumentSpecifier methodArgumentSpecifier) {
		this.methodArgumentSpecifier = methodArgumentSpecifier;
	}

	public void setParameterAnnotationSpecifiers(ComplexAnnotationSpecifier[] parameterAnnotations) {
		this.parameterAnnotationSpecifiers = parameterAnnotations;
	}

	private void excludeLongEncryptionMethods(LongEncryptionExcludes excludes, Enumeration<MethodNode> methods, Set<ClassNode> annotationClasses) {
		while (methods.hasMoreElements()) {
			MethodNode methodNode = methods.nextElement();
			if (this.matchesMethod(excludes, methodNode)) {
				ClassNode ownerClass = methodNode.getMethodOwner();
				String packageName = ExcludeStatementManager.getPackageNameForMatching(ownerClass);
				String simpleName = ExcludeStatementManager.getSimpleNameForMatching(ownerClass);
				if (this.matchesClass(ownerClass, annotationClasses, packageName, simpleName, excludes)) {
					excludes.excludeMethodConstants(methodNode, "exclusion parameter '" + this.ruleString + "'");
				}
			}
		}
	}

	public final boolean checkImplementsClauseMatch(AbstractClassNode classNode, ClassHierarchy classHierarchy) {
		for (int interfaceIndex = 0; interfaceIndex < this.implementsInterfaceNames.size(); interfaceIndex++) {
			String interfaceName = this.implementsInterfaceNames.get(interfaceIndex);
			ComplexAnnotationSpecifier annotationSpecifier = this.implementsAnnotationSpecifiers.get(interfaceIndex);

			try {
				if (classNode.isGeneratedClass()) {
					return false;
				}

				boolean clauseMatched;
				if (annotationSpecifier != null) {
					if (Options.ZKM_MATCH_ON_ORIGINAL && classHierarchy.hasHierarchySnapshot()) {
						clauseMatched = classHierarchy.isInterfaceOf(classNode.getClassName(), interfaceName, annotationSpecifier);
					} else {
						clauseMatched = classHierarchy.isInterfaceImplementedBy(classNode.getInternalName(), interfaceName, annotationSpecifier);
					}
				} else if (Options.ZKM_MATCH_ON_ORIGINAL && classHierarchy.hasHierarchySnapshot()) {
					clauseMatched = classHierarchy.isAssignableFrom(classNode.getClassName(), interfaceName);
				} else {
					clauseMatched = classHierarchy.isSubclassOf(classNode.getInternalName(), interfaceName);
				}

				if (!clauseMatched) {
					return false;
				}
			} catch (ClasspathException classpathException) {
				this.scriptContext
					.logWarning(
						"Error while executing \""
							+ this.getCommandName()
							+ "\" statement at line "
							+ this.getLineNumber()
							+ " while analysing '"
							+ this.ruleString
							+ "' : class '"
							+ GlobalUtils.internalToBinaryName(classpathException.getClassName())
							+ "' not found. (D)"
					);
			} catch (FileException fileException) {
				this.scriptContext
					.logWarning(
						"Error while executing \""
							+ this.getCommandName()
							+ "\" statement at line "
							+ this.getLineNumber()
							+ " while analysing '"
							+ this.ruleString
							+ "' : "
							+ fileException.getMessage()
							+ " (D)"
					);
			}
		}

		return true;
	}

	private boolean isMemberPrivate() {
		return this.memberAccessFlags.isPositivePrivate();
	}

	private void includeRenameClasses(DefaultExcludes excludes, Enumeration<ClassNode> classes, Set<ClassNode> annotationClasses) {
		while (classes.hasMoreElements()) {
			ClassNode classNode = classes.nextElement();
			this.applyRenameInclusionToClass(excludes, classNode, annotationClasses);
		}
	}

	public final boolean isContainedInLinkMethod() {
		return this.containedInClause != null && this.containedInClause.getFilterParameter() != null && this.containedInClause.getFilterParameter().linkMethodNamePattern != null;
	}

	private void applyTrimExclusionToMethods(TrimExcludes excludes, Enumeration<MethodNode> methods, Set<ClassNode> annotationClasses) {
		while (methods.hasMoreElements()) {
			MethodNode methodNode = methods.nextElement();
			if (this.matchesMethod(excludes, methodNode)) {
				ClassNode ownerClass = methodNode.getMethodOwner();
				String packageName = ExcludeStatementManager.getPackageNameForMatching(ownerClass);
				String simpleName = ExcludeStatementManager.getSimpleNameForMatching(ownerClass);
				if (this.matchesClass(ownerClass, annotationClasses, packageName, simpleName, excludes)) {
					excludes.excludeMethod(methodNode, "exclusion parameter '" + this.ruleString + "'");
					this.warnPackageExclusionIgnored(excludes, packageName);
					this.excludeClass(excludes, ownerClass);
				}
			}
		}
	}

	public void excludeSerializedClass(SerializedClassManager serializedClassManager) {
		this.excludeSerializedClasses(serializedClassManager, serializedClassManager.getProcessableClasses());
	}

	public boolean isContainedInMethodTarget() {
		return this.containedInClause == null || this.containedInClause.getFilterParameter() == null || this.containedInClause.getFilterParameter().targetType == 4;
	}

	public void includeExceptionObfuscation(ObfuscateExceptionExclude excludes) {
		if (this.isEnabled) {
			Set<ClassNode> annotationClasses = null;
			if (this.isSearchByAnnotation()) {
				boolean matchOnOriginal = Options.ZKM_MATCH_ON_ORIGINAL && (excludes.hasHierarchySnapshot() || excludes.isFieldNameObfFinish() || excludes.isMethodNameObfFinish());
				annotationClasses = this.findClassesByAnnotation(excludes.getWorkspaceClasses(), matchOnOriginal);
			}

			switch (this.targetType) {
				case 2:
					this.includeExceptionObfuscationClasses(excludes, excludes.getWorkspaceClasses(), annotationClasses);
					break;
				case 4:
					if (this.methodSpecifier.isExactName() && excludes.shouldMatchCurrentMethodName()) {
						Enumeration<ClassNode> classesByMethodName = excludes.getClassesByMethodName(this.methodSpecifier.toRuleString());
						if (classesByMethodName != null) {
							while (classesByMethodName.hasMoreElements()) {
								ClassNode classNode = classesByMethodName.nextElement();
								String packageName = ExcludeStatementManager.getPackageNameForMatching(classNode);
								String simpleName = ExcludeStatementManager.getSimpleNameForMatching(classNode);
								if (this.matchesClass(classNode, annotationClasses, packageName, simpleName, excludes)) {
									ArrayEnumeration<MethodNode> methodEnumeration = classNode.getMethodEnumeration();

									while (methodEnumeration.hasMoreElements()) {
										MethodNode methodNode = methodEnumeration.nextElement();
										if (this.matchesMethod(excludes, methodNode)) {
											excludes.includeMethod(methodNode, "unexclusion parameter '" + this.ruleString + "'");
										}
									}
								}
							}
						}
					} else {
						this.includeExceptionObfuscationMethods(excludes, excludes.getAllMethods(), annotationClasses);
					}
			}
		}
	}

	public void excludeExceptionObfuscation(ObfuscateExceptionExclude excludes) {
		if (this.isEnabled) {
			Set<ClassNode> annotationClasses = null;
			if (this.isSearchByAnnotation()) {
				boolean matchOnOriginal = Options.ZKM_MATCH_ON_ORIGINAL && (excludes.hasHierarchySnapshot() || excludes.isFieldNameObfFinish() || excludes.isMethodNameObfFinish());
				annotationClasses = this.findClassesByAnnotation(excludes.getWorkspaceClasses(), matchOnOriginal);
			}

			switch (this.targetType) {
				case 2:
					this.excludeExceptionObfuscationClasses(excludes, excludes.getProcessableClasses(), annotationClasses);
					break;
				case 4:
					if (this.methodSpecifier.isExactName() && excludes.shouldMatchCurrentMethodName()) {
						Enumeration<ClassNode> classesByMethodName = excludes.getClassesByMethodName(this.methodSpecifier.toRuleString());
						if (classesByMethodName != null) {
							while (classesByMethodName.hasMoreElements()) {
								ClassNode classNode = classesByMethodName.nextElement();
								String packageName = ExcludeStatementManager.getPackageNameForMatching(classNode);
								String simpleName = ExcludeStatementManager.getSimpleNameForMatching(classNode);
								if (this.matchesClass(classNode, annotationClasses, packageName, simpleName, excludes)) {
									ArrayEnumeration<MethodNode> methodEnumeration = classNode.getMethodEnumeration();

									while (methodEnumeration.hasMoreElements()) {
										MethodNode methodNode = methodEnumeration.nextElement();
										if (this.matchesMethod(excludes, methodNode)) {
											excludes.excludeMethod(methodNode, "exclusion parameter '" + this.ruleString + "'");
										}
									}
								}
							}
						}
					} else {
						this.excludeExceptionObfuscationMethods(excludes, excludes.getProcessableMethods(), annotationClasses);
					}
			}
		}
	}

	private void excludeLongEncryptionFields(LongEncryptionExcludes excludes, Enumeration<FieldNode> fields, Set<ClassNode> annotationClasses) {
		while (fields.hasMoreElements()) {
			FieldNode fieldNode = fields.nextElement();
			if (this.isFieldMatch(fieldNode, excludes)) {
				ClassNode ownerClass = fieldNode.getOwnerClass();
				String packageName = ExcludeStatementManager.getPackageNameForMatching(ownerClass);
				String simpleName = ExcludeStatementManager.getSimpleNameForMatching(ownerClass);
				if (this.matchesClass(ownerClass, annotationClasses, packageName, simpleName, excludes)) {
					excludes.excludeFieldConstants(fieldNode, "exclusion parameter '" + this.ruleString + "'");
				}
			}
		}
	}

	private boolean isClassFinal() {
		return this.classAccessFlags.isPositiveFinal();
	}

	private void applyRenameInclusionToClass(DefaultExcludes excludes, ClassNode classNode, Set<ClassNode> annotationClasses) {
		String packageName = ExcludeStatementManager.getPackageNameForMatching(classNode);
		String simpleName = ExcludeStatementManager.getSimpleNameForMatching(classNode);
		if (this.matchesClass(classNode, annotationClasses, packageName, simpleName, excludes)) {
			excludes.includeClassWithReason(classNode, "unexclusion parameter '" + this.ruleString + "'", false);
			this.unexcludePackage(excludes, packageName);
			if (this.classSpecifier != null && this.classSpecifier.includesMembers()) {
				this.includeAllMembersInRename(excludes, classNode);
			}
		}
	}

	public boolean isFieldMatch(AbstractFieldNode fieldNode, ClassHierarchy classHierarchy) {
		if (!matchesAccessFlags(fieldNode.getAccessFlags(), this.memberAccessFlags)) {
			return false;
		} else if (!this.matchesFieldName(fieldNode)) {
			return false;
		} else {
			return !this.isFieldTypeMatch(fieldNode) ? false : fieldHasAnnotation(fieldNode, this.memberAnnotationSpecifier, classHierarchy);
		}
	}

	public void includeAllMembersInTrim(TrimExcludes excludes, ClassNode classNode) {
		FieldNode[] fieldNodes = classNode.getFieldNodes();

		for (int fieldIndex = 0; fieldIndex < fieldNodes.length; fieldIndex++) {
			excludes.unexcludeField(fieldNodes[fieldIndex], "unexclusion parameter '" + this.ruleString + "'");
		}

		MethodNode[] methodNodes = classNode.getMethodNodes();

		for (int methodIndex = 0; methodIndex < methodNodes.length; methodIndex++) {
			MethodNode methodNode = methodNodes[methodIndex];
			excludes.unexcludeMethod(methodNode, "unexclusion parameter '" + this.ruleString + "'");
		}
	}

	public void excludeAllMembersFromTrim(TrimExcludes excludes, ClassNode classNode) {
		FieldNode[] fieldNodes = classNode.getFieldNodes();

		for (int fieldIndex = 0; fieldIndex < fieldNodes.length; fieldIndex++) {
			excludes.excludeField(fieldNodes[fieldIndex], "exclusion parameter '" + this.ruleString + "'");
		}

		MethodNode[] methodNodes = classNode.getMethodNodes();

		for (int methodIndex = 0; methodIndex < methodNodes.length; methodIndex++) {
			MethodNode methodNode = methodNodes[methodIndex];
			excludes.excludeMethod(methodNode, "exclusion parameter '" + this.ruleString + "'");
		}
	}

	private boolean checkClassAnnotationMatch(AbstractClassNode classNode, ClassHierarchy classHierarchy) {
		if (this.classAnnotationSpecifier != null) {
			boolean inheritedAnnotation;
			if (this.classAnnotationSpecifier.isExactName()) {
				inheritedAnnotation = false;
				String annotationInternalName = GlobalUtils.binaryToInternalName(this.classAnnotationSpecifier.toRuleString().substring(1));

				try {
					AbstractClassNode annotationClassNode = classHierarchy.getClassProvider().getClassNode(annotationInternalName);
					if (annotationClassNode.hasAnnotation("java/lang/annotation/Inherited")) {
						inheritedAnnotation = true;
					}
				} catch (FileException fileException) {
					String errorMessage = "Error while executing \""
						+ this.getCommandName()
						+ "\" statement at line "
						+ this.getLineNumber()
						+ " while analysing '"
						+ this.ruleString
						+ "' : '"
						+ fileException.getMessage()
						+ "' : While looking for class '"
						+ annotationInternalName
						+ "'. (F)";
					this.scriptContext.logError(errorMessage);
				}
			} else {
				inheritedAnnotation = true;
			}

			try {
				Integer innerClassOrdinal = classNode.hasInnerClassOrdinal() ? classNode.getInnerClassOrdinal() : null;
				boolean matchOnOriginal = Options.ZKM_MATCH_ON_ORIGINAL && classHierarchy.hasHierarchySnapshot();
				Set<String> annotationTypes = classHierarchy.getAnnotationTypes(classNode.getInternalName(), innerClassOrdinal, matchOnOriginal);
				if (!inheritedAnnotation) {
					AbstractClassNode targetClassNode = classHierarchy.getClassProvider().getClassNode(classNode.getInternalName(), innerClassOrdinal);
					Set<String> declaredAnnotationTypes = GlobalUtils.newHashSet();
					Enumeration<String> declaredAnnotationNames = targetClassNode.getAnnotationTypeNames();
					while (declaredAnnotationNames.hasMoreElements()) {
						String declaredAnnotationName = declaredAnnotationNames.nextElement();
						declaredAnnotationTypes.add(matchOnOriginal ? classHierarchy.getBinaryName(declaredAnnotationName) : declaredAnnotationName);
					}
					annotationTypes.retainAll(declaredAnnotationTypes);
				}
				return this.classAnnotationSpecifier.matchesAnnotationTypes(annotationTypes);
			} catch (ClasspathException classpathException) {
				String errorMessage = "Error while executing \""
						+ this.getCommandName()
						+ "\" statement at line "
						+ this.getLineNumber()
						+ " while analysing '"
						+ this.ruleString
						+ "' : class '"
						+ GlobalUtils.internalToBinaryName(classpathException.getClassName())
						+ "' not found while examining class '"
						+ classNode.getBinaryName()
						+ "'. (G)";
				if (!Options.useLogErrorForRenameFilter) {
					this.scriptContext.logFatalError(errorMessage);
				} else {
					this.scriptContext.logError(errorMessage);
				}
				return false;
			} catch (FileException fileException) {
				String errorMessage = "Error while executing \""
						+ this.getCommandName()
						+ "\" statement at line "
						+ this.getLineNumber()
						+ " while analysing '"
						+ this.ruleString
						+ "' : '"
						+ fileException.getMessage()
						+ "' : While examining class '"
						+ classNode.getBinaryName()
						+ "'. (H)";
				if (!Options.useLogErrorForRenameFilter) {
					this.scriptContext.logFatalError(errorMessage);
				} else {
					this.scriptContext.logError(errorMessage);
				}
				return false;
			}
		} else {
			return true;
		}
	}

	public void addClassAccessFlag(String accessFlag) {
		this.recordModifier(accessFlag, this.classAccessFlagStrings);
	}

	public final boolean isSearchByAnnotation() {
		return this.searchByAnnotationName != null;
	}

	public void setSourceFilePattern(String sourceFilePattern) {
		if (sourceFilePattern != null) {
			this.sourceFilePattern = sourceFilePattern.replace('\\', '/');
			if (!FileUtils.IS_UNIX && !this.sourceFilePattern.equals(this.sourceFilePattern.toLowerCase())) {
				this.scriptContext
					.logMessageInternal(
						"\""
							+ this.sourceFilePattern
							+ "\" converted to \""
							+ this.sourceFilePattern.toLowerCase()
							+ "\" in \""
							+ this.getCommandName()
							+ "\" statement at line "
							+ this.getLineNumber()
							+ " because file system \""
							+ FileUtils.OS_NAME
							+ "\" is case insensitive.",
						true
					);
				this.sourceFilePattern = this.sourceFilePattern.toLowerCase();
			}
		}
	}

	public void setSearchByAnnotationProperty(String propertyName) {
		this.searchByAnnotationProperty = propertyName;
	}

	private void logClassAndMembersWarning(String memberType) {
		this.scriptContext
			.logErrorInternal(
				"Class exclude parameter \""
					+ this.ruleString
					+ "\" in \""
					+ this.getCommandName()
					+ "\" statement at line "
					+ this.getLineNumber()
					+ "\" has a \"+\" followed by a "
					+ memberType
					+ " specification. \"+\" ignored.",
				true
			);
	}

	public void setLinkClassNamePattern(String linkClassNamePattern) {
		this.linkClassNamePattern = linkClassNamePattern;
		int linkMarkerIndex = linkClassNamePattern.indexOf("<link>");
		this.linkClassNameSuffix = linkClassNamePattern.substring(linkMarkerIndex + 6);
		this.linkClassNamePrefix = linkClassNamePattern.substring(0, linkMarkerIndex);
		this.primaryVisibilityModifier = null;
	}

	@Override
	public String toString() {
		return this.toRuleString();
	}

	public void addImplementsInterface(String interfaceName) {
		this.implementsInterfaceNames.add(interfaceName);
	}

	public void addMemberAccessFlag(String accessFlag) {
		this.recordModifier(accessFlag, this.memberAccessFlagStrings);
	}

	public void setExtendsClassName(String extendsClassName) {
		this.extendsClassName = extendsClassName;
	}

	public void addSearchPath(String searchPath) {
		this.searchPaths.add(searchPath);
	}

	private boolean isClassInterface() {
		return this.classAccessFlags.isPositiveInterface();
	}

	public void excludeStringEncryption(StringEncryptionExcludes stringEncryptionManager) {
		Set<ClassNode> annotationClasses = null;
		if (this.isSearchByAnnotation()) {
			boolean matchOnOriginal = Options.ZKM_MATCH_ON_ORIGINAL && (stringEncryptionManager.hasHierarchySnapshot() || stringEncryptionManager.isFieldNameObfFinish() || stringEncryptionManager.isMethodNameObfFinish());
			annotationClasses = this.findClassesByAnnotation(stringEncryptionManager.getWorkspaceClasses(), matchOnOriginal);
		}

		switch (this.targetType) {
			case 2:
				this.excludeStringEncryptionClasses(stringEncryptionManager, stringEncryptionManager.getProcessableClasses(), annotationClasses);
				break;
			case 3:
				if (this.fieldSpecifier.isExactName() && stringEncryptionManager.shouldMatchCurrentFieldName()) {
					Enumeration<ClassNode> classesByFieldName = stringEncryptionManager.getClassesByFieldName(this.fieldSpecifier.toRuleString());
					if (classesByFieldName != null) {
						while (classesByFieldName.hasMoreElements()) {
							ClassNode fieldOwnerClass = classesByFieldName.nextElement();
							String fieldOwnerPackageName = ExcludeStatementManager.getPackageNameForMatching(fieldOwnerClass);
							String fieldOwnerSimpleName = ExcludeStatementManager.getSimpleNameForMatching(fieldOwnerClass);
							if (this.matchesClass(fieldOwnerClass, annotationClasses, fieldOwnerPackageName, fieldOwnerSimpleName, stringEncryptionManager)) {
								ArrayEnumeration<FieldNode> fieldEnumeration = fieldOwnerClass.getFieldEnumeration();

								while (fieldEnumeration.hasMoreElements()) {
									FieldNode fieldNode = fieldEnumeration.nextElement();
									if (this.isFieldMatch(fieldNode, stringEncryptionManager)) {
										stringEncryptionManager.excludeFieldStrings(fieldNode, "exclusion parameter '" + this.ruleString + "'");
									}
								}
							}
						}
					}
				} else {
					this.excludeStringEncryptionFields(stringEncryptionManager, stringEncryptionManager.getProcessableFields(), annotationClasses);
				}
				break;
			case 4:
				if (this.methodSpecifier.isExactName() && stringEncryptionManager.shouldMatchCurrentMethodName()) {
					Enumeration<ClassNode> classesByMethodName = stringEncryptionManager.getClassesByMethodName(this.methodSpecifier.toRuleString());
					if (classesByMethodName != null) {
						while (classesByMethodName.hasMoreElements()) {
							ClassNode methodOwnerClass = classesByMethodName.nextElement();
							String methodOwnerPackageName = ExcludeStatementManager.getPackageNameForMatching(methodOwnerClass);
							String methodOwnerSimpleName = ExcludeStatementManager.getSimpleNameForMatching(methodOwnerClass);
							if (this.matchesClass(methodOwnerClass, annotationClasses, methodOwnerPackageName, methodOwnerSimpleName, stringEncryptionManager)) {
								ArrayEnumeration<MethodNode> methodEnumeration = methodOwnerClass.getMethodEnumeration();

								while (methodEnumeration.hasMoreElements()) {
									MethodNode methodNode = methodEnumeration.nextElement();
									if (this.matchesMethod(stringEncryptionManager, methodNode)) {
										stringEncryptionManager.excludeMethodStrings(methodNode, "exclusion parameter '" + this.ruleString + "'");
									}
								}
							}
						}
					}
				} else {
					this.excludeStringEncryptionMethods(stringEncryptionManager, stringEncryptionManager.getProcessableMethods(), annotationClasses);
				}
		}
	}

	public void applyFixedClassExclusion(FixedClassManager fixedClassManager) {
		this.applyFixedClassExclusion(fixedClassManager, fixedClassManager.getProcessableClasses());
	}

	private void includeLongEncryptionClasses(LongEncryptionExcludes excludes, Enumeration<ClassNode> classes, Set<ClassNode> annotationClasses) {
		while (classes.hasMoreElements()) {
			ClassNode classNode = classes.nextElement();
			String packageName = ExcludeStatementManager.getPackageNameForMatching(classNode);
			String simpleName = ExcludeStatementManager.getSimpleNameForMatching(classNode);
			if (this.matchesClass(classNode, annotationClasses, packageName, simpleName, excludes)) {
				excludes.includeClass(classNode, "unexclusion parameter '" + this.ruleString + "'");
			}
		}
	}

	public final boolean isLinkClass() {
		return this.linkClassNamePattern != null;
	}

	private void includeLongEncryptionFields(LongEncryptionExcludes excludes, Enumeration<FieldNode> fields, Set<ClassNode> annotationClasses) {
		while (fields.hasMoreElements()) {
			FieldNode fieldNode = fields.nextElement();
			if (this.isFieldMatch(fieldNode, excludes)) {
				ClassNode ownerClass = fieldNode.getOwnerClass();
				String packageName = ExcludeStatementManager.getPackageNameForMatching(ownerClass);
				String simpleName = ExcludeStatementManager.getSimpleNameForMatching(ownerClass);
				if (this.matchesClass(ownerClass, annotationClasses, packageName, simpleName, excludes)) {
					excludes.includeFieldConstants(fieldNode, "unexclusion parameter '" + this.ruleString + "'");
				}
			}
		}
	}

	private void excludeLongEncryptionClasses(LongEncryptionExcludes excludes, Enumeration<ClassNode> classes, Set<ClassNode> annotationClasses) {
		while (classes.hasMoreElements()) {
			ClassNode classNode = classes.nextElement();
			String packageName = ExcludeStatementManager.getPackageNameForMatching(classNode);
			String simpleName = ExcludeStatementManager.getSimpleNameForMatching(classNode);
			if (this.matchesClass(classNode, annotationClasses, packageName, simpleName, excludes)) {
				excludes.excludeClass(classNode, "exclusion parameter '" + this.ruleString + "'");
			}
		}
	}

	private boolean isMemberVolatile() {
		return this.memberAccessFlags.isPositiveVolatile();
	}

	public boolean hasImplementsClause() {
		return !this.implementsInterfaceNames.isEmpty();
	}

	private void excludeExceptionObfuscationClasses(ObfuscateExceptionExclude excludes, Enumeration<ClassNode> classes, Set<ClassNode> annotationClasses) {
		while (classes.hasMoreElements()) {
			ClassNode classNode = classes.nextElement();
			String packageName = ExcludeStatementManager.getPackageNameForMatching(classNode);
			String simpleName = ExcludeStatementManager.getSimpleNameForMatching(classNode);
			if (this.matchesClass(classNode, annotationClasses, packageName, simpleName, excludes)) {
				excludes.excludeClass(classNode, "exclusion parameter '" + this.ruleString + "'");
			}
		}
	}

	private void applyRenameExclusionToClass(DefaultExcludes excludes, ClassNode classNode, Set<ClassNode> annotationClasses) {
		String excludedPackageName = classNode.getPackageName();
		String packageName = ExcludeStatementManager.getPackageNameForMatching(classNode);
		String simpleName = ExcludeStatementManager.getSimpleNameForMatching(classNode);
		if (this.matchesClass(classNode, annotationClasses, packageName, simpleName, excludes)) {
			if (this.isLinkClass()) {
				excludes.matchClassToLinkedClass(classNode, this.linkClassNamePrefix, this.linkClassNameSuffix, this.searchPaths, "exclusion parameter '" + this.ruleString + "'");
			} else {
				excludes.excludeClass(classNode, "exclusion parameter '" + this.ruleString + "'");
			}

			this.excludePackage(excludes, excludedPackageName);
			if (this.classSpecifier != null && this.classSpecifier.includesMembers()) {
				this.excludeAllMembersFromRename(excludes, classNode);
			}
		}
	}

	private void includeParameterChangerMethods(ParameterChangerExcludes excludes, Enumeration<MethodNode> methods, Set<ClassNode> annotationClasses) {
		while (methods.hasMoreElements()) {
			MethodNode methodNode = methods.nextElement();
			if (ParameterChangerExcludes.isProcessableMethod(methodNode) && this.matchesMethod(excludes, methodNode)) {
				ClassNode methodOwnerClass = methodNode.getMethodOwner();
				String packageName = ExcludeStatementManager.getPackageNameForMatching(methodOwnerClass);
				String simpleName = ExcludeStatementManager.getSimpleNameForMatching(methodOwnerClass);
				if (this.matchesClass(methodOwnerClass, annotationClasses, packageName, simpleName, excludes)) {
					excludes.excludeMethod(methodNode, "inclusion parameter '" + this.ruleString + "'");
				}
			}
		}
	}

	public boolean hasContainingClause() {
		return this.containingClause != null;
	}

	private void noOpClassSpecifierCheck() {
		if (this.classSpecifier != null) {
		}
	}

	public void setIsAnnotationType() {
		this.isAnnotationType = true;
	}

	public void validateAccessFlags() {
		if (this.classAccessFlags != null) {
			if (this.isClassInterface() && this.isClassFinal()) {
				this.setEnabled(false);
				this.scriptContext
					.logWarningInternal(
						"Class exclude parameter \""
							+ this.ruleString
							+ "\" in \""
							+ this.getCommandName()
							+ "\" statement at line "
							+ this.getLineNumber()
							+ "\" has the invalid combination of modifiers : interface and final. The parameter will be ignored.",
						true
					);
			}

			if (this.isClassAbstract() && this.isClassFinal()) {
				this.setEnabled(false);
				this.scriptContext
					.logWarningInternal(
						"Class exclude parameter \""
							+ this.ruleString
							+ "\" in \""
							+ this.getCommandName()
							+ "\" statement at line "
							+ this.getLineNumber()
							+ "\" has the invalid combination of modifiers : abstract and final. The parameter will be ignored.",
						true
					);
			}

			if (this.classAccessFlags.hasConflictingModifiers()) {
				this.setEnabled(false);
				this.scriptContext
					.logWarningInternal(
						"Class exclude parameter \""
							+ this.ruleString
							+ "\" in \""
							+ this.getCommandName()
							+ "\" statement at line "
							+ this.getLineNumber()
							+ "\" has a contradictory combination of modifiers. The parameter will be ignored.",
						true
					);
			}
		}

		if (this.memberAccessFlags != null) {
			Vector<String> invalidModifierNames = new Vector<>();
			switch (this.targetType) {
				case 3:
					if (this.isMemberSynchronized()) {
						invalidModifierNames.addElement("synchronized");
					}

					if (this.isMemberNative()) {
						invalidModifierNames.addElement("native");
					}

					if (this.isMemberAbstract()) {
						invalidModifierNames.addElement("abstract");
					}

					if (invalidModifierNames.size() > 0) {
						this.setEnabled(false);
						this.scriptContext
							.logWarningInternal(
								"Field exclude parameter \""
									+ this.ruleString
									+ "\" in \""
									+ this.getCommandName()
									+ "\" statement at line "
									+ this.getLineNumber()
									+ "\" has modifiers that are invalid for fields : "
									+ this.joinWithAnd(invalidModifierNames)
									+ ". They will be ignored.",
								true
							);
					}

					if (this.isMemberVolatile() && this.isMemberFinal()) {
						this.setEnabled(false);
						this.scriptContext
							.logWarningInternal(
								"Field exclude parameter \""
									+ this.ruleString
									+ "\" in \""
									+ this.getCommandName()
									+ "\" statement at line "
									+ this.getLineNumber()
									+ "\" has the invalid combination of modifiers : final and volatile. The parameter will be ignored.",
								true
							);
					}
					break;
				case 4:
					if (this.isMemberTransient()) {
						invalidModifierNames.addElement("transient");
					}

					if (this.isMemberTransient()) {
						invalidModifierNames.addElement("enum");
					}

					if (invalidModifierNames.size() > 0) {
						this.setEnabled(false);
						this.scriptContext
							.logWarningInternal(
								"Method exclude parameter \""
									+ this.ruleString
									+ "\" in \""
									+ this.getCommandName()
									+ "\" statement at line "
									+ this.getLineNumber()
									+ "\" has modifiers that are invalid for methods : "
									+ this.joinWithAnd(invalidModifierNames)
									+ ". They will be ignored.",
								true
							);
					}

					if (this.isMemberAbstract() && this.isMemberPrivate()) {
						this.setEnabled(false);
						this.scriptContext
							.logWarningInternal(
								"Method exclude parameter \""
									+ this.ruleString
									+ "\" in \""
									+ this.getCommandName()
									+ "\" statement at line "
									+ this.getLineNumber()
									+ "\" has the invalid combination of modifiers : abstract and private. The parameter will be ignored.",
								true
							);
					}

					if (this.isMemberAbstract() && this.isMemberStatic()) {
						this.setEnabled(false);
						this.scriptContext
							.logWarningInternal(
								"Method exclude parameter \""
									+ this.ruleString
									+ "\" in \""
									+ this.getCommandName()
									+ "\" statement at line "
									+ this.getLineNumber()
									+ "\" has the invalid combination of modifiers : abstract and static. The parameter will be ignored.",
								true
							);
					}

					if (this.isMemberAbstract() && this.isMemberFinal()) {
						this.setEnabled(false);
						this.scriptContext
							.logWarningInternal(
								"Method exclude parameter \""
									+ this.ruleString
									+ "\" in \""
									+ this.getCommandName()
									+ "\" statement at line "
									+ this.getLineNumber()
									+ "\" has the invalid combination of modifiers : abstract and final. The parameter will be ignored.",
								true
							);
					}

					if (this.isMemberAbstract() && this.isMemberNative()) {
						this.setEnabled(false);
						this.scriptContext
							.logWarningInternal(
								"Method exclude parameter \""
									+ this.ruleString
									+ "\" in \""
									+ this.getCommandName()
									+ "\" statement at line "
									+ this.getLineNumber()
									+ "\" has the invalid combination of modifiers : abstract and native. The parameter will be ignored.",
								true
							);
					}

					if (this.isMemberAbstract() && this.isMemberSynchronized()) {
						this.setEnabled(false);
						this.scriptContext
							.logWarningInternal(
								"Method exclude parameter \""
									+ this.ruleString
									+ "\" in \""
									+ this.getCommandName()
									+ "\" statement at line "
									+ this.getLineNumber()
									+ "\" has the invalid combination of modifiers : abstract and synchronized. The parameter will be ignored.",
								true
							);
					}
			}

			if (this.memberAccessFlags.hasConflictingModifiers()) {
				this.setEnabled(false);
				this.scriptContext
					.logWarningInternal(
						"Member exclude parameter \""
							+ this.ruleString
							+ "\" in \""
							+ this.getCommandName()
							+ "\" statement at line "
							+ this.getLineNumber()
							+ "\" has a contradictory combination of modifiers. The parameter will be ignored.",
						true
					);
			}
		}
	}

	private void excludeSerializedClasses(SerializedClassManager serializedClassManager, Enumeration<ClassNode> classes) {
		Set<ClassNode> annotationClasses = null;
		if (this.isSearchByAnnotation()) {
			boolean matchOnOriginal = Options.ZKM_MATCH_ON_ORIGINAL && (serializedClassManager.hasHierarchySnapshot() || serializedClassManager.isFieldNameObfFinish() || serializedClassManager.isMethodNameObfFinish());
			annotationClasses = this.findClassesByAnnotation(serializedClassManager.getWorkspaceClasses(), matchOnOriginal);
		}

		while (classes.hasMoreElements()) {
			ClassNode classNode = classes.nextElement();
			String internalName = classNode.getInternalName();
			String className = classNode.getClassName();
			String packageName = ExcludeStatementManager.getPackageNameForMatching(classNode);
			String simpleName = ExcludeStatementManager.getSimpleNameForMatching(classNode);
			boolean useOriginalName = Options.ZKM_MATCH_ON_ORIGINAL && serializedClassManager.hasHierarchySnapshot();

			try {
				if (this.matchesClass(classNode, annotationClasses, packageName, simpleName, serializedClassManager)
					&& !classNode.isInterface()
					&& (
						this.implementsInterfaceNames.contains("java/io/Serializable")
							|| this.implementsInterfaceNames.contains("java/io/Externalizable")
							|| serializedClassManager.isSubclassOf(useOriginalName ? className : internalName, "java/io/Serializable")
							|| serializedClassManager.isSubclassOf(useOriginalName ? className : internalName, "java/io/Externalizable")
					)) {
					serializedClassManager.excludeClass(classNode, "exclusion parameter '" + this.ruleString + "'");
				}
			} catch (ClasspathException classpathException) {
				this.scriptContext
					.logWarning(
						"Error while executing \""
							+ this.getCommandName()
							+ "\" statement at line "
							+ this.getLineNumber()
							+ " while analysing '"
							+ this.ruleString
							+ "' : class '"
							+ GlobalUtils.internalToBinaryName(classpathException.getClassName())
							+ "' not found. (E)"
					);
			} catch (FileException fileException) {
				this.scriptContext
					.logWarning(
						"Error while executing \""
							+ this.getCommandName()
							+ "\" statement at line "
							+ this.getLineNumber()
							+ " while analysing '"
							+ this.ruleString
							+ "' : '"
							+ fileException.getMessage()
							+ "' (E)"
					);
			}
		}
	}

	private void noOpPackageSpecifierCheck() {
		if (this.packageSpecifier != null) {
		}
	}

	private void parseAccessFlags() {
		Iterator<String> accessFlagIterator = this.classAccessFlagStrings.keySet().iterator();
		if (accessFlagIterator.hasNext()) {
			this.classAccessFlags = new AccessFlags(1);
		}

		while (accessFlagIterator.hasNext()) {
			boolean negated = false;
			String modifierName = accessFlagIterator.next();
			if (modifierName.startsWith(AbstractScriptNode.NEGATED_MODIFIER_MARK)) {
				modifierName = modifierName.substring(1);
				negated = true;
			}

			if (negated) {
				if (modifierName.equals("public")) {
					this.classAccessFlags.markNegativePublic();
				} else if (modifierName.equals("package")) {
					this.classAccessFlags.markNegativePackagePrivate();
				} else if (modifierName.equals("final")) {
					this.classAccessFlags.markNegativeFinal();
				} else if (modifierName.equals("interface")) {
					this.classAccessFlags.markNegativeInterface();
				} else if (modifierName.equals("abstract")) {
					this.classAccessFlags.markNegativeAbstract();
				} else if (modifierName.equals("synthetic")) {
					this.classAccessFlags.markNegativeSynthetic();
				} else if (modifierName.equals("enum")) {
					this.classAccessFlags.markNegativeEnum();
				} else if (modifierName.equals("annotation")) {
					this.classAccessFlags.markNegativeAnnotation();
				}
			} else if (modifierName.equals("public")) {
				this.classAccessFlags.markPositivePublic();
			} else if (modifierName.equals("package")) {
				this.classAccessFlags.markPositivePackagePrivate();
			} else if (modifierName.equals("final")) {
				this.classAccessFlags.markPositiveFinal();
			} else if (modifierName.equals("interface")) {
				this.classAccessFlags.markPositiveInterface();
			} else if (modifierName.equals("abstract")) {
				this.classAccessFlags.markPositiveAbstract();
			} else if (modifierName.equals("synthetic")) {
				this.classAccessFlags.markPositiveSynthetic();
			} else if (modifierName.equals("enum")) {
				this.classAccessFlags.markPositiveEnum();
			} else if (modifierName.equals("annotation")) {
				this.classAccessFlags.markPositiveAnnotation();
			}
		}

		this.memberAccessFlags = parseAccessFlagsFromMap(this.memberAccessFlagStrings, this.targetType);
	}

	public boolean isAnnotationType() {
		return this.isAnnotationType;
	}
}
