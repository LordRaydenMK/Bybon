package dev.sanastasov.bybon.workout

import org.junit.Test

class WorkoutSessionNotificationPresentationTest {

    @Test
    fun `ongoing stays silent on the default workout channel`() {
        val presentation = WorkoutSessionNotificationKind.Ongoing.toPresentation()

        assert(presentation.channelId == WORKOUT_SESSION_CHANNEL_ID)
        assert(presentation.silent)
        assert(presentation.onlyAlertOnce)
        assert(presentation.restOverTitle == false)
    }

    @Test
    fun `rest complete re-alerts the same notification on the high channel`() {
        val presentation = WorkoutSessionNotificationKind.RestComplete.toPresentation()

        assert(presentation.channelId == REST_COMPLETE_CHANNEL_ID)
        assert(presentation.silent == false)
        assert(presentation.onlyAlertOnce == false)
        assert(presentation.restOverTitle)
        assert(WORKOUT_SESSION_CHANNEL_ID != REST_COMPLETE_CHANNEL_ID)
        assert(WORKOUT_SESSION_NOTIFICATION_ID == 1001)
        assert(LEGACY_WORKOUT_SESSION_CHANNEL_ID == "workout_session")
    }
}
