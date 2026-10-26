package com.zelix.klassmaster.script.statements.base;

import com.zelix.framework.analyzer.AnalyzingNode;
import com.zelix.framework.tree.ArchiveNode;
import com.zelix.framework.tree.ClasspathResource;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.base.AbstractScriptNode;
import com.zelix.logging.AbstractLogger;
import com.zelix.logging.LogContextLogger;
import com.zelix.utils.FileUtils;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.function.EmptyConsumer;
import com.zelix.utils.function.ZKMConsumer;
import com.zelix.workspace.Workspace;
import com.zelix.workspace.writer.JadFileProcessor;
import java.io.File;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;

public abstract class OutputStatement extends OutputStatementBase {
	public OutputStatement(int nodeType) {
		super(nodeType);
	}

	public String getLastModifiedTime() {
		return super.parameterValues.get("lastModifiedTime");
	}

	public boolean isDeleteEmptyDirectories() {
		boolean deleteEmptyDirectories = false;
		String parameterValue = super.parameterValues.get("deleteEmptyDirectories");
		if (parameterValue != null && parameterValue.equals("true")) {
			deleteEmptyDirectories = true;
		}

		return deleteEmptyDirectories;
	}

	private void createDirectoryIfNeeded(File directory, ScriptContext scriptContext, PrintWriter writer) {
		if (!directory.exists()) {
			if (!directory.mkdirs()) {
				scriptContext.logFatalError(
					"Could not create directory \"" + directory.getAbsolutePath() + "\" in \"" + this.getCommandName() + "\" statement at line " + this.getLineNumber() + "."
				);
			} else if (scriptContext.isVerbose()) {
				writer.println("\tCreated directory " + directory.getAbsolutePath() + "\"");
			}
		}
	}

	public abstract void saveWorkspace(Workspace workspace, File outputTarget, AbstractLogger logger, ScriptContext scriptContext, ZKMConsumer consumer);

