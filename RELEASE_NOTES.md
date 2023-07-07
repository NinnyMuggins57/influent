# Influent Release Notes
The following is a detailed summary of changes released with each build version.

## Release 1.3.*
Version 1.3 includes significant user experience refinements along with new import and export capabilities for saving and sharing work, logging improvements and additional flexibility for integration into different environments. Below is a highlight of its features, a detailed issue list by minor release number and outstanding issues.

### Feature Highlights
Highlights of the version 1.3.* releases include the following features. Minor release numbers are reserved for high priority hot fixes.

**UI and Usability Improvements**
+ Button visuals and feedback have been updated, along with added tooltips, improved responsiveness and adjusted colors across the application. Menus have been updated. In addition, cards are now automatically sorted by in/out flow when added to a column.

+ The pattern search user experience has been greatly simplified to make it more intuitive to invoke. Model patterns can now be imported as a workspace and used in a search in two clicks.
 
+ The advanced search user experience has been improved for more intuitive search expression and additional capabilities such as weighted terms. In Addition, it is now possible to search across multiple account types.

+ The details pane now expands and collapses in response to selections made in the workspace. Entity details have been changed to support multiple images per entity, with controls to browse and expand in the browser. Chart and table titles in the details pane have also been clarified.

+ Working with larger numbers of accounts is now more robust.
 
**Import and Export**
+ Import and export of filed workspace contents is now supported for saving and sharing work.

**Logging Improvements**
+ Error logging has been made more detailed and comprehensive, including detailed reporting of server errors.

+ User instrumentation has been added for optional analysis of usage.

**Integration Features**
+ Advanced search can be further configured with default search criteria and custom fuzzy likeness levels

+ New extension APIs allow custom defined actions for toolbar buttons, card selection and entity details. API Documentation to come.
 
+ Enhancements to the screen capture capability to support all conceivable authentication schemes.

+ Added support for database column naming variations and numeric entity IDs for additional flexibility.

+ Added support for HyperSQL databases.

+ Entity properties can now be reordered.

**Documentation**
+ An online User Guide has been added covering Quick Start, the UI and the various search techniques.    
 
### Release 1.3.6 - Resolved Issues
+ The workspace no longer requires a card to be focused.
+ Resolved some issues with deleting clusters.
+ Security fixes.
+ Searching fixes.
+ Fixed Transaction Table export.
 
### Release 1.3.5 - Resolved Issues
+ Resolved some visual issues when manipulating cards and files.
+ Resolved a case where some operations time out.

### Release 1.3.4 - Resolved Issues
+ Pattern search was not returning results in some cases.

### Release 1.3.3 - Resolved Issues
+ Multiple issues related to Import/Export.
+ Cross-browser fixes.
+ Numerous other bug fixes.

### Release 1.3.2 - Resolved Issues
+ Many issues related to large scale edge cases.
+ Resolved a thread leak issue in clustering.
+ Resolved an ehcache problem with synchronization of column changes.
+ Numerous other bug fixes.

### Release 1.3.1 - Resolved Issues
+ Linking and searching issues relating to filing cards have been resolved.
+ Fixed a number of issues with pattern searching.
+ Fixed Screen capture.
+ Numerous other bug fixes.
 
### Release 1.3.0 - Resolved Issues
+ Branching sometimes caused the current selection to change in an unpredictable way.
+ Export to CSV has been fixed.
+ Chart data now loads properly for non-account owner summaries.
+ Resolved issues with running in IE 9.
+ Numerous other bug fixes.

## Release 1.2.*
Version 1.2 includes a significant number of new features, as well as improvements to scalability and robustness. Below is a highlight of new features, a detailed issue list by minor release number, outstanding issues, and a summary of instructions for porting from 1.1.* versions.

### Feature Highlights
Highlights of the version 1.2.* releases include the following features. Minor release numbers are reserved for high priority hot fixes.

