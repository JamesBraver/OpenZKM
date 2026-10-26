package com.zelix.utils.dataset;

import com.zelix.framework.tree.ArchiveNode;
import java.io.File;

public class ArchiveEntry {
	public ArchiveNode node;
	public String name;
	public File file;

	public ArchiveEntry(ArchiveNode node, String name, File file) {
		this.node = node;
		this.name = name;
		this.file = file;
	}
}
