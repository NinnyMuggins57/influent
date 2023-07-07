INFLUENT HIERARCHICAL ENTITY CLUSTERING TOOL NOTES

The Influent entity clustering tool hierarchically clusters entities based on a configurable set of input features.  
The tool is used to cluster entities and populate the Influent cluster data view table.

The clusterer uses a divisive hierarchical clustering approach. Each "stage" of the clustering splits the previous stages' clusters into sub-clusters using a 
different feature.  For example, stage 1 could first cluster entities by latitude/longitude, stage 2 splits the resulting stage 1 clusters further by label, etc.

The features to use and the order to cluster is configurable through command line arguments.  Current feature types supported are:

ID - a unique id string for an entity
CC - an ISO 2 or 3 character country code - if specified entities will be clustered by continent, region and finally country
GEO - a latitude/longitude pair
LABEL - an entity "name" or "description" - e.g. "Sally" or "Bill's loan to mend fence"
NUMBER - a numeric value associated with an entity
CATEGORY - a categorical string value associated with an entity.  E.g. LOAN_SECTOR_AGRICULTURE or LENDER

An entity can have any number of the feature types described above (with the exception of ID).  
Each is provided a unique alias (feature name) to distinguish them in the command line arguments described in more detail below.


INSTALLATION INSTRUCTIONS

The entity clustering tool is a set of Java command line tools that utilize AMP LAB Spark (version 7.0) which is bundled through a Maven dependency.  

Requirements:

* JVM 1.6 or higher
* Maven package manager
* Spark requires Scala 2.9.2, which can be downloaded and installed from http://www.scala-lang.org/
* Running on Windows requires cygwin to be installed which can be downloaded and installed from http://www.cygwin.com/ 


ENTITY CLUSTERING USAGE

There are four steps to using the entity clustering tool: generating entity CSV files, pre-processing the input, clustering the entities and finally populating the Influent cluster data view.


STEP 1:  Generate entity CSV files for clustering
------
Input to the entity clustering tool is a directory of CSV files.  Each line in the CSV files is an entity to cluster and the comma list is the entity features to use when clustering.  
Each line must have the same ordering of comma values.  If a feature (comma value) is missing for an entity, specify an empty string for that value.

E.g. If CSV schema is: "id field, type, label, lat/lon" and an entity is missing the lat/lon value then the csv line should be specified as:  1234,LOAN,"Jack",, 

NOTES
* values in CSVs that contain commas must be enclosed in " ".  E.g. 1234,LOAN,"john's, jane and harry's loan",40.42;98.73
* values in CSVs that contain double quotes must be escaped with two consecutive double quotes.  E.g. 1234,LOAN,"john's ""big"" loan",40.42;98.73
* GEO feature values MUST be encoded as:  lat;lon   e.g.  40.4230;98.7372


STEP 2: Pre-process CSV files into entity cluster input files
------
This step converts the entity CSV files into an internal file format the entity clusterer reads natively.  
This step pre-processes entity features, such as standardizing numeric values, tokenizing and removing stop words from entity label features.  
The output of this step are "instance" files that are passed as input to the entity clusterer (step 3).

To pre-process the CSV files generated in step 1, you must specify the CSV schema for the pre-processor to correctly convert the input files. 
This is specified using the "--schema" argument of the PreProcessClusterInput command line tool.  The schema is defined as a comma list of <FEATURENAME>:<FEATURE_TYPE> pairs
that describes the order, name and type of the comma separated values.  Each feature is required to have a unique name and an associated type.  

USAGE: mvn PreProcessClusterInput --schema="<comma list of featureName:<ID | GEO | LABEL | CC | NUMBER | CATEGORY>>" --inputdir="<INPUT DIR>" --ouputdir="<OUTPUT DIR>"

EXAMPLE:  mvn PreProcessClusterInput --schema="entityType:CATEGORY,location:GEO,name:LABEL,avgTrns:NUMBER" --inputdir="rawinput" --outputdir="instances"


STEP 3: Cluster entities
------
After CSV entity files are pre-processed into "instance" files the ClusterEntities command line tool is executed on the resulting instance files.

