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

    public class Entry {
        String methodInfo;
        char controlType = '?';
        int frame = -1;

        public Entry(String entryStr) {
            String[] splitArray = entryStr.split(",");
            this.methodInfo = splitArray[0];
            this.controlType = splitArray[1].charAt(0);
            this.frame = Integer.valueOf(splitArray[3]);
        }

        public String toString() {
            return this.methodInfo + "," + this.controlType + this.frame;
        }

        //		String methodInfo;
        //		char controlType = '?';
        //		int variableID = -1;
        //		int fromFrame = -1;
        //		int toFrame = -1;
        //
        //		public Entry(String entryStr) {
        //			String[] splitArray = entryStr.split(",");
        //			this.methodInfo = splitArray[0];
        //			this.controlType = splitArray[1].charAt(0);
        //			if (controlType == Constants.CONTROL) {
        //				this.fromFrame = Integer.valueOf(splitArray[2]);
        //			} else {
        //				this.variableID = Integer.valueOf(splitArray[2]);
        //			}
        //			this.toFrame = Integer.valueOf(splitArray[3]);
        //		}
        //
        //		public String toString() {
        //			String toReturn = this.methodInfo + "," + this.controlType;
        //			if (controlType == Constants.CONTROL) {
        //				toReturn += ",f" + this.fromFrame;
        //			} else {
        //				toReturn += ",v" + this.variableID;
        //			}
        //			return toReturn + ",f" + this.toFrame;
        //		}
    }

    private Map<String, String> method_frame_map = new HashMap<String, String>();
    private String filename;

    //	private Map<String, String> method_ctrl_frame_map = new HashMap<String, String>();
    //	private Map<String, String> method_data_frame_map = new HashMap<String, String>();

    public OutputReader(String filename) {
        this.filename = filename;
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filename)));
            String logEntry;

            while ((logEntry = reader.readLine()) != null) {
                Entry entry = new Entry(logEntry);

                String flow = (method_frame_map.containsKey(entry.methodInfo)) ? method_frame_map.get(entry.methodInfo) : "";
                method_frame_map.put(entry.methodInfo, flow + entry.controlType + entry.frame);

                //				if (entry.controlType == Constants.CONTROL) {
                //					String controlFlow = (method_ctrl_frame_map.containsKey(entry.methodInfo)) ? method_ctrl_frame_map.get(entry.methodInfo) : "";
                //					method_ctrl_frame_map.put(entry.methodInfo, controlFlow + entry.controlType + entry.frame);
                //				} else {
                //					String dataFlow = (method_data_frame_map.containsKey(entry.methodInfo)) ? method_data_frame_map.get(entry.methodInfo) : "";
                //					method_data_frame_map.put(entry.methodInfo, dataFlow + entry.controlType + entry.frame);
                //				}
            }
            reader.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(this.filename + ": method_frame_map");
        System.out.println(method_frame_map);
        System.out.println();
    }

    public void findCommonSubstrings(Map<String, String> map, int threshold) {
        ArrayList<String> substrings = new ArrayList<String>();

        for (java.util.Map.Entry<String, String> mapList : map.entrySet()) {
            for (java.util.Map.Entry<String, String> thisList : method_frame_map.entrySet()) {
                //				if (!mapList.equals(thisList)) {
                // TODO LAN - getting both versions (aka 1&2 and 2&1)
                String commonSubstring = longestCommonSubstring(mapList.getValue().toString(), thisList.getValue().toString());
                if (commonSubstring.length() >= threshold) {
                    System.out.println("mapList: " + mapList.toString());
                    System.out.println(this.filename + ": " + thisList.toString());

                    System.out.println("Common String: " + commonSubstring);
                    substrings.add(commonSubstring);
                    System.out.println();
                }
                //				}
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

    // FIXME LAN - need to add TO and FROM frames; exceptions; record subroutine calls
    public static void main(String[] args) {
        OutputReader or1 = new OutputReader("data/tomcat-8.0.5_controlDataFlow-newest.output");
        OutputReader or2 = new OutputReader("data/tomcat-7.0.53_controlDataFlow-newest.output");
        or1.findCommonSubstrings(or2.method_frame_map, 4);

    }
}
