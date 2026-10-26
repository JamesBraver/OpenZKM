package com.zelix.tools.proguardConfigurationTranslate.commands;

import com.zelix.tools.proguardConfigurationTranslate.ProguardNode;
import com.zelix.tools.proguardConfigurationTranslate.ProguardScriptTranslator;
import com.zelix.tools.proguardConfigurationTranslate.base.BaseCommand;
import com.zelix.tools.proguardConfigurationTranslate.base.KeepCommand;
import com.zelix.tools.proguardConfigurationTranslate.javaccInterface.ClassPatternConsumer;
import com.zelix.tools.proguardConfigurationTranslate.parser.ClassPattern;
import com.zelix.utils.WildcardMaskPatternUtils;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DontWarn extends KeepCommand implements ClassPatternConsumer {
	private List<ClassPattern> classPatterns = new ArrayList<>();

	public DontWarn(int commandId) {
		super(commandId);
	}

	@Override
	public String getProguardCommandName() {
		return "-dontwarn";
	}

	@Override
	public void parseArguments(ProguardNode parent, ProguardScriptTranslator scriptTranslator) {
		BaseCommand previousCommand = this.getPreviousCommand();
		if (previousCommand != null && previousCommand instanceof If) {
			If ifCommand = (If)previousCommand;
			scriptTranslator.logError(
				"ProGuard '" + ifCommand.getProguardCommandName() + "' command at line " + ifCommand.getLineNumber() + " followed by '" + this.getProguardCommandName() + "' rather than a keep command."
			);
		}

		for (int childPosition = 0; childPosition < this.getChildrenCount(); childPosition++) {
			this.getChild(childPosition).parseArguments(this, scriptTranslator);
		}

		Iterator<ClassPattern> classPatternIterator = this.classPatterns.iterator();

		while (classPatternIterator.hasNext()) {
			ClassPattern classPattern = classPatternIterator.next();
			if (classPattern.isNegated()) {
				scriptTranslator.logWarning(
					"ProGuard negated class specification '!" + classPattern.getPattern() + "' is not supported in '" + this.getProguardCommandName() + "' at line " + this.getLineNumber() + "."
				);
				classPatternIterator.remove();
			} else {
				String classNamePattern = WildcardMaskPatternUtils.toClassNamePattern(classPattern.getPattern());
				classPattern.setPattern(classNamePattern);
			}
		}

		int priorMessageCount = scriptTranslator.getMessageCount();
		int priorWarningCount = scriptTranslator.getWarningCount();
		int priorErrorCount = scriptTranslator.getErrorCount();
		this.executeCommand(scriptTranslator, priorMessageCount, priorWarningCount, priorErrorCount);
	}

	@Override
	protected void executeCommand(ProguardScriptTranslator scriptTranslator, int initialMessageCount, int initialWarningCount, int initialErrorCount) {
		if (this.classPatterns.size() == 0) {
			scriptTranslator.addIgnoreMissingRefPattern("*.* +");
		} else {
			for (ClassPattern classPattern : this.classPatterns) {
				if (!classPattern.getPattern().equals("module-info")) {
					scriptTranslator.addIgnoreMissingRefPattern(classPattern.getPattern() + " +");
				}
			}
		}
	}

	@Override
	public void addClassPattern(ClassPattern classPattern) {
		this.classPatterns.add(classPattern);
	}
}
