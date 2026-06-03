package edu.ohsu.cmp.ecareplan.repository;

import edu.ohsu.cmp.ecareplan.entity.DefaultQuery;
import edu.ohsu.cmp.ecareplan.model.dataset.DataSetName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;

public interface DefaultQueryRepository extends JpaRepository<DefaultQuery, Long> {
    @Query("select dq from DefaultQuery dq where dq.dataSetName=:dataSetName")
    Collection<DefaultQuery> findByDataSetName(DataSetName dataSetName);
}
