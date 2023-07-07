---
section: Tour
subsection: System Description
permalink: tour/components/
layout: submenu
---

# System Description #

The Influent web app is a servlet, utilizing server-side Java and client-side HTML5 and JavaScript. Custom Influent applications can be created from raw datasets that describe the individual transactions and entities involved in the transaction flow data.

## Databases ##

Influent builds visual transaction flow analytics from conventional relational databases containing detailed **transaction data** (i.e., identities of participating entities, date and time and transaction value). Optional **entity data** can be added to further describe (e.g., names, images, locations) each of the entities involved in the transactions.

Influent can be configured to allow analysts to search for entities based on information in either set of data. Support for plugin enterprise search platforms such as [Apache Solr](http://lucene.apache.org/solr/) introduces the ability to execute "fuzzy" searches that can correct for misspellings and return results *like* the search terms (e.g., return *Steven* when searching for *Stephen*).

For ease of integration, Influent provides adapters for the following relational database formats:

- Microsoft SQL Server
- Oracle Database
- HyperSQL Database (HSQLDB)
- MySQL

## Influent Server ##

Influent server-side operations enable open exploration of millions of entities and hundreds of millions of transactions. Real-time, browser-based exploratory interaction is supported through the use of multi-scale pre-aggregations of transaction activity and a hierarchical clustering of entities.

Entity-to-entity transaction data is pre-aggregated to several semantically meaningful time scales. Per-entity summaries of all inbound/outbound transactions are computed using this same scheme to allow quick generation of transaction summary charts and entity-to-entity flow using user-defined time filters.

One challenge that the Influent server addresses is the need to compute clusters of similar entities to reflect subsets of entities relevant to the analyst’s current workspace. This requires computing cluster summaries as new information is requested. To overcome expensive database query operations, Influent computes entity clusters on demand using a hierarchical entity clustering algorithm that employs fast hashing methods and a large in-memory cache of entities and clusters. Using these techniques, clustering operations that would otherwise require minutes are reduced to seconds.

## Influent Client ##

The Influent client is a full-featured, interactive web application based on HTML5 and JavaScript. It enables analysts to explore financial or communications transaction flow data in a left-to-right, flow-oriented navigation method. The Influent workflow promotes:

- Advanced entity and transaction searches based on known attributes
- Exploratory data analysis
- Identification of suspicious behavior patterns

The Influent workspace also enables analysts to export their work to:

- XML files that can be loaded to share transaction flow graphs across sessions
- PNG images that can be shared with individuals that do not have access to the application
- CSV files that contain detailed lists of all accounts or transactions that match a specific set of search criteria

<div class="git">
	<h2>Interested in Learning More?</h2>

	<ul>
		<li><a href="../../docs/user-guide/">Docs</a>: Learn how to use Influent to explore your large-scale transaction flow data.
		<li><a href="../../community/developer-docs/">Community</a>: Learn how to download and use the Influent source code to install and implement your own custom deployment.
		<li><a href="../../demos/">Live Examples</a>: Explore live examples of the capabilities of Influent.
		<li><a href="../../contact/">Contact</a>: Contact Uncharted for more information about Influent or to submit technical suggestions/contributions.
	</ul>
</div>