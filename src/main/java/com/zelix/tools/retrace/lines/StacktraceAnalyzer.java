package com.zelix.tools.retrace.lines;

import com.zelix.exceptions.ChangeLogMethodException;
import com.zelix.exceptions.ChangeLogsParserException;
import com.zelix.exceptions.ClassResolveException;
import com.zelix.exceptions.ClassResolveNotFoundException;
import com.zelix.exceptions.LookupException;
import com.zelix.exceptions.StacktraceAnalysisException;
import com.zelix.framework.AbstractClassNode;
import com.zelix.framework.signature.MethodSignature;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.wrappers.MethodNode;
import com.zelix.framework.writer.base.AbstractMethodCallWriter;
import com.zelix.klassmaster.changelogs.parser.ChangeLogsParser;
import com.zelix.klassmaster.changelogs.parser.base.ChangeLogsNodeBase;
import com.zelix.klassmaster.changelogs.parser.context.ChangeLogContext;
import com.zelix.klassmaster.config.Options;
import com.zelix.klassmaster.constant.CopyrightConstants;
import com.zelix.klassmaster.script.TokenMgrError;
import com.zelix.repository.ClassResolver;
import com.zelix.repository.ClasspathManager;
import com.zelix.tools.retrace.ChangeLogInput;
import com.zelix.tools.retrace.Stacktrace;
import com.zelix.tools.retrace.StacktraceClassInfo;
import com.zelix.tools.retrace.StacktraceElement;
import com.zelix.tools.retrace.StacktraceEntry;
import com.zelix.tools.retrace.StacktraceMethodInfo;
import com.zelix.tools.retrace.StacktraceReader;
import com.zelix.utils.FileUtils;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.StringUtils;
import com.zelix.utils.collections.DualKeyMap;
import com.zelix.utils.collections.KeyValuePair;
import com.zelix.utils.collections.TripleKeyMap;
import com.zelix.utils.dataset.AtomicBool;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map.Entry;

import static com.zelix.framework.instructions.OpcodeConstants.*;

public class StacktraceAnalyzer {
	private static long owningThreadId = -1L;
	private final int maxComplexityA = 16000;
	private final int maxComplexityB = 2000;
	private List<String> changeLogFileNames;
	private ClasspathManager classpathManager;
	private boolean changeLogsLoaded;
	private String mergeErrors;
	private ClassResolver classResolver;
	private ChangeLogInput changeLog;

	public StacktraceAnalyzer(List<String> changeLogFileNames, ClasspathManager classpathManager) {
		if (changeLogFileNames != null && changeLogFileNames.size() != 0) {
			this.changeLogFileNames = changeLogFileNames;
			this.classpathManager = classpathManager;
			acquireThreadLock();
		} else {
			throw new IllegalArgumentException("Input change log file name must be provided");
		}
	}

	public static synchronized void releaseThreadLock() {
		long threadId = GlobalUtils.getThreadId();
		if (owningThreadId != -1L && threadId == owningThreadId) {
			owningThreadId = -1L;
		}
	}

	public static synchronized void acquireThreadLock() {
		long threadId = GlobalUtils.getThreadId();
		if (owningThreadId != -1L && threadId != owningThreadId) {
			throw new LookupException(
				"FATAL ERROR: : " + CopyrightConstants.NAME + " Stack Trace Translate Tool is not thread safe. If you need to run more than once instance concurrently then each instance must run in a separate JVM."
			);
		} else {
			owningThreadId = threadId;
		}
	}

	public String translate(String stackTrace, boolean analyzeBytecode) {
		return this.translateStackTrace(stackTrace, analyzeBytecode, 2);
	}

