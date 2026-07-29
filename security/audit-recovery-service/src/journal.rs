use crate::models::{AuditEntry, CreateAuditEntryRequest, IntegrityReport};
use hex;
use serde_json;
use sha2::{Digest, Sha256};
use std::fs::{File, OpenOptions};
use std::io::{BufRead, BufReader, Write};
use std::path::{Path, PathBuf};
use std::sync::Arc;
use std::time::SystemTime;
use tokio::sync::RwLock;
use uuid::Uuid;

pub const GENESIS_HASH: &str = "0000000000000000000000000000000000000000000000000000000000000000";

#[derive(Clone)]
pub struct JournalStore {
    file_path: PathBuf,
    state: Arc<RwLock<Vec<AuditEntry>>>,
}

impl JournalStore {
    pub fn new<P: AsRef<Path>>(path: P) -> Self {
        let file_path = path.as_ref().to_path_buf();
        if let Some(parent) = file_path.parent() {
            let _ = std::fs::create_dir_all(parent);
        }

        let mut entries = Vec::new();
        if file_path.exists() {
            if let Ok(file) = File::open(&file_path) {
                let reader = BufReader::new(file);
                for line in reader.lines().flatten() {
                    if let Ok(entry) = serde_json::from_str::<AuditEntry>(&line) {
                        entries.push(entry);
                    }
                }
            }
        }

        Self {
            file_path,
            state: Arc::new(RwLock::new(entries)),
        }
    }

    pub fn compute_entry_hash(
        prev_hash: &str,
        id: &str,
        timestamp: u64,
        service_name: &str,
        event_type: &str,
        user_id: Option<&str>,
        payload: &serde_json::Value,
    ) -> String {
        let mut hasher = Sha256::new();
        hasher.update(prev_hash.as_bytes());
        hasher.update(id.as_bytes());
        hasher.update(timestamp.to_string().as_bytes());
        hasher.update(service_name.as_bytes());
        hasher.update(event_type.as_bytes());
        if let Some(uid) = user_id {
            hasher.update(uid.as_bytes());
        }
        hasher.update(payload.to_string().as_bytes());
        hex::encode(hasher.finalize())
    }

    pub async fn append_entry(&self, req: CreateAuditEntryRequest) -> AuditEntry {
        let mut entries = self.state.write().await;
        let prev_hash = match entries.last() {
            Some(last) => last.hash.clone(),
            None => GENESIS_HASH.to_string(),
        };

        let id = Uuid::new_v4().to_string();
        let timestamp = SystemTime::now()
            .duration_since(SystemTime::UNIX_EPOCH)
            .unwrap_or_default()
            .as_secs();

        let hash = Self::compute_entry_hash(
            &prev_hash,
            &id,
            timestamp,
            &req.service_name,
            &req.event_type,
            req.user_id.as_deref(),
            &req.payload,
        );

        let entry = AuditEntry {
            id,
            timestamp,
            service_name: req.service_name,
            event_type: req.event_type,
            user_id: req.user_id,
            payload: req.payload,
            prev_hash,
            hash,
        };

        // Append to in-memory state
        entries.push(entry.clone());

        // Append to disk storage
        if let Ok(mut file) = OpenOptions::new()
            .create(true)
            .append(true)
            .open(&self.file_path)
        {
            if let Ok(json_line) = serde_json::to_string(&entry) {
                let _ = writeln!(file, "{}", json_line);
            }
        }

        entry
    }

    pub async fn get_all_entries(&self) -> Vec<AuditEntry> {
        self.state.read().await.clone()
    }

    pub async fn filter_entries(
        &self,
        service_name: Option<&str>,
        user_id: Option<&str>,
        event_type: Option<&str>,
    ) -> Vec<AuditEntry> {
        let entries = self.state.read().await;
        entries
            .iter()
            .filter(|e| {
                if let Some(s) = service_name {
                    if e.service_name != s {
                        return false;
                    }
                }
                if let Some(u) = user_id {
                    if e.user_id.as_deref() != Some(u) {
                        return false;
                    }
                }
                if let Some(evt) = event_type {
                    if e.event_type != evt {
                        return false;
                    }
                }
                true
            })
            .cloned()
            .collect()
    }

    pub async fn verify_integrity(&self) -> IntegrityReport {
        let entries = self.state.read().await;
        let mut expected_prev_hash = GENESIS_HASH.to_string();

        for entry in entries.iter() {
            if entry.prev_hash != expected_prev_hash {
                return IntegrityReport {
                    valid: false,
                    total_entries: entries.len(),
                    corrupted_entry_id: Some(entry.id.clone()),
                    message: format!(
                        "Hash chain broken at entry {}: prev_hash mismatch",
                        entry.id
                    ),
                };
            }

            let calculated_hash = Self::compute_entry_hash(
                &entry.prev_hash,
                &entry.id,
                entry.timestamp,
                &entry.service_name,
                &entry.event_type,
                entry.user_id.as_deref(),
                &entry.payload,
            );

            if entry.hash != calculated_hash {
                return IntegrityReport {
                    valid: false,
                    total_entries: entries.len(),
                    corrupted_entry_id: Some(entry.id.clone()),
                    message: format!("Cryptographic payload hash mismatch at entry {}", entry.id),
                };
            }

            expected_prev_hash = entry.hash.clone();
        }

        IntegrityReport {
            valid: true,
            total_entries: entries.len(),
            corrupted_entry_id: None,
            message: "Cryptographic journal integrity verified cleanly.".to_string(),
        }
    }
}
