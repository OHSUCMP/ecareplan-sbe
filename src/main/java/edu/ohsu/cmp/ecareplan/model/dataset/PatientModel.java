package edu.ohsu.cmp.ecareplan.model.dataset;

import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

public class PatientModel extends BaseDataSetModel<Patient> {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final String GENDER_EXTENSION_URL = "http://hl7.org/fhir/StructureDefinition/patient-genderIdentity";

    private final String name;
    private Long age;
    private String gender;

    public PatientModel(Patient patient) {
        super(patient);

        String officialName = null;
        String usualName = null;
        String defaultName = null;
        if (patient.hasName()) {
            defaultName = patient.getNameFirstRep().getNameAsSingleString();
            for (HumanName hn : patient.getName()) {
                if (officialName == null && hn.getUse() == HumanName.NameUse.OFFICIAL) {
                    officialName = buildName(hn);
                } else if (usualName == null && hn.getUse() == HumanName.NameUse.USUAL) {
                    usualName = buildName(hn);
                }
            }
        }
        if      (usualName != null)     this.name = usualName;
        else if (officialName != null)  this.name = officialName;
        else {
            logger.warn("no USUAL or OFFICIAL name for patient with id={} - using default", id);
            this.name = defaultName;
        }

        if (patient.getBirthDate() != null) {
            LocalDate start = patient.getBirthDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            LocalDate stop = LocalDate.now(ZoneId.systemDefault());
            age = ChronoUnit.YEARS.between(start, stop);
        }

        if (patient.hasExtension(GENDER_EXTENSION_URL)) {
            logger.debug("gender-identity extension found for Patient with id={}", id);

            Extension ext = patient.getExtensionByUrl(GENDER_EXTENSION_URL);
            CodeableConcept cc = (CodeableConcept) ext.getValue();
            if (cc.hasCoding()) {
                Coding c = cc.getCodingFirstRep();

                if (c.hasDisplay()) {
                    logger.debug("setting gender={} from extension Coding.display for Patient with id={}", c.getDisplay(), id);
                    gender = c.getDisplay();

                } else if (c.hasCode()) {
                    logger.debug("setting gender={} from extension Coding.code for Patient with id={}", c.getCode(), id);
                    gender = c.getCode();
                }
            }

            if (gender == null && cc.hasText()) {
                logger.debug("setting gender={} from extension CodeableConcept.text for Patient with id={}", cc.getText(), id);
                gender = cc.getText();
            }
        }

        if (gender == null && patient.hasGender()) {
            gender = patient.getGender().getDisplay();
        }
    }

    @Override
    public Patient toResourceForSDSExport() {
        return sourceResource;
    }

    public String getName() {
        return name;
    }

    public Long getAge() {
        return age;
    }

    public String getGender() {
        return gender;
    }

///////////////////////////////////////////////////////////////////////////////////
// private methods
//

    private String buildName(HumanName hn) {
        if (hn != null) {
            if (hn.hasText()) {
                // if the name element has text, just use that
                return hn.getText();

            } else if (hn.hasFamily() && hn.hasGiven()) {
                // otherwise, construct it from parts, providing those exist
                return hn.getGivenAsSingleString() + " " + hn.getFamily();
            }
        }
        return null;
    }
}
