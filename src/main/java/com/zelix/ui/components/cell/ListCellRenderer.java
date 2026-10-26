package com.zelix.ui.components.cell;

import com.zelix.ui.utils.GuiUtils;
import java.awt.Color;
import java.awt.Component;
import javax.swing.JList;
import javax.swing.UIManager;

public class ListCellRenderer extends AbstractListCellRenderer {
	private boolean isDisabled;

	public ListCellRenderer(Object value, boolean isDisabled) {
		super(value);
		this.isDisabled = isDisabled;
	}

	@Override
	public void initRenderer(Component component, JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
		if (this.isDisabled) {
			Color foreground = component.getForeground();
			Color background = component.getBackground();
			Color selectionForeground = list.getSelectionForeground();
			Color selectionBackground = list.getSelectionBackground();
			Color disabledForeground = UIManager.getColor("Button.disabledForeground");
			if (disabledForeground != null
				&& disabledForeground instanceof Color
				&& (isSelected || GuiUtils.areColorsDistinct(foreground, disabledForeground))
				&& (isSelected || GuiUtils.areColorsDistinct(background, disabledForeground))
				&& (!isSelected || GuiUtils.areColorsDistinct(selectionForeground, disabledForeground))
				&& (!isSelected || GuiUtils.areColorsDistinct(selectionBackground, disabledForeground))) {
				component.setForeground(disabledForeground);
				return;
			}

			disabledForeground = UIManager.getColor("Menu.disabledForeground");
			if (disabledForeground != null
				&& disabledForeground instanceof Color
				&& (isSelected || GuiUtils.areColorsDistinct(foreground, disabledForeground))
				&& (isSelected || GuiUtils.areColorsDistinct(background, disabledForeground))
				&& (!isSelected || GuiUtils.areColorsDistinct(selectionForeground, disabledForeground))
				&& (!isSelected || GuiUtils.areColorsDistinct(selectionBackground, disabledForeground))) {
				component.setForeground(disabledForeground);
				return;
			}

			Color candidateForeground;
			if (!isSelected && (GuiUtils.isNearBlack(foreground) || GuiUtils.isNearWhite(foreground))
				|| isSelected && (GuiUtils.isNearBlack(background) || GuiUtils.isNearWhite(selectionForeground))) {
				if (!isSelected) {
					if (GuiUtils.isDarkColor(selectionBackground)) {
						candidateForeground = Color.gray;
					} else {
						candidateForeground = Color.lightGray;
					}
				} else if (GuiUtils.isDarkColor(background)) {
					candidateForeground = Color.gray;
				} else {
					candidateForeground = Color.lightGray;
				}
			} else if (isSelected) {
				candidateForeground = this.contrastColor(selectionForeground, selectionForeground);
			} else {
				candidateForeground = this.contrastColor(foreground, foreground);
			}

			if (GuiUtils.areColorsDistinct(background, candidateForeground)) {
				component.setForeground(candidateForeground);
				return;
			}

			byte colorDifferenceThreshold = 100;
			int contrastAttemptCount = 0;

			do {
				if (isSelected) {
					candidateForeground = this.contrastColor(candidateForeground, selectionForeground);
				} else {
					candidateForeground = this.contrastColor(candidateForeground, foreground);
				}

				if ((isSelected || GuiUtils.areColorsDistinct(foreground, candidateForeground, colorDifferenceThreshold))
					&& (isSelected || GuiUtils.areColorsDistinct(background, candidateForeground, colorDifferenceThreshold))
					&& (!isSelected || GuiUtils.areColorsDistinct(selectionForeground, candidateForeground, colorDifferenceThreshold))
					&& (!isSelected || GuiUtils.areColorsDistinct(selectionBackground, candidateForeground, colorDifferenceThreshold))) {
					component.setForeground(candidateForeground);
					return;
				}
			} while (contrastAttemptCount++ < 3);
		}
	}

	private Color contrastColor(Color color, Color referenceColor) {
		return !GuiUtils.isNearWhite(referenceColor) ? color.brighter() : color.darker();
	}

	public boolean isDisabled() {
		return this.isDisabled;
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof ListCellRenderer ? super.value.equals(((ListCellRenderer)obj).value) : false;
	}
}
