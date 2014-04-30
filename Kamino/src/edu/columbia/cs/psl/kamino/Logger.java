package edu.columbia.cs.psl.kamino;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

public class Logger {
//	private OutputStreamWriter writer;
//	public File outputFile;
//
//	public Logger(File outputFile) {
//		this.outputFile = outputFile;
//		try {
//			this.writer = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(this.outputFile)));
//		} catch (FileNotFoundException e) {
//			e.printStackTrace();
//		}
//	}
//
//	public void record(String output) {
//		try {
//			this.writer.write(output);
//			this.writer.flush();
//		} catch (IOException e) {
//			System.out.println("IOException: Logger.record: output=" + output);
//			closeBuffer();
//			e.printStackTrace();
//		}
//	}

	public static void recordARFF(String output) {
		try {
			File file = new File("/Users/whimsy/Documents/Columbia/research_clones/jvm-clones/Kamino/data/" + Constants.TOMCAT_NAME + ".arff");
			if (file.exists()) {
				PrintWriter printWriter = new PrintWriter(new BufferedWriter(new FileWriter(file, true)));
				printWriter.println(output);
				printWriter.close();
			} else {
				String toARFF = "@RELATION "
				        + Constants.TOMCAT_NAME
				        + "\n\n@ATTRIBUTE method String \n@ATTRIBUTE flow_type {Control, Read, Write}\n@ATTRIBUTE from Numeric\n@ATTRIBUTE to Numeric\n"
				        + "\n@DATA\n";
				OutputStreamWriter static_writer = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(file)));
				static_writer.write(toARFF);
				static_writer.write(output + "\n");
				static_writer.close();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

//	public static void main(String[] args) {
//		Logger.recordARFF("methodName, Control, " + 4 + ", " + 5);
//	}

//	public void closeBuffer() {
//		try {
//			System.out.println("Logger: Buffer closed " + this.outputFile);
//			this.writer.close();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
}
