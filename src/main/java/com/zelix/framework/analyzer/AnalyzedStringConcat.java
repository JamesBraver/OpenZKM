package com.zelix.framework.analyzer;

import com.zelix.framework.attributes.BootstrapMethodEntry;
import com.zelix.framework.constant.StringNode;
import com.zelix.framework.writer.base.TypeWriter;
import java.util.ArrayList;
import java.util.List;

public class AnalyzedStringConcat {
	private final List<StringConcatPart> parsedParts = new ArrayList<>();
	private final BootstrapMethodEntry bootstrapMethodEntry;
	private final List<String> originalRecipeParts;
	private final int originalArgCount;
	private final StringNode originalRecipeNode;
	private final String originalRecipeValue;
	private final TypeWriter[] staticConstantArguments;
	private String newRecipeValue;

	public AnalyzedStringConcat(BootstrapMethodEntry bootstrapMethodEntry, List<String> originalRecipeParts, int originalArgCount) {
		this.bootstrapMethodEntry = bootstrapMethodEntry;
		this.originalRecipeParts = originalRecipeParts;
		this.originalArgCount = originalArgCount;
		TypeWriter[] bootstrapArguments = this.bootstrapMethodEntry.getBootstrapArguments();
		this.originalRecipeNode = (StringNode)bootstrapArguments[0];
		this.originalRecipeValue = this.originalRecipeNode.getValueAsString();
		this.staticConstantArguments = new TypeWriter[bootstrapArguments.length - 1];
		System.arraycopy(bootstrapArguments, 1, this.staticConstantArguments, 0, this.staticConstantArguments.length);
	}

	public void addParsedPart(StringConcatPart part) {
		this.parsedParts.add(part);
	}

	public BootstrapMethodEntry getBootstrapMethodEntry() {
		return this.bootstrapMethodEntry;
	}

	public void setNewRecipeValue(String newRecipeValue) {
		this.newRecipeValue = newRecipeValue;
	}

	public List<StringConcatPart> getParsedParts() {
		return this.parsedParts;
	}

	public List<String> getOriginalRecipeParts() {
		return this.originalRecipeParts;
	}

	public StringNode getOriginalRecipeNode() {
		return this.originalRecipeNode;
	}

	public TypeWriter[] getStaticConstantArguments() {
		return this.staticConstantArguments;
	}

	public String getNewRecipeValue() {
		return this.newRecipeValue;
	}
}
