package com.zelix.tools.parameterChanger.salt;

import com.zelix.framework.AbstractClassNode;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.wrappers.MethodNode;
import com.zelix.klassmaster.config.Options;
import com.zelix.tools.parameterChanger.ParameterChangerBuilder;
import com.zelix.tools.parameterChanger.dataset.ChangedMethod;
import com.zelix.utils.DebugUtils;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.collections.*;
import com.zelix.utils.dataset.AtomicBool;
import com.zelix.utils.dataset.AtomicInt;
import com.zelix.utils.dataset.AtomicLong;
import com.zelix.utils.dataset.Pair;
import com.zelix.utils.references.AtomicReference;

import java.util.*;
import java.util.Map.Entry;
import java.util.PrimitiveIterator.OfLong;

public class SaltManager {
	private static Iterator<Long> randomLowBits;
	private static int maxSortDepth;
	private final List<int[]> permutationTables = new ArrayList<>();
	private final List<Pair> duplicatePairs = new ArrayList<>();
	private boolean unusedFlag = false;
	private Random random;
	private final Iterator<Long> saltValueSource;
	private final MultiMap<int[], SaltEntry> entriesByTable;
	private final Set<int[]> tables;
	private final MasterSaltEntry masterEntry;
	private Set<SaltEntry> pendingEntries;
	private Set<SaltEntry> primaryEntries;
	private Iterator<Long> randomLongs;
	private Set<Long> usedValues;
	private List<SaltEntry> entries;
	private int maxSaltCount;
	private int initialEntryCount;
	private Set<int[]> pairedOnlyTables;
	private Set<int[]> primaryOnlyTables;
	private int initialCapacity;
	private int indexBitOffset;
	private int minExtraPairs;
	private List<SaltEntry> initialEntries;
	private Set<SaltEntry> pairedEntries;

	public SaltManager(
		List<ChangedMethod> changedMethods,
		List<Object> staticValues,
		List<Object> mapValues,
		AtomicReference<int[]> tableRef,
		List<Object> tableAssignments,
		List<Pair> chainPairs,
		Map<MethodNode, long[]> methodSaltValues,
		Iterator<Long> saltValueSource,
		Random random
	) {
		this.random = random;
		this.saltValueSource = saltValueSource;
		this.entriesByTable = new MultiMap<>();
		this.tables = GlobalUtils.newHashSet();
		this.masterEntry = null;
		int[] bitPermutation = new int[64];
		staticValues.add(bitPermutation);
		tableRef.set(bitPermutation);

		for (ChangedMethod changedMethod : changedMethods) {
			MethodNode methodNode = changedMethod.getMethodNode();
			long[] existingValues = methodSaltValues.get(methodNode);
			if (existingValues != null) {
				this.assignSaltPairFromValues(bitPermutation, staticValues, changedMethod, existingValues);
			} else {
				long[] generatedValues = new long[]{saltValueSource.next(), saltValueSource.next(), saltValueSource.next(), saltValueSource.next(), saltValueSource.next()};
				this.assignSaltPairFromValues(bitPermutation, staticValues, changedMethod, generatedValues);
			}
		}

		tableAssignments.add(0L);
		tableAssignments.add(bitPermutation);
		chainPairs.add(new Pair(0L, 1L));
	}

	public SaltManager(
		List<ChangedMethod> changedMethods,
		List<ChangedMethod> targetMethods,
		List<Object> staticValues,
		List<Object> mapValues,
		AtomicReference<int[]> tableRef,
		List<Object> tableAssignments,
		List<Pair> chainPairs,
		List<Pair> valuePairs,
		List<SaltOrder> saltOrders,
		AtomicReference<int[]> finalTableRef,
		Iterator<Long> saltValueSource,
		Random random,
		boolean java7OrNewer
	) {
		this.random = random;
		this.saltValueSource = saltValueSource;
		SaltManager.ChangedMethodComparator methodComparator = new SaltManager.ChangedMethodComparator(this);
		Collections.sort(changedMethods, methodComparator);
		Collections.sort(targetMethods, methodComparator);
		this.initSizes(changedMethods.size(), targetMethods.size());
		this.entriesByTable = new MultiMap<>(true, this.initialCapacity / 128);
		this.tables = new LinkedHashSet<>(GlobalUtils.getInitialCapacity(this.initialCapacity / 128));
		this.buildInitialEntries(staticValues, targetMethods.size(), java7OrNewer);
		this.masterEntry = MasterSaltEntry.of(this.random, this.initialCapacity, mapValues, this);
		KeyValuePairMultiMap<String, SaltEntry, Salt> groupedEntries = this.masterEntry.getEntryPairsByKey();
		this.initEntrySets(groupedEntries);
		this.assignTables(tableAssignments);
		IdentitySet<SaltEntry> assignedPrimaryEntries = new IdentitySet<>(this.primaryEntries.size());
		IdentitySet<SaltEntry> assignedPartneredEntries = new IdentitySet<>(this.pairedEntries.size());
		IdentityMap<SaltEntry, ChangedMethod> entryToMethod = new IdentityMap<>(GlobalUtils.getInitialCapacity(targetMethods.size()));
		int[] bitPermutation = ParameterChangerBuilder.createBitPermutation(this.random);
		this.pushTable(bitPermutation);
		tableRef.set((int[])this.currentTable().clone());
		this.unusedFlag = true;
		this.assignSaltPairs(assignedPrimaryEntries, assignedPartneredEntries, targetMethods, entryToMethod);
		this.buildSaltChains(assignedPrimaryEntries, assignedPartneredEntries, changedMethods, targetMethods, entryToMethod, chainPairs, valuePairs, saltOrders);
		int[] currentPermutation = this.currentTable();
		int[] permutationCopy = new int[currentPermutation.length];
		System.arraycopy(currentPermutation, 0, permutationCopy, 0, currentPermutation.length);
		finalTableRef.set(permutationCopy);
	}

