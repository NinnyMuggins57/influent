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
-- Influent Data Views 1.2 DRAFT
-- -----------------------------

--
-- FINANCIAL FLOW - ALL
--  Used to build the aggregate flow diagrams
--   
--   FromEntityId - entity UID that is the source of the transactions
--   FromEntityType - type of src entity: O = owner summary, A = account, S = cluster summary entity
--   ToEntityId - entity UID that is the target of the transactions
--   ToEntityType - type of dst entity: O = owner summary, A = account, S = cluster summary entity
--   FirstTransaction - datetime of first transaction
--   LastTransaction - datetime of last transaction
--   Amount - aggregate amount
--
create table FinFlow (FromEntityId varchar(100), FromEntityType varchar(1), ToEntityId varchar(100), ToEntityType varchar(1), FirstTransaction datetime, LastTransaction datetime, Amount float, CONSTRAINT pk_ID PRIMARY KEY (FromEntityId, ToEntityId));

create table FinEntity(EntityId varchar(100) PRIMARY KEY, InboundDegree int, UniqueInboundDegree int,  OutboundDegree int, UniqueOutboundDegree int);

--
-- FINANCIAL FLOW - AGGREGATED BY TIME
--  Used to build the aggregate flow diagrams (aggregated by time)
--  and used to build the highlighted sub-section of the time series charts on entities.
--   
--   FromEntityId - entity UID that is the source of the transactions
--   FromEntityType - type of src entity: O = owner summary, A = account, S = cluster summary entity
--   ToEntityId - entity UID that is the target of the transactions
--   ToEntityType - type of dst entity: O = owner summary, A = account, S = cluster summary entity
--   Amount - aggregate amount for this time period
--   Date - start of the time period
--
create table FinFlowDaily     (FromEntityId varchar(100), FromEntityType varchar(1), ToEntityId varchar(100), ToEntityType varchar(1), Amount float, PeriodDate datetime, CONSTRAINT pk_ID PRIMARY KEY (FromEntityId, ToEntityId, PeriodDate));
create table FinFlowWeekly    (FromEntityId varchar(100), FromEntityType varchar(1), ToEntityId varchar(100), ToEntityType varchar(1), Amount float, PeriodDate datetime, CONSTRAINT pk_ID PRIMARY KEY (FromEntityId, ToEntityId, PeriodDate));
create table FinFlowMonthly   (FromEntityId varchar(100), FromEntityType varchar(1), ToEntityId varchar(100), ToEntityType varchar(1), Amount float, PeriodDate datetime, CONSTRAINT pk_ID PRIMARY KEY (FromEntityId, ToEntityId, PeriodDate));
create table FinFlowQuarterly (FromEntityId varchar(100), FromEntityType varchar(1), ToEntityId varchar(100), ToEntityType varchar(1), Amount float, PeriodDate datetime, CONSTRAINT pk_ID PRIMARY KEY (FromEntityId, ToEntityId, PeriodDate));
create table FinFlowYearly    (FromEntityId varchar(100), FromEntityType varchar(1), ToEntityId varchar(100), ToEntityType varchar(1), Amount float, PeriodDate datetime, CONSTRAINT pk_ID PRIMARY KEY (FromEntityId, ToEntityId, PeriodDate));

