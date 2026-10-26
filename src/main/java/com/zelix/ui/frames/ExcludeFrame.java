package com.zelix.ui.frames;

import com.zelix.HelpManager;
import com.zelix.klassmaster.config.FilterConfig;
import com.zelix.klassmaster.constant.CopyrightConstants;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.ui.GUI;
import com.zelix.ui.dataset.ClassNodeEntry;
import com.zelix.ui.dataset.ParameterModel;
import com.zelix.ui.dialogs.ObfuscateNameExcludeDialog;
import com.zelix.ui.dialogs.filter.AddClassFilterDialog;
import com.zelix.ui.dialogs.filter.AddClassSuffixFilterDialog;
import com.zelix.ui.dialogs.filter.AddFieldFilterDialog;
import com.zelix.ui.dialogs.filter.AddMethodFilterDialog;
import com.zelix.ui.dialogs.filter.AddPackageFilterDialog;
import com.zelix.ui.layouts.PowerLayout;
import com.zelix.ui.utils.GuiUtils;
import com.zelix.utils.comparators.CaseInsensitiveComparator;
import com.zelix.utils.function.ZKMConsumer;
import com.zelix.workspace.PackageManager;
import java.awt.Container;
import java.awt.event.ItemEvent;
import java.util.Collections;
import java.util.List;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;

public abstract class ExcludeFrame extends TaskPreparingFrame {
	public static String[] layoutProperties = new String[]{
		"mainParamList.top=5",
		"mainParamList.left=5",
		"mainParamList.right=container.right-btnWidth-10",
		"mainParamList.bottom=container.height*50/100",
		"explanationArea.left=5",
		"explanationArea.top=mainParamList.bottom+5",
		"explanationArea.right=container.right-5",
		"explanationArea.bottom=container.bottom-5",
		"btnWidth=max(mainAddBtn.defaultWidth, mainModifyBtn.defaultWidth, mainDeleteBtn.defaultWidth)",
		"mainAddBtn.width=btnWidth",
		"mainAddBtn.left=mainParamList.right+5",
		"mainAddBtn.centerY=container.height*20/200",
		"mainModifyBtn.width=btnWidth",
		"mainModifyBtn.left=mainAddBtn.left",
		"mainModifyBtn.centerY=container.height*50/200",
		"mainDeleteBtn.width=btnWidth",
		"mainDeleteBtn.left=mainAddBtn.left",
		"mainDeleteBtn.centerY=container.height*80/200"
	};
	public static String[] extraLayoutProperties = new String[]{
		"applicationTypeLbl.centerY=applicationTypeComboBox.centerY",
		"applicationTypeLbl.left=5",
		"startSecondColumn=max(applicationTypeLbl.defaultWidth, excludePackageNamesLbl.defaultWidth, excludeMainClassLbl.defaultWidth)+10",
		"applicationTypeComboBox.top=5",
		"applicationTypeComboBox.left=startSecondColumn",
		"applicationTypeComboBox.right=container.right-5",
		"excludePackageNamesLbl.left=5",
		"excludePackageNamesLbl.centerY=excludedPackageComboBox.centerY",
		"excludedPackageComboBox.left=startSecondColumn",
		"excludedPackageComboBox.right=container.right-5",
		"excludedPackageComboBox.top=applicationTypeComboBox.bottom+5",
		"excludeMainClassLbl.left=5",
		"excludeMainClassLbl.centerY=mainClassComboBox.centerY",
		"mainClassComboBox.left=startSecondColumn",
		"mainClassComboBox.top=excludedPackageComboBox.bottom+5",
		"mainClassComboBox.right=container.right-5",
		"container.defaultRightPad=5",
		"container.defaultBottomPad=5"
	};
	public PackageManager packageManager;
	public DefaultComboBoxModel<String> excludedPackageModel;
	public JComboBox<String> excludedPackageComboBox;
	public boolean ignoreSelectionChange;

