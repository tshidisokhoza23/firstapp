import SwiftUI

struct CalendarView: View {
    @Environment(\.dismiss) var dismiss

    var body: some View {
        NavigationView {
            VStack {
                DatePicker("Select Date", selection: .constant(Date()), displayedComponents: [.date])
                    .datePickerStyle(GraphicalDatePickerStyle())
                    .padding()

                List {
                    Section(header: Text("Upcoming Events")) {
                        HStack {
                            Circle().fill(AppColors.brandPrimary).frame(width: 8, height: 8)
                            VStack(alignment: .leading) {
                                Text("TJ Platform Sync")
                                    .font(.headline)
                                Text("11:00 AM • Virtual Meeting")
                                    .font(.caption)
                                    .foregroundColor(AppColors.greyMedium)
                            }
                        }
                    }
                }
            }
            .navigationTitle("Calendar")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Done") { dismiss() }
                }
            }
        }
    }
}
