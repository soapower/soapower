
# --- !Ups

alter table service add column timeoutms bigint;
update service set timeoutms = 30000;

# --- !Downs

alter table service drop column timeoutms;
