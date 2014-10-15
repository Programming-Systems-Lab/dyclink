package cc.testbase;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import org.objectweb.asm.Type;

import edu.columbia.psl.cc.annot.analyzeClass;
import edu.columbia.psl.cc.annot.extractTemplate;
import edu.columbia.psl.cc.annot.testTemplate;

@analyzeClass
public class TemplateObjMethod {
	
	public static int __mib_id_gen = 1;
	
	public int __mib_id;
	
	public Object testObj;
	
	public static synchronized int __gen_mib_id() {
		return __mib_id_gen++;
	}
	
	@extractTemplate
	public TemplateObjMethod() {
		//__mib_id = __gen_mib_id();
		testObj = new Object();
		int a = __mib_id;
	}
	
	public void acceptObject(Object obj) {
		System.out.println(obj);
		Object[] objArray = new Object[5];
		objArray[0] = testObj;
		int a = objArray.length;
	};
	
	public void callObject() {
		Student stu = new Student();
		this.acceptObject(stu);
		this.acceptObject(2.0);
	}
	
	public void testStudentArray() {
		Student[] ss = new Student[3];
		Student s = new Student();
		ss[2] = s;
		String a = ss[2].home.address;
		ss[1] = new Student();
		
		Student[][][]ss2 = new Student[2][2][2];
		ss2[1][1][1] = new Student();
		Student sTmp = ss2[1][1][1];
		
		int[] intTest = new int[3];
		intTest[2] = intTest[1] + 3;
		this.acceptObject(intTest[2]);
	}
		
	public Integer sequenceObjTemplate(Integer a, Integer b, Integer c) {
		Integer d = a + 19;
		Integer e = d + b;
		Integer obj = e + c;
		return obj;
	}
	
	public Double parallelObjTemplate(Double a, Double b, Double c) {
		Double d = a + b;
		Double e = c + 3;
		Double f = d + e;
		return f;
	}
	
	public String testObjSequence(String s1, String s2) {
		String s3 = s1 + " columbia";
		String s4 = s3 + s2;
		String s5 = s4 + "!!!";
		return s5;
	}
	
	public static void main(String[] args) {
		int[][] test = {{1, 2}, {3, 4}};
		System.out.println(Arrays.toString(test));
		Student a = new Student();
		a.id = 1;
		a.name = "test123";
		
		Student b = new Student();
		b.id = 1;
		b.name = "test123";
		System.out.println(a.equals(b));
		System.out.println("a hash: " + a.hashCode());
		System.out.println("b hash: " + b.hashCode());
		int aHash = System.identityHashCode(a);
		a.getName();
		int bHash = System.identityHashCode(b);
		System.out.println("a jvm hash: " + Integer.toHexString(aHash));
		System.out.println("b jvm hash: " + Integer.toHexString(bHash));
		
		TemplateObjMethod tom = new TemplateObjMethod();
		System.out.println(Type.getType("[Ljava/lang/Object;").getSort());
	}
	
	public static class Home {
		public String address;
		
		public void setAddress(String address) {
			this.address = address;
		}
	}
	
	public static class Student {
		int id;
		String name;
		Home home = new Home();
		
		public String getName() {
			return name;
		}
		
		/*@Override
		public String toString() {
			return this.id + " " + this.name;
		}
		
		@Override
		public boolean equals(Object tmp) {
			if (!(tmp instanceof Student))
				return false;
			
			Student tmpStudent = (Student)tmp;
			if (tmpStudent.toString().equals(this.toString()))
				return true;
			else
				return false;
		}
		
		@Override
		public int hashCode() {
			return this.toString().hashCode();
		}*/
	}
}
