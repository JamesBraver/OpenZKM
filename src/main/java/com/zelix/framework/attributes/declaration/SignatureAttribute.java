package com.zelix.framework.attributes.declaration;

import com.zelix.exceptions.ClassParseFailedException;
import com.zelix.exceptions.DebugAssertFailedException;
import com.zelix.exceptions.LookupException;
import com.zelix.exceptions.ProcessingException;
import com.zelix.framework.AbstractClassNode;
import com.zelix.framework.AttributesNode;
import com.zelix.framework.StructureNode;
import com.zelix.framework.constant.ConstantPoolVisitor;
import com.zelix.framework.listener.SimpleReferenceListener;
import com.zelix.framework.signature.TreeEntry;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.writer.UTFStringWriter;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.StringUtils;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.dataset.ByteDataBuffer;
import com.zelix.utils.treeNode.TreeNavigator;
import com.zelix.workspace.ClassHierarchyNode;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

public class SignatureAttribute extends AttributesNode implements SimpleReferenceListener {
	private static char[] TYPE_START_CHARS = new char[]{'L', 'T'};
	private static char[] NEXT_TYPE_START_CHARS = new char[]{'L', 'T', '.'};
	private static char[] TYPE_DELIMITER_CHARS = new char[]{';', '<', ':', '.'};
	private UTFStringWriter signatureUtf8;

	public SignatureAttribute(StructureNode parent, int nameIndex, String attributeName, ByteDataBuffer buf, MultiMap<UTFStringWriter, SimpleReferenceListener> utf8References) throws IOException {
		super(parent, nameIndex, attributeName, buf, utf8References);
		int signatureIndex = buf.readUnsignedShort();
		TypeWriter signatureWriter = this.getConstantPoolEntry(signatureIndex);
		this.setSignatureUtf8(signatureWriter);
		utf8References.put(this.signatureUtf8, this);
	}

	private static void collapseNestedPath(
		TreeNavigator<TreeEntry> tree, TreeEntry entry, TreeNavigator.TreeNode<TreeEntry> treeNode, ArrayList<TreeEntry> path, Iterator<TreeNavigator.TreeNode<TreeEntry>> iterator, String mappedName
	) {
		TreeEntry firstChainEntry = path.get(0);
		TreeEntry lastChainEntry = path.get(path.size() - 1);

		for (int chainIndex = 0; chainIndex < path.size() - 1; chainIndex++) {
			TreeEntry discardedChainEntry = path.get(chainIndex);
			iterator.remove();
			if (chainIndex < path.size() - 2) {
				discardedChainEntry = iterator.next().getData();
			}
		}

		lastChainEntry.setPrefix(firstChainEntry.getPrefix());
		lastChainEntry.setName(mappedName);
		lastChainEntry.setDotSeparated(false);
	}

