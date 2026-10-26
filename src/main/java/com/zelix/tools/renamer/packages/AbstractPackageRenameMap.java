package com.zelix.tools.renamer.packages;

import com.zelix.utils.collections.readOnly.ReadOnlyMap;
import java.util.Map;

public abstract class AbstractPackageRenameMap {
	public Map<String, String> renameMap;

	public ReadOnlyMap<String, String> getReadOnlyMap() {
		return this.renameMap == null ? new ReadOnlyMap<>() : new ReadOnlyMap<>(this.renameMap);
	}

	public abstract int updateMap(Map<String, String> newMappings);

	public void clear() {
		if (this.renameMap != null) {
			this.renameMap.clear();
		}
	}

	public int getSize() {
		return this.renameMap != null ? this.renameMap.size() : 0;
	}
}
