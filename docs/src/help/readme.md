---
layout: default
section: Help
title: Introduction
permalink: help/index.html
next: getting-started.html
---

Introduction
============

Influent is a web-based application that enables analysts to visualize, follow and interact with transaction flows, revealing actors and behaviors of potential concern that might otherwise go unnoticed. Because Influent operates effectively at scale with big data sources, analysts can quickly focus on specific transactional patterns and actor characteristics from among millions of entities and hundreds of millions of transactions.

The Influent assessment and investigative workflow is powered by interactive link expansion, which enables analysts to view all of an account's incoming and outgoing transactions over a specified period. At the same time, the application uses configurable entity clustering to dynamically group similar accounts, speeding processing and highlighting accounts and patterns with similar properties and transactions.

Once a pattern or account of interest has been determined, Influent can be used to retrieve associated transaction records or search the data set for similar transactions. Influent can find transactional similar patterns based on a set of user-defined queries or using a specific example that has been loaded or saved in the workspace.

<a name="about-help"></a>About This Help
------------------------

This help system  is designed for analysts who want to understand how to navigate the Influent user interface and use the application to investigate and assess their own data sources. For information on building, installing and running the Influent source code, please refer to the project repository on [GitHub](https://github.com/oculusinfo/influent).

The following topics are covered in this help system:

1.  Introduction
2.  [Getting Started](getting-started.md)
3.  [Concepts](concepts.md)
4.  [Interface](interface.md)
5.  [Attribute Search](attribute-search.md)
6.  [Activity Search](activity-search.md)
7.  [Importing and Exporting Data](import-export.md)
8.  [Intellectual Property](intellectual-property.md)
9.  [Acknowledgments](acknowledgments.md)

### Help Examples

The look and feel of each Influent deployment is highly dependent on the source transaction data to which it is configured. While the topics in this help system refer to the interface for one specific data set (semi-anonymized Kiva microloans), many different configurations are available for the display of account details, summary icons and clustering properties. Such customization options are noted in the help where applicable.

Please note that an example application using the example Kiva data is available for your use in the Influent source code. A second example utilizing Bitcoin transactions is also available.

About Kiva
----------

The Kiva demo explores a multi-year snapshot of semi-anonymized Kiva microloan data. Kiva is a non-profit organization that facilitates crowd source funded loans for small entrepreneurs across the globe through its online platform, connecting *borrowers* with local *partners* (financial institutions) and *lenders*.

### Use of Kiva Data

To promote lending and in the interests of transparency with respect to any possible fraud concerns, Kiva participants agree to share all information publicly, with the exception of the precise amounts loaned by each lender. That public data is used here for demonstration purposes only, and with two important differences:

-   Any identifying information which lenders chose to publish about themselves publicly on Kiva has been removed, including names.
-   Loan amounts have been substituted in for each lender by evenly distributing the total amount loaned between all lenders for that loan.

Note that the Kiva demo represents an experimental exercise only, undertaken for the purposes of developing and testing Influent against realistic transaction data (and hopefully highlighting a great organization in the process!). It may not be used for any other purpose. No warranties are provided as to the accuracy of the data. The authors of Influent are in no way affiliated with the Kiva organization.

Open Source
-----------

Influent is under ongoing development and is freely available for download under [The MIT License](http://www.opensource.org/licenses/MIT) (MIT) open source licensing. Unlike GNU General Public License (GPL), MIT freely permits distribution of derivative work under proprietary license, without requiring the release of source code.

