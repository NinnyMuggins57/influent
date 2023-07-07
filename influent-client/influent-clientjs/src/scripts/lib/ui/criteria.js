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
define( [ 'lib/util/xfUtil' ],

    function( xfUtil ) {
	
		var CUSTOM_JQUERY_EVENT = 'component.change';
		var _supportsWeight = true;

		var _advancedSearchFuzzyLevels =
			aperture.config.get()['influent.config']['advancedSearchFuzzyLevels'];

		/**
		 * Returns true if fuzzy option is allowed by the range type and constraint
		 */
		function _fuzzyAllowed(descriptor) {
			// SINGLETON, LIST, BOUNDED, DISTRIBUTION 
			switch (descriptor.range) {
				case 'SINGLETON':
				case 'LIST':
					break;
				default:
					return false;
			}
			
			// REQUIRED_EQUALS, FUZZY_PARTIAL_OPTIONAL, NOT, OPTIONAL_EQUALS, FUZZY_REQUIRED
			switch (descriptor.constraint) {
			case 'FUZZY_PARTIAL_OPTIONAL':
			case 'FUZZY_REQUIRED':
				return true;
			}
		}
		
		/**
		 * Default validation fn
		 */
		function _unchecked(value) {
			return true;
		}

		/**
		 * Validation fns for each type
		 */
		var _typeValidators = {
			'BOOLEAN' : _unchecked,
			'DOUBLE' : _unchecked,
			'LONG' : _unchecked,
			'STRING' : _unchecked,
			'DATE' : _unchecked,
			'GEO' : _unchecked,
			'OTHER' : _unchecked
		};

		/**
		 * add the ui for the value editor and return the setter/getter fn
		 */
		function _constraintUI(parent) {
			var opt = $('<select class="advancedsearch-constraint"></select>')
				.appendTo(parent);

			return {
				add : function(value, label) {
					$('<option></option>')
						.attr('value', value)
						.html(label || value)
						.appendTo(opt);
					return this;
				},
				value : function(value) {
					if (arguments.length !== 0) {
						opt.val(value);
						return this;
					}  
					return opt.val();
				},
				change : function(callback) {
					opt.change(function() {
                        aperture.log.log({
                            type: aperture.log.draperType.USER,
                            workflow: aperture.log.draperWorkflow.WF_GETDATA,
                            activity: 'select_filter_menu_option',
                            description: 'Criteria constraint changed'
                        });
                        callback();
                    });
					return this;
				},
				clear : function() {
					opt.empty();
					return this;
				}
			};
		}
		
		/**
		 * add the ui for the value editor and return the value api
		 * TO DO: support customizations via plugin api
		 */
		function _valueUI(descriptor, parent, operator, boost) {
			operator.add('is');
			
			if (_fuzzyAllowed(descriptor)) {

				if (_advancedSearchFuzzyLevels == null) {
					operator.add('fuzzy', 'is ~like');
				} else {
					aperture.util.forEach(_advancedSearchFuzzyLevels, function(value, key) {
						operator.add('fuzzy_' + value, key);
					});
				}
			}
			
			operator.add('not', 'is NOT');

			function committed(changed) {
				changed.call(this, true);
			}
			function keypress(changed, e) {
                aperture.log.log({
                    type: aperture.log.draperType.USER,
                    workflow: aperture.log.draperWorkflow.WF_GETDATA,
                    activity: 'enter_filter_text',
                    description: 'Entering text into search criteria'
                });

				if (e.which) {
					changed.call(this, false);
				}
			}
			
			var val = $('<input class="advancedsearch-input"/>').appendTo(parent);
			
			// set from text or get as text
			return {
				validate : _typeValidators[descriptor.type],
				remove : function() {
					val.remove();
				},
				change : function(callback) {
					val.keypress(keypress.bind(this, callback))
						.change(committed.bind(this, callback));
				},
				value : function(text) {
					var weight = 0;
					var fuzzySimilarity = 0;
					var key;
					
					if (arguments.length !== 0) {
						var match = /([^:\s]+):(.+?)\s*$/.exec(text||'');
						var value = '';
						var not= false, fuzzy= false;

						if (match != null) {
							key = match[1];
		
							if ((not = (key.charAt(0) === '-'))) {
								key = key.substr(1);
							}
							
							if (key === descriptor.key) {
								value = match[2];

								var bmatch = /\^([\.0-9]+)$/.exec(value);
								if (bmatch != null) {
									weight = Number(bmatch[1]);
									
									if (!isNaN(weight) && weight > 0) {
										boost.value(weight);
										
										value = value.substring(0, value.length - bmatch[1].length - 1);
									}
								}

								if (value.charAt(0) === '"') {
									value = value.substring(1, value.length-1);
								} else {
									if (!_advancedSearchFuzzyLevels)
										fuzzy = true;
								}

								if (_advancedSearchFuzzyLevels != null) {

									var fmatch = /\~([\.0-9]+)$/.exec(value);
									if (fmatch != null) {
										fuzzySimilarity = Number(fmatch[1]);

										if (!isNaN(fuzzySimilarity) && fuzzySimilarity > 0) {
											value = value.substring(0, value.length - fmatch[1].length - 1);
										}
									}
								}
							} else {
								aperture.log.error('key does not match descriptor in advanced search term');
							}
						}
						
						if (not) {
							operator.value('not');
						} else if (fuzzySimilarity > 0) {
							operator.value('fuzzy_' + fuzzySimilarity);
						} else if (fuzzy) {
							operator.value('fuzzy');
						} else {
							operator.value('is');
						}

						val.val(value);
	
					} else {
						var inputValue = val.val();

						text = '';
						if (inputValue !== '') {
							key = descriptor.key;
							if (operator.value() === 'not') {
								text += '-' + key + ':' + inputValue;
							} else if (operator.value() === 'fuzzy') {
								text += key + ':' + inputValue;
							} else if (operator.value().indexOf('fuzzy_') !== -1) {
								var fuzz = Number(operator.value().substring('fuzzy_'.length, operator.value().length));
								text += key + ':' + inputValue  + '~' + fuzz;
							} else {
								text += key + ':"' + inputValue + '"';
							}

							weight = boost.value();

							if (weight !== 1) {
								text += '^' + weight;
							}
						}
						
						return text;
					}
				}
			};
		}
		
		function _boostUI(parent) {
			var _boost= 0;
			var api;
			
			// don't show the ui in this case.
			if (!_supportsWeight) {
				api = {
					value : function(boost) {
						if (arguments.length !== 0) {
							_boost = boost;
						} else {
							return _boost;
						}
					},
					change: function(callback) {
						return this;
					}
				};
				
			} else {
				var set = $('<div class="advancedsearch-boost"></div>')
					.appendTo(parent);

                xfUtil.makeTooltip(set, 'boost the weight of this criteria');

                api =  {
					value : function(boost) {
						var kids = set.children();
						if (arguments.length !== 0) {
							if (_boost !== boost) {
								_boost = boost;
								var i = Math.ceil(boost);
								var selected = kids.filter(function() {
									var x = Number($(this).attr('id'));
									return x <= i;
								});
								
								selected.addClass('advancedsearch-boost-selected');
								kids.not(selected).removeClass('advancedsearch-boost-selected');
							}
						} else {
							return _boost;
						}
					},
					
					change : function(callback) {
						set.bind(CUSTOM_JQUERY_EVENT, callback.bind(this));
						return this;
					}
				};
	
				var highlight = function(elem) {
					var i = Number($(elem).attr('id'));
					var kids = set.children();
					var selected = kids.filter(function() {
						var x = Number($(this).attr('id'));
						return x <= i;
					});
					
					selected.addClass('advancedsearch-boost-hover');
					kids.not(selected).removeClass('advancedsearch-boost-hover');
				};
	
				var down = false;
				
				[1,2,3,4,5].forEach(function(i) {
					$('<span class="advancedsearch-boost-one"></span>')
						.attr('id', String(i))
						.appendTo(set)
						.hover(function() {
							highlight(this);
						})
						.mousedown(function(e) {
							down = true;
							e.preventDefault();
							e.stopImmediatePropagation();
						})
						.mouseup(function() {
							if (down) {
								down = false;
                                var level = Number($(this).attr('id'));
								api.value(level);

                                aperture.log.log({
                                    type: aperture.log.draperType.USER,
                                    workflow: aperture.log.draperWorkflow.WF_GETDATA,
                                    activity: 'select_filter_menu_option',
                                    description: 'Boosting criteria',
                                    data: {
                                        level : level
                                    }
                                });
							}
						});
				});
				
			}
			
			api.value(1);
			return api;
		}
		
		/**
		 * Enables the field choices specified.
		 */
		function _enableMultiVal(allow) {
//			if (allow) {
//				$('.advancedsearch-param').css('color','');
//			} else {
				$('.advancedsearch-paramchoice').each(function(i, elem) {
					var key = $(elem).val();
					
					$('.advancedsearch-param[value="' +key+ '"]')
						.not(':selected').css('color','#aaa');
				});
//			}
		}	
		
		/**
		 * Creates a field selector
		 */
		function _fieldSelector(descriptors, descriptor) {
			var paramchoice = 
				$('<select class="advancedsearch-paramchoice"></select>');
			
			// build type options.
			descriptors.forEach(function(searchable) {
				$('<option class="advancedsearch-param"></option>')
					.attr('value', searchable.key)
					.html(searchable.friendlyText ? searchable.friendlyText : searchable.key)
					.appendTo(paramchoice);
			});

			// select
			paramchoice.val(descriptor.key);
			
			return paramchoice;
		}
		
		/**
		 * Enables the field choices specified.
		 */
		function _removeButton() {

            var removeButton = $('<div class="advancedsearch-remove-button"></div>');
            xfUtil.makeTooltip(removeButton, 'remove this criteria');

            return removeButton;
		}

		var allowMultival = false;
		
		/**
		 * Return the module for access by clients, which exposes a fn for
		 * adding new criteria ui to a parent and returning an interface to control it.
		 */
		return {

			/**
			 * @returns the criteria list.
			 */
			list : function() {
				var ui;
				ui= [];
				
				$('.advancedsearch-criteria').each(function(i, elem) {
					ui.push($(elem).data('api'));
				});

				return ui;
			},
			
			/**
			 * Sets or gets the option which specifies whether multiple values
			 * are allowed for the same field.
			 */
			multival : function(allow) {
				if (arguments.length !== 0) {
					if (allowMultival !== allow) {
						allowMultival = allow;
						
						_enableMultiVal(allow);
					}
					
					return this;
				}
				
				return allowMultival;
			},
			
			/**
			 * Sets or gets the option which specifies whether weight editing is allowed.
			 */
			weighted : function(allow) {
				_supportsWeight = allow;
				
				if (arguments.length !== 0) {
					_supportsWeight = allow;
					
					return this;
				}
				
				return _supportsWeight;
			},
			
			/**
			 * Creates a new criteria ui and appends it to parent.
			 * 
			 * @param descriptors
			 *  the list of descriptors provided by the custom search implementation.
			 * 
			 * @param descriptor 
			 *  the selected descriptor
			 * 
			 * @param parent
			 *  the parenting dom element as a selector, dom element or jquery selection
			 * 
			 * @returns Object
			 *  an interface to the new criteria object.
			 */
			add : function(descriptors, descriptor, parent) {
				var _ui = $('<div></div>')
					.addClass('advancedsearch-criteria')
					.appendTo(parent)
					.append(_removeButton().click(function() {
						api.remove();

                        aperture.log.log({
                            type: aperture.log.draperType.USER,
                            workflow: aperture.log.draperWorkflow.WF_GETDATA,
                            activity: 'select_filter_menu_option',
                            description: 'Criteria removed'
                        });
					}));
				
				// set up wrapper callbacks
				function changed(committed) {
					_ui.trigger(CUSTOM_JQUERY_EVENT, [committed]);
                }
				function onChange(callback, e, committed) {
					callback.call(this, committed);
				}
				
				var boost = _boostUI(_ui).change(changed.bind(this, true));
				
				var _left = $('<div></div>')
					.addClass('advancedsearch-criteria-left')
					.appendTo(_ui);
				var _right = $('<div></div>')
					.addClass('advancedsearch-criteria-right')
					.css('margin-right', _supportsWeight? 75:25)
					.appendTo(_ui);
				
					
				var _privates = {key : descriptor.key};
				var api= null;
				
				
				// create the field selector.
				_fieldSelector(descriptors, descriptor)
					.appendTo(_left)
					.change(function() {
						var descriptor = null;
						for (var i = 0; i < descriptors.length && descriptor === null; i++) {
							if (descriptors[i].key === $(this).val()) {
								descriptor = descriptors[i];
							}
						}
						_privates.key = descriptor.key;
						_privates.operator.clear();
						_privates.editor.remove();
						_privates.editor = _valueUI(descriptor, _right, _privates.operator, boost);
//						_privates.editor.value(val);
						_privates.editor.change(changed);

                        aperture.log.log({
                            type: aperture.log.draperType.USER,
                            workflow: aperture.log.draperWorkflow.WF_GETDATA,
                            activity: 'select_filter_menu_option',
                            description: 'Criteria field changed',
                            data: {
                                field: descriptor.friendlyText ? descriptor.friendlyText : descriptor.key
                            }
                        });
					});
				
				// create the editor
				_privates.operator = _constraintUI(_left).change(changed.bind(this, true));
				_privates.editor = _valueUI(descriptor, _right, _privates.operator, boost);
				_privates.editor.change(changed);
				
				// strip this out from the options in others.
				if (!allowMultival) {
					_enableMultiVal(false);
				}
				
				api = {
					
					/**
					 * @returns the key of the descriptor being edited.
					 */
					key : function() {
						return _privates.key;
					},
				
					/**
					 * Gets or sets the value
					 * 
					 * @param [text] the value
					 * @returns the value.
					 */
					value : function(text) {
						if (arguments.length !== 0) {
							_privates.editor.value(text);
							return this;
							
						} else {
							return _privates.editor.value();
						}
					},

					/**
					 * Validates and returns true if valid, else false.
					 * An implementation may choose to modify the content in this
					 * call to make it valid (for instance, reformatting a date).
					 * 
					 * @returns
					 */
					validate : function() {
						return _privates.editor.validate();
					},

					/**
					 * Sets a callback to receive change notifications.
					 * This method will take a single callback only.
					 */
					change : function(callback) {
						if (callback) {
							_ui.bind(CUSTOM_JQUERY_EVENT, onChange.bind(this, callback));
						} else {
							_ui.trigger(CUSTOM_JQUERY_EVENT);
						}
						return this;
					},
					
					/**
					 * Removes the ui from its parent.
					 */
					remove : function() {
						_ui.remove();
					}
				};
				
				_ui.data('api', api);

				return api;
			}
		};
	}
);
