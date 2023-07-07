---
section: Development 
title: Installation
layout: default
permalink: dev/installation/index.html
previous: ../
next: ../configuration/
---

#Installation

##<a name="prerequisites"></a>Prerequisites

The Influent web app is a servlet with server-side Java and client-side JavaScript and HTML5. To build Influent, you must first install several third-party tools and other Oculus Info projects upon which Influent is dependent.

###<a name="third-party-tools"></a>Third-Party Tools

Before retrieving the Influent source code and its dependencies, you should install the following third-party tools:

- **Java Development Kit (JDK)** version 1.7+ (<http://www.oracle.com/technetwork/java/>)
- **Apache Maven™** version 3.1+ (<http://maven.apache.org/>)
- An integrated development environment (IDE) such as **Eclipse™** (<http://www.eclipse.org/>), while not strictly necessary, will be useful. The following plugins may also be useful (if not already installed with your Eclipse package):
	- Maven Integration for Eclipse (m2e) (Juno and newer) (<https://www.eclipse.org/m2e/>)
	- EGit (<http://www.eclipse.org/egit/>) (We use command line git or third party tools, but this is an easy alternative)

####Eclipse Configuration

If you are using Eclipse, ensure that it is configured to use a JDK instead of a JRE:

1. From the **Window** menu, select **Preferences \> Java \> Installed JREs**.
2. Click **Add** next to the list of Installed JREs.
3. Select the JRE Type *Standard VM*, then click **Next**.
4. Click the **Directory** button next to the **JRE home** field and browse to the location in which your JDK is installed (e.g., *C:\Program Files\Java\jdk1.7.0\_55*). Click **OK**.
5. Click **Finish** on the JRE Definition dialog.
6. Select the check box corresponding to your JDK in the Installed JREs list and click **OK**.

###<a name="oculus-info-dependencies"></a>Oculus Info Dependencies

The Influent source code is dependent on the following Oculus Info projects. The process of retrieving and building these projects is similar to the processes for the Influent project.

####ApertureJS

ApertureJS is an open, adaptable and extensible JavaScript visualization framework with supporting REST services. To install ApertureJS:

1.  Determine which version of ApertureJS is required for your copy of the Influent source code by searching for the **\<aperture.version\>** element in the master Influent **pom.xml** file found in the root of your Influent working directory.
2.  Retrieve the source code for the appropriate version of ApertureJS from GitHub (<https://github.com/oculusinfo/aperturejs>).
3.  In your root *aperturejs* directory, run the following command:

    `mvn clean install`

4.  Alternatively, right click on the **aperturejs** project in your IDE (e.g., Eclipse) and select **Run As \> Maven install** from the context menu.

We recommend that you watch the ApertureJS project on GitHub so you receive an email notification each time a new release becomes available.

####Oculus Ensemble Clustering

Oculus Ensemble Clustering is a flexible multi-threaded clustering library for rapidly constructing tailored clustering solutions that leverage the different semantic aspects of heterogeneous data. To install Oculus Ensemble Clustering:

1.  Determine which version of Oculus Ensemble Clustering is required for your copy of the Influent source code by searching for the `<artifactId>ensemble-clustering</artifactId>` element in the **pom.xml** file found in the *influent-server* folder in your Influent working directory.
2.  Retrieve the source code for the appropriate version of Oculus Ensemble Clustering from GitHub (<https://github.com/oculusinfo/ensemble-clustering>).
3.  In your root *ensemble-clustering* directory, run the following command:

    `mvn clean install`

4.  Alternatively, right click on the **ensemble-clustering** project in your IDE (e.g., Eclipse) and select **Run As \> Maven Install** from the context menu.

We recommend that you watch the Oculus Ensemble Clustering project on GitHub so you receive an email notification each time a new release becomes available.

##<a name="influent-source-code"></a>Influent Source Code

After you have ensured that all prerequisites have been installed, you should download and build the Influent source code available on GitHub <https://github.com/oculusinfo/influent>. We recommend that you watch the project on GitHub so you receive an email notification each time a new release becomes available.

Note that for the purposes of providing an example, we describe the process of retrieving the Influent source code using Eclipse™ (<http://www.eclipse.org/>). However, you can perform similar steps with your preferred integrated development environment, version control software or with command line tools.

###<a name="downloading-source"></a>Downloading the Source

####Initial Setup

If this is your first time retrieving the source code, you must perform the initial setup procedure to configure your version control software or integrated development environment (IDE) to access and download the code from GitHub™. Otherwise, you can simply pull the latest code for the Influent project from within your preconfigured application of choice.

To configure Eclipse to retrieve the source code from GitHub:

1.  Open Eclipse and select **Import** from the **File** menu,
2.  Set the import source to **Git \> Projects from Git** and click **Next**.
3.  For the repository source, choose **Clone URI** and click **Next**.
4.  For the Source Git Repository, enter the **URI** location of the Influent GitHub project, <https://github.com/oculusinfo/influent.git>. This will automatically populate the **Host** (*github.com*) and **Repository path** (*oculusinfo/influent.git*) fields.
5.  Enter your GitHub **User** ID and **Password** in the Authentication section and click **Next**.
6.  In the list of branches, select *master* and click **Next**. This branch contains the latest release of Influent.
7.  Choose the **Directory** in which to save the local copy of the repository and click **Next**.
8.  Set the **Wizard for project import** option to *Import existing projects* and click **Next**.

Select the following projects for import and click **Finish:**

-   bitcoin
-   influent
-   influent-client
-   influent-server
-   influent-spi
-   kiva

####Subsequent Updates

After you have performed the initial setup of your Influent project within Eclipse, you can simply pull new updates to the project from GitHub:

1.  Right click on the *influent [master]* repository and select **Pull** from the context menu.
2.  The Pull Result dialog contains a list of all the files that have changed since you last updated the repository.

Note that retrieving the updated Influent source code may also require you to update its project dependencies, ApertureJS and Oculus Ensemble Clustering. Refer to [Prerequisites](#prerequisites) for more information on downloading and installing the correct versions of these projects.

###<a name="building-source"></a>Building the Source

Once you have retrieved the source code and imported all of the projects (*influent*, *influent-client*, *influent-server* and *influent-spi*) into your environment, right click on the Influent project and select **Run As \> Maven Install** from the context menu. This will build the local projects and ensure all the dependencies are downloaded into your current Maven repository.

##<a name="example-applications"></a>Example Applications

Example Maven-based applications are provided for public Kiva and Bitcoin transaction data, which connect to databases Oculus has made available online for demonstration purposes. To import these applications into Eclipse:

1.  Select **Import** from the **File** menu.
2.  On the Import dialog, select **Maven \> Existing Maven Projects** and click **Next**.
3.  On the Import Maven Projects dialog, specify your **Root Directory** (typically *C:\workspace\influent\*), select the Kiva and/or Bitcoin entries in the list of Projects.

The example applications can be run from the Kiva or Bitcoin project folders by executing the following command:

`mvn package jetty:run`

Alternatively, add a new launcher in Eclipse:

1.  Right click the Kiva or Bitcoin project and select **Run As \> Maven build...** from the context menu.
2.  On the Edit Configuration dialog, enter *package jetty:run* in the **Goals** field.
3.  Click **Apply** to save the changes, then click **Run**.