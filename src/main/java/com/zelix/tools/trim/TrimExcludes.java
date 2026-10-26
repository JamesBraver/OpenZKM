package com.zelix.tools.trim;

import com.zelix.exceptions.LexicalException;
import com.zelix.exceptions.ParsingException;
import com.zelix.framework.AbstractClassNode;
import com.zelix.framework.MemberNode;
import com.zelix.framework.StructureNode;
import com.zelix.framework.hierarchy.ClassHierarchy;
import com.zelix.framework.listener.ReferenceSetContainer;
import com.zelix.framework.signature.FieldSignature;
import com.zelix.framework.signature.MethodSignature;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.wrappers.FieldNode;
import com.zelix.framework.wrappers.MethodNode;
import com.zelix.klassmaster.config.Options;
import com.zelix.klassmaster.config.TrimConfig;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.ScriptParser;
import com.zelix.klassmaster.script.base.AbstractScriptNode;
import com.zelix.klassmaster.script.statements.base.ExclusionInclusionStatement;
import com.zelix.klassmaster.script.statements.exclusion.DefaultTrimExcludeInput;
import com.zelix.klassmaster.script.statements.specifier.RenameFilterParameter;
import com.zelix.repository.ClassRepository;
import com.zelix.tools.excludes.ExcludeStatementManager;
import com.zelix.tools.excludes.FixedClassManager;
import com.zelix.tools.excludes.InclusionExclusionManager;
import com.zelix.tools.excludes.SerializedClassManager;
import com.zelix.utils.DebugUtils;
import com.zelix.utils.FileUtils;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.collections.DualKeyMap;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.collections.RingQueue;
import com.zelix.utils.collections.readOnly.ReadOnlyMap;
import com.zelix.utils.enumerations.ArrayEnumeration;
import com.zelix.workspace.ClassHierarchyNode;
import com.zelix.workspace.ClassTreeManager;
import com.zelix.workspace.Workspace;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;

public class TrimExcludes extends InclusionExclusionManager {
	public static final String DEFAULT_CLASSLIB_EXCLUDES = "trimExclude  public *.* and"
		+ Options.lineSeparator
		+ "             *.* public * and"
		+ Options.lineSeparator
		+ "             *.* public *(*);"
		+ Options.lineSeparator;
	public static final String DEFAULT_FRAMEWORK_EXCLUDES = "trimExclude  public *.* and"
		+ Options.lineSeparator
		+ "             *.* public * and"
		+ Options.lineSeparator
		+ "             *.* protected * and"
		+ Options.lineSeparator
		+ "             *.* public *(*) and"
		+ Options.lineSeparator
		+ "             *.* protected *(*);"
		+ Options.lineSeparator;
	public static final String DEFAULT_MIDLET_EXCLUDES = "trimExclude *.* extends javax.microedition.midlet.MIDlet;" + Options.lineSeparator;
	public static final String DEFAULT_PACKAGEINFO_EXCLUDES = "trimExclude *.package-info;" + Options.lineSeparator;
	public static final String DEFAULT_FALLBACK_EXCLUDES = "trimExclude  *.* <init>() and //No-arg constructors.  Some APIs access them indirectly.  Eg. java.io.Externalizable "
		+ Options.lineSeparator
		+ "             *.* extends java.rmi.server.RemoteStub <init>(java.rmi.server.RemoteRef) and //RMI"
		+ Options.lineSeparator
		+ "             *.*_Skel implements java.rmi.server.Skeleton and //RMI "
		+ Options.lineSeparator
		+ "             *.*_Stub extends java.rmi.server.RemoteStub and //RMI "
		+ Options.lineSeparator
		+ "             *.*_Stub extends javax.rmi.CORBA.Stub and //RMI with iiop "
		+ Options.lineSeparator
		+ "             *.*_Tie implements javax.rmi.CORBA.Tie and //RMI with iiop "
		+ Options.lineSeparator
		+ "             interface *.*^ implements java.rmi.Remote *(*) throws java.rmi.RemoteException and //RMI "
		+ Options.lineSeparator
		+ "             *.* private readObject(java.io.ObjectInputStream) and   //Serialization "
		+ Options.lineSeparator
		+ "             *.* private writeObject(java.io.ObjectOutputStream) and //Serialization "
		+ Options.lineSeparator
		+ "             *.* static final long serialVersionUID and              //Serialization "
		+ Options.lineSeparator
		+ "             *.* writeReplace() and                                  //Serialization "
		+ Options.lineSeparator
		+ "             *.* readResolve() and                                   //Serialization "
		+ Options.lineSeparator
		+ "             *.* readObjectNoData() throws java.io.ObjectStreamException and //Serialization "
		+ Options.lineSeparator
		+ "             *.* java.io.ObjectStreamField[] serialPersistentFields and  //Serialization "
		+ Options.lineSeparator
		+ "             *.* implements javax.servlet.Servlet and  //Servlets "
		+ Options.lineSeparator
		+ "             *.* extends java.lang.Enum public * and     //enum "
		+ Options.lineSeparator
		+ "             *.* extends java.lang.Enum public values() and  //enum "
		+ Options.lineSeparator
		+ "             *.* extends java.lang.Enum public valueOf(java.lang.String) and  //enum "
		+ Options.lineSeparator
		+ "             *.*^ implements javax.ejb.EJBObject public *(*) and               //EJB Remote "
		+ Options.lineSeparator
		+ "             *.*^ implements javax.ejb.EJBLocalObject public *(*) and          //EJB Local "
		+ Options.lineSeparator
		+ "             *.*^ implements javax.ejb.EJBHome public create(*) and            //EJB Home "
		+ Options.lineSeparator
		+ "             *.*^ implements javax.ejb.EJBHome public find*(*) and             //EJB Home "
		+ Options.lineSeparator
		+ "             *.*^ implements javax.ejb.EJBLocalHome public create(*) and       //EJB Local Home "
		+ Options.lineSeparator
		+ "             *.*^ implements javax.ejb.EJBLocalHome public find*(*) and        //EJB Local Home "
		+ Options.lineSeparator
		+ "             *.*^ implements javax.ejb.EnterpriseBean public *(*) and          //EJB Bean "
		+ Options.lineSeparator
		+ "             @javax.ejb.Remote *.*^ public *(*) and                              "
		+ Options.lineSeparator
		+ "             @javax.ejb.RemoteHome *.*^ public *(*) and                          "
		+ Options.lineSeparator
		+ "             @javax.ejb.Stateless *.*^ public *(*) and                           "
		+ Options.lineSeparator
		+ "             @javax.ejb.Stateful *.*^ public *(*) and                            "
		+ Options.lineSeparator
		+ "             @javax.ejb.MessageDriven *.*^ public *(*) and                       "
		+ Options.lineSeparator
		+ "             @javax.persistence.Entity *.*^ public *(*) and                      "
		+ Options.lineSeparator
		+ "             @javax.persistence.Table *.*^ public *(*) and                       "
		+ Options.lineSeparator
		+ "             @javax.persistence.Entity *.* @javax.persistence.Column * and       "
		+ Options.lineSeparator
		+ "             *.* @javax.persistence.Id * and                                     "
		+ Options.lineSeparator
		+ "             *.* @javax.persistence.Id *(*) and                                  "
		+ Options.lineSeparator
		+ "             @javax.persistence.IdClass(value=*.*^) public *(*) and              "
		+ Options.lineSeparator
		+ "             @javax.jws.WebService *.*^ @javax.jws.WebMethod public *(*) and     "
		+ Options.lineSeparator
		+ "             *.* static createUI(javax.swing.JComponent) and                  //GUI "
		+ Options.lineSeparator
		+ "             *.* extends javax.swing.JComponent getUI()  and                  //Pluggable L&F"
		+ Options.lineSeparator
		+ "             @javax.xml.bind.annotation.XmlType *.* * and             //JAXB"
		+ Options.lineSeparator
		+ "             @javax.xml.bind.annotation.XmlType *.* public get*() and "
		+ Options.lineSeparator
		+ "             @javax.xml.bind.annotation.XmlType *.* public is*() and "
		+ Options.lineSeparator
		+ "             @javax.xml.bind.annotation.XmlType *.* public set*(*) and"
		+ Options.lineSeparator
		+ "             @javax.xml.bind.annotation.XmlRootElement *.* private * and       //JAXB"
		+ Options.lineSeparator
		+ "             @javax.xml.bind.annotation.XmlRootElement *.* public get*() and "
		+ Options.lineSeparator
		+ "             @javax.xml.bind.annotation.XmlRootElement *.* public is*() and "
		+ Options.lineSeparator
		+ "             @javax.xml.bind.annotation.XmlRootElement *.* public set*(*) and"
		+ Options.lineSeparator
		+ "             *.* @javax.annotation.PostConstructor *(*) and"
		+ Options.lineSeparator
		+ "             *.* @javax.annotation.PreDestroy *(*) and"
		+ Options.lineSeparator
		+ "             *.*MBean public *(*) and   //JMX"
		+ Options.lineSeparator
		+ "             *.*MXBean public *(*) and   //JMX"
		+ Options.lineSeparator
		+ "             *.* containing{@java.beans.ConstructorProperties *(*)} public get*() and //JMX"
		+ Options.lineSeparator
		+ "             *.* @javafx.fxml.FXML * and //FXML"
		+ Options.lineSeparator
		+ "             *.* @javafx.fxml.FXML *(*) and //FXML"
		+ Options.lineSeparator
		+ "             *.* $deserializeLambda$(java.lang.invoke.SerializedLambda) and //Serialized lambda"
		+ Options.lineSeparator
		+ "             *.* lambda$*(*) //lambda"
		+ Options.lineSeparator
		+ "             ;"
		+ Options.lineSeparator;
	private final boolean deleteAnnotationAttributes;
	private MultiMap<MethodNode, StructureNode> usageReferenceMap;
	private ClassRepository classRepository;
	private CommonSuperClassAnalyzer commonSuperClassAnalyzer;
	private Set<AbstractClassNode> excludedAnnotationTypes;
	private DualKeyMap<ClassNode, FieldNode, FieldNode> trimmedFieldsByClass;
	private DualKeyMap<ClassNode, MethodNode, MethodNode> pendingStaticMethods;
	private DualKeyMap<ClassNode, FieldNode, FieldNode> pendingInstanceFields;
	private DualKeyMap<ClassNode, MethodNode, MethodNode> trimmedMethodsByClass;
	private List<FieldNode> trimmedFields;
	private List<MethodNode> retainedMethods;
	private ClassNode[] workspaceClasses;
	private Set<AbstractClassNode> annotationCandidates;
	private List<FieldNode> retainedFields;
	private List<MethodNode> trimmedMethods;
	private MethodNode[] allMethods;
	private DualKeyMap<ClassNode, MethodNode, MethodNode> pendingInstanceMethods;
	private boolean trimResultsComputed;
	private FieldNode[] allFields;
	private HashSet<ClassNode> instanceRetainedClasses;
	private DualKeyMap<ClassNode, FieldNode, FieldNode> pendingStaticFields;

