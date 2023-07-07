---
section: Community
subsection: Developer Docs
chapter: Reference
topic: clusterer.config
permalink: community/developer-docs/reference/clustering/
layout: submenu
---

clusterer.config
===================

The following sections describe advanced clustering properties and concepts in the [clusterer.config](https://github.com/unchartedsoftware/influent/blob/master/influent-app/src/main/resources/clusterer.config) file in your project's [src/main/resources/](https://github.com/unchartedsoftware/influent/tree/master/influent-app/src/main/resources) folder:

- [Stop Words](#stop-words)
- [String Clustering Methods](#string-clustering)

For a description of the basic properties that should be changed for every application, see the [Clustering Configuration](../../how-to/clustering-config/) topic.

## <a name="stop-words"></a> Stop Words ##

For clusters that are grouped together based on their *label* value, you can decide whether to ignore common words (e.g., natural language search terms, individual letters, pronouns, etc.) that might otherwise prevent matches from being made.

1. Edit the **entity.clusterer.enablestopwords** property to turn stop words on (*true*) or off (*false*).
2. Add or remove words from the **entity.clusterer.stopwords** property to customize your stop words list. Individual words should be separated by commas.

## <a name="string-clustering"></a> String Clustering Methods ##

If you configure the Influent clustering algorithm to [group entities based on their labels](../../how-to/clustering-config/#hierarchical-clustering-fields), Influent uses a string clustering method to group non-exact matching labels. This method adaptively applies two string clustering techniques: fingerprint and edit distance.

### <a name="fingerprint"></a> Fingerprint ###

Fingerprint clustering groups entities by cleaning, sorting and normalizing the string values in the specified field and then comparing the transformed values. This transformation includes the following operations:

- Removing special characters (e.g., leading/trailing non-printing characters and punctuation)
- Converting all characters to lowercase
- Changing accented characters to ASCII representations
- Sorting unique, whitespace-separated parts of the string

For example, the following strings are treated as the same name and grouped together:

- "Héctor" and "hector "
- "John Doe" and "Doe John"

### <a name="edit-distance"></a> Edit Distance ###

Edit distance clustering uses normalized Levenshtein distance to compute the difference between strings. Distance is calculated as the number of deletions, insertions or substitutions required to transform one string into another. 

Distances are then normalized by the length of the longest string in the comparison. Any results within a threshold of 0.6 are clustered together.

For example, the strings "Michal" and "Michael" would be clustered together, as "Michal" requires one substitution and one insertion to become "Michael". Thus the normalized distance between the strings is:

	 2 (# of substitutions and insertions)
	---------------------------------------  = ~0.29
	7 (length of longest string, "Michael")