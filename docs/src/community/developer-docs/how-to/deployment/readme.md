---
section: Community
subsection: Developer Docs
chapter: How-To
topic: Deployment
permalink: community/developer-docs/how-to/deployment/
layout: submenu
---

# Deployment #

Once you have configured your data and modified the required application parameters, you can build and deploy your custom Influent app.

## <a name="building-your-project"></a> Building Your Project ##

<h6 class="procedure">To build your application to a WAR file</h6>

1. Execute the following command in your project folder:

	```bash
	mvn clean install
	```

2. Access the compiled WAR in the *target/* directory of your project folder.

<h6 class="procedure">To build your application and serve it on localhost</h6>

1. Execute the following command in your project folder:

	```bash
	mvn package jetty:run
	```

2. Access the application at `http://localhost:8080/<project-name>/`.

## <a name="web-app-deployment"></a> Web App Deployment ##

If you chose to build your application to a WAR file, the following sections describe the process of deploying it to [Apache Tomcat](http://tomcat.apache.org/) and [Jetty](http://www.eclipse.org/jetty/) web servers.

### Tomcat Configuration ###

1. Copy the WAR file into your server's *webapps/* folder. Note that the context path of your application will be `/<WAR_file_name>/`.
2. Ensure that the VM has enough heap space by setting a Tomcat options environment variable:
<div class="props">
		<table class="summaryTable" width="100%">
			<thead>
				<tr>
					<th scope="col" width="20%">Shell</th>
					<th scope="col" width="80%">Command</th>
				</tr>
			</thead>
			<tbody>
				<tr>
					<td class="description">Windows</td>
					<td class="description">set CATALINA_OPTS=-Xmx10240m</td>
				</tr>
				<tr>
					<td class="description">ksh/Bash</td>
					<td class="description">export CATALINA_OPTS="-Xmx10240m"</td>
				</tr>
				<tr>
					<td class="description">tcsh/csh</td>
					<td class="description">setenv CATALINA_OPTS "-Xmx10240m"</td>
				</tr>
			</tbody>
		</table>
	</nav>
</div>
3. If this is your only Tomcat installation, set *CATALINA\_HOME* to the Tomcat install directory. Otherwise, set *CATALINA\_BASE* to the Tomcat install directory for this installation.
4. Create a subfolder in your server's *bin/* folder called *bin/* and copy in the appropriate [PhantomJS](http://phantomjs.org/download.html) binary for your platform. The final path should be */bin/bin/phantomjs/*.
5. Run Tomcat using the platform-appropriate startup script (*.sh* or *.bat*) in your server's *bin/* folder.

### Jetty Configuration ###

Jetty supports both conventional WAR deployments and skinny WAR deployments, where Influent libraries are supplied outside of your app WAR file so they can be shared across applications or separated from your application code. For example, you may wish to customize two applications to different datasets or make it easier to drop in new Influent updates without having to rebuild your app.

#### Conventional Deployment ####

1. Copy the WAR file into your server's *webapps/* folder. Note that the context path of your application will be `/<WAR_file_name>/`.
2. Create a subfolder in your server's *bin/* folder called *bin/* and copy in the appropriate [PhantomJS](http://phantomjs.org/download.html) binary for your platform. The final path should be */bin/bin/phantomjs/*.
3. Execute the following command to start your Jetty server and set the proper heap space:

	```bash
	java -Xmx10240m -jar start.jar
	```

#### Skinny WAR Deployment ####

The full collection of Influent libraries and transitive dependencies (other libraries needed) is bundled in a ZIP file in the *influent-server/target/* directory. 

The full collection of client-side resource files (JavaScript, CSS, etc.) are bundled at the tail end of the **influent-client** build. Two versions are placed in ZIP files in the *influent-client/target/* directory:

- A raw source file useful for debugging
- A minimized form for optimal loading in deployed scenarios

The "skinny war" approach can be implemented in a standalone Jetty server by configuring the web app's XML file in contexts, similar to the following example. In this example, *lib-influent/* is the directory into which you would extract the contents of the ZIP file produced by the server build process.

```xml
<Configure class="org.eclipse.jetty.webapp.WebAppContext" id="context">
	<!--Specify the path to the influent directory in Jetty here!!!-->
	<Call name="newResource" id="jarpath">
		<Arg>lib-influent/</Arg>
	</Call>

	<!--Trace path to lib directory for debugging purposes (Optional)-->
	<Get class="java.lang.System" name="out">
		<Call name="println">
			<Arg>LOADING INFLUENT LIBS FROM<Ref id="jarpath"><Get name="file"/></Ref></Arg>
		</Call>
	</Get>

	<!--Set the customized class loader (Boiler Plate)-->
	<Get name="class"><Get name="classLoader" id="parentClassLoader"/></Get>
	<Set name="classLoader">
		<New class="org.eclipse.jetty.webapp.WebAppClassLoader">
			<Arg><Ref id="parentClassLoader"/></Arg>
			<Arg><Ref id="context"/></Arg>
			<Call name="addJars">
				<Arg><Ref id="jarpath"/></Arg>
			</Call>
		</New>
	</Set>
</Configure>
```

<div class="git">
	<h2>Interested in Learning More?</h2>

	<ul>
		<li><a href="../../../tour/">Tour</a>: Take our tour to learn more about Influent.
		<li><a href="../../../docs/user-guide/">Docs</a>: Learn how to use Influent to explore your large-scale transaction flow data.
		<li><a href="../../../demos/">Live Examples</a>: Explore live examples of the capabilities of Influent.
		<li><a href="../../../contact/">Contact</a>: Contact Uncharted for more information about Influent or to submit technical suggestions/contributions.
	</ul>
</div>