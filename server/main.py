# DrillBit 题库分发与笔记备份服务（FastAPI 单文件）
#
# 接口（全部需 Authorization: Bearer <DRILLBIT_TOKEN>）：
#   GET  /api/index          题库目录
#   GET  /api/banks/{bank_id} 题库全量 JSON
#   POST /api/notes           笔记全量备份（覆盖存储最新快照）
#
# 题库文件放本目录 banks/ 下，结构与 App 端 test_banks.json 中的单个题库一致：
#   {"id": "...", "name": "...", "version": N, "updatedAt": "MM-dd", "questions": [...]}
# 更新题库时：改 JSON 内容并把 version 加一即可，App 端同步时会对比版本。
#
# 部署（公网 + https 建议由 nginx/caddy 终结 TLS）：
#   pip install fastapi uvicorn
#   DRILLBIT_TOKEN=你的token uvicorn main:app --host 0.0.0.0 --port 8000

import json
import os
from pathlib import Path

from fastapi import Depends, FastAPI, Header, HTTPException, Request
from fastapi.responses import JSONResponse

app = FastAPI(title="DrillBit Server")
BANKS_DIR = Path(__file__).parent / "banks"
NOTES_FILE = Path(__file__).parent / "notes_backup.json"
TOKEN = os.environ.get("DRILLBIT_TOKEN", "")


async def check_token(authorization: str = Header(default="")) -> None:
    """简单 Bearer token 鉴权：token 通过环境变量 DRILLBIT_TOKEN 配置"""
    if not TOKEN:
        raise HTTPException(status_code=500, detail="服务器未配置 DRILLBIT_TOKEN")
    if authorization != f"Bearer {TOKEN}":
        raise HTTPException(status_code=401, detail="Token 不正确")


def load_bank(bank_id: str) -> dict:
    path = BANKS_DIR / f"{bank_id}.json"
    if not path.exists():
        raise HTTPException(status_code=404, detail=f"题库不存在：{bank_id}")
    return json.loads(path.read_text(encoding="utf-8"))


@app.get("/api/index")
async def api_index(_: None = Depends(check_token)) -> JSONResponse:
    items = []
    for path in sorted(BANKS_DIR.glob("*.json")):
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
            items.append(
                {
                    "id": data["id"],
                    "name": data["name"],
                    "version": data["version"],
                    "updatedAt": data.get("updatedAt", ""),
                    "questionCount": len(data.get("questions", [])),
                }
            )
        except (json.JSONDecodeError, KeyError):
            # 单个题库文件损坏不影响目录整体返回
            continue
    return JSONResponse(items)


@app.get("/api/banks/{bank_id}")
async def api_bank(bank_id: str, _: None = Depends(check_token)) -> JSONResponse:
    return JSONResponse(load_bank(bank_id))


@app.post("/api/notes")
async def api_notes(request: Request, _: None = Depends(check_token)) -> JSONResponse:
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
        json.dumps({"uploadedAt": __import__("time").strftime("%Y-%m-%d %H:%M:%S"), "notes": body}, ensure_ascii=False),
        encoding="utf-8",
    )
    tmp.replace(NOTES_FILE)
    return JSONResponse({"detail": f"已备份 {len(body)} 条笔记"})
