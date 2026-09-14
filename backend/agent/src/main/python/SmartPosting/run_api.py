"""Start the SmartPosting service used by Spring Boot."""

import os
import sys
from pathlib import Path
import uvicorn

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))


if __name__ == "__main__":
    uvicorn.run(
        "api:app",
        host=os.getenv("ZENTIDE_POST_REVIEW_HOST", "127.0.0.1"),
        port=int(os.getenv("ZENTIDE_POST_REVIEW_PORT", "8091")),
        reload=False,
    )
