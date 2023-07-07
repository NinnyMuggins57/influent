/**
 * Copyright (c) 2013 Oculus Info Inc. 
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

 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.

 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

-- -----------------------------
-- Influent Data Views 1.5 DRAFT
-- -----------------------------

--
--- Cluster membership dataview - note this view supports hierarchical clustering -  note: fuzzy cluster membership is not supported
---   table is denormalized with no referential integrity for simplicity of integration
--
CREATE TABLE global_cluster_dataview
(
id bigint PRIMARY KEY IDENTITY,
clusterid nvarchar(100),  					-- using nvarchar for uid rather than bigint to support guids and other uid schemes
rootid nvarchar(100), 						-- id of root cluster in hierarchy - NULL if this cluster is a root level cluster
parentid nvarchar(100), 					-- id of parent cluster in hierarchy - NULL if this cluster is a root level cluster
hierarchylevel int NOT NULL DEFAULT 0,		-- level of cluster in hierarchy - level 0 is the root and level N ( N > 0 ) is a leaf cluster
isleaf nvarchar(1) NOT NULL DEFAULT 'N',	-- whether this cluster is a leaf (not a parent of any other cluster) values: 'Y' or 'N'
entityid nvarchar(100) NOT NULL  			-- entity id of cluster member (entity id's are the id's of raw entities that are clustered)
);

-- 
-- indexes for fast data retrieval
--
CREATE INDEX ix_gcluster_dataview_rowid ON global_cluster_dataview_test (id);
CREATE INDEX ix_gcluster_dataview_id ON global_cluster_dataview_test (clusterid);
CREATE INDEX ix_gcluster_dataview_eid ON global_cluster_dataview_test (entityid);
CREATE INDEX ix_gcluster_dataview_pid ON global_cluster_dataview_test (parentid);
CREATE INDEX ix_gcluster_dataview_id_eid ON global_cluster_dataview_test (clusterid, entityid);
CREATE INDEX ix_gcluster_dataview_rid_lvl ON global_cluster_dataview_test (entityid, hierarchylevel);
