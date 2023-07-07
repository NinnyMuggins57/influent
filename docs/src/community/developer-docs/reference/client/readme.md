---
section: Community
subsection: Developer Docs
chapter: Reference
topic: client-config.js
permalink: community/developer-docs/reference/client/
layout: submenu
---

# client-config.js #

The following sections describe the client-side modules and properties in the [client-config.js](https://github.com/unchartedsoftware/influent/blob/master/influent-app/src/main/resources/client-config.js) file in your project's [src/main/resources/](https://github.com/unchartedsoftware/influent/tree/master/influent-app/src/main/resources) folder:

- [Plugin Views](#plugin-views)
- [Session Management](#session-management)
- [Header](#header-config)
- [Transaction Flow](#transaction-flow)
- [Search](#search)
- [Cards](#cards)

For most of client-side properties, you can simply use the default values. For a description of the properties that should be changed for every application, see the [Client Configuration](../../how-to/client-config/) topic.

**NOTE**: Values in the *aperture.log* and *aperture.io* sections of the **client-config.js** file are default settings for Aperture and should not be modified.

## <a name="plugin-views"></a> Plugin Views ##

Influent allows you to create your own plugin views that each appear as an additional tab in the application. For example, you may want to create a Map view that receives entity properties and displays them on a geographic map.

To create and display a plugin view, you must:

- Create a custom plugin module
- Edit the plugins section of the **client-config.js** file to specify

### Plugin Module ###

An example plugin module, [stubView.js](https://github.com/unchartedsoftware/influent/blob/master/kiva/src/main/webapp/scripts/stubView.js), is available in the Kiva example application at [kiva/src/main/webapp/scripts/](https://github.com/unchartedsoftware/influent/blob/master/kiva/src/main/webapp/scripts/).

### client-config.js Settings ###

The *influent.plugins* section of your **client-config.js** file allows you to specify a require module that will be loaded on initialization and will be available to the plugin via `this.module`. Require will look for the module relative to the base URL for the app (typically in *src/main/webapp/scripts/*).

```js
influent.plugins: {
	views : [
		{
			key : 'StubView',
			label : 'Stub View',
			icon : 'glyphicon glyphicon-heart',
			tooltip : 'View Stub View',
			stateless : true,
			module: 'stubView',
			view : function (type, propertyMatchDescriptors, element) {
				return this.module.onView(type, propertyMatchDescriptors, element)
			},
			receives : {
				entities: true,
				links: true
			},
			sends : {
				entities: true
			}
		}
	],
	…
}
```

Where:

<div class="props">
		<table class="summaryTable" width="100%">
			<thead>
				<tr>
					<th scope="col" width="15%">Property</th>
					<th scope="col" width="85%">Description</th>
				</tr>
			</thead>
			<tbody>
				<tr>
					<td class="property">key</td>
					<td class="description">Unique internal identifier for the view.</td>
				</tr>
				<tr>
					<td class="property">label</td>
					<td class="description">Text that appears on the view tab.</td>
				</tr>
				<tr>
					<td class="property">tooltip</td>
					<td class="description">Tool tip text for the operations bar button.</td>
				</tr>
				<tr>
					<td class="property">icon</td>
					<td class="description">CSS class (or space-separated classes) of the icon that will appear on the operations bar button and view tab.

						<br><br>The core application views use <a href="http://fortawesome.github.io/Font-Awesome/">Font Awesome</a> icons.</td>
				</tr>
				<tr>
					<td class="property">stateless</td>
					<td class="description">Indicates whether the view is able to reinstate its current view (i.e., on browser refresh) if the same match descriptors are supplied again.

						<br><br>For example, Influent's native Accounts view is stateless, while its Flow view is capable of accumulating content and is therefore stateful.</td>
				</tr>
				<tr>
					<td class="property">module</td>
					<td class="description">Pathname of a require module to load on initialization.</td>
				</tr>
				<tr>
					<td class="property">view</td>
					<td class="description">Function called when the tab is instructed to show that which is defined by the property match descriptors. The view function takes three parameters:
						<ul>
							<li><em>type</em>: Property match descriptor type (entities/links)</li>
							<li><em>propertyMatchDescriptors</em>: Subject selection criteria. For more information on property match descriptors, see the <a href="../../how-to/connect-data/#map-fields-to-descriptors">Connect Your Data to Influent</a> topic.</li>
							<li><em>container</em>: <a href="../../api/container/">API object</a> that exposes app functionality to the view.</li>
						</ul>
					</td>
				</tr>
				<tr>
					<td class="property">receives</td>
					<td class="description">List of property match descriptor types (entities, links) accepted by the view for this plugin.</td>
				</tr>
				<tr>
					<td class="property">sends</td>
					<td class="description">List of property match descriptor types (entities, links) that this plugin sends to other views.</td>
				</tr>
			</tbody>
		</table>
</div>

### <a name="toolbar"></a> Toolbar ###

Each view determines whether to display a toolbar button for another view by comparing the property match descriptors the other view receives with the property match descriptors the view sends. If there is a match, the view will display a toolbar button for the other view.

## <a name="session-management"></a> Session Management ##

The session management properties control authentication, session length and restoration.

<div class="props">
	<table class="summaryTable" width="100%">
		<thead>
			<tr>
				<th scope="col" width="35%">Property</th>
				<th scope="col" width="65%">Description</th>
			</tr>
		</thead>
		<tbody>
			<tr>
				<td class="property">useAuth</td>
				<td class="description">Indicate whether to require users to log in to your Influent application before beginning any work. Influent has been designed to integrate with Apache Shiro to enable authentication.</td>
			</tr>
			<tr>
				<td class="property">sessionTimeoutInMinutes</td>
				<td class="description">Number of minutes before an Influent session will time out. Defaults to <i>1440</i> (1 day).</td>
			</tr>
			<tr>
				<td class="property">sessionRestorationEnabled</td>
				<td class="description">Indicate whether to enable restoration of the workspace across sessions. This feature is intended for debugging purposes; it is not recommended that you enable this feature for end users. Defaults to <i>false</i>.</td>
			</tr>
		</tbody>
	</table>
</div>

## <a name="header-config"></a>Header ##

The Influent header contains information about the data source to which the application is connected and links to the developer and end user help.

<div class="props">
		<table class="summaryTable" width="100%">
			<thead>
				<tr>
					<th scope="col" width="15%">Property</th>
					<th scope="col" width="85%">Description</th>
				</tr>
			</thead>
			<tbody>
				<tr>
					<td class="property">datasource<!--applicationBanner/--></td>
					<td class="description">
						Name of your Influent project as it appears in the header section of the application. 
						<p>It is recommended you use the name of your data source for this value, especially if you are implementing more than one instance of the Influent application. In a secure environment, the banner can be used to display classification.</p>
					</td>
				</tr>
				<tr>
					<td class="property">title<!--windowTitle/--></td>
					<td class="description">Name of your Influent project as it appears in the title of the application (in tabs in the end user's Web browser and bookmarked links). It is recommended you use the name of your data source for this value, especially if you are implementing more than one instance of the Influent application.</td>
				</tr>
				<tr>
					<td class="property">help<!--helpURL/--></td>
					<td class="description">
						Location of the Influent end user help system. Defaults to <i>http://localhost:8080/docs/user-guide</i>. 
						<p>Help files are included in the source code as a set of Markdown files that can be viewed on GitHub. Alternatively, you can use <a href="http://jekyllrb.com/">Jekyll</a> to build a static site from the Markdown content.</p>
						<p>If your end users have access to the Internet, you can also choose to point to the <a href="http://community.influent.org/docs/user-guide/">User Guide</a> page on the <a href="http://community.influent.org">Influent website</a>.</p>
					</td>
				</tr>
				<tr>
					<td class="property">about</td>
					<td class="description">
						Location of the About Influent page. Defaults to <i>http://localhost:8080/docs/about</i>.
						<p>If your end users have access to the Internet, you can also choose to point to the <a href="http://community.influent.org/docs/about/">About</a> page on the <a href="http://community.influent.org">Influent website</a>.</p>
					</td>
				</tr>
				<tr>
					<td class="property">aboutFlow</td>
					<td class="description">
						Location of the help content that describes how to add accounts to the Flow view. A URL to this content is provided in the application when the Flow view is empty. Defaults to <i>http://localhost:8080/docs/user-guide/investigate-flow/#add-accounts</i>. 
						<p>If your end users have access to the Internet, you can also choose to point to the <a href="http://community.influent.org/docs/user-guide/investigate-flow/#add-accounts">context-specific help</a> on the <a href="http://community.influent.org">Influent website</a>.</p>
					</td>
				</tr>
			</tbody>
		</table>
</div>

Additional header details can be edited in the [branding.css](https://github.com/unchartedsoftware/influent/blob/master/influent-app/src/main/webapp/theme/branding.css) file in [src/main/webapp/theme/](https://github.com/unchartedsoftware/influent/tree/master/influent-app/src/main/webapp/theme).

- To modify the banner that indicates the scope of the source dataset, edit the value of the *content* property for the **.banner-text:before** selector. 

## <a name="transaction-flow"></a>Transaction Flow ##

The workspace is Influent's window for investigating accounts and their transaction flow. The transaction flow properties allow you to configure the default display properties of accounts in the workspace and limit the number of accounts that can be loaded at one time to optimize performance.

<div class="props">
	<table class="summaryTable" width="100%">
		<thead>
			<tr>
				<th scope="col" width="15%">Property</th>
				<th scope="col" width="85%">Description</th>
			</tr>
		</thead>
		<tbody>
			<tr>
				<td class="property">dateRangeIntervals</td>
				<td class="description">Edit or comment out the names of any of the default transaction flow period intervals: 
					<ul>
						<li><i>P14D : '2 weeks',</i></li>
						<li><i>P112D : '16 weeks',</i></li>
						<li><i>P1Y : '1 year',</i></li>
						<li><i>P16M : '16 months',</i></li>
						<li><i>P4Y : '4 years',</i></li>
						<li><i>P16Y : '16 years'</i></li>
					</ul>
				<strong>NOTE</strong>: Custom intervals are not supported, as this would require a change to the Influent Transaction database schema.</td>
			</tr>
			<tr>
				<td class="property">startingDateRange</td>
				<td class="description">Specify one of the default transaction flow period intervals (<i>P14D</i>, <i>P112D</i>, <i>P1Y</i>, <i>P16M</i>, <i>P4Y</i> or <i>P16Y</i>) to display at the start of every Influent session.</td>
			</tr>
			<tr>
				<td class="property">defaultEndDate</td>
				<td class="description">Specify the default end date for the transaction flow period shown at the start of every Influent session. Defaults to the current date.</td>
			</tr>
			<tr>
				<td class="property">dateFormat</td>
				<td class="description">Specify the default date format (e.g., <i>MMM D, YYYY</i>) used throughout Influent.</td>
			</tr>
			<tr>
				<td class="property">timeFormat</td>
				<td class="description">Specify the default time format (e.g., <i>h:mma</i>) used throughout Influent.</td>
			</tr>
			<tr>
				<td class="property">defaultShowDetails<!--defaultCardView/--></td>
				<td class="description">Indicate whether to display activity charts on account cards. Defaults to <i>false</i>.</td>
			</tr>
			<tr>
				<td class="property">objectDegreeWarningCount<!--branchWarningThresholdCount/--></td>
				<td class="description">Number of branched accounts that trigger the following warning in the user interface. Defaults to <i>1000</i>.
				<p>This branch operation will retrieve &lt;# greater than objectDegreeWarningCount&gt; linked accounts. Are you sure you want to retrieve ALL of them?</p></td>
			</tr>
			<tr>
				<td class="property">objectDegreeLimitCount<!--branchLimitThresholdCount/--></td>
				<td class="description">Number of branched accounts that trigger the following error in the user interface. Defaults to <i>10000</i>.

				<p>This branch operation would retrieve &lt;# greater than objectDegreeLimitCount&gt; accounts, which is more than the configured limit of &lt;objectDegreeLimitCount&gt;.</p></td>
			</tr>
			<tr>
				<td class="property">defaultGraphScale</td>
				<td class="description">The default scale used on card Transaction History charts. Defaults to <i>$0</i>, meaning this is the largest value a bar in the charts can represent before being clipped. Once a user selects an account to highlight, the default is overridden by the highlighted account's transaction history.</td>
			</tr>
			<tr>
				<td class="property">promptForDetailsInFooter</td>
				<td class="description">Indicates whether to display a dialog requiring user interaction to access the details of restricted entities. Defaults to <i>false</i>.</td>
			</tr>
		</tbody>
	</table>
</div>

## <a name="search"></a>Search ##

The search properties let you control how Influent performs searches on entities and transactions and the format of the results.

<div class="props">
	<table class="summaryTable" width="100%">
		<thead>
			<tr>
				<th scope="col" width="15%">Type</th>
				<th scope="col" width="15%">Property</th>
				<th scope="col" width="70%">Description</th>
			</tr>
		</thead>
		<tbody>
			<tr>
				<td class="description" rowspan="2">results</td>
				<td class="property">maxSearchResults<!--resultsCountLimit/--></td>
				<td class="description">Maximum number of search results to return. Defaults to <i>50</i>.</td>
			</tr>
			<tr>
				<td class="property">searchResultsPerPage<!--resultsPerPage/--></td>
				<td class="description">Results are returned in a paginated list; this controls the number of results to display on each page. Defaults to <i>12</i>.</td>
			</tr>
			<tr>
				<td class="description" rowspan="3">advanced</td>
				<td class="property">enableAdvancedSearchMatchType</td>
				<td class="description">Indicates whether the all/any switches are available to users in the Advanced Search dialog. Defaults to <i>false</i>.</td>
			</tr>
			<tr>
				<td class="property">enableAdvancedSearchWeightedTerms</td>
				<td class="description">Indicates whether the ability to weight search criteria is available to users in the Advanced Search dialog. Defaults to <i>true</i>.</td>
			</tr>
			<tr>
				<td class="property">advancedSearchFuzzyLevels</td>
				<td class="description">Indicates the degree of similarity in search results returned when the <i>is very like</i>, <i>is like</i> or <i>is vaguely like</i> operators are used. Should range between <i>0</i> and <i>1</i>, where higher numbers indicate more similarity to the supplied search criteria.</td>
			</tr>
			<tr>
				<td class="description" rowspan="3">pattern</td>
				<td class="property">usePatternSearchInFlowView</td>
				<td class="description">Indicates whether pattern searching is enabled in the Flow view. Influent is designed to support behavioral query by example searches using services such as [Graph QuBE](https://github.com/mitll/graph-qube) created by MIT Lincoln Labs in collaboration with Giant Oak. Defaults to <i>false</i>.</td>
			</tr>
		</tbody>
	</table>
</div>

## <a name="cards"></a>Cards ##

The card properties control how cards, which represent individuals in your Entity database, are displayed within the Influent workspace.

<div class="props">
	<table class="summaryTable" width="100%">
		<thead>
			<tr>
				<th scope="col" width="15%">Property</th>
				<th scope="col" width="85%">Description</th>
			</tr>
		</thead>
		<tbody>
			<tr>
				<td class="property">doubleEncodeSourceUncertainty<!--background/--></td>
				<td class="description">Cards can be configured to indicate two dimensions of uncertainty:
					<ul>
						<li>How out of date the information is</li>
						<li>How uncertain the source is</li>
					</ul>
					<p>This property indicates whether both dimensions should be mapped from the same value. Defaults to <i>false</i></p>.
				</td>
			</tr>
			<tr>
				<td class="property">iconMap</td>
				<td class="description">JavaScript that controls the icons displayed on account cards in the Influent workspace. See the <a href="../../how-to/client-config/#cards">Client Configuration</a> topic for detailed instructions on using this property.</td>
			</tr>
			<tr>
				<td class="property">iconOrder</td>
				<td class="description">Specifies the order in which icons on cards should be displayed.</td>
			</tr>
		</tbody>
	</table>
</div>