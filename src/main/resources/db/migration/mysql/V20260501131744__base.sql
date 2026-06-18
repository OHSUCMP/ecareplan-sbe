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

create table resource_categorization (
    id int not null auto_increment primary key,
    dataSetName varchar(50) not null,
    valuesetName varchar(255) not null,
    valuesetOid varchar(255) not null,
    category varchar(255),
    displayName varchar(255)
);

create unique index idxDataSetOid on resource_categorization(dataSetName, valuesetOid);

INSERT INTO resource_categorization (dataSetName, valuesetName, valuesetOid, category, displayName) VALUES
    ('CONCERNS', 'Arthritis Disorders', '2.16.840.1.113762.1.4.1222.81', 'Arthritis', 'Arthritis'),
    ('CONCERNS', 'Infectious Arthritis', '2.16.840.1.113762.1.4.1222.654', 'Arthritis', 'Arthritis'),
    ('CONCERNS', 'Osteoarthritis', '2.16.840.1.113762.1.4.1222.648', 'Arthritis', 'Osteoarthritis'),
    ('CONCERNS', 'Psoriatic Arthritis', '2.16.840.1.113762.1.4.1222.587', 'Arthritis', 'Arthritis'),
    ('CONCERNS', 'Reactive Arthritis', '2.16.840.1.113762.1.4.1222.588', 'Arthritis', 'Arthritis'),
    ('CONCERNS', 'Rheumatoid Arthritis', '2.16.840.1.113762.1.4.1222.651', 'Arthritis', 'Rheumatoid Arthritis'),
    ('CONCERNS', 'Acute Coronary Syndromes', '2.16.840.1.113883.3.3157.2000.10', 'Cardiovascular Disease', 'Heart Attack'),
    ('CONCERNS', 'Acute Myocardial Infarction', '2.16.840.1.113883.3.666.5.3011', 'Cardiovascular Disease', 'Heart Attack'),
    ('CONCERNS', 'American Heart Association Heart Failure Stage', '2.16.840.1.113762.1.4.1222.581', 'Cardiovascular Disease', 'Heart Failure Stage'),
    ('CONCERNS', 'Aneurysm', '2.16.840.1.113762.1.4.1222.627', 'Cardiovascular Disease', 'Aneurysm'),
    ('CONCERNS', 'Angina', '2.16.840.1.113762.1.4.1222.608', 'Cardiovascular Disease', 'Heart Pain'),
    ('CONCERNS', 'Aortic Disease', '2.16.840.1.113762.1.4.1222.636', 'Cardiovascular Disease', 'Aortic Disease'),
    ('CONCERNS', 'Arrhythmia', '2.16.840.1.113883.3.526.3.366', 'Cardiovascular Disease', 'Heart Rhythm Problem'),
    ('CONCERNS', 'Atherosclerotic Cardiovascular Disease', '2.16.840.1.113762.1.4.1222.584', 'Cardiovascular Disease', 'Coronary Blockages'),
    ('CONCERNS', 'Cardiomyopathy', '2.16.840.1.113762.1.4.1222.579', 'Cardiovascular Disease', 'Heart Structure Problem'),
    ('CONCERNS', 'Carotid Stenosis', '2.16.840.1.113762.1.4.1222.639', 'Cardiovascular Disease', 'Carotid Blockage'),
    ('CONCERNS', 'Cerebrovascular Disease', '2.16.840.1.113762.1.4.1222.1524', 'Cardiovascular Disease', 'Cerebrovascular Disease'),
    ('CONCERNS', 'Congestive heart failure Diagnosis', '2.16.840.1.113883.3.526.3.369', 'Cardiovascular Disease', 'Heart Failure'),
    ('CONCERNS', 'Coronary Artery Disease No MI', '2.16.840.1.113762.1.4.1222.1540', 'Cardiovascular Disease', 'Coronary Blockage'),
    ('CONCERNS', 'Heart Failure', '2.16.840.1.113762.1.4.1222.1543', 'Cardiovascular Disease', 'Heart Failure'),
    ('CONCERNS', 'Hypertension, Persistent', '2.16.840.1.113762.1.4.1222.1563', 'Cardiovascular Disease', 'High Blood Pressure'),
    ('CONCERNS', 'Hypertension, Primary and Secondary', '2.16.840.1.113762.1.4.1222.571', 'Cardiovascular Disease', 'High Blood Pressure'),
    ('CONCERNS', 'Hypertension Stage', '2.16.840.1.113762.1.4.1222.566', 'Cardiovascular Disease', 'High Blood Pressure'),
    ('CONCERNS', 'Hypertension, Essential or Primary', '2.16.840.1.113762.1.4.1222.1484', 'Cardiovascular Disease', 'High Blood Pressure'),
    ('CONCERNS', 'Hypertension, Pulmonary Hypertension', '2.16.840.1.113762.1.4.1222.569', 'Cardiovascular Disease', 'High Blood Pressure'),
    ('CONCERNS', 'Hypertension, Secondary', '2.16.840.1.113762.1.4.1222.642', 'Cardiovascular Disease', 'High Blood Pressure'),
    ('CONCERNS', 'Intracranial Stenosis', '2.16.840.1.113762.1.4.1222.614', 'Cardiovascular Disease', 'Heart Damage from Blockages'),
    ('CONCERNS', 'Ischemic Heart Disease', '2.16.840.1.113762.1.4.1222.615', 'Cardiovascular Disease', 'Thickened Left Heart'),
    ('CONCERNS', 'Left Ventricular Hypertrophy', '2.16.840.1.113762.1.4.1222.580', 'Cardiovascular Disease', 'Heart Functional Level'),
    ('CONCERNS', 'NYHA Heart Failure Functional Classifications', '2.16.840.1.113762.1.4.1222.27', 'Cardiovascular Disease', 'Blood Vessel Blockages away from Heart'),
    ('CONCERNS', 'Peripheral Vascular Disease', '2.16.840.1.113762.1.4.1222.1488', 'Cardiovascular Disease', 'Fast Heart Rate due to Standing'),
    ('CONCERNS', 'Postural tachycardia syndrome (POTS) Diagnosis', '2.16.840.1.113762.1.4.1222.645', 'Cardiovascular Disease', 'Fluid Leakage in Lung'),
    ('CONCERNS', 'Pulmonary Edema', '2.16.840.1.113762.1.4.1222.611', 'Cardiovascular Disease', 'High Risk Heart Pains'),
    ('CONCERNS', 'Stroke History', '2.16.840.1.113883.3.464.1003.104.12.1017', 'Cardiovascular Disease', 'Heart Valve Disease'),
    ('CONCERNS', 'Acute Renal Failure', '2.16.840.1.113762.1.4.1222.1534', 'Chronic Kidney Disease', 'Kidney Attack'),
    ('CONCERNS', 'Chronic Kidney Disease All Stages', '2.16.840.1.113762.1.4.1222.157', 'Chronic Kidney Disease', 'Chronic Kidney Disease'),
    ('CONCERNS', 'Chronic Kidney Disease Type or Cause', '2.16.840.1.113762.1.4.1222.6', 'Chronic Kidney Disease', 'Chronic Kidney Disease'),
    ('CONCERNS', 'Acanthosis Nigricans', '2.16.840.1.113762.1.4.1222.556', 'Diabetes Conditions', 'Thickening/Darkening of Skin from Diabetes'),
    ('CONCERNS', 'Amputated Limb (Not Traumatic)', '2.16.840.1.113762.1.4.1222.563', 'Diabetes Conditions', 'Amputated Limb (not due to injury)'),
    ('CONCERNS', 'Blindness', '2.16.840.1.113883.3.464.1003.115.12.1089', 'Diabetes Conditions', 'Blindness'),
    ('CONCERNS', 'Cataract of the eye', '2.16.840.1.113762.1.4.1222.498', 'Diabetes Conditions', 'Cataracts'),
    ('CONCERNS', 'Cellulitis', '2.16.840.1.113762.1.4.1222.533', 'Diabetes Conditions', 'Blindness'),
    ('CONCERNS', 'Charcot Foot', '2.16.840.1.113762.1.4.1222.531', 'Diabetes Conditions', 'Foot numbness'),
    ('CONCERNS', 'Complication due to Diabetes Mellitus', '2.16.840.1.113762.1.4.1222.1537', 'Diabetes Conditions', 'Complication of Diabetes'),
    ('CONCERNS', 'Diabetic Coma', '2.16.840.1.113762.1.4.1222.482', 'Diabetes Conditions', 'Coma from Diabetes'),
    ('CONCERNS', 'Diabetic Foot', '2.16.840.1.113762.1.4.1222.523', 'Diabetes Conditions', 'Foot Problems from Diabetes'),
    ('CONCERNS', 'Diabetic Foot Ulcer', '2.16.840.1.113762.1.4.1222.526', 'Diabetes Conditions', 'Foot Sore from Diabetes'),
    ('CONCERNS', 'Diabetic hand/ Diabetic cheiroarthropathy', '2.16.840.1.113762.1.4.1222.535', 'Diabetes Conditions', 'Hand Problems from Diabetes'),
    ('CONCERNS', 'Diabetic Neuropathy Conditions', '2.16.840.1.113762.1.4.1222.33', 'Diabetes Conditions', 'Nerve Damage from Diabetes'),
    ('CONCERNS', 'Diabetic Peripheral Angiopathy', '2.16.840.1.113762.1.4.1222.492', 'Diabetes Conditions', 'Artery Damage from Diabetes'),
    ('CONCERNS', 'Diabetic Retinopathy', '2.16.840.1.113883.3.526.3.327', 'Diabetes Conditions', 'Vision Loss from Diabetes'),
    ('CONCERNS', 'Dupuytren''s Contracture', '2.16.840.1.113762.1.4.1222.546', 'Diabetes Conditions', 'Finger Stiffening'),
    ('CONCERNS', 'Emotional Distress caused by Chronic Condition', '2.16.840.1.113762.1.4.1222.508', 'Diabetes Conditions', 'Stress from Chronic Illness'),
    ('CONCERNS', 'Eruptive Xanthomatosis', '2.16.840.1.113762.1.4.1222.557', 'Diabetes Conditions', 'Skin Bumps from Cholesterol'),
    ('CONCERNS', 'Gangrene', '2.16.840.1.113762.1.4.1222.543', 'Diabetes Conditions', 'Gangrene'),
    ('CONCERNS', 'Gestational Diabetes', '2.16.840.1.113762.1.4.1032.90', 'Diabetes Conditions', 'Diabetes in Pregnancy'),
    ('CONCERNS', 'Hyperglycemic Hyperosmolar Nonketotic Syndrome HHNS', '2.16.840.1.113762.1.4.1222.517', 'Diabetes Conditions', 'Coma from Diabetes'),
    ('CONCERNS', 'Hypoglycemia unawareness', '2.16.840.1.113762.1.4.1222.514', 'Diabetes Conditions', 'Drowsiness from Low Blood Sugar'),
    ('CONCERNS', 'Hypoglycemic event', '2.16.840.1.113762.1.4.1222.513', 'Diabetes Conditions', 'Low Blood Sugar Event'),
    ('CONCERNS', 'Ketoacidosis', '2.16.840.1.113762.1.4.1222.520', 'Diabetes Conditions', 'High Ketones from Low Blood pH'),
    ('CONCERNS', 'Maturity Onset Diabetes of the Young (MODY)', '2.16.840.1.113762.1.4.1222.420', 'Diabetes Conditions', 'Diabetes'),
    ('CONCERNS', 'Necrobiosis Lipoidica (Skin Lesions)', '2.16.840.1.113762.1.4.1222.540', 'Diabetes Conditions', 'Skin Injury from Small Artery Disease'),
    ('CONCERNS', 'Periodontitis', '2.16.840.1.113762.1.4.1222.560', 'Diabetes Conditions', 'Inflammation around the Teeth'),
    ('CONCERNS', 'Prediabetes (borderline diabetes)', '2.16.840.1.113762.1.4.1222.419', 'Diabetes Conditions', 'Prediabetes'),
    ('CONCERNS', 'Rubeosis Iridis', '2.16.840.1.113762.1.4.1222.501', 'Diabetes Conditions', 'Iris Reddening from Diabetes'),
    ('CONCERNS', 'Scleroderma or Thick Skin Syndrome', '2.16.840.1.113762.1.4.1222.549', 'Diabetes Conditions', 'Skin Thickening'),
    ('CONCERNS', 'Type 1 Diabetes', '2.16.840.1.113883.3.464.1003.103.12.1020', 'Diabetes Conditions', 'Autoimmune Diabetes'),
    ('CONCERNS', 'Type II Diabetes', '2.16.840.1.113883.3.464.1003.103.12.1021', 'Diabetes Conditions', 'Diabetes'),
    ('CONCERNS', 'Anxiety', '2.16.840.1.113762.1.4.1032.52', 'Mental Health', 'Anxiety'),
    ('CONCERNS', 'Bipolar Diagnosis', '2.16.840.1.113883.3.600.450', 'Mental Health', 'Bipolar'),
    ('CONCERNS', 'Depression Diagnosis', '2.16.840.1.113883.3.600.145', 'Mental Health', 'Depression'),
    ('CONCERNS', 'Dysthymia', '2.16.840.1.113883.3.67.1.101.1.254', 'Mental Health', 'Depression'),
    ('CONCERNS', 'Experience of Traumatic Events', '2.16.840.1.113762.1.4.1222.590', 'Mental Health', 'Trauma'),
    ('CONCERNS', 'Grief or Loss', '2.16.840.1.113762.1.4.1222.690', 'Mental Health', 'Grief'),
    ('CONCERNS', 'Major Depression', '2.16.840.1.113883.3.464.1003.105.12.1007', 'Mental Health', 'Depression'),
    ('CONCERNS', 'Post Partum Depression', '2.16.840.1.113762.1.4.1222.681', 'Mental Health', 'Depression after Pregnancy'),
    ('CONCERNS', 'PostTraumatic Stress Disorder PTSD', '2.16.840.1.113762.1.4.1222.103', 'Mental Health', 'PTSD'),
    ('CONCERNS', 'Psychological Trauma', '2.16.840.1.113762.1.4.1222.687', 'Mental Health', 'Trauma'),
    ('CONCERNS', 'Psychotic Depression', '2.16.840.1.113762.1.4.1222.678', 'Mental Health', 'Depression'),
    ('CONCERNS', 'Seasonal Affective Disorder', '2.16.840.1.113762.1.4.1222.684', 'Mental Health', 'Depression'),
    ('CONCERNS', 'Suicide Risk', '2.16.840.1.113762.1.4.1222.693', 'Mental Health', 'Risk of Suicide'),
    ('CONCERNS', 'Alkaline Phosphatase Deficiency Conditions', '2.16.840.1.113762.1.4.1222.93', 'Nutrition / Metabolic Conditions', 'Low Alkaline Phosphatase'),
    ('CONCERNS', 'Anemia Conditions', '2.16.840.1.113762.1.4.1222.53', 'Nutrition / Metabolic Conditions', 'Anemia'),
    ('CONCERNS', 'Familial Hypercholesterolemia', '2.16.840.1.113762.1.4.1047.100', 'Nutrition / Metabolic Conditions', 'Genetic High Cholesterol'),
    ('CONCERNS', 'Gout', '2.16.840.1.113762.1.4.1222.586', 'Nutrition / Metabolic Conditions', 'Gout'),
    ('CONCERNS', 'Hypercalcemia Conditions', '2.16.840.1.113762.1.4.1222.60', 'Nutrition / Metabolic Conditions', 'High Calcium'),
    ('CONCERNS', 'Hyperkalemia Conditions', '2.16.840.1.113762.1.4.1222.50', 'Nutrition / Metabolic Conditions', 'High Potassium'),
    ('CONCERNS', 'Hyperlipidemia Conditions', '2.16.840.1.113762.1.4.1222.73', 'Nutrition / Metabolic Conditions', 'High Blood Fats'),
    ('CONCERNS', 'Hyperphosphatemia Conditions', '2.16.840.1.113762.1.4.1222.66', 'Nutrition / Metabolic Conditions', 'High Phosphates in Blood'),
    ('CONCERNS', 'Hypertriglyceridemia', '2.16.840.1.113762.1.4.1222.742', 'Nutrition / Metabolic Conditions', 'High Triglycerides'),
    ('CONCERNS', 'Hyperuricemia Conditions', '2.16.840.1.113762.1.4.1222.85', 'Nutrition / Metabolic Conditions', 'High Uric Acid in Blood'),
    ('CONCERNS', 'Hypoalbuminemia Conditions', '2.16.840.1.113762.1.4.1222.47', 'Nutrition / Metabolic Conditions', 'Low Albumin'),
    ('CONCERNS', 'Hypocalcemia Conditions', '2.16.840.1.113762.1.4.1222.61', 'Nutrition / Metabolic Conditions', 'Low Calcium'),
    ('CONCERNS', 'Malnutrition Diagnosis', '2.16.840.1.113762.1.4.1222.1517', 'Nutrition / Metabolic Conditions', 'Malnutrition'),
    ('CONCERNS', 'Metabolic Acidosis Conditions', '2.16.840.1.113762.1.4.1222.70', 'Nutrition / Metabolic Conditions', 'Low Blood pH due to Kidney Problems'),
    ('CONCERNS', 'Metabolic syndrome', '2.16.840.1.113762.1.4.1222.1561', 'Nutrition / Metabolic Conditions', 'Diabetes'),
    ('CONCERNS', 'Obesity Conditions', '2.16.840.1.113762.1.4.1222.36', 'Nutrition / Metabolic Conditions', 'High BMI'),
    ('CONCERNS', 'Vitamin D Deficiency Conditions', '2.16.840.1.113762.1.4.1222.90', 'Nutrition / Metabolic Conditions', 'Low Vitamin D'),
    ('CONCERNS', 'Chronic Pain Conditions', '2.16.840.1.113762.1.4.1222.76', 'Pain Conditions', 'Chronic Pain'),
    ('CONCERNS', 'Low Back Pain', '2.16.840.1.113762.1.4.1222.1558', 'Pain Conditions', 'Low Back Pain'),
    ('CONCERNS', 'Migraine', '2.16.840.1.113762.1.4.1222.1552', 'Pain Conditions', 'Migraine'),
    ('CONCERNS', 'Chronic Tension-type Headache diagnosis', '2.16.840.1.113762.1.4.1222.1555', 'Pain Conditions', 'Chronic Headaches'),
    ('CONCERNS', 'Neuropathic Pain (Neuralgia)', '2.16.840.1.113762.1.4.1222.663', 'Pain Conditions', 'Nerve Pain'),
    ('CONCERNS', 'Phantom Pain', '2.16.840.1.113762.1.4.1222.669', 'Pain Conditions', 'Phantom Pain'),
    ('CONCERNS', 'Asthma Diagnosis', '2.16.840.1.113762.1.4.1222.1472', 'Respiratory Conditions', 'Asthma'),
    ('CONCERNS', 'Bronchiectasis Diagnosis', '2.16.840.1.113762.1.4.1222.1463', 'Respiratory Conditions', 'Lung Damage'),
    ('CONCERNS', 'Chronic obstructive pulmonary disease (COPD) Diagnosis', '2.16.840.1.113762.1.4.1222.1466', 'Respiratory Conditions', 'COPD'),
    ('CONCERNS', 'Interstitial lung disease Diagnosis', '2.16.840.1.113762.1.4.1222.1469', 'Respiratory Conditions', 'Interstitial Lung Damage'),
    ('CONCERNS', 'Pulmonary embolism Diagnosis', '2.16.840.1.113762.1.4.1222.1481', 'Respiratory Conditions', 'Lung Clot'),
    ('CONCERNS', 'Sleep Apnea Disorders', '2.16.840.1.113762.1.4.1222.102', 'Respiratory Conditions', 'Sleep Apnea'),
    ('CONCERNS', 'Social Determinants of Health', '2.16.840.1.113762.1.4.1196.788', 'Other Health Risk Factors', NULL);

