package com.zelix.framework.manifest;

import com.zelix.exceptions.ZKMException;
import com.zelix.framework.signature.MethodSignature;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.wrappers.MethodNode;
import com.zelix.klassmaster.config.Options;
import com.zelix.logging.AbstractLogger;
import com.zelix.utils.DebugUtils;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.IntegerUtils;
import com.zelix.utils.PathRemapper;
import com.zelix.utils.collections.readOnly.ReadOnlyMap;
import com.zelix.utils.dataset.AtomicBool;
import com.zelix.workspace.ClassHierarchyNode;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Map;
import java.util.StringTokenizer;

public class MainManifestAttributes extends AbstractManifestSection {
	private static final MethodSignature PREMAIN_SIGS_1 = new MethodSignature("premain(Ljava/lang/String;Ljava/lang/instrument/Instrumentation;)V");
	private static final MethodSignature PREMAIN_SIGS_2 = new MethodSignature("premain(Ljava/lang/String;)V");
	private static final MethodSignature AGENTMAIN_SIGS_1 = new MethodSignature("agentmain(Ljava/lang/String;Ljava/lang/instrument/Instrumentation;)V");
	private static final MethodSignature AGENTMAIN_SIGS_2 = new MethodSignature("agentmain(Ljava/lang/String;)V");

	public MainManifestAttributes(BufferedReader reader) throws IOException {
		super(reader);
	}

	public static String remapMidletAttribute(String attributeName, String attributeValue, ReadOnlyMap<String, String> packageRemappingMap, ReadOnlyMap<String, String> classRemappingMap) {
		String attributePrefix = attributeName + ": ";
		StringBuilder remappedValueBuffer = new StringBuilder();
		StringTokenizer valueTokenizer = new StringTokenizer(attributeValue, ",", true);
		int fieldIndex = 0;

		while (valueTokenizer.hasMoreTokens()) {
			String fieldToken = valueTokenizer.nextToken();
			if (fieldToken.equals(",")) {
				fieldIndex++;
			} else {
				switch (fieldIndex) {
					case 0:
						break;
					case 1:
						String remappedPath = PathRemapper.remapDirectoryOfPath(fieldToken, packageRemappingMap);
						if (!fieldToken.trim().equals(remappedPath)) {
							remappedValueBuffer.append(" " + remappedPath);
							continue;
						}
						break;
					case 2:
						String remappedClassName = AbstractManifestSection.remapClassName(fieldToken, classRemappingMap);
						if (!fieldToken.trim().equals(remappedClassName)) {
							remappedValueBuffer.append(" " + remappedClassName);
							continue;
						}
						break;
					default:
						DebugUtils.assertTrue(false, new String[]{"'" + attributeName + "' has invalid value '" + attributeValue + "' : " + fieldIndex});
				}
			}

			remappedValueBuffer.append(fieldToken);
		}

		return attributePrefix + remappedValueBuffer.toString();
	}

