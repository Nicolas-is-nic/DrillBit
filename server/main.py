# DrillBit 题库分发、笔记备份与账号云同步服务（FastAPI 单文件）
#
# 接口：
#   GET  /api/index          题库目录（双轨鉴权：账号 token 或静态 token）
#   GET  /api/banks/{bank_id} 题库全量 JSON（双轨鉴权）
#   GET  /api/img/{bank_id}/{filename} 题图文件（recall 题型，双轨鉴权）
#   POST /api/notes           笔记全量备份，过渡期保留（双轨鉴权）
#   POST /api/auth/login      账号登录，返回 token（仅账号 token 用于 sync 三端点）
#   POST /api/sync/upload     云同步上传：全量快照覆盖（仅账号 token）
#   GET  /api/sync/download   云同步下载：取最新快照（仅账号 token）
#   GET  /api/sync/meta       快照元信息：设备名/时间/字节数（仅账号 token）
#
# 题库文件放本目录 banks/ 下，结构与 App 端 test_banks.json 中的单个题库一致：
#   {"id": "...", "name": "...", "version": N, "updatedAt": "MM-dd", "questions": [...]}
# 更新题库时：改 JSON 内容并把 version 加一即可，App 端同步时会对比版本。
#
# 账号与数据存本目录 data.sqlite3（标准库 sqlite3，零额外依赖）。
# 建号（服务器上执行，App 内不提供注册）：
#   python3 main.py adduser <用户名>
#
# 部署（公网 + https 建议由 nginx/caddy 终结 TLS）：
#   pip install fastapi uvicorn
#   DRILLBIT_TOKEN=你的token uvicorn main:app --host 0.0.0.0 --port 8000

import asyncio
import getpass
import hashlib
import hmac
import json
import os
import secrets
import sqlite3
import sys
import time
from pathlib import Path

from fastapi import Depends, FastAPI, Header, HTTPException, Request
from fastapi.responses import FileResponse, JSONResponse

app = FastAPI(title="DrillBit Server")
BANKS_DIR = Path(__file__).parent / "banks"
NOTES_FILE = Path(__file__).parent / "notes_backup.json"
DB_FILE = Path(__file__).parent / "data.sqlite3"
TOKEN = os.environ.get("DRILLBIT_TOKEN", "")

# 同步包必须包含的五类数据键（与 agent_docs/账号与云同步方案.md 第三节一致）
PAYLOAD_KEYS = ["notes", "favorites", "deletedQuestions", "progress", "wrongs"]


# ---------------------------------------------------------------------------
# SQLite：users（账号 + 登录 token）与 snapshots（每用户一份全量快照）
# ---------------------------------------------------------------------------

def db() -> sqlite3.Connection:
    conn = sqlite3.connect(DB_FILE)
    conn.row_factory = sqlite3.Row
    return conn


def init_db() -> None:
    with db() as conn:
        conn.execute(
            "CREATE TABLE IF NOT EXISTS users ("
            "username TEXT PRIMARY KEY, password_hash TEXT NOT NULL, salt TEXT NOT NULL, "
            "token TEXT, created_at INTEGER NOT NULL)"
        )
        conn.execute(
            "CREATE TABLE IF NOT EXISTS snapshots ("
            "username TEXT PRIMARY KEY, payload TEXT NOT NULL, "
            "device TEXT NOT NULL, uploaded_at INTEGER NOT NULL)"
        )


def hash_password(password: str, salt_hex: str) -> str:
    """pbkdf2_hmac-sha256 十万轮（标准库实现，不引入 bcrypt 等依赖）"""
    return hashlib.pbkdf2_hmac(
        "sha256", password.encode("utf-8"), bytes.fromhex(salt_hex), 100_000
    ).hex()


# ---------------------------------------------------------------------------
# 建号 CLI：python3 main.py adduser <用户名>
# uvicorn 导入本模块时 sys.argv 不含 adduser，不会误入该分支
# ---------------------------------------------------------------------------

