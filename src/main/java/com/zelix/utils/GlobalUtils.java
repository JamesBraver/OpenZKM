package com.zelix.utils;

import com.zelix.framework.reference.ReferenceGraph;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.klassmaster.config.Options;
import com.zelix.utils.collections.*;
import com.zelix.utils.collections.readOnly.ReadOnlyRegistryView;
import com.zelix.utils.references.AtomicReference;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;

public class GlobalUtils {
	public static final String LINE_SEPARATOR = System.getProperty("line.separator");
	public static final char[] BASE16 = "0123456789ABCDEF".toCharArray();
	public static final char[] BASE36 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
	public static final char[] BASE52 = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
	public static final char[] BASE62 = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
	public static final char[] BASE62_SHUFFLED = "LGt1jHaSDNIK0OXJ8sCfkg7cpzdeul5nAWh6wUxEV3T4rvFMYPqoi9ymZ2RQbB".toCharArray();
	private static final int[] PRIME_NUMBER_LOOKUP_TBL = new int[]{
		5,
		7,
		11,
		13,
		17,
		19,
		23,
		29,
		31,
		37,
		41,
		43,
		47,
		53,
		59,
		61,
		67,
		71,
		73,
		79,
		83,
		89,
		97,
		101,
		107,
		113,
		127,
		131,
		137,
		149,
		157,
		163,
		167,
		173,
		179,
		191,
		197,
		211,
		223,
		229,
		239,
		251,
		257,
		263,
		269,
		277,
		283,
		293,
		307,
		317,
		331,
		347,
		359,
		367,
		379,
		389,
		397,
		409,
		419,
		431,
		443,
		457,
		467,
		479,
		491,
		503,
		521,
		541,
		557,
		569,
		587,
		599,
		613,
		631,
		647,
		661,
		677,
		691,
		709,
		727,
		743,
		761,
		787,
		809,
		827,
		853,
		877,
		907,
		929,
		953,
		977,
		997,
		1019,
		1049,
		1087,
		1109,
		1151,
		1181,
		1213,
		1249,
		1277,
		1303,
		1361,
		1399,
		1427,
		1459,
		1489,
		1523,
		1559,
		1597,
		1637,
		1693,
		1733,
		1777,
		1823,
		1861,
		1901,
		1949,
		1993,
		2039,
		2081,
		2129,
		2179,
		2237,
		2287,
		2333,
		2381,
		2437,
		2503,
		2557,
		2609,
		2663,
		2719,
		2777,
		2833,
		2897,
		2957,
		3019,
		3083,
		3163,
		3229,
		3299,
		3371,
		3449,
		3527,
		3607,
		3691,
		3767,
		3847,
		3929,
		4013,
		4099,
		4201,
		4289,
		4391,
		4481,
		4583,
		4679,
		4783,
		4889,
		4987,
		5087,
		5189,
		5297,
		5407,
		5519,
		5639,
		5779,
		5897,
		6029,
		6151,
		6277,
		6421,
		6551,
		6689,
		6823,
		6961,
		7103,
		7247,
		7393,
		7541,
		7699,
		7853,
		8011,
		8179,
		8353,
		8521,
		8693,
		8867,
		9049,
		9239,
		9431,
		9623,
		9817,
		10037,
		10243,
		10453,
		10663,
		10883,
		11113,
		11351,
		11579,
		11813,
		12071,
		12323,
		12577,
		12829,
		13093,
		13367,
		13649,
		13931,
		14221,
		14519,
		14813,
		15121,
		15427,
		15737,
		16057,
		16381,
		16729,
		17077,
		17419,
		17783,
		18143,
		18517,
		18899,
		19289,
		19681,
		20089,
		20507,
		20921,
		21341,
		21773,
		22229,
		22679,
		23143,
		23609,
		24083,
		24571,
		25073,
		25577,
		26099,
		26627,
		27179,
		27733,
		28289,
		28859,
		29437,
		30029,
		30631,
		31247,
		31873,
		32531,
		33191,
		33857,
		34537,
		35251,
		35963,
		36683,
		37423,
		38177,
		38953,
		39733,
		40529,
		41341,
		42169,
		43013,
		43889,
		44771,
		45667,
		46589,
		47521,
		48473,
		49451,
		50441,
		51461,
		52501,
		53569,
		54647,
		55763,
		56891,
		58031,
		59197,
		60383,
		61603,
		62851,
		64109,
		65393,
		66701,
		68041,
		69403,
		70793,
		72211,
		73673,
		75149,
		76667,
		78203,
		79769,
		81371,
		83003,
		84673,
		86369,
		88117,
		89891,
		91691,
		93529,
		95401,
		97327,
		99277,
		101267,
		101273,
		102019,
		102829,
		103787,
		104593,
		105397,
		106279,
		107053,
		107981,
		108821,
		109579,
		110503,
		111317,
		112153,
		113023,
		113843,
		114713,
		115597,
		116371,
		117239,
		118037,
		118907,
		119783,
		120647,
		121379,
		122209,
		123059,
		123887,
		124753,
		125621,
		126443,
		127331,
		128201,
		128981,
		129769,
		130633,
		131543,
		132409,
		133213,
		134089,
		135007,
		135757,
		136547,
		137393,
		138283,
		139177,
		139991,
		140813,
		141667,
		142537,
		143477,
		144341,
		145253,
		146051,
		146933,
		147761,
		148691,
		149423,
		150247,
		151157,
		151901,
		152783,
		153611,
		154523,
		155383,
		156253,
		157177,
		157999,
		158923,
		159773,
		160649,
		161521,
		162451,
		163243,
		164113,
		165047,
		165931,
		166847,
		167641,
		168631,
		169553,
		170351,
		171179,
		172079,
		172871,
		173807,
		174653,
		175673,
		176417,
		177257,
		178223,
		179021,
		179807,
		180623,
		181711,
		182489,
		183349,
		184199,
		185077,
		185893,
		186727,
		187559,
		188519,
		189391,
		190261,
		191137,
		192029,
		192853,
		193727,
		194681,
		195493,
		196429,
		197293,
		198139,
		198971,
		199889,
		200867,
		201757,
		202621,
		203459,
		204431,
		205327,
		206191,
		207061,
		207877,
		208697,
		209569,
		210347,
		211231,
		212131,
		213133,
		214007,
		214817,
		215833,
		216779,
		217643,
		218579,
		219433,
		220217,
		221093,
		222007,
		222919,
		223757,
		224669,
		225523,
		226433,
		227377,
		228281,
		229081,
		229837,
		230719,
		231589,
		232567,
		233549,
		234463,
		235307,
		236329,
		237217,
		238171,
		239027,
		239947,
		240853,
		241679,
		242521,
		243479,
		244367,
		245209,
		246131,
		246937,
		247873,
		248701,
		249539,
		250619,
		251417,
		252283,
		253307,
		254053,
		255023,
		255869,
		256801,
		257783,
		258617,
		259547,
		260483,
		261431,
		262349,
		263267,
		264113,
		265021,
		265921,
		266837,
		267601,
		268519,
		269341,
		270269,
		271127,
		272039,
		272933,
		273941,
		274847,
		275699,
		276557,
		277513,
		278479,
		279397,
		280297,
		281159,
		281959,
		282889,
		283859,
		284723,
		285611,
		286553,
		287537,
		288559,
		289369,
		290317,
		291167,
		292091,
		293071,
		294053,
		294923,
		295879,
		296753,
		297719,
		298681,
		299623
	};

