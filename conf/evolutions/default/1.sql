# --- Created by Ebean DDL
# To stop Ebean DDL generation, remove this comment and start using Evolutions

# --- !Ups

create table property (
  id                        bigint not null,
  akey                      varchar(255),
  update_date               timestamp,
  constraint uq_property_akey unique (akey),
  constraint pk_property primary key (id))
;

create table value (
  id                        bigint not null,
  order_key                 integer,
  value                     varchar(255),
  update_date               timestamp,
  temporary                 boolean,
  property_id               bigint,
  constraint pk_value primary key (id))
;

create sequence property_seq;

create sequence value_seq;

alter table value add constraint fk_value_property_1 foreign key (property_id) references property (id) on delete restrict on update restrict;
create index ix_value_property_1 on value (property_id);



# --- !Downs

SET REFERENTIAL_INTEGRITY FALSE;

drop table if exists property;

drop table if exists value;

SET REFERENTIAL_INTEGRITY TRUE;

drop sequence if exists property_seq;

drop sequence if exists value_seq;

