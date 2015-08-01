package edu.columbia.psl.cc.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import edu.columbia.psl.cc.config.MIBConfiguration;
import edu.columbia.psl.cc.pojo.InstNode;

public class StringUtil {
	
	private static String pattern = ")";
	
	private static final Pattern shouldRemove = Pattern.compile("[](){},.;!?\\/%]");
	
	public static boolean shouldIncludeClass(String name) {
		List<String> excludeClass = MIBConfiguration.getInstance().getExcludeClass();
		for (String exclude: excludeClass) {
			if (name.startsWith(exclude)) {
				return false;
			}
		}
		return true;
	}
	
	public static boolean isTestClass(String path) {
		List<String> testPaths = MIBConfiguration.getInstance().getTestPaths();
		
		if (testPaths == null) {
			return false;
		}
		
		return testPaths.contains(path);
	}
	
	public static boolean shouldIncludeMethod(String methodName, String methodDesc) {
		if (methodName.equals("toString") && Type.getReturnType(methodDesc).equals(Type.getType(String.class))) {
			return false;
		} else if (methodName.equals("equals") && Type.getReturnType(methodDesc).equals(Type.BOOLEAN_TYPE)) {
			return false;
		} else if (methodName.equals("hashCode") && Type.getReturnType(methodDesc).equals(Type.INT_TYPE)) {
			return false;
		} else {
			return true;
		}
	}
	
	public static String removeUUID(String name) {
		name = name.replace(".json", "");
		int lastSemi = name.lastIndexOf(":");
		name = name.substring(0, lastSemi);
		return name;
	}
	
	public static String genRelation(String s1, String s2) {
		return s1 + "->" + s2;
	}
	
	public static String genRuntimeDescription(Object o) {
		Type realType = Type.getType(o.getClass());
		
		if (realType.getSort() == Type.ARRAY) {
			StringBuilder sb = new StringBuilder();
			int dim = realType.getDimensions();
			String desc = realType.getElementType().getDescriptor();
			
			for (int i = 0; i < dim; i++) {
				sb.append("[");
			}
			sb.append(desc);
			return sb.toString();
		} else {
			return realType.getDescriptor();
		}
	}
	
	public static String cleanPuncHelper(String oriString, String newVal) {
		//String ret = oriString.replaceAll("\\W", newVal);
		String ret = shouldRemove.matcher(oriString).replaceAll(newVal);
		return ret;
	}
	
	public static String cleanPunc(String oriString, Object...newVal) {
		if (newVal.length == 0) {
			return cleanPuncHelper(oriString, "");
		} else {
			String replaceBy = (String)newVal[0];
			return cleanPuncHelper(oriString, replaceBy);
		}
	}
	
	public static String[] parseDesc(String desc) {
		int idx = desc.lastIndexOf(pattern);
		
		if (idx == -1) {
			System.err.println("Incorrect desc: " + desc);
			return null;
		}
		
		String in = desc.substring(1, idx);
		String[] rawIn = in.split(";");
		StringBuilder sb = new StringBuilder();
		for (String r: rawIn) {
			sb.append(cleanPunc(r, ".") + "+");
		}
		String retIn = "(" + sb.substring(0, sb.length() - 1) + ")";
		String retOut = cleanPunc(desc.substring(idx + 1, desc.length()), ".");
		if (retOut.charAt(retOut.length() - 1) == '.') {
			retOut = retOut.substring(0, retOut.length() - 1);
		}
		
		String[] ret = new String[]{retIn, retOut};
		return ret;
	}
	
	public static String genKeyWithObjId(String searchKey, int objId) {
		return searchKey + "-" + objId;
	}
	
	/**
	 * Append the id to the existing key, used in multiple purposes
	 * @param key
	 * @param id
	 * @return
	 */
	public static String genKeyWithId(String key, String id) {
		return key + ":" + id;
	}
	
	/**
	 * Append multiple ids to the existing key
	 * @param key
	 * @param ids
	 * @return
	 */
	public static String genKeyWithIds(String key, String...ids) {
		for (String id: ids) {
			key = genKeyWithId(key, id);
		}
		return key;
	}
	
