package edu.ohsu.cmp.ecareplan.entity;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import jakarta.persistence.*;
import org.apache.commons.codec.binary.Base64;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Questionnaire;

import java.nio.charset.StandardCharsets;

@Entity
@Table(name = "assessment")
public class Assessment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String label;
    private String resourceId;
    private String url;
    private String learnMoreUrl;
    private Boolean scored;
    private String codeSystem;
    private String code;

    @Transient
    private Coding coding = null;

    private String questionnaireResourceJsonB64;

    @Transient
    private Questionnaire questionnaire = null;

    private Boolean active;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getLearnMoreUrl() {
        return learnMoreUrl;
    }

    public void setLearnMoreUrl(String learnMoreUrl) {
        this.learnMoreUrl = learnMoreUrl;
    }

    public Boolean isScored() {
        return scored;
    }

    public void setScored(Boolean scored) {
        this.scored = scored;
    }

    public String getCodeSystem() {
        return codeSystem;
    }

    public void setCodeSystem(String codeSystem) {
        this.codeSystem = codeSystem;
        coding = null;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
        coding = null;
    }

    public Coding getCoding() {
        if (coding == null) {
            coding = new Coding(codeSystem, code, null);
        }
        return coding;
    }


    public String getQuestionnaireResourceJsonB64() {
        return questionnaireResourceJsonB64;
    }

    public void setQuestionnaireResourceJsonB64(String questionnaireResourceJsonB64) {
        this.questionnaireResourceJsonB64 = questionnaireResourceJsonB64;
        questionnaire = null;
    }

    public Questionnaire getQuestionnaire() {
        if (questionnaire == null) {
            FhirContext ctx = FhirContext.forR4();
            IParser parser = ctx.newJsonParser();
            String json = new String(Base64.decodeBase64(questionnaireResourceJsonB64), StandardCharsets.UTF_8);
            questionnaire = parser.parseResource(Questionnaire.class, json);
        }
        return questionnaire;
    }

    public Boolean isActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
