package com.zelix.tools.proguardConfigurationTranslate;

import com.zelix.exceptions.ProguardLexicalError;
import java.io.IOException;
import java.io.PrintStream;

public class ProguardTokenManager implements ProguardCommands {
	public static final long[] jjbitVec0 = new long[]{-4503599625273342L, -8193L, -17525614051329L, 1297036692691091455L};
	public static final long[] jjbitVec1 = new long[]{0L, 0L, 297242231151001600L, -36028797027352577L};
	public static final long[] jjbitVec2 = new long[]{4503586742468607L, -65536L, -432556670460100609L, 70501888360451L};
	public static final long[] jjbitVec3 = new long[]{0L, 288230376151711744L, -17179879616L, 4503599577006079L};
	public static final long[] jjbitVec4 = new long[]{-1L, -1L, -4093L, 234187180623206815L};
	public static final long[] jjbitVec5 = new long[]{-562949953421312L, -8547991553L, 255L, 1979120929931264L};
	public static final long[] jjbitVec6 = new long[]{576460743713488896L, -562949953419265L, -1L, 2017613045381988351L};
	public static final long[] jjbitVec7 = new long[]{35184371892224L, 0L, 274877906943L, 0L};
	public static final long[] jjbitVec8 = new long[]{2594073385365405664L, 17163157504L, 271902628478820320L, 4222140488351744L};
	public static final long[] jjbitVec9 = new long[]{247132830528276448L, 7881300924956672L, 2589004636761075680L, 4295032832L};
	public static final long[] jjbitVec10 = new long[]{2579997437506199520L, 15837691904L, 270153412153034720L, 0L};
	public static final long[] jjbitVec11 = new long[]{283724577500946400L, 12884901888L, 283724577500946400L, 13958643712L};
	public static final long[] jjbitVec12 = new long[]{288228177128316896L, 12884901888L, 3457638613854978016L, 127L};
	public static final long[] jjbitVec13 = new long[]{-9219431387180826626L, 127L, 2309762420256548246L, 805306463L};
	public static final long[] jjbitVec14 = new long[]{1L, 8796093021951L, 3840L, 0L};
	public static final long[] jjbitVec15 = new long[]{7679401525247L, 4128768L, -4294967296L, 36028797018898495L};
	public static final long[] jjbitVec16 = new long[]{-1L, -2080374785L, -1065151889409L, 288230376151711743L};
	public static final long[] jjbitVec17 = new long[]{-129L, -3263218305L, 9168625153884503423L, -140737496776899L};
	public static final long[] jjbitVec18 = new long[]{-2160230401L, 134217599L, -4294967296L, 9007199254740991L};
	public static final long[] jjbitVec19 = new long[]{-2L, -1L, -1L, -1L};
	public static final long[] jjbitVec20 = new long[]{-1L, 35923243902697471L, -4160749570L, 8796093022207L};
	public static final long[] jjbitVec21 = new long[]{0L, 0L, 4503599627370495L, 134217728L};
	public static final long[] jjbitVec22 = new long[]{-4294967296L, 72057594037927935L, 2199023255551L, 0L};
	public static final long[] jjbitVec23 = new long[]{-1L, -1L, -4026531841L, 288230376151711743L};
	public static final long[] jjbitVec24 = new long[]{-3233808385L, 4611686017001275199L, 6908521828386340863L, 2295745090394464220L};
	public static final long[] jjbitVec25 = new long[]{Long.MIN_VALUE, -9223372036854775807L, 281470681743360L, 0L};
	public static final long[] jjbitVec26 = new long[]{287031153606524036L, -4294967296L, 15L, 0L};
	public static final long[] jjbitVec27 = new long[]{521858996278132960L, -2L, -6977224705L, Long.MAX_VALUE};
	public static final long[] jjbitVec28 = new long[]{-527765581332512L, -1L, 72057589742993407L, 0L};
	public static final long[] jjbitVec29 = new long[]{-1L, -1L, 18014398509481983L, 0L};
	public static final long[] jjbitVec30 = new long[]{-1L, -1L, 274877906943L, 0L};
	public static final long[] jjbitVec31 = new long[]{-1L, -1L, 8191L, 0L};
	public static final long[] jjbitVec32 = new long[]{-1L, -1L, 68719476735L, 0L};
	public static final long[] jjbitVec33 = new long[]{70368744177663L, 0L, 0L, 0L};
	public static final long[] jjbitVec34 = new long[]{6881498030004502655L, -37L, 1125899906842623L, -524288L};
	public static final long[] jjbitVec35 = new long[]{4611686018427387903L, -65536L, -196609L, 1152640029630136575L};
	public static final long[] jjbitVec36 = new long[]{6755399441055744L, -11538275021824000L, -1L, 2305843009213693951L};
	public static final long[] jjbitVec37 = new long[]{-8646911293141286896L, -137304735746L, Long.MAX_VALUE, 425688104188L};
	public static final long[] jjbitVec38 = new long[]{0L, 0L, 297242235445968895L, -36028797027352577L};
	public static final long[] jjbitVec39 = new long[]{-1L, 288230406216515583L, -17179879616L, 4503599577006079L};
	public static final long[] jjbitVec40 = new long[]{-1L, -1L, -3973L, 234187180623206815L};
	public static final long[] jjbitVec41 = new long[]{-562949953421312L, -8547991553L, -4899916411759099649L, 1979120929931286L};
	public static final long[] jjbitVec42 = new long[]{576460743713488896L, -277081220972545L, -1L, 2305629702346244095L};
	public static final long[] jjbitVec43 = new long[]{-246290604654592L, 2047L, 562949953421311L, 0L};
	public static final long[] jjbitVec44 = new long[]{-864691128455135250L, 281268803551231L, -3186861885341720594L, 4503392135166367L};
	public static final long[] jjbitVec45 = new long[]{-3211631683292264476L, 9006925953907079L, -869759877059465234L, 281204393851839L};
	public static final long[] jjbitVec46 = new long[]{-878767076314341394L, 281215949093263L, -4341532606274353172L, 280925229301191L};
	public static final long[] jjbitVec47 = new long[]{-4327961440926441490L, 281212990012895L, -4327961440926441492L, 281214063754719L};
	public static final long[] jjbitVec48 = new long[]{-4323457841299070996L, 281212992110031L, 3457638613854978028L, 3377704004977791L};
	public static final long[] jjbitVec49 = new long[]{-8646911284551352322L, 67076095L, 4323434403644581270L, 872365919L};
	public static final long[] jjbitVec50 = new long[]{-4422530440275951615L, -554153860399361L, 2305843009196855263L, 64L};
	public static final long[] jjbitVec51 = new long[]{272457864671395839L, 67044351L, -4294967296L, 36028797018898495L};
	public static final long[] jjbitVec52 = new long[]{-2160230401L, 1123701017804671L, -4294967296L, 9007199254740991L};
	public static final long[] jjbitVec53 = new long[]{0L, 0L, -1L, 4393886810111L};
	public static final long[] jjbitVec54 = new long[]{-4227893248L, 72057594037927935L, 4398046511103L, 0L};
	public static final long[] jjbitVec55 = new long[]{-9223235697412870144L, -9223094959924576255L, 281470681743360L, 9126739968L};
	public static final long[] jjbitVec56 = new long[]{522136073208332512L, -2L, -6876561409L, Long.MAX_VALUE};
	public static final long[] jjbitVec57 = new long[]{6881498031078244479L, -37L, 1125899906842623L, -524288L};
	public static final long[] jjbitVec58 = new long[]{6755463865565184L, -11538275021824000L, -1L, -6917529027641081857L};
	public static final long[] jjbitVec59 = new long[]{-8646911293074243568L, -137304735746L, Long.MAX_VALUE, 1008806742219095292L};
	public static final long[] jjbitVec60 = new long[]{0L, 0L, -1L, -1L};
	public static final int[] jjnextStates = new int[]{6, 3, 11, 8};
	public static final String[] jjstrLiteralImages = new String[]{
		"",
		null,
		null,
		null,
		null,
		null,
		null,
		null,
		null,
		null,
		"\ufeff",
		"~",
		",",
		";",
		"@",
		".",
		"!",
		"?",
		"%",
		"*",
		"(",
		")",
		"{",
		"}",
		"**",
		"-if",
		"***",
		"...",
		"enum",
		"-dump",
		"-keep",
		"class",
		"final",
		"public",
		"static",
		"native",
		"<init>",
		"bridge",
		"private",
		"extends",
		"-target",
		"-injars",
		"volatile",
		"strictfp",
		"<fields>",
		"-verbose",
		"-include",
		"-outjars",
		"protected",
		"<methods>",
		"abstract",
		"-android",
		"transient",
		"interface",
		"synthetic",
		"-keepcode",
		"-zipalign",
		"-dontnote",
		"-dontwarn",
		"@interface",
		"implements",
		"-keepnames",
		"module-info",
		"includecode",
		"-printseeds",
		"-dontshrink",
		"-printusage",
		"-libraryjars",
		"synchronized",
		"-applymapping",
		"-microedition",
		"-printmapping",
		"-assumevalues",
		"-dontoptimize",
		"-dontcompress",
		"allowshrinking",
		"-optimizations",
		"-dontpreverify",
		"-basedirectory",
		"-dontobfuscate",
		"-keepattributes",
		"-ignorewarnings",
		"allowobfuscation",
		"-keepdirectories",
		"-forceprocessing",
		"allowoptimization",
		"-keepclassmembers",
		"-keeppackagenames",
		"-repackageclasses",
		"-whyareyoukeeping",
		"-adaptclassstrings",
		"-keepkotlinmetadata",
		"-printconfiguration",
		"-keepparameternames",
		"-optimizationpasses",
		"-adaptkotlinmetadata",
		"-assumenosideeffects",
		"-overloadaggressively",
		"-keepclassmembernames",
		"-obfuscationdictionary",
		"-adaptresourcefilenames",
		"-keepclasseswithmembers",
		"-flattenpackagehierarchy",
		"-allowaccessmodification",
		"includedescriptorclasses",
		"-dontprocesskotlinmetadata",
		"-addconfigurationdebugging",
		"-renamesourcefileattribute",
		"-adaptresourcefilecontents",
		"-useuniqueclassmembernames",
		"-assumenoescapingparameters",
		"-dontusemixedcaseclassnames",
		"-keepclasseswithmembernames",
		"-classobfuscationdictionary",
		"-assumenoexternalsideeffects",
		"-skipnonpubliclibraryclasses",
		"-mergeinterfacesaggressively",
		"-assumenoexternalreturnvalues",
		"-packageobfuscationdictionary",
		"-dontskipnonpubliclibraryclasses",
		"-dontskipnonpubliclibraryclassmembers",
		null,
		null,
		null,
		null,
		null,
		null,
		null,
		null,
		null
	};
	public static final String[] lexStateNames = new String[]{"DEFAULT", "IN_SINGLE_LINE_COMMENT", "IN_STRING_LITERAL"};
	public static final int[] jjnewLexState = new int[]{
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		1,
		0,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		2,
		0,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1
	};
	public static final long[] jjtoToken = new long[]{-1023L, -720575940379279361L, 0L};
	public static final long[] jjtoSkip = new long[]{190L, 0L, 0L};
	public static final long[] jjtoSpecial = new long[]{128L, 0L, 0L};
	public static final long[] jjtoMore = new long[]{320L, 720575940379279360L, 0L};
	public PrintStream debugStream = System.out;
	private final int[] jjrounds = new int[12];
	private final int[] jjstateSet = new int[24];
	private final StringBuilder jjimage = new StringBuilder();
	private StringBuilder image = this.jjimage;
	public int curLexState = 0;
	public int defaultLexState = 0;
	protected SimpleCharStream2 input_stream;
	public int jjround;
	public int jjmatchedKind;
	public int jjnewStateCnt;
	protected char curChar;
	public int jjmatchedPos;
	private int jjimageLen;

	public ProguardTokenManager(SimpleCharStream2 stream) {
		this.input_stream = stream;
	}

	private static final boolean jjCanMove_2(int highByte, int highIndex, int lowIndex, long highMask, long lowMask) {
		switch (highByte) {
			case 0:
				return (jjbitVec60[lowIndex] & lowMask) != 0L;
			default:
				return (jjbitVec19[highIndex] & highMask) != 0L;
		}
	}

	private static final boolean jjCanMove_0(int highByte, int highIndex, int lowIndex, long highMask, long lowMask) {
		switch (highByte) {
			case 0:
				return (jjbitVec1[lowIndex] & lowMask) != 0L;
			case 2:
				return (jjbitVec2[lowIndex] & lowMask) != 0L;
			case 3:
				return (jjbitVec3[lowIndex] & lowMask) != 0L;
			case 4:
				return (jjbitVec4[lowIndex] & lowMask) != 0L;
			case 5:
				return (jjbitVec5[lowIndex] & lowMask) != 0L;
			case 6:
				return (jjbitVec6[lowIndex] & lowMask) != 0L;
			case 7:
				return (jjbitVec7[lowIndex] & lowMask) != 0L;
			case 9:
				return (jjbitVec8[lowIndex] & lowMask) != 0L;
			case 10:
				return (jjbitVec9[lowIndex] & lowMask) != 0L;
			case 11:
				return (jjbitVec10[lowIndex] & lowMask) != 0L;
			case 12:
				return (jjbitVec11[lowIndex] & lowMask) != 0L;
			case 13:
				return (jjbitVec12[lowIndex] & lowMask) != 0L;
			case 14:
				return (jjbitVec13[lowIndex] & lowMask) != 0L;
			case 15:
				return (jjbitVec14[lowIndex] & lowMask) != 0L;
			case 16:
				return (jjbitVec15[lowIndex] & lowMask) != 0L;
			case 17:
				return (jjbitVec16[lowIndex] & lowMask) != 0L;
			case 18:
				return (jjbitVec17[lowIndex] & lowMask) != 0L;
			case 19:
				return (jjbitVec18[lowIndex] & lowMask) != 0L;
			case 20:
				return (jjbitVec19[lowIndex] & lowMask) != 0L;
			case 22:
				return (jjbitVec20[lowIndex] & lowMask) != 0L;
			case 23:
				return (jjbitVec21[lowIndex] & lowMask) != 0L;
			case 24:
				return (jjbitVec22[lowIndex] & lowMask) != 0L;
			case 30:
				return (jjbitVec23[lowIndex] & lowMask) != 0L;
			case 31:
				return (jjbitVec24[lowIndex] & lowMask) != 0L;
			case 32:
				return (jjbitVec25[lowIndex] & lowMask) != 0L;
			case 33:
				return (jjbitVec26[lowIndex] & lowMask) != 0L;
			case 48:
				return (jjbitVec27[lowIndex] & lowMask) != 0L;
			case 49:
				return (jjbitVec28[lowIndex] & lowMask) != 0L;
			case 77:
				return (jjbitVec29[lowIndex] & lowMask) != 0L;
			case 159:
				return (jjbitVec30[lowIndex] & lowMask) != 0L;
			case 164:
				return (jjbitVec31[lowIndex] & lowMask) != 0L;
			case 215:
				return (jjbitVec32[lowIndex] & lowMask) != 0L;
			case 250:
				return (jjbitVec33[lowIndex] & lowMask) != 0L;
			case 251:
				return (jjbitVec34[lowIndex] & lowMask) != 0L;
			case 253:
				return (jjbitVec35[lowIndex] & lowMask) != 0L;
			case 254:
				return (jjbitVec36[lowIndex] & lowMask) != 0L;
			case 255:
				return (jjbitVec37[lowIndex] & lowMask) != 0L;
			default:
				return (jjbitVec0[highIndex] & highMask) != 0L;
		}
	}

