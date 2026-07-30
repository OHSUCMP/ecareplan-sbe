create table assessment (
    id int not null auto_increment primary key,
    name varchar(255) unique not null,
    label varchar(255) not null,
    resourceId varchar(255) not null,
    url varchar(255) not null,
    learnMoreUrl varchar(255),
    isScored boolean not null,
    code varchar(255) not null,
    codeSystem varchar(255) not null,
    active boolean not null default true
);

create index idxName on assessment (name);

insert into assessment (id, name, label, resourceId, url, isScored, code, codeSystem, learnMoreUrl) values
    (1, 'PHQ-9', 'Depression Screening', '44249-1', 'http://ohsu.edu/fhir/Questionnaire/PHQ-9', true, '44249-1', 'http://loinc.org', 'https://medlineplus.gov/lab-tests/depression-screening/'),
    (2, 'GAD-7', 'Anxiety Screening', '69737-5', 'http://ohsu.edu/fhir/Questionnaire/GAD-7', true, '69737-5', 'http://loinc.org', null),
    (3, 'PROMIS-29-questionnaire', 'General Health Assessment', '62337-1', 'http://loinc.org/q/62337-1', false, '62337-1', 'http://loinc.org', null),
    (4, 'AHC-questionnaire', 'Health-Related Social Needs', '96777-8', 'http://loinc.org/q/96777-8', false, '96777-8', 'http://loinc.org', null),
    (5, 'caregiver-strain-questionnaire', 'Caregiver Strain Assessment', 'questionnaire-caregiver-strain-index', 'http://hl7.org/fhir/us/mcc/Questionnaire/caregiver-strain-index', false, 'caregiver-strain-index', 'http://hl7.org', null);

create table assessment_item (
    id int not null auto_increment primary key,
    assessmentId int not null,
    parentAssessmentItemId int,
    code varchar(255),
    codeSystem varchar(255),
    linkId varchar(255) not null,
    text varchar(255) not null,
    constraint ai_fk1 foreign key (assessmentId) references assessment (id),
    constraint ai_fk2 foreign key (parentAssessmentItemId) references assessment_item (id)
);

insert into assessment_item (id, assessmentId, parentAssessmentItemId, code, codeSystem, linkId, text) values
    (1, 1, null, 'no-code', null, 'phq9', 'Over the last 2 weeks, how often have you been bothered by any of the following problems?'),
    (2, 1, 1, '44250-9', 'http://loinc.org', '/44250-9', 'Little interest or pleasure in doing things'),
    (3, 1, 1, '44255-8', 'http://loinc.org', '/44255-8', 'Feeling down, depressed, or hopeless'),
    (4, 1, 1, '44259-0', 'http://loinc.org', '/44259-0', 'Trouble falling or staying asleep, or sleeping too much'),
    (5, 1, 1, '44254-1', 'http://loinc.org', '/44254-1', 'Feeling tired or having little energy'),
    (6, 1, 1, '44251-7', 'http://loinc.org', '/44251-7', 'Poor appetite or overeating'),
    (7, 1, 1, '44258-2', 'http://loinc.org', '/44258-2', 'Feeling bad about yourself-or that you are a failure or have let yourself or your family down'),
    (8, 1, 1, '44252-5', 'http://loinc.org', '/44252-5', 'Trouble concentrating on things, such as reading the newspaper or watching television'),
    (9, 1, 1, '44253-3', 'http://loinc.org', '/44253-3', 'Moving or speaking so slowly that other people could have noticed. Or the opposite – being so fidgety or restless that you were moving around a lot more than usual'),
    (10, 1, 1, '44260-8', 'http://loinc.org', '/44260-8', 'Thoughts that you would be better off dead, or of hurting yourself in some way'),
    (11, 1, 1, '44261-6', 'http://loinc.org', '/44261-6', 'Patient health questionnaire 9 item total score'),
    (12, 1, 1, '69722-7', 'http://loinc.org', '/69722-7', 'How difficult have these problems made it for you to do your work, take care of things at home, or get along with other people?'),
    (13, 1, 12, 'no-code', null, '/69722-7-help', 'If you checked off any problems on this questionnaire');

