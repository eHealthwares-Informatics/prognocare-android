package com.ehealthwares.prognocare.feature.forms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ehealthwares.prognocare.designsystem.theme.Spacing

/**
 * Compact, touch-friendly rendering of the web `annotate` field.
 *
 * The web renders an interactive SVG body / dermatome / organ map. On mobile we
 * use grouped selectable chips — the same ids are written back to the schema,
 * so submissions stay compatible with the web body map.
 *
 * Value shape (matches the web): body / organ → `List<String>` of ids,
 * dermatome → a single `String` id.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AnnotateField(
    field: FormField,
    value: Any?,
    enabled: Boolean,
    onValueChange: (Any?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val config = field.annotate ?: AnnotateConfig()
    val multiple = config.multiple

    val selectedIds: Set<String> = when (val v = value) {
        is List<*> -> v.mapNotNull { it?.toString() }.toSet()
        is String -> if (v.isBlank()) emptySet() else setOf(v)
        else -> emptySet()
    }

    fun emit(next: Set<String>) {
        if (multiple) {
            onValueChange(next.toList().ifEmpty { null })
        } else {
            onValueChange(next.firstOrNull())
        }
    }

    fun toggle(id: String) {
        if (!enabled) return
        val isOn = id in selectedIds
        emit(
            when {
                multiple && isOn -> selectedIds - id
                multiple -> selectedIds + id
                isOn -> emptySet()
                else -> setOf(id)
            },
        )
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = field.label + if (field.required) " *" else "",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.xs))

        when (config.target) {
            AnnotateTarget.BODY -> {
                val allowed = config.regions.toSet()
                val groups = annotateBodyRegions
                    .filter { allowed.isEmpty() || it.id in allowed }
                    .filter { config.views.isEmpty() || it.view.lowercase() in config.views.map(String::lowercase) }
                    .groupBy { it.view }
                groups.forEach { (view, regions) ->
                    Text(
                        text = view,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    ChipGrid(
                        items = regions.map { it.id to it.label },
                        selected = selectedIds,
                        enabled = enabled,
                        onToggle = ::toggle,
                    )
                    Spacer(Modifier.height(Spacing.xs))
                }
            }

            AnnotateTarget.DERMATOME -> {
                val groups = annotateDermatomes.groupBy { it.region }
                groups.forEach { (region, levels) ->
                    Text(
                        text = region,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    ChipGrid(
                        items = levels.map { it.id to it.id },
                        selected = selectedIds,
                        enabled = enabled,
                        onToggle = ::toggle,
                    )
                    Spacer(Modifier.height(Spacing.xs))
                }
            }

            AnnotateTarget.ORGAN -> {
                config.organ?.let { organ ->
                    Text(
                        text = organ.replace('-', ' ').replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                if (config.structures.isEmpty()) {
                    Text(
                        text = "No structures configured for this field.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    ChipGrid(
                        items = config.structures.map { it to it.prettifyId() },
                        selected = selectedIds,
                        enabled = enabled,
                        onToggle = ::toggle,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipGrid(
    items: List<Pair<String, String>>,
    selected: Set<String>,
    enabled: Boolean,
    onToggle: (String) -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(top = Spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        verticalArrangement = Arrangement.spacedBy(Spacing.xxs),
    ) {
        items.forEach { (id, label) ->
            FilterChip(
                selected = id in selected,
                onClick = { onToggle(id) },
                enabled = enabled,
                label = { Text(label) },
            )
        }
    }
}

internal fun String.prettifyId(): String =
    replace('-', ' ').replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }

/** Body regions (ids/labels mirror `body-map-data.ts`). */
internal data class AnnotateRegion(val id: String, val label: String, val view: String)

private fun bodyPair(
    view: String,
    idTemplate: String,
    labelTemplate: String,
): List<AnnotateRegion> = listOf("right", "left").map { side ->
    val label = labelTemplate.replace("{side}", side)
    AnnotateRegion(
        id = idTemplate.replace("{side}", side),
        label = label.replaceFirstChar { it.uppercase() },
        view = view,
    )
}

internal val annotateBodyRegions: List<AnnotateRegion> = buildList {
    add(AnnotateRegion("head", "Head and face", "Front"))
    add(AnnotateRegion("neck", "Neck", "Front"))
    addAll(bodyPair("Front", "{side}-shoulder", "{side} shoulder"))
    addAll(bodyPair("Front", "{side}-chest", "{side} chest"))
    addAll(bodyPair("Front", "{side}-upper-abdomen", "{side} upper abdomen"))
    addAll(bodyPair("Front", "{side}-lower-abdomen", "{side} lower abdomen"))
    add(AnnotateRegion("groin", "Groin", "Front"))
    addAll(bodyPair("Front", "{side}-upper-arm", "{side} upper arm"))
    addAll(bodyPair("Front", "{side}-forearm", "{side} forearm"))
    addAll(bodyPair("Front", "{side}-palm", "{side} palm"))
    addAll(bodyPair("Front", "{side}-thigh", "front of {side} thigh"))
    addAll(bodyPair("Front", "{side}-knee", "{side} knee"))
    addAll(bodyPair("Front", "{side}-shin", "{side} shin"))
    addAll(bodyPair("Front", "{side}-foot", "top of {side} foot"))

    add(AnnotateRegion("back-of-head", "Back of head", "Back"))
    add(AnnotateRegion("back-of-neck", "Back of neck", "Back"))
    addAll(bodyPair("Back", "{side}-shoulder-back", "back of {side} shoulder"))
    addAll(bodyPair("Back", "{side}-upper-back", "{side} upper back"))
    addAll(bodyPair("Back", "{side}-lower-back", "{side} lower back"))
    addAll(bodyPair("Back", "{side}-buttock", "{side} buttock"))
    addAll(bodyPair("Back", "{side}-upper-arm-back", "back of {side} upper arm"))
    addAll(bodyPair("Back", "{side}-forearm-back", "back of {side} forearm"))
    addAll(bodyPair("Back", "{side}-hand-back", "back of {side} hand"))
    addAll(bodyPair("Back", "{side}-thigh-back", "back of {side} thigh"))
    addAll(bodyPair("Back", "{side}-knee-back", "back of {side} knee"))
    addAll(bodyPair("Back", "{side}-calf", "{side} calf"))
    addAll(bodyPair("Back", "{side}-heel", "{side} heel"))
}

/** ISNCSCI sensory levels (ids mirror `dermatome-data.ts`). */
internal data class AnnotateDermatome(val id: String, val region: String)

internal val annotateDermatomes: List<AnnotateDermatome> = buildList {
    ('2'..'8').forEach { add(AnnotateDermatome("C$it", "Cervical")) }
    (1..12).forEach { add(AnnotateDermatome("T$it", "Thoracic")) }
    (1..5).forEach { add(AnnotateDermatome("L$it", "Lumbar")) }
    add(AnnotateDermatome("S1", "Sacral"))
    add(AnnotateDermatome("S2", "Sacral"))
    add(AnnotateDermatome("S3", "Sacral"))
    add(AnnotateDermatome("S4-5", "Sacral"))
}