	public String translateStackTrace(String stackTrace, boolean analyzeBytecode, int paramTypesMode) {
		StringBuilder outputBuffer = new StringBuilder();
		AtomicBool anyClassInChangeLog = new AtomicBool();
		String[] translatedStacktraces = this.getPossibleTranslations(stackTrace, analyzeBytecode, paramTypesMode, anyClassInChangeLog);
		if (!anyClassInChangeLog.get()) {
			outputBuffer.append(
				"NOTE: None of the classes in the stack trace were found in the change log or no stack trace lines found. Is the correct change log selected?  Do the stack trace lines start with 'at'?"
			);
			outputBuffer.append(Options.lineSeparator);
			outputBuffer.append(Options.lineSeparator);
		}

		if (translatedStacktraces.length == 1) {
			if (!analyzeBytecode || this.classpathManager == null) {
				outputBuffer.append("NOTE: Obfuscated bytecode not analyzed so there could be multiple possible methods for each trace line.");
				outputBuffer.append(Options.lineSeparator);
				outputBuffer.append(Options.lineSeparator);
			}

			outputBuffer.append(translatedStacktraces[0]);
		} else {
			outputBuffer.append("NOTE: " + translatedStacktraces.length + " translated stack traces are possible.");
			outputBuffer.append(Options.lineSeparator);
			outputBuffer.append(Options.lineSeparator);

			for (int translationIndex = 0; translationIndex < translatedStacktraces.length; translationIndex++) {
				String traceHeader = "Possible stack trace " + (translationIndex + 1) + " of " + translatedStacktraces.length + "...";
				outputBuffer.append(traceHeader + Options.lineSeparator);
				outputBuffer.append(StringUtils.repeat(traceHeader.length(), 61) + Options.lineSeparator);
				outputBuffer.append(translatedStacktraces[translationIndex]);
				if (translationIndex < translatedStacktraces.length - 1) {
					outputBuffer.append(Options.lineSeparator);
				}
			}
		}

		if (this.mergeErrors != null && this.mergeErrors.length() > 0) {
			outputBuffer.append(Options.lineSeparator);
			outputBuffer.append(this.mergeErrors);
		}

		return outputBuffer.toString();
	}

	private TripleKeyMap<String, ClassNode, Integer, Integer> getInvokeOpcodeMap(
		MethodNode callerMethod, HashMap<MethodNode, TripleKeyMap<String, ClassNode, Integer, Integer>> invokeOpcodeCache, ClassResolver classResolver
	) {
		TripleKeyMap<String, ClassNode, Integer, Integer> invokeOpcodeMap = invokeOpcodeCache.get(callerMethod);
		if (invokeOpcodeMap == null) {
			invokeOpcodeMap = new TripleKeyMap<>(27, 5, 5);
			HashMap<AbstractMethodCallWriter, Integer> opcodesByCallWriter = GlobalUtils.newHashMap(27);
			callerMethod.recordInvokeOpcodes(opcodesByCallWriter);

			for (Entry<AbstractMethodCallWriter, Integer> callWriterEntry : opcodesByCallWriter.entrySet()) {
				AbstractMethodCallWriter callWriter = callWriterEntry.getKey();
				if (!AbstractClassNode.isJavaLangInvokeHandle(callWriter.getOwnerName())) {
					Integer opcode = callWriterEntry.getValue();
					String ownerName = callWriter.getOwnerName();
					ClassNode ownerClass = classResolver.resolveClass(ownerName);
					invokeOpcodeMap.put(callWriter.getSignatureKey(), ownerClass, opcode, opcode);
				}
			}

			invokeOpcodeCache.put(callerMethod, invokeOpcodeMap);
		}

		return invokeOpcodeMap;
	}

	private StacktraceClassInfo createClassInfo(StacktraceElement stacktraceElement, ClassResolver classResolver, ChangeLogInput changeLog) {
		boolean classNotInChangeLog = false;
		if (stacktraceElement.isValid()) {
			classNotInChangeLog = !changeLog.hasClassMapping(stacktraceElement.getClassName());
		}

		return new StacktraceClassInfo(stacktraceElement, classResolver, classNotInChangeLog);
	}

