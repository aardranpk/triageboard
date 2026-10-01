package com.aardranpk.triageboard.incident;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncidentRepository extends JpaRepository<Incident, Long> {

    List<Incident> findByStatusNot(IncidentStatus status);

    @EntityGraph(attributePaths = "assignee")
    List<Incident> findByStatus(IncidentStatus status, Sort sort);

    @Override
    @EntityGraph(attributePaths = "assignee")
    List<Incident> findAll(Sort sort);
}