
# --- !Ups

update service set timeoutms = 120000;

update environment set nbDayKeepAllData = 5;
# --- !Downs
