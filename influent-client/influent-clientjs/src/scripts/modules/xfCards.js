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

define(
	[
		'lib/module', 'lib/channels', 'lib/util/currency', 'modules/xfWorkspace', 'modules/xfRest',
		'lib/models/xfClusterBase', 'lib/models/xfCard', 'lib/util/xfUtil', 'lib/constants'
	],
	function(
		modules, chan, currency, xfWorkspace, xfRest,
		xfClusterBase, xfCard, xfUtil, constants
	) {
		var DEFAULT_GRAPH_SCALE = (aperture.config.get()['influent.config']['defaultGraphScale'] != null) ? aperture.config.get()['influent.config']['defaultGraphScale'] : 0;

		var module = {};

		//--------------------------------------------------------------------------------------------------------------

		var _getClusterIcons = function (cluster, clusterCount) {

			var icons = [];

			var iconClassOrder = aperture.config.get()['influent.config'].iconOrder;
			var iconClassMap = aperture.config.get()['influent.config'].iconMap;
			var iconClass = null;

			for(var i = 0; i < iconClassOrder.length; i++) {
				iconClass = iconClassOrder[i];
				if (iconClassMap[iconClass]) {
					var properties = xfWorkspace.getPropertiesByTag(cluster, iconClass);

					for (var pi=0, pn= properties.length; pi<pn; pi++) {
						var property = properties[pi];
						var iconDist = property.range;

						if (iconDist) {
							var iconMap = iconClassMap[iconClass].map;

							if(iconMap != null) {
								var friendlyName = property.friendlyText;
								var newDistIcons = _generateDistIcons(iconClass, iconMap, iconDist, clusterCount, friendlyName);
								for(var j = 0; j < newDistIcons.length; j++) {
									icons.push(newDistIcons[j]);
								}
							}
							else {
								aperture.log.error('Unable to render icons of type ' + iconClass + '; misconfiguration in config.js.');
							}
						}
					}
				} else {
					aperture.log.error('icon order references a type that doesnt exist');
				}
			}

			return icons;
		};

		//--------------------------------------------------------------------------------------------------------------

		var _generateDistIcons = function (iconClass, iconMap, iconDist, clusterCount, friendlyName) {
			var toReturn = [];
			var count = 0;
			var nextIcon = null;
			var value = null;
			var d = iconDist.distribution;
			var i;
			var freq;

			for (i=0; i< d.length; i++) {
				freq = d[i];

				value = freq.range;
				count = freq.frequency;

				nextIcon = iconMap(iconClass, value);

				if (nextIcon) {
					toReturn.push(
						{
							type: iconClass,
							imgUrl: nextIcon.url || aperture.palette.icon(nextIcon.icon),
							title: (nextIcon.title || '') + ' ('+ count+ ')',
							score : count / clusterCount,
							friendlyName: friendlyName
						}
					);
				}
			}

			toReturn.sort(function(a, b) {return b['score'] - a['score'];});
			return toReturn;
		};

		//----------------------------------------------------------------------------------------------------------

		var _processEntityCluster = function(type, elementData, spec) {

			var stubMembers = [];
			var i=0;
			var memberSpec;
			for (i = 0; i < elementData.members.length; i++) {
				memberSpec = xfCard.getSpecTemplate();
				memberSpec.dataId = elementData.members[i];
				memberSpec.graphUrl = '';
				stubMembers.push(memberSpec);
			}

			for (i = 0; i < elementData.subclusters.length; i++) {
				memberSpec = xfClusterBase.getSpecTemplate();
				memberSpec.dataId = elementData.subclusters[i];
				memberSpec.graphUrl = '';
				stubMembers.push(memberSpec);
			}

			var entityCount = xfWorkspace.getEntityCount(elementData);
			var label = xfWorkspace.getValueByTag(elementData, 'LABEL');

			spec.dataId = elementData.uid;
			spec.accounttype = type;
			spec.members = stubMembers;
			spec.icons = _getClusterIcons(elementData, entityCount);
			spec.count = entityCount;
			spec.label = label;
			spec.inDegree = xfWorkspace.getValueByTag(elementData, 'INFLOWING');
			spec.outDegree = xfWorkspace.getValueByTag(elementData, 'OUTFLOWING');
			spec.promptForDetails = elementData.promptForDetails;

			if (type == constants.ACCOUNT_TYPES.ACCOUNT_OWNER) {
				spec.ownerId = spec.dataId;
			} else {
				spec.ownerId = xfWorkspace.getValueByTag(elementData, 'ACCOUNT_OWNER');

				// FIXME: Temp fix to protect against incoming null tags
				if (spec.ownerId === null) {
					spec.ownerId = '';
				}
			}
		};

		//----------------------------------------------------------------------------------------------------------

		var _processEntity = function(type, elementData, spec) {

			spec.dataId = elementData.uid;
			spec.accounttype = type;
			spec.label = xfWorkspace.getValueByTag(elementData, 'LABEL');
			spec.inDegree = xfWorkspace.getValueByTag(elementData, 'INFLOWING');
			spec.outDegree = xfWorkspace.getValueByTag(elementData, 'OUTFLOWING');
			spec.promptForDetails = elementData.promptForDetails;
			spec.icons = [];

			// do icons
			var iconMap = aperture.config.get()['influent.config'].iconMap;
			var tag = '';
			var tagMap;
			var property;
			var properties;
			var i, j;
			var count;
			var limit = 4;
			var values;
			var icon;

			for (tag in iconMap) {
				if (iconMap.hasOwnProperty(tag)) {
					properties = xfWorkspace.getPropertiesByTag(elementData, tag);

					if (properties.length) {
						tagMap = iconMap[tag];
						count = 0;

						for (i=0; i< properties.length; i++) {
							property = properties[i];

							if (property.range && property.range.values) {
								values = property.range.values;
							} else {
								values = [property.value];
							}

							for (j=0; j< values.length; j++) {
								icon = tagMap.map(property.key, values[j]);

								if (icon) {
									spec.icons.push({
										type: tag,
										imgUrl: icon.url || aperture.palette.icon(icon.icon),
										title: icon.title,
										friendlyName: property.friendlyText
									});

									if ((tagMap.limit && ++count === tagMap.limit) || spec.icons.length === limit) {
										break;
									}
								}
							}
						}
					}

					if (spec.icons.length === limit) {
						break;
					}
				}
			}
		};

		//--------------------------------------------------------------------------------------------------------------

		var _populateSpecWithData = function(elementData, spec) {

			if (elementData == null) {
				return;
			}

			if (spec.hasOwnProperty('confidenceInSrc') && elementData.uncertainty) {
				spec.confidenceInSrc = elementData.uncertainty.confidence;

				var doubleEncode = aperture.config.get()['influent.config'].doubleEncodeSourceUncertainty;

				if (doubleEncode != null && !doubleEncode) {
					spec.confidenceInAge = elementData.uncertainty.currency != null?
							elementData.uncertainty.currency : 1.0;
				} else {
					spec.confidenceInAge = spec.confidenceInSrc < 1.0? 0.0: 1.0;
				}
			}

			var type = xfUtil.getAccountTypeFromDataId(elementData.uid);
			if (type == constants.ACCOUNT_TYPES.ENTITY) {
				_processEntity(type, elementData, spec);
			} else {
				_processEntityCluster(type, elementData, spec);
			}
		};

		//--------------------------------------------------------------------------------------------------------------

		var _invalidateCardCharts = function(specs) {

			for (var i = 0; i < specs.length; i++) {

				var spec = specs[i];
				if (!spec.graphUrl) {
					continue;
				}

				var objects = xfWorkspace.getUIObjectsByDataId(spec.dataId);
				for (var j = 0; j < objects.length; j++) {
					var object = objects[j];
					object.update({
						graphUrl : ''
					});
					aperture.pubsub.publish(chan.RENDER_UPDATE_REQUEST, {UIObject : object});
				}
			}
		};

		//--------------------------------------------------------------------------------------------------------------

		var _updateCardsWithCharts = function(specs) {

			if(specs.length === 0) {
				return;
			}

			_invalidateCardCharts(specs);

			var updates = [];
			var focus = xfWorkspace.getFocus();

			getMaxDebitCredit(
				focus,
				function(focusMDC) {
					for (var i = 0; i < specs.length; i++) {

						var objects = xfWorkspace.getUIObjectsByDataId(specs[i].dataId);

						if (objects.length > 0) {
							for (var j = 0; j < objects.length; j++) {
								var specUIObj = objects[j];

								var contextObj = xfUtil.getContextByUIObject(specUIObj);

								updates.push({
									dataId : specs[i].dataId,
									contextId : contextObj.getXfId()
								});
							}
						}
					}

					updateCardTypeWithCharts(focus, focusMDC, updates);
				}
			);
		};

		//--------------------------------------------------------------------------------------------------------------

		function getMaxDebitCredit(focus, onReturn) {

			if (focus == null) {
				onReturn(DEFAULT_GRAPH_SCALE);
				return;
			}

			var focusId = focus.dataId;

			var entity = {
				contextId : focus.contextId,
				dataId : focusId
			};

			// Separate call for files?
			xfRest.request('/chart').inContext(entity.contextId).withData({

				sessionId: xfWorkspace.getSessionId(),
				entities: [entity],
				startDate: xfWorkspace.getFilterDates().startDate,
				endDate: xfWorkspace.getFilterDates().endDate,
				numBuckets: xfWorkspace.getFilterDates().numBuckets,
				focusId: [focus.dataId],
				focusMaxDebitCredit: '',
				focuscontextid: focus.contextId

			}).then(function (response) {

				var responseData = response[focusId];
				var maxCreditDebit = responseData ?
						responseData.maxDebit > responseData.maxCredit ?
					responseData.maxDebit : responseData.maxCredit : null;

				onReturn(maxCreditDebit);
			});
		}

		//--------------------------------------------------------------------------------------------------------------

		function updateCardTypeWithCharts(focus, focusMDC, entityIdArray) {

			if ( entityIdArray.length === 0 ) {
				return;
			}


			if (!_.isEmpty(entityIdArray)){
					var entityByContext = [];
					for (var i = 0; i < entityIdArray.length; i++) {
						if (!entityByContext[entityIdArray[i].contextId]) {
							entityByContext[entityIdArray[i].contextId] = [];
						}

						entityByContext[entityIdArray[i].contextId].push(entityIdArray[i]);
					}


					for (var context in entityByContext) {
						if (entityByContext.hasOwnProperty(context)) {

							xfRest.request('/chart').inContext( context ).withData({

								sessionId: xfWorkspace.getSessionId(),
								entities: entityByContext[context],
								startDate: xfWorkspace.getFilterDates().startDate,
								endDate: xfWorkspace.getFilterDates().endDate,
								numBuckets: xfWorkspace.getFilterDates().numBuckets,
								focusId: (focus == null) ? null : [focus.dataId],
								focusMaxDebitCredit: focusMDC,
								focuscontextid: (focus == null) ? null : focus.contextId
							}).then( _updateCard );
						}
					}
			}
		}

		//--------------------------------------------------------------------------------------------------------------

		function _updateCard(response) {

			var parents = {};
			for (var dataId in response) {
				if (response.hasOwnProperty(dataId)) {
					var cards = xfWorkspace.getUIObjectsByDataId(dataId);
					for (var i = 0; i < cards.length; i++) {

						var card = cards[i];

						// Store the parent in the map.
						var parent = card.getParent();
						parents[parent.getXfId()] = parent;

						card.showDetails(xfWorkspace.getVisualInfo().showDetails);
						card.update({
								graphUrl : aperture.io.restUrl('/chart?hash=' + xfUtil.getSafeURI(response[dataId].imageHash)),
								flow : {flowIn: response[dataId].totalCredit, flowOut: response[dataId].totalDebit}}
						);
					}
				}
			}

			for (var parentId in parents) {
				if (parents.hasOwnProperty(parentId)) {
					aperture.pubsub.publish(chan.RENDER_UPDATE_REQUEST, {UIObject : parents[parentId]});
				}
			}
		}

		//--------------------------------------------------------------------------------------------------------------
		module.updateCardsWithCharts = _updateCardsWithCharts;
		module.populateSpecWithData = _populateSpecWithData;
		//--------------------------------------------------------------------------------------------------------------

		// Register the module with the system
		modules.register('xfCards', function(sandbox) {
			xfWorkspace.registerChildModule(module);

			return {
				start: function() {
					xfWorkspace.start(sandbox.spec.sessionId, sandbox.spec.entityId);
				},
				end : function() {
					xfWorkspace.end();
				}
			};
		});

		return module;
	}
);

