package com.zelix.utils;

import com.zelix.utils.collections.readOnly.ReadOnlyMap;
import com.zelix.utils.comparators.CaseInsensitiveComparator;
import com.zelix.utils.dataset.AtomicInt;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

public class PathRemapper {
	private Map<String, String> remappedPathCache = GlobalUtils.newHashMap();
	private ReadOnlyMap<String, String> remappingMap;
	private Set<String> packagePrefixSet;
	private String[] sortedPackagePrefixes;

	public PathRemapper(ReadOnlyMap<String, String> remappingMap, Set<String> packagePrefixSet) {
		this.remappingMap = remappingMap;
		this.packagePrefixSet = GlobalUtils.newHashSet(packagePrefixSet);
		this.sortedPackagePrefixes = new String[packagePrefixSet.size()];
		int prefixIndex = 0;

		for (String packagePrefix : packagePrefixSet) {
			this.sortedPackagePrefixes[prefixIndex++] = packagePrefix;
		}

		Arrays.sort(this.sortedPackagePrefixes, CaseInsensitiveComparator.getInstance());
	}

	public static String computeMappedRelativePath(String referencingPath, String referencedName, ReadOnlyMap<String, String> remappingMap) {
		referencingPath = referencingPath.trim();
		referencedName = referencedName.trim();
		if (referencingPath.length() > 0 && referencingPath.charAt(0) == '/') {
			referencingPath = referencingPath.substring(1);
		}

		int lastSeparator = referencingPath.lastIndexOf(47);
		if (lastSeparator <= 0) {
			return referencedName;
		} else {
			String matchingPath = null;
			StringBuilder candidatePathBuffer = new StringBuilder(referencingPath.substring(0, lastSeparator));

			String mappedPath;
			while (true) {
				String candidatePath = candidatePathBuffer.toString();
				mappedPath = remappingMap.get(candidatePath);
				if (mappedPath != null) {
					matchingPath = candidatePath;
					break;
				}

				int truncateIndex = candidatePath.lastIndexOf("/");
				if (truncateIndex <= -1) {
					break;
				}

				candidatePathBuffer.setLength(truncateIndex);
			}

			if (matchingPath != null && !matchingPath.equals(mappedPath)) {
				String remainingPath = referencingPath.substring(matchingPath.length() + 1);
				if (remainingPath.length() >= referencedName.length()) {
					return referencedName;
				} else {
					String mappedReferencedDirectory;
					if (!referencedName.equals(referencingPath)) {
						String referencedDirectory = referencingPath.substring(0, referencingPath.indexOf(referencedName) - 1);
						mappedReferencedDirectory = remappingMap.get(referencedDirectory);
					} else {
						mappedReferencedDirectory = "";
					}

					String relativePath;
					if (mappedReferencedDirectory.equals(mappedPath)) {
						relativePath = remainingPath;
					} else if (mappedPath.startsWith(mappedReferencedDirectory)) {
						StringBuilder relativePathBuffer = new StringBuilder();
						int prefixLength = mappedReferencedDirectory.length();
						if (prefixLength == 0) {
							relativePathBuffer.append(mappedPath);
						} else {
							relativePathBuffer.append(mappedPath.substring(prefixLength + 1));
						}

						relativePathBuffer.append('/');
						relativePathBuffer.append(remainingPath);
						relativePath = relativePathBuffer.toString();
					} else {
						relativePath = "../" + remainingPath;
					}

					return relativePath;
				}
			} else {
				return referencedName;
			}
		}
	}