	public static <K, V> MethodNameMap<K, V> clone(MethodNameMap<K, V> map) {
		return new MethodNameMap<>(map);
	}

	public static List<String> formatTempFileNames(Collection<String> fileNames) {
		ArrayList<String> displayNames = new ArrayList<>(fileNames.size());

		for (String fileName : fileNames) {
			if (TempFileManager.isManagedTempFile(fileName)) {
				displayNames.add(TempFileManager.getOriginalName(fileName) + " (" + fileName + ")");
			} else {
				displayNames.add(fileName);
			}
		}

		return displayNames;
	}

	public static <E> NamedSet<E> newNamedHashSet(String name, Collection<E> collection) {
		return (NamedSet<E>)(Options.ZKM_REPRODUCIBLE_RANDOM ? new NamedLinkedHashSet<>(name, collection) : new NamedHashSet<>(name, collection));
	}

	public static <E> HashSet<E> newHashSet() {
		return (HashSet<E>)(Options.ZKM_REPRODUCIBLE_RANDOM ? new LinkedHashSet<>() : new HashSet<>());
	}

	public static String longToBaseN(long value, char[] alphabet) {
		if (value < 0L) {
			throw new IllegalArgumentException("Must be positive " + value);
		} else if (value == 0L) {
			return String.valueOf(alphabet[0]);
		} else {
			int base = alphabet.length;

			StringBuilder builder;
			for (builder = new StringBuilder(); value > 0L; value /= base) {
				builder.append(alphabet[(int)(value % base)]);
			}

			return builder.reverse().toString();
		}
	}

	public static int getUTF8Length(String text) {
		return toUTF8(text).length;
	}

	public static Random newRandomWithSeed(long seed) {
		return new Random(seed);
	}

	public static String findHashedProperty(Map<String, String> properties, String hash, String defaultValue) {
		for (Entry<String, String> entry : properties.entrySet()) {
			String hashedKey = hashStringForPropertyLookup(entry.getKey());
			if (hash.equals(hashedKey)) {
				return entry.getValue();
			}
		}

		System.getProperties().propertyNames();
		Enumeration<?> propertyNamesEnumerator = System.getProperties().propertyNames();

		while (propertyNamesEnumerator.hasMoreElements()) {
			String propertyName = (String)propertyNamesEnumerator.nextElement();
			String hashedPropertyName = hashStringForPropertyLookup(propertyName);
			if (hash.equals(hashedPropertyName)) {
				return System.getProperty(propertyName);
			}
		}

		return defaultValue;
	}

	private static String hexdigit(int value) {
		switch (value) {
			case 0:
				return "0";
			case 1:
				return "1";
			case 2:
				return "2";
			case 3:
				return "3";
			case 4:
				return "4";
			case 5:
				return "5";
			case 6:
				return "6";
			case 7:
				return "7";
			case 8:
				return "8";
			case 9:
				return "9";
			case 10:
				return "A";
			case 11:
				return "B";
			case 12:
				return "C";
			case 13:
				return "D";
			case 14:
				return "E";
			case 15:
				return "F";
			default:
				throw new RuntimeException(value + " can't be translated to a hex digit");
		}
	}

	public static byte[] toUTF8(String text) {
		char[] chars = text.toCharArray();
		int byteCount = 0;
		int charCount = text.length();
		byte[] byteBuffer = new byte[charCount * 3];

		for (int i = 0; i < charCount; i++) {
			char c = chars[i];
			if (c == 0) {
				byteBuffer[byteCount++] = -64;
				byteBuffer[byteCount++] = -128;
			} else if (c < 128) {
				byteBuffer[byteCount++] = (byte)c;
			} else if (c < 2048) {
				byteBuffer[byteCount++] = (byte)(192 | c >>> 6 & 31);
				byteBuffer[byteCount++] = (byte)(128 | c & '?');
			} else {
				byteBuffer[byteCount++] = (byte)(224 | c >>> '\f' & 15);
				byteBuffer[byteCount++] = (byte)(128 | c >>> 6 & 63);
				byteBuffer[byteCount++] = (byte)(128 | c & '?');
			}
		}

		byte[] utf8Bytes = new byte[byteCount];
		System.arraycopy(byteBuffer, 0, utf8Bytes, 0, byteCount);
		return utf8Bytes;
	}

