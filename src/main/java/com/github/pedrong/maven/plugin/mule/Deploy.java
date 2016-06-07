package com.github.pedrong.maven.plugin.mule;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

@Mojo(name = "deploy", requiresDirectInvocation = true, requiresDependencyResolution = ResolutionScope.RUNTIME)
public class Deploy extends AbstractMojo {

	/**
	 * Directory containing the generated Mule App.
	 */
	@Parameter(defaultValue = "${project.build.directory}", required = true)
	protected File outputDirectory;
	
	/**
	 * Name of the generated Mule App.
	 */
	@Parameter(defaultValue = "${project.build.finalName}", required = true)
	protected String finalName;

	/**
	 * The name that the application will be deployed as. Default is
	 * ${project.build.finalName}
	 */
	@Parameter(defaultValue = "${project.build.finalName}", required = true)
	@com.beust.jcommander.Parameter(names = {"-n", "--name"}, required = false, description = "Name of the application/domain. If not provided the application will try to get the name from the file name.")
	protected String name;

	/**
	 * The name that the application will be deployed as. Default is same as
	 * ${name}
	 */
	@Parameter(property = "mmcDeploymentName")
	protected String deploymentName;

	/**
	 * The version that the application will be deployed as. Default is the
	 * current time in milliseconds.
	 */
	@Parameter(defaultValue = "${project.version}", required = true)
	@com.beust.jcommander.Parameter(names = {"-v", "--version"}, required = false, description = "Version to be used on deployment. If not provided the application will try to get the version from the file name.")
	protected String version;

	/**
	 * The username for MMC
	 */
	@Parameter(property = "mmcUsername")
	@com.beust.jcommander.Parameter(names = {"-u", "--mmcUsername"}, required = false, description = "MMC username. Required for application (not domains) deployment.")
	protected String username;

	/**
	 * Password for MMC user
	 */
	@Parameter(property = "mmcPassword")
	@com.beust.jcommander.Parameter(names = {"-p", "--mmcPassword"}, required = false, description = "MMC password. Required for application (not domains) deployment.")
	protected String password;

	/**
	 * Directory containing the app resources.
	 */
	@Parameter(defaultValue = "${basedir}/src/main/app")
	protected File appDirectory;

	/**
	 */
	@Parameter(property = "mmcApiUrls")
	@com.beust.jcommander.Parameter(names = {"-m", "--mmcApiUrls"}, required = false, description = "MMC API URLs. Required for application deployment. For domain deployment please use --agentApiUrls")
	protected List<String> mmcApiUrls = new ArrayList<String>();

	/**
	 */
	@Parameter(property = "agentApiUrls")
	@com.beust.jcommander.Parameter(names = {"-a", "--agentApiUrls"}, required = false, description = "Mule Agent API URLs. Required for domain deployment. For application deployment please use --mmcApiUrls")
	protected List<String> agentApiUrls = new ArrayList<String>();

	/**
	 */
	@Parameter(property = "serverGroup")
	@com.beust.jcommander.Parameter(names = {"-g", "--serverGroup"}, required = false, description = "Server group name to be deployed to. Ignored if --clusterName is provided. Valid only for application deployment.")
	protected String serverGroup;

	/**
	 */
	@Parameter(property = "clusterName")
	@com.beust.jcommander.Parameter(names = {"-c", "--clusterName"}, required = false, description = "Cluster name where the artifact will be deployed. Valid only for application deployment.")
	protected String clusterName;
	
	/**
	 * Name of the generated Mule App.
	 */
	@Parameter (defaultValue = "${project.packaging}", required = true)
	protected String packaging;
	
	@com.beust.jcommander.Parameter(names = {"-f", "--file"}, required = true, description = "File to be deployed.")
	protected File artifactFile;
	
	protected MuleRest muleRest;

	@com.beust.jcommander.Parameter(names = {"-h", "--help"}, help = true, description = "Help information.")
	protected boolean commandLineHelp;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		
		File muleDeployableFile = getMuleZipFile(outputDirectory, finalName);
		
		if (packaging.equalsIgnoreCase("mule-domain")) {
			deployMuleDomain(muleDeployableFile);
		} else {
			deployMuleApplication(muleDeployableFile);
		}
	}

	protected void validateConfigurationForDomainDeploy() throws MojoFailureException {
		if (agentApiUrls.size() == 0) {
			throw new MojoFailureException("Domain deployment is only supported via Mule Agent API. Please inform agentApiUrls.");
		}
	}

	protected void validateConfigurationForApplicationDeploy() throws MojoFailureException {
		if (mmcApiUrls.size() == 0) {
			throw new MojoFailureException("Currently mule application deployment is only supported via MMC REST API. Please inform mmcApiUrls.");
		}
		
		if (username == null || password == null) {
			throw new MojoFailureException((username == null ? "Username" : "Password") + " not set.");
		}
		
		if (clusterName == null && serverGroup == null) {
			throw new MojoFailureException("serverGroup nor clusterName were set.");
		}
		
		if (deploymentName == null) {
			getLog().info("DeploymentName is not set, using application name [" + name + "]");
			deploymentName = name;
		}
	}

	protected void deployMuleApplication(File appFile) throws MojoFailureException {
		validateConfigurationForApplicationDeploy();
		
		for (String mmcApiUrl : mmcApiUrls) {
			try {
				//validateProject(appDirectory);
				getLog().info("Starting deployment of application file " + appFile);
				
				muleRest = buildMuleRest(new URL(mmcApiUrl));
				String versionId = muleRest.restfullyUploadRepository(name, version, appFile);
				String deploymentId = muleRest.restfullyCreateDeployment(serverGroup, deploymentName, clusterName, versionId);
				muleRest.restfullyDeployDeploymentById(deploymentId);
			} catch (IOException e) {
				throw new MojoFailureException("Error in attempting to deploy archive: " + e.toString(), e);
			}
		}
	}

	protected MuleRest buildMuleRest(URL mmcApiUrl) {
		return new MuleRest(mmcApiUrl, username, password);
	}

	protected File getMuleZipFile(File outputDirectory, String filename) throws MojoFailureException {
		File file = new File(outputDirectory, filename + ".zip");
		if (!file.exists()) {
			throw new MojoFailureException("Could not find file [" + file + "] for deployment.");
		}
		return file;
	}

	protected void validateProject(File appDirectory) throws MojoExecutionException {
		File muleConfig = new File(appDirectory, "mule-config.xml");
		File deploymentDescriptor = new File(appDirectory, "mule-deploy.properties");

		if ((muleConfig.exists() == false) && (deploymentDescriptor.exists() == false)) {
			throw new MojoExecutionException("No mule-config.xml or mule-deploy.properties");
		}
	}

	protected void deployMuleDomain(File domainFile) throws MojoFailureException {
        getLog().info("Starting deployment of domain [" + name + "]");
        
        validateConfigurationForDomainDeploy();
        
        for (String baseUrl : agentApiUrls) {
        	String url = baseUrl.trim() + "/domains/" + name;
        	
        	getLog().info("Uploading domain file " + domainFile + " to " + url);
        	
        	try {
	        	InputStreamEntity entity = new InputStreamEntity(new FileInputStream(domainFile));
	            HttpPut put = new HttpPut(url);
	            put.setEntity(entity);
	            put.setHeader("Content-Type", "application/octet-stream");
	            
	            HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
	        	CloseableHttpClient client = httpClientBuilder.build();
            	client.execute(put);
            	client.close();
            } catch (IOException e) {
            	throw new MojoFailureException("Error in attempting to deploy archive: " + e.toString(), e);
            }
        }
	}

}