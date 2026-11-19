package com.ehealthwares.prognocare.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Base64

class JwtClaimsTest {

    private fun token(payloadJson: String): String {
        val encoder = Base64.getUrlEncoder().withoutPadding()
        val header = encoder.encodeToString("""{"alg":"none"}""".toByteArray())
        val payload = encoder.encodeToString(payloadJson.toByteArray())
        return "$header.$payload.signature"
    }

    @Test
    fun `decodes organisation, location and roles`() {
        val claims = JwtClaims.decode(
            token(
                """{"sub":"u1","organizationId":"org-1","locationId":"loc-1","roles":["doctor","nurse"]}"""
            )
        )

        assertEquals("org-1", claims?.organizationId)
        assertEquals("loc-1", claims?.locationId)
        assertEquals(listOf("doctor", "nurse"), claims?.roles)
    }

    @Test
    fun `normalises a blank organisation for super admins`() {
        val claims = JwtClaims.decode(
            token("""{"sub":"u1","organizationId":"","locationId":null,"roles":["super_admin"]}""")
        )

        assertNull(claims?.organizationId)
        assertNull(claims?.locationId)
        assertEquals(listOf("super_admin"), claims?.roles)
    }

    @Test
    fun `returns null for a malformed token`() {
        assertNull(JwtClaims.decode(null))
        assertNull(JwtClaims.decode(""))
        assertNull(JwtClaims.decode("not-a-jwt"))
        assertNull(JwtClaims.decode("a.b.c"))
    }
}
