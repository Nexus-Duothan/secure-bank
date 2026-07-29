use crate::journal::JournalStore;
use crate::models::AnomalyReport;
use std::collections::HashMap;

pub struct AnomalyEngine;

impl AnomalyEngine {
    pub async fn analyze(journal: &JournalStore) -> Vec<AnomalyReport> {
        let entries = journal.get_all_entries().await;
        let mut user_event_counts: HashMap<String, usize> = HashMap::new();
        let mut user_failed_logins: HashMap<String, usize> = HashMap::new();

        for entry in entries {
            if let Some(user_id) = entry.user_id {
                *user_event_counts.entry(user_id.clone()).or_insert(0) += 1;

                if entry.event_type == "FAILED_LOGIN" || entry.event_type == "INVALID_OTP" {
                    *user_failed_logins.entry(user_id).or_insert(0) += 1;
                }
            }
        }

        let mut reports = Vec::new();

        for (user_id, count) in user_event_counts {
            let failed_count = user_failed_logins.get(&user_id).cloned().unwrap_or(0);

            if failed_count >= 3 {
                reports.push(AnomalyReport {
                    user_id: user_id.clone(),
                    event_count: count,
                    risk_score: 95,
                    reason: format!(
                        "Brute-force authentication burst detected: {} failed attempts.",
                        failed_count
                    ),
                    action_taken: "TEMPORARY_ACCOUNT_HOLD".to_string(),
                });
            } else if count >= 10 {
                reports.push(AnomalyReport {
                    user_id: user_id.clone(),
                    event_count: count,
                    risk_score: 75,
                    reason: format!(
                        "High-velocity transaction frequency detected: {} operations.",
                        count
                    ),
                    action_taken: "HIGH_RISK_FLAGGED".to_string(),
                });
            }
        }

        reports
    }
}
