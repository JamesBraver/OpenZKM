package com.zelix.ui.dialogs.filter;

import com.zelix.HelpManager;
import com.zelix.ui.components.panels.base.ContainingPanel;
import com.zelix.ui.components.panels.filter.ContainingClassPanel;
import com.zelix.ui.components.panels.filter.ContainingFieldPanel;
import com.zelix.ui.components.panels.filter.ContainingPackagePanel;
import com.zelix.ui.dataset.ParameterModel;
import com.zelix.ui.dialogs.base.BaseAddFilterDialog;
import com.zelix.utils.function.ZKMConsumer;
import javax.swing.JFrame;

public class AddFieldFilterDialog extends BaseAddFilterDialog {
	public ContainingPackagePanel packagePanel;
	public ContainingPanel classPanel;
	public ContainingFieldPanel fieldPanel;

	public AddFieldFilterDialog(JFrame parent, String title, ParameterModel parameterModel, int targetType, ZKMConsumer resultConsumer) {
		super(parent, title, parameterModel, targetType, resultConsumer);
	}

	@Override
	protected final void openHelpGUI() {
		if (super.targetType == 1) {
			HelpManager.showHelp("029");
		} else if (super.targetType == 2) {
			HelpManager.showHelp("023");
		} else if (super.targetType == 3) {
			HelpManager.showHelp("043");
		}
	}

	@Override
	public void initTabs() {
		this.packagePanel = new ContainingPackagePanel(this, super.parameterModel, super.targetType == 1, super.targetType);
		boolean higherLevelExcludeEnabled = super.targetType != 3;
		int targetType = super.targetType;
		ParameterModel parameterModel = super.parameterModel;
		this.classPanel = new ContainingClassPanel(this, parameterModel, higherLevelExcludeEnabled, targetType);
		this.fieldPanel = new ContainingFieldPanel(this, super.parameterModel, super.targetType);
		super.tabbedPane.addTab("Containing package", null, this.packagePanel, HelpManager.getToolTip("CONTAINING_PACKAGE_PART_OF_PARAM"));
		super.tabbedPane.addTab("Containing class", null, this.classPanel, HelpManager.getToolTip("CONTAINING_CLASS_PART_OF_PARAM"));
		super.tabbedPane.addTab("Field", null, this.fieldPanel, HelpManager.getToolTip("FIELD_PART_OF_PARAM"));
		super.tabbedPane.setSelectedComponent(this.fieldPanel);
	}
}
