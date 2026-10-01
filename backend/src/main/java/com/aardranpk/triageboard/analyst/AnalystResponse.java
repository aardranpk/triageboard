package com.aardranpk.triageboard.analyst;

public record AnalystResponse(Long id, String name, String email, boolean active) {

    public static AnalystResponse from(Analyst analyst) {
        return new AnalystResponse(analyst.getId(), analyst.getName(),
                analyst.getEmail(), analyst.isActive());
    }
}