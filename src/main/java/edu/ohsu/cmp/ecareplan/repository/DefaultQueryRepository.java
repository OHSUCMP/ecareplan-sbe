package edu.ohsu.cmp.ecareplan.repository;

import edu.ohsu.cmp.ecareplan.entity.DefaultQuery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DefaultQueryRepository extends JpaRepository<DefaultQuery, Long> {
    List<DefaultQuery> findByDataSetName(String dataSetName);
}
