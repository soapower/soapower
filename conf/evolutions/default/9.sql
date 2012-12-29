
# --- !Ups

update service set timeoutms = 120000;

alter table request_data alter column sender varchar(15) not null;
alter table request_data alter column remoteTarget varchar(80) not null;

update request_data set localTarget = '', remoteTarget = '' where purged = true;

create index idx_request_data_5 on request_data (isStats);
create index idx_request_data_6 on request_data (isStats, startTime);


update environment set nbDayKeepAllData = 3;

# --- !Downs

drop index if exists idx_request_data_5;
drop index if exists idx_request_data_6;