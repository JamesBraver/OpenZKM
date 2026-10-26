package com.zelix.tools.excludes;

import com.zelix.exceptions.LexicalException;
import com.zelix.exceptions.ParsingException;
import com.zelix.framework.AbstractClassNode;
import com.zelix.framework.AbstractFieldNode;
import com.zelix.framework.AbstractMethodNode;
import com.zelix.framework.MemberNode;
import com.zelix.framework.reference.ReferenceGraph;
import com.zelix.framework.reference.ReferenceGraphMap;
import com.zelix.framework.signature.MethodSignature;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.wrappers.FieldNode;
import com.zelix.framework.wrappers.MethodNode;
import com.zelix.klassmaster.changelogs.parser.context.ChangeLogContext;
import com.zelix.klassmaster.config.Options;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.ScriptParser;
import com.zelix.klassmaster.script.base.AbstractScriptNode;
import com.zelix.klassmaster.script.statements.base.ExclusionInclusionStatement;
import com.zelix.klassmaster.script.statements.exclusion.DefaultExcludeInput;
import com.zelix.klassmaster.script.statements.exclusion.UnexcludeStatement;
import com.zelix.klassmaster.script.statements.specifier.RenameFilterParameter;
import com.zelix.repository.Hierarchy;
import com.zelix.utils.FileUtils;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.StringUtils;
import com.zelix.utils.UnionFind;
import com.zelix.utils.collections.DualKeyMap;
import com.zelix.utils.collections.IdentityHashSet;
import com.zelix.utils.collections.KeyValuePair;
import com.zelix.utils.enumerations.ArrayEnumeration;
import com.zelix.utils.references.AtomicReference;
import com.zelix.workspace.ClassHierarchyNode;
import com.zelix.workspace.PackageManager;
import com.zelix.workspace.Workspace;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;

public class DefaultExcludes extends ModuleExcludesManager {
	public static final String DEFAULT_CLASSLIB_EXCLUDES = "exclude  public *.^* and"
		+ Options.lineSeparator
		+ "         *.* public * and"
		+ Options.lineSeparator
		+ "         *.* public *(*);"
		+ Options.lineSeparator;
	public static final String DEFAULT_FRAMEWORK_EXCLUDES = "exclude  public *.^* and"
		+ Options.lineSeparator
		+ "         *.* public * and"
		+ Options.lineSeparator
		+ "         *.* protected * and"
		+ Options.lineSeparator
		+ "         *.* public *(*) and"
		+ Options.lineSeparator
		+ "         *.* protected *(*);"
		+ Options.lineSeparator;
	public static final String DEFAULT_MIDLET_EXCLUDES = "exclude  *.* extends javax.microedition.midlet.MIDlet;" + Options.lineSeparator;
	public static final String DEFAULT_FALLBACK_EXCLUDES = "exclude *.<link>_Skel implements java.rmi.server.Skeleton and //RMI "
		+ Options.lineSeparator
		+ "      *.<link>_Stub extends java.rmi.server.RemoteStub and //RMI "
		+ Options.lineSeparator
		+ "      *._<link>_Stub extends javax.rmi.CORBA.Stub and //RMI with iiop "
		+ Options.lineSeparator
		+ "      *._<link>_Tie implements javax.rmi.CORBA.Tie and //RMI with iiop "
		+ Options.lineSeparator
		+ "      interface *.* implements java.rmi.Remote *(*) throws java.rmi.RemoteException +signatureClasses and //RMI "
		+ Options.lineSeparator
		+ "      *.<link>BeanInfo and   //JavaBeans "
		+ Options.lineSeparator
		+ "      *.<link>Customizer and //JavaBeans "
		+ Options.lineSeparator
		+ "      *.* private readObject(java.io.ObjectInputStream) and   //Serialization "
		+ Options.lineSeparator
		+ "      *.* private writeObject(java.io.ObjectOutputStream) and //Serialization "
		+ Options.lineSeparator
		+ "      *.* static final long serialVersionUID and              //Serialization "
		+ Options.lineSeparator
		+ "      *.* writeReplace() and                                  //Serialization "
		+ Options.lineSeparator
		+ "      *.* readResolve() and                                   //Serialization "
		+ Options.lineSeparator
		+ "      *.* readObjectNoData() throws java.io.ObjectStreamException and //Serialization "
		+ Options.lineSeparator
		+ "      *.* java.io.ObjectStreamField[] serialPersistentFields and  //Serialization "
		+ Options.lineSeparator
		+ "      *.* extends java.lang.Enum public * and     //enum "
		+ Options.lineSeparator
		+ "      *.* extends java.lang.Enum public values() and  //enum "
		+ Options.lineSeparator
		+ "      *.* extends java.lang.Enum public valueOf(java.lang.String) and  //enum "
		+ Options.lineSeparator
		+ "      *.* implements javax.ejb.EJBObject public *(*) and               //EJB Remote "
		+ Options.lineSeparator
		+ "      *.* implements javax.ejb.EJBLocalObject public *(*) and          //EJB Local "
		+ Options.lineSeparator
		+ "      *.* implements javax.ejb.EJBHome public create(*) and            //EJB Home "
		+ Options.lineSeparator
		+ "      *.* implements javax.ejb.EJBHome public find*(*) and             //EJB Home "
		+ Options.lineSeparator
		+ "      *.* implements javax.ejb.EJBLocalHome public create(*) and       //EJB Local Home "
		+ Options.lineSeparator
		+ "      *.* implements javax.ejb.EJBLocalHome public find*(*) and        //EJB Local Home "
		+ Options.lineSeparator
		+ "      *.* implements javax.ejb.EnterpriseBean public *(*) and          //EJB Bean "
		+ Options.lineSeparator
		+ "      *.*Key implements java.io.Serializable public * and              //EJB Primary Key "
		+ Options.lineSeparator
		+ "      @javax.ejb.Remote *.^*^ public *(*) and                       "
		+ Options.lineSeparator
		+ "      @javax.ejb.RemoteHome *.^*^ public *(*) and                   "
		+ Options.lineSeparator
		+ "      @javax.ejb.Stateless *.^*^ public *(*) and                    "
		+ Options.lineSeparator
		+ "      @javax.ejb.Stateful *.^*^ public *(*) and                     "
		+ Options.lineSeparator
		+ "      @javax.ejb.MessageDriven *.*^ public *(*) and                 "
		+ Options.lineSeparator
		+ "      @javax.persistence.Entity *.^*^ public *(*) and               "
		+ Options.lineSeparator
		+ "      @javax.persistence.Table *.^*^ public *(*) and                "
		+ Options.lineSeparator
		+ "      @javax.persistence.Entity *.* @javax.persistence.Column * and "
		+ Options.lineSeparator
		+ "      *.* @javax.persistence.Id * and                               "
		+ Options.lineSeparator
		+ "      *.* @javax.persistence.Id *(*) and                            "
		+ Options.lineSeparator
		+ "      @javax.persistence.IdClass(value=*.^*^) public *(*) and       "
		+ Options.lineSeparator
		+ "      @javax.jws.WebService *.*^ @javax.jws.WebMethod *(*) and      "
		+ Options.lineSeparator
		+ "      *.* implements java.lang.annotation.Annotation public value() and "
		+ Options.lineSeparator
		+ "      *.* static createUI(javax.swing.JComponent) and          //GUI "
		+ Options.lineSeparator
		+ "      *.^*^ native *(*) +signatureClasses and //Don't rename native methods or their packages or classes "
		+ Options.lineSeparator
		+ "      @javax.xml.bind.annotation.XmlType *.* * and    //JAXB"
		+ Options.lineSeparator
		+ "      @javax.xml.bind.annotation.XmlType *.* public get<link>() and"
		+ Options.lineSeparator
		+ "      @javax.xml.bind.annotation.XmlType *.* public is<link>() and"
		+ Options.lineSeparator
		+ "      @javax.xml.bind.annotation.XmlType *.* public set<link>(*) and"
		+ Options.lineSeparator
		+ "      *.* @javax.xml.bind.annotation.XmlTransient public get<link>() and"
		+ Options.lineSeparator
		+ "      *.* @javax.xml.bind.annotation.XmlTransient public is<link>() and"
		+ Options.lineSeparator
		+ "      *.* @javax.xml.bind.annotation.XmlTransient public set<link>(*) and"
		+ Options.lineSeparator
		+ "      @javax.xml.bind.annotation.XmlRootElement *.* private * and //JAXB"
		+ Options.lineSeparator
		+ "      @javax.xml.bind.annotation.XmlRootElement *.* public get<link>() and"
		+ Options.lineSeparator
		+ "      @javax.xml.bind.annotation.XmlRootElement *.* public is<link>() and"
		+ Options.lineSeparator
		+ "      @javax.xml.bind.annotation.XmlRootElement *.* public set<link>(*) and"
		+ Options.lineSeparator
		+ "      *.<link>MBean and      //JMX"
		+ Options.lineSeparator
		+ "      *.<link>MXBean and      //JMX"
		+ Options.lineSeparator
		+ "      *.* containing{@java.beans.ConstructorProperties *(*)} public get<link>() and //JMX"
		+ Options.lineSeparator
		+ "      *.* @javafx.fxml.FXML initialize() and //FXML"
		+ Options.lineSeparator
		+ "      com.javafx.main.Main and //FXML"
		+ Options.lineSeparator
		+ "      *.* $deserializeLambda$(java.lang.invoke.SerializedLambda) and //Serialized lambda"
		+ Options.lineSeparator
		+ "      *.* lambda$*(*) and //Serialized lambda"
		+ Options.lineSeparator
		+ "      *.* extends java.lang.Record <link>() //Records"
		+ Options.lineSeparator
		+ "      ;"
		+ Options.lineSeparator;
	private final Map<ClassNode, ExcludeElement> excludeElements = GlobalUtils.newHashMap();
	private final Map<AbstractMethodNode, String> methodPrefixes = GlobalUtils.newHashMap();
	private final ReferenceGraph<Object, AbstractMethodNode> propertyReferenceGraph = new ReferenceGraph<>();
	private final DualKeyMap<MethodNode, FieldNode, String> methodFieldPrefixes = new DualKeyMap<>();
	private ReferenceGraph<FieldNode, AbstractMethodNode> fieldReferenceGraph = new ReferenceGraph<>();
	private final DualKeyMap<MethodNode, String, Object> retainedPrefixMethods = new DualKeyMap<>();
	private final UnionFind<AbstractMethodNode> methodUnionFind = new UnionFind<>();
	private final ReferenceGraphMap<IdentityHashSet<AbstractMethodNode>, String, AbstractMethodNode> methodsByGroupAndPrefix = new ReferenceGraphMap<>();
	private final ReferenceGraph<IdentityHashSet<AbstractMethodNode>, FieldNode> fieldsByMethodGroup = new ReferenceGraph<>();
	private final Map<String, String> processablePackages = GlobalUtils.newHashMap();
	private final Map<String, String> excludedPackages = GlobalUtils.newHashMap();
	private AbstractClassNode[] changeLogClasses;
	private FieldNode[] allFields;
	private ClassNode[] workspaceClasses;
	private String[] allPackageNames;
	private MethodNode[] allMethods;

