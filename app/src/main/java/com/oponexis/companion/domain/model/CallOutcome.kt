package com.oponexis.companion.domain.model

enum class CallOutcomeCode(val persistedCode: String, val label: String) {
    Interested("interested", "Interested"),
    FollowUpRequired("follow_up_required", "Follow-up required"),
    NotInterested("not_interested", "Not interested"),
    WrongNumber("wrong_number", "Wrong number"),
	NoAvailability("no_availability", "No available appointment"),
	TopicAgriculture("topic_agriculture", "Agriculture"),
	TopicConstruction("topic_construction", "Construction equipment"),
	TopicTrucks("topic_trucks", "Trucks"),
	TopicOther("topic_other", "Other topic"),
    Other("other", "Other"),
}

data class PendingCallOutcome(
    val callRef: String,
    val observedAtEpochMillis: Long,
    val disconnectCategory: String,
    val durationBucket: String,
    val phoneNumber: String?,
    val displayName: String?,
    val isReturningCustomer: Boolean = false,
)
