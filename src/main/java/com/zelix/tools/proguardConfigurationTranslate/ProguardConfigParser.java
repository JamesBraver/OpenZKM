package com.zelix.tools.proguardConfigurationTranslate;

import com.zelix.exceptions.ProguardCommandParseException;
import com.zelix.tools.proguardConfigurationTranslate.base.ProguardNodeBase;
import com.zelix.tools.proguardConfigurationTranslate.commands.AdaptClassStrings;
import com.zelix.tools.proguardConfigurationTranslate.commands.AdaptKotlinMetadata;
import com.zelix.tools.proguardConfigurationTranslate.commands.AdaptResourceFileContents;
import com.zelix.tools.proguardConfigurationTranslate.commands.AdaptResourceFileNames;
import com.zelix.tools.proguardConfigurationTranslate.commands.AddConfigurationDebugging;
import com.zelix.tools.proguardConfigurationTranslate.commands.AllowAccessModification;
import com.zelix.tools.proguardConfigurationTranslate.commands.Android;
import com.zelix.tools.proguardConfigurationTranslate.commands.ApplyMapping;
import com.zelix.tools.proguardConfigurationTranslate.commands.AssumeNoEscapingParameters;
import com.zelix.tools.proguardConfigurationTranslate.commands.AssumeNoExternalReturnValues;
import com.zelix.tools.proguardConfigurationTranslate.commands.AssumeNoExternalSideEffect;
import com.zelix.tools.proguardConfigurationTranslate.commands.AssumeNoSideEffects;
import com.zelix.tools.proguardConfigurationTranslate.commands.AssumeValues;
import com.zelix.tools.proguardConfigurationTranslate.commands.BaseDirectory;
import com.zelix.tools.proguardConfigurationTranslate.commands.ClassObfuscationDictionary;
import com.zelix.tools.proguardConfigurationTranslate.commands.DontCompress;
import com.zelix.tools.proguardConfigurationTranslate.commands.DontNote;
import com.zelix.tools.proguardConfigurationTranslate.commands.DontObfuscate;
import com.zelix.tools.proguardConfigurationTranslate.commands.DontOptimize;
import com.zelix.tools.proguardConfigurationTranslate.commands.DontPreVerify;
import com.zelix.tools.proguardConfigurationTranslate.commands.DontProcessKotlinMetadata;
import com.zelix.tools.proguardConfigurationTranslate.commands.DontShrink;
import com.zelix.tools.proguardConfigurationTranslate.commands.DontSkipNonPublicLibraryClasses;
import com.zelix.tools.proguardConfigurationTranslate.commands.DontUseMixedCaseClassNames;
import com.zelix.tools.proguardConfigurationTranslate.commands.DontWarn;
import com.zelix.tools.proguardConfigurationTranslate.commands.Dump;
import com.zelix.tools.proguardConfigurationTranslate.commands.FlattenPackageHierarchy;
import com.zelix.tools.proguardConfigurationTranslate.commands.ForceProcessing;
import com.zelix.tools.proguardConfigurationTranslate.commands.If;
import com.zelix.tools.proguardConfigurationTranslate.commands.IgnoreWarnings;
import com.zelix.tools.proguardConfigurationTranslate.commands.InJars;
import com.zelix.tools.proguardConfigurationTranslate.commands.Include;
import com.zelix.tools.proguardConfigurationTranslate.commands.Keep;
import com.zelix.tools.proguardConfigurationTranslate.commands.KeepAttributes;
import com.zelix.tools.proguardConfigurationTranslate.commands.KeepClassMemberNames;
import com.zelix.tools.proguardConfigurationTranslate.commands.KeepClassMembers;
import com.zelix.tools.proguardConfigurationTranslate.commands.KeepClassesWithMemberNames;
import com.zelix.tools.proguardConfigurationTranslate.commands.KeepClassesWithMembers;
import com.zelix.tools.proguardConfigurationTranslate.commands.KeepCode;
import com.zelix.tools.proguardConfigurationTranslate.commands.KeepDirectories;
import com.zelix.tools.proguardConfigurationTranslate.commands.KeepKotlinMetadata;
import com.zelix.tools.proguardConfigurationTranslate.commands.KeepNames;
import com.zelix.tools.proguardConfigurationTranslate.commands.KeepPackageNames;
import com.zelix.tools.proguardConfigurationTranslate.commands.KeepParameterNames;
import com.zelix.tools.proguardConfigurationTranslate.commands.LibraryJars;
import com.zelix.tools.proguardConfigurationTranslate.commands.MergeInterfacesAggressively;
import com.zelix.tools.proguardConfigurationTranslate.commands.MicroEdition;
import com.zelix.tools.proguardConfigurationTranslate.commands.ObfuscationDictionary;
import com.zelix.tools.proguardConfigurationTranslate.commands.OptimizationPasses;
import com.zelix.tools.proguardConfigurationTranslate.commands.Optimizations;
import com.zelix.tools.proguardConfigurationTranslate.commands.OutJars;
import com.zelix.tools.proguardConfigurationTranslate.commands.OverloadAggressively;
import com.zelix.tools.proguardConfigurationTranslate.commands.PackageObfuscationDictionary;
import com.zelix.tools.proguardConfigurationTranslate.commands.PrintConfiguration;
import com.zelix.tools.proguardConfigurationTranslate.commands.PrintMapping;
import com.zelix.tools.proguardConfigurationTranslate.commands.PrintSeeds;
import com.zelix.tools.proguardConfigurationTranslate.commands.PrintUsage;
import com.zelix.tools.proguardConfigurationTranslate.commands.RenameSourceFileAttribute;
import com.zelix.tools.proguardConfigurationTranslate.commands.RepackageClasses;
import com.zelix.tools.proguardConfigurationTranslate.commands.SkipNonPublicLibraryClassMembers;
import com.zelix.tools.proguardConfigurationTranslate.commands.SkipNonPublicLibraryClasses;
import com.zelix.tools.proguardConfigurationTranslate.commands.Target;
import com.zelix.tools.proguardConfigurationTranslate.commands.UseUniqueClassMemberNames;
import com.zelix.tools.proguardConfigurationTranslate.commands.Verbose;
import com.zelix.tools.proguardConfigurationTranslate.commands.WhyAreYouKeeping;
import com.zelix.tools.proguardConfigurationTranslate.commands.ZipAlign;
import com.zelix.tools.proguardConfigurationTranslate.parser.AllFieldsNode;
import com.zelix.tools.proguardConfigurationTranslate.parser.AllMembersNode;
import com.zelix.tools.proguardConfigurationTranslate.parser.AllMethodsNode;
import com.zelix.tools.proguardConfigurationTranslate.parser.AnnotationPatternNode;
import com.zelix.tools.proguardConfigurationTranslate.parser.ArchiveFiltersNode;
import com.zelix.tools.proguardConfigurationTranslate.parser.AttributeNameNode;
import com.zelix.tools.proguardConfigurationTranslate.parser.ClassModifierNode;
import com.zelix.tools.proguardConfigurationTranslate.parser.ClassNameListNode;
import com.zelix.tools.proguardConfigurationTranslate.parser.ClassNameNode;
import com.zelix.tools.proguardConfigurationTranslate.parser.ClassPathEntryNode;
import com.zelix.tools.proguardConfigurationTranslate.parser.ClassPathFiltersNode;
import com.zelix.tools.proguardConfigurationTranslate.parser.ClassSpecification;
import com.zelix.tools.proguardConfigurationTranslate.parser.ClassTypeNode;
import com.zelix.tools.proguardConfigurationTranslate.parser.CommandStatement;
import com.zelix.tools.proguardConfigurationTranslate.parser.ExtendsClauseNode;
import com.zelix.tools.proguardConfigurationTranslate.parser.FieldSpecificationNode;
import com.zelix.tools.proguardConfigurationTranslate.parser.FieldTypePatternNode;
import com.zelix.tools.proguardConfigurationTranslate.parser.FileFiltersNode;
import com.zelix.tools.proguardConfigurationTranslate.parser.ImplementsClauseNode;
import com.zelix.tools.proguardConfigurationTranslate.parser.IntegerValueNode;
import com.zelix.tools.proguardConfigurationTranslate.parser.KeepModifiersNode;
import com.zelix.tools.proguardConfigurationTranslate.parser.MemberModifierNode;
import com.zelix.tools.proguardConfigurationTranslate.parser.MemberSpecificationNode;
import com.zelix.tools.proguardConfigurationTranslate.parser.MethodSpecificationNode;
import com.zelix.tools.proguardConfigurationTranslate.parser.NamePatternNode;
import com.zelix.tools.proguardConfigurationTranslate.parser.NegationNode;
import com.zelix.tools.proguardConfigurationTranslate.parser.ParameterListNode;
import com.zelix.tools.proguardConfigurationTranslate.parser.ParameterTypeNode;
import com.zelix.tools.proguardConfigurationTranslate.parser.ProguardConfigRootNode;
import com.zelix.tools.proguardConfigurationTranslate.parser.QuotedStringNode;
import com.zelix.tools.proguardConfigurationTranslate.parser.ReturnTypePatternNode;
import com.zelix.tools.proguardConfigurationTranslate.parser.TypePatternNode;
import com.zelix.utils.StringUtils;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

public class ProguardConfigParser implements ProguardCommands {
	private static int[] la1TokenBits4;
	private static int[] la1TokenBits0;
	private static int[] la1TokenBits2;
	private static int[] la1TokenBits1;
	private static int[] la1TokenBits3;
	protected LexicalBuffer2 lexicalBuffer = new LexicalBuffer2();
	private final int[] la1Generation = new int[71];
	private final LookaheadRecord[] lookaheadRecords = new LookaheadRecord[6];
	private boolean rescanning = false;
	private int lookaheadGcCounter = 0;
	private final TokenMgrError2 lookaheadSuccess = new TokenMgrError2();
	private List<int[]> expectedTokenSequences = new ArrayList<>();
	private int errorTokenKind = -1;
	private int[] errorTokenKinds = new int[100];
	public SimpleCharStream2 inputStream;
	public ProguardTokenManager tokenSource;
	public ProguardToken token;
	public ProguardToken nextToken;
	private int generationCounter;
	public int commandKind;
	private int lookaheadRemaining;
	private int errorTokenEndPos;
	private int[] errorTokenSequence;
	private ProguardToken scanPos;
	private ProguardToken lastPos;

	public ProguardConfigParser(Reader reader) {
		this.inputStream = new SimpleCharStream2(reader, 1, 1);
		this.tokenSource = new ProguardTokenManager(this.inputStream);
		this.token = new ProguardToken();
		this.token.next = this.nextToken = this.tokenSource.getNextToken();
		this.generationCounter = 0;

		for (int i = 0; i < 71; i++) {
			this.la1Generation[i] = -1;
		}

		for (int recordIndex = 0; recordIndex < this.lookaheadRecords.length; recordIndex++) {
			this.lookaheadRecords[recordIndex] = new LookaheadRecord();
		}
	}

	private static void initLa1TokenBits4() {
		la1TokenBits4 = new int[]{
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0
		};
	}

	private static void initLa1TokenBits2() {
		la1TokenBits2 = new int[]{
			0,
			-2361361,
			-2361361,
			0,
			0,
			0,
			2361344,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			16,
			0,
			0,
			16,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			16
		};
	}

	private static void initLa1TokenBits3() {
		la1TokenBits3 = new int[]{
			0,
			33554175,
			33554175,
			0,
			67108864,
			0,
			256,
			0,
			0,
			0,
			0,
			0,
			67108864,
			67108864,
			0,
			67108864,
			67108864,
			0,
			67108864,
			67108864,
			67108864,
			0,
			1610612736,
			67108864,
			0,
			67108864,
			0,
			67108864,
			0,
			67108864,
			0,
			67108864,
			1610612736,
			67108864,
			67108864,
			0,
			1610612736,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			-536870912,
			0,
			0,
			0,
			-536870912,
			-536870912,
			-536870912,
			-536870912,
			-536870912,
			0,
			0,
			0,
			-536870912,
			-536870912,
			-536870912,
			-536870912,
			0,
			0,
			1610612736,
			1610612736,
			0,
			0,
			0,
			0,
			0,
			0
		};
	}

	private static void initLa1TokenBits1() {
		la1TokenBits1 = new int[]{
			0,
			663282432,
			663282432,
			0,
			0,
			0,
			Integer.MIN_VALUE,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			1073741824,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			128,
			268435456,
			5708927,
			0,
			0,
			5573743,
			4096,
			0,
			0,
			0,
			131088,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			1073741824,
			16,
			0,
			136314880,
			0,
			4456451,
			0,
			5573743
		};
	}

	private static void initLa1TokenBits0() {
		la1TokenBits0 = new int[]{
			1024,
			1644167168,
			1644167168,
			4096,
			0,
			4096,
			0,
			4096,
			4096,
			4096,
			4096,
			4096,
			0,
			0,
			4096,
			0,
			0,
			4096,
			0,
			0,
			0,
			4096,
			65536,
			0,
			4096,
			0,
			4096,
			0,
			4096,
			0,
			4096,
			0,
			17367040,
			0,
			0,
			65536,
			65536,
			1048576,
			2048,
			1048576,
			4096,
			4096,
			16384,
			0,
			0,
			353189888,
			4194304,
			16384,
			268500992,
			84672512,
			84672512,
			84672512,
			218890240,
			84672512,
			16384,
			16384,
			4096,
			218890240,
			84672512,
			84672512,
			84672512,
			4096,
			65536,
			17301504,
			17301504,
			65536,
			-1879048192,
			65536,
			0,
			65536,
			268435456
		};
	}

	private void rescanToken() {
		this.rescanning = true;

		for (int recordIndex = 0; recordIndex < 6; recordIndex++) {
			try {
				LookaheadRecord lookaheadRecord = this.lookaheadRecords[recordIndex];

				while (true) {
					if (lookaheadRecord.tokenIndex > this.generationCounter) {
						this.lookaheadRemaining = lookaheadRecord.lookaheadLimit;
						this.lastPos = this.scanPos = lookaheadRecord.token;
						switch (recordIndex) {
							case 0:
								this.matchesArchiveFiltersExpansion();
								break;
							case 1:
								this.matchesClassModifierExpansion();
								break;
							case 2:
								this.matchesMethodSpecificationExpansion();
								break;
							case 3:
								this.matchesAllMembersStarSemicolon();
								break;
							case 4:
								this.matchesTypePatternThenFieldsKeyword();
								break;
							case 5:
								this.matchesReturnTypeThenMemberName();
						}
					}

					lookaheadRecord = lookaheadRecord.next;
					if (lookaheadRecord == null) {
						break;
					}
				}
			} catch (TokenMgrError2 ignored) {
			}
		}

		this.rescanning = false;
	}

	private boolean matchesMethodSpecWithReturnType() {
		if (this.isNextTypePattern()) {
			return true;
		} else {
			return this.isNextMemberName() ? true : this.scanToken(20);
		}
	}

	public final void parseAdaptResourceFileNames() throws ProguardCommandParseException {
		AdaptResourceFileNames commandNode = new AdaptResourceFileNames(53);
		boolean commitPending = true;
		this.lexicalBuffer.openNodeScope(commandNode);

		try {
			ProguardToken commandToken;
			commandToken = this.consumeToken(100);
			this.commandKind = 100;
			label77:
			switch (this.nextToken.kind) {
				case 122:
					this.parseStringLiteral();

					while (true) {
						switch (this.nextToken.kind) {
							case 12:
								this.consumeToken(12);
								this.parseStringLiteral();
								break;
							default:
								this.la1Generation[26] = this.generationCounter;
								break label77;
						}
					}
				default:
					this.la1Generation[27] = this.generationCounter;
			}

			this.lexicalBuffer.closeNodeScope(commandNode, true);
			commitPending = false;
			commandNode.setLineNumber(commandToken.beginLine);
		} catch (Throwable throwable) {
			if (commitPending) {
				this.lexicalBuffer.clearNodeScope(commandNode);
				commitPending = false;
			} else {
				this.lexicalBuffer.popNode();
			}

			if (throwable instanceof RuntimeException) {
				throw (RuntimeException)throwable;
			}

			if (throwable instanceof ProguardCommandParseException) {
				throw (ProguardCommandParseException)throwable;
			}

			throw (Error)throwable;
		} finally {
			if (commitPending) {
				this.lexicalBuffer.closeNodeScope(commandNode, true);
			}
		}
	}

	private boolean isNextTypePatternWildcardType() {
		return this.scanToken(127);
	}

	public final void parseAndroid() {
		Android commandNode = new Android(57);
		boolean commitPending = true;
		this.lexicalBuffer.openNodeScope(commandNode);

		try {
			ProguardToken commandToken = this.consumeToken(51);
			this.commandKind = 51;
			this.lexicalBuffer.closeNodeScope(commandNode, true);
			commitPending = false;
			commandNode.setLineNumber(commandToken.beginLine);
		} finally {
			if (commitPending) {
				this.lexicalBuffer.closeNodeScope(commandNode, true);
			}
		}
	}

	private boolean isNextFieldsTag() {
		return this.scanToken(44);
	}

	public final void parseAllMembers() {
		AllMembersNode commandNode = new AllMembersNode(76);
		this.lexicalBuffer.openNodeScope(commandNode);

		try {
			this.consumeToken(19);
		} finally {
			this.lexicalBuffer.closeNodeScope(commandNode, true);
		}
	}

	private boolean nextIsAllMembersWithSemicolon(int lookaheadDepth) {
		this.lookaheadRemaining = lookaheadDepth;
		this.lastPos = this.scanPos = this.token;

		try {
			return !this.matchesAllMembersStarSemicolon();
		} catch (TokenMgrError2 ignored) {
		} finally {
			this.saveLookaheadState(3, lookaheadDepth);
		}

		return true;
	}

	public final void parseKeepAttributes() {
		KeepAttributes commandNode = new KeepAttributes(48);
		boolean commitPending = true;
		this.lexicalBuffer.openNodeScope(commandNode);

		try {
			ProguardToken commandToken;
			commandToken = this.consumeToken(80);
			this.commandKind = 80;
			label77:
			switch (this.nextToken.kind) {
				case 16:
				case 125:
				case 126:
					this.parseAttributeName();

					while (true) {
						switch (this.nextToken.kind) {
							case 12:
								this.consumeToken(12);
								this.parseAttributeName();
								break;
							default:
								this.la1Generation[21] = this.generationCounter;
								break label77;
						}
					}
				default:
					this.la1Generation[22] = this.generationCounter;
			}

			this.lexicalBuffer.closeNodeScope(commandNode, true);
			commitPending = false;
			commandNode.setLineNumber(commandToken.beginLine);
		} catch (Throwable throwable) {
			if (commitPending) {
				this.lexicalBuffer.clearNodeScope(commandNode);
				commitPending = false;
			} else {
				this.lexicalBuffer.popNode();
			}

			if (throwable instanceof RuntimeException) {
				throw (RuntimeException)throwable;
			}

			if (throwable instanceof ProguardCommandParseException) {
				throw (ProguardCommandParseException)throwable;
			}

			throw (Error)throwable;
		} finally {
			if (commitPending) {
				this.lexicalBuffer.closeNodeScope(commandNode, true);
			}
		}
	}