	private static final boolean jjCanMove_1(int highByte, int highIndex, int lowIndex, long highMask, long lowMask) {
		switch (highByte) {
			case 0:
				return (jjbitVec38[lowIndex] & lowMask) != 0L;
			case 2:
				return (jjbitVec2[lowIndex] & lowMask) != 0L;
			case 3:
				return (jjbitVec39[lowIndex] & lowMask) != 0L;
			case 4:
				return (jjbitVec40[lowIndex] & lowMask) != 0L;
			case 5:
				return (jjbitVec41[lowIndex] & lowMask) != 0L;
			case 6:
				return (jjbitVec42[lowIndex] & lowMask) != 0L;
			case 7:
				return (jjbitVec43[lowIndex] & lowMask) != 0L;
			case 9:
				return (jjbitVec44[lowIndex] & lowMask) != 0L;
			case 10:
				return (jjbitVec45[lowIndex] & lowMask) != 0L;
			case 11:
				return (jjbitVec46[lowIndex] & lowMask) != 0L;
			case 12:
				return (jjbitVec47[lowIndex] & lowMask) != 0L;
			case 13:
				return (jjbitVec48[lowIndex] & lowMask) != 0L;
			case 14:
				return (jjbitVec49[lowIndex] & lowMask) != 0L;
			case 15:
				return (jjbitVec50[lowIndex] & lowMask) != 0L;
			case 16:
				return (jjbitVec51[lowIndex] & lowMask) != 0L;
			case 17:
				return (jjbitVec16[lowIndex] & lowMask) != 0L;
			case 18:
				return (jjbitVec17[lowIndex] & lowMask) != 0L;
			case 19:
				return (jjbitVec52[lowIndex] & lowMask) != 0L;
			case 20:
				return (jjbitVec19[lowIndex] & lowMask) != 0L;
			case 22:
				return (jjbitVec20[lowIndex] & lowMask) != 0L;
			case 23:
				return (jjbitVec53[lowIndex] & lowMask) != 0L;
			case 24:
				return (jjbitVec54[lowIndex] & lowMask) != 0L;
			case 30:
				return (jjbitVec23[lowIndex] & lowMask) != 0L;
			case 31:
				return (jjbitVec24[lowIndex] & lowMask) != 0L;
			case 32:
				return (jjbitVec55[lowIndex] & lowMask) != 0L;
			case 33:
				return (jjbitVec26[lowIndex] & lowMask) != 0L;
			case 48:
				return (jjbitVec56[lowIndex] & lowMask) != 0L;
			case 49:
				return (jjbitVec28[lowIndex] & lowMask) != 0L;
			case 77:
				return (jjbitVec29[lowIndex] & lowMask) != 0L;
			case 159:
				return (jjbitVec30[lowIndex] & lowMask) != 0L;
			case 164:
				return (jjbitVec31[lowIndex] & lowMask) != 0L;
			case 215:
				return (jjbitVec32[lowIndex] & lowMask) != 0L;
			case 250:
				return (jjbitVec33[lowIndex] & lowMask) != 0L;
			case 251:
				return (jjbitVec57[lowIndex] & lowMask) != 0L;
			case 253:
				return (jjbitVec35[lowIndex] & lowMask) != 0L;
			case 254:
				return (jjbitVec58[lowIndex] & lowMask) != 0L;
			case 255:
				return (jjbitVec59[lowIndex] & lowMask) != 0L;
			default:
				return (jjbitVec0[highIndex] & highMask) != 0L;
		}
	}

	private int jjMoveStringLiteralDfa8_0(long old0, long active0, long old1, long active1) {
		if (((active0 = active0 & old0) | (active1 = active1 & old1)) == 0L) {
			return this.jjStartNfa_0(6, old0, old1);
		} else {
			try {
				this.curChar = this.input_stream.readChar();
			} catch (IOException ignored) {
				this.jjStopStringLiteralDfa_0(7, active0, active1);
				return 8;
			}

			switch (this.curChar) {
				case '>':
					if ((active0 & 562949953421312L) != 0L) {
						return this.jjStopAtPos(8, 49);
					}
				case '?':
				case '@':
				case 'A':
				case 'B':
				case 'C':
				case 'D':
				case 'E':
				case 'F':
				case 'G':
				case 'H':
				case 'I':
				case 'J':
				case 'K':
				case 'L':
				case 'M':
				case 'N':
				case 'O':
				case 'P':
				case 'Q':
				case 'R':
				case 'S':
				case 'T':
				case 'U':
				case 'V':
				case 'W':
				case 'X':
				case 'Y':
				case 'Z':
				case '[':
				case '\\':
				case ']':
				case '^':
				case '_':
				case '`':
				case 'b':
				case 'h':
				case 'q':
				default:
					return this.jjStartNfa_0(7, active0, active1);
				case 'a':
					return this.jjMoveStringLiteralDfa9_0(active0, 0L, active1, 1677857028L);
				case 'c':
					if ((active0 & 18014398509481984L) != 0L) {
						return this.jjStartNfaWithStates_0(8, 54, 13);
					}

					return this.jjMoveStringLiteralDfa9_0(active0, 576460752303423488L, active1, 2748779069440L);
				case 'd':
					if ((active0 & 281474976710656L) != 0L) {
						return this.jjStartNfaWithStates_0(8, 48, 13);
					}

					return this.jjMoveStringLiteralDfa9_0(active0, 0L, active1, 8589934592L);
				case 'e':
					if ((active0 & 9007199254740992L) != 0L) {
						return this.jjStartNfaWithStates_0(8, 53, 13);
					} else if ((active0 & 36028797018963968L) != 0L) {
						return this.jjStopAtPos(8, 55);
					} else {
						if ((active0 & 144115188075855872L) != 0L) {
							return this.jjStopAtPos(8, 57);
						}

						return this.jjMoveStringLiteralDfa9_0(active0, 2305843009213693952L, active1, 1099512168449L);
					}
				case 'f':
					return this.jjMoveStringLiteralDfa9_0(active0, 0L, active1, 562949953421312L);
				case 'g':
					return this.jjMoveStringLiteralDfa9_0(active0, 0L, active1, 16777216L);
				case 'i':
					return this.jjMoveStringLiteralDfa9_0(active0, 0L, active1, 4398048610898L);
				case 'j':
					return this.jjMoveStringLiteralDfa9_0(active0, 0L, active1, 8L);
				case 'k':
					return this.jjMoveStringLiteralDfa9_0(active0, 0L, active1, 8388608L);
				case 'l':
					return this.jjMoveStringLiteralDfa9_0(active0, 0L, active1, 134217728L);
				case 'm':
					return this.jjMoveStringLiteralDfa9_0(active0, 0L, active1, 140737488355328L);
				case 'n':
					if ((active0 & 72057594037927936L) != 0L) {
						return this.jjStopAtPos(8, 56);
					} else {
						if ((active0 & 288230376151711744L) != 0L) {
							return this.jjStopAtPos(8, 58);
						}

						return this.jjMoveStringLiteralDfa9_0(active0, 4611686018427387904L, active1, 268435456L);
					}
				case 'o':
					return this.jjMoveStringLiteralDfa9_0(active0, Long.MIN_VALUE, active1, 28226666837835776L);
				case 'p':
					return this.jjMoveStringLiteralDfa9_0(active0, 0L, active1, 110338465748485280L);
				case 'r':
					return this.jjMoveStringLiteralDfa9_0(active0, 0L, active1, 65536L);
				case 's':
					return this.jjMoveStringLiteralDfa9_0(active0, 0L, active1, 299290505248768L);
				case 't':
					if ((active0 & 4503599627370496L) != 0L) {
						return this.jjStartNfaWithStates_0(8, 52, 13);
					}

					return this.jjMoveStringLiteralDfa9_0(active0, 1152921504606846976L, active1, 4503636134592512L);
				case 'u':
					return this.jjMoveStringLiteralDfa9_0(active0, 0L, active1, 35184372383744L);
				case 'v':
					return this.jjMoveStringLiteralDfa9_0(active0, 0L, active1, 8192L);
			}
		}
	}

	private int jjMoveStringLiteralDfa12_0(long old0, long active0) {
		if ((active0 = active0 & old0) == 0L) {
			return this.jjStartNfa_0(10, 0L, old0);
		} else {
			try {
				this.curChar = this.input_stream.readChar();
			} catch (IOException ignored) {
				this.jjStopStringLiteralDfa_0(11, 0L, active0);
				return 12;
			}

			switch (this.curChar) {
				case 'a':
					return this.jjMoveStringLiteralDfa13_0(active0, 5176775640350720L);
				case 'c':
					return this.jjMoveStringLiteralDfa13_0(active0, 17660905521152L);
				case 'd':
					return this.jjMoveStringLiteralDfa13_0(active0, 140771848093696L);
				case 'e':
					if ((active0 & 512L) != 0L) {
						return this.jjStopAtPos(12, 73);
					}

					return this.jjMoveStringLiteralDfa13_0(active0, 10141900254216192L);
				case 'f':
					return this.jjMoveStringLiteralDfa13_0(active0, 8192L);
				case 'g':
					if ((active0 & 32L) != 0L) {
						return this.jjStopAtPos(12, 69);
					} else if ((active0 & 128L) != 0L) {
						return this.jjStopAtPos(12, 71);
					}
				case 'b':
				case 'h':
				case 'j':
				case 'l':
				case 'o':
				case 'q':
				case 'v':
				default:
					return this.jjStartNfa_0(11, 0L, active0);
				case 'i':
					return this.jjMoveStringLiteralDfa13_0(active0, 2252899325313024L);
				case 'k':
					return this.jjMoveStringLiteralDfa13_0(active0, 2199023255552L);
				case 'm':
					return this.jjMoveStringLiteralDfa13_0(active0, 569087361024L);
				case 'n':
					if ((active0 & 64L) != 0L) {
						return this.jjStopAtPos(12, 70);
					}

					return this.jjMoveStringLiteralDfa13_0(active0, 1082267648L);
				case 'p':
					return this.jjMoveStringLiteralDfa13_0(active0, 108086391056891904L);
				case 'r':
					return this.jjMoveStringLiteralDfa13_0(active0, 8590475264L);
				case 's':
					if ((active0 & 256L) != 0L) {
						return this.jjStopAtPos(12, 72);
					} else {
						if ((active0 & 1024L) != 0L) {
							return this.jjStopAtPos(12, 74);
						}

						return this.jjMoveStringLiteralDfa13_0(active0, 18014398510530560L);
					}
				case 't':
					return this.jjMoveStringLiteralDfa13_0(active0, 67469312L);
				case 'u':
					return this.jjMoveStringLiteralDfa13_0(active0, 268435456L);
				case 'w':
					return this.jjMoveStringLiteralDfa13_0(active0, 281612415664128L);
			}
		}
	}

	private int jjMoveStringLiteralDfa3_0(long old0, long active0, long old1, long active1) {
		if (((active0 = active0 & old0) | (active1 = active1 & old1)) == 0L) {
			return this.jjStartNfa_0(1, old0, old1);
		} else {
			try {
				this.curChar = this.input_stream.readChar();
			} catch (IOException ignored) {
				this.jjStopStringLiteralDfa_0(2, active0, active1);
				return 3;
			}

			switch (this.curChar) {
				case 'a':
					return this.jjMoveStringLiteralDfa4_0(active0, 4402341478400L, active1, 580887951441920L);
				case 'b':
					return this.jjMoveStringLiteralDfa4_0(active0, 0L, active1, 8L);
				case 'c':
					return this.jjMoveStringLiteralDfa4_0(active0, 70368744177664L, active1, 18014398509482064L);
				case 'd':
					return this.jjMoveStringLiteralDfa4_0(active0, 2251937252638720L, active1, 4398046511104L);
				case 'e':
					return this.jjMoveStringLiteralDfa4_0(active0, 2350897148502999040L, active1, 316823241818112L);
				case 'f':
					return this.jjMoveStringLiteralDfa4_0(active0, 0L, active1, 34359738368L);
				case 'g':
				case 'h':
				case 'k':
				case 'q':
				case 'w':
				case 'x':
				default:
					return this.jjStartNfa_0(2, active0, active1);
				case 'i':
					return this.jjMoveStringLiteralDfa4_0(active0, 8899172237312L, active1, 2251800082120837L);
				case 'j':
					return this.jjMoveStringLiteralDfa4_0(active0, 2199023255552L, active1, 0L);
				case 'l':
					return this.jjMoveStringLiteralDfa4_0(active0, -8070450523657994240L, active1, 1649267441664L);
				case 'm':
					if ((active0 & 268435456L) != 0L) {
						return this.jjStartNfaWithStates_0(3, 28, 13);
					}

					return this.jjMoveStringLiteralDfa4_0(active0, 536870912L, active1, 0L);
				case 'n':
					return this.jjMoveStringLiteralDfa4_0(active0, 436849163854938112L, active1, 108238123661698562L);
				case 'o':
					return this.jjMoveStringLiteralDfa4_0(active0, 0L, active1, 2361344L);
				case 'p':
					return this.jjMoveStringLiteralDfa4_0(active0, 72057594037927936L, active1, 16777248L);
				case 'r':
					return this.jjMoveStringLiteralDfa4_0(active0, 36283883716608L, active1, 4503599628419072L);
				case 's':
					return this.jjMoveStringLiteralDfa4_0(active0, 2147483648L, active1, 10203472200745216L);
				case 't':
					return this.jjMoveStringLiteralDfa4_0(active0, 596586230318104576L, active1, 1073745920L);
				case 'u':
					return this.jjMoveStringLiteralDfa4_0(active0, 4611686018427387904L, active1, 0L);
				case 'v':
					return this.jjMoveStringLiteralDfa4_0(active0, 274877906944L, active1, 0L);
				case 'y':
					return this.jjMoveStringLiteralDfa4_0(active0, 0L, active1, 33554432L);
			}
		}
	}

	private int jjMoveStringLiteralDfa11_0(long old0, long active0, long old1, long active1) {
		long maskedActive0 = active0 & old0;
		long duplicatedMaskedActive0 = active0 & old0;
		if ((maskedActive0 | (active1 = active1 & old1)) == 0L) {
			return this.jjStartNfa_0(9, old0, old1);
		} else {
			try {
				this.curChar = this.input_stream.readChar();
			} catch (IOException ignored) {
				this.jjStopStringLiteralDfa_0(10, 0L, active1);
				return 11;
			}

			switch (this.curChar) {
				case 'a':
					return this.jjMoveStringLiteralDfa12_0(active1, 294912L);
				case 'b':
				case 'h':
				case 'j':
				case 'p':
				case 'q':
				case 'v':
				case 'w':
				case 'x':
				case 'y':
				default:
					return this.jjStartNfa_0(10, 0L, active1);
				case 'c':
					return this.jjMoveStringLiteralDfa12_0(active1, 642114790621184L);
				case 'd':
					if ((active1 & 16L) != 0L) {
						return this.jjStartNfaWithStates_0(11, 68, 13);
					}

					return this.jjMoveStringLiteralDfa12_0(active1, 4294967296L);
				case 'e':
					return this.jjMoveStringLiteralDfa12_0(active1, 140754714362112L);
				case 'f':
					return this.jjMoveStringLiteralDfa12_0(active1, 4503599627370496L);
				case 'g':
					return this.jjMoveStringLiteralDfa12_0(active1, 8858370048L);
				case 'i':
					return this.jjMoveStringLiteralDfa12_0(active1, 141312L);
				case 'k':
					return this.jjMoveStringLiteralDfa12_0(active1, 274877906944L);
				case 'l':
					return this.jjMoveStringLiteralDfa12_0(active1, 2286984202551296L);
				case 'm':
					return this.jjMoveStringLiteralDfa12_0(active1, 134217728L);
				case 'n':
					return this.jjMoveStringLiteralDfa12_0(active1, 108086427564114080L);
				case 'o':
					return this.jjMoveStringLiteralDfa12_0(active1, 1074286656L);
				case 'r':
					return this.jjMoveStringLiteralDfa12_0(active1, 23158463660032L);
				case 's':
					if ((active1 & 8L) != 0L) {
						return this.jjStopAtPos(11, 67);
					}

					return this.jjMoveStringLiteralDfa12_0(active1, 284361262892032L);
				case 't':
					return this.jjMoveStringLiteralDfa12_0(active1, 10133099698454528L);
				case 'u':
					return this.jjMoveStringLiteralDfa12_0(active1, 18014398509547520L);
				case 'z':
					return this.jjMoveStringLiteralDfa12_0(active1, 2097664L);
			}
		}
	}

	private int jjMoveStringLiteralDfa29_0(long old0, long active0) {
		if ((active0 = active0 & old0) == 0L) {
			return this.jjStartNfa_0(27, 0L, old0);
		} else {
			try {
				this.curChar = this.input_stream.readChar();
			} catch (IOException ignored) {
				this.jjStopStringLiteralDfa_0(28, 0L, active0);
				return 29;
			}

			switch (this.curChar) {
				case 's':
					return this.jjMoveStringLiteralDfa30_0(active0, 108086391056891904L);
				default:
					return this.jjStartNfa_0(28, 0L, active0);
			}
		}
	}

