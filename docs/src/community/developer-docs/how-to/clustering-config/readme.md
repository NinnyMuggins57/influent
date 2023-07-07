---
section: Community
subsection: Developer Docs
chapter: How-To
topic: Clustering Configuration
permalink: community/developer-docs/how-to/clustering-config/
layout: submenu
---

# Clustering Configuration #

To organize and simplify the visual representation of branched transaction flow, Influent can dynamically group together similar entities into clusters. Clusters can contain individual accounts and/or other clusters to form a hierarchy.

The following sections describe aspects of Influent's hierarchical clustering algorithm that can be configured using the [clusterer.config](https://github.com/unchartedsoftware/influent/blob/master/influent-app/src/main/resources/clusterer.config) file in the [src/main/resources/](https://github.com/unchartedsoftware/influent/tree/master/influent-app/src/main/resources) folder of your project directory.

**NOTE**: For information on advanced clustering properties and concepts, see the [Clustering Settings](../../reference/clustering/) reference topic.

## <a name="hierarchical-clustering-fields"></a>Hierarchical Grouping ##

The **entity.clusterer.clusterfields** value controls how Influent dynamically groups entities into hierarchical clusters. You can configure Influent to group entities on any of the fields in your entity data.

If you specify multiple fields, Influent considers each one in order. If matching values are found, the entities are grouped into a cluster and examined to determine whether further grouping is required. Influent then moves on the next field in the hierarchy.

<h6 class="procedure">To specify the fields on which to cluster your entities</h6>

