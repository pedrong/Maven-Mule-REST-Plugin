package com.github.pedrong.maven.plugin.mule;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;

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
	protected String version;

	/**
	 * The username for MMC
	 */
	@Parameter(property = "mmcUsername")
	protected String username;

	/**
	 * Password for MMC user
	 */
	@Parameter(property = "mmcPassword")
	protected String password;

	/**
	 * Directory containing the app resources.
	 */
	@Parameter(defaultValue = "${basedir}/src/main/app")
	protected File appDirectory;

	/**
	 */
	@Parameter(property = "mmcApiUrls")
	protected URL[] mmcApiUrls;

	/**
	 */
	@Parameter(property = "agentApiUrls")
	protected String[] agentApiUrls;

	/**
	 */
	@Parameter(property = "serverGroup")
	protected String serverGroup;

	/**
	 */
	@Parameter(property = "clusterName")
	protected String clusterName;
	
	/**
	 * Name of the generated Mule App.
	 */
	@Parameter (defaultValue = "${project.packaging}", required = true)
	protected String packaging;
	
	protected MuleRest muleRest;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		
		File muleDeployableFile = getMuleZipFile(outputDirectory, finalName);
		
		if (packaging.equalsIgnoreCase("mule-domain")) {
			validateConfigurationForDomainDeploy();
			deployMuleDomain(muleDeployableFile);
		} else {
			validateConfigurationForApplicationDeploy();
			deployMuleApplication(muleDeployableFile);
		}
	}

	protected void validateConfigurationForDomainDeploy() throws MojoFailureException {
		if (agentApiUrls.length == 0) {
			throw new MojoFailureException("Domain deployment is only supported via Mule Agent API. Please inform agentApiUrls.");
		}
	}

	protected void validateConfigurationForApplicationDeploy() throws MojoFailureException {
		if (mmcApiUrls.length == 0) {
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
		for (URL mmcApiUrl : mmcApiUrls) {
			try {
				//validateProject(appDirectory);
				getLog().info("Starting deployment of application file " + appFile);
				
				muleRest = buildMuleRest(mmcApiUrl);
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