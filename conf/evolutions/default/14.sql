# --- !Ups

alter table request_data modify column soapAction varchar(255) not null;

# --- !Downs

alter table request_data modify column soapAction varchar(50) not null;
