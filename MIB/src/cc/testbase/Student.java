package cc.testbase;

import edu.columbia.psl.cc.annot.analyzeClass;
import edu.columbia.psl.cc.annot.extractTemplate;

@analyzeClass
public class Student {

	private String name;
	
	private String uni;
	
	public void setName(String name) {
		this.name = name;
	}
	
	@extractTemplate
	public String getName() {
		return this.name;
	}
	
	public void setUni(String uni) {
		this.uni = uni;
	}
	
	public String getUni() {
		return this.uni;
	}

}
