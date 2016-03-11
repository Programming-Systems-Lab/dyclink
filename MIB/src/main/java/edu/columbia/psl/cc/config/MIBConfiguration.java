package edu.columbia.psl.cc.config;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import com.google.gson.reflect.TypeToken;

import edu.columbia.psl.cc.util.GsonManager;

public class MIBConfiguration {
	
	public static int INST_DATA_DEP = 0;
	
	public static int WRITE_DATA_DEP = 1;
	
	public static int CONTR_DEP = 2;
	
	public static int INST_STRAT = 0;
	
	public static int SUBSUB_STRAT = 1;
	
	public static int SUB_STRAT = 2;
	
	public static int CAT_STRAT = 3;
		
	public static int GRAPH_DIR = 0;
	
	public static int LABEL_MAP_DIR = 1;
	
	public static int CACHE_DIR = 2;
	
	private List<String> excludeClass;
	
	private List<String> testPaths;
	
	private String opTablePath;
	
	private String opCodeCatId;
	
	private String graphDir;
		
	private String labelmapDir;
	
	private String resultDir;
	
	private String debugDir;
	
	private String cacheDir;
	
	private double controlWeight;
	
	private double instDataWeight;
	
	private double writeDataWeight;
	
	private int precisionDigit;
	
	private int instThreshold;
	
	private int instLimit;
	
	private int callThreshold;
	
	private double pgAlpha;
	
	private int pgMaxIter;
	
	private double pgEpsilon;
	
	//Static threshold, before subgraph matching, decide if subgraph matching should conduct
	private double staticThreshold;
	
	//Dynamic threshold after subgraph matching, decide if hotzone should be selected
	private double simThreshold;
	
	private int assignmentThreshold;
	
	//0: inst, 1: subsubcat, 2: subcat, 3: cat
	private int simStrategy;
	
	private int testMethodThresh;
	
	private int threadInit;
	
	private HashMap<Integer, Integer> threadMethodIdxRecord;
	
	private boolean annotGuard;
	
	private boolean fieldTrack;
	
	private boolean debug;
	
	private boolean exclSpec;
	
	private boolean exclPkg;
	
	private boolean reduceGraph;
		
	private boolean nativeClass;
	
	private String dburl;
	
	private String dbusername;
	
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
	
	public static MIBConfiguration reloadInstance() {
		instance = null;
		return getInstance();
	}
	
	public static void main(String args[]) {
		System.out.println(MIBConfiguration.getInstance());
	}
		
	public List<String> getExcludeClass() {
		return this.excludeClass;
	}
	
	public void setExcludeClass(List<String> excludeClass) {
		this.excludeClass = excludeClass;
	}
	
	public List<String> getTestPaths() {
		return this.testPaths;
	}
	
