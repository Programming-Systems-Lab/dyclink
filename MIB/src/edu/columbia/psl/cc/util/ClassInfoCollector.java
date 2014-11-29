package edu.columbia.psl.cc.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

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
			Type targetReturn = targetMethodType.getReturnType();
			while (calledClass != null) {
				for (Method m: calledClass.getDeclaredMethods()) {
					/*if (m.isBridge() || m.isSynthetic())
						continue ;*/
					if (m.getName().equals(methodName)) {
						Type[] mArgs = Type.getArgumentTypes(m);
						Type mReturn = Type.getReturnType(m);
						
						if (!targetReturn.equals(mReturn))
							continue ;
						
						if (mArgs.length != targetArgs.length)
							continue ;
						
						int count = 0;
						for (int i =0; i < targetArgs.length; i++) {
							if (!targetArgs[i].equals(mArgs[i]))
								continue ;
							count++;
						}
						
						if (count == targetArgs.length)
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
	
	public static void main(String[] args) throws ClassNotFoundException {
		Class<?> testClass = retrieveCorrectClassByMethod("org/apache/xerces/parsers/AbstractSAXParser", 
				"parse", 
				"(Lorg/apache/xerces/xni/parser/XMLInputSource;)V", false);
		System.out.println(testClass);
		//Type targetMethodType = Type.getMethodType("(Lorg/apache/xerces/xniparser/XMLInputSource;)V");
		//System.out.println(targetMethodType.getReturnType());
		//System.out.println(targetMethodType.getArgumentTypes().length);
		
		/*Class<?> theClazz = Class.forName("org.apache.xerces.parsers.AbstractSAXParser");
		for (Method m: theClazz.getDeclaredMethods()) {
			System.out.println(m.getName());
			System.out.println("Args: ");
			for (Class aClazz: m.getParameterTypes()) {
				System.out.println(aClazz);
			}
			System.out.println("Return type: " + m.getReturnType());
		}*/
		
	}

}
