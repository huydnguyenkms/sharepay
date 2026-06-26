package com.sharepay.repository;

import com.sharepay.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByEventIsNullOrderByIdAsc();

    List<Category> findByEventId(Long eventId);

    @Query("select c from Category c where c.event is null or c.event.id = :eventId order by c.id asc")
    List<Category> findAvailableForEvent(@Param("eventId") Long eventId);
}
