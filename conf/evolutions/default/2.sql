
# --- !Ups

alter table service add column timeoutms bigint;
update service set timeoutms = 30000;

alter table service add column user varchar(255) null;
alter table service add column password varchar(255) null;

# --- !Downs

alter table service drop column timeoutms;
alter table service drop column user;
alter table service drop column password;
