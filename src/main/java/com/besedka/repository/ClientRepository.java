package com.besedka.repository;

import com.besedka.model.Client;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClientRepository extends JpaRepository<Client, Long> {
    Optional<Client> findByTelegramUserId(Long telegramUserId);
}
