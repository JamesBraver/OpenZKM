package com.zelix.ui.dialogs.filter;

import com.zelix.HelpManager;
import com.zelix.ui.components.panels.base.ContainingPanel;
import com.zelix.ui.components.panels.filter.ContainingClassPanel;
import com.zelix.ui.components.panels.filter.ContainingPackagePanel;
import com.zelix.ui.dataset.ParameterModel;
import com.zelix.ui.dialogs.base.BaseAddFilterDialog;
import com.zelix.utils.function.ZKMConsumer;
import javax.swing.JFrame;

public class AddClassFilterDialog extends BaseAddFilterDialog {
	public ContainingPackagePanel packagePanel;
	public ContainingPanel classPanel;

	public AddClassFilterDialog(JFrame parent, String title, ParameterModel parameterModel, int targetType, ZKMConsumer resultConsumer) {
		super(parent, title, parameterModel, targetType, resultConsumer);
	}

	@Override
	protected final void openHelpGUI() {
		if (super.targetType == 1) {
			HelpManager.showHelp("028");
		} else {
			HelpManager.showHelp("022");
		}
	}

	@Override
	public void initTabs() {
		this.packagePanel = new ContainingPackagePanel(this, super.parameterModel, super.targetType == 1, super.targetType);
		this.classPanel = new ContainingClassPanel(this, super.parameterModel, false, super.targetType);
		super.tabbedPane.addTab("Containing package", null, this.packagePanel, HelpManager.getToolTip("CONTAINING_PACKAGE_PART_OF_PARAM"));
		super.tabbedPane.addTab("Class", null, this.classPanel, HelpManager.getToolTip("CLASS_PART_OF_PARAM"));
		super.tabbedPane.setSelectedComponent(this.classPanel);
	}
}