--
-- FINANCIAL ENTITY SUMMARY
--  Used to build the time series charts on entities (aggregated by time).
--   
--   EntityId - entity UID
--   Date - start of the time period
--   InboundAmount - aggregate credits for this time period
--   InboundDegree - unique inbound transations by entity
--   OutboundAmount - aggregate debits for this time period
--   OutboundDegree - unique outbound transations by entity
--   Balance - aggregate credits - debits up until this time period
--
create table FinEntityDaily     (EntityId varchar(100), PeriodDate datetime, InboundAmount float, InboundDegree int, OutboundAmount float, OutboundDegree int, Balance float, CONSTRAINT pk_ID PRIMARY KEY (EntityId, PeriodDate));
create table FinEntityWeekly    (EntityId varchar(100), PeriodDate datetime, InboundAmount float, InboundDegree int, OutboundAmount float, OutboundDegree int, Balance float, CONSTRAINT pk_ID PRIMARY KEY (EntityId, PeriodDate));
create table FinEntityMonthly   (EntityId varchar(100), PeriodDate datetime, InboundAmount float, InboundDegree int, OutboundAmount float, OutboundDegree int, Balance float, CONSTRAINT pk_ID PRIMARY KEY (EntityId, PeriodDate));
create table FinEntityQuarterly (EntityId varchar(100), PeriodDate datetime, InboundAmount float, InboundDegree int, OutboundAmount float, OutboundDegree int, Balance float, CONSTRAINT pk_ID PRIMARY KEY (EntityId, PeriodDate));
create table FinEntityYearly    (EntityId varchar(100), PeriodDate datetime, InboundAmount float, InboundDegree int, OutboundAmount float, OutboundDegree int, Balance float, CONSTRAINT pk_ID PRIMARY KEY (EntityId, PeriodDate));

--
-- CLUSTER SUMMARY
--  Used to summarize an entity with a large number of associated entities (e.g. account owner with a large number of accounts)
--   It is up to each application to determine what cluster summaries to generate based on the size of data
--
--   EntityId - entity UID of cluster entity
--   Property - name of summary property
--   Tag - Property_Tag to associate with property
--   Type - the data type of property value (string, double, float, int, date)
--   Value - the string representation of the property value
--   Stat - an associated stat for the propety value such as frequency or weight
--
create table ClusterSummary	(EntityId varchar(100), Property varchar(50), Tag varchar(50), Type varchar(50), Value varchar(200), Stat float, CONSTRAINT pk_ID PRIMARY KEY (EntityId, Property, Value));

--
-- CLUSTER SUMMARY MEMBERS
--  Used to keep track of entities that are members of a cluster summary
--   It is up to each application to determine what cluster summaries to generate based on the size of data
--
--   SummaryId - UID of cluster summary
--   EntityId - member entity UID
--
create table ClusterSummaryMembers (SummaryId varchar(100), EntityId varchar(100), CONSTRAINT pk_ID PRIMARY KEY (SummaryId, EntityId));

--
-- DATA VIEW DRIVERS
--  These scripts will populate the data views above.
--
--  Step 1. Modify this to pull data from your raw data.  Add any transactions to cluster summaries as well.
--  
insert into FinFlowDaily
 SELECT [source_id], 'A', [dest_id], 'A', sum([amount]), convert(varchar(50), [dt], 101)
  FROM YOUR_RAW_DATA
  group by [source_id], [dest_id], convert(varchar(50), [dt], 101)

--
--  Step 2. The rest of the script will collect data from FinFlowDaily.
--          Execute the rest of this script "as-is".
--  

--  build the rest of the FinFlow aggregations
insert into FinFlowWeekly
 select FromEntityId, FromEntityType, ToEntityId, ToEntityType, sum(Amount), CONVERT(varchar(50), (DATEADD(dd, @@DATEFIRST - DATEPART(dw, PeriodDate) - 6, PeriodDate)), 101)
  from FinFlowDaily
  group by FromEntityId, FromEntityType, ToEntityId, ToEntityType, CONVERT(varchar(50), (DATEADD(dd, @@DATEFIRST - DATEPART(dw, PeriodDate) - 6, PeriodDate)), 101);
  
insert into FinFlowMonthly
 select FromEntityId, FromEntityType, ToEntityId, ToEntityType, sum(Amount), CONVERT(varchar(50), convert(varchar(4), DATEPART(yyyy, PeriodDate)) + '/' + convert(varchar(2), DATEPART(mm, PeriodDate)) + '/01', 101)
  from FinFlowDaily
  group by FromEntityId, FromEntityType, ToEntityId, ToEntityType, CONVERT(varchar(50), convert(varchar(4), DATEPART(yyyy, PeriodDate)) + '/' + convert(varchar(2), DATEPART(mm, PeriodDate)) + '/01', 101);
  
