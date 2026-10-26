package com.zelix.ui.dialogs;

import com.zelix.HelpManager;
import com.zelix.UIConfig;
import com.zelix.exceptions.StacktraceAnalysisException;
import com.zelix.klassmaster.KlassMaster;
import com.zelix.klassmaster.config.Options;
import com.zelix.klassmaster.constant.SystemConstants;
import com.zelix.repository.ClassRepository;
import com.zelix.repository.ClasspathManager;
import com.zelix.tools.retrace.lines.StacktraceAnalyzer;
import com.zelix.ui.GUI;
import com.zelix.ui.base.BaseDialog;
import com.zelix.ui.base.BaseDocumentListener;
import com.zelix.ui.components.PlatformAdaptiveScrollPanel;
import com.zelix.ui.components.panels.BeveledPanel;
import com.zelix.ui.components.panels.BorderedPanel;
import com.zelix.ui.dialogs.stacktrace.ChangeLogsListDialog;
import com.zelix.ui.dialogs.stacktrace.ObfuscatedBytecodeListDialog;
import com.zelix.ui.layouts.PowerLayout;
import com.zelix.ui.utils.GuiUtils;
import com.zelix.utils.FileUtils;
import com.zelix.utils.function.OptionalConsumer;
import com.zelix.utils.references.AtomicReference;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;