	private int jjMoveStringLiteralDfa6_0(long old0, long active0, long old1, long active1) {
		if (((active0 = active0 & old0) | (active1 = active1 & old1)) == 0L) {
			return this.jjStartNfa_0(4, old0, old1);
		} else {
			try {
				this.curChar = this.input_stream.readChar();
			} catch (IOException ignored) {
				this.jjStopStringLiteralDfa_0(5, active0, active1);
				return 6;
			}

			switch (this.curChar) {
				case '-':
					return this.jjMoveStringLiteralDfa7_0(active0, 4611686018427387904L, active1, 0L);
				case '.':
				case '/':
				case '0':
				case '1':
				case '2':
				case '3':
				case '4':
				case '5':
				case '6':
				case '7':
				case '8':
				case '9':
				case ':':
				case ';':
				case '<':
				case '=':
				case '>':
				case '?':
				case '@':
				case 'A':
				case 'B':
				case 'C':
				case 'D':
				case 'E':
				case 'F':
				case 'G':
				case 'H':
				case 'I':
				case 'J':
				case 'K':
				case 'L':
				case 'M':
				case 'N':
				case 'O':
				case 'P':
				case 'Q':
				case 'R':
				case 'S':
				case 'T':
				case 'U':
				case 'V':
				case 'W':
				case 'X':
				case 'Y':
				case 'Z':
				case '[':
				case '\\':
				case ']':
				case '^':
				case '_':
				case '`':
				case 'j':
				case 'q':
				default:
					return this.jjStartNfa_0(5, active0, active1);
				case 'a':
					return this.jjMoveStringLiteralDfa7_0(active0, 2603080584620146688L, active1, 550301073408L);
				case 'b':
					return this.jjMoveStringLiteralDfa7_0(active0, 0L, active1, 294912L);
				case 'c':
					return this.jjMoveStringLiteralDfa7_0(active0, 1125899906842624L, active1, 34695282688L);
				case 'd':
					return this.jjMoveStringLiteralDfa7_0(active0, 633318697598976L, active1, 0L);
				case 'e':
					if ((active0 & 274877906944L) != 0L) {
						return this.jjStartNfaWithStates_0(6, 38, 13);
					}

					return this.jjMoveStringLiteralDfa7_0(active0, -8065946932620558336L, active1, 10213642716971328L);
				case 'f':
					return this.jjMoveStringLiteralDfa7_0(active0, 576469548396445696L, active1, 0L);
				case 'g':
					return this.jjMoveStringLiteralDfa7_0(active0, 0L, active1, 18014398509481984L);
				case 'h':
					return this.jjMoveStringLiteralDfa7_0(active0, 0L, active1, 2050L);
				case 'i':
					return this.jjMoveStringLiteralDfa7_0(active0, 74309393851613184L, active1, 4538785073745920L);
				case 'k':
					return this.jjMoveStringLiteralDfa7_0(active0, 0L, active1, 108086393221152768L);
				case 'l':
					return this.jjMoveStringLiteralDfa7_0(active0, 4398046511104L, active1, 281629599727616L);
				case 'm':
					return this.jjMoveStringLiteralDfa7_0(active0, 0L, active1, 160L);
				case 'n':
					return this.jjMoveStringLiteralDfa7_0(active0, 0L, active1, 4398046511104L);
				case 'o':
					return this.jjMoveStringLiteralDfa7_0(active0, 180143985094819840L, active1, 2814758491259920L);
				case 'p':
					return this.jjMoveStringLiteralDfa7_0(active0, 0L, active1, 3146240L);
				case 'r':
					return this.jjMoveStringLiteralDfa7_0(active0, 140737488355328L, active1, 19859928784904L);
				case 's':
					if ((active0 & 549755813888L) != 0L) {
						return this.jjStartNfaWithStates_0(6, 39, 13);
					} else {
						if ((active0 & 2199023255552L) != 0L) {
							return this.jjStopAtPos(6, 41);
						}

						return this.jjMoveStringLiteralDfa7_0(active0, 52776558133248L, active1, 140737488355329L);
					}
				case 't':
					if ((active0 & 1099511627776L) != 0L) {
						return this.jjStopAtPos(6, 40);
					}

					return this.jjMoveStringLiteralDfa7_0(active0, 18295873486192640L, active1, 65536L);
				case 'u':
					return this.jjMoveStringLiteralDfa7_0(active0, 0L, active1, 4L);
			}
		}
	}

	private int jjMoveStringLiteralDfa21_0(long old0, long active0) {
		if ((active0 = active0 & old0) == 0L) {
			return this.jjStartNfa_0(19, 0L, old0);
		} else {
			try {
				this.curChar = this.input_stream.readChar();
			} catch (IOException ignored) {
				this.jjStopStringLiteralDfa_0(20, 0L, active0);
				return 21;
			}

			switch (this.curChar) {
				case 'a':
					return this.jjMoveStringLiteralDfa22_0(active0, 2199023255552L);
				case 'c':
					return this.jjMoveStringLiteralDfa22_0(active0, 20266473201074176L);
				case 'e':
					return this.jjMoveStringLiteralDfa22_0(active0, 1125968626319360L);
				case 'g':
					return this.jjMoveStringLiteralDfa22_0(active0, 4398046511104L);
				case 'i':
					return this.jjMoveStringLiteralDfa22_0(active0, 572295802257408L);
				case 'm':
					return this.jjMoveStringLiteralDfa22_0(active0, 70368744177664L);
				case 'n':
					return this.jjMoveStringLiteralDfa22_0(active0, 35184372088832L);
				case 'r':
					return this.jjMoveStringLiteralDfa22_0(active0, 117375202727297024L);
				case 's':
					return this.jjMoveStringLiteralDfa22_0(active0, 4645436627353600L);
				case 't':
					return this.jjMoveStringLiteralDfa22_0(active0, 17592186044416L);
				case 'y':
					if ((active0 & 34359738368L) != 0L) {
						return this.jjStopAtPos(21, 99);
					}
				case 'b':
				case 'd':
				case 'f':
				case 'h':
				case 'j':
				case 'k':
				case 'l':
				case 'o':
				case 'p':
				case 'q':
				case 'u':
				case 'v':
				case 'w':
				case 'x':
				default:
					return this.jjStartNfa_0(20, 0L, active0);
			}
		}
	}

	private int jjMoveStringLiteralDfa4_0(long old0, long active0, long old1, long active1) {
		if (((active0 = active0 & old0) | (active1 = active1 & old1)) == 0L) {
			return this.jjStartNfa_0(2, old0, old1);
		} else {
			try {
				this.curChar = this.input_stream.readChar();
			} catch (IOException ignored) {
				this.jjStopStringLiteralDfa_0(3, active0, active1);
				return 4;
			}

			switch (this.curChar) {
				case 'a':
					return this.jjMoveStringLiteralDfa5_0(active0, 72060067939090432L, active1, 8796143353856L);
				case 'b':
					return this.jjMoveStringLiteralDfa5_0(active0, 35184372088832L, active1, 0L);
				case 'c':
					return this.jjMoveStringLiteralDfa5_0(active0, 8796093022208L, active1, 4398047559680L);
				case 'd':
				case 'f':
				case 'm':
				case 'q':
				default:
					return this.jjStartNfa_0(3, active0, active1);
				case 'e':
					return this.jjMoveStringLiteralDfa5_0(active0, 1729663731886981120L, active1, 16384L);
				case 'g':
					return this.jjMoveStringLiteralDfa5_0(active0, 1236950581248L, active1, 4503599627370496L);
				case 'h':
					return this.jjMoveStringLiteralDfa5_0(active0, 18577348462903296L, active1, 16L);
				case 'i':
					return this.jjMoveStringLiteralDfa5_0(active0, 25769803776L, active1, 1073745920L);
				case 'j':
					return this.jjMoveStringLiteralDfa5_0(active0, 140737488355328L, active1, 0L);
				case 'k':
					return this.jjMoveStringLiteralDfa5_0(active0, 0L, active1, 18014398509481984L);
				case 'l':
					if ((active0 & 4294967296L) != 0L) {
						return this.jjStartNfaWithStates_0(4, 32, 13);
					}

					return this.jjMoveStringLiteralDfa5_0(active0, 4611773979357609984L, active1, 32L);
				case 'n':
					return this.jjMoveStringLiteralDfa5_0(active0, 549755813888L, active1, 268435589L);
				case 'o':
					return this.jjMoveStringLiteralDfa5_0(active0, 0L, active1, 549755944960L);
				case 'p':
					if ((active0 & 536870912L) != 0L) {
						return this.jjStopAtPos(4, 29);
					}

					if ((active0 & 1073741824L) != 0L) {
						this.jjmatchedKind = 30;
						this.jjmatchedPos = 4;
					}

					return this.jjMoveStringLiteralDfa5_0(active0, 2341871806232657920L, active1, 2551093213593600L);
				case 'r':
					return this.jjMoveStringLiteralDfa5_0(active0, 12384898975268864L, active1, 8589934664L);
				case 's':
					if ((active0 & 2147483648L) != 0L) {
						return this.jjStartNfaWithStates_0(4, 31, 13);
					}

					return this.jjMoveStringLiteralDfa5_0(active0, 4503599627370496L, active1, 562949953421312L);
				case 't':
					return this.jjMoveStringLiteralDfa5_0(active0, 432350030993555456L, active1, 108229602446452226L);
				case 'u':
					return this.jjMoveStringLiteralDfa5_0(active0, Long.MIN_VALUE, active1, 10239790444183808L);
				case 'v':
					return this.jjMoveStringLiteralDfa5_0(active0, 34359738368L, active1, 0L);
				case 'w':
					return this.jjMoveStringLiteralDfa5_0(active0, 0L, active1, 2361344L);
			}
		}
	}

	public void SkipLexicalActions(ProguardToken matchedToken) {
	}

	private int jjMoveStringLiteralDfa23_0(long old0, long active0) {
		if ((active0 = active0 & old0) == 0L) {
			return this.jjStartNfa_0(21, 0L, old0);
		} else {
			try {
				this.curChar = this.input_stream.readChar();
			} catch (IOException ignored) {
				this.jjStopStringLiteralDfa_0(22, 0L, active0);
				return 23;
			}

			switch (this.curChar) {
				case 'a':
					return this.jjMoveStringLiteralDfa24_0(active0, 2676211302006784L);
				case 'b':
				case 'c':
				case 'd':
				case 'e':
				case 'g':
				case 'h':
				case 'j':
				case 'k':
				case 'l':
				case 'o':
				case 'p':
				case 'q':
				case 'w':
				case 'x':
				default:
					break;
				case 'f':
					return this.jjMoveStringLiteralDfa24_0(active0, 1125899906842624L);
				case 'i':
					return this.jjMoveStringLiteralDfa24_0(active0, 22522396183363584L);
				case 'm':
					return this.jjMoveStringLiteralDfa24_0(active0, 35184372088832L);
				case 'n':
					if ((active0 & 549755813888L) != 0L) {
						return this.jjStopAtPos(23, 103);
					}

					return this.jjMoveStringLiteralDfa24_0(active0, 580542139465728L);
				case 'r':
					return this.jjMoveStringLiteralDfa24_0(active0, 108086391056891904L);
				case 's':
					if ((active0 & 1099511627776L) != 0L) {
						return this.jjStartNfaWithStates_0(23, 104, 13);
					}
					break;
				case 't':
					return this.jjMoveStringLiteralDfa24_0(active0, 70368744177664L);
				case 'u':
					return this.jjMoveStringLiteralDfa24_0(active0, 8796093022208L);
				case 'v':
					return this.jjMoveStringLiteralDfa24_0(active0, 9007199254740992L);
				case 'y':
					if ((active0 & 274877906944L) != 0L) {
						return this.jjStopAtPos(23, 102);
					}
			}

			return this.jjStartNfa_0(22, 0L, active0);
		}
	}

	private int jjMoveStringLiteralDfa33_0(long old0, long active0) {
		if ((active0 = active0 & old0) == 0L) {
			return this.jjStartNfa_0(31, 0L, old0);
		} else {
			try {
				this.curChar = this.input_stream.readChar();
			} catch (IOException ignored) {
				this.jjStopStringLiteralDfa_0(32, 0L, active0);
				return 33;
			}

			switch (this.curChar) {
				case 'b':
					return this.jjMoveStringLiteralDfa34_0(active0, 72057594037927936L);
				default:
					return this.jjStartNfa_0(32, 0L, active0);
			}
		}
	}

	private int jjMoveStringLiteralDfa32_0(long old0, long active0) {
		if ((active0 = active0 & old0) == 0L) {
			return this.jjStartNfa_0(30, 0L, old0);
		} else {
			try {
				this.curChar = this.input_stream.readChar();
			} catch (IOException ignored) {
				this.jjStopStringLiteralDfa_0(31, 0L, active0);
				return 32;
			}

			switch (this.curChar) {
				case 'm':
					return this.jjMoveStringLiteralDfa33_0(active0, 72057594037927936L);
				default:
					return this.jjStartNfa_0(31, 0L, active0);
			}
		}
	}

	private int jjMoveStringLiteralDfa16_0(long old0, long active0) {
		if ((active0 = active0 & old0) == 0L) {
			return this.jjStartNfa_0(14, 0L, old0);
		} else {
			try {
				this.curChar = this.input_stream.readChar();
			} catch (IOException ignored) {
				this.jjStopStringLiteralDfa_0(15, 0L, active0);
				return 16;
			}

			switch (this.curChar) {
				case 'a':
					return this.jjMoveStringLiteralDfa17_0(active0, 4503599761588224L);
				case 'b':
					return this.jjMoveStringLiteralDfa17_0(active0, 2251799813685248L);
				case 'c':
				case 'h':
				case 'j':
				case 'k':
				case 'o':
				case 'p':
				case 'q':
				default:
					return this.jjStartNfa_0(15, 0L, active0);
				case 'd':
					return this.jjMoveStringLiteralDfa17_0(active0, 2147483648L);
				case 'e':
					return this.jjMoveStringLiteralDfa17_0(active0, 184722248433664L);
				case 'f':
					return this.jjMoveStringLiteralDfa17_0(active0, 549755813888L);
				case 'g':
					if ((active0 & 33554432L) != 0L) {
						return this.jjStopAtPos(16, 89);
					}

					return this.jjMoveStringLiteralDfa17_0(active0, 70368811286528L);
				case 'i':
					return this.jjMoveStringLiteralDfa17_0(active0, 126103306685644800L);
				case 'l':
					return this.jjMoveStringLiteralDfa17_0(active0, 10150760067104768L);
				case 'm':
					return this.jjMoveStringLiteralDfa17_0(active0, 281612952535040L);
				case 'n':
					if ((active0 & 2097152L) != 0L) {
						return this.jjStartNfaWithStates_0(16, 85, 13);
					}

					return this.jjMoveStringLiteralDfa17_0(active0, 567365179801600L);
				case 'r':
					return this.jjMoveStringLiteralDfa17_0(active0, 1099511627776L);
				case 's':
					if ((active0 & 4194304L) != 0L) {
						return this.jjStopAtPos(16, 86);
					} else if ((active0 & 8388608L) != 0L) {
						return this.jjStopAtPos(16, 87);
					} else {
						return (active0 & 16777216L) != 0L ? this.jjStopAtPos(16, 88) : this.jjMoveStringLiteralDfa17_0(active0, 1073741824L);
					}
			}
		}
	}

	private int jjMoveStringLiteralDfa34_0(long old0, long active0) {
		if ((active0 = active0 & old0) == 0L) {
			return this.jjStartNfa_0(32, 0L, old0);
		} else {
			try {
				this.curChar = this.input_stream.readChar();
			} catch (IOException ignored) {
				this.jjStopStringLiteralDfa_0(33, 0L, active0);
				return 34;
			}

			switch (this.curChar) {
				case 'e':
					return this.jjMoveStringLiteralDfa35_0(active0, 72057594037927936L);
				default:
					return this.jjStartNfa_0(33, 0L, active0);
			}
		}
	}

	private void jjCheckNAdd(int state) {
		if (this.jjrounds[state] != this.jjround) {
			this.jjstateSet[this.jjnewStateCnt++] = state;
			this.jjrounds[state] = this.jjround;
		}
	}

