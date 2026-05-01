package edu.ohsu.cmp.ecareplan.repository;

import edu.ohsu.cmp.ecareplan.entity.DataSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DataSetRepository extends JpaRepository<DataSet, Long> {
    @Query("select d from DataSet d where d.name=:name")
    List<DataSet> findAll();
}
