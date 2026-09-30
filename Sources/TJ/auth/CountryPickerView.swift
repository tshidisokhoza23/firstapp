import SwiftUI

struct CountryCodeItem: Identifiable {
    let id = UUID()
    let name: String
    let code: String
    let flag: String
}

struct CountryPickerView: View {
    @Environment(\.dismiss) var dismiss
    var onSelectCountry: (CountryCodeItem) -> Void
    @State private var searchText: String = ""

    let countries = [
        CountryCodeItem(name: "United States", code: "+1", flag: "🇺🇸"),
        CountryCodeItem(name: "United Kingdom", code: "+44", flag: "🇬🇧"),
        CountryCodeItem(name: "Canada", code: "+1", flag: "🇨🇦"),
        CountryCodeItem(name: "Germany", code: "+49", flag: "🇩🇪"),
        CountryCodeItem(name: "France", code: "+33", flag: "🇫🇷"),
        CountryCodeItem(name: "Japan", code: "+81", flag: "🇯🇵"),
        CountryCodeItem(name: "India", code: "+91", flag: "🇮🇳"),
        CountryCodeItem(name: "Australia", code: "+61", flag: "🇦🇺"),
        CountryCodeItem(name: "South Africa", code: "+27", flag: "🇿🇦"),
        CountryCodeItem(name: "Brazil", code: "+55", flag: "🇧🇷")
    ]

    var body: some View {
        NavigationView {
            VStack {
                HStack {
                    Image(systemName: "magnifyingglass")
                        .foregroundColor(AppColors.greyMedium)
                    TextField("Search country or code...", text: $searchText)
                }
                .padding(10)
                .background(Color(UIColor.secondarySystemBackground))
                .cornerRadius(12)
                .padding(.horizontal)

                List {
                    ForEach(countries.filter { searchText.isEmpty || $0.name.localizedCaseInsensitiveContains(searchText) || $0.code.contains(searchText) }) { country in
                        Button(action: {
                            onSelectCountry(country)
                            dismiss()
                        }) {
                            HStack {
                                Text(country.flag)
                                    .font(.title2)
                                Text(country.name)
                                    .font(.body)
                                    .foregroundColor(.primary)
                                Spacer()
                                Text(country.code)
                                    .font(.headline)
                                    .foregroundColor(AppColors.brandPrimary)
                            }
                            .padding(.vertical, 4)
                        }
                    }
                }
                .listStyle(PlainListStyle())
            }
            .navigationTitle("Select Country")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Cancel") { dismiss() }
                }
            }
        }
    }
}
