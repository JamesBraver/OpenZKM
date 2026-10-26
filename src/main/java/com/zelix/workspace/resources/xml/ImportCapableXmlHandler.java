package com.zelix.workspace.resources.xml;

import com.zelix.framework.AbstractClassNode;
import com.zelix.framework.signature.FieldSignature;
import com.zelix.framework.signature.MethodSignature;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.logging.AbstractLogger;
import com.zelix.repository.ClassProvider;
import com.zelix.utils.FileUtils;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.PathRemapper;
import com.zelix.utils.collections.readOnly.ReadOnlyMap;
import com.zelix.utils.collections.readOnly.ReadOnlyRegistryView;
import com.zelix.utils.references.AtomicReference;
import com.zelix.workspace.AbstractWorkspace;
import com.zelix.workspace.resources.ResourceIndex;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

public abstract class ImportCapableXmlHandler extends BeanPropertyXmlHandler {
	public final Map<String, ClassNode> beanClasses = GlobalUtils.newHashMap();
	public final Map<String, SpringBeanNode> abstractBeanElements = GlobalUtils.newHashMap();
	public final Map<String, String> beanParentNames = GlobalUtils.newHashMap();
	public final ResourceIndex fileAdaptor;

	public ImportCapableXmlHandler(String fullResourcePath, ResourceIndex fileAdaptor, AbstractWorkspace classHierarchy, ClassProvider classProvider, AbstractLogger logger) {
		super(fullResourcePath, classHierarchy, classProvider, logger);
		this.fileAdaptor = fileAdaptor;
	}

	public ImportCapableXmlHandler(
		String fullResourcePath,
		ReadOnlyMap<String, String> classNameMapping,
		ReadOnlyRegistryView<AbstractClassNode, FieldSignature, FieldSignature> fieldMappingRegistry,
		ReadOnlyRegistryView<AbstractClassNode, MethodSignature, MethodSignature> methodMappingRegistry,
		ResourceIndex fileAdaptor,
		PathRemapper resourcePathTransformer,
		AbstractWorkspace classHierarchy,
		ClassProvider classProvider,
		AbstractLogger logger
	) {
		super(fullResourcePath, classNameMapping, fieldMappingRegistry, methodMappingRegistry, resourcePathTransformer, classHierarchy, classProvider, logger);
		this.fileAdaptor = fileAdaptor;
	}

	public abstract List<String> importResources(String location, String baseDirectory);

	public void processImport(String location) {
		List<String> importedResourcePaths;
		if (!location.startsWith("classpath:") && !location.startsWith("classpath*:") && !location.startsWith("file:")) {
			AtomicReference<String> directoryRef = new AtomicReference<>();
			AtomicReference<String> nameRef = new AtomicReference<>();
			FileUtils.splitPath(super.resourceName, directoryRef, nameRef);
			String baseDirectory = directoryRef.get();
			String relativeLocation = location;
			if (location.startsWith("/")) {
				relativeLocation = location.substring(1);
			}

			FileUtils.splitPath(relativeLocation, directoryRef, nameRef);
			String discardedSplitPathComponent = directoryRef.get();
			discardedSplitPathComponent = nameRef.get();
			importedResourcePaths = this.importResources(relativeLocation, baseDirectory);
		} else {
			int colonIndex = location.indexOf(":");
			String locationPath = location.substring(colonIndex + 1);
			if (locationPath.startsWith("/")) {
				locationPath = locationPath.substring(1);
			}

			importedResourcePaths = this.importResources(locationPath, null);
		}

		if (importedResourcePaths != null) {
			for (String importedResourcePath : importedResourcePaths) {
				this.fileAdaptor.addImport(importedResourcePath, super.fullResourcePath);
			}
		}
	}

	public List<String> splitBeanNames(String names) {
		StringTokenizer nameTokenizer = new StringTokenizer(names, " ,;");
		ArrayList<String> beanNames = new ArrayList<>(nameTokenizer.countTokens());

		while (nameTokenizer.hasMoreTokens()) {
			beanNames.add(nameTokenizer.nextToken());
		}

		return beanNames;
	}
}
