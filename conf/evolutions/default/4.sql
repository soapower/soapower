
# --- !Ups

alter table service drop column user;
alter table service drop column password;

# --- !Downs

alter table service add column user varchar(255) null;
alter table service add column password varchar(255) null;