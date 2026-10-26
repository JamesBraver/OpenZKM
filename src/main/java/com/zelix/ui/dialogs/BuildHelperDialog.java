package com.zelix.ui.dialogs;

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

public class BuildHelperDialog extends BaseDialog implements ActionListener, KeyListener {
	public static String containerStyles = "minWidth=btnWidth*3+20;mainPnl.top=0;mainPnl.left=0;mainPnl.right=container.right;mainPnl.bottom=container.bottom-btnHeight-10;btnWidth=max(previousBtn.defaultWidth, okBtn.defaultWidth, cancelBtn.defaultWidth);btnHeight=max(previousBtn.defaultHeight, okBtn.defaultHeight, cancelBtn.defaultHeight);previousBtn.width=btnWidth;previousBtn.height=btnHeight;previousBtn.top=mainPnl.bottom+5;previousBtn.centerX=container.width*16/100;okBtn.width=btnWidth;okBtn.height=btnHeight;okBtn.top=previousBtn.top;okBtn.centerX=container.width*50/100;cancelBtn.width=btnWidth;cancelBtn.height=btnHeight;cancelBtn.top=previousBtn.top;cancelBtn.centerX=container.width*84/100;";
	public static String[] messageAreaStyles = new String[]{
		"messageArea.top=5", "messageArea.left=5", "messageArea.right=container.width-5", "messageArea.bottom=container.bottom"
	};
	public JFrame dialogParentFrame;
	public ZKMConsumer resultConsumer;
	public JButton okButton;
	public boolean confirmed;
	public BorderedPanel mainPanel;
	public JButton previousButton;
	public JButton cancelButton;
	public JEditorPane helpPane;

	public BuildHelperDialog(JFrame parentFrame, String title, String okButtonText, String okButtonToolTip, String helpId, int width, int height, ZKMConsumer resultConsumer) {
		super(parentFrame, title, okButtonText, okButtonToolTip, helpId, width, height);
		this.dialogParentFrame = parentFrame;
		this.resultConsumer = resultConsumer;
		this.centerOnParent();
		GuiUtils.requestFocusInWindow(this.okButton);
	}

	public void onPrevious() {
		this.confirmed = true;
		this.close();
		this.resultConsumer.accept(2);
	}

	@Override
	protected void initGui(Object initArg1, Object initArg2, Object initArg3, Object initArg4, Object initArg5, Object initArg6, Object initArg7) {
		String okButtonText = (String)initArg1;
		String okButtonToolTip = (String)initArg2;
		String helpId = (String)initArg3;
		int dialogWidth = (Integer)initArg4;
		int dialogHeight = (Integer)initArg5;
		Container contentPane = this.getContentPane();
		PowerLayout rootLayout = new PowerLayout(contentPane);
		contentPane.setLayout(rootLayout);
		this.previousButton = new JButton("Previous");
		this.previousButton.setToolTipText(HelpManager.getToolTip("PREVIOUS_SCREEN"));
		this.okButton = new JButton(okButtonText);
		this.okButton.setToolTipText(okButtonToolTip);
		this.cancelButton = new JButton("Cancel");
		this.cancelButton.setToolTipText(HelpManager.getToolTip("CANCEL_DIALOG"));
		this.previousButton.addActionListener(this);
		this.okButton.addActionListener(this);
		this.cancelButton.addActionListener(this);
		this.previousButton.addKeyListener(this);
		this.okButton.addKeyListener(this);
		this.cancelButton.addKeyListener(this);
		contentPane.add(this.previousButton, "previousBtn");
		contentPane.add(this.okButton, "okBtn");
		contentPane.add(this.cancelButton, "cancelBtn");
		this.mainPanel = new BorderedPanel(false);
		contentPane.add(this.mainPanel, "mainPnl");
		rootLayout.applyStyles(containerStyles + "layout.minWidth=minWidth;layout.width=max(minWidth," + dialogWidth + ");layout.height=" + dialogHeight + ";");
		this.setHelpPage(helpId);
		this.setIconImage(KlassMaster.getIcon(this));
		GuiUtils.packWindow(this);
	}

	public void setHelpPage(String helpId) {
		this.mainPanel.setLayout(new BorderLayout());
		this.helpPane = null;
		this.helpPane = new JEditorPane();
		this.helpPane.setEditable(false);

		try {
			this.helpPane.setPage(HelpManager.buildHelpUrlSimple(helpId));
		} catch (IOException ioException) {
			this.helpPane.setText(ioException.toString() + " : " + helpId);
		}

		this.mainPanel.add(new PlatformAdaptiveScrollPanel(this.helpPane), "Center");
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Object eventSource = e.getSource();
		if (eventSource == this.previousButton) {
			this.onPrevious();
		} else if (eventSource == this.okButton) {
			this.onNext();
		} else if (eventSource == this.cancelButton) {
			this.onConfirm();
		}
	}

	@Override
	public void keyPressed(KeyEvent e) {
		if (e.getKeyCode() == 10) {
			if (e.getSource() == this.previousButton) {
				this.onPrevious();
			} else if (e.getSource() == this.okButton) {
				this.onNext();
			} else if (e.getSource() == this.cancelButton) {
				this.onConfirm();
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

	public void onNext() {
		this.confirmed = true;
		this.close();
		this.resultConsumer.accept(1);
	}
}
