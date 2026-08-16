package org.wowmr.util;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.JsePlatform;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class LootLoggerParser {

    private static final String PATH_ENV = "WOW_LOOTLOGGER_PATH";
    private static final String PATH_PROPERTY = "wow.lootlogger.path";

    public static List<String> loadDrops(File luaFile) {
        Globals g = JsePlatform.standardGlobals();
        g.get("dofile").call(LuaValue.valueOf(luaFile.getAbsolutePath()));
        LuaValue db = g.get("LootLoggerDB");
        if (!db.istable()) return List.of();

        LuaTable sessions = (LuaTable) db;
        LuaValue last = sessions.get(sessions.length());
        if (!last.istable()) return List.of();

        LuaValue items = last.get("items");
        if (!items.istable()) return List.of();

        List<String> out = new ArrayList<>();
        LuaTable tbl = (LuaTable) items;
        for (LuaValue key : tbl.keys()) {
            out.add(tbl.get(key).tojstring());
        }
        return out;
    }

    public static int[] loadMoney(File luaFile) {
        Globals g = JsePlatform.standardGlobals();
        g.get("dofile").call(LuaValue.valueOf(luaFile.getAbsolutePath()));
        LuaValue db = g.get("LootLoggerDB");
        if (!db.istable()) return new int[]{0, 0, 0};

        LuaTable sessions = (LuaTable) db;
        LuaValue last = sessions.get(sessions.length());
        if (!last.istable()) return new int[]{0, 0, 0};

        LuaValue money = last.get("money");
        if (!money.istable()) return new int[]{0, 0, 0};

        LuaTable m = (LuaTable) money;
        return new int[]{
                m.get("gold").toint(),
                m.get("silver").toint(),
                m.get("copper").toint()
        };
    }

    public record ParsedData(List<String> loot, int mobsKilled, int totalCopper) {}

    public static ParsedData parse() {
        File luaFile = getSavedVariablesFile();
        if (luaFile == null || !luaFile.isFile()) {
            return new ParsedData(List.of(), 0, 0);
        }

        List<String> drops = loadDrops(luaFile);
        int[] money = loadMoney(luaFile);
        int copper = money[0] * 10_000 + money[1] * 100 + money[2];

        // The current addon export does not provide a reliable mob-kill count.
        return new ParsedData(drops, 0, copper);
    }

    public static File getSavedVariablesFile() {
        String configuredPath = System.getenv(PATH_ENV);
        if (configuredPath == null || configuredPath.isBlank()) {
            configuredPath = System.getProperty(PATH_PROPERTY);
        }

        if (configuredPath == null || configuredPath.isBlank()) {
            return null;
        }

        File file = new File(configuredPath);
        return file.isFile() ? file : null;
    }
}