	public String translateMethodName(String className, String methodName, String[] paramTypeDescriptors, String returnTypeDescriptor) {
		if (!this.changeLogsLoaded) {
			this.loadChangeLogs();
		}

		return this.changeLog.getOldMethodName(className, methodName, paramTypeDescriptors, returnTypeDescriptor);
	}

	public void close() {
		if (this.classResolver != null) {
			this.classResolver.close();
		}

		releaseThreadLock();
	}

	private void loadChangeLogs() {
		StringWriter errorBuffer = new StringWriter();
		PrintWriter errorWriter = new PrintWriter(errorBuffer);

		for (String changeLogFileName : this.changeLogFileNames) {
			ChangeLogInput changeLogInput = this.loadChangeLog(changeLogFileName);
			if (changeLogInput == null) {
				throw new StacktraceAnalysisException("Change log '" + changeLogFileName + "' appears to contain no change log data.");
			}

			if (this.changeLog == null) {
				this.changeLog = changeLogInput;
			} else {
				try {
					this.changeLog.mergeChangeLog(changeLogInput, errorWriter);
				} catch (ChangeLogMethodException changeLogMethodError) {
					throw new StacktraceAnalysisException(changeLogMethodError.getMessage());
				}
			}
		}

		this.mergeErrors = errorBuffer.toString();
		if (this.classpathManager != null) {
			this.classResolver = new ClassResolver(this.classpathManager, FileUtils.IS_UNIX);
		}

		this.changeLogsLoaded = true;
	}

	private List<StacktraceAnalyzer.StacktraceEntryCandidates> buildPossibleStacktraces(List<StacktraceClassInfo> classInfos, ClassResolver classResolver) {
		HashMap<MethodNode, TripleKeyMap<String, ClassNode, Integer, Integer>> invokeOpcodeCache = GlobalUtils.newHashMap();
		ArrayList<Stacktrace> stacktraces = new ArrayList<>();
		boolean anyLineResolved = false;
		StacktraceClassInfo previousClassInfo = null;

		for (int classInfoIndex = 0; classInfoIndex < classInfos.size(); classInfoIndex++) {
			StacktraceClassInfo classInfo = classInfos.get(classInfoIndex);
			List<StacktraceMethodInfo> methodInfos = classInfo.getMethods();
			if (classInfoIndex == 0) {
				this.initStacktraces(stacktraces, classInfo);
			} else if (!classInfo.isValid()) {
				for (Stacktrace existingStacktrace : stacktraces) {
					existingStacktrace.addUnresolvedEntry(classInfo);
				}
			} else if (!previousClassInfo.isValid()) {
				this.expandStacktraces(classInfo, stacktraces, methodInfos, anyLineResolved);
			} else {
				ArrayList<Stacktrace> savedStacktraces = new ArrayList<>(stacktraces.size());
				ListIterator<Stacktrace> stacktraceIterator = stacktraces.listIterator();

				while (stacktraceIterator.hasNext()) {
					Stacktrace stacktrace = stacktraceIterator.next();
					stacktraceIterator.remove();
					savedStacktraces.add(stacktrace);
					MethodNode currentMethod = stacktrace.getCurrentMethod();

					for (int methodIndex = 0; methodIndex < methodInfos.size(); methodIndex++) {
						MethodNode candidateMethod = methodInfos.get(methodIndex).method;
						if (this.isInvocationPossible(currentMethod, candidateMethod, invokeOpcodeCache, classResolver)) {
							Stacktrace clonedStacktrace = (Stacktrace)stacktrace.clone();
							clonedStacktrace.addResolvedEntry(classInfo, candidateMethod);
							stacktraceIterator.add(clonedStacktrace);
						}
					}
				}

				if (stacktraces.size() == 0) {
					stacktraces = savedStacktraces;
					this.expandStacktraces(classInfo, savedStacktraces, methodInfos, anyLineResolved);
				}
			}

			previousClassInfo = classInfo;
			if (!anyLineResolved) {
				anyLineResolved = classInfo.isValid();
			}

			if (stacktraces.size() > 16000) {
				throw new StacktraceAnalysisException(
					"Bytecode analysis exceeded maximum complexity threshold (A). "
						+ stacktraces.size()
						+ " > "
						+ 16000
						+ ". Try translating just the final lines of the stack trace."
				);
			}
		}

		ArrayList<StacktraceAnalyzer.StacktraceEntryCandidates> entryCandidates = new ArrayList<>(stacktraces.size());

		for (int stacktraceIndex = 0; stacktraceIndex < stacktraces.size(); stacktraceIndex++) {
			entryCandidates.add(new StacktraceAnalyzer.StacktraceEntryCandidates(stacktraces.get(stacktraceIndex)));
		}

		if (stacktraces.size() > 2000) {
			throw new StacktraceAnalysisException(
				"Bytecode analysis exceeded maximum complexity threshold (B). "
					+ stacktraces.size()
					+ " > "
					+ 2000
					+ ". Try translating just the final lines of the stack trace."
			);
		} else {
			int sizeBeforeMerge;
			do {
				sizeBeforeMerge = entryCandidates.size();

				for (int candidateIndex = 0; candidateIndex < entryCandidates.size() - 1; candidateIndex++) {
					StacktraceAnalyzer.StacktraceEntryCandidates entryCandidate = entryCandidates.get(candidateIndex);
					ListIterator<StacktraceAnalyzer.StacktraceEntryCandidates> entryCandidateIterator = entryCandidates.listIterator(candidateIndex + 1);

					while (entryCandidateIterator.hasNext()) {
						StacktraceAnalyzer.StacktraceEntryCandidates otherEntryCandidate = entryCandidateIterator.next();
						if (entryCandidate.isMergeable(otherEntryCandidate)) {
							entryCandidate.mergeWith(otherEntryCandidate);
							entryCandidateIterator.remove();
						}
					}
				}
			} while (entryCandidates.size() < sizeBeforeMerge);

			return entryCandidates;
		}
	}