	public static String xorWithRepeatingKey(String input, String key) {
		char[] inputChars = input.toCharArray();
		char[] keyChars = key.toCharArray();
		StringBuilder builder = new StringBuilder(inputChars.length);

		for (int i = 0; i < inputChars.length; i++) {
			char keyChar;
			if (i < keyChars.length - 1) {
				keyChar = keyChars[i];
			} else {
				keyChar = keyChars[i % keyChars.length];
			}

			builder.append((char)((byte)inputChars[i] ^ (byte)keyChar));
		}

		return builder.toString();
	}

	public static <E> NamedSet<E> newNamedSet() {
		return (NamedSet<E>)(Options.ZKM_REPRODUCIBLE_RANDOM ? new NamedLinkedHashSet<>() : new NamedHashSet<>());
	}

	public static String xorString(String input, String key) {
		char[] inputChars = input.toCharArray();
		char[] keyChars = key.toCharArray();
		StringBuilder builder = new StringBuilder(inputChars.length);

		for (int i = 0; i < inputChars.length; i++) {
			char inputChar = inputChars[i];
			char keyChar;
			if (i < keyChars.length - 1) {
				keyChar = keyChars[i];
			} else {
				keyChar = keyChars[i % keyChars.length];
			}

			builder.append((char)((byte)inputChar ^ (byte)keyChar));
		}

		return builder.toString();
	}

	public static void shuffle(int[] array, int size, Random random) {
		for (int remaining = size; remaining > 1; remaining--) {
			swap(array, remaining - 1, random.nextInt(remaining));
		}
	}

	private static long generateRandomSeed(int seedOffset) {
		if (Options.ZKM_RANDOM_SEED != null) {
			try {
				long configuredSeed = Long.parseLong(Options.ZKM_RANDOM_SEED);
				long seed;
				if (configuredSeed > 0L && seedOffset < 0 || configuredSeed < 0L && seedOffset > 0) {
					seed = configuredSeed + seedOffset;
				} else {
					seed = configuredSeed - seedOffset;
				}

				return seed;
			} catch (Throwable ignored) {
			}
		}

		return generateRandomSeed();
	}

//   public static int getMaxLocals() {
//      if (Options.v != null) {
//         return 1;
//      } else if (Options.i) {
//         return 2;
//      } else if (Options.OK) {
//         return 3;
//      } else if (Options.zq != null) {
//         return 4;
//      } else if (Options.hz) {
//         return 5;
//      } else if (Options.zf) {
//         return 6;
//      } else if (Options.ze != null) {
//         return 7;
//      } else if (Options.Oo) {
//         return 8;
//      } else if (Options.Z) {
//         return 9;
//      } else if (Options.zY != null) {
//         return 10;
//      } else if (Options.Oy) {
//         return 11;
//      } else if (Options.o) {
//         return 12;
//      } else if (Options.hm != null) {
//         return 13;
//      } else if (Options.ln) {
//         return 14;
//      } else if (Options.ld) {
//         return 15;
//      } else if (Options.hv != null) {
//         return 16;
//      } else if (Options.lv) {
//         return 17;
//      } else if (Options.zH) {
//         return 18;
//      } else if (Options.O9 != null) {
//         return 19;
//      } else if (Options.z9) {
//         return 20;
//      } else if (Options.lG) {
//         return 21;
//      } else if (Options.hK != null) {
//         return 22;
//      } else if (Options.zx) {
//         return 23;
//      } else if (Options.l5) {
//         return 24;
//      } else if (Options.z != null) {
//         return 25;
//      } else if (Options.Q) {
//         return 26;
//      } else if (Options.lJ) {
//         return 27;
//      } else if (Options.Os != null) {
//         return 28;
//      } else if (Options.Oq) {
//         return 29;
//      } else if (Options.OS) {
//         return 30;
//      } else if (Options.ll != null) {
//         return 31;
//      } else if (Options.ha) {
//         return 32;
//      } else if (Options.a7) {
//         return 33;
//      } else if (Options.U != null) {
//         return 34;
//      } else if (Options.lH) {
//         return 35;
//      } else if (Options.hC) {
//         return 36;
//      } else if (Options.hH != null) {
//         return 37;
//      } else if (Options.y) {
//         return 38;
//      } else if (Options.z0) {
//         return 39;
//      } else if (Options.zE != null) {
//         return 40;
//      } else if (Options.Oz) {
//         return 41;
//      } else if (Options.l0) {
//         return 42;
//      } else if (Options.zm != null) {
//         return 43;
//      } else if (Options.OR) {
//         return 44;
//      } else if (Options.ho) {
//         return 45;
//      } else if (Options.lp != null) {
//         return 46;
//      } else if (Options.aN) {
//         return 47;
//      } else if (Options.z_) {
//         return 48;
//      } else if (Options.OE != null) {
//         return 49;
//      } else if (Options.Of) {
//         return 50;
//      } else if (Options.hc) {
//         return 51;
//      } else if (Options.zd != null) {
//         return 52;
//      } else if (Options.zC) {
//         return 53;
//      } else if (Options.zv) {
//         return 54;
//      } else if (Options.z7 != null) {
//         return 55;
//      } else if (Options.zR) {
//         return 56;
//      } else if (Options.zh) {
//         return 57;
//      } else if (Options.zW != null) {
//         return 58;
//      } else if (Options.zD) {
//         return 59;
//      } else if (Options.Ow) {
//         return 60;
//      } else if (Options.zS != null) {
//         return 61;
//      } else if (Options.hr) {
//         return 62;
//      } else if (Options.zn) {
//         return 63;
//      } else if (Options.zM != null) {
//         return 64;
//      } else if (Options.hL) {
//         return 65;
//      } else if (Options.OB) {
//         return 66;
//      } else if (Options.O3 != null) {
//         return 67;
//      } else if (Options.lS) {
//         return 68;
//      } else if (Options.Oi) {
//         return 69;
//      } else if (Options.l1 != null) {
//         return 70;
//      } else if (Options.Ox) {
//         return 71;
//      } else if (Options.zV) {
//         return 72;
//      } else if (Options.O1 != null) {
//         return 73;
//      } else if (Options.hu) {
//         return 74;
//      } else if (Options.Oe) {
//         return 75;
//      } else if (Options.Oc != null) {
//         return 76;
//      } else if (Options.z2) {
//         return 77;
//      } else if (Options.K) {
//         return 78;
//      } else if (Options.aS != null) {
//         return 79;
//      } else if (Options.zT) {
//         return 80;
//      } else if (Options.lx) {
//         return 81;
//      } else if (Options.zr != null) {
//         return 82;
//      } else if (Options.I) {
//         return 83;
//      } else if (Options.z1) {
//         return 84;
//      } else if (Options.OM != null) {
//         return 85;
//      } else if (Options.l8) {
//         return 86;
//      } else if (Options.L) {
//         return 87;
//      } else if (Options.hy != null) {
//         return 88;
//      } else if (Options.t) {
//         return 89;
//      } else if (Options.Og) {
//         return 90;
//      } else if (Options.zw != null) {
//         return 91;
//      } else if (Options.H) {
//         return 92;
//      } else if (Options.hk) {
//         return 93;
//      } else if (Options.lA != null) {
//         return 94;
//      } else if (Options.ax) {
//         return 95;
//      } else if (Options.zt) {
//         return 96;
//      } else if (Options.h3 != null) {
//         return 97;
//      } else if (Options.zi) {
//         return 98;
//      } else {
//         return Options.zN ? 99 : 0;
//      }
//   }

