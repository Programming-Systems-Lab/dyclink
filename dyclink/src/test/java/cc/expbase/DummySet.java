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
}
