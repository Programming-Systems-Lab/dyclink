package edu.columbia.psl.cc.abs;

import java.lang.reflect.Field;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.columbia.psl.cc.crawler.NativePackages;
import edu.columbia.psl.cc.pojo.InstNode;
import edu.columbia.psl.cc.util.StringUtil;

public abstract class AbstractRecorder {
	
	private final static Logger logger = LogManager.getLogger(AbstractRecorder.class);
	
	public static final int SIG_INVALID = -10;
	
	public static final int SIG_JVM = -20;
	
	public static final int CONSTRUCTOR_DEFAULT = -5;
	
	public static final double EPSILON = 0.0001;
	
	public static String defaultPkgId = String.valueOf(NativePackages.defaultId);
	
	public abstract void handleLdc(int opcode, int instIdx, int times, String addInfo);
	
	public abstract void handleField(int opcode, int instIdx, String owner, String name, String desc);
	
	public abstract void handleOpcode(int opcode, int instIdx, String addInfo);
	
	public abstract void handleOpcode(int opcode, int instIdx, int localVarIdx);
	
	public abstract void handleMultiNewArray(String desc, int dim, int instIdx);
	
	public abstract void handleRawMethod(int opcode, 
			int instIdx, 
			int linenum, 
			String owner, 
			String name, 
			String desc, 
			InstNode fullInst);
	
	public abstract void handleMethod(int opcode, int instIdx, int linenum, String owner, String name, String desc);
	
	public abstract void handleDup(int opcode);
	
	public abstract void dumpGraph();
	
	public static final int parseObjId(Object value) {
		if (value == null)
			return -1;
		
		Class<?> valueClass = value.getClass();
		try {
			Field idField = valueClass.getField(IMethodMiner.__mib_id);
			idField.setAccessible(true);
			/*System.out.println("Traverse fields of " + valueClass);
			for (Field f: valueClass.getFields()) {
				System.out.println(f);
			}*/
			int objId = idField.getInt(value);
			//System.out.println("Obj: " + value);
			//System.out.println("Id: " + objId);
			return objId;
		} catch (Exception ex) {
			//ex.printStackTrace();
			//System.out.println("Warning: object " + valueClass + " is not MIB-instrumented");
			if (StringUtil.shouldIncludeClass(valueClass.getName())) {
				logger.error("Invalid object id: " + valueClass.getName());
				return SIG_INVALID;
			} else {
				logger.warn("Warning: object " + valueClass + " is not MIB-instrumented");
				return SIG_JVM;
			}
		}
	}

}
