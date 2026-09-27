package com.example.murthy

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Paint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.roundToInt

data class LauncherApp(
    val name: String,
    val packageName: String,
    val icon: android.graphics.drawable.Drawable
)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AlphabetLauncherApp()
        }
    }
}

@Composable
fun AlphabetLauncherApp() {

    val context = LocalContext.current

    var installedApps by remember {
        mutableStateOf<List<LauncherApp>>(emptyList())
    }

    LaunchedEffect(Unit) {
        installedApps = loadInstalledApps(context)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        InteractiveAlphabetBar(
            apps = installedApps
        )
    }
}

fun loadInstalledApps(
    context: Context
): List<LauncherApp> {

    val packageManager = context.packageManager

    val intent = Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_LAUNCHER)
    }

    val resolveInfos =
        packageManager.queryIntentActivities(
            intent,
            PackageManager.MATCH_ALL
        )

    return resolveInfos
        .mapNotNull { resolveInfo ->

            val appName =
                resolveInfo.loadLabel(packageManager)
                    ?.toString()
                    ?.trim()

            val packageName =
                resolveInfo.activityInfo
                    ?.packageName
                    ?.trim()

            if (
                appName.isNullOrEmpty() ||
                packageName.isNullOrEmpty()
            ) {
                null
            } else {

                LauncherApp(
                    name = appName,
                    packageName = packageName,
                    icon = resolveInfo.loadIcon(packageManager)
                )
            }
        }
        .distinctBy {
            it.packageName
        }
        .sortedBy {
            it.name.lowercase(Locale.getDefault())
        }
}

