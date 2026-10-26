package com.zelix.tools.proguardConfigurationTranslate.base;

import com.zelix.tools.proguardConfigurationTranslate.ProguardScriptTranslator;
import com.zelix.tools.proguardConfigurationTranslate.commands.If;
import com.zelix.tools.proguardConfigurationTranslate.parser.ClassPattern;
import com.zelix.tools.proguardConfigurationTranslate.parser.ClassSpecification;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public abstract class BasePatternCommand extends KeepCommand {
	public Set<String> memberSpecs = new LinkedHashSet<>();
	public List<ClassPattern> classSpecs = new ArrayList<>();
	public String inheritanceClause;
	public String modifiersClause;
	public String annotationClause;
	public List<BasePatternCommand.TranslatedClassSpec> translatedSpecs;
	public If ifCommand;

	public BasePatternCommand(int commandId) {
		super(commandId);
	}

	private static String toCaretPattern(String classSpec) {
		int specLength = classSpec.length();
		int lastDotIndex = classSpec.lastIndexOf(46);
		if (lastDotIndex > 0 && lastDotIndex < specLength - 1 && classSpec.charAt(lastDotIndex + 1) == ')') {
			lastDotIndex++;
		}

		if (lastDotIndex > 0 && lastDotIndex < specLength - 1) {
			return classSpec.substring(0, lastDotIndex + 1) + "^" + classSpec.substring(lastDotIndex + 1) + "^";
		} else {
			return classSpec.charAt(0) == '.' && classSpec.charAt(specLength - 1) == '.' ? classSpec : "*.^" + classSpec + "^";
		}
	}

	private static String ensurePackageWildcard(String classSpec) {
		return classSpec.indexOf(46) > -1 ? classSpec : "*." + classSpec;
	}

	public void setInheritanceClause(String inheritanceClause) {
		this.inheritanceClause = inheritanceClause;
	}

	public abstract boolean isClassPattern();

	public List<BasePatternCommand.TranslatedClassSpec> translateClassSpecs(ProguardScriptTranslator scriptTranslator) {
		ArrayList<BasePatternCommand.TranslatedClassSpec> translatedSpecs = new ArrayList<>(this.classSpecs.size());

		for (ClassPattern classPattern : this.classSpecs) {
			String translatedPattern = this.translateClassSpec(classPattern.getPattern(), scriptTranslator);
			translatedSpecs.add(new BasePatternCommand.TranslatedClassSpec(translatedPattern, classPattern.isNegated(), classPattern.isInList()));
		}

		return translatedSpecs;
	}

	private String buildContainingConditions() {
		StringBuilder conditionBuffer = new StringBuilder();
		LinkedHashSet<String> conditions = new LinkedHashSet<>();
		if (this.ifCommand != null) {
			ClassSpecification classSpecification = this.ifCommand.getClassSpecification();
			if (classSpecification.hasFieldSpecs()) {
				conditions.addAll(classSpecification.getFieldSpecs());
			}

			if (classSpecification.hasMethodSpecs()) {
				conditions.addAll(classSpecification.getMethodSpecs());
			}
		}

		conditions.addAll(this.memberSpecs);
		int conditionCount = conditions.size();
		int conditionIndex = 0;

		for (String condition : conditions) {
			conditionBuffer.append(condition);
			if (conditionIndex < conditionCount - 1) {
				conditionBuffer.append(" && ");
			}

			conditionIndex++;
		}

		return conditionBuffer.toString();
	}

	public final void addClassSpec(ClassPattern classSpec) {
		this.classSpecs.add(classSpec);
	}

	public void setModifiersClause(String modifiersClause) {
		this.modifiersClause = modifiersClause;
	}

	public void setAnnotationClause(String annotationClause) {
		this.annotationClause = annotationClause;
	}

	public abstract boolean usesContainingClause();

	public void setIfCommand(If ifCommand) {
		this.ifCommand = ifCommand;
	}

	public void addMemberSpec(String memberSpec) {
		this.memberSpecs.add(memberSpec);
	}

	public String translateClassSpec(String classSpec, ProguardScriptTranslator scriptTranslator) {
		boolean linkSubstituted = false;
		ClassSpecification classSpecification = null;
		if (classSpec.indexOf("<") > -1) {
			if (classSpec.equals("<1>")) {
				if (this.ifCommand != null) {
					classSpecification = this.ifCommand.getClassSpecification();
					List<ClassPattern> classPatterns = classSpecification.getClassPatterns();
					if (classPatterns.size() == 1) {
						String firstPattern = classPatterns.get(0).getPattern();
						if (firstPattern.equals("*.*")) {
							classSpec = "*.*";
						} else if (firstPattern.startsWith("*")) {
							String patternAfterStar = firstPattern.substring(1);
							if (patternAfterStar.length() > 0) {
								classSpec = "<link>" + patternAfterStar;
								linkSubstituted = true;
							}
						} else {
							scriptTranslator.logWarning(
								"Does not support more complicated <n> syntax in '" + this.getProguardCommandName() + "' at line " + this.getLineNumber() + ". : '" + classSpec + "' : '" + firstPattern + "'. (A)"
							);
						}
					} else {
						scriptTranslator.logWarning(
							"Does not support more complicated <n> syntax in '"
								+ this.getProguardCommandName()
								+ "' at line "
								+ this.getLineNumber()
								+ ". : '"
								+ classSpec
								+ "' : "
								+ this.ifCommand.translatedSpecs.size()
								+ " (B)"
						);
					}
				} else {
					scriptTranslator.logWarning("<n> syntax in '" + this.getProguardCommandName() + "' at line " + this.getLineNumber() + " with no preceding '-if' command. : '" + classSpec + "'. (B)");
				}
			} else {
				scriptTranslator.logWarning("Does not support more complicated <n> syntax in '" + this.getProguardCommandName() + "' at line " + this.getLineNumber() + ". : '" + classSpec + "'. (C)");
			}
		}

		StringBuilder specBuffer = new StringBuilder();
		if (classSpecification != null && classSpecification.hasClassAnnotation()) {
			String classAnnotation = classSpecification.getClassAnnotation();
			if (linkSubstituted) {
				scriptTranslator.logWarning(
					"Does not support more complicated <n> syntax in '" + this.getProguardCommandName() + "' at line " + this.getLineNumber() + ". : '" + classAnnotation + "' : '" + classSpec + "'. (D)"
				);
			}

			specBuffer.append(classAnnotation);
		}

		if (this.annotationClause != null) {
			if (linkSubstituted) {
				scriptTranslator.logWarning(
					"Does not support more complicated <n> syntax in '" + this.getProguardCommandName() + "' at line " + this.getLineNumber() + ". : '" + this.annotationClause + "' : '" + classSpec + "'. (E)"
				);
			}

			specBuffer.append(this.annotationClause);
		}

		if (classSpecification != null && classSpecification.hasClassModifiers()) {
			String classModifiers = classSpecification.getClassModifiers();
			if (linkSubstituted) {
				scriptTranslator.logWarning(
					"Does not support more complicated <n> syntax in '" + this.getProguardCommandName() + "' at line " + this.getLineNumber() + ". : '" + classSpec + "' : '" + classModifiers + "'. (F)"
				);
			} else {
				specBuffer.append(classModifiers);
			}
		}

		if (this.modifiersClause != null) {
			if (linkSubstituted) {
				scriptTranslator.logWarning(
					"Does not support more complicated <n> syntax in '" + this.getProguardCommandName() + "' at line " + this.getLineNumber() + ". : '" + classSpec + "' : '" + this.modifiersClause + "'. (G)"
				);
			} else {
				specBuffer.append(this.modifiersClause);
			}
		}

		if (this.isClassPattern()) {
			specBuffer.append(toCaretPattern(classSpec));
			if (this.memberSpecs.size() == 0 && (classSpecification == null || !classSpecification.hasFieldSpecs() && !classSpecification.hasMethodSpecs())) {
				specBuffer.setLength(specBuffer.length() - 1);
			}
		} else {
			specBuffer.append(ensurePackageWildcard(classSpec));
		}

		int ifMemberSpecCount = classSpecification != null ? classSpecification.getFieldSpecs().size() + classSpecification.getMethodSpecs().size() : 0;
		int memberSpecCount = this.memberSpecs.size();
		if (this.usesContainingClause() && memberSpecCount > 0 || ifMemberSpecCount > 0) {
			if (linkSubstituted) {
				scriptTranslator.logWarning("Does not support more complicated <n> syntax in '" + this.getProguardCommandName() + "' at line " + this.getLineNumber() + ". : '" + classSpec + "'. (H)");
			} else {
				specBuffer.append(" containing {");
				if (memberSpecCount > 1) {
					specBuffer.append('(');
				}

				specBuffer.append(this.buildContainingConditions());
				if (memberSpecCount > 1) {
					specBuffer.append(')');
				}

				specBuffer.append("}");
			}
		}

		boolean ifExtendsFound = false;
		boolean ifImplementsFound = false;
		if (classSpecification != null && classSpecification.hasExtendsOrImplements()) {
			String extendsImplementsClause = classSpecification.getExtendsImplementsClause();
			specBuffer.append(extendsImplementsClause);
			if (extendsImplementsClause.indexOf("extends ") > -1) {
				ifExtendsFound = true;
			}

			if (extendsImplementsClause.indexOf("implements ") > -1) {
				ifImplementsFound = true;
			}
		}

		if (this.inheritanceClause != null) {
			if (ifExtendsFound && this.inheritanceClause.indexOf("extends ") > -1) {
				scriptTranslator.logWarning(
					"Does not support more complicated <n> syntax in '"
						+ this.getProguardCommandName()
						+ "' at line "
						+ this.getLineNumber()
						+ ". Two 'extends' clauses : '"
						+ this.inheritanceClause
						+ "' : '"
						+ classSpec
						+ "'. (I)"
				);
			} else if (ifImplementsFound && this.inheritanceClause.indexOf("implements ") > -1) {
				scriptTranslator.logWarning(
					"Does not support more complicated <n> syntax in '"
						+ this.getProguardCommandName()
						+ "' at line "
						+ this.getLineNumber()
						+ ". Two 'implements' clauses : '"
						+ this.inheritanceClause
						+ "' : '"
						+ classSpec
						+ "'. (J)"
				);
			} else {
				specBuffer.append(this.inheritanceClause);
			}
		}

		return specBuffer.toString();
	}

	public static class TranslatedClassSpec {
		private final String pattern;
		private final boolean negated;
		private final boolean inCommaSeparatedList;

		public TranslatedClassSpec(String pattern, boolean negated, boolean inCommaSeparatedList) {
			this.pattern = pattern;
			this.negated = negated;
			this.inCommaSeparatedList = inCommaSeparatedList;
		}

		public boolean isNegated() {
			return this.negated;
		}

		public String getPattern() {
			return this.pattern;
		}
	}
}
