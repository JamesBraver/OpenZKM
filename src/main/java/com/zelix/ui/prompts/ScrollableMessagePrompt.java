package com.zelix.ui.prompts;

import com.zelix.ui.components.PlatformAdaptiveScrollPanel;
import com.zelix.ui.layouts.PowerLayout;
import com.zelix.ui.prompts.base.BaseLongContentPrompt;
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
import java.io.StringReader;
import java.util.Vector;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextArea;

public class ScrollableMessagePrompt extends BaseLongContentPrompt {
	public static Vector<ScrollableMessagePrompt> openPrompts = new Vector<>();
	public static String[] layoutStyles = new String[]{
		"msgLbl.top=5",
		"msgLbl.left=container.width*5/100",
		"viewArea.top=msgLbl.bottom+5",
		"viewArea.left=container.width*5/100",
		"viewArea.right=container.width*95/100",
		"viewArea.bottom=container.bottom-okBtn.defaultHeight-10",
		"okBtn.top=viewArea.bottom+5",
		"okBtn.centerX=container.width*50/100",
		"layout.minWidth=250",
		"layout.minHeight=200"
	};
	protected ZKMConsumer closeCallback;
	public JButton okButton;

	public ScrollableMessagePrompt(JFrame parent, String title, String message, String content, boolean editable, boolean modal, boolean centerOnParent) {
		this(parent, title, message, new BufferedReader(new StringReader(content)), editable, modal, centerOnParent);
	}

	public ScrollableMessagePrompt(JFrame parent, String title, String message, BufferedReader content, boolean editable, boolean modal) {
		this(parent, title, message, content, editable, modal, true);
	}

	public ScrollableMessagePrompt(JFrame parent, String title, String message, BufferedReader content, boolean editable, boolean modal, boolean centerOnParent) {
		this(parent, title, message, content, editable, modal, true, null);
	}

	public ScrollableMessagePrompt(JFrame parent, String title, String message, BufferedReader content, boolean editable, boolean modal, boolean centerOnParent, ZKMConsumer closeCallback) {
		super(parent, title, message, content, editable, modal, centerOnParent);
		openPrompts.addElement(this);
		int promptOffset = openPrompts.size() * 10;
		this.closeCallback = closeCallback;
		this.positionPrompt(promptOffset, promptOffset, centerOnParent);
	}

	public ScrollableMessagePrompt(JFrame parent, String title, String message, String content, boolean editable, boolean modal) {
		this(parent, title, message, new BufferedReader(new StringReader(content)), editable, modal);
	}

	@Override
	public final void closeDialog() {
		openPrompts.removeElement(this);
		super.closeDialog();
		if (this.closeCallback != null) {
			this.closeCallback.accept();
		}
	}

	@Override
	protected void initComponents() {
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		Container contentPane = this.getContentPane();
		super.layout = new PowerLayout(contentPane);
		contentPane.setLayout(super.layout);
		super.messageLabel = new JLabel();
		contentPane.add(super.messageLabel, "msgLbl");
		super.contentArea = new JTextArea();
		contentPane.add(new PlatformAdaptiveScrollPanel(super.contentArea), "viewArea");
		this.okButton = new JButton("OK");
		contentPane.add(this.okButton, "okBtn");
		ScrollableMessagePrompt.PromptWindowListener promptWindowListener = new ScrollableMessagePrompt.PromptWindowListener(this);
		this.addWindowListener(promptWindowListener);
		ScrollableMessagePrompt.EnterKeyListener enterKeyListener = new ScrollableMessagePrompt.EnterKeyListener(this);
		this.okButton.addKeyListener(enterKeyListener);
		super.contentArea.addKeyListener(enterKeyListener);
		ScrollableMessagePrompt.OkButtonListener okButtonListener = new ScrollableMessagePrompt.OkButtonListener(this);
		this.okButton.addActionListener(okButtonListener);
		super.layout.setProperties(layoutStyles);
		this.setSize(Math.min(500, screenSize.width), Math.min(400, screenSize.height));
	}

	public static class OkButtonListener implements ActionListener {
		public final ScrollableMessagePrompt scrollableMessagePrompt;

		public OkButtonListener(ScrollableMessagePrompt scrollableMessagePrompt) {
			this.scrollableMessagePrompt = scrollableMessagePrompt;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			this.scrollableMessagePrompt.closeDialog();
		}
	}

	public static class PromptWindowListener extends WindowAdapter {
		public final ScrollableMessagePrompt scrollableMessagePrompt;

		public PromptWindowListener(ScrollableMessagePrompt scrollableMessagePrompt) {
			this.scrollableMessagePrompt = scrollableMessagePrompt;
		}

		@Override
		public void windowActivated(WindowEvent e) {
			GuiUtils.requestFocusInWindow(this.scrollableMessagePrompt.okButton);
		}

		@Override
		public void windowClosed(WindowEvent e) {
		}

		@Override
		public void windowClosing(WindowEvent e) {
			this.scrollableMessagePrompt.closeDialog();
		}
	}

	public static class EnterKeyListener extends KeyAdapter {
		public final ScrollableMessagePrompt scrollableMessagePrompt;

		public EnterKeyListener(ScrollableMessagePrompt scrollableMessagePrompt) {
			this.scrollableMessagePrompt = scrollableMessagePrompt;
		}

		@Override
		public void keyPressed(KeyEvent e) {
			if (e.getKeyCode() == 10) {
				this.scrollableMessagePrompt.closeDialog();
			}
		}
	}
}
