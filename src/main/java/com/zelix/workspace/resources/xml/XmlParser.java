package com.zelix.workspace.resources.xml;

import com.zelix.exceptions.XMLParseException;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.wrappers.FieldNode;
import com.zelix.framework.wrappers.MethodNode;
import com.zelix.utils.FileUtils;
import com.zelix.utils.collections.DualKeyMap;
import com.zelix.utils.dataset.AtomicInt;
import com.zelix.utils.references.AtomicReference;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class XmlParser {
	private static final String[] charStrings = new String[]{
		"\u0000",
		"\u0001",
		"\u0002",
		"\u0003",
		"\u0004",
		"\u0005",
		"\u0006",
		"\u0007",
		"\b",
		"\t",
		"\n",
		"\u000b",
		"\f",
		"\r",
		"\u000e",
		"\u000f",
		"\u0010",
		"\u0011",
		"\u0012",
		"\u0013",
		"\u0014",
		"\u0015",
		"\u0016",
		"\u0017",
		"\u0018",
		"\u0019",
		"\u001a",
		"\u001b",
		"\u001c",
		"\u001d",
		"\u001e",
		"\u001f",
		" ",
		"!",
		"\"",
		"#",
		"$",
		"%",
		"&",
		"'",
		"(",
		")",
		"*",
		"+",
		",",
		"-",
		".",
		"/",
		"0",
		"1",
		"2",
		"3",
		"4",
		"5",
		"6",
		"7",
		"8",
		"9",
		":",
		";",
		"<",
		"=",
		">",
		"?",
		"@",
		"A",
		"B",
		"C",
		"D",
		"E",
		"F",
		"G",
		"H",
		"I",
		"J",
		"K",
		"L",
		"M",
		"N",
		"O",
		"P",
		"Q",
		"R",
		"S",
		"T",
		"U",
		"V",
		"W",
		"X",
		"Y",
		"Z",
		"[",
		"\\",
		"]",
		"^",
		"_",
		"`",
		"a",
		"b",
		"c",
		"d",
		"e",
		"f",
		"g",
		"h",
		"i",
		"j",
		"k",
		"l",
		"m",
		"n",
		"o",
		"p",
		"q",
		"r",
		"s",
		"t",
		"u",
		"v",
		"w",
		"x",
		"y",
		"z",
		"{",
		"|",
		"}",
		"~",
		"\u007f"
	};
	private int state = 1;
	private int attributeState = 1;
	private char quoteChar = '~';
	private StringBuilder charDataBuffer = new StringBuilder();
	private StringBuilder tagNameBuffer = new StringBuilder();
	private boolean hasEndTagName = false;
	private boolean captureStarted = false;
	private StringBuilder segmentBuffer = new StringBuilder();
	private StringBuilder instructionBuffer = new StringBuilder();
	private boolean inText = false;
	private StringBuilder pendingText = new StringBuilder();
	private final XmlHandler handler;
	private final String resourcePath;
	private Map<ClassNode, String> classReferences;
	private Map<FieldNode, String> fieldReferences;
	private Map<MethodNode, String> methodReferences;
	private DualKeyMap<MethodNode, String, Object> propertyAccessors;
	private XmlElement currentElement;
	private String currentTagName;

	public XmlParser(InputStream inputStream, OutputStream outputStream, XmlHandler handler, String encoding, int skipBytes, int bomLength, int byteOrder, String resourcePath, boolean deleteComments) throws IOException {
		String effectiveEncoding;
		if (encoding == null) {
			effectiveEncoding = "UTF-8";
		} else {
			effectiveEncoding = encoding;
		}

		FileUtils.readBytes(inputStream, skipBytes);
		PushbackReader reader = new PushbackReader(new BufferedReader(new InputStreamReader(inputStream, effectiveEncoding)), 7);
		ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
		this.handler = handler;
		this.resourcePath = resourcePath;
		PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(outputBuffer, effectiveEncoding)));
		ArrayList<Object> tokens = new ArrayList<>();
		this.parse(reader, tokens, writer, deleteComments);
		this.flushTokens(tokens, writer);
		writer.close();
		byte[] outputBytes = prependBom(outputBuffer.toByteArray(), bomLength, byteOrder);
		outputStream.write(outputBytes);
		outputStream.flush();
	}

	public XmlParser(
		InputStream inputStream,
		XmlHandler handler,
		String encoding,
		int skipBytes,
		int bomLength,
		int byteOrder,
		Map<ClassNode, String> classReferences,
		Map<FieldNode, String> fieldReferences,
		Map<MethodNode, String> methodReferences,
		DualKeyMap<MethodNode, String, Object> propertyAccessors,
		String resourcePath
	) throws IOException {
		String effectiveEncoding;
		if (encoding == null) {
			effectiveEncoding = "UTF-8";
		} else {
			effectiveEncoding = encoding;
		}

		FileUtils.readBytes(inputStream, skipBytes);
		PushbackReader reader = new PushbackReader(new BufferedReader(new InputStreamReader(inputStream, effectiveEncoding)), 7);
		this.handler = handler;
		this.resourcePath = resourcePath;
		this.classReferences = classReferences;
		this.fieldReferences = fieldReferences;
		this.methodReferences = methodReferences;
		this.propertyAccessors = propertyAccessors;
		this.parse(reader, null, null, false);
	}

	public static String readFile(File file, AtomicReference<String> encoding, AtomicInt skipBytes, AtomicInt bomLength, AtomicInt byteOrder) throws IOException {
		String detectedEncoding = detectXmlEncoding(new FileInputStream(file), skipBytes, bomLength, byteOrder);
		encoding.set(detectedEncoding);
		return FileUtils.readToString(file, detectedEncoding);
	}

	public static byte[] prependBom(byte[] data, int bomLength, int byteOrder) {
		if (bomLength > 0) {
			switch (bomLength) {
				case 2:
					if (data.length < 2 || data[0] != -2 && data[0] != -1) {
						byte[] utf16Bom = new byte[2];
						if (byteOrder == 1) {
							utf16Bom[0] = -2;
							utf16Bom[1] = -1;
						} else if (byteOrder == 0) {
							utf16Bom[0] = -1;
							utf16Bom[1] = -2;
						}

						byte[] utf16Bytes = new byte[data.length + 2];
						utf16Bytes[0] = utf16Bom[0];
						utf16Bytes[1] = utf16Bom[1];
						System.arraycopy(data, 0, utf16Bytes, 2, data.length);
						data = utf16Bytes;
					}
					break;
				case 3:
					if (data.length < 3 || data[0] != -17) {
						byte[] utf8Bytes = new byte[data.length + 3];
						utf8Bytes[0] = -17;
						utf8Bytes[1] = -69;
						utf8Bytes[2] = -65;
						System.arraycopy(data, 0, utf8Bytes, 3, data.length);
						data = utf8Bytes;
					}
			}
		}

		return data;
	}

	private static String detectBomEncoding(PushbackInputStream pushbackStream, AtomicInt skipBytes, AtomicInt bomLength, AtomicInt byteOrder) throws IOException {
		bomLength.set(0);
		skipBytes.set(0);
		byteOrder.set(Integer.MAX_VALUE);
		String detectedEncoding = "UTF-8";
		byte[] headerBytes = new byte[4];
		int bytesRead = FileUtils.readFully(pushbackStream, headerBytes);
		if (bytesRead == -1) {
			bytesRead = 0;
		}

		pushbackStream.unread(headerBytes);
		if (bytesRead > 1) {
			int byte0 = headerBytes[0] & 255;
			int byte1 = headerBytes[1] & 255;
			int byte2 = headerBytes[2] & 255;
			int byte3 = headerBytes[3] & 255;
			if (byte0 != 60 || byte1 != 63 || byte2 != 120 || byte3 != 109) {
				if (byte0 == 239 && byte1 == 187 && byte2 == 191) {
					bomLength.set(3);
					skipBytes.set(3);
				} else if (byte0 == 255 && byte1 == 254) {
					detectedEncoding = "UTF-16";
					bomLength.set(2);
					byteOrder.set(0);
				} else if (byte0 == 254 && byte1 == 255) {
					detectedEncoding = "UTF-16";
					bomLength.set(2);
					byteOrder.set(1);
				} else if (byte0 == 60 && byte1 == 0 && byte2 == 63 && byte3 == 0) {
					detectedEncoding = "UTF-16LE";
					byteOrder.set(0);
				} else if (byte0 == 0 && byte1 == 60 && byte2 == 0 && byte3 == 63) {
					detectedEncoding = "UTF-16BE";
					byteOrder.set(1);
				}
			}
		}

		return detectedEncoding;
	}

	public static String detectXmlEncoding(InputStream inputStream, AtomicInt skipBytes, AtomicInt bomLength, AtomicInt byteOrder) {
		String bomEncoding = null;
		String declaredEncoding = null;
		PushbackReader reader = null;
		PushbackInputStream pushbackStream = new PushbackInputStream(inputStream, 4);

		try {
			boolean atDeclarationStart = true;
			StringBuffer declarationBuffer = new StringBuffer();
			bomEncoding = detectBomEncoding(pushbackStream, skipBytes, bomLength, byteOrder);
			FileUtils.readBytes(pushbackStream, skipBytes.get());
			InputStreamReader inputReader = new InputStreamReader(pushbackStream, bomEncoding);
			reader = new PushbackReader(new BufferedReader(inputReader), 7);

			int charCode;
			while ((charCode = reader.read()) != -1) {
				char currentChar = (char)charCode;
				if (currentChar != ' ' && currentChar != '\t' && currentChar != '\n' && currentChar != '\r') {
					if (atDeclarationStart) {
						atDeclarationStart = false;
						if (currentChar != '<' || (charCode = reader.read()) == -1 || (char)charCode != '?') {
							break;
						}
					} else {
						if (currentChar == '?' && (charCode = reader.read()) != -1) {
							char nextChar = (char)charCode;
							if (nextChar == '>') {
								break;
							}

							reader.unread(nextChar);
						}

						declarationBuffer.append(currentChar);
					}
				}
			}

			String declaration = declarationBuffer.toString();
			int encodingIndex = declaration.indexOf("encoding=\"");
			if (encodingIndex > -1) {
				int valueStart = encodingIndex + 10;
				if (valueStart < declaration.length()) {
					int valueEnd = declaration.indexOf(34, valueStart);
					if (valueEnd > -1) {
						declaredEncoding = declaration.substring(valueStart, valueEnd).trim();
						if (declaredEncoding.length() == 0) {
							declaredEncoding = null;
						}
					}
				}
			}
		} catch (IOException ignoredDetection) {
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException ignoredReaderClose) {
				}
			} else if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException ignoredStreamClose) {
				}
			}
		}

		if (declaredEncoding == null) {
			declaredEncoding = bomEncoding;
		}

		if (declaredEncoding.equals("UTF-16")) {
			if (bomLength.get() == 2) {
				skipBytes.set(2);
			}

			if (byteOrder.get() == 0) {
				declaredEncoding = "UTF-16LE";
			} else if (byteOrder.get() == 1) {
				declaredEncoding = "UTF-16BE";
			}
		}

		return declaredEncoding;
	}

	public static String readZipEntry(ZipFile zipFile, ZipEntry zipEntry, AtomicReference<String> encoding, AtomicInt skipBytes, AtomicInt bomLength, AtomicInt byteOrder) throws IOException {
		String detectedEncoding = detectXmlEncoding(zipFile.getInputStream(zipEntry), skipBytes, bomLength, byteOrder);
		encoding.set(detectedEncoding);
		return FileUtils.readToString(zipFile, zipEntry, detectedEncoding);
	}

	private void flushTokens(List<Object> tokens, PrintWriter writer) {
		this.handler.initialize();

		for (Object token : tokens) {
			writer.print(token);
		}

		tokens.clear();
	}

	public void handleEndTag(XmlElement element, List<Object> tokens, PrintWriter writer) {
		if (tokens != null) {
			this.handler.handleEndTag(element, tokens);
			if (this.handler.supportsHierarchy()) {
				this.flushTokens(tokens, writer);
			}
		} else {
			this.handler.recordEndTagReferences(element, this.classReferences, this.fieldReferences, this.methodReferences, this.propertyAccessors);
		}
	}

	public void handleProcessingInstruction(StringBuilder instructionBuffer, List<Object> tokens) {
		if (tokens != null) {
			this.handler.handleCharacters(instructionBuffer.toString().trim(), tokens);
		} else {
			this.handler.handleProcessingInstruction(instructionBuffer.toString().trim(), this.classReferences, this.fieldReferences, this.methodReferences, this.propertyAccessors);
		}
	}

	private boolean isQuoteChar(int charCode) {
		return this.quoteChar != '~' ? charCode == this.quoteChar : charCode == 39 || charCode == 34;
	}

	public void completeTag(int tagKind, List<Object> tokens, PrintWriter writer) {
		this.captureStarted = true;
		this.currentTagName = this.tagNameBuffer.toString().trim();
		switch (tagKind) {
			case 1:
				this.handleStartTag(this.currentElement, tokens, writer);
				break;
			case 2:
				this.handleEndTagByName(this.currentTagName, tokens, writer);
				break;
			case 3:
				this.handleEndTag(this.currentElement, tokens, writer);
		}
	}

	public final void handleEndTagByName(String tagName, List<Object> tokens, PrintWriter writer) {
		if (tagName.length() > 0) {
			this.handler.handleEndTagByName(tagName, tokens);
			if (tokens != null && this.handler.supportsHierarchy()) {
				this.flushTokens(tokens, writer);
			}
		}
	}

	private void handleCharacterData(String text, String tagName, List<Object> tokens, PrintWriter writer) {
		if (tagName == null) {
			String trimmedText = text.trim();
			if (trimmedText.length() > 1 && trimmedText.charAt(0) == '&' && trimmedText.charAt(trimmedText.length() - 1) == ';') {
				tagName = "!ENTITY";
			}
		}

		if (tokens != null) {
			this.handler.handleCharacterData(text, tagName, tokens);
			if (this.handler.supportsHierarchy()) {
				this.flushTokens(tokens, writer);
			}
		} else {
			this.handler.recordTextReferences(text, tagName, this.classReferences, this.fieldReferences, this.methodReferences, this.propertyAccessors);
		}
	}

	public void handleStartTag(XmlElement element, List<Object> tokens, PrintWriter writer) {
		if (tokens != null) {
			this.handler.handleStartTag(element, tokens);
			if (this.handler.supportsHierarchy()) {
				this.flushTokens(tokens, writer);
			}
		} else {
			this.handler.recordStartTagReferences(element, this.classReferences, this.fieldReferences, this.methodReferences, this.propertyAccessors);
		}
	}

	private void readComment(StringBuilder commentBuffer, PushbackReader reader) throws IOException {
		int charCode;
		while ((charCode = reader.read()) != -1) {
			char currentChar = (char)charCode;
			commentBuffer.append(currentChar);
			if (currentChar == '-' && (charCode = reader.read()) != -1) {
				char charAfterDash = (char)charCode;
				if (charAfterDash == '-') {
					if ((charCode = reader.read()) != -1) {
						char charAfterDoubleDash = (char)charCode;
						if (charAfterDoubleDash == '>') {
							commentBuffer.append("->");
							return;
						}

						reader.unread(charAfterDoubleDash);
						reader.unread(charAfterDash);
					} else {
						reader.unread(charAfterDash);
					}
				} else {
					reader.unread(charAfterDash);
				}
			}
		}

		throw new XMLParseException("Unterminated comment in '" + this.resourcePath + "'");
	}

	private void parse(PushbackReader reader, List<Object> tokens, PrintWriter writer, boolean deleteComments) throws IOException {
		boolean inQuote = false;
		int charCode = 0;

		while (charCode != -1 && (charCode = reader.read()) != -1) {
			char currentChar = (char)charCode;
			boolean whitespace = currentChar == ' ' || currentChar == '\t' || currentChar == '\n' || currentChar == '\r';
			label475:
			switch (this.state) {
				case 1:
					if (currentChar == '<') {
						this.inText = false;
						String charData = this.charDataBuffer.toString();
						if (charData.length() > 0) {
							this.handleCharacterData(charData, this.currentTagName, tokens, writer);
							this.charDataBuffer.setLength(0);
						}

						if (tokens != null) {
							tokens.add(this.pendingText.toString());
							this.pendingText.setLength(0);
							tokens.add(charStrings[currentChar]);
						}

						if ((charCode = reader.read()) != -1) {
							char charAfterLt = (char)charCode;
							if (charAfterLt == '!') {
								if (tokens != null) {
									tokens.add(charStrings[charAfterLt]);
								}

								if ((charCode = reader.read()) != -1) {
									char charAfterBang = (char)charCode;
									if (charAfterBang == '-') {
										if ((charCode = reader.read()) != -1) {
											char charAfterDash = (char)charCode;
											if (charAfterDash == '-') {
												if (tokens != null) {
													tokens.add("--");
												}

												StringBuilder commentBuffer = new StringBuilder();
												this.readComment(commentBuffer, reader);
												if (tokens != null) {
													if (!deleteComments) {
														tokens.add(commentBuffer.toString());
													} else {
														tokens.remove(tokens.size() - 1);
														tokens.remove(tokens.size() - 1);
														tokens.remove(tokens.size() - 1);
													}
												}
											} else {
												reader.unread(charAfterDash);
												reader.unread(charAfterBang);
												this.enterState(3);
											}
										} else if (tokens != null) {
											tokens.add(charStrings[charAfterBang]);
										}
									} else if (charAfterBang == '[') {
										if ((charCode = reader.read()) != -1) {
											char cdataStartAfterBracket = (char)charCode;
											if (cdataStartAfterBracket == 'C') {
												if ((charCode = reader.read()) != -1) {
													char cdataAfterC = (char)charCode;
													if (cdataAfterC == 'D') {
														if ((charCode = reader.read()) != -1) {
															char cdataAfterD = (char)charCode;
															if (cdataAfterD == 'A') {
																if ((charCode = reader.read()) != -1) {
																	char cdataAfterA = (char)charCode;
																	if (cdataAfterA == 'T') {
																		if ((charCode = reader.read()) != -1) {
																			char cdataAfterT = (char)charCode;
																			if (cdataAfterT == 'A') {
																				if ((charCode = reader.read()) != -1) {
																					char cdataAfterSecondA = (char)charCode;
																					if (cdataAfterSecondA == '[') {
																						if (tokens != null) {
																							tokens.add("[CDATA[");
																						}

																						this.enterState(6);
																					} else {
																						reader.unread(cdataAfterSecondA);
																						reader.unread(cdataAfterT);
																						reader.unread(cdataAfterA);
																						reader.unread(cdataAfterD);
																						reader.unread(cdataAfterC);
																						reader.unread(cdataStartAfterBracket);
																						reader.unread(charAfterBang);
																						this.enterState(3);
																					}
																				} else if (tokens != null) {
																					tokens.add("[CDATA");
																				}
																			} else {
																				reader.unread(cdataAfterT);
																				reader.unread(cdataAfterA);
																				reader.unread(cdataAfterD);
																				reader.unread(cdataAfterC);
																				reader.unread(cdataStartAfterBracket);
																				reader.unread(charAfterBang);
																				this.enterState(3);
																			}
																		} else if (tokens != null) {
																			tokens.add("[CDAT");
																		}
																	} else {
																		reader.unread(cdataAfterA);
																		reader.unread(cdataAfterD);
																		reader.unread(cdataAfterC);
																		reader.unread(cdataStartAfterBracket);
																		reader.unread(charAfterBang);
																		this.enterState(3);
																	}
																} else if (tokens != null) {
																	tokens.add("[CDA");
																}
															} else {
																reader.unread(cdataAfterD);
																reader.unread(cdataAfterC);
																reader.unread(cdataStartAfterBracket);
																reader.unread(charAfterBang);
																this.enterState(3);
															}
														} else if (tokens != null) {
															tokens.add("[CD");
														}
													} else {
														reader.unread(cdataAfterC);
														reader.unread(cdataStartAfterBracket);
														reader.unread(charAfterBang);
														this.enterState(3);
													}
												} else if (tokens != null) {
													tokens.add("[C");
												}
											} else {
												reader.unread(cdataStartAfterBracket);
												reader.unread(charAfterBang);
												this.enterState(3);
											}
										} else if (tokens != null) {
											tokens.add(charAfterBang);
										}
									} else if (charAfterBang == 'D') {
										if ((charCode = reader.read()) != -1) {
											char doctypeAfterD = (char)charCode;
											if (doctypeAfterD == 'O') {
												if ((charCode = reader.read()) != -1) {
													char doctypeAfterO = (char)charCode;
													if (doctypeAfterO == 'C') {
														if ((charCode = reader.read()) != -1) {
															char doctypeAfterC = (char)charCode;
															if (doctypeAfterC == 'T') {
																if ((charCode = reader.read()) != -1) {
																	char doctypeAfterT = (char)charCode;
																	if (doctypeAfterT == 'Y') {
																		if ((charCode = reader.read()) != -1) {
																			char doctypeAfterY = (char)charCode;
																			if (doctypeAfterY == 'P') {
																				if ((charCode = reader.read()) != -1) {
																					char doctypeAfterP = (char)charCode;
																					if (doctypeAfterP == 'E') {
																						if (tokens != null) {
																							tokens.add("DOCTYPE");
																						}

																						this.enterState(4);
																					} else {
																						reader.unread(doctypeAfterP);
																						reader.unread(doctypeAfterY);
																						reader.unread(doctypeAfterT);
																						reader.unread(doctypeAfterC);
																						reader.unread(doctypeAfterO);
																						reader.unread(doctypeAfterD);
																						reader.unread(charAfterBang);
																						this.enterState(3);
																					}
																				} else if (tokens != null) {
																					tokens.add("DOCTYP");
																				}
																			} else {
																				reader.unread(doctypeAfterY);
																				reader.unread(doctypeAfterT);
																				reader.unread(doctypeAfterC);
																				reader.unread(doctypeAfterO);
																				reader.unread(doctypeAfterD);
																				reader.unread(charAfterBang);
																				this.enterState(3);
																			}
																		} else if (tokens != null) {
																			tokens.add("DOCTY");
																		}
																	} else {
																		reader.unread(doctypeAfterT);
																		reader.unread(doctypeAfterC);
																		reader.unread(doctypeAfterO);
																		reader.unread(doctypeAfterD);
																		reader.unread(charAfterBang);
																		this.enterState(3);
																	}
																} else if (tokens != null) {
																	tokens.add("DOCT");
																}
															} else {
																reader.unread(doctypeAfterC);
																reader.unread(doctypeAfterO);
																reader.unread(doctypeAfterD);
																reader.unread(charAfterBang);
																this.enterState(3);
															}
														} else if (tokens != null) {
															tokens.add("DOC");
														}
													} else {
														reader.unread(doctypeAfterO);
														reader.unread(doctypeAfterD);
														reader.unread(charAfterBang);
														this.enterState(3);
													}
												} else if (tokens != null) {
													tokens.add("DO");
												}
											} else {
												reader.unread(doctypeAfterD);
												reader.unread(charAfterBang);
												this.enterState(3);
											}
										} else if (tokens != null) {
											tokens.add(charStrings[charAfterBang]);
										}
									} else {
										reader.unread(charAfterBang);
										this.enterState(3);
									}
								}
							} else if (charAfterLt == '?') {
								if (tokens != null) {
									tokens.add(charStrings[charAfterLt]);
								}

								this.enterState(2);
							} else if (charAfterLt == '/') {
								if (tokens != null) {
									tokens.add(charStrings[charAfterLt]);
								}

								this.enterState(8);
							} else {
								reader.unread(charAfterLt);
								this.enterState(7);
							}
						}
					} else if (whitespace) {
						if (!this.inText) {
							if (tokens != null) {
								tokens.add(charStrings[currentChar]);
							}
						} else {
							StringBuffer whitespaceBuffer = new StringBuffer();
							whitespaceBuffer.append(currentChar);

							do {
								charCode = reader.read();
								if (charCode != -1) {
									char nextChar = (char)charCode;
									if (nextChar != ' ' && nextChar != '\t' && nextChar != '\n' && nextChar != '\r') {
										if (nextChar == '<') {
											reader.unread(nextChar);
											this.pendingText.setLength(0);
											this.pendingText.append(whitespaceBuffer.toString());
										} else {
											whitespaceBuffer.append(nextChar);
											this.charDataBuffer.append(whitespaceBuffer);
										}
										break label475;
									}

									whitespaceBuffer.append(nextChar);
								} else {
									this.charDataBuffer.append(whitespaceBuffer);
								}
							} while (charCode != -1);
						}
					} else {
						this.inText = true;
						this.charDataBuffer.append(currentChar);
					}
					break;
				case 2:
					if (currentChar == '?') {
						if ((charCode = reader.read()) != -1) {
							char charAfterQuestion = (char)charCode;
							if (charAfterQuestion == '>') {
								this.handleProcessingInstruction(this.instructionBuffer, tokens);
								if (tokens != null) {
									tokens.add("?>");
								}

								this.enterState(1);
							} else {
								this.instructionBuffer.append(currentChar);
								reader.unread(charAfterQuestion);
							}
						}
					} else {
						this.instructionBuffer.append(currentChar);
					}
					break;
				case 3:
					if (tokens != null) {
						tokens.add(currentChar < charStrings.length ? charStrings[currentChar] : currentChar);
					}

					if (currentChar == '>') {
						this.enterState(1);
					}
					break;
				case 4:
					if (tokens != null) {
						tokens.add(currentChar < charStrings.length ? charStrings[currentChar] : currentChar);
					}

					if (currentChar == '>') {
						this.enterState(1);
					} else if (currentChar == '[') {
						this.enterState(5);
					}
					break;
				case 5:
					if (tokens != null) {
						tokens.add(currentChar < charStrings.length ? charStrings[currentChar] : currentChar);
					}

					if (this.isQuoteChar(currentChar)) {
						inQuote = !inQuote;
						if (inQuote) {
							this.quoteChar = currentChar;
						}
					} else if (!inQuote && currentChar == ']') {
						this.enterState(4);
					}
					break;
				case 6:
					if (tokens != null) {
						tokens.add(currentChar < charStrings.length ? charStrings[currentChar] : currentChar);
					}

					if (currentChar == ']' && (charCode = reader.read()) != -1) {
						char cdataEndAfterBracket = (char)charCode;
						if (cdataEndAfterBracket == ']') {
							if ((charCode = reader.read()) != -1) {
								char cdataEndAfterSecondBracket = (char)charCode;
								if (cdataEndAfterSecondBracket == '>') {
									if (tokens != null) {
										tokens.add("]>");
									}

									this.enterState(1);
								} else {
									reader.unread(cdataEndAfterSecondBracket);
									reader.unread(cdataEndAfterBracket);
								}
							} else if (tokens != null) {
								tokens.add(cdataEndAfterBracket < charStrings.length ? charStrings[cdataEndAfterBracket] : cdataEndAfterBracket);
							}
						} else {
							reader.unread(cdataEndAfterBracket);
						}
					}
					break;
				case 7:
					if (this.isQuoteChar(currentChar)) {
						inQuote = !inQuote;
						if (inQuote) {
							this.quoteChar = currentChar;
						}
					}

					if (currentChar == '>' && !inQuote) {
						switch (this.attributeState) {
							case 1:
								this.segmentBuffer.append(currentChar);
								this.currentElement.appendContent(this.segmentBuffer.toString());
								this.segmentBuffer.setLength(0);
								break;
							case 2:
								this.currentElement.addNameToken(this.segmentBuffer.toString());
								if (!this.captureStarted) {
									this.captureStarted = true;
									this.tagNameBuffer.append((CharSequence)this.segmentBuffer);
								}

								this.segmentBuffer.setLength(0);
								this.segmentBuffer.append(currentChar);
								this.currentElement.appendContent(this.segmentBuffer.toString());
								this.segmentBuffer.setLength(0);
						}

						this.completeTag(1, tokens, writer);
						this.enterState(1);
					} else if (currentChar == '/' && !inQuote) {
						if ((charCode = reader.read()) != -1) {
							char charAfterSlash = (char)charCode;
							if (charAfterSlash == '>') {
								switch (this.attributeState) {
									case 1:
										this.segmentBuffer.append(currentChar);
										this.segmentBuffer.append(charAfterSlash);
										this.currentElement.appendContent(this.segmentBuffer.toString());
										this.segmentBuffer.setLength(0);
										break;
									case 2:
										this.currentElement.addNameToken(this.segmentBuffer.toString());
										if (!this.captureStarted) {
											this.captureStarted = true;
											this.tagNameBuffer.append((CharSequence)this.segmentBuffer);
										}

										this.segmentBuffer.setLength(0);
										this.segmentBuffer.append(currentChar);
										this.segmentBuffer.append(charAfterSlash);
										this.currentElement.appendContent(this.segmentBuffer.toString());
										this.segmentBuffer.setLength(0);
								}

								this.completeTag(3, tokens, writer);
								this.enterState(1);
							} else {
								this.segmentBuffer.append(currentChar);
								reader.unread(charAfterSlash);
							}
						}
					} else {
						switch (this.attributeState) {
							case 1:
								if (!whitespace && currentChar != '=' && currentChar != this.quoteChar) {
									this.currentElement.appendContent(this.segmentBuffer.toString());
									this.segmentBuffer.setLength(0);
									this.segmentBuffer.append(currentChar);
									this.attributeState = 2;
								} else {
									this.segmentBuffer.append(currentChar);
									if (currentChar == this.quoteChar) {
										this.currentElement.appendContent(this.segmentBuffer.toString());
										this.segmentBuffer.setLength(0);
										this.attributeState = 3;
									}
								}
								break;
							case 2:
								if (!whitespace && currentChar != '=') {
									this.segmentBuffer.append(currentChar);
									break;
								}

								this.currentElement.addNameToken(this.segmentBuffer.toString());
								if (!this.captureStarted) {
									this.captureStarted = true;
									this.tagNameBuffer.append((CharSequence)this.segmentBuffer);
								}

								this.segmentBuffer.setLength(0);
								this.segmentBuffer.append(currentChar);
								this.attributeState = 1;
								break;
							case 3:
								if (currentChar == this.quoteChar) {
									this.currentElement.addAttribute(this.segmentBuffer.toString());
									this.segmentBuffer.setLength(0);
									this.segmentBuffer.append(currentChar);
									this.attributeState = 1;
								} else {
									this.segmentBuffer.append(currentChar);
								}
						}

						if (this.segmentBuffer.length() == 4 && this.segmentBuffer.toString().equals("<!--")) {
							this.readComment(this.segmentBuffer, reader);
							this.currentElement.appendContent(this.segmentBuffer.toString());
							this.segmentBuffer.setLength(0);
							this.attributeState = 1;
						}
					}
					break;
				case 8:
					if (currentChar == '>') {
						this.completeTag(2, tokens, writer);
						if (tokens != null) {
							if (this.pendingText.length() > 0) {
								tokens.add(this.pendingText.toString());
								this.pendingText.setLength(0);
							}

							tokens.add(currentChar < charStrings.length ? charStrings[currentChar] : currentChar);
						}

						this.enterState(1);
					} else if (whitespace) {
						if (this.hasEndTagName && !this.captureStarted) {
							this.captureStarted = true;
							this.pendingText.setLength(0);
						}

						if (tokens != null) {
							if (this.hasEndTagName) {
								this.pendingText.append(currentChar);
							} else {
								tokens.add(currentChar < charStrings.length ? charStrings[currentChar] : currentChar);
							}
						}
					} else if (currentChar == '<') {
						StringBuilder endTagCommentBuffer = new StringBuilder();
						endTagCommentBuffer.append(currentChar);
						this.readComment(endTagCommentBuffer, reader);
						this.pendingText.append(endTagCommentBuffer.toString());
					} else {
						this.hasEndTagName = true;
						if (!this.captureStarted) {
							this.tagNameBuffer.append(currentChar);
						}
					}
			}

			if (!inQuote && this.quoteChar != '~') {
				this.quoteChar = '~';
			}
		}

		if (this.state == 1 && this.charDataBuffer.length() > 0) {
			this.handleCharacterData(this.charDataBuffer.toString(), null, tokens, writer);
		}
	}

	public final void enterState(int state) {
		this.state = state;
		switch (this.state) {
			case 1:
				this.tagNameBuffer.setLength(0);
				this.hasEndTagName = false;
				this.captureStarted = false;
				this.pendingText.setLength(0);
				break;
			case 2:
				this.instructionBuffer.setLength(0);
				break;
			case 7:
				this.attributeState = 1;
				this.currentElement = new XmlElement();
				this.segmentBuffer.setLength(0);
			default:
				this.charDataBuffer.setLength(0);
		}
	}
}