	private static long deriveLinkValue(long baseValue, long entryValue, int[] table, AtomicLong xorOut, Set<Long> existingKeys, Random random) {
		long entryKey = ParameterChangerBuilder.permuteAllBits(entryValue, table) & 72057594037927935L;
		long baseDerived = ParameterChangerBuilder.permuteBits(baseValue, table);
		long baseKey = baseDerived & 72057594037927935L;
		long baseMiddleBits = baseDerived >>> 8 & 281474976710655L;
		long baseLowBits = baseDerived & 255L;
		OfLong longIterator = random.longs(0L, 72057594037927936L).iterator();

		long newKey;
		long composedBits;
		long randomBits;
		do {
			randomBits = longIterator.next();
			long shiftedBits = randomBits << 8;
			composedBits = shiftedBits | baseLowBits;
			long composedKey = composedBits & 72057594037927935L;
			newKey = entryKey ^ baseKey ^ composedKey;
		} while ((randomBits & 281474976710655L ^ baseMiddleBits) == 0L || existingKeys != null && existingKeys.contains(newKey));

		randomBits = ParameterChangerBuilder.permuteBits(composedBits, table);
		long combinedValue = entryValue ^ baseValue ^ randomBits;
		xorOut.set(combinedValue);
		if (existingKeys != null) {
			existingKeys.remove(entryKey);
			existingKeys.add(newKey);
		}

		return randomBits;
	}

	private static void merge(int low, int mid, int high, List<SaltEntry> entries, List<SaltEntry> scratch) {
		int leftIndex = low;
		int rightIndex = mid + 1;

		for (int i = low; i <= high; i++) {
			scratch.set(i, entries.get(i));
		}

		while (leftIndex <= mid && rightIndex <= high) {
			SaltEntry chosenEntry;
			if (scratch.get(leftIndex).isLessThanOrEqual(scratch.get(rightIndex))) {
				chosenEntry = scratch.get(leftIndex++);
			} else {
				chosenEntry = scratch.get(rightIndex++);
			}

			entries.set(low, chosenEntry);
			low++;
		}

		while (leftIndex <= mid) {
			entries.set(low, scratch.get(leftIndex));
			low++;
			leftIndex++;
		}
	}

	private static void sortEntries(List<SaltEntry> entries) {
		mergeSort(0, entries.size() - 1, entries, new ArrayList<>(entries), 0);
	}

	private static int maxSortDepthFor(int count) {
		byte sortDepth;
		if (count <= 14336 || Options.useMaxSaltCount) {
			sortDepth = 17;
		} else if (count <= 16384) {
			sortDepth = 12;
		} else if (count <= 20480) {
			sortDepth = 10;
		} else if (count <= 24576) {
			sortDepth = 8;
		} else if (count <= 28672) {
			sortDepth = 6;
		} else if (count <= 32768) {
			sortDepth = 4;
		} else if (count <= 35840) {
			sortDepth = 2;
		} else {
			sortDepth = 1;
		}

		return sortDepth;
	}

	public static int maxSaltCountFor(int methodCount, AtomicInt valuesPerEntry) {
		int minimumSaltCount = 0;
		if (Options.minSaltSize != null) {
			try {
				minimumSaltCount = Integer.parseInt(Options.minSaltSize);
			} catch (NumberFormatException ignored) {
			}
		}

		int saltCount;
		if (methodCount <= 2010) {
			saltCount = methodCount * 6;
			valuesPerEntry.set(2);
		} else if (methodCount >= 4510) {
			saltCount = methodCount * 3;
			valuesPerEntry.set(5);
		} else {
			float ratio = (methodCount - 2010) / 2500.0F;
			saltCount = (int)(methodCount * (6.0F - 3.0F * ratio));
			valuesPerEntry.set(2 + Math.round(3.0F * ratio));
		}

		return Math.max(saltCount, Math.max(2048, minimumSaltCount));
	}

	private static void mergeSort(int low, int high, List<SaltEntry> entries, List<SaltEntry> scratch, int depth) {
		if (low < high) {
			int mid = low + (high - low) / 2;
			if (++depth < maxSortDepth) {
				mergeSort(low, mid, entries, scratch, depth);
				mergeSort(mid + 1, high, entries, scratch, depth);
			}

			merge(low, mid, high, entries, scratch);
		}
	}

