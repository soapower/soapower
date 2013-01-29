# --- !Ups

alter table request_data drop index idx_request_data_1;

delete from request_data where isStats = 'true';

create index idx_request_data_1 on request_data (startTime, status, soapAction, environmentId);

create index idx_request_data_7 on request_data (environmentId, isStats);

# --- !Downs

alter table request_data drop index idx_request_data_1;

alter table request_data drop index idx_request_data_7;