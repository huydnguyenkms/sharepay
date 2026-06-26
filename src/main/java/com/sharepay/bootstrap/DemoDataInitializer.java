package com.sharepay.bootstrap;

import com.sharepay.domain.Category;
import com.sharepay.domain.enums.EventType;
import com.sharepay.domain.enums.SplitMethod;
import com.sharepay.dto.AuthDtos.AuthResponse;
import com.sharepay.dto.AuthDtos.RegisterRequest;
import com.sharepay.dto.EventDtos.EventRequest;
import com.sharepay.dto.EventDtos.EventResponse;
import com.sharepay.dto.MemberDtos.EventMemberRequest;
import com.sharepay.dto.MemberDtos.EventMemberResponse;
import com.sharepay.dto.TransactionDtos.CreateAdjustmentRequest;
import com.sharepay.dto.TransactionDtos.CreateExpenseRequest;
import com.sharepay.dto.TransactionDtos.CreateRefundRequest;
import com.sharepay.dto.TransactionDtos.ParticipantInput;
import com.sharepay.dto.TransactionDtos.PayerInput;
import com.sharepay.dto.TransactionDtos.SponsorInput;
import com.sharepay.dto.WorkspaceDtos.WorkspaceRequest;
import com.sharepay.dto.WorkspaceDtos.WorkspaceResponse;
import com.sharepay.repository.CategoryRepository;
import com.sharepay.repository.UserRepository;
import com.sharepay.service.AuthService;
import com.sharepay.service.EventMemberService;
import com.sharepay.service.EventService;
import com.sharepay.service.TransactionService;
import com.sharepay.service.WorkspaceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Seeds a realistic demo workspace/event (the spec's "Huy & Friends" / "Da Nang Trip 2026")
 * by driving the real service layer so the seeded ledger is guaranteed consistent. Runs on
 * the dev (H2) and docker (Postgres) profiles, but only when the database is empty
 * (no users) — so it never reseeds or touches a populated/production database.
 */
@Component
@Profile({"dev", "docker"})
public class DemoDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataInitializer.class);

    private static final String DEMO_EMAIL = "huy@sharepay.dev";
    private static final String DEMO_PASSWORD = "password123";

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final AuthService authService;
    private final WorkspaceService workspaceService;
    private final EventService eventService;
    private final EventMemberService eventMemberService;
    private final TransactionService transactionService;

    public DemoDataInitializer(UserRepository userRepository,
                               CategoryRepository categoryRepository,
                               AuthService authService,
                               WorkspaceService workspaceService,
                               EventService eventService,
                               EventMemberService eventMemberService,
                               TransactionService transactionService) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.authService = authService;
        this.workspaceService = workspaceService;
        this.eventService = eventService;
        this.eventMemberService = eventMemberService;
        this.transactionService = transactionService;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }
        seedDefaultCategories();

        AuthResponse auth = authService.register(new RegisterRequest(DEMO_EMAIL, DEMO_PASSWORD, "Huy Nguyen"));
        Long userId = auth.user().id();

        WorkspaceResponse workspace = workspaceService.create(userId,
                new WorkspaceRequest("Huy & Friends", "Friends who travel and eat together"));

        EventResponse event = eventService.create(userId, workspace.id(),
                new EventRequest("Da Nang Trip 2026", "Beach trip with the crew",
                        EventType.TRAVEL, "VND", LocalDate.of(2026, 6, 4), LocalDate.of(2026, 6, 6)));
        Long eventId = event.id();

        Map<String, Long> members = new LinkedHashMap<>();
        for (String name : List.of("Alice", "Bob", "Charlie", "David", "Emma", "Fiona")) {
            EventMemberResponse m = eventMemberService.add(userId, eventId,
                    new EventMemberRequest(name, name.toLowerCase() + "@example.com", null, null));
            members.put(name, m.id());
        }

        Map<String, Long> categories = new LinkedHashMap<>();
        categoryRepository.findByEventIsNullOrderByIdAsc()
                .forEach(c -> categories.put(c.getName(), c.getId()));

        // Hotel: Alice paid, split equally among everyone.
        transactionService.createExpense(userId, eventId, new CreateExpenseRequest(
                "Hotel - Hanami Hotel", categories.get("Accommodation"), bd("5400000"),
                LocalDate.of(2026, 6, 4), "Deluxe room 2 nights with breakfast", SplitMethod.EQUAL,
                List.of(new PayerInput(members.get("Alice"), bd("5400000"))),
                equalParticipants(members.values()), List.of()));

        // Team Dinner: Bob paid, split equally.
        transactionService.createExpense(userId, eventId, new CreateExpenseRequest(
                "Team Dinner", categories.get("Food"), bd("2450000"),
                LocalDate.of(2026, 6, 4), "Seafood restaurant", SplitMethod.EQUAL,
                List.of(new PayerInput(members.get("Bob"), bd("2450000"))),
                equalParticipants(members.values()), List.of()));

        // Taxi: Charlie paid, only Alice/Bob/Charlie share it.
        transactionService.createExpense(userId, eventId, new CreateExpenseRequest(
                "Taxi to Ba Na Hills", categories.get("Transportation"), bd("800000"),
                LocalDate.of(2026, 6, 5), null, SplitMethod.EQUAL,
                List.of(new PayerInput(members.get("Charlie"), bd("800000"))),
                List.of(new ParticipantInput(members.get("Alice"), null),
                        new ParticipantInput(members.get("Bob"), null),
                        new ParticipantInput(members.get("Charlie"), null)),
                List.of()));

        // Welcome Dinner: Alice paid; Emma sponsors 750,000; the rest is split among everyone.
        transactionService.createExpense(userId, eventId, new CreateExpenseRequest(
                "Welcome Dinner", categories.get("Food"), bd("2000000"),
                LocalDate.of(2026, 6, 4), "Emma treated part of dinner", SplitMethod.EQUAL,
                List.of(new PayerInput(members.get("Alice"), bd("2000000"))),
                equalParticipants(members.values()),
                List.of(new SponsorInput(members.get("Emma"), bd("750000")))));

        // Hotel refund: Alice received 500,000 back, credited to everyone equally.
        transactionService.createRefund(userId, eventId, new CreateRefundRequest(
                "Refund - Hotel", categories.get("Accommodation"), bd("500000"),
                LocalDate.of(2026, 6, 6), "One night discounted",
                members.get("Alice"), members.values().stream().toList()));

        // Adjustment: Bob owes Alice an extra 100,000 (manual correction).
        transactionService.createAdjustment(userId, eventId, new CreateAdjustmentRequest(
                "Manual correction", bd("100000"), LocalDate.of(2026, 6, 6),
                "Alice covered a small extra cost",
                members.get("Bob"), members.get("Alice")));

        log.info("=================================================================");
        log.info(" SharePay demo data seeded. Log in with:");
        log.info("   email:    {}", DEMO_EMAIL);
        log.info("   password: {}", DEMO_PASSWORD);
        log.info(" Workspace 'Huy & Friends' -> Event 'Da Nang Trip 2026' (id {})", eventId);
        log.info("=================================================================");
    }

    private void seedDefaultCategories() {
        if (!categoryRepository.findByEventIsNullOrderByIdAsc().isEmpty()) {
            return;
        }
        for (String name : List.of("Accommodation", "Food", "Transportation", "Entertainment",
                "Shopping", "Ticket", "Equipment", "Miscellaneous")) {
            categoryRepository.save(new Category(null, name));
        }
    }

    private List<ParticipantInput> equalParticipants(java.util.Collection<Long> memberIds) {
        return memberIds.stream().map(id -> new ParticipantInput(id, null)).toList();
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }
}
