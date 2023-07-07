---
section: Community
subsection: Developer Docs
chapter: How-To
topic: Database Configuration
permalink: community/developer-docs/how-to/database-config/
layout: submenu
---

# Database Configuration #

Influent builds transaction flow visualizations from your source relational databases. There are two types of data you can connect to Influent:

<div class="props">
	<table class="summaryTable" width="100%">
		<thead>
			<tr>
				<th scope="col" width="15%">Type</th>
				<th scope="col" width="40%">Contains</th>
				<th scope="col" width="45%">Transformation</th>
			</tr>
		</thead>
		<tbody>
			<tr>
				<td class="description">Transaction</td>
				<td class="description">Transaction details such as:
					<ul>
						<li>Unique transaction ID</li>
						<li>IDs of participating entities</li>
						<li>Date/time of the transaction</li>
						<li>Transaction value</li>
					</ul>
				</td>
				<td class="description">Using a <a href="#adding-transaction-tables">SQL script</a> in the source code, add a set of tables to your database that summarize the transactions and entities in your dataset.</td>
			</tr>
			<tr>
				<td class="description">Entity</td>
				<td class="description">Details about each entity in your transaction data such as:
					<ul>
						<li>IDs</li>
						<li>Images</li>
						<li>Names</li>
						<li>Locations</li>
					</ul>
				</td>
				<td class="description">
					<ol style="margin-top: 0px;padding-left: 20px">
						<li>Insert entity details into the <strong>EntitySummary</strong> table created by the SQL script.</li>
						<li>Index the <strong>EntitySummary</strong> table with a search platform (e.g., Apache Solr.)</li>
					</ol>
				</td>
			</tr>
		</tbody>
	</table>
</div>

The following sections describe the process of transforming your source transaction and entity data for use with Influent.

<div class="git">
	<ul>
		<li><a href="#supported-databases">Supported Database Formats</a></li>
		<li><a href="#transaction-data">Transaction Data</a>
			<ul>
				<li><a href="#required-fields">Required Fields</a></li>
				<li><a href="#add-influent-tables">Adding Influent Tables to Your Database</a>
					<ul>
						<li><a href="#resulting-tables">Resulting Tables</a></li>
					</ul>
				</li>
			</ul>
		</li>
		<li><a href="#entity-data">Entity Data</a>
			<ul>
				<li><a href="add-entity-details">Adding Entity Details Your Influent Tables</a></li>
			</ul>
		</li>
	</ul>
</div>

## <a name="supported-databases"></a> Supported Database Formats ##

For ease of integration with conventional relational databases, Influent provides adapters for:

- Microsoft SQL Server
- Oracle Database
- HyperSQL Database (HSQLDB)
- MySQL

## <a name="transaction-data"></a> Transaction Data ##