public class StacktraceTranslatorDialog extends BaseDialog implements ActionListener, ItemListener, KeyListener, MouseListener {
	public static String selectChangeLogPrompt = "Select the Change Log File";
	public static String instructionsText = "1) Select the change log file produced when the bytecode was obfuscated"
		+ Options.lineSeparator
		+ "2) Specify the obfuscated bytecode classpath which must include"
		+ Options.lineSeparator
		+ "   a) the obfuscated bytecode that generated the stack trace"
		+ Options.lineSeparator
		+ "   b) any supporting class libraries required to run the obfuscated bytecode"
		+ Options.lineSeparator
		+ "   c) an archive containing java.lang.Object (eg. rt.jar)"
		+ Options.lineSeparator
		+ "3) Paste the stack trace into the area above"
		+ Options.lineSeparator
		+ "4) Click on the <Translate> button"
		+ Options.lineSeparator
		+ "5) The translated stack trace will appear here";
	public static String[] layoutStyles = new String[]{
		"layout.minWidth=btnWidth*4",
		"layout.width=550",
		"layout.minHeight=rowHeight*15",
		"layout.height=550",
		"logFileNameLbl.left=5",
		"logFileNameLbl.centerY=logFileNameFld.centerY",
		"logBrowseBtn.left=lblWidth+10",
		"logBrowseBtn.centerY=logFileNameFld.centerY",
		"logFileNameFld.top=5",
		"logFileNameFld.left=logBrowseBtn.right+5",
		"logFileNameFld.right=container.right-5",
		"bytecodePathLbl.left=5",
		"bytecodePathLbl.centerY=bytecodePathFld.centerY",
		"bytecodePathBrowseBtn.left=lblWidth+10",
		"bytecodePathBrowseBtn.centerY=bytecodePathFld.centerY",
		"bytecodePathFld.top=logFileNameFld.top+rowHeight+5",
		"bytecodePathFld.left=bytecodePathBrowseBtn.right+5",
		"bytecodePathFld.right=container.right-5",
		"useBytecodeChk.left=5",
		"useBytecodeChk.top=bytecodePathLbl.bottom+7",
		"tracePnl.left=5",
		"tracePnl.top=useBytecodeChk.bottom+10",
		"tracePnl.right=container.right-5",
		"tracePnl.height=rowHeight*6",
		"resultsPnl.left=5",
		"resultsPnl.top=tracePnl.bottom+10",
		"resultsPnl.right=container.right-5",
		"resultsPnl.bottom=container.bottom-statusPnl.height-btnHeight-25",
		"lblWidth=max(logFileNameLbl.defaultWidth, bytecodePathLbl.defaultWidth)",
		"rowHeight=max(logFileNameLbl.defaultHeight, logBrowseBtn.defaultHeight, logFileNameFld.defaultHeight)",
		"btnWidth=max(translateBtn.defaultWidth, cancelBtn.defaultWidth, helpBtn.defaultWidth)",
		"btnHeight=max(translateBtn.defaultHeight, cancelBtn.defaultHeight, helpBtn.defaultHeight)",
		"translateBtn.centerX=container.width*16/100",
		"translateBtn.top=resultsPnl.bottom+15",
		"translateBtn.width=btnWidth",
		"cancelBtn.centerX=container.width*50/100",
		"cancelBtn.top=translateBtn.top",
		"cancelBtn.width=btnWidth",
		"helpBtn.centerX=container.width*84/100",
		"helpBtn.top=translateBtn.top",
		"helpBtn.width=btnWidth",
		"statusPnl.left=0",
		"statusPnl.bottom=container.bottom",
		"statusPnl.right=container.right"
	};
	public static String[] traceAreaStyles = new String[]{
		"traceAreaLbl.left=5",
		"traceAreaLbl.bottom=rowHeight+5",
		"traceAreaPasteBtn.bottom=traceAreaLbl.bottom",
		"traceAreaPasteBtn.right=container.right-5",
		"traceArea.left=5",
		"traceArea.top=traceAreaLbl.bottom+4",
		"traceArea.right=container.right-5",
		"traceArea.bottom=container.bottom-5",
		"rowHeight=max(traceAreaLbl.defaultHeight, traceAreaPasteBtn.defaultHeight)"
	};
	public static String[] resultsAreaStyles = new String[]{
		"resultsAreaLbl.left=5",
		"resultsAreaLbl.bottom=rowHeight+5",
		"resultsAreaCopyBtn.bottom=resultsAreaLbl.bottom",
		"resultsAreaCopyBtn.right=container.right-5",
		"resultsArea.left=5",
		"resultsArea.top=resultsAreaLbl.bottom+4",
		"resultsArea.right=container.right-5",
		"resultsArea.bottom=container.bottom-5",
		"rowHeight=max(resultsAreaLbl.defaultHeight, resultsAreaCopyBtn.defaultHeight)"
	};
	private List<String> changeLogFiles = new ArrayList<>();
	public boolean analyzerOutOfDate = true;
	private ClasspathManager classpathManager;
	public UIConfig uiConfig;
	public JButton translateButton;
	public StacktraceAnalyzer stacktraceAnalyzer;
	public JCheckBox useBytecodeCheckBox;
	private JTextField logFileNameField;
	public JButton helpButton;
	public JLabel statusLabel;
	public JTextArea resultsArea;
	public BorderedPanel statusPanel;
	public JButton resultsAreaCopyButton;
	public JTextArea traceArea;
	public JTextField bytecodePathField;
	public JButton cancelButton;
	public JButton logBrowseButton;
	public JButton traceAreaPasteButton;
	public JButton bytecodePathBrowseButton;

	public StacktraceTranslatorDialog(String title, UIConfig uiConfig) {
		super(title);
		this.initFromConfig(uiConfig, false);
	}

	public StacktraceTranslatorDialog(GUI gui, String title, UIConfig uiConfig) {
		super(gui, title);
		this.classpathManager = gui.getClasspathManager();
		this.initFromConfig(uiConfig, true);
	}

	public void focusBrowseButton() {
		GuiUtils.requestFocusInWindow(this.logBrowseButton);
	}

	public void browseChangeLogFile() {
		StacktraceTranslatorDialog.ChangeLogsListener changeLogsListener = new StacktraceTranslatorDialog.ChangeLogsListener(this);
		new ChangeLogsListDialog(this, "Change logs", this.changeLogFiles, this.logFileNameField.getText().trim(), HelpManager.getToolTip("CHANGELOG_LIST"), "OK", "OK", this.uiConfig, changeLogsListener);
	}

