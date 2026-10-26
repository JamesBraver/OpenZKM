package com.zelix;

import com.zelix.klassmaster.config.Options;
import com.zelix.ui.frames.HelpFrame;
import com.zelix.utils.dataset.IntPair;
import com.zelix.utils.dataset.IntSize;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.net.URL;
import java.util.ResourceBundle;

public class HelpManager {
	private static final HelpManager INSTANCE = new HelpManager();
	private static HelpFrame helpFrame;
	private static ResourceBundle resourceBundle;
	private String baseResourcePath;
	private IntPair windowPosition;
	private IntSize windowSize;

	private HelpManager() {
		String className = this.getClass().getName();
		String packagePath = className.substring(0, className.lastIndexOf(".") + 1);
		this.baseResourcePath = "/" + packagePath.replace('.', '/');
		UIConfig uiConfig = Options.uiConfig;
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		this.windowPosition = uiConfig.getWindowPosition();
		if (this.windowPosition.getKey() > screenSize.getWidth() - 50.0 || this.windowPosition.getValue() > screenSize.getHeight() - 50.0) {
			this.windowPosition = new IntPair(0, 0);
		}

		this.windowSize = uiConfig.getWindowSize();
		if (this.windowSize.getKey() >= screenSize.getWidth() - 25.0) {
			this.windowSize = new IntSize((int)screenSize.getWidth() - 50, this.windowSize.getValue());
		}

		if (this.windowSize.getValue() >= screenSize.getHeight() - 25.0) {
			this.windowSize = new IntSize(this.windowSize.getKey(), (int)screenSize.getHeight() - 50);
		}

		if (this.windowSize.getKey() < 300) {
			this.windowSize = new IntSize(300, this.windowSize.getValue());
		}

		if (this.windowSize.getValue() < 300) {
			this.windowSize = new IntSize(this.windowSize.getKey(), 300);
		}

		try {
			resourceBundle = ResourceBundle.getBundle(packagePath + "tooltip");
		} catch (Throwable ignored) {
		}
	}

	public static void closeHelp() {
		if (helpFrame != null) {
			helpFrame.close();
		}
	}

	public static void showHelp(String helpId, String anchor) {
		if (helpFrame == null) {
			helpFrame = new HelpFrame(INSTANCE);
		}

		helpFrame.showHelpPage(buildHelpUrl(helpId, anchor));
	}

	public static String getToolTip(String key) {
		if (resourceBundle != null) {
			try {
				return resourceBundle.getString(key);
			} catch (Throwable ignored) {
				return "";
			}
		} else {
			return "";
		}
	}

	public static void resetHelpFrame() {
		helpFrame = null;
	}

	public static void showHelp(String helpId) {
		showHelp(helpId, null);
	}

	public static boolean isHelpVisible() {
		return helpFrame != null;
	}

	public static URL buildHelpUrl(String helpId, String anchor) {
		StringBuffer urlBuffer = new StringBuffer();
		urlBuffer.append(INSTANCE.baseResourcePath);
		if (helpId != null && helpId.trim().length() > 0) {
			urlBuffer.append(helpId.trim());
			urlBuffer.append(".html");
		}

		if (anchor != null && anchor.trim().length() > 0) {
			urlBuffer.append("#");
			urlBuffer.append(anchor.trim());
		}

		return INSTANCE.getClass().getResource(urlBuffer.toString());
	}

	public static URL buildHelpUrlSimple(String helpId) {
		return buildHelpUrl(helpId, null);
	}

	public IntSize getWindowSize() {
		return this.windowSize;
	}

	public IntPair getWindowPosition() {
		return this.windowPosition;
	}
}
