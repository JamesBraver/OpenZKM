package com.zelix.ui.prompts;

import com.zelix.HelpManager;
import com.zelix.ui.base.BaseEscapableDialog;
import com.zelix.ui.layouts.PowerLayout;
import com.zelix.ui.utils.GuiUtils;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.InvocationTargetException;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

public class CopyablePrompt extends BaseEscapableDialog implements ClipboardOwner {
	public Frame parentFrame;
	public JLabel messageLabel;
	public JButton okButton;
	public JButton copyButton;
	public FontMetrics fontMetrics;

	public CopyablePrompt(Frame frame, String title, String message) {
		super(frame, title, true);
		this.parentFrame = frame;
		Container contentPane = this.getContentPane();
		PowerLayout rootLayout = new PowerLayout(contentPane);
		contentPane.setLayout(rootLayout);
		this.messageLabel = new JLabel(message.trim(), 0);
		contentPane.add(this.messageLabel, "msgLbl");
		this.okButton = new JButton("OK");
		contentPane.add(this.okButton, "okBtn");
		this.copyButton = new JButton("Copy");
		contentPane.add(this.copyButton, "copyBtn");
		this.copyButton.setToolTipText(HelpManager.getToolTip("COPY_TEXT"));
		if (frame == null) {
			this.setFont(new Font("Dialog", 0, 12));
		}

		this.fontMetrics = this.messageLabel.getFontMetrics(this.messageLabel.getFont());
		int messageWidth = this.fontMetrics.stringWidth(message.trim());
		int dialogWidth = Math.max(200, messageWidth + 60);
		int fontHeight = this.fontMetrics.getHeight();
		String layoutProperties = "layout.minWidth=%0;layout.minHeight=btnHeight + %1 + 10;msgLbl.centerY=okBtn.top/2;msgLbl.centerX=container.width*50/100;btnWidth=max(okBtn.defaultWidth, copyBtn.defaultWidth);btnHeight=max(okBtn.defaultHeight, copyBtn.defaultHeight);okBtn.right=container.width*50/100-10;okBtn.bottom=container.bottom-5;okBtn.width=btnWidth;okBtn.height=btnHeight;copyBtn.left=container.width*50/100+10;copyBtn.bottom=container.bottom-5;copyBtn.width=btnWidth;copyBtn.height=btnHeight;"
			.replace("%0", String.valueOf(dialogWidth))
			.replace("%1", String.valueOf(fontHeight));
		rootLayout.applyStyles(layoutProperties);
		this.setSize(dialogWidth, Math.max(100, fontHeight * 7));
		CopyablePrompt.PromptWindowListener windowListener = new CopyablePrompt.PromptWindowListener(this);
		this.addWindowListener(windowListener);
		CopyablePrompt.OkCopyEnterKeyListener buttonKeyListener = new CopyablePrompt.OkCopyEnterKeyListener(this);
		this.okButton.addKeyListener(buttonKeyListener);
		this.copyButton.addKeyListener(buttonKeyListener);
		CopyablePrompt.OkCopyActionListener buttonActionListener = new CopyablePrompt.OkCopyActionListener(this);
		this.okButton.addActionListener(buttonActionListener);
		this.copyButton.addActionListener(buttonActionListener);
		Dimension dialogSize = this.getSize();
		Point frameLocation = frame.getLocationOnScreen();
		Dimension frameSize = frame.getSize();
		int centeredX = frameSize.width / 2 - dialogSize.width / 2 + frameLocation.x;
		int centeredY = frameSize.height / 2 - dialogSize.height / 2 + frameLocation.y;
		centeredX = Math.max(0, centeredX);
		centeredY = Math.max(0, centeredY);
		this.setLocation(centeredX, centeredY);
		GuiUtils.requestFocusInWindow(this.okButton);
		GuiUtils.setVisible(this, true);
	}

	public static void show(Frame frame, String title, String message) {
		show(frame, title, message, false);
	}

	public static void show(Frame frame, String title, String message, boolean blocking) {
		CopyablePrompt.ShowPromptTask showTask = new CopyablePrompt.ShowPromptTask(frame, title, message);
		if (blocking) {
			if (!SwingUtilities.isEventDispatchThread()) {
				try {
					SwingUtilities.invokeAndWait(showTask);
				} catch (InterruptedException ignoredInterrupt) {
				} catch (InvocationTargetException ignoredInvocationTarget) {
				}
			} else {
				new CopyablePrompt(frame, title, message);
			}
		} else {
			SwingUtilities.invokeLater(showTask);
		}
	}

	public void copyToClipboard() {
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(this.messageLabel.getText()), this);
	}

	@Override
	public void lostOwnership(Clipboard clipboard, Transferable contents) {
	}

	@Override
	public void onEscapePressed() {
		this.setVisible(false);
		this.dispose();
	}

	public static class OkCopyActionListener implements ActionListener {
		public final CopyablePrompt prompt;

		public OkCopyActionListener(CopyablePrompt prompt) {
			this.prompt = prompt;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			Object eventSource = e.getSource();
			if (eventSource == this.prompt.okButton) {
				this.prompt.onEscapePressed();
			} else if (eventSource == this.prompt.copyButton) {
				this.prompt.copyToClipboard();
			}
		}
	}

	public static class OkCopyEnterKeyListener extends KeyAdapter {
		public final CopyablePrompt prompt;

		public OkCopyEnterKeyListener(CopyablePrompt prompt) {
			this.prompt = prompt;
		}

		@Override
		public void keyPressed(KeyEvent e) {
			Object eventSource = e.getSource();
			if (e.getKeyCode() == 10) {
				if (eventSource == this.prompt.okButton) {
					this.prompt.onEscapePressed();
				} else if (eventSource == this.prompt.copyButton) {
					this.prompt.copyToClipboard();
				}
			}
		}
	}

	public static class PromptWindowListener extends WindowAdapter {
		public final CopyablePrompt prompt;

		public PromptWindowListener(CopyablePrompt prompt) {
			this.prompt = prompt;
		}

		@Override
		public void windowActivated(WindowEvent e) {
			GuiUtils.requestFocusInWindow(this.prompt.okButton);
		}

		@Override
		public void windowClosing(WindowEvent e) {
			this.prompt.onEscapePressed();
		}
	}

	public static class ShowPromptTask implements Runnable {
		public final Frame parentFrame;
		public final String title;
		public final String message;

		public ShowPromptTask(Frame parentFrame, String title, String message) {
			this.parentFrame = parentFrame;
			this.title = title;
			this.message = message;
		}

		@Override
		public void run() {
			new CopyablePrompt(this.parentFrame, this.title, this.message);
		}
	}
}
