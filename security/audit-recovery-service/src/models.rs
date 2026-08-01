use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
pub struct AuditEntry {
    pub id: String,
    pub timestamp: u64,
    pub service_name: String,
    pub event_type: String,
    pub user_id: Option<String>,
    pub payload: serde_json::Value,
    pub prev_hash: String,
    pub hash: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct CreateAuditEntryRequest {
    pub service_name: String,
    pub event_type: String,
    pub user_id: Option<String>,
    pub payload: serde_json::Value,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct IntegrityReport {
    pub valid: bool,
    pub total_entries: usize,
    pub corrupted_entry_id: Option<String>,
    pub message: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct AnomalyReport {
    pub id: String,
    pub user_id: String,
    pub event_count: usize,
    pub risk_score: u32,
    pub reason: String,
    pub action_taken: String,
    pub status: String,
    pub detection_timestamp: u64,
}
