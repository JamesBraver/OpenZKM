package com.zelix.tools.excludes;

import com.zelix.framework.modules.ModuleInfoNode;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.statements.base.ExclusionInclusionStatement;
import com.zelix.klassmaster.script.statements.exclusion.UnexcludeStatement;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.enumerations.SnapshotEnumerator;
import com.zelix.workspace.Workspace;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class ModuleExcludesManager extends InclusionExclusionManager {
	private Set<ModuleInfoNode> processableModules = GlobalUtils.newHashSet();
	private Set<ModuleInfoNode> excludedModules = GlobalUtils.newHashSet();
	private Map<String, ModuleInfoNode> modulesByModelName = GlobalUtils.newHashMap();

	public ModuleExcludesManager(Workspace workspace, List<? extends ExclusionInclusionStatement> excludeStatements, List<UnexcludeStatement> unexcludeStatements, ScriptContext scriptContext) {
		super(workspace, excludeStatements, unexcludeStatements, scriptContext);
	}

	public void initializeProcessableModules() {
		Enumeration<ModuleInfoNode> moduleEnumeration = this.workspace.getModuleInfoEnumeration();

		while (moduleEnumeration.hasMoreElements()) {
			ModuleInfoNode module = moduleEnumeration.nextElement();
			this.processableModules.add(module);
			this.modulesByModelName.put(module.getModuleName(), module);
		}
	}

	public final void excludeModule(ModuleInfoNode module, String reason) {
		if (this.processableModules.contains(module)) {
			this.processableModules.remove(module);
			this.excludedModules.add(module);
			String message = "Excluding module \"" + module.getModuleName() + "\" from name obfuscation because of \"" + reason + "\"";
			if (super.scriptContext.isVerbose() && super.logWriter != null) {
				super.logWriter.println("\t" + message);
			}
		}
	}

	public Enumeration<ModuleInfoNode> getProcessableModules() {
		return new SnapshotEnumerator<>(this.processableModules);
	}
}
