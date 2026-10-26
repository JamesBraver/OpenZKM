package com.zelix.ui.dialogs;

import com.zelix.HelpManager;
import com.zelix.UIConfig;
import com.zelix.klassmaster.KlassMaster;
import com.zelix.ui.base.BaseDialog;
import com.zelix.ui.components.PlatformAdaptiveScrollPanel;
import com.zelix.ui.components.panels.BeveledPanel;
import com.zelix.ui.components.panels.BorderedPanel;
import com.zelix.ui.layouts.PowerLayout;
import com.zelix.ui.utils.GuiUtils;
import com.zelix.utils.function.ZKMConsumer;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JRadioButton;

public class InitialHelperDialog extends BaseDialog implements ActionListener, KeyListener {
	public static int BUILD_HELPER = 1;
	public static int SET_CLASSPATH = 2;
	private static String[] containerStyles = new String[]{
		"btnWidth=max(okBtn.defaultWidth, cancelBtn.defaultWidth, helpBtn.defaultWidth)",
		"btnHeight=max(okBtn.defaultHeight, cancelBtn.defaultHeight, helpBtn.defaultHeight)",
		"layout.minWidth=btnWidth*3+25",
		"mainPnl.top=5",
		"mainPnl.left=5",
		"mainPnl.right=container.right-5",
		"mainPnl.bottom=container.bottom-btnHeight-radioPnl.defaultHeight-20",
		"radioPnl.left=5",
		"radioPnl.right=container.right-5",
		"radioPnl.bottom=container.bottom-btnHeight-15",
		"okBtn.width=btnWidth",
		"okBtn.height=btnHeight",
		"okBtn.bottom=container.bottom-5",
		"okBtn.centerX=container.width*16/100",
		"cancelBtn.width=btnWidth",
		"cancelBtn.height=btnHeight",
		"cancelBtn.bottom=container.bottom-5",
		"cancelBtn.centerX=container.width*50/100",
		"helpBtn.width=btnWidth",
		"helpBtn.height=btnHeight",
		"helpBtn.bottom=container.bottom-5",
		"helpBtn.centerX=container.width*84/100"
	};
	private ZKMConsumer resultConsumer;
	private BorderedPanel mainPanel;
	private boolean confirmed;
	private JRadioButton buildHelperRadio;
	private ButtonGroup optionButtonGroup;
	private JButton okButton;
	private JRadioButton openClassesRadio;
	private JButton helpButton;
	private JButton cancelButton;
	private UIConfig uiConfig;

	public InitialHelperDialog(JFrame parentFrame, String title, UIConfig uiConfig, ZKMConsumer resultConsumer) {
		super(parentFrame, title, uiConfig);
		this.resultConsumer = resultConsumer;
		this.centerOnParent();
		GuiUtils.requestFocusInWindow(this.okButton);
	}

	@Override
	protected void initGui(Object initArg1, Object initArg2, Object initArg3, Object initArg4, Object initArg5, Object initArg6, Object initArg7) {
		this.uiConfig = (UIConfig)initArg1;
		Container contentPane = this.getContentPane();
		PowerLayout rootLayout = new PowerLayout(contentPane);
		contentPane.setLayout(rootLayout);
		this.buildHelperRadio = new JRadioButton("Run Build Helper");
		this.openClassesRadio = new JRadioButton("Set classpath and open classes");
		this.optionButtonGroup = new ButtonGroup();
		this.optionButtonGroup.add(this.openClassesRadio);
		this.optionButtonGroup.add(this.buildHelperRadio);
		this.okButton = new JButton("Next");
		this.okButton.setToolTipText(HelpManager.getToolTip("OK_PROCEED"));
		if (this.uiConfig.getUseBuildHelper()) {
			this.optionButtonGroup.setSelected(this.buildHelperRadio.getModel(), true);
		} else {
			this.optionButtonGroup.setSelected(this.openClassesRadio.getModel(), true);
		}

		this.cancelButton = new JButton("Cancel");
		this.cancelButton.setToolTipText(HelpManager.getToolTip("CANCEL_DIALOG"));
		this.helpButton = new JButton("Help");
		this.helpButton.setToolTipText(HelpManager.getToolTip("SCREEN_HELP"));
		this.okButton.addActionListener(this);
		this.cancelButton.addActionListener(this);
		this.helpButton.addActionListener(this);
		this.okButton.addKeyListener(this);
		this.cancelButton.addKeyListener(this);
		this.helpButton.addKeyListener(this);
		BeveledPanel radioPanel = new BeveledPanel(false, true);
		radioPanel.setLayout(new BoxLayout(radioPanel, 1));
		radioPanel.add(this.buildHelperRadio, "buildRadioBtn");
		radioPanel.add(this.openClassesRadio, "openRadioBtn");
		contentPane.add(radioPanel, "radioPnl");
		contentPane.add(this.okButton, "okBtn");
		contentPane.add(this.cancelButton, "cancelBtn");
		contentPane.add(this.helpButton, "helpBtn");
		this.mainPanel = new BorderedPanel(false);
		this.mainPanel.setLayout(new BorderLayout());
		JEditorPane helpPane = new JEditorPane();
		helpPane.setEditable(false);

		try {
			helpPane.setPage(HelpManager.buildHelpUrlSimple("000"));
		} catch (IOException ioException) {
			helpPane.setText(ioException.toString() + " : 000");
		}

		this.mainPanel.add(new PlatformAdaptiveScrollPanel(helpPane), "Center");
		contentPane.add(this.mainPanel, "mainPnl");
		rootLayout.setProperties(this.getContainerStyles());
		this.setIconImage(KlassMaster.getIcon(this));
		this.setSize(500, 400);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Object eventSource = e.getSource();
		if (eventSource == this.okButton) {
			this.applySelectedMode();
		} else if (eventSource == this.cancelButton) {
			this.onConfirm();
		} else if (eventSource == this.helpButton) {
			this.openHelpGUI();
		}
	}

	public void openHelpGUI() {
		HelpManager.showHelp("010");
	}

	@Override
	public void keyPressed(KeyEvent e) {
		if (e.getKeyCode() == 10) {
			if (e.getSource() == this.okButton) {
				this.applySelectedMode();
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
	public void onPositionCompleted() {
		GuiUtils.requestFocusInWindow(this.buildHelperRadio);
	}

	public void applySelectedMode() {
		this.confirmed = true;
		this.close();
		if (this.optionButtonGroup.isSelected(this.buildHelperRadio.getModel())) {
			this.uiConfig.setUseBuildHelper(true);
			this.uiConfig.saveConfig();
			this.resultConsumer.accept(BUILD_HELPER);
		} else {
			this.uiConfig.setUseBuildHelper(false);
			this.uiConfig.saveConfig();
			this.resultConsumer.accept(SET_CLASSPATH);
		}
	}

	@Override
	public void onConfirm() {
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

	public String[] getContainerStyles() {
		return containerStyles;
	}
}
