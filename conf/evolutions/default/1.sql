# --- First database schema

# --- !Ups

set ignorecase true;

create table environment (
  id                        bigint not null,
  name                      varchar(255) not null,
  constraint pk_environment primary key (id))
;

create table service (
  id                        bigint not null,
  description               varchar(255) not null,
  localTarget               varchar(255) not null,
  remoteTarget              varchar(255) not null,
  environment_id            bigint not null,
  constraint pk_service primary key (id))
;

create sequence environment_seq start with 1000;

create sequence service_seq start with 1000;

alter table service add constraint fk_service_environment_1 foreign key (environment_id) references environment (id) on delete restrict on update restrict;
create index ix_service_environment_1 on service (environment_id);


# --- !Downs

SET REFERENTIAL_INTEGRITY FALSE;

drop table if exists environment;

drop table if exists service;

SET REFERENTIAL_INTEGRITY TRUE;

drop sequence if exists environment_seq;

drop sequence if exists service_seq;