	public static int getNextPrime(int value, double scaler) {
		int scaledValue = (int)(value / scaler);
		int low = 0;
		int high = PRIME_NUMBER_LOOKUP_TBL.length - 1;
		int mid = 0;

		while (low <= high) {
			mid = (low + high) / 2;
			if (scaledValue < PRIME_NUMBER_LOOKUP_TBL[mid]) {
				high = mid - 1;
			} else {
				if (scaledValue <= PRIME_NUMBER_LOOKUP_TBL[mid]) {
					return PRIME_NUMBER_LOOKUP_TBL[mid];
				}

				low = mid + 1;
			}
		}

		if (PRIME_NUMBER_LOOKUP_TBL[mid] > scaledValue) {
			return PRIME_NUMBER_LOOKUP_TBL[mid];
		} else {
			return mid + 1 < PRIME_NUMBER_LOOKUP_TBL.length ? PRIME_NUMBER_LOOKUP_TBL[mid + 1] : scaledValue;
		}
	}

	public static <I, K> K getFromDualKeyMapOrDefault(I key, K defaultValue, DualKeyMap<I, K, K> map) {
		if (map != null) {
			K mappedValue = map.get(key, defaultValue);
			if (mappedValue != null) {
				return mappedValue;
			}
		}

		return defaultValue;
	}

	public static long generateRandomSeed() {
		return Options.dontUseSecureRandom ? new Random(System.nanoTime()).nextLong() : new SecureRandom(String.valueOf(System.nanoTime()).getBytes()).nextLong();
	}

	public static <E> void swap(E[] array, int index1, int index2) {
		E element = array[index1];
		array[index1] = array[index2];
		array[index2] = element;
	}

	public static <E> Set<E> toSet(E[] array) {
		HashSet<E> set = newHashSet(getInitialCapacity(array.length));

		for (E element : array) {
			if (!set.add(element)) {
				throw new IllegalArgumentException("The specified array must contain unique objects.");
			}
		}

		return set;
	}

	public static String findHashedSystemProperty(String hash, String defaultValue) {
		Enumeration<?> propertyNamesEnumerator = System.getProperties().propertyNames();

		while (propertyNamesEnumerator.hasMoreElements()) {
			String propertyName = (String)propertyNamesEnumerator.nextElement();
			String hashedPropertyName = hashStringForPropertyLookup(propertyName);
			if (hash.equals(hashedPropertyName)) {
				return System.getProperty(propertyName);
			}
		}

		return defaultValue;
	}

	public static byte[] baseNStringToByteArray(String encoded, String alphabet) {
		int encodedLength = encoded.length();
		byte[] bytes = new byte[encodedLength / 2];
		int byteIndex = 0;

		for (int charIndex = 0; charIndex < encodedLength; charIndex += 2) {
			bytes[byteIndex++] = (byte)baseNToLong(encoded.substring(charIndex, charIndex + 2), alphabet);
		}

		return bytes;
	}

	public static <K, V> Map<V, K> getInverseMap(Map<K, V> map) {
		HashMap<V, K> inverseMap = newHashMap(getInitialCapacity(map.size()));

		for (Entry<K, V> entry : map.entrySet()) {
			inverseMap.put(entry.getValue(), entry.getKey());
		}

		return inverseMap;
	}

	public static String generateComment(String[] lines) {
		return generateComment(true, lines);
	}

	public static String toBracedHexList(byte[] bytes) {
		if (bytes != null) {
			StringBuilder builder = new StringBuilder();
			builder.append("{");

			for (int i = 0; i < bytes.length; i++) {
				builder.append(toHex(bytes[i]));
				if (i < bytes.length - 1) {
					builder.append(',');
				}
			}

			return builder.toString() + "}";
		} else {
			return "{}";
		}
	}

