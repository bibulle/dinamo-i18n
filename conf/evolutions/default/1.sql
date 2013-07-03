# --- Created by Ebean DDL
# To stop Ebean DDL generation, remove this comment and start using Evolutions

# --- !Ups

create table property (
  id                        bigint auto_increment not null,
  key                       varchar(255),
  update_date               datetime,
  constraint uq_property_key unique (key),
  constraint pk_property primary key (id))
;

create table value (
  id                        bigint auto_increment not null,
  order_key                 integer,
  value                     varchar(255),
  update_date               datetime,
  temporary                 tinyint(1) default 0,
  property_id               bigint,
  constraint pk_value primary key (id))
;

alter table value add constraint fk_value_property_1 foreign key (property_id) references property (id) on delete restrict on update restrict;
create index ix_value_property_1 on value (property_id);



# --- !Downs

SET FOREIGN_KEY_CHECKS=0;

drop table property;

drop table value;

SET FOREIGN_KEY_CHECKS=1;

