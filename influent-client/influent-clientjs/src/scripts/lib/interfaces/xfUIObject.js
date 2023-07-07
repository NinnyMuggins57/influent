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

define(['jquery', 'lib/module', 'lib/channels'], function($, modules, chan) {
    var module = {};

    var unimpMethod = 'Unimplemented Method';

    module.implementedBy = function(implementingObject, implementingObjectModuleName) {

        implementingObject.clone = function() {
            console.error(unimpMethod + ': "clone" by module ' + implementingObjectModuleName);
        };

        implementingObject.getXfId = function() {
            console.error(unimpMethod + ': "getXfId" by module ' + implementingObjectModuleName);
        };

        implementingObject.getDataId = function() {
            console.error(unimpMethod + ': "getDataId" by module ' + implementingObjectModuleName);
        };

        implementingObject.getUIType = function() {
            console.error(unimpMethod + ': "getUIType" by module ' + implementingObjectModuleName);
        };

        implementingObject.getLabel = function() {
            console.error(unimpMethod + ': "getLabel" by module ' + implementingObjectModuleName);
        };

        implementingObject.getUIObjectByXfId = function(xfId) {
            console.error(unimpMethod + ': "getUIObjectByXfId" by module ' + implementingObjectModuleName);
        };

        implementingObject.getUIObjectsByDataId = function(dataId) {
            console.error(unimpMethod + ': "getUIObjectByDataId" by module ' + implementingObjectModuleName);
        };

        implementingObject.getParent = function() {
            console.error(unimpMethod + ': "getParent" by module ' + implementingObjectModuleName);
        };

        implementingObject.setParent = function(xfUIObj) {
            console.error(unimpMethod + ': "setParent" by module ' + implementingObjectModuleName);
        };

        implementingObject.getLinks = function() {
            console.error(unimpMethod + ': "getLinks" by module ' + implementingObjectModuleName);
        };

        implementingObject.getIncomingLinks = function() {
            console.error(unimpMethod + ': "getIncomingLinks" by module ' + implementingObjectModuleName);
        };

        implementingObject.getOutgoingLinks = function() {
            console.error(unimpMethod + ': "getOutgoingLinks" by module ' + implementingObjectModuleName);
        };

        implementingObject.addLink = function(link) {
            console.error(unimpMethod + ': "addLink" by module ' + implementingObjectModuleName);
            return false;
        };

        implementingObject.removeLink = function(xfId) {
            console.error(unimpMethod + ': "removeLink" by module ' + implementingObjectModuleName);
            return false;
        };

        implementingObject.removeAllLinks = function() {
            console.error(unimpMethod + ': "removeAllLinks" by module ' + implementingObjectModuleName);
        };

        implementingObject.collapseLinks = function(direction, deleteAfterCollapse) {
            console.error(unimpMethod + ': "collapseLinks" by module ' + implementingObjectModuleName);
        };

        implementingObject.isLinkedTo = function(uiObject) {
            console.error(unimpMethod + ': "isLinkedTo" by module ' + implementingObjectModuleName);
        };

        implementingObject.remove = function(eventChannel, dispose) {
            console.error(unimpMethod + ': "remove" by module ' + implementingObjectModuleName);
        };

        implementingObject.removeChild = function(xfId, disposeObject, preserveLinks) {
            console.error(unimpMethod + ': "removeChild" by module ' + implementingObjectModuleName);
        };

        implementingObject.removeAllChildren = function() {
            console.error(unimpMethod + ': "removeAllChildren" by module ' + implementingObjectModuleName);
        };

        implementingObject.insert = function(xfUIObj, beforeXfUIObj00) {
            console.error(unimpMethod + ': "insert" by module ' + implementingObjectModuleName);
        };

        implementingObject.update = function(spec) {
            console.error(unimpMethod + ': "update" by module ' + implementingObjectModuleName);
        };

        implementingObject.showDetails = function(bShow) {
            console.error(unimpMethod + ': "showDetails" by module ' + implementingObjectModuleName);
            return false;
        };

        implementingObject.getSpecs = function(bOnlyEmptySpecs) {
            console.error(unimpMethod + ': "getSpecs" by module ' + implementingObjectModuleName);
        };

        implementingObject.getVisualInfo = function() {
            console.error(unimpMethod + ': "getVisualInfo" by module ' + implementingObjectModuleName);
        };

        implementingObject.getFocus = function() {
            console.error(unimpMethod + ': "getFocus" by module ' + implementingObjectModuleName);
        };

        implementingObject.setFocus = function(data) {
            console.error(unimpMethod + ': "setFocus" by module ' + implementingObjectModuleName);
        };

        implementingObject.isSelected = function() {
            console.error(unimpMethod + ': "isSelected" by module ' + implementingObjectModuleName);
        };

        implementingObject.setSelection = function(xfId) {
            console.error(unimpMethod + ': "setSelection" by module ' + implementingObjectModuleName);
        };

        implementingObject.isHovered = function() {
            console.error(unimpMethod + ': "isHovered" by module ' + implementingObjectModuleName);
        };

        implementingObject.setHovering = function(xfId) {
            console.error(unimpMethod + ': "setHovering" by module ' + implementingObjectModuleName);
        };

        implementingObject.expand = function() {
            console.error(unimpMethod + ': "expand" by module ' + implementingObjectModuleName);
        };

        implementingObject.collapse = function() {
            console.error(unimpMethod + ': "collapse" by module ' + implementingObjectModuleName);
        };

        implementingObject.setDuplicateCount = function(count) {
            console.error(unimpMethod + ': "setDuplicateCount" by module ' + implementingObjectModuleName);
        };

        implementingObject.getVisibleDataIds = function() {
            console.error(unimpMethod + ': "getVisibleDataIds" by module ' + implementingObjectModuleName);
        };

        implementingObject.getLabel = function() {
            console.error(unimpMethod + ': "getLabel" by module ' + implementingObjectModuleName);
        };

        implementingObject.allowHover = function(bAllowHover) {
            console.error(unimpMethod + ': "allowHover" by module ' + implementingObjectModuleName);
        };

        implementingObject.cleanState = function() {
            console.error(unimpMethod + ': "cleanState" by module ' + implementingObjectModuleName);
        };

        implementingObject.exportState = function() {
            console.error(unimpMethod + ': "exportState" by module ' + implementingObjectModuleName);
        };
        
        implementingObject.saveState = function() {
            console.error(unimpMethod + ': "saveState" by module ' + implementingObjectModuleName);
        };

        implementingObject.restoreVisualState = function(state) {
            console.error(unimpMethod + ': "restoreState" by module ' + implementingObjectModuleName);
        };

        implementingObject.restoreHierarchy = function(state, workspace) {
            console.error(unimpMethod + ': "restoreHierarchy" by module ' + implementingObjectModuleName);
        };

        implementingObject.dispose = function() {
            console.error(unimpMethod + ': "dispose" by module ' + implementingObjectModuleName);
        };

        implementingObject.sortChildren = function(sortFunction) {
            console.error(unimpMethod + ': "sortChildren" by module ' + implementingObjectModuleName);
        };

        implementingObject.getTotalLinkAmount = function(includeIncoming, includeOutgoing) {
            console.error(unimpMethod + ': "getTotalLinkAmount" by module ' + implementingObjectModuleName);
        };
    };

    return module;
});
