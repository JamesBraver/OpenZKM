package com.zelix.framework.modules;

import com.zelix.event.EventRegistry;
import com.zelix.framework.AttributesNode;
import com.zelix.framework.attributes.UnknownAttribute;
import com.zelix.framework.attributes.module.ModuleAttribute;
import com.zelix.framework.tree.ClasspathResource;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.utils.collections.ThreeLevelMultiMap;
import com.zelix.utils.dataset.ByteDataBuffer;
import com.zelix.utils.references.AtomicReference;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;

public class ModuleInfoNode extends ClassNode {
	private ModuleAttribute moduleAttribute;
	private String moduleName;
	private final String originalModuleName;

	public ModuleInfoNode(
		ByteDataBuffer dataBuffer,
		ClasspathResource classpathResource,
		AtomicReference<String> progressMessage,
		EventRegistry eventRegistry,
		PrintWriter logWriter,
		PrintWriter warningLogWriter,
		ThreeLevelMultiMap<String, String, UnknownAttribute, Object> unknownAttributes
	) throws IOException {
		super(dataBuffer, classpathResource, progressMessage, eventRegistry, logWriter, warningLogWriter, unknownAttributes);

		for (AttributesNode attribute : this.attributes) {
			if (attribute instanceof ModuleAttribute) {
				this.moduleAttribute = (ModuleAttribute)attribute;
				break;
			}
		}

		this.moduleName = this.moduleAttribute.getModuleName();
		this.originalModuleName = this.moduleName;
	}

	public String getModuleName() {
		return this.moduleName;
	}

	public void remapPackageNames(HashMap<String, String> packageMappings) {
		this.constantPoolManager.remapPackageNames(packageMappings);
	}
}
