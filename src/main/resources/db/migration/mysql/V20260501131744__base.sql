create table user (
    id int not null auto_increment primary key,
    patIdHash char(64) unique not null,
    saltB64 varchar(64) not null,
    created datetime not null default current_timestamp
);

create table audit_data (
    id int not null auto_increment primary key,
    userId int not null,
    severity varchar(10) not null,
    event varchar(100) not null,
    details varchar(1000),
    created datetime not null default current_timestamp,
    constraint ad_fk1 foreign key (userId) references user (id)
        on delete restrict
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

create index idxOid on vsac_valueset(oid);

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

create index idxCode on vsac_concept(code);

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

insert into default_query (dataSetName, query, strategy) values ('CARE_PLANS', 'CarePlan?patient={PATIENT}&status=active&category=assess-plan&_revinclude=Provenance:target', 'PATIENT');
insert into default_query (dataSetName, query, strategy) values ('CARE_TEAMS', 'CareTeam?patient={PATIENT}&_include=CareTeam:participant&_revinclude=Provenance:target', 'PATIENT');
insert into default_query (dataSetName, query, strategy) values ('CLINICAL_NOTES', 'DocumentReference?patient={PATIENT}&category=clinical-note&date=ge{TWO_YEARS_AGO}&_revinclude=Provenance:target', 'PATIENT');
insert into default_query (dataSetName, query, strategy) values ('CONDITIONS', 'Condition?patient={PATIENT}&category=problem-list-item&clinical-status=active&_revinclude=Provenance:target', 'PATIENT');
insert into default_query (dataSetName, query, strategy) values ('CONDITIONS', 'Condition?patient={PATIENT}&category=health-concern&clinical-status=active&_revinclude=Provenance:target', 'PATIENT');
insert into default_query (dataSetName, query, strategy) values ('CONDITIONS', 'Condition?patient={PATIENT}&category=encounter-diagnosis&clinical-status=active&_revinclude=Provenance:target', 'PATIENT');
insert into default_query (dataSetName, query, strategy) values ('DIAGNOSTIC_REPORTS', 'DiagnosticReport?patient={PATIENT}&date=ge{THREE_YEARS_AGO}&_revinclude=Provenance:target', 'PATIENT');
insert into default_query (dataSetName, query, strategy) values ('GOALS', 'Goal?patient={PATIENT}&lifecycle-status=active,completed,cancelled&_revinclude=Provenance:target', 'PATIENT');
insert into default_query (dataSetName, query, strategy) values ('IMMUNIZATIONS', 'Immunization?patient={PATIENT}&status=completed&_revinclude=Provenance:target', 'PATIENT');
insert into default_query (dataSetName, query, strategy) values ('ENCOUNTERS', 'Encounter?patient={PATIENT}&date=ge{TWO_YEARS_AGO}&_revinclude=Provenance:target', 'PATIENT');
insert into default_query (dataSetName, query, strategy) values ('LAB_RESULTS', 'Observation?patient={PATIENT}&category=laboratory&date=ge{THREE_YEARS_AGO}&_revinclude=Provenance:target', 'PATIENT');
insert into default_query (dataSetName, query, strategy) values ('LAB_RESULTS', 'Observation?patient={PATIENT}&code=http://loinc.org|45066-8,http://loinc.org|48642-3,http://loinc.org|48643-1,http://loinc.org|50044-7,http://loinc.org|50210-4,http://loinc.org|50384-7,http://loinc.org|62238-1,http://loinc.org|69405-9,http://loinc.org|70969-1,http://loinc.org|77147-7,http://loinc.org|88293-6,http://loinc.org|88294-4,http://loinc.org|94677-2,http://loinc.org|98979-8,http://loinc.org|98980-6&date=ge{TEN_YEARS_AGO}&date=lt{THREE_YEARS_AGO}&_revinclude=Provenance:target', 'PATIENT');
insert into default_query (dataSetName, query, strategy) values ('MEDICATIONS', 'MedicationRequest?patient={PATIENT}&status=active&_include=MedicationRequest:requester&_revinclude=Provenance:target', 'PATIENT');
insert into default_query (dataSetName, query, strategy) values ('MEDICATIONS', 'MedicationRequest?patient={PATIENT}&status=on-hold,cancelled,completed,stopped&_total=10&_revinclude=Provenance:target', 'PATIENT');
insert into default_query (dataSetName, query, strategy) values ('PROCEDURES', 'Procedure?patient={PATIENT}&date=ge{THREE_YEARS_AGO}&_revinclude=Provenance:target', 'PATIENT');
insert into default_query (dataSetName, query, strategy) values ('QUESTIONNAIRE_RESPONSES', 'QuestionnaireResponse?patient={PATIENT}&status=completed&_revinclude=Provenance:target', 'PATIENT');
insert into default_query (dataSetName, query, strategy) values ('SERVICE_REQUESTS', 'ServiceRequest?patient={PATIENT}&status=active&authored=ge{TWO_YEARS_AGO}&_include=ServiceRequest:requester&_revinclude=Provenance:target', 'PATIENT');
insert into default_query (dataSetName, query, strategy) values ('SOCIAL_HISTORIES', 'Observation?patient={PATIENT}&category=social-history&_revinclude=Provenance:target', 'PATIENT');
insert into default_query (dataSetName, query, strategy) values ('SURVEY_OBSERVATIONS', 'Observation?patient={PATIENT}&category=survey&_revinclude=Provenance:target', 'PATIENT');
insert into default_query (dataSetName, query, strategy) values ('VITALS', 'Observation?patient={PATIENT}&code=http://loinc.org|8867-4&date=ge{TWO_YEARS_AGO}&_total=10&_revinclude=Provenance:target', 'PATIENT');
insert into default_query (dataSetName, query, strategy) values ('VITALS', 'Observation?patient={PATIENT}&code=http://loinc.org|59408-5&date=ge{TWO_YEARS_AGO}&_total=10&_revinclude=Provenance:target', 'PATIENT');
insert into default_query (dataSetName, query, strategy) values ('VITALS', 'Observation?patient={PATIENT}&code=http://loinc.org|8310-5&date=ge{TWO_YEARS_AGO}&_total=10&_revinclude=Provenance:target', 'PATIENT');
insert into default_query (dataSetName, query, strategy) values ('VITALS', 'Observation?patient={PATIENT}&code=http://loinc.org|29463-7&date=ge{TWO_YEARS_AGO}&_total=10&_revinclude=Provenance:target', 'PATIENT');
insert into default_query (dataSetName, query, strategy) values ('VITALS', 'Observation?patient={PATIENT}&code=http://loinc.org|8302-2&date=ge{TWO_YEARS_AGO}&_total=10&_revinclude=Provenance:target', 'PATIENT');
insert into default_query (dataSetName, query, strategy) values ('VITALS', 'Observation?patient={PATIENT}&code=http://loinc.org|39156-5&date=ge{TWO_YEARS_AGO}&_total=10&_revinclude=Provenance:target', 'PATIENT');
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
    endpointId int not null,
    dataSetName varchar(50) not null,
    query varchar(1000) not null,
    strategy varchar(20) not null,
    constraint eq_fk1 foreign key (endpointId) references endpoint (id)
        on delete cascade
);