	public TrimExcludes(
		Workspace workspace,
		ClassTreeManager classTreeManager,
		List<? extends ExclusionInclusionStatement> exclusionStatements,
		List<? extends ExclusionInclusionStatement> inclusionStatements,
		TrimConfig trimConfig,
		ScriptContext scriptContext,
		MultiMap<MethodNode, StructureNode> usageReferenceMap,
		SerializedClassManager serializedClassManager,
		FixedClassManager fixedClassManager
	) throws IOException {
		super(workspace, exclusionStatements, inclusionStatements, scriptContext);
		this.deleteAnnotationAttributes = trimConfig.deleteAnnotationAttributes;
		if (workspace.hasClasses()) {
			this.usageReferenceMap = usageReferenceMap;
			this.classRepository = workspace.getClassRepository();
			this.commonSuperClassAnalyzer = new CommonSuperClassAnalyzer(workspace, classTreeManager, this.classRepository);
			this.initializeProcessableMaps(workspace.getClasses(), workspace.getWorkspaceClassesSize(), classTreeManager);
			HashMap<RenameFilterParameter, RenameFilterParameter> seenExcludeParameters = GlobalUtils.newHashMap();
			ArrayList<RenameFilterParameter> excludeParameters = new ArrayList<>();

			for (int statementIndex = 0; statementIndex < super.exclusionStatements.size(); statementIndex++) {
				Enumeration<RenameFilterParameter> excludeParameterEnumeration = super.exclusionStatements.get(statementIndex).getRenameFilterParameters();

				while (excludeParameterEnumeration.hasMoreElements()) {
					RenameFilterParameter candidateExcludeParameter = excludeParameterEnumeration.nextElement();
					if (!seenExcludeParameters.containsKey(candidateExcludeParameter)) {
						seenExcludeParameters.put(candidateExcludeParameter, candidateExcludeParameter);
						excludeParameters.add(candidateExcludeParameter);
						if (candidateExcludeParameter.isAnnotationType() && trimConfig.deleteAnnotationAttributes && this.annotationCandidates == null) {
							this.collectAnnotationCandidates();
						}
					}
				}
			}

			if (scriptContext.isVerbose() && excludeParameters.size() > 0) {
				super.logWriter.println("\tTrim exclusion parameters:");

				for (int logIndex = excludeParameters.size() - 1; logIndex >= 0; logIndex--) {
					super.logWriter.println("\t\t" + excludeParameters.get(logIndex));
				}
			}

			for (int excludeParameterIndex = excludeParameters.size() - 1; excludeParameterIndex >= 0; excludeParameterIndex--) {
				RenameFilterParameter excludeParameter = excludeParameters.get(excludeParameterIndex);
				if (excludeParameter.isPackageTarget()) {
					scriptContext.logMessageInternal("Parameter '" + excludeParameter + "' in 'trimExclude' statement is a 'package' level parameter. It will be ignored.", true);
				} else if (excludeParameter.isClassTarget() && excludeParameter.isLinkClass() && excludeParameter.isClassWildcard()) {
					scriptContext.logWarningInternal("Parameter '" + excludeParameter + "' in 'trimExclude' statement cannot use both the '<link>' and '+' syntax. It will be ignored.", true);
				} else if (excludeParameter.isMethodTarget() && excludeParameter.hasLinkMethodNamePattern()) {
					scriptContext.logWarningInternal("Parameter '" + excludeParameter + "' in 'trimExclude' statement cannot use the '<link>' syntax. It will be ignored.", true);
				} else {
					if (excludeParameter.isMethodTarget() && excludeParameter.hasIncludeSignatureClasses()) {
						scriptContext.logWarningInternal("Parameter '" + excludeParameter + "' in 'trimExclude' cannot use the '+signatureClasses' keyword. It will be ignored.", true);
					}

					excludeParameter.applyTrimExclusion(this);
				}
			}

			if (super.inclusionStatements != null) {
				HashMap<RenameFilterParameter, RenameFilterParameter> seenUnexcludeParameters = GlobalUtils.newHashMap();
				ArrayList<RenameFilterParameter> unexcludeParameters = new ArrayList<>();

				for (int inclusionStatementIndex = 0; inclusionStatementIndex < super.inclusionStatements.size(); inclusionStatementIndex++) {
					Enumeration<RenameFilterParameter> unexcludeParameterEnumeration = super.inclusionStatements.get(inclusionStatementIndex).getRenameFilterParameters();

					while (unexcludeParameterEnumeration.hasMoreElements()) {
						RenameFilterParameter candidateUnexcludeParameter = unexcludeParameterEnumeration.nextElement();
						if (!seenUnexcludeParameters.containsKey(candidateUnexcludeParameter)) {
							seenUnexcludeParameters.put(candidateUnexcludeParameter, candidateUnexcludeParameter);
							unexcludeParameters.add(candidateUnexcludeParameter);
							if (candidateUnexcludeParameter.isAnnotationType() && trimConfig.deleteAnnotationAttributes && this.annotationCandidates == null) {
								this.collectAnnotationCandidates();
							}
						}
					}
				}

				if (scriptContext.isVerbose() && unexcludeParameters.size() > 0) {
					super.logWriter.println("\tTrim unexclusion parameters:");

					for (int unexcludeLogIndex = unexcludeParameters.size() - 1; unexcludeLogIndex >= 0; unexcludeLogIndex--) {
						super.logWriter.println("\t\t\"" + unexcludeParameters.get(unexcludeLogIndex) + "\"");
					}
				}

				if (unexcludeParameters.size() > 0 && excludeParameters.size() == 0) {
					scriptContext.logWarningInternal(
						"A 'trimUnexclude' statement can only operate on the results of a 'trimExclude' statement. No 'trimExclude' statement is in effect so the 'trimUnexclude' statement will be ignored.",
						true
					);
				} else {
					for (int unexcludeParameterIndex = unexcludeParameters.size() - 1; unexcludeParameterIndex >= 0; unexcludeParameterIndex--) {
						RenameFilterParameter unexcludeParameter = unexcludeParameters.get(unexcludeParameterIndex);
						if (unexcludeParameter.isPackageTarget()) {
							scriptContext.logWarningInternal("Parameter '" + unexcludeParameter + "' in 'trimUnexclude' statement is a 'package' level parameter. It will be ignored.", true);
						} else if (unexcludeParameter.isClassTarget() && unexcludeParameter.isLinkClass()) {
							scriptContext.logWarningInternal("Parameter '" + unexcludeParameter + "' in 'trimUnexclude' statement cannot use the '<link>' syntax. It will be ignored.", true);
						} else if (unexcludeParameter.isMethodTarget() && unexcludeParameter.hasLinkMethodNamePattern()) {
							scriptContext.logWarningInternal("Parameter '" + unexcludeParameter + "' in 'trimUnexclude' statement cannot use the '<link>' syntax. It will be ignored.", true);
						} else {
							if (unexcludeParameter.isMethodTarget() && unexcludeParameter.hasIncludeSignatureClasses()) {
								scriptContext.logWarningInternal("Parameter '" + unexcludeParameter + "' in 'trimUnexclude' cannot use the '+signatureClasses' keyword. It will be ignored.", true);
							}

							unexcludeParameter.applyTrimInclusion(this);
						}
					}
				}
			}

			ExclusionInclusionStatement defaultExclusionStatement = loadDefaultExclusions(scriptContext);
			ArrayList<RenameFilterParameter> defaultExcludeParameters = new ArrayList<>();
			if (defaultExclusionStatement != null) {
				HashMap<RenameFilterParameter, RenameFilterParameter> seenDefaultExcludeParameters = GlobalUtils.newHashMap();
				Enumeration<RenameFilterParameter> defaultExcludeParameterEnumeration = defaultExclusionStatement.getRenameFilterParameters();

				while (defaultExcludeParameterEnumeration.hasMoreElements()) {
					RenameFilterParameter candidateDefaultExcludeParameter = defaultExcludeParameterEnumeration.nextElement();
					if (!seenDefaultExcludeParameters.containsKey(candidateDefaultExcludeParameter)) {
						seenDefaultExcludeParameters.put(candidateDefaultExcludeParameter, candidateDefaultExcludeParameter);
						defaultExcludeParameters.add(candidateDefaultExcludeParameter);
					}
				}

				if (scriptContext.isVerbose() && defaultExcludeParameters.size() > 0) {
					super.logWriter.println("\tDefault trim exclusion parameters:");

					for (int defaultLogIndex = defaultExcludeParameters.size() - 1; defaultLogIndex >= 0; defaultLogIndex--) {
						super.logWriter.println("\t\t\"" + defaultExcludeParameters.get(defaultLogIndex) + "\"");
					}
				}

				for (int defaultParameterIndex = defaultExcludeParameters.size() - 1; defaultParameterIndex >= 0; defaultParameterIndex--) {
					RenameFilterParameter defaultExcludeParameter = defaultExcludeParameters.get(defaultParameterIndex);
					if (defaultExcludeParameter.isPackageTarget()) {
						scriptContext.logWarningInternal("Parameter '" + defaultExcludeParameter + "' in default 'trimExclude' is a 'package' level parameter. It will be ignored.", true);
					} else if (defaultExcludeParameter.isClassTarget() && defaultExcludeParameter.isLinkClass()) {
						scriptContext.logWarningInternal("Parameter '" + defaultExcludeParameter + "' in default 'trimExclude' cannot use the '<link>' syntax. It will be ignored.", true);
					} else if (defaultExcludeParameter.isMethodTarget() && defaultExcludeParameter.hasLinkMethodNamePattern()) {
						scriptContext.logWarningInternal("Parameter '" + defaultExcludeParameter + "' in 'trimExclude' statement cannot use the '<link>' syntax. It will be ignored.", true);
					} else {
						if (defaultExcludeParameter.isMethodTarget() && defaultExcludeParameter.hasIncludeSignatureClasses()) {
							scriptContext.logWarningInternal("Parameter '" + defaultExcludeParameter + "' in default 'trimExclude' cannot use the '+signatureClasses' keyword. It will be ignored.", true);
						}

						defaultExcludeParameter.applyTrimExclusion(this);
					}
				}
			}

			if (!trimConfig.deleteAnnotationAttributes) {
				Enumeration<RenameFilterParameter> packageInfoExclusionEnumeration = getPackageInfoExclusions(scriptContext).getRenameFilterParameters();

				while (packageInfoExclusionEnumeration.hasMoreElements()) {
					packageInfoExclusionEnumeration.nextElement().applyTrimExclusion(this);
				}
			}

			TrimExcludedMemberSet excludedMemberSet = new TrimExcludedMemberSet(super.excludedClasses.keySet(), super.excludedFields.keySet(), this.excludedMethods.keySet());
			scriptContext.setTrimExcludedMemberSet(excludedMemberSet);
			if (fixedClassManager != null) {
				fixedClassManager.applyTrimExclusions(this, scriptContext.getWriter());
			}

			if (serializedClassManager != null) {
				serializedClassManager.applyTrimExclusions(this, scriptContext.getWriter());
			}

			Map<ClassNode, String> archivePathByClassName = workspace.getClassNameToArchivePathMap();
			if (archivePathByClassName != null) {
				ArrayList<Entry<ClassNode, String>> classArchiveEntries = new ArrayList<>(archivePathByClassName.entrySet());
				TrimExcludes.ClassEntryComparator classEntryComparator = new TrimExcludes.ClassEntryComparator(this);
				Collections.sort(classArchiveEntries, classEntryComparator);

				for (int classArchiveEntryIndex = 0; classArchiveEntryIndex < classArchiveEntries.size(); classArchiveEntryIndex++) {
					Entry<ClassNode, String> classArchiveEntry = classArchiveEntries.get(classArchiveEntryIndex);
					this.excludeClass(classArchiveEntry.getKey(), classArchiveEntry.getValue());
				}
			}

			TrimExcludes.MemberEntryComparator memberEntryComparator = new TrimExcludes.MemberEntryComparator(this);
			Map<FieldNode, String> reasonByField = workspace.getClassPackageMap();
			if (reasonByField != null) {
				ArrayList<Entry<FieldNode, String>> fieldEntries = new ArrayList<>(reasonByField.entrySet());
				Collections.sort(fieldEntries, memberEntryComparator);

				for (int fieldEntryIndex = 0; fieldEntryIndex < fieldEntries.size(); fieldEntryIndex++) {
					Entry<FieldNode, String> fieldEntry = fieldEntries.get(fieldEntryIndex);
					this.excludeField(fieldEntry.getKey(), fieldEntry.getValue());
				}
			}

			Map<MethodNode, String> reasonByMethod = workspace.getEntryPointSignatures();
			if (reasonByMethod != null) {
				ArrayList<Entry<MethodNode, String>> methodEntries = new ArrayList<>(reasonByMethod.entrySet());
				Collections.sort(methodEntries, memberEntryComparator);

				for (int methodEntryIndex = 0; methodEntryIndex < methodEntries.size(); methodEntryIndex++) {
					Entry<MethodNode, String> methodEntry = methodEntries.get(methodEntryIndex);
					this.excludeMethod(methodEntry.getKey(), methodEntry.getValue());
				}
			}

			DualKeyMap<MethodNode, String, Object> resourceClassReferences = workspace.getResourceToClassReferences();
			if (resourceClassReferences != null) {
				for (MethodNode resourceMethod : resourceClassReferences.getGroupKeySet()) {
					this.excludeMethod(resourceMethod, null);
				}
			}

			for (ClassNode excludedClass : super.excludedClasses.keySet()) {
				this.instanceRetainedClasses.add(excludedClass);
			}

			this.retainReferencedMembers();
			this.retainLibraryOverrides();
		}
	}

