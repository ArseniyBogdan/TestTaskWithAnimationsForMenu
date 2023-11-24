package com.example.testtaskwithanimationsformenu

import android.animation.ValueAnimator
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val menu = findViewById<AnimatedMenu>(R.id.menu)

        val button = findViewById<Button>(R.id.button_menu)
        button.setOnClickListener{
            menu.startAnimation()
        }
        // проблема заключается в том, что мы не выезжаем за границы своего layout,
        // надо всё же уметь отрисовывать эти элементы поверх, то есть сделать ещё одну вложенность
        // типо то что сделали уже будет, но мы добавим FrameLayout
    }
}