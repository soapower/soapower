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
  timeoutms                    int not null,
  httpStatus                   int not null,
  response                     LONGTEXT not null,
  mockGroupId                  int not null,
  constraint pk_mock primary key (id)) ENGINE=InnoDB
;

alter table service add column mockGroupId int null;

alter table service add column useMockGroup enum('true','false') NOT NULL DEFAULT 'false';

alter table mock_group add constraint fk_mock_groups_1 foreign key (groupId) references groups (id) on delete restrict on update restrict;

alter table mock add constraint fk_mock_mockgroups_1 foreign key (mockGroupId) references mock_group (id) on delete restrict on update restrict;

alter table service add constraint fk_service_mockgroups_1 foreign key(mockGroupId) references mock_group(id) on delete restrict on update restrict;

insert into mock_group values (1, "NoMockGroup", 1);

update service set mockGroupId = 1;
update service set useMockGroup = 'false';

# --- !Downs

alter table service drop foreign key fk_service_mockgroups_1;
alter table service drop column mockGroupId;
alter table service drop column useMockGroup;
alter table mock drop foreign key fk_mock_mockgroups_1;
alter table mock_group drop foreign key fk_mock_groups_1;

drop table mock;
drop table mock_group;