	private static ExclusionInclusionStatement fallbackToDefaultExclusions(ScriptContext scriptContext, Throwable parseError) throws IOException {
		String defaultExcludePath = scriptContext.getDefaultTrimExcludePath();
		if (parseError != null) {
			System.err.println("\"" + defaultExcludePath + "\" had a parse error and will be ignored. See \"" + scriptContext.getLogFilePath() + "\" for more detail.");
			scriptContext.logWarning(
				"\""
					+ defaultExcludePath
					+ "\" had a parse error :"
					+ Options.lineSeparator
					+ parseError.getMessage()
					+ Options.lineSeparator
					+ "Will use the internal default trim statement and ignore \""
					+ defaultExcludePath
					+ "\"."
			);
		}

		BufferedReader fallbackReader = new BufferedReader(new StringReader(DEFAULT_FALLBACK_EXCLUDES));

		try {
			return parseExclusionStatement(scriptContext, fallbackReader);
		}
		catch (ParsingException ignored) {
			return null;
		} catch (LexicalException ignored) {
			return null;
		}
	}

	public static ExclusionInclusionStatement getPackageInfoExclusions(ScriptContext scriptContext) throws IOException {
		return parseExclusionText(DEFAULT_PACKAGEINFO_EXCLUDES, scriptContext);
	}

	private static ExclusionInclusionStatement parseExclusionStatement(ScriptContext scriptContext, BufferedReader reader) throws IOException {
		ScriptParser scriptParser = new ScriptParser(reader);

		AbstractScriptNode scriptNode;
		try {
			scriptNode = scriptParser.parseDefaultTrimExcludeInput();
			scriptNode.parseArguments(null, scriptContext);
		} finally {
			try {
				reader.close();
			} catch (IOException ignored) {
			}
		}

		return ((DefaultTrimExcludeInput)scriptNode).getExclusionStatement();
	}

	private static ExclusionInclusionStatement parseExclusionText(String exclusionText, ScriptContext scriptContext) throws IOException {
		BufferedReader reader = new BufferedReader(new StringReader(exclusionText));

		try {
			return parseExclusionStatement(scriptContext, reader);
		} catch (ParsingException ignored) {
		} catch (LexicalException ignored2) {
		}

		return null;
	}

	public static ExclusionInclusionStatement getClasslibExclusions(ScriptContext scriptContext) throws IOException {
		return parseExclusionText(DEFAULT_CLASSLIB_EXCLUDES, scriptContext);
	}

	public static ExclusionInclusionStatement getMidletExclusions(ScriptContext scriptContext) throws IOException {
		return parseExclusionText(DEFAULT_MIDLET_EXCLUDES, scriptContext);
	}

	public static ExclusionInclusionStatement loadDefaultExclusions(ScriptContext scriptContext) throws IOException {
		String defaultExcludePath = scriptContext.getDefaultTrimExcludePath();
		boolean usingInternalDefaults = false;

		BufferedReader reader;
		try {
			reader = FileUtils.createBufferedReader(new File(defaultExcludePath), Options.ZKM_SCRIPT_ENCODING);
			scriptContext.logRaw("Using '" + defaultExcludePath + "' for default trim exclusions", true);
		} catch (FileNotFoundException ignored) {
			reader = new BufferedReader(new StringReader(DEFAULT_FALLBACK_EXCLUDES));
			usingInternalDefaults = true;
			scriptContext.logRaw("Using internal default trim exclusions. File '" + defaultExcludePath + "' not found", true);
		} catch (IOException ioError) {
			reader = new BufferedReader(new StringReader(DEFAULT_FALLBACK_EXCLUDES));
			usingInternalDefaults = true;
			scriptContext.logRaw("Using internal default trim exclusions. Error opening file '" + defaultExcludePath + "' : " + ioError, true);
		}

		try {
			return parseExclusionStatement(scriptContext, reader);
		} catch (ParsingException parsingError) {
			if (!usingInternalDefaults) {
				return fallbackToDefaultExclusions(scriptContext, parsingError);
			}
		} catch (LexicalException lexicalError) {
			if (!usingInternalDefaults) {
				return fallbackToDefaultExclusions(scriptContext, lexicalError);
			}
		}

		return null;
	}

	public static ExclusionInclusionStatement getFrameworkExclusions(ScriptContext scriptContext) throws IOException {
		return parseExclusionText(DEFAULT_FRAMEWORK_EXCLUDES, scriptContext);
	}

	public ArrayList<ClassNode> getTrimmedFieldOwners() {
		ArrayList<ClassNode> trimmedOwners = new ArrayList<>();
		Enumeration<ClassNode> ownerEnumeration = this.trimmedFieldsByClass.getGroupKeys();

		while (ownerEnumeration.hasMoreElements()) {
			ClassNode ownerClass = ownerEnumeration.nextElement();
			if (super.excludedClasses.containsKey(ownerClass)) {
				trimmedOwners.add(ownerClass);
			}
		}

		return trimmedOwners;
	}

