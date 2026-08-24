package edu.ohsu.cmp.ecareplan.repository.rxclass;

import edu.ohsu.cmp.ecareplan.entity.rxclass.RxClassMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RxClassMemberRepository extends JpaRepository<RxClassMember, Long>  {
    List<RxClassMember> findByRxClass(String rxClass);
    List<RxClassMember> findByRxCui(String rxCui);
}
