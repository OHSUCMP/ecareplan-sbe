package edu.ohsu.cmp.ecareplan.service.vsac;

import edu.ohsu.cmp.ecareplan.entity.vsac.Concept;
import edu.ohsu.cmp.ecareplan.repository.vsac.ConceptRepository;
import edu.ohsu.cmp.ecareplan.service.BaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ConceptService extends BaseService {
    @Autowired
    private ConceptRepository repository;

    public Concept getConcept(String code, String codeSystem, String codeSystemVersion) {
        return repository.findConcept(code, codeSystem, codeSystemVersion);
    }
}
