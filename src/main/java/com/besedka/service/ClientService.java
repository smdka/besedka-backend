package com.besedka.service;

import com.besedka.model.Client;
import com.besedka.repository.ClientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClientService {

    private final ClientRepository clientRepository;

    public ClientService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    @Transactional
    public Client upsert(Long telegramUserId, String firstName, String lastName, String username) {
        return clientRepository.findByTelegramUserId(telegramUserId)
                .map(c -> {
                    c.setFirstName(firstName);
                    c.setLastName(lastName);
                    c.setUsername(username);
                    return c;
                })
                .orElseGet(() -> {
                    Client client = new Client();
                    client.setTelegramUserId(telegramUserId);
                    client.setFirstName(firstName);
                    client.setLastName(lastName);
                    client.setUsername(username);
                    return clientRepository.save(client);
                });
    }
}