	private void retainClassAndInnerClasses(ClassNode classNode, RingQueue<MethodNode> methodQueue, boolean retainInstanceMembers) {
		if (classNode.hasInnerClasses()) {
			this.retainClass(classNode, methodQueue, retainInstanceMembers);

			for (AbstractClassNode innerClass : classNode.getInnerClasses()) {
				this.retainClass((ClassNode)innerClass, methodQueue, retainInstanceMembers);
			}
		} else if (classNode.isInnerClass()) {
			ClassNode outerClass = (ClassNode)classNode.getOuterClass();
			this.retainClass(outerClass, methodQueue, retainInstanceMembers);

			for (AbstractClassNode outerInnerClass : outerClass.getInnerClasses()) {
				this.retainClass((ClassNode)outerInnerClass, methodQueue, retainInstanceMembers);
			}
		} else {
			this.retainClass(classNode, methodQueue, retainInstanceMembers);
		}
	}

	public Enumeration<MethodNode> getAllTrimmedMethods() {
		return this.trimmedMethodsByClass.getAllValues();
	}

	public boolean excludeAnnotationType(AbstractClassNode classNode, String reason) {
		if (this.excludedAnnotationTypes == null) {
			this.excludedAnnotationTypes = GlobalUtils.newHashSet();
		}

		boolean newlyExcluded = this.excludedAnnotationTypes.add(classNode);
		if (newlyExcluded && super.scriptContext.isVerbose() && reason != null && super.logWriter != null) {
			super.logWriter.println("\tMatching for annotation attribute exclusion class \"" + this.formatClassForDisplay(classNode) + "\" because of \"" + reason + "\"");
		}

		return newlyExcluded;
	}

	@Override
	public final Enumeration<ClassNode> getWorkspaceClasses() {
		return new ArrayEnumeration<>(this.workspaceClasses);
	}

	private void scanClassReferences(ClassNode classNode, RingQueue<MethodNode> methodQueue) {
		RingQueue<ClassNode> classQueue = new RingQueue<>();
		HashSet<ClassNode> scannedClasses = GlobalUtils.newHashSet();
		HashSet<ClassNode> retainedScannedClasses = GlobalUtils.newHashSet();
		classQueue.push(classNode);

		while (!classQueue.isEmpty()) {
			ClassNode currentClass = classQueue.pop();
			boolean instanceRetained = this.instanceRetainedClasses.contains(currentClass);
			boolean firstScan = scannedClasses.add(currentClass);
			boolean firstRetainedScan = false;
			if (instanceRetained) {
				firstRetainedScan = retainedScannedClasses.add(currentClass);
			}

			if (firstScan || firstRetainedScan) {
				ClassHierarchyNode hierarchyNode = ClassHierarchyNode.getNodeByInternalName(currentClass.getInternalName());
				if (firstScan) {
					ReferenceSetContainer referenceSet = currentClass.buildReferenceSetContainer();
					Enumeration<?> referenceEnumeration = referenceSet.getClassReferences();

					while (referenceEnumeration.hasMoreElements()) {
						ClassNode referencedClass = (ClassNode)referenceEnumeration.nextElement();
						this.scanClassAndInnerClasses(referencedClass, classQueue, methodQueue, true);
					}

					referenceEnumeration = referenceSet.getSignatureClassReferences();

					while (referenceEnumeration.hasMoreElements()) {
						ClassNode signatureClass = (ClassNode)referenceEnumeration.nextElement();
						this.scanClassAndInnerClasses(signatureClass, classQueue, methodQueue, false);
					}

					referenceEnumeration = referenceSet.getFieldReferences();

					while (referenceEnumeration.hasMoreElements()) {
						FieldNode referencedField = (FieldNode)referenceEnumeration.nextElement();
						this.excludeFieldWithDependencies(referencedField, methodQueue);
					}

					referenceEnumeration = referenceSet.getMethodReferences();

					while (referenceEnumeration.hasMoreElements()) {
						MethodNode referencedMethod = (MethodNode)referenceEnumeration.nextElement();
						this.excludeMethodAndQueue(referencedMethod, methodQueue);
					}

					ClassHierarchyNode superClassNode = hierarchyNode.getSuperClassNode();
					if (superClassNode != null) {
						ClassNode superClass = superClassNode.getClassNodeIfWorkspace();
						if (superClass != null) {
							this.scanClassAndInnerClasses(superClass, classQueue, methodQueue, false);
						}
					}
				}

				ClassNode outerClass = hierarchyNode.getOuterClassWrapper();
				if (outerClass != null) {
					if (classNode.isInnerClass() && outerClass.hasInnerClasses()) {
						outerClass = (ClassNode)outerClass.findInnerClassByOrdinal(classNode.getInnerClassOrdinal());
					}

					this.scanClassAndInnerClasses(outerClass, classQueue, methodQueue, instanceRetained);
				}

				Enumeration<ClassHierarchyNode> interfaceEnumeration = hierarchyNode.getInterfacesEnumeration();
				if (interfaceEnumeration != null) {
					while (interfaceEnumeration.hasMoreElements()) {
						ClassHierarchyNode interfaceNode = interfaceEnumeration.nextElement();
						if (interfaceNode.isWorkspaceClass()) {
							ClassNode interfaceClass = interfaceNode.getClassNodeIfWorkspace();
							if (classNode.isInnerClass() && interfaceClass.hasInnerClasses()) {
								interfaceClass = (ClassNode)interfaceClass.findInnerClassByOrdinal(classNode.getInnerClassOrdinal());
							}

							this.scanClassAndInnerClasses(interfaceClass, classQueue, methodQueue, instanceRetained);
						}
					}
				}
			}
		}
	}

	public boolean isAnnotationTypeExcluded(AbstractClassNode classNode) {
		return this.excludedAnnotationTypes != null && this.excludedAnnotationTypes.contains(classNode);
	}

	public final void excludeField(FieldNode fieldNode, String reason) {
		ClassNode ownerClass = super.processableFields.remove(fieldNode);
		if (ownerClass != null) {
			super.excludedFields.put(fieldNode, ownerClass);
			if (super.scriptContext.isVerbose() && reason != null && super.logWriter != null) {
				super.logWriter
					.println(
						"\tMatching field \""
							+ ExcludeStatementManager.formatFieldForDisplay(fieldNode, this)
							+ "\" in class \""
							+ this.formatClassForDisplay(fieldNode.getOwnerClass())
							+ "\" because of \""
							+ reason
							+ "\""
					);
			}
		}
	}

	public final Enumeration<FieldNode> getTrimmedFields() {
		return Collections.enumeration(this.trimmedFields);
	}

	@Override
	public final boolean hasNoProcessableFields() {
		return this.trimmedFields.size() == 0;
	}

	public void computeTrimResults() {
		if (this.workspace.hasClasses()) {
			this.resolveRetainedMembers();
			this.retainOverriddenMethods();
			this.classifyTrimmedMembers();
			this.trimResultsComputed = true;
		}
	}

	private void excludeFieldWithDependencies(FieldNode fieldNode, RingQueue<MethodNode> methodQueue) {
		ClassNode ownerClass = fieldNode.getOwnerClass();
		FieldSignature fieldSignature = fieldNode.getFieldSignature();
		if (ownerClass.hasInnerClasses()) {
			this.excludeField(fieldNode, null);
			Iterator<AbstractClassNode> innerClassIterator = ownerClass.getInnerClasses().iterator();

			while (innerClassIterator.hasNext()) {
				FieldNode innerField = (FieldNode)innerClassIterator.next().findField(fieldSignature);
				if (innerField != null) {
					this.excludeField(innerField, null);
				}
			}
		} else if (ownerClass.isInnerClass()) {
			ClassNode outerClass = (ClassNode)ownerClass.getOuterClass();
			FieldNode outerField = outerClass.findFieldNode(fieldSignature);
			if (outerField != null) {
				this.excludeField(outerField, null);
			}

			Iterator<AbstractClassNode> outerInnerClassIterator = outerClass.getInnerClasses().iterator();

			while (outerInnerClassIterator.hasNext()) {
				FieldNode outerInnerField = (FieldNode)outerInnerClassIterator.next().findField(fieldSignature);
				if (outerInnerField != null) {
					this.excludeField(outerInnerField, null);
				}
			}
		} else {
			this.excludeField(fieldNode, (String)null);
		}

		ClassNode fieldTypeClass = fieldNode.getFieldTypeClass();
		if (fieldTypeClass != null) {
			this.retainClassAndInnerClasses(fieldTypeClass, methodQueue, true);
		}

		ReferenceSetContainer referenceSet = fieldNode.getAllReferences();
		this.scanMemberReferences(referenceSet, methodQueue);
	}

	public boolean isDeleteAnnotationAttributes() {
		return this.deleteAnnotationAttributes;
	}

	private void collectAnnotationCandidates() {
		this.annotationCandidates = GlobalUtils.newHashSet();

		for (ClassNode workspaceClass : this.workspaceClasses) {
			if (workspaceClass.isAnnotation()) {
				this.annotationCandidates.add(workspaceClass);
			}

			workspaceClass.collectAnnotationClassRefs(this.annotationCandidates);
		}
	}

	public final void unexcludeField(FieldNode fieldNode, String reason) {
		ClassNode ownerClass = super.excludedFields.remove(fieldNode);
		if (ownerClass != null) {
			super.processableFields.put(fieldNode, ownerClass);
			if (super.scriptContext.isVerbose() && reason != null && super.logWriter != null) {
				super.logWriter
					.println(
						"\tUnmatching field \""
							+ ExcludeStatementManager.formatFieldForDisplay(fieldNode, this)
							+ "\" in class \""
							+ this.formatClassForDisplay(fieldNode.getOwnerClass())
							+ "\" for trim because of \""
							+ reason
							+ "\""
					);
			}
		}
	}

	public ReadOnlyMap<FieldNode, FieldNode> getTrimmedFieldsForClass(ClassNode classNode) {
		Map<FieldNode, FieldNode> trimmedFields = this.trimmedFieldsByClass.getGroup(classNode);
		return trimmedFields != null ? new ReadOnlyMap<>(trimmedFields) : null;
	}

