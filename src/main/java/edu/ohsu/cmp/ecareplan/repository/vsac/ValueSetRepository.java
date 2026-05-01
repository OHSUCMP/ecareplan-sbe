package edu.ohsu.cmp.ecareplan.repository.vsac;

import edu.ohsu.cmp.ecareplan.entity.vsac.ValueSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

public interface ValueSetRepository extends JpaRepository<ValueSet, Long> {
    ValueSet findOneByOid(@Param("oid") String oid);
}