	public void collectAttributeReferences(String sourcePath, Map<ClassNode, String> classNameToArchivePathMap, Map<MethodNode, String> entryPointSignatures, Map<MethodNode, String> resourceProtectedMethods) {
		String locationSuffix = " in '" + sourcePath + "'";
		Enumeration<String> attributeNameEnumeration = super.attributes.keysEnumeration();

		while (attributeNameEnumeration.hasMoreElements()) {
			String attributeName = attributeNameEnumeration.nextElement();
			if (attributeName.equals("Main-Class")
				|| attributeName.equals("Rsrc-Main-Class")
				|| attributeName.equals("Start-Class:")
				|| attributeName.equals("Premain-Class:")
				|| attributeName.equals("Agent-Class:")
				|| attributeName.equals("Launcher-Agent-Class:")
				|| attributeName.equals("JavaFX-Application-Class")
				|| attributeName.equals("JavaFX-Preloader-Class")
				|| attributeName.equals("JavaFX-Fallback-Class")) {
				ClassNode entryPointClassNode = ClassHierarchyNode.getWorkspaceClassNode(GlobalUtils.binaryToInternalName(this.getAttributeValue(attributeName)));
				if (entryPointClassNode != null) {
					classNameToArchivePathMap.put(entryPointClassNode, "MANIFEST.MF " + attributeName + locationSuffix);
					switch (attributeName) {
						case "Main-Class":
						case "Rsrc-Main-Class":
						case "Start-Class:":
							MethodNode entryPointMethodNode = entryPointClassNode.findMethodNode(MethodSignature.MAIN_METHOD);
							if (entryPointMethodNode != null) {
								entryPointSignatures.put(entryPointMethodNode, "MANIFEST.MF " + attributeName + locationSuffix);
								if (Options.mainClassAsEntryPoint) {
									resourceProtectedMethods.put(entryPointMethodNode, "MANIFEST.MF " + attributeName + locationSuffix);
								}
							}
							break;
						case "Premain-Class:":
						case "Agent-Class:":
						case "Launcher-Agent-Class:":
							this.findAndRegisterAgentMethod(attributeName, locationSuffix, PREMAIN_SIGS_1, entryPointClassNode, classNameToArchivePathMap, entryPointSignatures, resourceProtectedMethods);
							this.findAndRegisterAgentMethod(attributeName, locationSuffix, PREMAIN_SIGS_2, entryPointClassNode, classNameToArchivePathMap, entryPointSignatures, resourceProtectedMethods);
							this.findAndRegisterAgentMethod(attributeName, locationSuffix, AGENTMAIN_SIGS_1, entryPointClassNode, classNameToArchivePathMap, entryPointSignatures, resourceProtectedMethods);
							this.findAndRegisterAgentMethod(attributeName, locationSuffix, AGENTMAIN_SIGS_2, entryPointClassNode, classNameToArchivePathMap, entryPointSignatures, resourceProtectedMethods);
						case "JavaFX-Application-Class":
						case "JavaFX-Preloader-Class":
						case "JavaFX-Fallback-Class":
					}
				}
			} else if (attributeName.startsWith("MIDlet-") && IntegerUtils.isInteger(attributeName.substring(7))) {
				String midletAttributeValue = this.getAttributeValue(attributeName);
				StringTokenizer midletValueTokenizer = new StringTokenizer(midletAttributeValue, ",", true);
				int midletFieldIndex = 0;

				while (midletValueTokenizer.hasMoreTokens()) {
					String midletFieldToken = midletValueTokenizer.nextToken().trim();
					if (midletFieldToken.equals(",")) {
						midletFieldIndex++;
					} else {
						switch (midletFieldIndex) {
							case 0:
							case 1:
								break;
							case 2:
								ClassNode midletClassNode = ClassHierarchyNode.getWorkspaceClassNode(GlobalUtils.binaryToInternalName(midletFieldToken));
								if (midletClassNode != null) {
									classNameToArchivePathMap.put(midletClassNode, "MANIFEST.MF " + attributeName + locationSuffix);
								}
								break;
							default:
								DebugUtils.assertTrue(false, new String[]{"'" + attributeName + "' has invalid value '" + midletAttributeValue + "' : " + midletFieldIndex});
						}
					}
				}
			} else if (!attributeName.equals("Signature-Version")) {
				String attributeValue = this.getAttributeValue(attributeName);
				StringTokenizer valueTokenizer = new StringTokenizer(attributeValue, " ,;", true);

				while (valueTokenizer.hasMoreTokens()) {
					String candidateClassName = valueTokenizer.nextToken();
					if (candidateClassName.length() >= Options.minDottedNameLength && candidateClassName.indexOf(".") > -1 && candidateClassName.indexOf("/") == -1) {
						int dotIndex = candidateClassName.indexOf(".");
						if (dotIndex > 0 && dotIndex < candidateClassName.length() - 1 && AbstractManifestSection.isValidJavaIdentifierPath(candidateClassName, ".")) {
							String internalName = GlobalUtils.binaryToInternalName(candidateClassName);
							if (!internalName.endsWith(";") && internalName.indexOf("[") == -1 && internalName.indexOf(".") == -1 && internalName.indexOf("<") == -1 && internalName.indexOf(">") == -1) {
								ClassNode referencedClassNode = ClassHierarchyNode.getWorkspaceClassNode(internalName);
								if (referencedClassNode != null) {
									classNameToArchivePathMap.put(referencedClassNode, "MANIFEST.MF " + attributeName + locationSuffix);
								}
							}
						}
					}
				}
			}
		}
	}

	@Override
	public void write(
		PrintWriter writer, ReadOnlyMap<String, String> packageRemappingMap, ReadOnlyMap<String, String> classRemappingMap, PathRemapper pathRemapper, String sourcePath, boolean warnOnCollapse, AbstractLogger logger
	) {
		Enumeration<String> attributeNameEnumeration = super.attributes.keysEnumeration();

		while (attributeNameEnumeration.hasMoreElements()) {
			String attributeName = attributeNameEnumeration.nextElement();
			if (attributeName.equals("Main-Class")
				|| attributeName.equals("Rsrc-Main-Class")
				|| attributeName.equals("Start-Class:")
				|| attributeName.equals("JavaFX-Application-Class")
				|| attributeName.equals("JavaFX-Preloader-Class")
				|| attributeName.equals("JavaFX-Fallback-Class")
				|| attributeName.equals("Premain-Class:")
				|| attributeName.equals("Agent-Class:")
				|| attributeName.equals("Launcher-Agent-Class:")) {
				String attributeValue = this.getAttributeValue(attributeName);
				writer.println(this.foldLine(attributeName + ": " + AbstractManifestSection.remapClassName(attributeValue, classRemappingMap)));
			} else if (attributeName.equals("Manifest-Version") || attributeName.equals("Sealed")) {
				String rawAttributeValue = this.getRawAttributeValue(attributeName);
				writer.println(attributeName + ": " + rawAttributeValue);
			} else if (attributeName.equals("MIDlet-Icon")) {
				writer.println(this.foldLine(attributeName + ": " + PathRemapper.remapDirectoryOfPath(this.getAttributeValue(attributeName), packageRemappingMap)));
			} else if (attributeName.startsWith("MIDlet-") && IntegerUtils.isInteger(attributeName.substring(7))) {
				writer.println(this.getRemappedMidletAttribute(attributeName, packageRemappingMap, classRemappingMap));
			} else if (!attributeName.equals("Signature-Version")) {
				if (!attributeName.equals("Bundle-SymbolicName")
					&& !attributeName.equals("Require-Bundle")
					&& !attributeName.equals("Fragment-Host")
					&& !attributeName.equals("Class-Path")
					&& !attributeName.equals("Rsrc-Class-Path")
					&& !attributeName.equals("JavaFX-Class-Path")) {
					AtomicBool valueCollapsed = new AtomicBool();
					String remappedValue = this.getRemappedAttribute(attributeName, packageRemappingMap, classRemappingMap, valueCollapsed, sourcePath, warnOnCollapse, logger);
					if (!valueCollapsed.get() || Options.ZKM_WRITE_EMPTY_MANIFEST_VALUES) {
						writer.println(remappedValue);
					}
				} else {
					String foldedLine = this.foldLine(attributeName + ": " + this.getAttributeValue(attributeName));
					writer.println(foldedLine);
				}
			}
		}

		writer.println();
	}