	@Override
	public final boolean excludeClass(ClassNode classNode, String reason) {
		Object removedClass = super.processableClasses.remove(classNode);
		if (removedClass != null) {
			super.excludedClasses.put(classNode, classNode);
			if (super.scriptContext.isVerbose() && reason != null && super.logWriter != null) {
				super.logWriter.println("\tMatching class \"" + this.formatClassForDisplay(classNode) + "\" because of \"" + reason + "\"");
			}
		}

		return removedClass != null;
	}

	private void processPendingFields(RingQueue<MethodNode> methodQueue, DualKeyMap<ClassNode, FieldNode, FieldNode> pendingFieldMap, ClassNode classNode) {
		Map<FieldNode, FieldNode> pendingFields = pendingFieldMap.getGroup(classNode);
		if (pendingFields != null) {
			for (Object pendingField : pendingFields.keySet()) {
				this.excludeFieldWithDependencies((FieldNode)pendingField, methodQueue);
			}

			pendingFieldMap.removeGroup(classNode);
		}
	}

	private void retainReferencedMembers() {
		RingQueue<ClassNode> classQueue = new RingQueue<>();

		for (ClassNode excludedClass : super.excludedClasses.keySet()) {
			classQueue.push(excludedClass);
		}

		HashSet<ClassNode> scannedClasses = GlobalUtils.newHashSet();
		HashSet<ClassNode> retainedScannedClasses = GlobalUtils.newHashSet();

		while (!classQueue.isEmpty()) {
			ClassNode currentClass = classQueue.pop();
			boolean instanceRetained = this.instanceRetainedClasses.contains(currentClass);
			boolean firstScan = scannedClasses.add(currentClass);
			boolean firstRetainedScan = false;
			if (instanceRetained) {
				firstRetainedScan = retainedScannedClasses.add(currentClass);
			}

			if (firstScan || firstRetainedScan) {
				scannedClasses.add(currentClass);
				ClassHierarchyNode hierarchyNode = ClassHierarchyNode.getNodeByInternalName(currentClass.getInternalName());
				ReferenceSetContainer referenceSet = currentClass.buildReferenceSetContainer();
				Enumeration<?> referenceEnumeration = referenceSet.getClassReferences();

				while (referenceEnumeration.hasMoreElements()) {
					ClassNode referencedClass = (ClassNode)referenceEnumeration.nextElement();
					boolean classExcluded = this.excludeClassSilently(referencedClass);
					boolean newlyRetained = this.instanceRetainedClasses.add(referencedClass);
					if (classExcluded || newlyRetained) {
						classQueue.push(referencedClass);
					}
				}

				referenceEnumeration = referenceSet.getSignatureClassReferences();

				while (referenceEnumeration.hasMoreElements()) {
					ClassNode signatureClass = (ClassNode)referenceEnumeration.nextElement();
					if (this.excludeClassSilently(signatureClass)) {
						classQueue.push(signatureClass);
					}
				}

				referenceEnumeration = referenceSet.getFieldReferences();

				while (referenceEnumeration.hasMoreElements()) {
					FieldNode referencedField = (FieldNode)referenceEnumeration.nextElement();
					this.retainReferencedField(referencedField, classQueue);
				}

				referenceEnumeration = referenceSet.getMethodReferences();

				while (referenceEnumeration.hasMoreElements()) {
					MethodNode referencedMethod = (MethodNode)referenceEnumeration.nextElement();
					this.excludeMethodSilently(referencedMethod);
				}

				ClassHierarchyNode superClassNode = hierarchyNode.getSuperClassNode();
				if (superClassNode != null) {
					ClassNode superClass = superClassNode.getClassNodeIfWorkspace();
					if (superClass != null && this.excludeClassSilently(superClass)) {
						classQueue.push(superClass);
					}
				}

				ClassNode outerClass = hierarchyNode.getOuterClassWrapper();
				if (outerClass != null) {
					boolean outerClassExcluded = this.excludeClassSilently(outerClass);
					boolean outerNewlyRetained = false;
					if (instanceRetained) {
						outerNewlyRetained = this.instanceRetainedClasses.add(outerClass);
					}

					if (outerClassExcluded || outerNewlyRetained) {
						classQueue.push(outerClass);
					}
				}

				Enumeration<ClassHierarchyNode> interfaceEnumeration = hierarchyNode.getInterfacesEnumeration();
				if (interfaceEnumeration != null) {
					while (interfaceEnumeration.hasMoreElements()) {
						ClassHierarchyNode interfaceNode = interfaceEnumeration.nextElement();
						if (interfaceNode.isWorkspaceClass()) {
							ClassNode interfaceClass = interfaceNode.getClassNodeIfWorkspace();
							boolean interfaceClassExcluded = this.excludeClassSilently(interfaceClass);
							boolean interfaceNewlyRetained = false;
							if (instanceRetained) {
								interfaceNewlyRetained = this.instanceRetainedClasses.add(interfaceClass);
							}

							if (interfaceClassExcluded || interfaceNewlyRetained) {
								classQueue.push(interfaceClass);
							}
						}
					}
				}
			}
		}
	}

	public ReadOnlyMap<MethodNode, MethodNode> getTrimmedMethodsForClass(ClassNode classNode) {
		Map<MethodNode, MethodNode> trimmedMethods = this.trimmedMethodsByClass.getGroup(classNode);
		return trimmedMethods != null ? new ReadOnlyMap<>(trimmedMethods) : null;
	}

	@Override
	public final void includeClass(ClassNode classNode, String reason) {
		if (super.excludedClasses.remove(classNode) != null) {
			super.processableClasses.put(classNode, classNode);
			if (super.scriptContext.isVerbose() && reason != null && super.logWriter != null) {
				super.logWriter.println("\tUnmatching class \"" + this.formatClassForDisplay(classNode) + "\" for trim because of \"" + reason + "\"");
			}
		}
	}

	@Override
	public final boolean hasNoProcessableMethods() {
		return this.trimmedMethods.size() == 0;
	}

	@Override
	public boolean isFieldExcluded(FieldNode fieldNode) {
		return fieldNode.isStatic()
			? super.excludedFields.containsKey(fieldNode) && super.excludedClasses.containsKey(fieldNode.getOwnerClass())
			: super.excludedFields.containsKey(fieldNode) && this.instanceRetainedClasses.contains(fieldNode.getOwnerClass());
	}

