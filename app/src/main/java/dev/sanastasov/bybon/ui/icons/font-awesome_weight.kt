package dev.sanastasov.bybon.ui.icons
/*Font Awesome Free License
-------------------------

Font Awesome Free is free, open source, and GPL friendly. You can use it for
commercial projects, open source projects, or really almost whatever you want.
Full Font Awesome Free license: https://fontawesome.com/license/free.

# Icons: CC BY 4.0 License (https://creativecommons.org/licenses/by/4.0/)
In the Font Awesome Free download, the CC BY 4.0 license applies to all icons
packaged as SVG and JS file types.

# Fonts: SIL OFL 1.1 License (https://scripts.sil.org/OFL)
In the Font Awesome Free download, the SIL OFL license applies to all icons
packaged as web and desktop font files.

# Code: MIT License (https://opensource.org/licenses/MIT)
In the Font Awesome Free download, the MIT license applies to all non-font and
non-icon files.

# Attribution
Attribution is required by MIT, SIL OFL, and CC BY licenses. Downloaded Font
Awesome Free files already contain embedded comments with sufficient
attribution, so you shouldn't need to do anything additional when using these
files normally.

We've kept attribution comments terse, so we ask that you do not actively work
to remove them from files, especially code. They're a great way for folks to
learn about Font Awesome.

# Brand Icons
All brand icons are trademarks of their respective owners. The use of these
trademarks does not indicate endorsement of the trademark holder by Font
Awesome, nor vice versa. **Please do not use brand logos for any purpose except
to represent the company, product, or service to which they refer.**
*/
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val FontAwesomeWeight: ImageVector
    get() {
        if (_FontAwesomeWeight != null) return _FontAwesomeWeight!!
        
        _FontAwesomeWeight = ImageVector.Builder(
            name = "weight",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 512f,
            viewportHeight = 512f
        ).apply {
            path(
                fill = SolidColor(Color.Black)
            ) {
                moveTo(448f, 64f)
                horizontalLineToRelative(-25.98f)
                curveTo(438.44f, 92.28f, 448f, 125.01f, 448f, 160f)
                curveToRelative(0f, 105.87f, -86.13f, 192f, -192f, 192f)
                reflectiveCurveTo(64f, 265.87f, 64f, 160f)
                curveToRelative(0f, -34.99f, 9.56f, -67.72f, 25.98f, -96f)
                horizontalLineTo(64f)
                curveTo(28.71f, 64f, 0f, 92.71f, 0f, 128f)
                verticalLineToRelative(320f)
                curveToRelative(0f, 35.29f, 28.71f, 64f, 64f, 64f)
                horizontalLineToRelative(384f)
                curveToRelative(35.29f, 0f, 64f, -28.71f, 64f, -64f)
                verticalLineTo(128f)
                curveToRelative(0f, -35.29f, -28.71f, -64f, -64f, -64f)
                close()
                moveTo(256f, 320f)
                curveToRelative(88.37f, 0f, 160f, -71.63f, 160f, -160f)
                reflectiveCurveTo(344.37f, 0f, 256f, 0f)
                reflectiveCurveTo(96f, 71.63f, 96f, 160f)
                reflectiveCurveToRelative(71.63f, 160f, 160f, 160f)
                close()
                moveToRelative(-0.3f, -151.94f)
                lineToRelative(33.58f, -78.36f)
                curveToRelative(3.5f, -8.17f, 12.94f, -11.92f, 21.03f, -8.41f)
                curveToRelative(8.12f, 3.48f, 11.88f, 12.89f, 8.41f, 21f)
                lineToRelative(-33.67f, 78.55f)
                curveTo(291.73f, 188f, 296f, 197.45f, 296f, 208f)
                curveToRelative(0f, 22.09f, -17.91f, 40f, -40f, 40f)
                reflectiveCurveToRelative(-40f, -17.91f, -40f, -40f)
                curveToRelative(0f, -21.98f, 17.76f, -39.77f, 39.7f, -39.94f)
                close()
            }
        }.build()
        
        return _FontAwesomeWeight!!
    }

private var _FontAwesomeWeight: ImageVector? = null