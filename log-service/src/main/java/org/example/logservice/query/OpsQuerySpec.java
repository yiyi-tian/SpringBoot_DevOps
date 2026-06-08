package org.example.logservice.query;

import java.util.ArrayList;
import java.util.List;

public class OpsQuerySpec {

    private final String whereClause;
    private final List<Object> args;
    private final String orderBy;
    private final long startMs;
    private final long endMs;

    public OpsQuerySpec(String whereClause, List<Object> args, String orderBy, long startMs, long endMs) {
        this.whereClause = whereClause;
        this.args = new ArrayList<>(args);
        this.orderBy = orderBy;
        this.startMs = startMs;
        this.endMs = endMs;
    }

    public String getWhereClause() {
        return whereClause;
    }

    public List<Object> getArgs() {
        return new ArrayList<>(args);
    }

    public String getOrderBy() {
        return orderBy;
    }

    public long getStartMs() {
        return startMs;
    }

    public long getEndMs() {
        return endMs;
    }
}
