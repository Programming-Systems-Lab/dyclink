package example;

import java.io.File;

import edu.columbia.psl.cc.util.ObjectIdAllocater;

public class BasicTest extends BasicSuper {
	
	int id;
	
	public static int staticSum(BasicTest bt) {
		return 0;
	}
	
	public BasicTest() {
		System.out.println(super.superId);
	}
	
	@Override
	public void superMethod() {
		System.out.println("Called");
		this.id = 5;
	}
	
	public int sum(int i, int j) {
		return i + j + 5;
	}
	
	public int sum(int j) {
		for (int i = 0; i < j; i++) {
			j++;
		}
		return j;
	}
	
	public static void main(String[] args) {
		BasicTest bt = new BasicTest();
		System.out.println(bt.id);
		System.out.println(System.getProperty("java.home"));
		
		File jdkHome = new File("/Library/Java/JavaVirtualMachines/");
		File jdk7 = null;
		try {
			for (File f: jdkHome.listFiles()) {
				if (f.getName().matches("jdk1.7.*")) {
					jdk7 = f;
				}
			}
			System.out.println("JDK7: " + jdk7.getAbsolutePath());
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

}
