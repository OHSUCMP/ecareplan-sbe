package edu.ohsu.cmp.ecareplan.repository;

import edu.ohsu.cmp.ecareplan.entity.DefaultQuery;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DefaultQueryRepository extends JpaRepository<DefaultQuery, Long> {
}
