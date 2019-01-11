package com.util.player.view;

import android.content.Context;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.util.player.data.DataPlayInfo;
import com.util.player.R;

import java.util.ArrayList;

@RequiresApi(api = Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class PlayListAdapter extends BaseAdapter {
    private ArrayList<DataPlayInfo> mInfo = new ArrayList<>();
    private Context mContext;
    private View.OnClickListener mOnClickListener;
    private String mVideoNmae;

    public PlayListAdapter(Context context, View.OnClickListener clickListener) {
        this.mContext = context;
        this.mOnClickListener = clickListener;
    }

    @Override
    public int getCount() {
        return mInfo.size();
    }

    @Override
    public DataPlayInfo getItem(int i) {
        return mInfo.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        ViewHolder viewHolder;
        if (view == null) {
            view = LayoutInflater.from(mContext).inflate(R.layout.jz_layout_list_item_layout, null);
            viewHolder = new ViewHolder(view);
            view.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) view.getTag();
        }
        viewHolder.onBind(getItem(i), i);
        return view;
    }

    class ViewHolder {
        private TextView textView;

        public ViewHolder(View view) {
            textView = view.findViewById(R.id.textView);
        }

        public void onBind(DataPlayInfo item, int i) {
            textView.setText(item.getName());
            if (item.getName().equals(BaseUniversalPlayerView.mClarityText) || item.getName().equals(mVideoNmae)) {
                textView.setTextColor(mContext.getResources().getColor(R.color.jz_red));
            } else {
                textView.setTextColor(mContext.getResources().getColor(R.color.jz_deepgray));
            }
            textView.setTag(i);
            textView.setOnClickListener(onClickListener);

        }
    }

    private View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (null != mOnClickListener)
                mOnClickListener.onClick(view);
        }
    };

    public void setData(ArrayList<DataPlayInfo> info) {
        this.mInfo.clear();
        this.mInfo.addAll(info);
        this.notifyDataSetChanged();
    }

    public void setTitleText(String text) {
        mVideoNmae = text;
    }
}