	public ExcludeFrame(String title, GUI gui, FilterConfig filterConfig, PackageManager packageManager, List<ClassNodeEntry> classNodes, ScriptContext scriptContext, ZKMConsumer resultConsumer, int parameterCategory) {
		super(title, gui, classNodes, filterConfig, scriptContext, resultConsumer, parameterCategory);
		this.packageManager = packageManager;
		this.initFrame(title);
		GuiUtils.requestFocusInWindow(super.okButton);
	}

	@Override
	public final void applyToConfig() {
		super.applyToConfig();
		switch (this.excludedPackageComboBox.getSelectedIndex()) {
			case -1:
			case 0:
				((FilterConfig)super.filterConfig).setPackageQualifier(false, null);
				break;
			case 1:
				((FilterConfig)super.filterConfig).setPackageQualifier(true, "*.");
				break;
			default:
				((FilterConfig)super.filterConfig).setPackageQualifier(true, (String)this.excludedPackageComboBox.getSelectedItem() + ".");
		}
	}

	@Override
	public String[] getExtraLayoutProperties() {
		return extraLayoutProperties;
	}

	@Override
	public void modifyParameter(ParameterModel parameterModel, String ruleString, ZKMConsumer consumer) {
		switch (parameterModel.getTargetType()) {
			case 1:
				this.showParameterDialog("1", ruleString, consumer);
				break;
			case 2:
				if (parameterModel.isClassLinkPattern()) {
					this.showParameterDialog("5", ruleString, consumer);
				} else {
					this.showParameterDialog("2", ruleString, consumer);
				}
				break;
			case 3:
				this.showParameterDialog("3", ruleString, consumer);
				break;
			case 4:
				this.showParameterDialog("4", ruleString, consumer);
		}
	}

	@Override
	public String[] getLayoutProperties() {
		return layoutProperties;
	}

	@Override
	public String getEntryRuleString(ClassNodeEntry entry) {
		if (entry != null) {
			String binaryName = entry.getBinaryName();
			return this.getRuleString(binaryName, entry.isMainClass());
		} else {
			return null;
		}
	}

	private void validateSelection() {
		if (!this.ignoreSelectionChange) {
			switch (this.excludedPackageComboBox.getSelectedIndex()) {
				case -1:
				case 0:
					break;
				case 1:
					if (!super.paramMap.containsKey("*.")) {
						this.excludedPackageComboBox.setSelectedIndex(0);
					}
					break;
				default:
					if (!super.paramMap.containsKey((String)this.excludedPackageComboBox.getSelectedItem() + ".")) {
						this.excludedPackageComboBox.setSelectedIndex(0);
					}
			}
		}
	}

	@Override
	public void onParametersChanged() {
		super.onParametersChanged();
		this.validateSelection();
	}

	@Override
	public String getRuleString(String binaryName, boolean hasMainMethod) {
		String ruleString = null;
		if (binaryName != null) {
			int lastSeparatorIndex = binaryName.lastIndexOf(46);
			if (lastSeparatorIndex != -1) {
				ruleString = binaryName.substring(0, lastSeparatorIndex + 1) + "^" + binaryName.substring(lastSeparatorIndex + 1);
			} else {
				ruleString = binaryName;
			}

			if (hasMainMethod) {
				ruleString = ruleString + "^ " + TaskPreparingFrame.mainMethodPattern;
			}
		}

		return ruleString;
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		if (e.getSource() == this.excludedPackageComboBox) {
			if (this.ignoreSelectionChange) {
				return;
			}

			int packageIndex = this.excludedPackageModel.getIndexOf(e.getItem());
			switch (packageIndex) {
				case -1:
				case 0:
					break;
				case 1:
					if (e.getStateChange() == 1) {
						this.addParameter("*.");
					} else {
						this.ignoreSelectionChange = true;
						this.removeParameter("*.");
						this.ignoreSelectionChange = false;
					}
					break;
				default:
					String ruleString = this.excludedPackageModel.getElementAt(packageIndex) + ".";
					if (e.getStateChange() == 1) {
						this.addParameter(ruleString);
					} else {
						this.ignoreSelectionChange = true;
						this.removeParameter(ruleString);
						this.ignoreSelectionChange = false;
					}
			}
		} else {
			super.itemStateChanged(e);
		}
	}

