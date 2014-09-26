package edu.columbia.psl.cc.premain;

import java.lang.reflect.Method;

public class MIBDriver {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String className = args[0];
		String[] newArgs = new String[args.length - 1];
		
		for (int i = 1; i < args.length; i++) {
			newArgs[i] = args[i];
		}
		
		try {
			Class targetClass = Class.forName(className);
			System.out.println("Confirm class: " + targetClass);
			
			Method mainMethod = targetClass.getMethod("main", String[].class);
			System.out.println("Capture main method: " + mainMethod);
			mainMethod.invoke(null, (Object)newArgs);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

}
