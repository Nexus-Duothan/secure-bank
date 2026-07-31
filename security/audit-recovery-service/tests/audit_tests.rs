use audit_recovery_service::anomaly::AnomalyEngine;
use audit_recovery_service::journal::{JournalStore, GENESIS_HASH};
use audit_recovery_service::models::CreateAuditEntryRequest;
use tempfile::NamedTempFile;

#[tokio::test]
async fn test_audit_entry_creation_and_hash_chaining() {
    let tmp_file = NamedTempFile::new().unwrap();
    let journal = JournalStore::new(tmp_file.path());

    // 1. Create 1st entry
    let entry1 = journal
        .append_entry(CreateAuditEntryRequest {
            service_name: "auth-service".to_string(),
            event_type: "USER_REGISTERED".to_string(),
            user_id: Some("user-1001".to_string()),
            payload: serde_json::json!({ "email": "user1@securebank.com" }),
        })
        .await
        .unwrap();

    assert_eq!(entry1.prev_hash, GENESIS_HASH);
    assert!(!entry1.hash.is_empty());

    // 2. Create 2nd entry
    let entry2 = journal
        .append_entry(CreateAuditEntryRequest {
            service_name: "accounts-service".to_string(),
            event_type: "ACCOUNT_CREATED".to_string(),
            user_id: Some("user-1001".to_string()),
            payload: serde_json::json!({ "account_type": "CHECKING" }),
        })
        .await
        .unwrap();

    assert_eq!(entry2.prev_hash, entry1.hash);

    // 3. Verify integrity
    let report = journal.verify_integrity().await;
    assert!(report.valid);
    assert_eq!(report.total_entries, 2);
}

#[tokio::test]
async fn test_service_state_replay() {
    let tmp_file = NamedTempFile::new().unwrap();
    let journal = JournalStore::new(tmp_file.path());

    journal
        .append_entry(CreateAuditEntryRequest {
            service_name: "auth-service".to_string(),
            event_type: "LOGIN_SUCCESS".to_string(),
            user_id: Some("user-1".to_string()),
            payload: serde_json::json!({}),
        })
        .await
        .unwrap();

    journal
        .append_entry(CreateAuditEntryRequest {
            service_name: "transfer-service".to_string(),
            event_type: "TRANSFER_INITIATED".to_string(),
            user_id: Some("user-1".to_string()),
            payload: serde_json::json!({ "amount": 500 }),
        })
        .await
        .unwrap();

    let auth_events = journal
        .filter_entries(Some("auth-service"), None, None)
        .await;
    assert_eq!(auth_events.len(), 1);
    assert_eq!(auth_events[0].service_name, "auth-service");

    let transfer_events = journal
        .filter_entries(Some("transfer-service"), None, None)
        .await;
    assert_eq!(transfer_events.len(), 1);
    assert_eq!(transfer_events[0].service_name, "transfer-service");
}

#[tokio::test]
async fn test_anomaly_detection_failed_logins() {
    let tmp_file = NamedTempFile::new().unwrap();
    let journal = JournalStore::new(tmp_file.path());

    for _ in 0..3 {
        journal
            .append_entry(CreateAuditEntryRequest {
                service_name: "auth-service".to_string(),
                event_type: "FAILED_LOGIN".to_string(),
                user_id: Some("attacker-999".to_string()),
                payload: serde_json::json!({ "reason": "invalid_password" }),
            })
            .await
            .unwrap();
    }

    let anomalies = AnomalyEngine::analyze(&journal).await;
    assert_eq!(anomalies.len(), 1);
    assert_eq!(anomalies[0].user_id, "attacker-999");
    assert_eq!(anomalies[0].risk_score, 95);
    assert_eq!(anomalies[0].action_taken, "TEMPORARY_ACCOUNT_HOLD");
}

#[tokio::test]
async fn test_hash_delimiters_prevent_collision() {
    let payload = serde_json::json!({});
    let hash1 = JournalStore::compute_entry_hash(
        GENESIS_HASH,
        "id-1",
        1000,
        "AB",
        "C",
        Some("user-1"),
        &payload,
    );
    let hash2 = JournalStore::compute_entry_hash(
        GENESIS_HASH,
        "id-1",
        1000,
        "A",
        "BC",
        Some("user-1"),
        &payload,
    );

    assert_ne!(hash1, hash2);
}
