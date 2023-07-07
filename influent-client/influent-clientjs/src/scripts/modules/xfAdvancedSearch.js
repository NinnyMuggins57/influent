/**
 * Copyright (c) 2013-2014 Oculus Info Inc.
 * http://www.oculusinfo.com/
 *
 * Released under the MIT License.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
define(['lib/module', 'lib/channels', 'lib/ui/criteria', 'modules/xfWorkspace'],
	function(modules, chan, criteriaUI, xfWorkspace) {

		//--------------------------------------------------------------------------------------------------------------
		// Private Variables
		//--------------------------------------------------------------------------------------------------------------

		var _UIObjectState = {
			UIType : 'xfAdvancedSearch',
			fileId : '',
			contextId : '',
			subscriberTokens : null
		};

		var _defaultAdvancedSearchCriteria = {};

		var _enableAdvancedSearchMatchType =
			aperture.config.get()['influent.config']['enableAdvancedSearchMatchType'];
		var _patternsToo =
			aperture.config.get()['influent.config']['usePatternSearch'];
		var _weighted =
			aperture.config.get()['influent.config']['enableAdvancedSearchWeightedTerms'];

		if (_enableAdvancedSearchMatchType == null) {
			_enableAdvancedSearchMatchType = true;
		}
		if (_weighted == null) {
			_weighted = true;
		}

		criteriaUI.weighted(_weighted);

		var _searchParams = {};
		var _defaultType = null;

		//--------------------------------------------------------------------------------------------------------------

		//--------------------------------------------------------------------------------------------------------------
		// Private Methods
		//--------------------------------------------------------------------------------------------------------------
		function onInit() {
			aperture.io.rest('/searchparams',
				'POST',
				function(response) {

					// store the search parameter list
					aperture.util.forEach(response.data, function(searchable) {
						var map = {};
						var keys = searchable.propertyDescriptors.map(function(d) {
							return d.key;
						});

						if (_defaultAdvancedSearchCriteria[searchable.type] === undefined) {
							_defaultAdvancedSearchCriteria[searchable.type] = [];
						}

						_searchParams[searchable.type] = {
							list : searchable.propertyDescriptors,
							keys : keys,
							map : map
						};

						if (!_defaultType) {
							_defaultType = searchable.type;
						}

						searchable.propertyDescriptors.forEach(function(pd) {
							map[pd.key] = pd;
							if (pd.defaultTerm) {
								_defaultAdvancedSearchCriteria[searchable.type].push(pd.key);
							}
						});
					});

					// build the static part of the UI
					buildDialog();
					
				},
				{
					postData : {},
					contentType: 'application/json'
				}
			);
		}

		//--------------------------------------------------------------------------------------------------------------
		function buildDialog() {

			var buttons = [];
			buttons.push({
				text: 'Search', 'class': 'searchButton',
				click: function () {
					$(this).dialog('close');
					onSearchAction(true);
				}});


			var advancedTabs = $('#advancedTabs');
			var tabList = $('<ul></ul>')
				.appendTo(advancedTabs);


			// attribute match tab.
			advancedTabs.append(buildAttributeTab(tabList, buttons));

			// activity match tab.
			if (_patternsToo) {
				advancedTabs.append(buildActivityTab(tabList, buttons));
			}

			// cancel buttton last.
			buttons.push({
				text: 'Cancel',
				click: function() {
					$(this).dialog('close');
				}});

			// tab construction
			advancedTabs.tabs({
				heightStyle: 'auto'
			});

			var dialog = $('#advancedDialog');
			dialog.dialog({
				height: 465,
				autoOpen: false,
				modal: true,
				buttons: buttons,
				show: {
					effect: 'clip',
					duration: 100
				},
				hide: {
					effect: 'clip',
					duration: 100
				},
				width:650
			});

			// ?
			dialog.css('display', '');
		}

		//--------------------------------------------------------------------------------------------------------------
		function onAnyAll() {
			var matchType = $('input[name=advancedSearchbooleanOperation]:checked').val();

			criteriaUI.multival(matchType === 'any'? true:false);
		}

		//--------------------------------------------------------------------------------------------------------------
		function buildAttributeTab(tabList, buttons) {
			var tabListItem = $('<li></li>')
				.appendTo(tabList);

			$('<a></a>')
				.attr('href', '#match-tab-attrs')
				.html('Match on Account Attributes')
				.appendTo(tabListItem);

			var attributes = $('<div></div>')
				.attr('id', 'match-tab-attrs')
				.addClass('match-tab')
				.css('white-space', 'nowrap')
				.append('<br>');

			var typeLine = $('<div></div>')
				.addClass('advancedsearch-type-line')
				.appendTo(attributes);

			$('<label></label>')
				.attr('for', 'advancedsearch-entity-type')
				.html('Find: ')
				.appendTo(typeLine);

			var typeOptions = $('<select></select>')
				.attr('id', 'advancedsearch-entity-type')
				.appendTo(typeLine)
				.change(function() {
					var text = assembleSearchString();
					setFieldsFromString(text);
				});


			// build type options.
			aperture.util.forEach(_searchParams, function(set, type) {
				$('<option></option>')
				.attr('value', type)
				.html(type)
				.appendTo(typeOptions);
			});

			// ANY / ALL option, is optional
			if (_enableAdvancedSearchMatchType) {
				var anyAllLine = $('<div></div>')
					.addClass('advancedsearch-any-all-line')
					.appendTo(attributes);


				anyAllLine.append($('<span/>').html('where '));

				var andRadio = $('<input/>').attr({
					type:'radio',
					name:'advancedSearchbooleanOperation',
					value:'all',
					checked:true
				}).change(onAnyAll);

				anyAllLine.append(andRadio).append('<span class="advancedsearch-any-all-span">'
						+ andRadio.attr('value') + '</span>');

				var orRadio = $('<input/>').attr({
					type:'radio',
					name:'advancedSearchbooleanOperation',
					value:'any'
				}).change(onAnyAll);

				anyAllLine.append(orRadio).append('<span class="advancedsearch-any-all-span">'
						+ orRadio.attr('value') + '</span>');
				anyAllLine.append($('<span/>').html(' of the following match:'));
			}

			$('<div></div>')
				.attr('id', 'advancedsearch-criteria-container')
				.appendTo(attributes);

			$('<div></div>')
				.attr('id', 'advancedsearch-criteria-add')
				.text('add more criteria')
				.appendTo(attributes)
				.click(function() {
					addCriteriaRow($('#advancedsearch-entity-type').val(), undefined, $('#advancedsearch-criteria-container'));
				});

			return attributes;
		}

		//--------------------------------------------------------------------------------------------------------------
		function buildActivityTab(tabList, buttons) {
			var activityTab = $('<li></li>');
			var activityHref = $('<a></a>');
			activityHref.attr('href', '#match-tab-activity');
			activityHref.html('Match on Account Activity');
			activityTab.append(activityHref);
			tabList.append(activityTab);

			var activity = $('<div></div>');
			activity.attr('id', 'match-tab-activity');
			activity.addClass('match-tab');

			activity.append('<br>Like Account(s):<br><br><br>');

			var idfield = $('<label></label>');
			idfield.attr('for', 'likeIdProperty');
			idfield.css('width', '30%');
			idfield.css('float', 'left');
			idfield.css('text-align', 'right');
			idfield.html('uid(s):&nbsp;&nbsp;');

			var idline= $('<div></div>').appendTo(activity);
			idline.append(idfield);

			idfield = $('<input/>');
			idfield.attr('id', 'likeIdProperty');
			idfield.addClass('textPropertyClass');
			idfield.css('width', 140);
			idline.append(idfield);

			var patternEngineDescription =
				aperture.config.get()['influent.config'].patternQueryDescriptionHTML ||
					'SORRY, this data set does not appear to be indexed for behavioral query by example!';

			$('<div id="patternEngineDescription"></div>').appendTo(activity).html(patternEngineDescription);

			// only if multi-role/graph search is possible should we bother providing an option of applying
			// without executing the search immediately.
			buttons.push({
				text: 'Apply',
				click: function () {
					$(this).dialog('close');
					onSearchAction(false);
			}});

			return activity;
		}

		//--------------------------------------------------------------------------------------------------------------
		function addDefaultRows(type,parent) {
			var targerCriteria = [];
			var targetKeys = _defaultAdvancedSearchCriteria[type];
			for (var i = 0; i < _searchParams[type].list.length; i++) {
				if (targetKeys.indexOf(_searchParams[type].list[i].key) !== -1) {
					targerCriteria.push(_searchParams[type].list[i]);
				}
			}

			if (targerCriteria.length === 0) {
				addCriteriaRow(type,undefined,parent);
			} else {
				for (i = 0; i < targerCriteria.length; i++) {
					addCriteriaRow(type,targerCriteria[i],parent);
				}
			}
		}

		//--------------------------------------------------------------------------------------------------------------
		function addCriteriaRow(type, criteria, parent) {
			var set = _searchParams[type];
			var key = null;

			if (criteria===null && _defaultAdvancedSearchCriteria) {
				addDefaultRows(type,parent);
				return;
			}


			if (set) {
				if (criteria) {
					key = criteria.key;
				} else {
					aperture.util.forEachUntil(set.list, function(c) {
						var used = aperture.util.forEachUntil(criteriaUI.list(), function(row) {
							return row.key();
						}, c.key);

						if (c.key !== used) {
							key = c.key;
							return true;
						}
					});
				}

				if (key) {
					var descriptor = set.map[key];

					if (descriptor) {
						var row = criteriaUI.add(set, descriptor, parent);

						if (criteria) {
							row.value(criteria.text);
						}
					}
				}
			}
		}
		//--------------------------------------------------------------------------------------------------------------
		function addBlankCriteriaRow() {
			addCriteriaRow($('#advancedsearch-entity-type').val(), null, $('#advancedsearch-criteria-container'));
		}

		//--------------------------------------------------------------------------------------------------------------
		function clear() {
			criteriaUI.list().forEach(function(ui) {
				ui.remove();
			});
		}

		//--------------------------------------------------------------------------------------------------------------
		function onAdvancedSearchDialogRequest(eventChannel, data) {
			_UIObjectState.fileId = data.fileId;
			_UIObjectState.contextId = data.contextId;

			if (data.dataIds != null && !_.isEmpty(data.dataIds)) {
				setFieldsFromDataIds(data.dataIds);
				$('#advancedTabs').tabs('select', 0);
			} else {
				setFieldsFromString(data.terms);
			}

			$('#advancedDialog').dialog('open');
		}

		//--------------------------------------------------------------------------------------------------------------
		function parseQuery(searchString) {
			var nameValuePattern = /([^:\s]+)\s*:\s*(.+?)\s*(([^:\s]+):|$)/;
			var match = nameValuePattern.exec(searchString);
			var criteria = {list:[], map:{}};
			var key;

			while (match != null) {
				key = match[1];
				if (key.charAt(0) === '-') {
					key = key.substr(1);
				}

				criteria.list.push({
					key : key,
					text : match[1] + ':' + match[2]
				});

				criteria.map[key] = match[2];

				var matchlen = match[0].length - match[3].length;
				if (matchlen >= searchString.length) {
					break;
				}

				searchString = searchString.substr(matchlen);
				match = nameValuePattern.exec(searchString);
			}

			return criteria;
		}

		//--------------------------------------------------------------------------------------------------------------
		function seedFromEntities(entities) {
			var entity;
			var map = {matchtype : 'any'};
			var list = [{key: 'uid', text: ''}];
			var entitiesString = '';
			var termList = _searchParams[_defaultType].keys;

			if (!entities || entities.length === 0) {
				aperture.log.warn('no entities found for population request');
				return;
			}

			entities = entities[0].entities;

			for (var i = 0; i < entities.length; i++) {
				entity = entities[i];

				entitiesString += entity.uid + ', ';

				if (!map.datatype) {
					var etype = xfWorkspace.getValueByTag(entity, 'TYPE');
					if (etype && _searchParams[etype]) {
						map.datatype = etype;
						termList = _searchParams[etype].keys;
					}
				}

				for (var propKey in entity.properties ) {
					if ( entity.properties.hasOwnProperty(propKey) ) {
						var property = entity.properties[propKey];

						if (property.value && property.value !== '') {
							var useIt = true;

							if ( property.tags ) {
								for (var j = 0; j < property.tags.length; j++ ) {
									if (property.tags[j] === 'ID' ||
										property.tags[j] === 'GEO'
									) {
										useIt = false;
										break;
									}
								}
							}

							// Don't seed any empty values
							if (property.value === '') {
								useIt = false;
							}

							if (useIt) {
								if (map.hasOwnProperty(propKey)) {
									if (map[propKey].indexOf(property.value) === -1) {
										map[propKey] += ', ' + property.value;
									}
								} else {
									map[propKey] = String(property.value);
									list.push({
										key: propKey,
										text: '',
										ordinal: termList.indexOf(propKey)
									});
								}
							}
						}
					}
				}
			}

			map.uid = entitiesString.substring(0, entitiesString.length - 2);

			// copy final values into list as text
			list.forEach(function(p) {
				p.text = p.key+ ':'+ map[p.key];
			});

			list.sort(function(a,b) {
				return a.ordinal - b.ordinal;
			});

			setFieldsFromProperties({
				map: map,
				list: list
			});
		}

		//--------------------------------------------------------------------------------------------------------------
		function setFieldsFromString(searchString) {
			clear();

			if (searchString == null || searchString.length === 0) {
				addBlankCriteriaRow();
				return;
			}

			var criteria = parseQuery(searchString);

			if (criteria.map.like) {
				setFieldsFromDataIds(criteria.map.like.split(','));
				$('#advancedTabs').tabs('select', 1);
			} else {
				setFieldsFromProperties(criteria);
				$('#advancedTabs').tabs('select', 0);
			}
		}

		//--------------------------------------------------------------------------------------------------------------
		function setFieldsFromProperties(criteria) {
			var typeOpt = $('#advancedsearch-entity-type');

			if (criteria.map.datatype) {
				typeOpt.val(criteria.map.datatype);
			}
			var matchtype = criteria.map.matchtype;

			if (matchtype) {
				var matchtypeSel = '[value='+ matchtype + ']';

				var pair = $('input[name=advancedSearchbooleanOperation]');
				pair.not(matchtypeSel).attr('checked','');
				pair.filter(matchtypeSel).attr('checked','checked');
			}

			if (criteria.map.uid) {
				$('#likeIdProperty').val(criteria.map.uid? criteria.map.uid: '');
			} else {
				$('#likeIdProperty').val('');
			}

			var type = typeOpt.val();
			var parent = $('#advancedsearch-criteria-container');

			criteria.list.forEach(function(c) {
				addCriteriaRow(type, c, parent);
			});

			if (criteriaUI.list().length === 0) {
				addBlankCriteriaRow();
			}
		}

		//--------------------------------------------------------------------------------------------------------------
		function setFieldsFromDataIds(dataIds) {
			clear();

			aperture.io.rest(
				'/containedentities',
				'POST',
				function (response) {
					seedFromEntities(response.data);

					if (criteriaUI.list().length === 0) {
						addBlankCriteriaRow();
					}
				},
				{
					postData : {
						sessionId : xfWorkspace.getSessionId(),
						entitySets : [{
							contextId : _UIObjectState.contextId,
							entities : dataIds
						}],
						details : true
					},
					contentType: 'application/json'
				}
			);
		}

		//--------------------------------------------------------------------------------------------------------------
		function onSearchAction(execute) {
			var searchTerm = assembleSearchString();

			aperture.pubsub.publish(chan.SEARCH_REQUEST, {
				xfId : _UIObjectState.fileId,
				searchTerm : searchTerm,
				executeSearch : execute,
				noRener: true
			});
		}

		//--------------------------------------------------------------------------------------------------------------
		function assembleSearchString() {
			var isPattern = $('#advancedTabs').tabs( 'option', 'selected' );

			if (isPattern) {
				var id = $('#likeIdProperty').val();
				return id? 'like:'+ id : '';
			}
			
			var searchString = '';

			criteriaUI.list().forEach(function(ui) {
				var val = ui.value();

				if (val && val.length !== 0) {
					searchString += val+ ' ';
				}
			});


			if(searchString.length > 0) {
				searchString += 'datatype:'+ $('#advancedsearch-entity-type').val();
				
				if (_enableAdvancedSearchMatchType) {
					searchString += ' matchtype:"' + $('input[name=advancedSearchbooleanOperation]:checked').val() + '" ';
				}
			}

			return searchString;
		}
		
		//--------------------------------------------------------------------------------------------------------------
		// Public
		//--------------------------------------------------------------------------------------------------------------

		var xfAdvancedSearchModule = {};

		//--------------------------------------------------------------------------------------------------------------

		// Register the module with the system
		modules.register('xfAdvancedSearch', function() {
			return {
				start : function() {
					var subTokens = {};
					subTokens[chan.ADVANCE_SEARCH_DIALOG_REQUEST] = aperture.pubsub.subscribe(chan.ADVANCE_SEARCH_DIALOG_REQUEST, onAdvancedSearchDialogRequest);
					subTokens[chan.ALL_MODULES_STARTED] = aperture.pubsub.subscribe(chan.ALL_MODULES_STARTED, onInit);
					_UIObjectState.subscriberTokens = subTokens;
				},
				end : function(){
					for (var token in _UIObjectState.subscriberTokens) {
						if (_UIObjectState.subscriberTokens.hasOwnProperty(token)) {
							aperture.pubsub.unsubscribe(_UIObjectState.subscriberTokens[token]);
						}
					}
				}
			};
		});

		return xfAdvancedSearchModule;
	}
);
