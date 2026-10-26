package com.zelix.klassmaster.script;

import com.zelix.exceptions.ProcessingException;
import com.zelix.klassmaster.config.Options;
import com.zelix.utils.FileUtils;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.references.AtomicReference;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Properties;

public class ScriptPreprocessor {
	private static final String userDir = System.getProperty("user.dir");
	private final String marker;
	private final String directiveStart;
	private final String directiveEnd;
	private File includeDir;
	private Properties configProperties;
	private String script;

	public ScriptPreprocessor(String scriptFilePath, String includeDirectoryPath, Properties configProperties, String marker, String outputFilePath) throws IOException {
		if (marker == null) {
			this.marker = "%";
		} else {
			this.marker = marker;
		}

		this.directiveStart = '<' + this.marker;
		this.directiveEnd = this.marker + '>';
		if (includeDirectoryPath != null && !includeDirectoryPath.trim().isEmpty()) {
			this.includeDir = new File(includeDirectoryPath);
			if (!this.includeDir.exists()) {
				throw new ProcessingException("PREPROCESSOR ERROR:  include directory '" + this.includeDir.getAbsolutePath() + "' does not exist");
			}

			if (!this.includeDir.isDirectory()) {
				throw new ProcessingException("PREPROCESSOR ERROR:  include directory '" + this.includeDir.getAbsolutePath() + "' is not a directory");
			}
		}

		this.configProperties = configProperties;
		Options.relaxStackFrameCompatibility = !GlobalUtils.findHashedProperty(
				"b90e12da2adf99f479ecad90a530ea33b8701422fbbe250b5d5f8edb8f7096c8b61dba24a593c4406c81f08cf9f129d95040be9487d1c7f1b0a53713dd37cb2d",
				"true",
				configProperties
			)
			.equals("false");
		ArrayList<String> includeChain = new ArrayList<>();
		this.script = this.preprocessFile(scriptFilePath, includeChain);
		if (outputFilePath != null && !outputFilePath.isEmpty()) {
			AtomicReference<String> errMessageRef = new AtomicReference<>();
			FileUtils.writeStringToFile(this.script, new File(outputFilePath), errMessageRef);
			if (!errMessageRef.isNull()) {
				throw new ProcessingException("PREPROCESSOR ERROR:  '" + errMessageRef.get() + "'");
			}
		}
	}

	public ScriptPreprocessor(String scriptFilePath, Properties configProperties) throws IOException {
		this(scriptFilePath, null, configProperties, null);
	}

	public ScriptPreprocessor(String scriptFilePath, String includeDirectoryPath, Properties configProperties, String marker) throws IOException {
		this(scriptFilePath, includeDirectoryPath, configProperties, marker, null);
	}

	public BufferedReader getScriptReader() {
		return new BufferedReader(new StringReader(this.script));
	}

	private String buildIncludeChainPath(ArrayList<String> includeChain) {
		StringBuilder pathBuilder = new StringBuilder();

		for (int chainIndex = includeChain.size() - 1; chainIndex >= 0; chainIndex--) {
			String chainPath = includeChain.get(chainIndex);
			if (chainIndex == includeChain.size() - 1) {
				pathBuilder.append("'" + chainPath + "'");
			} else {
				pathBuilder.append(" included in '" + chainPath + "'");
			}
		}

		return pathBuilder.toString();
	}

	private String processIncludeDirective(String directive, ArrayList<String> includeChain) {
		int includeIndex = directive.indexOf("include");
		if (includeIndex == -1) {
			throw new ProcessingException("PREPROCESSOR ERROR: Missing 'include' within '" + this.directiveStart + directive + this.directiveEnd + "' in file " + this.buildIncludeChainPath(includeChain));
		} else if (!this.isBlank(directive.substring(0, includeIndex))) {
			throw new ProcessingException(
				"PREPROCESSOR ERROR: Invalid 'include' directive '"
					+ this.directiveStart
					+ directive
					+ this.directiveEnd
					+ "'. String '"
					+ directive.substring(0, includeIndex)
					+ "' is extraneous in file "
					+ this.buildIncludeChainPath(includeChain)
			);
		} else {
			int openQuoteIndex = directive.indexOf("\"", includeIndex + 7);
			if (openQuoteIndex > -1) {
				if (this.isBlank(directive.substring(includeIndex + 7, openQuoteIndex))) {
					int closeQuoteIndex = directive.indexOf("\"", openQuoteIndex + 1);
					if (closeQuoteIndex > -1) {
						String includePath = directive.substring(openQuoteIndex + 1, closeQuoteIndex);
						if (!this.isBlank(directive.substring(closeQuoteIndex + 1))) {
							throw new ProcessingException(
								"PREPROCESSOR ERROR: Invalid 'include' directive '"
									+ this.directiveStart
									+ directive
									+ this.directiveEnd
									+ "'. String '"
									+ directive.substring(closeQuoteIndex + 1)
									+ "' is extraneous in file "
									+ this.buildIncludeChainPath(includeChain)
							);
						} else {
							String resolvedPath;
							if (FileUtils.isRelativePath(includePath)) {
								if (this.includeDir != null) {
									File includeDirFile = new File(this.includeDir, includePath);
									if (!includeDirFile.exists()) {
										File userDirFile = new File(userDir, includePath);
										if (!userDirFile.exists()) {
											throw new ProcessingException(
												"PREPROCESSOR ERROR: Could not find either '"
													+ includeDirFile.getAbsolutePath()
													+ "' or '"
													+ userDirFile.getAbsolutePath()
													+ "' as specified in 'include' directive '"
													+ this.directiveStart
													+ directive
													+ this.directiveEnd
													+ "' in file "
													+ this.buildIncludeChainPath(includeChain)
											);
										}

										resolvedPath = userDirFile.getAbsolutePath();
									} else {
										resolvedPath = includeDirFile.getAbsolutePath();
									}
								} else {
									resolvedPath = new File(userDir, includePath).getAbsolutePath();
								}
							} else {
								resolvedPath = includePath;
							}

							return this.preprocessFile(resolvedPath, GlobalUtils.clone(includeChain));
						}
					} else {
						throw new ProcessingException(
							"PREPROCESSOR ERROR: Missing closing '\"' in 'include' directive '" + this.directiveStart + directive + this.directiveEnd + "' in file " + this.buildIncludeChainPath(includeChain)
						);
					}
				} else {
					throw new ProcessingException(
						"PREPROCESSOR ERROR: Invalid 'include' directive '"
							+ this.directiveStart
							+ directive
							+ this.directiveEnd
							+ "'. String '"
							+ directive.substring(includeIndex + 7, openQuoteIndex)
							+ "' is extraneous in file "
							+ this.buildIncludeChainPath(includeChain)
					);
				}
			} else {
				throw new ProcessingException(
					"PREPROCESSOR ERROR: Missing opening '\"' in 'include' directive '" + this.directiveStart + directive + this.directiveEnd + "' in file " + this.buildIncludeChainPath(includeChain)
				);
			}
		}
	}

