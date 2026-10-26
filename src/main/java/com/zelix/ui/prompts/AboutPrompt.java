package com.zelix.ui.prompts;

import com.zelix.klassmaster.KlassMaster;
import com.zelix.klassmaster.constant.CopyrightConstants;
import com.zelix.ui.base.BasePrompt;
import com.zelix.ui.components.panels.CopyrightPanel;
import com.zelix.ui.layouts.PowerLayout;
import com.zelix.ui.utils.GuiUtils;
import java.awt.Color;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.JButton;
import javax.swing.JFrame;

public class AboutPrompt extends BasePrompt implements ActionListener {
	public static String[] layoutProperties = new String[]{
		"bannerPnl.top=0", "bannerPnl.left=0", "okBtn.centerX=container.width*50/100", "okBtn.top=bannerPnl.bottom+10", "container.defaultBottomPad=10"
	};
	public JButton okButton;
	public CopyrightPanel copyrightPanel;

	public AboutPrompt(JFrame parent, String title, KlassMaster klassMaster) {
		super(parent, title, true);
		this.initComponents("version " + CopyrightConstants.VERSION + CopyrightConstants.SUFFIX, CopyrightConstants.COPYRIGHT_FULL, CopyrightConstants.WEBSITE, klassMaster);
		this.centerOnParent();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		this.closeDialog();
	}

	public void initComponents(String version, String copyright, String website, KlassMaster klassMaster) {
		Container contentPane = this.getContentPane();
		PowerLayout rootLayout = new PowerLayout(contentPane);
		contentPane.setLayout(rootLayout);
		this.copyrightPanel = new CopyrightPanel(version, copyright, website, klassMaster);
		if (!this.copyrightPanel.isUsingColorImage()) {
			contentPane.setBackground(CopyrightPanel.FALLBACK_GRAY);
		}

		if (!this.copyrightPanel.isUsingColorImage()) {
			this.setBackground(CopyrightPanel.FALLBACK_GRAY);
			this.setForeground(Color.black);
		}

		contentPane.add(this.copyrightPanel, "bannerPnl");
		this.okButton = new JButton("OK");
		this.okButton.addActionListener(this);
		contentPane.add(this.okButton, "okBtn");
		if (!this.copyrightPanel.isUsingColorImage()) {
			this.okButton.setBackground(CopyrightPanel.FALLBACK_GRAY);
			this.okButton.setForeground(Color.black);
		}

		rootLayout.setProperties(layoutProperties);
		GuiUtils.packWindow(this);
		this.setResizable(false);
		AboutPrompt.EnterKeyListener buttonKeyListener = new AboutPrompt.EnterKeyListener(this);
		this.okButton.addKeyListener(buttonKeyListener);
	}

	public static class EnterKeyListener extends KeyAdapter {
		public final AboutPrompt prompt;

		public EnterKeyListener(AboutPrompt prompt) {
			this.prompt = prompt;
		}

		@Override
		public void keyPressed(KeyEvent e) {
			if (e.getKeyCode() == 10) {
				this.prompt.closeDialog();
			}
		}
	}
}
