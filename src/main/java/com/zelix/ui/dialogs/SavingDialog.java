package com.zelix.ui.dialogs;

import lombok.SneakyThrows;

import com.zelix.HelpManager;
import com.zelix.klassmaster.KlassMaster;
import com.zelix.ui.GUI;
import com.zelix.ui.base.BaseDialog;
import com.zelix.ui.components.panels.FileSelectPanel;
import com.zelix.ui.filefilters.FileFilterBase;
import com.zelix.ui.filefilters.OnlyFolders;
import com.zelix.ui.layouts.PowerLayout;
import com.zelix.ui.utils.GuiUtils;
import com.zelix.utils.function.ZKMConsumer;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

public class SavingDialog extends BaseDialog implements ActionListener, KeyListener, PropertyChangeListener {
	private static String[] containerStyles = new String[]{
		"layout.minWidth=btnWidth*4+25",
		"layout.minHeight=300",
		"fileSelector.left=5",
		"fileSelector.top=0",
		"fileSelector.bottom=container.bottom-btnHeight-15",
		"fileSelector.right=container.right-5",
		"btnWidth=max(okBtn.defaultWidth, cancelBtn.defaultWidth, helpBtn.defaultWidth)",
		"btnHeight=max(okBtn.defaultHeight, cancelBtn.defaultHeight, helpBtn.defaultHeight)",
		"okBtn.centerX=container.width*16/100",
		"okBtn.bottom=container.bottom-5",
		"okBtn.width=btnWidth",
		"cancelBtn.centerX=container.width*50/100",
		"cancelBtn.bottom=container.bottom-5",
		"cancelBtn.width=btnWidth",
		"helpBtn.centerX=container.width*84/100",
		"helpBtn.bottom=container.bottom-5",
		"helpBtn.width=btnWidth"
	};
	public ZKMConsumer resultConsumer;
	public String defaultPath;
	public JButton helpButton;
	public JButton okButton;
	public FileSelectPanel fileSelectPanel;
	public JButton cancelButton;
	public boolean confirmed;

	public SavingDialog(JFrame parentFrame, String title, String okButtonText, String okButtonToolTip, String defaultPath, ZKMConsumer resultConsumer) {
		super(parentFrame, title, defaultPath, okButtonText, okButtonToolTip);
		this.resultConsumer = resultConsumer;
		this.defaultPath = defaultPath;
		this.centerOnParent();
		GuiUtils.requestFocusInWindow(this.okButton);
	}

	protected void createButtons(String okButtonText, String okButtonToolTip, PowerLayout layout, Container container) {
		this.okButton = new JButton(okButtonText);
		this.okButton.setToolTipText(okButtonToolTip);
		container.add(this.okButton, "okBtn");
		this.okButton.setEnabled(false);
		this.cancelButton = new JButton("Cancel");
		this.cancelButton.setToolTipText(HelpManager.getToolTip("CANCEL_DIALOG"));
		container.add(this.cancelButton, "cancelBtn");
		this.helpButton = new JButton("Help");
		this.helpButton.setToolTipText(HelpManager.getToolTip("SCREEN_HELP"));
		container.add(this.helpButton, "helpBtn");
		this.okButton.addActionListener(this);
		this.cancelButton.addActionListener(this);
		this.helpButton.addActionListener(this);
		this.okButton.addKeyListener(this);
		this.cancelButton.addKeyListener(this);
		this.helpButton.addKeyListener(this);
		layout.setProperties(containerStyles);
	}

	protected final void openHelpGUI() {
		HelpManager.showHelp("019");
	}

	@Override
	protected void initGui(Object initArg1, Object initArg2, Object initArg3, Object initArg4, Object initArg5, Object initArg6, Object initArg7) {
		Container contentPane = this.getContentPane();
		PowerLayout rootLayout = new PowerLayout(contentPane);
		contentPane.setLayout(rootLayout);
		File initialDirectory = null;
		File initialSelectedFile = null;
		String initialPath = (String)initArg1;
		String okButtonText = (String)initArg2;
		String okButtonToolTip = (String)initArg3;
		if (initialPath != null) {
			File candidateDirectory = new File(initialPath);
			if (candidateDirectory.exists() && candidateDirectory.isDirectory()) {
				initialSelectedFile = candidateDirectory;
				initialDirectory = candidateDirectory.getParentFile();
			}
		}

		FileFilterBase[] fileFilters = new FileFilterBase[]{new OnlyFolders()};
		this.fileSelectPanel = new FileSelectPanel(initialDirectory, false, 2, fileFilters, 0, false);
		contentPane.add(this.fileSelectPanel, "fileSelector");
		this.fileSelectPanel.addKeyListener(this);
		this.fileSelectPanel.addPropertyChangeListener(this);
		this.createButtons(okButtonText, okButtonToolTip, rootLayout, contentPane);
		this.fileSelectPanel.setSelectedFile(initialSelectedFile, true);
		this.setResizable(true);
		this.setIconImage(KlassMaster.getIcon(this));
		GuiUtils.packWindow(this);
	}

	@Override
	@SneakyThrows
	public void actionPerformed(ActionEvent e) {
		Object eventSource = e.getSource();
		if (eventSource instanceof JButton) {
			JButton button = (JButton)eventSource;
			if (button == this.okButton) {
				this.onOk();
			} else if (button == this.cancelButton) {
				this.onConfirm();
			} else if (button == this.helpButton) {
				this.openHelpGUI();
			}
		}
	}

	public boolean confirmOverwrite(File folder, String message) {
		if (((GUI)super.parentFrame).getWorkspace().getClassSourceDirectories().contains(folder)) {
			String[] options = new String[]{"Yes", "No"};
			if (JOptionPane.showOptionDialog(this, message, "Class overwrite warning", 2, 0, null, options, options[0]) == 1) {
				return false;
			}
		}

		return true;
	}

	@Override
	@SneakyThrows
	public void keyPressed(KeyEvent e) {
		if (e.getKeyCode() == 10) {
			if (e.getSource() == this.okButton || e.getSource() == this.fileSelectPanel) {
				this.onOk();
			} else if (e.getSource() == this.cancelButton) {
				this.onConfirm();
			} else if (e.getSource() == this.helpButton) {
				this.openHelpGUI();
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
	public void propertyChange(PropertyChangeEvent evt) {
		if (evt.getPropertyName().equals("selectionChanged")) {
			File[] selectedFiles = (File[])evt.getNewValue();
			if (selectedFiles != null && selectedFiles.length != 0) {
				this.okButton.setEnabled(true);
			} else {
				this.okButton.setEnabled(false);
			}
		}
	}

	@Override
	protected void onConfirm() {
		this.confirmed = true;
		this.close();
		this.resultConsumer.accept();
	}

	@Override
	public final void close() {
		super.close();
		if (!this.confirmed) {
			this.resultConsumer.accept();
		}
	}

	public void onOk() throws IOException {
		File selectedFile = this.fileSelectPanel.getFile();
		if (this.confirmOverwrite(selectedFile, "Some files will be saved to the same folder from which they were opened. Continue?")) {
			this.confirmed = true;
			this.close();
			this.resultConsumer.accept(selectedFile, 1);
		}
	}
}
