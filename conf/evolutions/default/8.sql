
# --- !Ups

create index idx_request_data_4 on request_data (startTime, soapAction, environmentId, status);

update request_data set response = '', request = '', requestHeaders = '', responseHeaders = '' where purged = true;

alter table request_data alter column sender varchar(25) not null;
alter table request_data alter column soapAction varchar(50) not null;
alter table request_data alter column localTarget varchar(50) not null;
alter table request_data alter column remoteTarget varchar(100) not null

# --- !Downs

drop index if exists idx_request_data_4;