	private boolean isNextCommaThenStringLiteral() {
		return this.scanToken(12) ? true : this.isNextStringLiteral();
	}

	public final void parseTypePattern() {
		TypePatternNode commandNode = new TypePatternNode(85);
		boolean commitPending = true;
		this.lexicalBuffer.openNodeScope(commandNode);

		try {
			switch (this.nextToken.kind) {
				case 18:
					ProguardToken percentToken = this.consumeToken(18);
					this.lexicalBuffer.closeNodeScope(commandNode, true);
					commitPending = false;
					commandNode.setValue(percentToken.image);
					break;
				case 19:
					ProguardToken starToken = this.consumeToken(19);
					this.lexicalBuffer.closeNodeScope(commandNode, true);
					commitPending = false;
					commandNode.setValue(starToken.image);
					break;
				case 24:
					ProguardToken doubleStarToken = this.consumeToken(24);
					this.lexicalBuffer.closeNodeScope(commandNode, true);
					commitPending = false;
					commandNode.setValue(doubleStarToken.image);
					break;
				case 26:
					ProguardToken tripleStarToken = this.consumeToken(26);
					this.lexicalBuffer.closeNodeScope(commandNode, true);
					commitPending = false;
					commandNode.setValue(tripleStarToken.image);
					break;
				case 125:
					ProguardToken nameToken = this.consumeToken(125);
					this.lexicalBuffer.closeNodeScope(commandNode, true);
					commitPending = false;
					commandNode.setValue(nameToken.image);
					break;
				case 126:
					ProguardToken wildcardNameToken = this.consumeToken(126);
					this.lexicalBuffer.closeNodeScope(commandNode, true);
					commitPending = false;
					commandNode.setValue(wildcardNameToken.image);
					break;
				case 127:
					ProguardToken wildcardTypeToken = this.consumeToken(127);
					this.lexicalBuffer.closeNodeScope(commandNode, true);
					commitPending = false;
					commandNode.setValue(wildcardTypeToken.image);
					break;
				default:
					this.la1Generation[58] = this.generationCounter;
					this.consumeToken(-1);
					throw new ProguardCommandParseException();
			}
		} finally {
			if (commitPending) {
				this.lexicalBuffer.closeNodeScope(commandNode, true);
			}
		}
	}

	public final void parseVerbose() {
		Verbose commandNode = new Verbose(58);
		boolean commitPending = true;
		this.lexicalBuffer.openNodeScope(commandNode);

		try {
			ProguardToken commandToken = this.consumeToken(45);
			this.commandKind = 45;
			this.lexicalBuffer.closeNodeScope(commandNode, true);
			commitPending = false;
			commandNode.setLineNumber(commandToken.beginLine);
		} finally {
			if (commitPending) {
				this.lexicalBuffer.closeNodeScope(commandNode, true);
			}
		}
	}

	public final void parseCommand() throws ProguardCommandParseException {
		CommandStatement commandNode = new CommandStatement(1);
		boolean commitPending = true;
		this.lexicalBuffer.openNodeScope(commandNode);
		boolean exceptionThrown = false;

		try {
			exceptionThrown = true;
			switch (this.nextToken.kind) {
				case 25:
					this.parseIf();
					exceptionThrown = false;
					break;
				case 26:
				case 27:
				case 28:
				case 31:
				case 32:
				case 33:
				case 34:
				case 35:
				case 36:
				case 37:
				case 38:
				case 39:
				case 42:
				case 43:
				case 44:
				case 48:
				case 49:
				case 50:
				case 52:
				case 53:
				case 54:
				case 59:
				case 60:
				case 62:
				case 63:
				case 68:
				case 75:
				case 82:
				case 85:
				case 104:
				default:
					this.la1Generation[2] = this.generationCounter;
					this.consumeToken(-1);
					throw new ProguardCommandParseException();
				case 29:
					this.parseDump();
					exceptionThrown = false;
					break;
				case 30:
					this.parseKeep();
					exceptionThrown = false;
					break;
				case 40:
					this.parseTarget();
					exceptionThrown = false;
					break;
				case 41:
					this.parseInJars();
					exceptionThrown = false;
					break;
				case 45:
					this.parseVerbose();
					exceptionThrown = false;
					break;
				case 46:
					this.parseInclude();
					exceptionThrown = false;
					break;
				case 47:
					this.parseOutJars();
					exceptionThrown = false;
					break;
				case 51:
					this.parseAndroid();
					exceptionThrown = false;
					break;
				case 55:
					this.parseKeepCode();
					exceptionThrown = false;
					break;
				case 56:
					this.parseZipAlign();
					exceptionThrown = false;
					break;
				case 57:
					this.parseDontNote();
					exceptionThrown = false;
					break;
				case 58:
					this.parseDontWarn();
					exceptionThrown = false;
					break;
				case 61:
					this.parseKeepNames();
					exceptionThrown = false;
					break;
				case 64:
					this.parsePrintSeeds();
					exceptionThrown = false;
					break;
				case 65:
					this.parseDontShrink();
					exceptionThrown = false;
					break;
				case 66:
					this.parsePrintUsage();
					exceptionThrown = false;
					break;
				case 67:
					this.parseLibraryJars();
					exceptionThrown = false;
					break;
				case 69:
					this.parseApplyMapping();
					exceptionThrown = false;
					break;
				case 70:
					this.parseMicroEdition();
					exceptionThrown = false;
					break;
				case 71:
					this.parsePrintMappings();
					exceptionThrown = false;
					break;
				case 72:
					this.parseAssumeValues();
					exceptionThrown = false;
					break;
				case 73:
					this.parseDontOptimize();
					exceptionThrown = false;
					break;
				case 74:
					this.parseDontCompress();
					exceptionThrown = false;
					break;
				case 76:
					this.parseOptimizations();
					exceptionThrown = false;
					break;
				case 77:
					this.parseDontPreVerify();
					exceptionThrown = false;
					break;
				case 78:
					this.parseBaseDirectory();
					exceptionThrown = false;
					break;
				case 79:
					this.parseDontObfuscate();
					exceptionThrown = false;
					break;
				case 80:
					this.parseKeepAttributes();
					exceptionThrown = false;
					break;
				case 81:
					this.parseIgnoreWarnings();
					exceptionThrown = false;
					break;
				case 83:
					this.parseKeepDirectories();
					exceptionThrown = false;
					break;
				case 84:
					this.parseForceProcessing();
					exceptionThrown = false;
					break;
				case 86:
					this.parseKeepClassMembers();
					exceptionThrown = false;
					break;
				case 87:
					this.parseKeepPackageNames();
					exceptionThrown = false;
					break;
				case 88:
					this.parseRepackageClasses();
					exceptionThrown = false;
					break;
				case 89:
					this.parseWhyAreYouKeeping();
					exceptionThrown = false;
					break;
				case 90:
					this.parseAdaptClassStrings();
					exceptionThrown = false;
					break;
				case 91:
					this.parseKeepKotlinMetadata();
					exceptionThrown = false;
					break;
				case 92:
					this.parsePrintConfiguration();
					exceptionThrown = false;
					break;
				case 93:
					this.parseKeepParameterNames();
					exceptionThrown = false;
					break;
				case 94:
					this.parseOptimizationPasses();
					exceptionThrown = false;
					break;
				case 95:
					this.parseAdaptKotlinMetadata();
					exceptionThrown = false;
					break;
				case 96:
					this.parseAssumeNoSideEffects();
					exceptionThrown = false;
					break;
				case 97:
					this.parseOverloadAggressively();
					exceptionThrown = false;
					break;
				case 98:
					this.parseKeepClassMemberNames();
					exceptionThrown = false;
					break;
				case 99:
					this.parseObfuscationDictionary();
					exceptionThrown = false;
					break;
				case 100:
					this.parseAdaptResourceFileNames();
					exceptionThrown = false;
					break;
				case 101:
					this.parseKeepClassesWithMembers();
					exceptionThrown = false;
					break;
				case 102:
					this.parseFlattenPackageHierarchy();
					exceptionThrown = false;
					break;
				case 103:
					this.parseAllowAccessModification();
					exceptionThrown = false;
					break;
				case 105:
					this.parseDontProcessKotlinMetadata();
					exceptionThrown = false;
					break;
				case 106:
					this.parseAddConfigurationDebugging();
					exceptionThrown = false;
					break;
				case 107:
					this.parseRenameSourceFileAttribute();
					exceptionThrown = false;
					break;
				case 108:
					this.parseAdaptResourceFileContents();
					exceptionThrown = false;
					break;
				case 109:
					this.parseUseUniqueClassMemberNames();
					exceptionThrown = false;
					break;
				case 110:
					this.parseAssumeNoEscapingParameters();
					exceptionThrown = false;
					break;
				case 111:
					this.parseDontUseMixedCaseClassNames();
					exceptionThrown = false;
					break;
				case 112:
					this.parseKeepClassesWithMemberNames();
					exceptionThrown = false;
					break;
				case 113:
					this.parseClassObfuscationDictionary();
					exceptionThrown = false;
					break;
				case 114:
					this.parseAssumeNoExternalSideEffect();
					exceptionThrown = false;
					break;
				case 115:
					this.parseSkipNonPublicLibraryClasses();
					exceptionThrown = false;
					break;
				case 116:
					this.parseMergeInterfacesAggressively();
					exceptionThrown = false;
					break;
				case 117:
					this.parseAssumeNoExternalReturnValues();
					exceptionThrown = false;
					break;
				case 118:
					this.parsePackageObfuscationDictionary();
					exceptionThrown = false;
					break;
				case 119:
					this.parseDontSkipNonPublicLibraryClasses();
					exceptionThrown = false;
					break;
				case 120:
					this.parseDontSkipNonPublicLibraryClassMembers();
					exceptionThrown = false;
			}
		} catch (Throwable throwable) {
			if (true) {
				this.lexicalBuffer.clearNodeScope(commandNode);
				commitPending = false;
			} else {
				this.lexicalBuffer.popNode();
			}
			if (throwable instanceof RuntimeException) {
				throw (RuntimeException)throwable;
			}

			if (throwable instanceof ProguardCommandParseException) {
				throw (ProguardCommandParseException)throwable;
			}

			throw (Error)throwable;
		} finally {
			if (exceptionThrown && commitPending) {
				this.lexicalBuffer.closeNodeScope(commandNode, true);
			}
		}

		this.lexicalBuffer.closeNodeScope(commandNode, true);
	}

	public final void parseParameterPattern() {
		ParameterTypeNode commandNode = new ParameterTypeNode(84);
		boolean commitPending = true;
		this.lexicalBuffer.openNodeScope(commandNode);

		try {
			switch (this.nextToken.kind) {
				case 18:
					ProguardToken percentToken = this.consumeToken(18);
					this.lexicalBuffer.closeNodeScope(commandNode, true);
					commitPending = false;
					commandNode.setValue(percentToken.image);
					break;
				case 19:
					ProguardToken starToken = this.consumeToken(19);
					this.lexicalBuffer.closeNodeScope(commandNode, true);
					commitPending = false;
					commandNode.setValue(starToken.image);
					break;
				case 24:
					ProguardToken doubleStarToken = this.consumeToken(24);
					this.lexicalBuffer.closeNodeScope(commandNode, true);
					commitPending = false;
					commandNode.setValue(doubleStarToken.image);
					break;
				case 26:
					ProguardToken tripleStarToken = this.consumeToken(26);
					this.lexicalBuffer.closeNodeScope(commandNode, true);
					commitPending = false;
					commandNode.setValue(tripleStarToken.image);
					break;
				case 27:
					ProguardToken ellipsisToken = this.consumeToken(27);
					this.lexicalBuffer.closeNodeScope(commandNode, true);
					commitPending = false;
					commandNode.setValue(ellipsisToken.image);
					break;
				case 125:
					ProguardToken nameToken = this.consumeToken(125);
					this.lexicalBuffer.closeNodeScope(commandNode, true);
					commitPending = false;
					commandNode.setValue(nameToken.image);
					break;
				case 126:
					ProguardToken wildcardNameToken = this.consumeToken(126);
					this.lexicalBuffer.closeNodeScope(commandNode, true);
					commitPending = false;
					commandNode.setValue(wildcardNameToken.image);
					break;
				case 127:
					ProguardToken wildcardTypeToken = this.consumeToken(127);
					this.lexicalBuffer.closeNodeScope(commandNode, true);
					commitPending = false;
					commandNode.setValue(wildcardTypeToken.image);
					break;
				default:
					this.la1Generation[57] = this.generationCounter;
					this.consumeToken(-1);
					throw new ProguardCommandParseException();
			}
		} catch (ProguardCommandParseException parseException) {
			throw new RuntimeException(parseException);
		} finally {
			if (commitPending) {
				this.lexicalBuffer.closeNodeScope(commandNode, true);
			}
		}
	}

	public final void parseAddConfigurationDebugging() {
		AddConfigurationDebugging commandNode = new AddConfigurationDebugging(33);
		boolean commitPending = true;
		this.lexicalBuffer.openNodeScope(commandNode);

		try {
			ProguardToken commandToken = this.consumeToken(106);
			this.commandKind = 106;
			this.lexicalBuffer.closeNodeScope(commandNode, true);
			commitPending = false;
			commandNode.setLineNumber(commandToken.beginLine);
		} finally {
			if (commitPending) {
				this.lexicalBuffer.closeNodeScope(commandNode, true);
			}
		}
	}

	public final void parseDontShrink() {
		DontShrink commandNode = new DontShrink(22);
		boolean commitPending = true;
		this.lexicalBuffer.openNodeScope(commandNode);

		try {
			ProguardToken commandToken = this.consumeToken(65);
			this.commandKind = 65;
			this.lexicalBuffer.closeNodeScope(commandNode, true);
			commitPending = false;
			commandNode.setLineNumber(commandToken.beginLine);
		} finally {
			if (commitPending) {
				this.lexicalBuffer.closeNodeScope(commandNode, true);
			}
		}
	}

	public final void parseDontNote() {
		DontNote commandNode = new DontNote(59);
		boolean commitPending = true;
		this.lexicalBuffer.openNodeScope(commandNode);

		try {
			ProguardToken commandToken;
			commandToken = this.consumeToken(57);
			this.commandKind = 57;
			label77:
			switch (this.nextToken.kind) {
				case 122:
					this.parseStringLiteral();

					while (true) {
						switch (this.nextToken.kind) {
							case 12:
								this.consumeToken(12);
								this.parseStringLiteral();
								break;
							default:
								this.la1Generation[30] = this.generationCounter;
								break label77;
						}
					}
				default:
					this.la1Generation[31] = this.generationCounter;
			}

			this.lexicalBuffer.closeNodeScope(commandNode, true);
			commitPending = false;
			commandNode.setLineNumber(commandToken.beginLine);
		} catch (Throwable throwable) {
			if (commitPending) {
				this.lexicalBuffer.clearNodeScope(commandNode);
				commitPending = false;
			} else {
				this.lexicalBuffer.popNode();
			}

			if (throwable instanceof RuntimeException) {
				throw (RuntimeException)throwable;
			}

			if (throwable instanceof ProguardCommandParseException) {
				throw (ProguardCommandParseException)throwable;
			}

			throw (Error)throwable;
		} finally {
			if (commitPending) {
				this.lexicalBuffer.closeNodeScope(commandNode, true);
			}
		}
	}

	public final void parseKeepModifiers() {
		KeepModifiersNode commandNode = new KeepModifiersNode(15);
		boolean commitPending = true;
		this.lexicalBuffer.openNodeScope(commandNode);

		try {
			switch (this.nextToken.kind) {
				case 63:
					ProguardToken includeCodeToken = this.consumeToken(63);
					this.lexicalBuffer.closeNodeScope(commandNode, true);
					commitPending = false;
					commandNode.setValue(includeCodeToken.image);
					break;
				case 75:
					ProguardToken allowShrinkingToken = this.consumeToken(75);
					this.lexicalBuffer.closeNodeScope(commandNode, true);
					commitPending = false;
					commandNode.setValue(allowShrinkingToken.image);
					break;
				case 82:
					ProguardToken allowObfuscationToken = this.consumeToken(82);
					this.lexicalBuffer.closeNodeScope(commandNode, true);
					commitPending = false;
					commandNode.setValue(allowObfuscationToken.image);
					break;
				case 85:
					ProguardToken allowOptimizationToken = this.consumeToken(85);
					this.lexicalBuffer.closeNodeScope(commandNode, true);
					commitPending = false;
					commandNode.setValue(allowOptimizationToken.image);
					break;
				case 104:
					ProguardToken includeDescriptorClassesToken = this.consumeToken(104);
					this.lexicalBuffer.closeNodeScope(commandNode, true);
					commitPending = false;
					commandNode.setValue(includeDescriptorClassesToken.image);
					break;
				default:
					this.la1Generation[6] = this.generationCounter;
					this.consumeToken(-1);
					throw new ProguardCommandParseException();
			}
		} finally {
			if (commitPending) {
				this.lexicalBuffer.closeNodeScope(commandNode, true);
			}
		}
	}

	public final void parseFlattenPackageHierarchy() {
		FlattenPackageHierarchy commandNode = new FlattenPackageHierarchy(46);
		boolean commitPending = true;
		this.lexicalBuffer.openNodeScope(commandNode);

		try {
			ProguardToken commandToken = this.consumeToken(102);
			this.commandKind = 102;
			switch (this.nextToken.kind) {
				case 122:
					this.parseStringLiteral();
					break;
				default:
					this.la1Generation[19] = this.generationCounter;
			}

			this.lexicalBuffer.closeNodeScope(commandNode, true);
			commitPending = false;
			commandNode.setLineNumber(commandToken.beginLine);
		} catch (Throwable throwable) {
			if (commitPending) {
				this.lexicalBuffer.clearNodeScope(commandNode);
				commitPending = false;
			} else {
				this.lexicalBuffer.popNode();
			}

			if (throwable instanceof RuntimeException) {
				throw (RuntimeException)throwable;
			}

			if (throwable instanceof ProguardCommandParseException) {
				throw (ProguardCommandParseException)throwable;
			}

			throw (Error)throwable;
		} finally {
			if (commitPending) {
				this.lexicalBuffer.closeNodeScope(commandNode, true);
			}
		}
	}

	public final void parseRepackageClasses() {
		RepackageClasses commandNode = new RepackageClasses(47);
		boolean commitPending = true;
		this.lexicalBuffer.openNodeScope(commandNode);

		try {
			ProguardToken commandToken = this.consumeToken(88);
			this.commandKind = 88;
			switch (this.nextToken.kind) {
				case 122:
					this.parseStringLiteral();
					break;
				default:
					this.la1Generation[20] = this.generationCounter;
			}

			this.lexicalBuffer.closeNodeScope(commandNode, true);
			commitPending = false;
			commandNode.setLineNumber(commandToken.beginLine);
		} catch (Throwable throwable) {
			if (commitPending) {
				this.lexicalBuffer.clearNodeScope(commandNode);
				commitPending = false;
			} else {
				this.lexicalBuffer.popNode();
			}

			if (throwable instanceof RuntimeException) {
				throw (RuntimeException)throwable;
			}

			if (throwable instanceof ProguardCommandParseException) {
				throw (ProguardCommandParseException)throwable;
			}

			throw (Error)throwable;
		} finally {
			if (commitPending) {
				this.lexicalBuffer.closeNodeScope(commandNode, true);
			}
		}
	}

