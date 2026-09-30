import SwiftUI

// MARK: - Media Item Models

public enum MediaType: String, Codable {
    case photo
    case video
}

public struct MediaPickerItem: Identifiable, Hashable {
    public let id: String
    public let mediaType: MediaType
    public let durationFormatted: String?
    public let thumbnailSymbol: String
    public var isSelected: Bool
    public var selectionIndex: Int?

    public init(id: String, mediaType: MediaType, durationFormatted: String? = nil, thumbnailSymbol: String, isSelected: Bool = false, selectionIndex: Int? = nil) {
        self.id = id
        self.mediaType = mediaType
        self.durationFormatted = durationFormatted
        self.thumbnailSymbol = thumbnailSymbol
        self.isSelected = isSelected
        self.selectionIndex = selectionIndex
    }
}

// MARK: - MediaPickerView

public struct MediaPickerView: View {
    public var onSendMedia: (([MediaPickerItem]) -> Void)?

    @State private var selectedAlbum: String = "Recents"
    @State private var maxSelectionLimit: Int = 10
    @State private var captionText: String = ""
    @State private var previewItem: MediaPickerItem? = nil

    private let albums = ["Recents", "Favorites", "Videos", "Screenshots", "TJ Downloads"]

    @State private var mediaItems: [MediaPickerItem] = [
        MediaPickerItem(id: "p1", mediaType: .photo, thumbnailSymbol: "photo.fill"),
        MediaPickerItem(id: "v1", mediaType: .video, durationFormatted: "0:45", thumbnailSymbol: "film.fill"),
        MediaPickerItem(id: "p2", mediaType: .photo, thumbnailSymbol: "mountain.2.fill"),
        MediaPickerItem(id: "p3", mediaType: .photo, thumbnailSymbol: "sun.max.fill"),
        MediaPickerItem(id: "v2", mediaType: .video, durationFormatted: "1:20", thumbnailSymbol: "video.fill"),
        MediaPickerItem(id: "p4", mediaType: .photo, thumbnailSymbol: "tree.fill"),
        MediaPickerItem(id: "p5", mediaType: .photo, thumbnailSymbol: "camera.fill"),
        MediaPickerItem(id: "v3", mediaType: .video, durationFormatted: "3:15", thumbnailSymbol: "play.rectangle.fill"),
        MediaPickerItem(id: "p6", mediaType: .photo, thumbnailSymbol: "leaf.fill"),
        MediaPickerItem(id: "p7", mediaType: .photo, thumbnailSymbol: "snowflake"),
        MediaPickerItem(id: "v4", mediaType: .video, durationFormatted: "0:15", thumbnailSymbol: "video.circle.fill"),
        MediaPickerItem(id: "p8", mediaType: .photo, thumbnailSymbol: "flame.fill")
    ]

    public init(onSendMedia: (([MediaPickerItem]) -> Void)? = nil) {
        self.onSendMedia = onSendMedia
    }

    private var selectedItems: [MediaPickerItem] {
        mediaItems.filter(\.isSelected).sorted { ($0.selectionIndex ?? 0) < ($1.selectionIndex ?? 0) }
    }