if len(sys.argv) > 1 and sys.argv[1] == "adduser":
    if len(sys.argv) != 3:
        print("用法: python3 main.py adduser <用户名>")
        sys.exit(1)
    init_db()
    username = sys.argv[2].strip()
    # 支持非交互建号：环境变量 DRILLBIT_NEW_PASSWORD 传入（ssh 远程执行用），否则交互输密码
    env_password = os.environ.get("DRILLBIT_NEW_PASSWORD", "")
    if env_password:
        password = confirm = env_password
    else:
        password = getpass.getpass("设置密码: ")
        confirm = getpass.getpass("再输一遍: ")
    if not username or not password or password != confirm:
        print("用户名或密码为空，或两次密码不一致")
        sys.exit(1)
    salt = secrets.token_hex(16)
    try:
        with db() as conn:
            conn.execute(
                "INSERT INTO users(username, password_hash, salt, token, created_at) VALUES(?,?,?,?,?)",
                (username, hash_password(password, salt), salt, None, int(time.time() * 1000)),
            )
    except sqlite3.IntegrityError:
        print(f"用户已存在：{username}")
        sys.exit(1)
    print(f"已建号：{username}")
    sys.exit(0)

init_db()


# ---------------------------------------------------------------------------
# 鉴权：账号 token（sync 专用）与双轨（题库/笔记旧接口过渡期）
# ---------------------------------------------------------------------------

def bearer_value(authorization: str) -> str:
    if authorization.startswith("Bearer "):
        return authorization[len("Bearer "):].strip()
    return ""


def username_of_token(token: str) -> str | None:
    if not token:
        return None
    with db() as conn:
        row = conn.execute(
            "SELECT username FROM users WHERE token = ?", (token,)
        ).fetchone()
    return row["username"] if row else None


async def check_account(authorization: str = Header(default="")) -> str:
    """账号 token 鉴权（sync 三端点专用），通过返回用户名"""
    username = username_of_token(bearer_value(authorization))
    if username is None:
        raise HTTPException(status_code=401, detail="账号未登录或 token 失效")
    return username


async def check_token_dual(authorization: str = Header(default="")) -> None:
    """双轨鉴权（过渡期）：静态 DRILLBIT_TOKEN 或账号 token 任一通过即可"""
    token = bearer_value(authorization)
    if TOKEN and hmac.compare_digest(token, TOKEN):
        return
    if username_of_token(token) is not None:
        return
    raise HTTPException(status_code=401, detail="Token 不正确")


# ---------------------------------------------------------------------------
# 题库与笔记备份（原有接口，鉴权改为双轨）
# ---------------------------------------------------------------------------

def load_bank(bank_id: str) -> dict:
    path = BANKS_DIR / f"{bank_id}.json"
    if not path.exists():
        raise HTTPException(status_code=404, detail=f"题库不存在：{bank_id}")
    return json.loads(path.read_text(encoding="utf-8"))


# 目录元信息缓存（review F-16：原每次请求全量解析 MB 级 JSON 且同步 IO 阻塞事件循环；
# 按「文件名+mtime+size」指纹失效，覆盖同名文件更新也能感知）
_INDEX_CACHE: dict = {"fingerprint": None, "items": []}


def bank_index_items() -> list:
    entries = sorted(BANKS_DIR.glob("*.json"))
    fingerprint = tuple((p.name, p.stat().st_mtime_ns, p.stat().st_size) for p in entries)
    if _INDEX_CACHE["fingerprint"] == fingerprint:
        return _INDEX_CACHE["items"]
    items = []
    for path in entries:
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
            items.append(
                {
                    "id": data["id"],
                    "name": data["name"],
                    "version": data["version"],
                    "updatedAt": data.get("updatedAt", ""),
                    "questionCount": len(data.get("questions", [])),
                    "sortKey": data.get("sortKey", 9999),
                    "category": data.get("category", "knowledge"),
                }
            )
        except (json.JSONDecodeError, KeyError):
            # 单个题库文件损坏不影响目录整体返回
            continue
    _INDEX_CACHE["fingerprint"] = fingerprint
    _INDEX_CACHE["items"] = items
    return items


@app.get("/api/index")
async def api_index(_: None = Depends(check_token_dual)) -> JSONResponse:
    return JSONResponse(bank_index_items())


@app.get("/api/banks/{bank_id}")
async def api_bank(bank_id: str, _: None = Depends(check_token_dual)) -> JSONResponse:
    return JSONResponse(load_bank(bank_id))


@app.get("/api/img/{bank_id}/{filename}")
async def api_img(bank_id: str, filename: str, _: None = Depends(check_token_dual)) -> FileResponse:
    """题图（recall 题型）：banks/img/{bank_id}/{filename}；resolve 后校验仍在图片目录内，防目录穿越"""
    base = (BANKS_DIR / "img" / bank_id).resolve()
    path = (base / filename).resolve()
    if base not in path.parents or not path.is_file():
        raise HTTPException(status_code=404, detail="题图不存在")
    return FileResponse(path)


