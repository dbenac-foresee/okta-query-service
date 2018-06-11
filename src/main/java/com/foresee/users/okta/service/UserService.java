package com.foresee.users.okta.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.foresee.okta.domain.user.OktaUser;
import com.foresee.okta.domain.user.UserStatus;
import com.foresee.okta.exception.InvalidUsernameException;
import com.foresee.okta.util.OktaUserUtil;
import com.foresee.users.okta.client.OktaUsersClient;
import com.foresee.users.okta.domain.UserEntity;
import com.foresee.users.okta.repository.UserRepository;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.util.Lists;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.foresee.users.okta.service.UserService.LogFile.*;

@Service
@Log4j2
public class UserService {

    private static final String PROVISIONED_FILTER = "status eq \"PROVISIONED\"";
    private static final Integer OKTA_PAGE_SIZE = 200;
    private static final String OKTA_TOKEN_PREFIX = "SSWS ";
    private static final String OKTA_HEADER_LINKS = "link";
    private static final String NEXT_PAGE_INDICATOR = "rel=\"next\"";
    private static final String MEDIA_TYPE_JSON = "application/json";
    private static final String LOG_DIR = "logs";
    private Pattern NEXT_PAGE_PATTERN = Pattern.compile("after=(\\w+)&");


    @Value("${okta.api-token}")
    private String apiToken;

    @Autowired
    private OktaUsersClient oktaUsersClient;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OktaUserUtil oktaUserUtil;

    @Autowired
    private ObjectMapper objectMapper;

    public void execute() throws InvalidUsernameException {
        initFiles();

        List<OktaUser> allUsers = getAllOktaUsers();

        handleUsers(allUsers);
    }

