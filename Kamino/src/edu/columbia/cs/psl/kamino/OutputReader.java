package edu.columbia.cs.psl.kamino;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

public class OutputReader {

	public class Entry {
		String methodInfo;
		char controlType;
		int variableID;
		int fromFrame;
		int toFrame;

		public Entry(String entryStr) {
			String[] splitArray = entryStr.split(",");
			this.methodInfo = splitArray[0];
			this.controlType = splitArray[1].charAt(0);
			if (controlType == Constants.CONTROL) {
				this.variableID = Integer.valueOf(splitArray[2]);
			} else {
				this.fromFrame = Integer.valueOf(splitArray[2]);
			}
			this.toFrame = Integer.valueOf(splitArray[3]);
		}
	}

	public OutputReader(String filename) {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filename)));
			String logEntry;

			while ((logEntry = reader.readLine()) != null) {
				Entry entry = new Entry(logEntry);
				System.out.println(logEntry);
				System.out.println(entry.methodInfo);
				System.out.println(entry.controlType);
				System.out.println(entry.variableID);
				System.out.println(entry.fromFrame);
				System.out.println(entry.toFrame);
				System.out.println();
			}
            reader.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
	        e.printStackTrace();
        }
		
	}
	// FIXME LAN - currently reading method control/data flow information,
	// 				need to track the movement between them at the method level in a long string
	//				and keep track of the same version of strings (combine them into one but record all versions)
	
	public static void main(String[] args) {
		new OutputReader("data/tomcat-8.0.5_controlDataFlow.output");
	}
}
