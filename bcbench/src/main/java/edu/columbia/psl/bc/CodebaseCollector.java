package edu.columbia.psl.bc;

import java.io.Console;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;

public class CodebaseCollector {
	
	public static Options options = new Options();
	
	private static Connection conn = null;
	
	static {
		options.addOption("db", true, "DB address");
		options.addOption("username", true, "DB username");
		options.addOption("funcat", true, "Functionality category");
		options.addOption("toks", true, "Token threshold");
		
		options.getOption("db").setRequired(true);
		options.getOption("username").setRequired(true);
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
			System.out.println("Confirm sqlString: " + sqlString);
			
			PreparedStatement stmt = conn.prepareStatement(sqlString);
			ResultSet rs = stmt.executeQuery();
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
				
				System.out.println(fi);
			}
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
		
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(name + ",");
			sb.append(start + ",");
			sb.append(end + ",");
			sb.append(size + ",");
			sb.append(tokens + ",");
			sb.append(proj + ",");
			sb.append(path + ",");
			sb.append(func_id + ",");
			sb.append(is_clone);
			return sb.toString();
		}
	}

}
