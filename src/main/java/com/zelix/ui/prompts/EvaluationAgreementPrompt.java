package com.zelix.ui.prompts;

import com.zelix.klassmaster.KlassMaster;
import com.zelix.ui.base.BasePrompt;
import com.zelix.ui.components.panels.CopyrightPanel;
import com.zelix.ui.layouts.PowerLayout;
import com.zelix.ui.utils.GuiUtils;
import com.zelix.utils.function.ZKMConsumer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class EvaluationAgreementPrompt extends BasePrompt implements ActionListener {
	public static String[] layoutProperties = new String[]{
		"btnHeight=max(okBtn.defaultHeight, cancelBtn.defaultHeight)",
		"btnWidth=max(okBtn.defaultWidth, cancelBtn.defaultWidth)",
		"bannerPnl.top=0",
		"bannerPnl.left=0",
		"bannerPnl.right=container.right",
		"okBtn.centerX=container.width*25/100",
		"cancelBtn.centerX=container.width*75/100",
		"okBtn.top=bannerPnl.bottom+10",
		"cancelBtn.top=okBtn.top",
		"okBtn.height=btnHeight",
		"okBtn.width=btnWidth",
		"cancelBtn.height=btnHeight",
		"cancelBtn.width=btnWidth",
		"okBtn.bottom=container.bottom-10"
	};
	public final JFrame ownerFrame;
	public final ZKMConsumer completionCallback;
	public CopyrightPanel copyrightPanel;
	public JButton agreeButton;
	public JButton disagreeButton;

	public EvaluationAgreementPrompt(JFrame ownerFrame, KlassMaster klassMaster, ZKMConsumer completionCallback) {
		super(ownerFrame, "Zelix KlassMaster Evaluation", true);
		this.completionCallback = completionCallback;
		this.ownerFrame = ownerFrame;
		this.initComponents(
			"version 27.0.0", "Copyright 1997-2026 Zelix Pty Ltd (47 078 740 093) All rights reserved", "www.zelix.com", klassMaster
		);
		this.positionPrompt(0, 0, false);
	}

	public void initComponents(String version, String copyright, String website, KlassMaster klassMaster) {
		Container contentPane = this.getContentPane();
		PowerLayout rootLayout = new PowerLayout(contentPane);
		contentPane.setLayout(rootLayout);
		this.copyrightPanel = new CopyrightPanel(version, copyright, website, klassMaster);
		if (!this.copyrightPanel.isUsingColorImage()) {
			this.setBackground(CopyrightPanel.FALLBACK_GRAY);
		}

		if (!this.copyrightPanel.isUsingColorImage()) {
			contentPane.setBackground(CopyrightPanel.FALLBACK_GRAY);
			contentPane.setForeground(Color.black);
		}

		contentPane.add(this.copyrightPanel, "bannerPnl");
		this.agreeButton = new JButton("I agree");
		contentPane.add(this.agreeButton, "okBtn");
		this.disagreeButton = new JButton("I don't agree");
		contentPane.add(this.disagreeButton, "cancelBtn");
		if (!this.copyrightPanel.isUsingColorImage()) {
			this.agreeButton.setBackground(CopyrightPanel.FALLBACK_GRAY);
			this.agreeButton.setForeground(Color.black);
		}

		if (!this.copyrightPanel.isUsingColorImage()) {
			this.disagreeButton.setBackground(CopyrightPanel.FALLBACK_GRAY);
			this.disagreeButton.setForeground(Color.black);
		}

		rootLayout.setProperties(layoutProperties);
		GuiUtils.packWindow(this);
		this.setResizable(false);

		KeyAdapter keyListener = new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == 10) {
					closeDialog();
					Object source = e.getSource();
					if (source == agreeButton) {
						completionCallback.accept(ownerFrame);
					} else {
						completionCallback.accept();
					}
				}
			}
		};
		this.agreeButton.addKeyListener(keyListener);
		this.disagreeButton.addKeyListener(keyListener);
		this.agreeButton.addActionListener(this);
		this.disagreeButton.addActionListener(this);

		WindowAdapter windowListener = new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				closeDialog();
				completionCallback.accept();
			}
		};
		this.addWindowListener(windowListener);
	}

	@Override
	public Action createEscapeAction() {
		return new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				closeDialog();
				completionCallback.accept();
			}
		};
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();
		this.closeDialog();
		if (source == this.agreeButton) {
			this.completionCallback.accept(this.ownerFrame);
		} else {
			this.completionCallback.accept();
		}
	}

	@Override
	public void closeDialog() {
		this.setVisible(false);
		this.dispose();
	}
}
