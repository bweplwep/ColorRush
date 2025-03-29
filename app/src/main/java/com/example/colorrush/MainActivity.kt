package com.example.colorrush

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import kotlin.random.Random

class MainActivity : AppCompatActivity() {
    private lateinit var gameView: GameView
    private lateinit var taskText: TextView
    private lateinit var scoreText: TextView
    private lateinit var restartButton: MaterialButton
    private var score = 0
    private var currentTargetColor = Color.RED
    private var gameSpeed = 2000L // Начальная скорость в миллисекундах
    private val handler = Handler(Looper.getMainLooper())
    private val colors = listOf(
        Color.RED,
        Color.GREEN,
        Color.BLUE,
        Color.YELLOW,
        Color.MAGENTA,
        Color.CYAN
    )
    private var gameInProgress = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        taskText = findViewById(R.id.taskText)
        scoreText = findViewById(R.id.scoreText)
        restartButton = findViewById(R.id.restartButton)
        val gameContainer = findViewById<FrameLayout>(R.id.gameContainer)

        // Настраиваем кнопку рестарта
        restartButton.setOnClickListener {
            restartGame()
        }

        // Адаптация цветов для темной/светлой темы
        val isDarkTheme = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK == 
            android.content.res.Configuration.UI_MODE_NIGHT_YES
        
        val backgroundColor = if (isDarkTheme) {
            ContextCompat.getColor(this, R.color.background_dark)
        } else {
            ContextCompat.getColor(this, R.color.background_light)
        }
        
        gameView = GameView(this, backgroundColor)
        gameContainer.addView(gameView)

        startGame()
    }

    private fun startGame() {
        // Сбрасываем параметры игры
        score = 0
        gameSpeed = 2000L
        gameInProgress = true
        
        // Обновляем интерфейс
        scoreText.text = "Очки: 0"
        restartButton.visibility = View.GONE
        
        // Запускаем игровой процесс
        updateTargetColor()
        spawnColorBlock()
    }
    
    private fun restartGame() {
        // Очищаем игровое поле
        gameView.clearBlocks()
        
        // Запускаем игру заново
        startGame()
    }

    private fun updateTargetColor() {
        currentTargetColor = colors.random()
        val colorName = when (currentTargetColor) {
            Color.RED -> "КРАСНЫЙ"
            Color.GREEN -> "ЗЕЛЁНЫЙ"
            Color.BLUE -> "СИНИЙ"
            Color.YELLOW -> "ЖЁЛТЫЙ"
            Color.MAGENTA -> "МАГЕНТА"
            Color.CYAN -> "ЦИАН"
            else -> "НЕИЗВЕСТНЫЙ"
        }
        taskText.text = "Нажми на $colorName"
    }

    private fun spawnColorBlock() {
        // Проверяем, что игра активна
        if (!gameInProgress) return
        
        val block = ColorBlock(this, colors.random())
        gameView.addBlock(block)

        handler.postDelayed({
            spawnColorBlock()
        }, gameSpeed)
    }

    fun onBlockTapped(color: Int) {
        if (!gameInProgress) return
        
        if (color == currentTargetColor) {
            score += 10
            scoreText.text = "Очки: $score"
            updateTargetColor()
            // Увеличиваем скорость каждые 50 очков
            if (score % 50 == 0) {
                gameSpeed = (gameSpeed * 0.8).toLong()
            }
        } else {
            // Игра окончена
            gameOver()
        }
    }

    private fun gameOver() {
        gameInProgress = false
        handler.removeCallbacksAndMessages(null)
        taskText.text = "Игра окончена! Очки: $score"
        restartButton.visibility = View.VISIBLE
    }
}

class GameView(context: Context, private val backgroundColor: Int) : View(context) {
    private val blocks = mutableListOf<ColorBlock>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    fun addBlock(block: ColorBlock) {
        blocks.add(block)
        invalidate()
    }
    
    fun clearBlocks() {
        blocks.clear()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // Отрисовка фона с учетом выбранной темы
        canvas.drawColor(backgroundColor)
        
        blocks.forEach { block ->
            paint.color = block.color
            canvas.drawCircle(block.x, block.y, block.radius, paint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val x = event.x
                val y = event.y
                
                // Используем toList() для создания копии списка, чтобы избежать проблем при удалении во время итерации
                blocks.toList().forEach { block ->
                    if (block.contains(x, y)) {
                        (context as MainActivity).onBlockTapped(block.color)
                        blocks.remove(block)
                        invalidate()
                        return true
                    }
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}

class ColorBlock(context: Context, val color: Int) {
    val radius = 60f // Увеличил размер кругов для удобства игры
    val x: Float
    val y: Float

    init {
        val screenWidth = context.resources.displayMetrics.widthPixels
        val screenHeight = context.resources.displayMetrics.heightPixels
        x = Random.nextFloat() * (screenWidth - radius * 2) + radius
        y = Random.nextFloat() * (screenHeight - radius * 2) + radius
    }

    fun contains(touchX: Float, touchY: Float): Boolean {
        val dx = touchX - x
        val dy = touchY - y
        return dx * dx + dy * dy <= radius * radius
    }
} 