	public void setDefaultCursor() {
		Cursor defaultCursor = new Cursor(0);
		this.setCursor(defaultCursor);
		this.statusPanel.setCursor(defaultCursor);
		this.traceArea.setCursor(defaultCursor);
		this.resultsArea.setCursor(defaultCursor);
		this.logFileNameField.setCursor(defaultCursor);
		this.bytecodePathField.setCursor(defaultCursor);
	}

	protected void showHelp() {
		HelpManager.showHelp("035");
	}

	public void setResultsText(String text) {
		this.resultsArea.setText(text);
		this.resultsArea.setCaretPosition(0);
		this.setDefaultCursor();
		this.statusLabel.setText(" ");
	}

	private void initFromConfig(UIConfig uiConfig, boolean positionDialog) {
		this.uiConfig = uiConfig;
		String lastClasspathPath = uiConfig.getLastClasspathPath();
		if (lastClasspathPath != null && lastClasspathPath.trim().length() > 0) {
			this.classpathManager = new ClasspathManager(lastClasspathPath.trim());
			this.classpathManager.setClassRepository(new ClassRepository(this.classpathManager, FileUtils.IS_UNIX));
		} else if (this.classpathManager == null) {
			this.classpathManager = new ClasspathManager(SystemConstants.userDirectory);
			this.classpathManager.setClassRepository(new ClassRepository(this.classpathManager, FileUtils.IS_UNIX));
		}

		this.classpathManager.initializeBootClasspath(new AtomicReference<>(), new AtomicReference<>());
		this.bytecodePathField.setText(this.classpathManager.getRawClasspath());
		this.bytecodePathField.setCaretPosition(0);
		boolean useBytecode = uiConfig.getUseBytecode();
		this.useBytecodeCheckBox.setSelected(useBytecode);
		this.bytecodePathBrowseButton.setEnabled(useBytecode);
		this.bytecodePathField.setEnabled(useBytecode);
		if (positionDialog) {
			this.positionDialog(0, 0, super.parentFrame != null);
			this.focusBrowseButton();
		}
	}