	private int jjMoveStringLiteralDfa26_0(long old0, long active0) {
		if ((active0 = active0 & old0) == 0L) {
			return this.jjStartNfa_0(24, 0L, old0);
		} else {
			try {
				this.curChar = this.input_stream.readChar();
			} catch (IOException ignored) {
				this.jjStopStringLiteralDfa_0(25, 0L, active0);
				return 26;
			}

			switch (this.curChar) {
				case 'a':
					return this.jjMoveStringLiteralDfa27_0(active0, 18014398509481984L);
				case 'b':
				case 'c':
				case 'd':
				case 'f':
				case 'g':
				case 'h':
				case 'i':
				case 'j':
				case 'k':
				case 'm':
				case 'n':
				case 'o':
				case 'p':
				case 'q':
				case 'r':
				case 'v':
				case 'w':
				case 'x':
				default:
					break;
				case 'e':
					return this.jjMoveStringLiteralDfa27_0(active0, 2251799813685248L);
				case 'l':
					return this.jjMoveStringLiteralDfa27_0(active0, 112589990684262400L);
				case 's':
					if ((active0 & 70368744177664L) != 0L) {
						return this.jjStopAtPos(26, 110);
					}

					if ((active0 & 140737488355328L) != 0L) {
						return this.jjStopAtPos(26, 111);
					}

					if ((active0 & 281474976710656L) != 0L) {
						return this.jjStopAtPos(26, 112);
					}
					break;
				case 't':
					return this.jjMoveStringLiteralDfa27_0(active0, 1125899906842624L);
				case 'u':
					return this.jjMoveStringLiteralDfa27_0(active0, 9007199254740992L);
				case 'y':
					if ((active0 & 562949953421312L) != 0L) {
						return this.jjStopAtPos(26, 113);
					}
			}

			return this.jjStartNfa_0(25, 0L, active0);
		}
	}

	public ProguardToken getNextToken() {
		ProguardToken specialToken = null;
		int curPos = 0;

		label119:
		while (true) {
			try {
				this.curChar = this.input_stream.BeginToken();
			} catch (IOException ignoredBeginToken) {
				this.jjmatchedKind = 0;
				ProguardToken eofToken = this.jjFillToken();
				eofToken.specialToken = specialToken;
				return eofToken;
			}

			this.image = this.jjimage;
			this.image.setLength(0);
			this.jjimageLen = 0;

			while (true) {
				switch (this.curLexState) {
					case 0:
						try {
							this.input_stream.backup(0);

							while (this.curChar <= ' ' && (4294981120L & 1L << this.curChar) != 0L) {
								this.curChar = this.input_stream.BeginToken();
							}
						} catch (IOException ignoredSkipWhitespace) {
							continue label119;
						}

						this.jjmatchedKind = Integer.MAX_VALUE;
						this.jjmatchedPos = 0;
						curPos = this.jjMoveStringLiteralDfa0_0();
						break;
					case 1:
						this.jjmatchedKind = Integer.MAX_VALUE;
						this.jjmatchedPos = 0;
						curPos = this.jjMoveStringLiteralDfa0_1();
						if (this.jjmatchedPos == 0 && this.jjmatchedKind > 8) {
							this.jjmatchedKind = 8;
						}
						break;
					case 2:
						this.jjmatchedKind = Integer.MAX_VALUE;
						this.jjmatchedPos = 0;
						curPos = this.jjMoveStringLiteralDfa0_2();
				}

				if (this.jjmatchedKind == Integer.MAX_VALUE) {
					break label119;
				}

				if (this.jjmatchedPos + 1 < curPos) {
					this.input_stream.backup(curPos - this.jjmatchedPos - 1);
				}

				if ((jjtoToken[this.jjmatchedKind >> 6] & 1L << (this.jjmatchedKind & 63)) != 0L) {
					ProguardToken matchedToken = this.jjFillToken();
					matchedToken.specialToken = specialToken;
					if (jjnewLexState[this.jjmatchedKind] != -1) {
						this.curLexState = jjnewLexState[this.jjmatchedKind];
					}

					return matchedToken;
				}

				if ((jjtoSkip[this.jjmatchedKind >> 6] & 1L << (this.jjmatchedKind & 63)) != 0L) {
					if ((jjtoSpecial[this.jjmatchedKind >> 6] & 1L << (this.jjmatchedKind & 63)) != 0L) {
						ProguardToken newSpecialToken = this.jjFillToken();
						if (specialToken == null) {
							specialToken = newSpecialToken;
						} else {
							newSpecialToken.specialToken = specialToken;
							specialToken = specialToken.next = newSpecialToken;
						}

						this.SkipLexicalActions(newSpecialToken);
					} else {
						this.SkipLexicalActions(null);
					}

					if (jjnewLexState[this.jjmatchedKind] != -1) {
						this.curLexState = jjnewLexState[this.jjmatchedKind];
					}
					break;
				}

				this.jjimageLen = this.jjimageLen + this.jjmatchedPos + 1;
				if (jjnewLexState[this.jjmatchedKind] != -1) {
					this.curLexState = jjnewLexState[this.jjmatchedKind];
				}

				curPos = 0;
				this.jjmatchedKind = Integer.MAX_VALUE;

				try {
					this.curChar = this.input_stream.readChar();
				} catch (IOException ignoredReadChar) {
					break label119;
				}
			}
		}

		int errorLine = this.input_stream.getEndLine();
		int errorColumn = this.input_stream.getEndColumn();
		String errorAfter = null;
		boolean EOFSeen = false;

		try {
			this.input_stream.readChar();
			this.input_stream.backup(1);
		} catch (IOException ignoredEofProbe) {
			EOFSeen = true;
			errorAfter = curPos <= 1 ? "" : this.input_stream.GetImage();
			if (this.curChar != '\n' && this.curChar != '\r') {
				errorColumn++;
			} else {
				errorLine++;
				errorColumn = 0;
			}
		}

		if (!EOFSeen) {
			this.input_stream.backup(1);
			errorAfter = curPos <= 1 ? "" : this.input_stream.GetImage();
		}

		throw new ProguardLexicalError(EOFSeen, this.curLexState, errorLine, errorColumn, errorAfter, this.curChar, 0);
	}

	private int jjMoveStringLiteralDfa7_0(long old0, long active0, long old1, long active1) {
		if (((active0 = active0 & old0) | (active1 = active1 & old1)) == 0L) {
			return this.jjStartNfa_0(5, old0, old1);
		} else {
			try {
				this.curChar = this.input_stream.readChar();
			} catch (IOException ignored) {
				this.jjStopStringLiteralDfa_0(6, active0, active1);
				return 7;
			}

			switch (this.curChar) {
				case '>':
					if ((active0 & 17592186044416L) != 0L) {
						return this.jjStopAtPos(7, 44);
					}
				case '?':
				case '@':
				case 'A':
				case 'B':
				case 'C':
				case 'D':
				case 'E':
				case 'F':
				case 'G':
				case 'H':
				case 'I':
				case 'J':
				case 'K':
				case 'L':
				case 'M':
				case 'N':
				case 'O':
				case 'P':
				case 'Q':
				case 'R':
				case 'S':
				case 'T':
				case 'U':
				case 'V':
				case 'W':
				case 'X':
				case 'Y':
				case 'Z':
				case '[':
				case '\\':
				case ']':
				case '^':
				case '_':
				case '`':
				case 'h':
				case 'j':
				case 'k':
				case 'u':
				case 'x':
				default:
					break;
				case 'a':
					return this.jjMoveStringLiteralDfa8_0(active0, 576460752303423488L, active1, 281672566177952L);
				case 'b':
					return this.jjMoveStringLiteralDfa8_0(active0, 0L, active1, 562949953421312L);
				case 'c':
					return this.jjMoveStringLiteralDfa8_0(active0, -9214364837600034816L, active1, 549764202496L);
				case 'd':
					if ((active0 & 2251799813685248L) != 0L) {
						return this.jjStopAtPos(7, 51);
					}

					return this.jjMoveStringLiteralDfa8_0(active0, 36028797018963968L, active1, 1099511627840L);
				case 'e':
					if ((active0 & 4398046511104L) != 0L) {
						return this.jjStartNfaWithStates_0(7, 42, 13);
					}

					if ((active0 & 35184372088832L) != 0L) {
						return this.jjStopAtPos(7, 45);
					}

					if ((active0 & 70368744177664L) != 0L) {
						return this.jjStopAtPos(7, 46);
					}

					return this.jjMoveStringLiteralDfa8_0(active0, 281474976710656L, active1, 18172796903366657L);
				case 'f':
					return this.jjMoveStringLiteralDfa8_0(active0, 0L, active1, 4398046806016L);
				case 'g':
					return this.jjMoveStringLiteralDfa8_0(active0, 72057594037927936L, active1, 0L);
				case 'i':
					return this.jjMoveStringLiteralDfa8_0(active0, 4629700416936869888L, active1, 108086391056891904L);
				case 'l':
					return this.jjMoveStringLiteralDfa8_0(active0, 0L, active1, 67108864L);
				case 'm':
					return this.jjMoveStringLiteralDfa8_0(active0, 2305843009213693952L, active1, 1024L);
				case 'n':
					return this.jjMoveStringLiteralDfa8_0(active0, 1157425104234217472L, active1, 16959146519691280L);
				case 'o':
					return this.jjMoveStringLiteralDfa8_0(active0, 0L, active1, 2201439174656L);
				case 'p':
					if ((active0 & 8796093022208L) != 0L) {
						return this.jjStartNfaWithStates_0(7, 43, 13);
					}
					break;
				case 'q':
					return this.jjMoveStringLiteralDfa8_0(active0, 0L, active1, 35184372088832L);
				case 'r':
					return this.jjMoveStringLiteralDfa8_0(active0, 288230376151711744L, active1, 538462210L);
				case 's':
					if ((active0 & 140737488355328L) != 0L) {
						return this.jjStopAtPos(7, 47);
					}

					return this.jjMoveStringLiteralDfa8_0(active0, 562949953421312L, active1, 8796093022212L);
				case 't':
					if ((active0 & 1125899906842624L) != 0L) {
						return this.jjStartNfaWithStates_0(7, 50, 13);
					}

					return this.jjMoveStringLiteralDfa8_0(active0, 144115188075855872L, active1, 136380928L);
				case 'v':
					return this.jjMoveStringLiteralDfa8_0(active0, 0L, active1, 256L);
				case 'w':
					return this.jjMoveStringLiteralDfa8_0(active0, 0L, active1, 131072L);
				case 'y':
					return this.jjMoveStringLiteralDfa8_0(active0, 0L, active1, 33554440L);
				case 'z':
					return this.jjMoveStringLiteralDfa8_0(active0, 0L, active1, 1073745920L);
			}

			return this.jjStartNfa_0(6, active0, active1);
		}
	}

	private int jjMoveNfa_1(int startState, int curPos) {
		int startsAt = 0;
		this.jjnewStateCnt = 3;
		int stateIndex = 1;
		this.jjstateSet[0] = startState;
		int kind = Integer.MAX_VALUE;

		while (true) {
			if (++this.jjround == Integer.MAX_VALUE) {
				this.ReInitRounds();
			}

			if (this.curChar < '@') {
				long charBitMask = 1L << this.curChar;

				do {
					stateIndex--;
					switch (this.jjstateSet[stateIndex]) {
						case 0:
							if ((13312L & charBitMask) != 0L && kind > 7) {
								kind = 7;
							}

							if (this.curChar == '\r') {
								this.jjstateSet[this.jjnewStateCnt++] = 1;
							}
							break;
						case 1:
							if (this.curChar == '\n' && kind > 7) {
								kind = 7;
							}
							break;
						case 2:
							if (this.curChar == '\r') {
								this.jjstateSet[this.jjnewStateCnt++] = 1;
							}
					}
				} while (stateIndex != startsAt);
			} else if (this.curChar < 128) {
				long maskedCharBitMask = 1L << (this.curChar & '?');

				do {
					int currentState = this.jjstateSet[--stateIndex];
				} while (stateIndex != startsAt);
			} else {
				int highByte = this.curChar >> '\b';
				int byteWordIndex = highByte >> 6;
				long highByteMask = 1L << (highByte & 63);
				byteWordIndex = (this.curChar & 255) >> 6;
				long lowByteMask = 1L << (this.curChar & '?');

				do {
					int wideCharState = this.jjstateSet[--stateIndex];
				} while (stateIndex != startsAt);
			}

			if (kind != Integer.MAX_VALUE) {
				this.jjmatchedKind = kind;
				this.jjmatchedPos = curPos;
				kind = Integer.MAX_VALUE;
			}

			curPos++;
			if ((stateIndex = this.jjnewStateCnt) == (startsAt = 3 - (this.jjnewStateCnt = startsAt))) {
				return curPos;
			}

			try {
				this.curChar = this.input_stream.readChar();
			} catch (IOException ignored) {
				return curPos;
			}
		}
	}

	private void ReInitRounds() {
		this.jjround = -2147483647;
		int i = 12;

		while (i-- > 0) {
			this.jjrounds[i] = Integer.MIN_VALUE;
		}
	}

	private final int jjStopStringLiteralDfa_0(int pos, long active0, long active1) {
		switch (pos) {
			case 0:
				if ((active0 & 218792960L) != 0L) {
					return 12;
				} else {
					if ((active0 & -3425817718492758016L) == 0L && (active1 & 1099513989136L) == 0L) {
						if ((active0 & 580610858942464L) != 0L) {
							return 4;
						}

						if ((active0 & 262144L) != 0L) {
							return 8;
						}

						return -1;
					}

					this.jjmatchedKind = 125;
					return 13;
				}
			case 1:
				if ((active0 & 83886080L) != 0L) {
					return 12;
				} else if ((active0 & 134217728L) != 0L) {
					if (this.jjmatchedPos != 1) {
						this.jjmatchedKind = 126;
						this.jjmatchedPos = 1;
					}

					return 12;
				} else {
					if ((active0 & -3425817718492758016L) == 0L && (active1 & 1099513989136L) == 0L) {
						return -1;
					}

					if (this.jjmatchedPos != 1) {
						this.jjmatchedKind = 125;
						this.jjmatchedPos = 1;
					}

					return 13;
				}
			case 2:
				if ((active0 & 201326592L) != 0L) {
					return 12;
				} else {
					if ((active0 & -3425817718492758016L) == 0L && (active1 & 1099513989136L) == 0L) {
						return -1;
					}

					this.jjmatchedKind = 125;
					this.jjmatchedPos = 2;
					return 13;
				}
			case 3:
				if ((active0 & -3425817718761193472L) == 0L && (active1 & 1099513989136L) == 0L) {
					if ((active0 & 268435456L) != 0L) {
						return 13;
					}

					return -1;
				}

				this.jjmatchedKind = 125;
				this.jjmatchedPos = 3;
				return 13;
			case 4:
				if ((active0 & -3425817725203644416L) == 0L && (active1 & 1099513989136L) == 0L) {
					if ((active0 & 6442450944L) != 0L) {
						return 13;
					}

					return -1;
				}

				if (this.jjmatchedPos != 4) {
					this.jjmatchedKind = 125;
					this.jjmatchedPos = 4;
				}

				return 13;
			case 5:
				if ((active0 & -3425817922772140032L) == 0L && (active1 & 1099513989136L) == 0L) {
					if ((active0 & 197568495616L) != 0L) {
						return 13;
					}

					return -1;
				}

				this.jjmatchedKind = 125;
				this.jjmatchedPos = 5;
				return 13;
			case 6:
				if ((active0 & 4611686018427387904L) != 0L) {
					if (this.jjmatchedPos < 5) {
						this.jjmatchedKind = 125;
						this.jjmatchedPos = 5;
					}

					return -1;
				} else {
					if ((active0 & -8037504765833248768L) == 0L && (active1 & 1099513989136L) == 0L) {
						if ((active0 & 824633720832L) != 0L) {
							return 13;
						}

						return -1;
					}

					this.jjmatchedKind = 125;
					this.jjmatchedPos = 6;
					return 13;
				}
			case 7:
				if ((active0 & -8038643859879624704L) == 0L && (active1 & 1099513989136L) == 0L) {
					if ((active0 & 4611686018427387904L) != 0L) {
						if (this.jjmatchedPos < 5) {
							this.jjmatchedKind = 125;
							this.jjmatchedPos = 5;
						}

						return -1;
					}

					if ((active0 & 1139094046375936L) != 0L) {
						return 13;
					}

					return -1;
				}

				this.jjmatchedKind = 125;
				this.jjmatchedPos = 7;
				return 13;
			case 8:
				if ((active0 & -8070450532247928832L) == 0L && (active1 & 1099513989136L) == 0L) {
					if ((active0 & 4611686018427387904L) != 0L) {
						if (this.jjmatchedPos < 5) {
							this.jjmatchedKind = 125;
							this.jjmatchedPos = 5;
						}

						return -1;
					}

					if ((active0 & 31806672368304128L) != 0L) {
						return 13;
					}

					return -1;
				}

				this.jjmatchedKind = 125;
				this.jjmatchedPos = 8;
				return 13;
			case 9:
				if ((active0 & 4611686018427387904L) != 0L) {
					if (this.jjmatchedPos < 5) {
						this.jjmatchedKind = 125;
						this.jjmatchedPos = 5;
					}

					return -1;
				} else {
					if ((active0 & Long.MIN_VALUE) == 0L && (active1 & 1099513989136L) == 0L) {
						if ((active0 & 1152921504606846976L) != 0L) {
							return 13;
						}

						return -1;
					}

					this.jjmatchedKind = 125;
					this.jjmatchedPos = 9;
					return 13;
				}
			case 10:
				if ((active0 & 4611686018427387904L) != 0L) {
					if (this.jjmatchedPos < 5) {
						this.jjmatchedKind = 125;
						this.jjmatchedPos = 5;
					}

					return -1;
				} else if ((active1 & 1099513989136L) != 0L) {
					this.jjmatchedKind = 125;
					this.jjmatchedPos = 10;
					return 13;
				} else {
					if ((active0 & Long.MIN_VALUE) != 0L) {
						return 13;
					}

					return -1;
				}
			case 11:
				if ((active1 & 1099513989120L) != 0L) {
					this.jjmatchedKind = 125;
					this.jjmatchedPos = 11;
					return 13;
				} else {
					if ((active1 & 16L) != 0L) {
						return 13;
					}

					return -1;
				}
			case 12:
				if ((active1 & 1099513989120L) != 0L) {
					this.jjmatchedKind = 125;
					this.jjmatchedPos = 12;
					return 13;
				}

				return -1;
			case 13:
				if ((active1 & 1099513987072L) != 0L) {
					this.jjmatchedKind = 125;
					this.jjmatchedPos = 13;
					return 13;
				} else {
					if ((active1 & 2048L) != 0L) {
						return 13;
					}

					return -1;
				}
			case 14:
				if ((active1 & 1099513987072L) != 0L) {
					this.jjmatchedKind = 125;
					this.jjmatchedPos = 14;
					return 13;
				}

				return -1;
			case 15:
				if ((active1 & 1099513724928L) != 0L) {
					this.jjmatchedKind = 125;
					this.jjmatchedPos = 15;
					return 13;
				} else {
					if ((active1 & 262144L) != 0L) {
						return 13;
					}

					return -1;
				}
			case 16:
				if ((active1 & 1099511627776L) != 0L) {
					this.jjmatchedKind = 125;
					this.jjmatchedPos = 16;
					return 13;
				} else {
					if ((active1 & 2097152L) != 0L) {
						return 13;
					}

					return -1;
				}
			case 17:
				if ((active1 & 1099511627776L) != 0L) {
					this.jjmatchedKind = 125;
					this.jjmatchedPos = 17;
					return 13;
				}

				return -1;
			case 18:
				if ((active1 & 1099511627776L) != 0L) {
					this.jjmatchedKind = 125;
					this.jjmatchedPos = 18;
					return 13;
				}

				return -1;
			case 19:
				if ((active1 & 1099511627776L) != 0L) {
					this.jjmatchedKind = 125;
					this.jjmatchedPos = 19;
					return 13;
				}

				return -1;
			case 20:
				if ((active1 & 1099511627776L) != 0L) {
					this.jjmatchedKind = 125;
					this.jjmatchedPos = 20;
					return 13;
				}

				return -1;
			case 21:
				if ((active1 & 1099511627776L) != 0L) {
					this.jjmatchedKind = 125;
					this.jjmatchedPos = 21;
					return 13;
				}

				return -1;
			case 22:
				if ((active1 & 1099511627776L) != 0L) {
					this.jjmatchedKind = 125;
					this.jjmatchedPos = 22;
					return 13;
				}

				return -1;
			case 23:
				if ((active1 & 1099511627776L) != 0L) {
					return 13;
				}

				return -1;
			default:
				return -1;
		}
	}

