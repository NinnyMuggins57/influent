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
		'lib/channels', 'lib/render/cardRenderer', 'lib/render/columnRenderer', 'lib/util/xfUtil'
	],
	function(
		chan, cardRenderer, columnRenderer, xfUtil
	) {

		/**
		 * the workspace renderer is the top level renderer, managing the dom element for the workspace model.
		 */
		var workspaceRenderer = {};

		//------------------------------------------------------------------------------------------------------------------
		var _renderDefaults = {
			WORKSPACE_MARGIN : 10,
			getStartingColumnPosX : function(numColumns){
				var centerBase =  $('#workspace').width()/2-cardRenderer.getRenderDefaults().CARD_WIDTH/2;      // Subtract half of card size,

				if(numColumns > 1) {
					centerBase = Math.max(this.WORKSPACE_MARGIN, centerBase - (numColumns - 1)*_columnDefaults.COLUMN_DISTANCE/2);
				}

				return centerBase;
			}
		};

		//------------------------------------------------------------------------------------------------------------------

		var _columnDefaults = columnRenderer.getRenderDefaults();

		//------------------------------------------------------------------------------------------------------------------
		
		function left(element) {
			if (element != null) {
				var l = element.css('left');
				return l? Number(l.replace('px','')) : 0;
			}
		}
		
		//------------------------------------------------------------------------------------------------------------------

		function positionColumns(columns, parentCanvas) {
			
			var x = 0;
			var maxHeight = 0;
			var parentId = '#'+ parentCanvas.attr('id');
			
			var colsn = $();
			var shift = null;
			
			aperture.util.forEach(columns, function(column) {
				var visualInfo = column.getVisualInfo();
				
				// create the dom element
				var element = columnRenderer.createElement(visualInfo);

				// keep track of these for return
				colsn = colsn.add(element);

				// need one reference point in old and new space if we can find one.
				if (shift == null && !column.isEmpty() && element.parent(parentId).length !== 0) {
					shift = x - left(element);
				}
				
				// append/re-append (keeps columns in the right order).
				parentCanvas.append(element);

				
				// track the maximum column height
				maxHeight = Math.max(maxHeight, columnRenderer.getHeight(element));

				// position the column.
				element.css('left', x);
				
				
				x+= _columnDefaults.COLUMN_DISTANCE;
			});
			
			// return data for the result
			return {
				width: x,
				height: maxHeight,
				shift:  shift,
				columns: colsn
			};
		}
		
		//------------------------------------------------------------------------------------------------------------------
		var processColumns = function(columns, parentCanvas) {

			// PROCESS COLUMNS
			// cache list of old children
			var colso = parentCanvas.children();

			// position the active children.
			var cdata = positionColumns(columns, parentCanvas);
			
			// remove any removed children.
			var removed = colso.not(cdata.columns).remove();


			// SIZE CONTAINER
			// resize the container
			var container = $('#workspace-content');
			
			container.css({
				'height': cdata.height + _renderDefaults.WORKSPACE_MARGIN,
				'width': cdata.width
			});
			
			// POSITION CONTAINER: NOTHING TO DO?
			// look for something added
			var firstAdd = cdata.columns.not(colso).first();
			
			// if nothing changed, done.
			if (firstAdd.length === 0 && removed.length === 0) {
				return;
			}

			
			// POSITION CONTAINER: JUST SET IT?
			// old and new insets
			var inset = {
				xo: left(container), 
				xn: _renderDefaults.getStartingColumnPosX(columns.length)
			};
			
			// if nothing old just set it, without animated transition, and we're done.
			if (colso.length === 0) {
				container.css('left', inset.xn);
				return;
			}
			
			// POSITION CONTAINER: ANIMATE IT
			var scrolled = $('#workspace');
			var scroll = {
				xo: scrolled.scrollLeft(), 
				xn: 0
			};

			// manipulating scroll?
			if (inset.xn === _renderDefaults.WORKSPACE_MARGIN && firstAdd.length !== 0) {
				var seex= left(firstAdd) + _renderDefaults.WORKSPACE_MARGIN;
				
				if (seex < scroll.xo) {
					scroll.xn = seex;
				} else {
					seex += _columnDefaults.COLUMN_DISTANCE;
					
					// if already scrolled enough, don't scroll
					scroll.xn = Math.max(scroll.xo, Math.max(0,seex - scrolled.width()));
				}
				
				scrolled.animate({scrollLeft: scroll.xn});
			}
			
			// anything but a wholesale change, try to transition from the same point
			if (cdata.shift != null) {
				
				// shift the old reference point into new space to start the anim
				container.css({left: inset.xo-cdata.shift});
			}

			// animate
			container.animate({left: inset.xn});
		};

		//------------------------------------------------------------------------------------------------------------------

		$('#workspace').click(function(event) {
			// deselect?
			if (xfUtil.isWorkspaceWhitespace(event.target)) {
				aperture.pubsub.publish(
					chan.SELECTION_CHANGE_REQUEST,
					{
						xfId: null,
						selected : true,
						noRender: false
					}
				);
			}
		});

		workspaceRenderer.createElement = function(visualInfo, capturing) {

			// the column container.
			var canvas = $('#' + visualInfo.xfId);
			if (canvas.length === 0){
				canvas = $('<div></div>');
				canvas.attr('id', visualInfo.xfId);
				
				var cardsDiv = $('#cards');
				cardsDiv.empty();
				cardsDiv.append(canvas);

				if (capturing) {
					var workspaceDiv = $('#workspace');
					workspaceDiv.css('overflow', 'visible');
				}
			}

			processColumns(visualInfo.children, canvas);

			return canvas;
		};

		//------------------------------------------------------------------------------------------------------------------
		
		workspaceRenderer.getSize = function() {
			var workspaceDiv = $('#workspace');
			var width = workspaceDiv[0].scrollWidth;
			var height = workspaceDiv[0].scrollHeight;

			return {
				width : width,
				height : height
			};
		};
		
		//------------------------------------------------------------------------------------------------------------------

		workspaceRenderer.getRenderDefaults = function(){
			return _.clone(_renderDefaults);
		};

		return workspaceRenderer;
	}
);
