package com.zelix.ui.frames.base;

import com.zelix.HelpManager;
import com.zelix.klassmaster.KlassMaster;
import com.zelix.klassmaster.changelogs.AbstractChangeLogContext;
import com.zelix.klassmaster.changelogs.parser.context.ChangeLogContext;
import com.zelix.klassmaster.config.FilterConfig;
import com.zelix.klassmaster.config.ObfuscateConfig;
import com.zelix.klassmaster.config.Options;
import com.zelix.klassmaster.constant.SystemConstants;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.ui.GUI;
import com.zelix.ui.base.BaseFrame;
import com.zelix.ui.components.panels.BorderedPanel;
import com.zelix.ui.components.panels.FileSelectPanel;
import com.zelix.ui.filefilters.AllFiles;
import com.zelix.ui.filefilters.FileFilterBase;
import com.zelix.ui.filefilters.TextFile;
import com.zelix.ui.layouts.PowerLayout;
import com.zelix.ui.prompts.CopyablePrompt;
import com.zelix.ui.utils.GuiUtils;
import com.zelix.utils.FileUtils;
import com.zelix.utils.dataset.ChangeLogFileData;
import com.zelix.utils.function.ZKMConsumer;
import lombok.SneakyThrows;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;

public abstract class BaseChangeLogInputFrame extends BaseFrame {
	public Font font = KlassMaster.getFont();
	public BorderedPanel mainPanel = new BorderedPanel(true);
	public JCheckBox useChangeLogFileCheckBox = new JCheckBox("use input change log file:");
	public JCheckBox produceChangeLogFileCheckBox = new JCheckBox("produce a change log file:");
	public JTextField changeLogFileInField = new JTextField();
	public JButton changeLogInBrowseButton = new JButton(". . .");
	public JTextField changeLogFileOutField = new JTextField();
	public JButton changeLogOutBrowseButton = new JButton(". . .");
	public ObfuscateConfig obfConfig;
	public FilterConfig filterConfig;
	public GUI gui;
	public ScriptContext scriptContext;
	public ZKMConsumer resultConsumer;
	public JCheckBox obfuscateMethodParameters;
	public JButton cancelButton;
	public JCheckBox obfuscateReferencesCheckBox;
	public DefaultComboBoxModel<String> keepInnerClassInformationModel;
	public JButton helpButton;
	public boolean skipCloseCallback;
	public FontMetrics fontMetrics;
	public DefaultComboBoxModel<String> methodParameterChangesModel;
	public JButton okButton;
	public JCheckBox autoReflectionHandling;
	public JComboBox<String> keepInnerClassInformation;
	public JComboBox<String> methodParameterChanges;
	public PrintWriter changeLogPrintWriter;
	public JButton previousButton;
	public Reader changeLogReader;

	public BaseChangeLogInputFrame(String title, String okButtonToolTip, GUI gui, ObfuscateConfig obfuscateConfig, FilterConfig filterConfig, ScriptContext scriptContext, ZKMConsumer resultConsumer) {
		this.obfConfig = obfuscateConfig;
		this.filterConfig = filterConfig;
		this.gui = gui;
		this.scriptContext = scriptContext;
		this.resultConsumer = resultConsumer;
		this.setFont(this.font);
		this.initFrame(title, okButtonToolTip);
		GuiUtils.requestFocusInWindow(this.okButton);
	}

	public void createButtons(String okButtonToolTip) {
		this.previousButton = new JButton("Previous");
		this.previousButton.setToolTipText(HelpManager.getToolTip("PREVIOUS_SCREEN"));
		this.okButton = new JButton("OK");
		this.okButton.setToolTipText(okButtonToolTip);
		this.cancelButton = new JButton("Cancel");
		this.cancelButton.setToolTipText(HelpManager.getToolTip("CANCEL_DIALOG"));
		this.helpButton = new JButton("Help");
		this.helpButton.setToolTipText(HelpManager.getToolTip("SCREEN_HELP"));
	}

