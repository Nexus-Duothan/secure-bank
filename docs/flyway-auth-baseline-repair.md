# Auth Flyway Baseline Note

`auth-service` keeps `V3__ensure_auth_schema_after_baseline.sql` on purpose as an idempotent compatibility migration for local databases that were baselined before the full auth schema existed.

## Why we do not rewrite `V3`

- It has already been applied in local databases.
- Editing an applied Flyway versioned migration would create checksum drift.
- A checksum drift would force manual repair on every database that already recorded version `3`.

## Safe local-development approach

1. Leave `V1`, `V2`, and `V3` untouched once they have been applied anywhere.
2. Put every new auth schema change in a fresh migration such as `V4__...sql`.
3. Use Flyway history queries to verify what your local database has recorded before changing anything.

## Useful verification queries

```sql
select installed_rank, version, description, success
from flyway_schema_history_auth
order by installed_rank;
```

```sql
select table_name
from information_schema.tables
where table_schema = 'public'
  and table_name in ('users', 'kyc_applications', 'user_sessions', 'password_reset_tokens')
order by table_name;
```

## If a local checksum mismatch ever happens

- Do not edit old migration files again to "match" the database.
- First confirm the current history table contents.
- Then use Flyway repair only as an explicit local maintenance step after you verify the file contents you intend to keep.

That keeps shared migration history stable while still giving local developers a recovery path.
