package com.oponexis.companion.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DestinationTest {
    @Test
    fun `bottom destinations are unique and exclude startup screens`() {
        val routes = Destination.bottomBarItems.map { it.route }

        assertEquals(routes.size, routes.toSet().size)
        assertFalse(Destination.Splash.route in routes)
        assertFalse(Destination.Onboarding.route in routes)
        assertTrue(Destination.Dashboard.route in routes)
    }
}

