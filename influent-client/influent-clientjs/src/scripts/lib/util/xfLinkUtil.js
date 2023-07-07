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
        'jquery', 'lib/util/xfUtil', 'lib/models/xfLink', 'lib/models/xfFile', 'lib/constants'
    ],
    function(
        $, xfUtil, xfLink, xfFile, constants
    ) {
        //--------------------------------------------------------------------------------------------------------------

        var _aggregateLeftLinks = function(clusterObj, clusterLinks, xfWorkspaceModule, renderAndSaveStateCallback){

            var workspaceState = xfWorkspaceModule.getVisualInfo();
            var sourceIds = [];
            var sourceDataIdMap = {};
            var visualInfo = clusterObj.getVisualInfo();

            var column = xfWorkspaceModule.getColumnByUIObject(clusterObj);
            var prevcolumn = xfWorkspaceModule.getPrevColumn(column);

            var targetIds = [];
            var targetDataIdMap = {};

            var fileClusterMap = {};
            for (var linkId in clusterLinks) {
                if (clusterLinks.hasOwnProperty(linkId)) {
                    var link = clusterLinks[linkId];
                    var uiObject = link.getSource();
                    // Make sure this is an incoming link.
                    if (uiObject.getXfId() != clusterObj.getXfId()){
//                        if (xfUtil.isFileCluster(uiObject)){
                        if (uiObject.getUIType() === constants.MODULE_NAMES.FILE){
                            var fileChildren = uiObject.getChildren();
                            for (var i=0; i < fileChildren.length; i++){
                                var childObj = fileChildren[i];
                                // Have this dataId map to the parent file cluster.
                                targetDataIdMap[childObj.getDataId()] = uiObject;
                            }
                        }
                        else {
                            targetDataIdMap[uiObject.getDataId()] = uiObject;
                        }
                        // Remove this link from the source/target uiObject's map.
                        // The cluster will get updated with the results of the
                        // aggregated link call.
                        link.remove();
                    }
                }
            }
            // Iterate all the unique entries in the map to make a list of target Ids.
            for (var targetId in targetDataIdMap){
                if (targetDataIdMap.hasOwnProperty(targetId)){
                    targetIds.push(targetId);
                }
            }

            // Return if there is nothing to aggregate.
            if (targetIds.length == 0 && fileClusterMap.length == 0){
                return;
            }

            if (targetIds.length > 0){
                if (clusterObj.isExpanded()){
                    for (var i=0; i < visualInfo.children.length; i++){
                        sourceDataIdMap[visualInfo.children[i].getDataId()] = visualInfo.children[i];
                    }
                }
                else {
                    sourceDataIdMap[clusterObj.getDataId()] = clusterObj;
                }

                // Iterate all the unique entries in the map to make a list of source Ids.
                for (var sourceId in sourceDataIdMap){
                    if (sourceDataIdMap.hasOwnProperty(sourceId)){
                        sourceIds.push(sourceId);
                    }
                }

                // Now aggregate all other links.
                aperture.io.rest(
                    '/aggregatedlinks',
                    'POST',
                    function(response){
                        var links = response.data;

                        for (var targetDataId in links) {
                            if (links.hasOwnProperty(targetDataId)) {
                                var targetObj = sourceDataIdMap[targetDataId];
                                if (targetObj){
                                    var incomingLinks = links[targetDataId];
                                    for (var i=0; i < incomingLinks.length; i++){
                                        var sourceDataId = incomingLinks[i].source;
                                        var amount = xfWorkspaceModule.getValueByTag(incomingLinks[i], 'AMOUNT');
                                        var linkCount = xfWorkspaceModule.getValueByTag(incomingLinks[i], 'CONSTRUCTED');
                                        var sourceObj = targetDataIdMap[sourceDataId];
                                        if (sourceObj){
                                            xfLink.createInstance(sourceObj, targetObj, amount, linkCount);
                                        }
                                        else {
                                            console.error('Unable to find target UIObject for dataId: ' + targetDataId);
                                        }
                                    }
                                }
                                else {
                                    console.error('Unable to find source UIObject for dataId: ' + targetDataId);
                                }
                            }
                        }

                        renderAndSaveStateCallback();
                    },
                    {
                        postData : {
                            sessionId : workspaceState.sessionId,
                            queryId: (new Date()).getTime(),
                            sourceIds : sourceIds,
                            targetIds : targetIds,
                            linktype : 'destination',
                            type: 'FINANCIAL',
                            aggregate: 'FLOW',
                            startdate: workspaceState.dates.startDate,
                            enddate: workspaceState.dates.endDate,
                            contextid: column.getXfId(),
                            targetcontextid: prevcolumn.getXfId()
                        },
                        contentType: 'application/json'
                    }
                );
            }
        };

        //--------------------------------------------------------------------------------------------------------------

        var _aggregateRightLinks = function(clusterObj, clusterLinks, xfWorkspaceModule, renderAndSaveStateCallback){

            var workspaceState = xfWorkspaceModule.getVisualInfo();
            var sourceIds = [];
            var sourceDataIdMap = {};
            var visualInfo = clusterObj.getVisualInfo();

            var column = xfWorkspaceModule.getColumnByUIObject(clusterObj);
            var nextcolumn = xfWorkspaceModule.getNextColumn(column);

            var targetIds = [];
            var targetDataIdMap = {};

            var fileClusterMap = {};
            for (var linkId in clusterLinks) {
                if (clusterLinks.hasOwnProperty(linkId)) {
                    var link = clusterLinks[linkId];
                    var uiObject = link.getDestination();
                    // Make sure this is an outgoing link.
                    if (uiObject.getXfId() != clusterObj.getXfId()){
                        if (uiObject.getUIType() === constants.MODULE_NAMES.FILE){
                            var fileChildren = uiObject.getChildren();
                            for (var i=0; i < fileChildren.length; i++){
                                var childObj = fileChildren[i];
                                // Have this dataId map to the parent file cluster.
                                targetDataIdMap[childObj.getDataId()] = uiObject;
                            }
                        }
                        else {
                            targetDataIdMap[uiObject.getDataId()] = uiObject;
                        }
                        // Remove this link from the source/target uiObject's map.
                        // The cluster will get updated with the results of the
                        // aggregated link call.
                        link.remove();
                    }
                }
            }

            // Iterate all the unique entries in the map to make a list of target Ids.
            for (var targetId in targetDataIdMap){
                if (targetDataIdMap.hasOwnProperty(targetId)){
                    targetIds.push(targetId);
                }
            }
            // Return if there is nothing to aggregate.
            if (targetIds.length == 0 && fileClusterMap.length == 0){
                return;
            }

            if (targetIds.length > 0){
                if (clusterObj.isExpanded()){
                    for (var i=0; i < visualInfo.children.length; i++){
                        sourceDataIdMap[visualInfo.children[i].getDataId()] = visualInfo.children[i];
                    }
                }
                else {
                    sourceDataIdMap[clusterObj.getDataId()] = clusterObj;
                }

                // Iterate all the unique entries in the map to make a list of source Ids.
                for (var sourceId in sourceDataIdMap){
                    if (sourceDataIdMap.hasOwnProperty(sourceId)){
                        sourceIds.push(sourceId);
                    }
                }

                aperture.io.rest(
                    '/aggregatedlinks',
                    'POST',
                    function(response){
                        var links = response.data;

                        for (var sourceDataId in links) {
                            if (links.hasOwnProperty(sourceDataId)) {
                                var sourceObj = sourceDataIdMap[sourceDataId];
                                if (sourceObj){
                                    var outgoingLinks = links[sourceDataId];
                                    for (var i=0; i < outgoingLinks.length; i++){
                                        var targetDataId = outgoingLinks[i].target;
                                        var amount = xfWorkspaceModule.getValueByTag(outgoingLinks[i], 'AMOUNT');
                                        var linkCount = xfWorkspaceModule.getValueByTag(outgoingLinks[i], 'CONSTRUCTED');
                                        var targetObj = targetDataIdMap[targetDataId];
                                        if (targetObj){
                                            xfLink.createInstance(sourceObj, targetObj, amount, linkCount);
                                        }
                                        else {
                                            console.error('Unable to find target UIObject for dataId: ' + targetDataId);
                                        }
                                    }
                                }
                                else {
                                    console.error('Unable to find source UIObject for dataId: ' + sourceDataId);
                                }
                            }
                        }
                        renderAndSaveStateCallback();
                    },
                    {
                        postData : {
                            sessionId : workspaceState.sessionId,
                            queryId: (new Date()).getTime(),
                            sourceIds : sourceIds,
                            targetIds : targetIds,
                            linktype : 'source',
                            type: 'FINANCIAL',
                            aggregate: 'FLOW',
                            startdate: workspaceState.dates.startDate,
                            enddate: workspaceState.dates.endDate,
                            contextid: column.getXfId(),
                            targetcontextid: nextcolumn.getXfId()
                        },
                        contentType: 'application/json'
                    }
                );
            }
        };
        //--------------------------------------------------------------------------------------------------------------
        return {
            aggregateRightLinks : _aggregateRightLinks,
            aggregateLeftLinks : _aggregateLeftLinks
        };
    }
);