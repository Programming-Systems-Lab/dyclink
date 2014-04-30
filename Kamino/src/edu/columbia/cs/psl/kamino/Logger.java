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
    //    
    //  public void closeBuffer() {
    //      try {
    //          System.out.println("Logger: Buffer closed " + this.outputFile);
    //          this.writer.close();
    //      } catch (IOException e) {
    //          e.printStackTrace();
    //      }
    //  }

    public static void recordARFF(String output) {
        try {
            if (Constants.NO_DATAFLOW_ARFF.exists()) {
                PrintWriter printWriter = new PrintWriter(new BufferedWriter(new FileWriter(Constants.NO_DATAFLOW_ARFF, true)));
                printWriter.println(output);
                printWriter.close();
            } else {
                String toARFF = "@RELATION "
                        + Constants.TOMCAT_NAME
                        + "\n\n@ATTRIBUTE method String \n@ATTRIBUTE flow_type {Control, Read, Write}\n@ATTRIBUTE from Numeric\n@ATTRIBUTE to Numeric\n"
                        + "\n@DATA\n";
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
            
            // FIXME LAN - Assuming that we don't need variable id information for the graphs - might be an incorrect assumption
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (!line.contains("@ATTRIBUTE") && (line.contains("Read") || line.contains("Write"))) {
                    String[] splitLineArray = line.split(", ");
                    String method_name = splitLineArray[0].replace(" ", "");
                    String flow_type = splitLineArray[1].replace(" ", "");
                    int variableID = Integer.valueOf(splitLineArray[2].replace("var", "").replace(" ", ""));
                    int frameID = Integer.valueOf(splitLineArray[3].replace(" ", ""));
                    int lastFrameID = (variable_lastFrame_map.containsKey(variableID)) ? variable_lastFrame_map.get(variableID) : frameID;

                    if (flow_type.equals("Write")) {
                        variable_lastFrame_map.put(variableID, frameID);
                    } 
                    writer.write(method_name + ", " + flow_type + ", " + lastFrameID + ", " + frameID + "\n");
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
        System.out.println("Finished");
    }
}
