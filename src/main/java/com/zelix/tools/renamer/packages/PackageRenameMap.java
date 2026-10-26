package com.zelix.tools.renamer.packages;

import com.zelix.utils.GlobalUtils;
import com.zelix.utils.collections.DualKeyMap;
import java.util.Map.Entry;
import java.util.Map;

public class PackageRenameMap extends AbstractPackageRenameMap {
	@Override
	public int updateMap(Map<String, String> newMappings) {
		if (super.renameMap == null) {
			super.renameMap = GlobalUtils.newHashMap(newMappings);
		} else if (newMappings != null && newMappings.size() > 0) {
			DualKeyMap<String, String, String> valueToKeyReverseMap = new DualKeyMap<>(super.renameMap.size());

			for (Entry<String, String> currentEntry : super.renameMap.entrySet()) {
				String originalKey = currentEntry.getKey();
				String currentValue = currentEntry.getValue();
				valueToKeyReverseMap.put(currentValue, originalKey, originalKey);
			}

			for (Entry<String, String> newMappingEntry : newMappings.entrySet()) {
				String remapKey = newMappingEntry.getKey();
				String newValue = newMappingEntry.getValue();
				Map<String, String> matchingOriginalKeys = valueToKeyReverseMap.getGroup(remapKey);
				if (matchingOriginalKeys != null) {
					for (String originalKeyToUpdate : matchingOriginalKeys.keySet()) {
						super.renameMap.put(originalKeyToUpdate, newValue);
					}
				} else {
					super.renameMap.put(remapKey, newValue);
				}
			}
		}

		return this.getSize();
	}
}
