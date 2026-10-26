package com.zelix.ui.components.panels;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.MediaTracker;
import javax.swing.JPanel;

public class LogoPanel extends JPanel {
	public Image logoImage;
	public boolean usingColorImage;
	public int imageWidth;
	public int imageHeight;

	public LogoPanel(Image colorImage, Image fallbackImage) {
		this.logoImage = colorImage;
		this.waitForImage();
		this.usingColorImage = true;
		if (!this.prepareImage(this.logoImage, this)) {
			this.logoImage = fallbackImage;
			this.usingColorImage = false;
			this.waitForImage();
			this.prepareImage(this.logoImage, this);
		}

		this.imageWidth = this.logoImage.getWidth(this);
		this.imageHeight = this.logoImage.getHeight(this);
		this.repaint();
	}

	private void waitForImage() {
		MediaTracker mediaTracker = new MediaTracker(this);
		mediaTracker.addImage(this.logoImage, 0);

		try {
			mediaTracker.waitForID(0);
		} catch (InterruptedException ignored) {
		}
	}

	public boolean isUsingColorImage() {
		return this.usingColorImage;
	}

	@Override
	public Dimension getMinimumSize() {
		return this.getPreferredSize();
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(this.imageWidth, this.imageHeight);
	}

	@Override
	public void paint(Graphics g) {
		this.update(g);
	}

	@Override
	public void update(Graphics g) {
		g.drawImage(this.logoImage, 0, 0, this.imageWidth, this.imageHeight, this);
	}
}
