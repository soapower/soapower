# --- !Ups

alter table service add column typeRequest enum('soap','rest') null;
alter table service add column httpMethod enum('get','post','put','delete') null;
alter table request_data add column requestContentType varchar(255);
alter table request_data add column responseContentType varchar(255);

# --- !Downs

alter table service drop column typeRequest;
alter table service drop column httpMethod;

alter table request_data drop column requestContentType;
alter table request_data drop column responseContentType;