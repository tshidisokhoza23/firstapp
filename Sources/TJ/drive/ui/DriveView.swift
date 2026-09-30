import SwiftUI

// MARK: - Drive Models

public struct DriveFileItem: Identifiable, Hashable {
    public let id: String
    public let name: String
    public let sizeFormatted: String
    public let updatedDate: String
    public let isFolder: Bool
    public let fileExtension: String
    public var isStarred: Bool
    public var iconName: String {
        if isFolder { return "folder.fill" }
        switch fileExtension.lowercased() {
        case "pdf": return "doc.fill"
        case "jpg", "png", "jpeg": return "photo.fill"
        case "mp4", "mov": return "film.fill"
        case "mp3", "wav": return "music.note"
        case "zip", "tar": return "archivebox.fill"
        default: return "doc.text.fill"
        }
    }

    public init(id: String, name: String, sizeFormatted: String, updatedDate: String, isFolder: Bool = false, fileExtension: String = "", isStarred: Bool = false) {
        self.id = id
        self.name = name
        self.sizeFormatted = sizeFormatted
        self.updatedDate = updatedDate
        self.isFolder = isFolder
        self.fileExtension = fileExtension
        self.isStarred = isStarred
    }
}

// MARK: - DriveView

public struct DriveView: View {
    @State private var searchText: String = ""
    @State private var selectedFilter: DriveFilter = .all
    @State private var isGridView: Bool = false
    @State private var showUploadSheet: Bool = false
    @State private var showNewFolderAlert: Bool = false
    @State private var newFolderName: String = ""
    @State private var selectedFileForDetails: DriveFileItem? = nil
    @State private var renameTarget: DriveFileItem?
    @State private var renameText = ""

    enum DriveFilter: String, CaseIterable, Identifiable {
        case all = "All Files"
        case folders = "Folders"
        case documents = "Documents"
        case media = "Media"
        case starred = "Starred"

        var id: String { rawValue }
    }

    @State private var files: [DriveFileItem] = [
        DriveFileItem(id: "f1", name: "Project Specifications", sizeFormatted: "--", updatedDate: "Today, 10:15 AM", isFolder: true, isStarred: true),
        DriveFileItem(id: "f2", name: "TJ App Designs 2026", sizeFormatted: "--", updatedDate: "Yesterday", isFolder: true),
        DriveFileItem(id: "f3", name: "Architecture_Overview.pdf", sizeFormatted: "4.2 MB", updatedDate: "Sep 28, 2026", isFolder: false, fileExtension: "pdf", isStarred: true),
        DriveFileItem(id: "f4", name: "Demo_Presentation.mp4", sizeFormatted: "128.5 MB", updatedDate: "Sep 25, 2026", isFolder: false, fileExtension: "mp4"),
        DriveFileItem(id: "f5", name: "ProfileBanner.png", sizeFormatted: "2.1 MB", updatedDate: "Sep 20, 2026", isFolder: false, fileExtension: "png"),
        DriveFileItem(id: "f6", name: "VoiceNote_Record.mp3", sizeFormatted: "8.4 MB", updatedDate: "Sep 18, 2026", isFolder: false, fileExtension: "mp3"),
        DriveFileItem(id: "f7", name: "Backup_Database.zip", sizeFormatted: "340.0 MB", updatedDate: "Sep 10, 2026", isFolder: false, fileExtension: "zip")
    ]

    public init() {}

    private var filteredFiles: [DriveFileItem] {
        files.filter { file in
            let matchesSearch = searchText.isEmpty || file.name.localizedCaseInsensitiveContains(searchText)
            switch selectedFilter {
            case .all: return matchesSearch
            case .folders: return matchesSearch && file.isFolder
            case .documents: return matchesSearch && (!file.isFolder && (file.fileExtension == "pdf" || file.fileExtension == "doc"))
            case .media: return matchesSearch && (!file.isFolder && ["jpg", "png", "mp4", "mp3"].contains(file.fileExtension))
            case .starred: return matchesSearch && file.isStarred
            }
        }
    }

