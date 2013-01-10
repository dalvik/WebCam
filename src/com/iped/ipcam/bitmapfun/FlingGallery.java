package com.iped.ipcam.bitmapfun;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.DisplayMetrics;
import android.util.FloatMath;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.view.animation.Transformation;
import android.widget.Adapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

// TODO:

// 1. In order to improve performance Cache screen bitmap and use for animation
// 2. Establish superfluous memory allocations and delay or replace with reused objects
//	  Probably need to make sure we are not allocating objects (strings, etc.) in loops

public class FlingGallery extends FrameLayout {
	// Constants

	private final int swipe_min_distance = 120;
	private final int swipe_max_off_path = 250;
	private final int swipe_threshold_veloicty = 400;

	// Properties

	private int mViewPaddingWidth = 0;
	private int mAnimationDuration = 200;
	private float mSnapBorderRatio = 0.5f;
	private boolean mIsGalleryCircular = true;

	// Members

	private int mGalleryWidth = 0;
	private boolean mIsTouched = false;
	private boolean mIsDragging = false;
	private float mCurrentOffset = 0.0f;
	private long mScrollTimestamp = 0;
	private int mFlingDirection = 0;
	private int mCurrentPosition = 0;
	private int mCurrentViewNumber = 0;

	private Context mContext;
	private Adapter mAdapter;
	private FlingGalleryView[] mViews;
	private FlingGalleryAnimation mAnimation;
	private GestureDetector mGestureDetector;
	private Interpolator mDecelerateInterpolater;

    static final int NONE = 0;// 初始状态
    static final int DRAG = 1;// 拖动
    static final int ZOOM = 2;// 缩放
    int mode = NONE;
    
    PointF prev = new PointF();
    PointF mid = new PointF();
    float dist = 1f;

    private VelocityTracker velocityTracker = null;
    
    private Matrix matrix = new Matrix();
    
    private Matrix savedMatrix = new Matrix();
    
    private float initImageWidth = 0;
    
    private float initImageHeight = 0;
    
    private DisplayMetrics dm;
    
    float minScaleR;// 最小缩放比例
    float curScaleR = 0;
    static float MAX_SCALE = 4f;// 最大缩放比例
    
	public FlingGallery(Context context, DisplayMetrics dm) {
		super(context);

		mContext = context;
		this.dm = dm;
		mAdapter = null;

		mViews = new FlingGalleryView[3];
		mViews[0] = new FlingGalleryView(0, this);
		mViews[1] = new FlingGalleryView(1, this);
		mViews[2] = new FlingGalleryView(2, this);

		mAnimation = new FlingGalleryAnimation();
		//mGestureDetector = new GestureDetector(new FlingGestureDetector());
		mDecelerateInterpolater = AnimationUtils.loadInterpolator(mContext,
				android.R.anim.decelerate_interpolator);
	}

	public void setPaddingWidth(int viewPaddingWidth) {
		mViewPaddingWidth = viewPaddingWidth;
	}

	public void setAnimationDuration(int animationDuration) {
		mAnimationDuration = animationDuration;
	}

	public void setSnapBorderRatio(float snapBorderRatio) {
		mSnapBorderRatio = snapBorderRatio;
	}

	public void setIsGalleryCircular(boolean isGalleryCircular) {
		if (mIsGalleryCircular != isGalleryCircular) {
			mIsGalleryCircular = isGalleryCircular;

			if (mCurrentPosition == getFirstPosition()) {
				// We need to reload the view immediately to the left to change
				// it to circular view or blank
				mViews[getPrevViewNumber(mCurrentViewNumber)].recycleView(getPrevPosition(mCurrentPosition));
			}

			if (mCurrentPosition == getLastPosition()) {
				// We need to reload the view immediately to the right to change
				// it to circular view or blank
				mViews[getNextViewNumber(mCurrentViewNumber)].recycleView(getNextPosition(mCurrentPosition));
			}
		}
	}

	public int getGalleryCount() {
		return (mAdapter == null) ? 0 : mAdapter.getCount();
	}

	public int getFirstPosition() {
		return 0;
	}

	public int getLastPosition() {
		return (getGalleryCount() == 0) ? 0 : getGalleryCount() - 1;
	}

	private int getPrevPosition(int relativePosition) {
		int prevPosition = relativePosition - 1;

		if (prevPosition < getFirstPosition()) {
			prevPosition = getFirstPosition() - 1;
			if (mIsGalleryCircular == true) {
				prevPosition = getLastPosition();
			}
		}

		return prevPosition;
	}