To cluster entities, you are required to specify the same schema you used in step 2 as well as a cluster order which determines the the order features will be used to hierarchically cluster the entities.
The "--clusterorder" argument is a comma list of feature names that must correspond to the feature names specified in the schema. 
The output of this process is a directory tree of text files that associate each entity with a cluster at every level in the cluster hierarchy.  The output is structured as:

OUTPUT DIRECTORIY
- CLUSTER ITERATION DIRECTORIES
-- PREVIOUS ITERATION CLUSTER SUB-DIRECTORIES  (a cluster directory exists only if it was a candidate to split into sub-clusters) 
--- Text files formatted as:  (SUB-CLUSTER-ID, Entity Features CSV LIST) 

USAGE: mvn ClusterEntities --schema="<comma list of featureName:<ID | GEO | LABEL | CC | NUMBER | CATEGORY>>" --clusterorder="<comma list of featureNames specified in schema>" --inputdir="<INPUT DIR>" --ouputdir="<OUTPUT DIR>"

The clusterorder parameter specifies the order that features will be used to cluster entities.  If clustering simultaneously on more than one feature is desired then parentheses can be places around groups of features to cluster by that group.

EXAMPLE: mvn ClusterEntities --schema="entityType:CATEGORY,location:GEO,name:LABEL,avgTrns:NUMBER" --clusterorder="entityType,(location,avgTrns,name)" --inputdir="instances" --outputdir="clusters"
The above example first clusters by country code and then simultaneously by the group: location, avgTrns and name

OPTIONAL arguments: 
--minclustersize="INTEGER"  determines the stopping criterion for further splitting a cluster
--maxstageiteration="INTEGER"  determines the maximum number of times a particular feature will be used to sub-cluster

EXAMPLE: mvn ClusterEntities --schema="entityType:CATEGORY,location:GEO,name:LABEL,avgTrns:NUMBER" --clusterorder="entityType,location,avgTrns,name" --inputdir="instances" --outputdir="clusters"

Note: depending on the size of the input data, computing environment, and number of features, this step may take several hours or more to complete.  
It is recommended to extract a small sample of the input first as a trial run to work out any format issues.   If the sample size is too small then you may only have one level of clusters.  
A suggested size of 10,000 sample entities is recommended for testing.


STEP 4:  Populate Influent Cluster Data Views
------

The final step is to populate the Influent cluster data view table with the entity clusters.  The PopulateClusterDataViews reads the output directory of step 3 and populates the data table specified.  
The specified dataview table must conform to the Influent cluster dataview table schema. 

PopulateClusterDataViews can be used to directly import cluster data into SQL data tables or export to a CSV file that can be imported manually.
Manual importing is much faster and the suggested method of populating the data tables.  

USAGE: mvn PopulateClusterDataViews --dbconstr=<DATABASE CONNECTION STRING> --dbuser=<DATABASE USERNAME> --dbpassword=<DATABASE PASSWORD> --dataviewtable=<CLUSTER DATAVIEW TABLENAME> --inputdir=<INPUT DIR> --exportToDB=<true|false> --exportToCSV=<true|false> --postProcessClusters=<true|false> --sanityCheck=<true|false>

EXAMPLE: mvn PopulateClusterDataViews --dbconstr="jdbc:jtds:sqlserver://mydatabaseserver:1433;databaseName=Kiva;selectMethod=cursor;" --dbuser="test" --dbpassword="test" --dataviewtable="global_cluster_dataview" --inputdir="clusters" --exportToDB="true" --postProcessClusters="true" --sanityCheck="true"

The suggested usage is to first generate CSV file, import into the db manually then re-run PopulateClusterDataViews to post process the clusters and perform sanity checks to ensure data integrity.

NOTES
-----
VM Arguments to help avoid heap overflow out of memory errors:

-Xss256m -Xms4096m -Xmx4096m     // specify the stack size, min heap size, max heap size

-Dspark.cache.class=spark.BoundedMemoryCache  // switch to using Spark memory management rather than default Java garbage collection