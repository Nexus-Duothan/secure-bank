# Audit & Recovery Service (`audit-recovery-service`)

The **Audit & Recovery Service** is a memory-safe Rust service responsible for immutable real-time transaction journaling, continuous threat auditing, and automated system state recovery.

---

## 🎯 What to Develop

- **Immutable Transaction Journal**: Maintain append-only, tamper-evident transaction logs (`FR-30`, `NFR-D5`).
- **Fraud & Anomaly Detection**: Real-time anomaly detection rules (unusual frequency, location, amount spikes) (`FR-31`).
- **Automated State Replay & Recovery**: Rebuild compromised service states by replaying clean audit journal logs (`FR-32`, `NFR-D5`).
- **Memory Safety & High Performance**: Zero buffer-overflow vulnerabilities leveraging Rust's memory safety primitives (`NFR-S8`).

---

## 🛠️ Prerequisites

- Rust & Cargo 1.97+
- Apache Kafka (`localhost:9092`)

---

## 🚀 How to Setup & Run

### 1. Start Message Broker

```bash
docker compose up -d kafka
```

### 2. Verify Syntax & Build

```bash
cargo check
cargo build
```

### 3. Run Service

```bash
cargo run
```
