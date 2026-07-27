DUOTHAN 6.0: PHASE 01 (RECON)

**SecureBank**

Project Blueprint and Software Requirements Specification

Version 1.0

22 July 2026

**Prepared by Team Nexus**

University of Moratuwa

# **Revision History**

| Version | Date       | Author(s)   | Description                         |
| :------ | :--------- | :---------- | :---------------------------------- |
| 0.1     | 19-07-2026 | All members | Document created; sections assigned |
| 1.0     | 22-07-2026 | All members | Final version for submission        |

# **Document Contributors**

| Section | Title                       | Author   |
| :------ | :-------------------------- | :------- |
| 1       | Problem Identification      | Samadhi  |
| 2       | Proposed Solution           | Pasindu  |
| 3       | System Architecture         | Yumeth   |
| 4       | Wireframes Design           | Samadhi  |
| 5       | Functional Requirements     | Kaveesha |
| 6       | Non-Functional Requirements | Kaveesha |
| 7       | Technology Stack Selection  | Yumrth   |

# **Definitions and Abbreviations**

| Term | Definition                             |
| :--- | :------------------------------------- |
| ATM  | Automated Teller Machine               |
| SME  | Small and Medium-sized Enterprise      |
| SSL  | Secure Sockets Layer                   |
| TOTP | Time-based One-Time Password           |
| RBAC | Role-Based Access Control              |
| A2A  | Account-to-Account (internal) transfer |
| A2V  | Account-to-Vendor (external) payment   |
| RBAC | Role-Based Access Control              |
| ELK  | Elasticsearch, Logstash, Kibana        |
| RTO  | Recovery Time Objective                |
| RPO  | Recovery Point Objective               |
| API  | Application Programming Interface      |

# **Table of Contents**

