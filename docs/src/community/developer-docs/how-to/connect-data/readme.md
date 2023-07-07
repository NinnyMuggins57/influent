---
section: Community
subsection: Developer Docs
chapter: How-To
topic: Connect Data to Influent
permalink: community/developer-docs/how-to/connect-data/
layout: submenu
---

# Connect Data to Influent #

Once you have transformed and indexed your transaction and entity data, you must specify how Influent should:

- Connect to your data sources
- Map the fields in your data to the transaction and entity properties required by Influent

## <a name="app-properties"></a> Specifying the Connection Details ##

The [server.config](https://github.com/unchartedsoftware/influent/blob/master/influent-app/src/main/resources/server.config) file in your Influent project's [src/main/resources/](https://github.com/unchartedsoftware/influent/tree/master/influent-app/src/main/resources) folder specifies how Influent connects to your database and Solr instance.

<h6 class="procedure">To edit the database and Solr connection details</h6>

1. Open the [server.config](https://github.com/unchartedsoftware/influent/blob/master/influent-app/src/main/resources/server.config) file.
2. Add or edit the following values in the *Database Server Properties* section of the file:
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

	<strong>NOTE</strong>: If you have a Microsoft SQL Server database, you can specify its location using the following legacy properties. However, the presence of the <strong>database.url</strong> and <strong>database.driver</strong> properties will automatically override the legacy properties.<ul>
		<li><strong>influent.midtier.server.name</strong> (URI)</li>
		<li><strong>influent.midtier.server.port</strong></li>
		<li><strong>influent.midtier.database.name</strong></li>
	</ul>
3. Edit the *Solr Server Properties* section to specify the URL of the core you added to your Solr instance: 
	- For single core instances containing both entity and transaction data:

		```
		influent.midtier.solr.url = http://solr.uncharted.software:8983/solr/aml
		```
	- For instances with separate cores for entity and transaction data:

		```
		influent.midtier.solr.entities.url = http://solr.uncharted.software:8983/solr/bitcoin-entities
		influent.midtier.solr.links.url = http://solr.uncharted.software:8983/solr/bitcoin-links
		```

4. Edit the **influent.pattern.search.remoteURL** value in the *Pattern Search Database* section to specify the URL of your Graph QuBE server (e.g., `http://solr.uncharted.sofware:8805/pattern/search/example`).
5. Save the [server.config](https://github.com/unchartedsoftware/influent/blob/master/influent-app/src/main/resources/server.config) file.

## <a name="raw-data"></a> Connecting to Your Raw Data Source ##

In addition to requiring access to the LinkFlow and Entity tables created by the DataViewTables script, Influent needs to connect to your raw data source to access your unique transaction IDs.

<h6 class="procedure">To connect to your raw data source</h6>

1. Open the [database-config.xml](https://github.com/unchartedsoftware/influent/blob/master/influent-app/src/main/resources/database-config.xml) file in the [src/main/resources/](https://github.com/unchartedsoftware/influent/tree/master/influent-app/src/main/resources) directory of your project.
2. Scroll to the FIN_LINK **&lt;dataTableSchema&gt;** and update the table **&lt;memberKey&gt;** value to point to the table in your raw data that contains your unique transaction IDs.
3. Save the file.

## <a name="data-types"></a> Understanding Data Types ##

Influent Service Provider Interfaces (SPIs) enable you to provide runtime-injected modules for search, data access, clustering and other services. The SPI protocols are defined in cross-language form with [Apache Avro](http://avro.apache.org/).

To map the fields in your data sources to the data types defined in the SPIs, you must edit the [property-config.xml](https://github.com/unchartedsoftware/influent/blob/master/influent-app/src/main/resources/property-config.xml) file in your project's [src/main/resources/](https://github.com/unchartedsoftware/influent/tree/master/influent-app/src/main/resources) folder.

The following sections describe the data types you should be aware of when editing this file. Complete documentation of each type can be found in the **DataEnums\_vX.X.avdl** file in [influent-spi/src/main/avro/](https://github.com/unchartedsoftware/influent/tree/master/influent-spi/src/main/avro).

- [FL\_RequiredPropertyKey](#fl_requiredpropertykey)
- [FL\_ReservedPropertyKey ](#fl_reservedpropertykey)
- [FL\_PropertyTag](#fl_propertytag)

### <a name="fl_requiredpropertykey"></a> FL\_RequiredPropertyKey ###

Each Influent app requires the presence of the following case-sensitive property keys. You must ensure that each one is mapped to an appropriate field or fields in your transaction or entity data.

<div class="props">
	<table class="summaryTable" width="100%">
		<thead>
			<tr>
				<th scope="col" width="18%">Type</th>
				<th scope="col" width="18%">Key</th>
				<th scope="col" width="64%">Description</th>
			</tr>
		</thead>
		<tbody>
			<tr>
				<td class="description" rowspan="2">Entities</td>
				<td class="property">NAME</td>
				<td class="description">Entity name. Generally user friendly, and does not need to be unique.</td>
			</tr>
			<tr>
				<td class="property">ID</td>
				<td class="description">Unique entity ID. Should correspond to an entity in the <a href="../../database/entity/#entitysummary">EntitySummary</a> table created when you executed the <a href="../database-config/#add-influent-tables-transaction-db">DataViewTables script</a>.</td>
			</tr>
			<tr>
				<td class="description" rowspan="5">Transactions</td>
				<td class="property">ID</td>
				<td class="description">Unique transaction ID. Should map to your raw data source.</td>
			</tr>
			<tr>
				<td class="property">FROM</td>
				<td class="description">Entity ID of the source account for a transaction.</td>
			</tr>
			<tr>
				<td class="property">TO</td>
				<td class="description">Entity ID of the destination account for a transaction.</td>
			</tr>
			<tr>
				<td class="property">DATE</td>
				<td class="description">Date or range on or during which a transaction occurred.</td>
			</tr>
			<tr>
				<td class="property">AMOUNT</td>
				<td class="description">Value of a transaction.</td>
			</tr>
		</tbody>
	</table>
</div>

### <a name="fl_reservedpropertykey"></a> FL\_ReservedPropertyKey ###

The following property keys are reserved for search keywords and should not be reused in your **property-config.xml** file:

- *TYPE*
- *ORDER*
- *MATCH*

### <a name="fl_propertytag"></a> FL\_PropertyTag ###

The Avro data model includes FL\_PropertyTag names defined in the application layer as a taxonomy of user and application concepts independent of the data source. These tags allow application semantics to be reused with new data with minimal design and development.

The following FL\_PropertyTags have applications that should be noted before you edit your property descriptors. For a complete list of FL_\PropertyTags, see the the **DataEnums\_vX.X.avdl** file in [influent-spi/src/main/avro/](https://github.com/unchartedsoftware/influent/tree/master/influent-spi/src/main/avro). 

<div class="props">
	<table class="summaryTable" width="100%">
		<thead>
			<tr>
				<th scope="col" width="18%">PropertyTag</th>
				<th scope="col" width="64%">Description</th>
			</tr>
		</thead>
		<tbody>
			<tr>
				<td class="property">SHARED_IDENTIFIER</td>
				<td class="description">Indicates that the property should be used to populate Influent's Advanced Account Search dialog when a user chooses to search for entities that are similar to a selected account.</td>
			</tr>
			<tr>
				<td class="property">FROM_LABEL</td>
				<td class="description" rowspan="2">Indicates that the property should be used as a label in the To/From columns of the Transaction search results.</td>
			</tr>
			<tr>
				<td class="property">TO_LABEL</td>
			</tr>
		</tbody>
	</table>
</div>

## <a name="map-fields-to-descriptors"></a> Mapping Fields to Property Descriptors ##

<h6 class="procedure">To configure the property descriptors for your app</h6>

1. Open the [property-config.xml](https://github.com/unchartedsoftware/influent/blob/master/influent-app/src/main/resources/property-config.xml) file in your project's [src/main/resources/](https://github.com/unchartedsoftware/influent/tree/master/influent-app/src/main/resources) folder. There are two sections in this file you must edit:
	- **\<entities\>**, which contains the configuration for entity data fields
	- **\<links\>**, which contains the configuration for transactions data fields
2. Create a **\<type\>** element for each entity and transaction type in your source data. In the Bitcoin example application, there is one entity type and one transaction type: 

	```xml
	<entities>
		<type key="account" friendlyText="Account"/>
		...
	</entities>
	...
	<links>
		<type key="financial" friendlyText="Financial"/>
		...
	</links>
	```

3. List your entity and transaction attributes as **\<property\>** elements.

	**NOTE**: The order in which properties are defined first is the order in which they are displayed in the user interface.

	```xml
	<property key="NAME" dataType="string" friendlyText="Name" 
			  levelOfDetail="key" searchableBy="freeText" 
			  memberKey="Label" multiValue="false">
		...
	</property>
	```

	Where:
	
	<div class="props">
		<table class="summaryTable" width="100%">
			<thead>
				<tr>
					<th scope="col" width="18%">Attribute</th>
					<th scope="col" width="10%">Required?</th>
					<th scope="col" width="72%">Description</th>
				</tr>
			</thead>
			<tbody>
				<tr>
					<td class="property">key</td>
					<td class="description">Yes</td>
					<td class="description">
						Unique field identifier entered as a custom value or an <a href="#fl_requiredpropertykey">FL_RequiredPropertyKey</a>. Users can enter the key value into the Accounts or Transactions search field followed by a term to search the corresponding field (e.g., <i>NAME:"daniel"</i>).
						<p><strong>NOTE</strong>: You cannot reuse any of the reserved property keys <a href="#fl_reservedpropertykey">FL_ReservedPropertyKey</a>.
					</td>
				</tr>
				<tr>
					<td class="property">dataType</td>
					<td class="description">Yes</td>
					<td class="description">Identifies the type of data contained in the field: <i>boolean</i>, <i>date</i>, <i>double</i>, <i>geo</i>, <i>image</i>, <i>integer</i>, <i>long</i>, <i>real</i>, <i>string</i>.</td>
				</tr>
				<tr>
					<td class="property">friendlyText</td>
					<td class="description">No</td>
					<td class="description">Alternate user-friendly field label to display in Influent.</td>
				</tr>
				<tr>
					<td class="property">levelOfDetail</td>
					<td class="description">No</td>
					<td class="description">
						Indicates where in the search results the field should be displayed:
						<ul>
							<li><i>key</i>: The field should be a column header in the search results, and should appear in the header of each individual result.</li>
							<li><i>summary</i>: The field should appear in the header of each individual result.</li>
							<li><i>full</i>: The field should appear only in the expanded view of each individual search result.</li>
							<li><i>hidden</i>: The field should appear not be returned to the client.</li>
						</ul>
						If this attribute is not included, the field may appear in the summary view of each individual result as well as the expanded view.</td>
				</tr>
				<tr>
					<td class="property">searchableBy</td>
					<td class="description">No</td>
					<td class="description">
						Indicates how the field can be searched:
						<ul>
							<li><i>descriptor</i>: Users can search the field by entering a value in the search bar and prefixing it with the <b>key</b> value followed by a colon. The field will also be available as a drop-down option on the Advanced Search dialog. This is the default value.</li>
							<li><i>freeText</i>: When users enter free text in the search bar (i.e., no search field is specified), the field should be searched by default.</li>
							<li><i>none</i>: The field should not be available for searching.
						</ul>
					</td>
				</tr>
				<tr>
					<td class="property">memberKey</td>
					<td class="description">No</td>
					<td class="description">If the field name in the data sources does not match the property key, <strong>memberKey</strong> defines that field name. However, if the field name varies by type or does not exist in every type, <strong>&lt;memberOf&gt;</strong> elements must be used instead.</td>
				</tr>
				<tr>
					<td class="property">sortable</td>
					<td class="description">No</td>
					<td class="description">
						Indicates whether users can sort results based on the values in the field. Defaults to <em>true</em>.
						<p><strong>NOTE</strong>: Sorting is not currently supported for multivalued fields.</a>.
					</td>
				</tr>
				<tr>
					<td class="property">multiValue</td>
					<td class="description">No</td>
					<td class="description">Indicates whether the field can support multiple values separated by commas. Defaults to <em>false</em>.</td>
				</tr>
			</tbody>
		</table>
	</div>
4. For each property with a field name that varies by type or does not exist in every type, add one or more **\<memberOf\>** elements to specify the applicable account or transaction fields.

	```xml
	<property key="NAME" dataType="string" friendlyText="Name" 
			  searchableBy="freeText" levelOfDetail="key">
				  <memberOf typeKey="lender" memberKey="LenderName"/>
				  <memberOf typeKey="loan" memberKey="LoanName"/>
				  <memberOf typeKey="partner" memberKey="PartnerName"/>
				  ...
	</property>
	```
	
	Where:
	
	<div class="props">
		<table class="summaryTable" width="100%">
			<thead>
				<tr>
					<th scope="col" width="18%">Attribute</th>
					<th scope="col" width="10%">Required?</th>
					<th scope="col" width="72%">Description</th>
				</tr>
			</thead>
			<tbody>
				<tr>
					<td class="property">typeKey</td>
					<td class="description">Yes</td>
					<td class="description">Account or transaction type for which the corresponding property is applicable. Must match one of the type keys you entered in step 2 above.</td>
				</tr>
				<tr>
					<td class="property">memberKey</td>
					<td class="description">Yes</td>
					<td class="description">Name of the account or transaction type-specific field that contains the values for the element. Make sure you exactly match the field name as it appears in your data source.</td>
				</tr>
			</tbody>
		</table>
	</div>
5. For each property that you want to map to an FL\_PropertyTag, add one or more **\<tag\>** elements.
	
	```xml
	<property key="AMOUNT" dataType="double" friendlyText="Amount (USD)"
			  levelOfDetail="key" searchableBy="freeText" memberKey="Amount">
				  <tags>
					  <tag>USD</tag>
				  </tags>
	</property>
	```
6. To define a non-primitive property like FL_GeoData (*dataType="geo"*), list the primitive fields it comprises under a **\<fields\>** element. Geo properties can be particularly useful for [dynamic clustering](../clustering-config/).

	```xml
	<property key="GEO" dataType="geo" friendlyText="Location" levelOfDetail="summary">
		<fields>
			<field name="text" key="LocationText" searchable="false"/>
			<field name="cc" key="CountryCode" searchable="true"/>
			<field name="lat" key="Latitude" searchable="false"/>
			<field name="lon" key="Longitude" searchable="false"/>
		</fields>
	</property>
	```

	Where:

	<div class="props">
		<table class="summaryTable" width="100%">
			<thead>
				<tr>
					<th scope="col" width="18%">Attribute</th>
					<th scope="col" width="10%">Required?</th>
					<th scope="col" width="72%">Description</th>
				</tr>
			</thead>
			<tbody>
				<tr>
					<td class="property">name</td>
					<td class="description">Yes</td>
					<td class="description">The type of geographic data contained in the field:
						<ul>
							<li><em>text</em>: Unstructured text field representing an address or other place reference</li>
							<li><em>lat</em>: Latitudinal coordinate</li>
							<li><em>lon</em>: Longitudinal coordinate</li>
							<li><em>cc</em>: Three-character ISO country code</li>
						</ul>
					</td>
				</tr>
				<tr>
					<td class="property">key</td>
					<td class="description">Yes</td>
					<td class="description">Reference to a unique field identifier defined elsewhere in the **property-config.xml** file.</td>
				</tr>
				<tr>
					<td class="property">searchable</td>
					<td class="description">No</td>
					<td class="description">Indicates whether the field is searchable when the parent meta-property is specified. Defaults to <em>false</em>.</td>
				</tr>
			</tbody>
		</table>
	</div>
7. Edit the **\<defaultOrderBy\>** element to specify the order in which search results should be displayed. If you specify multiple rules, they are applied according to the order in which they appear in the file. <p class="list-paragraph"><strong>NOTE</strong>: The **defaultOrderBy** values are only triggered if users do not explicitly specify a custom order in their search terms.</p>
	
	```xml
	<defaultOrderBy propertyKey="ENTITY" ascending="true"/>
	<defaultOrderBy propertyKey="DATE"/>
	```

	Where:

	<div class="props">
		<table class="summaryTable" width="100%">
			<thead>
				<tr>
					<th scope="col" width="18%">Attribute</th>
					<th scope="col" width="10%">Required?</th>
					<th scope="col" width="72%">Description</th>
				</tr>
			</thead>
			<tbody>
				<tr>
					<td class="property">propertyKey</td>
					<td class="description">Yes</td>
					<td class="description">The key of the field on which you want to order results.</td>
				</tr>
				<tr>
					<td class="property">ascending</td>
					<td class="description">No</td>
					<td class="description">Indicates whether to sort results in ascending order (<i>true</i>/<i>false</i>). If not included, results are sorted in descending order.</td>
				</tr>
			</tbody>
		</table>
	</div>
8. Edit the **\<searchHint\>** element to provide a helpful tip to display on an empty search page.
9. Save the file.

## <a name="one-to-many"></a> Handling One-to-Many Transactions ##

One-to-many transactions are common in email transaction records, where a single entity sends the same message to multiple recipients at the same time. Influent treats these transactions as sets of separate but related transactions.

For example, an email from `jsmith@influent.org` to `jdupont@influent.org` and `jperez@influent.org` is stored as two records:

- One from `jsmith@influent.org` to `jdupont@influent.org`
- One from `jsmith@influent.org` to `jperez@influent.org`

Both records have the same contents and are identified as belonging to the same transaction through a unique **EmailRecNo** attribute.

<h6 class="procedure">To configure Influent to group related transaction records</h6>

- Add a **groupByField** element in the **links** section of the **property-config.xml** file, where the *fieldName* attribute indicates the unique identifier shared by the related records.

	```xml
	<groupByField fieldName="EmailRecNo"/>
	```

## Next Steps ##

To configure the dynamic entity clustering methods used by Influent, see the [Clustering Configuration](../clustering-config) topic.