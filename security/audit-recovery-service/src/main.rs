mod anomaly;
mod api;
mod journal;
mod models;

use api::{create_router, AppState};
use journal::JournalStore;
use std::net::SocketAddr;
use std::path::PathBuf;

#[tokio::main]
async fn main() {
    tracing_subscriber::fmt::init();
    tracing::info!("Initializing SecureBank Audit & Recovery Service (Rust)...");

    let journal_path = PathBuf::from("data/audit_journal.jsonl");
    let journal = JournalStore::new(journal_path);

    let state = AppState { journal };
    let app = create_router(state);

    let port: u16 = std::env::var("PORT")
        .unwrap_or_else(|_| "8089".to_string())
        .parse()
        .unwrap_or(8089);

    let addr = SocketAddr::from(([0, 0, 0, 0], port));
    tracing::info!("Audit & Recovery Service listening on http://{}", addr);

    let listener = tokio::net::TcpListener::bind(addr).await.unwrap();
    axum::serve(listener, app).await.unwrap();
}
