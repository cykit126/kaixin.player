package com.kaixindev.kxplayer.ui;

import java.util.List;

import android.os.Bundle;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.kaixindev.kxplayer.Channel;
import com.kaixindev.kxplayer.ChannelListAdapter;
import com.kaixindev.kxplayer.R;
import com.kaixindev.kxplayer.service.ChannelSourceService;

public class CategoriesActivity extends ChannelListActivity {
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.tab_list_activity);
		
		List<Channel> channels = ChannelSourceService.getChannels();
		
		ListAdapter adapter = new ChannelListAdapter(channels, this);
		ListView listView = (ListView) findViewById(R.id.listview);
		listView.setOnItemClickListener(this);
		listView.setAdapter(adapter);
		
		hideLoading();
	}


}