	public final void parseDump() {
		Dump commandNode = new Dump(63);
		boolean commitPending = true;
		this.lexicalBuffer.openNodeScope(commandNode);

		try {
			ProguardToken commandToken = this.consumeToken(29);
			this.commandKind = 29;
			switch (this.nextToken.kind) {
				case 122:
					this.parseStringLiteral();
					break;
				default:
					this.la1Generation[34] = this.generationCounter;
			}

			this.lexicalBuffer.closeNodeScope(commandNode, true);
			commitPending = false;
			commandNode.setLineNumber(commandToken.beginLine);
		} catch (Throwable throwable) {
			if (commitPending) {
				this.lexicalBuffer.clearNodeScope(commandNode);
				commitPending = false;
			} else {
				this.lexicalBuffer.popNode();
			}

			if (throwable instanceof RuntimeException) {
				throw (RuntimeException)throwable;
			}

			if (throwable instanceof ProguardCommandParseException) {
				throw (ProguardCommandParseException)throwable;
			}

			throw (Error)throwable;
		} finally {
			if (commitPending) {
				this.lexicalBuffer.closeNodeScope(commandNode, true);
			}
		}
	}

	public final void parseAssumeNoExternalReturnValues() {
		AssumeNoExternalReturnValues commandNode = new AssumeNoExternalReturnValues(31);
		boolean commitPending = true;
		this.lexicalBuffer.openNodeScope(commandNode);

		try {
			ProguardToken commandToken = this.consumeToken(117);
			this.commandKind = 117;
			this.parseClassSpecification();
			this.lexicalBuffer.closeNodeScope(commandNode, true);
			commitPending = false;
			commandNode.setLineNumber(commandToken.beginLine);
		} catch (Throwable throwable) {
			if (commitPending) {
				this.lexicalBuffer.clearNodeScope(commandNode);
				commitPending = false;
			} else {
				this.lexicalBuffer.popNode();
			}

			if (throwable instanceof RuntimeException) {
				throw (RuntimeException)throwable;
			}

			if (throwable instanceof ProguardCommandParseException) {
				throw (ProguardCommandParseException)throwable;
			}

			throw (Error)throwable;
		} finally {
			if (commitPending) {
				this.lexicalBuffer.closeNodeScope(commandNode, true);
			}
		}
	}

	public final void parseNegation() {
		NegationNode commandNode = new NegationNode(79);
		this.lexicalBuffer.openNodeScope(commandNode);

		try {
			this.consumeToken(16);
		} finally {
			this.lexicalBuffer.closeNodeScope(commandNode, true);
		}
	}

	public final void parseDontWarn() {
		DontWarn commandNode = new DontWarn(60);
		boolean commitPending = true;
		this.lexicalBuffer.openNodeScope(commandNode);

		try {
			ProguardToken commandToken = this.consumeToken(58);
			this.commandKind = 58;
			switch (this.nextToken.kind) {
				case 16:
				case 19:
				case 24:
				case 62:
				case 125:
				case 126:
					this.parseClassNameList();
					break;
				default:
					this.la1Generation[32] = this.generationCounter;
			}

			this.lexicalBuffer.closeNodeScope(commandNode, true);
			commitPending = false;
			commandNode.setLineNumber(commandToken.beginLine);
		} catch (Throwable throwable) {
			if (commitPending) {
				this.lexicalBuffer.clearNodeScope(commandNode);
				commitPending = false;
			} else {
				this.lexicalBuffer.popNode();
			}

			if (throwable instanceof RuntimeException) {
				throw (RuntimeException)throwable;
			}

			if (throwable instanceof ProguardCommandParseException) {
				throw (ProguardCommandParseException)throwable;
			}

			throw (Error)throwable;
		} finally {
			if (commitPending) {
				this.lexicalBuffer.closeNodeScope(commandNode, true);
			}
		}
	}

	public final void parseRenameSourceFileAttribute() {
		RenameSourceFileAttribute commandNode = new RenameSourceFileAttribute(50);
		boolean commitPending = true;
		this.lexicalBuffer.openNodeScope(commandNode);

		try {
			ProguardToken commandToken = this.consumeToken(107);
			this.commandKind = 107;
			switch (this.nextToken.kind) {
				case 122:
					this.parseStringLiteral();
					break;
				default:
					this.la1Generation[23] = this.generationCounter;
			}

			this.lexicalBuffer.closeNodeScope(commandNode, true);
			commitPending = false;
			commandNode.setLineNumber(commandToken.beginLine);
		} catch (Throwable throwable) {
			if (commitPending) {
				this.lexicalBuffer.clearNodeScope(commandNode);
				commitPending = false;
			} else {
				this.lexicalBuffer.popNode();
			}

			if (throwable instanceof RuntimeException) {
				throw (RuntimeException)throwable;
			}

			if (throwable instanceof ProguardCommandParseException) {
				throw (ProguardCommandParseException)throwable;
			}

			throw (Error)throwable;
		} finally {
			if (commitPending) {
				this.lexicalBuffer.closeNodeScope(commandNode, true);
			}
		}
	}

	public final void parseDontCompress() {
		DontCompress commandNode = new DontCompress(52);
		boolean commitPending = true;
		this.lexicalBuffer.openNodeScope(commandNode);

		try {
			ProguardToken commandToken = this.consumeToken(74);
			this.commandKind = 74;
			this.lexicalBuffer.closeNodeScope(commandNode, true);
			commitPending = false;
			commandNode.setLineNumber(commandToken.beginLine);
		} finally {
			if (commitPending) {
				this.lexicalBuffer.closeNodeScope(commandNode, true);
			}
		}
	}

	public final void parseKeep() {
		Keep commandNode = new Keep(14);
		boolean commitPending = true;
		this.lexicalBuffer.openNodeScope(commandNode);

		try {
			ProguardToken commandToken = this.consumeToken(30);
			this.commandKind = 30;

			while (true) {
				switch (this.nextToken.kind) {
					case 12:
						this.consumeToken(12);
						this.parseKeepModifiers();
						break;
					default:
						this.la1Generation[5] = this.generationCounter;
						this.parseClassSpecification();
						this.lexicalBuffer.closeNodeScope(commandNode, true);
						commitPending = false;
						commandNode.setLineNumber(commandToken.beginLine);
						return;
				}
			}
		} catch (Throwable throwable) {
			if (commitPending) {
				this.lexicalBuffer.clearNodeScope(commandNode);
				commitPending = false;
			} else {
				this.lexicalBuffer.popNode();
			}

			if (throwable instanceof RuntimeException) {
				throw (RuntimeException)throwable;
			} else if (throwable instanceof ProguardCommandParseException) {
				throw (ProguardCommandParseException)throwable;
			} else {
				throw (Error)throwable;
			}
		} finally {
			if (commitPending) {
				this.lexicalBuffer.closeNodeScope(commandNode, true);
			}
		}
	}

	public final void parseFileFilters() throws ProguardCommandParseException {
		FileFiltersNode commandNode = new FileFiltersNode(73);
		boolean commitPending = true;
		this.lexicalBuffer.openNodeScope(commandNode);
		boolean exceptionThrown = false;

		try {
			exceptionThrown = true;
			this.parseStringLiteral();

			label71:
			while (true) {
				switch (this.nextToken.kind) {
					case 12:
						this.consumeToken(12);
						this.parseStringLiteral();
						break;
					default:
						this.la1Generation[41] = this.generationCounter;
						exceptionThrown = false;
						break label71;
				}
			}
		} catch (Throwable throwable) {
			if (true) {
				this.lexicalBuffer.clearNodeScope(commandNode);
				commitPending = false;
			} else {
				this.lexicalBuffer.popNode();
			}
			if (throwable instanceof RuntimeException) {
				throw (RuntimeException)throwable;
			}

			if (throwable instanceof ProguardCommandParseException) {
				throw (ProguardCommandParseException)throwable;
			}

			throw (Error)throwable;
		} finally {
			if (exceptionThrown && commitPending) {
				this.lexicalBuffer.closeNodeScope(commandNode, true);
			}
		}

		this.lexicalBuffer.closeNodeScope(commandNode, true);
	}

	public final void parseIf() {
		If commandNode = new If(13);
		boolean commitPending = true;
		this.lexicalBuffer.openNodeScope(commandNode);

		try {
			ProguardToken commandToken = this.consumeToken(25);
			this.commandKind = 25;
			this.parseClassSpecification();
			this.lexicalBuffer.closeNodeScope(commandNode, true);
			commitPending = false;
			commandNode.setLineNumber(commandToken.beginLine);
		} catch (Throwable throwable) {
			if (commitPending) {
				this.lexicalBuffer.clearNodeScope(commandNode);
				commitPending = false;
			} else {
				this.lexicalBuffer.popNode();
			}

			if (throwable instanceof RuntimeException) {
				throw (RuntimeException)throwable;
			}

			if (throwable instanceof ProguardCommandParseException) {
				throw (ProguardCommandParseException)throwable;
			}

			throw (Error)throwable;
		} finally {
			if (commitPending) {
				this.lexicalBuffer.closeNodeScope(commandNode, true);
			}
		}
	}

	public final void parseIgnoreWarnings() {
		IgnoreWarnings commandNode = new IgnoreWarnings(61);
		boolean commitPending = true;
		this.lexicalBuffer.openNodeScope(commandNode);

		try {
			ProguardToken commandToken = this.consumeToken(81);
			this.commandKind = 81;
			this.lexicalBuffer.closeNodeScope(commandNode, true);
			commitPending = false;
			commandNode.setLineNumber(commandToken.beginLine);
		} finally {
			if (commitPending) {
				this.lexicalBuffer.closeNodeScope(commandNode, true);
			}
		}
	}

	public final void parseAttributeName() {
		AttributeNameNode commandNode = new AttributeNameNode(69);
		boolean commitPending = true;
		this.lexicalBuffer.openNodeScope(commandNode);

		try {
			switch (this.nextToken.kind) {
				case 16:
				case 125:
					switch (this.nextToken.kind) {
						case 16:
							this.parseNegation();
							break;
						default:
							this.la1Generation[35] = this.generationCounter;
					}

					ProguardToken nameToken = this.consumeToken(125);
					this.lexicalBuffer.closeNodeScope(commandNode, true);
					commitPending = false;
					commandNode.setValue(nameToken.image);
					break;
				case 126:
					ProguardToken wildcardNameToken = this.consumeToken(126);
					this.lexicalBuffer.closeNodeScope(commandNode, true);
					commitPending = false;
					commandNode.setValue(wildcardNameToken.image);
					break;
				default:
					this.la1Generation[36] = this.generationCounter;
					this.consumeToken(-1);
					throw new ProguardCommandParseException();
			}
		} catch (Throwable throwable) {
			if (commitPending) {
				this.lexicalBuffer.clearNodeScope(commandNode);
				commitPending = false;
			} else {
				this.lexicalBuffer.popNode();
			}

			if (throwable instanceof RuntimeException) {
				throw (RuntimeException)throwable;
			}

			if (throwable instanceof ProguardCommandParseException) {
				throw (ProguardCommandParseException)throwable;
			}

			throw (Error)throwable;
		} finally {
			if (commitPending) {
				this.lexicalBuffer.closeNodeScope(commandNode, true);
			}
		}
	}

	public final void parseKeepNames() {
		KeepNames commandNode = new KeepNames(18);
		boolean commitPending = true;
		this.lexicalBuffer.openNodeScope(commandNode);

		try {
			ProguardToken commandToken = this.consumeToken(61);
			this.commandKind = 61;

			while (true) {
				switch (this.nextToken.kind) {
					case 12:
						this.consumeToken(12);
						this.parseKeepModifiers();
						break;
					default:
						this.la1Generation[9] = this.generationCounter;
						this.parseClassSpecification();
						this.lexicalBuffer.closeNodeScope(commandNode, true);
						commitPending = false;
						commandNode.setLineNumber(commandToken.beginLine);
						return;
				}
			}
		} catch (Throwable throwable) {
			if (commitPending) {
				this.lexicalBuffer.clearNodeScope(commandNode);
				commitPending = false;
			} else {
				this.lexicalBuffer.popNode();
			}

			if (throwable instanceof RuntimeException) {
				throw (RuntimeException)throwable;
			} else if (throwable instanceof ProguardCommandParseException) {
				throw (ProguardCommandParseException)throwable;
			} else {
				throw (Error)throwable;
			}
		} finally {
			if (commitPending) {
				this.lexicalBuffer.closeNodeScope(commandNode, true);
			}
		}
	}

	public final void parseAssumeNoExternalSideEffect() {
		AssumeNoExternalSideEffect commandNode = new AssumeNoExternalSideEffect(32);
		boolean commitPending = true;
		this.lexicalBuffer.openNodeScope(commandNode);

		try {
			ProguardToken commandToken = this.consumeToken(114);
			this.commandKind = 114;
			this.parseClassSpecification();
			this.lexicalBuffer.closeNodeScope(commandNode, true);
			commitPending = false;
			commandNode.setLineNumber(commandToken.beginLine);
		} catch (Throwable throwable) {
			if (commitPending) {
				this.lexicalBuffer.clearNodeScope(commandNode);
				commitPending = false;
			} else {
				this.lexicalBuffer.popNode();
			}

			if (throwable instanceof RuntimeException) {
				throw (RuntimeException)throwable;
			}

			if (throwable instanceof ProguardCommandParseException) {
				throw (ProguardCommandParseException)throwable;
			}

			throw (Error)throwable;
		} finally {
			if (commitPending) {
				this.lexicalBuffer.closeNodeScope(commandNode, true);
			}
		}
	}

	public final void parseZipAlign() {
		ZipAlign commandNode = new ZipAlign(66);
		boolean commitPending = true;
		this.lexicalBuffer.openNodeScope(commandNode);

		try {
			ProguardToken commandToken = this.consumeToken(56);
			this.commandKind = 56;
			this.parseIntegerLiteral();
			this.lexicalBuffer.closeNodeScope(commandNode, true);
			commitPending = false;
			commandNode.setLineNumber(commandToken.beginLine);
		} catch (Throwable throwable) {
			if (commitPending) {
				this.lexicalBuffer.clearNodeScope(commandNode);
				commitPending = false;
			} else {
				this.lexicalBuffer.popNode();
			}

			if (throwable instanceof RuntimeException) {
				throw (RuntimeException)throwable;
			}

			if (throwable instanceof ProguardCommandParseException) {
				throw (ProguardCommandParseException)throwable;
			}

			throw (Error)throwable;
		} finally {
			if (commitPending) {
				this.lexicalBuffer.closeNodeScope(commandNode, true);
			}
		}
	}

	public final void parseKeepParameterNames() {
		KeepParameterNames commandNode = new KeepParameterNames(49);
		boolean commitPending = true;
		this.lexicalBuffer.openNodeScope(commandNode);

		try {
			ProguardToken commandToken = this.consumeToken(93);
			this.commandKind = 93;
			this.lexicalBuffer.closeNodeScope(commandNode, true);
			commitPending = false;
			commandNode.setLineNumber(commandToken.beginLine);
		} finally {
			if (commitPending) {
				this.lexicalBuffer.closeNodeScope(commandNode, true);
			}
		}
	}

	public final void parseAdaptKotlinMetadata() {
		AdaptKotlinMetadata commandNode = new AdaptKotlinMetadata(26);
		boolean commitPending = true;
		this.lexicalBuffer.openNodeScope(commandNode);

		try {
			ProguardToken commandToken = this.consumeToken(95);
			this.commandKind = 95;
			this.lexicalBuffer.closeNodeScope(commandNode, true);
			commitPending = false;
			commandNode.setLineNumber(commandToken.beginLine);
		} finally {
			if (commitPending) {
				this.lexicalBuffer.closeNodeScope(commandNode, true);
			}
		}
	}

	public final void parseClassPathEntry() {
		ClassPathEntryNode commandNode = new ClassPathEntryNode(70);
		boolean commitPending = true;
		this.lexicalBuffer.openNodeScope(commandNode);
		boolean exceptionThrown = false;

		try {
			exceptionThrown = true;
			this.parseStringLiteral();
			switch (this.nextToken.kind) {
				case 20:
					this.parseClassPathFilters();
					break;
				default:
					this.la1Generation[37] = this.generationCounter;
			}

			label79:
			while (true) {
				switch (this.nextToken.kind) {
					case 11:
						this.consumeToken(11);
						this.parseStringLiteral();
						switch (this.nextToken.kind) {
							case 20:
								this.parseClassPathFilters();
								continue;
							default:
								this.la1Generation[39] = this.generationCounter;
								continue;
						}
					default:
						this.la1Generation[38] = this.generationCounter;
						exceptionThrown = false;
						break label79;
				}
			}
		} catch (Throwable throwable) {
			if (true) {
				this.lexicalBuffer.clearNodeScope(commandNode);
				commitPending = false;
			} else {
				this.lexicalBuffer.popNode();
			}
			if (throwable instanceof RuntimeException) {
				throw (RuntimeException)throwable;
			}

			if (throwable instanceof ProguardCommandParseException) {
				throw (ProguardCommandParseException)throwable;
			}

			throw (Error)throwable;
		} finally {
			if (exceptionThrown && commitPending) {
				this.lexicalBuffer.closeNodeScope(commandNode, true);
			}
		}

		this.lexicalBuffer.closeNodeScope(commandNode, true);
	}

	public final void parseOptimizationPasses() {
		OptimizationPasses commandNode = new OptimizationPasses(28);
		boolean commitPending = true;
		this.lexicalBuffer.openNodeScope(commandNode);

		try {
			ProguardToken commandToken = this.consumeToken(94);
			this.commandKind = 94;
			this.parseIntegerLiteral();
			this.lexicalBuffer.closeNodeScope(commandNode, true);
			commitPending = false;
			commandNode.setLineNumber(commandToken.beginLine);
		} catch (Throwable throwable) {
			if (commitPending) {
				this.lexicalBuffer.clearNodeScope(commandNode);
				commitPending = false;
			} else {
				this.lexicalBuffer.popNode();
			}

			if (throwable instanceof RuntimeException) {
				throw (RuntimeException)throwable;
			}

			if (throwable instanceof ProguardCommandParseException) {
				throw (ProguardCommandParseException)throwable;
			}

			throw (Error)throwable;
		} finally {
			if (commitPending) {
				this.lexicalBuffer.closeNodeScope(commandNode, true);
			}
		}
	}

