package edu.columbia.psl.cc.crawler;

import java.util.concurrent.atomic.AtomicInteger;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.google.gson.reflect.TypeToken;

import edu.columbia.psl.cc.config.MIBConfiguration;
import edu.columbia.psl.cc.util.GsonManager;

public class JavaPkgCrawler {
	
	//Save 0 for default
	private static AtomicInteger ai = new AtomicInteger(1);
	
	public static void main(String[] args) {
		//String website = args[0];
		String website = "http://docs.oracle.com/javase/7/docs/api/overview-summary.html#overview_description";
		
		System.out.println("Confirm javadoc location: " + website);
		
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
				np.addNativePackage(column.text(), ai.getAndIncrement());
			}
			
			System.out.println("Crawling result of java APIs: " + np.getNativePackages().size());
			GsonManager.writeJsonGeneric(np, fileName, npToken, MIBConfiguration.LABEL_MAP_DIR);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
