package com.kaixindev.kxplayer.ui;

import java.util.List;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.kaixindev.kxplayer.Channel;
import com.kaixindev.kxplayer.ChannelListAdapter;
import com.kaixindev.kxplayer.R;

public class FavoritesAdapter extends ChannelListAdapter {

	public FavoritesAdapter(List<Channel> channels, FavoritesActivity activity) {
		super(channels, activity);
	}
	
	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		View view = super.getView(position, convertView, parent);
		
		TextView btnDel = (TextView) view.findViewById(R.id.delete);
		btnDel.setVisibility(View.VISIBLE);
		btnDel.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				final Channel channel = mChannels.get(position);
				AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
				String content = mContext.getResources().getString(
						R.string.confirm_favor_delete,
						channel.name.get(getLanguage()));
				AlertDialog dialog = builder
						.setMessage(content)
						.setPositiveButton(R.string.confirm_favor_delete_yes,
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog, int which) {
										getActivity().removeFavor(channel.uri);
									}
								})
						.setNegativeButton(R.string.confirm_favor_delete_no, null).create();
				dialog.show();
			}
		});		
		
		return view;
	}
	
	protected FavoritesActivity getActivity() {
		return (FavoritesActivity)mContext;
	}
}
