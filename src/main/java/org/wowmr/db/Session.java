package org.wowmr.db;

import java.util.List;

public record Session(
        String date,
        int durationSeconds,
        int mobsKilled,
        int totalCopper,
        List<String> loot
) {}