	public static long getThreadId() {
		Thread thread = Thread.currentThread();
		if (!EnvironmentUtils.isRuntimeJava19OrNewer()) {
			return thread.getId();
		} else {
			Lookup lookup = MethodHandles.lookup();
			MethodType methodType = MethodType.methodType(long.class);
			Class<?> threadClass = thread.getClass();

			while (!threadClass.getName().equals("java.lang.Thread")) {
				threadClass = threadClass.getSuperclass();
			}

			try {
				MethodHandle threadIdHandle = lookup.findVirtual(threadClass, "threadId", methodType);
				return (long)threadIdHandle.invoke((Thread)thread);
			} catch (Throwable ignored) {
				if (EnvironmentUtils.isRuntimeJava19OrNewer()) {
				}

				return thread.getId();
			}
		}
	}

	public static String getCurrentTime() {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
		TimeZone timeZone = TimeZone.getDefault();
		dateFormat.setTimeZone(timeZone);
		return dateFormat.format(new Date());
	}

	public static long longHashCodeAbs(String text) {
		char[] charArray = text.toCharArray();
		long hash = 0L;
		if (charArray.length > 0) {
			for (int i = 0; i < charArray.length; i++) {
				hash = 31L * hash + charArray[i];
			}
		}

		return hash < 0L ? hash * -1L : hash;
	}

	public static void computeSortedSetDifferences(int[] left, int[] right, AtomicReference<int[]> onlyInRightRef, AtomicReference<int[]> onlyInLeftRef) {
		int maxLength;
		if (left.length > right.length) {
			maxLength = left.length;
		} else {
			maxLength = right.length;
		}

		int[] onlyInRight = new int[maxLength];
		int[] onlyInLeft = new int[maxLength];
		int rightOnlyCount = 0;
		int leftOnlyCount = 0;
		int leftIndex = 0;
		int rightIndex = 0;

		while (leftIndex < left.length && rightIndex < right.length) {
			if (left[leftIndex] == right[rightIndex]) {
				leftIndex++;
				rightIndex++;
			} else if (left[leftIndex] > right[rightIndex]) {
				onlyInRight[rightOnlyCount++] = right[rightIndex];
				rightIndex++;
			} else {
				onlyInLeft[leftOnlyCount++] = left[leftIndex];
				leftIndex++;
			}
		}

		while (leftIndex < left.length) {
			onlyInLeft[leftOnlyCount++] = left[leftIndex];
			leftIndex++;
		}

		while (rightIndex < right.length) {
			onlyInRight[rightOnlyCount++] = right[rightIndex];
			rightIndex++;
		}

		int[] leftDifference = new int[leftOnlyCount];
		System.arraycopy(onlyInLeft, 0, leftDifference, 0, leftOnlyCount);
		onlyInLeftRef.set(leftDifference);
		int[] rightDifference = new int[rightOnlyCount];
		System.arraycopy(onlyInRight, 0, rightDifference, 0, rightOnlyCount);
		onlyInRightRef.set(rightDifference);
	}

	public static <E> E getMappedOrDefault(E key, Map<E, E> map) {
		if (map != null) {
			E mappedValue = map.get(key);
			if (mappedValue != null) {
				return mappedValue;
			}
		}

		return key;
	}

	public static <K, V> HashMap<K, V> newHashMap(Map<K, V> source) {
		return (HashMap<K, V>)(Options.ZKM_REPRODUCIBLE_RANDOM ? new LinkedHashMap<>(source) : new HashMap<>(source));
	}

	public static String findHashedSystemProperty(String hash) {
		return findHashedSystemProperty(hash, null);
	}

	public static String hashStringForPropertyLookup(String input) {
		try {
			String salted = input + "xrcyorpnviag";
			return hashString(salted, "SHA-512");
		} catch (NoSuchAlgorithmException ignored) {
			DebugUtils.assertTrue(false, new String[]{ignored.toString()});
			return "";
		}
	}

	public static String md5Hex(String input) {
		try {
			return hashString(input, "MD5");
		} catch (NoSuchAlgorithmException ignored) {
			DebugUtils.assertTrue(false, new String[]{"(B)", ignored.toString()});
			return "";
		}
	}

	public static int toUnsignedShort(int value) {
		return value & 65535;
	}

	public static <E> CountingSet<E> clone(CountingSet<E> set) {
		return set.clone();
	}

	public static void listProperties(Properties properties, PrintWriter writer) {
		listProperties("", properties, writer);
	}

	public static String getStackTrace(Throwable throwable) {
		ByteArrayOutputStream stackTraceBytes = new ByteArrayOutputStream();
		PrintWriter stackTraceWriter = new PrintWriter(stackTraceBytes);
		throwable.printStackTrace(stackTraceWriter);
		stackTraceWriter.close();
		return stackTraceBytes.toString();
	}

	public static <I, K, V> DualKeyMap<I, K, V> clone(DualKeyMap<I, K, V> map) {
		return map.clone();
	}

	public static void shuffle(char[] array, Random random) {
		for (int remaining = array.length; remaining > 1; remaining--) {
			swap(array, remaining - 1, random.nextInt(remaining));
		}
	}

	public static String generateComment(boolean javadocStyle, String[] lines) {
		String openingDelimiter;
		String closingDelimiter;
		int fillChar;
		String linePrefix;
		String lineSuffix;
		if (javadocStyle) {
			openingDelimiter = "/**";
			closingDelimiter = "**/";
			fillChar = 42;
			linePrefix = "/* ";
			lineSuffix = " */";
		} else {
			openingDelimiter = "///";
			closingDelimiter = "///";
			fillChar = 47;
			linePrefix = "// ";
			lineSuffix = " //";
		}

		int totalLineLength = 0;
		int maxLineLength = 0;
		String[] lineArray = lines;
		int lineCount = lines.length;

		for (int lineIndex = 0; lineIndex < lineCount; lineIndex++) {
			int lineLength = lineArray[lineIndex].length();
			totalLineLength += lineLength;
			if (lineLength > maxLineLength) {
				maxLineLength = lineLength;
			}
		}

		StringBuilder builder = new StringBuilder(totalLineLength + lines.length * 6 + (maxLineLength + 6) * 2);
		builder.append(openingDelimiter + StringUtils.justificateString("", 76, maxLineLength, fillChar) + closingDelimiter);
		builder.append(LINE_SEPARATOR);

		for (String line : lines) {
			builder.append(linePrefix + StringUtils.justificateString(line, 76, maxLineLength, 32) + lineSuffix);
			builder.append(LINE_SEPARATOR);
		}

		builder.append(openingDelimiter + StringUtils.justificateString("", 76, maxLineLength, fillChar) + closingDelimiter);
		builder.append(LINE_SEPARATOR);
		return builder.toString();
	}