	public DefaultExcludes(
		Workspace workspace, List<? extends ExclusionInclusionStatement> excludeStatements, List<UnexcludeStatement> unexcludeStatements, AbstractClassNode[] changeLogClasses, ScriptContext scriptContext
	) throws IOException {
		super(workspace, excludeStatements, unexcludeStatements, scriptContext);
		this.changeLogClasses = changeLogClasses;
		if (!workspace.isEmpty()) {
			this.initializeProcessableModules();
			this.initializePackageNames(workspace.getPackageManager());
			this.initializeProcessableMaps(workspace.getClasses(), workspace.getWorkspaceClassesSize());
			this.initializeExclusions();
		}
	}

	public static ExclusionInclusionStatement getFrameworkExclusions(ScriptContext scriptContext) throws IOException {
		return parseExclusionText(DEFAULT_FRAMEWORK_EXCLUDES, scriptContext);
	}

	public static ExclusionInclusionStatement loadDefaultExclusions(ScriptContext scriptContext) throws IOException {
		String defaultExcludePath = scriptContext.getDefaultExcludePath();
		boolean fallbackUsed = false;
		BufferedReader defaultExcludeReader;
		try {
			defaultExcludeReader = FileUtils.createBufferedReader(new File(defaultExcludePath), Options.ZKM_SCRIPT_ENCODING);
			scriptContext.logRaw("Using '" + defaultExcludePath + "' for default exclusions", true);
		} catch (FileNotFoundException ignored) {
			defaultExcludeReader = new BufferedReader(new StringReader(DEFAULT_FALLBACK_EXCLUDES));
			fallbackUsed = true;
			scriptContext.logRaw("Using internal default exclusions. File '" + defaultExcludePath + "' not found", true);
		} catch (IOException ioException) {
			defaultExcludeReader = new BufferedReader(new StringReader(DEFAULT_FALLBACK_EXCLUDES));
			fallbackUsed = true;
			scriptContext.logRaw("Using internal default exclusions. Error opening file '" + defaultExcludePath + "' : " + ioException, true);
		}

		try {
			return parseExclusionStatement(scriptContext, defaultExcludeReader);
		} catch (ParsingException ignored) {
			if (!fallbackUsed) {
				return fallbackToDefaultExclusions(scriptContext, ignored);
			}
		} catch (LexicalException ignored2) {
			if (!fallbackUsed) {
				return fallbackToDefaultExclusions(scriptContext, ignored2);
			}
		}

		return null;
	}

	public static ExclusionInclusionStatement getClasslibExclusions(ScriptContext scriptContext) throws IOException {
		return parseExclusionText(DEFAULT_CLASSLIB_EXCLUDES, scriptContext);
	}

	private static ExclusionInclusionStatement parseExclusionText(String exclusionText, ScriptContext scriptContext) throws IOException {
		System.out.println(exclusionText);
		BufferedReader exclusionReader = new BufferedReader(new StringReader(exclusionText));

		try {
			return parseExclusionStatement(scriptContext, exclusionReader);
		} catch (ParsingException ignored) {
		} catch (LexicalException ignored2) {
		}

		return null;
	}

	public static ExclusionInclusionStatement getMidletExclusions(ScriptContext scriptContext) throws IOException {
		return parseExclusionText(DEFAULT_MIDLET_EXCLUDES, scriptContext);
	}

	private static ExclusionInclusionStatement parseExclusionStatement(ScriptContext scriptContext, BufferedReader reader) throws IOException {
		ScriptParser scriptParser = new ScriptParser(reader);

		AbstractScriptNode scriptNode;
		try {
			scriptNode = scriptParser.parseDefaultExcludeInput();
			scriptNode.parseArguments(null, scriptContext);
		} finally {
			try {
				reader.close();
			} catch (IOException ignored) {
			}
		}

		return ((DefaultExcludeInput)scriptNode).getParsedStatement();
	}

	private static ExclusionInclusionStatement fallbackToDefaultExclusions(ScriptContext scriptContext, Throwable parseError) throws IOException {
		if (parseError != null) {
			String defaultExcludePath = scriptContext.getDefaultExcludePath();
			System.err.println("\"" + defaultExcludePath + "\" had a parse error and will be ignored. See \"" + scriptContext.getLogFilePath() + "\" for more detail.");
			scriptContext.logWarning(
				"\""
					+ defaultExcludePath
					+ "\" had a parse error :"
					+ Options.lineSeparator
					+ parseError.getMessage()
					+ Options.lineSeparator
					+ "Will use the internal default exclude statement and ignore \""
					+ defaultExcludePath
					+ "\"."
			);
		}

		BufferedReader fallbackExcludesReader = new BufferedReader(new StringReader(DEFAULT_FALLBACK_EXCLUDES));

		try {
			return parseExclusionStatement(scriptContext, fallbackExcludesReader);
		} catch (ParsingException ignored) {
			return null;
		} catch (LexicalException ignored2) {
			return null;
		}
	}

	private String formatMembersForDisplay(Set<? extends MemberNode> memberNodes, HashMap<String, String> newNameMappings) {
		StringBuilder builder = new StringBuilder();

		for (MemberNode memberNode : memberNodes) {
			if (memberNode.isWorkspaceNode()) {
				if (builder.length() > 0) {
					builder.append(", ");
				}

				builder.append("\"");
				builder.append(GlobalUtils.getMappedOrDefault(memberNode.getInternalName(), newNameMappings).replace('/', '.'));
				builder.append(' ');
				builder.append(memberNode.getDisplayName());
				builder.append("\"");
			}
		}

		return builder.toString();
	}

	public final boolean hasNoProcessablePackages() {
		return this.processablePackages.size() == 0;
	}

