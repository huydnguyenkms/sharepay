package com.sharepay.web;

import com.sharepay.dto.MemberDtos.KnownMemberResponse;
import com.sharepay.dto.WorkspaceDtos.AddMemberRequest;
import com.sharepay.dto.WorkspaceDtos.UpdateRoleRequest;
import com.sharepay.dto.WorkspaceDtos.WorkspaceMemberResponse;
import com.sharepay.dto.WorkspaceDtos.WorkspaceRequest;
import com.sharepay.dto.WorkspaceDtos.WorkspaceResponse;
import com.sharepay.security.AppUserPrincipal;
import com.sharepay.service.WorkspaceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/workspaces")
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    public WorkspaceController(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    @GetMapping
    public List<WorkspaceResponse> list(@AuthenticationPrincipal AppUserPrincipal principal) {
        return workspaceService.listForUser(principal.getId());
    }

    @PostMapping
    public ResponseEntity<WorkspaceResponse> create(@AuthenticationPrincipal AppUserPrincipal principal,
                                                    @Valid @RequestBody WorkspaceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(workspaceService.create(principal.getId(), request));
    }

    @GetMapping("/{workspaceId}")
    public WorkspaceResponse get(@AuthenticationPrincipal AppUserPrincipal principal,
                                 @PathVariable Long workspaceId) {
        return workspaceService.get(principal.getId(), workspaceId);
    }

    @PutMapping("/{workspaceId}")
    public WorkspaceResponse update(@AuthenticationPrincipal AppUserPrincipal principal,
                                    @PathVariable Long workspaceId,
                                    @Valid @RequestBody WorkspaceRequest request) {
        return workspaceService.update(principal.getId(), workspaceId, request);
    }

    @DeleteMapping("/{workspaceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AppUserPrincipal principal,
                       @PathVariable Long workspaceId) {
        workspaceService.delete(principal.getId(), workspaceId);
    }

    // --- Members ---

    @GetMapping("/{workspaceId}/known-members")
    public List<KnownMemberResponse> knownMembers(@AuthenticationPrincipal AppUserPrincipal principal,
                                                  @PathVariable Long workspaceId) {
        return workspaceService.knownMembers(principal.getId(), workspaceId);
    }

    @GetMapping("/{workspaceId}/members")
    public List<WorkspaceMemberResponse> members(@AuthenticationPrincipal AppUserPrincipal principal,
                                                 @PathVariable Long workspaceId) {
        return workspaceService.listMembers(principal.getId(), workspaceId);
    }

    @PostMapping("/{workspaceId}/members")
    public ResponseEntity<WorkspaceMemberResponse> addMember(@AuthenticationPrincipal AppUserPrincipal principal,
                                                             @PathVariable Long workspaceId,
                                                             @Valid @RequestBody AddMemberRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(workspaceService.addMember(principal.getId(), workspaceId, request));
    }

    @PutMapping("/{workspaceId}/members/{memberId}")
    public WorkspaceMemberResponse updateRole(@AuthenticationPrincipal AppUserPrincipal principal,
                                              @PathVariable Long workspaceId,
                                              @PathVariable Long memberId,
                                              @Valid @RequestBody UpdateRoleRequest request) {
        return workspaceService.updateRole(principal.getId(), workspaceId, memberId, request);
    }

    @DeleteMapping("/{workspaceId}/members/{memberId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(@AuthenticationPrincipal AppUserPrincipal principal,
                             @PathVariable Long workspaceId,
                             @PathVariable Long memberId) {
        workspaceService.removeMember(principal.getId(), workspaceId, memberId);
    }
}
