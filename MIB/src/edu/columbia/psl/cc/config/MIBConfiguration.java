package edu.columbia.psl.cc.config;

import java.io.File;

import com.google.gson.reflect.TypeToken;

import edu.columbia.psl.cc.util.GsonManager;

public class MIBConfiguration {
	
	private static String srHandleCommon = "handleOpcode";
	
	private static String srHCDesc = "(III)V";
	
	private static String srHCDescString = "(IILjava/lang/String;)V";
	
	private static String srHandleField = "handleField";
	
	private static String srHandleFieldDesc = "(IILjava/lang/String;Ljava/lang/String;Ljava/lang/String;)V";
	
	private static String srHandleLdc = "handleLdc";
	
	private static String srHandleLdcDesc = "(IIILjava/lang/String;)V";
	
	private static String srHandleMultiArray = "handleMultiNewArray";
	
	private static String srHandleMultiArrayDesc = "(Ljava/lang/String;II)V";
	
	private static String srHandleMethod = "handleMethod";
	
	private static String srHandleMethodDesc = "(IIILjava/lang/String;Ljava/lang/String;Ljava/lang/String;)V";
	
	private static String srGraphDump = "dumpGraph";
	
	private static String srGraphDumpDesc = "(Z)V";
	
	private static String __mib_id_gen = "__MIB_ID_GEN";
	
	private static String __mib_id_gen_method = "__getMIBIDGen";
	
	private static String __mib_id = "__MIB_ID";
	
	private static String recordObjMap = "recordObjId";
	
	private static String recordObjDesc = "(ILjava/lang/Object;)V";
	
	private static String objOnStack = "updateObjOnStack";
	
	private static String objOnStackDesc = "(Ljava/lang/Object;)V";
	
	private String opTablePath;
	
	private String opCodeCatId;
	
	private String costTableDir;
	
	private int costLimit;
	
	private String templateDir;
	
	private String testDir;
	
	private String pathDir;
	
	private String labelmapDir;
	
	private String resultDir;
	
	private String debugDir;
	
	private double controlWeight;
	
	private double dataWeight;
	
	private int precisionDigit;
	
	//private static int parallelFactor = Runtime.getRuntime().availableProcessors();
	private int parallelFactor;
	
	private boolean annotGuard;
	
	private boolean cleanTemplate;
	
	private boolean cleanTest;
	
	private boolean debug;
	
	private static MIBConfiguration instance = null;
	
	private MIBConfiguration() {
		
	}
	
	public static MIBConfiguration getInstance() {
		if (instance == null) {
			File f = new File("./config/mib_config.json");
			TypeToken<MIBConfiguration> configType = new TypeToken<MIBConfiguration>(){};
			instance = GsonManager.readJsonGeneric(f, configType);
			return instance;
		}
		return instance;
	}
	
