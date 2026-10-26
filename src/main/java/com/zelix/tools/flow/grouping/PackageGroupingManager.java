package com.zelix.tools.flow.grouping;

import com.zelix.framework.AbstractClassNode;
import com.zelix.tools.flow.Condition;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.collections.NamedSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Map;
import java.util.Set;

public class PackageGroupingManager {
	private NamedSet<AbstractClassNode> groupedClasses = GlobalUtils.newNamedSet();
	private Set<String> packageNames = GlobalUtils.newHashSet();
	private Map<String, Condition> packageConditions = GlobalUtils.newHashMap();
	private PackageGrouping grouping;
	private Condition groupingCondition;

	public PackageGroupingManager(PackageGrouping grouping) {
		this.grouping = grouping;
		Iterator<NamedSet<? extends AbstractClassNode>> groupIterator = grouping.getGroups().iterator();

		while (groupIterator.hasNext()) {
			this.groupedClasses.addAll(groupIterator.next());
		}
	}

	public void collectPackageNames() {
		Iterator<AbstractClassNode> classIterator = this.groupedClasses.iterator();

		while (classIterator.hasNext()) {
			String packageName = classIterator.next().getPackageName();
			this.packageNames.add(packageName);
		}
	}

	public boolean containsClass(AbstractClassNode classNode) {
		return this.grouping.containsClass(classNode);
	}

	public void removeClass(Object classNode) {
		this.groupedClasses.remove(classNode);
	}

	public Condition getPackageCondition(String packageName) {
		return this.packageConditions.get(packageName);
	}

	public Set<AbstractClassNode> getCommonClasses() {
		return this.grouping.getCommonClasses();
	}

	public boolean hasAllConditions() {
		return this.groupingCondition != null && this.packageConditions.size() == this.packageNames.size();
	}

	public boolean containsCommonClass(AbstractClassNode classNode) {
		return this.grouping.containsCommonClass(classNode);
	}

	public List<String> getSortedPackageNames() {
		ArrayList<String> sortedPackageNames = new ArrayList<>(this.packageNames);
		Collections.sort(sortedPackageNames);
		return sortedPackageNames;
	}

	public void setGroupingCondition(Condition condition) {
		this.groupingCondition = condition;
	}

	public List<String> getGroupNames() {
		return this.grouping.getGroupNames();
	}

	public List<Condition> getPackageConditions() {
		ArrayList<Condition> conditions = new ArrayList<>(this.packageConditions.size());

		for (Entry<String, Condition> conditionByPackageName : this.packageConditions.entrySet()) {
			conditions.add(conditionByPackageName.getValue());
		}

		return conditions;
	}

	public Set<String> getPackageNames() {
		return Collections.unmodifiableSet(this.packageNames);
	}

	public Condition getGroupingCondition() {
		return this.groupingCondition;
	}

	public void putPackageCondition(String packageName, Condition condition) {
		this.packageConditions.put(packageName, condition);
	}

	public Set<AbstractClassNode> getGroupedClasses() {
		return Collections.unmodifiableSet(this.groupedClasses);
	}
}
