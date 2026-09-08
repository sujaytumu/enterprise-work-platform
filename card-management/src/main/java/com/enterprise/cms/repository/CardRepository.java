package com.enterprise.cms.repository;

import com.enterprise.cms.model.Card;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CardRepository extends JpaRepository<Card, String> {
    List<Card> findByCustomerId(String customerId);
}
