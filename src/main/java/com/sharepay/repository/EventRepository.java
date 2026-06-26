package com.sharepay.repository;

import com.sharepay.domain.Event;
import com.sharepay.domain.enums.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findByWorkspaceIdOrderByCreatedAtDesc(Long workspaceId);

    List<Event> findByWorkspaceIdAndStatusOrderByCreatedAtDesc(Long workspaceId, EventStatus status);

    long countByWorkspaceId(Long workspaceId);
}
