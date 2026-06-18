package edu.ohsu.cmp.ecareplan.repository;

import edu.ohsu.cmp.ecareplan.entity.ResourceCategorization;
import edu.ohsu.cmp.ecareplan.model.dataset.DataSetName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResourceCategorizationRepository extends JpaRepository<ResourceCategorization, Long> {
    List<ResourceCategorization> findByDataSetName(DataSetName dataSetName);
}
