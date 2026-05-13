package edu.ohsu.cmp.ecareplan.service;

import edu.ohsu.cmp.ecareplan.workspace.UserWorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

public abstract class BaseService {
    @Autowired
    protected UserWorkspaceService userWorkspaceService;

    @Autowired
    protected AuditService auditService;

    @Autowired
    protected FHIRService fhirService;

    @Autowired
    protected Environment env;
}
