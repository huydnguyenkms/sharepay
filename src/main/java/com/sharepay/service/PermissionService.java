package com.sharepay.service;

import com.sharepay.domain.WorkspaceMember;
import com.sharepay.domain.enums.Role;
import com.sharepay.exception.ForbiddenException;
import com.sharepay.repository.WorkspaceMemberRepository;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * Central role-based authorization. Maps each {@link Action} to the workspace roles
 * permitted to perform it, per the spec's permission matrix.
 */
@Service
public class PermissionService {

    public enum Action {
        VIEW,
        ADD_TRANSACTION,
        GENERATE_SETTLEMENT,
        MANAGE_EVENT,
        MANAGE_WORKSPACE
    }

    private static final java.util.Map<Action, Set<Role>> ALLOWED = java.util.Map.of(
            Action.VIEW, Set.of(Role.OWNER, Role.ADMIN, Role.MEMBER, Role.VIEWER),
            Action.ADD_TRANSACTION, Set.of(Role.OWNER, Role.ADMIN, Role.MEMBER),
            Action.GENERATE_SETTLEMENT, Set.of(Role.OWNER, Role.ADMIN),
            Action.MANAGE_EVENT, Set.of(Role.OWNER, Role.ADMIN),
            Action.MANAGE_WORKSPACE, Set.of(Role.OWNER)
    );

    private final WorkspaceMemberRepository workspaceMemberRepository;

    public PermissionService(WorkspaceMemberRepository workspaceMemberRepository) {
        this.workspaceMemberRepository = workspaceMemberRepository;
    }

    public Role roleOf(Long workspaceId, Long userId) {
        return workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .map(WorkspaceMember::getRole)
                .orElseThrow(() -> new ForbiddenException("You are not a member of this workspace"));
    }

    public Role require(Long workspaceId, Long userId, Action action) {
        Role role = roleOf(workspaceId, userId);
        if (!ALLOWED.get(action).contains(role)) {
            throw new ForbiddenException("Your role (" + role + ") cannot perform: " + action);
        }
        return role;
    }
}
