package com.foresee.users.okta.client;

import com.foresee.okta.domain.group.OktaGroup;
import com.foresee.okta.domain.user.OktaUser;
import com.foresee.okta.domain.user.OktaUserUpdateRequest;
import com.foresee.okta.domain.user.UserActivateResponse;
import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.context.annotation.Primary;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.foresee.okta.client.OktaUsersClient.OKTA_USER_SERVICE;
import static com.foresee.okta.domain.constant.OktaApiConstants.*;
import static com.foresee.platform.http.support.domain.constant.AttributeConstants.USER_ID;
import static org.springframework.web.bind.annotation.RequestMethod.*;

/**
 * @author dhiraj.patil
 * @since 7/31/2017
 * <p>
 * Feign client for User API calls for operations to manage users in Okta.
 * API Reference: https://developer.okta.com/docs/api/resources/users.html
 */
@Service
@Primary
@FeignClient(value = OKTA_USER_SERVICE, url = "${okta.url}")
public interface OktaUsersClient {

    String OKTA_USER_SERVICE = "okta-service";
    static final String OKTA_PARAM_QUERY = "q";
    static final String OKTA_PARAM_FILTER = "filter";
    static final String OKTA_PARAM_LIMIT = "limit";
    static final String OKTA_PARAM_AFTER = "after";

    /**
     * Get user by okta-id or by userName
     *
     * @param token
     * @param userId
     * @return Fetched User in Okta
     */
    @RequestMapping(value = OKTA_API_VERSION + OKTA_USERS + OKTA_ENDPOINT_USER_ID, method = GET,
            produces = OKTA_ACCEPT_HEADER, consumes = OKTA_CONTENT_TYPE_HEADER)
    ResponseEntity<OktaUser> getUser(@RequestHeader(AUTHORIZATION_HEADER) String token,
                                     @PathVariable(USER_ID) String userId);

    /**
     * Create a user in Okta
     *
     * @param activate
     * @param request
     * @return Newly created user in Okta
     */
    @RequestMapping(value = OKTA_API_VERSION + OKTA_USERS, method = POST)
    ResponseEntity<OktaUser> create(
            @RequestHeader(AUTHORIZATION_HEADER) String token,
            @RequestParam(OKTA_REQUEST_PARAM_ACTIVATE) Boolean activate,
            @RequestParam(OKTA_REQUEST_PARAM_PROVIDER) Boolean provider,
            @RequestBody OktaUser request);


    /**
     * Search for users in Okta using either a query search or filter search (one must be provided).
     * @param query an optional search parameter that will search in firstName, lastName, and email
     * @param filter an optional search parameter that will search by the field specified in the filter
     * @param limit limits the results
     * @param after gives you the next page of results (found in the header)
     * @return
     */
    @RequestMapping(value = OKTA_API_VERSION + OKTA_USERS, method = GET)
    ResponseEntity<List<OktaUser>> search(
            @RequestHeader(AUTHORIZATION_HEADER) String token,
            @RequestParam(OKTA_PARAM_QUERY) String query,
            @RequestParam(OKTA_PARAM_FILTER) String filter,
            @RequestParam(OKTA_PARAM_LIMIT) Integer limit,
            @RequestParam(OKTA_PARAM_AFTER) String after);

    /**
     * Update user in okta
     *
     * @param userId
     * @param payload
     * @return
     */
    @RequestMapping(value = OKTA_API_VERSION + OKTA_USERS + OKTA_ENDPOINT_USER_ID, method = PUT)
    ResponseEntity<OktaUser> update(@RequestHeader(AUTHORIZATION_HEADER) String token,
                                    @PathVariable(USER_ID) String userId,
                                    @RequestBody OktaUserUpdateRequest payload);

    /**
     * The the groups assigned to a user.
     *
     * @param token  the api key for the Okta tenant
     * @param userId the id of the user to find groups for
     * @return the list of groups that the user is assigned to
     */
    @RequestMapping(value = OKTA_API_VERSION + OKTA_USERS + OKTA_ENDPOINT_USER_ID + OKTA_GROUPS, method = GET)
    List<OktaGroup> getGroupsForUser(@RequestHeader(AUTHORIZATION_HEADER) String token,
                                     @PathVariable(USER_ID) String userId);

