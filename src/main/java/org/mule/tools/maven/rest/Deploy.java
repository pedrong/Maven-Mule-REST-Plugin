package org.mule.tools.maven.rest;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

@Mojo(name = "deploy", requiresDirectInvocation = true, requiresDependencyResolution = ResolutionScope.RUNTIME)
public class Deploy extends AbstractMojo {
	public static final String DEFAULT_NAME = "MuleApplication";

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
	@Parameter
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
	@Parameter
	protected String username;

	/**
	 * Password for MMC user
	 */
	@Parameter
	protected String password;

	/**
	 * Directory containing the app resources.
	 */
	@Parameter(defaultValue = "${basedir}/src/main/app")
	protected File appDirectory;

	/**
	 */
	@Parameter
	protected URL[] mmcApiUrls;

	/**
	 */
	@Parameter
	protected String[] agentApiUrls;

	/**
	 */
	@Parameter
	protected String serverGroup;

	/**
	 */
	@Parameter
	protected String clusterName;
	
	/**
	 * Name of the generated Mule App.
	 */
	@Parameter (defaultValue = "${project.packaging}", required = true)
	protected String packaging;
	
	protected MuleRest muleRest;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
//		if (name == null) {
//			getLog().info("Name is not set, using default \"{}\"", DEFAULT_NAME);
//			name = DEFAULT_NAME;
//		}
		if (deploymentName == null) {
			getLog().info("DeploymentName is not set, using application name [" + name + "]");
			deploymentName = name;
		}
//		if (version == null) {
//			version = new SimpleDateFormat("MM-dd-yyyy-HH:mm:ss").format(Calendar.getInstance().getTime());
//			logger.info("Version is not set, using a default of the timestamp: {}", version);
//		}
//		if (username == null || password == null) {
//			throw new MojoFailureException((username == null ? "Username" : "Password") + " not set.");
//		}
//		if (outputDirectory == null) {
//			throw new MojoFailureException("outputDirectory not set.");
//		}
//		if (finalName == null) {
//			throw new MojoFailureException("finalName not set.");
//		}
//		if (clusterName == null && serverGroup == null) {
//			throw new MojoFailureException("serverGroup nor clusterName were set.");
//		}
		
		if (packaging.equals("mule-domain")) {
			deployDomain(name, getMuleZipFile(outputDirectory, finalName));
		} else {
			try {
				//validateProject(appDirectory);
				muleRest = buildMuleRest();
				String versionId = muleRest.restfullyUploadRepository(name, version, getMuleZipFile(outputDirectory, finalName));
				String deploymentId = muleRest.restfullyCreateDeployment(serverGroup, deploymentName, clusterName, versionId);
				muleRest.restfullyDeployDeploymentById(deploymentId);
			} catch (Exception e) {
				throw new MojoFailureException("Error in attempting to deploy archive: " + e.toString(), e);
			}
		}
	}

	protected File getMuleZipFile(File outputDirectory, String filename) throws MojoFailureException {
		File file = new File(outputDirectory, filename + ".zip");
		if (!file.exists()) {
			throw new MojoFailureException("There no application ZIP file generated : check that you have configured the maven-mule-plugin to generated the this file");
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

	protected MuleRest buildMuleRest() {
		return new MuleRest(mmcApiUrls[0], username, password);
	}
	
	protected void deployDomain(String domainName, File domainFile) throws MojoFailureException{
        getLog().info("Starting auto deployment of domain [" + domainName + "]");
        
        for (String baseUrl : agentApiUrls) {
        	String url = baseUrl.trim() + "/domains/" + domainName;
        	
        	getLog().info("Uploading domain file " + domainFile + " to " + url);
        	
        	try {
	        	InputStreamEntity entity = new InputStreamEntity(new FileInputStream(domainFile));
	            HttpPut put = new HttpPut(url);
	            put.setEntity(entity);
	            
	            HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
	        	HttpClient client = httpClientBuilder.build();
            
            	HttpResponse response = client.execute(put);
            } catch (Exception e) {
            	throw new MojoFailureException("", e);
            }
        }
	}

}