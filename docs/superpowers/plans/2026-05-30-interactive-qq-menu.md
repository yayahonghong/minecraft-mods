# Interactive QQ Menu Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a text-based numbered menu to the QQ bot when users send `#menu` or `#help`.

**Background:** NapCat's OneBot v11 does not support the `keyboard` parameter in `send_group_msg`. Switching from clickable keyboard buttons to text menu + numeric reply.

**Architecture:** New `MenuSession` tracks per-user menu state (60s timeout). `QQCommandHandler` intercepts numeric replies for users with active session. `#cancel` to exit menu.

**Tech Stack:** Java 25, Gson, concurrent state management

---

### Task 1: Delete KeyboardBuilder

- [x] **Step 1: Remove file**
- [ ] **Step 2: Remove any references (already done in Task 4)**

---

### Task 2: Remove keyboard sendAction overload

- [x] **Step 1: Remove `sendAction(action, params, keyboard)` from QQWSClient**

---

### Task 3: Create MenuSession

**Files:**
- Create: `serverhelper/src/main/java/com/ysh/serverhelper/qqcmd/MenuSession.java`

- [x] **Step 1: Create the file**

```java
package com.ysh.serverhelper.qqcmd;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MenuSession {
    private static final long TIMEOUT_SECONDS = 60;
    private static final Map<Long, MenuSession> SESSIONS = new ConcurrentHashMap<>();

    private final long userId;
    private final long groupId;
    private final List<MenuItem> items;
    private final Instant createdAt;

    public record MenuItem(String label, String action) {}

    public MenuSession(long userId, long groupId, List<MenuItem> items) {
        this.userId = userId;
        this.groupId = groupId;
        this.items = items;
        this.createdAt = Instant.now();
    }

    public static boolean hasActive(long userId) { ... }
    public static MenuSession get(long userId) { ... }
    public static void set(MenuSession s) { ... }
    public static void remove(long userId) { ... }
    // ...
}
```

---

### Task 4: Rewrite QQCommandHandler with text menu

**Files:**
- Modify: `serverhelper/src/main/java/com/ysh/serverhelper/qqcmd/QQCommandHandler.java`

- [x] **Step 1: Rewrite handler logic**

New `handle()` flow:
1. Parse JSON, extract userId/groupId/message
2. If message starts with prefix:
   - `#cancel` → clear session if active
   - `#menu` or `#help` → create MenuSession, send numbered list
   - Other commands → execute normally
3. If user has active MenuSession → parse as number, execute menu item
4. Otherwise → ignore

---

### Task 5: Build and Verify

- [x] **Step 1: Compile**
- [x] **Step 2: Run tests**
- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "fix: NapCat 不支持 keyboard，改为文字菜单交互"
```
