package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FileCategory
import com.example.model.StorageStats
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.LavenderCategory
import com.example.ui.theme.MintCategory
import com.example.ui.theme.PeachCategory
import com.example.ui.theme.PurpleGlow
import com.example.ui.theme.SkyBlueCategory
import com.example.ui.theme.VioletGlow

@Composable
fun FrostedGlassBox(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(24.dp),
    backgroundColor: Color? = null,
    borderColor: Color? = null,
    content: @Composable () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val bg = backgroundColor ?: if (isDark) Color(0xCC161D27) else Color(0xF2FFFFFF)
    val border = borderColor ?: if (isDark) Color(0x33FFFFFF) else Color(0x80FFFFFF)

    Box(
        modifier = modifier
            .shadow(
                elevation = if (isDark) 8.dp else 4.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.2f),
                spotColor = Color.Black.copy(alpha = 0.3f)
            )
            .clip(shape)
            .background(bg)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        border.copy(alpha = 0.45f),
                        border.copy(alpha = 0.15f)
                    )
                ),
                shape = shape
            )
    ) {
        content()
    }
}

@Composable
fun FrostedSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearchClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    hintText: String = "Search files, folders…"
) {
    val isDark = isSystemInDarkTheme()
    val pillBg = if (isDark) Color(0x4D242E3D) else Color(0x66FFFFFF)
    val pillBorder = if (isDark) Color(0x40FFFFFF) else Color(0x99FFFFFF)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .shadow(elevation = 4.dp, shape = RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(pillBg)
                .border(
                    width = 1.dp,
                    brush = Brush.horizontalGradient(
                        colors = listOf(pillBorder, pillBorder.copy(alpha = 0.2f), pillBorder)
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .clickable { onSearchClick() }
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = query.ifBlank { hintText },
                    color = if (query.isBlank()) {
                        if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    } else {
                        if (isDark) Color.White else Color(0xFF0F172A)
                    },
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        IconButton(
            onClick = onNotificationClick,
            modifier = Modifier
                .size(48.dp)
                .testTag("notification_button")
        ) {
            Icon(
                imageVector = Icons.Default.NotificationsNone,
                contentDescription = "Notifications",
                tint = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun StorageGaugeCard(
    stats: StorageStats,
    onAnalyzeClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val animatedPercent by animateFloatAsState(
        targetValue = stats.usedPercentage.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 800),
        label = "gauge_percent"
    )

    FrostedGlassBox(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onAnalyzeClick() }
            .testTag("storage_card"),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "Storage Used",
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular Ring Gauge
                Box(
                    modifier = Modifier.size(86.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 8.dp.toPx()
                        val ringColor = if (isDark) Color(0xFF232D3F) else Color(0xFFE2E8F0)

                        // Background Track
                        drawArc(
                            color = ringColor,
                            startAngle = 135f,
                            sweepAngle = 270f,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )

                        // Progress Gradient Arc
                        val gradientBrush = Brush.sweepGradient(
                            listOf(CyanGlow, PurpleGlow, VioletGlow)
                        )
                        drawArc(
                            brush = gradientBrush,
                            startAngle = 135f,
                            sweepAngle = 270f * animatedPercent,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stats.formattedUsed,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color(0xFF0F172A)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(20.dp))

                // Stats Details & Horizontal Bar
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Text(
                            text = stats.formattedUsed,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color(0xFF0F172A)
                        )
                        Text(
                            text = " / ${stats.formattedTotal}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                            modifier = Modifier.padding(bottom = 1.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Horizontal Progress Bar with Gradient
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (isDark) Color(0xFF232D3F) else Color(0xFFE2E8F0))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(animatedPercent)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(CyanGlow, PurpleGlow)
                                    )
                                )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Free space: ${stats.formattedFree}",
                        fontSize = 12.sp,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )
                }
            }
        }
    }
}

@Composable
fun QuickAccessGrid(
    onCategoryClick: (FileCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            CategoryButton(
                title = "Images",
                color = PeachCategory,
                onClick = { onCategoryClick(FileCategory.IMAGE) },
                modifier = Modifier.weight(1f),
                testTag = "category_images"
            )
            CategoryButton(
                title = "Videos",
                color = LavenderCategory,
                onClick = { onCategoryClick(FileCategory.VIDEO) },
                modifier = Modifier.weight(1f),
                testTag = "category_videos"
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            CategoryButton(
                title = "Documents",
                color = SkyBlueCategory,
                onClick = { onCategoryClick(FileCategory.DOCUMENT) },
                modifier = Modifier.weight(1f),
                testTag = "category_documents"
            )
            CategoryButton(
                title = "Music",
                color = MintCategory,
                onClick = { onCategoryClick(FileCategory.AUDIO) },
                modifier = Modifier.weight(1f),
                testTag = "category_music"
            )
        }
    }
}

@Composable
private fun CategoryButton(
    title: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String
) {
    Box(
        modifier = modifier
            .height(56.dp)
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(18.dp),
                spotColor = color.copy(alpha = 0.5f)
            )
            .clip(RoundedCornerShape(18.dp))
            .background(color)
            .clickable(onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF1E293B)
        )
    }
}

@Composable
fun FloatingBottomBar(
    currentTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val barBg = if (isDark) Color(0xCC161D27) else Color(0xEBFFFFFF)
    val border = if (isDark) Color(0x33FFFFFF) else Color(0x99FFFFFF)

    Box(
        modifier = modifier
            .padding(horizontal = 48.dp, vertical = 16.dp)
            .height(60.dp)
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(30.dp),
                spotColor = Color.Black.copy(alpha = 0.35f)
            )
            .clip(RoundedCornerShape(30.dp))
            .background(barBg)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(border, border.copy(alpha = 0.2f))
                ),
                shape = RoundedCornerShape(30.dp)
            )
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Home Tab
            NavTabItem(
                icon = Icons.Default.Home,
                label = "Home",
                isSelected = currentTab == 0,
                onClick = { onTabSelected(0) },
                testTag = "nav_home"
            )

            // Storage / Browser Tab
            NavTabItem(
                icon = Icons.Default.GridView,
                label = "Files",
                isSelected = currentTab == 1,
                onClick = { onTabSelected(1) },
                testTag = "nav_browser"
            )

            // Settings Tab
            NavTabItem(
                icon = Icons.Default.Settings,
                label = "Settings",
                isSelected = currentTab == 2,
                onClick = { onTabSelected(2) },
                testTag = "nav_settings"
            )
        }
    }
}

@Composable
private fun NavTabItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val isDark = isSystemInDarkTheme()
    val activeTint = if (isDark) Color.White else Color(0xFF0F172A)
    val inactiveTint = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color(0x33FFFFFF) else Color(0x1A000000))
            )
        }
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) activeTint else inactiveTint,
            modifier = Modifier.size(24.dp)
        )
    }
}
