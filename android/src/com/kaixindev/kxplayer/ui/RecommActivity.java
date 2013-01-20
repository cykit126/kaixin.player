package com.kaixindev.kxplayer.ui;

import android.os.Bundle;

import com.kaixindev.kxplayer.R;
import com.kaixindev.kxplayer.config.Config;

public class RecommActivity extends ChannelListActivity {
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.tab_list_activity);
		
		loadChannels(Config.FILE_RECOMM_CHANNELS);
	}
}
