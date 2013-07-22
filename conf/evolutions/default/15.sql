# --- !Ups

create table groups (
  id                        int not null auto_increment,
  name                      varchar(255) not null,
  constraint pk_group primary key (id)) ENGINE=InnoDB;

insert into groups values (1, "DefaultGroup");

alter table environment add column groupId int not null;

update environment set groupId = 1;

alter table environment add constraint fk_groups_1 foreign key (groupId) references groups (id) on delete restrict on update restrict;

# --- !Downs

alter table environment drop foreign key fk_groups_1;
alter table environment drop column groupId;
drop table groups;
