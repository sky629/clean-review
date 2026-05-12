from __future__ import annotations


class PostgresRuntimeDependencyError(RuntimeError):
    pass


def connect_postgres(dsn: str):
    try:
        import psycopg
    except ImportError as exc:
        raise PostgresRuntimeDependencyError(
            "psycopg is required to persist review analysis results."
        ) from exc

    connection = psycopg.connect(dsn)
    connection.autocommit = True
    return connection