    private void handleUsers(List<OktaUser> oktaUsers) throws InvalidUsernameException {

        MultiValueMap<Long, OktaUser> foreseeIdMap = new LinkedMultiValueMap<>();
        Map<String, OktaUser> oktaIdMap = Maps.newHashMap();

        AtomicInteger activeUserCount = new AtomicInteger();
        AtomicInteger suspendedCount = new AtomicInteger();
        AtomicInteger provisionedCount = new AtomicInteger();
        AtomicInteger recoveryCount = new AtomicInteger();
        AtomicInteger stagedCount = new AtomicInteger();
        AtomicInteger deprovisionedCount = new AtomicInteger();
        oktaUsers.forEach(oktaUser -> {
            if (oktaUser.getProfile().getForeseeId() == null) {
                writeToFile(MISSING_FORESEE_ID_FILE,
                        StringUtils.joinWith(",",
                                oktaUser.getId(),
                                oktaUser.getProfile().getClientId(),
                                oktaUser.getProfile().getForeseeId(),
                                oktaUser.getProfile().getLogin(),
                                oktaUser.getStatus()
                        ));
                return;
            }
            foreseeIdMap.add(oktaUser.getProfile().getForeseeId(), oktaUser);
            oktaIdMap.put(oktaUser.getId(), oktaUser);
            switch (oktaUser.getStatus()) {
                case ACTIVE:
                    activeUserCount.incrementAndGet();
                    break;
                case PROVISIONED:
                    provisionedCount.incrementAndGet();
                    break;
                case SUSPENDED:
                    suspendedCount.incrementAndGet();
                    break;
                case DEPROVISIONED:
                    deprovisionedCount.incrementAndGet();
                    break;
                case RECOVERY:
                    recoveryCount.incrementAndGet();
                    break;
                case STAGED:
                    stagedCount.incrementAndGet();
                    break;
                default:
                    log.warn("Okta user {} has unexpected status {}", oktaUser.getProfile().getLogin(), oktaUser.getStatus());
                    break;
            }
        });

        writeToFile(STATUS_COUNTS,
                StringUtils.joinWith(",",
                        UserStatus.ACTIVE,
                        UserStatus.SUSPENDED,
                        UserStatus.PROVISIONED,
                        UserStatus.DEPROVISIONED,
                        UserStatus.STAGED,
                        UserStatus.RECOVERY
                ));
        writeToFile(STATUS_COUNTS,
                StringUtils.joinWith(",",
                        activeUserCount.get(),
                        suspendedCount.get(),
                        provisionedCount.get(),
                        deprovisionedCount.get(),
                        stagedCount.get(),
                        recoveryCount.get()
                ));

        List<UserEntity> foreseeUsers = getForeseeUsers(Lists.newArrayList(foreseeIdMap.keySet()));

        for (UserEntity foreseeUser : foreseeUsers) {
            if (foreseeUser.getOktaId() == null) {
                writeToFile(MISSING_OKTA_ID_FILE,
                        StringUtils.joinWith(",",
                                foreseeUser.getClientId(),
                                foreseeUser.getAccountEnabled(),
                                foreseeUser.getUserName(),
                                foreseeUser.getOktaStatus(),
                                foreseeUser.getOktaId(),
                                foreseeUser.getAuthenticationProvider()
                        ));
                continue;
            }
            OktaUser matchingOktaUser = oktaIdMap.get(foreseeUser.getOktaId());
            String oktaUsername = null;
            try {
                oktaUsername = oktaUserUtil.getOktaUsername(
                        foreseeUser.getUserName(),
                        foreseeUser.getUserNameSuffix(),
                        foreseeUser.getEmail());
            } catch (Exception e) {
                log.error("Invalid username: {}", foreseeUser.getUserName(), e);
            }

            // Make sure that a user and a matching okta user match both by okta id and by username
            if (matchingOktaUser == null
                    || (oktaUsername != null && !matchingOktaUser.getProfile().getLogin().equalsIgnoreCase(oktaUsername))) {
                writeToFile(INVALID_OKTA_ID_FILE,
                        StringUtils.joinWith(",",
                                foreseeUser.getClientId(),
                                foreseeUser.getAccountEnabled(),
                                foreseeUser.getUserName(),
                                foreseeUser.getOktaStatus(),
                                foreseeUser.getOktaId(),
                                foreseeUser.getAuthenticationProvider()
                        ));
                continue;
            }

            if ((foreseeUser.getAccountEnabled().equalsIgnoreCase("Y")
                    && matchingOktaUser.getStatus() == UserStatus.SUSPENDED)
                    || (foreseeUser.getAccountEnabled().equalsIgnoreCase("N")
                    && matchingOktaUser.getStatus() != UserStatus.SUSPENDED
                    && matchingOktaUser.getStatus() != UserStatus.PROVISIONED)) {
                writeToFile(MISMATCHED_STATUS,
                        toString(foreseeUser, matchingOktaUser));
            }

            writeToFile(FULL_USER_LIST, toString(foreseeUser, matchingOktaUser));

            // Passwords show as migrated on APP_USER but status in Okta is PROVISIONED
            if ("PASSWORD_MIGRATED".equalsIgnoreCase(foreseeUser.getOktaStatus())
                    && matchingOktaUser.getStatus() == UserStatus.PROVISIONED) {
                writeToFile(PASSWORDS_MIGHT_NOT_BE_MIGRATED,
                        toString(foreseeUser, matchingOktaUser));
            }
            // Passwords show as not migrated but the status in Okta is active so they probably are migrated
            if (!"PASSWORD_MIGRATED".equals(foreseeUser.getOktaStatus())
                    && "FORESEE".equalsIgnoreCase(foreseeUser.getAuthenticationProvider())
                    && UserStatus.ACTIVE == matchingOktaUser.getStatus()) {
                writeToFile(PASSWORDS_PROBABLY_MIGRATED,
                        toString(foreseeUser, matchingOktaUser));
            }
            DateTime lastRelease = new DateTime().withYear(2017).withMonthOfYear(12).withDayOfMonth(11);
            // Passwords that are not migrated
            if (!"PASSWORD_MIGRATED".equals(foreseeUser.getOktaStatus())
                    && foreseeUser.getPasswordMigrated().equalsIgnoreCase("N")
                    && matchingOktaUser.getStatus() != UserStatus.ACTIVE
                    && foreseeUser.getLastLogonDate() != null
                    && foreseeUser.getLastLogonDate().isAfter(lastRelease)
                    && foreseeUser.getPassword() != null) {
                writeToFile(PASSWORDS_NOT_MIGRATED,
                        toString(foreseeUser, matchingOktaUser));
            }

            if (("PASSWORD_MIGRATED".equals(foreseeUser.getOktaStatus())
                    || foreseeUser.getPasswordMigrated().equalsIgnoreCase("Y"))
                    && matchingOktaUser.getStatus() != UserStatus.ACTIVE) {
                writeToFile(PASSWORD_MIGRATED_BUT_NOT_ACTIVE,
                        toString(foreseeUser, matchingOktaUser));
            }

            oktaIdMap.remove(foreseeUser.getOktaId());
        }

        for (String oktaId : oktaIdMap.keySet()) {
            OktaUser oktaUser = oktaIdMap.get(oktaId);
            writeToFile(OKTA_USERS_TO_DELETE,
                    StringUtils.joinWith(",",
                            oktaId,
                            oktaUser.getProfile().getClientId(),
                            oktaUser.getProfile().getForeseeId(),
                            oktaUser.getProfile().getLogin(),
                            oktaUser.getStatus()
                    ));
//            if (oktaUser.getStatus() != UserStatus.DEPROVISIONED) {
//                oktaUsersClient.deactivateUser(
//                        MEDIA_TYPE_JSON,
//                        MEDIA_TYPE_JSON,
//                        OKTA_TOKEN_PREFIX + apiToken,
//                        oktaId);
//            }
        }
    }