	private final void initializeExclusions() throws IOException {
		HashMap<RenameFilterParameter, RenameFilterParameter> seenExclusionParams = GlobalUtils.newHashMap();
		ArrayList<RenameFilterParameter> distinctExclusionParams = new ArrayList<>();

		for (int exclusionStatementIndex = 0; exclusionStatementIndex < super.exclusionStatements.size(); exclusionStatementIndex++) {
			Enumeration<RenameFilterParameter> exclusionParams = super.exclusionStatements.get(exclusionStatementIndex).getRenameFilterParameters();

			while (exclusionParams.hasMoreElements()) {
				RenameFilterParameter exclusionParam = exclusionParams.nextElement();
				if (!seenExclusionParams.containsKey(exclusionParam)) {
					seenExclusionParams.put(exclusionParam, exclusionParam);
					distinctExclusionParams.add(exclusionParam);
				}
			}
		}

		Collections.sort(distinctExclusionParams);
		if (super.scriptContext.isVerbose() && distinctExclusionParams.size() > 0) {
			super.logWriter.println("\tExclusion parameters:");

			for (int exclusionPrintIndex = distinctExclusionParams.size() - 1; exclusionPrintIndex >= 0; exclusionPrintIndex--) {
				super.logWriter.println("\t\t" + distinctExclusionParams.get(exclusionPrintIndex));
			}
		}

		for (int exclusionParamIndex = distinctExclusionParams.size() - 1; exclusionParamIndex >= 0; exclusionParamIndex--) {
			distinctExclusionParams.get(exclusionParamIndex).applyRenameExclusion(this);
		}

		if (super.inclusionStatements != null) {
			HashMap<RenameFilterParameter, RenameFilterParameter> seenUnexclusionParams = GlobalUtils.newHashMap();
			Vector<RenameFilterParameter> distinctUnexclusionParams = new Vector<>();

			for (int inclusionStatementIndex = 0; inclusionStatementIndex < super.inclusionStatements.size(); inclusionStatementIndex++) {
				Enumeration<RenameFilterParameter> unexclusionParams = super.inclusionStatements.get(inclusionStatementIndex).getRenameFilterParameters();

				while (unexclusionParams.hasMoreElements()) {
					RenameFilterParameter unexclusionParam = unexclusionParams.nextElement();
					if (!seenUnexclusionParams.containsKey(unexclusionParam)) {
						seenUnexclusionParams.put(unexclusionParam, unexclusionParam);
						distinctUnexclusionParams.addElement(unexclusionParam);
					}
				}
			}

			if (distinctUnexclusionParams.size() > 0 && distinctExclusionParams.size() == 0) {
				super.scriptContext
					.logWarningInternal(
						"An 'unexclude' statement can only operate on the results of an 'exclude' statement. No 'exclude' statement is in effect so the 'unexclude' statement will be ignored.",
						true
					);
			} else {
				Collections.sort(distinctUnexclusionParams);
				if (super.scriptContext.isVerbose() && distinctUnexclusionParams.size() > 0) {
					super.logWriter.println("\tUnexclusion parameters:");

					for (int unexclusionPrintIndex = distinctUnexclusionParams.size() - 1; unexclusionPrintIndex >= 0; unexclusionPrintIndex--) {
						super.logWriter.println("\t\t\"" + distinctUnexclusionParams.elementAt(unexclusionPrintIndex) + "\"");
					}
				}

				for (int unexclusionParamIndex = distinctUnexclusionParams.size() - 1; unexclusionParamIndex >= 0; unexclusionParamIndex--) {
					RenameFilterParameter dedupedUnexclusionParam = distinctUnexclusionParams.elementAt(unexclusionParamIndex);
					if (dedupedUnexclusionParam.isClassTarget() && dedupedUnexclusionParam.isLinkClass()) {
						super.scriptContext.logWarningInternal("Parameter '" + dedupedUnexclusionParam + "' in 'unexclude' statement cannot use the '<link>' syntax. It will be ignored.", true);
					} else if (dedupedUnexclusionParam.isMethodTarget() && dedupedUnexclusionParam.hasLinkMethodNamePattern()) {
						super.scriptContext.logWarningInternal("Parameter '" + dedupedUnexclusionParam + "' in 'unexclude' statement cannot use the '<link>' syntax. It will be ignored.", true);
					} else {
						dedupedUnexclusionParam.applyRenameInclusion(this);
					}
				}
			}
		}

		ExclusionInclusionStatement defaultExclusionStatement = loadDefaultExclusions(super.scriptContext);
		if (defaultExclusionStatement != null) {
			HashMap<RenameFilterParameter, RenameFilterParameter> seenDefaultParams = GlobalUtils.newHashMap();
			Vector<RenameFilterParameter> distinctDefaultParams = new Vector<>();
			Enumeration<RenameFilterParameter> defaultParams = defaultExclusionStatement.getRenameFilterParameters();

			while (defaultParams.hasMoreElements()) {
				RenameFilterParameter defaultParam = defaultParams.nextElement();
				if (!seenDefaultParams.containsKey(defaultParam)) {
					seenDefaultParams.put(defaultParam, defaultParam);
					distinctDefaultParams.addElement(defaultParam);
				}
			}

			Collections.sort(distinctDefaultParams);
			if (super.scriptContext.isVerbose() && distinctDefaultParams.size() > 0) {
				super.logWriter.println("\tDefault exclusion parameters:");

				for (int defaultPrintIndex = distinctDefaultParams.size() - 1; defaultPrintIndex >= 0; defaultPrintIndex--) {
					super.logWriter.println("\t\t\"" + distinctDefaultParams.elementAt(defaultPrintIndex) + "\"");
				}
			}

			for (int defaultParamIndex = distinctDefaultParams.size() - 1; defaultParamIndex >= 0; defaultParamIndex--) {
				distinctDefaultParams.elementAt(defaultParamIndex).applyRenameExclusion(this);
			}
		}

		if (Options.excludeResourceEntryPoints) {
			DualKeyMap<MethodNode, String, Object> resourceToClassReferences = this.workspace.getResourceToClassReferences();
			if (resourceToClassReferences != null && !resourceToClassReferences.isEmpty()) {
				Map<MethodNode, String> entryPointSignatures = this.workspace.getEntryPointSignatures();

				for (Entry<MethodNode, Map<String, Object>> resourceReferenceEntry : resourceToClassReferences.getEntrySet()) {
					MethodNode methodNode = resourceReferenceEntry.getKey();

					for (Entry<String, Object> prefixEntry : resourceReferenceEntry.getValue().entrySet()) {
						String namePrefix = prefixEntry.getKey();
						String entryPointSignature = entryPointSignatures.get(methodNode);
						this.matchMethodToField(methodNode, namePrefix, prefixEntry.getValue(), entryPointSignature);
					}
				}
			}
		}
	}

	public final void unexcludeFieldWithWarning(FieldNode fieldNode, String reason, HashMap<String, String> newNameMappings) {
		ClassNode ownerClass = super.excludedFields.remove(fieldNode);
		if (ownerClass != null) {
			super.processableFields.put(fieldNode, ownerClass);
			String mappedName;
			if (newNameMappings != null && (mappedName = newNameMappings.get(fieldNode.getInternalName())) != null) {
				GlobalUtils.internalToBinaryName(mappedName);
			} else {
				fieldNode.getBinaryName();
			}

			super.scriptContext
				.logWarning(
					"UNEXCLUDING field \""
						+ ExcludeStatementManager.formatFieldForDisplay(fieldNode, this)
						+ "\" in class \""
						+ ExcludeStatementManager.formatClassForDisplay(fieldNode.getOwnerClass(), this.workspace)
						+ "\" for name obfuscation because \""
						+ reason
						+ "\""
				);
		}
	}

	public final void unexcludeField(FieldNode fieldNode, String reason) {
		ClassNode ownerClass = super.excludedFields.remove(fieldNode);
		if (ownerClass != null) {
			super.processableFields.put(fieldNode, ownerClass);
			if (super.scriptContext.isVerbose()) {
				super.logWriter
					.println(
						"\tUnexcluding field \""
							+ ExcludeStatementManager.formatFieldForDisplay(fieldNode, this)
							+ "\" in class \""
							+ ExcludeStatementManager.formatClassForDisplay(fieldNode.getOwnerClass(), this.workspace)
							+ "\" for name obfuscation because of \""
							+ reason
							+ "\""
					);
			}
		}
	}

	@Override
	public final Enumeration<ClassNode> getWorkspaceClasses() {
		return new ArrayEnumeration<>(this.workspaceClasses);
	}