@app.post("/api/notes")
async def api_notes(request: Request, _: None = Depends(check_token_dual)) -> JSONResponse:
    """全量覆盖式备份：请求体即 App 端全部笔记数组，服务器只留最新快照"""
    try:
        body = await request.json()
    except Exception:
        raise HTTPException(status_code=400, detail="请求体不是合法 JSON")
    if not isinstance(body, list):
        raise HTTPException(status_code=400, detail="请求体应为笔记数组")
    # 先写临时文件再原子改名，避免写入中断留下半截备份
    tmp = NOTES_FILE.with_suffix(".tmp")
    tmp.write_text(
        json.dumps({"uploadedAt": time.strftime("%Y-%m-%d %H:%M:%S"), "notes": body}, ensure_ascii=False),
        encoding="utf-8",
    )
    tmp.replace(NOTES_FILE)
    return JSONResponse({"detail": f"已备份 {len(body)} 条笔记"})


# ---------------------------------------------------------------------------
# 账号登录与云同步
# ---------------------------------------------------------------------------

@app.post("/api/auth/login")
async def api_login(request: Request) -> JSONResponse:
    try:
        body = await request.json()
    except Exception:
        raise HTTPException(status_code=400, detail="请求体不是合法 JSON")
    username = str(body.get("username", "")).strip()
    password = str(body.get("password", ""))
    if not username or not password:
        raise HTTPException(status_code=400, detail="用户名或密码为空")
    with db() as conn:
        row = conn.execute(
            "SELECT password_hash, salt FROM users WHERE username = ?", (username,)
        ).fetchone()
    if row is None or not hmac.compare_digest(row["password_hash"], hash_password(password, row["salt"])):
        # 慢化失败响应，降低爆破收益
        await asyncio.sleep(0.3)  # review F-19：不阻塞事件循环
        raise HTTPException(status_code=401, detail="用户名或密码错误")
    # 每次登录轮换 token（多端登录时旧端会 401，重新登录即可）
    new_token = secrets.token_hex(32)
    with db() as conn:
        conn.execute("UPDATE users SET token = ? WHERE username = ?", (new_token, username))
    return JSONResponse({"token": new_token, "username": username})


@app.post("/api/sync/upload")
async def api_sync_upload(request: Request, username: str = Depends(check_account)) -> JSONResponse:
    try:
        body = await request.json()
    except Exception:
        raise HTTPException(status_code=400, detail="请求体不是合法 JSON")
    device = str(body.get("device", "")).strip()
    payload = body.get("payload")
    if not device or not isinstance(payload, dict):
        raise HTTPException(status_code=400, detail="缺少 device 或 payload")
    for key in PAYLOAD_KEYS:
        if not isinstance(payload.get(key), list):
            raise HTTPException(status_code=400, detail=f"payload 缺少列表字段：{key}")
    now = int(time.time() * 1000)
    payload_text = json.dumps(payload, ensure_ascii=False)
    with db() as conn:
        conn.execute(
            "INSERT INTO snapshots(username, payload, device, uploaded_at) VALUES(?,?,?,?) "
            "ON CONFLICT(username) DO UPDATE SET payload=excluded.payload, "
            "device=excluded.device, uploaded_at=excluded.uploaded_at",
            (username, payload_text, device, now),
        )
    return JSONResponse({"uploaded_at": now})


@app.get("/api/sync/download")
async def api_sync_download(username: str = Depends(check_account)) -> JSONResponse:
    with db() as conn:
        row = conn.execute(
            "SELECT payload, device, uploaded_at FROM snapshots WHERE username = ?", (username,)
        ).fetchone()
    if row is None:
        raise HTTPException(status_code=404, detail="云端暂无快照")
    return JSONResponse(
        {
            "device": row["device"],
            "uploaded_at": row["uploaded_at"],
            "payload": json.loads(row["payload"]),
        }
    )


@app.get("/api/sync/meta")
async def api_sync_meta(username: str = Depends(check_account)) -> JSONResponse:
    with db() as conn:
        row = conn.execute(
            # review F-17：length() 取字节数，不整包读出含全部笔记正文的 payload
            "SELECT length(payload) AS bytes, device, uploaded_at FROM snapshots WHERE username = ?",
            (username,),
        ).fetchone()
    if row is None:
        raise HTTPException(status_code=404, detail="云端暂无快照")
    return JSONResponse(
        {"device": row["device"], "uploaded_at": row["uploaded_at"], "bytes": len(row["payload"])}
    )
