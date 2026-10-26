package com.zelix.ui.dialogs;

import com.zelix.HelpManager;
import com.zelix.klassmaster.KlassMaster;
import com.zelix.ui.base.BaseDialog;
import com.zelix.ui.components.panels.BorderedPanel;
import com.zelix.ui.layouts.PowerLayout;
import com.zelix.utils.function.ZKMConsumer;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Enumeration;
import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

public class ObfuscateNameExcludeDialog extends BaseDialog implements ActionListener, KeyListener {
	public static String[] containerStyles = new String[]{
		"mainPnlWidth=mainPnl.defaultWidth+15",
		"widthOfBtns=btnWidth*3+20",
		"layout.minWidth=max(widthOfBtns, mainPnlWidth)",
		"layout.minHeight=255",
		"btnWidth=max(okBtn.defaultWidth, cancelBtn.defaultWidth, helpBtn.defaultWidth)",
		"btnHeight=max(okBtn.defaultHeight, cancelBtn.defaultHeight, helpBtn.defaultHeight)",
		"mainPnl.left=0",
		"mainPnl.top=0",
		"mainPnl.right=container.right",
		"mainPnl.bottom=container.bottom-btnHeight-10",
		"okBtn.top=mainPnl.bottom+5",
		"okBtn.centerX=container.width*16/100",
		"okBtn.width=btnWidth",
		"okBtn.height=btnHeight",
		"cancelBtn.top=okBtn.top",
		"cancelBtn.centerX=container.width*50/100",
		"cancelBtn.width=btnWidth",
		"cancelBtn.height=btnHeight",
		"helpBtn.top=okBtn.top",
		"helpBtn.centerX=container.width*84/100",
		"helpBtn.width=btnWidth",
		"helpBtn.height=btnHeight"
	};
	public static String[] panelStyles = new String[]{
		"typeLbl.centerX=container.width*50/100",
		"typeLbl.centerY=container.height*10/100",
		"chkPanel.centerX=container.width*50/100",
		"chkPanel.centerY=container.height*50/100"
	};
	public static String[] checkBoxStyles = new String[]{
		"packageTypeChk.top=0",
		"packageTypeChk.left=0",
		"classTypeChk.top=packageTypeChk.bottom+5",
		"classTypeChk.left=0",
		"fieldTypeChk.top=classTypeChk.bottom+5",
		"fieldTypeChk.left=0",
		"methodTypeChk.top=fieldTypeChk.bottom+5",
		"methodTypeChk.left=0",
		"classSuffixTypeChk.top=methodTypeChk.bottom+5",
		"classSuffixTypeChk.left=0"
	};
	public ZKMConsumer resultConsumer;
	public JButton okButton;
	public BorderedPanel mainPanel;
	public JRadioButton classSuffixRadio;
	public JButton exitButton;
	public JRadioButton fieldRadio;
	public JRadioButton packageRadio;
	public JRadioButton classRadio;
	public JButton helpButton;
	public JRadioButton methodRadio;
	public ButtonGroup typeButtonGroup;

	public ObfuscateNameExcludeDialog(JFrame parent, String title, ZKMConsumer resultConsumer) {
		super(parent, title);
		this.resultConsumer = resultConsumer;
		this.centerOnParent();
	}

	@Override
	protected void initGui(Object initArg1, Object initArg2, Object initArg3, Object initArg4, Object initArg5, Object initArg6, Object initArg7) {
		Container contentPane = this.getContentPane();
		PowerLayout rootLayout = new PowerLayout(contentPane);
		contentPane.setLayout(rootLayout);
		this.okButton = new JButton("OK");
		this.okButton.setToolTipText(HelpManager.getToolTip("OK_PROCEED"));
		this.exitButton = new JButton("Exit");
		this.exitButton.setToolTipText(HelpManager.getToolTip("CANCEL_DIALOG"));
		this.helpButton = new JButton("Help");
		this.helpButton.setToolTipText(HelpManager.getToolTip("SCREEN_HELP"));
		this.okButton.addActionListener(this);
		this.exitButton.addActionListener(this);
		this.helpButton.addActionListener(this);
		this.okButton.addKeyListener(this);
		this.exitButton.addKeyListener(this);
		this.helpButton.addKeyListener(this);
		contentPane.add(this.okButton, "okBtn");
		contentPane.add(this.exitButton, "cancelBtn");
		contentPane.add(this.helpButton, "helpBtn");
		this.mainPanel = new BorderedPanel(true);
		contentPane.add(this.mainPanel, "mainPnl");
		rootLayout.setProperties(containerStyles);
		this.createTypePanel();
		this.setIconImage(KlassMaster.getIcon(this));
		this.pack();
	}

