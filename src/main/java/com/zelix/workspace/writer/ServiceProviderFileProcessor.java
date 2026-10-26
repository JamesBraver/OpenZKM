package com.zelix.workspace.writer;

import com.zelix.framework.wrappers.ClassNode;
import com.zelix.utils.FileUtils;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.StringUtils;
import com.zelix.utils.ZipEntryWriter;
import com.zelix.utils.collections.OrderedMap;
import com.zelix.utils.collections.readOnly.ReadOnlyMap;
import com.zelix.workspace.ClassHierarchyNode;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class ServiceProviderFileProcessor {
	public static final int SERVICE_PREFIX_LENGTH = 18;
	private OrderedMap<String, String> serviceImplementations = new OrderedMap<>();
	private ArrayList<String> implementationClassNames = new ArrayList<>();
	private boolean isDeflated;
	private String serviceInterfaceName;

	public ServiceProviderFileProcessor(ZipFile zipFile, ZipEntry zipEntry) throws IOException {
		this.isDeflated = zipEntry.getMethod() == 8;
		this.serviceInterfaceName = zipEntry.getName().substring(SERVICE_PREFIX_LENGTH).trim();
		InputStream serviceFileInputStream = null;
		BufferedReader serviceFileReader = null;

		try {
			serviceFileInputStream = zipFile.getInputStream(zipEntry);
			serviceFileReader = FileUtils.createReaderWithBomDetection(serviceFileInputStream, "UTF-8", null);

			String line;
			while ((line = serviceFileReader.readLine()) != null) {
				String implementationClassName = line;
				int commentIndex = line.indexOf("#");
				if (commentIndex != -1) {
					if (commentIndex > 0) {
						implementationClassName = line.substring(0, commentIndex - 1);
					} else {
						implementationClassName = "";
					}
				}

				implementationClassName = implementationClassName.trim();
				if (implementationClassName.length() > 0) {
					this.implementationClassNames.add(implementationClassName);
					this.serviceImplementations.put(line, implementationClassName);
				} else {
					this.serviceImplementations.put(line, null);
				}
			}
		} finally {
			if (serviceFileReader != null) {
				try {
					serviceFileReader.close();
				} catch (IOException ignoredReaderClose) {
				}
			} else if (serviceFileInputStream != null) {
				try {
					serviceFileInputStream.close();
				} catch (IOException ignoredStreamClose) {
				}
			}
		}
	}

	public boolean isDeflated() {
		return this.isDeflated;
	}

	public void collectServiceClasses(String archivePath, Map<ClassNode, String> classNameToArchivePathMap) {
		String sourceLocation = "'META-INF/services/" + this.serviceInterfaceName + "' in '" + archivePath + "'";
		ClassNode serviceInterfaceNode = ClassHierarchyNode.getWorkspaceClassNode(GlobalUtils.binaryToInternalName(this.serviceInterfaceName));
		if (serviceInterfaceNode != null) {
			classNameToArchivePathMap.put(serviceInterfaceNode, sourceLocation);
		}

		for (int i = 0; i < this.implementationClassNames.size(); i++) {
			ClassNode implementationClassNode = ClassHierarchyNode.getWorkspaceClassNode(GlobalUtils.binaryToInternalName(this.implementationClassNames.get(i)));
			if (implementationClassNode != null) {
				classNameToArchivePathMap.put(implementationClassNode, sourceLocation);
			}
		}
	}

	public void merge(ServiceProviderFileProcessor otherProcessor) {
		this.isDeflated = this.isDeflated || otherProcessor.isDeflated;
		HashSet<String> seenImplementationClassNames = GlobalUtils.newHashSet();

		for (Entry<String, String> implementationEntry : this.serviceImplementations.entrySet()) {
			String implementationClassName = implementationEntry.getValue();
			if (implementationClassName != null) {
				seenImplementationClassNames.add(implementationClassName);
			}
		}

		for (Entry<String, String> otherImplementationEntry : otherProcessor.serviceImplementations.entrySet()) {
			String otherServiceLine = otherImplementationEntry.getKey();
			String otherImplementationClassName = otherImplementationEntry.getValue();
			if (otherImplementationClassName != null) {
				if (seenImplementationClassNames.add(otherImplementationClassName)) {
					this.serviceImplementations.put(otherServiceLine, otherImplementationClassName);
				}
			} else if (!this.serviceImplementations.containsKey(otherServiceLine)) {
				this.serviceImplementations.put(otherServiceLine, null);
			}
		}
	}

	public String getServiceInterfaceName() {
		return this.serviceInterfaceName;
	}

	public String getRemappedFilePath(ReadOnlyMap<String, String> classRemappingMap) {
		String remappedInterfaceName = GlobalUtils.getMappedOrDefault(this.serviceInterfaceName.replace('.', '/'), classRemappingMap).replace('/', '.');
		return "META-INF/services/" + remappedInterfaceName;
	}

	public void writeRemappedServiceFile(ZipOutputStream zipOutputStream, Long timestamp, ReadOnlyMap<String, String> classRemappingMap, boolean isDeflated) throws IOException {
		ZipEntryWriter zipEntryWriter = new ZipEntryWriter(zipOutputStream, this.getRemappedFilePath(classRemappingMap), isDeflated);
		PrintWriter serviceFileWriter = new PrintWriter(new OutputStreamWriter(zipEntryWriter.getOutputStream(), "UTF-8"));

		for (Entry<String, String> implementationEntry : this.serviceImplementations.entrySet()) {
			String serviceLine = implementationEntry.getKey();
			String implementationClassName = implementationEntry.getValue();
			if (implementationClassName != null) {
				String internalClassName = implementationClassName.replace('.', '/');
				String mappedClassName = GlobalUtils.getMappedOrDefault(internalClassName, classRemappingMap);
				if (!internalClassName.equals(mappedClassName)) {
					serviceLine = StringUtils.replace(serviceLine, implementationClassName, mappedClassName.replace('/', '.'));
				}
			}

			serviceFileWriter.println(serviceLine);
		}

		serviceFileWriter.flush();
		zipEntryWriter.writeEntry(timestamp);
	}
}