	@Override
	protected void initGui(Object initArg1, Object initArg2, Object initArg3, Object initArg4, Object initArg5, Object initArg6, Object initArg7) {
		this.setFont(KlassMaster.getFont());
		Container contentPane = this.getContentPane();
		PowerLayout rootLayout = new PowerLayout(contentPane);
		contentPane.setLayout(rootLayout);
		this.logFileNameField = new JTextField();
		this.logFileNameField.setEditable(false);
		this.logFileNameField.setToolTipText(HelpManager.getToolTip("CHANGELOG_FLD_CHANGELOG_TRANSLATE"));
		this.bytecodePathField = new JTextField();
		this.bytecodePathField.setEditable(false);
		this.bytecodePathField.setToolTipText(HelpManager.getToolTip("CLASSPATH_MSG_CHANGELOG_TRANSLATE"));
		this.useBytecodeCheckBox = new JCheckBox("Analyze obfuscated bytecode (highly recommended)");
		this.useBytecodeCheckBox.setToolTipText(HelpManager.getToolTip("CLASSPATH_CHK_CHANGELOG_TRANSLATE"));
		this.traceArea = new JTextArea();
		Font baseFont = this.traceArea.getFont();
		Font monospacedFont = new Font("Monospaced", baseFont.getStyle(), baseFont.getSize());
		this.traceArea.setFont(monospacedFont);
		this.traceArea.setToolTipText(HelpManager.getToolTip("TRACE_CHANGELOG_TRANSLATE"));
		this.resultsArea = new JTextArea(instructionsText);
		this.resultsArea.setFont(monospacedFont);
		this.resultsArea.setEditable(false);
		this.resultsArea.setToolTipText(HelpManager.getToolTip("RESULTS_CHANGELOG_TRANSLATE"));
		this.logBrowseButton = new JButton(". . .");
		this.logBrowseButton.setToolTipText(HelpManager.getToolTip("BROWSE_CHANGELOG_TRANSLATE"));
		this.bytecodePathBrowseButton = new JButton(". . .");
		this.bytecodePathBrowseButton.setToolTipText(HelpManager.getToolTip("BROWSE_BYTECODE_CHANGELOG_TRANSLATE"));
		this.traceAreaPasteButton = new JButton("Paste");
		this.traceAreaPasteButton.setToolTipText(HelpManager.getToolTip("PASTE_CONTENTS"));
		this.resultsAreaCopyButton = new JButton("Copy");
		this.resultsAreaCopyButton.setToolTipText(HelpManager.getToolTip("COPY_CHANGELOG_TRANSLATE"));
		this.translateButton = new JButton("Translate");
		this.translateButton.setToolTipText(HelpManager.getToolTip("TRANSLATE_CHANGELOG_TRANSLATE"));
		this.cancelButton = new JButton("Cancel");
		this.cancelButton.setToolTipText(HelpManager.getToolTip("CANCEL_DIALOG"));
		this.helpButton = new JButton("Help");
		this.helpButton.setToolTipText(HelpManager.getToolTip("SCREEN_HELP"));
		JLabel logFileNameLabel = new JLabel("Change log file :");
		contentPane.add(this.logFileNameField, "logFileNameFld");
		JLabel bytecodePathLabel = new JLabel("Obfuscated bytecode :");
		contentPane.add(this.bytecodePathField, "bytecodePathFld");
		contentPane.add(this.useBytecodeCheckBox, "useBytecodeChk");
		BeveledPanel tracePanel = new BeveledPanel(false, true);
		PowerLayout traceLayout = new PowerLayout(tracePanel);
		tracePanel.setLayout(traceLayout);
		tracePanel.add(new PlatformAdaptiveScrollPanel(this.traceArea), "traceArea");
		tracePanel.add(this.traceAreaPasteButton, "traceAreaPasteBtn");
		JLabel traceAreaLabel = new JLabel("Paste original stack trace here...");
		tracePanel.add(traceAreaLabel, "traceAreaLbl");
		contentPane.add(tracePanel, "tracePnl");
		BeveledPanel resultsPanel = new BeveledPanel(false, true);
		PowerLayout resultsLayout = new PowerLayout(resultsPanel);
		resultsPanel.setLayout(resultsLayout);
		resultsPanel.add(new PlatformAdaptiveScrollPanel(this.resultsArea), "resultsArea");
		resultsPanel.add(this.resultsAreaCopyButton, "resultsAreaCopyBtn");
		JLabel resultsAreaLabel = new JLabel("Translated stack trace will appear here...");
		resultsPanel.add(resultsAreaLabel, "resultsAreaLbl");
		contentPane.add(resultsPanel, "resultsPnl");
		this.statusPanel = new BorderedPanel(false);
		this.statusPanel.setLayout(new BorderLayout());
		contentPane.add(this.statusPanel, "statusPnl");
		this.statusLabel = new JLabel(selectChangeLogPrompt);
		this.statusPanel.add(this.statusLabel, "Center");
		contentPane.add(this.logBrowseButton, "logBrowseBtn");
		contentPane.add(this.bytecodePathBrowseButton, "bytecodePathBrowseBtn");
		contentPane.add(this.translateButton, "translateBtn");
		contentPane.add(this.cancelButton, "cancelBtn");
		contentPane.add(this.helpButton, "helpBtn");
		this.logBrowseButton.addActionListener(this);
		this.bytecodePathBrowseButton.addActionListener(this);
		this.traceAreaPasteButton.addActionListener(this);
		this.resultsAreaCopyButton.addActionListener(this);
		this.translateButton.addActionListener(this);
		this.cancelButton.addActionListener(this);
		this.helpButton.addActionListener(this);
		this.logBrowseButton.addKeyListener(this);
		this.bytecodePathBrowseButton.addKeyListener(this);
		this.traceAreaPasteButton.addKeyListener(this);
		this.resultsAreaCopyButton.addKeyListener(this);
		this.translateButton.addKeyListener(this);
		this.cancelButton.addKeyListener(this);
		this.helpButton.addKeyListener(this);
		this.logFileNameField.addActionListener(this);
		this.logFileNameField.addMouseListener(this);
		this.bytecodePathField.addMouseListener(this);
		this.useBytecodeCheckBox.addItemListener(this);
		this.logFileNameField.getDocument().addDocumentListener(new StacktraceTranslatorDialog.LogFileNameListener(this));
		this.traceArea.getDocument().addDocumentListener(new StacktraceTranslatorDialog.TraceAreaListener(this));
		contentPane.add(logFileNameLabel, "logFileNameLbl");
		contentPane.add(bytecodePathLabel, "bytecodePathLbl");
		rootLayout.setProperties(layoutStyles);
		traceLayout.setProperties(traceAreaStyles);
		resultsLayout.setProperties(resultsAreaStyles);
		this.translateButton.setEnabled(false);
		this.setResizable(true);
		this.setIconImage(KlassMaster.getIcon(this));
		GuiUtils.packWindow(this);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Object eventSource = e.getSource();
		if (eventSource == this.logBrowseButton) {
			this.browseChangeLogFile();
		} else if (eventSource == this.bytecodePathBrowseButton) {
			this.browseBytecodeClasspath();
		} else if (eventSource == this.traceAreaPasteButton) {
			this.pasteTrace();
		} else if (eventSource == this.resultsAreaCopyButton) {
			this.copyResults();
		} else if (eventSource == this.translateButton) {
			this.translate();
		} else if (eventSource == this.cancelButton) {
			this.onConfirm();
		} else if (eventSource == this.helpButton) {
			this.showHelp();
		} else if ((eventSource == this.logFileNameField || eventSource == this.traceArea) && this.translateButton.isEnabled()) {
			this.translate();
		}
	}

