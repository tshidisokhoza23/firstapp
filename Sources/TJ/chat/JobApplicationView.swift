import SwiftUI

struct JobApplicationView: View {
    let job: JobItemModel
    @Environment(\.dismiss) var dismiss
    @State private var applicantName: String = "Alex Morgan"
    @State private var email: String = "alex.morgan@example.com"
    @State private var coverLetter: String = ""
    @State private var isSubmitted: Bool = false

    var body: some View {
        NavigationView {
            Form {
                Section(header: Text("Applicant Information")) {
                    TextField("Full Name", text: $applicantName)
                    TextField("Email", text: $email)
                }

                Section(header: Text("Cover Letter")) {
                    TextEditor(text: $coverLetter)
                        .frame(height: 120)
                }

                Section(header: Text("Resume")) {
                    HStack {
                        Image(systemName: "doc.text.fill")
                            .foregroundColor(AppColors.brandPrimary)
                        Text("Alex_Morgan_Resume.pdf")
                            .font(.subheadline)
                        Spacer()
                        Text("Attached")
                            .font(.caption)
                            .foregroundColor(.green)
                    }
                }

                Section {
                    Button(action: {
                        isSubmitted = true
                        DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) {
                            dismiss()
                        }
                    }) {
                        Text(isSubmitted ? "Application Submitted! 🎉" : "Submit Application")
                            .font(.headline)
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 8)
                    }
                    .listRowBackground(isSubmitted ? Color.green : AppColors.brandPrimary)
                }
            }
            .navigationTitle("Apply for \(job.title)")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancel") { dismiss() }
                }
            }
        }
    }
}
