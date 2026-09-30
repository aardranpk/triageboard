package com.aardranpk.triageboard.analyst;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalystRepository extends JpaRepository<Analyst, Long> {

    List<Analyst> findByActiveTrue();
}