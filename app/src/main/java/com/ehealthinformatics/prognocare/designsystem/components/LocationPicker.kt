package com.ehealthinformatics.prognocare.designsystem.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ehealthinformatics.prognocare.data.location.LocationScope
import com.ehealthinformatics.prognocare.designsystem.theme.Spacing

/**
 * Location-scope selector ("location based query"). Shows the active scope —
 * the user's staff location by default, or what they last picked — and lets
 * them switch to any other facility location or back to "All locations".
 *
 * The chosen scope is persisted in [LocationScope] (via [SessionStore]) so
 * every list screen uses it until changed.
 */
@Composable
fun LocationScopeChip(
    locationScope: LocationScope,
    onScopeChanged: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    var locations by remember { mutableStateOf(listOf<com.ehealthinformatics.prognocare.data.remote.models.Location>()) }
    var scope by remember { mutableStateOf(locationScope.current()) }

    LaunchedEffect(Unit) {
        // Refresh the label from the active location record, then cache the
        // full location list for the menu.
        val current = locationScope.current()
        if (!current.isAll && current.name == null) {
            current.id?.let { id ->
                locationScope.location(id)?.let { resolved ->
                    locationScope.setActive(id, resolved.name)
                }
            }
        }
        scope = locationScope.current()
        locations = locationScope.locations()
    }

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        FilterChip(
            selected = !scope.isAll,
            onClick = { expanded = true },
            label = {
                Text(
                    text = scope.name ?: "All locations",
                    fontWeight = FontWeight.Medium,
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("All locations") },
                leadingIcon = {
                    if (scope.isAll) {
                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                },
                onClick = {
                    locationScope.clearActive()
                    scope = locationScope.current()
                    expanded = false
                    onScopeChanged()
                },
            )
            locations.forEach { location ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = location.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (location.id == scope.id) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    },
                    leadingIcon = {
                        if (location.id == scope.id) {
                            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    },
                    onClick = {
                        locationScope.setActive(location.id, location.name)
                        scope = locationScope.current()
                        expanded = false
                        onScopeChanged()
                    },
                )
            }
        }
    }
}
