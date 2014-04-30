package edu.columbia.cs.psl.kamino;

import java.io.File;

public class Constants {

	public static final String TOMCAT_NAME = "tomcat-8.0.5";
	
    public static final File NO_DATAFLOW_ARFF = new File("/Users/lindsay/Documents/research_clones/jvm-clones/Kamino/data/" + Constants.TOMCAT_NAME + "_noDatFlow.arff");
    public static final File DATAFLOW_ARFF = new File("/Users/lindsay/Documents/research_clones/jvm-clones/Kamino/data/" + Constants.TOMCAT_NAME + "_dataFlow.arff");

	public final static char DATA_EDGE = 'D';
	public final static char CONTROL_EDGE = 'C';
}
