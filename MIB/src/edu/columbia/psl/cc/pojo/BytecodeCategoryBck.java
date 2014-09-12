package edu.columbia.psl.cc.pojo;

import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.util.HashMap;
import java.util.HashSet;

import org.objectweb.asm.Opcodes;

public class BytecodeCategoryBck {
	
	//private static String opTablePath = "opcodes/opcode_table.txt";
	
	private static String opTablePath = "opcodes/opcode_cats.csv";
	
	private static String opCodeCatId = "opcodes/opcode_ids.csv";
	
	private static HashMap<Integer, String> opcodeCategory = new HashMap<Integer, String>();
	
	private static HashMap<Integer, OpcodeObj> opcodeTable = new HashMap<Integer, OpcodeObj>();
	
	private static HashSet<Integer> localSet = new HashSet<Integer>();
	
	private static HashSet<Integer> stackSet = new HashSet<Integer>();
	
	private static HashSet<Integer> constSet = new HashSet<Integer>();
	
	private static HashSet<Integer> arithmeticSet = new HashSet<Integer>();
	
	private static HashSet<Integer> castSet = new HashSet<Integer>();
	
	private static HashSet<Integer> objectSet = new HashSet<Integer>();
	
	private static HashSet<Integer> fieldSet = new HashSet<Integer>();
	
	private static HashSet<Integer> methodSet = new HashSet<Integer>();
	
	private static HashSet<Integer> arraySet = new HashSet<Integer>();
	
	private static HashSet<Integer> jumpSet = new HashSet<Integer>();
	
	private static HashSet<Integer> returnSet = new HashSet<Integer>();
	
	static {
		loadOpcodeTable();
		loadOpcodeCategory();
		
		loadLocalSet();
		loadConstSet();
		loadConstSet();
		loadArithmeticSet();
		loadCastSet();
		loadObjectSet();
		loadFieldSet();
		loadMethodSet();
		loadArraySet();
		loadJumpSet();
		loadReturnSet();
	}
	
