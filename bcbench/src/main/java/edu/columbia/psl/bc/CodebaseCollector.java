package edu.columbia.psl.bc;

import java.io.BufferedWriter;
import java.io.Console;
import java.io.File;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;

import net.schmizz.sshj.SSHClient;

public class CodebaseCollector {
	
	public static Options options = new Options();
	
	public static String funcHeader = "name,start,end,id,size,toks,proj,path,func_cat,is_clone\n";
	
	public static String cloneHeader = "func1_id,func1,func2,func2_id,func_cat,syn_type,sim_line,sim_toks\n";
	
	private static Connection conn = null;
	
	static {
		options.addOption("db", true, "DB address");
		options.addOption("username", true, "DB username");
		options.addOption("funcat", true, "Functionality category");
		options.addOption("toks", true, "Token threshold");
		options.addOption("rName", true, "SCP username");
		options.addOption("rDir", true, "SCP directory");
		
		options.getOption("db").setRequired(true);
		options.getOption("username").setRequired(true);
		options.getOption("rName").setRequired(true);
		options.getOption("rDir").setRequired(true);
	}
	
	public static Connection getConnection(String db, String username, String pw) {
		try {
			String dbString = "jdbc:postgresql://" + db + ":5432/bigclonebench";
			if (conn == null) {
				conn = DriverManager.getConnection(dbString, username, pw);
			}
			
			if (conn != null) {
				int counter = 0;
				while (counter < 3) {
					if (conn.isValid(3)) {
						return conn;
					} else {
						System.err.println("Try to reconnect to db");
						conn = DriverManager.getConnection(dbString, username, pw);
						counter++;
					}
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
		return null;
	}
	
	public static String genFuncIds(Set<Integer> ids) {
		StringBuilder sb = new StringBuilder();
		for (Integer id: ids) {
			sb.append(id + ",");
		}
		
		return "(" + sb.substring(0, sb.length() - 1) + ")";
	}
	
	public static void exportSelectedMethods(Collection<FuncInfo> funcs, int funCat, int toks, String tF) {
		StringBuilder sb = new StringBuilder();
		sb.append(funcHeader);
		funcs.forEach(fi->{
			sb.append(fi.toString() + "\n");
		});
		
		File resultDir = new File("results");
		if (!resultDir.exists()) {
			resultDir.mkdir();
		}
		
		String path = resultDir.getAbsolutePath() + "/funcs_" + tF + "_cat" + funCat + "_toks" + toks + ".csv";
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(path));
			bw.write(sb.toString());
			bw.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	public static void exportSelectedClones(Collection<CloneInfo> clones, int funCat, int toks, String tf) {
		StringBuilder sb = new StringBuilder();
		sb.append(cloneHeader);
		clones.forEach(ci->{
			sb.append(ci.toString() + "\n");
		});
		
		File resultDir = new File("results");
		if (!resultDir.exists()) {
			resultDir.mkdir();
		}
		
		String path = resultDir.getAbsolutePath() + "/clones_" + tf + "_cat" + funCat + "_toks" + toks + ".csv";
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(path));
			bw.write(sb.toString());
			bw.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		try {
			Class.forName("org.postgresql.Driver");
			
			CommandLineParser parser = new DefaultParser();
			CommandLine cmd = parser.parse(options, args);
			
			String db = cmd.getOptionValue("db");
			String username = cmd.getOptionValue("username");
			
			System.out.println("Confirm db: " + db);
			System.out.println("Confirm username: " + username);
			
			Console console = System.console();
			
			if (console == null) {
				System.err.println("No valid console");
				System.exit(-1);
			}
			
			char[] pwArray = console.readPassword("Password: ");
			String password = new String(pwArray);
			
			Connection conn = getConnection(db, username, password);
			if (conn != null) {
				System.out.println("Connection succeeds");
			} else {
				System.out.println("Fail to connect to: " + db);
			}
			
			String funcatString = cmd.getOptionValue("funcat");
			int funcat = -1;
			if (funcatString != null) {
				funcat = Integer.valueOf(funcatString);
			}
			
			int toks = -1;
			String tokString = cmd.getOptionValue("toks");
			if (tokString != null) {
				toks = Integer.valueOf(tokString);
			}
			
			String innerQuery = "SELECT functionality_id, function_id, is_clone FROM tagged";
			if (funcat != -1) {
				innerQuery += (" WHERE functionality_id=" + funcat);
			}
			//String sqlString = "SELECT count(*) FROM functions WHERE id in (" + innerQuery + ")";
			String sqlString = "SELECT functions.*, tagged_f.functionality_id, tagged_f.is_clone "
					+ "FROM functions join (" + innerQuery + ") as tagged_f " 
					+ "ON functions.id = tagged_f.function_id";
			
			if (toks != -1) {
				sqlString += (" WHERE tokens>=" + toks);
			}
			//System.out.println("Confirm sqlString: " + sqlString);
			
			PreparedStatement stmt = conn.prepareStatement(sqlString);
			ResultSet rs = stmt.executeQuery();
			Map<Integer, FuncInfo> tps = new HashMap<Integer, FuncInfo>();
			Map<Integer, FuncInfo> fps = new HashMap<Integer, FuncInfo>();
			Map<Integer, FuncInfo> samples = new HashMap<Integer, FuncInfo>();
			
			while (rs.next()) {
				String name = rs.getString("name");
				int start = rs.getInt("startline");
				int end = rs.getInt("endline");
				int id = rs.getInt("id");
				int size = rs.getInt("normalized_size");
				int tokens = rs.getInt("tokens");
				String proj = rs.getString("project");
				String path = rs.getString("path");
				int func_id = rs.getInt("functionality_id");
				boolean is_clone = rs.getBoolean("is_clone");
				
				FuncInfo fi = new FuncInfo();
				fi.name = name;
				fi.start = start;
				fi.end = end;
				fi.id = id;
				fi.size = size;
				fi.tokens = tokens;
				fi.proj = proj;
				fi.path = path;
				fi.func_id= func_id;
				fi.is_clone = is_clone;
				
				if (is_clone) {
					tps.put(fi.id, fi);
				} else {
					fps.put(fi.id, fi);
				}
			}
			
			//Collect the true sample
			String innerSample = "SELECT function_id FROM samples WHERE functionality_id=" + funcat;
			String sampleSql = "SELECT * FROM functions WHERE id in (" + innerSample + ")";
			PreparedStatement sampleStmt = conn.prepareStatement(sampleSql);
			ResultSet sampleResult = sampleStmt.executeQuery();
			while (sampleResult.next()) {
				String name = sampleResult.getString("name");
				int start = sampleResult.getInt("startline");
				int end = sampleResult.getInt("endline");
				int id = sampleResult.getInt("id");
				int size = sampleResult.getInt("normalized_size");
				int tokens = sampleResult.getInt("tokens");
				String proj = sampleResult.getString("project");
				String path = sampleResult.getString("path");
				
				FuncInfo fi = new FuncInfo();
				fi.name = name;
				fi.start = start;
				fi.end = end;
				fi.id = id;
				fi.size = size;
				fi.tokens = tokens;
				fi.proj = proj;
				fi.path = path;
				fi.func_id= funcat;
				fi.is_clone = true;
				fi.isSample = true;
				
				//Samples is for constructing fp pairs
				samples.put(fi.id, fi);
				tps.put(fi.id, fi);
			}
			
			System.out.println("Confirm true functions size: " + tps.size());
			System.out.println("Confirm sample size: " + samples.size());
			System.out.println("Confirm false functions size: " + fps.size());
			exportSelectedMethods(tps.values(), funcat, toks, "tp");
			exportSelectedMethods(fps.values(), funcat, toks, "fp");
			
			String tIdString = genFuncIds(tps.keySet());
			String sampleString = genFuncIds(samples.keySet());
			String fIdString = genFuncIds(fps.keySet());
			
			String cloneSql = "SELECT * FROM clones WHERE functionality_id=" + funcat 
					+ " and function_id_one in " + tIdString 
					+ " and function_id_two in " + tIdString;
			//System.out.println("Confirm true clone sql: " + cloneSql);
			
			PreparedStatement cloneStmt = conn.prepareStatement(cloneSql);
			ResultSet cloneRs = cloneStmt.executeQuery();
			List<CloneInfo> tClones = new ArrayList<CloneInfo>();
			while (cloneRs.next()) {
				int cFunc1 = cloneRs.getInt(1);
				FuncInfo func1 = tps.get(cFunc1);
				int cFunc2 = cloneRs.getInt(2);
				FuncInfo func2 = tps.get(cFunc2);
				
				int cFuncId = cloneRs.getInt(3);
				int syncType = cloneRs.getInt(5);
				double sim_line = cloneRs.getDouble(6);
				double sim_toks = cloneRs.getDouble(7);
				
				CloneInfo ci = new CloneInfo();
				ci.fi1 = func1;
				ci.fi2 = func2;
				ci.func_id = cFuncId;
				ci.syn_type = syncType;
				ci.sim_line = sim_line;
				ci.sim_toks = sim_toks;
				tClones.add(ci);
			}
			System.out.println("Tota tp size: " + tClones.size());
			
			String fCloneSql = "SELECT * FROM false_positives WHERE functionality_id=" + funcat 
					+ " and function_id_one in " + sampleString  
					+ " and function_id_two in " + fIdString;
			//System.out.println("Confirm false clone sql: " + fCloneSql);
			PreparedStatement fCloneStmt1 = conn.prepareStatement(fCloneSql);
			
			String fCloneSql2 = "SELECT * FROM false_positives WHERE functionality_id=" + funcat 
					+ " and function_id_one in " + fIdString  
					+ " and function_id_two in " + sampleString;
			//System.out.println("Confirm false clone sql: " + fCloneSql2);
			PreparedStatement fCloneStmt2 = conn.prepareStatement(fCloneSql2);
			
			List<CloneInfo> fClones = new ArrayList<CloneInfo>();
			ResultSet fCloneRs1 = fCloneStmt1.executeQuery();
			ResultSet fCloneRs2 = fCloneStmt2.executeQuery();
			while (fCloneRs1.next()) {
				int fcFunc1 = fCloneRs1.getInt(1);
				FuncInfo func1 = tps.get(fcFunc1);
				int fcFunc2 = fCloneRs1.getInt(2);
				FuncInfo func2 = fps.get(fcFunc2);
				
				int fcFuncId = fCloneRs1.getInt(3);
				double sim_line = fCloneRs1.getDouble(5);
				double sim_toks = fCloneRs1.getDouble(6);
				int syncType = fCloneRs1.getInt(7);
				
				CloneInfo fci = new CloneInfo();
				fci.fi1 = func1;
				fci.fi2 = func2;
				fci.func_id = fcFuncId;
				fci.sim_line = sim_line;
				fci.sim_toks = sim_toks;
				fci.syn_type = syncType;
				
				fClones.add(fci);
			}
			System.out.println("First fp size: " + fClones.size());
			
			while (fCloneRs2.next()) {
				int fcFunc1 = fCloneRs2.getInt(1);
				FuncInfo func1 = fps.get(fcFunc1);
				int fcFunc2 = fCloneRs2.getInt(2);
				FuncInfo func2 = tps.get(fcFunc2);
				
				int fcFuncId = fCloneRs2.getInt(3);
				double sim_line = fCloneRs2.getDouble(5);
				double sim_toks = fCloneRs2.getDouble(6);
				int syncType = fCloneRs2.getInt(7);
				
				CloneInfo fci = new CloneInfo();
				fci.fi1 = func1;
				fci.fi2 = func2;
				fci.func_id = fcFuncId;
				fci.sim_line = sim_line;
				fci.sim_toks = sim_toks;
				fci.syn_type = syncType;
				
				fClones.add(fci);
			}
			
			System.out.println("Total fp size: " + fClones.size());
			exportSelectedClones(tClones, funcat, toks, "tp");
			exportSelectedClones(fClones, funcat, toks, "fp");
			System.out.println("Complete dumping clone info");
			
			//Start to retrieve files
			String remoteUsername = cmd.getOptionValue("rName");
			String remoteDir = cmd.getOptionValue("rDir");
			char[] scpChars = console.readPassword("SCP password");
			String scpPw = new String(scpChars);
			
			SSHClient ssh = new SSHClient();
			ssh.useCompression();
			ssh.loadKnownHosts();
			ssh.connect(db);
			
			ssh.authPassword(remoteUsername, scpPw);
			System.out.println("Retrieving true functions");
			tps.forEach((id, func)->{
				String remotePath = "";
				if (func.isSample) {
					remotePath = remoteDir + "/sample/" + func.name;
				} else {
					remotePath = remoteDir + "/selected/" + func.name;
				}
				String localPath = "codebase/" + func.name;
				try {
					System.out.println("Retrieving " + func.name);
					ssh.newSCPFileTransfer().download(remotePath, localPath);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			});
			
			System.out.println("Retrieving false functions");
			fps.forEach((id, func)->{
				String remotePath = remoteDir + "/selected/" + func.name;
				String localPath = "codebase/" + func.name;
				try {
					System.out.println("Retrieving " + func.name);
					ssh.newSCPFileTransfer().download(remotePath, localPath);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			});
			ssh.disconnect();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	public static class FuncInfo {
		String name;
		int start;
		int end;
		int id;
		int size;
		int tokens;
		String proj;
		String path;
		int func_id;
		boolean is_clone;
		transient boolean isSample;
		
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(name + ",");
			sb.append(start + ",");
			sb.append(end + ",");
			sb.append(id + ",");
			sb.append(size + ",");
			sb.append(tokens + ",");
			sb.append(proj + ",");
			sb.append(path + ",");
			sb.append(func_id + ",");
			sb.append(is_clone);
			return sb.toString();
		}
	}
	
	public static class CloneInfo {
		FuncInfo fi1;
		FuncInfo fi2;
		int func_id;
		int syn_type;
		double sim_line;
		double sim_toks;
		
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(this.fi1.id + ",");
			sb.append(this.fi1.name + ",");
			sb.append(this.fi2.id + ",");
			sb.append(this.fi2.name + ",");
			sb.append(this.func_id + ",");
			sb.append(this.syn_type + ",");
			sb.append(this.sim_line + ",");
			sb.append(this.sim_toks);
			return sb.toString();
			
		}
	}

}
