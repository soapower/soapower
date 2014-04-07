# --- !Ups

alter table service add column typeRequest enum('soap','rest') null;
alter table service add column httpMethod enum('get','post','put','delete') null;

# --- !Downs

alter table service drop column typeRequest;
alter table service drop column httpMethod;