	private void assignSaltPairs(Set<SaltEntry> primaryEntries, Set<SaltEntry> partneredEntries, List<ChangedMethod> targetMethods, IdentityMap<SaltEntry, ChangedMethod> entryToMethod) {
		Map<SaltEntry, Salt> saltByEntry = this.masterEntry.getEntryMap();
		int methodCount = targetMethods.size();
		ArrayList<Pair> entryPairs = new ArrayList<>(methodCount);
		this.usedValues = GlobalUtils.newHashSet(GlobalUtils.getInitialCapacity(this.primaryEntries.size()));
		ArrayList<SaltEntry> remainingEntries = new ArrayList<>(this.primaryEntries.size());

		for (SaltEntry primaryEntry : this.primaryEntries) {
			remainingEntries.add(primaryEntry);
		}

		SaltSorter saltSorter = new SaltSorter(this);
		Collections.sort(remainingEntries, saltSorter);

		do {
			DebugUtils.assertTrue(
				!remainingEntries.isEmpty(), new String[]{"Insufficient entries : " + this.primaryEntries.size() + " : " + methodCount + " : " + entryPairs.size() + " : " + this.pendingEntries.size()}
			);
			int randomIndex = this.random.nextInt(remainingEntries.size());
			SaltEntry pickedEntry = remainingEntries.remove(randomIndex);
			if (!this.pendingEntries.contains(pickedEntry) && !this.pairedEntries.contains(pickedEntry)) {
				Salt partnerSalt = saltByEntry.get(pickedEntry);
				if (partnerSalt.isSaltEntry()) {
					SaltEntry partnerEntry = (SaltEntry)partnerSalt;
					if (!this.pendingEntries.contains(partnerEntry) && !partneredEntries.contains(partnerEntry) && !this.primaryEntries.contains(partnerEntry)) {
						this.usedValues.add(pickedEntry.getMidBits());
						primaryEntries.add(pickedEntry);
						partneredEntries.add(partnerEntry);
						entryPairs.add(new Pair(pickedEntry, partnerEntry));
					}
				}
			}
		} while (entryPairs.size() < methodCount);

		int methodIndex = 0;
		HashSet<ChangedMethod> methodsWithInnerClasses = GlobalUtils.newHashSet();
		HashMap<ClassNode, ChangedMethod> methodByClass = GlobalUtils.newHashMap();

		for (Pair entryPair : entryPairs) {
			ChangedMethod changedMethod = targetMethods.get(methodIndex++);
			ClassNode ownerClass = changedMethod.getMethodNode().getMethodOwner();
			if (ownerClass.hasInnerClasses()) {
				methodsWithInnerClasses.add(changedMethod);
			} else if (ownerClass.isInnerClass()) {
				methodByClass.put(ownerClass, changedMethod);
				continue;
			}

			SaltEntry keyEntry = (SaltEntry)entryPair.getKey();
			SaltEntry valueEntry = (SaltEntry)entryPair.getValue();
			long keySaltValue = this.createEntrySaltValue(keyEntry);
			long randomSaltValue = this.createRandomSaltValue();
			changedMethod.setSaltEntries(keyEntry, valueEntry, keySaltValue, randomSaltValue);
			entryToMethod.put(valueEntry, changedMethod);
		}

		for (ChangedMethod outerMethod : methodsWithInnerClasses) {
			for (AbstractClassNode innerClass : outerMethod.getMethodNode().getMethodOwner().getInnerClasses()) {
				ChangedMethod innerMethod = methodByClass.get(innerClass);
				innerMethod.setSaltEntries(outerMethod.getKeyEntry(), outerMethod.getValueEntry(), outerMethod.getMethodValue(), outerMethod.getRandomSaltValue());
			}
		}
	}

	private void buildSaltChains(
		Set<SaltEntry> primaryEntries,
		Set<SaltEntry> partneredEntries,
		List<ChangedMethod> changedMethods,
		List<ChangedMethod> targetMethods,
		IdentityMap<SaltEntry, ChangedMethod> entryToMethod,
		List<Pair> chainPairs,
		List<Pair> valuePairs,
		List<SaltOrder> saltOrders
	) {
		sortEntries(this.entries);
		int entryIndex = 0;
		Iterator<SaltEntry> entryIterator = this.entries.iterator();

		while (entryIterator.hasNext()) {
			entryIterator.next().setIndex(entryIndex++);
		}

		IdentitySet<SaltEntry> assignedEntries = new IdentitySet<>(primaryEntries.size() + partneredEntries.size());
		assignedEntries.addAll(primaryEntries);
		assignedEntries.addAll(partneredEntries);
		OfLong longIterator = this.random.longs(1L, 281474976710655L).iterator();

		for (ChangedMethod targetMethod : targetMethods) {
			long saltSeed = longIterator.next();
			targetMethod.setSaltSeed(saltSeed);
		}

		Map<SaltEntry, Salt> saltByEntry = this.masterEntry.getEntryMap();
		HashSet<Long> usedKeys = GlobalUtils.newHashSet(GlobalUtils.getInitialCapacity(this.primaryEntries.size()));

		for (SaltEntry primaryEntry : this.primaryEntries) {
			usedKeys.add(primaryEntry.getValueKey());
		}

		IdentitySet<SaltEntry> processedEntries = new IdentitySet<>(this.entries.size());

		for (ChangedMethod changedMethod : changedMethods) {
			this.linkDependentMethods(changedMethod, primaryEntries, partneredEntries, chainPairs, valuePairs, usedKeys, processedEntries);
		}

		for (ChangedMethod linkedMethod : targetMethods) {
			if (linkedMethod.hasFalseDependents()) {
				this.linkFalseDependencies(linkedMethod, primaryEntries, partneredEntries, chainPairs);
			}
		}

		AtomicLong xorOut = new AtomicLong();

		for (SaltEntry entry : this.entries) {
			if (!processedEntries.contains(entry)) {
				long baseValue;
				if (entryToMethod.containsKey(entry)) {
					baseValue = entryToMethod.get(entry).getSaltSeed();
				} else {
					baseValue = this.randomLongs.next();
				}

				long derivedValue = deriveLinkValue(baseValue, entry.getCurrentValue(), entry.getBitPermutation(), xorOut, null, this.random);
				entry.setSaltValue(derivedValue);
				long encodedIndex = this.encodeIndex(entry.getIndex());
				valuePairs.add(new Pair(encodedIndex, derivedValue));
				processedEntries.add(entry);
			}
		}

		this.addExtraSaltOrders(primaryEntries, partneredEntries, assignedEntries, saltOrders);
		int maxIndex = Math.min(this.maxSaltCount, this.entries.size() - 1);
		if (!Options.skipSaltCrossLinks) {
			int pairMultiplier;
			if (chainPairs.size() < 1000) {
				pairMultiplier = 4;
			} else if (chainPairs.size() < 2000) {
				pairMultiplier = 2;
			} else {
				pairMultiplier = 1;
			}

			int extraPairCount = Math.max(this.minExtraPairs, Math.min(saltByEntry.size() / 2, chainPairs.size() * pairMultiplier));
			int pairIndex = 0;

			label81:
			do {
				int firstIndex = -1;
				SaltEntry firstEntry = null;
				int firstAttempt = 0;

				do {
					int firstCandidateIndex = this.random.nextInt(maxIndex + 1);
					SaltEntry firstCandidate = this.entries.get(firstCandidateIndex);
					if (!assignedEntries.contains(firstCandidate) && !firstCandidate.chainIntersects(assignedEntries)) {
						firstIndex = firstCandidateIndex;
						firstEntry = firstCandidate;
					}

					if (firstAttempt++ > maxIndex * 10) {
						break label81;
					}
				} while (firstIndex == -1);

				int secondIndex = -1;
				SaltEntry secondEntry = null;
				int secondAttempt = 0;

				do {
					int secondCandidateIndex = this.random.nextInt(maxIndex + 1);
					SaltEntry secondCandidate = this.entries.get(secondCandidateIndex);
					if (secondCandidate != firstEntry && !secondCandidate.hasPreviousSalt() && !secondCandidate.chainContains(firstEntry)) {
						secondIndex = secondCandidateIndex;
						secondEntry = secondCandidate;
					}

					if (secondAttempt++ > maxIndex * 10) {
						break label81;
					}
				} while (secondIndex == -1);

				firstEntry.appendToChain(secondEntry);
				long firstEncodedIndex = this.encodeIndex(firstIndex);
				long secondEncodedIndex = this.encodeIndex(secondIndex);
				chainPairs.add(new Pair(firstEncodedIndex, secondEncodedIndex));
			} while (++pairIndex < extraPairCount);
		}

		GlobalUtils.shuffle(chainPairs, this.random);
		GlobalUtils.shuffle(valuePairs, this.random);
	}

