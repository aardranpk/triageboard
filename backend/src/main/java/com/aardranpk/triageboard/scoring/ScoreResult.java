package com.aardranpk.triageboard.scoring;

import com.aardranpk.triageboard.incident.Severity;

public record ScoreResult(Severity severity, int riskScore) {
}