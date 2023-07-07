use [DB SCHEMA NAME];

-- NOTE: If dataview table names are different from defaults then modify the below scripts accordingly

--
--- Update schema to version 1.2 dataviews from 1.1
--
create table FinEntity(EntityId varchar(100), InboundDegree int, UniqueInboundDegree int,  OutboundDegree int, UniqueOutboundDegree int);
create table ClusterSummary	(EntityId varchar(100), Property varchar(50), Tag varchar(50), Type varchar(50), Value varchar(200), Stat float);
create table ClusterSummaryMembers (SummaryId varchar(100), EntityId varchar(100));

alter table FinFlow add FromEntityType varchar(1), ToEntityType varchar(1);
alter table FinFlowDaily add FromEntityType varchar(1), ToEntityType varchar(1);
alter table FinFlowWeekly add FromEntityType varchar(1), ToEntityType varchar(1);
alter table FinFlowMonthly add FromEntityType varchar(1), ToEntityType varchar(1);
alter table FinFlowQuarterly add FromEntityType varchar(1), ToEntityType varchar(1);
alter table FinFlowYearly add FromEntityType varchar(1), ToEntityType varchar(1);

alter table FinEntityDaily add InboundDegree int, OutboundDegree int;
alter table FinEntityWeekly add InboundDegree int, OutboundDegree int;
alter table FinEntityMonthly add InboundDegree int, OutboundDegree int;
alter table FinEntityQuarterly add InboundDegree int, OutboundDegree int;
alter table FinEntityYearly add InboundDegree int, OutboundDegree int;

--
--- disable the indices for bulk update
--
alter index [ix_ff_from] ON FinFlow disable;
alter index [ix_ff_to] ON FinFlow disable;
alter index [ix_ffd_from] ON FinFlowDaily disable;
alter index [ix_ffd_to] ON FinFlowDaily disable;
alter index [ix_ffw_from] ON FinFlowWeekly disable;
alter index [ix_ffw_to] ON FinFlowWeekly disable;
--
--- update the fin flow tables (skip Monthly, Quarterly and Yearly since they will be refreshed)
--
update FinFlow set FromEntityType = 'A', ToEntityType = 'A';
update FinFlowDaily set FromEntityType = 'A', ToEntityType = 'A';
update FinFlowWeekly set FromEntityType = 'A', ToEntityType = 'A';
--update FinFlowMonthly set FromEntityType = 'A', ToEntityType = 'A';
--update FinFlowQuarterly set FromEntityType = 'A', ToEntityType = 'A';
--update FinFlowYearly set FromEntityType = 'A', ToEntityType = 'A';

--
--- rebuild the indices
--

alter index [ix_ff_from] ON FinFlow rebuild;
alter index [ix_ff_to] ON FinFlow rebuild;
alter index [ix_ffd_from] ON FinFlowDaily rebuild;
alter index [ix_ffd_to] ON FinFlowDaily rebuild;
alter index [ix_ffw_from] ON FinFlowWeekly rebuild;
alter index [ix_ffw_to] ON FinFlowWeekly rebuild;


--
--- FinFlowMonthly was previously incorrectly calculated (refresh Monthly, Quarterly and Yearly tables)
--
truncate table FinFlowMonthly;
truncate table FinFlowQuarterly;
truncate table FinFlowYearly;

alter index [ix_ffm_from] ON FinFlowMonthly disable;
alter index [ix_ffm_to] ON FinFlowMonthly disable;
alter index [ix_ffq_from] ON FinFlowQuarterly disable;
alter index [ix_ffq_to] ON FinFlowQuarterly disable;
alter index [ix_ffy_from] ON FinFlowYearly disable;
alter index [ix_ffy_to] ON FinFlowYearly disable;

