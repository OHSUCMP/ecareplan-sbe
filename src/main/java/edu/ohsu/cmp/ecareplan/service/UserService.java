package edu.ohsu.cmp.ecareplan.service;

import edu.ohsu.cmp.ecareplan.entity.User;
import edu.ohsu.cmp.ecareplan.model.AuditSeverity;
import edu.ohsu.cmp.ecareplan.repository.UserRepository;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class UserService extends BaseService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Value("${security.salt}")
    private String salt;

    @Autowired
    private UserRepository repository;

    public User getUser(String fhirPatientId) {
        String patIdHash = buildPatIdHash(fhirPatientId);

        User u;
        if (repository.existsByPatIdHash(patIdHash)) {
            u = repository.findByPatIdHash(patIdHash);

        } else {
            u = new User(patIdHash);
            logger.info("Persisting new user with hash " + patIdHash);
            u.setCreated(new Date());
            u = repository.save(u);

            auditService.doAudit(u, AuditSeverity.INFO, "created user record");
        }

        return u;
    }


///////////////////////////////////////////////////////////////////////
// private methods
//

    private String buildPatIdHash(String patientId) {
        return DigestUtils.sha256Hex(patientId + salt);
    }
}