	public final void parseObfuscationDictionary() {
		ObfuscationDictionary commandNode = new ObfuscationDictionary(39);
		boolean commitPending = true;
		this.lexicalBuffer.openNodeScope(commandNode);

		try {
			ProguardToken commandToken = this.consumeToken(99);
			this.commandKind = 99;
			this.parseStringLiteral();
			this.lexicalBuffer.closeNodeScope(commandNode, true);
			commitPending = false;
			commandNode.setLineNumber(commandToken.beginLine);
		} catch (Throwable throwable) {
			if (commitPending) {
				this.lexicalBuffer.clearNodeScope(commandNode);
				commitPending = false;
			} else {
				this.lexicalBuffer.popNode();
			}

			if (throwable instanceof RuntimeException) {
				throw (RuntimeException)throwable;
			}

			if (throwable instanceof ProguardCommandParseException) {
				throw (ProguardCommandParseException)throwable;
			}

			throw (Error)throwable;
		} finally {
			if (commitPending) {
				this.lexicalBuffer.closeNodeScope(commandNode, true);
			}
		}
	}

	public final void parseDontProcessKotlinMetadata() {
		DontProcessKotlinMetadata commandNode = new DontProcessKotlinMetadata(68);
		boolean commitPending = true;
		this.lexicalBuffer.openNodeScope(commandNode);

		try {
			ProguardToken commandToken = this.consumeToken(105);
			this.commandKind = 105;
			this.lexicalBuffer.closeNodeScope(commandNode, true);
			commitPending = false;
			commandNode.setLineNumber(commandToken.beginLine);
		} finally {
			if (commitPending) {
				this.lexicalBuffer.closeNodeScope(commandNode, true);
			}
		}
	}

	public final void parseWhyAreYouKeeping() {
		WhyAreYouKeeping commandNode = new WhyAreYouKeeping(24);
		boolean commitPending = true;
		this.lexicalBuffer.openNodeScope(commandNode);

		try {
			ProguardToken commandToken = this.consumeToken(89);
			this.commandKind = 89;
			this.parseClassSpecification();
			this.lexicalBuffer.closeNodeScope(commandNode, true);
			commitPending = false;
			commandNode.setLineNumber(commandToken.beginLine);
		} catch (Throwable throwable) {
			if (commitPending) {
				this.lexicalBuffer.clearNodeScope(commandNode);
				commitPending = false;
			} else {
				this.lexicalBuffer.popNode();
			}

			if (throwable instanceof RuntimeException) {
				throw (RuntimeException)throwable;
			}

			if (throwable instanceof ProguardCommandParseException) {
				throw (ProguardCommandParseException)throwable;
			}

			throw (Error)throwable;
		} finally {
			if (commitPending) {
				this.lexicalBuffer.closeNodeScope(commandNode, true);
			}
		}
	}

	public final void parsePrintMappings() {
		PrintMapping commandNode = new PrintMapping(38);
		boolean commitPending = true;
		this.lexicalBuffer.openNodeScope(commandNode);

		try {
			ProguardToken commandToken = this.consumeToken(71);
			this.commandKind = 71;
			switch (this.nextToken.kind) {
				case 122:
					this.parseStringLiteral();
					break;
				default:
					this.la1Generation[16] = this.generationCounter;
			}

			this.lexicalBuffer.closeNodeScope(commandNode, true);
			commitPending = false;
			commandNode.setLineNumber(commandToken.beginLine);
		} catch (Throwable throwable) {
			if (commitPending) {
				this.lexicalBuffer.clearNodeScope(commandNode);
				commitPending = false;
			} else {
				this.lexicalBuffer.popNode();
			}

			if (throwable instanceof RuntimeException) {
				throw (RuntimeException)throwable;
			}

			if (throwable instanceof ProguardCommandParseException) {
				throw (ProguardCommandParseException)throwable;
			}

			throw (Error)throwable;
		} finally {
			if (commitPending) {
				this.lexicalBuffer.closeNodeScope(commandNode, true);
			}
		}
	}

	public final void parseKeepClassesWithMemberNames() {
		KeepClassesWithMemberNames commandNode = new KeepClassesWithMemberNames(20);
		boolean commitPending = true;
		this.lexicalBuffer.openNodeScope(commandNode);

		try {
			ProguardToken commandToken = this.consumeToken(112);
			this.commandKind = 112;

			while (true) {
				switch (this.nextToken.kind) {
					case 12:
						this.consumeToken(12);
						this.parseKeepModifiers();
						break;
					default:
						this.la1Generation[11] = this.generationCounter;
						this.parseClassSpecification();
						this.lexicalBuffer.closeNodeScope(commandNode, true);
						commitPending = false;
						commandNode.setLineNumber(commandToken.beginLine);
						return;
				}
			}
		} catch (Throwable throwable) {
			if (commitPending) {
				this.lexicalBuffer.clearNodeScope(commandNode);
				commitPending = false;
			} else {
				this.lexicalBuffer.popNode();
			}

			if (throwable instanceof RuntimeException) {
				throw (RuntimeException)throwable;
			} else if (throwable instanceof ProguardCommandParseException) {
				throw (ProguardCommandParseException)throwable;
			} else {
				throw (Error)throwable;
			}
		} finally {
			if (commitPending) {
				this.lexicalBuffer.closeNodeScope(commandNode, true);
			}
		}
	}

	public final void parseDontSkipNonPublicLibraryClasses() {
		DontSkipNonPublicLibraryClasses commandNode = new DontSkipNonPublicLibraryClasses(8);
		boolean commitPending = true;
		this.lexicalBuffer.openNodeScope(commandNode);

		try {
			ProguardToken commandToken = this.consumeToken(119);
			this.commandKind = 119;
			this.lexicalBuffer.closeNodeScope(commandNode, true);
			commitPending = false;
			commandNode.setLineNumber(commandToken.beginLine);
		} finally {
			if (commitPending) {
				this.lexicalBuffer.closeNodeScope(commandNode, true);
			}
		}
	}

	public final void parseAssumeNoEscapingParameters() {
		AssumeNoEscapingParameters commandNode = new AssumeNoEscapingParameters(30);
		boolean commitPending = true;
		this.lexicalBuffer.openNodeScope(commandNode);

		try {
			ProguardToken commandToken = this.consumeToken(110);
			this.commandKind = 110;
			this.parseClassSpecification();
			this.lexicalBuffer.closeNodeScope(commandNode, true);
			commitPending = false;
			commandNode.setLineNumber(commandToken.beginLine);
		} catch (Throwable throwable) {
			if (commitPending) {
				this.lexicalBuffer.clearNodeScope(commandNode);
				commitPending = false;
			} else {
				this.lexicalBuffer.popNode();
			}

			if (throwable instanceof RuntimeException) {
				throw (RuntimeException)throwable;
			}

			if (throwable instanceof ProguardCommandParseException) {
				throw (ProguardCommandParseException)throwable;
			}

			throw (Error)throwable;
		} finally {
			if (commitPending) {
				this.lexicalBuffer.closeNodeScope(commandNode, true);
			}
		}
	}

	public final void parseDontObfuscate() {
		DontObfuscate commandNode = new DontObfuscate(36);
		boolean commitPending = true;
		this.lexicalBuffer.openNodeScope(commandNode);

		try {
			ProguardToken commandToken = this.consumeToken(79);
			this.commandKind = 79;
			this.lexicalBuffer.closeNodeScope(commandNode, true);
			commitPending = false;
			commandNode.setLineNumber(commandToken.beginLine);
		} finally {
			if (commitPending) {
				this.lexicalBuffer.closeNodeScope(commandNode, true);
			}
		}
	}

	public final void parseDontUseMixedCaseClassNames() {
		DontUseMixedCaseClassNames commandNode = new DontUseMixedCaseClassNames(44);
		boolean commitPending = true;
		this.lexicalBuffer.openNodeScope(commandNode);

		try {
			ProguardToken commandToken = this.consumeToken(111);
			this.commandKind = 111;
			this.lexicalBuffer.closeNodeScope(commandNode, true);
			commitPending = false;
			commandNode.setLineNumber(commandToken.beginLine);
		} finally {
			if (commitPending) {
				this.lexicalBuffer.closeNodeScope(commandNode, true);
			}
		}
	}

	public final void parseArchiveFilters() {
		ArchiveFiltersNode commandNode = new ArchiveFiltersNode(72);
		boolean commitPending = true;
		this.lexicalBuffer.openNodeScope(commandNode);
		boolean exceptionThrown = false;

		try {
			exceptionThrown = true;
			this.parseStringLiteral();

			label71:
			while (true) {
				switch (this.nextToken.kind) {
					case 12:
						this.consumeToken(12);
						this.parseStringLiteral();
						break;
					default:
						this.la1Generation[40] = this.generationCounter;
						this.consumeToken(11);
						exceptionThrown = false;
						break label71;
				}
			}
		} catch (Throwable throwable) {
			if (true) {
				this.lexicalBuffer.clearNodeScope(commandNode);
				commitPending = false;
			} else {
				this.lexicalBuffer.popNode();
			}
			if (throwable instanceof RuntimeException) {
				throw (RuntimeException)throwable;
			}

			if (throwable instanceof ProguardCommandParseException) {
				throw (ProguardCommandParseException)throwable;
			}

			throw (Error)throwable;
		} finally {
			if (exceptionThrown && commitPending) {
				this.lexicalBuffer.closeNodeScope(commandNode, true);
			}
		}

		this.lexicalBuffer.closeNodeScope(commandNode, true);
	}

	public final void parseMergeInterfacesAggressively() {
		MergeInterfacesAggressively commandNode = new MergeInterfacesAggressively(35);
		boolean commitPending = true;
		this.lexicalBuffer.openNodeScope(commandNode);

		try {
			ProguardToken commandToken = this.consumeToken(116);
			this.commandKind = 116;
			this.lexicalBuffer.closeNodeScope(commandNode, true);
			commitPending = false;
			commandNode.setLineNumber(commandToken.beginLine);
		} finally {
			if (commitPending) {
				this.lexicalBuffer.closeNodeScope(commandNode, true);
			}
		}
	}

	public final void parseAnnotationPattern() {
		AnnotationPatternNode commandNode = new AnnotationPatternNode(80);
		boolean commitPending = true;
		this.lexicalBuffer.openNodeScope(commandNode);
		boolean exceptionThrown = false;

		try {
			exceptionThrown = true;
			this.consumeToken(14);
			this.parseClassName();
			exceptionThrown = false;
		} catch (Throwable throwable) {
			if (true) {
				this.lexicalBuffer.clearNodeScope(commandNode);
				commitPending = false;
			} else {
				this.lexicalBuffer.popNode();
			}
			if (throwable instanceof RuntimeException) {
				throw (RuntimeException)throwable;
			}

			if (throwable instanceof ProguardCommandParseException) {
				throw (ProguardCommandParseException)throwable;
			}

			throw (Error)throwable;
		} finally {
			if (exceptionThrown && commitPending) {
				this.lexicalBuffer.closeNodeScope(commandNode, true);
			}
		}

		this.lexicalBuffer.closeNodeScope(commandNode, true);
	}

	public final void parseAssumeValues() {
		AssumeValues commandNode = new AssumeValues(65);
		boolean commitPending = true;
		this.lexicalBuffer.openNodeScope(commandNode);

		try {
			ProguardToken commandToken = this.consumeToken(72);
			this.commandKind = 72;
			this.parseClassSpecification();
			this.lexicalBuffer.closeNodeScope(commandNode, true);
			commitPending = false;
			commandNode.setLineNumber(commandToken.beginLine);
		} catch (Throwable throwable) {
			if (commitPending) {
				this.lexicalBuffer.clearNodeScope(commandNode);
				commitPending = false;
			} else {
				this.lexicalBuffer.popNode();
			}

			if (throwable instanceof RuntimeException) {
				throw (RuntimeException)throwable;
			}

			if (throwable instanceof ProguardCommandParseException) {
				throw (ProguardCommandParseException)throwable;
			}

			throw (Error)throwable;
		} finally {
			if (commitPending) {
				this.lexicalBuffer.closeNodeScope(commandNode, true);
			}
		}
	}

	public final void parseUseUniqueClassMemberNames() {
		UseUniqueClassMemberNames commandNode = new UseUniqueClassMemberNames(43);
		boolean commitPending = true;
		this.lexicalBuffer.openNodeScope(commandNode);

		try {
			ProguardToken commandToken = this.consumeToken(109);
			this.commandKind = 109;
			this.lexicalBuffer.closeNodeScope(commandNode, true);
			commitPending = false;
			commandNode.setLineNumber(commandToken.beginLine);
		} finally {
			if (commitPending) {
				this.lexicalBuffer.closeNodeScope(commandNode, true);
			}
		}
	}

	public final void parseKeepKotlinMetadata() {
		KeepKotlinMetadata commandNode = new KeepKotlinMetadata(67);
		boolean commitPending = true;
		this.lexicalBuffer.openNodeScope(commandNode);

		try {
			ProguardToken commandToken = this.consumeToken(91);
			this.commandKind = 91;
			this.lexicalBuffer.closeNodeScope(commandNode, true);
			commitPending = false;
			commandNode.setLineNumber(commandToken.beginLine);
		} finally {
			if (commitPending) {
				this.lexicalBuffer.closeNodeScope(commandNode, true);
			}
		}
	}

	public final void parseMicroEdition() {
		MicroEdition commandNode = new MicroEdition(56);
		boolean commitPending = true;
		this.lexicalBuffer.openNodeScope(commandNode);

		try {
			ProguardToken commandToken = this.consumeToken(70);
			this.commandKind = 70;
			this.lexicalBuffer.closeNodeScope(commandNode, true);
			commitPending = false;
			commandNode.setLineNumber(commandToken.beginLine);
		} finally {
			if (commitPending) {
				this.lexicalBuffer.closeNodeScope(commandNode, true);
			}
		}
	}

	public final void parseDontPreVerify() {
		DontPreVerify commandNode = new DontPreVerify(55);
		boolean commitPending = true;
		this.lexicalBuffer.openNodeScope(commandNode);

		try {
			ProguardToken commandToken = this.consumeToken(77);
			this.commandKind = 77;
			this.lexicalBuffer.closeNodeScope(commandNode, true);
			commitPending = false;
			commandNode.setLineNumber(commandToken.beginLine);
		} finally {
			if (commitPending) {
				this.lexicalBuffer.closeNodeScope(commandNode, true);
			}
		}
	}

	public final void parseAssumeNoSideEffects() {
		AssumeNoSideEffects commandNode = new AssumeNoSideEffects(29);
		boolean commitPending = true;
		this.lexicalBuffer.openNodeScope(commandNode);

		try {
			ProguardToken commandToken = this.consumeToken(96);
			this.commandKind = 96;
			this.parseClassSpecification();
			this.lexicalBuffer.closeNodeScope(commandNode, true);
			commitPending = false;
			commandNode.setLineNumber(commandToken.beginLine);
		} catch (Throwable throwable) {
			if (commitPending) {
				this.lexicalBuffer.clearNodeScope(commandNode);
				commitPending = false;
			} else {
				this.lexicalBuffer.popNode();
			}

			if (throwable instanceof RuntimeException) {
				throw (RuntimeException)throwable;
			}

			if (throwable instanceof ProguardCommandParseException) {
				throw (ProguardCommandParseException)throwable;
			}

			throw (Error)throwable;
		} finally {
			if (commitPending) {
				this.lexicalBuffer.closeNodeScope(commandNode, true);
			}
		}
	}

	public final void parsePrintUsage() {
		PrintUsage commandNode = new PrintUsage(23);
		boolean commitPending = true;
		this.lexicalBuffer.openNodeScope(commandNode);

		try {
			ProguardToken commandToken = this.consumeToken(66);
			this.commandKind = 66;
			switch (this.nextToken.kind) {
				case 122:
					this.parseStringLiteral();
					break;
				default:
					this.la1Generation[13] = this.generationCounter;
			}

			this.lexicalBuffer.closeNodeScope(commandNode, true);
			commitPending = false;
			commandNode.setLineNumber(commandToken.beginLine);
		} catch (Throwable throwable) {
			if (commitPending) {
				this.lexicalBuffer.clearNodeScope(commandNode);
				commitPending = false;
			} else {
				this.lexicalBuffer.popNode();
			}

			if (throwable instanceof RuntimeException) {
				throw (RuntimeException)throwable;
			}

			if (throwable instanceof ProguardCommandParseException) {
				throw (ProguardCommandParseException)throwable;
			}

			throw (Error)throwable;
		} finally {
			if (commitPending) {
				this.lexicalBuffer.closeNodeScope(commandNode, true);
			}
		}
	}

	public final void parseDontSkipNonPublicLibraryClassMembers() {
		SkipNonPublicLibraryClassMembers commandNode = new SkipNonPublicLibraryClassMembers(9);
		boolean commitPending = true;
		this.lexicalBuffer.openNodeScope(commandNode);

		try {
			ProguardToken commandToken = this.consumeToken(120);
			this.commandKind = 120;
			this.lexicalBuffer.closeNodeScope(commandNode, true);
			commitPending = false;
			commandNode.setLineNumber(commandToken.beginLine);
		} finally {
			if (commitPending) {
				this.lexicalBuffer.closeNodeScope(commandNode, true);
			}
		}
	}

	public final void parseTarget() {
		Target commandNode = new Target(11);
		boolean commitPending = true;
		this.lexicalBuffer.openNodeScope(commandNode);

		try {
			ProguardToken commandToken = this.consumeToken(40);
			this.commandKind = 40;
			this.parseStringLiteral();
			this.lexicalBuffer.closeNodeScope(commandNode, true);
			commitPending = false;
			commandNode.setLineNumber(commandToken.beginLine);
		} catch (Throwable throwable) {
			if (commitPending) {
				this.lexicalBuffer.clearNodeScope(commandNode);
				commitPending = false;
			} else {
				this.lexicalBuffer.popNode();
			}

			if (throwable instanceof RuntimeException) {
				throw (RuntimeException)throwable;
			}

			if (throwable instanceof ProguardCommandParseException) {
				throw (ProguardCommandParseException)throwable;
			}

			throw (Error)throwable;
		} finally {
			if (commitPending) {
				this.lexicalBuffer.closeNodeScope(commandNode, true);
			}
		}
	}

