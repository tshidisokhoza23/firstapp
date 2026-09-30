import SwiftUI

struct ActiveRideSheet: View {
    @Environment(\.dismiss) var dismiss
    @EnvironmentObject var driveViewModel: DriveViewModel
    @State private var progressEta: Int = 3

    var body: some View {
        VStack(spacing: 20) {
            Capsule()
                .fill(AppColors.greyMedium)
                .frame(width: 40, height: 5)
                .padding(.top, 12)

            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text("Driver On The Way 🚕")
                        .font(.title2)
                        .fontWeight(.bold)
                    Text("Arriving in \(progressEta) mins • Toyota Camry (Black)")
                        .font(.subheadline)
                        .foregroundColor(AppColors.greyMedium)
                }
                Spacer()
            }
            .padding(.horizontal)

            HStack(spacing: 16) {
                Image(systemName: "person.crop.circle.fill")
                    .resizable()
                    .frame(width: 50, height: 50)
                    .foregroundColor(AppColors.greyMedium)

                VStack(alignment: .leading, spacing: 2) {
                    Text("Michael Vance ★ 4.9")
                        .font(.headline)
                    Text("Licence Plate: 7XYZ89")
                        .font(.caption)
                        .foregroundColor(AppColors.greyMedium)
                }

                Spacer()

                Button(action: {}) {
                    Image(systemName: "phone.circle.fill")
                        .font(.system(size: 40))
                        .foregroundColor(AppColors.brandPrimary)
                }
            }
            .padding()
            .background(Color(UIColor.secondarySystemBackground))
            .cornerRadius(16)
            .padding(.horizontal)

            Button(action: {
                driveViewModel.isBookingConfirmed = false
                dismiss()
            }) {
                Text("Cancel Ride")
                    .font(.headline)
                    .foregroundColor(.red)
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(Color.red.opacity(0.1))
                    .cornerRadius(16)
            }
            .padding(.horizontal)

            Spacer()
        }
    }
}

struct RideView: View {
    @EnvironmentObject var driveViewModel: DriveViewModel
    @State private var showingActiveRideSheet: Bool = false

    var body: some View {
        VStack(spacing: 0) {
            // Header
            HStack {
                Text("TJ Ride")
                    .font(.title2)
                    .fontWeight(.bold)
                Spacer()
                Button(action: {}) {
                    Image(systemName: "clock.arrow.circlepath")
                        .font(.title3)
                        .foregroundColor(.primary)
                }
            }
            .padding(.horizontal)
            .padding(.vertical, 12)

            // Map Placeholder Card
            ZStack {
                RoundedRectangle(cornerRadius: 16)
                    .fill(AppColors.brandPrimary.opacity(0.1))
                    .frame(height: 180)

                VStack(spacing: 8) {
                    Image(systemName: "map.fill")
                        .font(.system(size: 40))
                        .foregroundColor(AppColors.brandPrimary)
                    Text("Interactive Map Route")
                        .font(.subheadline)
                        .fontWeight(.semibold)
                        .foregroundColor(AppColors.brandPrimary)
                }
            }
            .padding(.horizontal)

            // Pickup & Destination Input Card
            VStack(spacing: 12) {
                HStack {
                    Circle().fill(Color.green).frame(width: 10, height: 10)
                    TextField("Pickup Location", text: $driveViewModel.pickupLocation)
                        .font(.system(size: 14))
                }

                Divider()

                HStack {
                    Rectangle().fill(AppColors.brandPrimary).frame(width: 10, height: 10)
                    TextField("Destination", text: $driveViewModel.destination)
                        .font(.system(size: 14))
                }
            }
            .padding()
            .background(Color(UIColor.secondarySystemBackground))
            .cornerRadius(16)
            .padding()

            // Ride Options
            VStack(alignment: .leading, spacing: 12) {
                Text("Choose a ride")
                    .font(.headline)
                    .padding(.horizontal)

                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 12) {
                        ForEach(driveViewModel.rideOptions) { option in
                            Button(action: {
                                driveViewModel.selectedOption = option
                            }) {
                                VStack(spacing: 8) {
                                    Image(systemName: option.iconName)
                                        .font(.title)
                                        .foregroundColor(driveViewModel.selectedOption?.id == option.id ? AppColors.brandPrimary : AppColors.greyMedium)
                                    Text(option.name)
                                        .font(.system(size: 13, weight: .semibold))
                                        .foregroundColor(.primary)
                                    Text(option.price)
                                        .font(.system(size: 15, weight: .bold))
                                        .foregroundColor(AppColors.brandPrimary)
                                    Text("\(option.etaMinutes) min away")
                                        .font(.system(size: 10))
                                        .foregroundColor(AppColors.greyMedium)
                                }
                                .padding()
                                .frame(width: 110)
                                .background(driveViewModel.selectedOption?.id == option.id ? AppColors.brandPrimary.opacity(0.1) : Color(UIColor.tertiarySystemBackground))
                                .cornerRadius(16)
                                .overlay(
                                    RoundedRectangle(cornerRadius: 16)
                                        .stroke(driveViewModel.selectedOption?.id == option.id ? AppColors.brandPrimary : Color.clear, lineWidth: 2)
                                )
                            }
                        }
                    }
                    .padding(.horizontal)
                }
            }

            Spacer()

            // Confirm Booking Button
            Button(action: {
                driveViewModel.bookRide()
                showingActiveRideSheet = true
            }) {
                Text(driveViewModel.isBookingConfirmed ? "Track Driver 🚕" : "Confirm Ride")
                    .font(.system(size: 16, weight: .bold))
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(driveViewModel.isBookingConfirmed ? Color.green : AppColors.brandPrimary)
                    .cornerRadius(16)
            }
            .padding(.horizontal)
            .padding(.bottom, 70)
        }
        .sheet(isPresented: $showingActiveRideSheet) {
            ActiveRideSheet()
        }
    }
}
