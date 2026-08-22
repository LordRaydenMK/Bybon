package dev.sanastasov.bybon.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("CheckReturnValue")
val SavedLocally: ImageVector
    get() {
        if (_sync_saved_locally != null) {
            return _sync_saved_locally!!
        }
        _sync_saved_locally =
            ImageVector.Builder(
                name = "sync_saved_locally",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            )
                .apply {
                    path(
                        fill = SolidColor(Color.Black),
                        fillAlpha = 1f,
                        stroke = null,
                        strokeAlpha = 1f,
                        strokeLineWidth = 1f,
                        strokeLineCap = StrokeCap.Butt,
                        strokeLineJoin = StrokeJoin.Bevel,
                        strokeLineMiter = 1f,
                        pathFillType = PathFillType.Companion.NonZero,
                    ) {
                        moveTo(10.93f, 14.05f)
                        lineTo(16.6f, 8.4f)
                        lineTo(15.18f, 6.97f)
                        lineToRelative(-4.25f, 4.25f)
                        lineTo(8.8f, 9.1f)
                        lineTo(7.4f, 10.5f)
                        lineToRelative(3.53f, 3.55f)
                        close()
                        moveTo(1f, 21f)
                        verticalLineTo(19f)
                        horizontalLineTo(23f)
                        verticalLineToRelative(2f)
                        horizontalLineTo(1f)
                        close()
                        moveTo(4f, 18f)
                        quadTo(3.18f, 18f, 2.59f, 17.41f)
                        reflectiveQuadTo(2f, 16f)
                        verticalLineTo(5f)
                        quadTo(2f, 4.17f, 2.59f, 3.59f)
                        reflectiveQuadTo(4f, 3f)
                        horizontalLineTo(20f)
                        quadToRelative(0.83f, 0f, 1.41f, 0.59f)
                        reflectiveQuadTo(22f, 5f)
                        verticalLineTo(16f)
                        quadToRelative(0f, 0.82f, -0.59f, 1.41f)
                        reflectiveQuadTo(20f, 18f)
                        horizontalLineTo(4f)
                        close()
                        moveTo(4f, 16f)
                        horizontalLineTo(20f)
                        verticalLineTo(5f)
                        horizontalLineTo(4f)
                        verticalLineTo(16f)
                        close()
                        moveToRelative(0f, 0f)
                        verticalLineTo(5f)
                        verticalLineTo(16f)
                        close()
                    }
                }
                .build()
        return _sync_saved_locally!!
    }

private var _sync_saved_locally: ImageVector? = null
