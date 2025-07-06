package org.wowmr;

import org.wowmr.db.DatabaseHelper;
import org.wowmr.db.Session;
import java.util.List;

public class SmokeTest {
    public static void main(String[] args) {
        DatabaseHelper.initDatabase();
        List<Session> all = DatabaseHelper.getAllSessions();
        if (all.isEmpty()) System.out.println("no sessions yet");
        else all.forEach(System.out::println);
    }
}
