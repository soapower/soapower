# --- !Ups

create table soapaction (
  id                        int not null auto_increment,
  name                      varchar(255) not null,
	thresholdms               int not null,
  constraint pk_soapaction primary key (id)) ENGINE=InnoDB
;

ALTER TABLE soapaction ADD CONSTRAINT uc_name UNIQUE (name)

# --- !Downs

drop table soapaction;
drop sequence soapaction_seq;

