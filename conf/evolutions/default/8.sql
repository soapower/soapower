# --- !Ups

create index idx_request_data_4 on request_data (startTime, soapAction, environmentId, purged, status);

update request_data set response = '', request = '', requestHeaders = '', responseHeaders = '' where purged = 'true';

alter table request_data modify column sender varchar(25) not null;
alter table request_data modify column soapAction varchar(50) not null;
alter table request_data modify column localTarget varchar(50) not null;
alter table request_data modify column remoteTarget varchar(100) not null;

alter table environment add column nbDayKeepAllData int not null default 10;
alter table request_data add column isStats enum('true','false') NOT NULL DEFAULT 'false';

delete from request_data where soapAction like '"%"';
delete from soapAction where name like '"%"';

# --- !Downs

drop index idx_request_data_4;

alter table environment drop column nbDayKeepAllData;
alter table request_data drop column isStats;