	public static String remapSignature(String signature, int keepInnerClassInfo, int keepGenericsInformation, HashMap<String, String> classNameMap, StructureNode sourceNode) {
		if (keepGenericsInformation == 1) {
			return signature;
		} else {
			TreeNavigator<TreeEntry> signatureTree = parseSignature(signature, sourceNode);
			if (signatureTree == null) {
				return signature;
			} else {
				Iterator<TreeNavigator.TreeNode<TreeEntry>> nodeIterator = signatureTree.iterator();

				while (nodeIterator.hasNext()) {
					TreeNavigator.TreeNode<TreeEntry> treeNode = nodeIterator.next();
					TreeEntry treeEntry = treeNode.getData();
					if (treeEntry.startsNameChain()) {
						ArrayList<TreeEntry> nameChain = new ArrayList<>();
						nameChain.add(treeEntry);

						for (TreeEntry parentEntry = treeEntry.getParent(); parentEntry != null; parentEntry = parentEntry.getParent()) {
							nameChain.add(parentEntry);
						}

						StringBuffer internalNameBuffer = new StringBuffer();

						for (int i = 0; i < nameChain.size(); i++) {
							if (i > 0) {
								internalNameBuffer.append("$");
							}

							internalNameBuffer.append(nameChain.get(i).getName());
						}

						String internalName = internalNameBuffer.toString();
						String simpleName = AbstractClassNode.getSimpleNameFromInternalName(internalName);
						String mappedInternalName = GlobalUtils.getMappedOrDefault(internalName, classNameMap);
						AbstractClassNode.getPackageName(mappedInternalName);
						String mappedSimpleName = AbstractClassNode.getSimpleNameFromInternalName(mappedInternalName);
						boolean innerClassInfoKept;
						if (keepInnerClassInfo == 0 || keepInnerClassInfo == 2 && simpleName.equals(mappedSimpleName)) {
							innerClassInfoKept = true;
						} else {
							innerClassInfoKept = false;
						}

						if (!internalName.equals(mappedInternalName) || !innerClassInfoKept) {
							StringTokenizer mappedNameTokenizer = new StringTokenizer(mappedSimpleName, "$");
							mappedNameTokenizer.countTokens();
							new StringTokenizer(simpleName, "$").countTokens();
							if (mappedNameTokenizer.countTokens() == 1) {
								collapseNestedPath(signatureTree, treeEntry, treeNode, nameChain, nodeIterator, mappedInternalName);
							} else if (innerClassInfoKept) {
								int chainIndex = 0;
								StringBuffer originalNameBuffer = null;
								StringBuffer mappedNameBuffer = null;

								for (TreeEntry chainEntry : nameChain) {
									String componentName = chainEntry.getName();
									if (chainIndex == 0) {
										String mappedFirstComponentName = GlobalUtils.getMappedOrDefault(componentName, classNameMap);
										if (!componentName.equals(mappedFirstComponentName)) {
											chainEntry.setName(mappedFirstComponentName);
										}

										originalNameBuffer = new StringBuffer(componentName);
										mappedNameBuffer = new StringBuffer(mappedFirstComponentName);
									} else {
										String previousOriginalName = originalNameBuffer.toString();
										String previousMappedName = mappedNameBuffer.toString();
										originalNameBuffer.append("$");
										originalNameBuffer.append(componentName);
										String currentOriginalName = originalNameBuffer.toString();
										String currentMappedName = GlobalUtils.getMappedOrDefault(currentOriginalName, classNameMap);
										String mappedComponentName = currentMappedName.substring(previousMappedName.length() + 1);
										currentOriginalName.substring(previousOriginalName.length() + 1);
										mappedNameBuffer.append("$");
										mappedNameBuffer.append(mappedComponentName);
										chainEntry.setName(mappedComponentName);
									}

									chainIndex++;
								}
							} else {
								collapseNestedPath(signatureTree, treeEntry, treeNode, nameChain, nodeIterator, mappedInternalName);
							}
						}
					} else if (!treeEntry.isDotSeparated()) {
						String entryName = treeEntry.getName();
						if (entryName != null) {
							String mappedEntryName = GlobalUtils.getMappedOrDefault(entryName, classNameMap);
							if (!entryName.equals(mappedEntryName)) {
								treeEntry.setName(mappedEntryName);
							}
						}
					}
				}

				StringBuffer remappedSignatureBuffer = new StringBuffer(signature.length());
				Enumeration<TreeEntry> entryEnumeration = signatureTree.getAllDataAsEnumeration();

				while (entryEnumeration.hasMoreElements()) {
					remappedSignatureBuffer.append(entryEnumeration.nextElement().getFullInternalName());
				}

				return remappedSignatureBuffer.toString();
			}
		}
	}

	public static TreeNavigator<TreeEntry> parseSignature(String signature, StructureNode sourceNode) {
		TreeNavigator<TreeEntry> signatureTree;
		try {
			signatureTree = new TreeNavigator<>(new TreeEntry("", null));
			int typeStartIndex = StringUtils.indexOfAny(signature, 0, TYPE_START_CHARS);
			if (typeStartIndex == -1) {
				return null;
			}

			int tokenStartIndex = 0;

			while (typeStartIndex > -1) {
				char typeStartChar = signature.charAt(typeStartIndex);
				int delimiterIndex = StringUtils.indexOfAny(signature, typeStartIndex + 1, TYPE_DELIMITER_CHARS);
				if (delimiterIndex == -1) {
					throw new ProcessingException("Parse error in " + (sourceNode != null ? sourceNode.getRawSourcePath() : "") + " : '" + signature + "' : " + typeStartIndex);
				}

				if (signature.charAt(delimiterIndex) == ':') {
					typeStartIndex = StringUtils.indexOfAny(signature, delimiterIndex + 1, NEXT_TYPE_START_CHARS);
				} else {
					char previousDelimiterChar = signature.charAt(tokenStartIndex);
					String entryPrefix;
					TreeEntry newEntry;
					if (typeStartChar == 'T') {
						entryPrefix = signature.substring(tokenStartIndex, delimiterIndex);
						newEntry = new TreeEntry(entryPrefix, null);
					} else {
						entryPrefix = signature.substring(tokenStartIndex, typeStartIndex + 1);
						String entryName = signature.substring(typeStartIndex + 1, delimiterIndex);
						newEntry = new TreeEntry(entryPrefix, entryName, typeStartChar == '.');
					}

					int genericsOpenedCount = StringUtils.countOccurrences(entryPrefix, '<');
					int genericsClosedCount = StringUtils.countOccurrences(entryPrefix, '>');
					int parentLevelsToAscend = genericsClosedCount - genericsOpenedCount;

					for (int i = 0; i < parentLevelsToAscend; i++) {
						signatureTree.navigateToParent();
					}

					if (signatureTree.isCurrentNodeRoot() || tokenStartIndex > 0 && previousDelimiterChar == '<' && parentLevelsToAscend != 0) {
						signatureTree.addChildToCurrent(newEntry);
					} else {
						signatureTree.addSiblingToCurrent(newEntry);
					}

					tokenStartIndex = delimiterIndex;
					typeStartIndex = StringUtils.indexOfAny(signature, delimiterIndex, NEXT_TYPE_START_CHARS);
					if (typeStartIndex == -1) {
						entryPrefix = signature.substring(delimiterIndex);
						TreeEntry tailEntry = new TreeEntry(entryPrefix, null);
						signatureTree.addSiblingToCurrent(tailEntry);
					}
				}
			}
		} catch (DebugAssertFailedException debugAssertFailedException) {
			throw debugAssertFailedException;
		} catch (LookupException lookupException) {
			throw new ProcessingException("Tree error in " + sourceNode.getRawSourcePath() + " : '" + signature + "' : '" + lookupException.getMessage() + "'");
		}

		Iterator<TreeNavigator.TreeNode<TreeEntry>> parentNodeIterator = signatureTree.iterator();

		while (parentNodeIterator.hasNext()) {
			TreeNavigator.TreeNode<TreeEntry> parentNode = parentNodeIterator.next();
			TreeEntry previousEntry = null;
			Enumeration<TreeNavigator.TreeNode<TreeEntry>> childEnumeration = parentNode.getChildren();

			while (childEnumeration.hasMoreElements()) {
				TreeNavigator.TreeNode<TreeEntry> childNode = childEnumeration.nextElement();
				TreeEntry childEntry = childNode.getData();
				childEntry.setTreeNode(childNode);
				if (childEntry.isDotSeparated()) {
					previousEntry.setParent(childEntry);
				}

				previousEntry = childEntry;
			}
		}

		return signatureTree;
	}

