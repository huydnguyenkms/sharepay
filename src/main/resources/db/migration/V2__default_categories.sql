-- Global default expense categories (event_id is null). Shared by every event.
INSERT INTO categories (event_id, name) VALUES
    (NULL, 'Accommodation'),
    (NULL, 'Food'),
    (NULL, 'Transportation'),
    (NULL, 'Entertainment'),
    (NULL, 'Shopping'),
    (NULL, 'Ticket'),
    (NULL, 'Equipment'),
    (NULL, 'Miscellaneous');
