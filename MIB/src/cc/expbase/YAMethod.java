package cc.expbase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

import org.objectweb.asm.Type;

public class YAMethod {
	
	public YAMethod() {
		
	}
	
	public YAMethod(int i) {
		
	}

	public void addSomething(ArrayList<Integer> target) {
		target.add(3);
	}
	
	public static void main(String[] ars) throws ClassNotFoundException {
		//YAMethod ym = new YAMethod();
		//int b = TemplateMethod.parentVar;
		//int c = TemplateMethod.sVar;
		TemplateMethod tp = new TemplateMethod();
		System.out.println(tp.getClass().getName());
		System.out.println(Type.getInternalName(tp.getClass()));
		System.out.println(Type.getType("[D").getInternalName());
		System.out.println(Type.ARRAY);
		System.out.println(Type.getType("cc/expbase/TemplateMethod").getSort());
		System.out.println(Type.METHOD);
		Object[] o = new Object[5];
		System.out.println(Class.forName("cc.expbase.TemplateMethod").getProtectionDomain().getCodeSource().getLocation().getPath());
		
		//HashMap<String, Object> h1 = new HashMap<String, Object>();
		//HashMap<String, Object> h2 = new HashMap<String, Object>();
		
		//System.out.println(h1.keySet().equals(h2.keySet()));
	}

}
