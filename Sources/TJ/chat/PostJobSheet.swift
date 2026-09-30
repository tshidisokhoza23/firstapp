import SwiftUI

struct PostJobSheet: View {
    @Environment(\.dismiss) var dismiss
    @State private var jobTitle: String = ""
    @State private var companyName: String = ""
    @State private var salaryRange: String = ""
    @State private var location: String = ""

    var body: some View {
        NavigationView {
            Form {
                Section(header: Text("Position Details")) {
                    TextField("Job Title (e.g. iOS Engineer)", text: $jobTitle)
                    TextField("Company Name", text: $companyName)
                    TextField("Salary Range (e.g. $120k - $160k)", text: $salaryRange)
                    TextField("Location (e.g. Remote / New York)", text: $location)
                }
            }
            .navigationTitle("Post a Job")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Post") { dismiss() }
                        .font(.headline)
                        .disabled(jobTitle.isEmpty || companyName.isEmpty)
                }
            }
        }
    }
}
