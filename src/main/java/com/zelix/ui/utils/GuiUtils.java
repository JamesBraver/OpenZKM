package com.zelix.ui.utils;

import com.zelix.exceptions.LookupException;
import com.zelix.ui.base.BaseEscapableDialog;
import com.zelix.ui.prompts.CopyablePrompt;
import com.zelix.ui.prompts.ErrorPrompt;
import java.awt.Color;
import java.awt.Component;
import java.awt.Window;
import java.lang.reflect.InvocationTargetException;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class GuiUtils {
	public static Class<?>[] NO_PARAM_TYPES = new Class<?>[0];
	public static String ESCAPE_ACTION_NAME = "ESCAPE_ACTION_NAME";
	public static final String LINE_SEPARATOR = System.getProperty("line.separator", "\n");

	public static boolean isNearWhite(Color color) {
		int channelSum = color.getRed() + color.getBlue() + color.getGreen();
		return 765 - channelSum < 50;
	}

	private static void setHtmlTextNow(JEditorPane editorPane, String html) {
		editorPane.setContentType("text/html");
		editorPane.setText(html);
		editorPane.setCaretPosition(0);
	}

	public static void packWindow(Window window) {
		if (SwingUtilities.isEventDispatchThread()) {
			window.pack();
		} else {
			try {
				SwingUtilities.invokeAndWait(window::pack);
			} catch (InterruptedException interruptedException) {
				throw new LookupException(interruptedException.getMessage() + " " + window.getClass().getName());
			} catch (InvocationTargetException invocationTargetException) {
				throw new LookupException(invocationTargetException.getTargetException().getMessage() + " " + window.getClass().getName());
			}
		}
	}

	public static boolean isNearBlack(Color color) {
		return color.getRed() + color.getBlue() + color.getGreen() < 50;
	}

	public static boolean isDarkColor(Color color) {
		return color.getRed() + color.getBlue() + color.getGreen() < 255;
	}

	public static void setHtmlText(JEditorPane editorPane, String html) {
		if (SwingUtilities.isEventDispatchThread()) {
			setHtmlTextNow(editorPane, html);
		} else {
			SwingUtilities.invokeLater(new GuiUtils.SetHtmlTextTask(editorPane, html));
		}
	}

	public static boolean areColorsDistinct(Color color1, Color color2) {
		return areColorsDistinct(color1, color2, 150);
	}

	public static void requestFocusInWindow(Component component) {
		if (SwingUtilities.isEventDispatchThread()) {
			requestFocusInWindowNow(component);
		} else {
			SwingUtilities.invokeLater(new GuiUtils.RequestFocusTask(component));
		}
	}

	private static void requestFocusInWindowNow(Component component) {
		try {
			component.getClass().getMethod("requestFocusInWindow", NO_PARAM_TYPES).invoke(component, NO_PARAM_TYPES);
		} catch (Throwable ignored) {
			component.requestFocus();
		}
	}

	public static BaseEscapableDialog createMessageDialog(JFrame frame, String title, String label, String message) {
		return (BaseEscapableDialog)(message.length() > 150 ? new ErrorPrompt(frame, title, label, message) : new CopyablePrompt(frame, title, message));
	}

	public static void setVisible(Window window, boolean visible) {
		if (SwingUtilities.isEventDispatchThread()) {
			window.setVisible(visible);
		} else {
			try {
				SwingUtilities.invokeAndWait(new GuiUtils.SetVisibleTask(window, visible));
			} catch (InterruptedException interruptedException) {
				throw new LookupException(interruptedException.getMessage() + " " + window.getClass().getName());
			} catch (InvocationTargetException invocationTargetException) {
				throw new LookupException(invocationTargetException.getTargetException().getMessage() + " " + window.getClass().getName());
			}
		}
	}

	public static boolean areColorsDistinct(Color color1, Color color2, int threshold) {
		int color1Red = color1.getRed();
		int color1Blue = color1.getBlue();
		int color1Green = color1.getGreen();
		int color2Red = color2.getRed();
		int color2Blue = color2.getBlue();
		int color2Green = color2.getGreen();
		int channelDeltaSum = Math.abs(color1Red - color2Red);
		channelDeltaSum += Math.abs(color1Blue - color2Blue);
		channelDeltaSum += Math.abs(color1Green - color2Green);
		int maxChannelDelta = Math.abs(color1Red - color2Red);
		int channelDelta = Math.abs(color1Blue - color2Blue);
		if (channelDelta > maxChannelDelta) {
			maxChannelDelta = channelDelta;
		}

		channelDelta = Math.abs(color1Green - color2Green);
		if (channelDelta > maxChannelDelta) {
			maxChannelDelta = channelDelta;
		}

		return channelDeltaSum >= threshold || maxChannelDelta > threshold * 2 / 3;
	}

	public static void requestFocusInWindowOnEdt(Component component) {
		requestFocusInWindowNow(component);
	}

	public static void setHtmlTextOnEdt(JEditorPane editorPane, String html) {
		setHtmlTextNow(editorPane, html);
	}

	public static class SetVisibleTask implements Runnable {
		public final Window window;
		public final boolean visible;

		public SetVisibleTask(Window window, boolean visible) {
			this.window = window;
			this.visible = visible;
		}

		@Override
		public void run() {
			this.window.setVisible(this.visible);
		}
	}

	public static class RequestFocusTask implements Runnable {
		public final Component component;

		public RequestFocusTask(Component component) {
			this.component = component;
		}

		@Override
		public void run() {
			GuiUtils.requestFocusInWindowOnEdt(this.component);
		}
	}

	public static class SetHtmlTextTask implements Runnable {
		public final JEditorPane editorPane;
		public final String html;

		public SetHtmlTextTask(JEditorPane editorPane, String html) {
			this.editorPane = editorPane;
			this.html = html;
		}

		@Override
		public void run() {
			GuiUtils.setHtmlTextOnEdt(this.editorPane, this.html);
		}
	}
}
