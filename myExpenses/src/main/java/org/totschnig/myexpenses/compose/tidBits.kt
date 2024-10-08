package org.totschnig.myexpenses.compose

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MultiChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import app.futured.donut.compose.DonutProgress
import app.futured.donut.compose.data.DonutModel
import app.futured.donut.compose.data.DonutSection
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.db2.FLAG_EXPENSE
import org.totschnig.myexpenses.db2.FLAG_INCOME
import org.totschnig.myexpenses.util.ui.calcProgressVisualRepresentation
import org.totschnig.myexpenses.util.ui.forCompose
import kotlin.experimental.and
import kotlin.experimental.inv
import kotlin.experimental.or

@Composable
fun ExpansionHandle(
    modifier: Modifier = Modifier,
    isExpanded: Boolean,
    toggle: () -> Unit
) {
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 0F else 180F
    )
    IconButton(modifier = modifier, onClick = toggle) {
        Icon(
            modifier = Modifier.rotate(rotationAngle),
            imageVector = Icons.Default.ExpandLess,
            contentDescription = stringResource(
                id = if (isExpanded) R.string.collapse
                else R.string.expand
            )
        )
    }
}

@Composable
fun ColorCircle(modifier: Modifier = Modifier, color: Int, content: @Composable BoxScope.() -> Unit = {}) {
    ColorCircle(modifier , Color(color), content)
}

@Composable
fun ColorCircle(modifier: Modifier = Modifier, color: Color, content: @Composable BoxScope.() -> Unit = {}) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center,
        content = content
    )
}

val generalPadding
    @Composable get() = dimensionResource(id = R.dimen.general_padding)

@Composable
fun DonutInABox(
    modifier: Modifier,
    progress: Int,
    fontSize: TextUnit,
    color: Color,
    excessColor: Color
) {

    Box(modifier = modifier) {
        DonutProgress(
            modifier = Modifier.fillMaxSize(),
            model = DonutModel(
                cap = 100f,
                gapWidthDegrees = 0f,
                gapAngleDegrees = 0f,
                strokeWidth = LocalContext.current.resources.getDimension(R.dimen.progress_donut_stroke_width),
                sections = calcProgressVisualRepresentation(progress).forCompose(color, excessColor)
            )
        )
        Text(
            modifier = Modifier.align(Alignment.Center),
            text = "%d".format(progress),
            fontSize = fontSize,
            )
    }
}

@Composable
fun TypeConfiguration(
    modifier: Modifier,
    typeFlags: Byte,
    onCheckedChange: (Byte) -> Unit
) {
    MultiChoiceSegmentedButtonRow(
        modifier = modifier
    ) {
        val options = listOf(R.string.expense to FLAG_EXPENSE, R.string.income to FLAG_INCOME)
        options.forEachIndexed { index, type ->
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                onCheckedChange = {
                    onCheckedChange(if(it) typeFlags or type.second else typeFlags and type.second.inv())
                },
                checked = (typeFlags and type.second) != 0u.toByte()
            ) {
                Text(stringResource(id = type.first))
            }
        }
    }
}

@Composable
fun emToDp(em: Float): Dp = with(LocalDensity.current) {
    (LocalTextStyle.current.fontSize.takeIf { it.isSp } ?: 12.sp).toDp()
} * em