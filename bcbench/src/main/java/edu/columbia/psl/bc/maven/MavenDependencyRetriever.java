package edu.columbia.psl.bc.maven;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.project.MavenProject;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.util.artifact.DefaultArtifact;
import org.sonatype.aether.util.artifact.JavaScopes;

import com.jcabi.aether.Aether;

/**
 * Copy and paste from http://stackoverflow.com/questions/11799923/programmatically-resolving-maven-dependencies-outside-of-a-plugin-get-reposito
 * @author mikefhsu
 *
 */
public class MavenDependencyRetriever {
	
	private static Options options = new Options();
	
	private static final Logger logger = LogManager.getLogger(MavenDependencyRetriever.class);
	
	static {
		options.addOption("cb", true, "The location of pom file");
		options.addOption("m2", true, "The m2 repository");
		options.addOption("evoJar", true, "The location of evo suite");
		options.addOption("inputLoc", true, "The location for generated input");
		
		options.getOption("cb").setRequired(true);
		options.getOption("m2").setRequired(true);
		options.getOption("evoJar").setRequired(true);
		options.getOption("inputLoc").setRequired(true);
	}
	
	public static void main(String[] args) throws Exception {
		try {
			CommandLineParser parser = new DefaultParser();
			CommandLine comm = parser.parse(options, args);
			
			String cbPath = comm.getOptionValue("cb");
			File codebase = new File(cbPath);
			List<File> repos = new ArrayList<File>();
			for (File dir: codebase.listFiles()) {
				if (dir.isDirectory() && !dir.getName().startsWith(".")) {
					logger.info("Detect repo: " + dir.getAbsolutePath());
					repos.add(dir);
				}
			}
			logger.info("Total detected repos: " + repos.size());
			
			String m2Path = comm.getOptionValue("m2");
			String evoJar = comm.getOptionValue("evoJar");
			String genInput = comm.getOptionValue("inputLoc");
			
			for (File repo: repos) {
				String pomPath = repo.getAbsolutePath() + "/pom.xml";
				String repoClassPath = repo.getAbsolutePath() + "/target/classes";
				
				File pomFile = new File(pomPath);
				File m2Dir = new File(m2Path);
				logger.info("Confirm pom file: " + pomFile.getAbsolutePath());
				logger.info("Confim m2 dir: " + m2Dir.getAbsolutePath());
						
			    List<String> classPaths = getClasspathFromMavenProject(pomFile, m2Dir);
			    StringBuilder pathBuilder = new StringBuilder();
			    classPaths.forEach(c->{
			    	pathBuilder.append(c + ";");
			    });
			    String classPathString = pathBuilder.substring(0, pathBuilder.length() - 1);
			    //logger.info("classpath = " + classPathString);
			    
			    ProcessBuilder pb = new ProcessBuilder();
			    List<String> commands = new ArrayList<String>();
			    commands.add("java");
			    commands.add("-jar");
			    commands.add(evoJar);
			    commands.add("-base_dir");
			    commands.add(genInput);
			    commands.add("-Dsearch_budget=10");
			    commands.add("-Dstopping_condition=MaxTime");
			    commands.add("-target");
			    commands.add(repoClassPath);
			    commands.add("-projectCP");
			    commands.add(classPathString);
			    pb.command(commands);
			    logger.info("EvoSuite command: " + pb.command());
			    
			    /*Process p = pb.start();
			    BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
			    String msg = null;
			    while ((msg = br.readLine()) != null) {
			    	logger.info(msg);
			    }
			    int ret = p.waitFor();
			    
			    if (ret != 0) {
			    	logger.error("Invalid termination");
			    } else {
			    	logger.error("Complete process");
			    }*/
			}
			
			String repoPath = comm.getOptionValue("repo");
			String pomPath = repoPath + "/pom.xml";
			String repoClassPath = repoPath + "/target/classes";
		} catch (Exception ex) {
			logger.error("Error: ", ex);
		}
	}
	
	public static List<String> getClasspathFromMavenProject(File projectPom, File localRepoFolder) {
		try {
		    MavenProject proj = loadProject(projectPom);
		    
		    /*proj.setRemoteArtifactRepositories(
		        Arrays.asList(
		            (ArtifactRepository) new MavenArtifactRepository(
		                "maven-central", "http://repo1.maven.org/maven2/", new DefaultRepositoryLayout(),
		                new ArtifactRepositoryPolicy(), new ArtifactRepositoryPolicy()
		            )
		        )
		    );*/
		    
		    RemoteRepository remoteRepo = new RemoteRepository("maven-central", "default", "http://repo1.maven.org/maven2/");
		    Collection<RemoteRepository> remotes = Arrays.asList(remoteRepo);

		    List<String> classpath = new ArrayList<String>();
		    Aether aether = new Aether(remotes, localRepoFolder);

		    List<org.apache.maven.model.Dependency> dependencies = proj.getDependencies();
		    Iterator<org.apache.maven.model.Dependency> it = dependencies.iterator();

		    while (it.hasNext()) {
		      org.apache.maven.model.Dependency depend = it.next();
		      if (depend.getGroupId().equals("junit") && depend.getArtifactId().equals("junit")) {
		    	  continue ;
		      }
		      
		      final Collection<Artifact> deps = aether.resolve(
		        new DefaultArtifact(depend.getGroupId(), depend.getArtifactId(), depend.getClassifier(), depend.getType(), depend.getVersion()),
		        JavaScopes.RUNTIME
		      );

		      Iterator<Artifact> artIt = deps.iterator();
		      while (artIt.hasNext()) {
		        Artifact art = artIt.next();
		        classpath.add(art.getFile().getAbsolutePath());
		        //classpath = classpath + " " + art.getFile().getAbsolutePath();
		      }
		    }

		    return classpath;
		} catch (Exception ex) {
			logger.error("Error: ", ex);
		}
		
		return null;
	}
	
	public static MavenProject loadProject(File pomFile) {
		try {
			MavenProject ret = null;
			MavenXpp3Reader mavenReader = new MavenXpp3Reader();
			
			if (pomFile != null && pomFile.exists()) {
				FileReader reader = null;
				try {
					reader = new FileReader(pomFile);
					Model model = mavenReader.read(reader);
					model.setPomFile(pomFile);
					ret = new MavenProject(model);
				} finally {
					reader.close();
				}
			}
			return ret;
		} catch (Exception ex) {
			logger.error("Error: ", ex);
		}
		return null;
	}
}
