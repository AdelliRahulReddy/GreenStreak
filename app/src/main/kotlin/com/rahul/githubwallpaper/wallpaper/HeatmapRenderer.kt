package com.rahul.githubwallpaper.wallpaper

import android.graphics.*
import com.rahul.githubwallpaper.data.CachedContributionData
import com.rahul.githubwallpaper.utils.DateUtils
import kotlin.random.Random

class HeatmapRenderer(
    private val width: Int,
    private val height: Int,
    private val isDarkMode: Boolean
) {
    
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    
    // GitHub colors
    private val colorsLight = intArrayOf(
        Color.parseColor("#EBEDF0"), Color.parseColor("#9BE9A8"),
        Color.parseColor("#40C463"), Color.parseColor("#30A14E"),
        Color.parseColor("#216E39")
    )
    
    private val colorsDark = intArrayOf(
        Color.parseColor("#161B22"), Color.parseColor("#0E4429"),
        Color.parseColor("#006D32"), Color.parseColor("#26A641"),
        Color.parseColor("#39D353")
    )
    
    private val colors = if (isDarkMode) colorsDark else colorsLight
    private val backgroundColor = if (isDarkMode) Color.parseColor("#0D1117") else Color.WHITE
    private val textColor = if (isDarkMode) Color.parseColor("#C9D1D9") else Color.parseColor("#24292F")
    private val accentColor = Color.parseColor("#39D353")
    private val todayHighlightColor = Color.parseColor("#FF6B35")
    
    private val quotes = arrayOf(
        "Commit to excellence every day",
        "Small commits lead to big changes",
        "Consistency is the key to mastery",
        "Build your legacy, one commit at a time",
        "Your code tells your story",
        "Progress over perfection",
        "Every day is a chance to improve",
        "Code with passion, ship with pride"
    )
    
    fun drawHeatmap(canvas: Canvas, data: CachedContributionData?) {
        canvas.drawColor(backgroundColor)
        
        if (data == null) {
            drawNoDataMessage(canvas)
            return
        }
        
        // Filter to only 2026 data
        val weeks2026 = data.weeks.filter { week ->
            week.contributionDays.any { it.date.startsWith("2026") }
        }.map { week ->
            com.rahul.githubwallpaper.data.Week(
                week.contributionDays.filter { it.date.startsWith("2026") }
            )
        }.filter { it.contributionDays.isNotEmpty() }
        
        if (weeks2026.isEmpty()) {
            drawNoDataMessage(canvas)
            return
        }
        
        drawCenteredLayout(canvas, data, weeks2026)
    }
    
    private fun drawNoDataMessage(canvas: Canvas) {
        textPaint.color = textColor
        textPaint.textSize = width * 0.05f
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("No contribution data", width / 2f, height / 2f, textPaint)
    }
    
    private fun drawCenteredLayout(canvas: Canvas, data: CachedContributionData, weeks2026: List<com.rahul.githubwallpaper.data.Week>) {
        val centerX = width / 2f
        var yPos = height * 0.22f
        
        // 1. Total contributions FIRST
        val total2026 = weeks2026.flatMap { it.contributionDays }.sumOf { it.contributionCount }
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.color = accentColor
        textPaint.textSize = width * 0.06f
        canvas.drawText("$total2026 contributions in 2026", centerX, yPos, textPaint)
        yPos += height * 0.05f
        
        // 2. Motivational quote SECOND
        textPaint.color = textColor
        textPaint.textSize = width * 0.045f
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        val quote = quotes[Random.nextInt(quotes.size)]
        
        // Split quote into two lines if it's too long
        if (quote.length > 30) {
            val words = quote.split(" ")
            val mid = words.size / 2
            val line1 = words.subList(0, mid).joinToString(" ")
            val line2 = words.subList(mid, words.size).joinToString(" ")
            canvas.drawText("\"$line1", centerX, yPos, textPaint)
            canvas.drawText("$line2\"", centerX, yPos + textPaint.textSize * 1.2f, textPaint)
        } else {
            canvas.drawText("\"$quote\"", centerX, yPos, textPaint)
        }
        
        // 3. Draw graph box in center at FIXED position (closer to quote)
        drawCenteredGraph(canvas, weeks2026, height * 0.36f)
    }
    
    private fun drawCenteredGraph(canvas: Canvas, weeks: List<com.rahul.githubwallpaper.data.Week>, startY: Float) {
        val boxPadding = width * 0.08f
        val availableWidth = width - (2 * boxPadding)
        val availableHeight = height * 0.45f
        
        val numWeeks = weeks.size
        val numDays = 7
        
        // Calculate cell size to fit in box
        val cellSize = minOf(
            availableWidth / (numWeeks + 1), // +1 for day labels space
            availableHeight / numDays
        ) * 0.85f
        
        val gap = cellSize * 0.15f
        
        // Calculate grid dimensions
        val gridWidth = numWeeks * cellSize + (numWeeks - 1) * gap
        val gridHeight = numDays * cellSize + (numDays - 1) * gap
        
        // Center the grid horizontally
        val dayLabelWidth = cellSize * 1.2f
        val startX = (width - gridWidth - dayLabelWidth) / 2f + dayLabelWidth
        val gridTop = startY
        
        val today = DateUtils.getTodayDate()
        
        // Draw day labels
        drawDayLabels(canvas, startX - cellSize * 1.5f, gridTop, cellSize, gap)
        
        // Draw the grid
        weeks.forEachIndexed { weekIndex, week ->
            week.contributionDays.forEachIndexed { dayIndex, day ->
                val x = startX + weekIndex * (cellSize + gap)
                val y = gridTop + dayIndex * (cellSize + gap)
                
                val level = day.getLevelInt().coerceIn(0, 4)
                paint.color = colors[level]
                paint.style = Paint.Style.FILL
                
                val rect = RectF(x, y, x + cellSize, y + cellSize)
                val cornerRadius = cellSize * 0.2f
                
                if (day.date == today) {
                    // Today's highlight
                    paint.setShadowLayer(cellSize * 0.5f, 0f, 0f, todayHighlightColor)
                    canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)
                    paint.clearShadowLayer()
                    
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = cellSize * 0.12f
                    paint.color = todayHighlightColor
                    canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)
                    paint.style = Paint.Style.FILL
                } else {
                    canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)
                }
            }
        }
        
        // Draw month labels below
        drawMonthLabels(canvas, weeks, startX, gridTop + gridHeight + cellSize * 0.8f, cellSize, gap)
    }
    
    private fun drawDayLabels(canvas: Canvas, x: Float, startY: Float, cellSize: Float, gap: Float) {
        val days = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        textPaint.color = textColor
        textPaint.textSize = cellSize * 0.4f
        textPaint.textAlign = Paint.Align.RIGHT
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        
        days.forEachIndexed { index, day ->
            val y = startY + index * (cellSize + gap) + cellSize * 0.65f
            canvas.drawText(day, x, y, textPaint)
        }
    }
    
    private fun drawMonthLabels(
        canvas: Canvas,
        weeks: List<com.rahul.githubwallpaper.data.Week>,
        startX: Float,
        yPos: Float,
        cellSize: Float,
        gap: Float
    ) {
        textPaint.color = textColor
        textPaint.textSize = cellSize * 0.55f
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        
        var lastMonth = ""
        weeks.forEachIndexed { index, week ->
            if (week.contributionDays.isNotEmpty()) {
                val date = week.contributionDays.first().date
                val month = date.substring(5, 7)
                
                if (month != lastMonth) {
                    val x = startX + index * (cellSize + gap) + cellSize / 2
                    val monthName = getMonthName(month)
                    canvas.drawText(monthName, x, yPos, textPaint)
                    lastMonth = month
                }
            }
        }
    }
    
    private fun getMonthName(monthNumber: String): String {
        return when (monthNumber) {
            "01" -> "Jan"
            "02" -> "Feb"
            "03" -> "Mar"
            "04" -> "Apr"
            "05" -> "May"
            "06" -> "Jun"
            "07" -> "Jul"
            "08" -> "Aug"
            "09" -> "Sep"
            "10" -> "Oct"
            "11" -> "Nov"
            "12" -> "Dec"
            else -> ""
        }
    }
}