	private void initStacktraces(List<Stacktrace> stacktraces, StacktraceClassInfo classInfo) {
		List<StacktraceMethodInfo> methodInfos = classInfo.getMethods();
		if (classInfo.isValid() && methodInfos.size() > 0) {
			for (int i = 0; i < methodInfos.size(); i++) {
				StacktraceMethodInfo methodInfo = methodInfos.get(i);
				MethodNode methodNode = methodInfo.method;
				if (methodNode == null) {
					methodNode = classInfo.getClassNode().findMethodNode(new MethodSignature(methodInfo.nameAndDescriptor));
				}

				Stacktrace methodStacktrace = new Stacktrace();
				methodStacktrace.addResolvedEntry(classInfo, methodNode);
				stacktraces.add(methodStacktrace);
			}
		} else {
			Stacktrace classStacktrace = new Stacktrace();
			classStacktrace.addUnresolvedEntry(classInfo);
			stacktraces.add(classStacktrace);
		}
	}

	private boolean isInvocationPossible(MethodNode callerMethod, MethodNode calleeMethod, HashMap<MethodNode, TripleKeyMap<String, ClassNode, Integer, Integer>> invokeOpcodeCache, ClassResolver classResolver) {
		DualKeyMap<ClassNode, Integer, Integer> invokeOpcodesByOwner = this.getInvokeOpcodeMap(callerMethod, invokeOpcodeCache, classResolver).getDualKeyMap(calleeMethod.getNameAndDescriptor());
		if (invokeOpcodesByOwner == null) {
			return false;
		} else {
			Enumeration<ClassNode> ownerClasses = invokeOpcodesByOwner.getGroupKeys();

			while (ownerClasses.hasMoreElements()) {
				ClassNode targetOwnerClass = ownerClasses.nextElement();
				ClassNode calleeOwnerClass = calleeMethod.getMethodOwner();
				Iterator<Integer> opcodeIterator = invokeOpcodesByOwner.getGroup(targetOwnerClass).keySet().iterator();

				while (opcodeIterator.hasNext()) {
					switch (opcodeIterator.next()) {
						case INVOKEVIRTUAL:
							if (targetOwnerClass != calleeOwnerClass
								&& !classResolver.hasSuperclass(targetOwnerClass.getInternalName(), calleeOwnerClass.getInternalName())
								&& !classResolver.hasSuperclass(calleeOwnerClass.getInternalName(), targetOwnerClass.getInternalName())) {
								break;
							}

							return true;
						case INVOKESPECIAL:
							if (targetOwnerClass != calleeOwnerClass && !classResolver.hasSuperclass(targetOwnerClass.getInternalName(), calleeOwnerClass.getInternalName())) {
								break;
							}

							return true;
						case INVOKESTATIC:
							if (targetOwnerClass == calleeOwnerClass) {
								return true;
							}
							break;
						case INVOKEINTERFACE:
							if (classResolver.implementsInterface(calleeOwnerClass.getInternalName(), targetOwnerClass.getInternalName())) {
								return true;
							}
					}
				}
			}

			return false;
		}
	}

