
# --- !Ups

update service set timeoutms = 120000;

alter table request_data alter column sender varchar(15) not null;
alter table request_data alter column remoteTarget varchar(80) not null;

update request_data set localTarget = '', remoteTarget = '' where purged = true;


update environment set nbDayKeepAllData = 3;

# --- !Downs
