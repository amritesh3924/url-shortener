package com.example.shortener.repository;

import com.example.shortener.model.Click;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ClickRepository extends JpaRepository<Click, Long> {
    List<Click> findAllByShortCode(String shortCode);
}
