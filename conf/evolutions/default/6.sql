# --- !Ups

alter table request_data add column requestHeaders varchar(2000) null;
alter table request_data add column responseHeaders varchar(2000) null;

# --- !Downs

alter table request_data drop column requestHeaders;
alter table request_data drop column responseHeaders;