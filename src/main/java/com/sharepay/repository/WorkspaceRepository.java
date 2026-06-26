package com.sharepay.repository;

import com.sharepay.domain.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {

    @Query("""
            select w from Workspace w
            join WorkspaceMember m on m.workspace = w
            where m.user.id = :userId
            order by w.createdAt desc
            """)
    List<Workspace> findAllForUser(Long userId);
}
