package com.zelix.ui.dataset;

import com.zelix.event.ChangeTrackingNotifier;
import com.zelix.exceptions.LexicalException;
import com.zelix.exceptions.ParsingException;
import com.zelix.exceptions.ProcessingException;
import com.zelix.framework.AccessFlags;
import com.zelix.klassmaster.config.Options;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.ScriptParser;
import com.zelix.klassmaster.script.statements.annotation.ComplexAnnotationSpecifier;
import com.zelix.klassmaster.script.statements.clause.ContainingClause;
import com.zelix.klassmaster.script.statements.specifier.RenameFilterParameter;
import com.zelix.utils.GlobalUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class ParameterModel extends ChangeTrackingNotifier {
	private static String nameOfPhrase = "the name of ";
	private List<String> implementsClassNames = new ArrayList<>();
	private List<String> methodArgumentTypeNames = new ArrayList<>();
	private List<String> throwsClassNames = new ArrayList<>();
	private AccessFlags classAccessFlags;
	private AccessFlags memberAccessFlags;
	public String sourceFilePattern;
	private List<String> packageNameParts;
	private boolean packageExactMatch;
	private boolean packageRecursive;
	private List<String> searchPaths;
	private String classNamePattern;
	private boolean classNameRecursive;
	private ContainingClause containingClause;
	private String extendsClassName;
	private String classAnnotationName;
	private String fieldNamePattern;
	private String fieldTypeDescriptor;
	private String methodNamePattern;
	private String methodLinkPattern;
	private String memberAnnotationName;
	private boolean includeSignatureClasses;
	private int targetType;
	public ScriptContext scriptContext;

	public ParameterModel(RenameFilterParameter renameFilterParameter) {
		if (renameFilterParameter.classAccessFlags != null) {
			this.classAccessFlags = (AccessFlags)renameFilterParameter.classAccessFlags.clone();
		}

		if (renameFilterParameter.memberAccessFlags != null) {
			this.memberAccessFlags = (AccessFlags)renameFilterParameter.memberAccessFlags.clone();
		}

		this.sourceFilePattern = renameFilterParameter.sourceFilePattern;
		this.packageNameParts = renameFilterParameter.getPackagePatterns();
		if (renameFilterParameter.packageSpecifier != null) {
			this.packageExactMatch = renameFilterParameter.packageSpecifier.isPackageExactMatch();
			this.packageRecursive = renameFilterParameter.packageSpecifier.isPackageRecursive();
		}

		if (renameFilterParameter.packageSpecifier != null) {
		}

		this.searchPaths = GlobalUtils.clone(renameFilterParameter.searchPaths);
		if (renameFilterParameter.linkClassNamePattern != null) {
			this.classNamePattern = renameFilterParameter.linkClassNamePattern;
		} else if (renameFilterParameter.classSpecifier != null) {
			this.classNamePattern = renameFilterParameter.classSpecifier.toRuleString();
			this.classNameRecursive = renameFilterParameter.classSpecifier.isRecursive();
			if (this.classNameRecursive) {
				this.classNamePattern = this.classNamePattern.substring(0, this.classNamePattern.length() - 1);
			}
		}

		if (renameFilterParameter.containingClause != null) {
			this.containingClause = renameFilterParameter.containingClause;
		}

		if (renameFilterParameter.extendsClassName != null) {
			this.extendsClassName = RenameFilterParameter.internalToBinaryName(renameFilterParameter.extendsClassName);
		}

		if (renameFilterParameter.implementsInterfaceNames != null) {
			for (int interfaceIndex = 0; interfaceIndex < renameFilterParameter.implementsInterfaceNames.size(); interfaceIndex++) {
				String interfaceName = renameFilterParameter.implementsInterfaceNames.get(interfaceIndex);
				this.implementsClassNames.add(RenameFilterParameter.internalToBinaryName(interfaceName));
			}
		}

		if (renameFilterParameter.classAnnotationSpecifier != null) {
			this.classAnnotationName = renameFilterParameter.classAnnotationSpecifier.toRuleString();
			if (this.classAnnotationName.startsWith("@")) {
				this.classAnnotationName = this.classAnnotationName.substring(1);
			}
		}

		if (renameFilterParameter.fieldSpecifier != null) {
			this.fieldNamePattern = renameFilterParameter.fieldSpecifier.toRuleString();
		}

		if (renameFilterParameter.fieldTypeDescriptor != null) {
			this.fieldTypeDescriptor = RenameFilterParameter.descriptorToHumanReadable(renameFilterParameter.fieldTypeDescriptor);
		}

		if (renameFilterParameter.methodSpecifier != null) {
			this.methodNamePattern = renameFilterParameter.methodSpecifier.toRuleString();
		}

		this.methodLinkPattern = renameFilterParameter.linkMethodNamePattern;
		if (renameFilterParameter.methodArgumentSpecifier != null && renameFilterParameter.methodArgumentSpecifier.rawDescriptor.length() > 0) {
			String methodArgumentsText = RenameFilterParameter.formatMethodArguments(renameFilterParameter.methodArgumentSpecifier, (ComplexAnnotationSpecifier[])null);
			StringTokenizer methodArgumentsTokenizer = new StringTokenizer(methodArgumentsText, ",");

			while (methodArgumentsTokenizer.hasMoreTokens()) {
				this.methodArgumentTypeNames.add(methodArgumentsTokenizer.nextToken());
			}
		}

		if (renameFilterParameter.memberAnnotationSpecifier != null) {
			this.memberAnnotationName = renameFilterParameter.memberAnnotationSpecifier.toRuleString();
			if (this.memberAnnotationName.startsWith("@")) {
				this.memberAnnotationName = this.memberAnnotationName.substring(1);
			}
		}

		if (renameFilterParameter.throwsExceptionNames != null) {
			for (int throwsIndex = 0; throwsIndex < renameFilterParameter.throwsExceptionNames.size(); throwsIndex++) {
				String throwsExceptionName = renameFilterParameter.throwsExceptionNames.get(throwsIndex);
				this.throwsClassNames.add(RenameFilterParameter.internalToBinaryName(throwsExceptionName));
			}
		}

		this.includeSignatureClasses = renameFilterParameter.includeSignatureClasses;
		this.targetType = renameFilterParameter.targetType;
		this.scriptContext = renameFilterParameter.scriptContext;
	}

	public static void parseRuleString(ScriptContext scriptContext, String ruleString) throws IOException {
		BufferedReader ruleReader = new BufferedReader(new StringReader(ruleString + ";"));
		ScriptParser scriptParser = new ScriptParser(ruleReader);

		try {
			scriptParser.parseSingleRenameFilterParameter().parseArguments(null, scriptContext);
		} catch (ParsingException parsingException) {
			throw new ProcessingException(parsingException.getMessage());
		} catch (LexicalException lexicalException) {
			throw new ProcessingException(lexicalException.getMessage());
		}
	}

	public void markClassPackagePrivate() {
		if (this.classAccessFlags == null) {
			this.classAccessFlags = new AccessFlags(1);
		}

		this.classAccessFlags.markPositivePackagePrivate();
		this.markChanged();
		this.notifySimpleChange();
	}

	public String getPackageNamePrefix() {
		StringBuffer packageNameBuffer = new StringBuffer();
		int packagePartCount = this.packageNameParts.size();
		if (packagePartCount > 0) {
			for (int i = 0; i < packagePartCount; i++) {
				packageNameBuffer.append(this.packageNameParts.get(i) + ".");
			}
		}

		return packageNameBuffer.toString();
	}

	public void setMemberNotSynchronized(boolean selected, int memberType) {
		if (this.memberAccessFlags == null) {
			this.memberAccessFlags = new AccessFlags(memberType);
		}

		this.memberAccessFlags.setNegativeSynchronized(selected);
		this.markChanged();
		this.notifySimpleChange();
	}

	public void clearClassPackagePrivate() {
		if (this.classAccessFlags == null) {
			this.classAccessFlags = new AccessFlags(1);
		}

		this.classAccessFlags.clearPositivePackagePrivate();
		this.markChanged();
		this.notifySimpleChange();
	}

	public boolean isClassPackagePrivate() {
		return this.classAccessFlags != null && this.classAccessFlags.isDefaultAccess();
	}

	public void setExtendsClassName(String extendsClassName) {
		this.extendsClassName = extendsClassName;
		this.markChanged();
		this.notifySimpleChange();
	}

	public void setClassInterface(boolean selected) {
		if (this.classAccessFlags == null) {
			this.classAccessFlags = new AccessFlags(1);
		}

		this.classAccessFlags.setPositiveInterface(selected);
		this.markChanged();
		this.notifySimpleChange();
	}

	public ScriptContext getScriptContext() {
		return this.scriptContext;
	}

	public void setMemberAbstract(boolean selected, int memberType) {
		if (this.memberAccessFlags == null) {
			this.memberAccessFlags = new AccessFlags(memberType);
		}

		this.memberAccessFlags.setPositiveAbstract(selected);
		this.markChanged();
		this.notifySimpleChange();
	}

	public boolean isClassLinkPattern() {
		return this.targetType == 2 && this.classNamePattern.contains("<link>");
	}

	public void markClassPublic() {
		if (this.classAccessFlags == null) {
			this.classAccessFlags = new AccessFlags(1);
		}

		this.classAccessFlags.markPositivePublic();
		this.markChanged();
		this.notifySimpleChange();
	}

	public void setClassNotAbstract(boolean selected) {
		if (this.classAccessFlags == null) {
			this.classAccessFlags = new AccessFlags(1);
		}

		this.classAccessFlags.setNegativeAbstract(selected);
		this.markChanged();
		this.notifySimpleChange();
	}

	public void setFieldNamePattern(String fieldNamePattern) {
		this.fieldNamePattern = fieldNamePattern;
		this.markChanged();
		this.notifySimpleChange();
	}

	public String getContainingClauseText() {
		return this.containingClause != null ? this.containingClause.toRuleString().substring(10) : "";
	}

	public void setClassNotInterface(boolean selected) {
		if (this.classAccessFlags == null) {
			this.classAccessFlags = new AccessFlags(1);
		}

		this.classAccessFlags.setNegativeInterface(selected);
		this.markChanged();
		this.notifySimpleChange();
	}

	public void setFieldTypeDescriptor(String fieldTypeDescriptor) {
		this.fieldTypeDescriptor = fieldTypeDescriptor;
		this.markChanged();
		this.notifySimpleChange();
	}

	public String getMemberAnnotationName() {
		return this.memberAnnotationName != null ? this.memberAnnotationName : "";
	}

	public String getExplanation(int parameterKind) {
		String excludePrefix;
		String alsoExcludePrefix;
		switch (parameterKind) {
			case 1:
				excludePrefix = "Exclude the name of ";
				alsoExcludePrefix = "Also exclude the name of ";
				break;
			case 2:
				excludePrefix = "Exclude ";
				alsoExcludePrefix = "Also exclude ";
				break;
			case 3:
				excludePrefix = "Exclude references to ";
				alsoExcludePrefix = "Also exclude references to ";
				break;
			default:
				excludePrefix = "Exclude the name of ";
				alsoExcludePrefix = "Also exclude the name of ";
		}

		int itemNumber = 1;
		char itemLetter = 'a';
		StringBuilder explanationBuilder = new StringBuilder();
		switch (this.targetType) {
			case 1:
				explanationBuilder.append(excludePrefix + "all packages:" + Options.lineSeparator);
				if (this.packageNameParts != null && this.packageNameParts.size() > 0) {
					StringBuilder packageMatchBuilder = new StringBuilder().append("\t");
					itemNumber++;
					explanationBuilder.append(packageMatchBuilder.append(1).append(") that match \"").append(this.getPackageNamePrefix()).append("\"").append(Options.lineSeparator).toString());
				}
				break;
			case 2:
				if (this.isClassLinkPattern()) {
					if (this.hasClassNamePrefix()) {
						explanationBuilder.append(
							"Exclude the prefix \""
								+ this.getClassNamePrefix()
								+ "\" and the suffix \""
								+ this.getClassNameSuffix()
								+ "\" from being renamed and link the non-prefix and non-suffix parts of the name in all classes:"
								+ Options.lineSeparator
						);
					} else {
						explanationBuilder.append(
							"Exclude the suffix \"" + this.getClassNameSuffix() + "\" from being renamed and link the non-suffix part of the name in all classes:" + Options.lineSeparator
						);
					}

					if (this.classAnnotationName != null) {
						StringBuilder linkClassAnnotationBuilder = new StringBuilder().append("\t");
						itemNumber++;
						explanationBuilder.append(
							linkClassAnnotationBuilder.append(1)
								.append(") that are annotated by a class matching \"")
								.append(this.classAnnotationName)
								.append("\"")
								.append(Options.lineSeparator)
								.toString()
						);
					}

					if (this.packageNameParts != null && this.packageNameParts.size() > 0) {
						explanationBuilder.append("\t" + itemNumber++ + ") that are in a package that matches \"" + this.getPackageNamePrefix() + "\"" + Options.lineSeparator);
					} else {
						explanationBuilder.append("\t" + itemNumber++ + ") that are not in a package" + Options.lineSeparator);
					}

					if (this.hasClassNamePrefix()) {
						explanationBuilder.append(
							"\t" + itemNumber++ + ") with a name that starts with \"" + this.getClassNamePrefix() + "\" and ends with \"" + this.getClassNameSuffix() + "\"" + Options.lineSeparator
						);
					} else {
						explanationBuilder.append("\t" + itemNumber++ + ") with a name that ends with \"" + this.getClassNameSuffix() + "\"" + Options.lineSeparator);
					}

					String linkedClassExtendsText = this.getExtendsClassName();
					if (linkedClassExtendsText.length() > 0) {
						explanationBuilder.append("\t" + itemNumber++ + ") that extend \"" + linkedClassExtendsText + "\"" + Options.lineSeparator);
					}

					String linkedClassImplementsText = this.getImplementsClassNames();
					if (linkedClassImplementsText.length() > 0) {
						explanationBuilder.append("\t" + itemNumber++ + ") that implement \"" + linkedClassImplementsText + "\"" + Options.lineSeparator);
					}
				} else {
					explanationBuilder.append(excludePrefix + "all classes:" + Options.lineSeparator);
					if (this.classAnnotationName != null) {
						StringBuilder classAnnotationBuilder = new StringBuilder().append("\t");
						itemNumber++;
						explanationBuilder.append(
							classAnnotationBuilder.append(1)
								.append(") that are annotated by a class matching \"")
								.append(this.classAnnotationName)
								.append("\"")
								.append(Options.lineSeparator)
								.toString()
						);
					}

					String classAccessFlagsText = this.getClassAccessFlagsString();
					if (classAccessFlagsText.length() > 0) {
						explanationBuilder.append("\t" + itemNumber++ + ") that are \"" + classAccessFlagsText + "\"" + Options.lineSeparator);
					}

					if (this.packageNameParts != null && this.packageNameParts.size() > 0) {
						explanationBuilder.append("\t" + itemNumber++ + ") that are in a package that matches \"" + this.getPackageNamePrefix() + "\"" + Options.lineSeparator);
					} else {
						explanationBuilder.append("\t" + itemNumber++ + ") that are not in a package" + Options.lineSeparator);
					}

					explanationBuilder.append("\t" + itemNumber++ + ") with a name that matches \"" + this.classNamePattern + "\"" + Options.lineSeparator);
					String containingClauseText = this.getContainingClauseText();
					if (containingClauseText.length() > 0) {
						explanationBuilder.append("\t" + itemNumber++ + ") that contains fields or methods matching \"" + containingClauseText + "\"" + Options.lineSeparator);
					}

					String classExtendsText = this.getExtendsClassName();
					if (classExtendsText.length() > 0) {
						explanationBuilder.append("\t" + itemNumber++ + ") that extend \"" + classExtendsText + "\"" + Options.lineSeparator);
					}

					String classImplementsText = this.getImplementsClassNames();
					if (classImplementsText.length() > 0) {
						explanationBuilder.append("\t" + itemNumber++ + ") that implement \"" + classImplementsText + "\"" + Options.lineSeparator);
					}
				}

				if (this.packageNameParts != null && this.packageNameParts.size() > 0 && this.packageRecursive) {
					explanationBuilder.append(alsoExcludePrefix + "any package containing a matching class" + Options.lineSeparator);
				}
				break;
			case 3:
				explanationBuilder.append(excludePrefix + "all fields:" + Options.lineSeparator);
				String fieldAccessFlagsText = this.getMemberAccessFlagsString();
				if (this.memberAnnotationName != null) {
					StringBuilder fieldAnnotationBuilder = new StringBuilder().append("\t");
					itemNumber++;
					explanationBuilder.append(
						fieldAnnotationBuilder.append(1)
							.append(") that are annotated by a class matching \"")
							.append(this.memberAnnotationName)
							.append("\"")
							.append(Options.lineSeparator)
							.toString()
					);
				}

				if (fieldAccessFlagsText.length() > 0) {
					explanationBuilder.append("\t" + itemNumber++ + ") that are \"" + fieldAccessFlagsText + "\"" + Options.lineSeparator);
				}

				if (this.fieldTypeDescriptor != null && this.fieldTypeDescriptor.length() > 0) {
					explanationBuilder.append("\t" + itemNumber++ + ") that are of type \"" + this.fieldTypeDescriptor + "\"" + Options.lineSeparator);
				}

				explanationBuilder.append("\t" + itemNumber++ + ") that have a name matching \"" + this.fieldNamePattern + "\"" + Options.lineSeparator);
				explanationBuilder.append("\t" + itemNumber++ + ") that are contained within a class:" + Options.lineSeparator);
				if (this.classAnnotationName != null) {
					StringBuilder fieldOwnerAnnotationBuilder = new StringBuilder().append("\t\t");
					itemLetter = 'b';
					explanationBuilder.append(
						fieldOwnerAnnotationBuilder.append('a')
							.append(") that is annotated by a class matching \"")
							.append(this.classAnnotationName)
							.append("\"")
							.append(Options.lineSeparator)
							.toString()
					);
				}

				String fieldOwnerAccessText = this.getClassAccessFlagsString();
				if (fieldOwnerAccessText.length() > 0) {
					explanationBuilder.append("\t\t" + itemLetter++ + ") that is \"" + fieldOwnerAccessText + "\"" + Options.lineSeparator);
				}

				if (this.packageNameParts != null && this.packageNameParts.size() > 0) {
					explanationBuilder.append("\t\t" + itemLetter++ + ") that is in a package that matches \"" + this.getPackageNamePrefix() + "\"" + Options.lineSeparator);
				} else {
					explanationBuilder.append("\t\t" + itemLetter++ + ") that is not in a package" + Options.lineSeparator);
				}

				explanationBuilder.append("\t\t" + itemLetter++ + ") with a name that matches \"" + this.classNamePattern + "\"" + Options.lineSeparator);
				String fieldOwnerExtendsText = this.getExtendsClassName();
				if (fieldOwnerExtendsText.length() > 0) {
					explanationBuilder.append("\t\t" + itemLetter++ + ") that extends \"" + fieldOwnerExtendsText + "\"" + Options.lineSeparator);
				}

				String fieldOwnerImplementsText = this.getImplementsClassNames();
				if (fieldOwnerImplementsText.length() > 0) {
					StringBuilder fieldOwnerImplementsBuilder = new StringBuilder().append("\t\t");
					char discardedNextFieldLetter = (char)(itemLetter + 1);
					explanationBuilder.append(fieldOwnerImplementsBuilder.append(itemLetter).append(") that implements \"").append(fieldOwnerImplementsText).append("\"").append(Options.lineSeparator).toString());
				}

				if (this.packageNameParts != null && this.packageNameParts.size() > 0 && this.packageRecursive) {
					explanationBuilder.append(alsoExcludePrefix + "any package containing a class containing a matching field" + Options.lineSeparator);
				}

				if (this.classNameRecursive) {
					explanationBuilder.append(alsoExcludePrefix + "any class containing a matching field" + Options.lineSeparator);
				}
				break;
			case 4:
				if (this.isMethodLinkPattern()) {
					explanationBuilder.append("Exclude the suffix \"" + this.getLinkPrefix(this.methodLinkPattern) + "\" from being renamed in all methods:" + Options.lineSeparator);
				} else {
					explanationBuilder.append(excludePrefix + "all methods:" + Options.lineSeparator);
				}

				if (this.memberAnnotationName != null) {
					StringBuilder methodAnnotationBuilder = new StringBuilder().append("\t");
					itemNumber++;
					explanationBuilder.append(
						methodAnnotationBuilder.append(1)
							.append(") that are annotated by a class matching \"")
							.append(this.memberAnnotationName)
							.append("\"")
							.append(Options.lineSeparator)
							.toString()
					);
				}

				String methodAccessFlagsText = this.getMemberAccessFlagsString();
				if (methodAccessFlagsText.length() > 0) {
					explanationBuilder.append("\t" + itemNumber++ + ") that are \"" + methodAccessFlagsText + "\"" + Options.lineSeparator);
				}

				if (this.methodLinkPattern != null) {
					explanationBuilder.append("\t" + itemNumber++ + ") that have a name matching starting with \"" + this.getLinkPrefix(this.methodLinkPattern) + "\"" + Options.lineSeparator);
				} else {
					explanationBuilder.append("\t" + itemNumber++ + ") that have a name matching \"" + this.methodNamePattern + "\"" + Options.lineSeparator);
				}

				String methodArgumentsText = this.getMethodArgumentTypeNames();
				if (methodArgumentsText.length() > 0) {
					if (methodArgumentsText.equals("*")) {
						explanationBuilder.append("\t" + itemNumber++ + ") that have any argument types" + Options.lineSeparator);
					} else {
						explanationBuilder.append("\t" + itemNumber++ + ") that have arguments matching \"" + methodArgumentsText + "\"" + Options.lineSeparator);
					}
				} else {
					explanationBuilder.append("\t" + itemNumber++ + ") that have no arguments" + Options.lineSeparator);
				}

				String throwsClassNamesText = this.getThrowsClassNames();
				if (throwsClassNamesText.length() > 0) {
					explanationBuilder.append("\t" + itemNumber++ + ") that throw \"" + throwsClassNamesText + "\"" + Options.lineSeparator);
				}

				explanationBuilder.append("\t" + itemNumber++ + ") that are contained within a class:" + Options.lineSeparator);
				if (this.classAnnotationName != null) {
					StringBuilder methodOwnerAnnotationBuilder = new StringBuilder().append("\t\t");
					itemLetter = 'b';
					explanationBuilder.append(
						methodOwnerAnnotationBuilder.append('a')
							.append(") that is annotated by a class matching \"")
							.append(this.classAnnotationName)
							.append("\"")
							.append(Options.lineSeparator)
							.toString()
					);
				}

				String methodOwnerAccessText = this.getClassAccessFlagsString();
				if (methodOwnerAccessText.length() > 0) {
					explanationBuilder.append("\t\t" + itemLetter++ + ") that is \"" + methodOwnerAccessText + "\"" + Options.lineSeparator);
				}

				if (this.packageNameParts != null && this.packageNameParts.size() > 0) {
					explanationBuilder.append("\t\t" + itemLetter++ + ") that is in a package that matches \"" + this.getPackageNamePrefix() + "\"" + Options.lineSeparator);
				} else {
					explanationBuilder.append("\t\t" + itemLetter++ + ") that is not in a package" + Options.lineSeparator);
				}

				explanationBuilder.append("\t\t" + itemLetter++ + ") with a name that matches \"" + this.classNamePattern + "\"" + Options.lineSeparator);
				String methodOwnerExtendsText = this.getExtendsClassName();
				if (methodOwnerExtendsText.length() > 0) {
					explanationBuilder.append("\t\t" + itemLetter++ + ") that extends \"" + methodOwnerExtendsText + "\"" + Options.lineSeparator);
				}

				String methodOwnerImplementsText = this.getImplementsClassNames();
				if (methodOwnerImplementsText.length() > 0) {
					StringBuilder methodOwnerImplementsBuilder = new StringBuilder().append("\t\t");
					char discardedNextMethodLetter = (char)(itemLetter + 1);
					explanationBuilder.append(methodOwnerImplementsBuilder.append(itemLetter).append(") that implements \"").append(methodOwnerImplementsText).append("\"").append(Options.lineSeparator).toString());
				}

				if (this.packageNameParts != null && this.packageNameParts.size() > 0 && this.packageRecursive) {
					explanationBuilder.append(alsoExcludePrefix + "any package containing a class containing a matching method" + Options.lineSeparator);
				}

				if (this.classNameRecursive) {
					explanationBuilder.append(alsoExcludePrefix + "any class containing a matching method" + Options.lineSeparator);
				}

				if (this.isMethodLinkPattern()) {
					explanationBuilder.append("Also rename the non-prefix part of the method name to match that of any corresponding field name in the class" + Options.lineSeparator);
				}

				if (this.includeSignatureClasses) {
					explanationBuilder.append(alsoExcludePrefix + "any class that appears in the method arguments" + Options.lineSeparator);
				}
		}

		if (parameterKind == 3) {
			explanationBuilder.append(
				"Note that the ZKM Script language also allows you to specify the reference obfuscation"
					+ Options.lineSeparator
					+ "based upon the methods from which the references are made."
					+ Options.lineSeparator
			);
		}

		return explanationBuilder.toString();
	}

	public AccessFlags getMemberAccessFlags() {
		return this.memberAccessFlags;
	}

	public void setMemberTransient(boolean selected, int memberType) {
		if (this.memberAccessFlags == null) {
			this.memberAccessFlags = new AccessFlags(memberType);
		}

		this.memberAccessFlags.setPositiveTransient(selected);
		this.markChanged();
		this.notifySimpleChange();
	}

	public void markMemberPrivate(int memberType) {
		if (this.memberAccessFlags == null) {
			this.memberAccessFlags = new AccessFlags(memberType);
		}

		this.memberAccessFlags.markPositivePrivate();
		this.markChanged();
		this.notifySimpleChange();
	}

	public int getTargetType() {
		return this.targetType;
	}

	public void setThrowsClassNames(String throwsClassNames) {
		StringTokenizer throwsClassNamesTokenizer = new StringTokenizer(throwsClassNames, ",");
		this.throwsClassNames = new ArrayList<>();

		while (throwsClassNamesTokenizer.hasMoreTokens()) {
			this.throwsClassNames.add(throwsClassNamesTokenizer.nextToken().trim());
		}

		this.markChanged();
		this.notifySimpleChange();
	}

	public boolean hasClassNamePrefix() {
		return this.classNamePattern.indexOf("<link>") > 0;
	}

	public void setMemberNotFinal(boolean selected, int memberType) {
		if (this.memberAccessFlags == null) {
			this.memberAccessFlags = new AccessFlags(memberType);
		}

		this.memberAccessFlags.setNegativeFinal(selected);
		this.markChanged();
		this.notifySimpleChange();
	}

	public void setMemberNotTransient(boolean selected, int memberType) {
		if (this.memberAccessFlags == null) {
			this.memberAccessFlags = new AccessFlags(memberType);
		}

		this.memberAccessFlags.setNegativeTransient(selected);
		this.markChanged();
		this.notifySimpleChange();
	}

	public void clearMemberPackagePrivate(int memberType) {
		if (this.memberAccessFlags == null) {
			this.memberAccessFlags = new AccessFlags(memberType);
		}

		this.memberAccessFlags.clearPositivePackagePrivate();
		this.markChanged();
		this.notifySimpleChange();
	}

	public void setMemberNative(boolean selected, int memberType) {
		if (this.memberAccessFlags == null) {
			this.memberAccessFlags = new AccessFlags(memberType);
		}

		this.memberAccessFlags.setPositiveNative(selected);
		this.markChanged();
		this.notifySimpleChange();
	}

	public String getLinkPrefix(String linkPattern) {
		return linkPattern.substring(0, linkPattern.indexOf("<link>"));
	}

	public void setClassNameSuffix(String suffix) {
		this.classNamePattern = "<link>" + suffix;
		this.markChanged();
		this.notifySimpleChange();
	}

	public String getFieldNamePattern() {
		return this.fieldNamePattern;
	}

	public void markMemberPackagePrivate(int memberType) {
		if (this.memberAccessFlags == null) {
			this.memberAccessFlags = new AccessFlags(memberType);
		}

		this.memberAccessFlags.markPositivePackagePrivate();
		this.markChanged();
		this.notifySimpleChange();
	}

	public String getMethodArgumentTypeNames() {
		StringBuffer argumentTypeNamesBuffer = new StringBuffer();
		if (this.methodArgumentTypeNames != null) {
			for (int i = 0; i < this.methodArgumentTypeNames.size(); i++) {
				argumentTypeNamesBuffer.append(this.methodArgumentTypeNames.get(i));
				if (i < this.methodArgumentTypeNames.size() - 1) {
					argumentTypeNamesBuffer.append(", ");
				}
			}
		}

		return argumentTypeNamesBuffer.toString();
	}

	public void setClassFinal(boolean selected) {
		if (this.classAccessFlags == null) {
			this.classAccessFlags = new AccessFlags(1);
		}

		this.classAccessFlags.setPositiveFinal(selected);
		this.markChanged();
		this.notifySimpleChange();
	}

	public void setClassAnnotationName(String annotationName) {
		if (annotationName.startsWith("@")) {
			annotationName = annotationName.substring(1);
		}

		this.classAnnotationName = annotationName;
		this.markChanged();
		this.notifySimpleChange();
	}

	public void setPackageNameParts(String packageName) {
		StringTokenizer packageNameTokenizer = new StringTokenizer(packageName, ".");
		this.packageNameParts = new ArrayList<>();

		while (packageNameTokenizer.hasMoreTokens()) {
			String packagePart = packageNameTokenizer.nextToken().trim();
			this.packageNameParts.add(packagePart);
		}

		this.markChanged();
		this.notifySimpleChange();
	}

	public void setPackageRecursive(boolean recursive) {
		this.packageRecursive = recursive;
		this.markChanged();
		this.notifySimpleChange();
	}

	public void setClassAbstract(boolean selected) {
		if (this.classAccessFlags == null) {
			this.classAccessFlags = new AccessFlags(1);
		}

		this.classAccessFlags.setPositiveAbstract(selected);
		this.markChanged();
		this.notifySimpleChange();
	}

	public void markMemberPublic(int memberType) {
		if (this.memberAccessFlags == null) {
			this.memberAccessFlags = new AccessFlags(memberType);
		}

		this.memberAccessFlags.markPositivePublic();
		this.markChanged();
		this.notifySimpleChange();
	}

	public String getClassAccessFlagsString() {
		StringBuffer classAccessFlagsBuffer = new StringBuffer();
		if (this.isClassPackagePrivate()) {
			classAccessFlagsBuffer.append("package ");
		}

		classAccessFlagsBuffer.append(this.classAccessFlags != null ? this.classAccessFlags.toAccessFlagsString() : "");
		return classAccessFlagsBuffer.toString();
	}

	public void setMethodNamePattern(String methodNamePattern) {
		this.methodNamePattern = methodNamePattern;
		this.markChanged();
		this.notifySimpleChange();
	}

	public void setMemberSynchronized(boolean selected, int memberType) {
		if (this.memberAccessFlags == null) {
			this.memberAccessFlags = new AccessFlags(memberType);
		}

		this.memberAccessFlags.setPositiveSynchronized(selected);
		this.markChanged();
		this.notifySimpleChange();
	}

	public String getClassAnnotationName() {
		return this.classAnnotationName != null ? this.classAnnotationName : "";
	}

	public void setClassNotFinal(boolean selected) {
		if (this.classAccessFlags == null) {
			this.classAccessFlags = new AccessFlags(1);
		}

		this.classAccessFlags.setNegativeFinal(selected);
		this.markChanged();
		this.notifySimpleChange();
	}

	public boolean isPackageRecursive() {
		return this.packageRecursive;
	}

	public String getImplementsClassNames() {
		if (this.implementsClassNames != null && this.implementsClassNames.size() > 0) {
			StringBuffer implementsClassNamesBuffer = new StringBuffer();

			for (int i = 0; i < this.implementsClassNames.size(); i++) {
				implementsClassNamesBuffer.append(this.implementsClassNames.get(i));
				if (i < this.implementsClassNames.size() - 1) {
					implementsClassNamesBuffer.append(", ");
				}
			}

			return implementsClassNamesBuffer.toString();
		} else {
			return "";
		}
	}

	public void markMemberProtected(int memberType) {
		if (this.memberAccessFlags == null) {
			this.memberAccessFlags = new AccessFlags(memberType);
		}

		this.memberAccessFlags.markPositiveProtected();
		this.markChanged();
		this.notifySimpleChange();
	}

	public String getClassNamePrefix() {
		return this.classNamePattern.substring(0, this.classNamePattern.indexOf("<link>"));
	}

	public void setMemberNotNative(boolean selected, int memberType) {
		if (this.memberAccessFlags == null) {
			this.memberAccessFlags = new AccessFlags(memberType);
		}

		this.memberAccessFlags.setNegativeNative(selected);
		this.markChanged();
		this.notifySimpleChange();
	}

	public boolean isMethodLinkPattern() {
		return this.targetType == 4 && this.methodLinkPattern != null;
	}

	public String getFieldTypeDescriptor() {
		return this.fieldTypeDescriptor;
	}

	public void setMethodArgumentTypeNames(String methodArgumentTypeNames) {
		StringTokenizer argumentTypeNamesTokenizer = new StringTokenizer(methodArgumentTypeNames, ",");
		this.methodArgumentTypeNames = new ArrayList<>();

		while (argumentTypeNamesTokenizer.hasMoreTokens()) {
			this.methodArgumentTypeNames.add(argumentTypeNamesTokenizer.nextToken().trim());
		}

		this.markChanged();
		this.notifySimpleChange();
	}

	public void setMemberNotAbstract(boolean selected, int memberType) {
		if (this.memberAccessFlags == null) {
			this.memberAccessFlags = new AccessFlags(memberType);
		}

		this.memberAccessFlags.setNegativeAbstract(selected);
		this.markChanged();
		this.notifySimpleChange();
	}

	public String getMemberAccessFlagsString() {
		StringBuffer memberAccessFlagsBuffer = new StringBuffer();
		if (this.isMemberPackagePrivate()) {
			memberAccessFlagsBuffer.append("package ");
		}

		memberAccessFlagsBuffer.append(this.memberAccessFlags != null ? this.memberAccessFlags.toAccessFlagsString() : "");
		return memberAccessFlagsBuffer.toString();
	}

	public void setClassNameRecursive(boolean recursive) {
		this.classNameRecursive = recursive;
		this.markChanged();
		this.notifySimpleChange();
	}

	public void setImplementsClassNames(String implementsClassNames) {
		StringTokenizer implementsClassNamesTokenizer = new StringTokenizer(implementsClassNames, ",");
		this.implementsClassNames = new ArrayList<>();

		while (implementsClassNamesTokenizer.hasMoreTokens()) {
			this.implementsClassNames.add(implementsClassNamesTokenizer.nextToken().trim());
		}

		this.markChanged();
		this.notifySimpleChange();
	}

	public void setMemberAnnotationName(String annotationName) {
		if (annotationName.startsWith("@")) {
			annotationName = annotationName.substring(1);
		}

		this.memberAnnotationName = annotationName;
		this.markChanged();
		this.notifySimpleChange();
	}

	public void setMemberVolatile(boolean selected, int memberType) {
		if (this.memberAccessFlags == null) {
			this.memberAccessFlags = new AccessFlags(memberType);
		}

		this.memberAccessFlags.setPositiveVolatile(selected);
		this.markChanged();
		this.notifySimpleChange();
	}

	public void setClassNamePattern(String classNamePattern) {
		boolean patternChanged = !this.classNamePattern.equals(classNamePattern);
		this.classNamePattern = classNamePattern;
		if (patternChanged) {
			this.markChanged();
			this.notifySimpleChange();
		}
	}

	public AccessFlags getClassAccessFlags() {
		return this.classAccessFlags;
	}

	public void setMemberNotVolatile(boolean selected, int memberType) {
		if (this.memberAccessFlags == null) {
			this.memberAccessFlags = new AccessFlags(memberType);
		}

		this.memberAccessFlags.setNegativeVolatile(selected);
		this.markChanged();
		this.notifySimpleChange();
	}

	public boolean isClassNameRecursive() {
		return this.classNameRecursive;
	}

	public String getExtendsClassName() {
		return this.extendsClassName != null ? this.extendsClassName : "";
	}

	@Override
	public String toString() {
		return this.getRuleString();
	}

	public String getThrowsClassNames() {
		if (this.throwsClassNames != null && this.throwsClassNames.size() > 0) {
			StringBuffer throwsClassNamesBuffer = new StringBuffer();

			for (int i = 0; i < this.throwsClassNames.size(); i++) {
				throwsClassNamesBuffer.append(this.throwsClassNames.get(i));
				if (i < this.throwsClassNames.size() - 1) {
					throwsClassNamesBuffer.append(", ");
				}
			}

			return throwsClassNamesBuffer.toString();
		} else {
			return "";
		}
	}

	public void setMemberNotStatic(boolean selected, int memberType) {
		if (this.memberAccessFlags == null) {
			this.memberAccessFlags = new AccessFlags(memberType);
		}

		this.memberAccessFlags.setNegativeStatic(selected);
		this.markChanged();
		this.notifySimpleChange();
	}

	public String getMethodNamePattern() {
		return this.methodNamePattern;
	}

	public boolean isMemberPackagePrivate() {
		return this.memberAccessFlags != null && this.memberAccessFlags.isDefaultAccess();
	}

	public void setMemberStatic(boolean selected, int memberType) {
		if (this.memberAccessFlags == null) {
			this.memberAccessFlags = new AccessFlags(memberType);
		}

		this.memberAccessFlags.setPositiveStatic(selected);
		this.markChanged();
		this.notifySimpleChange();
	}

	public String getClassNamePattern() {
		return this.classNamePattern;
	}

	public String getClassNameSuffix() {
		return this.classNamePattern.substring(this.classNamePattern.indexOf("<link>") + 6);
	}

	public final String getRuleString() {
		StringBuilder ruleBuilder = new StringBuilder();
		if (this.classAnnotationName != null && this.classAnnotationName.length() > 0) {
			if (!this.classAnnotationName.startsWith("@") && !this.classAnnotationName.startsWith("(")) {
				ruleBuilder.append('@');
			}

			ruleBuilder.append(this.classAnnotationName);
			ruleBuilder.append(" ");
		}

		if (this.classAccessFlags != null && this.isClassPackagePrivate()) {
			ruleBuilder.append("package ");
		}

		ruleBuilder.append(this.classAccessFlags != null ? this.classAccessFlags.toAccessFlagsString() : "");
		int packagePartCount = this.packageNameParts.size();
		if (this.sourceFilePattern != null) {
			ruleBuilder.append("\"");
			ruleBuilder.append(this.sourceFilePattern);
			ruleBuilder.append("\"");
			ruleBuilder.append("!");
		}

		if (packagePartCount > 0) {
			for (int packagePartIndex = 0; packagePartIndex < packagePartCount; packagePartIndex++) {
				ruleBuilder.append(this.packageNameParts.get(packagePartIndex) + '.');
			}

			if (this.packageExactMatch) {
				ruleBuilder.append(".");
			}

			if (this.packageRecursive) {
				ruleBuilder.append("^");
			}
		}

		if (this.classNamePattern != null) {
			ruleBuilder.append(this.classNamePattern);
			if (this.classNameRecursive) {
				ruleBuilder.append("^");
			}
		}

		if (this.containingClause != null) {
			ruleBuilder.append(' ' + this.containingClause.toRuleString());
		}

		if (this.extendsClassName != null && this.extendsClassName.length() > 0) {
			ruleBuilder.append(" extends " + this.extendsClassName);
		}

		if (this.implementsClassNames.size() > 0) {
			ruleBuilder.append(" implements ");

			for (int interfaceIndex = 0; interfaceIndex < this.implementsClassNames.size(); interfaceIndex++) {
				ruleBuilder.append(this.implementsClassNames.get(interfaceIndex));
				if (interfaceIndex < this.implementsClassNames.size() - 1) {
					ruleBuilder.append(", ");
				}
			}
		}

		if (this.searchPaths.size() > 0) {
			ruleBuilder.append(" search ");

			for (int searchPathIndex = 0; searchPathIndex < this.searchPaths.size(); searchPathIndex++) {
				ruleBuilder.append(this.searchPaths.get(searchPathIndex));
				if (searchPathIndex < this.searchPaths.size() - 1) {
					ruleBuilder.append(", ");
				}
			}
		}

		if (this.memberAnnotationName != null && this.memberAnnotationName.length() > 0) {
			ruleBuilder.append(" ");
			if (!this.memberAnnotationName.startsWith("@") && !this.memberAnnotationName.startsWith("(")) {
				ruleBuilder.append('@');
			}

			ruleBuilder.append(this.memberAnnotationName);
			ruleBuilder.append(" ");
		}

		if (this.memberAccessFlags != null) {
			if (ruleBuilder.length() > 0 && ruleBuilder.charAt(ruleBuilder.length() - 1) != ' ') {
				ruleBuilder.append(" ");
			}

			if (this.isMemberPackagePrivate()) {
				ruleBuilder.append("package ");
			}
		}

		ruleBuilder.append(this.memberAccessFlags != null ? this.memberAccessFlags.toAccessFlagsString() : "");
		if (this.fieldTypeDescriptor != null) {
			if (ruleBuilder.length() > 0 && ruleBuilder.charAt(ruleBuilder.length() - 1) != ' ') {
				ruleBuilder.append(' ');
			}

			ruleBuilder.append(this.fieldTypeDescriptor);
		}

		if (this.fieldNamePattern != null) {
			if (ruleBuilder.length() > 0 && ruleBuilder.charAt(ruleBuilder.length() - 1) != ' ') {
				ruleBuilder.append(' ');
			}

			ruleBuilder.append(this.fieldNamePattern);
		}

		if (this.methodLinkPattern != null || this.methodNamePattern != null) {
			if (ruleBuilder.length() > 0 && ruleBuilder.charAt(ruleBuilder.length() - 1) != ' ') {
				ruleBuilder.append(" ");
			}

			if (this.methodLinkPattern != null) {
				ruleBuilder.append(this.methodLinkPattern);
			} else {
				ruleBuilder.append(this.methodNamePattern);
			}

			ruleBuilder.append('(');
			if (this.methodArgumentTypeNames != null && this.methodArgumentTypeNames.size() > 0) {
				for (int argumentTypeIndex = 0; argumentTypeIndex < this.methodArgumentTypeNames.size(); argumentTypeIndex++) {
					ruleBuilder.append(this.methodArgumentTypeNames.get(argumentTypeIndex));
					if (argumentTypeIndex < this.methodArgumentTypeNames.size() - 1) {
						ruleBuilder.append(", ");
					}
				}
			}

			ruleBuilder.append(')');
			if (this.throwsClassNames.size() > 0) {
				ruleBuilder.append(" throws ");

				for (int throwsIndex = 0; throwsIndex < this.throwsClassNames.size(); throwsIndex++) {
					ruleBuilder.append(this.throwsClassNames.get(throwsIndex));
					if (throwsIndex < this.throwsClassNames.size() - 1) {
						ruleBuilder.append(", ");
					}
				}
			}
		}

		if (this.includeSignatureClasses) {
			ruleBuilder.append(" +signatureClasses");
		}

		return ruleBuilder.toString();
	}

	public boolean excludesClass() {
		return this.targetType == 2 || this.classNameRecursive;
	}

	public void setMemberFinal(boolean selected, int memberType) {
		if (this.memberAccessFlags == null) {
			this.memberAccessFlags = new AccessFlags(memberType);
		}

		this.memberAccessFlags.setPositiveFinal(selected);
		this.markChanged();
		this.notifySimpleChange();
	}
}
