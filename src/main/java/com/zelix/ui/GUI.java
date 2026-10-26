package com.zelix.ui;

import com.zelix.HelpManager;
import com.zelix.UIConfig;
import com.zelix.enums.MixCaseType;
import com.zelix.event.*;
import com.zelix.exceptions.ProcessingException;
import com.zelix.framework.MemberNode;
import com.zelix.framework.StructureNode;
import com.zelix.framework.constant.ConstantNode;
import com.zelix.framework.constant.StringNode;
import com.zelix.framework.reference.ReferenceGraph;
import com.zelix.framework.signature.MethodSignature;
import com.zelix.framework.tree.ArchiveNode;
import com.zelix.framework.tree.ClasspathResource;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.wrappers.FieldNode;
import com.zelix.framework.wrappers.MethodNode;
import com.zelix.framework.wrappers.property.ClassConstantsEntry;
import com.zelix.framework.wrappers.property.ClassMemberEntry;
import com.zelix.framework.writer.base.ConstantWriter;
import com.zelix.klassmaster.KlassMaster;
import com.zelix.klassmaster.config.ObfuscateConfig;
import com.zelix.klassmaster.config.Options;
import com.zelix.klassmaster.config.TrimConfig;
import com.zelix.klassmaster.constant.CopyrightConstants;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.statements.base.ExclusionInclusionStatement;
import com.zelix.klassmaster.script.statements.exclusion.*;
import com.zelix.logging.AbstractLogger;
import com.zelix.logging.GuiLogger;
import com.zelix.logging.GuiLoggerBase;
import com.zelix.repository.ClassLoadingStatus;
import com.zelix.repository.ClasspathManager;
import com.zelix.ui.base.BaseFrame;
import com.zelix.ui.components.ClassList;
import com.zelix.ui.components.PlatformAdaptiveList;
import com.zelix.ui.components.PlatformAdaptiveScrollPanel;
import com.zelix.ui.components.cell.ListCellRenderer;
import com.zelix.ui.components.panels.*;
import com.zelix.ui.dataset.ClassNodeEntry;
import com.zelix.ui.dialogs.*;
import com.zelix.ui.frames.OpenClassesFrame;
import com.zelix.ui.layouts.PowerLayout;
import com.zelix.ui.layouts.SplitLayout;
import com.zelix.ui.process.*;
import com.zelix.ui.prompts.*;
import com.zelix.ui.utils.GuiUtils;
import com.zelix.utils.DecompileUtils;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.NoDuplicatePrintWriter;
import com.zelix.utils.dataset.AtomicInt;
import com.zelix.utils.dataset.ChangeLogFileData;
import com.zelix.utils.function.OptionalConsumer;
import com.zelix.utils.function.ZKMConsumer;
import com.zelix.utils.references.AtomicReference;
import com.zelix.workspace.ClassHierarchyNode;
import com.zelix.workspace.PackageManager;
import com.zelix.workspace.Workspace;
import lombok.SneakyThrows;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.Theme;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.util.*;
import java.util.List;

public class GUI extends BaseFrame implements ReferenceChangeListener, ResourceChangedListener {
	public static String[] layoutStyles = new String[]{
		"mainPnl.top=0",
		"mainPnl.left=0",
		"mainPnl.right=container.right",
		"mainPnl.bottom=container.bottom-statusPnl.height",
		"statusPnl.left=0",
		"statusPnl.top=mainPnl.bottom",
		"statusPnl.right=container.right"
	};
	public static String[] classPropertiesStyles = new String[]{
		"classPropertiesPnl.width=container.width*33/100",
		"classPropertiesPnl.height=container.height",
		"classPropertiesPnl.top=0",
		"classPropertiesPnl.left=0",
		"secondaryPnl.width=container.width*33/100",
		"secondaryPnl.height=container.height",
		"secondaryPnl.top=0",
		"secondaryPnl.left=container.width*33/100",
		"tertiaryPnl.width=container.width*34/100",
		"tertiaryPnl.height=container.height",
		"tertiaryPnl.top=0",
		"tertiaryPnl.left=container.width*66/100"
	};
	private final JMenuBar menu = new JMenuBar();
	private final Font font = KlassMaster.getFont();
	private final ObservableReference propertiesListStatus = new ObservableReference();
	private final ObservableReference codeViewStatus = new ObservableReference();
	private final EventRegistry eventRegistry = new EventRegistry();
	private final Workspace workspace;
	private final AtomicReference<String> workspaceStatus;
	private final ClasspathManager classpathManager;
	private final String logFile;
	private final PrintWriter logWriter;
	private final UIConfig config;
	private final PropertyListListener propertyListListener;
	private final CodeViewerListener codeViewerListener;
	private JMenuItem zkmOverview;
	private JMenuItem buildHelper;
	private JMenuItem stacktraceTranslate;
	private JMenuItem saveAll;
	private int selectedClassIndex;
	private JMenuItem garbageCollect;
	private JMenuItem about;
	private BorderedPanel secondaryPnl;
	private PlatformAdaptiveList<String> propertiesList;
	private JMenuItem obfuscate;
	private JPanel viewPanel;
	private ListSelectionEvent lastPropertySelectionEvent;
	private JMenuItem scriptHelper;
	private JMenuItem browserHelp;
	private JTextArea codeView;
	private JMenuItem configTranslate;
	private JMenuItem classpath;
	private ClassList<ListCellRenderer> classList;
	private BorderedPanel codeViewWrapperPanel;
	private JLabel statusLabel;
	private MemberNode currentEditingMember;
	private JMenuItem open;
	private BorderedPanel classListPanel;
	private BorderedPanel tertiaryPnl;
	private JPanel mainPanel;
	private JPanel classPropertiesPanel;
	private JMenuItem trim;
	private int selectedPropertyIndex;
	private PlatformAdaptiveList<String> membersList;
	private PlatformAdaptiveScrollPanel codeViewPanel;
	private BorderedPanel statusPanel;
	private boolean disabled;
	private BorderedPanel classPropertiesPnl;
	private JMenuItem exit;

	public GUI(
		String title,
		Workspace workspace,
		AtomicReference<String> workspaceStatus,
		ClasspathManager classpathManager,
		String logFile,
		PrintWriter logWriter,
		UIConfig config
	) {
		this.workspace = workspace;
		this.workspaceStatus = workspaceStatus;
		this.classpathManager = classpathManager;
		this.logFile = logFile;
		this.logWriter = logWriter;
		this.config = config;
		workspaceStatus.subscribe(this);
		this.initGui(title);
		this.setupWaitCursorSafety();
		this.propertyListListener = new PropertyListListener(this.propertiesListStatus, this.propertiesList);
		this.codeViewerListener = new CodeViewerListener(this.codeViewStatus, this.codeView, this.codeViewPanel, this);
	}

	public void initGui(String title) {
		Container mainContentContainer = this.getContentPane();
		PowerLayout mainLayout = new PowerLayout(mainContentContainer);
		mainContentContainer.setLayout(mainLayout);
		mainContentContainer.setFont(this.font);
		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent event) {
				System.gc();
			}

