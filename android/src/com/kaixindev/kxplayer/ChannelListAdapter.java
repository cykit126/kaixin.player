package com.kaixindev.kxplayer;

import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.kaixindev.core.ObjectUtil;

public class ChannelListAdapter extends BaseAdapter {

	protected List<Channel> mChannels;
	protected Context mContext;

	public ChannelListAdapter(List<Channel> channels, Context context) {
		mChannels = channels;
		mContext = context;
	}

	@Override
	public int getCount() {
		return mChannels.size();
	}

	@Override
	public Object getItem(int position) {
		return mChannels.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		View view = convertView;
		if (convertView == null) {
			LayoutInflater inflater = (LayoutInflater) mContext
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			view = inflater.inflate(R.layout.channel_item, null);
		}

		String lang = getLanguage();

		TextView titleView = (TextView) view.findViewById(R.id.name);
		Channel channel = (Channel) getItem(position);
		if (channel != null) {
			titleView.setText(channel.name != null ? channel.name.get(lang) : null);
		}
		
		return view;
	}
	
	public void removeChannel(String uri) {
		for (Channel ch : mChannels) {
			if (ObjectUtil.equals(ch.uri, uri)) {
				mChannels.remove(ch);
				break;
			}
		}
	}

	protected String getLanguage() {
		return Locale.getDefault().toString();
	}
}
