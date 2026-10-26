package com.zelix.ui.splash;

import com.zelix.klassmaster.KlassMaster;
import com.zelix.klassmaster.constant.CopyrightConstants;
import com.zelix.ui.components.panels.CopyrightPanel;
import com.zelix.ui.layouts.PowerLayout;
import com.zelix.ui.utils.GuiUtils;
import com.zelix.utils.function.ZKMConsumer;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JWindow;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

public class SplashScreen extends JWindow {
	public static String[] layoutStyles = new String[]{"bannerPnl.top=0", "bannerPnl.left=0", "bannerPnl.bottom=container.bottom", "bannerPnl.right.container.right"};
	public boolean closed = false;
	public Frame frame;
	public ZKMConsumer splashCompleteCallback;
	public Thread autoCloseThread;
	public CopyrightPanel copyrightPanel;

	public SplashScreen(Frame frame, KlassMaster klassMaster, ZKMConsumer splashCompleteCallback) {
		super(frame);
		this.frame = frame;
		this.splashCompleteCallback = splashCompleteCallback;
		this.getRootPane().getInputMap(2).put(KeyStroke.getKeyStroke(27, 0), "Cancel");
		this.getRootPane().getActionMap().put("Cancel", this.createCloseAction());
		this.enableEvents(8L);
		this.initCopyrightPanel("version " + CopyrightConstants.VERSION + "", CopyrightConstants.COPYRIGHT_FULL, CopyrightConstants.WEBSITE, klassMaster);
		this.showCentered();
		this.autoCloseThread = new Thread(() -> {
			try {
				Thread.sleep(7500L);
			} catch (InterruptedException ignored) {
			}

			SwingUtilities.invokeLater(this::close);
		});
		this.autoCloseThread.start();
	}

	public void showCentered() {
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		Dimension windowSize = this.getSize();
		int centeredX = screenSize.width / 2 - windowSize.width / 2;
		int centeredY = screenSize.height / 2 - windowSize.height / 2;
		this.setLocation(centeredX, centeredY);
		GuiUtils.setVisible(this, true);
	}

	public void initCopyrightPanel(String version, String copyright, String website, KlassMaster klassMaster) {
		this.copyrightPanel = new CopyrightPanel(version, copyright, website, klassMaster);
		if (!this.copyrightPanel.isUsingColorImage()) {
			this.setBackground(CopyrightPanel.FALLBACK_GRAY);
			this.setForeground(Color.black);
		}

		Container contentPane = this.getContentPane();
		PowerLayout powerLayout = new PowerLayout(contentPane);
		contentPane.setLayout(powerLayout);
		if (!this.copyrightPanel.isUsingColorImage()) {
			this.copyrightPanel.setBackground(CopyrightPanel.FALLBACK_GRAY);
			this.copyrightPanel.setForeground(Color.black);
		}

		contentPane.add(this.copyrightPanel, "bannerPnl");
		powerLayout.setProperties(layoutStyles);
		GuiUtils.packWindow(this);
		SplashScreen.KeyTypedListener keyTypedListener = new SplashScreen.KeyTypedListener(this);
		SplashScreen.MousePressedListener mousePressedListener = new SplashScreen.MousePressedListener(this);
		this.copyrightPanel.addKeyListener(keyTypedListener);
		this.copyrightPanel.addMouseListener(mousePressedListener);
		GuiUtils.requestFocusInWindow(this.copyrightPanel);
	}

	public synchronized void close() {
		if (!this.closed) {
			this.closed = true;
			this.setVisible(false);
			this.dispose();
			this.splashCompleteCallback.accept(this.frame);
		}
	}

	public Action createCloseAction() {
		return new SplashScreen.CloseAction(this);
	}

	public static class KeyTypedListener extends KeyAdapter {
		public final SplashScreen splashScreen;

		public KeyTypedListener(SplashScreen splashScreen) {
			this.splashScreen = splashScreen;
		}

		@Override
		public void keyTyped(KeyEvent e) {
		}
	}

	public static class MousePressedListener extends MouseAdapter {
		public final SplashScreen splashScreen;

		public MousePressedListener(SplashScreen splashScreen) {
			this.splashScreen = splashScreen;
		}

		@Override
		public void mousePressed(MouseEvent e) {
			this.splashScreen.close();
		}
	}

	public static class CloseAction extends AbstractAction {
		public final SplashScreen splashScreen;

		public CloseAction(SplashScreen splashScreen) {
			this.splashScreen = splashScreen;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			this.splashScreen.close();
		}
	}
}