	public final void parseMemberSpecification() {
		MemberSpecificationNode commandNode = new MemberSpecificationNode(75);
		boolean commitPending = true;
		this.lexicalBuffer.openNodeScope(commandNode);
		boolean exceptionThrown = false;

		try {
			exceptionThrown = true;
			switch (this.nextToken.kind) {
				case 14:
					this.parseAnnotationPattern();
					break;
				default:
					this.la1Generation[47] = this.generationCounter;
			}

			label90:
			while (true) {
				switch (this.nextToken.kind) {
					case 16:
					case 28:
					case 32:
					case 33:
					case 34:
					case 35:
					case 37:
					case 38:
					case 42:
					case 43:
					case 48:
					case 50:
					case 52:
					case 54:
					case 68:
						this.parseMemberModifier();
						break;
					case 17:
					case 18:
					case 19:
					case 20:
					case 21:
					case 22:
					case 23:
					case 24:
					case 25:
					case 26:
					case 27:
					case 29:
					case 30:
					case 31:
					case 36:
					case 39:
					case 40:
					case 41:
					case 44:
					case 45:
					case 46:
					case 47:
					case 49:
					case 51:
					case 53:
					case 55:
					case 56:
					case 57:
					case 58:
					case 59:
					case 60:
					case 61:
					case 62:
					case 63:
					case 64:
					case 65:
					case 66:
					case 67:
					default:
						this.la1Generation[48] = this.generationCounter;
						if (this.nextIsMethodSpecification(Integer.MAX_VALUE)) {
							this.parseMethodSpecification();
						} else if (this.nextIsAllMembersWithSemicolon(Integer.MAX_VALUE)) {
							this.parseAllMembers();
						} else {
							switch (this.nextToken.kind) {
								case 18:
								case 19:
								case 24:
								case 26:
								case 44:
								case 125:
								case 126:
								case 127:
									this.parseFieldSpecification();
									break;
								default:
									this.la1Generation[49] = this.generationCounter;
									this.consumeToken(-1);
									throw new ProguardCommandParseException();
							}
						}

						this.consumeToken(13);
						exceptionThrown = false;
						break label90;
				}
			}
		} catch (Throwable throwable) {
			if (true) {
				this.lexicalBuffer.clearNodeScope(commandNode);
				commitPending = false;
			} else {
				this.lexicalBuffer.popNode();
			}
			if (throwable instanceof RuntimeException) {
				throw (RuntimeException)throwable;
			}

			if (throwable instanceof ProguardCommandParseException) {
				throw (ProguardCommandParseException)throwable;
			}

			throw (Error)throwable;
		} finally {
			if (exceptionThrown && commitPending) {
				this.lexicalBuffer.closeNodeScope(commandNode, true);
			}
		}

		this.lexicalBuffer.closeNodeScope(commandNode, true);
	}

	public final void parseDontOptimize() {
		DontOptimize commandNode = new DontOptimize(25);
		boolean commitPending = true;
		this.lexicalBuffer.openNodeScope(commandNode);

		try {
			ProguardToken commandToken = this.consumeToken(73);
			this.commandKind = 73;
			this.lexicalBuffer.closeNodeScope(commandNode, true);
			commitPending = false;
			commandNode.setLineNumber(commandToken.beginLine);
		} finally {
			if (commitPending) {
				this.lexicalBuffer.closeNodeScope(commandNode, true);
			}
		}
	}

	public final void parseIntegerLiteral() {
		IntegerValueNode commandNode = new IntegerValueNode(96);
		boolean commitPending = true;
		this.lexicalBuffer.openNodeScope(commandNode);

		try {
			ProguardToken commandToken = this.consumeToken(124);
			this.lexicalBuffer.closeNodeScope(commandNode, true);
			commitPending = false;
			commandNode.setValue(commandToken.image);
		} finally {
			if (commitPending) {
				this.lexicalBuffer.closeNodeScope(commandNode, true);
			}
		}
	}

	private boolean isNextFieldTypePatternName() {
		return this.scanToken(125);
	}

	public final void parseAllowAccessModification() {
		AllowAccessModification commandNode = new AllowAccessModification(34);
		boolean commitPending = true;
		this.lexicalBuffer.openNodeScope(commandNode);

		try {
			ProguardToken commandToken = this.consumeToken(103);
			this.commandKind = 103;
			this.lexicalBuffer.closeNodeScope(commandNode, true);
			commitPending = false;
			commandNode.setLineNumber(commandToken.beginLine);
		} finally {
			if (commitPending) {
				this.lexicalBuffer.closeNodeScope(commandNode, true);
			}
		}
	}

	private boolean matchesReturnTypeThenMemberName() {
		return this.isNextTypePattern() ? true : this.isNextMemberName();
	}

	private ProguardToken consumeToken(int kind) {
		ProguardToken savedToken = this.token;
		if ((this.token = this.nextToken).next != null) {
			this.nextToken = this.nextToken.next;
		} else {
			this.nextToken = this.nextToken.next = this.tokenSource.getNextToken();
		}

		if (this.token.kind != kind) {
			this.nextToken = this.token;
			this.token = savedToken;
			this.errorTokenKind = kind;
			throw this.generateParseException();
		} else {
			this.generationCounter++;
			if (++this.lookaheadGcCounter > 100) {
				this.lookaheadGcCounter = 0;

				for (int recordIndex = 0; recordIndex < this.lookaheadRecords.length; recordIndex++) {
					for (LookaheadRecord lookaheadRecord = this.lookaheadRecords[recordIndex]; lookaheadRecord != null; lookaheadRecord = lookaheadRecord.next) {
						if (lookaheadRecord.tokenIndex < this.generationCounter) {
							lookaheadRecord.token = null;
						}
					}
				}
			}

			return this.token;
		}
	}

	public final void parseOptimizations() {
		Optimizations commandNode = new Optimizations(27);
		boolean commitPending = true;
		this.lexicalBuffer.openNodeScope(commandNode);

		try {
			ProguardToken commandToken = this.consumeToken(76);
			this.commandKind = 76;
			this.parseStringLiteral();

			while (true) {
				switch (this.nextToken.kind) {
					case 12:
						this.consumeToken(12);
						this.parseStringLiteral();
						break;
					default:
						this.la1Generation[14] = this.generationCounter;
						this.lexicalBuffer.closeNodeScope(commandNode, true);
						commitPending = false;
						commandNode.setLineNumber(commandToken.beginLine);
						return;
				}
			}
		} catch (Throwable throwable) {
			if (commitPending) {
				this.lexicalBuffer.clearNodeScope(commandNode);
				commitPending = false;
			} else {
				this.lexicalBuffer.popNode();
			}

			if (throwable instanceof RuntimeException) {
				throw (RuntimeException)throwable;
			} else if (throwable instanceof ProguardCommandParseException) {
				throw (ProguardCommandParseException)throwable;
			} else {
				throw (Error)throwable;
			}
		} finally {
			if (commitPending) {
				this.lexicalBuffer.closeNodeScope(commandNode, true);
			}
		}
	}

	private boolean matchesMethodSpecMemberNameOpenParen() {
		return this.isNextMemberName() ? true : this.scanToken(20);
	}

	private void addErrorToken(int kind, int pos) {
		if (pos < 100) {
			if (pos == this.errorTokenEndPos + 1) {
				this.errorTokenKinds[this.errorTokenEndPos++] = kind;
			} else if (this.errorTokenEndPos != 0) {
				this.errorTokenSequence = new int[this.errorTokenEndPos];

				for (int pathIndex = 0; pathIndex < this.errorTokenEndPos; pathIndex++) {
					this.errorTokenSequence[pathIndex] = this.errorTokenKinds[pathIndex];
				}

				label39:
				for (int[] expectedSequence : this.expectedTokenSequences) {
					if (expectedSequence.length == this.errorTokenSequence.length) {
						for (int sequenceIndex = 0; sequenceIndex < this.errorTokenSequence.length; sequenceIndex++) {
							if (expectedSequence[sequenceIndex] != this.errorTokenSequence[sequenceIndex]) {
								continue label39;
							}
						}

						this.expectedTokenSequences.add(this.errorTokenSequence);
						break;
					}
				}

				if (pos != 0) {
					this.errorTokenKinds[(this.errorTokenEndPos = pos) - 1] = kind;
				}
			}
		}
	}

	private boolean isNextMemberNameStar() {
		return this.scanToken(19);
	}

	public final void parseLibraryJars() {
		LibraryJars commandNode = new LibraryJars(5);
		boolean commitPending = true;
		this.lexicalBuffer.openNodeScope(commandNode);

		try {
			ProguardToken commandToken = this.consumeToken(67);
			this.commandKind = 67;
			this.parseClassPathEntry();
			this.lexicalBuffer.closeNodeScope(commandNode, true);
			commitPending = false;
			commandNode.setLineNumber(commandToken.beginLine);
		} catch (Throwable throwable) {
			if (commitPending) {
				this.lexicalBuffer.clearNodeScope(commandNode);
				commitPending = false;
			} else {
				this.lexicalBuffer.popNode();
			}

			if (throwable instanceof RuntimeException) {
				throw (RuntimeException)throwable;
			}

			if (throwable instanceof ProguardCommandParseException) {
				throw (ProguardCommandParseException)throwable;
			}

			throw (Error)throwable;
		} finally {
			if (commitPending) {
				this.lexicalBuffer.closeNodeScope(commandNode, true);
			}
		}
	}

	public final void parseBaseDirectory() {
		BaseDirectory commandNode = new BaseDirectory(6);
		boolean commitPending = true;
		this.lexicalBuffer.openNodeScope(commandNode);

		try {
			ProguardToken commandToken = this.consumeToken(78);
			this.commandKind = 78;
			this.parseStringLiteral();
			this.lexicalBuffer.closeNodeScope(commandNode, true);
			commitPending = false;
			commandNode.setLineNumber(commandToken.beginLine);
		} catch (Throwable throwable) {
			if (commitPending) {
				this.lexicalBuffer.clearNodeScope(commandNode);
				commitPending = false;
			} else {
				this.lexicalBuffer.popNode();
			}

			if (throwable instanceof RuntimeException) {
				throw (RuntimeException)throwable;
			}

			if (throwable instanceof ProguardCommandParseException) {
				throw (ProguardCommandParseException)throwable;
			}

			throw (Error)throwable;
		} finally {
			if (commitPending) {
				this.lexicalBuffer.closeNodeScope(commandNode, true);
			}
		}
	}

	private boolean nextIsClassModifier(int lookaheadDepth) {
		this.lookaheadRemaining = lookaheadDepth;
		this.lastPos = this.scanPos = this.token;

		try {
			return !this.matchesClassModifierExpansion();
		} catch (TokenMgrError2 ignored) {
		} finally {
			this.saveLookaheadState(1, lookaheadDepth);
		}

		return true;
	}

	public final ProguardNodeBase parseConfiguration() {
		ProguardConfigRootNode rootNode = new ProguardConfigRootNode(0);
		boolean commitPending = true;
		this.lexicalBuffer.openNodeScope(rootNode);

		try {
			switch (this.nextToken.kind) {
				case 10:
					this.consumeToken(10);
					break;
				default:
					this.la1Generation[0] = this.generationCounter;
			}

			while (true) {
				switch (this.nextToken.kind) {
					case 25:
					case 29:
					case 30:
					case 40:
					case 41:
					case 45:
					case 46:
					case 47:
					case 51:
					case 55:
					case 56:
					case 57:
					case 58:
					case 61:
					case 64:
					case 65:
					case 66:
					case 67:
					case 69:
					case 70:
					case 71:
					case 72:
					case 73:
					case 74:
					case 76:
					case 77:
					case 78:
					case 79:
					case 80:
					case 81:
					case 83:
					case 84:
					case 86:
					case 87:
					case 88:
					case 89:
					case 90:
					case 91:
					case 92:
					case 93:
					case 94:
					case 95:
					case 96:
					case 97:
					case 98:
					case 99:
					case 100:
					case 101:
					case 102:
					case 103:
					case 105:
					case 106:
					case 107:
					case 108:
					case 109:
					case 110:
					case 111:
					case 112:
					case 113:
					case 114:
					case 115:
					case 116:
					case 117:
					case 118:
					case 119:
					case 120:
						this.parseCommand();
						break;
					case 26:
					case 27:
					case 28:
					case 31:
					case 32:
					case 33:
					case 34:
					case 35:
					case 36:
					case 37:
					case 38:
					case 39:
					case 42:
					case 43:
					case 44:
					case 48:
					case 49:
					case 50:
					case 52:
					case 53:
					case 54:
					case 59:
					case 60:
					case 62:
					case 63:
					case 68:
					case 75:
					case 82:
					case 85:
					case 104:
					default:
						this.la1Generation[1] = this.generationCounter;
						this.consumeToken(0);
						this.lexicalBuffer.closeNodeScope(rootNode, true);
						commitPending = false;
						return rootNode;
				}
			}
		} catch (Throwable throwable) {
			if (commitPending) {
				this.lexicalBuffer.clearNodeScope(rootNode);
				commitPending = false;
			} else {
				this.lexicalBuffer.popNode();
			}

			if (throwable instanceof RuntimeException) {
				throw (RuntimeException)throwable;
			} else if (throwable instanceof ProguardCommandParseException) {
				throw (ProguardCommandParseException)throwable;
			} else {
				throw (Error)throwable;
			}
		} finally {
			if (commitPending) {
				this.lexicalBuffer.closeNodeScope(rootNode, true);
			}
		}
	}

	public final void parseAllFields() {
		AllFieldsNode commandNode = new AllFieldsNode(91);
		this.lexicalBuffer.openNodeScope(commandNode);

		try {
			this.consumeToken(44);
		} finally {
			this.lexicalBuffer.closeNodeScope(commandNode, true);
		}
	}

	public final void parseOutJars() {
		OutJars commandNode = new OutJars(4);
		boolean commitPending = true;
		this.lexicalBuffer.openNodeScope(commandNode);

		try {
			ProguardToken commandToken = this.consumeToken(47);
			this.commandKind = 47;
			this.parseClassPathEntry();
			this.lexicalBuffer.closeNodeScope(commandNode, true);
			commitPending = false;
			commandNode.setLineNumber(commandToken.beginLine);
		} catch (Throwable throwable) {
			if (commitPending) {
				this.lexicalBuffer.clearNodeScope(commandNode);
				commitPending = false;
			} else {
				this.lexicalBuffer.popNode();
			}

			if (throwable instanceof RuntimeException) {
				throw (RuntimeException)throwable;
			}

			if (throwable instanceof ProguardCommandParseException) {
				throw (ProguardCommandParseException)throwable;
			}

			throw (Error)throwable;
		} finally {
			if (commitPending) {
				this.lexicalBuffer.closeNodeScope(commandNode, true);
			}
		}
	}

	private boolean isNextMemberNameWildcardName() {
		return this.scanToken(126);
	}

	private boolean isNextTypePatternPercent() {
		return this.scanToken(18);
	}

	public final void parseExtendsClause() {
		ExtendsClauseNode commandNode = new ExtendsClauseNode(81);
		boolean commitPending = true;
		this.lexicalBuffer.openNodeScope(commandNode);
		boolean exceptionThrown = false;

		try {
			exceptionThrown = true;
			this.consumeToken(39);
			switch (this.nextToken.kind) {
				case 14:
					this.parseAnnotationPattern();
					break;
				default:
					this.la1Generation[54] = this.generationCounter;
			}

			this.parseClassName();
			exceptionThrown = false;
		} catch (Throwable throwable) {
			if (true) {
				this.lexicalBuffer.clearNodeScope(commandNode);
				commitPending = false;
			} else {
				this.lexicalBuffer.popNode();
			}
			if (throwable instanceof RuntimeException) {
				throw (RuntimeException)throwable;
			}

			if (throwable instanceof ProguardCommandParseException) {
				throw (ProguardCommandParseException)throwable;
			}

			throw (Error)throwable;
		} finally {
			if (exceptionThrown && commitPending) {
				this.lexicalBuffer.closeNodeScope(commandNode, true);
			}
		}

		this.lexicalBuffer.closeNodeScope(commandNode, true);
	}

	private boolean matchesMethodSpecificationExpansion() {
		ProguardToken savedScanPos = this.scanPos;
		if (this.isNextAllMethods()) {
			this.scanPos = savedScanPos;
			if (this.matchesMethodSpecWithReturnType()) {
				this.scanPos = savedScanPos;
				if (this.matchesMethodSpecMemberNameOpenParen()) {
					return true;
				}
			}
		}

		return false;
	}

	public final void parseKeepPackageNames() {
		KeepPackageNames commandNode = new KeepPackageNames(45);
		boolean commitPending = true;
		this.lexicalBuffer.openNodeScope(commandNode);

		try {
			ProguardToken commandToken;
			commandToken = this.consumeToken(87);
			this.commandKind = 87;
			label77:
			switch (this.nextToken.kind) {
				case 122:
					this.parseStringLiteral();

					while (true) {
						switch (this.nextToken.kind) {
							case 12:
								this.consumeToken(12);
								this.parseStringLiteral();
								break;
							default:
								this.la1Generation[17] = this.generationCounter;
								break label77;
						}
					}
				default:
					this.la1Generation[18] = this.generationCounter;
			}

			this.lexicalBuffer.closeNodeScope(commandNode, true);
			commitPending = false;
			commandNode.setLineNumber(commandToken.beginLine);
		} catch (Throwable throwable) {
			if (commitPending) {
				this.lexicalBuffer.clearNodeScope(commandNode);
				commitPending = false;
			} else {
				this.lexicalBuffer.popNode();
			}

			if (throwable instanceof RuntimeException) {
				throw (RuntimeException)throwable;
			}

			if (throwable instanceof ProguardCommandParseException) {
				throw (ProguardCommandParseException)throwable;
			}

			throw (Error)throwable;
		} finally {
			if (commitPending) {
				this.lexicalBuffer.closeNodeScope(commandNode, true);
			}
		}
	}

	public final void parseImplementsClause() {
		ImplementsClauseNode commandNode = new ImplementsClauseNode(82);
		boolean commitPending = true;
		this.lexicalBuffer.openNodeScope(commandNode);
		boolean exceptionThrown = false;

		try {
			exceptionThrown = true;
			this.consumeToken(60);
			switch (this.nextToken.kind) {
				case 14:
					this.parseAnnotationPattern();
					break;
				default:
					this.la1Generation[55] = this.generationCounter;
			}

			this.parseClassName();
			exceptionThrown = false;
		} catch (Throwable throwable) {
			if (true) {
				this.lexicalBuffer.clearNodeScope(commandNode);
				commitPending = false;
			} else {
				this.lexicalBuffer.popNode();
			}
			if (throwable instanceof RuntimeException) {
				throw (RuntimeException)throwable;
			}

			if (throwable instanceof ProguardCommandParseException) {
				throw (ProguardCommandParseException)throwable;
			}

			throw (Error)throwable;
		} finally {
			if (exceptionThrown && commitPending) {
				this.lexicalBuffer.closeNodeScope(commandNode, true);
			}
		}

		this.lexicalBuffer.closeNodeScope(commandNode, true);
	}

	private boolean isNextMemberName() {
		ProguardToken savedScanPos = this.scanPos;
		if (this.isNextMemberNameStar()) {
			this.scanPos = savedScanPos;
			if (this.isNextMemberNameDoubleStar()) {
				this.scanPos = savedScanPos;
				if (this.isNextMemberNameInitTag()) {
					this.scanPos = savedScanPos;
					if (this.isNextMemberNameName()) {
						this.scanPos = savedScanPos;
						if (this.isNextMemberNameWildcardName()) {
							return true;
						}
					}
				}
			}
		}

		return false;
	}

	private boolean isNextMemberNameName() {
		return this.scanToken(125);
	}

