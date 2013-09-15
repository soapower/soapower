# --- !Ups

create table mock_group (
  id                        int not null auto_increment,
  name                      varchar(255) not null,
  groupId                   int not null,
  constraint pk_mock_group primary key (id)) ENGINE=InnoDB;

create table mock (
  id                           int not null auto_increment,
  name                         varchar(255) not null,
  description                  varchar(255) not null,
  criterias                    varchar(255) not null,
  timeout                      int not null,
  response                     varchar(255) not null,
  mockGroupId                  int not null,
  constraint pk_mock primary key (id)) ENGINE=InnoDB
;

alter table mock add constraint fk_mock_groups_1 foreign key (groupId) references group (id) on delete restrict on update restrict;

alter table mock add constraint fk_mock_mockgroups_1 foreign key (mockGroupId) references mock_group (id) on delete restrict on update restrict;

# --- !Downs

alter table mock drop foreign key fk_mock_mockgroups_1;
alter table mock_group drop foreign key fk_mock_groups_1;
drop table mock;
drop table mock_group;
