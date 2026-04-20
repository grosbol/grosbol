package com.example.confluence.sso.util;

import com.example.confluence.sso.config.SSOConfig;
import com.atlassian.confluence.user.UserAccessor;
import com.atlassian.plugin.spring.scanner.annotation.component.ConfluenceComponent;
import com.atlassian.user.Group;
import com.atlassian.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * Creates or updates a Confluence user from SSO attributes.
 *
 * <p>When {@code autoProvisionUsers} is enabled in config:
 * <ul>
 *   <li>Creates the user if they don't exist yet.</li>
 *   <li>Updates email / full name on every login.</li>
 *   <li>Optionally syncs group membership if {@code syncGroupMembership} is enabled.</li>
 * </ul>
 */
@ConfluenceComponent
public class UserProvisioningService {

    private static final Logger log = LoggerFactory.getLogger(UserProvisioningService.class);

    private final UserAccessor userAccessor;

    @Autowired
    public UserProvisioningService(UserAccessor userAccessor) {
        this.userAccessor = userAccessor;
    }

    /**
     * Find or create a Confluence user from SSO user attributes.
     *
     * @param username  derived Confluence username (e.g. email prefix)
     * @param email     user's email address
     * @param fullName  user's display name
     * @param groups    IdP group memberships
     * @param config    current SSO configuration
     * @return the resolved Confluence {@link User}, or {@code null} on failure
     */
    public User findOrProvision(String username, String email, String fullName,
                                List<String> groups, SSOConfig config) {
        // Normalise username
        String normalised = normalise(username);

        // Try to find existing user
        User user = userAccessor.getUserByName(normalised);

        if (user == null) {
            if (!config.isAutoProvisionUsers()) {
                log.warn("User '{}' not found and auto-provisioning is disabled.", normalised);
                return null;
            }
            user = createUser(normalised, email, fullName);
            if (user == null) return null;
        } else {
            // Update mutable attributes
            updateUser(user, email, fullName);
        }

        // Add to default group
        String defaultGroup = config.getDefaultGroup();
        if (defaultGroup != null && !defaultGroup.isBlank()) {
            addToGroup(user, defaultGroup);
        }

        // Sync IdP groups
        if (config.isSyncGroupMembership() && groups != null) {
            syncGroups(user, groups);
        }

        return user;
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private User createUser(String username, String email, String fullName) {
        try {
            User user = userAccessor.createUser(username);
            userAccessor.setEmail(user, email != null ? email : username + "@sso.local");
            userAccessor.setFullName(user, fullName != null ? fullName : username);
            log.info("Auto-provisioned new Confluence user '{}'", username);
            return user;
        } catch (Exception e) {
            log.error("Failed to create user '{}': {}", username, e.getMessage(), e);
            return null;
        }
    }

    private void updateUser(User user, String email, String fullName) {
        try {
            if (email != null && !email.isBlank()) {
                userAccessor.setEmail(user, email);
            }
            if (fullName != null && !fullName.isBlank()) {
                userAccessor.setFullName(user, fullName);
            }
        } catch (Exception e) {
            log.warn("Could not update attributes for user '{}': {}", user.getName(), e.getMessage());
        }
    }

    private void addToGroup(User user, String groupName) {
        try {
            Group group = userAccessor.getGroup(groupName);
            if (group == null) {
                group = userAccessor.createGroup(groupName);
                log.info("Created Confluence group '{}'", groupName);
            }
            if (!userAccessor.hasMembership(group, user)) {
                userAccessor.addMembership(group, user);
                log.debug("Added '{}' to group '{}'", user.getName(), groupName);
            }
        } catch (Exception e) {
            log.warn("Could not add user '{}' to group '{}': {}", user.getName(), groupName, e.getMessage());
        }
    }

    private void syncGroups(User user, List<String> idpGroups) {
        for (String groupName : idpGroups) {
            addToGroup(user, normalise(groupName));
        }
    }

    private String normalise(String value) {
        return value != null ? value.trim().toLowerCase() : "";
    }
}