    private List<OktaUser> getAllOktaUsers() {
        List<OktaUser> allUsers = Lists.newArrayList();
        try {
            File f = new File(LogFile.OKTA_USER_CACHE.getFilename());
            if (f.exists()) {
                allUsers.addAll(objectMapper.readValue(
                        f,
                        new TypeReference<List<OktaUser>>(){}));
            }
        } catch (Exception e) {
            log.error("Failed to read Okta users from cache");
        }

        if (!allUsers.isEmpty()) {
            log.info("Found {} Okta users in cache", allUsers.size());
            return allUsers;
        }

        String after = null;
        ResponseEntity<List<OktaUser>> response = null;
        int numPages = 0;
        int userCount = 0;

        do {
            response = oktaUsersClient.search(
                    OKTA_TOKEN_PREFIX + apiToken,
                    null,
                    null,
                    OKTA_PAGE_SIZE,
                    after);

            if (response == null || response.getBody().isEmpty()) {
                break;
            }

            List<String> headers = response.getHeaders().get(OKTA_HEADER_LINKS);

            String nextPageUrl = headers.stream()
                    .filter(url -> url.contains(NEXT_PAGE_INDICATOR))
                    .findFirst()
                    .orElse(null);

            after = null;
            if (nextPageUrl != null) {
                Matcher matcher = NEXT_PAGE_PATTERN.matcher(nextPageUrl);
                if (matcher.find()) {
                    after = matcher.group(1);
                }
            }
            numPages++;
            log.info("Read page {} of Okta users.", numPages);
            allUsers.addAll(response.getBody());
//            handleUsers(response.getBody());
            userCount += response.getBody().size();
        } while (after != null);

        log.info("Found {} pages of Okta users. {} total Okta users", numPages, userCount);

        try {
            writeToFile(OKTA_USER_CACHE, objectMapper.writeValueAsString(allUsers));
        } catch (Exception e) {
            log.error("Failed to cache Okta users");
        }
        return allUsers;
    }

    private List<UserEntity> getForeseeUsers(List<Long> userIds) {
        List<UserEntity> foreseeUsers = Lists.newArrayList();
        try {
            File f = new File(LogFile.APP_USER_CACHE.getFilename());
            if (f.exists()) {
                foreseeUsers.addAll(objectMapper.readValue(
                        f,
                        new TypeReference<List<UserEntity>>(){}));
            }
        } catch (Exception e) {
            log.error("Failed to read Foresee users from cache");
        }

        if (!foreseeUsers.isEmpty()) {
            log.info("Found {} Foresee users in cache", foreseeUsers.size());
            return foreseeUsers;
        }

        Iterables.partition(userIds, 1000).forEach(subList -> foreseeUsers.addAll(userRepository.findByIdIn(subList)));

        log.info("Read {} users from app_user", foreseeUsers.size());

        try {
            writeToFile(APP_USER_CACHE, objectMapper.writeValueAsString(foreseeUsers));
        } catch (Exception e) {
            log.error("Failed to cache Foresee users");
        }
        return foreseeUsers;
    }

