package com.aardranpk.triageboard.analyst;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aardranpk.triageboard.common.DuplicateResourceException;

@Service
@Transactional
public class AnalystService {

    private final AnalystRepository analystRepository;

    public AnalystService(AnalystRepository analystRepository) {
        this.analystRepository = analystRepository;
    }

    public AnalystResponse create(CreateAnalystRequest request) {
        if (analystRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException(
                    "Analyst with email " + request.email() + " already exists");
        }
        Analyst saved = analystRepository.save(new Analyst(request.name(), request.email()));
        return AnalystResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<AnalystResponse> list() {
        return analystRepository.findAll(Sort.by("name")).stream()
                .map(AnalystResponse::from)
                .toList();
    }
}