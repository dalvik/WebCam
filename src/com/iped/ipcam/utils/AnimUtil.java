package com.iped.ipcam.utils;

import android.content.Context;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

public class AnimUtil {

	public static void animEff(Context context, View animView, int animId) {
		Animation shakeAnimation = AnimationUtils.loadAnimation(context, animId);
		animView.setAnimation(shakeAnimation);
	}
}
