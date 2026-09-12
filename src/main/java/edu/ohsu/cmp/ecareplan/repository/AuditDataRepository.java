package edu.ohsu.cmp.ecareplan.repository;

import edu.ohsu.cmp.ecareplan.entity.AuditData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.Date;
import java.util.List;

public interface AuditDataRepository extends JpaRepository<AuditData, Long> {
    @Query("SELECT a FROM AuditData a WHERE a.userId = :userId AND a.event IN :eventList AND a.created BETWEEN :startDate AND :endDate")
    List<AuditData> getAuditDataForUser(Long userId, Collection<String> eventList, Date startDate, Date endDate);
}
