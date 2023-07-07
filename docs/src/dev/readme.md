---
section: Development
title: Overview
layout: default
permalink: dev/index.html
next: installation/
---

#Overview

This guide is designed for developers who want to build, install and run the Influent source code to create visual analytics relating to the transactions in their raw data.

Influent Service Provider Interfaces (SPIs) provide a plugin style framework for developers to provide runtime-injected modules for search, data access, clustering and other services. [Apache Avro™](http://avro.apache.org/) is used to define the SPI protocols in cross-language form. In process Java service implementations are injected via [Google Guice](https://code.google.com/p/google-guice/), which may optionally delegate to out of process providers using web services standards such as REST, for which Avro provides convenient serialization.

##About This Guide

The following topics are covered in this help system:

- Overview
- [Installation](installation/)
	- [Prerequisites](installation/#prerequisites)
	- [Influent Source Code](installation/#influent-source-code)
	- [Example Applications](installation/#example-applications)
- [Configuration](configuration/)
	- [Databases](configuration/#configuring-databases)
		- [Transaction Database](configuration/#transaction-database)
		- [Entity Database](configuration/#entity-database)
	- [App Server](configuration/#app-server)
	- [Clustering](configuration/#clustering)
	- [User Interface](configuration/#user-interface)
- [Deployment](deployment/)
	- [Building Your Project](deployment/#building-your-project)
	- [Web App Deployment](deployment/#web-app-deployment)