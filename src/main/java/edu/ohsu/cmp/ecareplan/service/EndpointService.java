package edu.ohsu.cmp.ecareplan.service;

import edu.ohsu.cmp.ecareplan.entity.Endpoint;
import edu.ohsu.cmp.ecareplan.entity.UserEndpoint;
import edu.ohsu.cmp.ecareplan.model.EndpointModel;
import edu.ohsu.cmp.ecareplan.repository.EndpointRepository;
import edu.ohsu.cmp.ecareplan.repository.UserEndpointRepository;
import edu.ohsu.cmp.ecareplan.util.CryptoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidParameterSpecException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class EndpointService extends BaseService {
    private static final Logger logger = LoggerFactory.getLogger(EndpointService.class);

    @Value("${endpoint.patientLaunch.name}")
    private String patientEndpointName;

    @Value("${endpoint.careTeamLaunch.name}")
    private String careTeamEndpointName;

    @Autowired
    private EndpointRepository endpointRepository;

    @Autowired
    private UserEndpointRepository userEndpointRepository;

    public Endpoint getPatientLaunchEndpoint() {
        return endpointRepository.findByName(patientEndpointName);
    }

    public Endpoint getCareTeamLaunchEndpoint() {
        return endpointRepository.findByName(careTeamEndpointName);
    }

    public List<EndpointModel> getAllThirdPartyEndpoints() {
        List<EndpointModel> list = new ArrayList<>();
        for (Endpoint endpoint : endpointRepository.findAll(Sort.by("name").ascending())) {
            if (endpoint.getName().equals(patientEndpointName) ||
                    endpoint.getName().equals(careTeamEndpointName)) {
                continue;
            }
            list.add(new EndpointModel(endpoint));
        }
        return list;
    }

    public List<UserEndpoint> getAllUserEndpoints(Long userId) {
        return userEndpointRepository.findByUserId(userId);
    }

    public UserEndpoint getUserEndpoint(Long userId, Long endpointId) {
        return userEndpointRepository.findByUserIdAndEndpointId(userId, endpointId).orElseThrow();
    }

    public UserEndpoint createUserEndpoint(Long userId, Long endpointId, String fhirPatientId, String refreshToken, SecretKey secretKey) throws NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, InvalidParameterSpecException, BadPaddingException, InvalidKeyException {
        UserEndpoint ue = new UserEndpoint();
        ue.setUserId(userId);
        ue.setEndpoint(endpointRepository.findById(endpointId).orElseThrow());
        ue.setEncryptedPatientId(CryptoUtil.encrypt(fhirPatientId, secretKey));
        if (refreshToken != null) ue.setEncryptedRefreshToken(CryptoUtil.encrypt(refreshToken, secretKey));
        ue.setCreated(new Date());
        userEndpointRepository.save(ue);
        return ue;
    }

    public UserEndpoint updateUserEndpointLastSyncCompleted(UserEndpoint ue) {
        ue.setLastSyncCompleted(new Date());
        return userEndpointRepository.save(ue);
    }

    public UserEndpoint updateUserEndpointRefreshToken(UserEndpoint userEndpoint, String refreshToken, SecretKey secretKey) throws NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, InvalidParameterSpecException, BadPaddingException, InvalidKeyException {
        userEndpoint.setEncryptedRefreshToken(CryptoUtil.encrypt(refreshToken, secretKey));
        return userEndpointRepository.save(userEndpoint);
    }
}