create table user_endpoint (
    id int not null auto_increment primary key,
    userId int not null,
    endpointId int not null,
    encryptedPatientId varchar(1000),
    encryptedRefreshToken text,
    lastSyncCompleted datetime,
    created datetime not null default current_timestamp,
    constraint ue_fk1 foreign key (userId) references user (id)
        on delete restrict,
    constraint ue_fk2 foreign key (endpointId) references endpoint (id)
        on delete cascade
);

create unique index idxUserEndpoint on user_endpoint(userId, endpointId);

create table resource_categorization_valueset (
    id int not null auto_increment primary key,
    dataSetName varchar(50) not null,
    valuesetName varchar(255) not null,
    valuesetOid varchar(255) not null,
    category varchar(255),
    commonName varchar(255)
);

create unique index idxDataSetOid on resource_categorization_valueset(dataSetName, valuesetOid);

insert into resource_categorization_valueset (dataSetName, valuesetName, valuesetOid, category, commonName) values
    ('CONDITIONS', 'Arthritis Disorders', '2.16.840.1.113762.1.4.1222.81', 'Arthritis', 'Arthritis'),
    ('CONDITIONS', 'Infectious Arthritis', '2.16.840.1.113762.1.4.1222.654', 'Arthritis', 'Arthritis'),
    ('CONDITIONS', 'Osteoarthritis', '2.16.840.1.113762.1.4.1222.648', 'Arthritis', 'Osteoarthritis'),
    ('CONDITIONS', 'Psoriatic Arthritis', '2.16.840.1.113762.1.4.1222.587', 'Arthritis', 'Arthritis'),
    ('CONDITIONS', 'Reactive Arthritis', '2.16.840.1.113762.1.4.1222.588', 'Arthritis', 'Arthritis'),
    ('CONDITIONS', 'Rheumatoid Arthritis', '2.16.840.1.113762.1.4.1222.651', 'Arthritis', 'Rheumatoid Arthritis'),
    ('CONDITIONS', 'Acute Coronary Syndromes', '2.16.840.1.113883.3.3157.2000.10', 'Cardiovascular Disease', 'Heart Attack'),
    ('CONDITIONS', 'Acute Myocardial Infarction', '2.16.840.1.113883.3.666.5.3011', 'Cardiovascular Disease', 'Heart Attack'),
    ('CONDITIONS', 'American Heart Association Heart Failure Stage', '2.16.840.1.113762.1.4.1222.581', 'Cardiovascular Disease', 'Heart Failure Stage'),
    ('CONDITIONS', 'Aneurysm', '2.16.840.1.113762.1.4.1222.627', 'Cardiovascular Disease', 'Aneurysm'),
    ('CONDITIONS', 'Angina', '2.16.840.1.113762.1.4.1222.608', 'Cardiovascular Disease', 'Heart Pain'),
    ('CONDITIONS', 'Aortic Disease', '2.16.840.1.113762.1.4.1222.636', 'Cardiovascular Disease', 'Aortic Disease'),
    ('CONDITIONS', 'Arrhythmia', '2.16.840.1.113883.3.526.3.366', 'Cardiovascular Disease', 'Heart Rhythm Problem'),
    ('CONDITIONS', 'Atherosclerotic Cardiovascular Disease', '2.16.840.1.113762.1.4.1222.584', 'Cardiovascular Disease', 'Coronary Blockages'),
    ('CONDITIONS', 'Cardiomyopathy', '2.16.840.1.113762.1.4.1222.579', 'Cardiovascular Disease', 'Heart Structure Problem'),
    ('CONDITIONS', 'Carotid Stenosis', '2.16.840.1.113762.1.4.1222.639', 'Cardiovascular Disease', 'Carotid Blockage'),
    ('CONDITIONS', 'Cerebrovascular Disease', '2.16.840.1.113762.1.4.1222.1524', 'Cardiovascular Disease', 'Cerebrovascular Disease'),
    ('CONDITIONS', 'Congestive heart failure Diagnosis', '2.16.840.1.113883.3.526.3.369', 'Cardiovascular Disease', 'Heart Failure'),
    ('CONDITIONS', 'Coronary Artery Disease No MI', '2.16.840.1.113762.1.4.1222.1540', 'Cardiovascular Disease', 'Coronary Blockage'),
    ('CONDITIONS', 'Heart Failure', '2.16.840.1.113762.1.4.1222.1543', 'Cardiovascular Disease', 'Heart Failure'),
    ('CONDITIONS', 'Hypertension, Persistent', '2.16.840.1.113762.1.4.1222.1563', 'Cardiovascular Disease', 'High Blood Pressure'),
    ('CONDITIONS', 'Hypertension, Primary and Secondary', '2.16.840.1.113762.1.4.1222.571', 'Cardiovascular Disease', 'High Blood Pressure'),
    ('CONDITIONS', 'Hypertension Stage', '2.16.840.1.113762.1.4.1222.566', 'Cardiovascular Disease', 'High Blood Pressure'),
    ('CONDITIONS', 'Hypertension, Essential or Primary', '2.16.840.1.113762.1.4.1222.1484', 'Cardiovascular Disease', 'High Blood Pressure'),
    ('CONDITIONS', 'Hypertension, Pulmonary Hypertension', '2.16.840.1.113762.1.4.1222.569', 'Cardiovascular Disease', 'High Blood Pressure'),
    ('CONDITIONS', 'Hypertension, Secondary', '2.16.840.1.113762.1.4.1222.642', 'Cardiovascular Disease', 'High Blood Pressure'),
    ('CONDITIONS', 'Intracranial Stenosis', '2.16.840.1.113762.1.4.1222.614', 'Cardiovascular Disease', 'Heart Damage from Blockages'),
    ('CONDITIONS', 'Ischemic Heart Disease', '2.16.840.1.113762.1.4.1222.615', 'Cardiovascular Disease', 'Thickened Left Heart'),
    ('CONDITIONS', 'Left Ventricular Hypertrophy', '2.16.840.1.113762.1.4.1222.580', 'Cardiovascular Disease', 'Heart Functional Level'),
    ('CONDITIONS', 'NYHA Heart Failure Functional Classifications', '2.16.840.1.113762.1.4.1222.27', 'Cardiovascular Disease', 'Blood Vessel Blockages away from Heart'),
    ('CONDITIONS', 'Peripheral Vascular Disease', '2.16.840.1.113762.1.4.1222.1488', 'Cardiovascular Disease', 'Fast Heart Rate due to Standing'),
    ('CONDITIONS', 'Postural tachycardia syndrome (POTS) Diagnosis', '2.16.840.1.113762.1.4.1222.645', 'Cardiovascular Disease', 'Fluid Leakage in Lung'),
    ('CONDITIONS', 'Pulmonary Edema', '2.16.840.1.113762.1.4.1222.611', 'Cardiovascular Disease', 'High Risk Heart Pains'),
    ('CONDITIONS', 'Stroke History', '2.16.840.1.113883.3.464.1003.104.12.1017', 'Cardiovascular Disease', 'Heart Valve Disease'),
    ('CONDITIONS', 'Acute Renal Failure', '2.16.840.1.113762.1.4.1222.1534', 'Chronic Kidney Disease', 'Kidney Attack'),
    ('CONDITIONS', 'Chronic Kidney Disease All Stages', '2.16.840.1.113762.1.4.1222.157', 'Chronic Kidney Disease', 'Chronic Kidney Disease'),
    ('CONDITIONS', 'Chronic Kidney Disease Type or Cause', '2.16.840.1.113762.1.4.1222.6', 'Chronic Kidney Disease', 'Chronic Kidney Disease'),
    ('CONDITIONS', 'Acanthosis Nigricans', '2.16.840.1.113762.1.4.1222.556', 'Diabetes Conditions', 'Thickening/Darkening of Skin from Diabetes'),
    ('CONDITIONS', 'Amputated Limb (Not Traumatic)', '2.16.840.1.113762.1.4.1222.563', 'Diabetes Conditions', 'Amputated Limb (not due to injury)'),
    ('CONDITIONS', 'Blindness', '2.16.840.1.113883.3.464.1003.115.12.1089', 'Diabetes Conditions', 'Blindness'),
    ('CONDITIONS', 'Cataract of the eye', '2.16.840.1.113762.1.4.1222.498', 'Diabetes Conditions', 'Cataracts'),
    ('CONDITIONS', 'Cellulitis', '2.16.840.1.113762.1.4.1222.533', 'Diabetes Conditions', 'Blindness'),
    ('CONDITIONS', 'Charcot Foot', '2.16.840.1.113762.1.4.1222.531', 'Diabetes Conditions', 'Foot numbness'),
    ('CONDITIONS', 'Complication due to Diabetes Mellitus', '2.16.840.1.113762.1.4.1222.1537', 'Diabetes Conditions', 'Complication of Diabetes'),
    ('CONDITIONS', 'Diabetic Coma', '2.16.840.1.113762.1.4.1222.482', 'Diabetes Conditions', 'Coma from Diabetes'),
    ('CONDITIONS', 'Diabetic Foot', '2.16.840.1.113762.1.4.1222.523', 'Diabetes Conditions', 'Foot Problems from Diabetes'),
    ('CONDITIONS', 'Diabetic Foot Ulcer', '2.16.840.1.113762.1.4.1222.526', 'Diabetes Conditions', 'Foot Sore from Diabetes'),
    ('CONDITIONS', 'Diabetic hand/ Diabetic cheiroarthropathy', '2.16.840.1.113762.1.4.1222.535', 'Diabetes Conditions', 'Hand Problems from Diabetes'),
    ('CONDITIONS', 'Diabetic Neuropathy Conditions', '2.16.840.1.113762.1.4.1222.33', 'Diabetes Conditions', 'Nerve Damage from Diabetes'),
    ('CONDITIONS', 'Diabetic Peripheral Angiopathy', '2.16.840.1.113762.1.4.1222.492', 'Diabetes Conditions', 'Artery Damage from Diabetes'),
    ('CONDITIONS', 'Diabetic Retinopathy', '2.16.840.1.113883.3.526.3.327', 'Diabetes Conditions', 'Vision Loss from Diabetes'),
    ('CONDITIONS', 'Dupuytren''s Contracture', '2.16.840.1.113762.1.4.1222.546', 'Diabetes Conditions', 'Finger Stiffening'),
    ('CONDITIONS', 'Emotional Distress caused by Chronic Condition', '2.16.840.1.113762.1.4.1222.508', 'Diabetes Conditions', 'Stress from Chronic Illness'),
    ('CONDITIONS', 'Eruptive Xanthomatosis', '2.16.840.1.113762.1.4.1222.557', 'Diabetes Conditions', 'Skin Bumps from Cholesterol'),
    ('CONDITIONS', 'Gangrene', '2.16.840.1.113762.1.4.1222.543', 'Diabetes Conditions', 'Gangrene'),
    ('CONDITIONS', 'Gestational Diabetes', '2.16.840.1.113762.1.4.1032.90', 'Diabetes Conditions', 'Diabetes in Pregnancy'),
    ('CONDITIONS', 'Hyperglycemic Hyperosmolar Nonketotic Syndrome HHNS', '2.16.840.1.113762.1.4.1222.517', 'Diabetes Conditions', 'Coma from Diabetes'),
    ('CONDITIONS', 'Hypoglycemia unawareness', '2.16.840.1.113762.1.4.1222.514', 'Diabetes Conditions', 'Drowsiness from Low Blood Sugar'),
    ('CONDITIONS', 'Hypoglycemic event', '2.16.840.1.113762.1.4.1222.513', 'Diabetes Conditions', 'Low Blood Sugar Event'),
    ('CONDITIONS', 'Ketoacidosis', '2.16.840.1.113762.1.4.1222.520', 'Diabetes Conditions', 'High Ketones from Low Blood pH'),
    ('CONDITIONS', 'Maturity Onset Diabetes of the Young (MODY)', '2.16.840.1.113762.1.4.1222.420', 'Diabetes Conditions', 'Diabetes'),
    ('CONDITIONS', 'Necrobiosis Lipoidica (Skin Lesions)', '2.16.840.1.113762.1.4.1222.540', 'Diabetes Conditions', 'Skin Injury from Small Artery Disease'),
    ('CONDITIONS', 'Periodontitis', '2.16.840.1.113762.1.4.1222.560', 'Diabetes Conditions', 'Inflammation around the Teeth'),
    ('CONDITIONS', 'Prediabetes (borderline diabetes)', '2.16.840.1.113762.1.4.1222.419', 'Diabetes Conditions', 'Prediabetes'),
    ('CONDITIONS', 'Rubeosis Iridis', '2.16.840.1.113762.1.4.1222.501', 'Diabetes Conditions', 'Iris Reddening from Diabetes'),
    ('CONDITIONS', 'Scleroderma or Thick Skin Syndrome', '2.16.840.1.113762.1.4.1222.549', 'Diabetes Conditions', 'Skin Thickening'),
    ('CONDITIONS', 'Type 1 Diabetes', '2.16.840.1.113883.3.464.1003.103.12.1020', 'Diabetes Conditions', 'Autoimmune Diabetes'),
    ('CONDITIONS', 'Type II Diabetes', '2.16.840.1.113883.3.464.1003.103.12.1021', 'Diabetes Conditions', 'Diabetes'),
    ('CONDITIONS', 'Anxiety', '2.16.840.1.113762.1.4.1032.52', 'Mental Health', 'Anxiety'),
    ('CONDITIONS', 'Bipolar Diagnosis', '2.16.840.1.113883.3.600.450', 'Mental Health', 'Bipolar'),
    ('CONDITIONS', 'Depression Diagnosis', '2.16.840.1.113883.3.600.145', 'Mental Health', 'Depression'),
    ('CONDITIONS', 'Dysthymia', '2.16.840.1.113883.3.67.1.101.1.254', 'Mental Health', 'Depression'),
    ('CONDITIONS', 'Experience of Traumatic Events', '2.16.840.1.113762.1.4.1222.590', 'Mental Health', 'Trauma'),
    ('CONDITIONS', 'Grief or Loss', '2.16.840.1.113762.1.4.1222.690', 'Mental Health', 'Grief'),
    ('CONDITIONS', 'Major Depression', '2.16.840.1.113883.3.464.1003.105.12.1007', 'Mental Health', 'Depression'),
    ('CONDITIONS', 'Post Partum Depression', '2.16.840.1.113762.1.4.1222.681', 'Mental Health', 'Depression after Pregnancy'),
    ('CONDITIONS', 'PostTraumatic Stress Disorder PTSD', '2.16.840.1.113762.1.4.1222.103', 'Mental Health', 'PTSD'),
    ('CONDITIONS', 'Psychological Trauma', '2.16.840.1.113762.1.4.1222.687', 'Mental Health', 'Trauma'),
    ('CONDITIONS', 'Psychotic Depression', '2.16.840.1.113762.1.4.1222.678', 'Mental Health', 'Depression'),
    ('CONDITIONS', 'Seasonal Affective Disorder', '2.16.840.1.113762.1.4.1222.684', 'Mental Health', 'Depression'),
    ('CONDITIONS', 'Suicide Risk', '2.16.840.1.113762.1.4.1222.693', 'Mental Health', 'Risk of Suicide'),
    ('CONDITIONS', 'Alkaline Phosphatase Deficiency Conditions', '2.16.840.1.113762.1.4.1222.93', 'Nutrition / Metabolic Conditions', 'Low Alkaline Phosphatase'),
    ('CONDITIONS', 'Anemia Conditions', '2.16.840.1.113762.1.4.1222.53', 'Nutrition / Metabolic Conditions', 'Anemia'),
    ('CONDITIONS', 'Familial Hypercholesterolemia', '2.16.840.1.113762.1.4.1047.100', 'Nutrition / Metabolic Conditions', 'Genetic High Cholesterol'),
    ('CONDITIONS', 'Gout', '2.16.840.1.113762.1.4.1222.586', 'Nutrition / Metabolic Conditions', 'Gout'),
    ('CONDITIONS', 'Hypercalcemia Conditions', '2.16.840.1.113762.1.4.1222.60', 'Nutrition / Metabolic Conditions', 'High Calcium'),
    ('CONDITIONS', 'Hyperkalemia Conditions', '2.16.840.1.113762.1.4.1222.50', 'Nutrition / Metabolic Conditions', 'High Potassium'),
    ('CONDITIONS', 'Hyperlipidemia Conditions', '2.16.840.1.113762.1.4.1222.73', 'Nutrition / Metabolic Conditions', 'High Blood Fats'),
    ('CONDITIONS', 'Hyperphosphatemia Conditions', '2.16.840.1.113762.1.4.1222.66', 'Nutrition / Metabolic Conditions', 'High Phosphates in Blood'),
    ('CONDITIONS', 'Hypertriglyceridemia', '2.16.840.1.113762.1.4.1222.742', 'Nutrition / Metabolic Conditions', 'High Triglycerides'),
    ('CONDITIONS', 'Hyperuricemia Conditions', '2.16.840.1.113762.1.4.1222.85', 'Nutrition / Metabolic Conditions', 'High Uric Acid in Blood'),
    ('CONDITIONS', 'Hypoalbuminemia Conditions', '2.16.840.1.113762.1.4.1222.47', 'Nutrition / Metabolic Conditions', 'Low Albumin'),
    ('CONDITIONS', 'Hypocalcemia Conditions', '2.16.840.1.113762.1.4.1222.61', 'Nutrition / Metabolic Conditions', 'Low Calcium'),
    ('CONDITIONS', 'Malnutrition Diagnosis', '2.16.840.1.113762.1.4.1222.1517', 'Nutrition / Metabolic Conditions', 'Malnutrition'),
    ('CONDITIONS', 'Metabolic Acidosis Conditions', '2.16.840.1.113762.1.4.1222.70', 'Nutrition / Metabolic Conditions', 'Low Blood pH due to Kidney Problems'),
    ('CONDITIONS', 'Metabolic syndrome', '2.16.840.1.113762.1.4.1222.1561', 'Nutrition / Metabolic Conditions', 'Diabetes'),
    ('CONDITIONS', 'Obesity Conditions', '2.16.840.1.113762.1.4.1222.36', 'Nutrition / Metabolic Conditions', 'High BMI'),
    ('CONDITIONS', 'Vitamin D Deficiency Conditions', '2.16.840.1.113762.1.4.1222.90', 'Nutrition / Metabolic Conditions', 'Low Vitamin D'),
    ('CONDITIONS', 'Chronic Pain Conditions', '2.16.840.1.113762.1.4.1222.76', 'Pain Conditions', 'Chronic Pain'),
    ('CONDITIONS', 'Low Back Pain', '2.16.840.1.113762.1.4.1222.1558', 'Pain Conditions', 'Low Back Pain'),
    ('CONDITIONS', 'Migraine', '2.16.840.1.113762.1.4.1222.1552', 'Pain Conditions', 'Migraine'),
    ('CONDITIONS', 'Chronic Tension-type Headache diagnosis', '2.16.840.1.113762.1.4.1222.1555', 'Pain Conditions', 'Chronic Headaches'),
    ('CONDITIONS', 'Neuropathic Pain (Neuralgia)', '2.16.840.1.113762.1.4.1222.663', 'Pain Conditions', 'Nerve Pain'),
    ('CONDITIONS', 'Phantom Pain', '2.16.840.1.113762.1.4.1222.669', 'Pain Conditions', 'Phantom Pain'),
    ('CONDITIONS', 'Asthma Diagnosis', '2.16.840.1.113762.1.4.1222.1472', 'Respiratory Conditions', 'Asthma'),
    ('CONDITIONS', 'Bronchiectasis Diagnosis', '2.16.840.1.113762.1.4.1222.1463', 'Respiratory Conditions', 'Lung Damage'),
    ('CONDITIONS', 'Chronic obstructive pulmonary disease (COPD) Diagnosis', '2.16.840.1.113762.1.4.1222.1466', 'Respiratory Conditions', 'COPD'),
    ('CONDITIONS', 'Interstitial lung disease Diagnosis', '2.16.840.1.113762.1.4.1222.1469', 'Respiratory Conditions', 'Interstitial Lung Damage'),
    ('CONDITIONS', 'Pulmonary embolism Diagnosis', '2.16.840.1.113762.1.4.1222.1481', 'Respiratory Conditions', 'Lung Clot'),
    ('CONDITIONS', 'Sleep Apnea Disorders', '2.16.840.1.113762.1.4.1222.102', 'Respiratory Conditions', 'Sleep Apnea'),
    ('CONDITIONS', 'Social Determinants of Health', '2.16.840.1.113762.1.4.1196.788', 'Other Health Risk Factors', 'Social Determinants of Health');

