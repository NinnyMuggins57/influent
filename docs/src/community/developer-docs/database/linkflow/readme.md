---
section: Community
subsection: Developer Docs
chapter: Database
topic: LinkFlow Tables
permalink: community/developer-docs/database/linkflow/
layout: submenu
---

# LinkFlow Tables #

The LinkFlow tables describe all of the transactions in your dataset, with each record representing one or more transactions between a specific source and destination entity. There are six different Transaction Flow tables, each of which has a different time-based aggregation scheme:

- [LinkFlow](#linkflow)
- [LinkFlowDaily](#linkflowdaily)
- [LinkFlowWeekly](#linkflowweekly)
- [LinkFlowMonthly](#linkflowmonthly)
- [LinkFlowQuarterly](#linkflowquarterly)
- [LinkFlowYearly](#linkflowyearly)

For a diagram illustrating how the LinkFlow tables relate to your raw data source and the other Influent tables, see the [Entity Relationships](#entity-relationships) section.

## <a name="linkflow"></a>LinkFlow ##

Each record in the LinkFlow table represents an aggregation of all transactions between a source and destination entity. Records are calculated from the information inserted into [LinkFlowDaily](#linkflowdaily).

<div class="props">
	<table class="summaryTable">
		<thead>
			<tr>
				<th scope="col">Column</th>
				<th scope="col">Data Type</th>
				<th scope="col">Nullable?</th>
				<th scope="col">Description</th>
			</tr>
		</thead>
		<tbody>
			<tr>
				<td class="property">FromEntityId</td>
				<td class="value">varchar(100)</td>
				<td class="description">No</td>
				<td class="description">Unique identifier of the entity that sent the transaction.</td>
			</tr>
			<tr>
				<td class="property">FromEntityType</td>
				<td class="value">varchar(1)</td>
				<td class="description">Yes</td>
				<td class="description">Type of entity that sent the transaction: 
					<ul>
						<li><em>O</em> = Owner summary</li>
						<li><em>A</em> = Account</li>
						<li><em>S</em> = Cluster summary entity</li>
					</ul>
				</td>
			</tr>
			<tr>
				<td class="property">ToEntityId</td>
				<td class="value">varchar(100)</td>
				<td class="description">No</td>
				<td class="description">Unique identifier of the entity that received the transaction.</td>
			</tr>
			<tr>
				<td class="property">ToEntityType</td>
				<td class="value">varchar(1)</td>
				<td class="description">Yes</td>
				<td class="description">Type of entity that received the transaction:
					<ul>
						<li><em>O</em> = owner summary</li>
						<li><em>A</em> = account</li>
						<li><em>S</em> = cluster summary entity</li>
					</ul>
				</td>
			</tr>
			<tr>
				<td class="property">FirstTransaction</td>
				<td class="value">datetime</td>
				<td class="description">Yes</td>
				<td class="description">Date and time (YYYY-MM-DD hh:mm:ss.fff) at which the corresponding entities first participated in a transaction.</td>
			</tr>
			<tr>
				<td class="property">LastTransaction</td>
				<td class="value">datetime</td>
				<td class="description">Yes</td>
				<td class="description">Date and time at which the corresponding entities last participated in a transaction.</td>
			</tr>
			<tr>
				<td class="property">Amount</td>
				<td class="value">float</td>
				<td class="description">Yes</td>
				<td class="description">Aggregate value of the transactions between the entities.</td>
			</tr>
		</tbody>
	</table>
</div>

## <a name="linkflowdaily"></a>LinkFlowDaily ##

Each record in the LinkFlowDaily table represents an aggregation of all transactions between a source and destination entity on a single day. Records are calculated directly from the information in your source database.

<div class="props">
	<table class="summaryTable">
		<thead>
			<tr>
				<th scope="col">Column</th>
				<th scope="col">Data Type</th>
				<th scope="col">Nullable?</th>
				<th scope="col">Description</th>
			</tr>
		</thead>
		<tbody>
			<tr>
				<td class="property">FromEntityId</td>
				<td class="value">varchar(100)</td>
				<td class="description">No</td>
				<td class="description">Unique identifier of the entity that sent the transaction.</td>
			</tr>
			<tr>
				<td class="property">FromEntityType</td>
				<td class="value">varchar(1)</td>
				<td class="description">Yes</td>
				<td class="description">Type of entity that sent the transaction: 
					<ul>
						<li><em>O</em> = Owner summary</li>
						<li><em>A</em> = Account</li>
						<li><em>S</em> = Cluster summary entity</li>
					</ul>
				</td>
			</tr>
			<tr>
				<td class="property">ToEntityId</td>
				<td class="value">varchar(100)</td>
				<td class="description">No</td>
				<td class="description">Unique identifier of the entity that received the transaction.</td>
			</tr>
			<tr>
				<td class="property">ToEntityType</td>
				<td class="value">varchar(1)</td>
				<td class="description">Yes</td>
				<td class="description">Type of entity that received the transaction:
					<ul>
						<li><em>O</em> = owner summary</li>
						<li><em>A</em> = account</li>
						<li><em>S</em> = cluster summary entity</li>
					</ul>
				</td>
			</tr>
			<tr>
				<td class="property">Amount</td>
				<td class="value">float</td>
				<td class="description">Yes</td>
				<td class="description">Aggregate value of the transactions between the entities on the corresponding day.</td>
			</tr>
			<tr>
				<td class="property">PeriodDate</td>
				<td class="value">datetime</td>
				<td class="description">Yes</td>
				<td class="description">Date (YYYY-MM-DD hh:mm:ss.fff) on which the transactions were executed.</td>
			</tr>
		</tbody>
	</table>
</div>

## <a name="linkflowweekly"></a>LinkFlowWeekly ##

Each record in the LinkFlowWeekly table represents an aggregation of all transactions between a source and destination entity in a single week (each of which starts on a Sunday). Records are calculated from the information inserted into [LinkFlowDaily](#linkflowdaily).

<div class="props">
	<table class="summaryTable">
		<thead>
			<tr>
				<th scope="col">Column</th>
				<th scope="col">Data Type</th>
				<th scope="col">Nullable?</th>
				<th scope="col">Description</th>
			</tr>
		</thead>
		<tbody>
			<tr>
				<td class="property">FromEntityId</td>
				<td class="value">varchar(100)</td>
				<td class="description">No</td>
				<td class="description">Unique identifier of the entity that sent the transaction.</td>
			</tr>
			<tr>
				<td class="property">FromEntityType</td>
				<td class="value">varchar(1)</td>
				<td class="description">Yes</td>
				<td class="description">Type of entity that sent the transaction: 
					<ul>
						<li><em>O</em> = Owner summary</li>
						<li><em>A</em> = Account</li>
						<li><em>S</em> = Cluster summary entity</li>
					</ul>
				</td>
			</tr>
			<tr>
				<td class="property">ToEntityId</td>
				<td class="value">varchar(100)</td>
				<td class="description">No</td>
				<td class="description">Unique identifier of the entity that received the transaction.</td>
			</tr>
			<tr>
				<td class="property">ToEntityType</td>
				<td class="value">varchar(1)</td>
				<td class="description">Yes</td>
				<td class="description">Type of entity that received the transaction:
					<ul>
						<li><em>O</em> = owner summary</li>
						<li><em>A</em> = account</li>
						<li><em>S</em> = cluster summary entity</li>
					</ul>
				</td>
			</tr>
			<tr>
				<td class="property">Amount</td>
				<td class="value">float</td>
				<td class="description">Yes</td>
				<td class="description">Aggregate value of the transactions between the entities during the corresponding week.</td>
			</tr>
			<tr>
				<td class="property">PeriodDate</td>
				<td class="value">datetime</td>
				<td class="description">Yes</td>
				<td class="description">First date (YYYY-MM-DD hh:mm:ss.fff) in the week over which the transactions were executed. Always corresponds to a Sunday.</td>
			</tr>
		</tbody>
	</table>
</div>

## <a name="linkflowmonthly"></a>LinkFlowMonthly ##

Each record in the LinkFlowMonthly table represents an aggregation of all transactions between a source and destination entity in a single calendar month. Records are calculated from the information inserted into [LinkFlowDaily](#linkflowdaily).

<div class="props">
	<table class="summaryTable">
		<thead>
			<tr>
				<th scope="col">Column</th>
				<th scope="col">Data Type</th>
				<th scope="col">Nullable?</th>
				<th scope="col">Description</th>
			</tr>
		</thead>
		<tbody>
			<tr>
				<td class="property">FromEntityId</td>
				<td class="value">varchar(100)</td>
				<td class="description">No</td>
				<td class="description">Unique identifier of the entity that sent the transaction.</td>
			</tr>
			<tr>
				<td class="property">FromEntityType</td>
				<td class="value">varchar(1)</td>
				<td class="description">Yes</td>
				<td class="description">Type of entity that sent the transaction: 
					<ul>
						<li><em>O</em> = Owner summary</li>
						<li><em>A</em> = Account</li>
						<li><em>S</em> = Cluster summary entity</li>
					</ul>
				</td>
			</tr>
			<tr>
				<td class="property">ToEntityId</td>
				<td class="value">varchar(100)</td>
				<td class="description">No</td>
				<td class="description">Unique identifier of the entity that received the transaction.</td>
			</tr>
			<tr>
				<td class="property">ToEntityType</td>
				<td class="value">varchar(1)</td>
				<td class="description">Yes</td>
				<td class="description">Type of entity that received the transaction:
					<ul>
						<li><em>O</em> = owner summary</li>
						<li><em>A</em> = account</li>
						<li><em>S</em> = cluster summary entity</li>
					</ul>
				</td>
			</tr>
			<tr>
				<td class="property">Amount</td>
				<td class="value">float</td>
				<td class="description">Yes</td>
				<td class="description">Aggregate value of the transactions between the entities during the corresponding month.</td>
			</tr>
			<tr>
				<td class="property">PeriodDate</td>
				<td class="value">datetime</td>
				<td class="description">Yes</td>
				<td class="description">First date (YYYY-MM-DD hh:mm:ss.fff) in the month over which the transactions were executed. Always corresponds to the first of the month.</td>
			</tr>
		</tbody>
	</table>
</div>

## <a name="linkflowquarterly"></a>LinkFlowQuarterly ##

Each record in the LinkFlowQuarterly table represents an aggregation of all transactions between a source and destination entity in a single quarter, where:

- Q1 = Jan 1 - Mar 31
- Q2 = Apr 1 - Jun 30
- Q3 = Jul 1 - Sep 30
- Q4 = Oct 1 - Dec 31

Records are calculated from the information inserted into [LinkFlowMonthly](#linkflowmonthly).

<div class="props">
	<table class="summaryTable">
		<thead>
			<tr>
				<th scope="col">Column</th>
				<th scope="col">Data Type</th>
				<th scope="col">Nullable?</th>
				<th scope="col">Description</th>
			</tr>
		</thead>
		<tbody>
			<tr>
				<td class="property">FromEntityId</td>
				<td class="value">varchar(100)</td>
				<td class="description">No</td>
				<td class="description">Unique identifier of the entity that sent the transaction.</td>
			</tr>
			<tr>
				<td class="property">FromEntityType</td>
				<td class="value">varchar(1)</td>
				<td class="description">Yes</td>
				<td class="description">Type of entity that sent the transaction: 
					<ul>
						<li><em>O</em> = Owner summary</li>
						<li><em>A</em> = Account</li>
						<li><em>S</em> = Cluster summary entity</li>
					</ul>
				</td>
			</tr>
			<tr>
				<td class="property">ToEntityId</td>
				<td class="value">varchar(100)</td>
				<td class="description">No</td>
				<td class="description">Unique identifier of the entity that received the transaction.</td>
			</tr>
			<tr>
				<td class="property">ToEntityType</td>
				<td class="value">varchar(1)</td>
				<td class="description">Yes</td>
				<td class="description">Type of entity that received the transaction:
					<ul>
						<li><em>O</em> = owner summary</li>
						<li><em>A</em> = account</li>
						<li><em>S</em> = cluster summary entity</li>
					</ul>
				</td>
			</tr>
			<tr>
				<td class="property">Amount</td>
				<td class="value">float</td>
				<td class="description">Yes</td>
				<td class="description">Aggregate value of the transactions between the entities during the corresponding quarter.</td>
			</tr>
			<tr>
				<td class="property">PeriodDate</td>
				<td class="value">datetime</td>
				<td class="description">Yes</td>
				<td class="description">First date (YYYY-MM-DD hh:mm:ss.fff) in the quarter over which the transactions were executed. Always corresponds to Jan 1, Apr 1, Jul 1 or Oct 1.</td>
			</tr>
		</tbody>
	</table>
</div>

## <a name="linkflowyearly"></a>LinkFlowYearly ##

Each record in the LinkFlowYearly table represents an aggregation of all transactions between a source and destination entity in a single calendar year. Records are calculated from the information inserted into [LinkFlowMonthly](#linkflowmonthly).

<div class="props">
	<table class="summaryTable">
		<thead>
			<tr>
				<th scope="col">Column</th>
				<th scope="col">Data Type</th>
				<th scope="col">Nullable?</th>
				<th scope="col">Description</th>
			</tr>
		</thead>
		<tbody>
			<tr>
				<td class="property">FromEntityId</td>
				<td class="value">varchar(100)</td>
				<td class="description">No</td>
				<td class="description">Unique identifier of the entity that sent the transaction.</td>
			</tr>
			<tr>
				<td class="property">FromEntityType</td>
				<td class="value">varchar(1)</td>
				<td class="description">Yes</td>
				<td class="description">Type of entity that sent the transaction: 
					<ul>
						<li><em>O</em> = Owner summary</li>
						<li><em>A</em> = Account</li>
						<li><em>S</em> = Cluster summary entity</li>
					</ul>
				</td>
			</tr>
			<tr>
				<td class="property">ToEntityId</td>
				<td class="value">varchar(100)</td>
				<td class="description">No</td>
				<td class="description">Unique identifier of the entity that received the transaction.</td>
			</tr>
			<tr>
				<td class="property">ToEntityType</td>
				<td class="value">varchar(1)</td>
				<td class="description">Yes</td>
				<td class="description">Type of entity that received the transaction:
					<ul>
						<li><em>O</em> = owner summary</li>
						<li><em>A</em> = account</li>
						<li><em>S</em> = cluster summary entity</li>
					</ul>
				</td>
			</tr>
			<tr>
				<td class="property">Amount</td>
				<td class="value">float</td>
				<td class="description">Yes</td>
				<td class="description">Aggregate value of the transactions between the entities during the corresponding year.</td>
			</tr>
			<tr>
				<td class="property">PeriodDate</td>
				<td class="value">datetime</td>
				<td class="description">Yes</td>
				<td class="description">First date (YYYY-MM-DD hh:mm:ss.fff) in the year over which the transactions were executed. Always corresponds to Jan 1.</td>
			</tr>
		</tbody>
	</table>
</div>

## <a name="entity-relationships"></a> Entity Relationships ##

The following entity relationship diagram illustrates the order in which the LinkFlow tables are built using the information in your source dataset. As each table is essentially a summary of your original data, each table is linked to every other table through the unique entity IDs in your dataset.

<img src="../../../../img/resources/db-linkflow-tables.png" class="screenshot" alt="LinkFlow Tables" />