# FirstApp Business Requirements & User Stories

## Document Overview
This document serves as the "Source of Truth" for the functional and non-functional requirements of FirstApp. It is designed to guide development, ensuring consistency between the Go backend and Android frontend.

---

## 1. Chat & Communication (Core Module)

### 1.1 Overview
The communication module provides a unified interface for Direct Messaging (DM), Group Chats, and Channels, mimicking the performance and reliability of high-end messaging apps like WhatsApp.

### 1.2 User Stories

| ID | User Story | Priority |
|----|------------|----------|
| US.1.1 | As a user, I want to sync my phone contacts to see who else is using FirstApp. | High |
| US.1.2 | As a user, I want to send real-time text and media messages to my contacts. | High |
| US.1.3 | As a user, I want to create and manage group chats with multiple participants. | High |
| US.1.4 | As a user, I want to follow and receive updates from Channels (1-to-many). | Medium |
| US.1.5 | As a user, I want to see if a message has been delivered and read. | High |
| US.1.6 | As a user, I want to see when my contacts are online or typing. | Medium |
| US.1.7 | As a user, I want a high-speed search to find chats, contacts, and specific message content. | High |
| US.1.8 | As a user, I want to reply to specific messages and react to them with emojis. | Medium |
| US.1.9 | As a user, I want to "Star" important messages for easy retrieval later. | Low |
| US.1.10 | As a user, I want my messages to be encrypted for privacy. | High |

### 1.3 Functional Requirements

#### 1.3.1 Messaging Capabilities
- **Message Types**: Support for Text, Image, Video, Audio (Voice Notes), Documents, Location, and Contact Cards.
- **Message States**: Pending (Local), Sent (Server), Delivered (Recipient Device), Read (Recipient Viewed).
- **Editing/Deleting**: Users can edit messages or delete them (Delete for Me vs. Delete for Everyone).
- **Media Management**: Automatic local caching of shared media; gallery view for shared media within a chat.

#### 1.3.2 Chat Organization
- **Pinning**: Support for pinning up to 3 important chats to the top of the list.
- **Archiving**: Ability to hide chats from the main list into an Archive section.
- **Muting**: Disable notifications for specific chats/groups.
- **Wallpapers**: Custom background selection per chat.

#### 1.3.3 Groups & Channels
- **Groups**: Member limits (up to 1024), Admin roles, Invite links/codes.
- **Channels**: Verified badges, Follower counts, Broadcast-only mode (admins post, followers view).

#### 1.3.4 Search Strategy
- **Hybrid Search**: 
    - **Local**: Use SQLite FTS4 for instant text matching across all local messages.
    - **Remote**: Server-side phone number normalization (matching last 9 digits) for finding unregistered contacts.
- **Categorization**: Results must be clearly separated into Chats, Contacts, and Messages.

### 1.4 Technical Constraints & Logic

- **Persistence**: "Offline-first" architecture using Room. All outgoing messages are saved locally with a `PENDING` status before being synced.
- **Real-time**: WebSocket (WS) connection for all live events (typing, presence, read receipts).
- **Security**: 
    - `FLAG_SECURE` implementation to prevent unauthorized screen capture in sensitive chats.
    - End-to-End Encryption (E2EE) using public/private key pairs stored in Android Keystore.
- **Performance**: Pagination for message history (e.g., 20 messages per page) to ensure smooth scrolling in long conversations.

---

## 2. Global Search System

### 2.1 Search Types
- **Global**: Default search across all modules (Chats, Jobs, Channels, Music).
- **Contextual**: Tab-specific search (e.g., when in the Jobs tab, search only queries the job board).

### 2.2 Media Filters
- Support for filtering message results by type: **Photos, Videos, Links, GIFs, Audio, Documents**.

---

## 3. Rewards & Wallet

### 3.1 Monetization & Engagement
- **Daily Bonus**: Small XP/Wallet balance award for daily logins.
- **Ad Rewards**: Users earn points for watching incentivized ads, which can be redeemed for features or premium status.