	public void loadOptions() {
		if (this.obfConfig.enableChangeLogFile) {
			this.useChangeLogFileCheckBox.setSelected(true);
		} else {
			this.useChangeLogFileCheckBox.setSelected(false);
		}

		if (this.obfConfig.changeLogFileIn != null && this.obfConfig.changeLogFileIn.length > 0) {
			this.changeLogFileInField.setText(this.obfConfig.changeLogFileIn[0].getPath());
		}

		this.changeLogFileInField.setEnabled(this.useChangeLogFileCheckBox.isSelected());
		this.changeLogInBrowseButton.setEnabled(this.useChangeLogFileCheckBox.isSelected());
		if (this.obfConfig.enableChangeLogOut) {
			this.produceChangeLogFileCheckBox.setSelected(true);
		} else {
			this.produceChangeLogFileCheckBox.setSelected(false);
		}

		if (this.obfConfig.changeLogFileOut != null) {
			this.changeLogFileOutField.setText(this.obfConfig.changeLogFileOut);
		}

		this.changeLogFileOutField.setEnabled(this.produceChangeLogFileCheckBox.isSelected());
		this.changeLogOutBrowseButton.setEnabled(this.produceChangeLogFileCheckBox.isSelected());
		this.keepInnerClassInformation.setSelectedIndex(this.obfConfig.keepInnerClassInfo);
		this.obfuscateReferencesCheckBox.setSelected(this.obfConfig.obfuscateReferences == 1);
		this.autoReflectionHandling.setSelected(this.obfConfig.autoReflectionHandling == 1);
		this.methodParameterChanges.setSelectedIndex(this.obfConfig.methodParameterChanges);
		this.obfuscateMethodParameters.setSelected(this.obfConfig.obfuscateParameters == 1);
		this.loadOptionsFromConfig();
	}

	public final void onOk() throws IOException {
		if (this.validateOptions()) {
			this.skipCloseCallback = true;
			this.close();
			this.saveOptions();
			this.resultConsumer.accept(this.obfConfig, 1);
		}
	}

	protected boolean openChangeLogIn(String fileName, File file) {
		BufferedReader logFileReader = null;

		try {
			String logFileEncoding = ChangeLogContext.getLogFileEncoding(file);
			if (logFileEncoding != null) {
				logFileReader = new BufferedReader(new InputStreamReader(new FileInputStream(file), logFileEncoding));
			} else {
				logFileReader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
			}

			StringBuffer contentBuffer = new StringBuffer();

			String line;
			while ((line = logFileReader.readLine()) != null) {
				contentBuffer.append(line);
				contentBuffer.append(Options.lineSeparator);
			}

			this.closeChangeLogReader();
			this.changeLogReader = new StringReader(contentBuffer.toString());
			return true;
		} catch (IOException ignored) {
			new CopyablePrompt(this, "File Error", "Error opening " + fileName);
			this.changeLogFileInField.selectAll();
			GuiUtils.requestFocusInWindow(this.changeLogFileInField);
			return false;
		} finally {
			if (logFileReader != null) {
				try {
					logFileReader.close();
				} catch (IOException ignoredClose) {
				}
			}
		}
	}

	public void saveOptionsToConfig() {
	}

	private void browseChangeLogInFile() {
		this.browseFile("Select Input Change Log File", this.changeLogFileInField);
	}

