# --- !Ups

update service set timeoutms = 120000;

alter table request_data modify column sender varchar(15) not null;
alter table request_data add column serviceId int not null default 0;

ALTER TABLE request_data DROP INDEX idx_request_data_2;
create index idx_request_data_2 on request_data (serviceId, startTime);

alter table request_data drop column localTarget;
alter table request_data drop column remoteTarget;

update environment set nbDayKeepAllData = 3;

create index idx_request_data_5 on request_data (isStats);
create index idx_request_data_6 on request_data (isStats, startTime);

# --- !Downs

ALTER TABLE request_data drop index idx_request_data_2;
ALTER TABLE request_data drop index idx_request_data_5;
ALTER TABLE request_data drop index idx_request_data_6;

alter table request_data add column localTarget varchar(255);
alter table request_data add column remoteTarget varchar(255);
alter table request_data drop column serviceId;

