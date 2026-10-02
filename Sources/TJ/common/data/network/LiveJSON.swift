import Foundation

enum LiveJSON {
    static func object(_ data: Data) -> [String: Any] {
        (try? JSONSerialization.jsonObject(with: data) as? [String: Any]) ?? [:]
    }

    static func list(_ data: Data, keys: [String]) -> [[String: Any]] {
        let json = try? JSONSerialization.jsonObject(with: data)
        if let rows = json as? [Any] {
            return rows.compactMap { $0 as? [String: Any] }
        }
        guard let obj = json as? [String: Any] else { return [] }
        for key in keys {
            if let rows = obj[key] as? [Any] {
                return rows.compactMap { $0 as? [String: Any] }
            }
        }
        return []
    }

    static func string(_ value: Any?) -> String {
        if let text = value as? String { return text }
        if let number = value as? NSNumber { return number.stringValue }
        return ""
    }

    static func int64(_ value: Any?) -> Int64 {
        if let number = value as? NSNumber { return number.int64Value }
        if let text = value as? String { return Int64(text) ?? 0 }
        return 0
    }

    static func int(_ value: Any?) -> Int { Int(int64(value)) }

    static func bool(_ value: Any?) -> Bool {
        if let flag = value as? Bool { return flag }
        if let number = value as? NSNumber { return number.boolValue }
        if let text = value as? String { return text == "true" || text == "1" }
        return false
    }

    static func strings(_ value: Any?) -> [String] {
        if let list = value as? [String] { return list.filter { !$0.isEmpty } }
        if let list = value as? [Any] {
            return list.compactMap { item -> String? in
                if let text = item as? String, !text.isEmpty { return text }
                if let obj = item as? [String: Any] {
                    let url = string(obj["media_url"]).isEmpty ? string(obj["url"]) : string(obj["media_url"])
                    return url.isEmpty ? nil : url
                }
                return nil
            }
        }
        let single = string(value)
        return single.isEmpty ? [] : [single]
    }

    static func chats(from data: Data) -> [ChatMessage] {
        list(data, keys: ["chats", "conversations", "data"]).compactMap(chat)
    }

    static func chat(_ json: [String: Any]) -> ChatMessage? {
        let id = string(json["conversation_id"]).isEmpty ? string(json["id"]) : string(json["conversation_id"])
        guard !id.isEmpty else { return nil }
        let rawType = string(json["type"]).uppercased()
        let type: ConversationType
        if rawType.contains("MINGLE") || id.hasPrefix("mingle_") {
            type = .mingle
        } else if rawType.contains("GROUP") {
            type = .group
        } else if rawType.contains("CHANNEL") {
            type = .channel
        } else if rawType.contains("JOB") {
            type = .jobChat
        } else {
            type = .direct
        }
        let title = string(json["title"])
        let contactName = string(json["contact_name"])
        let displayName = string(json["display_name"])
        let username = string(json["username"])
        let contactId = string(json["contact_id"])
        let rawPhone = string(json["phone_number"]).isEmpty ? string(json["contact_phone"]) : string(json["phone_number"])
        let phone = type == .mingle ? "" : rawPhone
        let local = phone.isEmpty ? nil : ContactSyncManager.shared.getContactNameByNumber(phone)
        let ids = [id, contactId]
        let server = realPersonLabel(names: [displayName, contactName, username], ids: ids)
        let name = type == .group || type == .channel
            ? realPersonLabel(names: [title, contactName], ids: ids)
            : realPersonLabel(local: local, names: [displayName, contactName, username, title], phone: phone, ids: ids)
        return ChatMessage(
            id: id,
            type: type,
            senderName: name,
            localContactName: local,
            serverProfileName: server,
            lastMessage: string(json["last_message"]),
            timestamp: int64(json["time"] ?? json["timestamp"] ?? json["updated_at"]),
            unreadCount: int(json["unread_count"]),
            avatarUrl: string(json["avatar_url"]).isEmpty ? nil : string(json["avatar_url"]),
            isOnline: bool(json["is_online"]),
            lastSeen: int64(json["last_seen"]),
            isMuted: bool(json["is_muted"]),
            isPinned: bool(json["is_pinned"]),
            isGroup: type == .group,
            isAdmin: bool(json["is_admin"]),
            adminsCanPost: bool(json["admins_can_post"]),
            isSubscribed: bool(json["is_subscribed"]) || bool(json["is_following"]),
            createdBy: string(json["created_by"]).isEmpty ? nil : string(json["created_by"]),
            phoneNumber: phone.nilIfEmpty,
            otherUserId: string(json["contact_id"]).nilIfEmpty,
            isMingle: type == .mingle
        )
    }

