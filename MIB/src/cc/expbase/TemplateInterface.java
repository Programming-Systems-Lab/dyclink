package cc.expbase;

import java.util.ArrayList;

public interface TemplateInterface {
	
	public int interfaceVar = 5;
	
	public Object interfaceObject = new Object();
	
	public Object doIt(Number n);
	
	public static final YAMethod ya = new YAMethod() {
		@Override
		public void addSomething(ArrayList<Integer> target) {
			target.add(interfaceVar);
			Object b = interfaceObject;
		}
	};

}