			@Override
			public void windowClosing(WindowEvent event) {
				GUI.this.close();
			}
		});
		this.mainPanel = new JPanel();
		this.viewPanel = new JPanel();
		this.classPropertiesPanel = new JPanel();
		this.statusPanel = new BorderedPanel(false);
		this.statusPanel.setLayout(new BorderLayout());
		this.classListPanel = new BorderedPanel(false);
		this.classPropertiesPnl = new BorderedPanel(false);
		this.secondaryPnl = new BorderedPanel(false);
		this.tertiaryPnl = new BorderedPanel(false);
		this.codeViewWrapperPanel = new BorderedPanel(false);
		mainContentContainer.add(this.mainPanel, "mainPnl");
		mainContentContainer.add(this.statusPanel, "statusPnl");
		mainLayout.setProperties(layoutStyles);
		this.mainPanel.setLayout(new SplitLayout(0, 25, false, 4));
		this.mainPanel.add(this.classListPanel);
		this.mainPanel.add(this.viewPanel);
		this.viewPanel.setLayout(new SplitLayout(1, 40, false, 4));
		this.viewPanel.add(this.classPropertiesPanel);
		this.viewPanel.add(this.codeViewWrapperPanel);
		PowerLayout classPropertiesPanelLayout = new PowerLayout(this.classPropertiesPanel);
		this.classPropertiesPanel.setLayout(classPropertiesPanelLayout);
		this.classPropertiesPanel.add(this.classPropertiesPnl, "classPropertiesPnl");
		this.classPropertiesPanel.add(this.secondaryPnl, "secondaryPnl");
		this.classPropertiesPanel.add(this.tertiaryPnl, "tertiaryPnl");
		classPropertiesPanelLayout.setProperties(classPropertiesStyles);
		this.classListPanel.setLayout(new BorderLayout());
		this.classList = new ClassList<>(new DefaultListModel<>());
		this.classList.setSelectionMode(0);
		this.classList.setToolTipText(HelpManager.getToolTip("BROWSER_CLASS_HIERARCHY"));
		this.classList.addListSelectionListener(event -> {
			PlatformAdaptiveList<?> list = (PlatformAdaptiveList<?>)event.getSource();
			if (!event.getValueIsAdjusting()) {
				int index = list.getSelectedIndex();
				if (index > -1) {
					this.setClassListIndex(index);
				}
			}
		});
		this.classListPanel.add(new PlatformAdaptiveScrollPanel(this.classList), "Center");
		this.classPropertiesPnl.setLayout(new BorderLayout());
		this.propertiesList = new PlatformAdaptiveList<>(new DefaultListModel<>());
		this.propertiesList.setSelectionMode(0);
		this.propertiesList.setToolTipText(HelpManager.getToolTip("BROWSER_CLASS_PROPERTIES"));
		this.propertiesList.addListSelectionListener(event -> {
			if (!event.getValueIsAdjusting()) {
				PlatformAdaptiveList<?> list = (PlatformAdaptiveList<?>)event.getSource();
				if (list.getSelectedIndex() > -1) {
					this.onPropertySelected(list.getSelectedIndex(), event);
				}
			}
		});
		this.classPropertiesPnl.add(new PlatformAdaptiveScrollPanel(this.propertiesList), "Center");
		this.secondaryPnl.setLayout(new BorderLayout());
		this.tertiaryPnl.setLayout(new BorderLayout());
		this.codeViewWrapperPanel.setLayout(new BorderLayout());
		this.codeView = new JTextArea();
		if (Options.ENABLE_PATCHES) {
			RSyntaxTextArea syntaxTextArea = new RSyntaxTextArea();
			syntaxTextArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
			syntaxTextArea.setEditable(false);

			try {
				Theme.load(Theme.class.getResourceAsStream("/com/zelix/eclipse.xml")).apply(syntaxTextArea);
			} catch (IOException themeLoadException) {
				themeLoadException.printStackTrace();
			}

			this.codeView = syntaxTextArea;
		}

		this.codeViewPanel = new PlatformAdaptiveScrollPanel(this.codeView);
		this.codeViewWrapperPanel.add(this.codeViewPanel, "Center");
		this.statusLabel = new JLabel("Status");
		this.statusPanel.add(this.statusLabel, "Center");
		ActionListener menuListener = this::handleMenuAction;
		int menuShortcutKeyMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
		JMenu fileMenu = new JMenu("File");
		fileMenu.setMnemonic('f');
		this.applyMenuIcon(fileMenu, "file");
		this.open = new JMenuItem("Open...");
		this.open.setMnemonic('o');
		this.open.setAccelerator(KeyStroke.getKeyStroke(79, menuShortcutKeyMask));
		this.open.addActionListener(menuListener);
		this.applyMenuIcon(this.open, "open");
		fileMenu.add(this.open);
		this.saveAll = new JMenuItem("Save all...");
		this.saveAll.setMnemonic('s');
		this.saveAll.setAccelerator(KeyStroke.getKeyStroke(83, menuShortcutKeyMask));
		this.saveAll.addActionListener(menuListener);
		this.applyMenuIcon(this.saveAll, "save");
		fileMenu.add(this.saveAll);
		this.exit = new JMenuItem("Exit");
		this.exit.setMnemonic('e');
		this.exit.setAccelerator(KeyStroke.getKeyStroke(69, menuShortcutKeyMask));
		this.exit.addActionListener(menuListener);
		this.applyMenuIcon(this.exit, "exit");
		fileMenu.add(this.exit);
		this.menu.add(fileMenu);
		JMenu toolsMenu = new JMenu("Tools");
		toolsMenu.setMnemonic('t');
		this.applyMenuIcon(toolsMenu, "tools");
		this.trim = new JMenuItem("Trim...");
		this.trim.setMnemonic('t');
		this.trim.setAccelerator(KeyStroke.getKeyStroke(84, menuShortcutKeyMask));
		this.trim.addActionListener(menuListener);
		this.applyMenuIcon(this.trim, "trim");
		toolsMenu.add(this.trim);
		this.obfuscate = new JMenuItem("Obfuscate...");
		this.obfuscate.setMnemonic('b');
		this.obfuscate.setAccelerator(KeyStroke.getKeyStroke(66, menuShortcutKeyMask));
		this.obfuscate.addActionListener(menuListener);
		this.applyMenuIcon(this.obfuscate, "obfuscate");
		toolsMenu.add(this.obfuscate);
		this.buildHelper = new JMenuItem("Build Helper...");
		this.buildHelper.setMnemonic('u');
		this.buildHelper.setAccelerator(KeyStroke.getKeyStroke(85, menuShortcutKeyMask));
		this.buildHelper.addActionListener(menuListener);
		this.applyMenuIcon(this.buildHelper, "buildHelper");
		toolsMenu.add(this.buildHelper);
		this.scriptHelper = new JMenuItem("ZKM Script Helper...");
		this.scriptHelper.setMnemonic('z');
		this.scriptHelper.setAccelerator(KeyStroke.getKeyStroke(90, menuShortcutKeyMask));
		this.scriptHelper.addActionListener(menuListener);
		this.applyMenuIcon(this.scriptHelper, "scriptHelper");
		toolsMenu.add(this.scriptHelper);
		this.stacktraceTranslate = new JMenuItem("Stack Trace translate...");
		this.stacktraceTranslate.setMnemonic('c');
		this.stacktraceTranslate.setAccelerator(KeyStroke.getKeyStroke(67, menuShortcutKeyMask));
		this.stacktraceTranslate.addActionListener(menuListener);
		this.applyMenuIcon(this.stacktraceTranslate, "stacktrace");
		toolsMenu.add(this.stacktraceTranslate);
		this.configTranslate = new JMenuItem("ProGuard configuration translate...");
		this.configTranslate.setMnemonic('d');
		this.configTranslate.setAccelerator(KeyStroke.getKeyStroke(68, menuShortcutKeyMask));
		this.configTranslate.addActionListener(menuListener);
		this.applyMenuIcon(this.configTranslate, "configTranslate");
		toolsMenu.add(this.configTranslate);
		this.menu.add(toolsMenu);
		JMenu optionsMenu = new JMenu("Options");
		optionsMenu.setMnemonic('o');
		this.applyMenuIcon(optionsMenu, "options");
		this.classpath = new JMenuItem("Classpath...");
		this.classpath.setMnemonic('p');
		this.classpath.setAccelerator(KeyStroke.getKeyStroke(80, menuShortcutKeyMask));
		this.classpath.addActionListener(menuListener);
		this.applyMenuIcon(this.classpath, "classpath");
		optionsMenu.add(this.classpath);
		this.garbageCollect = new JMenuItem("Garbage Collect");
		this.garbageCollect.setMnemonic('g');
		this.garbageCollect.setAccelerator(KeyStroke.getKeyStroke(71, menuShortcutKeyMask));
		this.garbageCollect.addActionListener(menuListener);
		this.applyMenuIcon(this.garbageCollect, "gc");
		optionsMenu.add(this.garbageCollect);
		this.menu.add(optionsMenu);
		JMenu helpMenu = new JMenu("Help");
		helpMenu.setMnemonic('h');
		this.applyMenuIcon(helpMenu, "help");
		this.browserHelp = new JMenuItem("Browser help");
		this.browserHelp.setMnemonic('r');
		this.browserHelp.setAccelerator(KeyStroke.getKeyStroke(82, menuShortcutKeyMask));
		this.browserHelp.addActionListener(menuListener);
		this.applyMenuIcon(this.browserHelp, "browserHelp");
		helpMenu.add(this.browserHelp);
		this.zkmOverview = new JMenuItem("ZKM Overview");
		this.zkmOverview.setMnemonic('k');
		this.zkmOverview.setAccelerator(KeyStroke.getKeyStroke(75, menuShortcutKeyMask));
		this.zkmOverview.addActionListener(menuListener);
		this.applyMenuIcon(this.zkmOverview, "overview");
		helpMenu.add(this.zkmOverview);
		this.about = new JMenuItem("About");
		this.about.setMnemonic('a');
		this.about.setAccelerator(KeyStroke.getKeyStroke(65, menuShortcutKeyMask));
		this.about.addActionListener(menuListener);
		this.applyMenuIcon(this.about, "about");
		helpMenu.add(this.about);
		this.menu.add(helpMenu);
		this.setJMenuBar(this.menu);
		this.setTitle(title);
		this.setResizable(true);
		this.setIconImage(KlassMaster.getIcon(this));
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		this.setSize(Math.min(635, screenSize.width), Math.min(475, screenSize.height - 50));
		Dimension newSize = this.getSize();
		int centerX = screenSize.width / 2 - newSize.width / 2;
		int centerY = screenSize.height / 2 - newSize.height / 2;
		this.setLocation(centerX, centerY);
	}

	public void openClasspathDialog() {
		ClasspathChangeListener classpathChangeListener = new ClasspathChangeListener(this);
		this.setDisable(true);
		new ClasspathFileListDialog(
			this,
			"Classpath",
			this.classpathManager,
			HelpManager.getToolTip("CLASSPATH_MSG"),
			"OK",
			HelpManager.getToolTip("SET_CLASSPATH"),
			this.config,
			classpathChangeListener
		);
	}

	public PackageManager getPackageManager() {
		return this.workspace.getPackageManager();
	}

	public ScriptContext createScript() {
		return new ScriptContext(this.workspace, this.classpathManager, true, this.logFile, this.logWriter);
	}

	private void applyMenuIcon(JMenuItem menuItem, String iconName) {
		if (Options.ENABLE_PATCHES) {
			menuItem.setIcon(new ImageIcon(Objects.requireNonNull(GUI.class.getResource("/icons/" + iconName + ".png"))));
		}
	}

	public void setDisable(boolean disabled) {
		this.disabled = disabled;
	}

	public void onClassesUpdate(
		ClasspathResource[] classes,
		ArchiveNode[] archiveNodes,
		ClasspathResource[] jadFiles,
		ClasspathResource[] allClasspathResources,
		AtomicReference<String> runningPath,
		Boolean openNestedArchives,
		Set<ArchiveNode> nestedArchives,
		ClassLoadingStatus classLoadingStatus
	) {
		String lastRunningPath = runningPath.get();
		this.config.setLastRunningPath(lastRunningPath);
		this.updateClassList(classes, archiveNodes, jadFiles, allClasspathResources, openNestedArchives, nestedArchives, classLoadingStatus);
	}

	public void setupWaitCursorSafety() {
		if (SwingUtilities.isEventDispatchThread()) {
			this.setWaitCursor();
		} else {
			SwingUtilities.invokeLater(this::setWaitCursor);
		}
	}

	public ClasspathManager getClasspathManager() {
		return this.classpathManager;
	}

	public void init() {
		if (!this.classpathManager.isBootClasspathInitialized()) {
			String bootClassPath = SelectBootstrapRuntimePrompt.selectBootstrapClassPath(
				this, CopyrightConstants.NAME + " - Select Java Bootstrap classes path", this.classpathManager.getRawClasspath()
			);
			if (bootClassPath != null && ClasspathManager.archiveContainsObject(bootClassPath)) {
				this.config.setLastClassPath(bootClassPath);
				this.config.saveConfig();
				this.classpathManager.appendClasspath(bootClassPath);
			}
		}

		InitialMethodListener initialMethodListener = new InitialMethodListener(this);
		this.setupWaitCursorSafety();
		this.setDisable(true);
		new InitialHelperDialog(this, "Initial Helper Dialog", this.config, initialMethodListener);
	}

	public void openEditConstantPanel(ConstantWriter constantWriter, ReferenceGraph<StringNode, StringNode> referenceGraph) {
		this.setupWaitCursorSafety();
		this.tertiaryPnl.removeAll();
		ConstantEditPanel constantEditPanel = new ConstantEditPanel(constantWriter, this.workspace, this.membersList, this, this.eventRegistry, referenceGraph);
		this.resetCursorSafety();
		this.tertiaryPnl.add(constantEditPanel, "Center");
		this.tertiaryPnl.validate();
	}

	public void setClassListIndex(int index) {
		if (this.disabled) {
			if (this.selectedClassIndex > -1 && this.selectedClassIndex != index) {
				this.classList.setSelectedIndex(this.selectedClassIndex);
				this.classList.ensureIndexIsVisible(this.selectedClassIndex);
			}
		} else if (index != this.selectedClassIndex) {
			this.eventRegistry.clearAll();
			this.tertiaryPnl.removeAll();
			this.selectedClassIndex = index;
			ClassNode classNode = this.workspace.getClassHierarchyNode(index).getClassNodeIfWorkspace();
			this.propertiesListStatus.updateSubscription(classNode);
			this.codeViewStatus.updateSubscription(classNode);
			if (classNode != null) {
				if (this.lastPropertySelectionEvent != null) {
					this.propertiesList.setSelectedIndex(this.selectedPropertyIndex);
					this.propertiesList.ensureIndexIsVisible(this.selectedPropertyIndex);
				}
			} else {
				this.secondaryPnl.removeAll();
			}
		}
	}

	public void saveAll(File outputFile, ZKMConsumer completionCallback) {
		this.config.setLastSavingPath(outputFile.getAbsolutePath());
		this.config.saveConfig();
		if (this.workspace.isEmpty()) {
			new CopyablePrompt(this, "No classes opened", "There are no classes to save. Use the \"File | Open\" menu to open your classes.");
			this.setDisable(false);
		} else if (!this.workspace.isErrorEmpty()) {
			new ErrorPrompt(
				this,
				"Opened classes not useable",
				"Opened classes not useable for the reason shown below.",
				this.workspace.getError() + " : Please adjust your " + CopyrightConstants.NAME + " classpath or class file selections."
			);
			this.setDisable(false);
		} else {
			this.setDisable(true);
			this.setupWaitCursorSafety();
			ScriptContext script = this.createScript();
			GuiLoggerBase logger = new GuiLoggerBase(this, script);
			ZKMConsumer writeCallback = new OptionalConsumer() {
				@Override
				public void accept() {
					GUI.this.resetCursorSafety();
					if (completionCallback == null) {
						GUI.this.setDisable(false);
					}
				}
			};
			new Thread(() -> {
				this.workspace.writeWorkspace(1, false, false, null, outputFile, logger, script, writeCallback);
				if (completionCallback != null) {
					completionCallback.accept();
				}
			}).start();
		}
	}

	public void openAccessEditPanel(MemberNode memberNode) {
		this.setupWaitCursorSafety();
		this.eventRegistry.unsubscribeGroup("DescListObservers");
		this.tertiaryPnl.removeAll();
		ChangeListener accessEditPanel = null;
		if (memberNode instanceof MethodNode) {
			MethodNode method = (MethodNode)memberNode;
			accessEditPanel = new MethodAccessEditPanel(method, this.workspace, this.membersList, this, this.eventRegistry);
			this.codeViewStatus.updateSubscription(method);
		} else if (memberNode instanceof FieldNode) {
			FieldNode field = (FieldNode)memberNode;
			accessEditPanel = new FieldAccessEditPanel(field, this.workspace, this.membersList, this, this.eventRegistry);
			this.codeViewStatus.updateSubscription(field);
		}

		this.currentEditingMember = memberNode;
		memberNode.onChange(accessEditPanel);
		this.resetCursorSafety();
		this.tertiaryPnl.add((Component)accessEditPanel, "Center");
		this.tertiaryPnl.validate();
	}

	public void handleMenuAction(ActionEvent event) {
		JMenuItem menuItem = (JMenuItem)event.getSource();
		if (menuItem == this.exit) {
			this.close();
		}

		if (!this.disabled) {
			if (menuItem == this.open) {
				this.openOpenDialog();
			} else if (menuItem == this.saveAll) {
				SaveCallbackListener saveCallbackListener = new SaveCallbackListener(this);
				this.setDisable(true);
				new SavingDialog(
					this, CopyrightConstants.NAME + " - Select a Folder", "OK", HelpManager.getToolTip("SAVE_CLASSES"), this.config.getLastSavingPath(), saveCallbackListener
				);
			} else if (menuItem == this.trim) {
				if (this.workspace.isClasspathChanged()) {
					new CopyablePrompt(this, "Classpath changed", "Classpath has been changed but the classes have not been reopened. You must reopen the classes. (D)");
					return;
				}

				ScriptContext scriptContext = this.createScript();
				new Trim(this, this.workspace, scriptContext);
			} else if (menuItem == this.obfuscate) {
				if (this.workspace.isClasspathChanged()) {
					new CopyablePrompt(this, "Classpath changed", "Classpath has been changed but the classes have not been reopened. You must reopen the classes. (E)");
					return;
				}

				ScriptContext obfuscateScript = this.createScript();

				try {
					new Obfuscate(this, this.workspace.getPackageManager(), obfuscateScript);
				} catch (Throwable obfuscationThrowable) {
					obfuscationThrowable.printStackTrace();
					this.setEnabled(true);
				}
			} else if (menuItem == this.buildHelper) {
				ScriptContext buildHelperScript = this.createScript();
				new BuildHelper(this, buildHelperScript, this.config);
			} else if (menuItem == this.scriptHelper) {
				ScriptContext scriptHelperScript = this.createScript();
				new ZKMScriptHelper(this, scriptHelperScript, this.config);
			} else if (menuItem == this.stacktraceTranslate) {
				this.setDisable(true);
				new StacktraceTranslatorDialog(this, CopyrightConstants.NAME + " - Stack Trace Translate", this.config);
			} else if (menuItem == this.configTranslate) {
				this.setDisable(true);
				new ProguardTranslatorDialog(this, CopyrightConstants.NAME + " - ProGuard Configuration Translate", this.config);
			} else if (menuItem == this.classpath) {
				this.openClasspathDialog();
			} else if (menuItem == this.about) {
				new AboutPrompt(this, "About " + CopyrightConstants.NAME + KlassMaster.SUFFIX, this.workspace.getKlassMaster());
			} else if (menuItem == this.browserHelp) {
				HelpManager.showHelp("016");
			} else if (menuItem == this.zkmOverview) {
				HelpManager.showHelp("015");
			} else if (menuItem == this.garbageCollect) {
				Runtime runtime = Runtime.getRuntime();
				int kBytes = (int)((runtime.totalMemory() - runtime.freeMemory()) / 1024L);
				this.workspaceStatus.set(kBytes + "K in use before garbage collection");
				runtime.gc();
			}
		}
	}

	public Workspace getWorkspace() {
		return this.workspace;
	}

	@Override
	public void onChanged(INotifier notifier, Object changeCode, Object changedObject, Object unused) {
		if (notifier == this.workspace) {
			Enumeration<ClassHierarchyNode> allClassesEnumeration = this.workspace.getAllClassesEnumeration();
			DefaultListModel<ListCellRenderer> classListModel = new DefaultListModel<>();
			int newIndex = -1;
			int classIndex = 0;

			ClassNode classNode;
			for (classNode = (ClassNode)this.propertiesListStatus.getNotifier(); allClassesEnumeration.hasMoreElements(); classIndex++) {
				ClassHierarchyNode classHierarchyNode = allClassesEnumeration.nextElement();
				if (classNode != null && classHierarchyNode.getClassNodeIfWorkspace() == classNode) {
					newIndex = classIndex;
				}

				String className = classHierarchyNode.getName();
				StringBuffer indentBuffer = new StringBuffer();
				int depth = classHierarchyNode.getDepth();

				for (int i = 0; i < depth; i++) {
					indentBuffer.append("      ");
				}

				ListCellRenderer renderer = new ListCellRenderer(indentBuffer + className, !classHierarchyNode.isWorkspaceClass());
				classListModel.addElement(renderer);
			}

			this.classList.setModel(classListModel);
			if (newIndex != -1) {
				this.classList.setSelectedIndex(newIndex);
				this.classList.ensureIndexIsVisible(newIndex);
			} else if (classNode != null) {
				this.selectedClassIndex = -1;
				this.propertiesListStatus.updateSubscription(null);
				this.codeViewStatus.updateSubscription(null);
				this.secondaryPnl.removeAll();
				this.tertiaryPnl.removeAll();
				this.eventRegistry.clearAll();
			}

			if (this.classList.getModel().getSize() > 0 && this.classList.getSelectedIndex() == -1) {
				this.selectedClassIndex = 0;
				this.classList.setSelectedIndex(0);
				this.classList.ensureIndexIsVisible(0);
			}

			this.classListPanel.validate();
		} else if (notifier == this.workspaceStatus && this.statusLabel != null) {
			String newStatus = (String)changeCode;
			if (newStatus.length() != 0) {
				this.statusLabel.setText(newStatus);
			} else {
				this.statusLabel.setText(" ");
			}
		} else if (notifier instanceof ClassMemberEntry) {
			int selectedIndex = this.membersList.getSelectedIndex();
			ClassMemberEntry memberEntry = (ClassMemberEntry)notifier;
			if (changeCode == null) {
				memberEntry.resetIndex();
				DefaultListModel<String> memberNameModel = new DefaultListModel<>();

				while (memberEntry.hasMoreMembers()) {
					memberNameModel.addElement(memberEntry.getNextMemberName());
				}

				this.membersList.setModel(memberNameModel);
				if (selectedIndex != -1) {
					int editingMemberIndex = memberEntry.findMemberIndex(this.currentEditingMember);
					if (editingMemberIndex > -1) {
						this.membersList.setSelectedIndex(editingMemberIndex);
						this.membersList.ensureIndexIsVisible(editingMemberIndex);
					} else {
						this.currentEditingMember = null;
						this.tertiaryPnl.removeAll();
						this.codeViewStatus.updateSubscription(this.propertiesListStatus.getNotifier());
					}
				}

				this.secondaryPnl.validate();
			} else if (changeCode instanceof AtomicInt
				&& ((AtomicInt)changeCode).get() == 0
				&& (this.selectedPropertyIndex == 1 && changedObject instanceof FieldNode || this.selectedPropertyIndex == 2 && changedObject instanceof MethodNode)) {
				MemberNode memberNode = (MemberNode)changedObject;
				int changedMemberIndex = memberEntry.findMemberIndex(memberNode);
				if (changedMemberIndex != -1) {
					((DefaultListModel)this.membersList.getModel()).setElementAt(memberNode.getNameDisplay(), changedMemberIndex);
				}

				if (selectedIndex != -1) {
					this.membersList.setSelectedIndex(selectedIndex);
					this.membersList.ensureIndexIsVisible(selectedIndex);
				}
			}
		} else if (notifier instanceof ClassConstantsEntry) {
			ClassConstantsEntry constantsEntry = (ClassConstantsEntry)notifier;
			int selectedConstantIndex = this.membersList.getSelectedIndex();
			int previousMemberCount = this.membersList.getModel().getSize();
			if (changeCode == null) {
				constantsEntry.resetIndex();
				boolean memberCountReduced = previousMemberCount > constantsEntry.getConstantCount();
				DefaultListModel<String> constantNameModel = new DefaultListModel<>();

				while (constantsEntry.hasMoreConstants()) {
					constantNameModel.addElement(constantsEntry.getNextConstantName());
				}

				this.membersList.setModel(constantNameModel);
				if (selectedConstantIndex != -1 && !memberCountReduced) {
					this.membersList.setSelectedIndex(selectedConstantIndex);
				}

				this.secondaryPnl.validate();
				if (memberCountReduced) {
					this.tertiaryPnl.removeAll();
				}
			} else if (changeCode instanceof AtomicInt && ((AtomicInt)changeCode).get() == 3) {
				ConstantNode constantNode = (ConstantNode)changedObject;
				int changedConstantIndex = constantsEntry.findConstantIndex(constantNode);
				((DefaultListModel)this.membersList.getModel()).setElementAt(constantNode.getDisplayValue(), changedConstantIndex);
				this.membersList.setSelectedIndex(selectedConstantIndex);
				this.membersList.ensureIndexIsVisible(selectedConstantIndex);
			}
		}
	}

	public void trim(List<String> excludeParams, TrimConfig trimConfig, ScriptContext scriptContext, ClassLoadingStatus classLoadingStatus) throws IOException {
		if (this.workspace.isEmpty()) {
			new CopyablePrompt(this, "No classes opened", "There are no classes to trim. Use the \"File | Open\" menu to open your classes.");
		} else if (!this.workspace.hasClasses()) {
			new CopyablePrompt(
				this, "Only module-info.class files opened", "The are no classes other than module-info to trim. Use the \"File | Open\" menu to open your classes."
			);
		} else if (!this.workspace.isErrorEmpty()) {
			new ErrorPrompt(
				this,
				"Opened classes not useable",
				"Opened classes not useable for the reason shown below.",
				this.workspace.getError() + " : Please adjust your " + CopyrightConstants.NAME + " classpath or class file selections."
			);
		} else if (this.workspace.isClasspathChanged()) {
			new CopyablePrompt(this, "Classpath changed", "Classpath has been changed but the classes have not been reopened. You must reopen the classes. (G)");
		} else {
			Vector<ExclusionInclusionStatement> excludeStatements = new Vector<>(1);
			String excludeStatementText = TrimExcludeStatement.buildStatementText(excludeParams, 12);
			if (excludeStatementText != null) {
				ExclusionInclusionStatement excludeStatement;
				try {
					excludeStatement = TrimExcludeStatement.parseTrimExcludeStatement(excludeStatementText, scriptContext);
				} catch (ProcessingException excludeParseException) {
					if (classLoadingStatus != null) {
						classLoadingStatus.setStatus(false, null);
					}

					scriptContext.logWarning(excludeParseException.getMessage());
					new CopyablePrompt(this, "Error", "Unexpected error in exclude parameters. (3) See the log for more detail.");
					if (classLoadingStatus != null) {
						classLoadingStatus.accept();
					}

					return;
				}

				excludeStatements.addElement(excludeStatement);
			}

			this.setupWaitCursorSafety();
			this.setDisable(true);
			GuiLoggerBase logger = new GuiLoggerBase(this, scriptContext);
			TrimListener trimListener = new TrimListener(this, classLoadingStatus);
			TrimTask trimTask = new TrimTask(this, classLoadingStatus, logger, trimConfig, excludeStatements, trimListener, scriptContext);
			new Thread(trimTask).start();
		}
	}

	private void updateClassList(
		ClasspathResource[] classes,
		ArchiveNode[] archiveNodes,
		ClasspathResource[] jadFiles,
		ClasspathResource[] allClasspathResources,
		Boolean openNestedArchives,
		Set<ArchiveNode> nestedArchives,
		ClassLoadingStatus classLoadingStatus
	) {
		boolean nestedArchivesEnabled = openNestedArchives;
		this.config.setOpenNestedArchives(nestedArchivesEnabled);
		this.config.saveConfig();
		ClasspathResource[] yamlFiles = new ClasspathResource[0];
		ClasspathResource[] propertiesFiles = new ClasspathResource[0];
		GuiUtils.requestFocusInWindow(this);
		if (classes != null) {
			this.propertiesListStatus.updateSubscription(null);
			this.codeViewStatus.updateSubscription(null);
			this.secondaryPnl.removeAll();
			this.tertiaryPnl.removeAll();
			this.setupWaitCursorSafety();
			GuiLogger logger = new GuiLogger(this, this, this.createScript());
			ClassLoadingListener classLoadingListener = new ClassLoadingListener(this, classLoadingStatus);
			LoadClassesTask loadClassesTask = new LoadClassesTask(
				this, logger, classes, archiveNodes, nestedArchives, jadFiles, allClasspathResources, yamlFiles, propertiesFiles, nestedArchivesEnabled, classLoadingListener, classLoadingStatus
			);
			new Thread(loadClassesTask).start();
		}
	}

	public List<ClassNodeEntry> getMainMethodClasses() {
		List<ClassNodeEntry> classNodeEntries = null;
		Enumeration<ClassNode> mainMethodClasses = this.workspace.getClassesByMethodSignature(MethodSignature.MAIN_METHOD);
		List<ClassNode> appletClasses = this.workspace.findClassesExtending("java/applet/Applet");
		if (mainMethodClasses != null && mainMethodClasses.hasMoreElements()) {
			classNodeEntries = new Vector<>();

			while (mainMethodClasses.hasMoreElements()) {
				classNodeEntries.add(new ClassNodeEntry(mainMethodClasses.nextElement(), true));
			}
		}

		if (appletClasses != null && appletClasses.size() > 0) {
			if (classNodeEntries == null) {
				classNodeEntries = new ArrayList<>(appletClasses.size());
			}

			for (int i = 0; i < appletClasses.size(); i++) {
				classNodeEntries.add(new ClassNodeEntry(appletClasses.get(i), false));
			}
		}

		return classNodeEntries;
	}

	private void setWaitCursor() {
		Cursor waitCursor = new Cursor(3);
		this.setCursor(waitCursor);
		this.classListPanel.setCursor(waitCursor);
		this.classPropertiesPnl.setCursor(waitCursor);
		this.secondaryPnl.setCursor(waitCursor);
		this.tertiaryPnl.setCursor(waitCursor);
		this.codeViewWrapperPanel.setCursor(waitCursor);
		this.classList.setCursor(waitCursor);
		this.propertiesList.setCursor(waitCursor);
		this.codeView.setCursor(waitCursor);
	}

	public void openOpenDialog() {
		UpdateClassesListener updateClassesListener = new UpdateClassesListener(this);
		this.setDisable(true);
		this.setEnabled(false);
		new OpenClassesFrame(this, CopyrightConstants.NAME + " - Open Classes", "OK", true, this.config.getLastRunningPath(), this.config.getOpenNestedArchives(), null, updateClassesListener);
	}

	public void obfuscate(List<String> excludeParams, List<String> obfuscateRefsIncludeParams, ObfuscateConfig obfuscateConfig, ScriptContext scriptContext, ClassLoadingStatus classLoadingStatus) throws IOException {
		this.toFront();
		if (this.workspace.isEmpty()) {
			new CopyablePrompt(this, "No classes opened", "There are no classes to obfuscate. Use the \"File | Open\" menu to open your classes.");
		} else if (!this.workspace.hasClasses()) {
			new CopyablePrompt(
				this, "Only module-info.class files opened", "The are no classes other than module-info to obfuscate. Use the \"File | Open\" menu to open your classes."
			);
		} else if (!this.workspace.isErrorEmpty()) {
			new ErrorPrompt(
				this,
				"Opened classes not useable",
				"Opened classes not useable for the reason shown below.",
				this.workspace.getError() + " : Please adjust your " + CopyrightConstants.NAME + " classpath or class file selections."
			);
		} else if (this.workspace.isClasspathChanged()) {
			new CopyablePrompt(this, "Classpath changed", "Classpath has been changed but the classes have not been reopened. You must reopen the classes. (H)");
		} else {
			Vector<ExclusionInclusionStatement> excludeStatements = new Vector<>(1);
			String excludeStatementText = ExcludeStatement.buildStatementText(excludeParams, 8);
			if (excludeStatementText != null) {
				ExclusionInclusionStatement excludeStatement;
				try {
					excludeStatement = ExcludeStatement.parseExcludeStatement(excludeStatementText, scriptContext);
				} catch (ProcessingException excludeParseException) {
					scriptContext.logWarning(excludeParseException.getMessage());
					new CopyablePrompt(this, "Error", "Unexpected error in exclude parameters. (1) See the log for more detail.");
					if (classLoadingStatus != null) {
						classLoadingStatus.setStatus(false, null);
						classLoadingStatus.accept();
					}

					return;
				}

				excludeStatements.addElement(excludeStatement);
			}

			Vector<ObfuscateReferencesIncludeStatement> obfuscateRefsStatements = new Vector<>(1);
			if (obfuscateRefsIncludeParams != null && obfuscateRefsIncludeParams.size() > 0) {
				String obfuscateRefsStatementText = ObfuscateReferencesIncludeStatement.buildStatementText(obfuscateRefsIncludeParams, 27);
				if (obfuscateRefsStatementText != null) {
					ObfuscateReferencesIncludeStatement obfuscateRefsStatement;
					try {
						obfuscateRefsStatement = ObfuscateReferencesIncludeStatement.parseIncludeStatement(obfuscateRefsStatementText, scriptContext);
					} catch (ProcessingException obfuscateRefsParseException) {
						scriptContext.logWarning(obfuscateRefsParseException.getMessage());
						new CopyablePrompt(this, "Error", "Unexpected error in obfuscateReferencesInclude parameters. (1) See the log for more detail.");
						if (classLoadingStatus != null) {
							classLoadingStatus.setStatus(false, null);
							classLoadingStatus.accept();
						}

						return;
					}

					obfuscateRefsStatements.addElement(obfuscateRefsStatement);
				}
			}

			this.setupWaitCursorSafety();
			GuiLoggerBase logger = new GuiLoggerBase(this, scriptContext);
			ObfuscateTask obfuscateTask = new ObfuscateTask(this, obfuscateConfig, excludeStatements, obfuscateRefsStatements, logger, new OptionalConsumer() {
				@Override
				public void accept() {
					GUI.this.resetCursorSafety();
					GUI.this.workspaceStatus.set(" ");
					if (classLoadingStatus == null) {
						GUI.this.setDisable(false);
					}
				}
			}, scriptContext, classLoadingStatus);
			new Thread(obfuscateTask).start();
		}
	}

	public void resetCursorSafety() {
		if (SwingUtilities.isEventDispatchThread()) {
			this.resetCursor();
		} else {
			SwingUtilities.invokeLater(this::resetCursor);
		}
	}

	@Override
	public void onChange(INotifier notifier, Object changeKind, Object changedNode, Object changeArg) {
		new ResourceChangeDispatcher(this, notifier, changeKind, changedNode, changeArg);
	}

	@Override
	public void close() {
		if (HelpManager.isHelpVisible()) {
			HelpManager.closeHelp();
		}

		super.close();
		if (this.classpathManager != null) {
			this.classpathManager.closeJrtFileSystem();
		}
	}

	public void onPropertySelected(int propertyIndex, ListSelectionEvent event) {
		ClassNode classNode = (ClassNode)this.propertiesListStatus.getNotifier();
		this.selectedPropertyIndex = propertyIndex;
		this.lastPropertySelectionEvent = event;
		this.eventRegistry.clearAll();
		this.secondaryPnl.removeAll();
		this.tertiaryPnl.removeAll();
		if (this.codeViewStatus.getNotifier() instanceof MemberNode) {
			this.codeViewStatus.updateSubscription(this.propertiesListStatus.getNotifier());
		}

		ObservableStructure propertyEntry = classNode.getClassPropertyEntry(propertyIndex);
		switch (propertyIndex) {
			case 0:
				NameEditPanel nameEditPanel = new NameEditPanel((ClassNode)propertyEntry.getNode(), this.workspace, this, this.eventRegistry);
				this.secondaryPnl.add(nameEditPanel, "Center");
				this.secondaryPnl.validate();
				break;
			case 1:
			case 2:
				ClassMemberEntry memberEntry = (ClassMemberEntry)propertyEntry;
				DefaultListModel<String> memberNameModel = new DefaultListModel<>();
				this.membersList = new PlatformAdaptiveList<>(memberNameModel);
				this.membersList.setSelectionMode(0);
				if (propertyIndex == 1) {
					this.membersList.setToolTipText(HelpManager.getToolTip("BROWSER_FIELD_LIST"));
				} else {
					this.membersList.setToolTipText(HelpManager.getToolTip("BROWSER_METHOD_LIST"));
				}

				this.membersList.addListSelectionListener(new GUI.MemberSelectionListener(memberEntry, this));
				this.eventRegistry.subscribeDefault(memberEntry, this);
				memberEntry.onChange(this);
				this.secondaryPnl.add(new PlatformAdaptiveScrollPanel(this.membersList), "Center");
				this.secondaryPnl.validate();
				break;
			case 3:
				ClassConstantsEntry constantsEntry = (ClassConstantsEntry)propertyEntry;
				DefaultListModel<String> constantNameModel = new DefaultListModel<>();
				this.membersList = new PlatformAdaptiveList<>(constantNameModel);
				this.membersList.setSelectionMode(0);
				this.membersList.setToolTipText(HelpManager.getToolTip("BROWSER_CONSTANTS_LIST"));
				this.membersList.addListSelectionListener(new GUI.ConstantSelectionListener(constantsEntry, this));
				this.eventRegistry.subscribeDefault(constantsEntry, this);
				constantsEntry.onChange(this);
				this.secondaryPnl.add(new PlatformAdaptiveScrollPanel(this.membersList), "Center");
				this.secondaryPnl.validate();
		}
	}

	public void resetCursor() {
		Cursor defaultCursor = Cursor.getDefaultCursor();
		this.setCursor(defaultCursor);
		this.classListPanel.setCursor(defaultCursor);
		this.classPropertiesPnl.setCursor(defaultCursor);
		this.secondaryPnl.setCursor(defaultCursor);
		this.tertiaryPnl.setCursor(defaultCursor);
		this.codeViewWrapperPanel.setCursor(defaultCursor);
		this.classList.setCursor(defaultCursor);
		this.propertiesList.setCursor(defaultCursor);
		this.codeView.setCursor(defaultCursor);
	}

	public static void setWaitCursorInternal(GUI gui) {
		gui.setWaitCursor();
	}

	public static Workspace getWorkspaceInternal(GUI gui) {
		return gui.workspace;
	}

	public static AtomicReference<String> getWorkspaceStatus(GUI gui) {
		return gui.workspaceStatus;
	}

	public static void updateClassListInternal(
		GUI gui,
		ClasspathResource[] classes,
		ArchiveNode[] archiveNodes,
		ClasspathResource[] jadFiles,
		ClasspathResource[] allClasspathResources,
		Boolean openNestedArchives,
		Set<ArchiveNode> nestedArchives,
		ClassLoadingStatus classLoadingStatus
	) {
		gui.updateClassList(classes, archiveNodes, jadFiles, allClasspathResources, openNestedArchives, nestedArchives, classLoadingStatus);
	}

	public static UIConfig getConfig(GUI gui) {
		return gui.config;
	}

	public static ObservableReference getPropertiesListStatus(GUI gui) {
		return gui.propertiesListStatus;
	}

	public static PrintWriter getLogWriter(GUI gui) {
		return gui.logWriter;
	}

	public static ObservableReference getCodeViewStatus(GUI gui) {
		return gui.codeViewStatus;
	}

	public static class ClassLoadingListener extends OptionalConsumer {
		public final GUI gui;
		public final ClassLoadingStatus classLoadingStatus;

		public ClassLoadingListener(GUI gui, ClassLoadingStatus classLoadingStatus) {
			this.gui = gui;
			this.classLoadingStatus = classLoadingStatus;
		}

		@Override
		public void accept() {
			this.gui.resetCursorSafety();
			GUI.getWorkspaceStatus(this.gui).set(" ");
			if (this.classLoadingStatus == null) {
				this.gui.setDisable(false);
			}
		}
	}

	public static class ClasspathChangeListener extends OptionalConsumer {
		public final GUI gui;

		public ClasspathChangeListener(GUI gui) {
			this.gui = gui;
		}

		public void onClasspathChanged(ClasspathManager classpathManager, Integer choice) {
			this.gui.setDisable(false);
		}

		@Override
		public void accept(Object arg, Object choice) {
			this.onClasspathChanged((ClasspathManager)arg, (Integer)choice);
		}

		@Override
		public void accept() {
			this.gui.setDisable(false);
		}
	}

	public static class CodeViewerListener implements ChangeListener, ResourceChangedListener {
		public final String lineSeparator = Options.lineSeparator;
		public final String truncationNotice = this.lineSeparator + this.lineSeparator + "Only the first " + 512000 + " bytes shown";
		public ObservableReference codeViewStatus;
		public JTextArea codeView;
		public PlatformAdaptiveScrollPanel codeViewPanel;
		public final GUI gui;

		public CodeViewerListener(ObservableReference codeViewStatus, JTextArea codeView, PlatformAdaptiveScrollPanel codeViewPanel, GUI gui) {
			this.codeViewStatus = codeViewStatus;
			this.codeView = codeView;
			this.codeViewPanel = codeViewPanel;
			this.gui = gui;
			this.codeView.setEditable(false);
			codeViewStatus.subscribe(this);
		}

		private void showDecompiledCode(ClassNode classNode) {
			RSyntaxTextArea syntaxTextArea = (RSyntaxTextArea)this.codeView;
			syntaxTextArea.setCodeFoldingEnabled(false);

			try (
				ByteArrayOutputStream classBytes = new ByteArrayOutputStream();
				DataOutputStream classOutput = new DataOutputStream(new BufferedOutputStream(classBytes))
			) {
				Workspace workspace = this.gui.getWorkspace();
				classNode.write(classOutput, workspace.getTypeResolver(), workspace, this.gui.createScript());
				classOutput.flush();
				String decompiledCode = DecompileUtils.decompile(classBytes.toByteArray());
				if (!decompiledCode.isEmpty()) {
					syntaxTextArea.setText(decompiledCode.replace('\u0000', '?'));
					syntaxTextArea.setCaretPosition(0);
				}
			} catch (IOException writeException) {
				writeException.printStackTrace();
			}
		}

		@Override
		public void onChanged(INotifier notifier, Object changeCode, Object changedObject, Object unused) {
			StructureNode structureNode = (StructureNode)((ObservableReference)notifier).getNotifier();
			if (structureNode != null) {
				if (changeCode != null && changeCode instanceof AtomicInt && ((AtomicInt)changeCode).get() == 3 && structureNode instanceof ClassNode) {
					return;
				}

				try {
					String codeText = structureNode.toCode();
					if (codeText.length() > 512000) {
						codeText = codeText.substring(0, 512000) + this.truncationNotice;
					}

					codeText = codeText.replace('\u0000', '?');
					this.codeView.setText(codeText);
					this.codeView.setCaretPosition(0);
					if (structureNode instanceof ClassNode) {
						if (Options.ENABLE_PATCHES && this.codeView instanceof RSyntaxTextArea) {
							this.showDecompiledCode((ClassNode)structureNode);
						}

						this.codeView.setToolTipText(HelpManager.getToolTip("BROWSER_VIEW_CLASS"));
					} else {
						if (Options.ENABLE_PATCHES && this.codeView instanceof RSyntaxTextArea) {
							((RSyntaxTextArea)this.codeView).setCodeFoldingEnabled(true);
						}

						if (structureNode instanceof FieldNode) {
							this.codeView.setToolTipText(HelpManager.getToolTip("BROWSER_VIEW_FIELD"));
						} else if (structureNode instanceof MethodNode) {
							this.codeView.setToolTipText(HelpManager.getToolTip("BROWSER_VIEW_METHOD"));
						} else {
							this.codeView.setToolTipText("");
						}
					}
				} catch (ProcessingException ignored) {
				}
			} else {
				this.codeView.setText("");
			}
		}

		@Override
		public void onChange(INotifier notifier, Object changeKind, Object changedNode, Object changeArg) {
			new ResourceChangeDispatcher(this, notifier, changeKind, changedNode, changeArg);
		}
	}

	public static class InitialMethodListener extends OptionalConsumer {
		public final GUI gui;

		public InitialMethodListener(GUI gui) {
			this.gui = gui;
		}

		@Override
		public void accept(Object arg) {
			this.acceptInitMethod((Integer)arg);
		}

		public void acceptInitMethod(Integer initMethod) {
			this.gui.resetCursorSafety();
			int initMethodCode = initMethod;
			if (initMethodCode == InitialHelperDialog.BUILD_HELPER) {
				ScriptContext scriptContext = this.gui.createScript();
				new BuildHelper(this.gui, scriptContext, GUI.getConfig(this.gui));
			} else if (initMethodCode == InitialHelperDialog.SET_CLASSPATH) {
				new OpenDialogHelper(this.gui, GUI.getConfig(this.gui));
			}
		}

		@Override
		public void accept() {
			this.gui.setDisable(false);
			this.gui.resetCursorSafety();
		}
	}

	public static class LoadClassesTask implements Runnable {
		public final GUI gui;
		public final AbstractLogger logger;
		public final ClasspathResource[] classes;
		public final ArchiveNode[] archiveNodes;
		public final Set<ArchiveNode> nestedArchives;
		public final ClasspathResource[] jadFiles;
		public final ClasspathResource[] allClasspathResources;
		public final ClasspathResource[] classpathResourceArray;
		public final ClasspathResource[] logFiles;
		public final boolean openNestedArchives;
		public final ZKMConsumer completionCallback;
		public final ClassLoadingStatus classLoadingStatus;

		public LoadClassesTask(
			GUI gui,
			AbstractLogger logger,
			ClasspathResource[] classes,
			ArchiveNode[] archiveNodes,
			Set<ArchiveNode> nestedArchives,
			ClasspathResource[] jadFiles,
			ClasspathResource[] allClasspathResources,
			ClasspathResource[] classpathResourceArray,
			ClasspathResource[] logFiles,
			boolean openNestedArchives,
			ZKMConsumer completionCallback,
			ClassLoadingStatus classLoadingStatus
		) {
			this.gui = gui;
			this.logger = logger;
			this.classes = classes;
			this.archiveNodes = archiveNodes;
			this.nestedArchives = nestedArchives;
			this.jadFiles = jadFiles;
			this.allClasspathResources = allClasspathResources;
			this.classpathResourceArray = classpathResourceArray;
			this.logFiles = logFiles;
			this.openNestedArchives = openNestedArchives;
			this.completionCallback = completionCallback;
			this.classLoadingStatus = classLoadingStatus;
		}

		@Override
		public void run() {
			this.gui.setDisable(true);
			NoDuplicatePrintWriter loadLogWriter = null;

			try {
				loadLogWriter = new NoDuplicatePrintWriter(new FileWriter("ZKM_LoadLog.txt"), true);
			} catch (IOException loadLogOpenException) {
				this.logger.error("ERROR:", "Couldn't open ZKM_LoadLog.txt : " + loadLogOpenException.getClass().getName());
			}

			PrintWriter warningLogWriter = null;

			try {
				warningLogWriter = new PrintWriter(new FileWriter("ZKM_WarningLog.txt"), true);
			} catch (IOException warningLogOpenException) {
				this.logger.error("ERROR:", "Couldn't open ZKM_WarningLog.txt : " + warningLogOpenException.getClass().getName());
			}

			AtomicInt reflectionWarningCount = new AtomicInt(0);

			try {
				GUI.getWorkspaceInternal(this.gui)
					.loadClasses(
						this.classes,
						this.archiveNodes,
						this.nestedArchives,
						null,
						null,
						null,
						null,
						null,
						(String)null,
						this.jadFiles,
						this.allClasspathResources,
						this.classpathResourceArray,
						this.logFiles,
						this.openNestedArchives,
						this.logger,
						this.completionCallback,
						this.classLoadingStatus,
						null,
						loadLogWriter,
						warningLogWriter,
						reflectionWarningCount,
						GUI.getLogWriter(this.gui)
					);
				if (!GUI.getWorkspaceInternal(this.gui).isEmpty() && !GUI.getWorkspaceInternal(this.gui).hasClasses()) {
					this.logger
						.errorWithDetails(
							"FATAL ERROR:",
							"No classes other than module-info opened",
							"No classes other than module-info opened. module-info classes are supported but they cannot be directly changed. Use the \"File | Open\" menu to also open your ordinary classes."
						);
				}
			} catch (Throwable loadFailure) {
				this.logger.errorWithDetails("FATAL ERROR:", "Unknown error : " + loadFailure.getClass().getName(), GlobalUtils.getStackTrace(loadFailure));
			} finally {
				if (loadLogWriter != null) {
					loadLogWriter.close();
				}

				if (warningLogWriter != null) {
					warningLogWriter.close();
				}
			}

			BufferedReader warningLogReader = null;
			BufferedReader loadLogReader = null;

			try {
				loadLogReader = new BufferedReader(new FileReader("ZKM_LoadLog.txt"));
				loadLogReader.mark(1);
				if (loadLogReader.read() != -1) {
					loadLogReader.reset();
					new ScrollableMessagePrompt(this.gui, "Class Open Errors", "The following errors were encounted...", loadLogReader, false, true);
				}
			} catch (FileNotFoundException ignoredMissingLoadLog) {
			} catch (IOException loadLogReadException) {
				new CopyablePrompt(this.gui, "Error", "Error reading ZKM_LoadLog.txt : " + loadLogReadException.getClass().getName());
			} finally {
				if (loadLogReader != null) {
					try {
						loadLogReader.close();
					} catch (IOException ignoredLoadLogClose) {
					}
				}
			}

			try {
				warningLogReader = new BufferedReader(new FileReader("ZKM_WarningLog.txt"));
				warningLogReader.mark(1);
				if (warningLogReader.read() != -1) {
					warningLogReader.reset();
					new WarningPrompt(this.gui, "Class Open Warnings", "API calls detected that may not be handled automatically...", warningLogReader, false, true);
				}
			} catch (FileNotFoundException ignoredMissingWarningLog) {
			} catch (IOException warningLogReadException) {
				new CopyablePrompt(this.gui, "Error", "Error reading ZKM_WarningLog.txt : " + warningLogReadException.getClass().getName());
			} finally {
				if (warningLogReader != null) {
					try {
						warningLogReader.close();
					} catch (IOException ignoredWarningLogClose) {
					}
				}
			}

			if (this.classLoadingStatus != null && this.gui.isVisible()) {
				this.classLoadingStatus.accept();
			}
		}
	}

	public static class ObfuscateTask implements Runnable {
		public final GUI gui;
		public final ObfuscateConfig obfuscateConfig;
		public final Vector<ExclusionInclusionStatement> excludeStatements;
		public final Vector<ObfuscateReferencesIncludeStatement> obfuscateReferencesIncludes;
		public final AbstractLogger logger;
		public final ZKMConsumer completionCallback;
		public final ScriptContext scriptContext;
		public final ClassLoadingStatus classLoadingStatus;

		public ObfuscateTask(
			GUI gui,
			ObfuscateConfig obfuscateConfig,
			Vector<ExclusionInclusionStatement> excludeStatements,
			Vector<ObfuscateReferencesIncludeStatement> obfuscateReferencesIncludes,
			AbstractLogger logger,
			ZKMConsumer completionCallback,
			ScriptContext scriptContext,
			ClassLoadingStatus classLoadingStatus
		) {
			this.gui = gui;
			this.obfuscateConfig = obfuscateConfig;
			this.excludeStatements = excludeStatements;
			this.obfuscateReferencesIncludes = obfuscateReferencesIncludes;
			this.logger = logger;
			this.completionCallback = completionCallback;
			this.scriptContext = scriptContext;
			this.classLoadingStatus = classLoadingStatus;
		}

		@Override
		@SneakyThrows
		public void run() {
			this.gui.setDisable(true);
			Workspace workspace = GUI.getWorkspaceInternal(this.gui);
			ChangeLogFileData[] changeLogFileData = this.obfuscateConfig.enableChangeLogFile ? this.obfuscateConfig.changeLogFileIn : null;
			List<UnexcludeStatement> unexcludeStatements = null;
			List<ObfuscateFlowExcludeStatement> obfuscateFlowExcludeStatements = null;
			List<ObfuscateFlowUnexcludeStatement> obfuscateFlowUnexcludeStatements = null;
			List<ObfuscateExceptionsExcludeStatement> obfuscateExceptionsExcludeStatements = null;
			List<ObfuscateExceptionsUnexcludeStatement> obfuscateExceptionsUnexcludeStatements = null;
			List<StringEncryptionExcludeStatement> encryptStringLiteralsExcludeStatements = null;
			List<StringEncryptionUnexcludeStatement> encryptStringLiteralsUnexcludeStatements = null;
			List<IntegerEncryptionExcludeStatement> integerEncryptionExcludeStatements = null;
			List<IntegerEncryptionUnexcludeStatement> integerEncryptionUnexcludeStatements = null;
			List<LongEncryptionExcludeStatement> longEncryptionExcludeStatements = null;
			List<LongEncryptionUnexcludeStatement> longEncryptionUnexcludeStatements = null;
			List<ExistingSerializedClassesStatement> existingSerializedClassesStatements = null;
			List<FixedClassesStatement> fixedClassesStatements = null;
			List<GroupingsStatement> groupingsStatements = null;
			List<AccessedByReflectionStatement> accessedByReflectionStatements = null;
			List<AccessedByReflectionExcludeStatement> accessedByReflectionExcludeStatements = null;
			List<ObfuscateReferencesExcludeStatement> obfuscateReferencesExcludeStatements = null;
			List<MethodParameterChangesIncludeStatement> methodParameterChangesIncludeStatements = null;
			List<MethodParameterChangesExcludeStatement> methodParameterChangesExcludeStatements = null;
			List<MethodParameterObfuscationIncludeStatement> methodParameterObfuscationIncludeStatements = null;
			List<MethodParameterObfuscationExcludeStatement> methodParameterObfuscationExcludeStatements = null;
			boolean obfuscateReferenceInSpecialClass = this.obfuscateConfig.obfuscateReferenceStructures == 1;
			ScriptContext context = this.scriptContext;
			ZKMConsumer completionCallback = this.completionCallback;
			AbstractLogger logger = this.logger;
			boolean useAsciiCharactersOnly = this.obfuscateConfig.useAsciiCharactersOnly;
			String newMethodNameFile = this.obfuscateConfig.newMethodNameFile;
			String newFieldNameFile = this.obfuscateConfig.newFieldNameFile;
			String newClassNameFile = this.obfuscateConfig.newClassNameFile;
			String newPackageNameFile = this.obfuscateConfig.newPackageNameFile;
			Integer assumeRuntimeVersionStringInteger = this.obfuscateConfig.assumeRuntimeVersionInteger;
			String methodParameterChangesPackage = this.obfuscateConfig.methodParameterChangesPackage;
			int methodParameterChanges = this.obfuscateConfig.methodParameterChanges;
			boolean uniqueMethodNames = this.obfuscateConfig.uniqueMethodNames;
			boolean uniqueClassNames = this.obfuscateConfig.uniqueClassNames;
			MixCaseType mixedCaseClassNames = this.obfuscateConfig.mixedCaseClassNames;
			boolean keepBalancedLocks = this.obfuscateConfig.keepBalancedLocks;
			boolean makeClassesPublic = this.obfuscateConfig.makeClassesPublic;
			boolean preverify = this.obfuscateConfig.preverify;
			String obfuscateReferencesPackage = this.obfuscateConfig.obfuscateReferencesPackage;
			int obfuscateReferences = this.obfuscateConfig.obfuscateReferences;
			String autoReflectionHash = this.obfuscateConfig.autoReflectionHash;
			String autoReflectionPackage = this.obfuscateConfig.autoReflectionPackage;
			int autoReflectionHandling = this.obfuscateConfig.autoReflectionHandling;
			int exceptionObfuscation = this.obfuscateConfig.exceptionObfuscation;
			String collapsePackagesWithDefaultValue = this.obfuscateConfig.collapseTargetPackage;
			boolean collapsePackagesWithDefault = this.obfuscateConfig.collapsePackagesWithDefault;
			int methodParameterOption = this.obfuscateConfig.methodParameterOption;
			int localVariableTables = this.obfuscateConfig.localVariableTables;
			int lineNumbers = this.obfuscateConfig.lineNumbers;
			int encryptLongConstants = this.obfuscateConfig.encryptLongConstants;
			int encryptIntegerConstants = this.obfuscateConfig.encryptIntegerConstants;
			int encryptStringLiterals = this.obfuscateConfig.encryptStringLiterals;
			int obfuscateParameters = this.obfuscateConfig.obfuscateParameters;
			int obfuscateControlFlow = this.obfuscateConfig.obfuscateControlFlow;
			int keepGenericsInformation = this.obfuscateConfig.keepGenericsInformation;
			int keepInnerClassInfo = this.obfuscateConfig.keepInnerClassInfo;
			boolean deriveGroupingsFromInputChangeLog = this.obfuscateConfig.useInputChangeLogGroupings;
			boolean allClassesOpened = this.obfuscateConfig.allClassesOpened;
			boolean randomizeObfuscation = this.obfuscateConfig.randomizeObfuscation;
			workspace.performObfuscation(
				changeLogFileData,
				this.obfuscateConfig.isLooseChangeLogFile,
				this.obfuscateConfig.changeLogPrintWriter,
				this.obfuscateConfig.changeLogFileOut,
				this.obfuscateConfig.useFileEncoding,
				this.excludeStatements,
				unexcludeStatements,
				obfuscateFlowExcludeStatements,
				obfuscateFlowUnexcludeStatements,
				obfuscateExceptionsExcludeStatements,
				obfuscateExceptionsUnexcludeStatements,
				encryptStringLiteralsExcludeStatements,
				encryptStringLiteralsUnexcludeStatements,
				integerEncryptionExcludeStatements,
				integerEncryptionUnexcludeStatements,
				longEncryptionExcludeStatements,
				longEncryptionUnexcludeStatements,
				existingSerializedClassesStatements,
				fixedClassesStatements,
				groupingsStatements,
				accessedByReflectionStatements,
				accessedByReflectionExcludeStatements,
				this.obfuscateReferencesIncludes,
				obfuscateReferencesExcludeStatements,
				methodParameterChangesIncludeStatements,
				methodParameterChangesExcludeStatements,
				methodParameterObfuscationIncludeStatements,
				methodParameterObfuscationExcludeStatements,
				this.obfuscateConfig.legalIdentifiers,
				this.obfuscateConfig.newNamesPrefix,
				this.obfuscateConfig.thideFieldNames,
				this.obfuscateConfig.hideStaticMethodNames,
				this.obfuscateConfig.aggressiveMethodRenaming,
				randomizeObfuscation,
				allClassesOpened,
				deriveGroupingsFromInputChangeLog,
				keepInnerClassInfo,
				keepGenericsInformation,
				obfuscateControlFlow,
				obfuscateParameters,
				encryptStringLiterals,
				encryptIntegerConstants,
				encryptLongConstants,
				lineNumbers,
				localVariableTables,
				methodParameterOption,
				collapsePackagesWithDefault,
				collapsePackagesWithDefaultValue,
				exceptionObfuscation,
				autoReflectionHandling,
				autoReflectionPackage,
				autoReflectionHash,
				obfuscateReferences,
				obfuscateReferenceInSpecialClass,
				obfuscateReferencesPackage,
				preverify,
				makeClassesPublic,
				keepBalancedLocks,
				mixedCaseClassNames,
				uniqueClassNames,
				uniqueMethodNames,
				methodParameterChanges,
				methodParameterChangesPackage,
				assumeRuntimeVersionStringInteger,
				newPackageNameFile,
				newClassNameFile,
				newFieldNameFile,
				newMethodNameFile,
				useAsciiCharactersOnly,
				logger,
				completionCallback,
				context
			);
			if (this.obfuscateConfig.changeLogPrintWriter != null) {
				this.obfuscateConfig.changeLogPrintWriter.close();
			}

			GUI.getCodeViewStatus(this.gui).markAndNotify();
			ClassNode classNode = (ClassNode)GUI.getPropertiesListStatus(this.gui).getNotifier();
			if (classNode != null) {
				classNode.refreshClassPropertyEntries();
			}

			if (this.classLoadingStatus != null) {
				this.classLoadingStatus.accept();
			}
		}
	}

	public static class PropertyListListener implements ChangeListener, ResourceChangedListener {
		public ObservableReference propertiesListStatus;
		public PlatformAdaptiveList<String> propertiesList;
		public DefaultListModel<String> propertiesModel;
		public Object lastNotifier;

		public PropertyListListener(ObservableReference propertiesListStatus, PlatformAdaptiveList<String> propertiesList) {
			this.propertiesListStatus = propertiesListStatus;
			this.propertiesList = propertiesList;
			this.propertiesModel = (DefaultListModel<String>)propertiesList.getModel();
			this.propertiesModel.addElement("no properties available");
			this.propertiesList.setEnabled(false);
			propertiesListStatus.subscribe(this);
			this.relayoutList();
		}

		private void relayoutList() {
			Container parentContainer = this.propertiesList.getParent();
			parentContainer.removeAll();
			parentContainer.add(this.propertiesList, "Center");
		}

		@Override
		public void onChanged(INotifier notifier, Object changeCode, Object changedObject, Object unused) {
			AbstractNotifier currentNotifier = ((ObservableReference)notifier).getNotifier();
			if (currentNotifier != null) {
				if (currentNotifier != this.lastNotifier) {
					if (currentNotifier instanceof ClassNode) {
						this.propertiesModel = new DefaultListModel<>();
						Enumeration<ObservableStructure> classProperties = ((ClassNode)currentNotifier).buildClassProperties();

						while (classProperties.hasMoreElements()) {
							ObservableStructure classProperty = classProperties.nextElement();
							this.propertiesModel.addElement(classProperty.getType());
						}

						this.propertiesList.setModel(this.propertiesModel);
						this.propertiesList.setEnabled(true);
					}

					this.relayoutList();
				}
			} else {
				this.propertiesModel = new DefaultListModel<>();
				this.propertiesModel.addElement("no properties available");
				this.propertiesList.setModel(this.propertiesModel);
				this.propertiesList.setEnabled(false);
				this.relayoutList();
			}

			this.lastNotifier = currentNotifier;
		}

		@Override
		public void onChange(INotifier notifier, Object changeKind, Object changedNode, Object changeArg) {
			new ResourceChangeDispatcher(this, notifier, changeKind, changedNode, changeArg);
		}
	}

	public static class SaveCallbackListener extends OptionalConsumer {
		public final GUI gui;

		public SaveCallbackListener(GUI gui) {
			this.gui = gui;
		}

		public void saveAllToFolder(File folder, Integer choice) {
			if (folder != null) {
				this.gui.saveAll(folder, null);
			}
		}

		public void noOpSaveToFolder(File file) {
		}

		@Override
		public void accept(Object arg, Object choice) {
			this.saveAllToFolder((File)arg, (Integer)choice);
		}

		@Override
		public void accept(Object arg) {
			this.noOpSaveToFolder((File)arg);
		}

		@Override
		public void accept() {
			this.gui.setDisable(false);
		}
	}

	public static class TrimListener extends OptionalConsumer {
		public final GUI gui;
		public final ClassLoadingStatus classLoadingStatus;

		public TrimListener(GUI gui, ClassLoadingStatus classLoadingStatus) {
			this.gui = gui;
			this.classLoadingStatus = classLoadingStatus;
		}

		@Override
		public void accept() {
			this.gui.resetCursorSafety();
			GUI.getWorkspaceStatus(this.gui).set(" ");
			if (this.classLoadingStatus == null) {
				this.gui.setDisable(false);
			}
		}
	}

	public static class TrimTask implements Runnable {
		public final GUI gui;
		public final ClassLoadingStatus classLoadingStatus;
		public final AbstractLogger logger;
		public final TrimConfig trimConfig;
		public final Vector<ExclusionInclusionStatement> excludeStatements;
		public final ZKMConsumer completionCallback;
		public final ScriptContext scriptContext;

		public TrimTask(
			GUI gui, ClassLoadingStatus classLoadingStatus, AbstractLogger logger, TrimConfig trimConfig, Vector<ExclusionInclusionStatement> excludeStatements, ZKMConsumer completionCallback, ScriptContext scriptContext
		) {
			this.gui = gui;
			this.classLoadingStatus = classLoadingStatus;
			this.logger = logger;
			this.trimConfig = trimConfig;
			this.excludeStatements = excludeStatements;
			this.completionCallback = completionCallback;
			this.scriptContext = scriptContext;
		}

		@Override
		@SneakyThrows
		public void run() {
			PrintWriter trimLogWriter = null;

			try {
				trimLogWriter = new PrintWriter(new FileWriter("ZKM_TrimLog.txt"));
			} catch (IOException trimLogOpenException) {
				if (this.classLoadingStatus != null) {
					this.classLoadingStatus.setStatus(false, null);
				}

				this.logger.error("ERROR:", "Couldn't open ZKM_TrimLog.txt : " + trimLogOpenException.getClass().getName());
			}

			GUI.getWorkspaceInternal(this.gui).trim(this.trimConfig, this.excludeStatements, null, null, null, trimLogWriter, this.logger, this.completionCallback, this.classLoadingStatus, this.scriptContext);
			GUI.getCodeViewStatus(this.gui).markAndNotify();
			ClassNode classNode = (ClassNode)GUI.getPropertiesListStatus(this.gui).getNotifier();
			if (classNode != null) {
				classNode.refreshClassPropertyEntries();
			}

			if (trimLogWriter != null) {
				trimLogWriter.close();
			}

			BufferedReader trimLogReader = null;

			try {
				trimLogReader = new BufferedReader(new FileReader("ZKM_TrimLog.txt"));
				trimLogReader.mark(1);
				if (trimLogReader.read() != -1) {
					trimLogReader.reset();
					new ScrollableMessagePrompt(this.gui, "Trim report", "Trim Report (ZKM_TrimLog.txt)...", trimLogReader, false, true, false);
				}
			} catch (FileNotFoundException ignoredMissingTrimLog) {
			} catch (IOException trimLogReadException) {
				new CopyablePrompt(this.gui, "Error", "Error reading ZKM_TrimLog.txt : " + trimLogReadException.getClass().getName());
			} finally {
				if (trimLogReader != null) {
					try {
						trimLogReader.close();
					} catch (IOException ignoredTrimLogClose) {
					}
				}
			}

			if (this.classLoadingStatus != null) {
				this.classLoadingStatus.accept();
			}
		}
	}

	public static class UpdateClassesListener extends OptionalConsumer {
		public final GUI gui;

		public UpdateClassesListener(GUI gui) {
			this.gui = gui;
		}

		@Override
		public void accept(Object classes, Object archiveNodes, Object jadResources, Object xmlResources, Object selectedResources, Object runningPathRef, Object openNestedArchives, Object archiveNodeSet) {
			this.updateClasses(
				(ClasspathResource[])classes,
				(ArchiveNode[])archiveNodes,
				(ClasspathResource[])jadResources,
				(ClasspathResource[])xmlResources,
				(ClasspathResource[])selectedResources,
				(AtomicReference<String>)runningPathRef,
				(Boolean)openNestedArchives,
				(Set<ArchiveNode>)archiveNodeSet
			);
		}

		public void updateClasses(
			ClasspathResource[] classes,
			ArchiveNode[] archiveNodes,
			ClasspathResource[] jadResources,
			ClasspathResource[] xmlResources,
			ClasspathResource[] selectedResources,
			AtomicReference<String> runningPathRef,
			Boolean openNestedArchives,
			Set<ArchiveNode> archiveNodeSet
		) {
			this.gui.setDisable(false);
			this.gui.onClassesUpdate(classes, archiveNodes, jadResources, xmlResources, runningPathRef, openNestedArchives, archiveNodeSet, (ClassLoadingStatus)null);
		}

		@Override
		public void accept() {
			this.gui.setDisable(false);
			GUI.updateClassListInternal(
				this.gui,
				(ClasspathResource[])null,
				(ArchiveNode[])null,
				(ClasspathResource[])null,
				(ClasspathResource[])null,
				GUI.getConfig(this.gui).getOpenNestedArchives(),
				null,
				(ClassLoadingStatus)null
			);
		}
	}

	public static class ConstantSelectionListener implements ListSelectionListener {
		public GUI gui;
		public ClassConstantsEntry classConstantsEntry;

		public ConstantSelectionListener(ClassConstantsEntry classConstantsEntry, GUI gui) {
			this.gui = gui;
			this.classConstantsEntry = classConstantsEntry;
		}

		@Override
		public void valueChanged(ListSelectionEvent event) {
			if (!event.getValueIsAdjusting()) {
				int selectedIndex = ((JList)event.getSource()).getSelectedIndex();
				if (selectedIndex > -1) {
					ConstantWriter constant = this.classConstantsEntry.getConstant(selectedIndex);
					this.gui.openEditConstantPanel(constant, this.classConstantsEntry.getReferenceGraph());
				}
			}
		}
	}

	public static class MemberSelectionListener implements ListSelectionListener {
		public GUI gui;
		public ClassMemberEntry classMemberEntry;

		public MemberSelectionListener(ClassMemberEntry classMemberEntry, GUI gui) {
			this.gui = gui;
			this.classMemberEntry = classMemberEntry;
		}

		@Override
		public void valueChanged(ListSelectionEvent event) {
			if (!event.getValueIsAdjusting()) {
				int selectedIndex = ((JList)event.getSource()).getSelectedIndex();
				if (selectedIndex > -1) {
					MemberNode memberNode = this.classMemberEntry.getMember(selectedIndex);
					this.gui.openAccessEditPanel(memberNode);
				}
			}
		}
	}
}
