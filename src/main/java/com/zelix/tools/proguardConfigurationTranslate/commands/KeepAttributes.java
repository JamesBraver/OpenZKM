package com.zelix.tools.proguardConfigurationTranslate.commands;

import com.zelix.tools.proguardConfigurationTranslate.ProguardScriptTranslator;
import com.zelix.tools.proguardConfigurationTranslate.base.KeepCommand;
import com.zelix.utils.StringUtils;
import com.zelix.utils.WildcardMaskPatternUtils;
import java.util.ArrayList;
import java.util.List;

public class KeepAttributes extends KeepCommand {
	public List<String> attributes = new ArrayList<>();

	public KeepAttributes(int commandId) {
		super(commandId);
	}

	public void addAttribute(String attribute) {
		this.attributes.add(attribute);
	}

	@Override
	public String getProguardCommandName() {
		return "-keepattributes";
	}

	@Override
	protected void executeCommand(ProguardScriptTranslator scriptTranslator, int initialMessageCount, int initialWarningCount, int initialErrorCount) {
		for (String attribute : this.attributes) {
			if (attribute.charAt(0) == '!') {
				scriptTranslator.logWarning("Negated attribute in ProGuard not supported : '" + attribute + "'");
			} else {
				attribute = StringUtils.replace(StringUtils.replace(WildcardMaskPatternUtils.normalizeWildcard(attribute), "?", "[a-zA-Z]"), "*", "[a-zA-Z]*");
				scriptTranslator.addAttributePattern(attribute);
			}
		}
	}
}
