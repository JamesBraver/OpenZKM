package com.zelix.ui.prompts;

import com.zelix.HelpManager;
import com.zelix.ui.components.PlatformAdaptiveScrollPanel;
import com.zelix.ui.layouts.PowerLayout;
import com.zelix.ui.layouts.SplitLayout;
import com.zelix.ui.utils.GuiUtils;
import com.zelix.utils.function.ZKMConsumer;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.net.URL;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.event.HyperlinkEvent.EventType;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

public class WarningPrompt extends ScrollableMessagePrompt {
	public static String[] layoutProperties = new String[]{
		"msgLbl.top=5",
		"msgLbl.left=container.width*5/100",
		"splitPanel.top=msgLbl.bottom+5",
		"splitPanel.bottom=container.bottom-okBtn.defaultHeight-10",
		"splitPanel.left=container.width*5/100",
		"splitPanel.right=container.width*95/100",
		"okBtn.top=splitPanel.bottom+5",
		"okBtn.centerX=container.width*50/100",
		"layout.minWidth=250",
		"layout.minHeight=200"
	};
	public JEditorPane helpPanel;

	public WarningPrompt(JFrame parent, String title, String message, BufferedReader content, boolean editable, boolean modal, boolean centerOnParent) {
		this(parent, title, message, content, editable, modal, true, null);
	}

	public WarningPrompt(JFrame parent, String title, String message, BufferedReader content, boolean editable, boolean modal, boolean centerOnParent, ZKMConsumer closeCallback) {
		super(parent, title, message, content, editable, modal, centerOnParent);
	}

	public WarningPrompt(JFrame parent, String title, String message, BufferedReader content, boolean editable, boolean modal) {
		this(parent, title, message, content, editable, modal, true);
	}

	@Override
	protected void initComponents() {
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		Container contentPane = this.getContentPane();
		super.layout = new PowerLayout(contentPane);
		contentPane.setLayout(super.layout);
		super.messageLabel = new JLabel();
		contentPane.add(super.messageLabel, "msgLbl");
		JPanel splitPanel = new JPanel();
		splitPanel.setLayout(new SplitLayout(1, 60, false, 7));
		contentPane.add(splitPanel, "splitPanel");
		super.contentArea = new JTextArea();
		splitPanel.add(new PlatformAdaptiveScrollPanel(super.contentArea), "viewArea");
		this.helpPanel = new JEditorPane();
		this.helpPanel.setEditable(false);

		try {
			this.helpPanel.setPage(HelpManager.buildHelpUrlSimple("036"));
		} catch (IOException ioException) {
			this.helpPanel.setText(ioException.toString() + " : 036");
		}

		splitPanel.add(new PlatformAdaptiveScrollPanel(this.helpPanel), "helpPane");
		super.okButton = new JButton("OK");
		contentPane.add(super.okButton, "okBtn");
		WarningPrompt.PromptWindowListener windowListener = new WarningPrompt.PromptWindowListener(this);
		this.addWindowListener(windowListener);
		WarningPrompt.EnterKeyListener buttonKeyListener = new WarningPrompt.EnterKeyListener(this);
		super.okButton.addKeyListener(buttonKeyListener);
		super.contentArea.addKeyListener(buttonKeyListener);
		WarningPrompt.OkActionListener buttonActionListener = new WarningPrompt.OkActionListener(this);
		super.okButton.addActionListener(buttonActionListener);
		this.helpPanel.addHyperlinkListener(new WarningPrompt.HelpHyperlinkListener(this));
		super.layout.setProperties(layoutProperties);
		this.setSize(Math.min(600, screenSize.width), Math.min(450, screenSize.height));
	}

	public static class HelpHyperlinkListener implements HyperlinkListener {
		public final WarningPrompt prompt;

		public HelpHyperlinkListener(WarningPrompt prompt) {
			this.prompt = prompt;
		}

		@Override
		public void hyperlinkUpdate(HyperlinkEvent e) {
			if (e.getEventType() == EventType.ACTIVATED) {
				URL hyperlinkUrl = null;

				try {
					hyperlinkUrl = e.getURL();
					this.prompt.helpPanel.setPage(hyperlinkUrl);
				} catch (Throwable pageLoadException) {
					this.prompt.helpPanel.setText(pageLoadException.toString() + " : " + hyperlinkUrl);
				}
			}
		}
	}

	public static class EnterKeyListener extends KeyAdapter {
		public final WarningPrompt prompt;

		public EnterKeyListener(WarningPrompt prompt) {
			this.prompt = prompt;
		}

		@Override
		public void keyPressed(KeyEvent e) {
			if (e.getKeyCode() == 10) {
				this.prompt.closeDialog();
			}
		}
	}

	public static class OkActionListener implements ActionListener {
		public final WarningPrompt prompt;

		public OkActionListener(WarningPrompt prompt) {
			this.prompt = prompt;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			this.prompt.closeDialog();
		}
	}

	public static class PromptWindowListener extends WindowAdapter {
		public final WarningPrompt prompt;

		public PromptWindowListener(WarningPrompt prompt) {
			this.prompt = prompt;
		}

		@Override
		public void windowActivated(WindowEvent e) {
			GuiUtils.requestFocusInWindow(this.prompt.okButton);
		}

		@Override
		public void windowClosed(WindowEvent e) {
		}

		@Override
		public void windowClosing(WindowEvent e) {
			this.prompt.closeDialog();
		}
	}
}
