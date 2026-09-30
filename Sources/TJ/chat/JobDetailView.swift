import SwiftUI

struct JobDetailView: View {
    let job: JobItemModel
    @Environment(\.dismiss) var dismiss
    @State private var showingApplySheet: Bool = false

    var body: some View {
        NavigationView {
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    HStack(spacing: 16) {
                        RoundedRectangle(cornerRadius: 16)
                            .fill(AppColors.brandPrimary.opacity(0.15))
                            .frame(width: 60, height: 60)
                            .overlay(
                                Image(systemName: "briefcase.fill")
                                    .font(.title)
                                    .foregroundColor(AppColors.brandPrimary)
                            )

                        VStack(alignment: .leading, spacing: 4) {
                            Text(job.title)
                                .font(.title3)
                                .fontWeight(.bold)
                            Text(job.company)
                                .font(.headline)
                                .foregroundColor(AppColors.brandPrimary)
                        }
                    }

                    HStack(spacing: 12) {
                        Label(job.location, systemImage: "location.fill")
                        Label(job.jobType, systemImage: "clock.fill")
                    }
                    .font(.caption)
                    .foregroundColor(AppColors.greyMedium)

                    Divider()

                    Text("Salary Range")
                        .font(.headline)
                    Text(job.salaryRange)
                        .font(.title2)
                        .fontWeight(.bold)
                        .foregroundColor(.green)

                    Divider()

                    Text("Job Description")
                        .font(.headline)
                    Text(job.description)
                        .font(.body)
                        .foregroundColor(AppColors.greyMedium)

                    Spacer(minLength: 40)

                    Button(action: { showingApplySheet = true }) {
                        Text("Apply Now")
                            .font(.headline)
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(AppColors.brandPrimary)
                            .cornerRadius(16)
                    }
                }
                .padding()
            }
            .navigationTitle("Job Details")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Done") { dismiss() }
                }
            }
            .sheet(isPresented: $showingApplySheet) {
                JobApplicationView(job: job)
            }
        }
    }
}