For Influent to read your transaction data, you must use the appropriate **DataViewTables** script (located in the [*influent-spi/src/main/dataviews*](https://github.com/unchartedsoftware/influent/tree/master/influent-spi/src/main/dataviews) directory) to add a set of tables to your source database.

The following sections describe the process of transforming your transaction data for use with Influent:

- [Required Fields](#required-fields)
- [Adding Influent Tables to Your Database](#add-influent-tables)
	- [Resulting Tables](#resulting-tables)
- [Connecting Your Database to Influent](#transaction-app-properties)
- [Enabling Searches for Entity IDs (*Optional*)](#transaction-db-search)

### <a name="required-fields"></a> Required Fields ###

Before you execute any commands in the SQL script, ensure that your database contains a table with the following information for every transaction:

<div class="props">
	<table class="summaryTable" width="100%">
		<thead>
			<tr>
				<th scope="col" width="25%">Field</th>
				<th scope="col" width="20%">Type</th>
				<th scope="col" width="55%">Contains</th>
			</tr>
		</thead>
		<tbody>
			<tr>
				<td class="property">Source ID</td>
				<td class="value">varchar</td>
				<td class="description">Unique IDs of entities who sent transactions</td>
			</tr>
			<tr>
				<td class="property">Destination ID</td>
				<td class="value">varchar</td>
				<td class="description">Unique IDs of entities who received transactions</td>
			</tr>
			<tr>
				<td class="property">Amount</td>
				<td class="value">float</td>
				<td class="description">Values of transactions (e.g., USD)</td>
			</tr>
			<tr>
				<td class="property">Date and Time</td>
				<td class="value">datetime</td>
				<td class="description">Date and time at which transactions occurred</td>
			</tr>
			<tr>
				<td class="property">Transaction ID</td>
				<td class="value">bigint or int</td>
				<td class="description">Unique IDs for each transaction</td>
			</tr>
		</tbody>
	</table>
</div>

**NOTE**: If your source and destination IDs are not strings, you can edit the SQL script to specify the correct data type. See the following section for more details.

### <a name="add-influent-tables"></a> Adding Influent Tables to Your Database ###

The **DataViewTables** scripts in the [*influent-spi/src/main/dataviews*](https://github.com/unchartedsoftware/influent/tree/master/influent-spi/src/main/dataviews) directory enable you to add Influent tables to your source database.

<h6 class="procedure">To run the DataViewTables script</h6>

1. Connect to the server on which your source database is located.
2. Open the appropriate **DataViewTables** script for your relational database management system:
<div class="props">
	<table class="summaryTable" width="100%">
		<thead>
			<tr>
				<th scope="col" width="30%">Database Type</th>
				<th scope="col" width="70%">File</th>
			</tr>
		</thead>
		<tbody>
			<tr>
				<td class="description">HyperSQL Database</td>
				<td class="description"><a href="https://github.com/unchartedsoftware/influent/blob/master/influent-spi/src/main/dataviews/DataViewTables_hsqldb.sql">DataViewTables_hsqldb.sql</a></td>
			</tr>
			<tr>
				<td class="description">Microsoft SQL Server</td>
				<td class="description"><a href="https://github.com/unchartedsoftware/influent/blob/master/influent-spi/src/main/dataviews/DataViewTables_mssql.sql">DataViewTables_mssql.sql</a></td>
			</tr>
			<tr>
				<td class="description">MySQL</td>
				<td class="description"><a href="https://github.com/unchartedsoftware/influent/blob/master/influent-spi/src/main/dataviews/DataViewTables_mysql.sql">DataViewTables_mysql.sql</a></td>
			</tr>
		</tbody>
	</table>
</div>
3. Find and replace all instances of the placeholder **YOUR\_RAW\_DATA** with the name of the table that contains your source transaction data.
4. Find and replace all instances of the following placeholder column names to reflect the schema of your source data, where:
<div class="props">
	<table class="summaryTable" width="100%">
		<thead>
			<tr>
				<th scope="col" width="20%">Placeholder</th>
				<th scope="col" width="20%">Type</th>
				<th scope="col" width="60%">Contains</th>
			</tr>
		</thead>
		<tbody>
			<tr>
				<td class="property">[source_id]</td>
				<td class="value">varchar</td>
				<td class="description">Unique source IDs</td>
			</tr>
			<tr>
				<td class="property">[dest_id]</td>
				<td class="value">varchar</td>
				<td class="description">Unique destination IDs</td>
			</tr>
			<tr>
				<td class="property">[amount]</td>
				<td class="value">float</td>
				<td class="description">Amount (value) of the transactions</td>
			</tr>
			<tr>
				<td class="property">[dt]</td>
				<td class="value">datetime</td>
				<td class="description">Date and time at which the transactions occurred</td>
			</tr>
		</tbody>
	</table>
</div>
5. If the entity IDs in your source data are not strings, find and replace all instances of the Influent entity ID data type *varchar(100)* with the appropriate type from your source data.
6. If your source data contains multiple entity types, you must create a unique EntitySummary table for each type. Modify the following statement and include the name of the entity types in the table (e.g., *EntitySummaryLender* and *EntitySummaryBorrower*).

	```sql
	create table EntitySummary(
	EntityId varchar(100) primary key, 
	IncomingLinks int not null, 
	UniqueIncomingLinks int not null,  
	OutgoingLinks int not null, 
	UniqueOutgoingLinks int not null, 
	NumLinks int, 
	MaxTransaction float, 
	AvgTransaction float, 
	StartDate datetime, 
	EndDate datetime
	-- additional type specific columns added here -- 
);
	```
7. If you created multiple EntitySummary tables, modify the `insert into EntitySummary` statement to your distinct entity types into the correct tables by uncommenting and editing following line:

	```sql
	-- where EntityId like 'myType.%' --
	```
8. Scroll to the *Summary Stats Table* section and edit the **SummaryValue** field for the **About** record insertion, which contains a description of your dataset that is presented to users on log in.
	
	```sql
	insert into DataSummary (SummaryOrder, SummaryKey, SummaryLabel, 
							 SummaryValue, UnformattedNumeric, 
							 UnformattedDatetime)
	values (
		1,
		'InfoSummary', 
		'About',
		'Some interesting description of your dataset can be written here.'
		null,
		null
	);
	```
9. Review the **SummaryValue** field for the remaining *Summary Stats* insertions. By default, this field omits formatting marks such as commas and currency signs that you may want to add manually.
10. Run the SQL script to create the tables. We recommend you execute the script one statement at a time to facilitate troubleshooting if you encounter errors.

#### <a name="resulting-tables"></a> Resulting Tables ####

After you execute the **DataViewTables** script, your database should contain the following additional table sets:

<div class="props">
	<table class="summaryTable" width="100%">
		<thead>
			<tr>
				<th scope="col" width="10%">Type</th>
				<th scope="col" width="65%">Contains</th>
				<th scope="col" width="25%">Tables</th>
			</tr>
		</thead>
		<tbody>
			<tr>
				<td class="description">Entity</td>
				<td class="description">Entities in your dataset. Stored in six tables, each with a different time-based aggregation scheme.
					<p>Used to build aggregate flow diagrams and time series charts on entities.</p></td>
				<td class="value">
					<ul style="margin-top: 0px;padding-left: 20px">
						<li>EntitySummary</li>
						<li>EntityDaily</li>
						<li>EntityWeekly</li>
						<li>EntityMonthly</li>
						<li>EntityQuarterly</li>
						<li>EntityYearly</li>
					</ul>
				</td>
			</tr>
			<tr>
				<td class="description">Flow</td>
				<td class="description">Transactions in your dataset, with each record representing one or more transactions between a specific source and destination entity. Stored in six tables, each with a different time-based aggregation scheme.
					<p>Used to build aggregate flow diagrams (by time) and highlighted sub-sections of time series charts on entities.</p></td>
				<td class="value">
					<ul style="margin-top: 0px;padding-left: 20px">
						<li>LinkFlow</li>
						<li>LinkFlowDaily</li>
						<li>LinkFlowWeekly</li>
						<li>LinkFlowMonthly</li>
						<li>LinkFlowQuarterly</li>
						<li>LinkFlowYearly</li>
					</ul>
				</td>
			</tr>
		</tbody>
	</table>
</div>

For more information on any of the new Influent tables, see the [Database Reference Guide](../../database/linkflow/).

## <a name="entity-data"></a> Entity Data ##

Influent allows users to investigate identifying attributes of individual entities involved in your transaction data. Entity searches can be enabled by indexing the *EntitySummary* table (created by the **DataViewTables** SQL script) with a plugin search platform such as [Apache Solr](http://lucene.apache.org/solr/).

By default, *EntitySummary* contains an **EntityId** column that can serve as a quick search field. Any additional entity details you want to make available in your app should be inserted into *EntitySummary* before indexing.

At a minimum, we recommend you insert the following columns into *EntitySummary* (if available):

- **Name**: A friendly name on which users can search and easily reference. This field can also serve as a field on which entities are clustered.
- **Location**: An address, country code or other data that can be geocoded. This field can serve as a field on which entities are clustered.

### <a name="add-entity-details"></a> Adding Entity Details to EntitySummary ###

<h6 class="procedure">To add entity details to EntitySummary</h6>

1. Add the appropriate columns to the *EntitySummary* table.
2. Insert the additional entity details into the new *EntitySummary* columns by joining on the **EntityId** column.

## Next Steps ##

To index your entity and transaction to enable enhanced search capabilities, see the [Search Configuration](../search-config) topic.