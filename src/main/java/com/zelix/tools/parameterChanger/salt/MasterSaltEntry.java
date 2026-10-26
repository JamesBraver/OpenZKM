package com.zelix.tools.parameterChanger.salt;

import com.zelix.tools.parameterChanger.ParameterChangerBuilder;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.collections.KeyValuePairMultiMap;
import com.zelix.utils.dataset.AtomicBool;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class MasterSaltEntry implements Salt {
	private static MasterSaltEntry INSTANCE;
	private final Random random;
	private final Map<SaltEntry, Salt> entryMap;
	private final SaltManager saltManager;
	private KeyValuePairMultiMap<String, SaltEntry, Salt> entryPairsByKey;
	private Salt nextSalt;

	private MasterSaltEntry(Random random, int mapCapacity, List<Object> masterMapData, SaltManager saltManager) {
		this.random = random;
		this.entryMap = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(mapCapacity));
		this.saltManager = saltManager;
		INSTANCE = this;
		boolean masterSaltAdded = false;
		this.entryPairsByKey = new KeyValuePairMultiMap<>(mapCapacity);
		AtomicBool entryLimitReached = new AtomicBool(false);

		for (int pairCount = 0; !entryLimitReached.get(); pairCount++) {
			long entrySaltValue = this.createSaltValue();
			SaltEntry saltEntry = (SaltEntry)saltManager.getOrCreateEntry(entrySaltValue, entryLimitReached);
			masterMapData.add(entrySaltValue);
			Salt pairedSalt;
			if (!masterSaltAdded && random.nextInt(mapCapacity / 2) == 0) {
				pairedSalt = this;
				masterSaltAdded = true;
				masterMapData.add(this);
			} else {
				long pairedSaltValue = this.createSaltValue();
				pairedSalt = saltManager.getOrCreateEntry(pairedSaltValue, entryLimitReached);
				masterMapData.add(pairedSaltValue);
			}

			this.entryPairsByKey.put(saltEntry.getKey(), saltEntry, pairedSalt);
			Salt replacedSalt = this.entryMap.put(saltEntry, pairedSalt);
		}
	}

	public static MasterSaltEntry of(Random random, int mapCapacity, List<Object> masterMapData, SaltManager saltManager) {
		INSTANCE = null;
		return new MasterSaltEntry(random, mapCapacity, masterMapData, saltManager);
	}

	@Override
	public void appendToChain(Salt salt) {
		if (this != salt) {
			if (this.nextSalt == null) {
				this.nextSalt = salt;
			} else {
				this.nextSalt.appendToChain(salt);
			}
		}
	}

	@Override
	public long advanceChain(long mixValue) {
		return mixValue;
	}

	@Override
	public long getCurrentValue() {
		return 0L;
	}

	@Override
	public boolean isLessThanOrEqual(Salt other) {
		if (this == other) {
			return true;
		} else {
			return other instanceof MasterSaltEntry ? System.identityHashCode(this) - System.identityHashCode(other) <= 0 : false;
		}
	}

	@Override
	public long getMidBits() {
		return 0L;
	}

	public Map<SaltEntry, Salt> getEntryMap() {
		return Collections.unmodifiableMap(this.entryMap);
	}

	public KeyValuePairMultiMap<String, SaltEntry, Salt> getEntryPairsByKey() {
		return this.entryPairsByKey;
	}

	@Override
	public boolean isSaltEntry() {
		return false;
	}

	private long createSaltValue() {
		int highByte = this.random.nextInt(256);
		long payload = this.saltManager.nextSaltValue();
		int lowByte = this.random.nextInt(256);
		return ParameterChangerBuilder.packAndPermuteLongParts(highByte, payload, lowByte, this.saltManager.currentTable());
	}
}
