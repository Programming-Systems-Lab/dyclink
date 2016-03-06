package example;

import edu.columbia.psl.cc.util.ObjectIdAllocater;

public class BasicTest {
	
	int id = 0;
	
	public BasicTest() {
		if (id == 0) {
			id = ObjectIdAllocater.getIndex();
		}
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

}
