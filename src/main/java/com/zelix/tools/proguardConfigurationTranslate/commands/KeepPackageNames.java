package com.zelix.tools.proguardConfigurationTranslate.commands;

import com.zelix.tools.proguardConfigurationTranslate.ProguardScriptTranslator;
import com.zelix.tools.proguardConfigurationTranslate.base.KeepCommand;
import com.zelix.tools.proguardConfigurationTranslate.parser.QuotedStringNode;
import com.zelix.utils.WildcardMaskPatternUtils;

public class KeepPackageNames extends KeepCommand {
	public KeepPackageNames(int commandId) {
		super(commandId);
	}

	@Override
	public String getProguardCommandName() {
		return "-keeppackagenames";
	}

	@Override
	protected void executeCommand(ProguardScriptTranslator scriptTranslator, int initialMessageCount, int initialWarningCount, int initialErrorCount) {
		int childCount = this.getChildrenCount();
		if (childCount > 0) {
			for (int i = 0; i < childCount; i++) {
				String packageName = ((QuotedStringNode)this.getChild(i)).getValue();
				String negationPrefix = "";
				String negationSuffix = "";
				int separatorIndex;
				if ((separatorIndex = packageName.indexOf(":")) == -1 && (separatorIndex = packageName.indexOf("/")) == -1 && (separatorIndex = packageName.indexOf("\\")) == -1) {
				}

				if (separatorIndex > -1) {
					scriptTranslator.logWarning("Package name in ProGuard '" + this.getProguardCommandName() + "' command must not contain '" + packageName.charAt(separatorIndex) + "' character : '" + packageName + "'");
				} else {
					if (packageName.charAt(0) == '!') {
						packageName = packageName.substring(1);
						negationPrefix = "!(";
						negationSuffix = ")";
					}

					String packageNamePattern = WildcardMaskPatternUtils.toPackageNamePattern(packageName);
					scriptTranslator.addExcludePattern(negationPrefix + packageNamePattern + negationSuffix);
				}
			}
		} else {
			scriptTranslator.addExcludePattern("*.");
		}
	}
}