	public int getMaxSortDepth() {
		return maxSortDepth;
	}

	private void linkFalseDependencies(ChangedMethod changedMethod, Set<SaltEntry> primaryEntries, Set<SaltEntry> partneredEntries, List<Pair> chainPairs) {
		List<ChangedMethod> dependentMethods = changedMethod.getFalseDependents();
		int dependentCount = dependentMethods.size();
		if (dependentCount != 0) {
			if (dependentCount > 1) {
				dependentMethods = new ArrayList<>(dependentMethods);
				GlobalUtils.shuffle(dependentMethods, this.random);
			}

			SaltEntry previousEntry = changedMethod.getValueEntry();
			ArrayList<KeyValuePair<ChangedMethod, SaltEntry>> methodEntryPairs = new ArrayList<>(dependentCount + 1);
			methodEntryPairs.add(new KeyValuePair<>(changedMethod, previousEntry));

			for (int i = 0; i < dependentCount; i++) {
				ChangedMethod dependentMethod = dependentMethods.get(i);
				SaltEntry keyEntry = dependentMethod.getKeyEntry();
				previousEntry.appendToChain(keyEntry);
				long previousEncodedIndex = this.encodeIndex(previousEntry.getIndex());
				long currentEncodedIndex = this.encodeIndex(keyEntry.getIndex());
				chainPairs.add(new Pair(previousEncodedIndex, currentEncodedIndex));
				methodEntryPairs.add(new KeyValuePair<>(dependentMethod, keyEntry));
				previousEntry = keyEntry;
			}
		}
	}

	public int getIndexBitOffset() {
		return this.indexBitOffset;
	}

	private void initSizes(int changedMethodCount, int targetMethodCount) {
		AtomicInt valuesPerEntry = new AtomicInt();
		this.maxSaltCount = maxSaltCountFor(targetMethodCount, valuesPerEntry);
		int valuesPerEntryCount = valuesPerEntry.get();
		this.initialEntryCount = this.maxSaltCount / valuesPerEntryCount;
		this.initialCapacity = this.maxSaltCount / 2;

		for (int bitIndex = 8; bitIndex < 25; bitIndex++) {
			if (2 << bitIndex > this.maxSaltCount) {
				this.indexBitOffset = 63 - bitIndex;
				break;
			}
		}

		maxSortDepth = maxSortDepthFor(this.maxSaltCount);
		long mask = (1L << this.indexBitOffset) - 1L;
		randomLowBits = this.random.longs(0L, mask + 1L).iterator();
		this.randomLongs = this.random.longs(0L, Long.MAX_VALUE).iterator();
		this.minExtraPairs = this.initialEntryCount / 5;
	}

	private long createRandomSaltValue() {
		int[] bitPermutation = this.currentTable();
		int randomHash = this.random.nextInt(256);
		int randomByte = this.random.nextInt(256);

		long saltValue;
		do {
			saltValue = this.nextSaltValue();
		} while (this.usedValues.contains(saltValue));

		this.usedValues.add(saltValue);
		return ParameterChangerBuilder.packAndPermuteLongParts(randomByte, saltValue, randomHash, bitPermutation);
	}

	private long createEntrySaltValue(SaltEntry entry) {
		int[] bitPermutation = this.currentTable();
		long midBits = entry.getMidBits();
		int entryHash = entry.hashCode();
		int randomByte = this.random.nextInt(256);
		return ParameterChangerBuilder.packAndPermuteLongParts(randomByte, midBits, entryHash, bitPermutation);
	}