    private void initFiles() {
        File f;
        try {
            File dir = new File(LOG_DIR);
            if (!dir.exists()) {
                dir.mkdir();
            }
            for (LogFile logFile : LogFile.values()) {
                if (!logFile.isCacheFile()) {
                    f = new File(logFile.getFilename());
                    f.delete();
                    f.createNewFile();
                    switch (logFile) {
                        case MISMATCHED_STATUS:
                        case PASSWORDS_MIGHT_NOT_BE_MIGRATED:
                        case PASSWORDS_PROBABLY_MIGRATED:
                        case PASSWORDS_NOT_MIGRATED:
                        case PASSWORD_MIGRATED_BUT_NOT_ACTIVE:
                        case FULL_USER_LIST:
                            writeToFile(logFile, getHeader());
                            break;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    String getHeader() {
        return StringUtils.joinWith(",",
                "CLIENT_ID",
                "USER_ID",
                "ACCOUNT_ENABLED",
                "LAST_LOGON_DATE",
                "USERNAME",
                "PASSWORD_MIGRATED",
                "OKTA_STATUS",
                "AUTHENTICATION_PROVIDER",
                "OKTA_ID",
                "STATUS_IN_OKTA",
                "LAST_OKTA_LOGIN",
                "LAST_PASSWORD_CHANGE");
    }

    String toString(UserEntity foreseeUser, OktaUser oktaUser) {
        return StringUtils.joinWith(",",
                foreseeUser.getClientId(),
                foreseeUser.getId(),
                foreseeUser.getAccountEnabled(),
                foreseeUser.getLastLogonDate(),
                foreseeUser.getUserName(),
                foreseeUser.getPasswordMigrated(),
                foreseeUser.getOktaStatus(),
                foreseeUser.getAuthenticationProvider(),
                foreseeUser.getOktaId(),
                oktaUser != null ? oktaUser.getStatus() : "n/a",
                oktaUser != null ?oktaUser.getLastLogin() : "n/a",
                oktaUser != null ?oktaUser.getPasswordChanged()  : "n/a");
    }


    void writeToFile (LogFile logFile, String message) {
        try (PrintWriter pw = new PrintWriter(new FileOutputStream(logFile.getFilename(), true))) {
            pw.println(message);
        } catch (Exception e) {
            log.error("Failed to write message to file {}. Message: {}", logFile.getFilename(), message, e);
        }
    }

    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    enum LogFile {
        MISSING_OKTA_ID_FILE("ForeseeUsersMissingOktaId.csv", false),
        INVALID_OKTA_ID_FILE("ForeseeUsersWithInvalidOktaId.csv", false),
        MISSING_FORESEE_ID_FILE("OktaUsersMissingForeseeId.csv", false),
        OKTA_USERS_TO_DELETE("OrphanOktaUsersToDelete.csv", false),
        MISMATCHED_STATUS("MismatchedStatus.csv", false),
        PASSWORD_MIGRATED_BUT_NOT_ACTIVE("PasswordsMigratedButNotActive.csv", false),
        PASSWORDS_PROBABLY_MIGRATED("PasswordsProbablyMigrated.csv", false),
        PASSWORDS_MIGHT_NOT_BE_MIGRATED("PasswordsMightNotBeMigrated.csv", false),
        STATUS_COUNTS("StatusCounts.csv", false),
        PASSWORDS_NOT_MIGRATED("PasswordsNotMigrated.csv", false),
        OKTA_USER_CACHE("OktaUserCache", true),
        APP_USER_CACHE("AppUserCache", true),
        FULL_USER_LIST("FullUserList.csv", false);

        private String filename;
        private boolean cacheFile;

        public String getFilename() {
            return LOG_DIR + "/" + filename;
        }

        public boolean isCacheFile() {
            return cacheFile;
        }
    }
}
