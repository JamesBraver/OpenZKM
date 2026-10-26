package com.zelix.ui.components.panels;

import com.zelix.klassmaster.KlassMaster;
import com.zelix.klassmaster.config.Options;
import com.zelix.klassmaster.constant.AntiStringerConstants;
import com.zelix.ui.layouts.PowerLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class CopyrightPanel extends BorderedPanel {
	public static final Color FALLBACK_GRAY = new Color(198, 198, 198);
	public static String[] panelStyles = new String[]{
		"centerX=container.width*50/100",
		"imagePnl.centerX=centerX",
		"imagePnl.top=container.top+10",
		"imagePnl.width=264",
		"imagePnl.height=60",
		"versionLbl.centerX=centerX",
		"versionLbl.top=imagePnl.bottom+5",
		"copyrightLbl.centerX=centerX",
		"copyrightLbl.top=versionLbl.bottom+5",
		"copyrightLbl.width=copyrightLbl.defaultWidth+40",
		"contactLbl.centerX=centerX",
		"contactLbl.top=copyrightLbl.bottom+5",
		"contactLbl.width=contactLbl.defaultWidth+25",
		"licenseePnl.left=container.left+5",
		"licenseePnl.right=container.right-5",
		"licenseePnl.top=contactLbl.bottom+20",
		"licenseePnl.bottom=container.bottom-5"
	};
	public static String[] licenseePanelStyles = new String[]{"licenseeTextPnl.top=2", "licenseeTextPnl.centerX=container.width*50/100", "container.defaultBottomPad=2"};
	private final String[] licenseInfoLines = new String[8];
	public JLabel versionLabel;
	public JLabel copyrightLabel;
	public LogoPanel logoPanel;
	public JLabel contactLabel;
	public PowerLayout layout;

	public CopyrightPanel(String version, String copyright, String website, KlassMaster klassMaster) {
		super(true);
		this.loadLicenseInfo(klassMaster);
		this.initComponents(version, copyright, website, klassMaster);
	}

	private void initComponents(String version, String copyright, String website, KlassMaster klassMaster) {
		this.setFont(KlassMaster.getFont());
		this.layout = new PowerLayout(this);
		this.setLayout(this.layout);
		Image colorImage = this.getToolkit().getImage(Options.uiConfig.getClass().getResource("1.png"));
		Image fallbackImage = this.getToolkit().getImage(Options.uiConfig.getClass().getResource("1.gif"));
		this.logoPanel = new LogoPanel(colorImage, fallbackImage);
		if (!this.logoPanel.isUsingColorImage()) {
			this.setBackground(FALLBACK_GRAY);
			this.setForeground(Color.black);
		}

		this.add(this.logoPanel, "imagePnl");
		this.versionLabel = new JLabel();
		this.add(this.versionLabel, "versionLbl");
		this.copyrightLabel = new JLabel();
		this.add(this.copyrightLabel, "copyrightLbl");
		this.contactLabel = new JLabel();
		this.add(this.contactLabel, "contactLbl");
		if (version != null) {
			this.versionLabel.setText(version);
			this.versionLabel.setHorizontalAlignment(0);
		}

		if (copyright != null) {
			this.copyrightLabel.setText(copyright);
			this.copyrightLabel.setHorizontalAlignment(0);
		}

		if (website != null) {
			this.contactLabel.setText(website);
			this.contactLabel.setHorizontalAlignment(0);
		}

		BorderedPanel licenseePanel = new BorderedPanel(false);
		this.add(licenseePanel, "licenseePnl");
		if (!this.logoPanel.isUsingColorImage()) {
			licenseePanel.setBackground(FALLBACK_GRAY);
			licenseePanel.setForeground(Color.black);
		}

		PowerLayout licenseePanelLayout = new PowerLayout(licenseePanel);
		licenseePanel.setLayout(licenseePanelLayout);
		JPanel licenseeTextPanel = new JPanel();
		if (!this.logoPanel.isUsingColorImage()) {
			licenseeTextPanel.setBackground(FALLBACK_GRAY);
		}

		licenseePanel.add(licenseeTextPanel, "licenseeTextPnl");
		JPanel evenLinesPanel = new JPanel();
		if (!this.logoPanel.isUsingColorImage()) {
			evenLinesPanel.setBackground(FALLBACK_GRAY);
		}

		licenseeTextPanel.add(evenLinesPanel);
		GridLayout evenLinesLayout = new GridLayout(0, 1);
		evenLinesPanel.setLayout(evenLinesLayout);
		JPanel oddLinesPanel = new JPanel();
		if (!this.logoPanel.isUsingColorImage()) {
			oddLinesPanel.setBackground(FALLBACK_GRAY);
		}

		licenseeTextPanel.add(oddLinesPanel);
		GridLayout oddLinesLayout = new GridLayout(0, 1);
		oddLinesPanel.setLayout(oddLinesLayout);
		JLabel licenseInfoLabel0 = new JLabel(this.licenseInfoLines[0]);
		JLabel licenseInfoLabel1 = new JLabel(this.licenseInfoLines[1]);
		JLabel licenseInfoLabel2 = new JLabel(this.licenseInfoLines[2]);
		JLabel licenseInfoLabel3 = new JLabel(this.licenseInfoLines[3]);
		// String var45 = KlassMaster.N(klassMaster.A(), klassMaster.L(), klassMaster.k(), this);
		// JLabel var44 = new JLabel(this.licenseInfoLines[3] + " " + var45);
		if (false) {
			licenseInfoLabel3.setText(licenseInfoLabel3.getText() + " " + evaluationExpiryDate(klassMaster, this));
		}

		JLabel licenseInfoLabel4 = new JLabel(this.licenseInfoLines[4]);
		JLabel licenseInfoLabel5 = new JLabel(this.licenseInfoLines[5]);
		JLabel licenseInfoLabel6 = new JLabel(this.licenseInfoLines[6]);
		JLabel licenseInfoLabel7 = new JLabel(this.licenseInfoLines[7]);
		licenseInfoLabel1.setFont(this.getFont());
		licenseInfoLabel3.setFont(this.getFont());
		licenseInfoLabel5.setFont(this.getFont());
		licenseInfoLabel7.setFont(this.getFont());
		evenLinesPanel.add(licenseInfoLabel0);
		oddLinesPanel.add(licenseInfoLabel1);
		evenLinesPanel.add(licenseInfoLabel2);
		oddLinesPanel.add(licenseInfoLabel3);
		evenLinesPanel.add(licenseInfoLabel4);
		oddLinesPanel.add(licenseInfoLabel5);
		evenLinesPanel.add(licenseInfoLabel6);
		oddLinesPanel.add(licenseInfoLabel7);
		this.layout.setProperties(panelStyles);
		licenseePanelLayout.setProperties(licenseePanelStyles);
		CopyrightPanel.MousePressedListener mousePressedListener = new CopyrightPanel.MousePressedListener(this);
		this.versionLabel.addMouseListener(mousePressedListener);
		this.copyrightLabel.addMouseListener(mousePressedListener);
		this.contactLabel.addMouseListener(mousePressedListener);
		licenseInfoLabel0.addMouseListener(mousePressedListener);
		licenseInfoLabel1.addMouseListener(mousePressedListener);
		licenseInfoLabel2.addMouseListener(mousePressedListener);
		licenseInfoLabel3.addMouseListener(mousePressedListener);
		licenseInfoLabel4.addMouseListener(mousePressedListener);
		licenseInfoLabel5.addMouseListener(mousePressedListener);
		licenseInfoLabel6.addMouseListener(mousePressedListener);
		licenseInfoLabel7.addMouseListener(mousePressedListener);
		licenseePanel.addMouseListener(mousePressedListener);
		licenseeTextPanel.addMouseListener(mousePressedListener);
		evenLinesPanel.addMouseListener(mousePressedListener);
		oddLinesPanel.addMouseListener(mousePressedListener);
	}

	public boolean isUsingColorImage() {
		return this.logoPanel.isUsingColorImage();
	}

	public void loadLicenseInfo(KlassMaster klassMaster) {
		String licenseStr = klassMaster.getLicenseStr();
		if (AntiStringerConstants.UNKNOWN_LICENSE_STRING.equals(licenseStr)) {
			for (int i = 0; i < 8; i++) {
				this.licenseInfoLines[i] = klassMaster.getLicenseInfo(i).trim();
			}
		}
	}

	private static String evaluationExpiryDate(KlassMaster klassMaster, CopyrightPanel panel) {
		return null;
	}

	public static class MousePressedListener extends MouseAdapter {
		public final CopyrightPanel copyrightPanel;

		public MousePressedListener(CopyrightPanel copyrightPanel) {
			this.copyrightPanel = copyrightPanel;
		}

		@Override
		public void mousePressed(MouseEvent e) {
			this.copyrightPanel.dispatchEvent(e);
		}
	}
}