	public void buildMethodGroups(Hierarchy hierarchy, HashMap<String, String> newNameMappings) {
		Iterator<? extends Entry<?, Set<AbstractMethodNode>>> adjacencyEntryIterator = this.propertyReferenceGraph.getAdjacencyEntries().iterator();

		while (adjacencyEntryIterator.hasNext()) {
			Set<AbstractMethodNode> referencingMethods = adjacencyEntryIterator.next().getValue();

			for (AbstractMethodNode referencingMethod : referencingMethods) {
				for (AbstractMethodNode otherReferencingMethod : referencingMethods) {
					this.methodUnionFind.union(referencingMethod, otherReferencingMethod);
				}
			}
		}

		adjacencyEntryIterator = this.fieldReferenceGraph.getAdjacencyEntries().iterator();

		while (adjacencyEntryIterator.hasNext()) {
			Set<AbstractMethodNode> fieldReferencingMethods = adjacencyEntryIterator.next().getValue();

			for (AbstractMethodNode fieldReferencingMethod : fieldReferencingMethods) {
				for (AbstractMethodNode otherFieldReferencingMethod : fieldReferencingMethods) {
					this.methodUnionFind.union(fieldReferencingMethod, otherFieldReferencingMethod);
				}
			}
		}

		for (AbstractMethodNode unionFindMethod : GlobalUtils.newHashSet(this.methodUnionFind.getElements())) {
			if (!unionFindMethod.isPrivate() && !unionFindMethod.isStatic()) {
				Set<AbstractMethodNode> methodAndOverrides = hierarchy.getMethodAndOverrides(unionFindMethod);
				if (methodAndOverrides != null) {
					for (AbstractMethodNode overrideMethod : methodAndOverrides) {
						this.methodUnionFind.union(unionFindMethod, overrideMethod);
						this.methodPrefixes.put(overrideMethod, this.methodPrefixes.get(unionFindMethod));
					}
				}
			}
		}

		for (Entry<FieldNode, Set<AbstractMethodNode>> fieldReferenceEntry : this.fieldReferenceGraph.getAdjacencyEntries()) {
			FieldNode adjacencyField = fieldReferenceEntry.getKey();

			for (AbstractMethodNode entryReferencingMethod : fieldReferenceEntry.getValue()) {
				IdentityHashSet<AbstractMethodNode> fieldMethodGroup = this.methodUnionFind.getSet(entryReferencingMethod);
				this.fieldsByMethodGroup.addReference(fieldMethodGroup, adjacencyField);
			}
		}

		for (Entry<FieldNode, Set<AbstractMethodNode>> fieldEntryCopy : GlobalUtils.newHashSet(this.fieldReferenceGraph.getAdjacencyEntries())) {
			FieldNode entryField = fieldEntryCopy.getKey();
			Set<AbstractMethodNode> entryMethods = fieldEntryCopy.getValue();

			for (AbstractMethodNode entryMethod : GlobalUtils.newHashSet(entryMethods)) {
				Iterator<AbstractMethodNode> methodGroupIterator = this.methodUnionFind.getSet(entryMethod).iterator();

				while (methodGroupIterator.hasNext()) {
					entryMethods.add(methodGroupIterator.next());
				}
			}
		}

		for (Entry<AbstractMethodNode, IdentityHashSet<AbstractMethodNode>> unionEntry : this.methodUnionFind.getEntries()) {
			AbstractMethodNode unionEntryMethod = unionEntry.getKey();
			String methodPrefix = this.methodPrefixes.get(unionEntryMethod);
			this.methodsByGroupAndPrefix.addReference(unionEntry.getValue(), methodPrefix, unionEntryMethod);
		}

		for (Entry<IdentityHashSet<AbstractMethodNode>, ReferenceGraph<String, AbstractMethodNode>> groupGraphEntry : this.methodsByGroupAndPrefix.getEntrySet()) {
			IdentityHashSet<AbstractMethodNode> methodGroup = groupGraphEntry.getKey();
			Set<FieldNode> referencingFields = this.fieldsByMethodGroup.getReferences(methodGroup);
			FieldNode firstExcludedField = null;
			if (referencingFields != null) {
				for (FieldNode candidateField : referencingFields) {
					if (this.isFieldExcluded(candidateField)) {
						firstExcludedField = candidateField;
						break;
					}
				}
			}

			AbstractMethodNode nonWorkspaceMethod = null;
			AbstractMethodNode excludedMethod = null;

			for (Entry<String, Set<AbstractMethodNode>> prefixEntry : groupGraphEntry.getValue().getAdjacencyEntries()) {
				HashSet<AbstractMethodNode> baseMethods = GlobalUtils.newHashSet();
				HashSet<AbstractMethodNode> methodsWithoutBase = GlobalUtils.newHashSet();

				for (AbstractMethodNode candidateMethod : prefixEntry.getValue()) {
					if (!candidateMethod.isWorkspaceNode()) {
						nonWorkspaceMethod = candidateMethod;
					} else if (this.isMethodExcluded((MethodNode)candidateMethod)) {
						excludedMethod = candidateMethod;
					}

					AbstractMethodNode baseMethod = null;
					if (!candidateMethod.isPrivate() && !candidateMethod.isStatic()) {
						baseMethod = hierarchy.getBaseMethod(candidateMethod);
					}

					if (baseMethod == null) {
						methodsWithoutBase.add(candidateMethod);
					} else {
						baseMethods.add(baseMethod);
					}
				}

				methodsWithoutBase.removeAll(baseMethods);
				StringBuilder reasonBuilder = new StringBuilder();
				if (baseMethods.size() > 1 || referencingFields != null && referencingFields.size() > 1) {
					reasonBuilder.append("Complex property inter-relationships");
				}

				if (firstExcludedField != null) {
					if (reasonBuilder.length() > 0) {
						reasonBuilder.append(" and ");
					}

					reasonBuilder.append("Field '");
					reasonBuilder.append(ExcludeStatementManager.formatFieldForDisplay(firstExcludedField, this));
					reasonBuilder.append("' is excluded");
				}

				if (excludedMethod != null) {
					if (reasonBuilder.length() > 0) {
						reasonBuilder.append(" and ");
					}

					reasonBuilder.append("Method '");
					reasonBuilder.append(ExcludeStatementManager.formatMethodForDisplay(excludedMethod, this));
					reasonBuilder.append("' is excluded");
				}

				if (nonWorkspaceMethod != null) {
					if (reasonBuilder.length() > 0) {
						reasonBuilder.append(" and ");
					}

					reasonBuilder.append("Method '");
					reasonBuilder.append(ExcludeStatementManager.formatMethodForDisplay(nonWorkspaceMethod, this));
					reasonBuilder.append("' is not opened for obfuscation");
				}

				if (reasonBuilder.length() > 0) {
					String reason = reasonBuilder.toString();
					String formattedMethodNames = this.formatMembersForDisplay(GlobalUtils.newHashSet(methodGroup), newNameMappings);
					String formattedFieldNames;
					if (referencingFields != null) {
						formattedFieldNames = this.formatMembersForDisplay(GlobalUtils.newHashSet(referencingFields), newNameMappings);
					} else {
						formattedFieldNames = "";
					}

					super.scriptContext
						.logWarning(
							"Excluding method"
								+ (methodGroup.size() == 1 ? "" : "s")
								+ " ["
								+ formattedMethodNames
								+ "] and field"
								+ (referencingFields != null && referencingFields.size() == 1 ? "" : "s")
								+ "["
								+ formattedFieldNames
								+ "] from name obfuscation due to \""
								+ reason
								+ "\""
						);

					for (AbstractMethodNode groupMethod : methodGroup) {
						if (groupMethod.isWorkspaceNode()) {
							this.excludeMethod((MethodNode)groupMethod, reason);
						}
					}

					if (referencingFields != null) {
						for (FieldNode groupReferencedField : referencingFields) {
							if (groupReferencedField.isWorkspaceNode()) {
								this.excludeField(groupReferencedField, reason);
							}
						}
					}
				}
			}
		}
	}

	public final boolean isPackageExcluded(String packageName) {
		return this.excludedPackages.containsKey(packageName);
	}

	public Set<AbstractMethodNode> getFieldAccessorMethods(FieldNode fieldNode) {
		Set<AbstractMethodNode> referencingMethods = this.fieldReferenceGraph.getReferences(fieldNode);
		if (referencingMethods != null && referencingMethods.size() > 0) {
			IdentityHashSet<AbstractMethodNode> methodGroup = this.methodUnionFind.getSet(referencingMethods.iterator().next());
			HashSet<AbstractMethodNode> workspaceMethods = GlobalUtils.newHashSet();

			for (AbstractMethodNode groupMethod : methodGroup) {
				if (groupMethod.isWorkspaceNode()) {
					workspaceMethods.add(groupMethod);
				}
			}

			return workspaceMethods;
		} else {
			return null;
		}
	}

	public final void unexcludeMethodSilently(MethodNode methodNode) {
		ClassNode ownerClass = this.excludedMethods.remove(methodNode);
		if (ownerClass != null) {
			this.processableMethods.put(methodNode, ownerClass);
		}
	}

	public void processFieldChangeLog(ChangeLogContext changeLogContext, HashMap<String, String> newNameMappings, FixedClassManager fixedClassManager) {
		if (changeLogContext != null) {
			ArrayList<FieldNode> renamedFields = new ArrayList<>();

			for (FieldNode excludedField : super.excludedFields.keySet()) {
				if (changeLogContext.hasNewFieldName(excludedField.getInternalName(), excludedField.getFieldSignature())) {
					renamedFields.add(excludedField);
				}
			}

			for (int fieldIndex = 0; fieldIndex < renamedFields.size(); fieldIndex++) {
				FieldNode renamedField = renamedFields.get(fieldIndex);
				this.unexcludeFieldWithWarning(renamedField, "Input ChangeLog specifies a new name '" + changeLogContext.getNewFieldName(renamedField.getInternalName(), renamedField.getFieldSignature()) + "'", newNameMappings);
				if (fixedClassManager != null) {
					fixedClassManager.recordFieldRename(renamedField, changeLogContext.getLogFileName());
				}
			}
		}
	}

	public final void unexcludePackage(String packageName, String reason) {
		if (packageName.length() != 0 && this.excludedPackages.remove(packageName) != null) {
			this.processablePackages.put(packageName, packageName);
			String binaryPackageName = GlobalUtils.internalToBinaryName(packageName);
			if (super.scriptContext.isVerbose()) {
				super.logWriter.println("\tUnexcluding package \"" + binaryPackageName + "\" for name obfuscation because of \"" + reason + "\"");
			}

			String subPackagePrefix = packageName + "/";
			Iterator<String> excludedPackageIterator = this.excludedPackages.keySet().iterator();

			while (excludedPackageIterator.hasNext()) {
				String subPackageName = excludedPackageIterator.next();
				if (subPackageName.startsWith(subPackagePrefix)) {
					excludedPackageIterator.remove();
					this.processablePackages.put(subPackageName, subPackageName);
					if (super.scriptContext.isVerbose()) {
						super.logWriter
							.println("\tUnexcluding package \"" + GlobalUtils.internalToBinaryName(subPackageName) + "\" for name obfuscation because \"" + binaryPackageName + "\" has been unexcluded");
					}
				}
			}
		}
	}

