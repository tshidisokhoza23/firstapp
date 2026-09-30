import SwiftUI
import Combine

class DriveViewModel: ObservableObject {
    @Published var pickupLocation: String = "Current Location"
    @Published var destination: String = "Financial District, SF"
    @Published var rideOptions: [RideOptionModel] = RideOptionModel.samples
    @Published var selectedOption: RideOptionModel? = RideOptionModel.samples.first
    @Published var isBookingConfirmed: Bool = false

    func bookRide() {
        isBookingConfirmed = true
    }
}
