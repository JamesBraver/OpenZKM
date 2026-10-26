package com.zelix.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface ZKMClassLevel {
	ExceptionObfuscationPolicy exceptionObfuscation() default ExceptionObfuscationPolicy.LIGHT;

	FlowObfuscationPolicy obfuscateFlow() default FlowObfuscationPolicy.LIGHT;
}