insert into assessment_item (id, assessmentId, parentAssessmentItemId, code, codeSystem, linkId, text) values
    (14, 2, null, 'no-code', null, 'gad7', 'Over the last two weeks, how often have you been bothered by the following problems?'),
    (15, 2, 14, '69725-0', 'http://loinc.org', '/69725-0', 'Feeling nervous, anxious or on edge'),
    (16, 2, 14, '68509-9', 'http://loinc.org', '/68509-9', 'Over the past 2 weeks have you not been able to stop or control worrying'),
    (17, 2, 14, '69733-4', 'http://loinc.org', '/69733-4', 'Worrying too much about different things'),
    (18, 2, 14, '69734-2', 'http://loinc.org', '/69734-2', 'Trouble relaxing'),
    (19, 2, 14, '69735-9', 'http://loinc.org', '/69735-9', 'Being so restless that it is hard to sit still'),
    (20, 2, 14, '69689-8', 'http://loinc.org', '/69689-8', 'Becoming easily annoyed or irritable.'),
    (21, 2, 14, '69736-7', 'http://loinc.org', '/69736-7', 'Feeling afraid as if something awful might happen'),
    (22, 2, 14, '70274-6', 'http://loinc.org', '/70274-6', 'Generalized anxiety disorder 7 item (GAD-7) total score [Reported.PHQ]');

insert into assessment_item (id, assessmentId, parentAssessmentItemId, code, codeSystem, linkId, text) values
    (23, 3, null, 'no-code', null, 'physical-function', 'Physical Function'),
    (24, 3, 23, '61597-1', 'http://loinc.org', '47202', 'Are you able to do chores such as vacuuming or yard work?'),
    (25, 3, 23, '61607-8', 'http://loinc.org', '47203', 'Are you able to go up and down stairs at a normal pace?'),
    (26, 3, 23, '61609-4', 'http://loinc.org', '47204', 'Are you able to go for a walk of at least 15 minutes?'),
    (27, 3, 23, '61635-9', 'http://loinc.org', '47205', 'Are you able to run errands and shop?'),
    (28, 3, null, 'no-code', null, 'anxiety', 'Anxiety'),
    (29, 3, 28, '61923-9', 'http://loinc.org', '47206', 'In the past 7 days - I felt fearful'),
    (30, 3, 28, '61941-1', 'http://loinc.org', '47207', 'In the past 7 days - I found it hard to focus on anything other than my anxiety'),
    (31, 3, 28, '61942-9', 'http://loinc.org', '47208', 'In the past 7 days - My worries overwhelmed me'),
    (32, 3, 28, '61949-4', 'http://loinc.org', '47209', 'In the past 7 days - I felt uneasy'),
    (33, 3, null, 'no-code', null, 'depression', 'Depression'),
    (34, 3, 33, '61953-6', 'http://loinc.org', '47210', 'In the past 7 days - I felt worthless'),
    (35, 3, 33, '61955-1', 'http://loinc.org', '47211', 'In the past 7 days - I felt helpless'),
    (36, 3, 33, '61967-6', 'http://loinc.org', '47212', 'In the past 7 days - I felt depressed'),
    (37, 3, 33, '61973-4', 'http://loinc.org', '47213', 'In the past 7 days - I felt hopeless'),
    (38, 3, null, 'no-code', null, 'fatigue', 'Fatigue'),
    (39, 3, 38, '61878-5', 'http://loinc.org', '47214', 'During the past 7 days - I feel fatigued'),
    (40, 3, 38, '61882-7', 'http://loinc.org', '47215', 'During the past 7 days - I have trouble starting things because I am tired'),
    (41, 3, 38, '61865-2', 'http://loinc.org', '47216', 'In the past 7 days - How run-down did you feel on average?'),
    (42, 3, 38, '61864-5', 'http://loinc.org', '47217', 'In the past 7 days - How fatigued were you on average?'),
    (43, 3, null, 'no-code', null, 'sleep-disturbance', 'Sleep Disturbance'),
    (44, 3, 43, '61987-4', 'http://loinc.org', '47218', 'In the past 7 days - My sleep quality was...'),
    (45, 3, 43, '61986-6', 'http://loinc.org', '47219', 'In the past 7 days - My sleep was refreshing'),
    (46, 3, 43, '61998-1', 'http://loinc.org', '47220', 'In the past 7 days - I had a problem with my sleep'),
    (47, 3, 43, '61999-9', 'http://loinc.org', '47221', 'In the past 7 days - I had difficulty falling asleep'),
    (48, 3, null, 'no-code', null, 'social-participation', 'Ability to Participate in Social Roles and Activities'),
    (49, 3, 48, '62041-9', 'http://loinc.org', '47222', 'In the past 7 days - I am satisfied with how much work I can do (include work at home)'),
    (50, 3, 48, '62046-8', 'http://loinc.org', '47223', 'In the past 7 days - I am satisfied with my ability to work (include work at home)'),
    (51, 3, 48, '62050-0', 'http://loinc.org', '47224', 'In the past 7 days - I am satisfied with my ability to do regular personal and household responsibilities'),
    (52, 3, 48, '62051-8', 'http://loinc.org', '47225', 'In the past 7 days - I am satisfied with my ability to perform my daily routines'),
    (53, 3, null, 'no-code', null, 'pain-interference', 'Pain Interference'),
    (54, 3, 53, '61758-9', 'http://loinc.org', '47226', 'In the past 7 days - How much did pain interfere with your day to day activities?'),
    (55, 3, 53, '61769-6', 'http://loinc.org', '47227', 'In the past 7 days - How much did pain interfere with work around the home?'),
    (56, 3, 53, '61773-8', 'http://loinc.org', '47228', 'In the past 7 days - How much did pain interfere with your ability to participate in social activities?'),
    (57, 3, 53, '61775-3', 'http://loinc.org', '47229', 'In the past 7 days - How much did pain interfere with your household chores?'),
    (58, 3, null, 'no-code', null, 'pain-intensity', 'Pain Intensity'),
    (59, 3, 58, '61583-1', 'http://loinc.org', '47230', 'In the past 7 days - How would you rate your pain on average?');

