package edu.columbia.psl.cc.abs;

import org.objectweb.asm.commons.LocalVariablesSorter;

public interface IMethodMiner {
	
	String srHandleCommon = "handleOpcode";
	
	String srHCDesc = "(III)V";
	
	String srHCDescString = "(IILjava/lang/String;)V";
	
	String srHandleField = "handleField";
	
	String srHandleFieldDesc = "(IILjava/lang/String;Ljava/lang/String;Ljava/lang/String;)V";
	
	String srHandleLdc = "handleLdc";
	
	String srHandleLdcDesc = "(IIILjava/lang/String;)V";
	
	String srHandleMultiArray = "handleMultiNewArray";
	
	String srHandleMultiArrayDesc = "(Ljava/lang/String;II)V";
	
	String srHandleMethod = "handleMethod";
	
	String srHandleMethodDesc = "(IIILjava/lang/String;Ljava/lang/String;Ljava/lang/String;)V";
	
	String srHandleMethodAfter = "handleMethodAfter";
	
	String srHandleMethodAfterDesc = "(II)V";
	
	String srGraphDump = "dumpGraph";
	
	String srGraphDumpDesc = "()V";
	
	String srLoadParent = "loadParent";
	
	String srLoadParentDesc = "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V";
	
	String srCheckClInit = "checkNGetClInit";
	
	String srCheckClInitDesc = "(Ljava/lang/String;)V";
	
	String srUpdateCurLabel = "updateCurLabel";
	
	String srUpdateCurLabelDesc = "(Ljava/lang/String;)V";
		
	String __mib_id = "__MIB_ID";
	
	String recordObjMap = "recordObjId";
	
	String recordObjDesc = "(ILjava/lang/Object;)V";
	
	String objOnStack = "updateObjOnStack";
	
	String objOnStackDesc = "(Ljava/lang/Object;I)V";
}
