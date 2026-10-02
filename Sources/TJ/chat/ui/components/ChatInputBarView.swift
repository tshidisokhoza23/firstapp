import SwiftUI

public struct ChatInputBarView: View {
    @Binding public var text: String
    public var isRecording: Bool = false
    public var recordingDuration: Int = 0
    public var isReviewingVoiceNote: Bool = false
    public var isPlayingReview: Bool = false
    public var enterIsSend: Bool = false
    public var placeholder: String = "Message..."

    public var onSend: () -> Void = {}
    public var onAttachClick: () -> Void = {}
    public var onEmojiClick: () -> Void = {}
    public var onCameraClick: () -> Void = {}
    public var onMicClick: () -> Void = {}
    public var onSendVoiceNote: () -> Void = {}
    public var onDiscardVoiceNote: () -> Void = {}
    public var onTogglePlayReview: () -> Void = {}

    public init(
        text: Binding<String>,
        isRecording: Bool = false,
        recordingDuration: Int = 0,
        isReviewingVoiceNote: Bool = false,
        isPlayingReview: Bool = false,
        enterIsSend: Bool = false,
        placeholder: String = "Message...",
        onSend: @escaping () -> Void = {},
        onAttachClick: @escaping () -> Void = {},
        onEmojiClick: @escaping () -> Void = {},
        onCameraClick: @escaping () -> Void = {},
        onMicClick: @escaping () -> Void = {},
        onSendVoiceNote: @escaping () -> Void = {},
        onDiscardVoiceNote: @escaping () -> Void = {},
        onTogglePlayReview: @escaping () -> Void = {}
    ) {
        self._text = text
        self.isRecording = isRecording
        self.recordingDuration = recordingDuration
        self.isReviewingVoiceNote = isReviewingVoiceNote
        self.isPlayingReview = isPlayingReview
        self.enterIsSend = enterIsSend
        self.placeholder = placeholder
        self.onSend = onSend
        self.onAttachClick = onAttachClick
        self.onEmojiClick = onEmojiClick
        self.onCameraClick = onCameraClick
        self.onMicClick = onMicClick
        self.onSendVoiceNote = onSendVoiceNote
        self.onDiscardVoiceNote = onDiscardVoiceNote
        self.onTogglePlayReview = onTogglePlayReview
    }

    public var body: some View {
        HStack(alignment: .bottom, spacing: 8) {
            HStack(spacing: 8) {
                if isReviewingVoiceNote {
                    Button(action: onDiscardVoiceNote) {
                        Image(systemName: "trash")
                            .foregroundColor(.red)
                    }

                    Button(action: onTogglePlayReview) {
                        Image(systemName: isPlayingReview ? "pause.fill" : "play.fill")
                            .foregroundColor(.accentColor)
                    }

                    Text("Voice Note Preview")
                        .font(.system(size: 14, weight: .medium))

                    Spacer()
                } else if isRecording {
                    Image(systemName: "mic.fill")
                        .foregroundColor(.red)

                    Text(String(format: "%d:%02d", recordingDuration / 60, recordingDuration % 60))
                        .font(.system(size: 16))

                    Text("Recording...")
                        .font(.system(size: 12, weight: .bold))
                        .foregroundColor(.red)

                    Spacer()
                } else {
                    Button(action: onEmojiClick) {
                        Image(systemName: "face.smiling")
                            .foregroundColor(.secondary)
                    }

                    TextField(placeholder, text: $text)
                        .textFieldStyle(PlainTextFieldStyle())
                        .submitLabel(enterIsSend ? .send : .return)
                        .onSubmit {
                            guard enterIsSend else { return }
                            let trimmed = text.trimmingCharacters(in: .whitespacesAndNewlines)
                            if !trimmed.isEmpty { onSend() }
                        }

                    Button(action: onAttachClick) {
                        Image(systemName: "paperclip")
                            .foregroundColor(.secondary)
                    }

                    if text.isEmpty {
                        Button(action: onCameraClick) {
                            Image(systemName: "camera")
                                .foregroundColor(.secondary)
                        }
                    }
                }
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .background(Color(UIColor.secondarySystemBackground))
            .clipShape(RoundedRectangle(cornerRadius: 24))

            Button(action: {
                if isReviewingVoiceNote {
                    onSendVoiceNote()
                } else if !text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                    onSend()
                } else {
                    onMicClick()
                }
            }) {
                ZStack {
                    Circle()
                        .fill(isRecording ? Color.red : Color.accentColor)
                        .frame(width: 48, height: 48)

                    Image(systemName: isReviewingVoiceNote || !text.isEmpty ? "paperplane.fill" : (isRecording ? "stop.fill" : "mic.fill"))
                        .font(.system(size: 20))
                        .foregroundColor(.white)
                }
            }
        }
        .padding(.horizontal, 8)
        .padding(.bottom, 8)
    }
}
