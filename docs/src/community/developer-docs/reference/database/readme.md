---
section: Community
subsection: Developer Docs
chapter: Reference
topic: database-config.xml
permalink: community/developer-docs/reference/database/
layout: submenu
---

# database-config.xml #

The following sections describe the [database-config.xml](https://github.com/unchartedsoftware/influent/blob/master/influent-app/src/main/resources/database-config.xml) file in your project's [src/main/resources/](https://github.com/unchartedsoftware/influent/tree/master/influent-app/src/main/resources) folder:

- [FIN_LINK](#fin-link)
- [Custom Details](#custom-details)
- [Multi-Type Configurations](#multi-type)

## <a name="fin-link"></a> FIN_LINK ##

For most deployments, the only section of the **database-config.xml** file you need to edit is the *FIN_LINK* **&lt;dataTableSchema&gt;**, which specifies the name of your raw data source table. This will allow you to pass your unique transaction IDs to Influent.

```xml
<dataTableSchema key="FIN_LINK">
	<columns>
	</columns>
	<tables>
		<table key="FIN_LINK" memberKey="aml.transactions"/>
	</tables>
</dataTableSchema>
```

<h6 class="procedure">To specify the name of your raw data source table</h6>

1. Edit the *FIN_LINK* table **\<memberKey\>** to pass in the correct table name. 
2. See [Mapping Fields to Property Descriptors](../../how-to/connect-data/#map-fields-to-descriptors) for information on mapping your transaction ID column to the property descriptor (*ID*) required by Influent.

## <a name="fin-link"></a> Custom Details ##

The **database-config.xml** also allows you to:

- Adjust table and columns you altered to correspond with your database administration standards
- Pass in additional entity or transaction details specific to your deployment

**NOTE**: All of the capitalized **\<key\>** values for the columns and tables in the following schemas are required by Influent:

- FIN_ENTITY
- FIN\_ENTITY\_BUCKETS
- FIN_FLOW
- FIN\_FLOW\_BUCKETS
- DATA_SUMMARY
- FIN_LINK
- CLUSTER_SUMMARY
- CLUSTER\_SUMMARY\_MEMBERS

<h6 class="procedure">To adjust for any of the required columns you altered</h6>

- Edit the corresponding **\<memberKey\>** and/or **\<memberType\>** attributes, where:
	<div class="props">
		<table class="summaryTable" width="100%">
			<thead>
				<tr>
					<th scope="col" width="20%">Property</th>
					<th scope="col" width="80%">Description</th>
				</tr>
			</thead>
			<tbody>
				<tr>
					<td class="property">memberKey</td>
					<td class="description">Column named that should be mapped to the corresponding <strong>key</strong></td>
				</tr>
				<tr>
					<td class="property">memberType</td>
					<td class="description">
						Type of data stored in the corresponding column:
						<ul>
							<li>string</li>
							<li>float</li>
							<li>integer</li>
							<li>date</li>
							<li>double</li>
							<li>boolean</li>
							<li>hex (binary/raw referred to locally in hex without the 0x prefix)</li>
						</ul>
					</td>
				</tr>
			</tbody>
		</table>
	</div>

<h6 class="procedure">To adjust for any of the required tables you altered</h6>

- Edit the corresponding **\<memberKey\>** attribute, where:
	<div class="props">
		<table class="summaryTable" width="100%">
			<thead>
				<tr>
					<th scope="col" width="20%">Property</th>
					<th scope="col" width="80%">Description</th>
				</tr>
			</thead>
			<tbody>
				<tr>
					<td class="property">memberKey</td>
					<td class="description">Table named that should be mapped to the corresponding <strong>key</strong></td>
				</tr>
			</tbody>
		</table>
	</div>

<h6 class="procedure">To add new schemas, tables or columns</h6>

- Follow the steps in the preceding sections and give each new **dataTableSchema**, **column** and **table** a unique **key** value. This value serves as an identifier that can be used by Influent.

## Multi-Type Configurations ##

For deployments that have multiple account types (e.g., the Kiva application supports lender, borrower and partner entities), the *FIN_ENTITY* schema requires unique tables for each type.

```xml
<tables>
	<table key="FIN_ENTITY">
		<memberOf typeKey="loan" memberKey="dbo.FinEntityLoan"/>
		<memberOf typeKey="lender" memberKey="dbo.FinEntityLender"/>
		<memberOf typeKey="partner" memberKey="dbo.FinEntityPartner"/>
	</table>
</tables>
```

List the individual *FIN_ENTITY* tables as **\<memberOf\>** elements, where:

<div class="props">
	<table class="summaryTable" width="100%">
		<thead>
			<tr>
				<th scope="col" width="20%">Property</th>
				<th scope="col" width="80%">Description</th>
			</tr>
		</thead>
		<tbody>
			<tr>
				<td class="property">typeKey</td>
				<td class="description">Type of entities stored in the table</td>
			</tr>
			<tr>
				<td class="property">memberKey</td>
				<td class="description">Name of the table in which the entities are stored</td>
			</tr>
		</tbody>
	</table>
</div>