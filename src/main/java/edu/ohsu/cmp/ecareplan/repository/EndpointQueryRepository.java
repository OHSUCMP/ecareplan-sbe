package edu.ohsu.cmp.ecareplan.repository;

import edu.ohsu.cmp.ecareplan.entity.EndpointQuery;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EndpointQueryRepository extends JpaRepository<EndpointQuery, Long> {
}
