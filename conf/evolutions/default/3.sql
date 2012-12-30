# --- !Ups

create table request_data (
  id                        int not null auto_increment,
  sender                    varchar(255) not null,
  soapAction                varchar(255) not null,
  environmentId             int not null,
  localTarget               varchar(255) not null,
  remoteTarget              varchar(255) not null,
  request                   LONGTEXT not null,
  startTime                 timestamp not null,
  response                  LONGTEXT null,
  timeInMillis              int null,
  status                    int(3) null,
  constraint pk_request_data primary key (id)) ENGINE=InnoDB
;

create index idx_request_data_1 on request_data (id);
create index idx_request_data_2 on request_data (localTarget, remoteTarget, startTime);


# --- !Downs

drop table request_data;
drop sequence request_data_seq;

