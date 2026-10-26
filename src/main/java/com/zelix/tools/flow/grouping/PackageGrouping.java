package com.zelix.tools.flow.grouping;

import com.zelix.framework.AbstractClassNode;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.collections.NamedSet;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class PackageGrouping implements Serializable, Comparable<PackageGrouping> {
	private NamedSet<NamedSet<? extends AbstractClassNode>> groups = GlobalUtils.newNamedSet();
	private NamedSet<? extends AbstractClassNode> commonClasses;
	private NamedSet<? extends AbstractClassNode> allClasses;

	public PackageGrouping(NamedSet<? extends AbstractClassNode> classGroup) {
		this.groups.add(classGroup);
		this.commonClasses = GlobalUtils.newNamedHashSet(classGroup);
		this.allClasses = GlobalUtils.newNamedHashSet(classGroup);
	}

	public PackageGrouping(NamedSet<NamedSet<? extends AbstractClassNode>> groups, NamedSet<? extends AbstractClassNode> commonClasses) {
		this.groups.addAll(groups);
		this.commonClasses = commonClasses;
		NamedSet<AbstractClassNode> groupedClasses = GlobalUtils.newNamedSet();
		this.allClasses = groupedClasses;

		for (NamedSet<? extends AbstractClassNode> group : this.groups) {
			groupedClasses.addAll(group);
		}
	}

	public NamedSet<NamedSet<? extends AbstractClassNode>> getGroupsNotIn(PackageGrouping other) {
		NamedSet<NamedSet<? extends AbstractClassNode>> groupsNotInOther = GlobalUtils.newNamedSet();
		NamedSet<NamedSet<? extends AbstractClassNode>> otherGroups = other.groups;

		for (NamedSet<? extends AbstractClassNode> group : this.groups) {
			if (!otherGroups.contains(group)) {
				groupsNotInOther.add(group);
			}
		}

		return groupsNotInOther;
	}

	public NamedSet<NamedSet<? extends AbstractClassNode>> getCommonGroups(PackageGrouping other) {
		NamedSet<NamedSet<? extends AbstractClassNode>> commonGroups = GlobalUtils.newNamedSet();
		NamedSet<NamedSet<? extends AbstractClassNode>> smallerGroups;
		NamedSet<NamedSet<? extends AbstractClassNode>> largerGroups;
		if (this.groups.size() > other.groups.size()) {
			smallerGroups = other.groups;
			largerGroups = this.groups;
		} else {
			smallerGroups = this.groups;
			largerGroups = other.groups;
		}

		for (NamedSet<? extends AbstractClassNode> group : smallerGroups) {
			if (largerGroups.contains(group)) {
				commonGroups.add(group);
			}
		}

		return commonGroups;
	}

	public boolean containsCommonClass(Object classNode) {
		return this.commonClasses.contains(classNode);
	}

	public int compareToGrouping(PackageGrouping other) {
		if (this.getGroupCount() < other.getGroupCount()) {
			return -1;
		} else if (this.getGroupCount() == other.getGroupCount()) {
			if (this.getCommonClassCount() < other.getCommonClassCount()) {
				return -1;
			} else {
				return this.getCommonClassCount() == other.getCommonClassCount() ? 0 : 1;
			}
		} else {
			return 1;
		}
	}

	public int getCommonClassCount() {
		return this.commonClasses.size();
	}

	public boolean containsClass(Object classNode) {
		return this.allClasses.contains(classNode);
	}

	public Set<NamedSet<? extends AbstractClassNode>> getGroups() {
		return Collections.unmodifiableSet(this.groups);
	}

	public List<String> getGroupNames() {
		ArrayList<String> groupNames = new ArrayList<>(this.groups.size());

		for (NamedSet<? extends AbstractClassNode> group : this.groups) {
			groupNames.add(group.getName());
		}

		return groupNames;
	}

	@Override
	public int compareTo(PackageGrouping other) {
		return this.compareToGrouping(other);
	}

	public PackageGrouping intersect(PackageGrouping other) {
		NamedSet<? extends AbstractClassNode> intersectedClasses = GlobalUtils.clone(this.commonClasses);
		intersectedClasses.retainAll(other.commonClasses);
		if (intersectedClasses.size() > 0) {
			NamedSet<NamedSet<? extends AbstractClassNode>> combinedGroups = GlobalUtils.newNamedHashSet(this.groups);
			combinedGroups.addAll(other.groups);
			return new PackageGrouping(combinedGroups, intersectedClasses);
		} else {
			return null;
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof PackageGrouping) {
			PackageGrouping otherGrouping = (PackageGrouping)obj;
			return this.groups.equals(otherGrouping.groups);
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return this.groups.hashCode();
	}

	public int getGroupCount() {
		return this.groups.size();
	}

	public Set<AbstractClassNode> getCommonClasses() {
		return Collections.unmodifiableSet(this.commonClasses);
	}
}