	private int getNextPosition(int relativePosition) {
		int nextPosition = relativePosition + 1;
		if (nextPosition > getLastPosition()) {
			nextPosition = getLastPosition() + 1;

			if (mIsGalleryCircular == true) {
				nextPosition = getFirstPosition();
			}
		}
		return nextPosition;
	}

	private int getPrevViewNumber(int relativeViewNumber) {
		return (relativeViewNumber == 0) ? 2 : relativeViewNumber - 1;
	}

	private int getNextViewNumber(int relativeViewNumber) {
		return (relativeViewNumber == 2) ? 0 : relativeViewNumber + 1;
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right,
			int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		// Calculate our view width
		mGalleryWidth = right - left;

		if (changed == true) {
			// Position views at correct starting offsets
			mViews[0].setOffset(0, 0, mCurrentViewNumber);
			mViews[1].setOffset(0, 0, mCurrentViewNumber);
			mViews[2].setOffset(0, 0, mCurrentViewNumber);
		}
	}

	public void setAdapter(Adapter adapter, int selectIndex) {
		mAdapter = adapter;
		if(selectIndex == -1) {
			mCurrentPosition = 0;
		} else {
			mCurrentPosition = selectIndex;
		}
		mCurrentViewNumber = 0;
		// mAdapter.getView(mCurrentPosition, convertView, parent)
		// Load the initial views from adapter
		mViews[0].recycleView(mCurrentPosition);
		mViews[1].recycleView(getNextPosition(mCurrentPosition));
		mViews[2].recycleView(getPrevPosition(mCurrentPosition));

		// Position views at correct starting offsets
		mViews[0].setOffset(0, 0, mCurrentViewNumber);
		mViews[1].setOffset(0, 0, mCurrentViewNumber);
		mViews[2].setOffset(0, 0, mCurrentViewNumber);
	}

	private int getViewOffset(int viewNumber, int relativeViewNumber) {
		// Determine width including configured padding width
		int offsetWidth = mGalleryWidth + mViewPaddingWidth;

		// Position the previous view one measured width to left
		if (viewNumber == getPrevViewNumber(relativeViewNumber)) {
			return offsetWidth;
		}

		// Position the next view one measured width to the right
		if (viewNumber == getNextViewNumber(relativeViewNumber)) {
			return offsetWidth * -1;
		}

		return 0;
	}

	public void movePrevious() {
		// Slide to previous view
		mFlingDirection = 1;
		processGesture();
	}

	public void moveNext() {
		// Slide to next view
		mFlingDirection = -1;
		processGesture();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_DPAD_LEFT:
			movePrevious();
			return true;

		case KeyEvent.KEYCODE_DPAD_RIGHT:
			moveNext();
			return true;

		case KeyEvent.KEYCODE_DPAD_CENTER:
		case KeyEvent.KEYCODE_ENTER:
		}

