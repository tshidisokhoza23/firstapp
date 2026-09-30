import SwiftUI

public struct CategoryItem: Identifiable, Hashable {
    public let id: String
    public let name: String

    public init(id: String, name: String) {
        self.id = id
        self.name = name
    }
}

public struct CompleteProfileView: View {
    @ObservedObject private var viewModel: AuthViewModel
    private var onCompleteSuccess: () -> Void
    private var onNavigateToTerms: () -> Void

    @State private var username: String = ""
    @State private var phoneNumber: String = ""
    @State private var selectedCategories: Set<String> = []

    private let categories: [CategoryItem] = [
        CategoryItem(id: "tech", name: "Technology"),
        CategoryItem(id: "news", name: "News & Politics"),
        CategoryItem(id: "sports", name: "Sports"),
        CategoryItem(id: "music", name: "Music"),
        CategoryItem(id: "gaming", name: "Gaming"),
        CategoryItem(id: "science", name: "Science")
    ]

    public init(
        viewModel: AuthViewModel,
        onCompleteSuccess: @escaping () -> Void,
        onNavigateToTerms: @escaping () -> Void = {}
    ) {
        self.viewModel = viewModel
        self.onCompleteSuccess = onCompleteSuccess
        self.onNavigateToTerms = onNavigateToTerms
    }

    public var body: some View {
        NavigationView {
            VStack(spacing: 20) {
                Text("Just a few more details to get started!")
                    .font(.subheadline)
                    .foregroundColor(.secondary)

                VStack(alignment: .leading, spacing: 8) {
                    Text("Username").font(.caption).foregroundColor(.secondary)
                    TextField("e.g. john_doe", text: $username)
                        .textFieldStyle(RoundedBorderTextFieldStyle())
                }

                VStack(alignment: .leading, spacing: 8) {
                    Text("Phone Number").font(.caption).foregroundColor(.secondary)
                    TextField("e.g. 821234567 or 0821234567", text: $phoneNumber)
                        .keyboardType(.phonePad)
                        .textFieldStyle(RoundedBorderTextFieldStyle())
                }

                VStack(alignment: .leading, spacing: 8) {
                    Text("Select interests for your feed (3+ recommended)")
                        .font(.footnote.bold())

                    LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                        ForEach(categories) { cat in
                            let isSelected = selectedCategories.contains(cat.id)
                            Button(action: {
                                if isSelected {
                                    selectedCategories.remove(cat.id)
                                } else {
                                    selectedCategories.insert(cat.id)
                                }
                            }) {
                                Text(cat.name)
                                    .font(.system(size: 14, weight: isSelected ? .bold : .regular))
                                    .foregroundColor(isSelected ? .white : .primary)
                                    .frame(maxWidth: .infinity)
                                    .frame(height: 50)
                                    .background(isSelected ? Color.appPrimary : Color(uiColor: .secondarySystemBackground))
                                    .cornerRadius(10)
                            }
                        }
                    }
                }

                Spacer()

                if case .error(let msg, _) = viewModel.authState {
                    Text(msg)
                        .font(.caption)
                        .foregroundColor(.red)
                }

                Button(action: {
                    onNavigateToTerms()
                }) {
                    Text("By clicking Finish Setup, you agree to FirstApp's Terms of Service & EULA, and acknowledge our Privacy Policy.")
                        .font(.caption2)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal)
                }

                Button(action: {
                    viewModel.completeProfile(
                        displayName: nil,
                        username: username.isEmpty ? nil : username,
                        phoneNumber: phoneNumber.isEmpty ? nil : phoneNumber,
                        categories: Array(selectedCategories)
                    )
                }) {
                    HStack {
                        if case .loading = viewModel.authState {
                            ProgressView()
                        } else {
                            Text("Finish Setup")
                                .font(.system(size: 16, weight: .bold))
                        }
                    }
                    .frame(maxWidth: .infinity)
                    .frame(height: 50)
                    .background(
                        username.isEmpty || phoneNumber.isEmpty ? Color.gray : Color.appPrimary
                    )
                    .foregroundColor(.white)
                    .cornerRadius(12)
                }
                .disabled(username.isEmpty || phoneNumber.isEmpty)
            }
            .padding(24)
            .navigationTitle("Complete Profile")
            .navigationBarTitleDisplayMode(.inline)
            .onReceive(viewModel.$authState) { state in
                if case .success = state {
                    onCompleteSuccess()
                }
            }
        }
    }
}