	public void assignTables(List<Object> tableAssignments) {
		sortEntries(this.entries);
		this.pendingEntries = new IdentitySet<>(this.entries.size());
		int entryIndex = 0;

		for (SaltEntry entry : this.entries) {
			entry.setIndex(entryIndex++);
			this.pendingEntries.add(entry);
		}

		HashSet<Long> usedKeys = GlobalUtils.newHashSet(GlobalUtils.getInitialCapacity(this.primaryEntries.size()));

		for (SaltEntry primaryEntry : this.primaryEntries) {
			usedKeys.add(primaryEntry.getValueKey());
		}

		ArrayList<int[]> permutations = new ArrayList<>(this.tables);
		ArrayList<int[]> unassignedPermutations = new ArrayList<>(permutations);
		int tableCount = permutations.size();
		ArrayList<KeyValuePair<int[], SaltEntry>> tableEntryPairs = new ArrayList<>(tableCount * 3);
		int passIndex = 0;

		do {
			int randomIndex;
			if (passIndex <= tableCount * 2) {
				randomIndex = this.random.nextInt(tableCount);
			} else {
				do {
					randomIndex = this.random.nextInt(tableCount);
				} while (!unassignedPermutations.contains(permutations.get(randomIndex)) || this.random.nextBoolean());
			}

			int[] bitPermutation = permutations.get(randomIndex);
			List<SaltEntry> tableEntries = this.entriesByTable.get(bitPermutation);
			SaltEntry selectedEntry = null;

			do {
				int randomEntryIndex = this.random.nextInt(tableEntries.size());
				SaltEntry candidateEntry = tableEntries.get(randomEntryIndex);
				if (candidateEntry.getIndex() < this.maxSaltCount) {
					selectedEntry = candidateEntry;
				}
			} while (selectedEntry == null);

			tableEntryPairs.add(new KeyValuePair<>(bitPermutation, selectedEntry));
			unassignedPermutations.remove(bitPermutation);
			passIndex++;
		} while (!unassignedPermutations.isEmpty());

		int pairCount = tableEntryPairs.size();

		for (int i = 0; i < pairCount; i++) {
			KeyValuePair<int[], SaltEntry> tableEntryPair = tableEntryPairs.get(i);
			this.assignTable(tableEntryPair.getKey(), tableEntryPair.getValue(), usedKeys, tableAssignments);
		}
	}

	private void buildInitialEntries(List<Object> values, int targetMethodCount, boolean java7OrNewer) {
		this.initialEntries = new ArrayList<>(this.initialEntryCount);
		this.entries = new ArrayList<>(this.maxSaltCount);
		int[] bitPermutation = ParameterChangerBuilder.createBitPermutation(this.random);
		this.pushTable(bitPermutation);
		values.add(this.currentTable().clone());
		int reuseDivisor = 64 * (targetMethodCount < 3010 ? 1 : 4);
		boolean valuesExtracted = false;
		int extractIndex = -1;
		if (Options.appendExtraSaltTable && java7OrNewer) {
			int extractLimit = Math.min(this.initialEntryCount * 17, 65035);
			extractIndex = Math.max(200 + this.random.nextInt(100), extractLimit / 34 + this.random.nextInt(100));
		}

		for (int entryOrdinal = 0; entryOrdinal < this.initialEntryCount; entryOrdinal++) {
			int randomByte = this.random.nextInt(256);
			Pair reusePair = null;
			int randomHash;
			long entryValue;
			if (this.initialEntries.size() > 1 && this.random.nextInt(reuseDivisor) == 0) {
				int randomEntryIndex = this.random.nextInt(this.initialEntries.size());
				SaltEntry reusedEntry = this.initialEntries.get(randomEntryIndex);
				reusePair = new Pair(reusedEntry);
				entryValue = reusedEntry.getMidBits();
				randomHash = reusedEntry.hashCode();
			} else {
				entryValue = this.nextSaltValue();
				randomHash = this.random.nextInt(256);
			}

			SaltEntry newEntry = new SaltEntry(randomByte, entryValue, randomHash, this.currentTable(), this.entriesByTable, this.tables);
			values.add(newEntry.getCurrentValue());
			this.initialEntries.add(newEntry);
			if (reusePair != null) {
				reusePair.setValue(newEntry);
				this.duplicatePairs.add(reusePair);
			}

			int tableEntryCount = this.entriesByTable.get(bitPermutation) != null ? this.entriesByTable.get(bitPermutation).size() : 0;
			if (tableEntryCount >= 64 && this.random.nextInt(128) == 0 || tableEntryCount > 153 && this.random.nextInt(32) == 0) {
				bitPermutation = (int[])bitPermutation.clone();
				this.pushTable(bitPermutation);
				values.add(this.currentTable().clone());
			}

			if (Options.appendExtraSaltTable && java7OrNewer && !valuesExtracted && entryOrdinal >= extractIndex) {
				valuesExtracted = true;
				long[] extractedValues = new long[100];
				int extractedCount = 0;
				Iterator<Object> valueIterator = values.iterator();

				while (valueIterator.hasNext()) {
					if (valueIterator.next() instanceof Long) {
						SaltEntry unusedEntry = this.initialEntries.get(extractedCount);
						extractedValues[extractedCount] = this.initialEntries.get(extractedCount).getValueBits(0, 6);
						extractedCount++;
					}

					if (extractedCount > 99) {
						break;
					}
				}

				values.add(extractedValues);
			}
		}

		this.entries.addAll(this.initialEntries);
		bitPermutation = ParameterChangerBuilder.createBitPermutation(this.random);
		this.pushTable(bitPermutation);
		values.add(this.currentTable().clone());
		sortEntries(this.entries);
		int entryIndex = 0;
		Iterator<SaltEntry> entryIterator = this.entries.iterator();

		while (entryIterator.hasNext()) {
			entryIterator.next().setIndex(entryIndex++);
		}
	}

	private long encodeIndex(int entryIndex) {
		int[] bitPermutation = this.currentTable();
		int bitCount = 64 - this.indexBitOffset;
		long lowBits = randomLowBits.next();
		return ParameterChangerBuilder.packAndPermuteBits(entryIndex, bitCount, lowBits, bitPermutation);
	}