insert into FinFlowMonthly (FromEntityId, FromEntityType, ToEntityId, ToEntityType, Amount, PeriodDate)
 select FromEntityId, FromEntityType, ToEntityId, ToEntityType, sum(Amount), CONVERT(varchar(50), convert(varchar(4), DATEPART(yyyy, PeriodDate)) + '/' + convert(varchar(2), DATEPART(mm, PeriodDate)) + '/01', 101)
  from FinFlowDaily
  group by FromEntityId, FromEntityType, ToEntityId, ToEntityType, CONVERT(varchar(50), convert(varchar(4), DATEPART(yyyy, PeriodDate)) + '/' + convert(varchar(2), DATEPART(mm, PeriodDate)) + '/01', 101);

insert into FinFlowQuarterly (FromEntityId, FromEntityType, ToEntityId, ToEntityType, Amount, PeriodDate)
 select FromEntityId, FromEntityType, ToEntityId, ToEntityType, sum(Amount), CONVERT(varchar(50), convert(varchar(4), DATEPART(yyyy, PeriodDate)) + '/' + case when DATEPART(q, PeriodDate)=1 then '01' when DATEPART(q, PeriodDate)=2 then '04' when DATEPART(q, PeriodDate)=3 then '07' when DATEPART(q, PeriodDate)=4 then '010' end + '/01', 101)
  from FinFlowMonthly
  group by FromEntityId, FromEntityType, ToEntityId, ToEntityType, CONVERT(varchar(50), convert(varchar(4), DATEPART(yyyy, PeriodDate)) + '/' + case when DATEPART(q, PeriodDate)=1 then '01' when DATEPART(q, PeriodDate)=2 then '04' when DATEPART(q, PeriodDate)=3 then '07' when DATEPART(q, PeriodDate)=4 then '010' end + '/01', 101);

insert into FinFlowYearly (FromEntityId, FromEntityType, ToEntityId, ToEntityType, Amount, PeriodDate)
 select FromEntityId, FromEntityType, ToEntityId, ToEntityType, sum(Amount), CONVERT(varchar(50), convert(varchar(4), DATEPART(yyyy, PeriodDate)) + '/01/01', 101)
  from FinFlowMonthly
  group by FromEntityId, FromEntityType, ToEntityId, ToEntityType, CONVERT(varchar(50), convert(varchar(4), DATEPART(yyyy, PeriodDate)) + '/01/01', 101);

alter index [ix_ffm_from] ON FinFlowMonthly rebuild;
alter index [ix_ffm_to] ON FinFlowMonthly rebuild;
alter index [ix_ffq_from] ON FinFlowQuarterly rebuild;
alter index [ix_ffq_to] ON FinFlowQuarterly rebuild;
alter index [ix_ffy_from] ON FinFlowYearly rebuild;
alter index [ix_ffy_to] ON FinFlowYearly rebuild;


--
--- refresh the fin entity tables
--
truncate table FinEntityDaily;
truncate table FinEntityWeekly;
truncate table FinEntityMonthly;
truncate table FinEntityQuarterly;
truncate table FinEntityYearly;

alter index [ix_fed] ON FinEntityDaily disable;
alter index [ix_few] ON FinEntityWeekly disable;
alter index [ix_fem] ON FinEntityMonthly disable;
alter index [ix_feq] ON FinEntityQuarterly disable;
alter index [ix_fey] ON FinEntityYearly disable;

create table temp_ids (Entity varchar(100));
create index tids on temp_ids (Entity);

insert into temp_ids
 select distinct FromEntityId
  from FinFlowYearly
 union
 select distinct ToEntityId
  from FinFlowYearly;

insert into FinEntityDaily (EntityId, PeriodDate, InboundAmount, InboundDegree, OutboundAmount, OutboundDegree, balance)
  select Entity, PeriodDate,
       sum(case when ToEntityId = Entity and FromEntityType = 'A' then Amount else 0 end),
       sum(case when ToEntityId = Entity and FromEntityType = 'A' then 1 else 0 end), -- calculate inbound degree
       sum(case when FromEntityId = Entity and ToEntityType = 'A' then Amount else 0 end),
       sum(case when FromEntityId = Entity and ToEntityType = 'A' then 1 else 0 end), -- calculate outbound degree
       0 -- TODO calculate balance
 from temp_ids
 join FinFlowDaily on FromEntityId = Entity or ToEntityId = Entity
 group by Entity, PeriodDate;

 drop table temp_ids;

