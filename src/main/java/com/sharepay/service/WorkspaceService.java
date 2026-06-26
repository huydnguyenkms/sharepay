package com.sharepay.service;

import com.sharepay.domain.User;
import com.sharepay.domain.Workspace;
import com.sharepay.domain.WorkspaceMember;
import com.sharepay.domain.enums.Role;
import com.sharepay.dto.WorkspaceDtos.AddMemberRequest;
import com.sharepay.dto.WorkspaceDtos.UpdateRoleRequest;
import com.sharepay.dto.MemberDtos.KnownMemberResponse;
import com.sharepay.dto.WorkspaceDtos.WorkspaceMemberResponse;
import com.sharepay.dto.WorkspaceDtos.WorkspaceRequest;
import com.sharepay.dto.WorkspaceDtos.WorkspaceResponse;
import com.sharepay.domain.EventMember;
import com.sharepay.exception.BadRequestException;
import com.sharepay.exception.ConflictException;
import com.sharepay.exception.NotFoundException;
import com.sharepay.repository.EventMemberRepository;
import com.sharepay.repository.EventRepository;
import com.sharepay.repository.UserRepository;
import com.sharepay.repository.WorkspaceMemberRepository;
import com.sharepay.repository.WorkspaceRepository;
import com.sharepay.service.PermissionService.Action;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository memberRepository;
    private final EventRepository eventRepository;
    private final EventMemberRepository eventMemberRepository;
    private final UserRepository userRepository;
    private final PermissionService permissionService;

    public WorkspaceService(WorkspaceRepository workspaceRepository,
                            WorkspaceMemberRepository memberRepository,
                            EventRepository eventRepository,
                            EventMemberRepository eventMemberRepository,
                            UserRepository userRepository,
                            PermissionService permissionService) {
        this.workspaceRepository = workspaceRepository;
        this.memberRepository = memberRepository;
        this.eventRepository = eventRepository;
        this.eventMemberRepository = eventMemberRepository;
        this.userRepository = userRepository;
        this.permissionService = permissionService;
    }

    @Transactional
    public WorkspaceResponse create(Long userId, WorkspaceRequest request) {
        User user = requireUser(userId);
        Workspace workspace = new Workspace();
        workspace.setName(request.name().trim());
        workspace.setDescription(request.description());
        workspace = workspaceRepository.save(workspace);

        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(workspace);
        member.setUser(user);
        member.setRole(Role.OWNER);
        memberRepository.save(member);

        return toResponse(workspace, Role.OWNER);
    }

    @Transactional(readOnly = true)
    public List<WorkspaceResponse> listForUser(Long userId) {
        return workspaceRepository.findAllForUser(userId).stream()
                .map(ws -> toResponse(ws, permissionService.roleOf(ws.getId(), userId)))
                .toList();
    }

    @Transactional(readOnly = true)
    public WorkspaceResponse get(Long userId, Long workspaceId) {
        Role role = permissionService.require(workspaceId, userId, Action.VIEW);
        return toResponse(requireWorkspace(workspaceId), role);
    }

    @Transactional
    public WorkspaceResponse update(Long userId, Long workspaceId, WorkspaceRequest request) {
        Role role = permissionService.require(workspaceId, userId, Action.MANAGE_WORKSPACE);
        Workspace workspace = requireWorkspace(workspaceId);
        workspace.setName(request.name().trim());
        workspace.setDescription(request.description());
        return toResponse(workspaceRepository.save(workspace), role);
    }

    @Transactional
    public void delete(Long userId, Long workspaceId) {
        permissionService.require(workspaceId, userId, Action.MANAGE_WORKSPACE);
        if (eventRepository.countByWorkspaceId(workspaceId) > 0) {
            throw new BadRequestException("Delete or move the workspace's events before deleting it");
        }
        Workspace workspace = requireWorkspace(workspaceId);
        memberRepository.findByWorkspaceId(workspaceId).forEach(memberRepository::delete);
        workspaceRepository.delete(workspace);
    }

    // --- Members ---

    @Transactional(readOnly = true)
    public List<WorkspaceMemberResponse> listMembers(Long userId, Long workspaceId) {
        permissionService.require(workspaceId, userId, Action.VIEW);
        return memberRepository.findByWorkspaceId(workspaceId).stream()
                .map(this::toMemberResponse)
                .toList();
    }

    @Transactional
    public WorkspaceMemberResponse addMember(Long userId, Long workspaceId, AddMemberRequest request) {
        permissionService.require(workspaceId, userId, Action.MANAGE_WORKSPACE);
        Workspace workspace = requireWorkspace(workspaceId);
        User target = userRepository.findByEmail(request.email().trim().toLowerCase())
                .orElseThrow(() -> new NotFoundException("No registered user with email " + request.email()));
        if (memberRepository.existsByWorkspaceIdAndUserId(workspaceId, target.getId())) {
            throw new ConflictException("User is already a member of this workspace");
        }
        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(workspace);
        member.setUser(target);
        member.setRole(request.role());
        return toMemberResponse(memberRepository.save(member));
    }

    @Transactional
    public WorkspaceMemberResponse updateRole(Long userId, Long workspaceId, Long memberId, UpdateRoleRequest request) {
        permissionService.require(workspaceId, userId, Action.MANAGE_WORKSPACE);
        WorkspaceMember member = requireMember(workspaceId, memberId);
        if (member.getRole() == Role.OWNER && request.role() != Role.OWNER
                && memberRepository.countByWorkspaceIdAndRole(workspaceId, Role.OWNER) <= 1) {
            throw new BadRequestException("A workspace must keep at least one owner");
        }
        member.setRole(request.role());
        return toMemberResponse(memberRepository.save(member));
    }

    @Transactional
    public void removeMember(Long userId, Long workspaceId, Long memberId) {
        permissionService.require(workspaceId, userId, Action.MANAGE_WORKSPACE);
        WorkspaceMember member = requireMember(workspaceId, memberId);
        if (member.getRole() == Role.OWNER
                && memberRepository.countByWorkspaceIdAndRole(workspaceId, Role.OWNER) <= 1) {
            throw new BadRequestException("Cannot remove the last owner of a workspace");
        }
        memberRepository.delete(member);
    }

    /**
     * Distinct participants seen across all of the workspace's events, so the same people can
     * be reused when adding members to a new event. Deduplicated by email when present,
     * otherwise by (case-insensitive) display name; the most recent occurrence wins.
     */
    @Transactional(readOnly = true)
    public List<KnownMemberResponse> knownMembers(Long userId, Long workspaceId) {
        permissionService.require(workspaceId, userId, Action.VIEW);
        Map<String, KnownMemberResponse> distinct = new LinkedHashMap<>();
        for (EventMember m : eventMemberRepository.findByWorkspaceId(workspaceId)) {
            String key = (m.getEmail() != null && !m.getEmail().isBlank())
                    ? "email:" + m.getEmail().trim().toLowerCase()
                    : "name:" + m.getDisplayName().trim().toLowerCase();
            distinct.putIfAbsent(key, new KnownMemberResponse(
                    m.getDisplayName(),
                    m.getEmail(),
                    m.getPhone(),
                    m.getUser() != null ? m.getUser().getId() : null));
        }
        return List.copyOf(distinct.values());
    }

    // --- helpers ---

    private WorkspaceResponse toResponse(Workspace ws, Role role) {
        return new WorkspaceResponse(
                ws.getId(),
                ws.getName(),
                ws.getDescription(),
                role,
                memberRepository.countByWorkspaceId(ws.getId()),
                eventRepository.countByWorkspaceId(ws.getId()),
                ws.getCreatedAt());
    }

    private WorkspaceMemberResponse toMemberResponse(WorkspaceMember m) {
        User u = m.getUser();
        return new WorkspaceMemberResponse(m.getId(), u.getId(), u.getEmail(), u.getDisplayName(), m.getRole());
    }

    private Workspace requireWorkspace(Long id) {
        return workspaceRepository.findById(id).orElseThrow(() -> NotFoundException.of("Workspace", id));
    }

    private WorkspaceMember requireMember(Long workspaceId, Long memberId) {
        WorkspaceMember member = memberRepository.findById(memberId)
                .orElseThrow(() -> NotFoundException.of("Workspace member", memberId));
        if (!member.getWorkspace().getId().equals(workspaceId)) {
            throw NotFoundException.of("Workspace member", memberId);
        }
        return member;
    }

    private User requireUser(Long id) {
        return userRepository.findById(id).orElseThrow(() -> NotFoundException.of("User", id));
    }
}