    public var body: some View {
        NavigationStack {
            ZStack(alignment: .bottomTrailing) {
                VStack(spacing: 0) {
                    // Search Bar
                    HStack {
                        Image(systemName: "magnifyingglass")
                            .foregroundColor(.secondary)
                        TextField("Search cloud drive...", text: $searchText)
                            .textFieldStyle(.plain)
                        if !searchText.isEmpty {
                            Button(action: { searchText = "" }) {
                                Image(systemName: "xmark.circle.fill")
                                    .foregroundColor(.secondary)
                            }
                        }
                    }
                    .padding(10)
                    .background(Color(.secondarySystemBackground))
                    .cornerRadius(12)
                    .padding(.horizontal)
                    .padding(.top, 8)

                    // Storage Summary Card
                    storageUsageCard
                        .padding(.horizontal)
                        .padding(.top, 12)

                    // Category Filter Pills & Layout Toggle
                    HStack {
                        ScrollView(.horizontal, showsIndicators: false) {
                            HStack(spacing: 8) {
                                ForEach(DriveFilter.allCases) { filter in
                                    Button(action: { selectedFilter = filter }) {
                                        Text(filter.rawValue)
                                            .font(.footnote)
                                            .fontWeight(selectedFilter == filter ? .bold : .medium)
                                            .padding(.horizontal, 14)
                                            .padding(.vertical, 6)
                                            .background(selectedFilter == filter ? Color.blue : Color(.tertiarySystemBackground))
                                            .foregroundColor(selectedFilter == filter ? .white : .primary)
                                            .cornerRadius(16)
                                    }
                                }
                            }
                        }

                        Button(action: { isGridView.toggle() }) {
                            Image(systemName: isGridView ? "list.bullet" : "squareshape.split.2x2")
                                .font(.system(size: 18))
                                .padding(8)
                                .background(Color(.secondarySystemBackground))
                                .cornerRadius(8)
                        }
                    }
                    .padding(.horizontal)
                    .padding(.top, 12)
                    .padding(.bottom, 8)

                    // File List or Grid
                    if filteredFiles.isEmpty {
                        Spacer()
                        VStack(spacing: 12) {
                            Image(systemName: "folder.badge.questionmark")
                                .font(.system(size: 48))
                                .foregroundColor(.secondary)
                            Text("No files found")
                                .font(.headline)
                                .foregroundColor(.secondary)
                        }
                        Spacer()
                    } else if isGridView {
                        ScrollView {
                            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                                ForEach(filteredFiles) { file in
                                    fileGridCard(file)
                                }
                            }
                            .padding(.horizontal)
                            .padding(.bottom, 80)
                        }
                    } else {
                        List {
                            ForEach(filteredFiles) { file in
                                fileListRow(file)
                            }
                        }
                        .listStyle(.plain)
                    }
                }

                // Floating Add Button
                Menu {
                    Button(action: { showUploadSheet = true }) {
                        Label("Upload File", systemImage: "doc.badge.plus")
                    }
                    Button(action: { showNewFolderAlert = true }) {
                        Label("New Folder", systemImage: "folder.badge.plus")
                    }
                } label: {
                    Image(systemName: "plus")
                        .font(.title2.bold())
                        .foregroundColor(.white)
                        .frame(width: 56, height: 56)
                        .background(Color.blue)
                        .clipShape(Circle())
                        .shadow(radius: 6)
                }
                .padding(.trailing, 20)
                .padding(.bottom, 20)
            }
            .navigationTitle("Cloud Drive")
            .sheet(isPresented: $showUploadSheet) {
                VStack(spacing: 20) {
                    Text("Upload File to Drive")
                        .font(.title2.bold())
                        .padding(.top)

                    Image(systemName: "cloud.pttr.fill")
                        .font(.system(size: 60))
                        .foregroundColor(.blue)

                    Text("Choose a file from device storage, photos, or documents to sync seamlessly.")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal)

                    Button("Select File from Device") {
                        showUploadSheet = false
                    }
                    .fontWeight(.bold)
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(Color.blue)
                    .cornerRadius(12)
                    .padding(.horizontal)

                    Spacer()
                }
                .presentationDetents([.height(300)])
            }
            .alert("Create New Folder", isPresented: $showNewFolderAlert) {
                TextField("Folder Name", text: $newFolderName)
                Button("Cancel", role: .cancel) { newFolderName = "" }
                Button("Create") {
                    if !newFolderName.isEmpty {
                        files.insert(DriveFileItem(id: UUID().uuidString, name: newFolderName, sizeFormatted: "--", updatedDate: "Just now", isFolder: true), at: 0)
                        newFolderName = ""
                    }
                }
            }
            .alert("Rename", isPresented: Binding(get: { renameTarget != nil }, set: { if !$0 { renameTarget = nil } })) {
                TextField("Name", text: $renameText)
                Button("Save") {
                    if let target = renameTarget, let index = files.firstIndex(where: { $0.id == target.id }) {
                        let old = files[index]
                        files[index] = DriveFileItem(id: old.id, name: renameText, sizeFormatted: old.sizeFormatted, updatedDate: old.updatedDate, isFolder: old.isFolder, fileExtension: old.fileExtension, isStarred: old.isStarred)
                    }
                    renameTarget = nil
                }
                Button("Cancel", role: .cancel) { renameTarget = nil }
            }
        }
    }

    // MARK: - Storage Usage Component

    private var storageUsageCard: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Label("Cloud Storage", systemImage: "icloud.fill")
                    .font(.subheadline.bold())
                    .foregroundColor(.blue)
                Spacer()
                Text("48.5 GB / 100 GB Used")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }

            GeometryReader { geo in
                HStack(spacing: 2) {
                    Rectangle().fill(Color.blue).frame(width: geo.size.width * 0.28)
                    Rectangle().fill(Color.purple).frame(width: geo.size.width * 0.12)
                    Rectangle().fill(Color.orange).frame(width: geo.size.width * 0.08)
                    Rectangle().fill(Color(.tertiarySystemFill))
                }
                .cornerRadius(4)
            }
            .frame(height: 8)

            HStack(spacing: 12) {
                storageLegend(color: .blue, text: "Docs")
                storageLegend(color: .purple, text: "Media")
                storageLegend(color: .orange, text: "Others")
                Spacer()
                Text("Upgrade")
                    .font(.caption.bold())
                    .foregroundColor(.blue)
            }
        }
        .padding(12)
        .background(Color(.secondarySystemGroupedBackground))
        .cornerRadius(14)
    }

    private func storageLegend(color: Color, text: String) -> some View {
        HStack(spacing: 4) {
            Circle().fill(color).frame(width: 6, height: 6)
            Text(text).font(.caption2).foregroundColor(.secondary)
        }
    }

    // MARK: - Row / Grid Cards

    private func fileListRow(_ file: DriveFileItem) -> some View {
        HStack(spacing: 12) {
            Image(systemName: file.iconName)
                .font(.title2)
                .foregroundColor(file.isFolder ? .orange : .blue)
                .frame(width: 40, height: 40)
                .background(file.isFolder ? Color.orange.opacity(0.12) : Color.blue.opacity(0.12))
                .cornerRadius(10)

            VStack(alignment: .leading, spacing: 2) {
                Text(file.name)
                    .font(.subheadline.weight(.semibold))
                    .lineLimit(1)

                Text("\(file.updatedDate) \(file.isFolder ? "" : "• " + file.sizeFormatted)")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }

            Spacer()

            Button(action: {
                if let index = files.firstIndex(where: { $0.id == file.id }) {
                    files[index].isStarred.toggle()
                }
            }) {
                Image(systemName: file.isStarred ? "star.fill" : "star")
                    .foregroundColor(file.isStarred ? .yellow : .gray.opacity(0.5))
            }

            Menu {
                Button(action: { shareFile(file) }) { Label("Download", systemImage: "square.and.arrow.down") }
                Button(action: { shareFile(file) }) { Label("Share Link", systemImage: "square.and.arrow.up") }
                Button(action: { renameFile(file) }) { Label("Rename", systemImage: "pencil") }
                Button(role: .destructive, action: {
                    files.removeAll { $0.id == file.id }
                }) { Label("Delete", systemImage: "trash") }
            } label: {
                Image(systemName: "ellipsis")
                    .foregroundColor(.gray)
                    .padding(.leading, 8)
            }
        }
        .padding(.vertical, 4)
    }

    private func shareFile(_ file: DriveFileItem) {
        let av = UIActivityViewController(activityItems: [file.name], applicationActivities: nil)
        if let scene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
           let root = scene.windows.first?.rootViewController {
            root.present(av, animated: true)
        }
    }

    private func renameFile(_ file: DriveFileItem) {
        renameText = file.name
        renameTarget = file
    }

    private func fileGridCard(_ file: DriveFileItem) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Image(systemName: file.iconName)
                    .font(.title2)
                    .foregroundColor(file.isFolder ? .orange : .blue)
                Spacer()
                Button(action: {
                    if let index = files.firstIndex(where: { $0.id == file.id }) {
                        files[index].isStarred.toggle()
                    }
                }) {
                    Image(systemName: file.isStarred ? "star.fill" : "star")
                        .foregroundColor(file.isStarred ? .yellow : .gray.opacity(0.4))
                }
            }

            Text(file.name)
                .font(.subheadline.bold())
                .lineLimit(2)

            Text(file.isFolder ? file.updatedDate : file.sizeFormatted)
                .font(.caption)
                .foregroundColor(.secondary)
        }
        .padding()
        .background(Color(.secondarySystemGroupedBackground))
        .cornerRadius(14)
    }
}

#Preview {
    DriveView()
}
