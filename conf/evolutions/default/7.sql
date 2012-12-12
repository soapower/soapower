# --- !Ups

alter table environment add column hourRecordXmlDataMin int not null default 6;
alter table environment add column hourRecordXmlDataMax int not null default 22;
alter table environment add column nbDayKeepXmlData int not null default 5;

# --- !Downs

alter table environment drop column hourRecordXmlDataMin;
alter table environment drop column hourRecordXmlDataMax;
alter table environment drop column nbDayKeepXmlData;