	private List<StacktraceAnalyzer.AbstractTranslation> analyzeStacktrace(String stackTrace, boolean analyzeBytecode) {
		if (stackTrace == null) {
			throw new IllegalArgumentException("Stack trace is null");
		} else {
			if (!this.changeLogsLoaded) {
				this.loadChangeLogs();
			}

			StacktraceReader stacktraceReader = new StacktraceReader(stackTrace);
			List<StacktraceAnalyzer.AbstractTranslation> translations;
			if (this.classResolver != null && analyzeBytecode) {
				try {
					ArrayList<StacktraceClassInfo> classInfos = new ArrayList<>(stacktraceReader.getLineCount());
					Enumeration<StacktraceElement> stacktraceElements = stacktraceReader.getLinesReversed();

					while (stacktraceElements.hasMoreElements()) {
						StacktraceElement stacktraceElement = stacktraceElements.nextElement();
						classInfos.add(this.createClassInfo(stacktraceElement, this.classResolver, this.changeLog));
					}

					List<StacktraceAnalyzer.StacktraceEntryCandidates> entryCandidates = this.buildPossibleStacktraces(classInfos, this.classResolver);
					translations = this.createEntryAnalyzers(entryCandidates);
				} catch (ClassResolveNotFoundException missingClassException) {
					throw new StacktraceAnalysisException("Class '" + missingClassException.getClassName() + "' not found in obfuscated bytecode classpath '" + this.classpathManager.getRawClasspath() + "'");
				} catch (ClassResolveException classResolveException) {
					throw new StacktraceAnalysisException(classResolveException.getMessage() + " : '" + this.classpathManager.getRawClasspath() + "'");
				}
			} else {
				translations = new ArrayList<>(0);
				StacktraceAnalyzer.ElementTranslation elementTranslation = new StacktraceAnalyzer.ElementTranslation(stacktraceReader, this.changeLog);
				translations.add(elementTranslation);
			}

			return translations;
		}
	}

	public String[] listPossibleMethods(String className, String methodName) {
		if (!this.changeLogsLoaded) {
			this.loadChangeLogs();
		}

		className = className.replace('/', '.');
		List<KeyValuePair<String, String>> candidates = this.changeLog.getOldMethodCandidates(className, methodName);
		String[] possibleMethods;
		if (candidates != null && candidates.size() != 0) {
			possibleMethods = new String[candidates.size()];

			for (int i = 0; i < candidates.size(); i++) {
				KeyValuePair<String, String> candidate = candidates.get(i);
				String parameterDescriptor = candidate.getValue();
				possibleMethods[i] = candidate.getKey() + "(" + (parameterDescriptor != null ? parameterDescriptor : "") + ")";
			}
		} else {
			possibleMethods = new String[]{methodName};
		}

		return possibleMethods;
	}