insert into FinFlowQuarterly
 select FromEntityId, FromEntityType, ToEntityId, ToEntityType, sum(Amount), CONVERT(varchar(50), convert(varchar(4), DATEPART(yyyy, PeriodDate)) + '/' + case when DATEPART(q, PeriodDate)=1 then '01' when DATEPART(q, PeriodDate)=2 then '04' when DATEPART(q, PeriodDate)=3 then '07' when DATEPART(q, PeriodDate)=4 then '010' end + '/01', 101)
  from FinFlowMonthly
  group by FromEntityId, FromEntityType, ToEntityId, ToEntityType, CONVERT(varchar(50), convert(varchar(4), DATEPART(yyyy, PeriodDate)) + '/' + case when DATEPART(q, PeriodDate)=1 then '01' when DATEPART(q, PeriodDate)=2 then '04' when DATEPART(q, PeriodDate)=3 then '07' when DATEPART(q, PeriodDate)=4 then '010' end + '/01', 101);
  
insert into FinFlowYearly
 select FromEntityId, FromEntityType, ToEntityId, ToEntityType, sum(Amount), CONVERT(varchar(50), convert(varchar(4), DATEPART(yyyy, PeriodDate)) + '/01/01', 101)
  from FinFlowMonthly
  group by FromEntityId, FromEntityType, ToEntityId, ToEntityType, CONVERT(varchar(50), convert(varchar(4), DATEPART(yyyy, PeriodDate)) + '/01/01', 101);

--  create FinFlow indices
create index ix_ffd_from on FinFlowDaily     (FromEntityId, PeriodDate, ToEntityId,   Amount);
create index ix_ffd_to   on FinFlowDaily     (ToEntityId,   PeriodDate, FromEntityId, Amount);
create index ix_ffw_from on FinFlowWeekly    (FromEntityId, PeriodDate, ToEntityId,   Amount);
create index ix_ffw_to   on FinFlowWeekly    (ToEntityId,   PeriodDate, FromEntityId, Amount);
create index ix_ffm_from on FinFlowMonthly   (FromEntityId, PeriodDate, ToEntityId,   Amount);
create index ix_ffm_to   on FinFlowMonthly   (ToEntityId,   PeriodDate, FromEntityId, Amount);
create index ix_ffq_from on FinFlowQuarterly (FromEntityId, PeriodDate, ToEntityId,   Amount);
create index ix_ffq_to   on FinFlowQuarterly (ToEntityId,   PeriodDate, FromEntityId, Amount);
create index ix_ffy_from on FinFlowYearly    (FromEntityId, PeriodDate, ToEntityId,   Amount);
create index ix_ffy_to   on FinFlowYearly    (ToEntityId,   PeriodDate, FromEntityId, Amount);

--  build FinFlow
insert into FinFlow 
 select FromEntityId, FromEntityType, ToEntityId, ToEntityType, min(PeriodDate), max(PeriodDate), sum(Amount)
  from FinFlowDaily
  group by FromEntityId, FromEntityType, ToEntityId, ToEntityType;

create index ix_ff_to_from on FinFlow (ToEntityId, FromEntityId);
create index ix_ff_from_to on FinFlow (FromEntityId, ToEntityId);

-- build FinEntity
insert into FinEntity
 select EntityId, sum(inboundDegree), sum(uniqueInboundDegree), sum(outboundDegree), sum(uniqueOutboundDegree)
  from (
   select FromEntityId as EntityId, 0 as inboundDegree, 0 as uniqueInboundDegree, count(ToEntityId) as outboundDegree, count( distinct ToEntityId ) as uniqueOutboundDegree
    from FinFlowDaily
    where ToEntityType = 'A'
    group by FromEntityId
   union
   select ToEntityId as EntityId, count(FromEntityId) as inboundDegree, count( distinct FromEntityId ) as uniqueInboundDegree, 0 as outboundDegree, 0 as uniqueOutboundDegree
    from FinFlowDaily
    where FromEntityType = 'A'
    group by ToEntityId
  ) q
  group by EntityId

create index ix_ff_id on FinEntity (EntityId);

--  build FinEntityDaily
create table temp_ids (Entity varchar(100));
create index tids on temp_ids (Entity);

insert into temp_ids
 select distinct FromEntityId
  from FinFlowYearly
 union
 select distinct ToEntityId
  from FinFlowYearly;
  
