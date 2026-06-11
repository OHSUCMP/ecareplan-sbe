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

insert into default_query (dataSetName, query, strategy) values ('ASSESSMENTS', 'QuestionnaireResponse?patient={PATIENT}&status=completed&_revinclude=Provenance:target', 'PATIENT');
insert into default_query (dataSetName, query, strategy) values ('CARE_PLANS', 'CarePlan?patient={PATIENT}&status=active&category=assess-plan&_revinclude=Provenance:target', 'PATIENT');
insert into default_query (dataSetName, query, strategy) values ('CARE_TEAMS', 'CareTeam?patient={PATIENT}&_include=CareTeam:participant&_revinclude=Provenance:target', 'PATIENT');
insert into default_query (dataSetName, query, strategy) values ('CLINICAL_NOTES', 'DocumentReference?patient={PATIENT}&category=clinical-note&date=ge{TWO_YEARS_AGO}&_revinclude=Provenance:target', 'PATIENT');
insert into default_query (dataSetName, query, strategy) values ('CONCERNS', 'Condition?patient={PATIENT}&category=problem-list-item&clinical-status=active&_revinclude=Provenance:target', 'PATIENT');
insert into default_query (dataSetName, query, strategy) values ('CONCERNS', 'Condition?patient={PATIENT}&category=health-concern&clinical-status=active&_revinclude=Provenance:target', 'PATIENT');
insert into default_query (dataSetName, query, strategy) values ('CONCERNS', 'Condition?patient={PATIENT}&category=encounter-diagnosis&clinical-status=active&_revinclude=Provenance:target', 'PATIENT');
insert into default_query (dataSetName, query, strategy) values ('DIAGNOSTIC_REPORTS', 'DiagnosticReport?patient={PATIENT}&date=ge{THREE_YEARS_AGO}&_revinclude=Provenance:target', 'PATIENT');
insert into default_query (dataSetName, query, strategy) values ('GOALS', 'Goal?patient={PATIENT}&lifecycle-status=active,completed,cancelled&_revinclude=Provenance:target', 'PATIENT');
insert into default_query (dataSetName, query, strategy) values ('IMMUNIZATIONS', 'Immunization?patient={PATIENT}&status=completed&_revinclude=Provenance:target', 'PATIENT');
insert into default_query (dataSetName, query, strategy) values ('INTERACTIONS', 'Encounter?patient={PATIENT}&date=ge{TWO_YEARS_AGO}&_revinclude=Provenance:target', 'PATIENT');
insert into default_query (dataSetName, query, strategy) values ('TESTS', 'Observation?patient={PATIENT}&category=laboratory&date=ge{THREE_YEARS_AGO}&_revinclude=Provenance:target', 'PATIENT');
insert into default_query (dataSetName, query, strategy) values ('TESTS', 'Observation?patient={PATIENT}&code=http://loinc.org|45066-8,http://loinc.org|48642-3,http://loinc.org|48643-1,http://loinc.org|50044-7,http://loinc.org|50210-4,http://loinc.org|50384-7,http://loinc.org|62238-1,http://loinc.org|69405-9,http://loinc.org|70969-1,http://loinc.org|77147-7,http://loinc.org|88293-6,http://loinc.org|88294-4,http://loinc.org|94677-2,http://loinc.org|98979-8,http://loinc.org|98980-6&date=ge{TEN_YEARS_AGO}&date=lt{THREE_YEARS_AGO}&_revinclude=Provenance:target', 'PATIENT');
insert into default_query (dataSetName, query, strategy) values ('MEDICATIONS', 'MedicationRequest?patient={PATIENT}&status=active&_include=MedicationRequest:requester&_revinclude=Provenance:target', 'PATIENT');
insert into default_query (dataSetName, query, strategy) values ('MEDICATIONS', 'MedicationRequest?patient={PATIENT}&status=on-hold,cancelled,completed,stopped&_count=10&_revinclude=Provenance:target', 'PATIENT');
insert into default_query (dataSetName, query, strategy) values ('PROCEDURES', 'Procedure?patient={PATIENT}&date=ge{THREE_YEARS_AGO}&_revinclude=Provenance:target', 'PATIENT');
insert into default_query (dataSetName, query, strategy) values ('SERVICE_REQUESTS', 'ServiceRequest?patient={PATIENT}&status=active&authored=ge{TWO_YEARS_AGO}&_include=ServiceRequest:requester&_revinclude=Provenance:target', 'PATIENT');
insert into default_query (dataSetName, query, strategy) values ('SOCIAL_HISTORIES', 'Observation?patient={PATIENT}&category=social-history&_revinclude=Provenance:target', 'PATIENT');
insert into default_query (dataSetName, query, strategy) values ('SURVEY_OBSERVATIONS', 'Observation?patient={PATIENT}&category=survey&_revinclude=Provenance:target', 'PATIENT');
insert into default_query (dataSetName, query, strategy) values ('VITALS', 'Observation?patient={PATIENT}&code=http://loinc.org|8867-4&date=ge{TWO_YEARS_AGO}&_count=10&_revinclude=Provenance:target', 'PATIENT');
insert into default_query (dataSetName, query, strategy) values ('VITALS', 'Observation?patient={PATIENT}&code=http://loinc.org|59408-5&date=ge{TWO_YEARS_AGO}&_count=10&_revinclude=Provenance:target', 'PATIENT');
insert into default_query (dataSetName, query, strategy) values ('VITALS', 'Observation?patient={PATIENT}&code=http://loinc.org|8310-5&date=ge{TWO_YEARS_AGO}&_count=10&_revinclude=Provenance:target', 'PATIENT');
insert into default_query (dataSetName, query, strategy) values ('VITALS', 'Observation?patient={PATIENT}&code=http://loinc.org|29463-7&date=ge{TWO_YEARS_AGO}&_count=10&_revinclude=Provenance:target', 'PATIENT');
insert into default_query (dataSetName, query, strategy) values ('VITALS', 'Observation?patient={PATIENT}&code=http://loinc.org|8302-2&date=ge{TWO_YEARS_AGO}&_count=10&_revinclude=Provenance:target', 'PATIENT');
insert into default_query (dataSetName, query, strategy) values ('VITALS', 'Observation?patient={PATIENT}&code=http://loinc.org|39156-5&date=ge{TWO_YEARS_AGO}&_count=10&_revinclude=Provenance:target', 'PATIENT');
insert into default_query (dataSetName, query, strategy) values ('VITALS', 'Observation?patient={PATIENT}&code=http://loinc.org|85354-9&date=ge{TWO_YEARS_AGO}&_revinclude=Provenance:target', 'PATIENT');
insert into default_query (dataSetName, query, strategy) values ('VITALS', 'Observation?patient={PATIENT}&code=http://loinc.org|72076-3&date=ge{TWO_YEARS_AGO}&_revinclude=Provenance:target', 'PATIENT');

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