@Composable
fun InteractiveAlphabetBar(
    apps: List<LauncherApp>
) {

    val context = LocalContext.current
    val letters = ('A'..'Z').toList()
    val scope = rememberCoroutineScope()

    var fingerX by remember {
        mutableFloatStateOf(0f)
    }

    var fingerY by remember {
        mutableFloatStateOf(0f)
    }

    var releaseY by remember {
        mutableFloatStateOf(0f)
    }

    var isDragging by remember {
        mutableStateOf(false)
    }

    var selectedIndex by remember {
        mutableStateOf<Int?>(null)
    }

    val releaseOffset = remember {
        Animatable(0f)
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {

        // ---------------------------------------------------------
        // HOME SCREEN
        // ---------------------------------------------------------

        if (!isDragging) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = 24.dp,

                        // Moved clock/date slightly lower
                        top = 55.dp,

                        end = 70.dp,
                        bottom = 40.dp
                    )
            ) {

                ClockAndDate()

                SpacerHeight(18)

                FavoriteApps(
                    apps = apps.take(6),
                    onAppClick = { app ->

                        launchApp(
                            context = context,
                            packageName = app.packageName
                        )
                    }
                )
            }
        }

        // ---------------------------------------------------------
        // FILTERED APP LIST
        // ---------------------------------------------------------

        if (
            isDragging &&
            selectedIndex != null
        ) {

            val selectedLetter =
                letters[selectedIndex!!]

            val selectedApps =
                apps.filter { app ->

                    app.name
                        .trim()
                        .firstOrNull()
                        ?.uppercaseChar() == selectedLetter
                }

            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth()
                    .padding(
                        start = 24.dp,
                        top = 150.dp,
                        end = 75.dp,
                        bottom = 50.dp
                    )
            ) {

                Text(
                    text = selectedLetter.toString(),
                    color = Color.White,
                    fontSize = 32.sp
                )

                if (selectedApps.isEmpty()) {

                    Text(
                        text = "No apps",
                        color = Color.Gray,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(
                            top = 16.dp
                        )
                    )

                } else {

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),

                        verticalArrangement =
                            Arrangement.spacedBy(2.dp)
                    ) {

                        items(
                            items = selectedApps,
                            key = {
                                it.packageName
                            }
                        ) { app ->

                            AppRow(
                                app = app,
                                onClick = {

                                    launchApp(
                                        context = context,
                                        packageName =
                                            app.packageName
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }

        // ---------------------------------------------------------
        // ALPHABET BAR + GESTURE
        // ---------------------------------------------------------

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {

                    // Increased from 10.dp to 12.dp
                    // so the alphabet line is slightly longer.
                    val letterSpacing =
                        12.dp.toPx()

                    detectDragGestures(

                        onDragStart = { offset ->

                            scope.launch {
                                releaseOffset.stop()
                            }

                            isDragging = true

                            fingerX = offset.x
                            fingerY = offset.y
                            releaseY = offset.y

                            selectedIndex =
                                calculateSelectedLetter(
                                    y = offset.y,
                                    height = size.height,
                                    letterCount =
                                        letters.size,
                                    letterSpacing =
                                        letterSpacing
                                )
                        },

                        onDrag = { change, _ ->

                            fingerX =
                                change.position.x

                            fingerY =
                                change.position.y

                            releaseY =
                                change.position.y

                            selectedIndex =
                                calculateSelectedLetter(
                                    y =
                                        change.position.y,
                                    height =
                                        size.height,
                                    letterCount =
                                        letters.size,
                                    letterSpacing =
                                        letterSpacing
                                )
                        },

                        onDragEnd = {

                            isDragging = false
                            releaseY = fingerY

                            scope.launch {

                                val baseX =
                                    size.width - 30f

                                val currentPull =
                                    fingerX - baseX

                                releaseOffset.snapTo(
                                    currentPull
                                )

                                releaseOffset.animateTo(
                                    targetValue = 0f,
                                    animationSpec =
                                        spring(
                                            dampingRatio =
                                                Spring.DampingRatioNoBouncy,
                                            stiffness =
                                                Spring.StiffnessMediumLow
                                        )
                                )

                                selectedIndex = null
                            }
                        },

                        onDragCancel = {

                            isDragging = false
                            releaseY = fingerY

                            scope.launch {

                                val baseX =
                                    size.width - 30f

                                val currentPull =
                                    fingerX - baseX

                                releaseOffset.snapTo(
                                    currentPull
                                )

                                releaseOffset.animateTo(
                                    targetValue = 0f,
                                    animationSpec =
                                        spring(
                                            dampingRatio =
                                                Spring.DampingRatioNoBouncy,
                                            stiffness =
                                                Spring.StiffnessMediumLow
                                        )
                                )

                                selectedIndex = null
                            }
                        }
                    )
                }
        ) {

            val canvasWidth = size.width
            val canvasHeight = size.height

            val baseX =
                canvasWidth - 30f

            // Same increased spacing here.
            val letterSpacing =
                12.dp.toPx()

            val alphabetHeight =
                letterSpacing *
                        (letters.size - 1)

            val firstLetterY =
                (canvasHeight - alphabetHeight) / 2f

            val activeFingerY =
                if (isDragging) {
                    fingerY
                } else {
                    releaseY
                }

            val horizontalPull =
                if (isDragging) {
                    fingerX - baseX
                } else {
                    releaseOffset.value
                }

            val sigma = 72f

            // -----------------------------------------------------
            // STAR
            // -----------------------------------------------------

            val starY =
                firstLetterY -
                        letterSpacing * 1.25f

            val starDistance =
                starY - activeFingerY

            val starInfluence =
                gaussianInfluence(
                    distance =
                        abs(starDistance),
                    sigma = sigma
                )

            val starCurve =
                sCurveInfluence(
                    influence =
                        starInfluence,
                    signedDistance =
                        starDistance
                )

            val starX =
                baseX +
                        horizontalPull *
                        starCurve

            val starPaint =
                Paint().apply {

                    isAntiAlias = true
                    textAlign =
                        Paint.Align.CENTER

                    color =
                        android.graphics.Color.WHITE

                    textSize =
                        20.sp.toPx()
                }

            drawContext.canvas.nativeCanvas.drawText(
                "☆",
                starX,
                starY + 7f,
                starPaint
            )

            // -----------------------------------------------------
            // BOTTOM DOT
            // -----------------------------------------------------

            val dotY =
                firstLetterY +
                        letterSpacing *
                        (letters.size - 1) +
                        letterSpacing * 1.25f

            val dotDistance =
                dotY - activeFingerY

            val dotInfluence =
                gaussianInfluence(
                    distance =
                        abs(dotDistance),
                    sigma = sigma
                )

            val dotCurve =
                sCurveInfluence(
                    influence =
                        dotInfluence,
                    signedDistance =
                        dotDistance
                )

            val dotX =
                baseX +
                        horizontalPull *
                        dotCurve

            drawCircle(
                color = Color.LightGray,
                radius = 3.5f,
                center = Offset(
                    dotX,
                    dotY
                )
            )

            // -----------------------------------------------------
            // LETTERS
            // -----------------------------------------------------

            letters.forEachIndexed { index, letter ->

                val baseY =
                    firstLetterY +
                            index *
                            letterSpacing

                val signedDistance =
                    baseY -
                            activeFingerY

                val distance =
                    abs(signedDistance)

                val influence =
                    gaussianInfluence(
                        distance = distance,
                        sigma = sigma
                    )

                val curve =
                    sCurveInfluence(
                        influence = influence,
                        signedDistance =
                            signedDistance
                    )

                val letterX =
                    baseX +
                            horizontalPull *
                            curve

                val isSelected =
                    isDragging &&
                            selectedIndex == index

                if (isSelected) {

                    drawCircle(
                        color =
                            Color(0xFF303030),

                        radius = 22f,

                        center =
                            Offset(
                                letterX,
                                baseY
                            )
                    )
                }

                val letterPaint =
                    Paint().apply {

                        isAntiAlias = true
                        textAlign =
                            Paint.Align.CENTER

                        color =
                            android.graphics.Color.WHITE

                        textSize =
                            if (isSelected) {
                                17.sp.toPx()
                            } else {
                                12.sp.toPx()
                            }
                    }

                drawContext.canvas.nativeCanvas.drawText(
                    letter.toString(),
                    letterX,
                    baseY + 5f,
                    letterPaint
                )
            }
        }
    }
}

// -------------------------------------------------------------
// CLOCK + DATE
// -------------------------------------------------------------

@Composable
fun ClockAndDate() {

    var currentTime by remember {
        mutableStateOf(Date())
    }

    LaunchedEffect(Unit) {

        while (true) {

            currentTime = Date()

            delay(1000)
        }
    }

    val time =
        SimpleDateFormat(
            "HH:mm",
            Locale.getDefault()
        ).format(currentTime)

    val date =
        SimpleDateFormat(
            "EEE, dd MMM",
            Locale.getDefault()
        ).format(currentTime)

    Column {

        Text(
            text = time,
            color = Color.White,
            fontSize = 64.sp
        )

        Text(
            text = date,
            color = Color.LightGray,
            fontSize = 18.sp
        )
    }
}

// -------------------------------------------------------------
// FAVOURITE APPS
// -------------------------------------------------------------

@Composable
fun FavoriteApps(
    apps: List<LauncherApp>,
    onAppClick: (LauncherApp) -> Unit
) {

    Column(
        verticalArrangement =
            Arrangement.spacedBy(2.dp)
    ) {

        apps.forEach { app ->

            AppRow(
                app = app,
                onClick = {
                    onAppClick(app)
                }
            )
        }
    }
}

// -------------------------------------------------------------
// APP ROW
// -------------------------------------------------------------

@Composable
fun AppRow(
    app: LauncherApp,
    onClick: () -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClick = onClick
            )
            .padding(
                vertical = 7.dp,
                horizontal = 4.dp
            ),

        verticalAlignment =
            Alignment.CenterVertically
    ) {

        AndroidView(
            factory = { context ->

                android.widget.ImageView(
                    context
                ).apply {

                    setImageDrawable(
                        app.icon
                    )

                    scaleType =
                        android.widget.ImageView
                            .ScaleType
                            .CENTER_INSIDE
                }
            },

            modifier =
                Modifier.size(38.dp)
        )

        Text(
            text = app.name,
            color = Color.White,
            fontSize = 16.sp,

            modifier =
                Modifier.padding(
                    start = 10.dp
                )
        )
    }
}

