package edu.ohsu.cmp.ecareplan.repository;

import edu.ohsu.cmp.ecareplan.entity.UserEndpoint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserEndpointRepository extends JpaRepository<UserEndpoint, Long> {
    List<UserEndpoint> findByUserId(Long userId);
    Boolean existsByUserIdAndEndpointId(Long userId, Long endpointId);
    UserEndpoint findByUserIdAndEndpointId(Long userId, Long endpointId);
}