	public static String completeMethodKeyWithInfo(String curMethodKey, 
			String pkgId, 
			String methodDesc, 
			int opcode) {
		int inputArgs = Type.getArgumentTypes(methodDesc).length;
		
		//For obj ref
		if (opcode != Opcodes.INVOKESTATIC)
			inputArgs += 1;
		
		String inputSize = String.valueOf(inputArgs);
		String outputSize = String.valueOf(Type.getReturnType(methodDesc).getSort() == Type.VOID? 0: 1);
		return genKeyWithIds(curMethodKey, inputSize, outputSize, pkgId);
	}
		
	/**
	 * Generate keys based a method's class name, method name and method desc
	 * @param className
	 * @param methodName
	 * @param methodDesc
	 * @return
	 */
	public static String genKey(String className, String methodName, String methodDesc) {
		String[] parsedDesc = StringUtil.parseDesc(methodDesc);
		String key = StringUtil.cleanPunc(className, ".") 
				+ ":" + StringUtil.cleanPunc(methodName) 
				+ ":" + parsedDesc[0] 
				+ ":" + parsedDesc[1];
		return key;
	}
	
	public static String extractPkg(String className) {
		int dotIdx = className.lastIndexOf(".");
		String pkg = className.substring(0, dotIdx);
		return pkg;
	}
	
	public static int extractPkgId(String addInfo) {
		String[] infoArr = addInfo.split(":");
		String valString = infoArr[infoArr.length - 1];
		return Integer.valueOf(valString);
	}
	
	public static int[] extractIO(String addInfo) {
		String[] infoArr = addInfo.split(":");
		return new int[] {Integer.valueOf(infoArr[infoArr.length - 3]), Integer.valueOf(infoArr[infoArr.length - 2])};
	}
	
	public static String genIdxKey(int threadId, 
			int threadMethodIdx, 
			int idx) {
		return genThreadWithMethodIdx(threadId, threadMethodIdx) + "-" + idx;
	}
	
	public static String genThreadWithMethodIdx(int threadId, int threadMethodIdx) {
		return threadId + "-" + threadMethodIdx;
	}
	
	public static String concateKey(String...elements) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < elements.length; i++) {
			sb.append(elements[i]);
			
			if (i != elements.length - 1)
				sb.append("-");
		}
		return sb.toString();
	}
	
	public static String genEdgeKey(InstNode from, InstNode to) {
		String fromKey = from.getFromMethod() + "-" + from.getThreadId() + "-" + from.getIdx();
		String toKey = to.getFromMethod() + "-" + to.getThreadId() + "-" + to.getIdx();
		return fromKey + "=>" + toKey;
	}
	
	public static String[] parseIdxKey(String idxKey) {
		return idxKey.split("-");
	}
	
	public static String parseElement(String inst, int idx) {
		String[] instElements = inst.split(" ");
		String label = instElements[idx];
		return label;
	}
	
	public static String replaceLabel(String oriInst, HashMap<String, Integer> labelMap) {
		String opLabel = StringUtil.parseElement(oriInst, 0);
		String npLabel = "L" + labelMap.get(opLabel);
		String newInst = oriInst.replace(opLabel, npLabel);
		return newInst;
	}
	
	public static String appendMap(String key) {
		return key + "_map";
	}
	
	public static String concateLineTrace(Collection trace) {
		StringBuilder sb = new StringBuilder();
		for (Object o: trace) {
			sb.append(o + ":");
		}
		
		if (sb.length() == 0)
			return "";
		else {
			return sb.substring(0, sb.length() - 1);
		}
	}
	
	public String aggregateLoad(Object...info) {
		StringBuilder sb = new StringBuilder();
		for (Object i: info) {
			sb.append(i + ":");
		}
		return sb.substring(0, sb.length() - 1);
	}
	
	public String separateLoad(String[] infoArray) {
		StringBuilder sb = new StringBuilder();
		for (String s: infoArray) {
			sb.append(s + " ");
		}
		return sb.substring(0, sb.length() - 1);
	}
	
	public static void main (String[] args) {
		//String name = "1234:6.json";
		//System.out.println("Remove 6: " + removeUUID(name));
		//String name = "SvdImplicitQrDecompose_D64";
		//System.out.println(cleanPunc(name, "."));
		//String name = "sun/util/PreHashedMap";
		//System.out.println("Include: " + StringUtil.shouldIncludeClass(name.replace("/", ".")));
		String name = "sun.util.PreHashedMap";
		System.out.println("Pkg name: " + extractPkg(name));
	}

}