    /**
     * Suspend/disable a user
     *
     * @param userId
     * @return
     */
    @RequestMapping(value = OKTA_API_VERSION + OKTA_USERS + OKTA_ENDPOINT_USER_ID + OKTA_LIFECYCLE + OKTA_SUSPEND, method = POST)
    ResponseEntity<OktaUser> suspend(@RequestHeader("Accept") String accept,
                                     @RequestHeader("Content-Type") String contentType,
                                     @RequestHeader(AUTHORIZATION_HEADER) String token,
                                     @PathVariable(USER_ID) String userId);

    /**
     * Unsuspend/enable a user
     *
     * @param userId
     * @return
     */
    @RequestMapping(value = OKTA_API_VERSION + OKTA_USERS + OKTA_ENDPOINT_USER_ID + OKTA_LIFECYCLE + OKTA_UNSUSPEND, method = POST)
    ResponseEntity<OktaUser> unsuspend(@RequestHeader("Accept") String accept,
                                       @RequestHeader("Content-Type") String contentType,
                                       @RequestHeader(AUTHORIZATION_HEADER) String token,
                                       @PathVariable(USER_ID) String userId);

    /**
     * Unlock user
     *
     * @param userId
     * @param payload
     * @return
     */
    @RequestMapping(value = OKTA_API_VERSION + OKTA_USERS + OKTA_ENDPOINT_USER_ID + OKTA_LIFECYCLE + OKTA_UNLOCK, method = POST)
    ResponseEntity<OktaUser> unlock(@RequestHeader("Accept") String accept,
                                    @RequestHeader("Content-Type") String contentType,
                                    @RequestHeader(AUTHORIZATION_HEADER) String token,
                                    @PathVariable(USER_ID) String userId,
                                    @RequestBody OktaUser payload); //todo: is there a payload required here?

    /**
     * Activate a user in Okta
     *
     * @param sendEmail
     * @param userId
     * @return Response with OktaUser entity
     */
    @RequestMapping(value = OKTA_API_VERSION + OKTA_USERS + OKTA_ENDPOINT_USER_ID + OKTA_LIFECYCLE + OKTA_ACTIVATE, method = POST)
    ResponseEntity<UserActivateResponse> activateUser(@RequestHeader("Accept") String accept,
                                                      @RequestHeader("Content-Type") String contentType,
                                                      @RequestHeader(AUTHORIZATION_HEADER) String token,
                                                      @PathVariable(USER_ID) String userId,
                                                      @RequestParam(OKTA_REQUEST_PARAM_SEND_EMAIL) Boolean sendEmail);

    /**
     * Deactivates a user in Okta
     *
     * @param accept
     * @param contentType
     * @param token
     * @param userId
     * @return
     */
    @RequestMapping(value = OKTA_API_VERSION + OKTA_USERS + OKTA_ENDPOINT_USER_ID + OKTA_LIFECYCLE + OKTA_DEACTIVATE, method = POST)
    ResponseEntity deactivateUser(@RequestHeader("Accept") String accept,
                                  @RequestHeader("Content-Type") String contentType,
                                  @RequestHeader(AUTHORIZATION_HEADER) String token,
                                  @PathVariable(USER_ID) String userId);

    /**
     * Deletes a user in Okta
     *
     * @param accept
     * @param contentType
     * @param token
     * @param userId
     */
    @RequestMapping(value = OKTA_API_VERSION + OKTA_USERS + OKTA_ENDPOINT_USER_ID, method = DELETE)
    void deleteUser(@RequestHeader("Accept") String accept,
                    @RequestHeader("Content-Type") String contentType,
                    @RequestHeader(AUTHORIZATION_HEADER) String token,
                    @PathVariable(USER_ID) String userId);
}