    static func messages(from data: Data, myUserId: String?) -> [Message] {
        list(data, keys: ["messages", "items", "data"]).compactMap { message($0, myUserId: myUserId) }
    }

    static func message(_ json: [String: Any], myUserId: String?) -> Message? {
        let id = string(json["message_id"]).isEmpty ? string(json["id"]) : string(json["message_id"])
        guard !id.isEmpty else { return nil }
        let senderId = string(json["sender_id"])
        var urls = strings(json["media_urls"])
        let single = string(json["media_url"])
        if !single.isEmpty && !urls.contains(single) { urls.insert(single, at: 0) }
        let rawType = string(json["media_type"]).uppercased()
        let type: MessageType
        switch rawType {
        case "IMAGE": type = .image
        case "VIDEO": type = .video
        case "AUDIO", "AUDIO_MESSAGE": type = .audioMessage
        case "LOCATION": type = .location
        case "CONTACT": type = .contact
        case "DOCUMENT", "FILE": type = .document
        default: type = urls.isEmpty ? .text : .image
        }
        let body = string(json["content"]).isEmpty ? string(json["message_text"]) : string(json["content"])
        let client = string(json["client_msg_id"]).isEmpty ? string(json["client_uuid"]) : string(json["client_msg_id"])
        return Message(
            id: id,
            conversationId: string(json["conversation_id"]),
            content: body,
            senderId: senderId,
            senderName: string(json["sender_name"]),
            timestamp: int64(json["created_at"] ?? json["timestamp"]),
            isFromMe: myUserId != nil && senderId == myUserId,
            type: type,
            mediaUrls: urls,
            fileName: string(json["file_name"]).nilIfEmpty,
            fileSize: string(json["file_size"]).nilIfEmpty,
            duration: string(json["duration"]).nilIfEmpty,
            isRead: bool(json["is_read"]),
            isDelivered: bool(json["is_delivered"]),
            clientId: client.nilIfEmpty
        )
    }

    static func isOpaqueId(_ text: String) -> Bool {
        let trimmed = text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard trimmed.count >= 16 else { return false }
        let allowed = CharacterSet(charactersIn: "0123456789abcdefABCDEF-")
        return trimmed.unicodeScalars.allSatisfy { allowed.contains($0) }
    }

    static func posts(from data: Data) -> [FeedPost] {
        let json = try? JSONSerialization.jsonObject(with: data)
        if let rows = json as? [Any] {
            return rows.compactMap { ($0 as? [String: Any]).flatMap(post) }
        }
        guard let obj = json as? [String: Any] else { return [] }
        if let items = obj["items"] as? [Any] {
            return items.compactMap { item -> FeedPost? in
                guard let row = item as? [String: Any] else { return nil }
                if let nested = row["post"] as? [String: Any] { return post(nested) }
                return post(row)
            }
        }
        for key in ["posts", "shorts", "data"] {
            if let rows = obj[key] as? [Any] {
                return rows.compactMap { ($0 as? [String: Any]).flatMap(post) }
            }
        }
        return []
    }

    static func post(_ json: [String: Any]) -> FeedPost? {
        post(json, nested: false)
    }

