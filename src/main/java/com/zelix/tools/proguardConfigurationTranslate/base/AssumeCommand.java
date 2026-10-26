package com.zelix.tools.proguardConfigurationTranslate.base;

import com.zelix.tools.proguardConfigurationTranslate.ProguardScriptTranslator;

public abstract class AssumeCommand extends BasePatternCommand {
	public AssumeCommand(int commandId) {
		super(commandId);
	}

	@Override
	public boolean isClassPattern() {
		return false;
	}

	private void addRemoveMethodCallsPattern(ProguardScriptTranslator scriptTranslator, String classMemberPattern, boolean negated) {
		if (negated) {
			scriptTranslator.addMethodCallsExcludePattern(classMemberPattern);
		} else {
			scriptTranslator.addMethodCallsIncludePattern(classMemberPattern);
		}
	}

	@Override
	public boolean usesContainingClause() {
		return false;
	}

	@Override
	protected final void executeCommand(ProguardScriptTranslator scriptTranslator, int initialMessageCount, int initialWarningCount, int initialErrorCount) {
		super.translatedSpecs = this.translateClassSpecs(scriptTranslator);

		for (BasePatternCommand.TranslatedClassSpec translatedSpec : super.translatedSpecs) {
			String classPattern = translatedSpec.getPattern();
			if (super.memberSpecs.size() == 0) {
				scriptTranslator.logWarning("ProGuard '" + this.getProguardCommandName() + "' at line " + this.getLineNumber() + " requires a method specification.");
			} else {
				for (String memberSpec : super.memberSpecs) {
					String classMemberPattern = classPattern + ' ' + memberSpec + "";
					this.addRemoveMethodCallsPattern(scriptTranslator, classMemberPattern, translatedSpec.isNegated());
				}
			}
		}
	}
}
