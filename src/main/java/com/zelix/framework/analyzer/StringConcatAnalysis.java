package com.zelix.framework.analyzer;

import com.zelix.framework.attributes.BootstrapMethodEntry;
import com.zelix.framework.attributes.BootstrapMethodsAttribute;
import com.zelix.framework.constant.StringNode;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.dataset.AtomicInt;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Map;

public class StringConcatAnalysis {
	public static final String RECIPE_ARG_TAG = String.valueOf('\u0001');
	public static final String RECIPE_CONST_TAG = String.valueOf('\u0002');
	private Map<BootstrapMethodEntry, AnalyzedStringConcat> analysisMap = GlobalUtils.newHashMap();
	private final ClassNode classNode;
	private final BootstrapMethodsAttribute bootstrapMethodsAttribute;

	public StringConcatAnalysis(ClassNode classNode, BootstrapMethodsAttribute bootstrapMethodsAttribute) {
		this.classNode = classNode;
		this.bootstrapMethodsAttribute = bootstrapMethodsAttribute;
	}

	public static List<String> parseRecipe(String recipe, AtomicInt argCount) {
		ArrayList<String> parts = new ArrayList<>();
		int tagCount = 0;
		int constantPartCount = 0;
		int scanPosition = 0;

		int tagIndex;
		do {
			boolean argTagFound = false;
			int nextArgTagIndex = recipe.indexOf(1, scanPosition);
			int nextConstTagIndex = recipe.indexOf(2, scanPosition);
			if (nextArgTagIndex > -1 && nextConstTagIndex == -1) {
				tagIndex = nextArgTagIndex;
				argTagFound = true;
			} else if (nextArgTagIndex == -1 && nextConstTagIndex > -1) {
				tagIndex = nextConstTagIndex;
				constantPartCount++;
			} else if (nextArgTagIndex <= -1 || nextArgTagIndex <= -1) {
				tagIndex = -1;
			} else if (nextArgTagIndex < nextConstTagIndex) {
				argTagFound = true;
				tagIndex = nextArgTagIndex;
			} else {
				tagIndex = nextConstTagIndex;
				constantPartCount++;
			}

			if (tagIndex > -1) {
				tagCount++;
				if (tagIndex > scanPosition) {
					parts.add(recipe.substring(scanPosition, tagIndex));
					constantPartCount++;
				}

				parts.add(argTagFound ? RECIPE_ARG_TAG : RECIPE_CONST_TAG);
				scanPosition = tagIndex + 1;
			}
		} while (tagIndex > -1);

		if (scanPosition < recipe.length()) {
			parts.add(recipe.substring(scanPosition));
			constantPartCount++;
		}

		if (constantPartCount > 0) {
			argCount.set(tagCount);
			return parts;
		} else {
			return null;
		}
	}

	public boolean isEmpty() {
		return this.analysisMap.isEmpty();
	}

	public void applyTransformations() {
		boolean argumentsTruncated = false;
		Iterator<Entry<BootstrapMethodEntry, AnalyzedStringConcat>> entryIterator = this.analysisMap.entrySet().iterator();

		while (entryIterator.hasNext()) {
			AnalyzedStringConcat analyzedConcat = entryIterator.next().getValue();
			StringNode recipeNode = analyzedConcat.getOriginalRecipeNode();
			recipeNode.setValueFromString(analyzedConcat.getNewRecipeValue());
			if (analyzedConcat.getStaticConstantArguments().length > 0) {
				argumentsTruncated = true;
				analyzedConcat.getBootstrapMethodEntry().truncateArguments(0);
			}
		}

		if (argumentsTruncated) {
			this.bootstrapMethodsAttribute.getAttributeLength();
		}
	}

	public boolean isEntryAnalyzed(BootstrapMethodEntry entry) {
		return this.analysisMap.containsKey(entry);
	}

	public void prepareTransformations() {
		Iterator<Entry<BootstrapMethodEntry, AnalyzedStringConcat>> entryIterator = this.analysisMap.entrySet().iterator();

		while (entryIterator.hasNext()) {
			AnalyzedStringConcat analyzedConcat = entryIterator.next().getValue();
			StringBuilder recipeBuilder = new StringBuilder();
			List<String> recipeParts = analyzedConcat.getOriginalRecipeParts();
			TypeWriter[] staticArguments = analyzedConcat.getStaticConstantArguments();
			int staticArgIndex = 0;

			for (int partIndex = 0; partIndex < recipeParts.size(); partIndex++) {
				String recipePart = recipeParts.get(partIndex);
				recipeBuilder.append(RECIPE_ARG_TAG);
				if (!recipePart.equals(RECIPE_ARG_TAG)) {
					if (recipePart.equals(RECIPE_CONST_TAG)) {
						StringNode constantNode = (StringNode)staticArguments[staticArgIndex];
						StringConcatPart constantPart = new StringConcatPart(partIndex, constantNode.getValueAsString(), constantNode);
						analyzedConcat.addParsedPart(constantPart);
						staticArgIndex++;
					} else {
						StringConcatPart literalPart = new StringConcatPart(partIndex, recipePart);
						analyzedConcat.addParsedPart(literalPart);
					}
				}
			}

			analyzedConcat.setNewRecipeValue(recipeBuilder.toString());
		}
	}

	public AnalyzedStringConcat getAnalyzedEntry(BootstrapMethodEntry entry) {
		return this.analysisMap.get(entry);
	}

	public void addAnalysisResult(BootstrapMethodEntry entry, List<String> recipeParts, int argCount) {
		this.analysisMap.put(entry, new AnalyzedStringConcat(entry, recipeParts, argCount));
	}
}