	public static long baseNToLong(String encoded, String alphabet) {
		long value = 0L;
		encoded = new StringBuilder(encoded).reverse().toString();
		int base = alphabet.length();
		long placeValue = 1L;

		for (char digitChar : encoded.toCharArray()) {
			value += alphabet.indexOf(digitChar) * placeValue;
			placeValue *= base;
		}

		return value;
	}

	@SuppressWarnings("unchecked")
	public static <E> ArrayList<E> clone(ArrayList<E> list) {
		return (ArrayList<E>)list.clone();
	}

	public static String toString(String[] parts) {
		StringBuilder builder = new StringBuilder();

		for (String part : parts) {
			builder.append(part);
		}

		return builder.toString();
	}

	public static <E> boolean isUnique(E[] array) {
		HashSet<E> seen = newHashSet(getInitialCapacity(array.length));

		for (E element : array) {
			if (!seen.add(element)) {
				return false;
			}
		}

		return true;
	}

	public static <E> NamedSet<E> newNamedHashSet(String name, Map<E, ?> map) {
		return (NamedSet<E>)(Options.ZKM_REPRODUCIBLE_RANDOM ? new NamedLinkedHashSet<>(name, map) : new NamedHashSet<>(name, map));
	}

	public static String toString(Collection<String> items) {
		StringBuilder builder = new StringBuilder();
		int itemCount = items.size();
		int index = 0;

		for (String item : items) {
			builder.append(item);
			if (index < itemCount - 2) {
				builder.append(", ");
			} else if (index < itemCount - 1) {
				builder.append(" and ");
			}

			index++;
		}

		return builder.toString();
	}

	public static <K, V> Map<K, V> clone(Map<K, V> map) {
		return newHashMap(map);
	}

	public static <E> void shuffle(E[] array, Random random) {
		for (int remaining = array.length; remaining > 1; remaining--) {
			swap(array, remaining - 1, random.nextInt(remaining));
		}
	}

	public static String internalToBinaryName(String internalName) {
		return internalName.replace('/', '.');
	}

	@SuppressWarnings("unchecked")
	public static <E> NamedSet<E> clone(NamedSet<E> set) {
		return (NamedSet<E>)set.clone();
	}

	public static String getTimestampPrefix() {
		return "[" + getCurrentTime() + "]";
	}

	public static long longHashCodeAbs2(String text) {
		char[] charArray = text.toCharArray();
		long hash = 0L;
		if (charArray.length > 0) {
			for (int i = 0; i < charArray.length; i++) {
				hash = 31L * hash + charArray[i];
			}
		}

		return hash < 0L ? hash * -1L : hash;
	}

	public static void swap(char[] array, int index1, int index2) {
		char element = array[index1];
		array[index1] = array[index2];
		array[index2] = element;
	}

	@SuppressWarnings("unchecked")
	public static <E> HashSet<E> clone(HashSet<E> set) {
		return (HashSet<E>)set.clone();
	}

	public static String typeDescriptorToBinaryName(String descriptor) {
		String binaryName = descriptor;
		int arrayDimensions = 0;
		if (descriptor.startsWith("[")) {
			arrayDimensions = StringUtils.countOccurrences(descriptor, "[".charAt(0));
			binaryName = descriptor.substring(arrayDimensions);
		}

		if (binaryName.endsWith(";")) {
			binaryName = binaryName.substring(1, binaryName.length() - 1);
		} else if (binaryName.length() == 1) {
			binaryName = TypeWriter.getPrimitiveName(binaryName);
		}

		binaryName = binaryName.replace('/', '.');

		for (int i = 0; i < arrayDimensions; i++) {
			binaryName = binaryName + "[]";
		}

		return binaryName;
	}

	public static <E> List<E> toList(Enumeration<E> enumeration) {
		ArrayList<E> list = new ArrayList<>();

		while (enumeration.hasMoreElements()) {
			list.add(enumeration.nextElement());
		}

		return list;
	}

	public static boolean areIntArraysEqual(int[] array1, int[] array2) {
		if (array1.length != array2.length) {
			return false;
		} else {
			for (int i = 0; i < array1.length; i++) {
				if (array1[i] != array2[i]) {
					return false;
				}
			}

			return true;
		}
	}

	public static int getInitialCapacity(int length) {
		return getNextPrime(length, 0.74);
	}

	public static <K, V> MultiMap<V, K> invertMultiMap(IMultiMap<K, V> multiMap) {
		ReferenceGraph<V, K> inverseGraph = new ReferenceGraph<>();

		for (Entry<K, List<V>> entry : multiMap.entrySet()) {
			K key = entry.getKey();

			for (V value : entry.getValue()) {
				inverseGraph.addReference(value, key);
			}
		}

		return inverseGraph.toMultiMap();
	}

	public static String toHex(int value) {
		int unsignedValue = ByteUtils.toUnsigned(value);
		int highNibble = unsignedValue / 16;
		int lowNibble = unsignedValue % 16;
		return hexdigit(highNibble) + hexdigit(lowNibble);
	}

	public static void swap(int[] array, int index1, int index2) {
		int element = array[index1];
		array[index1] = array[index2];
		array[index2] = element;
	}

	public static <T> T[] clone(T[] array) {
		return (T[])((Object[])array.clone());
	}