	public static String remapRelativePath(String referencingPath, String referencedPath, boolean isAbsolutePath, ReadOnlyMap<String, String> remappingMap) {
		boolean absolutePrefixRemoved = false;
		if (referencedPath.length() == 0) {
			return referencedPath;
		} else {
			if (isAbsolutePath && referencedPath.charAt(0) == '/') {
				absolutePrefixRemoved = true;
				if (referencedPath.length() == 1) {
					return referencedPath;
				}

				referencedPath = referencedPath.substring(1);
			}

			if (referencingPath.indexOf(92) > -1) {
				referencingPath = referencingPath.replace('\\', '/');
			}

			StringTokenizer referencingPathTokenizer = new StringTokenizer(referencingPath, "/");
			ArrayList<String> referencingSegments = new ArrayList<>(referencingPathTokenizer.countTokens());

			while (referencingPathTokenizer.hasMoreTokens()) {
				referencingSegments.add(referencingPathTokenizer.nextToken());
			}

			String matchingSuffixPath = "";
			StringBuilder candidatePathBuffer = new StringBuilder();

			for (int referencingSegmentIndex = referencingSegments.size() - 1; referencingSegmentIndex >= 0; referencingSegmentIndex--) {
				if (candidatePathBuffer.length() > 0) {
					candidatePathBuffer.insert(0, '/');
				}

				String referencingSegment = referencingSegments.get(referencingSegmentIndex);
				candidatePathBuffer.insert(0, referencingSegment);
				if (remappingMap.containsKey(candidatePathBuffer.toString())) {
					matchingSuffixPath = candidatePathBuffer.toString();
				} else if (matchingSuffixPath.length() > 0) {
					break;
				}
			}

			String matchingPath = null;
			String consumedReferencedPath = null;
			StringBuilder referencedPrefixBuffer = new StringBuilder();
			StringTokenizer referencedPathTokenizer = new StringTokenizer(referencedPath, "/");
			int referencedSegmentCount = referencedPathTokenizer.countTokens();

			for (int referencedSegmentIndex = 0; referencedSegmentIndex < referencedSegmentCount; referencedSegmentIndex++) {
				candidatePathBuffer.setLength(0);
				candidatePathBuffer.append(matchingSuffixPath);
				if (candidatePathBuffer.length() > 0) {
					candidatePathBuffer.append('/');
				}

				if (referencedPrefixBuffer.length() > 0) {
					referencedPrefixBuffer.append('/');
				}

				String referencedSegment = referencedPathTokenizer.nextToken();
				referencedPrefixBuffer.append(referencedSegment);
				candidatePathBuffer.append(referencedPrefixBuffer.toString());
				if (!remappingMap.containsKey(candidatePathBuffer.toString())) {
					break;
				}

				matchingPath = candidatePathBuffer.toString();
				consumedReferencedPath = referencedPrefixBuffer.toString();
			}

			if (matchingPath != null) {
				String mappedPath = remappingMap.get(matchingPath);
				if (matchingSuffixPath.length() > 0) {
					String mappedSuffixPath = remappingMap.get(matchingSuffixPath);
					if (matchingPath.length() > matchingSuffixPath.length()) {
						String relativePrefix = "";
						if (mappedPath.length() != mappedSuffixPath.length()) {
							if (mappedPath.length() > mappedSuffixPath.length()) {
								relativePrefix = mappedPath.substring(mappedSuffixPath.length() + (mappedPath.indexOf(47) == -1 ? 0 : 1));
							} else {
								relativePrefix = "..";
							}
						}

						if (consumedReferencedPath != null && consumedReferencedPath.length() < referencedPath.length()) {
							String remainingReferencedPath = referencedPath.substring(consumedReferencedPath.length() + (relativePrefix.length() > 0 ? 0 : 1));
							String relativePath = relativePrefix + remainingReferencedPath;
							return absolutePrefixRemoved ? '/' + relativePath : relativePath;
						} else {
							return absolutePrefixRemoved ? '/' + relativePrefix : relativePrefix;
						}
					} else {
						return absolutePrefixRemoved ? '/' + referencedPath : referencedPath;
					}
				} else if (consumedReferencedPath != null && consumedReferencedPath.length() < referencedPath.length()) {
					String referencedPathTail = referencedPath.substring(consumedReferencedPath.length() + (mappedPath.length() > 0 ? 0 : 1));
					String mappedReferencedPath = mappedPath + referencedPathTail;
					return absolutePrefixRemoved ? '/' + mappedReferencedPath : mappedReferencedPath;
				} else {
					return absolutePrefixRemoved ? '/' + mappedPath : mappedPath;
				}
			} else {
				return absolutePrefixRemoved ? '/' + referencedPath : referencedPath;
			}
		}
	}

	public static String remapPath(String path, ReadOnlyMap<String, String> remappingMap) {
		return remapPathAndSetDepth(path, remappingMap, new AtomicInt());
	}