	public String getPreprocessedScript() {
		return this.script;
	}

	private boolean isBlank(String text) {
		int textLength = text.length();

		for (int i = 0; i < textLength; i++) {
			if (text.charAt(i) != ' ') {
				return false;
			}
		}

		return true;
	}

	private String resolveVariables(String scriptText) {
		StringBuffer resolvedBuffer = new StringBuffer((int)(scriptText.length() * 1.2));
		int searchStart = 0;

		int markerStart;
		while ((markerStart = scriptText.indexOf(this.marker, searchStart)) > -1) {
			resolvedBuffer.append(scriptText.substring(searchStart, markerStart));
			if ((markerStart == 0 || scriptText.charAt(markerStart - 1) != '<') && markerStart < scriptText.length() - 3 && scriptText.charAt(markerStart + 1) != '>') {
				int markerEnd = scriptText.indexOf(this.marker, markerStart + this.marker.length());
				if (markerEnd > -1
					&& markerEnd > markerStart + this.marker.length()
					&& (
						markerStart <= this.marker.length()
							|| scriptText.charAt(markerStart - this.marker.length()) != '<'
							|| scriptText.length() - markerEnd < this.marker.length()
							|| scriptText.charAt(markerEnd + this.marker.length()) != '>'
					)) {
					String variableName = scriptText.substring(markerStart + this.marker.length(), markerEnd);
					String variableValue = null;
					if (this.configProperties != null) {
						variableValue = this.configProperties.getProperty(variableName);
					}

					if (variableValue == null) {
						variableValue = System.getProperty(variableName);
					}

					if (variableValue != null) {
						resolvedBuffer.append(variableValue);
						searchStart = markerEnd + this.marker.length();
					} else {
						resolvedBuffer.append(this.marker);
						searchStart = markerStart + this.marker.length();
					}
				} else {
					resolvedBuffer.append(this.marker);
					searchStart = markerStart + this.marker.length();
				}
			} else {
				resolvedBuffer.append(this.marker);
				searchStart = markerStart + this.marker.length();
			}
		}

		resolvedBuffer.append(scriptText.substring(searchStart));
		return resolvedBuffer.toString();
	}

	private String preprocessFile(String filePath, ArrayList<String> includeChain) {
		includeChain.add(filePath);
		StringBuilder resultBuilder = new StringBuilder(1028);
		BufferedReader reader = null;

		try {
			reader = FileUtils.createBufferedReader(filePath, Options.ZKM_SCRIPT_ENCODING);
			StringBuilder rawContentBuilder = new StringBuilder();

			int charCode;
			while ((charCode = reader.read()) != -1) {
				rawContentBuilder.append((char)charCode);
			}

			String fileContent = rawContentBuilder.toString();
			fileContent = this.resolveVariables(fileContent);
			int searchStart = 0;

			int startMarkerIndex;
			while ((startMarkerIndex = fileContent.indexOf(this.directiveStart, searchStart)) > -1) {
				resultBuilder.append(fileContent.substring(searchStart, startMarkerIndex));
				int endMarkerIndex = fileContent.indexOf(this.directiveEnd, startMarkerIndex);
				if (endMarkerIndex <= -1) {
					throw new ProcessingException("PREPROCESSOR ERROR: '" + this.directiveStart + "' without a matching '" + this.directiveEnd + "' in file " + this.buildIncludeChainPath(includeChain));
				}

				String includedContent = this.processIncludeDirective(fileContent.substring(startMarkerIndex + this.directiveStart.length(), endMarkerIndex), includeChain);
				resultBuilder.append(includedContent);
				searchStart = endMarkerIndex + this.directiveEnd.length();
			}

			resultBuilder.append(fileContent.substring(searchStart));
		} catch (FileNotFoundException ignoredFileNotFound) {
			throw new ProcessingException("PREPROCESSOR ERROR: Couldn't find file " + this.buildIncludeChainPath(includeChain));
		} catch (IOException ioException) {
			throw new ProcessingException("PREPROCESSOR ERROR: " + ioException + " in " + this.buildIncludeChainPath(includeChain));
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException ignoredCloseFailure) {
				}
			}
		}

		return resultBuilder.toString();
	}
}
