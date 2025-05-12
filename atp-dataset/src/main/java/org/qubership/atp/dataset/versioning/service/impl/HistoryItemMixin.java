package org.qubership.atp.dataset.versioning.service.impl;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnore;

public abstract class HistoryItemMixin {
    @JsonIgnore
    private BigDecimal commitId;

    @JsonIgnore
    public abstract BigDecimal getCommitId();
}