insert into assessment_item (id, assessmentId, parentAssessmentItemId, code, codeSystem, linkId, text) values
    (60, 4, null, 'no-code', null, 'ach-housing', 'Living Situation'),
    (61, 4, 60, '71802-3', 'http://loinc.org', '124296', 'What is your living situation today?'),
    (62, 4, 60, '96778-6', 'http://loinc.org', '123820', 'Think about the place you live. Do you have problems with any of the following?'),
    (63, 4, null, 'no-code', null, 'ach-food', 'Food'),
    (64, 4, 63, '88122-7', 'http://loinc.org', '123821', 'Within the past 12 months, you worried that your food would run out before you got money to buy more'),
    (65, 4, 63, '88123-5', 'http://loinc.org', '123822', 'Within the past 12 months, the food you bought just didn''t last and you didn''t have money to get more'),
    (66, 4, null, 'no-code', null, 'ach-other-risks', 'Other Risks'),
    (67, 4, 66, '93030-5', 'http://loinc.org', '123823', 'In the past 12 months, has lack of reliable transportation kept you from medical appointments, meetings, work or from getting things needed for daily living?'),
    (68, 4, 66, '96779-4', 'http://loinc.org', '123824', 'In the past 12 months has the electric, gas, oil, or water company threatened to shut off services in your home?'),
    (69, 4, null, 'no-code', null, 'ach-safety', 'Safety'),
    (70, 4, 69, '95618-5', 'http://loinc.org', '123825', 'How often does anyone, including family and friends, physically hurt you?'),
    (71, 4, 69, '95617-7', 'http://loinc.org', '123826', 'How often does anyone, including family and friends, insult or talk down to you?'),
    (72, 4, 69, '95616-9', 'http://loinc.org', '123827', 'How often does anyone, including family and friends, threaten you with harm?'),
    (73, 4, 69, '95615-1', 'http://loinc.org', '123828', 'How often does anyone, including family and friends, scream or curse at you?');

insert into assessment_item (id, assessmentId, parentAssessmentItemId, code, codeSystem, linkId, text) values
    (74, 5, null, 'no-code', null, 'caregiver-strain-group', 'Caregiver Strain'),
    (75, 5, 74, '???', 'http://hl7.org', 'q1', 'Sleep is disturbed (e.g., because . . . is in and out of bed or wanders around at night)'),
    (76, 5, 74, '???', 'http://hl7.org', 'q2', 'It is inconvenient (e.g., because helping takes so much time or it’s a long drive over to help)'),
    (77, 5, 74, '???', 'http://hl7.org', 'q3', 'It is a physical strain (e.g., because of lifting in and out of a chair; effort or concentration is required)'),
    (78, 5, 74, '???', 'http://hl7.org', 'q4', 'It is confining (e.g., helping restricts free time or cannot go visiting)'),
    (79, 5, 74, '???', 'http://hl7.org', 'q5', 'There have been family adjustments (e.g., because helping has disrupted routine; there has been no privacy)'),
    (80, 5, 74, '???', 'http://hl7.org', 'q6', 'There have been changes in personal plans (e.g., had to turn down a job; could not go on vacation)'),
    (81, 5, 74, '???', 'http://hl7.org', 'q7', 'There have been emotional adjustments (e.g., because of severe arguments)'),
    (82, 5, 74, '???', 'http://hl7.org', 'q8', 'Some behavior is upsetting (e.g., because of incontinence; . . . has trouble remembering things; or . . . accuses people of taking things)'),
    (83, 5, 74, '???', 'http://hl7.org', 'q9', 'It is upsetting to find . . . has changed so much from his/her former self (e.g., he/she is a different person than he/she used to be)'),
    (84, 5, 74, '???', 'http://hl7.org', 'q10', 'There have been work adjustments (e.g., because of having to take time off)'),
    (85, 5, 74, '???', 'http://hl7.org', 'q11', 'It is a financial strain'),
    (86, 5, 74, '???', 'http://hl7.org', 'q12', 'Feeling completely overwhelmed (e.g., because of worry about . . . ; concerns about how you will manage)');
