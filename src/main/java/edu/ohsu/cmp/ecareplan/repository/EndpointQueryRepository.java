package edu.ohsu.cmp.ecareplan.repository;

import edu.ohsu.cmp.ecareplan.entity.EndpointQuery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;

public interface EndpointQueryRepository extends JpaRepository<EndpointQuery, Long> {
    @Query("select e from EndpointQuery e where e.endpointId=:endpointId")
    Collection<EndpointQuery> findByEndpointId(Long endpointId);
}
