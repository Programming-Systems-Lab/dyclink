package cc.testbase;

import org.objectweb.asm.Type;


public final class Template2 extends TemplateParent{
	
	private static int staticInt = 0;

	public Template2() {
		
	}
	
	public String testBinOp(long a, long b, BinOp op) {
		long opResult = op.op(a, b);
		Student s = new Student("myId", "myName");
		String result = s.getId() + opResult;
		System.out.println(Type.getType(result.getClass()).getSort());
		return result;
	}
	
	public static void main (String[] args) {
		//Template2 t2 = new Template2();
		//String result = t2.testBinOp(3, 5, OR);
		String abc = "abc";
		int abcSize = abc.length();
		System.out.println(Type.OBJECT);
		
		String[] stringArray = new String[5];
		System.out.println(Type.getType("[Ljava/lang/String;").getSort());
		System.out.println(Type.getType(stringArray.getClass()).getSort());
		System.out.println(Type.ARRAY);
		String.class.getClassLoader();
		
		int a = staticInt;
	}
	
	private static class Student {
		String id;
		String name;
		
		public Student(String id, String name) {
			this.id = id;
			this.name = name;
		}
		
		public String getId() {
			return this.id;
		}
		
		public String getName() {
			return this.name;
		}
	}
	
    private static interface BinOp {
    	public long op(long a, long b);
    }
    
    private static final BinOp AND = new BinOp() {
    	public final long op(long a, long b) { return a & b; }
    };
    
    private static final BinOp OR = new BinOp() {
    	public final long op(long a, long b) { return a | b; }
    };
    
    private static final BinOp XOR = new BinOp() {
    	public final long op(long a, long b) { return a ^ b; }
    };
}
