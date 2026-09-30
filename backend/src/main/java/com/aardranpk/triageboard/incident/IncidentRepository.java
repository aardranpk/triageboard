package com.aardranpk.triageboard.incident;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface IncidentRepository extends JpaRepository<Incident, Long> {

    List<Incident> findByStatusNot(IncidentStatus status);
}