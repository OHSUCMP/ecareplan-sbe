package edu.ohsu.cmp.ecareplan.repository;

import edu.ohsu.cmp.ecareplan.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsUserByPatIdHash(String patIdHash);
    User findUserByPatIdHash(String patIdHash);
}