// -------------------------------------------------------------
// SPACER
// -------------------------------------------------------------

@Composable
fun SpacerHeight(
    height: Int
) {

    Box(
        modifier = Modifier.size(
            width = 1.dp,
            height = height.dp
        )
    )
}

// -------------------------------------------------------------
// LAUNCH APP
// -------------------------------------------------------------

fun launchApp(
    context: Context,
    packageName: String
) {

    val launchIntent =
        context.packageManager
            .getLaunchIntentForPackage(
                packageName
            )

    if (launchIntent != null) {

        launchIntent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK
        )

        context.startActivity(
            launchIntent
        )
    }
}

// -------------------------------------------------------------
// CURVE MATH
// -------------------------------------------------------------

fun gaussianInfluence(
    distance: Float,
    sigma: Float
): Float {

    return exp(
        -(distance * distance) /
                (2f * sigma * sigma)
    )
}

fun sCurveInfluence(
    influence: Float,
    signedDistance: Float
): Float {

    val normalizedDistance =
        signedDistance / 90f

    val smoothSide =
        1f /
                (
                        1f +
                                normalizedDistance *
                                normalizedDistance
                        )

    val mainCurve =
        influence * 0.82f

    val secondaryCurve =
        influence *
                smoothSide *
                0.18f

    return mainCurve +
            secondaryCurve
}

// -------------------------------------------------------------
// TOUCH POSITION → LETTER
// -------------------------------------------------------------

fun calculateSelectedLetter(
    y: Float,
    height: Int,
    letterCount: Int,
    letterSpacing: Float
): Int {

    val alphabetHeight =
        letterSpacing *
                (letterCount - 1)

    val firstLetterY =
        (height - alphabetHeight) / 2f

    val relativeY =
        y - firstLetterY

    val index =
        (relativeY / letterSpacing)
            .roundToInt()

    return index.coerceIn(
        0,
        letterCount - 1
    )
}