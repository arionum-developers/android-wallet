package arionum.net.cubedpixels.style;

import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;

public class Styler {
	//CUSTOM STYER
	//ITERATED THROUGH EVERY VIEW AND SEARCHES FOR TEXTVIEW AND BUTTONS
	//TO STYLE A CERTAIN FONT
	//REALLY STUPID xD
	//DONT USE THIS STUFF
	
	public static void styleText(Context context, TextView tv) {
		Typeface myTypeface = Typeface.createFromAsset(context.getAssets(), "fonts/segoeui2.ttf");
		tv.setTypeface(myTypeface);
	}

	private static void initStyle(Context context, RelativeLayout rl) {

		ArrayList<TextView> TVlist = new ArrayList<TextView>();
		ArrayList<Button> BTlist = new ArrayList<Button>();
		RelativeLayout layout = rl;
		for (int i = 0; i < layout.getChildCount(); i++) {
			if (layout.getChildAt(i) instanceof RelativeLayout)
				initStyle(context, (RelativeLayout) layout.getChildAt(i));
			if (layout.getChildAt(i) instanceof LinearLayout)
				initStyle(context, (LinearLayout) layout.getChildAt(i));
			if (layout.getChildAt(i) instanceof TextView)
				TVlist.add((TextView) layout.getChildAt(i));
			if (layout.getChildAt(i) instanceof Button)
				TVlist.add((Button) layout.getChildAt(i));
		}
		for (TextView i : TVlist)
			Styler.styleText(context, i);
		for (Button i : BTlist)
			Styler.styleText(context, i);

	}

	private static void initStyle(Context context, LinearLayout rl) {

		ArrayList<TextView> TVlist = new ArrayList<TextView>();
		ArrayList<Button> BTlist = new ArrayList<Button>();
		LinearLayout layout = rl;
		for (int i = 0; i < layout.getChildCount(); i++) {
			if (layout.getChildAt(i) instanceof LinearLayout)
				initStyle(context, (LinearLayout) layout.getChildAt(i));
			if (layout.getChildAt(i) instanceof RelativeLayout)
				initStyle(context, (RelativeLayout) layout.getChildAt(i));
			if (layout.getChildAt(i) instanceof TextView)
				TVlist.add((TextView) layout.getChildAt(i));
			if (layout.getChildAt(i) instanceof Button)
				TVlist.add((Button) layout.getChildAt(i));
		}
		for (TextView i : TVlist)
			Styler.styleText(context, i);
		for (Button i : BTlist)
			Styler.styleText(context, i);

	}

	public static void initStyle(Context context, View rl) {
		if (rl instanceof RelativeLayout)
			initStyle(context, (RelativeLayout) rl);
		if (rl instanceof LinearLayout)
			initStyle(context, (LinearLayout) rl);

		ArrayList<Button> btlist = new ArrayList<Button>();
		ArrayList<TextView> TVlist = new ArrayList<TextView>();
		View layout = rl;
		for (int i = 0; i < 9999; i++) {
			if (layout.findViewById(i) == null)
				continue;
			if (layout.findViewById(i) instanceof LinearLayout)
				initStyle(context, (LinearLayout) layout.findViewById(i));
			if (layout.findViewById(i) instanceof RelativeLayout)
				initStyle(context, (RelativeLayout) layout.findViewById(i));
			if (layout.findViewById(i) instanceof TextView)
				TVlist.add((TextView) layout.findViewById(i));
			if (layout.findViewById(i) instanceof Button)
				TVlist.add((Button) layout.findViewById(i));

		}
		for (TextView i : TVlist)
			Styler.styleText(context, i);
		for (Button i : btlist)
			Styler.styleText(context, i);

	}
}