	protected boolean openChangeLogOut(String fileName, File file) {
		try {
			String fileEncoding = AbstractChangeLogContext.getEncoding();
			if (fileEncoding != null) {
				this.changeLogPrintWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileName), fileEncoding), 2048));
				this.obfConfig.useFileEncoding = fileEncoding;
			} else {
				OutputStreamWriter fileWriter = new OutputStreamWriter(new FileOutputStream(fileName));
				this.changeLogPrintWriter = new PrintWriter(new BufferedWriter(fileWriter, 2048));
				this.obfConfig.useFileEncoding = "UTF-8";
			}

			return true;
		} catch (IOException ignored) {
			new CopyablePrompt(this, "File Error", "Error opening " + fileName);
			this.changeLogFileOutField.selectAll();
			GuiUtils.requestFocusInWindow(this.changeLogFileOutField);
			return false;
		}
	}

	public void loadOptionsFromConfig() {
	}

	public abstract void buildLayout(StringBuffer layoutProperties);

	protected abstract void showHelp();

	private boolean checkFileNameClash(String fileName, String otherFileName, String otherFileLabel, JTextField field) {
		if (fileName.equals(otherFileName) || !FileUtils.IS_UNIX && fileName.equalsIgnoreCase(otherFileName)) {
			new CopyablePrompt(this, "Error", "Output change log file \"" + fileName + "\" clashes with " + otherFileLabel + " file name.");
			field.selectAll();
			GuiUtils.requestFocusInWindow(field);
			return true;
		} else {
			return false;
		}
	}

	public void initFrame(String title, String okButtonToolTip) {
		this.setTitle(title);
		Container contentPane = this.getContentPane();
		PowerLayout powerLayout = new PowerLayout(contentPane);
		contentPane.setLayout(powerLayout);
		Dimension minFrameSize = this.getMinFrameSize();
		this.createButtons(okButtonToolTip);
		this.changeLogInBrowseButton.setToolTipText(HelpManager.getToolTip("SELECT_CHANGE_LOG_IN"));
		this.changeLogOutBrowseButton.setToolTipText(HelpManager.getToolTip("SELECT_CHANGE_LOG_OUT"));
		BaseChangeLogInputFrame.EscapeAction.WindowClosingListener windowClosingListener = new BaseChangeLogInputFrame.EscapeAction.WindowClosingListener(
			this
		);
		this.addWindowListener(windowClosingListener);
		BaseChangeLogInputFrame.EscapeAction.ButtonActionListener buttonActionListener = new BaseChangeLogInputFrame.EscapeAction.ButtonActionListener(
			this
		);
		this.changeLogInBrowseButton.addActionListener(buttonActionListener);
		this.changeLogOutBrowseButton.addActionListener(buttonActionListener);
		this.previousButton.addActionListener(buttonActionListener);
		this.okButton.addActionListener(buttonActionListener);
		this.cancelButton.addActionListener(buttonActionListener);
		this.helpButton.addActionListener(buttonActionListener);
		BaseChangeLogInputFrame.EscapeAction.ButtonEnterKeyListener buttonEnterKeyListener = new BaseChangeLogInputFrame.EscapeAction.ButtonEnterKeyListener(
			this
		);
		this.previousButton.addKeyListener(buttonEnterKeyListener);
		this.okButton.addKeyListener(buttonEnterKeyListener);
		this.cancelButton.addKeyListener(buttonEnterKeyListener);
		this.helpButton.addKeyListener(buttonEnterKeyListener);
		BaseChangeLogInputFrame.ChangeLogItemListener changeLogItemListener = new BaseChangeLogInputFrame.ChangeLogItemListener(this);
		this.useChangeLogFileCheckBox.addItemListener(changeLogItemListener);
		this.produceChangeLogFileCheckBox.addItemListener(changeLogItemListener);
		JScrollPane scrollPane = new JScrollPane(this.mainPanel);
		contentPane.add(scrollPane, "mainPnl");
		this.fontMetrics = this.getFontMetrics(this.font);
		this.layoutMainPanel();
		contentPane.add(this.previousButton, "previousBtn");
		contentPane.add(this.okButton, "okBtn");
		contentPane.add(this.cancelButton, "cancelBtn");
		contentPane.add(this.helpButton, "helpBtn");
		String minSizeStyles = "layout.minWidth=" + minFrameSize.width + ";layout.minHeight=" + minFrameSize.height;
		powerLayout.applyStyles(
			"layout.minWidth=btnWidth*4+25;btnWidth=max(previousBtn.defaultWidth, okBtn.defaultWidth, cancelBtn.defaultWidth, helpBtn.defaultWidth);btnHeight=max(previousBtn.defaultHeight, okBtn.defaultHeight, cancelBtn.defaultHeight, helpBtn.defaultHeight);mainPnl.top=0;mainPnl.left=0;mainPnl.bottom=container.bottom-btnHeight-10;mainPnl.right=container.right;previousBtn.top=mainPnl.bottom+5;previousBtn.centerX=container.width*12/100;previousBtn.width=btnWidth;previousBtn.height=btnHeight;okBtn.top=mainPnl.bottom+5;okBtn.centerX=container.width*37/100;okBtn.width=btnWidth;okBtn.height=btnHeight;cancelBtn.top=mainPnl.bottom+5;cancelBtn.centerX=container.width*62/100;cancelBtn.width=btnWidth;cancelBtn.height=btnHeight;helpBtn.top=mainPnl.bottom+5;helpBtn.centerX=container.width*87/100;helpBtn.width=btnWidth;helpBtn.height=btnHeight;"
				+ minSizeStyles
		);
		this.gui.setEnabled(false);
		this.loadOptions();
		this.setIconImage(KlassMaster.getIcon(this));
		this.pack();
		Dimension frameSize = this.getSize();
		Point guiLocation = this.gui.getLocationOnScreen();
		Dimension guiSize = this.gui.getSize();
		int frameX = guiSize.width / 2 - frameSize.width / 2 + guiLocation.x;
		int frameY = guiSize.height / 2 - frameSize.height / 2 + guiLocation.y;
		frameX = Math.max(0, frameX);
		frameY = Math.max(0, frameY);
		this.setLocation(frameX, frameY);
		GuiUtils.setVisible(this, true);
	}

	public void layoutMainPanel() {
		PowerLayout powerLayout = new PowerLayout(this.mainPanel);
		this.mainPanel.setLayout(powerLayout);
		StringBuffer layoutProperties = new StringBuffer();
		this.buildLayout(layoutProperties);
		powerLayout.applyStyles(layoutProperties.toString());
	}

	private void browseFile(String dialogTitle, JTextField targetField) {
		FileFilterBase[] fileFilters = new FileFilterBase[]{new TextFile(), new AllFiles()};
		FileSelectPanel fileSelectPanel = new FileSelectPanel(new File(SystemConstants.userDirectory), false, 1, fileFilters, 0, true);
		if (fileSelectPanel.showOpenDialog(this, dialogTitle) == 1) {
			File selectedFile = fileSelectPanel.getFile();
			targetField.setText(selectedFile.getAbsolutePath());
		}
	}

	public final void onCancel() {
		this.skipCloseCallback = true;
		this.close();
		this.resultConsumer.accept();
	}

	public void saveOptions() {
		this.obfConfig.enableChangeLogFile = this.useChangeLogFileCheckBox.isSelected();
		String changeLogInFileName = this.changeLogFileInField.getText().trim();
		if (changeLogInFileName.length() > 0) {
			ChangeLogFileData changeLogFileData = new ChangeLogFileData(changeLogInFileName);
			if (this.obfConfig.enableChangeLogFile) {
				changeLogFileData.setReader(this.changeLogReader);
			}

			this.obfConfig.changeLogFileIn = new ChangeLogFileData[1];
			this.obfConfig.changeLogFileIn[0] = changeLogFileData;
		} else {
			this.obfConfig.changeLogFileIn = null;
		}

		this.obfConfig.enableChangeLogOut = this.produceChangeLogFileCheckBox.isSelected();
		if (this.obfConfig.enableChangeLogOut && this.changeLogFileOutField.getText().trim().length() > 0) {
			this.obfConfig.changeLogPrintWriter = this.changeLogPrintWriter;
		} else {
			this.obfConfig.changeLogPrintWriter = null;
		}

		this.obfConfig.changeLogFileOut = this.changeLogFileOutField.getText().trim();
		this.obfConfig.keepInnerClassInfo = this.keepInnerClassInformation.getSelectedIndex();
		if (this.autoReflectionHandling.isSelected()) {
			this.obfConfig.autoReflectionHandling = 1;
		} else {
			this.obfConfig.autoReflectionHandling = 0;
		}

		this.obfConfig.methodParameterChanges = this.methodParameterChanges.getSelectedIndex();
		if (this.obfuscateReferencesCheckBox.isSelected()) {
			this.obfConfig.obfuscateReferences = 1;
		} else {
			this.obfConfig.obfuscateReferences = 0;
		}

		if (this.obfuscateMethodParameters.isSelected()) {
			this.obfConfig.obfuscateParameters = 1;
		} else {
			this.obfConfig.obfuscateParameters = 0;
		}

		this.saveOptionsToConfig();
	}

	public final void onPrevious() throws IOException {
		if (this.validateOptions()) {
			this.skipCloseCallback = true;
			this.close();
			this.saveOptions();
			this.resultConsumer.accept(this.obfConfig, 2);
		}
	}

	@Override
	public Action getAction() {
		return new BaseChangeLogInputFrame.EscapeAction(this);
	}

	public abstract Dimension getMinFrameSize();

	private void closeChangeLogReader() {
		if (this.changeLogReader != null) {
			try {
				this.changeLogReader.close();
			} catch (IOException ignored) {
			}
		}
	}

	public boolean validateOptions() {
		if (this.useChangeLogFileCheckBox.isSelected()) {
			if (this.changeLogFileInField.getText().trim().equals("")) {
				this.changeLogFileInField.selectAll();
				new CopyablePrompt(this, "Error", "A file name for the input change log must be entered");
				GuiUtils.requestFocusInWindow(this.changeLogFileInField);
				return false;
			}

			String changeLogInFileName = this.changeLogFileInField.getText().trim();
			File changeLogInFile = new File(changeLogInFileName);
			if (changeLogInFile.exists() && !changeLogInFile.canRead()) {
				new CopyablePrompt(this, "File Error", "Can't read from " + changeLogInFileName);
				this.changeLogFileInField.selectAll();
				GuiUtils.requestFocusInWindow(this.changeLogFileInField);
				return false;
			}

			if (this.checkFileNameClash(changeLogInFile.getAbsolutePath(), this.scriptContext.getLogFilePath(), "ZKM log", this.changeLogFileInField)) {
				return false;
			}

			if (this.checkFileNameClash(changeLogInFile.getAbsolutePath(), this.scriptContext.getTrimLogFilePath(), "trim log", this.changeLogFileInField)) {
				return false;
			}

			if (!this.openChangeLogIn(changeLogInFileName, changeLogInFile)) {
				return false;
			}
		} else {
			this.closeChangeLogReader();
		}

		if (this.produceChangeLogFileCheckBox.isSelected()) {
			if (this.changeLogFileOutField.getText().trim().equals("")) {
				this.changeLogFileOutField.selectAll();
				new CopyablePrompt(this, "Error", "A file name for the change log must be entered");
				GuiUtils.requestFocusInWindow(this.changeLogFileOutField);
				return false;
			} else {
				String changeLogOutFileName = this.changeLogFileOutField.getText().trim();
				File changeLogOutFile = new File(changeLogOutFileName);
				if (changeLogOutFile.exists() && !changeLogOutFile.canWrite()) {
					new CopyablePrompt(this, "File Error", "Can't write to " + changeLogOutFileName);
					this.changeLogFileOutField.selectAll();
					GuiUtils.requestFocusInWindow(this.changeLogFileOutField);
					return false;
				} else if (this.checkFileNameClash(changeLogOutFile.getAbsolutePath(), this.scriptContext.getLogFilePath(), "ZKM log", this.changeLogFileOutField)) {
					return false;
				} else {
					return this.checkFileNameClash(changeLogOutFile.getAbsolutePath(), this.scriptContext.getTrimLogFilePath(), "trim log", this.changeLogFileOutField) ? false : this.openChangeLogOut(changeLogOutFileName, changeLogOutFile);
				}
			}
		} else {
			return true;
		}
	}

	@Override
	public final void close() {
		super.close();
		if (!this.skipCloseCallback) {
			this.resultConsumer.accept();
		}
	}

	private void browseChangeLogOutFile() {
		this.browseFile("Select Output Change Log File", this.changeLogFileOutField);
	}

	public static void onSelectChangeLogInFile(BaseChangeLogInputFrame frame) {
		frame.browseChangeLogInFile();
	}

	public static void onSelectChangeLogOutFile(BaseChangeLogInputFrame frame) {
		frame.browseChangeLogOutFile();
	}

	public static class EscapeAction extends AbstractAction {
		public final BaseChangeLogInputFrame baseChangeLogInputFrame;

		public EscapeAction(BaseChangeLogInputFrame baseChangeLogInputFrame) {
			this.baseChangeLogInputFrame = baseChangeLogInputFrame;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (this.baseChangeLogInputFrame.isEnabled()) {
				this.baseChangeLogInputFrame.onCancel();
			}
		}

		public static class ButtonEnterKeyListener extends KeyAdapter {
			public final BaseChangeLogInputFrame baseChangeLogInputFrame;

			public ButtonEnterKeyListener(BaseChangeLogInputFrame baseChangeLogInputFrame) {
				this.baseChangeLogInputFrame = baseChangeLogInputFrame;
			}

			@Override
			@SneakyThrows
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == 10) {
					Object var17 = e.getSource();
					if (var17 == this.baseChangeLogInputFrame.changeLogInBrowseButton) {
						BaseChangeLogInputFrame.onSelectChangeLogInFile(this.baseChangeLogInputFrame);
					} else if (var17 == this.baseChangeLogInputFrame.changeLogOutBrowseButton) {
						BaseChangeLogInputFrame.onSelectChangeLogOutFile(this.baseChangeLogInputFrame);
					} else if (var17 == this.baseChangeLogInputFrame.previousButton) {
						this.baseChangeLogInputFrame.onPrevious();
					} else if (var17 == this.baseChangeLogInputFrame.okButton) {
						this.baseChangeLogInputFrame.onOk();
					} else if (var17 == this.baseChangeLogInputFrame.cancelButton) {
						this.baseChangeLogInputFrame.onCancel();
					} else if (var17 == this.baseChangeLogInputFrame.helpButton) {
						this.baseChangeLogInputFrame.showHelp();
					}
				}
			}
		}

		public static class ButtonActionListener implements ActionListener {
			public final BaseChangeLogInputFrame baseChangeLogInputFrame;

			public ButtonActionListener(BaseChangeLogInputFrame baseChangeLogInputFrame) {
				this.baseChangeLogInputFrame = baseChangeLogInputFrame;
			}

			@Override
			@SneakyThrows
			public void actionPerformed(ActionEvent e) {
				Object var17 = e.getSource();
				if (var17 == this.baseChangeLogInputFrame.changeLogInBrowseButton) {
					BaseChangeLogInputFrame.onSelectChangeLogInFile(this.baseChangeLogInputFrame);
				} else if (var17 == this.baseChangeLogInputFrame.changeLogOutBrowseButton) {
					BaseChangeLogInputFrame.onSelectChangeLogOutFile(this.baseChangeLogInputFrame);
				} else if (var17 == this.baseChangeLogInputFrame.previousButton) {
					this.baseChangeLogInputFrame.onPrevious();
				} else if (var17 == this.baseChangeLogInputFrame.okButton) {
					this.baseChangeLogInputFrame.onOk();
				} else if (var17 == this.baseChangeLogInputFrame.cancelButton) {
					this.baseChangeLogInputFrame.onCancel();
				} else if (var17 == this.baseChangeLogInputFrame.helpButton) {
					this.baseChangeLogInputFrame.showHelp();
				}
			}
		}

		public static class WindowClosingListener extends WindowAdapter {
			public final BaseChangeLogInputFrame baseChangeLogInputFrame;

			public WindowClosingListener(BaseChangeLogInputFrame baseChangeLogInputFrame) {
				this.baseChangeLogInputFrame = baseChangeLogInputFrame;
			}

			@Override
			public void windowClosing(WindowEvent e) {
				this.baseChangeLogInputFrame.close();
				this.baseChangeLogInputFrame.resultConsumer.accept();
			}
		}
	}

	public static class ChangeLogItemListener implements ItemListener {
		public final BaseChangeLogInputFrame baseChangeLogInputFrame;

		public ChangeLogItemListener(BaseChangeLogInputFrame baseChangeLogInputFrame) {
			this.baseChangeLogInputFrame = baseChangeLogInputFrame;
		}

		@Override
		public void itemStateChanged(ItemEvent e) {
			Object eventSource = e.getSource();
			if (eventSource == this.baseChangeLogInputFrame.useChangeLogFileCheckBox) {
				this.baseChangeLogInputFrame.changeLogFileInField.setEnabled(this.baseChangeLogInputFrame.useChangeLogFileCheckBox.isSelected());
				this.baseChangeLogInputFrame.changeLogInBrowseButton.setEnabled(this.baseChangeLogInputFrame.useChangeLogFileCheckBox.isSelected());
				if (!this.baseChangeLogInputFrame.useChangeLogFileCheckBox.isSelected()) {
					this.baseChangeLogInputFrame.changeLogFileInField.select(0, 0);
				}
			} else if (eventSource == this.baseChangeLogInputFrame.produceChangeLogFileCheckBox) {
				this.baseChangeLogInputFrame.changeLogFileOutField.setEnabled(this.baseChangeLogInputFrame.produceChangeLogFileCheckBox.isSelected());
				this.baseChangeLogInputFrame.changeLogOutBrowseButton.setEnabled(this.baseChangeLogInputFrame.produceChangeLogFileCheckBox.isSelected());
				if (!this.baseChangeLogInputFrame.produceChangeLogFileCheckBox.isSelected()) {
					this.baseChangeLogInputFrame.changeLogFileOutField.select(0, 0);
				}
			}
		}
	}
}
