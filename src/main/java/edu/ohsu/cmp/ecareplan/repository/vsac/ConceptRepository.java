package edu.ohsu.cmp.ecareplan.repository.vsac;

import edu.ohsu.cmp.ecareplan.entity.vsac.Concept;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConceptRepository extends JpaRepository<Concept, Long> {
    Concept findByCodeAndCodeSystemAndCodeSystemVersion(String code, String codeSystem, String version);
    Concept findFirstByCodeAndCodeSystemOrderByCodeSystemVersionDesc(String code, String codeSystem);
}
