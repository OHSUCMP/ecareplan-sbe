package edu.ohsu.cmp.ecareplan.repository;

import edu.ohsu.cmp.ecareplan.entity.UserEndpoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface UserEndpointRepository extends JpaRepository<UserEndpoint, Long> {
    @Query("select ue from UserEndpoint ue where ue.userId=:userId")
    List<UserEndpoint> findByUserId(Long userId);
}
