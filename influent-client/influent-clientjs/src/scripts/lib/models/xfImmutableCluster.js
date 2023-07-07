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
        'jquery', 'lib/interfaces/xfUIObject', 'lib/channels', 'lib/util/GUID', 'lib/util/xfUtil',
        'lib/models/xfCard', 'lib/models/xfSummaryCluster',
        'lib/models/xfClusterBase', 'lib/constants',
        'lib/extern/underscore'
    ],
    function(
        $, xfUIObject, chan, guid, xfUtil,
        xfCard, xfSummaryCluster,
        xfClusterBase, constants
    ) {

        //--------------------------------------------------------------------------------------------------------------
        // Private Variables
        //--------------------------------------------------------------------------------------------------------------

        var MODULE_NAME = 'xfImmutableCluster';

        //--------------------------------------------------------------------------------------------------------------
        // Public
        //--------------------------------------------------------------------------------------------------------------

        var xfImmutableCluster = {};

        //--------------------------------------------------------------------------------------------------------------

        xfImmutableCluster.createInstance = function(spec){

            //------------------
            // private variables
            //------------------

            var guidId = 'immutable_' + guid.generateGuid();
            
            var _UIObjectState = {
                xfId                : guidId,
                UIType              : MODULE_NAME,
                spec                : _.clone(spec),
                toolbarSpec         : xfClusterBase.getToolbarSpecTemplate(),
                children            : [],
                isExpanded          : false,
                isSelected          : false,
                isHighlighted       : false,
                isHovered			: false,
                showToolbar         : false,
                showDetails         : false,
                showSpinner         : false,
                links               : {}
            };

            //----------------
            // private methods
            //----------------

            var _createChildrenFromSpec = function(showSpinner, showToolbar) {

                for (var i = 0; i < _UIObjectState.children.length; i++) {
                    _UIObjectState.children[i].dispose();
                    _UIObjectState.children[i] = null;
                }
                _UIObjectState.children.length = 0;

                for (var i = 0; i < _UIObjectState.spec.members.length; i++) {

                    var childMemberSpec = _UIObjectState.spec.members[i];

                    var uiObject = {};
                    if (childMemberSpec.type == constants.MODULE_NAMES.SUMMARY_CLUSTER) {
                        uiObject = xfSummaryCluster.createInstance(childMemberSpec);
                    } else if (xfUtil.isClusterTypeFromSpec(childMemberSpec)) {
                        uiObject =  xfImmutableCluster.createInstance(childMemberSpec);
                    } else if (childMemberSpec.type == constants.MODULE_NAMES.ENTITY) {
                        uiObject = xfCard.createInstance(childMemberSpec);
                    }
                    uiObject.showDetails(_UIObjectState.showDetails);
                    uiObject.showSpinner(showSpinner);

                    // we set the children's toolbar state base on our toolbar state. However,
                    // we need to specifically set our children to not allow close
                    uiObject.updateToolbar(_UIObjectState.toolbarSpec);
                    uiObject.updateToolbar({'allowClose': false});

                    // #6947 also specifically set children to be searchable
                    uiObject.updateToolbar({'allowSearch': true});

                    uiObject.showToolbar(showToolbar);

                    // Add the child to the cluster.
                    uiObject.setParent(xfClusterInstance);
                    _UIObjectState.children.push(uiObject);
                }
            };

            //---------------
            // public methods
            //---------------

            // create new object instance
            var xfClusterInstance = xfClusterBase.createInstance(_UIObjectState);

            // create child placeholder cards from spec
            _createChildrenFromSpec(true, false);

            //----------
            // Overrides
            //----------

            xfClusterInstance.clone = function() {

                // create cloned object
                var clonedObject = xfImmutableCluster.createInstance(_UIObjectState.spec);

                // add necessary UI state
                clonedObject.showDetails(_UIObjectState.showDetails);
                (_UIObjectState.isExpanded) ? clonedObject.expand() : clonedObject.collapse();

                // make the cloned object an orphan
                clonedObject.setParent(null);

                // return cloned object
                return clonedObject;
            };

            //----------------------------------------------------------------------------------------------------------

            xfClusterInstance.removeChild = function(xfId, disposeObject, preserveLinks) {
                console.error('Unable to remove child from ' + MODULE_NAME);
            };

            //----------------------------------------------------------------------------------------------------------

            xfClusterInstance.removeAllChildren = function() {
                console.error('Unable to remove children from ' + MODULE_NAME);
            };

            //----------------------------------------------------------------------------------------------------------

            xfClusterInstance.insert = function(xfUIObj, beforeXfUIObj00) {
                console.error('Unable to insert children into ' + MODULE_NAME);
            };

            //----------------------------------------------------------------------------------------------------------

            xfClusterInstance.update = function(spec) {

                for (var key in spec) {
                    if (spec.hasOwnProperty(key)) {
                        _UIObjectState.spec[key] = spec[key];
                    }
                }

                _createChildrenFromSpec(false, true);
            };

            //----------------------------------------------------------------------------------------------------------

            xfClusterInstance.restoreVisualState = function(state) {

                this.cleanState();

                _UIObjectState.xfId = state.xfId;
                _UIObjectState.UIType = state.UIType;

                _UIObjectState.isExpanded = state.isExpanded;
                _UIObjectState.isSelected = state.isSelected;
                _UIObjectState.isHighlighted = state.isHighlighted;
                _UIObjectState.showToolbar = state.showToolbar;
                _UIObjectState.showDetails = state.showDetails;
                _UIObjectState.toolbarSpec = state.toolbarSpec;

                _UIObjectState.spec.dataId = state.spec.dataId;
                _UIObjectState.spec.type = state.spec.type;
                _UIObjectState.spec.subtype = state.spec.subtype;
                _UIObjectState.spec.count = state.spec.count;
                _UIObjectState.spec.icons = state.spec.icons;
                _UIObjectState.spec.detailsTextNodes = state.spec.detailsTextNodes;
                _UIObjectState.spec.graphUrl = state.spec.graphUrl;
                _UIObjectState.spec.duplicateCount = state.spec.duplicateCount;
                _UIObjectState.spec.label = state.spec.label;
                _UIObjectState.spec.confidenceInSrc = state.spec.confidenceInSrc;
                _UIObjectState.spec.confidenceInAge = state.spec.confidenceInAge;
                _UIObjectState.spec.flow = state.spec.flow;
                _UIObjectState.spec.members = state.spec.members;
                _UIObjectState.spec.inDegree = state.spec.inDegree;
                _UIObjectState.spec.outDegree = state.spec.outDegree;

                _UIObjectState.children = [];
                var childCount = state.children.length;
                for (var i = 0; i < childCount; i++) {
                    if (state.children[i].UIType == constants.MODULE_NAMES.ENTITY) {
                        var cardSpec = xfCard.getSpecTemplate();
                        var cardUIObj = xfCard.createInstance(cardSpec);
                        cardUIObj.cleanState();
                        cardUIObj.restoreVisualState(state.children[i]);
                        this._restoreObjectToCluster(cardUIObj);
                    } else if (state.children[i].UIType == constants.MODULE_NAMES.IMMUTABLE_CLUSTER) {
                        var clusterSpec = xfImmutableCluster.getSpecTemplate();
                        var clusterUIObj = xfImmutableCluster.createInstance(clusterSpec);
                        clusterUIObj.cleanState();
                        clusterUIObj.restoreVisualState(state.children[i]);
                        this._restoreObjectToCluster(clusterUIObj);
                    } else {
                        console.error("cluster children should only be of type " + constants.MODULE_NAMES.ENTITY + " or " + constants.MODULE_NAMES.IMMUTABLE_CLUSTER + ".");
                    }
                }
            };

            //----------------------------------------------------------------------------------------------------------

            xfClusterInstance._restoreObjectToCluster = function(object) {

                var memberSpec = _.clone(object.getVisualInfo().spec);
                memberSpec.parent = this;

                _UIObjectState.children.push(object);
                _UIObjectState.spec.members.push(memberSpec);

                object.setParent(this);
            };

            //----------------------------------------------------------------------------------------------------------

            return xfClusterInstance;
        };

        //--------------------------------------------------------------------------------------------------------------

        xfImmutableCluster.getSpecTemplate = function() {
            var spec = xfClusterBase.getSpecTemplate();
            spec.type = MODULE_NAME;
            return spec;
        };

        //--------------------------------------------------------------------------------------------------------------

        xfImmutableCluster.getModuleName = function() {
            return MODULE_NAME;
        };

        //--------------------------------------------------------------------------------------------------------------

        return xfImmutableCluster;
    }
);