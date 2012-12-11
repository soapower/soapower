# --- First database schema

# --- !Ups

set ignorecase true;

create table soapaction (
  id                        bigint not null,
  name                      varchar(255) not null,
	thresholdms               bigint not null,
  constraint pk_soapaction primary key (id))
;

create sequence soapaction_seq start with 1000;

ALTER TABLE soapaction ADD CONSTRAINT uc_name UNIQUE (name)

# --- !Downs

SET REFERENTIAL_INTEGRITY FALSE;

drop table if exists soapaction;

SET REFERENTIAL_INTEGRITY TRUE;

drop sequence if exists soapaction_seq;