	public final void parseInJars() {
		InJars commandNode = new InJars(3);
		boolean commitPending = true;
		this.lexicalBuffer.openNodeScope(commandNode);

		try {
			ProguardToken commandToken = this.consumeToken(41);
			this.commandKind = 41;
			this.parseClassPathEntry();
			this.lexicalBuffer.closeNodeScope(commandNode, true);
			commitPending = false;
			commandNode.setLineNumber(commandToken.beginLine);
		} catch (Throwable throwable) {
			if (commitPending) {
				this.lexicalBuffer.clearNodeScope(commandNode);
				commitPending = false;
			} else {
				this.lexicalBuffer.popNode();
			}

			if (throwable instanceof RuntimeException) {
				throw (RuntimeException)throwable;
			}

			if (throwable instanceof ProguardCommandParseException) {
				throw (ProguardCommandParseException)throwable;
			}

			throw (Error)throwable;
		} finally {
			if (commitPending) {
				this.lexicalBuffer.closeNodeScope(commandNode, true);
			}
		}
	}

	private boolean isNextTypePattern() {
		ProguardToken savedScanPos = this.scanPos;
		if (this.isNextTypePatternPercent()) {
			this.scanPos = savedScanPos;
			if (this.isNextTypePatternStar()) {
				this.scanPos = savedScanPos;
				if (this.isNextTypePatternDoubleStar()) {
					this.scanPos = savedScanPos;
					if (this.isNextTypePatternTripleStar()) {
						this.scanPos = savedScanPos;
						if (this.isNextTypePatternName()) {
							this.scanPos = savedScanPos;
							if (this.isNextTypePatternWildcardName()) {
								this.scanPos = savedScanPos;
								if (this.isNextTypePatternWildcardType()) {
									return true;
								}
							}
						}
					}
				}
			}
		}

		return false;
	}

	public final void parsePrintConfiguration() {
		PrintConfiguration commandNode = new PrintConfiguration(62);
		boolean commitPending = true;
		this.lexicalBuffer.openNodeScope(commandNode);

		try {
			ProguardToken commandToken = this.consumeToken(92);
			this.commandKind = 92;
			switch (this.nextToken.kind) {
				case 122:
					this.parseStringLiteral();
					break;
				default:
					this.la1Generation[33] = this.generationCounter;
			}

			this.lexicalBuffer.closeNodeScope(commandNode, true);
			commitPending = false;
			commandNode.setLineNumber(commandToken.beginLine);
		} catch (Throwable throwable) {
			if (commitPending) {
				this.lexicalBuffer.clearNodeScope(commandNode);
				commitPending = false;
			} else {
				this.lexicalBuffer.popNode();
			}

			if (throwable instanceof RuntimeException) {
				throw (RuntimeException)throwable;
			}

			if (throwable instanceof ProguardCommandParseException) {
				throw (ProguardCommandParseException)throwable;
			}

			throw (Error)throwable;
		} finally {
			if (commitPending) {
				this.lexicalBuffer.closeNodeScope(commandNode, true);
			}
		}
	}

	public final void parseClassName() {
		ClassNameNode commandNode = new ClassNameNode(89);
		boolean commitPending = true;
		this.lexicalBuffer.openNodeScope(commandNode);

		try {
			switch (this.nextToken.kind) {
				case 16:
					this.parseNegation();
					break;
				default:
					this.la1Generation[62] = this.generationCounter;
			}

			switch (this.nextToken.kind) {
				case 19:
					ProguardToken starToken = this.consumeToken(19);
					this.lexicalBuffer.closeNodeScope(commandNode, true);
					commitPending = false;
					commandNode.setValue(starToken.image);
					break;
				case 24:
					ProguardToken doubleStarToken = this.consumeToken(24);
					this.lexicalBuffer.closeNodeScope(commandNode, true);
					commitPending = false;
					commandNode.setValue(doubleStarToken.image);
					break;
				case 62:
					ProguardToken moduleInfoToken = this.consumeToken(62);
					this.lexicalBuffer.closeNodeScope(commandNode, true);
					commitPending = false;
					commandNode.setValue(moduleInfoToken.image);
					break;
				case 125:
					ProguardToken nameToken = this.consumeToken(125);
					this.lexicalBuffer.closeNodeScope(commandNode, true);
					commitPending = false;
					commandNode.setValue(nameToken.image);
					break;
				case 126:
					ProguardToken wildcardNameToken = this.consumeToken(126);
					this.lexicalBuffer.closeNodeScope(commandNode, true);
					commitPending = false;
					commandNode.setValue(wildcardNameToken.image);
					break;
				default:
					this.la1Generation[63] = this.generationCounter;
					this.consumeToken(-1);
					throw new ProguardCommandParseException();
			}
		} catch (Throwable throwable) {
			if (commitPending) {
				this.lexicalBuffer.clearNodeScope(commandNode);
				commitPending = false;
			} else {
				this.lexicalBuffer.popNode();
			}

			if (throwable instanceof RuntimeException) {
				throw (RuntimeException)throwable;
			}

			if (throwable instanceof ProguardCommandParseException) {
				throw (ProguardCommandParseException)throwable;
			}

			throw (Error)throwable;
		} finally {
			if (commitPending) {
				this.lexicalBuffer.closeNodeScope(commandNode, true);
			}
		}
	}

	private boolean isNextTypePatternDoubleStar() {
		return this.scanToken(24);
	}

	public final void parseClassPathFilters() {
		ClassPathFiltersNode commandNode = new ClassPathFiltersNode(71);
		boolean commitPending = true;
		this.lexicalBuffer.openNodeScope(commandNode);
		boolean exceptionThrown = false;

		try {
			exceptionThrown = true;
			this.consumeToken(20);
			if (this.nextIsArchiveFilters(Integer.MAX_VALUE)) {
				this.parseArchiveFilters();
			}

			this.parseFileFilters();
			this.consumeToken(21);
			exceptionThrown = false;
		} catch (Throwable throwable) {
			if (true) {
				this.lexicalBuffer.clearNodeScope(commandNode);
				commitPending = false;
			} else {
				this.lexicalBuffer.popNode();
			}
			if (throwable instanceof RuntimeException) {
				throw (RuntimeException)throwable;
			}

			if (throwable instanceof ProguardCommandParseException) {
				throw (ProguardCommandParseException)throwable;
			}

			throw (Error)throwable;
		} finally {
			if (exceptionThrown && commitPending) {
				this.lexicalBuffer.closeNodeScope(commandNode, true);
			}
		}

		if (true) {
			this.lexicalBuffer.closeNodeScope(commandNode, true);
		}
	}

	public final void parseForceProcessing() {
		ForceProcessing commandNode = new ForceProcessing(12);
		boolean commitPending = true;
		this.lexicalBuffer.openNodeScope(commandNode);

		try {
			ProguardToken commandToken = this.consumeToken(84);
			this.commandKind = 84;
			this.lexicalBuffer.closeNodeScope(commandNode, true);
			commitPending = false;
			commandNode.setLineNumber(commandToken.beginLine);
		} finally {
			if (commitPending) {
				this.lexicalBuffer.closeNodeScope(commandNode, true);
			}
		}
	}

	private boolean nextIsArchiveFilters(int lookaheadDepth) {
		this.lookaheadRemaining = lookaheadDepth;
		this.lastPos = this.scanPos = this.token;

		try {
			return !this.matchesArchiveFiltersExpansion();
		} catch (TokenMgrError2 ignored) {
		} finally {
			this.saveLookaheadState(0, lookaheadDepth);
		}

		return true;
	}

	public final void parseKeepClassesWithMembers() {
		KeepClassesWithMembers commandNode = new KeepClassesWithMembers(17);
		boolean commitPending = true;
		this.lexicalBuffer.openNodeScope(commandNode);

		try {
			ProguardToken commandToken = this.consumeToken(101);
			this.commandKind = 101;

			while (true) {
				switch (this.nextToken.kind) {
					case 12:
						this.consumeToken(12);
						this.parseKeepModifiers();
						break;
					default:
						this.la1Generation[8] = this.generationCounter;
						this.parseClassSpecification();
						this.lexicalBuffer.closeNodeScope(commandNode, true);
						commitPending = false;
						commandNode.setLineNumber(commandToken.beginLine);
						return;
				}
			}
		} catch (Throwable throwable) {
			if (commitPending) {
				this.lexicalBuffer.clearNodeScope(commandNode);
				commitPending = false;
			} else {
				this.lexicalBuffer.popNode();
			}

			if (throwable instanceof RuntimeException) {
				throw (RuntimeException)throwable;
			} else if (throwable instanceof ProguardCommandParseException) {
				throw (ProguardCommandParseException)throwable;
			} else {
				throw (Error)throwable;
			}
		} finally {
			if (commitPending) {
				this.lexicalBuffer.closeNodeScope(commandNode, true);
			}
		}
	}

	private boolean isNextFieldTypePatternDoubleStar() {
		return this.scanToken(24);
	}

	public final void parsePrintSeeds() {
		PrintSeeds commandNode = new PrintSeeds(21);
		boolean commitPending = true;
		this.lexicalBuffer.openNodeScope(commandNode);

		try {
			ProguardToken commandToken = this.consumeToken(64);
			this.commandKind = 64;
			switch (this.nextToken.kind) {
				case 122:
					this.parseStringLiteral();
					break;
				default:
					this.la1Generation[12] = this.generationCounter;
			}

			this.lexicalBuffer.closeNodeScope(commandNode, true);
			commitPending = false;
			commandNode.setLineNumber(commandToken.beginLine);
		} catch (Throwable throwable) {
			if (commitPending) {
				this.lexicalBuffer.clearNodeScope(commandNode);
				commitPending = false;
			} else {
				this.lexicalBuffer.popNode();
			}

			if (throwable instanceof RuntimeException) {
				throw (RuntimeException)throwable;
			}

			if (throwable instanceof ProguardCommandParseException) {
				throw (ProguardCommandParseException)throwable;
			}

			throw (Error)throwable;
		} finally {
			if (commitPending) {
				this.lexicalBuffer.closeNodeScope(commandNode, true);
			}
		}
	}

	public ProguardCommandParseException generateParseException() {
		this.expectedTokenSequences.clear();
		boolean[] expectedTokenKinds = new boolean[130];
		if (this.errorTokenKind >= 0) {
			expectedTokenKinds[this.errorTokenKind] = true;
			this.errorTokenKind = -1;
		}

		for (int la1Index = 0; la1Index < 71; la1Index++) {
			if (this.la1Generation[la1Index] == this.generationCounter) {
				for (int bitIndex = 0; bitIndex < 32; bitIndex++) {
					if ((la1TokenBits0[la1Index] & 1 << bitIndex) != 0) {
						expectedTokenKinds[bitIndex] = true;
					}

					if ((la1TokenBits1[la1Index] & 1 << bitIndex) != 0) {
						expectedTokenKinds[32 + bitIndex] = true;
					}

					if ((la1TokenBits2[la1Index] & 1 << bitIndex) != 0) {
						expectedTokenKinds[64 + bitIndex] = true;
					}

					if ((la1TokenBits3[la1Index] & 1 << bitIndex) != 0) {
						expectedTokenKinds[96 + bitIndex] = true;
					}

					if ((la1TokenBits4[la1Index] & 1 << bitIndex) != 0) {
						expectedTokenKinds[128 + bitIndex] = true;
					}
				}
			}
		}

		for (int tokenKind = 0; tokenKind < 130; tokenKind++) {
			if (expectedTokenKinds[tokenKind]) {
				this.errorTokenSequence = new int[1];
				this.errorTokenSequence[0] = tokenKind;
				this.expectedTokenSequences.add(this.errorTokenSequence);
			}
		}

		this.errorTokenEndPos = 0;
		this.rescanToken();
		this.addErrorToken(0, 0);
		int[][] expectedSequencesArray = new int[this.expectedTokenSequences.size()][];

		for (int sequenceIndex = 0; sequenceIndex < this.expectedTokenSequences.size(); sequenceIndex++) {
			expectedSequencesArray[sequenceIndex] = this.expectedTokenSequences.get(sequenceIndex);
		}

		return new ProguardCommandParseException(this.token, expectedSequencesArray, ProguardCommands.commands);
	}

	private boolean isNextFieldTypePatternTripleStar() {
		return this.scanToken(26);
	}

	public final void parseStringLiteral() {
		QuotedStringNode commandNode = new QuotedStringNode(97);
		boolean commitPending = true;
		this.lexicalBuffer.openNodeScope(commandNode);

		try {
			ProguardToken quoteToken = this.consumeToken(122);
			this.lexicalBuffer.closeNodeScope(commandNode, true);
			commitPending = false;
			String literalText = quoteToken.image;
			literalText = StringUtils.replace(literalText.substring(1, literalText.length() - 1), "\"\"", "\"");
			commandNode.setValue(literalText);
		} finally {
			if (commitPending) {
				this.lexicalBuffer.closeNodeScope(commandNode, true);
			}
		}
	}

	private boolean isNextMethodsTag() {
		return this.scanToken(49);
	}

	public final void parseMemberName() {
		NamePatternNode commandNode = new NamePatternNode(90);
		boolean commitPending = true;
		this.lexicalBuffer.openNodeScope(commandNode);

		try {
			switch (this.nextToken.kind) {
				case 19:
					ProguardToken starToken = this.consumeToken(19);
					this.lexicalBuffer.closeNodeScope(commandNode, true);
					commitPending = false;
					commandNode.setValue(starToken.image);
					break;
				case 24:
					ProguardToken doubleStarToken = this.consumeToken(24);
					this.lexicalBuffer.closeNodeScope(commandNode, true);
					commitPending = false;
					commandNode.setValue(doubleStarToken.image);
					break;
				case 36:
					ProguardToken initToken = this.consumeToken(36);
					this.lexicalBuffer.closeNodeScope(commandNode, true);
					commitPending = false;
					commandNode.setValue(initToken.image);
					break;
				case 125:
					ProguardToken nameToken = this.consumeToken(125);
					this.lexicalBuffer.closeNodeScope(commandNode, true);
					commitPending = false;
					commandNode.setValue(nameToken.image);
					break;
				case 126:
					ProguardToken wildcardNameToken = this.consumeToken(126);
					this.lexicalBuffer.closeNodeScope(commandNode, true);
					commitPending = false;
					commandNode.setValue(wildcardNameToken.image);
					break;
				default:
					this.la1Generation[64] = this.generationCounter;
					this.consumeToken(-1);
					throw new ProguardCommandParseException();
			}
		} finally {
			if (commitPending) {
				this.lexicalBuffer.closeNodeScope(commandNode, true);
			}
		}
	}

	private boolean nextIsAllFields(int lookaheadDepth) {
		this.lookaheadRemaining = lookaheadDepth;
		this.lastPos = this.scanPos = this.token;

		try {
			return !this.matchesTypePatternThenFieldsKeyword();
		} catch (TokenMgrError2 ignored) {
		} finally {
			this.saveLookaheadState(4, lookaheadDepth);
		}

		return true;
	}

	public final void parseFieldSpecification() {
		FieldSpecificationNode commandNode = new FieldSpecificationNode(77);
		boolean commitPending = true;
		this.lexicalBuffer.openNodeScope(commandNode);
		boolean exceptionThrown = false;

		try {
			exceptionThrown = true;
			if (this.nextIsAllFields(Integer.MAX_VALUE)) {
				switch (this.nextToken.kind) {
					case 18:
					case 19:
					case 24:
					case 26:
					case 125:
					case 126:
					case 127:
						this.parseFieldTypePattern();
						break;
					default:
						this.la1Generation[50] = this.generationCounter;
				}

				this.parseAllFields();
				exceptionThrown = false;
			} else {
				switch (this.nextToken.kind) {
					case 18:
					case 19:
					case 24:
					case 26:
					case 125:
					case 126:
					case 127:
						this.parseTypePattern();
						this.parseMemberName();
						exceptionThrown = false;
						break;
					default:
						this.la1Generation[51] = this.generationCounter;
						this.consumeToken(-1);
						throw new ProguardCommandParseException();
				}
			}
		} catch (Throwable throwable) {
			if (true) {
				this.lexicalBuffer.clearNodeScope(commandNode);
				commitPending = false;
			} else {
				this.lexicalBuffer.popNode();
			}
			if (throwable instanceof RuntimeException) {
				throw (RuntimeException)throwable;
			}

			if (throwable instanceof ProguardCommandParseException) {
				throw (ProguardCommandParseException)throwable;
			}

			throw (Error)throwable;
		} finally {
			if (exceptionThrown && commitPending) {
				this.lexicalBuffer.closeNodeScope(commandNode, true);
			}
		}

		this.lexicalBuffer.closeNodeScope(commandNode, true);
	}

	private boolean isNextTypePatternTripleStar() {
		return this.scanToken(26);
	}

	public final void parseMemberModifier() {
		MemberModifierNode commandNode = new MemberModifierNode(95);
		boolean commitPending = true;
		this.lexicalBuffer.openNodeScope(commandNode);

		try {
			switch (this.nextToken.kind) {
				case 16:
					this.parseNegation();
					break;
				default:
					this.la1Generation[69] = this.generationCounter;
			}

			switch (this.nextToken.kind) {
				case 28:
					ProguardToken enumToken = this.consumeToken(28);
					this.lexicalBuffer.closeNodeScope(commandNode, true);
					commitPending = false;
					commandNode.setValue(enumToken.image);
					break;
				case 29:
				case 30:
				case 31:
				case 36:
				case 39:
				case 40:
				case 41:
				case 44:
				case 45:
				case 46:
				case 47:
				case 49:
				case 51:
				case 53:
				case 55:
				case 56:
				case 57:
				case 58:
				case 59:
				case 60:
				case 61:
				case 62:
				case 63:
				case 64:
				case 65:
				case 66:
				case 67:
				default:
					this.la1Generation[70] = this.generationCounter;
					this.consumeToken(-1);
					throw new ProguardCommandParseException();
				case 32:
					ProguardToken finalToken = this.consumeToken(32);
					this.lexicalBuffer.closeNodeScope(commandNode, true);
					commitPending = false;
					commandNode.setValue(finalToken.image);
					break;
				case 33:
					ProguardToken publicToken = this.consumeToken(33);
					this.lexicalBuffer.closeNodeScope(commandNode, true);
					commitPending = false;
					commandNode.setValue(publicToken.image);
					break;
				case 34:
					ProguardToken staticToken = this.consumeToken(34);
					this.lexicalBuffer.closeNodeScope(commandNode, true);
					commitPending = false;
					commandNode.setValue(staticToken.image);
					break;
				case 35:
					ProguardToken nativeToken = this.consumeToken(35);
					this.lexicalBuffer.closeNodeScope(commandNode, true);
					commitPending = false;
					commandNode.setValue(nativeToken.image);
					break;
				case 37:
					ProguardToken bridgeToken = this.consumeToken(37);
					this.lexicalBuffer.closeNodeScope(commandNode, true);
					commitPending = false;
					commandNode.setValue(bridgeToken.image);
					break;
				case 38:
					ProguardToken privateToken = this.consumeToken(38);
					this.lexicalBuffer.closeNodeScope(commandNode, true);
					commitPending = false;
					commandNode.setValue(privateToken.image);
					break;
				case 42:
					ProguardToken volatileToken = this.consumeToken(42);
					this.lexicalBuffer.closeNodeScope(commandNode, true);
					commitPending = false;
					commandNode.setValue(volatileToken.image);
					break;
				case 43:
					ProguardToken strictfpToken = this.consumeToken(43);
					this.lexicalBuffer.closeNodeScope(commandNode, true);
					commitPending = false;
					commandNode.setValue(strictfpToken.image);
					break;
				case 48:
					ProguardToken protectedToken = this.consumeToken(48);
					this.lexicalBuffer.closeNodeScope(commandNode, true);
					commitPending = false;
					commandNode.setValue(protectedToken.image);
					break;
				case 50:
					ProguardToken abstractToken = this.consumeToken(50);
					this.lexicalBuffer.closeNodeScope(commandNode, true);
					commitPending = false;
					commandNode.setValue(abstractToken.image);
					break;
				case 52:
					ProguardToken transientToken = this.consumeToken(52);
					this.lexicalBuffer.closeNodeScope(commandNode, true);
					commitPending = false;
					commandNode.setValue(transientToken.image);
					break;
				case 54:
					ProguardToken syntheticToken = this.consumeToken(54);
					this.lexicalBuffer.closeNodeScope(commandNode, true);
					commitPending = false;
					commandNode.setValue(syntheticToken.image);
					break;
				case 68:
					ProguardToken synchronizedToken = this.consumeToken(68);
					this.lexicalBuffer.closeNodeScope(commandNode, true);
					commitPending = false;
					commandNode.setValue(synchronizedToken.image);
			}
		} catch (Throwable throwable) {
			if (commitPending) {
				this.lexicalBuffer.clearNodeScope(commandNode);
				commitPending = false;
			} else {
				this.lexicalBuffer.popNode();
			}

			if (throwable instanceof RuntimeException) {
				throw (RuntimeException)throwable;
			}

			if (throwable instanceof ProguardCommandParseException) {
				throw (ProguardCommandParseException)throwable;
			}

			throw (Error)throwable;
		} finally {
			if (commitPending) {
				this.lexicalBuffer.closeNodeScope(commandNode, true);
			}
		}
	}

