# --- !Ups

alter table service add column recordData enum('true','false') NOT NULL DEFAULT 'true';
alter table environment add column recordData enum('true','false') NOT NULL DEFAULT 'true';

# --- !Downs

alter table service drop column recordData;
alter table environment drop column recordData;
