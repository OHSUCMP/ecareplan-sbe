create table user (
    id int not null auto_increment primary key,
    patIdHash char(64) unique not null
);

create table audit_data (
    id int not null auto_increment primary key,
    userId int not null references user(id),
    severity varchar(10) not null,
    event varchar(100) not null,
    details varchar(1000),
    created datetime not null default current_timestamp
);

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

create table vsac_valueset_concept (
    valueSetId int not null,
    conceptId int not null,
    constraint vvc_pk1 primary key (valueSetId, conceptId),
    constraint vvc_fk1 foreign key (valueSetId) references vsac_valueset (id)
       on delete cascade,
    constraint vvc_fk2 foreign key (conceptId) references vsac_concept (id)
       on delete cascade
);

create table default_query (
    id int not null auto_increment primary key,
    dataSetName varchar(50) not null,
    query varchar(1000) not null,
    strategy varchar(20) not null
);

insert into default_query (dataSetName, query, strategy) values ('assessments', 'QuestionnaireResponse?status=completed&_revinclude=Provenance:target', 'patient');
insert into default_query (dataSetName, query, strategy) values ('carePlans', 'CarePlan?status=active&category=assess-plan&_revinclude=Provenance:target', 'patient');
insert into default_query (dataSetName, query, strategy) values ('careTeams', 'CareTeam?_include=CareTeam:participant&_revinclude=Provenance:target', 'patient');
insert into default_query (dataSetName, query, strategy) values ('clinicalNotes', 'DocumentReference?category=clinical-note&date=ge{TWO_YEARS_AGO}&_revinclude=Provenance:target', 'patient');
insert into default_query (dataSetName, query, strategy) values ('concerns', 'Condition?category=problem-list-item&clinical-status=active&_revinclude=Provenance:target', 'patient');
insert into default_query (dataSetName, query, strategy) values ('concerns', 'Condition?category=health-concern&clinical-status=active&_revinclude=Provenance:target', 'patient');
insert into default_query (dataSetName, query, strategy) values ('concerns', 'Condition?category=encounter-diagnosis&clinical-status=active&_revinclude=Provenance:target', 'patient');
insert into default_query (dataSetName, query, strategy) values ('diagnosticReports', 'DiagnosticReport?date=ge{THREE_YEARS_AGO}&_revinclude=Provenance:target', 'patient');
insert into default_query (dataSetName, query, strategy) values ('goals', 'Goal?lifecycle-status=active,completed,cancelled&_revinclude=Provenance:target', 'patient');
insert into default_query (dataSetName, query, strategy) values ('immunizations', 'Immunization?status=completed&_revinclude=Provenance:target', 'patient');
insert into default_query (dataSetName, query, strategy) values ('interactions', 'Encounter?date=ge{TWO_YEARS_AGO}&_revinclude=Provenance:target', 'patient');
insert into default_query (dataSetName, query, strategy) values ('tests', 'Observation?category=laboratory&date=ge{THREE_YEARS_AGO}&_revinclude=Provenance:target', 'patient');
insert into default_query (dataSetName, query, strategy) values ('tests', 'Observation?code=http://loinc.org|45066-8,http://loinc.org|48642-3,http://loinc.org|48643-1,http://loinc.org|50044-7,http://loinc.org|50210-4,http://loinc.org|50384-7,http://loinc.org|62238-1,http://loinc.org|69405-9,http://loinc.org|70969-1,http://loinc.org|77147-7,http://loinc.org|88293-6,http://loinc.org|88294-4,http://loinc.org|94677-2,http://loinc.org|98979-8,http://loinc.org|98980-6&date=ge{TEN_YEARS_AGO}&date=lt{THREE_YEARS_AGO}&_revinclude=Provenance:target', 'patient');
insert into default_query (dataSetName, query, strategy) values ('medications', 'MedicationRequest?status=active&_include=MedicationRequest:requester&_revinclude=Provenance:target', 'patient');
insert into default_query (dataSetName, query, strategy) values ('medications', 'MedicationRequest?status=on-hold,cancelled,completed,stopped&_count=10&_revinclude=Provenance:target', 'patient');
insert into default_query (dataSetName, query, strategy) values ('procedures', 'Procedure?date=ge{THREE_YEARS_AGO}&_revinclude=Provenance:target', 'patient');
insert into default_query (dataSetName, query, strategy) values ('serviceRequests', 'ServiceRequest?status=active&authored=ge{TWO_YEARS_AGO}&_include=ServiceRequest:requester&_revinclude=Provenance:target', 'patient');
insert into default_query (dataSetName, query, strategy) values ('socialHistories', 'Observation?category=social-history&_revinclude=Provenance:target', 'patient');
insert into default_query (dataSetName, query, strategy) values ('surveyObservations', 'Observation?category=survey&_revinclude=Provenance:target', 'patient');
insert into default_query (dataSetName, query, strategy) values ('vitals', 'Observation?code=http://loinc.org|8867-4&date=ge{TWO_YEARS_AGO}&_count=10&_revinclude=Provenance:target', 'patient');
insert into default_query (dataSetName, query, strategy) values ('vitals', 'Observation?code=http://loinc.org|59408-5&date=ge{TWO_YEARS_AGO}&_count=10&_revinclude=Provenance:target', 'patient');
insert into default_query (dataSetName, query, strategy) values ('vitals', 'Observation?code=http://loinc.org|8310-5&date=ge{TWO_YEARS_AGO}&_count=10&_revinclude=Provenance:target', 'patient');
insert into default_query (dataSetName, query, strategy) values ('vitals', 'Observation?code=http://loinc.org|29463-7&date=ge{TWO_YEARS_AGO}&_count=10&_revinclude=Provenance:target', 'patient');
insert into default_query (dataSetName, query, strategy) values ('vitals', 'Observation?code=http://loinc.org|8302-2&date=ge{TWO_YEARS_AGO}&_count=10&_revinclude=Provenance:target', 'patient');
insert into default_query (dataSetName, query, strategy) values ('vitals', 'Observation?code=http://loinc.org|39156-5&date=ge{TWO_YEARS_AGO}&_count=10&_revinclude=Provenance:target', 'patient');
insert into default_query (dataSetName, query, strategy) values ('vitals', 'Observation?code=http://loinc.org|85354-9&date=ge{TWO_YEARS_AGO}&_revinclude=Provenance:target', 'patient');
insert into default_query (dataSetName, query, strategy) values ('vitals', 'Observation?code=http://loinc.org|72076-3&date=ge{TWO_YEARS_AGO}&_revinclude=Provenance:target', 'patient');

create table endpoint (
    id int not null auto_increment primary key,
    name varchar(255) unique not null,
    iss varchar(255) not null,
    clientId varchar(255) not null,
    clientSecret varchar(255),
    redirectUri varchar(255) not null,
    scope varchar(1000) not null,
    providerType varchar(255)
);

insert into endpoint(name, iss, clientId, redirectUri, scope, providerType) values('Patient Launch', 'https://gw.interop.community/MCCOMPARE/data', 'GET_THIS_FROM_FHIR_APP_REGISTRATION', 'http://localhost:8088/patient/complete-handshake', 'launch/patient launch patient/*.read user/*.read openid profile', 'generic');

create table endpoint_query (
    id int not null auto_increment primary key,
    endpointId int not null references endpoint(id),
    dataSetName varchar(50) not null,
    query varchar(1000) not null,
    strategy varchar(20) not null,
    created datetime not null default current_timestamp
);

create table user_endpoint (
    id int not null auto_increment primary key,
    userId int not null references user(id),
    endpointId int not null references endpoint(id),
    fhirPatientId varchar(255),
    fhirUserId varchar(255),
    lastSync datetime,
    created datetime not null default current_timestamp
);