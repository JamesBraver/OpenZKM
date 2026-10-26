package com.zelix.ui.prompts.base;

import com.zelix.ui.base.BasePrompt;
import com.zelix.ui.layouts.PowerLayout;
import java.io.BufferedReader;
import java.io.IOException;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextArea;

public abstract class BaseLongContentPrompt extends BasePrompt {
	public static final String truncationNotice = BasePrompt.separator + BasePrompt.separator + "Only the first " + 512000 + " bytes shown";
	protected BufferedReader contentReader;
	protected PowerLayout layout;
	protected JTextArea contentArea;
	protected JLabel messageLabel;

	public BaseLongContentPrompt(JFrame parent, String title, String message, BufferedReader content, boolean editable, boolean modal, boolean centerOnParent) {
		super(parent, title, modal);
		this.initComponents();
		this.messageLabel.setText(message);
		this.contentReader = content;
		String currentLine = null;
		StringBuffer contentBuffer = new StringBuffer();
		this.contentArea.setEditable(editable);
		int contentLength = 0;
		if (content != null) {
			try {
				for (currentLine = content.readLine(); currentLine != null && contentLength < 512000; currentLine = content.readLine()) {
					contentLength += currentLine.length();
					contentBuffer.append(currentLine + BasePrompt.separator);
				}
			} catch (IOException ignoredReadFailure) {
			} finally {
				try {
					content.close();
				} catch (IOException ignoredCloseFailure) {
				}
			}
		}

		if (currentLine != null) {
			contentBuffer.append(truncationNotice);
		}

		this.contentArea.setText(contentBuffer.toString());
		this.contentArea.setCaretPosition(0);
	}

	protected abstract void initComponents();
}
