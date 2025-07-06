package org.wowmr.db;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper {
    private static final String DB_URL = "jdbc:sqlite:session_data.db";

    public static void initDatabase() {
        try (var conn = DriverManager.getConnection(DB_URL);
             var stmt = conn.createStatement()) {
            stmt.execute("""
        CREATE TABLE IF NOT EXISTS sessions (
          id INTEGER PRIMARY KEY AUTOINCREMENT,
          date TEXT NOT NULL,
          duration INTEGER NOT NULL,
          mobs INTEGER NOT NULL,
          copper INTEGER NOT NULL,
          loot TEXT
        )
      """);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void insertSession(Session s) {
        String sql = "INSERT INTO sessions(date,duration,mobs,copper,loot) VALUES(?,?,?,?,?)";
        try (var conn = DriverManager.getConnection(DB_URL);
             var ps   = conn.prepareStatement(sql)) {
            ps.setString(1, s.date());
            ps.setInt   (2, s.durationSeconds());
            ps.setInt   (3, s.mobsKilled());
            ps.setInt   (4, s.totalCopper());
            String lootCsv = String.join(",", s.loot());
            ps.setString(5, lootCsv);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static List<Session> getAllSessions() {
        List<Session> out = new ArrayList<>();
        String sql = "SELECT * FROM sessions ORDER BY id DESC";
        try (var conn = DriverManager.getConnection(DB_URL);
             var stmt = conn.createStatement();
             var rs   = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String date   = rs.getString("date");
                int    dur    = rs.getInt("duration");
                int    mobs   = rs.getInt("mobs");
                int    copper = rs.getInt("copper");
                String loot   = rs.getString("loot");
                List<String> lootList = loot == null || loot.isBlank()
                        ? List.of()
                        : List.of(loot.split(","));
                out.add(new Session(date, dur, mobs, copper, lootList));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return out;
    }
}