	private void resolveRetainedMembers() {
		RingQueue<MethodNode> methodQueue = new RingQueue<>();

		for (FieldNode excludedField : new ArrayList<>(super.excludedFields.keySet())) {
			ClassNode ownerClass = excludedField.getOwnerClass();
			if (super.excludedClasses.containsKey(ownerClass)) {
				ClassNode fieldTypeClass = excludedField.getFieldTypeClass();
				if (fieldTypeClass != null) {
					this.retainClassAndInnerClasses(fieldTypeClass, methodQueue, true);
				}

				ReferenceSetContainer fieldReferenceSet = excludedField.getAllReferences();
				this.scanMemberReferences(fieldReferenceSet, methodQueue);
			} else if (!excludedField.isStatic()) {
				this.pendingInstanceFields.put(ownerClass, excludedField, excludedField);
			} else {
				this.pendingStaticFields.put(ownerClass, excludedField, excludedField);
			}
		}

		for (MethodNode excludedMethod : this.excludedMethods.keySet()) {
			ClassNode methodOwner = excludedMethod.getMethodOwner();
			if (this.instanceRetainedClasses.contains(methodOwner)) {
				methodQueue.push(excludedMethod);
			} else if (super.excludedClasses.containsKey(methodOwner) && excludedMethod.isStatic()) {
				methodQueue.push(excludedMethod);
			} else if (excludedMethod.isStatic()) {
				this.pendingStaticMethods.put(methodOwner, excludedMethod, excludedMethod);
			} else {
				this.pendingInstanceMethods.put(methodOwner, excludedMethod, excludedMethod);
			}
		}

		HashSet<MethodNode> scannedMethods = GlobalUtils.newHashSet(GlobalUtils.getInitialCapacity(this.workspaceClasses.length + 5));

		while (!methodQueue.isEmpty()) {
			MethodNode currentMethod = methodQueue.pop();
			ClassNode currentOwner = currentMethod.getMethodOwner();
			if (scannedMethods.add(currentMethod)) {
				if (currentOwner.hasInnerClasses() || currentOwner.isInnerClass()) {
					MethodSignature methodSignature = currentMethod.getMethodSignature();
					Iterator<AbstractClassNode> innerClassIterator = currentOwner.getSelfAndAllInnerClasses().iterator();

					while (innerClassIterator.hasNext()) {
						MethodNode innerMethod = (MethodNode)innerClassIterator.next().findMethod(methodSignature);
						if (innerMethod != null && !scannedMethods.contains(innerMethod) && !methodQueue.contains(innerMethod)) {
							if (!innerMethod.isStatic()) {
								this.instanceRetainedClasses.add(innerMethod.getMethodOwner());
							}

							this.excludeMethodAndQueue(innerMethod, methodQueue);
						}
					}
				}

				Enumeration<MethodNode> overridingMethodEnumeration = this.commonSuperClassAnalyzer.getOverridingMethods(currentMethod);

				while (overridingMethodEnumeration.hasMoreElements()) {
					MethodNode overridingMethod = overridingMethodEnumeration.nextElement();
					ClassNode overridingOwner = overridingMethod.getMethodOwner();
					if (currentOwner.isInnerClass() && overridingOwner.hasInnerClasses()) {
						ClassNode originalOverridingOwner = overridingOwner;
						overridingOwner = (ClassNode)overridingOwner.findInnerClassByOrdinal(currentOwner.getInnerClassOrdinal());
						if (originalOverridingOwner != overridingOwner) {
							MethodNode originalOverridingMethod = overridingMethod;
							overridingMethod = overridingOwner.findMethodNode(overridingMethod.getMethodSignature());
							DebugUtils.assertNull(
								overridingMethod,
								"Multi-version class '"
									+ overridingOwner.getRawSourcePath()
									+ "' does not contain a method '"
									+ originalOverridingMethod.getSignature()
									+ "' while processing class '"
									+ currentOwner.getRawSourcePath()
									+ "' (A)."
							);
						}
					}

					if (this.instanceRetainedClasses.contains(overridingOwner)) {
						this.excludeMethodAndQueue(overridingMethod, methodQueue);
					} else {
						this.excludeMethodSilently(overridingMethod);
						this.pendingInstanceMethods.put(overridingOwner, overridingMethod, overridingMethod);
					}
				}

				for (MethodNode overriddenMethod : this.commonSuperClassAnalyzer.getOverriddenMethods(currentMethod)) {
					ClassNode overriddenOwner = overriddenMethod.getMethodOwner();
					if (currentOwner.isInnerClass() && overriddenOwner.hasInnerClasses()) {
						ClassNode originalOverriddenOwner = overriddenOwner;
						overriddenOwner = (ClassNode)overriddenOwner.findInnerClassByOrdinal(currentOwner.getInnerClassOrdinal());
						if (originalOverriddenOwner != overriddenOwner) {
							MethodNode originalOverriddenMethod = overriddenMethod;
							overriddenMethod = overriddenOwner.findMethodNode(overriddenMethod.getMethodSignature());
							DebugUtils.assertNull(
								overriddenMethod,
								"Multi-version class '"
									+ overriddenOwner.getRawSourcePath()
									+ "' does not contain a method '"
									+ originalOverriddenMethod.getSignature()
									+ "' while processing class '"
									+ currentOwner.getRawSourcePath()
									+ "' (B)."
							);
						}
					}

					if (this.instanceRetainedClasses.contains(overriddenOwner)) {
						this.excludeMethodAndQueue(overriddenMethod, methodQueue);
					} else {
						this.excludeMethodSilently(overriddenMethod);
						this.pendingInstanceMethods.put(overriddenOwner, overriddenMethod, overriddenMethod);
					}
				}

				for (ClassNode descriptorClass : currentMethod.getDescriptorWorkspaceClasses()) {
					if (currentOwner.isInnerClass() && descriptorClass.hasInnerClasses()) {
						descriptorClass = (ClassNode)descriptorClass.findInnerClassByOrdinal(currentOwner.getInnerClassOrdinal());
					}

					this.retainClassAndInnerClasses(descriptorClass, methodQueue, true);
				}

				ReferenceSetContainer methodReferenceSet = currentMethod.getAllReferences();
				this.scanMemberReferences(methodReferenceSet, methodQueue);
				HashSet<ClassNode> instanceReferencedClasses = GlobalUtils.newHashSet();
				HashSet<ClassNode> staticReferencedClasses = GlobalUtils.newHashSet();
				HashSet<FieldNode> dependencyFields = GlobalUtils.newHashSet();
				HashSet<MethodNode> dependencyMethods = GlobalUtils.newHashSet();
				currentMethod.collectRenameDependencies(instanceReferencedClasses, staticReferencedClasses, dependencyFields, dependencyMethods);
				staticReferencedClasses.removeAll(instanceReferencedClasses);

				for (ClassNode instanceReferencedClass : instanceReferencedClasses) {
					this.retainClassAndInnerClasses(instanceReferencedClass, methodQueue, true);
				}

				for (ClassNode staticReferencedClass : staticReferencedClasses) {
					this.retainClassAndInnerClasses(staticReferencedClass, methodQueue, false);
				}

				for (FieldNode dependencyField : dependencyFields) {
					this.excludeFieldWithDependencies(dependencyField, methodQueue);
				}

				for (MethodNode dependencyMethod : dependencyMethods) {
					this.excludeMethodAndQueue(dependencyMethod, methodQueue);
				}

				List<StructureNode> usageReferences = this.usageReferenceMap.get(currentMethod);
				if (usageReferences != null) {
					TrimExcludes.ClassNodeFirstComparator classNodeFirstComparator = new TrimExcludes.ClassNodeFirstComparator(this);
					Collections.sort(usageReferences, classNodeFirstComparator);

					for (int usageReferenceIndex = 0; usageReferenceIndex < usageReferences.size(); usageReferenceIndex++) {
						Object usageReference = usageReferences.get(usageReferenceIndex);
						if (usageReference instanceof ClassNode) {
							ClassNode usageReferenceClass = (ClassNode)usageReference;
							this.retainClassAndInnerClasses(usageReferenceClass, methodQueue, true);
						} else if (usageReference instanceof FieldNode) {
							FieldNode usageReferenceField = (FieldNode)usageReference;
							this.excludeFieldWithDependencies(usageReferenceField, methodQueue);
						} else if (usageReference instanceof MethodNode) {
							MethodNode usageReferenceMethod = (MethodNode)usageReference;
							this.excludeMethodAndQueue(usageReferenceMethod, methodQueue);
						}
					}
				}
			}
		}
	}

	public final void excludeMethod(MethodNode methodNode, String reason) {
		ClassNode ownerClass = this.processableMethods.remove(methodNode);
		if (ownerClass != null) {
			this.excludedMethods.put(methodNode, ownerClass);
			if (super.scriptContext.isVerbose() && reason != null && super.logWriter != null) {
				super.logWriter
					.println(
						"\tMatching method \""
							+ ExcludeStatementManager.formatMethodForDisplay(methodNode, this)
							+ "\" in class \""
							+ this.formatClassForDisplay(methodNode.getMethodOwner())
							+ (reason != null && reason.length() > 0 ? "\" because of \"" + reason + "\"" : "\"")
					);
			}
		}
	}

	private void excludeMethodSilently(MethodNode methodNode) {
		this.excludeMethod(methodNode, null);
	}

	public final Enumeration<MethodNode> getTrimmedMethods() {
		return Collections.enumeration(this.trimmedMethods);
	}

	public boolean hasAnnotationTypeExclusions() {
		return this.excludedAnnotationTypes != null && this.excludedAnnotationTypes.size() > 0;
	}

	private void processPendingMethods(RingQueue<MethodNode> methodQueue, DualKeyMap<ClassNode, MethodNode, MethodNode> pendingMethodMap, ClassNode classNode) {
		Map<MethodNode, MethodNode> pendingMethods = pendingMethodMap.getGroup(classNode);
		if (pendingMethods != null) {
			for (MethodNode pendingMethod : pendingMethods.keySet()) {
				methodQueue.push(pendingMethod);
			}

			pendingMethodMap.removeGroup(classNode);
		}
	}

	@Override
	public final Enumeration<MethodNode> getAllMethods() {
		return new ArrayEnumeration<>(this.allMethods);
	}

	private boolean excludeClassSilently(ClassNode classNode) {
		return this.excludeClass(classNode, null);
	}

	private void scanMemberReferences(ReferenceSetContainer referenceSet, RingQueue<MethodNode> methodQueue) {
		Enumeration<?> referenceEnumeration = referenceSet.getClassReferences();

		while (referenceEnumeration.hasMoreElements()) {
			ClassNode referencedClass = (ClassNode)referenceEnumeration.nextElement();
			this.retainClassAndInnerClasses(referencedClass, methodQueue, true);
		}

		referenceEnumeration = referenceSet.getSignatureClassReferences();

		while (referenceEnumeration.hasMoreElements()) {
			ClassNode signatureClass = (ClassNode)referenceEnumeration.nextElement();
			this.retainClassAndInnerClasses(signatureClass, methodQueue, false);
		}

		referenceEnumeration = referenceSet.getFieldReferences();

		while (referenceEnumeration.hasMoreElements()) {
			FieldNode referencedField = (FieldNode)referenceEnumeration.nextElement();
			this.excludeFieldWithDependencies(referencedField, methodQueue);
		}

		referenceEnumeration = referenceSet.getMethodReferences();

		while (referenceEnumeration.hasMoreElements()) {
			MethodNode referencedMethod = (MethodNode)referenceEnumeration.nextElement();
			this.excludeMethodAndQueue(referencedMethod, methodQueue);
		}
	}

