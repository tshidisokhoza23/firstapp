import SwiftUI

struct JobsListView: View {
    @StateObject private var jobsViewModel = JobsViewModel()

    var body: some View {
        List(jobsViewModel.jobs) { job in
            VStack(alignment: .leading, spacing: 8) {
                Text(job.title)
                    .font(.system(size: 16, weight: .bold))
                HStack {
                    Text(job.company)
                        .font(.system(size: 14, weight: .medium))
                        .foregroundColor(AppColors.brandPrimary)
                    Text("•")
                    Text(job.location)
                        .font(.system(size: 13))
                        .foregroundColor(AppColors.greyMedium)
                }
                HStack {
                    Text(job.salaryRange)
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundColor(.green)
                    Spacer()
                    Text(job.jobType)
                        .font(.system(size: 11, weight: .bold))
                        .padding(.horizontal, 8)
                        .padding(.vertical, 4)
                        .background(AppColors.greyLight)
                        .cornerRadius(6)
                }
            }
            .padding(.vertical, 8)
        }
        .listStyle(PlainListStyle())
    }
}
