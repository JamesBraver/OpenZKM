package com.zelix.workspace.resources;

import com.zelix.framework.wrappers.ClassNode;
import com.zelix.utils.GlobalUtils;
import com.zelix.workspace.ClassHierarchyNode;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;
import java.util.StringTokenizer;

public class YAMLFileProcessor {
	public YAMLFileProcessor(BufferedReader reader, String archivePath, Map<ClassNode, String> classNameToArchivePathMap) throws IOException {
		String line;
		while ((line = reader.readLine()) != null) {
			int commentIndex = line.indexOf(35);
			if (commentIndex > -1) {
				line = line.substring(0, commentIndex);
			}

			StringTokenizer tokenizer = new StringTokenizer(line, " []{},");

			while (tokenizer.hasMoreTokens()) {
				ClassNode classNode = ClassHierarchyNode.getWorkspaceClassNode(GlobalUtils.binaryToInternalName(tokenizer.nextToken()));
				if (classNode != null) {
					classNameToArchivePathMap.put(classNode, "Referenced in '" + archivePath + "'");
				}
			}
		}
	}
}
