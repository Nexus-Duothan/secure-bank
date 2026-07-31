use crate::journal::JournalStore;
use crate::models::AnomalyReport;
use std::collections::HashMap;
use std::time::SystemTime;
use uuid::Uuid;

pub struct AnomalyEngine;

impl AnomalyEngine {
    pub async fn analyze(journal: &JournalStore) -> Vec<AnomalyReport> {
        Self::analyze_windowed(journal, 3600).await
    }

    pub async fn analyze_windowed(
        journal: &JournalStore,
        window_seconds: u64,
    ) -> Vec<AnomalyReport> {
        let current_time = SystemTime::now()
            .duration_since(SystemTime::UNIX_EPOCH)
            .unwrap_or_default()
            .as_secs();

        let cutoff = current_time.saturating_sub(window_seconds);

        let entries = journal.get_all_entries().await;
        let mut user_event_counts: HashMap<String, usize> = HashMap::new();
        let mut user_failed_logins: HashMap<String, usize> = HashMap::new();

        for entry in entries {
            if entry.timestamp >= cutoff {
                if let Some(user_id) = entry.user_id {
                    *user_event_counts.entry(user_id.clone()).or_insert(0) += 1;

                    if entry.event_type == "FAILED_LOGIN" || entry.event_type == "INVALID_OTP" {
                        *user_failed_logins.entry(user_id).or_insert(0) += 1;
                    }
                }
            }
        }

        let mut reports = Vec::new();

        for (user_id, count) in user_event_counts {
            let failed_count = user_failed_logins.get(&user_id).cloned().unwrap_or(0);

            if failed_count >= 3 {
                reports.push(AnomalyReport {
                    id: Uuid::new_v4().to_string(),
                    user_id: user_id.clone(),
                    event_count: count,
                    risk_score: 95,
                    reason: format!(
                        "Brute-force authentication burst detected: {} failed attempts in last {}s.",
                        failed_count, window_seconds
                    ),
                    action_taken: "TEMPORARY_ACCOUNT_HOLD".to_string(),
                    status: "ACTIVE".to_string(),
                    detection_timestamp: current_time,
                });
            } else if count >= 10 {
                reports.push(AnomalyReport {
                    id: Uuid::new_v4().to_string(),
                    user_id: user_id.clone(),
                    event_count: count,
                    risk_score: 75,
                    reason: format!(
                        "High-velocity transaction frequency detected: {} operations in last {}s.",
                        count, window_seconds
                    ),
                    action_taken: "HIGH_RISK_FLAGGED".to_string(),
                    status: "ACTIVE".to_string(),
                    detection_timestamp: current_time,
                });
            }
        }

        reports
    }
}