	public final void initializeProcessableMaps(Enumeration<ClassNode> classes, int classCount, ClassTreeManager classTreeManager) {
		ArrayList<ClassNode> classList = new ArrayList<>();
		int loadFactor = GlobalUtils.getInitialCapacity(classCount);
		super.processableClasses = GlobalUtils.newHashMap(loadFactor);
		super.excludedClasses = GlobalUtils.newHashMap(loadFactor);
		super.processableFields = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(classCount * 5));
		super.excludedFields = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(classCount * 5));
		this.processableMethods = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(classCount * 5));
		this.excludedMethods = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(classCount * 5));
		this.instanceRetainedClasses = GlobalUtils.newHashSet(loadFactor);
		this.pendingStaticFields = new DualKeyMap<>(loadFactor, GlobalUtils.getInitialCapacity(5));
		this.pendingInstanceFields = new DualKeyMap<>(loadFactor, GlobalUtils.getInitialCapacity(5));
		this.pendingStaticMethods = new DualKeyMap<>(loadFactor, GlobalUtils.getInitialCapacity(5));
		this.pendingInstanceMethods = new DualKeyMap<>(loadFactor, GlobalUtils.getInitialCapacity(5));
		this.trimmedFieldsByClass = new DualKeyMap<>(loadFactor);
		this.trimmedMethodsByClass = new DualKeyMap<>(loadFactor);

		while (classes.hasMoreElements()) {
			ClassNode classNode = classes.nextElement();
			this.registerProcessableClass(classNode, classList);
			if (classNode.hasInnerClasses()) {
				for (AbstractClassNode innerClass : classNode.getInnerClasses()) {
					this.registerProcessableClass((ClassNode)innerClass, classList);
				}
			}
		}

		this.workspaceClasses = classList.toArray(new ClassNode[classList.size()]);
		this.allFields = new FieldNode[super.processableFields.size()];
		int fieldIndex = 0;
		Iterator<FieldNode> fieldIterator = super.processableFields.keySet().iterator();

		while (fieldIterator.hasNext()) {
			this.allFields[fieldIndex++] = fieldIterator.next();
		}

		this.allMethods = new MethodNode[this.processableMethods.size() + this.excludedMethods.size()];
		int methodIndex = 0;
		Iterator<MethodNode> processableMethodIterator = this.processableMethods.keySet().iterator();

		while (processableMethodIterator.hasNext()) {
			this.allMethods[methodIndex++] = processableMethodIterator.next();
		}

		Iterator<MethodNode> excludedMethodIterator = this.excludedMethods.keySet().iterator();

		while (excludedMethodIterator.hasNext()) {
			this.allMethods[methodIndex++] = excludedMethodIterator.next();
		}
	}

	public Enumeration<AbstractClassNode> getAnnotationCandidates() {
		return Collections.enumeration(this.annotationCandidates);
	}

	public boolean includeAnnotationType(AbstractClassNode classNode, String reason) {
		if (this.excludedAnnotationTypes == null) {
			this.excludedAnnotationTypes = GlobalUtils.newHashSet();
		}

		boolean newlyIncluded = this.excludedAnnotationTypes.remove(classNode);
		if (newlyIncluded && super.scriptContext.isVerbose() && reason != null && super.logWriter != null) {
			super.logWriter.println("\tUnmatching for annotation attribute exclusion class \"" + this.formatClassForDisplay(classNode) + "\" because of \"" + reason + "\"");
		}

		return newlyIncluded;
	}

	private void retainClass(ClassNode classNode, RingQueue<MethodNode> methodQueue, boolean retainInstanceMembers) {
		boolean classExcluded = this.excludeClassSilently(classNode);
		boolean newlyRetained = false;
		if (retainInstanceMembers) {
			newlyRetained = this.instanceRetainedClasses.add(classNode);
		}

		if (classExcluded || newlyRetained) {
			this.scanClassReferences(classNode, methodQueue);
		}

		if (classExcluded) {
			this.processPendingFields(methodQueue, this.pendingStaticFields, classNode);
			this.processPendingMethods(methodQueue, this.pendingStaticMethods, classNode);
		}

		if (newlyRetained) {
			this.processPendingFields(methodQueue, this.pendingInstanceFields, classNode);
			this.processPendingMethods(methodQueue, this.pendingInstanceMethods, classNode);
		}
	}

	@Override
	public boolean isMethodExcluded(MethodNode methodNode) {
		return methodNode.isStatic()
			? this.excludedMethods.containsKey(methodNode) && super.excludedClasses.containsKey(methodNode.getMethodOwner())
			: this.excludedMethods.containsKey(methodNode) && this.instanceRetainedClasses.contains(methodNode.getMethodOwner());
	}

	private void retainLibraryOverrides() {
		MethodNode[] libraryOverridingMethods = this.commonSuperClassAnalyzer.getLibraryOverridingMethods();

		for (int i = 0; i < libraryOverridingMethods.length; i++) {
			MethodNode overridingMethod = libraryOverridingMethods[i];
			this.excludeMethodSilently(overridingMethod);
			ClassNode methodOwner = overridingMethod.getMethodOwner();
			if (overridingMethod.isStatic()) {
				if (!super.excludedClasses.containsKey(methodOwner)) {
					this.pendingStaticMethods.put(methodOwner, overridingMethod, overridingMethod);
				}
			} else if (!this.instanceRetainedClasses.contains(methodOwner)) {
				this.pendingInstanceMethods.put(methodOwner, overridingMethod, overridingMethod);
			}
		}
	}

	@Override
	public final Enumeration<FieldNode> getAllFields() {
		return new ArrayEnumeration<>(this.allFields);
	}

	private void excludeMethodAndQueue(MethodNode methodNode, RingQueue<MethodNode> methodQueue) {
		this.excludeMethod(methodNode, null);
		methodQueue.push(methodNode);
	}

	private void scanClassAndInnerClasses(ClassNode classNode, RingQueue<ClassNode> classQueue, RingQueue<MethodNode> methodQueue, boolean retainInstanceMembers) {
		if (classNode.hasInnerClasses()) {
			this.markClassRetained(classNode, classQueue, methodQueue, retainInstanceMembers);

			for (AbstractClassNode innerClass : classNode.getInnerClasses()) {
				this.markClassRetained((ClassNode)innerClass, classQueue, methodQueue, retainInstanceMembers);
			}
		} else if (classNode.isInnerClass()) {
			ClassNode outerClass = (ClassNode)classNode.getOuterClass();
			this.markClassRetained(outerClass, classQueue, methodQueue, retainInstanceMembers);

			for (AbstractClassNode outerInnerClass : outerClass.getInnerClasses()) {
				this.markClassRetained((ClassNode)outerInnerClass, classQueue, methodQueue, retainInstanceMembers);
			}
		} else {
			this.markClassRetained(classNode, classQueue, methodQueue, retainInstanceMembers);
		}
	}

	private void registerProcessableClass(ClassNode classNode, List<ClassNode> classList) {
		super.processableClasses.put(classNode, classNode);
		classList.add(classNode);
		ArrayEnumeration<FieldNode> fieldEnumeration = classNode.getFieldEnumeration();

		while (fieldEnumeration.hasMoreElements()) {
			FieldNode fieldNode = fieldEnumeration.nextElement();
			super.processableFields.put(fieldNode, fieldNode.getOwnerClass());
		}

		ArrayEnumeration<MethodNode> methodEnumeration = classNode.getMethodEnumeration();

		while (methodEnumeration.hasMoreElements()) {
			MethodNode methodNode = methodEnumeration.nextElement();
			if (!methodNode.isClinit()) {
				this.processableMethods.put(methodNode, methodNode.getMethodOwner());
			} else {
				this.excludedMethods.put(methodNode, methodNode.getMethodOwner());
			}
		}
	}

	private void markClassRetained(ClassNode classNode, RingQueue<ClassNode> classQueue, RingQueue<MethodNode> methodQueue, boolean retainInstanceMembers) {
		boolean classExcluded = this.excludeClassSilently(classNode);
		boolean newlyRetained = false;
		if (retainInstanceMembers) {
			newlyRetained = this.instanceRetainedClasses.add(classNode);
		}

		if (classExcluded) {
			this.processPendingFields(methodQueue, this.pendingStaticFields, classNode);
			this.processPendingMethods(methodQueue, this.pendingStaticMethods, classNode);
		}

		if (newlyRetained) {
			this.processPendingFields(methodQueue, this.pendingInstanceFields, classNode);
			this.processPendingMethods(methodQueue, this.pendingInstanceMethods, classNode);
		}

		if (classExcluded || newlyRetained) {
			classQueue.push(classNode);
		}
	}

	private void retainOverriddenMethods() {
		for (MethodNode excludedMethod : GlobalUtils.newHashSet(this.excludedMethods.keySet())) {
			for (MethodNode overriddenMethod : this.commonSuperClassAnalyzer.getOverriddenMethods(excludedMethod)) {
				if (this.processableMethods.containsKey(overriddenMethod)) {
					this.excludeMethodSilently(overriddenMethod);
				}
			}
		}
	}

	public Enumeration<FieldNode> getAllTrimmedFields() {
		return this.trimmedFieldsByClass.getAllValues();
	}

	private void retainReferencedField(FieldNode fieldNode, RingQueue<ClassNode> classQueue) {
		this.excludeField(fieldNode, (String)null);
		ClassNode fieldTypeClass = fieldNode.getFieldTypeClass();
		if (fieldTypeClass != null) {
			this.excludeClassSilently(fieldTypeClass);
			classQueue.push(fieldTypeClass);
		}

		ReferenceSetContainer referenceSet = fieldNode.getAllReferences();
		Enumeration<?> referenceEnumeration = referenceSet.getClassReferences();

		while (referenceEnumeration.hasMoreElements()) {
			ClassNode referencedClass = (ClassNode)referenceEnumeration.nextElement();
			this.instanceRetainedClasses.add(referencedClass);
			this.excludeClassSilently(referencedClass);
			classQueue.push(referencedClass);
		}

		referenceEnumeration = referenceSet.getSignatureClassReferences();

		while (referenceEnumeration.hasMoreElements()) {
			ClassNode signatureClass = (ClassNode)referenceEnumeration.nextElement();
			this.excludeClassSilently(signatureClass);
			classQueue.push(signatureClass);
		}

		referenceEnumeration = referenceSet.getFieldReferences();

		while (referenceEnumeration.hasMoreElements()) {
			FieldNode referencedField = (FieldNode)referenceEnumeration.nextElement();
			this.retainReferencedField(referencedField, classQueue);
		}

		referenceEnumeration = referenceSet.getMethodReferences();

		while (referenceEnumeration.hasMoreElements()) {
			MethodNode referencedMethod = (MethodNode)referenceEnumeration.nextElement();
			this.excludeMethodSilently(referencedMethod);
		}
	}

	private void classifyTrimmedMembers() {
		this.trimmedFields = new ArrayList<>(super.processableFields.size() * 2);
		this.trimmedMethods = new ArrayList<>(this.processableMethods.size() * 2);
		this.retainedFields = new ArrayList<>(super.excludedFields.size());
		this.retainedMethods = new ArrayList<>(this.excludedMethods.size());

		for (int fieldIndex = 0; fieldIndex < this.allFields.length; fieldIndex++) {
			FieldNode fieldNode = this.allFields[fieldIndex];
			if (this.isFieldExcluded(this.allFields[fieldIndex])) {
				this.retainedFields.add(fieldNode);
			} else {
				this.trimmedFieldsByClass.put(fieldNode.getOwnerClass(), fieldNode, fieldNode);
				this.trimmedFields.add(fieldNode);
			}
		}

		for (int methodIndex = 0; methodIndex < this.allMethods.length; methodIndex++) {
			MethodNode methodNode = this.allMethods[methodIndex];
			if (this.isMethodExcluded(methodNode)) {
				this.retainedMethods.add(methodNode);
			} else {
				this.trimmedMethodsByClass.put(methodNode.getMethodOwner(), methodNode, methodNode);
				this.trimmedMethods.add(methodNode);
			}
		}
	}

	public ArrayList<ClassNode> getTrimmedMethodOwners() {
		ArrayList<ClassNode> trimmedOwners = new ArrayList<>();
		Enumeration<ClassNode> ownerEnumeration = this.trimmedMethodsByClass.getGroupKeys();

		while (ownerEnumeration.hasMoreElements()) {
			ClassNode ownerClass = ownerEnumeration.nextElement();
			if (super.excludedClasses.containsKey(ownerClass)) {
				trimmedOwners.add(ownerClass);
			}
		}

		return trimmedOwners;
	}

	public final void unexcludeMethod(MethodNode methodNode, String reason) {
		if (!methodNode.isClinit()) {
			ClassNode ownerClass = this.excludedMethods.remove(methodNode);
			if (ownerClass != null) {
				this.processableMethods.put(methodNode, ownerClass);
				if (super.scriptContext.isVerbose() && reason != null && super.logWriter != null) {
					super.logWriter
						.println(
							"\tUnmatching method \""
								+ ExcludeStatementManager.formatMethodForDisplay(methodNode, this)
								+ "\" in class \""
								+ this.formatClassForDisplay(methodNode.getMethodOwner())
								+ "\" for trim because of \""
								+ reason
								+ "\""
						);
				}
			}
		}
	}

	public void printTrimReport(PrintWriter printWriter, ClassHierarchy classHierarchy, boolean preview) {
		ArrayList<ClassNode> classes = new ArrayList<>(super.processableClasses.size());

		for (ClassNode processableClass : super.processableClasses.keySet()) {
			classes.add(processableClass);
		}

		Collections.sort(classes);
		String trimVerb = preview ? "Would trim" : "Trimmed";
		int classCount = classes.size();

		for (int classIndex = 0; classIndex < classCount; classIndex++) {
			ClassNode classNode = classes.get(classIndex);
			String classVersionSuffix = classNode.hasInnerClassOrdinal() ? " (multirelease version " + classNode.getInnerClassOrdinal() + ")" : "";
			if (!preview && super.scriptContext.isVerbose()) {
				super.logWriter.println("\tTrimmed class \"" + ExcludeStatementManager.getFormattedClassSignature(classNode, classHierarchy, false) + "\"" + classVersionSuffix);
			}

			if (printWriter != null) {
				printWriter.println(trimVerb + " class \"" + ExcludeStatementManager.getFormattedClassSignature(classNode, classHierarchy, false) + "\"" + classVersionSuffix);
			}
		}

		TrimExcludes.MemberDisplayNameComparator memberDisplayNameComparator = new TrimExcludes.MemberDisplayNameComparator(this);
		boolean staticFieldTrimmed = false;
		ArrayList<FieldNode> fields = new ArrayList<>(super.processableFields.size());
		Enumeration<FieldNode> trimmedFieldEnumeration = this.getAllTrimmedFields();

		while (trimmedFieldEnumeration.hasMoreElements()) {
			FieldNode fieldNode = trimmedFieldEnumeration.nextElement();
			ClassNode fieldOwner = fieldNode.getOwnerClass();
			if (this.isClassExcluded(fieldOwner)) {
				if (fieldNode.isStatic()) {
					staticFieldTrimmed = true;
				}

				fields.add(fieldNode);
			}
		}

		Collections.sort(fields, memberDisplayNameComparator);
		if (staticFieldTrimmed) {
			if (!preview && super.scriptContext.isVerbose()) {
				super.logWriter
					.println(
						"\tNB: Some \"static\" fields have been trimmed. Compilers can optimize accesses to the values of \"static\" fields such that the fields are never directly accessed."
					);
			}

			if (printWriter != null) {
				printWriter.println(
					"NB: Some \"static\" fields "
						+ (preview ? "would be" : "have been")
						+ " trimmed. Compilers can optimize accesses to the values of \"static\" fields such that the fields are never directly accessed."
				);
			}
		}

		int fieldCount = fields.size();

		for (int fieldIndex = 0; fieldIndex < fieldCount; fieldIndex++) {
			FieldNode trimmedField = fields.get(fieldIndex);
			ClassNode trimmedFieldOwner = trimmedField.getOwnerClass();
			String fieldVersionSuffix = trimmedFieldOwner.hasInnerClassOrdinal() ? " (multirelease version " + trimmedFieldOwner.getInnerClassOrdinal() + ")" : "";
			if (!preview && super.scriptContext.isVerbose()) {
				super.logWriter
					.println(
						"\tTrimmed field \""
							+ ExcludeStatementManager.formatFieldForDisplay(trimmedField, this)
							+ "\" in class \""
							+ ExcludeStatementManager.getFormattedClassSignature(trimmedFieldOwner, classHierarchy, false)
							+ "\""
							+ fieldVersionSuffix
					);
			}

			if (printWriter != null) {
				printWriter.println(
					trimVerb
						+ " field \""
						+ ExcludeStatementManager.formatFieldForDisplay(trimmedField, this)
						+ "\" in class \""
						+ ExcludeStatementManager.getFormattedClassSignature(trimmedFieldOwner, classHierarchy, false)
						+ "\""
						+ fieldVersionSuffix
				);
			}
		}

		ArrayList<MethodNode> methods = new ArrayList<>(this.processableMethods.size());
		Enumeration<MethodNode> trimmedMethodEnumeration = this.getAllTrimmedMethods();

		while (trimmedMethodEnumeration.hasMoreElements()) {
			MethodNode methodNode = trimmedMethodEnumeration.nextElement();
			ClassNode methodOwner = methodNode.getMethodOwner();
			if (this.isClassExcluded(methodOwner)) {
				methods.add(methodNode);
			}
		}

		Collections.sort(methods, memberDisplayNameComparator);
		int methodCount = methods.size();

		for (int methodIndex = 0; methodIndex < methodCount; methodIndex++) {
			MethodNode trimmedMethod = methods.get(methodIndex);
			ClassNode trimmedMethodOwner = trimmedMethod.getMethodOwner();
			String methodVersionSuffix = trimmedMethodOwner.hasInnerClassOrdinal() ? " (multirelease version " + trimmedMethodOwner.getInnerClassOrdinal() + ")" : "";
			if (!preview && super.scriptContext.isVerbose()) {
				super.logWriter
					.println(
						"\tTrimmed method \""
							+ ExcludeStatementManager.formatMethodForDisplay(trimmedMethod, this)
							+ "\" in class \""
							+ ExcludeStatementManager.getFormattedClassSignature(trimmedMethodOwner, classHierarchy, false)
							+ "\""
							+ methodVersionSuffix
					);
			}

			if (printWriter != null) {
				printWriter.println(
					trimVerb
						+ " method \""
						+ ExcludeStatementManager.formatMethodForDisplay(trimmedMethod, this)
						+ "\" in class \""
						+ ExcludeStatementManager.getFormattedClassSignature(trimmedMethodOwner, classHierarchy, false)
						+ "\""
						+ methodVersionSuffix
				);
			}
		}
	}

	public static class MemberDisplayNameComparator implements Comparator<MemberNode> {
		public final TrimExcludes trimExcludes;

		public MemberDisplayNameComparator(TrimExcludes trimExcludes) {
			this.trimExcludes = trimExcludes;
		}

		@Override
		public int compare(MemberNode o1, MemberNode o2) {
			return this.compareByDisplayName(o1, o2);
		}

		public int compareByDisplayName(MemberNode member1, MemberNode member2) {
			String displayName1 = member1.getNameDisplay();
			String displayName2 = member2.getNameDisplay();
			return displayName1.compareTo(displayName2);
		}
	}

	public static class ClassEntryComparator implements Comparator<Entry<ClassNode, String>> {
		public final TrimExcludes trimExcludes;

		public ClassEntryComparator(TrimExcludes trimExcludes) {
			this.trimExcludes = trimExcludes;
		}

		@Override
		public int compare(Entry<ClassNode, String> o1, Entry<ClassNode, String> o2) {
			int valueComparison = o1.getValue().compareTo(o2.getValue());
			return valueComparison == 0 ? o1.getKey().compareTo(o2.getKey()) : valueComparison;
		}
	}

	public static class ClassNodeFirstComparator implements Comparator<StructureNode> {
		public final TrimExcludes trimExcludes;

		public ClassNodeFirstComparator(TrimExcludes trimExcludes) {
			this.trimExcludes = trimExcludes;
		}

		public int compare(StructureNode o1, StructureNode o2) {
			return this.compareClassNodesFirst(o1, o2);
		}

		public int compareClassNodesFirst(StructureNode node1, StructureNode node2) {
			if (node1 instanceof ClassNode && !(node2 instanceof ClassNode)) {
				return -1;
			} else {
				return node1 instanceof ClassNode && node2 instanceof ClassNode || !(node1 instanceof ClassNode) && !(node2 instanceof ClassNode) ? 0 : 1;
			}
		}
	}

	public static class MemberEntryComparator implements Comparator<Entry<? extends MemberNode, String>> {
		public final TrimExcludes trimExcludes;

		public MemberEntryComparator(TrimExcludes trimExcludes) {
			this.trimExcludes = trimExcludes;
		}

		public int compare(Entry<? extends MemberNode, String> o1, Entry<? extends MemberNode, String> o2) {
			int valueComparison = o1.getValue().compareTo(o2.getValue());
			if (valueComparison == 0) {
				MemberNode member1 = o1.getKey();
				MemberNode member2 = o2.getKey();
				int classComparison = member1.getClassNode().compareTo(member2.getClassNode());
				return classComparison == 0 ? member1.compareToByName(member2) : classComparison;
			} else {
				return valueComparison;
			}
		}
	}
}
