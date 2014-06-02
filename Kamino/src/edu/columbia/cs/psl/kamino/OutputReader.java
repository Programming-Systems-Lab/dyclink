package edu.columbia.cs.psl.kamino;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class OutputReader {

    public class Entry {
        String methodInfo;
        char controlType = '?';
        int variableID = -1;
        int fromFrame = -1;
        int toFrame = -1;

        public Entry(String entryStr) {
            String[] splitArray = entryStr.split(",");
            this.methodInfo = splitArray[0];
            this.controlType = splitArray[1].charAt(0);
            if (controlType == Constants.CONTROL) {
                this.fromFrame = Integer.valueOf(splitArray[2]);
            } else {
                this.variableID = Integer.valueOf(splitArray[2]);
            }
            this.toFrame = Integer.valueOf(splitArray[3]);
        }

        public String toString() {
            String toReturn = this.methodInfo + "," + this.controlType;
            if (controlType == Constants.CONTROL) {
                toReturn += ",f" + this.fromFrame;
            } else {
                toReturn += ",v" + this.variableID;
            }
            return toReturn + ",f" + this.toFrame;
        }
    }

    private Map<String, ArrayList<Entry>> method_entry_map = new HashMap<String, ArrayList<Entry>>();

    public OutputReader(String filename) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filename)));
            String logEntry;

            while ((logEntry = reader.readLine()) != null) {
                Entry entry = new Entry(logEntry);

                ArrayList<Entry> entryList = (method_entry_map.containsKey(entry.methodInfo)) ? method_entry_map.get(entry.methodInfo) : new ArrayList<Entry>();
                entryList.add(entry);
                method_entry_map.put(entry.methodInfo, entryList);

                System.out.println(logEntry);
                System.out.println(entry);
                System.out.println();
            }
            reader.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        test();
    }

    public void test() {
        for (ArrayList<Entry> entryList : method_entry_map.values()) {
            // Find the 
        }
    }

    // From http://en.wikibooks.org/wiki/Algorithm_Implementation/Strings/Longest_common_substring#Java
    public static int longestSubstring(String first, String second) {
        if (first == null || second == null || first.length() == 0 || second.length() == 0) {
            return 0;
        }

        int maxLen = 0;
        int fl = first.length();
        int sl = second.length();
        int[][] table = new int[fl + 1][sl + 1];

        for (int s = 0; s <= sl; s++)
            table[0][s] = 0;
        for (int f = 0; f <= fl; f++)
            table[f][0] = 0;

        for (int i = 1; i <= fl; i++) {
            for (int j = 1; j <= sl; j++) {
                if (first.charAt(i - 1) == second.charAt(j - 1)) {
                    if (i == 1 || j == 1) {
                        table[i][j] = 1;
                    }
                    else {
                        table[i][j] = table[i - 1][j - 1] + 1;
                    }
                    if (table[i][j] > maxLen) {
                        maxLen = table[i][j];
                    }
                }
            }
        }
        return maxLen;
    }

    // FIXME LAN - currently reading method control/data flow information,
    // 				need to track the movement between them at the method level in a long string
    //				and keep track of the same version of strings (combine them into one but record all versions)

    public static void main(String[] args) {
        new OutputReader("data/tomcat-8.0.5_controlDataFlow.output");
    }
}
