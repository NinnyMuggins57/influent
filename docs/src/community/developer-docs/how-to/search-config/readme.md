---
section: Community
subsection: Developer Docs
chapter: How-To
topic: Search Configuration
permalink: community/developer-docs/how-to/search-config/
layout: submenu
---

# Search Configuration #

Influent's plugin-style framework allows you to integrate optional third-party search platforms into your app to enable:

- Indexing, error correction and configurable matching for text searches on entity and transaction data
- Transactional pattern searches that, starting with an example account or accounts, find other accounts with similar activity histories

The example Influent apps use [Apache Solr](#solr) and [Graph Query-by-Example (QuBE)](#qube) to implement these capabilities, respectively. The following sections describe how to install and configure these platforms.

## <a name="solr"></a> Apache Solr ##

Using [Apache Solr](http://lucene.apache.org/solr/), you can configure text searches on entity and transactions to adjust for misspellings and return results similar to the user's search criteria. The following sections describe how to install Solr and use the application to index your data.

**NOTE**: The following instructions describe how to create a single Solr core that indexes your entity and transaction data. If there is any overlap between your unique entity IDs and transaction IDs (e.g., both are sequential identifiers beginning at 1), you must create separate cores for your entity and transaction data.

### <a name="installing-apache-solr"></a> Installing Solr ###

Create an instance of the Solr platform version 4.X.X (version 5.X is not currently supported by Influent). For more information on installing Solr, see the Tutorials and other documentation on the [Apache Solr website](http://lucene.apache.org/solr/).

If necessary, change the Solr home directory as described in the [Solr Install wiki](https://wiki.apache.org/solr/SolrInstall). This directory stores your Solr cores, which describe your Solr configuration and data schema. The default Solr home directory is the *example/solr/* folder in your installation directory.

### <a name="creating-new-core-directory"></a> Creating a New Core Directory ###

In Solr, the core directory stores config and schema files that detail the fields in your transaction and/or entity tables.

**NOTE**: The following steps must be performed before you create a new core within the Solr Admin console.

<h6 class="procedure">To create a new core directory</h6>

1. Browse to the *example/solr/* folder in your Solr installation directory.
2. Create a subfolder for the new Solr core that will index your transaction and/or entity data. Name this folder after your data source or simply call it *influent*. <p class="list-paragraph"><strong>NOTE</strong>: We will use the latter for the purposes of this example.</p>
3. Create three subfolders within the *influent/* folder:
	- Copy the following folders from *example/example-DIH/solr/db/conf/*:
		- *conf/*: Contains Solr Data Import Handler config files useful for importing entity data.
		- *lib/*: Contains a version-specific HSQLDB JDBC driver. If your database was created with a different version or is a different format, copy the appropriate JDBC driver into this folder.
	- *data/*: Empty folder.

### <a name="defining-solr-schema"></a> Defining the Solr Schema ###

The **schema.xml** file in the *conf/* folder specifies the schema of the Solr table into which your transaction and/or entity details will be imported. 

<h6 class="procedure">To edit the schema</h6>

1. Open the **schema.xml** file in the *conf/* folder you copied in the previous step.
2. Add each of the transaction (from your raw data) and/or entity (from *EntitySummary*) columns you want to index in the following format. <p class="list-paragraph">For more information about the **schema.xml** format, see the <a href="http://wiki.apache.org/solr/SchemaXml">Solr SchemaXml wiki page</a>.</p>

	```xml
	<field name="lenders_name" type="text_general" indexed="true"
	 stored="true" required="true" multiValued="true"/>
	```
	
	Where:
	
	<div class="props">
		<table class="summaryTable" width="100%">
			<thead>
				<tr>
					<th scope="col" width="15%">Name</th>
					<th scope="col" width="10%">Required?</th>
					<th scope="col" width="75%">Description</th>
				</tr>
			</thead>
			<tbody>
				<tr>
					<td class="property">name</td>
					<td class="description">Yes</td>
					<td class="description">Unique alphanumeric string representing the field name. Cannot start with a digit.</td>
				</tr>
				<tr>
					<td class="property">type</td>
					<td class="description">Yes</td>
					<td class="description">
						Indicates the field type (e.g., <em>string</em>, <em>double</em> or <em>date</em>). Use only values defined by the <strong>&lt;fieldType&gt;</strong> elements in the <strong>schema.xml</strong> file.

						<p>For numeric fields that should be searchable using range queries (e.g., find all values from 0 to 10), the field type should be specified as <em>sint</em>, <em>slong</em>, <em>sfloat</em> or <em>sdouble</em>.

						<p><strong>NOTE</strong>: Depending on your version of Solr, the available <strong>&lt;fieldType&gt;</strong> elements in your <strong>schema.xml</strong> file may vary. 

						<p>Review the different elements to determine which types pass their values through analyzers and filters to enable synonym matching, stopwords and stemming. The latest version of Solr uses <em>text_general</em> for this purpose.</p>
					</td>
				</tr>
				<tr>
					<td class="property">indexed</td>
					<td class="description">No</td>
					<td class="description">Indicates whether the field should be indexed so it can be searched or sorted (<em>true</em>/<em>false</em>).</td>
				</tr>
				<tr>
					<td class="property">stored</td>
					<td class="description">No</td>
					<td class="description">Indicates whether the field can be retrieved (<em>true</em>/<em>false</em>).</td>
				</tr>
				<tr>
					<td class="property">required</td>
					<td class="description">No</td>
					<td class="description">
						Indicates whether the field is required (<em>true</em>/<em>false</em>). Errors will be generated if the data you import does not contain values for any required fields.
						<p><strong>NOTE</strong>: Any fields marked as required will be expected for all transaction and entity types you import. Therefore, this attribute should only be used for fields that all types have in common.</p>
					</td>
				</tr>
				<tr>
					<td class="property">multiValued</td>
					<td class="description">No</td>
					<td class="description">
						Indicates whether the field can contain multiple values for a single entity (<em>true</em>/<em>false</em>).
						<p><strong>NOTE</strong>: Search result sorting is not currently supported for multivalued fields.</p>
					</td>
				</tr>
			</tbody>
		</table>
	</div>
3. Remove or comment out any **\<field\>** elements defined in the source file that you do not need.
4. Save the **schema.xml** file.

### <a name="specifying-search-fields"></a> Choosing Fields to Import into Solr ###

The **db-data-config.xml** file in the *conf/* folder defines how to select the transaction (from your raw data) and/or entity (from *EntitySummary*) fields that can be imported into Solr from your database.

For complete details on the data-config schema, see the [Solr DataImportHandler wiki page](http://wiki.apache.org/solr/DataImportHandler).

<h6 class="procedure">To specify the fields you want to be able to import into Solr</h6>

1. Edit the attributes of the **\<dataSource\>** element to specify the details of the data you want to import:
<div class="props">
	<table class="summaryTable" width="100%">
		<thead>
			<tr>
				<th scope="col" width="15%">Attribute</th>
				<th scope="col" width="85%">Description</th>
			</tr>
		</thead>
		<tbody>
			<tr>
				<td class="property">driver</td>
				<td class="description">Classpath of the JDBC driver for the database type (e.g., <em>org.hsqldb.jdbcDriver</em>)</td>
			</tr>
			<tr>
				<td class="property">url</td>
				<td class="description">Location of the database</td>
			</tr>
			<tr>
				<td class="property">user</td>
				<td class="description">Username required to connect to the database</td>
			</tr>
			<tr>
				<td class="property">password</td>
				<td class="description">Password required to connect to the database</td>
			</tr>
		</tbody>
	</table>
</div>
2. Add one or more **\<entity\>** elements to the **\<document\>** element. Entities generally correspond to the different transaction and account types in Influent, each of which can have its own unique set of details. For each type, you should define the following attributes:
<div class="props">
	<table class="summaryTable" width="100%">
		<thead>
			<tr>
				<th scope="col" width="15%">Attribute</th>
				<th scope="col" width="85%">Description</th>
			</tr>
		</thead>
		<tbody>
			<tr>
				<td class="property">name</td>
				<td class="description">Unique name that describes the transaction or entity. Typically the name that will appear in Influent to describe the transaction or entity type.</td>
			</tr>
			<tr>
				<td class="property">transformer</td>
				<td class="description">List of the transformers used to modify fields (if necessary). For more information on the available transformers, see the Transformers section of the <a href="http://wiki.apache.org/solr/DataImportHandler#Transformer">Apache Solr DataImportHandler wiki page</a>.</td>
			</tr>
			<tr>
				<td class="property">query</td>
				<td class="description">SQL string used to retrieve information from the database containing the entity attributes you want to import</td>
			</tr>
		</tbody>
	</table>
</div>

	Depending on your data source, you may need to specify different account types. For example, the Kiva application supports one transaction type (*financial*) three different entity types (<em>lenders</em>, <em>borrowers</em> and <em>partners</em>).
3. Use the **query** attribute at the **\<entity\>** level to select all the columns in your raw data and/or *EntitySummary* tables.
4. Alternatively, add a set of **\<field\>** elements to each **\<entity\>** element to select individual transaction and/or entity details. Fields represent the entity attributes on which users of your Influent project can search. Each field can have its own unique set of attributes. Define the following attributes for each field you add:
<div class="props">
	<table class="summaryTable" width="100%">
		<thead>
			<tr>
				<th scope="col" width="15%">Attribute</th>
				<th scope="col" width="85%">Description</th>
			</tr>
		</thead>
		<tbody>
			<tr>
				<td class="property">Column</td>
				<td class="description">Table column from which the field values should be imported</td>
			</tr>
			<tr>
				<td class="property">Name</td>
				<td class="description">If you want to rename or transform a field, use this value enter a new name</td>
			</tr>
			<tr>
				<td class="property">Transformer-Specific Attributes</td>
				<td class="description">
					If you are using a transformer to modify an existing field, make sure to call the appropriate transformer-specific attribute (e.g., <strong>Template</strong> or <strong>Clob</strong>). 
					<p>For more information on transformer attributes, see the Transformers section of the <a href="http://wiki.apache.org/solr/DataImportHandler#Transformer">Apache Solr DataImportHandler wiki page</a>.</p>
				</td>
			</tr>
		</tbody>
	</table>
</div>
	
	**NOTE**: You must create individual **\<field\>** elements for fields that you want to rename or transform.
5. You can also define child **\<entity\>** elements to retrieve and join data from other tables not invoked in the parent **\<entity\>**.
6. Save the **db-data-config.xml** file.

For example, the **db-data-config.xml** file for the Bitcoin application has:

- One document (for transactions and accounts)
- Two entities:
	- account, which represents Bitcoin accounts
	- financial, which represents transactions between Bitcoin accounts

### <a name="adding-new-core"></a> Adding a New Core ###

Once you have finished editing the **db-data-config.xml** file, you can add the new core in the Solr Admin console.

<h6 class="procedure">To add the new core</h6>

1. Access the Solr Admin console and select **Core Admin** from the navigation menu.
2. On the Core Admin page, click **Add Core**.
3. On the New Core dialog:
	- Enter a unique **name** for the core. We recommended you use the same name as the folder you created in your installation directory.
	- In the **instanceDir** field, enter the name of the folder you created in your installation directory.
4. Click **Add Core**.

<p class="procedure-text">If Solr is able to successfully add the new core, the Core Admin page is refreshed to display its properties. Otherwise, an error message is displayed within the New Core dialog.</p>

<h6 class="procedure">To address any errors</h6>

1. Select **Logging** from the navigation menu to access details about the errors. Common errors include:
	- Missing dependencies in the *conf/* directory. Review the **schema.xml** file in this folder to make sure you have copies of all the *.txt* and *.xml* files that it references, along with any other dependencies.
	- Incorrect file paths for the **\<lib\>** directives (plugins). As described above, edit the **solrconfig.xml** file to specify the correct path (typically the *dist/* folder of your root Solr directory).
2. Perform the appropriate corrective actions.
3. Delete the **core.properties** file in the *instanceDir/* folder before attempting to add the core again.

### <a name="importing-your-data"></a>Importing Your Data ###

Once you have configured the **db-data-config.xml** file and created the new core in the Solr Admin console, you can begin to import your data.

**NOTE**: Depending on the size of your data, you may need to allocate more memory to your Solr instance before attempting your import. For more information on memory considerations, see the [Solr Performance Factors](https://wiki.apache.org/solr/SolrPerformanceFactors#RAM_Usage_Considerations) wiki.

<h6 class="procedure">To import your data</h6>

1. Select your new core in the Solr Admin console navigation menu, then click **Dataimport** in the submenu.
2. On the dataimport page:
	- Set the **Command** to *full-import*.
	- Select the **Clean**, **Commit** and **Optimize** options.
	- Use the **Entity** drop-down list to specify the entity type you want to import.
3. Click **Execute**.

<h6 class="procedure">To verify that your data was imported successfully</h6>

- Click **Query** under the core submenu in the navigation menu and perform several test queries on your data.

### <a name="deploy-solr"></a> Deploying Solr ###

For information on deploying your new core and Solr in a servlet container, see the [Solr Tomcat wiki](https://wiki.apache.org/solr/SolrTomcat).

## <a name="qube"></a> Graph QuBE ##

Using the [Graph Query-by-Example (QuBE)](https://github.com/mitll/graph-qube) tool created by MIT Lincoln Labs (MIT-LL) in collaboration with Giant Oak, you can enable transactional pattern searching. The following sections describe how to install and configure Graph QuBE for Influent.

### Installation ###

The process of installing and running the Graph QuBE tool is described in detail in the [MIT-LL Graph QuBE User Manual](https://github.com/mitll/graph-qube/raw/master/doc/XDATA_UserManual.pdf). In general, this process requires you to:

1. Download and build the [Graph QuBE source code](https://github.com/mitll/graph-qube).
2. Index your raw transaction data with Graph QuBE, which creates an H2 database with derived features for each of the transactions in your source data.
3. Start the Graph QuBE server and connect it to the H2 database. The server exposes entity and pattern search capabilities through REST queries.

**NOTE**: New implementations of GraphQuBE require custom dataset bindings.

## Next Steps ##

To connect your databases and indexed search data to Influent, see the [Connecting Your Data to Influent](../connect-data/) topic.