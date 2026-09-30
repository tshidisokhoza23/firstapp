import SwiftUI

public struct PostJobView: View {
    public var onBack: () -> Void
    @StateObject private var viewModel: JobViewModel

    @State private var title: String = ""
    @State private var category: String = ""
    @State private var descriptionText: String = ""
    @State private var durationDays: String = "1"
    @State private var skills: String = ""

    @MainActor public init(onBack: @escaping () -> Void, viewModel: JobViewModel = JobViewModel()) {
        self.onBack = onBack
        self._viewModel = StateObject(wrappedValue: viewModel)
    }

    public var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    Text("Job Details")
                        .font(.title3.bold())
                        .foregroundColor(.blue)

                    VStack(alignment: .leading, spacing: 6) {
                        Text("Job Title").font(.caption).foregroundColor(.secondary)
                        TextField("Job Title", text: $title)
                            .textFieldStyle(.roundedBorder)
                    }

                    VStack(alignment: .leading, spacing: 6) {
                        Text("Category (e.g., Software, General Work)").font(.caption).foregroundColor(.secondary)
                        TextField("Category", text: $category)
                            .textFieldStyle(.roundedBorder)
                    }

                    VStack(alignment: .leading, spacing: 6) {
                        Text("Description").font(.caption).foregroundColor(.secondary)
                        TextEditor(text: $descriptionText)
                            .frame(minHeight: 100)
                            .overlay(RoundedRectangle(cornerRadius: 8).stroke(Color.secondary.opacity(0.3), lineWidth: 1))
                    }

                    VStack(alignment: .leading, spacing: 6) {
                        Text("Duration (Days)").font(.caption).foregroundColor(.secondary)
                        TextField("1", text: $durationDays)
                            .keyboardType(.numberPad)
                            .textFieldStyle(.roundedBorder)
                    }

                    VStack(alignment: .leading, spacing: 6) {
                        Text("Required Skills (comma separated)").font(.caption).foregroundColor(.secondary)
                        TextField("Kotlin, Swift, UI Design", text: $skills)
                            .textFieldStyle(.roundedBorder)
                    }

                    Spacer().frame(height: 16)

                    let days = Int(durationDays) ?? 1
                    Button(action: {
                        let skillsList = skills.components(separatedBy: ",").map { $0.trimmingCharacters(in: .whitespaces) }.filter { !$0.isEmpty }
                        viewModel.createJob(title: title, description: descriptionText, category: category, skills: skillsList, durationDays: days)
                    }) {
                        Text("Post Job (R\(days * 20).00)")
                            .font(.headline)
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(Color.blue)
                            .foregroundColor(.white)
                            .cornerRadius(12)
                    }

                    Text("Posting cost is R20 per day. Your wallet will be charged upon posting.")
                        .font(.caption)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                        .frame(maxWidth: .infinity)
                }
                .padding(16)
            }
            .navigationTitle("Post a Job")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(action: onBack) {
                        Image(systemName: "chevron.left")
                    }
                }
            }
            .onChange(of: viewModel.createJobResult) { result in
                if case .success = result {
                    onBack()
                }
            }
        }
    }
}