	public static <K, V> HashMap<K, V> newHashMap(int size, float loadFactor) {
		return (HashMap<K, V>)(Options.ZKM_REPRODUCIBLE_RANDOM ? new LinkedHashMap<>(size, loadFactor) : new HashMap<>(size, loadFactor));
	}

	private static String hashString(String input, String algorithm) throws NoSuchAlgorithmException {
		MessageDigest digest = MessageDigest.getInstance(algorithm);
		digest.update(input.getBytes(), 0, input.length());
		StringBuilder builder = new StringBuilder();
		byte[] digestBytes = digest.digest();

		for (int i = 0; i < digestBytes.length; i++) {
			String hexByte = Integer.toHexString(digestBytes[i] & 255);
			if (hexByte.length() == 1) {
				builder.append('0');
			}

			builder.append(hexByte);
		}

		return builder.toString();
	}

	public static String binaryToInternalName(String binaryName) {
		return binaryName.replace('.', '/');
	}

	public static <K, V> HashMap<K, V> newHashMap(int size) {
		return (HashMap<K, V>)(Options.ZKM_REPRODUCIBLE_RANDOM ? new LinkedHashMap<>(size) : new HashMap<>(size));
	}

	public static <E> HashSet<E> newHashSet(int size) {
		return (HashSet<E>)(Options.ZKM_REPRODUCIBLE_RANDOM ? new LinkedHashSet<>(size) : new HashSet<>(size));
	}

	public static String truncateAndEscapeString(String text, int maxLength) {
		return text.length() > maxLength ? escapeString(text.substring(0, maxLength)) + " ...<" + (text.length() - maxLength) + " CHARS>" : escapeString(text);
	}

	public static void listProperties(String title, Properties properties, PrintWriter writer) {
		writer.println("-- Listing " + title + " properties --");
		Enumeration<?> keys = properties.keys();

		while (keys.hasMoreElements()) {
			Object key = keys.nextElement();
			Object value = properties.get(key);
			writer.println(key.toString() + " = " + value.toString());
		}
	}

	public static long obfuscateLong(long value, String key) {
		char[] keyChars = key.toCharArray();
		long mask = 0L;

		for (int i = 0; i < 8; i++) {
			char keyChar;
			if (i < keyChars.length - 1) {
				keyChar = keyChars[i];
			} else {
				keyChar = keyChars[i % keyChars.length];
			}

			if (i == 0) {
				mask |= keyChar & 7;
			} else {
				mask |= keyChar & 15;
			}

			mask <<= 4;
		}

		return value ^ mask;
	}

	public static void shuffleWithFixedSeed(char[] array) {
		shuffle(array, newRandom(97));
	}

	public static <E> boolean mergeListsAndReportConflicts(List<E> sourceList, List<E> targetList) {
		boolean conflictDetected = false;
		HashSet<E> seen = newHashSet(getInitialCapacity(sourceList.size() + targetList.size()));
		seen.addAll(targetList);

		for (E element : sourceList) {
			if (seen.add(element)) {
				targetList.add(element);
			} else {
				conflictDetected = true;
			}
		}

		return conflictDetected;
	}

	public static <E> HashSet<E> newHashSet(Collection<E> collection) {
		return (HashSet<E>)(Options.ZKM_REPRODUCIBLE_RANDOM ? new LinkedHashSet<>(collection) : new HashSet<>(collection));
	}

	public static String stringArrayToQuotedList(String[] strings) {
		StringBuilder builder = new StringBuilder();
		builder.append("'");

		for (int i = 0; i < strings.length; i++) {
			builder.append(strings[i]);
			if (i < strings.length - 2) {
				builder.append("', '");
			} else if (i < strings.length - 1) {
				builder.append("' and '");
			}
		}

		builder.append("'");
		return builder.toString();
	}

	@SuppressWarnings("unchecked")
	public static <E> LinkedHashSet<E> cloneLinkedHashSet(LinkedHashSet<E> set) {
		return (LinkedHashSet<E>)set.clone();
	}

	@SuppressWarnings("unchecked")
	public static <E> void shuffle(List<E> list, Random random) {
		int size = list.size();
		if (size >= 10 && !(list instanceof RandomAccess)) {
			Object[] elements = list.toArray();
			shuffle(elements, random);
			ListIterator<E> listIterator = list.listIterator();

			for (int i = 0; i < elements.length; i++) {
				listIterator.next();
				listIterator.set((E)elements[i]);
			}
		} else {
			for (int remaining = size; remaining > 1; remaining--) {
				swap(list, remaining - 1, random.nextInt(remaining));
			}
		}
	}

	public static <K, V> HashMap<K, V> newHashMap() {
		return (HashMap<K, V>)(Options.ZKM_REPRODUCIBLE_RANDOM ? new LinkedHashMap<>() : new HashMap<>());
	}

	public static String escapeString(String text) {
		char[] chars = text.toCharArray();
		StringBuilder builder = new StringBuilder();

		for (char c : chars) {
			int charCode = Integer.valueOf(c);
			switch (charCode) {
				case 10:
					builder.append("\\n");
					break;
				case 12:
					builder.append("\\f");
					break;
				case 13:
					builder.append("\\r");
					break;
				case 34:
					builder.append("\\\"");
					break;
				case 92:
					builder.append("\\\\");
					break;
				default:
					if (charCode >= 32 && charCode <= 126) {
						builder.append(c);
					} else {
						builder.append(toUnicodeEscapedString(charCode));
					}
			}
		}

		return builder.toString();
	}

	public static String toUnicodeEscapedString(int charCode) {
		String hex = Integer.toHexString(charCode);
		return "\\u" + StringUtils.justificateString(hex, 82, 4, 48);
	}

