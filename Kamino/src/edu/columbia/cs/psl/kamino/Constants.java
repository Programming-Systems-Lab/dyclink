package edu.columbia.cs.psl.kamino;

import java.io.File;

public class Constants {

	public final static String CLASS_NAME = "ByteCodeTest";
    public final static String INST_CLASS_FILE = "inst/BytecodeTest.class";
    
    public final static String BBFLOW_OUTPUT_FILE = "data/ByteCodeTest.output";
    public final static Logger logger = new Logger(new File(Constants.BBFLOW_OUTPUT_FILE));

    public final static char DATA_EDGE = 'D';
    public final static char CONTROL_EDGE = 'C';
    

}
