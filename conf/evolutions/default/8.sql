
# --- !Ups

create index idx_request_data_4 on request_data (startTime, soapAction, environmentId, status);


# --- !Downs

drop index if exists idx_request_data_4;
