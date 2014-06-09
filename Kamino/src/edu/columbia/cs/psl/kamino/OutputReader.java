package edu.columbia.cs.psl.kamino;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class OutputReader {

    private Map<String, ArrayList<FlowEntry>> method_entry_map = new HashMap<String, ArrayList<FlowEntry>>();
    private Map<String, Integer> variable_lastFrameID_map = new HashMap<String, Integer>();
    private String filename;

    public OutputReader(String filename) {
        this.filename = filename;
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filename)));
            String logEntry;

            while ((logEntry = reader.readLine()) != null) {
                char flowType = FlowEntry.determineFlowType(logEntry);
                FlowEntry entry = (flowType == Constants.CONTROL) ? new ControlFlowEntry(logEntry) : new DataFlowEntry(logEntry);

                // Set the data fromFrame
                if (flowType != Constants.CONTROL) {
                    // Find where this variable was seen last (if it's been seen before)
                    String key = entry.methodInfo + entry.variableID;
                    int bbFrom = (variable_lastFrameID_map.containsKey(key)) ? variable_lastFrameID_map.get(key) : entry.toFrame;
                    entry.setFromFrame(bbFrom);
                    variable_lastFrameID_map.put(key, entry.toFrame);
                }

                ArrayList<FlowEntry> flowEntryList = new ArrayList<FlowEntry>();
                if (method_entry_map.containsKey(entry.methodInfo)) {
                    flowEntryList = method_entry_map.get(entry.methodInfo);
                    flowEntryList.add(entry);
                } else {
                    flowEntryList.add(entry);
                }
                method_entry_map.put(entry.methodInfo, flowEntryList);
            }
            reader.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(this.filename);
        System.out.println(method_entry_map);
        System.out.println();
    }

    public void findCommonSubstrings(Map<String, ArrayList<FlowEntry>> map, int threshold) {
        System.out.println("threshold: " + threshold);
        ArrayList<String> substrings = new ArrayList<String>();

        System.out.println();
        System.out.println();
        System.out.println();
        for (java.util.Map.Entry<String, ArrayList<FlowEntry>> mapList : map.entrySet()) {
            for (java.util.Map.Entry<String, ArrayList<FlowEntry>> thisList : method_entry_map.entrySet()) {

                // FIXME LAN - update this with new setup
                String commonSubstring = longestCommonSubstring(mapList.getValue().toString(), thisList.getValue().toString());
                if (commonSubstring.length() > threshold) {
                    System.out.println("mapList: " + mapList.toString());
                    System.out.println(this.filename + ": " + thisList.toString());

                    System.out.println("Common String: " + commonSubstring);
                    substrings.add(commonSubstring);
                    System.out.println();
                }
            }
        }

    }

    // From http://en.wikibooks.org/wiki/Algorithm_Implementation/Strings/Longest_common_substring#Java
    private static String longestCommonSubstring(String firstStr, String secondStr) {
        int start = 0;
        int max = 0;
        for (int i = 0; i < firstStr.length(); i++) {
            for (int j = 0; j < secondStr.length(); j++) {
                int x = 0;
                while (firstStr.charAt(i + x) == secondStr.charAt(j + x)) {
                    x++;
                    if (((i + x) >= firstStr.length()) || ((j + x) >= secondStr.length()))
                        break;
                }
                if (x > max) {
                    max = x;
                    start = i;
                }
            }
        }
        return firstStr.substring(start, (start + max));
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
                    } else {
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

    public static void main(String[] args) {
        OutputReader or1 = new OutputReader("data/tomcat-7.0.53_controlDataFlow-newest.output");
        OutputReader or2 = new OutputReader("data/tomcat-8.0.50_controlDataFlow-newest.output");
        or1.findCommonSubstrings(or2.method_entry_map, 208);

    }
}
