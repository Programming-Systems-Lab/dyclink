package edu.columbia.cs.psl.kamino;

import java.io.File;

public class Constants {

	public static final String TOMCAT_8 = "tomcat-8.0.5";
	public static final String TOMCAT_7 = "tomcat-7.0.53";
	
	public static final String TOMCAT_VERSION = TOMCAT_7;
	
    public static final File NO_DATAFLOW_ARFF = new File("/Users/lindsay/Documents/research_clones/jvm-clones/Kamino/data/" + Constants.TOMCAT_VERSION + "_noDatFlow.arff");
    public static final File DATAFLOW_ARFF = new File("/Users/lindsay/Documents/research_clones/jvm-clones/Kamino/data/" + Constants.TOMCAT_VERSION + "_dataFlow.arff");

	public final static char DATA_EDGE = 'D';
	public final static char CONTROL_EDGE = 'C';
	
	public final static char READ = 'R';
	public final static char WRITE = 'W';
	public final static char CONTROL = 'C';
}