	private ChangeLogInput loadChangeLog(String changeLogFileName) {
		BufferedReader reader = null;
		ChangeLogsNodeBase changeLogsRoot = null;

		ChangeLogInput changeLogInput;
		try {
			changeLogInput = new ChangeLogInput(changeLogFileName);
			String encoding = ChangeLogContext.getLogFileEncoding(new File(changeLogFileName));
			if (encoding != null) {
				reader = new BufferedReader(new InputStreamReader(new FileInputStream(changeLogFileName), encoding));
			} else {
				reader = new BufferedReader(new InputStreamReader(new FileInputStream(changeLogFileName)));
			}

			if (ChangeLogsParser.instance == null) {
				new ChangeLogsParser(reader);
			} else {
				ChangeLogsParser.reInit(reader);
			}

			changeLogsRoot = ChangeLogsParser.parseInput();
			changeLogsRoot.interpret(null, changeLogInput);
			if (changeLogsRoot.getChildCount() == 0) {
				changeLogInput = null;
			} else {
				changeLogInput.remapMethodParameterLists();
			}
		} catch (UnsupportedEncodingException ignoredUnsupportedEncoding) {
			throw new StacktraceAnalysisException("File Error : UnsupportedEncodingException in \"" + changeLogFileName + "\"");
		} catch (FileNotFoundException ignoredFileNotFound) {
			throw new StacktraceAnalysisException("File Error : Couldn't open file \"" + changeLogFileName + "\"");
		} catch (TokenMgrError tokenMgrError) {
			throw new StacktraceAnalysisException("Lexical error while reading \"" + changeLogFileName + "\" : \"" + tokenMgrError.getMessage() + "\"");
		} catch (ChangeLogsParserException changeLogsParserException) {
			throw new StacktraceAnalysisException("Parse error while reading \"" + changeLogFileName + "\" : \"" + changeLogsParserException.getMessage() + "\"");
		} finally {
			if (changeLogsRoot != null) {
				changeLogsRoot.dispose();
			}

			if (reader != null) {
				try {
					reader.close();
				} catch (IOException ignoredReaderClose) {
				}
			}
		}

		return changeLogInput;
	}

	private List<StacktraceAnalyzer.AbstractTranslation> createEntryAnalyzers(List<StacktraceAnalyzer.StacktraceEntryCandidates> candidates) {
		ArrayList<StacktraceAnalyzer.AbstractTranslation> translations = new ArrayList<>(candidates.size());

		for (int i = 0; i < candidates.size(); i++) {
			StacktraceAnalyzer.StacktraceEntryCandidates entryCandidate = candidates.get(i);
			translations.add(new StacktraceAnalyzer.EntriesTranslation(entryCandidate, this.changeLog));
		}

		return translations;
	}

	private String[] getPossibleTranslations(String stackTrace, boolean analyzeBytecode, int paramTypesMode, AtomicBool anyClassInChangeLog) {
		anyClassInChangeLog.set(false);
		List<StacktraceAnalyzer.AbstractTranslation> translations = this.analyzeStacktrace(stackTrace, analyzeBytecode);
		String[] translatedStacktraces = new String[translations.size()];

		for (int i = 0; i < translatedStacktraces.length; i++) {
			StacktraceAnalyzer.AbstractTranslation translation = translations.get(i);
			if (translation.hasTranslatedLine()) {
				anyClassInChangeLog.set(true);
			}

			translatedStacktraces[i] = translation.getTranslatedTrace(paramTypesMode, this.changeLog, analyzeBytecode ? this.classResolver : null);
		}

		Arrays.sort(translatedStacktraces);
		return translatedStacktraces;
	}