	public final boolean excludeClassWithReason(ClassNode classNode, String reason, boolean warn) {
		if (this.excludeElements.containsKey(classNode)) {
			ClassNode linkedClass = (ClassNode)this.getExcludeElement(classNode).getLinkedClass();
			if (linkedClass != null) {
				super.scriptContext
					.logWarning(
						"Class \""
							+ this.formatClassForDisplay(classNode)
							+ "\" already linked by a <link> exclusion to \""
							+ this.formatClassForDisplay(linkedClass)
							+ "\" so \""
							+ reason
							+ "\" will have no impact on this class name."
					);
				return false;
			}
		}

		Object existingEntry = super.processableClasses.remove(classNode);
		if (existingEntry != null) {
			super.excludedClasses.put(classNode, classNode);
			String warningMessage = "Excluding class \"" + this.formatClassForDisplay(classNode) + "\" from name obfuscation because of \"" + reason + "\"";
			if (warn) {
				super.scriptContext.logWarning(warningMessage);
			} else if (super.scriptContext.isVerbose() && super.logWriter != null) {
				super.logWriter.println("\t" + warningMessage);
			}
		}

		return existingEntry != null;
	}

	public void processClassChangeLog(ChangeLogContext changeLogContext, FixedClassManager fixedClassManager) {
		if (changeLogContext != null) {
			ArrayList<ClassNode> renamedClasses = new ArrayList<>();

			for (ClassNode excludedClass : super.excludedClasses.keySet()) {
				if (changeLogContext.isSimpleNameChanged(excludedClass.getInternalName())) {
					renamedClasses.add(excludedClass);
				}
			}

			for (int classIndex = 0; classIndex < renamedClasses.size(); classIndex++) {
				ClassNode renamedClass = renamedClasses.get(classIndex);
				this.includeClassWithReason(renamedClass, "Input ChangeLog specifies a new name", true);
				if (fixedClassManager != null) {
					fixedClassManager.recordClassRename(renamedClass, changeLogContext.getLogFileName());
				}
			}
		}
	}

	public final void excludeMethodDueToOther(MethodNode methodNode, MethodNode otherMethodNode) {
		if (!methodNode.isConstructor() && !methodNode.isClinit()) {
			ClassNode ownerClass = this.processableMethods.remove(methodNode);
			if (ownerClass != null) {
				this.excludedMethods.put(methodNode, ownerClass);
				methodNode.getMethodOwner().getClassNameBinary();
				otherMethodNode.getMethodOwner().getClassNameBinary();
				if (super.scriptContext.isVerbose() && super.logWriter != null) {
					super.logWriter
						.println(
							"\tExcluding method \""
								+ ExcludeStatementManager.formatMethodForDisplay(methodNode, this)
								+ "\" in class \""
								+ ExcludeStatementManager.formatClassForDisplay(methodNode.getMethodOwner(), this.workspace)
								+ "\" from name obfuscation because method \""
								+ ExcludeStatementManager.formatMethodForDisplay(otherMethodNode, this)
								+ "\" in class \""
								+ ExcludeStatementManager.formatClassForDisplay(otherMethodNode.getMethodOwner(), this.workspace)
								+ "\" has been excluded (A)"
						);
				}
			}
		}
	}

	public final Enumeration<String> getExcludedPackageNames() {
		String[] excludedPackageNames = new String[this.excludedPackages.size()];
		Iterator<String> excludedPackageIterator = this.excludedPackages.keySet().iterator();
		int packageIndex = 0;

		while (excludedPackageIterator.hasNext()) {
			excludedPackageNames[packageIndex++] = excludedPackageIterator.next();
		}

		return new ArrayEnumeration<>(excludedPackageNames);
	}

	@Override
	public final boolean excludeClass(ClassNode classNode, String reason) {
		return this.excludeClassWithReason(classNode, reason, false);
	}

	public final void matchMethodToField(MethodNode methodNode, String namePrefix, Object resource, String reason) {
		String previousPrefix = this.methodPrefixes.put(methodNode, namePrefix);
		if (previousPrefix != null && !previousPrefix.equals(namePrefix)) {
			this.methodPrefixes.put(methodNode, previousPrefix);
			super.scriptContext
				.logWarning(
					"Method \""
						+ ExcludeStatementManager.formatMethodForDisplay(methodNode, this)
						+ "\" in class \""
						+ ExcludeStatementManager.formatClassForDisplay(methodNode.getMethodOwner(), this.workspace)
						+ "\" matched to more than one prefix : \""
						+ namePrefix
						+ "\", \""
						+ previousPrefix
						+ "\". Prefix \""
						+ namePrefix
						+ "\" will be ignored."
				);
		}

		ClassNode ownerClass = methodNode.getMethodOwner();
		int prefixLength = namePrefix.length();
		String methodName = methodNode.getName();
		String fieldTypeDescriptor = null;
		String fieldName;
		if (prefixLength > 0) {
			fieldName = MethodSignature.getFieldNameFromAccessor(methodName, namePrefix);
		} else {
			fieldName = methodName;
		}

		String returnDescriptor = methodNode.getReturnDescriptor();
		int parameterCount = methodNode.getParameterCount();
		if (parameterCount == 0 && !returnDescriptor.equals("V")) {
			fieldTypeDescriptor = returnDescriptor;
		} else if (parameterCount == 1 && returnDescriptor.equals("V")) {
			String parameterDescriptor = methodNode.getParameterDescriptor();
			fieldTypeDescriptor = parameterDescriptor.substring(1, parameterDescriptor.length() - 1);
		}

		ClassHierarchyNode ownerHierarchyNode = ClassHierarchyNode.getNodeByInternalName(ownerClass.getInternalName());
		AbstractFieldNode[] matchingFields = this.findMatchingFields(ownerClass, fieldName, fieldTypeDescriptor);
		if (matchingFields == null || matchingFields.length == 0) {
			do {
				ClassNode outerClass = ownerHierarchyNode.getOuterClassWrapper();
				if (outerClass != null) {
					matchingFields = this.findMatchingFields(outerClass, fieldName, fieldTypeDescriptor);
					ownerHierarchyNode = ClassHierarchyNode.getNodeByInternalName(outerClass.getInternalName());
				} else {
					ownerHierarchyNode = null;
				}
			} while (ownerHierarchyNode != null && (matchingFields == null || matchingFields.length == 0));
		}

		if (matchingFields != null && matchingFields.length != 0) {
			if (matchingFields.length == 1) {
				FieldNode matchedField = (FieldNode)matchingFields[0];
				this.propertyReferenceGraph.addReference(matchedField, methodNode);
				this.fieldReferenceGraph.addReference(matchedField, methodNode);
				String previousFieldPrefix = this.methodFieldPrefixes.put(methodNode, matchedField, namePrefix);
				if (super.scriptContext.isVerbose()) {
					super.logWriter
						.println(
							"\tMethod \""
								+ ExcludeStatementManager.formatMethodForDisplay(methodNode, this)
								+ "\" in class \""
								+ ExcludeStatementManager.formatClassForDisplay(ownerClass, this.workspace)
								+ "\" will be renamed to match field \""
								+ ExcludeStatementManager.formatFieldForDisplay(matchedField, this)
								+ "\""
								+ (namePrefix.length() > 0 ? " with the prefix \"" + namePrefix + "\" retained" : "")
								+ " because of \""
								+ reason
								+ "\""
						);
				}
			} else {
				Arrays.sort(matchingFields);
				if (resource != null) {
					this.propertyReferenceGraph.addReference(resource, methodNode);
				} else {
					this.propertyReferenceGraph.addReference(matchingFields[0], methodNode);
				}

				this.methodFieldPrefixes.put(methodNode, (FieldNode)matchingFields[0], namePrefix);
				StringBuilder fieldNamesBuilder = new StringBuilder();

				for (int fieldIndex = 0; fieldIndex < matchingFields.length; fieldIndex++) {
					this.fieldReferenceGraph.addReference((FieldNode)matchingFields[fieldIndex], methodNode);
					fieldNamesBuilder.append("\"");
					fieldNamesBuilder.append(matchingFields[fieldIndex].getDisplayName());
					fieldNamesBuilder.append("\"");
					if (fieldIndex < matchingFields.length - 1) {
						fieldNamesBuilder.append(", ");
					}
				}

				super.scriptContext
					.logWarning(
						"More than one field found in class \""
							+ ExcludeStatementManager.formatClassForDisplay(ownerClass, this.workspace)
							+ "\" to match method \""
							+ ExcludeStatementManager.formatMethodForDisplay(methodNode, this)
							+ "\" due to '"
							+ reason
							+ "' : "
							+ fieldNamesBuilder
					);
			}
		} else if (prefixLength > 0) {
			if (resource != null) {
				this.propertyReferenceGraph.addReference(resource, methodNode);
			} else {
				this.propertyReferenceGraph.addReference(methodNode.getInternalName() + '~' + fieldName, methodNode);
			}

			this.retainedPrefixMethods.put(methodNode, namePrefix, resource);
			if (super.scriptContext.isVerbose()) {
				super.logWriter
					.println(
						"\tMethod \""
							+ ExcludeStatementManager.formatMethodForDisplay(methodNode, this)
							+ "\" in class \""
							+ ExcludeStatementManager.formatClassForDisplay(ownerClass, this.workspace)
							+ "\" will be renamed with the prefix \""
							+ namePrefix
							+ "\" retained because of \""
							+ reason
							+ "\""
					);
			}
		} else if (resource != null) {
			this.propertyReferenceGraph.addReference(resource, methodNode);
		}
	}

