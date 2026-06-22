package edu.ohsu.cmp.ecareplan.repository;

import edu.ohsu.cmp.ecareplan.entity.ResourceCategorizationCoding;
import edu.ohsu.cmp.ecareplan.model.dataset.DataSetName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResourceCategorizationCodingRepository extends JpaRepository<ResourceCategorizationCoding, Long> {
    List<ResourceCategorizationCoding> findByDataSetName(DataSetName dataSetName);
}
