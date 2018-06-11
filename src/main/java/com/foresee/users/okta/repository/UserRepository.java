package com.foresee.users.okta.repository;

import com.foresee.users.okta.domain.UserEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

/**
 * JPA Repository to interact with the APP_USER table in WORKBENCH_APP Schema
 *
 * <p>
 *    Class extends  CrudRepository provided by Spring for regular Entity operations.
 *    In addition the com.answers.service requires the following custom queries
 *
 *    <ul>
 *        <li>
 *            Query to get the users who have been locked due to <=5 invalid login attempts so
 *            that they can be unlocked
 *        </li>
 *       <li>
 *            Query to get the active users with passwords to update their passwordExpiration date
 *        </li>
 *        <li>
 *            Query to get the users whose passwords expire today or have already expired
 *        </li>
 *        <li>
 *            Query to get the users whose have not logged in for more than 90 days
 *        </li>
 *        <li>
 *            Query to get the users who have not logged in and have been sent a reminder
 *        </li>
 *    </ul>
 * </p>
 */
public interface UserRepository extends CrudRepository<UserEntity,Long> {

     /**
     * Find users associated with user ids
     * @param userIds
     * @return list of users
     */
    List<UserEntity> findByIdIn(@Param("userIds") Collection<Long> userIds);

}
