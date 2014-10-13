package edu.columbia.psl.cc.config;

public class MIBConfiguration {

	private static String opTablePath = "opcodes/opcode_cats.csv";
	
	private static String opCodeCatId = "opcodes/opcode_ids.csv";
	
	private static String costTableDir = "./cost_tables/";
	
	private static int costLimit = (int)1E2;
	
	private static String srHandleCommon = "handleOpcode";
	
	private static String srHCDesc = "(III)V";
	
	private static String srHCDescString = "(IILjava/lang/String;)V";
	
	private static String srHandleField = "handleField";
	
	private static String srHandleFieldDesc = "(IILjava/lang/String;I)V";
	
	private static String srHandleLdc = "handleLdc";
	
	private static String srHandleLdcDesc = "(IIILjava/lang/String;)V";
	
	private static String srHandleMultiArray = "handleMultiNewArray";
	
	private static String srHandleMultiArrayDesc = "(Ljava/lang/String;II)V";
	
	private static String srHandleMethod = "handleMethod";
	
	private static String srHandleMethodDesc = "(IIILjava/lang/String;Ljava/lang/String;Ljava/lang/String;)V";
	
	private static String srGraphDump = "dumpGraph";
	
	private static String srGraphDumpDesc = "(Z)V";
	
	private static String templateDir = "./template";
	
	private static String testDir = "./test";
	
	private static String pathDir = "./path";
	
	private static String labelmapDir = "./labelmap";
	
	private static String __mib_id_gen = "__MIB_ID_GEN";
	
	private static String __mib_id_gen_method = "__getMIBIDGen";
	
	private static String __mib_id = "__MIB_ID";
	
	private static double controlWeight = 1.0;
	
	private static double dataWeight = 1.0;
	
	private static int precisionDigit = 3;
	
	public static int getPrecisionDigit() {
		return precisionDigit;
	}
	
	public static double getControlWeight() {
		return controlWeight;
	}
	
	public static double getDataWeight() {
		return dataWeight;
	}
	
	public static String getCostTableDir() {
		return costTableDir;
	}
	
	public static int getCostLimit() {
		return costLimit;
	}
	
	public static String getTemplateDir() {
		return templateDir;
	}
	
	public static String getTestDir() {
		return testDir;
	}
	
	public static String getPathDir() {
		return pathDir;
	}
	
	public static String getLabelmapDir() {
		return labelmapDir;
	}
	
	public static String getOpTablePath() {
		return opTablePath;
	}
	
	public static String getOpCodeCatId() {
		return opCodeCatId;
	}
	
	public static String getSrHandleCommon() {
		return srHandleCommon;
	}
	
	public static String getSrHCDesc() {
		return srHCDesc;
	}
	
	public static String getSrHCDescString() {
		return srHCDescString;
	}
	
	public static String getSrHandleMultiArray() {
		return srHandleMultiArray;
	}
	
	public static String getSrHandleMultiArrayDesc() {
		return srHandleMultiArrayDesc;
	}
	
	public static String getSrHandleMethod() {
		return srHandleMethod;
	}
	
	public static String getSrHandleMethodDesc() {
		return srHandleMethodDesc;
	}
	
	public static String getSrGraphDump() {
		return srGraphDump;
	}
	
	public static String getSrGraphDumpDesc() {
		return srGraphDumpDesc;
	}
	
	public static String getSrHandleLdc() {
		return srHandleLdc;
	}
	
	public static String getSrHandleLdcDesc() {
		return srHandleLdcDesc;
	}
	
	public static String getSrHandleField() {
		return srHandleField;
	}
	
	public static String getSrHandleFieldDesc() {
		return srHandleFieldDesc;
	}
	
	public static String getMIBIDGen() {
		return __mib_id_gen;
	}
	
	public static String getMIBIDGenMethod() {
		return __mib_id_gen_method;
	}
	
	public static String getMIBID() {
		return __mib_id;
	}

}