	@Override
	protected void executeCommand(ScriptContext scriptContext, int initialMessageCount, int initialWarningCount, int initialErrorCount) {
		Workspace workspace = scriptContext.getWorkspace();
		if (!workspace.hasWorkspaceClasses()) {
			scriptContext.logFatalError("Attempt to use \"" + this.getCommandName() + "\" statement at line " + this.getLineNumber() + " before opening classes");
		} else if (workspace.isEmpty()) {
			scriptContext.logFatalError("Attempt to use \"" + this.getCommandName() + "\" statement at line " + this.getLineNumber() + " with no classes opened");
		} else if (!workspace.isErrorEmpty()) {
			scriptContext.logFatalError(
				"Attempt to use \"" + this.getCommandName() + "\" statement at line " + this.getLineNumber() + " with unuseable classes : " + workspace.getError()
			);
		}

		HashSet<String> duplicateFileNames = GlobalUtils.newHashSet();
		ArchiveNode[] archiveNodes = workspace.getAllArchiveNodes();
		HashMap<String, ArchiveNode> archiveNodesByFileName = GlobalUtils.newHashMap();

		for (ArchiveNode archiveNode : archiveNodes) {
			String archiveFileName = archiveNode.getFileName();
			if (archiveNodesByFileName.put(archiveFileName, archiveNode) != null) {
				duplicateFileNames.add(archiveFileName);
			}
		}

		HashMap<JadFileProcessor, ArchiveNode> archiveNodeByJadFile = workspace.getJadFileToArchiveNodeMap();
		HashMap<String, JadFileProcessor> jadFilesByFileName = GlobalUtils.newHashMap();

		for (JadFileProcessor jadFile : archiveNodeByJadFile.keySet()) {
			String jadFileName = FileUtils.getFileName(jadFile.getFullPath());
			if (jadFilesByFileName.put(jadFileName, jadFile) != null) {
				duplicateFileNames.add(jadFileName);
			}
		}

		ClasspathResource[] xmlResources = workspace.getAllClasspathResources();
		HashMap<String, ClasspathResource> xmlResourcesByFileName = GlobalUtils.newHashMap();

		for (ClasspathResource xmlResource : xmlResources) {
			String xmlFileName = FileUtils.getFileName(xmlResource.getFullPath());
			Object previousXmlResource = xmlResourcesByFileName.put(xmlFileName, xmlResource);
			if (previousXmlResource != null) {
				duplicateFileNames.add(xmlFileName);
			}
		}

		ClasspathResource[] yamlResources = workspace.getYamlResources();
		HashMap<String, ClasspathResource> yamlResourcesByFileName = GlobalUtils.newHashMap();

		for (ClasspathResource yamlResource : yamlResources) {
			String yamlFileName = FileUtils.getFileName(yamlResource.getFullPath());
			Object previousYamlResource = yamlResourcesByFileName.put(yamlFileName, yamlResource);
			if (previousYamlResource != null) {
				duplicateFileNames.add(yamlFileName);
			}
		}

		ClasspathResource[] propertiesResources = workspace.getLogFiles();
		HashMap<String, ClasspathResource> propertiesResourcesByFileName = GlobalUtils.newHashMap();

		for (ClasspathResource propertiesResource : propertiesResources) {
			String propertiesFileName = FileUtils.getFileName(propertiesResource.getFullPath());
			if (propertiesResourcesByFileName.put(propertiesFileName, propertiesResource) != null) {
				duplicateFileNames.add(propertiesFileName);
			}
		}

		PrintWriter writer = scriptContext.getWriter();
		LogContextLogger logger = new LogContextLogger(scriptContext, AbstractScriptNode.getTimestampPrefix().length());
		EmptyConsumer emptyConsumer = EmptyConsumer.create();
		File baseOutputFile = null;
		HashMap<ArchiveNode, File> outputFilesByArchiveNode = GlobalUtils.newHashMap();
		HashMap<JadFileProcessor, File> outputFilesByJadFile = GlobalUtils.newHashMap();
		HashMap<ClasspathResource, File> outputFilesByXmlResource = GlobalUtils.newHashMap();
		HashMap<ClasspathResource, File> outputFilesByYamlResource = GlobalUtils.newHashMap();
		HashMap<ClasspathResource, File> outputFilesByPropertiesResource = GlobalUtils.newHashMap();
		if (super.stringArguments.size() != 1 || !FileUtils.isArchiveExtensionLenient(super.stringArguments.get(0)) || archiveNodes.length + xmlResources.length <= 1 && !scriptContext.hasLoadedClasses()) {
			for (String savePathArgument : super.stringArguments) {
				if (savePathArgument.trim().length() == 0) {
					scriptContext.logFatalError("Missing base save directory or archive name in '" + this.getCommandName() + "' statement at line " + this.getLineNumber() + ".");
				}

				File outputFile = this.resolveOutputFile(savePathArgument, scriptContext.getDefaultDirectory());
				String argumentFileName = FileUtils.getFileName(savePathArgument);
				if (archiveNodesByFileName.containsKey(argumentFileName)) {
					File previousArchiveOutputFile = outputFilesByArchiveNode.put(archiveNodesByFileName.get(argumentFileName), outputFile);
					if (previousArchiveOutputFile != null) {
						outputFilesByArchiveNode.put(archiveNodesByFileName.get(argumentFileName), previousArchiveOutputFile);
						scriptContext.logWarning(
							"'" + argumentFileName + "' appears more than once in '" + this.getCommandName() + "' statement at line " + this.getLineNumber() + " : '" + savePathArgument + "' ignored."
						);
					}

					if (duplicateFileNames.contains(argumentFileName)) {
						scriptContext.logFatalError(
							"Ambiguous save file reference in '"
								+ this.getCommandName()
								+ "' statement at line "
								+ this.getLineNumber()
								+ " : '"
								+ argumentFileName
								+ "' appears more than once in 'open' statement. Cannot resolve save reference. (A)"
						);
					}
				} else if (jadFilesByFileName.containsKey(argumentFileName)) {
					File previousJadOutputFile = outputFilesByJadFile.put(jadFilesByFileName.get(argumentFileName), outputFile);
					if (previousJadOutputFile != null) {
						outputFilesByJadFile.put(jadFilesByFileName.get(argumentFileName), previousJadOutputFile);
						scriptContext.logWarning(
							"'" + argumentFileName + "' appears more than once in '" + this.getCommandName() + "' statement at line " + this.getLineNumber() + " : '" + savePathArgument + "' ignored."
						);
					}

					if (duplicateFileNames.contains(argumentFileName)) {
						scriptContext.logFatalError(
							"Ambiguous save file reference in '"
								+ this.getCommandName()
								+ "' statement at line "
								+ this.getLineNumber()
								+ " : '"
								+ argumentFileName
								+ "' appears more than once in 'open' statement. Cannot resolve save reference. (B)"
						);
					}
				} else if (xmlResourcesByFileName.containsKey(argumentFileName)) {
					File previousXmlOutputFile = outputFilesByXmlResource.put(xmlResourcesByFileName.get(argumentFileName), outputFile);
					if (previousXmlOutputFile != null) {
						outputFilesByXmlResource.put(xmlResourcesByFileName.get(argumentFileName), previousXmlOutputFile);
						scriptContext.logWarning(
							"'" + argumentFileName + "' appears more than once in '" + this.getCommandName() + "' statement at line " + this.getLineNumber() + " : '" + savePathArgument + "' ignored."
						);
					}

					if (duplicateFileNames.contains(argumentFileName)) {
						scriptContext.logFatalError(
							"Ambiguous save file reference in '"
								+ this.getCommandName()
								+ "' statement at line "
								+ this.getLineNumber()
								+ " : '"
								+ argumentFileName
								+ "' appears more than once in 'open' statement. Cannot resolve save reference. (C)"
						);
					}
				} else if (yamlResourcesByFileName.containsKey(argumentFileName)) {
					File previousYamlOutputFile = outputFilesByYamlResource.put(yamlResourcesByFileName.get(argumentFileName), outputFile);
					if (previousYamlOutputFile != null) {
						outputFilesByYamlResource.put(yamlResourcesByFileName.get(argumentFileName), previousYamlOutputFile);
						scriptContext.logWarning(
							"'" + argumentFileName + "' appears more than once in '" + this.getCommandName() + "' statement at line " + this.getLineNumber() + " : '" + savePathArgument + "' ignored."
						);
					}

					if (duplicateFileNames.contains(argumentFileName)) {
						scriptContext.logFatalError(
							"Ambiguous save file reference in '"
								+ this.getCommandName()
								+ "' statement at line "
								+ this.getLineNumber()
								+ " : '"
								+ argumentFileName
								+ "' appears more than once in 'open' statement. Cannot resolve save reference. (D)"
						);
					}
				} else if (propertiesResourcesByFileName.containsKey(argumentFileName)) {
					File previousPropertiesOutputFile = outputFilesByPropertiesResource.put(propertiesResourcesByFileName.get(argumentFileName), outputFile);
					if (previousPropertiesOutputFile != null) {
						outputFilesByPropertiesResource.put(propertiesResourcesByFileName.get(argumentFileName), previousPropertiesOutputFile);
						scriptContext.logWarning(
							"'" + argumentFileName + "' appears more than once in '" + this.getCommandName() + "' statement at line " + this.getLineNumber() + " : '" + savePathArgument + "' ignored."
						);
					}

					if (duplicateFileNames.contains(argumentFileName)) {
						scriptContext.logFatalError(
							"Ambiguous save file reference in '"
								+ this.getCommandName()
								+ "' statement at line "
								+ this.getLineNumber()
								+ " : '"
								+ argumentFileName
								+ "' appears more than once in 'open' statement. Cannot resolve save reference. (E)"
						);
					}
				} else if (baseOutputFile == null) {
					baseOutputFile = outputFile;
				} else if (FileUtils.isArchiveExtensionLenient(outputFile.getName())) {
					scriptContext.logWarning(" '" + this.getCommandName() + "' statement at line " + this.getLineNumber() + " specifies a redundant path : '" + savePathArgument + "' ignored.");
				} else {
					scriptContext.logWarning(
						" '"
							+ this.getCommandName()
							+ "' statement at line "
							+ this.getLineNumber()
							+ " specifies more than one base save directory or archive : '"
							+ savePathArgument
							+ "' ignored."
					);
				}
			}
		} else {
			baseOutputFile = this.resolveOutputFile(super.stringArguments.get(0), scriptContext.getDefaultDirectory());
		}

		if (baseOutputFile == null) {
			if (archiveNodesByFileName.size() > outputFilesByArchiveNode.size()) {
				scriptContext.logFatalError(
					" '"
						+ this.getCommandName()
						+ "' statement at line "
						+ this.getLineNumber()
						+ " doesn't specify a base save directory or archive name yet not all archives given a specific save path."
				);
			}

			if (jadFilesByFileName.size() > outputFilesByJadFile.size()) {
				scriptContext.logFatalError(
					" '"
						+ this.getCommandName()
						+ "' statement at line "
						+ this.getLineNumber()
						+ " doesn't specify a base save directory or archive name yet not all JAD files given a specific save path."
				);
			}

			if (xmlResourcesByFileName.size() > outputFilesByXmlResource.size()) {
				scriptContext.logFatalError(
					" '"
						+ this.getCommandName()
						+ "' statement at line "
						+ this.getLineNumber()
						+ " doesn't specify a base save directory or archive name yet not all opened XML files given a specific save path."
				);
			}

			if (scriptContext.hasLoadedClasses()) {
				scriptContext.logFatalError(
					" '"
						+ this.getCommandName()
						+ "' statement at line "
						+ this.getLineNumber()
						+ " doesn't specify a base save directory or archive name yet classes have been opened from the file system."
				);
			}
		}

		StringBuilder pathBuffer = new StringBuilder("");
		Iterator<Entry<ArchiveNode, File>> archiveEntryIterator = outputFilesByArchiveNode.entrySet().iterator();

		while (archiveEntryIterator.hasNext()) {
			Entry<ArchiveNode, File> archiveEntry = archiveEntryIterator.next();
			if (archiveEntryIterator.hasNext()) {
				if (baseOutputFile == null && pathBuffer.length() == 0) {
					pathBuffer.append("\"");
				} else {
					pathBuffer.append(", \"");
				}
			} else {
				pathBuffer.append(" and \"");
			}

			File archiveOutputFile = archiveEntry.getValue();
			pathBuffer.append(archiveOutputFile.getAbsolutePath());
			pathBuffer.append("\"");
			this.createDirectoryIfNeeded(archiveOutputFile.getParentFile(), scriptContext, writer);
		}

		Iterator<Entry<JadFileProcessor, File>> jadEntryIterator = outputFilesByJadFile.entrySet().iterator();

		while (jadEntryIterator.hasNext()) {
			File jadOutputFile = jadEntryIterator.next().getValue();
			this.createDirectoryIfNeeded(jadOutputFile.getParentFile(), scriptContext, writer);
		}

		Iterator<Entry<ClasspathResource, File>> xmlEntryIterator = outputFilesByXmlResource.entrySet().iterator();

		while (xmlEntryIterator.hasNext()) {
			File xmlOutputFile = xmlEntryIterator.next().getValue();
			this.createDirectoryIfNeeded(xmlOutputFile.getParentFile(), scriptContext, writer);
		}

		writer.println(
			AbstractScriptNode.getTimestampPrefix()
				+ " Saving "
				+ workspace.getClassesCount()
				+ " classes in "
				+ (baseOutputFile == null ? "" : "\"" + baseOutputFile.getAbsolutePath() + "\"")
				+ pathBuffer.toString()
				+ (AnalyzingNode.getIntegrityStatus() == -1 ? ' ' : "")
		);
		System.out.println(AbstractScriptNode.getTimestampPrefix() + " Saving " + workspace.getClassesCount() + " classes...");
		if (baseOutputFile == null) {
			baseOutputFile = scriptContext.getDefaultDirectory();
		}

		if (!FileUtils.isArchiveExtensionLenient(baseOutputFile.getName())) {
			this.createDirectoryIfNeeded(baseOutputFile, scriptContext, writer);
		} else if (baseOutputFile.getParentFile() != null) {
			this.createDirectoryIfNeeded(baseOutputFile.getParentFile(), scriptContext, writer);
		}

		workspace.setCLDCVersionMap(outputFilesByArchiveNode);
		workspace.setCustomJadOutputPaths(outputFilesByJadFile);
		workspace.setCustomXmlOutputPaths(outputFilesByXmlResource);
		workspace.setCustomYamlOutputPaths(outputFilesByYamlResource);
		workspace.setCustomPropertiesOutputPaths(outputFilesByPropertiesResource);
		this.saveWorkspace(workspace, baseOutputFile, logger, scriptContext, emptyConsumer);
		this.reportCountChanges(scriptContext, initialMessageCount, initialWarningCount, initialErrorCount, "while executing");
	}

	public boolean isDeleteXMLComments() {
		boolean deleteXMLComments = false;
		String parameterValue = super.parameterValues.get("deleteXMLComments");
		if (parameterValue != null && parameterValue.equals("true")) {
			deleteXMLComments = true;
		}

		return deleteXMLComments;
	}

	private File resolveOutputFile(String path, File defaultDirectory) {
		String normalizedPath = FileUtils.normalizePathSeparators(path.trim());
		File outputFile;
		if (FileUtils.isRelativePath(normalizedPath)) {
			if (normalizedPath.equals(".")) {
				outputFile = defaultDirectory;
			} else {
				outputFile = new File(defaultDirectory, normalizedPath);
			}
		} else {
			outputFile = new File(normalizedPath);
		}

		return outputFile;
	}

	public int getArchiveCompressionMode() {
		int compressionMode = 3;
		String parameterValue = super.parameterValues.get("archiveCompression");
		if (parameterValue != null) {
			if (parameterValue.equals("all")) {
				compressionMode = 1;
			} else if (parameterValue.equals("none")) {
				compressionMode = 2;
			}
		}

		return compressionMode;
	}
}
