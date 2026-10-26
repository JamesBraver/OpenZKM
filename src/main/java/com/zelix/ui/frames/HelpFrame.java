package com.zelix.ui.frames;

import com.zelix.HelpManager;
import com.zelix.UIConfig;
import com.zelix.klassmaster.KlassMaster;
import com.zelix.klassmaster.config.Options;
import com.zelix.klassmaster.constant.CopyrightConstants;
import com.zelix.klassmaster.constant.SystemConstants;
import com.zelix.ui.base.BaseFrame;
import com.zelix.ui.components.PlatformAdaptiveScrollPanel;
import com.zelix.ui.layouts.PowerLayout;
import com.zelix.ui.utils.GuiUtils;
import com.zelix.utils.dataset.IntPair;
import com.zelix.utils.dataset.IntSize;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.net.URL;
import java.util.Vector;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent.EventType;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

public class HelpFrame extends BaseFrame implements ActionListener, ComponentListener, HyperlinkListener {
	public static String[] layoutProperties = new String[]{
		"msgArea.top=5",
		"msgArea.left=5",
		"msgArea.right=container.right-5",
		"msgArea.bottom=container.bottom-closeBtn.defaultHeight-10",
		"btnWidth=max(backBtn.defaultWidth,closeBtn.defaultWidth,forwardBtn.defaultWidth)",
		"btnHeight=max(backBtn.defaultHeight,closeBtn.defaultHeight,forwardBtn.defaultHeight)",
		"backBtn.bottom=container.bottom-5",
		"backBtn.right=closeBtn.left-5",
		"backBtn.width=btnWidth",
		"backBtn.height=btnHeight",
		"closeBtn.bottom=container.bottom-5",
		"closeBtn.centerX=container.width*50/100",
		"closeBtn.width=btnWidth",
		"closeBtn.height=btnHeight",
		"forwardBtn.bottom=container.bottom-5",
		"forwardBtn.left=closeBtn.right+5",
		"forwardBtn.width=btnWidth",
		"forwardBtn.height=btnHeight"
	};
	private static final boolean IS_IBM_JAVA = SystemConstants.javaVendor.indexOf("IBM") > -1;
	private Vector<URL> history = new Vector<>();
	public HelpManager helpManager;
	public JEditorPane messagePane;
	public JButton backButton;
	private long lastActivationTime;
	public JButton closeButton;
	public JButton forwardButton;
	private int forwardCount;

	public HelpFrame(HelpManager helpManager) {
		this.helpManager = helpManager;
		this.initScreenContent();
		IntPair windowPosition = helpManager.getWindowPosition();
		this.setLocation(windowPosition.getKey(), windowPosition.getValue());
		IntSize windowSize = helpManager.getWindowSize();
		this.setSize(windowSize.getKey(), windowSize.getValue());
	}

	private void navigateBack() {
		int historySize = this.history.size();
		if (this.forwardCount < historySize) {
			URL url = this.history.get(historySize - 2 - this.forwardCount);

			try {
				this.messagePane.setPage(url);
				this.forwardCount++;
				this.updateNavigationButtons();
			} catch (IOException ioException) {
				this.showLoadError(url, ioException);
			}
		}
	}

	private void showLoadError(URL url, Throwable throwable) {
		this.messagePane.setText(throwable.toString() + " : " + url);
	}

	@Override
	protected void onClose() {
		GuiUtils.requestFocusInWindow(this.closeButton);
	}

	public void showHelpPage(URL url) {
		if (SwingUtilities.isEventDispatchThread()) {
			this.navigateTo(url);
		} else {
			SwingUtilities.invokeLater(() -> navigateToLater(this, url));
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == this.closeButton) {
			this.setVisible(false);
		} else if (e.getSource() == this.backButton) {
			this.navigateBack();
		} else if (e.getSource() == this.forwardButton) {
			this.navigateForward();
		}
	}

	private void navigateTo(URL url) {
		try {
			this.messagePane.setPage(url);
			this.history.clear();
			this.forwardCount = 0;
			this.history.add(url);
			this.updateNavigationButtons();
		} catch (IOException ioException) {
			this.showLoadError(url, ioException);
		}

		if (!this.isVisible()) {
			this.setVisible(true);
		}

		if (this.getState() == 1) {
			this.setState(0);
		}

		this.toFront();
	}

	@Override
	public void componentHidden(ComponentEvent e) {
	}

	@Override
	public void componentMoved(ComponentEvent e) {
		UIConfig uiConfig = Options.uiConfig;
		Point frameLocation = this.getLocation();
		uiConfig.setWindowPosition((int)frameLocation.getX(), (int)frameLocation.getY());
		uiConfig.saveConfig();
	}

