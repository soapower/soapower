# --- !Ups

ALTER TABLE request_data modify column request LONGTEXT  NULL;

ALTER TABLE  request_data modify column request LONGBLOB NULL;

ALTER TABLE  request_data modify column response LONGBLOB NULL;

# --- !Downs
