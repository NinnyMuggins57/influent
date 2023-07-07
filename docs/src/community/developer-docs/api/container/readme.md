---
section: Community
subsection: Developer Docs
chapter: API
topic: Container API
permalink: community/developer-docs/api/container/
layout: submenu
---

# Container API #

In addition to a list of property match descriptors for selected results, the view plugin receives a container API object via the view function whenever a user:

- Clicks the toolbar button for a view plugin
- Revisits a stateless view through a refresh, bookmark, or the browser history 

Container API objects expose app functionality to the views. For more information on creating custom views for your application, see the [Plugin Views](../../reference/client/#plugin-views) section of the [Client Settings](../../reference/client/) reference topic.

## <a name="members"></a> Members ##

The Container API contains the members described in the following sections.

### element ###

The element member contains the raw DOM element the view plugin will populate.

### events ###

The events member should be used by both stateless and stateful views to send notifications of state change to the application.

<div class="props">
	<table class="summaryTable" width="100%">
		<thead>
			<tr>
				<th scope="col" width="35%">Methods</th>
				<th scope="col" width="65%">Description</th>
			</tr>
		</thead>
		<tbody>
			<tr>
				<td class="description"><strong>stateChange</strong>(
					<br>descriptors:PropertyMatchDescriptors,
					<br>limit:number
					<br>orderBy:OrderBy)
				</td>
				<td class="description">Send notification of state change. This will update the URL.</td>
			</tr>
		</tbody>
	</table>
</div>

### search ###

The search member can be used to invoke entities and link (transaction) searches.

<div class="props">
	<table class="summaryTable" width="100%">
		<thead>
			<tr>
				<th scope="col" width="35%">Methods</th>
				<th scope="col" width="65%">Description</th>
			</tr>
		</thead>
		<tbody>
			<tr>
				<td class="description"><strong>entities</strong>(
					<br>descriptors:PropertyMatchDescriptors,
					<br>callback:function
					<br>limit:number
					<br>orderBy:OrderBy)
				</td>
				<td class="description">Returns a set of Entity search results (not exceeding limit) through the callback function based on matching properties. Results are ordered based on orderBy.</td>
			</tr>
			<tr>
				<td class="description"><strong>links</strong>(
					<br>descriptors:PropertyMatchDescriptors,
					<br>callback:function
					<br>limit:number
					<br>orderBy:OrderBy)
				</td>
				<td class="description">Returns a set of Link (transaction) search results (not exceeding limit) through the callback function based on matching properties. Results are ordered based on orderBy.</td>
			</tr>
		</tbody>
	</table>
</div>