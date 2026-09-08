package com.enterprise.vault.repository;

import com.enterprise.vault.model.TokenMapping;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TokenMappingRepository extends JpaRepository<TokenMapping, String> {
}
