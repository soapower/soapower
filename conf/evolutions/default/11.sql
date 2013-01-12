# --- !Ups

alter table service add column recordXmlData enum('true','false') NOT NULL DEFAULT 'true';
alter table environment add column recordXmlData enum('true','false') NOT NULL DEFAULT 'true';

# --- !Downs

alter table service drop column recordXmlData;
alter table environment drop column recordXmlData;
