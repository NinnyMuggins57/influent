---
section: Docs
subsection: User Guide
chapter: How-To
topic: Understanding Influent Concepts
permalink: docs/user-guide/how-to/concepts/
layout: submenu
---

Understanding Influent Concepts
===============================

This topic describes how the individual entities and transactions in your source data are represented and persisted across the different tabs in Influent. <div class="git">The concepts in this topic include:

	<ul>
		<li><a href="#entities">Entities</a>
		<ul>
			<li><a href="#accounts">Accounts</a>
			<li><a href="#clusters">Clusters</a>
			<ul>
				<li><a href="#cluster-grouping">How Accounts Are Grouped into Clusters</a>
			</ul>
		</ul>
		<li><a href="#transactions">Transactions</a>
	</ul>
</div>

For more information on the individual tabs that make up the Influent application, see the [Interface](../interface/) topic.

## <a name="entities"></a>Entities ##

Influent supports the following entity types to represent the actors from your transaction flow data:

- [Accounts](#accounts) represent individual entities.
- [Clusters](#clusters) are dynamically generated hierarchical groups of similar accounts that can be expanded or collapsed.

**NOTE**: Influent also supports two optional entity types, [Account Owners](../../advanced/entity-representations/#account-owners) and [Cluster Summaries](../../advanced/entity-representations/#cluster-summaries), which can be used to simplify the Influent workspace and search results and to optimize application performance. For more information on these types, see the [Advanced Entity Representations](../../advanced/entity-representations/) topic.

### <a name="accounts"></a>Accounts ###

Accounts represent individual actors in your transaction flow data. Influent can differentiate between accounts of various types. Each account is associated with the following sets of attributes:

- **Account details**, which can include identifying information such as a name, location, description or photograph
- **Transaction details**, which includes the identity of the other accounts the transactions were carried out with, the date on which they occurred and their value.

You can interact with accounts on the following tabs in Influent:

- [Accounts](../interface/#accounts), where you can search for accounts based on identifying attributes
- [Flow](../interface/#flow), where you visualize and investigate the transactions in which accounts are involved

### <a name="clusters"></a>Clusters ###

To organize and simplify the Flow workspace, Influent often dynamically groups similar [accounts](#accounts) into clusters. Clusters act like accounts in the workspace, but can be expanded and collapsed to show or hide their child entities. 

Collapsed clusters display aggregate summary information about the attributes and transactions of their child entities. In addition to accounts, clusters can also contain other clusters. The nesting of clusters is generated according to configurable hierarchical groups.

Dynamically generated clusters appear only on the [Flow](../interface/#flow) tab in Influent.

#### <a name="cluster-grouping"></a>How Accounts Are Grouped into Clusters ####

The Influent hierarchical clustering algorithm can be configured to cluster accounts based on any entity attribute. In the generic financial application, accounts are clustered together based on the following hierarchical, categorical groups:

1. **Customer Type**: *commercial*, *personal* or *cash*.
2. **Account Class**: *cash*, *checking*, *line of credit*, *savings* or *credit card*.
3. **Customer State**: U.S. state in which they are located.
4. **Customer City**: city in which they are located.

By default, each cluster has a maximum of 10 child entities (accounts and/or clusters). This value can be configured for each Influent deployment.

## <a name="transactions"></a>Transactions ##

A transaction is a timestamped record of a link or event between two entities. Each transaction in Influent contains information about the value of the link (e.g., the monetary amount), the directionality (who sent the transaction and who received it) and the date and time on which it occurred.

Transactions appear on the following tabs in Influent:

- [Transactions](../interface/#transactions), where you can search for transactions based on the participating entities, date or value
- [Flow](../interface/#flow), where you can visualize and investigate the transactions between entities

## Next Steps ##

For a detailed description of the Influent user interface components, see the [Navigating the Interface](../interface) topic.