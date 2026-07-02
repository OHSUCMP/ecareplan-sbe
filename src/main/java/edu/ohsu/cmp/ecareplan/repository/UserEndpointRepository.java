package edu.ohsu.cmp.ecareplan.repository;

import edu.ohsu.cmp.ecareplan.entity.UserEndpoint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserEndpointRepository extends JpaRepository<UserEndpoint, Long> {
    List<UserEndpoint> findUserEndpointsByUserId(Long userId);
    Boolean existsUserEndpointByUserIdAndEndpointId(Long userId, Long endpointId);
    UserEndpoint findUserEndpointByUserIdAndEndpointId(Long userId, Long endpointId);
}