	private boolean isNextFieldTypePatternStar() {
		return this.scanToken(19);
	}

	public final void parseClassObfuscationDictionary() {
		ClassObfuscationDictionary commandNode = new ClassObfuscationDictionary(40);
		boolean commitPending = true;
		this.lexicalBuffer.openNodeScope(commandNode);

		try {
			ProguardToken commandToken = this.consumeToken(113);
			this.commandKind = 113;
			this.parseStringLiteral();
			this.lexicalBuffer.closeNodeScope(commandNode, true);
			commitPending = false;
			commandNode.setLineNumber(commandToken.beginLine);
		} catch (Throwable throwable) {
			if (commitPending) {
				this.lexicalBuffer.clearNodeScope(commandNode);
				commitPending = false;
			} else {
				this.lexicalBuffer.popNode();
			}

			if (throwable instanceof RuntimeException) {
				throw (RuntimeException)throwable;
			}

			if (throwable instanceof ProguardCommandParseException) {
				throw (ProguardCommandParseException)throwable;
			}

			throw (Error)throwable;
		} finally {
			if (commitPending) {
				this.lexicalBuffer.closeNodeScope(commandNode, true);
			}
		}
	}

	private boolean isNextMemberNameInitTag() {
		return this.scanToken(36);
	}

	public final void parseSkipNonPublicLibraryClasses() {
		SkipNonPublicLibraryClasses commandNode = new SkipNonPublicLibraryClasses(7);
		boolean commitPending = true;
		this.lexicalBuffer.openNodeScope(commandNode);

		try {
			ProguardToken commandToken = this.consumeToken(115);
			this.commandKind = 115;
			this.lexicalBuffer.closeNodeScope(commandNode, true);
			commitPending = false;
			commandNode.setLineNumber(commandToken.beginLine);
		} finally {
			if (commitPending) {
				this.lexicalBuffer.closeNodeScope(commandNode, true);
			}
		}
	}

	public final void parseKeepDirectories() {
		KeepDirectories commandNode = new KeepDirectories(10);
		boolean commitPending = true;
		this.lexicalBuffer.openNodeScope(commandNode);

		try {
			ProguardToken commandToken;
			commandToken = this.consumeToken(83);
			this.commandKind = 83;
			label77:
			switch (this.nextToken.kind) {
				case 122:
					this.parseStringLiteral();

					while (true) {
						switch (this.nextToken.kind) {
							case 12:
								this.consumeToken(12);
								this.parseStringLiteral();
								break;
							default:
								this.la1Generation[3] = this.generationCounter;
								break label77;
						}
					}
				default:
					this.la1Generation[4] = this.generationCounter;
			}

			this.lexicalBuffer.closeNodeScope(commandNode, true);
			commitPending = false;
			commandNode.setLineNumber(commandToken.beginLine);
		} catch (Throwable throwable) {
			if (commitPending) {
				this.lexicalBuffer.clearNodeScope(commandNode);
				commitPending = false;
			} else {
				this.lexicalBuffer.popNode();
			}

			if (throwable instanceof RuntimeException) {
				throw (RuntimeException)throwable;
			}

			if (throwable instanceof ProguardCommandParseException) {
				throw (ProguardCommandParseException)throwable;
			}

			throw (Error)throwable;
		} finally {
			if (commitPending) {
				this.lexicalBuffer.closeNodeScope(commandNode, true);
			}
		}
	}

	private boolean nextIsReturnTypePatternThenMemberName(int lookaheadDepth) {
		this.lookaheadRemaining = lookaheadDepth;
		this.lastPos = this.scanPos = this.token;

		try {
			return !this.matchesReturnTypeThenMemberName();
		} catch (TokenMgrError2 ignored) {
		} finally {
			this.saveLookaheadState(5, lookaheadDepth);
		}

		return true;
	}

	public final void parseClassSpecification() {
		ClassSpecification commandNode = new ClassSpecification(74);
		boolean commitPending = true;
		this.lexicalBuffer.openNodeScope(commandNode);
		boolean exceptionThrown = false;

		try {
			exceptionThrown = true;
			switch (this.nextToken.kind) {
				case 14:
					this.parseAnnotationPattern();
					break;
				default:
					this.la1Generation[42] = this.generationCounter;
			}

			while (this.nextIsClassModifier(Integer.MAX_VALUE)) {
				this.parseClassModifier();
			}

			this.parseClassType();
			this.parseClassNameList();
			switch (this.nextToken.kind) {
				case 39:
					this.parseExtendsClause();
					break;
				default:
					this.la1Generation[43] = this.generationCounter;
			}

			switch (this.nextToken.kind) {
				case 60:
					this.parseImplementsClause();
					break;
				default:
					this.la1Generation[44] = this.generationCounter;
			}

			label104:
			switch (this.nextToken.kind) {
				case 22:
					this.consumeToken(22);

					while (true) {
						switch (this.nextToken.kind) {
							case 14:
							case 16:
							case 18:
							case 19:
							case 24:
							case 26:
							case 28:
							case 32:
							case 33:
							case 34:
							case 35:
							case 36:
							case 37:
							case 38:
							case 42:
							case 43:
							case 44:
							case 48:
							case 49:
							case 50:
							case 52:
							case 54:
							case 68:
							case 125:
							case 126:
							case 127:
								this.parseMemberSpecification();
								break;
							case 15:
							case 17:
							case 20:
							case 21:
							case 22:
							case 23:
							case 25:
							case 27:
							case 29:
							case 30:
							case 31:
							case 39:
							case 40:
							case 41:
							case 45:
							case 46:
							case 47:
							case 51:
							case 53:
							case 55:
							case 56:
							case 57:
							case 58:
							case 59:
							case 60:
							case 61:
							case 62:
							case 63:
							case 64:
							case 65:
							case 66:
							case 67:
							case 69:
							case 70:
							case 71:
							case 72:
							case 73:
							case 74:
							case 75:
							case 76:
							case 77:
							case 78:
							case 79:
							case 80:
							case 81:
							case 82:
							case 83:
							case 84:
							case 85:
							case 86:
							case 87:
							case 88:
							case 89:
							case 90:
							case 91:
							case 92:
							case 93:
							case 94:
							case 95:
							case 96:
							case 97:
							case 98:
							case 99:
							case 100:
							case 101:
							case 102:
							case 103:
							case 104:
							case 105:
							case 106:
							case 107:
							case 108:
							case 109:
							case 110:
							case 111:
							case 112:
							case 113:
							case 114:
							case 115:
							case 116:
							case 117:
							case 118:
							case 119:
							case 120:
							case 121:
							case 122:
							case 123:
							case 124:
							default:
								this.la1Generation[45] = this.generationCounter;
								this.consumeToken(23);
								exceptionThrown = false;
								break label104;
						}
					}
				default:
					this.la1Generation[46] = this.generationCounter;
					exceptionThrown = false;
			}
		} catch (Throwable throwable) {
			if (true) {
				this.lexicalBuffer.clearNodeScope(commandNode);
				commitPending = false;
			} else {
				this.lexicalBuffer.popNode();
			}
			if (throwable instanceof RuntimeException) {
				throw (RuntimeException)throwable;
			}

			if (throwable instanceof ProguardCommandParseException) {
				throw (ProguardCommandParseException)throwable;
			}

			throw (Error)throwable;
		} finally {
			if (exceptionThrown && commitPending) {
				this.lexicalBuffer.closeNodeScope(commandNode, true);
			}
		}

		this.lexicalBuffer.closeNodeScope(commandNode, true);
	}

	private boolean matchesArchiveFiltersExpansion() {
		return this.matchesArchiveFilters();
	}

	private boolean isNextMemberNameDoubleStar() {
		return this.scanToken(24);
	}

	public final void parseKeepClassMembers() {
		KeepClassMembers commandNode = new KeepClassMembers(16);
		boolean commitPending = true;
		this.lexicalBuffer.openNodeScope(commandNode);

		try {
			ProguardToken commandToken = this.consumeToken(86);
			this.commandKind = 86;

			while (true) {
				switch (this.nextToken.kind) {
					case 12:
						this.consumeToken(12);
						this.parseKeepModifiers();
						break;
					default:
						this.la1Generation[7] = this.generationCounter;
						this.parseClassSpecification();
						this.lexicalBuffer.closeNodeScope(commandNode, true);
						commitPending = false;
						commandNode.setLineNumber(commandToken.beginLine);
						return;
				}
			}
		} catch (Throwable throwable) {
			if (commitPending) {
				this.lexicalBuffer.clearNodeScope(commandNode);
				commitPending = false;
			} else {
				this.lexicalBuffer.popNode();
			}

			if (throwable instanceof RuntimeException) {
				throw (RuntimeException)throwable;
			} else if (throwable instanceof ProguardCommandParseException) {
				throw (ProguardCommandParseException)throwable;
			} else {
				throw (Error)throwable;
			}
		} finally {
			if (commitPending) {
				this.lexicalBuffer.closeNodeScope(commandNode, true);
			}
		}
	}

	private boolean matchesAllMembersStarSemicolon() {
		return this.scanToken(19) ? true : this.scanToken(13);
	}

	public final void parseInclude() {
		Include commandNode = new Include(2);
		boolean commitPending = true;
		this.lexicalBuffer.openNodeScope(commandNode);

		try {
			ProguardToken commandToken = this.consumeToken(46);
			this.commandKind = 46;
			this.parseStringLiteral();
			this.lexicalBuffer.closeNodeScope(commandNode, true);
			commitPending = false;
			commandNode.setLineNumber(commandToken.beginLine);
		} catch (Throwable throwable) {
			if (commitPending) {
				this.lexicalBuffer.clearNodeScope(commandNode);
				commitPending = false;
			} else {
				this.lexicalBuffer.popNode();
			}

			if (throwable instanceof RuntimeException) {
				throw (RuntimeException)throwable;
			}

			if (throwable instanceof ProguardCommandParseException) {
				throw (ProguardCommandParseException)throwable;
			}

			throw (Error)throwable;
		} finally {
			if (commitPending) {
				this.lexicalBuffer.closeNodeScope(commandNode, true);
			}
		}
	}

	private boolean isNextFieldTypePatternPercent() {
		return this.scanToken(18);
	}

	private boolean isNextStringLiteral() {
		return this.scanToken(122);
	}

	public final void parseClassType() {
		ClassTypeNode commandNode = new ClassTypeNode(93);
		boolean commitPending = true;
		this.lexicalBuffer.openNodeScope(commandNode);

		try {
			switch (this.nextToken.kind) {
				case 16:
					this.parseNegation();
					break;
				default:
					this.la1Generation[65] = this.generationCounter;
			}

			switch (this.nextToken.kind) {
				case 28:
					ProguardToken enumToken = this.consumeToken(28);
					this.lexicalBuffer.closeNodeScope(commandNode, true);
					commitPending = false;
					commandNode.setValue(enumToken.image);
					break;
				case 31:
					ProguardToken classToken = this.consumeToken(31);
					this.lexicalBuffer.closeNodeScope(commandNode, true);
					commitPending = false;
					commandNode.setValue(classToken.image);
					break;
				case 53:
					ProguardToken interfaceToken = this.consumeToken(53);
					this.lexicalBuffer.closeNodeScope(commandNode, true);
					commitPending = false;
					commandNode.setValue(interfaceToken.image);
					break;
				case 59:
					ProguardToken annotationInterfaceToken = this.consumeToken(59);
					this.lexicalBuffer.closeNodeScope(commandNode, true);
					commitPending = false;
					commandNode.setValue(annotationInterfaceToken.image);
					break;
				default:
					this.la1Generation[66] = this.generationCounter;
					this.consumeToken(-1);
					throw new ProguardCommandParseException();
			}
		} catch (Throwable throwable) {
			if (commitPending) {
				this.lexicalBuffer.clearNodeScope(commandNode);
				commitPending = false;
			} else {
				this.lexicalBuffer.popNode();
			}

			if (throwable instanceof RuntimeException) {
				throw (RuntimeException)throwable;
			}

			if (throwable instanceof ProguardCommandParseException) {
				throw (ProguardCommandParseException)throwable;
			}

			throw (Error)throwable;
		} finally {
			if (commitPending) {
				this.lexicalBuffer.closeNodeScope(commandNode, true);
			}
		}
	}

	public final void parseFieldTypePattern() {
		FieldTypePatternNode commandNode = new FieldTypePatternNode(86);
		boolean commitPending = true;
		this.lexicalBuffer.openNodeScope(commandNode);

		try {
			switch (this.nextToken.kind) {
				case 18:
					ProguardToken percentToken = this.consumeToken(18);
					this.lexicalBuffer.closeNodeScope(commandNode, true);
					commitPending = false;
					commandNode.setValue(percentToken.image);
					break;
				case 19:
					ProguardToken starToken = this.consumeToken(19);
					this.lexicalBuffer.closeNodeScope(commandNode, true);
					commitPending = false;
					commandNode.setValue(starToken.image);
					break;
				case 24:
					ProguardToken doubleStarToken = this.consumeToken(24);
					this.lexicalBuffer.closeNodeScope(commandNode, true);
					commitPending = false;
					commandNode.setValue(doubleStarToken.image);
					break;
				case 26:
					ProguardToken tripleStarToken = this.consumeToken(26);
					this.lexicalBuffer.closeNodeScope(commandNode, true);
					commitPending = false;
					commandNode.setValue(tripleStarToken.image);
					break;
				case 125:
					ProguardToken nameToken = this.consumeToken(125);
					this.lexicalBuffer.closeNodeScope(commandNode, true);
					commitPending = false;
					commandNode.setValue(nameToken.image);
					break;
				case 126:
					ProguardToken wildcardNameToken = this.consumeToken(126);
					this.lexicalBuffer.closeNodeScope(commandNode, true);
					commitPending = false;
					commandNode.setValue(wildcardNameToken.image);
					break;
				case 127:
					ProguardToken wildcardTypeToken = this.consumeToken(127);
					this.lexicalBuffer.closeNodeScope(commandNode, true);
					commitPending = false;
					commandNode.setValue(wildcardTypeToken.image);
					break;
				default:
					this.la1Generation[59] = this.generationCounter;
					this.consumeToken(-1);
					throw new ProguardCommandParseException();
			}
		} finally {
			if (commitPending) {
				this.lexicalBuffer.closeNodeScope(commandNode, true);
			}
		}
	}

	private boolean isNextTypePatternName() {
		return this.scanToken(125);
	}

	public final void parseAdaptResourceFileContents() {
		AdaptResourceFileContents commandNode = new AdaptResourceFileContents(54);
		boolean commitPending = true;
		this.lexicalBuffer.openNodeScope(commandNode);

		try {
			ProguardToken commandToken;
			commandToken = this.consumeToken(108);
			this.commandKind = 108;
			label77:
			switch (this.nextToken.kind) {
				case 122:
					this.parseStringLiteral();

					while (true) {
						switch (this.nextToken.kind) {
							case 12:
								this.consumeToken(12);
								this.parseStringLiteral();
								break;
							default:
								this.la1Generation[28] = this.generationCounter;
								break label77;
						}
					}
				default:
					this.la1Generation[29] = this.generationCounter;
			}

			this.lexicalBuffer.closeNodeScope(commandNode, true);
			commitPending = false;
			commandNode.setLineNumber(commandToken.beginLine);
		} catch (Throwable throwable) {
			if (commitPending) {
				this.lexicalBuffer.clearNodeScope(commandNode);
				commitPending = false;
			} else {
				this.lexicalBuffer.popNode();
			}

			if (throwable instanceof RuntimeException) {
				throw (RuntimeException)throwable;
			}

			if (throwable instanceof ProguardCommandParseException) {
				throw (ProguardCommandParseException)throwable;
			}

			throw (Error)throwable;
		} finally {
			if (commitPending) {
				this.lexicalBuffer.closeNodeScope(commandNode, true);
			}
		}
	}

	private boolean isNextFieldTypePatternWildcardName() {
		return this.scanToken(126);
	}

	private boolean isNextTypePatternWildcardName() {
		return this.scanToken(126);
	}

