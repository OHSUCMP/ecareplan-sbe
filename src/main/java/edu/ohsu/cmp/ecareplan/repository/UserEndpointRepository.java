package edu.ohsu.cmp.ecareplan.repository;

import edu.ohsu.cmp.ecareplan.entity.UserEndpoint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserEndpointRepository extends JpaRepository<UserEndpoint, Long> {
    List<UserEndpoint> findByUserId(Long userId);
    Boolean existsByUserIdAndEndpointId(Long userId, Long endpointId);
    Optional<UserEndpoint> findByUserIdAndEndpointId(Long userId, Long endpointId);
}
