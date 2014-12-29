package com.danielto.sample;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v4.view.PagerAdapter;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.danielto.inlineviewpager.R;

import java.util.HashMap;
import java.util.Map;

public class InlinePagerAdapter extends PagerAdapter {

    private Context context;
    private Map<View, Integer> currentViewPositionsByView = new HashMap<View, Integer>();
    private SparseArray<View> currentViewPositionsByPosition = new SparseArray<View>();
    private LayoutInflater layoutInflater;


    public InlinePagerAdapter(Context context) {
        this.context = context;
        this.layoutInflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return context.getResources().getStringArray(R.array.album_cover_array).length;
    }

    @Override
    public boolean isViewFromObject(View view, Object o) {
        return false;
    }

    @Override
    public View instantiateItem(ViewGroup container, int position) {
        View view = layoutInflater.inflate(R.layout.pager_item, container, false);

//        ImageView imageView = (ImageView) view.findViewById(R.id.album_cover_view);
        String name = context.getResources().getStringArray(R.array.album_cover_array)[position];
        Log.d("Daniel", "position: " + position + "\t" + "name: " + name);
        int imageId = context.getResources().getIdentifier(name + "_cover",
                "drawable", context.getPackageName());

        Drawable drawable = context.getResources().getDrawable(imageId);
        ((ImageView) view.findViewById(R.id.album_cover_view)).setImageResource(imageId);

        container.addView(view);
        currentViewPositionsByView.put(view, position);
        currentViewPositionsByPosition.put(position, view);
        return view;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object view) {
        container.removeView((View) view);
        currentViewPositionsByView.remove(view);
        currentViewPositionsByPosition.remove(position);
    }
}