	public String translateClassName(String className) {
		if (!this.changeLogsLoaded) {
			this.loadChangeLogs();
		}

		boolean slashSeparated = className.indexOf("/") > -1;
		String dotSeparatedName;
		if (slashSeparated) {
			dotSeparatedName = className.replace('/', '.');
		} else {
			dotSeparatedName = className;
		}

		String oldClassName = this.changeLog.getOldClassName(dotSeparatedName);
		if (oldClassName == null) {
			return className;
		} else {
			if (slashSeparated) {
				oldClassName = oldClassName.replace('.', '/');
			}

			return oldClassName;
		}
	}

	private void expandStacktraces(StacktraceClassInfo classInfo, List<Stacktrace> stacktraces, List<StacktraceMethodInfo> methodInfos, boolean anyLineResolved) {
		ListIterator<Stacktrace> stacktraceIterator = stacktraces.listIterator();

		while (stacktraceIterator.hasNext()) {
			Stacktrace stacktrace = stacktraceIterator.next();
			stacktraceIterator.remove();

			for (int i = 0; i < methodInfos.size(); i++) {
				MethodNode methodNode = methodInfos.get(i).method;
				Stacktrace clonedStacktrace = (Stacktrace)stacktrace.clone();
				clonedStacktrace.addResolvedEntry(classInfo, methodNode, anyLineResolved);
				stacktraceIterator.add(clonedStacktrace);
			}
		}
	}

	public static class StacktraceEntryCandidates {
		private ArrayList<HashSet<StacktraceEntry>> entrySets = new ArrayList<>();

		public StacktraceEntryCandidates(Stacktrace stacktrace) {
			Enumeration<StacktraceEntry> entryEnumeration = stacktrace.getEntries();

			while (entryEnumeration.hasMoreElements()) {
				HashSet<StacktraceEntry> entrySet = GlobalUtils.newHashSet();
				entrySet.add(entryEnumeration.nextElement());
				this.entrySets.add(entrySet);
			}
		}

		public Enumeration<HashSet<StacktraceEntry>> getEntrySets() {
			return Collections.enumeration(this.entrySets);
		}

		public void mergeWith(StacktraceAnalyzer.StacktraceEntryCandidates otherCandidates) {
			for (int entrySetIndex = 0; entrySetIndex < this.entrySets.size(); entrySetIndex++) {
				this.entrySets.get(entrySetIndex).addAll(otherCandidates.entrySets.get(entrySetIndex));
			}
		}

		public boolean isMergeable(StacktraceAnalyzer.StacktraceEntryCandidates otherCandidates) {
			ArrayList<HashSet<StacktraceEntry>> otherEntrySets = otherCandidates.entrySets;

			for (int entrySetIndex = 0; entrySetIndex < this.entrySets.size(); entrySetIndex++) {
				HashSet<StacktraceEntry> entrySet = this.entrySets.get(entrySetIndex);
				HashSet<StacktraceEntry> otherEntrySet = otherEntrySets.get(entrySetIndex);
				HashSet<StacktraceEntry> previousEntrySet = null;
				HashSet<StacktraceEntry> otherPreviousEntrySet = null;
				HashSet<StacktraceEntry> nextEntrySet = null;
				HashSet<StacktraceEntry> otherNextEntrySet = null;
				if (entrySetIndex > 0) {
					previousEntrySet = this.entrySets.get(entrySetIndex - 1);
					otherPreviousEntrySet = otherEntrySets.get(entrySetIndex - 1);
				}

				if (entrySetIndex < this.entrySets.size() - 1) {
					nextEntrySet = this.entrySets.get(entrySetIndex + 1);
					otherNextEntrySet = otherEntrySets.get(entrySetIndex + 1);
				}

				if (!entrySet.equals(otherEntrySet) && (previousEntrySet != null && !previousEntrySet.equals(otherPreviousEntrySet) || nextEntrySet != null && !nextEntrySet.equals(otherNextEntrySet))) {
					return false;
				}
			}

			return true;
		}

