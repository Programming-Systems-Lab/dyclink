package edu.columbia.cs.psl.kamino;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class Logger {
	
	public static void recordOutput(String output) {
		System.out.println(output);
		
		try {
            if (Constants.OUTPUT.exists()) {
                PrintWriter printWriter = new PrintWriter(new BufferedWriter(new FileWriter(Constants.OUTPUT, true)));
                printWriter.println(output);
                printWriter.close();
            } else {
                OutputStreamWriter static_writer = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(Constants.OUTPUT)));
                static_writer.write(output + "\n");
                static_writer.close();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
	}

    public static void recordARFF(String output) {
        try {
            if (Constants.NO_DATAFLOW_ARFF.exists()) {
                PrintWriter printWriter = new PrintWriter(new BufferedWriter(new FileWriter(Constants.NO_DATAFLOW_ARFF, true)));
                printWriter.println(output);
                printWriter.close();
            } else {
                String toARFF = "@RELATION "
                        + Constants.TOMCAT_VERSION
                        + "\n\n@ATTRIBUTE method String \n@ATTRIBUTE flowType {C, R, W} \n@ATTRIBUTE variableID Numeric \n@ATTRIBUTE fromFrame Numeric \n@ATTRIBUTE toFrame Numeric \n@ATTRIBUTE frameDistance Numeric"
                        + " \n\n@DATA\n";
                OutputStreamWriter static_writer = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(Constants.NO_DATAFLOW_ARFF)));
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

    public void updateARFFDataFlow() {
        Map<Integer, Integer> variable_lastFrame_map = new HashMap<Integer, Integer>();
        try {
            OutputStreamWriter writer = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(Constants.DATAFLOW_ARFF)));
            BufferedReader bufferedReader = new BufferedReader(new FileReader(Constants.NO_DATAFLOW_ARFF));

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (!line.contains("@") && line.length() > 0) {
                    String[] splitLineArray = line.split(",");
                    char flow_type = splitLineArray[1].toCharArray()[0];
                    
                    if (flow_type == Constants.READ || flow_type == Constants.WRITE) {
                        String method_name = splitLineArray[0];
                        int variableID = Integer.valueOf(splitLineArray[2]);
                        int toFrameID = Integer.valueOf(splitLineArray[4]);
                        int fromFrameID = (variable_lastFrame_map.containsKey(variableID)) ? variable_lastFrame_map.get(variableID) : toFrameID;

                        if (flow_type == Constants.WRITE) {
                            variable_lastFrame_map.put(variableID, toFrameID);
                        }
                        writer.write(method_name + "," + flow_type + "," + variableID + "," + fromFrameID + "," + toFrameID + "," + Math.abs(toFrameID - fromFrameID) + "\n");
                    } else {
                        writer.write(line + "\n");
                    }
                } else {
                    writer.write(line + "\n");
                }
            }
            writer.close();
            bufferedReader.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Logger log = new Logger();
        log.updateARFFDataFlow();
        System.out.println("Finished: " + Constants.DATAFLOW_ARFF.getAbsolutePath());
    }
}