insert into resource_categorization_valueset (dataSetName, valuesetName, valuesetOid, commonName) values
    ('LAB_RESULTS', '24 Hour Urine Protein Excretion', '2.16.840.1.113762.1.4.1222.792', '24 Hour Urine Protein Excretion'),
    ('LAB_RESULTS', '24 Hour Urine Volume', '2.16.840.1.113762.1.4.1222.791', '24 Hour Urine Volume'),
    ('LAB_RESULTS', 'Alanine Aminotransferase (ALT), Bld/Ser/Plas', '2.16.840.1.113883.3.3616.200.110.102.5001', 'ALT'),
    ('LAB_RESULTS', 'Albumin in Blood, Plasma, or Serum', '2.16.840.1.113762.1.4.1222.151', 'Albumin'),
    ('LAB_RESULTS', 'Aldosterone/Renin Ratio', '2.16.840.1.113762.1.4.1222.811', 'Aldosterone/Renin Ratio'),
    ('LAB_RESULTS', 'Alkaline Phosphatase (Alp) in Blood, Serum or Plasma', '2.16.840.1.113762.1.4.1222.805', 'ALP'),
    ('LAB_RESULTS', 'Anion Gap', '2.16.840.1.113762.1.4.1222.153', 'Anion Gap'),
    ('LAB_RESULTS', 'Arterial Blood Gas (ABG)', '2.16.840.1.113762.1.4.1222.1575', 'Arterial Blood Gas'),
    ('LAB_RESULTS', 'Aspartate Aminotransferase (AST), Ser/Plas', '2.16.840.1.113883.3.3616.200.110.102.5006', 'AST'),
    ('LAB_RESULTS', 'Aspartate Transaminase or Alanine Aminotransferase Ratio', '2.16.840.1.113762.1.4.1222.804', 'Aspartate Transaminase or Alanine Aminotransferase Ratio'),
    ('LAB_RESULTS', 'B Type Natriuretic Peptide [Bnp] in Blood, Serum or Plasma', '2.16.840.1.113762.1.4.1222.795', 'B Type Natriuretic Peptide [Bnp'),
    ('LAB_RESULTS', 'Bicarbonate in blood, serum or plasma', '2.16.840.1.113762.1.4.1222.130', 'Bicarbonate'),
    ('LAB_RESULTS', 'Bilirubin, Total, Bld/Ser/Plas', '2.16.840.1.113883.3.3616.200.110.102.5007', 'Bilirubin'),
    ('LAB_RESULTS', 'Blood Ethanol Level', '2.16.840.1.113762.1.4.1222.813', 'Blood Ethanol Level'),
    ('LAB_RESULTS', 'Blood Urea Nitrogen', '2.16.840.1.113762.1.4.1222.113', 'Blood Urea Nitrogen'),
    ('LAB_RESULTS', 'Bone Biopsy Report', '2.16.840.1.113762.1.4.1222.870', 'Bone Biopsy Report'),
    ('LAB_RESULTS', 'C reactive Protein (CRP), Bld/Ser/Plas', '2.16.840.1.113883.3.3616.200.110.102.5015', 'C reactive Protein (CRP)'),
    ('LAB_RESULTS', 'Calcium (Not Corrected for Serum Albumin) in Blood, Plasms, or Serum', '2.16.840.1.113762.1.4.1222.794', 'Calcium (Not Corrected for Serum Albumin)'),
    ('LAB_RESULTS', 'Cerebral Spinal Fluid (CSF) Analysis', '2.16.840.1.113762.1.4.1222.1573', 'Cerebral Spinal Fluid (CSF) Analysis'),
    ('LAB_RESULTS', 'Chloride in blood, serum or plasma', '2.16.840.1.113762.1.4.1222.132', 'Chloride'),
    ('LAB_RESULTS', 'Coagulation Assay (PT, aPTT, Fibrinogen)', '2.16.840.1.113762.1.4.1222.1576', 'Coagulation Assay'),
    ('LAB_RESULTS', 'Complete Blood Count (with Diff)', '1.3.6.1.4.1.6997.4.1.2.271.13.38167.1.1.999.594', 'CBC'),
    ('LAB_RESULTS', 'Comprehensive Metabolic Panel (CMP)', '2.16.840.1.113762.1.4.1222.1574', 'CMP'),
    ('LAB_RESULTS', 'Corrected Calcium', '2.16.840.1.113762.1.4.1222.122', 'Calcium'),
    ('LAB_RESULTS', 'COVID 19 (SARS CoV 2), SARS CoV, Influenza virus A and B Tests', '2.16.840.1.113762.1.4.1222.1580', 'COVID 19 Test'),
    ('LAB_RESULTS', 'Creatine Kinase (CK, CK MB) in Blood, Serum, or Plasma', '2.16.840.1.113762.1.4.1222.1579', 'Creatine Kinase (CK, CK MB)'),
    ('LAB_RESULTS', 'Creatinine in blood, serum or plasma', '2.16.840.1.113762.1.4.1222.111', 'Creatinine'),
    ('LAB_RESULTS', 'Cystatin C', '2.16.840.1.113762.1.4.1222.138', 'Cystatin C'),
    ('LAB_RESULTS', 'D Dimer Test', '2.16.840.1.113762.1.4.1222.1577', 'D Dimer'),
    ('LAB_RESULTS', 'Drugs of Abuse Screen', '2.16.840.1.113762.1.4.1222.1597', 'Drugs of Abuse Screen'),
    ('LAB_RESULTS', 'Erythrocyte Distribution Width', '2.16.840.1.113762.1.4.1222.147', 'Erythrocyte Distribution Width'),
    ('LAB_RESULTS', 'Erythrocyte Sedimentation Rate (ESR), Blood', '2.16.840.1.113883.3.3616.200.110.102.5019', 'Erythrocyte Sedimentation Rate (ESR)'),
    ('LAB_RESULTS', 'Estimated Average Glucose', '2.16.840.1.113762.1.4.1222.150', 'Estimated Average Glucose'),
    ('LAB_RESULTS', 'Estimated Glomerular Filtration Rate (eGFR)', '2.16.840.1.113762.1.4.1222.179', 'eGFR'),
    ('LAB_RESULTS', 'Estimated Glomerular Filtration Rate NKF', '2.16.840.1.113883.3.6929.3.1000', 'eGFR'),
    ('LAB_RESULTS', 'Fasting Blood Glucose', '2.16.840.1.113762.1.4.1222.808', 'Fasting Blood Glucose'),
    ('LAB_RESULTS', 'Ferritin', '2.16.840.1.113762.1.4.1222.140', 'Ferritin'),
    ('LAB_RESULTS', 'Free T4 (Thyroxine) Test', '2.16.840.1.113883.3.7643.2.1019', 'Free T4 (Thyroxine)'),
    ('LAB_RESULTS', 'Gamma Glutamyl Transpeptidase (Ggt) in blood, serum or plasma', '2.16.840.1.113762.1.4.1222.806', 'Gamma Glutamyl Transpeptidase (Ggt)'),
    ('LAB_RESULTS', 'Gastric Tissue Biopsy Report', '2.16.840.1.113762.1.4.1222.869', 'Gastric Tissue Biopsy Report'),
    ('LAB_RESULTS', 'Glucose Tolerance Test Results', '2.16.840.1.113762.1.4.1032.94', 'Glucose Tolerance'),
    ('LAB_RESULTS', 'Hematocrit', '2.16.840.1.113762.1.4.1222.143', 'Hematocrit'),
    ('LAB_RESULTS', 'Hemoglobin', '2.16.840.1.113762.1.4.1222.114', 'Hemoglobin'),
    ('LAB_RESULTS', 'Hemoglobin A1C', '2.16.840.1.113762.1.4.1222.119', 'Hemoglobin A1c %'),
    ('LAB_RESULTS', 'High Density Lipoprotein', '2.16.840.1.113762.1.4.1222.135', 'HDL Cholesterol'),
    ('LAB_RESULTS', 'High Sensitivity Troponin', '2.16.840.1.113762.1.4.1222.801', 'High Sensitivity Troponin'),
    ('LAB_RESULTS', 'INR', '2.16.840.1.113883.3.117.1.7.1.213', 'INR'),
    ('LAB_RESULTS', 'Intact Parathyroid Hormone', '2.16.840.1.113762.1.4.1222.129', 'Intact Parathyroid Hormone'),
    ('LAB_RESULTS', 'Iron Saturation (Transferrin Saturation/TSAT)', '2.16.840.1.113762.1.4.1222.118', 'Iron Saturation'),
    ('LAB_RESULTS', 'Kidney Biopsy Report', '2.16.840.1.113762.1.4.1222.864', 'Kidney Biopsy Report'),
    ('LAB_RESULTS', 'KT/V Hemodialysis Ratio', '2.16.840.1.113762.1.4.1222.128', 'KT/V Hemodialysis Ratio'),
    ('LAB_RESULTS', 'Low Density Lipoprotein', '2.16.840.1.113762.1.4.1222.1568', 'LDL Cholesterol'),
    ('LAB_RESULTS', 'Mean Corpuscular Hemoglobin Concentration', '2.16.840.1.113762.1.4.1222.145', 'Mean Corpuscular Hemoglobin Concentration'),
    ('LAB_RESULTS', 'Mean Corpuscular Volume', '2.16.840.1.113762.1.4.1222.144', 'Mean Corpuscular Volume'),
    ('LAB_RESULTS', 'Microorganisms Detection by Blood Culture', '2.16.840.1.113762.1.4.1222.1581', 'Microorganisms Detection by Blood Culture'),
    ('LAB_RESULTS', 'Microorganisms Detection by Sputum Culture', '2.16.840.1.113762.1.4.1222.1582', 'Microorganisms Detection by Sputum Culture'),
    ('LAB_RESULTS', 'Muscle Biopsy Report', '2.16.840.1.113762.1.4.1222.868', 'Muscle Biopsy Report'),
    ('LAB_RESULTS', 'N Terminal Pro_B_Type Natriuretic Peptide [Nt_Probnp] in blood, serum or plasma', '2.16.840.1.113762.1.4.1222.797', 'N Terminal Pro_B_Type Natriuretic Peptide'),
    ('LAB_RESULTS', 'Oxygen Saturation, Blood', '2.16.840.1.113883.3.3616.200.110.102.5033', 'Oxygen Saturation'),
    ('LAB_RESULTS', 'Phosphorus in blood, serum or plasma', '2.16.840.1.113762.1.4.1222.123', 'Phosphorus'),
    ('LAB_RESULTS', 'Platelet Count', '2.16.840.1.113762.1.4.1222.146', 'Platelet Count'),
    ('LAB_RESULTS', 'Platelet Distribution Width', '2.16.840.1.113762.1.4.1222.148', 'Platelet Distribution Width'),
    ('LAB_RESULTS', 'Platelet Mean Volume', '2.16.840.1.113762.1.4.1222.149', 'Platelet Mean Volume'),
    ('LAB_RESULTS', 'Potassium in blood, serum or plasma', '2.16.840.1.113762.1.4.1222.120', 'Potassium'),
    ('LAB_RESULTS', 'Procalcitonin in Blood, Serum, or Plasma', '2.16.840.1.113762.1.4.1222.1578', 'Procalcitonin'),
    ('LAB_RESULTS', 'Random Blood Glucose Test', '2.16.840.1.113762.1.4.1222.809', 'Random Blood Glucose Test'),
    ('LAB_RESULTS', 'Red Blood Cell Count (Erythrocytes)', '2.16.840.1.113762.1.4.1222.141', 'Red Blood Cell Count (Erythrocytes)'),
    ('LAB_RESULTS', 'Serum Rheumatoid Factor', '2.16.840.1.113762.1.4.1222.812', 'Serum Rheumatoid Factor'),
    ('LAB_RESULTS', 'Skin Biopsy Report', '2.16.840.1.113762.1.4.1222.867', 'Skin Biopsy Report'),
    ('LAB_RESULTS', 'Sodium in blood, serum or plasma', '2.16.840.1.113762.1.4.1222.131', 'Sodium'),
    ('LAB_RESULTS', 'Thyroid Stimulating Hormone (TSH) Test', '2.16.840.1.113883.3.7643.3.1024', 'Thyroid Stimulating Hormone (TSH)'),
    ('LAB_RESULTS', 'Total Carbon Dioxide or Bicarbonate in blood, serum or plasma', '2.16.840.1.113762.1.4.1222.793', 'Total Carbon Dioxide or Bicarbonate'),
    ('LAB_RESULTS', 'Total Cholesterol', '2.16.840.1.113762.1.4.1222.139', 'Total Cholesterol'),
    ('LAB_RESULTS', 'Triglyceride in blood, serum or plasma', '2.16.840.1.113762.1.4.1222.137', 'Triglycerides'),
    ('LAB_RESULTS', 'Triiodothyronine in serum or plasma', '2.16.840.1.113762.1.4.1222.807', 'Triiodothyronine'),
    ('LAB_RESULTS', 'Troponin I, Bld/Ser/Plas', '2.16.840.1.113883.3.3616.200.110.102.5061', 'Troponin I'),
    ('LAB_RESULTS', 'Troponin T, Bld/Ser/Pla', '2.16.840.1.113883.3.3616.200.110.102.5062', 'Troponin T'),
    ('LAB_RESULTS', 'Urea Reduction Ratio', '2.16.840.1.113762.1.4.1222.819', 'Urea Reduction Ratio'),
    ('LAB_RESULTS', 'Uric Acid (Urate) in blood, serum or plasma', '2.16.840.1.113762.1.4.1222.810', 'Uric Acid (Urate)'),
    ('LAB_RESULTS', 'Urine Albumin Creatinine Ratio', '2.16.840.1.113883.3.6929.2.1002', 'Urine Albumin Creatinine Ratio'),
    ('LAB_RESULTS', 'Urine Protein to Creatinine Ratio (UPCR)', '2.16.840.1.113762.1.4.1222.790', 'Urine Protein to Creatinine Ratio (UPCR)'),
    ('LAB_RESULTS', 'Urine Sediment', '2.16.840.1.113762.1.4.1222.176', 'Urine Sediment'),
    ('LAB_RESULTS', 'Urine Urea Nitrogen', '2.16.840.1.113762.1.4.1222.154', 'Urine Urea Nitrogen'),
    ('LAB_RESULTS', 'Uroflowmetry', '2.16.840.1.113762.1.4.1222.823', 'Uroflowmetry'),
    ('LAB_RESULTS', 'Vitamin D Levels', '2.16.840.1.113762.1.4.1222.126', 'Vitamin D'),
    ('LAB_RESULTS', 'White Blood Cell (Leukocytes) Count', '2.16.840.1.113762.1.4.1222.142', 'White Blood Cell (Leukocytes) Count');