	@Override
	public void showParameterDialog(Object targetType, String ruleString, ZKMConsumer consumer) {
		String parameterType = (String)targetType;
		if (parameterType.equals("1")) {
			if (ruleString == null) {
				ruleString = "*.";
			}

			new AddPackageFilterDialog(this, CopyrightConstants.NAME + " Package Exclude Parameter", this.parseParameter(ruleString), 1, consumer);
		} else if (parameterType.equals("2")) {
			if (ruleString == null) {
				ruleString = "*.*";
			}

			new AddClassFilterDialog(this, CopyrightConstants.NAME + " Class Exclude Parameter", this.parseParameter(ruleString), 1, consumer);
		} else if (parameterType.equals("3")) {
			if (ruleString == null) {
				ruleString = "*.* *";
			}

			new AddFieldFilterDialog(this, CopyrightConstants.NAME + " Field Exclude Parameter", this.parseParameter(ruleString), 1, consumer);
		} else if (parameterType.equals("4")) {
			if (ruleString == null) {
				ruleString = "*.* *(*)";
			}

			new AddMethodFilterDialog(this, CopyrightConstants.NAME + " Method Exclude Parameter", this.parseParameter(ruleString), 1, consumer);
		} else if (parameterType.equals("5")) {
			if (ruleString == null) {
				ruleString = "*.<link>X";
			}

			new AddClassSuffixFilterDialog(this, CopyrightConstants.NAME + " Class Suffix Exclude Parameter", this.parseParameter(ruleString), 1, consumer);
		}
	}

	@Override
	public final void showHelp() {
		HelpManager.showHelp("025");
	}

	@Override
	public void addExtraComponents(PowerLayout layout, Container container) {
		JLabel label = new JLabel("Exclude package qualifiers:");
		container.add(label, "excludePackageNamesLbl");
		this.excludedPackageModel = new DefaultComboBoxModel<>();
		this.excludedPackageComboBox = new JComboBox<>(this.excludedPackageModel);
		container.add(this.excludedPackageComboBox, "excludedPackageComboBox");
		this.excludedPackageComboBox.addItem("<None>");
		this.excludedPackageComboBox.addItem("<All>");
		if (this.packageManager != null) {
			List<String> packageNames = this.packageManager.getPackageNames();
			Collections.sort(packageNames, CaseInsensitiveComparator.getInstance());

			for (int i = 0; i < packageNames.size(); i++) {
				String packageName = packageNames.get(i).replace('/', '.');
				this.excludedPackageComboBox.addItem(packageName);
			}

			if (((FilterConfig)super.filterConfig).isExcludePackageQualifier() && super.filterConfig.isPersisted()) {
				String packageQualifier = ((FilterConfig)super.filterConfig).getPackageQualifier();
				if (packageQualifier.equals("*.")) {
					this.excludedPackageComboBox.setSelectedIndex(1);
				} else {
					String qualifierPackageName = packageQualifier.substring(0, packageQualifier.length() - 1);
					int selectedIndex = this.excludedPackageModel.getIndexOf(qualifierPackageName);
					if (selectedIndex > -1) {
						this.excludedPackageComboBox.setSelectedIndex(selectedIndex);
					} else {
						this.removeParameter(packageQualifier);
						this.excludedPackageComboBox.setSelectedIndex(0);
					}
				}
			} else {
				this.excludedPackageComboBox.setSelectedIndex(0);
			}
		} else {
			this.excludedPackageComboBox.setEnabled(false);
		}

		this.addMainClassSelector(container, "Don't change main class name:", HelpManager.getToolTip("EXCLUDE_MAIN_CLASS_NAME_LIST"));
		this.excludedPackageComboBox.addItemListener(this);
		this.excludedPackageComboBox.setToolTipText(HelpManager.getToolTip("EXCLUDE_PACKAGE_NAMES_LIST"));
	}

	@Override
	public void showParameterTypeDialog(ZKMConsumer consumer) {
		new ObfuscateNameExcludeDialog(this, CopyrightConstants.NAME + " Exclude Parameter Type", consumer);
	}
}