    public var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                // Header / Album Picker Dropdown
                HStack {
                    Menu {
                        ForEach(albums, id: \.self) { album in
                            Button(action: { selectedAlbum = album }) {
                                HStack {
                                    Text(album)
                                    if selectedAlbum == album { Image(systemName: "checkmark") }
                                }
                            }
                        }
                    } label: {
                        HStack(spacing: 6) {
                            Text(selectedAlbum)
                                .font(.headline.bold())
                                .foregroundColor(.primary)
                            Image(systemName: "chevron.down")
                                .font(.caption.bold())
                                .foregroundColor(.secondary)
                        }
                    }

                    Spacer()

                    Text("\(selectedItems.count)/\(maxSelectionLimit) selected")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
                .padding(.horizontal)
                .padding(.vertical, 12)

                // Media Gallery Grid
                ScrollView {
                    LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 4), count: 3), spacing: 4) {
                        // Camera Tile
                        cameraTileView

                        // Items
                        ForEach(mediaItems) { item in
                            mediaTileView(item)
                        }
                    }
                    .padding(.horizontal, 4)
                }

                // Bottom Send Bar & Caption Input
                if !selectedItems.isEmpty {
                    bottomActionBar
                }
            }
            .navigationTitle("Select Media")
            .navigationBarTitleDisplayMode(.inline)
            .sheet(item: $previewItem) { item in
                mediaPreviewModal(item)
            }
        }
    }

    // MARK: - Subviews

    private var cameraTileView: some View {
        Button(action: {
            // Camera launch handler
        }) {
            VStack(spacing: 6) {
                Image(systemName: "camera.fill")
                    .font(.system(size: 28))
                    .foregroundColor(.white)
                Text("Camera")
                    .font(.caption.bold())
                    .foregroundColor(.white)
            }
            .frame(height: 120)
            .frame(maxWidth: .infinity)
            .background(LinearGradient(colors: [.blue, .purple], startPoint: .topLeading, endPoint: .bottomTrailing))
            .cornerRadius(8)
        }
    }

    private func mediaTileView(_ item: MediaPickerItem) -> some View {
        ZStack(alignment: .topTrailing) {
            ZStack(alignment: .bottomLeading) {
                // Background Thumbnail representation
                Rectangle()
                    .fill(Color(.secondarySystemBackground))
                    .frame(height: 120)
                    .overlay(
                        Image(systemName: item.thumbnailSymbol)
                            .font(.system(size: 32))
                            .foregroundColor(.blue.opacity(0.7))
                    )
                    .onTapGesture {
                        previewItem = item
                    }

                // Video Duration Badge
                if item.mediaType == .video, let duration = item.durationFormatted {
                    HStack(spacing: 4) {
                        Image(systemName: "play.fill")
                            .font(.system(size: 8))
                        Text(duration)
                            .font(.caption2.bold())
                    }
                    .foregroundColor(.white)
                    .padding(.horizontal, 6)
                    .padding(.vertical, 3)
                    .background(Color.black.opacity(0.6))
                    .cornerRadius(6)
                    .padding(6)
                }
            }

            // Selection Checkmark Circle
            Button(action: { toggleSelection(for: item) }) {
                ZStack {
                    Circle()
                        .stroke(Color.white, lineWidth: 2)
                        .background(item.isSelected ? Circle().fill(Color.blue) : Circle().fill(Color.black.opacity(0.3)))
                        .frame(width: 26, height: 26)

                    if item.isSelected, let idx = item.selectionIndex {
                        Text("\(idx)")
                            .font(.caption.bold())
                            .foregroundColor(.white)
                    }
                }
                .padding(6)
            }
        }
        .cornerRadius(8)
    }

    private var bottomActionBar: some View {
        VStack(spacing: 10) {
            HStack {
                Image(systemName: "text.bubble")
                    .foregroundColor(.secondary)
                TextField("Add a caption...", text: $captionText)
                    .textFieldStyle(.plain)
            }
            .padding(10)
            .background(Color(.secondarySystemBackground))
            .cornerRadius(12)

            HStack {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 6) {
                        ForEach(selectedItems) { item in
                            ZStack(alignment: .topTrailing) {
                                Image(systemName: item.thumbnailSymbol)
                                    .font(.system(size: 16))
                                    .frame(width: 44, height: 44)
                                    .background(Color.blue.opacity(0.15))
                                    .cornerRadius(8)

                                Button(action: { toggleSelection(for: item) }) {
                                    Image(systemName: "xmark.circle.fill")
                                        .font(.caption2)
                                        .foregroundColor(.red)
                                        .background(Color.white.clipShape(Circle()))
                                }
                                .offset(x: 4, y: -4)
                            }
                        }
                    }
                }

                Button(action: {
                    onSendMedia?(selectedItems)
                }) {
                    HStack(spacing: 6) {
                        Text("Send (\(selectedItems.count))")
                            .font(.subheadline.bold())
                        Image(systemName: "paperplane.fill")
                            .font(.caption)
                    }
                    .foregroundColor(.white)
                    .padding(.horizontal, 18)
                    .padding(.vertical, 12)
                    .background(Color.blue)
                    .cornerRadius(20)
                }
            }
        }
        .padding()
        .background(Color(.secondarySystemGroupedBackground))
    }

    private func mediaPreviewModal(_ item: MediaPickerItem) -> some View {
        NavigationStack {
            ZStack {
                Color.black.ignoresSafeArea()

                VStack {
                    Spacer()

                    Image(systemName: item.thumbnailSymbol)
                        .font(.system(size: 120))
                        .foregroundColor(.white)

                    if item.mediaType == .video {
                        Text("Video Preview • \(item.durationFormatted ?? "")")
                            .font(.headline)
                            .foregroundColor(.gray)
                    }

                    Spacer()

                    Button(action: {
                        toggleSelection(for: item)
                        previewItem = nil
                    }) {
                        Text(item.isSelected ? "Deselect Item" : "Select Item")
                            .font(.headline.bold())
                            .foregroundColor(.white)
                            .padding()
                            .frame(maxWidth: .infinity)
                            .background(item.isSelected ? Color.red : Color.blue)
                            .cornerRadius(12)
                            .padding(.horizontal)
                    }
                }
            }
            .navigationBarItems(trailing: Button("Done") { previewItem = nil }.foregroundColor(.white))
        }
    }

    private func toggleSelection(for item: MediaPickerItem) {
        if let idx = mediaItems.firstIndex(where: { $0.id == item.id }) {
            if mediaItems[idx].isSelected {
                mediaItems[idx].isSelected = false
                mediaItems[idx].selectionIndex = nil
                reorderSelectionIndices()
            } else {
                if selectedItems.count < maxSelectionLimit {
                    mediaItems[idx].isSelected = true
                    mediaItems[idx].selectionIndex = selectedItems.count + 1
                }
            }
        }
    }

    private func reorderSelectionIndices() {
        let currentSelected = mediaItems.filter(\.isSelected).sorted { ($0.selectionIndex ?? 0) < ($1.selectionIndex ?? 0) }
        for (i, sel) in currentSelected.enumerated() {
            if let idx = mediaItems.firstIndex(where: { $0.id == sel.id }) {
                mediaItems[idx].selectionIndex = i + 1
            }
        }
    }
}

#Preview {
    MediaPickerView()
}
