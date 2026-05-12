from __future__ import annotations

from review_analysis_worker.runtime.app import build_worker
from review_analysis_worker.runtime.config import WorkerSettings


def main() -> None:
    worker = build_worker(WorkerSettings.from_env())
    while True:
        worker.run_once()


if __name__ == "__main__":
    main()
