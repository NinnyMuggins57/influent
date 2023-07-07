---
section: Tour
subsection: Overview
permalink: tour/
layout: submenu
---

# Overview

Influent brings clarity to interactive financial forensics and communications analysis graphs by enabling flow-oriented navigation. With this technique, analysts can quickly search for accounts of interest and iteratively expand incoming and outgoing transactions in a left-to-right semantic flow layout. This greatly increases perceptual scalability of large-scale data, allowing analysts to "follow the money" in significantly less time and explore relevant sub-networks or entities.

## Perceptual Scalability

Perception of node / link semantics in standard graphs significantly degrades beyond a little over a
hundred nodes. ([Schneiderman, 2006,](http://www.cs.umd.edu/~ben/papers/Shneiderman2006Network.pdf) et al) Such large-scale graphs, which often resemble "hairballs," are limited by two critical constraints:

- Excessive link crossing and node occlusion make dense collections of graph entities difficult to explore and navigate, often obscuring who is connected to whom.
- The correlation of behavior over time cannot be adequately visualized.

Influent addresses these drawbacks by allowing for interactive branching, hierarchical clustering and pattern matching.

### Interactive Branching

On-demand, interactive expansion (branching) of the graph view enables Influent users to display incoming or outgoing transactions centered on a set of entity nodes. The left-to-right layout of the Influent workspace effectively illustrates the flow of transactions over time, with entities on the left sending transactions to entities on the right.

Even when the complexity of the Influent workspace is increased through interactive expansion, Influent ensures that users never lose sight of accounts of interest. Individual entity highlighting provides visual callouts of direct transfers to/from a specified entity.

### Hierarchical Clustering

To simplify the workspace and streamline processing time, Influent automatically clusters similar entities based on a set preconfigured hierarchical groupings. Hierarchically clustered graph entity nodes can be easily expanded (and collapsed) at any time to enable exploration of relevant sub-networks.

Aggregated transactional links are presented with clear markers that provide visual representations of the aggregation as an entity in the workspace.

### Pattern Matching

In addition to enabling users to manually explore custom datasets based on a set of known entity attributes, Influent provides plugin graph pattern matching tools that aid in locating transaction patterns that are similar to a user-defined example. Influent's default pattern matching tool is [Graph Query by Example (QuBE)](https://github.com/mitll/graph-qube) by [MIT Lincoln Laboratory](https://www.ll.mit.edu/) in collaboration with [Giant Oak](http://www.giantoak.com/), which performs a two-stage query that looks for accounts with similar:

1. Individual account activity features, which describe the average behavior of an account's activity with other accounts
2. Transactional features, which describe the transactions between accounts

## Open Source HTML5 Framework Technologies

Influent uses web-based technologies and the [Aperture JS](http://www.aperturejs.com/) framework for agile development and deployment. New applications utilizing a custom data source can be rapidly built using the Kiva or Bitcoin examples in the source code as a model. Influent and Aperture JS are open source projects with permissive licensing.

## Next

Review the [System Description](components/) for more information on the individual components that make up Influent.