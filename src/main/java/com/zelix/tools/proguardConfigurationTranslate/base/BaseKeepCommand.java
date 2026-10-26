package com.zelix.tools.proguardConfigurationTranslate.base;

import com.zelix.klassmaster.script.statements.specifier.RenameFilterParameter;
import com.zelix.tools.proguardConfigurationTranslate.ProguardNode;
import com.zelix.tools.proguardConfigurationTranslate.ProguardScriptTranslator;
import com.zelix.tools.proguardConfigurationTranslate.commands.If;
import com.zelix.tools.proguardConfigurationTranslate.javaccInterface.KeepModifierConsumer;
import com.zelix.utils.GlobalUtils;
import java.util.Set;

public abstract class BaseKeepCommand extends BasePatternCommand implements KeepModifierConsumer {
	private Set<String> keepModifiers = GlobalUtils.newHashSet();
	private boolean keepFromShrinking = true;
	private boolean keepFromObfuscation = true;
	private boolean includeDescriptorClasses = false;

	public BaseKeepCommand(int commandId) {
		super(commandId);
	}

	private static boolean isMethodSpec(String memberSpec) {
		return memberSpec.indexOf("(") > -1;
	}

	private void emitRule(ProguardScriptTranslator scriptTranslator, String rule, boolean negated) {
		if (this.keepFromShrinking) {
			if (negated) {
				scriptTranslator.addTrimUnexcludePattern(rule);
			} else {
				scriptTranslator.addTrimExcludePattern(rule);
			}
		}

		if (this.keepFromObfuscation) {
			if (negated) {
				scriptTranslator.addUnexcludePattern(rule);
			} else {
				scriptTranslator.addExcludePattern(rule);
			}
		}
	}

	@Override
	public final void addKeepModifier(String keepModifier) {
		this.keepModifiers.add(keepModifier);
	}

	@Override
	public void parseArguments(ProguardNode parent, ProguardScriptTranslator scriptTranslator) {
		super.parseArguments(parent, scriptTranslator);
	}

	@Override
	protected final void executeCommand(ProguardScriptTranslator scriptTranslator, int initialMessageCount, int initialWarningCount, int initialErrorCount) {
		BaseCommand previousCommand = this.getPreviousCommand();
		if (previousCommand != null && previousCommand instanceof If) {
			If ifCommand = (If)previousCommand;
			this.setIfCommand(ifCommand);
		}

		for (String keepModifier : this.keepModifiers) {
			if (keepModifier.equals("allowshrinking")) {
				this.keepFromShrinking = false;
			} else if (keepModifier.equals("allowobfuscation")) {
				this.keepFromObfuscation = false;
			} else if (keepModifier.equals("includedescriptorclasses")) {
				this.includeDescriptorClasses = true;
			}
		}

		super.translatedSpecs = this.translateClassSpecs(scriptTranslator);

		for (BasePatternCommand.TranslatedClassSpec translatedSpec : super.translatedSpecs) {
			String classPattern = translatedSpec.getPattern();
			if (super.memberSpecs.size() == 0) {
				this.emitRule(scriptTranslator, classPattern, translatedSpec.isNegated());
			} else {
				for (String memberSpec : super.memberSpecs) {
					String signatureClassesSuffix = "";
					if (this.includeDescriptorClasses) {
						if (isMethodSpec(memberSpec)) {
							signatureClassesSuffix = " +signatureClasses";
						} else {
							scriptTranslator.logWarning(
								"ProGuard keep modifier 'includedescriptorclasses' is not supported for fields in '" + this.getProguardCommandName() + "' at line " + this.getLineNumber() + "."
							);
						}
					}

					String rule = classPattern + ' ' + memberSpec + signatureClassesSuffix;
					this.emitRule(scriptTranslator, rule, translatedSpec.isNegated());
				}

				if (this.isClassPattern() && !this.usesContainingClause()) {
					if (classPattern.charAt(classPattern.length() - 1) == RenameFilterParameter.CARET_CHAR) {
						classPattern = classPattern.substring(0, classPattern.length() - 1);
					}

					this.emitRule(scriptTranslator, classPattern, translatedSpec.isNegated());
				}
			}
		}
	}
}
