"""Start the local API consumed by the ZENTIDE Spring Boot web service."""

import os
import sys
from pathlib import Path

import uvicorn

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))


if __name__ == "__main__":
    uvicorn.run(
        "api:app",
        host=os.getenv("ZENTIDE_AGENT_HOST", "127.0.0.1"),
        port=int(os.getenv("ZENTIDE_AGENT_PORT", "8090")),
        reload=False,
    )
