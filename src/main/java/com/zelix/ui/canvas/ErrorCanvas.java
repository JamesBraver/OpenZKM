package com.zelix.ui.canvas;

import java.awt.Canvas;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.util.StringTokenizer;
import java.util.Vector;

public class ErrorCanvas extends Canvas {
	public static int lineSpacing = 1;
	private String text;
	private int maxWidth;
	private FontMetrics fontMetrics;
	private int ascent;
	private int descent;
	private Vector<String> lines;
	private boolean wrapped;

	public ErrorCanvas(String text, int maxWidth, Font font) {
		this.text = text;
		this.maxWidth = maxWidth;
		this.setFont(font);
		this.fontMetrics = this.getFontMetrics(this.getFont());
		this.ascent = this.fontMetrics.getAscent();
		this.descent = this.fontMetrics.getDescent();
	}

	public void wrapText() {
		this.lines = new Vector<>();
		StringTokenizer tokenizer = new StringTokenizer(this.text, " \n\r", true);
		StringBuffer lineBuffer = new StringBuffer();
		int lineWidth = 0;
		int tokenCount = tokenizer.countTokens();

		for (int tokenIndex = 0; tokenIndex < tokenCount; tokenIndex++) {
			String token = tokenizer.nextToken();
			if (!token.equals("\n") && !token.equals("\r")) {
				int tokenWidth = this.fontMetrics.stringWidth(token);
				lineWidth += tokenWidth;
				if (lineWidth <= this.maxWidth) {
					lineBuffer.append(token);
				} else {
					this.lines.addElement(lineBuffer.toString());
					lineBuffer = new StringBuffer();
					if (!token.equals(" ")) {
						lineBuffer.append(token);
						lineWidth = tokenWidth;
					} else {
						lineWidth = 0;
					}
				}
			} else {
				this.lines.addElement(lineBuffer.toString());
				lineBuffer = new StringBuffer();
				lineWidth = 0;
			}

			if (tokenIndex == tokenCount - 1) {
				this.lines.addElement(lineBuffer.toString());
			}

			this.wrapped = true;
		}
	}

	@Override
	public Dimension getMinimumSize() {
		return new Dimension(this.maxWidth, this.getTextHeight());
	}

	@Override
	public Dimension getPreferredSize() {
		return this.getMinimumSize();
	}

	@Override
	public void invalidate() {
		super.invalidate();
		this.wrapped = false;
	}

	public int getTextHeight() {
		if (!this.wrapped) {
			this.wrapText();
		}

		int textHeight;
		if (this.lines == null) {
			textHeight = 0;
		} else {
			textHeight = this.lines.size() * (this.ascent + this.descent + lineSpacing);
		}

		return textHeight;
	}

	@Override
	public void paint(Graphics g) {
		if (!this.wrapped) {
			this.wrapText();
		}

		Dimension canvasSize = this.getSize();
		g.clearRect(0, 0, canvasSize.width, canvasSize.height);
		int baselineY = this.ascent;

		for (int i = 0; i < this.lines.size(); i++) {
			String line = this.lines.elementAt(i);
			g.drawString(line, 0, baselineY);
			baselineY += this.ascent + this.descent + lineSpacing;
		}
	}

	@Override
	public synchronized void setBounds(int x, int y, int width, int height) {
		super.setBounds(x, y, width, height);
		this.maxWidth = this.getSize().width;
	}

	@Override
	public void update(Graphics g) {
		this.paint(g);
	}

	@Override
	public void validate() {
		super.validate();
		this.repaint();
	}
}
