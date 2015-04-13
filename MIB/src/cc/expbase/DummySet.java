package cc.expbase;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.google.gson.reflect.TypeToken;

import edu.columbia.psl.cc.pojo.GraphTemplate;
import edu.columbia.psl.cc.util.GraphConstructor;
import edu.columbia.psl.cc.util.GsonManager;
import edu.columbia.psl.cc.util.TemplateLoader;

public class DummySet {

	public static void setParent(TemplateParent tp) {
		tp.pVar = 5;
	}
	
	public void stupidMethod() {
		try {
			String nullString = null;
			String.class.getName();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		/*List<Integer> dummy = new ArrayList<Integer>();
		dummy.add(0);
		dummy.add(1);
		dummy.add(2);
		dummy.add(3);
		dummy.add(4);
		System.out.println(dummy.subList(0, 4));*/
		TypeToken<GraphTemplate> graphToken = new TypeToken<GraphTemplate>(){};
		File dir = new File("/Users/mikefhsu/Mike/Research/ec2/mib_sandbox_v3/ejml_graphs");
		HashMap<String, GraphTemplate> templates = TemplateLoader.loadTemplate(dir, graphToken);
		int max = 0;
		String fileName = null;
		for (GraphTemplate t: templates.values()) {
			if (t.getVertexNum() > 10000) {
				max = t.getVertexNum();
				fileName = t.getMethodKey() + ": " + t.getThreadMethodId();
				System.out.println(max);
				System.out.println(fileName);
			}
		}
		//System.out.println("Max num: " + max);
		//System.out.println("Mehtod name: " + fileName);
	}

}
