package com.sharepay.service;

import com.sharepay.dto.TransactionDtos.CategoryRef;
import com.sharepay.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final EventService eventService;

    public CategoryService(CategoryRepository categoryRepository, EventService eventService) {
        this.categoryRepository = categoryRepository;
        this.eventService = eventService;
    }

    @Transactional(readOnly = true)
    public List<CategoryRef> listForEvent(Long userId, Long eventId) {
        eventService.getViewableEvent(userId, eventId);
        return categoryRepository.findAvailableForEvent(eventId).stream()
                .map(c -> new CategoryRef(c.getId(), c.getName()))
                .toList();
    }
}
