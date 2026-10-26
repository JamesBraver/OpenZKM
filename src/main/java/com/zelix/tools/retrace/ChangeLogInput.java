package com.zelix.tools.retrace;

import com.zelix.exceptions.ChangeLogMethodException;
import com.zelix.klassmaster.changelogs.AbstractChangeLogContext;
import com.zelix.klassmaster.changelogs.FieldKey;
import com.zelix.klassmaster.changelogs.MethodKey;
import com.zelix.klassmaster.changelogs.MethodMappingInfo;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.collections.DualKeyMap;
import com.zelix.utils.collections.HierarchicalMultiMap;
import com.zelix.utils.collections.KeyValuePair;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.collections.ThreeLevelMultiMap;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map.Entry;
import java.util.Map;

public class ChangeLogInput extends AbstractChangeLogContext {
	private boolean methodKeysHaveReturnType = true;
	private String logFileName;
	private ThreeLevelMultiMap<String, String, String, KeyValuePair<String, String>> oldMethodCandidatesByParams;
	private int classMappingCount;
	private DualKeyMap<String, Integer, Integer> lineNumberMappings;
	private Map<String, String> newToOldClassNameMap;
	private Map<String, String> oldClassNameToSourceName;
	private HierarchicalMultiMap<String, String, KeyValuePair<String, String>> oldMethodCandidates;
	private int packageMappingCount;

	public ChangeLogInput(String logFileName) {
		this.logFileName = logFileName;
	}

	public void remapMethodParameterLists() {
		if (this.hasParameterChangeData()) {
			ThreeLevelMultiMap<String, String, String, KeyValuePair<String, String>> remappedCandidatesByParams = new ThreeLevelMultiMap<>(this.oldMethodCandidatesByParams.size());

			for (Entry<String, HierarchicalMultiMap<String, String, KeyValuePair<String, String>>> classEntry : this.oldMethodCandidatesByParams.entrySet()) {
				String oldClassName = classEntry.getKey();

				for (Entry<String, MultiMap<String, KeyValuePair<String, String>>> methodEntry : classEntry.getValue().entrySet()) {
					String newMethodName = methodEntry.getKey();

					for (Entry<String, List<KeyValuePair<String, String>>> parameterEntry : methodEntry.getValue().entrySet()) {
						String parameterList = parameterEntry.getKey();
						String remappedParameterList = this.remapParameterList(parameterList, this.newToOldClassNameMap);

						for (KeyValuePair<String, String> oldMethodCandidate : parameterEntry.getValue()) {
							remappedCandidatesByParams.put(oldClassName, newMethodName, remappedParameterList, oldMethodCandidate);
						}
					}
				}
			}

			this.oldMethodCandidatesByParams = remappedCandidatesByParams;
		}
	}

	@Override
	public void addClassMapping(String newClassName, String oldClassName) {
		if (this.newToOldClassNameMap == null) {
			this.newToOldClassNameMap = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(this.classMappingCount));
		}

