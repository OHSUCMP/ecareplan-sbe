package edu.ohsu.cmp.ecareplan.repository.vsac;

import edu.ohsu.cmp.ecareplan.entity.vsac.Concept;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ConceptRepository extends JpaRepository<Concept, Long> {

    @Query("select c from Concept c where c.code=:code and c.codeSystem=:codeSystem and c.codeSystemVersion=:version")
    Concept findConcept(String code, String codeSystem, String version);
}