	public static String unescapeString(String text) {
		StringBuilder builder = new StringBuilder(text.length());

		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (c == '\\' && i + 1 < text.length()) {
				char escaped = text.charAt(i + 1);
				switch (escaped) {
					case '"':
						builder.append('"');
						i++;
						break;
					case '\'':
						builder.append('\'');
						i++;
						break;
					case '\\':
						builder.append('\\');
						i++;
						break;
					case 'n':
						builder.append('\n');
						i++;
						break;
					case 'r':
						builder.append('\r');
						i++;
						break;
					case 't':
						builder.append('\t');
						i++;
						break;
					case 'u':
						if (i + 5 < text.length()) {
							String hexCode = text.substring(i + 2, i + 6);

							try {
								builder.append((char)Integer.parseInt(hexCode, 16));
								i += 5;
								break;
							} catch (NumberFormatException ignored) {
							}
						}

						builder.append(c);
						break;
					default:
						builder.append(c);
				}
			} else {
				builder.append(c);
			}
		}

		return builder.toString();
	}

	public static String toQuotedString(Collection<String> items) {
		StringBuilder builder = new StringBuilder();
		builder.append("'");
		int itemCount = items.size();
		int index = 0;

		for (String item : items) {
			builder.append(item);
			if (index < itemCount - 2) {
				builder.append("', '");
			} else if (index < itemCount - 1) {
				builder.append("' and '");
			}

			index++;
		}

		builder.append("'");
		return builder.toString();
	}

	public static boolean isClassPresent(String className) {
		try {
			Class.forName(className);
			return true;
		} catch (ClassNotFoundException ignored) {
			return false;
		}
	}

	public static <I, T> T getFromRegistryOrDefault(I key, T defaultValue, ReadOnlyRegistryView<I, T, T> registry) {
		if (registry != null) {
			T registeredValue = registry.get(key, defaultValue);
			if (registeredValue != null) {
				return registeredValue;
			}
		}

		return defaultValue;
	}

	public static byte[] baseNStringToByteArray(String text, int radix) {
		DebugUtils.assertTrue(radix <= 36, new String[]{"MAX_RADIX is too small : 36 < " + radix});
		int length = text.length();
		byte[] array = new byte[length / 2];
		int byteIndex = 0;

		for (int i = 0; i < length; i += 2) {
			array[byteIndex++] = (byte)Integer.parseInt(text.substring(i, i + 2), radix);
		}

		return array;
	}

	public static String getKlassMasterClassName() {
		return xorWithRepeatingKey(
			"\t\u000e\u001bOU\t\r\u0007\u001f\u0001$\u000e\u000b\u0016\u0010\u0019\u000b\u0012\u0002\u0004]B*\u0002\u0006\\</\u000b\u0016\u0017\u0011\u0018",
			"java/lang/Object"
		);
	}

	public static Random newRandom(int seedOffset) {
		long seed = generateRandomSeed(seedOffset);
		Random random;
		if (Options.ZKM_REPRODUCIBLE_RANDOM) {
			random = new Random(seed);
		} else {
			random = (Random)(Options.dontUseSecureRandom ? new Random() : new SecureRandom());
			random.setSeed(seed);
		}

		return random;
	}

	public static <E> boolean areEquals(E[] array1, E[] array2) {
		if (array1.length != array2.length) {
			return false;
		} else {
			for (int i = 0; i < array1.length; i++) {
				if (!array1[i].equals(array2[i])) {
					return false;
				}
			}

			return true;
		}
	}

	private static <E> void swap(List<E> list, int index1, int index2) {
		E element = list.get(index1);
		list.set(index1, list.get(index2));
		list.set(index2, element);
	}

	public static String fromUTF8(byte[] bytes) {
		int charCount = 0;
		int byteCount = bytes.length;
		char[] chars = new char[byteCount];

		for (int i = 0; i < byteCount; i++) {
			int leadByte = 255 & bytes[i];
			if (leadByte < 192) {
				chars[charCount++] = (char)leadByte;
			} else if (leadByte < 224) {
				char twoByteChar = (char)((char)(leadByte & 31) << 6);
				int continuationByte = bytes[++i];
				twoByteChar = (char)(twoByteChar | (char)(continuationByte & 63));
				chars[charCount++] = twoByteChar;
			} else if (i < byteCount - 2) {
				char threeByteChar = (char)((char)(leadByte & 15) << '\f');
				int firstContinuation = bytes[++i];
				threeByteChar = (char)(threeByteChar | (char)(firstContinuation & 63) << 6);
				int secondContinuation = bytes[++i];
				threeByteChar = (char)(threeByteChar | (char)(secondContinuation & 63));
				chars[charCount++] = threeByteChar;
			}
		}

		return new String(chars, 0, charCount);
	}

	public static String findHashedProperty(String hash, String defaultValue, Properties properties) {
		if (properties == null) {
			return defaultValue;
		} else {
			Enumeration<?> propertyNamesEnumerator = properties.propertyNames();

			while (propertyNamesEnumerator.hasMoreElements()) {
				String propertyName = (String)propertyNamesEnumerator.nextElement();
				String hashedPropertyName = hashStringForPropertyLookup(propertyName);
				if (hash.equals(hashedPropertyName)) {
					return properties.getProperty(propertyName);
				}
			}

			return defaultValue;
		}
	}

	public static String toHex(byte[] bytes) {
		if (bytes == null) {
			return "";
		} else {
			StringBuilder builder = new StringBuilder();

			for (int i = 0; i < bytes.length; i++) {
				builder.append(toHex(bytes[i]));
			}

			return builder.toString();
		}
	}

	public static <E> NamedSet<E> newNamedHashSet(Collection<E> collection) {
		return (NamedSet<E>)(Options.ZKM_REPRODUCIBLE_RANDOM ? new NamedLinkedHashSet<>(collection) : new NamedHashSet<>(collection));
	}

	public static void shuffle(int[] array, Random random) {
		shuffle(array, array.length, random);
	}
}
