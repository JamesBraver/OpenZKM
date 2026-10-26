package com.zelix.ui.dialogs.script;

import com.zelix.HelpManager;
import com.zelix.UIConfig;
import com.zelix.klassmaster.KlassMaster;
import com.zelix.klassmaster.constant.SystemConstants;
import com.zelix.ui.base.BaseDialog;
import com.zelix.ui.components.PlatformAdaptiveScrollPanel;
import com.zelix.ui.components.panels.BorderedPanel;
import com.zelix.ui.components.panels.FileSelectPanel;
import com.zelix.ui.filefilters.AllFiles;
import com.zelix.ui.filefilters.FileFilterBase;
import com.zelix.ui.filefilters.TextFile;
import com.zelix.ui.layouts.PowerLayout;
import com.zelix.ui.prompts.CopyablePrompt;
import com.zelix.ui.utils.GuiUtils;
import com.zelix.utils.function.ZKMConsumer;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JTextArea;

public class ScriptOutputDialog extends BaseDialog implements ActionListener, KeyListener {
	public static String[] dialogStyles = new String[]{
		"layout.minWidth=btnWidth*4+25",
		"layout.minHeight=250",
		"mainPnl.top=0",
		"mainPnl.left=0",
		"mainPnl.right=container.right",
		"mainPnl.bottom=container.bottom-btnHeight-10",
		"btnWidth=max(previousBtn.defaultWidth, saveBtn.defaultWidth, cancelBtn.defaultWidth, helpBtn.defaultWidth)",
		"btnHeight=max(previousBtn.defaultHeight, saveBtn.defaultHeight, cancelBtn.defaultHeight, helpBtn.defaultHeight)",
		"previousBtn.top=mainPnl.bottom+5",
		"previousBtn.centerX=container.width*12/100",
		"previousBtn.width=btnWidth",
		"previousBtn.height=btnHeight",
		"saveBtn.top=previousBtn.top",
		"saveBtn.centerX=container.width*37/100",
		"saveBtn.width=btnWidth",
		"saveBtn.height=btnHeight",
		"cancelBtn.top=previousBtn.top",
		"cancelBtn.centerX=container.width*62/100",
		"cancelBtn.width=btnWidth",
		"cancelBtn.height=btnHeight",
		"helpBtn.top=previousBtn.top",
		"helpBtn.centerX=container.width*87/100",
		"helpBtn.width=btnWidth",
		"helpBtn.height=btnHeight"
	};
	public static String[] scriptAreaStyles = new String[]{"scriptArea.top=5", "scriptArea.left=5", "scriptArea.right=container.right-5", "scriptArea.bottom=container.bottom-5"};
	public JFrame parentFrameRef;
	public UIConfig uiConfig;
	public ZKMConsumer resultConsumer;
	public boolean confirmed;
	public JButton exitButton;
	public String scriptText;
	public BorderedPanel mainPanel;
	public JButton helpButton;
	public JTextArea scriptArea;
	public JButton saveButton;
	public JButton previousButton;

	public ScriptOutputDialog(JFrame parentFrame, String title, String scriptText, UIConfig uiConfig, ZKMConsumer resultConsumer) {
		super(parentFrame, title, scriptText);
		this.parentFrameRef = parentFrame;
		this.uiConfig = uiConfig;
		this.resultConsumer = resultConsumer;
		this.centerOnParent();
		GuiUtils.requestFocusInWindow(this.saveButton);
	}

	protected void openHelpGUI() {
		HelpManager.showHelp("034");
	}

	@Override
	protected void initGui(Object initArg1, Object initArg2, Object initArg3, Object initArg4, Object initArg5, Object initArg6, Object initArg7) {
		this.scriptText = (String)initArg1;
		Container contentPane = this.getContentPane();
		PowerLayout rootLayout = new PowerLayout(contentPane);
		contentPane.setLayout(rootLayout);
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		this.setSize(Math.min(600, screenSize.width), Math.min(480, screenSize.height));
		this.previousButton = new JButton("Previous");
		this.previousButton.setToolTipText(HelpManager.getToolTip("PREVIOUS_SCREEN"));
		this.saveButton = new JButton("Save");
		this.saveButton.setToolTipText(HelpManager.getToolTip("SAVE_SCRIPT"));
		this.exitButton = new JButton("Exit");
		this.exitButton.setToolTipText(HelpManager.getToolTip("CANCEL_DIALOG"));
		this.helpButton = new JButton("Help");
		this.helpButton.setToolTipText(HelpManager.getToolTip("SCREEN_HELP"));
		this.previousButton.addActionListener(this);
		this.saveButton.addActionListener(this);
		this.exitButton.addActionListener(this);
		this.helpButton.addActionListener(this);
		this.previousButton.addKeyListener(this);
		this.saveButton.addKeyListener(this);
		this.exitButton.addKeyListener(this);
		this.helpButton.addKeyListener(this);
		contentPane.add(this.previousButton, "previousBtn");
		contentPane.add(this.saveButton, "saveBtn");
		contentPane.add(this.exitButton, "cancelBtn");
		contentPane.add(this.helpButton, "helpBtn");
		this.mainPanel = new BorderedPanel(true);
		contentPane.add(this.mainPanel, "mainPnl");
		rootLayout.setProperties(dialogStyles);
		this.createScriptArea();
		this.setIconImage(KlassMaster.getIcon(this));
	}

	public void onPrevious() {
		this.confirmed = true;
		this.close();
		this.resultConsumer.accept(2);
	}

	public void saveScript() {
		File initialDirectory;
		if (this.uiConfig.getLastScriptPath() != null) {
			File lastScriptPathFile = new File(this.uiConfig.getLastScriptPath());
			if (lastScriptPathFile.exists() && lastScriptPathFile.isDirectory()) {
				initialDirectory = lastScriptPathFile;
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
				writer = new BufferedWriter(new FileWriter(selectedFile));
				writer.write(this.scriptText, 0, this.scriptText.length());
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
	public void actionPerformed(ActionEvent e) {
		Object eventSource = e.getSource();
		if (eventSource == this.previousButton) {
			this.onPrevious();
		} else if (eventSource == this.saveButton) {
			this.saveScript();
		} else if (eventSource == this.exitButton) {
			this.onConfirm();
		} else if (eventSource == this.helpButton) {
			this.openHelpGUI();
		}
	}

	@Override
	public void keyPressed(KeyEvent e) {
		if (e.getKeyCode() == 10) {
			if (e.getSource() == this.previousButton) {
				this.onPrevious();
			} else if (e.getSource() == this.saveButton) {
				this.saveScript();
			} else if (e.getSource() == this.exitButton) {
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

	public void createScriptArea() {
		PowerLayout scriptAreaLayout = new PowerLayout(this.mainPanel);
		this.mainPanel.setLayout(scriptAreaLayout);
		this.scriptArea = new JTextArea(this.scriptText);
		this.scriptArea.setFont(new Font("Courier", 0, 14));
		this.scriptArea.setEditable(false);
		this.mainPanel.add(new PlatformAdaptiveScrollPanel(this.scriptArea), "scriptArea");
		scriptAreaLayout.setProperties(scriptAreaStyles);
	}

	@Override
	protected void onConfirm() {
		this.confirmed = true;
		this.close();
		this.resultConsumer.accept();
	}

	@Override
	public void close() {
		super.close();
		if (!this.confirmed) {
			this.resultConsumer.accept();
		}
	}
}