		public int getLineCount() {
			return this.entrySets.size();
		}
	}

	public abstract static class AbstractTranslation {
		public AbstractLineAnalyzer[] lineAnalyzers;

		public String getTranslatedTrace(int parameterDetailLevel, ChangeLogInput changeLogInput, ClassResolver classResolver) {
			StringBuffer traceBuffer = new StringBuffer(1024);
			boolean previousLineResolved = false;
			AbstractLineAnalyzer previousAnalyzer = null;

			for (int analyzerIndex = this.lineAnalyzers.length - 1; analyzerIndex >= 0; analyzerIndex--) {
				AbstractLineAnalyzer lineAnalyzer = this.lineAnalyzers[analyzerIndex];
				if (previousLineResolved) {
					if (lineAnalyzer.isElementParsed()) {
						String analyzerClassName = lineAnalyzer.getClassName();
						if (analyzerClassName.startsWith("java.") || analyzerClassName.startsWith("sun.")) {
							traceBuffer.append("<Break in method call trace.>" + Options.lineSeparator);
						} else if (previousAnalyzer.isClassInitializer()) {
							traceBuffer.append("<Break in method call trace due to class initializer call.>" + Options.lineSeparator);
						} else {
							traceBuffer.append("<Break in method call trace. Could be due to JIT compiler inlining of method.>" + Options.lineSeparator);
						}
					} else {
						traceBuffer.append("<Break in method call trace.>" + Options.lineSeparator);
					}
				}

				traceBuffer.append(lineAnalyzer.getTranslatedLine(parameterDetailLevel, changeLogInput, classResolver));
				if (lineAnalyzer.hasWarning()) {
					traceBuffer.append("<" + lineAnalyzer.getWarningMessage() + ">" + Options.lineSeparator);
				}

				previousLineResolved = lineAnalyzer.isPreviousResolved();
				previousAnalyzer = lineAnalyzer;
			}

			return traceBuffer.toString();
		}

		public boolean hasTranslatedLine() {
			for (int analyzerIndex = 0; analyzerIndex < this.lineAnalyzers.length; analyzerIndex++) {
				if (this.lineAnalyzers[analyzerIndex].isClassResolved()) {
					return true;
				}
			}

			return false;
		}
	}

	public static class ElementTranslation extends StacktraceAnalyzer.AbstractTranslation {
		private int lineIndex;

		public ElementTranslation(StacktraceReader stacktraceReader, ChangeLogInput changeLogInput) {
			super.lineAnalyzers = new AbstractLineAnalyzer[stacktraceReader.getLineCount()];
			Enumeration<StacktraceElement> elementEnumeration = stacktraceReader.getLinesReversed();

			while (elementEnumeration.hasMoreElements()) {
				StacktraceElement element = elementEnumeration.nextElement();
				ElementAnalyzer elementAnalyzer = new ElementAnalyzer(element, changeLogInput);
				super.lineAnalyzers[this.lineIndex++] = elementAnalyzer;
			}
		}
	}

	public static class EntriesTranslation extends StacktraceAnalyzer.AbstractTranslation {
		public EntriesTranslation(StacktraceAnalyzer.StacktraceEntryCandidates entryCandidates, ChangeLogInput changeLogInput) {
			super.lineAnalyzers = new AbstractLineAnalyzer[entryCandidates.getLineCount()];
			int lineAnalyzerIndex = 0;
			Enumeration<HashSet<StacktraceEntry>> entrySetEnumeration = entryCandidates.getEntrySets();

			while (entrySetEnumeration.hasMoreElements()) {
				HashSet<StacktraceEntry> entrySet = entrySetEnumeration.nextElement();
				EntriesAnalyzer entriesAnalyzer = new EntriesAnalyzer(entrySet, changeLogInput);
				super.lineAnalyzers[lineAnalyzerIndex++] = entriesAnalyzer;
			}
		}
	}
}
