package com.threemdroid.digitalwallet.core.navigation

import com.threemdroid.digitalwallet.feature.addcard.AddCardRoutes
import com.threemdroid.digitalwallet.feature.categorydetails.CategoryDetailsRoutes
import com.threemdroid.digitalwallet.feature.carddetails.CardDetailsRoutes
import com.threemdroid.digitalwallet.feature.carddetails.EditCardRoutes
import com.threemdroid.digitalwallet.feature.fullscreencode.FullscreenCodeRoutes
import org.junit.Assert.assertEquals
import org.junit.Test

class NavRouteEncodingTest {

    @Test
    fun encodeRouteValue_encodesReservedCharacters() {
        assertEquals(
            "card%2Fid%3Fvalue%3D1%20two",
            encodeRouteValue("card/id?value=1 two")
        )
    }

    @Test
    fun persistedIdRoutes_encodeIdsSafely() {
        val rawId = "id/with spaces?and=value"
        val encodedId = "id%2Fwith%20spaces%3Fand%3Dvalue"

        assertEquals(
            "add_card/category/$encodedId",
            AddCardRoutes.chooserFromCategory(rawId)
        )
        assertEquals(
            "home/category-details/$encodedId",
            CategoryDetailsRoutes.categoryDetails(rawId)
        )
        assertEquals(
            "home/card-details/$encodedId",
            CardDetailsRoutes.cardDetails(rawId)
        )
        assertEquals(
            "home/card-details/$encodedId/edit",
            EditCardRoutes.editCard(rawId)
        )
        assertEquals(
            "home/card-details/$encodedId/fullscreen-code",
            FullscreenCodeRoutes.fullscreenCode(rawId)
        )
    }
}
