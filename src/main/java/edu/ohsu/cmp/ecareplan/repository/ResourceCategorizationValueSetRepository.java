package edu.ohsu.cmp.ecareplan.repository;

import edu.ohsu.cmp.ecareplan.entity.ResourceCategorizationValueSet;
import edu.ohsu.cmp.ecareplan.model.dataset.DataSetName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResourceCategorizationValueSetRepository extends JpaRepository<ResourceCategorizationValueSet, Long> {
    List<ResourceCategorizationValueSet> findByDataSetName(DataSetName dataSetName);
}