	public static String remapPathAndSetDepth(String path, ReadOnlyMap<String, String> remappingMap, AtomicInt depth) {
		depth.set(0);
		if (remappingMap == null) {
			return path;
		} else {
			path = path.trim();
			StringBuilder candidatePathBuffer = new StringBuilder(path);
			StringBuilder mappedPathBuffer = new StringBuilder();

			while (true) {
				String candidatePath = candidatePathBuffer.toString();
				String mappedPath = remappingMap.get(candidatePath);
				if (mappedPath != null) {
					if (!mappedPath.equals(candidatePath)) {
						mappedPathBuffer.setLength(0);
						mappedPathBuffer.append(mappedPath);
						String remainingPath = path.substring(candidatePath.length());
						if (mappedPathBuffer.length() == 0 && remainingPath.length() > 0 && remainingPath.charAt(0) == '/') {
							remainingPath = remainingPath.substring(1);
						}

						mappedPathBuffer.append(remainingPath);
						depth.set(StringUtils.countOccurrences(candidatePath, '/') + 1);
						return mappedPathBuffer.toString();
					}
					break;
				}

				int truncateIndex = candidatePath.lastIndexOf("/");
				if (truncateIndex <= -1) {
					break;
				}

				candidatePathBuffer.setLength(truncateIndex);
			}

			return path;
		}
	}

	public static String remapDirectoryOfPath(String path, ReadOnlyMap<String, String> remappingMap) {
		if (remappingMap == null) {
			return path;
		} else {
			path = path.trim();
			int lastSeparator = path.lastIndexOf(47);
			if (lastSeparator <= 0) {
				return path;
			} else {
				String fileNamePart = path.substring(lastSeparator);
				String directoryPath = path.substring(0, lastSeparator);
				String absolutePrefix;
				if (directoryPath.length() > 0 && directoryPath.charAt(0) == '/') {
					absolutePrefix = "/";
					directoryPath = directoryPath.substring(1);
				} else {
					absolutePrefix = "";
				}

				StringBuilder candidatePathBuffer = new StringBuilder(directoryPath);
				StringBuilder mappedPathBuffer = new StringBuilder();

				while (true) {
					String candidatePath = candidatePathBuffer.toString();
					String mappedPath = remappingMap.get(candidatePath);
					if (mappedPath != null) {
						if (!mappedPath.equals(candidatePath)) {
							mappedPathBuffer.setLength(0);
							mappedPathBuffer.append(mappedPath);
							String remainingPath = directoryPath.substring(candidatePath.length());
							if (mappedPathBuffer.length() == 0 && remainingPath.length() > 0 && remainingPath.charAt(0) == '/') {
								remainingPath = remainingPath.substring(1);
							}

							mappedPathBuffer.append(remainingPath);
							if (mappedPathBuffer.length() == 0) {
								fileNamePart = fileNamePart.substring(1);
							}

							return absolutePrefix + mappedPathBuffer.toString() + fileNamePart;
						}
						break;
					}

					int truncateIndex = candidatePath.lastIndexOf("/");
					if (truncateIndex <= -1) {
						break;
					}

					candidatePathBuffer.setLength(truncateIndex);
				}

				return path;
			}
		}
	}

	public String remapClassFile(String classFilePath, ReadOnlyMap<String, String> remappingMap) {
		if (remappingMap != null) {
			int classSuffixIndex = classFilePath.indexOf(".class");
			String classPath = classFilePath.substring(0, classSuffixIndex);
			String absolutePrefix = "";
			if (classPath.charAt(0) == '/') {
				classPath = classPath.substring(1);
				absolutePrefix = "/";
			}

			String mappedPath = remappingMap.get(classPath);
			if (mappedPath != null) {
				return absolutePrefix + mappedPath + ".class";
			}

			String matchedPackagePrefix = "";

			for (int i = this.sortedPackagePrefixes.length - 1; i >= 0; i--) {
				if (classPath.startsWith(this.sortedPackagePrefixes[i])) {
					matchedPackagePrefix = this.sortedPackagePrefixes[i];
					break;
				}
			}

			if (matchedPackagePrefix.length() > 0) {
				return absolutePrefix + matchedPackagePrefix + GlobalUtils.getMappedOrDefault(classPath.substring(matchedPackagePrefix.length()), remappingMap) + ".class";
			}
		}

		return classFilePath;
	}

	public boolean isPathMapped(String path) {
		String normalizedPath = path;
		if (path.charAt(0) == '/') {
			normalizedPath = path.substring(1);
		}

		if (normalizedPath.charAt(normalizedPath.length() - 1) == '/') {
			normalizedPath = normalizedPath.substring(0, normalizedPath.length() - 1);
		}

		return this.remappingMap.containsKey(normalizedPath);
	}

