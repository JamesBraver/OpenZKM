package com.zelix.tools.proguardConfigurationTranslate.commands;

import com.zelix.tools.proguardConfigurationTranslate.ProguardScriptTranslator;
import com.zelix.tools.proguardConfigurationTranslate.base.KeepCommand;
import com.zelix.tools.proguardConfigurationTranslate.parser.ClassPathEntryNode;
import java.util.Set;

public class LibraryJars extends KeepCommand {
	public LibraryJars(int commandId) {
		super(commandId);
	}

	@Override
	public String getProguardCommandName() {
		return "-libraryjars";
	}

	@Override
	protected void executeCommand(ProguardScriptTranslator scriptTranslator, int initialMessageCount, int initialWarningCount, int initialErrorCount) {
		int childCount = this.getChildrenCount();

		for (int i = 0; i < childCount; i++) {
			ClassPathEntryNode classPathEntry = (ClassPathEntryNode)this.getChild(i);
			Set<String> jarPaths = classPathEntry.getJarPaths();
			scriptTranslator.addClasspathEntries(jarPaths);
			if (classPathEntry.hasFileFilters()) {
				scriptTranslator.logWarning("Filters not supported in ProGuard '" + this.getProguardCommandName() + "' command : '" + classPathEntry.buildJarPathsAndFilters() + "'");
			}
		}
	}
}
