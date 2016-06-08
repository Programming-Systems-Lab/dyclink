package edu.columbia.psl.cc.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.objectweb.asm.Type;

import edu.columbia.psl.cc.config.MIBConfiguration;
import edu.columbia.psl.cc.datastruct.BytecodeCategory;
import edu.columbia.psl.cc.pojo.ClassMethodInfo;
import edu.columbia.psl.cc.pojo.InstNode;

public class ClassInfoCollector {
	
	private static Logger logger = LogManager.getLogger(ClassInfoCollector.class);
	
	private static boolean INIT_REF = MIBConfiguration.getInstance().isInitRef();
		
	private static HashMap<String, ClassMethodInfo> classMethodInfoMap = new HashMap<String, ClassMethodInfo>();
	
	private static HashMap<String, Class> methodToClass = new HashMap<String, Class>();
	
	private static HashMap<String, Class> fieldToClass = new HashMap<String, Class>();
	
	public static ClassMethodInfo initiateClassMethodInfo(String className, String methodName, String methodDesc, boolean isStatic) {
		String classMethodCacheKey = StringUtil.concateKey(className, methodName, methodDesc);
		
		if (classMethodInfoMap.containsKey(classMethodCacheKey)) {
			return classMethodInfoMap.get(classMethodCacheKey);
		}
		
		ClassMethodInfo cmi = new ClassMethodInfo();
		
		//Set up arg len and arg size
		Type methodType = Type.getMethodType(methodDesc);
		Type[] args = methodType.getArgumentTypes();
		Type returnType = methodType.getReturnType();
		
		int argSize = 0;
		int[] idxArray;
		if (INIT_REF && !isStatic) {
			idxArray = new int[args.length + 1];
			idxArray[0] = 0;
			
			int realIdx = 1;
			for (int i = 1; i < idxArray.length; i++) {
				idxArray[i] = realIdx;
				Type argType = args[i - 1];
				if (argType.getSort() == Type.DOUBLE || argType.getSort() == Type.LONG) {
					argSize += 2;
					realIdx += 2;
				} else {
					argSize++;
					realIdx++;
				}
			}
		} else {
			idxArray = new int[args.length];
			int realIdx = 1;
			if (isStatic) {
				realIdx = 0;
			}
			
			for (int i = 0; i < args.length; i++) {
				idxArray[i] = realIdx;
				if (args[i].getSort() == Type.DOUBLE || args[i].getSort() == Type.LONG) {
					argSize += 2;
					realIdx += 2;
				} else {
					argSize++;
					realIdx++;
				}
			}
		}
				
		cmi.args = args;
		cmi.returnType = returnType;
		cmi.argSize = argSize;
		//cmi.endIdx = endIdx;
		cmi.idxArray = idxArray;
		classMethodInfoMap.put(classMethodCacheKey, cmi);
		return cmi;
	}
	