[**Revision History 1**](#heading=)

[**Document Contributors 1**](#heading=)

[**Definitions and Abbreviations 2**](#heading=)

[**Table of Contents 3**](#heading=)

[**Executive Summary 5**](#heading=)

[**1\. Problem Identification 6**](#1.-problem-identification)

[1.1 Background and Impact Analysis 6](#1.1-background-and-impact-analysis)

[1.2 Problem Statement 6](#1.2-problem-statement)

[1.3 Affected Stakeholders 6](#1.3-affected-stakeholders)

[1.4 Significance for Economic Recovery 6](#1.4-significance-for-economic-recovery)

[**2\. Proposed Solution 8**](#2.-proposed-solution)

[2.1 Solution Overview 8](#2.1-solution-overview)

[2.2 System Operation 8](#2.2-system-operation)

[2.3 Security and Recovery by Design 8](#2.3-security-and-recovery-by-design)

[2.4 Value Delivered 9](#2.4-value-delivered)

[**3\. System Architecture 10**](#heading=)

[3.1 Architecture Overview 10](#heading=)

[3.2 Service Responsibilities 10](#heading=)

[3.3 Communication and Data Flow 11](#heading=)

[3.4 Failure Isolation and Resilience 12](#heading=)

[**4\. Wireframes Design 13**](#heading=)

[4.1 Key Screens 13](#heading=)

[4.2 Design File Reference 14](#heading=)

[**5\. Functional Requirements 15**](#heading=)

[5.1 Conventions 15](#heading=)

[5.2 Identity and Access Management 15](#heading=)

[5.3 Accounts and Balances 16](#heading=)

[5.4 Transfers and Payments 16](#heading=)

[5.5 Lending 17](#heading=)

[5.6 Notifications and Alerts 18](#heading=)

[5.7 Security, Audit and Recovery 18](#heading=)

[**6\. Non-Functional Requirements 20**](#heading=)

[6.1 Security 20](#heading=)

[6.2 Disaster Recovery and Resilience 20](#heading=)

[6.3 Reliability and Availability 21](#heading=)

[6.4 Performance and Scalability 22](#heading=)

[6.5 Observability and Auditability 22](#heading=)

[6.6 Usability and Accessibility 22](#heading=)

[6.7 Maintainability and Compliance 23](#heading=)

[**7\. Technology Stack Selection 24**](#heading=)

[7.1 Selection Criteria 24](#heading=)

[7.2 Selected Technologies 24](#heading=)

[7.3 Alignment with Requirements 25](#heading=)

# **Executive Summary**

The 2065 Super Malware Agent attack disabled the global digital banking infrastructure, causing a systemic failure of core banking, payments, and lending services. To resolve this crisis, SecureBank introduces a cloud-native digital banking platform designed to safely restore financial access and trust for individuals and small businesses. The solution replaces vulnerable monolithic structures by decomposing financial functions into independent, isolated services, while a dedicated Audit & Recovery Service maintains an immutable transaction journal for automated threat containment and state recovery. The architectural approach relies on a highly scalable microservices ecosystem secured by a unified API Gateway, zero-trust mutual TLS communication, and real-time ELK stack observability. By combining strict service isolation, continuous anomaly detection, and memory-safe technologies, this design ensures that any future breach is isolated to a single component, definitively preventing a recurrence of the total system collapse seen in 2065\.

# **1\. Problem Identification** {#1.-problem-identification}

## **1.1 Background and Impact Analysis** {#1.1-background-and-impact-analysis}

The 2065 Super Malware Agent attack disabled the digital infrastructure underpinning global banking almost instantaneously. Core banking systems such as the software that manages account balances, ledgers, and transaction processing went offline across affected institutions, halting deposits, withdrawals, and internal transfers. ATM networks lost connectivity to central banking servers, leaving cash machines inoperable even where the physical hardware remained intact. Digital payment platforms, including card networks, mobile wallets, and point-of-sale systems, stopped authorizing transactions, cutting off both online and in-person commerce. Lending systems responsible for loan origination, credit assessment, and repayment tracking also went dark, freezing access to credit for individuals and businesses alike.

Critically, this was an availability and trust failure rather than a data breach: customer account data remained intact due to secure backups. The systems needed to safely and verifiably restore access to that data, most notably the Master Key used to unlock the banking network, were compromised, meaning the challenge was not data recovery but secure, trustworthy reactivation of financial services.

## **1.2 Problem Statement** {#1.2-problem-statement}

There is no secure, resilient, and scalable digital banking platform capable of restoring core financial services, account access, payments, transfers, and lending in the aftermath of a systemic cyberattack, while ensuring that a single point of failure can never again disable the entire banking ecosystem.

## **1.3 Affected Stakeholders** {#1.3-affected-stakeholders}

- **Individual customers** \- Lost access to savings, salaries, and daily payments; those without cash reserves or alternative banking access were disproportionately affected, including elderly and remote populations.
- **Small and medium businesses (SMEs)** \- Lost the ability to process digital payments or accept card transactions, directly threatening daily cash flow and operational survival.
- **Merchants** – Unable to settle transactions or receive payouts through disabled payment gateways, halting revenue collection.
- **Bank staff and operations teams** \- Lost the operational tools needed to service customers, process manual overrides, or investigate account issues during the outage.
- **Regulators and government bodies** \- Unable to monitor financial system stability or disburse public funds (pensions, subsidies, welfare payments) through digital rails, amplifying downstream social impact.

## **1.4 Significance for Economic Recovery** {#1.4-significance-for-economic-recovery}

Digital banking is foundational infrastructure for a functioning modern economy as it enables commerce, credit issuance, wage payments, and financial inclusion at scale. Its prolonged absence forces a reversion to cash-only transactions, which are slower, harder to secure, and incapable of supporting the transaction volume a recovering economy needs. This reversion disproportionately harms those least able to absorb the disruption as SMEs lose working capital access, individuals without cash reserves are locked out of basic financial participation, and financial inequality widens.

Restoring secure, reliable digital banking is therefore not a convenience but a prerequisite for economic recovery: it re-enables commerce, restores credit flow to businesses and individuals, and rebuilds the public trust required for people to safely re-engage with digital financial systems. Without addressing the underlying security failure, not just restoring the old system, but rebuilding it to be resilient against a repeat attack, any recovery effort remains fragile and exposed to the same systemic risk that caused the original collapse.

# **2\. Proposed Solution** {#2.-proposed-solution}

## **2.1 Solution Overview** {#2.1-solution-overview}

To address the collapse of core banking, payments, and lending caused by the Super Malware Agent, we propose rebuilding the financial ecosystem as a cloud-native, microservices-based digital banking platform, replacing the monolithic architecture that allowed a single attack to bring down the entire system. Rather than one large application where a single point of failure can cascade into total collapse, the platform is decomposed into independent, isolated services that each own a specific banking function. If one service is compromised or fails, the rest of the system continues operating, and the affected service can be isolated, recovered, and redeployed without taking the whole bank offline.

The platform serves individuals and small businesses who depend on digital payments, transfers, and loans; the banking operator responsible for keeping services running; and regulators who require auditability into how the system behaves and recovers. Because customer data was already preserved through secure backups, the platform's purpose is not to recover lost records, but to restore safe, resilient access to that data.

## **2.2 System Operation** {#2.2-system-operation}

A single API Gateway acts as the unified entry point for all client traffic, handling SSL termination, request routing, rate limiting, and baseline authentication before any request reaches internal services, meaning attackers face one hardened perimeter rather than dozens of exposed endpoints. Behind the gateway, requests are routed to purpose-built services:

Identity & Access (Authentication, TOTP, User/RBAC services) verify who a user is and what they're allowed to do, enforcing multi-factor authentication on every session.

Core Banking services (Accounts, Transfer, Payments, Lending) handle the actual financial logic of ledger balances, internal transfers, external payments, and loans, each independently scalable and independently securable.

Resilience & Security is handled by an Audit & Recovery Service that immutably journals every transaction in real time, enabling threat detection and automated rollback/recovery if a breach is detected.

Infrastructure & Observability (Notification Service and an ELK stack of Logstash/Beats, Elasticsearch and Kibana) gives operators live visibility into system health, security anomalies and log data across every microservice.

Together, these layers allow a future attack to be detected and contained within minutes, rather than bringing the entire system down for months.

## **2.3 Security and Recovery by Design** {#2.3-security-and-recovery-by-design}

The platform's architecture ensures that a single compromise cannot disable the entire system, directly targeting the failure mode of the original attack where one compromised layer took down every dependent function at once. This is achieved through several reinforcing mechanisms:

Immutable journaling: every write to the ledger is journaled immutably before it is committed, so transaction history cannot be silently altered or destroyed.

Enforced MFA: multi-factor authentication at the identity layer prevents credential compromise from translating directly into account compromise.

Continuous anomaly detection: the Audit & Recovery Service continuously watches for anomalous patterns, enabling automated containment rather than manual, reactive incident response.

Service isolation: because each banking function is an independently deployable service, a breach in one (e.g., Lending) can be contained and remediated without disrupting others (e.g., Payments, Transfers).

This design shifts the system from reactive recovery to proactive containment, reducing both the blast radius and the response time of any future attack.

## **2.4 Value Delivered** {#2.4-value-delivered}

Individuals and small businesses: Digital payments, transfers, and loan services return to being available, fast and trustworthy, ending the forced reliance on cash.

The banking operator: A system that can isolate and recover from an attack on one service without a full outage, dramatically reducing downtime and service gaps.

Regulators evaluating recovery: Full auditability and observability are built into the architecture from day one, rather than bolted on after the fact, giving regulators continuous visibility rather than after-the-fact reporting.

# **3\. System Architecture**

## **3.1 Architecture Overview**

The platform's architecture is designed as a scalable ecosystem built from independent services, directly mitigating the vulnerabilities exposed by the 2065 cyber disaster. A unified API Gateway acts as the single entry point, routing all incoming client traffic from the Web Service while handling SSL termination and rate limiting. Behind this hardened perimeter, the system is divided into functional domains: Identity & Access, Core Banking, Resilience & Security, and Infrastructure. These domains communicate asynchronously where possible to prevent bottlenecks. The entire ecosystem is underpinned by the ELK stack (Elasticsearch, Logstash, Kibana), providing continuous real-time visibility into system health and security anomalies.

## **3.2 Service Responsibilities**

![][image1]

To ensure that a future malware attack cannot take down the entire system, the platform is decomposed into isolated services, each owning a single banking function.

| Domain                             | Service Name             | Responsibility                                                                                                             |
| :--------------------------------- | :----------------------- | :------------------------------------------------------------------------------------------------------------------------- |
| **Edge**                           | Web Service              | Frontend delivery and client-side application hosting.                                                                     |
|                                    | API Gateway              | Single entry point, SSL termination, request routing, rate limiting, and baseline authentication.                          |
| **Identity & Access**              | Authentication Service   | Session token management and sign-in methods.                                                                              |
|                                    | TOTP Service             | Time-based OTP generation and validation for multi-factor authentication.                                                  |
|                                    | User Service             | User details, profile management, and Role-Based Access Control (RBAC).                                                    |
| **Core Banking**                   | Accounts Service         | Account creation, state management, and ledger balance tracking.                                                           |
|                                    | Transfer Service         | Internal account-to-account (A2A) orchestration and validation.                                                            |
|                                    | Payments Service         | External account-to-vendor (A2V) processing and merchant settlement.                                                       |
|                                    | Lending Service          | Loan origination, payment tracking, and automated repayment triggers.                                                      |
| **Resilience & Security**          | Audit & Recovery Service | Immutable transaction journaling, real-time threat auditing, and automated system state recovery.                          |
| **Infrastructure & Observability** | Notification Service     | Multi-channel communication engine managing email, SMS, and push notifications.                                            |
|                                    | ELK Stack                | Logstash for ingestion, Elasticsearch for secure storage, and Kibana for centralized monitoring and anomaly visualization. |

##

## **3.3 Communication and Data Flow**

All external communication is secured via TLS 1.3 at the API Gateway. Once a request passes initial authentication and rate-limiting checks, the Gateway routes it to the appropriate internal service. Internal service-to-service communication is secured using mutual TLS (mTLS) to enforce a zero-trust architecture. For synchronous operations (e.g., verifying a user's balance before a transfer), services communicate via lightweight gRPC or REST APIs. For asynchronous operations (e.g., triggering a transaction alert via the Notification Service or writing logs to the Audit & Recovery Service), the system utilizes a durable message broker (such as Apache Kafka). This decoupled data flow ensures that if a peripheral service is slow or unavailable, core transactional flows remain uninterrupted.

## **3.4 Failure Isolation and Resilience**

The architecture prevents cascading failure by isolating each banking function into independently deployable services. If a single service, such as the Lending Service, is compromised or experiences a critical failure, the fault is contained; the Accounts, Transfer, and Payments services will continue operating normally. Furthermore, the Audit & Recovery Service maintains an immutable, append-only journal of all transactions. In the event of data corruption, this service allows administrators to instantly isolate the affected microservice, purge the corrupted state, and automatically rebuild it by replaying clean, verified audit logs.

# **4\. Wireframes Design**

## **4.1 Key Screens**

             *Login Page                             OTP Verification Page                           Dashboard Page*

##

##

##

##

##

##

##

##

     *Account Details Page                       Transfer Money Page                       Pay a Vendor Page*

##

##

    *Loan Application Page              Loan Repayment Tracker Page   Transaction & Audit History Page*

\\

       *Notifications Page                         Profile & Settings Page*

## **4.2 Design File Reference**

[https://www.figma.com/design/JjMWCmEFjKp6YfHwO5nHrw/Duothan?node-id=30-179\&t=VELLHuoe3zfz0k0p-1](https://www.figma.com/design/JjMWCmEFjKp6YfHwO5nHrw/Duothan?node-id=30-179&t=VELLHuoe3zfz0k0p-1)

# **5\. Functional Requirements**

This section defines what the system shall do from the user's perspective. Requirements are grouped by architectural service to ensure each function is traceable to a component of the system architecture in Section 3\.

## **5.1 Conventions**

Each requirement carries a unique identifier (FR-XX) and a priority assigned using the MoSCoW method: Must (essential for launch), Should (important but not launch-blocking), and Could (desirable enhancement). The word "shall" denotes a mandatory capability. Requirement groups map to the services defined in Section 3.2.

## **5.2 Identity and Access Management**

Services responsible: Authentication Service, TOTP Service, User Service.

| ID    | Requirement                 | Description                                                                                                                                                                                | Priority |
| :---- | :-------------------------- | :----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | :------- |
| FR-01 | User registration           | The system shall allow new and returning customers to register using their national ID or passport number, verified against the restored customer database backups.                        | Must     |
| FR-02 | Identity verification (KYC) | The system shall verify user identity through document upload and OTP confirmation before activating any account.                                                                          | Must     |
| FR-03 | Secure login                | The system shall allow users to sign in with email or username and password, with all credentials stored as salted hashes.                                                                 | Must     |
| FR-04 | Multi-factor authentication | The system shall require a time-based one-time password (TOTP) as a second factor for every login and for high-risk actions such as adding a payee, large transfers, and password changes. | Must     |
| FR-05 | Session management          | The system shall issue short-lived session tokens, log users out after five minutes of inactivity, and allow users to view and revoke active sessions on other devices.                    | Must     |
| FR-06 | Password recovery           | The system shall provide a secure, MFA-protected password reset flow via registered email or SMS.                                                                                          | Must     |
| FR-07 | Profile management          | Users shall be able to view and update contact details, notification preferences, and linked devices, with every change confirmed via OTP.                                                 | Should   |
| FR-08 | Role-based access control   | The system shall support distinct roles (customer, merchant, bank officer, and system administration), each with strictly limited permissions.                                             | Must     |

##

##

## **5.3 Accounts and Balances**

Service responsible: Accounts Service.

| ID    | Requirement         | Description                                                                                                                 | Priority |
| :---- | :------------------ | :-------------------------------------------------------------------------------------------------------------------------- | :------- |
| FR-09 | Account creation    | The system shall allow verified users to open savings and current accounts digitally, without visiting a physical branch.   | Must     |
| FR-10 | Account dashboard   | The system shall display real-time account balances, account status, and recent activity on a single dashboard.             | Must     |
| FR-11 | Transaction history | Users shall be able to view, search, and filter their full transaction history by date range, amount, and transaction type. | Must     |
| FR-12 | Statement download  | Users shall be able to download monthly account statements as PDF documents.                                                | Should   |
| FR-13 | Account freeze      | Users shall be able to instantly freeze or unfreeze their own account if they suspect compromise                            | Must     |

## **5.4 Transfers and Payments**

Services responsible: Transfer Service, Payments Service.

| ID    | Requirement                      | Description                                                                                                                                                              | Priority |
| :---- | :------------------------------- | :----------------------------------------------------------------------------------------------------------------------------------------------------------------------- | :------- |
| FR-14 | Internal fund transfers (A2A)    | The system shall allow users to transfer funds between accounts within the platform in real time, validating balance sufficiency before execution.                       | Must     |
| FR-15 | External payments (A2V)          | The system shall allow users to pay registered merchants and vendors, with settlement handled by the Payments Service.                                                   | Must     |
| FR-16 | Payee management                 | Users shall be able to add, edit, and remove saved payees; adding a new payee shall require OTP confirmation and impose a 12-hour cooling period before large transfers. | Must     |
| FR-17 | Transfer confirmation            | Before executing any transaction, the system shall display a confirmation summary (recipient, amount, fees) and require explicit user approval.                          | Must     |
| FR-18 | Transaction limits               | The system shall enforce configurable daily and per-transaction limits, and allow users to set lower personal limits as an additional safety control.                    | Must     |
| FR-19 | Scheduled and recurring payments | Users shall be able to schedule one-time future payments and recurring payments such as rent and utility bills.                                                          | Should   |
| FR-20 | QR-based merchant payments       | The system shall support QR code scanning for in-person merchant payments, enabling small businesses to accept digital payments.                                         | Should   |
| FR-21 | Transaction receipts             | The system shall generate a digital receipt with a unique reference number for every completed transaction.                                                              | Must     |

## **5.5 Lending**

Service responsible: Lending Service.

| ID    | Requirement          | Description                                                                                                                                                        | Priority |
| :---- | :------------------- | :----------------------------------------------------------------------------------------------------------------------------------------------------------------- | :------- |
| FR-22 | Loan application     | Verified users shall be able to apply for personal and small-business loans digitally by submitting required details and documents.                                | Must     |
| FR-23 | Loan status tracking | Applicants shall be able to track their application status (Submitted, Under Review, Approved or Rejected, Disbursed) in real time.                                | Must     |
| FR-24 | Repayment schedule   | The system shall display a repayment schedule showing installment amounts, due dates, and remaining balance.                                                       | Must     |
| FR-25 | Automated repayment  | The system shall automatically deduct loan installments from the linked account on the due date and retry according to a defined policy if funds are insufficient. | Should   |
| FR-26 | Repayment reminders  | The system shall notify borrowers before each due date via their preferred channel.                                                                                | Should   |

## **5.6 Notifications and Alerts**

Service responsible: Notification Service.

| ID    | Requirement            | Description                                                                                                                                                              | Priority |
| :---- | :--------------------- | :----------------------------------------------------------------------------------------------------------------------------------------------------------------------- | :------- |
| FR-27 | Transaction alerts     | The system shall send real-time notifications for every debit, credit, login, and profile change.                                                                        | Must     |
| FR-28 | Security alerts        | The system shall immediately alert users to suspicious activity, including failed login attempts, blocked transactions, and new payee additions.                         | Must     |
| FR-29 | Multi-channel delivery | Notifications shall be deliverable via email, SMS, and push notification according to user preference, with SMS as the guaranteed fallback for critical security alerts. | Must     |

## **5.7 Security, Audit and Recovery**

Services responsible: Audit & Recovery Service, API Gateway, Observability stack.

| ID    | Requirement                  | Description                                                                                                                                                                      | Priority |
| :---- | :--------------------------- | :------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | :------- |
| FR-30 | Immutable audit trail        | The system shall record every transaction and administrative action in an append-only, tamper-evident journal that cannot be modified or deleted.                                | Must     |
| FR-31 | Fraud detection and blocking | The system shall automatically flag and hold anomalous transactions, temporarily hold accounts (unusual amount, location, or frequency) for user confirmation or officer review. | Must     |
| FR-32 | Automated state recovery     | On detection of data corruption or service failure, the system shall rebuild the affected service's state by replaying clean, verified audit logs.                               | Must     |
| FR-33 | Rate limiting                | The API Gateway shall enforce rate limits on all endpoints to block brute-force and denial-of-service abuse.                                                                     | Must     |
| FR-34 | Administrative monitoring    | Authorised administrators shall be able to view system health, active threats, and anomaly visualisations through the centralised monitoring dashboard.                          | Must     |

# **6\. Non-Functional Requirements**

This section defines the quality standards of the platform. As the system is being rebuilt after the 2065 cyber disaster, security, disaster recovery, reliability, and cloud performance are treated as primary design constraints. Each requirement is expressed as a measurable target so that it can be verified through testing.

## **6.1 Security**

| ID     | Requirement              | Target / Standard                                                                                                                                                           |
| :----- | :----------------------- | :-------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| NFR-S1 | Encryption in transit    | All communication shall use TLS 1.3, terminated at the API Gateway; internal service-to-service traffic shall use mutual TLS (mTLS).                                        |
| NFR-S2 | Encryption at rest       | All databases, backups, and logs shall be encrypted at rest using AES-256, with keys managed in a dedicated key management service and rotated every 90 days.               |
| NFR-S3 | Zero-trust architecture  | No service shall trust another by default; every internal request shall be authenticated and authorised so that a compromised service cannot attack the rest of the system. |
| NFR-S4 | Credential protection    | Passwords shall be hashed with a modern adaptive algorithm (Argon2id or bcrypt); no credential or OTP secret shall be stored or logged in plain text.                       |
| NFR-S5 | Least privilege          | Every user role and service account shall hold only the minimum permissions required for its function.                                                                      |
| NFR-S6 | Input validation         | All inputs shall be validated and sanitised at the gateway and service level to prevent injection and related attacks, in line with the OWASP Top 10\.                      |
| NFR-S7 | Vulnerability management | The platform shall undergo automated dependency scanning on every build and scheduled penetration testing before each major release.                                        |
| NFR-S8 | Malware isolation        | Services shall run in isolated, immutable containers with no shared writable state, preventing lateral spread of malware between services.                                  |

## **6.2 Disaster Recovery and Resilience**

| ID     | Requirement                    | Target / Standard                                                                                                                                                        |
| :----- | :----------------------------- | :----------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| NFR-D1 | Recovery Time Objective (RTO)  | Core banking services (login, balances, transfers) shall be restored within 15 minutes of a total regional failure.                                                      |
| NFR-D2 | Recovery Point Objective (RPO) | Maximum acceptable data loss shall be near-zero for financial transactions (synchronous ledger replication) and no more than 5 minutes for non-critical data.            |
| NFR-D3 | Automated backups              | Encrypted backups shall be taken continuously for transaction data and daily for full system state, stored in a geographically separate region with immutable retention. |
| NFR-D4 | Multi-region failover          | The platform shall operate across at least two cloud regions with automated failover.                                                                                    |
| NFR-D5 | State replay recovery          | Any single service shall be rebuildable from the immutable audit journal without affecting other services.                                                               |
| NFR-D6 | Fault isolation                | Failure of any one service shall not cascade into total system failure; circuit breakers and graceful degradation shall keep unaffected services operational.            |
| NFR-D7 | Recovery testing               | Full disaster-recovery failover shall be tested at least quarterly, with results recorded and audited.                                                                   |

## **6.3 Reliability and Availability**

| ID     | Requirement              | Target / Standard                                                                                                                                     |
| :----- | :----------------------- | :---------------------------------------------------------------------------------------------------------------------------------------------------- |
| NFR-R1 | Availability             | Core banking services shall achieve 99.99% availability (approximately 52 minutes of downtime per year).                                              |
| NFR-R2 | Data integrity           | All financial transactions shall be ACID-compliant and idempotent; a retried transfer shall never execute twice, and the ledger shall always balance. |
| NFR-R3 | Zero-downtime deployment | Updates shall be released through rolling or blue-green deployments with automatic rollback on failure.                                               |
| NFR-R4 | Message durability       | Asynchronous events between services shall be delivered at least once via a durable message queue; no event shall be silently lost.                   |

## **6.4 Performance and Scalability**

| ID     | Requirement             | Target / Standard                                                                                                                   |
| :----- | :---------------------- | :---------------------------------------------------------------------------------------------------------------------------------- |
| NFR-P1 | Response time           | 95% of user-facing API requests shall complete in under 500 ms; transaction execution shall complete in under 2 seconds end to end. |
| NFR-P2 | Throughput              | The platform shall sustain at least 1,000 transactions per second at launch.                                                        |
| NFR-P3 | Horizontal auto-scaling | Each service shall scale out automatically based on load, independently of other services.                                          |
| NFR-P4 | Concurrent users        | The system shall support at least 100,000 concurrent active sessions without degradation.                                           |
| NFR-P5 | Load testing            | Performance targets shall be verified through load and stress testing at twice the expected peak before every major release.        |

## **6.5 Observability and Auditability**

| ID     | Requirement         | Target / Standard                                                                                                                                        |
| :----- | :------------------ | :------------------------------------------------------------------------------------------------------------------------------------------------------- |
| NFR-O1 | Centralised logging | Every service shall ship structured logs and metrics to the central observability stack in near real time (ingest delay under 30 seconds).               |
| NFR-O2 | Real-time alerting  | Anomalies such as error spikes, latency breaches, and suspicious traffic patterns shall trigger automated alerts to the operations team within 1 minute. |
| NFR-O3 | Distributed tracing | Every request shall carry a correlation identifier across services so any transaction can be traced end to end.                                          |
| NFR-O4 | Log retention       | Audit and security logs shall be retained in tamper-evident storage for a minimum of 7 years.                                                            |

## **6.6 Usability and Accessibility**

| ID     | Requirement            | Target / Standard                                                                                                                  |
| :----- | :--------------------- | :--------------------------------------------------------------------------------------------------------------------------------- |
| NFR-U1 | Ease of use            | A first-time user shall be able to complete registration and a first transfer in under 5 minutes without external help.            |
| NFR-U2 | Accessibility standard | The web interface shall conform to WCAG 2.1 Level AA.                                                                              |
| NFR-U3 | Responsive design      | The interface shall function correctly on mobile, tablet, and desktop screen sizes.                                                |
| NFR-U4 | Clarity of security    | Security steps such as MFA and confirmations shall be presented in plain language to rebuild user trust without creating friction. |

## **6.7 Maintainability and Compliance**

| ID     | Requirement               | Target / Standard                                                                                                                                         |
| :----- | :------------------------ | :-------------------------------------------------------------------------------------------------------------------------------------------------------- |
| NFR-M1 | Independent deployability | Each service shall be independently developed, tested, deployed, and versioned, with backward-compatible interfaces.                                      |
| NFR-M2 | Infrastructure as code    | All cloud infrastructure shall be defined as code so the entire environment can be rebuilt automatically after an attack.                                 |
| NFR-M3 | Test coverage             | Core banking services shall maintain at least 80% automated test coverage, with contract tests between services.                                          |
| NFR-M4 | Regulatory alignment      | The system shall align with recognised financial security standards, including PCI DSS for payment data and ISO 27001 practices for information security. |
| NFR-M5 | Data privacy              | Personal data shall be collected minimally, used only for stated purposes, and be exportable or erasable on user request.                                 |

Traceability: each functional requirement in Section 5 maps to a service defined in Section 3, and each non-functional requirement above addresses a specific failure mode exposed by the 2065 attack — lateral malware spread (NFR-S3, NFR-S8), total system outage (NFR-D4, NFR-D6), data corruption (NFR-D5, NFR-R2), and loss of public trust (NFR-U4, NFR-S1, NFR-S2).

# **7\. Technology Stack Selection**

## **7.1 Selection Criteria**

The technology stack was selected to strictly address the post-cyberattack environment, with a heavy emphasis on security, disaster recovery, cloud performance, and reliability. The primary criteria include memory safety to prevent exploit execution, enterprise-grade dependency management, high horizontal scalability, and rapid, accessible frontend delivery.

## **7.2 Selected Technologies**

| Layer                                   | Technology            | Justification                                                                                                                                                                                                                                                                                |
| :-------------------------------------- | :-------------------- | :------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Frontend UI**                         | React with Ant Design | React provides a highly responsive, component-based architecture. Ant Design offers a comprehensive, accessible UI library, allowing for the rapid translation of high-fidelity wireframes into a production-ready Web Service.                                                              |
| **Core Backend**                        | Java (Spring Boot)    | Spring Boot is an enterprise-grade framework with robust, battle-tested security modules (Spring Security). It is highly suited for the complex business logic required in the Core Banking domain (Accounts, Transfers, Lending) and integrates seamlessly with microservice architectures. |
| **Security & High-Performance Backend** | Rust                  | Rust is utilized for the Audit & Recovery Service and cryptographic operations. Its strict memory safety guarantees and zero-cost abstractions make it impervious to common vulnerabilities like buffer overflows, fulfilling the mandate for an attack-proof solution.                      |
| **Database (Relational)**               | PostgreSQL            | Used by Core Banking services to ensure strict ACID compliance for financial transactions, guaranteeing that ledgers always balance and data integrity is maintained.                                                                                                                        |
| **Message Broker**                      | Apache Kafka          | Provides high-throughput, durable event streaming for asynchronous inter-service communication, ensuring no transaction events are lost even during partial system outages.                                                                                                                  |
| **Observability**                       | ELK Stack             | Logstash, Elasticsearch, and Kibana provide real-time, centralized monitoring across all independent services, enabling the immediate detection of anomalies and fulfilling regulator requirements for continuous visibility.                                                                |

##

## **7.3 Alignment with Requirements**

This stack directly satisfies the architectural and non-functional requirements of the new banking platform. The combination of Java (Spring Boot) and Rust on the backend maps perfectly to the need for a scalable, independent-service architecture. Rust's memory safety prevents the lateral spread of malware at the system level, while Spring Boot handles the rigorous authentication and role-based access controls required at the application layer. The React and Ant Design frontend ensures the system meets strict usability targets, presenting complex security steps in a clear, accessible manner to rebuild public trust. Finally, the ELK stack and Kafka ensure the system shifts from reactive recovery to proactive containment, minimizing both the blast radius and response time of any future attack.

[image1]: data:image/png;base64,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
