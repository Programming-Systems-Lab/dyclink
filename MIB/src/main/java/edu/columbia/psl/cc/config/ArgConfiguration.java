package edu.columbia.psl.cc.config;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

public class ArgConfiguration {
	
	public final static String CONSTRUCT = "construct";
	
	public final static String CONSTRUCT_DES = "Only construct final graphs without mining";
	
	public final static String TARGET = "target";
	
	public final static String TARGET_DES = "The dir of template graphs";
	
	public final static String TEST = "test";
	
	public final static String TEST_DES  = "The dir of test graphs";
	
	/*public final static String GREPO = "graphrepo";
	
	public final static String GREPO_DES = "The dir of all graph repos";*/
	
	public final static String DB_PW = "dbpw";
	
	public final static String DB_PW_DES = "database password";
	
	private final static Options options = new Options();
	
	static {
		options.addOption(CONSTRUCT, false, CONSTRUCT_DES);
		
		Option targetOption = new Option(TARGET, TARGET_DES);
		targetOption.setRequired(true);
		targetOption.setArgs(1);
		options.addOption(targetOption);
		
		Option testOption = new Option(TEST, TEST_DES);
		testOption.setRequired(false);
		testOption.setArgs(1);
		options.addOption(testOption);
		
		/*Option graphrepo = new Option(GREPO, GREPO_DES);
		graphrepo.setRequired(false);
		graphrepo.setArgs(Option.UNLIMITED_VALUES);
		options.addOption(graphrepo);*/
		
		Option dbpw = new Option(DB_PW, DB_PW_DES);
		dbpw.setRequired(false);
		dbpw.setArgs(1);
		options.addOption(dbpw);
	}
	
	public static Options getOptions() {
		return options;
	}

}
