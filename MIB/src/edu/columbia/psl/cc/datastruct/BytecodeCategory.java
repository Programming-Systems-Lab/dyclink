package edu.columbia.psl.cc.datastruct;

import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import edu.columbia.psl.cc.config.MIBConfiguration;
import edu.columbia.psl.cc.pojo.OpcodeObj;
import edu.columbia.psl.cc.premain.MIBDriver;

public class BytecodeCategory {
	
	//private static String opTablePath = "opcodes/opcode_table.txt";
		
	private static HashMap<Integer, String> opcodeCategory = new HashMap<Integer, String>();
	
	private static HashMap<Integer, OpcodeObj> opcodeTable = new HashMap<Integer, OpcodeObj>();
	
	private static HashMap<Integer, HashSet<Integer>> catMap = new HashMap<Integer, HashSet<Integer>>();
	
	private static HashSet<Integer> replacedOps = new HashSet<Integer>();
	
	static {
		loadOpcodeTable();
		loadOpcodeCategory();
		
		replacedOps.add(Opcodes.GETFIELD);
		replacedOps.add(Opcodes.GETSTATIC);
		replacedOps.add(Opcodes.AALOAD);
	}
	
	private static void loadOpcodeCategory() {		
		File f = new File(MIBConfiguration.getInstance().getOpCodeCatId());
		
		if (!f.exists()) {
			System.err.println("Opcode category ID table does not exist");
			return ;
		}
		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(f));
			
			String line;
			while ((line = br.readLine()) != null) {
				String[] info = line.split(",");
				opcodeCategory.put(Integer.valueOf(info[0]), info[1]);
			}
			br.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	public static String getOpcodeCategoryName(int catId) {
		return opcodeCategory.get(catId);
	}
	
	public static List<String> processOpTableElement(String rawContent) {
		List<String> ret = new ArrayList<String>();
		
		if (rawContent.equals("no")) {
			return ret;
		}
		
		String[] contentArray = rawContent.split(":");
		for (String s: contentArray) {
			ret.add(s);
		}
		return ret;
	}
	
	private static void loadOpcodeTable() {
		File f = new File(MIBConfiguration.getInstance().getOpTablePath());
		if (!f.exists()) {
			System.err.println("Find no opcode table information");
			return ;
		}
		
		try {
			BufferedReader br = new BufferedReader(new FileReader(f));
			
			String line = null;
			while ((line = br.readLine()) != null) {
				String[] info = line.split(",");
				//info[2] is hex, we can have it by opcode
				
				int catId = Integer.valueOf(info[0]);
				int subCatId = Integer.valueOf(info[1]);
				int subSubCatId = Integer.valueOf(info[2]);
				int opcode = Integer.valueOf(info[3]);
				
				OpcodeObj oo = new OpcodeObj();
				oo.setCatId(catId);
				oo.setSubCatId(subCatId);
				oo.setSubSubCatId(subSubCatId);
				oo.setOpcode(opcode);
				oo.setInstruction(info[5]);
				
				//Process input
				List<String> inList = processOpTableElement(info[6]);
				oo.setInList(inList);
				
				//Process output
				List<String> outList = processOpTableElement(info[7]);
				oo.setOutList(outList);
				
				opcodeTable.put(opcode, oo);
				
				if (catMap.keySet().contains(catId)) {
					catMap.get(catId).add(opcode);
				} else {
					HashSet<Integer> catSet = new HashSet<Integer>();
					catSet.add(opcode);
					catMap.put(catId, catSet);
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	public static HashMap<Integer, OpcodeObj> getOpcodeTable() {
		return opcodeTable; 
	}
	
	public static OpcodeObj getOpcodeObj(int opcode) {
		return opcodeTable.get(opcode);
	}
	
	public static HashSet<Integer> getOpcodeSetByCat(int catId) {
		return catMap.get(catId);
	}
	
	public static int getSetIdByOpcode(int opcode) {
		for (Integer i: catMap.keySet()) {
			HashSet<Integer> ops = catMap.get(i);
			if (ops.contains(opcode)) {
				return i;
			}
		}
		return -1;
	}
	
	public static HashMap<Integer, String> getOpcodeCategory() {
		return opcodeCategory;
	}
	
	public static HashSet<Integer> writeCategory() {
		HashSet<Integer> ret = new HashSet<Integer>();
		ret.add(3);
		return ret;
	}
	
	public static HashSet<Integer> readCategory() {
		HashSet<Integer> ret = new HashSet<Integer>();
		ret.add(1);
		return ret;
	}
	
	public static HashSet<Integer> writeFieldCategory() {
		HashSet<Integer> ret = new HashSet<Integer>();
		ret.add(15);
		return ret;
	}
	
	public static HashSet<Integer> readFieldCategory() {
		HashSet<Integer> ret = new HashSet<Integer>();
		ret.add(14);
		return ret;
	}
	
	public static HashSet<Integer> controlCategory() {
		HashSet<Integer> ret = new HashSet<Integer>();
		ret.add(13);
		return ret;
	}
	
	/**
	 * Include dup and swap
	 * @return
	 */
	public static HashSet<Integer> dupCategory() {
		HashSet<Integer> ret = new HashSet<Integer>();
		ret.add(6);
		ret.add(7);
		return ret;
	}
	
	public static HashSet<Integer> staticMethodOps() {
		HashSet<Integer> ret = new HashSet<Integer>();
		ret.add(184);
		ret.add(186);
		return ret;
	}
	
	public static HashSet<Integer> returnOps() {
		HashSet<Integer> ret = new HashSet<Integer>();
		for (int i = 172; i <= 177; i++) {
			ret.add(i);
		}
		return ret;
	}
	
	public static HashSet<Integer> objMethodOps() {
		HashSet<Integer> ret = new HashSet<Integer>();
		ret.add(182);
		ret.add(183);
		ret.add(185);
		return ret;
	}
	
	public static HashSet<Integer> methodOps() {
		HashSet<Integer> allMethods = new HashSet<Integer>();
		allMethods.addAll(objMethodOps());
		allMethods.addAll(staticMethodOps());
		return allMethods;
	}
	
	public static HashSet<Integer> replacedOps() {
		return replacedOps;
	}
	
	public static HashSet<Integer> asmPrimitiveSort() {
		HashSet<Integer> ret = new HashSet<Integer>();
		ret.add(Type.BOOLEAN);
		ret.add(Type.BYTE);
		ret.add(Type.CHAR);
		ret.add(Type.DOUBLE);
		ret.add(Type.FLOAT);
		ret.add(Type.INT);
		ret.add(Type.LONG);
		ret.add(Type.SHORT);
		ret.add(Type.VOID);
		return ret;
	}
	
	public static Integer asmObjSort() {
		return Type.OBJECT;
	}
	
	public static void main(String[] args) {
		loadOpcodeTable();
		for (int opcode: opcodeTable.keySet()) {
			OpcodeObj oo = opcodeTable.get(opcode);
			System.out.println(oo.getCatId() + " " + oo.getSubCatId() + " " + oo.getSubSubCatId() + " " + " " + oo.getOpcode() + " " + oo.getInstruction());
		}
	}
	
}
