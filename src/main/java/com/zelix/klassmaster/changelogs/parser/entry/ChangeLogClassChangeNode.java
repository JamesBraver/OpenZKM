package com.zelix.klassmaster.changelogs.parser.entry;

import com.zelix.klassmaster.changelogs.AbstractChangeLogContext;
import com.zelix.klassmaster.changelogs.FieldKey;
import com.zelix.klassmaster.changelogs.MethodKey;
import com.zelix.klassmaster.changelogs.MethodMappingInfo;
import com.zelix.klassmaster.changelogs.parser.base.ChangeLogEntryNodeBase;
import com.zelix.klassmaster.changelogs.parser.base.ChangeLogsNode;
import com.zelix.klassmaster.changelogs.parser.javaccInterface.ClassNameReceiver;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.collections.MultiMap;
import java.util.Map;

public class ChangeLogClassChangeNode extends ChangeLogEntryNodeBase implements ClassNameReceiver {
	private MultiMap<MethodKey, MethodMappingInfo> methodMappings;
	private String newClassName;
	private String sourceName;
	private MultiMap<FieldKey, String> fieldMappings;
	private String oldClassName;
	private Map<Integer, Integer> lineNumberMappings;
	private int childCount;

	public ChangeLogClassChangeNode(int nodeKindIndex) {
		super(nodeKindIndex);
	}

	public void addFieldMapping(String newFieldName, String oldFieldName, String fieldType, int accessFlags) {
		if (this.fieldMappings == null) {
			int initialCapacity = GlobalUtils.getInitialCapacity(this.childCount);
			this.fieldMappings = new MultiMap<>(initialCapacity);
		}

		MultiMap<FieldKey, String> fieldMappings = this.fieldMappings;
		FieldKey fieldKey = new FieldKey(oldFieldName, fieldType, accessFlags);
		fieldMappings.put(fieldKey, newFieldName);
	}

	public void setSourceName(String sourceName) {
		this.sourceName = sourceName;
	}

	public void addMethodMapping(
		String oldMethodName,
		String oldMethodSignature,
		String methodReturnType,
		int accessFlags,
		String newMethodName,
		String newMethodSignature,
		boolean parameterObfuscated,
		String parameterChangeData,
		String parameterChangeLookupData,
		boolean manufactured
	) {
		if (this.methodMappings == null) {
			this.methodMappings = new MultiMap<>(Math.min(40, this.childCount));
		}

		if (oldMethodSignature == null) {
			oldMethodSignature = "";
		}

		if (newMethodSignature == null) {
			newMethodSignature = "";
		}

		boolean parameterChangeDeclared = parameterChangeData != null && parameterChangeData.length() > 0 || parameterObfuscated;
		MultiMap<MethodKey, MethodMappingInfo> methodMappings = this.methodMappings;
		MethodKey methodKey = new MethodKey(oldMethodName, oldMethodSignature, methodReturnType, accessFlags);
		MethodMappingInfo methodMappingInfo = new MethodMappingInfo(
			newMethodName, newMethodSignature, parameterObfuscated, parameterChangeData, parameterChangeLookupData, parameterChangeDeclared, manufactured
		);
		methodMappings.put(methodKey, methodMappingInfo);
	}

	@Override
	public void setClassName(String className) {
		this.oldClassName = className;
	}

	@Override
	protected void applyToContext(AbstractChangeLogContext context) {
		if (this.newClassName == null) {
			this.newClassName = this.oldClassName;
		}

		context.addClassMapping(this.newClassName, this.oldClassName);
		if (this.sourceName != null) {
			context.setClassSourceName(this.oldClassName, this.sourceName);
		}

		if (this.fieldMappings != null) {
			context.addFieldMappings(this.oldClassName, this.fieldMappings);
		}

		if (this.methodMappings != null) {
			context.addMethodMappings(this.oldClassName, this.methodMappings);
		}

		if (this.lineNumberMappings != null) {
			context.addInnerClassMappings(this.oldClassName, this.lineNumberMappings);
		}
	}

	public void addLineNumberMapping(Integer newLineNumber, Integer oldLineNumber) {
		if (this.lineNumberMappings == null) {
			this.lineNumberMappings = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(this.childCount));
		}

		this.lineNumberMappings.put(newLineNumber, oldLineNumber);
	}

	public void setNewClassName(String newClassName) {
		this.newClassName = newClassName;
	}

	@Override
	protected void prepareParse(ChangeLogsNode parent, AbstractChangeLogContext context, int childCount) {
		this.childCount = childCount;
		context.incrementClassMappingCount();
	}
}
