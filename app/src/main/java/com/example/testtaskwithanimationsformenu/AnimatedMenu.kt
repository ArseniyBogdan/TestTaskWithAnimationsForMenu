package com.example.testtaskwithanimationsformenu

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.PointF
import android.graphics.PorterDuff
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.Animation
import androidx.annotation.Dimension
import androidx.annotation.Px
import androidx.core.animation.addListener
import androidx.core.animation.doOnEnd
import kotlin.math.abs
import kotlin.math.max


class AnimatedMenu@JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
): ViewGroup(context, attrs, defStyleAttr) {
    private val pointClicked = PointF(0f, 0f)
    private var clickedViewIndex = -1
    private var menuShown: Boolean = false
    private val views: MutableList<View> = ArrayList<View>()

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if(views.isEmpty()){
            for(i in 0 until childCount){
                Log.d("Child $i: ", getChildAt(i).toString())
                views.add(getChildAt(i))
            }
        }

        for(view in views){
            measureChild(view)
        }

        var desiredWidth = 0
        var desiredHeight = 0
        for(view in views){
            val pixelsHeight = dpToPixels(view.measuredHeight)
            val pixelsWidth = dpToPixels(view.measuredWidth)
            desiredWidth = max(desiredWidth, pixelsWidth)
            desiredHeight += pixelsHeight
        }

        setMeasuredDimension(desiredWidth, desiredHeight)
    }

    @Px
    private fun dpToPixels(@Dimension(unit = Dimension.DP) dp: Int): Int{
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(), resources.displayMetrics).toInt()
    }

    override fun onLayout(p0: Boolean, p1: Int, p2: Int, p3: Int, p4: Int) {
        var height = 0 // конец анимации
        for(view in views){
            val pixelsHeight = dpToPixels(view.measuredHeight)
            val pixelsWidth = dpToPixels(view.measuredWidth)
            if(menuShown) {
                view.translationY = 0f
                view.layout(
                    0, height, pixelsWidth,
                    height + pixelsHeight
                )
            }
            else{
                view.layout(
                    0, measuredHeight, pixelsWidth,
                    measuredHeight + pixelsHeight
                )
                // Должны посчитать смещение
            }
            height += pixelsHeight
        }
    }

    private fun measureChild(child: View){
        val childHeightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        val childWidthSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        child.measure(childHeightSpec, childWidthSpec)
    }

    fun startAnimation(){
        if(menuShown){
            startBackwardAnimation()
            menuShown = false
        }
        else{
            startForwardAnimation()
            menuShown = true
        }
        // тут надо делать выбор, в какую сторону будет двигать наши дочерние элементы
        // при этом надо изначально распологать их в кучке вне этого Layout,
        // они должны выползать с позиции на одну ниже
        // движение должно быть с ускорением вначале и с замедление в конце
        // после этого надо будет придумать как сделать swipe и при этом сдвигать
        // каждый элемент вниз после swipe, тоже своей анимацией.
    }

    private fun startForwardAnimation(){
        var desiredHeight = 0
        val animators: MutableList<Animator> = ArrayList()
        for(index in 0 until views.size){
            val va = ObjectAnimator.ofFloat(views[index], TRANSLATION_Y,
                (desiredHeight - height).toFloat())
            animators.add(va)
            desiredHeight += views[index].height
        }
        val animatorSet = AnimatorSet()
        animatorSet.playSequentially(animators)
        animatorSet.duration = 200
        animatorSet.interpolator = AccelerateInterpolator()
        animatorSet.start()
    }

    private fun startBackwardAnimation(){
        var desiredHeight = 0
        val animators: MutableList<Animator> = ArrayList()
        for(index in 0 until views.size){
            val va = ObjectAnimator.ofFloat(views[index], TRANSLATION_Y,
                -views[index].translationY)
            views[index].translationY
            animators.add(va)
            desiredHeight += views[index].height
        }
        val animatorSet = AnimatorSet()
        animatorSet.playSequentially(animators)
        animatorSet.duration = 200
        animatorSet.interpolator = AccelerateInterpolator()
        animatorSet.start()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when(event.action){
            MotionEvent.ACTION_DOWN ->{
                pointClicked.apply {
                    x = event.x
                    y = event.y
                }
                clickedViewIndex = findViewByXY(event.x.toInt(), event.y.toInt())
            }
            MotionEvent.ACTION_UP ->{
                if(clickedViewIndex >= 0){
                    // нужно сверить, удалять или нет
                    val absOffsetX = abs(pointClicked.x - event.x.toInt())
                    if(absOffsetX > measuredWidth/2){
                        descentAnimation(clickedViewIndex)
                        views.removeAt(clickedViewIndex)
                    }
                    else{
                        views[clickedViewIndex].alpha = 1f
                        views[clickedViewIndex].x = 0f // тут начальные координаты надо бы ткнуть(хотя хз)
                    }
                }
                clickedViewIndex = -1
            }
            MotionEvent.ACTION_MOVE ->{
                val absOffsetX = abs(pointClicked.x - event.x.toInt())
                if(clickedViewIndex >= 0 && absOffsetX > measuredWidth/8){
                    val alphaView = 1 - absOffsetX / (measuredWidth/2f)
                    views[clickedViewIndex].alpha = max(alphaView, 0f)
                    views[clickedViewIndex].translationX = event.x - views[clickedViewIndex].width/2
                }
            }
        }
        return true
    }

    private fun findViewByXY(x: Int, y: Int): Int{
        for(i in 0 until views.size){
            if(views[i].x < x && x < views[i].x + views[i].width &&
                views[i].y < y && y < views[i].y + views[i].height){
                return i
            }
        }
        return -1
    }

    private fun descentAnimation(indexOfSwipedElement: Int){
        val animators: MutableList<Animator> = ArrayList()
        for(index in indexOfSwipedElement - 1 downTo 0){
            views[index].translationY
            val va = ObjectAnimator.ofFloat(views[index], TRANSLATION_Y,
                views[index].translationY + views[index].height)
            if(index == 0) {
                va.doOnEnd { removeViewAt(indexOfSwipedElement) }
            }
            animators.add(va)
        }

        AnimatorSet().apply{
            playSequentially(animators)
            duration = 200
            interpolator = AccelerateInterpolator()
            start()
        }
    }

    fun addNewView(view: View){
        views.add(view)
        addView(view)
    }
}