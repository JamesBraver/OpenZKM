package com.zelix.tools.renamer.packages;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public abstract class PackageNode implements Comparable<PackageNode> {
	public Set<PackageTreeNode> childPackages = new LinkedHashSet<>();
	private boolean allFromFilesystem = true;

	public void collectPackageNames(ArrayList<String> names) {
		this.addOwnPackageName(names);
		Iterator<PackageTreeNode> childPackageIterator = this.childPackages.iterator();

		while (childPackageIterator.hasNext()) {
			childPackageIterator.next().collectPackageNames(names);
		}
	}

	public final int compareByPackageName(PackageNode otherNode) {
		return this.getPackageName().toLowerCase().compareTo(otherNode.getPackageName().toLowerCase());
	}

	public boolean isAllFromFilesystem() {
		return this.allFromFilesystem;
	}

	public void addOwnPackageName(ArrayList<String> packageNames) {
	}

	public void collectPackageNodes(Map<String, PackageTreeNode> packageNodes) {
		this.addSelfToMap(packageNodes);
		Iterator<PackageTreeNode> childPackageIterator = this.childPackages.iterator();

		while (childPackageIterator.hasNext()) {
			childPackageIterator.next().collectPackageNodes(packageNodes);
		}
	}

	@Override
	public int compareTo(PackageNode other) {
		return this.compareByPackageName(other);
	}

	public abstract boolean isRootNode();

	public abstract void addSelfToMap(Map<String, PackageTreeNode> packageNodes);

	public abstract String getPackageName();

	public final void addPackagePath(int index, String[] pathSegments, Map<String, Boolean> fromFilesystemByPackage) {
		if (index < pathSegments.length) {
			String pathSegment = pathSegments[index];

			for (PackageTreeNode childPackage : this.childPackages) {
				if (pathSegment.equals(childPackage.getSimpleName())) {
					childPackage.addPackagePath(++index, pathSegments, fromFilesystemByPackage);
					return;
				}
			}

			String parentPackageName = this.getPackageName();
			String newPackageName = (parentPackageName.length() > 0 ? parentPackageName + "/" : "") + pathSegment;
			boolean fromFilesystem = false;
			if (fromFilesystemByPackage != null && fromFilesystemByPackage.containsKey(newPackageName)) {
				fromFilesystem = fromFilesystemByPackage.get(newPackageName);
			}

			PackageTreeNode newPackageNode = new PackageTreeNode(this, pathSegment, fromFilesystem);
			this.childPackages.add(newPackageNode);
			newPackageNode.addPackagePath(++index, pathSegments, fromFilesystemByPackage);
		}
	}

	public void setAllFromFilesystem(boolean allFromFilesystem) {
		this.allFromFilesystem = allFromFilesystem;
	}
}