	public String remapPath(String path) {
		return remapPath(path, this.remappingMap);
	}

	public String remapPathWithCache(String path) {
		if (path.indexOf("//") == -1
			&& path.indexOf("\\") == -1
			&& path.indexOf(":") == -1
			&& path.indexOf("!") == -1
			&& path.indexOf(" ") == -1
			&& path.indexOf("'") == -1
			&& path.indexOf("[") == -1
			&& path.indexOf("(") == -1) {
			String normalizedPath = path;
			String absolutePrefix = "";
			if (path.charAt(0) == '/') {
				normalizedPath = path.substring(1);
				absolutePrefix = "/";
			}

			int lastSeparator = normalizedPath.lastIndexOf(47);
			if (lastSeparator >= 0) {
				String fileName = normalizedPath.substring(lastSeparator + 1);
				String directoryPath = normalizedPath.substring(0, lastSeparator);
				String cachedMappedDirectory = this.remappedPathCache.get(directoryPath);
				if (cachedMappedDirectory != null) {
					StringBuilder remappedPathBuffer = new StringBuilder();
					remappedPathBuffer.append(absolutePrefix);
					remappedPathBuffer.append(cachedMappedDirectory);
					if (cachedMappedDirectory.length() > 0 && cachedMappedDirectory.charAt(cachedMappedDirectory.length() - 1) != '/' && (fileName.length() > 0 && fileName.charAt(0) != '/' || fileName.length() == 0 && path.endsWith("/"))
						)
					 {
						remappedPathBuffer.append("/");
					}

					remappedPathBuffer.append(fileName);
					return remappedPathBuffer.toString();
				}

				String matchedPackagePrefix = "";

				for (int i = this.sortedPackagePrefixes.length - 1; i >= 0; i--) {
					if (directoryPath.startsWith(this.sortedPackagePrefixes[i])) {
						matchedPackagePrefix = this.sortedPackagePrefixes[i];
						directoryPath = directoryPath.substring(this.sortedPackagePrefixes[i].length());
						break;
					}
				}

				StringBuilder candidatePathBuffer = new StringBuilder(directoryPath);

				while (true) {
					String candidatePath = candidatePathBuffer.toString();
					String mappedPath = this.remappingMap.get(candidatePath);
					if (mappedPath != null) {
						StringBuilder mappedDirectoryBuffer = new StringBuilder(mappedPath);
						String remainingPath = directoryPath.substring(candidatePath.length());
						if (mappedDirectoryBuffer.length() == 0 && remainingPath.length() > 0 && remainingPath.charAt(0) == '/') {
							remainingPath = remainingPath.substring(1);
						}

						mappedDirectoryBuffer.append(remainingPath);
						this.remappedPathCache.put(matchedPackagePrefix + directoryPath, matchedPackagePrefix + mappedDirectoryBuffer.toString());
						if (mappedDirectoryBuffer.length() > 0) {
							mappedDirectoryBuffer.append("/");
						}

						mappedDirectoryBuffer.append(fileName);
						return absolutePrefix + matchedPackagePrefix + mappedDirectoryBuffer.toString();
					}

					int truncateIndex = candidatePath.lastIndexOf("/");
					if (truncateIndex <= -1) {
						this.remappedPathCache.put(matchedPackagePrefix + directoryPath, matchedPackagePrefix + directoryPath);
						break;
					}

					candidatePathBuffer.setLength(truncateIndex);
				}
			}

			return path;
		} else {
			return path;
		}
	}

	public String remapRelativePath(String referencingPath, String referencedPath, boolean isAbsolutePath) {
		return remapRelativePath(referencingPath, referencedPath, isAbsolutePath, this.remappingMap);
	}

	public String getExactRemappedPath(String path) {
		String normalizedPath = path;
		String absolutePrefix = "";
		String trailingSlash = "";
		if (path.charAt(0) == '/') {
			normalizedPath = path.substring(1);
			absolutePrefix = "/";
		}

		if (normalizedPath.charAt(normalizedPath.length() - 1) == '/') {
			normalizedPath = normalizedPath.substring(0, normalizedPath.length() - 1);
			trailingSlash = "/";
		}

		String mappedPath = this.remappingMap.get(normalizedPath);
		return mappedPath != null ? absolutePrefix + mappedPath + (mappedPath.length() > 0 ? trailingSlash : "") : path;
	}
}
