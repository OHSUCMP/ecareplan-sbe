package edu.ohsu.cmp.ecareplan.repository;

import edu.ohsu.cmp.ecareplan.entity.ResourceCategorizationCoding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResourceCategorizationCodingRepository extends JpaRepository<ResourceCategorizationCoding, Long> {
    List<ResourceCategorizationCoding> findByDataSetName(String dataSetName);
}