INSERT INTO resource_categorization (dataSetName, valuesetName, valuesetOid, displayName) VALUES
    ('TESTS', '24 Hour Urine Protein Excretion', '2.16.840.1.113762.1.4.1222.792', '24 Hour Urine Protein Excretion'),
    ('TESTS', '24 Hour Urine Volume', '2.16.840.1.113762.1.4.1222.791', '24 Hour Urine Volume'),
    ('TESTS', 'Alanine Aminotransferase (ALT), Bld/Ser/Plas', '2.16.840.1.113883.3.3616.200.110.102.5001', 'ALT'),
    ('TESTS', 'Albumin in Blood, Plasma, or Serum', '2.16.840.1.113762.1.4.1222.151', 'Albumin'),
    ('TESTS', 'Aldosterone/Renin Ratio', '2.16.840.1.113762.1.4.1222.811', 'Aldosterone/Renin Ratio'),
    ('TESTS', 'Alkaline Phosphatase (Alp) in Blood, Serum or Plasma', '2.16.840.1.113762.1.4.1222.805', 'ALP'),
    ('TESTS', 'Anion Gap', '2.16.840.1.113762.1.4.1222.153', 'Anion Gap'),
    ('TESTS', 'Arterial Blood Gas (ABG)', '2.16.840.1.113762.1.4.1222.1575', 'Arterial Blood Gas'),
    ('TESTS', 'Aspartate Aminotransferase (AST), Ser/Plas', '2.16.840.1.113883.3.3616.200.110.102.5006', 'AST'),
    ('TESTS', 'Aspartate Transaminase or Alanine Aminotransferase Ratio', '2.16.840.1.113762.1.4.1222.804', 'Aspartate Transaminase or Alanine Aminotransferase Ratio'),
    ('TESTS', 'B Type Natriuretic Peptide [Bnp] in Blood, Serum or Plasma', '2.16.840.1.113762.1.4.1222.795', 'B Type Natriuretic Peptide [Bnp'),
    ('TESTS', 'Bicarbonate in blood, serum or plasma', '2.16.840.1.113762.1.4.1222.130', 'Bicarbonate'),
    ('TESTS', 'Bilirubin, Total, Bld/Ser/Plas', '2.16.840.1.113883.3.3616.200.110.102.5007', 'Bilirubin'),
    ('TESTS', 'Blood Ethanol Level', '2.16.840.1.113762.1.4.1222.813', 'Blood Ethanol Level'),
    ('TESTS', 'Blood Urea Nitrogen', '2.16.840.1.113762.1.4.1222.113', 'Blood Urea Nitrogen'),
    ('TESTS', 'Bone Biopsy Report', '2.16.840.1.113762.1.4.1222.870', 'Bone Biopsy Report'),
    ('TESTS', 'C reactive Protein (CRP), Bld/Ser/Plas', '2.16.840.1.113883.3.3616.200.110.102.5015', 'C reactive Protein (CRP)'),
    ('TESTS', 'Calcium (Not Corrected for Serum Albumin) in Blood, Plasms, or Serum', '2.16.840.1.113762.1.4.1222.794', 'Calcium (Not Corrected for Serum Albumin)'),
    ('TESTS', 'Cerebral Spinal Fluid (CSF) Analysis', '2.16.840.1.113762.1.4.1222.1573', 'Cerebral Spinal Fluid (CSF) Analysis'),
    ('TESTS', 'Chloride in blood, serum or plasma', '2.16.840.1.113762.1.4.1222.132', 'Chloride'),
    ('TESTS', 'Coagulation Assay (PT, aPTT, Fibrinogen)', '2.16.840.1.113762.1.4.1222.1576', 'Coagulation Assay'),
    ('TESTS', 'Complete Blood Count (with Diff)', '1.3.6.1.4.1.6997.4.1.2.271.13.38167.1.1.999.594', ''),
    ('TESTS', 'Comprehensive Metabolic Panel (CMP)', '2.16.840.1.113762.1.4.1222.1574', ''),
    ('TESTS', 'Corrected Calcium', '2.16.840.1.113762.1.4.1222.122', 'Calcium'),
    ('TESTS', 'COVID 19 (SARS CoV 2), SARS CoV, Influenza virus A and B Tests', '2.16.840.1.113762.1.4.1222.1580', 'COVID 19 Test'),
    ('TESTS', 'Creatine Kinase (CK, CK MB) in Blood, Serum, or Plasma', '2.16.840.1.113762.1.4.1222.1579', 'Creatine Kinase (CK, CK MB)'),
    ('TESTS', 'Creatinine in blood, serum or plasma', '2.16.840.1.113762.1.4.1222.111', 'Creatinine'),
    ('TESTS', 'Cystatin C', '2.16.840.1.113762.1.4.1222.138', 'Cystatin C'),
    ('TESTS', 'D Dimer Test', '2.16.840.1.113762.1.4.1222.1577', 'D Dimer'),
    ('TESTS', 'Drugs of Abuse Screen', '2.16.840.1.113762.1.4.1222.1597', 'Drugs of Abuse Screen'),
    ('TESTS', 'Erythrocyte Distribution Width', '2.16.840.1.113762.1.4.1222.147', 'Erythrocyte Distribution Width'),
    ('TESTS', 'Erythrocyte Sedimentation Rate (ESR), Blood', '2.16.840.1.113883.3.3616.200.110.102.5019', 'Erythrocyte Sedimentation Rate (ESR)'),
    ('TESTS', 'Estimated Average Glucose', '2.16.840.1.113762.1.4.1222.150', 'Estimated Average Glucose'),
    ('TESTS', 'Estimated Glomerular Filtration Rate (eGFR)', '2.16.840.1.113762.1.4.1222.179', 'eGFR'),
    ('TESTS', 'Estimated Glomerular Filtration Rate NKF', '2.16.840.1.113883.3.6929.3.1000', ''),
    ('TESTS', 'Fasting Blood Glucose', '2.16.840.1.113762.1.4.1222.808', 'Fasting Blood Glucose'),
    ('TESTS', 'Ferritin', '2.16.840.1.113762.1.4.1222.140', 'Ferritin'),
    ('TESTS', 'Free T4 (Thyroxine) Test', '2.16.840.1.113883.3.7643.2.1019', 'Free T4 (Thyroxine)'),
    ('TESTS', 'Gamma Glutamyl Transpeptidase (Ggt) in blood, serum or plasma', '2.16.840.1.113762.1.4.1222.806', 'Gamma Glutamyl Transpeptidase (Ggt)'),
    ('TESTS', 'Gastric Tissue Biopsy Report', '2.16.840.1.113762.1.4.1222.869', 'Gastric Tissue Biopsy Report'),
    ('TESTS', 'Glucose Tolerance Test Results', '2.16.840.1.113762.1.4.1032.94', 'Glucose Tolerance'),
    ('TESTS', 'Hematocrit', '2.16.840.1.113762.1.4.1222.143', 'Hematocrit'),
    ('TESTS', 'Hemoglobin', '2.16.840.1.113762.1.4.1222.114', 'Hemoglobin'),
    ('TESTS', 'Hemoglobin A1C', '2.16.840.1.113762.1.4.1222.119', 'Hemoglobin A1c %'),
    ('TESTS', 'High Density Lipoprotein', '2.16.840.1.113762.1.4.1222.135', 'HDL Cholesterol'),
    ('TESTS', 'High Sensitivity Troponin', '2.16.840.1.113762.1.4.1222.801', 'High Sensitivity Troponin'),
    ('TESTS', 'INR', '2.16.840.1.113883.3.117.1.7.1.213', 'INR'),
    ('TESTS', 'Intact Parathyroid Hormone', '2.16.840.1.113762.1.4.1222.129', 'Intact Parathyroid Hormone'),
    ('TESTS', 'Iron Saturation (Transferrin Saturation/TSAT)', '2.16.840.1.113762.1.4.1222.118', 'Iron Saturation'),
    ('TESTS', 'Kidney Biopsy Report', '2.16.840.1.113762.1.4.1222.864', 'Kidney Biopsy Report'),
    ('TESTS', 'KT/V Hemodialysis Ratio', '2.16.840.1.113762.1.4.1222.128', 'KT/V Hemodialysis Ratio'),
    ('TESTS', 'Low Density Lipoprotein', '2.16.840.1.113762.1.4.1222.1568', 'LDL Cholesterol'),
    ('TESTS', 'Mean Corpuscular Hemoglobin Concentration', '2.16.840.1.113762.1.4.1222.145', 'Mean Corpuscular Hemoglobin Concentration'),
    ('TESTS', 'Mean Corpuscular Volume', '2.16.840.1.113762.1.4.1222.144', 'Mean Corpuscular Volume'),
    ('TESTS', 'Microorganisms Detection by Blood Culture', '2.16.840.1.113762.1.4.1222.1581', 'Microorganisms Detection by Blood Culture'),
    ('TESTS', 'Microorganisms Detection by Sputum Culture', '2.16.840.1.113762.1.4.1222.1582', 'Microorganisms Detection by Sputum Culture'),
    ('TESTS', 'Muscle Biopsy Report', '2.16.840.1.113762.1.4.1222.868', 'Muscle Biopsy Report'),
    ('TESTS', 'N Terminal Pro_B_Type Natriuretic Peptide [Nt_Probnp] in blood, serum or plasma', '2.16.840.1.113762.1.4.1222.797', 'N Terminal Pro_B_Type Natriuretic Peptide'),
    ('TESTS', 'Oxygen Saturation, Blood', '2.16.840.1.113883.3.3616.200.110.102.5033', 'Oxygen Saturation'),
    ('TESTS', 'Phosphorus in blood, serum or plasma', '2.16.840.1.113762.1.4.1222.123', 'Phosphorus'),
    ('TESTS', 'Platelet Count', '2.16.840.1.113762.1.4.1222.146', 'Platelet Count'),
    ('TESTS', 'Platelet Distribution Width', '2.16.840.1.113762.1.4.1222.148', 'Platelet Distribution Width'),
    ('TESTS', 'Platelet Mean Volume', '2.16.840.1.113762.1.4.1222.149', 'Platelet Mean Volume'),
    ('TESTS', 'Potassium in blood, serum or plasma', '2.16.840.1.113762.1.4.1222.120', 'Potassium'),
    ('TESTS', 'Procalcitonin in Blood, Serum, or Plasma', '2.16.840.1.113762.1.4.1222.1578', 'Procalcitonin'),
    ('TESTS', 'Random Blood Glucose Test', '2.16.840.1.113762.1.4.1222.809', 'Random Blood Glucose Test'),
    ('TESTS', 'Red Blood Cell Count (Erythrocytes)', '2.16.840.1.113762.1.4.1222.141', 'Red Blood Cell Count (Erythrocytes)'),
    ('TESTS', 'Serum Rheumatoid Factor', '2.16.840.1.113762.1.4.1222.812', 'Serum Rheumatoid Factor'),
    ('TESTS', 'Skin Biopsy Report', '2.16.840.1.113762.1.4.1222.867', 'Skin Biopsy Report'),
    ('TESTS', 'Sodium in blood, serum or plasma', '2.16.840.1.113762.1.4.1222.131', 'Sodium'),
    ('TESTS', 'Thyroid Stimulating Hormone (TSH) Test', '2.16.840.1.113883.3.7643.3.1024', 'Thyroid Stimulating Hormone (TSH)'),
    ('TESTS', 'Total Carbon Dioxide or Bicarbonate in blood, serum or plasma', '2.16.840.1.113762.1.4.1222.793', 'Total Carbon Dioxide or Bicarbonate'),
    ('TESTS', 'Total Cholesterol', '2.16.840.1.113762.1.4.1222.139', 'Total Cholesterol'),
    ('TESTS', 'Triglyceride in blood, serum or plasma', '2.16.840.1.113762.1.4.1222.137', 'Triglycerides'),
    ('TESTS', 'Triiodothyronine in serum or plasma', '2.16.840.1.113762.1.4.1222.807', 'Triiodothyronine'),
    ('TESTS', 'Troponin I, Bld/Ser/Plas', '2.16.840.1.113883.3.3616.200.110.102.5061', 'Troponin I'),
    ('TESTS', 'Troponin T, Bld/Ser/Pla', '2.16.840.1.113883.3.3616.200.110.102.5062', 'Troponin T'),
    ('TESTS', 'Urea Reduction Ratio', '2.16.840.1.113762.1.4.1222.819', 'Urea Reduction Ratio'),
    ('TESTS', 'Uric Acid (Urate) in blood, serum or plasma', '2.16.840.1.113762.1.4.1222.810', 'Uric Acid (Urate)'),
    ('TESTS', 'Urine Albumin Creatinine Ratio', '2.16.840.1.113883.3.6929.2.1002', 'Urine Albumin Creatinine Ratio'),
    ('TESTS', 'Urine Protein to Creatinine Ratio (UPCR)', '2.16.840.1.113762.1.4.1222.790', 'Urine Protein to Creatinine Ratio (UPCR)'),
    ('TESTS', 'Urine Sediment', '2.16.840.1.113762.1.4.1222.176', 'Urine Sediment'),
    ('TESTS', 'Urine Urea Nitrogen', '2.16.840.1.113762.1.4.1222.154', 'Urine Urea Nitrogen'),
    ('TESTS', 'Uroflowmetry', '2.16.840.1.113762.1.4.1222.823', 'Uroflowmetry'),
    ('TESTS', 'Vitamin D Levels', '2.16.840.1.113762.1.4.1222.126', 'Vitamin D'),
    ('TESTS', 'White Blood Cell (Leukocytes) Count', '2.16.840.1.113762.1.4.1222.142', 'White Blood Cell (Leukocytes) Count');

INSERT INTO resource_categorization (dataSetName, valuesetName, valuesetOid, category) VALUES
    ('MEDICATIONS', 'ACEis and ARBs', '2.16.840.1.113762.1.4.1213.11', 'ACEis and ARBs'),
    ('MEDICATIONS', 'Erythropoiesis Stimulating Agent', '2.16.840.1.113762.1.4.1196.307', 'Erythropoiesis Stimulating Agent'),
    ('MEDICATIONS', 'Iron Supplement', '2.16.840.1.113762.1.4.1196.308', 'Iron Supplement'),
    ('MEDICATIONS', 'Phosphate Binders', '2.16.840.1.113762.1.4.1196.305', 'Phosphate Binders'),
    ('MEDICATIONS', 'Vitamin D', '2.16.840.1.113762.1.4.1196.306', 'Vitamin D');
