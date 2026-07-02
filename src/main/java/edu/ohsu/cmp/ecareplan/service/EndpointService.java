package edu.ohsu.cmp.ecareplan.service;

import edu.ohsu.cmp.ecareplan.entity.Endpoint;
import edu.ohsu.cmp.ecareplan.entity.UserEndpoint;
import edu.ohsu.cmp.ecareplan.model.EndpointModel;
import edu.ohsu.cmp.ecareplan.model.RefreshTokenData;
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
import java.security.InvalidAlgorithmParameterException;
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

    @Value("${endpoint.sds.name}")
    private String sdsEndpointName;

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

    public Endpoint getSDSEndpoint() { return endpointRepository.findByName(sdsEndpointName); }

    public List<EndpointModel> getAllThirdPartyEndpoints() {
        List<EndpointModel> list = new ArrayList<>();
        for (Endpoint endpoint : endpointRepository.findAll(Sort.by("name").ascending())) {
            if (endpoint.getName().equals(patientEndpointName) ||
                    endpoint.getName().equals(careTeamEndpointName) ||
                    endpoint.getName().equals(sdsEndpointName)) {
                continue;
            }
            list.add(new EndpointModel(endpoint));
        }
        return list;
    }

    public UserEndpoint getUserEndpoint(Long userId, Long endpointId) {
        if (userEndpointRepository.existsByUserIdAndEndpointId(userId, endpointId)) {
            return userEndpointRepository.findByUserIdAndEndpointId(userId, endpointId);

        } else {
            UserEndpoint ue = new UserEndpoint();
            ue.setUserId(userId);
            ue.setEndpoint(endpointRepository.findById(endpointId).orElseThrow());
            ue.setCreated(new Date());
            userEndpointRepository.save(ue);
            return ue;
        }
    }

    public void updateUserEndpointLastSyncCompleted(Long userId, Long endpointId) {
        UserEndpoint ue = getUserEndpoint(userId, endpointId);
        ue.setLastSyncCompleted(new Date());
        userEndpointRepository.save(ue);
    }

    public void setRefreshTokenData(Long userId, Long endpointId, SecretKey secretKey, RefreshTokenData refreshTokenData) throws NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, InvalidParameterSpecException, BadPaddingException, InvalidKeyException {
        UserEndpoint ue = getUserEndpoint(userId, endpointId);
        ue.setEncryptedRefreshTokenDataB64(CryptoUtil.encrypt(refreshTokenData, secretKey));
        userEndpointRepository.save(ue);
    }

    public RefreshTokenData getRefreshTokenData(Long userId, Long endpointId, SecretKey secretKey) throws NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException, InvalidAlgorithmParameterException {
        UserEndpoint ue = getUserEndpoint(userId, endpointId);
        return CryptoUtil.decrypt(RefreshTokenData.class, ue.getEncryptedRefreshTokenDataB64(), secretKey);
    }
}
