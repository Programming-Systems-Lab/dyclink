package edu.columbia.psl.cc.util;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import edu.columbia.psl.cc.config.MIBConfiguration;

public class ObjectIdAllocater {

	//Save 0 for method stack recorder to identify static method
	private static AtomicInteger indexer = new AtomicInteger(1);
	
	private static HashMap<String, AtomicInteger> classMethodIndexer = new HashMap<String, AtomicInteger>();
	
	public static synchronized int getIndex() {
		return indexer.getAndIncrement();
	}
	
	public static synchronized int getClassMethodIndex(String className, String methodName, String desc) {
		Class<?> correctClass = ClassInfoCollector.retrieveCorrectClassByMethod(className, methodName, desc, false);
		String methodKey = StringUtil.genKey(correctClass.getName(), methodName, desc);
		
		if (!classMethodIndexer.containsKey(methodKey)) {
			AtomicInteger ai = new AtomicInteger();
			classMethodIndexer.put(methodKey, ai);
		}
		return classMethodIndexer.get(methodKey).getAndIncrement();
	}
	
	public static int parseObjId(Object value) {
		if (value == null)
			return -1;
		
		Class<?> valueClass = value.getClass();
		try {
			Field idField = valueClass.getField(MIBConfiguration.getMibId());
			idField.setAccessible(true);
			/*System.out.println("Traverse fields of " + valueClass);
			for (Field f: valueClass.getFields()) {
				System.out.println(f);
			}*/
			int objId = idField.getInt(value);
			return objId;
		} catch (Exception ex) {
			//ex.printStackTrace();
			System.out.println("Warning: object " + valueClass + " is not MIB-instrumented");
			return -1;
		}
	}
	
	public static void main(String[] args) {
		System.out.println(getIndex());
		System.out.println(getClassMethodIndex("a", "b", "c"));
	}
}