	@Override
	public String normalizeAttributeName(String attributeName) {
		if (attributeName.equalsIgnoreCase("Manifest-Version")) {
			return "Manifest-Version";
		} else if (attributeName.equalsIgnoreCase("Class-Path")) {
			return "Class-Path";
		} else if (attributeName.equalsIgnoreCase("Rsrc-Class-Path")) {
			return "Rsrc-Class-Path";
		} else if (attributeName.equalsIgnoreCase("Main-Class")) {
			return "Main-Class";
		} else if (attributeName.equalsIgnoreCase("Rsrc-Main-Class")) {
			return "Rsrc-Main-Class";
		} else if (attributeName.equalsIgnoreCase("Start-Class:")) {
			return "Start-Class:";
		} else if (attributeName.equalsIgnoreCase("Premain-Class:")) {
			return "Premain-Class:";
		} else if (attributeName.equalsIgnoreCase("Agent-Class:")) {
			return "Agent-Class:";
		} else if (attributeName.equalsIgnoreCase("Launcher-Agent-Class:")) {
			return "Launcher-Agent-Class:";
		} else if (attributeName.equalsIgnoreCase("Sealed")) {
			return "Sealed";
		} else if (attributeName.equalsIgnoreCase("Signature-Version")) {
			return "Signature-Version";
		} else if (attributeName.equalsIgnoreCase("MicroEdition-Profile")) {
			return "MicroEdition-Profile";
		} else if (attributeName.equalsIgnoreCase("MicroEdition-Configuration")) {
			return "MicroEdition-Configuration";
		} else if (attributeName.equalsIgnoreCase("JavaFX-Application-Class")) {
			return "JavaFX-Application-Class";
		} else if (attributeName.equalsIgnoreCase("JavaFX-Preloader-Class")) {
			return "JavaFX-Preloader-Class";
		} else if (attributeName.equalsIgnoreCase("JavaFX-Fallback-Class")) {
			return "JavaFX-Fallback-Class";
		} else {
			return attributeName.equalsIgnoreCase("JavaFX-Class-Path") ? "JavaFX-Class-Path" : attributeName;
		}
	}

	public boolean isMicroEdition() {
		return this.getAttributeValue("MicroEdition-Configuration") != null;
	}

	public boolean isCldc10() {
		String configurationValue = this.getAttributeValue("MicroEdition-Configuration");
		if (configurationValue == null) {
			return false;
		} else {
			int dashIndex = configurationValue.indexOf("-");
			if (dashIndex > -1 && dashIndex < configurationValue.length() - 1) {
				String versionText = configurationValue.substring(dashIndex + 1);
				return versionText.startsWith("1.0");
			} else {
				throw new ZKMException("Invalid MicroEdition-Configuration '" + configurationValue + "' in manifest");
			}
		}
	}

	public String getRemappedMidletAttribute(String attributeName, ReadOnlyMap<String, String> packageRemappingMap, ReadOnlyMap<String, String> classRemappingMap) {
		String attributeValue = this.getAttributeValue(attributeName);
		return this.foldLine(remapMidletAttribute(attributeName, attributeValue, packageRemappingMap, classRemappingMap));
	}

	private void findAndRegisterAgentMethod(
		String attributeName, String locationSuffix, MethodSignature methodSignature, ClassNode classNode, Map<ClassNode, String> classNameToArchivePathMap, Map<MethodNode, String> entryPointSignatures, Map<MethodNode, String> resourceProtectedMethods
	) {
		MethodNode agentMethodNode = classNode.findMethodNode(methodSignature);
		if (agentMethodNode != null) {
			entryPointSignatures.put(agentMethodNode, "MANIFEST.MF " + attributeName + locationSuffix);
			resourceProtectedMethods.put(agentMethodNode, "MANIFEST.MF " + attributeName + locationSuffix);
		}
	}
}
