FROM rust:1.85-slim AS builder
WORKDIR /app
COPY security/audit-recovery-service .
RUN cargo build --release

FROM debian:bookworm-slim
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends ca-certificates && rm -rf /var/lib/apt/lists/*
COPY --from=builder /app/target/release/audit-recovery-service /app/audit-recovery-service
EXPOSE 8089
CMD ["/app/audit-recovery-service"]
