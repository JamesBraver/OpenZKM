package com.zelix.workspace.resources;

import com.zelix.framework.wrappers.ClassNode;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.StringUtils;
import com.zelix.workspace.ClassHierarchyNode;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;
import java.util.StringTokenizer;

public class PropertyFileParser {
	public PropertyFileParser(BufferedReader reader, String archivePath, Map<ClassNode, String> classNameToArchivePathMap) throws IOException {
		String line;
		while ((line = reader.readLine()) != null) {
			int contentStart = StringUtils.skipWhitespace(line, 0);
			String trimmedLine;
			if (contentStart > 0) {
				trimmedLine = line.substring(contentStart);
			} else {
				trimmedLine = line;
			}

			if (!trimmedLine.isEmpty() && trimmedLine.charAt(0) != '#' && trimmedLine.charAt(0) != '!') {
				boolean separatorSeen = false;
				StringTokenizer tokenizer = new StringTokenizer(trimmedLine, " \t=:\\!#", true);

				while (tokenizer.hasMoreTokens()) {
					String token = tokenizer.nextToken();
					if (token.length() == 1) {
						if (token.equals(":") || token.equals("=")) {
							separatorSeen = true;
						}
					} else if (separatorSeen) {
						String internalName = GlobalUtils.binaryToInternalName(token);
						if (!internalName.endsWith(";") && internalName.indexOf("[") == -1 && internalName.indexOf(".") == -1 && internalName.indexOf("<") == -1 && internalName.indexOf(">") == -1) {
							ClassNode classNode = ClassHierarchyNode.getWorkspaceClassNode(internalName);
							if (classNode != null) {
								classNameToArchivePathMap.put(classNode, "Referenced in '" + archivePath + "'");
							}
						}
					}
				}
			}
		}
	}
}
