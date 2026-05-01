drop table if exists user;
create table user (
    id int not null auto_increment primary key,
    patIdHash char(64) unique not null
);

drop table if exists audit_data;
create table audit_data (
    id int not null auto_increment primary key,
    userId int not null references user(id),
    severity varchar(10) not null,
    event varchar(100) not null,
    details varchar(1000),
    created datetime not null default current_timestamp
);

drop table if exists vsac_valueset;
create table vsac_valueset (
    id int not null auto_increment primary key,
    oid varchar(255) not null,
    displayName varchar(255) not null,
    version varchar(255) not null,
    source varchar(255),
    purpose text,
    type varchar(50),
    binding varchar(50),
    status varchar(50),
    revisionDate date,
    created datetime not null default current_timestamp,
    updated datetime not null default current_timestamp on update current_timestamp,
    constraint vv_c1 unique (oid, version)
);

drop table if exists vsac_concept;
create table vsac_concept (
    id int not null auto_increment primary key,
    code varchar(255) not null,
    codeSystem varchar(255) not null,
    codeSystemName varchar(255) not null,
    codeSystemVersion varchar(255) not null,
    displayName varchar(255) not null,
    created datetime not null default current_timestamp,
    updated datetime not null default current_timestamp on update current_timestamp,
    constraint vc_c1 unique (code, codeSystem, codeSystemVersion)
);

drop table if exists vsac_valueset_concept;
create table vsac_valueset_concept (
    valueSetId int not null,
    conceptId int not null,
    constraint vvc_pk1 primary key (valueSetId, conceptId),
    constraint vvc_fk1 foreign key (valueSetId) references vsac_valueset (id)
       on delete cascade,
    constraint vvc_fk2 foreign key (conceptId) references vsac_concept (id)
       on delete cascade
);

drop table if exists data_set;
create table data_set (
    id int not null auto_increment primary key,
    name varchar(255) unique not null
);

drop table if exists default_fhir_query;
create table default_fhir_query (
    id int not null auto_increment primary key,
    dataSetId int not null references data_set(id),
    query varchar(1000) not null
);

drop table if exists fhir_endpoint;
create table fhir_endpoint (
    id int not null auto_increment primary key,
    name varchar(255) unique not null,
    iss varchar(255) unique not null,
    clientId varchar(255) not null,
    clientSecret varchar(255),
    redirectUri varchar(255) not null,
    scope varchar(1000) not null
);

drop table if exists endpoint_fhir_query;
create table endpoint_fhir_query (
    id int not null auto_increment primary key,
    fhirEndpointId int not null references fhir_endpoint(id),
    dataSetId int not null references data_set(id),
    query varchar(1000) not null,
    created datetime not null default current_timestamp
);