package cc.expbase;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import cern.colt.Arrays;

public class DecompilerTest {
		
	public void formethod() {
		int sum = 0;
		for (int i = 0; i < 10; i++) {
			sum += i;
		}
		System.out.println(sum);
	}
	
	public void whilemethod() {
		int sum = 0;
		int i = 0;
		while (i < 10) {
			sum += i;
			i++;
		}
		System.out.println(sum);
	}
	
	public static void main(String[] args) throws Exception {
		CommandLineParser cParser = new DefaultParser();
		
		Options options = new Options();
		options.addOption("h", false, "Help");
		options.addOption("t", false, "Only construct final graphs without mining");
		
		Option templateOption = new Option("template", "The dir of template graphs");
		templateOption.setArgs(1);
		options.addOption(templateOption);
		
		Option testOption = new Option("test", "The dir of test graphs");
		testOption.setArgs(1);
		options.addOption(testOption);
		
		//options.addOption("graphrepo", false, "The dir of all graph repos");
		Option o = new Option("graphrepo", "The dir of all graph repos");
		o.setArgs(Option.UNLIMITED_VALUES);
		options.addOption(o);
		
		CommandLine cLine = cParser.parse(options, args);
		
		if (cLine.hasOption("h")) {
			System.out.println("t: Only construct final graphs without mining");
			System.out.println("template: The dir of template graphs");
			System.out.println("test: The dir of test graphs");
			System.out.println("graphrepo: The dir of all graph repos");
			System.exit(0);
		} 
		
		if (cLine.hasOption("t")) {
			System.out.println("Construction only");
		}
		
		System.out.println("Template: " + cLine.hasOption("template"));
		System.out.println("Test: " + cLine.hasOption("test"));
		
		for (Option tmp: cLine.getOptions()) {
			System.out.println(tmp.getOpt());
			System.out.println(tmp.getValue());
		}
		
		if (cLine.hasOption("template") && cLine.hasOption("test")) {
			System.out.println("Graph locations from command line");
			System.out.println("Templates: " + cLine.getOptionValue("template"));
			System.out.println("Tests: " + cLine.getOptionValue("test"));
		} else if (cLine.hasOption("graphrepo")) {
			System.out.println("Graph repos: " + Arrays.toString(cLine.getOptionValues("graphrepo")));
		}
	}

}
