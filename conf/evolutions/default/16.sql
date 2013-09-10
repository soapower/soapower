# --- !Ups

create table mock (
  id                           int not null auto_increment,
  name                         varchar(255) not null,
  description                  varchar(255) not null,
  criterias                    varchar(255) not null,
  timeout                      int not null,
  response                     varchar(255) not null,
  groupId                      int not null,
  constraint pk_mock primary key (id)) ENGINE=InnoDB
;

alter table mock add constraint fk_mock_groups_1 foreign key (groupId) references groups (id) on delete restrict on update restrict;

# --- !Downs

alter table mock drop foreign key fk_mock_groups_1;
drop table mock;

