package cc.testbase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.objectweb.asm.Type;

import edu.columbia.psl.cc.util.ObjectIdAllocater;


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
		/*String abc = "abc";
		int abcSize = abc.length();
		System.out.println(Type.OBJECT);
		
		String[] stringArray = new String[5];
		System.out.println(Type.getType("[Ljava/lang/String;").getSort());
		System.out.println(Type.getType(stringArray.getClass()).getSort());
		System.out.println(Type.ARRAY);
		String.class.getClassLoader();
		int a = staticInt;*/
		
		System.out.println("Main thread id: " + Thread.currentThread().getId() + " " + ObjectIdAllocater.getThreadId());
		ExecutorService executor = Executors.newFixedThreadPool(3);
		executor.execute(new Runnable() {
			public void run() {
				long result = OR.op(3, 5);
				System.out.println("Thread id: " + Thread.currentThread().getId());
				System.out.println(ObjectIdAllocater.getThreadId());
				System.out.println("Result: " + result);
			}
		});
		
		executor.execute(new Runnable() {
			public void run() {
				long result = OR.op(4, 6);
				System.out.println("Thread id: " + Thread.currentThread().getId());
				System.out.println(ObjectIdAllocater.getThreadId());
				System.out.println("Result: " + result);
			}
		});
		
		executor.execute(new Runnable() {
			public void run() {
				long result = OR.op(7, 3);
				System.out.println("Thread id: " + Thread.currentThread().getId());
				System.out.println(ObjectIdAllocater.getThreadId());
				System.out.println("Result: " + result);
			}
		});
		
		executor.shutdown();
		while(!executor.isTerminated());
		
		System.out.println("Experiment ends");
		System.out.println("Check again current thread id: " + ObjectIdAllocater.getThreadId());
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
