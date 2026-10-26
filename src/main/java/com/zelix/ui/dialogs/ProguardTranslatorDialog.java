package com.zelix.ui.dialogs;

import com.zelix.HelpManager;
import com.zelix.UIConfig;
import com.zelix.klassmaster.KlassMaster;
import com.zelix.klassmaster.config.Options;
import com.zelix.klassmaster.constant.SystemConstants;
import com.zelix.tools.proguardConfigurationTranslate.ProguardConfigUtils;
import com.zelix.ui.GUI;
import com.zelix.ui.base.BaseDialog;
import com.zelix.ui.base.BaseDocumentListener;
import com.zelix.ui.components.PlatformAdaptiveScrollPanel;
import com.zelix.ui.components.panels.BeveledPanel;
import com.zelix.ui.components.panels.BorderedPanel;
import com.zelix.ui.components.panels.FileSelectPanel;
import com.zelix.ui.filefilters.AllFiles;
import com.zelix.ui.filefilters.FileFilterBase;
import com.zelix.ui.filefilters.TextFile;
import com.zelix.ui.layouts.PowerLayout;
import com.zelix.ui.prompts.CopyablePrompt;
import com.zelix.ui.utils.GuiUtils;
import com.zelix.utils.FileUtils;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;

public class ProguardTranslatorDialog extends BaseDialog implements ActionListener, KeyListener, MouseListener {
	public static String initialStatusText = "Open a ProGuard configuration file or paste contents into upper area.";
	public static String initialResultsText = "1) Open a ProGuard configuration file or paste contents into upper area,"
		+ Options.lineSeparator
		+ "2) Click on the <Translate> button,"
		+ Options.lineSeparator
		+ "3) The translated ZKM Script will appear here.";
	public static String[] dialogStyles = new String[]{
		"layout.minWidth=btnWidth*4",
		"layout.width=550",
		"layout.minHeight=rowHeight*15",
		"layout.height=550",
		"proGuardScriptFileNameLbl.left=5",
		"proGuardScriptFileNameLbl.bottom=proGuardScriptOpenBtn.bottom",
		"proGuardScriptOpenBtn.left=proGuardScriptFileNameLbl.right+10",
		"proGuardScriptOpenBtn.centerY=rowHeight/2+10",
		"proGuardScriptFileNameFld.centerY=proGuardScriptOpenBtn.centerY",
		"proGuardScriptFileNameFld.left=proGuardScriptOpenBtn.right+5",
		"proGuardScriptFileNameFld.right=container.right-5",
		"proGuardPnl.left=5",
		"proGuardPnl.top=proGuardScriptFileNameFld.bottom+10",
		"proGuardPnl.right=container.right-5",
		"proGuardPnl.height=rowHeight*6",
		"resultsPnl.left=5",
		"resultsPnl.top=proGuardPnl.bottom+10",
		"resultsPnl.right=container.right-5",
		"resultsPnl.bottom=container.bottom-statusPnl.height-btnHeight-25",
		"rowHeight=max(proGuardScriptFileNameLbl.defaultHeight, proGuardScriptOpenBtn.defaultHeight, proGuardScriptFileNameFld.defaultHeight)",
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
	public static String[] proGuardPanelStyles = new String[]{
		"proGuardAreaLbl.left=5",
		"proGuardAreaLbl.bottom=rowHeight+5",
		"proGuardAreaPasteBtn.bottom=proGuardAreaLbl.bottom",
		"proGuardAreaPasteBtn.right=container.right-5",
		"proGuardArea.left=5",
		"proGuardArea.top=proGuardAreaLbl.bottom+4",
		"proGuardArea.right=container.right-5",
		"proGuardArea.bottom=container.bottom-5",
		"rowHeight=max(proGuardAreaLbl.defaultHeight, proGuardAreaPasteBtn.defaultHeight)"
	};
	public static String[] resultsPanelStyles = new String[]{
		"resultsAreaLbl.left=5",
		"resultsAreaLbl.bottom=rowHeight+5",
		"resultsAreaCopyBtn.bottom=resultsAreaLbl.bottom",
		"resultsAreaCopyBtn.right=container.right-5",
		"resultsAreaSaveBtn.bottom=resultsAreaLbl.bottom",
		"resultsAreaSaveBtn.right=resultsAreaCopyBtn.left-10",
		"resultsArea.left=5",
		"resultsArea.top=resultsAreaLbl.bottom+4",
		"resultsArea.right=container.right-5",
		"resultsArea.bottom=container.bottom-5",
		"rowHeight=max(resultsAreaLbl.defaultHeight, resultsAreaSaveBtn.defaultHeight, resultsAreaCopyBtn.defaultHeight)"
	};
	public boolean analyzerOutOfDate = true;
	public JButton copyButton;
	public JButton helpButton;
	public JButton pasteButton;
	public JTextField proGuardFileNameField;
	public JLabel statusLabel;
	public UIConfig uiConfig;
	public BorderedPanel statusPanel;
	public JTextArea resultsArea;
	public JButton openButton;
	public JButton cancelButton;
	public JButton saveButton;
	public JTextArea proGuardArea;
	public JButton translateButton;

	public ProguardTranslatorDialog(GUI parent, String title, UIConfig uiConfig) {
		super(parent, title);
		this.initConfig(uiConfig, true);
	}

	public void focusOpenButton() {
		GuiUtils.requestFocusInWindow(this.openButton);
	}

	public void setTranslatedScript(String translatedScript) {
		this.resultsArea.setText(translatedScript);
		this.resultsArea.setCaretPosition(0);
		this.setDefaultCursor();
		this.statusLabel.setText(" ");
	}

	public void pasteProguardConfig() {
		this.proGuardArea.selectAll();
		this.proGuardArea.paste();
		this.proGuardArea.setCaretPosition(0);
	}

	@Override
	protected void initGui(Object initArg1, Object initArg2, Object initArg3, Object initArg4, Object initArg5, Object initArg6, Object initArg7) {
		this.setFont(KlassMaster.getFont());
		Container contentPane = this.getContentPane();
		PowerLayout rootLayout = new PowerLayout(contentPane);
		contentPane.setLayout(rootLayout);
		this.proGuardFileNameField = new JTextField();
		this.proGuardFileNameField.setEditable(false);
		this.proGuardFileNameField.setToolTipText(HelpManager.getToolTip("PROGUARD_SCRIPT_FLD_PROGUARD_SCRIPT_TRANSLATE"));
		this.proGuardArea = new JTextArea();
		Font baseFont = this.proGuardArea.getFont();
		Font monospacedFont = new Font("Monospaced", baseFont.getStyle(), baseFont.getSize());
		this.proGuardArea.setFont(monospacedFont);
		this.proGuardArea.setToolTipText(HelpManager.getToolTip("PROGUARD_AREA_PROGUARD_SCRIPT_TRANSLATE"));
		this.resultsArea = new JTextArea(initialResultsText);
		this.resultsArea.setFont(monospacedFont);
		this.resultsArea.setEditable(false);
		this.resultsArea.setToolTipText(HelpManager.getToolTip("RESULTS_PROGUARD_SCRIPT_TRANSLATE"));
		this.openButton = new JButton("Open");
		this.openButton.setToolTipText(HelpManager.getToolTip("OPEN_PROGUARD_SCRIPT_TRANSLATE"));
		this.saveButton = new JButton("Save");
		this.saveButton.setToolTipText(HelpManager.getToolTip("SAVE_PROGUARD_SCRIPT_TRANSLATE"));
		this.pasteButton = new JButton("Paste");
		this.pasteButton.setToolTipText(HelpManager.getToolTip("PASTE_CONTENTS"));
		this.copyButton = new JButton("Copy");
		this.copyButton.setToolTipText(HelpManager.getToolTip("COPY_PROGUARD_SCRIPT_TRANSLATE"));
		this.translateButton = new JButton("Translate");
		this.cancelButton = new JButton("Cancel");
		this.cancelButton.setToolTipText(HelpManager.getToolTip("CANCEL_DIALOG"));
		this.helpButton = new JButton("Help");
		this.helpButton.setToolTipText(HelpManager.getToolTip("SCREEN_HELP"));
		JLabel proGuardFileNameLabel = new JLabel("ProGuard configuration file :");
		contentPane.add(this.proGuardFileNameField, "proGuardScriptFileNameFld");
		BeveledPanel proGuardPanel = new BeveledPanel(false, true);
		PowerLayout proGuardLayout = new PowerLayout(proGuardPanel);
		proGuardPanel.setLayout(proGuardLayout);
		proGuardPanel.add(new PlatformAdaptiveScrollPanel(this.proGuardArea), "proGuardArea");
		proGuardPanel.add(this.pasteButton, "proGuardAreaPasteBtn");
		JLabel proGuardAreaLabel = new JLabel("Open a ProGuard configuration file or paste contents into here...");
		proGuardPanel.add(proGuardAreaLabel, "proGuardAreaLbl");
		contentPane.add(proGuardPanel, "proGuardPnl");
		BeveledPanel resultsPanel = new BeveledPanel(false, true);
		PowerLayout resultsLayout = new PowerLayout(resultsPanel);
		resultsPanel.setLayout(resultsLayout);
		resultsPanel.add(new PlatformAdaptiveScrollPanel(this.resultsArea), "resultsArea");
		resultsPanel.add(this.saveButton, "resultsAreaSaveBtn");
		resultsPanel.add(this.copyButton, "resultsAreaCopyBtn");
		JLabel resultsAreaLabel = new JLabel("Translated ZKM Script will appear here...");
		resultsPanel.add(resultsAreaLabel, "resultsAreaLbl");
		contentPane.add(resultsPanel, "resultsPnl");
		this.statusPanel = new BorderedPanel(false);
		this.statusPanel.setLayout(new BorderLayout());
		contentPane.add(this.statusPanel, "statusPnl");
		this.statusLabel = new JLabel(initialStatusText);
		this.statusPanel.add(this.statusLabel, "Center");
		contentPane.add(this.openButton, "proGuardScriptOpenBtn");
		contentPane.add(this.translateButton, "translateBtn");
		contentPane.add(this.cancelButton, "cancelBtn");
		contentPane.add(this.helpButton, "helpBtn");
		this.openButton.addActionListener(this);
		this.saveButton.addActionListener(this);
		this.pasteButton.addActionListener(this);
		this.copyButton.addActionListener(this);
		this.translateButton.addActionListener(this);
		this.cancelButton.addActionListener(this);
		this.helpButton.addActionListener(this);
		this.openButton.addKeyListener(this);
		this.saveButton.addKeyListener(this);
		this.pasteButton.addKeyListener(this);
		this.copyButton.addKeyListener(this);
		this.translateButton.addKeyListener(this);
		this.cancelButton.addKeyListener(this);
		this.helpButton.addKeyListener(this);
		this.proGuardFileNameField.addActionListener(this);
		this.proGuardFileNameField.addMouseListener(this);
		this.proGuardArea.getDocument().addDocumentListener(new ProguardTranslatorDialog.ConfigAreaListener(this));
		contentPane.add(proGuardFileNameLabel, "proGuardScriptFileNameLbl");
		rootLayout.setProperties(dialogStyles);
		proGuardLayout.setProperties(proGuardPanelStyles);
		resultsLayout.setProperties(resultsPanelStyles);
		this.translateButton.setEnabled(false);
		this.setResizable(true);
		this.setIconImage(KlassMaster.getIcon(this));
		GuiUtils.packWindow(this);
	}

	public void setDefaultCursor() {
		Cursor defaultCursor = new Cursor(0);
		this.setCursor(defaultCursor);
		this.statusPanel.setCursor(defaultCursor);
		this.proGuardArea.setCursor(defaultCursor);
		this.resultsArea.setCursor(defaultCursor);
		this.proGuardFileNameField.setCursor(defaultCursor);
	}

	protected void showHelp() {
		HelpManager.showHelp("040");
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Object eventSource = e.getSource();
		if (eventSource == this.openButton) {
			this.openProguardConfigFile();
		} else if (eventSource == this.saveButton) {
			this.saveScript();
		} else if (eventSource == this.pasteButton) {
			this.pasteProguardConfig();
		} else if (eventSource == this.copyButton) {
			this.copyScript();
		} else if (eventSource == this.translateButton) {
			this.startTranslation();
		} else if (eventSource == this.cancelButton) {
			this.onConfirm();
		} else if (eventSource == this.helpButton) {
			this.showHelp();
		} else if ((eventSource == this.proGuardFileNameField || eventSource == this.proGuardArea) && this.translateButton.isEnabled()) {
			this.startTranslation();
		}
	}

	public void copyScript() {
		this.resultsArea.selectAll();
		this.resultsArea.copy();
	}

	private void initConfig(UIConfig uiConfig, boolean positionAndFocus) {
		this.uiConfig = uiConfig;
		String lastClasspathPath = uiConfig.getLastClasspathPath();
		if (lastClasspathPath != null) {
			lastClasspathPath.trim();
		}

		if (positionAndFocus) {
			this.positionDialog(0, 0, super.parentFrame != null);
			this.focusOpenButton();
		}
	}

	public void openProguardConfigFile() {
		File initialDirectory;
		if (this.uiConfig.getLastMappingPath() != null) {
			File lastMappingDir = new File(this.uiConfig.getLastMappingPath());
			if (lastMappingDir.exists() && lastMappingDir.isDirectory()) {
				initialDirectory = lastMappingDir;
			} else {
				initialDirectory = new File(SystemConstants.userDirectory);
			}
		} else {
			initialDirectory = new File(SystemConstants.userDirectory);
		}

		FileFilterBase[] fileFilters = new FileFilterBase[]{new TextFile(), new AllFiles()};
		FileSelectPanel fileSelectPanel = new FileSelectPanel(initialDirectory, false, 1, fileFilters, 0, true);
		if (fileSelectPanel.showOpenDialog(this, "Select a ProGuard Configuration File") == 1) {
			File selectedFile = fileSelectPanel.getFile();

			try {
				String configText = FileUtils.readToString(selectedFile);
				this.proGuardFileNameField.setText(selectedFile.getAbsolutePath());
				this.proGuardFileNameField.setCaretPosition(0);
				this.proGuardArea.setText(configText);
				this.proGuardArea.setCaretPosition(0);
			} catch (IOException ioException) {
				new CopyablePrompt(this, "File Error", "'" + selectedFile.getAbsolutePath() + "' : '" + ioException.getMessage() + "'");
			}

			File parentDirectory = selectedFile.getParentFile();
			if (parentDirectory != null && !parentDirectory.equals(initialDirectory)) {
				this.uiConfig.setLastMappingPath(parentDirectory.getAbsolutePath());
				this.uiConfig.saveConfig();
			}
		}
	}

	@Override
	public void keyPressed(KeyEvent e) {
		if (e.getKeyCode() == 10) {
			Component eventSource = (Component)e.getSource();
			if (eventSource == this.openButton) {
				this.openProguardConfigFile();
			} else if (eventSource == this.saveButton) {
				this.saveScript();
			} else if (eventSource == this.pasteButton) {
				this.pasteProguardConfig();
			} else if (eventSource == this.copyButton) {
				this.copyScript();
			} else if (eventSource == this.translateButton) {
				this.startTranslation();
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

	@Override
	public void mouseClicked(MouseEvent e) {
		if (e.getClickCount() == 2 && e.getSource() == this.proGuardFileNameField) {
			this.openProguardConfigFile();
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

	public void saveScript() {
		File initialDirectory;
		if (this.uiConfig.getLastScriptPath() != null) {
			File lastScriptDir = new File(this.uiConfig.getLastScriptPath());
			if (lastScriptDir.exists() && lastScriptDir.isDirectory()) {
				initialDirectory = lastScriptDir;
			} else {
				initialDirectory = new File(SystemConstants.userDirectory);
			}
		} else {
			initialDirectory = new File(SystemConstants.userDirectory);
		}

		FileFilterBase[] fileFilters = new FileFilterBase[]{new TextFile(), new AllFiles()};
		FileSelectPanel fileSelectPanel = new FileSelectPanel(initialDirectory, false, 1, fileFilters, 0, true);
		fileSelectPanel.setFile(new File(initialDirectory, "script.txt"));
		if (fileSelectPanel.showSaveDialog(this, "Save ZKM Script File") == 1) {
			File selectedFile = fileSelectPanel.getFile();
			BufferedWriter writer = null;

			try {
				String scriptText = this.resultsArea.getText();
				writer = new BufferedWriter(new FileWriter(selectedFile));
				writer.write(scriptText, 0, scriptText.length());
				writer.close();
				String parentDirectory = selectedFile.getParent();
				if (parentDirectory != null) {
					this.uiConfig.setLastScriptPath(parentDirectory);
					this.uiConfig.saveConfig();
				}
			} catch (IOException ioException) {
				new CopyablePrompt(this, "File Error", "Couldn't save \"" + selectedFile.getAbsolutePath() + "\" : " + ioException.getMessage());
			} finally {
				if (writer != null) {
					try {
						writer.close();
					} catch (IOException ignored) {
					}
				}
			}
		}
	}

	@Override
	protected void onConfirm() {
		this.close();
	}

	@Override
	public void close() {
		super.close();
		if (super.parentFrame != null) {
			((GUI)super.parentFrame).setDisable(false);
		} else {
			System.exit(0);
		}
	}

	public void startTranslation() {
		ProguardTranslatorDialog.TranslateConfigThread translateThread = new ProguardTranslatorDialog.TranslateConfigThread(this);
		this.resultsArea.setText("");
		this.statusLabel.setText("Analyzing.  Please wait...");
		this.setWaitCursor();
		translateThread.start();
	}

	public void setWaitCursor() {
		Cursor waitCursor = new Cursor(3);
		this.setCursor(waitCursor);
		this.statusPanel.setCursor(waitCursor);
		this.proGuardArea.setCursor(waitCursor);
		this.resultsArea.setCursor(waitCursor);
		this.proGuardFileNameField.setCursor(waitCursor);
	}

	public static class ConfigAreaListener extends BaseDocumentListener {
		public final ProguardTranslatorDialog translatorDialog;

		public ConfigAreaListener(ProguardTranslatorDialog translatorDialog) {
			this.translatorDialog = translatorDialog;
		}

		@Override
		public void onUpdate(DocumentEvent e) {
			if (this.translatorDialog.proGuardArea.getText().trim().length() > 0) {
				this.translatorDialog.translateButton.setEnabled(true);
			} else {
				this.translatorDialog.translateButton.setEnabled(false);
			}
		}
	}

	public static class TranslateConfigThread extends Thread {
		public final ProguardTranslatorDialog translatorDialog;
		public Runnable showResultTask;
		public String translatedScript;

		public TranslateConfigThread(ProguardTranslatorDialog translatorDialog) {
			this.translatorDialog = translatorDialog;
			this.showResultTask = new ProguardTranslatorDialog.TranslateConfigThread.ShowResultRunnable(this);
		}

		@Override
		public void run() {
			this.translatedScript = ProguardConfigUtils.translateProGuardConfig(this.translatorDialog.proGuardArea.getText(), null);
			SwingUtilities.invokeLater(this.showResultTask);
		}

		public static class ShowResultRunnable implements Runnable {
			public final ProguardTranslatorDialog.TranslateConfigThread translateConfigThread;

			public ShowResultRunnable(ProguardTranslatorDialog.TranslateConfigThread translateConfigThread) {
				this.translateConfigThread = translateConfigThread;
			}

			@Override
			public void run() {
				this.translateConfigThread.translatorDialog.setTranslatedScript(this.translateConfigThread.translatedScript);
			}
		}
	}
}