**Third Generation Clustering Architecture**
+ The previous architecture was designed to include a hierarchical pre-clustering option which we believed would give us improved performance.The pre-clustering process was designed to be highly distributed in order to scale to very large transactional graphs and was written in Spark. In practice however we were finding that the performance impact was not positive enough to justify the additional complexity. The need to tailor and run the preclustering process on each data set, on top of the need for runtime maintenance of cluster subselections in the database, was adding complexity to the integrator's task, to the database requirements, to the incorporation of new data, and to the code itself. Moreover the runtime performance was notably slower on small clustering operations, and not fast enough on large clustering operations to be practically useful.

  The new clustering architecture uses dynamic clustering exclusively, but with improvements from the first generation and incorporation of capabilities for pre-computing accounts summaries of large account owners and large branch clusters to address scalability edge cases. In addition, unlike previous generations a simple config file can be used to tailor it to a data set, and since the clusters are computed dynamically a configuration can be tested quickly before committing. This approach will also make it feasible to support end user control over clustering in the future as they use the tool, as may be appropriate to different analytic investigations.

**Search for Like Accounts**
+ Clicking the search button on a card toolbar now begins the process of launching a search for like accounts by populating the advanced search dialog with match criteria from the account(s) clicked. The match criteria may be further edited before executing the search.

**Search Support for Account Owners**
+ The search architecture now supports the ability to return *all* accounts belonging to the same individual or organization when a match is found against one of the accounts, and have them stacked in the results display. This makes it easier to disambiguate account owners when sorting through results, and makes it easy to search around an account discovered through branching to expand it to all accounts belonging to that owner.

**Pre-Computed Summaries of Large Account Owners and Branch Clusters**
+ When an organization owns thousands of accounts, summaries of those accounts can be pre-computed for display. Similarly when an accounts is linked to thousands of accounts, a summary of linked accounts may be pre-computed for rapid response. Pre-computed summaries display the same summary information as dynamic summaries, such as value distributions for key attributes and transaction charts, but may not be unstacked.

**Preview Annotations for Branch Operations**
+ The branch controls on accounts now include annotations that appear on hover. The annotations describe how many incoming and outgoing linked accounts exist and how many are currently displayed, informing the user of the basic effects of a branch operation without them needing to execute it.
 
**Authentication Support**
+ Support for configuration of authenticated user logins has been added through the integration of [Shiro](http://shiro.apache.org/). Shiro provides a wealth of secure authentication options through simple configuration files.

**Improved Release Structure**
+ Component projects and packaging have been updated to simplify the build process and make it possible to issue release updates that do not require recompilation of an Influent application. We are working towards a standard release as part of 1.3 that will require no integration code at all, only configuration files.
 
### Release 1.2.0 - Resolved Issues
+ Resolved an issue with the month table population in the data view scripts.
+ Resolved Solr issues with interpretation of certain searches.
+ Pattern search no longer appears in the workflow if not available.
+ Resolved concurrency issues.
+ The pinning feature as implemented had limited utility and was removed for greater clarity, in favor of the use of files. automatic subclustering of content for large files is in progress and will be released in version 1.3. 
+ Resolved issues with running in IE 9.
+ Numerous other bug fixes.

### Porting from Release 1.1.* to 1.2+
One of the major goals of the latest version was to greatly simplify application requirements through simplified project structures, elimination of complex clustering pre-processing, and simple configuration files. In order to support all of that, in addition to the introduction of some important new clustering capabilities to address scalability limits, some unusually significant changes were required in this version.

**Database Schema Changes**
+ To support account owner and cluster summaries some changes were made to the Influent "data view" table schemas. An upgrade script is provided in [influent-spi](influent-spi/src/main/dataviews), where the scripts to generate new tables have also been updated.

**Project Restructuring**
+ The projects have been reorganized for long term clarity and simplicity, with some projects having been removed. If you previously imported the projects in an IDE like Eclipse you will want to reimport them.

**Maven Changes for Applications**
+ The maven configuration requirements for Influent applications have been simplified significantly, and it is also now much easier to build Influent apps without maven. Build instructions have been added to the [README.md](README.md) file. The `bitcoin` application's [pom.xml](bitcoin/pom.xml) file can be used as an example.
