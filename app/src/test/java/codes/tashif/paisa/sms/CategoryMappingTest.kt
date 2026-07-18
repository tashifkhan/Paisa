package codes.tashif.paisa.sms

import org.junit.Assert.assertEquals
import org.junit.Test

class CategoryMappingTest {

    @Test
    fun zomato_is_food_dining() {
        assertEquals("Food & Dining", CategoryMapping.getCategory("ZOMATO"))
        assertEquals("Food & Dining", CategoryMapping.getCategory("Swiggy Bangalore"))
    }

    @Test
    fun grocery_merchants() {
        assertEquals("Groceries", CategoryMapping.getCategory("BigBasket"))
        assertEquals("Groceries", CategoryMapping.getCategory("Blinkit Order"))
    }

    @Test
    fun transport_not_transportation_label() {
        assertEquals("Transport", CategoryMapping.getCategory("Uber Trip"))
        assertEquals("Transport", CategoryMapping.getCategory("Ola Cabs"))
    }

    @Test
    fun fuel_preferred_over_generic_transport_keywords() {
        assertEquals("Fuel", CategoryMapping.getCategory("Shell Petrol"))
    }

    @Test
    fun subscriptions_streaming() {
        assertEquals("Subscriptions", CategoryMapping.getCategory("Netflix.com"))
        assertEquals("Subscriptions", CategoryMapping.getCategory("Spotify AB"))
    }

    @Test
    fun health_fitness_unified() {
        assertEquals("Health & Fitness", CategoryMapping.getCategory("Apollo Pharmacy"))
        assertEquals("Health & Fitness", CategoryMapping.getCategory("Cult Fit"))
    }

    @Test
    fun income_types() {
        assertEquals(
            "Salary",
            CategoryMapping.determineCategory("ACME Salary Credit", "INCOME")
        )
        assertEquals(
            "Refunds",
            CategoryMapping.determineCategory("Amazon Refund", "INCOME")
        )
        assertEquals(
            "Food & Dining",
            CategoryMapping.determineCategory("Zomato", "EXPENSE")
        )
    }

    @Test
    fun unknown_falls_back_to_others() {
        assertEquals("Others", CategoryMapping.getCategory("ZZZ Unknown Vendor XYZ"))
    }
}
