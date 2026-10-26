package com.zelix;

import com.zelix.exceptions.ChangeLogsParserException;
import com.zelix.exceptions.ClassResolveException;
import com.zelix.framework.attributes.code.CodeAttribute;
import com.zelix.framework.signature.MethodSignature;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.wrappers.MethodNode;
import com.zelix.klassmaster.AbstractBootstrap;
import com.zelix.klassmaster.changelogs.FieldKey;
import com.zelix.klassmaster.changelogs.MethodKey;
import com.zelix.klassmaster.changelogs.MethodMappingInfo;
import com.zelix.klassmaster.changelogs.parser.ChangeLogsParser;
import com.zelix.klassmaster.changelogs.parser.base.ChangeLogsNodeBase;
import com.zelix.klassmaster.changelogs.parser.context.ChangeLogContext;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.TokenMgrError;
import com.zelix.repository.ClassRepository;
import com.zelix.repository.ClassResolver;
import com.zelix.repository.ClasspathManager;
import com.zelix.utils.FileUtils;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.StringUtils;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.io.DirectoryScanner;
import com.zelix.utils.references.AtomicReference;
import com.zelix.workspace.Workspace;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

public class ZKMChangeLogConvert {
	private final File inputFile;
	private final File outputFile;
	private final ClassResolver classResolver;
	private int initialSpecialErrorCount;
	private int initialMessageCount;
	private ScriptContext scriptContext;
	private ChangeLogContext changeLogContext;
	private int initialWarningCount;
	private int initialErrorCount;

	public ZKMChangeLogConvert(String changeLogFileName, String outputFileName, String classpath) throws Exception {
		this.inputFile = new File(changeLogFileName);
		this.outputFile = new File(outputFileName);
		if (classpath != null && classpath.length() > 0) {
			AbstractBootstrap.isZKMJarInClasspath(classpath);
			ClasspathManager classpathManager = new ClasspathManager(classpath);
			classpathManager.setClassRepository(new ClassRepository(classpathManager, FileUtils.IS_UNIX));
			classpathManager.initializeBootClasspath(new AtomicReference<>(), new AtomicReference<>());
			this.classResolver = new ClassResolver(classpathManager, FileUtils.IS_UNIX);
		} else {
			this.classResolver = null;
		}

		if (!this.inputFile.exists() || this.inputFile.isDirectory()) {
			System.out.println("File '" + this.inputFile.getAbsolutePath() + "' doesn't exist or is a directory");
		} else if (this.outputFile.isDirectory()) {
			System.out.println("File '" + this.outputFile.getAbsolutePath() + "' is a directory");
		} else {
			AtomicReference<String> errorMessage = new AtomicReference<>();
			this.convertChangeLog(errorMessage);
			if (errorMessage.isNull()) {
				System.out.println("Converted contents of '" + this.inputFile.getAbsolutePath() + "' written to '" + this.outputFile.getAbsolutePath() + "'");
			} else {
				System.out.println("ERROR: " + errorMessage.get());
			}
		}
	}

	public static void main(String[] args) throws Exception {
		if (args.length != 2 && args.length != 3) {
			System.out.println("Usage: java com.zelix.ZKMChangeLogConvert <changeLogFileName> <outputFileName> [obfuscatedByteCodeClasspath]");
			System.exit(1);
		}

		new ZKMChangeLogConvert(args[0], args[1], args.length == 3 ? args[2] : null);
	}

	private void captureInitialMessageCounts() {
		this.initialMessageCount = this.scriptContext.getMessageCount();
		this.initialWarningCount = this.scriptContext.getWarningCount();
		this.initialErrorCount = this.scriptContext.getErrorCount();
		this.initialSpecialErrorCount = this.scriptContext.getSpecialErrorCount();
	}

