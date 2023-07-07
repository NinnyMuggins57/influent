---
section: Community
subsection: Developer Docs
chapter: Reference
topic: server.config
permalink: community/developer-docs/reference/server/
layout: submenu
---

# server.config #

The following sections describe the server-side modules and properties in the [server.config](https://github.com/unchartedsoftware/influent/blob/master/influent-app/src/main/resources/server.config) file in the [src/main/resources/](https://github.com/unchartedsoftware/influent/tree/master/influent-app/src/main/resources) folder of your project directory:

- [Build Options](#build-options)
- [Data Access](#data-access)
- [Config Files](#config-files)
- [Cache](#cache)
- [Authentication](#authentication)
- [Image Capture](#image-capture)
- [Pattern Search](#pattern-search)

For many of server-side properties, you can simply use the default values. For a description of the properties that should be changed for every application, see the [Server Configuration](../../how-to/server-config/) topic.

## <a name="build-options"></a>Build Options ##

Several configurable build options allow you to specify your build version number, configure Google Guice bindings and use an alternate client-side configuration file:

<div class="props">
	<table class="summaryTable" width="100%">
		<thead>
			<th scope="col" width="20%">Parameter</th>
			<th scope="col" width="80%">Description</th>
		</thead>
		<tbody>
			<tr>
				<td class="property">app.buildnumber</td>
				<td class="description">Version number applied to your build. By default, the build creation date is appended to the number you specify.</td>
			</tr>
			<tr>
				<td class="property">aperture.server.config.bindnames</td>
				<td class="description">Indicates whether to bind the Aperture server configurations to Google Guice. If this property is set to <i>false</i>, you must implement server-side code.</td>
			</tr>
		</tbody>
	</table>
</div>

### <a name="data-access"></a>Data Access ###

The Data Access sections ([Database Server Properties](#db-server) and [Solr Server Properties](#solr-server)) of the **server.config** file are used to connect Influent to your transaction and entity data.

For detailed instructions on setting these properties, see the [Connect Your Data to Influent](../../how-to/connect-data) topic.

### <a name="config-files"></a> Config Files ###

<div class="props">
	<table class="summaryTable" width="100%">
		<thead>
			<th scope="col" width="20%">Parameter</th>
			<th scope="col" width="80%">Description</th>
		</thead>
		<tbody>
			<tr>
				<td class="property">aperture.client.configfile</td>
				<td class="description">Relative path to your project's client and Aperture JS configuration file. Defaults to <b>client-config.js</b>.</td>
			</tr>
			<tr>
				<td class="property">influent.midtier.property.configfile</td>
				<td class="description">Name of the file that lists the Entity attributes passed through Solr.</td>
			</tr>
			<tr>
				<td class="property">influent.midtier.database.configfile</td>
				<td class="description">
					Name of the file used to:
					<ul>
						<li>Map table and column names in your database to aliases used by Influent</li>
						<li>Pass in other tables you want to make accessible within the application</li>
					</ul>
				</td>
			</tr>
		</tbody>
	</table>
</div>

#### <a name="db-server"></a> Database Server Properties ####

The following properties are used to connect Influent to the database containing your transaction and entity data:

<div class="props">
	<table class="summaryTable" width="100%">
		<thead>
			<tr>
				<th scope="col" width="40%">Property</th>
				<th scope="col" width="60%">Description</th>
			</tr>
		</thead>
		<tbody>
			<tr>
				<td class="property">influent.midtier.user.name</td>
				<td class="description">Username with which to access your database</td>
			</tr>
			<tr>
				<td class="property">influent.midtier.user.password</td>
				<td class="description">Password for the username with which to access your database</td>
			</tr>
			<tr>
				<td class="property">influent.midtier.database.type</td>
				<td class="description">Type of database in which your transaction and entity data is stored (<em>mssql</em>, <em>mysql</em>, <em>hsql</em> or <em>oracle</em>)</td>
			</tr>
			<tr>
				<td class="property">influent.midtier.database.url</td>
				<td class="description">Location of the database in which your transaction and entity data is stored (e.g., <em>jdbc:mysql://mysql.server.come/MyDB</em>)</td>
			</tr>
			<tr>
				<td class="property">influent.midtier.database.driver</td>
				<td class="description">Location of the JDBC driver (e.g., <em>org.hsqldb.jdbcDriver</em>) for the type of database to which you want to connect</td>
			</tr>
		</tbody>
	</table>
</div>
	
**NOTE**: If you have a Microsoft SQL Server database, you can specify its location using the following legacy properties. However, the presence of the **database.url** and **database.driver** properties will automatically override the legacy properties.

- **influent.midtier.server.name** (URI)
- **influent.midtier.server.port**
- **influent.midtier.database.name**

#### <a name="solr-server"></a> Solr Server Properties ####

The following properties are used to connect Influent to the database containing your transaction and entity data:

<div class="props">
	<table class="summaryTable" width="100%">
		<thead>
			<tr>
				<th scope="col" width="40%">Property</th>
				<th scope="col" width="60%">Description</th>
			</tr>
		</thead>
		<tbody>
			<tr>
				<td class="property">influent.midtier.solr.url</td>
				<td class="description">URL of the Solr Server used to enable searches on your entity and transaction data.</td>
			</tr>
		</tbody>
	</table>
</div>

## <a name="cache"></a> Cache ##

Edit the cache options to specify advanced Ehcache configuration properties:

<div class="props">
	<table class="summaryTable" width="100%">
		<thead>
			<tr>
				<th scope="col" width="20%">Cache</th>
				<th scope="col" width="80%">Stores</th>
			</tr>
		</thead>
		<tbody>
			<tr>
				<td class="property">aperture.cms.ehcache.maxSize</td>
				<td class="description">Maximum number of web pages that can be stored in <strong>SimplePageCachingFilter</strong>. Defaults to <i>1000</i>.</td>
			</tr>
			<tr>
				<td class="property">influent.midtier.ehcache.config</td>
				<td class="description">Name of the file containing the Ehcache configuration settings. Defaults to <i>ehcache.xml</i>.</td>
			</tr>
			<tr>
				<td class="property">influent.persistence.cache.name</td>
				<td class="description">Name of the persistence Ehcache. Defaults to <i>persistenceCache</i>.</td>
			</tr>
			<tr>
				<td class="property">influent.dynamic.clustering.cache.name</td>
				<td class="description">Name of the dynamic clustering Ehcache. Defaults to <i>dynamicClusteringCache</i>.</td>
			</tr>
			<tr>
				<td class="property">influent.charts.maxAge</td>
				<td class="description">Maximum age (in seconds) of charts stored in <strong>ChartDataCache</strong>. Defaults to <i>86400</i> (24 hours).</td>
			</tr>
		</tbody>
	</table>
</div>

## <a name="authentication"></a> Authentication ##

The following properties control the optional [Apache Shiro](http://shiro.apache.org/) plugin module, which allows you to enable user authentication for you Influent application.

For detailed instruction on implement Apache Shiro, see the [Server Configuration](../../how-to/server-config/#authentication) topic.

<div class="props">
	<table class="summaryTable" width="100%">
		<thead>
			<tr>
				<th scope="col" width="35%">Property</th>
				<th scope="col" width="65%">Description</th>
			</tr>
		</thead>
		<tbody>
			<tr>
				<td class="property">shiro.hashAlgorithmName</td>
				<td class="description">Specify the hash algorithm used: <em>MD5</em>, <em>SHA-1</em>, <em>SHA-256</em>, <em>SHA-384</em>, or <em>SHA-512</em>.</td>
			</tr>
			<tr>
				<td class="property">shiro.redirectUrl</td>
				<td class="description">Redirect link (path/filename) that forces users to complete the login page. Defaults to <i>/login.jsp</i>.</td>
			</tr>
			<tr>
				<td class="property">shiro.loginUrl</td>
				<td class="description">Direct link (path/filename) to the login page. Defaults to <i>/login.jsp</i></td>
			</tr>
			<tr>
				<td class="property">shiro.successUrl</td>
				<td class="description">Page (path/filename) to display upon successful login. Defaults to <i>/</i>.</td>
			</tr>
		</tbody>
	</table>
</div>

## <a name="image-capture"></a>Image Capture ##

The *PhantomImageModule* properties in the [server.config](https://github.com/unchartedsoftware/influent/blob/master/influent-app/src/main/resources/server.config) file control the PhantomJS module used to export screenshots of the Influent workspace.

<div class="props">
	<table class="summaryTable" width="100%">
		<thead>
			<tr>
				<th scope="col" width="35%">Property</th>
				<th scope="col" width="65%">Description</th>
			</tr>
		</thead>
		<tbody>
			<tr>
				<td class="property">aperture.imagecapture.cms.store</td>
				<td class="description">Name. Defaults to <i>influent</i>.</td>
			</tr>
			<tr>
				<td class="property">aperture.imagecapture.phantomjs.poolsize</td>
				<td class="description">Number of PhantomJS instances available to process image captures.</td>
			</tr>
			<tr>
				<td class="property">aperture.imagecapture.phantomjs.exepath</td>
				<td class="description">Direct link (path/filename) to the PhantomJS executable. Defaults to <i>bin/phantomjs</i>.</td>
			</tr>
			<tr>
				<td class="property">aperture.imagecapture.phantomjs.ssl-certificates-path</td>
				<td class="description">For use when user authentication is enabled. File path (path/filename) to SSL certificates required to use PhantomJS.</td>
			</tr>
		</tbody>
	</table>
</div>

## <a name="pattern-search"></a> Pattern Search Database ##

To connect your plugin pattern search module to Influent, set the value of the **influent.pattern.search.remoteURL** parameter to the location (URL) of your pattern search server.

For detailed instructions on installing and configuring a plugin pattern search module, see the [Search Configuration](../../how-to/search-config/#qube) topic.