	public void translate() {
		if (this.analyzerOutOfDate || this.stacktraceAnalyzer == null) {
			this.logFileNameField.getText().trim();
			this.stacktraceAnalyzer = new StacktraceAnalyzer(this.changeLogFiles, this.classpathManager);
			this.analyzerOutOfDate = false;
		}

		StacktraceTranslatorDialog.TranslateStacktraceThread translateThread = new StacktraceTranslatorDialog.TranslateStacktraceThread(this);
		this.resultsArea.setText("");
		this.statusLabel.setText("Analyzing.  Please wait...");
		this.setWaitCursor();
		translateThread.start();
	}

	public void setWaitCursor() {
		Cursor waitCursor = new Cursor(Cursor.WAIT_CURSOR);
		this.setCursor(waitCursor);
		this.statusPanel.setCursor(waitCursor);
		this.traceArea.setCursor(waitCursor);
		this.resultsArea.setCursor(waitCursor);
		this.logFileNameField.setCursor(waitCursor);
		this.bytecodePathField.setCursor(waitCursor);
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		if (e.getSource() == this.useBytecodeCheckBox) {
			boolean useBytecode = this.useBytecodeCheckBox.isSelected();
			this.bytecodePathBrowseButton.setEnabled(useBytecode);
			this.bytecodePathField.setEnabled(useBytecode);
			if (useBytecode != this.uiConfig.getUseBytecode()) {
				this.uiConfig.setUseBytecode(useBytecode);
				this.uiConfig.saveConfig();
			}
		}
	}

