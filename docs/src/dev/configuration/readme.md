---
section: Development
title: Configuration
layout: default
permalink: dev/configuration/index.html
previous: ../installation/
next: ../deployment/
---

#Configuration

When configuring your first Influent application, we recommend you copy one of the provided example applications as a template. Choose the one which most closely aligns with your source data:

- Kiva
	- Three separate account types (lenders, borrowers and partners), each of which has its own specific set of entity details
	- Entity search enabled through [Apache Solr™](http://lucene.apache.org/solr/), an optional plugin module that introduces "fuzzy" search capabilities to adjust for misspellings and return results that are similar to the user's search criteria.
- Bitcoin
	- One account type (Bitcoin account) with a limited set of entity details
	- Entity search enabled through simple database search, which cannot adjust for misspellings

##<a name="configuring-databases"></a>Configuring Databases

Influent requires two types of databases:

-   **Transaction Database**, which stores the following information about each transaction:

    -   Identities of entities involved, including who initiated the transaction and who received it
    -   Date and time at which the transaction occurred
    -   Value of the transaction

    For Influent to correctly visualize transaction flow, you must transform your Transaction database using the **DataViewTables.sql** script in the `influent-spi/src/main/dataviews` directory.

-   **Entity Database**, which stores detailed information about each of the entities involved in your transaction data. You do not need to transform this database before connecting it to Influent. Instead, you must specify the fields that you want users to be able to search within the Influent workspace.

###Supported Databases

For ease of integration with conventional relational databases, Influent provides adapters for:

-   Microsoft SQL Server<sup>®</sup>
-   Oracle Database
-   HSQLDB
-   MySQL

###<a name="transaction-database"></a>Transaction Database

Influent builds visual representations of transaction flow information using your source data. In order for Influent to read your transaction data properly, you must transform it using the **DataViewTables.sql** script in the `influent-spi/src/main/dataviews` directory.

####Database Requirements

Before you execute any of the commands in the SQL script, ensure that your transaction data contains the following information for every transaction:

-   **Source ID** (varchar): A unique ID representing the actor who initiated (sent) the transaction to the destination
-   **Destination ID** (varchar): A unique ID representing the actor who received the transaction from the source
-   **Amount** (float): The value of the transaction between the two actors. For financial data, this would be the amount in a universal currency like USD.
-   **Date and Time** (datetime): The date and time at which the transaction occurred

####Executing the Query

To use the script:

1.  Connect to the SQL Server on which your Transaction database is located.
2.  Open the **DataViewTables.sql** script in the `influent-spi/src/main/dataviews` directory.
3.  Scroll down to the Data View Drivers section and edit the insert statement to reflect your current Transaction database schema:
    -   **RAW\_DATA\_SOURCE**: Specify the name of the database in which your raw transaction details are stored
    -   **source\_id** (varchar): Specify the column in which the unique source IDs are stored
    -   **dest\_id** (varchar): Specify the column in which the unique destination IDs are stored
    -   **amount** (float): Specify the column in which the amount (value) of the transactions are stored
    -   **dt** (datetime): Specify the column in which the date and time at which the transactions occurred are stored

4.  It is recommended you execute the SQL script on your Transaction database one statement at a time in order to facilitate troubleshooting if you encounter errors.

####Resulting Databases

After you execute the DataViewTables.sql script, your Transaction database should contain the three sets of additional tables:

-   **FinEntity Tables**: Used to build all aggregate (by time) flow diagrams and build the time series charts on entities (aggregated by day).
    -   **FinEntity**
    -   **FinEntityDaily**
    -   **FinEntityWeekly**
    -   **FinEntityMonthly**
    -   **FinEntityQuarterly**
    -   **FinEntityYearly**
-   **FinFlow Tables**:Used to build aggregate flow diagrams (by time) and the highlighted sub-section of the time series charts on entities.
    -   **FinFlow**
    -   **FinFlowDaily**
    -   **FinFlowWeekly**
    -   **FinFlowMonthly**
    -   **FinFlowQuarterly**
    -   **FinFlowYearly**

###<a name="entity-database"></a>Entity Database

####Configuring Apache Solr (Optional)

[Apache Solr™](http://lucene.apache.org/solr/) is an optional plugin module for Influent. Solr enables you to implement entity searches within Influent that can adjust for misspellings and return results that are similar to the user's search criteria. These "fuzzy" search capabilities are not available without a search platform like Solr. Otherwise, you must configure Influent to perform simple database queries that find the exact search terms entered by the user.

To enable "fuzzy" search capabilities, you must install the Solr platform and import your Entity database. Configuring Influent to use Solr includes the following actions:

1.  [Installing Apache Solr](#installing-apache-solr)
2.  [Creating a New Core Directory](#creating-new-core-directory)
3.  [Defining the Solr Schema](#defining_solr_schema)
4.  [Choosing Which Fields to Import](#specifying-search-fields)
5.  [Adding a New Core](#adding-new-core)
6.  [Importing Your Data](#importing-your-data)
7.  [Configuring Influent's Advanced Search Dialog](#specifying-searchable-fields)
8.  [Editing the Influent App Properties](#editing-influent-app-properties)

#####Installing Apache Solr

Create an instance of the Solr platform (version 4.1.0 or newer). For more information on installing Solr, see the Tutorials and other documentation available on the Apache Solr website (<http://lucene.apache.org/solr/>).

#####Creating a New Core Directory

In Solr, the core directory stores configuration files that detail the data fields you want to make searchable within Influent and the way in which Solr executes those searches. Note that these steps must be performed before you try to create a new core within the Solr Admin console.

1.  Once your Solr installation is complete, browse to the *solr* folder in your Solr working directory and create a subfolder for the new Solr core that will contain your Entity data. Name this folder after your data source or simply call it *influent* (we will use this name for the purposes of this example).
2.  Create two subfolders within the *influent* folder:
    -   Copy and paste the *conf* folder and all its contents from *example/example-DIH/solr/db/*. This example contains Solr Data Import Handler configuration files that will be useful in importing your Entity relational database.
    -   Create an empty *data* folder

#####Defining the Solr Schema

The **schema.xml** file in the *conf* folder you copied in the previous step is used to specify the schema of the Solr table in which your entity details will be imported. To edit the **schema.xml** file:

1.  Browse to the *conf* folder you copied in the previous step and open the **schema.xml** file.
2.  Add each of the columns you want to be able to import in the following format. For more information on the schema.xml format, the Solr SchemaXml wiki page (http://wiki.apache.org/solr/SchemaXml).
3.  Remove or comment out any **\<field\>** elements defined in the source file that you do not need.
4.  When you have added all of the columns, save the **schema.xml** file.

```
name	(Required)
	Unique alphanumeric string that represents the name of the field; cannot
	start with a digit.

type	(Required)
	Indicates the field type (e.g., string, double or date); use only values
	defined by the <fieldType> elements in the schema.xml file.

indexed	(Optional)
	Indicates whether the field should be indexed so it can be searched or
	sorted (true/false)

stored	(Optional)
	Indicates whether the field can be retrieved (true/false)

required	(Optional)
	Indicates whether the field is required. Errors will be generated if the
	data you import does not contain values for any required fields.
	(true/false)

multiValued	(Optional)
	Indicates whether  the field can contain multiple values for a single
	entity (true/false)
```

#####Choosing Which Fields to Import

To specify the fields you want to import into Solr, you must edit the **db-data-config.xml** file in the *conf* folder you copied in the previous step. The following procedures describe the high-level process of editing this file. For complete details on the dataconfig schema, see the Solr DataImportHandler Wiki page (<http://wiki.apache.org/solr/DataImportHandler>).

1. Edit the attributes of the `<dataSource>` element to specify the type, location and login credentials of the database you want to import:

	-   **driver**: The location of the JDBC driver for the type of database you want to import (e.g., *org.hsqldb.jdbcDriver*).
	-   **url**: The location of the database you want to import.
	-   **user**: Username required to connect to the database.
	-   **password**: Password required to connect to the database.

2. Edit the `<document>` element so it contains one or more `<entity>` elements. Entities generally correspond to the different account types in Influent, each of which has its own unique set of searchable details. For each entity, you should define the following attributes:

	-   **name**: A unique name that describes the entity or account type. This is typically the name that will appear in Influent to describe the account type.
	-   **transformer**: A list of the transformers used to modify fields (if necessary). For more information on the available transformers, see the Transformers section of the Apache Solr DataImportHandler wiki page (<http://wiki.apache.org/solr/DataImportHandler#Transformer>).
	-   **query**: A SQL string used to retrieve information from the Entity database you want to import.

	Depending on your data source, you may need to specify different account types. For example, the Kiva application supports three different account types (lenders, borrowers and partners). Other implementations may only necessitate one type.

3. Each `<entity>` can have its own unique set of `<field>` elements on which you can search. For each field, you should define the following attributes:

	-   **Column**: The database column from which the field values should be imported.
	-   **Name**: If you want to rename or transform a field, use this value enter a new name.
	-   **Transformer-Specific Attributes**: If you are using a transformer to modify an existing field, make sure to call the appropriate transformer-specific attribute (e.g., Template or Clob). For more information on transformer attributes, see the Transformers section of the Apache Solr DataImportHandler wiki page (<http://wiki.apache.org/solr/DataImportHandler#Transformer>).

	Note that you if you use the `<entity>` element **query** attribute to select all entries from a table, you do not need to create `<field>` elements for every field you want to import. Instead, you only need to create `<field>` elements for fields that you want to rename or transform.

4. You can also define child `<entity>` elements to retrieve and join data from other tables not invoked in the parent `<entity>`.
5. When you have added all of the fields you want to import, save the **db-data-config.xml** file.

For example, the **db-data-config.xml** file for the Kiva application has a single document with three entities. Within Influent, an account is a document, while each of the different account types (lenders, borrowers and partners) are entities of that document. Users of the Kiva application can search for accounts of all types by using fields common to the three entities, or search for only a single account type by looking for fields specific to that type.

#####Adding a New Core

Once you have finished editing the **db-data-config.xml** file, you can add the new core in the Solr Admin console. To add the new core:

1.  Access the Solr Admin console and select **Core Admin** from the navigation menu.
2.  On the Core Admin page, click **Add Core**.
3.  On the New Core dialog:
    1.  Enter a unique **name** for the core. It is recommended you use the same name as the folder you created in your working directory.
    2.  In the **instanceDir** field, enter the name of the folder you created in your working directory.
    3.  Click **Add Core**.

If Solr is able to successfully add the new core, the Core Admin page is refreshed to display its properties. Otherwise, an error message will be displayed within the New Core dialog. Further details about any errors can be accessed by selecting **Logging** from the navigation menu. Common errors include:

-   Missing dependencies in the *conf* directory. Review the **schema.xml** file in this folder to make sure you have copies of all the .txt and .xml files that it references, along with any other dependencies.
-   Incorrect file paths for the `<lib>` directives (plugins). As described above, edit the **solrconfig.xml** file to specify the correct path (typically the *dist* folder of your root Solr directory).

If you encounter any errors, perform the appropriate corrective actions and delete the **core.properties** file in the **instanceDir** folder before attempting to add the core again.

#####Importing Your Data

Once you have configured the **db-data-config.xml** file and created the new core in the Solr Admin console, you can begin to import your data:

1. Select your new core in the Solr Admin console navigation menu, then click **Dataimport** in the submenu.
2. On the dataimport page:

	1.  Set the Command to full-import.
	2.  Select the **Clean**, **Commit** and **Optimize** options.
	3.  Use the **Entity** drop-down list to specify the entity you want to import.
	4.  Click **Execute**.

To verify that your data was imported successfully, click **Query** under the core submenu in the navigation menu and perform several test queries on your data.

#####Specifying the Searchable Fields in Influent

Influent's Advanced Search dialog can be configured to allow users to enter search criteria for any of the entity attributes you imported into Solr. You can the pass the fields to display in this dialog using a descriptor **fields.txt** file in your project's *src/main/resources* folder.

1.  For each account type, create a new section in the file with the first entry specifying the name of the account type in the following format:

    `type:type_name`

    These type names should correspond to the \<entity\> elements you created when you imported your data into Solr. In the Kiva application for example, there are three sections for each of the supported account types: *type:loan*, *type:lender* and *type:partner*.

2.  Under each account type, list all of the `<field>` elements that you imported into Solr for the corresponding `<entity>`.

```
data_type:property_name [suggested_values] {friendly_name}

data_type	(Required)
	Identifies the type of data contained in the field: integer, real, boolean,
	string or date.

property_name	(Required)
	Name of the field. Make sure you use the same values you passed to Solr,
	especially if you renamed or transformed any columns.

suggested_values	(Optional)
	List of values displayed to the user. Useful for categorical fields with a
	predefined set of values. Suggested values should be enclosed in square
	brackets and separated by commas.

friendly_name	(Optional)
	Alternate user-friendly name to display in Influent. Should be enclosed in
	braces.
```

Comment out any fields you do not want to appear in Influent by adding *\#* to the beginning of a line, then save the file.

#####Editing the Influent App Properties

After you have imported your data into Solr, you should configure the Influent app properties to point to the location of your Solr instance:

1.  Browse to the *src/main/resources* folder of your Influent and open the **app.properties** file.
2.  Edit the following values in the Midtier Server Properties section of the file:
    -   **influent.midtier.solr.url**: URL of the core you added to your Solr instance (e.g., http://solr:8983/solr/\#/influent)
    -   **influent.midtier.solr.descriptor**: Specify the name of the .txt file that contains a list of the entity details fields that can be searched on in the Influent application.

3.  Save the file.

##<a name="app-server"></a>App Server

Application-specific properties for the app server, including overrides for the default Aperture service configuration, can be edited using the **app.properties** file in *src/main/resources*.

-   [Authentication Module (Optional)](#authentication-module)
-   [Build Number](#build-number)
-   [User Interface Configuration](#user-interface-configuration)
-   [Guice](#guice)
-   [Image Capture Module](#image-capture-module)
-   [Cache](#cache)
-   [Data Access](#data-access)
-   [Data View Tables](#data-view-tables)
-   [Account ID Type](#account-id-type)
-   [Pattern Search](#pattern-search)

###<a name="authentication-module"></a>Authentication Module (Optional)

If you are using the Apache Shiro module to enable user authentication, use these properties to configure the login and hash algorithm settings.

```
shiro.hashAlgorithmName
	Specify the hash algorithm used.

shiro.redirectUrl
	Redirect link (path/filename) to the login page.
	Defaults to "/login.jsp".

shiro.loginUrl
	Direct link (path/filename) to the login page.
	Defaults to "/login.jsp"

shiro.successUrl
	Page (path/filename) to display upon successful login.
	Defaults to "/".
```

###<a name="build-number"></a>Build Number

The **app.buildnumber** property controls the version number applied to your build. By default, the date on which the build was created is appended to the build number text you specify.

###<a name="guice"></a>Guice

The **aperture.server.config.bindnames** property indicates whether you want to bind the Aperture server configurations to Google Guice. If this property is set to *false*, you will have to implement server-side code.

###<a name="user-interface-configuration"></a>User Interface Configuration

The **aperture.client.configfile** property passes your Influent project's **config.js** file to the Aperture client. This file contains several ApertureJS configuration settings.

###<a name="image-capture-module"></a>Image Capture Module

These properties control the PhantomJS module used to export screenshots of the Influent workspace.

```
aperture.imagecapture.cms.store
	Name. Defaults to "influent".

aperture.imagecapture.phantomjs.poolsize
	Number of PhantomJS instances available to process image captures.

aperture.imagecapture.phantomjs.exepath
	Direct link (path/filename) to the PhantomJS executable.
	Defaults to "bin/phantomjs".

aperture.imagecapture.phantomjs.ssl-certificates-path
	For use when user authentication is enabled. File path (path/filename) to SSL
	certificates required to use PhantomJS.
```

###<a name="cache"></a>Cache

```
aperture.cms.ehcache.maxSize
	Maximum size of the Ehcache. Defaults to 1000.

influent.midtier.ehcache.config
	Name of the file containing the Ehcache configuration settings.
	Defaults to "ehcache.xml"

influent.persistence.cache.name
	Name of the persistance Ehcache

influent.dynamic.clustering.cache.name
	Name of the dynamic clustering Ehcache

influent.charts.maxage
	Maximum size of the Ehcache for charts. Defaults to 86400.
```

###<a name="data-access"></a>Data Access

```
influent.midtier.server.name
	Name of the server (URI) in which your Transaction data is found.

influent.midtier.server.port
	Port used to connect to your Transaction database.

influent.midtier.database.name
	Name of the database in which your Transaction data is found.

influent.midtier.user.name
	Username with which to access your Transaction database.

influent.midtier.user.password
	Password for the username with which to access your Transaction database.

influent.midtier.solr.url
	URL of the Solr Server used to enable searches on your Entity data.

influent.midtier.solr.store.url
	URL 

influent.midtier.solr.descriptor
	Name of the file that lists the Entity attributes passed through Solr.
```

###<a name="data-view-tables"></a>Data View Tables

Use this section to map table or column names in your Transaction database to the names used by Influent.

###<a name="account-id-type"></a>Account ID Type

```
influent.data.view.idType
	Specify the format of the account IDs in your Entity database.

influent.data.view.dbo.idType
	If you have multiple entity types with different ID types, use this format
	to specify each format.
```

###<a name="pattern-search"></a>Pattern Search

```
influent.pattern.search.remoteURL
	Location (URL) of your service implementation for searching by pattern of
	activity using one or more model accounts.
```

##<a name="clustering"></a>Clustering

To organize and simplify the visual representation of search results and branched transaction flow, Influent often groups together similar entities into clusters. Clusters can contain individual accounts and/or other clusters to form a hierarchy.

In the Kiva application, for example, branching off a partner might reveal a single cluster of 20 lenders who gave money to the account (instead of populating the workspace with a card for each unique lender). This cluster would then be divided into subclusters, grouping accounts based on the geographic regions in which they are found. The subclusters could be further divided still, based on more granular locations or even by grouping accounts with similar names.

The following aspects of Influent's hierarchical clustering algorithm can be configured for each deployment:

-   **[Hierarchical Clustering Fields](#hierarchical_clustering_fields)**: Specify the fields on which to cluster accounts, their order and how they should be sorted.
-   **[Maximum Cluster Size](#maximum_cluster_size)**: Enter the maximum number of entities that can be grouped together in a cluster.
-   **[Stop Words](#stop_words)**: For clusters that are grouped together based on similar names, enter words that should be ignored (e.g., natural language search terms, individual letters, pronouns, etc.).
-   **[Distribution Summary Properties](#distribution_summary_properties)**: Specify the distribution summary icons that should be displayed on the clusters in the Influent workspace.

All of these values can be configured using the *clusterer.properties* file.

###<a name="hierarchical_clustering_fields"></a>Hierarchical Clustering Fields

The `entity.clusterer.clusterfields` value in the *clusterer.properties* file controls Influent's hierarchical clustering algorithm. It accepts FL\_PROPERTYTAG names and field names, each of which must be appended with a field type to indicate how the field should be sorted (i.e., geographic, categorical, label or numeric).

When multiple field names are specified, they must be separated by commas. The clusterer attempts to group entities based on the first field you specify. If none are found, it moves on to the second field and so on.

#### Format

    <FL_PROPERTYTAG | FIELDNAME>:<FIELD TYPE>

#### Properties

##### FL\_PROPERTYTAG

The following values are valid FL\_PROPERTYTAG names:

-   ID
-   TYPE
-   NAME
-   LABEL
-   STAT
-   TEXT
-   STATUS
-   GEO
-   DATE
-   AMOUNT
-   COUNT
-   USD
-   DURATION

##### FIELD TYPE

The following values are valid Field Types:

**geo**: Indicates that the field is a geographical location. Entities will be sorted first by continent, then region (e.g., Central Africa or Eastern Africa), country and finally by latitude/longitude (within a threshold distance), if available.

**categorical**: Indicates that the field is a category. Entities will be sorted by exact categorical value. This field type can only be applied to fields with string values.

**label**: Indicates that the field is a label. Entities will first be sorted alphabetically then using one of the following string clustering methods:

**fingerprint**: Uses fuzzy string matching to cluster entities based on their values. Fingerprint string clustering performs the following operations on the values in the specified field before comparing them to find matching strings:

-   Removes leading and trailing non-printing characters and special characters
-   Converts all characters to lower case
-   Changes accented characters to their ASCII representation
-   Sorts all unique, white space-separated parts of the name

For example, "Héctor" and "hector " are treated as the same name and sorted together, as are "John Doe" and "Doe John".

This is the default string clustering option.

**edit distance**: Uses normalized Levenshtein distance to compute the difference between strings. Distance is calculated as the number of deletions, insertions or substitutions required to transform one string into another. Distances are then normalized by the length of the longest string in the comparison. Any results within a threshold of 0.6 are clustered together.

For example, the strings "Michal" and "Michael" would be clustered together, as "Michal" requires one substitution and one insertion to become "Michael". Thus the normalized distance between the strings is 2 (the number of substitutions and insertions) divided by 7 (the length of the longest string, "Michael"), or ~0.29.

To use edit distance clustering in place of fingerprint string clustering, append :*edit* to the *label* field type.

**numeric**: Indicates that the field is a numeric value. Entities will be sorted by numeric value into bins, where appending a value in the format *:K* specifies the range of values to group in each bin. By default, the bin value is 100. Using the default, all values from 0 to 99 would be clustered together, as would 100 to 199 and so on.

#### Examples

In the Kiva application:

    entity.clusterer.clusterfields = TYPE:categorical,GEO:geo,LABEL:label

1.  `TYPE:categorical` - Kiva clusters entities first based on their `TYPE`, which is a `categorical` field. Thus, there will be three types of clusters at this level: all lenders, all partners or all borrowers.
2.  `GEO:geo` - Kiva then clusters entities based on their location (`GEO`), which is a `geo` field. Geographical clusters are also hierarchical. Influent first sorts by continent, then region, country and finally similar latitude/longitude (within a threshold distance), if available.
3.  `LABEL:label`: Lastly, Kiva clusters entities based on their name (`LABEL`), which is a `label` field. Kiva uses the default fingerprint string clustering method.

In the Bitcoin application:

```xml
entity.clusterer.clusterfields = AvgTransactionAmount:numeric:1000,
CountOfTransactions:numeric:10000,Degree:numeric:1000,LABEL:label
```

1.  `AvgTransactionAmount:numeric:1000` - Bitcoin clusters entities first based on their average transaction amount, which is a `numeric` field. Bins of 1,000 are created, meaning all entities with average transaction amounts ranging from 0 to 1,000 are clustered together, as are entities with amounts ranging from 1,001 to 2,000, etc.
2.  `CountOfTransactions:numeric:10000` - Bitcoin then clusters entities first based on the total number of transactions they have made, which is a `numeric` field. Bins of 10,000 are created, meaning all entities that have made 0 to 10,000 transactions are clustered together, as are entities with 10,001 to 20,000 transactions, etc.
3.  `Degree:numeric:1000` - Bitcoin then clusters entities first based on the total number of inbound and outbound connections they have, which is a `numeric` field. Bins of 1,000 are created, meaning all entities with connections ranging from 0 to 1,000 are clustered together, as are entities with connections ranging from 1,001 to 2,000, etc.
4.  `LABEL:label` - Lastly, Bitcoin clusters entities based on their name (`LABEL`), which is a `label` field. Bitcoin uses the default fingerprint string clustering method.

###<a name="maximum_cluster_size"></a>Maximum Cluster Size

The `entity.clusterer.maxclustersize` value in the *clusterer.properties* file controls the maximum number of entities that a cluster can contain. By default, this property is set to a value of 10 entities. Influent will subdivide a group of clustered entities until it and all of its subclusters have 10 entities or fewer.

###<a name="stop_words"></a>Stop Words

For clusters that are grouped together based on their `label` value, you can decide whether to ignore common words (e.g., natural language search terms, individual letters, pronouns, etc.) that might otherwise prevent matches from being made.

Use the `entity.clusterer.enablestopwords` property to turn stop words on (*true*) or off (*false*). Add or remove words from the `entity.clusterer.stopwords` property to customize your stop words list. Individual words should be separated by commas.

###<a name="distribution_summary_properties"></a>Distribution Summary Properties

In the Influent interface, each cluster is represented as a stack of cards to indicate that it is a group of several accounts and/or subclusters. On each cluster stack is a group of summary distribution icons that indicate the number and percentage of accounts in the cluster that share certain properties. The summary distribution icons also appear in the Details Pane for a selected cluster.

![Cluster Example](../../Resources/image/cluster-example-bl.png)

![Cluster Member Example](../../Resources/image/cluster-member-summary-bl.png)

You can edit which distribution summaries appear by setting the `entity.clusterer.clusterproperties` value in the *clusterer.properties* file. It accepts FL\_PROPERTYTAG names and field names, each of which must be appended with a descriptive name, which will appear above the summary icon in the Details Pane view.

When multiple field names are specified, they must be separated by commas.

#### Format

    <FL_PROPERTYTAG | FIELDNAME>:<DESCRIPTIVE NAME>

#### Properties

##### FL\_PROPERTYTAG

This property currently supports only SingleRange fields of type string and FL\_GeoData.

##### DESCRIPTIVE NAME

Enter a brief alphanumeric value to display in the Cluster Member Summary section of the Details Pane.

#### Examples

In the Kiva application:

```
entity.clusterer.clusterfields = TYPE:Kiva Account Type,GEO:Location,
STATUS:Status,WARNING:Warnings
```

For each cluster, Kiva displays the following distribution summary information:

-   `TYPE:Kiva Account Type` - The number and percentage of accounts that belong to each represented account type (lender, partner or borrower). In the Cluster Member Summary, this summary is labeled *Kiva Account Type*.
-   `GEO:Location` - The number and percentage of accounts in the cluster that are located in each represented country. In the Cluster Member Summary, this summary is labeled *Location*.
-   `STATUS:Status`: The number and percentage of accounts in the cluster that are closed or have defaulted. In the Cluster Member Summary, this summary is labeled *Status*.
-   `WARNING:Warning`: The number and percentage of accounts in the cluster that are have associated warnings (i.e., those with high delinquency or default rates). In the Cluster Member Summary, this summary is labeled *Warning*.

In the Bitcoin application:

```
entity.clusterer.clusterproperties = TYPE:type-dist,GEO:location-dist
```

For each cluster, Bitcoin displays the following distribution summary information:

-   `TYPE:type-dist` - The number and percentage of accounts that belong to each represented account type (bitcoin account). In the Cluster Member Summary, this summary is labeled *type-dist*.
-   `GEO:location-dist` - The number and percentage of accounts in the cluster that are located in each represented country. In the Cluster Member Summary, this summary is labeled *location-dist*.

##<a name="user-interface"></a>User Interface

The look and feel of your Influent application and its initial state can be configured using the **config.js** file in *src/main/resources*. While you can simply use the default values for most of the following user interface components, you should always edit the Header, Transaction Flow Period and Card properties, which are highly dependent on your Transaction and Entity data.

-   [Session Management](#session-management)
-   [Header](#header)
-   [Transaction Flow Period](#transaction-flow-period)
-   [Workspace](#workspace)
-   [Search](#search)
-   [Cards](#cards)

Note that the values in the *aperture.log*, *aperture.io*, *aperture.map* and *aperture.palette* sections of the **config.js** file are default settings for Aperture and should not be modified.

###<a name="session-management"></a>Session Management

The session management properties control authentication, session length and restoration.

```
useAuth
	Indicate whether to require users to log in to your Influent application
	before beginning any work. Influent has been designed to integrate with
	Apache Shiro to enable authentication. Defaults to "false".

sessionTimeoutInMinutes
	Number of minutes before an Influent session will time out. Defaults
	to 24*60.

sessionRestorationEnabled
	Indicate whether to enable restoration of the workspace across sessions.
	This feature is intended for debugging purposes; it is not recommended
	that you enable this feature for end users.
	Defaults to "false".
```

###<a name="header"></a>Header

The Influent header contains information on the data source to which the application is connected and links to the end user help.

```
banner
	Name of your Influent project as it appears in the header section of the
	application (above the Transaction Flow Period). 
	It is recommended you use the name of your data source for this value,
	especially if you are implementing more than one instance of the Influent
	application. In a secure environment, the banner can be used to display
	classification.

title
	Name of your Influent project as it appears in the title of the application
	(in tabs in the end user's Web browser and bookmarked links).
	It is recommended you use the name of your data source for this value,
	especially if you are implementing more than one instance of the Influent
	application.

help
	Location of the Influent end user help system. Defaults to
	"http://localhost:8080/help". 
	Help files are included in the source code in two formats:
		- As a set of Markdown files that can be viewed on GitHub
		- As a set of HTML files that can be viewed in any Web browser
	
	If your end users have access to the Internet, you can also choose to point
	to the help system on the Influent website, 
	https://influent.oculusinfo.com/help/.
```

###<a name="transaction-flow-period"></a>Transaction Flow Period

The Transaction Flow Period controls the period of time over which transactions are displayed in the user interface. You can configure the default period used at the start of every Influent investigation and rename or remove the available intervals.

```
dateRangeIntervals
	Edit or comment out the names of any of the default transaction flow period
	intervals:
	- P14D : '2 weeks',
	- P112D : '16 weeks',
	- P1Y : '1 year',
	- P16M : '16 months',
	- P4Y : '4 years',
	- P16Y : '16 years'
	
	Note that custom intervals are not supported, as this would require a change
	to the Influent Transaction database schema.	

startingDateRange

	Specify one of the default transaction flow period intervals (P14D, P112D,
	P1Y, P16M, P4Y or P16Y) to display at the start of every Influent session.
	Defaults to"P4Y".

defaultEndDate

	Specify the default end date (YYYY, M, D) for the transaction flow period
	shown at the start of every Influent session
```

###<a name="workspace"></a>Workspace

The workspace is Influent's window for investigating accounts and their transaction flow. The workspace properties allow you to configure the size of the default workspace and limit the number of accounts that can be loaded at one time to reduce load times.

```
workspaceWidth
	Default workspace width in pixels. Defaults to 1100.

objectDegreeWarningCount
	Number of branched accounts that trigger the following warning in the user
	interface. Defaults to 1000.

	This branch operation will retrieve <# greater than objectDegreeWarningCount>
	linked accounts. Are you sure you want to retrieve ALL of them?

objectDegreeLimitCount

	Number of branched accounts that trigger the following error in the user
	interface. Defaults to 10000.

	This branch operation would retrieve <# greater than objectDegreeLimitCount>
	accounts, which is more than the configured limit of
	<objectDegreeLimitCount>.
```

###<a name="search"></a>Search

The search properties let you control how Influent performs searches on entities and the format of the results

```
maxSearchResults
	Maximum number of search results to return. Defaults to 50.

searchResultsPerPage
	Results are returned in a paginated list; this controls the number of
	results to display on each page. Defaults to 12.

searchGroupBy

	Account attribute used to group similar accounts in search results. In the
	Kiva application for example, results are grouped by geographic
	location.

usePatternSearch
	Indicates whether pattern searching is enabled. Influent is designed to
	support behavioral query by example searches using services such as MIT
	Lincoln Labs Graph QuBE. Defaults to "true".

patternQueryDescriptionHTML
	Description (in HTML) that appears on the pattern search (Match on Account
	Activity) search dialog.

enableAdvancedSearchMatchType
	Indicates whether the all/any switches are available to users in the
	Advanced Search dialog. Defaults to "true".

defaultAdvancedSearchCriteria
	Lists the default fields (in JavaScript) that appear in the Advanced Search
	dialog for each account type. For example, the Kiva application uses the
	following configuration:

	'lender' : ['id','lenders_name','lenders_occupation'],
	'loan' : ['id','loans_name','loans_status'],
	'partner' : ['id','partners_name']

showAllAdvancedSearchCriteria

	Indicates whether to show all fields in the Advanced Search dialog by
	default. If false, users can still add fields from a drop-down list.
	Defaults to "false".

simpleViewWordCountLimit
	Limits the number of words that appear for each entry in the results when
	using the Simple Search view. Defaults to 1000.
```

###<a name="cards"></a>Cards

The card properties control how cards, which represent individuals in your Entity database, are displayed within the Influent workspace.

```
defaultShowDetails
	Indicate whether to display activity histograms on account cards. Defaults
	to "true".

doubleEncodeSourceUncertainty
	Cards can be configured to indicate two dimensions of uncertainty:
	-   How out of date the information is
	-   How uncertain the source is

	This property indicates whether both dimensions should be mapped from the
	same value. Defaults to "true".

iconMap
	JavaScript that controls the icons that are displayed on account cards in
	the Influent workspace. See the following section for examples.

iconOrder
	Specifies the order in which icons should be displayed, if more than one
	are used.
```

####Icon Mapping

In the Influent workspace, the cards that represent the accounts in your Transaction and Entity databases display a set of icons that indicate key account attributes. To edit which fields are displayed on the cards and the icons that represent them, edit the iconMap property.

The following example from the Kiva application illustrates how to use icons to indicate to which of the three types (lenders, loans or borrowers) an account belongs. At a minimum, each icon definition requires a title (the name that appears in a tooltip when you mouse over it) and the location of the image file to be displayed. You can also limit the number of icons of a particular type that appear on a card.

A set of default icons are available for your use in the ApertureJS project (*aperture\\aperture-icons\\src\\main\\resources\\oculus\\aperture\\icons\\hscb\\entity\\actor*). To use any of these icons, specify the parent folder (set the **title** to *'organization'* or *'person'*) and the filename (set the **attributes** to one of the filenames in the appropriate *attr/role* subfolder, e.g. *business*).

```javascript
iconMap : {
    TYPE : {
        map : function(name, value) {
            switch (value) {
                case 'lender':
                    return {
                        title: 'lender',
                        icon: {type: 'Person', attributes: {role: 'business'}}
                };
                case 'partner':
                    return {
                        title: 'partner',
                        icon: {type: 'Organization', attributes: {role: 'business'}}
                };
                case 'loan':
                    return {
                        title: 'borrower / loan',
                        icon: {type: 'Person'}
                };
            }
        },
        limit : 1
    },
```

To use your own custom icons, copy them to the *src/main/webapp/img* folder in your project, then use the **url** property to specify the location in which it can be found. The following example from the bitcoin application illustrates how to do this:

```javascript
iconMap : {
	TYPE : {
		map : function(name, value) {
			return {
				title: 'bitcoin account',
				url: 'img/bitcoin.png'
			};
		},
		distName : 'type-dist',
		limit : 1
	}
},
```