		return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if(velocityTracker == null) {
			velocityTracker = VelocityTracker.obtain();
		}
		velocityTracker.addMovement(event);
		switch (event.getAction() & MotionEvent.ACTION_MASK) {
        case MotionEvent.ACTION_DOWN:
            savedMatrix.set(matrix);
            prev.set(event.getX(), event.getY());
            mode = DRAG;
            // Stop animation
 			mIsTouched = true;
 			// Reset fling state
 			mFlingDirection = 0;
            break;
        case MotionEvent.ACTION_POINTER_DOWN:
            dist = spacing(event);
            if (spacing(event) > 10f) {
            	savedMatrix.set(matrix);
                midPoint(mid, event);
                mode = ZOOM;
            }
            break;
        case MotionEvent.ACTION_UP:
        case MotionEvent.ACTION_POINTER_UP:
            mode = NONE;
            final VelocityTracker mVelocityTracker = velocityTracker;  
            velocityTracker.computeCurrentVelocity(1000);  
            int velocityX = (int) mVelocityTracker.getXVelocity();
            if (Math.abs(prev.y - event.getY()) <= swipe_max_off_path) {
				if (event.getX() - prev.x > swipe_min_distance
						&& Math.abs(velocityX) > swipe_threshold_veloicty) {
					movePrevious();
				}

				if (prev.y - event.getX() > swipe_min_distance
						&& Math.abs(velocityX) > swipe_threshold_veloicty) {
					moveNext();
				}
			}
            mFlingDirection = 0;
            if (mIsTouched || mIsDragging) {
				processScrollSnap();
				processGesture();
			}
            break;
        case MotionEvent.ACTION_MOVE:
            if (mode == DRAG) {
            	 matrix.set(savedMatrix);
                 /*float dx = (event.getX() - prev.x);
                 if(Math.abs(dx)>5f){
                 	float dy = event.getY() - prev.y;
                 	matrix.postTranslate(dx, dy);
                 }*/
                 
            	if (mIsDragging == false) {
					// Stop animation
					mIsTouched = true;
					// Reconfigure scroll
					mIsDragging = true;
					mFlingDirection = 0;
					mScrollTimestamp = System.currentTimeMillis();
					mCurrentOffset = mViews[mCurrentViewNumber]
							.getCurrentOffset();
				}
            	float maxVelocity = mGalleryWidth / (mAnimationDuration / 1000.0f);
				long timestampDelta = System.currentTimeMillis() - mScrollTimestamp;
				float maxScrollDelta = maxVelocity * (timestampDelta / 1000.0f);
				float currentScrollDelta = prev.x - event.getX();

				if (currentScrollDelta < maxScrollDelta * -1)
					currentScrollDelta = maxScrollDelta * -1;
				if (currentScrollDelta > maxScrollDelta)
					currentScrollDelta = maxScrollDelta;
				int scrollOffset = Math.round(mCurrentOffset
						+ currentScrollDelta);

				// We can't scroll more than the width of our own frame layout
				if (scrollOffset >= mGalleryWidth)
					scrollOffset = mGalleryWidth;
				if (scrollOffset <= mGalleryWidth * -1)
					scrollOffset = mGalleryWidth * -1;

				mViews[0].setOffset(scrollOffset, 0, mCurrentViewNumber);
				mViews[1].setOffset(scrollOffset, 0, mCurrentViewNumber);
				mViews[2].setOffset(scrollOffset, 0, mCurrentViewNumber);
            } else if (mode == ZOOM) {
            	float newDist = spacing(event);
                if (newDist > 10f) {
                    matrix.set(savedMatrix);
                    float tScale = newDist / dist;
                    matrix.postScale(tScale, tScale, mid.x, mid.y);
                }
            }
            break;
        }
		mViews[mCurrentViewNumber].mExternalView.setImageMatrix(matrix);
        CheckView();
        return true;
	}
	
	private void processGesture() {
		int newViewNumber = mCurrentViewNumber;
		int reloadViewNumber = 0;
		int reloadPosition = 0;

		mIsTouched = false;
		mIsDragging = false;

		if (mFlingDirection > 0) {
			if (mCurrentPosition > getFirstPosition()
					|| mIsGalleryCircular == true) {
				// Determine previous view and outgoing view to recycle
				newViewNumber = getPrevViewNumber(mCurrentViewNumber);
				mCurrentPosition = getPrevPosition(mCurrentPosition);
				reloadViewNumber = getNextViewNumber(mCurrentViewNumber);
				reloadPosition = getPrevPosition(mCurrentPosition);
			}
		}

		if (mFlingDirection < 0) {
			if (mCurrentPosition < getLastPosition()
					|| mIsGalleryCircular == true) {
				// Determine the next view and outgoing view to recycle
				newViewNumber = getNextViewNumber(mCurrentViewNumber);
				mCurrentPosition = getNextPosition(mCurrentPosition);
				reloadViewNumber = getPrevViewNumber(mCurrentViewNumber);
				reloadPosition = getNextPosition(mCurrentPosition);
			}
		}

		if (newViewNumber != mCurrentViewNumber) {
			mCurrentViewNumber = newViewNumber;
			// Reload outgoing view from adapter in new position
			mViews[reloadViewNumber].recycleView(reloadPosition);
			
		}

		// Ensure input focus on the current view
		mViews[mCurrentViewNumber].requestFocus();
		mViews[mCurrentViewNumber].mExternalView.setImageMatrix(matrix);
		// Run the slide animations for view transitions
		mAnimation.prepareAnimation(mCurrentViewNumber);
		this.startAnimation(mAnimation);
		// Reset fling state
		mFlingDirection = 0;
	}

	private void processScrollSnap() {
		// Snap to next view if scrolled passed snap position
		float rollEdgeWidth = mGalleryWidth * mSnapBorderRatio;
		int rollOffset = mGalleryWidth - (int) rollEdgeWidth;
		int currentOffset = mViews[mCurrentViewNumber].getCurrentOffset();

		if (currentOffset <= rollOffset * -1) {
			// Snap to previous view
			mFlingDirection = 1;
		}

		if (currentOffset >= rollOffset) {
			// Snap to next view
			mFlingDirection = -1;
		}
	}


    
    private float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return FloatMath.sqrt(x * x + y * y);
    }
    
	private void midPoint(PointF point, MotionEvent event) {
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        point.set(x / 2, y / 2);
    }
	
	private class FlingGalleryView {
		private int mViewNumber;
		private FrameLayout mParentLayout;

		private ImageView mInvalidLayout = null;
		private LinearLayout mInternalLayout = null;
		private ImageView mExternalView = null;

		public FlingGalleryView(int viewNumber, FrameLayout parentLayout) {
			mViewNumber = viewNumber;
			mParentLayout = parentLayout;

			// Invalid layout is used when outside gallery
			mInvalidLayout = new ImageView(mContext);
			mInvalidLayout.setLayoutParams(new LinearLayout.LayoutParams(
					android.view.ViewGroup.LayoutParams.MATCH_PARENT,
					android.view.ViewGroup.LayoutParams.MATCH_PARENT));

			// Internal layout is permanent for duration
			mInternalLayout = new LinearLayout(mContext);
			mInternalLayout.setLayoutParams(new LinearLayout.LayoutParams(
					android.view.ViewGroup.LayoutParams.MATCH_PARENT,
					android.view.ViewGroup.LayoutParams.MATCH_PARENT));

			mParentLayout.addView(mInternalLayout);
		}

		public void recycleView(int newPosition) {
			if (mExternalView != null) {
				mInternalLayout.removeView(mExternalView);
			}

			if (mAdapter != null) {
				if (newPosition >= getFirstPosition()
						&& newPosition <= getLastPosition()) {
					mExternalView = (ImageView)mAdapter.getView(newPosition,mExternalView, mInternalLayout);
				} else {
					mExternalView = mInvalidLayout;
				}
			}

			if (mExternalView != null) {
				mInternalLayout.addView(mExternalView, new LinearLayout.LayoutParams(
										android.view.ViewGroup.LayoutParams.MATCH_PARENT,
										android.view.ViewGroup.LayoutParams.MATCH_PARENT));
			}
		}

		public void setOffset(int xOffset, int yOffset, int relativeViewNumber) {
			// Scroll the target view relative to its own position relative to
			// currently displayed view
			mInternalLayout.scrollTo(
					getViewOffset(mViewNumber, relativeViewNumber) + xOffset,
					yOffset);
		}

		public int getCurrentOffset() {
			// Return the current scroll position
			return mInternalLayout.getScrollX();
		}

		public void requestFocus() {
			mInternalLayout.requestFocus();
		}
	}

	private class FlingGalleryAnimation extends Animation {
		private boolean mIsAnimationInProgres;
		private int mRelativeViewNumber;
		private int mInitialOffset;
		private int mTargetOffset;
		private int mTargetDistance;

		public FlingGalleryAnimation() {
			mIsAnimationInProgres = false;
			mRelativeViewNumber = 0;
			mInitialOffset = 0;
			mTargetOffset = 0;
			mTargetDistance = 0;
		}

		public void prepareAnimation(int relativeViewNumber) {
			// If we are animating relative to a new view
			if (mRelativeViewNumber != relativeViewNumber) {
				if (mIsAnimationInProgres == true) {
					// We only have three views so if requested again to animate
					// in same direction we must snap
					int newDirection = (relativeViewNumber == getPrevViewNumber(mRelativeViewNumber)) ? 1
							: -1;
					int animDirection = (mTargetDistance < 0) ? 1 : -1;

					// If animation in same direction
					if (animDirection == newDirection) {
						// Ran out of time to animate so snap to the target
						// offset
						mViews[0].setOffset(mTargetOffset, 0,
								mRelativeViewNumber);
						mViews[1].setOffset(mTargetOffset, 0,
								mRelativeViewNumber);
						mViews[2].setOffset(mTargetOffset, 0,
								mRelativeViewNumber);
					}
				}

				// Set relative view number for animation
				mRelativeViewNumber = relativeViewNumber;
			}

			// Note: In this implementation the targetOffset will always be zero
			// as we are centering the view; but we include the calculations of
			// targetOffset and targetDistance for use in future implementations

			mInitialOffset = mViews[mRelativeViewNumber].getCurrentOffset();
			mTargetOffset = getViewOffset(mRelativeViewNumber,
					mRelativeViewNumber);
			mTargetDistance = mTargetOffset - mInitialOffset;

			// Configure base animation properties
			this.setDuration(mAnimationDuration);
			this.setInterpolator(mDecelerateInterpolater);

			// Start/continued animation
			mIsAnimationInProgres = true;
		}

		@Override
		protected void applyTransformation(float interpolatedTime,
				Transformation transformation) {
			// Ensure interpolatedTime does not over-shoot then calculate new
			// offset
			interpolatedTime = (interpolatedTime > 1.0f) ? 1.0f : interpolatedTime;
			int offset = mInitialOffset + (int) (mTargetDistance * interpolatedTime);

			for (int viewNumber = 0; viewNumber < 3; viewNumber++) {
				// Only need to animate the visible views as the other view will
				// always be off-screen
				if ((mTargetDistance > 0 && viewNumber != getNextViewNumber(mRelativeViewNumber))
						|| (mTargetDistance < 0 && viewNumber != getPrevViewNumber(mRelativeViewNumber))) {
					mViews[viewNumber].setOffset(offset, 0, mRelativeViewNumber);
				}
			}
		}

		@Override
		public boolean getTransformation(long currentTime,
				Transformation outTransformation) {
			if (super.getTransformation(currentTime, outTransformation) == false) {
				// Perform final adjustment to offsets to cleanup animation
				mViews[0].setOffset(mTargetOffset, 0, mRelativeViewNumber);
				mViews[1].setOffset(mTargetOffset, 0, mRelativeViewNumber);
				mViews[2].setOffset(mTargetOffset, 0, mRelativeViewNumber);

				// Reached the animation target
				mIsAnimationInProgres = false;

				return false;
			}

			// Cancel if the screen touched
			if (mIsTouched || mIsDragging) {
				// Note that at this point we still consider ourselves to be
				// animating
				// because we have not yet reached the target offset; its just
				// that the
				// user has temporarily interrupted the animation with a touch
				// gesture

				return false;
			}

			return true;
		}
	}

	public void updateResolution(String path, int w, int h) {
		initImageWidth = w;
		initImageHeight = h;
		minZoom();
		center();
		
		/*System.out.println("path = " + path + " " + w + " " + h);
		ImageInfo imageInfo = (ImageInfo)mAdapter.getItem(mCurrentViewNumber);
		System.out.println("current path = " + imageInfo.path);*/
	}
	
	private void minZoom() {
        minScaleR = Math.min((float) dm.widthPixels / (float) initImageWidth, (float) dm.heightPixels / (float) initImageHeight);
        System.out.println("minScaleR=" + minScaleR);
        curScaleR = minScaleR;
        //MAX_SCALE = Math.max((float) initImageWidth / (float) dm.widthPixels, (float) initImageHeight / (float) dm.heightPixels);;
        if (minScaleR != 1.0f) {
            matrix.postScale(minScaleR, minScaleR);
        }
    }
	
    private void CheckView() {
        float p[] = new float[9];
        matrix.getValues(p);
        curScaleR = p[0];
        System.out.println("curScaleR=" + curScaleR);
        if (mode == ZOOM) {
            if (curScaleR < minScaleR) {
                matrix.setScale(minScaleR, minScaleR);
            }
            if (curScaleR > MAX_SCALE) {
                matrix.set(savedMatrix);
            }
        }
        center();
    }
    
    private void center() {
        center(true, true);
    }
    
    protected void center(boolean horizontal, boolean vertical) {

        Matrix m = new Matrix();
        m.set(matrix);
        RectF rect = new RectF(0, 0, initImageWidth, initImageHeight);
        m.mapRect(rect);
        float height = rect.height();
        float width = rect.width();
        float deltaX = 0, deltaY = 0;

        if (vertical) {
            int screenHeight = dm.heightPixels;
            if (height <= screenHeight) {
                deltaY = (screenHeight - height) / 2 - rect.top;
            } else if (rect.top > 0) {
                deltaY = -rect.top;
            } else if (rect.bottom < screenHeight) {
                deltaY = screenHeight - rect.bottom;
            }
        }

        if (horizontal) {
            int screenWidth = dm.widthPixels;
            if (width <= screenWidth) {
                deltaX = (screenWidth - width) / 2 - rect.left;
            } else if (rect.left > 0) {
                deltaX = -rect.left;
            } else if (rect.right < screenWidth) {
                deltaX = screenWidth - rect.right;
            }
        }
       // Log.d("System.out", "deltaX= " + deltaX + " deltaY=" + deltaY);
        matrix.postTranslate(deltaX, deltaY);
    }

}
