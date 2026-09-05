import Foundation
import FamilyControls

public struct Rule: Identifiable, Codable {
    public var id: UUID
    public var productiveSelection: FamilyActivitySelection
    public var blockedSelection: FamilyActivitySelection
    public var productiveMinutesTarget: Int
    public var rewardMinutes: Int
    public var dailyCapMinutes: Int
    public var emergencyPassword: String
    public var isActive: Bool
    public var createdAt: Date

    public init(
        id: UUID = UUID(),
        productiveSelection: FamilyActivitySelection = FamilyActivitySelection(),
        blockedSelection: FamilyActivitySelection = FamilyActivitySelection(),
        productiveMinutesTarget: Int = 30,
        rewardMinutes: Int = 20,
        dailyCapMinutes: Int = 120,
        emergencyPassword: String = "",
        isActive: Bool = true,
        createdAt: Date = Date()
    ) {
        self.id = id
        self.productiveSelection = productiveSelection
        self.blockedSelection = blockedSelection
        self.productiveMinutesTarget = productiveMinutesTarget
        self.rewardMinutes = rewardMinutes
        self.dailyCapMinutes = dailyCapMinutes
        self.emergencyPassword = emergencyPassword
        self.isActive = isActive
        self.createdAt = createdAt
    }
}
