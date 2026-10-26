package com.zelix.klassmaster.script;

import com.zelix.exceptions.LookaheadSuccess;
import com.zelix.exceptions.ParsingException;
import com.zelix.klassmaster.script.base.AbstractScriptNode;
import com.zelix.klassmaster.script.statements.annotation.*;
import com.zelix.klassmaster.script.statements.clause.*;
import com.zelix.klassmaster.script.statements.command.*;
import com.zelix.klassmaster.script.statements.exclusion.*;
import com.zelix.klassmaster.script.statements.filter.*;
import com.zelix.klassmaster.script.statements.modifier.*;
import com.zelix.klassmaster.script.statements.name.*;
import com.zelix.klassmaster.script.statements.optionValue.*;
import com.zelix.klassmaster.script.statements.parameter.*;
import com.zelix.klassmaster.script.statements.signature.*;
import com.zelix.klassmaster.script.statements.specifier.*;
import com.zelix.klassmaster.script.statements.token.*;
import com.zelix.utils.StringUtils;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

public class ScriptParser implements ZKMScriptNodeTypes, ZKMScriptTokenList {
	private static int[] la1TokenBits0;
	private static int[] la1TokenBits6;
	private static int[] la1TokenBits4;
	private static int[] la1TokenBits1;
	private static int[] la1TokenBits3;
	private static int[] la1TokenBits5;
	private static int[] la1TokenBits2;
	protected LexicalBuffer lexicalBuffer = new LexicalBuffer();
	private final int[] la1Generation = new int[187];
	private final LookaheadRecord[] lookaheadRecords = new LookaheadRecord[28];
	private boolean rescanning = false;
	private int lookaheadGcCounter = 0;
	private final LookaheadSuccess lookaheadSuccess = new LookaheadSuccess();
	private List<int[]> expectedTokenSequences = new ArrayList<>();
	private int errorTokenKind = -1;
	private int[] errorTokenKinds = new int[100];
	public SimpleCharStream inputStream;
	public ZKMScriptTokenManager tokenSource;
	public ZKMScriptToken token;
	public ZKMScriptToken nextToken;
	private int generationCounter;
	private int errorTokenEndPos;
	private int[] errorTokenSequence;
	private ZKMScriptToken lastPos;
	private ZKMScriptToken scanPos;
	private int lookaheadRemaining;
	public int statementKind;

	public ScriptParser(Reader reader) {
		this.inputStream = new SimpleCharStream(reader, 1, 1);
		this.tokenSource = new ZKMScriptTokenManager(this.inputStream);
		this.token = new ZKMScriptToken();
		this.token.next = this.nextToken = this.tokenSource.getNextToken();
		this.generationCounter = 0;

		for (int la1Index = 0; la1Index < 187; la1Index++) {
			this.la1Generation[la1Index] = -1;
		}

		for (int recordIndex = 0; recordIndex < this.lookaheadRecords.length; recordIndex++) {
			this.lookaheadRecords[recordIndex] = new LookaheadRecord();
		}
	}

	private static void initLa1TokenBits4() {
		la1TokenBits4 = new int[]{
			0,
			-989527040,
			-989527040,
			4,
			0,
			33562624,
			0,
			0,
			33562624,
			0,
			33562624,
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
			419072489,
			536870930,
			0,
			536870930,
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
			419072489,
			0,
			0,
			32768,
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
			16384,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			4096,
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
			512,
			0
		};
	}

	private static void initLa1TokenBits2() {
		la1TokenBits2 = new int[]{
			0,
			1078165539,
			1078165539,
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
			82519465,
			0,
			0,
			268435456,
			0,
			268435456,
			268435456,
			0,
			268435456,
			0,
			0,
			268435456,
			0,
			268435456,
			268435456,
			0,
			268435456,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			1,
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
			0,
			0,
			0,
			-2145370112,
			0,
			0,
			0,
			0,
			0,
			0,
			4,
			134217728,
			1321240,
			81994913,
			83308473,
			0,
			81994921,
			4,
			134217728,
			0,
			0,
			8921352,
			4,
			134217728,
			1321240,
			81994913,
			83308473,
			82519465,
			82519465,
			0,
			-2145370112,
			0,
			0,
			0,
			0,
			4096,
			8921352,
			8921352,
			1321240,
			83308473,
			82519465,
			83308473,
			0,
			0,
			0,
			1321240,
			81994913,
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
			67108864,
			0,
			67109888,
			67108864,
			0,
			128,
			131072,
			537001984,
			0,
			0,
			0,
			0,
			81994913,
			0,
			0,
			0,
			0,
			81994921,
			0,
			0,
			0,
			0,
			0,
			81994913,
			81994921,
			0,
			79897761,
			81994913,
			0,
			0,
			0,
			0,
			0,
			81994913,
			81994913,
			81994913,
			0,
			0,
			0,
			81994913,
			81994913,
			0,
			81994913,
			0,
			0,
			81994913,
			0,
			81995425,
			0,
			0,
			0,
			81995425,
			0,
			0,
			81994913,
			0,
			0,
			81994913,
			0,
			0,
			0
		};
	}

	private static void initLa1TokenBits5() {
		la1TokenBits5 = new int[]{
			0,
			-328175882,
			-328175882,
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
			1048576,
			0,
			0,
			0,
			921608,
			310411521,
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
			921608,
			310411521,
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
			16777216,
			65536,
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

	private static void initLa1TokenBits0() {
		la1TokenBits0 = new int[]{
			16384,
			0,
			0,
			0,
			1073741824,
			0,
			16842752,
			1073741824,
			0,
			16842752,
			0,
			33554432,
			0,
			0,
			33554432,
			33554432,
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
			444596224,
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
			16384,
			0,
			0,
			16384,
			0,
			0,
			16384,
			0,
			0,
			16384,
			0,
			0,
			16384,
			0,
			16384,
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
			33554432,
			310378496,
			444596224,
			0,
			310378496,
			0,
			0,
			1048576,
			0,
			33554432,
			0,
			0,
			33554432,
			310378496,
			444596224,
			310378496,
			444596224,
			0,
			0,
			1048576,
			1048576,
			0,
			0,
			0,
			0,
			0,
			0,
			444596224,
			444596224,
			444596224,
			33554432,
			0,
			0,
			33554432,
			310378496,
			436207616,
			436207616,
			1048576,
			436207616,
			1048576,
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
			436207616,
			33554432,
			0,
			0,
			310378496,
			67108864,
			33554432,
			0,
			0,
			310378496,
			2097152,
			67108864,
			33554432,
			0,
			0,
			8388608,
			8388608,
			2097152,
			8388608,
			310378496,
			83886080,
			83886080,
			33554432,
			0,
			0,
			8388608,
			0,
			310378496,
			33554432,
			0,
			0,
			8388608,
			444596224,
			0,
			444596224,
			0,
			0,
			444596224,
			0,
			310378496,
			33554432,
			0,
			0,
			8388608,
			1048576,
			436207616,
			8388608,
			2097152,
			131072,
			0,
			0,
			0,
			0
		};
	}

	private static void initLa1TokenBits1() {
		la1TokenBits1 = new int[]{
			0,
			24720,
			24720,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			12,
			12,
			0,
			0,
			64,
			64,
			64,
			64,
			64,
			64,
			64,
			64,
			64,
			64,
			64,
			64,
			64,
			64,
			64,
			-1289884240,
			64,
			64,
			0,
			64,
			0,
			0,
			64,
			0,
			64,
			64,
			0,
			64,
			0,
			0,
			64,
			0,
			64,
			64,
			64,
			64,
			0,
			64,
			0,
			0,
			64,
			0,
			0,
			64,
			0,
			0,
			64,
			0,
			0,
			64,
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
			6176,
			0,
			64,
			0,
			0,
			236192000,
			-1323700832,
			-1084363360,
			0,
			-1323700816,
			0,
			0,
			0,
			16777216,
			33816832,
			0,
			0,
			236192000,
			-1323700832,
			-1082266208,
			-1289884240,
			-1289884240,
			0,
			0,
			0,
			0,
			0,
			0,
			524288,
			33816832,
			33816832,
			236192000,
			-1084363360,
			-1289884240,
			-1084363360,
			0,
			4,
			8,
			236192000,
			-1323700832,
			0,
			0,
			0,
			0,
			0,
			100352,
			536872960,
			-1610610688,
			536872960,
			0,
			536905728,
			536872960,
			537004544,
			536872960,
			536872960,
			268436480,
			268436480,
			268436480,
			0,
			0,
			4,
			8,
			-1323700816,
			0,
			0,
			4,
			8,
			-1323700816,
			0,
			0,
			0,
			4,
			8,
			-1323700816,
			-1323700816,
			0,
			-1323700832,
			-1323700832,
			0,
			0,
			0,
			4,
			8,
			-1323700832,
			-1323700832,
			-1323700832,
			0,
			4,
			8,
			-1323700832,
			-1323700831,
			1073741824,
			-1323700831,
			1073741824,
			0,
			-1323700831,
			2097152,
			-1319506528,
			0,
			4,
			8,
			-1319506528,
			0,
			0,
			-1323700831,
			0,
			0,
			-1323700832,
			131584,
			131584,
			131584
		};
	}

	private static void initLa1TokenBits3() {
		la1TokenBits3 = new int[]{
			0,
			-1073706988,
			-1073706988,
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
			2,
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
			520046496,
			16777216,
			0,
			16777216,
			0,
			0,
			0,
			0,
			0,
			8,
			0,
			8,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			8,
			0,
			8,
			2,
			2,
			0,
			520046496,
			0,
			0,
			16384,
			524416,
			0,
			0,
			0,
			8,
			8,
			2,
			8,
			0,
			0,
			0,
			8,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			64,
			0,
			4096,
			8192,
			0,
			64,
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
			2,
			0,
			0,
			0,
			0,
			0,
			2,
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
			536870912,
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
			1
		};
	}

	private static void initLa1TokenBits6() {
		la1TokenBits6 = new int[]{
			0,
			55787,
			55787,
			0,
			0,
			0,
			262144,
			0,
			0,
			262144,
			0,
			262144,
			0,
			0,
			262144,
			262144,
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
			31719424,
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
			2,
			0,
			0,
			256,
			0,
			0,
			0,
			0,
			1310720,
			1310720,
			16,
			516,
			0,
			262144,
			0,
			0,
			1048576,
			0,
			0,
			0,
			0,
			6291456,
			6291456,
			262144,
			6291456,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			6291456,
			39845888,
			6553600,
			23330816,
			16,
			516,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			6291456,
			31719424,
			6291456,
			0,
			0,
			0,
			0,
			6291456,
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
			73728,
			0,
			0,
			0,
			0,
			0,
			14680064,
			0,
			0,
			0,
			0,
			6291456,
			0,
			0,
			0,
			0,
			0,
			14680064,
			6291456,
			0,
			6291456,
			6291456,
			0,
			0,
			0,
			0,
			0,
			6291456,
			2097152,
			6291456,
			0,
			0,
			0,
			6291456,
			2097152,
			0,
			2097152,
			0,
			0,
			2097152,
			33554432,
			6291456,
			0,
			0,
			0,
			6291456,
			0,
			0,
			2097152,
			0,
			0,
			2097152,
			0,
			0,
			0
		};
	}

	public final void parseQualifiedClassName() {
		QualifiedClassName qualifiedClassName = new QualifiedClassName(217);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(qualifiedClassName);

		try {
			if (this.matchesPackageNameInQualifiedClassName(Integer.MAX_VALUE)) {
				this.parsePackageName();
			}

			this.parseClassName();
		} catch (Throwable throwable) {
			if (true) {
				this.lexicalBuffer.discardNode(qualifiedClassName);
				nodeOpen = false;
			} else {
				this.lexicalBuffer.pop();
			}
			if (throwable instanceof RuntimeException) {
				throw (RuntimeException)throwable;
			}

			if (throwable instanceof ParsingException) {
				throw (ParsingException)throwable;
			}

			throw (Error)throwable;
		} finally {
			if (true) {
				this.lexicalBuffer.endNode(qualifiedClassName, true);
			}
		}
	}

	private boolean memberSpecifierModifierKeywordLookahead() {
		return this.memberModifierListLookahead();
	}

	public final void parseKeepGenericsParameter() {
		KeepGenericsParameter keepGenericsParameter = new KeepGenericsParameter(107);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(keepGenericsParameter);

		try {
			try {
				ZKMScriptToken keywordToken = this.consumeParameter(114);
				this.consumeParameter(15);
				this.parseBoolean();
				this.lexicalBuffer.endNode(keepGenericsParameter, true);
				nodeOpen = false;
				keepGenericsParameter.setParameterName(keywordToken.image);
			} catch (Throwable throwable) {
				if (nodeOpen) {
					this.lexicalBuffer.discardNode(keepGenericsParameter);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			}
		} catch (Throwable throwable2) {
			if (nodeOpen) {
				this.lexicalBuffer.endNode(keepGenericsParameter, true);
			}

			throw throwable2;
		}
	}

	public final void parseLongEncryptionType() {
		LongEncryptionType longEncryptionType = new LongEncryptionType(166);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(longEncryptionType);

		try {
			switch (this.nextToken.kind) {
				case 43:
					ZKMScriptToken noneToken = this.consumeParameter(43);
					this.lexicalBuffer.endNode(longEncryptionType, true);
					nodeOpen = false;
					longEncryptionType.setText(noneToken.image);
					return;
				case 61:
					ZKMScriptToken normalToken = this.consumeParameter(61);
					this.lexicalBuffer.endNode(longEncryptionType, true);
					nodeOpen = false;
					longEncryptionType.setText(normalToken.image);
					return;
				default:
					this.la1Generation[130] = this.generationCounter;
					this.consumeParameter(-1);
					throw new ParsingException();
			}
		} finally {
			if (nodeOpen) {
				this.lexicalBuffer.endNode(longEncryptionType, true);
			}
		}
	}

	public final void parseEncryptionType() {
		EncryptionType encryptionType = new EncryptionType(164);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(encryptionType);

		try {
			switch (this.nextToken.kind) {
				case 41:
					ZKMScriptToken trueToken = this.consumeParameter(41);
					this.lexicalBuffer.endNode(encryptionType, true);
					nodeOpen = false;
					encryptionType.setText(trueToken.image);
					return;
				case 43:
					ZKMScriptToken noneToken = this.consumeParameter(43);
					this.lexicalBuffer.endNode(encryptionType, true);
					nodeOpen = false;
					encryptionType.setText(noneToken.image);
					return;
				case 49:
					ZKMScriptToken falseToken = this.consumeParameter(49);
					this.lexicalBuffer.endNode(encryptionType, true);
					nodeOpen = false;
					encryptionType.setText(falseToken.image);
					return;
				case 61:
					ZKMScriptToken normalToken = this.consumeParameter(61);
					this.lexicalBuffer.endNode(encryptionType, true);
					nodeOpen = false;
					encryptionType.setText(normalToken.image);
					return;
				case 74:
					ZKMScriptToken enhancedToken = this.consumeParameter(74);
					this.lexicalBuffer.endNode(encryptionType, true);
					nodeOpen = false;
					encryptionType.setText(enhancedToken.image);
					return;
				case 90:
					ZKMScriptToken aggressiveToken = this.consumeParameter(90);
					this.lexicalBuffer.endNode(encryptionType, true);
					nodeOpen = false;
					encryptionType.setText(aggressiveToken.image);
					return;
				case 102:
					ZKMScriptToken flowObfuscateToken = this.consumeParameter(102);
					this.lexicalBuffer.endNode(encryptionType, true);
					nodeOpen = false;
					encryptionType.setText(flowObfuscateToken.image);
					return;
				default:
					this.la1Generation[128] = this.generationCounter;
					this.consumeParameter(-1);
					throw new ParsingException();
			}
		} finally {
			if (nodeOpen) {
				this.lexicalBuffer.endNode(encryptionType, true);
			}
		}
	}

	public final void parseMixedCaseClassNamesParameter() {
		MixedCaseClassNamesParameter mixedCaseClassNames = new MixedCaseClassNamesParameter(120);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(mixedCaseClassNames);

		try {
			try {
				ZKMScriptToken keywordToken = this.consumeParameter(136);
				this.consumeParameter(15);
				this.parseBooleanOrIfInArchive();
				this.lexicalBuffer.endNode(mixedCaseClassNames, true);
				nodeOpen = false;
				mixedCaseClassNames.setParameterName(keywordToken.image);
			} catch (Throwable throwable) {
				if (nodeOpen) {
					this.lexicalBuffer.discardNode(mixedCaseClassNames);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			}
		} catch (Throwable throwable2) {
			if (nodeOpen) {
				this.lexicalBuffer.endNode(mixedCaseClassNames, true);
			}

			throw throwable2;
		}
	}

	public final void parseNegatedMemberModifier() {
		NegatedMemberModifier negatedMemberModifier = new NegatedMemberModifier(143);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(negatedMemberModifier);

		try {
			this.consumeParameter(25);
			this.parseMemberModifierHelper();
			this.lexicalBuffer.endNode(negatedMemberModifier, true);
		} catch (Throwable throwable) {
			Throwable caughtThrowable = throwable;

			try {
				if (true) {
					this.lexicalBuffer.discardNode(negatedMemberModifier);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}
				if (caughtThrowable instanceof RuntimeException) {
					throw (RuntimeException)caughtThrowable;
				} else if (caughtThrowable instanceof ParsingException) {
					throw (ParsingException)caughtThrowable;
				} else {
					throw (Error)caughtThrowable;
				}
			} catch (Throwable throwable2) {
				if (nodeOpen) {
					this.lexicalBuffer.endNode(negatedMemberModifier, true);
				}

				throw throwable2;
			}
		}
	}

	public final void parseQualifiedType() {
		QualifiedType qualifiedType = new QualifiedType(215);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(qualifiedType);

		try {
			this.parseType();

			while (true) {
				switch (this.nextToken.kind) {
					case 21:
						this.consumeParameter(21);
						this.parseType();
						break;
					default:
						this.la1Generation[181] = this.generationCounter;

						while (true) {
							switch (this.nextToken.kind) {
								case 17:
									this.parseArrayLevel();
									break;
								default:
									this.la1Generation[182] = this.generationCounter;
									this.lexicalBuffer.endNode(qualifiedType, true);
									return;
							}
						}
				}
			}
		} catch (Throwable throwable) {
			Throwable caughtThrowable = throwable;

			try {
				if (true) {
					this.lexicalBuffer.discardNode(qualifiedType);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}
				if (caughtThrowable instanceof RuntimeException) {
					throw (RuntimeException)caughtThrowable;
				} else if (caughtThrowable instanceof ParsingException) {
					throw (ParsingException)caughtThrowable;
				} else {
					throw (Error)caughtThrowable;
				}
			} catch (Throwable throwable2) {
				if (nodeOpen) {
					this.lexicalBuffer.endNode(qualifiedType, true);
				}

				throw throwable2;
			}
		}
	}

	public final void parsePrint() {
		PrintStatement print = new PrintStatement(82);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(print);

		try {
			try {
				ZKMScriptToken keywordToken = this.consumeParameter(46);
				this.statementKind = 46;
				this.parseStringLiteral();
				this.consumeParameter(19);
				this.lexicalBuffer.endNode(print, true);
				nodeOpen = false;
				print.setLineNumber(keywordToken.beginLine);
			} catch (Throwable throwable) {
				if (nodeOpen) {
					this.lexicalBuffer.discardNode(print);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			}
		} catch (Throwable throwable2) {
			if (nodeOpen) {
				this.lexicalBuffer.endNode(print, true);
			}

			throw throwable2;
		}
	}

	public final void parseResetLongEncryptionExclusions() {
		ResetLongEncryptionExclusionsStatement resetLongEncryptionExclusions = new ResetLongEncryptionExclusionsStatement(72);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(resetLongEncryptionExclusions);

		try {
			ZKMScriptToken keywordToken = this.consumeParameter(190);
			this.statementKind = 190;
			this.consumeParameter(19);
			this.lexicalBuffer.endNode(resetLongEncryptionExclusions, true);
			nodeOpen = false;
			resetLongEncryptionExclusions.setLineNumber(keywordToken.beginLine);
		} catch (Throwable throwable) {
			if (nodeOpen) {
				this.lexicalBuffer.endNode(resetLongEncryptionExclusions, true);
			}

			throw throwable;
		}
	}

	public final void parseMethodSignature() {
		MethodSignature methodSignature = new MethodSignature(206);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(methodSignature);

		try {
			this.parseComplexMethodSpecifier();
			this.consumeParameter(28);
			switch (this.nextToken.kind) {
				case 23:
				case 25:
				case 27:
				case 28:
				case 32:
				case 37:
				case 39:
				case 40:
				case 42:
				case 43:
				case 45:
				case 46:
				case 47:
				case 48:
				case 51:
				case 52:
				case 56:
				case 60:
				case 61:
				case 63:
				case 64:
				case 69:
				case 71:
				case 74:
				case 77:
				case 80:
				case 81:
				case 85:
				case 86:
				case 87:
				case 90:
				case 213:
					this.parseMethodArguments();
					break;
				default:
					this.la1Generation[168] = this.generationCounter;
			}

			this.consumeParameter(29);
			switch (this.nextToken.kind) {
				case 62:
					this.parseThrowsClause();
					break;
				default:
					this.la1Generation[169] = this.generationCounter;
			}

			switch (this.nextToken.kind) {
				case 125:
					this.parsePlusSignatureClasses();
					break;
				default:
					this.la1Generation[170] = this.generationCounter;
			}

			this.lexicalBuffer.endNode(methodSignature, true);
		} catch (Throwable throwable) {
			Throwable caughtThrowable = throwable;

			try {
				if (true) {
					this.lexicalBuffer.discardNode(methodSignature);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}
				if (caughtThrowable instanceof RuntimeException) {
					throw (RuntimeException)caughtThrowable;
				} else if (caughtThrowable instanceof ParsingException) {
					throw (ParsingException)caughtThrowable;
				} else {
					throw (Error)caughtThrowable;
				}
			} catch (Throwable throwable2) {
				if (nodeOpen) {
					this.lexicalBuffer.endNode(methodSignature, true);
				}

				throw throwable2;
			}
		}
	}

	public final void parseMethodName() {
		MethodName methodName = new MethodName(212);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(methodName);

		try {
			switch (this.nextToken.kind) {
				case 23:
					ZKMScriptToken starToken = this.consumeParameter(23);
					methodName.setText(starToken.image);
					this.lexicalBuffer.endNode(methodName, true);
					nodeOpen = false;
					starToken.setDescriptionIndex(5);
					return;
				case 37:
					ZKMScriptToken allToken = this.consumeParameter(37);
					methodName.setText(allToken.image);
					this.lexicalBuffer.endNode(methodName, true);
					nodeOpen = false;
					allToken.setDescriptionIndex(5);
					return;
				case 39:
					ZKMScriptToken openToken = this.consumeParameter(39);
					methodName.setText(openToken.image);
					this.lexicalBuffer.endNode(methodName, true);
					nodeOpen = false;
					openToken.setDescriptionIndex(5);
					return;
				case 40:
					ZKMScriptToken enumToken = this.consumeParameter(40);
					methodName.setText(enumToken.image);
					this.lexicalBuffer.endNode(methodName, true);
					nodeOpen = false;
					enumToken.setDescriptionIndex(5);
					return;
				case 42:
					ZKMScriptToken keepToken = this.consumeParameter(42);
					methodName.setText(keepToken.image);
					this.lexicalBuffer.endNode(methodName, true);
					nodeOpen = false;
					keepToken.setDescriptionIndex(5);
					return;
				case 43:
					ZKMScriptToken noneToken = this.consumeParameter(43);
					methodName.setText(noneToken.image);
					this.lexicalBuffer.endNode(methodName, true);
					nodeOpen = false;
					noneToken.setDescriptionIndex(5);
					return;
				case 45:
					ZKMScriptToken trimToken = this.consumeParameter(45);
					methodName.setText(trimToken.image);
					this.lexicalBuffer.endNode(methodName, true);
					nodeOpen = false;
					trimToken.setDescriptionIndex(5);
					return;
				case 46:
					ZKMScriptToken printToken = this.consumeParameter(46);
					methodName.setText(printToken.image);
					this.lexicalBuffer.endNode(methodName, true);
					nodeOpen = false;
					printToken.setDescriptionIndex(5);
					return;
				case 47:
					ZKMScriptToken lightToken = this.consumeParameter(47);
					methodName.setText(lightToken.image);
					this.lexicalBuffer.endNode(methodName, true);
					nodeOpen = false;
					lightToken.setDescriptionIndex(5);
					return;
				case 48:
					ZKMScriptToken heavyToken = this.consumeParameter(48);
					methodName.setText(heavyToken.image);
					this.lexicalBuffer.endNode(methodName, true);
					nodeOpen = false;
					heavyToken.setDescriptionIndex(5);
					return;
				case 51:
					ZKMScriptToken asciiToken = this.consumeParameter(51);
					methodName.setText(asciiToken.image);
					this.lexicalBuffer.endNode(methodName, true);
					nodeOpen = false;
					asciiToken.setDescriptionIndex(5);
					return;
				case 52:
					ZKMScriptToken bridgeToken = this.consumeParameter(52);
					methodName.setText(bridgeToken.image);
					this.lexicalBuffer.endNode(methodName, true);
					nodeOpen = false;
					bridgeToken.setDescriptionIndex(5);
					return;
				case 54:
					ZKMScriptToken initToken = this.consumeParameter(54);
					methodName.setText(initToken.image);
					this.lexicalBuffer.endNode(methodName, true);
					nodeOpen = false;
					initToken.setDescriptionIndex(5);
					return;
				case 56:
					ZKMScriptToken searchToken = this.consumeParameter(56);
					methodName.setText(searchToken.image);
					this.lexicalBuffer.endNode(methodName, true);
					nodeOpen = false;
					searchToken.setDescriptionIndex(5);
					return;
				case 60:
					ZKMScriptToken deleteToken = this.consumeParameter(60);
					methodName.setText(deleteToken.image);
					this.lexicalBuffer.endNode(methodName, true);
					nodeOpen = false;
					deleteToken.setDescriptionIndex(5);
					return;
				case 61:
					ZKMScriptToken normalToken = this.consumeParameter(61);
					methodName.setText(normalToken.image);
					this.lexicalBuffer.endNode(methodName, true);
					nodeOpen = false;
					normalToken.setDescriptionIndex(5);
					return;
				case 63:
					ZKMScriptToken randomToken = this.consumeParameter(63);
					methodName.setText(randomToken.image);
					this.lexicalBuffer.endNode(methodName, true);
					nodeOpen = false;
					randomToken.setDescriptionIndex(5);
					return;
				case 64:
					ZKMScriptToken excludeToken = this.consumeParameter(64);
					methodName.setText(excludeToken.image);
					this.lexicalBuffer.endNode(methodName, true);
					nodeOpen = false;
					excludeToken.setDescriptionIndex(5);
					return;
				case 69:
					ZKMScriptToken executeToken = this.consumeParameter(69);
					methodName.setText(executeToken.image);
					this.lexicalBuffer.endNode(methodName, true);
					nodeOpen = false;
					executeToken.setDescriptionIndex(5);
					return;
				case 71:
					ZKMScriptToken scrambleToken = this.consumeParameter(71);
					methodName.setText(scrambleToken.image);
					this.lexicalBuffer.endNode(methodName, true);
					nodeOpen = false;
					scrambleToken.setDescriptionIndex(5);
					return;
				case 73:
					ZKMScriptToken clinitToken = this.consumeParameter(73);
					methodName.setText(clinitToken.image);
					this.lexicalBuffer.endNode(methodName, true);
					nodeOpen = false;
					clinitToken.setDescriptionIndex(5);
					return;
				case 74:
					ZKMScriptToken enhancedToken = this.consumeParameter(74);
					methodName.setText(enhancedToken.image);
					this.lexicalBuffer.endNode(methodName, true);
					nodeOpen = false;
					enhancedToken.setDescriptionIndex(5);
					return;
				case 77:
					ZKMScriptToken syntheticToken = this.consumeParameter(77);
					methodName.setText(syntheticToken.image);
					this.lexicalBuffer.endNode(methodName, true);
					nodeOpen = false;
					syntheticToken.setDescriptionIndex(5);
					return;
				case 80:
					ZKMScriptToken classpathToken = this.consumeParameter(80);
					methodName.setText(classpathToken.image);
					this.lexicalBuffer.endNode(methodName, true);
					nodeOpen = false;
					classpathToken.setDescriptionIndex(5);
					return;
				case 81:
					ZKMScriptToken obfuscateToken = this.consumeParameter(81);
					methodName.setText(obfuscateToken.image);
					this.lexicalBuffer.endNode(methodName, true);
					nodeOpen = false;
					obfuscateToken.setDescriptionIndex(5);
					return;
				case 85:
					ZKMScriptToken randomizeToken = this.consumeParameter(85);
					methodName.setText(randomizeToken.image);
					this.lexicalBuffer.endNode(methodName, true);
					nodeOpen = false;
					randomizeToken.setDescriptionIndex(5);
					return;
				case 86:
					ZKMScriptToken groupingsToken = this.consumeParameter(86);
					methodName.setText(groupingsToken.image);
					this.lexicalBuffer.endNode(methodName, true);
					nodeOpen = false;
					groupingsToken.setDescriptionIndex(5);
					return;
				case 87:
					ZKMScriptToken annotationToken = this.consumeParameter(87);
					methodName.setText(annotationToken.image);
					this.lexicalBuffer.endNode(methodName, true);
					nodeOpen = false;
					annotationToken.setDescriptionIndex(5);
					return;
				case 90:
					ZKMScriptToken aggressiveToken = this.consumeParameter(90);
					methodName.setText(aggressiveToken.image);
					this.lexicalBuffer.endNode(methodName, true);
					nodeOpen = false;
					aggressiveToken.setDescriptionIndex(5);
					return;
				case 213:
					ZKMScriptToken nameToken = this.consumeParameter(213);
					methodName.setText(nameToken.image);
					this.lexicalBuffer.endNode(methodName, true);
					nodeOpen = false;
					nameToken.setDescriptionIndex(5);
					return;
				case 214:
					ZKMScriptToken wildcardNameToken = this.consumeParameter(214);
					methodName.setText(wildcardNameToken.image);
					this.lexicalBuffer.endNode(methodName, true);
					nodeOpen = false;
					wildcardNameToken.setDescriptionIndex(5);
					return;
				default:
					this.la1Generation[177] = this.generationCounter;
					this.consumeParameter(-1);
					throw new ParsingException();
			}
		} finally {
			if (nodeOpen) {
				this.lexicalBuffer.endNode(methodName, true);
			}
		}
	}

	public final void parseClassName() {
		ClassName className = new ClassName(198);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(className);

		try {
			switch (this.nextToken.kind) {
				case 23:
					ZKMScriptToken starToken = this.consumeParameter(23);
					className.setText(starToken.image);
					this.lexicalBuffer.endNode(className, true);
					nodeOpen = false;
					starToken.setDescriptionIndex(3);
					return;
				case 37:
					ZKMScriptToken allToken = this.consumeParameter(37);
					className.setText(allToken.image);
					this.lexicalBuffer.endNode(className, true);
					nodeOpen = false;
					allToken.setDescriptionIndex(3);
					return;
				case 39:
					ZKMScriptToken openToken = this.consumeParameter(39);
					className.setText(openToken.image);
					this.lexicalBuffer.endNode(className, true);
					nodeOpen = false;
					openToken.setDescriptionIndex(3);
					return;
				case 40:
					ZKMScriptToken enumToken = this.consumeParameter(40);
					className.setText(enumToken.image);
					this.lexicalBuffer.endNode(className, true);
					nodeOpen = false;
					enumToken.setDescriptionIndex(3);
					return;
				case 42:
					ZKMScriptToken keepToken = this.consumeParameter(42);
					className.setText(keepToken.image);
					this.lexicalBuffer.endNode(className, true);
					nodeOpen = false;
					keepToken.setDescriptionIndex(3);
					return;
				case 43:
					ZKMScriptToken noneToken = this.consumeParameter(43);
					className.setText(noneToken.image);
					this.lexicalBuffer.endNode(className, true);
					nodeOpen = false;
					noneToken.setDescriptionIndex(3);
					return;
				case 45:
					ZKMScriptToken trimToken = this.consumeParameter(45);
					className.setText(trimToken.image);
					this.lexicalBuffer.endNode(className, true);
					nodeOpen = false;
					trimToken.setDescriptionIndex(3);
					return;
				case 46:
					ZKMScriptToken printToken = this.consumeParameter(46);
					className.setText(printToken.image);
					this.lexicalBuffer.endNode(className, true);
					nodeOpen = false;
					printToken.setDescriptionIndex(3);
					return;
				case 47:
					ZKMScriptToken lightToken = this.consumeParameter(47);
					className.setText(lightToken.image);
					this.lexicalBuffer.endNode(className, true);
					nodeOpen = false;
					lightToken.setDescriptionIndex(3);
					return;
				case 48:
					ZKMScriptToken heavyToken = this.consumeParameter(48);
					className.setText(heavyToken.image);
					this.lexicalBuffer.endNode(className, true);
					nodeOpen = false;
					heavyToken.setDescriptionIndex(3);
					return;
				case 51:
					ZKMScriptToken asciiToken = this.consumeParameter(51);
					this.consumeParameter(21);
					className.setText(asciiToken.image);
					this.lexicalBuffer.endNode(className, true);
					nodeOpen = false;
					asciiToken.setDescriptionIndex(3);
					return;
				case 52:
					ZKMScriptToken bridgeToken = this.consumeParameter(52);
					className.setText(bridgeToken.image);
					this.lexicalBuffer.endNode(className, true);
					nodeOpen = false;
					bridgeToken.setDescriptionIndex(3);
					return;
				case 56:
					ZKMScriptToken searchToken = this.consumeParameter(56);
					className.setText(searchToken.image);
					this.lexicalBuffer.endNode(className, true);
					nodeOpen = false;
					searchToken.setDescriptionIndex(3);
					return;
				case 60:
					ZKMScriptToken deleteToken = this.consumeParameter(60);
					className.setText(deleteToken.image);
					this.lexicalBuffer.endNode(className, true);
					nodeOpen = false;
					deleteToken.setDescriptionIndex(3);
					return;
				case 61:
					ZKMScriptToken normalToken = this.consumeParameter(61);
					className.setText(normalToken.image);
					this.lexicalBuffer.endNode(className, true);
					nodeOpen = false;
					normalToken.setDescriptionIndex(3);
					return;
				case 63:
					ZKMScriptToken randomToken = this.consumeParameter(63);
					className.setText(randomToken.image);
					this.lexicalBuffer.endNode(className, true);
					nodeOpen = false;
					randomToken.setDescriptionIndex(3);
					return;
				case 64:
					ZKMScriptToken excludeToken = this.consumeParameter(64);
					className.setText(excludeToken.image);
					this.lexicalBuffer.endNode(className, true);
					nodeOpen = false;
					excludeToken.setDescriptionIndex(3);
					return;
				case 69:
					ZKMScriptToken executeToken = this.consumeParameter(69);
					className.setText(executeToken.image);
					this.lexicalBuffer.endNode(className, true);
					nodeOpen = false;
					executeToken.setDescriptionIndex(3);
					return;
				case 71:
					ZKMScriptToken scrambleToken = this.consumeParameter(71);
					className.setText(scrambleToken.image);
					this.lexicalBuffer.endNode(className, true);
					nodeOpen = false;
					scrambleToken.setDescriptionIndex(3);
					return;
				case 74:
					ZKMScriptToken enhancedToken = this.consumeParameter(74);
					className.setText(enhancedToken.image);
					this.lexicalBuffer.endNode(className, true);
					nodeOpen = false;
					enhancedToken.setDescriptionIndex(3);
					return;
				case 77:
					ZKMScriptToken syntheticToken = this.consumeParameter(77);
					className.setText(syntheticToken.image);
					this.lexicalBuffer.endNode(className, true);
					nodeOpen = false;
					syntheticToken.setDescriptionIndex(3);
					return;
				case 80:
					ZKMScriptToken classpathToken = this.consumeParameter(80);
					className.setText(classpathToken.image);
					this.lexicalBuffer.endNode(className, true);
					nodeOpen = false;
					classpathToken.setDescriptionIndex(3);
					return;
				case 81:
					ZKMScriptToken obfuscateToken = this.consumeParameter(81);
					className.setText(obfuscateToken.image);
					this.lexicalBuffer.endNode(className, true);
					nodeOpen = false;
					obfuscateToken.setDescriptionIndex(3);
					return;
				case 85:
					ZKMScriptToken randomizeToken = this.consumeParameter(85);
					className.setText(randomizeToken.image);
					this.lexicalBuffer.endNode(className, true);
					nodeOpen = false;
					randomizeToken.setDescriptionIndex(3);
					return;
				case 86:
					ZKMScriptToken groupingsToken = this.consumeParameter(86);
					className.setText(groupingsToken.image);
					this.lexicalBuffer.endNode(className, true);
					nodeOpen = false;
					groupingsToken.setDescriptionIndex(3);
					return;
				case 87:
					ZKMScriptToken annotationToken = this.consumeParameter(87);
					className.setText(annotationToken.image);
					this.lexicalBuffer.endNode(className, true);
					nodeOpen = false;
					annotationToken.setDescriptionIndex(3);
					return;
				case 90:
					ZKMScriptToken aggressiveToken = this.consumeParameter(90);
					className.setText(aggressiveToken.image);
					this.lexicalBuffer.endNode(className, true);
					nodeOpen = false;
					aggressiveToken.setDescriptionIndex(3);
					return;
				case 97:
					ZKMScriptToken packageInfoToken = this.consumeParameter(97);
					className.setText(packageInfoToken.image);
					this.lexicalBuffer.endNode(className, true);
					nodeOpen = false;
					packageInfoToken.setDescriptionIndex(3);
					return;
				case 213:
					ZKMScriptToken nameToken = this.consumeParameter(213);
					className.setText(nameToken.image);
					this.lexicalBuffer.endNode(className, true);
					nodeOpen = false;
					nameToken.setDescriptionIndex(3);
					return;
				case 214:
					ZKMScriptToken wildcardNameToken = this.consumeParameter(214);
					className.setText(wildcardNameToken.image);
					this.lexicalBuffer.endNode(className, true);
					nodeOpen = false;
					wildcardNameToken.setDescriptionIndex(3);
					return;
				default:
					this.la1Generation[159] = this.generationCounter;
					this.consumeParameter(-1);
					throw new ParsingException();
			}
		} finally {
			if (nodeOpen) {
				this.lexicalBuffer.endNode(className, true);
			}
		}
	}

	public final void parseStringLiteral() {
		StringLiteral stringLiteral = new StringLiteral(170);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(stringLiteral);

		try {
			ZKMScriptToken stringLiteralToken = this.consumeParameter(210);
			this.lexicalBuffer.endNode(stringLiteral, true);
			nodeOpen = false;
			String literalText = stringLiteralToken.image;
			literalText = StringUtils.replace(literalText.substring(1, literalText.length() - 1), "\"\"", "\"");
			stringLiteral.setText(literalText);
		} catch (Throwable throwable) {
			if (nodeOpen) {
				this.lexicalBuffer.endNode(stringLiteral, true);
			}

			throw throwable;
		}
	}

	public final void parseResetMethodParameterChangesExclusions() {
		ResetMethodParameterChangesExclusionsStatement resetMethodParameterChangesExclusions = new ResetMethodParameterChangesExclusionsStatement(78);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(resetMethodParameterChangesExclusions);

		try {
			ZKMScriptToken keywordToken = this.consumeParameter(206);
			this.statementKind = 206;
			this.consumeParameter(19);
			this.lexicalBuffer.endNode(resetMethodParameterChangesExclusions, true);
			nodeOpen = false;
			resetMethodParameterChangesExclusions.setLineNumber(keywordToken.beginLine);
		} catch (Throwable throwable) {
			if (nodeOpen) {
				this.lexicalBuffer.endNode(resetMethodParameterChangesExclusions, true);
			}

			throw throwable;
		}
	}

	public final void parseType() {
		Type type = new Type(218);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(type);

		try {
			switch (this.nextToken.kind) {
				case 37:
					ZKMScriptToken allToken = this.consumeParameter(37);
					this.lexicalBuffer.endNode(type, true);
					nodeOpen = false;
					type.setText(allToken.image);
					return;
				case 39:
					ZKMScriptToken openToken = this.consumeParameter(39);
					this.lexicalBuffer.endNode(type, true);
					nodeOpen = false;
					type.setText(openToken.image);
					return;
				case 40:
					ZKMScriptToken enumToken = this.consumeParameter(40);
					this.lexicalBuffer.endNode(type, true);
					nodeOpen = false;
					type.setText(enumToken.image);
					return;
				case 42:
					ZKMScriptToken keepToken = this.consumeParameter(42);
					this.lexicalBuffer.endNode(type, true);
					nodeOpen = false;
					type.setText(keepToken.image);
					return;
				case 43:
					ZKMScriptToken noneToken = this.consumeParameter(43);
					this.lexicalBuffer.endNode(type, true);
					nodeOpen = false;
					type.setText(noneToken.image);
					return;
				case 45:
					ZKMScriptToken trimToken = this.consumeParameter(45);
					this.lexicalBuffer.endNode(type, true);
					nodeOpen = false;
					type.setText(trimToken.image);
					return;
				case 46:
					ZKMScriptToken printToken = this.consumeParameter(46);
					this.lexicalBuffer.endNode(type, true);
					nodeOpen = false;
					type.setText(printToken.image);
					return;
				case 47:
					ZKMScriptToken lightToken = this.consumeParameter(47);
					this.lexicalBuffer.endNode(type, true);
					nodeOpen = false;
					type.setText(lightToken.image);
					return;
				case 48:
					ZKMScriptToken heavyToken = this.consumeParameter(48);
					this.lexicalBuffer.endNode(type, true);
					nodeOpen = false;
					type.setText(heavyToken.image);
					return;
				case 51:
					ZKMScriptToken asciiToken = this.consumeParameter(51);
					this.lexicalBuffer.endNode(type, true);
					nodeOpen = false;
					type.setText(asciiToken.image);
					return;
				case 52:
					ZKMScriptToken bridgeToken = this.consumeParameter(52);
					this.lexicalBuffer.endNode(type, true);
					nodeOpen = false;
					type.setText(bridgeToken.image);
					return;
				case 56:
					ZKMScriptToken searchToken = this.consumeParameter(56);
					this.lexicalBuffer.endNode(type, true);
					nodeOpen = false;
					type.setText(searchToken.image);
					return;
				case 60:
					ZKMScriptToken deleteToken = this.consumeParameter(60);
					this.lexicalBuffer.endNode(type, true);
					nodeOpen = false;
					type.setText(deleteToken.image);
					return;
				case 61:
					ZKMScriptToken normalToken = this.consumeParameter(61);
					this.lexicalBuffer.endNode(type, true);
					nodeOpen = false;
					type.setText(normalToken.image);
					return;
				case 63:
					ZKMScriptToken randomToken = this.consumeParameter(63);
					this.lexicalBuffer.endNode(type, true);
					nodeOpen = false;
					type.setText(randomToken.image);
					return;
				case 64:
					ZKMScriptToken excludeToken = this.consumeParameter(64);
					this.lexicalBuffer.endNode(type, true);
					nodeOpen = false;
					type.setText(excludeToken.image);
					return;
				case 69:
					ZKMScriptToken executeToken = this.consumeParameter(69);
					this.lexicalBuffer.endNode(type, true);
					nodeOpen = false;
					type.setText(executeToken.image);
					return;
				case 71:
					ZKMScriptToken scrambleToken = this.consumeParameter(71);
					this.lexicalBuffer.endNode(type, true);
					nodeOpen = false;
					type.setText(scrambleToken.image);
					return;
				case 74:
					ZKMScriptToken enhancedToken = this.consumeParameter(74);
					this.lexicalBuffer.endNode(type, true);
					nodeOpen = false;
					type.setText(enhancedToken.image);
					return;
				case 77:
					ZKMScriptToken syntheticToken = this.consumeParameter(77);
					this.lexicalBuffer.endNode(type, true);
					nodeOpen = false;
					type.setText(syntheticToken.image);
					return;
				case 80:
					ZKMScriptToken classpathToken = this.consumeParameter(80);
					this.lexicalBuffer.endNode(type, true);
					nodeOpen = false;
					type.setText(classpathToken.image);
					return;
				case 81:
					ZKMScriptToken obfuscateToken = this.consumeParameter(81);
					this.lexicalBuffer.endNode(type, true);
					nodeOpen = false;
					type.setText(obfuscateToken.image);
					return;
				case 85:
					ZKMScriptToken randomizeToken = this.consumeParameter(85);
					this.lexicalBuffer.endNode(type, true);
					nodeOpen = false;
					type.setText(randomizeToken.image);
					return;
				case 86:
					ZKMScriptToken groupingsToken = this.consumeParameter(86);
					this.lexicalBuffer.endNode(type, true);
					nodeOpen = false;
					type.setText(groupingsToken.image);
					return;
				case 87:
					ZKMScriptToken annotationToken = this.consumeParameter(87);
					this.lexicalBuffer.endNode(type, true);
					nodeOpen = false;
					type.setText(annotationToken.image);
					return;
				case 90:
					ZKMScriptToken aggressiveToken = this.consumeParameter(90);
					this.lexicalBuffer.endNode(type, true);
					nodeOpen = false;
					type.setText(aggressiveToken.image);
					return;
				case 213:
					ZKMScriptToken nameToken = this.consumeParameter(213);
					this.lexicalBuffer.endNode(type, true);
					nodeOpen = false;
					type.setText(nameToken.image);
					return;
				default:
					this.la1Generation[183] = this.generationCounter;
					this.consumeParameter(-1);
					throw new ParsingException();
			}
		} finally {
			if (nodeOpen) {
				this.lexicalBuffer.endNode(type, true);
			}
		}
	}

	public final void parseAggressiveOverloadParameter() {
		AggressiveOverloadParameter aggressiveOverloadParameter = new AggressiveOverloadParameter(108);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(aggressiveOverloadParameter);

		try {
			try {
				ZKMScriptToken keywordToken = this.consumeParameter(168);
				this.consumeParameter(15);
				this.parseBoolean();
				this.lexicalBuffer.endNode(aggressiveOverloadParameter, true);
				nodeOpen = false;
				aggressiveOverloadParameter.setParameterName(keywordToken.image);
			} catch (Throwable throwable) {
				if (nodeOpen) {
					this.lexicalBuffer.discardNode(aggressiveOverloadParameter);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			}
		} catch (Throwable throwable2) {
			if (nodeOpen) {
				this.lexicalBuffer.endNode(aggressiveOverloadParameter, true);
			}

			throw throwable2;
		}
	}

	public final void parseMethodParameterChangePackageParameter() {
		MethodParameterChangePackageParameter methodParameterChangePackageParameter = new MethodParameterChangePackageParameter(134);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(methodParameterChangePackageParameter);

		try {
			try {
				ZKMScriptToken keywordToken = this.consumeParameter(194);
				this.consumeParameter(15);
				this.parseStringLiteral();
				this.lexicalBuffer.endNode(methodParameterChangePackageParameter, true);
				nodeOpen = false;
				methodParameterChangePackageParameter.setParameterName(keywordToken.image);
			} catch (Throwable throwable) {
				if (nodeOpen) {
					this.lexicalBuffer.discardNode(methodParameterChangePackageParameter);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			}
		} catch (Throwable throwable2) {
			if (nodeOpen) {
				this.lexicalBuffer.endNode(methodParameterChangePackageParameter, true);
			}

			throw throwable2;
		}
	}

	public final void parseAutoReflectionType() {
		AutoReflectionType autoReflectionType = new AutoReflectionType(158);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(autoReflectionType);

		try {
			switch (this.nextToken.kind) {
				case 43:
					ZKMScriptToken noneToken = this.consumeParameter(43);
					this.lexicalBuffer.endNode(autoReflectionType, true);
					nodeOpen = false;
					autoReflectionType.setText(noneToken.image);
					return;
				case 61:
					ZKMScriptToken normalToken = this.consumeParameter(61);
					this.lexicalBuffer.endNode(autoReflectionType, true);
					nodeOpen = false;
					autoReflectionType.setText(normalToken.image);
					return;
				default:
					this.la1Generation[122] = this.generationCounter;
					this.consumeParameter(-1);
					throw new ParsingException();
			}
		} finally {
			if (nodeOpen) {
				this.lexicalBuffer.endNode(autoReflectionType, true);
			}
		}
	}

	public final void parseAssumeRuntimeVersionParameter() {
		AssumeRuntimeVersionParameter assumeRuntimeVersionParameter = new AssumeRuntimeVersionParameter(135);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(assumeRuntimeVersionParameter);

		try {
			try {
				ZKMScriptToken keywordToken = this.consumeParameter(147);
				this.consumeParameter(15);
				this.parseStringLiteral();
				this.lexicalBuffer.endNode(assumeRuntimeVersionParameter, true);
				nodeOpen = false;
				assumeRuntimeVersionParameter.setParameterName(keywordToken.image);
			} catch (Throwable throwable) {
				if (nodeOpen) {
					this.lexicalBuffer.discardNode(assumeRuntimeVersionParameter);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			}
		} catch (Throwable throwable2) {
			if (nodeOpen) {
				this.lexicalBuffer.endNode(assumeRuntimeVersionParameter, true);
			}

			throw throwable2;
		}
	}

	public final void parseExtendsClause() {
		ExtendsClause extendsClause = new ExtendsClause(153);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(extendsClause);

		try {
			this.consumeParameter(66);
			switch (this.nextToken.kind) {
				case 25:
				case 27:
				case 28:
					this.parseComplexAnnotationSpecifier();
					break;
				case 26:
				default:
					this.la1Generation[116] = this.generationCounter;
			}

			this.parseQualifiedClassName();
			this.lexicalBuffer.endNode(extendsClause, true);
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (true) {
					this.lexicalBuffer.discardNode(extendsClause);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}
				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable throwable2) {
				if (nodeOpen) {
					this.lexicalBuffer.endNode(extendsClause, true);
				}

				throw throwable2;
			}
		}
	}

	public final void parseReferencingAnnotationComponentName() {
		ReferencingAnnotationComponentName referencingAnnotationComponentName = new ReferencingAnnotationComponentName(180);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(referencingAnnotationComponentName);

		try {
			ZKMScriptToken nameToken = this.consumeParameter(213);
			this.lexicalBuffer.endNode(referencingAnnotationComponentName, true);
			nodeOpen = false;
			referencingAnnotationComponentName.setText(nameToken.image);
		} catch (Throwable throwable) {
			if (nodeOpen) {
				this.lexicalBuffer.endNode(referencingAnnotationComponentName, true);
			}

			throw throwable;
		}
	}

	public final void parseMemberModifierHelper() {
		MemberModifierHelper memberModifierHelper = new MemberModifierHelper(146);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(memberModifierHelper);

		try {
			switch (this.nextToken.kind) {
				case 40:
					ZKMScriptToken enumToken = this.consumeParameter(40);
					this.lexicalBuffer.endNode(memberModifierHelper, true);
					nodeOpen = false;
					memberModifierHelper.setText(enumToken.image);
					return;
				case 41:
				case 42:
				case 43:
				case 44:
				case 45:
				case 46:
				case 47:
				case 48:
				case 49:
				case 51:
				case 53:
				case 54:
				case 55:
				case 56:
				case 60:
				case 61:
				case 62:
				case 63:
				case 64:
				case 65:
				case 66:
				case 69:
				case 70:
				case 71:
				case 73:
				case 74:
				case 76:
				case 78:
				case 79:
				case 80:
				case 81:
				case 83:
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
				default:
					this.la1Generation[107] = this.generationCounter;
					this.consumeParameter(-1);
					throw new ParsingException();
				case 50:
					ZKMScriptToken finalToken = this.consumeParameter(50);
					this.lexicalBuffer.endNode(memberModifierHelper, true);
					nodeOpen = false;
					memberModifierHelper.setText(finalToken.image);
					return;
				case 52:
					ZKMScriptToken bridgeToken = this.consumeParameter(52);
					this.lexicalBuffer.endNode(memberModifierHelper, true);
					nodeOpen = false;
					memberModifierHelper.setText(bridgeToken.image);
					return;
				case 57:
					ZKMScriptToken publicToken = this.consumeParameter(57);
					this.lexicalBuffer.endNode(memberModifierHelper, true);
					nodeOpen = false;
					memberModifierHelper.setText(publicToken.image);
					return;
				case 58:
					ZKMScriptToken staticToken = this.consumeParameter(58);
					this.lexicalBuffer.endNode(memberModifierHelper, true);
					nodeOpen = false;
					memberModifierHelper.setText(staticToken.image);
					return;
				case 59:
					ZKMScriptToken nativeToken = this.consumeParameter(59);
					this.lexicalBuffer.endNode(memberModifierHelper, true);
					nodeOpen = false;
					memberModifierHelper.setText(nativeToken.image);
					return;
				case 67:
					ZKMScriptToken packageToken = this.consumeParameter(67);
					this.lexicalBuffer.endNode(memberModifierHelper, true);
					nodeOpen = false;
					memberModifierHelper.setText(packageToken.image);
					return;
				case 68:
					ZKMScriptToken privateToken = this.consumeParameter(68);
					this.lexicalBuffer.endNode(memberModifierHelper, true);
					nodeOpen = false;
					memberModifierHelper.setText(privateToken.image);
					return;
				case 72:
					ZKMScriptToken abstractToken = this.consumeParameter(72);
					this.lexicalBuffer.endNode(memberModifierHelper, true);
					nodeOpen = false;
					memberModifierHelper.setText(abstractToken.image);
					return;
				case 75:
					ZKMScriptToken volatileToken = this.consumeParameter(75);
					this.lexicalBuffer.endNode(memberModifierHelper, true);
					nodeOpen = false;
					memberModifierHelper.setText(volatileToken.image);
					return;
				case 77:
					ZKMScriptToken syntheticToken = this.consumeParameter(77);
					this.lexicalBuffer.endNode(memberModifierHelper, true);
					nodeOpen = false;
					memberModifierHelper.setText(syntheticToken.image);
					return;
				case 82:
					ZKMScriptToken protectedToken = this.consumeParameter(82);
					this.lexicalBuffer.endNode(memberModifierHelper, true);
					nodeOpen = false;
					memberModifierHelper.setText(protectedToken.image);
					return;
				case 84:
					ZKMScriptToken transientToken = this.consumeParameter(84);
					this.lexicalBuffer.endNode(memberModifierHelper, true);
					nodeOpen = false;
					memberModifierHelper.setText(transientToken.image);
					return;
				case 99:
					ZKMScriptToken synchronizedToken = this.consumeParameter(99);
					this.lexicalBuffer.endNode(memberModifierHelper, true);
					nodeOpen = false;
					memberModifierHelper.setText(synchronizedToken.image);
			}
		} finally {
			if (nodeOpen) {
				this.lexicalBuffer.endNode(memberModifierHelper, true);
			}
		}
	}

	public final void parseJarQualifier() {
		JarQualifier jarQualifier = new JarQualifier(188);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(jarQualifier);

		try {
			this.parseStringLiteral();
			this.consumeParameter(25);
			this.lexicalBuffer.endNode(jarQualifier, true);
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (true) {
					this.lexicalBuffer.discardNode(jarQualifier);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}
				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable throwable2) {
				if (nodeOpen) {
					this.lexicalBuffer.endNode(jarQualifier, true);
				}

				throw throwable2;
			}
		}
	}

	public final void parseExceptionObfuscationType() {
		ExceptionObfuscationType exceptionObfuscationType = new ExceptionObfuscationType(157);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(exceptionObfuscationType);

		try {
			switch (this.nextToken.kind) {
				case 43:
					ZKMScriptToken noneToken = this.consumeParameter(43);
					this.lexicalBuffer.endNode(exceptionObfuscationType, true);
					nodeOpen = false;
					exceptionObfuscationType.setText(noneToken.image);
					return;
				case 47:
					ZKMScriptToken lightToken = this.consumeParameter(47);
					this.lexicalBuffer.endNode(exceptionObfuscationType, true);
					nodeOpen = false;
					exceptionObfuscationType.setText(lightToken.image);
					return;
				case 48:
					ZKMScriptToken heavyToken = this.consumeParameter(48);
					this.lexicalBuffer.endNode(exceptionObfuscationType, true);
					nodeOpen = false;
					exceptionObfuscationType.setText(heavyToken.image);
					return;
				default:
					this.la1Generation[121] = this.generationCounter;
					this.consumeParameter(-1);
					throw new ParsingException();
			}
		} finally {
			if (nodeOpen) {
				this.lexicalBuffer.endNode(exceptionObfuscationType, true);
			}
		}
	}

	public final void parseNewClassNameFileParameter() {
		NewClassNameFileParameter newClassNameFileParameter = new NewClassNameFileParameter(98);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(newClassNameFileParameter);

		try {
			try {
				ZKMScriptToken keywordToken = this.consumeParameter(118);
				this.consumeParameter(15);
				this.parseStringLiteral();
				this.lexicalBuffer.endNode(newClassNameFileParameter, true);
				nodeOpen = false;
				newClassNameFileParameter.setParameterName(keywordToken.image);
			} catch (Throwable throwable) {
				if (nodeOpen) {
					this.lexicalBuffer.discardNode(newClassNameFileParameter);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			}
		} catch (Throwable throwable2) {
			if (nodeOpen) {
				this.lexicalBuffer.endNode(newClassNameFileParameter, true);
			}

			throw throwable2;
		}
	}

	public final void parseObfuscateReferencesParameter() {
		ObfuscateReferencesParameter obfuscateReferencesParameter = new ObfuscateReferencesParameter(138);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(obfuscateReferencesParameter);

		try {
			try {
				ZKMScriptToken keywordToken = this.consumeParameter(139);
				this.consumeParameter(15);
				this.parseObfuscateReferencesType();
				this.lexicalBuffer.endNode(obfuscateReferencesParameter, true);
				nodeOpen = false;
				obfuscateReferencesParameter.setParameterName(keywordToken.image);
			} catch (Throwable throwable) {
				if (nodeOpen) {
					this.lexicalBuffer.discardNode(obfuscateReferencesParameter);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			}
		} catch (Throwable throwable2) {
			if (nodeOpen) {
				this.lexicalBuffer.endNode(obfuscateReferencesParameter, true);
			}

			throw throwable2;
		}
	}

	public final void parseAndMethodSpecifier() {
		AndMethodSpecifier andMethodSpecifier = new AndMethodSpecifier(211);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(andMethodSpecifier);

		try {
			this.parseComplexMethodSpecifier();

			while (true) {
				switch (this.nextToken.kind) {
					case 35:
						this.consumeParameter(35);
						this.parseComplexMethodSpecifier();
						break;
					default:
						this.la1Generation[176] = this.generationCounter;
						this.lexicalBuffer.endNode(andMethodSpecifier, true);
						return;
				}
			}
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (true) {
					this.lexicalBuffer.discardNode(andMethodSpecifier);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}
				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable throwable2) {
				if (nodeOpen) {
					this.lexicalBuffer.endNode(andMethodSpecifier, true);
				}

				throw throwable2;
			}
		}
	}

	public final void parseBracketedClassSpecifier() {
		BracketedClassSpecifier bracketedClassSpecifier = new BracketedClassSpecifier(196);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(bracketedClassSpecifier);

		try {
			switch (this.nextToken.kind) {
				case 25:
					this.consumeParameter(25);
					bracketedClassSpecifier.setNegated();
					break;
				default:
					this.la1Generation[156] = this.generationCounter;
			}

			this.consumeParameter(28);
			this.parseAndClassSpecifier();

			while (true) {
				switch (this.nextToken.kind) {
					case 34:
						this.consumeParameter(34);
						this.parseAndClassSpecifier();
						break;
					default:
						this.la1Generation[157] = this.generationCounter;
						this.consumeParameter(29);
						this.lexicalBuffer.endNode(bracketedClassSpecifier, true);
						return;
				}
			}
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (true) {
					this.lexicalBuffer.discardNode(bracketedClassSpecifier);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}
				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable throwable2) {
				if (nodeOpen) {
					this.lexicalBuffer.endNode(bracketedClassSpecifier, true);
				}

				throw throwable2;
			}
		}
	}

	public final void parseDeleteSourceFileAttributesParameter() {
		DeleteSourceFileAttributesParameter deleteSourceFileAttributesParameter = new DeleteSourceFileAttributesParameter(88);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(deleteSourceFileAttributesParameter);

		try {
			try {
				ZKMScriptToken keywordToken = this.consumeParameter(178);
				this.consumeParameter(15);
				this.parseBoolean();
				this.lexicalBuffer.endNode(deleteSourceFileAttributesParameter, true);
				nodeOpen = false;
				deleteSourceFileAttributesParameter.setParameterName(keywordToken.image);
			} catch (Throwable throwable) {
				if (nodeOpen) {
					this.lexicalBuffer.discardNode(deleteSourceFileAttributesParameter);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			}
		} catch (Throwable throwable2) {
			if (nodeOpen) {
				this.lexicalBuffer.endNode(deleteSourceFileAttributesParameter, true);
			}

			throw throwable2;
		}
	}

	public final void parseRandomizeParameter() {
		RandomizeParameter randomizeParameter = new RandomizeParameter(109);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(randomizeParameter);

		try {
			try {
				ZKMScriptToken keywordToken = this.consumeParameter(85);
				this.consumeParameter(15);
				this.parseBoolean();
				this.lexicalBuffer.endNode(randomizeParameter, true);
				nodeOpen = false;
				randomizeParameter.setParameterName(keywordToken.image);
			} catch (Throwable throwable) {
				if (nodeOpen) {
					this.lexicalBuffer.discardNode(randomizeParameter);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			}
		} catch (Throwable throwable2) {
			if (nodeOpen) {
				this.lexicalBuffer.endNode(randomizeParameter, true);
			}

			throw throwable2;
		}
	}

	public final void parseArchiveCompressionParameter() {
		ArchiveCompressionParameter archiveCompressionParameter = new ArchiveCompressionParameter(59);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(archiveCompressionParameter);

		try {
			try {
				ZKMScriptToken keywordToken = this.consumeParameter(132);
				this.consumeParameter(15);
				this.parseArchiveCompressionType();
				this.lexicalBuffer.endNode(archiveCompressionParameter, true);
				nodeOpen = false;
				archiveCompressionParameter.setParameterName(keywordToken.image);
			} catch (Throwable throwable) {
				if (nodeOpen) {
					this.lexicalBuffer.discardNode(archiveCompressionParameter);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			}
		} catch (Throwable throwable2) {
			if (nodeOpen) {
				this.lexicalBuffer.endNode(archiveCompressionParameter, true);
			}

			throw throwable2;
		}
	}

	public final void parseDeleteAnnotationsParameter() {
		DeleteAnnotationsParameter deleteAnnotationsParameter = new DeleteAnnotationsParameter(90);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(deleteAnnotationsParameter);

		try {
			try {
				ZKMScriptToken keywordToken = this.consumeParameter(177);
				this.consumeParameter(15);
				this.parseBoolean();
				this.lexicalBuffer.endNode(deleteAnnotationsParameter, true);
				nodeOpen = false;
				deleteAnnotationsParameter.setParameterName(keywordToken.image);
			} catch (Throwable throwable) {
				if (nodeOpen) {
					this.lexicalBuffer.discardNode(deleteAnnotationsParameter);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			}
		} catch (Throwable throwable2) {
			if (nodeOpen) {
				this.lexicalBuffer.endNode(deleteAnnotationsParameter, true);
			}

			throw throwable2;
		}
	}

	public final void parseResetObfuscateReferenceExclusions() {
		ResetObfuscateReferenceExclusionsStatement resetObfuscateReferenceExclusionsStatement = new ResetObfuscateReferenceExclusionsStatement(67);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(resetObfuscateReferenceExclusionsStatement);

		try {
			ZKMScriptToken keywordToken = this.consumeParameter(203);
			this.statementKind = 203;
			this.consumeParameter(19);
			this.lexicalBuffer.endNode(resetObfuscateReferenceExclusionsStatement, true);
			nodeOpen = false;
			resetObfuscateReferenceExclusionsStatement.setLineNumber(keywordToken.beginLine);
		} catch (Throwable throwable) {
			if (nodeOpen) {
				this.lexicalBuffer.endNode(resetObfuscateReferenceExclusionsStatement, true);
			}

			throw throwable;
		}
	}

	public final void parseMethodParameter() {
		MethodParameter methodParameter = new MethodParameter(214);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(methodParameter);

		try {
			switch (this.nextToken.kind) {
				case 25:
				case 27:
				case 28:
					this.parseParameterComplexAnnotationSpecifier();
					break;
				case 26:
				default:
					this.la1Generation[179] = this.generationCounter;
			}

			switch (this.nextToken.kind) {
				case 23:
					this.parseWildcardType();
					break;
				case 32:
					this.parseParameterPlaceHolder();
					break;
				case 37:
				case 39:
				case 40:
				case 42:
				case 43:
				case 45:
				case 46:
				case 47:
				case 48:
				case 51:
				case 52:
				case 56:
				case 60:
				case 61:
				case 63:
				case 64:
				case 69:
				case 71:
				case 74:
				case 77:
				case 80:
				case 81:
				case 85:
				case 86:
				case 87:
				case 90:
				case 213:
					this.parseQualifiedType();
					break;
				default:
					this.la1Generation[180] = this.generationCounter;
					this.consumeParameter(-1);
					throw new ParsingException();
			}

			this.lexicalBuffer.endNode(methodParameter, true);
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (true) {
					this.lexicalBuffer.discardNode(methodParameter);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}
				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable throwable2) {
				if (nodeOpen) {
					this.lexicalBuffer.endNode(methodParameter, true);
				}

				throw throwable2;
			}
		}
	}

	public final void parseExceptionObfuscationParameter() {
		ExceptionObfuscationParameter exceptionObfuscationParameter = new ExceptionObfuscationParameter(131);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(exceptionObfuscationParameter);

		try {
			try {
				ZKMScriptToken keywordToken = this.consumeParameter(145);
				this.consumeParameter(15);
				this.parseExceptionObfuscationType();
				this.lexicalBuffer.endNode(exceptionObfuscationParameter, true);
				nodeOpen = false;
				exceptionObfuscationParameter.setParameterName(keywordToken.image);
			} catch (Throwable throwable) {
				if (nodeOpen) {
					this.lexicalBuffer.discardNode(exceptionObfuscationParameter);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			}
		} catch (Throwable throwable2) {
			if (nodeOpen) {
				this.lexicalBuffer.endNode(exceptionObfuscationParameter, true);
			}

			throw throwable2;
		}
	}

	public final void parseMemberSpecifierModifier() {
		MemberSpecifierModifier memberSpecifierModifier = new MemberSpecifierModifier(144);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(memberSpecifierModifier);

		try {
			this.parseMemberModifierHelper();
			this.lexicalBuffer.endNode(memberSpecifierModifier, true);
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (true) {
					this.lexicalBuffer.discardNode(memberSpecifierModifier);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}
				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable throwable2) {
				if (nodeOpen) {
					this.lexicalBuffer.endNode(memberSpecifierModifier, true);
				}

				throw throwable2;
			}
		}
	}

	public final void parseResetIgnoreMissingReferences() {
		ResetIgnoreMissingReferencesStatement resetIgnoreMissingReferencesStatement = new ResetIgnoreMissingReferencesStatement(65);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(resetIgnoreMissingReferencesStatement);

		try {
			ZKMScriptToken keywordToken = this.consumeParameter(187);
			this.statementKind = 187;
			this.consumeParameter(19);
			this.lexicalBuffer.endNode(resetIgnoreMissingReferencesStatement, true);
			nodeOpen = false;
			resetIgnoreMissingReferencesStatement.setLineNumber(keywordToken.beginLine);
		} catch (Throwable throwable) {
			if (nodeOpen) {
				this.lexicalBuffer.endNode(resetIgnoreMissingReferencesStatement, true);
			}

			throw throwable;
		}
	}

	public final void parseNewMethodNameFileParameter() {
		NewMethodNameFileParameter newMethodNameFile_2 = new NewMethodNameFileParameter(100);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(newMethodNameFile_2);

		try {
			try {
				ZKMScriptToken keywordToken = this.consumeParameter(123);
				this.consumeParameter(15);
				this.parseStringLiteral();
				this.lexicalBuffer.endNode(newMethodNameFile_2, true);
				nodeOpen = false;
				newMethodNameFile_2.setParameterName(keywordToken.image);
			} catch (Throwable throwable) {
				if (nodeOpen) {
					this.lexicalBuffer.discardNode(newMethodNameFile_2);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			}
		} catch (Throwable throwable2) {
			if (nodeOpen) {
				this.lexicalBuffer.endNode(newMethodNameFile_2, true);
			}

			throw throwable2;
		}
	}

	public final void parseExpectedFinalHash() {
		ExpectedFinalHash expectedFinalSHA2562 = new ExpectedFinalHash(9);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(expectedFinalSHA2562);

		try {
			try {
				ZKMScriptToken keywordToken = this.consumeParameter(141);
				this.consumeParameter(15);
				this.parseStringLiteral();
				this.lexicalBuffer.endNode(expectedFinalSHA2562, true);
				nodeOpen = false;
				expectedFinalSHA2562.setParameterName(keywordToken.image);
			} catch (Throwable throwable) {
				if (nodeOpen) {
					this.lexicalBuffer.discardNode(expectedFinalSHA2562);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			}
		} catch (Throwable throwable2) {
			if (nodeOpen) {
				this.lexicalBuffer.endNode(expectedFinalSHA2562, true);
			}

			throw throwable2;
		}
	}

	public final void parseLinkMethodName() {
		LinkMethodName linkMethodName = new LinkMethodName(208);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(linkMethodName);

		try {
			switch (this.nextToken.kind) {
				case 53:
					ZKMScriptToken linkToken = this.consumeParameter(53);
					linkMethodName.setText(linkToken.image);
					this.lexicalBuffer.endNode(linkMethodName, true);
					nodeOpen = false;
					linkToken.setDescriptionIndex(8);
					return;
				case 217:
					ZKMScriptToken linkedMethodNameToken = this.consumeParameter(217);
					linkMethodName.setText(linkedMethodNameToken.image);
					this.lexicalBuffer.endNode(linkMethodName, true);
					nodeOpen = false;
					linkedMethodNameToken.setDescriptionIndex(8);
					return;
				default:
					this.la1Generation[172] = this.generationCounter;
					this.consumeParameter(-1);
					throw new ParsingException();
			}
		} finally {
			if (nodeOpen) {
				this.lexicalBuffer.endNode(linkMethodName, true);
			}
		}
	}

	public final void parseAndAnnotationSpecifier() {
		AndAnnotationSpecifier andAnnotationSpecifier = new AndAnnotationSpecifier(177);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(andAnnotationSpecifier);

		try {
			this.parseComplexAnnotationSpecifier();

			while (true) {
				switch (this.nextToken.kind) {
					case 35:
						this.consumeParameter(35);
						this.parseComplexAnnotationSpecifier();
						break;
					default:
						this.la1Generation[137] = this.generationCounter;
						this.lexicalBuffer.endNode(andAnnotationSpecifier, true);
						return;
				}
			}
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (true) {
					this.lexicalBuffer.discardNode(andAnnotationSpecifier);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}
				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable throwable2) {
				if (nodeOpen) {
					this.lexicalBuffer.endNode(andAnnotationSpecifier, true);
				}

				throw throwable2;
			}
		}
	}

	public final void parseComplexClassSpecifier() {
		ComplexClassSpecifier complexClassSpecifier = new ComplexClassSpecifier(195);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(complexClassSpecifier);

		try {
			switch (this.nextToken.kind) {
				case 23:
				case 37:
				case 39:
				case 40:
				case 42:
				case 43:
				case 45:
				case 46:
				case 47:
				case 48:
				case 51:
				case 52:
				case 56:
				case 60:
				case 61:
				case 63:
				case 64:
				case 69:
				case 71:
				case 74:
				case 77:
				case 80:
				case 81:
				case 85:
				case 86:
				case 87:
				case 90:
				case 97:
				case 213:
				case 214:
					this.parseClassName();
					break;
				case 25:
				case 28:
					this.parseBracketedClassSpecifier();
					break;
				default:
					this.la1Generation[153] = this.generationCounter;
					this.consumeParameter(-1);
					throw new ParsingException();
			}

			label35:
			switch (this.nextToken.kind) {
				case 24:
				case 26:
					switch (this.nextToken.kind) {
						case 24:
							this.consumeParameter(24);
							complexClassSpecifier.setIncludesMembers();
							break label35;
						case 26:
							this.consumeParameter(26);
							complexClassSpecifier.setRecursive();
							break label35;
						default:
							this.la1Generation[154] = this.generationCounter;
							this.consumeParameter(-1);
							throw new ParsingException();
					}
				default:
					this.la1Generation[155] = this.generationCounter;
			}

			this.lexicalBuffer.endNode(complexClassSpecifier, true);
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (true) {
					this.lexicalBuffer.discardNode(complexClassSpecifier);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}
				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable throwable2) {
				if (nodeOpen) {
					this.lexicalBuffer.endNode(complexClassSpecifier, true);
				}

				throw throwable2;
			}
		}
	}

	public final void parseObfuscateFlowParameter() {
		ObfuscateFlowParameter obfuscateFlowParameter = new ObfuscateFlowParameter(105);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(obfuscateFlowParameter);

		try {
			try {
				ZKMScriptToken keywordToken = this.consumeParameter(101);
				this.consumeParameter(15);
				this.parseFlowObfuscationType();
				this.lexicalBuffer.endNode(obfuscateFlowParameter, true);
				nodeOpen = false;
				obfuscateFlowParameter.setParameterName(keywordToken.image);
			} catch (Throwable throwable) {
				if (nodeOpen) {
					this.lexicalBuffer.discardNode(obfuscateFlowParameter);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			}
		} catch (Throwable throwable2) {
			if (nodeOpen) {
				this.lexicalBuffer.endNode(obfuscateFlowParameter, true);
			}

			throw throwable2;
		}
	}

	public final void parseGarbageCollect() {
		GarbageCollectStatement garbageCollect = new GarbageCollectStatement(80);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(garbageCollect);

		try {
			try {
				ZKMScriptToken keywordToken = this.consumeParameter(36);
				this.statementKind = 36;
				switch (this.nextToken.kind) {
					case 212:
						this.parseIntegerLiteral();
						break;
					default:
						this.la1Generation[77] = this.generationCounter;
				}

				this.consumeParameter(19);
				this.lexicalBuffer.endNode(garbageCollect, true);
				nodeOpen = false;
				garbageCollect.setLineNumber(keywordToken.beginLine);
			} catch (Throwable throwable) {
				if (nodeOpen) {
					this.lexicalBuffer.discardNode(garbageCollect);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			}
		} catch (Throwable throwable2) {
			if (nodeOpen) {
				this.lexicalBuffer.endNode(garbageCollect, true);
			}

			throw throwable2;
		}
	}

	public final void parseMemberComplexAnnotationSpecifier() {
		MemberComplexAnnotationSpecifier memberComplexAnnotationSpecifier = new MemberComplexAnnotationSpecifier(172);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(memberComplexAnnotationSpecifier);

		try {
			this.parseComplexAnnotationSpecifier();
			this.lexicalBuffer.endNode(memberComplexAnnotationSpecifier, true);
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (true) {
					this.lexicalBuffer.discardNode(memberComplexAnnotationSpecifier);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}
				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable throwable2) {
				if (nodeOpen) {
					this.lexicalBuffer.endNode(memberComplexAnnotationSpecifier, true);
				}

				throw throwable2;
			}
		}
	}

	public final void parseBooleanOrIfInArchive() {
		BooleanOrIfInArchive booleanOrIfInArchive = new BooleanOrIfInArchive(224);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(booleanOrIfInArchive);

		try {
			switch (this.nextToken.kind) {
				case 41:
					ZKMScriptToken trueToken = this.consumeParameter(41);
					this.lexicalBuffer.endNode(booleanOrIfInArchive, true);
					nodeOpen = false;
					booleanOrIfInArchive.setText(trueToken.image);
					return;
				case 49:
					ZKMScriptToken falseToken = this.consumeParameter(49);
					this.lexicalBuffer.endNode(booleanOrIfInArchive, true);
					nodeOpen = false;
					booleanOrIfInArchive.setText(falseToken.image);
					return;
				case 96:
					ZKMScriptToken ifInArchiveToken = this.consumeParameter(96);
					this.lexicalBuffer.endNode(booleanOrIfInArchive, true);
					nodeOpen = false;
					booleanOrIfInArchive.setText(ifInArchiveToken.image);
					return;
				default:
					this.la1Generation[186] = this.generationCounter;
					this.consumeParameter(-1);
					throw new ParsingException();
			}
		} finally {
			if (nodeOpen) {
				this.lexicalBuffer.endNode(booleanOrIfInArchive, true);
			}
		}
	}

	public final void parseLineNumbersParameter() {
		LineNumbersParameter lineNumbers2 = new LineNumbersParameter(113);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(lineNumbers2);

		try {
			try {
				ZKMScriptToken keywordToken = this.consumeParameter(95);
				this.consumeParameter(15);
				this.parseLineNumberParameterType();
				this.lexicalBuffer.endNode(lineNumbers2, true);
				nodeOpen = false;
				lineNumbers2.setParameterName(keywordToken.image);
			} catch (Throwable throwable) {
				if (nodeOpen) {
					this.lexicalBuffer.discardNode(lineNumbers2);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			}
		} catch (Throwable throwable2) {
			if (nodeOpen) {
				this.lexicalBuffer.endNode(lineNumbers2, true);
			}

			throw throwable2;
		}
	}

	public final void parseDeleteXMLCommentsParameter() {
		DeleteXMLCommentsParameter deleteXMLComments2 = new DeleteXMLCommentsParameter(62);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(deleteXMLComments2);

		try {
			try {
				ZKMScriptToken keywordToken = this.consumeParameter(129);
				this.consumeParameter(15);
				this.parseBoolean();
				this.lexicalBuffer.endNode(deleteXMLComments2, true);
				nodeOpen = false;
				deleteXMLComments2.setParameterName(keywordToken.image);
			} catch (Throwable throwable) {
				if (nodeOpen) {
					this.lexicalBuffer.discardNode(deleteXMLComments2);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			}
		} catch (Throwable throwable2) {
			if (nodeOpen) {
				this.lexicalBuffer.endNode(deleteXMLComments2, true);
			}

			throw throwable2;
		}
	}

	public final void parseEncryptIntegerConstantsParameter() {
		EncryptIntegerConstantsParameter encryptIntegerConstantsParameter = new EncryptIntegerConstantsParameter(103);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(encryptIntegerConstantsParameter);

		try {
			try {
				ZKMScriptToken keywordToken = this.consumeParameter(160);
				this.consumeParameter(15);
				this.parseIntegerEncryptionType();
				this.lexicalBuffer.endNode(encryptIntegerConstantsParameter, true);
				nodeOpen = false;
				encryptIntegerConstantsParameter.setParameterName(keywordToken.image);
			} catch (Throwable throwable) {
				if (nodeOpen) {
					this.lexicalBuffer.discardNode(encryptIntegerConstantsParameter);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			}
		} catch (Throwable throwable2) {
			if (nodeOpen) {
				this.lexicalBuffer.endNode(encryptIntegerConstantsParameter, true);
			}

			throw throwable2;
		}
	}

	public final void parseNewPackageNameFileParameter() {
		NewPackageNameFileParameter newPackageNameFile2 = new NewPackageNameFileParameter(97);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(newPackageNameFile2);

		try {
			try {
				ZKMScriptToken keywordToken = this.consumeParameter(131);
				this.consumeParameter(15);
				this.parseStringLiteral();
				this.lexicalBuffer.endNode(newPackageNameFile2, true);
				nodeOpen = false;
				newPackageNameFile2.setParameterName(keywordToken.image);
			} catch (Throwable throwable) {
				if (nodeOpen) {
					this.lexicalBuffer.discardNode(newPackageNameFile2);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			}
		} catch (Throwable throwable2) {
			if (nodeOpen) {
				this.lexicalBuffer.endNode(newPackageNameFile2, true);
			}

			throw throwable2;
		}
	}

	public final void parseRenameFilterParameter() {
		RenameFilterParameter renameFilterParameter = new RenameFilterParameter(86);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(renameFilterParameter);
		boolean nodeClosePending = false;

		try {
			nodeClosePending = true;
			if (this.matchesComplexModuleSpecifier(Integer.MAX_VALUE)) {
				this.parseComplexModuleSpecifier();
				nodeClosePending = false;
			} else if (this.matchesComplexPackageSpecifier(Integer.MAX_VALUE)) {
				this.parseComplexPackageSpecifier();
				nodeClosePending = false;
			} else if (this.matchesStandAloneAnnotation(Integer.MAX_VALUE)) {
				this.parseStandAloneAnnotation();
				nodeClosePending = false;
			} else if (this.matchesReferencingAnnotation(Integer.MAX_VALUE)) {
				this.parseReferencingAnnotation();
				this.consumeParameter(28);
				this.parseReferencingAnnotationComponentName();
				this.consumeParameter(15);
				if (this.matchesComplexPackageSpecifierLookahead(Integer.MAX_VALUE)) {
					this.parseComplexPackageSpecifier();
				}

				this.parseComplexClassSpecifier();
				this.consumeParameter(29);
				switch (this.nextToken.kind) {
					case 66:
						this.parseExtendsClause();
						break;
					default:
						this.la1Generation[79] = this.generationCounter;
				}

				switch (this.nextToken.kind) {
					case 91:
						this.parseImplementsClause();
						break;
					default:
						this.la1Generation[80] = this.generationCounter;
				}

				label253:
				switch (this.nextToken.kind) {
					case 23:
					case 25:
					case 27:
					case 28:
					case 37:
					case 39:
					case 40:
					case 42:
					case 43:
					case 45:
					case 46:
					case 47:
					case 48:
					case 50:
					case 51:
					case 52:
					case 54:
					case 56:
					case 57:
					case 58:
					case 59:
					case 60:
					case 61:
					case 63:
					case 64:
					case 67:
					case 68:
					case 69:
					case 71:
					case 72:
					case 73:
					case 74:
					case 75:
					case 77:
					case 80:
					case 81:
					case 82:
					case 84:
					case 85:
					case 86:
					case 87:
					case 90:
					case 99:
					case 213:
					case 214:
						if (this.matchesMemberComplexAnnotationSpecifierLookahead(Integer.MAX_VALUE)) {
							this.parseMemberComplexAnnotationSpecifier();
						}

						while (this.matchesNegatableMemberModifierLookahead(Integer.MAX_VALUE)) {
							switch (this.nextToken.kind) {
								case 25:
									this.parseNegatedMemberModifier();
									break;
								case 40:
								case 50:
								case 52:
								case 57:
								case 58:
								case 59:
								case 67:
								case 68:
								case 72:
								case 75:
								case 77:
								case 82:
								case 84:
								case 99:
									this.parseMemberModifier();
									break;
								default:
									this.la1Generation[81] = this.generationCounter;
									this.consumeParameter(-1);
									throw new ParsingException();
							}
						}

						if (this.matchesMethodSignatureLookahead(Integer.MAX_VALUE)) {
							this.parseMethodSignature();
							nodeClosePending = false;
							break;
						} else {
							switch (this.nextToken.kind) {
								case 23:
								case 25:
								case 28:
								case 37:
								case 39:
								case 40:
								case 42:
								case 43:
								case 45:
								case 46:
								case 47:
								case 48:
								case 51:
								case 52:
								case 56:
								case 60:
								case 61:
								case 63:
								case 64:
								case 69:
								case 71:
								case 74:
								case 77:
								case 80:
								case 81:
								case 85:
								case 86:
								case 87:
								case 90:
								case 213:
								case 214:
									this.parseFieldSignature();
									nodeClosePending = false;
									break label253;
								default:
									this.la1Generation[82] = this.generationCounter;
									this.consumeParameter(-1);
									throw new ParsingException();
							}
						}
					case 24:
					case 26:
					case 29:
					case 30:
					case 31:
					case 32:
					case 33:
					case 34:
					case 35:
					case 36:
					case 38:
					case 41:
					case 44:
					case 49:
					case 53:
					case 55:
					case 62:
					case 65:
					case 66:
					case 70:
					case 76:
					case 78:
					case 79:
					case 83:
					case 88:
					case 89:
					case 91:
					case 92:
					case 93:
					case 94:
					case 95:
					case 96:
					case 97:
					case 98:
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
					case 125:
					case 126:
					case 127:
					case 128:
					case 129:
					case 130:
					case 131:
					case 132:
					case 133:
					case 134:
					case 135:
					case 136:
					case 137:
					case 138:
					case 139:
					case 140:
					case 141:
					case 142:
					case 143:
					case 144:
					case 145:
					case 146:
					case 147:
					case 148:
					case 149:
					case 150:
					case 151:
					case 152:
					case 153:
					case 154:
					case 155:
					case 156:
					case 157:
					case 158:
					case 159:
					case 160:
					case 161:
					case 162:
					case 163:
					case 164:
					case 165:
					case 166:
					case 167:
					case 168:
					case 169:
					case 170:
					case 171:
					case 172:
					case 173:
					case 174:
					case 175:
					case 176:
					case 177:
					case 178:
					case 179:
					case 180:
					case 181:
					case 182:
					case 183:
					case 184:
					case 185:
					case 186:
					case 187:
					case 188:
					case 189:
					case 190:
					case 191:
					case 192:
					case 193:
					case 194:
					case 195:
					case 196:
					case 197:
					case 198:
					case 199:
					case 200:
					case 201:
					case 202:
					case 203:
					case 204:
					case 205:
					case 206:
					case 207:
					case 208:
					case 209:
					case 210:
					case 211:
					case 212:
					default:
						this.la1Generation[83] = this.generationCounter;
						nodeClosePending = false;
				}
			} else {
				label287:
				switch (this.nextToken.kind) {
					case 23:
					case 25:
					case 27:
					case 28:
					case 36:
					case 37:
					case 39:
					case 40:
					case 42:
					case 43:
					case 45:
					case 46:
					case 47:
					case 48:
					case 50:
					case 51:
					case 52:
					case 56:
					case 57:
					case 60:
					case 61:
					case 63:
					case 64:
					case 67:
					case 69:
					case 71:
					case 72:
					case 74:
					case 77:
					case 80:
					case 81:
					case 83:
					case 85:
					case 86:
					case 87:
					case 90:
					case 97:
					case 210:
					case 213:
					case 214:
					case 216:
						if (this.matchesClassComplexAnnotationSpecifierLookahead(Integer.MAX_VALUE)) {
							this.parseClassComplexAnnotationSpecifier();
						}

						if (this.matchesJarQualifierBeforeLinkedClassName(Integer.MAX_VALUE)) {
							switch (this.nextToken.kind) {
								case 210:
									this.parseJarQualifier();
									break;
								default:
									this.la1Generation[84] = this.generationCounter;
							}

							switch (this.nextToken.kind) {
								case 23:
								case 25:
								case 28:
								case 36:
								case 37:
								case 39:
								case 40:
								case 42:
								case 43:
								case 45:
								case 46:
								case 47:
								case 48:
								case 51:
								case 52:
								case 56:
								case 60:
								case 61:
								case 63:
								case 64:
								case 67:
								case 69:
								case 71:
								case 74:
								case 77:
								case 80:
								case 81:
								case 85:
								case 86:
								case 87:
								case 90:
								case 213:
								case 214:
									this.parseComplexPackageSpecifier();
									break;
								default:
									this.la1Generation[85] = this.generationCounter;
							}

							this.parseLinkClassName();
							switch (this.nextToken.kind) {
								case 66:
									this.parseExtendsClause();
									break;
								default:
									this.la1Generation[86] = this.generationCounter;
							}

							switch (this.nextToken.kind) {
								case 91:
									this.parseImplementsClause();
									break;
								default:
									this.la1Generation[87] = this.generationCounter;
							}

							switch (this.nextToken.kind) {
								case 56:
									this.consumeParameter(56);
									this.parseLinkPackageName();

									while (true) {
										switch (this.nextToken.kind) {
											case 20:
												this.consumeParameter(20);
												this.parseLinkPackageName();
												break;
											default:
												this.la1Generation[88] = this.generationCounter;
												nodeClosePending = false;
												break label287;
										}
									}
								default:
									this.la1Generation[89] = this.generationCounter;
									nodeClosePending = false;
									break label287;
							}
						} else {
							switch (this.nextToken.kind) {
								case 23:
								case 25:
								case 28:
								case 36:
								case 37:
								case 39:
								case 40:
								case 42:
								case 43:
								case 45:
								case 46:
								case 47:
								case 48:
								case 50:
								case 51:
								case 52:
								case 56:
								case 57:
								case 60:
								case 61:
								case 63:
								case 64:
								case 67:
								case 69:
								case 71:
								case 72:
								case 74:
								case 77:
								case 80:
								case 81:
								case 83:
								case 85:
								case 86:
								case 87:
								case 90:
								case 97:
								case 210:
								case 213:
								case 214:
									while (this.matchesNegatableClassModifierLookahead(Integer.MAX_VALUE)) {
										switch (this.nextToken.kind) {
											case 25:
												this.parseNegatedClassModifier();
												break;
											case 40:
											case 50:
											case 57:
											case 67:
											case 72:
											case 77:
											case 83:
											case 87:
												this.parseClassModifier();
												break;
											default:
												this.la1Generation[90] = this.generationCounter;
												this.consumeParameter(-1);
												throw new ParsingException();
										}
									}

									if (this.matchesJarQualifierLookahead(Integer.MAX_VALUE)) {
										this.parseJarQualifier();
									}

									if (this.matchesComplexPackageSpecifierAlt8(Integer.MAX_VALUE)) {
										this.parseComplexPackageSpecifier();
									}

									this.parseComplexClassSpecifier();
									if (this.matchesContainingClause(Integer.MAX_VALUE)) {
										this.parseContainingClause();
									}

									switch (this.nextToken.kind) {
										case 66:
											this.parseExtendsClause();
											break;
										default:
											this.la1Generation[91] = this.generationCounter;
									}

									switch (this.nextToken.kind) {
										case 91:
											this.parseImplementsClause();
											break;
										default:
											this.la1Generation[92] = this.generationCounter;
									}

									switch (this.nextToken.kind) {
										case 23:
										case 25:
										case 27:
										case 28:
										case 37:
										case 39:
										case 40:
										case 42:
										case 43:
										case 45:
										case 46:
										case 47:
										case 48:
										case 50:
										case 51:
										case 52:
										case 53:
										case 54:
										case 56:
										case 57:
										case 58:
										case 59:
										case 60:
										case 61:
										case 63:
										case 64:
										case 67:
										case 68:
										case 69:
										case 71:
										case 72:
										case 73:
										case 74:
										case 75:
										case 77:
										case 80:
										case 81:
										case 82:
										case 84:
										case 85:
										case 86:
										case 87:
										case 90:
										case 99:
										case 213:
										case 214:
										case 217:
											if (this.matchesMemberComplexAnnotationSpecifierAlt10(Integer.MAX_VALUE)) {
												this.parseMemberComplexAnnotationSpecifier();
											}

											while (this.matchesMemberModifierListAlt11(Integer.MAX_VALUE)) {
												switch (this.nextToken.kind) {
													case 25:
														this.parseNegatedMemberModifier();
														break;
													case 40:
													case 50:
													case 52:
													case 57:
													case 58:
													case 59:
													case 67:
													case 68:
													case 72:
													case 75:
													case 77:
													case 82:
													case 84:
													case 99:
														this.parseMemberModifier();
														break;
													default:
														this.la1Generation[93] = this.generationCounter;
														this.consumeParameter(-1);
														throw new ParsingException();
												}
											}

											if (this.matchesMethodSignatureAlt12(Integer.MAX_VALUE)) {
												this.parseMethodSignature();
												nodeClosePending = false;
											} else if (this.matchesLinkMethodSignature(Integer.MAX_VALUE)) {
												this.parseLinkMethodSignature();
												nodeClosePending = false;
											} else {
												switch (this.nextToken.kind) {
													case 23:
													case 25:
													case 28:
													case 37:
													case 39:
													case 40:
													case 42:
													case 43:
													case 45:
													case 46:
													case 47:
													case 48:
													case 51:
													case 52:
													case 56:
													case 60:
													case 61:
													case 63:
													case 64:
													case 69:
													case 71:
													case 74:
													case 77:
													case 80:
													case 81:
													case 85:
													case 86:
													case 87:
													case 90:
													case 213:
													case 214:
														this.parseFieldSignature();
														nodeClosePending = false;
														break label287;
													default:
														this.la1Generation[94] = this.generationCounter;
														this.consumeParameter(-1);
														throw new ParsingException();
												}
											}
											break label287;
										case 24:
										case 26:
										case 29:
										case 30:
										case 31:
										case 32:
										case 33:
										case 34:
										case 35:
										case 36:
										case 38:
										case 41:
										case 44:
										case 49:
										case 55:
										case 62:
										case 65:
										case 66:
										case 70:
										case 76:
										case 78:
										case 79:
										case 83:
										case 88:
										case 89:
										case 91:
										case 92:
										case 93:
										case 94:
										case 95:
										case 96:
										case 97:
										case 98:
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
										case 125:
										case 126:
										case 127:
										case 128:
										case 129:
										case 130:
										case 131:
										case 132:
										case 133:
										case 134:
										case 135:
										case 136:
										case 137:
										case 138:
										case 139:
										case 140:
										case 141:
										case 142:
										case 143:
										case 144:
										case 145:
										case 146:
										case 147:
										case 148:
										case 149:
										case 150:
										case 151:
										case 152:
										case 153:
										case 154:
										case 155:
										case 156:
										case 157:
										case 158:
										case 159:
										case 160:
										case 161:
										case 162:
										case 163:
										case 164:
										case 165:
										case 166:
										case 167:
										case 168:
										case 169:
										case 170:
										case 171:
										case 172:
										case 173:
										case 174:
										case 175:
										case 176:
										case 177:
										case 178:
										case 179:
										case 180:
										case 181:
										case 182:
										case 183:
										case 184:
										case 185:
										case 186:
										case 187:
										case 188:
										case 189:
										case 190:
										case 191:
										case 192:
										case 193:
										case 194:
										case 195:
										case 196:
										case 197:
										case 198:
										case 199:
										case 200:
										case 201:
										case 202:
										case 203:
										case 204:
										case 205:
										case 206:
										case 207:
										case 208:
										case 209:
										case 210:
										case 211:
										case 212:
										case 215:
										case 216:
										default:
											this.la1Generation[95] = this.generationCounter;
											nodeClosePending = false;
											break label287;
									}
								default:
									this.la1Generation[96] = this.generationCounter;
									this.consumeParameter(-1);
									throw new ParsingException();
							}
						}
					case 24:
					case 26:
					case 29:
					case 30:
					case 31:
					case 32:
					case 33:
					case 34:
					case 35:
					case 38:
					case 41:
					case 44:
					case 49:
					case 53:
					case 54:
					case 55:
					case 58:
					case 59:
					case 62:
					case 65:
					case 66:
					case 68:
					case 70:
					case 73:
					case 75:
					case 76:
					case 78:
					case 79:
					case 82:
					case 84:
					case 88:
					case 89:
					case 91:
					case 92:
					case 93:
					case 94:
					case 95:
					case 96:
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
					case 125:
					case 126:
					case 127:
					case 128:
					case 129:
					case 130:
					case 131:
					case 132:
					case 133:
					case 134:
					case 135:
					case 136:
					case 137:
					case 138:
					case 139:
					case 140:
					case 141:
					case 142:
					case 143:
					case 144:
					case 145:
					case 146:
					case 147:
					case 148:
					case 149:
					case 150:
					case 151:
					case 152:
					case 153:
					case 154:
					case 155:
					case 156:
					case 157:
					case 158:
					case 159:
					case 160:
					case 161:
					case 162:
					case 163:
					case 164:
					case 165:
					case 166:
					case 167:
					case 168:
					case 169:
					case 170:
					case 171:
					case 172:
					case 173:
					case 174:
					case 175:
					case 176:
					case 177:
					case 178:
					case 179:
					case 180:
					case 181:
					case 182:
					case 183:
					case 184:
					case 185:
					case 186:
					case 187:
					case 188:
					case 189:
					case 190:
					case 191:
					case 192:
					case 193:
					case 194:
					case 195:
					case 196:
					case 197:
					case 198:
					case 199:
					case 200:
					case 201:
					case 202:
					case 203:
					case 204:
					case 205:
					case 206:
					case 207:
					case 208:
					case 209:
					case 211:
					case 212:
					case 215:
					default:
						this.la1Generation[97] = this.generationCounter;
						this.consumeParameter(-1);
						throw new ParsingException();
				}
			}
		} catch (Throwable throwable) {
			if (true) {
				this.lexicalBuffer.discardNode(renameFilterParameter);
				nodeOpen = false;
			} else {
				this.lexicalBuffer.pop();
			}
			if (throwable instanceof RuntimeException) {
				throw (RuntimeException)throwable;
			}

			if (throwable instanceof ParsingException) {
				throw (ParsingException)throwable;
			}

			throw (Error)throwable;
		} finally {
			if (nodeClosePending && nodeOpen) {
				this.lexicalBuffer.endNode(renameFilterParameter, true);
			}
		}

		this.lexicalBuffer.endNode(renameFilterParameter, true);
	}

	public final void parseObfuscateParametersParameter() {
		ObfuscateParametersParameter obfuscateParameters2 = new ObfuscateParametersParameter(106);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(obfuscateParameters2);

		try {
			try {
				ZKMScriptToken keywordToken = this.consumeParameter(135);
				this.consumeParameter(15);
				this.parseObfuscateParametersType();
				this.lexicalBuffer.endNode(obfuscateParameters2, true);
				nodeOpen = false;
				obfuscateParameters2.setParameterName(keywordToken.image);
			} catch (Throwable throwable) {
				if (nodeOpen) {
					this.lexicalBuffer.discardNode(obfuscateParameters2);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			}
		} catch (Throwable throwable2) {
			if (nodeOpen) {
				this.lexicalBuffer.endNode(obfuscateParameters2, true);
			}

			throw throwable2;
		}
	}

	public final void parseBoolean() {
		Boolean_JavaCC booleanLiteral = new Boolean_JavaCC(222);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(booleanLiteral);

		try {
			switch (this.nextToken.kind) {
				case 41:
					ZKMScriptToken trueToken = this.consumeParameter(41);
					this.lexicalBuffer.endNode(booleanLiteral, true);
					nodeOpen = false;
					booleanLiteral.setText(trueToken.image);
					return;
				case 49:
					ZKMScriptToken falseToken = this.consumeParameter(49);
					this.lexicalBuffer.endNode(booleanLiteral, true);
					nodeOpen = false;
					booleanLiteral.setText(falseToken.image);
					return;
				default:
					this.la1Generation[184] = this.generationCounter;
					this.consumeParameter(-1);
					throw new ParsingException();
			}
		} finally {
			if (nodeOpen) {
				this.lexicalBuffer.endNode(booleanLiteral, true);
			}
		}
	}

	public final void parseBracketedMethodSpecifier() {
		BracketedMethodSpecifier bracketedMethodSpecifier = new BracketedMethodSpecifier(210);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(bracketedMethodSpecifier);

		try {
			switch (this.nextToken.kind) {
				case 25:
					this.consumeParameter(25);
					bracketedMethodSpecifier.setNegated();
					break;
				default:
					this.la1Generation[174] = this.generationCounter;
			}

			this.consumeParameter(28);
			this.parseAndMethodSpecifier();

			while (true) {
				switch (this.nextToken.kind) {
					case 34:
						this.consumeParameter(34);
						this.parseAndMethodSpecifier();
						break;
					default:
						this.la1Generation[175] = this.generationCounter;
						this.consumeParameter(29);
						this.lexicalBuffer.endNode(bracketedMethodSpecifier, true);
						return;
				}
			}
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (true) {
					this.lexicalBuffer.discardNode(bracketedMethodSpecifier);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}
				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable throwable2) {
				if (nodeOpen) {
					this.lexicalBuffer.endNode(bracketedMethodSpecifier, true);
				}

				throw throwable2;
			}
		}
	}

	public final void parseOpenNestedArchivesParameter() {
		OpenNestedArchivesParameter openNestedArchives2 = new OpenNestedArchivesParameter(3);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(openNestedArchives2);

		try {
			try {
				ZKMScriptToken keywordToken = this.consumeParameter(130);
				this.consumeParameter(15);
				this.parseBoolean();
				this.lexicalBuffer.endNode(openNestedArchives2, true);
				nodeOpen = false;
				openNestedArchives2.setParameterName(keywordToken.image);
			} catch (Throwable throwable) {
				if (nodeOpen) {
					this.lexicalBuffer.discardNode(openNestedArchives2);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			}
		} catch (Throwable throwable2) {
			if (nodeOpen) {
				this.lexicalBuffer.endNode(openNestedArchives2, true);
			}

			throw throwable2;
		}
	}

	public final void parseMethodParametersParameter() {
		MethodParametersParameter methodParameters2 = new MethodParametersParameter(115);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(methodParameters2);

		try {
			try {
				ZKMScriptToken keywordToken = this.consumeParameter(113);
				this.consumeParameter(15);
				this.parseMethodParametersParameterType();
				this.lexicalBuffer.endNode(methodParameters2, true);
				nodeOpen = false;
				methodParameters2.setParameterName(keywordToken.image);
			} catch (Throwable throwable) {
				if (nodeOpen) {
					this.lexicalBuffer.discardNode(methodParameters2);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			}
		} catch (Throwable throwable2) {
			if (nodeOpen) {
				this.lexicalBuffer.endNode(methodParameters2, true);
			}

			throw throwable2;
		}
	}

	public final void parseLinkPackageName() {
		LinkPackageName linkPackageName = new LinkPackageName(193);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(linkPackageName);

		try {
			this.parseLinkLiteralPackageComponent();

			while (this.matchesDotLinkLiteralPackageComponent(2)) {
				this.consumeParameter(21);
				this.parseLinkLiteralPackageComponent();
			}

			switch (this.nextToken.kind) {
				case 21:
					this.consumeParameter(21);
					break;
				default:
					this.la1Generation[151] = this.generationCounter;
			}

			this.lexicalBuffer.endNode(linkPackageName, true);
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (true) {
					this.lexicalBuffer.discardNode(linkPackageName);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}
				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable throwable2) {
				if (nodeOpen) {
					this.lexicalBuffer.endNode(linkPackageName, true);
				}

				throw throwable2;
			}
		}
	}

	public final void parseResetExistingSerializedClasses() {
		ResetExistingSerializedClassesStatement resetExistingSerializedClasses = new ResetExistingSerializedClassesStatement(73);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(resetExistingSerializedClasses);

		try {
			ZKMScriptToken keywordToken = this.consumeParameter(195);
			this.statementKind = 195;
			this.consumeParameter(19);
			this.lexicalBuffer.endNode(resetExistingSerializedClasses, true);
			nodeOpen = false;
			resetExistingSerializedClasses.setLineNumber(keywordToken.beginLine);
		} catch (Throwable throwable) {
			if (nodeOpen) {
				this.lexicalBuffer.endNode(resetExistingSerializedClasses, true);
			}

			throw throwable;
		}
	}

	public final void parseLineNumberParameterType() {
		LineNumberParameterType lineNumberParameterType = new LineNumberParameterType(167);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(lineNumberParameterType);

		try {
			switch (this.nextToken.kind) {
				case 42:
					ZKMScriptToken keepToken = this.consumeParameter(42);
					this.lexicalBuffer.endNode(lineNumberParameterType, true);
					nodeOpen = false;
					lineNumberParameterType.setText(keepToken.image);
					return;
				case 60:
					ZKMScriptToken deleteToken = this.consumeParameter(60);
					this.lexicalBuffer.endNode(lineNumberParameterType, true);
					nodeOpen = false;
					lineNumberParameterType.setText(deleteToken.image);
					return;
				case 71:
					ZKMScriptToken scrambleToken = this.consumeParameter(71);
					this.lexicalBuffer.endNode(lineNumberParameterType, true);
					nodeOpen = false;
					lineNumberParameterType.setText(scrambleToken.image);
					return;
				default:
					this.la1Generation[131] = this.generationCounter;
					this.consumeParameter(-1);
					throw new ParsingException();
			}
		} finally {
			if (nodeOpen) {
				this.lexicalBuffer.endNode(lineNumberParameterType, true);
			}
		}
	}

	public final void parseDeleteDeprecatedAttributesParameter() {
		DeleteDeprecatedAttributesParameter deleteDeprecatedAttributes2 = new DeleteDeprecatedAttributesParameter(89);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(deleteDeprecatedAttributes2);

		try {
			try {
				ZKMScriptToken keywordToken = this.consumeParameter(179);
				this.consumeParameter(15);
				this.parseBoolean();
				this.lexicalBuffer.endNode(deleteDeprecatedAttributes2, true);
				nodeOpen = false;
				deleteDeprecatedAttributes2.setParameterName(keywordToken.image);
			} catch (Throwable throwable) {
				if (nodeOpen) {
					this.lexicalBuffer.discardNode(deleteDeprecatedAttributes2);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			}
		} catch (Throwable throwable2) {
			if (nodeOpen) {
				this.lexicalBuffer.endNode(deleteDeprecatedAttributes2, true);
			}

			throw throwable2;
		}
	}

	public final void parseModuleNameComponent() {
		ModuleNameComponent moduleNameComponent = new ModuleNameComponent(190);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(moduleNameComponent);

		try {
			switch (this.nextToken.kind) {
				case 23:
					ZKMScriptToken wildcardToken = this.consumeParameter(23);
					moduleNameComponent.setText(wildcardToken.image);
					this.lexicalBuffer.endNode(moduleNameComponent, true);
					nodeOpen = false;
					wildcardToken.setDescriptionIndex(1);
					return;
				case 36:
					ZKMScriptToken gcToken = this.consumeParameter(36);
					moduleNameComponent.setText(gcToken.image);
					this.lexicalBuffer.endNode(moduleNameComponent, true);
					nodeOpen = false;
					gcToken.setDescriptionIndex(1);
					return;
				case 37:
					ZKMScriptToken allToken = this.consumeParameter(37);
					moduleNameComponent.setText(allToken.image);
					this.lexicalBuffer.endNode(moduleNameComponent, true);
					nodeOpen = false;
					allToken.setDescriptionIndex(1);
					return;
				case 39:
					ZKMScriptToken openToken = this.consumeParameter(39);
					moduleNameComponent.setText(openToken.image);
					this.lexicalBuffer.endNode(moduleNameComponent, true);
					nodeOpen = false;
					openToken.setDescriptionIndex(1);
					return;
				case 40:
					ZKMScriptToken enumToken = this.consumeParameter(40);
					moduleNameComponent.setText(enumToken.image);
					this.lexicalBuffer.endNode(moduleNameComponent, true);
					nodeOpen = false;
					enumToken.setDescriptionIndex(1);
					return;
				case 42:
					ZKMScriptToken keepToken = this.consumeParameter(42);
					moduleNameComponent.setText(keepToken.image);
					this.lexicalBuffer.endNode(moduleNameComponent, true);
					nodeOpen = false;
					keepToken.setDescriptionIndex(1);
					return;
				case 43:
					ZKMScriptToken noneToken = this.consumeParameter(43);
					moduleNameComponent.setText(noneToken.image);
					this.lexicalBuffer.endNode(moduleNameComponent, true);
					nodeOpen = false;
					noneToken.setDescriptionIndex(1);
					return;
				case 45:
					ZKMScriptToken trimToken = this.consumeParameter(45);
					moduleNameComponent.setText(trimToken.image);
					this.lexicalBuffer.endNode(moduleNameComponent, true);
					nodeOpen = false;
					trimToken.setDescriptionIndex(1);
					return;
				case 46:
					ZKMScriptToken printToken = this.consumeParameter(46);
					moduleNameComponent.setText(printToken.image);
					this.lexicalBuffer.endNode(moduleNameComponent, true);
					nodeOpen = false;
					printToken.setDescriptionIndex(1);
					return;
				case 47:
					ZKMScriptToken lightToken = this.consumeParameter(47);
					moduleNameComponent.setText(lightToken.image);
					this.lexicalBuffer.endNode(moduleNameComponent, true);
					nodeOpen = false;
					lightToken.setDescriptionIndex(1);
					return;
				case 48:
					ZKMScriptToken heavyToken = this.consumeParameter(48);
					moduleNameComponent.setText(heavyToken.image);
					this.lexicalBuffer.endNode(moduleNameComponent, true);
					nodeOpen = false;
					heavyToken.setDescriptionIndex(1);
					return;
				case 51:
					ZKMScriptToken asciiToken = this.consumeParameter(51);
					moduleNameComponent.setText(asciiToken.image);
					this.lexicalBuffer.endNode(moduleNameComponent, true);
					nodeOpen = false;
					asciiToken.setDescriptionIndex(1);
					return;
				case 52:
					ZKMScriptToken bridgeToken = this.consumeParameter(52);
					moduleNameComponent.setText(bridgeToken.image);
					this.lexicalBuffer.endNode(moduleNameComponent, true);
					nodeOpen = false;
					bridgeToken.setDescriptionIndex(1);
					return;
				case 56:
					ZKMScriptToken searchToken = this.consumeParameter(56);
					moduleNameComponent.setText(searchToken.image);
					this.lexicalBuffer.endNode(moduleNameComponent, true);
					nodeOpen = false;
					searchToken.setDescriptionIndex(1);
					return;
				case 60:
					ZKMScriptToken deleteToken = this.consumeParameter(60);
					moduleNameComponent.setText(deleteToken.image);
					this.lexicalBuffer.endNode(moduleNameComponent, true);
					nodeOpen = false;
					deleteToken.setDescriptionIndex(1);
					return;
				case 61:
					ZKMScriptToken normalToken = this.consumeParameter(61);
					moduleNameComponent.setText(normalToken.image);
					this.lexicalBuffer.endNode(moduleNameComponent, true);
					nodeOpen = false;
					normalToken.setDescriptionIndex(1);
					return;
				case 63:
					ZKMScriptToken randomToken = this.consumeParameter(63);
					moduleNameComponent.setText(randomToken.image);
					this.lexicalBuffer.endNode(moduleNameComponent, true);
					nodeOpen = false;
					randomToken.setDescriptionIndex(1);
					return;
				case 64:
					ZKMScriptToken excludeToken = this.consumeParameter(64);
					moduleNameComponent.setText(excludeToken.image);
					this.lexicalBuffer.endNode(moduleNameComponent, true);
					nodeOpen = false;
					excludeToken.setDescriptionIndex(1);
					return;
				case 69:
					ZKMScriptToken executeToken = this.consumeParameter(69);
					moduleNameComponent.setText(executeToken.image);
					this.lexicalBuffer.endNode(moduleNameComponent, true);
					nodeOpen = false;
					executeToken.setDescriptionIndex(1);
					return;
				case 71:
					ZKMScriptToken scrambleToken = this.consumeParameter(71);
					moduleNameComponent.setText(scrambleToken.image);
					this.lexicalBuffer.endNode(moduleNameComponent, true);
					nodeOpen = false;
					scrambleToken.setDescriptionIndex(1);
					return;
				case 74:
					ZKMScriptToken enhancedToken = this.consumeParameter(74);
					moduleNameComponent.setText(enhancedToken.image);
					this.lexicalBuffer.endNode(moduleNameComponent, true);
					nodeOpen = false;
					enhancedToken.setDescriptionIndex(1);
					return;
				case 77:
					ZKMScriptToken syntheticToken = this.consumeParameter(77);
					moduleNameComponent.setText(syntheticToken.image);
					this.lexicalBuffer.endNode(moduleNameComponent, true);
					nodeOpen = false;
					syntheticToken.setDescriptionIndex(1);
					return;
				case 80:
					ZKMScriptToken classpathToken = this.consumeParameter(80);
					moduleNameComponent.setText(classpathToken.image);
					this.lexicalBuffer.endNode(moduleNameComponent, true);
					nodeOpen = false;
					classpathToken.setDescriptionIndex(1);
					return;
				case 81:
					ZKMScriptToken obfuscateToken = this.consumeParameter(81);
					moduleNameComponent.setText(obfuscateToken.image);
					this.lexicalBuffer.endNode(moduleNameComponent, true);
					nodeOpen = false;
					obfuscateToken.setDescriptionIndex(1);
					return;
				case 85:
					ZKMScriptToken randomizeToken = this.consumeParameter(85);
					moduleNameComponent.setText(randomizeToken.image);
					this.lexicalBuffer.endNode(moduleNameComponent, true);
					nodeOpen = false;
					randomizeToken.setDescriptionIndex(1);
					return;
				case 86:
					ZKMScriptToken groupingsToken = this.consumeParameter(86);
					moduleNameComponent.setText(groupingsToken.image);
					this.lexicalBuffer.endNode(moduleNameComponent, true);
					nodeOpen = false;
					groupingsToken.setDescriptionIndex(1);
					return;
				case 87:
					ZKMScriptToken annotationToken = this.consumeParameter(87);
					moduleNameComponent.setText(annotationToken.image);
					this.lexicalBuffer.endNode(moduleNameComponent, true);
					nodeOpen = false;
					annotationToken.setDescriptionIndex(1);
					return;
				case 90:
					ZKMScriptToken aggressiveToken = this.consumeParameter(90);
					moduleNameComponent.setText(aggressiveToken.image);
					this.lexicalBuffer.endNode(moduleNameComponent, true);
					nodeOpen = false;
					aggressiveToken.setDescriptionIndex(1);
					return;
				case 213:
					ZKMScriptToken nameToken = this.consumeParameter(213);
					moduleNameComponent.setText(nameToken.image);
					this.lexicalBuffer.endNode(moduleNameComponent, true);
					nodeOpen = false;
					nameToken.setDescriptionIndex(1);
					return;
				case 214:
					ZKMScriptToken wildcardNameToken = this.consumeParameter(214);
					moduleNameComponent.setText(wildcardNameToken.image);
					this.lexicalBuffer.endNode(moduleNameComponent, true);
					nodeOpen = false;
					wildcardNameToken.setDescriptionIndex(1);
					return;
				case 215:
					ZKMScriptToken moduleNameToken = this.consumeParameter(215);
					moduleNameComponent.setText(moduleNameToken.image);
					this.lexicalBuffer.endNode(moduleNameComponent, true);
					nodeOpen = false;
					moduleNameToken.setDescriptionIndex(1);
					return;
				default:
					this.la1Generation[149] = this.generationCounter;
					this.consumeParameter(-1);
					throw new ParsingException();
			}
		} finally {
			if (nodeOpen) {
				this.lexicalBuffer.endNode(moduleNameComponent, true);
			}
		}
	}

	public final void parseClassInitializationOrder() {
		ClassInitializationOrderStatement classInitializationOrder = new ClassInitializationOrderStatement(83);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(classInitializationOrder);

		try {
			try {
				ZKMScriptToken keywordToken = this.consumeParameter(167);
				this.statementKind = 167;
				this.parseClassInitializationOrderPair();

				while (true) {
					switch (this.nextToken.kind) {
						case 38:
							this.consumeParameter(38);
							this.parseClassInitializationOrderPair();
							break;
						default:
							this.la1Generation[78] = this.generationCounter;
							this.consumeParameter(19);
							this.lexicalBuffer.endNode(classInitializationOrder, true);
							nodeOpen = false;
							classInitializationOrder.setLineNumber(keywordToken.beginLine);
							return;
					}
				}
			} catch (Throwable throwable) {
				if (nodeOpen) {
					this.lexicalBuffer.discardNode(classInitializationOrder);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			}
		} catch (Throwable throwable2) {
			if (nodeOpen) {
				this.lexicalBuffer.endNode(classInitializationOrder, true);
			}

			throw throwable2;
		}
	}

	public final void parseDeleteExceptionAttributesParameter() {
		DeleteExceptionAttributesParameter deleteExceptionAttributes2 = new DeleteExceptionAttributesParameter(91);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(deleteExceptionAttributes2);

		try {
			try {
				ZKMScriptToken keywordToken = this.consumeParameter(172);
				this.consumeParameter(15);
				this.parseBoolean();
				this.lexicalBuffer.endNode(deleteExceptionAttributes2, true);
				nodeOpen = false;
				deleteExceptionAttributes2.setParameterName(keywordToken.image);
			} catch (Throwable throwable) {
				if (nodeOpen) {
					this.lexicalBuffer.discardNode(deleteExceptionAttributes2);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			}
		} catch (Throwable throwable2) {
			if (nodeOpen) {
				this.lexicalBuffer.endNode(deleteExceptionAttributes2, true);
			}

			throw throwable2;
		}
	}

	public final AbstractScriptNode parseSingleRenameFilterParameter() {
		SingleRenameFilterParameter singleExcludeParameter = new SingleRenameFilterParameter(52);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(singleExcludeParameter);

		try {
			switch (this.nextToken.kind) {
				case 14:
					this.consumeParameter(14);
					break;
				default:
					this.la1Generation[68] = this.generationCounter;
			}

			this.parseRenameFilterParameter();
			this.consumeParameter(19);
			this.consumeParameter(0);
			this.lexicalBuffer.endNode(singleExcludeParameter, true);
			nodeOpen = false;
			return singleExcludeParameter;
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (nodeOpen) {
					this.lexicalBuffer.discardNode(singleExcludeParameter);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable throwable2) {
				if (nodeOpen) {
					this.lexicalBuffer.endNode(singleExcludeParameter, true);
				}

				throw throwable2;
			}
		}
	}

	public final void parseExecute() {
		ExecuteStatement execute = new ExecuteStatement(81);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(execute);

		try {
			try {
				ZKMScriptToken keywordToken = this.consumeParameter(69);
				this.statementKind = 69;
				this.parseStringLiteral();
				this.consumeParameter(19);
				this.lexicalBuffer.endNode(execute, true);
				nodeOpen = false;
				execute.setLineNumber(keywordToken.beginLine);
			} catch (Throwable throwable) {
				if (nodeOpen) {
					this.lexicalBuffer.discardNode(execute);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			}
		} catch (Throwable throwable2) {
			if (nodeOpen) {
				this.lexicalBuffer.endNode(execute, true);
			}

			throw throwable2;
		}
	}

	private boolean moduleNameComponentPrintKeywordLookahead() {
		return this.scanToken(46);
	}

	private boolean memberModifierBridgeKeywordLookahead() {
		return this.scanToken(52);
	}

	public final AbstractScriptNode existingSerializedClasses() {
		ExistingSerializedClassesStatement existingSerializedClasses = new ExistingSerializedClassesStatement(26);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(existingSerializedClasses);

		try {
			ZKMScriptToken keywordToken = this.consumeParameter(169);
			this.statementKind = 169;
			this.parseRenameFilterParameter();

			while (true) {
				switch (this.nextToken.kind) {
					case 38:
						this.consumeParameter(38);
						this.parseRenameFilterParameter();
						break;
					default:
						this.la1Generation[28] = this.generationCounter;
						this.consumeParameter(19);
						this.lexicalBuffer.endNode(existingSerializedClasses, true);
						nodeOpen = false;
						existingSerializedClasses.setLineNumber(keywordToken.beginLine);
						return existingSerializedClasses;
				}
			}
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (nodeOpen) {
					this.lexicalBuffer.discardNode(existingSerializedClasses);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable throwable2) {
				if (nodeOpen) {
					this.lexicalBuffer.endNode(existingSerializedClasses, true);
				}

				throw throwable2;
			}
		}
	}

	public final void parseResetMethodParameterObfuscationExclusions() {
		ResetMethodParameterObfuscationExclusionsStatement resetMethodParameterObfuscationExclusions = new ResetMethodParameterObfuscationExclusionsStatement(79);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(resetMethodParameterObfuscationExclusions);

		try {
			ZKMScriptToken keywordToken = this.consumeParameter(207);
			this.statementKind = 207;
			this.consumeParameter(19);
			this.lexicalBuffer.endNode(resetMethodParameterObfuscationExclusions, true);
			nodeOpen = false;
			resetMethodParameterObfuscationExclusions.setLineNumber(keywordToken.beginLine);
		} catch (Throwable throwable) {
			if (nodeOpen) {
				this.lexicalBuffer.endNode(resetMethodParameterObfuscationExclusions, true);
			}

			throw throwable;
		}
	}

	public final AbstractScriptNode exclude() {
		ExcludeStatement exclude = new ExcludeStatement(14);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(exclude);

		try {
			ZKMScriptToken keywordToken = this.consumeParameter(64);
			this.statementKind = 64;
			this.parseRenameFilterParameter();

			while (true) {
				switch (this.nextToken.kind) {
					case 38:
						this.consumeParameter(38);
						this.parseRenameFilterParameter();
						break;
					default:
						this.la1Generation[16] = this.generationCounter;
						this.consumeParameter(19);
						this.lexicalBuffer.endNode(exclude, true);
						nodeOpen = false;
						exclude.setLineNumber(keywordToken.beginLine);
						return exclude;
				}
			}
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (nodeOpen) {
					this.lexicalBuffer.discardNode(exclude);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable throwable2) {
				if (nodeOpen) {
					this.lexicalBuffer.endNode(exclude, true);
				}

				throw throwable2;
			}
		}
	}

	public final void parseCharacterType() {
		CharacterType characterType = new CharacterType(127);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(characterType);

		try {
			switch (this.nextToken.kind) {
				case 51:
					ZKMScriptToken asciiToken = this.consumeParameter(51);
					this.lexicalBuffer.endNode(characterType, true);
					nodeOpen = false;
					characterType.setText(asciiToken.image);
					return;
				case 76:
					ZKMScriptToken nonAsciiToken = this.consumeParameter(76);
					this.lexicalBuffer.endNode(characterType, true);
					nodeOpen = false;
					characterType.setText(nonAsciiToken.image);
					return;
				default:
					this.la1Generation[104] = this.generationCounter;
					this.consumeParameter(-1);
					throw new ParsingException();
			}
		} finally {
			if (nodeOpen) {
				this.lexicalBuffer.endNode(characterType, true);
			}
		}
	}

	private boolean jarQualifierStringLiteralLookahead() {
		return this.scanToken(210);
	}

	public final void parseNegatedFileFilterComponent() {
		NegatedFileFilterComponent negatedFileFilterComponent = new NegatedFileFilterComponent(13);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(negatedFileFilterComponent);

		try {
			this.consumeParameter(25);
			this.parseStringLiteral();
			this.lexicalBuffer.endNode(negatedFileFilterComponent, true);
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (true) {
					this.lexicalBuffer.discardNode(negatedFileFilterComponent);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}
				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable throwable2) {
				if (nodeOpen) {
					this.lexicalBuffer.endNode(negatedFileFilterComponent, true);
				}

				throw throwable2;
			}
		}
	}

	private boolean matchesMemberComplexAnnotationSpecifierExpansionAlt10() {
		return this.startsComplexAnnotationSpecifier();
	}

	public final AbstractScriptNode obfuscateExceptionsExclude() {
		ObfuscateExceptionsExcludeStatement obfuscateExceptionsExclude = new ObfuscateExceptionsExcludeStatement(18);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(obfuscateExceptionsExclude);

		try {
			ZKMScriptToken keywordToken = this.consumeParameter(173);
			this.statementKind = 173;
			this.parseRenameFilterParameter();

			while (true) {
				switch (this.nextToken.kind) {
					case 38:
						this.consumeParameter(38);
						this.parseRenameFilterParameter();
						break;
					default:
						this.la1Generation[20] = this.generationCounter;
						this.consumeParameter(19);
						this.lexicalBuffer.endNode(obfuscateExceptionsExclude, true);
						nodeOpen = false;
						obfuscateExceptionsExclude.setLineNumber(keywordToken.beginLine);
						return obfuscateExceptionsExclude;
				}
			}
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (nodeOpen) {
					this.lexicalBuffer.discardNode(obfuscateExceptionsExclude);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable throwable2) {
				if (nodeOpen) {
					this.lexicalBuffer.endNode(obfuscateExceptionsExclude, true);
				}

				throw throwable2;
			}
		}
	}

	private boolean matchesLinkMethodSignatureExpansion() {
		return this.matchesLinkMethodSignatureStart() ? true : this.scanToken(28);
	}

	public final void parseAllowMethodParameterChangesParameter() {
		AllowMethodParameterChangesParameter allowMethodParameterChanges2 = new AllowMethodParameterChangesParameter(123);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(allowMethodParameterChanges2);

		try {
			try {
				ZKMScriptToken keywordToken = this.consumeParameter(183);
				this.consumeParameter(15);
				this.parseBoolean();
				this.lexicalBuffer.endNode(allowMethodParameterChanges2, true);
				nodeOpen = false;
				allowMethodParameterChanges2.setParameterName(keywordToken.image);
			} catch (Throwable throwable) {
				if (nodeOpen) {
					this.lexicalBuffer.discardNode(allowMethodParameterChanges2);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			}
		} catch (Throwable throwable2) {
			if (nodeOpen) {
				this.lexicalBuffer.endNode(allowMethodParameterChanges2, true);
			}

			throw throwable2;
		}
	}

	public final void parseObfuscateReferenceStructuresParameter() {
		ObfuscateReferenceStructuresParameter obfuscateReferenceStructures2 = new ObfuscateReferenceStructuresParameter(139);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(obfuscateReferenceStructures2);

		try {
			try {
				ZKMScriptToken keywordToken = this.consumeParameter(188);
				this.consumeParameter(15);
				this.parseObfuscateReferenceStructuresType();
				this.lexicalBuffer.endNode(obfuscateReferenceStructures2, true);
				nodeOpen = false;
				obfuscateReferenceStructures2.setParameterName(keywordToken.image);
			} catch (Throwable throwable) {
				if (nodeOpen) {
					this.lexicalBuffer.discardNode(obfuscateReferenceStructures2);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			}
		} catch (Throwable throwable2) {
			if (nodeOpen) {
				this.lexicalBuffer.endNode(obfuscateReferenceStructures2, true);
			}

			throw throwable2;
		}
	}

	public final void parseThrowsClause() {
		ThrowsClause throwsClause = new ThrowsClause(155);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(throwsClause);

		try {
			this.consumeParameter(62);
			this.parseQualifiedClassName();

			while (true) {
				switch (this.nextToken.kind) {
					case 20:
						this.consumeParameter(20);
						this.parseQualifiedClassName();
						break;
					default:
						this.la1Generation[120] = this.generationCounter;
						this.lexicalBuffer.endNode(throwsClause, true);
						return;
				}
			}
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (true) {
					this.lexicalBuffer.discardNode(throwsClause);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}
				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable throwable2) {
				if (nodeOpen) {
					this.lexicalBuffer.endNode(throwsClause, true);
				}

				throw throwable2;
			}
		}
	}

	public final void parseComplexPackageSpecifier() {
		ComplexPackageSpecifier complexPackageSpecifier = new ComplexPackageSpecifier(185);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(complexPackageSpecifier);

		try {
			switch (this.nextToken.kind) {
				case 23:
				case 36:
				case 37:
				case 39:
				case 40:
				case 42:
				case 43:
				case 45:
				case 46:
				case 47:
				case 48:
				case 51:
				case 52:
				case 56:
				case 60:
				case 61:
				case 63:
				case 64:
				case 67:
				case 69:
				case 71:
				case 74:
				case 77:
				case 80:
				case 81:
				case 85:
				case 86:
				case 87:
				case 90:
				case 213:
				case 214:
					this.parsePackageName();
					break;
				case 25:
				case 28:
					this.parseBracketedPackageSpecifier();
					break;
				default:
					this.la1Generation[143] = this.generationCounter;
					this.consumeParameter(-1);
					throw new ParsingException();
			}

			switch (this.nextToken.kind) {
				case 21:
					this.consumeParameter(21);
					complexPackageSpecifier.setPackageExactMatch();
					break;
				default:
					this.la1Generation[144] = this.generationCounter;
			}

			switch (this.nextToken.kind) {
				case 26:
					this.consumeParameter(26);
					complexPackageSpecifier.setPackageRecursive();
					break;
				default:
					this.la1Generation[145] = this.generationCounter;
			}

			this.lexicalBuffer.endNode(complexPackageSpecifier, true);
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (true) {
					this.lexicalBuffer.discardNode(complexPackageSpecifier);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}
				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable throwable2) {
				if (nodeOpen) {
					this.lexicalBuffer.endNode(complexPackageSpecifier, true);
				}

				throw throwable2;
			}
		}
	}

	public final void parseLastModifiedTime() {
		LastModifiedTime lastModifiedTime = new LastModifiedTime(63);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(lastModifiedTime);

		try {
			try {
				ZKMScriptToken keywordToken = this.consumeParameter(120);
				this.consumeParameter(15);
				this.parseStringLiteral();
				this.lexicalBuffer.endNode(lastModifiedTime, true);
				nodeOpen = false;
				lastModifiedTime.setParameterName(keywordToken.image);
			} catch (Throwable throwable) {
				if (nodeOpen) {
					this.lexicalBuffer.discardNode(lastModifiedTime);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			}
		} catch (Throwable throwable2) {
			if (nodeOpen) {
				this.lexicalBuffer.endNode(lastModifiedTime, true);
			}

			throw throwable2;
		}
	}

	public final void parseClassModifier() {
		ClassModifier classModifier = new ClassModifier(140);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(classModifier);

		try {
			switch (this.nextToken.kind) {
				case 40:
					ZKMScriptToken enumToken = this.consumeParameter(40);
					this.lexicalBuffer.endNode(classModifier, true);
					nodeOpen = false;
					classModifier.setText(enumToken.image);
					return;
				case 50:
					ZKMScriptToken finalToken = this.consumeParameter(50);
					this.lexicalBuffer.endNode(classModifier, true);
					nodeOpen = false;
					classModifier.setText(finalToken.image);
					return;
				case 57:
					ZKMScriptToken publicToken = this.consumeParameter(57);
					this.lexicalBuffer.endNode(classModifier, true);
					nodeOpen = false;
					classModifier.setText(publicToken.image);
					return;
				case 67:
					ZKMScriptToken packageToken = this.consumeParameter(67);
					this.lexicalBuffer.endNode(classModifier, true);
					nodeOpen = false;
					classModifier.setText(packageToken.image);
					return;
				case 72:
					ZKMScriptToken abstractToken = this.consumeParameter(72);
					this.lexicalBuffer.endNode(classModifier, true);
					nodeOpen = false;
					classModifier.setText(abstractToken.image);
					return;
				case 77:
					ZKMScriptToken syntheticToken = this.consumeParameter(77);
					this.lexicalBuffer.endNode(classModifier, true);
					nodeOpen = false;
					classModifier.setText(syntheticToken.image);
					return;
				case 83:
					ZKMScriptToken interfaceToken = this.consumeParameter(83);
					this.lexicalBuffer.endNode(classModifier, true);
					nodeOpen = false;
					classModifier.setText(interfaceToken.image);
					return;
				case 87:
					ZKMScriptToken annotationToken = this.consumeParameter(87);
					this.lexicalBuffer.endNode(classModifier, true);
					nodeOpen = false;
					classModifier.setText(annotationToken.image);
					return;
				default:
					this.la1Generation[105] = this.generationCounter;
					this.consumeParameter(-1);
					throw new ParsingException();
			}
		} finally {
			if (nodeOpen) {
				this.lexicalBuffer.endNode(classModifier, true);
			}
		}
	}

	public final void parseModuleName() {
		ModuleName moduleName = new ModuleName(189);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(moduleName);

		try {
			while (this.matchesModuleNameComponentBeforeDot(Integer.MAX_VALUE)) {
				this.parseModuleNameComponent();
				this.consumeParameter(21);
			}

			this.parseModuleNameComponent();
			this.consumeParameter(22);
			this.lexicalBuffer.endNode(moduleName, true);
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (true) {
					this.lexicalBuffer.discardNode(moduleName);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}
				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable throwable2) {
				if (nodeOpen) {
					this.lexicalBuffer.endNode(moduleName, true);
				}

				throw throwable2;
			}
		}
	}

	public final void parseFlowObfuscationType() {
		FlowObfuscationType flowObfuscationType = new FlowObfuscationType(162);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(flowObfuscationType);

		try {
			switch (this.nextToken.kind) {
				case 43:
					ZKMScriptToken noneToken = this.consumeParameter(43);
					this.lexicalBuffer.endNode(flowObfuscationType, true);
					nodeOpen = false;
					flowObfuscationType.setText(noneToken.image);
					return;
				case 47:
					ZKMScriptToken lightToken = this.consumeParameter(47);
					this.lexicalBuffer.endNode(flowObfuscationType, true);
					nodeOpen = false;
					flowObfuscationType.setText(lightToken.image);
					return;
				case 61:
					ZKMScriptToken normalToken = this.consumeParameter(61);
					this.lexicalBuffer.endNode(flowObfuscationType, true);
					nodeOpen = false;
					flowObfuscationType.setText(normalToken.image);
					return;
				case 90:
					ZKMScriptToken aggressiveToken = this.consumeParameter(90);
					this.lexicalBuffer.endNode(flowObfuscationType, true);
					nodeOpen = false;
					flowObfuscationType.setText(aggressiveToken.image);
					return;
				case 109:
					ZKMScriptToken extraAggressiveToken = this.consumeParameter(109);
					this.lexicalBuffer.endNode(flowObfuscationType, true);
					nodeOpen = false;
					flowObfuscationType.setText(extraAggressiveToken.image);
					return;
				default:
					this.la1Generation[126] = this.generationCounter;
					this.consumeParameter(-1);
					throw new ParsingException();
			}
		} finally {
			if (nodeOpen) {
				this.lexicalBuffer.endNode(flowObfuscationType, true);
			}
		}
	}

	public final void parseComplexModuleSpecifier() {
		ComplexModuleSpecifier complexModuleSpecifier = new ComplexModuleSpecifier(182);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(complexModuleSpecifier);

		try {
			switch (this.nextToken.kind) {
				case 23:
				case 36:
				case 37:
				case 39:
				case 40:
				case 42:
				case 43:
				case 45:
				case 46:
				case 47:
				case 48:
				case 51:
				case 52:
				case 56:
				case 60:
				case 61:
				case 63:
				case 64:
				case 69:
				case 71:
				case 74:
				case 77:
				case 80:
				case 81:
				case 85:
				case 86:
				case 87:
				case 90:
				case 213:
				case 214:
				case 215:
					this.parseModuleName();
					break;
				case 25:
				case 28:
					this.parseBracketedModuleSpecifier();
					break;
				default:
					this.la1Generation[138] = this.generationCounter;
					this.consumeParameter(-1);
					throw new ParsingException();
			}

			switch (this.nextToken.kind) {
				case 26:
					this.consumeParameter(26);
					complexModuleSpecifier.setRecursive();
					break;
				default:
					this.la1Generation[139] = this.generationCounter;
			}

			this.lexicalBuffer.endNode(complexModuleSpecifier, true);
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (true) {
					this.lexicalBuffer.discardNode(complexModuleSpecifier);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}
				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable throwable2) {
				if (nodeOpen) {
					this.lexicalBuffer.endNode(complexModuleSpecifier, true);
				}

				throw throwable2;
			}
		}
	}

	public final void parseEncryptParameter() {
		EncryptParameter encryptStringLiterals2 = new EncryptParameter(102);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(encryptStringLiterals2);

		try {
			try {
				ZKMScriptToken keywordToken = this.consumeParameter(150);
				this.consumeParameter(15);
				this.parseEncryptionType();
				this.lexicalBuffer.endNode(encryptStringLiterals2, true);
				nodeOpen = false;
				encryptStringLiterals2.setParameterName(keywordToken.image);
			} catch (Throwable throwable) {
				if (nodeOpen) {
					this.lexicalBuffer.discardNode(encryptStringLiterals2);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			}
		} catch (Throwable throwable2) {
			if (nodeOpen) {
				this.lexicalBuffer.endNode(encryptStringLiterals2, true);
			}

			throw throwable2;
		}
	}

	public final void parseLocalVariablesParameter() {
		LocalVariablesParameter localVariables2 = new LocalVariablesParameter(114);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(localVariables2);

		try {
			try {
				ZKMScriptToken keywordToken = this.consumeParameter(104);
				this.consumeParameter(15);
				this.parseLocalVariableParameterType();
				this.lexicalBuffer.endNode(localVariables2, true);
				nodeOpen = false;
				localVariables2.setParameterName(keywordToken.image);
			} catch (Throwable throwable) {
				if (nodeOpen) {
					this.lexicalBuffer.discardNode(localVariables2);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			}
		} catch (Throwable throwable2) {
			if (nodeOpen) {
				this.lexicalBuffer.endNode(localVariables2, true);
			}

			throw throwable2;
		}
	}

	public final void parseWildcardType() {
		WildcardType wildcardType = new WildcardType(221);
		this.lexicalBuffer.beginNode(wildcardType);

		try {
			this.consumeParameter(23);
		} finally {
			this.lexicalBuffer.endNode(wildcardType, true);
		}
	}

	public final void parseResetAccessedByReflection() {
		ResetAccessedByReflectionStatement resetAccessedByReflection = new ResetAccessedByReflectionStatement(76);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(resetAccessedByReflection);

		try {
			ZKMScriptToken keywordToken = this.consumeParameter(171);
			this.statementKind = 171;
			this.consumeParameter(19);
			this.lexicalBuffer.endNode(resetAccessedByReflection, true);
			nodeOpen = false;
			resetAccessedByReflection.setLineNumber(keywordToken.beginLine);
		} catch (Throwable throwable) {
			if (nodeOpen) {
				this.lexicalBuffer.endNode(resetAccessedByReflection, true);
			}

			throw throwable;
		}
	}

	public final AbstractScriptNode ignoreMissingReferences() {
		IgnoreMissingReferencesStatement ignoreMissingReferences = new IgnoreMissingReferencesStatement(28);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(ignoreMissingReferences);

		IgnoreMissingReferencesStatement returnedNode;
		try {
			ZKMScriptToken keywordToken;
			keywordToken = this.consumeParameter(161);
			this.statementKind = 161;
			label77:
			switch (this.nextToken.kind) {
				case 23:
				case 25:
				case 27:
				case 28:
				case 36:
				case 37:
				case 39:
				case 40:
				case 42:
				case 43:
				case 45:
				case 46:
				case 47:
				case 48:
				case 50:
				case 51:
				case 52:
				case 56:
				case 57:
				case 60:
				case 61:
				case 63:
				case 64:
				case 67:
				case 69:
				case 71:
				case 72:
				case 74:
				case 77:
				case 80:
				case 81:
				case 83:
				case 85:
				case 86:
				case 87:
				case 90:
				case 97:
				case 210:
				case 213:
				case 214:
				case 215:
				case 216:
					this.parseRenameFilterParameter();

					while (true) {
						switch (this.nextToken.kind) {
							case 38:
								this.consumeParameter(38);
								this.parseRenameFilterParameter();
								break;
							default:
								this.la1Generation[30] = this.generationCounter;
								break label77;
						}
					}
				case 24:
				case 26:
				case 29:
				case 30:
				case 31:
				case 32:
				case 33:
				case 34:
				case 35:
				case 38:
				case 41:
				case 44:
				case 49:
				case 53:
				case 54:
				case 55:
				case 58:
				case 59:
				case 62:
				case 65:
				case 66:
				case 68:
				case 70:
				case 73:
				case 75:
				case 76:
				case 78:
				case 79:
				case 82:
				case 84:
				case 88:
				case 89:
				case 91:
				case 92:
				case 93:
				case 94:
				case 95:
				case 96:
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
				case 125:
				case 126:
				case 127:
				case 128:
				case 129:
				case 130:
				case 131:
				case 132:
				case 133:
				case 134:
				case 135:
				case 136:
				case 137:
				case 138:
				case 139:
				case 140:
				case 141:
				case 142:
				case 143:
				case 144:
				case 145:
				case 146:
				case 147:
				case 148:
				case 149:
				case 150:
				case 151:
				case 152:
				case 153:
				case 154:
				case 155:
				case 156:
				case 157:
				case 158:
				case 159:
				case 160:
				case 161:
				case 162:
				case 163:
				case 164:
				case 165:
				case 166:
				case 167:
				case 168:
				case 169:
				case 170:
				case 171:
				case 172:
				case 173:
				case 174:
				case 175:
				case 176:
				case 177:
				case 178:
				case 179:
				case 180:
				case 181:
				case 182:
				case 183:
				case 184:
				case 185:
				case 186:
				case 187:
				case 188:
				case 189:
				case 190:
				case 191:
				case 192:
				case 193:
				case 194:
				case 195:
				case 196:
				case 197:
				case 198:
				case 199:
				case 200:
				case 201:
				case 202:
				case 203:
				case 204:
				case 205:
				case 206:
				case 207:
				case 208:
				case 209:
				case 211:
				case 212:
				default:
					this.la1Generation[31] = this.generationCounter;
			}

			this.consumeParameter(19);
			this.lexicalBuffer.endNode(ignoreMissingReferences, true);
			nodeOpen = false;
			ignoreMissingReferences.setLineNumber(keywordToken.beginLine);
			returnedNode = ignoreMissingReferences;
		} catch (Throwable throwable) {
			if (nodeOpen) {
				this.lexicalBuffer.discardNode(ignoreMissingReferences);
				nodeOpen = false;
			} else {
				this.lexicalBuffer.pop();
			}

			if (throwable instanceof RuntimeException) {
				throw (RuntimeException)throwable;
			}

			if (throwable instanceof ParsingException) {
				throw (ParsingException)throwable;
			}

			throw (Error)throwable;
		} finally {
			if (nodeOpen) {
				this.lexicalBuffer.endNode(ignoreMissingReferences, true);
			}
		}

		return returnedNode;
	}

	private boolean complexFieldSpecifierLookahead() {
		if (this.matchesComplexFieldSpecifierPrefix()) {
			return true;
		} else {
			ZKMScriptToken savedScanPos = this.scanPos;
			if (this.scanToken(31)) {
				this.scanPos = savedScanPos;
				if (this.scanToken(35)) {
					this.scanPos = savedScanPos;
					if (this.scanToken(34)) {
						this.scanPos = savedScanPos;
						if (this.scanToken(29)) {
							this.scanPos = savedScanPos;
							if (this.scanToken(38)) {
								this.scanPos = savedScanPos;
								if (this.scanToken(19)) {
									return true;
								}
							}
						}
					}
				}
			}

			return false;
		}
	}

	public final void parseAutoReflectionHash() {
		AutoReflectionHash autoReflectionHash2 = new AutoReflectionHash(136);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(autoReflectionHash2);

		try {
			try {
				ZKMScriptToken keywordToken = this.consumeParameter(134);
				this.consumeParameter(15);
				this.parseStringLiteral();
				this.lexicalBuffer.endNode(autoReflectionHash2, true);
				nodeOpen = false;
				autoReflectionHash2.setParameterName(keywordToken.image);
			} catch (Throwable throwable) {
				if (nodeOpen) {
					this.lexicalBuffer.discardNode(autoReflectionHash2);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			}
		} catch (Throwable throwable2) {
			if (nodeOpen) {
				this.lexicalBuffer.endNode(autoReflectionHash2, true);
			}

			throw throwable2;
		}
	}

	private boolean annotationValueOrElementLookahead() {
		return this.scanToken(34) ? true : this.memberComplexAnnotationSpecifierLookahead();
	}

	public final AbstractScriptNode parseDefaultMethodParameterChangesExclude() {
		DefaultMethodParameterChangesExcludeStatement defaultMethodParameterChangesExclude = new DefaultMethodParameterChangesExcludeStatement(48);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(defaultMethodParameterChangesExclude);

		try {
			ZKMScriptToken keywordToken = this.consumeParameter(193);
			this.statementKind = 193;
			this.parseRenameFilterParameter();

			while (true) {
				switch (this.nextToken.kind) {
					case 38:
						this.consumeParameter(38);
						this.parseRenameFilterParameter();
						break;
					default:
						this.la1Generation[62] = this.generationCounter;
						this.consumeParameter(19);
						this.lexicalBuffer.endNode(defaultMethodParameterChangesExclude, true);
						nodeOpen = false;
						defaultMethodParameterChangesExclude.setLineNumber(keywordToken.beginLine);
						return defaultMethodParameterChangesExclude;
				}
			}
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (nodeOpen) {
					this.lexicalBuffer.discardNode(defaultMethodParameterChangesExclude);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable throwable2) {
				if (nodeOpen) {
					this.lexicalBuffer.endNode(defaultMethodParameterChangesExclude, true);
				}

				throw throwable2;
			}
		}
	}

	private boolean jarQualifierLookahead() {
		return this.jarQualifierNextIsStringLiteralOrBang();
	}

	public final AbstractScriptNode parseScript() {
		ScriptInputNode scriptReader = new ScriptInputNode(0);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(scriptReader);

		try {
			switch (this.nextToken.kind) {
				case 14:
					this.consumeParameter(14);
					break;
				default:
					this.la1Generation[0] = this.generationCounter;
			}

			while (true) {
				switch (this.nextToken.kind) {
					case 36:
					case 39:
					case 45:
					case 46:
					case 64:
					case 65:
					case 69:
					case 79:
					case 80:
					case 81:
					case 86:
					case 94:
					case 98:
					case 100:
					case 107:
					case 111:
					case 126:
					case 127:
					case 138:
					case 144:
					case 146:
					case 152:
					case 154:
					case 158:
					case 159:
					case 161:
					case 162:
					case 164:
					case 165:
					case 166:
					case 167:
					case 169:
					case 170:
					case 171:
					case 173:
					case 174:
					case 180:
					case 181:
					case 182:
					case 186:
					case 187:
					case 189:
					case 190:
					case 191:
					case 192:
					case 193:
					case 195:
					case 197:
					case 198:
					case 199:
					case 200:
					case 203:
					case 204:
					case 206:
					case 207:
						this.parseBase();
						break;
					case 37:
					case 38:
					case 40:
					case 41:
					case 42:
					case 43:
					case 44:
					case 47:
					case 48:
					case 49:
					case 50:
					case 51:
					case 52:
					case 53:
					case 54:
					case 55:
					case 56:
					case 57:
					case 58:
					case 59:
					case 60:
					case 61:
					case 62:
					case 63:
					case 66:
					case 67:
					case 68:
					case 70:
					case 71:
					case 72:
					case 73:
					case 74:
					case 75:
					case 76:
					case 77:
					case 78:
					case 82:
					case 83:
					case 84:
					case 85:
					case 87:
					case 88:
					case 89:
					case 90:
					case 91:
					case 92:
					case 93:
					case 95:
					case 96:
					case 97:
					case 99:
					case 101:
					case 102:
					case 103:
					case 104:
					case 105:
					case 106:
					case 108:
					case 109:
					case 110:
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
					case 125:
					case 128:
					case 129:
					case 130:
					case 131:
					case 132:
					case 133:
					case 134:
					case 135:
					case 136:
					case 137:
					case 139:
					case 140:
					case 141:
					case 142:
					case 143:
					case 145:
					case 147:
					case 148:
					case 149:
					case 150:
					case 151:
					case 153:
					case 155:
					case 156:
					case 157:
					case 160:
					case 163:
					case 168:
					case 172:
					case 175:
					case 176:
					case 177:
					case 178:
					case 179:
					case 183:
					case 184:
					case 185:
					case 188:
					case 194:
					case 196:
					case 201:
					case 202:
					case 205:
					default:
						this.la1Generation[1] = this.generationCounter;
						this.consumeParameter(0);
						this.lexicalBuffer.endNode(scriptReader, true);
						nodeOpen = false;
						return scriptReader;
				}
			}
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (nodeOpen) {
					this.lexicalBuffer.discardNode(scriptReader);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable throwable2) {
				if (nodeOpen) {
					this.lexicalBuffer.endNode(scriptReader, true);
				}

				throw throwable2;
			}
		}
	}

	public final void parseMemberModifier() {
		MemberModifier memberModifier = new MemberModifier(142);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(memberModifier);

		try {
			this.parseMemberModifierHelper();
			this.lexicalBuffer.endNode(memberModifier, true);
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (true) {
					this.lexicalBuffer.discardNode(memberModifier);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}
				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable throwable2) {
				if (nodeOpen) {
					this.lexicalBuffer.endNode(memberModifier, true);
				}

				throw throwable2;
			}
		}
	}

	public final AbstractScriptNode integerEncryptionUnexclude() {
		IntegerEncryptionUnexcludeStatement integerEncryptionUnexclude = new IntegerEncryptionUnexcludeStatement(23);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(integerEncryptionUnexclude);

		try {
			ZKMScriptToken keywordToken = this.consumeParameter(174);
			this.statementKind = 174;
			this.parseRenameFilterParameter();

			while (true) {
				switch (this.nextToken.kind) {
					case 38:
						this.consumeParameter(38);
						this.parseRenameFilterParameter();
						break;
					default:
						this.la1Generation[25] = this.generationCounter;
						this.consumeParameter(19);
						this.lexicalBuffer.endNode(integerEncryptionUnexclude, true);
						nodeOpen = false;
						integerEncryptionUnexclude.setLineNumber(keywordToken.beginLine);
						return integerEncryptionUnexclude;
				}
			}
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (nodeOpen) {
					this.lexicalBuffer.discardNode(integerEncryptionUnexclude);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable throwable2) {
				if (nodeOpen) {
					this.lexicalBuffer.endNode(integerEncryptionUnexclude, true);
				}

				throw throwable2;
			}
		}
	}

	private boolean methodNameFilterAndChainLookahead() {
		if (this.matchesMethodSignatureStart()) {
			return true;
		} else {
			ZKMScriptToken savedScanPos;
			do {
				savedScanPos = this.scanPos;
			} while (!this.nextIsAndAndInMethodSignature());

			this.scanPos = savedScanPos;
			return false;
		}
	}

	public final void parseBracketedPackageSpecifier() {
		BracketedPackageSpecifier bracketedPackageSpecifier = new BracketedPackageSpecifier(186);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(bracketedPackageSpecifier);

		try {
			switch (this.nextToken.kind) {
				case 25:
					this.consumeParameter(25);
					bracketedPackageSpecifier.setNegated();
					break;
				default:
					this.la1Generation[146] = this.generationCounter;
			}

			this.consumeParameter(28);
			this.parseAndPackageSpecifier();

			while (true) {
				switch (this.nextToken.kind) {
					case 34:
						this.consumeParameter(34);
						this.parseAndPackageSpecifier();
						break;
					default:
						this.la1Generation[147] = this.generationCounter;
						this.consumeParameter(29);
						this.lexicalBuffer.endNode(bracketedPackageSpecifier, true);
						return;
				}
			}
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (true) {
					this.lexicalBuffer.discardNode(bracketedPackageSpecifier);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}
				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable throwable2) {
				if (nodeOpen) {
					this.lexicalBuffer.endNode(bracketedPackageSpecifier, true);
				}

				throw throwable2;
			}
		}
	}

	public final void parseMethodParameterChangesParameter() {
		MethodParameterChangesParameter methodParameterChanges2 = new MethodParameterChangesParameter(124);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(methodParameterChanges2);

		try {
			try {
				ZKMScriptToken keywordToken = this.consumeParameter(155);
				this.consumeParameter(15);
				this.parseMethodParameterChangesType();
				this.lexicalBuffer.endNode(methodParameterChanges2, true);
				nodeOpen = false;
				methodParameterChanges2.setParameterName(keywordToken.image);
			} catch (Throwable throwable) {
				if (nodeOpen) {
					this.lexicalBuffer.discardNode(methodParameterChanges2);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			}
		} catch (Throwable throwable2) {
			if (nodeOpen) {
				this.lexicalBuffer.endNode(methodParameterChanges2, true);
			}

			throw throwable2;
		}
	}

	private boolean methodNamePrintKeywordLookahead() {
		return this.scanToken(46);
	}

	public final AbstractScriptNode parseDefaultExcludeInput() {
		DefaultExcludeInput defaultExcludeInput = new DefaultExcludeInput(43);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(defaultExcludeInput);

		DefaultExcludeInput returnedNode;
		try {
			switch (this.nextToken.kind) {
				case 14:
					this.consumeParameter(14);
					break;
				default:
					this.la1Generation[54] = this.generationCounter;
			}

			switch (this.nextToken.kind) {
				case 64:
					this.parseDefaultExclude();
					break;
				default:
					this.la1Generation[55] = this.generationCounter;
			}

			this.consumeParameter(0);
			this.lexicalBuffer.endNode(defaultExcludeInput, true);
			nodeOpen = false;
			returnedNode = defaultExcludeInput;
		} catch (Throwable throwable) {
			if (nodeOpen) {
				this.lexicalBuffer.discardNode(defaultExcludeInput);
				nodeOpen = false;
			} else {
				this.lexicalBuffer.pop();
			}

			if (throwable instanceof RuntimeException) {
				throw (RuntimeException)throwable;
			}

			if (throwable instanceof ParsingException) {
				throw (ParsingException)throwable;
			}

			throw (Error)throwable;
		} finally {
			if (nodeOpen) {
				this.lexicalBuffer.endNode(defaultExcludeInput, true);
			}
		}

		return returnedNode;
	}

	private void saveLookaheadState(int specIndex, int lookaheadDepth) {
		LookaheadRecord lookaheadRecord;
		for (lookaheadRecord = this.lookaheadRecords[specIndex]; lookaheadRecord.generation > this.generationCounter; lookaheadRecord = lookaheadRecord.next) {
			if (lookaheadRecord.next == null) {
				lookaheadRecord = lookaheadRecord.next = new LookaheadRecord();
				break;
			}
		}

		lookaheadRecord.generation = this.generationCounter + lookaheadDepth - this.lookaheadRemaining;
		lookaheadRecord.first = this.token;
		lookaheadRecord.lookaheadCount = lookaheadDepth;
	}

	private void addErrorToken(int kind, int tokenPos) {
		if (tokenPos < 100) {
			if (tokenPos != this.errorTokenEndPos + 1) {
				if (this.errorTokenEndPos != 0) {
					this.errorTokenSequence = new int[this.errorTokenEndPos];

					for (int i = 0; i < this.errorTokenEndPos; i++) {
						this.errorTokenSequence[i] = this.errorTokenKinds[i];
					}

					label43:
					for (int[] expectedSequence : this.expectedTokenSequences) {
						if (expectedSequence.length == this.errorTokenSequence.length) {
							for (int tokenIndex = 0; tokenIndex < this.errorTokenSequence.length; tokenIndex++) {
								if (expectedSequence[tokenIndex] != this.errorTokenSequence[tokenIndex]) {
									continue label43;
								}
							}

							this.expectedTokenSequences.add(this.errorTokenSequence);
							break;
						}
					}

					if (tokenPos != 0) {
						this.errorTokenEndPos = tokenPos;
						this.errorTokenKinds[this.errorTokenEndPos - 1] = kind;
					}
				}
			} else {
				this.errorTokenKinds[this.errorTokenEndPos++] = kind;
			}
		}
	}

	public final void parseLegalIdsParameter() {
		LegalIdsParameter legalIdsParameter = new LegalIdsParameter(125);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(legalIdsParameter);

		try {
			try {
				ZKMScriptToken keywordToken = this.consumeParameter(116);
				this.consumeParameter(15);
				this.parseBoolean();
				this.lexicalBuffer.endNode(legalIdsParameter, true);
				nodeOpen = false;
				legalIdsParameter.setParameterName(keywordToken.image);
			} catch (Throwable throwable) {
				if (nodeOpen) {
					this.lexicalBuffer.discardNode(legalIdsParameter);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			}
		} catch (Throwable throwable2) {
			if (nodeOpen) {
				this.lexicalBuffer.endNode(legalIdsParameter, true);
			}

			throw throwable2;
		}
	}

	private boolean packageComponentScrambleKeywordLookahead() {
		return this.scanToken(71) ? true : this.scanToken(21);
	}

	public final AbstractScriptNode trimExclude() {
		TrimExcludeStatement trimExclude = new TrimExcludeStatement(29);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(trimExclude);

		try {
			ZKMScriptToken keywordToken = this.consumeParameter(94);
			this.statementKind = 94;
			this.parseRenameFilterParameter();

			while (true) {
				switch (this.nextToken.kind) {
					case 38:
						this.consumeParameter(38);
						this.parseRenameFilterParameter();
						break;
					default:
						this.la1Generation[32] = this.generationCounter;
						this.consumeParameter(19);
						this.lexicalBuffer.endNode(trimExclude, true);
						nodeOpen = false;
						trimExclude.setLineNumber(keywordToken.beginLine);
						return trimExclude;
				}
			}
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (nodeOpen) {
					this.lexicalBuffer.discardNode(trimExclude);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable throwable2) {
				if (nodeOpen) {
					this.lexicalBuffer.endNode(trimExclude, true);
				}

				throw throwable2;
			}
		}
	}

	public ParsingException generateParseException() {
		this.expectedTokenSequences.clear();
		boolean[] legalTokenKinds = new boolean[218];
		if (this.errorTokenKind >= 0) {
			legalTokenKinds[this.errorTokenKind] = true;
			this.errorTokenKind = -1;
		}

		for (int stateIndex = 0; stateIndex < 187; stateIndex++) {
			if (this.la1Generation[stateIndex] == this.generationCounter) {
				for (int bitIndex = 0; bitIndex < 32; bitIndex++) {
					if ((la1TokenBits0[stateIndex] & 1 << bitIndex) != 0) {
						legalTokenKinds[bitIndex] = true;
					}

					if ((la1TokenBits1[stateIndex] & 1 << bitIndex) != 0) {
						legalTokenKinds[32 + bitIndex] = true;
					}

					if ((ScriptParser.la1TokenBits2[stateIndex] & 1 << bitIndex) != 0) {
						legalTokenKinds[64 + bitIndex] = true;
					}

					if ((la1TokenBits3[stateIndex] & 1 << bitIndex) != 0) {
						legalTokenKinds[96 + bitIndex] = true;
					}

					if ((la1TokenBits4[stateIndex] & 1 << bitIndex) != 0) {
						legalTokenKinds[128 + bitIndex] = true;
					}

					if ((la1TokenBits5[stateIndex] & 1 << bitIndex) != 0) {
						legalTokenKinds[160 + bitIndex] = true;
					}

					if ((la1TokenBits6[stateIndex] & 1 << bitIndex) != 0) {
						legalTokenKinds[192 + bitIndex] = true;
					}
				}
			}
		}

		for (int tokenKind = 0; tokenKind < 218; tokenKind++) {
			if (legalTokenKinds[tokenKind]) {
				this.errorTokenSequence = new int[1];
				this.errorTokenSequence[0] = tokenKind;
				this.expectedTokenSequences.add(this.errorTokenSequence);
			}
		}

		this.errorTokenEndPos = 0;
		this.rescanToken();
		this.addErrorToken(0, 0);
		int[][] expectedTokenSequenceArray = new int[this.expectedTokenSequences.size()][];

		for (int sequenceIndex = 0; sequenceIndex < this.expectedTokenSequences.size(); sequenceIndex++) {
			expectedTokenSequenceArray[sequenceIndex] = this.expectedTokenSequences.get(sequenceIndex);
		}

		return new ParsingException(this.token, expectedTokenSequenceArray, ZKMScriptTokenList.tokenImage, this.statementKind);
	}

	public final AbstractScriptNode obfuscateFlowUnexclude() {
		ObfuscateFlowUnexcludeStatement obfuscateFlowUnexclude = new ObfuscateFlowUnexcludeStatement(17);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(obfuscateFlowUnexclude);

		try {
			ZKMScriptToken keywordToken = this.consumeParameter(154);
			this.statementKind = 154;
			this.parseRenameFilterParameter();

			while (true) {
				switch (this.nextToken.kind) {
					case 38:
						this.consumeParameter(38);
						this.parseRenameFilterParameter();
						break;
					default:
						this.la1Generation[19] = this.generationCounter;
						this.consumeParameter(19);
						this.lexicalBuffer.endNode(obfuscateFlowUnexclude, true);
						nodeOpen = false;
						obfuscateFlowUnexclude.setLineNumber(keywordToken.beginLine);
						return obfuscateFlowUnexclude;
				}
			}
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (nodeOpen) {
					this.lexicalBuffer.discardNode(obfuscateFlowUnexclude);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable throwable2) {
				if (nodeOpen) {
					this.lexicalBuffer.endNode(obfuscateFlowUnexclude, true);
				}

				throw throwable2;
			}
		}
	}

	private boolean annotationNameKeepKeywordLookahead() {
		return this.scanToken(42);
	}

	public final void parseBracketedFieldSpecifier() {
		BracketedFieldSpecifier bracketedFieldSpecifier = new BracketedFieldSpecifier(202);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(bracketedFieldSpecifier);

		try {
			switch (this.nextToken.kind) {
				case 25:
					this.consumeParameter(25);
					bracketedFieldSpecifier.setNegated();
					break;
				default:
					this.la1Generation[162] = this.generationCounter;
			}

			this.consumeParameter(28);
			this.parseAndFieldSpecifier();

			while (true) {
				switch (this.nextToken.kind) {
					case 34:
						this.consumeParameter(34);
						this.parseAndFieldSpecifier();
						break;
					default:
						this.la1Generation[163] = this.generationCounter;
						this.consumeParameter(29);
						this.lexicalBuffer.endNode(bracketedFieldSpecifier, true);
						return;
				}
			}
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (true) {
					this.lexicalBuffer.discardNode(bracketedFieldSpecifier);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}
				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable throwable2) {
				if (nodeOpen) {
					this.lexicalBuffer.endNode(bracketedFieldSpecifier, true);
				}

				throw throwable2;
			}
		}
	}

	public final AbstractScriptNode longEncryptionUnexclude() {
		LongEncryptionUnexcludeStatement longEncryptionUnexclude = new LongEncryptionUnexcludeStatement(25);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(longEncryptionUnexclude);

		try {
			ZKMScriptToken keywordToken = this.consumeParameter(159);
			this.statementKind = 159;
			this.parseRenameFilterParameter();

			while (true) {
				switch (this.nextToken.kind) {
					case 38:
						this.consumeParameter(38);
						this.parseRenameFilterParameter();
						break;
					default:
						this.la1Generation[27] = this.generationCounter;
						this.consumeParameter(19);
						this.lexicalBuffer.endNode(longEncryptionUnexclude, true);
						nodeOpen = false;
						longEncryptionUnexclude.setLineNumber(keywordToken.beginLine);
						return longEncryptionUnexclude;
				}
			}
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (nodeOpen) {
					this.lexicalBuffer.discardNode(longEncryptionUnexclude);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable throwable2) {
				if (nodeOpen) {
					this.lexicalBuffer.endNode(longEncryptionUnexclude, true);
				}

				throw throwable2;
			}
		}
	}

	private boolean methodNameBridgeKeywordLookahead() {
		return this.scanToken(52);
	}

	public final void parseDeriveGroupingsFromChangeLogParameter() {
		DeriveGroupingsFromChangeLogParameter deriveGroupingsFromInputChangeLog2 = new DeriveGroupingsFromChangeLogParameter(110);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(deriveGroupingsFromInputChangeLog2);

		try {
			try {
				ZKMScriptToken keywordToken = this.consumeParameter(201);
				this.consumeParameter(15);
				this.parseBoolean();
				this.lexicalBuffer.endNode(deriveGroupingsFromInputChangeLog2, true);
				nodeOpen = false;
				deriveGroupingsFromInputChangeLog2.setParameterName(keywordToken.image);
			} catch (Throwable throwable) {
				if (nodeOpen) {
					this.lexicalBuffer.discardNode(deriveGroupingsFromInputChangeLog2);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			}
		} catch (Throwable throwable2) {
			if (nodeOpen) {
				this.lexicalBuffer.endNode(deriveGroupingsFromInputChangeLog2, true);
			}

			throw throwable2;
		}
	}

	public final AbstractScriptNode parseDefaultTrimExcludeInput() {
		DefaultTrimExcludeInput defaultTrimExcludeInput = new DefaultTrimExcludeInput(45);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(defaultTrimExcludeInput);

		try {
			switch (this.nextToken.kind) {
				case 14:
					this.consumeParameter(14);
					break;
				default:
					this.la1Generation[57] = this.generationCounter;
			}

			switch (this.nextToken.kind) {
				case 94:
					this.parseDefaultTrimExclude();
					break;
				default:
					this.la1Generation[58] = this.generationCounter;
			}

			this.consumeParameter(0);
			this.lexicalBuffer.endNode(defaultTrimExcludeInput, true);
			nodeOpen = false;
			return defaultTrimExcludeInput;
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (nodeOpen) {
					this.lexicalBuffer.discardNode(defaultTrimExcludeInput);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable throwable2) {
				if (nodeOpen) {
					this.lexicalBuffer.endNode(defaultTrimExcludeInput, true);
				}

				throw throwable2;
			}
		}
	}

	private boolean moduleNameComponentWildcardLookahead() {
		return this.scanToken(23);
	}

	public final void parseResetStringEncryptionExclusions() {
		ResetStringEncryptionExclusionsStatement resetEncryptStringLiteralExclusions = new ResetStringEncryptionExclusionsStatement(70);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(resetEncryptStringLiteralExclusions);

		try {
			ZKMScriptToken keywordToken = this.consumeParameter(197);
			this.statementKind = 197;
			this.consumeParameter(19);
			this.lexicalBuffer.endNode(resetEncryptStringLiteralExclusions, true);
			nodeOpen = false;
			resetEncryptStringLiteralExclusions.setLineNumber(keywordToken.beginLine);
		} catch (Throwable throwable) {
			if (nodeOpen) {
				this.lexicalBuffer.endNode(resetEncryptStringLiteralExclusions, true);
			}

			throw throwable;
		}
	}

	public final void parseResetObfuscateFlowExclusions() {
		ResetObfuscateFlowExclusionsStatement resetObfuscateFlowExclusions = new ResetObfuscateFlowExclusionsStatement(68);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(resetObfuscateFlowExclusions);

		try {
			ZKMScriptToken keywordToken = this.consumeParameter(189);
			this.statementKind = 189;
			this.consumeParameter(19);
			this.lexicalBuffer.endNode(resetObfuscateFlowExclusions, true);
			nodeOpen = false;
			resetObfuscateFlowExclusions.setLineNumber(keywordToken.beginLine);
		} catch (Throwable throwable) {
			if (nodeOpen) {
				this.lexicalBuffer.endNode(resetObfuscateFlowExclusions, true);
			}

			throw throwable;
		}
	}

	private boolean fieldNameFilterOrElementLookahead() {
		return this.scanToken(34) ? true : this.complexFieldSpecifierAndChainLookahead();
	}

	public final AbstractScriptNode trimUnexclude() {
		TrimUnexcludeStatement trimUnexclude = new TrimUnexcludeStatement(30);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(trimUnexclude);

		try {
			ZKMScriptToken keywordToken = this.consumeParameter(100);
			this.statementKind = 100;
			this.parseRenameFilterParameter();

			while (true) {
				switch (this.nextToken.kind) {
					case 38:
						this.consumeParameter(38);
						this.parseRenameFilterParameter();
						break;
					default:
						this.la1Generation[33] = this.generationCounter;
						this.consumeParameter(19);
						this.lexicalBuffer.endNode(trimUnexclude, true);
						nodeOpen = false;
						trimUnexclude.setLineNumber(keywordToken.beginLine);
						return trimUnexclude;
				}
			}
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (nodeOpen) {
					this.lexicalBuffer.discardNode(trimUnexclude);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable throwable2) {
				if (nodeOpen) {
					this.lexicalBuffer.endNode(trimUnexclude, true);
				}

				throw throwable2;
			}
		}
	}

	public final void parseHideStaticMethodNamesParameter() {
		HideStaticMethodNamesParameter hideStaticMethodNamesParameter = new HideStaticMethodNamesParameter(130);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(hideStaticMethodNamesParameter);

		try {
			try {
				ZKMScriptToken keywordToken = this.consumeParameter(149);
				this.consumeParameter(15);
				this.parseBoolean();
				this.lexicalBuffer.endNode(hideStaticMethodNamesParameter, true);
				nodeOpen = false;
				hideStaticMethodNamesParameter.setParameterName(keywordToken.image);
			} catch (Throwable throwable) {
				if (nodeOpen) {
					this.lexicalBuffer.discardNode(hideStaticMethodNamesParameter);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			}
		} catch (Throwable throwable2) {
			if (nodeOpen) {
				this.lexicalBuffer.endNode(hideStaticMethodNamesParameter, true);
			}

			throw throwable2;
		}
	}

	private boolean packageComponentBridgeKeywordLookahead() {
		return this.scanToken(52) ? true : this.scanToken(21);
	}

	public final AbstractScriptNode encryptStringLiteralsUnexclude() {
		StringEncryptionUnexcludeStatement encryptStringLiteralsUnexclude = new StringEncryptionUnexcludeStatement(21);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(encryptStringLiteralsUnexclude);

		try {
			ZKMScriptToken keywordToken = this.consumeParameter(170);
			this.statementKind = 170;
			this.parseRenameFilterParameter();

			while (true) {
				switch (this.nextToken.kind) {
					case 38:
						this.consumeParameter(38);
						this.parseRenameFilterParameter();
						break;
					default:
						this.la1Generation[23] = this.generationCounter;
						this.consumeParameter(19);
						this.lexicalBuffer.endNode(encryptStringLiteralsUnexclude, true);
						nodeOpen = false;
						encryptStringLiteralsUnexclude.setLineNumber(keywordToken.beginLine);
						return encryptStringLiteralsUnexclude;
				}
			}
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (nodeOpen) {
					this.lexicalBuffer.discardNode(encryptStringLiteralsUnexclude);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable throwable2) {
				if (nodeOpen) {
					this.lexicalBuffer.endNode(encryptStringLiteralsUnexclude, true);
				}

				throw throwable2;
			}
		}
	}

	public final AbstractScriptNode parseMethodParameterChangesExclude() {
		MethodParameterChangesExcludeStatement methodParameterChangesExclude = new MethodParameterChangesExcludeStatement(38);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(methodParameterChangesExclude);

		try {
			ZKMScriptToken keywordToken = this.consumeParameter(193);
			this.statementKind = 193;
			this.parseRenameFilterParameter();

			while (true) {
				switch (this.nextToken.kind) {
					case 38:
						this.consumeParameter(38);
						this.parseRenameFilterParameter();
						break;
					default:
						this.la1Generation[49] = this.generationCounter;
						this.consumeParameter(19);
						this.lexicalBuffer.endNode(methodParameterChangesExclude, true);
						nodeOpen = false;
						methodParameterChangesExclude.setLineNumber(keywordToken.beginLine);
						return methodParameterChangesExclude;
				}
			}
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (nodeOpen) {
					this.lexicalBuffer.discardNode(methodParameterChangesExclude);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable throwable2) {
				if (nodeOpen) {
					this.lexicalBuffer.endNode(methodParameterChangesExclude, true);
				}

				throw throwable2;
			}
		}
	}

	private boolean literalPackageComponentLookahead() {
		return this.packageNameComponentLookahead();
	}

	public final void parseEncryptLongConstantsParameter() {
		EncryptLongConstantsParameter encryptLongConstantsParameter = new EncryptLongConstantsParameter(104);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(encryptLongConstantsParameter);

		try {
			try {
				ZKMScriptToken keywordToken = this.consumeParameter(148);
				this.consumeParameter(15);
				this.parseLongEncryptionType();
				this.lexicalBuffer.endNode(encryptLongConstantsParameter, true);
				nodeOpen = false;
				encryptLongConstantsParameter.setParameterName(keywordToken.image);
			} catch (Throwable throwable) {
				if (nodeOpen) {
					this.lexicalBuffer.discardNode(encryptLongConstantsParameter);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			}
		} catch (Throwable throwable2) {
			if (nodeOpen) {
				this.lexicalBuffer.endNode(encryptLongConstantsParameter, true);
			}

			throw throwable2;
		}
	}

	public final void parseResetGroupings() {
		ResetGroupingsStatement resetGroupings = new ResetGroupingsStatement(75);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(resetGroupings);

		try {
			ZKMScriptToken keywordToken = this.consumeParameter(107);
			this.statementKind = 107;
			this.consumeParameter(19);
			this.lexicalBuffer.endNode(resetGroupings, true);
			nodeOpen = false;
			resetGroupings.setLineNumber(keywordToken.beginLine);
		} catch (Throwable throwable) {
			if (nodeOpen) {
				this.lexicalBuffer.endNode(resetGroupings, true);
			}

			throw throwable;
		}
	}

	public final void parseArrayLevel() {
		ArrayLevel arrayLevel = new ArrayLevel(220);
		this.lexicalBuffer.beginNode(arrayLevel);

		try {
			this.consumeParameter(17);
			this.consumeParameter(18);
		} finally {
			this.lexicalBuffer.endNode(arrayLevel, true);
		}
	}

	public final void parsePreverifyParameter() {
		PreverifyParameter preverify2 = new PreverifyParameter(117);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(preverify2);

		try {
			try {
				ZKMScriptToken keywordToken = this.consumeParameter(78);
				this.consumeParameter(15);
				this.parseBoolean();
				this.lexicalBuffer.endNode(preverify2, true);
				nodeOpen = false;
				preverify2.setParameterName(keywordToken.image);
			} catch (Throwable throwable) {
				if (nodeOpen) {
					this.lexicalBuffer.discardNode(preverify2);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			}
		} catch (Throwable throwable2) {
			if (nodeOpen) {
				this.lexicalBuffer.endNode(preverify2, true);
			}

			throw throwable2;
		}
	}

	public final void parseBase() {
		NTStatement ntStatement = new NTStatement(1);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(ntStatement);

		try {
			switch (this.nextToken.kind) {
				case 36:
					this.parseGarbageCollect();
					break;
				case 37:
				case 38:
				case 40:
				case 41:
				case 42:
				case 43:
				case 44:
				case 47:
				case 48:
				case 49:
				case 50:
				case 51:
				case 52:
				case 53:
				case 54:
				case 55:
				case 56:
				case 57:
				case 58:
				case 59:
				case 60:
				case 61:
				case 62:
				case 63:
				case 66:
				case 67:
				case 68:
				case 70:
				case 71:
				case 72:
				case 73:
				case 74:
				case 75:
				case 76:
				case 77:
				case 78:
				case 82:
				case 83:
				case 84:
				case 85:
				case 87:
				case 88:
				case 89:
				case 90:
				case 91:
				case 92:
				case 93:
				case 95:
				case 96:
				case 97:
				case 99:
				case 101:
				case 102:
				case 103:
				case 104:
				case 105:
				case 106:
				case 108:
				case 109:
				case 110:
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
				case 125:
				case 128:
				case 129:
				case 130:
				case 131:
				case 132:
				case 133:
				case 134:
				case 135:
				case 136:
				case 137:
				case 139:
				case 140:
				case 141:
				case 142:
				case 143:
				case 145:
				case 147:
				case 148:
				case 149:
				case 150:
				case 151:
				case 153:
				case 155:
				case 156:
				case 157:
				case 160:
				case 163:
				case 168:
				case 172:
				case 175:
				case 176:
				case 177:
				case 178:
				case 179:
				case 183:
				case 184:
				case 185:
				case 188:
				case 194:
				case 196:
				case 201:
				case 202:
				case 205:
				default:
					this.la1Generation[2] = this.generationCounter;
					this.consumeParameter(-1);
					throw new ParsingException();
				case 39:
					this.open();
					break;
				case 45:
					this.parseTrim();
					break;
				case 46:
					this.parsePrint();
					break;
				case 64:
					this.exclude();
					break;
				case 65:
					this.parseSaveAll();
					break;
				case 69:
					this.parseExecute();
					break;
				case 79:
					this.unexclude();
					break;
				case 80:
					this.parseClasspath();
					break;
				case 81:
					this.parseObfuscate();
					break;
				case 86:
					this.groupings();
					break;
				case 94:
					this.trimExclude();
					break;
				case 98:
					this.fixedClasses();
					break;
				case 100:
					this.trimUnexclude();
					break;
				case 107:
					this.parseResetGroupings();
					break;
				case 111:
					this.parseResetExclusions();
					break;
				case 126:
					this.parseResetFixedClasses();
					break;
				case 127:
					this.removeMethodCalls();
					break;
				case 138:
					this.parseResetTrimExclusions();
					break;
				case 144:
					this.obfuscateFlowExclude();
					break;
				case 146:
					this.parseAccessedByReflection();
					break;
				case 152:
					this.longEncryptionExclude();
					break;
				case 154:
					this.obfuscateFlowUnexclude();
					break;
				case 158:
					this.parseResetRemoveMethodCalls();
					break;
				case 159:
					this.longEncryptionUnexclude();
					break;
				case 161:
					this.ignoreMissingReferences();
					break;
				case 162:
					this.encryptStringLiteralsExclude();
					break;
				case 164:
					this.integerEncryptionExclude();
					break;
				case 165:
					this.removeMethodCallsInclude();
					break;
				case 166:
					this.removeMethodCallsExclude();
					break;
				case 167:
					this.parseClassInitializationOrder();
					break;
				case 169:
					this.existingSerializedClasses();
					break;
				case 170:
					this.encryptStringLiteralsUnexclude();
					break;
				case 171:
					this.parseResetAccessedByReflection();
					break;
				case 173:
					this.obfuscateExceptionsExclude();
					break;
				case 174:
					this.integerEncryptionUnexclude();
					break;
				case 180:
					this.obfuscateReferencesInclude();
					break;
				case 181:
					this.obfuscateReferencesExclude();
					break;
				case 182:
					this.parseAccessedByReflectionExclude();
					break;
				case 186:
					this.obfuscateExceptionsUnexclude();
					break;
				case 187:
					this.parseResetIgnoreMissingReferences();
					break;
				case 189:
					this.parseResetObfuscateFlowExclusions();
					break;
				case 190:
					this.parseResetLongEncryptionExclusions();
					break;
				case 191:
					this.parseResetClassInitializationOrder();
					break;
				case 192:
					this.parseMethodParameterChangesInclude();
					break;
				case 193:
					this.parseMethodParameterChangesExclude();
					break;
				case 195:
					this.parseResetExistingSerializedClasses();
					break;
				case 197:
					this.parseResetStringEncryptionExclusions();
					break;
				case 198:
					this.parseResetIntegerEncryptionExclusions();
					break;
				case 199:
					this.parseMethodParameterObfuscationInclude();
					break;
				case 200:
					this.parseMethodParameterObfuscationExclude();
					break;
				case 203:
					this.parseResetObfuscateReferenceExclusions();
					break;
				case 204:
					this.parseResetObfuscateExceptionsExclusions();
					break;
				case 206:
					this.parseResetMethodParameterChangesExclusions();
					break;
				case 207:
					this.parseResetMethodParameterObfuscationExclusions();
			}

			this.lexicalBuffer.endNode(ntStatement, true);
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (true) {
					this.lexicalBuffer.discardNode(ntStatement);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}
				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable throwable2) {
				if (nodeOpen) {
					this.lexicalBuffer.endNode(ntStatement, true);
				}

				throw throwable2;
			}
		}
	}

	public final void parseFileFilter() {
		FileFilter fileFilter = new FileFilter(7);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(fileFilter);

		try {
			this.consumeParameter(30);
			switch (this.nextToken.kind) {
				case 25:
					this.parseNegatedFileFilterComponent();
					break;
				case 210:
					this.parseFileFilterComponent();
					break;
				default:
					this.la1Generation[11] = this.generationCounter;
					this.consumeParameter(-1);
					throw new ParsingException();
			}

			while (true) {
				switch (this.nextToken.kind) {
					case 34:
					case 35:
						switch (this.nextToken.kind) {
							case 34:
								this.parseOrFileFilterComponent();
								continue;
							case 35:
								this.parseAndFileFilterComponent();
								continue;
							default:
								this.la1Generation[13] = this.generationCounter;
								this.consumeParameter(-1);
								throw new ParsingException();
						}
					default:
						this.la1Generation[12] = this.generationCounter;
						this.consumeParameter(31);
						this.lexicalBuffer.endNode(fileFilter, true);
						return;
				}
			}
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (true) {
					this.lexicalBuffer.discardNode(fileFilter);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}
				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable throwable2) {
				if (nodeOpen) {
					this.lexicalBuffer.endNode(fileFilter, true);
				}

				throw throwable2;
			}
		}
	}

	public final void open() {
		LoadStatement open = new LoadStatement(2);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(open);

		try {
			try {
				ZKMScriptToken keywordToken = this.consumeParameter(39);
				this.statementKind = 39;
				switch (this.nextToken.kind) {
					case 130:
						this.parseOpenNestedArchivesParameter();
						break;
					default:
						this.la1Generation[3] = this.generationCounter;
				}

				this.parseStringLiteral();
				switch (this.nextToken.kind) {
					case 30:
						this.parseFileFilter();
						break;
					default:
						this.la1Generation[4] = this.generationCounter;
				}

				while (true) {
					switch (this.nextToken.kind) {
						case 141:
						case 153:
							this.parseLoadParameter();
							break;
						default:
							this.la1Generation[5] = this.generationCounter;

							label74:
							while (true) {
								switch (this.nextToken.kind) {
									case 16:
									case 24:
									case 210:
										label67:
										switch (this.nextToken.kind) {
											case 16:
												this.parseSkipArchivePath();
												continue;
											case 24:
												this.parseUnSkipArchivePath();
												continue;
											case 210:
												this.parseStringLiteral();
												switch (this.nextToken.kind) {
													case 30:
														this.parseFileFilter();
														break label67;
													default:
														this.la1Generation[7] = this.generationCounter;
														break label67;
												}
											default:
												this.la1Generation[9] = this.generationCounter;
												this.consumeParameter(-1);
												throw new ParsingException();
										}

										while (true) {
											switch (this.nextToken.kind) {
												case 141:
												case 153:
													this.parseLoadParameter();
													break;
												default:
													this.la1Generation[8] = this.generationCounter;
													continue label74;
											}
										}
									default:
										this.la1Generation[6] = this.generationCounter;
										this.consumeParameter(19);
										this.lexicalBuffer.endNode(open, true);
										nodeOpen = false;
										open.setLineNumber(keywordToken.beginLine);
										return;
								}
							}
					}
				}
			} catch (Throwable throwable) {
				if (nodeOpen) {
					this.lexicalBuffer.discardNode(open);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			}
		} catch (Throwable throwable2) {
			if (nodeOpen) {
				this.lexicalBuffer.endNode(open, true);
			}

			throw throwable2;
		}
	}

	public final void parseResetClassInitializationOrder() {
		ResetClassInitializationOrderStatement resetClassInitializationOrder = new ResetClassInitializationOrderStatement(85);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(resetClassInitializationOrder);

		try {
			ZKMScriptToken keywordToken = this.consumeParameter(191);
			this.statementKind = 191;
			this.consumeParameter(19);
			this.lexicalBuffer.endNode(resetClassInitializationOrder, true);
			nodeOpen = false;
			resetClassInitializationOrder.setLineNumber(keywordToken.beginLine);
		} catch (Throwable throwable) {
			if (nodeOpen) {
				this.lexicalBuffer.endNode(resetClassInitializationOrder, true);
			}

			throw throwable;
		}
	}

	public final void parseObfuscateReferenceStructuresType() {
		ObfuscateReferenceStructuresType obfuscateReferenceStructuresType = new ObfuscateReferenceStructuresType(161);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(obfuscateReferenceStructuresType);

		try {
			switch (this.nextToken.kind) {
				case 108:
					ZKMScriptToken inSpecialClassToken = this.consumeParameter(108);
					this.lexicalBuffer.endNode(obfuscateReferenceStructuresType, true);
					nodeOpen = false;
					obfuscateReferenceStructuresType.setText(inSpecialClassToken.image);
					return;
				case 142:
					ZKMScriptToken inReferencingClassesToken = this.consumeParameter(142);
					this.lexicalBuffer.endNode(obfuscateReferenceStructuresType, true);
					nodeOpen = false;
					obfuscateReferenceStructuresType.setText(inReferencingClassesToken.image);
					return;
				default:
					this.la1Generation[125] = this.generationCounter;
					this.consumeParameter(-1);
					throw new ParsingException();
			}
		} finally {
			if (nodeOpen) {
				this.lexicalBuffer.endNode(obfuscateReferenceStructuresType, true);
			}
		}
	}

	public final void parseMakeClassesPublicParameter() {
		MakeClassesPublicParameter makeClassesPublic2 = new MakeClassesPublicParameter(118);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(makeClassesPublic2);

		try {
			try {
				ZKMScriptToken keywordToken = this.consumeParameter(121);
				this.consumeParameter(15);
				this.parseBoolean();
				this.lexicalBuffer.endNode(makeClassesPublic2, true);
				nodeOpen = false;
				makeClassesPublic2.setParameterName(keywordToken.image);
			} catch (Throwable throwable) {
				if (nodeOpen) {
					this.lexicalBuffer.discardNode(makeClassesPublic2);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			}
		} catch (Throwable throwable2) {
			if (nodeOpen) {
				this.lexicalBuffer.endNode(makeClassesPublic2, true);
			}

			throw throwable2;
		}
	}

	public final void parseMethodArguments() {
		MethodArguments methodArguments = new MethodArguments(213);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(methodArguments);

		try {
			this.parseMethodParameter();

			while (true) {
				switch (this.nextToken.kind) {
					case 20:
						this.consumeParameter(20);
						this.parseMethodParameter();
						break;
					default:
						this.la1Generation[178] = this.generationCounter;
						this.lexicalBuffer.endNode(methodArguments, true);
						return;
				}
			}
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (true) {
					this.lexicalBuffer.discardNode(methodArguments);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}
				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable throwable2) {
				if (nodeOpen) {
					this.lexicalBuffer.endNode(methodArguments, true);
				}

				throw throwable2;
			}
		}
	}

	public final void parseClasspath() {
		ClasspathStatement classpath = new ClasspathStatement(53);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(classpath);

		try {
			try {
				ZKMScriptToken keywordToken = this.consumeParameter(80);
				this.statementKind = 80;

				while (true) {
					switch (this.nextToken.kind) {
						case 210:
							this.parseStringLiteral();
							break;
						case 212:
							this.parseIntegerLiteral();
							break;
						default:
							this.la1Generation[69] = this.generationCounter;
							this.consumeParameter(-1);
							throw new ParsingException();
					}

					switch (this.nextToken.kind) {
						case 210:
						case 212:
							break;
						default:
							this.la1Generation[70] = this.generationCounter;
							this.consumeParameter(19);
							this.lexicalBuffer.endNode(classpath, true);
							nodeOpen = false;
							classpath.setLineNumber(keywordToken.beginLine);
							return;
					}
				}
			} catch (Throwable throwable) {
				if (nodeOpen) {
					this.lexicalBuffer.discardNode(classpath);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			}
		} catch (Throwable throwable2) {
			if (nodeOpen) {
				this.lexicalBuffer.endNode(classpath, true);
			}

			throw throwable2;
		}
	}

	public final void parseArchiveCompressionType() {
		ArchiveCompressionType archiveCompressionType = new ArchiveCompressionType(60);
		boolean frameOpen = true;
		this.lexicalBuffer.beginNode(archiveCompressionType);

		try {
			switch (this.nextToken.kind) {
				case 37:
					ZKMScriptToken nameToken = this.consumeParameter(37);
					this.lexicalBuffer.endNode(archiveCompressionType, true);
					frameOpen = false;
					archiveCompressionType.setText(nameToken.image);
					return;
				case 43:
					ZKMScriptToken nameToken2 = this.consumeParameter(43);
					this.lexicalBuffer.endNode(archiveCompressionType, true);
					frameOpen = false;
					archiveCompressionType.setText(nameToken2.image);
					return;
				case 44:
					ZKMScriptToken nameToken3 = this.consumeParameter(44);
					this.lexicalBuffer.endNode(archiveCompressionType, true);
					frameOpen = false;
					archiveCompressionType.setText(nameToken3.image);
					return;
				default:
					this.la1Generation[76] = this.generationCounter;
					this.consumeParameter(-1);
					throw new ParsingException();
			}
		} finally {
			if (frameOpen) {
				this.lexicalBuffer.endNode(archiveCompressionType, true);
			}
		}
	}

	public final void parseChangeLogInParameter() {
		ChangeLogInParameter changeLogInParameter = new ChangeLogInParameter(95);
		boolean frameOpen = true;
		this.lexicalBuffer.beginNode(changeLogInParameter);

		try {
			switch (this.nextToken.kind) {
				case 110:
					ZKMScriptToken nameToken = this.consumeParameter(110);
					this.consumeParameter(15);
					this.parseStringLiteral();

					while (true) {
						switch (this.nextToken.kind) {
							case 20:
								this.consumeParameter(20);
								this.parseStringLiteral();
								break;
							default:
								this.la1Generation[100] = this.generationCounter;
								this.lexicalBuffer.endNode(changeLogInParameter, true);
								frameOpen = false;
								changeLogInParameter.setParameterName(nameToken.image);
								return;
						}
					}
				case 143:
					ZKMScriptToken nameToken2 = this.consumeParameter(143);
					this.consumeParameter(15);
					this.parseStringLiteral();

					while (true) {
						switch (this.nextToken.kind) {
							case 20:
								this.consumeParameter(20);
								this.parseStringLiteral();
								break;
							default:
								this.la1Generation[101] = this.generationCounter;
								this.lexicalBuffer.endNode(changeLogInParameter, true);
								frameOpen = false;
								changeLogInParameter.setParameterName(nameToken2.image);
								changeLogInParameter.markInReferencingClasses();
								return;
						}
					}
				default:
					this.la1Generation[102] = this.generationCounter;
					this.consumeParameter(-1);
					throw new ParsingException();
			}
		} catch (Throwable throwable) {
			if (frameOpen) {
				this.lexicalBuffer.discardNode(changeLogInParameter);
				frameOpen = false;
			} else {
				this.lexicalBuffer.pop();
			}

			if (throwable instanceof RuntimeException) {
				throw (RuntimeException)throwable;
			} else if (!(throwable instanceof ParsingException)) {
				throw (Error)throwable;
			} else {
				throw (ParsingException)throwable;
			}
		} finally {
			if (frameOpen) {
				this.lexicalBuffer.endNode(changeLogInParameter, true);
			}
		}
	}

	public final void parseNewNameCharactersParameter() {
		NewNameCharactersParameter newNameCharactersParameter = new NewNameCharactersParameter(126);
		boolean frameOpen = true;
		this.lexicalBuffer.beginNode(newNameCharactersParameter);

		try {
			try {
				ZKMScriptToken nameToken = this.consumeParameter(128);
				this.consumeParameter(15);
				this.parseCharacterType();
				this.lexicalBuffer.endNode(newNameCharactersParameter, true);
				frameOpen = false;
				newNameCharactersParameter.setParameterName(nameToken.image);
			} catch (Throwable throwable) {
				if (frameOpen) {
					this.lexicalBuffer.discardNode(newNameCharactersParameter);
					frameOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			}
		} catch (Throwable secondaryThrowable) {
			if (frameOpen) {
				this.lexicalBuffer.endNode(newNameCharactersParameter, true);
			}

			throw secondaryThrowable;
		}
	}

	public final void parseLoadParameter() {
		LoadParameter loadParameter = new LoadParameter(6);
		boolean frameOpen = true;
		this.lexicalBuffer.beginNode(loadParameter);

		try {
			switch (this.nextToken.kind) {
				case 141:
					this.parseExpectedFinalHash();
					break;
				case 153:
					this.parseExpectedInitialHash();
					break;
				default:
					this.la1Generation[10] = this.generationCounter;
					this.consumeParameter(-1);
					throw new ParsingException();
			}

			this.lexicalBuffer.endNode(loadParameter, true);
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (true) {
					this.lexicalBuffer.discardNode(loadParameter);
					frameOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}
				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable secondaryThrowable) {
				if (frameOpen) {
					this.lexicalBuffer.endNode(loadParameter, true);
				}

				throw secondaryThrowable;
			}
		}
	}

	public final void parseComplexFieldSpecifier() {
		ComplexFieldSpecifier complexFieldSpecifier = new ComplexFieldSpecifier(201);
		boolean frameOpen = true;
		this.lexicalBuffer.beginNode(complexFieldSpecifier);

		try {
			switch (this.nextToken.kind) {
				case 23:
				case 37:
				case 39:
				case 40:
				case 42:
				case 43:
				case 45:
				case 46:
				case 47:
				case 48:
				case 51:
				case 52:
				case 56:
				case 60:
				case 61:
				case 63:
				case 64:
				case 69:
				case 71:
				case 74:
				case 77:
				case 80:
				case 81:
				case 85:
				case 86:
				case 87:
				case 90:
				case 213:
				case 214:
					this.parseFieldName();
					break;
				case 25:
				case 28:
					this.parseBracketedFieldSpecifier();
					break;
				default:
					this.la1Generation[161] = this.generationCounter;
					this.consumeParameter(-1);
					throw new ParsingException();
			}

			this.lexicalBuffer.endNode(complexFieldSpecifier, true);
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (true) {
					this.lexicalBuffer.discardNode(complexFieldSpecifier);
					frameOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}
				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable secondaryThrowable) {
				if (frameOpen) {
					this.lexicalBuffer.endNode(complexFieldSpecifier, true);
				}

				throw secondaryThrowable;
			}
		}
	}

	public final void parseObfuscateReferencesPackageParameter() {
		ObfuscateReferencesPackageParameter obfuscateReferencesPackageParameter = new ObfuscateReferencesPackageParameter(133);
		boolean frameOpen = true;
		this.lexicalBuffer.beginNode(obfuscateReferencesPackageParameter);

		try {
			try {
				ZKMScriptToken nameToken = this.consumeParameter(175);
				this.consumeParameter(15);
				this.parseStringLiteral();
				this.lexicalBuffer.endNode(obfuscateReferencesPackageParameter, true);
				frameOpen = false;
				obfuscateReferencesPackageParameter.setParameterName(nameToken.image);
			} catch (Throwable throwable) {
				if (frameOpen) {
					this.lexicalBuffer.discardNode(obfuscateReferencesPackageParameter);
					frameOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			}
		} catch (Throwable secondaryThrowable) {
			if (frameOpen) {
				this.lexicalBuffer.endNode(obfuscateReferencesPackageParameter, true);
			}

			throw secondaryThrowable;
		}
	}

	public final void parseLiteralPackageComponent() {
		LiteralPackageComponent literalPackageComponent = new LiteralPackageComponent(192);
		boolean frameOpen = true;
		this.lexicalBuffer.beginNode(literalPackageComponent);

		try {
			switch (this.nextToken.kind) {
				case 23:
					ZKMScriptToken nameToken = this.consumeParameter(23);
					this.consumeParameter(21);
					literalPackageComponent.setText(nameToken.image);
					this.lexicalBuffer.endNode(literalPackageComponent, true);
					frameOpen = false;
					nameToken.setDescriptionIndex(2);
					return;
				case 36:
					ZKMScriptToken nameToken6 = this.consumeParameter(36);
					this.consumeParameter(21);
					literalPackageComponent.setText(nameToken6.image);
					this.lexicalBuffer.endNode(literalPackageComponent, true);
					frameOpen = false;
					nameToken6.setDescriptionIndex(2);
					return;
				case 37:
					ZKMScriptToken nameToken19 = this.consumeParameter(37);
					this.consumeParameter(21);
					literalPackageComponent.setText(nameToken19.image);
					this.lexicalBuffer.endNode(literalPackageComponent, true);
					frameOpen = false;
					nameToken19.setDescriptionIndex(2);
					return;
				case 39:
					ZKMScriptToken nameToken4 = this.consumeParameter(39);
					this.consumeParameter(21);
					literalPackageComponent.setText(nameToken4.image);
					this.lexicalBuffer.endNode(literalPackageComponent, true);
					frameOpen = false;
					nameToken4.setDescriptionIndex(2);
					return;
				case 40:
					ZKMScriptToken nameToken25 = this.consumeParameter(40);
					this.consumeParameter(21);
					literalPackageComponent.setText(nameToken25.image);
					this.lexicalBuffer.endNode(literalPackageComponent, true);
					frameOpen = false;
					nameToken25.setDescriptionIndex(2);
					return;
				case 42:
					ZKMScriptToken nameToken11 = this.consumeParameter(42);
					this.consumeParameter(21);
					literalPackageComponent.setText(nameToken11.image);
					this.lexicalBuffer.endNode(literalPackageComponent, true);
					frameOpen = false;
					nameToken11.setDescriptionIndex(2);
					return;
				case 43:
					ZKMScriptToken nameToken15 = this.consumeParameter(43);
					this.consumeParameter(21);
					literalPackageComponent.setText(nameToken15.image);
					this.lexicalBuffer.endNode(literalPackageComponent, true);
					frameOpen = false;
					nameToken15.setDescriptionIndex(2);
					return;
				case 45:
					ZKMScriptToken nameToken18 = this.consumeParameter(45);
					this.consumeParameter(21);
					literalPackageComponent.setText(nameToken18.image);
					this.lexicalBuffer.endNode(literalPackageComponent, true);
					frameOpen = false;
					nameToken18.setDescriptionIndex(2);
					return;
				case 46:
					ZKMScriptToken nameToken7 = this.consumeParameter(46);
					this.consumeParameter(21);
					literalPackageComponent.setText(nameToken7.image);
					this.lexicalBuffer.endNode(literalPackageComponent, true);
					frameOpen = false;
					nameToken7.setDescriptionIndex(2);
					return;
				case 47:
					ZKMScriptToken nameToken16 = this.consumeParameter(47);
					this.consumeParameter(21);
					literalPackageComponent.setText(nameToken16.image);
					this.lexicalBuffer.endNode(literalPackageComponent, true);
					frameOpen = false;
					nameToken16.setDescriptionIndex(2);
					return;
				case 48:
					ZKMScriptToken nameToken17 = this.consumeParameter(48);
					this.consumeParameter(21);
					literalPackageComponent.setText(nameToken17.image);
					this.lexicalBuffer.endNode(literalPackageComponent, true);
					frameOpen = false;
					nameToken17.setDescriptionIndex(2);
					return;
				case 51:
					ZKMScriptToken nameToken31 = this.consumeParameter(51);
					this.consumeParameter(21);
					literalPackageComponent.setText(nameToken31.image);
					this.lexicalBuffer.endNode(literalPackageComponent, true);
					frameOpen = false;
					nameToken31.setDescriptionIndex(2);
					return;
				case 52:
					ZKMScriptToken nameToken27 = this.consumeParameter(52);
					this.consumeParameter(21);
					literalPackageComponent.setText(nameToken27.image);
					this.lexicalBuffer.endNode(literalPackageComponent, true);
					frameOpen = false;
					nameToken27.setDescriptionIndex(2);
					return;
				case 56:
					ZKMScriptToken nameToken9 = this.consumeParameter(56);
					this.consumeParameter(21);
					literalPackageComponent.setText(nameToken9.image);
					this.lexicalBuffer.endNode(literalPackageComponent, true);
					frameOpen = false;
					nameToken9.setDescriptionIndex(2);
					return;
				case 60:
					ZKMScriptToken nameToken8 = this.consumeParameter(60);
					this.consumeParameter(21);
					literalPackageComponent.setText(nameToken8.image);
					this.lexicalBuffer.endNode(literalPackageComponent, true);
					frameOpen = false;
					nameToken8.setDescriptionIndex(2);
					return;
				case 61:
					ZKMScriptToken nameToken14 = this.consumeParameter(61);
					this.consumeParameter(21);
					literalPackageComponent.setText(nameToken14.image);
					this.lexicalBuffer.endNode(literalPackageComponent, true);
					frameOpen = false;
					nameToken14.setDescriptionIndex(2);
					return;
				case 63:
					ZKMScriptToken nameToken29 = this.consumeParameter(63);
					this.consumeParameter(21);
					literalPackageComponent.setText(nameToken29.image);
					this.lexicalBuffer.endNode(literalPackageComponent, true);
					frameOpen = false;
					nameToken29.setDescriptionIndex(2);
					return;
				case 64:
					ZKMScriptToken nameToken10 = this.consumeParameter(64);
					this.consumeParameter(21);
					literalPackageComponent.setText(nameToken10.image);
					this.lexicalBuffer.endNode(literalPackageComponent, true);
					frameOpen = false;
					nameToken10.setDescriptionIndex(2);
					return;
				case 67:
					ZKMScriptToken nameToken30 = this.consumeParameter(67);
					this.consumeParameter(21);
					literalPackageComponent.setText(nameToken30.image);
					this.lexicalBuffer.endNode(literalPackageComponent, true);
					frameOpen = false;
					nameToken30.setDescriptionIndex(2);
					return;
				case 69:
					ZKMScriptToken nameToken12 = this.consumeParameter(69);
					this.consumeParameter(21);
					literalPackageComponent.setText(nameToken12.image);
					this.lexicalBuffer.endNode(literalPackageComponent, true);
					frameOpen = false;
					nameToken12.setDescriptionIndex(2);
					return;
				case 71:
					ZKMScriptToken nameToken13 = this.consumeParameter(71);
					this.consumeParameter(21);
					literalPackageComponent.setText(nameToken13.image);
					this.lexicalBuffer.endNode(literalPackageComponent, true);
					frameOpen = false;
					nameToken13.setDescriptionIndex(2);
					return;
				case 74:
					ZKMScriptToken nameToken28 = this.consumeParameter(74);
					this.consumeParameter(21);
					literalPackageComponent.setText(nameToken28.image);
					this.lexicalBuffer.endNode(literalPackageComponent, true);
					frameOpen = false;
					nameToken28.setDescriptionIndex(2);
					return;
				case 77:
					ZKMScriptToken nameToken24 = this.consumeParameter(77);
					this.consumeParameter(21);
					literalPackageComponent.setText(nameToken24.image);
					this.lexicalBuffer.endNode(literalPackageComponent, true);
					frameOpen = false;
					nameToken24.setDescriptionIndex(2);
					return;
				case 80:
					ZKMScriptToken nameToken21 = this.consumeParameter(80);
					this.consumeParameter(21);
					literalPackageComponent.setText(nameToken21.image);
					this.lexicalBuffer.endNode(literalPackageComponent, true);
					frameOpen = false;
					nameToken21.setDescriptionIndex(2);
					return;
				case 81:
					ZKMScriptToken nameToken20 = this.consumeParameter(81);
					this.consumeParameter(21);
					literalPackageComponent.setText(nameToken20.image);
					this.lexicalBuffer.endNode(literalPackageComponent, true);
					frameOpen = false;
					nameToken20.setDescriptionIndex(2);
					return;
				case 85:
					ZKMScriptToken nameToken5 = this.consumeParameter(85);
					this.consumeParameter(21);
					literalPackageComponent.setText(nameToken5.image);
					this.lexicalBuffer.endNode(literalPackageComponent, true);
					frameOpen = false;
					nameToken5.setDescriptionIndex(2);
					return;
				case 86:
					ZKMScriptToken nameToken22 = this.consumeParameter(86);
					this.consumeParameter(21);
					literalPackageComponent.setText(nameToken22.image);
					this.lexicalBuffer.endNode(literalPackageComponent, true);
					frameOpen = false;
					nameToken22.setDescriptionIndex(2);
					return;
				case 87:
					ZKMScriptToken nameToken26 = this.consumeParameter(87);
					this.consumeParameter(21);
					literalPackageComponent.setText(nameToken26.image);
					this.lexicalBuffer.endNode(literalPackageComponent, true);
					frameOpen = false;
					nameToken26.setDescriptionIndex(2);
					return;
				case 90:
					ZKMScriptToken nameToken23 = this.consumeParameter(90);
					this.consumeParameter(21);
					literalPackageComponent.setText(nameToken23.image);
					this.lexicalBuffer.endNode(literalPackageComponent, true);
					frameOpen = false;
					nameToken23.setDescriptionIndex(2);
					return;
				case 213:
					ZKMScriptToken nameToken2 = this.consumeParameter(213);
					this.consumeParameter(21);
					literalPackageComponent.setText(nameToken2.image);
					this.lexicalBuffer.endNode(literalPackageComponent, true);
					frameOpen = false;
					nameToken2.setDescriptionIndex(2);
					return;
				case 214:
					ZKMScriptToken nameToken3 = this.consumeParameter(214);
					this.consumeParameter(21);
					literalPackageComponent.setText(nameToken3.image);
					this.lexicalBuffer.endNode(literalPackageComponent, true);
					frameOpen = false;
					nameToken3.setDescriptionIndex(2);
					return;
				default:
					this.la1Generation[150] = this.generationCounter;
					this.consumeParameter(-1);
					throw new ParsingException();
			}
		} finally {
			if (frameOpen) {
				this.lexicalBuffer.endNode(literalPackageComponent, true);
			}
		}
	}

	public final void parseClassComplexAnnotationSpecifier() {
		ClassComplexAnnotationSpecifier classComplexAnnotationSpecifier = new ClassComplexAnnotationSpecifier(171);
		boolean frameOpen = true;
		this.lexicalBuffer.beginNode(classComplexAnnotationSpecifier);

		try {
			this.parseComplexAnnotationSpecifier();
			this.lexicalBuffer.endNode(classComplexAnnotationSpecifier, true);
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (true) {
					this.lexicalBuffer.discardNode(classComplexAnnotationSpecifier);
					frameOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}
				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable secondaryThrowable) {
				if (frameOpen) {
					this.lexicalBuffer.endNode(classComplexAnnotationSpecifier, true);
				}

				throw secondaryThrowable;
			}
		}
	}

	public final void parseParameterPlaceHolder() {
		ParameterPlaceHolder parameterPlaceHolder = new ParameterPlaceHolder(216);
		this.lexicalBuffer.beginNode(parameterPlaceHolder);

		try {
			this.consumeParameter(32);
		} finally {
			this.lexicalBuffer.endNode(parameterPlaceHolder, true);
		}
	}

	public final void parseResetRemoveMethodCalls() {
		ResetRemoveMethodCallsStatement resetRemoveMethodCalls = new ResetRemoveMethodCallsStatement(77);
		boolean frameOpen = true;
		this.lexicalBuffer.beginNode(resetRemoveMethodCalls);

		try {
			ZKMScriptToken nameToken = this.consumeParameter(158);
			this.statementKind = 158;
			this.consumeParameter(19);
			this.lexicalBuffer.endNode(resetRemoveMethodCalls, true);
			frameOpen = false;
			resetRemoveMethodCalls.setLineNumber(nameToken.beginLine);
		} catch (Throwable throwable) {
			if (frameOpen) {
				this.lexicalBuffer.endNode(resetRemoveMethodCalls, true);
			}

			throw throwable;
		}
	}

	public final void parseUnSkipArchivePath() {
		UnSkipArchivePath unSkipArchivePath = new UnSkipArchivePath(5);
		boolean frameOpen = true;
		this.lexicalBuffer.beginNode(unSkipArchivePath);

		try {
			this.consumeParameter(24);
			this.parseStringLiteral();
			this.lexicalBuffer.endNode(unSkipArchivePath, true);
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (true) {
					this.lexicalBuffer.discardNode(unSkipArchivePath);
					frameOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}
				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable secondaryThrowable) {
				if (frameOpen) {
					this.lexicalBuffer.endNode(unSkipArchivePath, true);
				}

				throw secondaryThrowable;
			}
		}
	}

	public final void parseMemberSpecifierComplexAnnotationSpecifier() {
		MemberSpecifierComplexAnnotationSpecifier memberSpecifierComplexAnnotationSpecifier = new MemberSpecifierComplexAnnotationSpecifier(173);
		boolean frameOpen = true;
		this.lexicalBuffer.beginNode(memberSpecifierComplexAnnotationSpecifier);

		try {
			this.parseComplexAnnotationSpecifier();
			this.lexicalBuffer.endNode(memberSpecifierComplexAnnotationSpecifier, true);
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (true) {
					this.lexicalBuffer.discardNode(memberSpecifierComplexAnnotationSpecifier);
					frameOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}
				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable secondaryThrowable) {
				if (frameOpen) {
					this.lexicalBuffer.endNode(memberSpecifierComplexAnnotationSpecifier, true);
				}

				throw secondaryThrowable;
			}
		}
	}

	private void rescanToken() {
		this.rescanning = true;

		for (int i = 0; i < 28; i++) {
			try {
				LookaheadRecord lookaheadRecord = this.lookaheadRecords[i];

				while (true) {
					if (lookaheadRecord.generation > this.generationCounter) {
						this.lookaheadRemaining = lookaheadRecord.lookaheadCount;
						this.lastPos = this.scanPos = lookaheadRecord.first;
						switch (i) {
							case 0:
								this.matchesComplexPackageSpecifierExpansionAlt1();
								break;
							case 1:
								this.matchesMemberComplexAnnotationSpecifierExpansionAlt2();
								break;
							case 2:
								this.matchesMemberModifierListExpansionAlt3();
								break;
							case 3:
								this.matchesMethodSignatureExpansionAlt4();
								break;
							case 4:
								this.matchesClassComplexAnnotationSpecifierExpansion();
								break;
							case 5:
								this.matchesClassModifierListExpansion();
								break;
							case 6:
								this.matchesJarQualifierExpansion();
								break;
							case 7:
								this.matchesComplexPackageSpecifierExpansionAlt8();
								break;
							case 8:
								this.matchesContainingClauseExpansion();
								break;
							case 9:
								this.matchesMemberComplexAnnotationSpecifierExpansionAlt10();
								break;
							case 10:
								this.matchesMemberModifierListExpansionAlt11();
								break;
							case 11:
								this.matchesMethodSignatureExpansionAlt12();
								break;
							case 12:
								this.matchesLinkMethodSignatureExpansion();
								break;
							case 13:
								this.matchesJarQualifierBeforeLinkedClassNameExpansion();
								break;
							case 14:
								this.matchesComplexModuleSpecifierExpansion();
								break;
							case 15:
								this.matchesComplexPackageSpecifierExpansion();
								break;
							case 16:
								this.matchesStandAloneAnnotationExpansion();
								break;
							case 17:
								this.matchesReferencingAnnotationExpansion();
								break;
							case 18:
								this.matchesBracketedMemberSpecifierExpansion();
								break;
							case 19:
								this.memberSpecifierAnnotationLookahead();
								break;
							case 20:
								this.memberSpecifierModifierLoopLookahead();
								break;
							case 21:
								this.basicMethodSignatureLookahead();
								break;
							case 22:
								this.annotationPackageNameLookahead();
								break;
							case 23:
								this.moduleNameComponentLoopLookahead();
								break;
							case 24:
								this.literalPackageComponentLoopLookahead();
								break;
							case 25:
								this.linkPackageComponentLoopLookahead();
								break;
							case 26:
								this.complexFieldSpecifierLookahead();
								break;
							case 27:
								this.qualifiedClassNamePackageLookahead();
						}
					}

					if ((lookaheadRecord = lookaheadRecord.next) == null) {
						break;
					}
				}
			} catch (LookaheadSuccess ignored) {
			}
		}

		this.rescanning = false;
	}

	public final void parseReferencingAnnotation() {
		ReferencingAnnotation referencingAnnotation = new ReferencingAnnotation(179);
		boolean frameOpen = true;
		this.lexicalBuffer.beginNode(referencingAnnotation);

		try {
			this.consumeParameter(27);
			this.parseQualifiedClassName();
			this.lexicalBuffer.endNode(referencingAnnotation, true);
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (true) {
					this.lexicalBuffer.discardNode(referencingAnnotation);
					frameOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}
				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable secondaryThrowable) {
				if (frameOpen) {
					this.lexicalBuffer.endNode(referencingAnnotation, true);
				}

				throw secondaryThrowable;
			}
		}
	}

	public final void parseFieldSignature() {
		FieldSignature fieldSignature = new FieldSignature(200);
		boolean frameOpen = true;
		this.lexicalBuffer.beginNode(fieldSignature);

		try {
			if (this.matchesComplexFieldSpecifier(Integer.MAX_VALUE)) {
				this.parseComplexFieldSpecifier();
			} else {
				switch (this.nextToken.kind) {
					case 37:
					case 39:
					case 40:
					case 42:
					case 43:
					case 45:
					case 46:
					case 47:
					case 48:
					case 51:
					case 52:
					case 56:
					case 60:
					case 61:
					case 63:
					case 64:
					case 69:
					case 71:
					case 74:
					case 77:
					case 80:
					case 81:
					case 85:
					case 86:
					case 87:
					case 90:
					case 213:
						this.parseQualifiedType();
						this.parseComplexFieldSpecifier();
						break;
					default:
						this.la1Generation[160] = this.generationCounter;
						this.consumeParameter(-1);
						throw new ParsingException();
				}
			}

			this.lexicalBuffer.endNode(fieldSignature, true);
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (true) {
					this.lexicalBuffer.discardNode(fieldSignature);
					frameOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}
				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable secondaryThrowable) {
				if (frameOpen) {
					this.lexicalBuffer.endNode(fieldSignature, true);
				}

				throw secondaryThrowable;
			}
		}
	}

	public final void parseAndFileFilterComponent() {
		AndFileFilterComponent andFileFilterComponent = new AndFileFilterComponent(10);
		boolean frameOpen = true;
		this.lexicalBuffer.beginNode(andFileFilterComponent);

		try {
			this.consumeParameter(35);
			switch (this.nextToken.kind) {
				case 25:
					this.parseNegatedFileFilterComponent();
					break;
				case 210:
					this.parseFileFilterComponent();
					break;
				default:
					this.la1Generation[14] = this.generationCounter;
					this.consumeParameter(-1);
					throw new ParsingException();
			}

			this.lexicalBuffer.endNode(andFileFilterComponent, true);
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (true) {
					this.lexicalBuffer.discardNode(andFileFilterComponent);
					frameOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}
				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable secondaryThrowable) {
				if (frameOpen) {
					this.lexicalBuffer.endNode(andFileFilterComponent, true);
				}

				throw secondaryThrowable;
			}
		}
	}

	public final void parseDeleteUnknownAttributesParameter() {
		DeleteUnknownAttributesParameter deleteUnknownAttributesParameter = new DeleteUnknownAttributesParameter(92);
		boolean frameOpen = true;
		this.lexicalBuffer.beginNode(deleteUnknownAttributesParameter);

		try {
			try {
				ZKMScriptToken nameToken = this.consumeParameter(163);
				this.consumeParameter(15);
				this.parseBoolean();
				this.lexicalBuffer.endNode(deleteUnknownAttributesParameter, true);
				frameOpen = false;
				deleteUnknownAttributesParameter.setParameterName(nameToken.image);
			} catch (Throwable throwable) {
				if (frameOpen) {
					this.lexicalBuffer.discardNode(deleteUnknownAttributesParameter);
					frameOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			}
		} catch (Throwable secondaryThrowable) {
			if (frameOpen) {
				this.lexicalBuffer.endNode(deleteUnknownAttributesParameter, true);
			}

			throw secondaryThrowable;
		}
	}

	public final void parseChangeLogOutParameter() {
		ChangeLogOutParameter changeLogOutParameter = new ChangeLogOutParameter(96);
		boolean frameOpen = true;
		this.lexicalBuffer.beginNode(changeLogOutParameter);

		try {
			switch (this.nextToken.kind) {
				case 103:
					ZKMScriptToken nameToken = this.consumeParameter(103);
					this.consumeParameter(15);
					this.parseStringLiteral();
					this.lexicalBuffer.endNode(changeLogOutParameter, true);
					frameOpen = false;
					changeLogOutParameter.setParameterName(nameToken.image);
					return;
				case 115:
					ZKMScriptToken nameToken2 = this.consumeParameter(115);
					this.consumeParameter(15);
					this.parseStringLiteral();
					this.lexicalBuffer.endNode(changeLogOutParameter, true);
					frameOpen = false;
					changeLogOutParameter.setParameterName(nameToken2.image);
					return;
				default:
					this.la1Generation[103] = this.generationCounter;
					this.consumeParameter(-1);
					throw new ParsingException();
			}
		} catch (Throwable throwable) {
			if (frameOpen) {
				this.lexicalBuffer.discardNode(changeLogOutParameter);
				frameOpen = false;
			} else {
				this.lexicalBuffer.pop();
			}

			if (throwable instanceof RuntimeException) {
				throw (RuntimeException)throwable;
			} else if (!(throwable instanceof ParsingException)) {
				throw (Error)throwable;
			} else {
				throw (ParsingException)throwable;
			}
		} finally {
			if (frameOpen) {
				this.lexicalBuffer.endNode(changeLogOutParameter, true);
			}
		}
	}

	public final void parseFieldName() {
		FieldName fieldName = new FieldName(204);
		boolean frameOpen = true;
		this.lexicalBuffer.beginNode(fieldName);

		try {
			switch (this.nextToken.kind) {
				case 23:
					ZKMScriptToken nameToken = this.consumeParameter(23);
					fieldName.setText(nameToken.image);
					this.lexicalBuffer.endNode(fieldName, true);
					frameOpen = false;
					nameToken.setDescriptionIndex(4);
					return;
				case 37:
					ZKMScriptToken nameToken18 = this.consumeParameter(37);
					fieldName.setText(nameToken18.image);
					this.lexicalBuffer.endNode(fieldName, true);
					frameOpen = false;
					nameToken18.setDescriptionIndex(4);
					return;
				case 39:
					ZKMScriptToken nameToken4 = this.consumeParameter(39);
					fieldName.setText(nameToken4.image);
					this.lexicalBuffer.endNode(fieldName, true);
					frameOpen = false;
					nameToken4.setDescriptionIndex(4);
					return;
				case 40:
					ZKMScriptToken nameToken24 = this.consumeParameter(40);
					fieldName.setText(nameToken24.image);
					this.lexicalBuffer.endNode(fieldName, true);
					frameOpen = false;
					nameToken24.setDescriptionIndex(4);
					return;
				case 42:
					ZKMScriptToken nameToken10 = this.consumeParameter(42);
					fieldName.setText(nameToken10.image);
					this.lexicalBuffer.endNode(fieldName, true);
					frameOpen = false;
					nameToken10.setDescriptionIndex(4);
					return;
				case 43:
					ZKMScriptToken nameToken14 = this.consumeParameter(43);
					fieldName.setText(nameToken14.image);
					this.lexicalBuffer.endNode(fieldName, true);
					frameOpen = false;
					nameToken14.setDescriptionIndex(4);
					return;
				case 45:
					ZKMScriptToken nameToken17 = this.consumeParameter(45);
					fieldName.setText(nameToken17.image);
					this.lexicalBuffer.endNode(fieldName, true);
					frameOpen = false;
					nameToken17.setDescriptionIndex(4);
					return;
				case 46:
					ZKMScriptToken nameToken6 = this.consumeParameter(46);
					fieldName.setText(nameToken6.image);
					this.lexicalBuffer.endNode(fieldName, true);
					frameOpen = false;
					nameToken6.setDescriptionIndex(4);
					return;
				case 47:
					ZKMScriptToken nameToken15 = this.consumeParameter(47);
					fieldName.setText(nameToken15.image);
					this.lexicalBuffer.endNode(fieldName, true);
					frameOpen = false;
					nameToken15.setDescriptionIndex(4);
					return;
				case 48:
					ZKMScriptToken nameToken16 = this.consumeParameter(48);
					fieldName.setText(nameToken16.image);
					this.lexicalBuffer.endNode(fieldName, true);
					frameOpen = false;
					nameToken16.setDescriptionIndex(4);
					return;
				case 51:
					ZKMScriptToken nameToken29 = this.consumeParameter(51);
					fieldName.setText(nameToken29.image);
					this.lexicalBuffer.endNode(fieldName, true);
					frameOpen = false;
					nameToken29.setDescriptionIndex(4);
					return;
				case 52:
					ZKMScriptToken nameToken26 = this.consumeParameter(52);
					fieldName.setText(nameToken26.image);
					this.lexicalBuffer.endNode(fieldName, true);
					frameOpen = false;
					nameToken26.setDescriptionIndex(4);
					return;
				case 56:
					ZKMScriptToken nameToken8 = this.consumeParameter(56);
					fieldName.setText(nameToken8.image);
					this.lexicalBuffer.endNode(fieldName, true);
					frameOpen = false;
					nameToken8.setDescriptionIndex(4);
					return;
				case 60:
					ZKMScriptToken nameToken7 = this.consumeParameter(60);
					fieldName.setText(nameToken7.image);
					this.lexicalBuffer.endNode(fieldName, true);
					frameOpen = false;
					nameToken7.setDescriptionIndex(4);
					return;
				case 61:
					ZKMScriptToken nameToken13 = this.consumeParameter(61);
					fieldName.setText(nameToken13.image);
					this.lexicalBuffer.endNode(fieldName, true);
					frameOpen = false;
					nameToken13.setDescriptionIndex(4);
					return;
				case 63:
					ZKMScriptToken nameToken28 = this.consumeParameter(63);
					fieldName.setText(nameToken28.image);
					this.lexicalBuffer.endNode(fieldName, true);
					frameOpen = false;
					nameToken28.setDescriptionIndex(4);
					return;
				case 64:
					ZKMScriptToken nameToken9 = this.consumeParameter(64);
					fieldName.setText(nameToken9.image);
					this.lexicalBuffer.endNode(fieldName, true);
					frameOpen = false;
					nameToken9.setDescriptionIndex(4);
					return;
				case 69:
					ZKMScriptToken nameToken11 = this.consumeParameter(69);
					fieldName.setText(nameToken11.image);
					this.lexicalBuffer.endNode(fieldName, true);
					frameOpen = false;
					nameToken11.setDescriptionIndex(4);
					return;
				case 71:
					ZKMScriptToken nameToken12 = this.consumeParameter(71);
					fieldName.setText(nameToken12.image);
					this.lexicalBuffer.endNode(fieldName, true);
					frameOpen = false;
					nameToken12.setDescriptionIndex(4);
					return;
				case 74:
					ZKMScriptToken nameToken27 = this.consumeParameter(74);
					fieldName.setText(nameToken27.image);
					this.lexicalBuffer.endNode(fieldName, true);
					frameOpen = false;
					nameToken27.setDescriptionIndex(4);
					return;
				case 77:
					ZKMScriptToken nameToken23 = this.consumeParameter(77);
					fieldName.setText(nameToken23.image);
					this.lexicalBuffer.endNode(fieldName, true);
					frameOpen = false;
					nameToken23.setDescriptionIndex(4);
					return;
				case 80:
					ZKMScriptToken nameToken20 = this.consumeParameter(80);
					fieldName.setText(nameToken20.image);
					this.lexicalBuffer.endNode(fieldName, true);
					frameOpen = false;
					nameToken20.setDescriptionIndex(4);
					return;
				case 81:
					ZKMScriptToken nameToken19 = this.consumeParameter(81);
					fieldName.setText(nameToken19.image);
					this.lexicalBuffer.endNode(fieldName, true);
					frameOpen = false;
					nameToken19.setDescriptionIndex(4);
					return;
				case 85:
					ZKMScriptToken nameToken5 = this.consumeParameter(85);
					fieldName.setText(nameToken5.image);
					this.lexicalBuffer.endNode(fieldName, true);
					frameOpen = false;
					nameToken5.setDescriptionIndex(4);
					return;
				case 86:
					ZKMScriptToken nameToken21 = this.consumeParameter(86);
					fieldName.setText(nameToken21.image);
					this.lexicalBuffer.endNode(fieldName, true);
					frameOpen = false;
					nameToken21.setDescriptionIndex(4);
					return;
				case 87:
					ZKMScriptToken nameToken25 = this.consumeParameter(87);
					fieldName.setText(nameToken25.image);
					this.lexicalBuffer.endNode(fieldName, true);
					frameOpen = false;
					nameToken25.setDescriptionIndex(4);
					return;
				case 90:
					ZKMScriptToken nameToken22 = this.consumeParameter(90);
					fieldName.setText(nameToken22.image);
					this.lexicalBuffer.endNode(fieldName, true);
					frameOpen = false;
					nameToken22.setDescriptionIndex(4);
					return;
				case 213:
					ZKMScriptToken nameToken2 = this.consumeParameter(213);
					fieldName.setText(nameToken2.image);
					this.lexicalBuffer.endNode(fieldName, true);
					frameOpen = false;
					nameToken2.setDescriptionIndex(4);
					return;
				case 214:
					ZKMScriptToken nameToken3 = this.consumeParameter(214);
					fieldName.setText(nameToken3.image);
					this.lexicalBuffer.endNode(fieldName, true);
					frameOpen = false;
					nameToken3.setDescriptionIndex(4);
					return;
				default:
					this.la1Generation[165] = this.generationCounter;
					this.consumeParameter(-1);
					throw new ParsingException();
			}
		} finally {
			if (frameOpen) {
				this.lexicalBuffer.endNode(fieldName, true);
			}
		}
	}

	public final void parseEnhancedIncrementalParameter() {
		EnhancedIncrementalParameter enhancedIncrementalParameter = new EnhancedIncrementalParameter(111);
		boolean frameOpen = true;
		this.lexicalBuffer.beginNode(enhancedIncrementalParameter);

		try {
			try {
				ZKMScriptToken nameToken = this.consumeParameter(117);
				this.consumeParameter(15);
				this.parseBoolean();
				this.lexicalBuffer.endNode(enhancedIncrementalParameter, true);
				frameOpen = false;
				enhancedIncrementalParameter.setParameterName(nameToken.image);
			} catch (Throwable throwable) {
				if (frameOpen) {
					this.lexicalBuffer.discardNode(enhancedIncrementalParameter);
					frameOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			}
		} catch (Throwable secondaryThrowable) {
			if (frameOpen) {
				this.lexicalBuffer.endNode(enhancedIncrementalParameter, true);
			}

			throw secondaryThrowable;
		}
	}

	public final void parseResetObfuscateExceptionsExclusions() {
		ResetObfuscateExceptionsExclusionsStatement resetObfuscateExceptionsExclusions = new ResetObfuscateExceptionsExclusionsStatement(69);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(resetObfuscateExceptionsExclusions);

		try {
			ZKMScriptToken resetObfuscateExceptionsExclusionsToken = this.consumeParameter(204);
			this.statementKind = 204;
			this.consumeParameter(19);
			this.lexicalBuffer.endNode(resetObfuscateExceptionsExclusions, true);
			nodeOpen = false;
			resetObfuscateExceptionsExclusions.setLineNumber(resetObfuscateExceptionsExclusionsToken.beginLine);
		} catch (Throwable throwable) {
			if (nodeOpen) {
				this.lexicalBuffer.endNode(resetObfuscateExceptionsExclusions, true);
			}

			throw throwable;
		}
	}

	public final void parseBracketedAnnotationSpecifier() {
		BracketedAnnotationSpecifier bracketedAnnotationSpecifier = new BracketedAnnotationSpecifier(176);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(bracketedAnnotationSpecifier);

		try {
			switch (this.nextToken.kind) {
				case 25:
					this.consumeParameter(25);
					bracketedAnnotationSpecifier.setNegated();
					break;
				default:
					this.la1Generation[135] = this.generationCounter;
			}

			this.consumeParameter(28);
			this.parseAndAnnotationSpecifier();

			while (true) {
				switch (this.nextToken.kind) {
					case 34:
						this.consumeParameter(34);
						this.parseAndAnnotationSpecifier();
						break;
					default:
						this.la1Generation[136] = this.generationCounter;
						this.consumeParameter(29);
						this.lexicalBuffer.endNode(bracketedAnnotationSpecifier, true);
						return;
				}
			}
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (true) {
					this.lexicalBuffer.discardNode(bracketedAnnotationSpecifier);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}
				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable rethrownThrowable) {
				if (nodeOpen) {
					this.lexicalBuffer.endNode(bracketedAnnotationSpecifier, true);
				}

				throw rethrownThrowable;
			}
		}
	}

	public final void parseUniqueClassNamesParameter() {
		UniqueClassNamesParameter uniqueClassNamesParameter = new UniqueClassNamesParameter(121);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(uniqueClassNamesParameter);

		try {
			try {
				ZKMScriptToken uniqueClassNamesToken = this.consumeParameter(112);
				this.consumeParameter(15);
				this.parseBoolean();
				this.lexicalBuffer.endNode(uniqueClassNamesParameter, true);
				nodeOpen = false;
				uniqueClassNamesParameter.setParameterName(uniqueClassNamesToken.image);
			} catch (Throwable throwable) {
				if (nodeOpen) {
					this.lexicalBuffer.discardNode(uniqueClassNamesParameter);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			}
		} catch (Throwable rethrownThrowable) {
			if (nodeOpen) {
				this.lexicalBuffer.endNode(uniqueClassNamesParameter, true);
			}

			throw rethrownThrowable;
		}
	}

	public final void parseMethodParametersParameterType() {
		MethodParametersParameterType methodParametersParameterType = new MethodParametersParameterType(169);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(methodParametersParameterType);

		try {
			switch (this.nextToken.kind) {
				case 42:
					ZKMScriptToken keepToken = this.consumeParameter(42);
					this.lexicalBuffer.endNode(methodParametersParameterType, true);
					nodeOpen = false;
					methodParametersParameterType.setText(keepToken.image);
					return;
				case 60:
					ZKMScriptToken deleteToken = this.consumeParameter(60);
					this.lexicalBuffer.endNode(methodParametersParameterType, true);
					nodeOpen = false;
					methodParametersParameterType.setText(deleteToken.image);
					return;
				case 81:
					ZKMScriptToken obfuscateToken = this.consumeParameter(81);
					this.lexicalBuffer.endNode(methodParametersParameterType, true);
					nodeOpen = false;
					methodParametersParameterType.setText(obfuscateToken.image);
					return;
				case 93:
					ZKMScriptToken keepVisibleToken = this.consumeParameter(93);
					this.lexicalBuffer.endNode(methodParametersParameterType, true);
					nodeOpen = false;
					methodParametersParameterType.setText(keepVisibleToken.image);
					return;
				case 140:
					ZKMScriptToken keepIfNotObfuscatedToken = this.consumeParameter(140);
					this.lexicalBuffer.endNode(methodParametersParameterType, true);
					nodeOpen = false;
					methodParametersParameterType.setText(keepIfNotObfuscatedToken.image);
					return;
				case 176:
					ZKMScriptToken keepVisibleIfNotObfuscatedToken = this.consumeParameter(176);
					this.lexicalBuffer.endNode(methodParametersParameterType, true);
					nodeOpen = false;
					methodParametersParameterType.setText(keepVisibleIfNotObfuscatedToken.image);
					return;
				default:
					this.la1Generation[133] = this.generationCounter;
					this.consumeParameter(-1);
					throw new ParsingException();
			}
		} finally {
			if (nodeOpen) {
				this.lexicalBuffer.endNode(methodParametersParameterType, true);
			}
		}
	}

	public final void parseFileFilterComponent() {
		FileFilterComponent fileFilterComponent = new FileFilterComponent(12);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(fileFilterComponent);

		try {
			this.parseStringLiteral();
			this.lexicalBuffer.endNode(fileFilterComponent, true);
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (true) {
					this.lexicalBuffer.discardNode(fileFilterComponent);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}
				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable rethrownThrowable) {
				if (nodeOpen) {
					this.lexicalBuffer.endNode(fileFilterComponent, true);
				}

				throw rethrownThrowable;
			}
		}
	}

	public final void parseComplexMemberSpecifier() {
		ComplexMemberSpecifier complexMemberSpecifier = new ComplexMemberSpecifier(149);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(complexMemberSpecifier);

		try {
			if (this.matchesBracketedMemberSpecifier(Integer.MAX_VALUE)) {
				this.parseBracketedMemberSpecifier();
			} else {
				switch (this.nextToken.kind) {
					case 23:
					case 25:
					case 27:
					case 28:
					case 37:
					case 39:
					case 40:
					case 42:
					case 43:
					case 45:
					case 46:
					case 47:
					case 48:
					case 50:
					case 51:
					case 52:
					case 54:
					case 56:
					case 57:
					case 58:
					case 59:
					case 60:
					case 61:
					case 63:
					case 64:
					case 67:
					case 68:
					case 69:
					case 71:
					case 72:
					case 73:
					case 74:
					case 75:
					case 77:
					case 80:
					case 81:
					case 82:
					case 84:
					case 85:
					case 86:
					case 87:
					case 90:
					case 99:
					case 213:
					case 214:
						this.parseMemberSpecifier();
						break;
					case 24:
					case 26:
					case 29:
					case 30:
					case 31:
					case 32:
					case 33:
					case 34:
					case 35:
					case 36:
					case 38:
					case 41:
					case 44:
					case 49:
					case 53:
					case 55:
					case 62:
					case 65:
					case 66:
					case 70:
					case 76:
					case 78:
					case 79:
					case 83:
					case 88:
					case 89:
					case 91:
					case 92:
					case 93:
					case 94:
					case 95:
					case 96:
					case 97:
					case 98:
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
					case 125:
					case 126:
					case 127:
					case 128:
					case 129:
					case 130:
					case 131:
					case 132:
					case 133:
					case 134:
					case 135:
					case 136:
					case 137:
					case 138:
					case 139:
					case 140:
					case 141:
					case 142:
					case 143:
					case 144:
					case 145:
					case 146:
					case 147:
					case 148:
					case 149:
					case 150:
					case 151:
					case 152:
					case 153:
					case 154:
					case 155:
					case 156:
					case 157:
					case 158:
					case 159:
					case 160:
					case 161:
					case 162:
					case 163:
					case 164:
					case 165:
					case 166:
					case 167:
					case 168:
					case 169:
					case 170:
					case 171:
					case 172:
					case 173:
					case 174:
					case 175:
					case 176:
					case 177:
					case 178:
					case 179:
					case 180:
					case 181:
					case 182:
					case 183:
					case 184:
					case 185:
					case 186:
					case 187:
					case 188:
					case 189:
					case 190:
					case 191:
					case 192:
					case 193:
					case 194:
					case 195:
					case 196:
					case 197:
					case 198:
					case 199:
					case 200:
					case 201:
					case 202:
					case 203:
					case 204:
					case 205:
					case 206:
					case 207:
					case 208:
					case 209:
					case 210:
					case 211:
					case 212:
					default:
						this.la1Generation[110] = this.generationCounter;
						this.consumeParameter(-1);
						throw new ParsingException();
				}
			}

			this.lexicalBuffer.endNode(complexMemberSpecifier, true);
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (true) {
					this.lexicalBuffer.discardNode(complexMemberSpecifier);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}
				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable rethrownThrowable) {
				if (nodeOpen) {
					this.lexicalBuffer.endNode(complexMemberSpecifier, true);
				}

				throw rethrownThrowable;
			}
		}
	}

	public final void parseObfuscateParameter() {
		ObfuscateParameter obfuscateParameter = new ObfuscateParameter(94);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(obfuscateParameter);

		try {
			switch (this.nextToken.kind) {
				case 78:
					this.parsePreverifyParameter();
					break;
				case 79:
				case 80:
				case 81:
				case 82:
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
				case 96:
				case 97:
				case 98:
				case 99:
				case 100:
				case 102:
				case 107:
				case 108:
				case 109:
				case 111:
				case 120:
				case 125:
				case 126:
				case 127:
				case 129:
				case 130:
				case 132:
				case 137:
				case 138:
				case 140:
				case 141:
				case 142:
				case 144:
				case 146:
				case 152:
				case 153:
				case 154:
				case 157:
				case 158:
				case 159:
				case 161:
				case 162:
				case 163:
				case 164:
				case 165:
				case 166:
				case 167:
				case 169:
				case 170:
				case 171:
				case 172:
				case 173:
				case 174:
				case 176:
				case 177:
				case 178:
				case 179:
				case 180:
				case 181:
				case 182:
				case 184:
				case 186:
				case 187:
				case 189:
				case 190:
				case 191:
				case 192:
				case 193:
				case 195:
				case 196:
				case 197:
				case 198:
				case 199:
				case 200:
				default:
					this.la1Generation[99] = this.generationCounter;
					this.consumeParameter(-1);
					throw new ParsingException();
				case 85:
					this.parseRandomizeParameter();
					break;
				case 95:
					this.parseLineNumbersParameter();
					break;
				case 101:
					this.parseObfuscateFlowParameter();
					break;
				case 103:
				case 115:
					this.parseChangeLogOutParameter();
					break;
				case 104:
					this.parseLocalVariablesParameter();
					break;
				case 105:
					this.parseHideFieldNamesParameter();
					break;
				case 106:
					this.parseNewNamesPrefixParameter();
					break;
				case 110:
				case 143:
					this.parseChangeLogInParameter();
					break;
				case 112:
					this.parseUniqueClassNamesParameter();
					break;
				case 113:
					this.parseMethodParametersParameter();
					break;
				case 114:
					this.parseKeepGenericsParameter();
					break;
				case 116:
					this.parseLegalIdsParameter();
					break;
				case 117:
					this.parseEnhancedIncrementalParameter();
					break;
				case 118:
					this.parseNewClassNameFileParameter();
					break;
				case 119:
					this.parseNewFieldNameFileParameter();
					break;
				case 121:
					this.parseMakeClassesPublicParameter();
					break;
				case 122:
					this.parseUniqueMethodNamesParameter();
					break;
				case 123:
					this.parseNewMethodNameFileParameter();
					break;
				case 124:
					this.parseKeepBalancedLocksParameter();
					break;
				case 128:
					this.parseNewNameCharactersParameter();
					break;
				case 131:
					this.parseNewPackageNameFileParameter();
					break;
				case 133:
					this.parseKeepInnerClassesParameter();
					break;
				case 134:
					this.parseAutoReflectionHash();
					break;
				case 135:
					this.parseObfuscateParametersParameter();
					break;
				case 136:
					this.parseMixedCaseClassNamesParameter();
					break;
				case 139:
					this.parseObfuscateReferencesParameter();
					break;
				case 145:
					this.parseExceptionObfuscationParameter();
					break;
				case 147:
					this.parseAssumeRuntimeVersionParameter();
					break;
				case 148:
					this.parseEncryptLongConstantsParameter();
					break;
				case 149:
					this.parseHideStaticMethodNamesParameter();
					break;
				case 150:
					this.parseEncryptParameter();
					break;
				case 151:
					this.parseAutoReflectionPackage();
					break;
				case 155:
					this.parseMethodParameterChangesParameter();
					break;
				case 156:
					this.parseAutoReflectionParameter();
					break;
				case 160:
					this.parseEncryptIntegerConstantsParameter();
					break;
				case 168:
					this.parseAggressiveOverloadParameter();
					break;
				case 175:
					this.parseObfuscateReferencesPackageParameter();
					break;
				case 183:
					this.parseAllowMethodParameterChangesParameter();
					break;
				case 185:
					this.parseCollapsePackages();
					break;
				case 188:
					this.parseObfuscateReferenceStructuresParameter();
					break;
				case 194:
					this.parseMethodParameterChangePackageParameter();
					break;
				case 201:
					this.parseDeriveGroupingsFromChangeLogParameter();
			}

			this.lexicalBuffer.endNode(obfuscateParameter, true);
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (true) {
					this.lexicalBuffer.discardNode(obfuscateParameter);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}
				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable rethrownThrowable) {
				if (nodeOpen) {
					this.lexicalBuffer.endNode(obfuscateParameter, true);
				}

				throw rethrownThrowable;
			}
		}
	}

	public final void parseLinkLiteralPackageComponent() {
		LinkLiteralPackageComponent linkLiteralPackageComponent = new LinkLiteralPackageComponent(194);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(linkLiteralPackageComponent);

		try {
			switch (this.nextToken.kind) {
				case 23:
					ZKMScriptToken starToken = this.consumeParameter(23);
					linkLiteralPackageComponent.setText(starToken.image);
					this.lexicalBuffer.endNode(linkLiteralPackageComponent, true);
					nodeOpen = false;
					starToken.setDescriptionIndex(7);
					return;
				case 37:
					ZKMScriptToken allToken = this.consumeParameter(37);
					linkLiteralPackageComponent.setText(allToken.image);
					this.lexicalBuffer.endNode(linkLiteralPackageComponent, true);
					nodeOpen = false;
					allToken.setDescriptionIndex(7);
					return;
				case 39:
					ZKMScriptToken openToken = this.consumeParameter(39);
					linkLiteralPackageComponent.setText(openToken.image);
					this.lexicalBuffer.endNode(linkLiteralPackageComponent, true);
					nodeOpen = false;
					openToken.setDescriptionIndex(7);
					return;
				case 40:
					ZKMScriptToken enumToken = this.consumeParameter(40);
					this.consumeParameter(21);
					linkLiteralPackageComponent.setText(enumToken.image);
					this.lexicalBuffer.endNode(linkLiteralPackageComponent, true);
					nodeOpen = false;
					enumToken.setDescriptionIndex(7);
					return;
				case 42:
					ZKMScriptToken keepToken = this.consumeParameter(42);
					linkLiteralPackageComponent.setText(keepToken.image);
					this.lexicalBuffer.endNode(linkLiteralPackageComponent, true);
					nodeOpen = false;
					keepToken.setDescriptionIndex(7);
					return;
				case 43:
					ZKMScriptToken noneToken = this.consumeParameter(43);
					linkLiteralPackageComponent.setText(noneToken.image);
					this.lexicalBuffer.endNode(linkLiteralPackageComponent, true);
					nodeOpen = false;
					noneToken.setDescriptionIndex(7);
					return;
				case 45:
					ZKMScriptToken trimToken = this.consumeParameter(45);
					linkLiteralPackageComponent.setText(trimToken.image);
					this.lexicalBuffer.endNode(linkLiteralPackageComponent, true);
					nodeOpen = false;
					trimToken.setDescriptionIndex(7);
					return;
				case 46:
					ZKMScriptToken printToken = this.consumeParameter(46);
					linkLiteralPackageComponent.setText(printToken.image);
					this.lexicalBuffer.endNode(linkLiteralPackageComponent, true);
					nodeOpen = false;
					printToken.setDescriptionIndex(7);
					return;
				case 47:
					ZKMScriptToken lightToken = this.consumeParameter(47);
					linkLiteralPackageComponent.setText(lightToken.image);
					this.lexicalBuffer.endNode(linkLiteralPackageComponent, true);
					nodeOpen = false;
					lightToken.setDescriptionIndex(7);
					return;
				case 48:
					ZKMScriptToken heavyToken = this.consumeParameter(48);
					linkLiteralPackageComponent.setText(heavyToken.image);
					this.lexicalBuffer.endNode(linkLiteralPackageComponent, true);
					nodeOpen = false;
					heavyToken.setDescriptionIndex(7);
					return;
				case 51:
					ZKMScriptToken asciiToken = this.consumeParameter(51);
					this.consumeParameter(21);
					linkLiteralPackageComponent.setText(asciiToken.image);
					this.lexicalBuffer.endNode(linkLiteralPackageComponent, true);
					nodeOpen = false;
					asciiToken.setDescriptionIndex(7);
					return;
				case 52:
					ZKMScriptToken bridgeToken = this.consumeParameter(52);
					this.consumeParameter(21);
					linkLiteralPackageComponent.setText(bridgeToken.image);
					this.lexicalBuffer.endNode(linkLiteralPackageComponent, true);
					nodeOpen = false;
					bridgeToken.setDescriptionIndex(7);
					return;
				case 56:
					ZKMScriptToken searchToken = this.consumeParameter(56);
					linkLiteralPackageComponent.setText(searchToken.image);
					this.lexicalBuffer.endNode(linkLiteralPackageComponent, true);
					nodeOpen = false;
					searchToken.setDescriptionIndex(7);
					return;
				case 60:
					ZKMScriptToken deleteToken = this.consumeParameter(60);
					linkLiteralPackageComponent.setText(deleteToken.image);
					this.lexicalBuffer.endNode(linkLiteralPackageComponent, true);
					nodeOpen = false;
					deleteToken.setDescriptionIndex(7);
					return;
				case 61:
					ZKMScriptToken normalToken = this.consumeParameter(61);
					linkLiteralPackageComponent.setText(normalToken.image);
					this.lexicalBuffer.endNode(linkLiteralPackageComponent, true);
					nodeOpen = false;
					normalToken.setDescriptionIndex(7);
					return;
				case 63:
					ZKMScriptToken randomToken = this.consumeParameter(63);
					this.consumeParameter(21);
					linkLiteralPackageComponent.setText(randomToken.image);
					this.lexicalBuffer.endNode(linkLiteralPackageComponent, true);
					nodeOpen = false;
					randomToken.setDescriptionIndex(7);
					return;
				case 64:
					ZKMScriptToken excludeToken = this.consumeParameter(64);
					linkLiteralPackageComponent.setText(excludeToken.image);
					this.lexicalBuffer.endNode(linkLiteralPackageComponent, true);
					nodeOpen = false;
					excludeToken.setDescriptionIndex(7);
					return;
				case 69:
					ZKMScriptToken executeToken = this.consumeParameter(69);
					linkLiteralPackageComponent.setText(executeToken.image);
					this.lexicalBuffer.endNode(linkLiteralPackageComponent, true);
					nodeOpen = false;
					executeToken.setDescriptionIndex(7);
					return;
				case 71:
					ZKMScriptToken scrambleToken = this.consumeParameter(71);
					linkLiteralPackageComponent.setText(scrambleToken.image);
					this.lexicalBuffer.endNode(linkLiteralPackageComponent, true);
					nodeOpen = false;
					scrambleToken.setDescriptionIndex(7);
					return;
				case 74:
					ZKMScriptToken enhancedToken = this.consumeParameter(74);
					this.consumeParameter(21);
					linkLiteralPackageComponent.setText(enhancedToken.image);
					this.lexicalBuffer.endNode(linkLiteralPackageComponent, true);
					nodeOpen = false;
					enhancedToken.setDescriptionIndex(7);
					return;
				case 77:
					ZKMScriptToken syntheticToken = this.consumeParameter(77);
					this.consumeParameter(21);
					linkLiteralPackageComponent.setText(syntheticToken.image);
					this.lexicalBuffer.endNode(linkLiteralPackageComponent, true);
					nodeOpen = false;
					syntheticToken.setDescriptionIndex(7);
					return;
				case 80:
					ZKMScriptToken classpathToken = this.consumeParameter(80);
					linkLiteralPackageComponent.setText(classpathToken.image);
					this.lexicalBuffer.endNode(linkLiteralPackageComponent, true);
					nodeOpen = false;
					classpathToken.setDescriptionIndex(7);
					return;
				case 81:
					ZKMScriptToken obfuscateToken = this.consumeParameter(81);
					linkLiteralPackageComponent.setText(obfuscateToken.image);
					this.lexicalBuffer.endNode(linkLiteralPackageComponent, true);
					nodeOpen = false;
					obfuscateToken.setDescriptionIndex(7);
					return;
				case 86:
					ZKMScriptToken groupingsToken = this.consumeParameter(86);
					linkLiteralPackageComponent.setText(groupingsToken.image);
					this.lexicalBuffer.endNode(linkLiteralPackageComponent, true);
					nodeOpen = false;
					groupingsToken.setDescriptionIndex(7);
					return;
				case 87:
					ZKMScriptToken annotationToken = this.consumeParameter(87);
					this.consumeParameter(21);
					linkLiteralPackageComponent.setText(annotationToken.image);
					this.lexicalBuffer.endNode(linkLiteralPackageComponent, true);
					nodeOpen = false;
					annotationToken.setDescriptionIndex(7);
					return;
				case 90:
					ZKMScriptToken aggressiveToken = this.consumeParameter(90);
					linkLiteralPackageComponent.setText(aggressiveToken.image);
					this.lexicalBuffer.endNode(linkLiteralPackageComponent, true);
					nodeOpen = false;
					aggressiveToken.setDescriptionIndex(7);
					return;
				case 213:
					ZKMScriptToken nameToken = this.consumeParameter(213);
					linkLiteralPackageComponent.setText(nameToken.image);
					this.lexicalBuffer.endNode(linkLiteralPackageComponent, true);
					nodeOpen = false;
					nameToken.setDescriptionIndex(7);
					return;
				case 214:
					ZKMScriptToken wildcardNameToken = this.consumeParameter(214);
					linkLiteralPackageComponent.setText(wildcardNameToken.image);
					this.lexicalBuffer.endNode(linkLiteralPackageComponent, true);
					nodeOpen = false;
					wildcardNameToken.setDescriptionIndex(7);
					return;
				default:
					this.la1Generation[152] = this.generationCounter;
					this.consumeParameter(-1);
					throw new ParsingException();
			}
		} finally {
			if (nodeOpen) {
				this.lexicalBuffer.endNode(linkLiteralPackageComponent, true);
			}
		}
	}

	public final void parseTrimParameter() {
		TrimParameter trimParameter = new TrimParameter(87);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(trimParameter);

		try {
			switch (this.nextToken.kind) {
				case 163:
					this.parseDeleteUnknownAttributesParameter();
					break;
				case 172:
					this.parseDeleteExceptionAttributesParameter();
					break;
				case 177:
					this.parseDeleteAnnotationsParameter();
					break;
				case 178:
					this.parseDeleteSourceFileAttributesParameter();
					break;
				case 179:
					this.parseDeleteDeprecatedAttributesParameter();
					break;
				case 196:
					this.parseDeleteDebugExtensionAttributesParameter();
					break;
				default:
					this.la1Generation[98] = this.generationCounter;
					this.consumeParameter(-1);
					throw new ParsingException();
			}

			this.lexicalBuffer.endNode(trimParameter, true);
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (true) {
					this.lexicalBuffer.discardNode(trimParameter);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}
				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable rethrownThrowable) {
				if (nodeOpen) {
					this.lexicalBuffer.endNode(trimParameter, true);
				}

				throw rethrownThrowable;
			}
		}
	}

	public final void parseStandAloneAnnotation() {
		StandAloneAnnotation standAloneAnnotation = new StandAloneAnnotation(178);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(standAloneAnnotation);

		try {
			this.consumeParameter(27);
			this.parseQualifiedClassName();
			this.lexicalBuffer.endNode(standAloneAnnotation, true);
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (true) {
					this.lexicalBuffer.discardNode(standAloneAnnotation);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}
				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable rethrownThrowable) {
				if (nodeOpen) {
					this.lexicalBuffer.endNode(standAloneAnnotation, true);
				}

				throw rethrownThrowable;
			}
		}
	}

	public final void parseObfuscateReferencesType() {
		ObfuscateReferencesType obfuscateReferencesType = new ObfuscateReferencesType(160);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(obfuscateReferencesType);

		try {
			switch (this.nextToken.kind) {
				case 43:
					ZKMScriptToken noneToken = this.consumeParameter(43);
					this.lexicalBuffer.endNode(obfuscateReferencesType, true);
					nodeOpen = false;
					obfuscateReferencesType.setText(noneToken.image);
					return;
				case 61:
					ZKMScriptToken normalToken = this.consumeParameter(61);
					this.lexicalBuffer.endNode(obfuscateReferencesType, true);
					nodeOpen = false;
					obfuscateReferencesType.setText(normalToken.image);
					return;
				default:
					this.la1Generation[124] = this.generationCounter;
					this.consumeParameter(-1);
					throw new ParsingException();
			}
		} finally {
			if (nodeOpen) {
				this.lexicalBuffer.endNode(obfuscateReferencesType, true);
			}
		}
	}

	public final void parseAutoReflectionPackage() {
		AutoReflectionPackage autoReflectionPackage = new AutoReflectionPackage(132);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(autoReflectionPackage);

		try {
			try {
				ZKMScriptToken autoReflectionPackageToken = this.consumeParameter(151);
				this.consumeParameter(15);
				this.parseStringLiteral();
				this.lexicalBuffer.endNode(autoReflectionPackage, true);
				nodeOpen = false;
				autoReflectionPackage.setParameterName(autoReflectionPackageToken.image);
			} catch (Throwable throwable) {
				if (nodeOpen) {
					this.lexicalBuffer.discardNode(autoReflectionPackage);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			}
		} catch (Throwable rethrownThrowable) {
			if (nodeOpen) {
				this.lexicalBuffer.endNode(autoReflectionPackage, true);
			}

			throw rethrownThrowable;
		}
	}

	public final void parseMemberSpecifier() {
		MemberSpecifier memberSpecifier = new MemberSpecifier(152);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(memberSpecifier);

		try {
			if (this.matchesMemberSpecifierComplexAnnotationSpecifier(Integer.MAX_VALUE)) {
				this.parseMemberSpecifierComplexAnnotationSpecifier();
			}

			while (this.matchesMemberSpecifierModifierList(Integer.MAX_VALUE)) {
				switch (this.nextToken.kind) {
					case 25:
						this.parseNegatedMemberSpecifierModifier();
						break;
					case 40:
					case 50:
					case 52:
					case 57:
					case 58:
					case 59:
					case 67:
					case 68:
					case 72:
					case 75:
					case 77:
					case 82:
					case 84:
					case 99:
						this.parseMemberSpecifierModifier();
						break;
					default:
						this.la1Generation[114] = this.generationCounter;
						this.consumeParameter(-1);
						throw new ParsingException();
				}
			}

			if (this.matchesBasicMethodSignature(Integer.MAX_VALUE)) {
				this.parseBasicMethodSignature();
			} else {
				switch (this.nextToken.kind) {
					case 23:
					case 25:
					case 28:
					case 37:
					case 39:
					case 40:
					case 42:
					case 43:
					case 45:
					case 46:
					case 47:
					case 48:
					case 51:
					case 52:
					case 56:
					case 60:
					case 61:
					case 63:
					case 64:
					case 69:
					case 71:
					case 74:
					case 77:
					case 80:
					case 81:
					case 85:
					case 86:
					case 87:
					case 90:
					case 213:
					case 214:
						this.parseFieldSignature();
						break;
					default:
						this.la1Generation[115] = this.generationCounter;
						this.consumeParameter(-1);
						throw new ParsingException();
				}
			}

			this.lexicalBuffer.endNode(memberSpecifier, true);
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (true) {
					this.lexicalBuffer.discardNode(memberSpecifier);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}
				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable rethrownThrowable) {
				if (nodeOpen) {
					this.lexicalBuffer.endNode(memberSpecifier, true);
				}

				throw rethrownThrowable;
			}
		}
	}

	public final void parsePlusSignatureClasses() {
		PlusSignatureClasses plusSignatureClasses = new PlusSignatureClasses(156);
		this.lexicalBuffer.beginNode(plusSignatureClasses);

		try {
			this.consumeParameter(125);
		} finally {
			this.lexicalBuffer.endNode(plusSignatureClasses, true);
		}
	}

	public final void parseOrFileFilterComponent() {
		OrFileFilterComponent orFileFilterComponent = new OrFileFilterComponent(11);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(orFileFilterComponent);

		try {
			this.consumeParameter(34);
			switch (this.nextToken.kind) {
				case 25:
					this.parseNegatedFileFilterComponent();
					break;
				case 210:
					this.parseFileFilterComponent();
					break;
				default:
					this.la1Generation[15] = this.generationCounter;
					this.consumeParameter(-1);
					throw new ParsingException();
			}

			this.lexicalBuffer.endNode(orFileFilterComponent, true);
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (true) {
					this.lexicalBuffer.discardNode(orFileFilterComponent);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}
				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable rethrownThrowable) {
				if (nodeOpen) {
					this.lexicalBuffer.endNode(orFileFilterComponent, true);
				}

				throw rethrownThrowable;
			}
		}
	}

	public final void parseMethodParameterChangesType() {
		MethodParameterChangesType methodParameterChangesType = new MethodParameterChangesType(159);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(methodParameterChangesType);

		try {
			switch (this.nextToken.kind) {
				case 43:
					ZKMScriptToken noneToken = this.consumeParameter(43);
					this.lexicalBuffer.endNode(methodParameterChangesType, true);
					nodeOpen = false;
					methodParameterChangesType.setText(noneToken.image);
					return;
				case 61:
					ZKMScriptToken normalToken = this.consumeParameter(61);
					this.lexicalBuffer.endNode(methodParameterChangesType, true);
					nodeOpen = false;
					methodParameterChangesType.setText(normalToken.image);
					return;
				case 63:
					ZKMScriptToken randomToken = this.consumeParameter(63);
					this.lexicalBuffer.endNode(methodParameterChangesType, true);
					nodeOpen = false;
					methodParameterChangesType.setText(randomToken.image);
					return;
				case 102:
					ZKMScriptToken flowObfuscateToken = this.consumeParameter(102);
					this.lexicalBuffer.endNode(methodParameterChangesType, true);
					nodeOpen = false;
					methodParameterChangesType.setText(flowObfuscateToken.image);
					return;
				default:
					this.la1Generation[123] = this.generationCounter;
					this.consumeParameter(-1);
					throw new ParsingException();
			}
		} finally {
			if (nodeOpen) {
				this.lexicalBuffer.endNode(methodParameterChangesType, true);
			}
		}
	}

	public final void parseSkipArchivePath() {
		SkipArchivePath skipArchivePath = new SkipArchivePath(4);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(skipArchivePath);

		try {
			this.consumeParameter(16);
			this.parseStringLiteral();
			this.lexicalBuffer.endNode(skipArchivePath, true);
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (true) {
					this.lexicalBuffer.discardNode(skipArchivePath);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}
				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable rethrownThrowable) {
				if (nodeOpen) {
					this.lexicalBuffer.endNode(skipArchivePath, true);
				}

				throw rethrownThrowable;
			}
		}
	}

	public final void parseUniqueMethodNamesParameter() {
		UniqueMethodNamesParameter uniqueMethodNamesParameter = new UniqueMethodNamesParameter(122);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(uniqueMethodNamesParameter);

		try {
			try {
				ZKMScriptToken uniqueMethodNamesToken = this.consumeParameter(122);
				this.consumeParameter(15);
				this.parseBoolean();
				this.lexicalBuffer.endNode(uniqueMethodNamesParameter, true);
				nodeOpen = false;
				uniqueMethodNamesParameter.setParameterName(uniqueMethodNamesToken.image);
			} catch (Throwable throwable) {
				if (nodeOpen) {
					this.lexicalBuffer.discardNode(uniqueMethodNamesParameter);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			}
		} catch (Throwable rethrownThrowable) {
			if (nodeOpen) {
				this.lexicalBuffer.endNode(uniqueMethodNamesParameter, true);
			}

			throw rethrownThrowable;
		}
	}

	public final void parseAutoReflectionParameter() {
		AutoReflectionParameter autoReflectionParameter = new AutoReflectionParameter(137);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(autoReflectionParameter);

		try {
			try {
				ZKMScriptToken autoReflectionHandlingToken = this.consumeParameter(156);
				this.consumeParameter(15);
				this.parseAutoReflectionType();
				this.lexicalBuffer.endNode(autoReflectionParameter, true);
				nodeOpen = false;
				autoReflectionParameter.setParameterName(autoReflectionHandlingToken.image);
			} catch (Throwable throwable) {
				if (nodeOpen) {
					this.lexicalBuffer.discardNode(autoReflectionParameter);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			}
		} catch (Throwable rethrownThrowable) {
			if (nodeOpen) {
				this.lexicalBuffer.endNode(autoReflectionParameter, true);
			}

			throw rethrownThrowable;
		}
	}

	public final void parseObfuscateParametersType() {
		ObfuscateParametersType obfuscateParametersType = new ObfuscateParametersType(163);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(obfuscateParametersType);

		try {
			switch (this.nextToken.kind) {
				case 43:
					ZKMScriptToken noneToken = this.consumeParameter(43);
					this.lexicalBuffer.endNode(obfuscateParametersType, true);
					nodeOpen = false;
					obfuscateParametersType.setText(noneToken.image);
					return;
				case 61:
					ZKMScriptToken normalToken = this.consumeParameter(61);
					this.lexicalBuffer.endNode(obfuscateParametersType, true);
					nodeOpen = false;
					obfuscateParametersType.setText(normalToken.image);
					return;
				default:
					this.la1Generation[127] = this.generationCounter;
					this.consumeParameter(-1);
					throw new ParsingException();
			}
		} finally {
			if (nodeOpen) {
				this.lexicalBuffer.endNode(obfuscateParametersType, true);
			}
		}
	}

	public final void parseIntegerEncryptionType() {
		IntegerEncryptionType integerEncryptionType = new IntegerEncryptionType(165);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(integerEncryptionType);

		try {
			switch (this.nextToken.kind) {
				case 43:
					ZKMScriptToken noneToken = this.consumeParameter(43);
					this.lexicalBuffer.endNode(integerEncryptionType, true);
					nodeOpen = false;
					integerEncryptionType.setText(noneToken.image);
					return;
				case 61:
					ZKMScriptToken normalToken = this.consumeParameter(61);
					this.lexicalBuffer.endNode(integerEncryptionType, true);
					nodeOpen = false;
					integerEncryptionType.setText(normalToken.image);
					return;
				case 90:
					ZKMScriptToken aggressiveToken = this.consumeParameter(90);
					this.lexicalBuffer.endNode(integerEncryptionType, true);
					nodeOpen = false;
					integerEncryptionType.setText(aggressiveToken.image);
					return;
				default:
					this.la1Generation[129] = this.generationCounter;
					this.consumeParameter(-1);
					throw new ParsingException();
			}
		} finally {
			if (nodeOpen) {
				this.lexicalBuffer.endNode(integerEncryptionType, true);
			}
		}
	}

	public final void parseComplexMethodSpecifier() {
		ComplexMethodSpecifier complexMethodSpecifier = new ComplexMethodSpecifier(209);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(complexMethodSpecifier);

		try {
			switch (this.nextToken.kind) {
				case 23:
				case 37:
				case 39:
				case 40:
				case 42:
				case 43:
				case 45:
				case 46:
				case 47:
				case 48:
				case 51:
				case 52:
				case 54:
				case 56:
				case 60:
				case 61:
				case 63:
				case 64:
				case 69:
				case 71:
				case 73:
				case 74:
				case 77:
				case 80:
				case 81:
				case 85:
				case 86:
				case 87:
				case 90:
				case 213:
				case 214:
					this.parseMethodName();
					break;
				case 25:
				case 28:
					this.parseBracketedMethodSpecifier();
					break;
				default:
					this.la1Generation[173] = this.generationCounter;
					this.consumeParameter(-1);
					throw new ParsingException();
			}

			this.lexicalBuffer.endNode(complexMethodSpecifier, true);
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (true) {
					this.lexicalBuffer.discardNode(complexMethodSpecifier);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}
				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable rethrownThrowable) {
				if (nodeOpen) {
					this.lexicalBuffer.endNode(complexMethodSpecifier, true);
				}

				throw rethrownThrowable;
			}
		}
	}

	public final void parseAndFieldSpecifier() {
		AndFieldSpecifier andFieldSpecifier = new AndFieldSpecifier(203);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(andFieldSpecifier);

		try {
			this.parseComplexFieldSpecifier();

			while (true) {
				switch (this.nextToken.kind) {
					case 35:
						this.consumeParameter(35);
						this.parseComplexFieldSpecifier();
						break;
					default:
						this.la1Generation[164] = this.generationCounter;
						this.lexicalBuffer.endNode(andFieldSpecifier, true);
						return;
				}
			}
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (true) {
					this.lexicalBuffer.discardNode(andFieldSpecifier);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}
				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable rethrownThrowable) {
				if (nodeOpen) {
					this.lexicalBuffer.endNode(andFieldSpecifier, true);
				}

				throw rethrownThrowable;
			}
		}
	}

	public final void parseDeleteDebugExtensionAttributesParameter() {
		DeleteDebugExtensionAttributesParameter deleteDebugExtensionAttributesParameter = new DeleteDebugExtensionAttributesParameter(93);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(deleteDebugExtensionAttributesParameter);

		try {
			try {
				ZKMScriptToken deleteDebugExtensionAttributesToken = this.consumeParameter(196);
				this.consumeParameter(15);
				this.parseBoolean();
				this.lexicalBuffer.endNode(deleteDebugExtensionAttributesParameter, true);
				nodeOpen = false;
				deleteDebugExtensionAttributesParameter.setParameterName(deleteDebugExtensionAttributesToken.image);
			} catch (Throwable throwable) {
				if (nodeOpen) {
					this.lexicalBuffer.discardNode(deleteDebugExtensionAttributesParameter);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			}
		} catch (Throwable rethrownThrowable) {
			if (nodeOpen) {
				this.lexicalBuffer.endNode(deleteDebugExtensionAttributesParameter, true);
			}

			throw rethrownThrowable;
		}
	}

	public final void parseParameterComplexAnnotationSpecifier() {
		ParameterComplexAnnotationSpecifier parameterComplexAnnotationSpecifier = new ParameterComplexAnnotationSpecifier(174);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(parameterComplexAnnotationSpecifier);

		try {
			this.parseComplexAnnotationSpecifier();
			this.lexicalBuffer.endNode(parameterComplexAnnotationSpecifier, true);
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (true) {
					this.lexicalBuffer.discardNode(parameterComplexAnnotationSpecifier);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}
				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable rethrownThrowable) {
				if (nodeOpen) {
					this.lexicalBuffer.endNode(parameterComplexAnnotationSpecifier, true);
				}

				throw rethrownThrowable;
			}
		}
	}

	public final void parseNegatedMemberSpecifierModifier() {
		NegatedMemberSpecifierModifier negatedMemberSpecifierModifier = new NegatedMemberSpecifierModifier(145);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(negatedMemberSpecifierModifier);

		try {
			this.consumeParameter(25);
			this.parseMemberModifierHelper();
			this.lexicalBuffer.endNode(negatedMemberSpecifierModifier, true);
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (true) {
					this.lexicalBuffer.discardNode(negatedMemberSpecifierModifier);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}
				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable propagatedThrowable) {
				if (nodeOpen) {
					this.lexicalBuffer.endNode(negatedMemberSpecifierModifier, true);
				}

				throw propagatedThrowable;
			}
		}
	}

	public final void parseLinkMethodSignature() {
		LinkMethodSignature linkMethodSignature = new LinkMethodSignature(207);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(linkMethodSignature);

		try {
			this.parseLinkMethodName();
			this.consumeParameter(28);
			switch (this.nextToken.kind) {
				case 23:
				case 25:
				case 27:
				case 28:
				case 32:
				case 37:
				case 39:
				case 40:
				case 42:
				case 43:
				case 45:
				case 46:
				case 47:
				case 48:
				case 51:
				case 52:
				case 56:
				case 60:
				case 61:
				case 63:
				case 64:
				case 69:
				case 71:
				case 74:
				case 77:
				case 80:
				case 81:
				case 85:
				case 86:
				case 87:
				case 90:
				case 213:
					this.parseMethodArguments();
					break;
				default:
					this.la1Generation[171] = this.generationCounter;
			}

			this.consumeParameter(29);
			this.lexicalBuffer.endNode(linkMethodSignature, true);
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (true) {
					this.lexicalBuffer.discardNode(linkMethodSignature);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}
				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable propagatedThrowable) {
				if (nodeOpen) {
					this.lexicalBuffer.endNode(linkMethodSignature, true);
				}

				throw propagatedThrowable;
			}
		}
	}

	public final void parseDeleteEmptyDirectoriesParameter() {
		DeleteEmptyDirectoriesParameter deleteEmptyDirectories = new DeleteEmptyDirectoriesParameter(61);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(deleteEmptyDirectories);

		try {
			try {
				ZKMScriptToken parameterNameToken = this.consumeParameter(157);
				this.consumeParameter(15);
				this.parseBoolean();
				this.lexicalBuffer.endNode(deleteEmptyDirectories, true);
				nodeOpen = false;
				deleteEmptyDirectories.setParameterName(parameterNameToken.image);
			} catch (Throwable throwable) {
				if (nodeOpen) {
					this.lexicalBuffer.discardNode(deleteEmptyDirectories);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			}
		} catch (Throwable propagatedThrowable) {
			if (nodeOpen) {
				this.lexicalBuffer.endNode(deleteEmptyDirectories, true);
			}

			throw propagatedThrowable;
		}
	}

	private boolean literalPackageComponentLoopLookahead() {
		return this.packageNameComponentLookahead();
	}

	public final void parseKeepInnerClassesParameter() {
		KeepInnerClassesParameter keepInnerClassInfo = new KeepInnerClassesParameter(112);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(keepInnerClassInfo);

		try {
			try {
				ZKMScriptToken parameterNameToken = this.consumeParameter(133);
				this.consumeParameter(15);
				this.parseBooleanOrIfNameNotObfucated();
				this.lexicalBuffer.endNode(keepInnerClassInfo, true);
				nodeOpen = false;
				keepInnerClassInfo.setParameterName(parameterNameToken.image);
			} catch (Throwable throwable) {
				if (nodeOpen) {
					this.lexicalBuffer.discardNode(keepInnerClassInfo);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			}
		} catch (Throwable propagatedThrowable) {
			if (nodeOpen) {
				this.lexicalBuffer.endNode(keepInnerClassInfo, true);
			}

			throw propagatedThrowable;
		}
	}

	private boolean annotationNameAggressiveKeywordLookahead() {
		return this.scanToken(90);
	}

	public final void parseNegatedClassModifier() {
		NegatedClassModifier negatedClassModifier = new NegatedClassModifier(141);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(negatedClassModifier);

		try {
			this.consumeParameter(25);
			switch (this.nextToken.kind) {
				case 40:
					ZKMScriptToken enumModifierToken = this.consumeParameter(40);
					this.lexicalBuffer.endNode(negatedClassModifier, true);
					nodeOpen = false;
					negatedClassModifier.setText(enumModifierToken.image);
					return;
				case 50:
					ZKMScriptToken finalModifierToken = this.consumeParameter(50);
					this.lexicalBuffer.endNode(negatedClassModifier, true);
					nodeOpen = false;
					negatedClassModifier.setText(finalModifierToken.image);
					return;
				case 57:
					ZKMScriptToken publicModifierToken = this.consumeParameter(57);
					this.lexicalBuffer.endNode(negatedClassModifier, true);
					nodeOpen = false;
					negatedClassModifier.setText(publicModifierToken.image);
					return;
				case 67:
					ZKMScriptToken packageModifierToken = this.consumeParameter(67);
					this.lexicalBuffer.endNode(negatedClassModifier, true);
					nodeOpen = false;
					negatedClassModifier.setText(packageModifierToken.image);
					return;
				case 72:
					ZKMScriptToken abstractModifierToken = this.consumeParameter(72);
					this.lexicalBuffer.endNode(negatedClassModifier, true);
					nodeOpen = false;
					negatedClassModifier.setText(abstractModifierToken.image);
					return;
				case 77:
					ZKMScriptToken syntheticModifierToken = this.consumeParameter(77);
					this.lexicalBuffer.endNode(negatedClassModifier, true);
					nodeOpen = false;
					negatedClassModifier.setText(syntheticModifierToken.image);
					return;
				case 83:
					ZKMScriptToken interfaceModifierToken = this.consumeParameter(83);
					this.lexicalBuffer.endNode(negatedClassModifier, true);
					nodeOpen = false;
					negatedClassModifier.setText(interfaceModifierToken.image);
					return;
				case 87:
					ZKMScriptToken annotationModifierToken = this.consumeParameter(87);
					this.lexicalBuffer.endNode(negatedClassModifier, true);
					nodeOpen = false;
					negatedClassModifier.setText(annotationModifierToken.image);
					return;
				default:
					this.la1Generation[106] = this.generationCounter;
					this.consumeParameter(-1);
					throw new ParsingException();
			}
		} finally {
			if (nodeOpen) {
				this.lexicalBuffer.endNode(negatedClassModifier, true);
			}
		}
	}

	private boolean annotationNameObfuscateKeywordLookahead() {
		return this.scanToken(81);
	}

	private ZKMScriptToken consumeParameter(int kind) {
		ZKMScriptToken previousToken = this.token;
		this.token = this.nextToken;
		this.nextToken = this.token.next != null ? this.nextToken.next : (this.nextToken.next = this.tokenSource.getNextToken());
		if (this.token.kind != kind) {
			this.nextToken = this.token;
			this.token = previousToken;
			this.errorTokenKind = kind;
			throw this.generateParseException();
		} else {
			this.generationCounter++;
			if (++this.lookaheadGcCounter > 100) {
				this.lookaheadGcCounter = 0;

				for (int i = 0; i < this.lookaheadRecords.length; i++) {
					for (LookaheadRecord lookaheadRecord = this.lookaheadRecords[i]; lookaheadRecord != null; lookaheadRecord = lookaheadRecord.next) {
						if (lookaheadRecord.generation < this.generationCounter) {
							lookaheadRecord.first = null;
						}
					}
				}
			}

			return this.token;
		}
	}

	public final void parseSaveAll() {
		SaveAllStatement saveAll = new SaveAllStatement(57);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(saveAll);

		try {
			try {
				ZKMScriptToken startToken = this.consumeParameter(65);
				this.statementKind = 65;

				while (true) {
					switch (this.nextToken.kind) {
						case 120:
						case 129:
						case 132:
						case 157:
							this.parseSaveAllParameter();
							break;
						default:
							this.la1Generation[73] = this.generationCounter;

							while (true) {
								this.parseStringLiteral();
								switch (this.nextToken.kind) {
									case 210:
										break;
									default:
										this.la1Generation[74] = this.generationCounter;
										this.consumeParameter(19);
										this.lexicalBuffer.endNode(saveAll, true);
										nodeOpen = false;
										saveAll.setLineNumber(startToken.beginLine);
										return;
								}
							}
					}
				}
			} catch (Throwable throwable) {
				if (nodeOpen) {
					this.lexicalBuffer.discardNode(saveAll);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			}
		} catch (Throwable propagatedThrowable) {
			if (nodeOpen) {
				this.lexicalBuffer.endNode(saveAll, true);
			}

			throw propagatedThrowable;
		}
	}

	private boolean moduleNameComponentNormalKeywordLookahead() {
		return this.scanToken(61);
	}

	public final AbstractScriptNode parseSingleObfuscateReferencesIncludeInput() {
		SingleObfuscateReferencesIncludeInput singleObfuscateReferencesIncludeInput = new SingleObfuscateReferencesIncludeInput(51);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(singleObfuscateReferencesIncludeInput);

		try {
			switch (this.nextToken.kind) {
				case 14:
					this.consumeParameter(14);
					break;
				default:
					this.la1Generation[66] = this.generationCounter;
			}

			switch (this.nextToken.kind) {
				case 180:
					this.obfuscateReferencesInclude();
					break;
				default:
					this.la1Generation[67] = this.generationCounter;
			}

			this.consumeParameter(0);
			this.lexicalBuffer.endNode(singleObfuscateReferencesIncludeInput, true);
			nodeOpen = false;
			return singleObfuscateReferencesIncludeInput;
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (nodeOpen) {
					this.lexicalBuffer.discardNode(singleObfuscateReferencesIncludeInput);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable propagatedThrowable) {
				if (nodeOpen) {
					this.lexicalBuffer.endNode(singleObfuscateReferencesIncludeInput, true);
				}

				throw propagatedThrowable;
			}
		}
	}

	public final void parseCollapsePackages() {
		CollapsePackages collapsePackages = new CollapsePackages(116);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(collapsePackages);

		try {
			try {
				ZKMScriptToken parameterNameToken = this.consumeParameter(185);
				this.consumeParameter(15);
				this.parseStringLiteral();
				collapsePackages.setParameterName(parameterNameToken.image);
				this.lexicalBuffer.endNode(collapsePackages, true);
				nodeOpen = false;
				collapsePackages.setParameterName(parameterNameToken.image);
			} catch (Throwable throwable) {
				if (nodeOpen) {
					this.lexicalBuffer.discardNode(collapsePackages);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			}
		} catch (Throwable propagatedThrowable) {
			if (nodeOpen) {
				this.lexicalBuffer.endNode(collapsePackages, true);
			}

			throw propagatedThrowable;
		}
	}

	public final void parseAndMemberSpecifier() {
		AndMemberSpecifier andMemberSpecifier = new AndMemberSpecifier(151);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(andMemberSpecifier);

		try {
			this.parseComplexMemberSpecifier();

			while (true) {
				switch (this.nextToken.kind) {
					case 35:
						this.consumeParameter(35);
						this.parseComplexMemberSpecifier();
						break;
					default:
						this.la1Generation[113] = this.generationCounter;
						this.lexicalBuffer.endNode(andMemberSpecifier, true);
						return;
				}
			}
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (true) {
					this.lexicalBuffer.discardNode(andMemberSpecifier);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}
				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable propagatedThrowable) {
				if (nodeOpen) {
					this.lexicalBuffer.endNode(andMemberSpecifier, true);
				}

				throw propagatedThrowable;
			}
		}
	}

	public final AbstractScriptNode parseAccessedByReflectionExclude() {
		AccessedByReflectionExcludeStatement accessedByReflectionExclude = new AccessedByReflectionExcludeStatement(34);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(accessedByReflectionExclude);

		try {
			ZKMScriptToken startToken = this.consumeParameter(182);
			this.statementKind = 182;
			this.parseRenameFilterParameter();

			while (true) {
				switch (this.nextToken.kind) {
					case 38:
						this.consumeParameter(38);
						this.parseRenameFilterParameter();
						break;
					default:
						this.la1Generation[41] = this.generationCounter;
						this.consumeParameter(19);
						this.lexicalBuffer.endNode(accessedByReflectionExclude, true);
						nodeOpen = false;
						accessedByReflectionExclude.setLineNumber(startToken.beginLine);
						return accessedByReflectionExclude;
				}
			}
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (nodeOpen) {
					this.lexicalBuffer.discardNode(accessedByReflectionExclude);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable propagatedThrowable) {
				if (nodeOpen) {
					this.lexicalBuffer.endNode(accessedByReflectionExclude, true);
				}

				throw propagatedThrowable;
			}
		}
	}

	private boolean annotationNameRandomizeKeywordLookahead() {
		return this.scanToken(85);
	}

	public final void parseGrouping() {
		Grouping grouping = new Grouping(42);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(grouping);

		try {
			this.parseRenameFilterParameter();

			while (true) {
				switch (this.nextToken.kind) {
					case 38:
						this.consumeParameter(38);
						this.parseRenameFilterParameter();
						break;
					default:
						this.la1Generation[53] = this.generationCounter;
						this.lexicalBuffer.endNode(grouping, true);
						return;
				}
			}
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (true) {
					this.lexicalBuffer.discardNode(grouping);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}
				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable propagatedThrowable) {
				if (nodeOpen) {
					this.lexicalBuffer.endNode(grouping, true);
				}

				throw propagatedThrowable;
			}
		}
	}

	public final AbstractScriptNode removeMethodCallsInclude() {
		RemoveMethodCallsIncludeStatement removeMethodCallsInclude = new RemoveMethodCallsIncludeStatement(35);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(removeMethodCallsInclude);

		try {
			ZKMScriptToken startToken = this.consumeParameter(165);
			this.statementKind = 165;
			switch (this.nextToken.kind) {
				case 92:
					this.parseContainedInClause();
					break;
				default:
					this.la1Generation[42] = this.generationCounter;
			}

			this.parseRenameFilterParameter();

			while (true) {
				switch (this.nextToken.kind) {
					case 38:
						this.consumeParameter(38);
						switch (this.nextToken.kind) {
							case 92:
								this.parseContainedInClause();
								break;
							default:
								this.la1Generation[44] = this.generationCounter;
						}

						this.parseRenameFilterParameter();
						break;
					default:
						this.la1Generation[43] = this.generationCounter;
						this.consumeParameter(19);
						this.lexicalBuffer.endNode(removeMethodCallsInclude, true);
						nodeOpen = false;
						removeMethodCallsInclude.setLineNumber(startToken.beginLine);
						return removeMethodCallsInclude;
				}
			}
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (nodeOpen) {
					this.lexicalBuffer.discardNode(removeMethodCallsInclude);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable propagatedThrowable) {
				if (nodeOpen) {
					this.lexicalBuffer.endNode(removeMethodCallsInclude, true);
				}

				throw propagatedThrowable;
			}
		}
	}

	private boolean linkPackageComponentHeavyKeywordLookahead() {
		return this.scanToken(48);
	}

	private boolean matchesMemberSpecifierComplexAnnotationSpecifier(int lookaheadDepth) {
		this.lookaheadRemaining = lookaheadDepth;
		this.lastPos = this.scanPos = this.token;

		boolean matched;
		try {
			return !this.memberSpecifierAnnotationLookahead();
		} catch (LookaheadSuccess ignored) {
			matched = true;
		} finally {
			this.saveLookaheadState(19, lookaheadDepth);
		}

		return matched;
	}

	public final void parseBooleanOrIfNameNotObfucated() {
		BooleanOrIfNameNotObfucated booleanOrIfNameNotObfuscated = new BooleanOrIfNameNotObfucated(223);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(booleanOrIfNameNotObfuscated);

		try {
			switch (this.nextToken.kind) {
				case 41:
					ZKMScriptToken trueToken = this.consumeParameter(41);
					this.lexicalBuffer.endNode(booleanOrIfNameNotObfuscated, true);
					nodeOpen = false;
					booleanOrIfNameNotObfuscated.setText(trueToken.image);
					return;
				case 49:
					ZKMScriptToken falseToken = this.consumeParameter(49);
					this.lexicalBuffer.endNode(booleanOrIfNameNotObfuscated, true);
					nodeOpen = false;
					booleanOrIfNameNotObfuscated.setText(falseToken.image);
					return;
				case 137:
					ZKMScriptToken ifNameNotObfuscatedToken = this.consumeParameter(137);
					this.lexicalBuffer.endNode(booleanOrIfNameNotObfuscated, true);
					nodeOpen = false;
					booleanOrIfNameNotObfuscated.setText(ifNameNotObfuscatedToken.image);
					return;
				default:
					this.la1Generation[185] = this.generationCounter;
					this.consumeParameter(-1);
					throw new ParsingException();
			}
		} finally {
			if (nodeOpen) {
				this.lexicalBuffer.endNode(booleanOrIfNameNotObfuscated, true);
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
			int errorTokenIndex = 0;

			ZKMScriptToken tokenCursor;
			for (tokenCursor = this.token; tokenCursor != null && tokenCursor != this.scanPos; tokenCursor = tokenCursor.next) {
				errorTokenIndex++;
			}

			if (tokenCursor != null) {
				this.addErrorToken(kind, errorTokenIndex);
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

	private boolean matchesClassComplexAnnotationSpecifierLookahead(int lookaheadDepth) {
		this.lookaheadRemaining = lookaheadDepth;
		this.lastPos = this.scanPos = this.token;

		boolean matched;
		try {
			return !this.matchesClassComplexAnnotationSpecifierExpansion();
		} catch (LookaheadSuccess ignored) {
			matched = true;
		} finally {
			this.saveLookaheadState(4, lookaheadDepth);
		}

		return matched;
	}

	private boolean matchesMemberModifierListAlt11(int lookaheadDepth) {
		this.lookaheadRemaining = lookaheadDepth;
		this.lastPos = this.scanPos = this.token;

		boolean matched;
		try {
			return !this.matchesMemberModifierListExpansionAlt11();
		} catch (LookaheadSuccess ignored) {
			matched = true;
		} finally {
			this.saveLookaheadState(10, lookaheadDepth);
		}

		return matched;
	}

	private boolean matchesNegatableClassModifierLookahead(int lookaheadDepth) {
		this.lookaheadRemaining = lookaheadDepth;
		this.lastPos = this.scanPos = this.token;

		boolean matched;
		try {
			return !this.matchesClassModifierListExpansion();
		} catch (LookaheadSuccess ignored) {
			matched = true;
		} finally {
			this.saveLookaheadState(5, lookaheadDepth);
		}

		return matched;
	}

	private boolean matchesDotLinkLiteralPackageComponent(int lookaheadDepth) {
		this.lookaheadRemaining = lookaheadDepth;
		this.lastPos = this.scanPos = this.token;

		boolean matched;
		try {
			return !this.linkPackageComponentLoopLookahead();
		} catch (LookaheadSuccess ignored) {
			matched = true;
		} finally {
			this.saveLookaheadState(25, lookaheadDepth);
		}

		return matched;
	}

	private boolean matchesJarQualifierLookahead(int lookaheadDepth) {
		this.lookaheadRemaining = lookaheadDepth;
		this.lastPos = this.scanPos = this.token;

		boolean matched;
		try {
			return !this.matchesJarQualifierExpansion();
		} catch (LookaheadSuccess ignored) {
			matched = true;
		} finally {
			this.saveLookaheadState(6, lookaheadDepth);
		}

		return matched;
	}

	private boolean matchesMemberComplexAnnotationSpecifierAlt10(int lookaheadDepth) {
		this.lookaheadRemaining = lookaheadDepth;
		this.lastPos = this.scanPos = this.token;

		boolean matched;
		try {
			return !this.matchesMemberComplexAnnotationSpecifierExpansionAlt10();
		} catch (LookaheadSuccess ignored) {
			matched = true;
		} finally {
			this.saveLookaheadState(9, lookaheadDepth);
		}

		return matched;
	}

	private boolean matchesModuleNameComponentBeforeDot(int lookaheadDepth) {
		this.lookaheadRemaining = lookaheadDepth;
		this.lastPos = this.scanPos = this.token;

		boolean matched;
		try {
			return !this.moduleNameComponentLoopLookahead();
		} catch (LookaheadSuccess ignored) {
			matched = true;
		} finally {
			this.saveLookaheadState(23, lookaheadDepth);
		}

		return matched;
	}

	private boolean matchesComplexFieldSpecifier(int lookaheadDepth) {
		this.lookaheadRemaining = lookaheadDepth;
		this.lastPos = this.scanPos = this.token;

		boolean matched;
		try {
			return !this.complexFieldSpecifierLookahead();
		} catch (LookaheadSuccess ignored) {
			matched = true;
		} finally {
			this.saveLookaheadState(26, lookaheadDepth);
		}

		return matched;
	}

	private boolean matchesPackageNameInAnnotation(int lookaheadDepth) {
		this.lookaheadRemaining = lookaheadDepth;
		this.lastPos = this.scanPos = this.token;

		boolean matched;
		try {
			return !this.annotationPackageNameLookahead();
		} catch (LookaheadSuccess ignored) {
			matched = true;
		} finally {
			this.saveLookaheadState(22, lookaheadDepth);
		}

		return matched;
	}

	private boolean matchesMethodSignatureAlt12(int lookaheadDepth) {
		this.lookaheadRemaining = lookaheadDepth;
		this.lastPos = this.scanPos = this.token;

		boolean matched;
		try {
			return !this.matchesMethodSignatureExpansionAlt12();
		} catch (LookaheadSuccess ignored) {
			matched = true;
		} finally {
			this.saveLookaheadState(11, lookaheadDepth);
		}

		return matched;
	}

	private boolean matchesJarQualifierBeforeLinkedClassName(int lookaheadDepth) {
		this.lookaheadRemaining = lookaheadDepth;
		this.lastPos = this.scanPos = this.token;

		boolean matched;
		try {
			return !this.matchesJarQualifierBeforeLinkedClassNameExpansion();
		} catch (LookaheadSuccess ignored) {
			matched = true;
		} finally {
			this.saveLookaheadState(13, lookaheadDepth);
		}

		return matched;
	}

	private boolean matchesComplexPackageSpecifierAlt8(int lookaheadDepth) {
		this.lookaheadRemaining = lookaheadDepth;
		this.lastPos = this.scanPos = this.token;

		boolean matched;
		try {
			return !this.matchesComplexPackageSpecifierExpansionAlt8();
		} catch (LookaheadSuccess ignored) {
			matched = true;
		} finally {
			this.saveLookaheadState(7, lookaheadDepth);
		}

		return matched;
	}

	private boolean matchesReferencingAnnotation(int lookaheadDepth) {
		this.lookaheadRemaining = lookaheadDepth;
		this.lastPos = this.scanPos = this.token;

		boolean matched;
		try {
			return !this.matchesReferencingAnnotationExpansion();
		} catch (LookaheadSuccess ignored) {
			matched = true;
		} finally {
			this.saveLookaheadState(17, lookaheadDepth);
		}

		return matched;
	}

	private boolean matchesNegatableMemberModifierLookahead(int lookaheadDepth) {
		this.lookaheadRemaining = lookaheadDepth;
		this.lastPos = this.scanPos = this.token;

		boolean matched;
		try {
			return !this.matchesMemberModifierListExpansionAlt3();
		} catch (LookaheadSuccess ignored) {
			matched = true;
		} finally {
			this.saveLookaheadState(2, lookaheadDepth);
		}

		return matched;
	}

	private boolean matchesComplexModuleSpecifier(int lookaheadDepth) {
		this.lookaheadRemaining = lookaheadDepth;
		ZKMScriptToken startToken = this.token;
		this.scanPos = startToken;
		this.lastPos = startToken;

		boolean matched;
		try {
			return !this.matchesComplexModuleSpecifierExpansion();
		} catch (LookaheadSuccess ignored) {
			matched = true;
		} finally {
			this.saveLookaheadState(14, lookaheadDepth);
		}

		return matched;
	}

	private boolean matchesComplexPackageSpecifier(int lookaheadDepth) {
		this.lookaheadRemaining = lookaheadDepth;
		this.lastPos = this.scanPos = this.token;

		boolean matched;
		try {
			return !this.matchesComplexPackageSpecifierExpansion();
		} catch (LookaheadSuccess ignored) {
			matched = true;
		} finally {
			this.saveLookaheadState(15, lookaheadDepth);
		}

		return matched;
	}

	private boolean matchesComplexPackageSpecifierLookahead(int lookaheadDepth) {
		this.lookaheadRemaining = lookaheadDepth;
		this.lastPos = this.scanPos = this.token;

		boolean matched;
		try {
			return !this.matchesComplexPackageSpecifierExpansionAlt1();
		} catch (LookaheadSuccess ignored) {
			matched = true;
		} finally {
			this.saveLookaheadState(0, lookaheadDepth);
		}

		return matched;
	}

	private boolean matchesMemberComplexAnnotationSpecifierLookahead(int lookaheadDepth) {
		this.lookaheadRemaining = lookaheadDepth;
		this.lastPos = this.scanPos = this.token;

		boolean matched;
		try {
			return !this.matchesMemberComplexAnnotationSpecifierExpansionAlt2();
		} catch (LookaheadSuccess ignored) {
			matched = true;
		} finally {
			this.saveLookaheadState(1, lookaheadDepth);
		}

		return matched;
	}

	private boolean matchesMethodSignatureLookahead(int lookaheadDepth) {
		this.lookaheadRemaining = lookaheadDepth;
		this.lastPos = this.scanPos = this.token;

		boolean matched;
		try {
			return !this.matchesMethodSignatureExpansionAlt4();
		} catch (LookaheadSuccess ignored) {
			matched = true;
		} finally {
			this.saveLookaheadState(3, lookaheadDepth);
		}

		return matched;
	}

	private boolean matchesLinkMethodSignature(int lookaheadDepth) {
		this.lookaheadRemaining = lookaheadDepth;
		this.lastPos = this.scanPos = this.token;

		boolean matched;
		try {
			return !this.matchesLinkMethodSignatureExpansion();
		} catch (LookaheadSuccess ignored) {
			matched = true;
		} finally {
			this.saveLookaheadState(12, lookaheadDepth);
		}

		return matched;
	}

	private boolean matchesContainingClause(int lookaheadDepth) {
		this.lookaheadRemaining = lookaheadDepth;
		this.lastPos = this.scanPos = this.token;

		boolean matched;
		try {
			return !this.matchesContainingClauseExpansion();
		} catch (LookaheadSuccess ignored) {
			matched = true;
		} finally {
			this.saveLookaheadState(8, lookaheadDepth);
		}

		return matched;
	}

	private boolean matchesPackageNameInQualifiedClassName(int lookaheadDepth) {
		this.lookaheadRemaining = lookaheadDepth;
		this.lastPos = this.scanPos = this.token;

		boolean matched;
		try {
			return !this.qualifiedClassNamePackageLookahead();
		} catch (LookaheadSuccess ignored) {
			matched = true;
		} finally {
			this.saveLookaheadState(27, lookaheadDepth);
		}

		return matched;
	}

	private boolean matchesMemberSpecifierModifierList(int lookaheadDepth) {
		this.lookaheadRemaining = lookaheadDepth;
		this.lastPos = this.scanPos = this.token;

		boolean matched;
		try {
			return !this.memberSpecifierModifierLoopLookahead();
		} catch (LookaheadSuccess ignored) {
			matched = true;
		} finally {
			this.saveLookaheadState(20, lookaheadDepth);
		}

		return matched;
	}

	private boolean matchesNextLiteralPackageComponent(int lookaheadDepth) {
		this.lookaheadRemaining = lookaheadDepth;
		this.lastPos = this.scanPos = this.token;

		boolean matched;
		try {
			return !this.literalPackageComponentLoopLookahead();
		} catch (LookaheadSuccess ignored) {
			matched = true;
		} finally {
			this.saveLookaheadState(24, lookaheadDepth);
		}

		return matched;
	}

	private boolean matchesBracketedMemberSpecifier(int lookaheadDepth) {
		this.lookaheadRemaining = lookaheadDepth;
		this.lastPos = this.scanPos = this.token;

		boolean matched;
		try {
			return !this.matchesBracketedMemberSpecifierExpansion();
		} catch (LookaheadSuccess ignored) {
			matched = true;
		} finally {
			this.saveLookaheadState(18, lookaheadDepth);
		}

		return matched;
	}

	public final AbstractScriptNode parseDefaultExclude() {
		DefaultExcludeStatement defaultExclude = new DefaultExcludeStatement(44);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(defaultExclude);

		try {
			ZKMScriptToken startToken = this.consumeParameter(64);
			this.statementKind = 64;
			this.parseRenameFilterParameter();

			while (true) {
				switch (this.nextToken.kind) {
					case 38:
						this.consumeParameter(38);
						this.parseRenameFilterParameter();
						break;
					default:
						this.la1Generation[56] = this.generationCounter;
						this.consumeParameter(19);
						this.lexicalBuffer.endNode(defaultExclude, true);
						nodeOpen = false;
						defaultExclude.setLineNumber(startToken.beginLine);
						return defaultExclude;
				}
			}
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (nodeOpen) {
					this.lexicalBuffer.discardNode(defaultExclude);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable propagatedThrowable) {
				if (nodeOpen) {
					this.lexicalBuffer.endNode(defaultExclude, true);
				}

				throw propagatedThrowable;
			}
		}
	}

	public final void parseResetExclusions() {
		ResetExclusionsStatement resetExclusions = new ResetExclusionsStatement(64);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(resetExclusions);

		try {
			ZKMScriptToken startToken = this.consumeParameter(111);
			this.statementKind = 111;
			this.consumeParameter(19);
			this.lexicalBuffer.endNode(resetExclusions, true);
			nodeOpen = false;
			resetExclusions.setLineNumber(startToken.beginLine);
		} catch (Throwable throwable) {
			if (nodeOpen) {
				this.lexicalBuffer.endNode(resetExclusions, true);
			}

			throw throwable;
		}
	}

	private boolean annotationNameLightKeywordLookahead() {
		return this.scanToken(47);
	}

	private boolean matchesMemberModifierListExpansionAlt3() {
		ZKMScriptToken savedScanPos = this.scanPos;
		if (this.scanToken(25)) {
			this.scanPos = savedScanPos;
		}

		return this.memberModifierListLookahead();
	}

	private boolean linkPackageComponentSyntheticKeywordLookahead() {
		return this.scanToken(77);
	}

	private boolean fieldNameLightKeywordLookahead() {
		return this.scanToken(47);
	}

	private boolean annotationSpecifierLookahead() {
		if (this.scanToken(27)) {
			return true;
		} else {
			ZKMScriptToken savedScanPos = this.scanPos;
			if (this.matchesOneOrMoreDottedNameParts()) {
				this.scanPos = savedScanPos;
			}

			return this.matchesMemberSpecifierStart();
		}
	}

	private boolean matchesContainingClauseExpansion() {
		return this.scanToken(88) ? true : this.scanToken(30);
	}

	private boolean linkPackageComponentObfuscateKeywordLookahead() {
		return this.scanToken(81);
	}

	private boolean fieldNameHeavyKeywordLookahead() {
		return this.scanToken(48);
	}

	private boolean moduleNameComponentExecuteKeywordLookahead() {
		return this.scanToken(69);
	}

	private boolean memberModifierPackageKeywordLookahead() {
		return this.scanToken(67);
	}

	private boolean moduleNameComponentObfuscateKeywordLookahead() {
		return this.scanToken(81);
	}

	private boolean linkPackageComponentBridgeKeywordLookahead() {
		return this.scanToken(52);
	}

	private boolean fieldNameSearchKeywordLookahead() {
		return this.scanToken(56);
	}

	private boolean packageComponentSyntheticKeywordLookahead() {
		return this.scanToken(77) ? true : this.scanToken(21);
	}

	private boolean annotationSpecifierDelegatingLookahead() {
		return this.annotationSpecifierLookahead();
	}

	private boolean memberModifierSyntheticKeywordLookahead() {
		return this.scanToken(77);
	}

	private boolean matchesMemberComplexAnnotationSpecifierExpansionAlt2() {
		return this.startsComplexAnnotationSpecifier();
	}

	private boolean packageComponentObfuscateKeywordLookahead() {
		return this.scanToken(81) ? true : this.scanToken(21);
	}

	private boolean moduleNameComponentAggressiveKeywordLookahead() {
		return this.scanToken(90);
	}

	private boolean matchesComplexPackageSpecifierExpansionAlt8() {
		return this.matchesComplexPackageSpecifierStart();
	}

	private boolean literalPackageComponentListLookahead() {
		return this.matchesPackageNameComponentList();
	}

	private boolean packageComponentGroupingsKeywordLookahead() {
		return this.scanToken(86) ? true : this.scanToken(21);
	}

	private boolean memberSpecifierAnnotationLookahead() {
		return this.startsComplexAnnotationSpecifier();
	}

	private boolean memberModifierListLookahead() {
		ZKMScriptToken savedScanPos = this.scanPos;
		if (this.memberModifierPublicKeywordLookahead()) {
			this.scanPos = savedScanPos;
			if (this.memberModifierProtectedKeywordLookahead()) {
				this.scanPos = savedScanPos;
				if (this.memberModifierPackageKeywordLookahead()) {
					this.scanPos = savedScanPos;
					if (this.memberModifierListPrivateKeywordLookahead()) {
						this.scanPos = savedScanPos;
						if (this.nextIsFinalInMemberModifierSet()) {
							this.scanPos = savedScanPos;
							if (this.memberModifierAbstractKeywordLookahead()) {
								this.scanPos = savedScanPos;
								if (this.memberModifierListStaticKeywordLookahead()) {
									this.scanPos = savedScanPos;
									if (this.memberModifierVolatileKeywordLookahead()) {
										this.scanPos = savedScanPos;
										if (this.memberModifierTransientKeywordLookahead()) {
											this.scanPos = savedScanPos;
											if (this.memberModifierListSynchronizedKeywordLookahead()) {
												this.scanPos = savedScanPos;
												if (this.memberModifierNativeKeywordLookahead()) {
													this.scanPos = savedScanPos;
													if (this.memberModifierSyntheticKeywordLookahead()) {
														this.scanPos = savedScanPos;
														if (this.memberModifierNextIsEnum()) {
															this.scanPos = savedScanPos;
															if (this.memberModifierBridgeKeywordLookahead()) {
																return true;
															}
														}
													}
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}

		return false;
	}

	private boolean matchesComplexPackageSpecifierExpansionAlt1() {
		return this.matchesComplexPackageSpecifierStart();
	}

	private boolean fieldNameWildcardNameLookahead() {
		return this.scanToken(214);
	}

	private boolean complexFieldSpecifierClasspathKeywordLookahead() {
		return this.scanToken(80);
	}

	private boolean memberModifierListStaticKeywordLookahead() {
		return this.scanToken(58);
	}

	private boolean linkPackageComponentEnhancedKeywordLookahead() {
		return this.scanToken(74);
	}

	private boolean memberSpecifierModifierLoopLookahead() {
		ZKMScriptToken savedScanPos = this.scanPos;
		if (this.scanToken(25)) {
			this.scanPos = savedScanPos;
		}

		return this.memberSpecifierModifierKeywordLookahead();
	}

	private boolean packageNameNextIsEnhancedOrDot() {
		return this.scanToken(74) ? true : this.scanToken(21);
	}

	private boolean nextIsLinkedClassNameWithSuffix() {
		return this.scanToken(216);
	}

	private boolean matchesComplexModuleSpecifierStart() {
		ZKMScriptToken savedScanPos;
		do {
			savedScanPos = this.scanPos;
		} while (!this.matchesModuleNameComponentOrDot());

		this.scanPos = savedScanPos;
		return this.startsModuleNameComponent() ? true : this.scanToken(22);
	}

	private boolean linkPackageComponentNormalKeywordLookahead() {
		return this.scanToken(61);
	}

	private boolean methodSignatureClasspathKeywordLookahead() {
		return this.scanToken(80);
	}

	private boolean memberModifierListAnnotationKeywordLookahead() {
		return this.scanToken(87);
	}

	private boolean methodSignatureAnnotationKeywordLookahead() {
		return this.scanToken(87);
	}

	private boolean methodSignatureEnumKeywordLookahead() {
		return this.scanToken(40);
	}

	private boolean moduleNameComponentLoopLookahead() {
		return this.startsModuleNameComponent() ? true : this.scanToken(21);
	}

	private boolean packageNameNextIsAsciiOrDot() {
		return this.scanToken(51) ? true : this.scanToken(21);
	}

	private boolean packageNameNextIsEnumOrDot() {
		return this.scanToken(40) ? true : this.scanToken(21);
	}

	private boolean packageNameNextIsNoneOrDot() {
		return this.scanToken(43) ? true : this.scanToken(21);
	}

	private boolean matchesClassComplexAnnotationSpecifierExpansion() {
		return this.startsComplexAnnotationSpecifier();
	}

	private boolean memberModifierListPrivateKeywordLookahead() {
		return this.scanToken(68);
	}

	private boolean matchesComplexPackageSpecifierStart() {
		ZKMScriptToken savedScanPos = this.scanPos;
		if (this.bracketedPackageSpecifierLookahead()) {
			this.scanPos = savedScanPos;
			if (this.literalPackageComponentListLookahead()) {
				return true;
			}
		}

		savedScanPos = this.scanPos;
		if (this.nextIsDotInComplexPackageSpecifier()) {
			this.scanPos = savedScanPos;
		}

		savedScanPos = this.scanPos;
		if (this.complexPackageSpecifierRecursiveMarker()) {
			this.scanPos = savedScanPos;
		}

		return false;
	}

	private boolean matchesLinkMethodSignatureStart() {
		ZKMScriptToken savedScanPos = this.scanPos;
		if (this.nextIsLinkedMethodName()) {
			this.scanPos = savedScanPos;
			if (this.linkMethodSignatureNextIsLinkToken()) {
				return true;
			}
		}

		return false;
	}

	private boolean matchesComplexFieldSpecifierPrefix() {
		ZKMScriptToken savedScanPos = this.scanPos;
		if (this.complexFieldSpecifierParenLookahead()) {
			this.scanPos = savedScanPos;
			if (this.matchesMemberSpecifierStartAlt()) {
				return true;
			}
		}

		return false;
	}

	private boolean complexPackageSpecifierParenElementLookahead() {
		ZKMScriptToken savedScanPos = this.scanPos;
		if (this.nextIsBangInPackageSpecifier()) {
			this.scanPos = savedScanPos;
		}

		if (this.scanToken(28)) {
			return true;
		} else if (this.matchesAndOrOperand()) {
			return true;
		} else {
			do {
				savedScanPos = this.scanPos;
			} while (!this.nextIsOrOrInPackageSpecifier());

			this.scanPos = savedScanPos;
			return this.scanToken(29);
		}
	}

	private boolean memberModifierListSynchronizedKeywordLookahead() {
		return this.scanToken(99);
	}

	private boolean complexFieldSpecifierAndChainLookahead() {
		if (this.matchesComplexFieldSpecifierPrefix()) {
			return true;
		} else {
			ZKMScriptToken savedScanPos;
			do {
				savedScanPos = this.scanPos;
			} while (!this.nextIsAndAndThenComplexFieldSpecifier());

			this.scanPos = savedScanPos;
			return false;
		}
	}

	private boolean linkPackageComponentWildcardNameLookahead() {
		return this.scanToken(214);
	}

	private boolean basicMethodSignatureLookahead() {
		return this.matchesMethodSignatureStart() ? true : this.scanToken(28);
	}

	private boolean matchesMethodSignatureStart() {
		ZKMScriptToken savedScanPos = this.scanPos;
		if (this.bracketedMethodSpecifierLookahead()) {
			this.scanPos = savedScanPos;
			if (this.methodNameLookahead()) {
				return true;
			}
		}

		return false;
	}

	private boolean memberSpecifierStartSyntheticKeywordLookahead() {
		return this.scanToken(77);
	}

	private boolean complexFieldSpecifierGroupingsKeywordLookahead() {
		return this.scanToken(86);
	}

	private boolean memberSpecifierStartClasspathKeywordLookahead() {
		return this.scanToken(80);
	}

	private boolean complexFieldSpecifierParenLookahead() {
		return this.matchesComplexFieldSpecifierParenExpr();
	}

	private boolean packageNameNextIsNameOrDot() {
		return this.scanToken(213) ? true : this.scanToken(21);
	}

	private boolean memberModifierListInterfaceKeywordLookahead() {
		return this.scanToken(83);
	}

	private boolean packageNameNextIsPrintOrDot() {
		return this.scanToken(46) ? true : this.scanToken(21);
	}

	private boolean moduleNameComponentLightKeywordLookahead() {
		return this.scanToken(47);
	}

	private boolean nextIsDotInComplexPackageSpecifier() {
		return this.scanToken(21);
	}

	private boolean complexFieldSpecifierObfuscateKeywordLookahead() {
		return this.scanToken(81);
	}

	private boolean complexFieldSpecifierKeepKeywordLookahead() {
		return this.scanToken(42);
	}

	public final void parseResetFixedClasses() {
		ResetFixedClassesStatement resetFixedClasses = new ResetFixedClassesStatement(74);
		boolean commitPending = true;
		this.lexicalBuffer.beginNode(resetFixedClasses);

		try {
			ZKMScriptToken keywordToken = this.consumeParameter(126);
			this.statementKind = 126;
			this.consumeParameter(19);
			this.lexicalBuffer.endNode(resetFixedClasses, true);
			commitPending = false;
			resetFixedClasses.setLineNumber(keywordToken.beginLine);
		} catch (Throwable parseFailure) {
			if (commitPending) {
				this.lexicalBuffer.endNode(resetFixedClasses, true);
			}

			throw parseFailure;
		}
	}

	private boolean jarQualifierPackageSpecifierLookahead() {
		return this.matchesComplexPackageSpecifierStart();
	}

	public final AbstractScriptNode integerEncryptionExclude() {
		IntegerEncryptionExcludeStatement integerEncryptionExclude = new IntegerEncryptionExcludeStatement(22);
		boolean commitPending = true;
		this.lexicalBuffer.beginNode(integerEncryptionExclude);

		try {
			ZKMScriptToken keywordToken = this.consumeParameter(164);
			this.statementKind = 164;
			this.parseRenameFilterParameter();

			while (true) {
				switch (this.nextToken.kind) {
					case 38:
						this.consumeParameter(38);
						this.parseRenameFilterParameter();
						break;
					default:
						this.la1Generation[24] = this.generationCounter;
						this.consumeParameter(19);
						this.lexicalBuffer.endNode(integerEncryptionExclude, true);
						commitPending = false;
						integerEncryptionExclude.setLineNumber(keywordToken.beginLine);
						return integerEncryptionExclude;
				}
			}
		} catch (Throwable parseFailure) {
			Throwable throwable = parseFailure;

			try {
				if (commitPending) {
					this.lexicalBuffer.discardNode(integerEncryptionExclude);
					commitPending = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable propagatedFailure) {
				if (commitPending) {
					this.lexicalBuffer.endNode(integerEncryptionExclude, true);
				}

				throw propagatedFailure;
			}
		}
	}

	private boolean annotationMemberSpecifierLookahead() {
		ZKMScriptToken savedScanPos = this.scanPos;
		if (this.matchesOneOrMorePackageNameComponents()) {
			this.scanPos = savedScanPos;
		}

		return this.matchesMemberSpecifierStart();
	}

	public final void parseResetTrimExclusions() {
		ResetTrimExclusionsStatement resetTrimExclusions = new ResetTrimExclusionsStatement(66);
		boolean commitPending = true;
		this.lexicalBuffer.beginNode(resetTrimExclusions);

		try {
			ZKMScriptToken keywordToken = this.consumeParameter(138);
			this.statementKind = 138;
			this.consumeParameter(19);
			this.lexicalBuffer.endNode(resetTrimExclusions, true);
			commitPending = false;
			resetTrimExclusions.setLineNumber(keywordToken.beginLine);
		} catch (Throwable parseFailure) {
			if (commitPending) {
				this.lexicalBuffer.endNode(resetTrimExclusions, true);
			}

			throw parseFailure;
		}
	}

	public final AbstractScriptNode removeMethodCallsExclude() {
		RemoveMethodCallsExcludeStatement removeMethodCallsExclude = new RemoveMethodCallsExcludeStatement(36);
		boolean commitPending = true;
		this.lexicalBuffer.beginNode(removeMethodCallsExclude);

		try {
			ZKMScriptToken keywordToken = this.consumeParameter(166);
			this.statementKind = 166;
			switch (this.nextToken.kind) {
				case 92:
					this.parseContainedInClause();
					break;
				default:
					this.la1Generation[45] = this.generationCounter;
			}

			this.parseRenameFilterParameter();

			while (true) {
				switch (this.nextToken.kind) {
					case 38:
						this.consumeParameter(38);
						switch (this.nextToken.kind) {
							case 92:
								this.parseContainedInClause();
								break;
							default:
								this.la1Generation[47] = this.generationCounter;
						}

						this.parseRenameFilterParameter();
						break;
					default:
						this.la1Generation[46] = this.generationCounter;
						this.consumeParameter(19);
						this.lexicalBuffer.endNode(removeMethodCallsExclude, true);
						commitPending = false;
						removeMethodCallsExclude.setLineNumber(keywordToken.beginLine);
						return removeMethodCallsExclude;
				}
			}
		} catch (Throwable parseFailure) {
			Throwable throwable = parseFailure;

			try {
				if (commitPending) {
					this.lexicalBuffer.discardNode(removeMethodCallsExclude);
					commitPending = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable propagatedFailure) {
				if (commitPending) {
					this.lexicalBuffer.endNode(removeMethodCallsExclude, true);
				}

				throw propagatedFailure;
			}
		}
	}

	public final void parseBracketedModuleSpecifier() {
		BracketedModuleSpecifier bracketedModuleSpecifier = new BracketedModuleSpecifier(183);
		boolean commitPending = true;
		this.lexicalBuffer.beginNode(bracketedModuleSpecifier);

		try {
			switch (this.nextToken.kind) {
				case 25:
					this.consumeParameter(25);
					bracketedModuleSpecifier.setNegated();
					break;
				default:
					this.la1Generation[140] = this.generationCounter;
			}

			this.consumeParameter(28);
			this.parseAndModuleSpecifier();

			while (true) {
				switch (this.nextToken.kind) {
					case 34:
						this.consumeParameter(34);
						this.parseAndModuleSpecifier();
						break;
					default:
						this.la1Generation[141] = this.generationCounter;
						this.consumeParameter(29);
						this.lexicalBuffer.endNode(bracketedModuleSpecifier, true);
						return;
				}
			}
		} catch (Throwable parseFailure) {
			Throwable throwable = parseFailure;

			try {
				if (true) {
					this.lexicalBuffer.discardNode(bracketedModuleSpecifier);
					commitPending = false;
				} else {
					this.lexicalBuffer.pop();
				}
				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable propagatedFailure) {
				if (commitPending) {
					this.lexicalBuffer.endNode(bracketedModuleSpecifier, true);
				}

				throw propagatedFailure;
			}
		}
	}

	private boolean linkPackageComponentSearchKeywordLookahead() {
		return this.scanToken(56);
	}

	public final AbstractScriptNode groupings() {
		GroupingsStatement groupings = new GroupingsStatement(41);
		boolean commitPending = true;
		this.lexicalBuffer.beginNode(groupings);

		try {
			ZKMScriptToken keywordToken = this.consumeParameter(86);
			this.statementKind = 86;

			while (true) {
				this.consumeParameter(30);
				this.parseGrouping();
				this.consumeParameter(31);
				switch (this.nextToken.kind) {
					case 30:
						break;
					default:
						this.la1Generation[52] = this.generationCounter;
						this.consumeParameter(19);
						this.lexicalBuffer.endNode(groupings, true);
						commitPending = false;
						groupings.setLineNumber(keywordToken.beginLine);
						return groupings;
				}
			}
		} catch (Throwable parseFailure) {
			Throwable throwable = parseFailure;

			try {
				if (commitPending) {
					this.lexicalBuffer.discardNode(groupings);
					commitPending = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable propagatedFailure) {
				if (commitPending) {
					this.lexicalBuffer.endNode(groupings, true);
				}

				throw propagatedFailure;
			}
		}
	}

	public final AbstractScriptNode parseDefaultMethodParameterChangesExcludeInput() {
		DefaultMethodParameterChangesExcludeInput defaultMethodParameterChangesExcludeInput = new DefaultMethodParameterChangesExcludeInput(47);
		boolean commitPending = true;
		this.lexicalBuffer.beginNode(defaultMethodParameterChangesExcludeInput);

		try {
			switch (this.nextToken.kind) {
				case 14:
					this.consumeParameter(14);
					break;
				default:
					this.la1Generation[60] = this.generationCounter;
			}

			switch (this.nextToken.kind) {
				case 193:
					this.parseDefaultMethodParameterChangesExclude();
					break;
				default:
					this.la1Generation[61] = this.generationCounter;
			}

			this.consumeParameter(0);
			this.lexicalBuffer.endNode(defaultMethodParameterChangesExcludeInput, true);
			commitPending = false;
			return defaultMethodParameterChangesExcludeInput;
		} catch (Throwable parseFailure) {
			Throwable throwable = parseFailure;

			try {
				if (commitPending) {
					this.lexicalBuffer.discardNode(defaultMethodParameterChangesExcludeInput);
					commitPending = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable propagatedFailure) {
				if (commitPending) {
					this.lexicalBuffer.endNode(defaultMethodParameterChangesExcludeInput, true);
				}

				throw propagatedFailure;
			}
		}
	}

	public final void parseHideFieldNamesParameter() {
		HideFieldNamesParameter hideFieldNamesParameter = new HideFieldNamesParameter(129);
		boolean commitPending = true;
		this.lexicalBuffer.beginNode(hideFieldNamesParameter);

		try {
			try {
				ZKMScriptToken keywordToken = this.consumeParameter(105);
				this.consumeParameter(15);
				this.parseBoolean();
				this.lexicalBuffer.endNode(hideFieldNamesParameter, true);
				commitPending = false;
				hideFieldNamesParameter.setParameterName(keywordToken.image);
			} catch (Throwable parseFailure) {
				if (commitPending) {
					this.lexicalBuffer.discardNode(hideFieldNamesParameter);
					commitPending = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (parseFailure instanceof RuntimeException) {
					throw (RuntimeException)parseFailure;
				} else if (parseFailure instanceof ParsingException) {
					throw (ParsingException)parseFailure;
				} else {
					throw (Error)parseFailure;
				}
			}
		} catch (Throwable propagatedFailure) {
			if (commitPending) {
				this.lexicalBuffer.endNode(hideFieldNamesParameter, true);
			}

			throw propagatedFailure;
		}
	}

	private boolean matchesStandAloneAnnotation(int lookaheadDepth) {
		this.lookaheadRemaining = lookaheadDepth;
		this.lastPos = this.scanPos = this.token;

		boolean matched;
		try {
			return !this.matchesStandAloneAnnotationExpansion();
		} catch (LookaheadSuccess ignored) {
			matched = true;
		} finally {
			this.saveLookaheadState(16, lookaheadDepth);
		}

		return matched;
	}

	public final void parseAndPackageSpecifier() {
		AndPackageSpecifier andPackageSpecifier = new AndPackageSpecifier(187);
		boolean commitPending = true;
		this.lexicalBuffer.beginNode(andPackageSpecifier);

		try {
			this.parseComplexPackageSpecifier();

			while (true) {
				switch (this.nextToken.kind) {
					case 35:
						this.consumeParameter(35);
						this.parseComplexPackageSpecifier();
						break;
					default:
						this.la1Generation[148] = this.generationCounter;
						this.lexicalBuffer.endNode(andPackageSpecifier, true);
						return;
				}
			}
		} catch (Throwable parseFailure) {
			Throwable throwable = parseFailure;

			try {
				if (true) {
					this.lexicalBuffer.discardNode(andPackageSpecifier);
					commitPending = false;
				} else {
					this.lexicalBuffer.pop();
				}
				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable propagatedFailure) {
				if (commitPending) {
					this.lexicalBuffer.endNode(andPackageSpecifier, true);
				}

				throw propagatedFailure;
			}
		}
	}

	private boolean matchesComplexModuleSpecifierPrefix() {
		return this.matchesComplexModuleSpecifierStart();
	}

	public final AbstractScriptNode obfuscateReferencesInclude() {
		ObfuscateReferencesIncludeStatement obfuscateReferencesInclude = new ObfuscateReferencesIncludeStatement(31);
		boolean commitPending = true;
		this.lexicalBuffer.beginNode(obfuscateReferencesInclude);

		try {
			ZKMScriptToken keywordToken = this.consumeParameter(180);
			this.statementKind = 180;
			switch (this.nextToken.kind) {
				case 92:
					this.parseContainedInClause();
					break;
				default:
					this.la1Generation[34] = this.generationCounter;
			}

			this.parseRenameFilterParameter();

			while (true) {
				switch (this.nextToken.kind) {
					case 38:
						this.consumeParameter(38);
						switch (this.nextToken.kind) {
							case 92:
								this.parseContainedInClause();
								break;
							default:
								this.la1Generation[36] = this.generationCounter;
						}

						this.parseRenameFilterParameter();
						break;
					default:
						this.la1Generation[35] = this.generationCounter;
						this.consumeParameter(19);
						this.lexicalBuffer.endNode(obfuscateReferencesInclude, true);
						commitPending = false;
						obfuscateReferencesInclude.setLineNumber(keywordToken.beginLine);
						return obfuscateReferencesInclude;
				}
			}
		} catch (Throwable parseFailure) {
			Throwable throwable = parseFailure;

			try {
				if (commitPending) {
					this.lexicalBuffer.discardNode(obfuscateReferencesInclude);
					commitPending = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable propagatedFailure) {
				if (commitPending) {
					this.lexicalBuffer.endNode(obfuscateReferencesInclude, true);
				}

				throw propagatedFailure;
			}
		}
	}

	public final void parseIntegerLiteral() {
		IntegerLiteral integerLiteral = new IntegerLiteral(219);
		boolean commitPending = true;
		this.lexicalBuffer.beginNode(integerLiteral);

		try {
			ZKMScriptToken literalToken = this.consumeParameter(212);
			this.lexicalBuffer.endNode(integerLiteral, true);
			commitPending = false;
			integerLiteral.setText(literalToken.image);
		} catch (Throwable parseFailure) {
			if (commitPending) {
				this.lexicalBuffer.endNode(integerLiteral, true);
			}

			throw parseFailure;
		}
	}

	private boolean nextIsAndAndOrMemberSpecifierStart() {
		return this.scanToken(35) ? true : this.startsComplexAnnotationSpecifier();
	}

	public final AbstractScriptNode parseAccessedByReflection() {
		AccessedByReflectionStatement accessedByReflection = new AccessedByReflectionStatement(33);
		boolean commitPending = true;
		this.lexicalBuffer.beginNode(accessedByReflection);

		try {
			ZKMScriptToken keywordToken = this.consumeParameter(146);
			this.statementKind = 146;
			this.parseRenameFilterParameter();

			while (true) {
				switch (this.nextToken.kind) {
					case 38:
						this.consumeParameter(38);
						this.parseRenameFilterParameter();
						break;
					default:
						this.la1Generation[40] = this.generationCounter;
						this.consumeParameter(19);
						this.lexicalBuffer.endNode(accessedByReflection, true);
						commitPending = false;
						accessedByReflection.setLineNumber(keywordToken.beginLine);
						return accessedByReflection;
				}
			}
		} catch (Throwable parseFailure) {
			Throwable throwable = parseFailure;

			try {
				if (commitPending) {
					this.lexicalBuffer.discardNode(accessedByReflection);
					commitPending = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable propagatedFailure) {
				if (commitPending) {
					this.lexicalBuffer.endNode(accessedByReflection, true);
				}

				throw propagatedFailure;
			}
		}
	}

	private boolean methodSignatureDeleteKeywordLookahead() {
		return this.scanToken(60);
	}

	public final void parseSaveAllParameter() {
		SaveAllParameter saveAllParameter = new SaveAllParameter(58);
		boolean commitPending = true;
		this.lexicalBuffer.beginNode(saveAllParameter);

		try {
			switch (this.nextToken.kind) {
				case 120:
					this.parseLastModifiedTime();
					break;
				case 129:
					this.parseDeleteXMLCommentsParameter();
					break;
				case 132:
					this.parseArchiveCompressionParameter();
					break;
				case 157:
					this.parseDeleteEmptyDirectoriesParameter();
					break;
				default:
					this.la1Generation[75] = this.generationCounter;
					this.consumeParameter(-1);
					throw new ParsingException();
			}

			this.lexicalBuffer.endNode(saveAllParameter, true);
		} catch (Throwable parseFailure) {
			Throwable throwable = parseFailure;

			try {
				if (true) {
					this.lexicalBuffer.discardNode(saveAllParameter);
					commitPending = false;
				} else {
					this.lexicalBuffer.pop();
				}
				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable propagatedFailure) {
				if (commitPending) {
					this.lexicalBuffer.endNode(saveAllParameter, true);
				}

				throw propagatedFailure;
			}
		}
	}

	private boolean matchesComplexFieldSpecifierParenExpr() {
		ZKMScriptToken savedScanPos = this.scanPos;
		if (this.bracketedFieldSpecifierNextIsBang()) {
			this.scanPos = savedScanPos;
		}

		if (this.scanToken(28)) {
			return true;
		} else if (this.complexFieldSpecifierAndChainLookahead()) {
			return true;
		} else {
			do {
				savedScanPos = this.scanPos;
			} while (!this.fieldNameFilterOrElementLookahead());

			this.scanPos = savedScanPos;
			return this.scanToken(29);
		}
	}

	public final void parseKeepBalancedLocksParameter() {
		KeepBalancedLocksParameter keepBalancedLocksParameter = new KeepBalancedLocksParameter(119);
		boolean commitPending = true;
		this.lexicalBuffer.beginNode(keepBalancedLocksParameter);

		try {
			try {
				ZKMScriptToken keywordToken = this.consumeParameter(124);
				this.consumeParameter(15);
				this.parseBoolean();
				this.lexicalBuffer.endNode(keepBalancedLocksParameter, true);
				commitPending = false;
				keepBalancedLocksParameter.setParameterName(keywordToken.image);
			} catch (Throwable parseFailure) {
				if (commitPending) {
					this.lexicalBuffer.discardNode(keepBalancedLocksParameter);
					commitPending = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (parseFailure instanceof RuntimeException) {
					throw (RuntimeException)parseFailure;
				} else if (parseFailure instanceof ParsingException) {
					throw (ParsingException)parseFailure;
				} else {
					throw (Error)parseFailure;
				}
			}
		} catch (Throwable propagatedFailure) {
			if (commitPending) {
				this.lexicalBuffer.endNode(keepBalancedLocksParameter, true);
			}

			throw propagatedFailure;
		}
	}

	public final AbstractScriptNode longEncryptionExclude() {
		LongEncryptionExcludeStatement longEncryptionExclude = new LongEncryptionExcludeStatement(24);
		boolean commitPending = true;
		this.lexicalBuffer.beginNode(longEncryptionExclude);

		try {
			ZKMScriptToken keywordToken = this.consumeParameter(152);
			this.statementKind = 152;
			this.parseRenameFilterParameter();

			while (true) {
				switch (this.nextToken.kind) {
					case 38:
						this.consumeParameter(38);
						this.parseRenameFilterParameter();
						break;
					default:
						this.la1Generation[26] = this.generationCounter;
						this.consumeParameter(19);
						this.lexicalBuffer.endNode(longEncryptionExclude, true);
						commitPending = false;
						longEncryptionExclude.setLineNumber(keywordToken.beginLine);
						return longEncryptionExclude;
				}
			}
		} catch (Throwable parseFailure) {
			Throwable throwable = parseFailure;

			try {
				if (commitPending) {
					this.lexicalBuffer.discardNode(longEncryptionExclude);
					commitPending = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable propagatedFailure) {
				if (commitPending) {
					this.lexicalBuffer.endNode(longEncryptionExclude, true);
				}

				throw propagatedFailure;
			}
		}
	}

	private boolean nextIsBangInPackageSpecifier() {
		return this.scanToken(25);
	}

	public final void parseNewNamesPrefixParameter() {
		NewNamesPrefixParameter newNamesPrefixParameter = new NewNamesPrefixParameter(128);
		boolean commitPending = true;
		this.lexicalBuffer.beginNode(newNamesPrefixParameter);

		try {
			try {
				ZKMScriptToken keywordToken = this.consumeParameter(106);
				this.consumeParameter(15);
				this.parseStringLiteral();
				newNamesPrefixParameter.setParameterName(keywordToken.image);
				this.lexicalBuffer.endNode(newNamesPrefixParameter, true);
				commitPending = false;
				newNamesPrefixParameter.setParameterName(keywordToken.image);
			} catch (Throwable parseFailure) {
				if (commitPending) {
					this.lexicalBuffer.discardNode(newNamesPrefixParameter);
					commitPending = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (parseFailure instanceof RuntimeException) {
					throw (RuntimeException)parseFailure;
				} else if (parseFailure instanceof ParsingException) {
					throw (ParsingException)parseFailure;
				} else {
					throw (Error)parseFailure;
				}
			}
		} catch (Throwable propagatedFailure) {
			if (commitPending) {
				this.lexicalBuffer.endNode(newNamesPrefixParameter, true);
			}

			throw propagatedFailure;
		}
	}

	public final AbstractScriptNode removeMethodCalls() {
		RemoveMethodCallsStatement removeMethodCalls = new RemoveMethodCallsStatement(54);
		boolean commitPending = true;
		this.lexicalBuffer.beginNode(removeMethodCalls);

		try {
			ZKMScriptToken keywordToken = this.consumeParameter(127);
			this.statementKind = 127;
			this.consumeParameter(19);
			this.lexicalBuffer.endNode(removeMethodCalls, true);
			commitPending = false;
			removeMethodCalls.setLineNumber(keywordToken.beginLine);
			return removeMethodCalls;
		} catch (Throwable parseFailure) {
			if (commitPending) {
				this.lexicalBuffer.endNode(removeMethodCalls, true);
			}

			throw parseFailure;
		}
	}

	public final AbstractScriptNode parseTrim() {
		TrimStatement trim = new TrimStatement(55);
		boolean commitPending = true;
		this.lexicalBuffer.beginNode(trim);

		try {
			ZKMScriptToken keywordToken = this.consumeParameter(45);
			this.statementKind = 45;

			while (true) {
				switch (this.nextToken.kind) {
					case 163:
					case 172:
					case 177:
					case 178:
					case 179:
					case 196:
						this.parseTrimParameter();
						break;
					default:
						this.la1Generation[71] = this.generationCounter;
						this.consumeParameter(19);
						this.lexicalBuffer.endNode(trim, true);
						commitPending = false;
						trim.setLineNumber(keywordToken.beginLine);
						return trim;
				}
			}
		} catch (Throwable parseFailure) {
			Throwable throwable = parseFailure;

			try {
				if (commitPending) {
					this.lexicalBuffer.discardNode(trim);
					commitPending = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable propagatedFailure) {
				if (commitPending) {
					this.lexicalBuffer.endNode(trim, true);
				}

				throw propagatedFailure;
			}
		}
	}

	public final void parseAnnotation() {
		Annotation annotation = new Annotation(181);
		boolean commitPending = true;
		this.lexicalBuffer.beginNode(annotation);

		try {
			this.consumeParameter(27);
			if (this.matchesPackageNameInAnnotation(Integer.MAX_VALUE)) {
				this.parsePackageName();
			}

			this.parseClassName();
		} catch (Throwable parseFailure) {
			if (true) {
				this.lexicalBuffer.discardNode(annotation);
				commitPending = false;
			} else {
				this.lexicalBuffer.pop();
			}
			if (parseFailure instanceof RuntimeException) {
				throw (RuntimeException)parseFailure;
			}

			if (parseFailure instanceof ParsingException) {
				throw (ParsingException)parseFailure;
			}

			throw (Error)parseFailure;
		} finally {
			if (true) {
				this.lexicalBuffer.endNode(annotation, true);
			}
		}
	}

	private boolean nextIsInitTokenInMethodSignature() {
		return this.scanToken(54);
	}

	public final AbstractScriptNode unexclude() {
		UnexcludeStatement unexclude = new UnexcludeStatement(15);
		boolean commitPending = true;
		this.lexicalBuffer.beginNode(unexclude);

		try {
			ZKMScriptToken keywordToken = this.consumeParameter(79);
			this.statementKind = 79;
			this.parseRenameFilterParameter();

			while (true) {
				switch (this.nextToken.kind) {
					case 38:
						this.consumeParameter(38);
						this.parseRenameFilterParameter();
						break;
					default:
						this.la1Generation[17] = this.generationCounter;
						this.consumeParameter(19);
						this.lexicalBuffer.endNode(unexclude, true);
						commitPending = false;
						unexclude.setLineNumber(keywordToken.beginLine);
						return unexclude;
				}
			}
		} catch (Throwable parseFailure) {
			Throwable throwable = parseFailure;

			try {
				if (commitPending) {
					this.lexicalBuffer.discardNode(unexclude);
					commitPending = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable propagatedFailure) {
				if (commitPending) {
					this.lexicalBuffer.endNode(unexclude, true);
				}

				throw propagatedFailure;
			}
		}
	}

	private boolean matchesMemberSpecifierStart() {
		ZKMScriptToken savedScanPos = this.scanPos;
		if (this.annotationClassNameTokenIsStar()) {
			this.scanPos = savedScanPos;
			if (this.annotationClassNameTokenIsName()) {
				this.scanPos = savedScanPos;
				if (this.nextIsWildcardNameInMemberSpecifierStart()) {
					this.scanPos = savedScanPos;
					if (this.annotationClassNameTokenIsPackageInfo()) {
						this.scanPos = savedScanPos;
						if (this.nextIsOpenInClassName()) {
							this.scanPos = savedScanPos;
							if (this.annotationNameRandomizeKeywordLookahead()) {
								this.scanPos = savedScanPos;
								if (this.memberSpecifierPrintKeywordLookahead()) {
									this.scanPos = savedScanPos;
									if (this.memberSpecifierDeleteKeywordLookahead()) {
										this.scanPos = savedScanPos;
										if (this.nextIsSearchInClassNameSet()) {
											this.scanPos = savedScanPos;
											if (this.nextIsExcludeInClassName()) {
												this.scanPos = savedScanPos;
												if (this.annotationNameKeepKeywordLookahead()) {
													this.scanPos = savedScanPos;
													if (this.annotationClassNameTokenIsExecute()) {
														this.scanPos = savedScanPos;
														if (this.nextIsScrambleInClassNameSet()) {
															this.scanPos = savedScanPos;
															if (this.memberSpecifierNormalKeywordLookahead()) {
																this.scanPos = savedScanPos;
																if (this.nextIsNoneInClassNameSet()) {
																	this.scanPos = savedScanPos;
																	if (this.annotationNameLightKeywordLookahead()) {
																		this.scanPos = savedScanPos;
																		if (this.memberSpecifierHeavyKeywordLookahead()) {
																			this.scanPos = savedScanPos;
																			if (this.memberSpecifierTrimKeywordLookahead()) {
																				this.scanPos = savedScanPos;
																				if (this.nextIsAllInClassNameSet()) {
																					this.scanPos = savedScanPos;
																					if (this.annotationNameObfuscateKeywordLookahead()) {
																						this.scanPos = savedScanPos;
																						if (this.memberSpecifierStartClasspathKeywordLookahead()) {
																							this.scanPos = savedScanPos;
																							if (this.nextIsGroupingsInMemberSpecifierStart()) {
																								this.scanPos = savedScanPos;
																								if (this.annotationNameAggressiveKeywordLookahead()) {
																									this.scanPos = savedScanPos;
																									if (this.memberSpecifierStartSyntheticKeywordLookahead()) {
																										this.scanPos = savedScanPos;
																										if (this.annotationClassNameTokenIsEnum()) {
																											this.scanPos = savedScanPos;
																											if (this.nextIsAnnotationInMemberSpecifierStart()) {
																												this.scanPos = savedScanPos;
																												if (this.nextIsBridgeInClassNameSet()) {
																													this.scanPos = savedScanPos;
																													if (this.annotationClassNameTokenIsEnhanced()) {
																														this.scanPos = savedScanPos;
																														if (this.annotationClassNameTokenIsRandom()) {
																															this.scanPos = savedScanPos;
																															if (this.classNameNextIsAsciiOrDot()) {
																																return true;
																															}
																														}
																													}
																												}
																											}
																										}
																									}
																								}
																							}
																						}
																					}
																				}
																			}
																		}
																	}
																}
															}
														}
													}
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}

		return false;
	}

	public final void parseExpectedInitialHash() {
		ExpectedInitialHash expectedInitialHash = new ExpectedInitialHash(8);
		boolean commitPending = true;
		this.lexicalBuffer.beginNode(expectedInitialHash);

		try {
			try {
				ZKMScriptToken keywordToken = this.consumeParameter(153);
				this.consumeParameter(15);
				this.parseStringLiteral();
				this.lexicalBuffer.endNode(expectedInitialHash, true);
				commitPending = false;
				expectedInitialHash.setParameterName(keywordToken.image);
			} catch (Throwable parseFailure) {
				if (commitPending) {
					this.lexicalBuffer.discardNode(expectedInitialHash);
					commitPending = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (parseFailure instanceof RuntimeException) {
					throw (RuntimeException)parseFailure;
				} else if (parseFailure instanceof ParsingException) {
					throw (ParsingException)parseFailure;
				} else {
					throw (Error)parseFailure;
				}
			}
		} catch (Throwable propagatedFailure) {
			if (commitPending) {
				this.lexicalBuffer.endNode(expectedInitialHash, true);
			}

			throw propagatedFailure;
		}
	}

	private boolean matchesBasicMethodSignature(int lookaheadDepth) {
		this.lookaheadRemaining = lookaheadDepth;
		this.lastPos = this.scanPos = this.token;

		boolean matched;
		try {
			return !this.basicMethodSignatureLookahead();
		} catch (LookaheadSuccess ignored) {
			matched = true;
		} finally {
			this.saveLookaheadState(21, lookaheadDepth);
		}

		return matched;
	}

	public final void parseContainedInClause() {
		ContainedInClause containedInClause = new ContainedInClause(148);
		boolean commitPending = true;
		this.lexicalBuffer.beginNode(containedInClause);

		try {
			this.consumeParameter(92);
			this.consumeParameter(30);
			switch (this.nextToken.kind) {
				case 23:
				case 25:
				case 27:
				case 28:
				case 36:
				case 37:
				case 39:
				case 40:
				case 42:
				case 43:
				case 45:
				case 46:
				case 47:
				case 48:
				case 50:
				case 51:
				case 52:
				case 56:
				case 57:
				case 60:
				case 61:
				case 63:
				case 64:
				case 67:
				case 69:
				case 71:
				case 72:
				case 74:
				case 77:
				case 80:
				case 81:
				case 83:
				case 85:
				case 86:
				case 87:
				case 90:
				case 97:
				case 210:
				case 213:
				case 214:
				case 215:
				case 216:
					this.parseRenameFilterParameter();
					break;
				case 24:
				case 26:
				case 29:
				case 30:
				case 31:
				case 32:
				case 33:
				case 34:
				case 35:
				case 38:
				case 41:
				case 44:
				case 49:
				case 53:
				case 54:
				case 55:
				case 58:
				case 59:
				case 62:
				case 65:
				case 66:
				case 68:
				case 70:
				case 73:
				case 75:
				case 76:
				case 78:
				case 79:
				case 82:
				case 84:
				case 88:
				case 89:
				case 91:
				case 92:
				case 93:
				case 94:
				case 95:
				case 96:
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
				case 125:
				case 126:
				case 127:
				case 128:
				case 129:
				case 130:
				case 131:
				case 132:
				case 133:
				case 134:
				case 135:
				case 136:
				case 137:
				case 138:
				case 139:
				case 140:
				case 141:
				case 142:
				case 143:
				case 144:
				case 145:
				case 146:
				case 147:
				case 148:
				case 149:
				case 150:
				case 151:
				case 152:
				case 153:
				case 154:
				case 155:
				case 156:
				case 157:
				case 158:
				case 159:
				case 160:
				case 161:
				case 162:
				case 163:
				case 164:
				case 165:
				case 166:
				case 167:
				case 168:
				case 169:
				case 170:
				case 171:
				case 172:
				case 173:
				case 174:
				case 175:
				case 176:
				case 177:
				case 178:
				case 179:
				case 180:
				case 181:
				case 182:
				case 183:
				case 184:
				case 185:
				case 186:
				case 187:
				case 188:
				case 189:
				case 190:
				case 191:
				case 192:
				case 193:
				case 194:
				case 195:
				case 196:
				case 197:
				case 198:
				case 199:
				case 200:
				case 201:
				case 202:
				case 203:
				case 204:
				case 205:
				case 206:
				case 207:
				case 208:
				case 209:
				case 211:
				case 212:
				default:
					this.la1Generation[109] = this.generationCounter;
			}

			this.consumeParameter(31);
			this.lexicalBuffer.endNode(containedInClause, true);
		} catch (Throwable parseFailure) {
			Throwable throwable = parseFailure;

			try {
				if (true) {
					this.lexicalBuffer.discardNode(containedInClause);
					commitPending = false;
				} else {
					this.lexicalBuffer.pop();
				}
				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable propagatedFailure) {
				if (commitPending) {
					this.lexicalBuffer.endNode(containedInClause, true);
				}

				throw propagatedFailure;
			}
		}
	}

	public final AbstractScriptNode parseMethodParameterChangesInclude() {
		MethodParameterChangesIncludeStatement methodParameterChangesInclude = new MethodParameterChangesIncludeStatement(37);
		boolean commitPending = true;
		this.lexicalBuffer.beginNode(methodParameterChangesInclude);

		try {
			ZKMScriptToken keywordToken = this.consumeParameter(192);
			this.statementKind = 192;
			this.parseRenameFilterParameter();

			while (true) {
				switch (this.nextToken.kind) {
					case 38:
						this.consumeParameter(38);
						this.parseRenameFilterParameter();
						break;
					default:
						this.la1Generation[48] = this.generationCounter;
						this.consumeParameter(19);
						this.lexicalBuffer.endNode(methodParameterChangesInclude, true);
						commitPending = false;
						methodParameterChangesInclude.setLineNumber(keywordToken.beginLine);
						return methodParameterChangesInclude;
				}
			}
		} catch (Throwable parseFailure) {
			Throwable throwable = parseFailure;

			try {
				if (commitPending) {
					this.lexicalBuffer.discardNode(methodParameterChangesInclude);
					commitPending = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable propagatedFailure) {
				if (commitPending) {
					this.lexicalBuffer.endNode(methodParameterChangesInclude, true);
				}

				throw propagatedFailure;
			}
		}
	}

	private boolean nextIsAndAndInMethodSignature() {
		return this.scanToken(35) ? true : this.matchesMethodSignatureStart();
	}

	public final void parseAndClassSpecifier() {
		AndClassSpecifier andClassSpecifier = new AndClassSpecifier(197);
		boolean commitPending = true;
		this.lexicalBuffer.beginNode(andClassSpecifier);

		try {
			this.parseComplexClassSpecifier();

			while (true) {
				switch (this.nextToken.kind) {
					case 35:
						this.consumeParameter(35);
						this.parseComplexClassSpecifier();
						break;
					default:
						this.la1Generation[158] = this.generationCounter;
						this.lexicalBuffer.endNode(andClassSpecifier, true);
						return;
				}
			}
		} catch (Throwable parseFailure) {
			Throwable throwable = parseFailure;

			try {
				if (true) {
					this.lexicalBuffer.discardNode(andClassSpecifier);
					commitPending = false;
				} else {
					this.lexicalBuffer.pop();
				}
				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable propagatedFailure) {
				if (commitPending) {
					this.lexicalBuffer.endNode(andClassSpecifier, true);
				}

				throw propagatedFailure;
			}
		}
	}

	private boolean matchesComplexModuleSpecifierParenExpr() {
		ZKMScriptToken savedScanPos = this.scanPos;
		if (this.bracketedModuleSpecifierNextIsBang()) {
			this.scanPos = savedScanPos;
		}

		if (this.scanToken(28)) {
			return true;
		} else if (this.startsAndModuleSpecifier()) {
			return true;
		} else {
			do {
				savedScanPos = this.scanPos;
			} while (!this.nextIsOrThenAndModuleSpecifier());

			this.scanPos = savedScanPos;
			return this.scanToken(29);
		}
	}

	public final AbstractScriptNode fixedClasses() {
		FixedClassesStatement fixedClasses = new FixedClassesStatement(27);
		boolean commitPending = true;
		this.lexicalBuffer.beginNode(fixedClasses);

		try {
			ZKMScriptToken keywordToken = this.consumeParameter(98);
			this.statementKind = 98;
			this.parseRenameFilterParameter();

			while (true) {
				switch (this.nextToken.kind) {
					case 38:
						this.consumeParameter(38);
						this.parseRenameFilterParameter();
						break;
					default:
						this.la1Generation[29] = this.generationCounter;
						this.consumeParameter(19);
						this.lexicalBuffer.endNode(fixedClasses, true);
						commitPending = false;
						fixedClasses.setLineNumber(keywordToken.beginLine);
						return fixedClasses;
				}
			}
		} catch (Throwable parseFailure) {
			Throwable throwable = parseFailure;

			try {
				if (commitPending) {
					this.lexicalBuffer.discardNode(fixedClasses);
					commitPending = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable propagatedFailure) {
				if (commitPending) {
					this.lexicalBuffer.endNode(fixedClasses, true);
				}

				throw propagatedFailure;
			}
		}
	}

	public final void parseLocalVariableParameterType() {
		LocalVariableParameterType localVariableParameterType = new LocalVariableParameterType(168);
		boolean commitPending = true;
		this.lexicalBuffer.beginNode(localVariableParameterType);

		try {
			switch (this.nextToken.kind) {
				case 42:
					ZKMScriptToken keepToken = this.consumeParameter(42);
					this.lexicalBuffer.endNode(localVariableParameterType, true);
					commitPending = false;
					localVariableParameterType.setText(keepToken.image);
					return;
				case 60:
					ZKMScriptToken deleteToken = this.consumeParameter(60);
					this.lexicalBuffer.endNode(localVariableParameterType, true);
					commitPending = false;
					localVariableParameterType.setText(deleteToken.image);
					return;
				case 81:
					ZKMScriptToken obfuscateToken = this.consumeParameter(81);
					this.lexicalBuffer.endNode(localVariableParameterType, true);
					commitPending = false;
					localVariableParameterType.setText(obfuscateToken.image);
					return;
				case 184:
					ZKMScriptToken keepVisibleMethodParametersToken = this.consumeParameter(184);
					this.lexicalBuffer.endNode(localVariableParameterType, true);
					commitPending = false;
					localVariableParameterType.setText(keepVisibleMethodParametersToken.image);
					return;
				case 205:
					ZKMScriptToken keepIfNotObfuscatedToken = this.consumeParameter(205);
					this.lexicalBuffer.endNode(localVariableParameterType, true);
					commitPending = false;
					localVariableParameterType.setText(keepIfNotObfuscatedToken.image);
					return;
				case 208:
					ZKMScriptToken keepVisibleIfNotObfuscatedToken = this.consumeParameter(208);
					this.lexicalBuffer.endNode(localVariableParameterType, true);
					commitPending = false;
					localVariableParameterType.setText(keepVisibleIfNotObfuscatedToken.image);
					return;
				default:
					this.la1Generation[132] = this.generationCounter;
					this.consumeParameter(-1);
					throw new ParsingException();
			}
		} finally {
			if (commitPending) {
				this.lexicalBuffer.endNode(localVariableParameterType, true);
			}
		}
	}

	private boolean nextIsRandomizeInMemberSpecifierStart() {
		return this.scanToken(85);
	}

	public final void parseContainingClause() {
		ContainingClause containingClause = new ContainingClause(147);
		boolean commitPending = true;
		this.lexicalBuffer.beginNode(containingClause);

		try {
			this.consumeParameter(88);
			this.consumeParameter(30);
			switch (this.nextToken.kind) {
				case 23:
				case 25:
				case 27:
				case 28:
				case 37:
				case 39:
				case 40:
				case 42:
				case 43:
				case 45:
				case 46:
				case 47:
				case 48:
				case 50:
				case 51:
				case 52:
				case 54:
				case 56:
				case 57:
				case 58:
				case 59:
				case 60:
				case 61:
				case 63:
				case 64:
				case 67:
				case 68:
				case 69:
				case 71:
				case 72:
				case 73:
				case 74:
				case 75:
				case 77:
				case 80:
				case 81:
				case 82:
				case 84:
				case 85:
				case 86:
				case 87:
				case 90:
				case 99:
				case 213:
				case 214:
					this.parseComplexMemberSpecifier();
					break;
				case 24:
				case 26:
				case 29:
				case 30:
				case 31:
				case 32:
				case 33:
				case 34:
				case 35:
				case 36:
				case 38:
				case 41:
				case 44:
				case 49:
				case 53:
				case 55:
				case 62:
				case 65:
				case 66:
				case 70:
				case 76:
				case 78:
				case 79:
				case 83:
				case 88:
				case 89:
				case 91:
				case 92:
				case 93:
				case 94:
				case 95:
				case 96:
				case 97:
				case 98:
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
				case 125:
				case 126:
				case 127:
				case 128:
				case 129:
				case 130:
				case 131:
				case 132:
				case 133:
				case 134:
				case 135:
				case 136:
				case 137:
				case 138:
				case 139:
				case 140:
				case 141:
				case 142:
				case 143:
				case 144:
				case 145:
				case 146:
				case 147:
				case 148:
				case 149:
				case 150:
				case 151:
				case 152:
				case 153:
				case 154:
				case 155:
				case 156:
				case 157:
				case 158:
				case 159:
				case 160:
				case 161:
				case 162:
				case 163:
				case 164:
				case 165:
				case 166:
				case 167:
				case 168:
				case 169:
				case 170:
				case 171:
				case 172:
				case 173:
				case 174:
				case 175:
				case 176:
				case 177:
				case 178:
				case 179:
				case 180:
				case 181:
				case 182:
				case 183:
				case 184:
				case 185:
				case 186:
				case 187:
				case 188:
				case 189:
				case 190:
				case 191:
				case 192:
				case 193:
				case 194:
				case 195:
				case 196:
				case 197:
				case 198:
				case 199:
				case 200:
				case 201:
				case 202:
				case 203:
				case 204:
				case 205:
				case 206:
				case 207:
				case 208:
				case 209:
				case 210:
				case 211:
				case 212:
				default:
					this.la1Generation[108] = this.generationCounter;
			}

			this.consumeParameter(31);
			this.lexicalBuffer.endNode(containingClause, true);
		} catch (Throwable parseFailure) {
			Throwable throwable = parseFailure;

			try {
				if (true) {
					this.lexicalBuffer.discardNode(containingClause);
					commitPending = false;
				} else {
					this.lexicalBuffer.pop();
				}
				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable propagatedFailure) {
				if (commitPending) {
					this.lexicalBuffer.endNode(containingClause, true);
				}

				throw propagatedFailure;
			}
		}
	}

	public final AbstractScriptNode parseDefaultMethodParameterObfuscationExclude() {
		DefaultMethodParameterObfuscationExcludeStatement defaultMethodParameterObfuscationExclude = new DefaultMethodParameterObfuscationExcludeStatement(50);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(defaultMethodParameterObfuscationExclude);

		try {
			ZKMScriptToken keywordToken = this.consumeParameter(200);
			this.statementKind = 200;
			this.parseRenameFilterParameter();

			while (true) {
				switch (this.nextToken.kind) {
					case 38:
						this.consumeParameter(38);
						this.parseRenameFilterParameter();
						break;
					default:
						this.la1Generation[65] = this.generationCounter;
						this.consumeParameter(19);
						this.lexicalBuffer.endNode(defaultMethodParameterObfuscationExclude, true);
						nodeOpen = false;
						defaultMethodParameterObfuscationExclude.setLineNumber(keywordToken.beginLine);
						return defaultMethodParameterObfuscationExclude;
				}
			}
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (nodeOpen) {
					this.lexicalBuffer.discardNode(defaultMethodParameterObfuscationExclude);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable rethrownThrowable) {
				if (nodeOpen) {
					this.lexicalBuffer.endNode(defaultMethodParameterObfuscationExclude, true);
				}

				throw rethrownThrowable;
			}
		}
	}

	public final void parseResetIntegerEncryptionExclusions() {
		ResetIntegerEncryptionExclusionsStatement resetIntegerEncryptionExclusions = new ResetIntegerEncryptionExclusionsStatement(71);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(resetIntegerEncryptionExclusions);

		try {
			ZKMScriptToken keywordToken = this.consumeParameter(198);
			this.statementKind = 198;
			this.consumeParameter(19);
			this.lexicalBuffer.endNode(resetIntegerEncryptionExclusions, true);
			nodeOpen = false;
			resetIntegerEncryptionExclusions.setLineNumber(keywordToken.beginLine);
		} catch (Throwable rethrownThrowable) {
			if (nodeOpen) {
				this.lexicalBuffer.endNode(resetIntegerEncryptionExclusions, true);
			}

			throw rethrownThrowable;
		}
	}

	private boolean nextIsGcInModuleNameComponent() {
		return this.scanToken(36);
	}

	public final AbstractScriptNode obfuscateReferencesExclude() {
		ObfuscateReferencesExcludeStatement obfuscateReferencesExclude = new ObfuscateReferencesExcludeStatement(32);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(obfuscateReferencesExclude);

		try {
			ZKMScriptToken keywordToken = this.consumeParameter(181);
			this.statementKind = 181;
			switch (this.nextToken.kind) {
				case 92:
					this.parseContainedInClause();
					break;
				default:
					this.la1Generation[37] = this.generationCounter;
			}

			this.parseRenameFilterParameter();

			while (true) {
				switch (this.nextToken.kind) {
					case 38:
						this.consumeParameter(38);
						switch (this.nextToken.kind) {
							case 92:
								this.parseContainedInClause();
								break;
							default:
								this.la1Generation[39] = this.generationCounter;
						}

						this.parseRenameFilterParameter();
						break;
					default:
						this.la1Generation[38] = this.generationCounter;
						this.consumeParameter(19);
						this.lexicalBuffer.endNode(obfuscateReferencesExclude, true);
						nodeOpen = false;
						obfuscateReferencesExclude.setLineNumber(keywordToken.beginLine);
						return obfuscateReferencesExclude;
				}
			}
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (nodeOpen) {
					this.lexicalBuffer.discardNode(obfuscateReferencesExclude);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable rethrownThrowable) {
				if (nodeOpen) {
					this.lexicalBuffer.endNode(obfuscateReferencesExclude, true);
				}

				throw rethrownThrowable;
			}
		}
	}

	private boolean nextIsWildcardNameInMemberSpecifierStart() {
		return this.scanToken(214);
	}

	public final AbstractScriptNode parseDefaultMethodParameterObfuscationExcludeInput() {
		DefaultMethodParameterObfuscationExcludeInput defaultMethodParameterObfuscationExcludeInput = new DefaultMethodParameterObfuscationExcludeInput(49);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(defaultMethodParameterObfuscationExcludeInput);

		try {
			switch (this.nextToken.kind) {
				case 14:
					this.consumeParameter(14);
					break;
				default:
					this.la1Generation[63] = this.generationCounter;
			}

			switch (this.nextToken.kind) {
				case 200:
					this.parseDefaultMethodParameterObfuscationExclude();
					break;
				default:
					this.la1Generation[64] = this.generationCounter;
			}

			this.consumeParameter(0);
			this.lexicalBuffer.endNode(defaultMethodParameterObfuscationExcludeInput, true);
			nodeOpen = false;
			return defaultMethodParameterObfuscationExcludeInput;
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (nodeOpen) {
					this.lexicalBuffer.discardNode(defaultMethodParameterObfuscationExcludeInput);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable rethrownThrowable) {
				if (nodeOpen) {
					this.lexicalBuffer.endNode(defaultMethodParameterObfuscationExcludeInput, true);
				}

				throw rethrownThrowable;
			}
		}
	}

	public final void parseBracketedMemberSpecifier() {
		BracketedMemberSpecifier bracketedMemberSpecifier = new BracketedMemberSpecifier(150);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(bracketedMemberSpecifier);

		try {
			switch (this.nextToken.kind) {
				case 25:
					this.consumeParameter(25);
					bracketedMemberSpecifier.setNegated();
					break;
				default:
					this.la1Generation[111] = this.generationCounter;
			}

			this.consumeParameter(28);
			this.parseAndMemberSpecifier();

			while (true) {
				switch (this.nextToken.kind) {
					case 34:
						this.consumeParameter(34);
						this.parseAndMemberSpecifier();
						break;
					default:
						this.la1Generation[112] = this.generationCounter;
						this.consumeParameter(29);
						this.lexicalBuffer.endNode(bracketedMemberSpecifier, true);
						return;
				}
			}
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (true) {
					this.lexicalBuffer.discardNode(bracketedMemberSpecifier);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}
				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable rethrownThrowable) {
				if (nodeOpen) {
					this.lexicalBuffer.endNode(bracketedMemberSpecifier, true);
				}

				throw rethrownThrowable;
			}
		}
	}

	public final AbstractScriptNode parseMethodParameterObfuscationInclude() {
		MethodParameterObfuscationIncludeStatement methodParameterObfuscationInclude = new MethodParameterObfuscationIncludeStatement(39);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(methodParameterObfuscationInclude);

		try {
			ZKMScriptToken keywordToken = this.consumeParameter(199);
			this.statementKind = 199;
			this.parseRenameFilterParameter();

			while (true) {
				switch (this.nextToken.kind) {
					case 38:
						this.consumeParameter(38);
						this.parseRenameFilterParameter();
						break;
					default:
						this.la1Generation[50] = this.generationCounter;
						this.consumeParameter(19);
						this.lexicalBuffer.endNode(methodParameterObfuscationInclude, true);
						nodeOpen = false;
						methodParameterObfuscationInclude.setLineNumber(keywordToken.beginLine);
						return methodParameterObfuscationInclude;
				}
			}
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (nodeOpen) {
					this.lexicalBuffer.discardNode(methodParameterObfuscationInclude);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable rethrownThrowable) {
				if (nodeOpen) {
					this.lexicalBuffer.endNode(methodParameterObfuscationInclude, true);
				}

				throw rethrownThrowable;
			}
		}
	}

	private boolean matchesAtAnnotationMemberSpecifier() {
		return this.scanToken(27) ? true : this.annotationMemberSpecifierLookahead();
	}

	public final void parsePackageName() {
		PackageName packageName = new PackageName(191);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(packageName);

		try {
			do {
				this.parseLiteralPackageComponent();
			} while (this.matchesNextLiteralPackageComponent(Integer.MAX_VALUE));

			this.lexicalBuffer.endNode(packageName, true);
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (true) {
					this.lexicalBuffer.discardNode(packageName);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}
				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable rethrownThrowable) {
				if (nodeOpen) {
					this.lexicalBuffer.endNode(packageName, true);
				}

				throw rethrownThrowable;
			}
		}
	}

	public final void parseAndModuleSpecifier() {
		AndModuleSpecifier andModuleSpecifier = new AndModuleSpecifier(184);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(andModuleSpecifier);

		try {
			this.parseComplexModuleSpecifier();

			while (true) {
				switch (this.nextToken.kind) {
					case 35:
						this.consumeParameter(35);
						this.parseComplexModuleSpecifier();
						break;
					default:
						this.la1Generation[142] = this.generationCounter;
						this.lexicalBuffer.endNode(andModuleSpecifier, true);
						return;
				}
			}
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (true) {
					this.lexicalBuffer.discardNode(andModuleSpecifier);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}
				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable rethrownThrowable) {
				if (nodeOpen) {
					this.lexicalBuffer.endNode(andModuleSpecifier, true);
				}

				throw rethrownThrowable;
			}
		}
	}

	private boolean nextIsStarInFieldSpecifier() {
		return this.scanToken(23);
	}

	private boolean matchesJarQualifierExpansion() {
		return this.jarQualifierNextIsStringLiteralOrBang();
	}

	private boolean nextIsGroupingsInMethodSignature() {
		return this.scanToken(86);
	}

	private boolean nextIsSyntheticInModuleNameComponent() {
		return this.scanToken(77);
	}

	private boolean nextIsOpenInFieldSpecifier() {
		return this.scanToken(39);
	}

	private boolean nextIsAnnotationInMemberSpecifierStart() {
		return this.scanToken(87);
	}

	private boolean nextIsRandomizeInModuleNameComponent() {
		return this.scanToken(85);
	}

	private boolean nextIsOrOrInPackageSpecifier() {
		return this.scanToken(34) ? true : this.matchesAndOrOperand();
	}

	private boolean nextIsAnnotationInFieldSpecifier() {
		return this.scanToken(87);
	}

	private boolean nextIsKeepInLinkPackageComponent() {
		return this.scanToken(42);
	}

	private boolean matchesMemberSpecifierStartAlt() {
		return this.matchesFieldSignatureNameTokenSet();
	}

	private boolean matchesLinkPackageComponentStart() {
		ZKMScriptToken savedScanPos = this.scanPos;
		if (this.nextIsStarInLinkLiteralPackageComponent()) {
			this.scanPos = savedScanPos;
			if (this.nextIsNameInLinkLiteralPackageComponent()) {
				this.scanPos = savedScanPos;
				if (this.linkPackageComponentWildcardNameLookahead()) {
					this.scanPos = savedScanPos;
					if (this.linkPackageComponentOpenKeywordLookahead()) {
						this.scanPos = savedScanPos;
						if (this.nextIsPrintInLinkLiteralPackageComponent()) {
							this.scanPos = savedScanPos;
							if (this.nextIsDeleteInLinkLiteralPackageComponent()) {
								this.scanPos = savedScanPos;
								if (this.linkPackageComponentSearchKeywordLookahead()) {
									this.scanPos = savedScanPos;
									if (this.nextIsExcludeInLinkLiteralPackageComponent()) {
										this.scanPos = savedScanPos;
										if (this.nextIsKeepInLinkPackageComponent()) {
											this.scanPos = savedScanPos;
											if (this.nextIsExecuteInLinkLiteralPackageComponent()) {
												this.scanPos = savedScanPos;
												if (this.linkPackageComponentScrambleKeywordLookahead()) {
													this.scanPos = savedScanPos;
													if (this.linkPackageComponentNormalKeywordLookahead()) {
														this.scanPos = savedScanPos;
														if (this.nextIsNoneInLinkLiteralPackageComponent()) {
															this.scanPos = savedScanPos;
															if (this.nextIsLightInLinkLiteralPackageComponent()) {
																this.scanPos = savedScanPos;
																if (this.linkPackageComponentHeavyKeywordLookahead()) {
																	this.scanPos = savedScanPos;
																	if (this.linkPackageComponentTrimKeywordLookahead()) {
																		this.scanPos = savedScanPos;
																		if (this.linkPackageNameComponentIsAll()) {
																			this.scanPos = savedScanPos;
																			if (this.linkPackageComponentObfuscateKeywordLookahead()) {
																				this.scanPos = savedScanPos;
																				if (this.nextIsClasspathInLinkLiteralPackageComponent()) {
																					this.scanPos = savedScanPos;
																					if (this.linkPackageNameComponentIsGroupings()) {
																						this.scanPos = savedScanPos;
																						if (this.linkPackageComponentAggressiveKeywordLookahead()) {
																							this.scanPos = savedScanPos;
																							if (this.linkPackageComponentSyntheticKeywordLookahead()) {
																								this.scanPos = savedScanPos;
																								if (this.nextIsEnumInLinkLiteralPackageComponent()) {
																									this.scanPos = savedScanPos;
																									if (this.linkPackageNameComponentIsAnnotation()) {
																										this.scanPos = savedScanPos;
																										if (this.linkPackageComponentBridgeKeywordLookahead()) {
																											this.scanPos = savedScanPos;
																											if (this.linkPackageComponentEnhancedKeywordLookahead()) {
																												this.scanPos = savedScanPos;
																												if (this.nextIsRandomInLinkLiteralPackageComponent()) {
																													this.scanPos = savedScanPos;
																													if (this.nextIsAsciiInLinkPackageComponent()) {
																														return true;
																													}
																												}
																											}
																										}
																									}
																								}
																							}
																						}
																					}
																				}
																			}
																		}
																	}
																}
															}
														}
													}
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}

		return false;
	}

	private boolean annotationPackageNameLookahead() {
		return this.packageNameComponentLookahead();
	}

	private boolean nextIsAllInModuleNameComponent() {
		return this.scanToken(37);
	}

	private boolean nextIsClinitInMethodSignature() {
		return this.scanToken(73);
	}

	private boolean nextIsRandomizeOrDotInPackageName() {
		return this.scanToken(85) ? true : this.scanToken(21);
	}

	private boolean matchesMethodSignatureExpansionAlt4() {
		return this.matchesMethodSignatureStart() ? true : this.scanToken(28);
	}

	private boolean nextIsHeavyInMethodSignature() {
		return this.scanToken(48);
	}

	private boolean nextIsPackageInClassModifierSet() {
		return this.scanToken(67);
	}

	private boolean nextIsAsciiInLinkPackageComponent() {
		return this.scanToken(51);
	}

	private boolean matchesMemberModifierListExpansionAlt11() {
		ZKMScriptToken savedScanPos = this.scanPos;
		if (this.scanToken(25)) {
			this.scanPos = savedScanPos;
		}

		return this.memberModifierListLookahead();
	}

	private boolean nextIsGroupingsInMemberSpecifierStart() {
		return this.scanToken(86);
	}

	private boolean nextIsAnnotationOrDotInPackageName() {
		return this.scanToken(87) ? true : this.scanToken(21);
	}

	private boolean matchesPackageNameComponentList() {
		if (this.literalPackageComponentLookahead()) {
			return true;
		} else {
			ZKMScriptToken savedScanPos;
			do {
				savedScanPos = this.scanPos;
			} while (!this.literalPackageComponentLookahead());

			this.scanPos = savedScanPos;
			return false;
		}
	}

	private boolean nextIsTrimOrDotInPackageName() {
		return this.scanToken(45) ? true : this.scanToken(21);
	}

	private boolean nextIsSyntheticInClassModifierSet() {
		return this.scanToken(77);
	}

	private boolean nextIsNormalOrDotInPackageName() {
		return this.scanToken(61) ? true : this.scanToken(21);
	}

	private boolean matchesBracketedMemberSpecifierExpansion() {
		ZKMScriptToken savedScanPos = this.scanPos;
		if (this.scanToken(25)) {
			this.scanPos = savedScanPos;
		}

		return this.scanToken(28);
	}

	private boolean nextIsDeleteInFieldSpecifier() {
		return this.scanToken(60);
	}

	private boolean nextIsTrimInMethodSignature() {
		return this.scanToken(45);
	}

	private boolean nextIsExecuteOrDotInPackageName() {
		return this.scanToken(69) ? true : this.scanToken(21);
	}

	private boolean nextIsKeepInMethodSignature() {
		return this.scanToken(42);
	}

	private boolean nextIsLinkedMethodName() {
		return this.scanToken(217);
	}

	private boolean nextIsNormalInMethodSignature() {
		return this.scanToken(61);
	}

	private boolean nextIsOrOrInMethodSignature() {
		return this.scanToken(34) ? true : this.methodNameFilterAndChainLookahead();
	}

	private boolean moduleNameComponentTokenIsExclude() {
		return this.scanToken(64);
	}

	private boolean startsAndModuleSpecifier() {
		if (this.startsComplexModuleSpecifier()) {
			return true;
		} else {
			ZKMScriptToken savedScanPos;
			do {
				savedScanPos = this.scanPos;
			} while (!this.andModuleSpecifierLookahead());

			this.scanPos = savedScanPos;
			return false;
		}
	}

	private boolean annotationClassNameTokenIsEnhanced() {
		return this.scanToken(74);
	}

	private boolean linkPackageNameComponentIsAll() {
		return this.scanToken(37);
	}

	private boolean methodSpecifierClassNameIsLight() {
		return this.scanToken(47);
	}

	private boolean packageNameComponentNextIsWildcardNameOrDot() {
		return this.scanToken(214) ? true : this.scanToken(21);
	}

	private boolean methodSpecifierClassNameIsOpen() {
		return this.scanToken(39);
	}

	private boolean jarQualifierNextIsStringLiteralOrBang() {
		return this.jarQualifierStringLiteralLookahead() ? true : this.scanToken(25);
	}

	private boolean fieldSpecifierClassNameIsExecute() {
		return this.scanToken(69);
	}

	private boolean moduleNameComponentTokenIsASCII() {
		return this.scanToken(51);
	}

	private boolean nextIsOrThenAndModuleSpecifier() {
		return this.scanToken(34) ? true : this.startsAndModuleSpecifier();
	}

	private boolean fieldSpecifierClassNameIsName() {
		return this.scanToken(213);
	}

	private boolean matchesReferencingAnnotationExpansion() {
		if (this.matchesAtAnnotationMemberSpecifier()) {
			return true;
		} else if (this.scanToken(28)) {
			return true;
		} else {
			return this.nextIsNameInSpecifierAlternation() ? true : this.scanToken(15);
		}
	}

	private boolean linkPackageNameComponentIsGroupings() {
		return this.scanToken(86);
	}

	private boolean moduleNameComponentTokenIsBridge() {
		return this.scanToken(52);
	}

	private boolean nextIsAndAndThenComplexFieldSpecifier() {
		return this.scanToken(35) ? true : this.matchesComplexFieldSpecifierPrefix();
	}

	private boolean moduleNameComponentTokenIsNone() {
		return this.scanToken(43);
	}

	private boolean moduleNameComponentTokenIsSearch() {
		return this.scanToken(56);
	}

	private boolean methodSpecifierClassNameIsWildcardName() {
		return this.scanToken(214);
	}

	private boolean bracketedModuleSpecifierNextIsBang() {
		return this.scanToken(25);
	}

	private boolean fieldSpecifierClassNameIsScramble() {
		return this.scanToken(71);
	}

	private boolean packageNameComponentNextIsAggressiveOrDot() {
		return this.scanToken(90) ? true : this.scanToken(21);
	}

	private boolean annotationClassNameTokenIsName() {
		return this.scanToken(213);
	}

	private boolean memberModifierNextIsEnum() {
		return this.scanToken(40);
	}

	private boolean classModifierNextIsFinal() {
		return this.scanToken(50);
	}

	private boolean startsModuleNameComponent() {
		ZKMScriptToken savedScanPos = this.scanPos;
		if (this.moduleNameComponentWildcardLookahead()) {
			this.scanPos = savedScanPos;
			if (this.moduleNameComponentNameTokenLookahead()) {
				this.scanPos = savedScanPos;
				if (this.nextIsWildcardNameInModuleNameComponent()) {
					this.scanPos = savedScanPos;
					if (this.moduleNameComponentModuleNameTokenLookahead()) {
						this.scanPos = savedScanPos;
						if (this.moduleNameComponentOpenKeywordLookahead()) {
							this.scanPos = savedScanPos;
							if (this.nextIsRandomizeInModuleNameComponent()) {
								this.scanPos = savedScanPos;
								if (this.nextIsGcInModuleNameComponent()) {
									this.scanPos = savedScanPos;
									if (this.moduleNameComponentPrintKeywordLookahead()) {
										this.scanPos = savedScanPos;
										if (this.nextIsDeleteInModuleNameComponent()) {
											this.scanPos = savedScanPos;
											if (this.moduleNameComponentTokenIsSearch()) {
												this.scanPos = savedScanPos;
												if (this.moduleNameComponentTokenIsExclude()) {
													this.scanPos = savedScanPos;
													if (this.moduleNameComponentKeepKeywordLookahead()) {
														this.scanPos = savedScanPos;
														if (this.moduleNameComponentExecuteKeywordLookahead()) {
															this.scanPos = savedScanPos;
															if (this.moduleNameComponentScrambleKeywordLookahead()) {
																this.scanPos = savedScanPos;
																if (this.moduleNameComponentNormalKeywordLookahead()) {
																	this.scanPos = savedScanPos;
																	if (this.moduleNameComponentTokenIsNone()) {
																		this.scanPos = savedScanPos;
																		if (this.moduleNameComponentLightKeywordLookahead()) {
																			this.scanPos = savedScanPos;
																			if (this.moduleNameComponentHeavyKeywordLookahead()) {
																				this.scanPos = savedScanPos;
																				if (this.moduleNameComponentTrimKeywordLookahead()) {
																					this.scanPos = savedScanPos;
																					if (this.nextIsAllInModuleNameComponent()) {
																						this.scanPos = savedScanPos;
																						if (this.moduleNameComponentObfuscateKeywordLookahead()) {
																							this.scanPos = savedScanPos;
																							if (this.moduleNameComponentClasspathKeywordLookahead()) {
																								this.scanPos = savedScanPos;
																								if (this.moduleNameComponentGroupingsKeywordLookahead()) {
																									this.scanPos = savedScanPos;
																									if (this.moduleNameComponentAggressiveKeywordLookahead()) {
																										this.scanPos = savedScanPos;
																										if (this.nextIsSyntheticInModuleNameComponent()) {
																											this.scanPos = savedScanPos;
																											if (this.moduleNameComponentEnumKeywordLookahead()) {
																												this.scanPos = savedScanPos;
																												if (this.moduleNameComponentAnnotationKeywordLookahead()) {
																													this.scanPos = savedScanPos;
																													if (this.moduleNameComponentTokenIsBridge()) {
																														this.scanPos = savedScanPos;
																														if (this.nextIsEnhancedInModuleNameComponent()) {
																															this.scanPos = savedScanPos;
																															if (this.moduleNameComponentRandomKeywordLookahead()) {
																																this.scanPos = savedScanPos;
																																if (this.moduleNameComponentTokenIsASCII()) {
																																	return true;
																																}
																															}
																														}
																													}
																												}
																											}
																										}
																									}
																								}
																							}
																						}
																					}
																				}
																			}
																		}
																	}
																}
															}
														}
													}
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}

		return false;
	}

	private boolean startsBracketedAnnotationSpecifier() {
		ZKMScriptToken savedScanPos = this.scanPos;
		if (this.bracketedAnnotationSpecifierNextIsBang()) {
			this.scanPos = savedScanPos;
		}

		if (this.scanToken(28)) {
			return true;
		} else if (this.memberComplexAnnotationSpecifierLookahead()) {
			return true;
		} else {
			do {
				savedScanPos = this.scanPos;
			} while (!this.annotationValueOrElementLookahead());

			this.scanPos = savedScanPos;
			return this.scanToken(29);
		}
	}

	private boolean linkPackageComponentLoopLookahead() {
		return this.scanToken(21) ? true : this.matchesLinkPackageComponentStart();
	}

	private boolean startsBracketedModuleSpecifier() {
		return this.matchesComplexModuleSpecifierParenExpr();
	}

	public final AbstractScriptNode obfuscateFlowExclude() {
		ObfuscateFlowExcludeStatement obfuscateFlowExclude = new ObfuscateFlowExcludeStatement(16);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(obfuscateFlowExclude);

		try {
			ZKMScriptToken keywordToken = this.consumeParameter(144);
			this.statementKind = 144;
			this.parseRenameFilterParameter();

			while (true) {
				switch (this.nextToken.kind) {
					case 38:
						this.consumeParameter(38);
						this.parseRenameFilterParameter();
						break;
					default:
						this.la1Generation[18] = this.generationCounter;
						this.consumeParameter(19);
						this.lexicalBuffer.endNode(obfuscateFlowExclude, true);
						nodeOpen = false;
						obfuscateFlowExclude.setLineNumber(keywordToken.beginLine);
						return obfuscateFlowExclude;
				}
			}
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (nodeOpen) {
					this.lexicalBuffer.discardNode(obfuscateFlowExclude);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable rethrownThrowable) {
				if (nodeOpen) {
					this.lexicalBuffer.endNode(obfuscateFlowExclude, true);
				}

				throw rethrownThrowable;
			}
		}
	}

	private boolean startsComplexAnnotationSpecifier() {
		ZKMScriptToken savedScanPos = this.scanPos;
		if (this.matchesNegatableParenthesizedSpecifierList()) {
			this.scanPos = savedScanPos;
			if (this.annotationSpecifierDelegatingLookahead()) {
				return true;
			}
		}

		return false;
	}

	public final void parseBasicMethodSignature() {
		BasicMethodSignature basicMethodSignature = new BasicMethodSignature(205);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(basicMethodSignature);

		try {
			this.parseComplexMethodSpecifier();
			this.consumeParameter(28);
			switch (this.nextToken.kind) {
				case 23:
				case 25:
				case 27:
				case 28:
				case 32:
				case 37:
				case 39:
				case 40:
				case 42:
				case 43:
				case 45:
				case 46:
				case 47:
				case 48:
				case 51:
				case 52:
				case 56:
				case 60:
				case 61:
				case 63:
				case 64:
				case 69:
				case 71:
				case 74:
				case 77:
				case 80:
				case 81:
				case 85:
				case 86:
				case 87:
				case 90:
				case 213:
					this.parseMethodArguments();
					break;
				default:
					this.la1Generation[166] = this.generationCounter;
			}

			this.consumeParameter(29);
			switch (this.nextToken.kind) {
				case 62:
					this.parseThrowsClause();
					break;
				default:
					this.la1Generation[167] = this.generationCounter;
			}

			this.lexicalBuffer.endNode(basicMethodSignature, true);
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (true) {
					this.lexicalBuffer.discardNode(basicMethodSignature);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}
				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable rethrownThrowable) {
				if (nodeOpen) {
					this.lexicalBuffer.endNode(basicMethodSignature, true);
				}

				throw rethrownThrowable;
			}
		}
	}

	public final void parseLinkClassName() {
		LinkClassName linkClassName = new LinkClassName(199);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(linkClassName);

		try {
			ZKMScriptToken linkedClassNameToken = this.consumeParameter(216);
			linkClassName.setText(linkedClassNameToken.image);
			this.lexicalBuffer.endNode(linkClassName, true);
			nodeOpen = false;
			linkedClassNameToken.setDescriptionIndex(6);
		} catch (Throwable rethrownThrowable) {
			if (nodeOpen) {
				this.lexicalBuffer.endNode(linkClassName, true);
			}

			throw rethrownThrowable;
		}
	}

	private boolean annotationClassNameTokenIsEnum() {
		return this.scanToken(40);
	}

	public final AbstractScriptNode parseMethodParameterObfuscationExclude() {
		MethodParameterObfuscationExcludeStatement methodParameterObfuscationExclude = new MethodParameterObfuscationExcludeStatement(40);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(methodParameterObfuscationExclude);

		try {
			ZKMScriptToken keywordToken = this.consumeParameter(200);
			this.statementKind = 200;
			this.parseRenameFilterParameter();

			while (true) {
				switch (this.nextToken.kind) {
					case 38:
						this.consumeParameter(38);
						this.parseRenameFilterParameter();
						break;
					default:
						this.la1Generation[51] = this.generationCounter;
						this.consumeParameter(19);
						this.lexicalBuffer.endNode(methodParameterObfuscationExclude, true);
						nodeOpen = false;
						methodParameterObfuscationExclude.setLineNumber(keywordToken.beginLine);
						return methodParameterObfuscationExclude;
				}
			}
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (nodeOpen) {
					this.lexicalBuffer.discardNode(methodParameterObfuscationExclude);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable rethrownThrowable) {
				if (nodeOpen) {
					this.lexicalBuffer.endNode(methodParameterObfuscationExclude, true);
				}

				throw rethrownThrowable;
			}
		}
	}

	public final void parseNewFieldNameFileParameter() {
		NewFieldNameFileParameter newFieldNameFile2 = new NewFieldNameFileParameter(99);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(newFieldNameFile2);

		try {
			try {
				ZKMScriptToken fileNameToken = this.consumeParameter(119);
				this.consumeParameter(15);
				this.parseStringLiteral();
				this.lexicalBuffer.endNode(newFieldNameFile2, true);
				nodeOpen = false;
				newFieldNameFile2.setParameterName(fileNameToken.image);
			} catch (Throwable throwable) {
				if (nodeOpen) {
					this.lexicalBuffer.discardNode(newFieldNameFile2);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			}
		} catch (Throwable rethrownThrowable) {
			if (nodeOpen) {
				this.lexicalBuffer.endNode(newFieldNameFile2, true);
			}

			throw rethrownThrowable;
		}
	}

	private boolean annotationClassNameTokenIsExecute() {
		return this.scanToken(69);
	}

	public final AbstractScriptNode obfuscateExceptionsUnexclude() {
		ObfuscateExceptionsUnexcludeStatement obfuscateExceptionsUnexclude = new ObfuscateExceptionsUnexcludeStatement(19);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(obfuscateExceptionsUnexclude);

		try {
			ZKMScriptToken keywordToken = this.consumeParameter(186);
			this.statementKind = 186;
			this.parseRenameFilterParameter();

			while (true) {
				switch (this.nextToken.kind) {
					case 38:
						this.consumeParameter(38);
						this.parseRenameFilterParameter();
						break;
					default:
						this.la1Generation[21] = this.generationCounter;
						this.consumeParameter(19);
						this.lexicalBuffer.endNode(obfuscateExceptionsUnexclude, true);
						nodeOpen = false;
						obfuscateExceptionsUnexclude.setLineNumber(keywordToken.beginLine);
						return obfuscateExceptionsUnexclude;
				}
			}
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (nodeOpen) {
					this.lexicalBuffer.discardNode(obfuscateExceptionsUnexclude);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable rethrownThrowable) {
				if (nodeOpen) {
					this.lexicalBuffer.endNode(obfuscateExceptionsUnexclude, true);
				}

				throw rethrownThrowable;
			}
		}
	}

	private boolean linkPackageNameComponentIsAnnotation() {
		return this.scanToken(87);
	}

	public final void parseClassInitializationOrderPair() {
		ClassInitializationOrder classInitializationOrder = new ClassInitializationOrder(84);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(classInitializationOrder);

		try {
			this.parseQualifiedClassName();
			this.consumeParameter(33);
			this.parseQualifiedClassName();
			this.lexicalBuffer.endNode(classInitializationOrder, true);
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (true) {
					this.lexicalBuffer.discardNode(classInitializationOrder);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}
				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable rethrownThrowable) {
				if (nodeOpen) {
					this.lexicalBuffer.endNode(classInitializationOrder, true);
				}

				throw rethrownThrowable;
			}
		}
	}

	public final AbstractScriptNode parseDefaultTrimExclude() {
		DefaultTrimExcludeStatement defaultTrimExclude = new DefaultTrimExcludeStatement(46);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(defaultTrimExclude);

		try {
			ZKMScriptToken keywordToken = this.consumeParameter(94);
			this.statementKind = 94;
			this.parseRenameFilterParameter();

			while (true) {
				switch (this.nextToken.kind) {
					case 38:
						this.consumeParameter(38);
						this.parseRenameFilterParameter();
						break;
					default:
						this.la1Generation[59] = this.generationCounter;
						this.consumeParameter(19);
						this.lexicalBuffer.endNode(defaultTrimExclude, true);
						nodeOpen = false;
						defaultTrimExclude.setLineNumber(keywordToken.beginLine);
						return defaultTrimExclude;
				}
			}
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (nodeOpen) {
					this.lexicalBuffer.discardNode(defaultTrimExclude);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable rethrownThrowable) {
				if (nodeOpen) {
					this.lexicalBuffer.endNode(defaultTrimExclude, true);
				}

				throw rethrownThrowable;
			}
		}
	}

	private boolean matchesOneOrMorePackageNameComponents() {
		return this.matchesPackageNameComponentList();
	}

	private boolean annotationClassNameTokenIsRandom() {
		return this.scanToken(63);
	}

	private boolean fieldSpecifierClassNameIsNone() {
		return this.scanToken(43);
	}

	private boolean annotationClassNameTokenIsPackageInfo() {
		return this.scanToken(97);
	}

	private boolean startsComplexModuleSpecifier() {
		ZKMScriptToken savedScanPos = this.scanPos;
		if (this.startsBracketedModuleSpecifier()) {
			this.scanPos = savedScanPos;
			if (this.matchesComplexModuleSpecifierPrefix()) {
				return true;
			}
		}

		savedScanPos = this.scanPos;
		if (this.moduleSpecifierCaretTokenLookahead()) {
			this.scanPos = savedScanPos;
		}

		return false;
	}

	private boolean fieldSpecifierClassNameIsPrint() {
		return this.scanToken(46);
	}

	private boolean annotationClassNameTokenIsStar() {
		return this.scanToken(23);
	}

	private boolean linkMethodSignatureNextIsLinkToken() {
		return this.scanToken(53);
	}

	private boolean matchesStandAloneAnnotationExpansion() {
		if (this.matchesAnnotationOrClassName()) {
			return true;
		} else {
			ZKMScriptToken savedScanPos = this.scanPos;
			if (this.scanToken(38)) {
				this.scanPos = savedScanPos;
				if (this.scanToken(19)) {
					return true;
				}
			}

			return false;
		}
	}

	private boolean packageNameComponentNextIsRandomOrDot() {
		return this.scanToken(63) ? true : this.scanToken(21);
	}

	private boolean moduleNameComponentClasspathKeywordLookahead() {
		return this.scanToken(80);
	}

	private boolean moduleNameComponentOpenKeywordLookahead() {
		return this.scanToken(39);
	}

	private boolean nextIsExcludeInClassName() {
		return this.scanToken(64);
	}

	private boolean moduleNameComponentEnumKeywordLookahead() {
		return this.scanToken(40);
	}

	private boolean packageNameComponentNextIsClasspathOrDot() {
		return this.scanToken(80) ? true : this.scanToken(21);
	}

	private boolean memberModifierTransientKeywordLookahead() {
		return this.scanToken(84);
	}

	private boolean bracketedFieldSpecifierNextIsBang() {
		return this.scanToken(25);
	}

	private boolean classNameNextIsAsciiOrDot() {
		return this.scanToken(51) ? true : this.scanToken(21);
	}

	private boolean moduleNameComponentRandomKeywordLookahead() {
		return this.scanToken(63);
	}

	private boolean moduleNameComponentKeepKeywordLookahead() {
		return this.scanToken(42);
	}

	private boolean bracketedAnnotationSpecifierNextIsBang() {
		return this.scanToken(25);
	}

	private boolean bracketedMethodSpecifierNextIsBang() {
		return this.scanToken(25);
	}

	private boolean packageNameComponentNextIsExcludeOrDot() {
		return this.scanToken(64) ? true : this.scanToken(21);
	}

	private boolean moduleNameComponentModuleNameTokenLookahead() {
		return this.scanToken(215);
	}

	private boolean matchesJarQualifierBeforeLinkedClassNameExpansion() {
		ZKMScriptToken savedScanPos = this.scanPos;
		if (this.jarQualifierLookahead()) {
			this.scanPos = savedScanPos;
		}

		savedScanPos = this.scanPos;
		if (this.jarQualifierPackageSpecifierLookahead()) {
			this.scanPos = savedScanPos;
		}

		return this.nextIsLinkedClassNameWithSuffix();
	}

	private boolean memberModifierProtectedKeywordLookahead() {
		return this.scanToken(82);
	}

	private boolean packageNameComponentLookahead() {
		ZKMScriptToken savedScanPos = this.scanPos;
		if (this.packageNameComponentNextIsStarOrDot()) {
			this.scanPos = savedScanPos;
			if (this.packageNameNextIsNameOrDot()) {
				this.scanPos = savedScanPos;
				if (this.packageNameComponentNextIsWildcardNameOrDot()) {
					this.scanPos = savedScanPos;
					if (this.literalPackageComponentOpenThenDotLookahead()) {
						this.scanPos = savedScanPos;
						if (this.nextIsRandomizeOrDotInPackageName()) {
							this.scanPos = savedScanPos;
							if (this.literalPackageComponentGcThenDotLookahead()) {
								this.scanPos = savedScanPos;
								if (this.packageNameNextIsPrintOrDot()) {
									this.scanPos = savedScanPos;
									if (this.literalPackageComponentDeleteThenDotLookahead()) {
										this.scanPos = savedScanPos;
										if (this.literalPackageComponentSearchThenDotLookahead()) {
											this.scanPos = savedScanPos;
											if (this.packageNameComponentNextIsExcludeOrDot()) {
												this.scanPos = savedScanPos;
												if (this.packageNameComponentNextIsKeepOrDot()) {
													this.scanPos = savedScanPos;
													if (this.nextIsExecuteOrDotInPackageName()) {
														this.scanPos = savedScanPos;
														if (this.packageComponentScrambleKeywordLookahead()) {
															this.scanPos = savedScanPos;
															if (this.nextIsNormalOrDotInPackageName()) {
																this.scanPos = savedScanPos;
																if (this.packageNameNextIsNoneOrDot()) {
																	this.scanPos = savedScanPos;
																	if (this.packageNameComponentNextIsLightOrDot()) {
																		this.scanPos = savedScanPos;
																		if (this.nextIsHeavyOrDotInDottedName()) {
																			this.scanPos = savedScanPos;
																			if (this.nextIsTrimOrDotInPackageName()) {
																				this.scanPos = savedScanPos;
																				if (this.literalPackageComponentAllThenDotLookahead()) {
																					this.scanPos = savedScanPos;
																					if (this.packageComponentObfuscateKeywordLookahead()) {
																						this.scanPos = savedScanPos;
																						if (this.packageNameComponentNextIsClasspathOrDot()) {
																							this.scanPos = savedScanPos;
																							if (this.packageComponentGroupingsKeywordLookahead()) {
																								this.scanPos = savedScanPos;
																								if (this.packageNameComponentNextIsAggressiveOrDot()) {
																									this.scanPos = savedScanPos;
																									if (this.packageComponentSyntheticKeywordLookahead()) {
																										this.scanPos = savedScanPos;
																										if (this.packageNameNextIsEnumOrDot()) {
																											this.scanPos = savedScanPos;
																											if (this.nextIsAnnotationOrDotInPackageName()) {
																												this.scanPos = savedScanPos;
																												if (this.packageComponentBridgeKeywordLookahead()) {
																													this.scanPos = savedScanPos;
																													if (this.packageNameNextIsEnhancedOrDot()) {
																														this.scanPos = savedScanPos;
																														if (this.packageNameComponentNextIsRandomOrDot()) {
																															this.scanPos = savedScanPos;
																															if (this.packageNameComponentNextIsPackageOrDot()) {
																																this.scanPos = savedScanPos;
																																if (this.packageNameNextIsAsciiOrDot()) {
																																	return true;
																																}
																															}
																														}
																													}
																												}
																											}
																										}
																									}
																								}
																							}
																						}
																					}
																				}
																			}
																		}
																	}
																}
															}
														}
													}
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}

		return false;
	}

	private boolean packageNameComponentNextIsStarOrDot() {
		return this.scanToken(23) ? true : this.scanToken(21);
	}

	private boolean nextIsDeleteInLinkLiteralPackageComponent() {
		return this.scanToken(60);
	}

	private boolean moduleNameComponentHeavyKeywordLookahead() {
		return this.scanToken(48);
	}

	private boolean qualifiedClassNamePackageLookahead() {
		return this.packageNameComponentLookahead();
	}

	private boolean bracketedMethodSpecifierLookahead() {
		return this.matchesNegatableParenthesizedAndOrList();
	}

	private boolean fieldNameAggressiveKeywordLookahead() {
		return this.scanToken(90);
	}

	private boolean classModifierLookahead() {
		ZKMScriptToken savedScanPos = this.scanPos;
		if (this.classModifierPublicKeywordLookahead()) {
			this.scanPos = savedScanPos;
			if (this.nextIsPackageInClassModifierSet()) {
				this.scanPos = savedScanPos;
				if (this.classModifierNextIsFinal()) {
					this.scanPos = savedScanPos;
					if (this.memberModifierListInterfaceKeywordLookahead()) {
						this.scanPos = savedScanPos;
						if (this.classModifierAbstractKeywordLookahead()) {
							this.scanPos = savedScanPos;
							if (this.nextIsSyntheticInClassModifierSet()) {
								this.scanPos = savedScanPos;
								if (this.classModifierEnumKeywordLookahead()) {
									this.scanPos = savedScanPos;
									if (this.memberModifierListAnnotationKeywordLookahead()) {
										return true;
									}
								}
							}
						}
					}
				}
			}
		}

		return false;
	}

	private boolean packageNameComponentNextIsKeepOrDot() {
		return this.scanToken(42) ? true : this.scanToken(21);
	}

	private boolean fieldNameTrimKeywordLookahead() {
		return this.scanToken(45);
	}

	private boolean memberModifierNativeKeywordLookahead() {
		return this.scanToken(59);
	}

	private boolean nextIsOpenInClassName() {
		return this.scanToken(39);
	}

	private boolean packageNameComponentNextIsLightOrDot() {
		return this.scanToken(47) ? true : this.scanToken(21);
	}

	private boolean methodNameAggressiveKeywordLookahead() {
		return this.scanToken(90);
	}

	private boolean nextIsLightInLinkLiteralPackageComponent() {
		return this.scanToken(47);
	}

	private boolean nextIsExcludeInLinkLiteralPackageComponent() {
		return this.scanToken(64);
	}

	private boolean matchesMethodSignatureExpansionAlt12() {
		return this.matchesMethodSignatureStart() ? true : this.scanToken(28);
	}

	private boolean moduleNameComponentScrambleKeywordLookahead() {
		return this.scanToken(71);
	}

	private boolean packageNameComponentNextIsPackageOrDot() {
		return this.scanToken(67) ? true : this.scanToken(21);
	}

	private boolean memberModifierAbstractKeywordLookahead() {
		return this.scanToken(72);
	}

	private boolean classModifierAbstractKeywordLookahead() {
		return this.scanToken(72);
	}

	private boolean complexPackageSpecifierRecursiveMarker() {
		return this.scanToken(26);
	}

	private boolean nextIsPrintInLinkLiteralPackageComponent() {
		return this.scanToken(46);
	}

	private boolean fieldNameExcludeKeywordLookahead() {
		return this.scanToken(64);
	}

	private boolean nextIsNameInLinkLiteralPackageComponent() {
		return this.scanToken(213);
	}

	private boolean fieldNameRandomKeywordLookahead() {
		return this.scanToken(63);
	}

	private boolean methodNameObfuscateKeywordLookahead() {
		return this.scanToken(81);
	}

	private boolean fieldNameEnumKeywordLookahead() {
		return this.scanToken(40);
	}

	private boolean memberComplexAnnotationSpecifierLookahead() {
		if (this.startsComplexAnnotationSpecifier()) {
			return true;
		} else {
			ZKMScriptToken savedScanPos;
			do {
				savedScanPos = this.scanPos;
			} while (!this.nextIsAndAndOrMemberSpecifierStart());

			this.scanPos = savedScanPos;
			return false;
		}
	}

	private boolean literalPackageComponentDeleteThenDotLookahead() {
		return this.scanToken(60) ? true : this.scanToken(21);
	}

	private boolean linkPackageComponentAggressiveKeywordLookahead() {
		return this.scanToken(90);
	}

	private boolean memberSpecifierPrintKeywordLookahead() {
		return this.scanToken(46);
	}

	private boolean memberSpecifierTrimKeywordLookahead() {
		return this.scanToken(45);
	}

	private boolean literalPackageComponentOpenThenDotLookahead() {
		return this.scanToken(39) ? true : this.scanToken(21);
	}

	private boolean moduleNameComponentNameTokenLookahead() {
		return this.scanToken(213);
	}

	private boolean methodNameRandomKeywordLookahead() {
		return this.scanToken(63);
	}

	private boolean memberSpecifierDeleteKeywordLookahead() {
		return this.scanToken(60);
	}

	public final AbstractScriptNode parseObfuscate() {
		ObfuscateStatement obfuscate = new ObfuscateStatement(56);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(obfuscate);

		try {
			ZKMScriptToken keywordToken = this.consumeParameter(81);
			this.statementKind = 81;

			while (true) {
				switch (this.nextToken.kind) {
					case 78:
					case 85:
					case 95:
					case 101:
					case 103:
					case 104:
					case 105:
					case 106:
					case 110:
					case 112:
					case 113:
					case 114:
					case 115:
					case 116:
					case 117:
					case 118:
					case 119:
					case 121:
					case 122:
					case 123:
					case 124:
					case 128:
					case 131:
					case 133:
					case 134:
					case 135:
					case 136:
					case 139:
					case 143:
					case 145:
					case 147:
					case 148:
					case 149:
					case 150:
					case 151:
					case 155:
					case 156:
					case 160:
					case 168:
					case 175:
					case 183:
					case 185:
					case 188:
					case 194:
					case 201:
						this.parseObfuscateParameter();
						break;
					case 79:
					case 80:
					case 81:
					case 82:
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
					case 96:
					case 97:
					case 98:
					case 99:
					case 100:
					case 102:
					case 107:
					case 108:
					case 109:
					case 111:
					case 120:
					case 125:
					case 126:
					case 127:
					case 129:
					case 130:
					case 132:
					case 137:
					case 138:
					case 140:
					case 141:
					case 142:
					case 144:
					case 146:
					case 152:
					case 153:
					case 154:
					case 157:
					case 158:
					case 159:
					case 161:
					case 162:
					case 163:
					case 164:
					case 165:
					case 166:
					case 167:
					case 169:
					case 170:
					case 171:
					case 172:
					case 173:
					case 174:
					case 176:
					case 177:
					case 178:
					case 179:
					case 180:
					case 181:
					case 182:
					case 184:
					case 186:
					case 187:
					case 189:
					case 190:
					case 191:
					case 192:
					case 193:
					case 195:
					case 196:
					case 197:
					case 198:
					case 199:
					case 200:
					default:
						this.la1Generation[72] = this.generationCounter;
						this.consumeParameter(19);
						this.lexicalBuffer.endNode(obfuscate, true);
						nodeOpen = false;
						obfuscate.setLineNumber(keywordToken.beginLine);
						return obfuscate;
				}
			}
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (nodeOpen) {
					this.lexicalBuffer.discardNode(obfuscate);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable rethrownThrowable) {
				if (nodeOpen) {
					this.lexicalBuffer.endNode(obfuscate, true);
				}

				throw rethrownThrowable;
			}
		}
	}

	public final void parseImplementsClause() {
		ImplementsClause implementsClause = new ImplementsClause(154);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(implementsClause);

		try {
			this.consumeParameter(91);
			switch (this.nextToken.kind) {
				case 25:
				case 27:
				case 28:
					this.parseComplexAnnotationSpecifier();
					break;
				case 26:
				default:
					this.la1Generation[117] = this.generationCounter;
			}

			this.parseQualifiedClassName();

			while (true) {
				switch (this.nextToken.kind) {
					case 20:
						this.consumeParameter(20);
						switch (this.nextToken.kind) {
							case 25:
							case 27:
							case 28:
								this.parseComplexAnnotationSpecifier();
								break;
							case 26:
							default:
								this.la1Generation[119] = this.generationCounter;
						}

						this.parseQualifiedClassName();
						break;
					default:
						this.la1Generation[118] = this.generationCounter;
						this.lexicalBuffer.endNode(implementsClause, true);
						return;
				}
			}
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (true) {
					this.lexicalBuffer.discardNode(implementsClause);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}
				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable rethrownThrowable) {
				if (nodeOpen) {
					this.lexicalBuffer.endNode(implementsClause, true);
				}

				throw rethrownThrowable;
			}
		}
	}

	private boolean classModifierEnumKeywordLookahead() {
		return this.scanToken(40);
	}

	private boolean literalPackageComponentSearchThenDotLookahead() {
		return this.scanToken(56) ? true : this.scanToken(21);
	}

	private boolean memberModifierPublicKeywordLookahead() {
		return this.scanToken(57);
	}

	private boolean fieldNameBridgeKeywordLookahead() {
		return this.scanToken(52);
	}

	private boolean andModuleSpecifierLookahead() {
		return this.scanToken(35) ? true : this.startsComplexModuleSpecifier();
	}

	private boolean memberSpecifierNormalKeywordLookahead() {
		return this.scanToken(61);
	}

	private boolean moduleNameComponentAnnotationKeywordLookahead() {
		return this.scanToken(87);
	}

	private boolean literalPackageComponentAllThenDotLookahead() {
		return this.scanToken(37) ? true : this.scanToken(21);
	}

	private boolean linkPackageComponentTrimKeywordLookahead() {
		return this.scanToken(45);
	}

	private boolean fieldNameNormalKeywordLookahead() {
		return this.scanToken(61);
	}

	private boolean memberSpecifierHeavyKeywordLookahead() {
		return this.scanToken(48);
	}

	private boolean methodNameStarTokenLookahead() {
		return this.scanToken(23);
	}

	private boolean methodNameLookahead() {
		return this.methodNameKeywordChoiceLookahead();
	}

	private boolean bracketedPackageSpecifierLookahead() {
		return this.complexPackageSpecifierParenElementLookahead();
	}

	private boolean classModifierPublicKeywordLookahead() {
		return this.scanToken(57);
	}

	private boolean methodNameExcludeKeywordLookahead() {
		return this.scanToken(64);
	}

	private boolean memberModifierVolatileKeywordLookahead() {
		return this.scanToken(75);
	}

	private boolean moduleSpecifierCaretTokenLookahead() {
		return this.scanToken(26);
	}

	private boolean moduleNameComponentGroupingsKeywordLookahead() {
		return this.scanToken(86);
	}

	private boolean linkPackageComponentScrambleKeywordLookahead() {
		return this.scanToken(71);
	}

	private boolean matchesComplexModuleSpecifierExpansion() {
		if (this.startsComplexModuleSpecifier()) {
			return true;
		} else {
			ZKMScriptToken savedScanPos = this.scanPos;
			if (this.scanToken(38)) {
				this.scanPos = savedScanPos;
				if (this.scanToken(19)) {
					return true;
				}
			}

			return false;
		}
	}

	private boolean andPackageSpecifierLookahead() {
		return this.scanToken(35) ? true : this.matchesComplexPackageSpecifierStart();
	}

	private boolean literalPackageComponentGcThenDotLookahead() {
		return this.scanToken(36) ? true : this.scanToken(21);
	}

	private boolean moduleNameComponentTrimKeywordLookahead() {
		return this.scanToken(45);
	}

	private boolean fieldNameAllKeywordLookahead() {
		return this.scanToken(37);
	}

	private boolean fieldNameEnhancedKeywordLookahead() {
		return this.scanToken(74);
	}

	private boolean linkPackageComponentOpenKeywordLookahead() {
		return this.scanToken(39);
	}

	private boolean methodNameSyntheticKeywordLookahead() {
		return this.scanToken(77);
	}

	private boolean methodNameKeywordChoiceLookahead() {
		ZKMScriptToken savedScanPos = this.scanPos;
		if (this.methodNameStarTokenLookahead()) {
			this.scanPos = savedScanPos;
			if (this.nextIsNameInMethodNameSet()) {
				this.scanPos = savedScanPos;
				if (this.methodSpecifierClassNameIsWildcardName()) {
					this.scanPos = savedScanPos;
					if (this.nextIsInitTokenInMethodSignature()) {
						this.scanPos = savedScanPos;
						if (this.nextIsClinitInMethodSignature()) {
							this.scanPos = savedScanPos;
							if (this.methodSpecifierClassNameIsOpen()) {
								this.scanPos = savedScanPos;
								if (this.nextIsRandomizeInMemberSpecifierStart()) {
									this.scanPos = savedScanPos;
									if (this.methodNamePrintKeywordLookahead()) {
										this.scanPos = savedScanPos;
										if (this.methodSignatureDeleteKeywordLookahead()) {
											this.scanPos = savedScanPos;
											if (this.nextIsSearchInMethodNameSet()) {
												this.scanPos = savedScanPos;
												if (this.methodNameExcludeKeywordLookahead()) {
													this.scanPos = savedScanPos;
													if (this.nextIsKeepInMethodSignature()) {
														this.scanPos = savedScanPos;
														if (this.nextIsExecuteInMethodNameSet()) {
															this.scanPos = savedScanPos;
															if (this.nextIsScrambleInMethodNameSet()) {
																this.scanPos = savedScanPos;
																if (this.nextIsNormalInMethodSignature()) {
																	this.scanPos = savedScanPos;
																	if (this.nextIsNoneInMethodNameSet()) {
																		this.scanPos = savedScanPos;
																		if (this.methodSpecifierClassNameIsLight()) {
																			this.scanPos = savedScanPos;
																			if (this.nextIsHeavyInMethodSignature()) {
																				this.scanPos = savedScanPos;
																				if (this.nextIsTrimInMethodSignature()) {
																					this.scanPos = savedScanPos;
																					if (this.nextIsAllInMethodNameSet()) {
																						this.scanPos = savedScanPos;
																						if (this.methodNameObfuscateKeywordLookahead()) {
																							this.scanPos = savedScanPos;
																							if (this.methodSignatureClasspathKeywordLookahead()) {
																								this.scanPos = savedScanPos;
																								if (this.nextIsGroupingsInMethodSignature()) {
																									this.scanPos = savedScanPos;
																									if (this.methodNameAggressiveKeywordLookahead()) {
																										this.scanPos = savedScanPos;
																										if (this.methodNameSyntheticKeywordLookahead()) {
																											this.scanPos = savedScanPos;
																											if (this.methodSignatureEnumKeywordLookahead()) {
																												this.scanPos = savedScanPos;
																												if (this.methodSignatureAnnotationKeywordLookahead()) {
																													this.scanPos = savedScanPos;
																													if (this.methodNameBridgeKeywordLookahead()) {
																														this.scanPos = savedScanPos;
																														if (this.nextIsEnhancedInMethodNameSet()) {
																															this.scanPos = savedScanPos;
																															if (this.methodNameRandomKeywordLookahead()) {
																																this.scanPos = savedScanPos;
																																if (this.methodNameAsciiKeywordLookahead()) {
																																	return true;
																																}
																															}
																														}
																													}
																												}
																											}
																										}
																									}
																								}
																							}
																						}
																					}
																				}
																			}
																		}
																	}
																}
															}
														}
													}
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}

		return false;
	}

	private boolean methodNameAsciiKeywordLookahead() {
		return this.scanToken(51);
	}

	private boolean fieldNameRandomizeKeywordLookahead() {
		return this.scanToken(85);
	}

	private boolean nextIsWildcardNameInModuleNameComponent() {
		return this.scanToken(214);
	}

	private boolean matchesFieldSignatureNameTokenSet() {
		ZKMScriptToken savedScanPos = this.scanPos;
		if (this.nextIsStarInFieldSpecifier()) {
			this.scanPos = savedScanPos;
			if (this.fieldSpecifierClassNameIsName()) {
				this.scanPos = savedScanPos;
				if (this.fieldNameWildcardNameLookahead()) {
					this.scanPos = savedScanPos;
					if (this.nextIsOpenInFieldSpecifier()) {
						this.scanPos = savedScanPos;
						if (this.fieldNameRandomizeKeywordLookahead()) {
							this.scanPos = savedScanPos;
							if (this.fieldSpecifierClassNameIsPrint()) {
								this.scanPos = savedScanPos;
								if (this.nextIsDeleteInFieldSpecifier()) {
									this.scanPos = savedScanPos;
									if (this.fieldNameSearchKeywordLookahead()) {
										this.scanPos = savedScanPos;
										if (this.fieldNameExcludeKeywordLookahead()) {
											this.scanPos = savedScanPos;
											if (this.complexFieldSpecifierKeepKeywordLookahead()) {
												this.scanPos = savedScanPos;
												if (this.fieldSpecifierClassNameIsExecute()) {
													this.scanPos = savedScanPos;
													if (this.fieldSpecifierClassNameIsScramble()) {
														this.scanPos = savedScanPos;
														if (this.fieldNameNormalKeywordLookahead()) {
															this.scanPos = savedScanPos;
															if (this.fieldSpecifierClassNameIsNone()) {
																this.scanPos = savedScanPos;
																if (this.fieldNameLightKeywordLookahead()) {
																	this.scanPos = savedScanPos;
																	if (this.fieldNameHeavyKeywordLookahead()) {
																		this.scanPos = savedScanPos;
																		if (this.fieldNameTrimKeywordLookahead()) {
																			this.scanPos = savedScanPos;
																			if (this.fieldNameAllKeywordLookahead()) {
																				this.scanPos = savedScanPos;
																				if (this.complexFieldSpecifierObfuscateKeywordLookahead()) {
																					this.scanPos = savedScanPos;
																					if (this.complexFieldSpecifierClasspathKeywordLookahead()) {
																						this.scanPos = savedScanPos;
																						if (this.complexFieldSpecifierGroupingsKeywordLookahead()) {
																							this.scanPos = savedScanPos;
																							if (this.fieldNameAggressiveKeywordLookahead()) {
																								this.scanPos = savedScanPos;
																								if (this.nextIsSyntheticInFieldSignatureSet()) {
																									this.scanPos = savedScanPos;
																									if (this.fieldNameEnumKeywordLookahead()) {
																										this.scanPos = savedScanPos;
																										if (this.nextIsAnnotationInFieldSpecifier()) {
																											this.scanPos = savedScanPos;
																											if (this.fieldNameBridgeKeywordLookahead()) {
																												this.scanPos = savedScanPos;
																												if (this.fieldNameEnhancedKeywordLookahead()) {
																													this.scanPos = savedScanPos;
																													if (this.fieldNameRandomKeywordLookahead()) {
																														this.scanPos = savedScanPos;
																														if (this.nextIsAsciiInFieldSignatureSet()) {
																															return true;
																														}
																													}
																												}
																											}
																										}
																									}
																								}
																							}
																						}
																					}
																				}
																			}
																		}
																	}
																}
															}
														}
													}
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}

		return false;
	}

	private boolean nextIsFinalInMemberModifierSet() {
		return this.scanToken(50);
	}

	private boolean matchesNegatableParenthesizedSpecifierList() {
		return this.startsBracketedAnnotationSpecifier();
	}

	private boolean nextIsExecuteInLinkLiteralPackageComponent() {
		return this.scanToken(69);
	}

	private boolean nextIsEnumInLinkLiteralPackageComponent() {
		return this.scanToken(40);
	}

	private boolean nextIsNoneInClassNameSet() {
		return this.scanToken(43);
	}

	private boolean nextIsEnhancedInMethodNameSet() {
		return this.scanToken(74);
	}

	private boolean nextIsNameInMethodNameSet() {
		return this.scanToken(213);
	}

	private boolean nextIsSearchInMethodNameSet() {
		return this.scanToken(56);
	}

	private boolean nextIsScrambleInMethodNameSet() {
		return this.scanToken(71);
	}

	private boolean nextIsClasspathInLinkLiteralPackageComponent() {
		return this.scanToken(80);
	}

	private boolean nextIsStarInLinkLiteralPackageComponent() {
		return this.scanToken(23);
	}

	private boolean nextIsRandomInLinkLiteralPackageComponent() {
		return this.scanToken(63);
	}

	private boolean nextIsNoneInMethodNameSet() {
		return this.scanToken(43);
	}

	private boolean nextIsEnhancedInModuleNameComponent() {
		return this.scanToken(74);
	}

	private boolean nextIsSyntheticInFieldSignatureSet() {
		return this.scanToken(77);
	}

	private boolean nextIsHeavyOrDotInDottedName() {
		return this.scanToken(48) ? true : this.scanToken(21);
	}

	private boolean nextIsNameInSpecifierAlternation() {
		return this.scanToken(213);
	}

	private boolean matchesNegatableParenthesizedAndOrList() {
		ZKMScriptToken savedScanPos = this.scanPos;
		if (this.bracketedMethodSpecifierNextIsBang()) {
			this.scanPos = savedScanPos;
		}

		if (this.scanToken(28)) {
			return true;
		} else if (this.methodNameFilterAndChainLookahead()) {
			return true;
		} else {
			do {
				savedScanPos = this.scanPos;
			} while (!this.nextIsOrOrInMethodSignature());

			this.scanPos = savedScanPos;
			return this.scanToken(29);
		}
	}

	private boolean matchesOneOrMoreDottedNameParts() {
		return this.matchesPackageNameComponentList();
	}

	private boolean matchesClassModifierListExpansion() {
		ZKMScriptToken savedScanPos = this.scanPos;
		if (this.scanToken(25)) {
			this.scanPos = savedScanPos;
		}

		return this.classModifierLookahead();
	}

	private boolean matchesComplexPackageSpecifierExpansion() {
		if (this.matchesComplexPackageSpecifierStart()) {
			return true;
		} else {
			ZKMScriptToken savedScanPos = this.scanPos;
			if (this.scanToken(38)) {
				this.scanPos = savedScanPos;
				if (this.scanToken(19)) {
					this.scanPos = savedScanPos;
					if (this.scanToken(31)) {
						return true;
					}
				}
			}

			return false;
		}
	}

	private boolean nextIsScrambleInClassNameSet() {
		return this.scanToken(71);
	}

	private boolean nextIsExecuteInMethodNameSet() {
		return this.scanToken(69);
	}

	private boolean matchesAnnotationOrClassName() {
		return this.scanToken(27) ? true : this.annotationMemberSpecifierLookahead();
	}

	private boolean nextIsDeleteInModuleNameComponent() {
		return this.scanToken(60);
	}

	private boolean matchesAndOrOperand() {
		if (this.matchesComplexPackageSpecifierStart()) {
			return true;
		} else {
			ZKMScriptToken savedScanPos;
			do {
				savedScanPos = this.scanPos;
			} while (!this.andPackageSpecifierLookahead());

			this.scanPos = savedScanPos;
			return false;
		}
	}

	private boolean nextIsBridgeInClassNameSet() {
		return this.scanToken(52);
	}

	private boolean nextIsSearchInClassNameSet() {
		return this.scanToken(56);
	}

	private boolean nextIsAllInClassNameSet() {
		return this.scanToken(37);
	}

	private boolean matchesModuleNameComponentOrDot() {
		return this.startsModuleNameComponent() ? true : this.scanToken(21);
	}

	private boolean nextIsAllInMethodNameSet() {
		return this.scanToken(37);
	}

	public final void parseComplexAnnotationSpecifier() {
		ComplexAnnotationSpecifier complexAnnotationSpecifier = new ComplexAnnotationSpecifier(175);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(complexAnnotationSpecifier);

		try {
			switch (this.nextToken.kind) {
				case 25:
				case 28:
					this.parseBracketedAnnotationSpecifier();
					break;
				case 26:
				default:
					this.la1Generation[134] = this.generationCounter;
					this.consumeParameter(-1);
					throw new ParsingException();
				case 27:
					this.parseAnnotation();
			}

			this.lexicalBuffer.endNode(complexAnnotationSpecifier, true);
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (true) {
					this.lexicalBuffer.discardNode(complexAnnotationSpecifier);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}
				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable rethrownThrowable) {
				if (nodeOpen) {
					this.lexicalBuffer.endNode(complexAnnotationSpecifier, true);
				}

				throw rethrownThrowable;
			}
		}
	}

	public final AbstractScriptNode encryptStringLiteralsExclude() {
		StringEncryptionExcludeStatement encryptStringLiteralsExclude = new StringEncryptionExcludeStatement(20);
		boolean nodeOpen = true;
		this.lexicalBuffer.beginNode(encryptStringLiteralsExclude);

		try {
			ZKMScriptToken keywordToken = this.consumeParameter(162);
			this.statementKind = 162;
			this.parseRenameFilterParameter();

			while (true) {
				switch (this.nextToken.kind) {
					case 38:
						this.consumeParameter(38);
						this.parseRenameFilterParameter();
						break;
					default:
						this.la1Generation[22] = this.generationCounter;
						this.consumeParameter(19);
						this.lexicalBuffer.endNode(encryptStringLiteralsExclude, true);
						nodeOpen = false;
						encryptStringLiteralsExclude.setLineNumber(keywordToken.beginLine);
						return encryptStringLiteralsExclude;
				}
			}
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (nodeOpen) {
					this.lexicalBuffer.discardNode(encryptStringLiteralsExclude);
					nodeOpen = false;
				} else {
					this.lexicalBuffer.pop();
				}

				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ParsingException) {
					throw (ParsingException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable rethrownThrowable) {
				if (nodeOpen) {
					this.lexicalBuffer.endNode(encryptStringLiteralsExclude, true);
				}

				throw rethrownThrowable;
			}
		}
	}

	private boolean nextIsNoneInLinkLiteralPackageComponent() {
		return this.scanToken(43);
	}

	private boolean nextIsAsciiInFieldSignatureSet() {
		return this.scanToken(51);
	}

	static {
		initLa1TokenBits0();
		initLa1TokenBits1();
		initLa1TokenBits2();
		initLa1TokenBits3();
		initLa1TokenBits4();
		initLa1TokenBits5();
		initLa1TokenBits6();
	}
}