	public void applySelectedType() {
		this.close();
		JRadioButton selectedRadioButton = null;
		Enumeration<AbstractButton> typeButtons = this.typeButtonGroup.getElements();

		while (typeButtons.hasMoreElements()) {
			JRadioButton typeButton = (JRadioButton)typeButtons.nextElement();
			if (typeButton.isSelected()) {
				selectedRadioButton = typeButton;
				break;
			}
		}

		if (selectedRadioButton == this.packageRadio) {
			this.resultConsumer.accept("1");
		} else if (selectedRadioButton == this.classRadio) {
			this.resultConsumer.accept("2");
		} else if (selectedRadioButton == this.fieldRadio) {
			this.resultConsumer.accept("3");
		} else if (selectedRadioButton == this.methodRadio) {
			this.resultConsumer.accept("4");
		} else if (selectedRadioButton == this.classSuffixRadio) {
			this.resultConsumer.accept("5");
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Object eventSource = e.getSource();
		if (eventSource == this.okButton) {
			this.applySelectedType();
		} else if (eventSource == this.exitButton) {
			this.onConfirm();
		} else if (eventSource == this.helpButton) {
			this.openHelpGUI();
		}
	}

	public void createTypePanel() {
		PowerLayout mainPanelLayout = new PowerLayout(this.mainPanel);
		this.mainPanel.setLayout(mainPanelLayout);
		JLabel typeLabel = new JLabel("Select the exclude parameter type:");
		this.mainPanel.add(typeLabel, "typeLbl");
		this.typeButtonGroup = new ButtonGroup();
		this.packageRadio = new JRadioButton("Package", true);
		this.classRadio = new JRadioButton("Class", false);
		this.fieldRadio = new JRadioButton("Field", false);
		this.methodRadio = new JRadioButton("Method", false);
		this.classSuffixRadio = new JRadioButton("Class Suffix", false);
		this.typeButtonGroup.add(this.packageRadio);
		this.typeButtonGroup.add(this.classRadio);
		this.typeButtonGroup.add(this.fieldRadio);
		this.typeButtonGroup.add(this.methodRadio);
		this.typeButtonGroup.add(this.classSuffixRadio);
		JPanel typeRadioPanel = new JPanel();
		this.addTypeRadios(typeRadioPanel);
		this.mainPanel.add(typeRadioPanel, "chkPanel");
		mainPanelLayout.setProperties(panelStyles);
	}

	public void addTypeRadios(JPanel panel) {
		PowerLayout panelLayout = new PowerLayout(panel);
		panel.setLayout(panelLayout);
		panel.add(this.packageRadio, "packageTypeChk");
		panel.add(this.classRadio, "classTypeChk");
		panel.add(this.fieldRadio, "fieldTypeChk");
		panel.add(this.methodRadio, "methodTypeChk");
		panel.add(this.classSuffixRadio, "classSuffixTypeChk");
		panelLayout.setProperties(checkBoxStyles);
	}

	@Override
	public void keyPressed(KeyEvent e) {
		if (e.getKeyCode() == 10) {
			if (e.getSource() == this.okButton) {
				this.applySelectedType();
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

	protected final void openHelpGUI() {
		HelpManager.showHelp("026");
	}

	@Override
	protected void onConfirm() {
		this.close();
		this.resultConsumer.accept();
	}
}
