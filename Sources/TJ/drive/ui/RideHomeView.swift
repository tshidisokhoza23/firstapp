import SwiftUI

/// Android Ride tab. Uses the same ride, destination, and trip endpoints. No sample files.
public struct RideHomeView: View {
    @State private var options: [RideOption] = []
    @State private var destinations: [Destination] = []
    @State private var trips: [Trip] = []
    @State private var pickup = ""
    @State private var destination = ""
    @State private var selectedType = ""
    @State private var note = "Loading rides..."
    @State private var activeRideId = ""

    public init() {}

    public var body: some View {
        NavigationStack {
            List {
                Section("Request a ride") {
                    TextField("Pickup", text: $pickup)
                    TextField("Destination", text: $destination)
                    if options.isEmpty {
                        Text(note).foregroundColor(.secondary)
                    } else {
                        ForEach(options) { option in
                            Button {
                                selectedType = option.title
                            } label: {
                                HStack {
                                    VStack(alignment: .leading) {
                                        Text(option.title).font(.headline)
                                        Text(option.description).font(.caption).foregroundColor(.secondary)
                                    }
                                    Spacer()
                                    Text(option.price)
                                    if selectedType == option.title {
                                        Image(systemName: "checkmark")
                                    }
                                }
                            }
                        }
                    }
                    Button("Request ride") { Task { await requestRide() } }
                        .disabled(pickup.isEmpty || destination.isEmpty)
                    if !activeRideId.isEmpty {
                        Button("Cancel ride") { Task { await cancelRide() } }
                    }
                }
                Section("Saved places") {
                    if destinations.isEmpty {
                        Text("No saved places").foregroundColor(.secondary)
                    }
                    ForEach(destinations) { place in
                        Button {
                            destination = place.address.isEmpty ? place.name : place.address
                        } label: {
                            VStack(alignment: .leading) {
                                Text(place.name)
                                Text(place.address).font(.caption).foregroundColor(.secondary)
                            }
                        }
                    }
                }
                Section("Trips") {
                    if trips.isEmpty {
                        Text("No trips yet").foregroundColor(.secondary)
                    }
                    ForEach(trips) { trip in
                        VStack(alignment: .leading, spacing: 4) {
                            Text(trip.destinationAddress).font(.headline)
                            Text(trip.pickupAddress).font(.caption)
                            Text(trip.status).font(.caption).foregroundColor(.secondary)
                        }
                    }
                }
            }
            .navigationTitle("Ride")
            .task { await reload() }
        }
    }

    private func reload() async {
        let repo = DriveRepository.shared
        options = (try? await repo.getRideOptions()) ?? []
        destinations = (try? await repo.getDestinations()) ?? []
        trips = (try? await repo.getTrips()) ?? []
        if selectedType.isEmpty { selectedType = options.first?.title ?? "" }
        note = options.isEmpty ? "No ride options" : ""
    }

    private func requestRide() async {
        guard let loc = await CourtLocation.shared.current() else {
            note = "Location is required to request a ride"
            return
        }
        let lat = loc.coordinate.latitude
        let lng = loc.coordinate.longitude
        guard lat != 0 || lng != 0 else {
            note = "Location is required to request a ride"
            return
        }
        do {
            activeRideId = try await DriveRepository.shared.createRideRequest(
                pickup: pickup,
                dest: destination,
                pLat: lat,
                pLng: lng,
                dLat: lat,
                dLng: lng,
                fare: 0,
                distance: "",
                type: selectedType
            )
            note = "Ride requested"
            await reload()
        } catch {
            note = "Could not request a ride"
        }
    }

    private func cancelRide() async {
        guard !activeRideId.isEmpty else { return }
        _ = try? await DriveRepository.shared.cancelRide(rideId: activeRideId, reason: "cancelled", by: "rider")
        activeRideId = ""
        await reload()
    }
}
