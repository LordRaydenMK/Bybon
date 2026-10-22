@file:Suppress("TooManyFunctions")

package dev.sanastasov.bybon.workout.ui.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.sanastasov.bybon.workout.domain.ExerciseImages
import dev.sanastasov.bybon.workout.domain.Weight
import dev.sanastasov.bybon.workout.ui.formatSetPerformance
import okhttp3.HttpUrl

private val ImageHeight = 200.dp
private val ImageShape = RoundedCornerShape(12.dp)

internal fun LazyListScope.detailsImages(images: ExerciseImages?) {
    if (images == null) return
    item(key = "images") {
        when (images) {
            is ExerciseImages.StartAndPeak -> StartAndPeakImages(images.start, images.peak)
            is ExerciseImages.Main -> ExerciseImage(images.main, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun StartAndPeakImages(start: HttpUrl, peak: HttpUrl) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CaptionedImage("Start", start, Modifier.weight(1f))
        CaptionedImage("Peak", peak, Modifier.weight(1f))
    }
}

@Composable
private fun CaptionedImage(caption: String, url: HttpUrl, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ExerciseImage(url, Modifier.fillMaxWidth())
        Text(
            caption,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ExerciseImage(url: HttpUrl, modifier: Modifier = Modifier) {
    AsyncImage(
        url.toString(),
        contentDescription = null,
        modifier
            .height(ImageHeight)
            .clip(ImageShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentScale = ContentScale.Crop,
    )
}

@OptIn(ExperimentalLayoutApi::class)
internal fun LazyListScope.detailsChips(key: String, labels: List<String>) {
    if (labels.isEmpty()) return
    item(key = key) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            labels.forEach { label ->
                FactChip(label)
            }
        }
    }
}

@Composable
private fun FactChip(label: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
    ) {
        Text(
            label,
            Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

internal fun LazyListScope.detailsMuscleGroup(title: String, muscles: List<String>) {
    if (muscles.isEmpty()) return
    item(key = title) {
        DetailsSection(title) {
            Text(muscles.joinToString(", "), style = MaterialTheme.typography.bodyLarge)
        }
    }
}

internal fun LazyListScope.detailsNumberedList(title: String, items: List<String>) {
    if (items.isEmpty()) return
    item(key = title) {
        DetailsSection(title) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items.forEachIndexed { index, step ->
                    Text("${index + 1}. $step", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

internal fun LazyListScope.detailsBulletedList(title: String, items: List<String>) {
    if (items.isEmpty()) return
    item(key = title) {
        DetailsSection(title) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items.forEach { tip ->
                    Text("• $tip", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

internal fun LazyListScope.detailsPersonalRecord(personalRecordOneRm: Weight?) {
    if (personalRecordOneRm == null) return
    item(key = "personal-record") {
        DetailsSection("Personal record") {
            Text(
                "${personalRecordOneRm.kilograms} kg 1RM",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

internal fun LazyListScope.detailsOneRmTrend(points: List<ExerciseOneRmPointUi>) {
    if (points.isEmpty()) return
    item(key = "one-rm-trend") {
        ExerciseOneRmTrendCard(points)
    }
}

internal fun LazyListScope.detailsRecentSessions(sessions: List<ExerciseDetailsSessionUi>) {
    item(key = "recent-history") {
        DetailsSection("Last 3 months") {
            if (sessions.isEmpty()) {
                Text(
                    "No working sets in the last 3 months.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    sessions.forEach { session ->
                        RecentSessionBlock(session)
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentSessionBlock(session: ExerciseDetailsSessionUi) {
    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(session.planName, style = MaterialTheme.typography.titleSmall)
        Text(
            session.dateLabel,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        session.sets.forEach { set ->
            Text(
                "${set.number}. ${formatSetPerformance(set.weightKg, set.reps, set.oneRm)}",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
internal fun DetailsSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium,
        )
        content()
    }
}
