package com.zelix;

import com.zelix.exceptions.ChangeLogsParserException;
import com.zelix.klassmaster.changelogs.parser.ChangeLogsParser;
import com.zelix.klassmaster.changelogs.parser.base.ChangeLogsNodeBase;
import com.zelix.klassmaster.changelogs.parser.context.ChangeLogContext;
import com.zelix.klassmaster.changelogs.parser.context.SignatureEntry;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.TokenMgrError;
import com.zelix.utils.GlobalUtils;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class ZKMChangeLog {
	private ScriptContext scriptContext = new ScriptContext(
		null, null, false, "ZKM_log1.txt", (String)null, (String)null, (String)null, (String)null, (String)null, (String)null, false, false
	);
	private String logFileName;
	public int initialMessageCount;
	private ChangeLogContext context;
	public int initialSpecialErrorCount;
	public int initialWarningCount;
	public int initialErrorCount;

	public ZKMChangeLog(String logFileName) throws Exception {
		this.scriptContext.clearMessageCache();
		this.logFileName = logFileName;
		this.captureInitialLogCounts();
		this.parseLogFile();
		this.checkAndPrintLogMessages();
	}

	private void captureInitialLogCounts() {
		this.initialMessageCount = this.scriptContext.getMessageCount();
		this.initialWarningCount = this.scriptContext.getWarningCount();
		this.initialErrorCount = this.scriptContext.getErrorCount();
		this.initialSpecialErrorCount = this.scriptContext.getSpecialErrorCount();
	}

	private void parseLogFile() throws Exception {
		BufferedReader logReader = null;
		ChangeLogsNodeBase rootNode = null;

		try {
			this.context = new ChangeLogContext(this.logFileName, this.scriptContext);
			ChangeLogsParser parserInstance = ChangeLogsParser.instance;
			String fileEncoding = ChangeLogContext.getLogFileEncoding(new File(this.logFileName));
			if (fileEncoding != null) {
				logReader = new BufferedReader(new InputStreamReader(new FileInputStream(this.logFileName), fileEncoding));
			} else {
				logReader = new BufferedReader(new FileReader(this.logFileName));
			}

			if (parserInstance == null) {
				new ChangeLogsParser(logReader);
			} else {
				ChangeLogsParser.reInit(logReader);
			}

			rootNode = ChangeLogsParser.parseInput();
			rootNode.interpret(null, this.context);
			if (rootNode.getChildCount() == 0) {
				System.exit(1);
			} else {
				this.context.checkPackageMappings();
			}

			this.context.markParsed();
		} catch (FileNotFoundException ignored) {
			throw new Exception("File Error : Couldn't open file \"" + this.logFileName + "\"");
		} catch (TokenMgrError tokenMgrError) {
			throw new Exception("Lexical error while reading \"" + this.logFileName + "\" : \"" + tokenMgrError.getMessage() + "\"");
		} catch (ChangeLogsParserException parserException) {
			throw new Exception("Parse error while reading \"" + this.logFileName + "\" : \"" + parserException.getMessage() + "\"");
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

	private ArrayList<String> convertInternalToBinaryNames(ArrayList<String> internalNames) {
		for (int i = 0; i < internalNames.size(); i++) {
			String binaryName = GlobalUtils.internalToBinaryName(internalNames.get(i));
			internalNames.set(i, binaryName);
		}

		return internalNames;
	}

	private void checkAndPrintLogMessages() {
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

	public String getLogFileName() {
		return this.logFileName;
	}

	public String getNewClassName(String oldClassName) {
		if (oldClassName == null) {
			throw new IllegalArgumentException("Old class name must not be null.");
		} else {
			String newClassName = this.context.getNewClassName(oldClassName);
			return newClassName == null ? oldClassName : GlobalUtils.internalToBinaryName(newClassName);
		}
	}

	public List<String> getNewClassNames() {
		return this.convertInternalToBinaryNames(this.context.getNewClassNames());
	}

	public String getNewFieldName(String oldClassName, String oldFieldSignature) {
		if (oldClassName == null) {
			throw new IllegalArgumentException("Old class name must not be null.");
		} else {
			int spaceIndex = oldFieldSignature.indexOf(" ");
			if (spaceIndex > 0 && spaceIndex < oldFieldSignature.length() - 1) {
				String fieldType = oldFieldSignature.substring(0, spaceIndex);
				String fieldName = oldFieldSignature.substring(spaceIndex + 1);
				return this.context.hasOldClass(oldClassName) ? this.getNewFieldName(oldClassName, fieldName, fieldType) : fieldName;
			} else {
				throw new IllegalArgumentException("'" + oldFieldSignature + "' is not a valid field signature");
			}
		}
	}

	public String getNewFieldName(String oldClassName, String oldFieldName, String oldFieldDescriptor) {
		if (oldClassName == null) {
			throw new IllegalArgumentException("Old class name must not be null.");
		} else if (this.context.hasOldClass(oldClassName)) {
			if (this.context.hasFieldMapping(oldClassName, oldFieldName, oldFieldDescriptor)) {
				this.captureInitialLogCounts();
				String newFieldName = this.context.getNewFieldName(oldClassName, oldFieldName, oldFieldDescriptor);
				this.checkAndPrintLogMessages();
				return newFieldName == null ? oldFieldName : newFieldName;
			} else {
				return oldFieldName;
			}
		} else {
			return oldFieldName;
		}
	}

	public String getNewMethodName(String oldClassName, String oldMethodSignature) {
		if (oldClassName == null) {
			throw new IllegalArgumentException("Old class name must not be null.");
		} else {
			SignatureEntry signatureEntry = this.parseHumanReadableSignature(oldMethodSignature);
			return this.context.hasOldClass(oldClassName) ? this.getNewMethodName(oldClassName, signatureEntry.methodName, signatureEntry.parameterTypes, signatureEntry.returnType) : signatureEntry.methodName;
		}
	}

	public String getNewMethodName(String oldClassName, String oldMethodName, String[] parameterTypes, String returnType) {
		if (oldClassName == null) {
			throw new IllegalArgumentException("Old class name must not be null.");
		} else if (this.context.hasOldClass(oldClassName)) {
			if (this.context.hasMethodMapping(oldClassName, oldMethodName, parameterTypes, returnType)) {
				this.captureInitialLogCounts();
				String newMethodName = this.context.getNewMethodName(oldClassName, oldMethodName, parameterTypes, returnType);
				this.checkAndPrintLogMessages();
				return newMethodName == null ? oldMethodName : newMethodName;
			} else {
				return oldMethodName;
			}
		} else {
			return oldMethodName;
		}
	}

	public String getNewMethodSignature(String oldClassName, String oldMethodSignature) {
		if (oldClassName == null) {
			throw new IllegalArgumentException("Old class name must not be null.");
		} else {
			SignatureEntry signatureEntry = this.parseHumanReadableSignature(oldMethodSignature);
			return this.context.hasOldClass(oldClassName) ? this.getNewMethodSignature(oldClassName, signatureEntry.methodName, signatureEntry.parameterTypes, signatureEntry.returnType) : oldMethodSignature;
		}
	}

	public String getNewMethodSignature(String oldClassName, String oldMethodName, String[] parameterTypes, String returnType) {
		if (oldClassName == null) {
			throw new IllegalArgumentException("Old class name must not be null.");
		} else if (oldMethodName == null) {
			throw new IllegalArgumentException("Old method name must not be null.");
		} else if (returnType == null) {
			throw new IllegalArgumentException("Old method return type must not be null.");
		} else {
			SignatureEntry signatureEntry = new SignatureEntry(this, returnType, oldMethodName, parameterTypes);
			if (this.context.hasOldClass(oldClassName)) {
				if (this.context.hasMethodMapping(oldClassName, oldMethodName, parameterTypes, returnType)) {
					this.captureInitialLogCounts();
					String newMethodSignature = this.context.getNewMethodSignatureString(oldClassName, oldMethodName, parameterTypes, returnType);
					this.checkAndPrintLogMessages();
					return newMethodSignature == null ? signatureEntry.getSignature() : newMethodSignature;
				} else {
					return signatureEntry.getSignature();
				}
			} else {
				return signatureEntry.getSignature();
			}
		}
	}

	public String getNewPackageName(String oldPackageName) {
		if (oldPackageName == null) {
			throw new IllegalArgumentException("New class name must not be null.");
		} else {
			String newPackageName = this.context.getNewPackageName(oldPackageName);
			return newPackageName == null ? oldPackageName : GlobalUtils.internalToBinaryName(newPackageName);
		}
	}

	public List<String> getNewPackageNames() {
		return this.convertInternalToBinaryNames(this.context.getNewPackageNames());
	}

	public String getOldClassName(String newClassName) {
		if (newClassName == null) {
			throw new IllegalArgumentException("New class name must not be null.");
		} else {
			String oldClassName = this.context.getOldClassName(newClassName);
			return oldClassName == null ? newClassName : GlobalUtils.internalToBinaryName(oldClassName);
		}
	}

	public List<String> getOldClassNames() {
		return this.convertInternalToBinaryNames(this.context.getOldClassNames());
	}

	public String getOldFieldName(String newClassName, String newFieldName, String newFieldDescriptor) {
		if (newClassName == null) {
			throw new IllegalArgumentException("New class name must not be null.");
		} else {
			return this.context.getOldFieldName(newClassName, newFieldName, newFieldDescriptor);
		}
	}

	public List<String> getOldFieldSignaturesForClass(String oldClassName) {
		if (oldClassName == null) {
			throw new IllegalArgumentException("Old class name must not be null.");
		} else {
			return this.context.getClassOldFieldSignatures(oldClassName);
		}
	}

	public String getOldMethodName(String newClassName, String newMethodSignature) {
		if (newClassName == null) {
			throw new IllegalArgumentException("New class name must not be null.");
		} else {
			SignatureEntry signatureEntry = this.parseHumanReadableSignature(newMethodSignature);
			return this.context.hasNewClass(newClassName) ? this.getOldMethodName(newClassName, signatureEntry.methodName, signatureEntry.parameterTypes, signatureEntry.returnType) : signatureEntry.methodName;
		}
	}

	public String getOldMethodName(String newClassName, String newMethodName, String[] parameterTypes, String returnType) {
		if (newClassName == null) {
			throw new IllegalArgumentException("New class name must not be null.");
		} else if (this.context.hasNewClass(newClassName)) {
			if (this.context.hasOldMethodName(newClassName, newMethodName, parameterTypes, returnType)) {
				this.captureInitialLogCounts();
				String oldMethodName = this.context.getOldMethodName(newClassName, newMethodName, parameterTypes, returnType);
				this.checkAndPrintLogMessages();
				return oldMethodName == null ? newMethodName : oldMethodName;
			} else {
				return newMethodName;
			}
		} else {
			return newMethodName;
		}
	}

	public List<String> getOldMethodSignatures(String newClassName, String newMethodName) {
		if (newClassName == null) {
			throw new IllegalArgumentException("New class name must not be null.");
		} else {
			return this.context.getOldMethodSignatures(newClassName, newMethodName);
		}
	}

	public List<String> getOldMethodSignaturesForClass(String oldClassName) {
		if (oldClassName == null) {
			throw new IllegalArgumentException("Old class name must not be null.");
		} else {
			return this.context.getClassOldMethodSignatures(oldClassName);
		}
	}

	public List<String> getOldPackageNames() {
		return this.convertInternalToBinaryNames(this.context.getOldPackageNames());
	}

	public String getOldTypeName(String newTypeName) {
		if (newTypeName == null) {
			throw new IllegalArgumentException("New type name must not be null.");
		} else {
			String oldTypeName = this.context.getOldTypeName(newTypeName);
			return oldTypeName == null ? newTypeName : GlobalUtils.internalToBinaryName(oldTypeName);
		}
	}

	public boolean isOldClassPresent(String oldClassName) {
		if (oldClassName == null) {
			throw new IllegalArgumentException("Old class name must not be null.");
		} else {
			return this.context.hasOldClass(oldClassName);
		}
	}

	public boolean isOldFieldPresent(String oldClassName, String oldFieldName, String oldFieldDescriptor) {
		if (oldClassName == null) {
			throw new IllegalArgumentException("Old class name must not be null.");
		} else {
			return this.context.hasOldClass(oldClassName) ? this.context.hasFieldMapping(oldClassName, oldFieldName, oldFieldDescriptor) : false;
		}
	}

	public boolean isOldFieldPresent(String oldClassName, String oldFieldSignature) {
		if (oldClassName == null) {
			throw new IllegalArgumentException("Old class name must not be null.");
		} else if (this.context.hasOldClass(oldClassName)) {
			int spaceIndex = oldFieldSignature.indexOf(" ");
			if (spaceIndex > 0 && spaceIndex < oldFieldSignature.length() - 1) {
				String fieldType = oldFieldSignature.substring(0, spaceIndex);
				String fieldName = oldFieldSignature.substring(spaceIndex + 1);
				return this.isOldFieldPresent(oldClassName, fieldName, fieldType);
			} else {
				throw new IllegalArgumentException("'" + oldFieldSignature + "' is not a valid field signature");
			}
		} else {
			return false;
		}
	}

	public boolean isOldMethodPresent(String oldClassName, String oldMethodSignature) {
		if (oldClassName == null) {
			throw new IllegalArgumentException("Old class name must not be null.");
		} else if (this.context.hasOldClass(oldClassName)) {
			SignatureEntry signatureEntry = this.parseHumanReadableSignature(oldMethodSignature);
			return this.isOldMethodPresent(oldClassName, signatureEntry.methodName, signatureEntry.parameterTypes, signatureEntry.returnType);
		} else {
			return false;
		}
	}

	public boolean isOldMethodPresent(String oldClassName, String oldMethodName, String[] parameterTypes, String returnType) {
		if (oldClassName == null) {
			throw new IllegalArgumentException("Old class name must not be null.");
		} else {
			return this.context.hasOldClass(oldClassName) ? this.context.hasMethodMapping(oldClassName, oldMethodName, parameterTypes, returnType) : false;
		}
	}

	public boolean isOldPackagePresent(String oldPackageName) {
		if (oldPackageName == null) {
			throw new IllegalArgumentException("Old package name must not be null.");
		} else {
			return this.context.hasOldPackageName(oldPackageName);
		}
	}

	private SignatureEntry parseHumanReadableSignature(String signature) {
		signature = signature.trim();
		int spaceIndex = signature.indexOf(" ");
		if (spaceIndex <= -1) {
			throw new IllegalArgumentException("'" + signature + "' is not a valid method signature (1)");
		} else {
			String returnType = signature.substring(0, spaceIndex);
			String signatureBody = signature.substring(spaceIndex + 1).trim();
			int openParenIndex = signatureBody.indexOf("(");
			if (openParenIndex <= 0) {
				throw new IllegalArgumentException("'" + signature + "' is not a valid method signature (2)");
			} else {
				String methodName = signatureBody.substring(0, openParenIndex);
				int closeParenIndex = signatureBody.indexOf(")", openParenIndex);
				if (closeParenIndex <= -1) {
					throw new IllegalArgumentException("'" + signature + "' is not a valid method signature (3)");
				} else {
					signatureBody = signatureBody.substring(openParenIndex + 1, closeParenIndex);
					ArrayList<String> parameterTypes = new ArrayList<>();
					StringTokenizer parameterTokenizer = new StringTokenizer(signatureBody, ",");

					while (parameterTokenizer.hasMoreTokens()) {
						parameterTypes.add(parameterTokenizer.nextToken().trim());
					}

					String[] parameterTypesArray = new String[parameterTypes.size()];
					parameterTypesArray = parameterTypes.toArray(parameterTypesArray);
					return new SignatureEntry(this, returnType, methodName, parameterTypesArray);
				}
			}
		}
	}
}
