package edu.columbia.psl.cc.crawler;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class CodeJamCrawler {
	
	public static final String baseUrl = "http://www.go-hero.net/jam/";
	
	public static final String matchString = "http://code.google.com/codejam/contest/scoreboard/do?cmd=GetSourceCode";
	
	public static int writeFileToDest(ZipInputStream zipStream, String filePath) {
		try {
			BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
			byte[] bytesIn = new byte[1024];
			int read = 0;
			while ((read = zipStream.read(bytesIn)) != - 1) {
				bos.write(bytesIn, 0, read);
			}
			bos.close();
			System.out.println("Save file to " + filePath);
			return 1;
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return 0;
	}
	
	public static void writeFile(InputStream inputStream, File destFile) {
		try {
			ReadableByteChannel rbc = Channels.newChannel(inputStream);
			FileOutputStream fos = new FileOutputStream(destFile);
			fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
			fos.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	public static int unpackArchive(File tmpFile, File javaFile) {
		try {
			if (javaFile.exists()) {
				javaFile.delete();
			}
			
			ZipFile zipFile = new ZipFile(tmpFile);
			for (Enumeration entries = zipFile.entries(); entries.hasMoreElements();) {
				ZipEntry entry = (ZipEntry)entries.nextElement();
				writeFile(zipFile.getInputStream(entry), javaFile);
			}
			return 1;
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return 0;
	}
	
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
					
					//ReadableByteChannel rbc = Channels.newChannel(codeUrl.openStream());
					File tmpFile = File.createTempFile("arc", ".zip", problemDir);
					tmpFile.deleteOnExit();
					//FileOutputStream fos = new FileOutputStream(tmpFile);
					//fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
					writeFile(codeUrl.openStream(), tmpFile);
					
					File javaFile = new File(problemDir + "/" + userName + ".java");
					userCounter += unpackArchive(tmpFile, javaFile);
					
					System.out.println("Save file to: " + javaFile.getAbsolutePath());
				}
			}
			System.out.println("Total code files: " + userCounter);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

}
