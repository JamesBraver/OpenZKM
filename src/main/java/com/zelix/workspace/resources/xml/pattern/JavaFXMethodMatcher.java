package com.zelix.workspace.resources.xml.pattern;

import com.zelix.exceptions.FileException;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.logging.AbstractLogger;
import com.zelix.workspace.AbstractWorkspace;
import java.util.List;

public class JavaFXMethodMatcher implements MethodPattern {
	private final AbstractWorkspace classHierarchy;
	private final AbstractLogger logger;

	public JavaFXMethodMatcher(AbstractWorkspace classHierarchy, AbstractLogger logger) {
		this.classHierarchy = classHierarchy;
		this.logger = logger;
	}

	@Override
	public boolean matches(String descriptor) {
		List<String> parameterDescriptors = TypeWriter.parseParameterDescriptors(descriptor);
		int parameterCount = parameterDescriptors.size();
		if (parameterCount == 0) {
			return true;
		} else {
			if (parameterCount == 1) {
				String singleParameterDescriptor = parameterDescriptors.get(0);
				if (singleParameterDescriptor.charAt(0) == '[') {
					return false;
				}

				if (singleParameterDescriptor.charAt(0) != 'L' || singleParameterDescriptor.charAt(singleParameterDescriptor.length() - 1) != ';') {
					return false;
				}

				String singleParameterClassName = singleParameterDescriptor.substring(1, singleParameterDescriptor.length() - 1);

				try {
					if (!singleParameterClassName.equals("javafx/event/Event")
						&& !this.classHierarchy.isClassInstanceOf(singleParameterClassName, "javafx/event/Event")
						&& !singleParameterClassName.equals("javafx/collections/ListChangeListener")
						&& !this.classHierarchy.isSubclassOf(singleParameterClassName, "javafx/collections/ListChangeListener")
						&& !singleParameterClassName.equals("javafx/collections/MapChangeListener")
						&& !this.classHierarchy.isSubclassOf(singleParameterClassName, "javafx/collections/MapChangeListener")
						&& !singleParameterClassName.equals("javafx/collections/SetChangeListener")
						&& !this.classHierarchy.isSubclassOf(singleParameterClassName, "javafx/collections/SetChangeListener")) {
						return false;
					}

					return true;
				} catch (FileException eventLookupException) {
					this.logger.fatal("FATAL ERROR:", eventLookupException.getMessage());
				}
			} else if (parameterCount == 3) {
				String firstParameterDescriptor = parameterDescriptors.get(0);
				if (firstParameterDescriptor.charAt(0) == '[') {
					return false;
				}

				if (!parameterDescriptors.get(1).equals(parameterDescriptors.get(2))) {
					return false;
				}

				if (firstParameterDescriptor.charAt(0) != 'L' || firstParameterDescriptor.charAt(firstParameterDescriptor.length() - 1) != ';') {
					return false;
				}

				String firstParameterClassName = firstParameterDescriptor.substring(1, firstParameterDescriptor.length() - 1);

				try {
					if (!firstParameterClassName.equals("javafx/beans/value/ObservableValue") && !this.classHierarchy.isSubclassOf(firstParameterClassName, "javafx/beans/value/ObservableValue")) {
						return false;
					}

					return true;
				} catch (FileException observableValueLookupException) {
					this.logger.fatal("FATAL ERROR:", observableValueLookupException.getMessage());
				}
			}

			return false;
		}
	}
}