	@Override
	public void componentResized(ComponentEvent e) {
		UIConfig uiConfig = Options.uiConfig;
		Dimension frameSize = this.getSize();
		uiConfig.setWindowSize(frameSize.width, frameSize.height);
		uiConfig.saveConfig();
	}

	@Override
	public void componentShown(ComponentEvent e) {
	}

	private void navigateForward() {
		int historySize = this.history.size();
		if (this.forwardCount > 0) {
			URL url = this.history.get(historySize - this.forwardCount);

			try {
				this.messagePane.setPage(url);
				this.forwardCount--;
				this.updateNavigationButtons();
			} catch (IOException ioException) {
				this.showLoadError(url, ioException);
			}
		}
	}

	private void updateNavigationButtons() {
		int historySize = this.history.size();
		if (historySize <= 1) {
			this.backButton.setEnabled(false);
			this.forwardButton.setEnabled(false);
		} else {
			if (this.forwardCount < historySize - 1) {
				this.backButton.setEnabled(true);
			} else {
				this.backButton.setEnabled(false);
			}

			if (this.forwardCount > 0) {
				this.forwardButton.setEnabled(true);
			} else {
				this.forwardButton.setEnabled(false);
			}
		}
	}

	@Override
	public void hyperlinkUpdate(HyperlinkEvent e) {
		if (e.getEventType() == EventType.ACTIVATED) {
			URL url = null;

			try {
				url = e.getURL();
				if (IS_IBM_JAVA) {
					long currentTime = System.currentTimeMillis();
					long timeSinceLastActivation = currentTime - this.lastActivationTime;
					this.lastActivationTime = currentTime;
					if (timeSinceLastActivation < 500L) {
						return;
					}
				}

				this.messagePane.setPage(url);

				for (int i = 0; i < this.forwardCount; i++) {
					this.history.removeElementAt(this.history.size() - 1);
				}

				this.forwardCount = 0;
				this.history.add(url);
				this.updateNavigationButtons();
			} catch (Throwable navigationException) {
				this.showLoadError(url, navigationException);
			}
		}
	}

	public void initScreenContent() {
		this.setTitle(CopyrightConstants.NAME + " - Help");
		Container contentPane = this.getContentPane();
		PowerLayout layout = new PowerLayout(contentPane);
		contentPane.setLayout(layout);
		this.messagePane = new JEditorPane();
		this.messagePane.setEditable(false);
		contentPane.add(new PlatformAdaptiveScrollPanel(this.messagePane), "msgArea");
		this.closeButton = new JButton("Close");
		this.closeButton.setToolTipText(HelpManager.getToolTip("HELP_EXIT"));
		contentPane.add(this.closeButton, "closeBtn");
		this.backButton = new JButton("<");
		this.backButton.setToolTipText(HelpManager.getToolTip("HELP_BACK"));
		contentPane.add(this.backButton, "backBtn");
		this.forwardButton = new JButton(">");
		this.forwardButton.setToolTipText(HelpManager.getToolTip("HELP_FORWARD"));
		contentPane.add(this.forwardButton, "forwardBtn");
		layout.setProperties(layoutProperties);
		this.backButton.addActionListener(this);
		this.closeButton.addActionListener(this);
		this.forwardButton.addActionListener(this);
		HelpFrame.ButtonKeyListener keyListener = new HelpFrame.ButtonKeyListener(this);
		this.backButton.addKeyListener(keyListener);
		this.closeButton.addKeyListener(keyListener);
		this.forwardButton.addKeyListener(keyListener);
		this.addComponentListener(this);
		this.messagePane.addHyperlinkListener(this);
		this.updateNavigationButtons();

		try {
			this.setIconImage(KlassMaster.getIcon(this));
		} catch (Throwable ignored) {
		}
	}

	@Override
	public void close() {
		super.close();
		HelpManager.resetHelpFrame();
	}

	public static void navigateToLater(HelpFrame helpFrame, URL url) {
		helpFrame.navigateTo(url);
	}

	public static void goBack(HelpFrame helpFrame) {
		helpFrame.navigateBack();
	}

	public static void goForward(HelpFrame helpFrame) {
		helpFrame.navigateForward();
	}

	public static class ButtonKeyListener extends KeyAdapter {
		public final HelpFrame helpFrame;

		public ButtonKeyListener(HelpFrame helpFrame) {
			this.helpFrame = helpFrame;
		}

		@Override
		public void keyPressed(KeyEvent e) {
			if (e.getKeyCode() == 10) {
				if (e.getSource() == this.helpFrame.backButton) {
					HelpFrame.goBack(this.helpFrame);
				} else if (e.getSource() == this.helpFrame.closeButton) {
					this.helpFrame.close();
				} else if (e.getSource() == this.helpFrame.forwardButton) {
					HelpFrame.goForward(this.helpFrame);
				}
			}
		}
	}
}
