# --- First database schema

# --- !Ups

create table environment (
  id                        int not null auto_increment,
  name                      varchar(255) not null,
  constraint pk_environment primary key (id)) ENGINE=InnoDB
;

create table service (
  id                        int not null auto_increment,
  description               varchar(255) not null,
  localTarget               varchar(255) not null,
  remoteTarget              varchar(255) not null,
  environment_id            int not null,
  constraint pk_service primary key (id)) ENGINE=InnoDB
;

alter table service add constraint fk_service_environment_1 foreign key (environment_id) references environment (id) on delete restrict on update restrict;
create index ix_service_environment_1 on service (environment_id);


# --- !Downs

drop table environment;

drop table service;

