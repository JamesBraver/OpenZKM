package com.zelix.ui.components;

import java.awt.Component;
import java.util.StringTokenizer;
import javax.swing.JScrollPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class PlatformAdaptiveScrollPanel extends JScrollPane implements ChangeListener {
	private static final String javaVendor = System.getProperty("java.vendor", "Sun");
	private static final String javaVersion = System.getProperty("java.version");
	private static final String osName = System.getProperty("os.name");
	private Component viewComponent;
	private boolean repaintWorkaround;

	public PlatformAdaptiveScrollPanel(Component viewComponent) {
		super(viewComponent);
		this.viewComponent = viewComponent;
		this.initRepaintWorkaround();
	}

	private static boolean needsRepaintWorkaround() {
		if (javaVendor.indexOf("Sun") != -1 && osName.indexOf("Windows") != -1) {
			StringTokenizer versionTokenizer = new StringTokenizer(javaVersion, ".");
			int versionPartIndex = 0;

			while (versionTokenizer.hasMoreTokens()) {
				String versionPart = versionTokenizer.nextToken();
				switch (versionPartIndex) {
					case 0:
						try {
							if (Integer.parseInt(versionPart) > 1) {
								return false;
							}
						} catch (NumberFormatException ignoredMajorVersion) {
							return false;
						}
					default:
						versionPartIndex++;
						break;
					case 1:
						try {
							if (Integer.parseInt(versionPart) == 3) {
								return true;
							}

							return false;
						} catch (NumberFormatException ignoredMinorVersion) {
							return false;
						}
				}
			}
		}

		return false;
	}

	private void initRepaintWorkaround() {
		this.repaintWorkaround = needsRepaintWorkaround();
		this.getViewport().addChangeListener(this);
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		if (this.viewComponent != null) {
			this.viewComponent.repaint();
		}
	}
}
