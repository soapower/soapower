# --- !Ups

alter table environment add column hourRecordXmlDataMin int not null default 6;
alter table environment add column hourRecordXmlDataMax int not null default 22;
alter table environment add column nbDayKeepXmlData int not null default 5;
alter table request_data add column purged enum('true','false') NOT NULL DEFAULT 'false';

create index idx_request_data_3 on request_data (purged);

# --- !Downs

alter table environment drop column hourRecordXmlDataMin;
alter table environment drop column hourRecordXmlDataMax;
alter table environment drop column nbDayKeepXmlData;

alter table request_data drop column purged;