# --- !Ups

alter table request_data add column isMock enum('true','false') NOT NULL DEFAULT 'false';
alter table mock add column httpHeaders varchar(255) not null DEFAULT 'soapower -> mock';
alter table mock modify column httpHeaders LONGTEXT not null;

# --- !Downs

alter table request_data drop column isMock;
alter table mock drop column httpHeaders;
