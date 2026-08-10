package com.smarttennislab.match.repository;

import java.util.UUID;

public interface KpiCountBySet {

    UUID getSetId();

    String getKpiCode();

    long getTotal();
}
