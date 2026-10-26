package com.zelix.tools.parameterChanger.extra;

import com.zelix.klassmaster.config.Options;
import com.zelix.tools.parameterChanger.ParameterPattern;
import com.zelix.utils.dataset.AtomicInt;
import java.util.List;

public class ExtraParameterPatternBuilder {
	public static final char[][] PATTERNS = new char[][]{
		{'J'},
		{'I', 'I'},
		{'B', 'J'},
		{'J', 'B'},
		{'S', 'J'},
		{'J', 'S'},
		{'C', 'J'},
		{'J', 'C'},
		{'I', 'J'},
		{'J', 'I'},
		{'I', 'I', 'B'},
		{'I', 'B', 'I'},
		{'B', 'I', 'I'},
		{'S', 'S', 'I'},
		{'S', 'I', 'S'},
		{'S', 'I', 'I'},
		{'S', 'I', 'C'},
		{'S', 'C', 'I'},
		{'I', 'S', 'S'},
		{'I', 'S', 'I'},
		{'I', 'S', 'C'},
		{'I', 'I', 'S'},
		{'I', 'I', 'I'},
		{'I', 'I', 'C'},
		{'I', 'C', 'S'},
		{'I', 'C', 'I'},
		{'I', 'C', 'C'},
		{'C', 'S', 'I'},
		{'C', 'I', 'S'},
		{'C', 'I', 'I'},
		{'C', 'I', 'C'},
		{'C', 'C', 'I'}
	};

	private static ParameterPattern nextPatternFromFront(String name, List<String> parameterDescriptors, String returnDescriptor, char[] extraTypes, ParameterPattern currentPattern, boolean isVarArgs, boolean isNewPattern) {
		int parameterCount = parameterDescriptors.size();
		int extraTypeCount = extraTypes.length;
		int[] extraParameterIndexes = new int[extraTypeCount];
		if (!isNewPattern) {
			extraParameterIndexes = currentPattern.getExtraParameterIndexes();
			int totalParameterCount = parameterCount + extraTypeCount;

			for (int slotIndex = extraTypeCount - 1; slotIndex >= 0; slotIndex--) {
				if (extraParameterIndexes[slotIndex] < totalParameterCount - extraTypeCount + slotIndex - (isVarArgs ? 1 : 0)) {
					extraParameterIndexes[slotIndex]++;

					for (int higherSlotIndex = slotIndex + 1; higherSlotIndex < extraTypeCount; higherSlotIndex++) {
						extraParameterIndexes[higherSlotIndex] = extraParameterIndexes[higherSlotIndex - 1] + 1;
					}
					break;
				}
			}
		} else {
			int resetSlotIndex = 0;

			while (resetSlotIndex < extraTypeCount) {
				extraParameterIndexes[resetSlotIndex] = resetSlotIndex++;
			}
		}

		return new ParameterPattern(parameterDescriptors, returnDescriptor, extraTypes, extraParameterIndexes);
	}

	private static boolean isAtBackLimit(char[] extraTypes, List<String> parameterDescriptors, ParameterPattern currentPattern, boolean isVarArgs) {
		return currentPattern != null ? currentPattern.getFirstExtraParameter().getParameterIndex() >= parameterDescriptors.size() - (isVarArgs ? 1 : 0) : false;
	}

