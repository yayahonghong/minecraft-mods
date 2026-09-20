import asyncio
import inspect
import logging
import re
import time
import uuid

from aiohttp import web

from astrbot.api.event import MessageChain, MessageEventResult, filter
from astrbot.api.message_components import Plain
from astrbot.api.star import Context, Star, register

try:
    from astrbot.api import logger as logs
except ImportError:
    logs = logging.getLogger("astrbot")

CQ_PATTERN = re.compile(r"\[CQ:[^\]]*\]")

MENU_KEYWORDS = {"菜单", "帮助", "取消"}


def _as_int(value, default=0):
    try:
        return int(value)
    except (TypeError, ValueError):
        return default


@register("mc_bridge", "ysh", "Minecraft 服务器命令桥接", "1.0.0")
class McBridge(Star):
    """在 AstrBot 进程内起一个仅供 MC 服务器拉取的 HTTP 桥：

    - MC 模组出站 GET  /mc/poll?token=..&wait=秒   长轮询取命令
    - MC 模组出站 POST /mc/reply {token,id,text}   回传执行结果，插件用保存的原 event 回复到群

    需要配置：group_id、internal_token（与 MC 侧一致）、listen_host/port。
    """

    def __init__(self, context: Context, config=None):
        super().__init__(context)
        self.conf = config or {}
        self.group_id = str(self.conf.get("group_id") or "").strip()
        self.prefix = str(self.conf.get("command_prefix") or "#")
        self.host = str(self.conf.get("listen_host") or "127.0.0.1")
        self.port = _as_int(self.conf.get("listen_port"), 6186)
        self.token = str(self.conf.get("internal_token") or "")

        self.queue: asyncio.Queue = asyncio.Queue(maxsize=64)
        self.pending: dict = {}  # id -> (event, ts)
        self.runner = None
        self._loop = None

    async def initialize(self):
        self._loop = asyncio.get_running_loop()
        app = web.Application()
        app.router.add_get("/mc/poll", self._handle_poll)
        app.router.add_post("/mc/reply", self._handle_reply)
        self.runner = web.AppRunner(app)
        await self.runner.setup()
        site = web.TCPSite(self.runner, self.host, self.port)
        await site.start()
        logs.info(f"[mc_bridge] 桥接服务已启动: http://{self.host}:{self.port} (群={self.group_id})")

    async def terminate(self):
        if self.runner:
            await self.runner.cleanup()
            logs.info("[mc_bridge] 桥接服务已停止")

    # ---------- AstrBot 消息入口 ----------

    @filter.event_message_type(filter.EventMessageType.GROUP_MESSAGE)
    async def on_group_message(self, event):
        text = CQ_PATTERN.sub("", event.get_message_str() or "").strip()
        logs.info(
            f"[mc_bridge] 收到群消息 group_id={event.get_group_id()!r} "
            f"sender_id={event.get_sender_id()!r} 配置group_id={self.group_id!r} text={text}"
        )
        if not self.group_id:
            return
        if str(event.get_group_id()) != self.group_id:
            return
        if not text or not self._should_forward(text):
            return

        self._cleanup_pending()
        cmd_id = uuid.uuid4().hex
        if self.queue.full():
            logs.warning("[mc_bridge] 命令队列已满，丢弃新命令")
            return

        self.pending[cmd_id] = (event, time.monotonic())
        await self.queue.put(
            {
                "id": cmd_id,
                "user_id": str(event.get_sender_id()),
                "text": text,
            }
        )
        # 空结果 = 消费该消息不再走后续流水线（回复由 MC 执行后经 /mc/reply 异步发出）
        return MessageEventResult()

    def _should_forward(self, text: str) -> bool:
        if text.startswith(self.prefix):
            return True
        if text in MENU_KEYWORDS:
            return True
        if text.isdigit():
            return True
        return False

    # ---------- 桥接 HTTP ----------

    def _check_token(self, request_token):
        if not self.token:
            return False
        return request_token == self.token

    async def _handle_poll(self, request: web.Request):
        if not self._check_token(request.query.get("token", "")):
            return web.json_response({"error": "unauthorized"}, status=403)

        wait = min(max(_as_int(request.query.get("wait"), 5), 1), 30)
        commands = []
        deadline = time.monotonic() + wait
        while time.monotonic() < deadline:
            remaining = deadline - time.monotonic()
            if remaining <= 0:
                break
            try:
                item = await asyncio.wait_for(self.queue.get(), timeout=remaining)
            except asyncio.TimeoutError:
                break
            if item is not None:
                commands.append(item)
            if len(commands) >= 8:
                break
        return web.json_response({"commands": commands})

    async def _handle_reply(self, request: web.Request):
        try:
            body = await request.json()
        except Exception:
            return web.json_response({"error": "bad json"}, status=400)
        if not self._check_token(body.get("token", "")):
            return web.json_response({"error": "unauthorized"}, status=403)

        cmd_id = body.get("id", "")
        text = str(body.get("text", ""))
        entry = self.pending.pop(cmd_id, None)
        if entry is None:
            return web.json_response({"error": "unknown or expired id"}, status=404)

        event = entry[0]
        try:
            chain = MessageChain(chain=[Plain(text=text)])
            result = event.send(chain)
            if inspect.isawaitable(result):
                await result
            return web.json_response({"status": "ok"})
        except Exception as e:
            logs.error(f"[mc_bridge] 回复失败: {e}")
            return web.json_response({"error": str(e)}, status=500)

    def _cleanup_pending(self):
        now = time.monotonic()
        expired = [k for k, (_, ts) in self.pending.items() if now - ts > 300]
        for k in expired:
            self.pending.pop(k, None)
        if expired:
            logs.warning(f"[mc_bridge] 清理 {len(expired)} 条超时未回传命令")
