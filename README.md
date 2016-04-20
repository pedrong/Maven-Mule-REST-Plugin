#Maven Mule Plugin#

This project was forked from https://github.com/NicholasAStuart/Maven-Mule-REST-Plugin. Among many changes, the main one is that I have added support for Mule Domain deployment through Mule Agent (https://docs.mulesoft.com/mule-agent/v/1.4.0/).

This is a personal project and is not affiliated with MuleSoft or the maven mule plugin in any way.

Example:

	<project>
		...
		<build>
			<plugins>
				<plugin>
					<groupId>com.github.pedrong</groupId>
					<artifactId>mule-maven-plugin</artifactId>
					<version>1.3.0-SNAPSHOT</version>
				</plugin>
			</plugins>
		</build>
		...
	</project>

# Calling the plugin #

There is only one goal, deploy. To call the plugin, do the following

	mule-maven-plugin:deploy
	
This goal will
*   delete an existing mule application archive from the MMC Repository if version contains "SNAPSHOT"
*	upload the mule application archive to the MMC Repository
*	delete an existing deployment having the same application name
*	create a new deployment this the uploaded archive, with target the given serverGroup
*	perform a deploy request to make MMC deploy into target server group

## Security ##
In order to post to the Mule Repository, you need only these permissions:

*	Repository Read 
*	Repository Modify

## Configuration Options ##
<table>
	<tr>
		<th>Property
		<th>Description
		<th>Default
<tr>
	<td>
		mmcApiUrls
	<td>
		The URLs of the Mule MMC(s) API (usually .../api). Not used for domain deployment. See agentApiUrls below.
	<td>
		http://localhost:8585/mmc/api
<tr>
	<td>
		agentApiUrls
	<td>
		The URLs of the Mule Agent(s) API. Mandatory for domain deployment.
	<td>

<tr>
	<td>
		name
	<td>
		What to name the application/domain when it is uploaded to the repository
	<td>
		${project.build.finalName}
<tr>
	<td>
		deploymentName
	<td>
		What to name the deployment when it is uploaded to the repository
	<td>
		same as <code>name</code>
<tr>
	<td>
		version
	<td>
		What version to give the software when it is uploaded to the repository
	<td>
		${project.version}
<tr>
	<td>
		serverGroup
	<td>
		The name of the target Mule serverGroup
	<td>
<tr>
	<td>
		clusterName
	<td>
		The name of the target Mule cluster
	<td>
<tr>
	<td>
		password
	<td>
		The password to the Mule MMC API.
	<td>
<tr>
	<td>
		username
	<td>
		The username to the Mule MMC API.
	<td>
</table> 
