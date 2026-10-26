package com.zelix.framework.manifest;

import com.zelix.framework.wrappers.ClassNode;
import com.zelix.klassmaster.config.Options;
import com.zelix.logging.AbstractLogger;
import com.zelix.utils.PathRemapper;
import com.zelix.utils.collections.readOnly.ReadOnlyMap;
import com.zelix.utils.dataset.AtomicBool;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Map;

public class IndividualManifestSection extends AbstractManifestSection {
	public IndividualManifestSection(BufferedReader reader) throws IOException {
		super(reader);
	}

	@Override
	public void write(
		PrintWriter writer, ReadOnlyMap<String, String> packageRemappingMap, ReadOnlyMap<String, String> classRemappingMap, PathRemapper pathRemapper, String sourcePath, boolean warnOnCollapse, AbstractLogger logger
	) {
		Enumeration<String> attributeNameEnumeration = super.attributes.keysEnumeration();

		while (attributeNameEnumeration.hasMoreElements()) {
			String attributeName = attributeNameEnumeration.nextElement();
			if (attributeName.equals("Name")) {
				String nameValue = this.getName();
				if (nameValue != null && nameValue.length() <= 0) {
					writer.println(this.foldLine("Name: "));
				} else {
					writer.println(this.foldLine("Name: " + AbstractManifestSection.remapResourceName(nameValue, classRemappingMap, pathRemapper)));
				}
			} else if (attributeName.equals("Java-Bean")) {
				String rawJavaBeanValue = this.getRawAttributeValue("Java-Bean");
				writer.println("Java-Bean: " + rawJavaBeanValue);
			} else if (attributeName.equals("Sealed")) {
				String rawSealedValue = this.getRawAttributeValue("Sealed");
				writer.println("Sealed: " + rawSealedValue);
			} else if (!attributeName.equals("Magic") && attributeName.indexOf("Digest") == -1) {
				AtomicBool valueCollapsed = new AtomicBool();
				String remappedValue = this.getRemappedAttribute(attributeName, packageRemappingMap, classRemappingMap, valueCollapsed, sourcePath, warnOnCollapse, logger);
				if (!valueCollapsed.get() || Options.ZKM_WRITE_EMPTY_MANIFEST_VALUES) {
					writer.println(remappedValue);
				}
			}
		}

		writer.println();
	}

	@Override
	public String normalizeAttributeName(String attributeName) {
		if (attributeName.equalsIgnoreCase("Name")) {
			return "Name";
		} else if (attributeName.equalsIgnoreCase("Java-Bean")) {
			return "Java-Bean";
		} else {
			return attributeName.equalsIgnoreCase("Sealed") ? "Sealed" : attributeName;
		}
	}

	public String getName() {
		return this.getAttributeValue("Name");
	}

	public void collectAttributeReferences(String sourcePath, Map<ClassNode, String> classNameToArchivePathMap) {
	}
}
