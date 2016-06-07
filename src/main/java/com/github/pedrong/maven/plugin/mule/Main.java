package com.github.pedrong.maven.plugin.mule;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipFile;

import org.apache.maven.monitor.logging.DefaultLog;
import org.codehaus.plexus.logging.console.ConsoleLogger;

import com.beust.jcommander.JCommander;

public class Main {

	private final static Pattern patternNameVersion = Pattern.compile("(\\w+([-_]{0,1}\\w)*)([-_](\\d+(\\.\\d+)*[-_]{0,1}\\w*)){0,1}.zip");

	public static void main(String[] args) throws Exception {
		Deploy deployHelder = new Deploy();
		JCommander jCommander = new JCommander(deployHelder, args);
		
		// Shows usage
		if (deployHelder.commandLineHelp) {
			jCommander.usage();
			System.exit(0);
		}
		
		File muleDeployableFile = deployHelder.artifactFile;
		
		// Checks if the file is a zip archive
		if (!muleDeployableFile.getName().endsWith(".zip")) {
			System.err.println("File is not zip.");
			jCommander.usage();
			System.exit(-1);
		}
		
		// If name or version were not provided it tries to get these from the file name using regex.
		if (deployHelder.name == null || deployHelder.version == null) {
			Matcher matcher = patternNameVersion.matcher(muleDeployableFile.getName());
			
			if (matcher.find()) {
				if (deployHelder.name == null && matcher.groupCount() >= 1) {
					deployHelder.name = matcher.group(1);
				} else {
					System.err.println("Name was not especified and could be retrieved from the file name.");
					jCommander.usage();
					System.exit(-1);
				}
				
				if (deployHelder.version == null && matcher.groupCount() >= 4) {
					deployHelder.version = matcher.group(4);
				} else {
					System.err.println("Version was not especified and could be retrieved from the file name.");
					jCommander.usage();
					System.exit(-1);
				}
			}
		}
		
		// Sets logs
		deployHelder.setLog(new DefaultLog(new ConsoleLogger()));
		
		// Find out if the zip file is a domain or a app
		ZipFile zipFile = new ZipFile(muleDeployableFile);
		
		try {
			if (zipFile.getEntry("mule-domain-config.xml") != null) {
				if (deployHelder.agentApiUrls == null || deployHelder.agentApiUrls.isEmpty()) {
					System.err.println("Missing agentApiUrls in order to deploy domains.");
					System.exit(-1);
				}
				// Deploy domain
				deployHelder.deployMuleDomain(muleDeployableFile);
			} else if (zipFile.getEntry("mule-deploy.properties") != null) {
				if (deployHelder.mmcApiUrls == null || deployHelder.mmcApiUrls.isEmpty()) {
					System.err.println("Missing mmcApiUrls in order to deploy applications.");
					System.exit(-1);
				}
				// Deploy app
				deployHelder.deployMuleApplication(muleDeployableFile);
			}
		} finally {
			zipFile.close();
		}
		
	}

}