	private static void loadOpcodeCategory() {		
		File f = new File(opCodeCatId);
		
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
	
	private static void loadOpcodeTable() {
		File f = new File(opTablePath);
		if (!f.exists()) {
			System.err.println("Find no opcode table information");
			return ;
		}
		
		try {
			BufferedReader br = new BufferedReader(new FileReader(f));
			
			String line = null;
			while ((line = br.readLine()) != null) {
				String[] info = line.split(",");
				//info[1] is hex, we can have it by opcode
				OpcodeObj oo = new OpcodeObj();
				oo.setCatId(Integer.valueOf(info[0]));
				int opcode = Integer.valueOf(info[2]);
				oo.setOpcode(opcode);
				oo.setInstruction(info[3]);
				opcodeTable.put(opcode, oo);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	public static OpcodeObj getOpcodeObj(int opcode) {
		return opcodeTable.get(opcode);
	}
	
	/**
	 * 0 - 20
	 */
	private static void loadLocalSet() {
		for (int i = 0 ; i <= 20; i++) {}
	}
	
	public static HashSet<Integer> getLocalSet() {
		return localSet;
	}
	
	private static void loadStackSet() {
		stackSet.add(Opcodes.POP);
		stackSet.add(Opcodes.POP2);
		stackSet.add(Opcodes.DUP);
		stackSet.add(Opcodes.DUP_X1);
		stackSet.add(Opcodes.DUP_X2);
		stackSet.add(Opcodes.DUP2);
		stackSet.add(Opcodes.DUP2_X1);
		stackSet.add(Opcodes.DUP2_X2);
		stackSet.add(Opcodes.SWAP);
	}
	
	public static HashSet<Integer> getStackSet() {
		return stackSet;
	}
	
	private static void loadConstSet() {
		constSet.add(Opcodes.ACONST_NULL);
		constSet.add(Opcodes.DCONST_0);
		constSet.add(Opcodes.DCONST_1);
		constSet.add(Opcodes.FCONST_0);
		constSet.add(Opcodes.FCONST_1);
		constSet.add(Opcodes.FCONST_2);
		constSet.add(Opcodes.ICONST_M1);
		constSet.add(Opcodes.ICONST_0);
		constSet.add(Opcodes.ICONST_1);
		constSet.add(Opcodes.ICONST_2);
		constSet.add(Opcodes.ICONST_3);
		constSet.add(Opcodes.ICONST_4);
		constSet.add(Opcodes.ICONST_5);
		constSet.add(Opcodes.LCONST_0);
		constSet.add(Opcodes.LCONST_1);
		
		constSet.add(Opcodes.BIPUSH);
		constSet.add(Opcodes.LDC);
		constSet.add(Opcodes.SIPUSH);
	}
	
	public static HashSet<Integer> getConstSet() {
		return constSet;
	}
	
	private static void loadArithmeticSet() {
		arithmeticSet.add(Opcodes.DADD);
		arithmeticSet.add(Opcodes.FADD);
		arithmeticSet.add(Opcodes.IADD);
		arithmeticSet.add(Opcodes.LADD);
		arithmeticSet.add(Opcodes.DSUB);
		arithmeticSet.add(Opcodes.FSUB);
		arithmeticSet.add(Opcodes.ISUB);
		arithmeticSet.add(Opcodes.LSUB);
		arithmeticSet.add(Opcodes.DMUL);
		arithmeticSet.add(Opcodes.FMUL);
		arithmeticSet.add(Opcodes.IMUL);
		arithmeticSet.add(Opcodes.LMUL);
		arithmeticSet.add(Opcodes.DDIV);
		arithmeticSet.add(Opcodes.FDIV);
		arithmeticSet.add(Opcodes.IDIV);
		arithmeticSet.add(Opcodes.LDIV);
		arithmeticSet.add(Opcodes.DREM);
		arithmeticSet.add(Opcodes.FREM);
		arithmeticSet.add(Opcodes.IREM);
		arithmeticSet.add(Opcodes.LREM);
		
		arithmeticSet.add(Opcodes.ISHL);
		arithmeticSet.add(Opcodes.ISHR);
		arithmeticSet.add(Opcodes.LSHL);
		arithmeticSet.add(Opcodes.LSHR);
		
		arithmeticSet.add(Opcodes.IUSHR);
		arithmeticSet.add(Opcodes.LUSHR);
		
		arithmeticSet.add(Opcodes.IOR);
		arithmeticSet.add(Opcodes.LOR);
		
		arithmeticSet.add(Opcodes.IAND);
		arithmeticSet.add(Opcodes.LAND);
		
		arithmeticSet.add(Opcodes.IXOR);
		arithmeticSet.add(Opcodes.LXOR);
		
		arithmeticSet.add(Opcodes.INEG);
		arithmeticSet.add(Opcodes.LNEG);
		arithmeticSet.add(Opcodes.FNEG);
		arithmeticSet.add(Opcodes.DNEG);
		
		arithmeticSet.add(Opcodes.LCMP);
		arithmeticSet.add(Opcodes.FCMPL);
		arithmeticSet.add(Opcodes.FCMPG);
		arithmeticSet.add(Opcodes.DCMPL);
		arithmeticSet.add(Opcodes.DCMPG);
	}
	
	public static HashSet<Integer> getArithmeticSet() {
		return arithmeticSet;
	}
	
	private static void loadCastSet() {
		castSet.add(Opcodes.I2B);
		castSet.add(Opcodes.I2C);
		castSet.add(Opcodes.I2D);
		castSet.add(Opcodes.I2F);
		castSet.add(Opcodes.I2L);
		castSet.add(Opcodes.I2S);
		
		castSet.add(Opcodes.F2D);
		castSet.add(Opcodes.F2I);
		castSet.add(Opcodes.F2L);
		
		castSet.add(Opcodes.L2D);
		castSet.add(Opcodes.L2F);
		castSet.add(Opcodes.L2I);
		
		castSet.add(Opcodes.D2F);
		castSet.add(Opcodes.D2I);
		castSet.add(Opcodes.D2L);
		
		castSet.add(Opcodes.CHECKCAST);
	}
	
	public static HashSet<Integer> getCastSet() {
		return castSet;
	}
	
	private static void loadObjectSet() {
		objectSet.add(Opcodes.MULTIANEWARRAY);
		objectSet.add(Opcodes.NEW);
		objectSet.add(Opcodes.NEWARRAY);
		objectSet.add(Opcodes.ANEWARRAY);
		objectSet.add(Opcodes.INSTANCEOF);
	}
	
	public static HashSet<Integer> getObjectSet() {
		return objectSet;
	}
	
	private static void loadFieldSet() {
		fieldSet.add(Opcodes.GETFIELD);
		fieldSet.add(Opcodes.PUTFIELD);
		
		fieldSet.add(Opcodes.GETSTATIC);
		fieldSet.add(Opcodes.PUTSTATIC);
	}
	
	public static HashSet<Integer> getFieldSet() {
		return fieldSet;
	}
	
	private static void loadMethodSet() {
		methodSet.add(Opcodes.INVOKEDYNAMIC);
		methodSet.add(Opcodes.INVOKEINTERFACE);
		methodSet.add(Opcodes.INVOKESPECIAL);
		methodSet.add(Opcodes.INVOKESTATIC);
		methodSet.add(Opcodes.INVOKEVIRTUAL);
	}
	
	public static HashSet<Integer> getMethodSet() {
		return methodSet;
	}
	
	private static void loadArraySet() {
		arraySet.add(Opcodes.IALOAD);
		arraySet.add(Opcodes.LALOAD);
		arraySet.add(Opcodes.FALOAD);
		arraySet.add(Opcodes.DALOAD);
		arraySet.add(Opcodes.AALOAD);
		arraySet.add(Opcodes.BALOAD);
		arraySet.add(Opcodes.CALOAD);
		arraySet.add(Opcodes.SALOAD);
		
		arraySet.add(Opcodes.IASTORE);
		arraySet.add(Opcodes.LASTORE);
		arraySet.add(Opcodes.FASTORE);
		arraySet.add(Opcodes.DASTORE);
		arraySet.add(Opcodes.AASTORE);
		arraySet.add(Opcodes.BASTORE);
		arraySet.add(Opcodes.CASTORE);
		arraySet.add(Opcodes.SASTORE);
		
		arraySet.add(Opcodes.ARRAYLENGTH);
	}
	
	public static HashSet<Integer> getArraySet() {
		return arraySet;
	}
	
	private static void loadJumpSet() {
		jumpSet.add(Opcodes.IF_ACMPNE);
		jumpSet.add(Opcodes.IF_ACMPEQ);
		jumpSet.add(Opcodes.IF_ICMPEQ);
		jumpSet.add(Opcodes.IF_ICMPGE);
		jumpSet.add(Opcodes.IF_ICMPGT);
		jumpSet.add(Opcodes.IF_ICMPLE);
		jumpSet.add(Opcodes.IF_ICMPLT);
		jumpSet.add(Opcodes.IF_ICMPNE);
		jumpSet.add(Opcodes.IFEQ);
		jumpSet.add(Opcodes.IFGE);
		jumpSet.add(Opcodes.IFGT);
		jumpSet.add(Opcodes.IFLE);
		jumpSet.add(Opcodes.IFLT);
		jumpSet.add(Opcodes.IFNE);
		jumpSet.add(Opcodes.IFNONNULL);
		jumpSet.add(Opcodes.IFNULL);
		
		jumpSet.add(Opcodes.GOTO);
		
		jumpSet.add(Opcodes.TABLESWITCH);
		jumpSet.add(Opcodes.LOOKUPSWITCH);
		
		jumpSet.add(Opcodes.JSR);
	}
	
	public static HashSet<Integer> getJumpSet() {
		return jumpSet;
	}
	
	private static void loadReturnSet() {
		returnSet.add(Opcodes.ARETURN);
		returnSet.add(Opcodes.DRETURN);
		returnSet.add(Opcodes.FRETURN);
		returnSet.add(Opcodes.IRETURN);
		returnSet.add(Opcodes.LRETURN);
		returnSet.add(Opcodes.RETURN);
	}
	
	public static HashSet<Integer> getReturnSet() {
		return returnSet;
	}
	
}

