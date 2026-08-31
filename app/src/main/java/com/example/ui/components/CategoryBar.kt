package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CategoryEntity
import com.example.data.model.SoundClipEntity
import com.example.ui.CategoryFilter
import com.example.ui.theme.MinimalBorder
import com.example.ui.theme.MinimalBorderActive
import com.example.ui.theme.MinimalPrimary
import com.example.ui.theme.MinimalSurface
import com.example.ui.theme.MinimalSurfaceElevated
import com.example.ui.theme.MinimalSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun CategoryBar(
    categories: List<CategoryEntity>,
    allSounds: List<SoundClipEntity>,
    selectedFilter: CategoryFilter,
    onFilterSelected: (CategoryFilter) -> Unit,
    onManageCategoriesClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalCount = allSounds.size
    val favCount = allSounds.count { it.isFavorite }

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // "All Sounds" Chip
        item {
            CategoryChip(
                label = "All Sounds",
                icon = "🔥",
                count = totalCount,
                isSelected = selectedFilter is CategoryFilter.All,
                accentColor = MinimalPrimary,
                testTag = "category_chip_all",
                onClick = { onFilterSelected(CategoryFilter.All) }
            )
        }

        // "Favorites" Chip
        item {
            CategoryChip(
                label = "Favorites",
                icon = "⭐",
                count = favCount,
                isSelected = selectedFilter is CategoryFilter.Favorites,
                accentColor = Color(0xFFE11D48),
                testTag = "category_chip_favorites",
                onClick = { onFilterSelected(CategoryFilter.Favorites) }
            )
        }

        // Custom Categories
        items(categories, key = { it.id }) { cat ->
            val count = allSounds.count { it.categoryId == cat.id }
            val catColor = parseColorSafe(cat.colorHex)
            val isSelected = selectedFilter is CategoryFilter.Custom && selectedFilter.categoryId == cat.id

            CategoryChip(
                label = cat.name,
                icon = cat.icon.ifEmpty { "📁" },
                count = count,
                isSelected = isSelected,
                accentColor = catColor,
                testTag = "category_chip_${cat.id}",
                onClick = { onFilterSelected(CategoryFilter.Custom(cat.id)) }
            )
        }

        // "Manage / Add Category" Button
        item {
            Box(
                modifier = Modifier
                    .testTag("manage_categories_button")
                    .semantics { contentDescription = "Manage and add categories" }
                    .clip(RoundedCornerShape(20.dp))
                    .background(MinimalSurfaceVariant)
                    .border(1.dp, MinimalBorder, RoundedCornerShape(20.dp))
                    .clickable { onManageCategoriesClick() }
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "Categories",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryChip(
    label: String,
    icon: String,
    count: Int,
    isSelected: Boolean,
    accentColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) accentColor.copy(alpha = 0.12f) else MinimalSurface,
        label = "chipBg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) accentColor else MinimalBorder,
        label = "chipBorder"
    )
    val textColor = if (isSelected) accentColor else TextPrimary

    Box(
        modifier = Modifier
            .testTag(testTag)
            .semantics { contentDescription = "Category $label with $count sounds, ${if (isSelected) "selected" else "not selected"}" }
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .border(if (isSelected) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = icon, fontSize = 13.sp)
            Text(
                text = label,
                color = textColor,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
            // Count pill
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (isSelected) accentColor.copy(alpha = 0.2f) else MinimalSurfaceVariant)
                    .padding(horizontal = 6.dp, vertical = 1.dp)
            ) {
                Text(
                    text = count.toString(),
                    color = if (isSelected) accentColor else TextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
