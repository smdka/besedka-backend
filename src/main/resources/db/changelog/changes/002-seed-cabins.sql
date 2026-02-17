-- liquibase formatted sql

-- changeset besedka:2
INSERT INTO cabins (name, location, price_per_hour, description)
VALUES ('Бурцево', 'Бурцево, Россия', 246,
        'Уютный деревянный дом с панорамными окнами, баней на дровах и видом на заснеженные поля.'),
       ('Кирики', 'Кирики, Россия', 320,
        'Тихий загородный домик среди елей с камином, террасой и собственным прудом.');

INSERT INTO cabin_photos (cabin_id, photo_url, position)
VALUES (1, '/assets/burtsevo-main.png', 0),
       (1, '/assets/burtsevo-2.png', 1),
       (1, '/assets/burtsevo-3.png', 2),
       (2, '/assets/kiriki-main.png', 0),
       (2, '/assets/kiriki-2.png', 1),
       (2, '/assets/kiriki-3.png', 2);
