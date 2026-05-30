package com.ysh.serverhelper.ws;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class KeyboardBuilder {

    public static JsonObject buildButton(String text, String command) {
        JsonObject btn = new JsonObject();
        btn.addProperty("text", text);
        btn.addProperty("action_type", 2);
        btn.addProperty("command", command);
        return btn;
    }

    public static JsonArray buildRow(JsonObject... buttons) {
        JsonArray row = new JsonArray();
        for (JsonObject btn : buttons) {
            row.add(btn);
        }
        return row;
    }

    public static JsonObject buildKeyboard(JsonArray... rows) {
        JsonArray allRows = new JsonArray();
        for (JsonArray row : rows) {
            allRows.add(row);
        }
        JsonObject keyboard = new JsonObject();
        keyboard.add("rows", allRows);
        return keyboard;
    }

    public static JsonObject buildDefaultMenuKeyboard() {
        return buildKeyboard(
                buildRow(
                        buildButton("👥 在线玩家", "#list"),
                        buildButton("📊 状态", "#status")
                ),
                buildRow(
                        buildButton("🎮 TPS", "#tps"),
                        buildButton("🔄 刷新菜单", "#menu")
                )
        );
    }
}