	private static ParameterPattern nextPatternFromBack(String name, List<String> parameterDescriptors, String returnDescriptor, char[] extraTypes, ParameterPattern currentPattern, boolean isVarArgs, boolean isNewPattern) {
		int parameterCount = parameterDescriptors.size();
		int extraTypeCount = extraTypes.length;
		int[] extraParameterIndexes = new int[extraTypeCount];
		if (!isNewPattern) {
			extraParameterIndexes = currentPattern.getExtraParameterIndexes();
			int discardedTotalParameterCount = parameterCount + extraTypeCount;

			for (int slotIndex = 0; slotIndex < extraTypeCount; slotIndex++) {
				if (extraParameterIndexes[slotIndex] != slotIndex) {
					extraParameterIndexes[slotIndex]--;

					for (int lowerSlotIndex = slotIndex - 1; lowerSlotIndex >= 0; lowerSlotIndex--) {
						extraParameterIndexes[lowerSlotIndex] = extraParameterIndexes[lowerSlotIndex + 1] - 1;
					}
					break;
				}
			}
		} else {
			for (int resetSlotIndex = 0; resetSlotIndex < extraTypeCount; resetSlotIndex++) {
				extraParameterIndexes[resetSlotIndex] = parameterCount + resetSlotIndex - (isVarArgs ? 1 : 0);
			}
		}

		return new ParameterPattern(parameterDescriptors, returnDescriptor, extraTypes, extraParameterIndexes);
	}

	private static boolean isAtFrontLimit(char[] extraTypes, List<String> parameterDescriptors, ParameterPattern currentPattern) {
		if (currentPattern != null) {
			int frontLimitIndex = currentPattern.getExtraParameterCount() - 1;
			return currentPattern.getLastExtraParameter().getParameterIndex() == frontLimitIndex;
		} else {
			return false;
		}
	}

	public ParameterPattern nextPatternAcrossTemplates(
		String name, boolean hasLambdaSamParamCount, List<String> parameterDescriptors, String returnDescriptor, AtomicInt templateIndex, ParameterPattern currentPattern, boolean isVarArgs, char[][] templates
	) {
		char[] extraTypes = templates[templateIndex.get()];
		boolean patternIsNew = currentPattern == null;
		boolean limitReached;
		if (!Options.ascendingExtraParamOrder && !hasLambdaSamParamCount) {
			limitReached = isAtFrontLimit(extraTypes, parameterDescriptors, currentPattern);
		} else {
			limitReached = isAtBackLimit(extraTypes, parameterDescriptors, currentPattern, isVarArgs);
		}

		if (limitReached) {
			templateIndex.increaseAndGet();
			if (templateIndex.get() >= templates.length) {
				return null;
			}

			extraTypes = templates[templateIndex.get()];
			patternIsNew = true;
		}

		ParameterPattern nextPattern;
		if (!Options.ascendingExtraParamOrder && !hasLambdaSamParamCount) {
			nextPattern = nextPatternFromBack(name, parameterDescriptors, returnDescriptor, extraTypes, currentPattern, isVarArgs, patternIsNew);
		} else {
			nextPattern = nextPatternFromFront(name, parameterDescriptors, returnDescriptor, extraTypes, currentPattern, isVarArgs, patternIsNew);
		}

		return nextPattern;
	}

	public ParameterPattern nextPatternInTemplate(String name, boolean hasLambdaSamParamCount, List<String> parameterDescriptors, String returnDescriptor, char[] extraTypes, ParameterPattern currentPattern, boolean isVarArgs) {
		boolean patternIsNew = currentPattern == null;
		boolean limitReached;
		if (!Options.ascendingExtraParamOrder && !hasLambdaSamParamCount) {
			limitReached = isAtFrontLimit(extraTypes, parameterDescriptors, currentPattern);
		} else {
			limitReached = isAtBackLimit(extraTypes, parameterDescriptors, currentPattern, isVarArgs);
		}

		if (limitReached) {
			return null;
		} else {
			ParameterPattern nextPattern;
			if (!Options.ascendingExtraParamOrder && !hasLambdaSamParamCount) {
				nextPattern = nextPatternFromBack(name, parameterDescriptors, returnDescriptor, extraTypes, currentPattern, isVarArgs, patternIsNew);
			} else {
				nextPattern = nextPatternFromFront(name, parameterDescriptors, returnDescriptor, extraTypes, currentPattern, isVarArgs, patternIsNew);
			}

			return nextPattern;
		}
	}
}
