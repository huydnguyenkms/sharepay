package com.sharepay.repository;

import com.sharepay.domain.EventMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EventMemberRepository extends JpaRepository<EventMember, Long> {

    List<EventMember> findByEventIdOrderByIdAsc(Long eventId);

    long countByEventId(Long eventId);

    @Query("select m from EventMember m where m.event.workspace.id = :workspaceId order by m.id desc")
    List<EventMember> findByWorkspaceId(@Param("workspaceId") Long workspaceId);
}
