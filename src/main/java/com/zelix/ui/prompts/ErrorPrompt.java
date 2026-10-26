package com.zelix.ui.prompts;

import com.zelix.HelpManager;
import com.zelix.ui.base.BaseEscapableDialog;
import com.zelix.ui.components.PlatformAdaptiveScrollPanel;
import com.zelix.ui.layouts.PowerLayout;
import com.zelix.ui.utils.GuiUtils;
import com.zelix.utils.StringUtils;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.InvocationTargetException;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

public class ErrorPrompt extends BaseEscapableDialog {
	public static final String LINE_SEPARATOR = GuiUtils.LINE_SEPARATOR;
	public static String[] layoutProperties = new String[]{
		"msgLbl.top=5",
		"msgLbl.left=5",
		"msgPane.left=5",
		"msgPane.right=container.right-5",
		"msgPane.top=msgLbl.bottom+5",
		"msgPane.bottom=container.bottom-okBtn.defaultHeight-10",
		"btnWidth=max(okBtn.defaultWidth, copyBtn.defaultWidth)",
		"btnHeight=max(okBtn.defaultHeight, copyBtn.defaultHeight)",
		"okBtn.right=container.width*50/100-10",
		"okBtn.bottom=container.bottom-5",
		"okBtn.width=btnWidth",
		"okBtn.height=btnHeight",
		"copyBtn.left=container.width*50/100+10",
		"copyBtn.bottom=okBtn.bottom",
		"copyBtn.width=btnWidth",
		"copyBtn.height=btnHeight"
	};
	public JLabel messageLabel;
	public JEditorPane messagePane;
	public JButton okButton;
	public JButton copyButton;

	public ErrorPrompt(JFrame frame, String title, String message, String details) {
		super(frame, title, true);
		if (frame.isVisible()) {
			Container contentPane = this.getContentPane();
			PowerLayout rootLayout = new PowerLayout(contentPane);
			contentPane.setLayout(rootLayout);
			this.messageLabel = new JLabel(message);
			details = StringUtils.replace(StringUtils.replace(StringUtils.replace(details, "<", "&LT;"), ">", "&GT;"), LINE_SEPARATOR, "<br>");
			this.messagePane = new JEditorPane();
			this.messagePane.setEditable(false);
			GuiUtils.setHtmlText(this.messagePane, details);
			contentPane.add(this.messageLabel, "msgLbl");
			contentPane.add(new PlatformAdaptiveScrollPanel(this.messagePane), "msgPane");
			this.okButton = new JButton("OK");
			contentPane.add(this.okButton, "okBtn");
			this.copyButton = new JButton("Copy");
			contentPane.add(this.copyButton, "copyBtn");
			this.copyButton.setToolTipText(HelpManager.getToolTip("COPY_TEXT"));
			rootLayout.setProperties(layoutProperties);
			this.setSize(400, 300);
			ErrorPrompt.PromptWindowListener windowListener = new ErrorPrompt.PromptWindowListener(this);
			this.addWindowListener(windowListener);
			ErrorPrompt.OkCopyEnterKeyListener buttonKeyListener = new ErrorPrompt.OkCopyEnterKeyListener(this);
			this.okButton.addKeyListener(buttonKeyListener);
			this.copyButton.addKeyListener(buttonKeyListener);
			ErrorPrompt.OkCopyActionListener buttonActionListener = new ErrorPrompt.OkCopyActionListener(this);
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
			GuiUtils.setVisible(this, true);
		}
	}

	public static void show(JFrame frame, String title, String message, String details, boolean blocking) {
		ErrorPrompt.ShowPromptTask showTask = new ErrorPrompt.ShowPromptTask(frame, title, message, details);
		if (blocking) {
			if (!SwingUtilities.isEventDispatchThread()) {
				try {
					SwingUtilities.invokeAndWait(showTask);
				} catch (InterruptedException ignoredInterrupt) {
				} catch (InvocationTargetException ignoredInvocationTarget) {
				}
			} else {
				new ErrorPrompt(frame, title, message, details);
			}
		} else {
			SwingUtilities.invokeLater(showTask);
		}
	}

	public void copyToClipboard() {
		this.messagePane.selectAll();
		this.messagePane.copy();
	}

	@Override
	public void onEscapePressed() {
		this.setVisible(false);
		this.dispose();
	}

	public static class OkCopyActionListener implements ActionListener {
		public final ErrorPrompt prompt;

		public OkCopyActionListener(ErrorPrompt prompt) {
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
		public final ErrorPrompt prompt;

		public OkCopyEnterKeyListener(ErrorPrompt prompt) {
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
		public final ErrorPrompt prompt;

		public PromptWindowListener(ErrorPrompt prompt) {
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
		public final JFrame parentFrame;
		public final String title;
		public final String message;
		public final String details;

		public ShowPromptTask(JFrame parentFrame, String title, String message, String details) {
			this.parentFrame = parentFrame;
			this.title = title;
			this.message = message;
			this.details = details;
		}

		@Override
		public void run() {
			new ErrorPrompt(this.parentFrame, this.title, this.message, this.details);
		}
	}
}
