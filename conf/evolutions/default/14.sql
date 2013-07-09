# --- !Ups

create table environment_group (
  id                        int not null auto_increment,
  name                      varchar(255) not null,
  constraint pk_group primary key (id)) ENGINE=InnoDB
;


# --- !Downs


drop table environment_group;