	@Override
	public void keyPressed(KeyEvent e) {
		if (e.getKeyCode() == 10) {
			Component eventSource = (Component)e.getSource();
			if (eventSource == this.logBrowseButton) {
				this.browseChangeLogFile();
			} else if (eventSource == this.bytecodePathBrowseButton) {
				this.browseBytecodeClasspath();
			} else if (eventSource == this.traceAreaPasteButton) {
				this.pasteTrace();
			} else if (eventSource == this.resultsAreaCopyButton) {
				this.copyResults();
			} else if (eventSource == this.translateButton) {
				this.translate();
			} else if (eventSource == this.cancelButton) {
				this.onConfirm();
			} else if (eventSource == this.helpButton) {
				this.showHelp();
			}
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
	}

	@Override
	public void keyTyped(KeyEvent e) {
	}

	public void pasteTrace() {
		this.traceArea.selectAll();
		this.traceArea.paste();
		this.traceArea.setCaretPosition(0);
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		if (e.getClickCount() == 2) {
			Object eventSource = e.getSource();
			if (eventSource == this.logFileNameField) {
				this.browseChangeLogFile();
			} else if (eventSource == this.bytecodePathField) {
				this.browseBytecodeClasspath();
			}
		}
	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent e) {
	}

	@Override
	public void mousePressed(MouseEvent e) {
	}

	@Override
	public void mouseReleased(MouseEvent e) {
	}

	@Override
	protected void onConfirm() {
		this.close();
	}

	@Override
	public void close() {
		super.close();
		if (this.stacktraceAnalyzer != null) {
			this.stacktraceAnalyzer.close();
		}

		if (super.parentFrame != null) {
			((GUI)super.parentFrame).setDisable(false);
		} else {
			System.exit(0);
		}
	}

	public void copyResults() {
		this.resultsArea.selectAll();
		this.resultsArea.copy();
	}

	public void browseBytecodeClasspath() {
		StacktraceTranslatorDialog.ClasspathListener classpathListener = new StacktraceTranslatorDialog.ClasspathListener(this);
		new ObfuscatedBytecodeListDialog(
			this,
			"Obfuscated Bytecode Classpath",
			this.classpathManager,
			HelpManager.getToolTip("CLASSPATH_MSG_CHANGELOG_TRANSLATE"),
			"OK",
			HelpManager.getToolTip("BROWSE_BYTECODE_OK_CHANGELOG_TRANSLATE"),
			this.uiConfig,
			classpathListener
		);
	}

	public static JTextField getChangeLogFileField(StacktraceTranslatorDialog stacktraceTranslatorDialog) {
		return stacktraceTranslatorDialog.logFileNameField;
	}

	public static List<String> setChangeLogFiles(StacktraceTranslatorDialog stacktraceTranslatorDialog, List<String> changeLogFiles) {
		stacktraceTranslatorDialog.changeLogFiles = changeLogFiles;
		return stacktraceTranslatorDialog.changeLogFiles;
	}

	public static class TraceAreaListener extends BaseDocumentListener {
		public final StacktraceTranslatorDialog stacktraceTranslatorDialog;

		public TraceAreaListener(StacktraceTranslatorDialog stacktraceTranslatorDialog) {
			this.stacktraceTranslatorDialog = stacktraceTranslatorDialog;
		}

		@Override
		public void onUpdate(DocumentEvent e) {
			if (StacktraceTranslatorDialog.getChangeLogFileField(this.stacktraceTranslatorDialog).getText().trim().length() > 0 && this.stacktraceTranslatorDialog.traceArea.getText().trim().length() > 0) {
				this.stacktraceTranslatorDialog.translateButton.setEnabled(true);
			} else {
				this.stacktraceTranslatorDialog.translateButton.setEnabled(false);
			}
		}
	}

	public static class LogFileNameListener extends BaseDocumentListener {
		public final StacktraceTranslatorDialog stacktraceTranslatorDialog;

		public LogFileNameListener(StacktraceTranslatorDialog stacktraceTranslatorDialog) {
			this.stacktraceTranslatorDialog = stacktraceTranslatorDialog;
		}

		@Override
		public void onUpdate(DocumentEvent e) {
			this.stacktraceTranslatorDialog.analyzerOutOfDate = true;
			if (StacktraceTranslatorDialog.getChangeLogFileField(this.stacktraceTranslatorDialog).getText().trim().length() > 0) {
				this.stacktraceTranslatorDialog.statusLabel.setText(" ");
			} else {
				this.stacktraceTranslatorDialog.statusLabel.setText(StacktraceTranslatorDialog.selectChangeLogPrompt);
			}

			if (StacktraceTranslatorDialog.getChangeLogFileField(this.stacktraceTranslatorDialog).getText().trim().length() > 0 && this.stacktraceTranslatorDialog.traceArea.getText().trim().length() > 0) {
				this.stacktraceTranslatorDialog.translateButton.setEnabled(true);
			} else {
				this.stacktraceTranslatorDialog.translateButton.setEnabled(false);
			}
		}
	}

	public static class ClasspathListener extends OptionalConsumer {
		public final StacktraceTranslatorDialog stacktraceTranslatorDialog;

		public ClasspathListener(StacktraceTranslatorDialog stacktraceTranslatorDialog) {
			this.stacktraceTranslatorDialog = stacktraceTranslatorDialog;
		}

		@Override
		public void accept(Object arg, Object choice) {
			this.acceptClasspath((ClasspathManager)arg, (Integer)choice);
		}

		public void acceptClasspath(ClasspathManager classpathManager, Integer choice) {
			if (classpathManager != null) {
				this.stacktraceTranslatorDialog.bytecodePathField.setText(classpathManager.getRawClasspath());
				this.stacktraceTranslatorDialog.bytecodePathField.setCaretPosition(0);
			}
		}

		@Override
		public void accept() {
		}
	}

	public static class ChangeLogsListener extends OptionalConsumer {
		public final StacktraceTranslatorDialog stacktraceTranslatorDialog;

		public ChangeLogsListener(StacktraceTranslatorDialog stacktraceTranslatorDialog) {
			this.stacktraceTranslatorDialog = stacktraceTranslatorDialog;
		}

		public void acceptChangeLogs(List<String> changeLogFiles, String changeLogFilesText, Integer choice) {
			if (changeLogFiles != null && changeLogFilesText != null) {
				StacktraceTranslatorDialog.setChangeLogFiles(this.stacktraceTranslatorDialog, new ArrayList<>(changeLogFiles));
				StacktraceTranslatorDialog.getChangeLogFileField(this.stacktraceTranslatorDialog).setText(changeLogFilesText);
				StacktraceTranslatorDialog.getChangeLogFileField(this.stacktraceTranslatorDialog).setCaretPosition(0);
			}
		}

		@Override
		public void accept(Object changeLogFiles, Object changeLogFilesText, Object choice) {
			this.acceptChangeLogs((List<String>)changeLogFiles, (String)changeLogFilesText, (Integer)choice);
		}

		@Override
		public void accept() {
		}
	}

	public static class TranslateStacktraceThread extends Thread {
		public final StacktraceTranslatorDialog stacktraceTranslatorDialog;
		public Runnable showResultTask;
		public String translatedStacktrace;

		public TranslateStacktraceThread(StacktraceTranslatorDialog stacktraceTranslatorDialog) {
			this.stacktraceTranslatorDialog = stacktraceTranslatorDialog;
			this.showResultTask = new StacktraceTranslatorDialog.TranslateStacktraceThread.ShowResultRunnable(this);
		}

		@Override
		public void run() {
			try {
				this.translatedStacktrace = this.stacktraceTranslatorDialog.stacktraceAnalyzer.translate(this.stacktraceTranslatorDialog.traceArea.getText(), this.stacktraceTranslatorDialog.useBytecodeCheckBox.isSelected());
			} catch (StacktraceAnalysisException analyzeException) {
				this.translatedStacktrace = analyzeException.getMessage();
			}

			SwingUtilities.invokeLater(this.showResultTask);
		}

		public static class ShowResultRunnable implements Runnable {
			public final StacktraceTranslatorDialog.TranslateStacktraceThread translateStacktraceThread;

			public ShowResultRunnable(StacktraceTranslatorDialog.TranslateStacktraceThread translateStacktraceThread) {
				this.translateStacktraceThread = translateStacktraceThread;
			}

			@Override
			public void run() {
				this.translateStacktraceThread.stacktraceTranslatorDialog.setResultsText(this.translateStacktraceThread.translatedStacktrace);
			}
		}
	}
}
