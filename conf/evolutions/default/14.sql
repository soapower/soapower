
 --- !Ups

alter table request_data modify column soapAction varchar(255) not null;

create table environment_group (
  groupId                        int not null auto_increment,
  groupName                      varchar(255) not null,
  constraint pk_group primary key (groupId)) ENGINE=InnoDB
;
insert into environment_group values (1, "DefaultGroup");

alter table environment add column groupId int not null;
update  environment set groupId = 1;
alter table environment add constraint fk_environment_group_1 foreign key (groupId) references environment_group (groupId) on delete restrict on update restrict;



 --- !Downs

alter table request_data modify column soapAction varchar(50) not null;


alter table environment drop foreign key fk_environment_group_1;	
alter table environment drop column groupId;
drop table environment_group;