	public final void initializeProcessableMaps(Enumeration<ClassNode> classes, int classCount) {
		this.workspaceClasses = new ClassNode[classCount];
		super.processableClasses = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(classCount));
		super.excludedClasses = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(classCount));
		super.processableFields = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(classCount * 5));
		super.excludedFields = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(classCount * 5));
		this.processableMethods = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(classCount * 5));
		this.excludedMethods = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(classCount * 5));
		int classIndex = 0;

		while (classes.hasMoreElements()) {
			ClassNode classNode = classes.nextElement();
			super.processableClasses.put(classNode, classNode);
			this.workspaceClasses[classIndex++] = classNode;
			ArrayEnumeration<FieldNode> fieldEnumeration = classNode.getFieldEnumeration();

			while (fieldEnumeration.hasMoreElements()) {
				FieldNode fieldNode = fieldEnumeration.nextElement();
				super.processableFields.put(fieldNode, fieldNode.getOwnerClass());
			}

			ArrayEnumeration<MethodNode> methodEnumeration = classNode.getMethodEnumeration();

			while (methodEnumeration.hasMoreElements()) {
				MethodNode methodNode = methodEnumeration.nextElement();
				this.processableMethods.put(methodNode, methodNode.getMethodOwner());
			}
		}

		this.allFields = new FieldNode[super.processableFields.size()];
		int fieldIndex = 0;
		Enumeration<FieldNode> processableFieldEnumeration = this.getProcessableFields();

		while (processableFieldEnumeration.hasMoreElements()) {
			this.allFields[fieldIndex++] = processableFieldEnumeration.nextElement();
		}

		this.allMethods = new MethodNode[this.processableMethods.size()];
		int methodIndex = 0;
		Enumeration<MethodNode> processableMethodEnumeration = this.getProcessableMethods();

		while (processableMethodEnumeration.hasMoreElements()) {
			this.allMethods[methodIndex++] = processableMethodEnumeration.nextElement();
		}
	}

	public void processMethodChangeLog(ChangeLogContext changeLogContext, Hierarchy hierarchy, HashMap<String, String> newNameMappings, FixedClassManager fixedClassManager) {
		if (changeLogContext != null) {
			ArrayList<KeyValuePair<MethodNode, Boolean>> renamedMethodPairs = new ArrayList<>();
			HashMap<MethodNode, KeyValuePair<MethodNode, Boolean>> linkedMethodPairs = GlobalUtils.newHashMap();
			ReferenceGraph<String, MethodSignature> renamedMethodGraph = changeLogContext.getRenamedMethodGraph();

			for (MethodNode excludedMethod : this.excludedMethods.keySet()) {
				AbstractMethodNode baseMethod = null;
				if (!excludedMethod.isPrivate() && !excludedMethod.isStatic()) {
					baseMethod = hierarchy.getBaseMethod(excludedMethod);
				}

				MethodSignature methodSignature = excludedMethod.getMethodSignature();
				String internalName = excludedMethod.getInternalName();
				ClassNode ownerClass = excludedMethod.getMethodOwner();
				ClassHierarchyNode hierarchyNode = ClassHierarchyNode.getNodeByInternalName(internalName);
				if (changeLogContext.hasNewMethodName(internalName, methodSignature) || baseMethod != null && changeLogContext.hasNewMethodName(baseMethod.getInternalName(), methodSignature) || renamedMethodGraph.isReferencedBy(internalName, methodSignature)) {
					Boolean nameChanged;
					if (changeLogContext.hasNewMethodName(internalName, methodSignature) || baseMethod != null && changeLogContext.hasNewMethodName(baseMethod.getInternalName(), methodSignature)) {
						nameChanged = Boolean.TRUE;
					} else {
						nameChanged = Boolean.FALSE;
					}

					renamedMethodPairs.add(new KeyValuePair<>(excludedMethod, nameChanged));
					if (ownerClass.isInterface()) {
						AtomicReference<List<ClassHierarchyNode>> implementationsRef = hierarchy.getMethodImplementationsRef(hierarchyNode, methodSignature);
						if (implementationsRef != null) {
							List<ClassHierarchyNode> implementations = implementationsRef.get();

							for (int implementationIndex = 0; implementationIndex < implementations.size(); implementationIndex++) {
								ClassHierarchyNode implementationNode = implementations.get(implementationIndex);
								if (implementationNode.isWorkspaceClass() && implementationNode != hierarchyNode) {
									MethodNode implementationMethod = this.workspace.getMethodBySignature(implementationNode.getClassNodeIfWorkspace(), methodSignature);
									if (implementationMethod != null) {
										linkedMethodPairs.put(implementationMethod, new KeyValuePair<>(excludedMethod, nameChanged));
									}
								}
							}
						}
					}
				}
			}

			for (int renamedPairIndex = 0; renamedPairIndex < renamedMethodPairs.size(); renamedPairIndex++) {
				KeyValuePair<MethodNode, Boolean> renamedPair = renamedMethodPairs.get(renamedPairIndex);
				MethodNode renamedMethod = renamedPair.getKey();
				if (renamedPair.getValue()) {
					this.unexcludeMethodWithWarning(renamedMethod, "Input ChangeLog specifies a new name", newNameMappings);
				} else {
					this.unexcludeMethodWithWarning(renamedMethod, "Input ChangeLog specifies name unchanged", newNameMappings);
				}

				linkedMethodPairs.remove(renamedMethod);
				if (fixedClassManager != null) {
					fixedClassManager.recordMethodChange(renamedMethod, changeLogContext.getLogFileName(), true);
				}
			}

			for (Entry<MethodNode, KeyValuePair<MethodNode, Boolean>> linkedMethodEntry : linkedMethodPairs.entrySet()) {
				MethodNode linkedMethod = linkedMethodEntry.getKey();
				KeyValuePair<MethodNode, Boolean> linkedPair = linkedMethodEntry.getValue();
				MethodNode linkedSourceMethod = linkedPair.getKey();
				Boolean linkedNameChanged = linkedPair.getValue();
				String linkedOwnerClassDisplay = ExcludeStatementManager.formatClassForDisplay(linkedSourceMethod.getMethodOwner(), this.workspace);
				if (linkedNameChanged) {
					this.unexcludeMethodWithWarning(linkedMethod, "Input ChangeLog specifies a new name for a linked method in class '" + linkedOwnerClassDisplay + "'", newNameMappings);
				} else {
					this.unexcludeMethodWithWarning(linkedMethod, "Input ChangeLog specifies name unchanged for linked method in class '" + linkedOwnerClassDisplay + "'", newNameMappings);
				}

				if (fixedClassManager != null) {
					fixedClassManager.recordMethodChange(linkedMethod, changeLogContext.getLogFileName(), true);
				}
			}

			for (Entry<FieldNode, Set<AbstractMethodNode>> fieldReferenceEntry : this.fieldReferenceGraph.getAdjacencyEntries()) {
				AbstractMethodNode renamedReferencingMethod = null;

				for (AbstractMethodNode referencingMethod : fieldReferenceEntry.getValue()) {
					String referencingInternalName = referencingMethod.getInternalName();
					MethodSignature referencingMethodSignature = referencingMethod.getMethodSignature();
					if (changeLogContext.getNewMethodName(referencingInternalName, referencingMethodSignature) != null) {
						renamedReferencingMethod = referencingMethod;
						break;
					}
				}

				if (renamedReferencingMethod != null) {
					String ownerClassDisplay = ExcludeStatementManager.formatClassForDisplay(renamedReferencingMethod.getClassNode(), this.workspace);

					for (AbstractMethodNode linkedReferencingMethod : fieldReferenceEntry.getValue()) {
						if (linkedReferencingMethod.isWorkspaceNode() && this.isMethodExcluded((MethodNode)linkedReferencingMethod)) {
							this.unexcludeMethodWithWarning((MethodNode)linkedReferencingMethod, "Input ChangeLog specifies a new name for a linked method in class '" + ownerClassDisplay + "'", newNameMappings);
						}
					}
				}
			}

			HashSet<IdentityHashSet<AbstractMethodNode>> seenSets = GlobalUtils.newHashSet();

			for (IdentityHashSet<AbstractMethodNode> methodGroup : this.methodUnionFind.getDistinctSets()) {
				if (seenSets.add(methodGroup)) {
					AbstractMethodNode renamedGroupMethod = null;

					for (AbstractMethodNode groupMethod : methodGroup) {
						String groupMethodInternalName = groupMethod.getInternalName();
						MethodSignature groupMethodSignature = groupMethod.getMethodSignature();
						if (changeLogContext.getNewMethodName(groupMethodInternalName, groupMethodSignature) != null) {
							renamedGroupMethod = groupMethod;
							break;
						}
					}

					if (renamedGroupMethod != null) {
						String groupOwnerClassDisplay = ExcludeStatementManager.formatClassForDisplay(renamedGroupMethod.getClassNode(), this.workspace);

						for (AbstractMethodNode excludedGroupMethod : methodGroup) {
							if (excludedGroupMethod.isWorkspaceNode() && this.isMethodExcluded((MethodNode)excludedGroupMethod)) {
								this.unexcludeMethodWithWarning((MethodNode)excludedGroupMethod, "Input ChangeLog specifies a new name for a linked method in class '" + groupOwnerClassDisplay + "'", newNameMappings);
								this.retainedPrefixMethods.removeGroup(excludedGroupMethod);
							}
						}
					}
				}
			}

			Iterator<Entry<MethodNode, Map<String, Object>>> prefixMethodIterator = this.retainedPrefixMethods.getEntrySet().iterator();

			while (prefixMethodIterator.hasNext()) {
				AbstractMethodNode prefixMethod = prefixMethodIterator.next().getKey();
				String prefixMethodInternalName = prefixMethod.getInternalName();
				MethodSignature prefixMethodSignature = prefixMethod.getMethodSignature();
				if (changeLogContext.hasProcessedMethodMapping(prefixMethodInternalName, prefixMethodSignature)) {
					String binaryClassName;
					String mappedName;
					if (newNameMappings != null && (mappedName = newNameMappings.get(prefixMethod.getInternalName())) != null) {
						binaryClassName = GlobalUtils.internalToBinaryName(mappedName);
					} else {
						binaryClassName = prefixMethod.getBinaryName();
					}

					super.scriptContext
						.logWarning(
							"Prefix exclusion of method \""
								+ ExcludeStatementManager.formatMethodForDisplay(prefixMethod, this)
								+ "\" in class \""
								+ binaryClassName
								+ "\" may not be effective because method appears in input change log \""
								+ changeLogContext.getLogFileName()
								+ "\" (2)"
						);
				}
			}
		}
	}

	@Override
	public final void includeClass(ClassNode classNode, String reason) {
		this.includeClassWithReason(classNode, reason, false);
	}

	@Override
	public boolean hasNoProcessableMethods() {
		if (this.processableMethods.size() == 0) {
			return true;
		} else {
			for (MethodNode processableMethod : this.processableMethods.keySet()) {
				if (!processableMethod.isConstructor() && !processableMethod.isClinit()) {
					return false;
				}
			}

			return true;
		}
	}

	public Set<MethodNode> getRetainedPrefixMethods() {
		return GlobalUtils.newHashSet(this.retainedPrefixMethods.getGroupKeySet());
	}

	public final ExcludeElement getExcludeElement(ClassNode classNode) {
		return this.excludeElements.get(classNode);
	}

	public final Enumeration<AbstractClassNode> getChangeLogClasses() {
		return new ArrayEnumeration<>(this.changeLogClasses);
	}

	public final void excludeField(FieldNode fieldNode, String reason) {
		ClassNode ownerClass = super.processableFields.remove(fieldNode);
//						+ ExcludeStatementManager.formatFieldForDisplay(fieldNode, this)
//						+ "\" in class \""
//						+ this.formatClassForDisplay(fieldNode.getOwnerClass())
//						+ "\" from name obfuscation because of \""
//						+ reason
//						+ "\""
//		);;
		if (ownerClass != null) {
			this.excludedFields.put(fieldNode, ownerClass);
			if (super.scriptContext.isVerbose() && super.logWriter != null) {
				super.logWriter
					.println(
						"\tExcluding field \""
							+ ExcludeStatementManager.formatFieldForDisplay(fieldNode, this)
							+ "\" in class \""
							+ this.formatClassForDisplay(fieldNode.getOwnerClass())
							+ "\" from name obfuscation because of \""
							+ reason
							+ "\""
					);
			}
		}
	}

	public Set<AbstractMethodNode> getMethodsWithSamePrefix(AbstractMethodNode methodNode) {
		IdentityHashSet<AbstractMethodNode> methodGroup = this.methodUnionFind.getSet(methodNode);
		return methodGroup != null ? GlobalUtils.newHashSet(this.methodsByGroupAndPrefix.getReferences(methodGroup, this.methodPrefixes.get(methodNode))) : null;
	}

	@Override
	public final Enumeration<MethodNode> getAllMethods() {
		return new ArrayEnumeration<>(this.allMethods);
	}

	public final void excludeMethod(MethodNode methodNode, String reason) {
		if (!methodNode.isConstructor() && !methodNode.isClinit()) {
			ClassNode ownerClass = this.processableMethods.remove(methodNode);
			if (ownerClass != null) {
				this.excludedMethods.put(methodNode, ownerClass);
				if (super.scriptContext.isVerbose() && super.logWriter != null) {
					super.logWriter
						.println(
							"\tExcluding method \""
								+ ExcludeStatementManager.formatMethodForDisplay(methodNode, this)
								+ "\" in class \""
								+ this.formatClassForDisplay(methodNode.getMethodOwner())
								+ "\" from name obfuscation because of \""
								+ reason
								+ "\" (B)"
						);
				}
			}
		}
	}

	public Set<AbstractMethodNode> getMethodGroup(AbstractMethodNode methodNode) {
		IdentityHashSet<AbstractMethodNode> methodGroup = this.methodUnionFind.getSet(methodNode);
		return methodGroup != null ? GlobalUtils.newHashSet(methodGroup) : null;
	}

	public final void initializePackageNames(PackageManager packageManager) {
		List<String> packageNames = packageManager.getPackageNames();
		int packageCount = packageNames.size();
		this.allPackageNames = new String[packageCount];

		for (int packageIndex = 0; packageIndex < packageCount; packageIndex++) {
			String packageName = packageNames.get(packageIndex);
			this.processablePackages.put(packageName, packageName);
			this.allPackageNames[packageIndex] = packageName;
		}
	}

	private AbstractFieldNode[] findMatchingFields(AbstractClassNode classNode, String fieldName, String fieldTypeDescriptor) {
		AbstractFieldNode[] matchingFields = null;
		if (fieldTypeDescriptor == null) {
			matchingFields = classNode.findFieldsByName(fieldName);
		} else {
			AbstractFieldNode matchedField = classNode.findField(fieldName, fieldTypeDescriptor);
			if (matchedField != null) {
				matchingFields = new AbstractFieldNode[]{matchedField};
			}
		}

		return matchingFields;
	}

	public final void matchClassToLinkedClass(ClassNode classNode, String namePrefix, String nameSuffix, List<String> searchPaths, String reason) {
		if (super.excludedClasses.containsKey(classNode)) {
			super.scriptContext
				.logWarning(
					"Class \""
						+ ExcludeStatementManager.formatClassForDisplay(classNode, this.workspace)
						+ "\" already subject to an exclusion so \""
						+ reason
						+ "\" will have no impact on this class."
				);
		} else {
			int prefixLength = namePrefix.length();
			int suffixLength = nameSuffix.length();
			String simpleName = classNode.getSimpleNameInternal();
			String packagePrefix = classNode.getPackageName();
			if (packagePrefix.length() > 0) {
				packagePrefix = packagePrefix + "/";
			}

			String linkedClassName = packagePrefix + simpleName.substring(prefixLength, simpleName.length() - suffixLength);
			ClassNode linkedClass = ClassHierarchyNode.getWorkspaceClassNode(linkedClassName);
			if (linkedClass == null && searchPaths.size() > 0) {
				String strippedSimpleName = simpleName.substring(prefixLength, simpleName.length() - suffixLength);

				label86:
				for (int searchPathIndex = 0; searchPathIndex < searchPaths.size(); searchPathIndex++) {
					String searchPath = searchPaths.get(searchPathIndex);
					if (searchPath.indexOf(42) == -1) {
						linkedClassName = searchPath + "/" + strippedSimpleName;
						linkedClass = ClassHierarchyNode.getWorkspaceClassNode(linkedClassName);
						if (linkedClass != null) {
							break;
						}
					} else {
						for (int packageNameIndex = 0; packageNameIndex < this.allPackageNames.length; packageNameIndex++) {
							String packageName = this.allPackageNames[packageNameIndex];
							if (StringUtils.matchesWildcard(packageName, searchPath)) {
								linkedClassName = packageName + "/" + strippedSimpleName;
								linkedClass = ClassHierarchyNode.getWorkspaceClassNode(linkedClassName);
								if (linkedClass != null) {
									break label86;
								}
							}
						}
					}
				}
			}

			String linkedClassBinaryName = GlobalUtils.internalToBinaryName(linkedClassName);
			boolean previousLinkRestored = false;
			ExcludeElement previousElement = this.excludeElements.put(classNode, new ExcludeElement(linkedClass, namePrefix, nameSuffix));
			if (previousElement != null) {
				ClassNode previousLinkedClass = (ClassNode)previousElement.getLinkedClass();
				if (previousLinkedClass != null) {
					if (linkedClass == null) {
						this.excludeElements.put(classNode, previousElement);
						previousLinkRestored = true;
					} else if (previousLinkedClass != linkedClass) {
						super.scriptContext
							.logFatalError(
								"Class \""
									+ ExcludeStatementManager.formatClassForDisplay(classNode, this.workspace)
									+ "\" matches \""
									+ reason
									+ "\" but it is also matched by another <link> exclusion parameter with"
									+ (((String)previousElement.getPrefix()).length() > 0 ? " prefix \"" + (String)previousElement.getPrefix() + "\" and" : "")
									+ " suffix \""
									+ nameSuffix
									+ "\". Cannot satisfy both matches."
							);
					}
				}
			}

			if (linkedClass == null) {
				super.scriptContext
					.logWarning(
						"Class \""
							+ ExcludeStatementManager.formatClassForDisplay(classNode, this.workspace)
							+ "\" matches \""
							+ reason
							+ "\" but no class matching \""
							+ simpleName.substring(prefixLength, simpleName.length() - suffixLength)
							+ "\" found in the same package"
							+ (searchPaths.size() == 0 ? "." : " or in the search path.")
					);
			}

			if (super.scriptContext.isVerbose()) {
				if (linkedClass == null) {
					if (!previousLinkRestored) {
						super.logWriter
							.println(
								"\tExcluding"
									+ (prefixLength > 0 ? " prefix \"" + namePrefix + "\" and" : "")
									+ " suffix \""
									+ nameSuffix
									+ "\" from renaming of class \""
									+ this.formatClassForDisplay(classNode)
									+ "\" from name obfuscation because of \""
									+ reason
									+ "\"."
							);
					}
				} else {
					super.logWriter
						.println(
							"\tClass \""
								+ this.formatClassForDisplay(classNode)
								+ "\" will be renamed to match \""
								+ linkedClassBinaryName
								+ "\" with the"
								+ (prefixLength > 0 ? " prefix \"" + namePrefix + "\" and the" : "")
								+ " suffix \""
								+ nameSuffix
								+ "\" retained because of \""
								+ reason
								+ "\""
						);
				}
			}
		}
	}

	public final String getMethodPrefix(AbstractMethodNode methodNode) {
		return methodNode != null ? this.methodPrefixes.get(methodNode) : null;
	}

	@Override
	public final Enumeration<FieldNode> getAllFields() {
		return new ArrayEnumeration<>(this.allFields);
	}

	public Set<FieldNode> getReferencedFields() {
		return GlobalUtils.newHashSet(this.fieldReferenceGraph.getMembers());
	}

	private final void unexcludePackageWithWarning(String packageName, ChangeLogContext changeLogContext, FixedClassManager fixedClassManager, String reason, ArrayList<String> otherExcludedPackageNames) {
		if (packageName.length() != 0 && this.excludedPackages.remove(packageName) != null) {
			this.processablePackages.put(packageName, packageName);
			String binaryPackageName = GlobalUtils.internalToBinaryName(packageName);
			super.scriptContext.logWarning("UNEXCLUDING package \"" + binaryPackageName + "\" for name obfuscation because \"" + reason + "\"");
			if (fixedClassManager != null) {
				fixedClassManager.recordPackageRename(packageName, changeLogContext.getLogFileName());
			}

			int otherPackageCount = otherExcludedPackageNames.size();

			for (int otherPackageIndex = 0; otherPackageIndex < otherPackageCount; otherPackageIndex++) {
				String otherPackageName = otherExcludedPackageNames.get(otherPackageIndex);
				if (otherPackageName.compareTo(packageName) <= 0) {
					break;
				}

				if (otherPackageName.startsWith(packageName) && otherPackageName.charAt(packageName.length()) == '/' && this.excludedPackages.remove(otherPackageName) != null) {
					this.processablePackages.put(otherPackageName, otherPackageName);
					super.scriptContext
						.logWarning("UNEXCLUDING package \"" + GlobalUtils.internalToBinaryName(otherPackageName) + "\" for name obfuscation because \"" + binaryPackageName + "\" has been unexcluded");
					if (fixedClassManager != null) {
						fixedClassManager.recordPackageRename(packageName, changeLogContext.getLogFileName());
					}
				}
			}
		}
	}

	public final void unexcludeMethod(MethodNode methodNode, String reason) {
		if (!methodNode.isConstructor() && !methodNode.isClinit()) {
			ClassNode ownerClass = this.excludedMethods.remove(methodNode);
			if (ownerClass != null) {
				this.processableMethods.put(methodNode, ownerClass);
				if (super.scriptContext.isVerbose()) {
					super.logWriter
						.println(
							"\tUnexcluding method \""
								+ ExcludeStatementManager.formatMethodForDisplay(methodNode, this)
								+ "\" in class \""
								+ this.formatClassForDisplay(methodNode.getMethodOwner())
								+ "\" for name obfuscation because of \""
								+ reason
								+ "\""
						);
				}
			}
		}
	}

	public final Enumeration<String> getProcessablePackageNames() {
		String[] processablePackageNames = new String[this.processablePackages.size()];
		Iterator<String> processablePackageIterator = this.processablePackages.keySet().iterator();
		int packageIndex = 0;

		while (processablePackageIterator.hasNext()) {
			processablePackageNames[packageIndex++] = processablePackageIterator.next();
		}

		return new ArrayEnumeration<>(processablePackageNames);
	}

	public final void excludePackage(String packageName, String reason) {
		if (packageName.length() != 0 && this.processablePackages.remove(packageName) != null) {
			this.excludedPackages.put(packageName, packageName);
			String binaryPackageName = GlobalUtils.internalToBinaryName(packageName);
			if (super.scriptContext.isVerbose()) {
				super.logWriter.println("\tExcluding package \"" + binaryPackageName + "\" from name obfuscation because of \"" + reason + "\"");
			}

			int separatorIndex = packageName.lastIndexOf("/");

			while (separatorIndex > -1) {
				String parentPackageName = packageName.substring(0, separatorIndex);
				if (this.processablePackages.remove(parentPackageName) != null) {
					this.excludedPackages.put(parentPackageName, parentPackageName);
					if (super.scriptContext.isVerbose()) {
						super.logWriter
							.println("\tExcluding package \"" + GlobalUtils.internalToBinaryName(parentPackageName) + "\" from name obfuscation because \"" + binaryPackageName + "\" has been excluded");
					}
				}

				separatorIndex = parentPackageName.lastIndexOf("/");
			}
		}
	}

	public final void unexcludeMethodWithWarning(MethodNode methodNode, String reason, HashMap<String, String> newNameMappings) {
		if (!methodNode.isConstructor() && !methodNode.isClinit()) {
			ClassNode ownerClass = this.excludedMethods.remove(methodNode);
			if (ownerClass != null) {
				this.processableMethods.put(methodNode, ownerClass);
				super.scriptContext
					.logWarning(
						"UNEXCLUDING method \""
							+ ExcludeStatementManager.formatMethodForDisplay(methodNode, this)
							+ "\" in class \""
							+ this.formatClassForDisplay(methodNode.getMethodOwner())
							+ "\" for name obfuscation because \""
							+ reason
							+ "\""
					);
			}
		}
	}

	public final void includeClassWithReason(ClassNode classNode, String reason, boolean warn) {
		if (super.excludedClasses.remove(classNode) != null) {
			super.processableClasses.put(classNode, classNode);
			if (warn) {
				super.scriptContext.logWarning("UNEXCLUDING class \"" + this.formatClassForDisplay(classNode) + "\" for name obfuscation because \"" + reason + "\"");
			} else if (super.scriptContext.isVerbose()) {
				super.logWriter.println("\tUnexcluding class \"" + this.formatClassForDisplay(classNode) + "\" for name obfuscation because of \"" + reason + "\"");
			}
		}
	}

	public boolean isInMethodGroup(AbstractMethodNode methodNode) {
		return this.methodUnionFind.contains(methodNode);
	}

	public void processPackageChangeLog(ChangeLogContext changeLogContext, FixedClassManager fixedClassManager) {
		if (changeLogContext != null) {
			ArrayList<String> renamedPackages = new ArrayList<>();
			ArrayList<String> unchangedPackages = new ArrayList<>();

			for (String excludedPackageName : this.excludedPackages.keySet()) {
				if (changeLogContext.hasOldPackageName(excludedPackageName)) {
					if (changeLogContext.hasNewPackageName(excludedPackageName)) {
						renamedPackages.add(excludedPackageName);
					}
				} else {
					unchangedPackages.add(excludedPackageName);
				}
			}

			Collections.sort(renamedPackages);
			Collections.sort(unchangedPackages, Collections.reverseOrder());

			for (int renamedPackageIndex = 0; renamedPackageIndex < renamedPackages.size(); renamedPackageIndex++) {
				String renamedPackageName = renamedPackages.get(renamedPackageIndex);
				this.unexcludePackageWithWarning(renamedPackageName, changeLogContext, fixedClassManager, "Input ChangeLog specifies a new name for package '" + GlobalUtils.internalToBinaryName(renamedPackageName) + "'", unchangedPackages);
			}
		}
	}
}
