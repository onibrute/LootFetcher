package org.wowmr.util;

import org.luaj.vm2.*;
import org.luaj.vm2.lib.jse.JsePlatform;

import java.io.File;
import java.util.*;

public class LootLoggerParser {

    public static List<String> loadDrops(File luaFile) {
        Globals g = JsePlatform.standardGlobals();
        g.get("dofile").call(LuaValue.valueOf(luaFile.getAbsolutePath()));
        LuaValue db = g.get("LootLoggerDB");
        if (!db.istable()) return List.of();
        LuaTable sessions = (LuaTable)db;
        LuaValue last = sessions.get(sessions.length());
        if (!last.istable()) return List.of();
        LuaValue items = last.get("items");
        if (!items.istable())    return List.of();
        List<String> out = new ArrayList<>();
        LuaTable tbl = (LuaTable)items;
        for (LuaValue key : tbl.keys()) {
            out.add(tbl.get(key).tojstring());
        }
        return out;
    }

    public static int[] loadMoney(File luaFile) {
        Globals g = JsePlatform.standardGlobals();
        g.get("dofile").call(LuaValue.valueOf(luaFile.getAbsolutePath()));
        LuaValue db = g.get("LootLoggerDB");
        if (!db.istable()) return new int[]{0,0,0};
        LuaTable sessions = (LuaTable)db;
        LuaValue last = sessions.get(sessions.length());
        if (!last.istable()) return new int[]{0,0,0};
        LuaValue mon = last.get("money");
        if (!mon.istable()) return new int[]{0,0,0};
        LuaTable m = (LuaTable)mon;
        return new int[]{
                m.get("gold").toint(),
                m.get("silver").toint(),
                m.get("copper").toint()
        };
    }

    public static class ParsedData {
        private final List<String> loot;
        private final int mobsKilled;
        private final int totalCopper;

        public ParsedData(List<String> loot, int mobsKilled, int totalCopper) {
            this.loot = loot;
            this.mobsKilled = mobsKilled;
            this.totalCopper = totalCopper;
        }

        public List<String> loot() {
            return loot;
        }

        public int mobsKilled() {
            return mobsKilled;
        }

        public int totalCopper() {
            return totalCopper;
        }
    }
    public static ParsedData parse() {
        File luaFile = getSavedVariablesFile(); // metoda deja existentă
        if (luaFile == null || !luaFile.exists()) {
            return new ParsedData(List.of(), 0, 0);
        }

        List<String> drops = loadDrops(luaFile);    // deja există
        int[] money = loadMoney(luaFile);           // deja există
        int copper = money[0] * 10000 + money[1] * 100 + money[2];

        return new ParsedData(drops, 0, copper); // mobsKilled = 0 (default)
    }
    public static File getSavedVariablesFile() {
        String home = System.getProperty("user.home");
        File path = new File(home,
                "Documents\\WoW 10.2.7 - Firestorm\\WoW 10.2.7 - Firestorm\\WTF\\Account\\seriosfrate2002@gmail.com\\SavedVariables\\LootLogger.lua"
        );
        return path.exists() ? path : null;
    }

}
