package dev.sanastasov.bybon.workout.ui.plans

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.sanastasov.bybon.workout.domain.WorkoutPlan
import dev.sanastasov.bybon.workout.domain.fullBodyA
import dev.sanastasov.bybon.workout.domain.fullBodyB
import dev.sanastasov.bybon.workout.domain.upperBodyA

@Composable
fun WorkoutsTab(
    state: WorkoutPlansUiState,
    onAction: (WorkoutPlansAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier.padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (state.showMyPlansHeading) {
            item(key = "heading-my-plans") {
                SectionHeading("My Plans")
            }
        }
        items(state.activePlans, key = { it.plan.id.id }) { planUi ->
            WorkoutPlanCard(
                planUi = planUi,
                onAction = onAction,
            )
        }
        if (state.showArchivedPlansHeading) {
            item(key = "heading-archived-plans") {
                SectionHeading("Archived Plans")
            }
            items(state.archivedPlans, key = { it.plan.id.id }) { planUi ->
                WorkoutPlanCard(
                    planUi = planUi,
                    onAction = onAction,
                )
            }
        }
        if (state.showArchivedPlansButton) {
            item(key = "show-archived") {
                ArchiveFilterButton("Show Archived Plans") {
                    onAction(WorkoutPlansAction.OnShowArchivedPlans)
                }
            }
        }
        if (state.hideArchivedPlansButton) {
            item(key = "hide-archived") {
                ArchiveFilterButton("Hide Archived Plans") {
                    onAction(WorkoutPlansAction.OnHideArchivedPlans)
                }
            }
        }
    }
}

@Composable
private fun SectionHeading(title: String) {
    Text(
        title,
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.titleMedium,
    )
}

@Composable
private fun ArchiveFilterButton(label: String, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        TextButton(onClick) {
            Text(label)
        }
    }
}

@Composable
private fun WorkoutPlanCard(planUi: WorkoutPlanUi, onAction: (WorkoutPlansAction) -> Unit) {
    val plan = planUi.plan
    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            Arrangement.spacedBy(4.dp),
        ) {
            WorkoutPlanInfo(
                plan = plan,
                isActive = planUi.isActive,
                headerTrailing = {
                    if (!plan.isArchived) {
                        PlanOverflowMenu(
                            plan,
                            onEditClicked = { onAction(WorkoutPlansAction.OnEditPlan(it)) },
                            onArchiveClicked = { onAction(WorkoutPlansAction.OnArchivePlan(it)) },
                        )
                    }
                },
            )
            if (plan.isArchived) {
                TextButton(
                    { onAction(WorkoutPlansAction.OnUnarchivePlan(plan)) },
                    Modifier.align(Alignment.CenterHorizontally),
                ) {
                    Text("Unarchive")
                }
            } else {
                TextButton(
                    { onAction(WorkoutPlansAction.OnStartPlan(plan)) },
                    Modifier.align(Alignment.CenterHorizontally),
                ) {
                    Text(if (planUi.isActive) "Open Workout" else "Start Workout")
                }
            }
        }
    }
}

@Composable
private fun PlanOverflowMenu(
    plan: WorkoutPlan,
    onEditClicked: (WorkoutPlan) -> Unit,
    onArchiveClicked: (WorkoutPlan) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton({ expanded = true }) {
            Icon(
                Icons.Filled.MoreVert,
                contentDescription = "More options for ${plan.name}",
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = { Text("Edit Plan") },
                onClick = {
                    expanded = false
                    onEditClicked(plan)
                },
            )
            DropdownMenuItem(
                text = { Text("Archive Plan") },
                onClick = {
                    expanded = false
                    onArchiveClicked(plan)
                },
            )
        }
    }
}

@Composable
internal fun WorkoutPlanInfo(
    plan: WorkoutPlan,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    headerTrailing: @Composable RowScope.() -> Unit = {},
) {
    Column(modifier, Arrangement.spacedBy(4.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                plan.name,
                Modifier.weight(1f),
                fontWeight = FontWeight.Bold,
            )
            when {
                plan.isArchived -> PlanBadge("Archived")
                isActive -> PlanBadge("Active")
            }
            headerTrailing()
        }
        Spacer(Modifier.height(4.dp))

        plan.description?.let {
            Text(it, fontSize = 14.sp)
        }

        plan.sets.forEach { planedSet ->
            val summary =
                "${planedSet.sets} x ${planedSet.exercise.name} - " +
                    "${planedSet.repRange.first} to ${planedSet.repRange.last} reps"
            Text(summary)
        }
    }
}

@Composable
private fun PlanBadge(label: String) {
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

@Preview
@Composable
private fun WorkoutPlansContentPreview() {
    Surface {
        WorkoutsTab(
            WorkoutPlansUiState(
                plans = listOf(
                    WorkoutPlanUi(fullBodyA, isActive = true),
                    WorkoutPlanUi(fullBodyB, isActive = false),
                ),
            ),
            {},
        )
    }
}

@Preview
@Composable
private fun WorkoutPlansWithArchivedPreview() {
    Surface {
        WorkoutsTab(
            WorkoutPlansUiState(
                plans = listOf(
                    WorkoutPlanUi(fullBodyA, isActive = false),
                    WorkoutPlanUi(fullBodyB, isActive = false),
                    WorkoutPlanUi(upperBodyA, isActive = false),
                ),
                showArchived = true,
                hasArchivedPlans = true,
            ),
            {},
        )
    }
}