insert into resource_categorization_valueset (dataSetName, valuesetName, valuesetOid, category) values
    ('MEDICATIONS', 'ACEis and ARBs', '2.16.840.1.113762.1.4.1213.11', 'ACEis and ARBs'),
    ('MEDICATIONS', 'Erythropoiesis Stimulating Agent', '2.16.840.1.113762.1.4.1196.307', 'Erythropoiesis Stimulating Agent'),
    ('MEDICATIONS', 'Iron Supplement', '2.16.840.1.113762.1.4.1196.308', 'Iron Supplement'),
    ('MEDICATIONS', 'Phosphate Binders', '2.16.840.1.113762.1.4.1196.305', 'Phosphate Binders'),
    ('MEDICATIONS', 'Vitamin D', '2.16.840.1.113762.1.4.1196.306', 'Vitamin D');

create table resource_categorization_coding (
    id int not null auto_increment primary key,
    dataSetName varchar(50) not null,
    codeSystemUrl varchar(255) not null,
    code varchar(255) not null,
    category varchar(255),
    commonName varchar(255)
);

create unique index idxDataSetSystemCode on resource_categorization_coding(dataSetName, codeSystemUrl, code);

insert into resource_categorization_coding (dataSetName, codeSystemUrl, code, commonName) values
    ('VITALS', 'http://loinc.org', '85354-9', 'Blood Pressure'),
    ('VITALS', 'http://loinc.org', '8302-2', 'Height'),
    ('VITALS', 'http://loinc.org', '29463-7', 'Weight'),
    ('VITALS', 'http://loinc.org', '39156-5', 'BMI'),
    ('VITALS', 'http://loinc.org', '8310-5', 'Temperature'),
    ('VITALS', 'http://loinc.org', '8867-4', 'Heart Rate'),
    ('VITALS', 'http://loinc.org', '2708-6', 'Oxygen Saturation'),
    ('VITALS', 'http://loinc.org', '9279-1', 'Respiratory Rate');

