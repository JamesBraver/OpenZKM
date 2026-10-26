package com.zelix.ui.prompts;

import com.zelix.klassmaster.KlassMaster;
import com.zelix.klassmaster.constant.CopyrightConstants;
import com.zelix.repository.ClasspathManager;
import com.zelix.ui.base.BasePrompt;
import com.zelix.ui.canvas.ErrorCanvas;
import com.zelix.ui.components.panels.BeveledPanel;
import com.zelix.ui.components.panels.FileSelectPanel;
import com.zelix.ui.filefilters.AllFiles;
import com.zelix.ui.filefilters.FileFilterBase;
import com.zelix.ui.filefilters.FoldersAndArchives;
import com.zelix.ui.layouts.PowerLayout;
import com.zelix.ui.utils.GuiUtils;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import javax.swing.JButton;
import javax.swing.JFrame;

public class SelectBootstrapRuntimePrompt extends BasePrompt implements ActionListener, KeyListener, PropertyChangeListener {
	public static String[] layoutProperties = new String[]{
		"layout.minWidth=300",
		"layout.minHeight=300",
		"etchedPanel.top=5",
		"etchedPanel.left=10",
		"etchedPanel.right=container.right-10",
		"fileSelector.top=etchedPanel.bottom",
		"fileSelector.left=10",
		"fileSelector.right=container.right-btnWidth-30",
		"fileSelector.bottom=container.bottom-5",
		"btnWidth=max(okBtn.defaultWidth, cancelBtn.defaultWidth)",
		"okBtn.centerY=container.height-etchedPanel.bottom*25/100+etchedPanel.bottom",
		"okBtn.left=fileSelector.right+15",
		"okBtn.width=btnWidth",
		"cancelBtn.centerY=container.height-etchedPanel.bottom*75/100+etchedPanel.bottom",
		"cancelBtn.left=fileSelector.right+15",
		"cancelBtn.width=btnWidth"
	};
	public String bootstrapClassPath;
	public FileSelectPanel fileSelectPanel;
	public JButton cancelButton;
	public JButton okButton;

	private SelectBootstrapRuntimePrompt(JFrame parent, String title, String classPath) {
		super(parent, title, true);
		this.bootstrapClassPath = classPath;
		this.initComponents();
		this.centerOnParent();
	}

	public static String selectBootstrapClassPath(JFrame parent, String title, String classPath) {
		return (new SelectBootstrapRuntimePrompt(parent, title, classPath)).bootstrapClassPath;
	}

	@Override
	public void closeDialog() {
		super.closeDialog();
	}

	protected void initComponents() {
		Container contentPane = this.getContentPane();
		PowerLayout rootLayout = new PowerLayout(contentPane);
		contentPane.setLayout(rootLayout);
		ErrorCanvas errorCanvas = new ErrorCanvas(
			CopyrightConstants.NAME + " could not find the class java.lang.Object. Please select the archive file that contains the Java bootstrap classes that you wish " + CopyrightConstants.NAME + " to use.",
			250,
			KlassMaster.getFont()
		);
		BeveledPanel etchedPanel = new BeveledPanel(false, true, 5, 5);
		etchedPanel.setLayout(new BorderLayout());
		etchedPanel.add(errorCanvas, "Center");
		contentPane.add(etchedPanel, "etchedPanel");
		File initialDirectory = null;
		if (this.bootstrapClassPath != null) {
			if (this.bootstrapClassPath.indexOf(File.pathSeparator) > -1) {
				this.bootstrapClassPath = this.bootstrapClassPath.substring(0, this.bootstrapClassPath.indexOf(File.pathSeparatorChar));
			}

			File bootstrapFile = new File(this.bootstrapClassPath);
			if (bootstrapFile.exists()) {
				if (bootstrapFile.isDirectory()) {
					initialDirectory = bootstrapFile;
				} else {
					initialDirectory = bootstrapFile.getParentFile();
				}
			}
		}

		FileFilterBase[] fileFilters = new FileFilterBase[]{new FoldersAndArchives(), new AllFiles()};
		this.fileSelectPanel = new FileSelectPanel(initialDirectory, true, 3, fileFilters, 0, false);
		contentPane.add(this.fileSelectPanel, "fileSelector");
		this.fileSelectPanel.addKeyListener(this);
		this.fileSelectPanel.addPropertyChangeListener(this);
		this.okButton = new JButton("OK");
		contentPane.add(this.okButton, "okBtn");
		this.cancelButton = new JButton("Cancel");
		contentPane.add(this.cancelButton, "cancelBtn");
		this.okButton.addActionListener(this);
		this.cancelButton.addActionListener(this);
		this.okButton.addKeyListener(this);
		this.cancelButton.addKeyListener(this);
		rootLayout.setProperties(layoutProperties);
		this.okButton.setEnabled(false);
		GuiUtils.packWindow(this);
		this.setResizable(true);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Object eventSource = e.getSource();
		if (eventSource instanceof JButton) {
			JButton button = (JButton)eventSource;
			if (button == this.okButton) {
				this.onOk();
			}

			if (button == this.cancelButton) {
				this.onCancel();
			}
		}
	}

	public void onCancel() {
		this.closeDialog();
	}

	@Override
	public void keyPressed(KeyEvent e) {
		if (e.getKeyCode() == 10) {
			if (e.getSource() == this.okButton || e.getSource() == this.fileSelectPanel) {
				this.onOk();
			} else if (e.getSource() == this.cancelButton) {
				this.onCancel();
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
			boolean bootstrapArchiveFound = false;
			File[] selectedFiles = (File[])evt.getNewValue();

			for (int i = 0; i < selectedFiles.length; i++) {
				if (ClasspathManager.archiveContainsObject(selectedFiles[i].getAbsolutePath())) {
					bootstrapArchiveFound = true;
					break;
				}
			}

			if (bootstrapArchiveFound) {
				this.okButton.setEnabled(true);
			} else {
				this.okButton.setEnabled(false);
			}
		}
	}

	public void onOk() {
		File[] selectedFiles = this.fileSelectPanel.getFiles();
		StringBuffer classPathBuffer = new StringBuffer();

		for (int i = 0; i < selectedFiles.length; i++) {
			classPathBuffer.append(selectedFiles[i].getAbsolutePath());
			if (i < selectedFiles.length - 1) {
				classPathBuffer.append(File.pathSeparator);
			}
		}

		this.bootstrapClassPath = classPathBuffer.toString();
		this.closeDialog();
	}
}
