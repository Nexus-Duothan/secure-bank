use crate::anomaly::AnomalyEngine;
use crate::journal::JournalStore;
use crate::models::{AnomalyReport, AuditEntry, CreateAuditEntryRequest, IntegrityReport};
use axum::{
    extract::{Path, Query, Request, State},
    http::{HeaderMap, StatusCode},
    middleware::{self, Next},
    response::Response,
    routing::{get, post},
    Json, Router,
};
use serde::Deserialize;

pub const DEFAULT_API_KEY: &str = "securebank_audit_internal_secret_key_2026";

#[derive(Clone)]
pub struct AppState {
    pub journal: JournalStore,
}

#[derive(Deserialize)]
pub struct FilterQuery {
    pub service_name: Option<String>,
    pub user_id: Option<String>,
    pub event_type: Option<String>,
    pub limit: Option<usize>,
    pub offset: Option<usize>,
}

async fn auth_middleware(
    headers: HeaderMap,
    request: Request,
    next: Next,
) -> Result<Response, (StatusCode, Json<serde_json::Value>)> {
    let expected_key =
        std::env::var("AUDIT_SERVICE_API_KEY").unwrap_or_else(|_| DEFAULT_API_KEY.to_string());

    let provided_key = headers
        .get("X-Internal-Service-Key")
        .and_then(|v| v.to_str().ok())
        .or_else(|| {
            headers
                .get("Authorization")
                .and_then(|v| v.to_str().ok())
                .and_then(|v| v.strip_prefix("Bearer "))
        });

    match provided_key {
        Some(key) if key == expected_key => Ok(next.run(request).await),
        _ => Err((
            StatusCode::UNAUTHORIZED,
            Json(serde_json::json!({
                "error": "Unauthorized access to internal audit service API",
                "message": "Missing or invalid X-Internal-Service-Key header or Bearer token"
            })),
        )),
    }
}

pub fn create_router(state: AppState) -> Router {
    Router::new()
        .route("/api/v1/audit/entries", post(create_entry).get(get_entries))
        .route("/api/v1/audit/verify", get(verify_journal))
        .route(
            "/api/v1/audit/replay/:service_name",
            post(replay_service_state),
        )
        .route("/api/v1/audit/anomalies", get(get_anomalies))
        .route_layer(middleware::from_fn(auth_middleware))
        .with_state(state)
}

async fn create_entry(
    State(state): State<AppState>,
    Json(payload): Json<CreateAuditEntryRequest>,
) -> Result<(StatusCode, Json<AuditEntry>), (StatusCode, String)> {
    match state.journal.append_entry(payload).await {
        Ok(entry) => Ok((StatusCode::CREATED, Json(entry))),
        Err(e) => Err((
            StatusCode::INTERNAL_SERVER_ERROR,
            format!("Failed to persist audit entry to storage: {}", e),
        )),
    }
}

async fn get_entries(
    State(state): State<AppState>,
    Query(query): Query<FilterQuery>,
) -> Json<Vec<AuditEntry>> {
    let entries = state
        .journal
        .filter_entries(
            query.service_name.as_deref(),
            query.user_id.as_deref(),
            query.event_type.as_deref(),
            query.limit,
            query.offset,
        )
        .await;
    Json(entries)
}

async fn verify_journal(State(state): State<AppState>) -> Json<IntegrityReport> {
    let report = state.journal.verify_integrity().await;
    Json(report)
}

async fn replay_service_state(
    State(state): State<AppState>,
    Path(service_name): Path<String>,
) -> Result<Json<Vec<AuditEntry>>, (StatusCode, Json<serde_json::Value>)> {
    let report = state.journal.verify_integrity().await;
    if !report.valid {
        return Err((
            StatusCode::CONFLICT,
            Json(serde_json::json!({
                "error": "Cannot replay state from tampered or corrupted audit journal",
                "integrity_report": report
            })),
        ));
    }

    let replayed = state
        .journal
        .filter_entries(Some(&service_name), None, None, None, None)
        .await;
    Ok(Json(replayed))
}

async fn get_anomalies(State(state): State<AppState>) -> Json<Vec<AnomalyReport>> {
    let anomalies = AnomalyEngine::analyze(&state.journal).await;
    Json(anomalies)
}
