# --- !Ups

create table environment_group (
  id                        int not null auto_increment,
  name                      varchar(255) not null,
  constraint pk_group primary key (id)) ENGINE=InnoDB;

insert into environment_group values (1, "DefaultGroup");

alter table environment add column groupId int not null;

update environment set groupId = 1;

alter table environment add constraint fk_environment_group_1 foreign key (groupId) references environment_group (id) on delete restrict on update restrict;

# --- !Downs

alter table environment drop foreign key fk_environment_group_1;
alter table environment drop column groupId;
drop table environment_group;
