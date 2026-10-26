package com.zelix.ui.dialogs.script;

import com.zelix.HelpManager;
import com.zelix.klassmaster.KlassMaster;
import com.zelix.ui.base.BaseDialog;
import com.zelix.ui.components.PlatformAdaptiveScrollPanel;
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
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFrame;

public class ScriptBuildHelperDialog extends BaseDialog implements ActionListener, KeyListener {
	public static String dialogStyles = "minWidth=btnWidth*3+20;mainPnl.top=0;mainPnl.left=0;mainPnl.right=container.right;mainPnl.bottom=container.bottom-btnHeight-10;btnWidth=max(okBtn.defaultWidth, cancelBtn.defaultWidth, helpBtn.defaultWidth);btnHeight=max(okBtn.defaultHeight, cancelBtn.defaultHeight, helpBtn.defaultHeight);okBtn.width=btnWidth;okBtn.height=btnHeight;okBtn.top=mainPnl.bottom+5;okBtn.centerX=container.width*16/100;cancelBtn.width=btnWidth;cancelBtn.height=btnHeight;cancelBtn.top=okBtn.top;cancelBtn.centerX=container.width*50/100;helpBtn.width=btnWidth;helpBtn.height=btnHeight;helpBtn.top=okBtn.top;helpBtn.centerX=container.width*84/100;";
	public static String[] messageAreaStyles = new String[]{"messageArea.top=5;messageArea.left=5;messageArea.right=container.width-5;messageArea.bottom=container.bottom"};
	public JFrame parentFrameRef;
	public ZKMConsumer resultConsumer;
	public boolean confirmed;
	public JButton cancelButton;
	public String helpId;
	public JButton helpButton;
	public JEditorPane helpPanel;
	public BorderedPanel mainPanel;
	public JButton okButton;

	public ScriptBuildHelperDialog(JFrame parentFrame, String title, String okButtonText, String cancelButtonText, String inlineHelpId, String helpId, int width, int height, ZKMConsumer resultConsumer) {
		super(parentFrame, title, okButtonText, cancelButtonText, inlineHelpId, helpId, width, height);
		this.parentFrameRef = parentFrame;
		this.resultConsumer = resultConsumer;
		this.centerOnParent();
	}

	public void onOk() {
		this.confirmed = true;
		this.close();
		this.resultConsumer.accept(1);
	}

	@Override
	protected void initGui(Object initArg1, Object initArg2, Object initArg3, Object initArg4, Object initArg5, Object initArg6, Object initArg7) {
		String okButtonText = (String)initArg1;
		String cancelButtonText = (String)initArg2;
		String helpPageId = (String)initArg3;
		this.helpId = (String)initArg4;
		int dialogWidth = (Integer)initArg5;
		int dialogHeight = (Integer)initArg6;
		Container contentPane = this.getContentPane();
		PowerLayout rootLayout = new PowerLayout(contentPane);
		contentPane.setLayout(rootLayout);
		this.okButton = new JButton(okButtonText);
		this.okButton.setToolTipText(HelpManager.getToolTip("OK_PROCEED"));
		this.cancelButton = new JButton(cancelButtonText);
		this.cancelButton.setToolTipText(HelpManager.getToolTip("CANCEL_DIALOG"));
		this.helpButton = new JButton("Help");
		this.helpButton.setToolTipText(HelpManager.getToolTip("SCREEN_HELP"));
		this.okButton.addActionListener(this);
		this.cancelButton.addActionListener(this);
		this.helpButton.addActionListener(this);
		this.okButton.addKeyListener(this);
		this.cancelButton.addKeyListener(this);
		this.helpButton.addKeyListener(this);
		contentPane.add(this.okButton, "okBtn");
		contentPane.add(this.cancelButton, "cancelBtn");
		contentPane.add(this.helpButton, "helpBtn");
		this.mainPanel = new BorderedPanel(false);
		contentPane.add(this.mainPanel, "mainPnl");
		rootLayout.applyStyles(dialogStyles + "layout.minWidth=minWidth;layout.width=max(minWidth," + dialogWidth + ");layout.height=" + dialogHeight + ";");
		this.createHelpPanel(helpPageId);
		this.setIconImage(KlassMaster.getIcon(this));
		GuiUtils.packWindow(this);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Object eventSource = e.getSource();
		if (eventSource == this.okButton) {
			this.onOk();
		} else if (eventSource == this.cancelButton) {
			this.onConfirm();
		} else if (eventSource == this.helpButton) {
			this.openHelpGUI();
		}
	}

	protected final void openHelpGUI() {
		HelpManager.showHelp(this.helpId);
	}

	@Override
	public void keyPressed(KeyEvent e) {
		if (e.getKeyCode() == 10) {
			if (e.getSource() == this.okButton) {
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
	public void onPositionCompleted() {
		GuiUtils.requestFocusInWindow(this.okButton);
	}

	public void createHelpPanel(String helpPageId) {
		this.mainPanel.setLayout(new BorderLayout());
		this.helpPanel = new JEditorPane();
		this.helpPanel.setEditable(false);

		try {
			this.helpPanel.setPage(HelpManager.buildHelpUrlSimple(helpPageId));
		} catch (IOException ioException) {
			this.helpPanel.setText(ioException.toString() + " : " + helpPageId);
		}

		this.mainPanel.add(new PlatformAdaptiveScrollPanel(this.helpPanel), "Center");
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
