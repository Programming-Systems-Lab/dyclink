package edu.columbia.psl.cc.annot;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)

public @interface extractTemplate {
	
}
