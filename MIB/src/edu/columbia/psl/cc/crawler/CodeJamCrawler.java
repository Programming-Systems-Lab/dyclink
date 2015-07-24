package edu.columbia.psl.cc.crawler;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class CodeJamCrawler {
	
	public static final String baseUrl = "http://www.go-hero.net/jam/";
	
	public static final String matchString = "http://code.google.com/codejam/contest/scoreboard/do?cmd=GetSourceCode";
	
	public static void main(String[] args) {
		try {
			System.out.println("Code jam year:");
			Scanner s = new Scanner(System.in);
			int year = s.nextInt();
			
			System.out.println("Code jam round (World Finals = 6):");
			int round = s.nextInt();
			
			System.out.println("Problem id:");
			int problemId = s.nextInt();
			
			String finalUrl = baseUrl + year + "/solutions/" + round + "/" + problemId + "/Java";
			System.out.println("Confirm final URL: " + finalUrl);
			
			System.out.println("Your code repository:");
			String codeRepo = null;
			while (true) {
				codeRepo = s.next();
				if (codeRepo == null || codeRepo.length() == 0 || codeRepo.isEmpty()) {
					System.err.println("Invalid code repo. Please give a new one");
					continue ;
				}
				
				File codeRepoFile = new File(codeRepo);
				if (codeRepoFile.exists() && !codeRepoFile.isDirectory()) {
					System.err.println("Code repo name has been used. Please give a new one");
					continue;
				} else if (!codeRepoFile.exists()) {
					codeRepoFile.mkdir();
					break ;
				} else {
					break ;
				}
			}
			
			File roundDir = new File(codeRepo + "/round" + round);
			if (!roundDir.exists()) {
				roundDir.mkdir();
			}
			
			File problemDir = new File(roundDir.getAbsolutePath() + "/problem" + problemId);
			if (!problemDir.exists()) {
				problemDir.mkdir();
			}
			
			Document codeJamHome = Jsoup.connect(finalUrl).get();
			//System.out.println("Show elements");
			/*for (Element e: codeJamHome.getAllElements()) {
				System.out.println(e.toString());
			}*/
			
			int userCounter = 0;
			for (Element e: codeJamHome.select("a[href]")) {
				String link = e.attr("abs:href");
				if (link.startsWith(matchString)) {
					System.out.println(link);
					String[] sep = link.split("&");
					
					String userString = sep[sep.length - 1];
					int eqPos = userString.indexOf("=");
					String userName = userString.substring(eqPos + 1, userString.length());
					System.out.println(userName);
					
					URL codeUrl = new URL(link);
					ReadableByteChannel rbc = Channels.newChannel(codeUrl.openStream());
					File codeZipFile = new File(problemDir + "/" + userName + ".zip");
					System.out.println("Check roundDir: " + roundDir);
					if (!codeZipFile.exists()) {
						codeZipFile.createNewFile();
					}
					FileOutputStream fos = new FileOutputStream(codeZipFile);
					fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
					
					System.out.println("Save file to: " + codeZipFile.getAbsolutePath());
					
					userCounter++;
				}
			}
			System.out.println("Total code files: " + userCounter);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

}