	private int jjMoveStringLiteralDfa28_0(long old0, long active0) {
		if ((active0 = active0 & old0) == 0L) {
			return this.jjStartNfa_0(26, 0L, old0);
		} else {
			try {
				this.curChar = this.input_stream.readChar();
			} catch (IOException ignored) {
				this.jjStopStringLiteralDfa_0(27, 0L, active0);
				return 28;
			}

			switch (this.curChar) {
				case 's':
					if ((active0 & 9007199254740992L) != 0L) {
						return this.jjStopAtPos(28, 117);
					}

					return this.jjMoveStringLiteralDfa29_0(active0, 108086391056891904L);
				case 'y':
					if ((active0 & 18014398509481984L) != 0L) {
						return this.jjStopAtPos(28, 118);
					}
				default:
					return this.jjStartNfa_0(27, 0L, active0);
			}
		}
	}

	private int jjMoveStringLiteralDfa17_0(long old0, long active0) {
		if ((active0 = active0 & old0) == 0L) {
			return this.jjStartNfa_0(15, 0L, old0);
		} else {
			try {
				this.curChar = this.input_stream.readChar();
			} catch (IOException ignored) {
				this.jjStopStringLiteralDfa_0(16, 0L, active0);
				return 17;
			}

			switch (this.curChar) {
				case 'a':
					return this.jjMoveStringLiteralDfa18_0(active0, 8815420375040L);
				case 'b':
				case 'f':
				case 'h':
				case 'j':
				case 'k':
				case 'l':
				case 'q':
				case 'u':
				default:
					return this.jjStartNfa_0(16, 0L, active0);
				case 'c':
					return this.jjMoveStringLiteralDfa18_0(active0, 108228232351842304L);
				case 'd':
					return this.jjMoveStringLiteralDfa18_0(active0, 567347999932416L);
				case 'e':
					return this.jjMoveStringLiteralDfa18_0(active0, 299549809704960L);
				case 'g':
					return this.jjMoveStringLiteralDfa18_0(active0, 4503599627370496L);
				case 'i':
					return this.jjMoveStringLiteralDfa18_0(active0, 549755813888L);
				case 'm':
					return this.jjMoveStringLiteralDfa18_0(active0, 35184372088832L);
				case 'n':
					return this.jjMoveStringLiteralDfa18_0(active0, 2199023255552L);
				case 'o':
					return this.jjMoveStringLiteralDfa18_0(active0, 18014433137655808L);
				case 'p':
					return this.jjMoveStringLiteralDfa18_0(active0, 70368744177664L);
				case 'r':
					return this.jjMoveStringLiteralDfa18_0(active0, 11258999068426240L);
				case 's':
					if ((active0 & 67108864L) != 0L) {
						return this.jjStopAtPos(17, 90);
					}

					return this.jjMoveStringLiteralDfa18_0(active0, 1125899906842624L);
				case 't':
					return this.jjMoveStringLiteralDfa18_0(active0, 134217728L);
				case 'v':
					return this.jjMoveStringLiteralDfa18_0(active0, 8589934592L);
			}
		}
	}

	private int jjMoveStringLiteralDfa2_0(long old0, long active0, long old1, long active1) {
		if (((active0 = active0 & old0) | (active1 = active1 & old1)) == 0L) {
			return this.jjStartNfa_0(0, old0, old1);
		} else {
			try {
				this.curChar = this.input_stream.readChar();
			} catch (IOException ignored) {
				this.jjStopStringLiteralDfa_0(1, active0, active1);
				return 2;
			}

			switch (this.curChar) {
				case '*':
					if ((active0 & 67108864L) != 0L) {
						return this.jjStartNfaWithStates_0(2, 26, 12);
					}
				case '+':
				case ',':
				case '-':
				case '/':
				case '0':
				case '1':
				case '2':
				case '3':
				case '4':
				case '5':
				case '6':
				case '7':
				case '8':
				case '9':
				case ':':
				case ';':
				case '<':
				case '=':
				case '>':
				case '?':
				case '@':
				case 'A':
				case 'B':
				case 'C':
				case 'D':
				case 'E':
				case 'F':
				case 'G':
				case 'H':
				case 'I':
				case 'J':
				case 'K':
				case 'L':
				case 'M':
				case 'N':
				case 'O':
				case 'P':
				case 'Q':
				case 'R':
				case 'S':
				case 'T':
				case 'U':
				case 'V':
				case 'W':
				case 'X':
				case 'Y':
				case 'Z':
				case '[':
				case '\\':
				case ']':
				case '^':
				case '_':
				case '`':
				case 'j':
				case 'm':
				case 'q':
				default:
					break;
				case '.':
					if ((active0 & 134217728L) != 0L) {
						return this.jjStartNfaWithStates_0(2, 27, 12);
					}
					break;
				case 'a':
					return this.jjMoveStringLiteralDfa3_0(active0, 4504718466351104L, active1, 18014398509498368L);
				case 'b':
					return this.jjMoveStringLiteralDfa3_0(active0, 8589934592L, active1, 34359738368L);
				case 'c':
					return this.jjMoveStringLiteralDfa3_0(active0, Long.MIN_VALUE, active1, 1099511627776L);
				case 'd':
					return this.jjMoveStringLiteralDfa3_0(active0, 4611686018427387904L, active1, 22061166624768L);
				case 'e':
					return this.jjMoveStringLiteralDfa3_0(active0, 2342469941631909888L, active1, 4794026016964608L);
				case 'f':
					if ((active0 & 33554432L) != 0L) {
						return this.jjStopAtPos(2, 25);
					}
					break;
				case 'g':
					return this.jjMoveStringLiteralDfa3_0(active0, 0L, active1, 131072L);
				case 'h':
					return this.jjMoveStringLiteralDfa3_0(active0, 0L, active1, 33554432L);
				case 'i':
					return this.jjMoveStringLiteralDfa3_0(active0, 72075598540832768L, active1, 72L);
				case 'k':
					return this.jjMoveStringLiteralDfa3_0(active0, 0L, active1, 2251799813685248L);
				case 'l':
					return this.jjMoveStringLiteralDfa3_0(active0, 4398046511104L, active1, 563774589503488L);
				case 'n':
					return this.jjMoveStringLiteralDfa3_0(active0, 596799591408467968L, active1, 16L);
				case 'o':
					return this.jjMoveStringLiteralDfa3_0(active0, 432627039204278272L, active1, 108229327569593858L);
				case 'p':
					return this.jjMoveStringLiteralDfa3_0(active0, 1152921504606846976L, active1, 1073745952L);
				case 'r':
					return this.jjMoveStringLiteralDfa3_0(active0, 8796093022208L, active1, 268435589L);
				case 's':
					return this.jjMoveStringLiteralDfa3_0(active0, 1125899906842624L, active1, 10238656572817664L);
				case 't':
					return this.jjMoveStringLiteralDfa3_0(active0, 9007783370293248L, active1, 0L);
				case 'u':
					return this.jjMoveStringLiteralDfa3_0(active0, 140738293661696L, active1, 0L);
				case 'v':
					return this.jjMoveStringLiteralDfa3_0(active0, 0L, active1, 8589934592L);
			}

			return this.jjStartNfa_0(1, active0, active1);
		}
	}

	private int jjMoveStringLiteralDfa15_0(long old0, long active0) {
		if ((active0 = active0 & old0) == 0L) {
			return this.jjStartNfa_0(13, 0L, old0);
		} else {
			try {
				this.curChar = this.input_stream.readChar();
			} catch (IOException ignored) {
				this.jjStopStringLiteralDfa_0(14, 0L, active0);
				return 15;
			}

			switch (this.curChar) {
				case 'a':
					return this.jjMoveStringLiteralDfa16_0(active0, 10133101845938176L);
				case 'd':
					return this.jjMoveStringLiteralDfa16_0(active0, 134217728L);
				case 'e':
					return this.jjMoveStringLiteralDfa16_0(active0, 25165824L);
				case 'f':
					return this.jjMoveStringLiteralDfa16_0(active0, 4294967296L);
				case 'g':
					if ((active0 & 1048576L) != 0L) {
						return this.jjStopAtPos(15, 84);
					}
				case 'b':
				case 'c':
				case 'j':
				case 'k':
				case 'p':
				case 'q':
				default:
					return this.jjStartNfa_0(14, 0L, active0);
				case 'h':
					return this.jjMoveStringLiteralDfa16_0(active0, 281887293571072L);
				case 'i':
					return this.jjMoveStringLiteralDfa16_0(active0, 2270010475020288L);
				case 'l':
					return this.jjMoveStringLiteralDfa16_0(active0, 108097386173169664L);
				case 'm':
					return this.jjMoveStringLiteralDfa16_0(active0, 35184372088832L);
				case 'n':
					if ((active0 & 262144L) != 0L) {
						return this.jjStartNfaWithStates_0(15, 82, 13);
					}

					return this.jjMoveStringLiteralDfa16_0(active0, 70368844840960L);
				case 'o':
					return this.jjMoveStringLiteralDfa16_0(active0, 568447513657344L);
				case 'r':
					return this.jjMoveStringLiteralDfa16_0(active0, 17184063488L);
				case 's':
					if ((active0 & 524288L) != 0L) {
						return this.jjStopAtPos(15, 83);
					}

					return this.jjMoveStringLiteralDfa16_0(active0, 4644346779402240L);
				case 't':
					return this.jjMoveStringLiteralDfa16_0(active0, 18014433137655808L);
			}
		}
	}

	private int jjStartNfaWithStates_0(int pos, int kind, int state) {
		this.jjmatchedKind = kind;
		this.jjmatchedPos = pos;

		try {
			this.curChar = this.input_stream.readChar();
		} catch (IOException ignored) {
			return pos + 1;
		}

		return this.jjMoveNfa_0(state, pos + 1);
	}

	private int jjMoveStringLiteralDfa24_0(long old0, long active0) {
		if ((active0 = active0 & old0) == 0L) {
			return this.jjStartNfa_0(22, 0L, old0);
		} else {
			try {
				this.curChar = this.input_stream.readChar();
			} catch (IOException ignored) {
				this.jjStopStringLiteralDfa_0(23, 0L, active0);
				return 24;
			}

			switch (this.curChar) {
				case 'a':
					return this.jjMoveStringLiteralDfa25_0(active0, 9570149208162304L);
				case 'b':
				case 'c':
				case 'd':
				case 'f':
				case 'g':
				case 'h':
				case 'i':
				case 'j':
				case 'k':
				case 'l':
				case 'p':
				case 'q':
				case 'r':
				case 'u':
				case 'w':
				case 'x':
				default:
					return this.jjStartNfa_0(23, 0L, active0);
				case 'e':
					return this.jjMoveStringLiteralDfa25_0(active0, 1231453023109120L);
				case 'm':
					return this.jjMoveStringLiteralDfa25_0(active0, 422212465065984L);
				case 'n':
					return this.jjMoveStringLiteralDfa25_0(active0, 4398046511104L);
				case 'o':
					return this.jjMoveStringLiteralDfa25_0(active0, 18014398509481984L);
				case 's':
					return this.jjMoveStringLiteralDfa25_0(active0, 2251799813685248L);
				case 't':
					return this.jjMoveStringLiteralDfa25_0(active0, 28587302322176L);
				case 'v':
					return this.jjMoveStringLiteralDfa25_0(active0, 4503599627370496L);
				case 'y':
					return this.jjMoveStringLiteralDfa25_0(active0, 108086391056891904L);
			}
		}
	}

	private int jjMoveStringLiteralDfa30_0(long old0, long active0) {
		if ((active0 = active0 & old0) == 0L) {
			return this.jjStartNfa_0(28, 0L, old0);
		} else {
			try {
				this.curChar = this.input_stream.readChar();
			} catch (IOException ignored) {
				this.jjStopStringLiteralDfa_0(29, 0L, active0);
				return 30;
			}

			switch (this.curChar) {
				case 'e':
					return this.jjMoveStringLiteralDfa31_0(active0, 36028797018963968L);
				case 'm':
					return this.jjMoveStringLiteralDfa31_0(active0, 72057594037927936L);
				default:
					return this.jjStartNfa_0(29, 0L, active0);
			}
		}
	}

