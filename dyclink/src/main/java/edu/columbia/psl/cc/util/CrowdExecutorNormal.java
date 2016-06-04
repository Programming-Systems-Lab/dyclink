package edu.columbia.psl.cc.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.reflect.TypeToken;

import cern.colt.Arrays;
import edu.columbia.psl.cc.config.MIBConfiguration;

public class CrowdExecutorNormal {
	
	private static final Logger logger = LogManager.getLogger(CrowdExecutor.class);
		
	private static final Class[] parameters = new Class[]{URL.class};
	
	private static final Class[] mainParameters = new Class[]{String[].class};
	
	private static final String graphRepo = "./graphrepo";
	
	private static final TypeToken<MIBConfiguration> configToken = new TypeToken<MIBConfiguration>(){};
	
	public static void main(String[] args) throws Exception {
		File graphDir = new File(graphRepo);
		if (!graphDir.exists()) {
			graphDir.mkdir();
		}
		
		File jdkFile = null;
		File jdkHome = new File("/Library/Java/JavaVirtualMachines/");
		for (File f: jdkHome.listFiles()) {
			if (f.getName().matches("jdk1.7.*")) {
				jdkFile = f;
				break ;
			}
		}
		
		String jdkPath = null;
		if (jdkFile == null) {
			logger.error("Cannot locate jdk path...");
			System.exit(-1);
		} else {
			jdkPath = jdkFile.getAbsolutePath() + "/Contents/Home/bin/java";
			logger.info("Confirm jdk path: " + jdkPath);
		}
				
		File binDir = new File(args[0]);
		URLClassLoader sysloader = (URLClassLoader)ClassLoader.getSystemClassLoader();
		Class sysClass = URLClassLoader.class;
		
		Method method = sysClass.getDeclaredMethod("addURL", parameters);
		method.setAccessible(true);
		method.invoke(sysloader, new Object[]{binDir.toURI().toURL()});
		
		logger.info("Bin dir: " + args[0]);
		logger.info("Problem pkg: " + args[1]);
		
		String binClusterDir = args[0] + "/" + args[1];
		File codeJamDir = new File(binClusterDir);
		String binClusterdir = codeJamDir.getAbsolutePath();
		logger.info("Bin cluster dir: " + binClusterDir);
		
		//TreeMap<String, String> executableClasses = new TreeMap<String, String>();
		HashSet<String> executableClasses = new HashSet<String>();
		for (File usrDir: codeJamDir.listFiles()) {
			if (usrDir.isDirectory() && !usrDir.getName().startsWith(".")) {
				String userDirName = usrDir.getName();
				/*File usrRepo = new File(graphDir.getAbsolutePath() + "/" + args[2] + "-" + userDirName);
				if (!usrRepo.exists()) {
					usrRepo.mkdir();
				}*/
				
				for (File classFile: usrDir.listFiles()) {
					String className = classFile.getName();
					String fullName = args[1] + "." + userDirName + "." + className.substring(0, className.length() - 6);
					//System.out.println("Class fullname: " + fullName);
					
					Class checkClass = Class.forName(fullName);
					try {
						Method mainMethod = checkClass.getMethod("main", mainParameters);
						//executableClasses.put(checkClass.getName(), usrRepo.getAbsolutePath());
						executableClasses.add(checkClass.getName());
					} catch (Exception ex) {
						//Just don't record the class has no main
					}
				}
			}
		}
		logger.info("Executable classes: " + executableClasses.size());
		
		int success = 0;
		for (String execClass: executableClasses) {
			//String graphRepoPath = executableClasses.get(execClass);
			
			//Need to set the grah path
			/*MIBConfiguration config = MIBConfiguration.reloadInstance();
			config.setGraphDir(graphRepoPath);
			
			System.out.println("Check config before writing: " + config.getThreadMethodIdxRecord());
			
			String fileName = "./config/mib_config.json";
			GsonManager.writeJsonGeneric(config, fileName, configToken, -1);*/
						
			List<String> commands = new ArrayList<String>();
			//commands.add("java")
			commands.add(jdkPath);
			//commands.add("-javaagent:target/dyclink-0.0.1-SNAPSHOT.jar");
			//commands.add("-noverify");
			//commands.add("-XX:-UseSplitVerifier");
			commands.add("-Xmx4g");
			commands.add("-cp");
			commands.add("target/dyclink-0.0.1-SNAPSHOT.jar:" + binDir.getAbsolutePath());
			commands.add("edu.columbia.psl.cc.premain.MIBDriver");
			commands.add(execClass);
			System.out.println("Execute " + commands);
			
			ProcessBuilder pBuilder = new ProcessBuilder();
			pBuilder.command(commands);
			//pBuilder.redirectErrorStream(true).redirectOutput(tmpLog);
			final Process process = pBuilder.start();
			
			Thread stdOutThread = new Thread() {
				public void run() {
					InputStream is = process.getInputStream();
					InputStreamReader reader = new InputStreamReader(is);
					BufferedReader br = new BufferedReader(reader);
					
					try {
						String buf = "";
						while ((buf = br.readLine()) != null) {
							System.out.println(buf);
						}
					} catch (Exception ex) {
						//ex.printStackTrace();
						logger.error("Error: ", ex);
					}
				}
			};
			
			Thread stdErrThread = new Thread() {
				public void run() {
					InputStream errIs = process.getErrorStream();
					InputStreamReader reader = new InputStreamReader(errIs);
					BufferedReader br = new BufferedReader(reader);
					
					try {
						String buf = "";
						while ((buf = br.readLine()) != null) {
							System.out.println(buf);
						}
					} catch (Exception ex) {
						//ex.printStackTrace();
						logger.error("Error: ", ex);
					}
				}
			};
			
			stdOutThread.start();
			stdErrThread.start();
			
			stdOutThread.join();
			stdErrThread.join();
			
			int errCode = process.waitFor();
			if (errCode == 0) {
				success++;
			}
			
			logger.info("Complete " + execClass);
		}
		logger.info("Success executions: " + success);
	}

}

