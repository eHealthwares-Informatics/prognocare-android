package com.ehealthwares.prognocare

import com.ehealthwares.prognocare.data.remote.models.MeResponse
import com.ehealthwares.prognocare.data.remote.models.ModuleInfo
import com.ehealthwares.prognocare.feature.auth.UserRoleMapper
import com.ehealthwares.prognocare.navigation.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UserRoleMapperTest {

    private fun me(
        roles: List<String> = emptyList(),
        modules: List<ModuleInfo> = emptyList(),
    ) = MeResponse(id = "u1", username = "alice", roles = roles, modules = modules)

    @Test
    fun `maps admin role code to Admin`() {
        assertEquals(UserRole.Admin, UserRoleMapper.map(me(roles = listOf("admin"))))
    }

    @Test
    fun `maps super_admin role code to Admin`() {
        assertEquals(UserRole.Admin, UserRoleMapper.map(me(roles = listOf("super_admin"))))
    }

    @Test
    fun `maps cashier role code to Finance`() {
        assertEquals(UserRole.Finance, UserRoleMapper.map(me(roles = listOf("cashier"))))
    }

    @Test
    fun `maps lis_technician role code to Technician`() {
        assertEquals(UserRole.Technician, UserRoleMapper.map(me(roles = listOf("lis_technician"))))
    }

    @Test
    fun `maps emr_clinician role code to Doctor`() {
        assertEquals(UserRole.Doctor, UserRoleMapper.map(me(roles = listOf("emr_clinician"))))
    }

    @Test
    fun `maps customer role code to Patient`() {
        assertEquals(UserRole.Patient, UserRoleMapper.map(me(roles = listOf("customer"))))
    }

    @Test
    fun `matches role code case-insensitively`() {
        assertEquals(UserRole.Admin, UserRoleMapper.map(me(roles = listOf("ADMIN"))))
    }

    @Test
    fun `falls back to Doctor when no role code matches`() {
        assertEquals(UserRole.Doctor, UserRoleMapper.map(me(roles = listOf("unknown_role"))))
    }

    @Test
    fun `derives Doctor from emr module when no role code`() {
        val res = UserRoleMapper.map(
            me(modules = listOf(ModuleInfo(id = "emr", name = "EMR"))),
        )
        assertEquals(UserRole.Doctor, res)
    }

    @Test
    fun `derives Technician from lis module when no role code`() {
        val res = UserRoleMapper.map(
            me(modules = listOf(ModuleInfo(id = "lis", name = "LIS"))),
        )
        assertEquals(UserRole.Technician, res)
    }

    @Test
    fun `hasRole is true when a backend code maps to the mobile role`() {
        val res = me(roles = listOf("admin", "cashier"))
        assertTrue(UserRoleMapper.hasRole(res, UserRole.Finance))
    }
}