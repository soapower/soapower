
# --- !Ups

update service set timeoutms = 120000;

alter table request_data alter column sender varchar(15) not null;
alter table request_data alter column remoteTarget varchar(80) not null;

update request_data set localTarget = '', remoteTarget = '' where purged = true;

create index idx_request_data_5 on request_data (isStats);
create index idx_request_data_6 on request_data (isStats, startTime);

update environment set nbDayKeepAllData = 3;

drop index idx_request_data_2;

alter table request_data add column serviceId int not null default -1;
create index idx_request_data_2 on request_data (serviceId, startTime);

alter table request_data drop column localTarget;
alter table request_data drop column remoteTarget;

# --- !Downs

drop index if exists idx_request_data_2;
drop index if exists idx_request_data_5;
drop index if exists idx_request_data_6;

alter table request_data add column localTarget varchar(255) not null default '';
alter table request_data add column remoteTarget varchar(255) not null default '';
alter table request_data drop column serviceId;

create index idx_request_data_2 on request_data (localTarget, remoteTarget, startTime);