create table medication_flag (
    id int not null auto_increment primary key,
    label varchar(30) not null,
    backgroundColor char(7) not null,
    textColor char(7) not null
);

insert into medication_flag (id, label, backgroundColor, textColor) values
    (1, 'Neuroleptics', '#FFCC00', '#000000'),
    (2, 'Antidepressants', '#3399FF', '#FFFFFF'),
    (3, 'Anxiolytics', '#33CC33', '#000000'),
    (4, 'Recovery Support', '#990000', '#FFFFFF'),
    (5, 'Opioids', '#FF3333', '#000000');

create table medication_flag_rxclass (
    id int not null auto_increment primary key,
    medicationFlagId int not null,
    rxClass varchar(10) unique not null,
    constraint mfr_fk1 foreign key (medicationFlagId) references medication_flag (id)
        on delete cascade
);

insert into medication_flag_rxclass (medicationFlagId, rxClass) values
    (1, 'N05A'), (1, 'N05AA'), (1, 'N05AB'),
    (1, 'N05AC'), (1, 'N05AD'), (1, 'N05AE'),
    (1, 'N05AF'), (1, 'N05AG'), (1, 'N05AH'),
    (1, 'N05AL'), (1, 'N05AN'), (1, 'N05AX');

insert into medication_flag_rxclass (medicationFlagId, rxClass) values
    (2, 'N06A'), (2, 'N06AA'), (2, 'N06AB'),
    (2, 'N06AF'), (2, 'N06AG'), (2, 'N06AX');

insert into medication_flag_rxclass (medicationFlagId, rxClass) values
    (3, 'N05B'), (3, 'N05BA'), (3, 'N05BB'),
    (3, 'N05BC'), (3, 'N05BD'), (3, 'N05BE'),
    (3, 'N05BX');

insert into medication_flag_rxclass (medicationFlagId, rxClass) values
    (4, 'N07B'), (4, 'N07BA'), (4, 'N07BB'),
    (4, 'N07BC');

insert into medication_flag_rxclass (medicationFlagId, rxClass) values
    (5, 'N02A'), (5, 'N02AA'), (5, 'N02AB'),
    (5, 'N02AC'), (5, 'N02AD'), (5, 'N02AE'),
    (5, 'N02AF'), (5, 'N02AG'), (5, 'N02AJ'),
    (5, 'N02AX');
