import SwiftUI
import Combine

class JobsViewModel: ObservableObject {
    @Published var jobs: [JobItemModel] = JobItemModel.samples
    @Published var selectedJob: JobItemModel? = nil
}