insert into FinEntityWeekly (EntityId, PeriodDate, InboundAmount, InboundDegree, OutboundAmount, OutboundDegree, balance)
 select EntityId, CONVERT(varchar(50), (DATEADD(dd, @@DATEFIRST - DATEPART(dw, PeriodDate) - 6, PeriodDate)), 101), sum(InboundAmount), sum(InboundDegree), sum(OutboundAmount), sum(OutboundDegree), 0
  from FinEntityDaily
  group by EntityId, CONVERT(varchar(50), (DATEADD(dd, @@DATEFIRST - DATEPART(dw, PeriodDate) - 6, PeriodDate)), 101);
  
insert into FinEntityMonthly (EntityId, PeriodDate, InboundAmount, InboundDegree, OutboundAmount, OutboundDegree, balance)
 select EntityId, CONVERT(varchar(50), convert(varchar(4), DATEPART(yyyy, PeriodDate)) + '/' + convert(varchar(2), DATEPART(mm, PeriodDate)) + '/01', 101), sum(InboundAmount), sum(InboundDegree), sum(OutboundAmount), sum(OutboundDegree), 0
  from FinEntityDaily
  group by EntityId, CONVERT(varchar(50), convert(varchar(4), DATEPART(yyyy, PeriodDate)) + '/' + convert(varchar(2), DATEPART(mm, PeriodDate)) + '/01', 101);
  
insert into FinEntityQuarterly (EntityId, PeriodDate, InboundAmount, InboundDegree, OutboundAmount, OutboundDegree, balance)
 select EntityId, CONVERT(varchar(50), convert(varchar(4), DATEPART(yyyy, PeriodDate)) + '/' + case when DATEPART(q, PeriodDate)=1 then '01' when DATEPART(q, PeriodDate)=2 then '04' when DATEPART(q, PeriodDate)=3 then '07' when DATEPART(q, PeriodDate)=4 then '010' end + '/01', 101), sum(InboundAmount), sum(InboundDegree), sum(OutboundAmount), sum(OutboundDegree), 0
  from FinEntityMonthly
  group by EntityId, CONVERT(varchar(50), convert(varchar(4), DATEPART(yyyy, PeriodDate)) + '/' + case when DATEPART(q, PeriodDate)=1 then '01' when DATEPART(q, PeriodDate)=2 then '04' when DATEPART(q, PeriodDate)=3 then '07' when DATEPART(q, PeriodDate)=4 then '010' end + '/01', 101);
  
insert into FinEntityYearly (EntityId, PeriodDate, InboundAmount, InboundDegree, OutboundAmount, OutboundDegree, balance)
 select EntityId, CONVERT(varchar(50), convert(varchar(4), DATEPART(yyyy, PeriodDate)) + '/01/01', 101), sum(InboundAmount), sum(InboundDegree), sum(OutboundAmount), sum(OutboundDegree), 0
  from FinEntityQuarterly
  group by EntityId, CONVERT(varchar(50), convert(varchar(4), DATEPART(yyyy, PeriodDate)) + '/01/01', 101);

alter index [ix_fed] ON FinEntityDaily rebuild;
alter index [ix_few] ON FinEntityWeekly rebuild;
alter index [ix_fem] ON FinEntityMonthly rebuild;
alter index [ix_feq] ON FinEntityQuarterly rebuild;
alter index [ix_fey] ON FinEntityYearly rebuild;


--
--- build FinEntity
--
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
create index ix_csum on ClusterSummary	(EntityId);
create index ix_cmem on ClusterSummaryMembers  (SummaryId);