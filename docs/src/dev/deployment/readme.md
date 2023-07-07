---
section: Development
title: Deployment
layout: default
permalink: dev/deployment/index.html
previous: ../configuration/
---

#Deployment

##<a name="building-your-project"></a>Building Your Project

To build your Influent project on the command line, execute the following command in your root working directory:

    mvn clean install

###Server-Side Libraries

The full collection of Influent libraries and transitive dependencies (other libraries needed) is bundled at the tail end of the `influent-server` build and placed in a ZIP file in the `influent-server/target` directory for "skinny war" deployments. Alternatively, these libraries can be included in your app WAR file in conventional fashion, as in the `bitcoin` and `kiva` example apps.

###Client-Side Resources

The full collection of client-side resource files (JavaScript, CSS, etc.) are bundled at the tail end of the `influent-client` build. Two versions are placed in ZIP files in the `influent-client/target` directory:

-   A raw source file useful for debugging
-   A minimized form for optimal loading in deployed scenarios

##<a name="web-app-deployment"></a>Web App Deployment

Typically, you will compile your Influent application to a WAR file. In some cases, it might be advantageous to share Influent libraries across applications, or to separate them from your application code for other reasons. For instance, you may wish to customize two applications to two different data sets, or you may wish to make it easier to drop in new Influent updates without having to rebuild your app. This approach is called the "skinny war" approach, where libraries are supplied outside of your war file.

###Tomcat Configuration

#### Skinny WAR

The "skinny war" approach can be implemented in a Tomcat configuration in catalina.properties using the shared.loader property. For instance the following adds lib-influent/ as a shared directory to Tomcat for apps to load jars from, where that directory would then contain the contents of the zip file in influent-server/target produced by the maven build process.

	shared.loader=${catalina.base}/lib-influent/*.jar

EDITOR'S NOTE: An issue has been discovered with this approach which is currently being looked into. In the meantime please use the conventional self-contained WAR approach.

#### Basic Install

1.  Copy the Influent WAR file into your Tomcat webapps folder (e.g., C:\apache-tomcat-8.0.5\webapps).
2.  Ensure that the VM has enough heap space by setting Tomcat options environment variable

	```
	set CATALINA\_OPTS=-Xmx10240m (Windows, no "" around the value)
	export CATALINA\_OPTS="-Xmx10240m" (ksh/bash, "" around the value)
	setenv CATALINA\_OPTS "-Xmx10240m" (tcsh/csh, "" around the value)
	```

3.  If this is your only Tomcat installation, set CATALINA\_HOME to the tomcat install directory. Otherwise, set CATALINA\_BASE to the tomcat install directory for this installation.
4.  Create a subfolder in the bin folder called bin and copy the phantomjs binary for your platform into it. The final path should be \bin\\\phantomjs
5.  Run Tomcat with \bin\startup to deploy the application.

###Standalone Jetty Configuration

The "skinny war" approach can be implemented in a standalone Jetty server by configuring the web app's xml file in contexts, similar to the following example. Similar to above, in this example lib-influent/ is the directory into which you would extract the contents of the zip file produced by the server build process.

```xml
<Configure class="org.eclipse.jetty.webapp.WebAppContext" id="context">
	<!--Specify the path to the influent directory in Jetty here!!!-->
	<Call name="newResource" id="jarpath">
		<Arg>lib-influent/</Arg>
	</Call>

	<!--Trace path to lib directory for debugging purposes (Optional)-->
	<Get class="java.lang.System" name="out">
		<Call name="println">
			<Arg>LOADING INFLUENT LIBS FROM <Ref id="jarpath"><Get name="file"/></Ref></Arg>
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