package edu.ohsu.cmp.ecareplan.repository;

import edu.ohsu.cmp.ecareplan.entity.EndpointQuery;
import edu.ohsu.cmp.ecareplan.model.dataset.DataSetName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;

public interface EndpointQueryRepository extends JpaRepository<EndpointQuery, Long> {
    @Query("select e from EndpointQuery e where e.endpointId=:endpointId and e.dataSetName=:dataSetName")
    Collection<EndpointQuery> findByEndpointIdAndDataSetName(Long endpointId, DataSetName dataSetName);
}
