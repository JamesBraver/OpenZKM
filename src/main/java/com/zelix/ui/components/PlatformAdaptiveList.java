package com.zelix.ui.components;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.StringTokenizer;
import javax.swing.JList;
import javax.swing.ListModel;

public class PlatformAdaptiveList<E> extends JList<E> {
	private static final String javaVendor = System.getProperty("java.vendor", "Sun");
	private static final String javaVersion = System.getProperty("java.version");
	private static final String osName = System.getProperty("os.name");
	private int mouseEventCount;
	private boolean repaintWorkaround;

	public PlatformAdaptiveList() {
		this.initRepaintWorkaround();
	}

	public PlatformAdaptiveList(ListModel<E> model) {
		super(model);
		this.initRepaintWorkaround();
	}

	private static boolean needsRepaintWorkaround() {
		if ((javaVendor.indexOf("Sun") != -1 || javaVendor.indexOf("IBM") != -1) && osName.indexOf("Windows") != -1) {
			StringTokenizer versionTokenizer = new StringTokenizer(javaVersion, ".");

			for (int versionPartIndex = 0; versionTokenizer.hasMoreTokens(); versionPartIndex++) {
				String versionPart = versionTokenizer.nextToken();
				switch (versionPartIndex) {
					case 0:
						try {
							if (Integer.parseInt(versionPart) > 1) {
								return false;
							}
							break;
						} catch (NumberFormatException ignoredMajorVersion) {
							return false;
						}
					case 1:
						try {
							int minorVersion = Integer.parseInt(versionPart);
							if (minorVersion < 4) {
								return true;
							}

							if (minorVersion > 4) {
								return false;
							}
							break;
						} catch (NumberFormatException ignoredMinorVersion) {
							return false;
						}
					case 2:
						if (versionPart.length() > 0) {
							if (versionPart.charAt(0) == '0') {
								return true;
							}

							return false;
						}
				}
			}
		}

		return false;
	}

	private void initRepaintWorkaround() {
		this.repaintWorkaround = needsRepaintWorkaround();
		this.addMouseListener(new PlatformAdaptiveList.RepaintMouseListener(this));
		this.addMouseMotionListener(new PlatformAdaptiveList.RepaintMouseMotionListener(this));
	}

	public static int incrementMouseEventCount(PlatformAdaptiveList<?> list) {
		return ++list.mouseEventCount;
	}

	public static boolean isRepaintWorkaroundEnabled(PlatformAdaptiveList<?> list) {
		return list.repaintWorkaround;
	}

	public static class RepaintMouseMotionListener implements MouseMotionListener {
		public final PlatformAdaptiveList<?> list;

		public RepaintMouseMotionListener(PlatformAdaptiveList<?> list) {
			this.list = list;
		}

		@Override
		public void mouseDragged(MouseEvent e) {
			if (PlatformAdaptiveList.isRepaintWorkaroundEnabled(this.list) && PlatformAdaptiveList.incrementMouseEventCount(this.list) % 30 == 0) {
				this.list.repaint();
			}
		}

		@Override
		public void mouseMoved(MouseEvent e) {
			if (PlatformAdaptiveList.isRepaintWorkaroundEnabled(this.list) && PlatformAdaptiveList.incrementMouseEventCount(this.list) % 30 == 0) {
				this.list.repaint();
			}
		}
	}

	public static class RepaintMouseListener implements MouseListener {
		public final PlatformAdaptiveList<?> list;

		public RepaintMouseListener(PlatformAdaptiveList<?> list) {
			this.list = list;
		}

		@Override
		public void mouseClicked(MouseEvent e) {
		}

		@Override
		public void mouseEntered(MouseEvent e) {
			if (PlatformAdaptiveList.isRepaintWorkaroundEnabled(this.list)) {
				this.list.repaint();
			}
		}

		@Override
		public void mouseExited(MouseEvent e) {
			if (PlatformAdaptiveList.isRepaintWorkaroundEnabled(this.list)) {
				this.list.repaint();
			}
		}

		@Override
		public void mousePressed(MouseEvent e) {
		}

		@Override
		public void mouseReleased(MouseEvent e) {
		}
	}
}
