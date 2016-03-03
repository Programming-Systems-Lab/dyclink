package cc.expbase;

import java.util.ArrayList;
import java.util.List;

import edu.columbia.psl.cc.annot.analyzeClass;
import edu.columbia.psl.cc.annot.extractTemplate;

@analyzeClass
public class StudentDriver {
	
	private List<Student> students = new ArrayList<Student>();
	
	public void initStudents() {
		Student s1 = new Student();
		s1.setName("s1");
		s1.setUni("s123");
		
		Student s2 = new Student();
		s2.setName("s2");
		s2.setUni("s234");
		
		Student s3 = new Student();
		s3.setName("s3");
		s3.setUni("s345");
		
		students.add(s1);
		students.add(s2);
		students.add(s3);
	}
	
	@extractTemplate
	public Student searchStudent(String name, String uni) {
		for (Student s: this.students) {
			if (s.getName().equals(name) && s.getUni().equals(uni)) {
				return s;
			}
		}
		return null;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		StudentDriver sd = new StudentDriver();
		sd.initStudents();
		Student found = sd.searchStudent("s1", "s123");
		System.out.println("Student: " + found.getName() + " " + found.getUni());
	}

}