	public final void parseMethodSpecification() {
		MethodSpecificationNode commandNode = new MethodSpecificationNode(78);
		boolean commitPending = true;
		this.lexicalBuffer.openNodeScope(commandNode);
		boolean exceptionThrown = false;

		try {
			exceptionThrown = true;
			switch (this.nextToken.kind) {
				case 18:
				case 19:
				case 24:
				case 26:
				case 36:
				case 125:
				case 126:
				case 127:
					if (this.nextIsReturnTypePatternThenMemberName(Integer.MAX_VALUE)) {
						this.parseReturnTypePattern();
					}

					this.parseMemberName();
					this.consumeToken(20);
					switch (this.nextToken.kind) {
						case 18:
						case 19:
						case 24:
						case 26:
						case 27:
						case 125:
						case 126:
						case 127:
							this.parseParameterList();
							break;
						default:
							this.la1Generation[52] = this.generationCounter;
					}

					this.consumeToken(21);
					exceptionThrown = false;
					break;
				case 49:
					this.parseAllMethods();
					exceptionThrown = false;
					break;
				default:
					this.la1Generation[53] = this.generationCounter;
					this.consumeToken(-1);
					throw new ProguardCommandParseException();
			}
		} catch (Throwable throwable) {
			if (true) {
				this.lexicalBuffer.clearNodeScope(commandNode);
				commitPending = false;
			} else {
				this.lexicalBuffer.popNode();
			}
			if (throwable instanceof RuntimeException) {
				throw (RuntimeException)throwable;
			}

			if (throwable instanceof ProguardCommandParseException) {
				throw (ProguardCommandParseException)throwable;
			}

			throw (Error)throwable;
		} finally {
			if (exceptionThrown && commitPending) {
				this.lexicalBuffer.closeNodeScope(commandNode, true);
			}
		}

		this.lexicalBuffer.closeNodeScope(commandNode, true);
	}

	public final void parseKeepClassMemberNames() {
		KeepClassMemberNames commandNode = new KeepClassMemberNames(19);
		boolean commitPending = true;
		this.lexicalBuffer.openNodeScope(commandNode);

		try {
			ProguardToken commandToken = this.consumeToken(98);
			this.commandKind = 98;

			while (true) {
				switch (this.nextToken.kind) {
					case 12:
						this.consumeToken(12);
						this.parseKeepModifiers();
						break;
					default:
						this.la1Generation[10] = this.generationCounter;
						this.parseClassSpecification();
						this.lexicalBuffer.closeNodeScope(commandNode, true);
						commitPending = false;
						commandNode.setLineNumber(commandToken.beginLine);
						return;
				}
			}
		} catch (Throwable throwable) {
			if (commitPending) {
				this.lexicalBuffer.clearNodeScope(commandNode);
				commitPending = false;
			} else {
				this.lexicalBuffer.popNode();
			}

			if (throwable instanceof RuntimeException) {
				throw (RuntimeException)throwable;
			} else if (throwable instanceof ProguardCommandParseException) {
				throw (ProguardCommandParseException)throwable;
			} else {
				throw (Error)throwable;
			}
		} finally {
			if (commitPending) {
				this.lexicalBuffer.closeNodeScope(commandNode, true);
			}
		}
	}

	private void saveLookaheadState(int specIndex, int lookaheadDepth) {
		LookaheadRecord lookaheadRecord;
		for (lookaheadRecord = this.lookaheadRecords[specIndex]; lookaheadRecord.tokenIndex > this.generationCounter; lookaheadRecord = lookaheadRecord.next) {
			if (lookaheadRecord.next == null) {
				lookaheadRecord = lookaheadRecord.next = new LookaheadRecord();
				break;
			}
		}

		lookaheadRecord.tokenIndex = this.generationCounter + lookaheadDepth - this.lookaheadRemaining;
		lookaheadRecord.token = this.token;
		lookaheadRecord.lookaheadLimit = lookaheadDepth;
	}

	public final void parseReturnTypePattern() {
		ReturnTypePatternNode commandNode = new ReturnTypePatternNode(87);
		boolean commitPending = true;
		this.lexicalBuffer.openNodeScope(commandNode);

		try {
			switch (this.nextToken.kind) {
				case 18:
					ProguardToken percentToken = this.consumeToken(18);
					this.lexicalBuffer.closeNodeScope(commandNode, true);
					commitPending = false;
					commandNode.setValue(percentToken.image);
					break;
				case 19:
					ProguardToken starToken = this.consumeToken(19);
					this.lexicalBuffer.closeNodeScope(commandNode, true);
					commitPending = false;
					commandNode.setValue(starToken.image);
					break;
				case 24:
					ProguardToken doubleStarToken = this.consumeToken(24);
					this.lexicalBuffer.closeNodeScope(commandNode, true);
					commitPending = false;
					commandNode.setValue(doubleStarToken.image);
					break;
				case 26:
					ProguardToken tripleStarToken = this.consumeToken(26);
					this.lexicalBuffer.closeNodeScope(commandNode, true);
					commitPending = false;
					commandNode.setValue(tripleStarToken.image);
					break;
				case 125:
					ProguardToken nameToken = this.consumeToken(125);
					this.lexicalBuffer.closeNodeScope(commandNode, true);
					commitPending = false;
					commandNode.setValue(nameToken.image);
					break;
				case 126:
					ProguardToken wildcardNameToken = this.consumeToken(126);
					this.lexicalBuffer.closeNodeScope(commandNode, true);
					commitPending = false;
					commandNode.setValue(wildcardNameToken.image);
					break;
				case 127:
					ProguardToken wildcardTypeToken = this.consumeToken(127);
					this.lexicalBuffer.closeNodeScope(commandNode, true);
					commitPending = false;
					commandNode.setValue(wildcardTypeToken.image);
					break;
				default:
					this.la1Generation[60] = this.generationCounter;
					this.consumeToken(-1);
					throw new ProguardCommandParseException();
			}
		} finally {
			if (commitPending) {
				this.lexicalBuffer.closeNodeScope(commandNode, true);
			}
		}
	}

	private boolean matchesClassModifierExpansion() {
		ProguardToken savedScanPos = this.scanPos;
		if (this.scanToken(16)) {
			this.scanPos = savedScanPos;
		}

		savedScanPos = this.scanPos;
		if (this.scanToken(33)) {
			this.scanPos = savedScanPos;
			if (this.scanToken(32)) {
				this.scanPos = savedScanPos;
				if (this.scanToken(50)) {
					this.scanPos = savedScanPos;
					if (this.scanToken(54)) {
						return true;
					}
				}
			}
		}

		return false;
	}

	public final void parseKeepCode() {
		KeepCode commandNode = new KeepCode(64);
		boolean commitPending = true;
		this.lexicalBuffer.openNodeScope(commandNode);

		try {
			ProguardToken commandToken = this.consumeToken(55);
			this.commandKind = 55;
			this.parseClassSpecification();
			this.lexicalBuffer.closeNodeScope(commandNode, true);
			commitPending = false;
			commandNode.setLineNumber(commandToken.beginLine);
		} catch (Throwable throwable) {
			if (commitPending) {
				this.lexicalBuffer.clearNodeScope(commandNode);
				commitPending = false;
			} else {
				this.lexicalBuffer.popNode();
			}

			if (throwable instanceof RuntimeException) {
				throw (RuntimeException)throwable;
			}

			if (throwable instanceof ProguardCommandParseException) {
				throw (ProguardCommandParseException)throwable;
			}

			throw (Error)throwable;
		} finally {
			if (commitPending) {
				this.lexicalBuffer.closeNodeScope(commandNode, true);
			}
		}
	}

	private boolean matchesArchiveFilters() {
		if (this.isNextStringLiteral()) {
			return true;
		} else {
			ProguardToken savedScanPos;
			do {
				savedScanPos = this.scanPos;
			} while (!this.isNextCommaThenStringLiteral());

			this.scanPos = savedScanPos;
			return this.scanToken(11);
		}
	}

	private boolean isNextFieldTypePatternStart() {
		return this.isNextFieldTypePattern();
	}

	public final void parseClassNameList() {
		ClassNameListNode commandNode = new ClassNameListNode(88);
		boolean commitPending = true;
		this.lexicalBuffer.openNodeScope(commandNode);
		boolean exceptionThrown = false;

		try {
			exceptionThrown = true;
			this.parseClassName();

			label71:
			while (true) {
				switch (this.nextToken.kind) {
					case 12:
						this.consumeToken(12);
						this.parseClassName();
						break;
					default:
						this.la1Generation[61] = this.generationCounter;
						exceptionThrown = false;
						break label71;
				}
			}
		} catch (Throwable throwable) {
			if (true) {
				this.lexicalBuffer.clearNodeScope(commandNode);
				commitPending = false;
			} else {
				this.lexicalBuffer.popNode();
			}
			if (throwable instanceof RuntimeException) {
				throw (RuntimeException)throwable;
			}

			if (throwable instanceof ProguardCommandParseException) {
				throw (ProguardCommandParseException)throwable;
			}

			throw (Error)throwable;
		} finally {
			if (exceptionThrown && commitPending) {
				this.lexicalBuffer.closeNodeScope(commandNode, true);
			}
		}

		this.lexicalBuffer.closeNodeScope(commandNode, true);
	}

	public final void parsePackageObfuscationDictionary() {
		PackageObfuscationDictionary commandNode = new PackageObfuscationDictionary(41);
		boolean commitPending = true;
		this.lexicalBuffer.openNodeScope(commandNode);

		try {
			ProguardToken commandToken = this.consumeToken(118);
			this.commandKind = 118;
			this.parseStringLiteral();
			this.lexicalBuffer.closeNodeScope(commandNode, true);
			commitPending = false;
			commandNode.setLineNumber(commandToken.beginLine);
		} catch (Throwable throwable) {
			if (commitPending) {
				this.lexicalBuffer.clearNodeScope(commandNode);
				commitPending = false;
			} else {
				this.lexicalBuffer.popNode();
			}

			if (throwable instanceof RuntimeException) {
				throw (RuntimeException)throwable;
			}

			if (throwable instanceof ProguardCommandParseException) {
				throw (ProguardCommandParseException)throwable;
			}

			throw (Error)throwable;
		} finally {
			if (commitPending) {
				this.lexicalBuffer.closeNodeScope(commandNode, true);
			}
		}
	}

	private boolean isNextFieldTypePattern() {
		ProguardToken savedScanPos = this.scanPos;
		if (this.isNextFieldTypePatternPercent()) {
			this.scanPos = savedScanPos;
			if (this.isNextFieldTypePatternStar()) {
				this.scanPos = savedScanPos;
				if (this.isNextFieldTypePatternDoubleStar()) {
					this.scanPos = savedScanPos;
					if (this.isNextFieldTypePatternTripleStar()) {
						this.scanPos = savedScanPos;
						if (this.isNextFieldTypePatternName()) {
							this.scanPos = savedScanPos;
							if (this.isNextFieldTypePatternWildcardName()) {
								this.scanPos = savedScanPos;
								if (this.isNextFieldTypePatternWildcardType()) {
									return true;
								}
							}
						}
					}
				}
			}
		}

		return false;
	}

	private boolean isNextFieldTypePatternWildcardType() {
		return this.scanToken(127);
	}

	public final void parseApplyMapping() {
		ApplyMapping commandNode = new ApplyMapping(37);
		boolean commitPending = true;
		this.lexicalBuffer.openNodeScope(commandNode);

		try {
			ProguardToken commandToken = this.consumeToken(69);
			this.commandKind = 69;
			switch (this.nextToken.kind) {
				case 122:
					this.parseStringLiteral();
					break;
				default:
					this.la1Generation[15] = this.generationCounter;
			}

			this.lexicalBuffer.closeNodeScope(commandNode, true);
			commitPending = false;
			commandNode.setLineNumber(commandToken.beginLine);
		} catch (Throwable throwable) {
			if (commitPending) {
				this.lexicalBuffer.clearNodeScope(commandNode);
				commitPending = false;
			} else {
				this.lexicalBuffer.popNode();
			}

			if (throwable instanceof RuntimeException) {
				throw (RuntimeException)throwable;
			}

			if (throwable instanceof ProguardCommandParseException) {
				throw (ProguardCommandParseException)throwable;
			}

			throw (Error)throwable;
		} finally {
			if (commitPending) {
				this.lexicalBuffer.closeNodeScope(commandNode, true);
			}
		}
	}

	private boolean isNextTypePatternStar() {
		return this.scanToken(19);
	}

	public final void parseAdaptClassStrings() {
		AdaptClassStrings commandNode = new AdaptClassStrings(51);
		boolean commitPending = true;
		this.lexicalBuffer.openNodeScope(commandNode);

		try {
			ProguardToken commandToken;
			commandToken = this.consumeToken(90);
			this.commandKind = 90;
			label77:
			switch (this.nextToken.kind) {
				case 122:
					this.parseStringLiteral();

					while (true) {
						switch (this.nextToken.kind) {
							case 12:
								this.consumeToken(12);
								this.parseStringLiteral();
								break;
							default:
								this.la1Generation[24] = this.generationCounter;
								break label77;
						}
					}
				default:
					this.la1Generation[25] = this.generationCounter;
			}

			this.lexicalBuffer.closeNodeScope(commandNode, true);
			commitPending = false;
			commandNode.setLineNumber(commandToken.beginLine);
		} catch (Throwable throwable) {
			if (commitPending) {
				this.lexicalBuffer.clearNodeScope(commandNode);
				commitPending = false;
			} else {
				this.lexicalBuffer.popNode();
			}

			if (throwable instanceof RuntimeException) {
				throw (RuntimeException)throwable;
			}

			if (throwable instanceof ProguardCommandParseException) {
				throw (ProguardCommandParseException)throwable;
			}

			throw (Error)throwable;
		} finally {
			if (commitPending) {
				this.lexicalBuffer.closeNodeScope(commandNode, true);
			}
		}
	}

	public final void parseAllMethods() {
		AllMethodsNode commandNode = new AllMethodsNode(92);
		this.lexicalBuffer.openNodeScope(commandNode);

		try {
			this.consumeToken(49);
		} finally {
			this.lexicalBuffer.closeNodeScope(commandNode, true);
		}
	}

	private boolean isNextAllMethods() {
		return this.isNextMethodsTag();
	}

	public final void parseClassModifier() {
		ClassModifierNode commandNode = new ClassModifierNode(94);
		boolean commitPending = true;
		this.lexicalBuffer.openNodeScope(commandNode);

		try {
			switch (this.nextToken.kind) {
				case 16:
					this.parseNegation();
					break;
				default:
					this.la1Generation[67] = this.generationCounter;
			}

			switch (this.nextToken.kind) {
				case 32:
					ProguardToken finalToken = this.consumeToken(32);
					this.lexicalBuffer.closeNodeScope(commandNode, true);
					commitPending = false;
					commandNode.setValue(finalToken.image);
					break;
				case 33:
					ProguardToken publicToken = this.consumeToken(33);
					this.lexicalBuffer.closeNodeScope(commandNode, true);
					commitPending = false;
					commandNode.setValue(publicToken.image);
					break;
				case 50:
					ProguardToken abstractToken = this.consumeToken(50);
					this.lexicalBuffer.closeNodeScope(commandNode, true);
					commitPending = false;
					commandNode.setValue(abstractToken.image);
					break;
				case 54:
					ProguardToken syntheticToken = this.consumeToken(54);
					this.lexicalBuffer.closeNodeScope(commandNode, true);
					commitPending = false;
					commandNode.setValue(syntheticToken.image);
					break;
				default:
					this.la1Generation[68] = this.generationCounter;
					this.consumeToken(-1);
					throw new ProguardCommandParseException();
			}
		} catch (Throwable throwable) {
			if (commitPending) {
				this.lexicalBuffer.clearNodeScope(commandNode);
				commitPending = false;
			} else {
				this.lexicalBuffer.popNode();
			}

			if (throwable instanceof RuntimeException) {
				throw (RuntimeException)throwable;
			}

			if (throwable instanceof ProguardCommandParseException) {
				throw (ProguardCommandParseException)throwable;
			}

			throw (Error)throwable;
		} finally {
			if (commitPending) {
				this.lexicalBuffer.closeNodeScope(commandNode, true);
			}
		}
	}

	private boolean scanToken(int kind) {
		if (this.scanPos == this.lastPos) {
			this.lookaheadRemaining--;
			if (this.scanPos.next == null) {
				this.lastPos = this.scanPos = this.scanPos.next = this.tokenSource.getNextToken();
			} else {
				this.lastPos = this.scanPos = this.scanPos.next;
			}
		} else {
			this.scanPos = this.scanPos.next;
		}

		if (this.rescanning) {
			int tokenOffset = 0;

			ProguardToken rescannedToken;
			for (rescannedToken = this.token; rescannedToken != null && rescannedToken != this.scanPos; rescannedToken = rescannedToken.next) {
				tokenOffset++;
			}

			if (rescannedToken != null) {
				this.addErrorToken(kind, tokenOffset);
			}
		}

		if (this.scanPos.kind != kind) {
			return true;
		} else if (this.lookaheadRemaining == 0 && this.scanPos == this.lastPos) {
			throw this.lookaheadSuccess;
		} else {
			return false;
		}
	}

	public final void parseParameterList() {
		ParameterListNode commandNode = new ParameterListNode(83);
		boolean commitPending = true;
		this.lexicalBuffer.openNodeScope(commandNode);
		boolean exceptionThrown = false;

		try {
			exceptionThrown = true;
			this.parseParameterPattern();

			label71:
			while (true) {
				switch (this.nextToken.kind) {
					case 12:
						this.consumeToken(12);
						this.parseParameterPattern();
						break;
					default:
						this.la1Generation[56] = this.generationCounter;
						exceptionThrown = false;
						break label71;
				}
			}
		} catch (Throwable throwable) {
			if (true) {
				this.lexicalBuffer.clearNodeScope(commandNode);
				commitPending = false;
			} else {
				this.lexicalBuffer.popNode();
			}
			if (throwable instanceof RuntimeException) {
				throw (RuntimeException)throwable;
			}

			if (throwable instanceof ProguardCommandParseException) {
				throw (ProguardCommandParseException)throwable;
			}

			throw (Error)throwable;
		} finally {
			if (exceptionThrown && commitPending) {
				this.lexicalBuffer.closeNodeScope(commandNode, true);
			}
		}

		this.lexicalBuffer.closeNodeScope(commandNode, true);
	}

	private boolean matchesTypePatternThenFieldsKeyword() {
		ProguardToken savedScanPos = this.scanPos;
		if (this.isNextFieldTypePatternStart()) {
			this.scanPos = savedScanPos;
		}

		return this.isNextFieldsTag();
	}

	public final void parseOverloadAggressively() {
		OverloadAggressively commandNode = new OverloadAggressively(42);
		boolean commitPending = true;
		this.lexicalBuffer.openNodeScope(commandNode);

		try {
			ProguardToken commandToken = this.consumeToken(97);
			this.commandKind = 97;
			this.lexicalBuffer.closeNodeScope(commandNode, true);
			commitPending = false;
			commandNode.setLineNumber(commandToken.beginLine);
		} finally {
			if (commitPending) {
				this.lexicalBuffer.closeNodeScope(commandNode, true);
			}
		}
	}

	private boolean nextIsMethodSpecification(int lookaheadDepth) {
		this.lookaheadRemaining = lookaheadDepth;
		this.lastPos = this.scanPos = this.token;

		try {
			return !this.matchesMethodSpecificationExpansion();
		} catch (TokenMgrError2 ignored) {
		} finally {
			this.saveLookaheadState(2, lookaheadDepth);
		}

		return true;
	}

	static {
		initLa1TokenBits0();
		initLa1TokenBits1();
		initLa1TokenBits2();
		initLa1TokenBits3();
		initLa1TokenBits4();
	}
}