    private static func post(_ json: [String: Any], nested: Bool) -> FeedPost? {
        let id = string(json["id"])
        guard !id.isEmpty else { return nil }
        let author = json["author"] as? [String: Any]
        var urls = strings(json["media_urls"])
        urls.append(contentsOf: strings(json["media"]))
        let direct = string(json["media_url"]).isEmpty ? string(json["content_url"]) : string(json["media_url"])
        if !direct.isEmpty && !urls.contains(direct) { urls.insert(direct, at: 0) }
        let thumb = string(json["thumbnail_url"])
        if !thumb.isEmpty && !urls.contains(thumb) { urls.append(thumb) }
        let name = string(json["author_name"]).isEmpty ? string(author?["display_name"]) : string(json["author_name"])
        let handle = string(json["author_handle"]).isEmpty ? string(author?["username"]) : string(json["author_handle"])
        let avatar = string(json["author_avatar_url"]).isEmpty ? string(author?["avatar_url"]) : string(json["author_avatar_url"])
        return FeedPost(
            id: id,
            authorId: string(json["author_id"]).isEmpty ? string(author?["id"]) : string(json["author_id"]),
            authorName: name,
            authorHandle: handle,
            authorAvatar: avatar.nilIfEmpty,
            content: string(json["content"]),
            mediaURLs: urls,
            likes: int(json["likes"]),
            repliesCount: int(json["replies_count"]) == 0 ? int(json["reply_count"]) : int(json["replies_count"]),
            postType: string(json["post_type"]),
            isLiked: bool(json["is_liked"]),
            isBookmarked: bool(json["is_bookmarked"]),
            views: int(json["views"]) == 0 ? int(json["view_count"]) : int(json["views"]),
            shares: int(json["shares"]) == 0 ? int(json["actual_shares"]) : int(json["shares"]),
            repostCount: int(json["repost_count"]),
            quoteCount: int(json["quote_count"]),
            isReposted: bool(json["is_reposted"]),
            mediaType: string(json["media_type"]),
            repostOfId: string(json["repost_of_id"]),
            originalAuthorId: nested ? "" : authorId(json["original_post"] as? [String: Any]),
            originalAuthorName: nested ? "" : authorName(json["original_post"] as? [String: Any]),
            originalAuthorHandle: nested ? "" : authorHandle(json["original_post"] as? [String: Any]),
            originalAuthorAvatar: nested ? nil : authorAvatar(json["original_post"] as? [String: Any]),
            originalContent: nested ? "" : string((json["original_post"] as? [String: Any])?["content"]),
            originalMediaURLs: nested ? [] : mediaURLs(json["original_post"] as? [String: Any]),
            originalMediaType: nested ? "" : string((json["original_post"] as? [String: Any])?["media_type"]),
            originalPostType: nested ? "" : string((json["original_post"] as? [String: Any])?["post_type"])
        )
    }

    private static func authorId(_ json: [String: Any]?) -> String {
        let author = json?["author"] as? [String: Any]
        let direct = string(json?["author_id"])
        return direct.isEmpty ? string(author?["id"]) : direct
    }

    private static func authorName(_ json: [String: Any]?) -> String {
        let author = json?["author"] as? [String: Any]
        let direct = string(json?["author_name"])
        return direct.isEmpty ? string(author?["display_name"]) : direct
    }

    private static func authorHandle(_ json: [String: Any]?) -> String {
        let author = json?["author"] as? [String: Any]
        let direct = string(json?["author_handle"])
        return direct.isEmpty ? string(author?["username"]) : direct
    }

    private static func authorAvatar(_ json: [String: Any]?) -> String? {
        let author = json?["author"] as? [String: Any]
        let direct = string(json?["author_avatar_url"])
        let value = direct.isEmpty ? string(author?["avatar_url"]) : direct
        return value.nilIfEmpty
    }

