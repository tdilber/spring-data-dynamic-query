package com.beyt.jdq.context;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.io.Serializable;


@Component
@RequestScope
public class DBSelectionContext implements Serializable {
    public enum Database {
        READ, WRITE
    }

    private Database database = Database.WRITE;

    public Database getDatabase() {
        return database;
    }

    public void setDatabase(Database database) {
        this.database = database;
    }
}
