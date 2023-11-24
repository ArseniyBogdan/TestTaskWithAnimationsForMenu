package com.example.testtaskwithanimationsformenu

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.PointF
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import androidx.annotation.Dimension
import androidx.annotation.Px
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
    private val views: MutableList<View> = ArrayList()

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if(views.isEmpty()){
            for(i in 0 until childCount){
                Log.d("Child $i: ", getChildAt(i).toString())
                views.add(getChildAt(i))
            }
        }

        var desiredWidth = 0
        var desiredHeight = 0
        for(view in views){
            measureChild(view)
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
        var height = 0
        for(view in views){
            val pixelsHeight = dpToPixels(view.measuredHeight)
            val pixelsWidth = dpToPixels(view.measuredWidth)
            view.layout(
                0, height, pixelsWidth,
                height + pixelsHeight
            )
            if(menuShown) {
                view.translationY = 0f
            }
            else{
                // hiding elements below of the ViewGroup
                view.translationY = measuredHeight.toFloat() - height
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
    }

    // animation of the appearance of views
    private fun startForwardAnimation(){
        val animators: MutableList<Animator> = ArrayList()
        for(index in 0 until views.size){
            val va = ObjectAnimator.ofFloat(views[index], TRANSLATION_Y, 0f)
            animators.add(va)
        }
        startAnim(animators)
    }

    // animation of the disappearing of views
    private fun startBackwardAnimation(){
        var desiredHeight = 0
        val animators: MutableList<Animator> = ArrayList()
        for(index in 0 until views.size){
            val va = ObjectAnimator.ofFloat(views[index], TRANSLATION_Y,
                measuredHeight - desiredHeight.toFloat())
            animators.add(va)
            desiredHeight += views[index].height
        }
        startAnim(animators)
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
                    val absOffsetX = abs(pointClicked.x - event.x.toInt())
                    if(absOffsetX > measuredWidth/2){
                        descentAnimation(clickedViewIndex)
                        views.removeAt(clickedViewIndex)
                    }
                    else{
                        views[clickedViewIndex].alpha = 1f
                        views[clickedViewIndex].x = 0f
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

    // search for the index of the element that the point belongs to
    private fun findViewByXY(x: Int, y: Int): Int{
        for(i in 0 until views.size){
            if(views[i].x < x && x < views[i].x + views[i].width &&
                views[i].y < y && y < views[i].y + views[i].height){
                return i
            }
        }
        return -1
    }

    // drop animation for views, which were located above swiped view
    private fun descentAnimation(indexOfSwipedElement: Int){
        val animators: MutableList<Animator> = ArrayList()
        for(index in indexOfSwipedElement - 1 downTo 0){
            val va = ObjectAnimator.ofFloat(views[index], TRANSLATION_Y,
                views[index].height.toFloat())
            if(index == 0) {
                va.doOnEnd { removeViewAt(indexOfSwipedElement) }
            }
            animators.add(va)
        }
        startAnim(animators)
    }

    private fun startAnim(views: List<Animator>){
        AnimatorSet().apply{
            playSequentially(views)
            duration = 200
            interpolator = AccelerateInterpolator()
            start()
        }
    }

    // for programmatically adding views
    fun addNewView(view: View){
        views.add(view)
        addView(view)
    }
}