	public static ClassMethodInfo retrieveClassMethodInfo(String className, String methodName, String methodDesc, int opcode) {
		String classMethodCacheKey = StringUtil.concateKey(className, methodName, methodDesc);
				
		if (classMethodInfoMap.containsKey(classMethodCacheKey)) {
			return classMethodInfoMap.get(classMethodCacheKey);
		} else {
			boolean isStatic = false;
			if (BytecodeCategory.staticMethodOps().contains(opcode)) {
				isStatic = true;
			}
			
			initiateClassMethodInfo(className, methodName, methodDesc, isStatic);			
			return classMethodInfoMap.get(classMethodCacheKey);
		}
	}
	
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
			//ex.printStackTrace();
			logger.error("Error: ", ex);
		}
		return null;
	}

	public static Class<?> retrieveCorrectClassByMethod(String className, 
			String methodName, String methodDescriptor, boolean direct) {
		String classMethodCacheKey = StringUtil.concateKey(className, methodName, methodDescriptor);
		if (methodToClass.containsKey(classMethodCacheKey)) {
			return methodToClass.get(classMethodCacheKey);
		}
		
		try { 
			className = className.replace("/", ".");
			Class<?> calledClass = Class.forName(className);
			
			if (direct) {
				//direct is for <init> and private method of INVOKESPECIAL
				methodToClass.put(classMethodCacheKey, calledClass);
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
												
						if (mArgs.length != targetArgs.length)
							continue ;
						
						int count = 0;
						for (int i =0; i < targetArgs.length; i++) {
							if (!targetArgs[i].equals(mArgs[i]))
								continue ;
							count++;
						}
						
						if (count == targetArgs.length) {
							methodToClass.put(classMethodCacheKey, calledClass);
							return calledClass;
						}
					}
				}
				calledClass = calledClass.getSuperclass();
			}
			
			//Something wrong if reaching here
			return calledClass;
		} catch (Exception ex) {
			//ex.printStackTrace();
			logger.error("Error: ", ex);
		}
		return null;
	}
	
	/**
	 * @param className
	 * @param name
	 * @return
	 */
	public static Class<?> retrieveCorrectClassByField(String className, String name) {
		String classFieldKey = StringUtil.concateKey(className, name);
		if (fieldToClass.containsKey(classFieldKey))
			return fieldToClass.get(classFieldKey);
		
		try {
			className = className.replace("/", ".");
			Class<?> calledClass = Class.forName(className);
			Class<?>[] interfaces = calledClass.getInterfaces();
			while (calledClass != null) {
				for (Field f: calledClass.getDeclaredFields()) {
					//Name should be enough to find the correct field?
					if (f.getName().equals(name)) {
						fieldToClass.put(classFieldKey, calledClass);
						return calledClass;
					}
				}
				calledClass = calledClass.getSuperclass();
			}
			
			if (calledClass == null) {
				//Because inner class access public outer class
				List<Class<?>> interfaceQueue = new ArrayList<Class<?>>();
				for (Class<?> inter: interfaces) {
					interfaceQueue.add(inter);
				}
				
				while (interfaceQueue.size() > 0) {
					Class<?> inter = interfaceQueue.remove(0);
					for (Field f: inter.getDeclaredFields()) {
						if (f.getName().equals(name)) {
							fieldToClass.put(classFieldKey, inter);
							return inter;
						}
					}
					
					for (Class<?> interParent: inter.getInterfaces()) {
						interfaceQueue.add(interParent);
					}
				}
			}
			//Something wrong if we reach here
			return calledClass;
		} catch (Exception ex) {
			//ex.printStackTrace();
			logger.error("Error: ", ex);
		}
		return null;
	}
	
	public static void main(String[] args) throws ClassNotFoundException {
		/*Class<?> testClass = retrieveCorrectClassByMethod("org/apache/xerces/parsers/AbstractSAXParser", 
				"parse", 
				"(Lorg/apache/xerces/xni/parser/XMLInputSource;)V", false);
		System.out.println(testClass);*/
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
		
		/*String className = "org.ujmp.core.doublematrix.calculation.general.decomposition.Solve$1";
		String fieldName = "MATRIXSQUARELARGEMULTITHREADED";
		System.out.println("Class name: " + retrieveCorrectClassByField(className, fieldName).getName());
		try {
			Class innerClass = Class.forName(className);
			System.out.println("Check inner class: " + innerClass);
			System.out.println("Outer class: " + innerClass.getEnclosingClass());
			int[] ar = new int[5];
			int[][] multiAr = new int[3][4];
			Object[] objSingleDim = new Object[5];
			Object[][] obj = new Object[4][5];
			Object o = new Object();
		} catch (Exception ex) {
			ex.printStackTrace();
		}*/
		initiateClassMethodInfo("a", "b", "(D[LR5P1Y11.aditsu.Cakes$P;I)D", false);
		String classMethodCacheKey = StringUtil.concateKey("a", "b", "(D[LR5P1Y11.aditsu.Cakes$P;I)D");
		System.out.println(Arrays.toString(classMethodInfoMap.get(classMethodCacheKey).idxArray));
	}

}
