import SwiftUI

// MARK: - Wallet Models

public enum TransactionType: String, Codable {
    case received
    case sent
    case payout
    case reward
}

public enum TransactionStatus: String, Codable {
    case completed = "Completed"
    case pending = "Pending"
    case failed = "Failed"

    public var statusColor: Color {
        switch self {
        case .completed: return .green
        case .pending: return .orange
        case .failed: return .red
        }
    }
}

public struct WalletTransaction: Identifiable, Hashable {
    public let id: String
    public let title: String
    public let counterparty: String
    public let timestamp: String
    public let amountFormatted: String
    public let isCredit: Bool
    public let type: TransactionType
    public let status: TransactionStatus

    public init(id: String, title: String, counterparty: String, timestamp: String, amountFormatted: String, isCredit: Bool, type: TransactionType, status: TransactionStatus = .completed) {
        self.id = id
        self.title = title
        self.counterparty = counterparty
        self.timestamp = timestamp
        self.amountFormatted = amountFormatted
        self.isCredit = isCredit
        self.type = type
        self.status = status
    }
}

// MARK: - WalletView

public struct WalletView: View {
    @State private var tokenBalance: Double = 1250.75
    @State private var usdValue: Double = 1250.75 * 0.85
    @State private var selectedFilter: WalletFilter = .all
    @State private var showPayoutSheet: Bool = false
    @State private var payoutAmountText: String = ""
    @State private var selectedPayoutMethod: String = "Chase Bank (****4821)"
    @State private var alertToastMessage: String? = nil
    @State private var showAlertToast: Bool = false

    enum WalletFilter: String, CaseIterable, Identifiable {
        case all = "All"
        case received = "Received"
        case sent = "Sent"
        case payouts = "Payouts"

        var id: String { rawValue }
    }

    private let payoutMethods = ["Chase Bank (****4821)", "Stripe Connect", "Ethereum Wallet (0x71C...3a9)", "PayPal"]

    @State private var transactions: [WalletTransaction] = [
        WalletTransaction(id: "tx1", title: "Content Reward Payout", counterparty: "TJ Creator Fund", timestamp: "Today, 2:15 PM", amountFormatted: "+$120.00", isCredit: true, type: .reward),
        WalletTransaction(id: "tx2", title: "Direct Transfer", counterparty: "To @sconnor", timestamp: "Yesterday", amountFormatted: "-$45.50", isCredit: false, type: .sent),
        WalletTransaction(id: "tx3", title: "Bank Cashout Payout", counterparty: "Chase Bank ****4821", timestamp: "Sep 27, 2026", amountFormatted: "-$300.00", isCredit: false, type: .payout, status: .completed),
        WalletTransaction(id: "tx4", title: "Mingle Points Purchase", counterparty: "TJ Store", timestamp: "Sep 24, 2026", amountFormatted: "-$10.00", isCredit: false, type: .sent),
        WalletTransaction(id: "tx5", title: "Group Tip Received", counterparty: "From @alexmorgan", timestamp: "Sep 20, 2026", amountFormatted: "+$50.00", isCredit: true, type: .received)
    ]

    public init() {}

    private var filteredTransactions: [WalletTransaction] {
        transactions.filter { tx in
            switch selectedFilter {
            case .all: return true
            case .received: return tx.isCredit
            case .sent: return !tx.isCredit && tx.type != .payout
            case .payouts: return tx.type == .payout
            }
        }
    }