insert into FinEntityDaily select Entity, PeriodDate,
       sum(case when ToEntityId = Entity and FromEntityType = 'A' then Amount else 0 end),
       sum(case when ToEntityId = Entity and FromEntityType = 'A' then 1 else 0 end), -- calculate inbound degree
       sum(case when FromEntityId = Entity and ToEntityType = 'A' then Amount else 0 end),
       sum(case when FromEntityId = Entity and ToEntityType = 'A' then 1 else 0 end), -- calculate outbound degree
       0 -- TODO calculate balance
 from temp_ids
 join FinFlowDaily on FromEntityId = Entity or ToEntityId = Entity
 group by Entity, PeriodDate;
 
-- cleanup
drop table temp_ids;

-- build the rest of the FinEntity aggregations
insert into FinEntityWeekly
 select EntityId, CONVERT(varchar(50), (DATEADD(dd, @@DATEFIRST - DATEPART(dw, PeriodDate) - 6, PeriodDate)), 101), sum(InboundAmount), sum(InboundDegree), sum(OutboundAmount), sum(OutboundDegree), 0
  from FinEntityDaily
  group by EntityId, CONVERT(varchar(50), (DATEADD(dd, @@DATEFIRST - DATEPART(dw, PeriodDate) - 6, PeriodDate)), 101);
  
insert into FinEntityMonthly
 select EntityId, CONVERT(varchar(50), convert(varchar(4), DATEPART(yyyy, PeriodDate)) + '/' + convert(varchar(2), DATEPART(mm, PeriodDate)) + '/01', 101), sum(InboundAmount), sum(InboundDegree), sum(OutboundAmount), sum(OutboundDegree), 0
  from FinEntityDaily
  group by EntityId, CONVERT(varchar(50), convert(varchar(4), DATEPART(yyyy, PeriodDate)) + '/' + convert(varchar(2), DATEPART(mm, PeriodDate)) + '/01', 101);
  
insert into FinEntityQuarterly
 select EntityId, CONVERT(varchar(50), convert(varchar(4), DATEPART(yyyy, PeriodDate)) + '/' + case when DATEPART(q, PeriodDate)=1 then '01' when DATEPART(q, PeriodDate)=2 then '04' when DATEPART(q, PeriodDate)=3 then '07' when DATEPART(q, PeriodDate)=4 then '010' end + '/01', 101), sum(InboundAmount), sum(InboundDegree), sum(OutboundAmount), sum(OutboundDegree), 0
  from FinEntityMonthly
  group by EntityId, CONVERT(varchar(50), convert(varchar(4), DATEPART(yyyy, PeriodDate)) + '/' + case when DATEPART(q, PeriodDate)=1 then '01' when DATEPART(q, PeriodDate)=2 then '04' when DATEPART(q, PeriodDate)=3 then '07' when DATEPART(q, PeriodDate)=4 then '010' end + '/01', 101);
  
insert into FinEntityYearly
 select EntityId, CONVERT(varchar(50), convert(varchar(4), DATEPART(yyyy, PeriodDate)) + '/01/01', 101), sum(InboundAmount), sum(InboundDegree), sum(OutboundAmount), sum(OutboundDegree), 0
  from FinEntityQuarterly
  group by EntityId, CONVERT(varchar(50), convert(varchar(4), DATEPART(yyyy, PeriodDate)) + '/01/01', 101);
 
create index ix_fed on FinEntityDaily     (EntityId, PeriodDate, InboundAmount, OutboundAmount);
create index ix_few on FinEntityWeekly    (EntityId, PeriodDate, InboundAmount, OutboundAmount);
create index ix_fem on FinEntityMonthly   (EntityId, PeriodDate, InboundAmount, OutboundAmount);
create index ix_feq on FinEntityQuarterly (EntityId, PeriodDate, InboundAmount, OutboundAmount);
create index ix_fey on FinEntityYearly    (EntityId, PeriodDate, InboundAmount, OutboundAmount);

create index ix_csum on ClusterSummary	(EntityId);
create index ix_cmem on ClusterSummaryMembers  (SummaryId);