import SwiftUI
import UniformTypeIdentifiers

public struct JobProfileView: View {
    public var onClose: () -> Void
    public var onNavigateToEditExperience: (String?) -> Void = { _ in }

    @StateObject private var viewModel: JobProfileViewModel

    @State private var headline: String = ""
    @State private var fullName: String = ""
    @State private var email: String = ""
    @State private var phone: String = ""
    @State private var city: String = ""
    @State private var country: String = "ZA"
    @State private var education: String = ""
    @State private var experience: String = ""
    @State private var noFormalExperience: Bool = false
    @State private var skillChips: [String] = []
    @State private var newSkillInput: String = ""
    @State private var aboutMe: String = ""
    @State private var linkedinUrl: String = ""
    @State private var portfolioUrl: String = ""
    @State private var workPreference: String = "Remote"
    @State private var availability: String = "Immediately"
    @State private var expectedSalary: String = ""
    @State private var showingFileImporter = false

    public init(
        onClose: @escaping () -> Void,
        onNavigateToEditExperience: @escaping (String?) -> Void = { _ in },
        viewModel: JobProfileViewModel = JobProfileViewModel()
    ) {
        self.onClose = onClose
        self.onNavigateToEditExperience = onNavigateToEditExperience
        self._viewModel = StateObject(wrappedValue: viewModel)
    }

    public var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    VStack(alignment: .leading, spacing: 4) {
                        Text("Showcase your talent")
                            .font(.title2.bold())
                        Text("Complete your profile to get matched with personalized job opportunities.")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                    }

                    SectionHeaderView(title: "TARGET ROLE & TITLE")
                    ProfileTextFieldView(value: $headline, label: "Target Role / Headline", placeholder: "e.g. Senior Software Developer, Driver")

                    SectionHeaderView(title: "PERSONAL INFORMATION")
                    ProfileTextFieldView(value: $fullName, label: "Full Name")
                    ProfileTextFieldView(value: $email, label: "Email Address")
                    ProfileTextFieldView(value: $phone, label: "Phone Number")

                    HStack(spacing: 8) {
                        ProfileTextFieldView(value: $city, label: "City / Region", placeholder: "e.g. Johannesburg")
                        ProfileTextFieldView(value: $country, label: "Country Code", placeholder: "ZA")
                            .frame(width: 100)
                    }

                    SectionHeaderView(title: "PROFESSIONAL LINKS")
                    ProfileTextFieldView(value: $linkedinUrl, label: "LinkedIn Profile URL", placeholder: "https://linkedin.com/in/...")
                    ProfileTextFieldView(value: $portfolioUrl, label: "Portfolio/GitHub URL", placeholder: "https://github.com/...")

                    SectionHeaderView(title: "PROFESSIONAL SUMMARY")
                    ProfileTextFieldView(value: $aboutMe, label: "About Me", placeholder: "Highlight your career achievements...", isMultiline: true)

                    SectionHeaderView(title: "EXPERIENCE & SKILLS")
                    ProfileTextFieldView(value: $education, label: "Highest Education")

                    Toggle(isOn: $noFormalExperience) {
                        Text("I am a student or career changer (No formal experience yet)")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                    }

                    // Key Skills
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Key Skills (Tap to add tags)")
                            .font(.caption)
                            .foregroundColor(.secondary)

                        HStack {
                            TextField("Type skill (e.g. Swift)", text: $newSkillInput)
                                .textFieldStyle(.roundedBorder)
                            Button("Add") {
                                let trimmed = newSkillInput.trimmingCharacters(in: .whitespaces)
                                if !trimmed.isEmpty && !skillChips.contains(trimmed) {
                                    skillChips.append(trimmed)
                                    newSkillInput = ""
                                }
                            }
                            .buttonStyle(.borderedProminent)
                        }