	@Override
	public void acceptVisitor(ConstantPoolVisitor visitor) {
		visitor.visit(this.attributeNameUtf8, this, this.getParent());
		this.signatureUtf8.accept(visitor, this, this.getParent());
	}

	private TreeNavigator<TreeEntry> parseSignatureTree() {
		return parseSignature(this.signatureUtf8.getValue(), this);
	}

	@Override
	public final void writeAttributeBody(DataOutputStream dataOutputStream) throws IOException {
		super.writeAttributeBody(dataOutputStream);
		dataOutputStream.writeShort(this.signatureUtf8.getTag());
	}

	@Override
	public void updateSimpleNameReference(UTFStringWriter oldReference, UTFStringWriter newReference) {
		if (this.signatureUtf8 == oldReference) {
			this.signatureUtf8 = newReference;
		} else {
			super.updateSimpleNameReference(oldReference, newReference);
		}
	}

	public void setSignatureUtf8(TypeWriter typeWriter) {
		if (!(typeWriter instanceof UTFStringWriter)) {
			throw new ClassParseFailedException(this.getInternalName() + " : Invalid Signature Attribute");
		} else {
			this.signatureUtf8 = (UTFStringWriter)typeWriter;
		}
	}

	public void collectReferencedClasses(Set<ClassNode> signatureClassReferences) {
		TreeNavigator<TreeEntry> signatureTree = this.parseSignatureTree();
		if (signatureTree != null) {
			Enumeration<TreeEntry> entryEnumeration = signatureTree.getAllDataAsEnumeration();

			while (entryEnumeration.hasMoreElements()) {
				String className = null;
				TreeEntry treeEntry = entryEnumeration.nextElement();
				if (!treeEntry.startsNameChain()) {
					if (!treeEntry.isDotSeparated()) {
						className = treeEntry.getName();
					}
				} else {
					StringBuffer internalNameBuffer = new StringBuffer();
					internalNameBuffer.append(treeEntry.getName());

					for (TreeEntry parentEntry = treeEntry.getParent(); parentEntry != null; parentEntry = parentEntry.getParent()) {
						internalNameBuffer.append("$");
						internalNameBuffer.append(parentEntry.getName());
					}

					className = internalNameBuffer.toString();
				}

				if (className != null) {
					ClassNode classNode = ClassHierarchyNode.getWorkspaceClassNode(className);
					if (classNode != null) {
						signatureClassReferences.add(classNode);
					}
				}
			}
		}
	}

	@Override
	public final void writeAttributeBodyRemapped(DataOutputStream dataOutputStream, Map<TypeWriter, TypeWriter> writerMap, ScriptContext scriptContext) throws IOException {
		super.writeAttributeBodyRemapped(dataOutputStream, writerMap, scriptContext);
		UTFStringWriter remappedSignatureWriter = (UTFStringWriter)writerMap.get(this.signatureUtf8);
		if (remappedSignatureWriter != null) {
			dataOutputStream.writeShort(remappedSignatureWriter.getTag());
		} else {
			dataOutputStream.writeShort(this.signatureUtf8.getTag());
		}
	}

	@Override
	public void remapDescriptors(int keepInnerClassInfo, int keepGenericsInformation, HashMap<String, String> packageRenameMap, HashMap<String, String> classNameMap) {
		String signature = this.signatureUtf8.getValue();
		String remappedSignature = remapSignature(signature, keepInnerClassInfo, keepGenericsInformation, classNameMap, this);
		if (!signature.equals(remappedSignature)) {
			this.signatureUtf8.setValue(remappedSignature);
		}
	}
}
