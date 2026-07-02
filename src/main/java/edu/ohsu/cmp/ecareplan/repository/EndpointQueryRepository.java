package edu.ohsu.cmp.ecareplan.repository;

import edu.ohsu.cmp.ecareplan.entity.EndpointQuery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EndpointQueryRepository extends JpaRepository<EndpointQuery, Long> {
    List<EndpointQuery> findByEndpointIdAndDataSetName(Long endpointId, String dataSetName);
}
