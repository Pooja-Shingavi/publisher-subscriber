create database cse586_lab2;
create table cse586_lab2.STATUS(ID int, STATUS int, SUBJECT varchar(100));
create table cse586_lab2.TOPICS(ID int, NAME varchar(100));
create table cse586_lab2.TOPIC_MESSAGE(ID int, TOPIC varchar(100), MESSAGE varchar(100));
insert into cse586_lab2.STATUS(ID,STATUS,SUBJECT) values(1,0,'none');