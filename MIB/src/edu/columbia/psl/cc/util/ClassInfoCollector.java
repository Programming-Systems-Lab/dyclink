package edu.columbia.psl.cc.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;

import org.objectweb.asm.Type;

public class ClassInfoCollector {
	
	public static Class<?> retrieveCorrectClassByConstructor(String className, String constName, String constDescriptor) {
		try {
			className = className.replace("/", ".");
			Class<?> calledClass = Class.forName(className);
			if (calledClass != null) {
				for (Constructor<?> c: calledClass.getDeclaredConstructors()) {
					if (c.getName().equals(constName) 
							&& constDescriptor.equals(Type.getConstructorDescriptor(c))) {
						return calledClass;
					}
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}

	public static Class<?> retrieveCorrectClassByMethod(String className, String methodName, String methodDescriptor, boolean direct) {
		try {
			className = className.replace("/", ".");
			Class<?> calledClass = Class.forName(className);
			
			if (direct) {
				//direct is for <init> and private method of INVOKESPECIAL
				return calledClass;
			}
			
			Type targetMethodType = Type.getMethodType(methodDescriptor);
			Type[] targetArgs = targetMethodType.getArgumentTypes();
			while (calledClass != null) {
				for (Method m: calledClass.getDeclaredMethods()) {
					if (m.isBridge() || m.isSynthetic())
						continue ;
					
					if (m.getName().equals(methodName)) {
						Type[] mArgs = Type.getArgumentTypes(m);
						
						if (mArgs.length != targetArgs.length)
							continue;
						
						for (int i =0; i < targetArgs.length; i++) {
							if (!targetArgs[i].equals(mArgs[i]))
								continue ;
						} 
						return calledClass;
					}
				}
				calledClass = calledClass.getSuperclass();
			}
			return calledClass;
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}
	
	/**
	 * @param className
	 * @param name
	 * @return
	 */
	public static Class<?> retrieveCorrectClassByField(String className, String name) {
		try {
			className = className.replace("/", ".");
			Class<?> calledClass = Class.forName(className);
			while (calledClass != null) {
				for (Field f: calledClass.getDeclaredFields()) {
					//Name should be enough to find the correct field?
					if (f.getName().equals(name)) {
						return calledClass;
					}
				}
				calledClass = calledClass.getSuperclass();
			}
			return calledClass;
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}
	
	public static void main(String[] args) {
		Class clazz = retrieveCorrectClassByField("cc/testbase/TemplateParent", "sVar");
		Class clazz2 = retrieveCorrectClassByMethod("java/util/ArrayList", "<init>", "()V", true);
		System.out.println("Check class name: " + clazz.getName());
		System.out.println("Check clazz2: " + clazz2.getName());
		
		ArrayList a = new ArrayList();
	}

}
