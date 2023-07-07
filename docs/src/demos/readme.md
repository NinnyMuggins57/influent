---
section: Demos
permalink: demos/
layout: section
---

Influent Demonstrations
=======================

The following demos illustrate how Influent, when applied to large datasets, enables analysts to explore patterns of financial or communication transactions between entities over time.

These demos represent experimental exercises only, undertaken for the purposes of developing and testing Influent against realistic transaction data. They may not be used for any other purpose. Where publicly available data has been incorporated, no warranties are provided as to the accuracy of the content.

## <a name="financial"></a> Financial Activity Application ##

This application is built off a dataset used to demonstrate a typical workflow where Influent is used to explore suspicious increase in financial activity.

\> Explore the [Financial Activity Application demo...](http://influent.org/influent/)

To follow along with the workflow example, see the [Getting Started](../docs/user-guide/getting-started/) topic or visit our Influent AML and Banking product site, <http://influent.io>, to watch a video demonstration of the example.

## <a name="kiva"></a> ![Kiva](../img/resources/kiva.png) ##

The Kiva demo explores a multi-year snapshot of semi-anonymized [Kiva](http://www.kiva.org) microloan data. Kiva is an award-winning non-profit organisation that facilitates crowd source funded loans for small entrepreneurs across the globe through its online platform, connecting *borrowers* with local *partners* (financial institutions) and *lenders*.

\> Explore the [Kiva demo...](http://influent.org/kiva/)

#### Use of Kiva Data ####

To promote lending and transparency with respect to any possible fraud concerns, Kiva participants agree to share all information publicly, *with the exception of the precise amounts loaned by each lender*. That public data is used here for demonstration purposes only, and with two important differences:

- Any self-identifying information that lenders publicly published on Kiva has been removed, including names.
- Loan amounts have been substituted in for each lender by evenly distributing the total amount loaned between all lenders for that loan.

## <a name="bitcoin"></a> Bitcoin ##

The Bitcoin demo explores a multi-year dataset of Bitcoin transactions made between accounts computed from low-level Bitcoin exchange activity. Bitcoin is a decentralized digital currency operating on a peer-to-peer system without a central repository or administrator.

\> Explore the [Bitcoin demo...](http://influent.org/bitcoin/)

While most entities in the dataset are anonymous and identified only by a unique user ID (e.g., 28497), some well-known organizations (e.g., WikiLeaks and Silk Road) have publicized keys that may serve as accessible test entities on which to search. The Communities view provides an interactive map of financial transfer activity among Bitcoin users, from the global level down to fine-grained detail.

The Bitcoin demo draws on Bitcoin transaction data compiled by [Barret Schloerke](https://github.com/schloerke/Bitcoin-Transaction-Network-Extraction) and account labels derived from [Blockchain](https://blockchain.info/).

## <a name="email"></a> Email ##

This demo explores emails from the government staff of politician Scott Walker during his 2010 campaign for Governor. The emails were unsealed and publicly published as part of a John Doe probe into possible illegal coordination between Walker's government staff and his political campaign. As a result of the probe, Milwaukee County office worker Kelly Rindfleisch was convicted of Misconduct in Public Office.

\> Explore the [Email demo...](http://influent.org/walker/)

The email transaction data has been cleaned and enhanced with computed categorizations of the contents and their senders. You can visualize email transactions between account owners and drill down into the contents of individual emails. Search functionality also enables you to search the full text of individual emails.

## <a name="amazon"></a> Amazon ##

This demo explores relationships between the products offered on amazon.com and the customers that buy and review them. Products are linked to other products through buyers based on Amazon's top five "Customers who bought this item also bought..." list for each product, and through customers who review the same products over time.

Links between products reviewed by the same customers and commonly purchased by the same customers reveal the kinds of patterns in purchasing affinity often used in marketing strategies. In this demo, the addition of a Communities view provides an interactive map that illustrates association and disassociation between products across the entire dataset, from the global level down to fine-grained detail.

\> Explore the [Amazon demo...](http://influent.org/amazon/)

The Amazon demo draws on public Amazon product data collected in 2003 by [Jure Leskovec](http://cs.stanford.edu/people/jure/) and published by the [Stanford Network Analysis Project (SNAP)](https://snap.stanford.edu/data/) for research. All real customer data has been removed, including the contents of each review. Customer names have been synthetically generated.

<div class="git">
	<h2>Interested in Learning More?</h2>

	<ul>
		<li><a href="../tour/">Tour</a>: Take our tour to learn more about Influent.
		<li><a href="../docs/user-guide/">Docs</a>: Learn how to use Influent to explore your large-scale transaction flow data.
		<li><a href="../community/developer-docs/">Community</a>: Learn how to download and use the Influent source code to install and implement your own custom deployment.
		<li><a href="../contact/">Contact</a>: Contact Uncharted for more information about Influent or to submit technical suggestions/contributions.
	</ul>
</div>