	private int jjMoveStringLiteralDfa13_0(long old0, long active0) {
		if ((active0 = active0 & old0) == 0L) {
			return this.jjStartNfa_0(11, 0L, old0);
		} else {
			try {
				this.curChar = this.input_stream.readChar();
			} catch (IOException ignored) {
				this.jjStopStringLiteralDfa_0(12, 0L, active0);
				return 13;
			}

			switch (this.curChar) {
				case 'a':
					return this.jjMoveStringLiteralDfa14_0(active0, 8388608L);
				case 'b':
					return this.jjMoveStringLiteralDfa14_0(active0, 17184063488L);
				case 'c':
					return this.jjMoveStringLiteralDfa14_0(active0, 24910535438893056L);
				case 'e':
					if ((active0 & 32768L) != 0L) {
						return this.jjStopAtPos(13, 79);
					}

					return this.jjMoveStringLiteralDfa14_0(active0, 17675937972224L);
				case 'f':
					return this.jjMoveStringLiteralDfa14_0(active0, 8796093022208L);
				case 'g':
					if ((active0 & 2048L) != 0L) {
						return this.jjStartNfaWithStates_0(13, 75, 13);
					}

					return this.jjMoveStringLiteralDfa14_0(active0, 274878038016L);
				case 'i':
					return this.jjMoveStringLiteralDfa14_0(active0, 281646777237504L);
				case 'o':
					return this.jjMoveStringLiteralDfa14_0(active0, 2748779069440L);
				case 'p':
					return this.jjMoveStringLiteralDfa14_0(active0, 71469363101696L);
				case 'r':
					return this.jjMoveStringLiteralDfa14_0(active0, 10133100033998848L);
				case 's':
					if ((active0 & 4096L) != 0L) {
						return this.jjStopAtPos(13, 76);
					}

					return this.jjMoveStringLiteralDfa14_0(active0, 35184388866048L);
				case 't':
					return this.jjMoveStringLiteralDfa14_0(active0, 567348136247296L);
				case 'u':
					return this.jjMoveStringLiteralDfa14_0(active0, 108086391056891904L);
				case 'y':
					if ((active0 & 8192L) != 0L) {
						return this.jjStopAtPos(13, 77);
					} else if ((active0 & 16384L) != 0L) {
						return this.jjStopAtPos(13, 78);
					}
				case 'd':
				case 'h':
				case 'j':
				case 'k':
				case 'l':
				case 'm':
				case 'n':
				case 'q':
				case 'v':
				case 'w':
				case 'x':
				default:
					return this.jjStartNfa_0(12, 0L, active0);
			}
		}
	}

	private void jjCheckNAddStates(int start, int end) {
		do {
			this.jjCheckNAdd(jjnextStates[start]);
		} while (start++ != end);
	}

	private int jjMoveStringLiteralDfa1_0(long active0, long active1) {
		try {
			this.curChar = this.input_stream.readChar();
		} catch (IOException ignored) {
			this.jjStopStringLiteralDfa_0(0, active0, active1);
			return 1;
		}

		switch (this.curChar) {
			case '*':
				if ((active0 & 16777216L) != 0L) {
					this.jjmatchedKind = 24;
					this.jjmatchedPos = 1;
				}

				return this.jjMoveStringLiteralDfa2_0(active0, 67108864L, active1, 0L);
			case '+':
			case ',':
			case '-':
			case '/':
			case '0':
			case '1':
			case '2':
			case '3':
			case '4':
			case '5':
			case '6':
			case '7':
			case '8':
			case '9':
			case ':':
			case ';':
			case '<':
			case '=':
			case '>':
			case '?':
			case '@':
			case 'A':
			case 'B':
			case 'C':
			case 'D':
			case 'E':
			case 'F':
			case 'G':
			case 'H':
			case 'I':
			case 'J':
			case 'K':
			case 'L':
			case 'M':
			case 'N':
			case 'O':
			case 'P':
			case 'Q':
			case 'R':
			case 'S':
			case 'T':
			case 'U':
			case 'V':
			case 'W':
			case 'X':
			case 'Y':
			case 'Z':
			case '[':
			case '\\':
			case ']':
			case '^':
			case '_':
			case '`':
			case 'e':
			case 'g':
			case 'h':
			case 'j':
			case 'q':
			default:
				return this.jjStartNfa_0(0, active0, active1);
			case '.':
				return this.jjMoveStringLiteralDfa2_0(active0, 134217728L, active1, 0L);
			case 'a':
				return this.jjMoveStringLiteralDfa2_0(active0, 2251834173423616L, active1, 10226083123167520L);
			case 'b':
				return this.jjMoveStringLiteralDfa2_0(active0, 1125899906842624L, active1, 16384L);
			case 'c':
				return this.jjMoveStringLiteralDfa2_0(active0, 0L, active1, 562949953421312L);
			case 'd':
				return this.jjMoveStringLiteralDfa2_0(active0, 432345564764438528L, active1, 108229327568545282L);
			case 'f':
				return this.jjMoveStringLiteralDfa2_0(active0, 17592186044416L, active1, 274878955520L);
			case 'i':
				return this.jjMoveStringLiteralDfa2_0(active0, 576533393118855168L, active1, 131072L);
			case 'k':
				return this.jjMoveStringLiteralDfa2_0(active0, 2341871807306399744L, active1, 281630279794688L);
			case 'l':
				return this.jjMoveStringLiteralDfa2_0(active0, 2147483648L, active1, 2361352L);
			case 'm':
				return this.jjMoveStringLiteralDfa2_0(active0, 1153484454560268288L, active1, 4503599627370560L);
			case 'n':
				return this.jjMoveStringLiteralDfa2_0(active0, -9214364837331599360L, active1, 1099511627776L);
			case 'o':
				return this.jjMoveStringLiteralDfa2_0(active0, 4611831153962254336L, active1, 44023418880L);
			case 'p':
				return this.jjMoveStringLiteralDfa2_0(active0, 0L, active1, 18014398777917573L);
			case 'r':
				return this.jjMoveStringLiteralDfa2_0(active0, 4785486920941568L, active1, 8796109799424L);
			case 's':
				return this.jjMoveStringLiteralDfa2_0(active0, 0L, active1, 2251799813685248L);
			case 't':
				return this.jjMoveStringLiteralDfa2_0(active0, 9912784519168L, active1, 0L);
			case 'u':
				return this.jjMoveStringLiteralDfa2_0(active0, 8589934592L, active1, 35184372088832L);
			case 'v':
				return this.jjMoveStringLiteralDfa2_0(active0, 35184372088832L, active1, 0L);
			case 'w':
				return this.jjMoveStringLiteralDfa2_0(active0, 0L, active1, 33554432L);
			case 'x':
				return this.jjMoveStringLiteralDfa2_0(active0, 549755813888L, active1, 0L);
			case 'y':
				return this.jjMoveStringLiteralDfa2_0(active0, 18014398509481984L, active1, 16L);
			case 'z':
				return this.jjMoveStringLiteralDfa2_0(active0, 72057594037927936L, active1, 0L);
		}
	}

	private final int jjStartNfa_0(int pos, long active0, long active1) {
		return this.jjMoveNfa_0(this.jjStopStringLiteralDfa_0(pos, active0, active1), pos + 1);
	}

	private int jjMoveStringLiteralDfa18_0(long old0, long active0) {
		if ((active0 = active0 & old0) == 0L) {
			return this.jjStartNfa_0(16, 0L, old0);
		} else {
			try {
				this.curChar = this.input_stream.readChar();
			} catch (IOException ignored) {
				this.jjStopStringLiteralDfa_0(17, 0L, active0);
				return 18;
			}

			switch (this.curChar) {
				case 'a':
					if ((active0 & 134217728L) != 0L) {
						return this.jjStopAtPos(18, 91);
					}

					return this.jjMoveStringLiteralDfa19_0(active0, 2322168557862912L);
				case 'b':
					return this.jjMoveStringLiteralDfa19_0(active0, 35184372088832L);
				case 'c':
					return this.jjMoveStringLiteralDfa19_0(active0, 18141941858304L);
				case 'e':
					return this.jjMoveStringLiteralDfa19_0(active0, 9011605891186688L);
				case 'g':
					return this.jjMoveStringLiteralDfa19_0(active0, 4503599627370496L);
				case 'i':
					return this.jjMoveStringLiteralDfa19_0(active0, 1688849860263936L);
				case 'l':
					return this.jjMoveStringLiteralDfa19_0(active0, 108228228056875008L);
				case 'm':
					return this.jjMoveStringLiteralDfa19_0(active0, 283828618788864L);
				case 'n':
					if ((active0 & 268435456L) != 0L) {
						return this.jjStopAtPos(18, 92);
					}

					return this.jjMoveStringLiteralDfa19_0(active0, 18014501588697088L);
				case 'r':
					return this.jjMoveStringLiteralDfa19_0(active0, 274877906944L);
				case 's':
					if ((active0 & 536870912L) != 0L) {
						return this.jjStopAtPos(18, 93);
					} else if ((active0 & 1073741824L) != 0L) {
						return this.jjStopAtPos(18, 94);
					}
				case 'd':
				case 'f':
				case 'h':
				case 'j':
				case 'k':
				case 'o':
				case 'p':
				case 'q':
				default:
					return this.jjStartNfa_0(17, 0L, active0);
				case 't':
					return this.jjMoveStringLiteralDfa19_0(active0, 8802535473152L);
			}
		}
	}

	private int jjMoveStringLiteralDfa20_0(long old0, long active0) {
		if ((active0 = active0 & old0) == 0L) {
			return this.jjStartNfa_0(18, 0L, old0);
		} else {
			try {
				this.curChar = this.input_stream.readChar();
			} catch (IOException ignored) {
				this.jjStopStringLiteralDfa_0(19, 0L, active0);
				return 20;
			}

			switch (this.curChar) {
				case 'a':
					return this.jjMoveStringLiteralDfa21_0(active0, 70368744177664L);
				case 'b':
					return this.jjMoveStringLiteralDfa21_0(active0, 108086391056891904L);
				case 'c':
				case 'd':
				case 'f':
				case 'g':
				case 'h':
				case 'j':
				case 'k':
				case 'l':
				case 'o':
				case 'p':
				case 'q':
				case 'v':
				case 'w':
				case 'x':
				default:
					return this.jjStartNfa_0(19, 0L, active0);
				case 'e':
					return this.jjMoveStringLiteralDfa21_0(active0, 5911111949877248L);
				case 'i':
					return this.jjMoveStringLiteralDfa21_0(active0, 18014398509481984L);
				case 'm':
					return this.jjMoveStringLiteralDfa21_0(active0, 68719476736L);
				case 'n':
					return this.jjMoveStringLiteralDfa21_0(active0, 17592186044416L);
				case 'r':
					return this.jjMoveStringLiteralDfa21_0(active0, 44289702756352L);
				case 's':
					if ((active0 & 17179869184L) != 0L) {
						return this.jjStopAtPos(20, 98);
					}

					return this.jjMoveStringLiteralDfa21_0(active0, 141836999983104L);
				case 't':
					return this.jjMoveStringLiteralDfa21_0(active0, 565698732490752L);
				case 'u':
					return this.jjMoveStringLiteralDfa21_0(active0, 9011597301252096L);
				case 'y':
					return (active0 & 8589934592L) != 0L ? this.jjStopAtPos(20, 97) : this.jjMoveStringLiteralDfa21_0(active0, 2251799813685248L);
			}
		}
	}

	private int jjMoveStringLiteralDfa0_2() {
		switch (this.curChar) {
			case '"':
				return this.jjStartNfaWithStates_2(0, 122, 2);
			default:
				return this.jjMoveNfa_2(0, 0);
		}
	}

	private int jjMoveStringLiteralDfa27_0(long old0, long active0) {
		if ((active0 = active0 & old0) == 0L) {
			return this.jjStartNfa_0(25, 0L, old0);
		} else {
			try {
				this.curChar = this.input_stream.readChar();
			} catch (IOException ignored) {
				this.jjStopStringLiteralDfa_0(26, 0L, active0);
				return 27;
			}

			switch (this.curChar) {
				case 'a':
					return this.jjMoveStringLiteralDfa28_0(active0, 108086391056891904L);
				case 'e':
					return this.jjMoveStringLiteralDfa28_0(active0, 9007199254740992L);
				case 'r':
					return this.jjMoveStringLiteralDfa28_0(active0, 18014398509481984L);
				case 's':
					if ((active0 & 1125899906842624L) != 0L) {
						return this.jjStopAtPos(27, 114);
					}

					if ((active0 & 2251799813685248L) != 0L) {
						return this.jjStopAtPos(27, 115);
					}
					break;
				case 'y':
					if ((active0 & 4503599627370496L) != 0L) {
						return this.jjStopAtPos(27, 116);
					}
			}

			return this.jjStartNfa_0(26, 0L, active0);
		}
	}

	private int jjMoveNfa_2(int startState, int curPos) {
		int startsAt = 0;
		this.jjnewStateCnt = 3;
		int stateIndex = 1;
		this.jjstateSet[0] = startState;
		int kind = Integer.MAX_VALUE;

		while (true) {
			if (++this.jjround == Integer.MAX_VALUE) {
				this.ReInitRounds();
			}

			if (this.curChar < '@') {
				long charBitMask = 1L << this.curChar;

				do {
					stateIndex--;
					switch (this.jjstateSet[stateIndex]) {
						case 0:
							if ((-17179882497L & charBitMask) != 0L) {
								if (kind > 123) {
									kind = 123;
								}
							} else if (this.curChar == '"') {
								this.jjstateSet[this.jjnewStateCnt++] = 2;
							}
							break;
						case 1:
							if (this.curChar == '"') {
								this.jjstateSet[this.jjnewStateCnt++] = 2;
							}
							break;
						case 2:
							if (this.curChar == '"' && kind > 123) {
								kind = 123;
							}
					}
				} while (stateIndex != startsAt);
			} else if (this.curChar < 128) {
				long maskedCharBitMask = 1L << (this.curChar & '?');

				do {
					stateIndex--;
					switch (this.jjstateSet[stateIndex]) {
						case 0:
							kind = 123;
					}
				} while (stateIndex != startsAt);
			} else {
				int highByte = this.curChar >> '\b';
				int highByteIndex = highByte >> 6;
				long highByteMask = 1L << (highByte & 63);
				int lowByteIndex = (this.curChar & 255) >> 6;
				long lowByteMask = 1L << (this.curChar & '?');

				do {
					stateIndex--;
					switch (this.jjstateSet[stateIndex]) {
						case 0:
							if (jjCanMove_2(highByte, highByteIndex, lowByteIndex, highByteMask, lowByteMask) && kind > 123) {
								kind = 123;
							}
					}
				} while (stateIndex != startsAt);
			}

			if (kind != Integer.MAX_VALUE) {
				this.jjmatchedKind = kind;
				this.jjmatchedPos = curPos;
				kind = Integer.MAX_VALUE;
			}

			curPos++;
			if ((stateIndex = this.jjnewStateCnt) == (startsAt = 3 - (this.jjnewStateCnt = startsAt))) {
				return curPos;
			}

			try {
				this.curChar = this.input_stream.readChar();
			} catch (IOException ignored) {
				return curPos;
			}
		}
	}

	private int jjStartNfaWithStates_2(int pos, int kind, int state) {
		this.jjmatchedKind = kind;
		this.jjmatchedPos = pos;

		try {
			this.curChar = this.input_stream.readChar();
		} catch (IOException ignored) {
			return pos + 1;
		}

		return this.jjMoveNfa_2(state, pos + 1);
	}

	private int jjMoveStringLiteralDfa9_0(long old0, long active0, long old1, long active1) {
		if (((active0 = active0 & old0) | (active1 = active1 & old1)) == 0L) {
			return this.jjStartNfa_0(7, old0, old1);
		} else {
			try {
				this.curChar = this.input_stream.readChar();
			} catch (IOException ignored) {
				this.jjStopStringLiteralDfa_0(8, active0, active1);
				return 9;
			}

			switch (this.curChar) {
				case 'a':
					return this.jjMoveStringLiteralDfa10_0(active0, 0L, active1, 283476230152L);
				case 'b':
					return this.jjMoveStringLiteralDfa10_0(active0, 0L, active1, 18014398509481984L);
				case 'c':
					return this.jjMoveStringLiteralDfa10_0(active0, 0L, active1, 1589248L);
				case 'd':
					return this.jjMoveStringLiteralDfa10_0(active0, Long.MIN_VALUE, active1, 1L);
				case 'e':
					if ((active0 & 576460752303423488L) != 0L) {
						return this.jjStopAtPos(9, 59);
					}

					return this.jjMoveStringLiteralDfa10_0(active0, 0L, active1, 14745000701075456L);
				case 'f':
					return this.jjMoveStringLiteralDfa10_0(active0, 4611686018427387904L, active1, 268435456L);
				case 'g':
					return this.jjMoveStringLiteralDfa10_0(active0, 0L, active1, 4398046511108L);
				case 'h':
				case 'j':
				case 'k':
				case 'q':
				case 'v':
				case 'w':
				case 'x':
				case 'y':
				default:
					return this.jjStartNfa_0(8, active0, active1);
				case 'i':
					return this.jjMoveStringLiteralDfa10_0(active0, 0L, active1, 140771982376960L);
				case 'l':
					return this.jjMoveStringLiteralDfa10_0(active0, 0L, active1, 2147483904L);
				case 'm':
					return this.jjMoveStringLiteralDfa10_0(active0, 0L, active1, 538968576L);
				case 'n':
					return this.jjMoveStringLiteralDfa10_0(active0, 0L, active1, 108086391056893954L);
				case 'o':
					return this.jjMoveStringLiteralDfa10_0(active0, 0L, active1, 17660905521152L);
				case 'p':
					return this.jjMoveStringLiteralDfa10_0(active0, 0L, active1, 160L);
				case 'r':
					return this.jjMoveStringLiteralDfa10_0(active0, 0L, active1, 132096L);
				case 's':
					if ((active0 & 1152921504606846976L) != 0L) {
						return this.jjStartNfaWithStates_0(9, 60, 13);
					} else {
						if ((active0 & 2305843009213693952L) != 0L) {
							return this.jjStopAtPos(9, 61);
						}

						return this.jjMoveStringLiteralDfa10_0(active0, 0L, active1, 282733473726464L);
					}
				case 't':
					return this.jjMoveStringLiteralDfa10_0(active0, 0L, active1, 1073745984L);
				case 'u':
					return this.jjMoveStringLiteralDfa10_0(active0, 0L, active1, 2823545893683200L);
				case 'z':
					return this.jjMoveStringLiteralDfa10_0(active0, 0L, active1, 16L);
			}
		}
	}

