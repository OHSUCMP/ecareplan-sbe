package edu.ohsu.cmp.ecareplan.repository;

import edu.ohsu.cmp.ecareplan.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsUserByPatIdHash(@Param("patIdHash") String patIdHash);

    @Query("select p from User p where p.patIdHash=:patIdHash")
    List<User> findByPatIdHash(@Param("patIdHash") String patIdHash);
    User findOneByPatIdHash(@Param("patIdHash") String patIdHash);
}