    public var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 20) {
                    // Balance Header Card
                    balanceCardView

                    // Quick Action Row
                    quickActionsRow

                    // Linked Payment / Payout Methods
                    linkedPayoutMethodsCard

                    // Transaction History Section
                    transactionHistorySection
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 20)
            }
            .navigationTitle("Digital Wallet")
            .navigationBarTitleDisplayMode(.inline)
            .background(Color(.systemGroupedBackground).ignoresSafeArea())
            .alert("Wallet Notice", isPresented: $showAlertToast) {
                Button("OK", role: .cancel) { }
            } message: {
                Text(alertToastMessage ?? "")
            }
            .sheet(isPresented: $showPayoutSheet) {
                payoutModalSheet
            }
        }
    }

    // MARK: - Subviews

    private var balanceCardView: some View {
        VStack(spacing: 12) {
            HStack {
                Label("TJ Token Balance", systemImage: "bolt.shield.fill")
                    .font(.caption.bold())
                    .foregroundColor(.white.opacity(0.8))
                Spacer()
                Text("USD Value: $\(String(format: "%.2f", usdValue))")
                    .font(.caption)
                    .foregroundColor(.white.opacity(0.8))
            }

            HStack(alignment: .firstTextBaseline, spacing: 4) {
                Text("\(String(format: "%.2f", tokenBalance))")
                    .font(.system(size: 38, weight: .bold))
                    .foregroundColor(.white)
                Text("TJ")
                    .font(.title3.bold())
                    .foregroundColor(.cyan)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.vertical, 4)
        }
        .padding(20)
        .background(
            LinearGradient(colors: [Color.indigo, Color.purple, Color.blue], startPoint: .topLeading, endPoint: .bottomTrailing)
        )
        .cornerRadius(20)
        .shadow(radius: 8)
    }

    private var quickActionsRow: some View {
        HStack(spacing: 12) {
            walletActionButton(title: "Send", icon: "arrow.up.right.circle.fill", color: .blue) {
                alertToastMessage = "Send tokens feature opened."
                showAlertToast = true
            }

            walletActionButton(title: "Receive", icon: "arrow.down.left.circle.fill", color: .green) {
                alertToastMessage = "Your Wallet QR Code: 0x71C...3a9"
                showAlertToast = true
            }

            walletActionButton(title: "Deposit", icon: "plus.circle.fill", color: .purple) {
                alertToastMessage = "Deposit funds via Apple Pay or Card."
                showAlertToast = true
            }

            walletActionButton(title: "Payout", icon: "banknote.fill", color: .orange) {
                payoutAmountText = ""
                showPayoutSheet = true
            }
        }
    }

    private func walletActionButton(title: String, icon: String, color: Color, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            VStack(spacing: 6) {
                Image(systemName: icon)
                    .font(.system(size: 22))
                    .foregroundColor(color)

                Text(title)
                    .font(.caption.bold())
                    .foregroundColor(.primary)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 12)
            .background(Color(.secondarySystemGroupedBackground))
            .cornerRadius(14)
        }
    }

    private var linkedPayoutMethodsCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("LINKED PAYOUT METHODS")
                .font(.caption.bold())
                .foregroundColor(.secondary)

            VStack(spacing: 0) {
                HStack {
                    Label("Chase Checking (****4821)", systemImage: "building.columns.fill")
                        .font(.subheadline.bold())
                    Spacer()
                    Text("Primary")
                        .font(.caption.bold())
                        .foregroundColor(.blue)
                }
                .padding()

                Divider().padding(.leading, 16)

                HStack {
                    Label("Stripe Merchant Account", systemImage: "creditcard.fill")
                        .font(.subheadline)
                    Spacer()
                    Text("Active")
                        .font(.caption)
                        .foregroundColor(.green)
                }
                .padding()
            }
            .background(Color(.secondarySystemGroupedBackground))
            .cornerRadius(16)
        }
    }

    private var transactionHistorySection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("TRANSACTION HISTORY")
                .font(.caption.bold())
                .foregroundColor(.secondary)

            // Filter Pills
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(WalletFilter.allCases) { filter in
                        Button(action: { selectedFilter = filter }) {
                            Text(filter.rawValue)
                                .font(.footnote)
                                .fontWeight(selectedFilter == filter ? .bold : .medium)
                                .padding(.horizontal, 14)
                                .padding(.vertical, 6)
                                .background(selectedFilter == filter ? Color.blue : Color(.tertiarySystemBackground))
                                .foregroundColor(selectedFilter == filter ? .white : .primary)
                                .cornerRadius(16)
                        }
                    }
                }
            }

            VStack(spacing: 0) {
                ForEach(filteredTransactions) { tx in
                    HStack(spacing: 12) {
                        Circle()
                            .fill(tx.isCredit ? Color.green.opacity(0.12) : Color.red.opacity(0.12))
                            .frame(width: 42, height: 42)
                            .overlay(
                                Image(systemName: tx.isCredit ? "arrow.down.left" : "arrow.up.right")
                                    .font(.system(size: 16, weight: .bold))
                                    .foregroundColor(tx.isCredit ? .green : .red)
                            )

                        VStack(alignment: .leading, spacing: 2) {
                            Text(tx.title)
                                .font(.subheadline.weight(.semibold))
                            Text("\(tx.counterparty) • \(tx.timestamp)")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }

                        Spacer()

                        VStack(alignment: .trailing, spacing: 2) {
                            Text(tx.amountFormatted)
                                .font(.subheadline.bold())
                                .foregroundColor(tx.isCredit ? .green : .primary)

                            Text(tx.status.rawValue)
                                .font(.caption2.bold())
                                .foregroundColor(tx.status.statusColor)
                        }
                    }
                    .padding()

                    if tx.id != filteredTransactions.last?.id {
                        Divider().padding(.leading, 16)
                    }
                }
            }
            .background(Color(.secondarySystemGroupedBackground))
            .cornerRadius(16)
        }
    }

    private var payoutModalSheet: some View {
        NavigationStack {
            VStack(alignment: .leading, spacing: 20) {
                Text("Request Payout")
                    .font(.title2.bold())
                    .padding(.top)

                VStack(alignment: .leading, spacing: 8) {
                    Text("Payout Amount ($ USD)")
                        .font(.caption.bold())
                        .foregroundColor(.secondary)

                    TextField("Enter amount e.g. 150.00", text: $payoutAmountText)
                        .keyboardType(.decimalPad)
                        .padding()
                        .background(Color(.secondarySystemBackground))
                        .cornerRadius(12)
                }

                VStack(alignment: .leading, spacing: 8) {
                    Text("Destination Payout Method")
                        .font(.caption.bold())
                        .foregroundColor(.secondary)

                    Menu {
                        ForEach(payoutMethods, id: \.self) { method in
                            Button(action: { selectedPayoutMethod = method }) {
                                Text(method)
                            }
                        }
                    } label: {
                        HStack {
                            Text(selectedPayoutMethod)
                                .foregroundColor(.primary)
                            Spacer()
                            Image(systemName: "chevron.down")
                                .foregroundColor(.secondary)
                        }
                        .padding()
                        .background(Color(.secondarySystemBackground))
                        .cornerRadius(12)
                    }
                }

                Spacer()

                Button(action: {
                    if let amount = Double(payoutAmountText), amount > 0 {
                        tokenBalance -= (amount / 0.85)
                        usdValue = tokenBalance * 0.85
                        transactions.insert(
                            WalletTransaction(
                                id: UUID().uuidString,
                                title: "Bank Cashout Payout",
                                counterparty: selectedPayoutMethod,
                                timestamp: "Just now",
                                amountFormatted: "-$\(String(format: "%.2f", amount))",
                                isCredit: false,
                                type: .payout,
                                status: .pending
                            ),
                            at: 0
                        )
                        showPayoutSheet = false
                        alertToastMessage = "Payout request of $\(String(format: "%.2f", amount)) submitted to \(selectedPayoutMethod)."
                        showAlertToast = true
                    }
                }) {
                    Text("Confirm & Initiate Payout")
                        .fontWeight(.bold)
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color.orange)
                        .cornerRadius(12)
                }
            }
            .padding(.horizontal)
            .navigationBarItems(trailing: Button("Cancel") { showPayoutSheet = false })
        }
        .presentationDetents([.height(380)])
    }
}

#Preview {
    WalletView()
}
