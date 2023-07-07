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
define(['jquery', 'lib/module', 'lib/channels', 'modules/xfWorkspace'],
    function($, modules, chan, xfWorkspace) {

        //--------------------------------------------------------------------------------------------------------------
        // Private Variables
        //--------------------------------------------------------------------------------------------------------------

        var MODULE_NAME = 'xfAdvancedSearch';

        var _UIObjectState = {
            UIType : MODULE_NAME,
            childModule : undefined,
            properties : {},
            fileId : '',
            subscriberTokens : null
        };

        var _buildForType = null;

        var _blank = {
            value: '',
            not: false,
            fuzzy: false
        };

        //--------------------------------------------------------------------------------------------------------------
        // Private Methods
        //--------------------------------------------------------------------------------------------------------------

        var _onAdvancedSearchDialogRequest = function(eventChannel, data) {

            if (data.dataIds != null && !_.isEmpty(data.dataIds)) {
                setFieldsFromDataIds(data.dataIds);
                $('#advancedTabs').tabs('select', 0);
            } else if (data.terms != null) {
                setFieldsFromString(data.terms);
            } else {
                setFieldsFromString('');
            }
        	
            _UIObjectState.fileId = data.fileId;
            $('#advancedDialog').dialog('open');
        };

        //--------------------------------------------------------------------------------------------------------------
        
        var _modalBoxCreator = function() {
            var currentProps = [];

            aperture.io.rest('/searchparams?queryId='+(new Date()).getTime(),
                'GET',
                function(response) {
                    var entityColumns = [];
                    if (response.data.length != 0) {

                        var maxFields = 15;

                        for (var x = 0; x < response.data.length; x++) {
                            var key = response.data[x];
                            var type = key['type'];
                            var description = key.propertyDescriptors;
                            var desc = [];

                            if (maxFields < description.length-1)
                                description.splice (maxFields, description.length-1);

                            for (var v = 0; v < description.length; v++) {
                            	var range = description[v].range;
                            	var constraints = {};
                            	
                            	// TODO may still need to iron out incompatability issues w v1.5:
                            	// This is designed to take in ALL valid constraints for a given FL_PropertyDescriptor so that the user only sees an appropriate subset of constraints for each property.
                            	// Thus, for this to work properly, one either needs to add a 'getValidConstraints' to FL_PropertyDescriptor OR pass back different object with this information in getDescriptors().
                            	// Either way, an API change will be needed to accommodate this :(
                            	
                        		constraints.EQUALS = true;
                        		constraints.FUZZY  = true;
                        		constraints.NOT    = true;
                            	constraints[range] = true;
                            	
                                desc.push({
                                	key: description[v]['key'], 
                                	suggestedTerms: description[v]['suggestedTerms'], 
                                	friendlyText: description[v]['friendlyText'],
                                	constraints: constraints
                                });
                            }

                            entityColumns.push({type: type, data: desc});
                        }
                    }
                    
                    _initializeModalComponents(entityColumns);
                    currentProps = entityColumns[0]['data'];

                    _buildForType = function () {
                        var propertyContainer = $('#propertyContainer');
                        var propertyLine;
                        var oldFriendlyNames = [];
                        var oldFieldValues = [];
                        var oldNotValues = [];
                        var oldFuzzyValues = [];

                        var oldInd = -1;
                        for (var i = 0; i < entityColumns[0]['data'].length; i++) {
                            if(entityColumns[i]['data'] == _UIObjectState.properties) {
                                oldInd = i;
                                break;
                            }
                        }

                        if(oldInd != -1) {
                        	for (var i = 0; i < entityColumns[oldInd]['data'].length; i++) {
                        		oldFriendlyNames[i] = entityColumns[oldInd]['data'][i]['friendlyText'];
                        	}
                        	
                            propertyContainer.children().each(function() {
                            	$(this).children().each(function() {
                                	var idName = $(this).attr('id');
                                	var idx = -1;
                                	if(idName != null) {
                                        if(idName.indexOf('textProperty') == 0) {
                                        	idx = idName.substring('textProperty'.length);
                                        	oldFieldValues[idx] = $(this).val();
                                        }
                                        else if(idName.indexOf('not') == 0) {
                                        	idx = idName.substring('not'.length);
                                        	oldNotValues[idx] = $(this).attr('checked');
                                        }
                                        else if(idName.indexOf('fuzzy') == 0) {
                                        	idx = idName.substring('fuzzy'.length);
                                        	oldFuzzyValues[idx] = $(this).attr('checked');
                                        }
                                	}
                            	});
                            });
                        }

                        propertyContainer.empty();
                        var col = $(this).val();

                        var ind = 0;
                        for (var x = 0; x < entityColumns.length; x++) {
                            if (entityColumns[x]['type'] == col) {
                                ind = x;
                                break;
                            }
                        }

                        for (var i = 0; i < entityColumns[ind]['data'].length; i++) {
                        	propertyLine = $('<div></div>');
                            var inputType = entityColumns[ind]['data'][i]['suggestedTerms'].length == 0 ?  'input' : 'select';

                            var element = $('<label></label>');
                            element.attr('for', ('textProperty' + i.toString()));
                            element.css('width', '30%');
                            element.css('float', 'left');
                            element.css('text-align', 'right');
                            element.html(entityColumns[ind]['data'][i]['friendlyText'] + ':&nbsp;&nbsp;');
                            propertyLine.append(element);

                            element = $('<' + inputType + '/>');
                            element.attr('id', ('textProperty' + i.toString()));
                            element.addClass('textPropertyClass');
                            element.css('width', 140);
                            
                            var oldFriendlyNameIdx = jQuery.inArray(entityColumns[ind]['data'][i]['friendlyText'], oldFriendlyNames);
                            if(oldFriendlyNameIdx != -1) {
                            	element.val(oldFieldValues[oldFriendlyNameIdx]);
                            }

                            if (inputType == 'select') {
                                var option = $('<option></option>');
                                element.append(option);
                                for (var j = 0; j < entityColumns[ind]['data'][i]['suggestedTerms'].length; j++) {
                                    option = $('<option></option>');
                                    option.html(entityColumns[ind]['data'][i]['suggestedTerms'][j]);
                                    element.append(option);
                                }
                            }

                            propertyLine.append(element);

                            
                            element = $('<label></label>');
                            element.attr('for', ('not' + i.toString()));
                            element.html('  &nbsp;&nbsp;Not?: ');
                            propertyLine.append(element);
                            
                            element = $('<input/>');
                            element.attr('id', ('not' + i.toString()));
                            element.attr('type', 'checkbox');
                            element.css('position', 'relative');
                            element.css('top', 3);
                            if(oldFriendlyNameIdx != -1) {
                        		element.attr('checked', oldNotValues[oldFriendlyNameIdx]);
                            }
                            propertyLine.append(element);

                            element = $('<label></label>');
                            element.attr('for', ('fuzzy' + i.toString()));
                            element.html('  &nbsp;&nbsp;Fuzzy?: ');
                            propertyLine.append(element);

                            element = $('<input/>');
                            element.attr('id', ('fuzzy' + i.toString()));
                            element.attr('type', 'checkbox');
                            element.css('position', 'relative');
                            element.css('top', 3);
                            if(oldFriendlyNameIdx != -1) {
                            	element.attr('checked', oldFuzzyValues[oldFriendlyNameIdx]);
                            }
                            propertyLine.append(element);
                            
                            propertyContainer.append(propertyLine);
                        }

                        currentProps = entityColumns[ind]['data'];
                        propertyContainer.append('<br><br>');

                        _UIObjectState.properties = currentProps;
                        _constructDialog(_UIObjectState);
                    };
                    
                    $('#entities').change(_buildForType);

                    _UIObjectState.properties = currentProps;
                    _constructDialog(_UIObjectState);
                },
                {}
            );
        };

        //--------------------------------------------------------------------------------------------------------------

        var _initializeModalComponents = function (entityCols) {

            var first = entityCols[0]['data'];
            var i;

            var advancedTabs = $('#advancedTabs');

            var tabs = $('<ul></ul>');
            advancedTabs.append(tabs);

            var attributeTab = $('<li></li>');
            var attributeHref = $('<a></a>');
            attributeHref.attr('href', '#match-tab-attrs');
            attributeHref.html('Match on Account Attributes');
            attributeTab.append(attributeHref);
            tabs.append(attributeTab);

            var attributes = $('<div></div>');
            attributes.attr('id', 'match-tab-attrs');
            attributes.addClass('match-tab');

            attributes.css('white-space', 'nowrap');
            attributes.append('<br>');

            var element = $('<label></label>');
            element.attr('for', 'entities');
            element.html('Type: ');
            attributes.append(element);

            element = $('<select></select>');
            element.attr('id', 'entities');

            for (i = 0; i < entityCols.length; i++) {
                var option = $('<option></option>');
                option.attr('value', entityCols[i]['type']);
                option.html(entityCols[i]['type']);
                element.append(option);
            }

            attributes.append(element);
            attributes.append('<br>');

            attributes.append('<br>');
            attributes.append('<br>');

            var propertyContainer = $('<div></div>');
            var propertyLine;
            propertyContainer.attr('id', 'propertyContainer');

            for (i = 0; i < first.length; i++) {
            	propertyLine = $('<div></div>');
            	propertyLine.attr('id', 'propertyLine-' + first[i]['friendlyText']);
            	
                var inputType = first[i]['suggestedTerms'].length == 0 ?  'input' : 'select';

                element = $('<label></label>');
                element.attr('for', ('textProperty' + i.toString()));
                element.css('width', '30%');
                element.css('float', 'left');
                element.css('text-align', 'right');
                element.html(first[i]['friendlyText'] + ':&nbsp;&nbsp;');
                propertyLine.append(element);

                element = $('<' + inputType + '/>');
                element.attr('id', ('textProperty' + i.toString()));
                element.addClass('textPropertyClass');
                element.css('width', 140);

                if (inputType == 'select') {
                    for (var j = 0; j < first[i]['suggestedTerms'].length; j++) {
                        option = $('<option></option>');
                        option.html(first[i]['suggestedTerms'][j]);
                        element.append(option);
                    }
                }

                propertyLine.append(element);
                
                
                element = $('<label></label>');
                element.attr('for', ('not' + i.toString()));
                element.html('  &nbsp;&nbsp;Not?: ');
                propertyLine.append(element);
                
                if (!first[i].constraints.NOT) {
                	element.css('display', 'none');
                }
                
                element = $('<input/>');
                element.attr('id', ('not' + i.toString()));
                element.attr('type', 'checkbox');
                element.css('position', 'relative');
                element.css('top', 3);
                propertyLine.append(element);

                if (!first[i].constraints.NOT) {
                	element.css('display', 'none');
                }
                
                
                element = $('<label></label>');
                element.attr('for', ('fuzzy' + i.toString()));
                element.html('  &nbsp;&nbsp;Fuzzy?: ');
                propertyLine.append(element);

                if (!first[i].constraints.EQUALS || !first[i].constraints.FUZZY) {
                	element.css('display', 'none');
                }
                
                element = $('<input/>');
                element.attr('id', ('fuzzy' + i.toString()));
                element.attr('type', 'checkbox');
                element.css('position', 'relative');
                element.css('top', 3);
                propertyLine.append(element);
                
                
                if (!first[i].constraints.EQUALS || !first[i].constraints.FUZZY) {
                	element.css('display', 'none');
                }
                
                propertyContainer.append(propertyLine);
            }

            $('#propertyContainer').append('<br><br>');
            attributes.append(propertyContainer);

            advancedTabs.append(attributes);

            if (aperture.config.get()['influent.config']['usePatternSearch']) {

                var activityTab = $('<li></li>');
                var activityHref = $('<a></a>');
                activityHref.attr('href', '#match-tab-activity');
                activityHref.html('Match on Account Activity');
                activityTab.append(activityHref);
                tabs.append(activityTab);

                var activity = $('<div></div>');
                activity.attr('id', 'match-tab-activity');
                activity.addClass('match-tab');

                activity.append('<br>Like Account(s):<br><br><br>');

                var idfield = $('<label></label>');
                idfield.attr('for', 'likeIdProperty');
                idfield.css('width', '30%');
                idfield.css('float', 'left');
                idfield.css('text-align', 'right');
                idfield.html('id(s):&nbsp;&nbsp;');

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

                advancedTabs.append(activity);
            }

            return entityCols[0]['type'];
        };

        //--------------------------------------------------------------------------------------------------------------
        
        /**
         * Parse a query string and return a hash of named value objects, each containing value, not, fuzzy
         */
        function parseQuery(searchString) {
        	var nameValuePattern = /([^:\s]+)\s*:\s*(.+?)\s*(([^:\s]+):|$)/;
        	var match = nameValuePattern.exec(searchString);
        	var props = {};
        	var key, value;
        	var not, fuzzy;
        	
        	while (match != null) {
        		key = match[1];
        		value = match[2];
        		
        		if (not = !!(key.charAt(0) === '-')) {
        			key = key.substr(1);
        		}
        		if (!(fuzzy = !(value.charAt(0) === '"'))) {
        			value = value.substring(1, value.length-1);
        		}
        		props[key] = {
        			value: value,
        			not: not,
        			fuzzy : fuzzy
        		};
        		
        		var matchlen = match[0].length - match[3].length;
        		if (matchlen >= searchString.length) {
        			break;
        		}
        		
        		searchString = searchString.substr(matchlen);
        		match = nameValuePattern.exec(searchString);
        	}
        	
        	return props;
        }

        //--------------------------------------------------------------------------------------------------------------

        function seedFromEntities(entities) {
        	var entity;
        	var props = {};
            var entitiesString = '';
            var isCluster = entities.length > 1;
        	
        	for (var i = 0; i < entities.length; i++) {
        		entity = entities[i];

                entitiesString += entity.uid + ', ';

        		props.datatype = {
    				value: xfWorkspace.getValueByTag(entity, 'TYPE')
        		};
        		
                for (var propKey in entity.properties ) {
                    if ( entity.properties.hasOwnProperty(propKey) ) {
                        var property = entity.properties[propKey];
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
                        
                        if (useIt) {
                            if (props.hasOwnProperty(propKey)) {
                                if (props[propKey].value.indexOf(property.value) == -1) {
                                    props[propKey].value += ", " + property.value;
                                }
                            } else {
                                props[propKey] = {
                                    value: new String(property.value),
                                    fuzzy: true
                                };
                            }
                        }
                    }
                }
        	}

            props['id'] = {
                value: entitiesString.substring(0, entitiesString.length - 2),
                fuzzy: false,
                not: false
            };

            setFieldsFromProperties(props);
        }

        //--------------------------------------------------------------------------------------------------------------

        /**
         * Populate fields from a search string
         */
        function setFieldsFromString(searchString) {

            var props = parseQuery(searchString);

            if (props.like) {
                setFieldsFromDataIds(props.like.value.split(','));
                $('#advancedTabs').tabs('select', 1);
            } else {
                setFieldsFromProperties(props);
                $('#advancedTabs').tabs('select', 0);
            }
        }

        //--------------------------------------------------------------------------------------------------------------

        /**
         * Populate fields from a list of properties
         */
        function setFieldsFromProperties(properties) {

            if (properties.datatype) {
                $('#entities').val(properties.datatype.value);
                if (_buildForType) {
                    _buildForType.call($('#entities').get(0));
                }
            }

            if (properties.id) {
                $('#likeIdProperty').val(properties.id.value);
            } else {
                $('#likeIdProperty').val('');
            }

            for (var x = 0; x < _UIObjectState.properties.length; x++) {
                var key = _UIObjectState.properties[x]['key'];
                var value = properties[key] || _blank;

                $('#textProperty'+x).val(value.value);
                $('#not'+x).attr('checked', value.not);
                $('#fuzzy'+x).attr('checked', value.fuzzy);
            }
        }

        //--------------------------------------------------------------------------------------------------------------

        /**
         * Contact the server for properties to populate fields with
         */
        function setFieldsFromDataIds(dataIds) {
            aperture.io.rest(
                '/entities',
                'POST',
                function (response) {
                    seedFromEntities(response.data);
                },
                {
                    postData : {
                        sessionId : xfWorkspace.getSessionId(),
                        queryId: (new Date()).getTime(),
                        entities : dataIds,
                        contextid : '',
                        isFlattened : true,
                        details : true
                    },
                    contentType: 'application/json'
                }
            );
        }

        //--------------------------------------------------------------------------------------------------------------

        var _constructDialog = function(state){
        	$('#advancedTabs').tabs({ 
        		heightStyle: 'auto'
        	});
            $('#advancedDialog').dialog({
            	height: 450,
                autoOpen: false,
                modal: true,
                buttons: {
                    'Apply': function () {
                        var searchString = "";
                        var isPattern = $('#advancedTabs').tabs( 'option', 'selected' );

                        if (isPattern) {
                        	var id = $('#likeIdProperty').val();
                        	if (id) {
                            	searchString = "like:"+ id;
                        	}
                        	
                        } else {
	                        for (var x = 0; x < state.properties.length; x++) {
	                            if ($('#textProperty'+x).val() != "") {
	
	                            	var key = state.properties[x]['key'];
	                                var val = $('#textProperty'+x).val();
	                                if ($('#not'+x).is(':checked')) {
	                                    key = '-' + key;
	                                } 
	                                if (!$('#fuzzy'+x).is(':checked')) {
	                                    val = '"' + val + '"';
	                                }
	
	                                searchString += key + ':' + val + ' ';
	                            }
	                        }
	
	                        searchString += 'datatype:'+ $('#entities').val();
                        }
                        
                        var searchData = {
                            xfId : _UIObjectState.fileId,
                            searchTerm : searchString,
                            noRender : true
                        };

                        aperture.pubsub.publish(chan.SEARCH_REQUEST, searchData);

                        $(this).dialog('close');
                    },
                    'Cancel': function() {
                        $(this).dialog('close');
                    }
                },
                show: {
                    effect: 'clip',
                    duration: 100
                },
                hide: {
                    effect: 'clip',
                    duration: 100
                },
                width:500
            });
            
            $('#advancedDialog').css('display', '');
        };

        //--------------------------------------------------------------------------------------------------------------
        // Public
        //--------------------------------------------------------------------------------------------------------------

        var xfAdvancedSearchModule = {};

        //--------------------------------------------------------------------------------------------------------------

        xfAdvancedSearchModule.registerChildModule = function(childModule) {
            _UIObjectState.childModule = childModule;
        };

        //--------------------------------------------------------------------------------------------------------------

        // Register the module with the system
        modules.register('xfAdvancedSearch', function() {
        	return {
	        	start : function() {
	                var subTokens = {};
	                subTokens[chan.ADVANCE_SEARCH_DIALOG_REQUEST] = aperture.pubsub.subscribe(chan.ADVANCE_SEARCH_DIALOG_REQUEST, _onAdvancedSearchDialogRequest);
	                subTokens[chan.ALL_MODULES_STARTED] = aperture.pubsub.subscribe(chan.ALL_MODULES_STARTED, _modalBoxCreator);
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