	public void setTestPaths(List<String> testPaths) {
		this.testPaths = testPaths;
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

	public double getControlWeight() {
		return controlWeight;
	}

	public void setControlWeight(double controlWeight) {
		this.controlWeight = controlWeight;
	}

	public double getInstDataWeight() {
		return instDataWeight;
	}

	public void setInstDataWeight(double instDataWeight) {
		this.instDataWeight = instDataWeight;
	}
	
	public double getWriteDataWeight() {
		return this.writeDataWeight;
	}
	
	public void setWriteDataWeight(double writeDataWeight) {
		this.writeDataWeight = writeDataWeight;
	}
	
	public int getPrecisionDigit() {
		return precisionDigit;
	}

	public void setPrecisionDigit(int precisionDigit) {
		this.precisionDigit = precisionDigit;
	}
	
	public int getInstThreshold() {
		return this.instThreshold;
	}
	
	public void setInstThreshold(int instThreshold) {
		this.instThreshold = instThreshold;
	}
	
	public int getCallThreshold() {
		return this.callThreshold;
	}
	
	public void setCallThreshold(int callThreshold) {
		this.callThreshold = callThreshold;
	}
	
	public int getInstLimit() {
		return this.instLimit;
	}
	
	public void setInstLimit(int instLimit) {
		this.instLimit = instLimit;
	}
	
	public void setTestMethodThresh(int testMethodThresh) {
		this.testMethodThresh = testMethodThresh;
	}
	
	public int getTestMethodThresh() {
		return this.testMethodThresh;
	}
	
	public void setThreadInit(int threadInit) {
		this.threadInit = threadInit;
	}
	
	public int getThreadInit() {
		return this.threadInit;
	}
	
	public void setThreadMethodIdxRecord(HashMap<Integer, Integer> threadMethodIdxRecord) {
		this.threadMethodIdxRecord = threadMethodIdxRecord;
	}
	
	public HashMap<Integer, Integer> getThreadMethodIdxRecord() {
		return this.threadMethodIdxRecord;
	}
	
	public double getPgAlpha() {
		return this.pgAlpha;
	}
	
	public void segPgAlpha(double pgAlpha) {
		this.pgAlpha = pgAlpha;
	}
	
	public void setPgMaxIter(int pgMaxIter) {
		this.pgMaxIter = pgMaxIter;
	}
	
	public int getPgMaxIter() {
		return this.pgMaxIter;
	}
	
	public void setPgEpsilon(double pgEpsilon) {
		this.pgEpsilon = pgEpsilon;
	}
	
	public double getPgEpsilon() {
		return this.pgEpsilon;
	}
		
	public void setStaticThreshold(double staticThreshold) {
		this.staticThreshold = staticThreshold;
	}
	
	public double getStaticThreshold() {
		return this.staticThreshold;
	}
	
	public void setSimThreshold(double simThreshold) {
		this.simThreshold = simThreshold;
	}
	
	public double getSimThreshold() {
		return this.simThreshold;
	}
	
	public void setAssignmentThreshold(int assignmentThreshold) {
		this.assignmentThreshold = assignmentThreshold;
	}
	
	public int getAssignmentThreshold() {
		return this.assignmentThreshold;
	}
	
	public void setSimStrategy(int simStrategy) {
		this.simStrategy = simStrategy;
	}
	
	public int getSimStrategy() {
		return this.simStrategy;
	}
	
	public void setGraphDir(String graphDir) {
		this.graphDir = graphDir;
	}
	
	public String getGraphDir() {
		return this.graphDir;
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
	
	public String getCacheDir() {
		return this.cacheDir;
	}
	
	public void setCacheDir(String cacheDir) {
		this.cacheDir = cacheDir;
	}

	public boolean isAnnotGuard() {
		return annotGuard;
	}

	public void setAnnotGuard(boolean annotGuard) {
		this.annotGuard = annotGuard;
	}
	
	public boolean isFieldTrack() {
		return this.fieldTrack;
	}
	
	public void setFieldTrack(boolean fieldTrack) {
		this.fieldTrack = fieldTrack;
	}
		
	public boolean isDebug() {
		return this.debug;
	}
	
	public void setDebug(boolean debug) {
		this.debug = debug;
	}
	
	public boolean isExclSpec() {
		return this.exclSpec;
	}
	
	public void setExclSpec(boolean exclSpec) {
		this.exclSpec = exclSpec;
	}
	
	public boolean isExclPkg() {
		return this.exclPkg;
	}
	
	public void setExclPkg(boolean exclPkg) {
		this.exclPkg = exclPkg;
	}
	
	public boolean isReduceGraph() {
		return this.reduceGraph;
	}
	
	public void setReduceGraph(boolean reduceGraph) {
		this.reduceGraph = reduceGraph;
	}
		
	public boolean isNativeClass() {
		return this.nativeClass;
	}
	
	public void setNativeClass(boolean nativeClass) {
		this.nativeClass = nativeClass;
	}
	
	public void setDburl(String dburl) {
		this.dburl = dburl;
	}
	
	public String getDburl() {
		return this.dburl;
	}
	
	public void setDbusername(String dbusername) {
		this.dbusername = dbusername;
	}
	
	public String getDbusername() {
		return this.dbusername;
	}
		
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("op table path: " + this.opTablePath + "\n");
		sb.append("op code cat id: " + this.opCodeCatId + "\n");
		sb.append("graph dir: " + this.graphDir + "\n");
		sb.append("label dir: " + this.labelmapDir + "\n");
		sb.append("result dir: " + this.resultDir + "\n");
		sb.append("debug dir: " + this.debugDir + "\n");
		sb.append("control weigtht: " + this.controlWeight + "\n");
		sb.append("inst data weigtht: " + this.instDataWeight + "\n");
		sb.append("write data weight: " + this.writeDataWeight + "\n");
		sb.append("precision digit: " + this.precisionDigit + "\n");
		sb.append("minimum inst number: " + this.instThreshold + "\n");
		sb.append("maximum call number: " + this.callThreshold + "\n");
		sb.append("inst selection number: " + this.instLimit + "\n");
		sb.append("test method threshold: " + this.testMethodThresh + "\n");
		sb.append("alpha of PageRank: " + this.pgAlpha + "\n");
		sb.append("max iteration of PageRank: " + this.pgMaxIter + "\n");
		sb.append("epsilon of PageRank: " + this.pgEpsilon + "\n");
		sb.append("instruction cat lavel: " + this.simStrategy + "\n");
		sb.append("assignment threshold: " + this.assignmentThreshold + "\n");
		sb.append("static threshold: " + this.staticThreshold + "\n");
		sb.append("dynamic threshod: " + this.simThreshold + "\n");
		sb.append("annotation guard: " + this.annotGuard + "\n");
		sb.append("field track: " + this.fieldTrack + "\n");
		sb.append("reduce graph: " + this.reduceGraph + "\n");
		sb.append("native classification: " + this.nativeClass + "\n");
		sb.append("debug: " + this.debug + "\n");
		sb.append("exclude spec. methods: " + this.exclSpec + "\n");
		sb.append("exclude pkg methods: " + this.exclPkg + "\n");
		return sb.toString();
	}
}