	private int jjMoveStringLiteralDfa25_0(long old0, long active0) {
		if ((active0 = active0 & old0) == 0L) {
			return this.jjStartNfa_0(23, 0L, old0);
		} else {
			try {
				this.curChar = this.input_stream.readChar();
			} catch (IOException ignored) {
				this.jjStopStringLiteralDfa_0(24, 0L, active0);
				return 25;
			}

			switch (this.curChar) {
				case 'a':
					if ((active0 & 2199023255552L) != 0L) {
						return this.jjStopAtPos(25, 105);
					}
				case 'b':
				case 'd':
				case 'f':
				case 'h':
				case 'i':
				case 'j':
				case 'k':
				case 'm':
				case 'o':
				case 'p':
				case 'q':
				default:
					break;
				case 'c':
					return this.jjMoveStringLiteralDfa26_0(active0, 109212290963734528L);
				case 'e':
					if ((active0 & 8796093022208L) != 0L) {
						return this.jjStopAtPos(25, 107);
					}

					return this.jjMoveStringLiteralDfa26_0(active0, 4925812092436480L);
				case 'g':
					if ((active0 & 4398046511104L) != 0L) {
						return this.jjStopAtPos(25, 106);
					}
					break;
				case 'l':
					return this.jjMoveStringLiteralDfa26_0(active0, 9007199254740992L);
				case 'n':
					return this.jjMoveStringLiteralDfa26_0(active0, 18014398509481984L);
				case 'r':
					return this.jjMoveStringLiteralDfa26_0(active0, 633318697598976L);
				case 's':
					if ((active0 & 17592186044416L) != 0L) {
						return this.jjStopAtPos(25, 108);
					}

					if ((active0 & 35184372088832L) != 0L) {
						return this.jjStopAtPos(25, 109);
					}

					return this.jjMoveStringLiteralDfa26_0(active0, 2251799813685248L);
			}

			return this.jjStartNfa_0(24, 0L, active0);
		}
	}

	private int jjMoveStringLiteralDfa22_0(long old0, long active0) {
		if ((active0 = active0 & old0) == 0L) {
			return this.jjStartNfa_0(20, 0L, old0);
		} else {
			try {
				this.curChar = this.input_stream.readChar();
			} catch (IOException ignored) {
				this.jjStopStringLiteralDfa_0(21, 0L, active0);
				return 22;
			}

			switch (this.curChar) {
				case 'a':
					return this.jjMoveStringLiteralDfa23_0(active0, 108121575428980736L);
				case 'b':
					return this.jjMoveStringLiteralDfa23_0(active0, 8796093022208L);
				case 'c':
				case 'i':
				case 'j':
				case 'k':
				case 'm':
				case 'p':
				case 'q':
				case 'r':
				default:
					return this.jjStartNfa_0(21, 0L, active0);
				case 'd':
					return this.jjMoveStringLiteralDfa23_0(active0, 2199023255552L);
				case 'e':
					return this.jjMoveStringLiteralDfa23_0(active0, 89060441849856L);
				case 'f':
					return this.jjMoveStringLiteralDfa23_0(active0, 1125899906842624L);
				case 'g':
					return this.jjMoveStringLiteralDfa23_0(active0, 4398046511104L);
				case 'h':
					return this.jjMoveStringLiteralDfa23_0(active0, 274877906944L);
				case 'l':
					return this.jjMoveStringLiteralDfa23_0(active0, 2251799813685248L);
				case 'n':
					return this.jjMoveStringLiteralDfa23_0(active0, 9429411719806976L);
				case 'o':
					return this.jjMoveStringLiteralDfa23_0(active0, 563499709235200L);
				case 's':
					if ((active0 & 68719476736L) != 0L) {
						return this.jjStopAtPos(22, 100);
					} else {
						if ((active0 & 137438953472L) != 0L) {
							return this.jjStopAtPos(22, 101);
						}

						return this.jjMoveStringLiteralDfa23_0(active0, 4503599627370496L);
					}
				case 't':
					return this.jjMoveStringLiteralDfa23_0(active0, 18014398509481984L);
			}
		}
	}

	private int jjStopAtPos(int pos, int kind) {
		this.jjmatchedKind = kind;
		this.jjmatchedPos = pos;
		return pos + 1;
	}

	private int jjMoveStringLiteralDfa19_0(long old0, long active0) {
		if ((active0 = active0 & old0) == 0L) {
			return this.jjStartNfa_0(17, 0L, old0);
		} else {
			try {
				this.curChar = this.input_stream.readChar();
			} catch (IOException ignored) {
				this.jjStopStringLiteralDfa_0(18, 0L, active0);
				return 19;
			}

			switch (this.curChar) {
				case 'a':
					if ((active0 & 2147483648L) != 0L) {
						return this.jjStopAtPos(19, 95);
					}

					return this.jjMoveStringLiteralDfa20_0(active0, 142764712919040L);
				case 'b':
					return this.jjMoveStringLiteralDfa20_0(active0, 286010462175232L);
				case 'c':
					return this.jjMoveStringLiteralDfa20_0(active0, 562949953421312L);
				case 'd':
					return this.jjMoveStringLiteralDfa20_0(active0, 19140298416324608L);
				case 'e':
					return this.jjMoveStringLiteralDfa20_0(active0, 37400575213568L);
				case 'i':
					return this.jjMoveStringLiteralDfa20_0(active0, 108086391056891904L);
				case 'l':
					return this.jjMoveStringLiteralDfa20_0(active0, 8589934592L);
				case 'o':
					return this.jjMoveStringLiteralDfa20_0(active0, 17592186044416L);
				case 'r':
					return this.jjMoveStringLiteralDfa20_0(active0, 6825768185233408L);
				case 's':
					if ((active0 & 4294967296L) != 0L) {
						return this.jjStopAtPos(19, 96);
					}
				case 'f':
				case 'g':
				case 'h':
				case 'j':
				case 'k':
				case 'm':
				case 'n':
				case 'p':
				case 'q':
				default:
					return this.jjStartNfa_0(18, 0L, active0);
				case 't':
					return this.jjMoveStringLiteralDfa20_0(active0, 9015995347763200L);
			}
		}
	}

	private int jjMoveStringLiteralDfa0_1() {
		return this.jjMoveNfa_1(0, 0);
	}

	private int jjMoveStringLiteralDfa10_0(long old0, long active0, long old1, long active1) {
		if (((active0 = active0 & old0) | (active1 = active1 & old1)) == 0L) {
			return this.jjStartNfa_0(8, old0, old1);
		} else {
			try {
				this.curChar = this.input_stream.readChar();
			} catch (IOException ignored) {
				this.jjStopStringLiteralDfa_0(9, active0, active1);
				return 10;
			}

			switch (this.curChar) {
				case 'b':
					return this.jjMoveStringLiteralDfa11_0(active0, 0L, active1, 2251799813750784L);
				case 'c':
					return this.jjMoveStringLiteralDfa11_0(active0, 0L, active1, 36558778695680L);
				case 'd':
				case 'h':
				case 'j':
				case 'l':
				case 'p':
				case 'q':
				case 'v':
				case 'w':
				default:
					return this.jjStartNfa_0(9, active0, active1);
				case 'e':
					if ((active0 & Long.MIN_VALUE) != 0L) {
						return this.jjStartNfaWithStates_0(10, 63, 13);
					} else {
						if ((active1 & 4L) != 0L) {
							return this.jjStopAtPos(10, 66);
						}

						return this.jjMoveStringLiteralDfa11_0(active0, 0L, active1, 281612953584656L);
					}
				case 'f':
					return this.jjMoveStringLiteralDfa11_0(active0, 0L, active1, 18014398509481984L);
				case 'g':
					return this.jjMoveStringLiteralDfa11_0(active0, 0L, active1, 8598323200L);
				case 'i':
					return this.jjMoveStringLiteralDfa11_0(active0, 0L, active1, 7786730208L);
				case 'k':
					if ((active1 & 2L) != 0L) {
						return this.jjStopAtPos(10, 65);
					}

					return this.jjMoveStringLiteralDfa11_0(active0, 0L, active1, 33556480L);
				case 'm':
					return this.jjMoveStringLiteralDfa11_0(active0, 0L, active1, 17184063488L);
				case 'n':
					return this.jjMoveStringLiteralDfa11_0(active0, 0L, active1, 134348800L);
				case 'o':
					if ((active0 & 4611686018427387904L) != 0L) {
						return this.jjStopAtPos(10, 62);
					}

					return this.jjMoveStringLiteralDfa11_0(active0, 0L, active1, 108086425416630272L);
				case 'r':
					return this.jjMoveStringLiteralDfa11_0(active0, 0L, active1, 4512395720400904L);
				case 's':
					if ((active1 & 1L) != 0L) {
						return this.jjStopAtPos(10, 64);
					}

					return this.jjMoveStringLiteralDfa11_0(active0, 0L, active1, 636067543777280L);
				case 't':
					return this.jjMoveStringLiteralDfa11_0(active0, 0L, active1, 540672L);
				case 'u':
					return this.jjMoveStringLiteralDfa11_0(active0, 0L, active1, 22058952032512L);
				case 'x':
					return this.jjMoveStringLiteralDfa11_0(active0, 0L, active1, 10273836649938944L);
			}
		}
	}

	private void jjCheckNAddTwoStates(int state1, int state2) {
		this.jjCheckNAdd(state1);
		this.jjCheckNAdd(state2);
	}

	protected ProguardToken jjFillToken() {
		String literalImage = jjstrLiteralImages[this.jjmatchedKind];
		String curTokenImage = literalImage == null ? this.input_stream.GetImage() : literalImage;
		int beginLine = this.input_stream.getBeginLine();
		int beginColumn = this.input_stream.getBeginColumn();
		int endLine = this.input_stream.getEndLine();
		int endColumn = this.input_stream.getEndColumn();
		ProguardToken token = ProguardToken.newToken(this.jjmatchedKind);
		token.kind = this.jjmatchedKind;
		token.image = curTokenImage;
		token.beginLine = beginLine;
		token.endLine = endLine;
		token.beginColumn = beginColumn;
		token.endColumn = endColumn;
		return token;
	}

