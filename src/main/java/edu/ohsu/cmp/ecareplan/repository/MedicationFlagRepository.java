package edu.ohsu.cmp.ecareplan.repository;

import edu.ohsu.cmp.ecareplan.entity.MedicationFlag;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicationFlagRepository extends JpaRepository<MedicationFlag, Long> {
}