- Edit the **entity.clusterer.clusterfields** value to pass in [FL\_PropertyTag](../connect-data/#fl_propertytag) names and/or field names from your entity data in the following format. Each entry should be separated by a comma.

	```
	<FL_PropertyTag | FIELDNAME>:<FIELD TYPE>
	```

	Where valid FL\_PropertyTag names include:

	```
	ID, TYPE, NAME, LABEL, STAT, TEXT, STATUS, GEO, DATE, AMOUNT, COUNT, USD, 
	DURATION and TOPIC
	```

	And valid Field Types include:

	<div class="props">
		<table class="summaryTable" width="100%">
			<thead>
				<tr>
					<th scope="col" width="15%">Field Type</th>
					<th scope="col" width="85%">Group By</th>
				</tr>
			</thead>
			<tbody>
				<tr>
					<td class="property">geo</td>
					<td class="description">Hierarchical geographic location:
						<ol>
							<li>Continent</li>
							<li>Region (e.g., <em>South Asia</em> or <em>East Asia</em>)</li>
							<li>Country</li>
							<li>Latitude/longitude (within a threshold distance)</li>
						</ol>
					</td>
				</tr>
				<tr>
					<td class="property">categorical</td>
					<td class="description">Exact categorical value. Only apply to fields with string values.<br>
					</td>
				</tr>
				<tr>
					<td class="property">label</td>
					<td class="description">Label value:
						<ol>
							<li>Alphabetical</li>
							<li><a href="../../reference/clustering/#string-clustering">Fuzzy string clustering</a></li>
						</ol>
					</td>
				</tr>
				<tr>
					<td class="property">numeric:K</td>
					<td class="description">Numeric values in bins (e.g., <em>0-99</em>, <em>100-199</em>, etc.) where:
						<p><em>K</em> is an optional value (e.g., <em>50</em>) that specifies the range of values in each bin. Defaults to <em>100</em>.</p>
					</td>
				</tr>
				<tr>
					<td class="property">topic:K</td>
					<td class="description">Topic tags in distribution property bins where:
						<p><em>K</em> is an optional value between 0 (exactly the same) and 1 (completely different) that specifies the tolerance for bins. Defaults to <em>0.5</em>.</p>
					</td>
				</tr>
			</tbody>
		</table>
	</div>

### <a name="hier-examples"></a>Example ###

In the Kiva application:

```
entity.clusterer.clusterfields = TYPE:categorical,GEO:geo,LABEL:label
```

1. *TYPE:categorical* - Clusters entities based on a *categorical* grouping of the *TYPE* FL\_PropertyTag. In the Kiva application, this property tag is mapped to an account type field which supports three values:
	- *Lenders*
	- *Partners*
	- *Loans* (borrowers)
2. *GEO:geo* - Clusters entities based on a geographical (*geo*) grouping of the *GEO* FL\_PropertyTag. In the Kiva application, this property tag is mapped to a derived field which support hierarchical geographical data:
	<ol type="a">
		<li>Continent</li>
		<li>Region</li>
		<li>Country</li>
		<li>Similar latitude/longitude (within a threshold distance), if available</li>
	</ol>
3. *LABEL:label*: Clusters entities based on a *label* grouping of the *LABEL* FL\_PropertyTag. In the Kiva application, this property tag is mapped to an account name field.

## <a name="maximum-cluster-size"></a>Maximum Cluster Size ##

You can control the maximum number of entities that clusters can contain. Influent uses this setting to subdivide clusters until they and any of their subclusters contain no more than the configured value of entities.

By default, this property is set to 6 entities.

<h6 class="procedure">To specify the maximum number of entities a cluster can contain</h6>

- Set the **entity.clusterer.maxclustersize** property to the desired value.

## <a name="distribution-summary-properties"></a>Distribution Summary Properties ##

In the Influent interface, each cluster is represented as a stack of cards. Each stack features a group of summary icons that indicate the distribution of accounts in the cluster that share certain properties. The summary icons also appear in the Details Pane for a selected cluster.

<img src="../../../../img/screenshots/cluster-example.png" class="screenshot" alt="Cluster Stack Summary Distribution Icons" />

<h6 class="procedure">To specify the summary icons you want to appear on each cluster</h6>

- Edit the **entity.clusterer.clusterproperties** value to pass in [FL\_PropertyTag](../connect-data/#fl_propertytag) names and/or field names from your entity data in the following format. Each entry should be separated by a comma.

	```
	<FL_PropertyTag | FIELDNAME>:<DESCRIPTIVE NAME>:<true | false>
	```

	Where:

	<div class="props">
			<table class="summaryTable" width="100%">
				<thead>
					<tr>
						<th scope="col" width="29%">Component</th>
						<th scope="col" width="71%">Description</th>
					</tr>
				</thead>
				<tbody>
					<tr>
						<td class="property">&lt;FL_PropertyTag | FIELDNAME&gt;</td>
						<td class="description">This property currently supports only:
							<ul>
								<li>SingleRange fields of type:
									<ul>
										<li>string</li>
										<li>FL_GeoData</li>
									</ul>
								</li>
								<li>DistributionRange fields of type:
									<ul>
										<li>FL_TOPIC</li>
									</ul>
								</li>
							</ul>
						</td>
					</tr>
					<tr>
						<td class="property">&lt;DESCRIPTIVE NAME&gt;</td>
						<td class="description">Brief alphanumeric string to display in the Cluster Member Summary section of the Details Pane.</td>
					</tr>
					<tr>
						<td class="property">&lt;true | false&gt;</td>
						<td class="description">Optional component that indicates whether to normalize the distribution to sum to 1.0. Defaults to <em>false</em>.</td>
					</tr>
				</tbody>
			</table>
	</div>

### <a name="distrib-examples"></a>Example ###

In the Kiva application:

```
entity.clusterer.clusterfields = TYPE:Kiva Account Type,GEO:Location,
STATUS:Status,WARNING:Warnings
```

For each cluster, Kiva displays the following distribution summary information:

<div class="props">
		<table class="summaryTable" width="100%">
			<thead>
				<tr>
					<th scope="col" width="33%">Property</th>
					<th scope="col" width="42%">Summarizes</th>
					<th scope="col" width="25%">Label</th>
				</tr>
			</thead>
			<tbody>
				<tr>
					<td class="property">TYPE:Kiva Account Type</td>
					<td class="description">Accounts belonging to each represented account type:
						<ul>
							<li><em>Lender</em></li>
							<li><em>Partner</em></li>
							<li><em>Loan</em> (borrower)</li>
						</ul>
					</td>
					<td class="value">Kiva Account Type</td>
				</tr>
				<tr>
					<td class="property">GEO:Location</td>
					<td class="description">Accounts located in each represented country</td>
					<td class="value">Location</td>
				</tr>
				<tr>
					<td class="property">STATUS:Status</td>
					<td class="description">Accounts that are closed or have defaulted</td>
					<td class="value">Status</td>
				</tr>
				<tr>
					<td class="property">WARNING:Warnings</td>
					<td class="description">Accounts associated with warnings (i.e., those with high delinquency or default rates)</td>
					<td class="value">Warnings</td>
				</tr>
			</tbody>
		</table>
</div>

## Next Steps ##

For information on configuring the Influent server, see the [Server Configuration](../server-config/) topic.

For more information on advanced clustering settings, see the [Clustering Settings](../../reference/clustering/) reference topic.