    private static func mediaURLs(_ json: [String: Any]?) -> [String] {
        guard let json else { return [] }
        var urls = strings(json["media_urls"])
        urls.append(contentsOf: strings(json["media"]))
        let direct = string(json["media_url"]).isEmpty ? string(json["content_url"]) : string(json["media_url"])
        if !direct.isEmpty && !urls.contains(direct) { urls.insert(direct, at: 0) }
        return urls
    }

    static func adverts(from data: Data) -> [Advert] {
        list(data, keys: ["adverts", "ads", "data"]).compactMap(advert)
    }

    static func advert(_ json: [String: Any]) -> Advert? {
        let id = string(json["id"])
        guard !id.isEmpty else { return nil }
        return Advert(
            id: id,
            advertiserName: string(json["advertiser_name"]).isEmpty ? string(json["title"]) : string(json["advertiser_name"]),
            advertiserId: string(json["user_id"]),
            title: string(json["title"]),
            avatarUrl: string(json["avatar_url"]).nilIfEmpty,
            contentUrl: string(json["content_url"]).nilIfEmpty,
            contentText: string(json["content_text"]),
            actionText: string(json["action_text"]).isEmpty ? "Learn More" : string(json["action_text"]),
            targetUrl: string(json["target_url"]),
            isSponsored: json["is_sponsored"] == nil ? true : bool(json["is_sponsored"]),
            impressions: string(json["impressions"]).isEmpty ? "0" : string(json["impressions"]),
            clicks: string(json["clicks"]).isEmpty ? "0" : string(json["clicks"]),
            isNativeAd: bool(json["is_native_ad"]),
            isViewed: bool(json["is_viewed"])
        )
    }

    static func enrichMissingProfiles(_ chats: [ChatMessage]) async -> [ChatMessage] {
        var updated = chats
        for index in updated.indices {
            let chat = updated[index]
            guard chat.type == .direct || chat.type == .jobChat || chat.type == .mingle else { continue }
            guard let userId = chat.otherUserId, !userId.isEmpty else { continue }
            let ids = [userId, chat.id]
            let profileName = realPersonLabel(names: [chat.serverProfileName, chat.senderName], ids: ids)
            if chat.localContactName != nil { continue }
            if !profileName.isEmpty && !looksLikePhone(profileName) { continue }
            guard let profile = await userProfile(userId) else { continue }
            let phone = profile.phone.isEmpty ? (chat.phoneNumber ?? "") : profile.phone
            let local = phone.isEmpty ? nil : ContactSyncManager.shared.getContactNameByNumber(phone)
            let resolved = realPersonLabel(
                local: local,
                names: [profile.displayName, profile.username, chat.serverProfileName],
                phone: phone,
                ids: ids
            )
            guard !resolved.isEmpty else { continue }
            updated[index].localContactName = local
            let server = realPersonLabel(names: [profile.displayName, profile.username], ids: ids)
            updated[index].serverProfileName = server.isEmpty ? chat.serverProfileName : server
            updated[index].senderName = resolved
            updated[index].phoneNumber = phone.isEmpty ? chat.phoneNumber : phone
            if (updated[index].avatarUrl ?? "").isEmpty, !profile.avatar.isEmpty {
                updated[index].avatarUrl = profile.avatar
            }
            let savedName = resolved
            let savedPhone = updated[index].phoneNumber
            await MainActor.run {
                GlobalCallState.shared.updateContactInfo(id: userId, name: savedName, phone: savedPhone)
            }
        }
        return updated
    }

    static func userProfile(_ id: String) async -> (displayName: String, username: String, phone: String, avatar: String)? {
        guard let data = try? await API.data("users/\(id)") else { return nil }
        let root = object(data)
        let user = (root["user"] as? [String: Any]) ?? root
        let displayName = string(user["display_name"])
        let username = string(user["username"])
        let phone = string(user["phone_number"])
        let avatar = string(user["avatar_url"])
        if displayName.isEmpty && username.isEmpty && phone.isEmpty { return nil }
        return (displayName, username, phone, avatar)
    }
}

extension String {
    var nilIfEmpty: String? { isEmpty ? nil : self }
}
