import SwiftUI

struct HealthView: View {
    @Environment(\.presentationMode) var presentationMode
    @ObservedObject var screenTimeManager = ScreenTimeManager.shared

    var body: some View {
        NavigationView {
            ZStack {
                Color(red: 15/255.0, green: 23/255.0, blue: 42/255.0)
                    .ignoresSafeArea()

                VStack(alignment: .leading, spacing: 20) {
                    Text("System Diagnostics")
                        .font(.title2)
                        .fontWeight(.bold)
                        .foregroundColor(.white)

                    Text("Verify Screen Time permissions and background extension status.")
                        .font(.subheadline)
                        .foregroundColor(Color(red: 148/255.0, green: 163/255.0, blue: 184/255.0))

                    HStack {
                        Image(systemName: screenTimeManager.isAuthorized ? "checkmark.circle.fill" : "xmark.circle.fill")
                            .foregroundColor(screenTimeManager.isAuthorized ? .green : .red)
                            .font(.title2)

                        VStack(alignment: .leading) {
                            Text("Screen Time Permission")
                                .fontWeight(.bold)
                                .foregroundColor(.white)
                            Text(screenTimeManager.isAuthorized ? "Authorized and active" : "Not authorized")
                                .font(.caption)
                                .foregroundColor(Color(red: 148/255.0, green: 163/255.0, blue: 184/255.0))
                        }
                    }
                    .padding()
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(Color(red: 30/255.0, green: 41/255.0, blue: 59/255.0))
                    .cornerRadius(16)

                    Spacer()
                }
                .padding()
            }
            .navigationTitle("Permission Health")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Done") {
                        presentationMode.wrappedValue.dismiss()
                    }
                    .foregroundColor(Color(red: 99/255.0, green: 102/255.0, blue: 241/255.0))
                }
            }
        }
    }
}
