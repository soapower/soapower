# --- First database schema

# --- !Ups

set ignorecase true;

create table request_data (
  id                        bigint not null,
  sender                    varchar(255) not null,
  environmentId             varchar(255) not null,
  localTarget               varchar(255) not null,
  remoteTarget              varchar(255) not null,
  request                   clob not null,
  startTime                 timestamp not null,
  response                  clob null,
  timeInMillis              long null,
  status                    int(3) null,
  constraint pk_request_data primary key (id))
;

create sequence request_data_seq start with 1000;

create index idx_request_data_1 on request_data (id);
create index idx_request_data_2 on request_data (localTarget, remoteTarget, startTime);


# --- !Downs

SET REFERENTIAL_INTEGRITY FALSE;

drop table if exists request_data;

SET REFERENTIAL_INTEGRITY TRUE;

drop sequence if exists request_data_seq;

