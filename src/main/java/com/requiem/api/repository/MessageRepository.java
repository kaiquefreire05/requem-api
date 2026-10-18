package com.requiem.api.repository;

import com.requiem.api.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MessageRepository extends JpaRepository<Message, String> {

    List<Message> findBySessionIdOrderByCreatedAtAsc(String sessionId);

    // Returns the latest message for a session (used in the sessions list summary)
    Optional<Message> findFirstBySessionIdOrderByCreatedAtDesc(String sessionId);
}
