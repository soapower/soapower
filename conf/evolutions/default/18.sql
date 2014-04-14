# --- !Ups

alter table environment change recordXmlData recordContentData enum('true','false');
alter table environment change hourRecordXmlDataMin hourRecordContentDataMin int(11);
alter table environment change hourRecordXmlDataMax hourRecordContentDataMax int(11);
alter table environment change nbDayKeepXmlData nbDayKeepContentData int(11);

alter table service change recordXmlData recordContentData enum('true','false');

alter table service add column typeRequest enum('soap','rest') null;
alter table service add column httpMethod enum('get','post','put','delete') null;
alter table request_data add column contentType varchar(255) null;
alter table request_data add column requestCall varchar(255) null;
alter table request_data change soapAction serviceAction varchar(255);

rename table soapaction to serviceaction;

# --- !Downs

alter table environment change recordContentData recordXmlData ENUM('true','false');
alter table environment change hourRecordContentDataMin hourRecordXmlDataMin int(11);
alter table environment change hourRecordContentDataMin hourRecordXmlDataMin int(11);
alter table environment change nbDayKeepContentData nbDayKeepXmlData int(11);

alter table service change recordContentData recordXmlData ENUM('true','false');

alter table service drop column typeRequest;
alter table service drop column httpMethod;
alter table request_data drop column contentType;
alter table request_data drop column requestCall;
alter table request_data change serviceAction soapAction varchar(255);

rename table serviceaction to soapaction;