package com.foresee.users.okta.domain;

import lombok.*;
import org.joda.time.DateTime;

import javax.persistence.*;
import java.io.Serializable;

/**
 * Entity representing the APP_USER table
 * <p>
 * <p>
 * Supports 3 custom named queries as explained below
 * <p>
 * <ul>
 * <li>
 * Obtain list of users whose accounts have been locked prior to 30 minutes and unlock them
 * </li>
 * <li>
 * Obtain list of users whose accounts are active/enabled, are not shadow users and have a not null password
 * and passwordExpirationDate is not set. We want to ensure there is a override expiration set up
 * </li>
 * <li>
 * Obtain list of users whose accounts are set to expire today or have already expired
 * </li>
 * <li>
 * Obtain list of users who have not logged in for X days and to whom reminders need to be sent
 * </li>
 * <li>
 * Obtain list of users who have not logged in for X days and whose accounts needs to be disabled
 * </li>
 * <li>
 * Obtain list of users whose password status is X and is not a shadow user
 * </li>
 * <li>
 * Obtain list of users whose password activation code needs to be deleted
 * </li>
 * <li>
 * Find user by user name
 * </li>
 * <li>
 * Find user by user id
 * </li>
 * <li>
 * Find user by email address
 * </li>
 * </ul>
 * </p>
 */
@Entity
@Table(name = "APP_USER")
@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class UserEntity implements Serializable {
    /**
     * Serial Version UID
     */
    private static final long serialVersionUID = 1L;

    @Column(name = "AUTHENTICATION_PROVIDER")
    private String authenticationProvider;

    @Column(name = "EXTERNAL_ID")
    private String externalId;

    @Column(name = "OKTA_ID")
    private String oktaId;

    @Column(name = "OKTA_STATUS")
    private String oktaStatus;

    @Column(name = "CHALLENGE_QUESTION_SET")
    private String challengeQuestionSet ;

    @Column(name = "PASSWORD_MIGRATED")
    private String passwordMigrated ;

    @Column(name = "USERNAME_SUFFIX")
    private String userNameSuffix;
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE,
            generator = "user_seq")
    @SequenceGenerator(name = "user_seq",
            sequenceName = "USER_SEQ")
    @Column(name = "ID", updatable = false, nullable = false)
    private Long id;

    @Column(name = "SHADOW")
    private String shadow;

    @Column(name = "SHADOW_ID")
    private Long shadowId;

    @Column(name = "VERSION")
    private Long version;

    @Column(name = "CLIENT_ID", nullable = false)
    private Long clientId;

    @Column(name = "USERNAME", nullable = false)
    private String userName;

    @Column(name = "PASSWORD")
    private String password;

    @Column(name = "FIRST_NAME", nullable = false)
    private String firstName;

    @Column(name = "LAST_NAME", nullable = false)
    private String lastName;

    @Column(name = "EMAIL", nullable = false)
    private String email;

    @Column(name = "PHONE_NUMBER")
    private String phone;

    @Column(name = "ACCOUNT_ENABLED")
    private String accountEnabled;

    @Column(name = "ACCOUNT_EXPIRED")
    private String accountExpired;

    @Column(name = "ACCOUNT_LOCKED")
    private String accountLocked;

    @Column(name = "SESSION_ACTIVE")
    private String sessionActive;

    @Column(name = "SINGLE_USE")
    private String singleUse;

    @Column(name = "IS_PROFILE_LOCKED")
    private String profileLocked;

    @Column(name = "PASSWORD_STATUS")
    private String passwordStatus;

    @Column(name = "CREATE_DATE")
    private DateTime createdDate;

    @Column(name = "LAST_MODIFIED")
    private DateTime lastModifiedDate;

    @Column(name = "LAST_LOGON_DATE")
    private DateTime lastLogonDate;

    @Column(name = "FAILED_LOGON_DATE")
    private DateTime failedLogonDate;

    @Column(name = "LOGON_COUNT")
    private Long logonCount;

    @Column(name = "FAILED_LOGON_COUNT")
    private Long failedLogonCount;

}
