package edu.columbia.psl.cc.premain;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import edu.columbia.psl.cc.inst.MIBClassFileTransformer;
import edu.columbia.psl.cc.inst.NaiveTransformer;

public class PreMain {
	
	public static Instrumentation inst;

	public static void premain(String args, Instrumentation inst) {
		PreMain.inst = inst;
		//showAllLoadedClasses();
		
		ClassFileTransformer classTransformer = new MIBClassFileTransformer();
		//ClassFileTransformer classTransformer = new NaiveTransformer();
		
		inst.addTransformer(classTransformer, true);
		//retransformPreLoadedClasses();
		//showAllLoadedClasses();
		
		/*System.out.println("Instrumnetation: " + inst.getAll);
		try {
			Field f = sun.instrument.InstrumentationImpl.class.getDeclaredField("mTransformerManager");
			f.setAccessible(true);
			
			sun.instrument.TransformerManager tmp = (sun.instrument.TransformerManager)f.get(inst);
			Field tList = tmp.getClass().getDeclaredField("mTransformerList");
			tList.setAccessible(true);
			Object[] arr = (Object[])tList.get(tmp);
			System.out.println("Transformerlist: " + arr.length);
		} catch (Exception ex) {
			ex.printStackTrace();;
		}*/
	}
	
	public static void retransformPreLoadedClasses() {
		try {
			int counter = 0;
			for (Class clazz: inst.getAllLoadedClasses()) {
				if (clazz.getClassLoader() != null 
						&& !clazz.isArray() 
						&& !clazz.isPrimitive()) {
					System.out.println("Retransform class: " + clazz.getName());
					inst.retransformClasses(clazz);
					
					counter++;
				}
			}
			System.out.println("Total transform preloaded classes: " + counter);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	public static Class searchLoadedClasses(String targetName) {
		//System.out.println("All loaded classes: " + inst.getAllLoadedClasses().length);
		Class ret = null;
		int counter = 0;
		for (Class clazz: inst.getAllLoadedClasses()) {
			if (clazz.getName().equals(targetName)) {
				ret = clazz;
				counter++;
				System.out.println("Find " + counter + " " + targetName);
				System.out.println("Class loader: " + clazz.getClassLoader());
			}
		}
		//System.out.println("Non-bootstrapping classes: " + records.size());
		//System.out.println();
		return ret;
	}
	
	public static void analyzeClassLoaders(String className) {
		Set<ClassLoader> loaders = new HashSet<ClassLoader>();
		for (Class clazz: inst.getAllLoadedClasses()) {
			ClassLoader loader = clazz.getClassLoader();
			if (loader != null) {
				loaders.add(loader);
			}
		}
		
		for (ClassLoader loader: loaders) {
			try {
				Method m = ClassLoader.class.getDeclaredMethod("findLoadedClass", String.class);
				m.setAccessible(true);
				Class c = (Class)m.invoke(loader, className);
				if (c != null)
					System.out.println("Find " + className + " in " + loader + " " + c.hashCode());
				else 
					System.out.println("Fail to find " + className + " in " + loader);
			} catch (Exception ex) {
				System.out.println("Fail to find " + className + " in " + loader);
			}
		}
	}

}