	private void linkDependentMethods(ChangedMethod changedMethod, Set<SaltEntry> primaryEntries, Set<SaltEntry> partneredEntries, List<Pair> chainPairs, List<Pair> valuePairs, Set<Long> usedKeys, Set<SaltEntry> processedEntries) {
		List<ChangedMethod> dependentMethods = changedMethod.getDependentMethods();
		int dependentCount = dependentMethods.size();
		if (dependentCount != 0) {
			if (dependentCount > 1) {
				dependentMethods = new ArrayList<>(dependentMethods);
				GlobalUtils.shuffle(dependentMethods, this.random);
			}

			SaltEntry previousEntry = changedMethod.getValueEntry();
			ArrayList<KeyValuePair<ChangedMethod, SaltEntry>> methodEntryPairs = new ArrayList<>(dependentCount + 1);
			methodEntryPairs.add(new KeyValuePair<>(changedMethod, previousEntry));

			for (int i = 0; i < dependentCount; i++) {
				ChangedMethod dependentMethod = dependentMethods.get(i);
				SaltEntry keyEntry = dependentMethod.getKeyEntry();
				previousEntry.appendToChain(keyEntry);
				long previousEncodedIndex = this.encodeIndex(previousEntry.getIndex());
				long currentEncodedIndex = this.encodeIndex(keyEntry.getIndex());
				chainPairs.add(new Pair(previousEncodedIndex, currentEncodedIndex));
				methodEntryPairs.add(new KeyValuePair<>(dependentMethod, keyEntry));
				previousEntry = keyEntry;
			}

			KeyValuePair<ChangedMethod, SaltEntry> firstPair = methodEntryPairs.get(0);
			long baseSaltSeed = firstPair.getKey().getSaltSeed();
			SaltEntry firstEntry = firstPair.getValue();
			AtomicLong xorOut = new AtomicLong();
			long firstDerivedValue = deriveLinkValue(baseSaltSeed, firstEntry.getCurrentValue(), firstEntry.getBitPermutation(), xorOut, null, this.random);
			firstEntry.setSaltValue(firstDerivedValue);
			processedEntries.add(firstEntry);
			long firstEncodedIndex = this.encodeIndex(firstEntry.getIndex());
			valuePairs.add(new Pair(firstEncodedIndex, firstDerivedValue));
			int pairCount = methodEntryPairs.size();

			for (int pairIndex = 1; pairIndex < pairCount; pairIndex++) {
				KeyValuePair<ChangedMethod, SaltEntry> pair = methodEntryPairs.get(pairIndex);
				ChangedMethod pairedMethod = pair.getKey();
				SaltEntry pairedEntry = pair.getValue();
				long derivedValue = deriveLinkValue(baseSaltSeed, pairedEntry.getCurrentValue(), pairedEntry.getBitPermutation(), xorOut, usedKeys, this.random);
				pairedEntry.setSaltValue(derivedValue);
				processedEntries.add(pairedEntry);
				long encodedIndex = this.encodeIndex(pairedEntry.getIndex());
				pairedEntry.pushValue(xorOut.get());
				valuePairs.add(new Pair(encodedIndex, derivedValue));
				long entrySaltValue = this.createEntrySaltValue(pairedEntry);
				pairedMethod.setMethodValue(entrySaltValue);
				if (!pairedMethod.usesStaticSaltField()) {
					SaltEntry valueEntry = pairedMethod.getValueEntry();
					long saltSeed = pairedMethod.getSaltSeed();
					valueEntry.setSaltValue(saltSeed);
					processedEntries.add(valueEntry);
					long valueEncodedIndex = this.encodeIndex(valueEntry.getIndex());
					valuePairs.add(new Pair(valueEncodedIndex, saltSeed));
				}
			}

			for (ChangedMethod linkedMethod : dependentMethods) {
				this.linkDependentMethods(linkedMethod, primaryEntries, partneredEntries, chainPairs, valuePairs, usedKeys, processedEntries);
			}
		}
	}

	private void assignSaltPairFromValues(int[] table, List<Object> values, ChangedMethod changedMethod, long[] methodValues) {
		long valueEntryValue = methodValues[0];
		long keyEntryValue = methodValues[1];
		long randomSaltValue = methodValues[2];
		long saltSeed = methodValues[3];
		long keyEntrySaltValue = methodValues[4];
		SaltEntry keyEntry = new SaltEntry(keyEntryValue, table, this.entriesByTable, this.tables);
		keyEntry.setSaltValue(keyEntrySaltValue);
		int randomByte = this.random.nextInt(256);
		int randomHash = this.random.nextInt(256);
		SaltEntry valueEntry = new SaltEntry(randomByte, valueEntryValue, randomHash, table, this.entriesByTable, this.tables);
		changedMethod.setSaltEntries(keyEntry, valueEntry, keyEntryValue, randomSaltValue);
		changedMethod.setSaltSeed(saltSeed);
		values.add(keyEntry.getCurrentValue());
		values.add(valueEntry.getCurrentValue());
	}

	private boolean isTableValid(int[] table, List<SaltEntry> entries, Set<Long> usedKeys) {
		boolean valid = true;
		HashSet<Long> seenKeys = GlobalUtils.newHashSet(GlobalUtils.getInitialCapacity(entries.size()));

		for (SaltEntry entry : entries) {
			long midBits = entry.getMidBits();
			long currentValue = entry.getCurrentValue();
			long candidateMidBits = ParameterChangerBuilder.extractPermutedBitRange(currentValue, 8, 55, table);
			long valueKey = ParameterChangerBuilder.extractLowBits(currentValue, 56, table);
			boolean paired = this.pairedEntries.contains(entry);
			boolean primary = this.primaryEntries.contains(entry);
			if (primary) {
				valid = valid && seenKeys.add(valueKey);
			}

			if (!valid) {
				return false;
			}

			if (candidateMidBits == midBits) {
				return false;
			}

			if (paired && candidateMidBits == 0L) {
				return false;
			}

			if (paired && candidateMidBits > 140737488355327L) {
				return false;
			}

			if (primary && usedKeys.contains(valueKey)) {
				return false;
			}
		}

		return true;
	}