		this.newToOldClassNameMap.put(newClassName, oldClassName);
	}

	@Override
	public boolean isLineNumberChangeSupported() {
		return true;
	}

	public String getOldClassName(String newClassName) {
		return this.newToOldClassNameMap.get(newClassName);
	}

	@Override
	public void addInnerClassMappings(String className, Map<Integer, Integer> lineNumberMappings) {
		if (this.lineNumberMappings == null) {
			this.lineNumberMappings = new DualKeyMap<>(this.classMappingCount);
		}

		this.lineNumberMappings.putGroup(className, lineNumberMappings);
	}

	public boolean hasClassMapping(String newClassName) {
		return this.newToOldClassNameMap.containsKey(newClassName);
	}

	@Override
	public void addTraceBackClass(String className, List<String> data) {
	}

	@Override
	public void addMethodMappings(String className, MultiMap<MethodKey, MethodMappingInfo> methodMappings) {
		if (this.oldMethodCandidates == null) {
			this.oldMethodCandidates = new HierarchicalMultiMap<>(this.classMappingCount);
			this.oldMethodCandidatesByParams = new ThreeLevelMultiMap<>(this.classMappingCount);
		}

		Enumeration<MethodKey> methodKeyEnumeration = methodMappings.keys();

		while (methodKeyEnumeration.hasMoreElements()) {
			MethodKey methodKey = methodKeyEnumeration.nextElement();
			String oldMethodName = methodKey.getName();
			String oldReturnType = methodKey.getDescriptor();
			String oldParameterList = methodKey.getParameterDescriptor();

			for (MethodMappingInfo mappingInfo : methodMappings.get(methodKey)) {
				String newMethodName = mappingInfo.getNewName();
				if (mappingInfo.hasParameterChangeData()) {
					this.setParameterChangeData(true);
					this.oldMethodCandidatesByParams.put(className, newMethodName, mappingInfo.getEffectiveParameterList(), new KeyValuePair<>(oldMethodName, oldReturnType));
				} else if (mappingInfo.isParameterObfuscated()) {
					this.oldMethodCandidatesByParams.put(className, newMethodName, mappingInfo.getEffectiveParameterList(), new KeyValuePair<>(oldMethodName, oldReturnType));
				} else {
					this.oldMethodCandidatesByParams.put(className, newMethodName, oldParameterList, new KeyValuePair<>(oldMethodName, oldReturnType));
				}

				this.oldMethodCandidates.put(className, newMethodName, new KeyValuePair<>(oldMethodName, oldParameterList));
			}

			this.methodKeysHaveReturnType = methodKey.getDescriptor() != null;
		}
	}

	public boolean isOldClassNameMapped(String oldClassName) {
		return this.newToOldClassNameMap.containsValue(oldClassName);
	}

	@Override
	public void addPackageMapping(String newPackageName, String oldPackageName) {
	}

	public String getOldTypeName(String typeName) {
		return AbstractChangeLogContext.remapDescriptor(typeName, this.newToOldClassNameMap);
	}

	public boolean hasLineNumberMappings(String oldClassName) {
		return this.lineNumberMappings != null && this.lineNumberMappings.containsGroup(oldClassName);
	}

	@Override
	public void setModuleObfuscateRefsClass(String className, String moduleName) {
	}

	@Override
	public void addFieldMappings(String className, MultiMap<FieldKey, String> fieldMappings) {
	}

	public List<KeyValuePair<String, String>> getOldMethodCandidates(String oldClassName, String newMethodName) {
		return this.oldMethodCandidates.getValues(oldClassName, newMethodName);
	}

	public String[] getOldMethodNames(String oldClassName, String newMethodName, String oldReturnType, String oldParameterList) {
		String[] oldMethodNames = null;
		if (this.oldMethodCandidatesByParams != null) {
			List<KeyValuePair<String, String>> methodCandidates = this.oldMethodCandidatesByParams.getValues(oldClassName, newMethodName, oldParameterList);
			if (methodCandidates != null) {
				if (oldReturnType == null) {
					oldMethodNames = new String[methodCandidates.size()];

					for (int i = 0; i < oldMethodNames.length; i++) {
						oldMethodNames[i] = methodCandidates.get(i).getKey();
					}
				} else {
					ArrayList<String> matchingOldMethodNames = new ArrayList<>();

					for (int candidateIndex = 0; candidateIndex < methodCandidates.size(); candidateIndex++) {
						KeyValuePair<String, String> candidate = methodCandidates.get(candidateIndex);
						String candidateReturnType = candidate.getValue();
						if (candidateReturnType == null || candidateReturnType.equals(oldReturnType)) {
							matchingOldMethodNames.add(candidate.getKey());
						}
					}

					if (matchingOldMethodNames.size() > 0) {
						oldMethodNames = new String[matchingOldMethodNames.size()];
						oldMethodNames = matchingOldMethodNames.toArray(oldMethodNames);
					}
				}
			}
		}

		return oldMethodNames;
	}

	@Override
	public void addMemberClass(String className, List<String> data) {
	}

	@Override
	public void incrementPackageMappingCount() {
		this.packageMappingCount++;
	}

	@Override
	public void setModuleAutoReflectionClass(String className, String moduleName) {
	}

	@Override
	public boolean isFieldNameChangeSupported() {
		return false;
	}

	public ChangeLogInput mergeChangeLog(ChangeLogInput other, PrintWriter errorWriter) {
		if (other.newToOldClassNameMap != null) {
			ArrayList<String> newClassNames = new ArrayList<>(other.newToOldClassNameMap.keySet());
			Collections.sort(newClassNames);

			for (String newClassName : newClassNames) {
				String otherOldClassName = other.newToOldClassNameMap.get(newClassName);
				if (this.newToOldClassNameMap != null && this.newToOldClassNameMap.containsKey(newClassName)) {
					String thisOldClassName = this.newToOldClassNameMap.get(newClassName);
					if (!thisOldClassName.equals(otherOldClassName)) {
						errorWriter.println(
							"ERROR: Class name '" + newClassName + "' mapping clash while merging with '" + other.logFileName + "'. '" + thisOldClassName + "' and '" + otherOldClassName + "' both map to '" + newClassName + "'."
						);
					}
				} else {
					this.addClassMapping(newClassName, otherOldClassName);
				}
			}
		}

		if (other.oldClassNameToSourceName != null) {
			for (Entry<String, String> sourceNameEntry : other.oldClassNameToSourceName.entrySet()) {
				String oldClassName = sourceNameEntry.getKey();
				String otherSourceName = sourceNameEntry.getValue();
				if (this.oldClassNameToSourceName != null && this.oldClassNameToSourceName.containsKey(oldClassName)) {
					String thisSourceName = this.oldClassNameToSourceName.get(oldClassName);
					if (!thisSourceName.equals(otherSourceName)) {
						errorWriter.println(
							"ERROR: Class name '"
								+ oldClassName
								+ "' source name mapping clash while merging with '"
								+ other.logFileName
								+ "'. '"
								+ oldClassName
								+ "' maps to '"
								+ otherSourceName
								+ "' and '"
								+ otherSourceName
								+ "'."
						);
					}
				} else {
					this.setClassSourceName(oldClassName, otherSourceName);
				}
			}
		}

		if (other.oldMethodCandidatesByParams != null) {
			if (this.oldMethodCandidatesByParams != null && this.methodKeysHaveReturnType != other.methodKeysHaveReturnType) {
				throw new ChangeLogMethodException(
					"Change log methods "
						+ (this.methodKeysHaveReturnType ? "have return types" : "have no return types")
						+ " which is inconsistent with change log \""
						+ other.logFileName
						+ "\" whose methods "
						+ (other.methodKeysHaveReturnType ? "have return types" : "have no return types")
						+ ". All method entries in change logs being merged must have return types or no method entries can have return types."
				);
			}

			if (this.oldMethodCandidatesByParams != null && this.hasParameterChangeData() != other.hasParameterChangeData()) {
				throw new ChangeLogMethodException(
					"Change log methods "
						+ (this.hasParameterChangeData() ? "have new method parameter data" : "have no new method parameter data")
						+ " which is inconsistent with change log \""
						+ other.logFileName
						+ "\" whose methods "
						+ (other.hasParameterChangeData() ? "have new method parameter data" : "have no new method parameter data")
						+ ". All method entries in change logs being merged must have new method parameter data or no method entries can have new method parameter data."
				);
			}

			Enumeration<String> methodClassNames = other.oldMethodCandidatesByParams.keys();

			while (methodClassNames.hasMoreElements()) {
				String methodClassName = methodClassNames.nextElement();
				HierarchicalMultiMap<String, String, KeyValuePair<String, String>> otherCandidatesByMethodName = other.oldMethodCandidatesByParams.get(methodClassName);
				if (this.oldMethodCandidatesByParams != null && this.oldMethodCandidatesByParams.containsKey(methodClassName)) {
					HierarchicalMultiMap<String, String, KeyValuePair<String, String>> thisCandidatesByMethodName = this.oldMethodCandidatesByParams.get(methodClassName);
					Enumeration<String> otherMethodNames = otherCandidatesByMethodName.keys();

					while (otherMethodNames.hasMoreElements()) {
						String newMethodName = otherMethodNames.nextElement();
						MultiMap<String, KeyValuePair<String, String>> otherCandidatesByParams = otherCandidatesByMethodName.get(newMethodName);
						if (thisCandidatesByMethodName.containsKey(newMethodName)) {
							MultiMap<String, KeyValuePair<String, String>> thisCandidatesByParams = thisCandidatesByMethodName.get(newMethodName);
							Enumeration<String> otherParameterLists = otherCandidatesByParams.keys();

							while (otherParameterLists.hasMoreElements()) {
								String parameterList = otherParameterLists.nextElement();
								List<KeyValuePair<String, String>> otherCandidates = otherCandidatesByParams.get(parameterList);
								if (thisCandidatesByParams.containsKey(parameterList)) {
									List<KeyValuePair<String, String>> thisCandidates = thisCandidatesByParams.get(parameterList);

									for (KeyValuePair<String, String> otherCandidate : otherCandidates) {
										String otherOldMethodName = otherCandidate.getKey();
										String otherReturnType = otherCandidate.getValue();

										for (KeyValuePair<String, String> thisCandidate : thisCandidates) {
											String thisOldMethodName = thisCandidate.getKey();
											String thisReturnType = thisCandidate.getValue();
											if (otherReturnType != null) {
												if (otherReturnType.equals(thisReturnType) && !otherOldMethodName.equals(thisOldMethodName)) {
													errorWriter.println(
														"ERROR: Method name clash name while merging with '"
															+ other.logFileName
															+ "'. Both '"
															+ thisOldMethodName
															+ "' and '"
															+ otherOldMethodName
															+ "' map to new method name '"
															+ otherReturnType
															+ " "
															+ newMethodName
															+ "("
															+ parameterList
															+ ")' in class '"
															+ methodClassName
															+ "'."
													);
												}
											} else if (!otherOldMethodName.equals(thisOldMethodName)) {
												errorWriter.println(
													"ERROR: Method name clash name while merging with '"
														+ other.logFileName
														+ "'. Both '"
														+ thisOldMethodName
														+ "' and '"
														+ otherOldMethodName
														+ "' map to new method name '"
														+ newMethodName
														+ "("
														+ parameterList
														+ ")' in class '"
														+ methodClassName
														+ "'."
												);
											}
										}
									}
								} else {
									errorWriter.println(
										"WARNING: Different versions of class '"
											+ methodClassName
											+ "' while merging with '"
											+ other.logFileName
											+ "'.  Class '"
											+ methodClassName
											+ "' in '"
											+ this.logFileName
											+ "' does not have a new method name '"
											+ newMethodName
											+ "("
											+ parameterList
											+ ")'."
									);
								}
							}
						} else {
							errorWriter.println(
								"WARNING: Different versions of class '"
									+ methodClassName
									+ "' while merging with '"
									+ other.logFileName
									+ "'.  Class '"
									+ methodClassName
									+ "' in '"
									+ this.logFileName
									+ "' does not have a new method name '"
									+ newMethodName
									+ "'."
							);
						}
					}
				} else {
					MultiMap<MethodKey, MethodMappingInfo> methodMappings = new MultiMap<>();
					Enumeration<String> rebuildMethodNames = otherCandidatesByMethodName.keys();

					while (rebuildMethodNames.hasMoreElements()) {
						String rebuildMethodName = rebuildMethodNames.nextElement();
						MultiMap<String, KeyValuePair<String, String>> rebuildCandidatesByParams = otherCandidatesByMethodName.get(rebuildMethodName);
						Enumeration<String> rebuildParameterLists = rebuildCandidatesByParams.keys();

						while (rebuildParameterLists.hasMoreElements()) {
							String rebuildParameterList = rebuildParameterLists.nextElement();

							for (KeyValuePair<String, String> rebuildCandidate : rebuildCandidatesByParams.get(rebuildParameterList)) {
								MethodKey methodKey = new MethodKey(rebuildCandidate.getKey(), rebuildParameterList, rebuildCandidate.getValue(), 0);
								MethodMappingInfo mappingInfo = new MethodMappingInfo(rebuildMethodName);
								methodMappings.put(methodKey, mappingInfo);
							}
						}
					}

					this.addMethodMappings(methodClassName, methodMappings);
				}
			}
		}

		if (other.lineNumberMappings != null) {
			Enumeration<String> lineNumberClassNames = other.lineNumberMappings.getGroupKeys();

			while (lineNumberClassNames.hasMoreElements()) {
				String lineNumberClassName = lineNumberClassNames.nextElement();
				Map<Integer, Integer> otherLineNumbers = other.lineNumberMappings.getGroup(lineNumberClassName);
				if (this.lineNumberMappings != null && this.lineNumberMappings.containsGroup(lineNumberClassName)) {
					Map<Integer, Integer> thisLineNumbers = this.lineNumberMappings.getGroup(lineNumberClassName);

					for (Entry<Integer, Integer> lineNumberEntry : otherLineNumbers.entrySet()) {
						Integer newLineNumber = lineNumberEntry.getKey();
						Integer otherOldLineNumber = lineNumberEntry.getValue();
						if (thisLineNumbers.containsKey(newLineNumber)) {
							if (!thisLineNumbers.get(newLineNumber).equals(otherOldLineNumber)) {
								errorWriter.println(
									"WARNING: Different line number mapping in class '"
										+ lineNumberClassName
										+ "' while merging with '"
										+ other.logFileName
										+ "'. Line number "
										+ newLineNumber
										+ " is mapped to by "
										+ otherOldLineNumber
										+ " and "
										+ thisLineNumbers.get(newLineNumber)
								);
							}
						} else {
							errorWriter.println(
								"WARNING: Different line number mapping in class '"
									+ lineNumberClassName
									+ "' while merging with '"
									+ other.logFileName
									+ "'. Line number "
									+ newLineNumber
									+ " does not appear in '"
									+ this.logFileName
									+ "'."
							);
						}
					}
				} else {
					this.addInnerClassMappings(lineNumberClassName, this.lineNumberMappings.copyMap(otherLineNumbers));
				}
			}
		}

		return this;
	}

	@Override
	public void setParameterChangeClasses(List<String> classNames) {
	}

	@Override
	public void setAutoReflectionClass(String className) {
	}

	public String getSourceName(String oldClassName) {
		return this.oldClassNameToSourceName != null ? this.oldClassNameToSourceName.get(oldClassName) : null;
	}

	@Override
	public void setModuleParameterChangeClasses(List<String> classNames, String moduleName) {
	}

	@Override
	public void incrementClassMappingCount() {
		this.classMappingCount++;
	}

	public String getOldMethodName(String newClassName, String newMethodName, String[] newArgumentTypes, String newReturnType) {
		if (newClassName == null) {
			throw new IllegalArgumentException("New class name cannot be null");
		} else if (newMethodName == null) {
			throw new IllegalArgumentException("New method name cannot be null");
		} else if (newArgumentTypes == null) {
			throw new IllegalArgumentException("New argument types array cannot be null");
		} else if (newReturnType == null) {
			throw new IllegalArgumentException("New return type cannot be null");
		} else {
			String oldClassName = GlobalUtils.getMappedOrDefault(newClassName, this.newToOldClassNameMap);
			String newParameterList = this.formatParameterList(newArgumentTypes);
			String oldParameterList = this.remapParameterList(newParameterList, this.newToOldClassNameMap);
			String oldReturnType = AbstractChangeLogContext.remapDescriptor(newReturnType, this.newToOldClassNameMap);
			List<KeyValuePair<String, String>> methodCandidates = this.oldMethodCandidatesByParams.getValues(oldClassName, newMethodName, oldParameterList);
			if (methodCandidates.size() == 1) {
				KeyValuePair<String, String> singleCandidate = methodCandidates.get(0);
				if (singleCandidate.getValue() == null || oldReturnType.equals(singleCandidate.getValue())) {
					return singleCandidate.getKey();
				}
			} else {
				for (KeyValuePair<String, String> candidate : methodCandidates) {
					if (oldReturnType.equals(candidate.getValue())) {
						return candidate.getKey();
					}
				}
			}

			return newMethodName;
		}
	}

	@Override
	public void addForwardClass(String packageName, String className, List<String> data) {
	}

	public Integer getOldLineNumber(String oldClassName, int newLineNumber) {
		return this.lineNumberMappings.get(oldClassName, this.getCachedInteger(newLineNumber));
	}

	@Override
	public void setObfuscateReferencesClass(String className) {
	}

	@Override
	public void setClassSourceName(String className, String sourceName) {
		if (this.oldClassNameToSourceName == null) {
			this.oldClassNameToSourceName = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(this.classMappingCount));
		}

		this.oldClassNameToSourceName.put(className, sourceName);
	}
}