	private void convertChangeLog(AtomicReference<String> errorRef) throws Exception {
		errorRef.set(null);
		this.scriptContext = new ScriptContext(
			(Workspace)null,
			(ClasspathManager)null,
			false,
			"ZKM_ConvertLog.txt",
			(String)null,
			(String)null,
			(String)null,
			(String)null,
			(String)null,
			(String)null,
			false,
			false
		);
		this.scriptContext.clearMessageCache();
		this.captureInitialMessageCounts();
		this.parseChangeLog();
		this.printMessagesAndExit();
		Map<String, String> oldToNewClassNameMap = this.changeLogContext.getOldToNewClassNameMap();
		PrintWriter outputWriter = null;

		try {
			File outputDirectory = this.outputFile.getAbsoluteFile().getParentFile();
			if (outputDirectory != null) {
				DirectoryScanner.ensureDirectory(outputDirectory.getAbsolutePath());
			}

			outputWriter = new PrintWriter(new BufferedWriter(new FileWriter(this.outputFile)));
			if (oldToNewClassNameMap != null) {
				StringBuilder lineBuffer = new StringBuilder();

				for (String oldClassName : new TreeSet<>(oldToNewClassNameMap.keySet())) {
					String newClassName = oldToNewClassNameMap.get(oldClassName);
					lineBuffer.setLength(0);
					lineBuffer.append(oldClassName);
					lineBuffer.append(" -> ");
					lineBuffer.append(newClassName);
					lineBuffer.append(':');
					outputWriter.println(lineBuffer.toString());
					MultiMap<FieldKey, String> fieldMappings = this.changeLogContext.getRawFieldMappings(oldClassName);
					if (fieldMappings != null) {
						for (FieldKey fieldKey : new TreeSet<>(fieldMappings.keySet())) {
							lineBuffer.setLength(0);
							lineBuffer.append("    ");
							if (fieldKey.getDescriptor() != null) {
								lineBuffer.append(fieldKey.getDescriptor());
								lineBuffer.append(' ');
							}

							lineBuffer.append(fieldKey.getName());
							lineBuffer.append(" -> ");
							List<String> newFieldNames = fieldMappings.get(fieldKey);
							lineBuffer.append(newFieldNames.get(0));
							outputWriter.println(lineBuffer.toString());
						}
					}

					ClassNode classNode = null;
					if (this.classResolver != null) {
						try {
							classNode = this.classResolver.resolveClass(GlobalUtils.binaryToInternalName(newClassName));
						} catch (ClassResolveException classResolveException) {
							this.scriptContext.logError(classResolveException.getMessage());
						}
					}

					MultiMap<MethodKey, MethodMappingInfo> methodMappings = this.changeLogContext.getRawMethodMappings(oldClassName);
					if (methodMappings != null) {
						for (MethodKey methodKey : new TreeSet<>(methodMappings.keySet())) {
							lineBuffer.setLength(0);
							lineBuffer.append("    ");
							if (methodKey.getDescriptor() != null) {
								MethodMappingInfo methodMappingInfo = methodMappings.get(methodKey).get(0);
								if (classNode != null) {
									String newParameterDescriptor = MethodSignature.fromHumanReadableParameters(methodMappingInfo.getEffectiveParameterList());
									String oldReturnDescriptor = MethodSignature.fromHumanReadableType(methodKey.getDescriptor(), oldToNewClassNameMap);
									MethodSignature methodSignature = new MethodSignature(methodMappingInfo.getNewName(), newParameterDescriptor, oldReturnDescriptor);
									MethodNode methodNode = classNode.findMethodNode(methodSignature);
									if (methodNode != null) {
										CodeAttribute codeAttribute = methodNode.getCodeAttribute();
										if (codeAttribute != null) {
											int[] lineNumbers = codeAttribute.getLineNumbers();
											if (lineNumbers.length > 0) {
												lineBuffer.append(lineNumbers[0]);
												lineBuffer.append(':');
												lineBuffer.append(lineNumbers[lineNumbers.length - 1]);
												lineBuffer.append(':');
											}
										}
									} else {
										this.scriptContext
											.logError("Method '" + methodSignature + "' not found in class '" + classNode.getBinaryName() + "'. Line numbers could not be included in mapping.");
									}
								}

								lineBuffer.append(methodKey.getDescriptor());
								lineBuffer.append(' ');
								lineBuffer.append(methodKey.getName());
								lineBuffer.append('(');
								String oldParameterDescriptor = StringUtils.replace(methodKey.getParameterDescriptor(), ", ", ",");
								lineBuffer.append(oldParameterDescriptor);
								lineBuffer.append(')');
								lineBuffer.append(" -> ");
								lineBuffer.append(methodMappingInfo.getNewName());
								outputWriter.println(lineBuffer.toString());
							}
						}
					}
				}
			} else {
				errorRef.set("'" + this.inputFile.getAbsolutePath() + "' contains no class mappings.");
			}
		} finally {
			if (outputWriter != null) {
				outputWriter.close();
			}
		}
	}

	private void printMessagesAndExit() {
		boolean newErrorsDetected = false;
		boolean newMessagesDetected = false;
		if (this.initialSpecialErrorCount < this.scriptContext.getSpecialErrorCount() || this.initialErrorCount < this.scriptContext.getErrorCount()) {
			newErrorsDetected = true;
			newMessagesDetected = true;
		}

		if (this.initialWarningCount < this.scriptContext.getWarningCount() || this.initialMessageCount < this.scriptContext.getMessageCount()) {
			newMessagesDetected = true;
		}

		if (newMessagesDetected) {
			System.err.println(this.scriptContext.getMessagesAsString());
		}

		if (newErrorsDetected) {
			System.exit(1);
		}
	}

	private void parseChangeLog() throws Exception {
		BufferedReader logReader = null;
		ChangeLogsNodeBase rootNode = null;

		try {
			this.changeLogContext = new ChangeLogContext(this.inputFile.getAbsolutePath(), this.scriptContext);
			ChangeLogsParser parserInstance = ChangeLogsParser.instance;
			String fileEncoding = ChangeLogContext.getLogFileEncoding(this.inputFile);
			if (fileEncoding != null) {
				logReader = new BufferedReader(new InputStreamReader(new FileInputStream(this.inputFile), fileEncoding));
			} else {
				logReader = new BufferedReader(new FileReader(this.inputFile));
			}

			if (parserInstance == null) {
				new ChangeLogsParser(logReader);
			} else {
				ChangeLogsParser.reInit(logReader);
			}

			rootNode = ChangeLogsParser.parseInput();
			rootNode.interpret(null, this.changeLogContext);
			if (rootNode.getChildCount() == 0) {
				System.out.println("ERROR: '" + this.inputFile.getAbsolutePath() + "' is empty.");
				System.exit(1);
			} else {
				this.changeLogContext.checkPackageMappings();
			}

			this.changeLogContext.markParsed();
		} catch (FileNotFoundException ignored) {
			throw new Exception("File Error : Couldn't open file \"" + this.inputFile.getAbsolutePath() + "\"");
		} catch (TokenMgrError tokenMgrError) {
			throw new Exception("Lexical error while reading \"" + this.inputFile.getAbsolutePath() + "\" : \"" + tokenMgrError.getMessage() + "\"");
		} catch (ChangeLogsParserException parserException) {
			throw new Exception("Parse error while reading \"" + this.inputFile.getAbsolutePath() + "\" : \"" + parserException.getMessage() + "\"");
		} finally {
			if (rootNode != null) {
				rootNode.dispose();
			}

			if (logReader != null) {
				try {
					logReader.close();
				} catch (IOException closeException) {
				}
			}
		}
	}
}