	private void initEntrySets(KeyValuePairMultiMap<String, SaltEntry, Salt> groupedEntries) {
		Map<SaltEntry, Salt> saltByEntry = this.masterEntry.getEntryMap();
		this.primaryEntries = new IdentitySet<>(GlobalUtils.getInitialCapacity(saltByEntry.size()));
		this.pairedEntries = new IdentitySet<>(GlobalUtils.getInitialCapacity(saltByEntry.size()));
		MultiMap<Salt, SaltEntry> entriesBySalt = new MultiMap<>(true, saltByEntry.size());

		for (Entry<SaltEntry, Salt> entry : saltByEntry.entrySet()) {
			Salt saltEntry = entry.getKey();
			Salt partnerSalt = entry.getValue();
			this.primaryEntries.add((SaltEntry)saltEntry);
			if (partnerSalt.isSaltEntry()) {
				this.pairedEntries.add((SaltEntry)partnerSalt);
			}

			entriesBySalt.put(partnerSalt, (SaltEntry)saltEntry);
		}

		IdentitySet<Salt> primaryOnlyEntries = new IdentitySet<>(this.primaryEntries.size());

		for (Salt primaryEntry : this.primaryEntries) {
			if (!this.pairedEntries.contains(primaryEntry)) {
				primaryOnlyEntries.add(primaryEntry);
			}
		}

		IdentitySet<Salt> pairedOnlyEntries = new IdentitySet<>(this.pairedEntries.size());

		for (Salt pairedEntry : this.pairedEntries) {
			if (!this.primaryEntries.contains(pairedEntry)) {
				pairedOnlyEntries.add(pairedEntry);
			}
		}

		IdentitySet<SaltEntry> duplicateEntries = new IdentitySet<>(this.duplicatePairs.size() * 2);

		for (Pair duplicatePair : this.duplicatePairs) {
			duplicateEntries.add((SaltEntry)duplicatePair.getKey());
			duplicateEntries.add((SaltEntry)duplicatePair.getValue());
		}

		IdentitySet<SaltEntry> conflictingEntries = new IdentitySet<>();
		IdentitySet<SaltEntry> duplicateGroupEntries = new IdentitySet<>();

		for (Entry<String, List<KeyValuePair<SaltEntry, Salt>>> groupedEntry : groupedEntries.entrySet()) {
			String unusedKey = groupedEntry.getKey();
			List<KeyValuePair<SaltEntry, Salt>> groupPairs = groupedEntry.getValue();
			int groupSize = groupPairs.size();
			if (groupSize > 1) {
				duplicateGroupEntries.clear();
				SaltEntry firstEntry = null;

				for (int i = 0; i < groupSize; i++) {
					SaltEntry pairEntry = groupPairs.get(i).getKey();
					if (i == 0) {
						firstEntry = pairEntry;
					}

					if (i > 0 && pairEntry != firstEntry) {
						conflictingEntries.add(pairEntry);
					}

					if (duplicateEntries.contains(pairEntry)) {
						duplicateGroupEntries.add(pairEntry);
					}
				}

				if (duplicateGroupEntries.size() > 1) {
				}
			}
		}

		int tableIndex = 0;
		StringBuilder debugText = new StringBuilder();

		for (int[] debugPermutation : this.tables) {
			List<SaltEntry> debugEntries = this.entriesByTable.get(debugPermutation);
			debugText.append("#" + tableIndex++ + " (" + System.identityHashCode(debugPermutation) + ")\t" + debugEntries.size());
			debugText.append(DebugUtils.LINE_SEPARATOR);
		}

		this.primaryOnlyTables = new IdentitySet<>(this.entriesByTable.size());
		this.pairedOnlyTables = new IdentitySet<>(this.entriesByTable.size());

		for (int[] bitPermutation : this.tables) {
			List<SaltEntry> tableEntries = this.entriesByTable.get(bitPermutation);
			boolean primaryFound = false;
			boolean pairedFound = false;

			for (SaltEntry tableEntry : tableEntries) {
				if (this.primaryEntries.contains(tableEntry)) {
					primaryFound = true;
				}

				if (this.pairedEntries.contains(tableEntry)) {
					pairedFound = true;
				}

				if (primaryFound && pairedFound) {
					break;
				}
			}

			if (primaryFound && !pairedFound) {
				this.primaryOnlyTables.add(bitPermutation);
			} else if (!primaryFound && pairedFound) {
				this.pairedOnlyTables.add(bitPermutation);
			}
		}
	}

	public int getTableBatchSize() {
		return 128;
	}

	private void pushTable(int[] table) {
		this.permutationTables.add(table);
	}