	private int jjMoveNfa_0(int startState, int curPos) {
		int startsAt = 0;
		this.jjnewStateCnt = 12;
		int stateIndex = 1;
		this.jjstateSet[0] = startState;
		int kind = Integer.MAX_VALUE;

		while (true) {
			if (++this.jjround == Integer.MAX_VALUE) {
				this.ReInitRounds();
			}

			if (this.curChar < '@') {
				long charBitMask = 1L << this.curChar;

				do {
					stateIndex--;
					switch (this.jjstateSet[stateIndex]) {
						case 0:
							if ((287948901175001088L & charBitMask) != 0L) {
								if (kind > 124) {
									kind = 124;
								}

								this.jjCheckNAdd(0);
							}
							break;
						case 1:
							if ((-8935348299901189633L & charBitMask) != 0L) {
								if (kind > 126) {
									kind = 126;
								}

								this.jjCheckNAddStates(0, 3);
							} else if (this.curChar == '%') {
								if (kind > 127) {
									kind = 127;
								}

								this.jjCheckNAdd(8);
							} else if (this.curChar == '<') {
								this.jjstateSet[this.jjnewStateCnt++] = 4;
							}

							if ((287948901175001088L & charBitMask) != 0L) {
								if (kind > 124) {
									kind = 124;
								}

								this.jjCheckNAdd(0);
							} else if (this.curChar == '$') {
								if (kind > 125) {
									kind = 125;
								}

								this.jjCheckNAdd(2);
							}
							break;
						case 2:
							if ((287948970162897407L & charBitMask) != 0L) {
								if (kind > 125) {
									kind = 125;
								}

								this.jjCheckNAdd(2);
							}
							break;
						case 3:
							if (this.curChar == '<') {
								this.jjstateSet[this.jjnewStateCnt++] = 4;
							}
							break;
						case 4:
							if ((287667426198290432L & charBitMask) != 0L) {
								this.jjstateSet[this.jjnewStateCnt++] = 5;
							}
							break;
						case 5:
							if (this.curChar == '>') {
								kind = 126;
								this.jjCheckNAddTwoStates(6, 3);
							}
							break;
						case 6:
							if ((-8935348299901189633L & charBitMask) != 0L) {
								if (kind > 126) {
									kind = 126;
								}

								this.jjCheckNAddTwoStates(6, 3);
							}
							break;
						case 7:
							if (this.curChar == '%') {
								kind = 127;
								this.jjCheckNAdd(8);
							}
						case 8:
						case 9:
						default:
							break;
						case 10:
							if ((-8935348299901189633L & charBitMask) != 0L) {
								if (kind > 126) {
									kind = 126;
								}

								this.jjCheckNAddStates(0, 3);
							}
							break;
						case 11:
							if ((-8935348299901189633L & charBitMask) != 0L) {
								if (kind > 127) {
									kind = 127;
								}

								this.jjCheckNAddTwoStates(11, 8);
							}
							break;
						case 12:
							if ((-8935348299901189633L & charBitMask) != 0L) {
								if (kind > 127) {
									kind = 127;
								}

								this.jjCheckNAddTwoStates(11, 8);
							} else if (this.curChar == '<') {
								this.jjstateSet[this.jjnewStateCnt++] = 4;
							}

							if ((-8935348299901189633L & charBitMask) != 0L) {
								if (kind > 126) {
									kind = 126;
								}

								this.jjCheckNAddTwoStates(6, 3);
							}
							break;
						case 13:
							if ((-8935348299901189633L & charBitMask) != 0L) {
								if (kind > 127) {
									kind = 127;
								}

								this.jjCheckNAddTwoStates(11, 8);
							} else if (this.curChar == '<') {
								this.jjstateSet[this.jjnewStateCnt++] = 4;
							}

							if ((-8935348299901189633L & charBitMask) != 0L) {
								if (kind > 126) {
									kind = 126;
								}

								this.jjCheckNAddTwoStates(6, 3);
							}

							if ((287948970162897407L & charBitMask) != 0L) {
								if (kind > 125) {
									kind = 125;
								}

								this.jjCheckNAdd(2);
							}
					}
				} while (stateIndex != startsAt);
			} else if (this.curChar < 128) {
				long maskedCharBitMask = 1L << (this.curChar & '?');

				do {
					stateIndex--;
					switch (this.jjstateSet[stateIndex]) {
						case 1:
							if ((-8646911290859585538L & maskedCharBitMask) != 0L) {
								if (kind > 126) {
									kind = 126;
								}

								this.jjCheckNAddStates(0, 3);
							}

							if ((576460745995190270L & maskedCharBitMask) != 0L) {
								if (kind > 125) {
									kind = 125;
								}

								this.jjCheckNAdd(2);
							}
							break;
						case 2:
							if ((-8646911290859585538L & maskedCharBitMask) != 0L) {
								if (kind > 125) {
									kind = 125;
								}

								this.jjCheckNAdd(2);
							}
						case 3:
						case 4:
						case 5:
						case 7:
						default:
							break;
						case 6:
							if ((-8646911290859585538L & maskedCharBitMask) != 0L) {
								if (kind > 126) {
									kind = 126;
								}

								this.jjCheckNAddTwoStates(6, 3);
							}
							break;
						case 8:
							if (this.curChar == '[') {
								this.jjstateSet[this.jjnewStateCnt++] = 9;
							}
							break;
						case 9:
							if (this.curChar == ']') {
								kind = 127;
								this.jjCheckNAdd(8);
							}
							break;
						case 10:
							if ((-8646911290859585538L & maskedCharBitMask) != 0L) {
								if (kind > 126) {
									kind = 126;
								}

								this.jjCheckNAddStates(0, 3);
							}
							break;
						case 11:
							if ((-8646911290859585538L & maskedCharBitMask) != 0L) {
								if (kind > 127) {
									kind = 127;
								}

								this.jjCheckNAddTwoStates(11, 8);
							}
							break;
						case 12:
							if ((-8646911290859585538L & maskedCharBitMask) != 0L) {
								if (kind > 127) {
									kind = 127;
								}

								this.jjCheckNAddTwoStates(11, 8);
							} else if (this.curChar == '[') {
								this.jjstateSet[this.jjnewStateCnt++] = 9;
							}

							if ((-8646911290859585538L & maskedCharBitMask) != 0L) {
								if (kind > 126) {
									kind = 126;
								}

								this.jjCheckNAddTwoStates(6, 3);
							}
							break;
						case 13:
							if ((-8646911290859585538L & maskedCharBitMask) != 0L) {
								if (kind > 127) {
									kind = 127;
								}

								this.jjCheckNAddTwoStates(11, 8);
							} else if (this.curChar == '[') {
								this.jjstateSet[this.jjnewStateCnt++] = 9;
							}

							if ((-8646911290859585538L & maskedCharBitMask) != 0L) {
								if (kind > 126) {
									kind = 126;
								}

								this.jjCheckNAddTwoStates(6, 3);
							}

							if ((-8646911290859585538L & maskedCharBitMask) != 0L) {
								if (kind > 125) {
									kind = 125;
								}

								this.jjCheckNAdd(2);
							}
					}
				} while (stateIndex != startsAt);
			} else {
				int highByte = this.curChar >> '\b';
				int highByteIndex = highByte >> 6;
				long highByteMask = 1L << (highByte & 63);
				int lowByteIndex = (this.curChar & 255) >> 6;
				long lowByteMask = 1L << (this.curChar & '?');

				do {
					stateIndex--;
					switch (this.jjstateSet[stateIndex]) {
						case 1:
							if (jjCanMove_0(highByte, highByteIndex, lowByteIndex, highByteMask, lowByteMask)) {
								if (kind > 125) {
									kind = 125;
								}

								this.jjCheckNAdd(2);
							}

							if (jjCanMove_1(highByte, highByteIndex, lowByteIndex, highByteMask, lowByteMask)) {
								if (kind > 126) {
									kind = 126;
								}

								this.jjCheckNAddStates(0, 3);
							}
							break;
						case 2:
							if (jjCanMove_1(highByte, highByteIndex, lowByteIndex, highByteMask, lowByteMask)) {
								if (kind > 125) {
									kind = 125;
								}

								this.jjCheckNAdd(2);
							}
						case 3:
						case 4:
						case 5:
						case 7:
						case 8:
						case 9:
						default:
							break;
						case 6:
							if (jjCanMove_1(highByte, highByteIndex, lowByteIndex, highByteMask, lowByteMask)) {
								if (kind > 126) {
									kind = 126;
								}

								this.jjCheckNAddTwoStates(6, 3);
							}
							break;
						case 10:
							if (jjCanMove_1(highByte, highByteIndex, lowByteIndex, highByteMask, lowByteMask)) {
								if (kind > 126) {
									kind = 126;
								}

								this.jjCheckNAddStates(0, 3);
							}
							break;
						case 11:
							if (jjCanMove_1(highByte, highByteIndex, lowByteIndex, highByteMask, lowByteMask)) {
								if (kind > 127) {
									kind = 127;
								}

								this.jjCheckNAddTwoStates(11, 8);
							}
							break;
						case 12:
							if (jjCanMove_1(highByte, highByteIndex, lowByteIndex, highByteMask, lowByteMask)) {
								if (kind > 126) {
									kind = 126;
								}

								this.jjCheckNAddTwoStates(6, 3);
							}

							if (jjCanMove_1(highByte, highByteIndex, lowByteIndex, highByteMask, lowByteMask)) {
								if (kind > 127) {
									kind = 127;
								}

								this.jjCheckNAddTwoStates(11, 8);
							}
							break;
						case 13:
							if (jjCanMove_1(highByte, highByteIndex, lowByteIndex, highByteMask, lowByteMask)) {
								if (kind > 125) {
									kind = 125;
								}

								this.jjCheckNAdd(2);
							}

							if (jjCanMove_1(highByte, highByteIndex, lowByteIndex, highByteMask, lowByteMask)) {
								if (kind > 126) {
									kind = 126;
								}

								this.jjCheckNAddTwoStates(6, 3);
							}

							if (jjCanMove_1(highByte, highByteIndex, lowByteIndex, highByteMask, lowByteMask)) {
								if (kind > 127) {
									kind = 127;
								}

								this.jjCheckNAddTwoStates(11, 8);
							}
					}
				} while (stateIndex != startsAt);
			}

			if (kind != Integer.MAX_VALUE) {
				this.jjmatchedKind = kind;
				this.jjmatchedPos = curPos;
				kind = Integer.MAX_VALUE;
			}

			curPos++;
			if ((stateIndex = this.jjnewStateCnt) == (startsAt = 12 - (this.jjnewStateCnt = startsAt))) {
				return curPos;
			}

			try {
				this.curChar = this.input_stream.readChar();
			} catch (IOException ignored) {
				return curPos;
			}
		}
	}

	private int jjMoveStringLiteralDfa14_0(long old0, long active0) {
		if ((active0 = active0 & old0) == 0L) {
			return this.jjStartNfa_0(12, 0L, old0);
		} else {
			try {
				this.curChar = this.input_stream.readChar();
			} catch (IOException ignored) {
				this.jjStopStringLiteralDfa_0(13, 0L, active0);
				return 14;
			}

			switch (this.curChar) {
				case 'a':
					return this.jjMoveStringLiteralDfa15_0(active0, 18155137474232320L);
				case 'b':
					return this.jjMoveStringLiteralDfa15_0(active0, 108086391056891904L);
				case 'c':
					return this.jjMoveStringLiteralDfa15_0(active0, 34359738368L);
				case 'd':
					return this.jjMoveStringLiteralDfa15_0(active0, 549755813888L);
				case 'e':
					return this.jjMoveStringLiteralDfa15_0(active0, 4503891689865216L);
				case 'f':
					return this.jjMoveStringLiteralDfa15_0(active0, 17665200488448L);
				case 'g':
				case 'h':
				case 'j':
				case 'k':
				case 'p':
				case 'q':
				case 'r':
				default:
					return this.jjStartNfa_0(13, 0L, active0);
				case 'i':
					return this.jjMoveStringLiteralDfa15_0(active0, 646512939892736L);
				case 'l':
					return this.jjMoveStringLiteralDfa15_0(active0, 2251799813685248L);
				case 'm':
					return this.jjMoveStringLiteralDfa15_0(active0, 8388608L);
				case 'n':
					return this.jjMoveStringLiteralDfa15_0(active0, 10133099699503104L);
				case 'o':
					return this.jjMoveStringLiteralDfa15_0(active0, 262144L);
				case 's':
					if ((active0 & 65536L) != 0L) {
						return this.jjStopAtPos(14, 80);
					} else {
						if ((active0 & 131072L) != 0L) {
							return this.jjStopAtPos(14, 81);
						}

						return this.jjMoveStringLiteralDfa15_0(active0, 35192978800640L);
					}
				case 't':
					return this.jjMoveStringLiteralDfa15_0(active0, 284913098031104L);
			}
		}
	}

	private int jjMoveStringLiteralDfa36_0(long old0, long active0) {
		if ((active0 = active0 & old0) == 0L) {
			return this.jjStartNfa_0(34, 0L, old0);
		} else {
			try {
				this.curChar = this.input_stream.readChar();
			} catch (IOException ignored) {
				this.jjStopStringLiteralDfa_0(35, 0L, active0);
				return 36;
			}

			switch (this.curChar) {
				case 's':
					if ((active0 & 72057594037927936L) != 0L) {
						return this.jjStopAtPos(36, 120);
					}
				default:
					return this.jjStartNfa_0(35, 0L, active0);
			}
		}
	}

	private int jjMoveStringLiteralDfa0_0() {
		switch (this.curChar) {
			case '!':
				return this.jjStopAtPos(0, 16);
			case '"':
				return this.jjStopAtPos(0, 121);
			case '#':
				return this.jjStopAtPos(0, 6);
			case '%':
				return this.jjStartNfaWithStates_0(0, 18, 8);
			case '(':
				return this.jjStopAtPos(0, 20);
			case ')':
				return this.jjStopAtPos(0, 21);
			case '*':
				this.jjmatchedKind = 19;
				return this.jjMoveStringLiteralDfa1_0(83886080L, 0L);
			case ',':
				return this.jjStopAtPos(0, 12);
			case '-':
				return this.jjMoveStringLiteralDfa1_0(2848776355095511040L, 144114088561866735L);
			case '.':
				this.jjmatchedKind = 15;
				return this.jjMoveStringLiteralDfa1_0(134217728L, 0L);
			case ';':
				return this.jjStopAtPos(0, 13);
			case '<':
				return this.jjMoveStringLiteralDfa1_0(580610858942464L, 0L);
			case '?':
				return this.jjStartNfaWithStates_0(0, 17, 12);
			case '@':
				this.jjmatchedKind = 14;
				return this.jjMoveStringLiteralDfa1_0(576460752303423488L, 0L);
			case 'a':
				return this.jjMoveStringLiteralDfa1_0(1125899906842624L, 2361344L);
			case 'b':
				return this.jjMoveStringLiteralDfa1_0(137438953472L, 0L);
			case 'c':
				return this.jjMoveStringLiteralDfa1_0(2147483648L, 0L);
			case 'e':
				return this.jjMoveStringLiteralDfa1_0(550024249344L, 0L);
			case 'f':
				return this.jjMoveStringLiteralDfa1_0(4294967296L, 0L);
			case 'i':
				return this.jjMoveStringLiteralDfa1_0(-8061443332993187840L, 1099511627776L);
			case 'm':
				return this.jjMoveStringLiteralDfa1_0(4611686018427387904L, 0L);
			case 'n':
				return this.jjMoveStringLiteralDfa1_0(34359738368L, 0L);
			case 'p':
				return this.jjMoveStringLiteralDfa1_0(281758444552192L, 0L);
			case 's':
				return this.jjMoveStringLiteralDfa1_0(18023211782373376L, 16L);
			case 't':
				return this.jjMoveStringLiteralDfa1_0(4503599627370496L, 0L);
			case 'v':
				return this.jjMoveStringLiteralDfa1_0(4398046511104L, 0L);
			case '{':
				return this.jjStopAtPos(0, 22);
			case '}':
				return this.jjStopAtPos(0, 23);
			case '~':
				return this.jjStopAtPos(0, 11);
			case '\ufeff':
				return this.jjStartNfaWithStates_0(0, 10, 12);
			default:
				return this.jjMoveNfa_0(1, 0);
		}
	}

	private int jjMoveStringLiteralDfa31_0(long old0, long active0) {
		if ((active0 = active0 & old0) == 0L) {
			return this.jjStartNfa_0(29, 0L, old0);
		} else {
			try {
				this.curChar = this.input_stream.readChar();
			} catch (IOException ignored) {
				this.jjStopStringLiteralDfa_0(30, 0L, active0);
				return 31;
			}

			switch (this.curChar) {
				case 'e':
					return this.jjMoveStringLiteralDfa32_0(active0, 72057594037927936L);
				case 's':
					if ((active0 & 36028797018963968L) != 0L) {
						return this.jjStopAtPos(31, 119);
					}
				default:
					return this.jjStartNfa_0(30, 0L, active0);
			}
		}
	}

	private int jjMoveStringLiteralDfa35_0(long old0, long active0) {
		if ((active0 = active0 & old0) == 0L) {
			return this.jjStartNfa_0(33, 0L, old0);
		} else {
			try {
				this.curChar = this.input_stream.readChar();
			} catch (IOException ignored) {
				this.jjStopStringLiteralDfa_0(34, 0L, active0);
				return 35;
			}

			switch (this.curChar) {
				case 'r':
					return this.jjMoveStringLiteralDfa36_0(active0, 72057594037927936L);
				default:
					return this.jjStartNfa_0(34, 0L, active0);
			}
		}
	}

	private int jjMoveStringLiteralDfa5_0(long old0, long active0, long old1, long active1) {
		if (((active0 = active0 & old0) | (active1 = active1 & old1)) == 0L) {
			return this.jjStartNfa_0(3, old0, old1);
		} else {
			try {
				this.curChar = this.input_stream.readChar();
			} catch (IOException ignored) {
				this.jjStopStringLiteralDfa_0(4, active0, active1);
				return 5;
			}

			switch (this.curChar) {
				case '>':
					if ((active0 & 68719476736L) != 0L) {
						return this.jjStopAtPos(5, 36);
					}
				case '?':
				case '@':
				case 'A':
				case 'B':
				case 'C':
				case 'D':
				case 'E':
				case 'F':
				case 'G':
				case 'H':
				case 'I':
				case 'J':
				case 'K':
				case 'L':
				case 'M':
				case 'N':
				case 'O':
				case 'P':
				case 'Q':
				case 'R':
				case 'S':
				case 'T':
				case 'U':
				case 'V':
				case 'W':
				case 'X':
				case 'Y':
				case 'Z':
				case '[':
				case '\\':
				case ']':
				case '^':
				case '_':
				case '`':
				case 'b':
				case 'g':
				case 'h':
				case 'j':
				case 'q':
				case 'v':
				case 'x':
				default:
					return this.jjStartNfa_0(4, active0, active1);
				case 'a':
					return this.jjMoveStringLiteralDfa6_0(active0, 1266637395197952L, active1, 18014398509547528L);
				case 'c':
					if ((active0 & 8589934592L) != 0L) {
						return this.jjStartNfaWithStates_0(5, 33, 13);
					} else {
						if ((active0 & 17179869184L) != 0L) {
							return this.jjStartNfaWithStates_0(5, 34, 13);
						}

						return this.jjMoveStringLiteralDfa6_0(active0, 36310271995674624L, active1, 281629616505856L);
					}
				case 'd':
					return this.jjMoveStringLiteralDfa6_0(active0, -9223353894912917504L, active1, 1099512168448L);
				case 'e':
					if ((active0 & 34359738368L) != 0L) {
						return this.jjStartNfaWithStates_0(5, 35, 13);
					} else {
						if ((active0 & 137438953472L) != 0L) {
							return this.jjStartNfaWithStates_0(5, 37, 13);
						}

						return this.jjMoveStringLiteralDfa6_0(active0, 4629701516448497664L, active1, 4503599628419072L);
					}
				case 'f':
					return this.jjMoveStringLiteralDfa6_0(active0, 9007199254740992L, active1, 0L);
				case 'i':
					return this.jjMoveStringLiteralDfa6_0(active0, 4507997673881600L, active1, 0L);
				case 'k':
					return this.jjMoveStringLiteralDfa6_0(active0, 0L, active1, 134217728L);
				case 'l':
					return this.jjMoveStringLiteralDfa6_0(active0, 72057594037927936L, active1, 8589934592L);
				case 'm':
					return this.jjMoveStringLiteralDfa6_0(active0, 1152921504606846976L, active1, 10212269367496960L);
				case 'n':
					return this.jjMoveStringLiteralDfa6_0(active0, 2449958197289549824L, active1, 2286984185774080L);
				case 'o':
					return this.jjMoveStringLiteralDfa6_0(active0, 2849934139195392L, active1, 4398048903744L);
				case 'p':
					return this.jjMoveStringLiteralDfa6_0(active0, 0L, active1, 2199568523264L);
				case 'r':
					return this.jjMoveStringLiteralDfa6_0(active0, 576462951326679040L, active1, 33685520L);
				case 's':
					return this.jjMoveStringLiteralDfa6_0(active0, 0L, active1, 108649375370053634L);
				case 't':
					return this.jjMoveStringLiteralDfa6_0(active0, 9070970929152L, active1, 17938266456197L);
				case 'u':
					return this.jjMoveStringLiteralDfa6_0(active0, 70368744177664L, active1, 140737488355328L);
				case 'w':
					return this.jjMoveStringLiteralDfa6_0(active0, 288230376151711744L, active1, 549755813888L);
				case 'y':
					return this.jjMoveStringLiteralDfa6_0(active0, 0L, active1, 32L);
			}
		}
	}
}