	public static void main(String args[]) {
		System.out.println(MIBConfiguration.getInstance());
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

	public static String getSrHandleField() {
		return srHandleField;
	}

	public static String getSrHandleFieldDesc() {
		return srHandleFieldDesc;
	}

	public static String getSrHandleLdc() {
		return srHandleLdc;
	}

	public static String getSrHandleLdcDesc() {
		return srHandleLdcDesc;
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

	public static String getMibIdGen() {
		return __mib_id_gen;
	}

	public static String getMibIdGenMethod() {
		return __mib_id_gen_method;
	}

	public static String getMibId() {
		return __mib_id;
	}

	public static String getRecordObjMap() {
		return recordObjMap;
	}

	public static String getRecordObjDesc() {
		return recordObjDesc;
	}

	public static String getObjOnStack() {
		return objOnStack;
	}

	public static String getObjOnStackDesc() {
		return objOnStackDesc;
	}
	
	public String getOpTablePath() {
		return opTablePath;
	}

	public void setOpTablePath(String opTablePath) {
		this.opTablePath = opTablePath;
	}

	public String getOpCodeCatId() {
		return opCodeCatId;
	}

	public void setOpCodeCatId(String opCodeCatId) {
		this.opCodeCatId = opCodeCatId;
	}

	public String getCostTableDir() {
		return costTableDir;
	}

	public void setCostTableDir(String costTableDir) {
		this.costTableDir = costTableDir;
	}

	public int getCostLimit() {
		return costLimit;
	}

	public void setCostLimit(int costLimit) {
		this.costLimit = costLimit;
	}

	public double getControlWeight() {
		return controlWeight;
	}

	public void setControlWeight(double controlWeight) {
		this.controlWeight = controlWeight;
	}

	public double getDataWeight() {
		return dataWeight;
	}

	public void setDataWeight(double dataWeight) {
		this.dataWeight = dataWeight;
	}

	public int getPrecisionDigit() {
		return precisionDigit;
	}

	public void setPrecisionDigit(int precisionDigit) {
		this.precisionDigit = precisionDigit;
	}

	public int getParallelFactor() {
		return parallelFactor;
	}

	public void setParallelFactor(int parallelFactor) {
		this.parallelFactor = parallelFactor;
	}
	
	public String getTemplateDir() {
		return templateDir;
	}

	public void setTemplateDir(String templateDir) {
		this.templateDir = templateDir;
	}

	public String getTestDir() {
		return testDir;
	}

	public void setTestDir(String testDir) {
		this.testDir = testDir;
	}

	public String getPathDir() {
		return pathDir;
	}

	public void setPathDir(String pathDir) {
		this.pathDir = pathDir;
	}

	public String getLabelmapDir() {
		return labelmapDir;
	}

	public void setLabelmapDir(String labelmapDir) {
		this.labelmapDir = labelmapDir;
	}

	public String getResultDir() {
		return resultDir;
	}

	public void setResultDir(String resultDir) {
		this.resultDir = resultDir;
	}
	
	public String getDebugDir() {
		return this.debugDir;
	}
	
	public void setDebugDir(String debugDir) {
		this.debugDir = debugDir;
	}

	public boolean isAnnotGuard() {
		return annotGuard;
	}

	public void setAnnotGuard(boolean annotGuard) {
		this.annotGuard = annotGuard;
	}

	public boolean isCleanTemplate() {
		return cleanTemplate;
	}

	public void setCleanTemplate(boolean cleanTemplate) {
		this.cleanTemplate = cleanTemplate;
	}

	public boolean isCleanTest() {
		return cleanTest;
	}

	public void setCleanTest(boolean cleanTest) {
		this.cleanTest = cleanTest;
	}
	
	public boolean isDebug() {
		return this.debug;
	}
	
	public void setDebug(boolean debug) {
		this.debug = debug;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("op table path: " + this.opTablePath + "\n");
		sb.append("op code cat id: " + this.opCodeCatId + "\n");
		sb.append("cost table dir: " + this.costTableDir + "\n");
		sb.append("cost limit: " + this.costLimit + "\n");
		sb.append("template dir: " + this.templateDir + "\n");
		sb.append("test dir: " + this.testDir + "\n");
		sb.append("path dir: " + this.pathDir + "\n");
		sb.append("label dir: " + this.labelmapDir + "\n");
		sb.append("result dir: " + this.resultDir + "\n");
		sb.append("debug dir: " + this.debugDir + "\n");
		sb.append("control weigtht: " + this.controlWeight + "\n");
		sb.append("data weigth: " + this.dataWeight + "\n");
		sb.append("precision digit: " + this.precisionDigit + "\n");
		sb.append("parallel factor: " + this.parallelFactor + "\n");
		sb.append("annotation guard: " + this.annotGuard + "\n");
		sb.append("clean template" + this.cleanTemplate + "\n");
		sb.append("clean test: " + this.cleanTest + "\n");
		sb.append("debug: " + this.debug + "\n");
		return sb.toString();
	}
}
