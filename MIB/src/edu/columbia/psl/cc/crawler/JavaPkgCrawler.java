package edu.columbia.psl.cc.crawler;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.google.gson.reflect.TypeToken;

import edu.columbia.psl.cc.config.MIBConfiguration;
import edu.columbia.psl.cc.util.GsonManager;

public class JavaPkgCrawler {
	
	public static void main(String[] args) {
		//String website = args[0];
		String website = "http://docs.oracle.com/javase/7/docs/api/overview-summary.html#overview_description";
		String fileName = "nativePackages";
		NativePackages np = new NativePackages();
		TypeToken<NativePackages> npToken = new TypeToken<NativePackages>(){};
		try {
			Document doc = Jsoup.connect(website).get();
			Element table = doc.select("table").first();
			Elements raws = table.select("tr");
			
			//Don't need the header
			for (int i = 1; i < raws.size(); i++) {
				Element raw = raws.get(i);
				Element column = raw.select("td").first();
				np.addNativePackage(column.text());
			}
			
			System.out.println("Check result");
			System.out.println(np.getNativePackages());
			
			GsonManager.writeJsonGeneric(np, fileName, npToken, MIBConfiguration.LABEL_MAP_DIR);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

}