                        if !skillChips.isEmpty {
                            FlowLayout(spacing: 8) {
                                ForEach(skillChips, id: \.self) { chip in
                                    HStack(spacing: 4) {
                                        Text(chip)
                                            .font(.caption.bold())
                                        Button(action: {
                                            skillChips.removeAll { $0 == chip }
                                        }) {
                                            Image(systemName: "xmark")
                                                .font(.caption2)
                                        }
                                    }
                                    .padding(.horizontal, 10)
                                    .padding(.vertical, 6)
                                    .background(Color.blue.opacity(0.15))
                                    .foregroundColor(.blue)
                                    .cornerRadius(16)
                                }
                            }
                        }
                    }

                    SectionHeaderView(title: "JOB PREFERENCES")

                    VStack(alignment: .leading, spacing: 8) {
                        Text("Work Preference").font(.caption).foregroundColor(.secondary)
                        HStack(spacing: 8) {
                            ForEach(["Remote", "On-site", "Hybrid"], id: \.self) { pref in
                                Button(action: { workPreference = pref }) {
                                    Text(pref)
                                        .font(.subheadline)
                                        .padding(.horizontal, 12)
                                        .padding(.vertical, 6)
                                        .background(workPreference == pref ? Color.blue : Color.secondary.opacity(0.1))
                                        .foregroundColor(workPreference == pref ? .white : .primary)
                                        .cornerRadius(16)
                                }
                            }
                        }
                    }

                    VStack(alignment: .leading, spacing: 4) {
                        Text("Expected Monthly Salary (ZAR / R)").font(.caption).foregroundColor(.secondary)
                        HStack {
                            Text("R ").font(.headline).foregroundColor(.blue)
                            TextField("15,000", text: $expectedSalary)
                                .textFieldStyle(.roundedBorder)
                        }
                    }

                    ProfileTextFieldView(value: $availability, label: "Availability", placeholder: "e.g. 2 weeks notice / Immediate")

                    SectionHeaderView(title: "RESUME")
                    Button(action: { showingFileImporter = true }) {
                        HStack {
                            Image(systemName: "doc.badge.plus")
                            Text("UPLOAD CV / RESUME (PDF)")
                                .fontWeight(.bold)
                        }
                        .frame(maxWidth: .infinity)
                        .frame(height: 50)
                        .overlay(RoundedRectangle(cornerRadius: 8).stroke(Color.blue, lineWidth: 1))
                    }

                    Spacer().frame(height: 16)

                    Button(action: {
                        let combinedSkills = skillChips.joined(separator: ", ")
                        viewModel.updateProfile(
                            JobProfileData(
                                fullName: fullName,
                                email: email,
                                phone: phone,
                                location: city.isEmpty ? country : "\(city), \(country)",
                                city: city,
                                country: country,
                                education: education,
                                experience: experience,
                                skills: combinedSkills,
                                skillsList: skillChips,
                                aboutMe: aboutMe,
                                linkedinUrl: linkedinUrl,
                                portfolioUrl: portfolioUrl,
                                workPreference: workPreference,
                                availability: availability,
                                expectedSalary: expectedSalary,
                                currency: "ZAR",
                                headline: headline,
                                targetJobTitles: headline.isEmpty ? [] : [headline],
                                noFormalExperience: noFormalExperience
                            )
                        )
                    }) {
                        Text("SAVE PROFESSIONAL PROFILE")
                            .font(.headline)
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(Color.blue)
                            .foregroundColor(.white)
                            .cornerRadius(8)
                    }
                }
                .padding(16)
            }
            .navigationTitle("Professional Profile")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(action: onClose) {
                        Image(systemName: "xmark")
                    }
                }
            }
            .onAppear {
                if case .success(let data) = viewModel.profile {
                    populateFields(data)
                }
            }
            .onChange(of: viewModel.profile) { res in
                if case .success(let data) = res {
                    populateFields(data)
                }
            }
            .onChange(of: viewModel.saveResult) { res in
                if case .success = res {
                    onClose()
                    viewModel.resetState()
                }
            }
            .fileImporter(isPresented: $showingFileImporter, allowedContentTypes: [.pdf]) { result in
                switch result {
                case .success(let url):
                    print("Selected file: \(url)")
                case .failure(let error):
                    print("Import failed: \(error)")
                }
            }
        }
    }

    private func populateFields(_ data: JobProfileData) {
        headline = data.headline
        fullName = data.fullName
        email = data.email
        phone = data.phone
        city = data.city.isEmpty ? data.location : data.city
        country = data.country
        education = data.education
        experience = data.experience
        noFormalExperience = data.noFormalExperience
        skillChips = data.getParsedSkills()
        aboutMe = data.aboutMe
        linkedinUrl = data.linkedinUrl
        portfolioUrl = data.portfolioUrl
        workPreference = data.workPreference
        availability = data.availability
        expectedSalary = data.expectedSalary
    }
}

public struct SectionHeaderView: View {
    public let title: String

    public var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(title)
                .font(.system(size: 12, weight: .black))
                .foregroundColor(.blue)
                .tracking(1)
            Divider()
                .background(Color.blue.opacity(0.3))
        }
        .padding(.top, 8)
    }
}

public struct ProfileTextFieldView: View {
    @Binding public var value: String
    public let label: String
    public var placeholder: String = ""
    public var isMultiline: Bool = false

    public var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label)
                .font(.system(size: 14))
                .foregroundColor(.secondary)

            if isMultiline {
                TextEditor(text: $value)
                    .frame(minHeight: 80)
                    .overlay(RoundedRectangle(cornerRadius: 8).stroke(Color.secondary.opacity(0.3), lineWidth: 1))
            } else {
                TextField(placeholder, text: $value)
                    .textFieldStyle(.roundedBorder)
            }
        }
    }
}

public struct FlowLayout: Layout {
    public var spacing: CGFloat = 8

    public init(spacing: CGFloat = 8) {
        self.spacing = spacing
    }

    public func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let width = proposal.width ?? .infinity
        var height: CGFloat = 0
        var x: CGFloat = 0
        var y: CGFloat = 0
        var maxHeight: CGFloat = 0

        for subview in subviews {
            let size = subview.sizeThatFits(.unspecified)
            if x + size.width > width {
                x = 0
                y += maxHeight + spacing
                maxHeight = 0
            }
            x += size.width + spacing
            maxHeight = max(maxHeight, size.height)
        }
        height = y + maxHeight
        return CGSize(width: width, height: height)
    }

    public func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        var x = bounds.minX
        var y = bounds.minY
        var maxHeight: CGFloat = 0

        for subview in subviews {
            let size = subview.sizeThatFits(.unspecified)
            if x + size.width > bounds.maxX {
                x = bounds.minX
                y += maxHeight + spacing
                maxHeight = 0
            }
            subview.place(at: CGPoint(x: x, y: y), proposal: .unspecified)
            x += size.width + spacing
            maxHeight = max(maxHeight, size.height)
        }
    }
}