	private void addExtraSaltOrders(Set<SaltEntry> primaryEntries, Set<SaltEntry> partneredEntries, Set<SaltEntry> assignedEntries, List<SaltOrder> saltOrders) {
		Map<SaltEntry, Salt> saltByEntry = this.masterEntry.getEntryMap();
		int addedCount = 0;

		for (SaltEntry saltEntry : saltByEntry.keySet()) {
			if (!primaryEntries.contains(saltEntry) && !assignedEntries.contains(saltEntry) && !this.pairedEntries.contains(saltEntry)) {
				Salt partnerSalt = saltByEntry.get(saltEntry);
				if (partnerSalt.isSaltEntry() && !partneredEntries.contains(partnerSalt) && !assignedEntries.contains(partnerSalt) && !this.primaryEntries.contains(partnerSalt)) {
					SaltEntry partnerEntry = (SaltEntry)partnerSalt;
					long entrySaltValue = this.createEntrySaltValue(saltEntry);
					long firstRandomSaltValue = this.createRandomSaltValue();
					long secondRandomSaltValue = this.createRandomSaltValue();
					saltOrders.add(new SaltOrder(saltEntry, partnerEntry, entrySaltValue, firstRandomSaltValue, secondRandomSaltValue));
					primaryEntries.add(saltEntry);
					partneredEntries.add(partnerEntry);
					assignedEntries.add(saltEntry);
					assignedEntries.add(partnerEntry);
					addedCount++;
				}
			}

			if (addedCount > 1) {
				break;
			}
		}
	}

	public long nextSaltValue() {
		return this.saltValueSource.next();
	}

	public Salt getOrCreateEntry(long saltValue, AtomicBool limitReached) {
		int[] bitPermutation = this.currentTable();
		int entryIndex = (int)ParameterChangerBuilder.extractPermutedBitRange(saltValue, this.indexBitOffset, 63, bitPermutation);
		if (entryIndex < this.initialEntries.size()) {
			return this.entries.get(entryIndex);
		} else {
			if (this.entries.size() % 128 == 0) {
				bitPermutation = (int[])bitPermutation.clone();
				this.pushTable(bitPermutation);
			}

			SaltEntry newEntry = new SaltEntry(saltValue, this.currentTable(), this.entriesByTable, this.tables);
			newEntry.setIndex(this.entries.size());
			this.entries.add(newEntry);
			if (this.entries.size() >= this.maxSaltCount - 1) {
				limitReached.set(true);
			}

			return newEntry;
		}
	}

	private void assignTable(int[] table, SaltEntry entry, Set<Long> usedKeys, List<Object> values) {
		List<SaltEntry> tableEntries = this.entriesByTable.get(table);
		int[] bitPermutation = null;
		int attemptCount = -1;
		if (this.pairedOnlyTables.contains(table)) {
			while (++attemptCount <= 8192) {
				bitPermutation = ParameterChangerBuilder.createBitPermutation(this.random);
				if (this.isTableValid(bitPermutation, tableEntries, usedKeys)) {
					break;
				}
			}
		} else {
			while (++attemptCount <= 8192) {
				bitPermutation = (int[])table.clone();
				ParameterChangerBuilder.shuffleBitPermutation(bitPermutation, this.random);
				if (this.isTableValid(bitPermutation, tableEntries, usedKeys)) {
					break;
				}
			}
		}

		if (attemptCount <= 8192) {
			int entryIndex = entry.getIndex();
			long encodedIndex = this.encodeIndex(entryIndex);
			values.add(encodedIndex);
			values.add(bitPermutation.clone());
			HashSet<Long> removedKeys = GlobalUtils.newHashSet(GlobalUtils.getInitialCapacity(tableEntries.size()));

			for (SaltEntry primaryEntry : tableEntries) {
				if (this.primaryEntries.contains(primaryEntry)) {
					removedKeys.add(primaryEntry.getValueKey());
				}
			}

			entry.setBitPermutation(bitPermutation);
			HashSet<Long> addedKeys = GlobalUtils.newHashSet(GlobalUtils.getInitialCapacity(tableEntries.size()));

			for (SaltEntry removedEntry : tableEntries) {
				this.pendingEntries.remove(removedEntry);
				if (this.primaryEntries.contains(removedEntry)) {
					addedKeys.add(removedEntry.getValueKey());
				}
			}

			for (Long removedKey : removedKeys) {
				usedKeys.remove(removedKey);
			}

			for (Long addedKey : addedKeys) {
				usedKeys.add(addedKey);
			}
		}
	}

	public int[] currentTable() {
		return this.permutationTables.get(this.permutationTables.size() - 1);
	}

	public static class SaltSorter implements Comparator<SaltEntry> {
		public final SaltManager saltManager;

		public SaltSorter(SaltManager saltManager) {
			this.saltManager = saltManager;
		}

		public int compareByIndex(SaltEntry entry1, SaltEntry entry2) {
			return entry1.getIndex() - entry2.getIndex();
		}

		@Override
		public int compare(SaltEntry o1, SaltEntry o2) {
			return this.compareByIndex(o1, o2);
		}
	}

	public static class ChangedMethodComparator implements Comparator<ChangedMethod> {
		public final SaltManager saltManager;

		public ChangedMethodComparator(SaltManager saltManager) {
			this.saltManager = saltManager;
		}

		public int compareByMethodSignature(ChangedMethod changedMethod1, ChangedMethod changedMethod2) {
			StringBuilder signatureBuilder1 = new StringBuilder();
			MethodNode methodNode1 = changedMethod1.getMethodNode();
			signatureBuilder1.append(methodNode1.getInternalName());
			signatureBuilder1.append(' ');
			signatureBuilder1.append(methodNode1.getDisplayNameWithDescriptor());
			StringBuilder signatureBuilder2 = new StringBuilder();
			MethodNode methodNode2 = changedMethod2.getMethodNode();
			signatureBuilder2.append(methodNode2.getInternalName());
			signatureBuilder2.append(' ');
			signatureBuilder2.append(methodNode2.getDisplayNameWithDescriptor());
			return signatureBuilder1.toString().compareTo(signatureBuilder2.toString());
		}

		@Override
		public int compare(ChangedMethod o1, ChangedMethod o2) {
			return this.compareByMethodSignature(o1, o2);
		}
	}
}
