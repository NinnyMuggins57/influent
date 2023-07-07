# influent

Influent is a new web based application for visually and interactively following transaction flow, revealing actors and behaviors of potential concern that might otherwise go unnoticed. Summary visualization of transactional patterns and actor characteristics, interactive link expansion and dynamic entity clustering enable Influent to operate effectively at scale with big data sources, in any modern web browser. Influent has been used to explore data sets with millions of entities and hundreds of millions of transactions.

Service Provider Interfaces (SPIs) provide a plugin style framework for developers to provide runtime-injected modules for search, data access, clustering and other services. [Avro](http://avro.apache.org/) is used to define the SPI protocols in cross-language form in [influent-spi](influent-spi/src/main/avro). In process Java service implementations are injected via [Guice](https://code.google.com/p/google-guice/), which may optionally delegate to out of process providers using web services standards such as REST, for which Avro provides convenient serialization.

![Influent example with public Kiva data set](https://raw.github.com/oculusinfo/wiki-assets/master/influent/influent-kiva.png) 

## Building Influent
The Influent web app is a servlet, with server side Java and client side JavaScript + HTML5. To build Influent on the command line you will need the following to be installed first:

 + [Java](http://www.java.com/). We recommend JDK 1.7+.
 + [Maven](http://maven.apache.org/). We recommend version 3.1+

Once installed, execute the following command in the root influent directory:
```
mvn clean install
```
The full collection of influent libraries and transitive dependencies (other libraries needed) will be bundled at the tail end of the `influent-server` build and placed in a zip file in the `influent-server/target` directory for the "skinny war" deployment options described [below](#web-app-deployment). Alternatively those libraries can be included in your influent app war file in conventional fashion, as in the `bitcoin` and `kiva` example apps.

The full collection of client-side resources files (JavaScript, CSS, etc) are bundled at the tail end of the `influent-client` build and two variations are placed in zip files in the `influent-client/target` directory. One contains the file in raw source form useful for debugging, and one contains them in minimized form for optimal loading in a deployed scenario.

## Building and Running Influent Apps
Influent exposes APIs for integration of large enterprise data sources. For ease of integration with conventional relational databases, adapters are provided for MSSQL, Oracle, and MySQL. SQL scripts are provided in the `influent-spi` [dataviews](influent-spi/src/main/dataviews) directory for creating the required Influent Data View tables.

Clustering is configurable for each data set. The integrator chooses ordered fields by which to cluster as well as the fields to compute distributions for that will be mapped to icons in `config.js` in the same way as individual icons (see Kiva's [config.js](kiva/src/main/resources/config.js) as an example). Clustering configuration is documented in the `influent-server` clustering [README](influent-server/src/main/resources/influent/server/clustering/README.md).

Example maven based applications are provided for public Kiva and Bitcoin transaction data, which connect to databases we have made available online for demonstration purposes. To import these applications into Eclipse, for example, use the `File > Import... > Maven > Existing Maven Projects...` command in Eclipse that comes with the [m2e](http://www.eclipse.org/m2e/) plugin.

## Web App Deployment
Typically your Influent application will be compiled to a war file. In some cases it might be advantageous to share Influent libraries across applications, or to separate them from your application code for other reasons. For instance you may wish to customize two applications to two different data sets, or you may wish to make it easier to drop in new Influent updates without having to rebuild your app. This approach is called the "skinny war" approach, where libraries are supplied outside of your war file.

### Tomcat Configuration
The "skinny war" approach can be implemented in a Tomcat configuration in `catalina.properties` using the `shared.loader` property. For instance the following adds `lib-influent/` as a shared directory to Tomcat for apps to load jars from, where that directory would then contain the contents of the zip file in `influent-server/target` produced by the maven build process.
```properties
shared.loader=${catalina.base}/lib-influent/*.jar
```
EDITOR's NOTE: An issue has been discovered with this approach which is currently being looked into. In the meantime please use the conventional self-contained war approach.

### Standalone Jetty Configuration
The "skinny war" approach can be implemented in a standalone Jetty server by configuring the web app's xml file in contexts, similar to the following example. Similar to above, in this example `lib-influent/` is the directory into which you would extract the contents of the zip file produced by the server build process.

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
